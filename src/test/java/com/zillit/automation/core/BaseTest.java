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
    /** Set once the device is unreachable, so the rest of the class fails fast. */
    private static boolean deviceLost;
    private static String deviceLostReason = "";
    /** Why the last landing-marker lookup failed, if it did. See settleLanding(). */
    private WebDriverException lastLookupFailure;

    @BeforeAll
    void startSession() {
        driver = DriverFactory.create();
        // The package under test comes from CONFIG, never from whatever happens to be
        // on screen. Reading getCurrentPackage() here poisons the entire session if the
        // app is not foreground yet (slow cold start, the screen dropped to the launcher
        // while Gradle compiled, or a previous suite left the camera / gallery / QR
        // scanner in front): appPackage becomes e.g. the launcher, so every byAppId()
        // builds "<launcher>:id/bottomNavigation" and matches nothing, AND the
        // settleLanding() self-heal — which only re-activates when the current package
        // differs from appPackage — concludes it is already in the right app and never
        // brings it up. The run then fails 90s later with a misleading
        // "No known landing screen appeared", because the ids were WRONG rather than
        // absent and every lookup burned its full timeout.
        //
        // configuredAppPackage() gives zillit.resourcePackage precedence, so a flavour
        // whose resource ids differ from its application id can still be pointed
        // elsewhere; zillit.appPackage is the ordinary answer and is already set.
        appPackage = Config.configuredAppPackage();
        if (appPackage.isEmpty()) {
            // Nothing configured — the foreground app is the only clue left.
            appPackage = driver.getCurrentPackage();
        }
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

    /**
     * Presses back a few times if the app is genuinely stuck on a non-landing screen.
     *
     * <p><b>It WAITS before deciding that.</b> "No landing marker on screen" is also true
     * of an app that is still on its splash, and this runs in @BeforeAll — the earliest
     * possible moment. Backing out of a launching app does not unwind a stray screen, it
     * walks straight out of the project: project → project list → Start &amp; Join. Every
     * suite afterwards then skips with "Could not enter a project" on a device that was
     * logged in the whole time.
     *
     * <p>This used to be protected by accident: the three presence checks each waited 2s,
     * so a pass cost ~6s and the app usually finished starting inside it. Making those
     * checks instant removed that grace and turned a latent bug into a reliable one — so
     * the wait is now explicit and owned, instead of being a side effect of slow lookups.
     */
    private void unwindStrayScreens() {
        for (int i = 0; i < 4; i++) {
            if (waitForAnyLanding(Duration.ofSeconds(10))) {
                return; // already on a landing, or got there while we waited
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

    /**
     * Taps the Home bottom-nav tab if it is there. Scoped to the bottomNavigation
     * container: the nav tabs and the Home UNIT chips share the id menuText, so an
     * unscoped match taps a unit believing it tapped a tab.
     */
    private void selectHomeTabIfPresent() {
        try {
            var nav = driver.findElements(byAppId("bottomNavigation"));
            if (nav.isEmpty()) return;
            for (var tab : nav.get(0).findElements(byAppId("menuText"))) {
                String text = tab.getText();
                if (text != null && text.trim().equalsIgnoreCase("Home")) {
                    tab.click();
                    Thread.sleep(1000);
                    return;
                }
            }
        } catch (WebDriverException | InterruptedException ignored) {
            // Nav not ready — the suite's own entry code still self-heals.
        }
    }

    /** True once any of the three landing markers shows up within the budget. */
    private boolean waitForAnyLanding(Duration budget) {
        long deadline = System.currentTimeMillis() + budget.toMillis();
        while (true) {
            if (isPresent(byAppId("bottomNavigation"))
                    || isPresent(byAppId("projectListRecyclerView"))
                    || isPresent(byAppId("start_project_btn"))) {
                return true;
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
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
        // A dead UiAutomator2 instrumentation cannot be waited out. Every lookup after
        // it dies throws, settleLanding() swallows those throws as "marker not on
        // screen", and each remaining case in the class then burns its full 120s before
        // failing with "No known landing screen appeared" — a whole suite can spend
        // twenty minutes reporting a problem that was decided in its first case. Check
        // first, and rebuild the session instead of re-living the timeout.
        if (deviceLost) {
            // Decided by an earlier case in this class. Re-living the 120s landing budget
            // per case is how one lost emulator turned a 23-case suite into a 39-minute
            // run that proved nothing about the app.
            throw new IllegalStateException(deviceLostReason
                    + " — every remaining case in this class is failed immediately"
                    + " rather than waiting out a landing that cannot appear.");
        }
        if (!sessionAlive()) {
            if (recreateSession()) {
                firstLaunchHandled = false;
            } else {
                deviceLost = true;
                deviceLostReason = "The device stopped answering and the Appium session"
                        + " could not be rebuilt (the rebuild errors are above)";
                throw new IllegalStateException(deviceLostReason
                        + ". This is an environment failure, not a product one: nothing"
                        + " about the app under test was exercised.");
            }
        }
        if (!firstLaunchHandled) {
            prepareInitialState();
            firstLaunchHandled = true;
            return;
        }
        if (relaunchBetweenTests()) {
            relaunchApp();
            return;
        }
        // Ladder suites deliberately stay on one screen between cases, so NOTHING
        // re-foregrounds the app when a case ends somewhere else. A system file picker,
        // the gallery, a viewer or a share sheet belongs to another package, and every
        // byAppId() lookup after that searches a foreign activity, finds nothing, and
        // skips — one stranded case quietly takes the remainder of the module with it.
        recoverIfOutsideApp();
    }

    /**
     * Returns to the app under test if the previous case ended in another app.
     *
     * <p>Back presses first (they unwind the foreign activity cleanly), then activateApp
     * as the fallback for a screen that will not close.
     */
    private void recoverIfOutsideApp() {
        String current = safeCurrentPackage();
        if (appPackage.equals(current)) {
            return;
        }
        System.out.println(">>> Previous case ended in " + current
                + " — returning to " + appPackage + " before the next one");
        for (int i = 0; i < 4 && !appPackage.equals(safeCurrentPackage()); i++) {
            try {
                driver.navigate().back();
                Thread.sleep(700);
            } catch (WebDriverException | InterruptedException ignored) {
                break;
            }
        }
        ensureForeground();
    }

    private void prepareInitialState() {
        landing = settleLanding(120);
        // ---- in-project suites: normalise the TAB, do not restart the app.
        //
        // The original problem was real: Landing.BOTTOM_NAV only means "the bottom bar is
        // visible", which is equally true on Tools, Email or inside a unit's chat, so a
        // suite could start one tab away from the screen it asserts against.
        //
        // Restarting the app fixed that but cost a full cold start — splash, auth, project
        // fetch — on top of the launch that had ALREADY happened, so every suite paid for
        // two launches and roughly a minute before its first case. It also opened a window
        // where the landing search backs out of a half-loaded app.
        //
        // Tapping Home is the same correction for about a second, and it cannot leave the
        // project. HomeChatPage.isLoaded() self-heals onto Home as well, so this is belt
        // and braces, not the only guard.
        if (!requiresLoggedOutStart() && landing == Landing.BOTTOM_NAV) {
            selectHomeTabIfPresent();
        }
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
        // allowBackUnwind = FALSE. The app was just started fresh, so there is no stray
        // sub-screen to unwind — and a cold start (splash, auth, project fetch) routinely
        // takes longer than settleLanding's 8s grace. Left enabled, the self-heal starts
        // pressing BACK while the app is still loading and walks it OUT of the project:
        // project → project list → Start & Join. Every in-project suite then skips with
        // "Could not enter a project", on a device that was logged in the whole time.
        landing = settleLanding(90, false);
        if (requiresLoggedOutStart() && landing != Landing.START_AND_JOIN) {
            throw new IllegalStateException("Expected Start & Join after relaunch, got " + landing);
        }
    }

    /**
     * Drives past first-run gates (app-lock alert, onboarding) until one of the
     * three landing screens is visible, and reports which one.
     */
    protected Landing settleLanding(int timeoutSeconds) {
        return settleLanding(timeoutSeconds, true);
    }

    /**
     * @param allowBackUnwind press BACK periodically to unwind a stray sub-screen left by
     *   an earlier suite. Safe when we inherited an unknown screen; HARMFUL right after a
     *   relaunch, where the only thing BACK can do is leave the project.
     */
    protected Landing settleLanding(int timeoutSeconds, boolean allowBackUnwind) {
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        By okBtn = AppiumBy.androidUIAutomator("new UiSelector().textMatches(\"(?i)ok\")");
        By skipBtn = byAppId("skipBtn");
        By projectList = byAppId("projectListRecyclerView");
        By bottomNav = byAppId("bottomNavigation");
        By startAndJoin = byAppId("start_project_btn");

        long nextActivate = 0;
        lastLookupFailure = null;
        long nextBack = System.currentTimeMillis() + 8000; // give the app time to settle first
        while (System.currentTimeMillis() < deadline) {
            // Self-heal: if a prior run left the app backgrounded (on the
            // launcher / a system screen), the landing never appears. Re-foreground
            // it. Throttled so we don't spam activateApp every loop.
            // Every marker lookup below swallows its exceptions, because "the hierarchy
            // was in flux" is the usual reason one fails and the next pass settles it.
            // A dead instrumentation fails them the same way and never settles, so the
            // loop would wait out its whole timeout to report a missing landing screen
            // when the real answer — and it is not going to change — is that the device
            // half of the session is gone. Watch for that wording explicitly.
            if (lastLookupFailure != null && instrumentationGone(lastLookupFailure)) {
                throw new IllegalStateException(
                        "The UiAutomator2 instrumentation on the device is gone, so no"
                                + " landing screen can be detected — waiting out the"
                                + " remaining "
                                + Math.max(0, (deadline - System.currentTimeMillis()) / 1000)
                                + "s would not change that.", lastLookupFailure);
            }
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
            if (allowBackUnwind && appPackage.equals(cur) && System.currentTimeMillis() > nextBack) {
                try {
                    driver.navigate().back();
                } catch (WebDriverException ignored) {
                }
                nextBack = System.currentTimeMillis() + 2500;
            }
            // The landing checks are instant now, so pace the loop explicitly rather than
            // hammering the device with a page-source dump as fast as it can answer.
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
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

    /**
     * Appium's wording for "the on-device half of the session is gone".
     *
     * <p>The UiAutomator2 instrumentation runs as its own process on the device and can be
     * killed by the system (low memory, an ANR, or simply a long run of expensive
     * UiScrollable queries). The Appium server stays up and keeps answering, so the
     * session looks healthy from here — it just cannot reach the device any more, and says
     * so in the error text rather than in the status code.
     */
    private static boolean instrumentationGone(Throwable t) {
        for (Throwable e = t; e != null; e = e.getCause()) {
            String message = e.getMessage();
            if (message == null) continue;
            if (message.contains("instrumentation process is not running")
                    || message.contains("cannot be proxied to UiAutomator2 server")
                    || message.contains("A session is either terminated or not started")
                    // The emulator itself can drop off the bus mid-run — the five-suite
                    // verification lost it and adb answered "device offline" /
                    // "protocol fault" for the rest of the hour. That reaches us exactly
                    // as a dead instrumentation does, and waiting it out is as pointless.
                    || message.contains("device offline")
                    || message.contains("device not found")
                    || message.contains("protocol fault")
                    || message.contains("Error executing adbExec")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Can the session still reach the device? One cheap question, no waiting.
     *
     * <p>The probe MUST travel through the instrumentation, which is why it looks for an
     * id that cannot exist rather than asking something simpler.
     * {@code getCurrentPackage()} is served by adb on the Appium host and keeps answering
     * cheerfully for a session whose on-device half is dead — the exact state this is
     * here to catch. A findElements for a missing id is proxied to UiAutomator2, so it
     * returns an empty list when the instrumentation is alive and fails loudly when it
     * is not.
     */
    protected boolean sessionAlive() {
        try {
            driver.findElements(byAppId("zillitInstrumentationProbe"));
            return true;
        } catch (WebDriverException e) {
            return !instrumentationGone(e);
        }
    }

    /**
     * Throws away a session whose instrumentation has died and starts a fresh one.
     *
     * <p>Only safe BETWEEN cases: page objects hold the driver they were built with, so a
     * rebuild mid-case leaves the case holding a corpse. Suites build their page objects
     * per method, which is what makes the @BeforeEach hook the right place for this.
     *
     * <p>noReset is on, so this re-launches the instrumentation rather than reinstalling
     * the APK — about thirty seconds, against the twenty-odd minutes a class of timeouts
     * costs.
     */
    protected boolean recreateSession() {
        System.out.println(">>> The UiAutomator2 instrumentation on the device is gone"
                + " — rebuilding the Appium session");
        try {
            driver.quit();
        } catch (Exception ignored) {
            // Already unreachable; nothing to close politely.
        }
        // Two attempts, because the first one lands while the dying server is still
        // half-registered with the system. Appium then reports "The instrumentation
        // process cannot be initialized" and blames the app under test, which is
        // misleading — the app is fine; the previous UiAutomator2 server is what has not
        // finished letting go. Force-stopping it and waiting makes the retry work; this
        // was verified by hand against exactly the state that defeated the first attempt.
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                driver = DriverFactory.create();
                landing = Landing.UNKNOWN;
                ensureForeground();
                return true;
            } catch (Exception e) {
                System.out.println(">>> Rebuild attempt " + attempt + " failed: "
                        + e.getMessage());
                if (attempt == 2) return false;
                clearStaleInstrumentation();
            }
        }
        return false;
    }

    /**
     * Force-stops the UiAutomator2 server on the device so a new session can claim
     * {@code UiAutomation} for itself.
     *
     * <p>Shells out to adb rather than going through the driver, because the whole point
     * of being here is that there is no working driver to ask.
     */
    /**
     * Force-stops the UiAutomator2 server so a rebuild can take UiAutomation.
     *
     * <p>NOTE ON adb: every call here starts the adb server if it is not running. Two of
     * them racing produced this in a real run —
     * <pre>could not install *smartsocket* listener: Address already in use
     * adb: cannot connect to daemon</pre>
     * — two daemons fighting for port 5037, after which nothing could reach the device for
     * the rest of the run. The calls below are sequential and each waits, so they cannot
     * race each other; what they must not do is run while Appium is also shelling out. The
     * rebuild attempt that follows is therefore spaced by the sleep at the end.
     */
    private void clearStaleInstrumentation() {
        String udid = Config.get("zillit.udid");
        for (String pkg : new String[]{
                "io.appium.uiautomator2.server", "io.appium.uiautomator2.server.test"}) {
            try {
                java.util.List<String> cmd = new java.util.ArrayList<>(
                        java.util.List.of(adbPath()));
                if (udid != null && !udid.isBlank()) {
                    cmd.add("-s");
                    cmd.add(udid);
                }
                cmd.addAll(java.util.List.of("shell", "am", "force-stop", pkg));
                new ProcessBuilder(cmd).redirectErrorStream(true).start()
                        .waitFor(20, java.util.concurrent.TimeUnit.SECONDS);
            } catch (Exception e) {
                System.out.println(">>> Could not force-stop " + pkg + ": " + e.getMessage());
            }
        }
        try {
            Thread.sleep(5000); // let the system release the UiAutomation connection
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** adb from ANDROID_HOME / ANDROID_SDK_ROOT when PATH does not carry it. */
    private static String adbPath() {
        for (String var : new String[]{"ANDROID_HOME", "ANDROID_SDK_ROOT"}) {
            String root = System.getenv(var);
            if (root == null || root.isBlank()) continue;
            Path candidate = Paths.get(root, "platform-tools", "adb");
            if (Files.isExecutable(candidate)) return candidate.toString();
        }
        Path home = Paths.get(System.getProperty("user.home"),
                "Library", "Android", "sdk", "platform-tools", "adb");
        return Files.isExecutable(home) ? home.toString() : "adb";
    }

    /**
     * Is the element on screen RIGHT NOW? No waiting.
     *
     * <p>settleLanding() asks this for three different landing markers on every pass of
     * its loop. At 2s apiece that was up to 6s burned per pass on the two markers that
     * were never going to match — so the slowest landing to detect was also the one
     * charged the most for it. The loop itself is the wait; a single pass should be cheap.
     */
    private boolean isPresent(By locator) {
        try {
            for (org.openqa.selenium.WebElement candidate : driver.findElements(locator)) {
                if (candidate.isDisplayed()) {
                    return true;
                }
            }
        } catch (WebDriverException e) {
            // Usually the hierarchy in flux, and the next pass decides. Keep it anyway:
            // settleLanding() reads it to tell that apart from a dead instrumentation.
            lastLookupFailure = e;
        }
        return false;
    }

    /**
     * Clicks the control if it is there, paying nothing when it is not.
     *
     * <p>Presence is checked instantly first; only once something matches do we spend a
     * short wait on it becoming clickable. The old version waited 700ms on EVERY locator
     * whether or not it existed — and dismissPermissionPrompts() asks for three of them,
     * on every pass of settleLanding(), so ~2.1s went on confirming the absence of
     * dialogs over and over. The overwhelmingly common case is "no dialog", and that case
     * should be free.
     */
    private boolean clickIfPresent(By locator) {
        try {
            if (driver.findElements(locator).isEmpty()) {
                return false;
            }
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

    private com.zillit.automation.pages.common.ConfirmPopupPage popup() {
        return new com.zillit.automation.pages.common.ConfirmPopupPage(driver);
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
