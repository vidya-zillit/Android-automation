package com.zillit.automation.pages.box;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * BOX — "Distribute To": who an event or note goes out to.
 *
 * <p>Tabs across the top (All Departments, Departments, Users, Presets, Self), a strip of
 * chips for who is currently chosen, and Cancel / Done. Ids from
 * {@code new_box_activity_select_invitees.xml}; each row of {@code rvUsers} is
 * {@code new_box_item_invitee} — a checkbox, a name and a subtitle.
 *
 * <p><b>Saving an event addressed to a real person notifies that person.</b> Everything
 * here can be exercised without that: pick, see the chip, unpick, see it go, then Cancel.
 * Only a save commits, so a case that saves should address nothing but the tester's own
 * account unless the project owner has said otherwise.
 */
public class BoxInviteePage extends BoxSchedulePage {

    public BoxInviteePage(AndroidDriver driver) {
        super(driver);
    }

    public boolean isLoaded(Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        do {
            if (displayed("btnDone") && (displayed("tabUsers") || displayed("tabSelf"))) {
                return true;
            }
            pause(400);
        } while (System.currentTimeMillis() < end);
        return false;
    }

    // ───────────────────────────── tabs ─────────────────────────────

    public boolean openUsers()        { return clickIfPresent("tabUsers"); }
    public boolean openPresets()      { return clickIfPresent("tabPresets"); }
    public boolean openSelf()         { return clickIfPresent("tabSelf"); }
    public boolean openDepartments()  { return clickIfPresent("tabDepartments"); }

    public boolean usersShown()   { return displayed("rvUsers"); }
    public boolean presetsShown() { return displayed("rvPresets"); }

    public boolean usersEmpty()   { return displayed("tvUsersEmpty"); }
    public boolean presetsEmpty() { return displayed("tvPresetsEmpty"); }

    public boolean searchUsers(String term)   { return fill("etSearchUsers", term); }
    public boolean searchPresets(String term) { return fill("etSearchPresets", term); }

    // ───────────────────────────── people ─────────────────────────────

    /** Every user name the Users tab is showing. */
    public List<String> userNames() { return namesIn("rvUsers"); }

    /** Every preset name the Presets tab is showing. */
    public List<String> presetNames() { return namesIn("rvPresets"); }

    /**
     * Ticks or unticks a user by name.
     *
     * <p>The row's checkbox is a sibling of its name, so the row is reached by walking up
     * from the name to the nearest ancestor that holds a checkbox.
     */
    public boolean setUserChecked(String name, boolean checked) {
        try {
            List<WebElement> lists = driver.findElements(byId("rvUsers"));
            if (lists.isEmpty()) return false;
            for (WebElement label : lists.get(0).findElements(byId("tvName"))) {
                String t = label.getText();
                if (t == null || !t.trim().contains(name)) continue;
                WebElement box = label.findElement(AppiumBy.xpath(
                        "./ancestor::*[.//*[contains(@resource-id,'cbSelect')]][1]"
                                + "//*[contains(@resource-id,'cbSelect')]"));
                boolean now = "true".equals(box.getAttribute("checked"));
                if (now == checked) return true;
                box.click();
                pause(900);
                return "true".equals(box.getAttribute("checked")) == checked;
            }
        } catch (Exception ignored) { }
        return false;
    }

    public boolean isUserChecked(String name) {
        try {
            List<WebElement> lists = driver.findElements(byId("rvUsers"));
            if (lists.isEmpty()) return false;
            for (WebElement label : lists.get(0).findElements(byId("tvName"))) {
                String t = label.getText();
                if (t == null || !t.trim().contains(name)) continue;
                WebElement box = label.findElement(AppiumBy.xpath(
                        "./ancestor::*[.//*[contains(@resource-id,'cbSelect')]][1]"
                                + "//*[contains(@resource-id,'cbSelect')]"));
                return "true".equals(box.getAttribute("checked"));
            }
        } catch (Exception ignored) { }
        return false;
    }

    /** Who is chosen right now, read from the chip strip rather than from the rows. */
    public List<String> selectedChips() {
        List<String> out = new ArrayList<>();
        try {
            List<WebElement> groups = driver.findElements(byId("selectedChips"));
            if (groups.isEmpty()) return out;
            // Chips are Buttons, not TextViews, and each carries a textless close icon.
            for (WebElement chip : groups.get(0).findElements(AppiumBy.xpath(".//*"))) {
                String t = chip.getText();
                if (t != null && !t.trim().isEmpty()) out.add(t.trim());
            }
        } catch (Exception ignored) { }
        return out;
    }

    public boolean selectSelf() { return clickIfPresent("btnSelectSelf"); }

    public boolean choosePreset(String name) {
        try {
            List<WebElement> lists = driver.findElements(byId("rvPresets"));
            if (lists.isEmpty()) return false;
            for (WebElement el : lists.get(0).findElements(
                    AppiumBy.className("android.widget.TextView"))) {
                String t = el.getText();
                if (t != null && t.trim().contains(name)) {
                    el.click();
                    pause(1200);
                    return true;
                }
            }
        } catch (Exception ignored) { }
        return false;
    }

    public boolean done()   { return clickIfPresent("btnDone"); }
    public boolean cancel() { return clickIfPresent("btnCancel"); }

    /** What this screen can offer, for a skip message that says why. */
    public String availability() {
        return "usersTab=" + displayed("tabUsers")
                + " usersEmpty=" + usersEmpty()
                + " users=" + userNames()
                + " presetsTab=" + displayed("tabPresets")
                + " presets=" + presetNames();
    }

    private List<String> namesIn(String listId) {
        List<String> out = new ArrayList<>();
        try {
            List<WebElement> lists = driver.findElements(byId(listId));
            if (lists.isEmpty()) return out;
            for (WebElement el : lists.get(0).findElements(byId("tvName"))) {
                String t = el.getText();
                if (t != null && !t.isBlank()) out.add(t.trim());
            }
            if (out.isEmpty()) {
                for (WebElement el : lists.get(0).findElements(
                        AppiumBy.className("android.widget.TextView"))) {
                    String t = el.getText();
                    if (t != null && !t.isBlank()) out.add(t.trim());
                }
            }
        } catch (Exception ignored) { }
        return out;
    }

    private boolean fill(String id, String value) {
        try {
            for (WebElement e : driver.findElements(byId(id))) {
                if (!e.isDisplayed()) continue;
                e.click();
                e.clear();
                if (!value.isEmpty()) e.sendKeys(value);
                hideKeyboard();
                pause(900);
                return true;
            }
            return false;
        } catch (Exception e) { return false; }
    }
}
