package com.zillit.automation.tests.location;

import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.location.LocationEntriesPage;
import com.zillit.automation.pages.map.LocationToolPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tools → LOCATION, the whole tool on one device (Vidya Testing, emulator): toolbar,
 * tabs, group-by, search, the add form and every client-side validation in
 * {@code LocationDetailPage.validate()}, a ZL_AUTO location's full life — create in Selects,
 * episode listing, entries page, view / edit, a second entry, move to Shortlist and Final,
 * Generate Full PDF, delete — plus the row menus each level offers per tab.
 *
 * <p>Case ids LOC-xx match {@code LOCATION_TOOL_TESTCASES.md}. The suite writes only
 * ZL_AUTO-named locations and deletes them (every tab, since Move leaves a copy) in
 * LOC-99 and again in the base class's @AfterAll sweep.
 *
 * <pre>
 * ZILLIT_FLAVOR=dev ZILLIT_UDID=emulator-5554 ./run-and-report.sh --only LocationToolFullTest
 * </pre>
 */
@Order(TestOrder.LOCATION_TOOL_FULL)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LocationToolFullTest extends LocationBaseTest {

    private static final String SELECTS = LocationToolPage.TAB_SELECT;
    private static final String SHORTLIST = LocationToolPage.TAB_SHORTLIST;
    private static final String FINAL = LocationToolPage.TAB_FINAL;

    private static final String SCENE = "901";
    private static final String CITY = "ZL Auto City";
    private static final String ADDRESS = "1 ZL Auto Street";
    private static final String DESC = "ZL_AUTO description";
    private static final String DESC_EDITED = "ZL_AUTO description edited";
    private static final String NEW_EPISODE = "9901";

    /** The lifecycle fixture. */
    private final String locA = uniqueName("Loc");
    /** The validation forms' name — never meant to be saved; swept if a check let it through. */
    private final String locV = uniqueName("Val");
    /** A location filed under its own episode (LOC-50). */
    private final String locB = uniqueName("Ep");

    /** Tab the fixture is known to be in (the furthest stage), or null. */
    private String fixtureStage;
    /** The validation form is open with a link attachment (LOC-17…LOC-24). */
    private boolean validationFormOpen;

    // ═════════════════════════ structure ═════════════════════════

    @Test
    @Order(1)
    @DisplayName("[LOC-01] The tool's toolbar reads 'Location' and the tab strip is Selects / Shortlist / Final")
    void headerAndTabs() {
        LocationToolPage tool = openTool();
        assertEquals("location", String.valueOf(tool.toolbarHeader()).trim().toLowerCase(),
                "Toolbar header (Tools.kt passes header = R.string.location)");
        assertEquals(LocationToolPage.TABS_IN_ORDER, tool.tabLabels(), "Tab labels / order");
    }

    @Test
    @Order(2)
    @DisplayName("[LOC-02] Every tab has the search box ('Search by Scene No/Location Name/City'), + and camera")
    void everyTabHasSearchAndAdd() {
        for (String tab : LocationToolPage.TABS_IN_ORDER) {
            LocationToolPage tool = openTab(tab);
            String hint = tool.searchFieldText();
            assertTrue(hint != null && hint.contains("Search by Scene No/Location Name/City"),
                    tab + ": search hint was " + hint);
            assertTrue(tool.addButtonShown(), tab + ": the + (takeSceneShots) button is missing");
            assertTrue(tool.cameraButtonShown(), tab + ": the camera (uploadFromCam) button is missing");
        }
    }

    @Test
    @Order(3)
    @DisplayName("[LOC-03] Unit chat: the chat icon shows on Selects and Shortlist, not on Final")
    void unitChatPerTab() {
        assertTrue(openTab(SELECTS).unitChatShown(), "Selects should offer its unit chat (chatIv)");
        assertTrue(openTab(SHORTLIST).unitChatShown(), "Shortlist should offer its unit chat (chatIv)");
        assertFalse(openTab(FINAL).unitChatShown(),
                "Final must not offer a unit chat (handleUnitChat: chatRl hidden for PUBLISH)");
    }

    @Test
    @Order(31)
    @DisplayName("[LOC-04] TV project: every tab groups its rows as 'Episode No : N'")
    void groupedByEpisode() {
        int seen = 0;
        for (String tab : LocationToolPage.TABS_IN_ORDER) {
            List<String> rows = openTab(tab).rowLabels();
            for (String r : rows) {
                assertTrue(r.startsWith("Episode No : "), tab + " row is not an episode group: " + r);
            }
            seen += rows.size();
        }
        assumeTrue(seen > 0, "No rows in any tab to check the grouping on");
    }

    @Test
    @Order(5)
    @DisplayName("[LOC-05] The group-by icon offers exactly Scene no / Episode No / location name")
    void groupByMenuItems() {
        LocationToolPage tool = openTab(SELECTS);
        assertTrue(tool.groupByIconShown(), "No group-by icon (toolbar settingImage)");
        List<String> items = tool.openGroupByMenu();
        tool.back();
        assertEquals(Set.of("Filter by Scene no", "Filter by Episode No", "Filter by location name"),
                new java.util.HashSet<>(items),
                "Group-by items (LocationPage removes city/gender/talent/character/contact): " + items);
    }

    @Test
    @Order(6)
    @DisplayName("[LOC-06] Search with an empty field says 'Please Enter something for search'")
    void emptySearchRefused() {
        LocationToolPage tool = openTab(SELECTS);
        tool.clearSearch();
        tool.tapSearchIcon();
        assertTrue(tool.messageShown("Please Enter something for search", Duration.ofSeconds(4)),
                "No 'Please Enter something for search' snackbar; snackbar=" + tool.snackbarText());
    }

    @Test
    @Order(31)
    @DisplayName("[LOC-07] A search that matches nothing empties the list; clearing it restores the rows")
    void searchNoMatchAndClear() {
        LocationToolPage tool = openTab(SELECTS);
        int before = tool.folderCount();
        assumeTrue(before > 0, "Selects is empty — nothing to filter");
        tool.search("zzqq_no_such_location");
        assertEquals(0, tool.folderCount(), "A no-match search still lists " + tool.rowLabels());
        tool.clearSearch();
        sleep(1500);
        assertEquals(before, tool.folderCount(), "Clearing the search did not restore the rows");
    }

    // ═════════════════════════ the add form ═════════════════════════

    private LocationToolPage freshForm(String tab) {
        LocationToolPage tool = openTab(tab);
        assumeTrue(tool.openAddForm(), "The + form did not open from " + tab);
        return tool;
    }

    @Test
    @Order(10)
    @DisplayName("[LOC-10] '+' opens 'Add Location Details' with Upload Media, Pick on map, every field and Submit")
    void formLayout() {
        LocationToolPage tool = freshForm(SELECTS);
        try {
            assertEquals("add location details", String.valueOf(tool.headerText()).trim().toLowerCase(), "Form header");
            assertTrue(tool.uploadMediaShown(), "No 'Upload Media' in the form toolbar");
            assertTrue(tool.locationSectionShown(), "No Location block (Pick on map) on a create form");
            for (String f : new String[]{LocationToolPage.FIELD_NAME, LocationToolPage.FIELD_EPISODE,
                    LocationToolPage.FIELD_SCENE, LocationToolPage.FIELD_ADDRESS, LocationToolPage.FIELD_CITY,
                    "descInc", "contactNameInc", "emailInc"}) {
                assertNotNull(tool.fieldText(f), "Field " + f + " is missing from the form");
            }
            assertNotNull(tool.countryCode(), "No phone country code (countryCodeText)");
            assertTrue(tool.submitShown(), "No Submit button");
        } finally {
            tool.back();
        }
    }

    @Test
    @Order(11)
    @DisplayName("[LOC-11] Submit with no media or link: 'Location Attachment Required.', the form stays")
    void attachmentRequired() {
        LocationToolPage tool = freshForm(SELECTS);
        try {
            tool.setField(LocationToolPage.FIELD_NAME, locV);
            created.computeIfAbsent(locV, k -> new LinkedHashSet<>()).add(EP);
            assertTrue(tool.submitForm(), "Submit not tappable");
            assertTrue(tool.messageShown(LocationToolPage.ERR_ATTACHMENT, Duration.ofSeconds(5)),
                    "No 'Location Attachment Required.'; snackbar=" + tool.snackbarText());
            assertTrue(tool.formShown(Duration.ofSeconds(3)), "The form closed without an attachment");
        } finally {
            tool.back();
        }
    }

    @Test
    @Order(12)
    @DisplayName("[LOC-12] The maps Link field is not a text box: tapping it opens the map picker")
    void linkFieldOpensPicker() {
        LocationToolPage tool = freshForm(SELECTS);
        try {
            assertTrue(tool.tapLinkFieldOpensPicker(),
                    "Tapping the Link field did not open the map picker (handleLocationSection: the link is"
                            + " derived from the pin, keyListener = null, click → launchPlacePicker)");
        } finally {
            for (int i = 0; i < 3 && !tool.formShown(Duration.ofSeconds(2)); i++) tool.back();
            tool.back();
        }
    }

    @Test
    @Order(13)
    @DisplayName("[LOC-13] Scene No. keeps only letters/digits and at most 15 characters")
    void sceneFilter() {
        LocationToolPage tool = freshForm(SELECTS);
        try {
            String v = tool.typeKeys(LocationToolPage.FIELD_SCENE, "12-AB#c45678901234567");
            assertNotNull(v, "Scene field unreadable");
            assertTrue(v.matches("[A-Za-z0-9]*"), "Scene No. kept a non-alphanumeric character: " + v);
            assertTrue(v.length() <= 15, "Scene No. is longer than 15: " + v + " (" + v.length() + ")");
        } finally {
            tool.back();
        }
    }

    @Test
    @Order(14)
    @DisplayName("[LOC-14] Episode no. keeps only digits and commas")
    void episodeFilter() {
        LocationToolPage tool = freshForm(SELECTS);
        try {
            String v = tool.typeKeys(LocationToolPage.FIELD_EPISODE, "1a,2b");
            assertEquals("1,2", v, "Episode filter (getNumericWithCommaOnlyFilter)");
        } finally {
            tool.back();
        }
    }

    @Test
    @Order(15)
    @DisplayName("[LOC-15] Upload Media in Selects offers a Link option")
    void selectsPickerHasLink() {
        LocationToolPage tool = freshForm(SELECTS);
        try {
            List<String> opts = tool.openUploadMediaOptions();
            assertTrue(opts.contains("Link"), "No 'Link' in the Selects picker: " + opts);
        } finally {
            tool.back();
            if (tool.formShown(Duration.ofSeconds(2))) tool.back();
        }
    }

    @Test
    @Order(16)
    @DisplayName("[LOC-16] Upload Link: empty → 'Please Enter URL', non-URL → 'Please Enter Valid URL', Cancel closes")
    void linkDialogValidation() {
        LocationToolPage tool = freshForm(SELECTS);
        try {
            assumeTrue(tool.openLinkDialog(), "Upload Link dialog did not open");
            tool.tapDialogButton(true);
            assertTrue(tool.linkDialogShown(Duration.ofSeconds(3)), "Empty Submit closed the link dialog");
            assertTrue(tool.messageShown("Please Enter URL", Duration.ofSeconds(4)), "No 'Please Enter URL' error");

            tool.pasteIntoLinkDialog("not a url");
            assertEquals("not a url", String.valueOf(tool.linkDialogValue()).trim(), "Paste icon did not fill the field");
            tool.tapDialogButton(true);
            assertTrue(tool.linkDialogShown(Duration.ofSeconds(3)), "A non-URL was accepted as a link");
            assertTrue(tool.messageShown("Please Enter Valid URL", Duration.ofSeconds(4)), "No 'Please Enter Valid URL' error");

            tool.tapDialogButton(false);
            assertFalse(tool.linkDialogShown(Duration.ofSeconds(2)), "Cancel did not close the link dialog");
            assertTrue(tool.formShown(Duration.ofSeconds(3)), "Cancel left the form");
        } finally {
            if (tool.linkDialogShown(Duration.ofSeconds(1))) tool.tapDialogButton(false);
            tool.back();
        }
    }

    // ── validation chain on one form (link attachment present, so each check is reached) ──

    private LocationToolPage validationForm() {
        assumeTrue(validationFormOpen, "The validation form (LOC-17) is not open");
        LocationToolPage tool = new LocationToolPage(driver);
        assumeTrue(tool.formShown(Duration.ofSeconds(5)), "The validation form is no longer on screen");
        return tool;
    }

    /** Submits and asserts the form refused with {@code msg} (error balloon, snackbar, or focus on the field). */
    private void expectRefusal(LocationToolPage tool, String msg, String field) {
        assumeTrue(tool.formShown(Duration.ofSeconds(3)), "The form was lost before Submit (a stray BACK?)");
        assertTrue(tool.submitForm(), "Submit not tappable");
        boolean shown = tool.messageShown(msg, Duration.ofSeconds(5));
        boolean focused = field != null && tool.fieldFocused(field);
        boolean stillOpen = tool.formShown(Duration.ofSeconds(3));
        if (!stillOpen) {
            validationFormOpen = false;
            throw new AssertionError("The form SAVED instead of refusing with '" + msg + "' — \""
                    + locV + "\" may now exist (it is swept at the end)");
        }
        assertTrue(shown || focused, "Expected '" + msg + "'" + (field == null ? "" : " / focus on " + field)
                + "; snackbar=" + tool.snackbarText());
    }

    @Test
    @Order(17)
    @DisplayName("[LOC-17] Upload Media → Link → paste → Submit accepts a URL and returns to the form")
    void linkAttachmentListed() {
        LocationToolPage tool = freshForm(SELECTS);
        created.computeIfAbsent(locV, k -> new LinkedHashSet<>()).add(EP);
        assertTrue(tool.addLinkAttachment(LINK), "Upload Link did not accept a pasted https:// URL");
        assertFalse(tool.linkDialogShown(Duration.ofSeconds(2)), "The Upload Link dialog stayed open after Submit");
        assertTrue(tool.formShown(Duration.ofSeconds(5)), "The form was lost after adding the link");
        // Not asserted: a preview. A link whose page has no og:image adds nothing visible
        // (loadImageForLink hides imageHolder; linkLi has no content) — see LOC-Q3.
        validationFormOpen = true;
    }

    @Test
    @Order(18)
    @DisplayName("[LOC-18] No name → 'Please Enter Name'")
    void nameRequired() {
        LocationToolPage tool = validationForm();
        tool.setField(LocationToolPage.FIELD_NAME, "");
        expectRefusal(tool, "Please Enter Name", LocationToolPage.FIELD_NAME);
    }

    @Test
    @Order(19)
    @DisplayName("[LOC-19] Scene No. starting with a letter → 'Scene No. always starts with a number.'")
    void sceneMustStartWithDigit() {
        LocationToolPage tool = validationForm();
        tool.setField(LocationToolPage.FIELD_NAME, locV);
        tool.setField(LocationToolPage.FIELD_SCENE, "A12");
        expectRefusal(tool, "Scene No. always starts with a number.", LocationToolPage.FIELD_SCENE);
    }

    @Test
    @Order(20)
    @DisplayName("[LOC-20] Duplicate episodes '1,1' → 'Duplicate episode numbers found. Please remove duplicates.'")
    void duplicateEpisodes() {
        LocationToolPage tool = validationForm();
        tool.setField(LocationToolPage.FIELD_SCENE, SCENE);
        tool.setField(LocationToolPage.FIELD_EPISODE, "1,1");
        expectRefusal(tool, "Duplicate episode numbers found", LocationToolPage.FIELD_EPISODE);
    }

    @Test
    @Order(21)
    @DisplayName("[LOC-21] Contact name with no email or phone → 'Please enter email or phone number'")
    void contactNeedsEmailOrPhone() {
        LocationToolPage tool = validationForm();
        tool.setField(LocationToolPage.FIELD_EPISODE, EP);
        tool.setField("contactNameInc", "ZL_AUTO Contact");
        expectRefusal(tool, "Please enter email or phone number", null);
    }

    @Test
    @Order(22)
    @DisplayName("[LOC-22] A malformed email → 'Please Enter Valid Email.'")
    void invalidEmail() {
        LocationToolPage tool = validationForm();
        tool.setField("emailInc", "zl_auto_not_an_email");
        expectRefusal(tool, "Please Enter Valid Email.", null);
    }

    @Test
    @Order(23)
    @DisplayName("[LOC-23] A 2-digit phone → 'Phone number must be minimum 5 and maximum 20 digit…'")
    void shortPhone() {
        LocationToolPage tool = validationForm();
        tool.setField("emailInc", "");
        assumeTrue(tool.setPhone("12"), "Phone field not reachable");
        expectRefusal(tool, "Phone number must be minimum 5", null);
    }

    @Test
    @Order(24)
    @DisplayName("[LOC-24] A phone with no country code → 'Please Select Country Code First.'")
    void phoneNeedsCountryCode() {
        LocationToolPage tool = validationForm();
        String cc = tool.countryCode();
        if (cc != null && !cc.isEmpty() && !"Code".equalsIgnoreCase(cc)) {
            tool.setPhone("");
            assumeTrue(false, "The country code is pre-filled (" + cc + "); a valid phone would save the form");
        }
        assumeTrue(tool.setPhone("9876543210"), "Phone field not reachable");
        expectRefusal(tool, "Please Select Country Code First.", null);
    }

    @Test
    @Order(25)
    @DisplayName("[LOC-25] BACK from a filled form saves nothing")
    void backDiscards() {
        LocationToolPage tool = new LocationToolPage(driver);
        if (validationFormOpen && tool.formShown(Duration.ofSeconds(2))) {
            tool.back();
            if (tool.formShown(Duration.ofSeconds(2))) tool.back();
        }
        validationFormOpen = false;
        LocationToolPage list = rowInTab(SELECTS, locV, EP, Duration.ofSeconds(8));
        assertTrue(list == null, "\"" + locV + "\" was saved although every submit was refused / the form was backed out");
        created.remove(locV);
    }

    @Test
    @Order(26)
    @DisplayName("[LOC-26] Shortlist '+': empty episode → 'Episode no. cannot be empty.', empty scene → 'Please Enter Scene No.'")
    void shortlistRequiresEpisodeAndScene() {
        LocationToolPage tool = freshForm(SHORTLIST);
        created.computeIfAbsent(locV, k -> new LinkedHashSet<>()).add(EP);
        validationFormOpen = true;
        try {
            assumeTrue(tool.addLinkAttachment(LINK), "Could not add a link attachment");
            tool.setField(LocationToolPage.FIELD_NAME, locV);
            expectRefusal(tool, "Episode no. cannot be empty.", LocationToolPage.FIELD_EPISODE);
            tool.setField(LocationToolPage.FIELD_EPISODE, EP);
            expectRefusal(tool, "Please Enter Scene No.", LocationToolPage.FIELD_SCENE);
        } finally {
            validationFormOpen = false;
            tool.back();
            if (tool.formShown(Duration.ofSeconds(2))) tool.back();
        }
        assertTrue(rowInTab(SHORTLIST, locV, EP, Duration.ofSeconds(5)) == null, "A refused Shortlist form saved \"" + locV + "\"");
        created.remove(locV);
    }

    @Test
    @Order(27)
    @DisplayName("[LOC-27] Upload Media in Final offers no Link (Final entries need real media)")
    void finalPickerHasNoLink() {
        LocationToolPage tool = freshForm(FINAL);
        try {
            List<String> opts = tool.openUploadMediaOptions();
            assumeTrue(!opts.isEmpty(), "The picker did not open");
            assertFalse(opts.contains("Link"), "Final's picker offers 'Link' (PICKER_ITEM_LINK is excluded for PUBLISH): " + opts);
        } finally {
            tool.back();
            if (tool.formShown(Duration.ofSeconds(2))) tool.back();
        }
    }

    @Test
    @Order(28)
    @DisplayName("[LOC-28] Pick on map fills a google.com/maps?q= link; Copy puts it on the clipboard ('Copied To Clipboard.')")
    void pickOnMapAndCopy() {
        LocationToolPage tool = freshForm(SELECTS);
        try {
            String link = tool.pickCurrentLocationOnMap();
            assumeTrue(link != null, "The map picker returned no pin on this device");
            assertTrue(link.matches("https://www\\.google\\.com/maps\\?q=-?\\d+\\.\\d{6},-?\\d+\\.\\d{6}"),
                    "Link format (buildMapsLink, Locale.US %.6f): " + link);
            String snack = tool.copyMapLink();
            assertTrue((snack != null && snack.contains("Copied To Clipboard"))
                            || tool.messageShown("Copied To Clipboard", Duration.ofSeconds(3)),
                    "No 'Copied To Clipboard.' after Copy; snackbar=" + snack);
            assertEquals(link, String.valueOf(tool.clipboard()).trim(), "Clipboard after Copy");
        } finally {
            tool.back();
            if (tool.formShown(Duration.ofSeconds(2))) tool.back();
        }
    }

    // ═════════════════════════ lifecycle of a ZL_AUTO location ═════════════════════════

    private void requireFixture(String stage) {
        assumeTrue(fixtureStage != null, "LOC-30 did not create \"" + locA + "\"");
        if (stage != null) assumeTrue(stage.equals(fixtureStage), "The fixture is in " + fixtureStage + ", not " + stage);
    }

    @Test
    @Order(30)
    @DisplayName("[LOC-30] A ZL_AUTO location saved from Selects '+' is listed under 'Episode No : 1'")
    void createInSelects() {
        assertTrue(createLocation(SELECTS, locA, SCENE, EP, CITY, ADDRESS, DESC),
                "The form did not accept a complete location; snackbar=" + new LocationToolPage(driver).snackbarText());
        fixtureStage = SELECTS;
        assertNotNull(rowInTab(SELECTS, locA, EP, Duration.ofSeconds(45)), "\"" + locA + "\" never appeared in Selects › Episode No : 1");
    }

    @Test
    @Order(31)
    @DisplayName("[LOC-31] The episode listing row reads 'Location Name : <name>' with 'Scene No. 901' under it")
    void listingRowText() {
        requireFixture(SELECTS);
        LocationToolPage tool = rowInTab(SELECTS, locA, EP, Duration.ofSeconds(15));
        assumeTrue(tool != null, "Row not found");
        String label = tool.rowLabels().stream().filter(l -> l.toLowerCase().endsWith(locA.toLowerCase())).findFirst().orElse("");
        assertTrue(label.startsWith("Location Name : "), "Row label: " + label);
        String sub = tool.rowSubtitle(locA);
        assertTrue(sub != null && sub.contains(SCENE), "Row subtitle should carry the scene: " + sub);
    }

    @Test
    @Order(32)
    @DisplayName("[LOC-32] Tab search by name and by city finds the fixture's episode; the listing search finds its row")
    void searchFindsFixture() {
        requireFixture(SELECTS);
        LocationToolPage tool = openTab(SELECTS);
        tool.search(locA);
        List<String> byName = tool.rowLabels();
        tool.clearSearch();
        tool.search(CITY);
        List<String> byCity = tool.rowLabels();
        tool.clearSearch();
        assertTrue(byName.contains("Episode No : " + EP), "Search by name → " + byName);
        assertTrue(byCity.contains("Episode No : " + EP), "Search by city → " + byCity);
        assertTrue(byName.size() <= 1, "A unique name matched several episode groups: " + byName);

        LocationToolPage listing = openEpisode(SELECTS, EP);
        assumeTrue(listing != null, "Episode 1 listing did not open");
        listing.search(locA);
        List<String> rows = listing.rowLabels();
        listing.clearSearch();
        assertTrue(rows.size() == 1 && rows.get(0).toLowerCase().endsWith(locA.toLowerCase()),
                "Listing search by the unique name should leave exactly the fixture: " + rows);
    }

    @Test
    @Order(33)
    @DisplayName("[LOC-33] Group by location name / Scene no re-groups the tab; Episode No restores it")
    void groupByModes() {
        requireFixture(SELECTS);
        LocationToolPage tool = openTab(SELECTS);
        try {
            tool.openGroupByMenu();
            assertTrue(tool.tapPopupItem("Filter by location name"), "Could not pick 'Filter by location name'");
            assertTrue(tool.hasRow(locA), "Grouped by name: no 'Location Name : " + locA + "' row; rows " + tool.rowLabels());
            tool.clearSearch();

            tool.openGroupByMenu();
            assertTrue(tool.tapPopupItem("Filter by Scene no"), "Could not pick 'Filter by Scene no'");
            assertTrue(tool.labelShown("Scene No. " + SCENE) || tool.rowLabels().stream().anyMatch(l -> l.startsWith("Scene No.")),
                    "Grouped by scene: rows " + tool.rowLabels());
            tool.search(locA);
            assertTrue(tool.labelShown("Scene No. " + SCENE), "Grouped by scene + search: no 'Scene No. 901' row; " + tool.rowLabels());
            tool.clearSearch();
        } finally {
            tool.openGroupByMenu();
            tool.tapPopupItem("Filter by Episode No");
        }
        assertTrue(tool.rowLabels().stream().allMatch(l -> l.startsWith("Episode No : ")),
                "'Filter by Episode No' did not restore the episode groups: " + tool.rowLabels());
    }

    @Test
    @Order(34)
    @DisplayName("[LOC-34] Episode row menu: Move / Delete are refused with 'navigate to the next page' — nothing moves or deletes")
    void episodeRowRefusesMoveAndDelete() {
        requireFixture(SELECTS);
        LocationToolPage tool = openTab(SELECTS);
        assumeTrue(tool.rowLabels().stream().allMatch(l -> l.startsWith("Episode No : ")),
                "Not grouped by episode — refusing to touch an episode-row Delete");
        List<String> items = tool.openMenuOfLabel("Episode No : " + EP);
        assertEquals(List.of("Move To Shortlist", "Move To Final", "Delete"), items, "Episode row menu in Selects");
        tool.tapPopupItemQuick("Move To Shortlist");
        boolean moveToast = tool.toastShown("To move this folder, please navigate to the next page.", Duration.ofSeconds(4));
        System.out.println(">>> move toast seen: " + moveToast);
        assertFalse(tool.formShown(Duration.ofSeconds(2)), "Move on an episode row opened the move form");

        sleep(2500);
        tool.openMenuOfLabel("Episode No : " + EP);
        tool.tapPopupItemQuick("Delete");
        boolean deleteToast = tool.toastShown("To delete this folder, please navigate to the next page.", Duration.ofSeconds(4));
        System.out.println(">>> delete toast seen: " + deleteToast);
        if (tool.dialogShown(Duration.ofSeconds(2))) {
            tool.tapDialogButton(false);   // NO — never delete a whole episode
            throw new AssertionError("Delete on an EPISODE row asked to confirm a whole-episode delete"
                    + " (expected the 'navigate to the next page' refusal)");
        }
        // The toasts are short and UiAutomator2 does not always catch them; what must hold is
        // that neither action did anything: the episode and the fixture are still listed.
        sleep(2000);
        assertTrue(tool.labelShown("Episode No : " + EP), "'Episode No : " + EP + "' left Selects after Move/Delete on its row");
        assertNotNull(rowInTab(SELECTS, locA, EP, Duration.ofSeconds(15)), "The fixture left Selects after Move/Delete on its episode row");
    }

    @Test
    @Order(35)
    @DisplayName("[LOC-35] Selects listing row menu: Move To Shortlist, Move To Final, Delete")
    void selectsListingMenu() {
        requireFixture(SELECTS);
        LocationToolPage tool = rowInTab(SELECTS, locA, EP, Duration.ofSeconds(15));
        assumeTrue(tool != null, "Row not found");
        List<String> items = tool.openRowMenu(locA);
        tool.back();
        assertEquals(List.of("Move To Shortlist", "Move To Final", "Delete"), items, "Selects listing row menu");
    }

    @Test
    @Order(36)
    @DisplayName("[LOC-36] The location opens its entries page: one entry with name, scene, city, link and description")
    void entriesPage() {
        requireFixture(SELECTS);
        LocationEntriesPage e = entries(SELECTS, locA, EP);
        assumeTrue(e != null, "The entries page did not open");
        assertEquals("location", String.valueOf(e.toolbarHeader()).trim().toLowerCase(), "Entries page header");
        assertEquals(1, e.entryCount(), "Entries: " + e.entryTexts());
        String info = String.join(" | ", e.entryTexts()).toLowerCase();
        System.out.println(">>> entry info: " + info);
        for (String want : new String[]{locA.toLowerCase(), SCENE, CITY.toLowerCase()}) {
            assertTrue(info.contains(want), "Entry info lacks '" + want + "': " + info);
        }
        assertTrue(e.descriptions().stream().anyMatch(d -> d.contains(DESC)), "Description line: " + e.descriptions());
    }

    @Test
    @Order(37)
    @DisplayName("[LOC-37] Selects entry menu: View Details, Edit, Forward, Download, Image Reply, both moves, Read By, Delete — no Chat")
    void selectsEntryMenu() {
        requireFixture(SELECTS);
        LocationEntriesPage e = entries(SELECTS, locA, EP);
        assumeTrue(e != null, "The entries page did not open");
        List<String> items = e.openEntryMenu(0);
        e.back();
        assertEquals(List.of("View Details", "Edit", "Forward", "Download", "Image Reply",
                        "Move To Shortlist", "Move To Final", "Read By", "Delete"), items,
                "Selects entry menu (common_lcw_menu minus Chat)");
    }

    @Test
    @Order(38)
    @DisplayName("[LOC-38] View Details is read-only: values shown, fields disabled, no Submit, no Upload Media")
    void viewDetailsReadOnly() {
        requireFixture(SELECTS);
        LocationEntriesPage e = entries(SELECTS, locA, EP);
        assumeTrue(e != null, "The entries page did not open");
        assumeTrue(e.entryAction(0, "View Details"), "View Details not offered");
        try {
            assertTrue(e.formShown(Duration.ofSeconds(10)), "View Details did not open the form");
            assertEquals(CITY, String.valueOf(e.fieldText(LocationToolPage.FIELD_CITY)).trim(), "City shown");
            assertFalse(e.fieldEnabled(LocationToolPage.FIELD_NAME), "Name is editable on View Details");
            assertFalse(e.fieldEnabled(LocationToolPage.FIELD_CITY), "City is editable on View Details");
            assertFalse(e.submitShown(), "Submit is shown on View Details");
            assertFalse(e.uploadMediaShown(), "Upload Media is shown on View Details");
        } finally {
            e.back();
        }
    }

    @Test
    @Order(39)
    @DisplayName("[LOC-39] Edit: two episodes refused ('…at a time during update.'); a new description saves and shows")
    void editEntry() {
        requireFixture(SELECTS);
        LocationEntriesPage e = entries(SELECTS, locA, EP);
        assumeTrue(e != null, "The entries page did not open");
        assumeTrue(e.entryAction(0, "Edit"), "Edit not offered");
        assertTrue(e.formShown(Duration.ofSeconds(10)), "Edit did not open the form");
        assertTrue(String.valueOf(e.fieldText(LocationToolPage.FIELD_NAME)).toLowerCase().contains(locA.toLowerCase()),
                "Edit form name: " + e.fieldText(LocationToolPage.FIELD_NAME));
        e.setField(LocationToolPage.FIELD_EPISODE, "1,2");
        e.submitForm();
        boolean refused = e.messageShown("Only one episode will be allowed at a time during update.", Duration.ofSeconds(5))
                || e.fieldFocused(LocationToolPage.FIELD_EPISODE);
        assertTrue(e.formShown(Duration.ofSeconds(2)), "Edit with episodes '1,2' SAVED — expected a refusal");
        assertTrue(refused, "No 'Only one episode … during update.' refusal; snackbar=" + e.snackbarText());

        e.setField(LocationToolPage.FIELD_EPISODE, EP);
        e.setField("descInc", DESC_EDITED);
        assertTrue(e.submitForm(), "Submit not tappable");
        for (int i = 0; i < 10 && e.formShown(Duration.ofSeconds(1)); i++) sleep(1500);
        assertFalse(e.formShown(Duration.ofSeconds(1)), "The edit did not save; snackbar=" + e.snackbarText());
        LocationEntriesPage again = entries(SELECTS, locA, EP);
        assumeTrue(again != null, "Entries page did not reopen");
        long end = System.currentTimeMillis() + 20000;
        while (System.currentTimeMillis() < end && again.descriptions().stream().noneMatch(d -> d.contains(DESC_EDITED))) sleep(2000);
        assertTrue(again.descriptions().stream().anyMatch(d -> d.contains(DESC_EDITED)),
                "Edited description not shown: " + again.descriptions());
    }

    @Test
    @Order(40)
    @DisplayName("[LOC-40] Entries 'Select' mode: Delete / Move with nothing ticked are refused; Cancel leaves the mode")
    void entriesSelectMode() {
        requireFixture(SELECTS);
        LocationEntriesPage e = entries(SELECTS, locA, EP);
        assumeTrue(e != null, "The entries page did not open");
        assertTrue(e.enterSelectMode(), "Toolbar Select did not raise the selection bar");
        assertEquals("Move To Shortlist", e.buttonText("btnMoveTo"), "First move button in Selects");
        assertEquals("Move To Final", e.buttonText("btnMoveToPublish"), "Second move button in Selects");
        e.click("btnDelete");
        assertTrue(e.messageShown("Please keep at least one item from list.", Duration.ofSeconds(4)),
                "Delete with nothing ticked; snackbar=" + e.snackbarText());
        e.click("btnMoveTo");
        assertTrue(e.messageShown("Please choose at least one user from the list.", Duration.ofSeconds(4)),
                "Move with nothing ticked; snackbar=" + e.snackbarText());
        sleep(4500);   // the snackbar sits over the bar's buttons until it times out
        e.click("btnCancel");
        assertFalse(e.selectionBarShown(Duration.ofSeconds(2)), "Cancel did not close the selection bar");
    }

    @Test
    @Order(41)
    @DisplayName("[LOC-41] '+' inside the location pre-fills and locks its name and scene, and carries the episode")
    void addInsideLocationPrefills() {
        requireFixture(SELECTS);
        LocationEntriesPage e = entries(SELECTS, locA, EP);
        assumeTrue(e != null, "The entries page did not open");
        assumeTrue(e.click("takeSceneShots"), "No + on the entries page");
        try {
            assertTrue(e.formShown(Duration.ofSeconds(10)), "+ did not open the form");
            assertTrue(String.valueOf(e.fieldText(LocationToolPage.FIELD_NAME)).toLowerCase().contains(locA.toLowerCase()),
                    "Name not pre-filled: " + e.fieldText(LocationToolPage.FIELD_NAME));
            assertFalse(e.fieldEnabled(LocationToolPage.FIELD_NAME), "Pre-filled name is editable");
            assertEquals(SCENE, String.valueOf(e.fieldText(LocationToolPage.FIELD_SCENE)).trim(), "Scene not pre-filled");
            String ep = String.valueOf(e.fieldText(LocationToolPage.FIELD_EPISODE)).trim();
            assertEquals(EP, ep, "BUG: '+' inside Episode " + EP + "'s location opens with Episode no. '" + ep
                    + "' — LibraryPage forwards EPISODE_NO from the NEW Intent (always absent), not its own,"
                    + " so the entry is saved outside the episode unless the user re-types it");
        } finally {
            e.back();
        }
    }

    @Test
    @Order(42)
    @DisplayName("[LOC-42] A second entry added inside the location is listed on its entries page")
    void secondEntry() {
        requireFixture(SELECTS);
        LocationEntriesPage e = entries(SELECTS, locA, EP);
        assumeTrue(e != null, "The entries page did not open");
        int before = e.entryCount();
        assumeTrue(e.click("takeSceneShots") && e.formShown(Duration.ofSeconds(10)), "+ did not open the form");
        e.setField(LocationToolPage.FIELD_EPISODE, EP);          // see LOC-41
        assumeTrue(e.setField("descInc", "ZL_AUTO second entry"), "Description field not reachable");
        assumeTrue(e.addLinkAttachment(LINK + "/second"), "Could not add a link attachment");
        String typed = String.valueOf(e.fieldText("descInc")).trim();
        assumeTrue("ZL_AUTO second entry".equals(typed), "The description field reads '" + typed + "' before Submit");
        String refusal = submitUntilClosed(e);
        assertTrue(refusal == null, "The second entry was refused: " + refusal);
        LocationEntriesPage again = entries(SELECTS, locA, EP);
        assumeTrue(again != null, "Entries page did not reopen");
        assertTrue(again.waitEntryCount(before + 1, Duration.ofSeconds(30)),
                "Entries after adding one: " + again.entryCount() + " (was " + before + ")");
        System.out.println(">>> entries after LOC-42: " + again.entryTexts() + " / " + again.descriptions());
        long end = System.currentTimeMillis() + 30000;
        while (System.currentTimeMillis() < end && again.descriptions().stream().noneMatch(d -> d.contains("second entry"))) {
            sleep(3000);
            LocationEntriesPage re = entries(SELECTS, locA, EP);
            if (re != null) again = re;
        }
        assertTrue(again.descriptions().stream().anyMatch(d -> d.contains("ZL_AUTO second entry")),
                "The second entry is listed but without the description it was saved with: "
                        + again.entryTexts() + " / " + again.descriptions());
    }

    @Test
    @Order(43)
    @DisplayName("[LOC-43] Entry Delete asks 'Are you sure you want to delete?': No keeps it, Yes removes only that entry")
    void deleteOneEntry() {
        requireFixture(SELECTS);
        LocationEntriesPage e = entries(SELECTS, locA, EP);
        assumeTrue(e != null, "The entries page did not open");
        int before = e.entryTexts().size();
        int idx = -1;
        List<String> infos = e.entryTexts();
        for (int i = 0; i < infos.size(); i++) if (infos.get(i).contains("/second")) idx = i;
        assumeTrue(idx >= 0, "The second entry (LOC-42) is not listed: " + infos);
        assertTrue(e.entryActionFor("/second", "Delete"), "Delete not offered on the second entry");
        assertTrue(e.dialogShown(Duration.ofSeconds(5)), "No delete confirmation");
        assertTrue(String.valueOf(e.dialogMessage()).contains("Are you sure you want to delete?"), "Dialog: " + e.dialogMessage());
        e.tapDialogButton(false);
        assertTrue(String.join(" ", e.entryTexts()).contains("/second"), "'No' still deleted the entry");
        assertTrue(e.entryActionFor("/second", "Delete") && e.dialogShown(Duration.ofSeconds(5)), "Delete not offered the second time");
        e.tapDialogButton(true);
        long end = System.currentTimeMillis() + 30000;
        while (System.currentTimeMillis() < end && String.join(" ", e.entryTexts()).contains("/second")) sleep(2000);
        assertFalse(String.join(" ", e.entryTexts()).contains("/second"), "The second entry is still listed: " + e.entryTexts());
        assertTrue(String.join(" ", e.entryTexts()).toLowerCase().contains(locA.toLowerCase()), "The wrong entry went");
    }

    /**
     * Submits; when the form is still up a few seconds later, notes any snackbar (they time
     * out quickly) and taps Submit once more — the first tap can go to the IME closing.
     *
     * @return null once the form closed, else what the form said
     */
    private String submitUntilClosed(LocationToolPage tool) {
        StringBuilder seen = new StringBuilder();
        for (int attempt = 0; attempt < 2; attempt++) {
            tool.submitForm();
            for (int i = 0; i < 12; i++) {
                String sn = tool.snackbarText();
                if (sn != null && seen.indexOf(sn) < 0) seen.append("[").append(sn).append("] ");
                if (!tool.formShown(Duration.ofSeconds(1))) return null;
                sleep(1000);
            }
        }
        return "form still open after two Submits; snackbars " + seen;
    }

    // ── moves ──

    @Test
    @Order(44)
    @DisplayName("[LOC-44] Move form: no Location block, name locked; '1,2' and empty episode refused; Submit lands it in Shortlist")
    void moveToShortlist() {
        requireFixture(SELECTS);
        LocationToolPage tool = rowInTab(SELECTS, locA, EP, Duration.ofSeconds(15));
        assumeTrue(tool != null, "Row not found");
        assumeTrue(tool.rowMenu(locA, "Move To Shortlist"), "'Move To Shortlist' not offered");
        if (!tool.formShown(Duration.ofSeconds(6))) tool.confirmDialog(Duration.ofSeconds(3));
        assertTrue(tool.formShown(Duration.ofSeconds(10)), "The move form did not open");
        assertFalse(tool.locationSectionShown(), "A move form shows the Location block (it must keep the stored pin)");
        assertFalse(tool.fieldEnabled(LocationToolPage.FIELD_NAME), "Name editable on a move");
        assertEquals(EP, String.valueOf(tool.fieldText(LocationToolPage.FIELD_EPISODE)).trim(), "Episode on the move form");
        assertEquals(SCENE, String.valueOf(tool.fieldText(LocationToolPage.FIELD_SCENE)).trim(), "Scene on the move form");

        tool.setField(LocationToolPage.FIELD_EPISODE, "");
        expectMoveRefusal(tool, "Episode no. cannot be empty.");
        tool.setField(LocationToolPage.FIELD_EPISODE, "1,2");
        expectMoveRefusal(tool, "Only one episode will be allowed at a time during move.");

        tool.setField(LocationToolPage.FIELD_EPISODE, EP);
        assertTrue(tool.submitForm(), "Submit not tappable");
        for (int i = 0; i < 10 && tool.formShown(Duration.ofSeconds(1)); i++) sleep(1500);
        assertFalse(tool.formShown(Duration.ofSeconds(1)), "The move was refused; snackbar=" + tool.snackbarText());
        assertNotNull(rowInTab(SHORTLIST, locA, EP, Duration.ofSeconds(45)), "\"" + locA + "\" did not arrive in Shortlist");
        fixtureStage = SHORTLIST;
    }

    private void expectMoveRefusal(LocationToolPage tool, String msg) {
        assumeTrue(tool.formShown(Duration.ofSeconds(3)), "The move form was lost before Submit (a stray BACK?)");
        tool.submitForm();
        boolean shown = tool.messageShown(msg, Duration.ofSeconds(5)) || tool.fieldFocused(LocationToolPage.FIELD_EPISODE);
        assertTrue(tool.formShown(Duration.ofSeconds(2)), "The move SAVED instead of refusing with '" + msg + "'");
        assertTrue(shown, "No '" + msg + "'; snackbar=" + tool.snackbarText());
    }

    @Test
    @Order(45)
    @DisplayName("[LOC-45] Shortlist: listing menu is Move To Final + Delete; the entry menu drops Move To Shortlist")
    void shortlistMenus() {
        requireFixture(SHORTLIST);
        LocationToolPage tool = rowInTab(SHORTLIST, locA, EP, Duration.ofSeconds(15));
        assumeTrue(tool != null, "Row not found");
        List<String> items = tool.openRowMenu(locA);
        tool.back();
        assertEquals(List.of("Move To Final", "Delete"), items, "Shortlist listing row menu");
        LocationEntriesPage e = entries(SHORTLIST, locA, EP);
        assumeTrue(e != null, "Entries page did not open");
        List<String> entry = e.openEntryMenu(0);
        e.back();
        assertFalse(entry.contains("Move To Shortlist"), "Shortlist entry still offers Move To Shortlist: " + entry);
        assertTrue(entry.contains("Move To Final") && entry.contains("Edit"), "Shortlist entry menu: " + entry);
    }

    @Test
    @Order(46)
    @DisplayName("[LOC-46] Move To Final from Shortlist lands the location in Final")
    void moveToFinal() {
        requireFixture(SHORTLIST);
        LocationToolPage tool = rowInTab(SHORTLIST, locA, EP, Duration.ofSeconds(15));
        assumeTrue(tool != null, "Row not found");
        assumeTrue(tool.rowMenu(locA, "Move To Final"), "'Move To Final' not offered");
        if (!tool.formShown(Duration.ofSeconds(6))) tool.confirmDialog(Duration.ofSeconds(3));
        assertTrue(tool.formShown(Duration.ofSeconds(10)), "The move form did not open");
        String ep = String.valueOf(tool.fieldText(LocationToolPage.FIELD_EPISODE)).trim();
        if (!ep.equals(EP) && tool.fieldEnabled(LocationToolPage.FIELD_EPISODE)) tool.setField(LocationToolPage.FIELD_EPISODE, EP);
        assertTrue(tool.submitForm(), "Submit not tappable");
        for (int i = 0; i < 10 && tool.formShown(Duration.ofSeconds(1)); i++) sleep(1500);
        assertFalse(tool.formShown(Duration.ofSeconds(1)), "The move was refused; snackbar=" + tool.snackbarText());
        assertNotNull(rowInTab(FINAL, locA, EP, Duration.ofSeconds(45)), "\"" + locA + "\" did not arrive in Final");
        fixtureStage = FINAL;
    }

    @Test
    @Order(47)
    @DisplayName("[LOC-47] Final: listing menu is Generate Full PDF + Delete; no + on its entries; entry menu has no Edit / moves")
    void finalMenus() {
        requireFixture(FINAL);
        LocationToolPage tool = rowInTab(FINAL, locA, EP, Duration.ofSeconds(15));
        assumeTrue(tool != null, "Row not found");
        List<String> items = tool.openRowMenu(locA);
        tool.back();
        assertEquals(List.of("Generate Full PDF", "Delete"), items, "Final listing row menu");
        LocationEntriesPage e = entries(FINAL, locA, EP);
        assumeTrue(e != null, "Entries page did not open");
        List<String> entry = e.openEntryMenu(0);
        e.back();
        assertEquals(List.of("View Details", "Forward", "Download", "Read By", "Delete"), entry, "Final entry menu");
    }

    @Test
    @Order(48)
    @DisplayName("[LOC-48] Generate Full PDF: choose with/without details, then 'Report Generated Successfully.' — closed without sharing")
    void generateFullPdf() {
        requireFixture(FINAL);
        LocationToolPage tool = rowInTab(FINAL, locA, EP, Duration.ofSeconds(15));
        assumeTrue(tool != null, "Row not found");
        assumeTrue(tool.rowMenu(locA, "Generate Full PDF"), "'Generate Full PDF' not offered");
        assertTrue(tool.dialogShown(Duration.ofSeconds(6)), "No PDF choice dialog");
        assertEquals("Generation of Location list.", String.valueOf(tool.dialogMessage()).trim(), "Dialog message");
        assertEquals("Publish without details", tool.dialogButton(false), "Left button");
        assertEquals("Publish with details", tool.dialogButton(true), "Right button");
        tool.tapDialogButton(false);
        boolean done = tool.textShown("Report Generated Successfully", Duration.ofSeconds(90));
        try {
            assertTrue(done, "The PDF report dialog never appeared");
            assertTrue(tool.idShown("viewBtn", Duration.ofSeconds(2)), "Report dialog has no View button");
        } finally {
            tool.tapId("closeTv");
        }
        assertFalse(tool.idShown("closeTv", Duration.ofSeconds(2)), "Close did not dismiss the report dialog");
    }

    @Test
    @Order(49)
    @DisplayName("[LOC-49] Listing Delete: 'No' keeps the location, 'Yes' removes it from Final")
    void deleteFromFinal() {
        requireFixture(FINAL);
        LocationToolPage tool = rowInTab(FINAL, locA, EP, Duration.ofSeconds(15));
        assumeTrue(tool != null, "Row not found");
        assumeTrue(tool.rowMenu(locA, "Delete"), "'Delete' not offered");
        assertTrue(tool.dialogShown(Duration.ofSeconds(6)), "No delete confirmation");
        assertTrue(String.valueOf(tool.dialogMessage()).contains("Are you sure you want to delete?"), "Dialog: " + tool.dialogMessage());
        tool.tapDialogButton(false);
        assertTrue(tool.rowShown(locA) || tool.hasRow(locA), "'No' still removed the row");
        assumeTrue(tool.rowMenu(locA, "Delete") && tool.dialogShown(Duration.ofSeconds(6)), "Delete not offered again");
        tool.tapDialogButton(true);
        sleep(3000);
        assertTrue(tool.waitForRowGone(locA, Duration.ofSeconds(30)), "\"" + locA + "\" is still in Final after Yes");
        fixtureStage = SHORTLIST;   // copies may remain in earlier tabs; LOC-99 removes them
    }

    @Test
    @Order(50)
    @DisplayName("[LOC-50] A location saved with a new episode number opens its own 'Episode No : N' group, which goes with it")
    void newEpisodeGroup() {
        assertTrue(createLocation(SELECTS, locB, "902", NEW_EPISODE, CITY, ADDRESS, null),
                "The form refused a complete location; snackbar=" + new LocationToolPage(driver).snackbarText());
        LocationToolPage tool = rowInTab(SELECTS, locB, NEW_EPISODE, Duration.ofSeconds(45));
        assertNotNull(tool, "No 'Episode No : " + NEW_EPISODE + "' group listing \"" + locB + "\"");
        LocationToolPage root = openTab(SELECTS);
        root.search(NEW_EPISODE);
        boolean groupThere = root.labelShown("Episode No : " + NEW_EPISODE);
        root.clearSearch();
        assertTrue(groupThere, "Searching '" + NEW_EPISODE + "' does not show its episode group: " + root.rowLabels());
        assertTrue(deleteEverywhere(locB, Set.of(NEW_EPISODE)), "\"" + locB + "\" could not be deleted");
        created.remove(locB);
        root = openTab(SELECTS);
        root.search(NEW_EPISODE);
        sleep(1500);
        boolean stillThere = root.labelShown("Episode No : " + NEW_EPISODE);
        List<String> rows = root.rowLabels();
        root.clearSearch();
        assertFalse(stillThere, "'Episode No : " + NEW_EPISODE + "' is still listed with nothing in it: " + rows);
    }

    @Test
    @Order(99)
    @DisplayName("[LOC-99] Cleanup: the ZL_AUTO location is gone from every tab")
    void cleanup() {
        assumeTrue(!created.isEmpty(), "Nothing was created");
        List<String> names = new java.util.ArrayList<>(created.keySet());
        for (String name : names) {
            assertTrue(deleteEverywhere(name, created.get(name)), "\"" + name + "\" is still listed somewhere");
            created.remove(name);
        }
        fixtureStage = null;
        // Whatever episode a copy ended up under, a tab search by name finds its group.
        for (String name : names) {
            for (String tab : LocationToolPage.TABS_IN_ORDER) {
                LocationToolPage tool = openTab(tab);
                tool.search(name);
                List<String> left = tool.rowLabels();
                tool.clearSearch();
                assertTrue(left.isEmpty(), "\"" + name + "\" is still filed in " + tab + " under " + left
                        + " — delete it by hand (it is a ZL_AUTO test location)");
            }
        }
    }
}
