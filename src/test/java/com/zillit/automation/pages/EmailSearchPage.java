package com.zillit.automation.pages;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;

/**
 * Email search (SearchActivity, fragment_search.xml): et_search, btn_clear,
 * btn_filter, btn_back; field chips chip_subject/from/to/cc/bcc; status chips
 * chip_status_all/read/unread; switch_attachments; results rv_results,
 * tv_result_count, empty_initial, empty_no_results.
 */
public class EmailSearchPage extends BasePage {

    public EmailSearchPage(AndroidDriver driver) {
        super(driver);
    }

    public boolean isLoaded(Duration timeout) {
        return isVisible("et_search", timeout);
    }

    public void type(String query) {
        WebElement f = waitVisible("et_search");
        f.click();
        f.clear();
        f.sendKeys(query);
    }

    public boolean clear() {
        try {
            waitVisible("btn_clear").click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean toggleFilters() {
        try {
            waitVisible("btn_filter").click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean tapFieldChip(String id) {   // chip_subject / chip_from / chip_to / chip_cc / chip_bcc
        try {
            driver.findElement(byId(id)).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean areFieldChipsShown() {
        return isVisible("chip_subject", Duration.ofSeconds(3))
                || isVisible("chip_from", Duration.ofSeconds(2));
    }

    public boolean hasResults(Duration timeout) {
        return isVisible("rv_results", timeout) || isVisible("tv_result_count", timeout);
    }

    public boolean isNoResults() {
        return isVisible("empty_no_results", Duration.ofSeconds(3));
    }

    public boolean isInitialState() {
        return isVisible("empty_initial", Duration.ofSeconds(3));
    }

    public void back() {
        try {
            waitVisible("btn_back").click();
        } catch (WebDriverException e) {
            driver.navigate().back();
        }
    }
}
