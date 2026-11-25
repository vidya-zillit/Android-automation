package com.zillit.automation.pages.drive;

import com.zillit.automation.pages.common.BasePage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;

/**
 * The Markdown / plain-text viewer and editor — {@code text.DriveTextFileActivity}
 * (Zillit-Android b5ae04154). Its layout is built in code, so NOTHING has a resource-id:
 *
 * <pre>
 *   title TextView (bold, the file name; "name · version N" for a version)
 *   Button "Edit"   — when the user may edit (and not a version)
 *   Button "Save"   — only while editing; reads "Saving…" during a save
 *   read-only body  — a monospace TextView in a ScrollView
 *   edit body       — one multi-line android.widget.EditText
 *   status          — "Opening file…" / "This file is empty." / an error
 * </pre>
 *
 * Back with unsaved text asks "Discard your changes?" (Discard / Keep editing); Back after a
 * save asks "Let others know you updated this file?" — answered with Skip by
 * {@link DriveEditorPage#skipNotify()}.
 */
public class DriveTextEditorPage extends BasePage {

    public DriveTextEditorPage(AndroidDriver driver) {
        super(driver);
    }

    public boolean isOpen() {
        try {
            String a = driver.currentActivity();
            return a != null && a.contains("DriveTextFileActivity");
        } catch (WebDriverException e) { return false; }
    }

    public boolean waitOpen(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (isOpen()) return true;
            sleep(500);
        }
        return false;
    }

    /** Waits for "Opening file…" to go. */
    public boolean waitLoaded(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (isOpen() && !hasText("Opening file…")) return true;
            sleep(600);
        }
        return false;
    }

    public boolean hasEditButton() { return hasButton("Edit"); }
    public boolean hasSaveButton() { return hasButton("Save") || hasButton("Saving…"); }
    public boolean tapEdit()       { return tapButton("Edit"); }
    public boolean tapSave()       { return tapButton("Save"); }

    public boolean isEditing() { return editor() != null; }

    private WebElement editor() {
        try {
            List<WebElement> els = driver.findElements(By.className("android.widget.EditText"));
            for (WebElement e : els) if (e.isDisplayed() && e.getRect().getHeight() > 200) return e;
        } catch (WebDriverException ignored) { }
        return null;
    }

    public String editorText() {
        WebElement e = editor();
        return e == null ? "" : e.getText();
    }

    /** Replaces the editing box's text (a native EditText, so sendKeys works). */
    public boolean setText(String text) {
        WebElement e = editor();
        if (e == null) return false;
        try { e.clear(); e.sendKeys(text); hideKeyboardIfShown(); return true; }
        catch (WebDriverException ex) { return false; }
    }

    /** Appends to what is there. */
    public boolean append(String text) {
        String now = editorText();
        return setText((now == null ? "" : now) + text);
    }

    /** The first TextView — the bold file-name title. */
    public String title() {
        try {
            for (WebElement t : driver.findElements(By.className("android.widget.TextView"))) {
                String s = t.getText();
                if (s != null && !s.isBlank()) return s;
            }
        } catch (WebDriverException ignored) { }
        return "";
    }

    /** Every visible text — the read-only body is one of them. */
    public boolean bodyContains(String text) { return hasTextContaining(text); }

    public boolean discardDialogShown() { return hasText("Discard your changes?"); }
    public boolean discard()            { return tapButton("Discard"); }
    public boolean keepEditing()        { return tapButton("Keep editing"); }

    /** A toast ("Saved" / "No changes to save") — UiAutomator2 exposes toasts by xpath. */
    public boolean toastShown(String text, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        String esc = text.replace("&", "&amp;").replace("'", "&apos;").replace("\"", "&quot;");
        while (System.currentTimeMillis() < deadline) {
            // A toast's element goes stale before getText() can read it; the page source holds
            // its text while it is up (<android.widget.Toast text="…">).
            try {
                String src = driver.getPageSource();
                if (src.contains("android.widget.Toast") && (src.contains(text) || src.contains(esc))) return true;
            } catch (WebDriverException ignored) { }
            sleep(200);
        }
        return false;
    }


    // ---------------- helpers ----------------

    private boolean hasText(String text) {
        try {
            return !driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().text(\"" + text + "\")")).isEmpty();
        } catch (WebDriverException e) { return false; }
    }

    private boolean hasTextContaining(String text) {
        try {
            return !driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().textContains(\"" + text + "\")")).isEmpty();
        } catch (WebDriverException e) { return false; }
    }

    private boolean hasButton(String text) {
        try {
            return !driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().className(\"android.widget.Button\").text(\"" + text + "\")")).isEmpty();
        } catch (WebDriverException e) { return false; }
    }

    private boolean tapButton(String text) {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().className(\"android.widget.Button\").text(\"" + text + "\")")).click();
            sleep(1000);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
