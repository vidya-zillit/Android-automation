package com.zillit.automation.tests.map;

import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.map.MapDialogPage;
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
 * MAP &rarr; <b>pinning and the boundary rules</b>.
 *
 * <p>{@code evaluatePinBoundary()} runs the <b>zone check first, then the city</b>, and
 * returns one of three verdicts. Each leads to a different dialog, and the wording is the
 * only thing telling the user which boundary they crossed, so the wording is asserted:
 *
 * <pre>
 *   Inside                 → no dialog at all; the pin proceeds silently
 *   OutsideZone (in city)  → "Outside Studio Zone"  · Yes / No
 *   OutsideCity (+ zone)   → "Outside &lt;city&gt;" · Create Another City / Pin in &lt;city&gt; / Cancel
 * </pre>
 *
 * <p>Two defaults are easy to miss and both are deliberate: a radius stored as 0 is treated
 * as <b>30 miles</b>, and a city or zone with no coordinates returns <b>Inside</b> — a
 * data-integrity problem must not reach the user as a rejection.
 *
 * <p><b>On determinism.</b> A test cannot pick a screen point and know it lands outside a
 * city, because that depends on the camera's zoom. These cases therefore <i>provoke</i> a
 * tap and then assert on whichever dialog appears, rather than asserting that a particular
 * verdict was reached. Where no dialog appears the case skips, saying so — that is the
 * honest outcome, not a pass.
 *
 * <p><b>Writes to the backend</b> only where a case confirms a pin; those cases name their
 * fixture with the {@code ZL AUTO} prefix and {@link MapLocationCrudTest} cleans up.
 */
@Order(TestOrder.MAP_PIN_BOUNDARY)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MapPinBoundaryTest extends MapBaseTest {

    /** Enters pin mode, or skips with the reason. */
    private MapPage pinMode() {
        assumeTrue(ensureCitySelected(),
                "Pin mode needs a selected city and this project has none");
        MapPage m = map();
        if (!m.pinModeActive(Duration.ofSeconds(1))) {
            assumeTrue(m.togglePinMode(), "Pin button not available (needs posting rights)");
        }
        assumeTrue(m.pinModeActive(Duration.ofSeconds(8)), "Pin mode did not start");
        return m;
    }

    private void leavePinMode(MapPage m) {
        if (m.pinModeActive(Duration.ofSeconds(1))) m.togglePinMode();
        sleep(1000);
        backToMap();
    }

    // ---------------- entering and leaving pin mode ----------------

    @Test
    @Order(1)
    @DisplayName("P-01 Pin mode refuses without a city")
    void pinNeedsCity() {
        MapPage m = map();
        // The label alone is the WRONG precondition. The Map restores lastVisitedCityId
        // asynchronously, so tvCityName can still read "Select a City" for a moment after
        // the ViewModel already holds a city — and this case then "failed" claiming the
        // app let pin mode start without one. It does not: MapViewModel.togglePinMode()
        // (:586) returns with the "Please select a city first" warning when
        // _selectedCity.value is null. The toolbar is the honest signal, because
        // updateToolbarTabs() (MapFragment:955) only shows btnPin once a city is
        // selected — so if btnPin is on the strip, a city IS selected, whatever the
        // label currently says.
        assumeTrue(m.noCitySelected(), "A city is already selected; this case needs none");
        boolean pinButtonOffered = m.toolbarHasButton("btnPin");
        assumeTrue(!pinButtonOffered,
                "btnPin is on the toolbar, so a city is selected even though the label"
                        + " still reads \"Select a City\" — this case needs a city-less map");
        // With no city the refusal shows up as the button being absent rather than as a
        // toast. Either way the user cannot pin.
        boolean bannerAppeared = m.togglePinMode() && m.pinModeActive(Duration.ofSeconds(4));
        assertFalse(bannerAppeared,
                "Pin mode started with no city selected — togglePinMode() should refuse "
                        + "with \"Please select a city first\"");
    }

    @Test
    @Order(2)
    @DisplayName("P-02/P-03 Pin mode shows a banner and the button becomes Close")
    void pinModeTogglesCleanly() {
        MapPage m = pinMode();
        assertTrue(m.pinModeActive(Duration.ofSeconds(5)), "No pin-mode banner");
        String onLabel = m.pinButtonLabel();
        assertTrue(onLabel != null && onLabel.toLowerCase().contains("close"),
                "In pin mode the button should read \"Close\", got: " + onLabel);

        assumeTrue(m.togglePinMode(), "Could not leave pin mode");
        sleep(1200);
        assertFalse(m.pinModeActive(Duration.ofSeconds(3)), "Banner stayed after leaving");
        String offLabel = m.pinButtonLabel();
        assertTrue(offLabel == null || offLabel.toLowerCase().contains("pin"),
                "Out of pin mode the button should read \"Pin Location\", got: " + offLabel);
        backToMap();
    }

    // ---------------- inside ----------------

    @Test
    @Order(3)
    @DisplayName("P-04/P-05 A pin inside the boundary raises no dialog at all")
    void insideIsSilent() {
        MapPage m = pinMode();
        // The map centres on the selected city, so its centre is inside by construction.
        assumeTrue(m.tapMapCentre(), "Could not tap the map");
        sleep(2000);

        MapDialogPage dlg = new MapDialogPage(driver);
        boolean dialogShown = dlg.shown(Duration.ofSeconds(4));
        if (dialogShown) {
            String title = String.valueOf(dlg.title());
            dlg.tap(MapDialogPage.BTN_CANCEL);
            dlg.dismiss();
            // A dialog here means the city's own centre read as outside its radius, which
            // is a genuine finding rather than a test problem.
            assertFalse(title.toLowerCase().contains("outside"),
                    "Tapping the city centre raised \"" + title + "\" — a point at the "
                            + "centre of the selected city must evaluate as Inside");
        }
        leavePinMode(m);
    }

    // ---------------- outside ----------------

    @Test
    @Order(4)
    @DisplayName("P-06/P-09 A pin outside the boundary raises the right dialog for the verdict")
    void outsideRaisesVerdictDialog() {
        MapPage m = pinMode();
        // Zoom out so one tap can reach well beyond the city circle, then tap a corner.
        m.zoomOut(3);
        assumeTrue(m.tapMapOffset(0.42, 0.40), "Could not tap the map");
        sleep(2500);

        MapDialogPage dlg = new MapDialogPage(driver);
        assumeTrue(dlg.shown(Duration.ofSeconds(8)),
                "No boundary dialog — the tap most likely still landed inside the city at "
                        + "this zoom, so there is no verdict to assert on");

        String title = String.valueOf(dlg.title());
        java.util.List<String> buttons = dlg.buttonLabels();

        if (title.contains(MapDialogPage.TITLE_OUTSIDE_ZONE)) {
            // Outside the zone but still in the city: a two-option confirm.
            assertTrue(dlg.hasButton(MapDialogPage.BTN_YES)
                            && dlg.hasButton(MapDialogPage.BTN_NO),
                    "\"" + MapDialogPage.TITLE_OUTSIDE_ZONE + "\" should offer Yes/No; got "
                            + buttons);
            assertFalse(dlg.hasButton(MapDialogPage.BTN_CREATE_ANOTHER_CITY),
                    "The outside-zone confirm must NOT offer Create Another City — the pin "
                            + "is still inside the selected city");
            assertTrue(String.valueOf(dlg.message()).toLowerCase().contains("outside"),
                    "Message should say what was crossed; got: " + dlg.message());
            dlg.tap(MapDialogPage.BTN_NO);
        } else if (title.toLowerCase().startsWith("outside")) {
            // Outside both: the three-option confirm.
            assertTrue(dlg.hasButton(MapDialogPage.BTN_CREATE_ANOTHER_CITY),
                    "The outside-city confirm should offer Create Another City; got " + buttons);
            assertTrue(dlg.hasButton(MapDialogPage.BTN_CANCEL),
                    "The outside-city confirm should offer Cancel; got " + buttons);
            assertTrue(buttons.size() >= 3,
                    "Expected three options (Create Another City / Pin in <city> / Cancel); got "
                            + buttons);
            assertTrue(String.valueOf(dlg.message()).contains("outside the selected city"),
                    "Message should name both boundaries; got: " + dlg.message());
            dlg.tap(MapDialogPage.BTN_CANCEL);
        } else {
            assumeTrue(false, "Unrecognised dialog while pinning: " + title);
        }

        sleep(1500);
        leavePinMode(m);
    }

    @Test
    @Order(5)
    @DisplayName("P-07/P-10 Declining the boundary dialog pins nothing")
    void decliningPinsNothing() {
        assumeTrue(ensureCitySelected(), "Needs a selected city");
        MapPage before = map();
        int countBefore = before.locationCount();

        MapPage m = pinMode();
        m.zoomOut(3);
        assumeTrue(m.tapMapOffset(0.42, 0.40), "Could not tap the map");
        sleep(2500);

        MapDialogPage dlg = new MapDialogPage(driver);
        assumeTrue(dlg.shown(Duration.ofSeconds(8)),
                "No boundary dialog raised — nothing to decline");

        // "No" on the two-option confirm, "Cancel" on the three-option one.
        boolean declined = dlg.tap(MapDialogPage.BTN_NO) || dlg.tap(MapDialogPage.BTN_CANCEL);
        assumeTrue(declined, "Could not decline the dialog; buttons were " + dlg.buttonLabels());
        sleep(2500);

        leavePinMode(m);
        int countAfter = map().locationCount();
        assertTrue(countAfter <= countBefore,
                "Declining the boundary dialog still created a pin: " + countBefore
                        + " → " + countAfter);
    }

    @Test
    @Order(6)
    @DisplayName("P-13 The boundary dialog tells the user where the pin would land")
    void dialogCarriesAddress() {
        MapPage m = pinMode();
        m.zoomOut(3);
        assumeTrue(m.tapMapOffset(0.42, 0.40), "Could not tap the map");
        sleep(3000);

        MapDialogPage dlg = new MapDialogPage(driver);
        assumeTrue(dlg.shown(Duration.ofSeconds(8)), "No boundary dialog raised");
        String msg = String.valueOf(dlg.message());
        // The address comes from a reverse geocode, which can legitimately return nothing
        // over open water or without a geocoding key — so its absence is a skip.
        assumeTrue(msg.contains("New location:"),
                "Dialog carried no reverse-geocoded address (geocoder returned nothing)");
        assertTrue(msg.indexOf("New location:") < msg.length() - "New location:".length() - 2,
                "\"New location:\" present but no address follows it");

        dlg.tap(MapDialogPage.BTN_NO);
        dlg.tap(MapDialogPage.BTN_CANCEL);
        dlg.dismiss();
        sleep(1200);
        leavePinMode(m);
    }

    @Test
    @Order(7)
    @DisplayName("P-10b Dismissing the dialog counts as Cancel, not as confirm")
    void dismissCountsAsCancel() {
        assumeTrue(ensureCitySelected(), "Needs a selected city");
        int countBefore = map().locationCount();

        MapPage m = pinMode();
        m.zoomOut(3);
        assumeTrue(m.tapMapOffset(0.42, 0.40), "Could not tap the map");
        sleep(2500);

        MapDialogPage dlg = new MapDialogPage(driver);
        assumeTrue(dlg.shown(Duration.ofSeconds(8)), "No boundary dialog raised");
        dlg.dismiss();                       // BACK — the dialog's own dismiss listener
        sleep(2500);

        leavePinMode(m);
        assertTrue(map().locationCount() <= countBefore,
                "Dismissing the dialog created a pin — the dismiss listener should "
                        + "deliver Cancel");
    }
}
