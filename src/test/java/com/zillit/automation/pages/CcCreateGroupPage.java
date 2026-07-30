package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;

/**
 * C&C Create / Edit Group (CreateGroupPage, activity_create_group_page.xml).
 * Verified live: gpName → inner editTextInput (name), selectMember (→ member
 * multi-select picker), rvVw (selected members), submit → inner buttonBtn.
 * Validation: name required (group_name_required), 3–40 chars
 * (group_name_validate), ≥1 member (group_members_are_required).
 */
public class CcCreateGroupPage extends BasePage {

    public CcCreateGroupPage(AndroidDriver driver) {
        super(driver);
    }

    public boolean isLoaded(Duration timeout) {
        return isVisible("gpName", timeout)
                || isVisible("submit", Duration.ofSeconds(3))
                || textShown("Create Group", Duration.ofSeconds(3));
    }

    public boolean isEditMode() {
        return textShown("Edit Group", Duration.ofSeconds(2))
                || isVisible("changeImage", Duration.ofSeconds(2)); // avatar only in edit
    }

    public void setName(String name) {
        try {
            WebElement f = driver.findElement(byId("gpName"))
                    .findElement(By.className("android.widget.EditText"));
            f.click();
            f.clear();
            if (!name.isEmpty()) f.sendKeys(name);
            hideKeyboard();
        } catch (WebDriverException e) {
            try {
                WebElement f = driver.findElement(byId("editTextInput"));
                f.click();
                f.clear();
                if (!name.isEmpty()) f.sendKeys(name);
                hideKeyboard();
            } catch (WebDriverException ignored) {
            }
        }
    }

    public String nameText() {
        try {
            return driver.findElement(byId("editTextInput")).getText();
        } catch (WebDriverException e) {
            return "";
        }
    }

    /** Opens the member multi-select picker. */
    public boolean tapSelectMember() {
        try {
            waitVisible("selectMember").click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** The member picker (SearchUsersPage-style select page) is open. */
    public boolean isMemberPickerOpen(Duration timeout) {
        return textShown("SELECT MEMBER", timeout) || isVisible("recyclerView", timeout)
                || isVisible("item_view", Duration.ofSeconds(2));
    }

    /**
     * Selects the first N members in the picker. Rows are `item_view` (tapping
     * toggles a tickview); confirmed by the bottom "Submit" (buttonBtn) — a
     * back-press DISCARDS the selection (verified live).
     */
    public int selectMembers(int n) {
        int selected = 0;
        var rows = driver.findElements(byId("item_view"));
        int count = Math.min(n, rows.size());
        for (int i = 0; i < count; i++) {
            try { rows.get(i).click(); selected++; sleep(400); } catch (WebDriverException ignored) {}
        }
        confirmMemberPicker();
        return selected;
    }

    /** Confirms the member picker via its bottom "Submit" (buttonBtn). */
    public void confirmMemberPicker() {
        try {
            driver.findElement(byId("buttonBtn")).click();
            sleep(1200);
            return;
        } catch (WebDriverException ignored) {
        }
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().textMatches(\"(?i)(submit|done|add)\")")).click();
            sleep(1000);
        } catch (WebDriverException ignored) {
        }
    }

    public int selectedMemberCount() {
        try {
            return driver.findElement(byId("rvVw"))
                    .findElements(By.className("android.view.ViewGroup")).size();
        } catch (WebDriverException e) {
            return 0;
        }
    }

    public boolean tapSubmit() {
        try {
            driver.findElement(byId("submit"))
                    .findElement(byId("buttonBtn")).click();
            return true;
        } catch (WebDriverException e) {
            try { driver.findElement(byId("buttonBtn")).click(); return true; }
            catch (WebDriverException e2) { return false; }
        }
    }

    /** Invalid name/no-members keep the form open (validation blocks save). */
    public boolean isStillOpen() {
        return isVisible("gpName", Duration.ofSeconds(2)) || isVisible("submit", Duration.ofSeconds(2));
    }

    public boolean waitClosed(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (!isVisible("submit", Duration.ofMillis(700))) return true;
        }
        return false;
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
