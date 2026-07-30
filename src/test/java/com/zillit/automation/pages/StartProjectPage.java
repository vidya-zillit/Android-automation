package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/** Create-project form (StartProjectActivity). */
public class StartProjectPage extends BasePage {

    public StartProjectPage(AndroidDriver driver) {
        super(driver);
    }

    public boolean isLoaded() {
        return waitVisible("editAdminFirstName").isDisplayed();
    }

    /** Quick check without the long wait — used for stay-on-screen recovery. */
    public boolean isFormVisible() {
        scrollToId("editAdminFirstName");
        return isVisible("editAdminFirstName", Duration.ofSeconds(4));
    }

    // ---------- plain text fields ----------

    public void setFirstName(String value) {
        typeInto("editAdminFirstName", value);
    }

    public void setLastName(String value) {
        typeInto("editAdminLastName", value);
    }

    public void setProjectName(String value) {
        typeInto("editProjectName", value);
    }

    public void setStorageCode(String value) {
        typeInto("storageCode", value);
    }

    /** The storage-code field is not rendered in every flavor/config. */
    public boolean isStorageCodeVisible() {
        scrollToId("storageCode");
        return isVisible("storageCode", Duration.ofSeconds(4));
    }

    public void setPhone(String value) {
        typeInto("etPhone", value);
    }

    public void setSubType(String value) {
        typeInto("selectSubProjectWidget", value);
    }

    public void setEmail(String value) {
        scrollToId("emailEditText");
        WebElement field = waitVisible("emailEditText");
        field.clear();
        if (!value.isEmpty()) {
            field.sendKeys(value);
        }
        hideKeyboard();
    }

    // ---------- pickers ----------

    /**
     * Opens the project-type dropdown and picks the first item whose text matches
     * (case-insensitive contains). Returns false if no such item exists.
     */
    /**
     * The exposed dropdown's popup items are NOT exposed to accessibility
     * (invisible to UiAutomator), so selection works by tapping the popup's
     * screen position and verifying the result through the field's own text.
     * Wrong slots self-correct: each attempt tries the next item position.
     */
    public boolean selectProjectType(String match) {
        scrollToId("selectProjectType");
        String want = match.toLowerCase();
        if (dropdownText().toLowerCase().contains(want)) {
            return true; // already selected
        }
        org.openqa.selenium.Rectangle r = waitVisible("selectProjectType")
                .findElement(byId("autoCompleteDropDown")).getRect();
        int itemH = (int) (r.getHeight() * 0.75);
        int centerX = r.getX() + r.getWidth() / 2;

        for (int attempt = 0; attempt < 6; attempt++) {
            waitVisible("selectProjectType").findElement(byId("autoCompleteDropDown")).click();
            pause(700); // popup animates open (first click may only focus)
            int slot = attempt % 3;
            tapAt(centerX, r.getY() + r.getHeight() + itemH * slot + itemH / 2);
            pause(500);
            if (isVisible("commonRecyclerView", Duration.ofSeconds(1))) {
                // Popup was closed and the tap hit the language field instead.
                driver.navigate().back();
                waitVisible("selectProjectType");
                continue;
            }
            if (dropdownText().toLowerCase().contains(want)) {
                return true;
            }
        }
        return dropdownText().toLowerCase().contains(want);
    }

    private String dropdownText() {
        try {
            String t = driver.findElement(byId("selectProjectType"))
                    .findElement(byId("autoCompleteDropDown")).getText();
            return t == null ? "" : t;
        } catch (WebDriverException e) {
            return "";
        }
    }

    private void tapAt(int x, int y) {
        driver.executeScript("mobile: clickGesture",
                java.util.Map.of("x", x, "y", y));
    }

    private void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Opens the language picker (search list screen) and picks the given language. */
    public void selectLanguage(String name) {
        scrollToId("select_project_language");
        waitVisible("select_project_language")
                .findElement(byId("autoCompleteDropDown"))
                .click();
        waitVisible("commonRecyclerView");
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().resourceIdMatches(\".*commonRecyclerView\"))"
                            + ".scrollIntoView(new UiSelector().textContains(\"" + name + "\"))"));
        } catch (WebDriverException ignored) {
            // List may be short enough that no scrolling is needed.
        }
        wait.until(ExpectedConditions.elementToBeClickable(
                AppiumBy.androidUIAutomator(
                        "new UiSelector().textContains(\"" + name + "\")")))
                .click();
        // Back on the form once the language field is visible again.
        waitVisible("select_project_language");
    }

    public boolean isSubTypeFieldVisible() {
        return isVisible("selectSubProjectTypeHolder", Duration.ofSeconds(4));
    }

    /**
     * For Entertainment projects the sub-type field opens a search list —
     * picks the first entry matching the name (case-insensitive).
     */
    public boolean selectSubTypeFromList(String name) {
        scrollToId("selectSubProjectInput");
        waitVisible("selectSubProjectInput").click();
        if (!isVisible("commonRecyclerView", Duration.ofSeconds(10))) {
            return false;
        }
        try {
            wait.until(ExpectedConditions.elementToBeClickable(
                    AppiumBy.androidUIAutomator(
                            "new UiSelector().textMatches(\"(?i).*" + name + ".*\")")))
                    .click();
            waitVisible("selectSubProjectInput"); // back on the form
            return true;
        } catch (WebDriverException e) {
            driver.navigate().back();
            return false;
        }
    }

    // ---------- email verification ----------

    public void tapVerifyEmail() {
        scrollToId("tvVerify");
        waitVisible("tvVerify").click();
    }

    public String verifyLabelText() {
        scrollToId("tvVerify");
        return waitVisible("tvVerify").getText();
    }

    /** Waits for the OTP bottom sheet to open after Verify is tapped. */
    public boolean waitForOtpSheet(Duration timeout) {
        return isVisible("otpView", timeout);
    }

    /**
     * Blocks until the email shows as Verified. The OTP itself is entered by a
     * human on the emulator — this only polls the Verify label.
     */
    public boolean waitForManualOtpVerification(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            try {
                String label = driver.findElement(byId("tvVerify")).getText();
                if ("Verified".equalsIgnoreCase(label)) {
                    return true;
                }
            } catch (WebDriverException ignored) {
                // Bottom sheet is covering the label — keep polling.
            }
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    // ---------- terms + submit ----------

    public void acceptTerms() {
        scrollToId("termsCheckbox");
        WebElement checkbox = waitVisible("termsCheckbox")
                .findElement(By.className("android.widget.CheckBox"));
        if ("false".equals(checkbox.getAttribute("checked"))) {
            checkbox.click();
        }
    }

    /** Scrolls to the bottom of the form; true when the Submit button is reachable. */
    public boolean scrollToSubmit() {
        scrollToId("submit_btn");
        return isVisible("submit_btn", Duration.ofSeconds(5));
    }

    public void tapSubmit() {
        scrollToId("submit_btn");
        waitVisible("submit_btn").click();
        hideKeyboard();
    }

    /** Waits until the app lands on the share-project-code screen (project created). */
    public boolean waitForProjectCreated(Duration timeout) {
        try {
            new WebDriverWait(driver, timeout).until(d -> {
                String activity = ((AndroidDriver) d).currentActivity();
                return activity != null && activity.contains("ShareProjectCode");
            });
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }
}
