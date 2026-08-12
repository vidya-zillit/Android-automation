package com.zillit.automation.tests;

import com.zillit.automation.core.TestOrder;

import com.zillit.automation.pages.CateringPage;
import com.zillit.automation.pages.HomeChatPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

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
class CateringTest extends ToolChatBaseTest {

    private static final String[] MEALS = {"Breakfast", "Lunch", "Dinner"};

    @Override protected String moduleName() { return "Catering"; }

    /**
     * Catering's attachment picker is RESTRICTED compared with the other chat
     * modules — live it offers only Photo / Image Gallery / Audio / Document
     * (no Video, Video Gallery, Location or Contact).
     */
    @Override protected int minAttachmentOptions() { return 4; }

    /**
     * A CATERER has no keyboard until a recipient is chosen, so make the chat
     * postable before every case — otherwise all the inherited chat cases would
     * skip when this account is in the catering department. A regular user is
     * unaffected (their input bar is always there).
     */
    @Override protected void afterModuleReady() {
        try { new CateringPage(driver).ensurePostable(); } catch (Exception ignored) { }
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
        boolean hasRecipient = !cat.selectedRecipient().isBlank();
        assertTrue(hasRecipient || !page.isInputBarVisible(),
                "A caterer must not be able to type before choosing a recipient");
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
        boolean directed = page.textShown("To : ", Duration.ofSeconds(6))
                && !page.textShown("To : All Caterer", Duration.ofSeconds(2));
        System.out.println(">>> directed message posted; audience label present=" + directed);
        assertTrue(directed, "A directed caterer message should be labelled with the recipient");
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
        // A catering unit CANNOT be deleted from the app, so creating a uniquely
        // named unit on every run would slowly clutter the project. Reuse the QA
        // unit when it already exists; only create it the first time.
        String existing = before.stream()
                .filter(u -> u.toLowerCase().startsWith(QA_UNIT.toLowerCase()))
                .findFirst().orElse(null);
        if (existing != null) {
            createdUnit = existing;
            System.out.println(">>> reusing existing QA unit '" + existing
                    + "' (units cannot be deleted in-app, so we don't create another)");
            assertTrue(page.visibleUnitNames().contains(existing),
                    "The QA catering unit should be listed in the tab strip");
            return;
        }
        assumeTrue(cat.openCreateUnit() && cat.isCreateUnitScreenOpen(Duration.ofSeconds(10)),
                "Create-unit screen did not open");
        cat.setUnitName(QA_UNIT);
        assumeTrue(cat.submitUnit(), "Submit not tappable");
        sleep(3000);
        List<String> after = page.visibleUnitNames();
        System.out.println(">>> units " + before + " -> " + after + " (new: '" + QA_UNIT + "')");
        boolean created = after.stream().anyMatch(u -> u.equalsIgnoreCase(QA_UNIT))
                || page.textShown(QA_UNIT, Duration.ofSeconds(8));
        assertTrue(created, "The new unit '" + QA_UNIT + "' should appear in the meal tab strip");
        createdUnit = after.stream().filter(u -> u.equalsIgnoreCase(QA_UNIT)).findFirst().orElse(QA_UNIT);
        screenshot("catering-unit-created");
    }

    /** Fixed name so repeated runs REUSE one QA unit instead of piling up new ones. */
    private static final String QA_UNIT = "QA Snack";

    /** Set by {@link #createNewMealUnit()} so the next case can post in it. */
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

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
