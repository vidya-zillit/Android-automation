package com.zillit.automation.tests.map;

import com.zillit.automation.core.TestOrder;

import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.pages.map.MapDialogPage;
import com.zillit.automation.pages.map.MapLocationFormPage;
import com.zillit.automation.pages.map.MapLocationListPage;
import com.zillit.automation.pages.map.MapPage;
import com.zillit.automation.pages.map.MapTypeManagerPage;
import io.appium.java_client.AppiumBy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * MAP — cases the web E2E report has and the Android suites lacked (gap file
 * {@code web-e2e/gaps/map.md}, groups M1, M5, M10, M11, M14).
 *
 * <p>One ordered story on the selected city, with its own fixtures:
 * <pre>
 *   typeDoomed  ZL_AUTO type with one subtype     (M1: deleted while a location uses it)
 *   locA        pinned, typed typeDoomed
 *   locB        pinned, no type                   ("Unknown" chip, detail sheet)
 *   locC        pinned, typed typeSolo — a type created inline from the location form
 * </pre>
 *
 * <p><b>Data safety.</b> Every fixture is named {@code ZL_AUTO …} and recorded when it is
 * created. Cleanup deletes only names in those records — never by prefix, never anything a
 * person or another suite made (the project's real cities and locations, e.g. Mountain
 * View, are read but never edited or deleted).
 *
 * <p>Source checked: {@code Zillit-Android-devbuild} (matches Zillit_32539-dev). Two things
 * the gap file assumed have changed there: the map's filter rows now count with
 * {@code getTypeCount} (so F-16 is expected to pass), and the List View now also lists the
 * city's Shortlisted/Final Location-tool pins (rows without Edit/Delete), which is why this
 * suite pairs row buttons to rows by position, not by index.
 */
@ExtendWith(ReasonLogger.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Order(TestOrder.MAP_WEB_GAP)
class MapWebGapTest extends MapBaseTest {

    static final String GAP_PREFIX = "ZL_AUTO";

    private final String stamp = String.valueOf(System.currentTimeMillis() % 100000);
    private final String typeDoomed = GAP_PREFIX + " TypeDoomed " + stamp;
    private final String typeSolo = GAP_PREFIX + " Solo " + stamp;
    private final String subName = GAP_PREFIX + " Rooftop " + stamp;

    private String locA = GAP_PREFIX + " LocA " + stamp;
    private String locB = GAP_PREFIX + " LocB " + stamp;
    private String locC = GAP_PREFIX + " LocC " + stamp;

    /** Exact names of every location / type this run created and has not deleted yet. */
    private final List<String> myLocations = new ArrayList<>();
    private final List<String> myTypes = new ArrayList<>();

    private boolean fixturesReady() {
        return myLocations.contains(locA) && myLocations.contains(locB) && myLocations.contains(locC);
    }

    // ───────────────────────────── fixtures ─────────────────────────────

    @Test
    @Order(1)
    @DisplayName("WG-00 Fixtures: a ZL_AUTO type with a subtype, and three pinned ZL_AUTO locations")
    void fixtures() {
        assumeTrue(ensureCitySelected(), "No city on this project to pin into");
        assumeTrue(viaToolbar(MapPage::openTypes), "Types button not available — needs a city AND posting rights");
        boolean typeMade = createTypeWithSubtype(typeDoomed, subName);
        if (typeMade) myTypes.add(typeDoomed);
        backToMap();
        assertTrue(typeMade, "Could not create the fixture type \"" + typeDoomed + "\" with subtype \""
                + subName + "\": " + lastGapFailure);

        double[][] pts = {{0.46, 0.53}, {0.55, 0.47}, {0.5, 0.6}};
        String[] names = {locA, locB, locC};
        String[] types = {typeDoomed, null, typeSolo};
        List<String> failed = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            ensureToolbarReady();
            boolean ok = createLocationByPin(pts[i][0], pts[i][1], names[i], types[i]);
            if (!ok) {
                String first = lastPinFailure;
                backToMap();
                ensureToolbarReady();
                sleep(1500);
                // Retry somewhere else: the commit tap aims at the info window, which an
                // existing marker or the bottom panel can cover at one spot.
                double[][] alt = {{0.38, 0.40}, {0.62, 0.62}};
                for (int k = 0; k < alt.length && !ok; k++) {
                    ok = createLocationByPin(alt[k][0], alt[k][1], names[i], types[i]);
                    if (!ok) { backToMap(); ensureToolbarReady(); sleep(1200); }
                }
                if (!ok) failed.add(names[i] + ": " + first + " | retries: " + lastPinFailure);
            }
            if (ok) {
                myLocations.add(names[i]);
                if (types[i] != null && !myTypes.contains(types[i])) myTypes.add(types[i]);
            }
        }
        assertTrue(failed.isEmpty(), "Pinned fixtures were not created: " + failed);
    }

    // ───────────────────────── M10 filters and counts ─────────────────────────

    @Test
    @Order(2)
    @DisplayName("F-16 Filter rows count each type, 'Unknown' included, and match the List View chips")
    void filterChipCountsMatchList() { // [WEB map.md:413]
        assumeTrue(fixturesReady(), "WG-00 did not create the three fixtures");
        ensureToolbarReady();
        MapPage m = map();
        assumeTrue(m.openFilterPanel(), "Filter button not available: " + m.toolbarSnapshot());
        assumeTrue(m.filterPanelShown(Duration.ofSeconds(8)), "Filter dialog did not open");
        Map<String, Integer> mapCounts = m.filterChipCounts();
        m.closeFilterDialog();
        sleep(800);
        System.out.println(">>> F-16 map filter rows: " + mapCounts);

        assertEquals(Integer.valueOf(1), mapCounts.get(typeDoomed),
                "The filter row for \"" + typeDoomed + "\" (one location) reads " + mapCounts.get(typeDoomed)
                        + ". Rows: " + mapCounts);
        assertEquals(Integer.valueOf(1), mapCounts.get(typeSolo),
                "The filter row for \"" + typeSolo + "\" (one location) reads " + mapCounts.get(typeSolo));
        Integer unknown = mapCounts.get("Unknown");
        assertNotNull(unknown, "No 'Unknown' filter row although \"" + locB + "\" has no type. Rows: " + mapCounts);
        assertTrue(unknown >= 1, "The 'Unknown' filter row reads " + unknown + " although \"" + locB
                + "\" has no type — the exact-match count bug (gap M10, MapFragment chip count)");

        MapLocationListPage list = openList();
        List<String> mismatches = new ArrayList<>();
        for (Map.Entry<String, Integer> e : mapCounts.entrySet()) {
            Integer listCount = list.chipCount(e.getKey());
            if (!e.getValue().equals(listCount)) {
                mismatches.add(e.getKey() + ": map " + e.getValue() + " vs list " + listCount);
            }
        }
        List<String> chips = list.allChipTexts();
        list.backToMap();
        backToMap();
        assertTrue(mismatches.isEmpty(), "Map filter counts disagree with the List View chips: "
                + mismatches + ". List chips: " + chips);
    }

    @Test
    @Order(3)
    @DisplayName("F-16b Applying one type reads 'Filter (1)' and narrows the count; Reset restores both")
    void applyAndResetFilter() { // [WEB map.md:413]
        assumeTrue(fixturesReady(), "WG-00 did not create the three fixtures");
        ensureToolbarReady();
        MapPage m = map();
        int before = m.locationCount();
        assumeTrue(before > 1, "Need more than one location to see a filter narrow anything");

        assumeTrue(m.openFilterPanel() && m.filterPanelShown(Duration.ofSeconds(8)), "Filter dialog did not open");
        assumeTrue(m.revealFilterRow(typeSolo), "No filter row for " + typeSolo + ": " + m.filterTypeLabels());
        assertTrue(m.toggleTypeFilter(typeSolo), "Could not tap the " + typeSolo + " row");
        assertEquals("Apply (1)", m.filterApplyLabel(), "Apply did not count the pending selection");
        assertTrue(m.applyFilterDialog(), "Apply not tappable");
        sleep(1500);

        String label = m.filterButtonLabel();
        int narrowed = m.locationCount();
        assertEquals("Filter (1)", label, "Toolbar filter button after applying one type");
        assertEquals(1, narrowed, "Header count with only \"" + typeSolo + "\" applied (one location of that type)");

        assumeTrue(m.openFilterPanel() && m.filterPanelShown(Duration.ofSeconds(8)), "Filter dialog did not reopen");
        assertTrue(m.resetFilterDialog(), "Reset not tappable");
        sleep(1500);
        assertEquals("Filter", m.filterButtonLabel(), "Toolbar filter button after Reset");
        assertEquals(before, m.locationCount(), "Header count after Reset");
    }

    @Test
    @Order(4)
    @DisplayName("V-06 The header count equals the List View's 'N total'")
    void headerCountEqualsList() { // [WEB map.md:113, map.md:395]
        assumeTrue(fixturesReady(), "WG-00 did not create the three fixtures");
        ensureToolbarReady();
        MapPage m = map();
        assumeTrue("Filter".equals(m.filterButtonLabel()), "A type filter is active; the header would be narrowed");
        int header = m.locationCount();
        MapLocationListPage list = openList();
        int total = list.totalCount();
        list.backToMap();
        backToMap();
        assertEquals(header, total, "Map header reads " + header + " locations, List View reads " + total + " total");
    }

    @Test
    @Order(5)
    @DisplayName("M-10 An applied type filter is cleared after leaving and re-entering the Map")
    void filterResetOnReopen() { // [WEB map.md:1109]
        assumeTrue(fixturesReady(), "WG-00 did not create the three fixtures");
        ensureToolbarReady();
        MapPage m = map();
        String city = m.cityName();
        assumeTrue(m.openFilterPanel() && m.filterPanelShown(Duration.ofSeconds(8)), "Filter dialog did not open");
        assumeTrue(m.revealFilterRow(typeSolo) && m.toggleTypeFilter(typeSolo), "Could not pick " + typeSolo);
        m.applyFilterDialog();
        sleep(1200);
        assumeTrue("Filter (1)".equals(m.filterButtonLabel()), "The filter did not apply: " + m.filterButtonLabel());

        leaveMapToTools();
        openMap();
        MapPage again = map();
        assertEquals(city, again.cityName(), "Re-entering the Map changed the city");
        // null = the Filter chip could not be brought on screen (toolbar strip), not a state.
        String label = again.filterButtonLabel();
        for (int i = 0; i < 2 && label == null; i++) {
            leaveMapToTools();
            openMap();
            label = map().filterButtonLabel();
        }
        assumeTrue(label != null, "The Filter chip could not be scrolled into view to read its label");
        assertEquals("Filter", label, "The type filter survived leaving and re-entering the Map");
    }

    @Test
    @Order(6)
    @DisplayName("C-22c Switching city clears an applied type filter")
    void switchCityClearsFilter() { // [WEB map.md:307]
        assumeTrue(fixturesReady(), "WG-00 did not create the three fixtures");
        ensureToolbarReady();
        MapPage m = map();
        String home = m.cityName();
        assumeTrue(m.openCityPanel(), "City panel did not open");
        List<String> cities = m.visibleCityNames();
        m.closeCityPanel();
        String other = cities.stream().filter(c -> !c.equals(home)).findFirst().orElse(null);
        assumeTrue(other != null, "Only one city on the project, so there is no city to switch to: " + cities);

        assumeTrue(m.openFilterPanel() && m.filterPanelShown(Duration.ofSeconds(8)), "Filter dialog did not open");
        assumeTrue(m.revealFilterRow(typeSolo) && m.toggleTypeFilter(typeSolo), "Could not pick " + typeSolo);
        m.applyFilterDialog();
        sleep(1200);
        assumeTrue("Filter (1)".equals(m.filterButtonLabel()), "The filter did not apply");

        String afterSwitch;
        try {
            assumeTrue(m.openCityPanel() && m.selectCity(other), "Could not switch to " + other);
            sleep(3000);
            afterSwitch = m.filterButtonLabel();
        } finally {
            if (m.openCityPanel()) { m.selectCity(home); sleep(3000); }
        }
        assertEquals("Filter", afterSwitch, "Switching city kept the type filter of the previous city");
    }

    // ─────────────────────────── types ───────────────────────────

    @Test
    @Order(7)
    @DisplayName("T-10b Searching a subtype name lists its parent type")
    void typeSearchMatchesSubtype() { // [WEB map.md:389]
        assumeTrue(myTypes.contains(typeDoomed), "WG-00 did not create the type");
        ensureToolbarReady();
        assumeTrue(viaToolbar(MapPage::openTypes), "Types button not available");
        MapTypeManagerPage types = new MapTypeManagerPage(driver);
        assumeTrue(types.isLoaded(Duration.ofSeconds(12)), "Type manager did not open");
        assumeTrue(types.search(subName), "Type search field not available");
        sleep(1200);
        List<String> shown = types.typeNames();
        backToMap();
        assertTrue(shown.contains(typeDoomed),
                "Searching the subtype \"" + subName + "\" did not list its type \"" + typeDoomed + "\": " + shown);
    }

    @Test
    @Order(8)
    @DisplayName("H-07 A padded type name is saved trimmed")
    void paddedTypeNameTrimmed() { // [WEB map.md:1182]
        String trimmed = GAP_PREFIX + " Pad " + stamp;
        ensureToolbarReady();
        assumeTrue(viaToolbar(MapPage::openTypes), "Types button not available");
        boolean made = createTypeWithSubtype("   " + trimmed + "   ", null);
        MapTypeManagerPage types = new MapTypeManagerPage(driver);
        types.search(trimmed);
        sleep(1200);
        List<String> raw = rawTexts("tvName");
        if (raw.stream().anyMatch(t -> t.trim().equals(trimmed))) {
            myTypes.add(raw.stream().filter(t -> t.trim().equals(trimmed)).findFirst().get());
        }
        backToMap();
        assumeTrue(made || !raw.isEmpty(), "The padded type was not created: " + lastGapFailure);
        assertTrue(raw.contains(trimmed),
                "The type list does not show \"" + trimmed + "\" exactly; raw row texts: " + quoteAll(raw));
    }

    // ─────────────────────── M5 saved-input hygiene ───────────────────────

    @Test
    @Order(9)
    @DisplayName("H-01 Padded name and scene are saved trimmed")
    void paddedNameAndSceneTrimmed() { // [WEB map.md:1139, map.md:1169]
        // The Scene Number field only exists for a location typed "Shooting" in this build
        // (fragment_location_form.xml: sceneLayout gone; LocationFormFragment shows it when
        // locationType == "Shooting") — the same rule as web's "shoot" type. So this case
        // pins its own Shooting-typed fixture rather than touching the shared ones.
        String locD = GAP_PREFIX + " LocD " + stamp;
        ensureToolbarReady();
        boolean made = createLocationByPin(0.44, 0.44, locD, "Shooting")
                || createLocationByPin(0.6, 0.4, locD, "Shooting");
        assumeTrue(made, "Could not pin the Shooting fixture: " + lastPinFailure);
        myLocations.add(locD);
        String trimmed = GAP_PREFIX + " Trim " + stamp;
        MapLocationFormPage form = editRow(locD);
        assumeTrue(form.sceneFieldShown(), "No Scene field even on a \"Shooting\" location — the type did not stick");
        assumeTrue(form.setScene("  12B  "), "Scene field not available");
        assumeTrue(form.setName("   " + trimmed + "   "), "Could not type the name");
        assumeTrue(form.save(), "Save not available");
        sleep(4000);
        assertFalse(form.stillOpen(), "The edit with a padded name was rejected");
        rename(locD, trimmed);

        MapLocationListPage list = openList();
        String rawName = rawRowName(list, trimmed);
        String scene = list.rowChildText(trimmed, "tvScene");
        list.backToMap();
        // Where did the scene go? Reopen the form: it reloads the stored record.
        MapLocationFormPage again = editRow(trimmed);
        String storedScene = again.sceneFieldShown() ? again.sceneValue() : "(no scene field)";
        again.cancel();
        sleep(1000);
        backToMap();
        assertEquals(trimmed, rawName, "The saved name kept its padding");
        assertEquals("Scene #12B", scene, "The List View card shows scene \"" + scene + "\"; the reopened form"
                + " holds \"" + storedScene + "\" (typed \"  12B  \"). Web bug map.md:1169 is the padding; an"
                + " empty value means the scene was not stored at all.");
    }

    @Test
    @Order(10)
    @DisplayName("H-02 A spaces-only name is refused and the form stays open")
    void spacesOnlyNameRefused() { // [WEB map.md:828]
        assumeTrue(myLocations.contains(locB), "No fixture to edit");
        MapLocationFormPage form = editRow(locB);
        assumeTrue(form.setName("     "), "Could not type the name");
        assumeTrue(form.save(), "Save not available");
        sleep(2500);
        boolean open = form.stillOpen();
        if (open) form.cancel();
        sleep(1200);
        MapLocationListPage list = openList();
        boolean kept = list.revealRow(locB);
        list.backToMap();
        backToMap();
        assertTrue(open, "A spaces-only name was accepted — expected \"" + MapLocationFormPage.ERR_NO_NAME + "\"");
        assertTrue(kept, "\"" + locB + "\" lost its name after the refused edit");
    }

    @Test
    @Order(11)
    @DisplayName("H-03 A unicode, emoji and RTL name saves, and lower-case search finds it")
    void unicodeNameSavesAndIsFound() { // [WEB map.md:1145, map.md:883]
        assumeTrue(myLocations.contains(locC), "WG-00 did not create " + locC);
        String uni = GAP_PREFIX + " Ünïcødé 🎬 שלום " + stamp;
        MapLocationFormPage form = editRow(locC);
        assumeTrue(form.setName(uni), "Could not type the name");
        assumeTrue(uni.equals(String.valueOf(form.nameValue()).trim()),
                "The driver could not type the unicode name (field reads \"" + form.nameValue()
                        + "\"), so the app's handling cannot be judged here");
        assumeTrue(form.save(), "Save not available");
        sleep(4000);
        assertFalse(form.stillOpen(), "A unicode/emoji/RTL name was refused");
        rename(locC, uni);
        locC = uni;

        MapLocationListPage list = openList();
        boolean listed = list.revealRow(uni);
        List<String> names = listed ? List.of() : list.allNames();
        list.backToMap();
        backToMap();
        assertTrue(listed, "The unicode name is not listed exactly as typed: " + names);

        MapPage m = map();
        assumeTrue(m.openInScreenSearch(), "In-screen search did not open");
        m.typeSearch("ünïcødé");
        sleep(1500);
        List<String> hits = m.searchResultNames();
        m.closeInScreenSearch();
        sleep(1800);
        ensureToolbarReady();
        assertTrue(hits.contains(uni), "Lower-case search \"ünïcødé\" did not find \"" + uni + "\": " + hits);
    }

    @Test
    @Order(12)
    @DisplayName("H-04 HTML-looking name and description are shown as literal text")
    void htmlLookingTextIsLiteral() { // [WEB map.md:1151, map.md:895]
        assumeTrue(myLocations.contains(locA), "WG-00 did not create " + locA);
        String html = GAP_PREFIX + " <b>Bold</b> " + stamp;
        String desc = "<script>alert(1)</script> & <i>x</i>";
        MapLocationFormPage form = editRow(locA);
        assumeTrue(form.setName(html), "Could not type the name");
        assumeTrue(form.setDescription(desc), "Description field not available");
        assumeTrue(form.save(), "Save not available");
        sleep(4000);
        assertFalse(form.stillOpen(), "An HTML-looking name/description was refused");
        rename(locA, html);
        locA = html;

        MapLocationListPage list = openList();
        boolean listed = list.revealRow(html);
        assertTrue(listed, "The name is not listed literally as \"" + html + "\": " + list.allNames());
        assumeTrue(list.tapRowActionByBand(html, "btnView"), "View not offered on the row");
        sleep(1500);
        boolean descShown = !driver.findElements(AppiumBy.xpath("//*[@text=" + xpathLiteral(desc) + "]")).isEmpty();
        driver.navigate().back();
        sleep(1000);
        list.backToMap();
        backToMap();
        assertTrue(descShown, "The detail sheet does not show the description literally: \"" + desc + "\"");
    }

    @Test
    @Order(13)
    @DisplayName("H-08 Searching regex and special characters never crashes the Map")
    void specialCharacterSearchIsSafe() { // [WEB map.md:883]
        mapHere();
        MapPage m = map();
        assumeTrue(m.openInScreenSearch(), "In-screen search did not open");
        m.typeSearch(".* ( [ \\ % $^");
        sleep(2000);
        boolean alive = m.searchBarShown(Duration.ofSeconds(3));
        String pkg = String.valueOf(driver.getCurrentPackage());
        m.closeInScreenSearch();
        sleep(1800);
        ensureToolbarReady();
        assertTrue(pkg.contains("zillit"), "The app left the foreground after a special-character search: " + pkg);
        assertTrue(alive, "The search bar vanished after a special-character query");
        assertTrue(map().isLoaded(Duration.ofSeconds(5)), "The Map is gone after a special-character search");
    }

    // ───────────────────── M11 / M14 form and detail ─────────────────────

    @Test
    @Order(14)
    @DisplayName("N-07 Picking a type with subtypes shows them as checkboxes; another type replaces them")
    void typePickDrivesSubtypes() { // [WEB map.md:357]
        assumeTrue(myLocations.contains(locB) && myTypes.contains(typeDoomed) && myTypes.contains(typeSolo),
                "Fixtures missing");
        MapLocationFormPage form = editRow(locB);
        try {
            assumeTrue(form.pickTypeFromDropdown(typeDoomed), "Could not pick " + typeDoomed + " from the dropdown");
            List<String> subs = form.subtypeLabels();
            assertTrue(subs.contains(subName), "Picking \"" + typeDoomed + "\" did not show its subtype \""
                    + subName + "\": " + subs);
            assumeTrue(form.pickTypeFromDropdown(typeSolo), "Could not pick " + typeSolo);
            List<String> after = form.subtypeLabels();
            assertFalse(after.contains(subName), "Switching to \"" + typeSolo + "\" kept the previous type's subtype: " + after);
        } finally {
            form.cancel();   // nothing saved: locB stays untyped for N-15b
            sleep(1200);
            backToMap();
        }
    }

    @Test
    @Order(15)
    @DisplayName("N-15b An untyped location's detail sheet reads 'Unknown' and has no Description or Photos")
    void untypedDetailShowsOnlyWhatItHas() { // [WEB map.md:1032]
        assumeTrue(myLocations.contains(locB), "No untyped fixture");
        MapLocationListPage list = openList();
        assumeTrue(list.tapRowActionByBand(locB, "btnView"), "View not offered on " + locB);
        sleep(1500);
        // The sheet is built in code (no ids). "Coordinates" is always added, so it proves
        // the sheet is the thing on screen; the badge reads "<icon> Unknown" ("Z Unknown").
        boolean sheet = !driver.findElements(AppiumBy.xpath("//*[@text='Coordinates']")).isEmpty();
        List<String> onSheet = new ArrayList<>();
        for (WebElement e : driver.findElements(AppiumBy.xpath("//*[string-length(@text)>0]"))) {
            try { onSheet.add(e.getText()); } catch (Exception ignored) { }
        }
        boolean unknown = onSheet.stream().anyMatch(t -> t.trim().endsWith(" Unknown") || t.trim().equals("Unknown"));
        boolean desc = !driver.findElements(AppiumBy.xpath("//*[@text='Description']")).isEmpty();
        boolean photos = !driver.findElements(AppiumBy.xpath("//*[@text='Photos']")).isEmpty();
        driver.navigate().back();
        sleep(1000);
        list.backToMap();
        backToMap();
        assumeTrue(sheet, "View did not open the detail sheet (no 'Coordinates' row). On screen: " + onSheet);
        assertTrue(unknown, "The detail sheet of an untyped location does not read 'Unknown'. On screen: " + onSheet);
        assertFalse(desc, "The detail sheet shows a Description section for a location with none");
        assertFalse(photos, "The detail sheet shows a Photos section for a location with none");
    }

    @Test
    @Order(16)
    @DisplayName("N-11b A double tap on Update Location saves once and leaves one row")
    void doubleTapUpdateSavesOnce() { // [WEB map.md:859]
        assumeTrue(myLocations.contains(locB), "No fixture to edit");
        String desc = GAP_PREFIX + " desc " + stamp;
        MapLocationFormPage form = editRow(locB);
        assumeTrue(form.setDescription(desc), "Description field not available");
        assumeTrue(form.doubleTapSave(), "Could not tap Save");
        sleep(4500);
        assertFalse(form.stillOpen(), "The form stayed open after a double-tapped Update");

        MapLocationListPage list = openList();
        long rows = list.allNames().stream().filter(locB::equals).count();
        assertEquals(1, rows, "\"" + locB + "\" is listed " + rows + " times after a double-tapped Update");
        assumeTrue(list.tapRowActionByBand(locB, "btnEdit"), "Edit not offered on reopen");
        MapLocationFormPage again = new MapLocationFormPage(driver);
        assumeTrue(again.isLoaded(Duration.ofSeconds(10)), "Edit form did not reopen");
        String saved = again.descriptionValue();
        again.cancel();
        sleep(1000);
        backToMap();
        assertEquals(desc, String.valueOf(saved).trim(), "The description was not saved by the double-tapped Update");
    }

    @Test
    @Order(17)
    @DisplayName("N-11c Names edited in the List View show on the map's bottom panel, old names gone")
    void listEditsReachMapPanel() { // [WEB map.md:732] (sheet bug MAP-01)
        assumeTrue(myLocations.contains(locA) && myLocations.contains(locC), "Fixtures missing");
        assumeTrue(!locA.equals(GAP_PREFIX + " LocA " + stamp) && !locC.equals(GAP_PREFIX + " LocC " + stamp),
                "H-04/H-03 did not rename the fixtures, so there is no edit to look for");
        backToMap();
        ensureToolbarReady();
        MapPage m = map();
        assumeTrue(m.openPanelList(), "The bottom panel list did not open (btnLocationPill)");
        List<String> names = new ArrayList<>();
        for (MapPage.PanelCard c : m.panelListCards()) names.add(c.name);
        m.closeBottomPanel();
        backToMap();
        assertTrue(names.contains(locA), "The panel does not show the edited name \"" + locA + "\": " + names);
        assertTrue(names.contains(locC), "The panel does not show the edited name \"" + locC + "\": " + names);
        assertFalse(names.contains(GAP_PREFIX + " LocA " + stamp) || names.contains(GAP_PREFIX + " LocC " + stamp),
                "The panel still shows a pre-edit name: " + names);
    }

    @Test
    @Order(18)
    @DisplayName("C-16b The Cities button reads 'Cities (N)' with N equal to the panel's rows")
    void citiesButtonCountsCities() { // [WEB map.md:56]
        // From a freshly opened Map, so the toolbar strip starts at its left end where the
        // Cities chip lives (a strip scrolled right by earlier cases cannot always be
        // brought back with a swipe).
        leaveMapToTools();
        openMap();
        ensureToolbarReady();
        MapPage m = map();
        String label = m.citiesButtonLabel();
        assumeTrue(m.openCityPanel(), "City panel did not open");
        List<String> cities = m.visibleCityNames();
        m.closeCityPanel();
        sleep(800);
        assumeTrue(!cities.isEmpty(), "No cities on the project");
        assertEquals("Cities (" + cities.size() + ")", label,
                "The Cities button does not count the panel's cities " + cities);
    }

    @Test
    @Order(19)
    @DisplayName("H-06 A multi-line description keeps its lines; a spaces-only one saves empty")
    void multiLineAndBlankDescription() { // [WEB map.md:1163]
        assumeTrue(myLocations.contains(locA), "WG-00 did not create " + locA);
        String two = "ZL_AUTO line one\nZL_AUTO line two";
        MapLocationFormPage form = editRow(locA);
        assumeTrue(form.setDescription(two), "Description field not available");
        assumeTrue(String.valueOf(form.descriptionValue()).contains("\n"),
                "The driver could not type a line break (field reads \"" + form.descriptionValue() + "\")");
        assumeTrue(form.save(), "Save not available");
        sleep(4000);
        assertFalse(form.stillOpen(), "A multi-line description was refused");

        MapLocationListPage list = openList();
        assumeTrue(list.tapRowActionByBand(locA, "btnView"), "View not offered on " + locA);
        sleep(1500);
        boolean kept = !driver.findElements(AppiumBy.xpath("//*[@text=" + xpathLiteral(two) + "]")).isEmpty();
        driver.navigate().back();
        sleep(1000);
        list.backToMap();
        assertTrue(kept, "The detail sheet does not show the description with its line break");

        MapLocationFormPage again = editRow(locA);
        assumeTrue(again.setDescription("     "), "Description field not available");
        assumeTrue(again.save(), "Save not available");
        sleep(4000);
        assertFalse(again.stillOpen(), "A spaces-only description was refused");
        MapLocationListPage list2 = openList();
        assumeTrue(list2.tapRowActionByBand(locA, "btnView"), "View not offered on " + locA);
        sleep(1500);
        boolean descSection = !driver.findElements(AppiumBy.xpath("//*[@text='Description']")).isEmpty();
        boolean oldTextBack = !driver.findElements(AppiumBy.xpath("//*[@text=" + xpathLiteral(two) + "]")).isEmpty();
        driver.navigate().back();
        sleep(1000);
        list2.backToMap();
        backToMap();
        assertFalse(descSection, "After saving a spaces-only description the detail sheet still shows a Description"
                + (oldTextBack ? " — the PREVIOUS text, so the clear never reached the server." : ".")
                + " The form trims it to \"\", and UpdateLocationRequest.description defaults to \"\" with"
                + " encodeDefaults=false, so an empty description is left OUT of the PUT and the server keeps"
                + " the old one: a description (and likewise scene/type) can never be cleared from Android."
                + " Web saves it empty (map.md:1163).");
    }

    @Test
    @Order(24)
    @DisplayName("F-02c More than eight matches: the search lists exactly eight results")
    void searchCapsAtEight() { // [WEB map.md:877]
        // The cap is SearchResultAdapter's (8). Android shows no "+N more" line (web does) —
        // that half is web-only. The query is the city's own name as it appears in
        // reverse-geocoded addresses; the expected match count is read from the List View.
        ensureToolbarReady();
        MapPage m = map();
        String city = String.valueOf(m.cityName()).split("[,(]")[0].trim();
        assumeTrue(!city.isEmpty() && !"Select a City".equals(city), "No city selected");
        String query = null;
        int own = 0;
        for (String q : new String[]{city.toLowerCase(), "a", "e", ","}) {
            MapLocationListPage list = openList();
            own = countOwnRowsMatching(list, q);
            list.backToMap();
            backToMap();
            if (own >= 9) { query = q; break; }
        }
        assumeTrue(query != null, "Fewer than nine of the city's own locations match any probe"
                + " (\"" + city + "\", a, e, \",\") — the cap cannot be seen on this city");
        MapPage m2 = map();
        assumeTrue(m2.openInScreenSearch(), "In-screen search did not open");
        m2.typeSearch(query);
        sleep(2000);
        int shown = rowsIn("rvSearchResults", "tvName");
        m2.closeInScreenSearch();
        sleep(1800);
        ensureToolbarReady();
        assertEquals(8, shown, own + " locations match \"" + query + "\" but the search lists " + shown);
    }

    @Test
    @Order(25)
    @DisplayName("T-07 After the Update confirmation, a subtype added in the edit form is saved")
    void editTypeAddsSubtype() { // [WEB map.md:383] (conflict C3: confirm comes BEFORE the form on Android)
        // The Pad type from H-07: a header this suite made through the type manager. (The
        // inline Solo type exists only as a location's type text — the backend does not
        // create a header for it on this build, so it has no Edit.)
        String padType = GAP_PREFIX + " Pad " + stamp;
        assumeTrue(myTypes.stream().anyMatch(t -> t.trim().equals(padType)), "H-07 did not create " + padType);
        String sub = GAP_PREFIX + " Sub2 " + stamp;
        ensureToolbarReady();
        assumeTrue(viaToolbar(MapPage::openTypes), "Types button not available");
        MapTypeManagerPage types = new MapTypeManagerPage(driver);
        assumeTrue(types.isLoaded(Duration.ofSeconds(12)), "Type manager did not open");
        types.search(padType);
        sleep(1200);
        assumeTrue(types.editType(padType), "Edit not offered on " + padType);
        MapDialogPage dlg = new MapDialogPage(driver);
        assumeTrue(dlg.shown(Duration.ofSeconds(6)), "No Update confirmation");
        assumeTrue(dlg.tap(MapTypeManagerPage.BTN_UPDATE), "No Update button on the confirmation");
        assumeTrue(types.formShown(Duration.ofSeconds(8)), "The edit form did not open after Update");
        types.expandSheet();
        boolean added = types.addSubtype(sub);
        if (!added) { types.scrollSheetContentUp(); added = types.addSubtype(sub); }
        assumeTrue(added, "The subtype row did not accept \"" + sub + "\"");
        assertTrue(types.submitForm(), "Update not tappable: " + types.visibleButtonTexts());
        sleep(3000);
        types.search(padType);
        sleep(1200);
        List<String> subs = types.typeSubtypes();
        backToMap();
        assertTrue(subs.stream().anyMatch(x -> x.contains(sub)),
                "\"" + padType + "\" does not list the subtype added in its edit form: " + subs);
    }

    @Test
    @Order(26)
    @DisplayName("N-19 Tapping a location's marker opens the bottom panel on its card")
    void markerTapOpensCard() { // [WEB map.md:611]
        assumeTrue(myLocations.contains(locB), "No fixture");
        WebElement marker = centreOn(locB);
        assumeTrue(marker != null, "No accessibility node for the marker of \"" + locB + "\" even zoomed in —"
                + " the Maps SDK exposes none on this build, so a marker cannot be addressed");
        // A REAL touch at the marker's node: the node's accessibility click does not reach
        // the cluster item listener (06:21 run: the panel stayed a pill).
        map().tapElementCentre(marker);
        sleep(2500);
        MapPage m = map();
        boolean carousel = m.carouselShown(Duration.ofSeconds(5));
        List<String> cards = m.carouselNames();
        m.closeBottomPanel();
        backToMap();
        // The Maps SDK reports the SAME bounds for every marker node (06:10 dump: LocB and Bold
        // both [500,808][580,908]), so a tap there is not known to land on this marker. If it
        // changed nothing, the marker could not be addressed — a precondition, not a verdict.
        assumeTrue(carousel, "The tap at the marker node's bounds did not reach a marker (panel stayed a pill);"
                + " marker positions are not exposed reliably on this build");
        assertTrue(cards.contains(locB), "A marker tap opened the carousel on other cards: " + cards);
    }

    @Test
    @Order(27)
    @DisplayName("P-14/P-17 Dragging a saved marker asks 'Move Location'; Cancel keeps it, Move moves it")
    void markerDragMoveFlow() { // [WEB map.md:623, map.md:744]
        assumeTrue(myLocations.contains(locB), "No fixture");
        String before = coordsOf(locB);
        WebElement marker = centreOn(locB);
        assumeTrue(marker != null, "No accessibility node for the marker of \"" + locB + "\"");
        MapPage m = map();
        assumeTrue(m.longPressDrag(marker, 90, 60), "The drag gesture did not run");
        MapDialogPage dlg = new MapDialogPage(driver);
        boolean asked = dlg.shown(Duration.ofSeconds(8));
        assumeTrue(asked || !before.equals(coordsOf(locB)), "The long-press drag at the marker node's bounds"
                + " did not pick up a marker (no dialog, no move); marker positions are not exposed reliably");
        assertTrue(asked, "The marker MOVED without asking 'Move Location' first");
        String title = String.valueOf(dlg.title());
        assertTrue(title.equals("Move Location") || title.startsWith("Outside"),
                "Unexpected dialog after the drag: " + title);
        dlg.tap(MapDialogPage.BTN_CANCEL);
        sleep(2500);
        backToMap();
        assertEquals(before, coordsOf(locB), "Cancel on the move dialog still moved \"" + locB + "\"");

        WebElement again = centreOn(locB);
        assumeTrue(again != null, "Marker not addressable for the Move half");
        assumeTrue(map().longPressDrag(again, 90, 60), "The drag gesture did not run");
        MapDialogPage dlg2 = new MapDialogPage(driver);
        assumeTrue(dlg2.shown(Duration.ofSeconds(8)), "No dialog on the second drag");
        boolean tapped = dlg2.tap(MapDialogPage.BTN_MOVE) || dlg2.tap(MapDialogPage.BTN_YES);
        assumeTrue(tapped, "No Move/Yes on " + dlg2.title() + ": " + dlg2.buttonLabels());
        String t = toast(Duration.ofSeconds(4));
        sleep(3000);
        backToMap();
        String after = coordsOf(locB);
        assertNotNull(after, "\"" + locB + "\" is gone after Move");
        assertFalse(after.equals(before), "Move did not change the coordinates (" + before + "), toast: " + t);
    }

    @Test
    @Order(30)
    @DisplayName("H-05 A 1,000-character name: the outcome is recorded and the card stays on screen")
    void thousandCharacterName() { // [WEB map.md:1157]
        assumeTrue(myLocations.contains(locB), "No fixture");
        StringBuilder sb = new StringBuilder(GAP_PREFIX + " Long " + stamp + " ");
        while (sb.length() < 1000) sb.append("word ");
        String longName = sb.toString().trim();
        MapLocationFormPage form = editRow(locB);
        assumeTrue(form.setName(longName), "Could not type the long name");
        assumeTrue(form.save(), "Save not available");
        // Up to 20 s: a long name is a slow save, and "Saving..." must not be read as a refusal.
        String t = null, label = null;
        boolean saved = false;
        long end = System.currentTimeMillis() + 20000;
        while (System.currentTimeMillis() < end) {
            if (t == null) t = toast(Duration.ofSeconds(1));
            if (!form.stillOpen()) { saved = true; break; }
            label = form.saveLabel();
            if (t != null && !"Saving...".equals(label)) break;
        }
        System.out.println(">>> H-05 1000-char name saved=" + saved + " toast=" + t + " saveLabel=" + label);
        if (!saved) {
            form.cancel();
            sleep(1000);
            backToMap();
            assertNotNull(t, "The 1,000-character name was not saved within 20 s and no message was read"
                    + " (Save reads \"" + label + "\"). Web expects an error toast when it is refused.");
            return;
        }
        rename(locB, longName);
        locB = longName;
        backToMap();
        ensureToolbarReady();
        MapPage m = map();
        assumeTrue(m.openPanelList(), "The bottom panel list did not open");
        int width = driver.manage().window().getSize().getWidth();
        int worst = 0;
        for (WebElement e : driver.findElements(byAppId("tvName"))) {
            try {
                if (!String.valueOf(e.getText()).startsWith(GAP_PREFIX + " Long " + stamp)) continue;
                org.openqa.selenium.Rectangle r = e.getRect();
                worst = Math.max(worst, r.getX() + r.getWidth());
            } catch (Exception ignored) { }
        }
        m.closeBottomPanel();
        backToMap();
        assertTrue(worst <= width, "The long name's card text runs off the screen: right edge " + worst + " > " + width);
    }

    // ───────────── M1: deleting a type that has locations ─────────────

    @Test
    @Order(40)
    @DisplayName("T-12 Deleting a type keeps its locations in place, untyped")
    void deletingTypeKeepsItsLocations() { // [WEB map.md:1357]
        assumeTrue(myTypes.contains(typeDoomed) && myLocations.contains(locA), "Fixtures missing");
        // Identify the location by name AND coordinates, so a same-named row cannot stand in
        // for it (the 06:21 run passed falsely: H-09 had left an untyped row with locA's name).
        MapLocationListPage before = openList();
        assumeTrue(before.revealRow(locA), "\"" + locA + "\" is not listed before the delete");
        String coords = before.rowChildText(locA, "tvCoords");
        String typeBefore = before.rowChildText(locA, "tvTypeBadge");
        before.backToMap();
        assumeTrue(typeBefore != null && typeBefore.contains(typeDoomed),
                "\"" + locA + "\" does not carry " + typeDoomed + " before the delete (badge: " + typeBefore + ")");
        assumeTrue(viaToolbar(MapPage::openTypes), "Types button not available");
        MapTypeManagerPage types = new MapTypeManagerPage(driver);
        assumeTrue(types.isLoaded(Duration.ofSeconds(12)), "Type manager did not open");
        types.search(typeDoomed);
        sleep(1200);
        assumeTrue(types.deleteType(typeDoomed), "Delete not offered on " + typeDoomed);
        MapDialogPage dlg = new MapDialogPage(driver);
        assumeTrue(dlg.shown(Duration.ofSeconds(6)), "Delete raised no confirmation");
        String msg = String.valueOf(dlg.message());
        assumeTrue(dlg.tap(MapDialogPage.BTN_DELETE), "No Delete button in the confirmation");
        sleep(3500);
        types.search(typeDoomed);
        sleep(1000);
        boolean typeGone = !types.typeNames().contains(typeDoomed);
        if (typeGone) myTypes.remove(typeDoomed);
        backToMap();
        assertTrue(typeGone, "The type survived a confirmed delete");

        MapLocationListPage list = openList();
        boolean stillListed = list.revealRow(locA) && String.valueOf(coords).equals(list.rowChildText(locA, "tvCoords"));
        String badge = stillListed ? list.rowChildText(locA, "tvTypeBadge") : null;
        list.backToMap();
        backToMap();
        if (!stillListed) myLocations.remove(locA);
        assertTrue(stillListed, "Deleting the type \"" + typeDoomed + "\" also deleted the location \"" + locA
                + "\" (" + coords + ") that used it. The dialog promised the opposite: \"" + msg + "\" — data loss"
                + " (web bug map.md:1357; same deleteHeader endpoint on Android)");
        assertTrue(badge == null || badge.isEmpty() || !badge.contains(typeDoomed),
                "\"" + locA + "\" still carries the deleted type's badge: " + badge);
    }

    @Test
    @Order(41)
    @DisplayName("V-07 Deleting the last location of the selected List View chip leaves no blank list")
    void deleteLastOfSelectedChip() { // [WEB map.md:1231]
        assumeTrue(myLocations.contains(locC), "No fixture of " + typeSolo);
        MapLocationListPage list = openList();
        assumeTrue(list.tapChip(typeSolo), "No chip for " + typeSolo + ": " + list.allChipTexts());
        List<String> shown = list.names();
        assumeTrue(shown.size() == 1 && shown.get(0).equals(locC),
                "The " + typeSolo + " chip should show only " + locC + ": " + shown);
        assumeTrue(list.tapRowActionByBand(locC, "btnDelete"), "Delete not offered on " + locC);
        MapDialogPage dlg = new MapDialogPage(driver);
        assumeTrue(dlg.shown(Duration.ofSeconds(6)), "Delete raised no confirmation");
        assumeTrue(dlg.tap(MapDialogPage.BTN_DELETE), "No Delete button");
        sleep(4000);
        myLocations.remove(locC);
        List<String> after = list.names();
        boolean empty = list.emptyStateShown(Duration.ofSeconds(3));
        String soloChip = list.chipFor(typeSolo);
        list.backToMap();
        backToMap();
        assertFalse(after.contains(locC), "\"" + locC + "\" is still listed after a confirmed delete");
        // Android falls back to "All" when the active type has no rows left
        // (LocationListFragment, "A filter whose type no longer exists…"); web keeps the
        // chip and shows the empty state. Either is fine — a blank list with neither is not.
        assertTrue(empty || !after.isEmpty(),
                "The list is blank with no empty state after deleting the chip's last location");
        assertTrue(soloChip == null, "The chip for the now-empty type is still offered: " + soloChip);
    }

    @Test
    @Order(45)
    @DisplayName("H-09 Two locations may share a name: the outcome is recorded and the list and search agree")
    void duplicateLocationNames() { // [WEB map.md:1176]
        // Runs after T-12/V-07 so a duplicate name cannot confuse them. Uses locB and H-01's
        // Shooting fixture; nothing later depends on either name.
        String other = GAP_PREFIX + " Trim " + stamp;
        assumeTrue(myLocations.contains(locB) && myLocations.contains(other), "Fixtures missing");
        String shared = locB;
        MapLocationFormPage form = editRow(other);
        assumeTrue(form.setName(shared), "Could not type the name");
        assumeTrue(form.save(), "Save not available");
        sleep(4000);
        boolean accepted = !form.stillOpen();
        if (!accepted) { form.cancel(); sleep(1000); } else myLocations.remove(other);   // cleanup deletes every row named `shared`
        System.out.println(">>> H-09 duplicate location name accepted=" + accepted);

        MapLocationListPage list = openList();
        long rows = list.allRowKeys().stream().filter(k -> k.startsWith(shared + "|")).count();
        list.backToMap();
        backToMap();
        MapPage m = map();
        assumeTrue(m.openInScreenSearch(), "In-screen search did not open");
        m.typeSearch(shared);
        sleep(1500);
        m.closeKeyboard();      // rows under the IME are not in the tree
        sleep(800);
        long hits = m.searchResultNames().stream().filter(shared::equals).count();
        m.closeInScreenSearch();
        sleep(1800);
        ensureToolbarReady();
        assertEquals(accepted ? 2 : 1, rows, "The List View disagrees with the save outcome (accepted=" + accepted + ")");
        assertEquals(accepted ? 2 : 1, hits, "Search disagrees with the save outcome (accepted=" + accepted + ")");
    }

    // ───────────────────── M3: two users in real time ─────────────────────
    //
    // Self-contained: each case makes its own ZL_AUTO data and removes it. A = this suite's
    // device (ZILLIT_UDID, Vidya Testing); B = ZILLIT_RECEIVER_UDID (the Pixel, Vidya
    // Pixel). Without a receiver they skip. Run on their own:
    //   ZILLIT_UDID=emulator-5556 ZILLIT_RECEIVER_UDID=34281FDH2005XC \
    //     ./run-and-report.sh --only 'MapWebGapTest.rt*'

    private static final Duration LIVE = Duration.ofSeconds(40);
    private io.appium.java_client.android.AndroidDriver b;
    private String rtCity;

    /** B's driver, on the Map, on the same city as A. Skips when there is no receiver. */
    private io.appium.java_client.android.AndroidDriver peer() {
        String udid = firstNonBlank(com.zillit.automation.core.Config.get("zillit.receiver.udid"),
                System.getenv("ZILLIT_RECEIVER_UDID"));
        assumeTrue(udid != null, "Two-user case: needs ZILLIT_RECEIVER_UDID (user B's device)");
        assumeTrue(ensureCitySelected(), "A has no city selected");
        ensureToolbarReady();
        rtCity = String.valueOf(map().cityName()).trim();
        if (b == null) {
            b = com.zillit.automation.core.DriverFactory.create(udid, 8300);
        }
        assumeTrue(enterMap(b), "B could not reach the Map");
        MapPage mb = new MapPage(b);
        if (!rtCity.equals(String.valueOf(mb.cityName()).trim())) {
            assumeTrue(mb.openCityPanel() && mb.selectCity(rtCity), "B could not open " + rtCity);
            sleep(3000);
        }
        assumeTrue(rtCity.equals(String.valueOf(mb.cityName()).trim()), "B is not on " + rtCity);
        return b;
    }

    /**
     * Safety net for method-filtered runs (--only 'MapWebGapTest.<method>'), where WG-99
     * does not run: whatever this run created and still holds is removed here.
     */
    @org.junit.jupiter.api.AfterAll
    void cleanupIfNeeded() {
        if (myLocations.isEmpty() && myTypes.isEmpty()) return;
        try { cleanup(); } catch (Throwable t) {
            System.out.println(">>> after-all cleanup: " + t.getMessage() + " | left " + myLocations + " " + myTypes);
        }
    }

    @org.junit.jupiter.api.AfterAll
    void quitPeer() {
        try { if (b != null) b.quit(); } catch (Exception ignored) { }
    }

    @Test
    @Order(50)
    @DisplayName("RT-02 A pin B adds in A's open city updates A's count live; B's delete takes it back")
    void rtPinCountLive() { // [WEB map.md:806, map.md:1239]
        io.appium.java_client.android.AndroidDriver d = peer();
        MapPage ma = map();
        int before = ma.locationCount();
        String name = GAP_PREFIX + " RtPin " + stamp;
        boolean made = false;
        try {
            made = pinOn(d, name);
            assumeTrue(made, "B could not drop the pin");
            int after = waitCount(ma, before + 1);
            assertEquals(before + 1, after, "A's header did not follow B's new pin without a reload");
            assertTrue(deleteOn(d, name), "B could not delete its pin");
            made = false;
            assertEquals(before, waitCount(ma, before), "A's header did not follow B's delete");
        } finally {
            if (made) deleteOn(d, name);
        }
    }

    @Test
    @Order(51)
    @DisplayName("RT-03 A type B adds appears in A's open type list; B's delete removes it")
    void rtTypeLive() { // [WEB map.md:818, map.md:1272]
        io.appium.java_client.android.AndroidDriver d = peer();
        String name = GAP_PREFIX + " RtType " + stamp;
        ensureToolbarReady();
        assumeTrue(viaToolbar(MapPage::openTypes), "A: Types button not available");
        MapTypeManagerPage ta = new MapTypeManagerPage(driver);
        assumeTrue(ta.isLoaded(Duration.ofSeconds(12)), "A: type manager did not open");
        boolean made = false;
        try {
            MapPage mb = new MapPage(d);
            assumeTrue(mb.openTypes(), "B: Types button not available");
            made = typeOn(d, name);
            assumeTrue(made, "B could not create the type");
            assertTrue(waitTypeListed(ta, name, true), "B's new type \"" + name + "\" never appeared in A's open list");
            assertTrue(deleteTypeOn(d, name), "B could not delete the type");
            made = false;
            assertTrue(waitTypeListed(ta, name, false), "B deleted \"" + name + "\" but A's open list still shows it");
        } finally {
            if (made) deleteTypeOn(d, name);
            backToMap();
        }
    }

    @Test
    @Order(52)
    @DisplayName("RT-06 A zone B adds becomes A's active zone live; B's delete moves A off it")
    void rtZoneLive() { // [WEB map.md:812, map.md:1259]
        io.appium.java_client.android.AndroidDriver d = peer();
        MapPage ma = map();
        String beforeLabel = ma.toolbarLabel("btnZoneList");
        assumeTrue("Studio Zone".equals(beforeLabel),
                "A's city already has an active zone (" + beforeLabel + "); B's new one would not be shown");
        String name = GAP_PREFIX + " RtZone " + stamp;
        boolean made = false;
        try {
            made = zoneOn(d, name);
            assumeTrue(made, "B could not create the zone");
            assertTrue(waitLabel(ma, name), "A's zone button never named B's new zone (reads "
                    + ma.toolbarLabel("btnZoneList") + ")");
            assertTrue(deleteZoneOn(d, name), "B could not delete the zone");
            made = false;
            assertTrue(waitLabel(ma, "Studio Zone"), "B deleted the zone but A still shows \""
                    + ma.toolbarLabel("btnZoneList") + "\"");
        } finally {
            if (made) deleteZoneOn(d, name);
        }
    }

    @Test
    @Order(53)
    @DisplayName("RT-07 A stale edit form does not overwrite B's newer description")
    void rtStaleEdit() { // [WEB map.md:1245]
        io.appium.java_client.android.AndroidDriver d = peer();
        String name = GAP_PREFIX + " RtEdit " + stamp;
        String renamed = name + " A";
        String bDesc = GAP_PREFIX + " by B " + stamp;
        ensureToolbarReady();
        assumeTrue(createLocationByPin(0.52, 0.45, name), "A could not pin " + name + ": " + lastPinFailure);
        myLocations.add(name);
        String saved;
        try {
            MapLocationFormPage fa = editRow(name);                 // A's form is now stale-to-be
            assumeTrue(editDescriptionOn(d, name, bDesc), "B could not edit the description");
            assumeTrue(fa.setName(renamed) && fa.save(), "A could not save");
            sleep(4000);
            rename(name, renamed);
            MapLocationFormPage again = editRow(renamed);
            saved = again.descriptionValue();
            again.cancel();
            sleep(1000);
            backToMap();
        } finally {
            // This case runs on its own (--only 'MapWebGapTest.rt*'), without WG-99: remove
            // the location here, under whichever of its two names it carries now.
            for (String n : new String[]{renamed, name}) {
                if (!myLocations.contains(n)) continue;
                MapLocationListPage l = openListOrNull();
                if (l != null && l.tapRowActionByBand(n, "btnDelete")) {
                    MapDialogPage dlg = new MapDialogPage(driver);
                    if (dlg.shown(Duration.ofSeconds(6))) dlg.tap(MapDialogPage.BTN_DELETE);
                    sleep(3500);
                    if (!l.revealRow(n)) myLocations.remove(n);
                }
                backToMap();
            }
        }
        assertEquals(bDesc, String.valueOf(saved).trim(),
                "A's save from a form opened BEFORE B's edit wiped B's description (full-record PUT,"
                        + " LocationFormViewModel.submitToApi; web bug map.md:1245)");
    }

    // ---- B-side helpers (all page objects bound to B's driver) ----

    private boolean enterMap(io.appium.java_client.android.AndroidDriver d) {
        try {
            if (new MapPage(d).isLoaded(Duration.ofSeconds(2))) return true;
            d.executeScript("mobile: activateApp", Map.of("appId", appPackage));
            sleep(3000);
            if (new MapPage(d).isLoaded(Duration.ofSeconds(2))) return true;
            for (int i = 0; i < 40; i++) {
                if (!d.findElements(byAppId("bottomNavigation")).isEmpty()) break;
                if (!d.findElements(byAppId("projectListRecyclerView")).isEmpty()) {
                    com.zillit.automation.pages.common.ProjectListPage pl =
                            new com.zillit.automation.pages.common.ProjectListPage(d);
                    List<String> names = pl.visibleProjectNames();
                    if (!names.isEmpty()) pl.tapProject(names.get(0));
                }
                sleep(1500);
            }
            com.zillit.automation.pages.common.ToolsPage tools = new com.zillit.automation.pages.common.ToolsPage(d);
            if (!tools.openToolsTab() || !tools.isLoaded(Duration.ofSeconds(15))) return false;
            tools.searchTools("Map");
            if (!tools.openModule("Map")) return false;
            return new MapPage(d).isLoaded(Duration.ofSeconds(25));
        } catch (Exception e) {
            return false;
        }
    }

    private void bToMap(io.appium.java_client.android.AndroidDriver d) {
        for (int i = 0; i < 5 && !new MapPage(d).isLoaded(Duration.ofSeconds(2)); i++) {
            try { d.navigate().back(); } catch (Exception ignored) { }
            sleep(900);
        }
    }

    private boolean pinOn(io.appium.java_client.android.AndroidDriver d, String name) {
        bToMap(d);
        MapPage m = new MapPage(d);
        if (!m.pinModeActive(Duration.ofSeconds(1)) && !m.togglePinMode()) return false;
        if (!m.pinModeActive(Duration.ofSeconds(6)) || !m.dropPinAt(0.5, 0.5) || !m.commitPin()) return false;
        MapDialogPage dlg = new MapDialogPage(d);
        if (dlg.shown(Duration.ofSeconds(3)) && !dlg.tap(MapDialogPage.BTN_YES)) dlg.tapStartsWith("Pin in ");
        MapLocationFormPage f = new MapLocationFormPage(d);
        if (!f.isLoaded(Duration.ofSeconds(12)) || !f.setName(name) || !f.save()) return false;
        sleep(4000);
        boolean ok = !f.stillOpen();
        bToMap(d);
        if (m.pinModeActive(Duration.ofSeconds(1))) m.togglePinMode();
        return ok;
    }

    private boolean deleteOn(io.appium.java_client.android.AndroidDriver d, String name) {
        bToMap(d);
        MapPage m = new MapPage(d);
        if (!m.openViewAll()) return false;
        MapLocationListPage l = new MapLocationListPage(d);
        if (!l.isLoaded(Duration.ofSeconds(15))) return false;
        sleep(1500);
        if (!l.tapRowActionByBand(name, "btnDelete")) { bToMap(d); return false; }
        MapDialogPage dlg = new MapDialogPage(d);
        if (dlg.shown(Duration.ofSeconds(6))) dlg.tap(MapDialogPage.BTN_DELETE);
        sleep(3500);
        boolean gone = !l.revealRow(name);
        bToMap(d);
        return gone;
    }

    private boolean editDescriptionOn(io.appium.java_client.android.AndroidDriver d, String name, String desc) {
        bToMap(d);
        MapPage m = new MapPage(d);
        if (!m.openViewAll()) return false;
        MapLocationListPage l = new MapLocationListPage(d);
        if (!l.isLoaded(Duration.ofSeconds(15))) return false;
        sleep(1500);
        if (!l.tapRowActionByBand(name, "btnEdit")) { bToMap(d); return false; }
        MapLocationFormPage f = new MapLocationFormPage(d);
        boolean ok = f.isLoaded(Duration.ofSeconds(10)) && f.setDescription(desc) && f.save();
        sleep(4000);
        ok = ok && !f.stillOpen();
        bToMap(d);
        return ok;
    }

    private boolean typeOn(io.appium.java_client.android.AndroidDriver d, String name) {
        MapTypeManagerPage t = new MapTypeManagerPage(d);
        if (!t.isLoaded(Duration.ofSeconds(12)) || !t.openAdd() || !t.formShown(Duration.ofSeconds(10))) return false;
        t.expandSheet();
        if (!t.setTypeName(name) || !t.submitForm()) return false;
        sleep(3000);
        t.search(name);
        sleep(1000);
        return t.typeNames().contains(name);
    }

    private boolean deleteTypeOn(io.appium.java_client.android.AndroidDriver d, String name) {
        MapTypeManagerPage t = new MapTypeManagerPage(d);
        if (!t.isLoaded(Duration.ofSeconds(3))) {
            bToMap(d);
            if (!new MapPage(d).openTypes() || !t.isLoaded(Duration.ofSeconds(12))) return false;
        }
        t.search(name);
        sleep(1000);
        if (!t.deleteType(name)) return false;
        MapDialogPage dlg = new MapDialogPage(d);
        if (dlg.shown(Duration.ofSeconds(6))) dlg.tap(MapDialogPage.BTN_DELETE);
        sleep(3000);
        t.search(name);
        sleep(1000);
        boolean gone = !t.typeNames().contains(name);
        bToMap(d);
        return gone;
    }

    private boolean zoneOn(io.appium.java_client.android.AndroidDriver d, String name) {
        bToMap(d);
        if (!new MapPage(d).openZoneList()) return false;
        com.zillit.automation.pages.map.MapZonePage z = new com.zillit.automation.pages.map.MapZonePage(d);
        if (!z.listLoaded(Duration.ofSeconds(15)) || !z.openAddZone() || !z.formLoaded(Duration.ofSeconds(12))) return false;
        if (!z.chooseIntersectionMode() || !z.setStreet1(System.getProperty("zillit.zoneStreet1", "Castro Street"))
                || !z.setStreet2(System.getProperty("zillit.zoneStreet2", "El Camino Real")) || !z.findIntersection()) return false;
        if (!z.setZoneName(name)) return false;
        z.pickRadiusChip("30 mi");
        if (!z.save()) return false;
        sleep(4000);
        boolean ok = !z.formStillOpen();
        bToMap(d);
        return ok;
    }

    private boolean deleteZoneOn(io.appium.java_client.android.AndroidDriver d, String name) {
        bToMap(d);
        if (!new MapPage(d).openZoneList()) return false;
        com.zillit.automation.pages.map.MapZonePage z = new com.zillit.automation.pages.map.MapZonePage(d);
        if (!z.listLoaded(Duration.ofSeconds(15))) return false;
        sleep(1200);
        boolean any = false;
        for (int i = 0; i < 3 && z.tapRowByBand(name, "btnDelete"); i++) {
            MapDialogPage dlg = new MapDialogPage(d);
            if (dlg.shown(Duration.ofSeconds(6))) dlg.tap(MapDialogPage.BTN_DELETE);
            sleep(3000);
            any = true;
        }
        boolean gone = z.rowsNamed(name) == 0;
        bToMap(d);
        return any && gone;
    }

    private int waitCount(MapPage m, int want) {
        long end = System.currentTimeMillis() + LIVE.toMillis();
        int c = m.locationCount();
        while (c != want && System.currentTimeMillis() < end) { sleep(1500); c = m.locationCount(); }
        return c;
    }

    private boolean waitTypeListed(MapTypeManagerPage t, String name, boolean listed) {
        long end = System.currentTimeMillis() + LIVE.toMillis();
        while (System.currentTimeMillis() < end) {
            t.search(name);
            sleep(1200);
            if (t.typeNames().contains(name) == listed) return true;
            sleep(1500);
        }
        return false;
    }

    private boolean waitLabel(MapPage m, String want) {
        long end = System.currentTimeMillis() + LIVE.toMillis();
        while (System.currentTimeMillis() < end) {
            if (want.equals(m.toolbarLabel("btnZoneList"))) return true;
            sleep(2000);
        }
        return false;
    }

    private static String firstNonBlank(String... v) {
        for (String s : v) if (s != null && !s.isBlank()) return s;
        return null;
    }

    // ─────────────────────────────── cleanup ───────────────────────────────

    @Test
    @Order(99)
    @DisplayName("WG-99 Cleanup: only the ZL_AUTO locations and types this suite created are deleted")
    void cleanup() {
        // What this run recorded, plus anything an EARLIER run of this same suite left
        // behind when it died before its cleanup. The sweep matches only the exact name
        // shapes this class creates ("ZL_AUTO LocA 12345", "ZL_AUTO TypeDoomed 12345", …),
        // so it can never reach a person's data or another suite's fixtures.
        List<String> notDeleted = new ArrayList<>();
        java.util.LinkedHashSet<String> locs = new java.util.LinkedHashSet<>(myLocations);
        try {
            MapLocationListPage list = openListOrNull();
            if (list != null) {
                for (String n : list.allNames()) if (OWN_LOCATION.matcher(n).matches()) locs.add(n);
                list.backToMap();
            }
        } catch (Exception ignored) { }
        for (String name : locs) {
            if (!name.startsWith(GAP_PREFIX)) { notDeleted.add(name + " (not ZL_AUTO — refused)"); continue; }
            MapLocationListPage list = openListOrNull();
            if (list == null) { notDeleted.add(name + " (list did not open)"); continue; }
            // Up to three rows: H-09 can leave two locations sharing one name.
            for (int i = 0; i < 3 && list.tapRowActionByBand(name, "btnDelete"); i++) {
                MapDialogPage dlg = new MapDialogPage(driver);
                if (dlg.shown(Duration.ofSeconds(6))) dlg.tap(MapDialogPage.BTN_DELETE);
                sleep(3500);
                System.out.println(">>> WG-99 deleted location " + name);
            }
            if (list.revealRow(name)) notDeleted.add(name); else myLocations.remove(name);
            list.backToMap();
            backToMap();
        }

        java.util.LinkedHashSet<String> types = new java.util.LinkedHashSet<>(myTypes);
        MapTypeManagerPage tm = openTypesOrNull();
        if (tm != null) {
            tm.search(GAP_PREFIX);
            sleep(1200);
            for (String n : rawTexts("tvName")) if (OWN_TYPE.matcher(n.trim()).matches()) types.add(n);
            backToMap();
        }
        for (String type : types) {
            if (!type.trim().startsWith(GAP_PREFIX)) { notDeleted.add(type + " (not ZL_AUTO — refused)"); continue; }
            MapTypeManagerPage t = openTypesOrNull();
            if (t == null) { notDeleted.add(type + " (types did not open)"); continue; }
            t.search(type.trim());
            sleep(1200);
            if (t.deleteType(type) || t.deleteType(type.trim())) {
                MapDialogPage dlg = new MapDialogPage(driver);
                if (dlg.shown(Duration.ofSeconds(6))) dlg.tap(MapDialogPage.BTN_DELETE);
                sleep(3000);
                System.out.println(">>> WG-99 deleted type \"" + type + "\"");
                t.search(type.trim());
                sleep(1000);
            }
            if (rawTexts("tvName").stream().anyMatch(x -> x.trim().equals(type.trim()))) notDeleted.add(type);
            else myTypes.remove(type);
            backToMap();
        }
        assertTrue(notDeleted.isEmpty(), "Left on the project by this suite: " + notDeleted);
    }

    /** Location names this class creates, in every shape the cases rename them to. */
    private static final Pattern OWN_LOCATION = Pattern.compile(
            "ZL_AUTO RtEdit \\d{1,5} A|ZL_AUTO (RtPin|RtEdit|LocA|LocD|LocB|LocC|Trim|Ünïcødé 🎬 שלום|<b>Bold</b>) \\d{1,5}|ZL_AUTO Long \\d{1,5}( word)+");
    /** Type names this class creates. */
    private static final Pattern OWN_TYPE = Pattern.compile("ZL_AUTO (TypeDoomed|Solo|Pad|RtType) \\d{1,5}");

    private MapLocationListPage openListOrNull() {
        try {
            backToMap();
            ensureToolbarReady();
            MapPage m = new MapPage(driver);
            if (!viaToolbar(MapPage::openViewAll)) return null;
            MapLocationListPage list = new MapLocationListPage(driver);
            if (!list.isLoaded(Duration.ofSeconds(15))) return null;
            sleep(1500);
            return list;
        } catch (Exception e) {
            return null;
        }
    }

    private MapTypeManagerPage openTypesOrNull() {
        try {
            backToMap();
            ensureToolbarReady();
            MapPage m = new MapPage(driver);
            if (!viaToolbar(MapPage::openTypes)) return null;
            MapTypeManagerPage t = new MapTypeManagerPage(driver);
            return t.isLoaded(Duration.ofSeconds(12)) ? t : null;
        } catch (Exception e) {
            return null;
        }
    }

    // ─────────────────────────────── plumbing ───────────────────────────────

    private String lastGapFailure;

    /**
     * Creates a type with a subtype from the open type manager. The subtype row is flaky to
     * reach on some runs (the sheet's IME/peek state), so a failed attempt is cancelled and
     * retried once from a fresh sheet before it counts.
     */
    private boolean createTypeWithSubtype(String name, String subtype) {
        if (createTypeOnce(name, subtype)) return true;
        if (subtype == null || lastGapFailure == null || !lastGapFailure.startsWith("subtype")) return false;
        System.out.println(">>> retrying the type sheet after: " + lastGapFailure);
        sleep(1500);
        MapTypeManagerPage types = new MapTypeManagerPage(driver);
        if (types.formShown(Duration.ofSeconds(1))) { types.cancelForm(); sleep(1000); }
        if (!types.isLoaded(Duration.ofSeconds(5))) {
            backToMap();
            ensureToolbarReady();
            if (!viaToolbar(MapPage::openTypes)) return gapFail("Types button not available on retry");
        }
        return createTypeOnce(name, subtype);
    }

    private boolean createTypeOnce(String name, String subtype) {
        MapTypeManagerPage types = new MapTypeManagerPage(driver);
        lastGapFailure = null;
        if (!types.isLoaded(Duration.ofSeconds(10))) return gapFail("type manager never loaded");
        if (!types.openAdd()) return gapFail("Add button not available — no posting access?");
        if (!types.formShown(Duration.ofSeconds(10))) return gapFail("add-type sheet never appeared");
        types.expandSheet();
        if (!types.setTypeName(name)) return gapFail("could not type the name");
        boolean subOk = subtype == null || types.addSubtype(subtype);
        if (!subOk) {
            types.scrollSheetContentUp();
            subOk = types.addSubtype(subtype);
        }
        if (!subOk) {
            // Leave the sheet closed, so the next case does not start under it.
            types.cancelForm();
            return gapFail("subtype \"" + subtype + "\" was not accepted; field holds \""
                    + types.subtypeFieldText() + "\"");
        }
        if (!types.submitForm()) return gapFail("Create not tappable: " + types.visibleButtonTexts());
        sleep(3000);
        types.search(name.trim());
        sleep(1000);
        boolean ok = types.typeNames().contains(name.trim());
        return ok || gapFail("Create was tapped but \"" + name.trim() + "\" is not listed: " + types.typeNames());
    }

    private boolean gapFail(String why) {
        lastGapFailure = why;
        System.out.println(">>> " + why);
        return false;
    }

    private MapLocationListPage openList() {
        backToMap();
        ensureToolbarReady();
        assumeTrue(viaToolbar(MapPage::openViewAll), "View All not available, even from a freshly opened Map");
        MapLocationListPage list = new MapLocationListPage(driver);
        assumeTrue(list.isLoaded(Duration.ofSeconds(15)), "The location list did not open");
        sleep(1500);
        return list;
    }

    private MapLocationFormPage editRow(String name) {
        MapLocationListPage list = openList();
        assumeTrue(list.tapRowActionByBand(name, "btnEdit"), "Edit not offered on the row for " + name);
        MapLocationFormPage form = new MapLocationFormPage(driver);
        assumeTrue(form.isLoaded(Duration.ofSeconds(10)), "Edit form did not open");
        sleep(1500);
        return form;
    }

    private void rename(String from, String to) {
        int i = myLocations.indexOf(from);
        if (i >= 0) myLocations.set(i, to);
    }

    /** Opens the in-screen search on {@code name} and taps it, so the camera centres on it; returns its marker. */
    private WebElement centreOn(String name) {
        backToMap();
        ensureToolbarReady();
        MapPage m = map();
        if (!m.openInScreenSearch()) return null;
        m.typeSearch(name);
        sleep(1500);
        boolean tapped = false;
        for (WebElement e : driver.findElements(AppiumBy.xpath(
                "//*[@resource-id='" + appPackage + ":id/rvSearchResults']//*[@resource-id='"
                        + appPackage + ":id/tvName']"))) {
            try { if (name.equals(e.getText().trim())) { e.click(); tapped = true; break; } } catch (Exception ignored) { }
        }
        if (!tapped) { m.closeInScreenSearch(); sleep(1500); ensureToolbarReady(); return null; }
        sleep(2500);
        ensureToolbarReady();
        return map().markerFor(name);
    }

    /** "lat, lng" of a List View row (tvCoords), or null when the row is not listed. */
    private String coordsOf(String name) {
        MapLocationListPage list = openList();
        String c = list.revealRow(name) ? list.rowChildText(name, "tvCoords") : null;
        list.backToMap();
        backToMap();
        return c;
    }

    /** Taps Edit on the n-th row (1-based) whose name equals the first duplicate on screen. */
    private boolean tapEditOnSameNamedRow(MapLocationListPage list, int n) {
        List<WebElement> names = new ArrayList<>(driver.findElements(byAppId("tvName")));
        List<WebElement> edits = new ArrayList<>(driver.findElements(byAppId("btnEdit")));
        java.util.Comparator<WebElement> byY = java.util.Comparator.comparingInt(e -> {
            try { return e.getRect().getY(); } catch (Exception x) { return Integer.MAX_VALUE; }
        });
        names.sort(byY);
        edits.sort(byY);
        int seen = 0;
        for (int i = 0; i < names.size(); i++) {
            try {
                if (!locA.equals(names.get(i).getText().trim()) || ++seen != n) continue;
                int y = names.get(i).getRect().getY();
                int nextY = i + 1 < names.size() ? names.get(i + 1).getRect().getY() : Integer.MAX_VALUE;
                for (WebElement b : edits) {
                    int by = b.getRect().getY();
                    if (by > y && by < nextY) { b.click(); sleep(1500); return true; }
                }
            } catch (Exception ignored) { }
        }
        return false;
    }

    /** Rows in a list that the city's own (non Location-tool) locations would match on name/address/type. */
    private int countOwnRowsMatching(MapLocationListPage list, String q) {
        java.util.Set<String> seen = new java.util.LinkedHashSet<>();
        int matches = 0;
        for (int pass = 0; pass < 14; pass++) {
            int before = seen.size();
            List<WebElement> names = new ArrayList<>(driver.findElements(byAppId("tvName")));
            for (WebElement n : names) {
                try {
                    String name = n.getText().trim();
                    if (!seen.add(name)) continue;
                    String badge = String.valueOf(list.rowChildText(name, "tvTypeBadge"));
                    if (badge.contains("Shoot Location")) continue;     // overlay row: search ignores it
                    String addr = String.valueOf(list.rowChildText(name, "tvAddress"));
                    if ((name + " " + addr + " " + badge).toLowerCase().contains(q)) matches++;
                } catch (Exception ignored) { }
            }
            if (seen.size() == before) break;
            try {
                org.openqa.selenium.Rectangle r = driver.findElement(byAppId("rvLocations")).getRect();
                int x = r.getX() + r.getWidth() / 2;
                org.openqa.selenium.interactions.PointerInput f = new org.openqa.selenium.interactions.PointerInput(
                        org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
                org.openqa.selenium.interactions.Sequence sq = new org.openqa.selenium.interactions.Sequence(f, 1);
                sq.addAction(f.createPointerMove(Duration.ZERO, org.openqa.selenium.interactions.PointerInput.Origin.viewport(),
                        x, r.getY() + (int) (r.getHeight() * 0.8)));
                sq.addAction(f.createPointerDown(0));
                sq.addAction(f.createPointerMove(Duration.ofMillis(500), org.openqa.selenium.interactions.PointerInput.Origin.viewport(),
                        x, r.getY() + (int) (r.getHeight() * 0.3)));
                sq.addAction(f.createPointerUp(0));
                driver.perform(List.of(sq));
                sleep(900);
            } catch (Exception e) { break; }
        }
        return matches;
    }

    private int rowsIn(String containerId, String childId) {
        return driver.findElements(AppiumBy.xpath("//*[@resource-id='" + appPackage + ":id/" + containerId
                + "']//*[@resource-id='" + appPackage + ":id/" + childId + "']")).size();
    }

    /**
     * Runs a toolbar action, re-entering the Map between attempts. The toolbar strip is a
     * HorizontalScrollView whose swipes are sometimes swallowed or jump past a chip; a fresh
     * Map always starts the strip at its left end, from where the forward swipe is reliable.
     */
    private boolean viaToolbar(java.util.function.Predicate<MapPage> action) {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                backToMap();
                ensureToolbarReady();
                MapPage m = new MapPage(driver);
                if (m.isLoaded(Duration.ofSeconds(10)) && action.test(m)) return true;
            } catch (org.opentest4j.TestAbortedException ignored) { }
            System.out.println(">>> toolbar action failed (attempt " + (attempt + 1) + "); re-entering the Map");
            leaveMapToTools();
            try { openMap(); } catch (org.opentest4j.TestAbortedException ignored) { }
            sleep(1500);
        }
        return false;
    }

    /** Gets back onto the Map, re-opening it from Tools if a case left the app elsewhere. */
    private void mapHere() {
        for (int i = 0; i < 2; i++) {
            try { backToMap(); } catch (org.opentest4j.TestAbortedException ignored) { }
            if (new MapPage(driver).isLoaded(Duration.ofSeconds(8))) { ensureToolbarReady(); return; }
            leaveMapToTools();
            try { openMap(); } catch (org.opentest4j.TestAbortedException ignored) { }
        }
        if (!new MapPage(driver).isLoaded(Duration.ofSeconds(3))) {
            // Seen twice: Tools listed "Map" but the tap did not open it. Restart to a clean state.
            try { recoverToBottomNav(); openMap(); } catch (org.opentest4j.TestAbortedException ignored) { }
        }
        ensureToolbarReady();
    }

    /** Leaves the Map for the Tools list with BACK, stopping at the bottom-nav boundary. */
    private void leaveMapToTools() {
        for (int i = 0; i < 5 && !onBottomNavScreen(); i++) {
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(1000);
        }
    }

    /** Untrimmed texts of every element with this id, for whitespace assertions. */
    private List<String> rawTexts(String id) {
        List<String> out = new ArrayList<>();
        for (WebElement e : driver.findElements(byAppId(id))) {
            try { if (e.getText() != null) out.add(e.getText()); } catch (Exception ignored) { }
        }
        return out;
    }

    /** The untrimmed tvName of the List View row whose trimmed name is {@code trimmed}. */
    private String rawRowName(MapLocationListPage list, String trimmed) {
        list.revealRow(trimmed);
        for (String t : rawTexts("tvName")) if (t.trim().equals(trimmed)) return t;
        return null;
    }

    private static String quoteAll(List<String> l) {
        List<String> q = new ArrayList<>();
        for (String s : l) q.add("\"" + s + "\"");
        return q.toString();
    }

    static String xpathLiteral(String s) {
        if (!s.contains("'")) return "'" + s + "'";
        if (!s.contains("\"")) return "\"" + s + "\"";
        return "concat('" + s.replace("'", "',\"'\",'") + "')";
    }

    /** The text of the first Toast seen within the timeout, or null. */
    String toast(Duration timeout) {
        Pattern p = Pattern.compile("<android\\.widget\\.Toast[^>]*?text=\"([^\"]*)\"");
        long end = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < end) {
            try {
                Matcher mm = p.matcher(String.valueOf(driver.getPageSource()));
                if (mm.find()) return mm.group(1).replace("&quot;", "\"").replace("&apos;", "'")
                        .replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&");
            } catch (Exception ignored) { }
            sleep(250);
        }
        return null;
    }
}
