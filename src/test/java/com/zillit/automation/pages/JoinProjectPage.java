package com.zillit.automation.pages;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;

/** Project-code entry screen (JoinProjectActivity). */
public class JoinProjectPage extends BasePage {

    public JoinProjectPage(AndroidDriver driver) {
        super(driver);
    }

    public boolean isLoaded() {
        return waitVisible("editTextInput").isDisplayed();
    }

    /** True while the project-code input is still on screen (no navigation happened). */
    public boolean isStillOnPage(Duration settleTime) {
        try {
            Thread.sleep(settleTime.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return isVisible("editTextInput", Duration.ofSeconds(5));
    }

    public void enterProjectCode(String code) {
        WebElement input = waitVisible("editTextInput");
        input.clear();
        if (!code.isEmpty()) {
            input.sendKeys(code);
        }
        hideKeyboard();
    }

    public void acceptTerms() {
        WebElement checkbox = waitVisible("termsCheckbox")
                .findElement(org.openqa.selenium.By.className("android.widget.CheckBox"));
        if ("false".equals(checkbox.getAttribute("checked"))) {
            checkbox.click();
        }
    }

    public boolean isTermsCheckboxVisible() {
        return isVisible("termsCheckbox", Duration.ofSeconds(5));
    }

    /** The inner QR button (hidden on phones — container stays, button is gone). */
    public boolean isQrJoinButtonShown() {
        if (!isVisible("joinUsingQrBtn", Duration.ofSeconds(4))) {
            return false;
        }
        try {
            return driver.findElement(byId("joinUsingQrBtn"))
                    .findElement(org.openqa.selenium.By.className("android.widget.TextView"))
                    .isDisplayed();
        } catch (org.openqa.selenium.WebDriverException e) {
            return false;
        }
    }

    public boolean isNextVisible() {
        return isVisible("nextBtn", Duration.ofSeconds(5));
    }

    public void tapNext() {
        waitVisible("nextBtn").click();
    }

    public void goBack() {
        driver.navigate().back();
    }
}
