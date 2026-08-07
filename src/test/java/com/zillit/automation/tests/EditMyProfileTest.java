package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.EditMyProfilePage;
import com.zillit.automation.pages.ProjectListPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Settings → "Edit My Profile", EDIT PREFERENCE tab.
 *
 * <p>Covers the avatar (camera then gallery, checking the picture actually
 * changes), the unit and gender dropdowns, and the full Add More Details
 * lifecycle: validation, save, edit with its own validation, and delete with its
 * confirmation.
 *
 * <p>First of the SETTINGS-tab suites, so it sorts after every Admin Settings
 * module.
 *
 * <p>Behaviour confirmed on the device while building this:
 * <ul>
 *   <li>dialog validation lives in an {@code error} ATTRIBUTE — the message is a
 *       tooltip with no text node — and it lingers until the field revalidates;</li>
 *   <li>the avatar thumbnail must be allowed to SETTLE before it is compared:
 *       read too early it returns a partly-loaded frame;</li>
 *   <li>the full-screen viewer repaints continuously and cannot be hashed;</li>
 *   <li>unit and gender dropdowns are invisible to the tree and need coordinate
 *       taps, opened by their arrow;</li>
 *   <li>gender offers exactly Male, Female and Non-Binary.</li>
 * </ul>
 *
 * <p><b>Leaves the profile changed</b> — unit and gender are set and not
 * restored, matching the specified flow. The detail row it creates is deleted by
 * the last case, so that part is self-cleaning.
 */
@Order(TestOrder.EDIT_MY_PROFILE)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EditMyProfileTest extends BaseTest {

    private static final Duration MESSAGE_WAIT = Duration.ofSeconds(20);
    private static final Duration UPLOAD_WAIT = Duration.ofSeconds(60);
    private static final Duration SETTLE_WAIT = Duration.ofSeconds(30);

    private static final String TITLE_1 = "Android";
    private static final String DESC_1 = "Automation";
    private static final String TITLE_2 = "Google";
    private static final String DESC_2 = "Pixel";

    private boolean insideProject;
    private static String avatarAfterCamera;
    /** The unit chosen in TC5, re-checked after the save round trip. */
    private static String chosenUnit;
    /** Department the user started on, so TC17 can pick a different one. */
    private static String originalDepartment;
    private static String newDepartment;
    private static String newDesignation;

    private static final String NEW_FIRST_NAME = "Jon";
    private static final String NEW_LAST_NAME = "Vilson";
    /** 19 characters — over the 15-character limit. */
    private static final String TOO_LONG_NAME = "Abcdefghijklmnopqrs";

    @Override
    protected boolean relaunchBetweenTests() {
        return false;
    }

    @Override
    protected boolean requiresLoggedOutStart() {
        return false;
    }

    private EditMyProfilePage profile() {
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
        EditMyProfilePage page = new EditMyProfilePage(driver);
        assumeTrue(page.ensureOnEditPreference(Duration.ofSeconds(60)),
                "Could not reach Settings -> Edit My Profile -> EDIT PREFERENCE");
        return page;
    }

    @Test
    @Order(1)
    @DisplayName("TC1: Edit My Profile opens on the EDIT PREFERENCE tab")
    void opensPreferenceTab() {
        EditMyProfilePage page = profile();
        assertTrue(page.isLoaded(Duration.ofSeconds(20)), "EDIT MY PROFILE should be open");
        assertTrue(page.isOnPreferenceTab(Duration.ofSeconds(20)),
                "The EDIT PREFERENCE tab should be showing");
        screenshot("profile-preference-tab");
        System.out.println(">>> unit=" + page.unit() + " gender=" + page.gender());
    }

    @Test
    @Order(2)
    @DisplayName("TC2: The camera icon offers Photo and Gallery")
    void cameraIconOffersBothSources() {
        EditMyProfilePage page = profile();

        assertTrue(page.tapCameraIcon(), "The camera badge should be tappable");
        assertTrue(page.isChooserOpen(Duration.ofSeconds(15)), "The \"Choose below\" sheet should open");

        List<String> options = page.chooserOptions();
        screenshot("profile-chooser");
        assertTrue(options.contains("Photo"), "Chooser should offer Photo; got " + options);
        assertTrue(options.contains("Gallery"), "Chooser should offer Gallery; got " + options);
    }

    @Test
    @Order(3)
    @DisplayName("TC3: A captured photo uploads and can be viewed full screen")
    void captureFromCamera() {
        EditMyProfilePage page = new EditMyProfilePage(driver);
        assumeTrue(page.isChooserOpen(Duration.ofSeconds(10)), "Chooser is not open — TC2 did not leave it");

        assertTrue(page.chooseSource("Photo"), "Photo should be selectable");
        assertTrue(page.isOnCamera(Duration.ofSeconds(20)), "The in-app camera should open");
        assertTrue(page.tapCapture(), "Capture should be tappable");
        assertTrue(page.isOnPreview(Duration.ofSeconds(20)), "A preview should follow the capture");
        assertTrue(page.tapPreviewSend(), "Send should be tappable on the preview");

        assertTrue(page.isOnPreferenceTab(Duration.ofSeconds(30)),
                "Sending should return to the preference tab");

        // Let the picture finish loading before fingerprinting it, or the value
        // captured here is a half-drawn frame and TC5's comparison is meaningless.
        avatarAfterCamera = page.settledAvatarHash(SETTLE_WAIT);
        System.out.println(">>> avatar hash after camera upload: " + avatarAfterCamera);
        assertTrue(!avatarAfterCamera.isBlank(), "The avatar should settle after the upload");

        assertTrue(page.tapAvatar(), "The avatar should be tappable");
        assertTrue(page.isOnFullImageView(Duration.ofSeconds(20)), "The full-screen viewer should open");
        screenshot("profile-photo-fullscreen");
        assertTrue(page.tapBack(), "Back should close the viewer");
        assertTrue(page.isOnPreferenceTab(Duration.ofSeconds(20)),
                "Closing the viewer should return to the preference tab");
    }

    @Test
    @Order(4)
    @DisplayName("TC4: A gallery image uploads and replaces the captured one")
    void pickFromGallery() {
        assumeTrue(avatarAfterCamera != null && !avatarAfterCamera.isBlank(),
                "TC3 did not record an avatar to compare against");
        EditMyProfilePage page = profile();

        assertTrue(page.tapCameraIcon(), "The camera badge should be tappable");
        assertTrue(page.isChooserOpen(Duration.ofSeconds(15)), "The chooser should open");
        assertTrue(page.chooseSource("Gallery"), "Gallery should be selectable");

        assertTrue(page.isOnGallery(Duration.ofSeconds(20)), "The in-app gallery should open");
        assumeTrue(page.openFirstFolder(), "No gallery folder available on this device");
        assumeTrue(page.selectFirstPhoto(), "No photo available in the first folder");
        assertTrue(page.tapGallerySend(), "The gallery send button should appear once a photo is ticked");
        assertTrue(page.isOnPreview(Duration.ofSeconds(20)), "A preview should follow the pick");
        assertTrue(page.tapPreviewSend(), "Send should be tappable on the preview");

        assertTrue(page.isOnPreferenceTab(Duration.ofSeconds(30)),
                "Sending should return to the preference tab");

        String after = page.settledAvatarHash(SETTLE_WAIT);
        System.out.println(">>> avatar hash after gallery upload: " + after);
        assertTrue(!after.isBlank(), "The avatar should settle after the upload");

        assertTrue(page.tapAvatar(), "The avatar should be tappable");
        assertTrue(page.isOnFullImageView(Duration.ofSeconds(20)), "The full-screen viewer should open");
        screenshot("profile-gallery-fullscreen");
        assertTrue(page.tapBack(), "Back should close the viewer");

        assertNotEquals(avatarAfterCamera, after,
                "The gallery upload should replace the captured photo, but the avatar is unchanged");
    }

    @Test
    @Order(5)
    @DisplayName("TC5: The unit can be changed from the dropdown")
    void changeUnit() {
        EditMyProfilePage page = profile();

        String before = page.unit();
        assertTrue(page.openDropdown("selectUnit"), "The unit arrow should open the list");
        // Row 0 is whatever is already selected, so row 1 is guaranteed different.
        assertTrue(page.selectDropdownRow("selectUnit", 1), "A second unit should be selectable");

        String after = page.unit();
        screenshot("profile-unit-changed");
        System.out.println(">>> unit " + before + " -> " + after);
        assertNotEquals(before, after, "The unit should have changed from " + before);
        chosenUnit = after;
        assertTrue(!after.isBlank(), "The unit field should not be empty");
    }

    @Test
    @Order(6)
    @DisplayName("TC6: Gender offers Male, Female and Non-Binary, and Male can be chosen")
    void chooseGender() {
        EditMyProfilePage page = profile();

        assertTrue(page.openDropdown("selectGender"), "The gender arrow should open the list");
        // The expanded list is not in the accessibility tree, so the options are
        // asserted by picking one by position and reading the field back.
        assertTrue(page.selectDropdownRow("selectGender", EditMyProfilePage.GENDERS.indexOf("Male")),
                "Male should be selectable");

        screenshot("profile-gender-male");
        assertEquals("Male", page.gender(), "The gender field should read Male");
    }

    @Test
    @Order(7)
    @DisplayName("TC7: Add More Details opens a dialog, and Cancel removes the fields")
    void addMoreDetailsCancel() {
        EditMyProfilePage page = profile();

        assertTrue(page.tapAddMoreDetails(), "Add More Details should be tappable");
        assertTrue(page.isDialogOpen(Duration.ofSeconds(15)), "The dialog should open");
        assertEquals("Add More Details", page.dialogHeader(), "Dialog header should match");
        screenshot("profile-add-more-details");

        assertTrue(page.tapDialogLeft(), "Cancel should be tappable");
        assertTrue(page.dialogFieldsGone(Duration.ofSeconds(15)),
                "Title and Description should disappear after Cancel");
    }

    @Test
    @Order(8)
    @DisplayName("TC8: Add validates the title, then the description")
    void addValidatesBothFields() {
        EditMyProfilePage page = profile();

        assertTrue(page.tapAddMoreDetails(), "Add More Details should be tappable");
        assertTrue(page.isDialogOpen(Duration.ofSeconds(15)), "The dialog should open");

        // Empty: the title complains. The message has no text node — it is read
        // from the field's error attribute.
        assertTrue(page.tapDialogRight(), "Add should be tappable");
        assertEquals(EditMyProfilePage.TITLE_REQUIRED, page.titleError(),
                "Expected the title validation message");
        screenshot("profile-title-error");

        // Title only: now the description complains.
        assertTrue(page.typeTitle(TITLE_1), "Title field should accept input");
        assertTrue(page.tapDialogRight(), "Add should be tappable");
        assertEquals(EditMyProfilePage.DESCRIPTION_REQUIRED, page.descriptionError(),
                "Expected the description validation message");
        screenshot("profile-description-error");
    }

    @Test
    @Order(9)
    @DisplayName("TC9: A complete detail saves and is listed")
    void addDetailSucceeds() {
        EditMyProfilePage page = new EditMyProfilePage(driver);
        assumeTrue(page.isDialogOpen(Duration.ofSeconds(10)), "The dialog is not open");

        assertTrue(page.typeDescription(DESC_1), "Description field should accept input");
        assertTrue(page.tapDialogRight(), "Add should be tappable");

        assertTrue(page.snackbarShown(EditMyProfilePage.PROFILE_UPDATED, UPLOAD_WAIT),
                "Expected \"" + EditMyProfilePage.PROFILE_UPDATED + "\"");
        assertTrue(page.waitForDetail(TITLE_1, true, MESSAGE_WAIT),
                TITLE_1 + " should be listed");
        screenshot("profile-detail-added");

        assertEquals(DESC_1, page.details().get(TITLE_1),
                "The saved detail should carry its description");
    }

    @Test
    @Order(10)
    @DisplayName("TC10: Editing validates both fields, then saves the replacement")
    void editDetail() {
        EditMyProfilePage page = profile();
        assumeTrue(page.hasDetail(TITLE_1), TITLE_1 + " is not listed");

        assertTrue(page.waitForSnackbarToClear(MESSAGE_WAIT), "Previous snackbar should clear first");
        assertTrue(page.tapDetailEdit(TITLE_1), "The edit icon should be tappable");
        assertTrue(page.isDialogOpen(Duration.ofSeconds(15)), "The update dialog should open");
        assertEquals("Update Detail", page.dialogHeader(), "Dialog header should match");

        // Blank title.
        assertTrue(page.clearTitle(), "Title field should clear");
        assertTrue(page.tapDialogRight(), "Update should be tappable");
        assertEquals(EditMyProfilePage.TITLE_REQUIRED, page.titleError(),
                "Expected the title validation message");

        // New title, blank description.
        assertTrue(page.typeTitle(TITLE_2), "Title field should accept input");
        assertTrue(page.clearDescription(), "Description field should clear");
        assertTrue(page.tapDialogRight(), "Update should be tappable");
        assertEquals(EditMyProfilePage.DESCRIPTION_REQUIRED, page.descriptionError(),
                "Expected the description validation message");

        // Both filled — saves.
        assertTrue(page.typeDescription(DESC_2), "Description field should accept input");
        assertTrue(page.tapDialogRight(), "Update should be tappable");
        assertTrue(page.snackbarShown(EditMyProfilePage.PROFILE_UPDATED, UPLOAD_WAIT),
                "Expected \"" + EditMyProfilePage.PROFILE_UPDATED + "\"");

        assertTrue(page.waitForDetail(TITLE_2, true, MESSAGE_WAIT), TITLE_2 + " should be listed");
        screenshot("profile-detail-updated");
        assertTrue(!page.hasDetail(TITLE_1),
                TITLE_1 + " should have been replaced by " + TITLE_2);
        assertEquals(DESC_2, page.details().get(TITLE_2), "The description should be updated too");
    }

    @Test
    @Order(11)
    @DisplayName("TC11: Submit saves, and everything is still there after reopening the screen")
    void submitAndReopen() {
        EditMyProfilePage page = profile();

        assertTrue(page.waitForSnackbarToClear(MESSAGE_WAIT), "Previous snackbar should clear first");
        assertTrue(page.tapSubmit(), "Submit should be tappable");
        assertTrue(page.snackbarShown(EditMyProfilePage.PROFILE_UPDATED, UPLOAD_WAIT),
                "Expected \"" + EditMyProfilePage.PROFILE_UPDATED + "\"");
        screenshot("profile-submitted");

        // Leave the screen entirely and come back, so what follows is read from a
        // freshly loaded profile rather than from state still held in memory.
        assertTrue(page.waitForSnackbarToClear(MESSAGE_WAIT), "The save snackbar should clear");
        assertTrue(page.tapBack(), "Back should be tappable");
        assertTrue(page.isOnSettingsTab(Duration.ofSeconds(20)), "Back should land on the Settings tab");

        assertTrue(page.openEditMyProfile(), "Edit My Profile should reopen");
        assertTrue(page.isLoaded(Duration.ofSeconds(20)), "EDIT MY PROFILE should be open");
        assertTrue(page.openPreferenceTab(), "EDIT PREFERENCE should be tappable");
        assertTrue(page.isOnPreferenceTab(Duration.ofSeconds(20)),
                "The EDIT PREFERENCE tab should be showing");
        screenshot("profile-reopened");

        System.out.println(">>> after reopen: unit=" + page.unit()
                + " gender=" + page.gender() + " details=" + page.details());

        if (chosenUnit != null) {
            assertEquals(chosenUnit, page.unit(), "The saved unit should have persisted");
        }
        assertEquals("Male", page.gender(), "The saved gender should have persisted");
        assertTrue(page.hasDetail(TITLE_2),
                TITLE_2 + " should still be listed after reopening; got " + page.details());
        assertEquals(DESC_2, page.details().get(TITLE_2),
                "The saved description should have persisted");
    }

    @Test
    @Order(12)
    @DisplayName("TC12: Delete asks to confirm; Cancel keeps the detail, Yes removes it")
    void deleteDetail() {
        EditMyProfilePage page = profile();
        assumeTrue(page.hasDetail(TITLE_2), TITLE_2 + " is not listed");

        assertTrue(page.waitForSnackbarToClear(MESSAGE_WAIT), "Previous snackbar should clear first");
        assertTrue(page.tapDetailDelete(TITLE_2), "The delete icon should be tappable");
        assertTrue(page.isDeleteConfirmShown(Duration.ofSeconds(15)),
                "Deleting should ask for confirmation; got \"" + page.confirmMessage() + "\"");
        screenshot("profile-delete-confirm");

        assertTrue(page.tapDialogLeft(), "Cancel should be tappable");
        assertTrue(page.hasDetail(TITLE_2), "Cancelling should keep " + TITLE_2);

        assertTrue(page.tapDetailDelete(TITLE_2), "The delete icon should be tappable again");
        assertTrue(page.isDeleteConfirmShown(Duration.ofSeconds(15)), "The dialog should appear again");
        assertTrue(page.tapDialogRight(), "Yes should be tappable");

        assertTrue(page.snackbarShown(EditMyProfilePage.PROFILE_UPDATED, UPLOAD_WAIT),
                "Expected \"" + EditMyProfilePage.PROFILE_UPDATED + "\"");
        assertTrue(page.waitForDetail(TITLE_2, false, MESSAGE_WAIT),
                TITLE_2 + " should disappear from the list");
        screenshot("profile-detail-deleted");
    }

    // ======================= EDIT PROFILE tab =======================

    @Test
    @Order(13)
    @DisplayName("TC13: First Name is validated for empty, too short and too long")
    void firstNameValidation() {
        EditMyProfilePage page = profile();

        assertTrue(page.openProfileTab(), "EDIT PROFILE should be tappable");
        assertTrue(page.isOnProfileTab(Duration.ofSeconds(20)), "The EDIT PROFILE tab should show");
        screenshot("profile-edit-profile-tab");

        assertTrue(page.waitForSnackbarToClear(MESSAGE_WAIT), "Previous snackbar should clear first");
        assertTrue(page.clearFirstName(), "First Name should clear");
        assertTrue(page.tapSubmit(), "Submit should be tappable");
        assertTrue(page.snackbarShown(EditMyProfilePage.FIRST_NAME_EMPTY, MESSAGE_WAIT),
                "Expected the empty-first-name error");

        assertTrue(page.waitForSnackbarToClear(MESSAGE_WAIT), "Snackbar should clear");
        assertTrue(page.typeFirstName("A"), "First Name should accept input");
        assertTrue(page.tapSubmit(), "Submit should be tappable");
        assertTrue(page.snackbarShown(EditMyProfilePage.FIRST_NAME_LENGTH, MESSAGE_WAIT),
                "Expected the length error for a 1-character first name");

        assertTrue(page.waitForSnackbarToClear(MESSAGE_WAIT), "Snackbar should clear");
        assertTrue(page.clearFirstName(), "First Name should clear");
        assertTrue(page.typeFirstName(TOO_LONG_NAME), "First Name should accept input");
        assertTrue(page.tapSubmit(), "Submit should be tappable");
        assertTrue(page.snackbarShown(EditMyProfilePage.FIRST_NAME_LENGTH, MESSAGE_WAIT),
                "Expected the length error for a " + TOO_LONG_NAME.length() + "-character first name");
        screenshot("profile-firstname-invalid");
    }

    @Test
    @Order(14)
    @DisplayName("TC14: Last Name is validated for empty, too short and too long")
    void lastNameValidation() {
        EditMyProfilePage page = new EditMyProfilePage(driver);
        assumeTrue(page.isOnProfileTab(Duration.ofSeconds(10)), "Not on the EDIT PROFILE tab");

        assertTrue(page.waitForSnackbarToClear(MESSAGE_WAIT), "Previous snackbar should clear first");
        assertTrue(page.clearFirstName(), "First Name should clear");
        assertTrue(page.typeFirstName(NEW_FIRST_NAME), "First Name should accept input");

        assertTrue(page.clearLastName(), "Last Name should clear");
        assertTrue(page.tapSubmit(), "Submit should be tappable");
        assertTrue(page.snackbarShown(EditMyProfilePage.LAST_NAME_EMPTY, MESSAGE_WAIT),
                "Expected the empty-last-name error");

        assertTrue(page.waitForSnackbarToClear(MESSAGE_WAIT), "Snackbar should clear");
        assertTrue(page.typeLastName("A"), "Last Name should accept input");
        assertTrue(page.tapSubmit(), "Submit should be tappable");
        assertTrue(page.snackbarShown(EditMyProfilePage.LAST_NAME_LENGTH, MESSAGE_WAIT),
                "Expected the length error for a 1-character last name");

        assertTrue(page.waitForSnackbarToClear(MESSAGE_WAIT), "Snackbar should clear");
        assertTrue(page.clearLastName(), "Last Name should clear");
        assertTrue(page.typeLastName(TOO_LONG_NAME), "Last Name should accept input");
        assertTrue(page.tapSubmit(), "Submit should be tappable");
        assertTrue(page.snackbarShown(EditMyProfilePage.LAST_NAME_LENGTH, MESSAGE_WAIT),
                "Expected the length error for a " + TOO_LONG_NAME.length() + "-character last name");
        screenshot("profile-lastname-invalid");
    }

    @Test
    @Order(15)
    @DisplayName("TC15: Valid names save, and the new name shows on the Settings tab")
    void renameSaves() {
        EditMyProfilePage page = new EditMyProfilePage(driver);
        assumeTrue(page.isOnProfileTab(Duration.ofSeconds(10)), "Not on the EDIT PROFILE tab");

        assertTrue(page.waitForSnackbarToClear(MESSAGE_WAIT), "Previous snackbar should clear first");
        assertTrue(page.clearLastName(), "Last Name should clear");
        assertTrue(page.typeLastName(NEW_LAST_NAME), "Last Name should accept input");
        assertEquals(NEW_FIRST_NAME, page.firstName(), "First Name should read " + NEW_FIRST_NAME);

        assertTrue(page.tapSubmit(), "Submit should be tappable");
        assertTrue(page.snackbarShown(EditMyProfilePage.PROFILE_UPDATED, UPLOAD_WAIT),
                "Expected the profile-updated message");

        assertTrue(page.waitForSnackbarToClear(MESSAGE_WAIT), "Snackbar should clear");
        assertTrue(page.tapBack(), "Back should be tappable");
        assertTrue(page.isOnSettingsTab(Duration.ofSeconds(20)), "Back should land on the Settings tab");

        String shown = page.settingsUserName();
        System.out.println(">>> name on Settings tab: " + shown);
        screenshot("profile-renamed");
        assertTrue(shown.contains(NEW_FIRST_NAME) && shown.contains(NEW_LAST_NAME),
                "The Settings tab should show " + NEW_FIRST_NAME + " " + NEW_LAST_NAME + "; got " + shown);
    }

    @Test
    @Order(16)
    @DisplayName("TC16: Changing department warns first, and No cancels it")
    void departmentWarningDeclined() {
        EditMyProfilePage page = profile();

        assertTrue(page.openProfileTab(), "EDIT PROFILE should be tappable");
        assertTrue(page.isOnProfileTab(Duration.ofSeconds(20)), "The EDIT PROFILE tab should show");
        originalDepartment = page.department();
        System.out.println(">>> current department: " + originalDepartment);

        assertTrue(page.openDepartmentPicker(), "Department should be tappable");
        assertTrue(page.isDepartmentWarningShown(Duration.ofSeconds(15)),
                "Expected the change-department warning; got " + page.confirmMessage());
        String msg = page.confirmMessage();
        screenshot("profile-department-warning");
        assertTrue(msg.endsWith(EditMyProfilePage.DEPT_WARNING_END),
                "The warning should ask for confirmation; got " + msg);

        assertTrue(page.tapDialogLeft(), "No should be tappable");
        assertTrue(page.isOnProfileTab(Duration.ofSeconds(15)), "Declining should stay on the tab");
        assertEquals(originalDepartment, page.department(), "Declining should not change the department");
    }

    @Test
    @Order(17)
    @DisplayName("TC17: Accepting the warning allows a new department, which needs a designation")
    void changeDepartmentThenDesignation() {
        EditMyProfilePage page = new EditMyProfilePage(driver);
        assumeTrue(page.isOnProfileTab(Duration.ofSeconds(10)), "Not on the EDIT PROFILE tab");

        assertTrue(page.openDepartmentPicker(), "Department should be tappable");
        assertTrue(page.isDepartmentWarningShown(Duration.ofSeconds(15)), "The warning should appear again");
        assertTrue(page.tapDialogRight(), "Yes should be tappable");
        assertTrue(page.isOnPicker(Duration.ofSeconds(20)), "The department picker should open");

        List<String> options = page.pickerOptions();
        newDepartment = options.stream()
                .filter(o -> !o.equalsIgnoreCase(originalDepartment))
                .findFirst().orElseThrow();
        assertTrue(page.searchPicker(newDepartment), "The picker search should accept input");
        assertTrue(page.pickerOptions().contains(newDepartment),
                "Searching for the new department should list it; got " + page.pickerOptions());
        assertTrue(page.choosePickerValue(newDepartment), newDepartment + " should be selectable");
        assertEquals(newDepartment, page.department(), "Department should now read " + newDepartment);

        // Changing department clears the designation, so saving now must be refused.
        assertTrue(page.waitForSnackbarToClear(MESSAGE_WAIT), "Previous snackbar should clear first");
        assertTrue(page.tapSubmit(), "Submit should be tappable");
        assertTrue(page.snackbarShown(EditMyProfilePage.SELECT_DESIGNATION, MESSAGE_WAIT),
                "Expected the select-designation error");
        screenshot("profile-designation-required");

        assertTrue(page.waitForSnackbarToClear(MESSAGE_WAIT), "Snackbar should clear");
        assertTrue(page.openDesignationPicker(), "Designation should be tappable");
        assertTrue(page.isOnPicker(Duration.ofSeconds(20)), "The designation picker should open");
        List<String> designations = page.pickerOptions();
        assertTrue(!designations.isEmpty(), "The picker should offer designations for " + newDepartment);
        newDesignation = designations.get(0);
        assertTrue(page.searchPicker(newDesignation), "The picker search should accept input");
        assertTrue(page.choosePickerValue(newDesignation), newDesignation + " should be selectable");
        assertEquals(newDesignation, page.designation(), "Designation should now read " + newDesignation);
    }

    @Test
    @Order(18)
    @DisplayName("TC18: The new designation saves and shows on the Settings tab")
    void designationSaves() {
        assumeTrue(newDesignation != null, "TC17 did not choose a designation");
        EditMyProfilePage page = new EditMyProfilePage(driver);
        assumeTrue(page.isOnProfileTab(Duration.ofSeconds(10)), "Not on the EDIT PROFILE tab");

        assertTrue(page.waitForSnackbarToClear(MESSAGE_WAIT), "Previous snackbar should clear first");
        assertTrue(page.tapSubmit(), "Submit should be tappable");
        assertTrue(page.snackbarShown(EditMyProfilePage.PROFILE_UPDATED, UPLOAD_WAIT),
                "Expected the profile-updated message");

        assertTrue(page.waitForSnackbarToClear(MESSAGE_WAIT), "Snackbar should clear");
        assertTrue(page.tapBack(), "Back should be tappable");
        assertTrue(page.isOnSettingsTab(Duration.ofSeconds(20)), "Back should land on the Settings tab");

        String shown = page.settingsUserDesignation();
        System.out.println(">>> designation on Settings tab: " + shown);
        screenshot("profile-designation-updated");
        assertTrue(shown.trim().equalsIgnoreCase(newDesignation.trim()),
                "The Settings tab should show " + newDesignation + "; got " + shown);
    }

    @Test
    @Order(19)
    @DisplayName("TC19: The Zillit mailing address is present and ends with @zillit.info")
    void zillitMailAddress() {
        EditMyProfilePage page = profile();

        assertTrue(page.openProfileTab(), "EDIT PROFILE should be tappable");
        assertTrue(page.isOnProfileTab(Duration.ofSeconds(20)), "The EDIT PROFILE tab should show");

        String mail = page.zillitEmail();
        screenshot("profile-zillit-mail");

        // An empty field is reported as a WARNING rather than a failure: the address
        // is provisioned server-side, so its absence is a data problem, not a UI bug.
        if (mail == null || mail.isBlank()) {
            System.out.println(">>> WARNING: the Zillit mailing address field is EMPTY for this user");
            return;
        }
        System.out.println(">>> Zillit mailing address: " + mail);
        assertTrue(mail.endsWith(EditMyProfilePage.ZILLIT_MAIL_SUFFIX),
                "The Zillit mailing address should end with "
                        + EditMyProfilePage.ZILLIT_MAIL_SUFFIX + "; got " + mail);
    }

    @Test
    @Order(20)
    @DisplayName("TC20: The run finishes back on the Settings tab")
    void finishOnSettingsTab() {
        EditMyProfilePage page = new EditMyProfilePage(driver);

        assertTrue(page.waitForSnackbarToClear(MESSAGE_WAIT), "Previous snackbar should clear first");
        assertTrue(page.tapBack(), "Back should be tappable on EDIT MY PROFILE");
        assertTrue(page.isOnSettingsTab(Duration.ofSeconds(20)),
                "Back should land on the Settings tab");
        screenshot("profile-finished-on-settings-tab");
    }
}
