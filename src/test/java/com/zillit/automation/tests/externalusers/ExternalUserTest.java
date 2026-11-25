package com.zillit.automation.tests.externalusers;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.common.BottomNavPage;
import com.zillit.automation.pages.externalusers.ExternalUserFormPage;
import com.zillit.automation.pages.externalusers.ExternalUserListPage;
import com.zillit.automation.pages.common.ProjectListPage;
import com.zillit.automation.pages.common.ToolsPage;
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

    /**
     * The app is NOT restarted between cases.
     *
     * <p>{@link BaseTest#relaunchBetweenTests()} defaults to TRUE: before every case the
     * app is terminated and launched again, which means splash, authentication, the
     * project list, and a walk back through Tools — per case. On a phone that is most of
     * the run's time, and on screen it looks like the app opening and closing over and
     * over, which is exactly what it is.
     *
     * <p>These cases are a ladder — EU-30 creates a user, EU-31 finds it, EU-32 renames it,
     * EU-37 deletes it — so they belong on one continuous session, and every helper here
     * already recovers to the listing on its own.
     */
    @Override protected boolean relaunchBetweenTests() { return false; }

    private String createdName;
    /** The email EU-30 saved, kept for the EXT-01b lower-case check on the detail screen. */
    private String createdEmail;

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
    @Order(3)
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
    @Order(5)
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
        String candidateEmail = fixtureEmail();
        assumeTrue(f.setFullName(candidate), "The name could not be typed");
        assumeTrue(f.setEmail(candidateEmail), "The email could not be typed");
        assumeTrue(f.chooseUserType(ExternalUserFormPage.TYPE_VENDOR),
                "Vendor could not be chosen");
        assumeTrue(String.valueOf(f.gender()).trim().equalsIgnoreCase("Gender")
                        || String.valueOf(f.gender()).isBlank(),
                "The form already has a gender (\"" + f.gender() + "\"), so 'without a"
                        + " gender' is not what would be tested");

        // Give this user a phone number. EU-35 taps the contact on the detail screen to
        // check it opens the dialer, and that tap does nothing unless BOTH the country
        // code and the number are set — so a suite that never saves one can never test it.
        // It skipped every run for exactly that reason, which looked like a finding and
        // was a gap in the fixture.
        if (f.chooseCountry(null)) {
            f.setPhone("9876543210");
        } else {
            System.out.println(">>> EU-30 note: no country could be chosen, so this user"
                    + " is saved without a phone and EU-35 will have nothing to dial.");
        }
        assumeTrue(f.save(), "The save button was not clickable");
        sleep(4000);

        assertFalse(f.stillOpen(),
                "A Vendor with a name and an email was refused, and gender is the only"
                        + " thing missing — but its validation is commented out, so it"
                        + " should not be required. The form says: " + f.visibility());
        createdName = candidate;        // only now is there a user to look for
        createdEmail = candidateEmail;
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
        // Matched EXACTLY, both ways, and the new name is recorded first.
        //
        // Counting every fixture row assumed a clean project, and broke the moment an
        // earlier run left one behind: it reported a duplicate this case had not created.
        // A "contains" test for the old name is no better — the renamed row contains it,
        // since "… 8109 UPD" contains "… 8109". And recording the new name only after the
        // assertions meant a failure here left every later case hunting for the OLD name.
        String previous = createdName;
        createdName = renamed;

        List<String> names = after.names();
        assertTrue(names.stream().anyMatch(renamed::equals),
                "The user was renamed to \"" + renamed + "\" and no row carries exactly"
                        + " that: " + names);
        assertFalse(names.stream().anyMatch(previous::equals),
                "A row still carries the old name \"" + previous + "\" exactly, so the"
                        + " update added a user instead of changing one: " + names);
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
        // [WEB EXT-01b] The email comes back exactly as typed, all lower case. Appium types
        // with setText, so this proves storage only; whether Gboard capitalises the first
        // letter while a person types is a manual check (the field has no CAP flag:
        // AddUpdateExternalUserPage.kt:195-198).
        if (createdEmail != null) {
            String shown = String.valueOf(f.email()).trim();
            assertEquals(createdEmail, shown,
                    "EXT-01b: the email was saved as \"" + createdEmail + "\" and the detail"
                            + " screen shows \"" + shown + "\"");
            assertEquals(shown.toLowerCase(java.util.Locale.ROOT), shown,
                    "EXT-01b: the stored email is not lower case: \"" + shown + "\"");
        }
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
        sleep(3000);

        // Re-read from a FRESH listing, not from the one the dialog was dismissed over.
        // The rows are re-bound when the dialog closes, and reading across that moment
        // returned an empty list — which read as "Cancel deleted the user" when the user
        // was never touched. A claim this serious has to be made on a settled screen.
        ExternalUserListPage after = reopenListing();
        assumeTrue(after.search(FIXTURE_PREFIX), "The search box could not be used");
        sleep(2000);
        assertNotNull(after.find(createdName),
                "Cancelling the delete removed \"" + createdName + "\" anyway."
                        + " Rows after a reopen: " + after.names());
        after.clearSearch();
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

    // ── from the QA sheet's Android column: defects a human confirmed by hand ──

    @Test
    @Order(40)
    @DisplayName("EU-39 A phone number longer than 15 digits is refused")
    void overlongPhoneRefused() {
        ExternalUserListPage list = new ExternalUserListPage(driver);
        assumeTrue(list.isLoaded(Duration.ofSeconds(20)), "The listing did not load");
        assumeTrue(list.openAdd(), "The add button could not be opened");

        ExternalUserFormPage form = new ExternalUserFormPage(driver);
        assumeTrue(form.isLoaded(Duration.ofSeconds(15)), "The form did not open");
        String name = fixtureName("LongPhone");
        assumeTrue(form.setFullName(name), "The name could not be typed");
        assumeTrue(form.setEmail(fixtureEmail()), "The email could not be typed");
        assumeTrue(form.chooseUserType("Vendor"), "Vendor could not be chosen");
        assumeTrue(form.chooseCountry("India"), "A country could not be chosen");
        // Sixteen digits. E.164 allows fifteen at most, and the web client refuses this.
        assumeTrue(form.setPhone("1234567890123456"), "The phone could not be typed");

        int before = list.rows().size();
        form.save();
        sleep(4000);

        // Either the form refuses it, or it is still open with an error — what must NOT
        // happen is a saved contact nobody can call. A record created this way is also
        // un-editable on web, which refuses its own stored number on the way back in, so
        // the department cannot even be corrected afterwards.
        boolean stillOnForm = form.isLoaded(Duration.ofSeconds(3));
        if (!stillOnForm) {
            ExternalUserListPage after = new ExternalUserListPage(driver);
            if (after.isLoaded(Duration.ofSeconds(15))) {
                assertTrue(after.rows().size() <= before,
                        "A 16-digit phone number saved a new external user. There is no"
                                + " length validation on this field at all, and the stored"
                                + " number cannot be dialled — nor the record edited on web,"
                                + " which refuses the number it was given.");
            }
        }
        assertTrue(stillOnForm,
                "The form accepted a 16-digit phone number and closed. E.164 allows fifteen"
                        + " digits at most, and the web client refuses this one.");
    }

    @Test
    @Order(41)
    @DisplayName("EU-40 A filter that matches nothing says so, instead of going blank")
    void emptyFilterSaysSo() {
        ExternalUserListPage list = new ExternalUserListPage(driver);
        assumeTrue(list.isLoaded(Duration.ofSeconds(20)), "The listing did not load");

        String emptyType = null;
        for (String type : List.of(ExternalUserListPage.FILTER_VENDOR,
                ExternalUserListPage.FILTER_OTHERS, ExternalUserListPage.FILTER_CREW)) {
            if (!list.openFilters() || !list.chooseFilter(type)) continue;
            sleep(2500);
            if (list.rows().isEmpty()) { emptyType = type; break; }
        }
        assumeTrue(emptyType != null,
                "Every user type has members on this project, so an empty filter cannot be"
                        + " produced to look at");

        // The same gap the Sides lists have: an empty RecyclerView draws nothing, so a
        // filter that matched nothing is indistinguishable from a list that failed to load.
        assertTrue(list.emptyStateShown(),
                "Filtering by \"" + emptyType + "\" matched nothing and the list area is"
                        + " simply blank — no illustration, no message. There is no way to"
                        + " tell an empty filter from a failed load.");
        if (list.openFilters()) list.chooseFilter(ExternalUserListPage.FILTER_ALL);
        sleep(2000);
    }

    @Test
    @Order(42)
    @DisplayName("EU-41 The add button is named for what it actually opens")
    void addButtonIsNamedCorrectly() {
        ExternalUserListPage list = new ExternalUserListPage(driver);
        assumeTrue(list.isLoaded(Duration.ofSeconds(20)), "The listing did not load");
        String label = list.addButtonLabel();
        assumeTrue(label != null && !label.isBlank(),
                "The add button carries no name at all, so a screen reader has nothing to"
                        + " announce");

        // It opens ADD EXTERNAL USER. Anyone working by ear is told it adds a unit.
        assertFalse(label.toLowerCase().contains("unit"),
                "The add button on the External Users list is named \"" + label + "\","
                        + " but it opens the Add External User form.");
    }

    // ─────────────────────── web gap cases (sheet bugs EXT-01 / EXT-02) ───────────────────────

    /** Titles the web refuses in "Add More Information": they repeat the form's own fields. */
    private static final List<String> RESERVED_TITLES =
            List.of("Full Name", "full name", "FULL NAME", "e-mail", "Email");

    @Test
    @Order(43)
    @DisplayName("EU-43 One tap on the toolbar back chevron leaves the add, detail and update screens [WEB EXT-02]")
    void backChevronOneTap() {
        // [WEB EXT-02] The web needs two taps. Android wires backBtn once
        // (BaseActivity.kt:524 → onBackPressedDispatcher), so one tap must be enough.
        // Self-contained, so it runs alone (--only 'ExternalUserTest.backChevronOneTap'): it
        // saves its own ZL AUTO user for the detail and update screens and deletes it after.
        ExternalUserFormPage add = openAddForm();
        assumeTrue(add.onAddScreen(), "The add form's header reads \"" + add.header() + "\"");
        assertLeavesInOneTap(add, "ADD EXTERNAL USER");

        String name = fixtureName("Back");
        String email = fixtureEmail();
        ExternalUserFormPage f = openAddForm();
        assumeTrue(f.setFullName(name) && f.setEmail(email)
                        && f.chooseUserType(ExternalUserFormPage.TYPE_VENDOR) && f.save(),
                "Could not save a fixture user for the detail and update screens");
        sleep(4000);
        assumeTrue(!f.stillOpen(), "The fixture user was refused: " + f.visibility());
        try {
            ExternalUserListPage list = new ExternalUserListPage(driver);
            assumeTrue(list.isLoaded(Duration.ofSeconds(20)) && list.search(FIXTURE_PREFIX),
                    "The listing did not come back");
            assumeTrue(list.viewDetails(name), "View More was not available on \"" + name + "\"");
            ExternalUserFormPage detail = new ExternalUserFormPage(driver);
            boolean detailOpen = detail.isLoaded(Duration.ofSeconds(15)) && detail.onDetailScreen();
            if (!detailOpen) backToModule();
            assumeTrue(detailOpen, "The detail screen did not open; header \"" + detail.header() + "\"");
            // [WEB EXT-01b] also checked here, so it has a standalone run: the stored email is
            // exactly what was typed, all lower case.
            String shown = String.valueOf(detail.email()).trim();
            assertEquals(email, shown, "EXT-01b: saved \"" + email + "\", detail shows \"" + shown + "\"");
            assertLeavesInOneTap(detail, "USER DETAILS");

            ExternalUserListPage again = new ExternalUserListPage(driver);
            assumeTrue(again.isLoaded(Duration.ofSeconds(20)) && again.search(FIXTURE_PREFIX),
                    "The listing did not come back");
            assumeTrue(again.edit(name), "Edit not offered on \"" + name + "\"");
            ExternalUserFormPage update = new ExternalUserFormPage(driver);
            // Identified by its "Update" button, not its header: the edit form's header reads
            // "ADD EXTERNAL USER", because initView() sets the title before handleIntentData()
            // sets isEdit (AddUpdateExternalUserPage.kt:177-180, :190-192) — a separate app bug.
            boolean updateOpen = update.isLoaded(Duration.ofSeconds(15))
                    && "Update".equalsIgnoreCase(String.valueOf(update.saveLabel()).trim());
            if (!updateOpen) backToModule();
            assumeTrue(updateOpen, "The update form did not open; header \"" + update.header()
                    + "\", button \"" + update.saveLabel() + "\"");
            System.out.println(">>> EU-43 note: the update form's header reads \"" + update.header() + "\"");
            assertLeavesInOneTap(update, "UPDATE EXTERNAL USER");
        } finally {
            deleteFixture(name);
        }
    }

    /** Deletes one fixture user by name; never throws (finally blocks). */
    private void deleteFixture(String name) {
        try {
            backToModule();
            ExternalUserListPage l = new ExternalUserListPage(driver);
            if (l.isLoaded(Duration.ofSeconds(15)) && l.search(FIXTURE_PREFIX) && l.delete(name)
                    && l.deleteConfirmShown(Duration.ofSeconds(6))) {
                l.confirmDelete();
                System.out.println(">>> fixture \"" + name + "\" deleted");
            } else {
                System.out.println("!!! fixture \"" + name + "\" not deleted — EU-38 sweeps ZL AUTO users");
            }
            l.clearSearch();
        } catch (Throwable t) {
            System.out.println("!!! fixture cleanup failed: " + t);
        }
    }

    private void assertLeavesInOneTap(ExternalUserFormPage form, String screen) {
        // How many AddUpdateExternalUserPage instances are on the back stack. Two would mean
        // the screen was opened twice (a test artefact), not that the chevron needs two taps.
        int stacked = stackedFormScreens();
        String tag = screen.toLowerCase().replaceAll("[^a-z]+", "-");
        boolean kb = keyboardShown();
        screenshot("eu43-" + tag + "-before");
        assertTrue(form.tapToolbarBack(), "The toolbar back chevron could not be tapped on " + screen);
        sleep(2000);
        screenshot("eu43-" + tag + "-after-tap1");
        System.out.println(">>> EU-43 " + screen + ": keyboardBefore=" + kb + " activityAfter=" + resumedActivity());
        boolean listing = new ExternalUserListPage(driver).isLoaded(Duration.ofSeconds(5));
        String header = String.valueOf(new ExternalUserFormPage(driver).header()).trim();
        boolean stillThere = new ExternalUserFormPage(driver).isLoaded(Duration.ofSeconds(1));
        String second = "";
        if (!listing) {
            // Diagnose before recovering: does a SECOND tap leave (the EXT-02 symptom), or is
            // the screen stuck?
            boolean tapped2 = new ExternalUserFormPage(driver).tapToolbarBack();
            sleep(2000);
            screenshot("eu43-" + tag + "-after-tap2");
            boolean listing2 = new ExternalUserListPage(driver).isLoaded(Duration.ofSeconds(5));
            second = " A second tap " + (tapped2 ? (listing2 ? "DID leave" : "did not leave either") : "was not possible") + ".";
            if (!listing2) backToModule();     // never leave the next step stranded
        }
        System.out.println(">>> EU-43 " + screen + ": stacked=" + stacked + " oneTapLeft=" + (listing && !stillThere) + second);
        assumeTrue(stacked <= 1, screen + " was on the back stack " + stacked
                + " times before the tap, so one tap could only uncover the other copy — a test artefact, not EXT-02");
        assertTrue(listing && !stillThere,
                "EXT-02: one tap on the back chevron did not leave " + screen
                        + " (listing shown=" + listing + ", header now \"" + header + "\")."
                        + second + " Back-stack copies of the screen before the tap: "
                        + (stacked < 0 ? "unknown" : String.valueOf(stacked)) + ".");
    }

    private boolean keyboardShown() {
        try { return driver.isKeyboardShown(); } catch (Exception e) { return false; }
    }

    /** Count of AddUpdateExternalUserPage records in the activity stack, via adb; -1 if unknown. */
    private int stackedFormScreens() {
        try {
            String udid = System.getenv().getOrDefault("ZILLIT_UDID", "");
            String sdk = System.getenv().getOrDefault("ANDROID_HOME",
                    System.getProperty("user.home") + "/Library/Android/sdk");
            java.util.List<String> cmd = new java.util.ArrayList<>(List.of(sdk + "/platform-tools/adb"));
            if (!udid.isBlank()) { cmd.add("-s"); cmd.add(udid); }
            cmd.addAll(List.of("shell", "dumpsys", "activity", "activities"));
            ProcessBuilder pb = new ProcessBuilder(cmd).redirectErrorStream(true);
            pb.environment().put("ADB_MDNS", "0");
            Process pr = pb.start();
            String out = new String(pr.getInputStream().readAllBytes());
            pr.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);
            int n = 0;
            for (String line : out.split("\n")) {
                if (line.contains("Hist") && line.contains("AddUpdateExternalUserPage")) n++;
            }
            return n;
        } catch (Exception e) {
            return -1;
        }
    }

    @Test
    @Order(44)
    @DisplayName("EU-42 \"Add More Information\" refuses titles that repeat the form's own fields [WEB EXT-01a]")
    void reservedInfoTitlesRefused() {
        // [WEB EXT-01a] The dialog (UIExtentions.showDialogWithMultipleEditBox) checks only
        // that title and description are non-empty, so every reserved title is expected to
        // be accepted today — this case then FAILS, naming EXT-01. Nothing is ever saved:
        // the form is left with back.
        ExternalUserFormPage f = openAddForm();
        java.util.List<String> accepted = new java.util.ArrayList<>();
        try {
            for (String title : RESERVED_TITLES) {
                int before = f.moreInfoTitles().size();
                assumeTrue(f.addMoreInfo(), "\"Add More Information\" could not be tapped");
                assumeTrue(f.moreInfoDialogShown(), "The Add More Information dialog did not open");
                boolean closed = f.submitMoreInfo(title, "x");
                if (!closed) {
                    // Refused: the dialog stayed. Cancel it (never BACK twice into the list).
                    try {
                        for (var b : driver.findElements(byAppId("leftButton"))) {
                            if (b.isDisplayed()) { b.click(); break; }
                        }
                    } catch (Exception ignored) { }
                    sleep(800);
                }
                List<String> rows = f.moreInfoTitles();
                System.out.println(">>> EU-42 '" + title + "' dialogClosed=" + closed + " rows=" + rows);
                // The dialog closing on Add IS the acceptance: a refusal keeps it open with an
                // error on the field (as it does for an empty title). The row count is only
                // logged — recMoreField sits near the bottom and later rows can be off screen.
                if (closed) accepted.add(title);
            }
        } finally {
            closeFormDiscarding();
        }
        assertTrue(accepted.isEmpty(),
                "EXT-01: \"Add More Information\" accepted titles that repeat the form's own"
                        + " fields: " + accepted + ". The dialog only checks for an empty title"
                        + " (UIExtentions.kt showDialogWithMultipleEditBox), so a user can carry"
                        + " a second \"Full Name\" or \"Email\" with different content.");
    }

    /** Backs out of the add form (and any dialog over it) to the listing, saving nothing. */
    private void closeFormDiscarding() {
        for (int i = 0; i < 4; i++) {
            if (new ExternalUserListPage(driver).isLoaded(Duration.ofSeconds(2))) return;
            if (!new ExternalUserFormPage(driver).isLoaded(Duration.ofSeconds(1))
                    && !new ExternalUserFormPage(driver).moreInfoDialogShown()) break;
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(1200);
        }
        backToModule();
    }

    @Test
    @Order(60)
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
    /**
     * Gets to the External Users listing by the shortest route that works.
     *
     * <p>The app is opened once and a project entered once, for the whole suite. After
     * that every case starts from wherever the last one finished, and the cheapest true
     * answer wins:
     *
     * <ol>
     *   <li>already on the listing — do nothing;</li>
     *   <li>inside the project — go through Tools, and nothing else;</li>
     *   <li>somewhere else in the app — step back until one of the above;</li>
     *   <li>on the project list — enter a project, which happens once.</li>
     * </ol>
     *
     * <p>Nothing here ever restarts the app or re-enters the project to recover. Watching
     * splash, login and the project list scroll past before every case is not just slow:
     * it throws away the state each case leaves for the next, which is the whole point of
     * a suite that creates a user, finds it, renames it and then deletes it.
     */
    private void openExternalUsers() {
        ExternalUserListPage listing = new ExternalUserListPage(driver);
        BottomNavPage nav = new BottomNavPage(driver);

        // 1 — already there.
        if (listing.isLoaded(Duration.ofSeconds(5))) return;

        // 2 — inside the project: Tools is one tap away.
        if (nav.waitUntilLoaded(Duration.ofSeconds(3))) {
            openFromTools();
            return;
        }

        // 3 — somewhere else in the app: step back, never off a screen that is a place to
        // start from, and never out of the app.
        for (int i = 0; i < 6; i++) {
            if (listing.isLoaded(Duration.ofSeconds(2))) return;
            if (nav.waitUntilLoaded(Duration.ofSeconds(2))) { openFromTools(); return; }
            if (onProjectList()) break;
            if (!appPackage.equals(safePackage())) break;
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(900);
        }

        // 4 — the project list: go in. Once per suite in practice, because nothing above
        // ever comes back here.
        if (onProjectList()) {
            ProjectListPage projects = new ProjectListPage(driver);
            List<String> names = projects.visibleProjectNames();
            assumeTrue(!names.isEmpty(),
                    "The project list is open but shows no projects to enter."
                            + " This account may have none on this device.");
            assumeTrue(projects.tapProject(names.get(0)),
                    "Could not enter \"" + names.get(0) + "\". Projects listed: " + names);
        }

        if (!nav.waitUntilLoaded(Duration.ofSeconds(60))) {
            assumeTrue(false, "The bottom navigation never loaded. On screen instead: "
                    + whereAreWe());
        }
        openFromTools();
    }

    /**
     * Tools tab → External Users. The only recovery route a case ever needs.
     *
     * <p>Deliberately separate: going back to Tools is a tab tap and a list open, which is
     * what a person does. Restarting the app to reach the same screen is not.
     */
    private void openFromTools() {
        ToolsPage tools = new ToolsPage(driver);
        assumeTrue(tools.openToolsTab(), "The Tools tab could not be opened");
        assumeTrue(tools.isLoaded(Duration.ofSeconds(30)), "The Tools tab did not load");
        tools.searchTools("External");
        sleep(1200);
        if (!tools.openIfVisible(TOOL)) {
            assumeTrue(false, "\"" + TOOL + "\" is not offered in Tools. Tools visible: "
                    + tools.toolNames());
        }
        assumeTrue(new ExternalUserListPage(driver).isLoaded(Duration.ofSeconds(25)),
                "The External Users listing did not open");
    }

    /**
     * Are we on the project list? Asked without waiting, and without throwing.
     *
     * <p>{@code ProjectListPage.isLoaded()} reads like a predicate and is not one: it calls
     * {@code waitVisible}, which blocks for the full 30-second timeout and then throws
     * TimeoutException. Using it as a question meant that every time the answer was "no" —
     * the ordinary case, since the app is usually inside a project — the case died on a
     * timeout instead of moving on. A guard has to be cheap and total: it answers now, and
     * it answers for every screen.
     */
    private boolean onProjectList() {
        try {
            return !driver.findElements(byAppId("projectListRecyclerView")).isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /** A short description of where the app is, for a message that can be acted on. */
    private String whereAreWe() {
        String pkg = safePackage();
        if (!appPackage.equals(pkg)) {
            return "a different app (" + (pkg.isEmpty() ? "unreadable" : pkg) + ")";
        }
        if (onProjectList()) return "the project list";
        if (new ExternalUserListPage(driver).isLoaded(Duration.ofSeconds(1))) {
            return "the External Users listing";
        }
        return "activity " + resumedActivity();
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

    /**
     * Comes back to a freshly loaded listing.
     *
     * <p>Used when an assertion is about to say something serious about the data — that a
     * user survived a cancelled delete, say. Reading the list while its rows are being
     * re-bound after a dialog closes can return nothing at all, and "nothing" is
     * indistinguishable from "deleted".
     */
    private ExternalUserListPage reopenListing() {
        openFromTools();
        ExternalUserListPage fresh = new ExternalUserListPage(driver);
        assumeTrue(fresh.isLoaded(Duration.ofSeconds(20)), "The listing did not reload");
        return fresh;
    }

    /** Leaves the form without leaving the module. */
    private void closeForm() {
        ExternalUserFormPage f = new ExternalUserFormPage(driver);
        if (!f.isLoaded(Duration.ofSeconds(1))) return;
        try { driver.navigate().back(); } catch (Exception ignored) { }
        sleep(1500);
    }

    /** Comes back from wherever a hand-over took us, without acting on that screen. */
    /**
     * Comes back from wherever a hand-over took us — the Email module, the dialer —
     * without acting on that screen.
     *
     * <p>Guarded the same way as {@link #openExternalUsers()}: stepping back is for
     * escaping screens we did not mean to be on, and it stops at the project list, at the
     * bottom navigation, or at the edge of the app. An unguarded run of BACK presses walks
     * straight off the project list and out of the app, and then every later case relaunches
     * and lands in the same place — the open-and-close loop.
     */
    private void backToModule() {
        ProjectListPage projects = new ProjectListPage(driver);
        BottomNavPage nav = new BottomNavPage(driver);
        for (int i = 0; i < 6; i++) {
            if (new ExternalUserListPage(driver).isLoaded(Duration.ofSeconds(2))
                    || new ExternalUserFormPage(driver).isLoaded(Duration.ofSeconds(1))) {
                break;
            }
            if (onProjectList()) break;
            if (nav.waitUntilLoaded(Duration.ofSeconds(2))) break;
            // Being in ANOTHER app is the ordinary case here — this runs after a hand-over
            // to Email or to the dialer, and BACK is how a person comes back. Stopping at
            // the app's edge left the dialer on screen, so the next step could not find a
            // landing and reported a case that had in fact done exactly what it set out
            // to do.
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
