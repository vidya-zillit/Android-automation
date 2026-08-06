package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Settings → Admin Settings → "Manage Tool Groups"
 * (header reads "MANAGE TOOL GROUPING/SETTING").
 *
 * <p>Ids confirmed against the live screen: {@code addGroupBtn} is the NEW
 * button, {@code recGroups} the list, and each row carries {@code groupNameTv},
 * {@code groupTypeTv} ("Default group" / "Custom group") and {@code editIv}.
 *
 * <p>Create and Rename both use a plain {@code AlertDialog}: title
 * {@code alertTitle} ("Create group" / "Rename group"), {@code button1} = Save,
 * {@code button2} = Cancel, and an EditText that has <b>no resource-id at all</b>,
 * so it is located by class within the dialog. Tapping {@code editIv} opens a
 * popup menu offering "Rename" and "Delete".
 *
 * <p><b>Why the edit icon is found by geometry.</b> {@code groupNameTv} and
 * {@code editIv} are NOT nested under a shared {@code rowRoot} in the
 * accessibility tree, so the usual
 * {@code .../ancestor::*[rowRoot]//editIv} idiom finds nothing — verified on the
 * device. Rows are therefore paired by vertical position: the edit icon whose
 * centre sits on the same line as the group's label.
 */
public class ManageToolGroupsPage extends BasePage {

    public ManageToolGroupsPage(AndroidDriver driver) {
        super(driver);
    }

    // ---------------- navigation ----------------

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

    public boolean openManageToolGroups() {
        return tapListRow("Manage Tool Groups");
    }

    private boolean tapListRow(String label) {
        // Scroll the row into view FIRST. Admin Settings has ~14 entries and
        // "Manage Tool Groups" is the 13th — well below the fold — so an
        // unscrolled lookup simply never finds it, the screen never opens, and
        // every case in the suite skips for a reason that points nowhere near
        // the real cause.
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true))"
                            + ".scrollIntoView(new UiSelector().textContains(\"" + label + "\"))"));
        } catch (WebDriverException notScrollable) {
            // Short list, or already visible — the lookup below decides.
        }
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

    /**
     * Identified by the NEW button plus at least one group row — never by the
     * header text. The Admin Settings screen it is opened from has a row reading
     * "Manage Tool Groups", and a textContains check would match that just as
     * readily.
     */
    public boolean isLoaded() {
        // Identify by the TOOLBAR HEADER, which never scrolls.
        //
        // This used to look for addGroupBtn (the NEW button). That button lives at
        // the TOP of the list, so as soon as anything scrolled down — collecting
        // group names, reaching the TOOLS section — Android recycled it out of the
        // view tree and isLoaded() answered "false" for a screen that was plainly
        // open. Callers then backed all the way out to the bottom nav and walked
        // Settings -> Admin Settings -> Manage Tool Groups again, for nothing.
        //
        // "MANAGE TOOL GROUPING/SETTING" is safe to match on: the Admin Settings row
        // that opens this screen reads "Manage Tool Groups", so there is no overlap
        // of the kind that caught out the Customization screen.
        long deadline = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < deadline) {
            if (headerShown()) {
                return true;
            }
            sleep(300);
        }
        return false;
    }

    private boolean headerShown() {
        try {
            for (WebElement e : driver.findElements(byId("headerText"))) {
                String t = e.getText();
                if (t != null && t.toUpperCase().contains("MANAGE TOOL GROUPING")) {
                    return true;
                }
            }
        } catch (WebDriverException ignored) {
        }
        return false;
    }

    private boolean present(String id) {
        try {
            return !driver.findElements(byId(id)).isEmpty();
        } catch (WebDriverException e) {
            return false;
        }
    }

    // ---------------- the group list ----------------

    /** Group names currently rendered (viewport only — the list scrolls). */
    public List<String> groupNames() {
        List<String> out = new ArrayList<>();
        try {
            for (WebElement e : driver.findElements(byId("groupNameTv"))) {
                String t = e.getText();
                if (t != null && !t.isBlank()) out.add(t);
            }
        } catch (WebDriverException ignored) {
        }
        return out;
    }

    /** Every group name, scrolling the list from the top. */
    public List<String> allGroupNamesScrolled() {
        scrollToTop();
        java.util.LinkedHashSet<String> seen = new java.util.LinkedHashSet<>(groupNames());
        int quiet = 0;
        for (int i = 0; i < 25 && quiet < 3; i++) {
            int before = seen.size();
            swipe(0.80, 0.50);
            seen.addAll(groupNames());
            quiet = (seen.size() == before) ? quiet + 1 : 0;
        }
        return new ArrayList<>(seen);
    }

    /** True once the named group is on screen (scrolls to find it). */
    public boolean scrollToGroup(String name) {
        if (isGroupVisible(name)) return true;
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true))"
                            + ".scrollIntoView(new UiSelector().text(\"" + name + "\"))"));
        } catch (WebDriverException ignored) {
        }
        return isGroupVisible(name);
    }

    private boolean isGroupVisible(String name) {
        return groupNames().stream().anyMatch(n -> n.equals(name));
    }

    public boolean hasGroup(String name) {
        return allGroupNamesScrolled().stream().anyMatch(n -> n.equals(name));
    }

    // ---------------- creating ----------------

    /** The "NEW" button above the group list. */
    public boolean tapNew() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(byId("addGroupBtn")))
                    .click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    // ---------------- the Create / Rename dialog ----------------

    public boolean isDialogOpen(Duration timeout) {
        return isVisible("alertTitle", timeout);
    }

    /** The dialog's EditText — it carries no resource-id, so match on class. */
    private WebElement dialogField() {
        return new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.presenceOfElementLocated(
                        AppiumBy.androidUIAutomator(
                                "new UiSelector().className(\"android.widget.EditText\")")));
    }

    public void typeGroupName(String value) {
        WebElement f = dialogField();
        f.click();
        f.clear();
        if (!value.isEmpty()) {
            f.sendKeys(value);
        }
    }

    /**
     * Empties the dialog's field. {@code clear()} alone is not dependable on this
     * EditText, and leftover text turns "empty name rejected" into a completely
     * different outcome, so the result is verified and topped up with deletes.
     */
    public void clearGroupName() {
        WebElement f = dialogField();
        f.click();
        try {
            f.clear();
        } catch (WebDriverException ignored) {
        }
        for (int attempt = 0; attempt < 3 && !dialogFieldText().isEmpty(); attempt++) {
            int remaining = dialogFieldText().length();
            for (int i = 0; i < remaining + 2; i++) {
                try {
                    driver.pressKey(new io.appium.java_client.android.nativekey.KeyEvent(
                            io.appium.java_client.android.nativekey.AndroidKey.DEL));
                } catch (WebDriverException stop) {
                    break;
                }
            }
        }
    }

    public String dialogFieldText() {
        try {
            String t = dialogField().getText();
            return t == null ? "" : t;
        } catch (WebDriverException e) {
            return "";
        }
    }

    /**
     * Dialog's positive button (Save / Delete / OK).
     *
     * <p><b>The id lives in the ANDROID package, not the app's.</b> These are the
     * framework AlertDialog buttons — {@code android:id/button1} and
     * {@code android:id/button2} — so {@code byId()}, which prefixes the app's
     * package, builds an id that does not exist and the button is never found.
     * Everything else on this screen (including {@code alertTitle}) IS app-owned,
     * which makes the exception easy to miss.
     */
    public boolean tapSave() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(AppiumBy.id("android:id/button1")))
                    .click();
            return true;
        } catch (WebDriverException e) {
            // Fall back to the visible label if the framework id ever changes.
            for (String label : new String[]{"Save", "Delete", "OK"}) {
                try {
                    driver.findElement(AppiumBy.androidUIAutomator(
                            "new UiSelector().clickable(true).textMatches(\"(?i)" + label + "\")")).click();
                    return true;
                } catch (WebDriverException next) {
                    // try the next label
                }
            }
            return false;
        }
    }

    /** Dialog's negative button (Cancel) — also framework-owned. */
    public boolean tapCancel() {
        try {
            driver.findElement(AppiumBy.id("android:id/button2")).click();
            return true;
        } catch (WebDriverException e) {
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiSelector().clickable(true).textMatches(\"(?i)Cancel\")")).click();
                return true;
            } catch (WebDriverException e2) {
                return false;
            }
        }
    }

    // ---------------- per-row edit menu ----------------

    /**
     * Taps a group's edit icon, pairing row to icon by vertical position — see the
     * class note on why the tree structure rules out an XPath relationship.
     */
    public boolean tapEditIcon(String groupName) {
        if (!scrollToGroup(groupName)) {
            return false;
        }
        try {
            WebElement label = driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceIdMatches(\".*:id/groupNameTv\").text(\""
                            + groupName + "\")"));
            Rectangle lb = label.getRect();
            int labelCentreY = lb.getY() + lb.getHeight() / 2;

            WebElement best = null;
            int bestDelta = Integer.MAX_VALUE;
            for (WebElement icon : driver.findElements(byId("editIv"))) {
                Rectangle ib = icon.getRect();
                int delta = Math.abs(ib.getY() + ib.getHeight() / 2 - labelCentreY);
                if (delta < bestDelta) {
                    bestDelta = delta;
                    best = icon;
                }
            }
            // Must be on the SAME line — a row height's tolerance. Without this a
            // missing icon would silently tap whichever row happened to be nearest.
            if (best == null || bestDelta > lb.getHeight()) {
                return false;
            }
            best.click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Popup menu entry — "Rename" or "Delete". */
    public boolean tapMenuOption(String label) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(8))
                    .until(ExpectedConditions.elementToBeClickable(AppiumBy.androidUIAutomator(
                            "new UiSelector().textMatches(\"(?i)" + label + "\")")))
                    .click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    // ---------------- the TOOLS section ----------------

    /**
     * The group label shown under a tool, or "" when the tool is in no group.
     *
     * <p>Tool rows are {@code toolNameTv} + an optional {@code groupNameTv} +
     * {@code moveBtn}. The label uses the SAME id as the group rows above, so it
     * is matched by vertical position rather than by id alone. An ungrouped tool
     * simply has no label element — that absence is what "the group name
     * disappeared" means in practice.
     */
    public String toolGroupLabel(String toolName) {
        if (!scrollToTool(toolName)) {
            return "";
        }
        try {
            Rectangle tb = toolRect(toolName);
            if (tb == null) return "";
            int toolCentreY = tb.getY() + tb.getHeight() / 2;
            for (WebElement label : driver.findElements(byId("groupNameTv"))) {
                Rectangle lb = label.getRect();
                // The label sits just BELOW the tool name, within one row height.
                int delta = (lb.getY() + lb.getHeight() / 2) - toolCentreY;
                if (delta > 0 && delta <= tb.getHeight() * 2) {
                    String t = label.getText();
                    return t == null ? "" : t;
                }
            }
        } catch (WebDriverException ignored) {
        }
        return "";
    }

    /** Taps a tool's "Move" button, pairing row to button by vertical position. */
    public boolean tapMoveButton(String toolName) {
        if (!scrollToTool(toolName)) {
            return false;
        }
        try {
            Rectangle tb = toolRect(toolName);
            if (tb == null) return false;
            int centreY = tb.getY() + tb.getHeight() / 2;
            WebElement best = null;
            int bestDelta = Integer.MAX_VALUE;
            for (WebElement btn : driver.findElements(byId("moveBtn"))) {
                Rectangle bb = btn.getRect();
                int delta = Math.abs(bb.getY() + bb.getHeight() / 2 - centreY);
                if (delta < bestDelta) {
                    bestDelta = delta;
                    best = btn;
                }
            }
            if (best == null || bestDelta > tb.getHeight() * 2) {
                return false;
            }
            best.click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    private Rectangle toolRect(String toolName) {
        try {
            return driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceIdMatches(\".*:id/toolNameTv\").text(\""
                            + toolName + "\")")).getRect();
        } catch (WebDriverException e) {
            return null;
        }
    }

    /** Brings a tool row on screen (the TOOLS section sits below the groups). */
    public boolean scrollToTool(String toolName) {
        if (toolRect(toolName) != null) return true;
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true))"
                            + ".scrollIntoView(new UiSelector().resourceIdMatches(\".*:id/toolNameTv\")"
                            + ".text(\"" + toolName + "\"))"));
        } catch (WebDriverException ignored) {
        }
        return toolRect(toolName) != null;
    }

    /**
     * True when the tool sits below the "NOT IN GROUP TOOLS" heading, i.e. it
     * belongs to no group. That heading carries no resource-id, so it is located
     * by text and compared by vertical position.
     */
    public boolean isUnderNotInGroupSection(String toolName) {
        scrollToBottom();
        try {
            Rectangle heading = driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().textContains(\"NOT IN GROUP TOOLS\")")).getRect();
            Rectangle tool = toolRect(toolName);
            return tool != null && tool.getY() > heading.getY();
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean hasNotInGroupSection() {
        return textShown("NOT IN GROUP TOOLS", Duration.ofSeconds(3));
    }

    private void scrollToBottom() {
        for (int i = 0; i < 12; i++) {
            swipe(0.80, 0.45);
        }
    }

    /** Dismisses an informational popup (the delete-blocked warning) via its OK button. */
    public boolean tapDialogOk() {
        // Framework-owned id — see the note on tapSave().
        try {
            new WebDriverWait(driver, Duration.ofSeconds(8))
                    .until(ExpectedConditions.elementToBeClickable(AppiumBy.id("android:id/button1")))
                    .click();
            return true;
        } catch (WebDriverException e) {
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiSelector().clickable(true).textMatches(\"(?i)(ok|okay|got it)\")")).click();
                return true;
            } catch (WebDriverException e2) {
                return false;
            }
        }
    }

    /** True if a dialog/popup showing the given text is up. */
    public boolean popupShown(String message, Duration timeout) {
        return textShown(message, timeout);
    }

    // ---------------- feedback ----------------

    /**
     * True if a snackbar containing {@code message} appears in time. Polled hard:
     * the snackbar lives a couple of seconds, so a single lookup can land either
     * side of it and report a false negative.
     */
    public boolean snackbarShown(String message, Duration timeout) {
        // These messages are android.widget.Toast, NOT Material snackbars — verified
        // on the device, class="android.widget.Toast" with no resource-id at all.
        // That is why a UiSelector lookup missed them: a Toast lives in its own
        // WINDOW, and UiSelector searches the active one. getPageSource() spans every
        // window, so it sees the Toast; the same reason
        // CreateAdditionalShootingUnitPage.hasValidationError() greps the source.
        //
        // The Toast is only up for a second or two, so poll continuously and do the
        // cheapest possible comparison on each pass.
        String escaped = message.replace("&", "&amp;").replace("'", "&apos;")
                .replace("<", "&lt;").replace(">", "&gt;");
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            try {
                String src = driver.getPageSource();
                if (src != null && (src.contains(message) || src.contains(escaped))) {
                    return true;
                }
            } catch (WebDriverException ignored) {
                // Source can fail mid-transition — just try again.
            }
        }
        return false;
    }

    public boolean tapBackButton() {
        try {
            driver.findElement(byId("backBtn")).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean isOnAdminSettings(Duration timeout) {
        return onAdminSettingsScreen(timeout);
    }

    /**
     * True when the SETTINGS TAB is showing — the bottom-nav screen whose list
     * carries the "Admin Settings" row.
     *
     * <p>Mutually exclusive with {@link #isOnAdminSettings}: this screen has the
     * bottom nav, that one does not. Without separating the two, a caller sitting
     * on the Settings tab looks like it is somewhere unknown and gets sent through
     * a full unwind instead of simply opening Admin Settings.
     */
    public boolean isOnSettingsTab(Duration timeout) {
        return onSettingsTabScreen(timeout);
    }


    // ---------------- scrolling ----------------

    private void scrollToTop() {
        for (int i = 0; i < 10; i++) {
            swipe(0.55, 0.90);
        }
    }

    /** Slow drag, started low so it lands on the list and not the toolbar. */
    private void swipe(double fromPct, double toPct) {
        var size = driver.manage().window().getSize();
        var finger = new org.openqa.selenium.interactions.PointerInput(
                org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        var seq = new org.openqa.selenium.interactions.Sequence(finger, 1);
        int x = size.getWidth() / 2;
        seq.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(),
                x, (int) (size.getHeight() * fromPct)));
        seq.addAction(finger.createPointerDown(0));
        seq.addAction(finger.createPointerMove(Duration.ofMillis(700),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(),
                x, (int) (size.getHeight() * toPct)));
        seq.addAction(finger.createPointerUp(0));
        driver.perform(java.util.Collections.singletonList(seq));
        sleep(400);
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
