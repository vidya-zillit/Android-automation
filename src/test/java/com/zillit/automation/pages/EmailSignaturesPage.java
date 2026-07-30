package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;

/**
 * Email Signatures — list (SignatureListActivity, fragment_signature_list.xml)
 * and editor (EditSignatureActivity, fragment_edit_signature.xml).
 * List: recyclerSignatures, emptyState, fabAdd (visible only when list empty —
 * one signature max); row item_signature → tv_signature_name, btn_edit,
 * btn_delete, web_view_preview. Editor: editName, webViewEditor, Save = selectTv.
 * Validation: name required (inputLayoutName error "Name is required").
 */
public class EmailSignaturesPage extends BasePage {

    public EmailSignaturesPage(AndroidDriver driver) {
        super(driver);
    }

    public boolean isLoaded(Duration timeout) {
        return isVisible("recyclerSignatures", timeout)
                || isVisible("emptyState", Duration.ofSeconds(3))
                || isVisible("fabAdd", Duration.ofSeconds(3));
    }

    public boolean isEmpty() {
        return isVisible("emptyState", Duration.ofSeconds(3));
    }

    public int signatureCount() {
        try {
            return driver.findElements(byId("tv_signature_name")).size();
        } catch (WebDriverException e) {
            return 0;
        }
    }

    /** Add FAB (hidden when a signature already exists — one max). */
    public boolean isAddVisible() {
        return isVisible("fabAdd", Duration.ofSeconds(3));
    }

    public boolean tapAdd() {
        try {
            waitVisible("fabAdd").click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean tapEditFirst() {
        var edits = driver.findElements(byId("btn_edit"));
        if (edits.isEmpty()) return false;
        try {
            edits.get(0).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean tapDeleteFirst() {
        var dels = driver.findElements(byId("btn_delete"));
        if (dels.isEmpty()) return false;
        try {
            dels.get(0).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean confirmDelete(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiSelector().textMatches(\"(?i)delete\")")).click();
                return true;
            } catch (WebDriverException ignored) {
            }
            sleep(400);
        }
        return false;
    }

    // ---------------- editor ----------------

    public boolean isEditorLoaded(Duration timeout) {
        return isVisible("editName", timeout) || isVisible("webViewEditor", Duration.ofSeconds(3));
    }

    public void setName(String name) {
        try {
            WebElement f = driver.findElement(byId("editName"));
            f.click();
            f.clear();
            f.sendKeys(name);
            hideKeyboard();
        } catch (WebDriverException ignored) {
        }
    }

    public void setContent(String text) {
        try {
            WebElement body = driver.findElement(By.className("android.webkit.WebView"));
            body.click();
            sleep(400);
            body.sendKeys(text);
        } catch (WebDriverException ignored) {
        }
    }

    public boolean tapSave() {
        try {
            driver.findElement(byId("selectTv")).click();
            return true;
        } catch (WebDriverException ignored) {
        }
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().textMatches(\"(?i)save\")")).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Empty name keeps the editor open (validation error). */
    public boolean isEditorStillOpen() {
        return isVisible("editName", Duration.ofSeconds(2));
    }

    public boolean waitEditorClosed(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (!isVisible("editName", Duration.ofMillis(600))) return true;
        }
        return false;
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
