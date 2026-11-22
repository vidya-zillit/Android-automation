package com.zillit.automation.pages.common;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public abstract class BasePage {

    /**
     * Runtime resource-id prefix. Flavors rewrite it to the full application id
     * (e.g. com.zillit.zillitapp.dev), so it comes from configuration.
     *
     * <p><b>Deliberately NOT the foreground package.</b> This field is static, so
     * it is resolved once and reused by every page in the run — which means one
     * bad reading poisons everything after it. It used to fall back to
     * {@code getCurrentPackage()}, and whenever the app happened to be backgrounded
     * at that moment (launcher up, camera or QR scanner still in front) every
     * locator was built against that other package. Nothing matched, and because
     * the ids were merely WRONG rather than absent, each lookup ran its full
     * timeout — the suite appeared to hang instead of failing.
     */
    private static String idPrefix;

    protected final AndroidDriver driver;
    protected final WebDriverWait wait;

    protected BasePage(AndroidDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        if (idPrefix == null) {
            String pkg = com.zillit.automation.core.Config.configuredAppPackage();
            if (pkg.isEmpty()) {
                // Nothing configured — the foreground app is the only clue left.
                pkg = driver.getCurrentPackage();
            }
            idPrefix = pkg + ":id/";
        }
    }

    /**
     * Picks a file by (partial) name in the Android SYSTEM picker and hands it to the app —
     * and never strands the run when the file is not there.
     *
     * <p>Every page's upload comes through here, and the picker itself is driven by
     * {@link SystemFilePickerPage#pickAndReturn}: one implementation of leaving Recent
     * (where pushed fixtures do not show), finding the file, and backing out when it is
     * missing, instead of a second copy here.
     *
     * @return true only when the picker closed with the file handed over.
     */
    protected boolean pickSystemFile(String namePart, Duration timeout) {
        return new SystemFilePickerPage(driver).pickAndReturn(namePart, timeout);
    }

    /**
     * Brings the app under test back to the front after a flow left us somewhere else
     * (system picker, gallery, viewer, launcher).
     *
     * <p>Back presses first, because they unwind the foreign activity cleanly; activateApp
     * as the fallback, because a picker that refuses to close still must not own the
     * screen for the rest of the run.
     */
    protected boolean returnToAppUnderTest() {
        String app = appPackage();
        for (int i = 0; i < 5; i++) {
            String current;
            try {
                current = driver.getCurrentPackage();
            } catch (WebDriverException e) {
                current = null;
            }
            if (app.equals(current)) return true;
            try {
                driver.navigate().back();
            } catch (WebDriverException ignored) {
            }
            pauseMillis(700);
        }
        try {
            driver.executeScript("mobile: activateApp", java.util.Map.of("appId", app));
            pauseMillis(1200);
        } catch (WebDriverException ignored) {
        }
        try {
            return app.equals(driver.getCurrentPackage());
        } catch (WebDriverException e) {
            return false;
        }
    }

    private void pauseMillis(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ─────────────────────── list rows, sliced by geometry ───────────────────────
    //
    // Appium scopes an ELEMENT-relative XPath to that element's own subtree, so
    // "./ancestor::*[…]" from a row's title finds nothing at all — it never looks upward.
    // Walking up is not an option either: the LinearLayouts a row is assembled from are
    // routinely pruned out of the accessibility tree (measured on the Sides script card,
    // whose lay_actions container simply is not there), so there is often no container
    // element to reach. What does survive is position: every view belonging to a row sits
    // between that row's own anchor and the next row's.
    //
    // A band therefore runs from "the anchor's own top, less its height" — so a sibling
    // sharing the anchor's line is included — up to the same point for the next anchor.
    // No density constants, nothing to tune per screen.

    /** One row's vertical slice of the screen. */
    protected static final class Band {
        public final int from;
        public final int to;
        Band(int from, int to) { this.from = from; this.to = to; }
        boolean holds(int y) { return y >= from && y < to; }
        @Override public String toString() { return "[" + from + ".." + to + ")"; }
    }

    // ─────────────── lists taller than the screen ───────────────
    //
    // A row that is scrolled out of view is not in the accessibility tree at all, so
    // reading the window and calling it "the list" is reading whatever happens to fit.
    // That is fine until the project has more scripts, sides or call sheets than a screen
    // holds — and then a reader reports a side as missing, or a sweep calls the project
    // clean, because the row it wanted was below the fold.
    //
    // So: read the window; if the list visibly ENDS on screen, that was the whole list and
    // nothing is scrolled (the common case, and it costs no gestures). Otherwise scroll
    // through the container, merging on a key, and put the scroll position back.

    /** How many windows deep a list is followed before giving up. */
    private static final int MAX_LIST_PASSES = 14;

    /**
     * Whether the list visibly ends on screen, so there is nothing below the fold.
     *
     * <p>Measured against the <b>row pitch</b> — the gap between two consecutive anchors —
     * and NOT against the anchor's own height. That distinction is the whole of it: an
     * anchor is a title at the TOP of its row, so on a list of tall rows the last title
     * sits far above the container's bottom even while its own row runs off the screen and
     * more rows follow. Measured the naive way, a four-card script list reported itself
     * complete after three — which is exactly the bug this method existed to prevent.
     *
     * <p>Also true when there is at most one anchor: nothing to scroll for, and no pitch to
     * measure.
     */
    protected boolean listEndsOnScreen(String containerId, String anchorId) {
        try {
            List<WebElement> container = driver.findElements(byId(containerId));
            if (container.isEmpty()) return true;      // no container to scroll
            List<WebElement> anchors = elementsById(anchorId);
            if (anchors.size() <= 1) return true;      // nothing listed, or a single row
            org.openqa.selenium.Rectangle box = container.get(0).getRect();
            int lastTop = anchors.get(anchors.size() - 1).getRect().getY();
            int prevTop = anchors.get(anchors.size() - 2).getRect().getY();
            int pitch = Math.max(lastTop - prevTop, 1);
            // One more row of this pitch would have to fit entirely in what is left.
            return lastTop + pitch < box.getY() + box.getHeight();
        } catch (Exception e) {
            return false;   // cannot tell — follow the list rather than trust a guess
        }
    }

    /**
     * Reads a whole list, following it past the bottom of the screen when it goes there.
     *
     * @param containerId the scrollable view (a SwipeRefreshLayout or ScrollView is fine)
     * @param anchorId    the id that marks one row — used to see where the list ends
     * @param readWindow  reads the rows currently on screen
     * @param key         a stable identity per row, so windows can be merged; rows whose
     *                    key is null are dropped, since they cannot be de-duplicated
     */
    protected <T> List<T> readWholeList(String containerId, String anchorId,
                                        java.util.function.Supplier<List<T>> readWindow,
                                        java.util.function.Function<T, String> key) {
        java.util.Map<String, T> seen = new java.util.LinkedHashMap<>();
        collect(seen, readWindow, key);
        if (listEndsOnScreen(containerId, anchorId)) {
            return new ArrayList<>(seen.values());
        }
        int scrolls = 0;
        for (int pass = 1; pass < MAX_LIST_PASSES; pass++) {
            if (!scrollList(containerId, true)) break;
            scrolls++;
            int before = seen.size();
            collect(seen, readWindow, key);
            if (seen.size() == before) break;          // that window added nothing new
        }
        // Back where we started, so the next reader does not begin mid-list.
        for (int i = 0; i < scrolls; i++) scrollList(containerId, false);
        return new ArrayList<>(seen.values());
    }

    private <T> void collect(java.util.Map<String, T> seen,
                             java.util.function.Supplier<List<T>> readWindow,
                             java.util.function.Function<T, String> key) {
        for (T row : readWindow.get()) {
            String k = key.apply(row);
            if (k != null) seen.putIfAbsent(k, row);
        }
    }

    /**
     * Scrolls one window inside a container.
     *
     * <p>The drag stays between a third and four fifths of the container's height: it
     * never starts at the very top, where a {@code SwipeRefreshLayout} would read a
     * downward drag as pull-to-refresh instead of a scroll.
     *
     * @param forward true to move further down the list
     */
    protected boolean scrollList(String containerId, boolean forward) {
        try {
            List<WebElement> container = driver.findElements(byId(containerId));
            if (container.isEmpty()) return false;
            org.openqa.selenium.Rectangle box = container.get(0).getRect();
            int x = box.getX() + box.getWidth() / 2;
            int low = box.getY() + (int) (box.getHeight() * 0.80);
            int high = box.getY() + (int) (box.getHeight() * 0.33);
            if (forward) dragBetween(x, low, x, high); else dragBetween(x, high, x, low);
            pauseMillis(900);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void dragBetween(int x1, int y1, int x2, int y2) {
        org.openqa.selenium.interactions.PointerInput f =
                new org.openqa.selenium.interactions.PointerInput(
                        org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        org.openqa.selenium.interactions.Sequence seq =
                new org.openqa.selenium.interactions.Sequence(f, 1);
        seq.addAction(f.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x1, y1));
        seq.addAction(f.createPointerDown(0));
        for (int i = 1; i <= 10; i++) {
            seq.addAction(f.createPointerMove(Duration.ofMillis(40),
                    org.openqa.selenium.interactions.PointerInput.Origin.viewport(),
                    x1 + (x2 - x1) * i / 10, y1 + (y2 - y1) * i / 10));
        }
        seq.addAction(f.createPointerUp(0));
        driver.perform(List.of(seq));
    }

    /**
     * Brings a row onto the screen so it can be tapped.
     *
     * <p>Reading a list past the fold is only half of it: a row that is not rendered
     * cannot be clicked either, so every per-row action scrolls to its row first.
     *
     * @return true once a view with {@code anchorId} reads exactly {@code text}
     */
    protected boolean scrollToRow(String containerId, String anchorId, String text) {
        if (rowOnScreen(anchorId, text)) return true;
        int scrolls = 0;
        for (int pass = 1; pass < MAX_LIST_PASSES; pass++) {
            if (!scrollList(containerId, true)) break;
            scrolls++;
            if (rowOnScreen(anchorId, text)) return true;
        }
        for (int i = 0; i < scrolls; i++) scrollList(containerId, false);
        return false;
    }

    private boolean rowOnScreen(String anchorId, String text) {
        for (WebElement e : elementsById(anchorId)) {
            try {
                String s = e.getText();
                if (s != null && text.equals(s.trim())) return true;
            } catch (Exception ignored) { }
        }
        return false;
    }

    /**
     * Whether any text in the container satisfies {@code match}, following the container
     * past the bottom of the screen.
     *
     * <p>Used for the "does this screen say anything about being empty" questions: a
     * verdict of "it says nothing" is only worth reporting once the whole screen has
     * actually been looked at.
     */
    protected boolean anyTextMatches(String containerId,
                                     java.util.function.Predicate<String> match) {
        if (scanVisibleText(match)) return true;
        int scrolls = 0;
        for (int pass = 1; pass < MAX_LIST_PASSES; pass++) {
            if (!scrollList(containerId, true)) break;
            scrolls++;
            if (scanVisibleText(match)) {
                for (int i = 0; i < scrolls; i++) scrollList(containerId, false);
                return true;
            }
        }
        for (int i = 0; i < scrolls; i++) scrollList(containerId, false);
        return false;
    }

    private boolean scanVisibleText(java.util.function.Predicate<String> match) {
        try {
            for (WebElement t : driver.findElements(
                    AppiumBy.className("android.widget.TextView"))) {
                String s = t.getText();
                if (s != null && !s.isBlank() && match.test(s.trim())) return true;
            }
        } catch (Exception ignored) { }
        return false;
    }

    /** Elements with this id, top to bottom. Never throws. */
    protected List<WebElement> elementsById(String id) {
        List<WebElement> out = new ArrayList<>();
        try { out.addAll(driver.findElements(byId(id))); } catch (Exception ignored) { }
        out.sort(Comparator.comparingInt(e -> {
            try { return e.getRect().getY(); } catch (Exception ex) { return Integer.MAX_VALUE; }
        }));
        return out;
    }

    /** One band per element of {@code anchorId}, in the same order. */
    protected List<Band> bandsOf(String anchorId) {
        List<WebElement> anchors = elementsById(anchorId);
        List<int[]> tops = new ArrayList<>();   // {top, height}
        for (WebElement a : anchors) {
            try { tops.add(new int[]{a.getRect().getY(), a.getRect().getHeight()}); }
            catch (Exception e) { tops.add(new int[]{Integer.MAX_VALUE, 0}); }
        }
        List<Band> out = new ArrayList<>();
        for (int i = 0; i < tops.size(); i++) {
            int from = tops.get(i)[0] - Math.max(tops.get(i)[1], 1);
            int to = i + 1 < tops.size()
                    ? tops.get(i + 1)[0] - Math.max(tops.get(i + 1)[1], 1)
                    : Integer.MAX_VALUE;
            out.add(new Band(from, to));
        }
        return out;
    }

    /** The band holding the given anchor element, or null when it is not on screen. */
    protected Band bandOfAnchor(String anchorId, WebElement anchor) {
        List<WebElement> anchors = elementsById(anchorId);
        List<Band> bands = bandsOf(anchorId);
        int y;
        try { y = anchor.getRect().getY(); } catch (Exception e) { return null; }
        for (int i = 0; i < anchors.size() && i < bands.size(); i++) {
            try {
                if (anchors.get(i).getRect().getY() == y) return bands.get(i);
            } catch (Exception ignored) { }
        }
        for (Band b : bands) if (b.holds(y)) return b;
        return null;
    }

    /** Every element with this id inside the band, top to bottom. */
    protected List<WebElement> elementsIn(Band band, String id) {
        List<WebElement> out = new ArrayList<>();
        if (band == null) return out;
        for (WebElement e : elementsById(id)) {
            try { if (band.holds(e.getRect().getY())) out.add(e); } catch (Exception ignored) { }
        }
        return out;
    }

    /** The first element with this id inside the band, or null. */
    protected WebElement elementIn(Band band, String id) {
        List<WebElement> all = elementsIn(band, id);
        return all.isEmpty() ? null : all.get(0);
    }

    /** Text of the first element with this id inside the band, or null. */
    protected String textIn(Band band, String id) {
        WebElement e = elementIn(band, id);
        if (e == null) return null;
        try {
            String t = e.getText();
            return t == null ? null : t.trim();
        } catch (Exception ex) { return null; }
    }

    protected By byId(String id) {
        return AppiumBy.id(idPrefix + id);
    }

    /** The app package resource-ids resolve against (config override or foreground app). */
    protected String appPackage() {
        return idPrefix.substring(0, idPrefix.indexOf(":id/"));
    }

    protected WebElement waitVisible(String id) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(byId(id)));
    }

    protected boolean isVisible(String id, Duration timeout) {
        try {
            new WebDriverWait(driver, timeout)
                    .until(ExpectedConditions.visibilityOfElementLocated(byId(id)));
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Scrolls the first scrollable container until the given id is on screen. */
    protected void scrollToId(String id) {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true))"
                            + ".scrollIntoView(new UiSelector().resourceId(\"" + idPrefix + id + "\"))"));
        } catch (WebDriverException ignored) {
            // Not scrollable or already on screen — the follow-up lookup will decide.
        }
    }

    /** Types into the EditText nested inside a widget container. */
    protected void typeInto(String containerId, String text) {
        scrollToId(containerId);
        WebElement field = waitVisible(containerId)
                .findElement(By.className("android.widget.EditText"));
        field.clear();
        if (!text.isEmpty()) {
            field.sendKeys(text);
        }
        hideKeyboard();
    }

    protected void hideKeyboard() {
        try {
            driver.hideKeyboard();
        } catch (WebDriverException ignored) {
            // Keyboard was not open.
        }
    }

    /**
     * Dismisses the soft keyboard ONLY when one is actually up.
     *
     * <p>{@link #hideKeyboard()} is not safe to call blindly: UiAutomator2 implements it
     * by pressing BACK, and {@code sendKeys} frequently types without ever raising the
     * soft keyboard — so the BACK lands on the ACTIVITY instead and navigates away. That
     * is how a search case ended up asserting against the Tools screen: it typed a query,
     * "hid" a keyboard that was never shown, and the BACK press closed Document
     * Distribution entirely. The failure then read as "the empty state did not appear",
     * which is true but says nothing about search.
     *
     * <p>Prefer this in any page where a stray BACK would leave the screen under test.
     */
    protected void hideKeyboardIfShown() {
        try {
            if (driver.isKeyboardShown()) driver.hideKeyboard();
        } catch (WebDriverException ignored) {
            // No keyboard, or the driver cannot tell — either way, do NOT press BACK.
        }
    }

    /**
     * Settles the IME WITHOUT any possibility of a BACK press.
     *
     * <p>Use this inside dialogs and bottom sheets. {@link #hideKeyboardIfShown()} only
     * narrows the window rather than closing it: {@code isKeyboardShown()} still reports
     * true for a moment after the IME has begun animating away, so the guarded
     * {@code hideKeyboard()} can still fire a BACK — and in a cancelable
     * {@code BottomSheetDialog} that BACK dismisses the SHEET. The symptom is brutal to
     * read: the next field lookup reports "field missing" and the Save button reports
     * "disabled", for a sheet that no longer exists.
     *
     * <p>{@code performEditorAction done} is consumed by the focused editor and never
     * reaches the dialog. Leaving the keyboard up afterwards is harmless — a
     * {@code BottomSheetDialog} uses {@code SOFT_INPUT_ADJUST_RESIZE}, so its buttons sit
     * above the IME.
     */
    /**
     * Closes the soft keyboard with BACK — but ONLY when it is stably up.
     *
     * <p>Needed because a covered view is not merely off-screen, it is absent from the
     * accessibility tree: with the IME up, {@code dd_recipients_recycler} disappears from
     * the dump entirely, so scrolling for it cannot help and a recipient that was added
     * perfectly well reads back as zero rows.
     *
     * <p>BACK is safe HERE and nowhere else: with the IME showing, Android gives the BACK
     * to the input method, which closes it and never reaches the dialog. The danger is
     * firing BACK when the keyboard is already going away — then the dialog takes it and
     * dismisses. {@code isKeyboardShown()} briefly reports true during that animation, so
     * this samples it TWICE with a gap and only acts when both agree.
     */
    protected boolean closeKeyboardIfStablyShown() {
        try {
            if (!driver.isKeyboardShown()) return false;
            Thread.sleep(400);
            if (!driver.isKeyboardShown()) return false;   // it was already closing
            driver.navigate().back();
            Thread.sleep(500);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    protected void settleImeSafely() {
        // DELIBERATELY A NO-OP. Both ways of dismissing the IME after typing turned out to
        // corrupt the test:
        //   • driver.hideKeyboard() is a BACK press on UiAutomator2 — inside a cancelable
        //     dialog it dismisses the SHEET, after which every field reads as "missing"
        //     and every button as "disabled".
        //   • mobile: performEditorAction {done} appends literal text to the focused
        //     field. Observed on device: an address typed as "not-an-email" read back as
        //     "not-an-email/done/" immediately before the Add tap.
        // Leaving the keyboard up is harmless — dialogs use SOFT_INPUT_ADJUST_RESIZE, so
        // their buttons stay reachable — and anything the IME covers is brought back by
        // the pages' explicit reveal helpers (revealRecipients / revealBodyEditor), which
        // is the honest fix: scroll to what you need instead of relying on a side effect.
        // Kept as a named method so the reasoning stays attached to the call sites.
    }

    /** True when any element containing the text (snackbar, toast, label) appears. */
    public boolean textShown(String text, Duration timeout) {
        try {
            new WebDriverWait(driver, timeout)
                    .until(ExpectedConditions.presenceOfElementLocated(
                            AppiumBy.androidUIAutomator(
                                    "new UiSelector().textContains(\"" + text + "\")")));
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean textShown(String text) {
        return textShown(text, Duration.ofSeconds(10));
    }

    /**
     * True only when the ADMIN SETTINGS screen itself is up.
     *
     * <p>Do NOT implement this as {@code textShown("ADMIN SETTINGS")}.
     * {@code UiSelector.textContains()} is case-INSENSITIVE, so that check also
     * matches the Settings tab's row labelled "Admin Settings" — verified on
     * device, where it returned the {@code nameTv} whose text is "Admin
     * Settings". Every page object carried that version, so a caller standing on
     * the Settings tab was told it had already arrived, skipped tapping the row,
     * and then hunted for Admin Settings entries that were never on screen.
     *
     * <p>The two screens are separated on structure instead:
     * {@code adminSettingsRecyclerView} exists only here, and the bottom nav is
     * absent here but always present on the Settings tab.
     */
    protected boolean onAdminSettingsScreen(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            try {
                if (!driver.findElements(byId("adminSettingsRecyclerView")).isEmpty()
                        && driver.findElements(byId("bottomNavigation")).isEmpty()
                        && "ADMIN SETTINGS".equalsIgnoreCase(headerTextOrEmpty())) {
                    return true;
                }
            } catch (WebDriverException ignored) {
                // Tree in flux mid-transition — retry until the budget runs out.
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            pause(200);
        }
    }

    /**
     * True when the SETTINGS TAB is up — the bottom-nav screen carrying the
     * "Admin Settings" row. Mutually exclusive with
     * {@link #onAdminSettingsScreen} by construction: this one requires the
     * bottom nav, that one requires its absence.
     */
    protected boolean onSettingsTabScreen(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            try {
                boolean bottomNav = !driver.findElements(byId("bottomNavigation")).isEmpty();
                boolean adminRow = !driver.findElements(AppiumBy.androidUIAutomator(
                        "new UiSelector().resourceIdMatches(\".*:id/nameTv\")"
                                + ".textMatches(\"(?i)Admin Settings\")")).isEmpty();
                if (bottomNav && adminRow) {
                    return true;
                }
            } catch (WebDriverException ignored) {
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            pause(200);
        }
    }

    /**
     * The screen's header, or "" when it has none.
     *
     * <p>Needed because {@code adminSettingsRecyclerView} is NOT unique to Admin
     * Settings — the USER MANAGEMENT screen reuses the same id and also has no
     * bottom nav, so without the header those two screens are indistinguishable
     * and a caller standing in User Management is told it is on Admin Settings.
     */
    private String headerTextOrEmpty() {
        try {
            var found = driver.findElements(byId("headerText"));
            if (found.isEmpty()) {
                return "";
            }
            String t = found.get(0).getText();
            return t == null ? "" : t.trim();
        } catch (WebDriverException e) {
            return "";
        }
    }

    /**
     * Private on purpose: JoinUserDetailPage and StartProjectPage already declare
     * their own private pause(long), and a protected one here would clash with
     * both as a weaker-access override.
     */
    private void pause(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
