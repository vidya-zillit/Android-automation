package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;

/**
 * Tools bottom-nav tab — a searchable list of tool modules grouped into
 * PRODUCTION / ADMIN sections (Document Distribution, Weather, Confidential Info,
 * Info, Viewing & Posting Rights Grid, …). Each row opens its module. The
 * "Confidential Info" and "Info" modules are Bulletin-style CHAT screens
 * (header + rvChat + etMessage/ivAttachment/ivRecord + a History button), so once
 * opened they are driven with {@link HomeChatPage}.
 */
public class ToolsPage extends BasePage {

    public ToolsPage(AndroidDriver driver) {
        super(driver);
    }

    /** Tap the Tools tab in the bottom navigation. */
    public boolean openToolsTab() {
        try {
            WebElement nav = driver.findElement(byId("bottomNavigation"));
            for (WebElement it : nav.findElements(byId("menuText"))) {
                String t = it.getText();
                if (t != null && t.equalsIgnoreCase("Tools")) { it.click(); sleep(1200); return true; }
            }
        } catch (WebDriverException ignored) {
        }
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().textMatches(\"(?i)Tools\")")).click();
            sleep(1200);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /**
     * True when the Tools screen is up.
     *
     * <p>Checks the search box BY ID first. The old version matched the text
     * "Search tools" — which is the box's HINT, so it vanishes the instant anything is
     * typed — with section headers as fallbacks, and those disappear too when a filter
     * matches nothing. The result: once a previous navigation left a query in the box,
     * Tools reported "not loaded" for ever and every attempt to re-enter a module failed
     * with the screen plainly on display. {@code searchEt} is present regardless of what
     * it contains.
     */
    public boolean isLoaded(Duration timeout) {
        return isVisible("searchEt", timeout)
                || textShown("Search tools", Duration.ofSeconds(1))
                || textShown("PRODUCTION", Duration.ofSeconds(1))
                || textShown("ADMIN", Duration.ofSeconds(1));
    }

    // ---------------- search (searchEt) ----------------

    /**
     * Opens the module if its row is ALREADY on screen — no clearing, no scrolling.
     *
     * <p>The cheapest path, and common in practice: a previous navigation may have left
     * the list filtered to exactly this row, or it simply sits above the fold. Checking
     * costs one lookup, and skipping the clear+scroll saves the whole round trip.
     *
     * @return true only when a row was actually found AND tapped.
     */
    public boolean openIfVisible(String label) {
        // EXCLUDE EditText. The search box holds the query from the previous navigation —
        // literally the module's name — so a bare text match found the INPUT, clicked it,
        // and reported success while only moving the caret. The log said
        // "opened=true library=false" for every label with the app sitting on the bottom
        // nav: a tap that lands on the thing you searched WITH rather than the thing you
        // searched FOR.
        try {
            for (WebElement el : driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().textMatches(\"(?i)" + label + "\")"))) {
                try {
                    String cls = el.getAttribute("className");
                    if (cls != null && cls.contains("EditText")) continue;
                    if (!el.isDisplayed()) continue;
                    el.click();
                    sleep(1200);
                    return true;
                } catch (WebDriverException ignored) { }
            }
        } catch (WebDriverException ignored) { }
        return false;
    }

    /** Filter the tool list via the "Search tools" box (searchEt). */
    public void searchTools(String text) {
        try {
            WebElement f = driver.findElement(byId("searchEt"));
            f.click();
            f.clear();
            f.sendKeys(text);
            sleep(1200);
        } catch (WebDriverException fallback) {
            try {
                WebElement f = driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiSelector().className(\"android.widget.EditText\")"));
                f.click(); f.clear(); f.sendKeys(text); sleep(1200);
            } catch (WebDriverException ignored) {}
        }
    }

    public void clearSearch() {
        try {
            WebElement f = driver.findElement(byId("searchEt"));
            f.clear();
            sleep(1000);
        } catch (WebDriverException ignored) {}
    }

    public boolean isEmptyStateShown(Duration timeout) {
        return textShown("No tools match", timeout) || isVisible("emptyView", Duration.ofSeconds(2));
    }

    // ---------------- groups + tool rows ----------------

    public java.util.List<String> groupTitles() {
        try {
            return driver.findElements(byId("groupTitleTv")).stream()
                    .map(WebElement::getText).filter(t -> t != null && !t.isBlank()).toList();
        } catch (WebDriverException e) { return java.util.List.of(); }
    }

    public java.util.List<String> toolNames() {
        try {
            return driver.findElements(byId("nameTv")).stream()
                    .map(WebElement::getText).filter(t -> t != null && !t.isBlank()).toList();
        } catch (WebDriverException e) { return java.util.List.of(); }
    }

    public int groupCount() {
        try { return driver.findElements(byId("groupTitleTv")).size(); }
        catch (WebDriverException e) { return 0; }
    }

    /** Scroll the Tools list top→bottom and collect ALL group titles (deduped, in order). */
    public java.util.List<String> allGroupTitlesScrolled() { return collectScrolled("groupTitleTv"); }

    /** Scroll the Tools list top→bottom and collect ALL tool names (deduped, in order). */
    public java.util.List<String> allToolNamesScrolled() { return collectScrolled("nameTv"); }

    private java.util.List<String> collectScrolled(String id) {
        java.util.LinkedHashSet<String> seen = new java.util.LinkedHashSet<>();
        scrollToTop();
        int stable = 0;
        for (int i = 0; i < 12 && stable < 2; i++) {
            int before = seen.size();
            try {
                for (WebElement e : driver.findElements(byId(id))) {
                    String t = e.getText();
                    if (t != null && !t.isBlank()) seen.add(t);
                }
            } catch (WebDriverException ignored) {}
            stable = (seen.size() == before) ? stable + 1 : 0;
            swipeUp();
        }
        return new java.util.ArrayList<>(seen);
    }

    private void scrollToTop() {
        for (int i = 0; i < 5; i++) {
            var size = driver.manage().window().getSize();
            swipe(size.getWidth() / 2, (int) (size.getHeight() * 0.35), size.getWidth() / 2, (int) (size.getHeight() * 0.8));
        }
    }

    private void swipeUp() {
        var size = driver.manage().window().getSize();
        swipe(size.getWidth() / 2, (int) (size.getHeight() * 0.75), size.getWidth() / 2, (int) (size.getHeight() * 0.35));
    }

    private void swipe(int x1, int y1, int x2, int y2) {
        var finger = new org.openqa.selenium.interactions.PointerInput(
                org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        var seq = new org.openqa.selenium.interactions.Sequence(finger, 1);
        seq.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x1, y1));
        seq.addAction(finger.createPointerDown(0));
        seq.addAction(finger.createPointerMove(Duration.ofMillis(300),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x2, y2));
        seq.addAction(finger.createPointerUp(0));
        driver.perform(java.util.Collections.singletonList(seq));
        sleep(500);
    }

    public int toolRowCount() {
        try { return driver.findElements(byId("nameTv")).size(); }
        catch (WebDriverException e) { return 0; }
    }

    /** True if every group header also shows a count (data rendered properly). */
    public boolean allGroupsHaveCounts() {
        try {
            int titles = driver.findElements(byId("groupTitleTv")).size();
            int counts = driver.findElements(byId("groupCountTv")).size();
            return titles > 0 && counts >= titles;
        } catch (WebDriverException e) { return false; }
    }

    // ---------------- reorder groups (ReorderToolGroupsBottomSheet) ----------------

    public boolean openReorder() {
        try { driver.findElement(byId("reorderBtn")).click(); sleep(1200); return true; }
        catch (WebDriverException e) { return false; }
    }

    public boolean isReorderSheetOpen(Duration timeout) {
        return isVisible("reorderRecycler", timeout)
                || textShown("Reorder groups", Duration.ofSeconds(2))
                || isVisible("saveBtn", Duration.ofSeconds(2));
    }

    public java.util.List<String> reorderGroupNames() {
        try {
            return driver.findElements(byId("groupNameTv")).stream()
                    .map(WebElement::getText).filter(t -> t != null && !t.isBlank()).toList();
        } catch (WebDriverException e) { return java.util.List.of(); }
    }

    /** Drag the first group's handle down past the second row (swaps order). */
    public boolean dragFirstGroupDown() {
        try {
            var handles = driver.findElements(byId("dragHandle"));
            if (handles.size() < 2) return false;
            var r1 = handles.get(0).getRect();
            var r2 = handles.get(1).getRect();
            int x = r1.getX() + r1.getWidth() / 2;
            int y1 = r1.getY() + r1.getHeight() / 2;
            int y2 = r2.getY() + r2.getHeight() + 20;   // drop below row 2
            w3cDrag(x, y1, x, y2);
            sleep(800);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    public boolean saveReorder() {
        try { driver.findElement(byId("saveBtn")).click(); sleep(1500); return true; }
        catch (WebDriverException e) { return false; }
    }

    public boolean cancelReorder() {
        try { driver.findElement(byId("cancelBtn")).click(); sleep(800); return true; }
        catch (WebDriverException e) { return false; }
    }

    // ---------------- customization (ToolsEnableDisablePage) ----------------

    /** Open Customization of tools via the banner card (admin only). */
    public boolean openCustomization() {
        scrollToTop();   // the banner is at the top; a scrolled list hides it
        try { driver.findElement(byId("toolsCustomization")).click(); sleep(1500); return true; }
        catch (WebDriverException e) { return false; }
    }

    public boolean isCustomizationOpen(Duration timeout) {
        boolean open = textShown("Customization of tools", timeout)
                || isVisible("submitBtn", Duration.ofSeconds(3));
        if (open) waitCustomizationLoaded(Duration.ofSeconds(10));   // list loads async
        return open;
    }

    /** Wait until the customization tool list has populated (rows load via API). */
    public boolean waitCustomizationLoaded(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (!customizationToolNames().isEmpty()) return true;
            sleep(500);
        }
        return false;
    }

    public java.util.List<String> customizationToolNames() {
        try {
            return driver.findElements(byId("nameTv")).stream()
                    .map(WebElement::getText).filter(t -> t != null && !t.isBlank()).toList();
        } catch (WebDriverException e) { return java.util.List.of(); }
    }

    /** In the customization list, a tool's enabled tick (infoIv) count. */
    public int enabledTickCount() {
        try { return driver.findElements(byId("infoIv")).size(); }
        catch (WebDriverException e) { return 0; }
    }

    /** Tap a tool row (by name) in the customization list to toggle it. Scrolls to find it. */
    public boolean toggleCustomizationTool(String name) {
        String sel = "new UiSelector().resourceIdMatches(\".*:id/nameTv\").textMatches(\"(?i)" + name + "\")";
        for (int i = 0; i < 14; i++) {
            try {
                driver.findElement(AppiumBy.androidUIAutomator(sel)).click();
                sleep(900);
                return true;
            } catch (WebDriverException notVisible) {
                var size = driver.manage().window().getSize();
                swipe(size.getWidth() / 2, (int) (size.getHeight() * 0.75),
                        size.getWidth() / 2, (int) (size.getHeight() * 0.35));
            }
        }
        return false;
    }

    /** Confirm the "disable tool" warning dialog (Yes). */
    public boolean confirmDisableDialog(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiSelector().clickable(true).textMatches(\"(?i)(yes|ok|confirm)\")")).click();
                return true;
            } catch (WebDriverException notYet) { sleep(300); }
        }
        return false;
    }

    public boolean saveCustomization() {
        try { driver.findElement(byId("submitBtn")).click(); sleep(2000); return true; }
        catch (WebDriverException e) { return false; }
    }

    private void w3cDrag(int x1, int y1, int x2, int y2) {
        var finger = new org.openqa.selenium.interactions.PointerInput(
                org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        var seq = new org.openqa.selenium.interactions.Sequence(finger, 1);
        seq.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x1, y1));
        seq.addAction(finger.createPointerDown(0));
        seq.addAction(finger.createPointerMove(Duration.ofMillis(400),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x1, y1)); // hold to grab
        seq.addAction(finger.createPointerMove(Duration.ofMillis(900),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x2, y2));
        seq.addAction(finger.createPointerUp(0));
        driver.perform(java.util.Collections.singletonList(seq));
    }

    public boolean isModuleListed(String name, Duration timeout) {
        return textShown(name, timeout);
    }

    /** Open a tool module by its exact (case-insensitive) label. */
    public boolean openModule(String name) {
        // Match the ROW label TextView only — NOT the search EditText, whose text
        // also equals the typed module name (that would just re-focus the search).
        String rowSel = "new UiSelector().className(\"android.widget.TextView\")"
                + ".textMatches(\"(?i)" + name + "\")";
        long deadline = System.currentTimeMillis() + 8000;
        while (System.currentTimeMillis() < deadline) {
            try {
                driver.findElement(AppiumBy.androidUIAutomator(rowSel)).click();
                sleep(1500);
                return true;
            } catch (WebDriverException notYet) {
                try {
                    driver.findElement(AppiumBy.androidUIAutomator(
                            "new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(" + rowSel + ")"));
                } catch (WebDriverException ignored) {
                }
                sleep(500);
            }
        }
        return false;
    }

    /** The opened module's chat is up (Bulletin-style input bar). */
    public boolean isModuleChatOpen(Duration timeout) {
        return isVisible("etMessage", timeout) || isVisible("rvChat", Duration.ofSeconds(3));
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
