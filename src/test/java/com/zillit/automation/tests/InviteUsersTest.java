package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.InviteUsersPage;
import com.zillit.automation.pages.ProjectListPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Settings → "Invite Users" (header "SHARE PROJECT CODE").
 *
 * <p>Read-only: it checks the invitation text and the project code, then leaves.
 * It never presses Share, so no invitation is ever sent.
 *
 * <p>The code is per project and regenerates, so only its SHAPE is asserted —
 * "CODE: " followed by digits, starting with the expected prefix. Pinning the
 * exact value would fail on any other project.
 */
@Order(TestOrder.INVITE_USERS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class InviteUsersTest extends BaseTest {

    private static final Duration WAIT = Duration.ofSeconds(20);
    private static final Duration LOAD_WAIT = Duration.ofSeconds(60);

    /** Project codes observed so far all begin with this. */
    private static final String CODE_PREFIX = "98";

    private boolean insideProject;

    @Override
    protected boolean relaunchBetweenTests() {
        return false;
    }

    @Override
    protected boolean requiresLoggedOutStart() {
        return false;
    }

    private InviteUsersPage invite() {
        if (!insideProject) {
            if (landing == Landing.PROJECT_LIST) {
                ProjectListPage list = new ProjectListPage(driver);
                List<String> names = list.visibleProjectNames();
                assumeTrue(!names.isEmpty(), "No projects");
                assumeTrue(list.tapProject(names.get(0)), "Could not enter a project");
            }
            assumeTrue(new BottomNavPage(driver).waitUntilLoaded(LOAD_WAIT), "No project");
            insideProject = true;
        }
        InviteUsersPage page = new InviteUsersPage(driver);
        assumeTrue(page.ensureOnInviteUsers(LOAD_WAIT),
                "Could not reach Settings -> " + InviteUsersPage.SETTINGS_ROW);
        return page;
    }

    @Test
    @Order(1)
    @DisplayName("TC1: Invite Users opens and shows the invitation text")
    void showsInviteText() {
        InviteUsersPage page = invite();
        assertTrue(page.isLoaded(WAIT), "SHARE PROJECT CODE should be open");
        screenshot("invite-users-open");

        String text = page.inviteText();
        System.out.println(">>> invitation text:");
        System.out.println(text);

        assertEquals(InviteUsersPage.INVITE_TEXT, text,
                "The invitation text should match the app's wording exactly");
    }

    @Test
    @Order(2)
    @DisplayName("TC2: A project code is shown, and it starts with 98")
    void showsProjectCode() {
        InviteUsersPage page = invite();

        assertTrue(page.codeShown(WAIT), "A project code should be shown");
        String label = page.codeLabel();
        String code = page.projectCode();
        System.out.println(">>> " + label + "   (digits: " + code + ")");
        screenshot("invite-users-code");

        // Shape only — the code differs per project and regenerates.
        assertTrue(label.startsWith("CODE:"), "The label should read \"CODE: …\"; got " + label);
        assertTrue(code.matches("\\d+"), "The code should be all digits; got " + code);
        assertTrue(code.startsWith(CODE_PREFIX),
                "The code should start with " + CODE_PREFIX + "; got " + code);
    }

    @Test
    @Order(3)
    @DisplayName("TC3: Back returns to the Settings tab")
    void backToSettings() {
        InviteUsersPage page = invite();

        assertTrue(page.tapBack(), "The top back arrow should be tappable");
        assertTrue(page.isOnSettingsTab(WAIT), "Back should return to the Settings tab");
        screenshot("invite-users-finished");
    }
}
