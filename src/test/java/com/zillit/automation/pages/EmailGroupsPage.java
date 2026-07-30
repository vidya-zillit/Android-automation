package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;

/**
 * Email Groups list (EmailGroupsActivity, fragment_email_groups.xml): toolbar,
 * recyclerGroups, emptyState, fabAdd; row item_email_group → tv_group_name,
 * btn_edit, btn_delete, tv_member_count.
 *
 * Add/Edit group (EditEmailGroupActivity, fragment_edit_email_group.xml):
 * editName, selectMember, rvMembers, tv_no_members; toolbar "Save" = selectTv.
 */
public class EmailGroupsPage extends BasePage {

    public EmailGroupsPage(AndroidDriver driver) {
        super(driver);
    }

    public boolean isLoaded(Duration timeout) {
        return isVisible("fabAdd", timeout)
                || isVisible("recyclerGroups", Duration.ofSeconds(3))
                || isVisible("emptyState", Duration.ofSeconds(3));
    }

    public int groupCount() {
        try {
            return driver.findElements(byId("tv_group_name")).size();
        } catch (WebDriverException e) {
            return 0;
        }
    }

    public boolean tapAdd() {
        try {
            waitVisible("fabAdd").click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean isGroupShown(String name, Duration timeout) {
        return textShown(name, timeout);
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

    public boolean tapDeleteGroup(String name) {
        // Delete button lives in the same row as the group name.
        try {
            var dels = driver.findElements(byId("btn_delete"));
            if (dels.isEmpty()) return false;
            dels.get(0).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean confirmDeleteGroup(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            for (String pat : new String[]{"delete", "yes", "ok", "confirm"}) {
                try {
                    driver.findElement(AppiumBy.androidUIAutomator(
                            "new UiSelector().textMatches(\"(?i)" + pat + "\")")).click();
                    return true;
                } catch (WebDriverException ignored) {
                }
            }
            sleep(400);
        }
        return false;
    }

    // ---------------- Add / Edit group form ----------------

    public boolean isEditFormLoaded(Duration timeout) {
        return isVisible("editName", timeout) || isVisible("selectMember", Duration.ofSeconds(3));
    }

    public void setGroupName(String name) {
        try {
            WebElement f = driver.findElement(byId("editName"))
                    .findElement(org.openqa.selenium.By.className("android.widget.EditText"));
            f.click();
            f.clear();
            f.sendKeys(name);
        } catch (WebDriverException e) {
            try {
                WebElement f = driver.findElement(byId("editName"));
                f.click();
                f.sendKeys(name);
            } catch (WebDriverException ignored) {
            }
        }
        hideKeyboard();
    }

    public boolean tapSelectMember() {
        try {
            waitVisible("selectMember").click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public int selectedMemberCount() {
        try {
            return driver.findElement(byId("rvMembers"))
                    .findElements(org.openqa.selenium.By.className("android.view.ViewGroup")).size();
        } catch (WebDriverException e) {
            return 0;
        }
    }

    /** Taps the toolbar "Save" (selectTv). */
    public boolean tapSave() {
        for (String id : new String[]{"selectTv"}) {
            try {
                driver.findElement(byId(id)).click();
                return true;
            } catch (WebDriverException ignored) {
            }
        }
        // Fallback: a "Save" text in the toolbar.
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().textMatches(\"(?i)save\")")).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Empty name / no members keep the form open (save blocked). */
    public boolean isEditFormStillOpen() {
        return isVisible("editName", Duration.ofSeconds(2))
                || isVisible("selectMember", Duration.ofSeconds(2));
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
