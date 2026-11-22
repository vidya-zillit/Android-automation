package com.zillit.automation.tests.map;

import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.map.MapPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * MAP &rarr; <b>directions and share</b>.
 *
 * <p>Both leave the Map's own surface — directions opens an in-screen panel, share hands
 * off to the system chooser — so the recurring risk is not that they fail but that they
 * strand the run <i>outside the app</i>, after which every later suite skips. Each case
 * here asserts the return path as firmly as the feature.
 *
 * <p>The share text is built in two registers, and the difference is deliberate: a Map pin
 * gets a compact block, while a Location-tool overlay pin gets the fuller one with
 * "Shoot Location", Episode No and the spelled-out stage, because its recipient is usually
 * outside the Location tool where a bare "Final" says nothing. The coordinates are
 * formatted to six places under {@code Locale.US} so a comma-decimal locale cannot corrupt
 * the maps link — a case worth keeping even though it rarely moves.
 */
@Order(TestOrder.MAP_DIRECTIONS_SHARE)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MapDirectionsShareTest extends MapBaseTest {

    /** Opens the bottom panel on a city that has at least one pin, or skips. */
    private MapPage panelWithPin() {
        assumeTrue(ensureCitySelected(), "Needs a selected city");
        MapPage m = map();
        assumeTrue(m.locationCount() > 0, "This city has no pins to act on");
        assumeTrue(m.bottomPanelShown(Duration.ofSeconds(8))
                        || m.carouselShown(Duration.ofSeconds(4)),
                "Bottom panel not open — tap a marker first");
        return m;
    }

    /** Brings the app back to the foreground after a hand-off to another package. */
    private void returnToApp() {
        try {
            String pkg = driver.getCurrentPackage();
            if (pkg == null || !pkg.contains("zillit")) {
                try { driver.navigate().back(); } catch (Exception ignored) { }
                sleep(1500);
                pkg = driver.getCurrentPackage();
                if (pkg == null || !pkg.contains("zillit")) {
                    driver.executeScript("mobile: activateApp",
                            java.util.Map.of("appId", appPackage));
                    sleep(2500);
                }
            }
        } catch (Exception ignored) { }
        backToMap();
    }

    // ---------------- directions ----------------

    @Test
    @Order(1)
    @DisplayName("D-01/D-02 Directions opens a panel with the destination prefilled")
    void directionsOpens() {
        MapPage m = panelWithPin();
        assumeTrue(m.cardDirections(), "Directions not available on the card");
        sleep(2500);
        assertTrue(m.directionsPanelShown(Duration.ofSeconds(10)),
                "Directions panel did not open");
        String to = m.directionsTo();
        assertTrue(to != null && !to.isBlank(),
                "Destination field is empty — it should carry the pin's name");
        m.closeDirections();
        backToMap();
    }

    @Test
    @Order(2)
    @DisplayName("D-07 Distance reads in both km and miles")
    void distanceFormat() {
        MapPage m = panelWithPin();
        assumeTrue(m.cardDirections(), "Directions not available on the card");
        assumeTrue(m.directionsPanelShown(Duration.ofSeconds(10)), "Panel did not open");
        // Needs an origin before a route exists; without location permission there is none.
        m.useMyLocation();
        sleep(4000);
        String dist = m.directionsDistance();
        assumeTrue(dist != null && !dist.isBlank(),
                "No distance computed — most likely no location fix on this device");
        assertTrue(dist.contains("km") && dist.contains("mi"),
                "Distance should read \"N km (M mi)\"; got: " + dist);
        m.closeDirections();
        backToMap();
    }

    @Test
    @Order(3)
    @DisplayName("D-11 Closing directions returns to the Map intact")
    void directionsClose() {
        MapPage m = panelWithPin();
        assumeTrue(m.cardDirections(), "Directions not available on the card");
        assumeTrue(m.directionsPanelShown(Duration.ofSeconds(10)), "Panel did not open");
        assumeTrue(m.closeDirections(), "Close button not available");
        sleep(1500);
        assertTrue(m.isLoaded(Duration.ofSeconds(10)),
                "Map not intact after closing directions");
        assertFalse(m.directionsPanelShown(Duration.ofSeconds(3)),
                "Directions panel stayed open after Close");
    }

    @Test
    @Order(4)
    @DisplayName("D-08 Share directions hands off and the Map survives the return")
    void shareDirections() {
        MapPage m = panelWithPin();
        assumeTrue(m.cardDirections(), "Directions not available on the card");
        assumeTrue(m.directionsPanelShown(Duration.ofSeconds(10)), "Panel did not open");
        assumeTrue(m.shareDirections(), "Share-directions button not available");
        sleep(3000);
        // Whether a chooser appears depends on what can handle text/plain on this device;
        // what must hold either way is that we can get back.
        returnToApp();
        assertTrue(new MapPage(driver).isLoaded(Duration.ofSeconds(15)),
                "Could not return to the Map after sharing directions");
    }

    // ---------------- share a pin ----------------

    @Test
    @Order(5)
    @DisplayName("S-01 Sharing a pin opens the system chooser")
    void sharePin() {
        MapPage m = panelWithPin();
        assumeTrue(m.cardShare(), "Share not available on the card");
        sleep(3000);
        boolean handedOff = m.shareSheetShown(Duration.ofSeconds(8));
        returnToApp();
        assumeTrue(handedOff,
                "No share target on this device for text/plain, so no chooser appeared");
        assertTrue(new MapPage(driver).isLoaded(Duration.ofSeconds(15)),
                "Could not return to the Map after sharing a pin");
    }

    @Test
    @Order(6)
    @DisplayName("S-01b The Map survives a share round trip without losing its city")
    void shareKeepsCity() {
        MapPage m = panelWithPin();
        String cityBefore = m.cityName();
        assumeTrue(m.cardShare(), "Share not available on the card");
        sleep(3000);
        returnToApp();
        MapPage after = new MapPage(driver);
        assumeTrue(after.isLoaded(Duration.ofSeconds(15)), "Map not restored");
        assertTrue(cityBefore == null || cityBefore.equals(after.cityName()),
                "City changed across a share round trip: " + cityBefore
                        + " → " + after.cityName());
    }

    // ---------------- places result card ----------------

    @Test
    @Order(7)
    @DisplayName("F-05 The places result card shows name, address and coordinates")
    void placesResultCard() {
        MapPage m = map();
        assumeTrue(m.placesResultShown(Duration.ofSeconds(3)),
                "No places result card on screen — it appears after picking a Places hit");
        assertTrue(m.placeName() != null && !m.placeName().isBlank(),
                "Places card shows no name");
        assertTrue(m.placeCoords() != null && !m.placeCoords().isBlank(),
                "Places card shows no coordinates");
        assertTrue(m.closePlacesResult(), "Could not close the places card");
    }
}
