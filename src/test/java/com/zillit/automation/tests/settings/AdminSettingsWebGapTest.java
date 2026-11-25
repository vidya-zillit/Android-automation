package com.zillit.automation.tests.settings;

import com.zillit.automation.core.TestOrder;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.pages.adminsettings.AdminSettingsPage;
import com.zillit.automation.pages.adminsettings.DepartmentListingOrderPage;
import com.zillit.automation.pages.adminsettings.UserManagementPage;
import com.zillit.automation.pages.common.BottomNavPage;
import com.zillit.automation.pages.common.ProjectListPage;
import com.zillit.automation.pages.settings.AdminRightsPage;
import io.appium.java_client.AppiumBy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Admin Settings cases the web E2E report has and the Android suites lacked
 * (gap file {@code gaps/settings.md}): S-M1 every core tile, S-M3 Approve User Profile,
 * S-M5 the User Management row controls, S-M4 the Admin Access switch (Cancel, demote,
 * promote) and S-M6 the Department Listing Order back guard.
 *
 * <p><b>Roles are restored.</b> S-M4 flips the other member's Admin Access and puts it
 * back to what it was, in the case and again in a {@code finally}. Run it while holding the
 * other test device's lock, so no suite on that device sees the role change.
 * S-M6 drags a department but leaves with "No" (no save) and checks the order did not change.
 */
@Order(TestOrder.ADMIN_SETTINGS_WEB_GAP)
@ExtendWith(ReasonLogger.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AdminSettingsWebGapTest extends BaseTest {

    private static final Duration LOAD = Duration.ofSeconds(30);
    private static final Duration TOGGLE = Duration.ofSeconds(30);

    private boolean insideProject;

    @Override protected boolean relaunchBetweenTests() { return false; }
    @Override protected boolean requiresLoggedOutStart() { return false; }

    private AdminRightsPage rights() { return new AdminRightsPage(driver); }

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

    /** Admin Settings, reached from wherever the app is. */
    private AdminSettingsPage reachAdminSettings() {
        enterProject();
        AdminSettingsPage admin = new AdminSettingsPage(driver);
        for (int i = 0; i < 8; i++) {
            dismissPermissionPrompts();
            clearBlockingPopup();
            if (admin.isOpen(Duration.ofSeconds(2))) {
                if (admin.hasSearchBox()) admin.clearSearch();
                return admin;
            }
            if (!driver.findElements(byAppId("bottomNavigation")).isEmpty()) {
                admin.openSettingsTab();
                sleep(1200);
                assumeTrue(admin.openAdminSettings(),
                        "Admin Settings row not present — this account may not be a project admin");
                admin.isOpen(LOAD);
                continue;
            }
            try { driver.navigate().back(); } catch (WebDriverException ignored) { }
            sleep(1000);
        }
        assumeTrue(admin.isOpen(Duration.ofSeconds(3)), "Could not reach Admin Settings");
        return admin;
    }

    // ---------------- S-M1 ----------------

    /** Rows every non-"Other" project shows (AdminSettingsActivity builds them unconditionally). */
    private static final List<String> CORE_TILES = List.of(
            "Approve New User Requests",
            "Approve User Profile",
            "Create/Update Home Units.",
            "Create New Department",
            "Create New Designation",
            "Customization of tools",
            "Manage Tool Groups",
            "Edit Project Name",
            "User Viewing & Posting Rights Grid",
            "Pre-Approved Users",
            "Set/View SOS Receivers",
            "User Management",
            "Watermark Logo of Company",
            "Company Details");

    @Test
    @Order(1)
    @DisplayName("S-M1 [WEB SET-1] Admin Settings lists every core tile by name")
    void everyCoreTileListed() {
        AdminSettingsPage admin = reachAdminSettings();
        AdminSettingsPage.Sweep sweep = admin.sweep();
        List<String> rows = sweep.rows;
        System.out.println(">>> admin rows (" + rows.size() + "): " + rows);
        screenshot("admin-core-tiles");
        List<String> missing = new ArrayList<>();
        for (String t : CORE_TILES) if (!rows.contains(t)) missing.add(t);
        // Two rows change wording with the project: the crew/staff listing order (an "Other"
        // project says Staff List) and Delete Project / Stop Project Deletion.
        boolean listingOrder = rows.stream().anyMatch(r -> r.startsWith("Change Department Listing Order for"));
        boolean deleteRow = rows.contains("Delete Project") || rows.contains("Stop Project Deletion");
        if (!listingOrder) missing.add("Change Department Listing Order for Crew/Staff List");
        if (!deleteRow) missing.add("Delete Project / Stop Project Deletion");
        assertTrue(missing.isEmpty(), "Admin Settings is missing core tiles " + missing + ". Listed: " + rows);
    }

    // ---------------- S-M3 ----------------

    @Test
    @Order(2)
    @DisplayName("S-M3 [WEB SET-8] Approve User Profile opens its 'Select User' queue, and Back returns")
    void approveUserProfileOpens() {
        reachAdminSettings();
        AdminRightsPage p = rights();
        assertTrue(p.tapAdminRow(AdminRightsPage.APPROVE_PROFILE_ROW),
                "The '" + AdminRightsPage.APPROVE_PROFILE_ROW + "' row should be tappable");
        boolean open = p.approveListOpen(Duration.ofSeconds(20));
        System.out.println(">>> approve-profile header='" + p.header() + "' activity=" + p.currentActivity());
        screenshot("approve-user-profile");
        assertTrue(open, "Approve User Profile should open the 'Select User' list (ApproveNewUsersListActivity)");
        sleep(2000);
        assertTrue(p.approveListHasRowsOrEmptyView(), "The queue should show either requests or its empty view");
        p.back();
        assertTrue(p.adminSettingsOpen(Duration.ofSeconds(15)), "Back should return to Admin Settings");
    }

    // ---------------- S-M5 ----------------

    @Test
    @Order(3)
    @DisplayName("S-M5 [WEB SET-21] Each User Management row has Status, Admin Access and the edit icon")
    void userManagementRowControls() {
        reachAdminSettings();
        UserManagementPage um = new UserManagementPage(driver);
        assertTrue(um.openUserManagement(), "User Management row should be tappable");
        assertTrue(um.isLoaded(LOAD), "USER MANAGEMENT should open");
        sleep(1500);
        AdminRightsPage p = rights();
        List<String> names = p.userManagementNames();
        System.out.println(">>> User Management rows: " + names);
        assumeTrue(!names.isEmpty(), "No members listed in User Management");
        screenshot("user-management-row-controls");
        List<String> problems = new ArrayList<>();
        for (String n : names.subList(0, Math.min(3, names.size()))) {
            if (um.statusEnabled(n) == null) problems.add(n + ": no Status switch (icCheck)");
            if (um.adminAccessEnabled(n) == null) problems.add(n + ": no Admin Access switch (chkAdmin)");
            if (p.userManagementControl(n, "changeProfile") == null) problems.add(n + ": no edit icon (changeProfile)");
        }
        assertTrue(problems.isEmpty(), "User Management rows are missing controls: " + problems);
    }

    // ---------------- S-M4 ----------------

    @Test
    @Order(4)
    @DisplayName("S-M4 [WEB SET-27/28/29] Admin Access: demote needs no dialog, No keeps a non-admin, a deactivated member is gated, Yes grants (role and status restored)")
    void adminAccessCancelDemotePromote() {
        assumeTrue(System.getenv("ZILLIT_ROLE_CHANGES_OK") != null,
                "Changes another member's rights/role: run only while holding that member's device lock "
                        + "(set ZILLIT_ROLE_CHANGES_OK=1)");
        reachAdminSettings();
        UserManagementPage um = new UserManagementPage(driver);
        assertTrue(um.openUserManagement() && um.isLoaded(LOAD), "USER MANAGEMENT should open");
        sleep(1500);
        List<String> names = rights().userManagementNames();
        assumeTrue(!names.isEmpty(), "No members listed in User Management");
        String user = names.get(0);
        Boolean original = um.adminAccessEnabled(user);
        Boolean status = um.statusEnabled(user);
        assumeTrue(original != null, "No Admin Access switch for " + user);
        assumeTrue(Boolean.TRUE.equals(status), user + " is deactivated — Admin Access is locked");
        System.out.println(">>> S-M4 on " + user + " (admin=" + original + ")");
        try {
            if (original) {
                // Demote: AddEditAdminsPage calls the API straight away, no dialog.
                assertTrue(um.tapAdminAccessToggle(user), "Admin Access switch should be tappable");
                boolean dialog = um.isDialogShown(Duration.ofSeconds(3));
                if (dialog) um.tapDialogNo();
                assertTrue(!dialog, "Removing Admin Access should not ask for confirmation (it asked: '"
                        + um.dialogMessage() + "')");
                assertTrue(um.waitForAdminAccess(user, false, TOGGLE), "Admin Access should settle OFF");
            }
            // Grant, then No: the switch goes back off and nothing is saved.
            assertTrue(um.tapAdminAccessToggle(user), "Admin Access switch should be tappable");
            assertTrue(um.isDialogShown(Duration.ofSeconds(10)), "Granting Admin Access should ask for confirmation");
            assertTrue(um.dialogMessage().startsWith(UserManagementPage.ADMIN_ACCESS_WARNING),
                    "Expected the admin-access warning; got '" + um.dialogMessage() + "'");
            um.tapDialogNo();
            sleep(1500);
            assertTrue(um.waitForAdminAccess(user, false, Duration.ofSeconds(5)),
                    "No on the confirmation must leave Admin Access OFF");
            // Re-open the screen: the server must agree.
            um.tapBack();
            assumeTrue(um.isOnAdminSettings(Duration.ofSeconds(15)), "Back did not return to Admin Settings");
            assertTrue(um.openUserManagement() && um.isLoaded(LOAD), "USER MANAGEMENT should reopen");
            sleep(1500);
            assertTrue(um.waitForAdminAccess(user, false, Duration.ofSeconds(10)),
                    "After No and a reopen, " + user + " must still be a non-admin");
            screenshot("admin-access-cancelled");

            // S-M4.3 (web #27): while deactivated, the member's actions are gated. Done here,
            // while the member is a non-admin (an admin's Status switch is locked).
            deactivatedMemberIsGated(um, user);

            // Grant with Yes, then demote again (web #28) — or just restore when it was an admin.
            assertTrue(um.tapAdminAccessToggle(user), "Admin Access switch should be tappable");
            assertTrue(um.isDialogShown(Duration.ofSeconds(10)), "Granting should ask for confirmation");
            um.tapDialogYes();
            assertTrue(um.waitForAdminAccess(user, true, TOGGLE), "Yes should grant Admin Access");
            if (!original) {
                assertTrue(um.tapAdminAccessToggle(user), "Admin Access switch should be tappable");
                boolean dialog = um.isDialogShown(Duration.ofSeconds(3));
                if (dialog) um.tapDialogNo();
                assertTrue(!dialog, "Removing Admin Access should not ask for confirmation");
                assertTrue(um.waitForAdminAccess(user, false, TOGGLE), "Admin Access should settle OFF");
            }
        } finally {
            if (statusTouched) {
                boolean st = restoreStatusOn(user);
                System.out.println(">>> Status for " + user + " restored ON: " + st);
                assertTrue(st, "Could not re-activate " + user);
                statusTouched = false;
            }
            boolean ok = restoreAdmin(user, original);
            System.out.println(">>> Admin Access for " + user + " restored to " + original + ": " + ok);
            assertTrue(ok, "Could not restore " + user + "'s Admin Access to " + original);
        }
    }

    private void deactivatedMemberIsGated(UserManagementPage um, String user) {
        Boolean active = um.statusEnabled(user);
        WebElement statusSw = rights().userManagementControl(user, "icCheck");
        if (!Boolean.TRUE.equals(active) || statusSw == null || !Boolean.TRUE.equals(rights().enabledState(statusSw))) {
            System.out.println(">>> S-M4.3 not run: Status switch for " + user + " is not an enabled ON switch");
            return;
        }
        statusTouched = true;
        assertTrue(um.tapStatusToggle(user), "Status switch should be tappable");
        assertTrue(um.waitForStatus(user, false, TOGGLE), "Status should settle OFF (deactivated)");
        Boolean adminEnabled = rights().enabledState(rights().userManagementControl(user, "chkAdmin"));
        assertTrue(um.tapEditIcon(user), "The edit icon should be tappable");
        boolean toast = rights().toastOrSnackShown("perform this action on disable user", Duration.ofSeconds(4));
        boolean opened = um.isOnChangeProfile(Duration.ofSeconds(3));
        System.out.println(">>> deactivated: adminSwitchEnabled=" + adminEnabled + " toast=" + toast + " changeProfileOpened=" + opened);
        if (opened) um.tapBack();
        assertTrue(!opened, "Change Profile must not open for a deactivated member");
        assertTrue(toast, "The edit icon on a deactivated member should say 'You can't perform this action on disable user'");
        assertTrue(Boolean.FALSE.equals(adminEnabled), "Admin Access should be locked (disabled) for a deactivated member");
        assertTrue(um.tapStatusToggle(user), "Status switch should be tappable again");
        assertTrue(um.waitForStatus(user, true, TOGGLE), "Status should settle back ON");
        statusTouched = false;
    }

    private boolean statusTouched;

    private boolean restoreStatusOn(String user) {
        try {
            UserManagementPage um = new UserManagementPage(driver);
            if (!um.ensureOnUserManagement(Duration.ofSeconds(60))) return false;
            sleep(1500);
            Boolean st = um.statusEnabled(user);
            if (Boolean.TRUE.equals(st)) return true;
            um.tapStatusToggle(user);
            return um.waitForStatus(user, true, TOGGLE);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean restoreAdmin(String user, boolean wanted) {
        try {
            UserManagementPage um = new UserManagementPage(driver);
            if (um.isDialogShown(Duration.ofSeconds(1))) um.tapDialogNo();
            if (!um.ensureOnUserManagement(Duration.ofSeconds(60))) return false;
            sleep(1500);
            Boolean now = um.adminAccessEnabled(user);
            if (now == null) return false;
            if (now == wanted) return true;
            um.tapAdminAccessToggle(user);
            if (um.isDialogShown(Duration.ofSeconds(5))) {
                if (wanted) um.tapDialogYes(); else um.tapDialogNo();
            }
            return um.waitForAdminAccess(user, wanted, TOGGLE);
        } catch (Exception e) {
            System.out.println(">>> restoreAdmin failed: " + e);
            return false;
        }
    }

    // ---------------- S-M6 ----------------

    @Test
    @Order(5)
    @DisplayName("S-M6 [WEB SET-12] Department Listing Order: Back without changes closes; after a drag it asks to save, and No keeps the order")
    void departmentOrderBackGuard() {
        reachAdminSettings();
        DepartmentListingOrderPage dept = new DepartmentListingOrderPage(driver);
        assertTrue(dept.openChangeDepartmentListingOrder(), "The listing-order row should be tappable");
        assumeTrue(dept.isLoaded(), "The listing-order screen did not load");
        sleep(1500);
        List<String> before = dept.visibleDepartmentNames();
        System.out.println(">>> departments: " + before);
        assumeTrue(before.size() >= 3, "Fewer than 3 departments on screen — cannot drag");

        // 1. No change → back closes straight to Admin Settings.
        driver.navigate().back();
        sleep(1500);
        boolean asked = rights().toastOrSnackShown("Do you want to save changes?", Duration.ofSeconds(2));
        assertTrue(!asked, "Back with NO changes should not ask to save");
        assertTrue(dept.isOnAdminSettings(Duration.ofSeconds(10)), "Back with no changes should return to Admin Settings");

        // 2. Drag → back → the save prompt → No → order unchanged on reopen.
        assertTrue(dept.openChangeDepartmentListingOrder(), "The listing-order row should be tappable again");
        assumeTrue(dept.isLoaded(), "The listing-order screen did not load");
        sleep(1500);
        String moved = before.get(2);
        boolean dragged = dept.dragDepartmentToPosition(moved, 1);
        System.out.println(">>> dragged '" + moved + "' to 1: " + dragged + " → " + dept.visibleDepartmentNames());
        assumeTrue(!dept.visibleDepartmentNames().equals(before), "The drag did not change the order — nothing to guard");
        driver.navigate().back();
        boolean prompt = rights().toastOrSnackShown("Do you want to save changes?", Duration.ofSeconds(6));
        screenshot("dept-order-unsaved-prompt");
        try {
            assertTrue(prompt, "Back after a reorder should ask 'Do you want to save changes?'");
        } finally {
            if (prompt) tapText("No");
            sleep(1500);
        }
        assertTrue(dept.isOnAdminSettings(Duration.ofSeconds(10)), "No should close the screen to Admin Settings");
        assertTrue(dept.openChangeDepartmentListingOrder(), "The listing-order row should be tappable again");
        sleep(2000);
        List<String> after = dept.visibleDepartmentNames();
        System.out.println(">>> order after No: " + after);
        assertEquals(before, after, "Leaving with No must not save the dragged order");
        driver.navigate().back();
    }

    // ---------------- weak covered rows ----------------

    @Test
    @Order(6)
    @DisplayName("[WEB SET-7] Edit Project Name opens prefilled with the current project name")
    void editProjectNamePrefilled() {
        reachAdminSettings();
        var page = new com.zillit.automation.pages.adminsettings.EditProjectNamePage(driver);
        assertTrue(page.openEditProjectName(), "Edit Project Name should be tappable");
        assumeTrue(page.isLoaded(), "Edit Project Name did not load");
        sleep(1500);
        String toolbar = page.toolbarProjectName();
        String field = page.projectNameFieldText();
        System.out.println(">>> project '" + toolbar + "' field '" + field + "'");
        driver.navigate().back();
        sleep(1200);
        assumeTrue(!toolbar.isBlank(), "Could not read the project name from the toolbar");
        assertEquals(toolbar, field, "The name field should be prefilled with the current project name");
    }

    @Test
    @Order(7)
    @DisplayName("[WEB SET-23] Change Profile closed without saving changes nothing")
    void changeProfileCloseWithoutSaving() {
        reachAdminSettings();
        UserManagementPage um = new UserManagementPage(driver);
        assertTrue(um.openUserManagement() && um.isLoaded(LOAD), "USER MANAGEMENT should open");
        sleep(1500);
        var before = um.users();
        assumeTrue(!before.isEmpty(), "No members listed");
        String user = before.keySet().iterator().next();
        assertTrue(um.tapEditIcon(user), "The edit icon should be tappable");
        assertTrue(um.isOnChangeProfile(Duration.ofSeconds(15)), "The edit icon should open CHANGE PROFILE");
        driver.navigate().back();
        sleep(1500);
        if (um.isDialogShown(Duration.ofSeconds(2))) um.tapDialogNo();
        assertTrue(um.isLoaded(Duration.ofSeconds(15)), "Back should return to User Management");
        sleep(1500);
        var after = um.users();
        System.out.println(">>> before " + before + " after " + after);
        assertEquals(before.get(user), after.get(user), "Closing Change Profile without saving must not change " + user);
    }

    @Test
    @Order(8)
    @DisplayName("[WEB SET-34] Edit My Profile → Preference → Edit Email & Phone opens, and Back returns")
    void editEmailAndPhoneOpens() {
        enterProject();
        var prof = new com.zillit.automation.pages.settings.EditMyProfilePage(driver);
        for (int i = 0; i < 5 && driver.findElements(byAppId("bottomNavigation")).isEmpty(); i++) {
            driver.navigate().back();
            sleep(900);
        }
        assumeTrue(prof.ensureOnEditPreference(Duration.ofSeconds(60)), "Could not reach Edit My Profile → Preference");
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().resourceIdMatches(\".*:id/selectEmailEdit\"))"));
        } catch (WebDriverException ignored) { }
        boolean tapped;
        try { driver.findElement(byAppId("selectEmailEdit")).click(); tapped = true; }
        catch (WebDriverException e) { tapped = false; }
        assertTrue(tapped, "The 'Edit Email & Phone' control (selectEmailEdit) should be on the Preference tab");
        sleep(2500);
        String act = rights().currentActivity();
        System.out.println(">>> Edit Email & Phone activity: " + act);
        screenshot("edit-email-phone");
        boolean opened = act.contains("EditEmailAndPhone");
        driver.navigate().back();
        sleep(1500);
        assertTrue(opened, "Edit Email & Phone should open EditEmailAndPhoneActivity (now " + act + ")");
        assertTrue(prof.isOnPreferenceTab(Duration.ofSeconds(10)), "Back should return to Edit My Profile");
        prof.tapBack();
    }

    private void tapText(String label) {
        try {
            for (WebElement b : driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().clickable(true).textMatches(\"(?i)" + label + "\")"))) {
                b.click();
                return;
            }
        } catch (WebDriverException ignored) { }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
