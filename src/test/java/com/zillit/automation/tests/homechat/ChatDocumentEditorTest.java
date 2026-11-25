package com.zillit.automation.tests.homechat;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.Config;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.callsheet.UploadModePage;
import com.zillit.automation.pages.cc.CcChatThreadPage;
import com.zillit.automation.pages.cc.CcLandingPage;
import com.zillit.automation.pages.common.BottomNavPage;
import com.zillit.automation.pages.common.ChatDocumentEditorPage;
import com.zillit.automation.pages.common.ChatDocumentEditorPage.Outcome;
import com.zillit.automation.pages.common.MediaPreviewPage;
import com.zillit.automation.pages.common.ProjectListPage;
import com.zillit.automation.pages.common.SystemFilePickerPage;
import com.zillit.automation.pages.common.ToolsPage;
import com.zillit.automation.pages.homechat.HomeChatPage;
import com.zillit.automation.pages.tools.CateringPage;
import io.appium.java_client.AppiumBy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.WebDriverException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Opening a DOCUMENT message in any chat opens the app's own editor in VIEW (read-only) mode.
 *
 * <p>Per case: send {@code zl_drive_doc.docx} (or {@code zl_drive_book.xlsx}) picked by its FULL
 * file name through the system picker, with a {@code ZL_AUTO} caption; tap the bubble on its
 * real click target; then hard-assert
 * <ol>
 *   <li>{@code OnlyOfficeEditorActivity} comes to the front (not another app),</li>
 *   <li>the page loads ("Loading editor…" goes, no "Failed to load editor"),</li>
 *   <li>the page's "View only." notice is shown and the app's own "Edit" button is NOT,</li>
 *   <li>ONE Back returns to the same chat with no "Saving…" / leave / notify dialog,</li>
 * </ol>
 * and finally delete the message it sent. Negative control: a PDF never opens the editor.
 * Cases and results: {@code CHAT_DOCUMENT_EDITOR_TESTCASES.md}. Behaviour source: Zillit-Android
 * {@code origin/development}, {@code ChatAttachmentEditor.openChatAttachment}.
 *
 * <p>Runs on project {@code ZL-Android-Automation} (override: {@code zillit.chatDocProject}); it
 * switches to it when the device is on another project.
 */
@Order(TestOrder.CHAT_DOCUMENT_EDITOR)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ChatDocumentEditorTest extends BaseTest {

    private static final String DOCX = "zl_drive_doc.docx";
    private static final String XLSX = "zl_drive_book.xlsx";
    private static final String PDF = "zl_test_doc.pdf";
    /**
     * The runner's copy of the same PDF in Download/. The picker reopens in the folder it was
     * last in (Download, after the Office fixtures), and its helper does not always find its
     * way to Documents/ from there.
     */
    private static final String PDF_IN_DOWNLOAD = "zl_drive_doc.pdf";

    private static final Duration OUTCOME = Duration.ofSeconds(30);
    private static final Duration LOAD = Duration.ofSeconds(60);

    private final String project = Config.has("zillit.chatDocProject")
            ? Config.get("zillit.chatDocProject")
            : Config.has("zillit.runProject") ? Config.get("zillit.runProject") : "ZL-Android-Automation";

    /** null = not probed yet; see {@link ChatDocumentEditorPage#buildHasChatEditor}. */
    private Boolean buildHasFeature;
    private boolean probed;

    @Override protected boolean relaunchBetweenTests() { return false; }
    @Override protected boolean requiresLoggedOutStart() { return false; }

    // ═════════════════════════════ the cases ═════════════════════════════

    @Test
    @Order(1)
    @DisplayName("CDE-01 Bulletin: a .docx message opens the editor read-only; Back returns to the chat")
    void bulletinDocx() {
        requireFeatureBuild();
        reachHomeUnit("Bulletin");
        unitChatCase("Bulletin", DOCX, false);
    }

    @Test
    @Order(2)
    @DisplayName("CDE-02 Bulletin (negative control): a .pdf message opens ANOTHER app, never the editor")
    void bulletinPdfNegative() {
        reachHomeUnit("Bulletin");
        HomeChatPage chat = new HomeChatPage(driver);
        need(chat.isInputBarVisible(), "No posting rights in Bulletin — skipping");
        String caption = caption("bulletin-pdf");
        boolean sent = sendUnitDoc(chat, PDF, caption, false);
        if (!sent) {
            backToUnitChat();
            sent = sendUnitDoc(chat, PDF_IN_DOWNLOAD, caption, false);
        }
        need(sent, "Could not send " + PDF + " (nor its Download/ copy " + PDF_IN_DOWNLOAD + ")");
        ChatDocumentEditorPage ed = new ChatDocumentEditorPage(driver);
        try {
            ChatDocumentEditorPage.pause(ChatDocumentEditorPage.TAP_GUARD.toMillis());
            assertTrue(chat.tapNewestDocBubble(), "No document bubble to tap");
            Outcome o = ed.waitForOutcome(OUTCOME);
            System.out.println(">>> CDE Bulletin PDF: outcome=" + o + " pkg=" + ed.lastPackage()
                    + " activity=" + ed.lastActivity());
            screenshot("cde-bulletin-pdf");
            if (o == Outcome.EDITOR) driver.navigate().back();
            assertNotEquals(Outcome.EDITOR, o, "A PDF is not an editor type: it must not open "
                    + "OnlyOfficeEditorActivity");
            assertTrue(o == Outcome.EXTERNAL_APP || o == Outcome.NO_APP_TOAST,
                    "Tapping a PDF should hand it to another app (or say no app can open it); "
                            + "nothing happened within " + OUTCOME.toSeconds() + "s");
        } finally {
            ed.leaveExternalApp();
            backToUnitChat();
            deleteNewestUnitDoc(caption);
        }
    }

    @Test
    @Order(3)
    @DisplayName("CDE-03 Call Sheet: a .xlsx message opens the editor read-only; Back returns to the chat")
    void callSheetXlsx() {
        requireFeatureBuild();
        reachHomeUnit("Call Sheet");
        unitChatCase("Call Sheet", XLSX, true);
    }

    @Test
    @Order(4)
    @DisplayName("CDE-04 C&C 1:1: a .docx message opens the editor read-only; Back returns to the thread")
    void ccOneToOne() {
        requireFeatureBuild();
        ccCase("C&C 1:1", this::reachCcOneToOne, DOCX);
    }

    @Test
    @Order(5)
    @DisplayName("CDE-05 C&C group: a .xlsx message opens the editor read-only; Back returns to the thread")
    void ccGroup() {
        requireFeatureBuild();
        ccCase("C&C group", this::reachCcGroup, XLSX);
    }

    @Test
    @Order(6)
    @DisplayName("CDE-06 Info: a .docx message opens the editor read-only; Back returns to the chat")
    void info() {
        requireFeatureBuild();
        reachTool("Info", true);
        unitChatCase("Info", DOCX, false);
    }

    @Test
    @Order(7)
    @DisplayName("CDE-07 Confidential Info: a .xlsx message opens the editor read-only; Back returns to the chat")
    void confidentialInfo() {
        requireFeatureBuild();
        reachTool("Confidential Info", true);
        unitChatCase("Confidential Info", XLSX, false);
    }

    @Test
    @Order(8)
    @DisplayName("CDE-08 Catering: a .docx message opens the editor read-only; Back returns to the chat")
    void catering() {
        requireFeatureBuild();
        reachTool("Catering", true);
        try { new CateringPage(driver).ensurePostable(); } catch (WebDriverException ignored) { }
        unitChatCase("Catering", DOCX, false);
    }

    @Test
    @Order(9)
    @DisplayName("CDE-09 Production Report chat: a .docx message opens the editor read-only; Back returns")
    void productionReportChat() {
        requireFeatureBuild();
        // The chat is its own tool since the "chat split" (Constants.PRODUCTION_REPORT_CHAT_TOOL);
        // on this project its label is not translated and shows the raw key.
        reachToolAny(List.of("Production Report Chat", "production_report_tool_chat_label"));
        need(activityContains("ProductionReportChat"),
                "The Production Report chat tool opened " + activity() + ", not ProductionReportChatActivity");
        unitChatCase("Production Report chat", DOCX, true);
    }

    @Test
    @Order(10)
    @DisplayName("CDE-10 Accounts chat: a .docx message opens the editor read-only; Back returns to the chat")
    void accounts() {
        requireFeatureBuild();
        reachTool("Accounts", false);
        need(activityContains("AccountsActivity"),
                "Accounts opened " + activity() + ", not the Accounts chat (AccountsActivity)");
        unitChatCase("Accounts", DOCX, false);
    }

    @Test
    @Order(11)
    @DisplayName("CDE-11 Casting (Main Cast) chat: a .docx message opens the editor read-only")
    void castingMain() {
        requireFeatureBuild();
        reachToolAny(List.of("Casting (Main Cast)"));
        if (!activityContains("OtherChatsActivity")) { tapIfPresent("chatIv", Duration.ofSeconds(6)); sleep(2500); }
        need(activityContains("OtherChatsActivity"),
                "Casting (Main Cast) opened " + activity() + ", not a chat (OtherChatsActivity)");
        unitChatCase("Casting (Main Cast)", DOCX, false);
    }

    @Test
    @Order(12)
    @DisplayName("CDE-12 Costume/Wardrobe (Main Cast) chat: a .docx message opens the editor read-only")
    void wardrobeMain() {
        requireFeatureBuild();
        reachToolAny(List.of("Costume (Main Cast)", "Wardrobe (Main Cast)", "Wardrobe"));
        if (!activityContains("OtherChatsActivity")) { tapIfPresent("chatIv", Duration.ofSeconds(6)); sleep(2500); }
        need(activityContains("OtherChatsActivity"),
                "The Costume/Wardrobe tool opened " + activity() + ", not a chat (OtherChatsActivity)");
        unitChatCase("Costume (Main Cast)", DOCX, false);
    }

    @Test
    @Order(13)
    @DisplayName("CDE-13 Location chat — skipped: the Location tool is deferred in this automation")
    void location() {
        need(false, "Location tool is deferred in this automation on purpose (its map/overlay "
                + "cases skip by design); its chat (unit keys location / location_unit) is not driven");
    }

    @Test
    @Order(14)
    @DisplayName("CDE-14 Forms & Signature discussion chat: a .docx message opens the editor read-only")
    void formsDiscussion() {
        requireFeatureBuild();
        reachToolAny(List.of("Documents & Signature", "Forms & Signature"));
        // Dashboard → "Standard Documents" (CommonListingView) → "Chat with Users" (chatIcon)
        // → FormDiscussionChatPage, which needs a recipient before the input bar appears.
        need(tapText("Standard Documents", Duration.ofSeconds(8)),
                "No 'Standard Documents' row on " + activity());
        sleep(2500);
        need(tapIfPresent("chatIcon", Duration.ofSeconds(8)),
                "No 'Chat with Users' (chatIcon) on " + activity());
        sleep(2500);
        need(activityContains("FormDiscussionChatPage"),
                "The chat icon opened " + activity() + ", not FormDiscussionChatPage");
        if (!present("etMessage")) {
            need(tapIfPresent("userName", Duration.ofSeconds(5)), "No 'Select User' control");
            sleep(2500);
            need(tapIfPresent("nameTv", Duration.ofSeconds(8)), "No user to pick in SELECT USERS");
            need(tapIfPresent("buttonBtn", Duration.ofSeconds(5)), "No Submit on SELECT USERS");
            sleep(3000);
        }
        unitChatCase("Forms discussion", DOCX, false);
    }

    @Test
    @Order(15)
    @DisplayName("CDE-15 [expected-fail] Form/contract chat window (FormChatPage) opens a .docx in the editor "
            + "— merge 59332b63b put it back on openFileFromExternalFromUrl")
    void formChatWindowRegression() {
        need(false, "FormChatPage is reached only from a sent form/contract that has a signer "
                + "discussion (DocumentViewIngPage / FormFragment 'Reply & start discussion'); "
                + "ZL-Android-Automation has none for this account, and creating one sends a real "
                + "signature request. Source check: origin/development FormChatPage.kt:1127 calls "
                + "openFileFromExternalFromUrl (0 openChatAttachment calls; 59332b63b^2 had 2).");
    }

    // ═════════════════════════════ shared case bodies ═════════════════════════════

    /** Unit-chat case (HomeChatAdapter-style screens: Home units, Tools chats). */
    private void unitChatCase(String module, String fixture, boolean callSheet) {
        HomeChatPage chat = new HomeChatPage(driver);
        need(chat.isInputBarVisible(), "No posting rights in " + module + " — skipping");
        need(present("ivAttachment"), module + " chat has no attachment button (text-only chat) "
                + "— a document cannot be sent here");
        String caption = caption(slug(module));
        need(sendUnitDoc(chat, fixture, caption, callSheet),
                "Could not send " + fixture + " in " + module + ": " + lastSendProblem);
        String activityBefore = activity();
        ChatDocumentEditorPage ed = new ChatDocumentEditorPage(driver);
        try {
            ChatDocumentEditorPage.pause(ChatDocumentEditorPage.TAP_GUARD.toMillis());
            ed.markLogcat();
            assertTrue(chat.tapNewestDocBubble(), "No document bubble to tap in " + module);
            assertEditorReadOnlyAndBack(module, fixture, ed,
                    () -> activity().equals(activityBefore) && present("etMessage"));
        } finally {
            if (!ed.isEditorActivity()) ed.leaveExternalApp();
            else driver.navigate().back();
            backToUnitChat();
            deleteNewestUnitDoc(caption);
        }
    }

    /**
     * C&amp;C case (ChatAndGroupAdapter: the click is on srcImage).
     *
     * <p>When the fresh message does not open the editor, the thread is left and re-entered and
     * the same card tapped again, so the report can say whether it is the just-sent message
     * (no stored {@code _id} yet → no config call at all) or Drive refusing the config.
     */
    private void ccCase(String module, java.util.function.Supplier<CcChatThreadPage> reach, String fixture) {
        CcChatThreadPage t = reach.get();
        need(t != null && t.isLoaded(Duration.ofSeconds(15)), module + " thread did not open");
        String caption = caption(slug(module));
        need(sendCcDoc(t, fixture, caption), "Could not send " + fixture + " in " + module);
        ChatDocumentEditorPage ed = new ChatDocumentEditorPage(driver);
        BooleanSupplier inThread = () -> activityContains("ChatAndGroupPage") && present("etMessage");
        try {
            ChatDocumentEditorPage.pause(ChatDocumentEditorPage.TAP_GUARD.toMillis());
            ed.markLogcat();
            assertTrue(ed.tapNewestCcDocument(), "No document card (srcImage) to tap in " + module);
            try {
                assertEditorReadOnlyAndBack(module, fixture, ed, inThread);
            } catch (AssertionError first) {
                if (ed.isEditorActivity() || ed.lastActivity().contains(ChatDocumentEditorPage.EDITOR_ACTIVITY)) {
                    throw first;
                }
                // Second look: leave, re-enter (the message now has its stored _id), tap again.
                ed.leaveExternalApp();
                String second = "not retried";
                try {
                    t.back();
                    sleep(1500);
                    CcChatThreadPage again = reach.get();
                    if (again != null && again.isLoaded(Duration.ofSeconds(15))) {
                        ChatDocumentEditorPage.pause(ChatDocumentEditorPage.TAP_GUARD.toMillis());
                        ed.markLogcat();
                        if (ed.tapNewestCcDocument()) {
                            Outcome o2 = ed.waitForOutcome(OUTCOME);
                            String server2 = o2 == Outcome.EDITOR ? "" : ed.editorConfigFailure();
                            second = o2 + (o2 == Outcome.EXTERNAL_APP ? " (" + ed.lastPackage() + ")" : "")
                                    + (server2.isEmpty() ? "" : ", editor config answer '" + server2 + "'");
                            if (o2 == Outcome.EDITOR) driver.navigate().back();
                            else ed.leaveExternalApp();
                        }
                    }
                } catch (RuntimeException e) {
                    second = "retry failed: " + e.getClass().getSimpleName();
                }
                System.out.println(">>> CDE " + module + " after re-entering the thread: " + second);
                throw new AssertionError(first.getMessage() + " | Re-entering the thread and tapping the "
                        + "same card again: " + second, first);
            }
        } finally {
            if (!ed.isEditorActivity()) ed.leaveExternalApp();
            else driver.navigate().back();
            CcChatThreadPage now = new CcChatThreadPage(driver);
            boolean deleted = now.isLoaded(Duration.ofSeconds(5)) && now.deleteMessage(caption);
            System.out.println(">>> CDE " + module + " cleanup: deleted=" + deleted);
        }
    }

    /** The four hard checks, shared by every positive case. */
    private void assertEditorReadOnlyAndBack(String module, String fixture,
                                             ChatDocumentEditorPage ed, BooleanSupplier backInChat) {
        Outcome o = ed.waitForOutcome(OUTCOME);
        System.out.println(">>> CDE " + module + ": outcome=" + o + " pkg=" + ed.lastPackage()
                + " activity=" + ed.lastActivity());
        if (o != Outcome.EDITOR) {
            screenshot("cde-" + slug(module) + "-not-editor");
            String server = ed.editorConfigFailure();
            System.out.println(">>> CDE " + module + ": editor config answer='" + server + "'");
            String why = switch (o) {
                case EXTERNAL_APP -> "it opened in ANOTHER app (" + ed.lastPackage() + "/" + ed.lastActivity()
                        + "). For an Office file that is openChatAttachment's silent fallback: "
                        + (server.isEmpty()
                            ? "no /editor/…/config call was logged, so this chat is not wired to the editor"
                            : "Drive refused the editor config with '" + server + "'");
                case NO_APP_TOAST -> "the app fell back to another-app open and found no app for it";
                default -> "nothing opened within " + OUTCOME.toSeconds() + "s";
            };
            assertEquals(Outcome.EDITOR, o, module + ": tapping " + fixture
                    + " should open the app's editor (OnlyOfficeEditorActivity) — " + why);
        }
        boolean loaded = ed.waitLoaded(LOAD);
        String failure = ed.failureText();
        boolean notice = loaded && ed.viewOnlyNoticeShown(Duration.ofSeconds(15));
        boolean editBtn = ed.editButtonShown();
        List<String> tools = ed.editToolsShown();
        boolean name = ed.fileNameShown(fixture.substring(0, fixture.lastIndexOf('.')));
        System.out.println(">>> CDE " + module + ": status='" + ed.statusText() + "' loaded=" + loaded + " failure='" + failure
                + "' viewOnlyNotice=" + notice + " appEditButton=" + editBtn + " editTools=" + tools
                + " fileNameShown=" + name + " | " + ed.describe());
        screenshot("cde-" + slug(module) + "-editor");
        assertTrue(failure.isEmpty(), module + ": the editor showed an error: '" + failure + "'");
        assertTrue(loaded, module + ": the editor page did not finish loading within "
                + LOAD.toSeconds() + "s (\"Loading editor…\" still up, or no WebView)");
        assertTrue(!editBtn, module + ": the app's own \"Edit\" button is shown — a chat file must "
                + "never offer a switch to edit mode (canSwitchToEdit = source == null && …)");
        assertTrue(tools.isEmpty(), module + ": editing tools are exposed in view mode: " + tools);
        assertTrue(notice, module + ": the editor page should say \"View only\" (status-text / notice); "
                + "it says '" + ed.statusText() + "' — \"Live\" means the page loaded in EDIT mode");
        String back = ed.backToChat(backInChat, Duration.ofSeconds(8));
        System.out.println(">>> CDE " + module + ": back='" + back + "'");
        assertTrue(back.isEmpty(), module + ": " + back);
    }

    // ═════════════════════════════ sending ═════════════════════════════

    private boolean sendUnitDoc(HomeChatPage chat, String fixture, String caption, boolean callSheet) {
        lastSendProblem = "";
        int docsBefore = chat.docBubbleCountByBadge();
        try {
            chat.tapAttachment();
        } catch (WebDriverException e) {
            lastSendProblem = "attachment button not tappable";
            return false;
        }
        String snack = snackbarText(Duration.ofMillis(1500));
        if (!snack.isEmpty()) {
            // e.g. Catering / Accounts: "You cannot post any message until a person joins the
            // … department" — the chat refuses to post at all, before any picker.
            lastSendProblem = "the chat refused to post: \"" + snack + "\"";
            return false;
        }
        if (!chat.isAttachmentPickerVisible()) {
            lastSendProblem = "attachment sheet did not open";
            return false;
        }
        if (!chat.tapAttachmentOption("Document")) {
            lastSendProblem = "no 'Document' option, options=" + chat.attachmentOptions();
            chat.dismissAttachmentPicker();
            return false;
        }
        for (int i = 0; i < 3; i++) { if (!dismissPermissionPrompts()) break; sleep(500); }
        // Let the system picker come up first: the picker helper starts navigating at once, and
        // on a picker that is not there yet its roots-drawer swipe pulls the notification shade.
        if (!new SystemFilePickerPage(driver).isOpen(Duration.ofSeconds(15))) {
            lastSendProblem = "the system picker did not open after 'Document' (now " + currentPackage() + ")";
            return false;
        }
        if (!chat.pickSystemFileByName(fixture, Duration.ofSeconds(20))) {
            lastSendProblem = "the system picker could not pick " + fixture;
            return false;
        }
        MediaPreviewPage prev = new MediaPreviewPage(driver);
        if (prev.isPreviewOpen()) {
            try { prev.setCaption(caption); } catch (WebDriverException ignored) { }
            prev.tapPreviewSend();
        }
        {
            // ZL-21154: with content already in a Call Sheet / Production Report unit, the
            // upload-mode sheet is the last gate. Continuation keeps what is there.
            UploadModePage mode = new UploadModePage(driver);
            if (mode.isShown(Duration.ofSeconds(callSheet ? 15 : 5))) {
                mode.chooseContinuation();
                mode.tapSend();
            }
        }
        prev.waitPreviewClosed(Duration.ofSeconds(30));
        // An unsent message has no stored 24-hex _id, and the app then opens it in another
        // app — so "sent" is a precondition of every assertion after this.
        // waitAttachmentSent alone is satisfied by ANY finished bubble already in the chat (an
        // older photo), so first wait for THIS document: its caption, or one more doc bubble.
        long deadline = System.currentTimeMillis() + 60_000;
        boolean arrived = false;
        while (!arrived && System.currentTimeMillis() < deadline) {
            arrived = chat.docBubbleCountByBadge() > docsBefore || captionShown(caption);
            if (!arrived) sleep(1000);
        }
        if (!arrived) {
            lastSendProblem = "the document bubble never appeared (" + chat.attachmentSendState() + ")";
            return false;
        }
        boolean sent = chat.waitAttachmentSent(Duration.ofSeconds(90));
        if (!sent) lastSendProblem = "not sent — " + chat.attachmentSendState();
        return sent;
    }

    private boolean sendCcDoc(CcChatThreadPage t, String fixture, String caption) {
        if (!t.openAttachmentSheet()) return false;
        if (!t.tapAttachmentOption("Document")) { t.dismissAttachmentSheet(); return false; }
        for (int i = 0; i < 3; i++) { if (!dismissPermissionPrompts()) break; sleep(500); }
        SystemFilePickerPage picker = new SystemFilePickerPage(driver);
        if (!picker.isOpen(Duration.ofSeconds(15))) return false;
        if (!picker.pickAndReturn(fixture, Duration.ofSeconds(20))) return false;
        MediaPreviewPage prev = new MediaPreviewPage(driver);
        if (prev.isPreviewOpen()) {
            try { prev.setCaption(caption); } catch (WebDriverException ignored) { }
            prev.tapPreviewSend();
            prev.waitPreviewClosed(Duration.ofSeconds(30));
        }
        if (!t.messageShown(caption, Duration.ofSeconds(30))) return false;
        return t.waitMediaSent(Duration.ofSeconds(90));
    }

    // ═════════════════════════════ cleanup ═════════════════════════════

    private void deleteNewestUnitDoc(String caption) {
        HomeChatPage chat = new HomeChatPage(driver);
        boolean deleted = false;
        try {
            if (chat.longPressNewestDocBubble() && chat.tapMenuOption("Delete")) {
                chat.tapDeleteActionBar(Duration.ofSeconds(6));
                deleted = confirmYesDialog(Duration.ofSeconds(8));
                sleep(1500);
            } else {
                try { driver.navigate().back(); } catch (WebDriverException ignored) { }
            }
        } catch (WebDriverException e) {
            System.out.println(">>> CDE cleanup error: " + e.getClass().getSimpleName());
        }
        System.out.println(">>> CDE cleanup of '" + caption + "': deleted=" + deleted);
        clearBlockingPopup();
    }

    // ═════════════════════════════ navigation ═════════════════════════════

    private void requireFeatureBuild() {
        if (!probed) {
            buildHasFeature = ChatDocumentEditorPage.buildHasChatEditor(Config.get("zillit.udid"), appPackage);
            probed = true;
            System.out.println(">>> CDE installed build has ChatAttachmentEditor: " + buildHasFeature);
        }
        need(buildHasFeature == null || buildHasFeature,
                "The installed " + appPackage + " build predates f824d9211 (no ChatAttachmentEditor in "
                        + "its dex): every chat document opens in another app there by design. "
                        + "Install a current development build to run this case.");
    }

    private void enterProject() {
        dismissPermissionPrompts();
        clearBlockingPopup();
        if (landing == Landing.PROJECT_LIST || present("projectListRecyclerView")) {
            ProjectListPage list = new ProjectListPage(driver);
            need(list.tapProject(project), "Project '" + project + "' is not in the list");
        }
        BottomNavPage nav = new BottomNavPage(driver);
        unwindToBottomNav();
        need(nav.waitUntilLoaded(Duration.ofSeconds(60)), "Could not enter a project");
        String current = nav.currentProjectName();
        if (!current.toLowerCase().contains(project.toLowerCase())) {
            System.out.println(">>> CDE on project '" + current + "' — switching to '" + project + "'");
            ProjectListPage list = nav.tapChangeProject();
            sleep(2000);
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView("
                                + "new UiSelector().textContains(\"" + project + "\"))"));
            } catch (WebDriverException ignored) { }
            need(list.tapProject(project), "Could not switch to project '" + project + "'");
            need(nav.waitUntilLoaded(Duration.ofSeconds(60)), "Project '" + project + "' did not open");
            sleep(3000);
            dismissPermissionPrompts();
        }
    }

    private void unwindToBottomNav() {
        for (int i = 0; i < 6 && !present("bottomNavigation"); i++) {
            if (!appPackage.equals(currentPackage())) {
                try {
                    driver.executeScript("mobile: activateApp", java.util.Map.of("appId", appPackage));
                } catch (WebDriverException ignored) { }
            } else {
                try { driver.navigate().back(); } catch (WebDriverException ignored) { }
            }
            sleep(900);
            clearBlockingPopup();
        }
    }

    private void reachHomeUnit(String unit) {
        enterProject();
        HomeChatPage chat = new HomeChatPage(driver);
        need(chat.isLoaded(), "Home units strip not visible");
        need(chat.selectUnit(unit), "No '" + unit + "' unit on this project; units: "
                + chat.visibleUnitNames());
        sleep(1500);
        clearBlockingPopup();
    }

    private void reachTool(String exactName, boolean expectChat) {
        enterProject();
        ToolsPage tools = new ToolsPage(driver);
        need(tools.openToolsTab(), "Could not open the Tools tab");
        need(tools.isLoaded(Duration.ofSeconds(15)), "Tools list did not load");
        tools.searchTools(exactName);
        need(tools.isModuleListed(exactName, Duration.ofSeconds(6)),
                "'" + exactName + "' is not a tool on this project");
        need(tools.openModule(exactName), "Could not open '" + exactName + "'");
        sleep(2500);
        dismissPermissionPrompts();
        if (expectChat) {
            need(tools.isModuleChatOpen(Duration.ofSeconds(15)), "'" + exactName + "' chat did not open");
        }
    }

    private void reachToolAny(List<String> labels) {
        enterProject();
        ToolsPage tools = new ToolsPage(driver);
        need(tools.openToolsTab(), "Could not open the Tools tab");
        need(tools.isLoaded(Duration.ofSeconds(15)), "Tools list did not load");
        List<String> seen = new ArrayList<>();
        for (String l : labels) {
            String q = l.length() > 12 ? l.substring(0, 12) : l;
            tools.searchTools(q);
            if (tools.isModuleListed(l, Duration.ofSeconds(4)) && tools.openModuleContaining(l)) {
                sleep(2500);
                dismissPermissionPrompts();
                return;
            }
            seen.addAll(tools.visibleModuleNames());
        }
        need(false, "None of " + labels + " is a tool on this project (saw " + seen + ")");
    }

    private CcChatThreadPage reachCcOneToOne() {
        enterProject();
        need(new BottomNavPage(driver).selectTab("C&C"), "Could not open the C&C tab");
        sleep(1500);
        dismissPermissionPrompts();
        CcLandingPage p = new CcLandingPage(driver);
        need(p.isLoaded(Duration.ofSeconds(30)), "C&C landing did not load");
        p.tapChip("Members");
        sleep(600);
        need(p.openSearchUsers(), "Could not open the member search");
        String peer = Config.has("zillit.ccPeer") ? Config.get("zillit.ccPeer") : "Vidya";
        p.searchUsers(peer);
        if (p.searchUserNames().isEmpty()) p.searchUsers("");
        need(p.openFirstSearchUser(), "No member to open a 1:1 chat with");
        return new CcChatThreadPage(driver);
    }

    private CcChatThreadPage reachCcGroup() {
        enterProject();
        need(new BottomNavPage(driver).selectTab("C&C"), "Could not open the C&C tab");
        sleep(1500);
        dismissPermissionPrompts();
        CcLandingPage p = new CcLandingPage(driver);
        need(p.isLoaded(Duration.ofSeconds(30)), "C&C landing did not load");
        p.tapChip("Groups");
        sleep(1000);
        String group = Config.has("zillit.ccGroup") ? Config.get("zillit.ccGroup") : "QA Cc Grp";
        if (!p.openRow(group, Duration.ofSeconds(6))) {
            List<String> rows = p.rowNames();
            if (!rows.isEmpty()) {
                need(p.openRow(rows.get(0), Duration.ofSeconds(6)), "Could not open group " + rows.get(0));
            } else {
                // None for this account: make the same "QA Cc Grp" CcGroupCreateTest makes, with the
                // other test account as its member. It is kept for later runs (and for C&C suites).
                need(p.isCreateGroupVisible(), "No C&C group and no create-group button");
                com.zillit.automation.pages.cc.CcCreateGroupPage g = p.tapCreateGroup();
                need(g.isLoaded(Duration.ofSeconds(12)), "Create-group screen did not open");
                g.setName(group + " " + (System.currentTimeMillis() % 1_000_000));
                if (g.tapSelectMember() && g.isMemberPickerOpen(Duration.ofSeconds(8))) g.selectMembers(1);
                g.tapSubmit();
                g.waitClosed(Duration.ofSeconds(20));
                CcLandingPage p2 = new CcLandingPage(driver);
                if (p2.isLoaded(Duration.ofSeconds(3))) {
                    p2.tapChip("Groups");
                    p2.openRow(group, Duration.ofSeconds(8));
                }
            }
        }
        return new CcChatThreadPage(driver);
    }

    private void backToUnitChat() {
        for (int i = 0; i < 3 && !present("etMessage"); i++) {
            if (!appPackage.equals(currentPackage())) {
                new ChatDocumentEditorPage(driver).leaveExternalApp();
            } else {
                try { driver.navigate().back(); } catch (WebDriverException ignored) { }
            }
            sleep(800);
        }
    }

    // ═════════════════════════════ small helpers ═════════════════════════════

    /**
     * assumeTrue that also PRINTS the reason: this runner's console and XML show a bare
     * "SKIPPED" for an aborted case, and every skip here needs its reason in the report.
     */
    /** Why the last {@link #sendUnitDoc} failed, for the skip reason. */
    private String lastSendProblem = "";

    private String snackbarText(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        do {
            try {
                var s = driver.findElements(io.appium.java_client.AppiumBy.id("com.google.android.material:id/snackbar_text"));
                if (s.isEmpty()) s = driver.findElements(byAppId("snackbar_text"));
                if (!s.isEmpty()) return s.get(0).getText();
            } catch (WebDriverException ignored) { }
            sleep(300);
        } while (System.currentTimeMillis() < deadline);
        return "";
    }

    private boolean captionShown(String caption) {
        try {
            return !driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().textContains(\"" + caption + "\")")).isEmpty();
        } catch (WebDriverException e) {
            return false;
        }
    }

    private static void need(boolean condition, String reason) {
        if (!condition) System.out.println(">>> CDE SKIP: " + reason);
        org.junit.jupiter.api.Assumptions.assumeTrue(condition, reason);
    }

    private String caption(String what) {
        return "ZL_AUTO chatdoc " + what + " " + (System.currentTimeMillis() % 1_000_000);
    }

    private static String slug(String s) {
        return s.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    private boolean present(String id) {
        try {
            return !driver.findElements(byAppId(id)).isEmpty();
        } catch (WebDriverException e) {
            return false;
        }
    }

    private boolean tapIfPresent(String id, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            try {
                var els = driver.findElements(byAppId(id));
                if (!els.isEmpty() && els.get(0).isDisplayed()) { els.get(0).click(); return true; }
            } catch (WebDriverException ignored) { }
            sleep(500);
        }
        return false;
    }

    private boolean tapText(String text, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            try {
                var els = driver.findElements(AppiumBy.androidUIAutomator(
                        "new UiSelector().text(\"" + text + "\")"));
                if (!els.isEmpty()) { els.get(0).click(); return true; }
            } catch (WebDriverException ignored) { }
            sleep(500);
        }
        return false;
    }

    private String activity() {
        try {
            String a = driver.currentActivity();
            return a == null ? "" : a;
        } catch (WebDriverException e) {
            return "";
        }
    }

    private boolean activityContains(String part) {
        return activity().contains(part);
    }

    private String currentPackage() {
        try {
            String p = driver.getCurrentPackage();
            return p == null ? "" : p;
        } catch (WebDriverException e) {
            return "";
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
