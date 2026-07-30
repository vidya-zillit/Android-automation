package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;

/**
 * C&C Group Info / User Profile (ProfileShowingActivity, activity_profile_showing_page.xml).
 * Verified live: membersRv (member list), totalMembersCount, editGroup (admin →
 * CreateGroupPage; the ONLY way to ADD members), changeImage (admin photo),
 * deleteOrLeave ("Delete Group" for admin), leaveButton (non-sole-admin),
 * audioCall/videoCall. Member row (rv_member_view): userNameTv, designationTv,
 * adminHeader. Member LONG-PRESS (admin) → remove / make_group_admin /
 * diable_from_admin. Delete/leave → confirm dialogs.
 */
public class CcGroupInfoPage extends BasePage {

    public CcGroupInfoPage(AndroidDriver driver) {
        super(driver);
    }

    public boolean isLoaded(Duration timeout) {
        return isVisible("membersRv", timeout)
                || isVisible("totalMembersCount", Duration.ofSeconds(3))
                || isVisible("deleteOrLeave", Duration.ofSeconds(3));
    }

    public String membersCountText() {
        try {
            return driver.findElement(byId("totalMembersCount")).getText();
        } catch (WebDriverException e) {
            return "";
        }
    }

    public int memberRowCount() {
        try {
            return driver.findElement(byId("membersRv")).findElements(byId("userNameTv")).size();
        } catch (WebDriverException e) {
            return driver.findElements(byId("userNameTv")).size();
        }
    }

    public List<String> memberNames() {
        try {
            return driver.findElements(byId("userNameTv")).stream()
                    .map(WebElement::getText).filter(t -> t != null && !t.isBlank()).toList();
        } catch (WebDriverException e) {
            return List.of();
        }
    }

    // ---------------- ADD member (via Edit Group) ----------------

    public boolean isEditVisible() {   // admin only
        return isVisible("editGroup", Duration.ofSeconds(4));
    }

    /** Opens Edit Group (CreateGroupPage in edit mode — where members are added). */
    public CcCreateGroupPage tapEditGroup() {
        waitVisible("editGroup").click();
        return new CcCreateGroupPage(driver);
    }

    // ---------------- REMOVE member / admin toggle (member long-press) ----------------

    /** Long-presses a member row (by index, skipping yourself at 0 usually). */
    public boolean longPressMember(int index) {
        try {
            var rows = driver.findElements(byId("userNameTv"));
            if (index >= rows.size()) return false;
            var r = rows.get(index).getRect();
            w3cLongPress(r.getX() + r.getWidth() / 2, r.getY() + r.getHeight() / 2, 1000);
            sleep(700);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** After a member long-press, the option dropdown shows Remove / make admin / etc. */
    public boolean isMemberMenuOpen(Duration timeout) {
        return textShown("Remove", timeout) || textShown("admin", timeout);
    }

    public boolean tapMemberOption(String label) {   // Remove / Make Group Admin / Disable from Admin
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().textMatches(\"(?i).*" + label + ".*\")")).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    // ---------------- DELETE group / LEAVE ----------------

    public boolean isDeleteGroupVisible() {   // admin
        return isVisible("deleteOrLeave", Duration.ofSeconds(4))
                && (textShown("Delete", Duration.ofSeconds(2)));
    }

    public boolean tapDeleteOrLeave() {
        try { driver.findElement(byId("deleteOrLeave")).click(); return true; }
        catch (WebDriverException e) { return false; }
    }

    public boolean tapLeave() {
        try { driver.findElement(byId("leaveButton")).click(); return true; }
        catch (WebDriverException e) { return false; }
    }

    /** Confirms an "Are you sure…" dialog (delete group / remove user / leave). */
    public boolean confirmDialog(Duration timeout, String... labels) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            for (String l : labels) {
                try {
                    driver.findElement(AppiumBy.androidUIAutomator(
                            "new UiSelector().textMatches(\"(?i)" + l + "\")")).click();
                    return true;
                } catch (WebDriverException ignored) {
                }
            }
            sleep(300);
        }
        return false;
    }

    public boolean isConfirmShown(Duration timeout) {
        return textShown("sure", timeout);
    }

    public void back() {
        try { driver.navigate().back(); } catch (WebDriverException ignored) {}
    }

    // ---------------- helpers ----------------

    private void w3cLongPress(int x, int y, int holdMs) {
        var finger = new org.openqa.selenium.interactions.PointerInput(
                org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        var seq = new org.openqa.selenium.interactions.Sequence(finger, 1);
        seq.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, y));
        seq.addAction(finger.createPointerDown(0));
        seq.addAction(finger.createPointerMove(Duration.ofMillis(holdMs),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, y));
        seq.addAction(finger.createPointerUp(0));
        driver.perform(java.util.Collections.singletonList(seq));
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
