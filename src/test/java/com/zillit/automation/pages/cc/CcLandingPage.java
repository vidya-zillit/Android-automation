package com.zillit.automation.pages.cc;

import com.zillit.automation.pages.common.BasePage;
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

    /**
     * The chat list itself. NOT mainRvView alone: group info (activity_profile_showing_page)
     * also has a mainRvView, so it used to pass for the landing and recovery stopped there.
     */
    public boolean isLoaded(Duration timeout) {
        return isVisible("tavViewRv", timeout)
                || isVisible("createGroupBtn", Duration.ofSeconds(2))
                || (isVisible("searchVw", Duration.ofSeconds(2)) && isVisible("mainRvView", Duration.ofSeconds(1)));
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

    /** Names of the visible list rows (mainRvView → name), e.g. under the Favourites chip. */
    public List<String> rowNames() {
        try {
            return driver.findElement(byId("mainRvView")).findElements(byId("name")).stream()
                    .map(WebElement::getText).filter(t -> t != null && !t.isBlank()).toList();
        } catch (WebDriverException e) {
            return List.of();   // mainRvView is hidden when the Favourites chip is empty
        }
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

    // ---------------- row details / list geometry (web-gap cases) ----------------

    /** One visible mainRvView row: its name, its designationVw text ("" when hidden), and
     *  whether the favourite star (isFevouritesIv — user rows only) is shown. */
    public record Row(String name, String designation, boolean star) {}

    /** The rows currently on screen. Paired by geometry (one band per `name` anchor):
     *  element-relative XPath into mainRvView does not return the row containers. */
    public List<Row> rows() {
        java.util.ArrayList<Row> out = new java.util.ArrayList<>();
        try {
            List<WebElement> anchors = elementsById("name");
            List<Band> bands = bandsOf("name");
            for (int i = 0; i < anchors.size() && i < bands.size(); i++) {
                String name = anchors.get(i).getText();
                if (name == null || name.isBlank()) continue;
                String des = textIn(bands.get(i), "designationVw");
                boolean star = elementIn(bands.get(i), "isFevouritesIv") != null;
                out.add(new Row(name.trim(), des == null ? "" : des, star));
            }
        } catch (WebDriverException ignored) {
        }
        return out;
    }

    private String firstText(WebElement parent, String id) {
        try {
            var els = parent.findElements(byId(id));
            if (els.isEmpty()) return "";
            String t = els.get(0).getText();
            return t == null ? "" : t.trim();
        } catch (WebDriverException e) {
            return "";
        }
    }

    /** The landing search card (searchVw) is on screen. */
    public boolean isSearchCardShown() {
        return isVisible("searchVw", Duration.ofSeconds(3));
    }

    /** layHideSearch — the overlay that turns the search box into a "new chat" launcher
     *  (shown on the All and Members chips, ChatFragment.kt:120). */
    public boolean isNewChatOverlayShown() {
        return isVisible("layHideSearch", Duration.ofSeconds(2));
    }

    /** Types into the landing search box itself (works where the overlay is hidden). */
    public void typeInlineSearch(String text) {
        try {
            WebElement f = waitVisible("searchEditTextVw");
            f.click();
            f.clear();
            f.sendKeys(text);
            sleep(1500);
        } catch (WebDriverException ignored) {
        }
    }

    /** Clears the landing search field (crossIcon is never made visible by ChatFragment). */
    public void clearInlineSearch() {
        try {
            driver.findElement(byId("searchEditTextVw")).clear();
            sleep(1200);
        } catch (WebDriverException ignored) {
        }
    }

    public String inlineSearchText() {
        try {
            String t = driver.findElement(byId("searchEditTextVw")).getText();
            return t == null ? "" : t;
        } catch (WebDriverException e) {
            return "";
        }
    }

    /** Bounds of the chat list (mainRvView), or null when it is hidden. */
    public org.openqa.selenium.Rectangle listRect() {
        try {
            return driver.findElement(byId("mainRvView")).getRect();
        } catch (WebDriverException e) {
            return null;
        }
    }

    private static final String[] ROW_PARTS = {"name", "designationVw", "lastEntryTxtVw", "lastMessage",
            "profileIv", "lastMessageTime", "disconnedtedTxtView"};

    /** Bottom edge of the lowest visible row (lowest bottom of any row part), or -1. */
    public int lastRowBottom() {
        int max = -1;
        for (String id : ROW_PARTS) {
            for (WebElement e : elementsById(id)) {
                try { var r = e.getRect(); max = Math.max(max, r.getY() + r.getHeight()); }
                catch (WebDriverException ignored) { }
            }
        }
        return max;
    }

    /** Row pitch (distance between two consecutive row names), else one row's own height; -1 when empty. */
    public int rowHeight() {
        List<WebElement> names = elementsById("name");
        try {
            if (names.size() >= 2) return names.get(1).getRect().getY() - names.get(0).getRect().getY();
            if (names.size() == 1) {
                int top = names.get(0).getRect().getY();
                int bottom = lastRowBottom();
                return bottom > top ? bottom - top : -1;
            }
        } catch (WebDriverException ignored) {
        }
        return -1;
    }

    /** The list's empty view (emptyView → textTv), "" when not shown. */
    public String emptyViewText() {
        try {
            var els = driver.findElements(byId("textTv"));
            for (WebElement e : els) {
                if (e.isDisplayed() && e.getText() != null && !e.getText().isBlank()) return e.getText();
            }
        } catch (WebDriverException ignored) {
        }
        return "";
    }

    public boolean isEmptyViewShown() {
        return isVisible("emptyMainLi", Duration.ofSeconds(2));
    }

    /** Any progress/loader view on the landing (ProgressBar, shimmer). */
    public boolean isLoaderShown() {
        try {
            return !driver.findElements(By.className("android.widget.ProgressBar")).isEmpty();
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Long-press a group row → "Delete" → "Yes". For cleaning up groups this run created. */
    public boolean deleteGroupViaLongPress(String exactName) {
        try {
            WebElement row = driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceIdMatches(\".*:id/name\").text(\"" + exactName + "\")"));
            var r = row.getRect();
            w3cLongPress(r.getX() + r.getWidth() / 2, r.getY() + r.getHeight() / 2, 1000);
            sleep(900);
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().textMatches(\"(?i)delete\")")).click();
            sleep(800);
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().textMatches(\"(?i)yes\")")).click();
            sleep(2000);
            return true;
        } catch (WebDriverException e) {
            try { driver.navigate().back(); } catch (WebDriverException ignored) {}
            return false;
        }
    }

    /** Exact-name row present in the current list. */
    public boolean hasRowNamed(String exactName) {
        return rows().stream().anyMatch(r -> r.name().equals(exactName))
                || rowNames().contains(exactName);
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
