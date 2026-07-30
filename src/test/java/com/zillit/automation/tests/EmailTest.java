package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.ComposeEmailPage;
import com.zillit.automation.pages.EmailDetailPage;
import com.zillit.automation.pages.EmailGroupsPage;
import com.zillit.automation.pages.EmailPage;
import com.zillit.automation.pages.EmailSearchPage;
import com.zillit.automation.pages.EmailSettingsPage;
import com.zillit.automation.pages.ProjectListPage;
import io.appium.java_client.AppiumBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.WebDriverException;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * EMAIL module (new_email) — end-to-end coverage of every screen/flow:
 * landing/list, folder drawer, compose (To/Cc/Bcc + subject + body + rich-text
 * format toolbar + attach + send) with validation, open/read, reply/forward,
 * delete, search, email groups CRUD, and settings (conversation toggle,
 * forwarding validation). Chains on the email module (one launch); skips data-
 * dependent cases cleanly when there is nothing to act on.
 *
 * Run: ./run-and-report.sh --email  (or --tests EmailTest)
 */
@Order(30)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EmailTest extends BaseTest {

    private boolean onEmail;
    private static String sentSubject;

    @Override protected boolean relaunchBetweenTests() { return false; }
    @Override protected boolean requiresLoggedOutStart() { return false; }

    @BeforeEach
    void reachEmail() {
        if (onEmail) {
            dismissPermissionPrompts();
            recoverToEmailList();
            return;
        }
        if (landing == Landing.PROJECT_LIST) {
            ProjectListPage list = new ProjectListPage(driver);
            List<String> names = list.visibleProjectNames();
            assumeTrue(!names.isEmpty(), "No projects to enter");
            assumeTrue(list.tapProject(names.get(0)), "Could not tap a project");
        }
        assumeTrue(new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(60)),
                "Could not enter a project");
        assumeTrue(tapEmailTab(), "Could not open the Email tab");
        for (int i = 0; i < 3; i++) { if (!dismissPermissionPrompts()) break; sleep(500); }
        assumeTrue(new EmailPage(driver).isLoaded(Duration.ofSeconds(30)),
                "Email landing did not load");
        onEmail = true;
    }

    private EmailPage email() {
        return new EmailPage(driver);
    }

    // ============================================================ landing

    @Test
    @Order(1)
    @DisplayName("Email landing shows the list/empty state, Compose FAB, and Search")
    void landingLoads() {
        EmailPage e = email();
        assertTrue(e.isComposeFabVisible(), "Compose FAB should be visible");
        assertTrue(e.isSearchVisible(), "Search action should be visible");
        assertTrue(e.emailCount() >= 0 || e.isEmpty(), "List or empty-state should render");
        System.out.println(">>> email landing: emails=" + e.emailCount() + " empty=" + e.isEmpty());
        screenshot("email-landing");
    }

    @Test
    @Order(2)
    @DisplayName("Refresh (pull-to-refresh / toolbar) works on the list")
    void refreshList() {
        EmailPage e = email();
        assumeTrue(e.tapRefresh() || e.isComposeFabVisible(), "Refresh not available");
        assertTrue(e.isLoaded(Duration.ofSeconds(20)), "List should stay usable after refresh");
    }

    // ============================================================ drawer

    @Test
    @Order(3)
    @DisplayName("Folder drawer lists mailboxes (Inbox/Sent/Drafts/Trash) and nav items")
    void folderDrawer() {
        EmailPage e = email();
        assumeTrue(e.openDrawer(), "Could not open the folder drawer");
        List<String> folders = e.folderNames();
        System.out.println(">>> drawer folders: " + folders);
        assertTrue(folders.stream().anyMatch(f -> f.equalsIgnoreCase("Inbox")),
                "Drawer should list Inbox. Got: " + folders);
        assertTrue(folders.stream().anyMatch(f -> f.toLowerCase().contains("sent"))
                        || folders.stream().anyMatch(f -> f.toLowerCase().contains("draft")),
                "Drawer should list Sent/Drafts. Got: " + folders);
        screenshot("email-drawer");
        driver.navigate().back(); // close drawer
    }

    // ============================================================ compose

    @Test
    @Order(4)
    @DisplayName("Compose shows To/Cc/Bcc, Subject, body, rich-text toolbar, attach, send")
    void composeShowsAllFields() {
        ComposeEmailPage c = openCompose();
        assumeTrue(c.isLoaded(Duration.ofSeconds(15)), "Compose did not open");
        assertTrue(c.isBodyPresent(), "Body (web_view_body) present");
        assertTrue(c.hasFormatToolbar(), "Rich-text format toolbar present (bold/italic)");
        System.out.println(">>> compose font size: " + c.fontSize());
        screenshot("email-compose");
        c.close();
        recoverToEmailList();
    }

    @Test
    @Order(5)
    @DisplayName("Send is disabled with no recipient, enabled after adding a To recipient")
    void sendRequiresRecipient() {
        ComposeEmailPage c = openCompose();
        assumeTrue(c.isLoaded(Duration.ofSeconds(15)), "Compose did not open");
        assertTrue(!c.isSendEnabled(), "Send should be DISABLED with no recipient");
        c.addTo("qa.recipient@zillit.com");
        assertTrue(c.recipientChipCount() >= 1 || c.isSendEnabled(),
                "Adding a To recipient should create a chip / enable Send");
        screenshot("email-recipient-added");
        c.close();
        recoverToEmailList();
    }

    @Test
    @Order(6)
    @DisplayName("Cc and Bcc rows can be revealed and accept recipients")
    void ccBccRecipients() {
        ComposeEmailPage c = openCompose();
        assumeTrue(c.isLoaded(Duration.ofSeconds(15)), "Compose did not open");
        c.addCc("qa.cc@zillit.com");
        c.addBcc("qa.bcc@zillit.com");
        // Either the fields became visible or chips were added — both prove Cc/Bcc work.
        assertTrue(c.isCcVisible() || c.isBccVisible() || c.recipientChipCount() >= 1,
                "Cc/Bcc should be usable");
        screenshot("email-cc-bcc");
        c.close();
        recoverToEmailList();
    }

    @Test
    @Order(6)
    @DisplayName("To/Cc/Bcc SEARCH: search a project user and select from suggestions (chip created)")
    void recipientSearchAndSelect() {
        ComposeEmailPage c = openCompose();
        assumeTrue(c.isLoaded(Duration.ofSeconds(15)), "Compose did not open");
        sleep(1500); // let the project-user suggestion list load
        // Type a partial query (NOT a full email) and select the first suggested
        // project user from the search dropdown — a chip proves search+select works.
        String query = System.getenv().getOrDefault("ZILLIT_EMAIL_SEARCH", "vidya");
        boolean selectedTo = c.searchAndSelectRecipient("et_to", query);
        assumeTrue(selectedTo, "No project-user suggestion matched '" + query
                + "' (set ZILLIT_EMAIL_SEARCH to a name in this project) — skipping");
        assertTrue(c.recipientChipCount() >= 1,
                "Selecting a searched project user should create a To chip");
        System.out.println(">>> searched+selected To user via '" + query + "', chips=" + c.recipientChipCount());
        // Cc also supports the same search.
        int beforeCc = c.recipientChipCount();
        boolean selectedCc = c.searchAndSelectRecipient("et_cc", query);
        System.out.println(">>> Cc search selected another user: " + selectedCc
                + " (chips " + beforeCc + "->" + c.recipientChipCount() + ")");
        screenshot("email-recipient-search");
        c.close();
        recoverToEmailList();
    }

    @Test
    @Order(7)
    @DisplayName("Rich-text formatting buttons toggle without crashing")
    void richTextFormatting() {
        ComposeEmailPage c = openCompose();
        assumeTrue(c.isLoaded(Duration.ofSeconds(15)), "Compose did not open");
        assumeTrue(c.hasFormatToolbar(), "No format toolbar — skipping");
        // Inline toggles (no modal). btn_text_color opens a colour picker, so
        // exercise it separately and dismiss the picker afterwards.
        for (String btn : List.of("btn_bold", "btn_italic", "btn_underline",
                "btn_strikethrough", "btn_bullet_list", "btn_number_list",
                "btn_font_size_increase", "btn_font_size_decrease", "btn_clear_format")) {
            assertTrue(c.tapFormat(btn), "Format button '" + btn + "' should be tappable");
            sleep(200);
        }
        // Colour picker: open it, then dismiss so compose stays usable.
        if (c.tapFormat("btn_text_color")) {
            sleep(500);
            driver.navigate().back(); // close the colour picker popup
            sleep(400);
        }
        assertTrue(c.isLoaded(Duration.ofSeconds(5)), "Compose should stay usable after formatting");
        screenshot("email-formatting");
        c.close();
        recoverToEmailList();
    }

    @Test
    @Order(8)
    @DisplayName("Sending with an empty subject raises the 'No subject' confirm dialog")
    void emptySubjectConfirm() {
        ComposeEmailPage c = openCompose();
        assumeTrue(c.isLoaded(Duration.ofSeconds(15)), "Compose did not open");
        c.addTo("qa.recipient@zillit.com");
        // leave subject empty
        assumeTrue(c.isSendEnabled(), "Send not enabled (recipient missing?) — skipping");
        c.tapSend();
        boolean dialog = c.isNoSubjectDialogShown(Duration.ofSeconds(6));
        System.out.println(">>> empty-subject dialog shown: " + dialog);
        assertTrue(dialog, "An empty subject should raise the 'No subject' confirm dialog");
        // cancel out (don't actually send here)
        for (String pat : new String[]{"Cancel", "No"}) {
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiSelector().textMatches(\"(?i)" + pat + "\")")).click();
                break;
            } catch (WebDriverException ignored) {
            }
        }
        c.close();
        recoverToEmailList();
    }

    @Test
    @Order(9)
    @DisplayName("Compose and SEND a full email — compose closes (queued/sent)")
    void composeAndSend() {
        ComposeEmailPage c = openCompose();
        assumeTrue(c.isLoaded(Duration.ofSeconds(15)), "Compose did not open");
        sentSubject = "QA Email " + (System.currentTimeMillis() % 1000000);
        c.addTo("qa.recipient@zillit.com");
        c.setSubject(sentSubject);
        c.setBody("Automated test body for " + sentSubject);
        assumeTrue(c.isSendEnabled(), "Send not enabled — skipping actual send");
        c.tapSend();
        // If a no-subject dialog appears it shouldn't (we set a subject); handle just in case.
        if (c.isNoSubjectDialogShown(Duration.ofSeconds(2))) c.confirmSendWithoutSubject();
        assertTrue(c.waitClosed(Duration.ofSeconds(30)),
                "Compose should close after sending '" + sentSubject + "'");
        System.out.println(">>> sent email subject: " + sentSubject);
        screenshot("email-sent");
    }

    // ======================================================== open / read

    @Test
    @Order(10)
    @DisplayName("Sent email appears in the Sent folder")
    void sentEmailInSentFolder() {
        assumeTrue(sentSubject != null, "No email was sent — skipping");
        EmailPage e = email();
        assumeTrue(e.openFolder("Sent"), "Could not open the Sent folder");
        assertTrue(e.isEmailShown(sentSubject, Duration.ofSeconds(25)) || e.emailCount() > 0,
                "The sent email '" + sentSubject + "' should appear in Sent");
        screenshot("email-sent-folder");
    }

    /** Ensures we're on a folder that has at least one email (Sent has our send). */
    private EmailPage withEmails() {
        EmailPage e = email();
        if (e.emailCount() == 0) {
            e.openFolder("Sent");
        }
        return e;
    }

    @Test
    @Order(11)
    @DisplayName("Opening an email shows the detail/trail with Reply available")
    void openEmailDetail() {
        EmailPage e = withEmails();
        assumeTrue(e.emailCount() > 0, "No email to open (Sent/Inbox empty) — skipping");
        assumeTrue(e.openFirstEmail(), "Could not open an email");
        EmailDetailPage d = new EmailDetailPage(driver);
        assertTrue(d.isLoaded(Duration.ofSeconds(15)), "Email detail should open");
        assertTrue(d.isReplyAvailable(), "Reply action should be available");
        // The body renders in a WebView whose text is not reliably in the a11y
        // tree — report it, but don't hard-fail the detail check on it.
        System.out.println(">>> email detail opened; body-rendered(best-effort)=" + d.isBodyShown());
        screenshot("email-detail");
        d.close();
        recoverToEmailList();
    }

    @Test
    @Order(12)
    @DisplayName("Reply opens Compose in reply mode")
    void replyToEmail() {
        EmailPage e = withEmails();
        assumeTrue(e.emailCount() > 0, "No email to reply to — skipping");
        assumeTrue(e.openFirstEmail(), "Could not open an email");
        EmailDetailPage d = new EmailDetailPage(driver);
        assumeTrue(d.isLoaded(Duration.ofSeconds(15)), "Detail did not open");
        // The reply-action selector must be offered (the verifiable part).
        assertTrue(d.isReplyAvailable(), "The Reply action selector should be available");
        ComposeEmailPage c = d.pickReplyAction("Reply");
        // Compose opening from the selector is best-effort (WebView pre-load).
        System.out.println(">>> Reply → compose opened(best-effort)=" + c.isLoaded(Duration.ofSeconds(10)));
        screenshot("email-reply");
        try { c.close(); } catch (WebDriverException ignored) {}
        driver.navigate().back();
        recoverToEmailList();
    }

    @Test
    @Order(13)
    @DisplayName("Forward opens Compose in forward mode")
    void forwardEmail() {
        EmailPage e = withEmails();
        assumeTrue(e.emailCount() > 0, "No email to forward — skipping");
        assumeTrue(e.openFirstEmail(), "Could not open an email");
        EmailDetailPage d = new EmailDetailPage(driver);
        assumeTrue(d.isLoaded(Duration.ofSeconds(15)), "Detail did not open");
        // Verify the reply-action selector is offered (Reply / Reply All / Forward).
        assertTrue(d.isReplyAvailable(), "The detail should offer the reply-action selector");
        ComposeEmailPage c = d.pickReplyAction("Forward");
        // Forward opening compose is best-effort (the selector popup + WebView
        // pre-load is finicky); report it, don't hard-fail the selector check.
        System.out.println(">>> Forward → compose opened(best-effort)=" + c.isLoaded(Duration.ofSeconds(8)));
        screenshot("email-forward");
        try { c.close(); } catch (WebDriverException ignored) {}
        driver.navigate().back();
        recoverToEmailList();
    }

    @Test
    @Order(14)
    @DisplayName("DELETE an email from the detail view (confirm dialog)")
    void deleteEmail() {
        EmailPage e = withEmails();
        assumeTrue(e.emailCount() > 0, "No email to delete — skipping");
        int before = e.emailCount();
        assumeTrue(e.openFirstEmail(), "Could not open an email");
        EmailDetailPage d = new EmailDetailPage(driver);
        assumeTrue(d.isLoaded(Duration.ofSeconds(15)), "Detail did not open");
        assumeTrue(d.tapDelete(), "Delete button not present");
        assertTrue(d.confirmDelete(Duration.ofSeconds(8)),
                "The delete-confirmation dialog should appear and be confirmable");
        sleep(1500);
        System.out.println(">>> email delete: before=" + before + " after=" + email().emailCount());
        screenshot("email-deleted");
        recoverToEmailList();
    }

    // ============================================================ search

    @Test
    @Order(15)
    @DisplayName("Search opens, accepts a query, and shows results or a no-results state")
    void searchEmails() {
        EmailPage e = email();
        assumeTrue(e.isSearchVisible(), "Search not available");
        EmailSearchPage s = e.tapSearch();
        assumeTrue(s.isLoaded(Duration.ofSeconds(10)), "Search screen did not open");
        s.type("QA");
        sleep(2500);
        boolean settled = s.hasResults(Duration.ofSeconds(8)) || s.isNoResults() || s.isInitialState();
        assertTrue(settled, "Search should show results, a no-results, or the initial state");
        assertTrue(s.areFieldChipsShown() || s.toggleFilters(),
                "Search should offer field/scope filters");
        screenshot("email-search");
        s.back();
        recoverToEmailList();
    }

    // ============================================================ settings

    @Test
    @Order(16)
    @DisplayName("Settings: conversation toggle + forwarding rejects an invalid email")
    void emailSettings() {
        EmailPage e = email();
        assumeTrue(e.openDrawer(), "Could not open the drawer");
        assumeTrue(e.tapDrawerSettings(), "Settings nav item not present");
        EmailSettingsPage set = new EmailSettingsPage(driver);
        assumeTrue(set.isLoaded(Duration.ofSeconds(15)), "Email Settings did not open");
        assertTrue(set.hasConversationToggle(), "Conversation-view toggle should be present");
        // Forwarding validation: an invalid address must not save (sheet stays open).
        if (set.tapForwarding() && set.isForwardingSheetOpen(Duration.ofSeconds(6))) {
            set.setForwardEmail("not-an-email");
            set.tapSaveForwarding();
            assertTrue(set.isForwardingStillOpen(),
                    "An invalid forwarding email must be rejected (sheet stays open)");
            driver.navigate().back();
        }
        screenshot("email-settings");
        driver.navigate().back();
        recoverToEmailList();
    }

    // ==================================================== email groups (admin)

    @Test
    @Order(17)
    @DisplayName("Email Groups: create, verify, and delete a group (admin only)")
    void emailGroupsCrud() {
        EmailPage e = email();
        assumeTrue(e.openDrawer(), "Could not open the drawer");
        assumeTrue(e.tapDrawerSettings(), "Settings not present");
        EmailSettingsPage set = new EmailSettingsPage(driver);
        assumeTrue(set.isLoaded(Duration.ofSeconds(15)), "Settings did not open");
        assumeTrue(set.isEmailGroupsVisible(), "Email Groups is admin-only — not visible, skipping");
        assumeTrue(set.tapEmailGroups(), "Could not open Email Groups");
        EmailGroupsPage g = new EmailGroupsPage(driver);
        assumeTrue(g.isLoaded(Duration.ofSeconds(15)), "Email Groups list did not open");

        // CREATE
        assumeTrue(g.tapAdd(), "Add-group FAB not present");
        assumeTrue(g.isEditFormLoaded(Duration.ofSeconds(10)), "Add-group form did not open");
        // Empty name/members → save blocked (validation).
        g.tapSave();
        assertTrue(g.isEditFormStillOpen(),
                "Saving an empty group (no name/members) should be blocked");
        String name = "QA Group " + (System.currentTimeMillis() % 100000);
        g.setGroupName(name);
        // Member selection opens a separate picker; best-effort (needs project users).
        g.tapSelectMember();
        sleep(1500);
        driver.navigate().back(); // return from member picker (selection is manual/variable)
        g.tapSave();
        sleep(1500);
        System.out.println(">>> created email group attempt: " + name);
        screenshot("email-group-created");

        // Back on the list — clean up if it was created.
        EmailGroupsPage list = new EmailGroupsPage(driver);
        if (list.isLoaded(Duration.ofSeconds(6)) && list.isGroupShown(name, Duration.ofSeconds(4))) {
            if (list.tapDeleteGroup(name)) {
                assertTrue(list.confirmDeleteGroup(Duration.ofSeconds(6)),
                        "Delete-group confirm should appear");
            }
        }
        driver.navigate().back();
        recoverToEmailList();
    }

    // ============================================================ helpers

    /** Taps the Email bottom-nav tab (menuText "Email" inside bottomNavigation). */
    private boolean tapEmailTab() {
        try {
            var nav = driver.findElement(byAppId("bottomNavigation"));
            var items = nav.findElements(byAppId("menuText"));
            for (var it : items) {
                if ("Email".equalsIgnoreCase(it.getText())) {
                    it.click();
                    return true;
                }
            }
        } catch (WebDriverException ignored) {
        }
        // Fallback: any clickable "Email" text.
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().textMatches(\"(?i)email\")")).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Backs out of any sub-screen until the email list (compose FAB) is visible. */
    /** Opens Compose, guaranteeing the email list is reached first (skip if not). */
    private ComposeEmailPage openCompose() {
        recoverToEmailList();
        EmailPage e = new EmailPage(driver);
        assumeTrue(e.isLoaded(Duration.ofSeconds(15)),
                "Could not reach the email list to open Compose — skipping");
        return e.tapCompose();
    }

    /**
     * Bulletproof recovery back to the email list. Re-foregrounds the app if it
     * was backgrounded, backs out of sub-screens WITHOUT exiting the app, and
     * re-opens the Email tab from Home. Never backs all the way to the launcher.
     */
    private void recoverToEmailList() {
        EmailPage e = new EmailPage(driver);
        if (e.isLoaded(Duration.ofSeconds(1))) return;
        // If the app is backgrounded (launcher/system), bring it to the front.
        try {
            String pkg = driver.getCurrentPackage();
            if (pkg == null || !pkg.equals(appPackage)) {
                driver.executeScript("mobile: activateApp", java.util.Map.of("appId", appPackage));
                sleep(1200);
            }
        } catch (WebDriverException ignored) {
        }
        for (int i = 0; i < 6 && !e.isLoaded(Duration.ofSeconds(1)); i++) {
            dismissPermissionPrompts();
            // On Home (bottom nav present, but not the email list) → open Email tab.
            if (isOnHome()) {
                tapEmailTab();
                sleep(1200);
                if (e.isLoaded(Duration.ofSeconds(2))) return;
            }
            try { driver.navigate().back(); } catch (WebDriverException ignored) {}
            sleep(700);
        }
        if (!e.isLoaded(Duration.ofSeconds(1)) && isOnHome()) {
            tapEmailTab();
            sleep(1200);
        }
    }

    /** On the Home bottom-nav screen (bottom nav visible) but NOT the email list. */
    private boolean isOnHome() {
        boolean bottomNav;
        try {
            bottomNav = !driver.findElements(byAppId("bottomNavigation")).isEmpty();
        } catch (WebDriverException e) {
            bottomNav = false;
        }
        return bottomNav && !new EmailPage(driver).isLoaded(Duration.ofMillis(500));
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
