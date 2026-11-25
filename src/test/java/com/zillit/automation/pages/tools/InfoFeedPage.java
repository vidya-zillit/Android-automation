package com.zillit.automation.pages.tools;

import com.zillit.automation.pages.homechat.HomeChatPage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * The Info / Confidential Info feed ({@code ConfidentialInfo.kt}, one activity for both
 * tools), on top of the shared chat helpers in {@link HomeChatPage}.
 *
 * <p>Everything here is read from the app source, not from device dumps:
 * <ul>
 *   <li>{@code activity_confidential_info.xml}: {@code layOption btnBack tvCount laySearch
 *       edittext_search top_button bottom_button layAction layButton tvAction rvChat
 *       userHistoryBtn arrowDownCard layPostMsg layNoMsg tvRequest}.</li>
 *   <li>The composer ({@code CommonKeyboardFragment}): {@code etMessage ivEmoji emojiPicker
 *       backPress ivSearch ivAttachment ivRecord ivSendMessage llReply llDelete}.</li>
 *   <li>A text row ({@code chat_text_layout.xml}): {@code materialCardView tvUserName tvMessage
 *       btnTryAgain tvMsgTime ivClock rvMessageReply}; a comment row
 *       ({@code reply_chat_item.xml}): {@code tvUserName tvMessage deleteIv tvMsgTime}.</li>
 *   <li>History ({@code activity_user_chat_history_page.xml}): {@code toolbar/headerText
 *       headerMsg searchView searchHolder rvChat}.</li>
 * </ul>
 * Page object owned by the Info / Confidential Info suites.
 */
public class InfoFeedPage extends HomeChatPage {

    public static final String EMPTY_SEARCH = "Please type text to search.";
    public static final String EMPTY_SEND = "Please Type Message To Send";
    public static final String LIMIT_ERROR = "There is only 2000 Characters limit allowed to send";
    public static final String EDIT_EMPTY = "Message could not be empty";
    public static final String NO_EDIT_PERMISSION = "You Don't Have Permission To Edit This Message.";
    public static final String NO_EDIT_OTHER_COMMENT = "You can't edit other users message";
    public static final String HISTORY_HEADER = "Records of deleted messages";
    public static final int TEXT_LIMIT = 2000;

    public InfoFeedPage(AndroidDriver driver) {
        super(driver);
    }

    // ---------------- generic ----------------

    public boolean visible(String id, Duration timeout) {
        return isVisible(id, timeout);
    }

    /** Present AND displayed right now (no wait). */
    public boolean shownNow(String id) {
        try {
            for (WebElement e : driver.findElements(byId(id))) {
                if (e.isDisplayed()) return true;
            }
        } catch (WebDriverException ignored) { }
        return false;
    }

    public String textOf(String id) {
        try {
            return driver.findElement(byId(id)).getText();
        } catch (WebDriverException e) {
            return null;
        }
    }

    public boolean tap(String id) {
        try {
            driver.findElement(byId(id)).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** A snackbar / toast / label containing the text appears within the timeout. */
    public boolean messageAppears(String text, Duration timeout) {
        return textShown(text, timeout);
    }

    public void hideKeyboardSafely() {
        hideKeyboardIfShown();
    }

    /** The composer's current text ("" when absent or empty; the hint is not text). */
    public String composerText() {
        try {
            WebElement f = driver.findElement(byId("etMessage"));
            String t = f.getText();
            String hint = null;
            try { hint = f.getAttribute("hint"); } catch (WebDriverException ignored) { }
            if (t == null) return "";
            if (hint != null && t.equals(hint)) return "";
            if ("true".equals(safeAttr(f, "showing-hint"))) return "";
            return t;
        } catch (WebDriverException e) {
            return "";
        }
    }

    private static String safeAttr(WebElement e, String name) {
        try { return e.getAttribute(name); } catch (WebDriverException x) { return null; }
    }

    /** Puts text in the composer without sending (sendKeys bypasses the IME). */
    public void typeDraft(String text) {
        typeMessage(text);
    }

    /** Number of on-screen elements whose text is exactly {@code text}. */
    public int exactTextCount(String text) {
        try {
            return driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().text(\"" + esc(text) + "\")")).size();
        } catch (WebDriverException e) {
            return 0;
        }
    }

    /** Number of on-screen elements whose text contains {@code text}. */
    public int containsTextCount(String text) {
        try {
            return driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().textContains(\"" + esc(text) + "\")")).size();
        } catch (WebDriverException e) {
            return 0;
        }
    }

    // ---------------- message rows ----------------

    /** The text of a view with {@code id} inside the message card holding {@code text}. */
    public String rowChildText(String text, String id) {
        String pkg = appPackage();
        String xpath = "//*[@resource-id='" + pkg + ":id/materialCardView']"
                + "[.//*[contains(@text," + xpathLiteral(text) + ")]]"
                + "//*[@resource-id='" + pkg + ":id/" + id + "']";
        try {
            List<WebElement> els = driver.findElements(AppiumBy.xpath(xpath));
            if (els.isEmpty()) return null;
            return els.get(0).getText();
        } catch (WebDriverException e) {
            return null;
        }
    }

    /** {@code tvMsgTime} of the row: "Edited Nov 23, 2026 09:41 PM" once edited. */
    public String rowTime(String text) {
        return rowChildText(text, "tvMsgTime");
    }

    public String rowUserName(String text) {
        return rowChildText(text, "tvUserName");
    }

    /** The full tvMessage of the row whose message contains {@code part}. */
    public String messageTextContaining(String part) {
        try {
            for (WebElement e : driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceIdMatches(\".*:id/tvMessage\").textContains(\""
                            + esc(part) + "\")"))) {
                return e.getText();
            }
        } catch (WebDriverException ignored) { }
        return null;
    }

    /** Taps the tvMessage of the row containing the text (selection-mode toggle). */
    public boolean tapMessage(String text) {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceIdMatches(\".*:id/tvMessage\").textContains(\""
                            + esc(text) + "\")")).click();
            sleepMs(600);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /**
     * Taps where the "Read more" / "Read less" span can be: it closes the LAST line of the
     * text, whose length is unknown, so successive attempts sweep that line right to left
     * (and two heights, as the line height depends on the font size setting).
     */
    public boolean tapReadToggle(String part, int attempt) {
        double[] xs = {0.92, 0.8, 0.68, 0.56, 0.44, 0.32, 0.2, 0.1};
        try {
            hideKeyboardIfShown();
            WebElement e = driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceIdMatches(\".*:id/tvMessage\").textContains(\""
                            + esc(part) + "\")"));
            Rectangle r = e.getRect();
            int dy = (attempt / xs.length) % 2 == 0 ? 18 : 40;
            tapAt(r.getX() + (int) (r.getWidth() * xs[attempt % xs.length]), r.getY() + r.getHeight() - dy);
            sleepMs(700);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    // ---------------- search ----------------

    public boolean openSearch() {
        hideKeyboardIfShown();
        clearMessage();
        if (!isVisible("ivSearch", Duration.ofSeconds(4))) return false;
        tap("ivSearch");
        return isVisible("laySearch", Duration.ofSeconds(5));
    }

    public boolean isSearchOpen() {
        return shownNow("laySearch");
    }

    /** Replaces the search text WITHOUT running the search. */
    public void setSearchText(String q) {
        WebElement f = driver.findElement(byId("edittext_search"));
        f.click();
        f.clear();
        if (!q.isEmpty()) f.sendKeys(q);
    }

    /** Runs the search with the IME Search action, as a user pressing the search key. */
    public void submitSearch() {
        try {
            driver.findElement(byId("edittext_search")).click();
        } catch (WebDriverException ignored) { }
        try {
            driver.executeScript("mobile: performEditorAction", Map.of("action", "search"));
        } catch (WebDriverException e) {
            // Fallback: the IME search key.
            driver.pressKey(new io.appium.java_client.android.nativekey.KeyEvent(
                    io.appium.java_client.android.nativekey.AndroidKey.SEARCH));
        }
        sleepMs(1500);
    }

    public void search(String q) {
        setSearchText(q);
        submitSearch();
    }

    /** {@code tvCount}: the match count in search, the selection count in selection mode. */
    public String countText() {
        String t = textOf("tvCount");
        return t == null ? "" : t.trim();
    }

    /** Waits for tvCount to read something non-blank; returns it ("" on timeout). */
    public String waitCount(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            String c = countText();
            if (!c.isEmpty()) return c;
            sleepMs(400);
        }
        return countText();
    }

    public boolean tapSearchUp() {
        boolean ok = tap("top_button");
        sleepMs(1500);
        return ok;
    }

    public boolean tapSearchDown() {
        boolean ok = tap("bottom_button");
        sleepMs(1500);
        return ok;
    }

    /** {@code btnBack} in the option bar: closes search AND selection mode. */
    public boolean tapOptionBack() {
        hideKeyboardIfShown();
        boolean ok = tap("btnBack");
        sleepMs(900);
        return ok;
    }

    // ---------------- selection mode ----------------

    public boolean isSelectionMode() {
        return shownNow("layAction");
    }

    public boolean waitSelectionMode(Duration timeout) {
        return isVisible("layAction", timeout);
    }

    public String actionLabel() {
        return textOf("tvAction");
    }

    // ---------------- history ----------------

    public boolean openHistory() {
        hideKeyboardIfShown();
        if (!isVisible("userHistoryBtn", Duration.ofSeconds(5))) return false;
        tap("userHistoryBtn");
        return isHistoryOpen(Duration.ofSeconds(12));
    }

    /** UserChatHistoryPage: its activity, or the "Records of deleted messages" strip. */
    public boolean isHistoryOpen(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            try {
                String a = driver.currentActivity();
                if (a != null && a.contains("UserChatHistory")) return true;
            } catch (WebDriverException ignored) { }
            if (shownNow("headerMsg")) return true;
            sleepMs(500);
        }
        return false;
    }

    public String toolbarHeader() {
        isVisible("headerText", Duration.ofSeconds(6));   // History fills it in after loading starts
        return textOf("headerText");
    }

    public String historyHeaderMessage() {
        return textOf("headerMsg");
    }

    /**
     * History search is reachable when the toolbar's search icon ({@code settingImage}) is
     * shown or the search holder already is. The app shows the icon only for the Script
     * Notes tools (UserChatHistoryPage.setToolbar), which is sheet bug INFO-22 / CONF-20.
     */
    public boolean historySearchReachable() {
        return shownNow("settingImage") || shownNow("searchHolder") || shownNow("searchView");
    }

    /** Looks for the text in History, scrolling the list both ways. */
    public boolean historyShows(String text, int swipes) {
        return messageShownScrolled(text, swipes);
    }

    // ---------------- scroll controls ----------------

    public boolean isArrowDownShown() {
        return shownNow("arrowDownCard");
    }

    /** Scrolls the feed towards OLDER rows a few times. */
    public void scrollOlder(int times) {
        for (int i = 0; i < times; i++) {
            try {
                WebElement rv = driver.findElement(byId("rvChat"));
                driver.executeScript("mobile: scrollGesture", Map.of(
                        "elementId", ((org.openqa.selenium.remote.RemoteWebElement) rv).getId(),
                        "direction", "up",
                        "percent", 0.9));
            } catch (WebDriverException ignored) { }
            sleepMs(400);
        }
    }

    /** Scrolls the feed towards NEWER rows (to the very bottom after a few). */
    public void scrollNewer(int times) {
        for (int i = 0; i < times; i++) {
            try {
                WebElement rv = driver.findElement(byId("rvChat"));
                driver.executeScript("mobile: scrollGesture", Map.of(
                        "elementId", ((org.openqa.selenium.remote.RemoteWebElement) rv).getId(),
                        "direction", "down",
                        "percent", 0.9));
            } catch (WebDriverException ignored) { }
            sleepMs(400);
        }
    }

    // ---------------- emoji ----------------

    public boolean openEmojiPicker() {
        // No keyboard dismissal: ivEmoji hides the IME itself, and a BACK that reaches the
        // screen leaves the tool.
        for (int i = 0; i < 2; i++) {
            if (!tap("ivEmoji")) return false;
            if (isVisible("emojiPicker", Duration.ofSeconds(5))) return true;
            sleepMs(800);
        }
        return false;
    }

    /**
     * Taps the first emoji in the picker grid. The picker is androidx EmojiPickerView: the
     * emoji are TextViews with a one-or-two code point text.
     */
    public String pickFirstEmoji() {
        try {
            WebElement picker = driver.findElement(byId("emojiPicker"));
            // androidx EmojiView may expose the emoji as text OR only as content-desc.
            for (WebElement t : picker.findElements(By.xpath(".//*"))) {
                for (String s : new String[]{t.getText(), safeAttr(t, "content-desc")}) {
                    if (s != null && !s.isBlank() && s.codePointCount(0, s.length()) <= 4
                            && !Character.isLetterOrDigit(s.codePointAt(0))) {
                        t.click();
                        sleepMs(500);
                        return s;
                    }
                }
            }
            // EmojiView draws on a canvas and may expose nothing: tap grid positions and
            // read what the composer gained.
            Rectangle r = picker.getRect();
            String before = composerText();
            double[][] spots = {{0.06, 0.30}, {0.06, 0.40}, {0.06, 0.50}, {0.18, 0.40}, {0.06, 0.62}};
            for (double[] sp : spots) {
                tapAt(r.getX() + (int) (r.getWidth() * sp[0]), r.getY() + (int) (r.getHeight() * sp[1]));
                sleepMs(600);
                String after = composerText();
                if (after.length() > before.length() && after.startsWith(before)) {
                    return after.substring(before.length());
                }
            }
        } catch (WebDriverException ignored) { }
        return null;
    }

    public boolean tapEmojiBackspace() {
        boolean ok = tap("backPress");
        sleepMs(400);
        return ok;
    }

    // ---------------- reply bar ----------------

    /** Text of a view inside the composer's reply quote ({@code llReply}). */
    public String replyQuote(String id) {
        try {
            return driver.findElement(byId("llReply")).findElement(byId(id)).getText();
        } catch (WebDriverException e) {
            return null;
        }
    }

    public boolean closeReplyQuote() {
        boolean ok = tap("llDelete");
        sleepMs(600);
        return ok;
    }

    // ---------------- comments ----------------

    /** Taps the chevron ({@code deleteIv}) on the comment row containing the text. */
    public boolean openCommentMenu(String commentText) {
        String pkg = appPackage();
        // A comment row: tvMessage with the text and its sibling deleteIv.
        String xpath = "//*[@resource-id='" + pkg + ":id/tvMessage'][contains(@text,"
                + xpathLiteral(commentText) + ")]/../*[@resource-id='" + pkg + ":id/deleteIv']";
        for (int i = 0; i < 3; i++) {
            try {
                driver.findElement(AppiumBy.xpath(xpath)).click();
                if (isMenuOpen(Duration.ofSeconds(3))) return true;
            } catch (WebDriverException ignored) { }
            nudgeChatUp();
        }
        return false;
    }

    // ---------------- read by ----------------

    public boolean readByOpen(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            try {
                String a = driver.currentActivity();
                if (a != null && a.contains("ReadByUser")) return true;
            } catch (WebDriverException ignored) { }
            if (shownNow("readByRv")) return true;
            sleepMs(500);
        }
        return false;
    }

    /** Types into Read By's search widget (its field filters as you type). */
    public boolean readBySearch(String q) {
        try {
            WebElement f = driver.findElement(byId("search_widget_edit_textvw"));
            f.click();
            f.clear();
            f.sendKeys(q);
            sleepMs(1500);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    // ---------------- gallery (HomeChatLibraryActivity) ----------------

    /** Taps the library tab whose label starts with the text ("Docs" or "Docs (3)"). */
    public boolean tapLibraryTab(String label) {
        long deadline = System.currentTimeMillis() + 10000;   // the library opens as a new screen
        while (System.currentTimeMillis() < deadline) {
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiSelector().textStartsWith(\"" + label + "\")")).click();
                sleepMs(1200);
                return true;
            } catch (WebDriverException e) {
                sleepMs(500);
            }
        }
        return false;
    }

    /** The label of the library tab that starts with the text, e.g. "Docs (3)". */
    public String libraryTabLabel(String label) {
        textShown(label, Duration.ofSeconds(8));
        try {
            return driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().textStartsWith(\"" + label + "\")")).getText();
        } catch (WebDriverException e) {
            return null;
        }
    }

    public List<String> texts(String id) {
        try {
            return driver.findElements(byId(id)).stream().map(WebElement::getText)
                    .filter(t -> t != null && !t.isBlank()).toList();
        } catch (WebDriverException e) {
            return List.of();
        }
    }

    // ---------------- toasts, geometry, rows ----------------

    /** A Toast containing the text (UiAutomator2 exposes toasts as android.widget.Toast). */
    public boolean toastShown(String part, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            try {
                if (!driver.findElements(AppiumBy.xpath("//android.widget.Toast[contains(@text,"
                        + xpathLiteral(part) + ")]")).isEmpty()) return true;
            } catch (WebDriverException ignored) { }
            if (textShown(part, Duration.ofMillis(300))) return true;
            sleepMs(200);
        }
        return false;
    }

    /** On-screen bounds of the first displayed view with the id, or null. */
    public Rectangle rectOf(String id) {
        try {
            for (WebElement e : driver.findElements(byId(id))) {
                if (e.isDisplayed()) return e.getRect();
            }
        } catch (WebDriverException ignored) { }
        return null;
    }

    /** Bounds of the tvMessage holding the text, or null. */
    public Rectangle messageRect(String part) {
        try {
            return driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceIdMatches(\".*:id/tvMessage\").textContains(\""
                            + esc(part) + "\")")).getRect();
        } catch (WebDriverException e) {
            return null;
        }
    }

    public static boolean overlaps(Rectangle a, Rectangle b) {
        if (a == null || b == null) return false;
        return a.getX() < b.getX() + b.getWidth() && b.getX() < a.getX() + a.getWidth()
                && a.getY() < b.getY() + b.getHeight() && b.getY() < a.getY() + a.getHeight();
    }

    /** All visible tvMessage texts, top to bottom. */
    public List<String> visibleMessages() {
        return texts("tvMessage");
    }

    /** Pulls the feed down hard (towards the oldest row / pull-to-refresh). */
    public void flingToOlder() {
        try {
            WebElement rv = driver.findElement(byId("rvChat"));
            driver.executeScript("mobile: flingGesture", Map.of(
                    "elementId", ((org.openqa.selenium.remote.RemoteWebElement) rv).getId(),
                    "direction", "up", "speed", 6000));
        } catch (WebDriverException ignored) { }
        sleepMs(700);
    }

    /** Long-presses the newest view with the id (an image or audio bubble). */
    public boolean longPressNewest(String id) {
        hideKeyboardIfShown();
        try {
            List<WebElement> els = driver.findElements(byId(id));
            if (els.isEmpty()) return false;
            WebElement e = els.get(els.size() - 1);
            driver.executeScript("mobile: longClickGesture", Map.of(
                    "elementId", ((org.openqa.selenium.remote.RemoteWebElement) e).getId(),
                    "duration", 1200));
            return isMenuOpen(Duration.ofSeconds(4));
        } catch (WebDriverException e) {
            return false;
        }
    }

    /**
     * Long-presses the row that holds the newest view with the id, at points AROUND that view
     * (a play button or seek bar consumes the long-press itself).
     */
    public boolean longPressRowAround(String id) {
        hideKeyboardIfShown();
        try {
            List<WebElement> els = driver.findElements(byId(id));
            if (els.isEmpty()) return false;
            Rectangle r = els.get(els.size() - 1).getRect();
            // The row's long-press lives on mainLi; its sender name sits just above the play
            // button and passes the press up to it.
            int[][] pts = {{r.getX() + 150, r.getY() - 45},
                    {r.getX() + r.getWidth() / 2, r.getY() + r.getHeight() + 70},
                    {r.getX() + r.getWidth() + 120, r.getY() + r.getHeight() / 2}};
            for (int[] pt : pts) {
                longPressAt(pt[0], pt[1], 1300);
                if (isMenuOpen(Duration.ofSeconds(3))) return true;
            }
        } catch (WebDriverException ignored) { }
        return false;
    }

    public void longPressAt(int x, int y, int holdMs) {
        try {
            driver.executeScript("mobile: longClickGesture", Map.of("x", x, "y", y, "duration", holdMs));
            return;
        } catch (WebDriverException ignored) { }
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

    public boolean tapNewest(String id) {
        try {
            List<WebElement> els = driver.findElements(byId(id));
            if (els.isEmpty()) return false;
            els.get(els.size() - 1).click();
            sleepMs(1500);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Taps a row of a list screen (ForwardPage) by its exact label. */
    public boolean tapExactText(String label) {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().text(\"" + esc(label) + "\")")).click();
            sleepMs(1500);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Every visible TextView text on screen (for list screens owned by other modules). */
    public List<String> allTexts() {
        try {
            return driver.findElements(By.className("android.widget.TextView")).stream()
                    .map(WebElement::getText).filter(t -> t != null && !t.isBlank()).toList();
        } catch (WebDriverException e) {
            return List.of();
        }
    }

    public String currentActivity() {
        try { return driver.currentActivity(); } catch (WebDriverException e) { return ""; }
    }

    // ---------------- helpers ----------------

    public void tapAt(int x, int y) {
        var finger = new org.openqa.selenium.interactions.PointerInput(
                org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        var seq = new org.openqa.selenium.interactions.Sequence(finger, 1);
        seq.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, y));
        seq.addAction(finger.createPointerDown(0));
        seq.addAction(finger.createPointerUp(0));
        driver.perform(java.util.Collections.singletonList(seq));
    }

    /** Two taps on Send as fast as the device takes them (UiAutomator2 doubleClickGesture). */
    public void doubleTapSend() {
        WebElement send = driver.findElement(byId("ivSendMessage"));
        driver.executeScript("mobile: doubleClickGesture", Map.of(
                "elementId", ((org.openqa.selenium.remote.RemoteWebElement) send).getId()));
    }

    private static String xpathLiteral(String s) {
        if (!s.contains("'")) return "'" + s + "'";
        return "concat('" + s.replace("'", "',\"'\",'") + "')";
    }

    private static String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void sleepMs(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
