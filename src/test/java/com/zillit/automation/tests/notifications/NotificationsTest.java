package com.zillit.automation.tests.notifications;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.common.BottomNavPage;
import com.zillit.automation.pages.common.ProjectListPage;
import com.zillit.automation.pages.common.ToolsPage;
import com.zillit.automation.pages.notifications.NotificationsPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebDriverException;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * NOTIFICATIONS — the web sheet's two notification bugs (gap file {@code notifications.md}).
 *
 * <ul>
 *   <li>NOTIF-01: join-request / profile-request notifications must not take a non-admin into
 *       the admin approval screen. The push route ({@code Extensions.kt navigationHandler},
 *       SETTINGS_NOTIFICATION_KEY → PROFILE_CHANGE / PROJECT_JOIN_USER) opens
 *       {@code ApproveNewUsersListActivity} with no admin check. The in-app list route does not
 *       exist on Android: a row tap is a no-op (NotificationActivity.kt:173-175), pinned here.</li>
 *   <li>NOTIF-02: the list shows a recent row on its FIRST open (it loads once in onCreate).</li>
 * </ul>
 *
 * <p>Single device. Read-only: nothing is deleted or sent.
 */
@ExtendWith(ReasonLogger.class)
@Order(TestOrder.NOTIFICATIONS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NotificationsTest extends BaseTest {

    private boolean insideProject;
    /** Rows read on the first open of the list in this app session (NT-02). */
    private List<String> firstOpenRows;

    @Override protected boolean relaunchBetweenTests() { return false; }
    @Override protected boolean requiresLoggedOutStart() { return false; }

    @BeforeEach
    void reachBottomNav() {
        if (!insideProject) {
            if (landing == Landing.PROJECT_LIST) {
                ProjectListPage list = new ProjectListPage(driver);
                List<String> names = list.visibleProjectNames();
                assumeTrue(!names.isEmpty(), "No projects");
                assumeTrue(list.tapProject(names.get(0)), "Could not enter a project");
            }
            insideProject = true;
        }
        dismissPermissionPrompts();
        BottomNavPage nav = new BottomNavPage(driver);
        for (int i = 0; i < 6 && !nav.waitUntilLoaded(Duration.ofSeconds(2)); i++) {
            try { driver.navigate().back(); } catch (WebDriverException ignored) { }
            pause(600);
        }
        assumeTrue(nav.waitUntilLoaded(Duration.ofSeconds(30)), "Bottom navigation not reached");
    }

    private NotificationsPage openList() {
        NotificationsPage p = new NotificationsPage(driver);
        assumeTrue(p.openFromToolbarLogo(), "The toolbar logo did not open the notification list");
        assertTrue(p.waitSettled(Duration.ofSeconds(30)),
                "The notification list kept its loader on screen for 30 s");
        return p;
    }

    private void closeList() {
        try { driver.navigate().back(); } catch (WebDriverException ignored) { }
        pause(800);
    }

    // [WEB NOTIF-02]
    @Test
    @Order(1)
    @DisplayName("NT-02 The first open of the notification list already shows the newest rows")
    void newestRowsOnFirstOpen() {
        NotificationsPage p = openList();
        firstOpenRows = p.rows();
        boolean firstEmpty = p.isEmptyStateShown();
        System.out.println(">>> first open: empty=" + firstEmpty + " rows=" + firstOpenRows);
        closeList();

        NotificationsPage again = openList();
        List<String> second = again.rows();
        System.out.println(">>> second open: rows=" + second);
        closeList();
        assumeTrue(!second.isEmpty(), "The project has no notifications for this user — nothing to compare");

        if (!top(second, 3).equals(top(firstOpenRows, 3))) {
            // A push may have landed between the two opens; a third read tells the two apart.
            NotificationsPage third = openList();
            List<String> rows3 = third.rows();
            closeList();
            System.out.println(">>> third open: rows=" + rows3);
            assertEquals(top(rows3, 3), top(firstOpenRows, 3),
                    "NOTIF-02: the first open of the list missed rows that a reopen shows. first="
                            + top(firstOpenRows, 3) + " later=" + top(rows3, 3));
        }
        assertFalse(firstEmpty, "NOTIF-02: the first open showed the empty state, a reopen lists " + second.size() + " rows");
    }

    // [WEB NOTIF-01] in-app half
    @Test
    @Order(2)
    @DisplayName("NT-01a Tapping a row in the notification list stays on the list (no route into admin screens)")
    void rowTapStaysOnList() {
        NotificationsPage p = openList();
        List<String> rows = p.rows();
        assumeTrue(!rows.isEmpty(), "No notification rows to tap");
        int idx = p.indexOfRow("join|profile|request");
        if (idx < 0) idx = 0;
        System.out.println(">>> tapping row " + idx + ": " + rows.get(idx));
        assertTrue(p.tapRow(idx), "Could not tap the row");
        pause(3000);
        String act = p.activity();
        System.out.println(">>> after tap: activity=" + act);
        try {
            assertFalse(act.endsWith(NotificationsPage.APPROVE_ACTIVITY),
                    "NOTIF-01: a list row opened the admin approval screen");
            assertTrue(act.endsWith(NotificationsPage.ACTIVITY),
                    "A row tap is a no-op on Android (onNotificationItemTap is empty); now on " + act
                            + ". If this was wired up on purpose, it needs the admin check of NOTIF-01.");
        } finally {
            closeList();
        }
    }

    // [WEB NOTIF-01] push half
    @Test
    @Order(3)
    @DisplayName("NT-01b A non-admin tapping a join/profile-request push is not taken to Approve New Users")
    void nonAdminRequestPushStaysOut() {
        boolean admin = isProjectAdmin();
        System.out.println(">>> current user is project admin=" + admin);
        assumeTrue(!admin, "Needs a NON-admin account: both test accounts (Vidya Testing, Vidya Pixel) are "
                + "project admins, and demoting one or creating a join request from a stranger is out of bounds");
        NotificationsPage p = new NotificationsPage(driver);
        String tapped = p.tapShadeNotification("join|profile change|profile update");
        assumeTrue(tapped != null, "No join/profile-request push in the shade for this non-admin");
        System.out.println(">>> tapped push: " + tapped);
        pause(5000);
        String act = p.activity();
        System.out.println(">>> after push tap: activity=" + act);
        assertFalse(act.endsWith(NotificationsPage.APPROVE_ACTIVITY),
                "NOTIF-01: a non-admin was taken to ApproveNewUsersListActivity by a request push "
                        + "(Extensions.kt navigationHandler has no admin check)");
    }

    /** The Customization banner on the Tools tab is shown only to admins (Tools.kt:477, 489). */
    private boolean isProjectAdmin() {
        ToolsPage tools = new ToolsPage(driver);
        assumeTrue(tools.openToolsTab() && tools.isLoaded(Duration.ofSeconds(15)), "Tools tab did not load");
        for (int i = 0; i < 4; i++) {
            if (!driver.findElements(byAppId("toolsCustomization")).isEmpty()) return true;
            var size = driver.manage().window().getSize();
            var finger = new org.openqa.selenium.interactions.PointerInput(
                    org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
            var seq = new org.openqa.selenium.interactions.Sequence(finger, 1);
            int x = size.getWidth() / 2;
            seq.addAction(finger.createPointerMove(Duration.ZERO,
                    org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, (int) (size.getHeight() * 0.45)));
            seq.addAction(finger.createPointerDown(0));
            seq.addAction(finger.createPointerMove(Duration.ofMillis(400),
                    org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, (int) (size.getHeight() * 0.85)));
            seq.addAction(finger.createPointerUp(0));
            driver.perform(java.util.Collections.singletonList(seq));
            pause(600);
        }
        return false;
    }

    private static List<String> top(List<String> rows, int n) {
        return rows == null ? List.of() : rows.subList(0, Math.min(n, rows.size()));
    }

    private void pause(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
