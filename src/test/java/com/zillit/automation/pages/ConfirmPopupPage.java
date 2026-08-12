package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;

import java.time.Duration;

/**
 * The app's ONE confirmation popup — {@code common_dialog_layout_with_button}, used from
 * ~200 places: delete a message, forward it, share it, leave a group, empty a list.
 *
 * <pre>
 *   ┌──────────────────────────────┐
 *   │ header                       │
 *   │ message                      │
 *   │      [ leftButton ] [ rightButton ]
 *   └──────────────────────────────┘
 *        No / Cancel      Yes / Confirm / Delete
 * </pre>
 *
 * <p><b>Why this class exists.</b> It is a custom {@code Dialog}, not an
 * {@code AlertDialog}, so it has no {@code android:id/button1} and every helper that
 * looks for the framework ids walks straight past it. The popup then stays on screen, and
 * because it is modal, every following case fails or skips against a UI nobody can touch
 * — one unhandled confirmation after a Delete would take out the whole rest of a suite.
 *
 * <p>Matching by BUTTON TEXT is not good enough either: the labels change per call site
 * ("Yes", "Confirm", "Delete", "Leave"), and a text search for "Delete" happily matches
 * the menu row that opened the dialog in the first place.
 */
public class ConfirmPopupPage extends BasePage {

    public ConfirmPopupPage(AndroidDriver driver) {
        super(driver);
    }

    /**
     * The app uses TWO confirmation families, and a helper that knows only one silently
     * walks past the other:
     *
     * <ul>
     *   <li><b>Custom</b> — {@code common_dialog_layout_with_button} with
     *       {@code leftButton} / {@code rightButton}. Used across the chat modules,
     *       ~200 call sites.</li>
     *   <li><b>Framework</b> — {@code MaterialAlertDialogBuilder} with
     *       {@code android:id/button1} (positive) and {@code button2} (negative). This is
     *       what Document Distribution uses, e.g. LibraryFragment's
     *       "Delete this folder and everything inside it? This can't be undone."</li>
     * </ul>
     *
     * Both are modal, so missing either one blocks every later case.
     */
    private static final String FW_POSITIVE = "android:id/button1";
    private static final String FW_NEGATIVE = "android:id/button2";

    public boolean isShown(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (present("rightButton") || present("leftButton")) return true;
            if (frameworkPresent(FW_POSITIVE) || frameworkPresent(FW_NEGATIVE)) return true;
            sleep(300);
        }
        return false;
    }

    /** True when the dialog on screen is the FRAMEWORK one rather than the app's own. */
    public boolean isFrameworkDialog() {
        return !present("rightButton") && frameworkPresent(FW_POSITIVE);
    }

    private boolean frameworkPresent(String resourceId) {
        try {
            return !driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceId(\"" + resourceId + "\")")).isEmpty();
        } catch (WebDriverException e) {
            return false;
        }
    }

    private boolean clickFramework(String resourceId) {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceId(\"" + resourceId + "\")")).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** What it is asking — header + message, for assertions and failure text. */
    public String text() {
        String header = textOf("header");
        String msg = textOf("message");
        String combined = (header + " " + msg).trim();
        if (!combined.isEmpty()) return combined;
        // Framework dialog: title + message carry the platform ids.
        String t = frameworkText("android:id/alertTitle");
        String m = frameworkText("android:id/message");
        return (t + " " + m).trim();
    }

    private String frameworkText(String resourceId) {
        try {
            return driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceId(\"" + resourceId + "\")")).getText();
        } catch (WebDriverException e) {
            return "";
        }
    }

    public String confirmLabel() { return textOf("rightButton"); }
    public String cancelLabel()  { return textOf("leftButton"); }

    /**
     * Confirms (right button) and waits for the popup to actually close — tapping is not
     * the same as it being gone, and a half-dismissed dialog blocks the next step just as
     * effectively as an untouched one.
     */
    public boolean accept(Duration timeout) {
        if (!isShown(timeout)) return false;
        if (!click("rightButton") && !clickFramework(FW_POSITIVE)) return false;
        return waitGone(Duration.ofSeconds(10));
    }

    /** Declines (left button). */
    public boolean cancel(Duration timeout) {
        if (!isShown(timeout)) return false;
        if (!click("leftButton") && !clickFramework(FW_NEGATIVE)) return false;
        return waitGone(Duration.ofSeconds(10));
    }

    /**
     * Clears whatever confirmation is blocking the screen, by DECLINING it.
     *
     * <p>For recovery paths only. Declines rather than confirms on purpose: a blind "Yes"
     * on an unknown dialog can delete something no test asked to delete.
     */
    public boolean clearIfBlocking() {
        boolean cleared = false;
        for (int i = 0; i < 3 && isShown(Duration.ofSeconds(1)); i++) {
            if (cancel(Duration.ofSeconds(1))) { cleared = true; continue; }
            try { driver.navigate().back(); Thread.sleep(600); } catch (Exception e) { break; }
        }
        return cleared;
    }

    public boolean waitGone(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (!present("rightButton") && !present("leftButton")
                    && !frameworkPresent(FW_POSITIVE) && !frameworkPresent(FW_NEGATIVE)) {
                return true;
            }
            sleep(300);
        }
        return false;
    }

    /**
     * Confirms the popup if one appeared, and reports whether there WAS one — so a caller
     * can tell "no confirmation is shown for this action" (a finding) from "I confirmed
     * it" (business as usual).
     */
    public boolean acceptIfShown(Duration timeout) {
        return isShown(timeout) && accept(Duration.ofSeconds(2));
    }

    // ---------------- helpers ----------------

    private boolean present(String id) {
        try { return !driver.findElements(byId(id)).isEmpty(); }
        catch (WebDriverException e) { return false; }
    }

    private String textOf(String id) {
        try { return driver.findElement(byId(id)).getText(); }
        catch (WebDriverException e) { return ""; }
    }

    private boolean click(String id) {
        try { driver.findElement(byId(id)).click(); return true; }
        catch (WebDriverException e) {
            // Some call sites render the buttons inside a scrolling container; fall back
            // to a text tap on the SAME dialog rather than anywhere on screen.
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiSelector().resourceId(\"" + appPackage() + ":id/" + id + "\")")).click();
                return true;
            } catch (WebDriverException ignored) { return false; }
        }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
