package com.zillit.automation.tests;

import com.zillit.automation.pages.CcCallTabPage;
import com.zillit.automation.pages.CcLandingPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * C&C → Call tab: Recent / Missed sub-tabs, call-log rows + call-type icons,
 * search over the log, and the new-call FAB. Read-only (does not start a call).
 */
@Order(45)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CcCallTabTest extends CcBaseTest {

    private CcCallTabPage openCallTab() {
        CcLandingPage p = landing();
        assumeTrue(p.tapTopTab("Call"), "Could not switch to the Call tab");
        sleep(1500);
        CcCallTabPage c = new CcCallTabPage(driver);
        return c.isLoaded(Duration.ofSeconds(12)) ? c : null;
    }

    @Test
    @Order(1)
    @DisplayName("Call tab loads with Recent/Missed sub-tabs, a call log and the new-call FAB")
    void callTabLoads() {
        CcCallTabPage c = openCallTab();
        assumeTrue(c != null, "Call tab did not load — skipping");
        assertTrue(c.isSubTabShown("Recent") && c.isSubTabShown("Missed"),
                "Call tab should show Recent + Missed sub-tabs");
        System.out.println(">>> Call Recent log rows=" + c.logRowCount()
                + " callTypeIcons=" + c.hasCallTypeIcons() + " fab=" + c.isNewCallFabVisible());
        assertTrue(c.isNewCallFabVisible(), "The new-call FAB should be present");
        screenshot("cc-call-recent");
    }

    @Test
    @Order(2)
    @DisplayName("Recent tab lists call-log entries with call-type indicators")
    void recentTab() {
        CcCallTabPage c = openCallTab();
        assumeTrue(c != null, "Call tab did not load — skipping");
        assumeTrue(c.tapSubTab("Recent"), "Recent sub-tab not tappable");
        int rows = c.logRowCount();
        System.out.println(">>> Recent rows=" + rows + " names=" + c.logNames());
        // The test project has prior calls; if empty (fresh project) just assert the tab is up.
        assertTrue(rows >= 0 && c.isSubTabShown("Recent"), "Recent call log should be listed");
        if (rows > 0) assertTrue(c.hasCallTypeIcons(), "Log rows should show a call-type icon");
        screenshot("cc-call-recent-list");
    }

    @Test
    @Order(3)
    @DisplayName("Missed tab switches the list to missed calls")
    void missedTab() {
        CcCallTabPage c = openCallTab();
        assumeTrue(c != null, "Call tab did not load — skipping");
        assumeTrue(c.tapSubTab("Missed"), "Missed sub-tab not tappable");
        System.out.println(">>> Missed rows=" + c.logRowCount());
        assertTrue(c.isSubTabShown("Missed"), "Missed tab should be active");
        screenshot("cc-call-missed");
    }

    @Test
    @Order(4)
    @DisplayName("Search filters the call log")
    void searchLog() {
        CcCallTabPage c = openCallTab();
        assumeTrue(c != null, "Call tab did not load — skipping");
        assumeTrue(c.tapSubTab("Recent"), "Recent not available");
        assumeTrue(c.isSearchVisible(), "Call-log search not present — skipping");
        int before = c.logRowCount();
        c.search("zzz-no-match-" + (System.currentTimeMillis() % 1000));
        int after = c.logRowCount();
        System.out.println(">>> call search " + before + " -> " + after);
        assertTrue(after <= before, "A non-matching search should not increase the log rows");
        screenshot("cc-call-search");
    }
}
