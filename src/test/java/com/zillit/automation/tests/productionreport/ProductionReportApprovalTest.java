package com.zillit.automation.tests.productionreport;

import com.zillit.automation.core.DriverFactory;
import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.productionreport.ProductionReportPage;
import com.zillit.automation.pages.workflow.DocEditorPage;
import com.zillit.automation.pages.workflow.DocWorkflowPage;
import com.zillit.automation.pages.workflow.DocWorkflowPage.HistoryEntry;
import com.zillit.automation.pages.workflow.DocWorkflowPage.ListKind;
import com.zillit.automation.pages.workflow.DocWorkflowPage.Row;
import io.appium.java_client.android.AndroidDriver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * PRODUCTION REPORT CREATION — the approval round across TWO devices: the creator (BaseTest's own
 * driver, {@code ZILLIT_UDID}, the emulator) sends a sheet for signature; the approver
 * ({@code ZILLIT_RECEIVER_UDID}, the Pixel) rejects it, the creator resubmits, the approver
 * approves, and both see it Finalized. Cases PRC-6x of {@code PRODUCTION_REPORT_CREATION_TESTCASES.md}.
 *
 * <p>Each step runs against one device by swapping {@link #driver} — the page objects and the
 * base navigation all read that field — so the base's helpers work unchanged on either side.
 *
 * <p><b>Leaves one locked sheet</b> ({@code ZL_AUTO_PR_APPR_<run>}, Final Approved) per run:
 * the app cannot delete an approved sheet. Publishing stays out of this suite.
 */
@ExtendWith(ReasonLogger.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Order(TestOrder.PRODUCTION_REPORT_APPROVAL)
class ProductionReportApprovalTest extends ProductionReportBaseTest {

    private static final String RUN = String.format("%04d", System.currentTimeMillis() % 10000);
    private final String name = PREFIX + "APPR_" + RUN;
    private AndroidDriver creator, approver;
    private String serial;
    private boolean rejected, resubmitted, approved;

    @BeforeAll
    void secondDevice() {
        String udidB = System.getenv().getOrDefault("ZILLIT_RECEIVER_UDID", "");
        assumeTrue(!udidB.isBlank(), "ZILLIT_RECEIVER_UDID is required — the approval round needs the approver's device");
        creator = driver;
        approver = DriverFactory.create(udidB, 8210);
    }

    @AfterAll
    void closeSecondDevice() {
        if (approver != null) { try { approver.quit(); } catch (Exception ignored) { } }
        driver = creator;
    }

    /** Runs {@code body} with {@link #driver} pointed at the given device. */
    private <T> T on(AndroidDriver d, Supplier<T> body) {
        AndroidDriver keep = driver;
        driver = d;
        try { return body.get(); } finally { driver = keep; }
    }

    private void on(AndroidDriver d, Runnable body) { on(d, () -> { body.run(); return null; }); }

    // ───────────────────────────── creator sends ─────────────────────────────

    @Test
    @Order(10)
    @DisplayName("PRC-51 (creator) A new sheet sent for signature waits in Sent as \"Pending Signature\"")
    void creatorSends() {
        on(creator, () -> {
            serial = createReport(name, true);
            assertTrue(page().rowAction(ListKind.DRAFTS, serial, "Send for Signature"), "Send for Signature not offered");
            assertTrue(page().waitRowGone(ListKind.DRAFTS, serial, Duration.ofSeconds(30)), "Still in Drafts");
            onSubTab(DocWorkflowPage.SUB_SENT);
            Row r = page().waitRow(ListKind.SENT, serial, Duration.ofSeconds(30));
            assertNotNull(r, "#" + serial + " not in Sent");
            assertEquals(DocWorkflowPage.ST_PENDING, r.status, "Sent status");
        });
    }

    // ───────────────────────────── approver sees it ─────────────────────────────

    @Test
    @Order(20)
    @DisplayName("PRC-60/62/63 (approver) Approvals carries a badge; Received has the request with Approve and Reject")
    void approverReceives() {
        assumeTrue(serial != null, "Nothing was sent");
        on(approver, () -> {
            openTool();
            ProductionReportPage p = page();
            String badge = p.tabBadge(DocWorkflowPage.TAB_APPROVALS);
            p.openTab(DocWorkflowPage.TAB_APPROVALS);
            assertTrue(p.waitSubTab(DocWorkflowPage.SUB_RECEIVED, Duration.ofSeconds(20)), "No Received sub-tab: " + p.subTabNames());
            p.openSubTab(DocWorkflowPage.SUB_RECEIVED);
            Row r = p.waitRow(ListKind.RECEIVED, serial, Duration.ofSeconds(45));
            assertNotNull(r, "#" + serial + " not in the approver's Received");
            assertEquals(DocWorkflowPage.ST_PENDING, r.status, "Received status");
            assertEquals("For Signature", r.stage, "Stage");
            assertEquals("0/1 approved", r.approvals, "Approval count");
            List<String> items = p.openRowMenu(ListKind.RECEIVED, serial);
            p.closePopup();
            assertEquals(List.of("View", "View History", "Chat", "Comment", "Approve", "Reject"), items, "Received menu");
            assertTrue(badge != null && badge.contains("notification"),
                    "The Approvals tab carried no badge for the new request (content-desc: " + badge + ")");
        });
    }

    @Test
    @Order(30)
    @DisplayName("PRC-64/65 (approver) Reject needs a reason; with one, the request turns \"Final Rejected\"")
    void approverRejects() {
        assumeTrue(serial != null, "Nothing was sent");
        on(approver, () -> {
            onSubTab(DocWorkflowPage.SUB_RECEIVED);
            ProductionReportPage p = page();
            assertTrue(p.rowAction(ListKind.RECEIVED, serial, "Reject"), "Reject not offered");
            assertTrue(p.rejectDialogShown(Duration.ofSeconds(8)), "Reject dialog did not open");
            p.tapReject();
            assertEquals("Please provide a rejection reason", p.rejectError(), "Empty-reason error");
            p.typeRejectReason("ZL_AUTO reject " + RUN);
            p.tapReject();
            Row r = p.waitStatus(ListKind.RECEIVED, serial, DocWorkflowPage.ST_REJECTED, Duration.ofSeconds(45));
            assertNotNull(r, "The request left Received after the reject");
            assertEquals(DocWorkflowPage.ST_REJECTED, r.status, "Status after reject — on dev (2026-11-23) POST "
                    + "/approval-requests/<id>/reject answers 404 approval_request_not_found and the app shows nothing (finding 8)");
            rejected = true;
            List<String> items = p.openRowMenu(ListKind.RECEIVED, serial);
            p.closePopup();
            assertFalse(items.contains("Approve") || items.contains("Reject"), "Approve/Reject still offered: " + items);
        });
    }

    @Test
    @Order(40)
    @DisplayName("PRC-66 (creator) Sent shows \"Final Rejected\" and History carries the reason")
    void creatorSeesRejection() {
        assumeTrue(rejected, "PRC-65 did not reject");
        on(creator, () -> {
            onSubTab(DocWorkflowPage.SUB_SENT);
            ProductionReportPage p = page();
            Row r = p.waitStatus(ListKind.SENT, serial, DocWorkflowPage.ST_REJECTED, Duration.ofSeconds(30));
            assertNotNull(r, "#" + serial + " not in Sent");
            assertEquals(DocWorkflowPage.ST_REJECTED, r.status, "Creator's status");
            assertTrue(p.rowAction(ListKind.SENT, serial, "History"), "History not offered");
            try {
                List<HistoryEntry> h = p.historyEntries();
                assertTrue(h.stream().anyMatch(e -> "Rejected".equals(e.action)
                        && String.valueOf(e.reason).contains("ZL_AUTO reject " + RUN)), "No Rejected row with the reason: " + h);
            } finally {
                p.closeDialog();
            }
        });
    }

    @Test
    @Order(50)
    @DisplayName("PRC-67 (creator) Edit → Save moves it back to Draft (after \"Confirm save\"); it can be sent again")
    void creatorResubmits() {
        assumeTrue(rejected, "PRC-65 did not reject");
        on(creator, () -> {
            onSubTab(DocWorkflowPage.SUB_SENT);
            ProductionReportPage p = page();
            assertTrue(p.rowAction(ListKind.SENT, serial, "Edit"), "Edit not offered on a rejected sheet");
            DocEditorPage ed = editor();
            assertTrue(ed.isOpen(Duration.ofSeconds(30)), "Editor did not open");
            assertTrue(ed.tapOverflow(DocEditorPage.MENU_SAVE), "Save not offered");
            assertEquals("Confirm save", p.alertTitle(), "Confirm before re-saving a shared sheet");
            p.tapAlertButton("Continue");
            assertTrue(p.isLoaded(Duration.ofSeconds(30)), "Save did not return to the tool");
            onTab(DocWorkflowPage.TAB_DRAFTS);
            Row r = p.waitRow(ListKind.DRAFTS, serial, Duration.ofSeconds(30));
            assertNotNull(r, "The re-saved sheet is not back in Drafts");
            assertEquals(DocWorkflowPage.ST_DRAFT, r.status, "Status after re-save");
            assertTrue(p.rowAction(ListKind.DRAFTS, serial, "Send for Signature"), "Send for Signature not offered");
            assertTrue(p.waitRowGone(ListKind.DRAFTS, serial, Duration.ofSeconds(30)), "Still in Drafts after resending");
            resubmitted = true;
        });
    }

    @Test
    @Order(60)
    @DisplayName("PRC-68 (approver) Round 2: Approve Without Signature takes it out of Received")
    void approverApproves() {
        assumeTrue(serial != null, "Nothing was sent");
        on(approver, () -> {
            onSubTab(DocWorkflowPage.SUB_RECEIVED);
            ProductionReportPage p = page();
            Row r = p.waitStatus(ListKind.RECEIVED, serial, DocWorkflowPage.ST_PENDING, Duration.ofSeconds(45));
            assertTrue(r != null && DocWorkflowPage.ST_PENDING.equals(r.status), "No pending request to approve: " + r);
            assertTrue(p.rowAction(ListKind.RECEIVED, serial, "Approve"), "Approve not offered");
            assertTrue(p.approveDialogShown(Duration.ofSeconds(8)), "Approve dialog did not open");
            assertEquals("Approve " + name, p.approveTitle(), "Approve dialog title");
            assertTrue(p.approveWithoutSignature(), "\"Approve Without Signature\" not offered");
            assertTrue(p.waitRowGone(ListKind.RECEIVED, serial, Duration.ofSeconds(45)) || !p.subTabNames().contains(DocWorkflowPage.SUB_RECEIVED),
                    "The request is still in Received after approving");
            approved = true;
        });
    }

    @Test
    @Order(70)
    @DisplayName("PRC-69 (creator) Finalized: \"Final Approved\", Publish offered, Edit and Delete gone")
    void creatorFinalized() {
        assumeTrue(approved, "PRC-68 did not approve");
        on(creator, () -> {
            onSubTab(DocWorkflowPage.SUB_FINALIZED);
            ProductionReportPage p = page();
            Row r = p.waitRow(ListKind.FINALIZED, serial, Duration.ofSeconds(45));
            assertNotNull(r, "#" + serial + " not in the creator's Finalized");
            assertEquals(DocWorkflowPage.ST_APPROVED, r.status, "Status");
            List<String> items = p.openRowMenu(ListKind.FINALIZED, serial);
            p.closePopup();
            assertEquals(List.of("View", "View History", "Comment", "Publish"), items, "Creator's Finalized menu");
        });
    }

    @Test
    @Order(80)
    @DisplayName("PRC-70 (approver) Finalized: \"Final Approved\" with no Publish")
    void approverFinalized() {
        assumeTrue(approved, "PRC-68 did not approve");
        on(approver, () -> {
            onSubTab(DocWorkflowPage.SUB_FINALIZED);
            ProductionReportPage p = page();
            Row r = p.waitRow(ListKind.FINALIZED, serial, Duration.ofSeconds(45));
            assertNotNull(r, "#" + serial + " not in the approver's Finalized");
            List<String> items = p.openRowMenu(ListKind.FINALIZED, serial);
            p.closePopup();
            assertEquals(List.of("View", "View History", "Comment"), items, "Approver's Finalized menu");
        });
    }
}
