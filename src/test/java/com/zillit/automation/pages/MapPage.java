package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Tools &rarr; <b>Map</b> ({@code NewMapPage} &rarr; {@code map_nav_graph} &rarr;
 * {@code MapFragment}).
 *
 * <p>Two different things live on this screen and must not be confused:
 * <ul>
 *   <li>the Map module's <b>own</b> pins, filed under the selected city, editable;</li>
 *   <li>a read-only <b>overlay</b> of Shortlisted / Final entries owned by the
 *       <i>Location tool</i>, fetched project-wide from {@code /location/map-pins}.</li>
 * </ul>
 *
 * <p>Both render through the same {@code rvCarousel} / marker code, because
 * {@code LocationToolPin.toPinnedLocation()} projects the overlay onto the Map's own
 * {@code PinnedLocation} shape. What separates them on screen is small and easy to miss:
 * an overlay card has <b>no</b> {@code btnEdit} and <b>no</b> {@code btnDelete}, and its
 * {@code tvType} carries the spelled-out label ("Shortlisted Shoot Location") where a
 * filter chip would carry the short tag ("Shortlisted").
 *
 * <p>{@link #isLoaded} deliberately keys on {@code map} + {@code tvCityName} rather than on
 * any toolbar button: most of the toolbar is hidden until a city is selected, so a
 * button-based check would report "not loaded" on exactly the fresh-project state the
 * tests most need to assert on.
 */
public class MapPage extends BasePage {

    /** Label on the overlay toggle, from {@code R.string.map_show_location_pins}. */
    public static final String SHOOT_LOCATIONS = "Shoot Locations";

    /** Spelled-out card labels — {@code LocationPinStatus.longLabelRes}. */
    public static final String LABEL_SHORTLISTED = "Shortlisted Shoot Location";
    public static final String LABEL_FINAL = "Finalized Shoot Location";

    /** Short tags, used as the marker TYPE and therefore on the filter chips. */
    public static final String TAG_SHORTLISTED = "Shortlisted";
    public static final String TAG_FINAL = "Final";

    public MapPage(AndroidDriver driver) {
        super(driver);
    }

    // ---------------- loading ----------------

    public boolean isLoaded(Duration timeout) {
        if (!isVisible("map", timeout)) return false;
        return isVisible("tvCityName", Duration.ofSeconds(3));
    }

    public String headerText() {
        return text("headerText");
    }

    /** "Select a City" on a project with no city chosen, else the city's name. */
    public String cityName() {
        return text("tvCityName");
    }

    /** Raw "{@code n locations}" string under the city name. */
    public String locationCountText() {
        return text("tvLocationCount");
    }

    /** The number out of {@link #locationCountText()}, or -1 when it cannot be read. */
    public int locationCount() {
        String raw = locationCountText();
        if (raw == null) return -1;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)").matcher(raw);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    public boolean noCitySelected() {
        return "Select a City".equalsIgnoreCase(String.valueOf(cityName()).trim());
    }

    // ---------------- tutorial card ----------------

    public boolean tutorialShown(Duration timeout) {
        return isVisible("tutorialCard", timeout);
    }

    public boolean tutorialBodyShown() {
        return isVisible("tutorialBody", Duration.ofSeconds(2));
    }

    public boolean toggleTutorial() {
        return clickIfPresent("btnToggleTutorial");
    }

    // ---------------- cities ----------------

    /**
     * The cities button. Its LABEL is state-dependent: "Add City" while the project has
     * no cities, the city affordance once it has some. Tests assert on the label rather
     * than assuming one.
     */
    public String citiesButtonLabel() {
        return text("btnCities");
    }

    /**
     * Opens the city panel, and is safe to call when it is already open.
     *
     * <p>{@code btnCities} is wired to {@code toggleCityPanel()}, so a blind click on an
     * open panel CLOSES it. Cases that had already been through the panel then skipped
     * with "City panel did not open" — the panel had been open the whole time and this
     * call is what shut it.
     */
    public boolean openCityPanel() {
        if (cityPanelShown(Duration.ofSeconds(1))) return true;
        if (!clickIfPresent("btnCities")) return false;
        return cityPanelShown(Duration.ofSeconds(8));
    }

    public boolean cityPanelShown(Duration timeout) {
        return isVisible("cityListContainer", timeout) || isVisible("rvCities", timeout);
    }

    public boolean closeCityPanel() {
        return clickIfPresent("btnCloseCityPanel");
    }

    /**
     * City names in the panel.
     *
     * <p>Scoped to {@code rvCities} deliberately. The row layout is {@code item_city.xml},
     * whose name id is {@code tvCityName} — the SAME id the Map's own header uses for
     * "Select a City". Unscoped, this would return the header too, and a test asserting
     * "the panel lists cities" would pass on an empty panel.
     */
    public List<String> visibleCityNames() {
        return textsIn("rvCities", "tvCityName");
    }

    public boolean searchCities(String term) {
        return typeIfPresent("etCitySearch", term);
    }

    public boolean selectCity(String name) {
        try {
            WebElement row = driver.findElement(AppiumBy.xpath(
                    "//*[@resource-id='" + appPackage() + ":id/rvCities']"
                            + "//*[@resource-id='" + appPackage() + ":id/tvCityName'"
                            + " and @text='" + name + "']"));
            row.click();
            sleep(1500);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ---------------- current-city card ----------------

    /**
     * The "Add Current City" card in the city panel ({@code cardCurrentCity}).
     *
     * <p>Built from the device's GPS fix, it is NOT a saved city — it is an offer to
     * create one, and it is easy to mistake for a list entry. Tapping
     * {@link #addCurrentCity()} does not save either: it opens the city form PREFILLED
     * with the name and coordinates, and the user still has to Save.
     *
     * <p>Hidden for view-only users, along with the rest of the create path.
     */
    public boolean currentCityCardShown(Duration timeout) {
        return isVisible("cardCurrentCity", timeout)
                || isVisible("tvCurrentCityName", timeout);
    }

    public String currentCityName()    { return text("tvCurrentCityName"); }
    public String currentCityAddress() { return text("tvCurrentCityAddress"); }
    /** The action link — "Add Current City" while the city is not yet saved. */
    public String currentCityAction()  { return text("tvCurrentCityAction"); }

    public boolean addCurrentCity() {
        return clickIfPresent("tvCurrentCityAction") || clickIfPresent("cardCurrentCity");
    }

    // ---------------- toolbar ----------------

    /**
     * Toolbar buttons that only exist once a city is selected, or that posting rights gate.
     * Returns the ids actually present, so a test can assert on the SET rather than
     * probing each one and masking which was missing.
     */
    /**
     * Is one toolbar button reachable? Scrolls the strip to it only if it is not already
     * on screen.
     *
     * <p>Cheaper than {@link #visibleToolbarButtons()}, which scrolls across all nine —
     * and UiScrollable is exactly what this suite has to use sparingly.
     */
    public boolean toolbarHasButton(String id) {
        return present(id) || (scrollToolbarTo(id) && present(id));
    }

    /**
     * Which toolbar chips are on screen RIGHT NOW, without scrolling to look for them.
     *
     * <p>For failure messages. {@link #visibleToolbarButtons()} scrolls the strip to each
     * of nine ids, and UiScrollable on a live map is the most expensive thing this suite
     * can do — the lifecycle suite called it from five failure paths and lost the
     * instrumentation ELEVEN times in one run, while the zone suite, which never calls it,
     * lost it none. Diagnosing a problem must not make the problem worse.
     */
    /** The displayed-chip set, without logging — for before/after comparisons. */
    private List<String> toolbarSnapshotQuiet() {
        List<String> found = new ArrayList<>();
        for (String id : List.of("btnCities", "btnLocationPins", "btnFitAll", "btnSearch",
                "btnFilter", "btnPin", "btnViewAll", "btnZoneList", "btnTypes")) {
            if (displayed(id)) found.add(id);
        }
        return found;
    }

    public List<String> toolbarSnapshot() {
        // Print the FULL picture alongside the short list. The short list only names what
        // is displayed, and the question that keeps coming back is about the chips that
        // are not — are they off-screen, hidden by rights, or never created at all? Those
        // three need different answers and look identical from the short list.
        System.out.println(">>> toolbar chips: " + chipReport());
        List<String> found = new ArrayList<>();
        for (String id : List.of("btnCities", "btnLocationPins", "btnFitAll", "btnSearch",
                "btnFilter", "btnPin", "btnViewAll", "btnZoneList", "btnTypes")) {
            if (displayed(id)) found.add(id);
        }
        return found;
    }

    public List<String> visibleToolbarButtons() {
        // The strip overflows, so "visible" has to mean "reachable": each id is scrolled
        // to before it is judged present. A plain read would report the later buttons as
        // absent purely because they are off the right-hand edge.
        List<String> found = new ArrayList<>();
        for (String id : List.of("btnCities", "btnLocationPins", "btnFitAll", "btnSearch",
                "btnFilter", "btnPin", "btnViewAll", "btnZoneList", "btnTypes")) {
            if (present(id) || (scrollToolbarTo(id) && present(id))) found.add(id);
        }
        // Leave the strip where we found it — but only if it actually moved.
        // scrollToolbarTo() now returns immediately when the chip is already on screen,
        // so this is a no-op in the common case instead of a rewind.
        scrollToolbarTo("btnCities");
        return found;
    }

    /**
     * Scrolls the toolbar strip until {@code id} is on screen.
     *
     * <p>Uses UiAutomator's own {@code UiScrollable} rather than a coordinate swipe. A
     * hand-rolled swipe across this strip starts near the right screen edge, and Android
     * reads an edge-adjacent horizontal swipe as the BACK gesture — so the "scroll" walks
     * the app backwards out of the Map instead of moving the strip, and every toolbar
     * action then reports its button as missing.
     */
    private boolean scrollToolbarTo(String id) {
        // DISPLAYED, not merely present: the strip is a HorizontalScrollView, so a chip
        // past the right edge is still in the hierarchy. Keying on presence skipped the
        // scroll those chips needed, and every case then said "View All not available".
        if (displayed(id)) return true;

        // Swipe INSIDE the strip's own bounds rather than using UiScrollable.
        //
        // Measured: with the toolbar at its left end only [btnCities, btnFitAll, btnSearch]
        // are on screen, and UiScrollable could not bring btnViewAll into view — while
        // costing more than anything else this suite does on a live map. A direct swipe is
        // both cheaper and more reliable.
        //
        // The margins matter. A horizontal swipe that starts within a few pixels of the
        // screen edge is read by Android as the BACK gesture, which walks the app out of
        // the Map entirely — that is why this used UiScrollable in the first place. Both
        // ends stay well inside the strip, so no edge gesture fires.
        try {
            // SWIPE ON A CHIP, not on the container's midpoint.
            //
            // The chip report settled what is happening: a chip scrolled out of view is
            // ABSENT from the accessibility tree, not merely hidden — one snapshot showed
            // only btnZoneList and btnTypes, another only btnCities, btnFitAll and
            // btnSearch. So the strip does scroll; what failed was the gesture. Taking the
            // container's vertical centre assumes the strip's bounds are where its rect
            // says, and the measured swipe (y=701) moved nothing at all. A chip that is
            // demonstrably on screen gives a point the strip certainly owns.
            WebElement strip = driver.findElement(byId("toolbarScrollView"));
            org.openqa.selenium.Rectangle r = strip.getRect();
            int y = r.getY() + r.getHeight() / 2;
            for (String visible : List.of("btnCities", "btnFitAll", "btnSearch",
                    "btnZoneList", "btnTypes", "btnViewAll")) {
                java.util.List<WebElement> els = driver.findElements(byId(visible));
                if (els.isEmpty()) continue;
                try {
                    if (!els.get(0).isDisplayed()) continue;
                    org.openqa.selenium.Rectangle cr = els.get(0).getRect();
                    y = cr.getY() + cr.getHeight() / 2;
                    break;
                } catch (Exception ignored) { }
            }
            // KEEP CLEAR OF THE SCREEN EDGES.
            //
            // The set measurement caught this: after a swipe the displayed chips went from
            // [btnCities, btnFitAll, btnSearch] to NOTHING. A scrolled strip still shows
            // chips — an empty toolbar means the app left the Map altogether. The swipe
            // started at x=920 on a 1080-wide screen, inside Android's back-gesture zone,
            // so the system read it as Back and walked the app out of the module. Every
            // "View All not available" after that was this gesture's doing.
            //
            // 0.55 → 0.10 of the strip keeps both ends far from either edge.
            int fromX = r.getX() + (int) (r.getWidth() * 0.55);
            int toX = r.getX() + (int) (r.getWidth() * 0.10);
            // MEASURE, do not assume. Every run says the toolbar shows only the first
            // three chips, which means either the swipe is not scrolling the strip or it
            // scrolls and springs back. Reading btnCities' x before and after says which:
            // if the strip moved, the first chip moves left (or leaves the screen).
            // MEASURE THE SET, not one chip's x.
            //
            // The previous measurement watched btnCities and reported "STRIP DID NOT
            // MOVE" every time — but a chip scrolled out of view is ABSENT from the tree,
            // so its x reads -1 whether the strip moved or not. The signal was worthless,
            // and I chased it through two fixes. Which chips are on screen does change
            // when the strip scrolls, so compare that instead.
            // Let the strip settle before touching it. The same gesture at the same
            // coordinates moved the strip twice and did nothing once in a single run, so
            // what is left is timing, not geometry — a swipe that arrives while the map is
            // still settling is swallowed.
            sleep(700);
            String beforeSet = toolbarSnapshotQuiet().toString();
            // Six, not four: the strip holds nine chips and shows three, so reaching the
            // far end from the near end takes more than four moves when one is swallowed.
            for (int i = 0; i < 6; i++) {
                swipeHorizontally(fromX, toX, y);
                sleep(600);
                if (displayed(id)) return true;
            }
            String afterSet = toolbarSnapshotQuiet().toString();
            System.out.println(">>> toolbar chips: " + chipReport());
            System.out.println(">>> toolbar swipe: " + beforeSet + " -> " + afterSet
                    + (beforeSet.equals(afterSet) ? "  (STRIP DID NOT MOVE)" : "  (strip moved)")
                    + "; strip rect x=" + r.getX() + " y=" + r.getY()
                    + " w=" + r.getWidth() + " h=" + r.getHeight()
                    + "; swiped " + fromX + " -> " + toX + " at y=" + y);
            // Not to the right — try back the other way, in case it is left of the window.
            for (int i = 0; i < 6; i++) {
                swipeHorizontally(toX, fromX, y);
                sleep(600);
                if (displayed(id)) return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Every toolbar chip with whether it exists, whether it is displayed, and where.
     *
     * <p>The one-chip measurement said btnCities was not displayed at all while an earlier
     * snapshot had listed it — so the question is no longer "did the strip scroll" but
     * "what is on this strip at all". This answers both at once.
     */
    public String chipReport() {
        StringBuilder sb = new StringBuilder();
        for (String id : List.of("btnCities", "btnLocationPins", "btnFitAll", "btnSearch",
                "btnFilter", "btnPin", "btnViewAll", "btnZoneList", "btnTypes")) {
            try {
                List<WebElement> els = driver.findElements(byId(id));
                if (els.isEmpty()) { sb.append(id).append("=absent "); continue; }
                WebElement e = els.get(0);
                boolean shown = false;
                try { shown = e.isDisplayed(); } catch (Exception ignored) { }
                sb.append(id).append('=').append(shown ? "shown@" : "hidden@")
                        .append(e.getRect().getX()).append(' ');
            } catch (Exception ex) {
                sb.append(id).append("=err ");
            }
        }
        return sb.toString().trim();
    }

    /** The x of a toolbar chip, or -1 when it is not on screen. For measuring scroll. */
    private int chipX(String id) {
        try {
            for (WebElement e : driver.findElements(byId(id))) {
                if (e.isDisplayed()) return e.getRect().getX();
            }
        } catch (Exception ignored) {
        }
        return -1;
    }

    /**
     * A horizontal drag made of MANY small steps.
     *
     * <p>One big move from start to end does not scroll this strip. The geometry was
     * right all along — measured: strip at x=367 y=654 w=692 h=95, swiped 920→505 at
     * y=701, squarely inside it — and nothing moved. Android's scroll detector works on
     * the stream of MOVE events: a single jump from one point to another gives it no
     * movement to follow, so it never starts a scroll. Twelve steps give it a gesture it
     * recognises.
     *
     * <p>The pin drag works for the same reason, by accident: it holds first, which puts
     * the marker into drag mode before the move arrives.
     */
    private void swipeHorizontally(int fromX, int toX, int y) {
        org.openqa.selenium.interactions.PointerInput finger =
                new org.openqa.selenium.interactions.PointerInput(
                        org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        org.openqa.selenium.interactions.Sequence seq =
                new org.openqa.selenium.interactions.Sequence(finger, 1);
        seq.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), fromX, y));
        seq.addAction(finger.createPointerDown(0));
        int steps = 12;
        for (int i = 1; i <= steps; i++) {
            int x = fromX + (toX - fromX) * i / steps;
            seq.addAction(finger.createPointerMove(Duration.ofMillis(40),
                    org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, y));
        }
        seq.addAction(finger.createPointerUp(0));
        driver.perform(java.util.List.of(seq));
    }

    /**
     * Clicks a toolbar button, scrolling the strip to it first.
     *
     * <p>Without this, every toolbar action silently no-ops once a city is selected and the
     * strip overflows — which on this screen is always, since seven buttons share 692px.
     */
    private boolean clickToolbar(String id) {
        if (clickIfPresent(id)) return true;
        if (scrollToolbarTo(id) && clickIfPresent(id)) return true;
        return false;
    }

    public boolean fitAll() {
        return clickToolbar("btnFitAll");
    }

    /**
     * The TOOLBAR magnifier — toggles the in-screen {@code searchBar}.
     *
     * <p>City-gated: {@code updateToolbarTabs()} hides it until a city is selected.
     */
    public boolean openInScreenSearch() {
        return clickToolbar("btnSearch");
    }

    /**
     * The FLOATING magnifier — launches Google's Places Autocomplete overlay.
     *
     * <p>This is a different feature from {@link #openInScreenSearch()}, not a fallback
     * for it. ZL-19600 replaced the in-screen places dropdown with this overlay because
     * result rows went stale; {@code searchBar} is the Map's own location search and
     * never appears from here. Treating the two as interchangeable makes a test tap this
     * one and then wait for a bar that is never coming.
     */
    public boolean openPlacesSearch() {
        return clickIfPresent("fabSearch");
    }

    /**
     * True when Google's Places Autocomplete overlay is up.
     *
     * <p>Matched on a resource-id prefix because the overlay belongs to the Places SDK,
     * not to this app, so its ids carry Google's package rather than ours.
     */
    public boolean placesAutocompleteShown(Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        do {
            try {
                if (!driver.findElements(AppiumBy.xpath(
                        "//*[contains(@resource-id,'places_autocomplete')]")).isEmpty()) {
                    return true;
                }
            } catch (Exception ignored) { }
            sleep(500);
        } while (System.currentTimeMillis() < end);
        return false;
    }

    public boolean searchBarShown(Duration timeout) {
        return isVisible("searchBar", timeout) || isVisible("etSearch", timeout);
    }

    public boolean typeSearch(String term) {
        // No hideKeyboard() here: UiAutomator2 implements it as a BACK press, which on
        // this screen closes the search bar instead of the IME.
        return typeIfPresent("etSearch", term);
    }

    /**
     * Result rows in the in-screen search list.
     *
     * <p>Scoped to {@code rvSearchResults}, and that is not optional:
     * {@code item_search_result.xml} labels its rows {@code tvName}/{@code tvType} — the
     * same two ids the carousel cards use. Unscoped, this would return whatever is behind
     * the search overlay and a "my location is findable" assertion would pass without the
     * search having matched anything at all.
     */
    public List<String> searchResultNames() {
        if (!isVisible("rvSearchResults", Duration.ofSeconds(4))) return List.of();
        return textsIn("rvSearchResults", "tvName");
    }

    /** Type labels on the result rows, in the same order as {@link #searchResultNames()}. */
    public List<String> searchResultTypes() {
        if (!isVisible("rvSearchResults", Duration.ofSeconds(4))) return List.of();
        return textsIn("rvSearchResults", "tvType");
    }

    /**
     * Taps the first search result.
     *
     * <p>What this does is worth stating, because it is not what you would guess: the
     * adapter's click handler animates the camera to the pin, calls {@code toggleSearch()}
     * and clears the query — so the search CLOSES and no card opens. A pin's card comes
     * from tapping its marker, which is a different gesture entirely.
     */
    public boolean tapFirstSearchResult() {
        try {
            List<WebElement> rows = driver.findElements(AppiumBy.xpath(
                    "//*[@resource-id='" + appPackage() + ":id/rvSearchResults']"
                            + "//*[@resource-id='" + appPackage() + ":id/tvName']"));
            if (rows.isEmpty()) return false;
            rows.get(0).click();
            sleep(2000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Closes the in-screen search the same way the user does — the toolbar toggle. */
    public boolean closeInScreenSearch() {
        return clickToolbar("btnSearch");
    }

    public boolean searchResultsShown(Duration timeout) {
        return isVisible("rvSearchResults", timeout);
    }

    public boolean cycleMapType() {
        return clickIfPresent("fabMapType");
    }

    // ---------------- filters ----------------

    public boolean openFilterPanel() {
        return clickToolbar("btnFilter");
    }

    public boolean filterPanelShown(Duration timeout) {
        return isVisible("filterPanel", timeout) || isVisible("rvFilterTypes", timeout);
    }

    public boolean clearFilters() {
        return clickIfPresent("tvClearFilters");
    }

    /**
     * Filter chip labels — these carry the SHORT tags, never the spelled-out labels.
     *
     * <p>The chip row is {@code item_filter_type.xml}, whose label id is
     * {@code tvTypeName}. It is not {@code tvName}: that id belongs to the carousel and
     * location-list cards, so reading it here would silently return pin names and make a
     * chip-wording assertion test the wrong text.
     */
    public List<String> filterTypeLabels() {
        if (!isVisible("rvFilterTypes", Duration.ofSeconds(4))) return List.of();
        return textsIn("rvFilterTypes", "tvTypeName");
    }

    /** The per-type pin counts on the chips, in {@link #filterTypeLabels()} order. */
    public List<String> filterTypeCounts() {
        if (!isVisible("rvFilterTypes", Duration.ofSeconds(4))) return List.of();
        return textsIn("rvFilterTypes", "tvCount");
    }

    /**
     * Toggles one filter chip by its label.
     *
     * <p>Clicks {@code filterItemRoot} rather than the label: the adapter's listener is on
     * the row, and a tap that lands on the TextView does not always bubble to it.
     */
    public boolean toggleTypeFilter(String label) {
        try {
            WebElement row = driver.findElement(AppiumBy.xpath(
                    "//*[@resource-id='" + appPackage() + ":id/rvFilterTypes']"
                            + "//*[@resource-id='" + appPackage() + ":id/tvTypeName'"
                            + " and @text='" + label + "']"
                            + "/ancestor::*[@resource-id='" + appPackage()
                            + ":id/filterItemRoot'][1]"));
            row.click();
            sleep(1500);
            return true;
        } catch (Exception ignored) {
            // The ancestor axis is unreliable on some UiAutomator builds — pair by index.
        }
        try {
            List<WebElement> labels = driver.findElements(AppiumBy.xpath(
                    "//*[@resource-id='" + appPackage() + ":id/rvFilterTypes']"
                            + "//*[@resource-id='" + appPackage() + ":id/tvTypeName']"));
            List<WebElement> roots = driver.findElements(AppiumBy.xpath(
                    "//*[@resource-id='" + appPackage() + ":id/rvFilterTypes']"
                            + "//*[@resource-id='" + appPackage() + ":id/filterItemRoot']"));
            for (int i = 0; i < labels.size() && i < roots.size(); i++) {
                if (label.equals(labels.get(i).getText())) {
                    roots.get(i).click();
                    sleep(1500);
                    return true;
                }
            }
        } catch (Exception ignored) { }
        return false;
    }

    /** True when the chip for {@code label} is showing its selected tick. */
    /**
     * Is a filter chip selected? <b>This cannot be read from the device.</b>
     *
     * <p>The app marks a selected row by swapping the ImageView's drawable
     * ({@code checkbox_on_background} vs {@code checkbox_off_background}) and tinting the
     * row's background ({@code MapFragment:2251-2259}). Neither a drawable resource nor a
     * background colour reaches the accessibility tree, and `ivCheck` is present either
     * way — so the old implementation, which asked whether an `ivCheck` existed, answered
     * "yes" for every row whatever its state, and "no" the moment the XPath drifted.
     *
     * <p>Kept, and kept honest: it returns {@code false} and the cases now assert what the
     * filter DOES instead. A chip that merely looks selected proves nothing anyway.
     *
     * @deprecated nothing observable backs this; assert the filtered result instead
     */
    @Deprecated
    public boolean typeFilterSelected(String label) {
        return false;
    }

    // ---------------- location-tool overlay ----------------

    /**
     * True when the "Shoot Locations" toggle exists at all.
     *
     * <p>It is GONE, not disabled, for a user without Location-tool VIEW access —
     * {@code applyPostingRights()} keys it on {@code canViewLocationTool}, which is
     * independent of the Map's own posting rights.
     */
    public boolean overlayToggleShown(Duration timeout) {
        return isVisible("btnLocationPins", timeout);
    }

    /** Full label, e.g. "Shoot Locations" or "Shoot Locations (7)". */
    public String overlayToggleLabel() {
        return text("btnLocationPins");
    }

    /**
     * The count the toggle advertises, or -1 when it shows none.
     *
     * <p>The app only appends the count while the overlay is ON <i>and</i> at least one
     * pin loaded, so -1 legitimately means either "off" or "on with nothing to draw" —
     * {@link #overlayActive()} separates those.
     */
    public int overlayPinCount() {
        String raw = overlayToggleLabel();
        if (raw == null) return -1;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\((\\d+)\\)").matcher(raw);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    /**
     * Whether the overlay is switched on, read from the toggle's SELECTED state.
     *
     * <p>The app signals state by tint, which Appium cannot read; it also sets the
     * accessibility selected flag, which it can. Where the flag is absent we fall back
     * to the count suffix, which only ever appears while the overlay is on.
     */
    public boolean overlayActive() {
        try {
            WebElement b = driver.findElement(byId("btnLocationPins"));
            String sel = b.getAttribute("selected");
            if (sel != null) return Boolean.parseBoolean(sel);
        } catch (Exception ignored) { }
        return overlayPinCount() >= 0;
    }

    public boolean toggleOverlay() {
        boolean clicked = clickToolbar("btnLocationPins");
        if (clicked) sleep(2500);   // a flick ON triggers a network fetch
        return clicked;
    }

    // ---------------- carousel / bottom panel ----------------

    public boolean bottomPanelShown(Duration timeout) {
        return isVisible("bottomLocationPanel", timeout)
                || isVisible("rvCarousel", timeout);
    }

    public boolean closeBottomPanel() {
        return clickIfPresent("btnCloseLocationPanel");
    }

    public boolean togglePanelMode() {
        return clickIfPresent("btnLocationPill");
    }

    public boolean carouselShown(Duration timeout) {
        return isVisible("rvCarousel", timeout);
    }

    /**
     * Names on the visible carousel cards.
     *
     * <p>Scoped to {@code rvCarousel}: the bottom panel can show {@code rvLocationList}
     * instead, whose rows are {@code item_location_card.xml} and carry {@code tvName} too.
     */
    public List<String> carouselNames() {
        return textsIn("rvCarousel", "tvName");
    }

    /** Type labels on the visible carousel cards ({@code item_carousel_card.xml}). */
    public List<String> carouselTypes() {
        return textsIn("rvCarousel", "tvType");
    }

    /** True when any visible card carries one of the two spelled-out overlay labels. */
    public boolean anyOverlayCardShown() {
        for (String t : carouselTypes()) {
            if (t == null) continue;
            if (t.contains(LABEL_SHORTLISTED) || t.contains(LABEL_FINAL)) return true;
        }
        return false;
    }

    public boolean openViewAll() {
        return clickToolbar("btnViewAll");
    }

    // ---------------- pin mode ----------------

    /**
     * Toggles pin mode.
     *
     * <p>Needs a selected city: {@code togglePinMode()} refuses with the warning
     * "Please select a city first" otherwise, so a test that skips the city step sees a
     * toast rather than a banner.
     */
    public boolean togglePinMode() {
        return clickToolbar("btnPin");
    }

    public boolean pinModeActive(Duration timeout) {
        return isVisible("pinModeBanner", timeout);
    }

    /**
     * The pin button's label, which doubles as the mode indicator:
     * "Pin Location" when off, "Close" when on.
     */
    public String pinButtonLabel() {
        return text("btnPin");
    }

    /** Taps the centre of the map — where pin mode drops its provisional marker. */
    /**
     * Drops a preview pin at the map centre and commits it through the marker's
     * "Add Location" info window, which is how a location is actually created in this app.
     *
     * <p>THIS is the creation path, not the address field. {@code MapFragment.handlePinAction}
     * places a draggable marker and reverse-geocodes the point
     * ({@code GoogleGeocoder.reverseGeocode} — the Geocoding HTTP API, NOT Places
     * Autocomplete), then the info-window click at {@code MapFragment:1668} runs the
     * boundary check and navigates to the location form with {@code cityId}, {@code lat}
     * and {@code lng} in its arguments. {@code LocationFormFragment:161-184} takes those
     * and fills {@code etAddress} itself. So the address never has to be typed, and the
     * non-focusable address field does not block creation on this path at all.
     *
     * <p>The info window is drawn by a custom {@code InfoWindowAdapter}, which Google Maps
     * renders as a bitmap on the map canvas — it is NOT in the view hierarchy, so it has
     * no resource id and can only be tapped by coordinate. It sits directly above the
     * marker, whose anchor is the tapped point.
     *
     * @return true once the location form is open
     */
    /**
     * Drops a preview pin at a point expressed as a FRACTION of the map view, so a case
     * can choose where the pin lands relative to the city centre.
     *
     * <p>The map is centred on the selected city, so {@code (0.5, 0.5)} is inside it by
     * construction. A fraction near an edge is far from the centre on the ground, which is
     * how the "outside the zone" and "outside the city" verdicts are reached without
     * knowing a single coordinate — the distance scales with the current zoom.
     *
     * @param fx horizontal fraction, 0.0 = left edge, 1.0 = right edge
     * @param fy vertical fraction, 0.0 = top, 1.0 = bottom
     */
    public boolean dropPinAt(double fx, double fy) {
        try {
            WebElement mapView = driver.findElement(byId("map"));
            org.openqa.selenium.Rectangle r = mapView.getRect();
            lastPinX = r.getX() + (int) (r.getWidth() * fx);
            lastPinY = r.getY() + (int) (r.getHeight() * fy);
            tapAt(lastPinX, lastPinY);
            sleep(4000);   // the info window first reads "Resolving address..."
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Taps the "Add Location" info window of the pin dropped by {@link #dropPinAt}.
     *
     * <p>Separate from dropping it because the boundary dialog fires on THIS tap, not on
     * the drop ({@code MapFragment:1668}) — a case that wants to assert the dialog has to
     * get control back in between.
     *
     * @return true if the commit tap landed on the window
     */
    public boolean commitPin() {
        if (lastPinX < 0) return false;
        // WALK UPWARDS, one step at a time, re-aiming after every miss.
        //
        // The earlier version tried several offsets measured from the ORIGINAL drop point.
        // That cannot work, and the app says why: in pin mode setOnMapClickListener routes
        // every map tap to handlePinAction (MapFragment:1755-1758), which REMOVES the old
        // preview marker and places a new one where you just tapped. So a missed commit
        // tap does not leave the pin where it was — it moves the pin to the miss, and the
        // info window moves with it. Every later offset was then measured from a point the
        // marker had already left, which is why all six missed and only one pin in three
        // ever got saved.
        //
        // Walking up fixes it precisely because of that behaviour: whatever happens, the
        // pin is at the last point tapped, so its window is a step above THAT. Either the
        // step lands on the window and the form opens, or it drops the pin one step higher
        // and the next step tries again from there.
        int h = driver.manage().window().getSize().getHeight();
        int step = (int) (h * 0.07);
        int y = lastPinY;
        for (int i = 0; i < 8 && y - step > 0; i++) {
            y -= step;
            tapAt(lastPinX, y);
            // Each new preview pin re-runs the reverse geocode, and the window reads
            // "Resolving address…" until it returns. Tapping through that is harmless but
            // the window is shorter, so give it a moment to settle at full height.
            sleep(2200);
            if (!driver.findElements(byId("etName")).isEmpty()) {
                lastPinY = y;
                return true;                                            // the form opened
            }
            if (!driver.findElements(org.openqa.selenium.By.id("android:id/alertTitle"))
                    .isEmpty()) {
                lastPinY = y;
                return true;                                            // boundary dialog
            }
        }
        return false;
    }

    /**
     * Drags the pin placed by {@link #dropPinAt} to another fraction of the map.
     *
     * <p>A long press is required first: the marker only becomes draggable once Maps has
     * accepted the press, and a plain move is read as a map pan instead.
     */
    public boolean dragPinTo(double fx, double fy) {
        try {
            WebElement mapView = driver.findElement(byId("map"));
            org.openqa.selenium.Rectangle r = mapView.getRect();
            int toX = r.getX() + (int) (r.getWidth() * fx);
            int toY = r.getY() + (int) (r.getHeight() * fy);
            org.openqa.selenium.interactions.PointerInput finger =
                    new org.openqa.selenium.interactions.PointerInput(
                            org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
            org.openqa.selenium.interactions.Sequence drag =
                    new org.openqa.selenium.interactions.Sequence(finger, 1);
            drag.addAction(finger.createPointerMove(Duration.ZERO,
                    org.openqa.selenium.interactions.PointerInput.Origin.viewport(),
                    lastPinX, lastPinY));
            drag.addAction(finger.createPointerDown(0));
            drag.addAction(new org.openqa.selenium.interactions.Pause(finger, Duration.ofMillis(1200)));
            drag.addAction(finger.createPointerMove(Duration.ofMillis(1500),
                    org.openqa.selenium.interactions.PointerInput.Origin.viewport(), toX, toY));
            drag.addAction(finger.createPointerUp(0));
            driver.perform(java.util.List.of(drag));
            lastPinX = toX;
            lastPinY = toY;
            sleep(2500);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Where the last preview pin was dropped, so the commit tap knows where to aim. */
    private int lastPinX = -1;
    private int lastPinY = -1;

    public boolean dropPinAndCommit() {
        try {
            WebElement mapView = driver.findElement(byId("map"));
            org.openqa.selenium.Rectangle r = mapView.getRect();
            int x = r.getX() + r.getWidth() / 2;
            int y = r.getY() + r.getHeight() / 2;
            tapAt(x, y);
            // The window first reads "Resolving address..." and is replaced when the
            // reverse geocode returns. Committing before that lands on a stale marker.
            sleep(4000);
            // Walk upwards from the pin: the window's height depends on how long the
            // resolved address is, so try a few offsets rather than guessing one.
            for (int dy : new int[]{130, 180, 90, 230}) {
                tapAt(x, y - dy);
                sleep(1500);
                if (!driver.findElements(byId("etName")).isEmpty()) return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean tapMapCentre() {
        try {
            WebElement mapView = driver.findElement(byId("map"));
            org.openqa.selenium.Rectangle r = mapView.getRect();
            int x = r.getX() + r.getWidth() / 2;
            int y = r.getY() + r.getHeight() / 2;
            tapAt(x, y);
            sleep(2500);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Taps a point offset from the map centre, as a fraction of the visible map.
     *
     * <p>This is how a test reaches "somewhere else on screen" without knowing real
     * coordinates. It cannot by itself guarantee the point lands outside a city or zone —
     * that depends on the camera's zoom — so boundary tests confirm the verdict by reading
     * the dialog that follows rather than by assuming where the tap landed.
     */
    public boolean tapMapOffset(double fx, double fy) {
        try {
            WebElement mapView = driver.findElement(byId("map"));
            org.openqa.selenium.Rectangle r = mapView.getRect();
            int x = (int) (r.getX() + r.getWidth() * (0.5 + fx));
            int y = (int) (r.getY() + r.getHeight() * (0.5 + fy));
            x = Math.max(r.getX() + 8, Math.min(x, r.getX() + r.getWidth() - 8));
            y = Math.max(r.getY() + 8, Math.min(y, r.getY() + r.getHeight() - 8));
            tapAt(x, y);
            sleep(2500);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void tapAt(int x, int y) {
        org.openqa.selenium.interactions.PointerInput finger =
                new org.openqa.selenium.interactions.PointerInput(
                        org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        org.openqa.selenium.interactions.Sequence tap =
                new org.openqa.selenium.interactions.Sequence(finger, 1);
        tap.addAction(finger.createPointerMove(java.time.Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, y));
        tap.addAction(finger.createPointerDown(0));
        tap.addAction(new org.openqa.selenium.interactions.Pause(finger, Duration.ofMillis(120)));
        tap.addAction(finger.createPointerUp(0));
        driver.perform(java.util.List.of(tap));
    }

    /** Zoom out so a single tap can land well outside the city circle. */
    public boolean zoomOut(int steps) {
        try {
            WebElement mapView = driver.findElement(byId("map"));
            org.openqa.selenium.Rectangle r = mapView.getRect();
            int cx = r.getX() + r.getWidth() / 2;
            int cy = r.getY() + r.getHeight() / 2;
            for (int i = 0; i < steps; i++) {
                // Double-tap with two fingers is the Maps gesture for zoom out.
                tapAt(cx - 60, cy);
                tapAt(cx + 60, cy);
                sleep(400);
            }
            sleep(1500);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ---------------- zones ----------------

    public boolean openZoneList() {
        return clickToolbar("btnZoneList");
    }

    /** Label carries the ACTIVE zone's name once one is applied. */
    public String zoneButtonLabel() {
        return text("btnZoneList");
    }

    public boolean openTypes() {
        return clickToolbar("btnTypes");
    }

    public boolean openPinMode() {
        return clickToolbar("btnPin");
    }

    // ---------------- directions ----------------

    public boolean directionsPanelShown(Duration timeout) {
        return isVisible("directionsPanel", timeout)
                || isVisible("etDirectionsTo", timeout);
    }

    public String directionsTo()       { return text("etDirectionsTo"); }
    public String directionsFrom()     { return text("etDirectionsFrom"); }
    public String directionsDistance() { return text("tvDirectionsDistance"); }
    public String directionsDuration() { return text("tvDirectionsDuration"); }

    public boolean useMyLocation()    { return clickIfPresent("btnUseMyLocation"); }
    public boolean shareDirections()  { return clickIfPresent("btnShareDirections"); }
    public boolean closeDirections()  { return clickIfPresent("btnCloseDirections"); }
    public boolean callDriverShown()  { return present("btnCallDriverMap"); }

    // ---------------- places result card ----------------

    public boolean placesResultShown(Duration timeout) {
        return isVisible("placesResultCard", timeout);
    }

    public String placeName()    { return text("tvPlaceName"); }
    public String placeAddress() { return text("tvPlaceAddress"); }
    public String placeCoords()  { return text("tvPlaceCoords"); }

    public boolean closePlacesResult() { return clickIfPresent("btnClosePlacesResult"); }
    public boolean pinPlace()          { return clickIfPresent("btnPlacePin"); }
    public boolean placeDirections()   { return clickIfPresent("btnPlaceDirections"); }

    // ---------------- carousel row actions ----------------

    public boolean cardShare()      { return clickIfPresent("btnShare"); }
    public boolean cardView()       { return clickIfPresent("btnView"); }
    public boolean cardEdit()       { return clickIfPresent("btnEdit"); }
    public boolean cardDelete()     { return clickIfPresent("btnDelete"); }
    public boolean cardDirections() { return clickIfPresent("btnDirections"); }

    public boolean cardEditShown()   { return present("btnEdit"); }
    public boolean cardDeleteShown() { return present("btnDelete"); }

    /** True once the system share sheet is up (it belongs to Android, not to this app). */
    public boolean shareSheetShown(Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        do {
            try {
                String pkg = driver.getCurrentPackage();
                if (pkg != null && !pkg.contains("zillit")) return true;
                if (!driver.findElements(AppiumBy.xpath(
                        "//*[contains(@resource-id,'chooser') or contains(@resource-id,'resolver')]"))
                        .isEmpty()) {
                    return true;
                }
            } catch (Exception ignored) { }
            sleep(500);
        } while (System.currentTimeMillis() < end);
        return false;
    }

    // ---------------- small helpers ----------------

    /** On screen AND laid out — not merely somewhere in the hierarchy. */
    private boolean displayed(String id) {
        try {
            for (WebElement e : driver.findElements(byId(id))) {
                if (e.isDisplayed()) return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean present(String id) {
        try {
            return !driver.findElements(byId(id)).isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * First element with this id, in view-hierarchy order.
     *
     * <p>Order matters for {@code tvCityName} and {@code tvLocationCount}: both ids appear
     * once in the Map's own header and again on every row of {@code item_city.xml}. The
     * header sits above {@code cityListContainer} in {@code fragment_map.xml}, so the first
     * match is the header even while the city panel is open. Row-level reads go through
     * {@link #textsIn} instead, never through here.
     */
    private String text(String id) {
        try {
            List<WebElement> els = driver.findElements(byId(id));
            return els.isEmpty() ? null : els.get(0).getText();
        } catch (Exception e) {
            return null;
        }
    }

    /** Texts of {@code childId} found INSIDE {@code containerId}. */
    private List<String> textsIn(String containerId, String childId) {
        List<String> out = new ArrayList<>();
        try {
            List<WebElement> els = driver.findElements(AppiumBy.xpath(
                    "//*[@resource-id='" + appPackage() + ":id/" + containerId + "']"
                            + "//*[@resource-id='" + appPackage() + ":id/" + childId + "']"));
            for (WebElement e : els) {
                String t = e.getText();
                if (t != null && !t.isBlank()) out.add(t.trim());
            }
        } catch (Exception ignored) { }
        return out;
    }

    private boolean clickIfPresent(String id) {
        try {
            List<WebElement> els = driver.findElements(byId(id));
            if (els.isEmpty()) return false;
            els.get(0).click();
            sleep(900);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean typeIfPresent(String id, String value) {
        try {
            List<WebElement> els = driver.findElements(byId(id));
            if (els.isEmpty()) return false;
            WebElement f = els.get(0);
            f.click();
            f.clear();
            f.sendKeys(value);
            sleep(1200);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }
}
