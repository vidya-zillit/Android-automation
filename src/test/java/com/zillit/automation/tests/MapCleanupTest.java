package com.zillit.automation.tests;

import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.MapDialogPage;
import com.zillit.automation.pages.MapLocationListPage;
import com.zillit.automation.pages.MapPage;
import com.zillit.automation.pages.MapTypeManagerPage;
import com.zillit.automation.pages.MapZonePage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * HOUSEKEEPING — removes {@code ZL AUTO} fixtures the Map suites left behind.
 *
 * <p>A suite deletes what it created, but only if it survives to its cleanup case. When a
 * run loses its session midway the fixtures stay on the project, and the next run's counts
 * start from the wrong number. This removes them.
 *
 * <p><b>Opt-in only</b> ({@code ./run-ui.sh --map-cleanup}). It is deliberately NOT part of
 * {@code --map-all} or {@code --all}: it deletes data, and nothing should do that unasked.
 *
 * <p>Scope is the {@code ZL AUTO} prefix and nothing else. Anything a person made on this
 * project is left alone.
 */
@ExtendWith(ReasonLogger.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Order(TestOrder.MAP_CLEANUP)
class MapCleanupTest extends MapBaseTest {

    @Test
    @Order(1)
    @DisplayName("CL-01 Every ZL AUTO location is deleted")
    void deleteLeftoverLocations() {
        assumeTrue(ensureCitySelected(), "No city on this project");
        int removed = 0;
        for (int pass = 0; pass < 12; pass++) {
            ensureToolbarReady();
            MapPage m = map();
            if (!m.openViewAll()) break;
            MapLocationListPage list = new MapLocationListPage(driver);
            if (!list.isLoaded(Duration.ofSeconds(12))) break;

            String target = list.names().stream()
                    .filter(n -> n != null && n.contains(FIXTURE_PREFIX))
                    .findFirst().orElse(null);
            if (target == null) { list.backToMap(); break; }

            System.out.println(">>> deleting location: " + target);
            if (!list.deleteLocation(target)) { list.backToMap(); break; }
            MapDialogPage dlg = new MapDialogPage(driver);
            if (dlg.shown(Duration.ofSeconds(6))) {
                if (!dlg.tap(MapDialogPage.BTN_DELETE)) dlg.tap(MapDialogPage.BTN_YES);
            }
            sleep(3500);
            removed++;
            backToMap();
        }
        System.out.println(">>> locations removed: " + removed);

        ensureToolbarReady();
        MapPage m = map();
        if (m.openViewAll()) {
            MapLocationListPage list = new MapLocationListPage(driver);
            if (list.isLoaded(Duration.ofSeconds(12))) {
                List<String> left = list.names().stream()
                        .filter(n -> n != null && n.contains(FIXTURE_PREFIX)).toList();
                list.backToMap();
                backToMap();
                assertTrue(left.isEmpty(), "Still on the project after cleanup: " + left);
            }
        }
    }

    @Test
    @Order(2)
    @DisplayName("CL-02 Every ZL AUTO studio zone is deleted")
    void deleteLeftoverZones() {
        assumeTrue(ensureCitySelected(), "No city on this project");
        int removed = 0;
        for (int pass = 0; pass < 12; pass++) {
            ensureToolbarReady();
            MapPage m = map();
            if (!m.openZoneList()) break;
            MapZonePage zones = new MapZonePage(driver);
            if (!zones.listLoaded(Duration.ofSeconds(12))) break;

            String target = zones.zoneNames().stream()
                    .filter(n -> n != null && n.contains(FIXTURE_PREFIX))
                    .findFirst().orElse(null);
            if (target == null) { backToMap(); break; }

            System.out.println(">>> deleting zone: " + target);
            if (!zones.deleteZone(target)) { backToMap(); break; }
            MapDialogPage dlg = new MapDialogPage(driver);
            if (dlg.shown(Duration.ofSeconds(6))) {
                if (!dlg.tap(MapDialogPage.BTN_DELETE)) dlg.tap(MapDialogPage.BTN_YES);
            }
            sleep(3500);
            removed++;
            backToMap();
        }
        System.out.println(">>> zones removed: " + removed);
    }

    @Test
    @Order(3)
    @DisplayName("CL-03 Every ZL AUTO location type is deleted")
    void deleteLeftoverTypes() {
        assumeTrue(ensureCitySelected(), "No city on this project");
        int removed = 0;
        for (int pass = 0; pass < 12; pass++) {
            ensureToolbarReady();
            MapPage m = map();
            if (!m.openTypes()) break;
            MapTypeManagerPage types = new MapTypeManagerPage(driver);
            if (!types.isLoaded(Duration.ofSeconds(12))) break;

            String target = types.typeNames().stream()
                    .filter(n -> n != null && n.contains(FIXTURE_PREFIX))
                    .findFirst().orElse(null);
            if (target == null) { backToMap(); break; }

            System.out.println(">>> deleting type: " + target);
            if (!types.deleteType(target)) { backToMap(); break; }
            MapDialogPage dlg = new MapDialogPage(driver);
            if (dlg.shown(Duration.ofSeconds(6))) {
                if (!dlg.tap(MapDialogPage.BTN_DELETE)) dlg.tap(MapDialogPage.BTN_YES);
            }
            sleep(3000);
            removed++;
            backToMap();
        }
        System.out.println(">>> types removed: " + removed);
    }
}
