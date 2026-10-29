package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.PreApprovedUsersPage;
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
 * Settings → Admin Settings → "Pre-Approved Users".
 *
 * <p>Walks the whole validation ladder on ONE screen instance
 * ({@code relaunchBetweenTests()=false}): each case continues on the form the
 * previous one left, which is how the app really behaves — validation fires only
 * on Submit and fields keep whatever was last entered.
 *
 * <p>Every expected message below was read off the running app, not copied from
 * the spec. Two things differ from the written steps and are handled here:
 * the designation is listed as <b>"2nd assistant director"</b> in lower case, and
 * the list also contains "Crowd 2nd assistant director" — so the pick is matched
 * EXACTLY rather than by prefix.
 *
 * <p><b>This suite creates a real pre-approved user</b> on the backend when it
 * reaches the end of the ladder. That is the specified behaviour, not a side
 * effect, but it is worth knowing before adding it to a nightly sweep.
 */
@Order(TestOrder.PRE_APPROVED_USERS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PreApprovedUsersTest extends BaseTest {

    // ---- messages, all confirmed against the live app ----
    private static final String FIRST_NAME_EMPTY = "First Name cannot be empty";
    private static final String FIRST_NAME_LENGTH =
            "First Name should be of at least 3 characters or at most 15 characters.";
    private static final String LAST_NAME_EMPTY = "Last Name cannot be empty";
    private static final String LAST_NAME_LENGTH =
            "Last Name should be of at least 3 characters or at most 15 characters.";
    private static final String SELECT_DEPARTMENT = "Please select your department";
    private static final String SELECT_DESIGNATION = "Please select designation";
    private static final String SELECT_UNIT = "Please select unit";
    private static final String INVALID_EMAIL = "Please input valid email address";
    private static final String PHONE_REQUIRED = "Phone Number is required";
    private static final String CREATED = "Pre-approved user has been created successfully";

    // ---- test data ----
    private static final String FIRST_NAME = "Karan";
    private static final String LAST_NAME = "Mehra";
    private static final String DEPARTMENT = "Assistant Directors";
    /** Lower case — that is how the app lists it. */
    private static final String DESIGNATION = "2nd assistant director";
    private static final String UNIT_HINT = "Select Unit";
    private static final int MAIN_UNIT_ROW = 0;          // Main Unit is first in the dropdown
    private static final String COUNTRY = "India";
    private static final String BAD_EMAIL = "Bhavik@zillit";
    private static final String GOOD_EMAIL = "Bhavik@zillit.com";
    private static final String PHONE = "9501178101";

    private static final Duration SNACKBAR_WAIT = Duration.ofSeconds(8);

    private boolean insideProject;
    /** Set once the share sheet confirmed the user was created. */
    private boolean sharingSheetSeen;

    @Override
    protected boolean relaunchBetweenTests() {
        return false;
    }

    @Override
    protected boolean requiresLoggedOutStart() {
        return false;
    }

    /** Returns the Pre-Approved Users form, navigating (or recovering) to it as needed. */
    private PreApprovedUsersPage preApproved() {
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
        PreApprovedUsersPage page = new PreApprovedUsersPage(driver);

        // Route from WHEREVER the app currently is. Each case leaves it on a
        // different screen (the form, Admin Settings, the Settings tab, the
        // not-joined list), so a fixed path is wrong most of the time.
        //
        // ensureOnPreApprovedUsers loops instead of testing each state once: on the
        // Settings tab it opens Admin Settings first, on Admin Settings it goes
        // straight to the row, and a tap that does not land is retried on the next
        // pass. The old if-chain gave up after a single miss, which is why starting
        // from the Settings tab kept skipping rather than navigating.
        assumeTrue(page.ensureOnPreApprovedUsers(Duration.ofSeconds(60)),
                "Could not reach Settings -> Admin Settings -> Pre-Approved Users");
        // Department and unit both load over the network on open — the dropdowns are
        // empty until those return, so waiting here is not optional.
        page.waitForLoaderGone(Duration.ofSeconds(30));
        return page;
    }

    @Test
    @Order(1)
    @DisplayName("Test Case 1: Submitting an empty form is rejected on First Name")
    void emptyFormRejected() {
        PreApprovedUsersPage page = preApproved();
        screenshot("pre-approved-opened");

        assertTrue(page.tapSubmit(), "Submit should be tappable");
        assertTrue(page.snackbarShown(FIRST_NAME_EMPTY, SNACKBAR_WAIT),
                "Expected: " + FIRST_NAME_EMPTY + " — saw: " + page.lastSnackbarText());
        screenshot("pre-approved-first-name-empty");
    }

    @Test
    @Order(2)
    @DisplayName("Test Case 2: First Name shorter than 3 characters is rejected")
    void firstNameTooShort() {
        PreApprovedUsersPage page = preApproved();

        page.setFirstName("Ka");
        assertTrue(page.tapSubmit(), "Submit should be tappable");
        assertTrue(page.snackbarShown(FIRST_NAME_LENGTH, SNACKBAR_WAIT),
                "Expected: " + FIRST_NAME_LENGTH + " — saw: " + page.lastSnackbarText());
        screenshot("pre-approved-first-name-length");
    }

    @Test
    @Order(3)
    @DisplayName("Test Case 3: With a valid First Name, the empty Last Name is rejected")
    void lastNameEmpty() {
        PreApprovedUsersPage page = preApproved();

        page.setFirstName(FIRST_NAME);
        assertTrue(page.tapSubmit(), "Submit should be tappable");
        assertTrue(page.snackbarShown(LAST_NAME_EMPTY, SNACKBAR_WAIT),
                "Expected: " + LAST_NAME_EMPTY + " — saw: " + page.lastSnackbarText());
        screenshot("pre-approved-last-name-empty");
    }

    @Test
    @Order(4)
    @DisplayName("Test Case 4: Last Name shorter than 3 characters is rejected")
    void lastNameTooShort() {
        PreApprovedUsersPage page = preApproved();

        page.setLastName("Sh");
        assertTrue(page.tapSubmit(), "Submit should be tappable");
        assertTrue(page.snackbarShown(LAST_NAME_LENGTH, SNACKBAR_WAIT),
                "Expected: " + LAST_NAME_LENGTH + " — saw: " + page.lastSnackbarText());
        screenshot("pre-approved-last-name-length");
    }

    @Test
    @Order(5)
    @DisplayName("Test Case 5: With both names valid, the missing department is rejected")
    void departmentRequired() {
        PreApprovedUsersPage page = preApproved();

        page.setLastName(LAST_NAME);
        assertTrue(page.tapSubmit(), "Submit should be tappable");
        assertTrue(page.snackbarShown(SELECT_DEPARTMENT, SNACKBAR_WAIT),
                "Expected: " + SELECT_DEPARTMENT + " — saw: " + page.lastSnackbarText());
        screenshot("pre-approved-department-required");
    }

    @Test
    @Order(6)
    @DisplayName("Test Case 6: Choosing a department leaves the designation outstanding")
    void designationRequired() {
        PreApprovedUsersPage page = preApproved();

        assertTrue(page.openDepartmentPicker(), "Select Department should open the picker");
        assertTrue(page.isPickerOpen(Duration.ofSeconds(10)), "Department picker should be open");
        page.searchInPicker(DEPARTMENT);
        assertTrue(page.selectFromPicker(DEPARTMENT), DEPARTMENT + " should be selectable");
        assertEquals(DEPARTMENT, page.departmentLabel(), "Department field should show the choice");
        screenshot("pre-approved-department-selected");

        assertTrue(page.tapSubmit(), "Submit should be tappable");
        assertTrue(page.snackbarShown(SELECT_DESIGNATION, SNACKBAR_WAIT),
                "Expected: " + SELECT_DESIGNATION + " — saw: " + page.lastSnackbarText());
    }

    @Test
    @Order(7)
    @DisplayName("Test Case 7: Choosing a designation leaves the unit outstanding")
    void unitRequired() {
        PreApprovedUsersPage page = preApproved();

        assertTrue(page.openDesignationPicker(), "Select Designation should open the picker");
        assertTrue(page.isPickerOpen(Duration.ofSeconds(10)), "Designation picker should be open");
        page.searchInPicker(DESIGNATION);
        assertTrue(page.selectFromPicker(DESIGNATION), DESIGNATION + " should be selectable");
        assertEquals(DESIGNATION, page.designationLabel(), "Designation field should show the choice");
        screenshot("pre-approved-designation-selected");

        assertTrue(page.tapSubmit(), "Submit should be tappable");
        assertTrue(page.snackbarShown(SELECT_UNIT, SNACKBAR_WAIT),
                "Expected: " + SELECT_UNIT + " — saw: " + page.lastSnackbarText());
    }

    @Test
    @Order(8)
    @DisplayName("Test Case 8: With a unit chosen, a malformed email is rejected")
    void invalidEmailRejected() {
        PreApprovedUsersPage page = preApproved();

        // Per the spec there is NO Submit between choosing the unit and typing the
        // email — the unit selection is verified indirectly, by the run reaching the
        // email rule instead of repeating "Please select unit".
        assertTrue(page.openDropdown(UNIT_HINT), "Select Unit should expand");
        assertTrue(page.selectDropdownRow(UNIT_HINT, MAIN_UNIT_ROW), "Main Unit should be selectable");
        screenshot("pre-approved-unit-selected");

        page.setEmail(BAD_EMAIL);
        assertTrue(page.tapSubmit(), "Submit should be tappable");
        assertTrue(page.snackbarShown(INVALID_EMAIL, SNACKBAR_WAIT),
                "Expected: " + INVALID_EMAIL + " — saw: " + page.lastSnackbarText());
        screenshot("pre-approved-invalid-email");
    }

    @Test
    @Order(9)
    @DisplayName("Test Case 9: With a valid email and country, the missing phone is rejected")
    void phoneRequired() {
        PreApprovedUsersPage page = preApproved();

        page.setEmail(GOOD_EMAIL);
        assertTrue(page.openCountryPicker(), "Country should open its picker");
        assertTrue(page.isPickerOpen(Duration.ofSeconds(10)), "Country picker should be open");
        page.searchInPicker(COUNTRY);
        assertTrue(page.selectFromPicker(COUNTRY), COUNTRY + " should be selectable");
        screenshot("pre-approved-country-selected");

        assertTrue(page.tapSubmit(), "Submit should be tappable");
        assertTrue(page.snackbarShown(PHONE_REQUIRED, SNACKBAR_WAIT),
                "Expected: " + PHONE_REQUIRED + " — saw: " + page.lastSnackbarText());
        screenshot("pre-approved-phone-required");
    }

    @Test
    @Order(10)
    @DisplayName("Test Case 10: A complete form creates the pre-approved user")
    void createsPreApprovedUser() {
        PreApprovedUsersPage page = preApproved();

        page.setPhone(PHONE);
        assertTrue(page.tapSubmit(), "Submit should be tappable");

        // The success SNACKBAR cannot be asserted here. On success the app calls
        // shareText(), and the system share sheet — a different app entirely —
        // takes the window before the snackbar can be read. Log it if we happen to
        // catch it, but assert on the share sheet, which only appears when the
        // create actually succeeded.
        if (page.snackbarShown(CREATED, Duration.ofSeconds(3))) {
            System.out.println(">>> success snackbar seen: " + CREATED);
        }
        assertTrue(page.isSharingSheetOpen(Duration.ofSeconds(25)),
                "The share sheet should open, which only happens once the user is created");
        screenshot("pre-approved-created-sharing-sheet");
        sharingSheetSeen = true;
    }

    @Test
    @Order(11)
    @DisplayName("Test Case 11: Closing the sharing screen leaves the form cleared")
    void sharingSheetClosesAndFormResets() {
        assumeTrue(sharingSheetSeen, "The user was never created");
        PreApprovedUsersPage page = new PreApprovedUsersPage(driver);

        assertTrue(page.dismissSharingSheet(Duration.ofSeconds(15)),
                "Swiping down should close the sharing screen");
        assertTrue(page.isLoaded(), "Should be back on the Pre-Approved Users form");
        screenshot("pre-approved-after-sharing-closed");

        assertTrue(page.isFormCleared(),
                "Every field should be cleared after a successful create");
        screenshot("pre-approved-form-cleared");
    }

    @Test
    @Order(12)
    @DisplayName("Test Case 12: The created user appears in the not-joined list")
    void createdUserInNotJoinedList() {
        assumeTrue(sharingSheetSeen, "The user was never created");
        PreApprovedUsersPage page = preApproved();

        assertTrue(page.tapListOfNotJoinedUsers(), "List of not joined users should be tappable");
        assertTrue(page.isNotJoinedListOpen(Duration.ofSeconds(15)), "The list should open");
        screenshot("pre-approved-not-joined-list");

        assertTrue(page.isUserListed(FIRST_NAME + " " + LAST_NAME, DESIGNATION),
                FIRST_NAME + " " + LAST_NAME + " (" + DESIGNATION + ") should be in the list");
    }

    @Test
    @Order(13)
    @DisplayName("Test Case 13: Back twice returns to the Admin Settings list")
    void backReturnsToAdminSettings() {
        PreApprovedUsersPage page = new PreApprovedUsersPage(driver);

        assertTrue(page.tapBackButton(), "Back should leave the not-joined list");
        assertTrue(page.isLoaded(), "Should be back on the Pre-Approved Users form");

        assertTrue(page.tapBackButton(), "Back should leave Pre-Approved Users");
        assertTrue(page.isOnAdminSettings(Duration.ofSeconds(10)),
                "Should finish on the Admin Settings list");
        screenshot("pre-approved-back-on-admin-settings");
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }
}
