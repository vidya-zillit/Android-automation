package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public abstract class BasePage {

    /**
     * Runtime resource-id prefix. Flavors rewrite it to the full application id
     * (e.g. com.zillit.zillitapp.dev), so resolve it from the foreground app.
     */
    private static String idPrefix;

    protected final AndroidDriver driver;
    protected final WebDriverWait wait;

    protected BasePage(AndroidDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        if (idPrefix == null) {
            String pkg = com.zillit.automation.core.Config.has("zillit.resourcePackage")
                    ? com.zillit.automation.core.Config.get("zillit.resourcePackage")
                    : driver.getCurrentPackage();
            idPrefix = pkg + ":id/";
        }
    }

    protected By byId(String id) {
        return AppiumBy.id(idPrefix + id);
    }

    protected WebElement waitVisible(String id) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(byId(id)));
    }

    protected boolean isVisible(String id, Duration timeout) {
        try {
            new WebDriverWait(driver, timeout)
                    .until(ExpectedConditions.visibilityOfElementLocated(byId(id)));
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Scrolls the first scrollable container until the given id is on screen. */
    protected void scrollToId(String id) {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true))"
                            + ".scrollIntoView(new UiSelector().resourceId(\"" + idPrefix + id + "\"))"));
        } catch (WebDriverException ignored) {
            // Not scrollable or already on screen — the follow-up lookup will decide.
        }
    }

    /** Types into the EditText nested inside a widget container. */
    protected void typeInto(String containerId, String text) {
        scrollToId(containerId);
        WebElement field = waitVisible(containerId)
                .findElement(By.className("android.widget.EditText"));
        field.clear();
        if (!text.isEmpty()) {
            field.sendKeys(text);
        }
        hideKeyboard();
    }

    protected void hideKeyboard() {
        try {
            driver.hideKeyboard();
        } catch (WebDriverException ignored) {
            // Keyboard was not open.
        }
    }

    /** True when any element containing the text (snackbar, toast, label) appears. */
    public boolean textShown(String text, Duration timeout) {
        try {
            new WebDriverWait(driver, timeout)
                    .until(ExpectedConditions.presenceOfElementLocated(
                            AppiumBy.androidUIAutomator(
                                    "new UiSelector().textContains(\"" + text + "\")")));
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean textShown(String text) {
        return textShown(text, Duration.ofSeconds(10));
    }
}
