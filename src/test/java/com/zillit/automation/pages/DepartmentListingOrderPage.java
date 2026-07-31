package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Settings → Admin Settings → "Change Department Listing Order for Crew List"
 * (DepartmentListPage). Each row: position number (tvPriority), name
 * (tv_department_name), "Update" (tvChange — opens a priority bottom sheet),
 * chevron (imgNext), inside a cardView row container.
 *
 * The priority popup does NOT close on the hardware back button (must use its
 * own X / btn_cancel), and leaving this screen with a pending change can show
 * a separate "Do you want to save changes?" (No / Yes) confirmation.
 */
public class DepartmentListingOrderPage extends BasePage {

    public DepartmentListingOrderPage(AndroidDriver driver) {
        super(driver);
    }

    /** Same menuText-in-bottomNavigation idiom as ToolsPage.openToolsTab(), for the Settings tab. */
    public boolean openSettingsTab() {
        try {
            WebElement navBar = driver.findElement(byId("bottomNavigation"));
            for (WebElement it : navBar.findElements(byId("menuText"))) {
                String t = it.getText();
                if (t != null && t.equalsIgnoreCase("Settings")) {
                    it.click();
                    return true;
                }
            }
        } catch (WebDriverException ignored) {
        }
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().textMatches(\"(?i)Settings\")")).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean openAdminSettings() {
        return tapListRow("Admin Settings");
    }

    public boolean openChangeDepartmentListingOrder() {
        return tapListRow("Change Department Listing Order for Crew List");
    }

    public boolean openCreateNewDepartment() {
        return tapListRow("Create New Department");
    }

    /**
     * Top-left back arrow — shared "backBtn" id on both this screen and
     * Create New Department.
     */
    public boolean tapBackButton() {
        try {
            driver.findElement(byId("backBtn")).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /**
     * Ordered department names in the reference "DEPARTMENTS" list on the
     * Create New Department screen (memberNameTv rows) — plain names, no
     * priority numbers; "position" here just means list order.
     */
    public List<String> referenceDepartmentNames() {
        List<String> out = new ArrayList<>();
        for (WebElement e : driver.findElements(byId("memberNameTv"))) {
            try {
                out.add(e.getText());
            } catch (WebDriverException ignored) {
            }
        }
        return out;
    }

    /**
     * Same ancestor-row xpath idiom already used by ProjectListPage.tapProject(),
     * with a bounded wait — this is called right after an Activity transition
     * (Settings, Admin Settings), and an instant lookup can fire before that
     * screen's RecyclerView has finished rendering.
     */
    private boolean tapListRow(String label) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath(
                            "//*[contains(@resource-id,'nameTv') and contains(@text,\"" + label
                                    + "\")]/ancestor::*[contains(@resource-id,'commonListItemHolder')]")))
                    .click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean isLoaded() {
        return isVisible("departmentRecyclerView", Duration.ofSeconds(15));
    }

    public void search(String text) {
        WebElement field = waitVisible("search_widget_edit_textvw");
        field.clear();
        if (!text.isEmpty()) {
            field.sendKeys(text);
        }
        hideKeyboard();
    }

    public void clearSearch() {
        search("");
    }

    /** Ordered (top to bottom) department names currently rendered. */
    public List<String> visibleDepartmentNames() {
        List<String> out = new ArrayList<>();
        for (WebElement e : driver.findElements(byId("tv_department_name"))) {
            try {
                out.add(e.getText());
            } catch (WebDriverException ignored) {
            }
        }
        return out;
    }

    /** 1-based position of a department, or -1 if not currently visible. */
    public int positionOf(String departmentName) {
        int idx = visibleDepartmentNames().indexOf(departmentName);
        return idx < 0 ? -1 : idx + 1;
    }

    /** Opens the priority-entry popup for a department row (its "Update" action). */
    public boolean tapUpdateFor(String departmentNameContains) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(8))
                    .until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath(
                            "//*[contains(@resource-id,'tv_department_name') and contains(@text,\"" + departmentNameContains
                                    + "\")]/ancestor::*[contains(@resource-id,'cardView')]//*[contains(@resource-id,'tvChange')]")))
                    .click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean isUpdatePopupOpen() {
        return isVisible("editTextWidget", Duration.ofSeconds(5));
    }

    /** Sets the priority value in the open popup (clears first). */
    public void setPriority(String value) {
        typeInto("editTextWidget", value);
    }

    public void tapPopupSave() {
        driver.findElement(byId("submit_btn")).click();
    }

    /** The popup ignores the hardware back button — close it via its explicit X. */
    public void tapPopupClose() {
        driver.findElement(byId("btn_cancel")).click();
    }

    /** Main screen Save — persists priority updates and/or a drag reorder. */
    public void tapMainSave() {
        driver.findElement(byId("buttonBtn")).click();
    }

    /** True once Save has redirected back to the Admin Settings list. */
    public boolean isOnAdminSettings(Duration timeout) {
        return textShown("ADMIN SETTINGS", timeout);
    }

    public boolean tapReset() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(8))
                    .until(ExpectedConditions.elementToBeClickable(
                            AppiumBy.androidUIAutomator("new UiSelector().textMatches(\"(?i)reset\")")))
                    .click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /**
     * Confirms a Yes/No-style dialog (Reset confirmation, or the "Do you want to
     * save changes?" prompt on leaving this screen) if one appears within
     * {@code timeout}. A safe no-op when none shows up.
     */
    public boolean confirmYesIfShown(Duration timeout) {
        try {
            new WebDriverWait(driver, timeout)
                    .until(ExpectedConditions.elementToBeClickable(
                            AppiumBy.androidUIAutomator("new UiSelector().textMatches(\"(?i)yes\")")))
                    .click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /**
     * Finds the department, holds it, and drags it — in ONE unbroken gesture,
     * never releasing until it reaches {@code targetPosition} (1-based) — up
     * through every intermediate row along the way. Real RecyclerView drag
     * reorder tracks the finger continuously as it passes over each row, so a
     * release-and-regrab per row (the previous approach here) doesn't reorder
     * the same way a single continuous hold-and-drag does.
     */
    public boolean dragDepartmentToPosition(String departmentName, int targetPosition) {
        List<String> order = visibleDepartmentNames();
        int idx = order.indexOf(departmentName); // 0-based
        if (idx < 0) {
            return false;
        }
        if (idx == targetPosition - 1) {
            return true;
        }
        try {
            Rectangle source = plainRowBounds(departmentName);
            int cx = source.x + source.width / 2;
            int startY = source.y + source.height / 2;

            // A single continuous drag through several rows consistently lands one
            // row PAST the row centered under the drop point (observed: aiming at
            // position 2's center landed at position 3) — so aim at the row one
            // earlier instead, near its bottom edge, to compensate.
            int biasIdx = Math.max(0, targetPosition - 2);
            String targetDeptName = order.get(Math.min(biasIdx, order.size() - 1));
            Rectangle target = plainRowBounds(targetDeptName);
            int endY = target.y + target.height - 5;

            int rowsToPass = Math.abs(idx - (targetPosition - 1));
            dragRowContinuous(cx, startY, endY, rowsToPass);
            sleepQuiet(1500);
        } catch (WebDriverException e) {
            return false;
        }
        return visibleDepartmentNames().indexOf(departmentName) == targetPosition - 1;
    }

    /** Row bounds without scrolling — caller is responsible for the row already being visible. */
    private Rectangle plainRowBounds(String departmentName) {
        return driver.findElement(AppiumBy.xpath(
                "//*[contains(@resource-id,'tv_department_name') and @text=\"" + departmentName + "\"]"
                        + "/ancestor::*[contains(@resource-id,'cardView')]")).getRect();
    }

    /**
     * ONE continuous touch: press and hold, then move through {@code rowsToPass}
     * intermediate waypoints between start and end, and only release at the very
     * end — the hold is never broken mid-drag. The "hold" and "settle" phases use
     * a pointerMove to the SAME coordinates rather than a Pause — a Pause action
     * emits no motion event at all, so Android's long-press/drag recognition never
     * actually engages and the subsequent jump just reads as a scroll. A same-point
     * move (the technique ToolsPage.w3cDrag already uses successfully) keeps the
     * touch "alive" the same way a real finger's natural jitter would.
     */
    @SuppressWarnings("unchecked")
    private void dragRowContinuous(int x, int startY, int endY, int rowsToPass) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence seq = new Sequence(finger, 0);
        seq.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, startY));
        seq.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        seq.addAction(finger.createPointerMove(Duration.ofMillis(800),
                PointerInput.Origin.viewport(), x, startY)); // hold-to-grab (same point, keeps the touch alive)

        int steps = Math.max(4, Math.min(rowsToPass + 2, 12));
        for (int i = 1; i <= steps; i++) {
            int y = startY + (endY - startY) * i / steps;
            seq.addAction(finger.createPointerMove(Duration.ofMillis(250),
                    PointerInput.Origin.viewport(), x, y));
            seq.addAction(finger.createPointerMove(Duration.ofMillis(200),
                    PointerInput.Origin.viewport(), x, y)); // brief same-point dwell so the reorder animation keeps up
        }
        seq.addAction(finger.createPointerMove(Duration.ofMillis(600),
                PointerInput.Origin.viewport(), x, endY)); // settle at the drop point before releasing
        seq.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(List.of(seq));
    }

    private void sleepQuiet(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }
}
