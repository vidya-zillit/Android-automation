package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Settings → Admin Settings → "Delete Project", and the countdown it puts on the
 * Settings tab.
 *
 * <p>Ids confirmed by walking the flow: the Admin Settings row is an ordinary
 * {@code nameTv} that relabels itself between "Delete Project" and "Stop Project
 * Deletion"; the delay choice is a bottom sheet whose {@code header} carries the
 * question and whose {@code valueTextView} rows are 12 Hours, 24 Hours, 48 Hours
 * and Cancel; stopping asks through the app's usual {@code message} /
 * {@code leftButton} / {@code rightButton} dialog; and once armed, the Settings
 * tab shows {@code projectTimeLeft} as HH:MM:SS.
 *
 * <p><b>The row label IS the state.</b> There is no separate flag to read —
 * "Delete Project" means nothing is scheduled, "Stop Project Deletion" means a
 * timer is running. {@link #deletionScheduled} reads exactly that.
 *
 * <p><b>Snackbars linger here too</b>, so an action whose message matters must
 * wait for the previous one to clear first or it reads stale text.
 *
 * <p><b>This page arms a real deletion.</b> Choosing a delay schedules the
 * project to be destroyed after it, recoverable only by stopping the timer before
 * it expires.
 */
public class DeleteprojectEntertainment extends BasePage {

    // ---- labels and messages, all read off the running app ----
    public static final String ROW_DELETE = "Delete Project";
    public static final String ROW_STOP = "Stop Project Deletion";
    public static final String CONFIRM_DELETE = "Are you sure you want to delete this project?";
    public static final String CONFIRM_STOP = "Are you sure you want to stop project deletion?";
    /** The app spells "deletion" correctly and ends with a full stop. */
    public static final String MARKED = "Project has been marked for deletion successfully.";
    public static final String STOPPED = "Project deletion has been stopped successfully.";

    public static final String OPT_12H = "12 Hours";
    public static final String OPT_24H = "24 Hours";
    public static final String OPT_48H = "48 Hours";
    public static final String OPT_CANCEL = "Cancel";

    public DeleteprojectEntertainment(AndroidDriver driver) {
        super(driver);
    }

    // ---------------- navigation ----------------

    public boolean openSettingsTab() {
        try {
            WebElement navBar = driver.findElement(byId("bottomNavigation"));
            for (WebElement it : navBar.findElements(byId("menuText"))) {
                String t = it.getText();
                if (t != null && t.equalsIgnoreCase("Settings")) {
                    it.click();
                    sleep(1500);
                    return true;
                }
            }
        } catch (WebDriverException ignored) {
        }
        return false;
    }

    public boolean openAdminSettings() {
        return tapListRow("Admin Settings");
    }

    private boolean tapListRow(String label) {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true))"
                            + ".scrollIntoView(new UiSelector().textContains(\"" + label + "\"))"));
        } catch (WebDriverException notScrollable) {
            // Short list or already visible.
        }
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath(
                            "//*[contains(@resource-id,'nameTv') and contains(@text,\"" + label
                                    + "\")]/ancestor::*[contains(@resource-id,'commonListItemHolder')]")))
                    .click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Gets to ADMIN SETTINGS from wherever the app is, retrying each hop. */
    public boolean ensureOnAdminSettings(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (onAdminSettingsScreen(Duration.ofMillis(800))) {
                return true;
            }
            if (isOptionsSheetOpen(Duration.ofMillis(300))) {
                chooseOption(OPT_CANCEL);
                sleep(1200);
                continue;
            }
            if (isConfirmDialogShown(Duration.ofMillis(300))) {
                tapDialogNo();
                sleep(1200);
                continue;
            }
            if (onSettingsTabScreen(Duration.ofMillis(400))) {
                openAdminSettings();
                sleep(1500);
                continue;
            }
            if (hasBottomNav()) {
                openSettingsTab();
                sleep(1500);
                continue;
            }
            try {
                driver.navigate().back();
            } catch (WebDriverException ignored) {
            }
            sleep(900);
        }
        return false;
    }

    public boolean isOnAdminSettings(Duration timeout) {
        return onAdminSettingsScreen(timeout);
    }

    public boolean isOnSettingsTab(Duration timeout) {
        return onSettingsTabScreen(timeout);
    }

    private boolean hasBottomNav() {
        try {
            return !driver.findElements(byId("bottomNavigation")).isEmpty();
        } catch (WebDriverException e) {
            return false;
        }
    }

    // ---------------- the row ----------------

    /**
     * The current label of the delete row, scrolling it into view first.
     *
     * <p>Returns "" when neither label is present, which is how a caller can tell
     * the row simply has not rendered yet.
     */
    public String deleteRowLabel() {
        for (String label : new String[] {ROW_STOP, ROW_DELETE}) {
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiScrollable(new UiSelector().scrollable(true))"
                                + ".scrollIntoView(new UiSelector().textContains(\"" + label + "\"))"));
            } catch (WebDriverException notFound) {
                continue;
            }
            try {
                for (WebElement n : driver.findElements(byId("nameTv"))) {
                    String t = safeText(n);
                    if (t != null && t.trim().equalsIgnoreCase(label)) {
                        return t.trim();
                    }
                }
            } catch (WebDriverException ignored) {
            }
        }
        return "";
    }

    /** True while a deletion timer is armed — the row reads "Stop Project Deletion". */
    public boolean deletionScheduled() {
        return ROW_STOP.equalsIgnoreCase(deleteRowLabel());
    }

    /** Waits for the row to settle on a label; the change follows a server round trip. */
    public boolean waitForRowLabel(String expected, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            if (expected.equalsIgnoreCase(deleteRowLabel())) {
                return true;
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(700);
        }
    }

    public boolean tapDeleteRow() {
        return tapRow(ROW_DELETE);
    }

    public boolean tapStopRow() {
        return tapRow(ROW_STOP);
    }

    private boolean tapRow(String label) {
        return tapListRow(label);
    }

    // ---------------- delay options sheet ----------------

    public boolean isOptionsSheetOpen(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            try {
                if (!driver.findElements(byId("valueTextView")).isEmpty()
                        && !driver.findElements(byId("header")).isEmpty()) {
                    return true;
                }
            } catch (WebDriverException ignored) {
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(200);
        }
    }

    /** The sheet's question — "Are you sure you want to delete this project?". */
    public String optionsSheetHeader() {
        return textOf("header");
    }

    public List<String> options() {
        List<String> out = new ArrayList<>();
        try {
            for (WebElement v : driver.findElements(byId("valueTextView"))) {
                String t = safeText(v);
                if (t != null && !t.isBlank()) {
                    out.add(t.trim());
                }
            }
        } catch (WebDriverException ignored) {
        }
        return out;
    }

    /** Picks a delay (or Cancel) by EXACT label. */
    public boolean chooseOption(String label) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath(
                            "//*[contains(@resource-id,'valueTextView') and @text=\"" + label + "\"]")))
                    .click();
            sleep(1500);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    // ---------------- stop-deletion dialog ----------------

    public boolean isConfirmDialogShown(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            try {
                if (!driver.findElements(byId("message")).isEmpty()
                        && !driver.findElements(byId("rightButton")).isEmpty()) {
                    return true;
                }
            } catch (WebDriverException ignored) {
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(200);
        }
    }

    public String dialogMessage() {
        return textOf("message");
    }

    public boolean tapDialogNo() {
        return tapById("leftButton");
    }

    public boolean tapDialogYes() {
        return tapById("rightButton");
    }

    // ---------------- countdown ----------------

    /** Raw countdown text on the Settings tab, e.g. "11:59:48", or "" when absent. */
    public String countdownText() {
        return textOf("projectTimeLeft");
    }

    public boolean countdownShown(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            if (!countdownText().isBlank()) {
                return true;
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(500);
        }
    }

    /** The countdown as seconds, or -1 when it is missing or unparseable. */
    public long countdownSeconds() {
        String t = countdownText();
        String[] parts = t.split(":");
        if (parts.length != 3) {
            return -1;
        }
        try {
            return Long.parseLong(parts[0].trim()) * 3600
                    + Long.parseLong(parts[1].trim()) * 60
                    + Long.parseLong(parts[2].trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * True when the countdown actually ticks down.
     *
     * <p>Presence alone would pass on a frozen label, so two samples are taken a
     * few seconds apart and the second must be strictly smaller.
     */
    public boolean countdownIsRunning(Duration sampleGap) {
        long first = countdownSeconds();
        if (first < 0) {
            return false;
        }
        sleep(Math.max(2000, sampleGap.toMillis()));
        long second = countdownSeconds();
        return second >= 0 && second < first;
    }

    // ---------------- snackbars ----------------

    public String currentSnackbar() {
        try {
            List<WebElement> found = driver.findElements(byId("snackbar_text"));
            return found.isEmpty() ? null : safeText(found.get(0));
        } catch (WebDriverException e) {
            return null;
        }
    }

    public boolean snackbarShown(String message, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            String s = currentSnackbar();
            if (s != null && s.contains(message)) {
                return true;
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(250);
        }
    }

    /** Load-bearing: a leftover message is read instead of the new one otherwise. */
    public boolean waitForSnackbarToClear(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            if (currentSnackbar() == null) {
                return true;
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(400);
        }
    }

    // ---------------- helpers ----------------

    public boolean tapBack() {
        return tapById("backBtn");
    }

    private boolean tapById(String id) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(byId(id)))
                    .click();
            sleep(900);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    private String textOf(String id) {
        try {
            List<WebElement> found = driver.findElements(byId(id));
            return found.isEmpty() ? "" : String.valueOf(safeText(found.get(0))).trim();
        } catch (WebDriverException e) {
            return "";
        }
    }

    private String safeText(WebElement e) {
        try {
            return e.getText();
        } catch (WebDriverException stale) {
            return "";
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
