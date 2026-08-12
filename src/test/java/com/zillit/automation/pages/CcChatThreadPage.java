package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;

/**
 * C&C chat thread (ChatAndGroupPage, activity_chat_and_group_page.xml) — 1-1 or
 * group. Verified live: rvVw (messages), etMessage, mainSendVw (→ ivRecord /
 * ivSend / editMessageTick), attachment, emoji, search, replyView/replyClose;
 * toolbar profileImage + userName (→ ProfileShowingActivity group info),
 * audioCall + videoCall (call buttons), mediaDocsLinksBtn. NOTE: different IDs
 * from the Home-units chat, so this is a dedicated page object.
 */
public class CcChatThreadPage extends BasePage {

    public CcChatThreadPage(AndroidDriver driver) {
        super(driver);
    }

    public boolean isLoaded(Duration timeout) {
        return isVisible("etMessage", timeout) && isVisible("rvVw", Duration.ofSeconds(3));
    }

    public String title() {
        try {
            return driver.findElement(byId("userName")).getText();
        } catch (WebDriverException e) {
            return "";
        }
    }

    // ---------------- send ----------------

    public void typeMessage(String text) {
        WebElement f = waitVisible("etMessage");
        f.click();
        f.sendKeys(text);
    }

    public void clearMessage() {
        try { driver.findElement(byId("etMessage")).clear(); } catch (WebDriverException ignored) {}
    }

    /** Taps the send button (mainSendVw shows ivSend once text is typed). */
    public void tapSend() {
        try {
            driver.findElement(byId("ivSend")).click();
        } catch (WebDriverException e) {
            try { driver.findElement(byId("mainSendVw")).click(); } catch (WebDriverException ignored) {}
        }
    }

    public void sendMessage(String text) {
        typeMessage(text);
        sleep(400);
        tapSend();
        sleep(900);
    }

    public boolean messageShown(String text, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        By loc = AppiumBy.androidUIAutomator("new UiSelector().textContains(\"" + escape(text) + "\")");
        while (System.currentTimeMillis() < deadline) {
            try { if (!driver.findElements(loc).isEmpty()) return true; } catch (WebDriverException ignored) {}
            sleep(400);
        }
        return false;
    }

    // ---------------- DID IT ACTUALLY SEND? ----------------
    //
    // C&C rows expose LESS than the Bulletin-style chats, and the difference decides what
    // can honestly be asserted here:
    //
    //   MEDIA rows (rv_item_image_video_*_sender, rv_item_audio_sender) carry
    //     `uploadingProgressStatus` + `retryVw` — a real, id-addressable failure state.
    //   TEXT rows (rv_item_message_sender) carry only `messageStatus`, an ImageView whose
    //     DRAWABLE is swapped (ic_clock → single_tick → double_tick). No id per state, no
    //     contentDescription, so the accessibility tree cannot tell pending from sent.
    //
    // Consequence: a C&C TEXT send cannot be verified on the sender's device. The proof
    // that works is the RECEIVER (see TwoUserChatTest) — which is why the two-device
    // suite, not this page, is what certifies C&C delivery.
    //
    // One line in the app would remove the whole problem: a contentDescription on
    // `messageStatus` ("pending" / "sent" / "delivered" / "read"). Worth asking for — it
    // is the same ask as the three unlabelled Switches on the Drive share screen.

    /** A media row is still uploading or has failed — the Retry affordance is showing. */
    public boolean mediaSendUnfinished() {
        return isVisible("retryVw", Duration.ofSeconds(2))
                || isVisible("uploadingProgressStatus", Duration.ofSeconds(1));
    }

    /**
     * Waits for a media send to finish — no Retry, no upload progress left on screen.
     *
     * <p>Unlike the text case above this IS verifiable, because the failure state has an
     * id. Use it after sending any attachment in C&C.
     */
    public boolean waitMediaSent(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (!mediaSendUnfinished()) return true;
            sleep(1000);
        }
        return false;
    }

    public boolean isInputBarVisible() {
        return isVisible("etMessage", Duration.ofSeconds(3));
    }

    public boolean isRecordButtonVisible() {
        return isVisible("ivRecord", Duration.ofSeconds(2));
    }

    // ---------------- long-press options ----------------

    /** Long-presses the newest message bubble containing the text. */
    public boolean longPressMessage(String text) {
        // A raised keyboard pushes the newest bubble under the IME and throws off
        // the press point — hide it first, then retry once if the menu doesn't open.
        hideKeyboard();
        sleep(400);
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                WebElement bubble = driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiSelector().textContains(\"" + escape(text) + "\")"));
                var r = bubble.getRect();
                w3cLongPress(r.getX() + r.getWidth() / 2, r.getY() + r.getHeight() / 2, 1000);
                if (isMenuOpen(Duration.ofSeconds(3))) return true;
            } catch (WebDriverException e) {
                sleep(500);
            }
        }
        return false;
    }

    public boolean isMenuOpen(Duration timeout) {
        return isVisible("mainTitle", timeout);
    }

    // ---------------- image bubbles (chat_attachment_view: imageParent) ----------------

    public int imageBubbleCount() {
        // C&C sent-image bubble is `srcImage` (ImageView inside mainCardVw).
        try { return driver.findElements(byId("srcImage")).size(); }
        catch (WebDriverException e) { return 0; }
    }

    /** Wait until at least one image/attachment bubble is present after a send. */
    public boolean waitImageBubble(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (imageBubbleCount() > 0) return true;
            sleep(800);
        }
        return false;
    }

    /** Long-press the newest image bubble (opens the option menu with Image Reply). */
    public boolean longPressNewestImage() {
        hideKeyboard();
        sleep(400);
        try {
            var imgs = driver.findElements(byId("srcImage"));
            if (imgs.isEmpty()) return false;
            WebElement img = imgs.get(imgs.size() - 1);
            var r = img.getRect();
            w3cLongPress(r.getX() + r.getWidth() / 2, r.getY() + r.getHeight() / 2, 1000);
            return isMenuOpen(Duration.ofSeconds(3));
        } catch (WebDriverException e) {
            return false;
        }
    }

    public List<String> menuOptions() {
        try {
            return driver.findElements(byId("mainTitle")).stream()
                    .map(WebElement::getText).filter(t -> t != null && !t.isBlank()).toList();
        } catch (WebDriverException e) {
            return List.of();
        }
    }

    public boolean tapMenuOption(String label) {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceIdMatches(\".*mainTitle\").textMatches(\"(?i)"
                            + label + "\")")).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public void closeMenu() {
        try { driver.navigate().back(); } catch (WebDriverException ignored) {}
    }

    // ---------------- reply ----------------

    public boolean isReplyPreviewVisible() {
        return isVisible("replyView", Duration.ofSeconds(8))
                || isVisible("replyMessage", Duration.ofSeconds(2))
                || isVisible("replyUserName", Duration.ofSeconds(2));
    }

    public void cancelReply() {
        try { driver.findElement(byId("replyClose")).click(); } catch (WebDriverException ignored) {}
    }

    /** Long-press a message → Reply → type + send the reply. Returns true if sent. */
    public boolean replyTo(String targetText, String replyText) {
        if (!longPressMessage(targetText)) { System.out.println(">>> replyTo: longPress failed"); return false; }
        if (!tapMenuOption("Reply")) { System.out.println(">>> replyTo: tap Reply failed"); return false; }
        if (!isReplyPreviewVisible()) { System.out.println(">>> replyTo: no reply preview (replyView)"); return false; }
        sendMessage(replyText);
        return true;
    }

    // ---------------- edit own message ----------------

    /**
     * Edit an own text message: long-press → Edit → replace text → save. C&C edits
     * INLINE (the old text loads into etMessage and mainSendVw shows editMessageTick);
     * falls back to the dialog form (message + rightButton) if that's what appears.
     */
    public boolean editMessage(String oldText, String newText) {
        if (!longPressMessage(oldText)) return false;
        if (!tapMenuOption("Edit")) return false;
        sleep(600);
        // Inline path: etMessage now holds the old text.
        try {
            WebElement f = driver.findElement(byId("etMessage"));
            f.clear();
            f.sendKeys(newText);
            sleep(400);
            try { driver.findElement(byId("editMessageTick")).click(); return true; }
            catch (WebDriverException noTick) {
                try { driver.findElement(byId("ivSend")).click(); return true; }
                catch (WebDriverException noSend) { /* fall through to dialog */ }
            }
        } catch (WebDriverException notInline) { /* fall through to dialog */ }
        // Dialog path (like Home chat): message → inner EditText, rightButton to save.
        try {
            WebElement field = driver.findElement(byId("message"))
                    .findElement(By.className("android.widget.EditText"));
            field.clear();
            field.sendKeys(newText);
            driver.findElement(byId("rightButton")).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    // ---------------- delete message (3-step) ----------------

    /**
     * Delete a message the 3-step way (menu Delete → action-bar Delete `tvAction` →
     * Yes), same flow as Home chat / call-sheet. Returns true if the confirm was tapped.
     */
    public boolean deleteMessage(String text) {
        if (!longPressMessage(text)) return false;
        if (!tapMenuOption("Delete")) return false;
        sleep(600);
        // C&C message-delete raises a custom dialog with `cancel` + `actionButton`
        // (the confirm). Click actionButton; fall back to a tvAction action bar or a
        // Yes/OK dialog for the shared code paths that use those instead.
        long deadline = System.currentTimeMillis() + 6000;
        while (System.currentTimeMillis() < deadline) {
            try { driver.findElement(byId("actionButton")).click(); return true; }
            catch (WebDriverException noBtn) { /* try other shapes */ }
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiSelector().resourceIdMatches(\".*tvAction\").textMatches(\"(?i)delete\")")).click();
                return confirmYes(Duration.ofSeconds(5));
            } catch (WebDriverException noAction) { /* try Yes/OK */ }
            if (confirmYes(Duration.ofSeconds(1))) return true;
            sleep(300);
        }
        return false;
    }

    /** Taps a Yes/OK/Confirm/Delete button in a plain confirmation dialog. */
    public boolean confirmYes(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        By yes = AppiumBy.androidUIAutomator(
                "new UiSelector().clickable(true).textMatches(\"(?i)(yes|ok|confirm|delete)\")");
        while (System.currentTimeMillis() < deadline) {
            try { driver.findElement(yes).click(); return true; }
            catch (WebDriverException notYet) { sleep(300); }
        }
        return false;
    }

    // ---------------- attachment sheet (PickerDialog) ----------------

    public boolean tapAttachment() {
        try { waitVisible("attachment").click(); return true; }
        catch (WebDriverException e) { return false; }
    }

    /** Open the attachment "Choose below" sheet. */
    public boolean openAttachmentSheet() {
        if (!tapAttachment()) return false;
        return isAttachmentSheetOpen(Duration.ofSeconds(6));
    }

    public boolean isAttachmentSheetOpen(Duration timeout) {
        return textShown("Choose below", timeout)
                || isVisible("rvItems", Duration.ofSeconds(2))
                || textShown("Gallery", Duration.ofSeconds(2));
    }

    /** True if an attachment option label is present (Photo/Gallery/Video/Video
     *  Gallery/Document/Location/Contact/Audio). */
    public boolean attachmentOptionShown(String label) {
        return textShown(label, Duration.ofSeconds(3));
    }

    public boolean tapAttachmentOption(String label) {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().textMatches(\"(?i)" + label + "\")")).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public void dismissAttachmentSheet() {
        try { driver.navigate().back(); } catch (WebDriverException ignored) {}
    }

    /**
     * Best-effort: send an image through the system Gallery picker. Returns true if
     * an image bubble count increased. System-picker UI is device-specific, so this
     * returns false (caller should skip) when the picker can't be navigated.
     */
    public boolean sendImageViaGallery() {
        int before = imageBubbleCount();
        if (!openAttachmentSheet()) return false;
        if (!tapAttachmentOption("Gallery")) return false;
        grantPermissionIfPresent();
        // The Gallery uses the app's own "All Media" grid + preview (same as Home
        // chat), so reuse MediaPreviewPage: open All Media → select 1 → send FAB →
        // preview → send.
        MediaPreviewPage media = new MediaPreviewPage(driver);
        if (!media.isGalleryOpen()) return false;
        media.openAllMediaAlbumIfListed();
        // The Fallery gallery opens on the album/bucket list — if no photo grid yet,
        // tap the first bucket (imageViewBucketImage) to enter an album.
        if (media.galleryThumbCount() == 0) {
            try {
                var buckets = driver.findElements(byId("imageViewBucketImage"));
                if (!buckets.isEmpty()) { buckets.get(0).click(); sleep(1500); }
            } catch (WebDriverException ignored) {}
        }
        if (media.galleryThumbCount() == 0) { try { driver.navigate().back(); } catch (WebDriverException ignored) {} return false; }
        media.selectPhotos(1);
        if (!media.isSendFabVisible()) { try { driver.navigate().back(); } catch (WebDriverException ignored) {} return false; }
        media.tapGallerySendFab();
        if (!media.isPreviewOpen()) return false;
        media.setCaption("");
        media.tapPreviewSend();
        media.waitPreviewClosed(Duration.ofSeconds(60));
        // The image bubble (srcImage) appears asynchronously as the upload completes.
        return waitImageBubble(Duration.ofSeconds(25));
    }

    /** Count of NON-image media bubbles (video/doc/audio/location/contact cards). */
    public int cardBubbleCount() {
        try { return driver.findElements(byId("mainCardVw")).size(); }
        catch (WebDriverException e) { return 0; }
    }

    /** Best-effort: send a VIDEO via the Video Gallery (same Fallery pipeline). */
    public boolean sendVideoViaGallery() {
        int before = cardBubbleCount();
        if (!openAttachmentSheet()) return false;
        if (!tapAttachmentOption("Video Gallery")) return false;
        grantPermissionIfPresent();
        MediaPreviewPage media = new MediaPreviewPage(driver);
        if (!media.isGalleryOpen()) return false;
        // Enter a bucket that contains the video ("All Media" has everything; else
        // "Movies"), then look for a video thumbnail.
        enterGalleryBucket("All Media", "Movies", "DCIM");
        if (media.galleryVideoCount() == 0) { try { driver.navigate().back(); } catch (WebDriverException ignored) {} return false; }
        media.selectVideos(1);
        if (!media.isSendFabVisible()) { try { driver.navigate().back(); } catch (WebDriverException ignored) {} return false; }
        media.tapGallerySendFab();
        if (!media.isPreviewOpen()) return false;
        media.setCaption("");
        media.tapPreviewSend();
        media.waitPreviewClosed(Duration.ofSeconds(60));
        return waitCardIncrease(before, Duration.ofSeconds(25));
    }

    /** Best-effort: send a DOCUMENT via the system SAF picker (namePart = fixture). */
    public boolean sendDocument(String namePart) {
        int before = cardBubbleCount();
        if (!openAttachmentSheet()) return false;
        if (!tapAttachmentOption("Document")) return false;
        grantPermissionIfPresent();
        sleep(2000);
        // System documents UI — pick the fixture by name.
        boolean picked = pickSystemFileByName(namePart, Duration.ofSeconds(10));
        if (!picked) { recoverBack(); return false; }
        // a preview/send confirm may follow
        MediaPreviewPage media = new MediaPreviewPage(driver);
        if (media.isPreviewOpen()) { media.setCaption(""); media.tapPreviewSend(); media.waitPreviewClosed(Duration.ofSeconds(40)); }
        return waitCardIncrease(before, Duration.ofSeconds(25));
    }

    /** Best-effort: share the current LOCATION (map → send-location button). */
    public boolean sendLocation() {
        int before = cardBubbleCount();
        if (!openAttachmentSheet()) return false;
        if (!tapAttachmentOption("Location")) return false;
        grantPermissionIfPresent();
        sleep(2500);
        boolean sendUp = isVisible("sendLocation", Duration.ofSeconds(10));
        if (!sendUp) { recoverBack(); return false; }
        try { driver.findElement(byId("sendLocation")).click(); } catch (WebDriverException e) { return false; }
        return waitCardIncrease(before, Duration.ofSeconds(25));
    }

    /** Best-effort: share a CONTACT (system contact picker → confirm/send). */
    public boolean sendContact() {
        int before = cardBubbleCount();
        if (!openAttachmentSheet()) return false;
        if (!tapAttachmentOption("Contact")) return false;
        grantPermissionIfPresent();
        sleep(2500);
        // System contacts UI — tap the first contact row.
        try {
            var rows = driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceIdMatches(\".*(contact|title|name).*\").clickable(true)"));
            if (rows.isEmpty()) { recoverBack(); return false; }
            rows.get(0).click();
            sleep(1500);
        } catch (WebDriverException e) { recoverBack(); return false; }
        // confirm/send screen
        if (isVisible("sendBtn", Duration.ofSeconds(6))) {
            try { driver.findElement(byId("sendBtn")).click(); } catch (WebDriverException ignored) {}
        }
        return waitCardIncrease(before, Duration.ofSeconds(25));
    }

    /** Enter a Fallery gallery album by trying label names, else the first bucket. */
    private void enterGalleryBucket(String... labels) {
        for (String label : labels) {
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiSelector().textContains(\"" + label + "\")")).click();
                sleep(1500);
                return;
            } catch (WebDriverException notThis) { /* try next */ }
        }
        try {
            var buckets = driver.findElements(byId("imageViewBucketImage"));
            if (!buckets.isEmpty()) { buckets.get(0).click(); sleep(1500); }
        } catch (WebDriverException ignored) {}
    }

    private boolean waitCardIncrease(int before, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (cardBubbleCount() > before) return true;
            sleep(800);
        }
        return false;
    }

    /** In an open Android SYSTEM picker (Documents), tap the first item matching name. */
    private boolean pickSystemFileByName(String namePart, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            try {
                WebElement e = driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiSelector().textContains(\"" + namePart + "\")"));
                e.click();
                return true;
            } catch (WebDriverException notYet) {
                // Try a documents search box.
                try {
                    driver.findElement(AppiumBy.androidUIAutomator(
                            "new UiSelector().descriptionContains(\"Search\")")).click();
                    WebElement s = driver.findElement(By.className("android.widget.EditText"));
                    s.sendKeys(namePart);
                    sleep(1200);
                } catch (WebDriverException ignored) {}
            }
            sleep(600);
        }
        return false;
    }

    private void recoverBack() {
        // Media flows can strand us on a system picker / gallery preview — back out,
        // re-activate the app if we left it, until the chat input bar is back.
        for (int i = 0; i < 6; i++) {
            try {
                String pkg = driver.getCurrentPackage();
                if (pkg == null || !pkg.contains("zillit")) {
                    driver.executeScript("mobile: activateApp",
                            java.util.Map.of("appId", appPackage()));
                    sleep(1000);
                    continue;
                }
            } catch (WebDriverException ignored) {}
            try { if (isInputBarVisible()) return; } catch (WebDriverException ignored) {}
            try { driver.navigate().back(); } catch (WebDriverException ignored) {}
            sleep(700);
        }
    }

    // ---------------- in-thread search ----------------
    // Toolbar `search` opens searchVwMain (search_text_vw.xml): editSearch input,
    // cancel (close), arrowUpSearch / arrowDownSearch (prev/next match), searchCount.

    public boolean openChatSearch() {
        try { driver.findElement(byId("search")).click(); }
        catch (WebDriverException e) { return false; }
        return isVisible("editSearch", Duration.ofSeconds(6));
    }

    public boolean isChatSearchOpen(Duration timeout) {
        return isVisible("editSearch", timeout);
    }

    public void typeChatSearch(String text) {
        try {
            WebElement f = waitVisible("editSearch");
            f.click();
            f.clear();
            f.sendKeys(text);
            sleep(600);
            driver.pressKey(new io.appium.java_client.android.nativekey.KeyEvent(
                    io.appium.java_client.android.nativekey.AndroidKey.ENTER));
            sleep(1200);
        } catch (WebDriverException ignored) {
        }
    }

    public String chatSearchCount() {
        try { return driver.findElement(byId("searchCount")).getText(); }
        catch (WebDriverException e) { return ""; }
    }

    public boolean nextSearchResult() {
        try { driver.findElement(byId("arrowDownSearch")).click(); sleep(600); return true; }
        catch (WebDriverException e) { return false; }
    }

    public boolean prevSearchResult() {
        try { driver.findElement(byId("arrowUpSearch")).click(); sleep(600); return true; }
        catch (WebDriverException e) { return false; }
    }

    public void closeChatSearch() {
        try { driver.findElement(byId("cancel")).click(); } catch (WebDriverException ignored) {}
    }

    // ---------------- voice note (tap-to-record) ----------------
    // Empty input → ivRecord (mic) shows. Tap it → audioVw overlay (audioTimer +
    // audiMicVw + canelAudiRec) and ivSend appears. Tap ivSend → sends the .m4a.

    /** Record a short voice note and send it. Returns true if the record overlay
     *  showed and the send completed (overlay dismissed). */
    public boolean recordAndSendVoice(long recordMs) {
        clearMessage();
        sleep(400);
        try { driver.findElement(byId("ivRecord")).click(); }
        catch (WebDriverException e) { return false; }
        // grant the mic permission prompt if it appears
        grantPermissionIfPresent();
        boolean recording = isVisible("audioVw", Duration.ofSeconds(6))
                || isVisible("audioTimer", Duration.ofSeconds(2));
        if (!recording) return false;
        sleep(recordMs);
        try { driver.findElement(byId("ivSend")).click(); }
        catch (WebDriverException e) {
            try { driver.findElement(byId("mainSendVw")).click(); } catch (WebDriverException ignored) {}
        }
        sleep(1500);
        // sent → the recording overlay is gone
        return !isVisible("audioVw", Duration.ofSeconds(2));
    }

    public boolean cancelVoiceRecording() {
        try { driver.findElement(byId("canelAudiRec")).click(); return true; }
        catch (WebDriverException e) { return false; }
    }

    private void grantPermissionIfPresent() {
        for (int i = 0; i < 3; i++) {
            try {
                WebElement b = driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiSelector().textMatches(\"(?i)(allow|while using the app|allow all the time)\")"));
                b.click();
                sleep(500);
            } catch (WebDriverException none) { return; }
        }
    }

    // ---------------- call buttons ----------------

    public boolean isAudioCallVisible() {
        return isVisible("audioCall", Duration.ofSeconds(6));
    }

    public boolean isVideoCallVisible() {
        return isVisible("videoCall", Duration.ofSeconds(6));
    }

    public boolean tapAudioCall() {
        try { driver.findElement(byId("audioCall")).click(); return true; }
        catch (WebDriverException e) { return false; }
    }

    public boolean tapVideoCall() {
        try { driver.findElement(byId("videoCall")).click(); return true; }
        catch (WebDriverException e) { return false; }
    }

    // ---------------- Copy ----------------

    /** Long-press → Copy (puts the message text on the clipboard). */
    public boolean copyMessage(String text) {
        if (!longPressMessage(text)) return false;
        return tapMenuOption("Copy");
    }

    /** Reads the device clipboard (verifies Copy). Appium's clipboard API is the
     *  reliable path here — pasting into the input box returns only the hint. */
    public String clipboardText() {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                String v = ((io.appium.java_client.clipboard.HasClipboard) driver).getClipboardText();
                if (v != null && !v.isBlank()) return v;
            } catch (WebDriverException e) {
                // fall through, retry
            }
            sleep(500);
        }
        return "";
    }

    // ---------------- emoji reactions ----------------
    // The long-press popup (reaction bar + menu) is a PopupWindow that is NOT in the
    // accessibility tree, so the emoji row can only be driven by coordinates. We
    // anchor off the "Copy" menu row (which IS queryable): the reaction strip sits
    // ~90px above it, 👍 starts ~copy.x+43, and emojis repeat every ~101px.
    // Order: 👍(0) ❤️(1) 😂(2) 😮(3) 😢(4) 🙏(5) ➕(6=add-more).

    public boolean reactTo(String text, int emojiIndex) {
        if (!longPressMessage(text)) return false;
        try {
            // Anchor off the TOP-MOST menu row (1-1 = Copy, group = Edit) — the
            // reaction strip sits ~90px above the popup menu regardless of chat type.
            WebElement topRow = driver.findElements(byId("mainTitle")).stream()
                    .min(java.util.Comparator.comparingInt(e -> e.getRect().getY()))
                    .orElseThrow();
            var r = topRow.getRect();
            int x = r.getX() + 43 + emojiIndex * 101;
            int y = r.getY() - 90;
            tapXY(x, y);
            sleep(2000);
            return hasReaction(Duration.ofSeconds(6));
        } catch (Exception e) {
            closeMenu();
            return false;
        }
    }

    /** True once a reaction badge is attached to a bubble. */
    public boolean hasReaction(Duration timeout) {
        return isVisible("mainEmojiView", timeout)
                || isVisible("emojiView", Duration.ofSeconds(2))
                || isVisible("emojiTextView", Duration.ofSeconds(2));
    }

    // ---------------- Forward ----------------

    /**
     * Long-press → Forward → (select mode) → forward arrow → confirm "Forward",
     * which opens the ForwardPage recipient picker. Returns true if ForwardPage
     * opened. Leaves you ON ForwardPage — call leaveForwardPage() to back out
     * WITHOUT actually forwarding.
     */
    public boolean forwardMessage(String text) {
        if (!longPressMessage(text)) return false;
        if (!tapMenuOption("Forward")) return false;
        sleep(1500);
        var size = driver.manage().window().getSize();
        // Forward arrow is top-right of the select-mode bar.
        tapXY((int) (size.getWidth() * 0.93), (int) (size.getHeight() * 0.12));
        sleep(1500);
        // Confirm dialog "Are you sure you want to forward?" → Forward.
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().clickable(true).textMatches(\"(?i)forward\")")).click();
        } catch (WebDriverException noBtn) {
            tapXY((int) (size.getWidth() * 0.79), (int) (size.getHeight() * 0.555));
        }
        sleep(2500);
        return currentActivityContains("ForwardPage");
    }

    public boolean isForwardPageOpen() {
        return currentActivityContains("ForwardPage");
    }

    /** Cancel out of the ForwardPage picker (does NOT forward). */
    public void leaveForwardPage() {
        for (int i = 0; i < 3 && currentActivityContains("ForwardPage"); i++) {
            try { driver.navigate().back(); } catch (WebDriverException ignored) {}
            sleep(800);
        }
    }

    // ---------------- Share ----------------

    /**
     * Long-press → Share → (select mode with a share icon top-right) → tap the
     * share icon, which hands the text to the OS share sheet. Returns true if the
     * Share menu option + select-mode share icon were driven.
     */
    public boolean shareMessage(String text) {
        if (!longPressMessage(text)) return false;
        if (!tapMenuOption("Share")) return false;
        sleep(1500);
        // Share enters a message select-mode (input bar is replaced by a selection
        // top bar) — the input box disappearing is the reliable "Share invoked"
        // signal. Then the share icon (top-right) hands off to the OS share sheet.
        boolean selectMode = !isVisible("etMessage", Duration.ofSeconds(2));
        var size = driver.manage().window().getSize();
        tapXY((int) (size.getWidth() * 0.93), (int) (size.getHeight() * 0.12));
        sleep(1500);
        return selectMode;
    }

    /** True if a share target surfaced (package changed OR an Android chooser is up). */
    public boolean isShareSurfaceShown(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            try {
                String pkg = driver.getCurrentPackage();
                if (pkg != null && !pkg.contains("zillit")) return true;   // system chooser / target app
            } catch (WebDriverException ignored) {}
            try {
                String src = driver.getPageSource();
                if (src.contains("resolver") || src.contains("chooser")
                        || src.contains("android:id/resolver_list")) return true;
            } catch (WebDriverException ignored) {}
            sleep(400);
        }
        return false;
    }

    private boolean currentActivityContains(String needle) {
        try {
            String a = driver.currentActivity();
            return a != null && a.toLowerCase().contains(needle.toLowerCase());
        } catch (WebDriverException e) {
            return false;
        }
    }

    private void tapXY(int x, int y) {
        var finger = new org.openqa.selenium.interactions.PointerInput(
                org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        var seq = new org.openqa.selenium.interactions.Sequence(finger, 1);
        seq.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, y));
        seq.addAction(finger.createPointerDown(0));
        seq.addAction(finger.createPointerUp(0));
        driver.perform(java.util.Collections.singletonList(seq));
    }

    // ---------------- open group info / profile ----------------

    public CcGroupInfoPage openGroupInfo() {
        try {
            driver.findElement(byId("userName")).click();
        } catch (WebDriverException e) {
            try { driver.findElement(byId("profileImage")).click(); } catch (WebDriverException ignored) {}
        }
        return new CcGroupInfoPage(driver);
    }

    public void back() {
        try { driver.navigate().back(); } catch (WebDriverException ignored) {}
    }

    // ---------------- helpers ----------------

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

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
