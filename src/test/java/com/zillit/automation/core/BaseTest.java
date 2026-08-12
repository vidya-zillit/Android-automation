package com.zillit.automation.core;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
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
@ExtendWith(FailureScreenshot.class)
public abstract class BaseTest {

    public enum Landing { START_AND_JOIN, PROJECT_LIST, BOTTOM_NAV, UNKNOWN }

    protected AndroidDriver driver;
    protected String appPackage;
    protected Landing landing = Landing.UNKNOWN;
    private boolean firstLaunchHandled;

    @BeforeAll
    void startSession() {
        driver = DriverFactory.create();
        // The package under test comes from CONFIG, never from whatever happens to be
        // on screen. Reading getCurrentPackage() here poisons the entire session if the
        // app is not foreground yet (slow cold start, or the screen dropped to the
        // launcher while Gradle compiled): appPackage becomes e.g. the launcher, so
        // every byAppId() builds "<launcher>:id/bottomNavigation" and matches nothing,
        // AND the settleLanding() self-heal — which only re-activates when the current
        // package differs from appPackage — concludes it is already in the right app and
        // never brings it up. The run then fails 90s later with a misleading
        // "No known landing screen appeared".
        appPackage = Config.has("zillit.appPackage")
                ? Config.get("zillit.appPackage")
                : driver.getCurrentPackage();
        ensureForeground();
        // Nudge past any leftover editor/gallery/preview screen from a prior run
        // WITHOUT a full relaunch (a relaunch re-hits splash + the app-lock alert
        // + slow API routing, which stalls landing detection). A few back presses
        // unwind stray media screens; harmless on a real landing.
        unwindStrayScreens();
    }

    /**
     * Brings the app under test to the foreground before anything else runs.
     *
     * <p>A session can legitimately start with the app backgrounded — the runner
     * relaunches it, then Gradle compiles for minutes and the screen drops to the
     * launcher. Activating here means the landing search starts from the app rather
     * than spending its whole timeout looking at someone else's screen.
     */
    private void ensureForeground() {
        for (int i = 0; i < 3; i++) {
            if (appPackage.equals(safeCurrentPackage())) return;
            try {
                driver.executeScript("mobile: activateApp", Map.of("appId", appPackage));
                Thread.sleep(1500);
            } catch (WebDriverException | InterruptedException ignored) {
                return;
            }
        }
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
        // Name what was actually on screen — "no landing screen" alone sends you hunting
        // for a broken locator when the real story is usually "the app was not running".
        String onScreen = safeCurrentPackage();
        throw new IllegalStateException(
                "No known landing screen appeared within " + timeoutSeconds + "s"
                        + " (app under test: " + appPackage + ", foreground: " + onScreen + ")"
                        + (appPackage.equals(onScreen) ? ""
                        : " — the app under test was NOT in the foreground; it may have failed"
                        + " to launch, crashed, or the device dropped to the launcher."));
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

    // ---------------- the app's own confirmation popup ----------------
    //
    // Nearly every destructive or outward-facing action in this app — delete a message,
    // forward it, share it, empty a list — routes through ONE dialog:
    // `common_dialog_layout_with_button`, with `leftButton` (No / Cancel) and
    // `rightButton` (Yes / Confirm / Delete). It is used in ~200 places.
    //
    // It is a custom Dialog, NOT an AlertDialog, so it has no `android:id/button1`. Any
    // helper that only looks for the framework button ids sails straight past it, the
    // popup stays on screen, and every following case fails or skips against a blocked
    // UI — which is exactly the cascade this suite kept producing after a Delete.

    private com.zillit.automation.pages.ConfirmPopupPage popup() {
        return new com.zillit.automation.pages.ConfirmPopupPage(driver);
    }

    /** True while the app's confirm popup is on screen. */
    protected boolean confirmPopupShown(Duration timeout) {
        return popup().isShown(timeout);
    }

    /** What the popup is asking — for assertions and readable failure messages. */
    protected String confirmPopupMessage() {
        return popup().text();
    }

    /** Confirms it (Yes / Confirm / Delete) and waits for it to close. */
    protected boolean confirmPopupAccept(Duration timeout) {
        return popup().accept(timeout);
    }

    /** Declines it (No / Cancel). */
    protected boolean confirmPopupCancel(Duration timeout) {
        return popup().cancel(timeout);
    }

    /**
     * Safety net for recovery paths: clears a confirmation left blocking the screen, by
     * DECLINING it, so a popup from one case cannot swallow the next.
     */
    protected boolean clearBlockingPopup() {
        return popup().clearIfBlocking();
    }

    /**
     * WAITS for and confirms a "Are you sure you want to delete?" (No / Yes)
     * style dialog — the button appears a beat after Delete is tapped, so an
     * instant lookup misses it and leaves the popup stuck. Prefers "Yes" (the
     * confirm), then Delete / OK / Confirm. Returns true if it clicked one.
     *
     * <p>Tries the app's OWN popup ids first ({@link #confirmPopupAccept}); the text
     * search below only covers framework dialogs and silently missed every
     * {@code showDialogWithButton} confirmation in the app.
     */
    protected boolean confirmYesDialog(Duration total) {
        if (confirmPopupAccept(Duration.ofSeconds(Math.min(6, Math.max(1, total.toSeconds()))))) {
            return true;
        }
        return confirmYesDialogByText(total);
    }

    private boolean confirmYesDialogByText(Duration total) {
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
    /**
     * Writes a screenshot (and the page source) under a STABLE name so the report
     * generator can match it back to the test — no timestamp. Called automatically
     * by {@link FailureScreenshot} when a test fails or is skipped.
     */
    void captureDiagnostics(String status, String cls, String method, String displayName) {
        String base = status + "__" + cls + "__" + method;
        Path dir = Paths.get("build", "screenshots");
        boolean shot = false;
        try {
            Files.createDirectories(dir);
            Files.write(dir.resolve(base + ".png"), driver.getScreenshotAs(OutputType.BYTES));
            shot = true;
        } catch (Exception ignored) {
            // diagnostics are best-effort — never mask the real failure
        }
        try {
            String src = driver.getPageSource();
            if (src != null && !src.isBlank()) Files.writeString(dir.resolve(base + ".xml"), src);
        } catch (Exception ignored) { }
        // Manifest the report generator reads: status, class, display name, file.
        if (shot) {
            try {
                String line = String.join("\t", status, cls,
                        displayName == null ? "" : displayName.replace("\t", " "), base + ".png");
                Files.writeString(dir.resolve("manifest.tsv"), line + System.lineSeparator(),
                        java.nio.charset.StandardCharsets.UTF_8,
                        java.nio.file.StandardOpenOption.CREATE,
                        java.nio.file.StandardOpenOption.APPEND);
            } catch (Exception ignored) { }
        }
    }

    /** Set by {@link FailureScreenshot} before each test so step shots know their owner. */
    private String currentClass = "Unknown";
    private String currentDisplayName = "";
    private int stepCounter;

    void setTestContext(String cls, String displayName) {
        this.currentClass = cls;
        this.currentDisplayName = displayName == null ? "" : displayName;
        this.stepCounter = 0;
    }

    /**
     * Evidence screenshot for a step INSIDE a test — the "this is what the screen
     * looked like when we asserted" shot, captured whether or not the test passes.
     *
     * <p>Writes two files. The full-size PNG (timestamped) is kept for local
     * debugging. A downscaled JPEG is what the HTML report embeds: a raw device
     * screenshot is ~2 MB, and the report inlines images as base64 to stay a single
     * shareable file, so embedding full-size shots across ~270 calls would produce a
     * report hundreds of megabytes large. The thumbnail keeps it in the low MBs.
     */
    protected void screenshot(String name) {
        try {
            Path dir = Paths.get("build", "screenshots");
            Files.createDirectories(dir);
            byte[] png = driver.getScreenshotAs(OutputType.BYTES);
            String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            Files.write(dir.resolve(name + "-" + stamp + ".png"), png);

            String thumb = String.format("STEP__%s__%02d__%s.jpg", currentClass, ++stepCounter, safe(name));
            if (writeThumbnail(png, dir.resolve(thumb))) {
                String line = String.join("\t", "STEP", currentClass,
                        currentDisplayName.replace("\t", " "), thumb, name);
                Files.writeString(dir.resolve("manifest.tsv"), line + System.lineSeparator(),
                        java.nio.charset.StandardCharsets.UTF_8,
                        java.nio.file.StandardOpenOption.CREATE,
                        java.nio.file.StandardOpenOption.APPEND);
            }
        } catch (IOException | WebDriverException ignored) {
            // Screenshots are best-effort diagnostics; never fail a test over them.
        }
    }

    private static String safe(String s) {
        return s == null ? "shot" : s.replaceAll("[^A-Za-z0-9._-]", "-");
    }

    /** Scales the shot down to {@value #THUMB_WIDTH}px wide and writes it as JPEG. */
    private static boolean writeThumbnail(byte[] png, Path out) {
        try {
            java.awt.image.BufferedImage src =
                    javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(png));
            if (src == null) return false;
            int w = Math.min(THUMB_WIDTH, src.getWidth());
            int h = Math.max(1, (int) Math.round(src.getHeight() * (w / (double) src.getWidth())));
            java.awt.image.BufferedImage dst =
                    new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = dst.createGraphics();
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                    java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(src, 0, 0, w, h, null);
            g.dispose();
            return javax.imageio.ImageIO.write(dst, "jpg", out.toFile());
        } catch (Exception e) {
            return false;   // thumbnails are best-effort
        }
    }

    private static final int THUMB_WIDTH = 420;
}
