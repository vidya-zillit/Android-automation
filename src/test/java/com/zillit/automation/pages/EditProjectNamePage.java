package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Settings → Admin Settings → "Edit Project Name".
 *
 * <p>Ids confirmed by inspecting the live screen (never guessed): the toolbar
 * carries {@code backBtn} and {@code projectName} (the name shown at the TOP of
 * the app, which is what a successful rename has to update), the header is
 * {@code headerText}, the field is {@code editTextInput} inside
 * {@code editTextWidget}, and Submit is {@code buttonBtn} inside
 * {@code submit_btn_project_name}.
 *
 * <p><b>Validation is shown in a SNACKBAR, not a Toast.</b> That matters: this
 * screen's messages ARE visible to automation, unlike the Toast-based errors on
 * Company Details, which Android hides from the accessibility tree. The snackbar
 * is short-lived though (~2 seconds), so {@link #snackbarShown} polls tightly
 * rather than waiting on a single lookup.
 *
 * <p>The field itself enforces a 40-character ceiling via an input filter — extra
 * characters are dropped as they are typed, so there is no error to assert;
 * {@link #projectNameFieldText} is read back and measured instead.
 */
public class EditProjectNamePage extends BasePage {

    public EditProjectNamePage(AndroidDriver driver) {
        super(driver);
    }

    // ---------------- navigation ----------------

    /** Same menuText-in-bottomNavigation idiom the other Admin Settings pages use. */
    public boolean openSettingsTab() {
        try {
            WebElement navBar = driver.findElement(byId("bottomNavigation"));
            for (WebElement it : navBar.findElements(byId("menuText"))) {
                String t = it.getText();
                if (t != null && t.equalsIgnoreCase("Settings")) {
                    it.click();
                    return true;
                }
            }
        } catch (WebDriverException ignored) {
        }
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().textMatches(\"(?i)Settings\")")).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean openAdminSettings() {
        return tapListRow("Admin Settings");
    }

    public boolean openEditProjectName() {
        return tapListRow("Edit Project Name");
    }

    private boolean tapListRow(String label) {
        // Admin Settings is a long list and "Edit Project Name" sits near its
        // bottom, so bring the row on screen before looking for it.
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true))"
                            + ".scrollIntoView(new UiSelector().textContains(\"" + label + "\"))"));
        } catch (WebDriverException notScrollable) {
            // Short list, or already visible — the lookup below decides.
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

    /**
     * Identified by the FIELD plus the Submit button, never by the header text.
     * The Admin Settings screen it is reached from has a row reading "Edit Project
     * Name" too, and textContains would match that just as happily — the same trap
     * that made the Customization screen report itself loaded while still on the
     * menu.
     */
    public boolean isLoaded() {
        long deadline = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < deadline) {
            if (present("editTextInput") && present("buttonBtn")) {
                return true;
            }
            sleep(300);
        }
        return false;
    }

    private boolean present(String id) {
        try {
            return !driver.findElements(byId(id)).isEmpty();
        } catch (WebDriverException e) {
            return false;
        }
    }

    // ---------------- the field ----------------

    private WebElement field() {
        return new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.presenceOfElementLocated(byId("editTextInput")));
    }

    /**
     * Taps the field and empties it, verifying the result.
     *
     * <p>{@code clear()} alone is not trustworthy on this EditText — when it
     * silently leaves text behind, Submit reports a completely different message
     * ("please enter a new project name", or nothing at all) and the empty-name
     * case fails for a reason that looks nothing like the real cause. So: clear,
     * check, and fall back to deleting character by character.
     */
    public void clearProjectName() {
        WebElement f = field();
        f.click();
        try {
            f.clear();
        } catch (WebDriverException ignored) {
        }
        for (int attempt = 0; attempt < 3 && !projectNameFieldText().isEmpty(); attempt++) {
            int remaining = projectNameFieldText().length();
            for (int i = 0; i < remaining + 2; i++) {
                try {
                    driver.pressKey(new io.appium.java_client.android.nativekey.KeyEvent(
                            io.appium.java_client.android.nativekey.AndroidKey.DEL));
                } catch (WebDriverException stop) {
                    break;
                }
            }
        }
        hideKeyboard();
    }

    /** Clears, then types the given value. */
    public void setProjectName(String value) {
        WebElement f = field();
        f.click();
        f.clear();
        if (!value.isEmpty()) {
            f.sendKeys(value);
        }
        hideKeyboard();
    }

    /** Types WITHOUT clearing first — for checking the input filter's ceiling. */
    public void typeProjectName(String value) {
        WebElement f = field();
        f.click();
        f.sendKeys(value);
        hideKeyboard();
    }

    /** Current contents of the field (what the input filter actually allowed). */
    public String projectNameFieldText() {
        try {
            String t = field().getText();
            return t == null ? "" : t;
        } catch (WebDriverException e) {
            return "";
        }
    }

    public boolean tapSubmit() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(byId("buttonBtn")))
                    .click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    // ---------------- feedback ----------------

    /**
     * True if a snackbar containing {@code message} appears within the timeout.
     *
     * <p>Polls hard on purpose. The snackbar lives roughly two seconds, so a single
     * lookup with a long implicit wait can easily land either side of it and report
     * a false negative. Matches on CONTAINS, so exact punctuation cannot break the
     * assertion — the success message ends with a full stop that is easy to miss
     * when transcribing it from a spec.
     */
    public boolean snackbarShown(String message, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            try {
                if (!driver.findElements(AppiumBy.androidUIAutomator(
                        "new UiSelector().textContains(\"" + message + "\")")).isEmpty()) {
                    return true;
                }
            } catch (WebDriverException ignored) {
            }
        }
        return false;
    }

    /** The project name shown in the toolbar at the TOP of the screen. */
    public String toolbarProjectName() {
        try {
            return driver.findElement(byId("projectName")).getText();
        } catch (WebDriverException e) {
            return "";
        }
    }

    /** Top toolbar back arrow. */
    public boolean tapBackButton() {
        try {
            driver.findElement(byId("backBtn")).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean isOnAdminSettings(Duration timeout) {
        return onAdminSettingsScreen(timeout);
    }

    /**
     * True when the SETTINGS TAB is showing — the bottom-nav screen whose list
     * carries the "Admin Settings" row.
     *
     * <p>Mutually exclusive with {@link #isOnAdminSettings}: this screen has the
     * bottom nav, that one does not. Without separating the two, a caller sitting
     * on the Settings tab looks like it is somewhere unknown and gets sent through
     * a full unwind instead of simply opening Admin Settings.
     */
    public boolean isOnSettingsTab(Duration timeout) {
        return onSettingsTabScreen(timeout);
    }


    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
