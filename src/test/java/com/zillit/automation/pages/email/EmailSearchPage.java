package com.zillit.automation.pages.email;

import com.zillit.automation.pages.common.BasePage;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;

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

    /** Checked state of a field chip (chip_subject / chip_from / chip_to / chip_cc / chip_bcc). */
    public Boolean isFieldChipChecked(String id) {
        try {
            return Boolean.parseBoolean(driver.findElement(byId(id)).getAttribute("checked"));
        } catch (WebDriverException e) {
            return null;
        }
    }

    /** Sets a field chip on/off (tapping toggles it); true when it ends in the wanted state. */
    public boolean setFieldChip(String id, boolean on) {
        Boolean now = isFieldChipChecked(id);
        if (now == null) return false;
        if (now != on) {
            tapFieldChip(id);
            sleep(400);
        }
        Boolean after = isFieldChipChecked(id);
        return after != null && after == on;
    }

    /**
     * Waits until a search has finished: either the results container
     * (tv_result_count) or the no-results view is shown and the spinner is gone.
     */
    public boolean waitSearchSettled(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            boolean searching = isVisible("progress_search", Duration.ofMillis(300));
            if (!searching && (isVisible("tv_result_count", Duration.ofMillis(500)) || isNoResults(Duration.ofMillis(500)))) {
                return true;
            }
            sleep(500);
        }
        return false;
    }

    /** Subjects of the result cards (rv_results → tv_subject); empty when none. */
    public List<String> resultSubjects() {
        try {
            return driver.findElement(byId("rv_results")).findElements(byId("tv_subject")).stream()
                    .map(WebElement::getText).map(t -> t == null ? "" : t).toList();
        } catch (WebDriverException e) {
            return List.of();
        }
    }

    public String resultCountText() {
        try {
            return driver.findElement(byId("tv_result_count")).getText();
        } catch (WebDriverException e) {
            return "";
        }
    }

    public boolean isNoResults(Duration timeout) {
        return isVisible("empty_no_results", timeout);
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

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
