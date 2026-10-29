package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.ProjectListPage;
import com.zillit.automation.pages.SosReceiversPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Settings → Admin Settings → "Set/View SOS Receivers" (Member tab).
 *
 * <p>Runs the whole receiver lifecycle on one screen instance
 * ({@code relaunchBetweenTests()=false}): validate the empty form, pick a member
 * through the SELECT USERS picker, save, edit, delete, and add again.
 *
 * <p><b>No user is hard-coded.</b> The project's members change between runs, so
 * the member exercised here — and the designation searched for — are read from
 * the picker at the start and reused by the later cases. When the picker is empty
 * the cases skip rather than fail.
 *
 * <p>The suite is <b>self-restoring</b>: the final case adds a receiver back, so
 * it finishes with the project in the state it started in.
 *
 * <p>Behaviour confirmed on the device while building this:
 * <ul>
 *   <li>Submit on an empty form raises the snackbar "Please fill all details.";</li>
 *   <li>the picker filters on DESIGNATION as well as name, despite its
 *       "Search By User Name" hint;</li>
 *   <li>choosing a member needs the picker's OWN Submit before the form updates;</li>
 *   <li>saving resets both form fields to their placeholders;</li>
 *   <li>delete is immediate — there is no confirmation dialog.</li>
 * </ul>
 */
@Order(TestOrder.SOS_RECEIVERS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SosReceiversTest extends BaseTest {

    /** Alert shown on the Outsider tab when the profile has no mobile number. */
    private static final String MOBILE_NOT_UPDATED =
            "Your Mobile Number is not updated. You need to fill in your contact details "
                    + "so that the outsider can get in touch with you.";

    // ---- Outsider fixture. Names are this suite's own data, not project users,
    // so unlike the member they are safe to fix.
    private static final String COUNTRY = "India";
    private static final String COUNTRY_CODE = "+91";
    private static final String VALID_NUMBER = "9501183278";
    private static final String TOO_LONG_NUMBER = "1234567890123456789012345678901";  // 31 digits
    private static final String OUTSIDER_NAME = "Harsh";
    private static final String RENAMED = "Ramu";
    private static final String OUTSIDER_PHONE = "95011832";
    private static final String RELATION = "Security";

    private static final Duration SNACKBAR_WAIT = Duration.ofSeconds(10);
    private static final Duration LIST_WAIT = Duration.ofSeconds(30);

    private boolean insideProject;

    // ---- read from the live picker in TC3 ----
    private static String memberName;
    private static String memberDesignation;
    /** Set when TC12 actually saw the alert, so TC13 knows the screen is reachable. */
    private static boolean mobileAlertSeen;

    @Override
    protected boolean relaunchBetweenTests() {
        return false;
    }

    @Override
    protected boolean requiresLoggedOutStart() {
        return false;
    }

    /** Returns the SOS receivers screen, navigating (or recovering) to it as needed. */
    private SosReceiversPage sos() {
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
        SosReceiversPage page = new SosReceiversPage(driver);
        assumeTrue(page.ensureOnSosReceivers(Duration.ofSeconds(60)),
                "Could not reach Settings -> Admin Settings -> Set/View SOS Receivers");
        return page;
    }

    @Test
    @Order(1)
    @DisplayName("TC1: Admin Settings -> Set/View SOS Receivers opens")
    void opensSosReceivers() {
        SosReceiversPage page = sos();
        assertTrue(page.isLoaded(Duration.ofSeconds(20)), "SET/VIEW SOS RECEIVERS should be open");
        screenshot("sos-receivers-open");
        System.out.println(">>> existing receivers: " + page.receivers());
    }

    @Test
    @Order(2)
    @DisplayName("TC2: Submit with nothing filled shows \"Please fill all details.\"")
    void submitEmptyShowsValidation() {
        SosReceiversPage page = sos();

        assertTrue(page.tapSubmit(), "Submit should be tappable");
        assertTrue(page.snackbarShown(SosReceiversPage.FILL_ALL_DETAILS, SNACKBAR_WAIT),
                "Expected the snackbar \"" + SosReceiversPage.FILL_ALL_DETAILS + "\"");
        screenshot("sos-receivers-empty-submit");
    }

    @Test
    @Order(3)
    @DisplayName("TC3: The Select Member picker lists members with designations")
    void openMemberPicker() {
        SosReceiversPage page = sos();

        assertTrue(page.openMemberPicker(), "Select Member field should be tappable");
        assertTrue(page.isOnMemberPicker(Duration.ofSeconds(20)), "SELECT USERS picker should open");

        Map<String, String> members = page.pickerMembers();
        screenshot("sos-receivers-picker");
        System.out.println(">>> members (" + members.size() + "): " + members);

        assumeTrue(!members.isEmpty(), "No members available to add as an SOS receiver");

        // Prefer a member that has a designation, since TC4 searches on it.
        memberName = members.entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isBlank())
                .map(Map.Entry::getKey)
                .findFirst().orElse(members.keySet().iterator().next());
        memberDesignation = members.get(memberName);
        System.out.println(">>> using member: " + memberName + " (" + memberDesignation + ")");
    }

    @Test
    @Order(4)
    @DisplayName("TC4: Searching a DESIGNATION filters the picker to that member")
    void searchByDesignation() {
        assumeTrue(memberName != null, "No member captured in TC3");
        assumeTrue(memberDesignation != null && !memberDesignation.isBlank(),
                "Chosen member has no designation to search on");
        SosReceiversPage page = new SosReceiversPage(driver);
        assumeTrue(page.isOnMemberPicker(Duration.ofSeconds(10)), "Not on the SELECT USERS picker");

        assertTrue(page.searchMember(memberDesignation), "Search box should accept input");
        Map<String, String> filtered = page.pickerMembers();
        screenshot("sos-receivers-search-designation");

        assertTrue(filtered.containsKey(memberName),
                "Searching '" + memberDesignation + "' should still show " + memberName
                        + "; got " + filtered.keySet());
        assertTrue(filtered.values().stream()
                        .allMatch(d -> d != null && d.toLowerCase().contains(memberDesignation.toLowerCase())),
                "Every remaining row should match the designation searched; got " + filtered);
    }

    @Test
    @Order(5)
    @DisplayName("TC5: Clearing the search restores the picker list")
    void clearSearch() {
        assumeTrue(memberName != null, "No member captured in TC3");
        SosReceiversPage page = new SosReceiversPage(driver);
        assumeTrue(page.isOnMemberPicker(Duration.ofSeconds(10)), "Not on the SELECT USERS picker");

        assertTrue(page.clearMemberSearch(), "Search box should clear");
        assertTrue(page.pickerMembers().containsKey(memberName),
                "Clearing the search should show " + memberName + " again");
        screenshot("sos-receivers-search-cleared");
    }

    @Test
    @Order(6)
    @DisplayName("TC6: Searching the NAME filters the picker to that member")
    void searchByName() {
        assumeTrue(memberName != null, "No member captured in TC3");
        SosReceiversPage page = new SosReceiversPage(driver);
        assumeTrue(page.isOnMemberPicker(Duration.ofSeconds(10)), "Not on the SELECT USERS picker");

        assertTrue(page.searchMember(memberName), "Search box should accept input");
        Map<String, String> filtered = page.pickerMembers();
        screenshot("sos-receivers-search-name");

        assertTrue(filtered.containsKey(memberName),
                "Searching '" + memberName + "' should show that member; got " + filtered.keySet());
        assertEquals(1, filtered.size(),
                "A full-name search should leave a single row; got " + filtered.keySet());
    }

    @Test
    @Order(7)
    @DisplayName("TC7: Choosing the member fills the name and designation fields")
    void chooseMemberFillsForm() {
        assumeTrue(memberName != null, "No member captured in TC3");
        SosReceiversPage page = new SosReceiversPage(driver);
        assumeTrue(page.isOnMemberPicker(Duration.ofSeconds(10)), "Not on the SELECT USERS picker");

        assertTrue(page.selectMember(memberName), memberName + " row should be tappable");
        assertTrue(page.memberIsTicked(Duration.ofSeconds(10)),
                "The chosen row should show its tick");

        // The picker has its OWN Submit; the form does not update until it is used.
        assertTrue(page.confirmMemberSelection(), "Picker Submit should be tappable");
        assertTrue(page.isLoaded(Duration.ofSeconds(20)),
                "Confirming should return to SET/VIEW SOS RECEIVERS");

        screenshot("sos-receivers-member-chosen");
        assertEquals(memberName, page.memberField(),
                "The member field should show " + memberName);
        assertEquals(memberDesignation, page.designationField(),
                "The designation field should show " + memberDesignation);
    }

    @Test
    @Order(8)
    @DisplayName("TC8: Submit adds the receiver with edit and delete icons and resets the form")
    void submitAddsReceiver() {
        assumeTrue(memberName != null, "No member captured in TC3");
        SosReceiversPage page = new SosReceiversPage(driver);
        assumeTrue(page.isLoaded(Duration.ofSeconds(10)), "Not on the SOS receivers screen");

        assertTrue(page.tapSubmit(), "Submit should be tappable");
        assertTrue(page.waitUntilReceiverListed(memberName, LIST_WAIT),
                memberName + " should appear in the receivers list");

        screenshot("sos-receivers-added");
        System.out.println(">>> receivers after add: " + page.receivers());

        assertTrue(page.receiverHasEditAndDelete(memberName),
                memberName + " should have both an edit and a delete icon");
        assertTrue(page.formIsReset(),
                "The form should reset after saving; member field = '" + page.memberField()
                        + "', designation = '" + page.designationField() + "'");
    }

    @Test
    @Order(9)
    @DisplayName("TC9: The edit icon opens the receiver, and back returns to the list")
    void editThenBack() {
        assumeTrue(memberName != null, "No member captured in TC3");
        SosReceiversPage page = new SosReceiversPage(driver);
        assumeTrue(page.isLoaded(Duration.ofSeconds(10)), "Not on the SOS receivers screen");
        assumeTrue(page.isReceiverListed(memberName), memberName + " is not listed");

        assertTrue(page.tapEditIcon(memberName), "Edit icon should be tappable");
        assertTrue(page.isOnEditScreen(Duration.ofSeconds(20)), "EDIT SOS RECEIVERS should open");
        screenshot("sos-receivers-edit");
        assertEquals(memberName, page.memberField(),
                "The edit screen should be prefilled with " + memberName);

        assertTrue(page.tapBack(), "Back should be tappable on the edit screen");
        assertTrue(page.isLoaded(Duration.ofSeconds(20)),
                "Back should return to SET/VIEW SOS RECEIVERS");
        assertTrue(page.isReceiverListed(memberName),
                memberName + " should still be listed after leaving the edit screen");
    }

    @Test
    @Order(10)
    @DisplayName("TC10: Delete removes the receiver from the list")
    void deleteReceiver() {
        assumeTrue(memberName != null, "No member captured in TC3");
        SosReceiversPage page = new SosReceiversPage(driver);
        assumeTrue(page.isLoaded(Duration.ofSeconds(10)), "Not on the SOS receivers screen");
        assumeTrue(page.isReceiverListed(memberName), memberName + " is not listed");

        // No confirmation dialog — the row goes as soon as the icon is tapped.
        assertTrue(page.tapDeleteIcon(memberName), "Delete icon should be tappable");
        assertTrue(page.waitUntilReceiverGone(memberName, LIST_WAIT),
                memberName + " should disappear from the receivers list");
        screenshot("sos-receivers-deleted");
        System.out.println(">>> receivers after delete: " + page.receivers());
    }

    @Test
    @Order(11)
    @DisplayName("TC11: The member can be added again after deletion")
    void addAgainAfterDelete() {
        assumeTrue(memberName != null, "No member captured in TC3");
        SosReceiversPage page = sos();

        assertTrue(page.openMemberPicker(), "Select Member field should be tappable");
        assertTrue(page.isOnMemberPicker(Duration.ofSeconds(20)), "SELECT USERS picker should open");
        assertTrue(page.selectMember(memberName), memberName + " row should be tappable");
        assertTrue(page.confirmMemberSelection(), "Picker Submit should be tappable");
        assertTrue(page.isLoaded(Duration.ofSeconds(20)), "Should return to the receivers screen");

        assertEquals(memberName, page.memberField(),
                "The member field should show " + memberName + " again");
        assertTrue(page.tapSubmit(), "Submit should be tappable");
        assertTrue(page.waitUntilReceiverListed(memberName, LIST_WAIT),
                memberName + " should be back in the receivers list");

        screenshot("sos-receivers-re-added");
        assertTrue(page.receiverHasEditAndDelete(memberName),
                "The re-added receiver should have both icons");
        assertTrue(page.formIsReset(), "The form should reset again after saving");
    }

    // ======================= OUTSIDER TAB =======================

    @Test
    @Order(12)
    @DisplayName("TC12: Outsider warns when the profile has no mobile number, and Cancel returns to Member")
    void outsiderMobileAlertCancel() {
        SosReceiversPage page = sos();

        assertTrue(page.openOutsiderTab(), "Outsider tab should be tappable");
        // One-shot per account: once a number is saved the app never asks again,
        // so an absent alert is normal rather than a failure.
        boolean alert = page.isMobileAlertShown(Duration.ofSeconds(5));
        if (!alert) {
            System.out.println(">>> no mobile-number alert — this profile already has one");
            assertTrue(page.isOnOutsiderForm(Duration.ofSeconds(20)),
                    "Without the alert the Outsider form should open directly");
            return;
        }

        screenshot("sos-outsider-alert");
        assertEquals(MOBILE_NOT_UPDATED, page.alertMessage(),
                "Alert text should match the app's wording");

        assertTrue(page.tapAlertCancel(), "Cancel should be tappable");
        assertTrue(page.isLoaded(Duration.ofSeconds(20)),
                "Cancelling should return to the receivers screen");
        assertTrue(page.tabSelected("Member"),
                "Cancelling should leave the MEMBER tab selected");
        mobileAlertSeen = true;
    }

    @Test
    @Order(13)
    @DisplayName("TC13: Update validates the number and saves it")
    void updateMobileNumber() {
        assumeTrue(mobileAlertSeen,
                "This profile already has a mobile number — the update screen is unreachable");
        SosReceiversPage page = sos();

        assertTrue(page.openOutsiderTab(), "Outsider tab should be tappable");
        assertTrue(page.isMobileAlertShown(Duration.ofSeconds(10)), "Alert should appear again");
        assertTrue(page.tapAlertUpdate(), "Update should be tappable");
        assertTrue(page.isOnUpdateMobile(Duration.ofSeconds(20)),
                "UPDATE MOBILE NUMBER should open");
        screenshot("sos-update-mobile");

        // Too short.
        assertTrue(page.typeUpdateMobile("1"), "Number field should accept input");
        assertTrue(page.tapUpdateMobileSubmit(), "Submit should be tappable");
        assertTrue(page.snackbarShown(SosReceiversPage.PHONE_LENGTH, SNACKBAR_WAIT),
                "Expected \"" + SosReceiversPage.PHONE_LENGTH + "\" for a 1-digit number");

        // Country code.
        assertTrue(page.clearUpdateMobile(), "Number field should clear");
        assertTrue(page.openUpdateMobileCountryPicker(), "Code field should be tappable");
        assertTrue(page.isOnCountryPicker(Duration.ofSeconds(20)),
                "SELECT COUNTRY ISD CODE should open");
        assertTrue(page.searchCountry(COUNTRY), "Country search should accept input");
        assertTrue(page.countryOptions().contains(COUNTRY),
                "Searching '" + COUNTRY + "' should list it; got " + page.countryOptions());
        assertTrue(page.chooseCountry(COUNTRY), COUNTRY + " should be selectable");
        assertTrue(page.isOnUpdateMobile(Duration.ofSeconds(20)), "Should return to the number screen");

        // Too long.
        assertTrue(page.typeUpdateMobile(TOO_LONG_NUMBER), "Number field should accept input");
        assertTrue(page.tapUpdateMobileSubmit(), "Submit should be tappable");
        assertTrue(page.snackbarShown(SosReceiversPage.PHONE_LENGTH, SNACKBAR_WAIT),
                "Expected \"" + SosReceiversPage.PHONE_LENGTH + "\" for a "
                        + TOO_LONG_NUMBER.length() + "-digit number");

        // Valid.
        assertTrue(page.clearUpdateMobile(), "Number field should clear");
        assertTrue(page.typeUpdateMobile(VALID_NUMBER), "Number field should accept input");
        assertTrue(page.tapUpdateMobileSubmit(), "Submit should be tappable");
        assertTrue(page.snackbarShown(SosReceiversPage.PROFILE_UPDATED, SNACKBAR_WAIT),
                "Expected \"" + SosReceiversPage.PROFILE_UPDATED + "\"");
        screenshot("sos-mobile-updated");

        assertTrue(page.tapBack(), "Back should be tappable");
        assertTrue(page.isOnReceiversScreen(Duration.ofSeconds(20)),
                "Back should return to the receivers screen");
    }

    @Test
    @Order(14)
    @DisplayName("TC14: Submitting the empty Outsider form toasts \"Please Select Country Code First.\"")
    void outsiderEmptySubmit() {
        SosReceiversPage page = sos();

        assertTrue(page.openOutsiderTab(), "Outsider tab should be tappable");
        assertTrue(page.isOnOutsiderForm(Duration.ofSeconds(20)), "Outsider form should be up");

        assertTrue(page.tapSubmit(), "Submit should be tappable");
        // A TOAST, not a snackbar — it lives in its own window.
        assertTrue(page.toastShown(SosReceiversPage.SELECT_COUNTRY_CODE, SNACKBAR_WAIT),
                "Expected the toast \"" + SosReceiversPage.SELECT_COUNTRY_CODE + "\"");
        screenshot("sos-outsider-empty-submit");
    }

    @Test
    @Order(15)
    @DisplayName("TC15: With a country code set, the relation field reports its own error")
    void outsiderCountryCodeThenRelationError() {
        SosReceiversPage page = new SosReceiversPage(driver);
        assumeTrue(page.isOnOutsiderForm(Duration.ofSeconds(10)), "Not on the Outsider form");

        assertTrue(page.openOutsiderCountryPicker(), "Code field should be tappable");
        assertTrue(page.isOnCountryPicker(Duration.ofSeconds(20)), "Country picker should open");
        assertTrue(page.searchCountry(COUNTRY), "Country search should accept input");
        assertTrue(page.chooseCountry(COUNTRY), COUNTRY + " should be selectable");
        assertTrue(page.isOnOutsiderForm(Duration.ofSeconds(20)), "Should return to the Outsider form");

        assertEquals(COUNTRY_CODE, page.outsiderCountryCode(),
                "Choosing " + COUNTRY + " should set the code to " + COUNTRY_CODE);
        screenshot("sos-outsider-country-set");

        // Only the relation publishes error TEXT; name and mobile draw a red icon
        // that is not in the accessibility tree at all, so they cannot be asserted.
        assertTrue(page.fieldErrors().contains(SosReceiversPage.SELECT_RELATION),
                "Expected the relation error \"" + SosReceiversPage.SELECT_RELATION
                        + "\"; got " + page.fieldErrors());
    }

    @Test
    @Order(16)
    @DisplayName("TC16: Name, relation and a too-short number are rejected on length")
    void outsiderShortNumberRejected() {
        SosReceiversPage page = new SosReceiversPage(driver);
        assumeTrue(page.isOnOutsiderForm(Duration.ofSeconds(10)), "Not on the Outsider form");

        assertTrue(page.typeOutsiderName(OUTSIDER_NAME), "Name field should accept input");
        assertTrue(page.typeOutsiderPhone("12"), "Mobile field should accept input");
        assertTrue(page.openRelationDropdown(), "The relation arrow should open the list");
        assertTrue(page.selectRelation(RELATION),
                RELATION + " should be selectable from the relation list");

        assertEquals(OUTSIDER_NAME, page.outsiderName(), "Name should read " + OUTSIDER_NAME);
        assertEquals(RELATION, page.relation(), "Relation should read " + RELATION);
        screenshot("sos-outsider-filled");

        assertTrue(page.tapSubmit(), "Submit should be tappable");
        assertTrue(page.snackbarShown(SosReceiversPage.PHONE_LENGTH, SNACKBAR_WAIT),
                "Expected \"" + SosReceiversPage.PHONE_LENGTH + "\" for a 2-digit number");
    }

    @Test
    @Order(17)
    @DisplayName("TC17: A valid number adds the outsider and resets the form")
    void outsiderAdded() {
        SosReceiversPage page = new SosReceiversPage(driver);
        assumeTrue(page.isOnOutsiderForm(Duration.ofSeconds(10)), "Not on the Outsider form");

        assertTrue(page.clearOutsiderPhone(), "Mobile field should clear");
        assertTrue(page.typeOutsiderPhone(OUTSIDER_PHONE), "Mobile field should accept input");

        // The guidance paragraph is always on screen, so this is a presence check,
        // not something that appears in response to the input.
        assertTrue(page.guidanceText().startsWith("Please fill in the name and mobile number"),
                "The SOS guidance text should be shown; got " + page.guidanceText());

        assertTrue(page.tapSubmit(), "Submit should be tappable");
        assertTrue(page.waitUntilReceiverListed(OUTSIDER_NAME, LIST_WAIT),
                OUTSIDER_NAME + " should appear in the list");
        screenshot("sos-outsider-added");
        System.out.println(">>> outsiders: " + page.receivers());

        assertTrue(page.receiverHasEditAndDelete(OUTSIDER_NAME),
                OUTSIDER_NAME + " should have both an edit and a delete icon");
        assertEquals(SosReceiversPage.NAME_PLACEHOLDER, page.outsiderName(),
                "The name field should reset after saving");
        assertEquals(SosReceiversPage.RELATION_PLACEHOLDER, page.relation(),
                "The relation field should reset after saving");
    }

    @Test
    @Order(18)
    @DisplayName("TC18: Editing rejects a blank name, then saves the new one")
    void outsiderEditRename() {
        SosReceiversPage page = new SosReceiversPage(driver);
        // Outsider tab — isLoaded() keys on the Member-only field and would skip.
        assumeTrue(page.isOnReceiversScreen(Duration.ofSeconds(10)), "Not on the receivers screen");
        assumeTrue(page.isReceiverListed(OUTSIDER_NAME), OUTSIDER_NAME + " is not listed");

        assertTrue(page.tapEditIcon(OUTSIDER_NAME), "Edit icon should be tappable");
        assertTrue(page.isOnEditScreen(Duration.ofSeconds(20)), "EDIT SOS RECEIVERS should open");
        assertEquals(OUTSIDER_NAME, page.outsiderName(),
                "The edit screen should be prefilled with " + OUTSIDER_NAME);

        assertTrue(page.clearOutsiderName(), "Name field should clear");
        assertTrue(page.tapSubmit(), "Submit should be tappable");
        assertTrue(page.snackbarShown(SosReceiversPage.FILL_ALL_DETAILS, SNACKBAR_WAIT),
                "Expected \"" + SosReceiversPage.FILL_ALL_DETAILS + "\" for a blank name");

        assertTrue(page.typeOutsiderName(RENAMED), "Name field should accept input");
        assertTrue(page.tapSubmit(), "Submit should be tappable");
        assertTrue(page.waitUntilReceiverListed(RENAMED, LIST_WAIT),
                "The renamed outsider " + RENAMED + " should appear in the list");
        screenshot("sos-outsider-renamed");
        assertTrue(!page.isReceiverListed(OUTSIDER_NAME),
                "The old name " + OUTSIDER_NAME + " should no longer be listed");
    }

    @Test
    @Order(19)
    @DisplayName("TC19: Deleting removes the outsider from the list")
    void outsiderDeleted() {
        SosReceiversPage page = new SosReceiversPage(driver);
        // Outsider tab — isLoaded() keys on the Member-only field and would skip.
        assumeTrue(page.isOnReceiversScreen(Duration.ofSeconds(10)), "Not on the receivers screen");
        assumeTrue(page.isReceiverListed(RENAMED), RENAMED + " is not listed");

        assertTrue(page.tapDeleteIcon(RENAMED), "Delete icon should be tappable");
        assertTrue(page.waitUntilReceiverGone(RENAMED, LIST_WAIT),
                RENAMED + " should disappear from the list");
        screenshot("sos-outsider-deleted");
    }

    @Test
    @Order(20)
    @DisplayName("TC20: The run finishes by going back to Admin Settings")
    void finishOnAdminSettings() {
        SosReceiversPage page = new SosReceiversPage(driver);

        assertTrue(page.tapBack(), "Back should be tappable on the receivers screen");
        assertTrue(page.isOnAdminSettings(Duration.ofSeconds(20)),
                "Back should land on ADMIN SETTINGS");
        screenshot("sos-finished-on-admin-settings");
    }
}
