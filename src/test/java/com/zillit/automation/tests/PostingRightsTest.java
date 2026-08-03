package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.HomeChatPage;
import com.zillit.automation.pages.HomeChatPage.Rights;
import com.zillit.automation.pages.ProjectListPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Posting-rights (permission) gating on Home units. The app is binary per unit:
 *   - HAS rights  → the message input bar shows; you can send / reply / edit.
 *   - NO rights   → the "You do not have Posting Rights…" banner + Request
 *                   button show instead; you can VIEW but not post, and Reply/
 *                   Edit are absent from the long-press menu.
 *
 * These tests verify that gating. The NO-rights cases only run when the account
 * actually has a view-only unit (an admin account may have rights everywhere) —
 * they skip with a clear message otherwise, so they never false-fail.
 */
@Order(TestOrder.POSTING_RIGHTS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PostingRightsTest extends BaseTest {

    private boolean insideProject;

    @Override
    protected boolean relaunchBetweenTests() {
        return false;
    }

    @Override
    protected boolean requiresLoggedOutStart() {
        return false;
    }

    @BeforeEach
    void enterProject() {
        if (insideProject) {
            dismissPermissionPrompts();
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
        assumeTrue(new HomeChatPage(driver).isLoaded(), "Home units strip not visible");
        insideProject = true;
    }

    private String firstUnitWith(Rights want) {
        HomeChatPage home = new HomeChatPage(driver);
        for (Map.Entry<String, Rights> e : home.classifyAllUnits().entrySet()) {
            if (e.getValue() == want) return e.getKey();
        }
        return null;
    }

    @Test
    @Order(1)
    @DisplayName("Every unit resolves to a definite rights state (input bar XOR no-rights banner)")
    void everyUnitHasConsistentState() {
        HomeChatPage home = new HomeChatPage(driver);
        Map<String, Rights> states = home.classifyAllUnits();
        assumeTrue(!states.isEmpty(), "No chat units to classify");
        System.out.println(">>> unit rights: " + states);
        for (Map.Entry<String, Rights> e : states.entrySet()) {
            assertTrue(e.getValue() != Rights.UNKNOWN,
                    "Unit '" + e.getKey() + "' should show either the input bar or the "
                            + "no-rights banner, not neither/both");
        }
        screenshot("rights-classification");
    }

    @Test
    @Order(2)
    @DisplayName("A unit WITH posting rights lets you post and offers Reply on long-press")
    void unitWithRightsCanPost() {
        String unit = firstUnitWith(Rights.POST);
        assumeTrue(unit != null, "No unit with posting rights on this account");
        HomeChatPage home = new HomeChatPage(driver);
        home.selectUnit(unit);
        assertTrue(home.isInputBarVisible(), "Input bar should be shown on a rights unit");
        assertTrue(!home.isNoRightsBannerVisible(), "No-rights banner must NOT show on a rights unit");
        // Reply requires posting rights — send a message and confirm Reply is offered.
        String marker = "qa-rights-" + (System.currentTimeMillis() % 1000000);
        home.sendMessage(marker);
        assumeTrue(home.messageShown(marker, Duration.ofSeconds(20)), "Need a message to long-press");
        assumeTrue(home.longPressMessage(marker), "Long-press failed");
        assertTrue(home.menuOptions().stream().anyMatch(o -> o.equalsIgnoreCase("Reply")),
                "Reply should be available where you have posting rights");
        home.closeMenu();
        screenshot("rights-can-post");
    }

    @Test
    @Order(3)
    @DisplayName("A view-only unit (NO rights) blocks posting and shows the Request banner")
    void viewOnlyUnitBlocksPosting() {
        String unit = firstUnitWith(Rights.NO_RIGHTS);
        assumeTrue(unit != null,
                "No view-only unit on this account (admin likely has rights everywhere) — "
                        + "run on an account/unit without posting rights to exercise this path");
        HomeChatPage home = new HomeChatPage(driver);
        home.selectUnit(unit);
        assertTrue(home.isNoRightsBannerVisible(),
                "The no-posting-rights banner should show on a view-only unit");
        assertTrue(home.isRequestRightsButtonVisible(),
                "The Request(-rights) button should be offered");
        assertTrue(!home.isInputBarVisible(),
                "The message input bar must be HIDDEN when you have no posting rights");
        // Viewing is still allowed.
        assertTrue(home.canViewMessages(),
                "Messages should still be viewable without posting rights");
        screenshot("rights-view-only");
    }

    @Test
    @Order(4)
    @DisplayName("On a view-only unit the long-press menu does NOT offer Reply")
    void viewOnlyUnitHidesReply() {
        String unit = firstUnitWith(Rights.NO_RIGHTS);
        assumeTrue(unit != null, "No view-only unit on this account — skipping");
        HomeChatPage home = new HomeChatPage(driver);
        home.selectUnit(unit);
        assumeTrue(home.canViewMessages(), "No messages to long-press on the view-only unit");
        // Long-press the first existing message; if a menu opens, Reply must be absent.
        assumeTrue(home.longPressFirstMessage(), "No message available to long-press");
        if (home.isMenuOpen(Duration.ofSeconds(4))) {
            assertTrue(home.menuOptions().stream().noneMatch(o -> o.equalsIgnoreCase("Reply")),
                    "Reply must NOT be offered without posting rights. Options: " + home.menuOptions());
            home.closeMenu();
        }
        screenshot("rights-view-only-menu");
    }
}
