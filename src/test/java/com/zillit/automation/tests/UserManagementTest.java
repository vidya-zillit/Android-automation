package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.ProjectListPage;
import com.zillit.automation.pages.UserManagementPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Settings → Admin Settings → "User Management".
 *
 * <p>Follows one user through a profile change, an Admin Access grant, and a
 * Status disable/enable, checking after each that C&amp;C → Contacts agrees —
 * because that list is what the rest of the crew actually sees.
 *
 * <p><b>No user, department or designation is hard-coded.</b> Project members
 * change between runs, so the user is read from the live list and the new
 * department is whichever option differs from the one already set. When the list
 * is empty the cases skip rather than fail.
 *
 * <p><b>This suite leaves two changes behind</b> — the user's department and
 * designation, and their Admin Access grant. Status is restored, since the run
 * disables and re-enables it. That is what the specified flow does; it is worth
 * knowing before adding this to a nightly sweep.
 *
 * <p>Behaviour confirmed on the device while building this:
 * <ul>
 *   <li>search filters on NAME <em>and</em> DESIGNATION, despite the
 *       "Search By User Name" hint;</li>
 *   <li>Submit on CHANGE PROFILE raises a confirm dialog — "No" returns to the
 *       form with the edits intact, and only "Yes" saves;</li>
 *   <li>granting Admin Access confirms too, but toggling Status does not;</li>
 *   <li>Contacts appends "(Admin)" to the NAME rather than showing a separate
 *       tag, and a disabled user drops out of Contacts entirely.</li>
 * </ul>
 */
@Order(TestOrder.USER_MANAGEMENT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserManagementTest extends BaseTest {

    private static final Duration TOGGLE_WAIT = Duration.ofSeconds(30);
    private static final Duration CONTACTS_WAIT = Duration.ofSeconds(45);

    private boolean insideProject;

    // ---- read from the live list in TC1 ----
    private static String userName;
    private static String originalDesignation;
    private static String newDepartment;
    private static String newDesignation;

    @Override
    protected boolean relaunchBetweenTests() {
        return false;
    }

    @Override
    protected boolean requiresLoggedOutStart() {
        return false;
    }

    /** Returns the User Management list, navigating (or recovering) to it as needed. */
    private UserManagementPage users() {
        if (!insideProject) {
            if (landing == Landing.PROJECT_LIST) {
                ProjectListPage list = new ProjectListPage(driver);
                List<String> names = list.visibleProjectNames();
                assumeTrue(!names.isEmpty(), "No projects");
                assumeTrue(list.tapProject(names.get(0)), "Could not enter a project");
            }
            assumeTrue(new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(60)), "No project");
            insideProject = true;
        }
        UserManagementPage page = new UserManagementPage(driver);
        assumeTrue(page.ensureOnUserManagement(Duration.ofSeconds(60)),
                "Could not reach Settings -> Admin Settings -> User Management");
        return page;
    }

    @Test
    @Order(1)
    @DisplayName("TC1: User Management lists users with their designations")
    void listsUsers() {
        UserManagementPage page = users();
        assertTrue(page.isLoaded(Duration.ofSeconds(20)), "USER MANAGEMENT should be open");

        Map<String, String> all = page.allUsersScrolled();
        screenshot("user-management-list");
        System.out.println(">>> users (" + all.size() + "): " + all);
        assumeTrue(!all.isEmpty(), "No users on this project — nothing to manage");

        // Prefer someone with a designation, since TC3 searches on it.
        userName = all.entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isBlank())
                .map(Map.Entry::getKey)
                .findFirst().orElse(all.keySet().iterator().next());
        originalDesignation = all.get(userName);
        System.out.println(">>> using user: " + userName + " (" + originalDesignation + ")");
    }

    @Test
    @Order(2)
    @DisplayName("TC2: Searching a NAME filters the list, and clearing restores it")
    void searchByName() {
        assumeTrue(userName != null, "No user captured in TC1");
        UserManagementPage page = users();

        assertTrue(page.search(userName), "Search box should accept input");
        Map<String, String> filtered = page.users();
        screenshot("user-management-search-name");
        assertTrue(filtered.containsKey(userName),
                "Searching '" + userName + "' should show that user; got " + filtered.keySet());

        assertTrue(page.clearSearch(), "Search box should clear");
        assertTrue(page.isUserListed(userName),
                "Clearing the search should show " + userName + " again");
    }

    @Test
    @Order(3)
    @DisplayName("TC3: Searching a DESIGNATION filters to the matching user")
    void searchByDesignation() {
        assumeTrue(userName != null, "No user captured in TC1");
        assumeTrue(originalDesignation != null && !originalDesignation.isBlank(),
                "Chosen user has no designation to search on");
        UserManagementPage page = users();

        assertTrue(page.search(originalDesignation), "Search box should accept input");
        Map<String, String> filtered = page.users();
        screenshot("user-management-search-designation");

        assertTrue(filtered.containsKey(userName),
                "Searching '" + originalDesignation + "' should still show " + userName
                        + "; got " + filtered.keySet());
        assertTrue(filtered.values().stream()
                        .allMatch(d -> d != null
                                && d.toLowerCase().contains(originalDesignation.toLowerCase())),
                "Every remaining row should match the designation searched; got " + filtered);
    }

    @Test
    @Order(4)
    @DisplayName("TC4: The edit icon opens CHANGE PROFILE for that user")
    void openChangeProfile() {
        assumeTrue(userName != null, "No user captured in TC1");
        UserManagementPage page = users();

        assumeTrue(page.isUserListed(userName), userName + " is not listed");
        assertTrue(page.tapEditIcon(userName), "Edit icon should be tappable");
        assertTrue(page.isOnChangeProfile(Duration.ofSeconds(20)), "CHANGE PROFILE should open");

        screenshot("user-management-change-profile");
        System.out.println(">>> current department=" + page.department()
                + " designation=" + page.designation());
        assertTrue(!page.department().isBlank(), "Department should be prefilled");
    }

    @Test
    @Order(5)
    @DisplayName("TC5: A different department can be searched and selected")
    void changeDepartment() {
        assumeTrue(userName != null, "No user captured in TC1");
        UserManagementPage page = new UserManagementPage(driver);
        assumeTrue(page.isOnChangeProfile(Duration.ofSeconds(10)), "Not on CHANGE PROFILE");

        String current = page.department();
        assertTrue(page.openDepartmentPicker(), "Department field should be tappable");
        List<String> options = page.pickerOptions();
        assertTrue(options.size() > 1, "Department picker should offer alternatives; got " + options);

        newDepartment = options.stream()
                .filter(o -> !o.equalsIgnoreCase(current))
                .findFirst().orElseThrow();
        assertTrue(page.searchPicker(newDepartment), "Picker search should accept input");
        assertTrue(page.pickerOptions().contains(newDepartment),
                "Searching '" + newDepartment + "' should list it; got " + page.pickerOptions());
        assertTrue(page.choosePickerValue(newDepartment), newDepartment + " should be selectable");

        screenshot("user-management-department-changed");
        assertEquals(newDepartment, page.department(), "Department should now read " + newDepartment);
        assertNotEquals(current, page.department(), "Department should have changed from " + current);
    }

    @Test
    @Order(6)
    @DisplayName("TC6: A designation from the new department can be searched and selected")
    void changeDesignation() {
        assumeTrue(newDepartment != null, "TC5 did not change the department");
        UserManagementPage page = new UserManagementPage(driver);
        assumeTrue(page.isOnChangeProfile(Duration.ofSeconds(10)), "Not on CHANGE PROFILE");

        assertTrue(page.openDesignationPicker(), "Designation field should be tappable");
        List<String> options = page.pickerOptions();
        assertTrue(!options.isEmpty(), "Designation picker should offer values for " + newDepartment);

        newDesignation = options.get(0);
        assertTrue(page.searchPicker(newDesignation), "Picker search should accept input");
        assertTrue(page.choosePickerValue(newDesignation), newDesignation + " should be selectable");

        screenshot("user-management-designation-changed");
        assertEquals(newDesignation, page.designation(),
                "Designation should now read " + newDesignation);
        assertNotEquals(UserManagementPage.NO_DESIGNATION, page.designation(),
                "Designation should no longer be the placeholder");
    }

    @Test
    @Order(7)
    @DisplayName("TC7: Submit warns about erased communication; No cancels, Yes saves")
    void submitConfirmDialog() {
        assumeTrue(newDesignation != null, "TC6 did not set a designation");
        UserManagementPage page = new UserManagementPage(driver);
        assumeTrue(page.isOnChangeProfile(Duration.ofSeconds(10)), "Not on CHANGE PROFILE");

        // First pass: decline.
        assertTrue(page.tapChangeProfileSubmit(), "Submit should be tappable");
        assertTrue(page.isDialogShown(Duration.ofSeconds(15)), "A confirm dialog should appear");
        String message = page.dialogMessage();
        screenshot("user-management-confirm-dialog");
        System.out.println(">>> dialog: " + message);

        // The tool names inside the warning vary by project, so only its stable
        // opening and closing are asserted.
        assertTrue(message.startsWith(UserManagementPage.DEPT_CHANGE_WARNING_START),
                "Dialog should warn about changing department; got " + message);
        assertTrue(message.endsWith(UserManagementPage.DEPT_CHANGE_WARNING_END),
                "Dialog should ask for confirmation; got " + message);

        assertTrue(page.tapDialogNo(), "No should be tappable");
        assertTrue(page.isOnChangeProfile(Duration.ofSeconds(15)),
                "Declining should stay on CHANGE PROFILE");
        assertEquals(newDepartment, page.department(), "Declining should keep the chosen department");
        assertEquals(newDesignation, page.designation(), "Declining should keep the chosen designation");

        // Second pass: accept.
        assertTrue(page.tapChangeProfileSubmit(), "Submit should be tappable again");
        assertTrue(page.isDialogShown(Duration.ofSeconds(15)), "The dialog should appear again");
        assertTrue(page.tapDialogYes(), "Yes should be tappable");

        assertTrue(page.isLoaded(Duration.ofSeconds(30)),
                "Saving should return to USER MANAGEMENT");
    }

    @Test
    @Order(8)
    @DisplayName("TC8: The list shows the newly saved designation")
    void listShowsNewDesignation() {
        assumeTrue(newDesignation != null, "TC6 did not set a designation");
        UserManagementPage page = users();

        Map<String, String> all = page.users();
        screenshot("user-management-updated");
        System.out.println(">>> users after save: " + all);

        assertEquals(newDesignation, all.get(userName),
                userName + " should now show the saved designation");
    }

    @Test
    @Order(9)
    @DisplayName("TC9: Admin Access can be granted, with confirmation")
    void grantAdminAccess() {
        assumeTrue(userName != null, "No user captured in TC1");
        UserManagementPage page = users();

        Boolean before = page.adminAccessEnabled(userName);
        assumeTrue(before != null, "Admin Access toggle not found for " + userName);
        assumeTrue(!before, userName + " already has Admin Access");

        assertTrue(page.tapAdminAccessToggle(userName), "Admin Access toggle should be tappable");
        assertTrue(page.isDialogShown(Duration.ofSeconds(15)),
                "Granting Admin Access should ask for confirmation");
        assertTrue(page.dialogMessage().startsWith(UserManagementPage.ADMIN_ACCESS_WARNING),
                "Expected the admin-access warning; got " + page.dialogMessage());
        assertTrue(page.tapDialogYes(), "Yes should be tappable");

        assertTrue(page.waitForAdminAccess(userName, true, TOGGLE_WAIT),
                "The Admin Access toggle should settle ON");
        screenshot("user-management-admin-granted");
    }

    @Test
    @Order(10)
    @DisplayName("TC10: Contacts shows the new designation and the (Admin) tag")
    void contactsShowAdminAndDesignation() {
        assumeTrue(newDesignation != null, "TC6 did not set a designation");
        UserManagementPage page = new UserManagementPage(driver);

        assertTrue(page.tapBack(), "Back should leave User Management");
        assertTrue(page.isOnAdminSettings(Duration.ofSeconds(20)), "Should land on ADMIN SETTINGS");
        assertTrue(page.tapBack(), "Back should leave Admin Settings");
        assertTrue(page.isOnSettingsTab(Duration.ofSeconds(20)), "Should land on the Settings tab");

        assertTrue(page.openCncTab(), "C&C tab should be reachable");
        assertTrue(page.openContactsTab(), "Contacts tab should open");
        assertTrue(page.waitForContact(userName, true, CONTACTS_WAIT),
                userName + " should be listed in Contacts");

        String label = page.contactLabel(userName);
        String designation = page.contactDesignation(userName);
        screenshot("user-management-contacts-admin");
        System.out.println(">>> contact: " + label + " / " + designation);

        // The tag is appended to the NAME rather than shown as a separate view.
        assertTrue(label.contains(UserManagementPage.ADMIN_TAG),
                "An admin should be tagged " + UserManagementPage.ADMIN_TAG + "; got " + label);
        assertTrue(designation.trim().equalsIgnoreCase(newDesignation.trim()),
                "Contacts should show the saved designation '" + newDesignation
                        + "'; got '" + designation + "'");
    }

    @Test
    @Order(11)
    @DisplayName("TC11: Disabling Status removes the user from Contacts")
    void disableStatusHidesFromContacts() {
        assumeTrue(userName != null, "No user captured in TC1");
        UserManagementPage page = users();

        Boolean before = page.statusEnabled(userName);
        assumeTrue(before != null && before, userName + " is not currently enabled");

        // No confirmation on this one — the switch flips straight away.
        assertTrue(page.tapStatusToggle(userName), "Status toggle should be tappable");
        assertTrue(page.waitForStatus(userName, false, TOGGLE_WAIT),
                "The Status toggle should settle OFF");
        screenshot("user-management-status-off");

        assertTrue(page.tapBack(), "Back should leave User Management");
        assertTrue(page.tapBack(), "Back should leave Admin Settings");
        assertTrue(page.openCncTab(), "C&C tab should be reachable");
        assertTrue(page.openContactsTab(), "Contacts tab should open");

        assertTrue(page.waitForContact(userName, false, CONTACTS_WAIT),
                userName + " should disappear from Contacts while disabled; got " + page.contacts());
        screenshot("user-management-contacts-hidden");
    }

    @Test
    @Order(12)
    @DisplayName("TC12: Re-enabling Status brings the user back to Contacts")
    void enableStatusRestoresContact() {
        assumeTrue(userName != null, "No user captured in TC1");
        UserManagementPage page = users();

        Boolean before = page.statusEnabled(userName);
        assumeTrue(before != null && !before, userName + " is not currently disabled");

        assertTrue(page.tapStatusToggle(userName), "Status toggle should be tappable");
        assertTrue(page.waitForStatus(userName, true, TOGGLE_WAIT),
                "The Status toggle should settle back ON");
        screenshot("user-management-status-on");

        assertTrue(page.tapBack(), "Back should leave User Management");
        assertTrue(page.tapBack(), "Back should leave Admin Settings");
        assertTrue(page.openCncTab(), "C&C tab should be reachable");
        assertTrue(page.openContactsTab(), "Contacts tab should open");

        assertTrue(page.waitForContact(userName, true, CONTACTS_WAIT),
                userName + " should reappear in Contacts once enabled; got " + page.contacts());
        screenshot("user-management-contacts-restored");
    }

    @Test
    @Order(13)
    @DisplayName("TC13: The run finishes on Admin Settings")
    void finishOnAdminSettings() {
        UserManagementPage page = new UserManagementPage(driver);

        assertTrue(page.openSettingsTab(), "Settings tab should be reachable");
        assertTrue(page.isOnSettingsTab(Duration.ofSeconds(20)), "Should be on the Settings tab");
        assertTrue(page.openAdminSettings(), "Admin Settings row should be tappable");
        assertTrue(page.isOnAdminSettings(Duration.ofSeconds(20)),
                "Should finish on ADMIN SETTINGS");
        screenshot("user-management-finished");
    }
}
