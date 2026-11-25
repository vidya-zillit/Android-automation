package com.zillit.automation.pages.tools;

import com.zillit.automation.pages.common.BasePage;
import com.zillit.automation.pages.common.BottomNavPage;
import com.zillit.automation.pages.homechat.HomeChatPage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;

import java.time.Duration;

/**
 * Tools → CATERING (CateringActivity). The chat itself is the Bulletin-style
 * Home units chat (drive it with {@link HomeChatPage}); this page covers the
 * two-sided CATERER-only affordances:
 *
 * <ul>
 *   <li>{@code addMember} — the "Select User" chip (caterer only). Opens an Alert
 *       with {@code leftButton}="All Users" (broadcast) and {@code rightButton}=
 *       "Select User" (directed to one user).</li>
 *   <li>{@code userName} — the chosen recipient label ("All Users" / a user name).</li>
 *   <li>{@code layNoMsg} — the grey hint bar shown while no recipient is chosen;
 *       the input bar is hidden until then.</li>
 *   <li>{@code createNewUnit} — the "+" that adds a meal unit (caterer only).</li>
 * </ul>
 *
 * A regular (non-caterer) user sees none of these and can always type.
 */
public class CateringPage extends BasePage {

    public CateringPage(AndroidDriver driver) {
        super(driver);
    }

    /** True when the current account is acting as a CATERER (recipient chip present). */
    public boolean isCatererMode() {
        return isVisible("addMember", Duration.ofSeconds(4));
    }

    public boolean isRecipientHintShown() {
        return isVisible("layNoMsg", Duration.ofSeconds(3));
    }

    public boolean isCreateUnitVisible() {
        return isVisible("createNewUnit", Duration.ofSeconds(3));
    }

    // ---------------- create meal unit (caterer only) ----------------
    // "+" (createNewUnit) → "ADD CATERING UNIT" screen: unitName → editTextInput,
    // submit via btnSubmit → buttonBtn ("Submit").

    public boolean openCreateUnit() {
        try { driver.findElement(byId("createNewUnit")).click(); sleep(1500); return true; }
        catch (WebDriverException e) { return false; }
    }

    public boolean isCreateUnitScreenOpen(Duration timeout) {
        return textShown("ADD CATERING UNIT", timeout)
                || isVisible("unitName", Duration.ofSeconds(3));
    }

    public void setUnitName(String name) {
        try {
            var field = driver.findElement(byId("unitName")).findElement(byId("editTextInput"));
            field.click();
            field.clear();
            field.sendKeys(name);
            hideKeyboard();
        } catch (WebDriverException outer) {
            try {
                var f = driver.findElement(byId("editTextInput"));
                f.click(); f.clear(); f.sendKeys(name); hideKeyboard();
            } catch (WebDriverException ignored) { }
        }
    }

    public String unitNameText() {
        try { return driver.findElement(byId("unitName")).findElement(byId("editTextInput")).getText(); }
        catch (WebDriverException e) { return ""; }
    }

    public boolean submitUnit() {
        try {
            driver.findElement(byId("btnSubmit")).click();
            sleep(2500);
            return true;
        } catch (WebDriverException e) {
            try {
                driver.findElement(byId("buttonBtn")).click();
                sleep(2500);
                return true;
            } catch (WebDriverException e2) { return false; }
        }
    }

    /** Back out of the create-unit screen without creating anything. */
    public void cancelCreateUnit() {
        try { driver.findElement(byId("backBtn")).click(); }
        catch (WebDriverException e) {
            try { driver.navigate().back(); } catch (WebDriverException ignored) { }
        }
        sleep(1200);
    }

    /** The recipient currently selected ("All Users", a user name, or "" if none). */
    public String selectedRecipient() {
        try { return driver.findElement(byId("userName")).getText(); }
        catch (WebDriverException e) { return ""; }
    }

    /** Tap the "Select User" chip to open the recipient dialog. */
    public boolean openRecipientPicker() {
        try { driver.findElement(byId("addMember")).click(); sleep(1200); return true; }
        catch (WebDriverException e) { return false; }
    }

    public boolean isRecipientDialogOpen(Duration timeout) {
        return textShown("All Users", timeout) && textShown("Select User", Duration.ofSeconds(2));
    }

    /** Dialog → "All Users" (broadcast to every user + caterer). */
    public boolean chooseAllUsers() {
        try { driver.findElement(byId("leftButton")).click(); sleep(1500); return true; }
        catch (WebDriverException e) { return false; }
    }

    /** Dialog → "Select User" (opens the user list for a directed message). */
    public boolean chooseSelectUser() {
        try { driver.findElement(byId("rightButton")).click(); sleep(1800); return true; }
        catch (WebDriverException e) { return false; }
    }

    public boolean cancelRecipientDialog() {
        try { driver.findElement(byId("btn_cancel")).click(); sleep(800); return true; }
        catch (WebDriverException e) { return false; }
    }

    /** In the "Select User" list, pick the first user; returns their name ("" if none). */
    public String pickFirstUserFromList() {
        for (String id : new String[]{"userNameTv", "name", "nameTv", "title"}) {
            try {
                var rows = driver.findElements(byId(id));
                if (!rows.isEmpty()) {
                    String n = rows.get(0).getText();
                    rows.get(0).click();
                    sleep(1500);
                    confirmUserSelection();
                    return n == null ? "" : n;
                }
            } catch (WebDriverException ignored) {
            }
        }
        return "";
    }

    /** Some user pickers need an explicit submit/done after ticking a row. */
    private void confirmUserSelection() {
        for (String label : new String[]{"submit", "done", "ok", "save", "select"}) {
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiSelector().clickable(true).textMatches(\"(?i)" + label + "\")")).click();
                sleep(1000);
                return;
            } catch (WebDriverException ignored) {
            }
        }
    }

    /**
     * Makes the chat postable regardless of role: a caterer must pick a recipient
     * before the keyboard appears, a regular user can always type. Returns true if
     * the input bar is usable afterwards.
     */
    public boolean ensurePostable() {
        HomeChatPage chat = new HomeChatPage(driver);
        if (chat.isInputBarVisible()) return true;
        if (!isCatererMode()) return false;
        if (!openRecipientPicker()) return false;
        if (!isRecipientDialogOpen(Duration.ofSeconds(6))) return false;
        chooseAllUsers();
        return chat.isInputBarVisible();
    }

    // ---------------- Settings → Catering Settings (caterer only) ----------------
    // SettingPage lists "Catering Settings" for a catering-department user; it opens
    // AccAndCaterSettingsActivity: recMembers rows (item_unit_list_layout.xml:
    // itemCommonListContainer → labelTextView / editImage / deleteImage).
    // Delete → snackbar "Are you sure you want to delete this unit?" with a "Yes" action
    // (Material snackbar_action). Edit → CreateNewUnitPage "Edit Catering Unit", the same
    // unitName / btnSubmit form the "+" opens.

    /** Leaves the Catering module, switches to the Settings tab and opens Catering Settings. */
    public boolean openCateringSettings() {
        for (int i = 0; i < 4 && driver.findElements(byId("bottomNavigation")).isEmpty(); i++) {
            try { driver.navigate().back(); } catch (WebDriverException ignored) { }
            sleep(900);
        }
        if (!new BottomNavPage(driver).selectTab("Settings")) return false;
        sleep(1200);
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true))"
                            + ".scrollIntoView(new UiSelector().text(\"Catering Settings\"))")).click();
        } catch (WebDriverException e) {
            return false;
        }
        sleep(2000);
        return isCateringSettingsOpen(Duration.ofSeconds(10));
    }

    public boolean isCateringSettingsOpen(Duration timeout) {
        return isVisible("recMembers", timeout)
                && driver.findElements(byId("bottomNavigation")).isEmpty();
    }

    /** Back out of Catering Settings and land on the Tools tab, ready to reopen the module. */
    public void leaveCateringSettings() {
        for (int i = 0; i < 3 && driver.findElements(byId("bottomNavigation")).isEmpty(); i++) {
            try { driver.navigate().back(); } catch (WebDriverException ignored) { }
            sleep(900);
        }
        new BottomNavPage(driver).selectTab("Tools");
    }

    /** Every unit name in Catering Settings, following the list past the fold. */
    public java.util.List<String> settingsUnitNames() {
        return readWholeList("recMembers", "labelTextView", () -> {
            java.util.List<String> out = new java.util.ArrayList<>();
            for (var e : driver.findElements(byId("labelTextView"))) {
                try {
                    String t = e.getText();
                    if (t != null && !t.isBlank()) out.add(t.trim());
                } catch (WebDriverException ignored) { }
            }
            return out;
        }, n -> n);
    }

    public boolean settingsUnitListed(String name) {
        return settingsUnitNames().stream().anyMatch(n -> n.equalsIgnoreCase(name));
    }

    /** Taps a row button (editImage / deleteImage) on the Catering Settings row named {@code name}. */
    private boolean tapSettingsRowButton(String name, String buttonId) {
        if (!scrollToRow("recMembers", "labelTextView", name)) return false;
        String pkg = appPackage();
        try {
            driver.findElement(AppiumBy.xpath(
                    "//*[@resource-id='" + pkg + ":id/itemCommonListContainer']"
                            + "[.//*[@resource-id='" + pkg + ":id/labelTextView' and @text='" + name + "']]"
                            + "//*[@resource-id='" + pkg + ":id/" + buttonId + "']")).click();
            sleep(1200);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Edit on the row → the "Edit Catering Unit" form. */
    public boolean editUnitInSettings(String name) {
        return tapSettingsRowButton(name, "editImage");
    }

    /**
     * Delete on the row, then "Yes" on the confirmation snackbar. Returns false when either
     * the row button or the confirmation is missing. The snackbar lasts 4 s, so it is
     * tapped straight away.
     */
    public boolean deleteUnitInSettings(String name) {
        if (!tapSettingsRowButton(name, "deleteImage")) return false;
        long deadline = System.currentTimeMillis() + 4000;
        while (System.currentTimeMillis() < deadline) {
            try {
                var yes = driver.findElements(byId("snackbar_action"));
                if (!yes.isEmpty()) { yes.get(0).click(); return true; }
                var byText = driver.findElements(AppiumBy.androidUIAutomator(
                        "new UiSelector().clickable(true).text(\"Yes\")"));
                if (!byText.isEmpty()) { byText.get(0).click(); return true; }
            } catch (WebDriverException ignored) { }
            sleep(250);
        }
        return false;
    }

    // ---------------- web-gap helpers (gaps/catering.md) ----------------

    /** The composer's current text ("" when empty or absent). Draft cases M3. */
    public String composerText() {
        try {
            var f = driver.findElements(byId("etMessage"));
            for (var e : f) {
                if (!e.isDisplayed()) continue;
                String t = e.getText();
                return t == null ? "" : t;
            }
        } catch (WebDriverException ignored) { }
        return "";
    }

    /** Types into the composer WITHOUT sending (leaves a draft). */
    public boolean typeDraft(String text) {
        try {
            var f = driver.findElement(byId("etMessage"));
            f.click();
            f.clear();
            f.sendKeys(text);
            sleep(500);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public void clearComposer() {
        try { driver.findElement(byId("etMessage")).clear(); } catch (WebDriverException ignored) { }
    }

    /**
     * The sender name on the message row whose own text contains {@code text} — the row's
     * top-level {@code tvUserName}, not one inside its comment list ({@code rvMessageReply}).
     */
    public String senderOfRow(String text) {
        String pkg = appPackage();
        try {
            var cards = driver.findElements(AppiumBy.xpath(
                    "//*[@resource-id='" + pkg + ":id/materialCardView']"
                            + "[.//*[@resource-id='" + pkg + ":id/tvMessage' and contains(@text," + lit(text) + ")]]"));
            for (var c : cards) {
                for (var n : c.findElements(AppiumBy.xpath(
                        ".//*[@resource-id='" + pkg + ":id/tvUserName']"
                                + "[not(ancestor::*[@resource-id='" + pkg + ":id/rvMessageReply'])]"))) {
                    String t = n.getText();
                    if (t != null && !t.isBlank()) return t.trim();
                }
            }
        } catch (WebDriverException ignored) { }
        return "";
    }

    /** One rendered message row: its sender and its own text (comments excluded). */
    public static final class Row {
        public final String sender, text, audience;
        Row(String sender, String text, String audience) { this.sender = sender; this.text = text; this.audience = audience; }
        @Override public String toString() { return sender + ": " + text + " [" + audience + "]"; }
    }

    /** The message rows currently rendered (text rows only). */
    public java.util.List<Row> visibleRows() {
        java.util.List<Row> out = new java.util.ArrayList<>();
        String pkg = appPackage();
        String notReply = "[not(ancestor::*[@resource-id='" + pkg + ":id/rvMessageReply'])]";
        try {
            for (var c : driver.findElements(byId("materialCardView"))) {
                String sender = firstText(c, ".//*[@resource-id='" + pkg + ":id/tvUserName']" + notReply);
                String text = firstText(c, ".//*[@resource-id='" + pkg + ":id/tvMessage']" + notReply);
                String to = firstText(c, ".//*[@resource-id='" + pkg + ":id/receiverName']");
                if (!text.isBlank()) out.add(new Row(sender, text, to));
            }
        } catch (WebDriverException ignored) { }
        return out;
    }

    /**
     * A text row posted by someone other than {@code ownName}, searching older rows by
     * swiping the list down (towards older messages) up to {@code maxSwipes} times.
     */
    public Row foreignRow(String ownName, int maxSwipes) {
        for (int i = 0; i <= maxSwipes; i++) {
            for (Row r : visibleRows()) {
                if (!r.sender.isBlank() && !r.sender.equalsIgnoreCase(ownName)
                        && r.text.length() >= 3 && !r.text.contains("\n")) return r;
            }
            swipeChatOlder();
        }
        return null;
    }

    private void swipeChatOlder() {
        var size = driver.manage().window().getSize();
        int x = size.getWidth() / 2;
        var finger = new org.openqa.selenium.interactions.PointerInput(
                org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        var seq = new org.openqa.selenium.interactions.Sequence(finger, 1);
        seq.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, (int) (size.getHeight() * 0.35)));
        seq.addAction(finger.createPointerDown(0));
        seq.addAction(finger.createPointerMove(Duration.ofMillis(350),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, (int) (size.getHeight() * 0.72)));
        seq.addAction(finger.createPointerUp(0));
        try { driver.perform(java.util.Collections.singletonList(seq)); } catch (WebDriverException ignored) { }
        sleep(900);
    }

    /** Taps the message row carrying {@code text} (selection mode: toggles it). */
    public boolean tapRow(String text) {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceIdMatches(\".*:id/tvMessage\").textContains(\"" + esc(text) + "\")")).click();
            sleep(900);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Selection-mode counter ({@code tvCount}), "" when not shown. */
    public String selectionCount() {
        try {
            for (var e : driver.findElements(byId("tvCount"))) {
                if (e.isDisplayed()) { String t = e.getText(); return t == null ? "" : t.trim(); }
            }
        } catch (WebDriverException ignored) { }
        return "";
    }

    public boolean selectionBarShown() {
        try {
            for (var e : driver.findElements(byId("layOption"))) if (e.isDisplayed()) return true;
        } catch (WebDriverException ignored) { }
        return false;
    }

    /** {@code btnBack} on the selection / search bar. */
    public void leaveSelection() {
        try { driver.findElement(byId("btnBack")).click(); sleep(900); }
        catch (WebDriverException ignored) { }
    }

    /** A snackbar (or any label) with this text, polled up to {@code timeout}. */
    public boolean snackShown(String text, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            try {
                if (!driver.findElements(AppiumBy.androidUIAutomator(
                        "new UiSelector().textContains(\"" + esc(text) + "\")")).isEmpty()) return true;
            } catch (WebDriverException ignored) { }
            if (System.currentTimeMillis() >= deadline) return false;
            sleep(200);
        }
    }

    /** True while the REQUEST bar ({@code tvRequest}) is visible — Catering keeps it gone. */
    public boolean requestBarVisible() {
        try {
            for (var e : driver.findElements(byId("tvRequest"))) if (e.isDisplayed()) return true;
        } catch (WebDriverException ignored) { }
        return false;
    }

    // ---------------- uploads (Document / Image Gallery) ----------------

    /** Why the last upload helper returned false (foreground package/activity at that moment). */
    public String lastUploadProblem = "";

    private boolean uploadFailed(String step) {
        String pkg = "", act = "";
        try { pkg = driver.getCurrentPackage(); act = driver.currentActivity(); } catch (WebDriverException ignored) { }
        lastUploadProblem = step + " (foreground " + pkg + " / " + act + ")";
        System.out.println(">>> upload stopped: " + lastUploadProblem);
        return false;
    }

    /** Backs out of a picker / preview until the Catering composer (or the bottom nav) is back. */
    public void backToChat() {
        HomeChatPage chat = new HomeChatPage(driver);
        for (int i = 0; i < 5; i++) {
            if (isOnCatering() && chat.isInputBarVisible()) return;
            if (!driver.findElements(byId("bottomNavigation")).isEmpty()) return;
            try {
                String pkg = driver.getCurrentPackage();
                if (pkg != null && !pkg.equals(appPackage())) {
                    driver.executeScript("mobile: activateApp", java.util.Map.of("appId", appPackage()));
                    sleep(1200);
                    continue;
                }
                driver.navigate().back();
            } catch (WebDriverException ignored) { }
            sleep(1000);
            try {
                var yes = driver.findElements(AppiumBy.androidUIAutomator("new UiSelector().clickable(true).textMatches(\"(?i)(yes|discard|ok)\")"));
                if (!yes.isEmpty()) { yes.get(0).click(); sleep(800); }
            } catch (WebDriverException ignored) { }
        }
    }

    /**
     * Paperclip → Document → system picker → the fixture → preview (caption) → send.
     * Returns true once the picker handed the file back and the preview was sent (or
     * there was no preview). The caller checks the bubble.
     */
    public boolean sendDocument(String fixture, String caption) {
        HomeChatPage chat = new HomeChatPage(driver);
        if (!chat.isAttachmentVisible()) return false;
        chat.tapAttachment();
        if (!chat.isAttachmentPickerVisible()) return false;
        if (!chat.tapAttachmentOption("Document")) { chat.dismissAttachmentPicker(); return uploadFailed("no Document option"); }
        allowPermissionIfAsked();
        if (!new com.zillit.automation.pages.common.SystemFilePickerPage(driver).isOpen(Duration.ofSeconds(15))) { uploadFailed("system picker did not open"); backToChat(); return false; }
        if (!chat.pickSystemFileByName(fixture, Duration.ofSeconds(25))) { uploadFailed("picker could not pick " + fixture); backToChat(); return false; }
        if (!sendFromPreview(caption)) { uploadFailed("preview had no Send"); backToChat(); return false; }
        return true;
    }


    /** Waits for gallery thumbnails; when the album list is showing, opens the first non-empty album. */
    private int galleryThumbs(com.zillit.automation.pages.common.MediaPreviewPage media) {
        for (int i = 0; i < 12; i++) {
            int n = media.galleryThumbCount();
            if (n > 0) return n;
            if (i == 3) {
                // Album list showing: open the album with the most items.
                try {
                    var names = driver.findElements(byId("appCompatTextViewBucketName"));
                    var counts = driver.findElements(byId("appCompatTextViewBucketItemCount"));
                    int best = 0, bestN = -1;
                    for (int k = 0; k < counts.size() && k < names.size(); k++) {
                        int c;
                        try { c = Integer.parseInt(counts.get(k).getText().replaceAll("\\D", "")); } catch (Exception e) { c = 0; }
                        if (c > bestN) { bestN = c; best = k; }
                    }
                    if (!names.isEmpty()) { names.get(best).click(); sleep(1500); }
                } catch (WebDriverException ignored) { }
            }
            sleep(500);
        }
        return media.galleryThumbCount();
    }

    /** Paperclip → Image Gallery → first photo → preview (caption) → send. */
    public boolean sendGalleryImage(String caption) {
        HomeChatPage chat = new HomeChatPage(driver);
        if (!chat.isAttachmentVisible()) return false;
        chat.tapAttachment();
        if (!chat.isAttachmentPickerVisible()) return false;
        if (!chat.tapAttachmentOption("Image Gallery")) { chat.dismissAttachmentPicker(); return uploadFailed("no Image Gallery option"); }
        allowPermissionIfAsked();
        var media = new com.zillit.automation.pages.common.MediaPreviewPage(driver);
        if (!media.isGalleryOpen()) { uploadFailed("the in-app gallery did not open"); backToChat(); return false; }
        media.openAllMediaAlbumIfListed();
        if (galleryThumbs(media) == 0) { uploadFailed("gallery showed no photos"); backToChat(); return false; }
        media.selectPhotos(1);
        if (media.isSendFabVisible()) media.tapGallerySendFab();
        return sendFromPreview(caption);
    }

    /** Paperclip → Audio → system picker → the fixture → preview → send. */
    public boolean sendAudioFile(String fixture) {
        HomeChatPage chat = new HomeChatPage(driver);
        if (!chat.isAttachmentVisible()) return false;
        chat.tapAttachment();
        if (!chat.isAttachmentPickerVisible()) return false;
        if (!chat.tapAttachmentOption("Audio")) { chat.dismissAttachmentPicker(); return uploadFailed("no Audio option"); }
        allowPermissionIfAsked();
        if (!new com.zillit.automation.pages.common.SystemFilePickerPage(driver).isOpen(Duration.ofSeconds(15))) { uploadFailed("audio picker did not open"); backToChat(); return false; }
        if (!chat.pickSystemFileByName(fixture, Duration.ofSeconds(25))) { uploadFailed("picker could not pick " + fixture); backToChat(); return false; }
        if (!sendFromPreview("")) { uploadFailed("preview had no Send"); backToChat(); return false; }
        return true;
    }

    private boolean sendFromPreview(String caption) {
        var prev = new com.zillit.automation.pages.common.MediaPreviewPage(driver);
        long deadline = System.currentTimeMillis() + 12000;
        while (!prev.isPreviewOpen() && System.currentTimeMillis() < deadline) sleep(500);
        if (prev.isPreviewOpen()) {
            if (caption != null && !caption.isEmpty()) {
                try { prev.setCaption(caption); } catch (WebDriverException ignored) { }
            }
            try {
                prev.tapPreviewSend();
            } catch (WebDriverException e) {
                return false;
            }
            prev.waitPreviewClosed(Duration.ofSeconds(45));
        }
        return true;
    }

    private void allowPermissionIfAsked() {
        for (int i = 0; i < 3; i++) {
            try {
                var b = driver.findElements(AppiumBy.androidUIAutomator(
                        "new UiSelector().textMatches(\"(?i)(allow|while using the app|allow all)\")"));
                if (b.isEmpty()) return;
                b.get(0).click();
                sleep(700);
            } catch (WebDriverException e) { return; }
        }
    }

    /**
     * Long-presses the newest AUDIO row. AudioVH puts the long-click on {@code mainLi}; the
     * sender name inside it has no listener of its own, so pressing it reaches the row
     * without hitting the play button.
     */
    public boolean longPressNewestAudio() {
        String pkg = appPackage();
        HomeChatPage chat = new HomeChatPage(driver);
        try {
            var rows = driver.findElements(AppiumBy.xpath(
                    "//*[@resource-id='" + pkg + ":id/mainLi'][.//*[@resource-id='" + pkg + ":id/ivPlayPause']]"));
            if (rows.isEmpty()) return false;
            var row = rows.get(rows.size() - 1);
            var names = row.findElements(byId("tvUserName"));
            var target = names.isEmpty() ? row : names.get(0);
            var r = target.getRect();
            int x = names.isEmpty() ? r.getX() + r.getWidth() - 30 : r.getX() + r.getWidth() / 2;
            int y = r.getY() + r.getHeight() / 2;
            var finger = new org.openqa.selenium.interactions.PointerInput(
                    org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
            var seq = new org.openqa.selenium.interactions.Sequence(finger, 1);
            seq.addAction(finger.createPointerMove(Duration.ZERO,
                    org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, y));
            seq.addAction(finger.createPointerDown(0));
            seq.addAction(finger.createPointerMove(Duration.ofMillis(1300),
                    org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, y));
            seq.addAction(finger.createPointerUp(0));
            driver.perform(java.util.Collections.singletonList(seq));
            return chat.isMenuOpen(Duration.ofSeconds(4));
        } catch (WebDriverException e) {
            return false;
        }
    }

    /**
     * The unread badge on a meal tab ({@code custom_tabbar_item}: {@code menuText} + the
     * {@code zBadge} counter, HomeUnitHolder.handleBadges). "" when the tab shows no badge.
     */
    public String mealTabBadge(String meal) {
        String pkg = appPackage();
        try {
            var chips = driver.findElements(AppiumBy.xpath(
                    "//*[@resource-id='" + pkg + ":id/recTab']/*[.//*[@resource-id='" + pkg
                            + ":id/menuText' and @text=" + lit(meal) + "]]"));
            for (var c : chips) {
                for (var b : c.findElements(byId("zBadge"))) {
                    if (b.isDisplayed()) { String t = b.getText(); return t == null ? "" : t.trim(); }
                }
            }
        } catch (WebDriverException ignored) { }
        return "";
    }

    /** The Catering screen (meal strip, no bottom nav) is in front. */
    public boolean isOnCatering() {
        return isCateringScreen(Duration.ofSeconds(2));
    }

    /**
     * The Catering screen itself: its own meal strip (a Breakfast chip) with no bottom nav.
     * Confidential Info / Info / Bulletin have chats too, so "a chat is open" is not enough.
     */
    public boolean isCateringScreen(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        String pkg = appPackage();
        while (true) {
            try {
                if (driver.findElements(byId("bottomNavigation")).isEmpty()
                        && !driver.findElements(AppiumBy.xpath("//*[@resource-id='" + pkg + ":id/recTab']//*[@resource-id='"
                        + pkg + ":id/menuText' and @text='Breakfast']")).isEmpty()) return true;
            } catch (WebDriverException ignored) { }
            if (System.currentTimeMillis() >= deadline) return false;
            sleep(400);
        }
    }

    /**
     * Opens Tools → Catering by the tile's TEXT and verifies it landed on Catering. The Tools list
     * re-sorts live (badged tiles first, ToolsGroupedAdapter), so a row found by text can move
     * under the finger before the tap lands: a wrong landing is backed out of and retried.
     */
    public boolean openCateringFromTools() {
        if (isCateringScreen(Duration.ofSeconds(1))) return true;
        var tools = new com.zillit.automation.pages.common.ToolsPage(driver);
        for (int attempt = 0; attempt < 3; attempt++) {
            for (int i = 0; i < 4 && driver.findElements(byId("bottomNavigation")).isEmpty(); i++) {
                try { driver.navigate().back(); } catch (WebDriverException ignored) { }
                sleep(700);
            }
            if (!tools.openToolsTab() || !tools.isLoaded(Duration.ofSeconds(15))) continue;
            tools.searchTools("Catering");
            if (!tools.isModuleListed("Catering", Duration.ofSeconds(6))) continue;
            sleep(800);   // let a live re-sort settle before the tap
            if (!tapToolTileByText("Catering")) continue;
            if (isCateringScreen(Duration.ofSeconds(15))) return true;
            System.out.println(">>> Tools tap did not land on Catering (attempt " + (attempt + 1) + ") — backing out");
        }
        return false;
    }

    /** Taps the Tools row whose label TextView reads exactly {@code label} (row text, never index). */
    private boolean tapToolTileByText(String label) {
        String pkg = appPackage();
        try {
            var rows = driver.findElements(AppiumBy.xpath("//*[@resource-id='" + pkg + ":id/nameTv' and @text=" + lit(label) + "]"));
            if (rows.isEmpty()) rows = driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().className(\"android.widget.TextView\").text(\"" + label + "\")"));
            for (var r : rows) {
                if (!label.equals(r.getText())) continue;
                r.click();
                sleep(1500);
                return true;
            }
        } catch (WebDriverException ignored) { }
        return false;
    }

    /** Taps Retry ({@code btnTryAgain}) on the row carrying {@code text}. */
    public boolean tapRetryOn(String text) {
        String pkg = appPackage();
        try {
            driver.findElement(AppiumBy.xpath("//*[@resource-id='" + pkg + ":id/materialCardView'][.//*[contains(@text,"
                    + lit(text) + ")]]//*[@resource-id='" + pkg + ":id/btnTryAgain']")).click();
            sleep(1200);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** A chat row (tvMessage, not the composer) whose text contains {@code part}. */
    public boolean rowContaining(String part, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            try {
                if (!driver.findElements(AppiumBy.xpath("//*[@resource-id='" + appPackage()
                        + ":id/tvMessage' and contains(@text," + lit(part) + ")]")).isEmpty()) return true;
            } catch (WebDriverException ignored) { }
            if (System.currentTimeMillis() >= deadline) return false;
            sleep(500);
        }
    }

    /** How many rows show exactly this text. */
    public int exactRowCount(String text) {
        try {
            return driver.findElements(AppiumBy.xpath("//*[@resource-id='" + appPackage()
                    + ":id/tvMessage' and @text=" + lit(text) + "]")).size();
        } catch (WebDriverException e) {
            return 0;
        }
    }

    /** The {@code tvMsgTime} label of the row carrying {@code text} ("Edited …" once edited). */
    public String rowTimeText(String text) {
        String pkg = appPackage();
        try {
            for (var e : driver.findElements(AppiumBy.xpath("//*[@resource-id='" + pkg + ":id/materialCardView'][.//*[@resource-id='"
                    + pkg + ":id/tvMessage' and contains(@text," + lit(text) + ")]]//*[@resource-id='" + pkg + ":id/tvMsgTime']"))) {
                String t = e.getText();
                if (t != null && !t.isBlank()) return t.trim();
            }
        } catch (WebDriverException ignored) { }
        return "";
    }

    // ---------------- in-chat search ----------------

    public boolean openSearch() {
        try {
            driver.findElement(byId("ivSearch")).click();
            sleep(1000);
            return isVisible("edittext_search", Duration.ofSeconds(4));
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Types {@code q} into the search bar and presses the IME Search action. */
    public void search(String q) {
        try {
            var box = driver.findElement(byId("edittext_search"));
            box.click();
            box.clear();
            if (!q.isEmpty()) box.sendKeys(q);
            driver.executeScript("mobile: performEditorAction", java.util.Map.of("action", "search"));
            sleep(1500);
        } catch (WebDriverException ignored) { }
    }

    /** The search match counter ({@code tvCount}) once it shows a number. */
    public String searchCount(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        String last = "";
        while (System.currentTimeMillis() < deadline) {
            last = selectionCount();
            if (last.matches("\\d+")) return last;
            sleep(400);
        }
        return last;
    }

    // ---------------- batch 2 helpers ----------------

    /** Image Gallery with {@code n} photos selected at once. */
    public boolean sendGalleryImages(int n, String caption) {
        HomeChatPage chat = new HomeChatPage(driver);
        if (!chat.isAttachmentVisible()) return false;
        chat.tapAttachment();
        if (!chat.isAttachmentPickerVisible()) return false;
        if (!chat.tapAttachmentOption("Image Gallery")) { chat.dismissAttachmentPicker(); return uploadFailed("no Image Gallery option"); }
        allowPermissionIfAsked();
        var media = new com.zillit.automation.pages.common.MediaPreviewPage(driver);
        if (!media.isGalleryOpen()) { uploadFailed("the in-app gallery did not open"); backToChat(); return false; }
        media.openAllMediaAlbumIfListed();
        if (galleryThumbs(media) < n) { uploadFailed("gallery showed fewer than " + n + " photos"); backToChat(); return false; }
        media.selectPhotos(n);
        if (media.isSendFabVisible()) media.tapGallerySendFab();
        return sendFromPreview(caption);
    }

    /** Document → fixture → the preview is up; returns true when the preview opened (nothing sent). */
    public boolean openDocumentPreview(String fixture) {
        HomeChatPage chat = new HomeChatPage(driver);
        if (!chat.isAttachmentVisible()) return false;
        chat.tapAttachment();
        if (!chat.isAttachmentPickerVisible()) return false;
        if (!chat.tapAttachmentOption("Document")) { chat.dismissAttachmentPicker(); return false; }
        allowPermissionIfAsked();
        if (!new com.zillit.automation.pages.common.SystemFilePickerPage(driver).isOpen(Duration.ofSeconds(15))) return false;
        if (!chat.pickSystemFileByName(fixture, Duration.ofSeconds(25))) return false;
        var prev = new com.zillit.automation.pages.common.MediaPreviewPage(driver);
        long deadline = System.currentTimeMillis() + 12000;
        while (!prev.isPreviewOpen() && System.currentTimeMillis() < deadline) sleep(500);
        return prev.isPreviewOpen();
    }

    /** Backs out of the media preview without sending (answers a leave prompt with Yes/OK). */
    public void cancelPreview() {
        var prev = new com.zillit.automation.pages.common.MediaPreviewPage(driver);
        try { prev.tapPreviewBack(); } catch (WebDriverException e) { try { driver.navigate().back(); } catch (WebDriverException ignored) { } }
        sleep(800);
        try { prev.confirmExitIfPrompted(); } catch (WebDriverException ignored) { }
        sleep(1000);
    }

    // reply bar (llReply inside the keyboard fragment)

    public boolean replyBarShown() {
        try { for (var e : driver.findElements(byId("llReply"))) if (e.isDisplayed()) return true; }
        catch (WebDriverException ignored) { }
        return false;
    }

    public String replyQuoteText() {
        try {
            var bar = driver.findElements(byId("llReply"));
            if (bar.isEmpty()) return "";
            var m = bar.get(0).findElements(byId("tvMessage"));
            if (m.isEmpty()) return "";
            String t = m.get(0).getText();
            return t == null ? "" : t;
        } catch (WebDriverException e) {
            return "";
        }
    }

    public boolean paperclipShown() {
        try { for (var e : driver.findElements(byId("ivAttachment"))) if (e.isDisplayed()) return true; }
        catch (WebDriverException ignored) { }
        return false;
    }

    // comments (rvMessageReply → reply_chat_item: tvUserName / tvMessage / deleteIv / tvMsgTime)

    /** How many comment rows carry exactly {@code text}. */
    public int commentCount(String text) {
        String pkg = appPackage();
        try {
            return driver.findElements(AppiumBy.xpath("//*[@resource-id='" + pkg + ":id/rvMessageReply']//*[@resource-id='"
                    + pkg + ":id/tvMessage' and @text=" + lit(text) + "]")).size();
        } catch (WebDriverException e) {
            return 0;
        }
    }

    public boolean commentShown(String part) {
        String pkg = appPackage();
        try {
            return !driver.findElements(AppiumBy.xpath("//*[@resource-id='" + pkg + ":id/rvMessageReply']//*[@resource-id='"
                    + pkg + ":id/tvMessage' and contains(@text," + lit(part) + ")]")).isEmpty();
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Taps the comment's arrow ({@code deleteIv}) to open its option menu. */
    public boolean openCommentMenu(String text) {
        hideKeyboard();
        sleep(600);
        String pkg = appPackage();
        try {
            var arrows = driver.findElements(AppiumBy.xpath("//*[@resource-id='" + pkg + ":id/tvMessage' and contains(@text,"
                    + lit(text) + ")]/ancestor::*[.//*[@resource-id='" + pkg + ":id/deleteIv']][1]//*[@resource-id='"
                    + pkg + ":id/deleteIv']"));
            if (arrows.isEmpty()) return false;
            var r = arrows.get(0).getRect();
            tap(r.getX() + r.getWidth() / 2, r.getY() + r.getHeight() / 2);
            return new HomeChatPage(driver).isMenuOpen(Duration.ofSeconds(4));
        } catch (WebDriverException e) {
            return false;
        }
    }

    // emoji picker

    public boolean openEmojiPicker() {
        try {
            driver.findElement(byId("ivEmoji")).click();
            sleep(1200);
            return emojiPickerShown();
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean emojiPickerShown() {
        try { for (var e : driver.findElements(byId("emojiPicker"))) if (e.isDisplayed()) return true; }
        catch (WebDriverException ignored) { }
        return false;
    }

    /** Taps the first emoji cell of the grid (below the category header row). */
    public boolean tapFirstEmoji() {
        try {
            var picker = driver.findElement(byId("emojiPicker"));
            var pr = picker.getRect();
            int minY = pr.getY() + pr.getHeight() / 5;
            for (var e : picker.findElements(AppiumBy.xpath(".//*"))) {
                var r = e.getRect();
                if (r.getY() < minY || r.getWidth() <= 0 || r.getWidth() > pr.getWidth() / 3) continue;
                if (!"true".equals(e.getAttribute("clickable"))) continue;
                e.click();
                sleep(700);
                return true;
            }
        } catch (WebDriverException ignored) { }
        return false;
    }

    public void tapComposer() {
        try { driver.findElement(byId("etMessage")).click(); sleep(800); } catch (WebDriverException ignored) { }
    }

    // read more

    /** The full text of the row's {@code tvMessage} containing {@code part}. */
    public String rowText(String part) {
        String pkg = appPackage();
        try {
            for (var e : driver.findElements(AppiumBy.xpath("//*[@resource-id='" + pkg + ":id/tvMessage' and contains(@text,"
                    + lit(part) + ")]"))) {
                String t = e.getText();
                if (t != null) return t;
            }
        } catch (WebDriverException ignored) { }
        return "";
    }

    /** Taps the bottom-right end of the row's text (where the Read more / Read less span sits). */
    public boolean tapEndOfRow(String part) {
        String pkg = appPackage();
        try {
            var l = driver.findElements(AppiumBy.xpath("//*[@resource-id='" + pkg + ":id/tvMessage' and contains(@text,"
                    + lit(part) + ")]"));
            if (l.isEmpty()) return false;
            var r = l.get(0).getRect();
            tap(r.getX() + r.getWidth() - 25, r.getY() + r.getHeight() - 12);
            sleep(1200);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Taps the start of the row's text (where a link at the start of the message sits). */
    public boolean tapStartOfRow(String part) {
        String pkg = appPackage();
        try {
            var l = driver.findElements(AppiumBy.xpath("//*[@resource-id='" + pkg + ":id/tvMessage' and contains(@text,"
                    + lit(part) + ")]"));
            if (l.isEmpty()) return false;
            var r = l.get(0).getRect();
            tap(r.getX() + 30, r.getY() + 15);
            sleep(2500);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    private void tap(int x, int y) {
        var finger = new org.openqa.selenium.interactions.PointerInput(
                org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        var seq = new org.openqa.selenium.interactions.Sequence(finger, 1);
        seq.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, y));
        seq.addAction(finger.createPointerDown(0));
        seq.addAction(finger.createPointerMove(Duration.ofMillis(60),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, y));
        seq.addAction(finger.createPointerUp(0));
        driver.perform(java.util.Collections.singletonList(seq));
    }

    // scroll-to-bottom arrow

    public boolean arrowDownShown() {
        try { for (var e : driver.findElements(byId("arrowDownCard"))) if (e.isDisplayed()) return true; }
        catch (WebDriverException ignored) { }
        return false;
    }

    public boolean tapArrowDown() {
        try { driver.findElement(byId("arrowDownCard")).click(); sleep(1500); return true; }
        catch (WebDriverException e) { return false; }
    }

    /** Swipes the list towards OLDER messages {@code n} times. */
    public void swipeOlder(int n) { for (int i = 0; i < n; i++) swipeChatOlder(); }

    /** Double-taps Send quickly (two clicks back to back). */
    public void doubleTapSend() {
        try {
            var b = driver.findElement(byId("ivSendMessage"));
            b.click();
            try { b.click(); } catch (WebDriverException ignored) { }
        } catch (WebDriverException ignored) { }
        sleep(1500);
    }

    // ForwardPage

    /** All non-blank labels in ForwardPage's list (rvView). */
    public java.util.List<String> forwardListLabels() {
        java.util.List<String> out = new java.util.ArrayList<>();
        try {
            var rv = driver.findElements(byId("rvView"));
            if (rv.isEmpty()) return out;
            for (var e : rv.get(0).findElements(AppiumBy.className("android.widget.TextView"))) {
                String t = e.getText();
                if (t != null && !t.isBlank()) out.add(t.trim());
            }
        } catch (WebDriverException ignored) { }
        return out;
    }

    public boolean tapForwardOption(String label) {
        try {
            var rv = driver.findElement(byId("rvView"));
            for (var e : rv.findElements(AppiumBy.className("android.widget.TextView"))) {
                if (label.equalsIgnoreCase(String.valueOf(e.getText()).trim())) { e.click(); sleep(2000); return true; }
            }
        } catch (WebDriverException ignored) { }
        return false;
    }

    // Select Users list (UsersListPage, caterer's "Select User")

    public boolean usersListOpen(Duration timeout) {
        return isVisible("recDepartments", timeout);
    }

    public java.util.List<String> usersListNames() {
        java.util.List<String> out = new java.util.ArrayList<>();
        try {
            var rv = driver.findElements(byId("recDepartments"));
            if (rv.isEmpty()) return out;
            for (var e : rv.get(0).findElements(AppiumBy.className("android.widget.TextView"))) {
                String t = e.getText();
                if (t != null && !t.isBlank()) out.add(t.trim());
            }
        } catch (WebDriverException ignored) { }
        return out;
    }

    public void searchUsersList(String q) {
        try {
            var box = driver.findElement(byId("searchView")).findElement(byId("searchWidgetEditTextvw"));
            box.click(); box.clear();
            if (!q.isEmpty()) box.sendKeys(q);
            sleep(1500);
        } catch (WebDriverException ignored) { }
    }

    public boolean usersListEmptyShown() {
        try { for (var e : driver.findElements(byId("emptyMainLi"))) if (e.isDisplayed()) return true; }
        catch (WebDriverException ignored) { }
        return false;
    }

    // Tools tab tile badge

    /** The unread badge on the Tools tab's Catering row ("" when none). */
    public String toolsTileBadge() {
        String pkg = appPackage();
        try {
            for (var row : driver.findElements(AppiumBy.xpath("//*[@resource-id='" + pkg + ":id/commonListItemHolder'][.//*[@resource-id='"
                    + pkg + ":id/nameTv' and @text='Catering']]"))) {
                for (var b : row.findElements(byId("badge"))) {
                    if (b.isDisplayed()) { String t = b.getText(); return t == null ? "" : t.trim(); }
                }
            }
        } catch (WebDriverException ignored) { }
        return "";
    }

    /** Leaves Catering to the bottom-nav Tools tab (Catering row visible after a search). */
    public boolean showToolsRow() {
        for (int i = 0; i < 4 && driver.findElements(byId("bottomNavigation")).isEmpty(); i++) {
            try { driver.navigate().back(); } catch (WebDriverException ignored) { }
            sleep(800);
        }
        var tools = new com.zillit.automation.pages.common.ToolsPage(driver);
        if (!tools.openToolsTab() || !tools.isLoaded(Duration.ofSeconds(15))) return false;
        tools.searchTools("Catering");
        return tools.isModuleListed("Catering", Duration.ofSeconds(6));
    }

    /**
     * Taps the newest IMAGE bubble (an imageParent with no attachmentType document badge) in its
     * centre and reports whether CustomImageViewer — Catering's openFileListShowingPage — opened.
     */
    public boolean openNewestImageViewer() {
        try {
            var badges = new java.util.ArrayList<org.openqa.selenium.Rectangle>();
            for (var b : driver.findElements(byId("attachmentType"))) badges.add(b.getRect());
            org.openqa.selenium.WebElement best = null;
            int bestBottom = -1;
            for (var p : driver.findElements(byId("imageParent"))) {
                var r = p.getRect();
                if (r.getHeight() <= 0) continue;
                boolean doc = badges.stream().anyMatch(o -> {
                    int cx = o.getX() + o.getWidth() / 2, cy = o.getY() + o.getHeight() / 2;
                    return cx >= r.getX() && cx <= r.getX() + r.getWidth() && cy >= r.getY() && cy <= r.getY() + r.getHeight();
                });
                if (!doc && r.getY() + r.getHeight() > bestBottom) { best = p; bestBottom = r.getY() + r.getHeight(); }
            }
            if (best == null) return false;
            var r = best.getRect();
            tap(r.getX() + r.getWidth() / 2, r.getY() + r.getHeight() / 2);
            long deadline = System.currentTimeMillis() + 10000;
            while (System.currentTimeMillis() < deadline) {
                String a = driver.currentActivity();
                if (a != null && a.contains("CustomImageViewer")) return true;
                sleep(500);
            }
        } catch (WebDriverException ignored) { }
        return false;
    }

    /**
     * Taps along the LAST line of the row's text until the "Read more"/"Read less" span toggles it
     * (UiAutomator cannot locate a span; the span ends the text, so its x depends on the line length).
     */
    public boolean toggleReadMore(String part, java.util.function.Predicate<String> done) {
        String pkg = appPackage();
        for (int i = 0; i < 8; i++) {
            try {
                var l = driver.findElements(AppiumBy.xpath("//*[@resource-id='" + pkg + ":id/tvMessage' and contains(@text,"
                        + lit(part) + ")]"));
                if (l.isEmpty()) return false;
                var r = l.get(0).getRect();
                int y = r.getY() + r.getHeight() - 18;
                int x = r.getX() + 40 + (r.getWidth() - 80) * (7 - i) / 7;
                tap(x, y);
                sleep(900);
                if (done.test(rowText(part))) return true;
            } catch (WebDriverException ignored) { }
        }
        return false;
    }

    /** Taps the image of the card whose caption contains {@code caption}; true once CustomImageViewer is up. */
    public boolean openImageWithCaption(String caption) {
        String pkg = appPackage();
        try {
            if (arrowDownShown()) tapArrowDown();
            var imgs = driver.findElements(AppiumBy.xpath("//*[@resource-id='" + pkg + ":id/materialCardView'][.//*[contains(@text,"
                    + lit(caption) + ")]]//*[@resource-id='" + pkg + ":id/imageParent']"));
            if (imgs.isEmpty()) return false;
            var r = imgs.get(0).getRect();
            tap(r.getX() + r.getWidth() / 2, r.getY() + r.getHeight() / 3);
            long deadline = System.currentTimeMillis() + 10000;
            while (System.currentTimeMillis() < deadline) {
                String a = driver.currentActivity();
                if (a != null && a.contains("CustomImageViewer")) return true;
                sleep(500);
            }
        } catch (WebDriverException ignored) { }
        return false;
    }

    /** Scrolls the list a little so the bottom of the row's text is on screen. */
    public void bringRowEndOnScreen(String part) {
        String pkg = appPackage();
        try {
            var size = driver.manage().window().getSize();
            for (int i = 0; i < 4; i++) {
                var l = driver.findElements(AppiumBy.xpath("//*[@resource-id='" + pkg + ":id/tvMessage' and contains(@text," + lit(part) + ")]"));
                if (l.isEmpty()) return;
                var r = l.get(0).getRect();
                if (r.getY() + r.getHeight() < size.getHeight() * 0.78) return;
                int x = size.getWidth() / 2;
                var finger = new org.openqa.selenium.interactions.PointerInput(org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
                var seq = new org.openqa.selenium.interactions.Sequence(finger, 1);
                seq.addAction(finger.createPointerMove(Duration.ZERO, org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, (int) (size.getHeight() * 0.7)));
                seq.addAction(finger.createPointerDown(0));
                seq.addAction(finger.createPointerMove(Duration.ofMillis(400), org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, (int) (size.getHeight() * 0.45)));
                seq.addAction(finger.createPointerUp(0));
                driver.perform(java.util.Collections.singletonList(seq));
                sleep(800);
            }
        } catch (WebDriverException ignored) { }
    }

    // ---------------- chat library (Gallery) ----------------

    /** In the chat library (HomeChatLibraryActivity): taps the tab whose label starts with {@code tab}. */
    public boolean openLibraryTab(String tab) {
        try {
            for (var e : driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().textStartsWith(\"" + tab + "\")"))) {
                e.click();
                sleep(1500);
                return true;
            }
        } catch (WebDriverException ignored) { }
        return false;
    }

    /** Library rows as "title | subtitle". */
    public java.util.List<String[]> libraryRows() {
        java.util.List<String[]> out = new java.util.ArrayList<>();
        try {
            for (var row : driver.findElements(byId("rootRow"))) {
                String t = firstText(row, ".//*[contains(@resource-id,':id/tvTitle')]");
                String s = firstText(row, ".//*[contains(@resource-id,':id/tvSubtitle')]");
                if (!t.isBlank()) out.add(new String[]{t, s});
            }
        } catch (WebDriverException ignored) { }
        return out;
    }

    // ---------------- Catering Settings search ----------------

    public void searchSettingsUnits(String q) {
        try {
            var box = driver.findElement(byId("searchIncluded")).findElement(byId("searchWidgetEditTextvw"));
            box.click();
            box.clear();
            if (!q.isEmpty()) box.sendKeys(q);
            sleep(1200);
        } catch (WebDriverException ignored) { }
    }

    /** Unit names rendered right now in Catering Settings (no scrolling). */
    public java.util.List<String> settingsUnitNamesOnScreen() {
        java.util.List<String> out = new java.util.ArrayList<>();
        try {
            for (var e : driver.findElements(byId("labelTextView"))) {
                String t = e.getText();
                if (t != null && !t.isBlank()) out.add(t.trim());
            }
        } catch (WebDriverException ignored) { }
        return out;
    }

    /** Delete on a Catering Settings row WITHOUT confirming — for a system unit that must refuse. */
    public boolean tapDeleteOnSettingsRow(String name) {
        return tapSettingsRowButton(name, "deleteImage");
    }

    private String firstText(org.openqa.selenium.WebElement parent, String xpath) {
        try {
            for (var e : parent.findElements(AppiumBy.xpath(xpath))) {
                String t = e.getText();
                if (t != null && !t.isBlank()) return t.trim();
            }
        } catch (WebDriverException ignored) { }
        return "";
    }

    private static String esc(String s) { return s.replace("\"", "\\\""); }

    private static String lit(String s) {
        if (!s.contains("'")) return "'" + s + "'";
        return "concat('" + s.replace("'", "',\"'\",'") + "')";
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
