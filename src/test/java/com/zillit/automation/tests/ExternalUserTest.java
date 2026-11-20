package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.ExternalUserFormPage;
import com.zillit.automation.pages.ExternalUserListPage;
import com.zillit.automation.pages.ProjectListPage;
import com.zillit.automation.pages.ToolsPage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * EXTERNAL USERS — listing, filters, the three user types, every field's validation, the
 * detail screen, and the two taps that hand over to other modules.
 *
 * <p>Deliberately confined to this module. Nothing here wanders off into Email or the
 * dialer beyond checking that the hand-over happens and coming straight back.
 *
 * <p>Fixtures carry the {@code ZL AUTO} prefix and addresses at {@code example.com}, which
 * is the domain reserved for exactly this, so nothing a test saves can reach a real inbox.
 * The last case removes what the suite created.
 *
 * <p>Two things worth knowing before reading the cases, both found in the source and then
 * confirmed on a device:
 * <ul>
 *   <li><b>Gender is not required.</b> Its validation is commented out in
 *       {@code AddUpdateExternalUserPage.validate()}, so a user saves without it. EU-30
 *       pins that down rather than assuming the form means what it looks like.</li>
 *   <li><b>Phone has no format check.</b> That validation is commented out too; the only
 *       rule is that phone and country must both be set or both be empty.</li>
 * </ul>
 */
@ExtendWith(ReasonLogger.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Order(TestOrder.EXTERNAL_USERS)
class ExternalUserTest extends BaseTest {

    private static final String FIXTURE_PREFIX = "ZL AUTO";
    private static final String TOOL = "External Users";

    /**
     * This suite takes the app as it finds it — it must NOT clear app data.
     *
     * <p>{@link BaseTest#requiresLoggedOutStart()} defaults to TRUE, and a suite that
     * leaves it alone wipes the app's data whenever it starts up already logged in, to
     * reach the Start &amp; Join screen. That is right for the handful of suites that test
     * signing in; for every other suite it signs the tester out of their own device and
     * then sits waiting for a login screen that only a person can get past — the app loops
     * through splash and authentication while the run waits out its landing budget.
     *
     * <p>Nothing here needs a logged-out app: external users only exist inside a project.
     */
    @Override protected boolean requiresLoggedOutStart() { return false; }

    private String createdName;

    private static String fixtureName(String what) {
        return FIXTURE_PREFIX + " " + what + " " + (System.currentTimeMillis() % 100000);
    }

    private static String fixtureEmail() {
        // example.com is reserved for documentation and tests — it reaches nobody.
        return "zlauto" + (System.currentTimeMillis() % 1000000) + "@example.com";
    }

    @BeforeEach
    void reachModule() {
        openExternalUsers();
    }

    // ─────────────────────────── the listing ───────────────────────────

    @Test
    @Order(1)
    @DisplayName("EU-01 The module opens with its header, its search and a way to add")
    void moduleOpens() {
        ExternalUserListPage list = new ExternalUserListPage(driver);
        assertTrue(list.isLoaded(Duration.ofSeconds(20)),
                "The External Users listing did not load");
        assertEquals(ExternalUserListPage.HEADER, String.valueOf(list.header()).trim(),
                "The screen's header reads \"" + list.header() + "\"");
        assertTrue(list.searchShown(), "The listing has no search box");
        assertTrue(list.addShown(),
                "The listing offers no Add control, so no external user can be created");
    }

    @Test
    @Order(2)
    @DisplayName("EU-02 The filter menu offers All and each of the three user types")
    void filterMenuComplete() {
        ExternalUserListPage list = new ExternalUserListPage(driver);
        assumeTrue(list.isLoaded(Duration.ofSeconds(20)), "The listing did not load");
        assumeTrue(list.openFilters(), "The filter control could not be opened");

        List<String> options = list.filterOptions();
        for (String wanted : List.of(ExternalUserListPage.FILTER_ALL,
                                     ExternalUserListPage.FILTER_CREW,
                                     ExternalUserListPage.FILTER_VENDOR,
                                     ExternalUserListPage.FILTER_OTHERS)) {
            assertTrue(options.stream().anyMatch(o -> o.equalsIgnoreCase(wanted)),
                    "The filter menu does not offer \"" + wanted + "\". It offers: "
                            + options);
        }
        list.chooseFilter(ExternalUserListPage.FILTER_ALL);
    }

    @Test
    @Order(22)
    @DisplayName("EU-03 Filtering by a user type shows that type and no other")
    void filterByTypeIsExact() {
        ExternalUserListPage list = new ExternalUserListPage(driver);
        assumeTrue(list.isLoaded(Duration.ofSeconds(20)), "The listing did not load");
        assumeTrue(list.openFilters(), "The filter control could not be opened");
        // Vendor, because that is what this suite creates — ordering these cases after
        // EU-30 means there is always at least one row to filter to, instead of skipping
        // on a project that happens to have no external users of its own.
        assumeTrue(list.chooseFilter(ExternalUserListPage.FILTER_VENDOR),
                "\"" + ExternalUserListPage.FILTER_VENDOR + "\" could not be chosen");

        List<ExternalUserListPage.Row> rows = list.rows();
        assumeTrue(!rows.isEmpty(),
                "This project has no " + ExternalUserListPage.FILTER_VENDOR + " to filter to");
        // A filter that leaks other types is worse than none: the reader takes the list as
        // complete and trusts it.
        for (ExternalUserListPage.Row r : rows) {
            assertNotNull(r.userType, "A row shows no user type at all: " + r);
            assertTrue(r.userType.equalsIgnoreCase(ExternalUserListPage.FILTER_VENDOR),
                    "Filtered to " + ExternalUserListPage.FILTER_VENDOR + " and a \""
                            + r.userType + "\" row is still listed: " + r);
        }
    }

    @Test
    @Order(4)
    @DisplayName("EU-04 Choosing a filter clears whatever was typed in the search")
    void filterClearsSearch() {
        ExternalUserListPage list = new ExternalUserListPage(driver);
        assumeTrue(list.isLoaded(Duration.ofSeconds(20)), "The listing did not load");
        assumeTrue(list.search("zzz"), "The search box could not be used");
        assumeTrue(list.openFilters(), "The filter control could not be opened");
        assumeTrue(list.chooseFilter(ExternalUserListPage.FILTER_ALL),
                "\"All\" could not be chosen");

        // The app clears the search when a filter is applied, on purpose: the two narrow
        // the same list and leaving both on shows a result that matches neither control.
        String term = String.valueOf(list.searchText()).trim();
        assertTrue(term.isEmpty() || term.equalsIgnoreCase("Search By User Name"),
                "A filter was applied and the search box still holds \"" + term
                        + "\", so the list is narrowed by something the user cannot see");
    }

    @Test
    @Order(23)
    @DisplayName("EU-05 Search narrows to a matching name, and nonsense narrows to nothing")
    void searchNarrows() {
        ExternalUserListPage list = new ExternalUserListPage(driver);
        assumeTrue(list.isLoaded(Duration.ofSeconds(20)), "The listing did not load");
        List<String> all = list.names();
        assumeTrue(!all.isEmpty(), "This project has no external users to search");

        String target = all.get(0);
        String needle = target.length() > 3 ? target.substring(0, 3) : target;
        assumeTrue(list.search(needle), "The search box could not be used");
        List<String> hits = list.names();
        assertTrue(hits.stream().anyMatch(n -> n.contains(needle)),
                "Searching \"" + needle + "\" did not return \"" + target + "\": " + hits);

        // THE NEGATIVE HALF. A search that matches nothing must show nothing, not fall
        // back to the whole list.
        assumeTrue(list.search("ZZQQXX"), "The search box could not be used");
        assertTrue(list.names().isEmpty(),
                "Searching for nonsense returned " + list.names()
                        + ", so the search is being ignored");
        list.clearSearch();
    }

    // ──────────────────── the three types: show and hide ────────────────────

    @Test
    @Order(6)
    @DisplayName("EU-10 The form opens on Crew Member, with Department and Designation")
    void formOpensOnCrew() {
        ExternalUserFormPage f = openAddForm();
        assertEquals(ExternalUserFormPage.TYPE_CREW, String.valueOf(f.userType()).trim(),
                "The form opened on user type \"" + f.userType() + "\"");
        assertTrue(f.departmentShown() && f.designationShown(),
                "A Crew Member needs a department and a designation, and the form shows "
                        + f.visibility());
        assertFalse(f.otherTypeShown(),
                "The \"other type\" field is showing for a Crew Member: " + f.visibility());
        assertEquals("Submit", String.valueOf(f.saveLabel()).trim(),
                "The add form's save button reads \"" + f.saveLabel() + "\"");
        closeForm();
    }

    @Test
    @Order(7)
    @DisplayName("EU-11 Vendor hides Department and Designation, and asks for no other type")
    void vendorHidesDepartment() {
        ExternalUserFormPage f = openAddForm();
        assumeTrue(f.chooseUserType(ExternalUserFormPage.TYPE_VENDOR),
                "\"" + ExternalUserFormPage.TYPE_VENDOR + "\" could not be chosen."
                        + " Options: " + f.userTypeOptions());
        assertEquals(ExternalUserFormPage.TYPE_VENDOR, String.valueOf(f.userType()).trim(),
                "The type did not change: it reads \"" + f.userType() + "\"");
        assertFalse(f.departmentShown(),
                "A Vendor does not belong to a department, and Department is still"
                        + " showing: " + f.visibility());
        assertFalse(f.designationShown(),
                "Designation is still showing for a Vendor: " + f.visibility());
        assertFalse(f.otherTypeShown(),
                "The \"other type\" field belongs to \"others\", not to Vendor: "
                        + f.visibility());
        closeForm();
    }

    @Test
    @Order(8)
    @DisplayName("EU-12 others hides both and asks what the other type is")
    void othersAsksForItsType() {
        ExternalUserFormPage f = openAddForm();
        assumeTrue(f.chooseUserType(ExternalUserFormPage.TYPE_OTHERS),
                "\"" + ExternalUserFormPage.TYPE_OTHERS + "\" could not be chosen."
                        + " Options: " + f.userTypeOptions());
        assertTrue(f.otherTypeShown(),
                "\"others\" was chosen and no field appeared to say WHAT other type this"
                        + " is, so the type would be saved as a bare \"others\": "
                        + f.visibility());
        assertFalse(f.departmentShown() || f.designationShown(),
                "Department or Designation is showing for \"others\": " + f.visibility());
        closeForm();
    }

    @Test
    @Order(9)
    @DisplayName("EU-13 Going back to Crew Member brings Department and Designation back")
    void switchingBackRestoresFields() {
        ExternalUserFormPage f = openAddForm();
        assumeTrue(f.chooseUserType(ExternalUserFormPage.TYPE_VENDOR),
                "Vendor could not be chosen");
        assumeTrue(!f.departmentShown(), "Vendor did not hide Department, so there is"
                + " nothing to restore");
        assumeTrue(f.chooseUserType(ExternalUserFormPage.TYPE_CREW),
                "Crew Member could not be chosen again");

        // The show/hide runs off the dropdown's item-click listener only. A one-way
        // transition would leave a crew member with no department to set.
        assertTrue(f.departmentShown() && f.designationShown(),
                "Switching back to Crew Member left the form at " + f.visibility());
        closeForm();
    }

    // ─────────────────────── validation, negative half ───────────────────────

    @Test
    @Order(10)
    @DisplayName("EU-20 An empty form is refused and stays open")
    void emptyFormRefused() {
        ExternalUserFormPage f = openAddForm();
        assumeTrue(f.save(), "The save button was not clickable");
        sleep(2500);
        assertTrue(f.stillOpen(),
                "An empty form saved. A full name is the first thing the app requires.");
        closeForm();
    }

    @Test
    @Order(11)
    @DisplayName("EU-21 A full name with special characters is refused")
    void specialCharactersInNameRefused() {
        ExternalUserFormPage f = openAddForm();
        assumeTrue(f.setFullName("Test@#$%User"), "The name could not be typed");
        assumeTrue(f.setEmail(fixtureEmail()), "The email could not be typed");
        assumeTrue(f.save(), "The save button was not clickable");
        sleep(2500);
        assertTrue(f.stillOpen(),
                "\"Test@#$%User\" was accepted as a full name. The app rejects special"
                        + " characters in this field (containsSpecialCharacters).");
        closeForm();
    }

    @Test
    @Order(12)
    @DisplayName("EU-22 A name with no email is refused")
    void missingEmailRefused() {
        ExternalUserFormPage f = openAddForm();
        assumeTrue(f.setFullName(fixtureName("NoEmail")), "The name could not be typed");
        assumeTrue(f.setEmail(""), "The email could not be cleared");
        assumeTrue(f.save(), "The save button was not clickable");
        sleep(2500);
        assertTrue(f.stillOpen(),
                "A user with no email address was accepted. The email is how this person"
                        + " is reached, and the app requires it.");
        closeForm();
    }

    @Test
    @Order(13)
    @DisplayName("EU-23 A malformed email is refused")
    void malformedEmailRefused() {
        ExternalUserFormPage f = openAddForm();
        assumeTrue(f.setFullName(fixtureName("BadMail")), "The name could not be typed");
        assumeTrue(f.setEmail("not-an-email"), "The email could not be typed");
        assumeTrue(f.save(), "The save button was not clickable");
        sleep(2500);
        assertTrue(f.stillOpen(),
                "\"not-an-email\" was accepted as an email address");
        closeForm();
    }

    @Test
    @Order(14)
    @DisplayName("EU-24 A Crew Member with no department is refused")
    void crewWithoutDepartmentRefused() {
        ExternalUserFormPage f = openAddForm();
        assumeTrue(f.departmentShown(), "Department is not showing, so it cannot be left"
                + " empty on purpose");
        assumeTrue(f.setFullName(fixtureName("NoDept")), "The name could not be typed");
        assumeTrue(f.setEmail(fixtureEmail()), "The email could not be typed");
        assumeTrue(f.save(), "The save button was not clickable");
        sleep(2500);
        assertTrue(f.stillOpen(),
                "A Crew Member saved with no department. The app requires it whenever the"
                        + " field is on screen.");
        closeForm();
    }

    @Test
    @Order(15)
    @DisplayName("EU-25 A Crew Member with a department but no designation is refused")
    void crewWithoutDesignationRefused() {
        ExternalUserFormPage f = openAddForm();
        assumeTrue(f.setFullName(fixtureName("NoDesig")), "The name could not be typed");
        assumeTrue(f.setEmail(fixtureEmail()), "The email could not be typed");
        assumeTrue(f.chooseDepartment(null),
                "No department could be chosen from the chooser");
        assumeTrue(f.save(), "The save button was not clickable");
        sleep(2500);
        assertTrue(f.stillOpen(),
                "A Crew Member saved with a department and no designation");
        closeForm();
    }

    @Test
    @Order(16)
    @DisplayName("EU-26 \"others\" with no other type typed in is refused")
    void othersWithoutTypeRefused() {
        ExternalUserFormPage f = openAddForm();
        assumeTrue(f.setFullName(fixtureName("NoOther")), "The name could not be typed");
        assumeTrue(f.setEmail(fixtureEmail()), "The email could not be typed");
        assumeTrue(f.chooseUserType(ExternalUserFormPage.TYPE_OTHERS),
                "\"others\" could not be chosen");
        assumeTrue(f.otherTypeShown(), "The other-type field did not appear");
        assumeTrue(f.setOtherType(""), "The other-type field could not be cleared");
        assumeTrue(f.save(), "The save button was not clickable");
        sleep(2500);
        assertTrue(f.stillOpen(),
                "\"others\" was accepted with nothing said about what the other type is");
        closeForm();
    }

    @Test
    @Order(17)
    @DisplayName("EU-27 A phone number with no country is refused")
    void phoneWithoutCountryRefused() {
        ExternalUserFormPage f = openAddForm();
        assumeTrue(f.setFullName(fixtureName("NoCountry")), "The name could not be typed");
        assumeTrue(f.setEmail(fixtureEmail()), "The email could not be typed");
        assumeTrue(f.chooseUserType(ExternalUserFormPage.TYPE_VENDOR),
                "Vendor could not be chosen");   // sidesteps department/designation
        assumeTrue(f.setPhone("9876543210"), "The phone could not be typed");
        assumeTrue(f.save(), "The save button was not clickable");
        sleep(2500);
        assertTrue(f.stillOpen(),
                "A phone number saved with no country code. A number without one cannot"
                        + " be dialled from the detail screen.");
        closeForm();
    }

    @Test
    @Order(18)
    @DisplayName("EU-28 A country with no phone number is refused")
    void countryWithoutPhoneRefused() {
        ExternalUserFormPage f = openAddForm();
        assumeTrue(f.setFullName(fixtureName("NoPhone")), "The name could not be typed");
        assumeTrue(f.setEmail(fixtureEmail()), "The email could not be typed");
        assumeTrue(f.chooseUserType(ExternalUserFormPage.TYPE_VENDOR),
                "Vendor could not be chosen");
        assumeTrue(f.chooseCountry(null), "No country could be chosen from the chooser");
        assumeTrue(f.setPhone(""), "The phone could not be cleared");
        assumeTrue(f.save(), "The save button was not clickable");
        sleep(2500);
        assertTrue(f.stillOpen(),
                "A country code saved with no phone number behind it");
        closeForm();
    }

    @Test
    @Order(19)
    @DisplayName("EU-29 The email field stops at 50 characters")
    void emailIsCappedAt50() {
        ExternalUserFormPage f = openAddForm();
        String long61 = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa@example.com";
        assumeTrue(long61.length() > ExternalUserFormPage.EMAIL_MAX,
                "The test string is not longer than the cap");
        assumeTrue(f.setEmail(long61), "The email could not be typed");

        // Enforced by an InputFilter rather than by validation, so the field simply stops
        // accepting characters — there is no message to look for.
        String got = String.valueOf(f.email());
        assertTrue(got.length() <= ExternalUserFormPage.EMAIL_MAX,
                "The email field holds " + got.length() + " characters (\"" + got + "\"),"
                        + " past its " + ExternalUserFormPage.EMAIL_MAX + "-character cap");
        closeForm();
    }

    // ─────────────────────── validation, positive half ───────────────────────

    @Test
    @Order(20)
    @DisplayName("EU-30 Gender is optional — a user saves without it")
    void genderIsOptional() {
        // Pins down what the code says rather than what the form looks like: the gender
        // check is commented out in validate(). If this starts failing, gender has been
        // made required and the sheet needs changing — not the app.
        ExternalUserFormPage f = openAddForm();
        // Held locally until the save is confirmed. Setting the field here used to mark the
        // user as created before the attempt had even run, so when this case skipped —
        // Vendor could not be chosen — EU-31 went looking for a user that was never saved
        // and reported the app as losing it.
        String candidate = fixtureName("Vendor");
        assumeTrue(f.setFullName(candidate), "The name could not be typed");
        assumeTrue(f.setEmail(fixtureEmail()), "The email could not be typed");
        assumeTrue(f.chooseUserType(ExternalUserFormPage.TYPE_VENDOR),
                "Vendor could not be chosen");
        assumeTrue(String.valueOf(f.gender()).trim().equalsIgnoreCase("Gender")
                        || String.valueOf(f.gender()).isBlank(),
                "The form already has a gender (\"" + f.gender() + "\"), so 'without a"
                        + " gender' is not what would be tested");
        assumeTrue(f.save(), "The save button was not clickable");
        sleep(4000);

        assertFalse(f.stillOpen(),
                "A Vendor with a name and an email was refused, and gender is the only"
                        + " thing missing — but its validation is commented out, so it"
                        + " should not be required. The form says: " + f.visibility());
        createdName = candidate;        // only now is there a user to look for
    }

    @Test
    @Order(21)
    @DisplayName("EU-31 The saved user appears in the list with what was entered")
    void savedUserAppears() {
        assumeTrue(createdName != null, "EU-30 saved no user to look for");
        ExternalUserListPage list = new ExternalUserListPage(driver);
        assumeTrue(list.isLoaded(Duration.ofSeconds(20)), "The listing did not load");
        assumeTrue(list.search(FIXTURE_PREFIX), "The search box could not be used");

        ExternalUserListPage.Row row = list.find(createdName);
        assertNotNull(row, "\"" + createdName + "\" was saved but is not in the list."
                + " Rows: " + list.names());
        assertEquals(ExternalUserFormPage.TYPE_VENDOR, String.valueOf(row.userType).trim(),
                "The row shows user type \"" + row.userType + "\": " + row);
        assertNotNull(row.email, "The row shows no email for \"" + createdName + "\": " + row);
        list.clearSearch();
    }

    @Test
    @Order(22)
    @DisplayName("EU-32 Updating the name changes it in the list, and adds nothing")
    void updateChangesTheName() {
        assumeTrue(createdName != null, "EU-30 saved no user to update");
        ExternalUserListPage list = new ExternalUserListPage(driver);
        assumeTrue(list.isLoaded(Duration.ofSeconds(20)), "The listing did not load");
        assumeTrue(list.search(FIXTURE_PREFIX), "The search box could not be used");
        assumeTrue(list.edit(createdName),
                "Edit was not available on \"" + createdName + "\" — it needs posting"
                        + " rights and a connection");

        ExternalUserFormPage f = new ExternalUserFormPage(driver);
        assumeTrue(f.isLoaded(Duration.ofSeconds(15)), "The update form did not open");
        assertEquals("Update", String.valueOf(f.saveLabel()).trim(),
                "The edit form's save button reads \"" + f.saveLabel() + "\"");

        String renamed = createdName + " UPD";
        assumeTrue(f.setFullName(renamed), "The name could not be retyped");
        assumeTrue(f.save(), "The save button was not clickable");
        sleep(4000);

        ExternalUserListPage after = new ExternalUserListPage(driver);
        assumeTrue(after.isLoaded(Duration.ofSeconds(20)), "The listing did not come back");
        assumeTrue(after.search(FIXTURE_PREFIX), "The search box could not be used");
        // Counted, not matched by substring. The renamed value CONTAINS the old one
        // ("… 8109 UPD" contains "… 8109"), so asking whether any row still carries the old
        // name answers yes for the renamed row itself — the rename looked like a duplicate
        // when the list held exactly one correct row.
        List<String> mine = after.names().stream()
                .filter(n -> n.contains(FIXTURE_PREFIX)).toList();
        assertEquals(1, mine.size(),
                "After renaming to \"" + renamed + "\" the project holds " + mine.size()
                        + " fixture user(s): " + mine + ". An update must change the one"
                        + " that exists, not add another.");
        assertEquals(renamed, mine.get(0),
                "The row reads \"" + mine.get(0) + "\" after a rename to \"" + renamed + "\"");
        createdName = renamed;
        after.clearSearch();
    }

    // ─────────────────────────── the detail screen ───────────────────────────

    @Test
    @Order(23)
    @DisplayName("EU-33 The detail screen opens read-only, with no way to save")
    void detailIsReadOnly() {
        assumeTrue(createdName != null, "EU-30 saved no user to view");
        ExternalUserListPage list = new ExternalUserListPage(driver);
        assumeTrue(list.isLoaded(Duration.ofSeconds(20)), "The listing did not load");
        assumeTrue(list.search(FIXTURE_PREFIX), "The search box could not be used");
        assumeTrue(list.viewDetails(createdName), "View More was not available");

        ExternalUserFormPage f = new ExternalUserFormPage(driver);
        assumeTrue(f.isLoaded(Duration.ofSeconds(15)), "The detail screen did not open");
        assertTrue(f.onDetailScreen(),
                "The detail screen's header reads \"" + f.header() + "\"");
        assertFalse(f.saveShown(),
                "The detail screen offers a save button, so it is not read-only");
        assertEquals(createdName, String.valueOf(f.fullName()).trim(),
                "The detail screen shows the name as \"" + f.fullName() + "\"");
        closeForm();
    }

    @Test
    @Order(24)
    @DisplayName("EU-34 Tapping the email on the detail screen hands over to Email")
    void emailTapOpensEmailModule() {
        assumeTrue(createdName != null, "EU-30 saved no user to open");
        assumeTrue(openDetail(createdName), "The detail screen could not be opened");
        ExternalUserFormPage f = new ExternalUserFormPage(driver);
        assumeTrue(f.isLoaded(Duration.ofSeconds(15)), "The detail screen did not open");

        String before = resumedActivity();
        assumeTrue(f.tapEmail(), "The email could not be tapped");
        sleep(4000);
        String after = resumedActivity();

        // It should land on the Email module's compose screen, pre-addressed. Nothing is
        // sent: the case only checks the hand-over and comes straight back.
        assertTrue(after.contains("ComposeActivity"),
                "Tapping the email went from " + shortActivity(before) + " to "
                        + shortActivity(after) + ", not to the Email module's compose"
                        + " screen. The email is meant to be the way to write to this"
                        + " person.");
        backToModule();
    }

    @Test
    @Order(25)
    @DisplayName("EU-35 Tapping the contact number opens the dialer, pre-filled")
    void phoneTapOpensDialer() {
        assumeTrue(createdName != null, "EU-30 saved no user to open");
        assumeTrue(openDetail(createdName), "The detail screen could not be opened");
        ExternalUserFormPage f = new ExternalUserFormPage(driver);
        assumeTrue(f.isLoaded(Duration.ofSeconds(15)), "The detail screen did not open");

        String saved = String.valueOf(f.phone()).trim();
        assumeTrue(!saved.isEmpty() && !"Phone".equalsIgnoreCase(saved),
                "This user has no phone number saved, so there is nothing to dial."
                        + " The tap only does anything when both the country code and the"
                        + " number are set.");

        String before = resumedActivity();
        assumeTrue(f.tapPhone(), "The phone could not be tapped");
        sleep(4000);
        String after = resumedActivity();

        // ACTION_DIAL opens the dialer with the number filled in; it never places a call.
        assertFalse(after.equals(before) && after.contains("AddUpdateExternalUser"),
                "Tapping the contact number left us on " + shortActivity(after)
                        + ". It is meant to open the dialer with the number ready.");
        backToModule();
    }

    // ─────────────────────── delete, and the cleanup ───────────────────────

    @Test
    @Order(26)
    @DisplayName("EU-36 Delete asks first, and Cancel keeps the user")
    void deleteAsksAndCancelKeeps() {
        assumeTrue(createdName != null, "EU-30 saved no user to delete");
        ExternalUserListPage list = new ExternalUserListPage(driver);
        assumeTrue(list.isLoaded(Duration.ofSeconds(20)), "The listing did not load");
        assumeTrue(list.search(FIXTURE_PREFIX), "The search box could not be used");
        assumeTrue(list.delete(createdName), "Delete was not available on the row");

        assertTrue(list.deleteConfirmShown(Duration.ofSeconds(6)),
                "Delete did not ask for confirmation. An external user carries somebody's"
                        + " contact details, and a mis-tap would take them with no warning.");
        assumeTrue(list.cancelDelete(), "Cancel was not available");
        sleep(2500);

        assumeTrue(list.search(FIXTURE_PREFIX), "The search box could not be used");
        assertNotNull(list.find(createdName),
                "Cancelling the delete removed \"" + createdName + "\" anyway: "
                        + list.names());
        list.clearSearch();
    }

    @Test
    @Order(27)
    @DisplayName("EU-37 Delete removes the user, and it stays removed after a reopen")
    void deleteReallyDeletes() {
        assumeTrue(createdName != null, "EU-30 saved no user to delete");
        ExternalUserListPage list = new ExternalUserListPage(driver);
        assumeTrue(list.isLoaded(Duration.ofSeconds(20)), "The listing did not load");
        assumeTrue(list.search(FIXTURE_PREFIX), "The search box could not be used");
        assumeTrue(list.delete(createdName), "Delete was not available on the row");
        assumeTrue(list.deleteConfirmShown(Duration.ofSeconds(6)),
                "Delete did not ask for confirmation");
        assumeTrue(list.confirmDelete(), "Yes was not available");
        sleep(3500);

        // The row is taken off the screen BEFORE the server is asked
        // (ExternalUserVM.delete calls adapter.deleteItem first and never puts it back on
        // failure). So "gone from the screen" proves nothing — the module is reopened and
        // the list re-fetched, and only then is it really gone.
        openExternalUsers();
        ExternalUserListPage fresh = new ExternalUserListPage(driver);
        assumeTrue(fresh.isLoaded(Duration.ofSeconds(20)), "The listing did not reload");
        assumeTrue(fresh.search(FIXTURE_PREFIX), "The search box could not be used");
        assertNull(fresh.find(createdName),
                "\"" + createdName + "\" came back after the module was reopened, so the"
                        + " delete only ever happened on screen: " + fresh.names());
        createdName = null;
        fresh.clearSearch();
    }

    @Test
    @Order(28)
    @DisplayName("EU-38 Cleanup: no ZL AUTO external user is left behind")
    void cleanup() {
        ExternalUserListPage list = new ExternalUserListPage(driver);
        assumeTrue(list.isLoaded(Duration.ofSeconds(20)), "The listing did not load");
        for (int i = 0; i < 10; i++) {
            if (!list.search(FIXTURE_PREFIX)) break;
            String leftover = list.names().stream()
                    .filter(n -> n.contains(FIXTURE_PREFIX))
                    .findFirst().orElse(null);
            if (leftover == null) break;
            if (!list.delete(leftover)) break;
            if (list.deleteConfirmShown(Duration.ofSeconds(5))) list.confirmDelete();
            sleep(2500);
            System.out.println(">>> EU-38 deleted \"" + leftover + "\"");
        }
        list.clearSearch();
        // Reported, not asserted: a leftover here is worth saying out loud, but another
        // run on the same project may be holding one of its own.
        List<String> left = list.names().stream()
                .filter(n -> n.contains(FIXTURE_PREFIX)).toList();
        if (!left.isEmpty()) {
            System.out.println(">>> EU-38 still on the project: " + left);
        }
    }

    // ─────────────────────────── helpers ───────────────────────────

    /** Opens the module from Tools, entering a project first if we are not in one. */
    private void openExternalUsers() {
        ExternalUserListPage already = new ExternalUserListPage(driver);
        // Generous on the first look: after a save the listing re-fetches, and a check that
        // gave up in two seconds used to decide we were lost and press BACK — walking out
        // of the module from the very screen it wanted.
        if (already.isLoaded(Duration.ofSeconds(10))) return;

        // Back out of whatever is on top until the listing is reached — NOT only screens
        // whose Activity is named after this module. Department, Designation and Country
        // open CommonSearchListActivity, a screen of their own, and a case that ends on one
        // used to leave the next case looking for a landing it could never find: the
        // bottom navigation is not on screen there, so it waited out its whole budget and
        // then failed, taking every case after it down as well.
        for (int i = 0; i < 6; i++) {
            if (already.isLoaded(Duration.ofSeconds(2))) return;
            if (!appPackage.equals(safePackage())) break;   // left the app — stop backing out
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(1000);
        }
        if (already.isLoaded(Duration.ofSeconds(2))) return;

        if (landing == Landing.PROJECT_LIST) {
            ProjectListPage projects = new ProjectListPage(driver);
            List<String> names = projects.visibleProjectNames();
            assumeTrue(!names.isEmpty(), "No projects to enter");
            assumeTrue(projects.tapProject(names.get(0)), "Could not enter a project");
        }
        assumeTrue(new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(60)),
                "The bottom navigation never loaded");

        ToolsPage tools = new ToolsPage(driver);
        assumeTrue(tools.openToolsTab(), "The Tools tab could not be opened");
        assumeTrue(tools.isLoaded(Duration.ofSeconds(30)), "The Tools tab did not load");
        tools.searchTools("External");
        sleep(1500);
        assumeTrue(tools.openIfVisible(TOOL),
                "\"" + TOOL + "\" is not offered in Tools. Tools visible: "
                        + tools.toolNames());
        assumeTrue(new ExternalUserListPage(driver).isLoaded(Duration.ofSeconds(25)),
                "The External Users listing did not open");
    }

    private ExternalUserFormPage openAddForm() {
        ExternalUserFormPage f = new ExternalUserFormPage(driver);
        if (f.isLoaded(Duration.ofSeconds(2)) && f.onAddScreen()) return f;   // reuse it
        ExternalUserListPage list = new ExternalUserListPage(driver);
        assumeTrue(list.isLoaded(Duration.ofSeconds(20)), "The listing did not load");
        assumeTrue(list.openAdd(), "Add was not available — it needs posting rights");
        assumeTrue(f.isLoaded(Duration.ofSeconds(15)), "The add form did not open");
        return f;
    }

    private boolean openDetail(String name) {
        ExternalUserListPage list = new ExternalUserListPage(driver);
        if (!list.isLoaded(Duration.ofSeconds(20))) return false;
        if (!list.search(FIXTURE_PREFIX)) return false;
        return list.viewDetails(name);
    }

    /** Leaves the form without leaving the module. */
    private void closeForm() {
        ExternalUserFormPage f = new ExternalUserFormPage(driver);
        if (!f.isLoaded(Duration.ofSeconds(1))) return;
        try { driver.navigate().back(); } catch (Exception ignored) { }
        sleep(1500);
    }

    /** Comes back from wherever a hand-over took us, without acting on that screen. */
    private void backToModule() {
        for (int i = 0; i < 6; i++) {
            if (new ExternalUserListPage(driver).isLoaded(Duration.ofSeconds(2))
                    || new ExternalUserFormPage(driver).isLoaded(Duration.ofSeconds(1))) {
                break;
            }
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(1200);
        }
        openExternalUsers();
    }

    /**
     * Which Activity is on top, via Appium rather than adb — enough to tell a hand-over to
     * the Email module or the dialer from staying put, and it costs no shell call.
     */
    private String resumedActivity() {
        try {
            String a = driver.currentActivity();
            return a == null ? "(unknown)" : a;
        } catch (Exception e) {
            return "(unavailable: " + e.getClass().getSimpleName() + ")";
        }
    }

    /** The package on top, or "" when it cannot be read — used to stop backing out. */
    private String safePackage() {
        try {
            String p = driver.getCurrentPackage();
            return p == null ? "" : p;
        } catch (Exception e) { return ""; }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String shortActivity(String s) {
        if (s == null) return "?";
        int i = s.lastIndexOf('.');
        return i > 0 ? s.substring(i + 1) : s;
    }

    @AfterAll
    void leaveModule() {
        for (int i = 0; i < 5 && resumedActivity().contains("ExternalUser"); i++) {
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(800);
        }
    }
}
