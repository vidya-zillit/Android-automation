package com.zillit.automation.tests.tools;

import com.zillit.automation.core.TestOrder;

import com.zillit.automation.pages.tools.CateringPage;
import com.zillit.automation.pages.homechat.HomeChatPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tools → CATERING. Same Bulletin-style chat as Confidential Info / Info (rvChat
 * + etMessage / ivAttachment / ivRecord + the long-press option set), so it
 * inherits the whole shared case set from {@link ToolChatBaseTest}.
 *
 * Catering additionally has a MEAL-TAB strip (Breakfast / Lunch / Dinner) built
 * on the same recTab → menuText chips as the Home units strip, and each meal is
 * its OWN conversation. The cases below cover the tabs: they render, each one
 * opens a usable chat, posting works in every meal, and — most importantly —
 * a message posted in one meal does NOT leak into another.
 */
@Order(TestOrder.TOOL_CATERING)
@org.junit.jupiter.api.extension.ExtendWith(com.zillit.automation.core.ReasonLogger.class)
class CateringTest extends ToolChatBaseTest {

    private static final String[] MEALS = {"Breakfast", "Lunch", "Dinner"};

    @Override protected String moduleName() { return "Catering"; }

    /**
     * CateringActivity.onAttachmentClick (the composer's paperclip) opens openCommonMediaPicker
     * with exactly these five pickers: PICKER_CAMERA, PICKER_IMAGE_GALLERY, PICKER_ITEM_PICK_AUDIO,
     * PICKER_ITEM_DOCUMENT and PICKER_MULTTPLE_IMAGE_CAPTURE ("Multiple\nCapture", Android-only).
     */
    private static final List<String> CATERING_PICKERS =
            List.of("Photo", "Image Gallery", "Audio", "Document", "Multiple Capture");

    /**
     * Catering's attachment picker is RESTRICTED compared with the other chat modules:
     * every branch of CateringActivity passes exactly {PICKER_CAMERA, PICKER_IMAGE_GALLERY,
     * PICKER_ITEM_PICK_AUDIO, PICKER_ITEM_DOCUMENT}, labelled Photo / Image Gallery / Audio /
     * Document (mediaHandler/ItemAdapter). A size check would still pass if Video or
     * Location crept in, so the exact set is asserted.
     */
    @Test
    @Order(6)
    @Override
    @DisplayName("C2 [WEB CAT-59] Attachment picker offers exactly Photo / Image Gallery / Audio / Document / Multiple Capture")
    void attachmentPickerHasMediaTypes() {
        HomeChatPage page = chatPage();
        assumeTrue(page.isInputBarVisible(), "No posting rights — skipping");
        assumeTrue(page.isAttachmentVisible(), "No attachment button — skipping");
        page.tapAttachment();
        // With no caterer on the project the paperclip shows a snackbar instead of a picker.
        assumeTrue(page.isAttachmentPickerVisible(),
                "Attachment picker did not open (no caterer on the project, or no recipient chosen)");
        List<String> opts = page.attachmentOptions();
        System.out.println(">>> Catering attachment options: " + opts);
        page.dismissAttachmentPicker();
        java.util.Set<String> got = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        opts.forEach(o -> got.add(o.replaceAll("\\s+", " ").trim()));
        java.util.Set<String> want = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        want.addAll(CATERING_PICKERS);
        assertTrue(got.equals(want) && opts.size() == CATERING_PICKERS.size(),
                "Catering's picker must be exactly " + CATERING_PICKERS
                        + " (no Video, Video Gallery, Location or Contact). Got: " + opts);
    }

    /**
     * Catering never offers Read By: CateringActivity passes {@code showReadBy = false} for
     * messages and for comments. The inherited case expects the read-by screen and, through
     * an assumption, always skipped here — this checks the real rule instead.
     */
    @Test
    @Order(12)
    @Override
    @DisplayName("READ BY USER is NOT offered on a Catering message")
    void readByUser() {
        HomeChatPage page = chatPage();
        assumeTrue(page.isInputBarVisible(), "No posting rights — skipping");
        String marker = "qa-catering-rbu-" + (System.currentTimeMillis() % 1000000);
        page.sendMessage(marker);
        assumeTrue(page.waitMessageSent(marker, Duration.ofSeconds(30)),
                "Marker did not SEND — it is " + page.sendStateOf(marker));
        assumeTrue(page.longPressMessage(marker), "Long-press did not open the menu");
        List<String> opts = page.menuOptions();
        System.out.println(">>> Catering options on a sent message: " + opts);
        page.closeMenu();
        screenshot("catering-no-readby");
        assumeTrue(!opts.isEmpty(), "The options menu rendered no labels — nothing to inspect");
        assertTrue(opts.stream().noneMatch(o -> o.toLowerCase().contains("read by")),
                "Catering must not offer Read By (showReadBy = false). Options were: " + opts);
    }

    /**
     * A CATERER has no keyboard until a recipient is chosen, so make the chat
     * postable before every case — otherwise all the inherited chat cases would
     * skip when this account is in the catering department. A regular user is
     * unaffected (their input bar is always there).
     */
    @Override protected void afterModuleReady() {
        try { new CateringPage(driver).ensurePostable(); } catch (Exception ignored) { }
    }

    /**
     * Before the shared module entry: a picker or media preview left open by a failed upload
     * (another app in front, or the app's preview screen) would otherwise strand every later case.
     */
    @Override
    @org.junit.jupiter.api.BeforeEach
    void enterModule() {
        try { if (driver != null) new CateringPage(driver).backToChat(); } catch (Exception ignored) { }
        // BaseTest settles `landing` once at session start; a project list that flashed during the
        // splash leaves it at PROJECT_LIST even though the app is inside the project, and the shared
        // entry then looks for projects to tap ("No projects to enter") on every case.
        try {
            if (landing == Landing.PROJECT_LIST && !driver.findElements(byAppId("bottomNavigation")).isEmpty())
                landing = Landing.BOTTOM_NAV;
        } catch (Exception ignored) { }
        super.enterModule();
        // Other agents post live in shared chats, and the Tools list sorts badged tiles first, so
        // the shared open can land on a neighbouring tool (seen: Confidential Info). Verify by the
        // Catering meal strip; if wrong, reopen Catering by its tile TEXT.
        CateringPage cat = new CateringPage(driver);
        if (!cat.isCateringScreen(Duration.ofSeconds(3))) {
            System.out.println(">>> not on Catering after the module entry — reopening Catering by its tile text");
            assumeTrue(cat.openCateringFromTools(), "Could not open Catering from the Tools tab (by its tile text)");
            afterModuleReady();
        }
    }

    private CateringPage catering() { return new CateringPage(driver); }

    private HomeChatPage chatPage() {
        return new HomeChatPage(driver);
    }

    // ---------------- meal tabs ----------------

    @Test
    @Order(20)
    @DisplayName("Catering shows the meal tabs: Breakfast / Lunch / Dinner")
    void mealTabsVisible() {
        HomeChatPage page = chatPage();
        List<String> tabs = page.visibleUnitNames();
        System.out.println(">>> Catering meal tabs: " + tabs);
        assertTrue(!tabs.isEmpty(), "Catering should show its meal tabs");
        for (String meal : MEALS) {
            assertTrue(tabs.stream().anyMatch(t -> t.equalsIgnoreCase(meal)),
                    "Meal tab '" + meal + "' should be listed. Got: " + tabs);
        }
        // [WEB CAT-131/167] labels go through getDataFromLabelKey: no raw "_label" key may leak.
        assertTrue(tabs.stream().noneMatch(t -> t.matches("[a-z0-9]+(_[a-z0-9]+)+")),
                "A meal tab shows a raw label key instead of its name: " + tabs);
        screenshot("catering-tabs");
    }

    @Test
    @Order(21)
    @DisplayName("Each meal tab opens its own chat (input bar or no-rights banner)")
    void eachMealTabOpens() {
        HomeChatPage page = chatPage();
        for (String meal : MEALS) {
            assumeTrue(page.selectUnit(meal), "Could not select the '" + meal + "' tab");
            sleep(800);
            boolean usable = page.isInputBarVisible() || page.isNoRightsBannerVisible();
            System.out.println(">>> " + meal + " tab → chat usable=" + usable);
            assertTrue(usable, "The '" + meal + "' tab should show a chat (input bar or no-rights banner)");
        }
        screenshot("catering-tab-switch");
        page.selectUnit("Breakfast");   // leave on the first meal
    }

    @Test
    @Order(22)
    @DisplayName("Send a message in EVERY meal tab — each one posts")
    void sendInEveryMeal() {
        HomeChatPage page = chatPage();
        for (String meal : MEALS) {
            assumeTrue(page.selectUnit(meal), "Could not select '" + meal + "'");
            sleep(800);
            if (!page.isInputBarVisible()) {
                System.out.println(">>> " + meal + ": no posting rights — skipping this meal");
                continue;
            }
            String marker = "qa-catering-" + meal.toLowerCase() + "-" + (System.currentTimeMillis() % 1000000);
            page.sendMessage(marker);
            boolean posted = page.waitMessageSent(marker, Duration.ofSeconds(30));
            System.out.println(">>> " + meal + " posted '" + marker + "'=" + posted);
            assertTrue(posted, "The message should post in the '" + meal + "' tab");
        }
        screenshot("catering-sent-all-meals");
        page.selectUnit("Breakfast");
    }

    @Test
    @Order(23)
    @DisplayName("Meals are separate conversations — a Breakfast message does NOT appear in Lunch")
    void mealConversationsAreIsolated() {
        HomeChatPage page = chatPage();
        assumeTrue(page.selectUnit("Breakfast"), "Could not select Breakfast");
        sleep(800);
        assumeTrue(page.isInputBarVisible(), "No posting rights on Breakfast — skipping");
        String marker = "qa-catering-iso-" + (System.currentTimeMillis() % 1000000);
        page.sendMessage(marker);
        assumeTrue(page.waitMessageSent(marker, Duration.ofSeconds(30)),
                "The Breakfast marker did not SEND — it is " + page.sendStateOf(marker));

        // Switch to Lunch: the Breakfast-only message must not be there.
        assumeTrue(page.selectUnit("Lunch"), "Could not select Lunch");
        sleep(1500);
        boolean leaked = page.messageShown(marker, Duration.ofSeconds(4));
        System.out.println(">>> Breakfast marker visible in Lunch=" + leaked + " (expected false)");
        assertTrue(!leaked, "A Breakfast message must NOT appear in the Lunch conversation");
        screenshot("catering-meal-isolation");
    }

    @Test
    @Order(24)
    @DisplayName("A posted Catering message SURVIVES a meal-tab switch (persistence)")
    void messagePersistsAcrossTabSwitch() {
        HomeChatPage page = chatPage();
        assumeTrue(page.selectUnit("Breakfast"), "Could not select Breakfast");
        sleep(800);
        assumeTrue(page.isInputBarVisible(), "No posting rights on Breakfast — skipping");
        String marker = "qa-catering-persist-" + (System.currentTimeMillis() % 1000000);
        page.sendMessage(marker);
        assumeTrue(page.waitMessageSent(marker, Duration.ofSeconds(30)),
                "The marker never SENT — it is " + page.sendStateOf(marker));
        System.out.println(">>> posted in Breakfast: " + marker);

        // Leave to another meal and come back. NOTE: the conversation reloads at a
        // scroll position that may not render our row, so search WITH scrolling —
        // a plain viewport lookup falsely reads as "the message vanished".
        assumeTrue(page.selectUnit("Lunch"), "Could not select Lunch");
        sleep(1500);
        assumeTrue(page.selectUnit("Breakfast"), "Could not return to Breakfast");
        sleep(2500);
        boolean stillThere = page.messageShownScrolled(marker, 6);
        System.out.println(">>> after Lunch→Breakfast, marker still present=" + stillThere);
        screenshot("catering-persistence");
        assertTrue(stillThere,
                "A message posted in a Catering meal must still be in that meal after switching tabs and back");
    }

    // ---------------- Catering business rules (production/user side) ----------------
    // 1. No caterer on the project  → nobody can post.
    // 2. A USER's message goes ONLY to the caterer(s) — the UI labels it "To : All Caterer".
    // 3. A user CANNOT reply to a CATERER's message.
    // 4. A caterer CAN reply to any user's message.        (caterer-side — needs the caterer account)
    // 5. A caterer's message is visible to ALL users and ALL caterers.  (caterer-side)

    @Test
    @Order(30)
    @DisplayName("RULE 2 — a user's message is addressed to the caterer (\"To : All Caterer\")")
    void userMessageIsAddressedToCaterer() {
        HomeChatPage page = chatPage();
        assumeTrue(!catering().isCatererMode(),
                "Signed in as a CATERER — this is a regular-user rule (a caterer's post is "
                        + "labelled \"To : All Users\"). Skipping.");
        assumeTrue(page.selectUnit("Breakfast"), "Could not select Breakfast");
        sleep(800);
        assumeTrue(page.isInputBarVisible(), "No posting rights — skipping");
        String marker = "qa-cater-to-" + (System.currentTimeMillis() % 1000000);
        page.sendMessage(marker);
        assumeTrue(page.waitMessageSent(marker, Duration.ofSeconds(30)),
                "Marker did not SEND — it is " + page.sendStateOf(marker));
        // The bubble carries the audience label — proof the post is caterer-directed.
        boolean toCaterer = page.textShown("To : All Caterer", Duration.ofSeconds(6))
                || page.textShown("All Caterer", Duration.ofSeconds(3));
        System.out.println(">>> user message audience label shown=" + toCaterer);
        assertTrue(toCaterer,
                "A message posted by a non-caterer should be labelled as going to the caterer "
                        + "(\"To : All Caterer\") — it is not broadcast to other users");
        screenshot("catering-rule2-to-caterer");
    }

    @Test
    @Order(31)
    @DisplayName("RULE 1 — posting is only possible while a caterer exists (input bar state)")
    void postingRequiresACaterer() {
        HomeChatPage page = chatPage();
        assumeTrue(page.selectUnit("Breakfast"), "Could not select Breakfast");
        sleep(800);
        boolean canPost = page.isInputBarVisible();
        boolean blocked = page.isNoRightsBannerVisible();
        System.out.println(">>> Catering posting: inputBar=" + canPost + " blockedBanner=" + blocked);
        // On this project a caterer exists, so posting is enabled. When no caterer is
        // assigned the module must block posting instead of silently accepting a
        // message that can never be delivered.
        assertTrue(canPost || blocked,
                "Catering must either allow posting (a caterer exists) or clearly block it (no caterer)");
        screenshot("catering-rule1-posting-state");
    }

    @Test
    @Order(32)
    @DisplayName("RULE 3 — Reply is NOT offered on a CATERER-authored message")
    void userCannotReplyToCatererMessage() {
        HomeChatPage page = chatPage();
        assumeTrue(page.selectUnit("Breakfast"), "Could not select Breakfast");
        sleep(1200);
        // A CATERER's post renders "To : All Users"; a regular user's renders
        // "To : All Caterer". Only the former proves a caterer-authored message —
        // matching on the bare word "Caterer" would falsely match our OWN messages.
        boolean catererAuthored = page.textShown("To : All Users", Duration.ofSeconds(5));
        assumeTrue(catererAuthored,
                "No caterer-authored message (\"To : All Users\") in this conversation yet — "
                        + "rule 3 needs the caterer side to post first");
        assumeTrue(page.longPressMessage("To : All Users"), "Could not long-press the caterer message");
        List<String> opts = page.menuOptions();
        boolean replyOffered = opts.stream().anyMatch(o -> o.equalsIgnoreCase("Reply"));
        System.out.println(">>> caterer-message options: " + opts + " | Reply offered=" + replyOffered);
        page.closeMenu();
        screenshot("catering-rule3-no-reply-on-caterer-msg");
        assertTrue(!replyOffered,
                "Reply must NOT be offered on a caterer-authored message. Options were: " + opts);
    }

    @Test
    @Order(33)
    @DisplayName("RULE 3b — Reply IS offered on a regular user's own message")
    void replyAllowedOnUserMessage() {
        HomeChatPage page = chatPage();
        assumeTrue(!catering().isCatererMode(),
                "Signed in as a CATERER — my own post is caterer-authored, so Reply is correctly "
                        + "hidden (rule 3). This case only applies to a regular user. Skipping.");
        assumeTrue(page.selectUnit("Breakfast"), "Could not select Breakfast");
        sleep(800);
        assumeTrue(page.isInputBarVisible(), "No posting rights — skipping");
        String marker = "qa-cater-rep3b-" + (System.currentTimeMillis() % 1000000);
        page.sendMessage(marker);
        assumeTrue(page.waitMessageSent(marker, Duration.ofSeconds(30)),
                "Marker did not SEND — it is " + page.sendStateOf(marker));
        assumeTrue(page.longPressMessage(marker), "Long-press failed");
        List<String> opts = page.menuOptions();
        boolean replyOffered = opts.stream().anyMatch(o -> o.equalsIgnoreCase("Reply"));
        System.out.println(">>> user-message options: " + opts + " | Reply offered=" + replyOffered);
        page.closeMenu();
        assertTrue(replyOffered,
                "Reply SHOULD be offered on a non-caterer message. Options were: " + opts);
        screenshot("catering-rule3b-reply-on-user-msg");
    }

    // ---------------- CATERER-side rules (account must be in the catering dept) ----------------
    // These all skip cleanly when the signed-in account is a regular user.

    @Test
    @Order(40)
    @DisplayName("CATERER — sees the recipient picker + create-unit, and has NO keyboard until a recipient is chosen")
    void catererRecipientGating() {
        CateringPage cat = catering();
        assumeTrue(cat.isCatererMode(), "Account is not a caterer — skipping the caterer-side rules");
        HomeChatPage page = chatPage();
        System.out.println(">>> caterer UI: picker=" + cat.isCatererMode()
                + " hintBar=" + cat.isRecipientHintShown()
                + " createUnit=" + cat.isCreateUnitVisible()
                + " recipient='" + cat.selectedRecipient() + "'"
                + " inputBar=" + page.isInputBarVisible());
        assertTrue(cat.isCreateUnitVisible(),
                "A caterer should see the create-meal-unit (+) control");
        // Either a recipient is already chosen (keyboard up) or the hint bar is shown
        // with the keyboard hidden — never a keyboard with no recipient.
        String rec = cat.selectedRecipient();
        boolean hasRecipient = !rec.isBlank() && !rec.equalsIgnoreCase("Select User");
        assertTrue(hasRecipient || !page.isInputBarVisible(),
                "A caterer must not be able to type before choosing a recipient");
        if (!hasRecipient) assertTrue(cat.isRecipientHintShown(),
                "With no recipient chosen the caterer instructions bar (layNoMsg) should be shown");
        screenshot("catering-caterer-gating");
    }

    @Test
    @Order(41)
    @DisplayName("CATERER — recipient dialog offers \"All Users\" and \"Select User\"")
    void catererRecipientDialogOptions() {
        CateringPage cat = catering();
        assumeTrue(cat.isCatererMode(), "Not a caterer — skipping");
        assumeTrue(cat.openRecipientPicker(), "Could not open the recipient picker");
        boolean open = cat.isRecipientDialogOpen(Duration.ofSeconds(8));
        System.out.println(">>> recipient dialog open=" + open);
        assertTrue(open, "The recipient dialog should offer both \"All Users\" and \"Select User\"");
        screenshot("catering-recipient-dialog");
        cat.chooseAllUsers();   // leave in a postable state
    }

    @Test
    @Order(42)
    @DisplayName("RULE 5 — a CATERER's broadcast is addressed to everyone (\"To : All Users\")")
    void catererBroadcastGoesToAllUsers() {
        CateringPage cat = catering();
        assumeTrue(cat.isCatererMode(), "Not a caterer — skipping");
        HomeChatPage page = chatPage();
        assumeTrue(page.selectUnit("Breakfast"), "Could not select Breakfast");
        sleep(800);
        // Explicitly choose the broadcast audience.
        if (!"All Users".equalsIgnoreCase(cat.selectedRecipient())) {
            assumeTrue(cat.openRecipientPicker() && cat.isRecipientDialogOpen(Duration.ofSeconds(6)),
                    "Recipient dialog did not open");
            assumeTrue(cat.chooseAllUsers(), "Could not choose All Users");
        }
        assumeTrue(page.isInputBarVisible(), "Keyboard did not enable after choosing All Users");
        String marker = "qa-cater-bcast-" + (System.currentTimeMillis() % 1000000);
        page.sendMessage(marker);
        assumeTrue(page.waitMessageSent(marker, Duration.ofSeconds(30)),
                "Broadcast did not SEND — it is " + page.sendStateOf(marker));
        boolean toAll = page.textShown("To : All Users", Duration.ofSeconds(6));
        System.out.println(">>> caterer broadcast '" + marker + "' labelled To:All Users=" + toAll);
        assertTrue(toAll,
                "A caterer's broadcast should be labelled \"To : All Users\" (visible to all users + caterers)");
        screenshot("catering-rule5-broadcast");
    }

    @Test
    @Order(43)
    @DisplayName("CATERER — \"Select User\" sends a DIRECTED message (To : <user>)")
    void catererDirectedMessage() {
        CateringPage cat = catering();
        assumeTrue(cat.isCatererMode(), "Not a caterer — skipping");
        HomeChatPage page = chatPage();
        assumeTrue(cat.openRecipientPicker() && cat.isRecipientDialogOpen(Duration.ofSeconds(6)),
                "Recipient dialog did not open");
        assumeTrue(cat.chooseSelectUser(), "Could not choose Select User");
        String picked = cat.pickFirstUserFromList();
        assumeTrue(!picked.isBlank(), "No user available to direct a message to — skipping");
        System.out.println(">>> directing a message to: " + picked + " | label='" + cat.selectedRecipient() + "'");
        assumeTrue(page.isInputBarVisible(), "Keyboard did not enable after picking a user");
        String marker = "qa-cater-direct-" + (System.currentTimeMillis() % 1000000);
        page.sendMessage(marker);
        assumeTrue(page.waitMessageSent(marker, Duration.ofSeconds(30)),
                "Directed message did not SEND — it is " + page.sendStateOf(marker));
        String label = cat.visibleRows().stream().filter(r -> r.text.contains(marker))
                .map(r -> r.audience).findFirst().orElse("");
        System.out.println(">>> directed row label: '" + label + "'");
        boolean directed = label.contains(picked) && !label.contains("All Users") && !label.contains("All Caterer");
        System.out.println(">>> directed message posted; audience label present=" + directed);
        assertTrue(directed, "A directed caterer message should be labelled 'To : " + picked + "', not '" + label + "'");
        screenshot("catering-directed-message");
        // restore broadcast mode for later cases
        if (cat.openRecipientPicker() && cat.isRecipientDialogOpen(Duration.ofSeconds(6))) cat.chooseAllUsers();
    }

    @Test
    @Order(44)
    @DisplayName("RULE 3 (caterer view) — Reply is NOT offered on a caterer-authored message")
    void replyHiddenOnCatererAuthoredMessage() {
        CateringPage cat = catering();
        assumeTrue(cat.isCatererMode(), "Not a caterer — skipping");
        HomeChatPage page = chatPage();
        assumeTrue(page.selectUnit("Breakfast"), "Could not select Breakfast");
        sleep(1200);
        assumeTrue(page.textShown("To : All Users", Duration.ofSeconds(6)),
                "No caterer-authored message present — skipping");
        assumeTrue(page.longPressMessage("To : All Users"), "Could not long-press a caterer message");
        List<String> opts = page.menuOptions();
        boolean replyOffered = opts.stream().anyMatch(o -> o.equalsIgnoreCase("Reply"));
        System.out.println(">>> caterer-authored message options: " + opts + " | Reply=" + replyOffered);
        page.closeMenu();
        screenshot("catering-rule3-caterer-view");
        assertTrue(!replyOffered,
                "Reply must NOT be offered on a caterer-authored message. Options: " + opts);
    }

    @Test
    @Order(45)
    @DisplayName("RULE 4 — a CATERER CAN reply to a regular user's message")
    void catererCanReplyToUserMessage() {
        CateringPage cat = catering();
        assumeTrue(cat.isCatererMode(), "Not a caterer — skipping");
        HomeChatPage page = chatPage();
        assumeTrue(page.selectUnit("Breakfast"), "Could not select Breakfast");
        sleep(1200);
        // A regular user's post renders "To : All Caterer" — that's the one a caterer
        // is allowed to reply to. Needs a message from a NON-caterer account.
        assumeTrue(page.messageShownScrolled("To : All Caterer", 6),
                "No regular-user message (\"To : All Caterer\") in this conversation — "
                        + "rule 4 needs a post from a non-caterer account");
        assumeTrue(page.longPressMessage("To : All Caterer"), "Could not long-press the user message");
        List<String> opts = page.menuOptions();
        boolean replyOffered = opts.stream().anyMatch(o -> o.equalsIgnoreCase("Reply"));
        System.out.println(">>> user-authored message (caterer view) options: " + opts
                + " | Reply=" + replyOffered);
        page.closeMenu();
        screenshot("catering-rule4-caterer-reply-to-user");
        assertTrue(replyOffered,
                "A caterer SHOULD be able to Reply to a regular user's message. Options: " + opts);
    }

    // ---------------- create a meal UNIT (caterer only) ----------------

    @Test
    @Order(46)
    @DisplayName("CATERER — the \"+\" opens the ADD CATERING UNIT screen (name + Submit)")
    void createUnitScreenOpens() {
        CateringPage cat = catering();
        assumeTrue(cat.isCatererMode(), "Not a caterer — skipping");
        assumeTrue(cat.isCreateUnitVisible(), "No create-unit (+) control");
        assumeTrue(cat.openCreateUnit(), "Could not tap the + control");
        boolean open = cat.isCreateUnitScreenOpen(Duration.ofSeconds(10));
        System.out.println(">>> ADD CATERING UNIT screen open=" + open);
        assertTrue(open, "The + should open the ADD CATERING UNIT screen");
        screenshot("catering-create-unit-screen");
        cat.cancelCreateUnit();
    }

    @Test
    @Order(47)
    @DisplayName("CATERER — creating a unit with an EMPTY name is rejected")
    void createUnitEmptyNameRejected() {
        CateringPage cat = catering();
        assumeTrue(cat.isCatererMode(), "Not a caterer — skipping");
        assumeTrue(cat.openCreateUnit() && cat.isCreateUnitScreenOpen(Duration.ofSeconds(10)),
                "Create-unit screen did not open");
        cat.setUnitName("");
        cat.submitUnit();
        // A blank name must NOT create a unit — we should still be on the form.
        boolean stillOnForm = cat.isCreateUnitScreenOpen(Duration.ofSeconds(4));
        System.out.println(">>> after submitting a blank unit name, still on form=" + stillOnForm);
        assertTrue(stillOnForm, "An empty unit name should be rejected, not accepted");
        screenshot("catering-create-unit-empty");
        cat.cancelCreateUnit();
    }

    @Test
    @Order(48)
    @DisplayName("CATERER — create a NEW meal unit → it appears in the tab strip")
    void createNewMealUnit() {
        CateringPage cat = catering();
        HomeChatPage page = chatPage();
        assumeTrue(cat.isCatererMode(), "Not a caterer — skipping");
        List<String> before = page.visibleUnitNames();
        // A custom unit CAN be deleted (Settings → Catering Settings, see cases 52/53),
        // so every run creates its own uniquely named unit and removes it again.
        String name = QA_UNIT_PREFIX + (System.currentTimeMillis() % 100000);
        assumeTrue(cat.openCreateUnit() && cat.isCreateUnitScreenOpen(Duration.ofSeconds(10)),
                "Create-unit screen did not open");
        cat.setUnitName(name);
        assumeTrue(cat.submitUnit(), "Submit not tappable");
        sleep(3000);
        List<String> after = page.visibleUnitNames();
        System.out.println(">>> units " + before + " -> " + after + " (new: '" + name + "')");
        boolean created = after.stream().anyMatch(u -> u.equalsIgnoreCase(name))
                || page.textShown(name, Duration.ofSeconds(8));
        assertTrue(created, "The new unit '" + name + "' should appear in the meal tab strip");
        createdUnit = after.stream().filter(u -> u.equalsIgnoreCase(name)).findFirst().orElse(name);
        screenshot("catering-unit-created");
    }

    /** Unique per run; cases 52/53 rename and delete it again. */
    private static final String QA_UNIT_PREFIX = "QA Snack ";

    /** Set by {@link #createNewMealUnit()} (and updated by the rename) for the later cases. */
    private static String createdUnit;

    @Test
    @Order(49)
    @DisplayName("CATERER — send a message in the NEWLY created unit → it posts")
    void sendMessageInCreatedUnit() {
        CateringPage cat = catering();
        HomeChatPage page = chatPage();
        assumeTrue(cat.isCatererMode(), "Not a caterer — skipping");
        assumeTrue(createdUnit != null, "No unit was created by the previous case — skipping");
        assumeTrue(page.selectUnit(createdUnit), "Could not select the new unit '" + createdUnit + "'");
        sleep(1200);
        // A caterer still has to choose a recipient in the new unit before typing.
        assumeTrue(cat.ensurePostable(), "Could not enable posting in the new unit");
        String marker = "qa-newunit-" + (System.currentTimeMillis() % 1000000);
        page.sendMessage(marker);
        boolean posted = page.waitMessageSent(marker, Duration.ofSeconds(30));
        System.out.println(">>> posted in new unit '" + createdUnit + "': " + marker + " = " + posted);
        assertTrue(posted, "A message should post in the newly created unit '" + createdUnit + "'");
        // and it must survive a reload (switch away and back)
        page.selectUnit("Breakfast");
        sleep(1500);
        page.selectUnit(createdUnit);
        sleep(2000);
        boolean persisted = page.messageShownScrolled(marker, 5);
        System.out.println(">>> new-unit message persisted after reload=" + persisted);
        assertTrue(persisted, "The message in the new unit should persist after switching tabs");
        screenshot("catering-new-unit-message");
    }

    @Test
    @Order(50)
    @DisplayName("RULE 1 — with NO caterer on the project, a message must NOT post")
    void noCatererBlocksPosting() {
        HomeChatPage page = chatPage();
        assumeTrue(!catering().isCatererMode(), "Signed in as a caterer — skipping");
        assumeTrue(page.selectUnit("Breakfast"), "Could not select Breakfast");
        sleep(1000);
        assumeTrue(page.isInputBarVisible(), "No input bar at all — nothing to attempt");
        String marker = "qa-cater-nocat-" + (System.currentTimeMillis() % 1000000);
        page.sendMessage(marker);
        sleep(1500);
        // The app's no-caterer guard is a snackbar: "You cannot post any message
        // until a person joins the catering department".
        boolean warned = page.textShown("cannot post", Duration.ofSeconds(4))
                || page.textShown("catering department", Duration.ofSeconds(2));
        boolean echoedLocally = page.messageShownScrolled(marker, 4);
        assumeTrue(warned,
                "No no-caterer warning appeared — a caterer probably exists on this project. "
                        + "Rule 1 can only be checked with NO caterer. Skipping.");
        // The message may ECHO locally (optimistic UI) — the real check is whether it
        // SURVIVES a reload. Switching meal tabs re-loads the conversation.
        page.selectUnit("Lunch");
        sleep(1500);
        page.selectUnit("Breakfast");
        sleep(2500);
        boolean persisted = page.messageShownScrolled(marker, 6);
        System.out.println(">>> NO-CATERER send: marker=" + marker + " warningShown=" + warned
                + " echoedLocally=" + echoedLocally + " persistedAfterReload=" + persisted);
        if (echoedLocally && !persisted)
            System.out.println("!!! UX ISSUE: the message was BLOCKED (correctly not saved) but still "
                    + "rendered in the chat as if sent, then silently vanished on reload. The input bar "
                    + "also stays enabled with no caterer — only a snackbar warns.");
        screenshot("catering-rule1-no-caterer");
        assertTrue(!persisted,
                "RULE 1 VIOLATION: with no caterer on the project the message '" + marker
                        + "' was saved. Posting must be blocked until someone joins the catering department.");
    }

    // ---------------- Settings → Catering Settings: rename and delete (caterer only) ----------------
    // AccAndCaterSettingsActivity lists the catering units with edit and delete. Delete asks
    // "Are you sure you want to delete this unit?" and calls USER_UNIT_CATERING_DELETE; Edit
    // opens CreateNewUnitPage as "Edit Catering Unit". System-defined units refuse deletion.

    @Test
    @Order(52)
    @DisplayName("CATERER — the new unit can be RENAMED from Settings → Catering Settings")
    void renameUnitInCateringSettings() {
        CateringPage cat = catering();
        assumeTrue(cat.isCatererMode(), "Not a caterer — skipping");
        assumeTrue(createdUnit != null, "Case 48 created no unit this run — nothing to rename");
        assumeTrue(cat.openCateringSettings(), "Could not open Settings → Catering Settings");
        assertTrue(cat.settingsUnitListed(createdUnit),
                "The unit '" + createdUnit + "' created from the + should be listed in Catering Settings. "
                        + "Listed: " + cat.settingsUnitNames());
        assertTrue(cat.editUnitInSettings(createdUnit),
                "Catering Settings should offer Edit on the custom unit '" + createdUnit + "'");
        assumeTrue(cat.isCreateUnitScreenOpen(Duration.ofSeconds(10)), "Edit Catering Unit screen did not open");
        String renamed = createdUnit + " R";
        cat.setUnitName(renamed);
        assumeTrue(cat.submitUnit(), "Submit not tappable");
        sleep(3000);
        List<String> listed = cat.settingsUnitNames();
        System.out.println(">>> Catering Settings after rename: " + listed);
        boolean renamedShown = listed.stream().anyMatch(n -> n.equalsIgnoreCase(renamed));
        if (renamedShown) createdUnit = renamed;   // case 53 deletes whatever name it now has
        screenshot("catering-unit-renamed");
        assertTrue(renamedShown,
                "After Edit → Submit the unit should be listed as '" + renamed + "'. Listed: " + listed);
    }

    @Test
    @Order(53)
    @DisplayName("CATERER — the new unit can be DELETED from Settings → Catering Settings")
    void deleteUnitInCateringSettings() {
        CateringPage cat = catering();
        assumeTrue(createdUnit != null, "Case 48 created no unit this run — nothing to delete");
        if (!cat.isCateringSettingsOpen(Duration.ofSeconds(2))) {
            assumeTrue(cat.isCatererMode(), "Not a caterer — skipping");
            assumeTrue(cat.openCateringSettings(), "Could not open Settings → Catering Settings");
        }
        String unit = createdUnit;
        assumeTrue(cat.settingsUnitListed(unit),
                "The unit '" + unit + "' is not listed in Catering Settings — nothing to delete");
        assertTrue(cat.deleteUnitInSettings(unit),
                "Delete on '" + unit + "' should ask \"Are you sure you want to delete this unit?\" "
                        + "with a Yes action");
        sleep(3500);
        List<String> listed = cat.settingsUnitNames();
        boolean gone = listed.stream().noneMatch(n -> n.equalsIgnoreCase(unit));
        System.out.println(">>> Catering Settings after delete: " + listed + " | gone=" + gone);
        screenshot("catering-unit-deleted");
        if (gone) createdUnit = null;
        assertTrue(gone, "The deleted unit '" + unit + "' is still listed in Catering Settings: " + listed);

        // ...and it is gone from the module's tab strip too.
        cat.leaveCateringSettings();
        enterModule();
        List<String> tabs = chatPage().visibleUnitNames();
        assertTrue(tabs.stream().noneMatch(t -> t.equalsIgnoreCase(unit)),
                "The deleted unit '" + unit + "' is still a Catering tab: " + tabs);
    }

    // =====================================================================================
    // Web-gap cases (gaps/catering.md). Catering-specific groups only: the shared Bulletin
    // groups (search, offline/Retry, uploads, edit rules, selection, comments, emoji, ...) are
    // added to ToolChatBaseTest and inherited here.
    //
    // Roles on the automation project (2026-11-23): the emulators' Vidya Testing is CREW,
    // the Pixel's Vidya Pixel is the CATERER. Every case below reads the role from the screen
    // (addMember present = caterer) and skips with the reason when it needs the other one.
    // =====================================================================================

    /** Text markers this run posted in Breakfast; case 99 deletes them. */
    private static final List<String> POSTED = new java.util.ArrayList<>();

    private static String stamp() { return String.valueOf(System.currentTimeMillis() % 1000000); }

    /** Posts a ZL_AUTO marker in the selected meal and returns it, or null when it did not SEND. */
    private String postMarker(HomeChatPage page, String kind) {
        String m = "ZL_AUTO-cat-" + kind + "-" + stamp();
        page.sendMessage(m);
        POSTED.add(m);
        return page.waitMessageSent(m, Duration.ofSeconds(30)) ? m : null;
    }

    private void onBreakfast(HomeChatPage page) {
        assumeTrue(page.selectUnit("Breakfast"), "Could not select Breakfast");
        sleep(1000);
        catering().ensurePostable();
    }

    @Test
    @Order(60)
    @DisplayName("M3 [WEB CAT-194] A draft typed in Breakfast stays in Breakfast across a Lunch visit (CAT-02)")
    void draftStaysWithItsMealTab() {
        HomeChatPage page = chatPage();
        CateringPage cat = catering();
        onBreakfast(page);
        assumeTrue(page.isInputBarVisible(), "No composer on Breakfast — skipping");
        String draft = "ZL_AUTO-cat-draft-" + stamp();
        assumeTrue(cat.typeDraft(draft), "Could not type into the composer");
        String lunch = "", back = "";
        try {
            assumeTrue(page.selectUnit("Lunch"), "Could not select Lunch");
            sleep(1500);
            lunch = cat.composerText();
            assumeTrue(page.selectUnit("Breakfast"), "Could not return to Breakfast");
            sleep(2000);
            back = cat.composerText();
            System.out.println(">>> draft '" + draft + "': Lunch composer='" + lunch + "' Breakfast after='" + back + "'");
            screenshot("catering-draft-after-tab-switch");
        } finally {
            cat.clearComposer();
        }
        assertTrue(!lunch.contains(draft), "A Breakfast draft must not appear in Lunch's composer");
        assertTrue(back.contains(draft),
                "CAT-02: the unsent Breakfast draft should still be in the composer after visiting Lunch "
                        + "(web keeps it); the composer holds '" + back + "'");
    }

    @Test
    @Order(61)
    @DisplayName("M3 [WEB CAT-282] An unsent draft survives leaving Catering and reopening it (CAT-02)")
    void cateringDraftSurvivesReopen() {
        HomeChatPage page = chatPage();
        CateringPage cat = catering();
        onBreakfast(page);
        assumeTrue(page.isInputBarVisible(), "No composer on Breakfast — skipping");
        String draft = "ZL_AUTO-cat-draft2-" + stamp();
        assumeTrue(cat.typeDraft(draft), "Could not type into the composer");
        String after = "";
        try {
            try { driver.hideKeyboard(); } catch (Exception ignored) { }
            for (int i = 0; i < 3 && driver.findElements(byAppId("bottomNavigation")).isEmpty(); i++) {
                driver.navigate().back();
                sleep(1000);
            }
            enterModule();
            assumeTrue(page.selectUnit("Breakfast"), "Could not select Breakfast after reopening");
            sleep(2000);
            after = cat.composerText();
            System.out.println(">>> draft '" + draft + "' after reopening Catering: composer='" + after + "'");
            screenshot("catering-draft-after-reopen");
        } finally {
            cat.clearComposer();
        }
        assertTrue(after.contains(draft),
                "CAT-02: the unsent draft should survive leaving Catering and reopening it; the composer holds '"
                        + after + "'");
    }

    /** Labels Catering must never offer on a message (CateringActivity hard-codes them off). */
    private static final List<String> NEVER_IN_CATERING_MENU = List.of(
            "read by", "copy", "print", "edit and post", "image reply", "remote", "distribution", "publish");

    private static boolean has(List<String> opts, String label) {
        return opts.stream().anyMatch(o -> o.equalsIgnoreCase(label));
    }

    @Test
    @Order(62)
    @DisplayName("M10 [WEB CAT-352] An own text message offers exactly the Catering set — no Read By / Copy / Print / Download")
    void ownTextMenuIsCateringSet() {
        HomeChatPage page = chatPage();
        onBreakfast(page);
        assumeTrue(page.isInputBarVisible(), "No composer — skipping");
        String m = postMarker(page, "menu");
        assumeTrue(m != null, "The marker did not send — skipping");
        assumeTrue(page.longPressMessage(m), "Long-press did not open the menu");
        List<String> opts = page.menuOptions();
        page.closeMenu();
        boolean caterer = catering().isCatererMode();
        System.out.println(">>> own text menu (caterer=" + caterer + "): " + opts);
        List<String> missing = new java.util.ArrayList<>();
        for (String want : List.of("Edit", "Forward", "Gallery", "Share", "Delete"))
            if (!has(opts, want)) missing.add(want);
        assertTrue(missing.isEmpty(), "An own delivered text should offer " + missing + ". Menu: " + opts);
        // Reply follows the AUTHOR's department (CA showReplyOption): a caterer's own post has none.
        assertTrue(has(opts, "Reply") != caterer,
                (caterer ? "A caterer-authored message must NOT offer Reply" : "A crew member's message should offer Reply")
                        + ". Menu: " + opts);
        List<String> forbidden = opts.stream()
                .filter(o -> NEVER_IN_CATERING_MENU.stream().anyMatch(f -> o.toLowerCase().contains(f))
                        || o.equalsIgnoreCase("Download"))
                .toList();
        assertTrue(forbidden.isEmpty(), "Catering must not offer " + forbidden + " on a text message. Menu: " + opts);
    }

    @Test
    @Order(63)
    @DisplayName("M10 [WEB CAT-542] Another member's message offers no Edit")
    void foreignMessageHasNoEdit() {
        HomeChatPage page = chatPage();
        CateringPage cat = catering();
        onBreakfast(page);
        assumeTrue(page.isInputBarVisible(), "No composer — skipping");
        String m = postMarker(page, "own");
        assumeTrue(m != null, "The marker did not send — skipping");
        String me = cat.senderOfRow(m);
        assumeTrue(!me.isBlank(), "Could not read my own sender name from the posted row");
        CateringPage.Row other = cat.foreignRow(me, 8);
        assumeTrue(other != null, "No message from another member is visible in Breakfast (need the "
                + (cat.isCatererMode() ? "crew" : "caterer") + " side to post first)");
        System.out.println(">>> me='" + me + "' foreign row: " + other);
        assumeTrue(page.longPressMessage(other.text), "Long-press on the other member's message did not open the menu");
        List<String> opts = page.menuOptions();
        page.closeMenu();
        System.out.println(">>> foreign message menu: " + opts);
        screenshot("catering-foreign-menu");
        assertTrue(!has(opts, "Edit"), "Another member's message must not offer Edit. Menu: " + opts);
        assertTrue(opts.stream().noneMatch(o -> o.toLowerCase().contains("read by")),
                "Catering never offers Read By. Menu: " + opts);
    }

    /**
     * Waits until {@code count} rises above {@code before}. Counts are of RENDERED bubbles, so the
     * list is brought back to its newest row (scroll-to-bottom arrow) on every poll — a new bubble
     * inserted below the fold would otherwise never be counted.
     */
    private boolean grew(java.util.function.IntSupplier count, int before, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        CateringPage cat = catering();
        while (System.currentTimeMillis() < deadline) {
            if (cat.arrowDownShown()) cat.tapArrowDown();
            if (count.getAsInt() > before) return true;
            sleep(1000);
        }
        return false;
    }

    private static boolean docSent;
    private static final String DOC_FIXTURE = "zl_test_doc";

    @Test
    @Order(64)
    @DisplayName("M11 [WEB CAT-357] An own document without a caption offers Download and no Edit")
    void documentMenu() {
        HomeChatPage page = chatPage();
        CateringPage cat = catering();
        onBreakfast(page);
        assumeTrue(page.isAttachmentVisible(), "No paperclip — skipping");
        if (cat.arrowDownShown()) cat.tapArrowDown();
        int before = page.docBubbleCountByBadge();
        assumeTrue(cat.sendDocument(DOC_FIXTURE, ""), "The document upload did not start: " + cat.lastUploadProblem);
        assertTrue(grew(page::docBubbleCountByBadge, before, Duration.ofSeconds(60)),
                "The document should post as a new bubble — " + page.attachmentSendState());
        assertTrue(page.waitAttachmentSent(Duration.ofSeconds(60)),
                "The document should finish uploading — " + page.attachmentSendState());
        docSent = true;
        assumeTrue(page.longPressNewestDocBubble(), "Long-press on the document did not open the menu");
        List<String> opts = page.menuOptions();
        page.closeMenu();
        System.out.println(">>> document menu: " + opts);
        screenshot("catering-doc-menu");
        assertTrue(has(opts, "Download"), "A document should offer Download. Menu: " + opts);
        assertTrue(!has(opts, "Edit"), "A document without a caption must not offer Edit. Menu: " + opts);
    }

    @Test
    @Order(65)
    @DisplayName("M11 [WEB CAT-362] An own image with a caption offers Download and Edit, and no Edit and Post / Image Reply")
    void captionedImageMenu() {
        HomeChatPage page = chatPage();
        CateringPage cat = catering();
        onBreakfast(page);
        assumeTrue(page.isAttachmentVisible(), "No paperclip — skipping");
        String cap = "ZL_AUTO-cat-cap-" + stamp();
        assumeTrue(cat.sendGalleryImage(cap), "Image Gallery upload did not start: " + cat.lastUploadProblem);
        assertTrue(page.waitMessageSent(cap, Duration.ofSeconds(60)),
                "The captioned image should post with its caption — " + page.attachmentSendState());
        assumeTrue(page.longPressMessage(cap), "Long-press on the image's caption did not open the menu");
        List<String> opts = page.menuOptions();
        page.closeMenu();
        System.out.println(">>> captioned image menu: " + opts);
        screenshot("catering-image-menu");
        assertTrue(has(opts, "Download"), "An image should offer Download. Menu: " + opts);
        assertTrue(has(opts, "Edit"), "An own image WITH a caption should offer Edit (caption edit). Menu: " + opts);
        assertTrue(!has(opts, "Edit and Post") && opts.stream().noneMatch(o -> o.toLowerCase().contains("image reply")),
                "Catering must not offer Edit and Post or Image Reply. Menu: " + opts);
    }

    @Test
    @Order(66)
    @DisplayName("M11 [WEB CAT-367] An own audio file offers Download and no Edit")
    void audioMenu() {
        HomeChatPage page = chatPage();
        CateringPage cat = catering();
        onBreakfast(page);
        assumeTrue(page.isAttachmentVisible(), "No paperclip — skipping");
        int before = page.audioBubbleCount();
        assumeTrue(cat.sendAudioFile("zl_test_audio"), "The Audio upload did not start: " + cat.lastUploadProblem);
        assertTrue(grew(page::audioBubbleCount, before, Duration.ofSeconds(60)),
                "The audio file should post as an audio bubble — " + page.attachmentSendState());
        page.waitAttachmentSent(Duration.ofSeconds(60));
        assumeTrue(cat.longPressNewestAudio(), "Long-press on the audio bubble did not open the menu");
        List<String> opts = page.menuOptions();
        page.closeMenu();
        System.out.println(">>> audio menu: " + opts);
        screenshot("catering-audio-menu");
        assertTrue(has(opts, "Download"), "An audio message should offer Download. Menu: " + opts);
        assertTrue(!has(opts, "Edit"), "An audio message (no caption) must not offer Edit. Menu: " + opts);
    }

    @Test
    @Order(67)
    @DisplayName("M31 [WEB CAT-527] Gallery → Docs lists the uploaded document with a readable size (CAT-05)")
    void galleryDocsTab() {
        HomeChatPage page = chatPage();
        CateringPage cat = catering();
        onBreakfast(page);
        assumeTrue(docSent, "No document was uploaded by case 64 this run");
        String m = postMarker(page, "gal");
        assumeTrue(m != null, "The marker did not send — skipping");
        assertTrue(page.galleryOpensLibrary(m), "Gallery should open the chat library");
        try {
            assertTrue(cat.openLibraryTab("Docs"), "The library should have a Docs tab");
            List<String[]> rows = cat.libraryRows();
            rows.forEach(r -> System.out.println(">>> Docs row: " + r[0] + " | " + r[1]));
            screenshot("catering-gallery-docs");
            String[] doc = rows.stream().filter(r -> r[0].contains(DOC_FIXTURE)).findFirst().orElse(null);
            assertTrue(doc != null, "Docs should list " + DOC_FIXTURE + ". Rows: "
                    + rows.stream().map(r -> r[0]).toList());
            String size = doc[1].split("·")[0].trim();
            assertTrue(size.matches("(?i)\\d+(\\.\\d+)?\\s?(B|KB|MB|GB)"),
                    "CAT-05: the Docs size should read like '12 KB', not a bare number; it reads '" + doc[1] + "'");
        } finally {
            page.leaveOverlayScreen();
        }
    }

    @Test
    @Order(68)
    @DisplayName("M33 [WEB CAT-215] Posting is forced on in Catering: no REQUEST bar, the paperclip is there")
    void noRequestBar() {
        HomeChatPage page = chatPage();
        CateringPage cat = catering();
        onBreakfast(page);
        boolean request = cat.requestBarVisible();
        System.out.println(">>> REQUEST bar=" + request + " paperclip=" + page.isAttachmentVisible());
        assertTrue(!request, "Catering keeps posting on (handlePostingRights is a no-op): no REQUEST bar");
        assertTrue(page.isAttachmentVisible(), "The paperclip should be offered in Catering");
    }

    @Test
    @Order(69)
    @DisplayName("M24 [WEB CAT-724/742] Delete selection refuses another member's message (no permission / 2 h) and keeps the count at 1")
    void deleteSelectionRefusesForeignRow() {
        HomeChatPage page = chatPage();
        CateringPage cat = catering();
        onBreakfast(page);
        assumeTrue(page.isInputBarVisible(), "No composer — skipping");
        String m = postMarker(page, "delsel");
        assumeTrue(m != null, "The marker did not send — skipping");
        String me = cat.senderOfRow(m);
        assumeTrue(page.longPressMessage(m), "Long-press failed");
        assumeTrue(page.tapMenuOption("Delete"), "Delete not offered on my own message");
        try {
            assertTrue(cat.selectionBarShown(), "Delete should open the selection bar");
            assertEquals("1", cat.selectionCount(), "The own message should be selected (count 1)");
            CateringPage.Row other = cat.foreignRow(me, 6);
            assumeTrue(other != null, "No message from another member visible — need the other side to post");
            System.out.println(">>> tapping foreign row in delete mode: " + other);
            cat.tapRow(other.text);
            boolean refused = cat.snackShown("no permission to delete other user message", Duration.ofSeconds(4))
                    || cat.snackShown("delete message after 2 Hours", Duration.ofSeconds(1));
            String count = cat.selectionCount();
            System.out.println(">>> refused=" + refused + " count=" + count);
            screenshot("catering-delete-foreign-refused");
            // CateringActivity: an ADMIN may select anyone's row; everyone else gets the snackbar.
            assumeTrue(refused || !"1".equals(count),
                    "Neither refused nor selected — the tap did not land on the row");
            assertTrue(refused && "1".equals(count),
                    "A non-admin must not be able to select another member's message for delete "
                            + "(expected 'You have no permission to delete other user message.' and count 1; "
                            + "count is '" + count + "'). If this account is a project admin the app allows it.");
        } finally {
            cat.leaveSelection();
        }
        cat.tapArrowDown();
        assertTrue(page.messageShownScrolled(m, 4), "Leaving the selection must not delete anything");
    }

    // ---------------- caterer-only (run on the caterer's device) ----------------

    @Test
    @Order(70)
    @DisplayName("M23 [WEB CAT-624/652] CATERER — a user picked in Breakfast is NOT carried into Lunch")
    void receiverDoesNotLeakAcrossMeals() {
        CateringPage cat = catering();
        HomeChatPage page = chatPage();
        assumeTrue(cat.isCatererMode(), "Not a caterer — this is the caterer's recipient picker");
        assumeTrue(page.selectUnit("Breakfast"), "Could not select Breakfast");
        sleep(1000);
        assumeTrue(cat.openRecipientPicker() && cat.isRecipientDialogOpen(Duration.ofSeconds(6)),
                "Recipient dialog did not open");
        assumeTrue(cat.chooseSelectUser(), "Could not choose Select User");
        String picked = cat.pickFirstUserFromList();
        assumeTrue(!picked.isBlank(), "No user to pick");
        String inBreakfast = cat.selectedRecipient();
        assumeTrue(page.selectUnit("Lunch"), "Could not select Lunch");
        sleep(2000);
        String inLunch = cat.selectedRecipient();
        System.out.println(">>> picked '" + picked + "' in Breakfast (label '" + inBreakfast
                + "') → Lunch label '" + inLunch + "' hint=" + cat.isRecipientHintShown());
        screenshot("catering-receiver-lunch");
        try {
            assertTrue(!inLunch.equalsIgnoreCase(inBreakfast) || inBreakfast.isBlank(),
                    "The recipient '" + inBreakfast + "' picked in Breakfast leaked into Lunch "
                            + "(handleUnitChangeObserver never resets userName.tag). Lunch should start at 'Select User'.");
        } finally {
            if (cat.openRecipientPicker() && cat.isRecipientDialogOpen(Duration.ofSeconds(6))) cat.chooseAllUsers();
            page.selectUnit("Breakfast");
        }
    }

    @Test
    @Order(71)
    @DisplayName("M26 [WEB CAT-606] CATERER — Catering Settings lists the meal tabs, refuses to delete Breakfast, and search filters")
    void cateringSettingsScreen() {
        CateringPage cat = catering();
        HomeChatPage page = chatPage();
        assumeTrue(cat.isCatererMode(), "Not a caterer — Catering Settings is only listed for the catering department");
        List<String> tabs = page.visibleUnitNames();
        assumeTrue(cat.openCateringSettings(), "Could not open Settings → Catering Settings");
        try {
            List<String> units = cat.settingsUnitNames();
            System.out.println(">>> tabs " + tabs + " | Catering Settings " + units);
            for (String meal : MEALS)
                assertTrue(units.stream().anyMatch(u -> u.equalsIgnoreCase(meal)),
                        "Catering Settings should list '" + meal + "'. Listed: " + units);
            assertTrue(cat.tapDeleteOnSettingsRow("Breakfast"), "Breakfast should have a delete control");
            boolean refused = cat.snackShown("Unit Cannot be Deleted", Duration.ofSeconds(4));
            screenshot("catering-settings-system-unit");
            assertTrue(refused, "Deleting the system unit Breakfast should be refused with 'Unit Cannot be Deleted.'");
            assertTrue(cat.settingsUnitNamesOnScreen().stream().anyMatch(u -> u.equalsIgnoreCase("Breakfast")),
                    "Breakfast must still be listed after the refused delete");
            cat.searchSettingsUnits("ZL_AUTO-nomatch-" + stamp());
            List<String> none = cat.settingsUnitNamesOnScreen();
            cat.searchSettingsUnits("Lunch");
            List<String> lunch = cat.settingsUnitNamesOnScreen();
            System.out.println(">>> search nonsense → " + none + " | 'Lunch' → " + lunch);
            cat.searchSettingsUnits("");
            assertTrue(none.isEmpty(), "A nonsense search should list no units: " + none);
            assertTrue(!lunch.isEmpty() && lunch.stream().allMatch(u -> u.toLowerCase().contains("lunch")),
                    "Searching 'Lunch' should list only matching units: " + lunch);
        } finally {
            cat.leaveCateringSettings();
            moduleReopenNeeded();
        }
    }

    @Test
    @Order(51)
    @DisplayName("M27 [WEB CAT-630/712/736] CATERER — a second unit with a name already in use is refused")
    void duplicateUnitNameRefused() {
        CateringPage cat = catering();
        HomeChatPage page = chatPage();
        assumeTrue(cat.isCatererMode(), "Not a caterer — skipping");
        assumeTrue(createdUnit != null, "Case 48 created no unit this run — nothing to duplicate");
        String dup = createdUnit;
        assumeTrue(cat.openCreateUnit() && cat.isCreateUnitScreenOpen(Duration.ofSeconds(10)),
                "Create-unit screen did not open");
        cat.setUnitName(dup);
        cat.submitUnit();
        sleep(2500);
        boolean stillOnForm = cat.isCreateUnitScreenOpen(Duration.ofSeconds(3));
        System.out.println(">>> duplicate '" + dup + "': still on form=" + stillOnForm);
        screenshot("catering-unit-duplicate");
        if (stillOnForm) cat.cancelCreateUnit();
        sleep(1500);
        long copies = page.visibleUnitNames().stream().filter(u -> u.equalsIgnoreCase(dup)).count();
        if (copies > 1) {
            // Wrongly accepted: remove the extra copy so the run leaves one unit behind at most.
            if (cat.openCateringSettings()) {
                cat.deleteUnitInSettings(dup);
                sleep(3000);
                cat.leaveCateringSettings();
            }
            moduleReopenNeeded();
        }
        assertTrue(stillOnForm && copies <= 1,
                "Creating a second unit named '" + dup + "' should be refused and keep the form open "
                        + "(stillOnForm=" + stillOnForm + ", copies in the tab strip=" + copies + ")");
    }

    // ---------------- Catering versions of the shared feed groups ----------------
    // ToolChatBaseTest's shared groups run only for the ConfidentialInfo.kt feeds
    // (infoFeedCases()); Catering differs (no History, no Read By, "Image Gallery"), so the
    // Catering web cases that carry Catering sheet bugs are covered here.

    private boolean onEmulator() {
        String udid = System.getenv("ZILLIT_UDID");
        return udid != null && udid.startsWith("emulator-");
    }

    private boolean setConnectivity(boolean on) {
        try {
            driver.executeScript("mobile: setConnectivity", java.util.Map.of("wifi", on, "data", on));
            return true;
        } catch (Exception e) {
            System.out.println(">>> setConnectivity(" + on + ") unavailable: " + e.getMessage());
            return false;
        }
    }

    @Test
    @Order(80)
    @DisplayName("M1 [WEB CAT-557] A message sent offline shows Retry; Retry offline is refused; back online Retry delivers it once")
    void offlineSendRetry() {
        HomeChatPage page = chatPage();
        CateringPage cat = catering();
        assumeTrue(onEmulator(), "Offline cases run only on an emulator (ZILLIT_UDID=emulator-*): the Pixel is shared");
        onBreakfast(page);
        assumeTrue(page.isInputBarVisible(), "No composer — skipping");
        String m = "ZL_AUTO-cat-offline-" + stamp();
        POSTED.add(m);
        boolean off = false, retryShown = false, offlineRetryRefused = false;
        String offlineState = "";
        try {
            off = setConnectivity(false);
            assumeTrue(off, "This driver would not toggle connectivity — skipping");
            sleep(3000);
            page.sendMessage(m);
            long deadline = System.currentTimeMillis() + 25000;
            while (System.currentTimeMillis() < deadline && !retryShown) {
                retryShown = page.messageFailed(m);
                sleep(1000);
            }
            offlineState = page.sendStateOf(m);
            System.out.println(">>> offline send: " + offlineState);
            screenshot("catering-offline-send");
            if (retryShown && cat.tapRetryOn(m)) {
                offlineRetryRefused = cat.snackShown("Internet connection lost", Duration.ofSeconds(4));
            }
        } finally {
            if (off) { setConnectivity(true); sleep(6000); }
        }
        if (page.messageFailed(m)) cat.tapRetryOn(m);
        boolean delivered = page.waitMessageSent(m, Duration.ofSeconds(40));
        int copies = cat.exactRowCount(m);
        System.out.println(">>> back online: delivered=" + delivered + " copies=" + copies
                + " offlineRetryRefused=" + offlineRetryRefused);
        assertTrue(retryShown, "A message sent offline should turn into a failed row offering Retry (btnTryAgain); "
                + "it is " + offlineState + " (CateringVm.sendTextMessage waits for translateTextToOtherLanguage, a network "
                + "call, before saving the row)");
        assertTrue(offlineRetryRefused, "Retry while still offline should say 'Internet connection lost…'");
        assertTrue(delivered, "Back online, Retry should deliver the message — it is " + page.sendStateOf(m));
        assertTrue(copies == 1, "Exactly one copy should be posted, found " + copies);
    }

    @Test
    @Order(81)
    @DisplayName("M2 [WEB CAT-44] 2,000 characters post; 2,001 are refused with the limit snackbar and kept (CAT-04)")
    void messageLengthLimit() {
        HomeChatPage page = chatPage();
        CateringPage cat = catering();
        onBreakfast(page);
        assumeTrue(page.isInputBarVisible(), "No composer — skipping");
        String head = "ZL_AUTO-cat-limit-" + stamp() + "-";
        String over = head + "x".repeat(2001 - head.length());
        assumeTrue(cat.typeDraft(over), "Could not type into the composer");
        page.tapSend();
        boolean snack = cat.snackShown("Characters limit allowed to send", Duration.ofSeconds(5));
        String kept = cat.composerText();
        boolean posted = cat.rowContaining(head, Duration.ofSeconds(3));
        System.out.println(">>> 2,001 chars: snackbar=" + snack + " kept=" + kept.length() + " posted=" + posted);
        cat.clearComposer();
        assertTrue(snack, "2,001 characters should be refused with 'There is only 2000 Characters limit allowed to send'");
        assertTrue(!posted, "The over-limit message must not post");
        String exact = head.replace("limit", "lim2k") + "y".repeat(2000 - head.length());
        assumeTrue(cat.typeDraft(exact), "Could not type into the composer");
        page.tapSend();
        POSTED.add(exact.substring(0, 30));
        boolean ok = cat.rowContaining(exact.substring(0, 30), Duration.ofSeconds(20));
        assertTrue(ok, "Exactly 2,000 characters should post");
        // Web caps at 8,000 with a counter from 7,200; Android's TEXT_LIMIT is 2,000 (CAT-04, open).
    }

    @Test
    @Order(82)
    @DisplayName("M9 [WEB CAT-387] An empty edit is refused ('Message could not be empty') and the dialog stays open")
    void emptyEditRefused() {
        HomeChatPage page = chatPage();
        CateringPage cat = catering();
        onBreakfast(page);
        String m = postMarker(page, "edempty");
        assumeTrue(m != null, "The marker did not send — skipping");
        assumeTrue(page.longPressMessage(m) && page.tapMenuOption("Edit"), "Edit not offered");
        assumeTrue(page.isEditDialogOpen(Duration.ofSeconds(8)), "Edit dialog did not open");
        page.setEditDialogText("   ");
        page.saveEditDialog();
        boolean error = cat.snackShown("Message could not be empty", Duration.ofSeconds(4));
        boolean stillOpen = page.isEditDialogOpen(Duration.ofSeconds(2));
        System.out.println(">>> empty edit: error=" + error + " dialogStillOpen=" + stillOpen);
        if (stillOpen) page.cancelEditDialog();
        sleep(800);
        // KB shows the error as EditText.setError — a popup outside the accessibility tree — so
        // the refusal is asserted by its effect: the dialog stays open and nothing changes.
        System.out.println(">>> 'Message could not be empty' visible to UiAutomator=" + error);
        assertTrue(stillOpen, "An empty edit must be refused: the edit dialog should stay open");
        assertTrue(page.messageShown(m, Duration.ofSeconds(3)), "The original text must be unchanged");
    }

    @Test
    @Order(83)
    @DisplayName("M9 [WEB CAT-387] Update with unchanged text does not mark the message 'Edited' (CAT-03)")
    void unchangedUpdateNotEdited() {
        HomeChatPage page = chatPage();
        CateringPage cat = catering();
        onBreakfast(page);
        String m = postMarker(page, "edsame");
        assumeTrue(m != null, "The marker did not send — skipping");
        assumeTrue(page.longPressMessage(m) && page.tapMenuOption("Edit"), "Edit not offered");
        assumeTrue(page.isEditDialogOpen(Duration.ofSeconds(8)), "Edit dialog did not open");
        page.saveEditDialog();          // nothing changed
        sleep(4000);
        String time = cat.rowTimeText(m);
        System.out.println(">>> unchanged update: row time '" + time + "'");
        screenshot("catering-unchanged-update");
        assumeTrue(!time.isBlank(), "Could not read the row's time label");
        assertTrue(!time.startsWith("Edited"),
                "CAT-03: Update with no change should not mark the message 'Edited' (the row reads '" + time + "')");
    }

    @Test
    @Order(84)
    @DisplayName("M4 [WEB CAT-104/517] Search finds a posted marker; empty search is refused; a nonsense word counts 0")
    void searchMarker() {
        HomeChatPage page = chatPage();
        CateringPage cat = catering();
        onBreakfast(page);
        String m = postMarker(page, "find");
        assumeTrue(m != null, "The marker did not send — skipping");
        assumeTrue(cat.openSearch(), "The search icon (ivSearch) did not open the search bar");
        try {
            cat.search("");
            boolean emptyRefused = cat.snackShown("Please type text to search", Duration.ofSeconds(4));
            cat.leaveSelection();                       // a fresh bar: the refused search leaves no count
            assumeTrue(cat.openSearch(), "The search bar did not reopen");
            cat.search(m);
            String found = cat.searchCount(Duration.ofSeconds(8));
            cat.search("zzqq-nomatch-" + stamp());
            String none = cat.searchCount(Duration.ofSeconds(8));
            System.out.println(">>> search: empty refused=" + emptyRefused + " '" + m + "' → " + found + " nonsense → " + none);
            screenshot("catering-search");
            assertTrue(emptyRefused, "Searching with an empty box should say 'Please type text to search.'");
            assertTrue(found.matches("[1-9]\\d*"), "Searching the posted marker should count at least 1, got '" + found + "'");
            assertTrue("0".equals(none), "A nonsense word should count 0, got '" + none + "'");
        } finally {
            cat.leaveSelection();
        }
        assertTrue(page.isInputBarVisible(), "Closing search should bring the composer back");
    }

    @Test
    @Order(199)
    @DisplayName("Clean-up: this run's ZL_AUTO text markers in Breakfast are deleted")
    void sweepPostedMarkers() {
        HomeChatPage page = chatPage();
        assumeTrue(!POSTED.isEmpty(), "Nothing posted this run");
        assumeTrue(page.selectUnit("Breakfast"), "Could not select Breakfast");
        sleep(1200);
        int deleted = 0;
        for (String m : new java.util.ArrayList<>(POSTED)) {
            if (!page.messageShown(m, Duration.ofSeconds(2))) continue;
            try {
                if (!page.longPressMessage(m) || !page.tapMenuOption("Delete")) { page.leaveOverlayScreen(); continue; }
                page.tapDeleteActionBar(Duration.ofSeconds(6));
                if (confirmYesDialog(Duration.ofSeconds(6))) { deleted++; POSTED.remove(m); }
                sleep(1200);
            } catch (Exception e) {
                catering().leaveSelection();
            }
        }
        System.out.println(">>> clean-up deleted " + deleted + ", left " + POSTED);
    }

    // ---------------- batch 2: remaining Catering web rows ----------------

    @Test
    @Order(85)
    @DisplayName("M4 [WEB CAT-522/104] Search by my own name lists my rows; a space-only query is refused like an empty one")
    void searchOwnNameAndSpaces() {
        HomeChatPage page = chatPage();
        CateringPage cat = catering();
        onBreakfast(page);
        String m = postMarker(page, "me");
        assumeTrue(m != null, "The marker did not send — skipping");
        String me = cat.senderOfRow(m);
        assumeTrue(!me.isBlank(), "Could not read my sender name");
        assumeTrue(cat.openSearch(), "The search bar did not open");
        String mine, spaces;
        boolean spaceRefused;
        try {
            cat.search(me);
            mine = cat.searchCount(Duration.ofSeconds(8));
            cat.search("   ");
            spaceRefused = cat.snackShown("Please type text to search", Duration.ofSeconds(4));
            spaces = cat.selectionCount();
            System.out.println(">>> search '" + me + "' → " + mine + " | spaces refused=" + spaceRefused + " count='" + spaces + "'");
        } finally {
            cat.leaveSelection();
        }
        assertTrue(mine.matches("[1-9]\\d*"), "Searching my own name should list my rows, count was '" + mine + "'");
        assertTrue(spaceRefused, "A space-only search should be refused like an empty one (web: 'Please write in search box'); "
                + "Android trims it to \"\" and matches every row (count '" + spaces + "')");
    }

    private void officeDocument(String fixture) {
        HomeChatPage page = chatPage();
        CateringPage cat = catering();
        onBreakfast(page);
        assumeTrue(page.isAttachmentVisible(), "No paperclip — skipping");
        String cap = "ZL_AUTO-cat-" + fixture.substring(fixture.lastIndexOf('.') + 1) + "-" + stamp();
        boolean started = cat.sendDocument(fixture.substring(0, fixture.lastIndexOf('.')), cap);
        if (!started) screenshot("catering-doc-" + fixture);
        if (fixture.endsWith(".csv") && cat.lastUploadProblem.contains("could not pick")) {
            // The fixture is in Download (prep pushes it); the system picker hides it because the
            // Document picker's MIME allow-list (mediaHandler/contracts/FilePickerContract) has no text/csv.
            assertTrue(false, "CSV cannot be chosen in Catering's Document picker (web CAT-317 uploads one): "
                    + "FilePickerContract's MIME allow-list has no text/csv — " + cat.lastUploadProblem);
        }
        assumeTrue(started, "The " + fixture + " upload did not start: " + cat.lastUploadProblem);
        boolean sent = page.waitMessageSent(cap, Duration.ofSeconds(120));
        System.out.println(">>> " + fixture + " posted=" + sent + " (" + page.attachmentSendState() + ")");
        assertTrue(sent, fixture + " should post with its caption — " + page.sendStateOf(cap));
    }

    @Test
    @Order(86)
    @DisplayName("M5 [WEB CAT-307] An XLSX document posts through Document with its caption")
    void xlsxUpload() { officeDocument("zl_drive_book.xlsx"); }

    @Test
    @Order(86)
    @DisplayName("M5 [WEB CAT-312] A DOCX document posts through Document with its caption")
    void docxUpload() { officeDocument("zl_drive_doc.docx"); }

    @Test
    @Order(86)
    @DisplayName("M5 [WEB CAT-317] A CSV document posts through Document with its caption")
    void csvUpload() { officeDocument("zl_drive_sheet.csv"); }

    @Test
    @Order(87)
    @DisplayName("M6 [WEB CAT-332] Two images picked at once land as two image rows")
    void twoImagesAtOnce() {
        HomeChatPage page = chatPage();
        CateringPage cat = catering();
        onBreakfast(page);
        assumeTrue(page.isAttachmentVisible(), "No paperclip — skipping");
        int before = page.imageBubbleCount();
        assumeTrue(cat.sendGalleryImages(2, ""), "Two-image upload did not start: " + cat.lastUploadProblem);
        sleep(3000);
        boolean two = grew(page::imageBubbleCount, before + 1, Duration.ofSeconds(60));
        System.out.println(">>> image bubbles " + before + " → " + page.imageBubbleCount());
        assertTrue(two, "Two picked images should land as two image rows (" + before + " → " + page.imageBubbleCount() + ")");
    }

    @Test
    @Order(88)
    @DisplayName("M7 [WEB CAT-74] Backing out of the document preview posts nothing")
    void cancelPreviewPostsNothing() {
        HomeChatPage page = chatPage();
        CateringPage cat = catering();
        onBreakfast(page);
        assumeTrue(page.isAttachmentVisible(), "No paperclip — skipping");
        int before = page.docBubbleCountByBadge();
        boolean preview = cat.openDocumentPreview(DOC_FIXTURE);
        assumeTrue(preview, "No preview screen opened after picking the document (nothing to cancel)");
        cat.cancelPreview();
        page.leaveOverlayScreen();
        sleep(3000);
        int after = page.docBubbleCountByBadge();
        System.out.println(">>> doc bubbles before " + before + " after cancel " + after);
        assertTrue(after <= before, "Cancelling the preview must post nothing (" + before + " → " + after + ")");
    }

    @Test
    @Order(89)
    @DisplayName("M8 [WEB CAT-322] A captioned document's Edit changes the caption and marks it 'Edited'")
    void captionEditOnDocument() {
        HomeChatPage page = chatPage();
        CateringPage cat = catering();
        onBreakfast(page);
        assumeTrue(page.isAttachmentVisible(), "No paperclip — skipping");
        String cap = "ZL_AUTO-cat-doccap-" + stamp();
        assumeTrue(cat.sendDocument(DOC_FIXTURE, cap), "The document upload did not start: " + cat.lastUploadProblem);
        assertTrue(page.waitMessageSent(cap, Duration.ofSeconds(60)), "The captioned document should post — " + page.sendStateOf(cap));
        assumeTrue(page.longPressMessage(cap), "Long-press on the caption did not open the menu");
        List<String> opts = page.menuOptions();
        assertTrue(has(opts, "Edit"), "A captioned document should offer Edit. Menu: " + opts);
        page.tapMenuOption("Edit");
        assumeTrue(page.isEditDialogOpen(Duration.ofSeconds(8)), "The caption edit dialog did not open");
        String edited = cap + " edited";
        page.setEditDialogText(edited);
        page.saveEditDialog();
        assertTrue(page.messageShown(edited, Duration.ofSeconds(20)), "The edited caption should be shown");
        sleep(2000);
        String time = cat.rowTimeText(edited);
        System.out.println(">>> edited caption row time '" + time + "'");
        assertTrue(time.startsWith("Edited"), "The captioned document should carry the red 'Edited' tag, time reads '" + time + "'");
    }

    @Test
    @Order(90)
    @DisplayName("M9 [WEB CAT-382] Edit → Cancel leaves the message unchanged and not 'Edited'")
    void editCancelKeepsText() {
        HomeChatPage page = chatPage();
        CateringPage cat = catering();
        onBreakfast(page);
        String m = postMarker(page, "edcancel");
        assumeTrue(m != null, "The marker did not send — skipping");
        assumeTrue(page.longPressMessage(m) && page.tapMenuOption("Edit"), "Edit not offered");
        assumeTrue(page.isEditDialogOpen(Duration.ofSeconds(8)), "Edit dialog did not open");
        page.setEditDialogText(m + "-NOPE");
        page.cancelEditDialog();
        sleep(2500);
        boolean changed = cat.rowContaining(m + "-NOPE", Duration.ofSeconds(2));
        String time = cat.rowTimeText(m);
        System.out.println(">>> cancel edit: changed=" + changed + " time='" + time + "'");
        assertTrue(!changed && cat.rowContaining(m, Duration.ofSeconds(2)), "Cancel must keep the original text");
        assertTrue(!time.startsWith("Edited"), "Cancel must not mark the message 'Edited' ('" + time + "')");
    }

    @Test
    @Order(91)
    @DisplayName("M9 [WEB CAT-387] An edit over 2,000 characters is refused with the limit snackbar")
    void overLimitEditRefused() {
        HomeChatPage page = chatPage();
        CateringPage cat = catering();
        onBreakfast(page);
        String m = postMarker(page, "edlong");
        assumeTrue(m != null, "The marker did not send — skipping");
        assumeTrue(page.longPressMessage(m) && page.tapMenuOption("Edit"), "Edit not offered");
        assumeTrue(page.isEditDialogOpen(Duration.ofSeconds(8)), "Edit dialog did not open");
        page.setEditDialogText(m + "x".repeat(2001));
        page.saveEditDialog();
        // KB shows the word_limit_error snackbar on the ACTIVITY behind the edit dialog, a window
        // UiAutomator does not read while the dialog is up — so the refusal is asserted by effect.
        boolean refused = cat.snackShown("Characters limit allowed to send", Duration.ofSeconds(3));
        boolean stillOpen = page.isEditDialogOpen(Duration.ofSeconds(2));
        System.out.println(">>> over-limit edit: snackbar visible=" + refused + " dialog still open=" + stillOpen);
        if (stillOpen) page.cancelEditDialog();
        sleep(1500);
        assertTrue(stillOpen, "An edit over 2,000 characters must be refused: the edit dialog should stay open");
        assertTrue(cat.exactRowCount(m) == 1, "The original message should be unchanged");
    }

    @Test
    @Order(92)
    @DisplayName("M12 [WEB CAT-497/502] Download on an own document and an own image says 'File Downloaded Successfully.'")
    void downloadDocumentAndImage() {
        HomeChatPage page = chatPage();
        CateringPage cat = catering();
        onBreakfast(page);
        assumeTrue(docSent, "No document was uploaded by case 64 this run");
        assumeTrue(page.longPressNewestDocBubble(), "Long-press on the document did not open the menu");
        assumeTrue(page.tapMenuOption("Download"), "Download not offered on the document");
        boolean docOk = cat.snackShown("File Downloaded Successfully", Duration.ofSeconds(25));
        boolean noRight = cat.snackShown("Downloading Rights", Duration.ofSeconds(1));
        if (noRight) page.clearBlockingPopup();
        System.out.println(">>> document download ok=" + docOk + " rightsAlert=" + noRight);
        assertTrue(!noRight, "This account has no Catering Download right (rights alert shown) — see M25");
        assertTrue(docOk, "Downloading the document should report 'File Downloaded Successfully.'");
        String cap = "ZL_AUTO-cat-dlimg-" + stamp();
        assumeTrue(cat.sendGalleryImage(cap), "Image upload did not start: " + cat.lastUploadProblem);
        assumeTrue(page.waitMessageSent(cap, Duration.ofSeconds(60)), "The image did not post");
        assumeTrue(page.longPressMessage(cap) && page.tapMenuOption("Download"), "Download not offered on the image");
        assertTrue(cat.snackShown("File Downloaded Successfully", Duration.ofSeconds(25)),
                "Downloading the image should report 'File Downloaded Successfully.'");
    }

    @Test
    @Order(93)
    @DisplayName("M13 [WEB CAT-512/507] An image opens the in-app viewer and Back closes it; a PDF hands off to a viewer app")
    void viewers() {
        HomeChatPage page = chatPage();
        CateringPage cat = catering();
        onBreakfast(page);
        String cap = "ZL_AUTO-cat-view-" + stamp();
        assumeTrue(cat.sendGalleryImage(cap), "Image upload did not start: " + cat.lastUploadProblem);
        assumeTrue(page.waitMessageSent(cap, Duration.ofSeconds(60)), "The image did not post");
        boolean viewer = cat.openImageWithCaption(cap);
        System.out.println(">>> image viewer opened=" + viewer);
        page.closeViewer();
        boolean back = page.isInputBarVisible();
        assertTrue(viewer, "Tapping an image should open the in-app viewer (CustomImageViewer)");
        assertTrue(back, "Back should close the viewer and show the chat again");
        assumeTrue(docSent, "No document was uploaded by case 64 this run");
        boolean opened = page.openNewestDoc();
        String pkg = page.safeCurrentPackage();
        System.out.println(">>> PDF opened=" + opened + " foreground=" + pkg);
        screenshot("catering-pdf-viewer");
        page.returnToChat(appPackage);
        if (!page.isInputBarVisible()) enterModule();
        assertTrue(opened, "Tapping the PDF should open it (Android hands documents to a viewer app)");
    }

    @Test
    @Order(94)
    @DisplayName("M14 [WEB CAT-372] Selection mode counts the selection, hides the composer, and Back leaves it")
    void selectionModeBar() {
        HomeChatPage page = chatPage();
        CateringPage cat = catering();
        onBreakfast(page);
        String a = postMarker(page, "selA");
        String b = postMarker(page, "selB");
        assumeTrue(a != null && b != null, "The markers did not send — skipping");
        assumeTrue(page.longPressMessage(a) && page.tapMenuOption("Delete"), "Delete not offered");
        String one, two;
        boolean composerHidden;
        try {
            one = cat.selectionCount();
            composerHidden = !page.isInputBarVisible();
            cat.tapRow(b);
            two = cat.selectionCount();
            System.out.println(">>> selection count " + one + " → " + two + ", composer hidden=" + composerHidden);
        } finally {
            cat.leaveSelection();
        }
        assertEquals("1", one, "Delete should start selection mode with 1 selected");
        assertTrue(composerHidden, "Selection mode should hide the composer");
        assertEquals("2", two, "Tapping another own row should count 2");
        assertTrue(page.isInputBarVisible(), "Back should bring the composer back");
        assertTrue(cat.rowContaining(a, Duration.ofSeconds(3)) && cat.rowContaining(b, Duration.ofSeconds(3)),
                "Leaving selection mode must delete nothing");
    }

    @Test
    @Order(95)
    @DisplayName("M15 [WEB CAT-402] Delete can be backed out of: Back at the selection, and No at the dialog")
    void deleteBackOut() {
        HomeChatPage page = chatPage();
        CateringPage cat = catering();
        onBreakfast(page);
        String m = postMarker(page, "delno");
        assumeTrue(m != null, "The marker did not send — skipping");
        assumeTrue(page.longPressMessage(m) && page.tapMenuOption("Delete"), "Delete not offered");
        cat.leaveSelection();
        assertTrue(cat.rowContaining(m, Duration.ofSeconds(3)), "Backing out of the selection must keep the message");
        assumeTrue(page.longPressMessage(m) && page.tapMenuOption("Delete"), "Delete not offered");
        page.tapDeleteActionBar(Duration.ofSeconds(6));
        assumeTrue(page.isEditDialogOpen(Duration.ofSeconds(1)) || cat.snackShown("Are you sure you want to delete", Duration.ofSeconds(6)),
                "The delete confirmation did not appear");
        page.tapDialogLeft();   // "No"
        sleep(1500);
        assertTrue(cat.rowContaining(m, Duration.ofSeconds(3)), "No at the confirmation must keep the message");
        assertTrue(page.isInputBarVisible(), "No should also leave selection mode (composer back)");
    }

    @Test
    @Order(96)
    @DisplayName("M15 [WEB CAT-407/412] Two selected messages are deleted together and stay gone after a reopen")
    void multiDeleteStaysGone() {
        HomeChatPage page = chatPage();
        CateringPage cat = catering();
        onBreakfast(page);
        String a = postMarker(page, "mdA");
        String b = postMarker(page, "mdB");
        assumeTrue(a != null && b != null, "The markers did not send — skipping");
        assumeTrue(page.longPressMessage(a) && page.tapMenuOption("Delete"), "Delete not offered");
        cat.tapRow(b);
        assertEquals("2", cat.selectionCount(), "Both own messages should be selected");
        page.tapDeleteActionBar(Duration.ofSeconds(6));
        assertTrue(confirmYesDialog(Duration.ofSeconds(8)), "The delete confirmation should appear");
        sleep(2500);
        assertTrue(!cat.rowContaining(a, Duration.ofSeconds(2)) && !cat.rowContaining(b, Duration.ofSeconds(1)),
                "Both messages should be gone");
        for (int i = 0; i < 3 && driver.findElements(byAppId("bottomNavigation")).isEmpty(); i++) {
            driver.navigate().back();
            sleep(1000);
        }
        enterModule();
        assumeTrue(page.selectUnit("Breakfast"), "Could not reselect Breakfast");
        sleep(2500);
        assertTrue(!page.messageShownScrolled(a, 3) && !page.messageShownScrolled(b, 2),
                "Deleted messages must stay gone after reopening Catering");
    }

    @Test
    @Order(97)
    @DisplayName("M16 [WEB CAT-427/432] The reply bar quotes the message, hides the paperclip, and dismissing restores it")
    void replyBar() {
        HomeChatPage page = chatPage();
        CateringPage cat = catering();
        assumeTrue(!cat.isCatererMode(), "A caterer gets no Reply on its own messages — crew-side case");
        onBreakfast(page);
        String m = postMarker(page, "rbar");
        assumeTrue(m != null, "The marker did not send — skipping");
        assumeTrue(page.longPressMessage(m) && page.tapMenuOption("Reply"), "Reply not offered");
        boolean bar = cat.replyBarShown();
        String quote = cat.replyQuoteText();
        boolean clipHidden = !cat.paperclipShown();
        page.cancelReplyPreview();
        sleep(1000);
        boolean barGone = !cat.replyBarShown();
        boolean clipBack = cat.paperclipShown();
        System.out.println(">>> reply bar=" + bar + " quote='" + quote + "' clipHidden=" + clipHidden + " → gone=" + barGone + " clipBack=" + clipBack);
        assertTrue(bar && quote.contains(m), "Reply should show the bar quoting '" + m + "', quote was '" + quote + "'");
        assertTrue(clipHidden, "The paperclip should be hidden while replying");
        assertTrue(barGone && clipBack, "Dismissing the reply should remove the bar and bring the paperclip back");
    }

    @Test
    @Order(98)
    @DisplayName("M16 [WEB CAT-437] A whitespace-only reply sends nothing and keeps the quote")
    void whitespaceReplyRefused() {
        HomeChatPage page = chatPage();
        CateringPage cat = catering();
        assumeTrue(!cat.isCatererMode(), "Crew-side case");
        onBreakfast(page);
        String m = postMarker(page, "rspace");
        assumeTrue(m != null, "The marker did not send — skipping");
        assumeTrue(page.longPressMessage(m) && page.tapMenuOption("Reply"), "Reply not offered");
        cat.typeDraft("   ");
        try { driver.findElement(byAppId("ivSendMessage")).click(); } catch (Exception ignored) { }
        boolean refused = cat.snackShown("Please Type Message To Send", Duration.ofSeconds(4));
        boolean stillQuoted = cat.replyBarShown();
        page.cancelReplyPreview();
        cat.clearComposer();
        System.out.println(">>> whitespace reply refused=" + refused + " quote kept=" + stillQuoted);
        assertTrue(refused || !page.isSendButtonVisible(), "A whitespace-only reply must not send");
        assertTrue(stillQuoted, "The quote should stay after the refused reply");
        assertTrue(cat.commentCount("   ") == 0, "No blank comment should be added");
    }

    @Test
    @Order(100)
    @DisplayName("M17 [WEB CAT-422/442/447] A comment renders once; its menu has Edit/Delete (no Read By); edit, then No keeps it and Yes deletes it")
    void commentLifecycle() {
        HomeChatPage page = chatPage();
        CateringPage cat = catering();
        assumeTrue(!cat.isCatererMode(), "Crew-side case (replying to own message)");
        onBreakfast(page);
        String m = postMarker(page, "cparent");
        assumeTrue(m != null, "The marker did not send — skipping");
        assumeTrue(page.longPressMessage(m) && page.tapMenuOption("Reply"), "Reply not offered");
        String c = "ZL_AUTO-cat-c-" + stamp();
        page.sendMessage(c);
        sleep(5000);
        int once = cat.commentCount(c);
        assertEquals(1, once, "The comment '" + c + "' should render exactly once");
        assertTrue(cat.openCommentMenu(c), "The comment's arrow should open its menu");
        List<String> opts = page.menuOptions();
        System.out.println(">>> comment menu: " + opts);
        assertTrue(has(opts, "Edit") && has(opts, "Delete"), "An own comment should offer Edit and Delete: " + opts);
        assertTrue(opts.stream().noneMatch(o -> o.toLowerCase().contains("read by")), "No Read By on a Catering comment: " + opts);
        page.tapMenuOption("Edit");
        assumeTrue(page.isEditDialogOpen(Duration.ofSeconds(8)), "Comment edit dialog did not open");
        String ce = c + " edited";
        page.setEditDialogText(ce);
        page.saveEditDialog();
        assertTrue(cat.commentShown(ce) || page.messageShown(ce, Duration.ofSeconds(15)), "The comment should read '" + ce + "'");
        sleep(1500);
        assertTrue(cat.openCommentMenu(ce), "The edited comment's arrow should open its menu");
        page.tapMenuOption("Delete");
        assumeTrue(cat.snackShown("Are you sure you want to delete", Duration.ofSeconds(6)), "Delete confirmation did not appear");
        page.tapDialogLeft();
        sleep(1500);
        assertTrue(cat.commentShown(ce), "No must keep the comment");
        assertTrue(cat.openCommentMenu(ce), "The comment's arrow should open its menu again");
        page.tapMenuOption("Delete");
        assertTrue(confirmYesDialog(Duration.ofSeconds(8)), "Delete confirmation should appear");
        sleep(2500);
        assertTrue(!cat.commentShown(ce), "Yes should delete the comment");
    }

    @Test
    @Order(101)
    @DisplayName("M18 [WEB CAT-54/277] The emoji picker opens, appends an emoji at the end, it sends, and tapping the box closes it")
    void emojiPicker() {
        HomeChatPage page = chatPage();
        CateringPage cat = catering();
        onBreakfast(page);
        String base = "ZL_AUTO-cat-emo-" + stamp();
        assumeTrue(cat.typeDraft(base), "Could not type");
        assertTrue(cat.openEmojiPicker(), "ivEmoji should open the emoji picker");
        assertTrue(cat.tapFirstEmoji(), "The picker should offer emoji to tap");
        String text = cat.composerText();
        System.out.println(">>> composer after emoji: '" + text + "'");
        assertTrue(text.startsWith(base) && text.length() > base.length(), "The emoji should be appended at the end: '" + text + "'");
        cat.tapComposer();
        boolean closed = !cat.emojiPickerShown();
        page.tapSend();
        POSTED.add(base);
        assertTrue(page.waitMessageSent(base, Duration.ofSeconds(30)), "The message with the emoji should send");
        assertTrue(closed, "Tapping the composer should close the emoji picker");
    }

    @Test
    @Order(102)
    @DisplayName("M19 [WEB CAT-247] A long message is clipped with Read more, expands, and collapses with Read less")
    void readMoreReadLess() {
        HomeChatPage page = chatPage();
        CateringPage cat = catering();
        onBreakfast(page);
        String head = "ZL_AUTO-cat-long-" + stamp() + " ";
        String tail = "TAIL-" + stamp();
        String body = head + "word ".repeat(240) + tail;
        assumeTrue(cat.typeDraft(body), "Could not type");
        page.tapSend();
        POSTED.add(head.trim());
        assumeTrue(cat.rowContaining(head.trim(), Duration.ofSeconds(30)), "The long message did not post");
        sleep(2000);
        String collapsed = cat.rowText(head.trim());
        assertTrue(collapsed.contains("Read more") && !collapsed.contains(tail),
                "A 1,200+ character message should end in 'Read more' with the tail hidden; text ends '"
                        + collapsed.substring(Math.max(0, collapsed.length() - 40)) + "'");
        assertTrue(cat.toggleReadMore(head.trim(), t -> t.contains(tail)),
                "Tapping 'Read more' should expand to the full text (tail '" + tail + "')");
        String open = cat.rowText(head.trim());
        assertTrue(open.contains("Read less"), "The expanded text should offer 'Read less'");
        cat.bringRowEndOnScreen(head.trim());
        assertTrue(cat.toggleReadMore(head.trim(), t -> !t.contains(tail)), "Read less should collapse it again");
    }

    @Test
    @Order(103)
    @DisplayName("M20 [WEB CAT-257/262] An https link and an e-mail address are tappable links that leave the app")
    void linksAndEmail() {
        HomeChatPage page = chatPage();
        CateringPage cat = catering();
        onBreakfast(page);
        String url = "https://example.com/zl-auto-" + stamp();
        page.sendMessage(url);
        POSTED.add(url);
        assumeTrue(page.waitMessageSent(url, Duration.ofSeconds(30)), "The link did not send");
        cat.tapStartOfRow(url);
        String pkgLink = page.safeCurrentPackage();
        page.returnToChat(appPackage);
        if (!page.isInputBarVisible()) enterModule();
        onBreakfast(page);
        String mail = "zl.auto." + stamp() + "@example.com";
        page.sendMessage(mail);
        POSTED.add(mail);
        assumeTrue(page.waitMessageSent(mail, Duration.ofSeconds(30)), "The e-mail address did not send");
        cat.tapStartOfRow(mail);
        String pkgMail = page.safeCurrentPackage();
        page.returnToChat(appPackage);
        if (!page.isInputBarVisible()) enterModule();
        System.out.println(">>> link opened " + pkgLink + " | mail opened " + pkgMail);
        assertTrue(pkgLink != null && !pkgLink.equals(appPackage), "Tapping the https link should open a browser");
        // The Linkify mailto link needs an app that handles mailto:; the test emulators have none, and the
        // app then does nothing. Only a device with a mail app can show this half.
        assumeTrue(pkgMail != null && !pkgMail.equals(appPackage),
                "Tapping the e-mail address left the app in front — no mailto: handler on this device");
    }

    @Test
    @Order(104)
    @DisplayName("M21 [WEB CAT-252/272] HTML stays literal text; special characters and markdown round-trip exactly")
    void literalRendering() {
        HomeChatPage page = chatPage();
        CateringPage cat = catering();
        onBreakfast(page);
        String html = "<b>bold</b> ZL_AUTO-cat-html-" + stamp();
        page.sendMessage(html);
        POSTED.add(html);
        assertTrue(page.waitMessageSent(html, Duration.ofSeconds(30)), "The HTML text should send");
        assertTrue(cat.exactRowCount(html) == 1, "HTML must render as literal text: '" + cat.rowText("ZL_AUTO-cat-html") + "'");
        String special = "مرحبا Café ©®™ *bold* _it_ 12345 ZL_AUTO-cat-sp-" + stamp();
        page.sendMessage(special);
        POSTED.add(special);
        assertTrue(page.waitMessageSent(special, Duration.ofSeconds(30)), "The special-character text should send");
        assertTrue(cat.exactRowCount(special) == 1, "Special characters and markdown should round-trip exactly: '"
                + cat.rowText("ZL_AUTO-cat-sp") + "'");
    }

    @Test
    @Order(105)
    @DisplayName("M22 [WEB CAT-114/24/537] The feed opens on the newest row; scrolling up shows the arrow; the arrow returns to the newest")
    void scrollArrow() {
        HomeChatPage page = chatPage();
        CateringPage cat = catering();
        onBreakfast(page);
        String m = postMarker(page, "newest");
        assumeTrue(m != null, "The marker did not send — skipping");
        assumeTrue(page.selectUnit("Lunch"), "Could not select Lunch");
        sleep(1200);
        assumeTrue(page.selectUnit("Breakfast"), "Could not return to Breakfast");
        sleep(2500);
        boolean newestShown = cat.rowContaining(m, Duration.ofSeconds(4));
        boolean arrowAtOpen = cat.arrowDownShown();
        cat.swipeOlder(5);
        boolean arrowAfterScroll = cat.arrowDownShown();
        boolean tapped = arrowAfterScroll && cat.tapArrowDown();
        boolean backAtNewest = cat.rowContaining(m, Duration.ofSeconds(5));
        sleep(1000);
        boolean arrowGone = !cat.arrowDownShown();
        System.out.println(">>> newest shown=" + newestShown + " arrowAtOpen=" + arrowAtOpen + " arrowAfterScroll="
                + arrowAfterScroll + " tapped=" + tapped + " backAtNewest=" + backAtNewest + " arrowGone=" + arrowGone);
        assertTrue(newestShown && !arrowAtOpen, "A meal tab should open on its newest row with no scroll-to-bottom arrow");
        assertTrue(arrowAfterScroll, "Scrolling up should show the scroll-to-bottom arrow (arrowDownCard)");
        assertTrue(backAtNewest && arrowGone, "The arrow should return to the newest row and hide");
    }

    @Test
    @Order(106)
    @DisplayName("M22 [WEB CAT-119] Scrolling to the top loads older pages (more than one 50-row page seen)")
    void olderPagesLoad() {
        HomeChatPage page = chatPage();
        CateringPage cat = catering();
        onBreakfast(page);
        java.util.Set<String> seen = new java.util.LinkedHashSet<>();
        int still = 0;
        for (int i = 0; i < 40 && seen.size() <= 55 && still < 4; i++) {
            int before = seen.size();
            cat.visibleRows().forEach(r -> seen.add(r.text));
            still = seen.size() == before ? still + 1 : 0;
            cat.swipeOlder(1);
        }
        System.out.println(">>> distinct rows seen scrolling up: " + seen.size() + (still >= 4 ? " (top reached)" : ""));
        assumeTrue(seen.size() > 50 || still < 4,
                "Breakfast holds only " + seen.size() + " text rows: there is no second 50-row page to load");
        assertTrue(seen.size() > 50, "Scrolling up should load older pages beyond the first 50 rows (saw " + seen.size() + ")");
        cat.tapArrowDown();
    }

    @Test
    @Order(107)
    @DisplayName("M33 [WEB CAT-49/99] Typing '@' opens no mention picker; the options menu closes on Back")
    void noMentionAndMenuCloses() {
        HomeChatPage page = chatPage();
        CateringPage cat = catering();
        onBreakfast(page);
        assumeTrue(cat.typeDraft("@a"), "Could not type");
        sleep(1500);
        boolean picker = !driver.findElements(io.appium.java_client.AppiumBy.androidUIAutomator(
                "new UiSelector().resourceIdMatches(\"(?i).*:id/.*(mention|tagUser|userTag|suggest).*\")")).isEmpty();
        cat.clearComposer();
        assertTrue(!picker, "Catering has no mentions: typing '@' must not open a picker");
        String m = postMarker(page, "menuback");
        assumeTrue(m != null, "The marker did not send — skipping");
        assumeTrue(page.longPressMessage(m), "Long-press did not open the menu");
        driver.navigate().back();
        sleep(1000);
        assertTrue(!page.isMenuOpen(Duration.ofSeconds(1)), "Back should close the options menu");
        assertTrue(page.isInputBarVisible(), "Closing the menu should leave the chat usable");
    }

    @Test
    @Order(108)
    @DisplayName("M33 [WEB CAT-297] A fast double tap on Send posts the message once")
    void doubleSendOnce() {
        HomeChatPage page = chatPage();
        CateringPage cat = catering();
        onBreakfast(page);
        String m = "ZL_AUTO-cat-dbl-" + stamp();
        POSTED.add(m);
        assumeTrue(cat.typeDraft(m), "Could not type");
        cat.doubleTapSend();
        assumeTrue(page.waitMessageSent(m, Duration.ofSeconds(30)), "The message did not send");
        sleep(3000);
        assertEquals(1, cat.exactRowCount(m), "A double tap on Send must post the message once");
    }

    @Test
    @Order(109)
    @DisplayName("M33 [WEB CAT-462/467] Forward lists Home / Info / Confidential Info / Members / Group / Email; Home lists units without Calendar or Call Sheet")
    void forwardDestinations() {
        HomeChatPage page = chatPage();
        CateringPage cat = catering();
        onBreakfast(page);
        String m = postMarker(page, "fwd");
        assumeTrue(m != null, "The marker did not send — skipping");
        assertTrue(page.forwardOpensPage(m), "Forward (after 'Are you sure you want to forward?') should open ForwardPage");
        try {
            List<String> top = cat.forwardListLabels();
            System.out.println(">>> forward destinations: " + top);
            List<String> missing = new java.util.ArrayList<>();
            for (String want : List.of("Home", "Info", "Confidential Info", "Members", "Group", "Email"))
                if (top.stream().noneMatch(t -> t.equalsIgnoreCase(want))) missing.add(want);
            assertTrue(missing.isEmpty(), "ForwardPage should list " + missing + ". Listed: " + top);
            assertTrue(top.stream().noneMatch(t -> t.equalsIgnoreCase("Catering")), "Catering is not a forward destination: " + top);
            assertTrue(cat.tapForwardOption("Home"), "Home should be tappable");
            List<String> units = List.of();
            for (int i = 0; i < 10; i++) {
                units = cat.forwardListLabels();
                if (!units.isEmpty() && !units.equals(top)) break;
                sleep(1000);
            }
            if (units.equals(top)) units = List.of();
            System.out.println(">>> Home units offered for a text forward: " + units);
            assertTrue(!units.isEmpty(), "Home should list the Home units");
            assertTrue(units.stream().noneMatch(u -> u.toLowerCase().contains("calendar")), "Calendar must not be offered: " + units);
            assertTrue(units.stream().noneMatch(u -> u.toLowerCase().contains("call sheet")),
                    "Call Sheet is offered only for documents, not for a text: " + units);
        } finally {
            page.leaveOverlayScreen();   // never forwards
            if (!page.isInputBarVisible()) enterModule();
        }
    }

    @Test
    @Order(110)
    @DisplayName("M33 [WEB CAT-532] A posted row shows my name and a 'MMM dd, yyyy hh:mm a' time")
    void rowHeaderAndTime() {
        HomeChatPage page = chatPage();
        CateringPage cat = catering();
        onBreakfast(page);
        String m = postMarker(page, "hdr");
        assumeTrue(m != null, "The marker did not send — skipping");
        String sender = cat.senderOfRow(m);
        String time = cat.rowTimeText(m);
        System.out.println(">>> row sender '" + sender + "' time '" + time + "'");
        assertTrue(!sender.isBlank(), "The row should show the sender's name");
        assertTrue(time.matches("(?i)[A-Z]{3} \\d{2}, \\d{4} \\d{2}:\\d{2} [AP]M"), "The time should read like 'NOV 23, 2026 10:15 PM' (MMM dd, yyyy hh:mm a), was '" + time + "'");
    }

    @Test
    @Order(111)
    @DisplayName("[WEB CAT-287] A posted message is still there after leaving Catering and reopening it")
    void persistsAcrossReopen() {
        HomeChatPage page = chatPage();
        onBreakfast(page);
        String m = postMarker(page, "reopen");
        assumeTrue(m != null, "The marker did not send — skipping");
        for (int i = 0; i < 3 && driver.findElements(byAppId("bottomNavigation")).isEmpty(); i++) {
            driver.navigate().back();
            sleep(1000);
        }
        enterModule();
        assumeTrue(page.selectUnit("Breakfast"), "Could not reselect Breakfast");
        sleep(2500);
        assertTrue(page.messageShownScrolled(m, 5), "The message should still be in Breakfast after a reopen");
    }

    @Test
    @Order(112)
    @DisplayName("[WEB CAT-377] An edit updates the text and marks the row 'Edited'")
    void editMarksEdited() {
        HomeChatPage page = chatPage();
        CateringPage cat = catering();
        onBreakfast(page);
        String m = postMarker(page, "edtag");
        assumeTrue(m != null, "The marker did not send — skipping");
        assumeTrue(page.longPressMessage(m) && page.tapMenuOption("Edit"), "Edit not offered");
        assumeTrue(page.isEditDialogOpen(Duration.ofSeconds(8)), "Edit dialog did not open");
        String e = m + "-E";
        page.setEditDialogText(e);
        page.saveEditDialog();
        POSTED.add(e);
        assertTrue(page.waitMessageSent(e, Duration.ofSeconds(30)), "The edit should save");
        sleep(1500);
        String time = cat.rowTimeText(e);
        assertTrue(time.startsWith("Edited"), "An edited row should carry the 'Edited' tag, time reads '" + time + "'");
    }

    @Test
    @Order(113)
    @DisplayName("[WEB CAT-205/210] A crew member has no '+' (create unit) and no recipient chip")
    void crewHasNoCatererControls() {
        CateringPage cat = catering();
        assumeTrue(!cat.isCatererMode(), "Signed in as the caterer — crew-side check");
        assertTrue(!cat.isCreateUnitVisible(), "A non-caterer must not see the create-unit '+'");
        assertTrue(!cat.isRecipientHintShown(), "A non-caterer has no caterer instructions bar");
    }

    @Test
    @Order(114)
    @DisplayName("[WEB CAT-601] CATERER — Select Users excludes my own name, a nonsense search shows the empty view, and it reopens")
    void selectUsersModal() {
        CateringPage cat = catering();
        HomeChatPage page = chatPage();
        assumeTrue(cat.isCatererMode(), "Not a caterer — skipping");
        onBreakfast(page);
        String m = postMarker(page, "selu");
        String me = m == null ? "" : cat.senderOfRow(m);
        assumeTrue(cat.openRecipientPicker() && cat.isRecipientDialogOpen(Duration.ofSeconds(6)), "Recipient dialog did not open");
        assumeTrue(cat.chooseSelectUser(), "Could not choose Select User");
        try {
            assertTrue(cat.usersListOpen(Duration.ofSeconds(10)), "Select User should open the 'Select Users' list");
            List<String> names = cat.usersListNames();
            System.out.println(">>> Select Users: " + names + " (me='" + me + "')");
            assumeTrue(!me.isBlank(), "Could not read my own name");
            assertTrue(names.stream().noneMatch(n -> n.equalsIgnoreCase(me)), "My own name should not be offered: " + names);
            cat.searchUsersList("zzqq-" + stamp());
            assertTrue(cat.usersListEmptyShown(), "A nonsense search should show the empty view");
            cat.searchUsersList("");
        } finally {
            driver.navigate().back();
            sleep(1500);
        }
        assumeTrue(cat.openRecipientPicker() && cat.isRecipientDialogOpen(Duration.ofSeconds(6)), "Recipient dialog did not reopen");
        assumeTrue(cat.chooseSelectUser(), "Could not choose Select User again");
        boolean reopened = cat.usersListOpen(Duration.ofSeconds(10));
        driver.navigate().back();
        sleep(1200);
        if (cat.openRecipientPicker() && cat.isRecipientDialogOpen(Duration.ofSeconds(6))) cat.chooseAllUsers();
        assertTrue(reopened, "The Select Users list should open again");
    }

    /** After leaving to Settings, the next case's @BeforeEach must reopen the module. */
    private void moduleReopenNeeded() {
        try { enterModule(); } catch (Exception ignored) { }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
