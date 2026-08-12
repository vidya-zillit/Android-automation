package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;

/**
 * DOCUMENT DISTRIBUTION — the DOCUMENT PICKER ({@code documentPickerFragment}), reached
 * from the composer's "attach from library".
 *
 * <p>Identity: {@code dd_footer_count} — this screen's unique id. {@code dd_recycler}
 * is shared by nine screens (trap 3.3) and its rows here even reuse {@code dd_name} /
 * {@code dd_check} from the library rows, so NOTHING row-level identifies the picker.
 *
 * <p>The footer count reads "N selected" and the Attach button is DISABLED at zero
 * selection ({@code ddAttach.isEnabled = selectedCount > 0}) — which makes E-8
 * ("attaching nothing is a no-op") assertable on the button alone.
 */
public class DocDistPickerPage extends BasePage {

    public DocDistPickerPage(AndroidDriver driver) {
        super(driver);
    }

    public boolean isOpen(Duration timeout) {
        return isVisible("dd_footer_count", timeout);
    }

    public boolean hasScopeChips() { return isVisible("dd_scope_chips", Duration.ofSeconds(3)); }

    // ---------------- list ----------------

    public List<String> fileNames() {
        try {
            return driver.findElements(byId("dd_name")).stream()
                    .map(WebElement::getText).filter(t -> t != null && !t.isBlank()).toList();
        } catch (WebDriverException e) { return List.of(); }
    }

    public boolean isEmptyShown() { return isVisible("dd_empty", Duration.ofSeconds(3)); }

    /** Ticks the row containing this name (dd_check on the picker row). */
    public boolean selectFile(String name) {
        try {
            driver.findElement(AppiumBy.xpath(
                    "//*[@resource-id='" + appPackage() + ":id/dd_row']"
                            + "[.//*[contains(@text,'" + name + "')]]"
                            + "//*[@resource-id='" + appPackage() + ":id/dd_check']")).click();
            sleep(300);
            return true;
        } catch (WebDriverException e) {
            // Some builds make the whole row the toggle.
            try {
                driver.findElement(AppiumBy.xpath(
                        "//*[@resource-id='" + appPackage() + ":id/dd_row']"
                                + "[.//*[contains(@text,'" + name + "')]]")).click();
                sleep(300);
                return true;
            } catch (WebDriverException e2) { return false; }
        }
    }

    public boolean search(String text) {
        try {
            WebElement f = driver.findElement(byId("dd_search_input"));
            f.click(); f.clear();
            if (!text.isEmpty()) f.sendKeys(text);
            hideKeyboardIfShown();
            sleep(500);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    // ---------------- footer ----------------

    public boolean tapSelectAll() { return click("dd_select_all"); }
    public boolean tapClear()     { return click("dd_clear"); }
    public boolean tapAttach()    { return click("dd_attach"); }

    /** "N selected" from dd_footer_count. */
    public String footerCount() { return textOf("dd_footer_count"); }

    /** The N out of "N selected" — -1 when unreadable. */
    public int selectedCount() {
        String t = footerCount();
        if (t == null) return -1;
        var m = java.util.regex.Pattern.compile("(\\d+)").matcher(t);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    public boolean isAttachEnabled() {
        try { return driver.findElement(byId("dd_attach")).isEnabled(); }
        catch (WebDriverException e) { return false; }
    }

    // ---------------- helpers ----------------

    private String textOf(String id) {
        try { return driver.findElement(byId(id)).getText(); }
        catch (WebDriverException e) { return ""; }
    }

    private boolean click(String id) {
        try { driver.findElement(byId(id)).click(); sleep(350); return true; }
        catch (WebDriverException e) { return false; }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
