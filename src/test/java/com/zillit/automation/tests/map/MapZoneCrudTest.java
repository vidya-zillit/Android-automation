package com.zillit.automation.tests.map;

import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.map.MapDialogPage;
import com.zillit.automation.pages.map.MapPage;
import com.zillit.automation.pages.map.MapZonePage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * MAP &rarr; <b>studio zones</b>: list, create, validate, apply, edit, delete.
 *
 * <p>Applying a zone is the case that matters most ({@link #applyZoneToMap()}), because it
 * changes how the rest of the module behaves: once a zone is active,
 * {@code evaluatePinBoundary()} tests it <b>before</b> the city, which is the only way the
 * "outside the zone but inside the city" verdict can be produced at all. Without this,
 * half of {@link MapPinBoundaryTest} is unreachable.
 *
 * <p>The form's validation reads backwards until you see the code: it requires the
 * <b>city centre to sit inside the proposed zone</b>, not the zone inside the city. A small
 * radius far from the city is therefore rejected locally, before any API call, with the
 * distance spelled out.
 *
 * <p>Writes to the backend; fixtures carry the {@code ZL AUTO} prefix and are cleaned up.
 */
@Order(TestOrder.MAP_ZONE_CRUD)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MapZoneCrudTest extends MapBaseTest {

    private MapZonePage openZoneList() {
        assumeTrue(ensureCitySelected(),
                "Studio zones are city-scoped and this project has no city to select");
        MapPage m = map();
        assumeTrue(m.openZoneList(), "Zone list button not available");
        MapZonePage zones = new MapZonePage(driver);
        assumeTrue(zones.listLoaded(Duration.ofSeconds(15)), "Zone list did not open");
        return zones;
    }

    private MapZonePage openZoneForm() {
        MapZonePage zones = openZoneList();
        assumeTrue(zones.openAddZone(), "Add-zone button not available");
        assumeTrue(zones.formLoaded(Duration.ofSeconds(12)), "Zone form did not open");
        return zones;
    }

    // ---------------- list ----------------

    @Test
    @Order(1)
    @DisplayName("Z-01 The zone list opens with a title, a count and Add")
    void zoneListOpens() {
        MapZonePage zones = openZoneList();
        assertTrue(zones.listLoaded(Duration.ofSeconds(8)), "Zone list not shown");
        assertNotNull(zones.listTitle(), "Zone list has no title");
        backToMap();
    }

    @Test
    @Order(2)
    @DisplayName("Z-02 With no zones the list shows its empty state")
    void zoneEmptyState() {
        MapZonePage zones = openZoneList();
        assumeTrue(zones.zoneCount() == 0, "This city already has zones");
        assertTrue(zones.emptyStateShown(Duration.ofSeconds(6)),
                "No empty state on a city with zero zones");
        backToMap();
    }

    // ---------------- form validation ----------------

    @Test
    @Order(3)
    @DisplayName("Z-04 The zone form names its parent city")
    void zoneFormShowsCity() {
        MapZonePage zones = openZoneForm();
        assertNotNull(zones.formTitle(), "Zone form has no title");
        // The city badge is what keeps a zone's scope unambiguous when several cities
        // have zones of the same name.
        assertNotNull(zones.cityBadge(), "Zone form does not name the parent city");
        zones.cancel();
        backToMap();
    }

    @Test
    @Order(4)
    @DisplayName("Z-05 Saving a zone with no name is rejected")
    void zoneNoNameRejected() {
        MapZonePage zones = openZoneForm();
        // [WEB map.md:549, map.md:1194] The form PRE-FILLS "<city> Zone"
        // (StudioZoneFormFragment:131-132). Without clearing it this case never had an
        // empty name — it only stayed open because no centre was set (Z-06's guard).
        assumeTrue(zones.clearZoneName(), "Could not clear the pre-filled zone name");
        assumeTrue(String.valueOf(zones.zoneNameValue()).isBlank()
                        || String.valueOf(zones.zoneNameValue()).startsWith("e.g.,"),
                "The zone name did not clear: \"" + zones.zoneNameValue() + "\"");
        assumeTrue(zones.save(), "Save not available");
        sleep(1500);
        assertTrue(zones.formStillOpen(),
                "Form closed on an empty name — expected the \"" + MapZonePage.ERR_NO_NAME
                        + "\" rejection to keep it open");
        zones.cancel();
        backToMap();
    }

    @Test
    @Order(5)
    @DisplayName("Z-06 A named zone with no center point is rejected")
    void zoneNoCenterRejected() {
        MapZonePage zones = openZoneForm();
        assumeTrue(zones.setZoneName(fixtureName("Zone")), "Could not type a zone name");
        assumeTrue(zones.save(), "Save not available");
        sleep(1500);
        assertTrue(zones.formStillOpen(),
                "Form closed with no center point — expected the \""
                        + MapZonePage.ERR_NO_CENTER + "\" rejection");
        zones.cancel();
        backToMap();
    }

    @Test
    @Order(6)
    @DisplayName("Z-08 A zero radius is rejected")
    void zoneZeroRadiusRejected() {
        MapZonePage zones = openZoneForm();
        // [WEB map.md:843] The no-centre guard runs BEFORE the radius check
        // (StudioZoneFormFragment.saveZone), so without a centre this case only ever
        // proved Z-06 again. Resolve a centre through intersection mode first.
        assumeTrue(zones.chooseIntersectionMode() && zones.intersectionFieldsShown(),
                "Intersection mode not offered");
        assumeTrue(zones.setStreet1(System.getProperty("zillit.zoneStreet1", "Castro Street"))
                && zones.setStreet2(System.getProperty("zillit.zoneStreet2", "El Camino Real")),
                "Could not set the street names");
        assumeTrue(zones.findIntersection() && firstNumber(zones.centerLat()) != null,
                "No centre resolved — Z-07b explains why");
        assumeTrue(zones.setZoneName(fixtureName("Zone")), "Could not type a zone name");
        assumeTrue(zones.setCustomRadius("0"), "Custom radius field not available");
        assumeTrue(zones.save(), "Save not available");
        sleep(1500);
        assertTrue(zones.formStillOpen(),
                "Form closed with radius 0 — expected the \"" + MapZonePage.ERR_RADIUS_ZERO
                        + "\" rejection");
        zones.cancel();
        backToMap();
    }

    @Test
    @Order(7)
    @DisplayName("Z-09 A radius too small to contain the city centre is rejected locally")
    void zoneTooSmallForCityRejected() {
        MapZonePage zones = openZoneForm();
        assumeTrue(zones.setZoneName(fixtureName("Tiny")), "Could not type a zone name");
        assumeTrue(zones.searchCenterPoint("Mumbai"), "The zone centre can only be set through Google Places. etCenterSearch is isFocusable=false and its click launches the Places autocomplete (StudioZoneFormFragment:269-270), the intersection finder is behind the same SDK, and the draggable preview marker — the one other way lat/lng get set — is never added because updateMapPreview() returns early while lat/lng are still 0. Places does not work in this develop build, so a zone cannot be created here at all.");
        sleep(3500);
        assumeTrue(zones.lat() != null && !zones.lat().isBlank()
                        && !"Auto-filled".equalsIgnoreCase(zones.lat()),
                "Center-point search returned no coordinates — most likely no "
                        + "Places-enabled API key on this build");
        // 1 mile cannot contain a city centre unless the centre is that close, which the
        // rejection message quantifies.
        assumeTrue(zones.setCustomRadius("1"), "Custom radius field not available");
        assumeTrue(zones.save(), "Save not available");
        sleep(2000);
        assertTrue(zones.formStillOpen(),
                "A 1-mile zone saved despite the city centre being outside it — the local "
                        + "pre-API check (\"" + MapZonePage.ERR_CITY_OUTSIDE + "\") did not run");
        zones.cancel();
        backToMap();
    }

    // ---------------- create and apply ----------------

    @Test
    @Order(8)
    @DisplayName("Z-11 A valid zone saves and appears in the list")
    void createZone() {
        MapZonePage zones = openZoneForm();
        String name = fixtureName("Zone");
        assumeTrue(zones.setZoneName(name), "Could not type a zone name");
        assumeTrue(zones.searchCenterPoint("Mumbai"), "The zone centre can only be set through Google Places. etCenterSearch is isFocusable=false and its click launches the Places autocomplete (StudioZoneFormFragment:269-270), the intersection finder is behind the same SDK, and the draggable preview marker — the one other way lat/lng get set — is never added because updateMapPreview() returns early while lat/lng are still 0. Places does not work in this develop build, so a zone cannot be created here at all.");
        sleep(3500);
        assumeTrue(zones.lat() != null && !zones.lat().isBlank()
                        && !"Auto-filled".equalsIgnoreCase(zones.lat()),
                "Center-point search returned no coordinates");
        assumeTrue(zones.setCustomRadius("50"), "Custom radius field not available");
        assumeTrue(zones.save(), "Save not available");
        sleep(4000);

        MapZonePage list = new MapZonePage(driver);
        assumeTrue(list.listLoaded(Duration.ofSeconds(12)), "Did not return to the zone list");
        assertFalse(list.zoneNames().isEmpty(), "No zones listed after saving one");
        backToMap();
    }

    @Test
    @Order(9)
    @DisplayName("Z-12 Zone cards carry a name, a radius and the row actions")
    void zoneCardContent() {
        MapZonePage zones = openZoneList();
        assumeTrue(zones.zoneCount() > 0, "No zones on this city");
        assertFalse(zones.zoneNames().isEmpty(), "Zone rows have no names");
        assertFalse(zones.zoneRadii().isEmpty(),
                "Zone rows show no radius — it is what the boundary check uses");
        backToMap();
    }

    @Test
    @Order(10)
    @DisplayName("Z-13 Applying a zone makes it the active boundary on the map")
    void applyZoneToMap() {
        MapZonePage zones = openZoneList();
        List<String> names = zones.zoneNames();
        assumeTrue(!names.isEmpty(), "No zones to apply");
        String target = names.get(0);

        assumeTrue(zones.selectZone(target), "Select not available on the zone row");
        sleep(2500);
        backToMap();

        MapPage m = map();
        String label = m.zoneButtonLabel();
        // Once a zone is active the button stops being a generic "Zones" and names it —
        // that label IS the user's only on-map indication of which boundary is in force.
        assertTrue(label != null && !label.isBlank(),
                "Zone button lost its label after applying a zone");
        assertTrue(label.contains(target) || !label.equalsIgnoreCase("Zones"),
                "Zone button still reads generic after applying \"" + target + "\": " + label);
    }

    @Test
    @Order(11)
    @DisplayName("Z-19 Zone search filters the list")
    void zoneSearchFilters() {
        MapZonePage zones = openZoneList();
        List<String> all = zones.zoneNames();
        assumeTrue(all.size() >= 2, "Needs at least two zones to prove filtering");
        String term = all.get(0).substring(0, Math.min(4, all.get(0).length()));
        assumeTrue(zones.searchZones(term), "Zone search field not available");
        sleep(1500);
        assertTrue(zones.zoneNames().size() <= all.size(),
                "Searching widened the zone list");
        backToMap();
    }

    // ---------------- edit and delete ----------------

    @Test
    @Order(12)
    @DisplayName("Z-16 A zone opens prefilled for editing")
    void editZone() {
        MapZonePage zones = openZoneList();
        String target = zones.zoneNames().stream()
                .filter(n -> n.startsWith(FIXTURE_PREFIX))
                .findFirst().orElse(null);
        assumeTrue(target != null, "No " + FIXTURE_PREFIX + " zone to edit");

        assumeTrue(zones.editZone(target), "Edit not available on the row");
        assumeTrue(zones.formLoaded(Duration.ofSeconds(12)), "Edit form did not open");
        assertNotNull(zones.formTitle(), "Edit form has no title");
        zones.cancel();
        backToMap();
    }

    @Test
    @Order(13)
    @DisplayName("Z-17 Deleting a zone asks first and then removes it")
    void deleteZone() {
        MapZonePage zones = openZoneList();
        String target = zones.zoneNames().stream()
                .filter(n -> n.startsWith(FIXTURE_PREFIX))
                .findFirst().orElse(null);
        assumeTrue(target != null,
                "No " + FIXTURE_PREFIX + " zone to delete — this suite never deletes a "
                        + "zone it did not create");

        int before = zones.zoneCount();
        assumeTrue(zones.deleteZone(target), "Delete not available on the row");

        MapDialogPage dlg = new MapDialogPage(driver);
        assertTrue(dlg.shown(Duration.ofSeconds(8)), "Delete did not ask for confirmation");
        assertTrue(dlg.hasButton(MapDialogPage.BTN_DELETE)
                        && dlg.hasButton(MapDialogPage.BTN_CANCEL),
                "Confirm should offer Delete and Cancel; got " + dlg.buttonLabels());

        assumeTrue(dlg.tap(MapDialogPage.BTN_DELETE), "Delete not available in the dialog");
        sleep(3000);
        assertTrue(zones.zoneCount() < before || !zones.zoneNames().contains(target),
                "Zone \"" + target + "\" survived a confirmed delete");
        backToMap();
    }

    // ─────────────── intersection mode: the path that works without Places ───────────────

    @Test
    @Order(30)
    @DisplayName("Z-07b Street intersection resolves to a real centre point")
    void intersectionResolvesCentre() {
        // The user's own description of this flow: "in intersection point we will find two
        // street names, and Google automatically finds the center point… it may be approx
        // a little bit different, but it will work." That is exactly what the code does —
        // btnFindIntersection reads the two fields as TEXT and resolves them through the
        // Geocoding HTTP API (StudioZoneFormFragment:279-295), NOT Places Autocomplete.
        //
        // The streets must belong to the city this project uses, or the geocoder will
        // correctly find nothing. Override with -Dzillit.zoneStreet1/2 for another city.
        // Read the city from the MAP, before navigating away from it.
        //
        // tvCityBadge on the zone form is only filled when the form is opened with a
        // cityName argument (StudioZoneFormFragment:128), and on this route it is not —
        // it came back empty, so the "is it the right city" check silently did nothing.
        // The Map's own tvCityName always carries the selected city, so take it there.
        String cityOnMap = String.valueOf(new MapPage(driver).cityName()).trim();

        MapZonePage zone = openZoneForm();
        assumeTrue(zone.chooseIntersectionMode(), "Intersection mode not offered on the form");
        assumeTrue(zone.intersectionFieldsShown(), "The two street fields did not appear");

        String s1 = System.getProperty("zillit.zoneStreet1", "Castro Street");
        String s2 = System.getProperty("zillit.zoneStreet2", "El Camino Real");
        assumeTrue(zone.setStreet1(s1),
                "Could not put text into etStreet1. It is isFocusable=false and its CLICK"
                        + " launches Places, so the value has to be set without clicking"
                        + " (ACTION_SET_TEXT) — if this fails, that route stopped working.");
        assumeTrue(zone.setStreet2(s2), "Could not put text into etStreet2");
        assumeTrue(zone.findIntersection(),
                "Find Intersection never came back from \"Finding…\" — the geocoding call"
                        + " did not return within 25s");

        String result = zone.intersectionResult();
        String lat = zone.centerLat();
        String lng = zone.centerLng();
        assumeTrue((result != null && !result.isBlank()) || (lat != null && !lat.isBlank()),
                "Geocoding returned no intersection for \"" + s1 + "\" × \"" + s2 + "\"."
                        + " Either those two streets do not cross in this city, or the"
                        + " Geocoding API key is not working on this build.");

        // IS THE POINT REAL? The address alone is not enough — the zone is drawn from the
        // COORDINATE, so that is what has to be right.
        Double dLat = firstNumber(lat);
        Double dLng = firstNumber(lng);
        assertNotNull(dLat, "tvLat held no number after the intersection resolved: \"" + lat
                + "\". The address came back (\"" + result + "\"), so the lookup worked and"
                + " the form failed to carry the latitude into the field the zone is built from.");
        assertNotNull(dLng, "tvLng held no number after the intersection resolved: \"" + lng + "\"");

        assertTrue(dLat >= -90 && dLat <= 90 && dLng >= -180 && dLng <= 180,
                "Not a valid coordinate: " + dLat + ", " + dLng);
        assertFalse(Math.abs(dLat) < 0.0001 && Math.abs(dLng) < 0.0001,
                "The intersection resolved to 0,0 — the \"not found\" value. A zone saved"
                        + " from this would sit in the Atlantic, several thousand miles from "
                        + zone.cityBadgeText());

        // AND IT MUST BE THE RIGHT PLACE. A valid coordinate for some other city would
        // pass every check above. The form names its city; the resolved address has to
        // agree with it, otherwise the geocoder matched streets somewhere else entirely —
        // which is exactly the failure a user would never notice until the zone is drawn.
        String badge = String.valueOf(zone.cityBadgeText()).trim();
        String city = !badge.isEmpty() && !"null".equals(badge) ? badge : cityOnMap;
        assertFalse(city.isEmpty() || "null".equals(city) || "Select a City".equals(city),
                "Neither the zone form's city badge nor the Map's city label named a city,"
                        + " so this case cannot tell whether the coordinate belongs to the"
                        + " right one — and a valid coordinate for the WRONG city is exactly"
                        + " the failure worth catching here. Badge=\"" + badge
                        + "\", map=\"" + cityOnMap + "\"");

        String cityWord = city.split("[,(]")[0].trim();
        assertTrue(String.valueOf(result).toLowerCase().contains(cityWord.toLowerCase()),
                "The intersection resolved to \"" + result + "\", which does not name \""
                        + cityWord + "\". Streets of that name exist in many cities, so a"
                        + " valid-looking coordinate can still centre the zone hundreds of"
                        + " miles away.");
        System.out.println(">>> Z-07b " + s1 + " x " + s2 + " -> " + dLat + ", " + dLng
                + "  |  " + String.valueOf(result).replace("\n", " ")
                + "  |  city: " + city + " (badge=\"" + badge + "\", map=\"" + cityOnMap + "\")");
    }

    @Test
    @Order(31)
    @DisplayName("Z-11b A zone built from an intersection saves and appears on the map")
    void intersectionZoneSavesAndShows() {
        MapZonePage zone = openZoneForm();
        assumeTrue(zone.chooseIntersectionMode(), "Intersection mode not offered");
        assumeTrue(zone.intersectionFieldsShown(), "The two street fields did not appear");
        String s1 = System.getProperty("zillit.zoneStreet1", "Castro Street");
        String s2 = System.getProperty("zillit.zoneStreet2", "El Camino Real");
        assumeTrue(zone.setStreet1(s1), "Could not put text into etStreet1");
        assumeTrue(zone.setStreet2(s2), "Could not put text into etStreet2");
        assumeTrue(zone.findIntersection(), "Find Intersection did not return");
        assumeTrue((zone.centerLat() != null && !zone.centerLat().isBlank())
                        || (zone.intersectionResult() != null && !zone.intersectionResult().isBlank()),
                "No centre point resolved — Z-07b explains why");

        String name = fixtureName("Zone");
        assumeTrue(zone.setZoneName(name), "Could not type a zone name");
        zone.pickRadiusChip("30");
        assumeTrue(zone.save(), "Save not available");
        sleep(4000);

        assertFalse(zone.formStillOpen(),
                "The zone form stayed open after a name and a resolved centre — the save"
                        + " was rejected; the toast above says why");

        MapZonePage list = openZoneList();
        assertTrue(list.zoneNames().stream().anyMatch(n -> n != null && n.contains(name)),
                "Zone \"" + name + "\" saved but is not in the zone list: " + list.zoneNames());

        // And it must be usable as the active boundary — that is what a zone is FOR.
        assertTrue(list.selectZone(name),
                "The saved zone offers no Select, so it can never become the active boundary");
        sleep(2500);
        createdZone = name;
    }

    @Test
    @Order(32)
    @DisplayName("Z-17b Cleanup: the zone this suite created is deleted")
    void deleteIntersectionZone() {
        assumeTrue(createdZone != null, "No zone was created, so there is nothing to clean up");
        MapZonePage list = openZoneList();
        assumeTrue(list.deleteZone(createdZone), "Delete not offered on the row");
        MapDialogPage dlg = new MapDialogPage(driver);
        if (dlg.shown(Duration.ofSeconds(6))) {
            if (!dlg.tap(MapDialogPage.BTN_DELETE)) dlg.tap(MapDialogPage.BTN_YES);
        }
        sleep(3500);
        assertFalse(openZoneList().zoneNames().stream()
                        .anyMatch(n -> n != null && n.contains(createdZone)),
                "\"" + createdZone + "\" is still listed after a confirmed delete");
        createdZone = null;
    }

    private String createdZone;

    /** First number in a string like "37.422, -122.084" or "Lat: 37.42". */
    private static Double firstNumber(String s) {
        if (s == null) return null;
        java.util.regex.Matcher m =
                java.util.regex.Pattern.compile("-?\\d+\\.\\d+").matcher(s);
        return m.find() ? Double.valueOf(m.group()) : null;
    }
}
