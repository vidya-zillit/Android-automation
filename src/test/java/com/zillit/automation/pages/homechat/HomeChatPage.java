package com.zillit.automation.pages.homechat;

import com.zillit.automation.pages.common.BasePage;
import com.zillit.automation.pages.common.BottomNavPage;
import com.zillit.automation.pages.common.ConfirmPopupPage;
import com.zillit.automation.pages.common.MediaPreviewPage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * The Home tab's chat: a horizontal strip of UNITS (recTab) and, per unit, a
 * message list (rvChat) with an input bar (etMessage / ivSendMessage /
 * ivAttachment). Reply is via long-press menu (rvVw rows, label mainTitle).
 */
public class HomeChatPage extends BasePage {

    public HomeChatPage(AndroidDriver driver) {
        super(driver);
    }

    // ---------- unit strip ----------

    /**
     * True once the Home units strip is up — self-healing onto the Home tab if it is not.
     *
     * <p><b>Why the self-heal.</b> {@code recTab} exists ONLY on the Home tab, but
     * "inside a project" is detected by the bottom bar, which is equally present on
     * Tools, Email, C&amp;C and Settings, and inside a unit's chat. So a session that
     * started with the app parked on another tab passed every "are we in a project"
     * check and then failed this one, and because the suites guard on it with
     * assumeTrue, the whole module reported SKIPPED rather than saying what was wrong.
     *
     * <p>Three steps, cheapest first: look; back out of a sub-screen that is covering
     * the nav; tap Home. Each is a no-op when it is not needed.
     */
    public boolean isLoaded() {
        if (isVisible("recTab", Duration.ofSeconds(8))) return true;

        // NO back-presses here, deliberately.
        //
        // This is a PREDICATE. Callers ask it freely, often inside loops — ChatOptionsTest's
        // ensureInChat() asks it up to six times while recovering from Forward/Share/Delete.
        // When this method pressed BACK to "unwind" on the caller's behalf, that recovery
        // loop became roughly twenty-four back-presses: chat → units → project list →
        // Start & Join → out of the app. The phone ended up on the launcher and every
        // later case reported "Home tab not reachable; bottom-nav tabs seen: []", which is
        // true and completely useless, because by then nothing was on screen to find.
        //
        // Tapping Home is safe — it cannot leave the project — so the tab correction stays.
        // Unwinding a sub-screen belongs to the caller, which knows what it opened and has
        // a relaunch to fall back on.
        if (driver.findElements(byId("bottomNavigation")).isEmpty()) {
            return false; // not on a bottom-nav screen at all — the caller decides what to do
        }
        BottomNavPage nav = new BottomNavPage(driver);
        if (nav.goHome()) {
            return isVisible("recTab", Duration.ofSeconds(10));
        }
        return false;
    }

    /**
     * The unit chips (Bulletin / Calendar / Call Sheet / …). IMPORTANT: the
     * units strip (recTab) and the BOTTOM-NAV tabs (Home/Email/Tools/C&C/
     * Settings) both use the id `menuText`, so we MUST scope the lookup to the
     * recTab container — otherwise selecting a "unit" taps a bottom-nav tab.
     */
    private List<WebElement> unitChipElements() {
        try {
            return driver.findElement(byId("recTab")).findElements(byId("menuText"));
        } catch (WebDriverException e) {
            return java.util.List.of();
        }
    }

    public List<String> visibleUnitNames() {
        return unitChipElements().stream()
                .map(WebElement::getText)
                .filter(t -> t != null && !t.isBlank())
                .toList();
    }

    /** Taps the unit CHIP whose label matches (case-insensitive contains). */
    public boolean selectUnit(String nameContains) {
        for (WebElement u : unitChipElements()) {
            String name = u.getText();
            if (name != null && name.toLowerCase().contains(nameContains.toLowerCase())) {
                u.click();
                sleep(800); // unit content swaps in
                return true;
            }
        }
        return false;
    }

    /** Selects the first unit that shows the chat input bar (not the calendar unit). */
    public boolean selectFirstChatUnit() {
        List<String> names = visibleUnitNames();
        for (String name : names) {
            if (name.toLowerCase().contains("calendar")) continue;
            if (selectUnit(name) && (isInputBarVisible() || isNoRightsBannerVisible())) {
                return true;
            }
        }
        return false;
    }

    // ---------- input bar state ----------

    public boolean isInputBarVisible() {
        return isVisible("etMessage", Duration.ofSeconds(6));
    }

    public boolean isNoRightsBannerVisible() {
        return isVisible("layNoMsg", Duration.ofSeconds(3));
    }

    public boolean isRequestRightsButtonVisible() {
        return isVisible("tvRequest", Duration.ofSeconds(3));
    }

    /** Classifies the selected unit's posting-rights state. */
    public enum Rights { POST, NO_RIGHTS, UNKNOWN }

    public Rights rightsState() {
        if (isInputBarVisible()) return Rights.POST;
        if (isNoRightsBannerVisible()) return Rights.NO_RIGHTS;
        return Rights.UNKNOWN;
    }

    /** Selects each unit in turn and returns name → rights state. */
    public java.util.Map<String, Rights> classifyAllUnits() {
        java.util.LinkedHashMap<String, Rights> map = new java.util.LinkedHashMap<>();
        for (String name : visibleUnitNames()) {
            if (name.toLowerCase().contains("calendar")) continue; // calendar isn't a chat unit
            if (selectUnit(name)) {
                sleep(600);
                map.put(name, rightsState());
            }
        }
        return map;
    }

    /** True if the messages list is present (viewing is possible even without rights). */
    public boolean canViewMessages() {
        return isVisible("rvChat", Duration.ofSeconds(5));
    }

    public boolean isSendButtonVisible() {
        return isVisible("ivSendMessage", Duration.ofSeconds(3));
    }

    public boolean isRecordButtonVisible() {
        return isVisible("ivRecord", Duration.ofSeconds(3));
    }

    public boolean isAttachmentVisible() {
        return isVisible("ivAttachment", Duration.ofSeconds(3));
    }

    // ---------- typing / sending ----------

    public void typeMessage(String text) {
        WebElement field = waitVisible("etMessage");
        field.click();
        field.clear();
        if (!text.isEmpty()) {
            field.sendKeys(text);
        }
    }

    public void clearMessage() {
        try {
            WebElement field = driver.findElement(byId("etMessage"));
            field.clear();
        } catch (WebDriverException ignored) {
        }
        // Also tap the clear-text button if present.
        try {
            driver.findElement(byId("clearTextBtn")).click();
        } catch (WebDriverException ignored) {
        }
    }

    public void tapSend() {
        waitVisible("ivSendMessage").click();
    }

    /**
     * Full image-send flow (attach → own gallery → pick 1 → preview → send).
     * Returns true if an image was sent. Best-effort — false if no gallery image.
     */
    public boolean sendOneImage(String caption, java.util.function.Consumer<Void> permHandler) {
        if (!isAttachmentVisible()) return false;
        tapAttachment();
        if (!isAttachmentPickerVisible()) return false;
        if (!tapAttachmentOption("Gallery")) { dismissAttachmentPicker(); return false; }
        if (permHandler != null) permHandler.accept(null);
        com.zillit.automation.pages.common.MediaPreviewPage media =
                new com.zillit.automation.pages.common.MediaPreviewPage(driver);
        if (!media.isGalleryOpen()) return false;
        media.openAllMediaAlbumIfListed();
        if (media.galleryThumbCount() == 0) { driver.navigate().back(); return false; }
        media.selectPhotos(1);
        if (!media.isSendFabVisible()) { driver.navigate().back(); return false; }
        media.tapGallerySendFab();
        if (!media.isPreviewOpen()) return false;
        media.setCaption(caption);
        media.tapPreviewSend();
        media.waitPreviewClosed(Duration.ofSeconds(60));
        return true;
    }

    // ---------- audio recording ----------

    public boolean isRecordButtonAvailable() {
        return isVisible("ivRecord", Duration.ofSeconds(4));
    }

    public void tapRecord() {
        waitVisible("ivRecord").click();
    }

    public boolean isRecordingActive(Duration timeout) {
        return isVisible("audioVw", timeout);
    }

    /**
     * Stops recording and posts the audio. Tapping send stops the recording and
     * shows a preview (playback + Cancel / "Post Audio"); "Post Audio" commits it.
     */
    public void sendRecording() {
        waitVisible("ivSendMessage").click();
        sleep(800);
        try {
            new WebDriverWait(driver, Duration.ofSeconds(6))
                    .until(ExpectedConditions.elementToBeClickable(AppiumBy.androidUIAutomator(
                            "new UiSelector().textMatches(\"(?i)post audio\")")))
                    .click();
            sleep(900);
        } catch (WebDriverException ignored) {
            // No preview step on this build — the send already committed.
        }
    }

    public void cancelRecording() {
        try {
            driver.findElement(byId("canelAudiRec")).click();
        } catch (WebDriverException ignored) {
        }
    }

    /** Sends text and returns after a short settle. */
    public void sendMessage(String text) {
        typeMessage(text);
        wait.until(ExpectedConditions.elementToBeClickable(byId("ivSendMessage")));
        tapSend();
        sleep(900);
    }

    // ---------------- DID IT ACTUALLY SEND? ----------------
    //
    // A bubble appearing in the list proves NOTHING about delivery: the app inserts it
    // locally the moment you tap send, then decorates it with a status. Reading only the
    // text is how a whole run of chat suites went green while every message sat
    // undelivered with a clock on it.
    //
    // What the app actually renders (TextVH.checkMessageUploadedOnServer, and the same
    // three-state pattern in AttachmentVH / AudioVH / the reply holders):
    //
    //     status -1  →  btnTryAgain visible          (failed — "Retry" offered)
    //     status  0  →  ivClock visible              (pending — the clock)
    //     status  1  →  btnTryAgain visible          (still not on the server)
    //     status  2  →  neither                      (SENT)
    //
    // So "sent" is the absence of both, ON THAT MESSAGE'S OWN ROW — never anywhere on
    // screen, because one stuck message would otherwise condemn every later assertion.

    /** The pending clock on the row carrying this text. */
    public boolean messagePending(String text) {
        return rowHasVisible(text, "ivClock");
    }

    /** The Retry control on the row carrying this text — the send failed. */
    public boolean messageFailed(String text) {
        // The btnTryAgain ConstraintLayout itself can be missing from the accessibility tree; its
        // "Retry" label is not (seen on emulator-5560: three offline rows showing "Retry" while
        // the id lookup reported "sent").
        return rowHasVisible(text, "btnTryAgain") || rowHasRetryLabel(text);
    }

    private boolean rowHasRetryLabel(String text) {
        String pkg = appPackage();
        String xpath = "//*[@resource-id='" + pkg + ":id/materialCardView']"
                + "[.//*[@text=" + xpathLiteral(text) + "]]//*[@text='Retry']";
        try {
            for (WebElement e : driver.findElements(AppiumBy.xpath(xpath))) if (e.isDisplayed()) return true;
        } catch (WebDriverException ignored) { }
        return retryBelowByGeometry(text);
    }

    /**
     * Geometry fallback: a "Retry" label (or btnTryAgain) drawn in the band just below this
     * message's text, inside the list. Needed because on emulator-5560 neither card-scoped XPath
     * matched rows that plainly showed "Retry" on screen (2026-11-24).
     */
    private boolean retryBelowByGeometry(String text) {
        try {
            List<WebElement> msgs = driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().text(\"" + escape(text) + "\")"));
            if (msgs.isEmpty()) return false;
            org.openqa.selenium.Rectangle m = msgs.get(msgs.size() - 1).getRect();
            List<WebElement> cands = new java.util.ArrayList<>(driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().text(\"Retry\")")));
            cands.addAll(driver.findElements(byId("btnTryAgain")));
            int top = m.getY() + m.getHeight() - 4, bottom = m.getY() + m.getHeight() + 160;
            for (WebElement c : cands) {
                org.openqa.selenium.Rectangle r = c.getRect();
                int cy = r.getY() + r.getHeight() / 2;
                if (r.getHeight() > 0 && cy >= top && cy <= bottom) return true;
            }
        } catch (WebDriverException ignored) { }
        return false;
    }

    /** Taps "Retry" on the failed row carrying the text (accepting a resend confirmation). */
    public boolean tapRetry(String text) {
        String pkg = appPackage();
        String base = "//*[@resource-id='" + pkg + ":id/materialCardView'][.//*[@text=" + xpathLiteral(text) + "]]";
        for (String tail : new String[]{"//*[@text='Retry']", "//*[@resource-id='" + pkg + ":id/btnTryAgain']"}) {
            try {
                List<WebElement> l = driver.findElements(AppiumBy.xpath(base + tail));
                if (!l.isEmpty()) {
                    l.get(0).click();
                    sleepQuiet(800);
                    confirmPopup().acceptIfShown(Duration.ofSeconds(3));
                    return true;
                }
            } catch (WebDriverException ignored) { }
        }
        // geometry fallback: the "Retry" label just below the message text
        try {
            List<WebElement> msgs = driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().text(\"" + escape(text) + "\")"));
            if (msgs.isEmpty()) return false;
            org.openqa.selenium.Rectangle m = msgs.get(msgs.size() - 1).getRect();
            for (WebElement c : driver.findElements(AppiumBy.androidUIAutomator("new UiSelector().text(\"Retry\")"))) {
                org.openqa.selenium.Rectangle r = c.getRect();
                int cy = r.getY() + r.getHeight() / 2;
                if (cy >= m.getY() + m.getHeight() - 4 && cy <= m.getY() + m.getHeight() + 160) {
                    c.click();
                    sleepQuiet(800);
                    confirmPopup().acceptIfShown(Duration.ofSeconds(3));
                    return true;
                }
            }
        } catch (WebDriverException ignored) { }
        return false;
    }

    /**
     * Waits until the message has genuinely reached the server — no clock, no Retry.
     *
     * <p>Use this instead of {@link #messageShown} wherever a test means "the message was
     * sent". {@code messageShown} answers a different, weaker question: "is the text on
     * screen".
     */
    public boolean waitMessageSent(String text, Duration timeout) {
        if (!messageShown(text, Duration.ofSeconds(Math.min(5, timeout.toSeconds())))) {
            // On a phone the IME stays up after Send and covers the newest row; a covered
            // view is absent from the accessibility tree (measured on the Pixel: the row's
            // tvUserName was there, its tvMessage was not). Close the IME, jump to latest.
            revealNewestRow();
            if (!messageShown(text, timeout)) return false;
        }
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (!messagePending(text) && !messageFailed(text)) return true;
            sleep(1000);
        }
        return false;
    }

    /**
     * Closes the soft keyboard (only when it is STABLY up — a BACK while it is already
     * closing would leave the chat) and taps the jump-to-latest arrow if it is showing.
     */
    public void revealNewestRow() {
        try {
            if (driver.isKeyboardShown()) {
                sleepQuiet(400);
                if (driver.isKeyboardShown()) { driver.navigate().back(); sleepQuiet(600); }
            }
        } catch (WebDriverException ignored) { }
        try {
            var arrow = driver.findElements(byId("arrowDownCard"));
            if (!arrow.isEmpty() && arrow.get(0).isDisplayed()) { arrow.get(0).click(); sleepQuiet(800); }
        } catch (WebDriverException ignored) { }
        // The newest row can still be clipped by the composer: its timeLi (clock / Retry) then
        // sits under layPostRights and is absent from the tree — measured on emulator-5560 with an
        // offline row, where every Retry lookup failed. A short scroll toward newer rows lifts it.
        try {
            WebElement rv = driver.findElement(byId("rvChat"));
            driver.executeScript("mobile: scrollGesture", java.util.Map.of(
                    "elementId", ((org.openqa.selenium.remote.RemoteWebElement) rv).getId(),
                    "direction", "down", "percent", 0.5));
            sleepQuiet(400);
        } catch (WebDriverException ignored) { }
    }

    /** Why a send is not complete — for a failure message that names the cause. */
    public String sendStateOf(String text) {
        if (!messageShown(text, Duration.ofSeconds(2))) return "not in the list at all";
        if (messageFailed(text)) return "FAILED — the row is offering Retry";
        if (messagePending(text)) return "PENDING — the row still shows the clock";
        return "sent";
    }

    /**
     * True when the message row containing {@code text} also contains a VISIBLE view with
     * the given id.
     *
     * <p>Anchored on the text and scoped to its own card: {@code ivClock} and
     * {@code btnTryAgain} exist on every sender row, so an unscoped lookup reports the
     * state of whichever message happens to be rendered first.
     */
    private boolean rowHasVisible(String text, String id) {
        String pkg = appPackage();
        // The card wrapping one message row; the text lives inside it.
        String xpath = "//*[@resource-id='" + pkg + ":id/materialCardView']"
                + "[.//*[@text=" + xpathLiteral(text) + "]]"
                + "//*[@resource-id='" + pkg + ":id/" + id + "']";
        try {
            for (WebElement e : driver.findElements(AppiumBy.xpath(xpath))) {
                if (e.isDisplayed()) return true;
            }
        } catch (WebDriverException ignored) {
            // Fall through: no row, or the tree changed under us — treated as "not shown".
        }
        return false;
    }

    /** XPath string literal that survives an apostrophe in the message. */
    private static String xpathLiteral(String s) {
        if (!s.contains("'")) return "'" + s + "'";
        return "concat('" + s.replace("'", "',\"'\",'") + "')";
    }

    /** True if a chat bubble containing the exact text is present in the list. */
    public boolean messageShown(String text, Duration timeout) {
        try {
            new WebDriverWait(driver, timeout).until(ExpectedConditions.presenceOfElementLocated(
                    AppiumBy.androidUIAutomator(
                            "new UiSelector().textContains(\"" + escape(text) + "\")")));
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /**
     * Like {@link #messageShown} but SCROLLS the conversation looking for the text.
     * A RecyclerView only materialises rows near the viewport, so a message that is
     * genuinely in the conversation but scrolled out of view is invisible to a plain
     * text lookup — which otherwise reads as "the message vanished". Scrolls back
     * (older) then forward (newer) before giving up.
     */
    public boolean messageShownScrolled(String text, int maxSwipes) {
        if (messageShown(text, Duration.ofSeconds(3))) return true;
        // search upwards (older messages)
        for (int i = 0; i < maxSwipes; i++) {
            swipeChat(true);
            if (messageShown(text, Duration.ofMillis(700))) return true;
        }
        // back down (newer)
        for (int i = 0; i < maxSwipes * 2; i++) {
            swipeChat(false);
            if (messageShown(text, Duration.ofMillis(700))) return true;
        }
        return false;
    }

    /** One chat swipe: up=true reveals OLDER messages, up=false reveals NEWER. */
    private void swipeChat(boolean older) {
        var size = driver.manage().window().getSize();
        int x = size.getWidth() / 2;
        int yFrom = (int) (size.getHeight() * (older ? 0.35 : 0.75));
        int yTo = (int) (size.getHeight() * (older ? 0.75 : 0.35));
        var finger = new org.openqa.selenium.interactions.PointerInput(
                org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        var seq = new org.openqa.selenium.interactions.Sequence(finger, 1);
        seq.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, yFrom));
        seq.addAction(finger.createPointerDown(0));
        seq.addAction(finger.createPointerMove(Duration.ofMillis(300),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, yTo));
        seq.addAction(finger.createPointerUp(0));
        driver.perform(java.util.Collections.singletonList(seq));
        sleepQuiet(500);
    }

    public int messageCount() {
        return driver.findElements(byId("rvChat")).isEmpty()
                ? 0
                : driver.findElement(byId("rvChat")).findElements(By.className("android.view.ViewGroup")).size();
    }

    // ---------- reply ----------

    /**
     * Nudges the chat list up a little so the newest bubble clears the input
     * bar. The freshly-sent message is pinned at the bottom, directly behind the
     * message box — a long-press on it lands on the input bar instead. A small
     * upward scroll lifts it into a pressable position. Safe no-op if empty.
     */
    public void nudgeChatUp() {
        try {
            WebElement rv = driver.findElement(byId("rvChat"));
            driver.executeScript("mobile: scrollGesture", java.util.Map.of(
                    "elementId", ((org.openqa.selenium.remote.RemoteWebElement) rv).getId(),
                    "direction", "up",
                    "percent", 0.25));
            sleepQuiet(400);
        } catch (WebDriverException ignored) {
        }
    }

    private void sleepQuiet(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Long-presses the newest message bubble containing the given text. */
    public boolean longPressMessage(String text) {
        // A raised keyboard (still up right after sending) pushes the newest bubble
        // under the IME so the press point misses — hide it first.
        hideKeyboard();
        sleepQuiet(400);
        // Long-press must register on both an emulator AND a physical device
        // (which has a longer threshold and may keep the bubble near the input
        // bar). Try the element long-click first; if the menu doesn't open, fall
        // back to a W3C coordinate long-press at the bubble's centre with a longer
        // hold, nudging the list up between attempts so the bubble is unobstructed.
        for (int attempt = 0; attempt < 4; attempt++) {
            try {
                WebElement bubble = driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiSelector().textContains(\"" + escape(text) + "\")"));
                if (attempt % 2 == 0) {
                    driver.executeScript("mobile: longClickGesture", java.util.Map.of(
                            "elementId", ((org.openqa.selenium.remote.RemoteWebElement) bubble).getId(),
                            "duration", 1200));
                } else {
                    org.openqa.selenium.Rectangle rect = bubble.getRect();
                    w3cLongPress(rect.getX() + rect.getWidth() / 2,
                            rect.getY() + rect.getHeight() / 2, 1200);
                }
                if (isMenuOpen(Duration.ofSeconds(3))) return true;
            } catch (WebDriverException e) {
                // fall through to the nudge + retry
            }
            nudgeChatUp();
        }
        return isMenuOpen(Duration.ofSeconds(1));
    }

    /**
     * Long-presses near the LEFT edge of the first line of the bubble carrying the text. Use it for
     * a message that contains a URL: the centre of an autolinked text is often the link, and a
     * long-press there never opens the message menu.
     */
    public boolean longPressMessageLeft(String text) {
        hideKeyboard();
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                WebElement el = driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiSelector().textContains(\"" + escape(text) + "\")"));
                org.openqa.selenium.Rectangle r = el.getRect();
                w3cLongPress(r.getX() + Math.max(8, r.getWidth() / 12), r.getY() + Math.min(20, r.getHeight() / 2), 1200);
                if (isMenuOpen(Duration.ofSeconds(3))) return true;
            } catch (WebDriverException ignored) { }
            nudgeChatUp();
        }
        return false;
    }

    /**
     * Deletes the newest image/media bubble whose size label equals {@code sizeLabel}
     * (e.g. "File Size : 1.44 KB" — the runner's zl_test_*.jpg fixture). Returns true when one went.
     */
    public boolean deleteNewestMediaWithSize(String sizeLabel) {
        revealNewestRow();
        try {
            List<WebElement> sizes = driver.findElements(byId("tvFileSize"));
            for (int i = sizes.size() - 1; i >= 0; i--) {
                if (!sizeLabel.equals(sizes.get(i).getText())) continue;
                org.openqa.selenium.Rectangle sr = sizes.get(i).getRect();
                // the bubble sits right above its size label
                w3cLongPress(sr.getX() + sr.getWidth() / 2, Math.max(0, sr.getY() - 120), 1200);
                if (!isMenuOpen(Duration.ofSeconds(3))) return false;
                if (!tapMenuOption("Delete")) { closeMenu(); return false; }
                tapDeleteActionBar(Duration.ofSeconds(5));
                confirmPopup().acceptIfShown(Duration.ofSeconds(5));
                sleepQuiet(1500);
                if (inSelectionMode(Duration.ofMillis(500))) exitSelectionMode();
                return true;
            }
        } catch (WebDriverException ignored) { }
        return false;
    }

    /** Long-presses the LAST (newest) bubble containing the text — e.g. a forwarded copy. */
    public boolean longPressLastMessage(String text) {
        hideKeyboard();
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                List<WebElement> all = driver.findElements(AppiumBy.androidUIAutomator(
                        "new UiSelector().textContains(\"" + escape(text) + "\")"));
                if (!all.isEmpty()) {
                    org.openqa.selenium.Rectangle r = all.get(all.size() - 1).getRect();
                    w3cLongPress(r.getX() + r.getWidth() / 2, r.getY() + r.getHeight() / 2, 1200);
                    if (isMenuOpen(Duration.ofSeconds(3))) return true;
                }
            } catch (WebDriverException ignored) { }
            nudgeChatUp();
        }
        return false;
    }

    /** Scrolls a chat message containing the text into view (best-effort). */
    private void scrollMessageIntoView(String text) {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().resourceIdMatches(\".*rvChat\"))"
                            + ".scrollIntoView(new UiSelector().textContains(\"" + escape(text) + "\"))"));
            sleep(400);
        } catch (WebDriverException ignored) {
            // Already visible or not scrollable — the long-press loop still tries.
        }
    }

    /** A W3C touch long-press at absolute screen coordinates. */
    private void w3cLongPress(int x, int y, int holdMs) {
        org.openqa.selenium.interactions.PointerInput finger =
                new org.openqa.selenium.interactions.PointerInput(
                        org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        org.openqa.selenium.interactions.Sequence seq =
                new org.openqa.selenium.interactions.Sequence(finger, 1);
        seq.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, y));
        seq.addAction(finger.createPointerDown(0));
        seq.addAction(finger.createPointerMove(Duration.ofMillis(holdMs),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, y));
        seq.addAction(finger.createPointerUp(0));
        driver.perform(java.util.Collections.singletonList(seq));
    }

    /** Long-presses the first visible message bubble in the list. */
    public boolean longPressFirstMessage() {
        var msgs = driver.findElement(byId("rvChat"))
                .findElements(By.className("android.view.ViewGroup"));
        if (msgs.isEmpty()) return false;
        try {
            driver.executeScript("mobile: longClickGesture", java.util.Map.of(
                    "elementId", ((org.openqa.selenium.remote.RemoteWebElement) msgs.get(0)).getId(),
                    "duration", 800));
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Taps a row in the long-press dropdown by its label (e.g. "Reply", "Copy"). */
    public boolean tapMenuOption(String label) {
        try {
            wait.until(ExpectedConditions.elementToBeClickable(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceIdMatches(\".*mainTitle\").textMatches(\"(?i)"
                            + label + "\")"))).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** All option labels currently shown in the long-press dropdown menu. */
    public List<String> menuOptions() {
        return driver.findElements(byId("mainTitle")).stream()
                .map(WebElement::getText)
                .filter(t -> t != null && !t.isBlank())
                .toList();
    }

    public boolean isMenuOpen(Duration timeout) {
        return isVisible("rvVw", timeout);
    }

    /**
     * DELETE is a two-stage flow on this build: tapping the menu "Delete" enters
     * a selection/action-bar mode (toolbar Cancel + a Delete action, id
     * {@code tvAction} inside a clickable {@code layButton}); tapping THAT action
     * is what raises the final "Are you sure you want to delete? / No / Yes"
     * dialog. This taps the action-bar Delete. Best-effort: returns false (and is
     * a safe no-op) if the flow instead went straight to the Yes/No dialog.
     * Targets the action-bar Delete by its id ({@code tvAction}) — NOT a generic
     * "Delete" text — so it never re-taps the menu row or the dialog title.
     */
    public boolean tapDeleteActionBar(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        By action = AppiumBy.androidUIAutomator(
                "new UiSelector().resourceIdMatches(\".*tvAction\").textMatches(\"(?i)delete\")");
        while (System.currentTimeMillis() < deadline) {
            try {
                driver.findElement(action).click();
                return true;
            } catch (WebDriverException notYet) {
                sleepQuiet(300);
            }
        }
        return false;
    }

    // ---------- edit (Update) dialog ----------

    public boolean isEditDialogOpen(Duration timeout) {
        // The "Edit Message" dialog is an overlay: page source often exposes only
        // its inner field (editTextInput). Detect by that or the dialog chrome.
        return isVisible("editTextInput", timeout)
                || textShown("Edit Message", Duration.ofSeconds(2))
                || (isVisible("rightButton", Duration.ofSeconds(2)) && isVisible("message", Duration.ofSeconds(1)));
    }

    /** Replaces the edit-dialog text with newText. */
    public void setEditDialogText(String newText) {
        WebElement field;
        try {
            field = driver.findElement(byId("editTextInput"));
        } catch (WebDriverException notInner) {
            field = driver.findElement(byId("message")).findElement(By.className("android.widget.EditText"));
        }
        field.clear();
        field.sendKeys(newText);
        hideKeyboard();
    }

    public void saveEditDialog() {
        // "Update" button = rightButton; click by id, else by its label text.
        try {
            driver.findElement(byId("rightButton")).click();
        } catch (WebDriverException e) {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().clickable(true).textMatches(\"(?i)update\")")).click();
        }
    }

    public void cancelEditDialog() {
        try {
            driver.findElement(byId("leftButton")).click();
        } catch (WebDriverException e) {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().clickable(true).textMatches(\"(?i)cancel\")")).click();
        }
    }

    public void closeMenu() {
        driver.navigate().back();
    }

    public boolean isReplyPreviewVisible() {
        return isVisible("llReply", Duration.ofSeconds(5));
    }

    public void cancelReplyPreview() {
        try {
            driver.findElement(byId("llDelete")).click();
        } catch (WebDriverException ignored) {
        }
    }

    // ---------- long-press option flows (Forward / Gallery / Read By User / Share) ----------

    private boolean currentActivityContains(String needle) {
        try {
            String a = driver.currentActivity();
            return a != null && a.toLowerCase().contains(needle.toLowerCase());
        } catch (WebDriverException e) {
            return false;
        }
    }

    /**
     * FORWARD: long-press → Forward → Yes/No confirm → Yes → ForwardPage
     * (recipient picker, list rvView). Returns true if ForwardPage opened.
     * Leaves you on ForwardPage — call {@link #leaveOverlayScreen()} to back out
     * WITHOUT forwarding.
     */
    public boolean forwardOpensPage(String text) {
        if (!longPressMessage(text)) return false;
        if (!tapMenuOption("Forward")) return false;
        // "Are you sure you want to forward?" — the app's own popup, which needs a REAL
        // wait: it renders a beat after the menu tap, and the old instant text lookup
        // usually missed it. A missed confirmation is not a silent no-op either — the
        // dialog is modal, so it blocks every later case in the suite.
        confirmPopup().acceptIfShown(Duration.ofSeconds(8));
        sleepQuiet(2000);
        return currentActivityContains("ForwardPage") || isVisible("rvView", Duration.ofSeconds(4));
    }

    /** The app's shared confirmation dialog (delete / forward / share / leave). */
    public ConfirmPopupPage confirmPopup() {
        return new ConfirmPopupPage(driver);
    }

    /**
     * Clears a confirmation left blocking the chat — call it in recovery paths so one
     * unhandled popup cannot cascade into a suite full of skips.
     */
    public boolean clearBlockingPopup() {
        return confirmPopup().clearIfBlocking();
    }

    /**
     * GALLERY: long-press → Gallery → opens the chat library (media/docs/links
     * viewer, HomeChatLibraryActivity). Returns true if the library opened.
     */
    public boolean galleryOpensLibrary(String text) {
        if (!longPressMessage(text)) return false;
        if (!tapMenuOption("Gallery")) return false;
        sleepQuiet(2500);
        return currentActivityContains("Library")
                || isVisible("mediaDocsLinksBtn", Duration.ofSeconds(3))
                || textShown("Media", Duration.ofSeconds(3))
                || textShown("Docs", Duration.ofSeconds(2))
                || textShown("Links", Duration.ofSeconds(2))
                || textShown("Gallery", Duration.ofSeconds(2));
    }

    /**
     * READ BY USER: long-press → Read By User → ReadByUserPage (list readByRv,
     * delivered/read sections). Returns true if the read-by screen opened.
     */
    public boolean readByOpensPage(String text) {
        if (!longPressMessage(text)) return false;
        if (!tapMenuOption("Read By User")) return false;
        sleepQuiet(2000);
        return currentActivityContains("ReadByUser")
                || isVisible("readByRv", Duration.ofSeconds(4))
                || textShown("Read By", Duration.ofSeconds(3))
                || textShown("Delivered", Duration.ofSeconds(2));
    }

    /**
     * SHARE: long-press → Share → the in-app share action bar (tvAction = "Share").
     * Returns true if the share flow started.
     */
    public boolean shareStarts(String text) {
        if (!longPressMessage(text)) return false;
        if (!tapMenuOption("Share")) return false;
        // Share confirms first on some builds — accept it if it appears, and leave the
        // screen clean if it does not.
        confirmPopup().acceptIfShown(Duration.ofSeconds(6));
        sleepQuiet(1500);
        // action-bar Share (tvAction) or an OS/app chooser
        try {
            if (!driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceIdMatches(\".*tvAction\").textMatches(\"(?i)share\")")).isEmpty())
                return true;
        } catch (WebDriverException ignored) {}
        return currentActivityContains("share") || !currentActivityContains("home") || textShown("Share", Duration.ofSeconds(3));
    }

    /** Back out of a full-screen overlay (ForwardPage / Library / ReadByUser) to the chat. */
    public void leaveOverlayScreen() {
        // A modal confirmation swallows BACK, so a leftover popup would keep this loop
        // pressing back against a dialog and return with the chat still unreachable.
        clearBlockingPopup();
        for (int i = 0; i < 4 && !isInputBarVisible(); i++) {
            try { driver.navigate().back(); } catch (WebDriverException ignored) {}
            sleepQuiet(700);
        }
    }

    // ---------- sent media in chat (post-send) ----------

    /** True once a media (image/attachment) bubble is present in the chat. */
    public boolean mediaBubbleShown(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (!driver.findElements(byId("imageParent")).isEmpty()
                    || !driver.findElements(byId("attachmentType")).isEmpty()
                    || !driver.findElements(byId("recAttachment")).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** True if an upload/prepare loader is (or was) visible on a media bubble. */
    public boolean uploadLoaderShown(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (!driver.findElements(byId("uploadingProgress")).isEmpty()
                    || !driver.findElements(byId("progressBar")).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** Taps the newest media bubble to open the full-screen viewer. */
    public boolean tapNewestMediaBubble() {
        var bubbles = driver.findElements(byId("imageParent"));
        if (bubbles.isEmpty()) bubbles = driver.findElements(byId("imageRl"));
        if (bubbles.isEmpty()) return false;
        bubbles.get(bubbles.size() - 1).click();
        sleep(900);
        return true;
    }

    // ---------- per-type bubble counting (for cross-device receipt) ----------

    /** Number of elements currently rendered with the given resource id. */
    public int countOf(String id) {
        try {
            return driver.findElements(byId(id)).size();
        } catch (WebDriverException e) {
            return 0;
        }
    }

    public int imageBubbleCount() { return countOf("imageParent"); }   // chat_attachment_view
    public int audioBubbleCount() {                                     // chat_audio_layout
        int n = countOf("audioVw");
        if (n == 0) n = countOf("ivPlayPause");
        if (n == 0) n = countOf("playPauseIcon");
        if (n == 0) n = countOf("durationCount");
        return n;
    }
    public int docBubbleCount() {                                       // document bubble
        int n = countOf("fileNameTv");
        if (n == 0) n = countOf("tvFileSize");
        return n;
    }

    // ---------- OPEN / PLAY the media (verify it's actually usable) ----------

    /** Full-screen image/photo viewer is open (photoView / ivImage / view_image). */
    public boolean isImageViewerOpen(Duration timeout) {
        return isVisible("photoView", timeout)
                || isVisible("ivImage", Duration.ofMillis(600))
                || isVisible("view_image", Duration.ofMillis(600));
    }

    /** A video/media player surface is open (videoViewMain / videoVw / audioPlayer). */
    public boolean isPlayerOpen(Duration timeout) {
        return isVisible("videoViewMain", timeout)
                || isVisible("videoVw", Duration.ofMillis(600))
                || isVisible("audioPlayer", Duration.ofMillis(600));
    }

    /** The full-screen media viewer activity (GalleryItemView) is on top. */
    private boolean viewerActivityOpen() {
        try {
            String a = driver.currentActivity();
            return a != null && (a.contains("Gallery") || a.contains("ImageView")
                    || a.contains("MediaView") || a.contains("VideoView"));
        } catch (WebDriverException e) {
            return false;
        }
    }

    /**
     * Taps the newest image bubble and confirms it opens (full-screen viewer or
     * the GalleryItemView activity). A received image may need a download tap
     * first; tries the card / image-row targets since the exact clickable child
     * varies. Best-effort — returns whether a viewer actually opened.
     */
    public boolean openNewestImage() {
        tapDownloadIfPresent();
        for (String id : new String[]{"imageParent", "imageRl", "materialCardView"}) {
            var bubbles = driver.findElements(byId(id));
            if (bubbles.isEmpty()) continue;
            try {
                bubbles.get(bubbles.size() - 1).click();
            } catch (WebDriverException e) {
                continue;
            }
            if (viewerActivityOpen() || isImageViewerOpen(Duration.ofSeconds(3))
                    || isPlayerOpen(Duration.ofSeconds(1))) {
                return true;
            }
        }
        return false;
    }

    /** Taps a download control on the newest media bubble if one is shown. */
    public void tapDownloadIfPresent() {
        for (String id : new String[]{"downloadImage", "downLoadView", "mainDownloadView"}) {
            var d = driver.findElements(byId(id));
            if (!d.isEmpty()) {
                try {
                    d.get(d.size() - 1).click();
                    sleep(1500); // let the download finish
                } catch (WebDriverException ignored) {
                }
                return;
            }
        }
    }

    /** An audio (voice-note) bubble with a play control is present. */
    public boolean isAudioBubblePresent(Duration timeout) {
        return isVisible("ivPlayPause", timeout);
    }

    /**
     * Taps play on the newest audio bubble. Returns true if the control was
     * tapped (the bubble is playable). Downloads-then-plays on the receiver, so
     * we tap once and treat presence of the play control as playable.
     */
    public boolean playNewestAudio() {
        var play = driver.findElements(byId("ivPlayPause"));
        if (play.isEmpty()) return false;
        try {
            play.get(play.size() - 1).click();
            sleep(800);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /**
     * Opens the newest document bubble and reports whether ANYTHING opened.
     *
     * <p>Office files (docx/xlsx/pptx…) open the app's own editor read-only
     * ({@code OnlyOfficeEditorActivity}); PDF/txt and every fallback open in another app.
     * Either way something comes to the front, and that is what this waits for (25 s: the
     * chat asks Drive for the editor config before any screen appears). Use
     * {@code ChatDocumentEditorPage.waitForOutcome} after {@link #tapNewestDocBubble()} to
     * tell WHICH of the two happened.
     *
     * @return false when there is no document bubble, the tap failed, or nothing opened.
     */
    public boolean openNewestDoc() {
        String before = safeActivity();
        if (!tapNewestDocBubble()) return false;
        String app = appPackage();
        long deadline = System.currentTimeMillis() + 25000;
        while (System.currentTimeMillis() < deadline) {
            String pkg = safeCurrentPackage();
            if (pkg != null && !pkg.isEmpty() && !pkg.equals(app)) return true;   // another app
            String act = safeActivity();
            if (!act.isEmpty() && !act.equals(before)) return true;               // our editor
            if (!driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().textContains(\"proper application for viewing\")")).isEmpty()) {
                return false;                                                     // "no app" toast
            }
            sleep(500);
        }
        return false;
    }

    /**
     * Taps the newest document bubble on its REAL click target.
     *
     * <p>{@code AttachmentVH} (chat_attachment_view.xml) attaches the click listener only to
     * {@code landScapeView} / {@code portraitView} / {@code centerView} / {@code ivPlay}, all
     * inside {@code imageParent}. {@code fileNameTv} (Call Sheet / Production Report units only)
     * and the {@code attachmentType} overlay have NO listener, so the old target only worked by
     * accident. Measured on device (2026-11-23): a docx renders in {@code landScapeView}, filling
     * {@code imageParent}, with {@code attachmentType} drawn in its bottom-left corner — so the
     * target is the bottom-most {@code imageParent} that carries an {@code attachmentType} overlay
     * (a document, not a photo), tapped in its centre.
     *
     * @return true once a tap was sent to a document bubble.
     */
    public boolean tapNewestDocBubble() {
        WebElement target = null;
        long deadline = System.currentTimeMillis() + 10000;
        while (target == null && System.currentTimeMillis() < deadline) {
            target = newestDocBubble();
            if (target != null) break;
            // The list can sit scrolled up (keyboard just closed, a new row inserted below the
            // fold): the "jump to latest" arrow brings the newest message back on screen.
            try {
                var arrow = driver.findElements(byId("arrowDownCard"));
                if (!arrow.isEmpty() && arrow.get(0).isDisplayed()) arrow.get(0).click();
            } catch (WebDriverException ignored) { }
            sleep(800);
        }
        if (target == null) return false;
        try {
            org.openqa.selenium.Rectangle r = target.getRect();
            tapAt(r.getX() + r.getWidth() / 2, r.getY() + r.getHeight() / 2);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /**
     * Long-presses the newest DOCUMENT bubble (not just the newest media, which may be someone
     * else's photo) and reports whether the options menu opened.
     */
    public boolean longPressNewestDocBubble() {
        WebElement target = newestDocBubble();
        if (target == null) return false;
        try {
            org.openqa.selenium.Rectangle r = target.getRect();
            w3cLongPress(r.getX() + r.getWidth() / 2, r.getY() + r.getHeight() / 2, 1200);
            return isMenuOpen(Duration.ofSeconds(4));
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Document bubbles on screen: {@code imageParent}s carrying an {@code attachmentType} badge. */
    public int docBubbleCountByBadge() {
        return countOf("attachmentType");
    }

    /** The bottom-most {@code imageParent} that has an {@code attachmentType} overlay inside it. */
    private WebElement newestDocBubble() {
        List<org.openqa.selenium.Rectangle> overlays = new java.util.ArrayList<>();
        try {
            for (WebElement t : driver.findElements(byId("attachmentType"))) {
                if (t.isDisplayed()) overlays.add(t.getRect());
            }
        } catch (WebDriverException ignored) { }
        WebElement best = null;
        int bestBottom = -1;
        try {
            for (WebElement p : driver.findElements(byId("imageParent"))) {
                org.openqa.selenium.Rectangle r = p.getRect();
                if (r.getHeight() <= 0) continue;
                // The badge's centre lies inside the bubble (it sits in the bottom-left corner).
                boolean isDoc = overlays.stream().anyMatch(o -> {
                    int cx = o.getX() + o.getWidth() / 2, cy = o.getY() + o.getHeight() / 2;
                    return cx >= r.getX() && cx <= r.getX() + r.getWidth()
                            && cy >= r.getY() && cy <= r.getY() + r.getHeight();
                });
                int bottom = r.getY() + r.getHeight();
                if (isDoc && bottom > bestBottom) { best = p; bestBottom = bottom; }
            }
        } catch (WebDriverException ignored) { }
        return best;
    }

    private void tapAt(int x, int y) {
        var finger = new org.openqa.selenium.interactions.PointerInput(
                org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        var seq = new org.openqa.selenium.interactions.Sequence(finger, 1);
        seq.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, y));
        seq.addAction(finger.createPointerDown(0));
        seq.addAction(finger.createPointerMove(Duration.ofMillis(80),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, y));
        seq.addAction(finger.createPointerUp(0));
        driver.perform(java.util.Collections.singletonList(seq));
    }

    private String safeActivity() {
        try {
            String a = driver.currentActivity();
            return a == null ? "" : a;
        } catch (WebDriverException e) {
            return "";
        }
    }

    // ---------- "you pick, automation sends" send screens ----------

    /** Media (image/video) preview screen is up (shared caption + send button). */
    public boolean isMediaPreviewUp(Duration timeout) {
        return isVisible("description", timeout) && isVisible("sendBtn", Duration.ofSeconds(3));
    }

    /** The map's "send this location" button is up. */
    public boolean isLocationSendUp(Duration timeout) {
        return isVisible("sendLocation", timeout);
    }

    public boolean tapLocationSend() {
        try {
            driver.findElement(byId("sendLocation")).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** The contact-confirm screen is up (contact name + send button). */
    public boolean isContactSendUp(Duration timeout) {
        return isVisible("contactName", timeout) && isVisible("sendBtn", Duration.ofSeconds(3));
    }

    /** Taps the shared preview/contact "send" button (id sendBtn). */
    public boolean tapSendBtn() {
        try {
            driver.findElement(byId("sendBtn")).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public void setPreviewCaption(String text) {
        try {
            WebElement f = driver.findElement(byId("description"));
            f.click();
            f.clear();
            if (text != null && !text.isEmpty()) f.sendKeys(text);
            hideKeyboard();
        } catch (WebDriverException ignored) {
        }
    }

    /** Total message rows currently rendered (for a "a new message posted" check). */
    public int chatRowCount() {
        try {
            return driver.findElement(byId("rvChat"))
                    .findElements(By.className("android.view.ViewGroup")).size();
        } catch (WebDriverException e) {
            return 0;
        }
    }

    /** Closes an open full-screen viewer/player and returns to the chat. */
    public void closeViewer() {
        for (int i = 0; i < 2 && !isInputBarVisible(); i++) {
            try {
                driver.navigate().back();
            } catch (WebDriverException ignored) {
            }
            sleep(600);
        }
    }

    /** True if a download control is present on a received media bubble. */
    public boolean isDownloadControlPresent() {
        return !driver.findElements(byId("downloadImage")).isEmpty()
                || !driver.findElements(byId("downLoadView")).isEmpty();
    }

    public boolean tapDownload() {
        var d = driver.findElements(byId("downloadImage"));
        if (d.isEmpty()) d = driver.findElements(byId("downLoadView"));
        if (d.isEmpty()) return false;
        d.get(d.size() - 1).click();
        return true;
    }

    /** Long-presses the newest media bubble (for reply/options on media). */
    public boolean longPressNewestMedia() {
        // Robust like longPressMessage: a fire-and-forget 800ms gesture often did
        // NOT open the menu (esp. on a physical device), yet returned true — so
        // the reply/options assertions ran against a menu that was never open.
        // Retry with a longer element long-click, then a W3C coordinate press,
        // and only report success when the menu is actually open.
        for (int attempt = 0; attempt < 4; attempt++) {
            var bubbles = driver.findElements(byId("imageParent"));
            if (bubbles.isEmpty()) bubbles = driver.findElements(byId("attachmentType"));
            if (bubbles.isEmpty()) return false;
            WebElement el = bubbles.get(bubbles.size() - 1);
            try {
                if (attempt % 2 == 0) {
                    driver.executeScript("mobile: longClickGesture", java.util.Map.of(
                            "elementId", ((org.openqa.selenium.remote.RemoteWebElement) el).getId(),
                            "duration", 1200));
                } else {
                    org.openqa.selenium.Rectangle r = el.getRect();
                    w3cLongPress(r.getX() + r.getWidth() / 2, r.getY() + r.getHeight() / 2, 1200);
                }
                if (isMenuOpen(Duration.ofSeconds(3))) return true;
            } catch (WebDriverException e) {
                // retry
            }
            nudgeChatUp();
        }
        return isMenuOpen(Duration.ofSeconds(1));
    }

    // ---------- attachment ----------

    public void tapAttachment() {
        waitVisible("ivAttachment").click();
    }

    public boolean isAttachmentPickerVisible() {
        return isVisible("rvItems", Duration.ofSeconds(6));
    }

    public List<String> attachmentOptions() {
        return driver.findElements(byId("tvLabel")).stream()
                .map(WebElement::getText)
                .filter(t -> t != null && !t.isBlank())
                .toList();
    }

    public void dismissAttachmentPicker() {
        driver.navigate().back();
    }

    /**
     * In an open Android SYSTEM picker, taps the first item whose text contains namePart.
     * Returns false — and hands the screen back to the app — when the file is not there.
     *
     * @see BasePage#pickSystemFile(String, java.time.Duration)
     */
    public boolean pickSystemFileByName(String namePart, Duration timeout) {
        return pickSystemFile(namePart, timeout);
    }

    /** True once a sent attachment (media/doc) bubble or its loader is present. */
    public boolean attachmentPostedOrUploading(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (!driver.findElements(byId("attachmentType")).isEmpty()
                    || !driver.findElements(byId("recAttachment")).isEmpty()
                    || !driver.findElements(byId("tvFileSize")).isEmpty()
                    || !driver.findElements(byId("uploadingProgress")).isEmpty()
                    || !driver.findElements(byId("imageParent")).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Waits for an attachment to be FULLY SENT — the bubble is there, and it is no longer
     * uploading and not offering Retry.
     *
     * <p>{@link #attachmentPostedOrUploading} deliberately accepts an in-flight upload,
     * which makes it useful as a "did the send start" check and useless as proof of
     * delivery: an upload that later fails satisfies it forever. {@code AttachmentVH}
     * drives the same three states as a text row — {@code ivClock} pending,
     * {@code btnTryAgain} failed, neither = sent — so require the finished state.
     */
    public boolean waitAttachmentSent(Duration timeout) {
        if (!attachmentPostedOrUploading(Duration.ofSeconds(Math.min(20, timeout.toSeconds())))) {
            return false;
        }
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (!anyVisible("uploadingProgress") && !anyVisible("btnTryAgain") && !anyVisible("ivClock")) {
                return true;
            }
            sleepQuiet(1000);
        }
        return false;
    }

    /** Why an attachment send is not finished — for a failure message that names it. */
    public String attachmentSendState() {
        if (anyVisible("btnTryAgain")) return "FAILED — the bubble is offering Retry";
        if (anyVisible("uploadingProgress")) return "still UPLOADING";
        if (anyVisible("ivClock")) return "PENDING — the clock is still showing";
        if (!driver.findElements(byId("attachmentType")).isEmpty()
                || !driver.findElements(byId("imageParent")).isEmpty()) return "sent";
        return "no attachment bubble at all";
    }

    private boolean anyVisible(String id) {
        try {
            for (WebElement e : driver.findElements(byId(id))) {
                if (e.isDisplayed()) return true;
            }
        } catch (WebDriverException ignored) { }
        return false;
    }

    /** The 8 media options a normal unit's picker must offer. */
    public static final List<String> EXPECTED_MEDIA = List.of(
            "Photo", "Gallery", "Video", "Video Gallery",
            "Audio", "Document", "Location", "Contact");

    /** Taps a picker option by its exact label (e.g. "Document"). */
    public boolean tapAttachmentOption(String label) {
        for (WebElement o : driver.findElements(byId("tvLabel"))) {
            if (label.equalsIgnoreCase(o.getText())) {
                o.click();
                return true;
            }
        }
        return false;
    }

    /**
     * After tapping a media option, returns true if SOMETHING opened without
     * crashing — either a system picker/camera (foreground app changed) or an
     * in-app screen / permission dialog. Returns false only if the app died.
     */
    public boolean optionOpenedSomething(Duration settle) {
        sleep(settle.toMillis());
        String pkg = safeCurrentPackage();
        // Crash / unexpected app death would drop us to the launcher.
        boolean atLauncher = pkg != null && pkg.contains("launcher");
        return !atLauncher;
    }

    public String safeCurrentPackage() {
        try {
            return driver.getCurrentPackage();
        } catch (WebDriverException e) {
            return null;
        }
    }

    // ---------- call-sheet document-upload popups ----------

    /** Selects the call-sheet unit (name contains "call"); false if none. */
    public boolean selectCallSheetUnit() {
        for (String name : visibleUnitNames()) {
            if (name.toLowerCase().contains("call")) {
                selectUnit(name);
                return isInputBarVisible() || isNoRightsBannerVisible();
            }
        }
        return false;
    }

    public boolean isCallSheetFirstPopupShown(Duration timeout) {
        return textShown("continuation of the existing Call Sheet", timeout);
    }

    /** Call-sheet History button (userHistoryBtn) — views replaced call sheets. */
    public boolean isHistoryButtonVisible() {
        return isVisible("userHistoryBtn", Duration.ofSeconds(5));
    }

    public void tapHistory() {
        waitVisible("userHistoryBtn").click();
    }

    /** A document/attachment bubble (file-size label or attachment type) is present. */
    public boolean isDocBubbleShown(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (!driver.findElements(byId("tvFileSize")).isEmpty()
                    || !driver.findElements(byId("attachmentType")).isEmpty()
                    || !driver.findElements(byId("imageParent")).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public boolean isCallSheetReplaceWarningShown(Duration timeout) {
        return textShown("replaced with the new upload", timeout);
    }

    /** The confirm dialog's left button ("Continuation" / "No"). */
    public void tapDialogLeft() {
        waitVisible("leftButton").click();
    }

    /** The confirm dialog's right button ("New" / "Yes"). */
    public void tapDialogRight() {
        waitVisible("rightButton").click();
    }

    /** The extra Cancel button (viewBtn) on the first call-sheet popup. */
    public void tapDialogCancel() {
        waitVisible("viewBtn").click();
    }

    /** True if the open attachment picker offers ONLY the Document option. */
    public boolean isDocumentOnlyPicker() {
        List<String> opts = attachmentOptions();
        return opts.size() == 1 && opts.get(0).equalsIgnoreCase("Document");
    }

    /** Returns to the chat from whatever a media option opened (system or in-app). */
    public void returnToChat(String appPackage) {
        for (int i = 0; i < 4; i++) {
            if (isInputBarVisible()) return;
            // Allow any permission dialog first, then back out.
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiSelector().textMatches(\"(?i)(allow|while using the app|deny|cancel)\")"))
                        .click();
            } catch (WebDriverException ignored) {
                driver.navigate().back();
            }
            sleep(600);
            if (!appPackage.equals(safeCurrentPackage())) {
                // Still in a system app — keep backing out.
                try { driver.navigate().back(); } catch (WebDriverException ignored) {}
            }
        }
    }

    // =====================================================================
    // Web-gap helpers (2026-11-23). Ids/strings are from the app source:
    // fragment_home.xml, emoji_keyboard_layout.xml, chat_text_layout.xml,
    // CommonKeyboardFragment.kt, Home.kt.
    // =====================================================================

    /**
     * A 7-letter, digit-free stamp for message markers.
     *
     * <p>Why no digits: {@code tvMessage} is {@code autoLink="all"} (chat_text_layout.xml:113), so a
     * run of 7+ digits ("…-1453025") is linkified as a PHONE NUMBER, and the linkified row no longer
     * matches a {@code textContains} lookup of the marker — measured on the Pixel 2026-11-23: the
     * message was on screen and every lookup reported "not in the list at all".
     */
    public static String letterStamp() {
        long v = System.currentTimeMillis() % 8_031_810_176L; // 26^7
        char[] c = new char[7];
        for (int i = 6; i >= 0; i--) { c[i] = (char) ('a' + (v % 26)); v /= 26; }
        return new String(c);
    }

    /** The composer's hint ("Type a message..."); UiAutomator2 reports it as the text of an empty field. */
    public static final String COMPOSER_HINT = "Type a message...";

    /** The composer's current text, "" when it only shows its hint. */
    public String composerText() {
        try {
            WebElement f = driver.findElement(byId("etMessage"));
            String t = f.getText();
            if (t == null) return "";
            String hint = null;
            try { hint = f.getAttribute("hint"); } catch (WebDriverException ignored) { }
            if (t.equals(COMPOSER_HINT) || (hint != null && !hint.isEmpty() && t.equals(hint))) return "";
            return t;
        } catch (WebDriverException e) {
            return "";
        }
    }

    /** Types into the composer WITHOUT clearing first (appends at the cursor). */
    public void appendToComposer(String text) {
        WebElement field = waitVisible("etMessage");
        field.click();
        field.sendKeys(text);
    }

    /** True when the reply bar is up — a short wait, for "must be gone" checks. */
    public boolean replyBarShown(Duration timeout) {
        return isVisible("llReply", timeout);
    }

    /** The sender name shown in the reply bar (llReply → tvUserName), "" when absent. */
    public String replyBarUserName() {
        try {
            return driver.findElement(byId("llReply")).findElement(byId("tvUserName")).getText();
        } catch (WebDriverException e) {
            return "";
        }
    }

    /** The quoted text shown in the reply bar (llReply → tvMessage), "" when absent. */
    public String replyBarQuote() {
        try {
            return driver.findElement(byId("llReply")).findElement(byId("tvMessage")).getText();
        } catch (WebDriverException e) {
            return "";
        }
    }

    /** The unit chip that is currently selected, or "" (menuText carries selected=true). */
    public String selectedUnitName() {
        for (WebElement u : unitChipElements()) {
            try {
                if ("true".equals(u.getAttribute("selected"))) return u.getText();
            } catch (WebDriverException ignored) { }
        }
        return "";
    }

    /** The raw chip labels, blanks included (visibleUnitNames() drops blanks). */
    public List<String> rawUnitLabels() {
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        for (WebElement u : unitChipElements()) {
            try { out.add(u.getText() == null ? "" : u.getText()); } catch (WebDriverException ignored) { }
        }
        return out;
    }

    /** The first unit chip whose label contains the needle, or null. */
    public String unitNameContaining(String needle) {
        for (String n : visibleUnitNames()) {
            if (n.toLowerCase().contains(needle.toLowerCase())) return n;
        }
        return null;
    }

    /**
     * The first chat unit that is neither Calendar nor Call Sheet — the "Bulletin" role.
     * Returns its name, or null.
     */
    public String bulletinLikeUnit() {
        String b = unitNameContaining("bulletin");
        if (b != null) return b;
        for (String n : visibleUnitNames()) {
            String l = n.toLowerCase();
            if (!l.contains("calendar") && !l.contains("call")) return n;
        }
        return null;
    }

    /** Text of the badge on a unit chip (custom_tabbar_item.xml → badge), "" when hidden. */
    public String unitBadge(String unitName) {
        String pkg = appPackage();
        String xp = "//*[@resource-id='" + pkg + ":id/recTab']/*"
                + "[.//*[@resource-id='" + pkg + ":id/menuText' and @text=" + xpathLiteral(unitName) + "]]"
                + "//*[@resource-id='" + pkg + ":id/badge']";
        try {
            for (WebElement b : driver.findElements(AppiumBy.xpath(xp))) {
                if (b.isDisplayed()) return b.getText() == null ? "" : b.getText();
            }
        } catch (WebDriverException ignored) { }
        return "";
    }

    /** Number of rendered views whose text contains the marker (rows, not the composer). */
    public int rowsContaining(String marker) {
        String pkg = appPackage();
        try {
            return driver.findElements(AppiumBy.xpath("//*[@resource-id='" + pkg + ":id/rvChat']"
                    + "//*[contains(@text," + xpathLiteral(marker) + ")]")).size();
        } catch (WebDriverException e) {
            return 0;
        }
    }

    /**
     * Text of the view {@code id} inside the message card whose text CONTAINS {@code marker}
     * (top-level row only — not the nested comment cards). "" when not found.
     */
    public String rowText(String marker, String id) {
        String pkg = appPackage();
        String xp = "//*[@resource-id='" + pkg + ":id/materialCardView']"
                + "[./*[@resource-id='" + pkg + ":id/mainLi']/*[contains(@text," + xpathLiteral(marker) + ")]]"
                + "//*[@resource-id='" + pkg + ":id/" + id + "']";
        try {
            List<WebElement> els = driver.findElements(AppiumBy.xpath(xp));
            if (!els.isEmpty()) return els.get(0).getText() == null ? "" : els.get(0).getText();
        } catch (WebDriverException ignored) { }
        return "";
    }

    /** The timestamp line of the row (tvMsgTime): "Edited …" once edited. */
    public String rowTime(String marker) {
        return rowText(marker, "tvMsgTime");
    }

    /** The full tvMessage text of the row carrying the marker (may end in " Read more"). */
    public String rowMessage(String marker) {
        String pkg = appPackage();
        try {
            List<WebElement> els = driver.findElements(AppiumBy.xpath("//*[@resource-id='" + pkg
                    + ":id/rvChat']//*[@resource-id='" + pkg
                    + ":id/tvMessage' and contains(@text," + xpathLiteral(marker) + ")]"));
            for (WebElement e : els) {
                // skip the reply-bar quote in the composer
                if (e.getText() != null) return e.getText();
            }
        } catch (WebDriverException ignored) { }
        return "";
    }

    /** True when a reply (comment) with this text sits inside a parent's rvMessageReply. */
    public boolean replyNestedUnderParent(String reply) {
        String pkg = appPackage();
        try {
            return !driver.findElements(AppiumBy.xpath("//*[@resource-id='" + pkg + ":id/rvMessageReply']"
                    + "//*[contains(@text," + xpathLiteral(reply) + ")]")).isEmpty();
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** True when the parent row (marker) shows the reply text inside its own rvMessageReply. */
    public boolean replyUnderRow(String parentMarker, String reply) {
        String pkg = appPackage();
        String xp = "//*[@resource-id='" + pkg + ":id/materialCardView']"
                + "[./*[@resource-id='" + pkg + ":id/mainLi']/*[contains(@text," + xpathLiteral(parentMarker) + ")]]"
                + "//*[@resource-id='" + pkg + ":id/rvMessageReply']//*[contains(@text," + xpathLiteral(reply) + ")]";
        try {
            return !driver.findElements(AppiumBy.xpath(xp)).isEmpty();
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Taps the comment's menu arrow (deleteIv) on the reply carrying this text. */
    public boolean openCommentMenu(String reply) {
        String pkg = appPackage();
        String xp = "//*[@resource-id='" + pkg + ":id/rvMessageReply']/*"
                + "[.//*[contains(@text," + xpathLiteral(reply) + ")]]//*[@resource-id='" + pkg + ":id/deleteIv']";
        for (int i = 0; i < 3; i++) {
            try {
                List<WebElement> els = driver.findElements(AppiumBy.xpath(xp));
                if (!els.isEmpty()) {
                    els.get(0).click();
                    if (isMenuOpen(Duration.ofSeconds(3))) return true;
                }
            } catch (WebDriverException ignored) { }
            nudgeChatUp();
        }
        return false;
    }

    /** Tap (not long-press) the row carrying the text — toggles it in selection mode. */
    public boolean tapRow(String text) {
        try {
            WebElement el = driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().textContains(\"" + escape(text) + "\")"));
            org.openqa.selenium.Rectangle r = el.getRect();
            tapAt(r.getX() + r.getWidth() / 2, r.getY() + r.getHeight() / 2);
            sleepQuiet(600);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    // ---------- selection mode (Delete / Share) ----------

    /** The selection action bar is up (layAction with btnBack / tvAction). */
    public boolean inSelectionMode(Duration timeout) {
        return isVisible("layAction", timeout);
    }

    /** The selection counter (tvCount), "" when hidden. */
    public String selectionCount() {
        try {
            WebElement c = driver.findElement(byId("tvCount"));
            return c.isDisplayed() ? c.getText() : "";
        } catch (WebDriverException e) {
            return "";
        }
    }

    /** Leaves selection / search mode via its back arrow. */
    public void exitSelectionMode() {
        for (String id : new String[]{"btnBack", "btnBack2"}) {
            try {
                WebElement b = driver.findElement(byId(id));
                if (b.isDisplayed()) { b.click(); sleepQuiet(600); return; }
            } catch (WebDriverException ignored) { }
        }
    }

    /**
     * Deletes one of OUR OWN messages: long-press → Delete → action-bar Delete → Yes.
     * Best-effort cleanup; returns true once the text is gone.
     */
    public boolean deleteOwnMessage(String text) {
        try {
            revealNewestRow();
            if (!messageShown(text, Duration.ofSeconds(3)) && !messageShownScrolled(text, 2)) return true;
            // A local-only row left by an offline case shows Retry: send it first, then delete it,
            // so nothing stays behind in this device's local chat.
            if (messageFailed(text) && tapRetry(text)) waitMessageSent(text, Duration.ofSeconds(30));
            if (!longPressMessage(text) && !longPressMessageLeft(text)) return false;
            if (!tapMenuOption("Delete")) { closeMenu(); return false; }
            tapDeleteActionBar(Duration.ofSeconds(5));
            confirmPopup().accept(Duration.ofSeconds(6));
            sleepQuiet(1500);
            return !messageShown(text, Duration.ofSeconds(3));
        } catch (WebDriverException e) {
            return false;
        } finally {
            if (inSelectionMode(Duration.ofMillis(500))) exitSelectionMode();
        }
    }

    /**
     * Deletes up to {@code max} of OUR OWN leftover rows whose text starts with the prefix
     * (an earlier run that died before its cleanup). Returns how many went.
     */
    public int sweepOwnRows(String prefix, int max) {
        int n = 0;
        revealNewestRow();
        java.util.Set<String> tried = new java.util.HashSet<>();
        for (int i = 0; i < max; i++) {
            String t = null;
            try {
                for (WebElement e : driver.findElements(AppiumBy.xpath("//*[@resource-id='" + appPackage()
                        + ":id/rvChat']//*[@resource-id='" + appPackage() + ":id/tvMessage' and starts-with(@text,"
                        + xpathLiteral(prefix) + ")]"))) {
                    String x = e.getText();
                    if (x != null && !tried.contains(x)) { t = x; break; }
                }
            } catch (WebDriverException ignored) { }
            if (t == null) break;
            tried.add(t);
            if (deleteOwnMessage(t.length() > 60 ? t.substring(0, 60) : t)) n++;
        }
        return n;
    }

    /**
     * The newest text plus the number of media bubbles on screen — for "nothing was posted"
     * checks. Not the timestamp: the newest row's tvMsgTime can sit under the keyboard and drop
     * out of the tree, which made an unchanged feed read as changed (HomeChatTest, 2026-11-24).
     */
    public String newestRowSignature() {
        revealNewestRow();
        String msg = "";
        int media = 0;
        try {
            List<WebElement> m = driver.findElement(byId("rvChat")).findElements(byId("tvMessage"));
            if (!m.isEmpty()) msg = m.get(m.size() - 1).getText();
            media = driver.findElement(byId("rvChat")).findElements(byId("imageParent")).size();
        } catch (WebDriverException ignored) { }
        return msg + " | media=" + media;
    }

    /** The current activity name (short), "" when unknown. */
    public String activityName() {
        return safeActivity();
    }

    // ---------- search (ivSearch → laySearch) ----------

    public boolean openSearch() {
        try {
            waitVisible("ivSearch").click();
            return isVisible("edittext_search", Duration.ofSeconds(5));
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Types the query; when {@code imeSearch} presses the IME Search action. */
    public void searchFor(String query, boolean imeSearch) {
        WebElement f = waitVisible("edittext_search");
        f.click();
        f.clear();
        f.sendKeys(query);
        if (imeSearch) {
            try {
                driver.executeScript("mobile: performEditorAction", java.util.Map.of("action", "search"));
            } catch (WebDriverException ignored) { }
            sleepQuiet(1200);
            // On the emulator the editor action did not always reach the field (count stayed
            // blank). The up arrow runs the same search when none is active (Home.kt topButton:
            // !isEnableSearch → handleSearch), so use it as the fallback for a non-blank query.
            if (!query.trim().isEmpty() && searchCount().isBlank()) {
                tapSearchArrow(true);
            }
        }
    }

    /** Search result count (tvCount) — "" while nothing has been searched. */
    public String searchCount() {
        return selectionCount();
    }

    public void tapSearchArrow(boolean up) {
        try {
            driver.findElement(byId(up ? "top_button" : "bottom_button")).click();
            sleepQuiet(1000);
        } catch (WebDriverException ignored) { }
    }

    public boolean isSearchOpen(Duration timeout) {
        return isVisible("laySearch", timeout);
    }

    public void closeSearch() {
        try {
            driver.findElement(byId("btnBack2")).click();
            sleepQuiet(700);
        } catch (WebDriverException ignored) { }
    }

    // ---------- scroll-to-bottom arrow ----------

    public boolean arrowDownShown(Duration timeout) {
        return isVisible("arrowDownCard", timeout);
    }

    public void tapArrowDown() {
        try { driver.findElement(byId("arrowDownCard")).click(); sleepQuiet(1200); }
        catch (WebDriverException ignored) { }
    }

    /** Swipes the chat to reveal OLDER rows n times. */
    public void scrollToOlder(int n) {
        for (int i = 0; i < n; i++) swipeChat(true);
    }

    /** True when the row carrying the text is fully inside the visible chat list. */
    public boolean rowFullyVisible(String text) {
        try {
            WebElement rv = driver.findElement(byId("rvChat"));
            org.openqa.selenium.Rectangle list = rv.getRect();
            for (WebElement e : driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().textContains(\"" + escape(text) + "\")"))) {
                org.openqa.selenium.Rectangle r = e.getRect();
                if (r.getHeight() > 0 && r.getY() >= list.getY()
                        && r.getY() + r.getHeight() <= list.getY() + list.getHeight()) return true;
            }
        } catch (WebDriverException ignored) { }
        return false;
    }

    // ---------- double tap ----------

    /** Two taps on Send within ~100 ms (one W3C sequence). */
    public void doubleTapSend() {
        WebElement s = waitVisible("ivSendMessage");
        org.openqa.selenium.Rectangle r = s.getRect();
        int x = r.getX() + r.getWidth() / 2, y = r.getY() + r.getHeight() / 2;
        var finger = new org.openqa.selenium.interactions.PointerInput(
                org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        var seq = new org.openqa.selenium.interactions.Sequence(finger, 1);
        var origin = org.openqa.selenium.interactions.PointerInput.Origin.viewport();
        seq.addAction(finger.createPointerMove(Duration.ZERO, origin, x, y));
        seq.addAction(finger.createPointerDown(0));
        seq.addAction(finger.createPointerMove(Duration.ofMillis(30), origin, x, y));
        seq.addAction(finger.createPointerUp(0));
        seq.addAction(new org.openqa.selenium.interactions.Pause(finger, Duration.ofMillis(40)));
        seq.addAction(finger.createPointerDown(0));
        seq.addAction(finger.createPointerMove(Duration.ofMillis(30), origin, x, y));
        seq.addAction(finger.createPointerUp(0));
        driver.perform(java.util.Collections.singletonList(seq));
    }

    /** The voice recording bar (audioVw) is up. */
    public boolean recordingBarShown(Duration timeout) {
        return isVisible("audioVw", timeout);
    }

    // ---------- emoji picker ----------

    public boolean openEmojiPicker() {
        try {
            waitVisible("ivEmoji").click();
            return isVisible("emojiPicker", Duration.ofSeconds(5));
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean emojiPickerShown(Duration timeout) {
        return isVisible("emojiPicker", timeout);
    }

    /**
     * Taps the first emoji cell in the picker and returns its text (content-desc
     * fallback), or null when no cell was found.
     */
    public String pickFirstEmoji() {
        try {
            WebElement picker = driver.findElement(byId("emojiPicker"));
            for (WebElement c : picker.findElements(By.xpath(".//*[@clickable='true']"))) {
                String t = c.getText();
                if (t == null || t.isBlank()) t = c.getAttribute("content-desc");
                if (t == null || t.isBlank()) continue;
                // the category tabs are clickable ImageViews without text; emojis carry text
                if (t.codePoints().anyMatch(cp -> cp > 0x2000)) {
                    c.click();
                    sleepQuiet(500);
                    return t;
                }
            }
        } catch (WebDriverException ignored) { }
        return null;
    }

    /** Taps the composer (closes the emoji picker per CommonKeyboardFragment). */
    public void tapComposer() {
        try { waitVisible("etMessage").click(); sleepQuiet(500); } catch (WebDriverException ignored) { }
    }

    // ---------- edit dialog ----------

    /** Current text in the open edit dialog, "" when it cannot be read. */
    public String editDialogText() {
        try {
            return driver.findElement(byId("editTextInput")).getText();
        } catch (WebDriverException e) {
            try {
                return driver.findElement(byId("message")).findElement(By.className("android.widget.EditText")).getText();
            } catch (WebDriverException e2) {
                return "";
            }
        }
    }

    /** Sets the edit dialog text WITHOUT hiding the keyboard (no stray BACK in a dialog). */
    public void setEditDialogTextNoBack(String newText) {
        WebElement field;
        try {
            field = driver.findElement(byId("editTextInput"));
        } catch (WebDriverException notInner) {
            field = driver.findElement(byId("message")).findElement(By.className("android.widget.EditText"));
        }
        field.clear();
        if (!newText.isEmpty()) field.sendKeys(newText);
    }

    // ---------- helpers ----------

    private String escape(String s) {
        return s.replace("\"", "\\\"");
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
