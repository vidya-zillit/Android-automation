package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.ProjectListPage;
import com.zillit.automation.pages.TutorialForAdminPage;
import com.zillit.automation.pages.TutorialForAdminPage.Step;
import com.zillit.automation.pages.ZillitHelpPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Settings → "Zillit Help" (header "ZILLIT GUIDE") — the whole guide, entry by entry.
 *
 * <p>The guide lists six entries and this suite covers them in place:
 * <ul>
 *   <li>TC1–TC6 — Contact us, through to sending mail from Email Us</li>
 *   <li>TC7–TC8 — the web pages: FAQ and Privacy Policy</li>
 *   <li>TC10 — back out of the guide</li>
 *   <li>TC11–TC22 — Tutorial for Admin, the eleven-stop guided tour</li>
 * </ul>
 *
 * <p>"Reviews" and "Terms of Use" are the entries not opened — Reviews hands off
 * to the store listing, and Terms of Use was dropped at request. Both are still
 * checked as ROWS by TC1, which asserts the guide lists all six.
 *
 * <p><b>This suite SENDS a real email on every run.</b> The composer arrives with
 * "To" already set to Zillit's Support address, so the message reaches their
 * support inbox as well as the BCC address below. The subject and body say
 * plainly that it is a test and needs no action, but it is still a live send —
 * worth knowing before this goes into a nightly sweep.
 *
 * <p><b>The tutorial is one continuous flow</b>, so TC11 opens it, TC12–TC21 each
 * advance a stop and TC22 closes it. A stop that has gone missing is an
 * {@code assertTrue} and not an {@code assumeTrue}: if the tour ended early, every
 * later case skipping would leave the suite looking green when the walkthrough had
 * actually collapsed. What "highlighted" means is spelt out on
 * {@link TutorialForAdminPage} — the overlay's highlight has no node of its own,
 * so each stop is pinned down by its text, by its target being on screen, and by
 * the tooltip sitting on the correct side of that target.
 */
@Order(TestOrder.ZILLIT_HELP)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ZillitHelpTest extends BaseTest {

    private static final Duration WAIT = Duration.ofSeconds(20);
    private static final Duration LOAD_WAIT = Duration.ofSeconds(60);
    /** Web pages fetch over the network, so they get their own budget. */
    private static final Duration PAGE_WAIT = Duration.ofSeconds(45);
    /** The tour's overlay animates in, so a stop's text lands a beat after the tap. */
    private static final Duration STEP_WAIT = Duration.ofSeconds(15);

    private static final String BCC = "Bhavik@zillit.com";
    private static final String SUBJECT = "Please ignore mail is for testing purpose";
    private static final String BODY =
            "This message is for testing purposes only. Please ignore it. No action is required.";

    private static final List<Step> STEPS = TutorialForAdminPage.STEPS;

    private boolean insideProject;

    @Override
    protected boolean relaunchBetweenTests() {
        return false;
    }

    @Override
    protected boolean requiresLoggedOutStart() {
        return false;
    }

    /** Makes sure a project is open — everything here lives inside one. */
    private void enterProject() {
        if (insideProject) {
            return;
        }
        if (landing == Landing.PROJECT_LIST) {
            ProjectListPage list = new ProjectListPage(driver);
            List<String> names = list.visibleProjectNames();
            assumeTrue(!names.isEmpty(), "No projects");
            assumeTrue(list.tapProject(names.get(0)), "Could not enter a project");
        }
        assumeTrue(new BottomNavPage(driver).waitUntilLoaded(LOAD_WAIT), "No project");
        insideProject = true;
    }

    private ZillitHelpPage help() {
        enterProject();
        ZillitHelpPage page = new ZillitHelpPage(driver);
        assumeTrue(page.ensureOnZillitHelp(LOAD_WAIT),
                "Could not reach Settings -> " + ZillitHelpPage.SETTINGS_ROW);
        return page;
    }

    private TutorialForAdminPage tutorial() {
        enterProject();
        return new TutorialForAdminPage(driver);
    }

    // ================= Contact us =================

    @Test
    @Order(1)
    @DisplayName("TC1: Zillit Help opens the guide with all its entries")
    void opensGuide() {
        ZillitHelpPage page = help();
        assertTrue(page.isLoaded(WAIT), "ZILLIT GUIDE should be open");

        List<String> entries = page.guideEntries();
        System.out.println(">>> guide entries: " + entries);
        screenshot("zillit-help-guide");

        for (String expected : ZillitHelpPage.GUIDE_ENTRIES) {
            assertTrue(entries.contains(expected),
                    "The guide should list " + expected + "; got " + entries);
        }
    }

    @Test
    @Order(2)
    @DisplayName("TC2: Contact us offers Call Us and Email Us")
    void opensContactUs() {
        ZillitHelpPage page = help();

        assertTrue(page.openContactUs(), "Contact us should be tappable");
        assertTrue(page.isOnContactUs(WAIT), "CONTACT US should open");
        screenshot("zillit-help-contact-us");

        assertTrue(page.hasCallUsCard(), "A Call Us card should be shown");
        assertTrue(page.hasEmailUsCard(), "An Email Us card should be shown");
    }

    @Test
    @Order(3)
    @DisplayName("TC3: Email Us opens the composer with Support already in To")
    void opensComposer() {
        ZillitHelpPage page = new ZillitHelpPage(driver);
        assumeTrue(page.isOnContactUs(Duration.ofSeconds(10)), "Not on CONTACT US");

        assertTrue(page.tapEmailUs(), "Email Us should be tappable");
        assertTrue(page.isOnCompose(Duration.ofSeconds(30)), "The mail composer should open");
        screenshot("zillit-help-composer");

        // The app fills the recipient itself — nothing types it.
        assertEquals(ZillitHelpPage.SUPPORT_RECIPIENT, page.toRecipient(),
                "The composer should arrive addressed to " + ZillitHelpPage.SUPPORT_RECIPIENT);
    }

    @Test
    @Order(4)
    @DisplayName("TC4: BCC, subject and body accept their content")
    void fillsMail() {
        ZillitHelpPage page = new ZillitHelpPage(driver);
        assumeTrue(page.isOnCompose(Duration.ofSeconds(10)), "Not on the composer");

        assertTrue(page.typeBcc(BCC), "The BCC field should accept input");
        // Committed, the address collapses to a chip holding the DISPLAY NAME
        // only — "Bhavik", not the whole address — hence the prefix check.
        String bcc = page.bccRecipient();
        assertTrue(!bcc.isBlank() && BCC.toLowerCase().startsWith(bcc.toLowerCase()),
                "The BCC chip should reflect " + BCC + "; got '" + bcc + "'");

        assertTrue(page.typeSubject(SUBJECT), "The subject field should accept input");
        assertEquals(SUBJECT, page.subject(), "The subject should read what was typed");

        assertTrue(page.typeBody(BODY), "The body should accept input");
        assertTrue(page.body().contains(BODY),
                "The body should contain the typed text; got '" + page.body() + "'");
        screenshot("zillit-help-filled");
    }

    @Test
    @Order(5)
    @DisplayName("TC5: Send despatches the mail and returns to Contact us")
    void sendsMail() {
        ZillitHelpPage page = new ZillitHelpPage(driver);
        assumeTrue(page.isOnCompose(Duration.ofSeconds(10)), "Not on the composer");

        assertTrue(page.tapSend(), "Send should be tappable");
        // The composer closing back to CONTACT US is the signal the send went
        // through — no snackbar or toast is raised.
        assertTrue(page.isOnContactUs(Duration.ofSeconds(45)),
                "Sending should close the composer and return to CONTACT US");
        assertTrue(!page.isOnCompose(Duration.ofSeconds(2)), "The composer should be gone");
        screenshot("zillit-help-sent");
        System.out.println(">>> test mail sent to Support, BCC " + BCC);
    }

    @Test
    @Order(6)
    @DisplayName("TC6: Back from Contact us returns to the guide")
    void backToGuide() {
        ZillitHelpPage page = new ZillitHelpPage(driver);
        assumeTrue(page.isOnContactUs(Duration.ofSeconds(10)), "Not on CONTACT US");

        assertTrue(page.tapBack(), "Back should be tappable on CONTACT US");
        assertTrue(page.isLoaded(WAIT), "Back should return to ZILLIT GUIDE");
        screenshot("zillit-help-back-to-guide");
    }

    // ================= the web pages =================

    @Test
    @Order(7)
    @DisplayName("TC7: FAQ opens a web page that loads")
    void faqLoads() {
        checkWebPage("FAQ", "FAQ", "Frequently Asked Questions", "zillit-help-faq");
    }

    @Test
    @Order(8)
    @DisplayName("TC8: Privacy Policy opens a web page that loads")
    void privacyPolicyLoads() {
        checkWebPage("Privacy Policy", "PRIVACY POLICY", "Privacy Policy", "zillit-help-privacy");
    }

    // TC9 (Terms of Use) removed at request. The row is still checked in TC1's
    // entry list; it is simply never opened. Order 9 is left free for it.

    @Test
    @Order(10)
    @DisplayName("TC10: Back from the guide returns to the Settings tab")
    void backOutToSettings() {
        ZillitHelpPage page = help();

        assertTrue(page.tapBack(), "Back should be tappable on ZILLIT GUIDE");
        assertTrue(page.isOnSettingsTab(WAIT), "Back should return to the Settings tab");
        screenshot("zillit-help-finished");
    }

    // ================= Tutorial for Admin =================

    @Test
    @Order(11)
    @DisplayName("TC11: Tutorial for Admin opens on the Home tab with Stop and Next")
    void tutorialOpensOnHome() {
        TutorialForAdminPage page = tutorial();
        assumeTrue(page.ensureOnZillitGuide(LOAD_WAIT), "Could not reach ZILLIT GUIDE");

        assertTrue(page.openTutorial(), TutorialForAdminPage.GUIDE_ROW + " should be tappable");
        // The tour takes over the Home tab rather than opening a screen of its own.
        assertTrue(page.isOnHomeTab(WAIT), "The tutorial should redirect to the Home tab");
        assertTrue(page.isTourShowing(WAIT), "The first coach-mark should appear");

        checkStop(page, 0);
    }

    @Test
    @Order(12)
    @DisplayName("TC12: Next highlights Bulletin")
    void tutorialBulletin() {
        advance(1);
    }

    @Test
    @Order(13)
    @DisplayName("TC13: Next highlights Calendar")
    void tutorialCalendar() {
        advance(2);
    }

    @Test
    @Order(14)
    @DisplayName("TC14: Next highlights Call Sheet")
    void tutorialCallSheet() {
        advance(3);
    }

    @Test
    @Order(15)
    @DisplayName("TC15: Next highlights the Zillit icon")
    void tutorialZillitIcon() {
        advance(4);
    }

    @Test
    @Order(16)
    @DisplayName("TC16: Next highlights the project name")
    void tutorialProjectName() {
        advance(5);
    }

    @Test
    @Order(17)
    @DisplayName("TC17: Next highlights SOS")
    void tutorialSos() {
        advance(6);
    }

    @Test
    @Order(18)
    @DisplayName("TC18: Next highlights Email")
    void tutorialEmail() {
        advance(7);
    }

    @Test
    @Order(19)
    @DisplayName("TC19: Next highlights Tools")
    void tutorialTools() {
        advance(8);
    }

    @Test
    @Order(20)
    @DisplayName("TC20: Next highlights C&C")
    void tutorialChatAndCall() {
        advance(9);
    }

    @Test
    @Order(21)
    @DisplayName("TC21: The last stop highlights Settings and offers Close instead of Stop")
    void tutorialSettings() {
        advance(10);
    }

    @Test
    @Order(22)
    @DisplayName("TC22: Close ends the tour and gives the Home tab back")
    void tutorialCloseEndsTour() {
        TutorialForAdminPage page = tutorial();
        assertTrue(page.isTourShowing(Duration.ofSeconds(5)),
                "The tour should still be on its last stop");
        assertEquals(TutorialForAdminPage.CLOSE, page.nextLabel(),
                "The last stop's button should read Close");

        assertTrue(page.tapClose(), "Close should be tappable");
        assertTrue(page.tourGone(WAIT), "Close should dismiss the tour");
        assertTrue(page.isOnHomeTab(WAIT), "The Home tab should be usable again");
        screenshot("tutorial-admin-12-closed");
    }

    // ================= helpers =================

    /**
     * Opens a guide entry, proves the page really rendered, and comes back.
     *
     * <p>"Loaded" means the WebView exposes content AND that content mentions the
     * page's own subject. The header alone is not enough — it appears instantly
     * while the page is still blank, so a header-only check would pass on a
     * failed load.
     */
    private void checkWebPage(String entry, String header, String expectedText, String shot) {
        ZillitHelpPage page = help();

        assertTrue(page.openGuideEntry(entry), entry + " should be tappable");
        assertTrue(page.isWebPageLoaded(header, PAGE_WAIT),
                entry + " should open " + header + " and render content; "
                        + "header was '" + page.headerText() + "', "
                        + page.webViewNodeCount() + " nodes");
        System.out.println(">>> " + header + ": " + page.webViewNodeCount() + " nodes, first texts "
                + page.webViewTexts().stream().limit(3).toList());
        screenshot(shot);

        assertTrue(page.webPageContains(expectedText),
                header + " should mention \"" + expectedText + "\"");

        assertTrue(page.tapBack(), "Back should be tappable on " + header);
        assertTrue(page.isLoaded(WAIT), "Back should return to ZILLIT GUIDE");
    }

    /** Presses Next on the tour, then checks the stop it lands on. */
    private void advance(int index) {
        TutorialForAdminPage page = tutorial();
        Step step = STEPS.get(index);

        assertTrue(page.isTourShowing(Duration.ofSeconds(5)),
                "The tour should still be running before " + step.label
                        + "; it ended after " + STEPS.get(index - 1).label);
        assertTrue(page.tapNext(), "Next should be tappable on " + STEPS.get(index - 1).label);

        checkStop(page, index);
    }

    /**
     * Everything one tour stop has to show: its own text, the thing it points at,
     * and the right pair of buttons.
     */
    private void checkStop(TutorialForAdminPage page, int index) {
        Step step = STEPS.get(index);
        boolean last = index == STEPS.size() - 1;

        // The messages are suppliers, not strings. Built eagerly they would call
        // tooltipDesc()/panelTop()/panelBottom() on EVERY stop just to describe a
        // failure that did not happen — three wasted device round trips a stop,
        // which is real money on a loaded emulator where each one can take seconds.
        assertTrue(page.waitForDesc(step.desc, STEP_WAIT),
                () -> "Stop " + (index + 1) + " (" + step.label + ") should read its own text.\n"
                        + "  expected: " + step.desc + "\n"
                        + "  actual  : " + page.tooltipDesc());

        assertTrue(page.targetVisible(step),
                () -> step.label + " should be on screen for its stop to point at");
        assertTrue(page.anchoredToTarget(step),
                () -> "The tooltip should sit " + step.anchor + " " + step.label
                        + "; panel was [" + page.panelTop() + ".." + page.panelBottom() + "]");

        if (last) {
            // The last stop drops Stop and relabels Next.
            assertFalse(page.hasStopButton(), "The last stop should not offer Stop");
            assertEquals(TutorialForAdminPage.CLOSE, page.nextLabel(),
                    "The last stop's button should read Close");
        } else {
            assertTrue(page.hasStopButton(), step.label + " should offer Stop");
            assertTrue(page.hasNextButton(), step.label + " should offer Next");
            assertEquals(TutorialForAdminPage.NEXT, page.nextLabel(),
                    step.label + " should offer Next");
        }

        // Print what was actually verified, not just that something was. The text
        // is the assertion that matters — it is what tells the stops apart.
        System.out.println(">>> tour stop " + (index + 1) + "/" + STEPS.size() + ": " + step.label);
        System.out.println("    text    : " + step.desc);
        System.out.println("    target  : " + step.label + " on screen, tooltip " + step.anchor);
        System.out.println("    buttons : " + (last ? TutorialForAdminPage.CLOSE + " (no Stop)"
                : TutorialForAdminPage.STOP + " + " + TutorialForAdminPage.NEXT));
        screenshot(String.format("tutorial-admin-%02d-%s", index + 1,
                step.label.toLowerCase().replaceAll("[^a-z0-9]+", "-")));
    }
}
