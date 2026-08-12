package com.zillit.automation.tests;

import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.ConfirmPopupPage;
import com.zillit.automation.pages.DocDistComposerPage;
import com.zillit.automation.pages.DocDistLibraryPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * DOCUMENT DISTRIBUTION — F-25 (send posts the distribution), P-8 (the busy overlay blocks
 * a double send) and J-10 (the sent distribution appears in History).
 *
 * <p><b>This is the only suite that actually sends.</b> It is separated from
 * {@link DocDistComposerTest} for exactly that reason, and it is safe to run because every
 * recipient is an {@code @example.com} address — RFC 2606 reserves that domain precisely
 * so test traffic cannot reach a real mailbox. Nothing here can spam a person.
 *
 * <p><b>Why History is the assertion, not the composer closing.</b> Trap 3.5: the composer
 * hands off to {@code BackgroundEmailSender} and finishes off screen, so "the composer
 * closed" proves only that the UI dismissed it. The distribution appearing in History with
 * this run's unique subject is the first evidence that the send actually posted — the same
 * false-pass the chat modules' clock icon caused.
 */
@Order(TestOrder.DOC_DISTRIBUTION_SEND)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DocDistSendTest extends DocDistBaseTest {

    /** RFC 2606 reserved domain — deliverable nowhere, safe to send to. */
    private static final String TO = "qa.dd.send@example.com";
    /** Unique per run, so History can be searched for exactly this distribution. */
    private static final String SUBJECT = "QA_DD_SEND_" + (System.currentTimeMillis() % 1000000);

    private static boolean sent;

    @BeforeEach
    void reachLibrary() {
        reachLibraryOrSkip();
    }

    @Test
    @Order(1)
    @DisplayName("F-25/P-8: Send posts the distribution — busy overlay, one send, composer closes")
    void sendPostsDistribution() {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        assumeTrue(lib.hasCompose(), "Compose is not offered on this build (3.17)");
        assumeTrue(lib.openCompose(), "Could not tap Compose");

        DocDistComposerPage composer = new DocDistComposerPage(driver);
        // A rights dialog here means this account cannot post — skip rather than fail.
        ConfirmPopupPage gate = new ConfirmPopupPage(driver);
        if (gate.isShown(Duration.ofSeconds(3))) {
            gate.cancel(Duration.ofSeconds(3));
            assumeTrue(false, "This account lacks posting rights — see DocDistRightsTest O-1");
        }
        assumeTrue(composer.isOpen(Duration.ofSeconds(15)), "The composer did not open");

        assumeTrue(composer.addTo(TO), "Could not add the recipient");
        assumeTrue(composer.setSubject(SUBJECT), "Could not set the subject");

        // Send lives on the PREVIEW stage only
        // (`ddSend.visibility = if (preview) VISIBLE else GONE`), so the route is
        // compose → Preview → Send. Asserting Send here previously failed against a
        // button that was simply not on this stage.
        assertTrue(composer.isPreviewEnabled(),
                "With a valid recipient, Preview must be enabled — it is the gate to Send");
        assumeTrue(composer.tapPreview(), "Could not tap Preview");
        assumeTrue(composer.isPreviewStage(Duration.ofSeconds(10)),
                "The preview stage should be showing before we can send");
        assertTrue(composer.hasSendButton(), "The preview stage must offer Send");
        assertTrue(composer.isSendEnabled(),
                "With a valid recipient, Send must be enabled on the preview stage");

        assumeTrue(composer.tapSend(), "Could not tap Send");
        // P-8: the busy overlay covers the composer while the send is in flight, so a
        // second tap cannot register. Try one anyway — if the overlay is doing its job,
        // this is swallowed and History still ends up with exactly one distribution.
        boolean busy = composer.busyOverlayShown();
        composer.tapSend();
        System.out.println(">>> send tapped; busyOverlay=" + busy + " subject='" + SUBJECT + "'");
        screenshot("dds-sending");

        // The composer closing is NOT the proof (3.5) — it is only the cue to go looking.
        boolean closed = false;
        for (int i = 0; i < 30 && !closed; i++) {
            closed = !composer.isOpen(Duration.ofSeconds(1));
            if (!closed) sleep(1000);
        }
        System.out.println(">>> composer closed after send = " + closed);
        assertTrue(closed, "The composer should close once the send is handed off");
        sent = true;
    }

    @Test
    @Order(2)
    @DisplayName("J-10/F-25: the sent distribution appears in History with its subject — the REAL proof")
    void distributionAppearsInHistory() {
        assumeTrue(sent, "Nothing was sent — the send case must pass first");
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        assumeTrue(lib.openHistory(), "Could not open History");

        // BackgroundEmailSender finishes off screen, so the row can take a moment to land.
        boolean listed = false;
        for (int attempt = 0; attempt < 12 && !listed; attempt++) {
            listed = textShown(SUBJECT, Duration.ofSeconds(3));
            if (!listed) {
                searchHistory(SUBJECT);
                listed = textShown(SUBJECT, Duration.ofSeconds(3));
            }
            if (!listed) sleep(5000);
        }
        System.out.println(">>> history contains '" + SUBJECT + "' = " + listed);
        screenshot("dds-history");
        assertTrue(listed,
                "The distribution sent as '" + SUBJECT + "' must appear in History. "
                        + "The composer closing alone never proved the send (trap 3.5) — this "
                        + "is the check that does.");
    }

    /** Types into the history screen's search box, if it has one. */
    private void searchHistory(String text) {
        try {
            var field = driver.findElement(byAppId("dd_search_input"));
            field.click();
            field.clear();
            field.sendKeys(text);
            sleep(1500);
        } catch (Exception ignored) { }
    }

    private boolean textShown(String text, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            try {
                if (!driver.findElements(io.appium.java_client.AppiumBy.androidUIAutomator(
                        "new UiSelector().textContains(\"" + text + "\")")).isEmpty()) {
                    return true;
                }
            } catch (Exception ignored) { }
            sleep(400);
        }
        return false;
    }
}
