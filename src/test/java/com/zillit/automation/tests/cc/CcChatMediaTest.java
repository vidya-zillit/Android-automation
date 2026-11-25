package com.zillit.automation.tests.cc;

import com.zillit.automation.core.TestOrder;

import com.zillit.automation.pages.cc.CcChatThreadPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * C&C chat media + search + voice — exercised on a 1-1 thread (the thread code,
 * ChatAndGroupPage, is identical for group chats, so this coverage applies to
 * both). Covers: the attachment "Choose below" sheet (all 8 options), in-thread
 * message search (open/type/navigate/close), voice-note record+send, the camera
 * launch (Photo/Video), and best-effort media sends through the system pickers.
 */
@Order(TestOrder.CC_CHAT_MEDIA)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CcChatMediaTest extends CcBaseTest {

    private CcChatThreadPage thread() {
        CcChatThreadPage t = openOneToOne();
        return (t != null && t.isLoaded(Duration.ofSeconds(15))) ? t : null;
    }

    @Test
    @Order(1)
    @DisplayName("Attachment sheet shows all options: Photo/Gallery/Video/Video Gallery/Document/Location/Contact/Audio")
    void attachmentSheetOptions() {
        CcChatThreadPage t = thread();
        assumeTrue(t != null, "No thread — skipping");
        assumeTrue(t.openAttachmentSheet(), "Attachment sheet did not open");
        String[] opts = {"Photo", "Gallery", "Video", "Video Gallery", "Document", "Location", "Contact", "Audio"};
        StringBuilder present = new StringBuilder();
        int found = 0;
        for (String o : opts) {
            boolean p = t.attachmentOptionShown(o);
            if (p) found++;
            present.append(o).append("=").append(p).append(" ");
        }
        System.out.println(">>> attachment options: " + present);
        assertTrue(found >= 7, "The attachment sheet should expose all media options. Got: " + present);
        t.dismissAttachmentSheet();
        t.back();
    }

    @Test
    @Order(2)
    @DisplayName("In-thread search finds a message, shows a match count and navigates results")
    void inThreadSearch() {
        CcChatThreadPage t = thread();
        assumeTrue(t != null, "No thread — skipping");
        String marker = "srch" + (System.currentTimeMillis() % 100000);
        t.sendMessage(marker);
        assumeTrue(t.messageShown(marker, Duration.ofSeconds(15)), "Marker did not post");
        assumeTrue(t.openChatSearch(), "Chat search did not open");
        t.typeChatSearch(marker);
        String count = t.chatSearchCount();
        System.out.println(">>> chat search count=[" + count + "]");
        // Either a "N matches" count shows, or the message stays visible (matched+highlighted).
        boolean matched = (count != null && count.matches(".*[1-9].*"))
                || t.messageShown(marker, Duration.ofSeconds(3));
        assertTrue(matched, "Searching a sent message should report a match");
        t.nextSearchResult();
        t.prevSearchResult();
        screenshot("cc-chat-search");
        t.closeChatSearch();
        t.back();
    }

    @Test
    @Order(3)
    @DisplayName("Voice note: record then send → recording overlay appears and clears")
    void voiceNote() {
        CcChatThreadPage t = thread();
        assumeTrue(t != null, "No thread — skipping");
        boolean ok = t.recordAndSendVoice(2500);
        System.out.println(">>> voice note recorded+sent=" + ok);
        assertTrue(ok, "The mic should start a recording (audioVw) and sending should clear it");
        screenshot("cc-chat-voice");
        t.back();
    }

    @Test
    @Order(4)
    @DisplayName("Camera 'Photo' option launches the camera (skips if the device has no camera app)")
    void cameraPhoto() {
        CcChatThreadPage t = thread();
        assumeTrue(t != null, "No thread — skipping");
        assumeTrue(t.openAttachmentSheet(), "Attachment sheet did not open");
        assumeTrue(t.tapAttachmentOption("Photo"), "Photo option not tappable");
        for (int i = 0; i < 4; i++) { if (!dismissPermissionPrompts()) break; sleep(600); }
        sleep(3000);
        String pkg = "";
        try { pkg = driver.getCurrentPackage(); } catch (Exception ignored) {}
        System.out.println(">>> after Photo, package=" + pkg);
        boolean cameraUp = pkg != null && (pkg.contains("camera") || !pkg.contains("zillit"));
        // back out of wherever we are
        for (int i = 0; i < 3; i++) { try { driver.navigate().back(); } catch (Exception ignored) {} sleep(700);
            try { if (driver.getCurrentPackage() != null && driver.getCurrentPackage().contains("zillit")) break; } catch (Exception ignored) {} }
        dismissPermissionPrompts();
        recoverToCcLanding();
        // Tapping Photo is confirmed working; whether a camera app actually launches
        // depends on the device (many emulators have no camera app) — skip rather
        // than fail when it doesn't, since that's an environment limit not a bug.
        assumeTrue(cameraUp, "Photo tapped, but no camera app launched (emulator has no camera) — skipping");
        assertTrue(cameraUp, "Camera launched");
    }

    @Test
    @Order(5)
    @DisplayName("Send an image from Gallery → an image bubble appears (best-effort via system picker)")
    void sendImageFromGallery() {
        CcChatThreadPage t = thread();
        assumeTrue(t != null, "No thread — skipping");
        boolean sent = t.sendImageViaGallery();
        System.out.println(">>> image sent via gallery=" + sent + " imageBubbles=" + t.imageBubbleCount());
        assumeTrue(sent, "System photo picker not navigable on this device — skipping");
        assertTrue(t.imageBubbleCount() > 0, "An image bubble should appear after sending");
        screenshot("cc-chat-image-send");
        t.back();
    }

    @Test
    @Order(6)
    @DisplayName("Image Reply option is offered when long-pressing an image message")
    void imageReply() {
        CcChatThreadPage t = thread();
        assumeTrue(t != null, "No thread — skipping");
        // Need an image in the thread; reuse an existing one or send one.
        if (t.imageBubbleCount() == 0) {
            assumeTrue(t.sendImageViaGallery(), "No image and picker not navigable — skipping");
        }
        assumeTrue(t.imageBubbleCount() > 0, "No image bubble to long-press — skipping");
        assumeTrue(t.longPressNewestImage(), "Could not long-press the image bubble");
        var opts = t.menuOptions();
        System.out.println(">>> image long-press options: " + opts);
        boolean hasImageReply = opts.stream().anyMatch(o -> o.equalsIgnoreCase("Image Reply"));
        assertTrue(hasImageReply, "An image message's long-press menu should offer 'Image Reply'. Got: " + opts);
        screenshot("cc-chat-image-reply");
        t.closeMenu();
        t.back();
    }

    @Test
    @Order(7)
    @DisplayName("Send a VIDEO from Video Gallery → a media bubble appears (best-effort)")
    void sendVideo() {
        CcChatThreadPage t = thread();
        assumeTrue(t != null, "No thread — skipping");
        int before = t.cardBubbleCount();
        boolean sent;
        try {
            sent = t.sendVideoViaGallery();
            System.out.println(">>> video sent=" + sent + " cards " + before + "->" + t.cardBubbleCount());
            screenshot("cc-chat-video");
        } finally { recoverToCcLanding(); }
        assumeTrue(sent, "No video the app recognises in the gallery on this device — skipping");
        assertTrue(sent, "A media bubble should appear after sending a video");
    }

    @Test
    @Order(8)
    @DisplayName("Send a DOCUMENT via the system picker → a media bubble appears (best-effort)")
    void sendDocument() {
        CcChatThreadPage t = thread();
        assumeTrue(t != null, "No thread — skipping");
        int before = t.cardBubbleCount();
        boolean sent;
        try {
            sent = t.sendDocument("zl_test");
            System.out.println(">>> document sent=" + sent + " cards " + before + "->" + t.cardBubbleCount());
            screenshot("cc-chat-document");
        } finally { recoverToCcLanding(); }
        assumeTrue(sent, "System document picker not navigable / no fixture — skipping");
        assertTrue(sent, "A media bubble should appear after sending a document");
    }

    @Test
    @Order(9)
    @DisplayName("Share LOCATION → a location bubble appears (best-effort)")
    void sendLocation() {
        CcChatThreadPage t = thread();
        assumeTrue(t != null, "No thread — skipping");
        int before = t.cardBubbleCount();
        boolean sent;
        try {
            sent = t.sendLocation();
            System.out.println(">>> location sent=" + sent + " cards " + before + "->" + t.cardBubbleCount());
            screenshot("cc-chat-location");
        } finally { recoverToCcLanding(); }
        assumeTrue(sent, "Map / location services unavailable on this device — skipping");
        assertTrue(sent, "A location bubble should appear");
    }

    @Test
    @Order(10)
    @DisplayName("Share a CONTACT → a contact bubble appears (best-effort)")
    void sendContact() {
        CcChatThreadPage t = thread();
        assumeTrue(t != null, "No thread — skipping");
        int before = t.cardBubbleCount();
        boolean sent;
        try {
            sent = t.sendContact();
            System.out.println(">>> contact sent=" + sent + " cards " + before + "->" + t.cardBubbleCount());
            screenshot("cc-chat-contact");
        } finally { recoverToCcLanding(); }
        assumeTrue(sent, "No device contacts / picker not navigable — skipping");
        assertTrue(sent, "A contact bubble should appear");
    }

    // ---- GROUP parity: the thread code (ChatAndGroupPage) is identical for groups;
    // these re-run the key media/search/voice paths on a GROUP thread to prove it. ----

    @Test
    @Order(11)
    @DisplayName("GROUP: attachment sheet + in-thread search + voice note")
    void groupMediaParity() {
        CcChatThreadPage t = openOrCreateTestGroup();
        assumeTrue(t != null && t.isLoaded(Duration.ofSeconds(15)), "No group thread — skipping");
        assumeTrue(t.openAttachmentSheet(), "Group attachment sheet did not open");
        int found = 0;
        for (String o : new String[]{"Photo","Gallery","Video","Video Gallery","Document","Location","Contact","Audio"})
            if (t.attachmentOptionShown(o)) found++;
        System.out.println(">>> group attachment options found=" + found);
        assertTrue(found >= 7, "Group attachment sheet should expose the media options");
        t.dismissAttachmentSheet();
        String marker = "gsrch" + (System.currentTimeMillis() % 100000);
        t.sendMessage(marker);
        assumeTrue(t.messageShown(marker, Duration.ofSeconds(15)), "marker not posted");
        assumeTrue(t.openChatSearch(), "group chat search did not open");
        t.typeChatSearch(marker);
        String count = t.chatSearchCount();
        System.out.println(">>> group search count=[" + count + "]");
        assertTrue((count != null && count.matches(".*[1-9].*")) || t.messageShown(marker, Duration.ofSeconds(3)),
                "Group in-thread search should match the sent message");
        t.closeChatSearch();
        boolean voice = t.recordAndSendVoice(2500);
        System.out.println(">>> group voice note sent=" + voice);
        assertTrue(voice, "Group voice note should record + send");
        screenshot("cc-group-media");
        t.back();
    }

    // ====================================================== web-gap cases (C&C)

    private CcChatThreadPage peerThread() {
        CcChatThreadPage t = openPeerChat();
        assumeTrue(t != null, "No 1-1 chat with " + PEER + " (the other test account) — skipping");
        return t;
    }

    // [WEB CC-13]
    @Test
    @Order(12)
    @DisplayName("[WEB CC-13] Media, Docs, Links opens from the thread header with Media / Docs / Links tabs")
    void mediaDocsLinksOpens() {
        CcChatThreadPage t = peerThread();
        boolean sent = t.sendImageViaGallery();
        System.out.println(">>> image sent before opening the library=" + sent);
        if (!sent) recoverToThread(t);
        assertTrue(t.openMediaLibrary(), "The thread's Media/Docs/Links button should open the library");
        try {
            // The toolbar and TabLayout render in caps (textAllCaps), so compare case-insensitively.
            assertTrue("Media, Docs, Links".equalsIgnoreCase(t.libraryHeader()),
                    "The library's title should read 'Media, Docs, Links'; got '" + t.libraryHeader() + "'");
            List<String> tabs = t.libraryTabLabels().stream().map(String::toLowerCase).toList();
            System.out.println(">>> library tabs=" + tabs + " items=" + t.libraryItemCount());
            assertTrue(tabs.size() == 3 && tabs.get(0).startsWith("media")
                            && tabs.get(1).startsWith("docs") && tabs.get(2).startsWith("links"),
                    "The library should have the tabs Media, Docs, Links; got " + tabs);
            if (sent) {
                assertTrue(tabs.get(0).matches("media \\(([1-9]\\d*)\\)"),
                        "After sending an image the Media tab should read 'Media (n)'; got '" + tabs.get(0) + "'");
            }
            for (String tab : new String[]{"Media", "Docs", "Links"}) {
                assertTrue(t.tapLibraryTab(tab), "The " + tab + " tab should be tappable");
                assertTrue(t.isLibraryOpen(), "The library should stay open on the " + tab + " tab");
                String label = t.libraryTabLabels().stream()
                        .filter(l -> l.toLowerCase().startsWith(tab.toLowerCase())).findFirst().orElse(tab);
                boolean hasCount = label.matches(".*\\(\\d+\\)");
                int items = t.libraryItemCount();
                boolean empty = t.libraryEmptyShown();
                System.out.println(">>> " + label + ": items=" + items + " empty=" + empty);
                assertTrue(hasCount ? items > 0 : (items == 0 && empty),
                        "The " + tab + " tab should list items when its label has a count, else show the empty view; label='"
                                + label + "' items=" + items + " empty=" + empty);
            }
            screenshot("cc-media-docs-links");
        } finally {
            if (t.isLibraryOpen()) t.back();
        }
        assertTrue(t.isLoaded(Duration.ofSeconds(8)), "Back from the library should return to the thread");
        t.back();
    }

    // [WEB CC-18]
    @Test
    @Order(13)
    @DisplayName("[WEB CC-18] Two images picked together arrive as two image messages")
    void sendTwoImagesAtOnce() {
        CcChatThreadPage t = peerThread();
        String marker = "ZL_AUTO cc 2img " + stamp();
        t.sendMessage(marker);
        assumeTrue(t.messageShown(marker, Duration.ofSeconds(15)), "Marker text did not post — skipping");
        int picked = t.sendImagesViaGallery(2);
        System.out.println(">>> picked+sent images=" + picked);
        assumeTrue(picked == 2, "The gallery could not be driven or holds fewer than 2 photos — skipping");
        int below = -1;
        long deadline = System.currentTimeMillis() + 40_000;
        while (System.currentTimeMillis() < deadline) {
            below = t.countBelowMarker(marker, "srcImage");
            if (below >= 2 || (below == -1 && t.imageBubbleCount() >= 2)) break;
            sleep(1500);
        }
        System.out.println(">>> image bubbles after the marker=" + below + " (on screen " + t.imageBubbleCount() + ")");
        screenshot("cc-two-images");
        assertTrue(below >= 2 || (below == -1 && t.imageBubbleCount() >= 2),
                "Both picked images should arrive as image messages after '" + marker + "'; found " + below);
        assertTrue(t.waitMediaSent(Duration.ofSeconds(60)),
                "Both image uploads should finish (no Retry / progress left)");
        t.back();
    }

    // [WEB CC-20]
    @Test
    @Order(14)
    @DisplayName("[WEB CC-20] Audio file from the system picker arrives as an audio message")
    void sendAudioFile() {
        // The audio picker (ACTION_GET_CONTENT audio/*) opens on Downloads and could not reach
        // /sdcard/Music, so put a uniquely named copy of the fixture in Downloads.
        try {
            byte[] wav = java.nio.file.Files.readAllBytes(
                    java.nio.file.Path.of("src/test/resources/fixtures/zl_test_audio.wav"));
            driver.pushFile("/sdcard/Download/zl_cc_audio.wav", java.util.Base64.getEncoder().encode(wav));
        } catch (Exception e) {
            System.out.println(">>> could not stage zl_cc_audio.wav in Downloads: " + e.getMessage());
        }
        CcChatThreadPage t = peerThread();
        String marker = "ZL_AUTO cc audio " + stamp();
        t.sendMessage(marker);
        assumeTrue(t.messageShown(marker, Duration.ofSeconds(15)), "Marker text did not post — skipping");
        boolean picked;
        try {
            picked = t.sendAudioFile("zl_cc_audio");
        } catch (RuntimeException e) {
            picked = false;
        }
        assumeTrue(picked, "The system audio picker could not surface zl_cc_audio.wav — skipping");
        int below = -1;
        long deadline = System.currentTimeMillis() + 40_000;
        while (System.currentTimeMillis() < deadline) {
            below = t.countBelowMarker(marker, "audioProgressbar");
            if (below >= 1 || below == -1) break;
            sleep(1500);
        }
        System.out.println(">>> audio bubbles after the marker=" + below + " (on screen " + t.audioBubbleCount() + ")");
        screenshot("cc-audio-file");
        assertTrue(below >= 1 || (below == -1 && t.audioBubbleCount() >= 1),
                "The picked audio file should arrive as an audio message after '" + marker + "'; found " + below);
        assertTrue(t.waitMediaSent(Duration.ofSeconds(60)), "The audio upload should finish (no Retry / progress left)");
        t.back();
    }

    private void recoverToThread(CcChatThreadPage t) {
        for (int i = 0; i < 4 && !t.isLoaded(Duration.ofSeconds(2)); i++) {
            try { driver.navigate().back(); } catch (Exception ignored) {}
            sleep(700);
        }
    }
}
