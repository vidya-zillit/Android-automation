package com.zillit.automation.pages;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/** Project chooser shown to logged-in users (ProjectListActivity). */
public class ProjectListPage extends BasePage {

    public ProjectListPage(AndroidDriver driver) {
        super(driver);
    }

    public boolean isLoaded() {
        return waitVisible("projectListRecyclerView").isDisplayed();
    }

    public List<String> visibleProjectNames() {
        try {
            return driver.findElements(byId("projectNameText")).stream()
                    .map(WebElement::getText)
                    .collect(Collectors.toList());
        } catch (WebDriverException e) {
            return List.of();
        }
    }

    public void search(String text) {
        WebElement field = waitVisible("search_widget_edit_textvw");
        field.clear();
        if (!text.isEmpty()) {
            field.sendKeys(text);
        }
        hideKeyboard();
        pauseForFilter();
    }

    public void clearSearch() {
        search("");
    }

    public boolean isSearchVisible() {
        return isVisible("search_widget_edit_textvw", Duration.ofSeconds(5));
    }

    public boolean isStartProjectVisible() {
        return isVisible("start_project_btn", Duration.ofSeconds(5));
    }

    public boolean isJoinProjectVisible() {
        return isVisible("join_project_btn", Duration.ofSeconds(5));
    }

    public boolean isEmptyStateShown() {
        return isVisible("noDataFound", Duration.ofSeconds(5))
                || visibleProjectNames().isEmpty();
    }

    /** Taps the row of the first project whose name contains the given text. */
    public boolean tapProject(String nameContains) {
        for (WebElement label : driver.findElements(byId("projectNameText"))) {
            String name = label.getText();
            if (name != null && name.toLowerCase().contains(nameContains.toLowerCase())) {
                try {
                    // Click the whole row — the click listener lives on the
                    // holder, not the text label.
                    driver.findElement(io.appium.java_client.AppiumBy.xpath(
                            "//*[contains(@resource-id,'projectNameText') and contains(@text,\""
                                    + name + "\")]/ancestor::*[contains(@resource-id,'projectHolder')]"))
                            .click();
                } catch (WebDriverException e) {
                    label.click(); // fallback: tap the label itself
                }
                return true;
            }
        }
        return false;
    }

    private void pauseForFilter() {
        try {
            Thread.sleep(800); // TextWatcher filtering is instant but re-layout isn't
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
