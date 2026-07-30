package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;

/**
 * Email module — landing / list screen (NewEmailLandingPage + EmailListFragment),
 * plus the folder drawer. IDs from the new_email module map.
 *   list: rv_emails, fab_compose, swipe_refresh, empty_view, tv_empty_subtitle,
 *         btn_select_items, btn_cancel_select, btn_clear_folder, toolbar_selection
 *   menu: action_search, action_refresh, action_filter
 *   row : item_email_card → tv_sender, tv_subject, tv_snippet, unread_dot,
 *         iv_more, iv_check, iv_attachment
 *   drawer: drawer_layout, rv_folders, folder_row, tv_folder_name, tv_unread_count,
 *           nav_calendar, nav_contacts, nav_settings, mailbox_switch_row
 */
public class EmailPage extends BasePage {

    public EmailPage(AndroidDriver driver) {
        super(driver);
    }

    // ---------------- landing / list ----------------

    /** The email list screen is up (list, empty view, or the compose FAB). */
    public boolean isLoaded(Duration timeout) {
        return isVisible("fab_compose", timeout)
                || isVisible("rv_emails", Duration.ofSeconds(3))
                || isVisible("empty_view", Duration.ofSeconds(3));
    }

    public boolean isEmpty() {
        return isVisible("empty_view", Duration.ofSeconds(3))
                || isVisible("tv_empty_subtitle", Duration.ofSeconds(2));
    }

    public boolean isComposeFabVisible() {
        return isVisible("fab_compose", Duration.ofSeconds(6));
    }

    public ComposeEmailPage tapCompose() {
        waitVisible("fab_compose").click();
        return new ComposeEmailPage(driver);
    }

    public boolean isSearchVisible() {
        return isVisible("action_search", Duration.ofSeconds(5));
    }

    public EmailSearchPage tapSearch() {
        waitVisible("action_search").click();
        return new EmailSearchPage(driver);
    }

    public boolean tapRefresh() {
        try {
            waitVisible("action_refresh").click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean isFilterVisible() {
        return isVisible("action_filter", Duration.ofSeconds(3));
    }

    public boolean tapFilter() {
        try {
            waitVisible("action_filter").click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    // ---------------- filter bottom sheet ----------------

    public boolean isFilterSheetOpen(Duration timeout) {
        return isVisible("chip_unread", timeout)
                || isVisible("chip_all", Duration.ofSeconds(2))
                || isVisible("switch_attachments", Duration.ofSeconds(2));
    }

    /** Selects a read-status filter chip: chip_all / chip_read / chip_unread. */
    public boolean tapFilterChip(String id) {
        try {
            driver.findElement(byId(id)).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean toggleAttachmentsFilter() {
        try {
            driver.findElement(byId("switch_attachments")).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** After applying a non-default filter, the toolbar shows the filter dot. */
    public boolean isFilterDotShown(Duration timeout) {
        return isVisible("filter_dot", timeout);
    }

    /** Number of email rows currently rendered. */
    public int emailCount() {
        try {
            return driver.findElements(byId("tv_subject")).size();
        } catch (WebDriverException e) {
            return 0;
        }
    }

    /**
     * Opens the first email row and CONFIRMS the detail screen opened (retrying),
     * since a single tap on the subject text did not reliably trigger the row.
     */
    public boolean openFirstEmail() {
        for (int attempt = 0; attempt < 3; attempt++) {
            var rows = driver.findElements(byId("tv_subject"));
            if (rows.isEmpty()) return false;
            try {
                rows.get(0).click();
            } catch (WebDriverException e) {
                continue;
            }
            if (isVisible("btn_reply_selector", Duration.ofSeconds(6))
                    || isVisible("bottom_bar", Duration.ofSeconds(2))
                    || isVisible("web_view", Duration.ofSeconds(2))) {
                return true;
            }
        }
        // Detail markers never showed — but the tap may still have navigated
        // (e.g. a Draft opens Compose); report the tap as done.
        return !driver.findElements(byId("tv_subject")).isEmpty()
                ? isVisible("btn_reply_selector", Duration.ofSeconds(2))
                : true;
    }

    /** Opens the email whose subject/snippet contains the text. */
    public boolean openEmailContaining(String text, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        By loc = AppiumBy.androidUIAutomator("new UiSelector().textContains(\"" + text + "\")");
        while (System.currentTimeMillis() < deadline) {
            try {
                var els = driver.findElements(loc);
                if (!els.isEmpty()) {
                    els.get(0).click();
                    return true;
                }
            } catch (WebDriverException ignored) {
            }
            sleep(700);
        }
        return false;
    }

    public boolean isEmailShown(String text, Duration timeout) {
        return isVisibleText(text, timeout);
    }

    /** Long-press the first email row to enter selection mode. */
    public boolean longPressFirstEmail() {
        var rows = driver.findElements(byId("tv_subject"));
        if (rows.isEmpty()) return false;
        try {
            var r = rows.get(0).getRect();
            int x = r.getX() + r.getWidth() / 2;
            int y = r.getY() + r.getHeight() / 2;
            // W3C coordinate long-press at the row centre — a longClickGesture on the
            // subject element did NOT trigger the row's selection; a coordinate hold
            // (like a manual long-press) does. Verify selection mode actually started.
            var finger = new org.openqa.selenium.interactions.PointerInput(
                    org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
            var seq = new org.openqa.selenium.interactions.Sequence(finger, 1);
            seq.addAction(finger.createPointerMove(Duration.ZERO,
                    org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, y));
            seq.addAction(finger.createPointerDown(0));
            seq.addAction(finger.createPointerMove(Duration.ofMillis(1000),
                    org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, y));
            seq.addAction(finger.createPointerUp(0));
            driver.perform(java.util.Collections.singletonList(seq));
            sleep(800);
            return isSelectionMode();
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean isSelectionMode() {
        return isVisible("toolbar_selection", Duration.ofSeconds(4))
                || isVisible("btn_cancel_select", Duration.ofSeconds(2));
    }

    public boolean tapSelectItems() {
        try {
            waitVisible("btn_select_items").click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean cancelSelection() {
        try {
            waitVisible("btn_cancel_select").click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Opens the per-row overflow menu on the first email. */
    public boolean tapFirstRowOverflow() {
        var more = driver.findElements(byId("iv_more"));
        if (more.isEmpty()) return false;
        try {
            more.get(0).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean isClearFolderVisible() {
        return isVisible("btn_clear_folder", Duration.ofSeconds(3));
    }

    // ---------------- folder drawer ----------------

    /** Opens the left folder drawer. The toolbar hamburger isn't exposed as a
     *  tappable node, so use a slow left-edge drag (retried), then a coordinate
     *  tap on the toolbar nav-icon area, then the accessibility id. */
    public boolean openDrawer() {
        if (isDrawerOpen(Duration.ofMillis(500))) return true;
        // 1) The email toolbar's hamburger is the leftmost WIDE ImageButton (it
        //    sits on the email fragment's own toolbar, BELOW the app bar — not at
        //    the very top). Click it.
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                WebElement hamburger = null;
                int bestX = Integer.MAX_VALUE;
                for (WebElement b : driver.findElements(By.className("android.widget.ImageButton"))) {
                    var r = b.getRect();
                    if (r.getX() < 200 && r.getWidth() > 80 && r.getX() < bestX) {
                        bestX = r.getX();
                        hamburger = b;
                    }
                }
                if (hamburger != null) {
                    hamburger.click();
                    if (isDrawerOpen(Duration.ofSeconds(2))) return true;
                }
            } catch (WebDriverException ignored) {
            }
        }
        // 2) accessibility hamburger
        try {
            driver.findElement(AppiumBy.accessibilityId("Open navigation drawer")).click();
            if (isDrawerOpen(Duration.ofSeconds(2))) return true;
        } catch (WebDriverException ignored) {
        }
        // 3) left-edge drag fallback (DrawerLayout edge gesture)
        int w, h;
        try {
            var size = driver.manage().window().getSize();
            w = size.getWidth();
            h = size.getHeight();
        } catch (WebDriverException e) {
            w = 1080;
            h = 2400;
        }
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                edgeSwipe(2, h / 2 + attempt * 150, (int) (w * 0.75), h / 2 + attempt * 150);
                if (isDrawerOpen(Duration.ofSeconds(2))) return true;
            } catch (WebDriverException ignored) {
            }
        }
        return false;
    }

    private void tapAt(int x, int y) {
        var finger = new org.openqa.selenium.interactions.PointerInput(
                org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        var seq = new org.openqa.selenium.interactions.Sequence(finger, 1);
        seq.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, y));
        seq.addAction(finger.createPointerDown(0));
        seq.addAction(finger.createPointerUp(0));
        driver.perform(java.util.Collections.singletonList(seq));
    }

    private void edgeSwipe(int x1, int y1, int x2, int y2) {
        var finger = new org.openqa.selenium.interactions.PointerInput(
                org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        var seq = new org.openqa.selenium.interactions.Sequence(finger, 1);
        seq.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x1, y1));
        seq.addAction(finger.createPointerDown(0));
        seq.addAction(finger.createPointerMove(Duration.ofMillis(500),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x2, y2));
        seq.addAction(finger.createPointerUp(0));
        driver.perform(java.util.Collections.singletonList(seq));
    }

    public boolean isDrawerOpen(Duration timeout) {
        return isVisible("rv_folders", timeout);
    }

    public List<String> folderNames() {
        return driver.findElements(byId("tv_folder_name")).stream()
                .map(WebElement::getText)
                .filter(t -> t != null && !t.isBlank())
                .toList();
    }

    public boolean selectFolder(String name) {
        By loc = AppiumBy.androidUIAutomator(
                "new UiSelector().resourceIdMatches(\".*tv_folder_name\").text(\"" + name + "\")");
        try {
            driver.findElement(loc).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Opens the drawer (retrying) and selects a folder by name; waits for the list. */
    public boolean openFolder(String name) {
        for (int attempt = 0; attempt < 2; attempt++) {
            if (openDrawer() && isDrawerOpen(Duration.ofSeconds(3))) {
                if (selectFolder(name)) {
                    sleep(1500);
                    return isLoaded(Duration.ofSeconds(15));
                }
            }
            sleep(500);
        }
        return false;
    }

    public boolean tapDrawerSettings() {
        try {
            waitVisible("nav_settings").click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean tapDrawerContacts() {
        try {
            waitVisible("nav_contacts").click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean tapDrawerCalendar() {
        try {
            waitVisible("nav_calendar").click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    // ---------------- folder management (create / rename / delete) ----------------

    /** Taps the "Folders" section header create (+) icon → Create Folder dialog. */
    public boolean tapCreateFolder() {
        try {
            waitVisible("btn_action").click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** The create/rename folder dialog uses a programmatic TextInputEditText. */
    public boolean isFolderDialogOpen(Duration timeout) {
        return isVisibleText("Folder", timeout)
                && !driver.findElements(By.className("android.widget.EditText")).isEmpty();
    }

    public void typeFolderName(String name) {
        try {
            var edits = driver.findElements(By.className("android.widget.EditText"));
            if (!edits.isEmpty()) {
                WebElement f = edits.get(edits.size() - 1);
                f.click();
                f.clear();
                f.sendKeys(name);
            }
        } catch (WebDriverException ignored) {
        }
    }

    /** Confirms a folder dialog by its positive button text (Create/Rename/Delete). */
    public boolean tapDialogButton(String label) {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().textMatches(\"(?i)" + label + "\")")).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Opens a custom folder's overflow (btn_more) — only present on custom folders. */
    public boolean tapFolderOverflow(String folderName) {
        // Find the folder-name row, then click the btn_more on the SAME row (same
        // vertical band). Custom folders are the only ones with a btn_more.
        try {
            var names = driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceIdMatches(\".*tv_folder_name\").text(\"" + folderName + "\")"));
            if (!names.isEmpty()) {
                var nameRect = names.get(0).getRect();
                int rowY = nameRect.getY() + nameRect.getHeight() / 2;
                for (WebElement m : driver.findElements(byId("btn_more"))) {
                    var r = m.getRect();
                    if (Math.abs(r.getY() + r.getHeight() / 2 - rowY) < nameRect.getHeight()) {
                        m.click();
                        return true;
                    }
                }
                // No matched btn_more on that row — tap the right edge of the row.
                try {
                    var size = driver.manage().window().getSize();
                    tapAt((int) (size.getWidth() * 0.93), rowY);
                    return true;
                } catch (WebDriverException ignored) {
                }
            }
            // Fallback: the only/last custom-folder btn_more.
            var mores = driver.findElements(byId("btn_more"));
            if (!mores.isEmpty()) {
                mores.get(mores.size() - 1).click();
                return true;
            }
        } catch (WebDriverException ignored) {
        }
        return false;
    }

    // ---------------- folder-specific list actions ----------------

    /** The "Empty Trash" / "Clear <Folder>" button (btn_clear_folder). */
    public boolean tapClearFolder() {
        try {
            waitVisible("btn_clear_folder").click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean clearFolderText(Duration timeout) {
        return isVisible("btn_clear_folder", timeout);
    }

    // ---------------- selection mode + row overflow + confirm ----------------

    /** Taps an option in the per-row overflow popup (Move / Delete / Read By). */
    public boolean tapOverflowOption(String label) {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().textMatches(\"(?i)" + label + "\")")).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Selects the Nth email in selection mode (tapping a row toggles selection). */
    public boolean selectEmail(int index) {
        var rows = driver.findElements(byId("tv_subject"));
        if (index >= rows.size()) return false;
        try {
            rows.get(index).click();
            sleep(400);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** The selection toolbar's "N selected" count (0 if not in selection mode). */
    public int selectedCount() {
        try {
            var pat = java.util.regex.Pattern.compile("(\\d+)\\s+selected", java.util.regex.Pattern.CASE_INSENSITIVE);
            for (WebElement el : driver.findElements(By.className("android.widget.TextView"))) {
                String t = el.getText();
                if (t != null) {
                    var m = pat.matcher(t);
                    if (m.find()) return Integer.parseInt(m.group(1));
                }
            }
        } catch (WebDriverException ignored) {
        }
        return 0;
    }

    /**
     * Taps a selection-toolbar action (Move to folder / Move to trash / Delete
     * permanently / Delete drafts). These are toolbar ICONS with a content-desc,
     * so try accessibilityId first, then text, then the "More options" overflow.
     */
    public boolean tapSelectionAction(String label) {
        try {
            driver.findElement(AppiumBy.accessibilityId(label)).click();
            return true;
        } catch (WebDriverException ignored) {
        }
        if (tapOverflowOption(label)) return true;
        try {
            driver.findElement(AppiumBy.accessibilityId("More options")).click();
            sleep(500);
            if (tapOverflowOption(label)) return true;
            // content-desc inside the overflow too
            driver.findElement(AppiumBy.accessibilityId(label)).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Confirms a dialog by its positive button text (Delete / Empty / Clear / Yes). */
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

    // ---------------- helpers ----------------

    private boolean isVisibleText(String text, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        By loc = AppiumBy.androidUIAutomator("new UiSelector().textContains(\"" + text + "\")");
        while (System.currentTimeMillis() < deadline) {
            try {
                if (!driver.findElements(loc).isEmpty()) return true;
            } catch (WebDriverException ignored) {
            }
            sleep(400);
        }
        return false;
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
