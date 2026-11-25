package com.zillit.automation.pages.cc;

import com.zillit.automation.pages.common.BasePage;
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

    /**
     * Picks the first member in the picker and confirms it; returns that member's
     * name (itemtext), or "" when the picker is empty. The edit-mode picker only
     * lists users NOT already in the group (CreateGroupPage.selectMemberPage), so
     * this always adds someone new.
     */
    public String selectFirstMember() {
        try {
            var rows = driver.findElements(byId("item_view"));
            if (rows.isEmpty()) return "";
            WebElement row = rows.get(0);
            String name;
            try { name = row.findElement(byId("itemtext")).getText(); }
            catch (WebDriverException e) { name = ""; }
            row.click();
            sleep(400);
            confirmMemberPicker();
            return name == null ? "" : name.trim();
        } catch (WebDriverException e) {
            return "";
        }
    }

    /**
     * Ticks the picker row whose name contains {@code name} (scrolling to it) and confirms.
     * Returns false — after backing out of the picker — when that user is not listed (for
     * example already a member: the edit-mode picker hides existing members).
     */
    public boolean selectMemberNamed(String name) {
        try {
            WebElement label;
            try {
                label = driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiScrollable(new UiSelector().scrollable(true))"
                                + ".scrollIntoView(new UiSelector().resourceIdMatches(\".*:id/itemtext\").textContains(\""
                                + name + "\"))"));
            } catch (WebDriverException notScrollable) {
                label = driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiSelector().resourceIdMatches(\".*:id/itemtext\").textContains(\"" + name + "\")"));
            }
            label.click();
            sleep(500);
            confirmMemberPicker();
            return true;
        } catch (WebDriverException e) {
            try { driver.navigate().back(); } catch (WebDriverException ignored) {}
            sleep(800);
            return false;
        }
    }

    /** Types the name as-is and leaves the keyboard up (the length filter acts on input). */
    public void typeNameRaw(String name) {
        try {
            WebElement f = driver.findElement(byId("gpName"))
                    .findElement(By.className("android.widget.EditText"));
            f.click();
            f.clear();
            f.sendKeys(name);
            sleep(600);
        } catch (WebDriverException ignored) {
        }
    }

    /**
     * Anything on the form that tells the user about the name length: a TextInputLayout
     * counter / error / helper line, or any visible text mentioning "40". Returns the text
     * found, or "" when nothing is shown.
     */
    public String lengthHintShown() {
        try {
            var els = driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceIdMatches(\".*:id/textinput_(counter|error|helper_text|suffix_text)\")"));
            for (WebElement e : els) {
                String t = e.getText();
                if (t != null && !t.isBlank()) return t;
            }
            var forty = driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().textContains(\"40\")"));
            for (WebElement e : forty) {
                String t = e.getText();
                if (t != null && !t.contains("ZL AUTO")) return t;
            }
        } catch (WebDriverException ignored) {
        }
        return "";
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

    /** Names shown in the form's selected-members list (rvVw). */
    public java.util.List<String> selectedMemberNames() {
        try {
            return driver.findElement(byId("rvVw")).findElements(By.className("android.widget.TextView")).stream()
                    .map(WebElement::getText).filter(t -> t != null && !t.isBlank()).toList();
        } catch (WebDriverException e) {
            return java.util.List.of();
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
