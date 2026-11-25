package com.zillit.automation.tests.settings;

import com.zillit.automation.core.TestOrder;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.pages.adminsettings.AdminSettingsPage;
import com.zillit.automation.pages.common.BottomNavPage;
import com.zillit.automation.pages.common.ProjectListPage;
import com.zillit.automation.pages.settings.AdminRightsPage;
import com.zillit.automation.pages.settings.AdminRightsPage.RightsRow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Settings → Admin Settings → "User Viewing &amp; Posting Rights Grid" (web gap S-M2:
 * web Settings #3, #24, #25, #26, #30, #31, #32). On Android the per-user View / Post /
 * Download rights live here, not in User Management, and no suite opened the screen.
 *
 * <p>Flow (app source {@code bottomNav/settings/admin/permissiongrid/}): on this build the row opens
 * the user list ({@code PermissionGridUserList}, "Select User") directly; a user opens {@code PermissionUserDetails} with a unit table whose
 * rows have {@code chkView}, {@code chkPost} and {@code chkDownload}.
 *
 * <p><b>Rights are restored.</b> The grant cases change ONE unit's right for ONE user and
 * put it back — in the case itself and again in a {@code finally} — so a failing run never
 * leaves a user with a right they did not have. A non-admin member is preferred as the
 * target; the project's second test account is the only other member on the automation
 * project.
 */
@Order(TestOrder.PERMISSION_GRID)
@ExtendWith(ReasonLogger.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PermissionGridTest extends BaseTest {

    private static final Duration LOAD = Duration.ofSeconds(30);
    private static final Duration SAVE = Duration.ofSeconds(25);

    private boolean insideProject;
    /** The member whose rights table the chained cases use; picked in case 2. */
    private static String target;

    @Override protected boolean relaunchBetweenTests() { return false; }
    @Override protected boolean requiresLoggedOutStart() { return false; }

    private AdminRightsPage page() { return new AdminRightsPage(driver); }

    private void enterProject() {
        if (insideProject) return;
        if (landing == Landing.PROJECT_LIST) {
            ProjectListPage list = new ProjectListPage(driver);
            List<String> names = list.visibleProjectNames();
            assumeTrue(!names.isEmpty(), "No projects to enter");
            assumeTrue(list.tapProject(names.get(0)), "Could not tap a project");
        }
        assumeTrue(new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(60)), "Could not enter a project");
        insideProject = true;
    }

    /**
     * Gets to the grid's "Select User" list from wherever the app is. On this build the Admin
     * Settings row opens PermissionGridUserList directly (Section "home"); the older
     * PermissionGridMainActivity ("Select Section") is commented out in AdminSettingsActivity.
     */
    private boolean reachGridUsers() {
        enterProject();
        AdminRightsPage p = page();
        AdminSettingsPage admin = new AdminSettingsPage(driver);
        for (int i = 0; i < 8; i++) {
            dismissPermissionPrompts();
            if (p.userListOpen(Duration.ofSeconds(2))) return true;
            if (p.gridMainOpen(Duration.ofSeconds(1))) { p.openSection("Home"); continue; }
            if (admin.isOpen(Duration.ofSeconds(1))) {
                p.tapAdminRow(AdminRightsPage.GRID_ROW);
                continue;
            }
            if (!driver.findElements(byAppId("bottomNavigation")).isEmpty()) {
                admin.openSettingsTab();
                sleep(1200);
                assumeTrue(admin.openAdminSettings(),
                        "Admin Settings row not present — this account may not be a project admin");
                admin.isOpen(LOAD);
                continue;
            }
            p.back();
        }
        return p.userListOpen(Duration.ofSeconds(3));
    }

    /** Opens the chosen member's rights table (Home section). */
    private AdminRightsPage reachTargetDetails() {
        assumeTrue(target != null, "No target member was chosen in case 2");
        AdminRightsPage p = page();
        if (p.userDetailsOpen(Duration.ofSeconds(2)) && target.equals(p.detailsUserName())) return p;
        if (p.userDetailsOpen(Duration.ofSeconds(1))) p.back();
        if (!p.userListOpen(Duration.ofSeconds(2))) {
            assumeTrue(reachGridUsers(), "Could not reach the Rights Grid's user list");
        }
        assumeTrue(p.openGridUser(target), target + " is not in the grid's user list");
        assumeTrue(p.userDetailsOpen(LOAD), "The rights table did not open for " + target);
        return p;
    }

    /** Leaves the rights table and opens it again — the re-read proves the server kept the value. */
    private AdminRightsPage reopenTarget() {
        AdminRightsPage p = page();
        p.back();
        assumeTrue(p.userListOpen(Duration.ofSeconds(15)), "Back did not return to the grid's user list");
        assumeTrue(p.openGridUser(target), target + " is not in the grid's user list");
        assumeTrue(p.userDetailsOpen(LOAD), "The rights table did not reopen");
        sleep(1500);
        return p;
    }

    private RightsRow firstRow(AdminRightsPage p, Predicate<RightsRow> want) {
        for (int i = 0; i < 10; i++) {
            List<RightsRow> rows = p.rightsRows();
            if (!rows.isEmpty()) return rows.stream().filter(want).findFirst().orElse(null);
            sleep(800);
        }
        return null;
    }

    // ---------------- open ----------------

    @Test
    @Order(1)
    @DisplayName("S-M2.1 [WEB SET-3] The Rights Grid row opens its 'Select User' list (members + the grid-view link)")
    void gridOpens() {
        assertTrue(reachGridUsers(), "Tapping '" + AdminRightsPage.GRID_ROW
                + "' should open the grid's Select User list (rvUsers)");
        AdminRightsPage p = page();
        System.out.println(">>> grid header='" + p.header() + "' activity=" + p.currentActivity()
                + " users=" + p.gridUserNames());
        screenshot("rights-grid-users");
        assertTrue(p.adminListGone(), "The Admin Settings list should be replaced by the grid, not still shown");
        assertTrue(p.header().equalsIgnoreCase("Select User") || p.currentActivity().contains("PermissionGridUserList"),
                "The grid should open titled 'Select User' (header '" + p.header() + "')");
        assertTrue(!p.gridUserNames().isEmpty(), "The grid should list the project's members");
    }

    @Test
    @Order(2)
    @DisplayName("S-M2.2 [WEB SET-24] Home → a member → the rights table names them and every row has View / Post / Download")
    void sectionUserTable() {
        assumeTrue(reachGridUsers(), "Could not reach the Rights Grid's user list");
        AdminRightsPage p = page();
        List<String> users = p.gridUserNames();
        System.out.println(">>> grid users: " + users);
        assumeTrue(!users.isEmpty(), "No members listed in the grid — nothing to open");

        // Prefer a member who is not an admin: an admin's rights are mostly fixed.
        String chosen = null;
        for (String u : users) {
            if (!p.openGridUser(u) || !p.userDetailsOpen(LOAD)) { p.back(); continue; }
            Boolean admin = p.detailsAdminChecked();
            System.out.println(">>> " + u + " admin=" + admin + " status=" + p.detailsStatusChecked());
            if (chosen == null) chosen = u;
            if (Boolean.FALSE.equals(admin)) { chosen = u; break; }
            p.back();
            p.userListOpen(Duration.ofSeconds(10));
        }
        assumeTrue(chosen != null, "No member's rights table could be opened");
        target = chosen;
        p = reachTargetDetails();
        assertEquals(target, p.detailsUserName(), "The table should be headed by the member that was tapped");
        assertTrue(p.columnHeadersShown(), "The View / Post / Download column headers should be shown");
        RightsRow any = firstRow(p, r -> true);
        List<RightsRow> rows = p.rightsRows();
        System.out.println(">>> rights for " + target + ": " + rows);
        screenshot("rights-grid-table");
        assertTrue(any != null && !rows.isEmpty(), "The rights table (rvUnits) should list at least one unit");
        assertTrue(rows.stream().allMatch(r -> r.hasAllThree),
                "Every unit row should carry a View, a Post and a Download switch: " + rows);
    }

    @Test
    @Order(3)
    @DisplayName("S-M2.3 [WEB SET-25] Home ⇄ Tools swaps the unit list, and Home brings the first list back")
    void homeToolsSwap() {
        AdminRightsPage p = reachTargetDetails();
        firstRow(p, r -> true);
        List<String> home = p.rightsRows().stream().map(r -> r.unit).toList();
        assumeTrue(!home.isEmpty(), "The Home table is empty — nothing to compare");
        assertTrue(p.tapToolsTab(), "The Tools tab (tvTool) should be tappable");
        sleep(2000);
        List<String> tools = p.rightsRows().stream().map(r -> r.unit).toList();
        System.out.println(">>> Home units " + home + " | Tools units " + tools);
        screenshot("rights-grid-tools-tab");
        assertTrue(!tools.isEmpty() && !tools.equals(home),
                "Tools should replace the Home unit list with the tool list. Home=" + home + " Tools=" + tools);
        assertTrue(p.tapHomeTab(), "The Home tab (tvHome) should be tappable");
        sleep(2000);
        List<String> back = p.rightsRows().stream().map(r -> r.unit).toList();
        assertEquals(home, back, "Home should bring the original unit list back");
    }

    @Test
    @Order(4)
    @DisplayName("S-M2.4 [WEB SET-26] A greyed-out (not updatable) switch does not change when tapped")
    void greyedSwitchIsInert() {
        AdminRightsPage p = reachTargetDetails();
        firstRow(p, r -> true);
        RightsRow row = null;
        String sw = null;
        for (String tab : new String[]{"Home", "Tools"}) {
            if (tab.equals("Tools")) p.tapToolsTab(); else p.tapHomeTab();
            sleep(1500);
            for (RightsRow r : p.rightsRows()) {
                if (Boolean.FALSE.equals(r.viewOn)) { row = r; sw = "chkView"; break; }
                if (Boolean.FALSE.equals(r.postOn)) { row = r; sw = "chkPost"; break; }
                if (Boolean.FALSE.equals(r.downloadOn)) { row = r; sw = "chkDownload"; break; }
            }
            if (row != null) break;
        }
        assumeTrue(row != null, "No greyed-out switch on " + target + "'s first screen of units "
                + "(the server marks every right updatable) — nothing to check");
        boolean before = switch (sw) {
            case "chkView" -> Boolean.TRUE.equals(row.view);
            case "chkPost" -> Boolean.TRUE.equals(row.post);
            default -> Boolean.TRUE.equals(row.download);
        };
        System.out.println(">>> disabled " + sw + " on " + row);
        boolean unchanged;
        try {
            p.tapRight(row.unit, sw);
            sleep(3000);
            unchanged = p.waitRight(row.unit, sw, before, Duration.ofSeconds(3));
        } finally {
            // Should never be needed — but if the "disabled" switch did flip, put the right back.
            if (!p.waitRight(row.unit, sw, before, Duration.ofSeconds(1))) {
                boolean ok = restore(row.unit, row);
                System.out.println(">>> disabled switch changed; restored=" + ok);
            }
        }
        assertTrue(unchanged, "A disabled " + sw + " on '" + row.unit + "' must not change when tapped");
        p.tapHomeTab();
    }

    // ---------------- grant and revoke (restored) ----------------

    @Test
    @Order(5)
    @DisplayName("S-M2.5 [WEB SET-30] Grant View, it survives a reopen, then revoke it (restored)")
    void grantThenRevokeView() {
        assumeTrue(System.getenv("ZILLIT_ROLE_CHANGES_OK") != null,
                "Changes another member's rights/role: run only while holding that member's device lock "
                        + "(set ZILLIT_ROLE_CHANGES_OK=1)");
        AdminRightsPage p = reachTargetDetails();
        RightsRow row = firstRow(p, r -> Boolean.FALSE.equals(r.view) && Boolean.TRUE.equals(r.viewOn));
        assumeTrue(row != null, "No unit with View off and updatable for " + target + ": " + p.rightsRows());
        String unit = row.unit;
        boolean changed = false;
        try {
            assertTrue(p.tapRight(unit, "chkView"), "View switch on '" + unit + "' should be tappable");
            changed = true;
            assertTrue(p.waitRight(unit, "chkView", true, SAVE), "View on '" + unit + "' should settle ON");
            p = reopenTarget();
            assertTrue(p.waitRight(unit, "chkView", true, Duration.ofSeconds(10)),
                    "Granted View on '" + unit + "' should still be ON after leaving and reopening the table");
            assertTrue(p.tapRight(unit, "chkView"), "View switch should be tappable again");
            assertTrue(p.waitRight(unit, "chkView", false, SAVE), "View on '" + unit + "' should settle OFF");
            p = reopenTarget();
            assertTrue(p.waitRight(unit, "chkView", false, Duration.ofSeconds(10)),
                    "Revoked View on '" + unit + "' should still be OFF after reopening");
            changed = false;
        } finally {
            if (changed) restore(unit, row);
        }
    }

    @Test
    @Order(6)
    @DisplayName("S-M2.6 [WEB SET-31] Grant Post — View follows it on — then revoke both (restored)")
    void grantPostViewFollows() {
        assumeTrue(System.getenv("ZILLIT_ROLE_CHANGES_OK") != null,
                "Changes another member's rights/role: run only while holding that member's device lock "
                        + "(set ZILLIT_ROLE_CHANGES_OK=1)");
        AdminRightsPage p = reachTargetDetails();
        RightsRow row = firstRow(p, r -> Boolean.FALSE.equals(r.post) && Boolean.TRUE.equals(r.postOn));
        assumeTrue(row != null, "No unit with Post off and updatable for " + target + ": " + p.rightsRows());
        String unit = row.unit;
        boolean changed = false;
        try {
            assertTrue(p.tapRight(unit, "chkPost"), "Post switch on '" + unit + "' should be tappable");
            changed = true;
            assertTrue(p.waitRight(unit, "chkPost", true, SAVE), "Post on '" + unit + "' should settle ON");
            // The client only sends "post"; View turning on is the server's rule, so read it
            // after the re-fetch, not straight after the tap.
            assertTrue(p.waitRight(unit, "chkView", true, Duration.ofSeconds(10)),
                    "Granting Post on '" + unit + "' should turn View on too (web rule). Row: " + p.rightsRow(unit));
            screenshot("rights-grid-post-granted");
        } finally {
            if (changed) {
                boolean ok = restore(unit, row);
                System.out.println(">>> Post/View on '" + unit + "' restored=" + ok);
                assertTrue(ok, "Could not restore '" + unit + "' to " + row);
            }
        }
    }

    @Test
    @Order(7)
    @DisplayName("S-M2.7 [WEB SET-32] Grant Download — View follows it on — then revoke (restored)")
    void grantThenRevokeDownload() {
        assumeTrue(System.getenv("ZILLIT_ROLE_CHANGES_OK") != null,
                "Changes another member's rights/role: run only while holding that member's device lock "
                        + "(set ZILLIT_ROLE_CHANGES_OK=1)");
        AdminRightsPage p = reachTargetDetails();
        RightsRow row = firstRow(p, r -> Boolean.FALSE.equals(r.download) && Boolean.TRUE.equals(r.downloadOn));
        assumeTrue(row != null, "No unit with Download off and updatable for " + target + ": " + p.rightsRows());
        String unit = row.unit;
        boolean changed = false;
        try {
            assertTrue(p.tapRight(unit, "chkDownload"), "Download switch on '" + unit + "' should be tappable");
            changed = true;
            assertTrue(p.waitRight(unit, "chkDownload", true, SAVE), "Download on '" + unit + "' should settle ON");
            assertTrue(p.waitRight(unit, "chkView", true, Duration.ofSeconds(10)),
                    "Download needs View: granting it should leave View on. Row: " + p.rightsRow(unit));
        } finally {
            if (changed) {
                boolean ok = restore(unit, row);
                System.out.println(">>> Download/View on '" + unit + "' restored=" + ok);
                assertTrue(ok, "Could not restore '" + unit + "' to " + row);
            }
        }
    }

    /**
     * Puts the row back to {@code original}: Download first, then Post, then View, each only
     * when it differs. Unticking View while Post is on goes through "ViewPost", which clears
     * Post too, so the order matters. Never throws.
     */
    private boolean restore(String unit, RightsRow original) {
        try {
            AdminRightsPage p = page();
            if (!p.userDetailsOpen(Duration.ofSeconds(3))) p = reachTargetDetails();
            String[][] order = {{"chkDownload"}, {"chkPost"}, {"chkView"}};
            for (String[] o : order) {
                String sw = o[0];
                boolean want = switch (sw) {
                    case "chkView" -> Boolean.TRUE.equals(original.view);
                    case "chkPost" -> Boolean.TRUE.equals(original.post);
                    default -> Boolean.TRUE.equals(original.download);
                };
                RightsRow now = p.rightsRow(unit);
                if (now == null) return false;
                boolean cur = switch (sw) {
                    case "chkView" -> Boolean.TRUE.equals(now.view);
                    case "chkPost" -> Boolean.TRUE.equals(now.post);
                    default -> Boolean.TRUE.equals(now.download);
                };
                if (cur != want) {
                    p.tapRight(unit, sw);
                    p.waitRight(unit, sw, want, SAVE);
                }
            }
            RightsRow end = p.rightsRow(unit);
            System.out.println(">>> restore '" + unit + "': wanted " + original + " now " + end);
            return end != null && Boolean.TRUE.equals(end.view) == Boolean.TRUE.equals(original.view)
                    && Boolean.TRUE.equals(end.post) == Boolean.TRUE.equals(original.post)
                    && Boolean.TRUE.equals(end.download) == Boolean.TRUE.equals(original.download);
        } catch (Exception e) {
            System.out.println(">>> restore of '" + unit + "' failed: " + e);
            return false;
        }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
