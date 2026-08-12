package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DOCUMENT DISTRIBUTION — the HISTORY list ({@code historyFragment} /
 * fragment_dd_history.xml), reached from the library's History shortcut
 * ({@code DocDistLibraryPage.openHistory()}).
 *
 * <p><b>This screen cannot be identified by {@code dd_recycler}</b> — nine screens in the
 * module share it (trap 3.3). The screen-unique signal is {@code dd_result_count}, but it
 * is {@code View.GONE} until the first response lands ({@code state.total == null} on
 * mount), and a GONE view is simply not in the accessibility tree. So identity is
 * "{@code dd_result_count} displayed OR the shared toolbar header reads History" — the
 * fragment sets that header in {@code onViewCreated}, before any network call, and the
 * detail screen overwrites it with the distribution's subject the moment you drill in.
 *
 * <p><b>What {@code dd_result_count} actually says</b> (HistoryListFragment.render): the
 * SERVER's total for the current query — not "rows on screen". With
 * {@code HistoryListViewModel.PAGE_LIMIT = 50} a project with 300 sends renders
 * "300 results" over a list holding 50 rows, so "count == rows" is only true while the
 * whole result set fits in one page. {@link #resultCountValue()} exposes the number;
 * the CALLER decides which relationship is safe to assert.
 *
 * <p><b>Pagination</b> (added 2026-07-29/30). The fragment's scroll listener calls
 * {@code loadNextPage()} once the last visible row is within 8 of the end, and the VM
 * appends the next 50 while {@code dd_paging_progress} (a bottom-centre spinner, NOT the
 * full-screen {@code dd_progress}) is up. Scroll-collecting therefore GROWS the list as
 * it goes — {@link #allRowKeysScrolled()} keeps swiping until the set stops changing
 * rather than assuming one pass is the whole list (trap 3.4).
 *
 * <p><b>Rows.</b> {@code dd_subject} {@code dd_status} {@code dd_to} {@code dd_recipients}
 * {@code dd_date} are bound unconditionally on every row; {@code dd_counts} is
 * {@code View.GONE} when a send has nothing delivered / opened / failed yet
 * (HistoryListAdapter.bindCounts), so it is the one field that may legitimately be
 * missing. Because the other four are always present, the n-th {@code dd_subject} and the
 * n-th {@code dd_date} belong to the same row — which is what makes the cheap
 * three-query {@link #visibleRowKeys()} safe here (it would NOT be safe in the library,
 * where folder rows carry controls that file rows do not).
 *
 * <p><b>There is no Analytics entry on this screen in the current source.</b> ZL-19532
 * removed it — the fragment says so in a comment, {@code menu/dd_history_list.xml}
 * (which still declares {@code dd_action_analytics}) is never inflated, and nothing in
 * the module navigates to {@code historyAnalyticsFragment}. {@link #hasAnalyticsEntry()}
 * still looks, so a suite running against an older build reports the difference instead
 * of guessing.
 */
public class DocDistHistoryPage extends BasePage {

    /** The header the fragment sets on the shared common_toolbar (R.string.dd_action_history). */
    public static final String HEADER = "History";

    /** HistoryListViewModel.PAGE_LIMIT — one server page. Pagination only exists above it. */
    public static final int PAGE_LIMIT = 50;

    /** Leading integer of "1,234 results" (plurals/dd_history_result_count). */
    private static final Pattern COUNT = Pattern.compile("([0-9][0-9,.\\u00a0\\u202f]*)");

    public DocDistHistoryPage(AndroidDriver driver) {
        super(driver);
    }

    // ---------------- identity ----------------

    /**
     * True on the history list. Polls two independent signals because the primary one
     * ({@code dd_result_count}) does not exist until the first response lands.
     */
    public boolean isOpen(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (displayed("dd_result_count")) return true;
            if (HEADER.equalsIgnoreCase(headerText().trim())) return true;
            sleep(400);
        }
        return false;
    }

    /** The shared toolbar's header — "History" here, the subject on the detail screen. */
    public String headerText() { return textOf("headerText"); }

    /**
     * Waits out the first load: the full-screen spinner goes, and the screen settles on
     * either rows or the empty state. Returns false if it never settled — the caller
     * should say so rather than assert against a half-painted screen.
     */
    public boolean waitLoaded(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            boolean spinning = displayed("dd_progress");
            if (!spinning && (visibleRowCount() > 0 || isEmptyStateShown() || displayed("dd_result_count"))) {
                return true;
            }
            sleep(500);
        }
        return false;
    }

    /** True when the list can be READ — rows rendered, or the empty state up. */
    public boolean isReadable() { return visibleRowCount() > 0 || isEmptyStateShown(); }

    // ---------------- result count ----------------

    public boolean hasResultCount()  { return displayed("dd_result_count"); }
    public String resultCountText()  { return textOf("dd_result_count"); }

    /**
     * The number the server reported for the current query, or -1 when the header is not
     * shown / unparseable. Thousands separators (including the non-breaking kinds some
     * locales use) are stripped before parsing.
     */
    public int resultCountValue() {
        Matcher m = COUNT.matcher(resultCountText());
        if (!m.find()) return -1;
        String digits = m.group(1).replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return -1;
        try { return Integer.parseInt(digits); }
        catch (NumberFormatException e) { return -1; }
    }

    // ---------------- empty state ----------------

    /** {@code dd_empty} — "No emails sent yet." Also what a no-match SEARCH lands on. */
    public boolean isEmptyStateShown() { return displayed("dd_empty"); }

    public String emptyText() { return textOf("dd_empty"); }

    // ---------------- rows ----------------

    public int visibleRowCount() { return texts("dd_subject").size(); }

    public List<String> visibleSubjects() { return texts("dd_subject"); }

    /**
     * One key per RENDERED row: {@code subject|date|recipients}.
     *
     * <p>Zipping three id lists by index is only sound because every history row binds
     * all three unconditionally (HistoryListAdapter.bind) — {@code dd_counts} is
     * deliberately left out of the key precisely because it is the one field the adapter
     * hides. The key exists so a scroll-collect can DEDUPE without collapsing two
     * genuinely different sends that happen to share a subject.
     */
    public List<String> visibleRowKeys() {
        List<String> subjects = texts("dd_subject");
        List<String> dates = texts("dd_date");
        List<String> recipients = texts("dd_recipients");
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < subjects.size(); i++) {
            keys.add(subjects.get(i)
                    + "|" + (i < dates.size() ? dates.get(i) : "")
                    + "|" + (i < recipients.size() ? recipients.get(i) : ""));
        }
        return keys;
    }

    /**
     * Every row, scroll-collected — and this list GROWS as it scrolls, because reaching
     * the bottom triggers the next server page (trap 3.4 plus pagination). Stops when two
     * consecutive passes add nothing, then rewinds to the top so the next call starts
     * from a known position.
     *
     * @param maxSwipes ceiling on the work — a project with thousands of sends would
     *                  otherwise page forever. Report the ceiling if you hit it.
     */
    public List<String> allRowKeysScrolled(int maxSwipes) {
        Set<String> seen = new LinkedHashSet<>(visibleRowKeys());
        int stable = 0;
        for (int i = 0; i < maxSwipes && stable < 3; i++) {
            int before = seen.size();
            swipe(0.78, 0.30);
            // A page fetch may be in flight — the spinner sits at the bottom while the
            // already-loaded rows keep rendering, so waiting on it is cheap and stops the
            // "stable" counter from tripping on a slow network.
            waitPagingSettled(Duration.ofSeconds(6));
            seen.addAll(visibleRowKeys());
            stable = (seen.size() == before) ? stable + 1 : 0;
        }
        scrollToTop();
        return new ArrayList<>(seen);
    }

    public List<String> allRowKeysScrolled() { return allRowKeysScrolled(40); }

    /** All subjects, scroll-collected (the key's first field). */
    public List<String> allSubjectsScrolled() {
        List<String> out = new ArrayList<>();
        for (String key : allRowKeysScrolled()) out.add(key.substring(0, key.indexOf('|')));
        return out;
    }

    /** A field of the n-th RENDERED row (1-based), e.g. {@code rowField(1, "dd_status")}. */
    public String rowField(int oneBasedIndex, String id) {
        try {
            return driver.findElement(AppiumBy.xpath(
                    "(//*[@resource-id='" + appPackage() + ":id/dd_card'])[" + oneBasedIndex + "]"
                            + "//*[@resource-id='" + appPackage() + ":id/" + id + "']")).getText();
        } catch (WebDriverException e) {
            return "";
        }
    }

    /** True when the n-th rendered row actually carries that field (dd_counts may not). */
    public boolean rowHasField(int oneBasedIndex, String id) {
        try {
            return !driver.findElements(AppiumBy.xpath(
                    "(//*[@resource-id='" + appPackage() + ":id/dd_card'])[" + oneBasedIndex + "]"
                            + "//*[@resource-id='" + appPackage() + ":id/" + id + "']")).isEmpty();
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Opens the n-th rendered row (1-based) — the whole card is the click target. */
    public boolean openRowAt(int oneBasedIndex) {
        try {
            driver.findElement(AppiumBy.xpath(
                    "(//*[@resource-id='" + appPackage() + ":id/dd_card'])["
                            + oneBasedIndex + "]")).click();
            sleep(500);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Opens the row whose subject matches exactly, scrolling to reach it. */
    public boolean openRowBySubject(String subject) {
        for (int i = 0; i < 12; i++) {
            List<String> subjects = visibleSubjects();
            for (int r = 0; r < subjects.size(); r++) {
                if (subjects.get(r).equals(subject)) return openRowAt(r + 1);
            }
            swipe(0.78, 0.30);
            waitPagingSettled(Duration.ofSeconds(4));
        }
        scrollToTop();
        return false;
    }

    /** Index (1-based) of the first rendered row whose status pill matches, or -1. */
    public int indexOfRowWithStatus(String status) {
        int rows = visibleRowCount();
        for (int i = 1; i <= rows; i++) {
            if (status.equalsIgnoreCase(rowField(i, "dd_status").trim())) return i;
        }
        return -1;
    }

    // ---------------- search ----------------

    /**
     * Server-side search (the VM debounces 300 ms, then re-queries page 0), so this waits
     * out the debounce plus the round trip rather than reading the list immediately.
     */
    public boolean search(String text) {
        try {
            WebElement f = driver.findElement(byId("dd_search_input"));
            f.click();
            f.clear();
            if (!text.isEmpty()) f.sendKeys(text);
            hideKeyboardIfShown();
            sleep(400);
            waitLoaded(Duration.ofSeconds(20));
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean clearSearch() { return search(""); }

    // ---------------- refresh / paging ----------------

    public boolean pullToRefresh() {
        try {
            driver.findElement(byId("dd_swipe"));
            swipe(0.35, 0.80);
            sleep(600);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean pagingSpinnerShown() { return displayed("dd_paging_progress"); }

    /** Waits for a next-page fetch to finish, if one is running. */
    public boolean waitPagingSettled(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (!pagingSpinnerShown()) return true;
            sleep(300);
        }
        return false;
    }

    /**
     * Scrolls to the bottom looking for the paging spinner, and reports whether it was
     * ever seen. It is a RACE by nature — a fast response can repaint before a poll — so
     * treat a false as "not observed", never as "pagination is broken".
     */
    public boolean scrollAndObservePaging(int maxSwipes) {
        boolean seen = false;
        for (int i = 0; i < maxSwipes; i++) {
            swipe(0.78, 0.30);
            for (int p = 0; p < 6 && !seen; p++) {
                if (pagingSpinnerShown()) { seen = true; break; }
                sleep(150);
            }
            waitPagingSettled(Duration.ofSeconds(6));
        }
        return seen;
    }

    public void scrollToTop() {
        for (int i = 0; i < 14; i++) swipe(0.30, 0.78);
    }

    /**
     * One screenful down, then waits out any page fetch it triggered — for callers
     * scanning the list for a particular row (a FAILED status, a known subject) rather
     * than collecting all of it.
     */
    public void scrollDown() {
        swipe(0.78, 0.30);
        waitPagingSettled(Duration.ofSeconds(6));
    }

    // ---------------- analytics entry (ZL-19532) ----------------

    /**
     * Looks for a way into the Analytics screen from History.
     *
     * <p>Expected to be FALSE on a current build: ZL-19532 pulled the button, the menu
     * resource that declares {@code dd_action_analytics} is never inflated, and
     * {@code historyAnalyticsFragment} sits in dd_nav_graph with nothing navigating to it.
     * Only a direct "Analytics" label and the STANDARD ActionBar overflow are probed —
     * blind-tapping the shared toolbar's other controls (settings, scanner, project name)
     * would navigate out of the module and poison every later case.
     */
    public boolean hasAnalyticsEntry() {
        if (analyticsLabelShown()) return true;
        if (!openOverflow()) return false;
        boolean found = analyticsLabelShown();
        if (!found) dismissOverflow();
        return found;
    }

    /** Taps the Analytics entry (opening the overflow first if that is where it lives). */
    public boolean openAnalytics() {
        if (!analyticsLabelShown() && !openOverflow()) return false;
        if (!analyticsLabelShown()) return false;
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().text(\"Analytics\")")).click();
            sleep(600);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    private boolean analyticsLabelShown() {
        try {
            return !driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().text(\"Analytics\")")).isEmpty();
        } catch (WebDriverException e) {
            return false;
        }
    }

    private boolean openOverflow() {
        for (String selector : new String[]{
                "new UiSelector().description(\"More options\")",
                "new UiSelector().resourceIdMatches(\".*:id/overflow\")"}) {
            try {
                List<WebElement> hits = driver.findElements(AppiumBy.androidUIAutomator(selector));
                if (hits.isEmpty()) continue;
                hits.get(0).click();
                sleep(350);
                return true;
            } catch (WebDriverException ignored) { }
        }
        return false;
    }

    private void dismissOverflow() {
        try { driver.navigate().back(); sleep(300); } catch (WebDriverException ignored) { }
    }

    // ---------------- helpers ----------------

    private List<String> texts(String id) {
        try {
            return driver.findElements(byId(id)).stream()
                    .map(WebElement::getText)
                    .map(t -> t == null ? "" : t)
                    .toList();
        } catch (WebDriverException e) {
            return List.of();
        }
    }

    private String textOf(String id) {
        try { return driver.findElement(byId(id)).getText(); }
        catch (WebDriverException e) { return ""; }
    }

    /** Presence AND displayed — a GONE view is absent from the tree, a hidden one is not. */
    private boolean displayed(String id) {
        try {
            for (WebElement e : driver.findElements(byId(id))) {
                if (e.isDisplayed()) return true;
            }
        } catch (WebDriverException ignored) { }
        return false;
    }

    private void swipe(double fromY, double toY) {
        try {
            var size = driver.manage().window().getSize();
            driver.executeScript("mobile: swipeGesture", java.util.Map.of(
                    "left", size.width / 2 - 100,
                    "top", (int) (size.height * Math.min(fromY, toY)),
                    "width", 200,
                    "height", (int) (size.height * Math.abs(fromY - toY)),
                    "direction", fromY > toY ? "up" : "down",
                    "percent", 0.8));
            Thread.sleep(250);
        } catch (Exception ignored) { }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
