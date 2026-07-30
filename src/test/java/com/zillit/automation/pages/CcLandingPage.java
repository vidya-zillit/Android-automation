package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;

/**
 * C&C (Chat & Calling) landing — the ChatAndCall host + ChatFragment.
 * Verified live 2026-07-28: mainRvView (unified user+group list), tavViewRv
 * (filter chips All/Members/Groups/Favorites/Department), searchEditTextVw,
 * createGroupBtn (FAB → CreateGroupPage), tlTab (Chat/Call/Contacts).
 * Row (chat_group_listing_rv_vw): name, profileIv, badgeCount, isFevouritesIv,
 * designationVw ("N members" for a group).
 */
public class CcLandingPage extends BasePage {

    public CcLandingPage(AndroidDriver driver) {
        super(driver);
    }

    public boolean isLoaded(Duration timeout) {
        return isVisible("mainRvView", timeout)
                || isVisible("createGroupBtn", Duration.ofSeconds(3))
                || isVisible("tavViewRv", Duration.ofSeconds(3));
    }

    // ---------------- top tabs (Chat / Call / Contacts) ----------------

    public boolean tapTopTab(String label) {   // "Chat" / "Call" / "Contacts"
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().textMatches(\"(?i)" + label + "\")")).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    // ---------------- filter chips ----------------

    /** All chip labels — scoped to tavViewRv if present, else a global tabName search. */
    private List<WebElement> chipElements() {
        try {
            var scoped = driver.findElement(byId("tavViewRv")).findElements(byId("tabName"));
            if (!scoped.isEmpty()) return scoped;
        } catch (WebDriverException ignored) {
        }
        try { return driver.findElements(byId("tabName")); }
        catch (WebDriverException e) { return List.of(); }
    }

    public List<String> chipLabels() {
        return chipElements().stream()
                .map(WebElement::getText).filter(t -> t != null && !t.isBlank()).toList();
    }

    public boolean tapChip(String label) {   // All / Unread / Members / Groups / Favourites
        try {
            chipElements().stream()
                    .filter(e -> label.equalsIgnoreCase(e.getText())).findFirst()
                    .orElseThrow().click();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ---------------- search ----------------

    public boolean isSearchVisible() {
        return isVisible("searchEditTextVw", Duration.ofSeconds(5));
    }

    public void search(String text) {
        try {
            WebElement f = waitVisible("searchEditTextVw");
            f.click();
            f.clear();
            f.sendKeys(text);
            sleep(1200);
        } catch (WebDriverException ignored) {
        }
    }

    public void clearSearch() {
        try {
            driver.findElement(byId("crossIcon")).click();
        } catch (WebDriverException ignored) {
        }
    }

    /** The "new 1-1 chat" search overlay (opens SearchUsersPage). Chip-conditional. */
    public boolean tapNewChatSearch() {
        try {
            driver.findElement(byId("layHideSearch")).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    // ---------------- list rows ----------------

    public int rowCount() {
        try {
            return driver.findElement(byId("mainRvView")).findElements(byId("name")).size();
        } catch (WebDriverException e) {
            try { return driver.findElements(byId("name")).size(); } catch (WebDriverException e2) { return 0; }
        }
    }

    public boolean isEmpty() {
        return rowCount() == 0;
    }

    public boolean openRow(String nameContains, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        By loc = AppiumBy.androidUIAutomator(
                "new UiSelector().resourceIdMatches(\".*:id/name\").textContains(\"" + nameContains + "\")");
        while (System.currentTimeMillis() < deadline) {
            try {
                var els = driver.findElements(loc);
                if (!els.isEmpty()) { els.get(0).click(); return true; }
            } catch (WebDriverException ignored) {
            }
            sleep(600);
        }
        return false;
    }

    public boolean openFirstRow() {
        try {
            var rows = driver.findElements(byId("name"));
            if (rows.isEmpty()) return false;
            rows.get(0).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean isRowShown(String nameContains, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (textShown(nameContains, Duration.ofSeconds(1))) return true;
        }
        return false;
    }

    /** Long-press a group row → the edit/call/delete/leave dropdown (groups only). */
    public boolean longPressRow(String nameContains) {
        try {
            WebElement row = driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceIdMatches(\".*:id/name\").textContains(\"" + nameContains + "\")"));
            var r = row.getRect();
            w3cLongPress(r.getX() + r.getWidth() / 2, r.getY() + r.getHeight() / 2, 1000);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    // ---------------- SearchUsersPage (new 1-1 chat) ----------------
    // The "Members" chip / new-chat overlay opens SearchUsersPage, which lists
    // project users in two sections — "Members chatted with" and "Members not
    // chatted with" (notChattedWithRv) — each row a clickable ViewGroup wrapping
    // userNameTv / profileIv / designationTv. Tapping a row opens the 1-1 thread.

    /**
     * Open SearchUsersPage (the new-1-1-chat picker). Tapping the landing
     * "Search members chats" box launches it; fall back to the new-chat overlay.
     */
    public boolean openSearchUsers() {
        try {
            waitVisible("searchEditTextVw").click();
            if (isSearchUsersPageOpen(Duration.ofSeconds(6))) return true;
        } catch (WebDriverException ignored) {
        }
        tapNewChatSearch();
        return isSearchUsersPageOpen(Duration.ofSeconds(6));
    }

    public boolean isSearchUsersPageOpen(Duration timeout) {
        return isVisible("search_widget_edit_textvw", timeout)
                || isVisible("notChattedWithRv", Duration.ofSeconds(2))
                || textShown("Members not chatted with", Duration.ofSeconds(2));
    }

    public List<String> searchUserNames() {
        try {
            return driver.findElements(byId("userNameTv")).stream()
                    .map(WebElement::getText).filter(t -> t != null && !t.isBlank()).toList();
        } catch (WebDriverException e) {
            return List.of();
        }
    }

    /** Type into the SearchUsersPage search box to filter the member list. */
    public void searchUsers(String text) {
        try {
            WebElement f = waitVisible("search_widget_edit_textvw");
            f.click();
            f.clear();
            f.sendKeys(text);
            sleep(1200);
        } catch (WebDriverException ignored) {
        }
    }

    /** Open the first user row on SearchUsersPage → their 1-1 chat thread. */
    public boolean openFirstSearchUser() {
        try {
            var rows = driver.findElements(byId("userNameTv"));
            if (rows.isEmpty()) return false;
            // The name TextView isn't itself clickable — click its center, which
            // lands inside the clickable row ViewGroup.
            rows.get(0).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    // ---------------- create group ----------------

    public boolean isCreateGroupVisible() {
        return isVisible("createGroupBtn", Duration.ofSeconds(5));
    }

    public CcCreateGroupPage tapCreateGroup() {
        waitVisible("createGroupBtn").click();
        return new CcCreateGroupPage(driver);
    }

    // ---------------- Call tab ----------------

    public boolean isCallTabControlsVisible() {
        return isVisible("createRandomCall", Duration.ofSeconds(5))
                || isVisible("tlTab", Duration.ofSeconds(3));
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
