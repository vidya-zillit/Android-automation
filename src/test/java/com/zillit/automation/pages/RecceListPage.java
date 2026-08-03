package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;

/**
 * RECCE list screen (RecceListFragment / fragment_recce_list.xml) — reached from
 * Tools → Recce. Search (recceSearch), tab chips All/Published/Drafts + a Unit
 * filter, the recce cards (recceRecycler, row item_recce_card), the empty state
 * and the "Create Recce" FAB (recceFab, hidden without posting rights).
 */
public class RecceListPage extends BasePage {

    public RecceListPage(AndroidDriver driver) {
        super(driver);
    }

    public boolean isLoaded(Duration timeout) {
        return isVisible("recceFab", timeout)
                || isVisible("recceSearch", Duration.ofSeconds(3))
                || isVisible("recceRecycler", Duration.ofSeconds(3))
                || isVisible("recceState", Duration.ofSeconds(3));
    }

    // ---------------- state ----------------

    public boolean isEmptyStateShown() {
        return isVisible("recceState", Duration.ofSeconds(4))
                || textShown("No recces yet", Duration.ofSeconds(2));
    }

    public String emptyStateTitle() {
        try { return driver.findElement(byId("stateTitle")).getText(); }
        catch (WebDriverException e) { return ""; }
    }

    // ---------------- cards ----------------

    public int cardCount() {
        try { return driver.findElements(byId("cardTitle")).size(); }
        catch (WebDriverException e) { return 0; }
    }

    public List<String> cardTitles() {
        try {
            return driver.findElements(byId("cardTitle")).stream()
                    .map(WebElement::getText).filter(t -> t != null && !t.isBlank()).toList();
        } catch (WebDriverException e) { return List.of(); }
    }

    /** Every card shows a status pill (Draft / Published). */
    public List<String> cardStatuses() {
        try {
            return driver.findElements(byId("cardStatus")).stream()
                    .map(WebElement::getText).filter(t -> t != null && !t.isBlank()).toList();
        } catch (WebDriverException e) { return List.of(); }
    }

    /**
     * BLACK-BOX pagination probe: scrolls the list from top to bottom collecting
     * every DISTINCT card title it can reach, and keeps scrolling until no new
     * title appears for several consecutive swipes (so a lazily-loaded next page
     * gets a chance to arrive). Returns them in the order encountered.
     *
     * This is how we verify paging with only an APK: if the app pages correctly,
     * every seeded recce is reachable; if it silently caps (e.g. one fixed page),
     * the collected count plateaus below the seeded count.
     */
    public java.util.List<String> collectAllTitlesByScrolling(int maxSwipes, int stableRounds) {
        java.util.LinkedHashSet<String> seen = new java.util.LinkedHashSet<>();
        scrollListToTop();
        int stable = 0;
        for (int i = 0; i < maxSwipes && stable < stableRounds; i++) {
            int before = seen.size();
            seen.addAll(cardTitles());
            if (seen.size() == before) {
                stable++;
                sleep(900);          // give a pending page-load a chance to land
            } else {
                stable = 0;
            }
            swipeListUp();
        }
        seen.addAll(cardTitles());
        return new java.util.ArrayList<>(seen);
    }

    public void scrollListToTop() {
        for (int i = 0; i < 12; i++) swipeList(0.35, 0.85);
    }

    private void swipeListUp() { swipeList(0.80, 0.30); }

    private void swipeList(double fromPct, double toPct) {
        var size = driver.manage().window().getSize();
        var finger = new org.openqa.selenium.interactions.PointerInput(
                org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        var seq = new org.openqa.selenium.interactions.Sequence(finger, 1);
        seq.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(),
                size.getWidth() / 2, (int) (size.getHeight() * fromPct)));
        seq.addAction(finger.createPointerDown(0));
        seq.addAction(finger.createPointerMove(Duration.ofMillis(300),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(),
                size.getWidth() / 2, (int) (size.getHeight() * toPct)));
        seq.addAction(finger.createPointerUp(0));
        driver.perform(java.util.Collections.singletonList(seq));
        sleep(450);
    }

    /**
     * Opens a recce by title. SCROLLS to find it — a plain text match only sees
     * the cards currently rendered, so in a long list a recently created recce
     * that is off-screen would be reported as "not found".
     */
    public boolean openRecce(String titleContains) {
        String sel = "new UiSelector().resourceIdMatches(\".*:id/cardTitle\").textContains(\""
                + titleContains + "\")";
        // try where we are, then from the top, scrolling down
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < 25; i++) {
                try {
                    driver.findElement(AppiumBy.androidUIAutomator(sel)).click();
                    sleep(2000);
                    return true;
                } catch (WebDriverException notYet) {
                    swipeListUp();
                }
            }
            scrollListToTop();
        }
        return false;
    }

    /** True if a card with this title can be found by scrolling the list. */
    public boolean hasRecce(String titleContains) {
        String sel = "new UiSelector().resourceIdMatches(\".*:id/cardTitle\").textContains(\""
                + titleContains + "\")";
        scrollListToTop();
        for (int i = 0; i < 25; i++) {
            try {
                if (!driver.findElements(AppiumBy.androidUIAutomator(sel)).isEmpty()) return true;
            } catch (WebDriverException ignored) { }
            swipeListUp();
        }
        return false;
    }

    public boolean openFirstRecce() {
        try {
            var rows = driver.findElements(byId("cardTitle"));
            if (rows.isEmpty()) return false;
            rows.get(0).click();
            sleep(2000);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    // ---------------- search + filters ----------------

    public boolean isSearchVisible() { return isVisible("recceSearch", Duration.ofSeconds(4)); }

    public void search(String text) {
        try {
            WebElement f = driver.findElement(byId("recceSearch"));
            f.click();
            f.clear();
            f.sendKeys(text);
            sleep(1200);
        } catch (WebDriverException ignored) { }
    }

    public void clearSearch() {
        try { driver.findElement(byId("recceSearch")).clear(); sleep(1000); }
        catch (WebDriverException ignored) { }
    }

    public boolean tapChip(String which) {   // "All" | "Published" | "Drafts"
        String id = switch (which.toLowerCase()) {
            case "published" -> "chipPublished";
            case "drafts", "draft" -> "chipDrafts";
            default -> "chipAll";
        };
        try { driver.findElement(byId(id)).click(); sleep(1500); return true; }
        catch (WebDriverException e) { return false; }
    }

    public String chipText(String which) {
        String id = switch (which.toLowerCase()) {
            case "published" -> "chipPublished";
            case "drafts", "draft" -> "chipDrafts";
            case "unit" -> "chipUnit";
            default -> "chipAll";
        };
        try { return driver.findElement(byId(id)).getText(); }
        catch (WebDriverException e) { return ""; }
    }

    public boolean areChipsShown() {
        return isVisible("chipAll", Duration.ofSeconds(4))
                && isVisible("chipPublished", Duration.ofSeconds(2))
                && isVisible("chipDrafts", Duration.ofSeconds(2));
    }

    public boolean openUnitFilter() {
        try { driver.findElement(byId("chipUnit")).click(); sleep(1200); return true; }
        catch (WebDriverException e) { return false; }
    }

    /** Unit label shown on each visible card. */
    public java.util.List<String> cardUnits() {
        try {
            return driver.findElements(byId("cardUnit")).stream()
                    .map(WebElement::getText).filter(t -> t != null && !t.isBlank()).toList();
        } catch (WebDriverException e) { return java.util.List.of(); }
    }

    /** The unit-filter popup's entries (first one is the "all units" option). */
    public java.util.List<String> unitFilterOptions() {
        try {
            return driver.findElements(AppiumBy.id("android:id/title")).stream()
                    .map(WebElement::getText).filter(t -> t != null && !t.isBlank()).toList();
        } catch (WebDriverException e) { return java.util.List.of(); }
    }

    /** Picks a unit from the open unit-filter popup. */
    public boolean chooseUnitFilter(String labelContains) {
        for (String id : new String[]{"android:id/title", "android:id/text1"}) {
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiSelector().resourceId(\"" + id + "\").textContains(\""
                                + labelContains + "\")")).click();
                sleep(1800);
                return true;
            } catch (WebDriverException ignored) { }
        }
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().clickable(true).textContains(\"" + labelContains + "\")")).click();
            sleep(1800);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    // ---------------- create / delete ----------------

    public boolean isCreateFabVisible() { return isVisible("recceFab", Duration.ofSeconds(4)); }

    public RecceFormPage tapCreate() {
        try { driver.findElement(byId("recceFab")).click(); sleep(2500); } catch (WebDriverException ignored) { }
        return new RecceFormPage(driver);
    }

    /**
     * FAST create for bulk seeding: taps the FAB and returns as soon as the form
     * is actually up, instead of sleeping a fixed 2.5s. Returns null if the form
     * never appeared.
     */
    public RecceFormPage tapCreateFast(Duration timeout) {
        try { driver.findElement(byId("recceFab")).click(); }
        catch (WebDriverException e) { return null; }
        RecceFormPage f = new RecceFormPage(driver);
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (f.isFormUp()) return f;
            sleep(150);
        }
        return null;
    }

    /** Cheap check used by the fast seeding loop — no long implicit waits. */
    public boolean isListUp() {
        try {
            return !driver.findElements(byId("recceFab")).isEmpty()
                    || !driver.findElements(byId("recceState")).isEmpty();
        } catch (WebDriverException e) { return false; }
    }

    /** Waits (polling) until the list screen is back. */
    public boolean waitListUp(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (isListUp()) return true;
            sleep(150);
        }
        return false;
    }

    /** Delete the card whose title contains the text (card delete → confirm dialog). */
    public boolean deleteRecce(String titleContains) {
        try {
            // find the card's delete button by walking from the matching title row
            var titles = driver.findElements(byId("cardTitle"));
            var deletes = driver.findElements(byId("cardDelete"));
            int idx = -1;
            for (int i = 0; i < titles.size(); i++) {
                String t = titles.get(i).getText();
                if (t != null && t.contains(titleContains)) { idx = i; break; }
            }
            if (idx < 0 || idx >= deletes.size()) return false;
            deletes.get(idx).click();
            sleep(1200);
        } catch (WebDriverException e) { return false; }
        return confirmDelete();
    }

    public boolean isDeleteDialogShown(Duration timeout) {
        return textShown("Delete recce", timeout);
    }

    public boolean confirmDelete() {
        long deadline = System.currentTimeMillis() + 6000;
        while (System.currentTimeMillis() < deadline) {
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiSelector().clickable(true).textMatches(\"(?i)(delete|yes|ok|confirm)\")")).click();
                sleep(2000);
                return true;
            } catch (WebDriverException notYet) { sleep(300); }
        }
        return false;
    }

    public void pullToRefresh() {
        var size = driver.manage().window().getSize();
        var finger = new org.openqa.selenium.interactions.PointerInput(
                org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        var seq = new org.openqa.selenium.interactions.Sequence(finger, 1);
        seq.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(),
                size.getWidth() / 2, (int) (size.getHeight() * 0.45)));
        seq.addAction(finger.createPointerDown(0));
        seq.addAction(finger.createPointerMove(Duration.ofMillis(600),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(),
                size.getWidth() / 2, (int) (size.getHeight() * 0.85)));
        seq.addAction(finger.createPointerUp(0));
        driver.perform(java.util.Collections.singletonList(seq));
        sleep(2500);
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
