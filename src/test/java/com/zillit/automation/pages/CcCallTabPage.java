package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;

/**
 * C&C → Call tab (CallFragment, fragment_call.xml). Sub-tabs Recent / Missed
 * (tlTab + viewPager, each a RecentCallFragment); per-tab search
 * (search_widget_edit_textvw), clear-all (deleteIv), call-log list (recMembers)
 * with rows call_member_item.xml (userNameTv, timeVw, ivCallType =
 * missed/outgoing/incoming, callIv call-back, ivFav, infoIcon); new-call FAB
 * createRandomCall.
 */
public class CcCallTabPage extends BasePage {

    public CcCallTabPage(AndroidDriver driver) {
        super(driver);
    }

    public boolean isLoaded(Duration timeout) {
        return isVisible("tlTab", timeout)
                || isVisible("recMembers", Duration.ofSeconds(3))
                || textShown("Recent", Duration.ofSeconds(3));
    }

    /** Switch to a sub-tab by label: "Recent" / "Missed". */
    public boolean tapSubTab(String label) {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().textMatches(\"(?i)" + label + "\")")).click();
            sleep(1200);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean isSubTabShown(String label) {
        return textShown(label, Duration.ofSeconds(4));
    }

    /** Number of call-log rows currently listed. */
    public int logRowCount() {
        try {
            return driver.findElement(byId("recMembers")).findElements(byId("userNameTv")).size();
        } catch (WebDriverException e) {
            try { return driver.findElements(byId("userNameTv")).size(); }
            catch (WebDriverException e2) { return 0; }
        }
    }

    public List<String> logNames() {
        try {
            return driver.findElements(byId("userNameTv")).stream()
                    .map(WebElement::getText).filter(t -> t != null && !t.isBlank()).toList();
        } catch (WebDriverException e) {
            return List.of();
        }
    }

    /** A call-type indicator icon is present on the rows (missed/outgoing/incoming). */
    public boolean hasCallTypeIcons() {
        try { return !driver.findElements(byId("ivCallType")).isEmpty(); }
        catch (WebDriverException e) { return false; }
    }

    public boolean isSearchVisible() {
        return isVisible("search_widget_edit_textvw", Duration.ofSeconds(4));
    }

    public void search(String text) {
        if (!isVisible("search_widget_edit_textvw", Duration.ofSeconds(5))) return;
        try {
            WebElement f = driver.findElement(byId("search_widget_edit_textvw"));
            f.click();
            f.clear();
            f.sendKeys(text);
            sleep(1200);
        } catch (WebDriverException ignored) {
        }
    }

    public boolean isNewCallFabVisible() {
        return isVisible("createRandomCall", Duration.ofSeconds(4));
    }

    public boolean tapNewCallFab() {
        try { waitVisible("createRandomCall").click(); return true; }
        catch (WebDriverException e) { return false; }
    }

    public boolean isClearLogVisible() {
        return isVisible("deleteIv", Duration.ofSeconds(3));
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
