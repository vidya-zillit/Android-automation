package com.zillit.automation.core;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * One Appium session per test class (APK installed once), one fast app launch
 * per test method (or per class for ladder-style suites).
 *
 * The app's landing screen depends on login state: Start & Join (logged out),
 * Project List (logged in, several projects) or Bottom Navigation (logged in,
 * single project). Suites declare what they need via
 * {@link #requiresLoggedOutStart()}; logged-out suites get app data cleared
 * automatically when the app is found logged in.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseTest {

    public enum Landing { START_AND_JOIN, PROJECT_LIST, BOTTOM_NAV, UNKNOWN }

    protected AndroidDriver driver;
    protected String appPackage;
    protected Landing landing = Landing.UNKNOWN;
    private boolean firstLaunchHandled;

    @BeforeAll
    void startSession() {
        driver = DriverFactory.create();
        appPackage = driver.getCurrentPackage();
        // Nudge past any leftover editor/gallery/preview screen from a prior run
        // WITHOUT a full relaunch (a relaunch re-hits splash + the app-lock alert
        // + slow API routing, which stalls landing detection). A few back presses
        // unwind stray media screens; harmless on a real landing.
        unwindStrayScreens();
    }

    /** Presses back a few times if the app is on a non-landing (media) screen. */
    private void unwindStrayScreens() {
        for (int i = 0; i < 4; i++) {
            if (isPresent(byAppId("bottomNavigation"))
                    || isPresent(byAppId("projectListRecyclerView"))
                    || isPresent(byAppId("start_project_btn"))) {
                return; // already on a landing
            }
            if (!appPackage.equals(safeCurrentPackage())) {
                return; // left the app (a system screen) — don't keep backing out
            }
            try {
                driver.navigate().back();
                Thread.sleep(800);
            } catch (WebDriverException | InterruptedException ignored) {
            }
        }
    }

    @AfterAll
    void stopSession() {
        if (driver != null) {
            driver.quit();
        }
    }

    /** Ladder-style suites override to false and stay on one screen. */
    protected boolean relaunchBetweenTests() {
        return true;
    }

    /**
     * True (default): the suite needs the logged-out Start & Join entry; app
     * data is cleared if the app turns out to be logged in. False: the suite
     * takes whatever state exists and reads {@link #landing}.
     */
    protected boolean requiresLoggedOutStart() {
        return true;
    }

    @BeforeEach
    void freshLaunch() {
        if (!firstLaunchHandled) {
            prepareInitialState();
            firstLaunchHandled = true;
            return;
        }
        if (relaunchBetweenTests()) {
            relaunchApp();
        }
    }

    private void prepareInitialState() {
        landing = settleLanding(120);
        if (requiresLoggedOutStart() && landing != Landing.START_AND_JOIN) {
            System.out.println(">>> App is logged in (" + landing
                    + ") but this suite needs a fresh start — clearing app data");
            driver.executeScript("mobile: clearApp", Map.of("appId", appPackage));
            driver.executeScript("mobile: activateApp", Map.of("appId", appPackage));
            landing = settleLanding(120);
            if (landing != Landing.START_AND_JOIN) {
                throw new IllegalStateException(
                        "Start & Join did not appear even after clearing app data (got "
                                + landing + ")");
            }
        }
    }

    /** Kills and reopens the app, settling on its landing screen again. */
    protected void relaunchApp() {
        driver.executeScript("mobile: terminateApp", Map.of("appId", appPackage));
        driver.executeScript("mobile: activateApp", Map.of("appId", appPackage));
        landing = settleLanding(90);
        if (requiresLoggedOutStart() && landing != Landing.START_AND_JOIN) {
            throw new IllegalStateException("Expected Start & Join after relaunch, got " + landing);
        }
    }

    /**
     * Drives past first-run gates (app-lock alert, onboarding) until one of the
     * three landing screens is visible, and reports which one.
     */
    protected Landing settleLanding(int timeoutSeconds) {
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        By okBtn = AppiumBy.androidUIAutomator("new UiSelector().textMatches(\"(?i)ok\")");
        By skipBtn = byAppId("skipBtn");
        By projectList = byAppId("projectListRecyclerView");
        By bottomNav = byAppId("bottomNavigation");
        By startAndJoin = byAppId("start_project_btn");

        long nextActivate = 0;
        long nextBack = System.currentTimeMillis() + 8000; // give the app time to settle first
        while (System.currentTimeMillis() < deadline) {
            // Self-heal: if a prior run left the app backgrounded (on the
            // launcher / a system screen), the landing never appears. Re-foreground
            // it. Throttled so we don't spam activateApp every loop.
            String cur = safeCurrentPackage();
            if (cur != null && !appPackage.equals(cur) && System.currentTimeMillis() > nextActivate) {
                try {
                    driver.executeScript("mobile: activateApp", Map.of("appId", appPackage));
                } catch (WebDriverException ignored) {
                }
                nextActivate = System.currentTimeMillis() + 4000;
            }
            // Clear any permission/location prompts blocking the screen. Runs
            // fast when there are none; NB: do NOT `continue` afterwards or a
            // persistently-matched control could starve the landing checks.
            dismissPermissionPrompts();
            // Order matters: the project list also contains a start_project_btn.
            if (isPresent(projectList)) return Landing.PROJECT_LIST;
            if (isPresent(bottomNav)) return Landing.BOTTOM_NAV;
            clickIfPresent(okBtn);
            clickIfPresent(skipBtn);
            if (isPresent(startAndJoin) && !isPresent(projectList)) {
                return Landing.START_AND_JOIN;
            }
            // Self-heal #2: a prior class can foreground a deep sub-screen (e.g. an
            // email folder) with no landing marker. Periodically press back to
            // unwind toward a landing while we're in-app.
            if (appPackage.equals(cur) && System.currentTimeMillis() > nextBack) {
                try {
                    driver.navigate().back();
                } catch (WebDriverException ignored) {
                }
                nextBack = System.currentTimeMillis() + 2500;
            }
        }
        throw new IllegalStateException(
                "No known landing screen appeared within " + timeoutSeconds + "s");
    }

    protected By byAppId(String id) {
        return AppiumBy.id(appPackage + ":id/" + id);
    }

    private String safeCurrentPackage() {
        try {
            return driver.getCurrentPackage();
        } catch (WebDriverException e) {
            return null;
        }
    }

    private boolean isPresent(By locator) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(2))
                    .until(ExpectedConditions.visibilityOfElementLocated(locator));
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    private boolean clickIfPresent(By locator) {
        // Short wait: dialogs render fast, so a long wait here just wastes time
        // on every no-op check (this runs constantly). 700ms is plenty.
        try {
            new WebDriverWait(driver, Duration.ofMillis(700))
                    .until(ExpectedConditions.elementToBeClickable(locator))
                    .click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /**
     * Dismisses the permission prompts the app shows: the in-app "important
     * Notifications" sheet (CONTINUE), Android system permission dialogs (Allow
     * / While using the app / Only this time — e.g. mic, camera, media,
     * location, contacts) and the Google "Location Accuracy" sheet (No thanks).
     * Safe anytime — a no-op when none are present. Returns true if it dismissed
     * at least one prompt.
     */
    protected boolean dismissPermissionPrompts() {
        boolean any = false;
        By inAppContinue = byAppId("tvContinue");
        // Grant buttons: "Allow", "Allow all", "Allow all the time", "While using
        // the app", "Only this time" (full-match; "Don't allow" is NOT matched).
        By systemAllow = AppiumBy.androidUIAutomator(
                "new UiSelector().textMatches(\"(?i)(allow all.*|allow|while using the app|"
                        + "allow only while using the app|only this time)\")");
        // Google "Location Accuracy" settings dialog — decline (privacy-preserving).
        By noThanks = AppiumBy.androidUIAutomator(
                "new UiSelector().textMatches(\"(?i)(no thanks|not now|skip)\")");
        for (int i = 0; i < 12; i++) {
            if (clickIfPresent(inAppContinue)) { any = true; continue; }
            if (clickIfPresent(systemAllow)) { any = true; continue; }
            if (clickIfPresent(noThanks)) { any = true; continue; }
            break; // nothing left to dismiss
        }
        return any;
    }

    /**
     * WAITS for and confirms a "Are you sure you want to delete?" (No / Yes)
     * style dialog — the button appears a beat after Delete is tapped, so an
     * instant lookup misses it and leaves the popup stuck. Prefers "Yes" (the
     * confirm), then Delete / OK / Confirm. Returns true if it clicked one.
     */
    protected boolean confirmYesDialog(Duration total) {
        // Poll ONLY for the confirm button ("Yes"/"Confirm"/"OK"). Deliberately
        // NOT "Delete": that is the *trigger* (long-press menu / multi-select
        // top-bar) that OPENS this dialog — clicking it just re-opens the popup
        // and leaves the UI stuck (the exact bug this method exists to fix).
        long deadline = System.currentTimeMillis() + total.toMillis();
        By confirm = AppiumBy.androidUIAutomator(
                "new UiSelector().textMatches(\"(?i)(yes|confirm|ok)\")");
        while (System.currentTimeMillis() < deadline) {
            try {
                new WebDriverWait(driver, Duration.ofMillis(700))
                        .until(ExpectedConditions.elementToBeClickable(confirm))
                        .click();
                // Verify the dialog actually closed (the button is gone).
                try {
                    new WebDriverWait(driver, Duration.ofSeconds(2))
                            .until(ExpectedConditions.invisibilityOfElementLocated(confirm));
                } catch (WebDriverException ignored) {
                }
                return true;
            } catch (WebDriverException notYet) {
                // Confirm button not shown yet — keep polling until the deadline.
            }
        }
        return false;
    }

    /**
     * Polls for a permission prompt for up to {@code total} and dismisses it.
     * Use where a system prompt is EXPECTED but may appear after a short delay
     * (mic on record, media/document pickers) — a single dismiss pass can run
     * before the OS shows the dialog. Returns true if anything was dismissed.
     */
    protected boolean waitAndDismissPermissions(Duration total) {
        long deadline = System.currentTimeMillis() + total.toMillis();
        boolean any = false;
        while (System.currentTimeMillis() < deadline) {
            if (dismissPermissionPrompts()) { any = true; }
            else if (any) break; // dismissed something, now clear — done
        }
        return any;
    }

    /** Saves a screenshot under build/screenshots for debugging and reports. */
    protected void screenshot(String name) {
        try {
            Path dir = Paths.get("build", "screenshots");
            Files.createDirectories(dir);
            String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            Files.write(dir.resolve(name + "-" + stamp + ".png"),
                    driver.getScreenshotAs(OutputType.BYTES));
        } catch (IOException | WebDriverException ignored) {
            // Screenshots are best-effort diagnostics; never fail a test over them.
        }
    }
}
