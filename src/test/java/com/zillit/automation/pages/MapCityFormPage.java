package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Map &rarr; <b>city form</b> ({@code fragment_city_form.xml}), and the city rows it feeds.
 *
 * <p>A city here is more than a name: it is a place, a <i>center point</i> and a
 * <i>radius</i>. The radius is not cosmetic — it is the circle every later pin is measured
 * against by {@code evaluatePinBoundary()}, so a city saved with the wrong radius silently
 * changes the behaviour of every boundary case downstream.
 *
 * <p>The center point can be given two ways, and the form swaps its fields between them:
 * <ul>
 *   <li><b>Center Point</b> — {@code etCenterSearch}, a place lookup;</li>
 *   <li><b>Street Intersection</b> — {@code etStreet1} + {@code etStreet2} +
 *       {@code btnFindIntersection}, which geocodes the crossing.</li>
 * </ul>
 *
 * <p>The same field set also ships as {@code dialog_add_city.xml}; the ids are identical,
 * so this page object drives either.
 */
public class MapCityFormPage extends BasePage {

    public static final String ERR_NO_NAME = "Please enter a city name";
    public static final String ERR_NO_PLACE = "Please search and select a city location";
    public static final String ERR_NO_STREETS = "Please enter both street names";
    public static final String MSG_INTERSECTION_FOUND = "Intersection found!";
    public static final String MSG_NO_INTERSECTION = "No intersection found";

    public MapCityFormPage(AndroidDriver driver) {
        super(driver);
    }

    /**
     * True when the CITY FORM is up.
     *
     * <p>Deliberately not keyed on {@code etCitySearch}: that id exists on the Map's own
     * city PANEL as well ({@code fragment_map.xml}), so a form check built on it reports
     * "form open" while the user is still looking at the list. {@code btnSave} and
     * {@code tvFormTitle} belong to the form alone.
     */
    /**
     * Presence of the form's own markers, not their visibility — the form scrolls, and
     * {@link #save()} scrolls it, so a visibility check goes false on a healthy form.
     */
    public boolean isLoaded(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        do {
            if (present("btnSave") || present("tvFormTitle")) return true;
            pause(400);
        } while (System.currentTimeMillis() < deadline);
        return false;
    }

    public String formTitle() {
        return text("tvFormTitle");
    }

    // ---------------- the city itself ----------------

    /** The Places search field — the form's only way to name a city on the add path. */
    public boolean searchFieldShown() { return lastWithId("etCitySearch") != null; }

    public boolean saveShown() { return lastWithId("btnSave") != null; }

    /**
     * Would a user be able to type a city name into the search field?
     *
     * <p>Asks the view, not the outcome of a sendKeys: the field is an
     * {@code AutoCompleteTextView} that the fragment deliberately turns into a launcher for
     * the Places autocomplete, so it reports itself as not enabled for input while still
     * being present and clickable.
     */
    public boolean searchFieldAcceptsTyping() {
        WebElement e = lastWithId("etCitySearch");
        if (e == null) return false;
        try {
            return e.isEnabled() && Boolean.parseBoolean(e.getAttribute("focusable"));
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean searchCity(String term) {
        return fill("etCitySearch", term);
    }

    public String cityNameShown() { return text("tvCityName"); }
    public String latShown()      { return text("tvLat"); }
    public String lngShown()      { return text("tvLng"); }

    /** True once a place has been picked — lat/lng stop reading "Auto-filled". */
    public boolean placePicked() {
        String lat = latShown();
        return lat != null && !lat.isBlank() && !lat.equalsIgnoreCase("Auto-filled");
    }

    // ---------------- center point ----------------

    public boolean useCenterPointMode() { return clickIfPresent("btnCenterPoint"); }
    public boolean useIntersectionMode() { return clickIfPresent("btnIntersection"); }

    public boolean searchCenterPoint(String term) {
        return fill("etCenterSearch", term);
    }

    public boolean enterStreets(String one, String two) {
        boolean a = one == null || fill("etStreet1", one);
        boolean b = two == null || fill("etStreet2", two);
        return a && b;
    }

    public boolean findIntersection() { return clickIfPresent("btnFindIntersection"); }

    public String intersectionResult() { return text("tvIntersectionResult"); }
    public String centerLat() { return text("tvCenterLat"); }
    public String centerLng() { return text("tvCenterLng"); }

    // ---------------- radius ----------------

    /**
     * True when the "Center Point &amp; Zone" section is on screen at all.
     *
     * <p>It is {@code View.GONE} in EDIT mode — {@code parseEditCity()} hides both the
     * section and its divider — so a city's centre point and radius cannot be changed
     * after creation. In ADD mode the section exists but its CONTENT is collapsed until
     * {@link #enableCenterPoint()} flips the switch.
     */
    public boolean centerPointSectionShown() {
        return present("sectionCenterPoint") || present("switchCenterPoint");
    }

    /**
     * Flips the Center Point switch, where one exists.
     *
     * <p><b>It does not, in this build.</b> {@code fragment_city_form.xml} declares
     * {@code sectionCenterPoint} with {@code android:visibility="gone"}, and nothing in
     * {@code CityFormFragment} ever sets it visible — the only reference is
     * {@code parseEditCity()} setting it GONE again. So the switch, the Center Point /
     * Street Intersection toggle, the intersection finder and the radius chips are all
     * unreachable from the app, in add mode and edit mode alike.
     *
     * <p>Kept, and kept honest: it returns false rather than pretending, and the cases
     * that need it skip with that reason. If the section is ever re-enabled this starts
     * working with no change here.
     */
    public boolean enableCenterPoint() {
        if (present("chipGroupRadius")) return true;      // already expanded
        if (!present("switchCenterPoint")) return false;  // not in this build
        scrollToId("switchCenterPoint");
        return clickIfPresent("switchCenterPoint");
    }

    /**
     * Preset radius chips (0 / 30 / 40 / 50 / 60 plus a custom chip).
     *
     * <p>Two things hide these, and both have bitten this suite: the form is taller than
     * the screen, and the whole section is collapsed behind {@code switchCenterPoint}
     * until it is switched on. So enable, then scroll, then read.
     */
    public List<String> radiusChips() {
        enableCenterPoint();
        scrollToId("chipGroupRadius");
        return textsIn("chipGroupRadius", null);
    }

    public boolean pickRadiusChip(String label) {
        try {
            driver.findElement(AppiumBy.xpath(
                    "//*[@resource-id='" + appPackage() + ":id/chipGroupRadius']"
                            + "//*[@text='" + label + "']")).click();
            pause(700);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Custom radius in miles. Overrides whichever chip is selected. */
    public boolean setCustomRadius(String miles) {
        enableCenterPoint();
        scrollToId("etCustomRadius");
        return fill("etCustomRadius", miles);
    }

    // ---------------- save / cancel ----------------

    public boolean save()   { scrollToId("btnSave"); return clickIfPresent("btnSave"); }
    public boolean cancel() { return clickIfPresent("btnCancel"); }
    public boolean close()  { return clickIfPresent("btnClose"); }

    /** True while the form is still up — i.e. a save was rejected. */
    public boolean stillOpen() {
        return isLoaded(Duration.ofSeconds(3));
    }

    // ---------------- city rows (item_city_card.xml) ----------------

    /**
     * City names in the panel.
     *
     * <p>The panel's rows are {@code item_city.xml}: {@code tvCityName},
     * {@code tvCityDescription}, {@code btnCityEdit}, {@code btnCityDelete}. They are NOT
     * {@code item_city_card.xml} ({@code tvName} / {@code btnEdit} / {@code btnDelete}),
     * which belongs to the full-screen {@code cityListFragment}. Using the card's ids here
     * finds nothing and every row action silently reports "not available".
     */
    public List<String> cityNames() {
        return textsIn("rvCities", "tvCityName");
    }

    public int cityCount() {
        return cityNames().size();
    }

    /** The "N cities" counter above the list. */
    public String countText() {
        return text("tvCount");
    }

    /**
     * Taps a per-row button on the city identified by name.
     *
     * <p>Tries an ancestor XPath first, then falls back to pairing by index. The fallback
     * matters: {@code ancestor::*[...][1]} is evaluated inconsistently by UiAutomator's
     * XPath, sometimes returning the OUTERMOST matching ancestor rather than the nearest,
     * which taps the wrong row's button — or nothing, silently.
     */
    private boolean tapRowButton(String cityName, String buttonId) {
        try {
            driver.findElement(AppiumBy.xpath(
                    "//*[@resource-id='" + appPackage() + ":id/tvCityName' and @text='" + cityName + "']"
                            + "/ancestor::*[.//*[@resource-id='" + appPackage() + ":id/" + buttonId + "']][1]"
                            + "//*[@resource-id='" + appPackage() + ":id/" + buttonId + "']")).click();
            pause(1200);
            return true;
        } catch (Exception ignored) { }
        try {
            List<String> names = cityNames();
            int idx = names.indexOf(cityName);
            if (idx < 0) return false;
            List<WebElement> btns = driver.findElements(byId(buttonId));
            if (idx >= btns.size()) return false;
            btns.get(idx).click();
            pause(1200);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean editCity(String name)   { return tapRowButton(name, "btnCityEdit"); }
    public boolean deleteCity(String name) { return tapRowButton(name, "btnCityDelete"); }

    // ---------------- helpers ----------------

    private String text(String id) {
        try {
            List<WebElement> els = driver.findElements(byId(id));
            return els.isEmpty() ? null : els.get(0).getText();
        } catch (Exception e) { return null; }
    }

    private List<String> textsIn(String containerId, String childId) {
        List<String> out = new ArrayList<>();
        String child = childId == null
                ? "*"
                : "*[@resource-id='" + appPackage() + ":id/" + childId + "']";
        try {
            for (WebElement e : driver.findElements(AppiumBy.xpath(
                    "//*[@resource-id='" + appPackage() + ":id/" + containerId + "']//" + child))) {
                String t = e.getText();
                if (t != null && !t.isBlank()) out.add(t.trim());
            }
        } catch (Exception ignored) { }
        return out;
    }

    private boolean present(String id) {
        try { return !driver.findElements(byId(id)).isEmpty(); }
        catch (Exception e) { return false; }
    }

    private boolean clickIfPresent(String id) {
        scrollFormTo(id);
        WebElement e = lastWithId(id);
        if (e == null) return false;
        try {
            e.click();
            pause(1200);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * The element with this id that a user could actually touch.
     *
     * <p>{@code etCitySearch} exists on the Map's city PANEL and again on the city FORM
     * that opens over it, so an id lookup here routinely returns two matches. Taking the
     * first types into the panel's field, behind the form, and the case skips with
     * "Could not type a city name".
     *
     * <p>Taking the LAST was the first fix and it was a guess: view order is not screen
     * order, and the panel's copy can perfectly well come second. Displayed-and-enabled
     * is the property that actually distinguishes them — the hidden one is neither — so
     * that is what this picks, keeping position only as the final tie-break.
     */
    protected WebElement lastWithId(String id) {
        try {
            List<WebElement> els = driver.findElements(byId(id));
            if (els.isEmpty()) return null;
            WebElement displayed = null;
            for (WebElement e : els) {
                try {
                    if (!e.isDisplayed()) continue;
                    if (e.isEnabled()) return e;
                    if (displayed == null) displayed = e;
                } catch (Exception ignored) {
                    // Stale between the find and the question — the next candidate answers.
                }
            }
            return displayed != null ? displayed : els.get(els.size() - 1);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Scrolls a tall form until {@code id} is on screen — but only for the fields that
     * are actually below the fold.
     *
     * <p>The id whitelist is not an optimisation, it is a stability fix. {@code UiScrollable}
     * is an expensive UiAutomator call, and running one on every field interaction —
     * including the many that are already visible, and the ones that are legitimately
     * absent — overloaded the UiAutomator2 server until it died mid-run with
     * "unknown server-side error", taking whole suites down with it.
     */
    protected boolean scrollFormTo(String id) {
        if (!driver.findElements(byId(id)).isEmpty()) return true;
        if (!SCROLLABLE_FIELDS.contains(id)) return false;
        // BOUNDED, AND NO UiScrollable.
        //
        // This used UiScrollable with its default 30 search swipes. A field that is not on
        // the form at all — etCustomRadius before Custom is picked, say — is never found,
        // so every such lookup paid a full 30-swipe sweep. On the ZONE form that happens
        // on a screen carrying a live Google Map (fragment_studio_zone_form.xml embeds a
        // SupportMapFragment), which is the one condition under which this suite loses the
        // UiAutomator2 instrumentation. Measured: the zone suite rebuilt its session 2-6
        // times per run while the lifecycle suite, after the same class of fix, rebuilt 0.
        //
        // Four short swipes reach the bottom of any of these forms.
        try {
            org.openqa.selenium.Dimension size = driver.manage().window().getSize();
            int x = size.getWidth() / 2;
            int fromY = (int) (size.getHeight() * 0.72);
            int toY = (int) (size.getHeight() * 0.38);
            for (int i = 0; i < 4; i++) {
                swipeVertically(x, fromY, toY);
                pause(400);
                if (!driver.findElements(byId(id)).isEmpty()) return true;
            }
        } catch (Exception ignored) { }
        return !driver.findElements(byId(id)).isEmpty();
    }

    /** Fields known to sit below the fold on these forms. */
    private static final java.util.Set<String> SCROLLABLE_FIELDS = java.util.Set.of(
            "btnSave", "btnCancel", "chipGroupRadius", "etCustomRadius",
            "switchCenterPoint", "sectionCenterPoint", "etCenterSearch",
            "etStreet1", "etStreet2", "btnFindIntersection", "etZoneName",
            "etDescription", "etScene", "etAddress", "uploadArea");

    private boolean fill(String id, String value) {   // NB: not typeInto — BasePage already has that name, with different semantics
        scrollFormTo(id);
        WebElement f = lastWithId(id);
        if (f == null) return false;
        try {
            f.click();
            f.clear();
            f.sendKeys(value);
            pause(1000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void pause(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }

    /** One short vertical drag. Used instead of UiScrollable, which is far more expensive. */
    private void swipeVertically(int x, int fromY, int toY) {
        org.openqa.selenium.interactions.PointerInput finger =
                new org.openqa.selenium.interactions.PointerInput(
                        org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        org.openqa.selenium.interactions.Sequence seq =
                new org.openqa.selenium.interactions.Sequence(finger, 1);
        seq.addAction(finger.createPointerMove(java.time.Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, fromY));
        seq.addAction(finger.createPointerDown(0));
        seq.addAction(finger.createPointerMove(java.time.Duration.ofMillis(420),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, toY));
        seq.addAction(finger.createPointerUp(0));
        driver.perform(java.util.List.of(seq));
    }
}
