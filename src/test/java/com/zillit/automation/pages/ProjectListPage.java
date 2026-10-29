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
        // Tap the field before typing. Without focus the keystrokes can land
        // elsewhere and the list is never filtered, so the caller then picks from
        // the FULL project list believing it picked from search results.
        try {
            field.click();
        } catch (WebDriverException ignored) {
            // Already focused.
        }
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

    /**
     * Opens the project whose name matches EXACTLY (ignoring case and padding).
     *
     * <p>Use this whenever the wrong neighbour would be a real problem.
     * {@link #tapProject} matches on "contains" and takes the first hit, which is
     * unsafe here: a remote shooting unit created from a project carries a name
     * containing the parent's, so searching the parent matches both and the unit
     * — listed first — is the one that opens.
     *
     * @return false when no row matches exactly, so the caller can fail loudly
     *         rather than silently landing somewhere else
     */
    public boolean tapProjectExact(String name) {
        String wanted = name.trim().toLowerCase();
        for (WebElement label : driver.findElements(byId("projectNameText"))) {
            String shown = label.getText();
            if (shown == null || !matchesProject(shown, wanted)) {
                continue;
            }
            try {
                driver.findElement(io.appium.java_client.AppiumBy.xpath(
                        "//*[contains(@resource-id,'projectNameText') and @text=\"" + shown
                                + "\"]/ancestor::*[contains(@resource-id,'projectHolder')]"))
                        .click();
            } catch (WebDriverException e) {
                label.click();
            }
            return true;
        }
        return false;
    }

    /**
     * True when a listed row names this project.
     *
     * <p>Rows carry the viewer's ROLE as a suffix — "ZL TESTING 01 AUGUST - Admin"
     * — while the in-project header shows the bare name, so a plain equality test
     * never matches. Exactly one trailing " - token" is stripped before comparing,
     * which keeps the match tight: a shooting unit listed as
     * "Parent - Unit A - Admin" strips to "Parent - Unit A" and correctly fails to
     * match its parent, which is the confusion that opened the wrong project
     * before.
     */
    private boolean matchesProject(String shown, String wantedLower) {
        String t = shown.trim();
        if (t.equalsIgnoreCase(wantedLower)) {
            return true;
        }
        String stripped = t.replaceFirst("\\s+-\\s+\\S+$", "").trim();
        return stripped.equalsIgnoreCase(wantedLower);
    }

    /** Project names currently listed — useful for diagnosing a failed pick. */
    public java.util.List<String> listedProjectNames() {
        java.util.List<String> out = new java.util.ArrayList<>();
        try {
            for (WebElement label : driver.findElements(byId("projectNameText"))) {
                String t = label.getText();
                if (t != null && !t.isBlank()) {
                    out.add(t.trim());
                }
            }
        } catch (WebDriverException ignored) {
        }
        return out;
    }

    private void pauseForFilter() {
        try {
            Thread.sleep(800); // TextWatcher filtering is instant but re-layout isn't
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
