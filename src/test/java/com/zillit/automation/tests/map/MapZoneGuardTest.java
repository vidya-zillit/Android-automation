package com.zillit.automation.tests.map;

import com.zillit.automation.core.TestOrder;

import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.pages.map.MapDialogPage;
import com.zillit.automation.pages.map.MapPage;
import com.zillit.automation.pages.map.MapZonePage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * MAP &rarr; studio zone guards and zone state — web-gap groups M7 and M8.
 *
 * <p>Zones are created through STREET-INTERSECTION mode, the one centre-point route that
 * works without Places (Geocoding HTTP API — see MapZoneCrudTest Z-07b). Default streets
 * are the ones Z-07b proves for the project's city; override with
 * {@code -Dzillit.zoneStreet1/2}.
 *
 * <p>What the source says ({@code StudioZoneFormFragment.saveZone}, devbuild):
 * <pre>
 *   blank name            → "Please enter a zone name"                       (first)
 *   no centre             → "Please enter both street names for the intersection"
 *   Custom, blank radius  → etCustomRadius.toIntOrNull() ?: 30   — saves 30 mi silently
 *   radius &lt;= 0          → "Radius must be greater than 0"   (no upper bound at all)
 *   city centre outside   → "&lt;City&gt; is X miles from the center point, but zone radius is N…"
 *   btnSave               → never disabled while saving (LocationForm's is)
 * </pre>
 *
 * <p><b>Data safety.</b> Every zone here is named {@code ZL_AUTO …} and recorded; cleanup
 * deletes only recorded names. Zones that belong to the city already (real ones) are read
 * and may be shown on the map, never edited or deleted.
 */
@ExtendWith(ReasonLogger.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Order(TestOrder.MAP_ZONE_GUARDS)
class MapZoneGuardTest extends MapBaseTest {

    private static final String PREFIX = "ZL_AUTO";
    private final String stamp = String.valueOf(System.currentTimeMillis() % 100000);
    private final String s1 = System.getProperty("zillit.zoneStreet1", "Castro Street");
    private final String s2 = System.getProperty("zillit.zoneStreet2", "El Camino Real");

    /** Exact names of zones this run created (a double submit may create two rows). */
    private final List<String> myZones = new ArrayList<>();

    private String zoneA;   // 30 mi, used for Details and as the "active" zone in Z-18

    // ─────────────────────────── guards that never save ───────────────────────────

    @Test
    @Order(1)
    @DisplayName("Z-07 Intersection mode with one street blank resolves nothing and cannot save")
    void intersectionBlankStreet() { // [WEB map.md:838]
        MapZonePage z = openZoneForm();
        assumeTrue(z.chooseIntersectionMode() && z.intersectionFieldsShown(), "Intersection mode not offered");
        assumeTrue(z.setStreet1(s1), "Could not set street 1");
        assumeTrue(z.findIntersection(), "Find Intersection did not respond");
        String t1 = toast(Duration.ofSeconds(3));
        String lat = z.centerLat();
        String result = z.intersectionResult();
        assumeTrue(z.save(), "Save not available");
        String t2 = toast(Duration.ofSeconds(3));
        boolean open = z.formStillOpen();
        z.cancel();
        backToMap();
        assertTrue(lat == null || lat.isBlank() || lat.equalsIgnoreCase("Auto-filled") || firstNumber(lat) == null,
                "A centre was resolved from ONE street: lat=" + lat + ", result=" + result + " (toast: " + t1 + ")");
        assertTrue(open, "Save in intersection mode with no centre closed the form. Toast: " + t2);
        System.out.println(">>> Z-07 toasts: find=\"" + t1 + "\" save=\"" + t2 + "\"");
    }

    @Test
    @Order(2)
    @DisplayName("Z-05b A blank zone name is refused even with a resolved centre")
    void blankNameRefusedWithCentre() { // [WEB map.md:549]
        int before = zoneTotal();
        MapZonePage z = openZoneForm();
        resolveIntersection(z);
        assumeTrue(z.clearZoneName(), "Could not clear the zone name");
        assumeTrue(z.save(), "Save not available");
        String t = toast(Duration.ofSeconds(3));
        boolean open = z.formStillOpen();
        z.cancel();
        backToMap();
        assertTrue(open, "A zone with an empty name was saved (toast: " + t + ")");
        assertEquals(before, zoneTotal(), "The zone count changed after a refused blank-name save");
    }

    // ─────────────────────────── radius guards ───────────────────────────

    @Test
    @Order(3)
    @DisplayName("Z-08b 'Custom' with an empty radius is refused, not saved as 30 miles")
    void customBlankRadiusRefused() { // [WEB map.md:543]
        String name = PREFIX + " Blank " + stamp;
        MapZonePage z = openZoneForm();
        resolveIntersection(z);
        assumeTrue(z.setZoneName(name), "Could not type the zone name");
        assumeTrue(z.chooseCustomRadiusLeftBlank(), "The Custom radius field did not appear");
        assumeTrue(z.save(), "Save not available");
        String t = toast(Duration.ofSeconds(4));
        sleep(2500);
        boolean saved = !z.formStillOpen();
        String radius = null;
        if (saved) {
            myZones.add(name);
            MapZonePage list = openZoneList();
            radius = list.radiusOf(name);
        } else {
            z.cancel();
        }
        backToMap();
        assertFalse(saved, "\"Custom\" with an EMPTY radius saved the zone (" + radius + ", toast \"" + t
                + "\"). StudioZoneFormFragment.saveZone uses etCustomRadius.toIntOrNull() ?: 30, so a blank"
                + " custom radius silently becomes 30 miles — web refuses it (map.md:543). [gap M7]");
    }

    @Test
    @Order(4)
    @DisplayName("Z-08c A small Custom radius saves and the card shows that radius")
    void customSmallRadiusSaves() { // [WEB map.md:555, map.md:848]
        String name = PREFIX + " Custom " + stamp;
        MapZonePage z = openZoneForm();
        resolveIntersection(z);
        assumeTrue(z.setZoneName(name), "Could not type the zone name");
        int miles = 1;
        assumeTrue(z.setCustomRadius("1"), "Custom radius field not available");
        assumeTrue(z.save(), "Save not available");
        String t = toast(Duration.ofSeconds(4));
        sleep(2000);
        if (z.formStillOpen() && (t == null || t.contains(MapZonePage.ERR_CITY_OUTSIDE))) {
            // Android also requires the CITY centre inside the zone; 1 mile from this
            // intersection does not reach it. Use the smallest whole radius that does —
            // still a Custom value, which is what this case is about. (Toasts are not
            // always readable; without one, 7 mi — not a chip value — is used.)
            Double d = t == null ? null : firstNumber(t.replaceAll("^.*? is ", ""));
            miles = d == null ? 7 : (int) Math.ceil(d) + 1;
            System.out.println(">>> Z-08c 1 mi cannot contain the city centre (" + t + "); using " + miles);
            assumeTrue(z.setCustomRadius(String.valueOf(miles)), "Could not retype the radius");
            assumeTrue(String.valueOf(miles).equals(String.valueOf(z.customRadiusValue()).trim()),
                    "The radius field reads \"" + z.customRadiusValue() + "\" after typing " + miles);
            assumeTrue(z.save(), "Save not available");
            t = toast(Duration.ofSeconds(4));
            sleep(2000);
        }
        boolean saved = !z.formStillOpen();
        if (!saved) { z.cancel(); backToMap(); }
        assertTrue(saved, "A " + miles + "-mile Custom zone was refused: " + t);
        myZones.add(name);
        MapZonePage list = openZoneList();
        String radius = list.radiusOf(name);
        backToMap();
        assertEquals(miles + " mi radius", radius, "The zone card does not show the Custom radius it was"
                + " saved with (" + miles + " typed and read back from etCustomRadius before Save). 30 is the"
                + " adapter's fallback for miles <= 0 (StudioZoneAdapter:48), so the stored radius is lost or"
                + " replaced.");
    }

    @Test
    @Order(5)
    @DisplayName("Z-08d A 201-mile radius is refused (web caps the radius at 200)")
    void radiusAbove200Refused() { // [WEB map.md:843]
        String name = PREFIX + " Huge " + stamp;
        MapZonePage z = openZoneForm();
        resolveIntersection(z);
        assumeTrue(z.setZoneName(name), "Could not type the zone name");
        assumeTrue(z.setCustomRadius("201"), "Custom radius field not available");
        assumeTrue(z.save(), "Save not available");
        String t = toast(Duration.ofSeconds(4));
        sleep(2500);
        boolean saved = !z.formStillOpen();
        String radius = null;
        if (saved) { myZones.add(name); radius = openZoneList().radiusOf(name); } else z.cancel();
        backToMap();
        assertFalse(saved, "A 201-mile zone was saved as \"" + radius + "\" (toast \"" + t + "\"). StudioZoneFormFragment.saveZone"
                + " only refuses radius <= 0 — there is no upper bound; web never submits 201 (map.md:843). [gap M7]");
    }

    // ─────────────────────────── double submit ───────────────────────────

    @Test
    @Order(6)
    @DisplayName("Z-11c A double tap on Create Zone creates exactly one zone")
    void doubleTapCreatesOne() { // [WEB map.md:865]
        String name = PREFIX + " Twice " + stamp;
        int before = zoneTotal();
        MapZonePage z = openZoneForm();
        resolveIntersection(z);
        assumeTrue(z.setZoneName(name), "Could not type the zone name");
        z.pickRadiusChip("30 mi");
        assumeTrue(z.doubleTapSave(), "Could not tap Create Zone");
        sleep(5000);
        if (z.formStillOpen()) { z.cancel(); }
        MapZonePage list = openZoneList();
        int rows = list.rowsNamed(name);
        for (int i = 0; i < Math.max(rows, 1); i++) myZones.add(name);
        int after = list.totalFromHeader();
        backToMap();
        assertTrue(rows >= 1, "The double-tapped zone was not created at all");
        assertEquals(1, rows, "A double tap on Create Zone created " + rows + " zones named \"" + name
                + "\" (zones " + before + " → " + after + "). btnSave is never disabled while saving"
                + " (StudioZoneFormFragment:299, 556-599). [gap M7]");
    }

    @Test
    @Order(65)
    @DisplayName("Z-19b Two zones may share a name: the list matches the save outcome")
    void duplicateZoneNames() { // [WEB map.md:1200]
        String name = PREFIX + " Twin " + stamp;
        assumeTrue(createZone(name, "30 mi"), "Could not create the first " + name);
        MapZonePage z = openZoneForm();
        resolveIntersection(z);
        assumeTrue(z.setZoneName(name), "Could not type the zone name");
        z.pickRadiusChip("30 mi");
        assumeTrue(z.save(), "Save not available");
        sleep(4000);
        boolean accepted = !z.formStillOpen();
        if (accepted) myZones.add(name); else { z.cancel(); }
        System.out.println(">>> Z-19b duplicate zone name accepted=" + accepted);
        MapZonePage list = openZoneList();
        int rows = list.rowsNamed(name);
        backToMap();
        assertEquals(accepted ? 2 : 1, rows, "The zone list disagrees with the save outcome (accepted=" + accepted + ")");
    }

    // ─────────────────────────── M8 details, selection, fallback ───────────────────────────

    @Test
    @Order(7)
    @DisplayName("Z-15 A zone's View opens 'Studio Zone Details' with radius and coordinates; OK closes it")
    void zoneDetails() { // [WEB map.md:579]
        zoneA = PREFIX + " ZoneA " + stamp;
        assumeTrue(createZone(zoneA, "30 mi"), "Could not create " + zoneA);
        MapZonePage list = openZoneList();
        assumeTrue(list.tapRowByBand(zoneA, "btnView"), "View not offered on " + zoneA);
        MapDialogPage dlg = new MapDialogPage(driver);
        assertTrue(dlg.shown(Duration.ofSeconds(6)), "View raised no dialog");
        String title = String.valueOf(dlg.title());
        String msg = String.valueOf(dlg.message());
        dlg.tap("OK");
        sleep(1000);
        boolean closed = !dlg.shown(Duration.ofSeconds(2));
        backToMap();
        assertEquals("Studio Zone Details", title, "Details dialog title");
        assertTrue(msg.contains("Name: " + zoneA), "Details do not name the zone: " + msg);
        assertTrue(msg.contains("Radius: 30 miles"), "Details do not show the 30-mile radius: " + msg);
        assertTrue(Pattern.compile("Coordinates: -?\\d+\\.\\d{6}, -?\\d+\\.\\d{6}").matcher(msg).find(),
                "Details do not show coordinates: " + msg);
        assertTrue(closed, "OK did not close the details dialog");
    }

    @Test
    @Order(8)
    @DisplayName("Z-13b Opening the Map on a city with zones names a zone on the zone button")
    void reopenShowsActiveZone() { // [WEB map.md:537]
        assumeTrue(zoneTotal() > 0, "This city has no zones");
        leaveMapToTools();
        openMap();
        String label = map().toolbarLabel("btnZoneList");
        assertTrue(label != null && !label.isBlank() && !label.equals("Studio Zone"),
                "The city has zones but the zone button reads \"" + label + "\" — the first zone should be"
                        + " active on open (MapViewModel: setActiveZone(it.first()))");
    }

    @Test
    @Order(9)
    @DisplayName("Z-18 Deleting the active zone moves the map off it")
    void deletingActiveZoneFallsBack() { // [WEB map.md:779, map.md:1121]
        assumeTrue(zoneA != null && myZones.contains(zoneA), "Z-15 did not create " + zoneA);
        MapZonePage list = openZoneList();
        assumeTrue(list.tapRowByBand(zoneA, "btnSelect"), "Show on Map not offered on " + zoneA);
        sleep(2500);
        backToMap();
        assumeTrue(zoneA.equals(map().toolbarLabel("btnZoneList")),
                "Selecting " + zoneA + " did not make it the active zone: " + map().toolbarLabel("btnZoneList"));

        list = openZoneList();
        assumeTrue(list.tapRowByBand(zoneA, "btnDelete"), "Delete not offered on " + zoneA);
        MapDialogPage dlg = new MapDialogPage(driver);
        assumeTrue(dlg.shown(Duration.ofSeconds(6)) && dlg.tap(MapDialogPage.BTN_DELETE), "Delete confirmation failed");
        sleep(3500);
        boolean gone = list.rowsNamed(zoneA) == 0;
        if (gone) myZones.remove(zoneA);
        backToMap();
        // The map hears about the delete through studioZonesChanged and refetches; give it
        // up to 20 s before calling the label stale.
        String label = map().toolbarLabel("btnZoneList");
        for (int i = 0; i < 10 && zoneA.equals(label); i++) { sleep(2000); label = map().toolbarLabel("btnZoneList"); }
        String afterReopen = null;
        if (zoneA.equals(label)) {
            leaveMapToTools();
            openMap();
            afterReopen = map().toolbarLabel("btnZoneList");
        }
        assertTrue(gone, zoneA + " survived a confirmed delete");
        assertNotEquals(zoneA, label, "20 s after deleting the active zone the zone button still names it"
                + (afterReopen == null ? "" : "; after leaving and reopening the Map it reads \"" + afterReopen + "\"")
                + ". MapViewModel.syncActiveZoneWithFreshList should clear it on the refetch.");
        int left = zoneTotal();
        if (left > 0) {
            assertNotEquals("Studio Zone", label,
                    "Zones remain (" + left + ") but none became active after deleting the active one");
        }
    }

    // ─────────────────────────── cleanup ───────────────────────────

    @Test
    @Order(99)
    @DisplayName("ZG-99 Cleanup: only the ZL_AUTO zones this run created are deleted")
    void cleanup() {
        List<String> left = new ArrayList<>();
        for (String name : new ArrayList<>(myZones)) {
            if (!myZones.contains(name)) continue;    // a duplicate already handled
            if (!name.startsWith(PREFIX)) { left.add(name + " (not ZL_AUTO — refused)"); continue; }
            for (int pass = 0; pass < 4; pass++) {
                MapZonePage list = openZoneList();
                list.searchZones(name);
                sleep(1000);
                if (!list.zoneNames().contains(name)) break;
                if (!list.tapRowByBand(name, "btnDelete")) break;
                MapDialogPage dlg = new MapDialogPage(driver);
                if (dlg.shown(Duration.ofSeconds(6))) dlg.tap(MapDialogPage.BTN_DELETE);
                sleep(3000);
                backToMap();
            }
            MapZonePage list = openZoneList();
            if (list.rowsNamed(name) == 0) myZones.removeIf(name::equals); else left.add(name);
            backToMap();
        }
        assertTrue(left.isEmpty(), "Zones this run created are still on the city: " + left);
    }

    // ─────────────────────────── plumbing ───────────────────────────

    private MapZonePage openZoneList() {
        backToMap();
        ensureToolbarReady();
        assumeTrue(ensureCitySelected(), "Studio zones are city-scoped and no city could be selected");
        MapPage m = map();
        assumeTrue(m.openZoneList(), "Zone list button not available");
        MapZonePage zones = new MapZonePage(driver);
        assumeTrue(zones.listLoaded(Duration.ofSeconds(15)), "Zone list did not open");
        sleep(1200);
        return zones;
    }

    private MapZonePage openZoneForm() {
        MapZonePage zones = openZoneList();
        assumeTrue(zones.openAddZone(), "Add-zone button not available (no posting rights?)");
        assumeTrue(zones.formLoaded(Duration.ofSeconds(12)), "Zone form did not open");
        return zones;
    }

    /** Sets the centre through the intersection finder; skips the case if geocoding fails. */
    private void resolveIntersection(MapZonePage z) {
        assumeTrue(z.chooseIntersectionMode() && z.intersectionFieldsShown(), "Intersection mode not offered");
        assumeTrue(z.setStreet1(s1) && z.setStreet2(s2), "Could not set the street names");
        assumeTrue(z.findIntersection(), "Find Intersection did not return within 25s");
        assumeTrue(firstNumber(z.centerLat()) != null,
                "Geocoding returned no intersection for " + s1 + " × " + s2 + " (see Z-07b)");
    }

    private boolean createZone(String name, String chip) {
        MapZonePage z = openZoneForm();
        resolveIntersection(z);
        if (!z.setZoneName(name)) return false;
        z.pickRadiusChip(chip);
        if (!z.save()) return false;
        sleep(4000);
        if (z.formStillOpen()) { z.cancel(); backToMap(); return false; }
        myZones.add(name);
        backToMap();
        return true;
    }

    /** "N zones" from the zone list header. */
    private int zoneTotal() {
        MapZonePage list = openZoneList();
        int n = list.totalFromHeader();
        backToMap();
        return n;
    }

    private void leaveMapToTools() {
        for (int i = 0; i < 5 && !onBottomNavScreen(); i++) {
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(1000);
        }
    }

    private static Double firstNumber(String s) {
        if (s == null) return null;
        Matcher m = Pattern.compile("-?\\d+(\\.\\d+)?").matcher(s);
        return m.find() ? Double.valueOf(m.group()) : null;
    }

    private String toast(Duration timeout) {
        Pattern p = Pattern.compile("<android\\.widget\\.Toast[^>]*?text=\"([^\"]*)\"");
        long end = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < end) {
            try {
                Matcher mm = p.matcher(String.valueOf(driver.getPageSource()));
                if (mm.find()) return mm.group(1).replace("&quot;", "\"").replace("&apos;", "'")
                        .replace("&amp;", "&");
            } catch (Exception ignored) { }
            sleep(250);
        }
        return null;
    }
}
