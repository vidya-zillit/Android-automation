package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * User-details form shown after joining a project with a valid code
 * (JoinUserDetailPage). Department/designation open search lists; the unit
 * field is the accessibility-invisible exposed dropdown (coordinate taps).
 */
public class JoinUserDetailPage extends BasePage {

    public JoinUserDetailPage(AndroidDriver driver) {
        super(driver);
    }

    public boolean isLoaded(Duration timeout) {
        return isVisible("enter_name", timeout);
    }

    public void setFirstName(String value) {
        typeInto("enter_name", value);
    }

    public void setLastName(String value) {
        typeInto("enterLastName", value);
    }

    public void setEmail(String value) {
        typeInto("etEmail", value);
    }

    public void setPhone(String value) {
        typeInto("etPhone", value);
    }

    public void tapDesignation() {
        scrollToId("select_designation");
        waitVisible("select_designation").click();
    }

    /** Opens the department search list and picks its first entry. */
    public boolean selectFirstDepartment() {
        scrollToId("select_department");
        waitVisible("select_department").click();
        return pickFirstFromSearchList();
    }

    /** Opens the designation search list and picks its first entry. */
    public boolean selectFirstDesignation() {
        tapDesignation();
        return pickFirstFromSearchList();
    }

    private boolean pickFirstFromSearchList() {
        if (!isVisible("commonRecyclerView", Duration.ofSeconds(10))) {
            return false;
        }
        try {
            wait.until(ExpectedConditions.elementToBeClickable(
                    AppiumBy.androidUIAutomator(
                            "new UiSelector().resourceIdMatches(\".*commonRecyclerView\")"
                                    + ".childSelector(new UiSelector().index(0))")))
                    .click();
            return isLoaded(Duration.ofSeconds(15));
        } catch (WebDriverException e) {
            driver.navigate().back();
            return false;
        }
    }

    /**
     * Selects the first available unit via coordinate taps (the dropdown's
     * popup is invisible to accessibility), verified through the field text.
     */
    public boolean selectFirstUnit() {
        scrollToId("selectUnit");
        org.openqa.selenium.Rectangle r = waitVisible("selectUnit")
                .findElement(byId("autoCompleteDropDown")).getRect();
        int itemH = (int) (r.getHeight() * 0.75);
        for (int attempt = 0; attempt < 6; attempt++) {
            waitVisible("selectUnit").findElement(byId("autoCompleteDropDown")).click();
            pause(700);
            int slot = attempt % 3;
            driver.executeScript("mobile: clickGesture", java.util.Map.of(
                    "x", r.getX() + r.getWidth() / 2,
                    "y", r.getY() + r.getHeight() + itemH * slot + itemH / 2));
            pause(500);
            if (!unitText().isEmpty()) {
                return true;
            }
        }
        return !unitText().isEmpty();
    }

    private String unitText() {
        try {
            String t = driver.findElement(byId("selectUnit"))
                    .findElement(byId("autoCompleteDropDown")).getText();
            return t == null ? "" : t.trim();
        } catch (WebDriverException e) {
            return "";
        }
    }

    private void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void tapSubmit() {
        scrollToId("submit_btn");
        waitVisible("submit_btn").click();
        hideKeyboard();
    }

    public void dismissDialog() {
        driver.navigate().back();
    }
}
