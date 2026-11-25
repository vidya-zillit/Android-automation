package com.zillit.automation.pages.map;

import com.zillit.automation.pages.common.BasePage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Map &rarr; <b>Studio zones</b> — the list ({@code fragment_studio_zone_list.xml}) and the
 * form ({@code fragment_studio_zone_form.xml}), reached from {@code btnZoneList}.
 *
 * <p>A studio zone is a named circle <i>inside</i> a city. Applying one with
 * {@code btnSelect} changes how the whole Map behaves afterwards: {@code evaluatePinBoundary()}
 * tests the <b>zone first</b> and only falls through to the city when the point misses it,
 * which is what makes the "outside the zone but inside the city" verdict reachable at all.
 *
 * <p>The form's own validation is the inverse of what people expect: it checks that the
 * <b>city centre sits inside the proposed zone</b>, not that the zone sits inside the city.
 * A zone drawn too small around a point far from the city is rejected with the distance
 * spelled out, before any API call.
 */
public class MapZonePage extends BasePage {

    public static final String ERR_NO_NAME = "Please enter a zone name";
    public static final String ERR_NO_CENTER = "Please search and select a center point location";
    public static final String ERR_NO_STREETS = "Please enter both street names for the intersection";
    public static final String ERR_RADIUS_ZERO = "Radius must be greater than 0";
    /** Fragment of the city-outside-zone rejection; the full text carries the distance. */
    public static final String ERR_CITY_OUTSIDE = "from the center point, but zone radius is";

    public MapZonePage(AndroidDriver driver) {
        super(driver);
    }

    // ---------------- list ----------------

    public boolean listLoaded(Duration timeout) {
        return isVisible("rvZones", timeout) || isVisible("tvTitle", timeout);
    }

    public String listTitle() { return text("tvTitle"); }
    public String countText() { return text("tvCount"); }

    public boolean emptyStateShown(Duration timeout) {
        return isVisible("emptyState", timeout);
    }

    public boolean searchZones(String term) {
        return fill("etZoneSearch", term);
    }

    public boolean openAddZone() { return clickIfPresent("btnAdd"); }

    public List<String> zoneNames() {
        return textsIn("rvZones", "tvName");
    }

    public List<String> zoneRadii() {
        return textsIn("rvZones", "tvRadius");
    }

    public int zoneCount() { return zoneNames().size(); }

    /**
     * Taps a per-row button on the row identified by name.
     *
     * <p>XPath first, then an index pairing. {@code ancestor::*[...][1]} is evaluated
     * inconsistently by UiAutomator's XPath — it can return the outermost matching
     * ancestor rather than the nearest — which taps another row's button, or nothing at
     * all, with no error to show for it.
     */
    private boolean tapRowButton(String zoneName, String buttonId) {
        try {
            driver.findElement(AppiumBy.xpath(
                    "//*[@resource-id='" + appPackage() + ":id/tvName' and @text='" + zoneName + "']"
                            + "/ancestor::*[.//*[@resource-id='" + appPackage() + ":id/" + buttonId + "']][1]"
                            + "//*[@resource-id='" + appPackage() + ":id/" + buttonId + "']")).click();
            pause(1300);
            return true;
        } catch (Exception ignored) { }
        try {
            List<String> names = zoneNames();
            int idx = names.indexOf(zoneName);
            if (idx < 0) return false;
            List<WebElement> btns = driver.findElements(byId(buttonId));
            if (idx >= btns.size()) return false;
            btns.get(idx).click();
            pause(1300);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Applies the zone to the map — this is what makes it the ACTIVE boundary. */
    public boolean selectZone(String name) { return tapRowButton(name, "btnSelect"); }
    public boolean viewZone(String name)   { return tapRowButton(name, "btnView"); }
    public boolean editZone(String name)   { return tapRowButton(name, "btnEdit"); }
    public boolean deleteZone(String name) { return tapRowButton(name, "btnDelete"); }

    // ---------------- form ----------------

    public boolean formLoaded(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        do {
            // Presence, not visibility: the form scrolls and save() scrolls it.
            if (present("etZoneName") || present("btnSave")) return true;
            pause(400);
        } while (System.currentTimeMillis() < deadline);
        return false;
    }

    public String formTitle()  { return text("tvFormTitle"); }
    /** The parent city, shown as a badge so the zone's scope is never ambiguous. */
    public String cityBadge()  { return text("tvCityBadge"); }

    public boolean setZoneName(String name) { return fill("etZoneName", name); }

    public boolean useCenterPointMode()  { return clickIfPresent("btnCenterPoint"); }
    public boolean useIntersectionMode() { return clickIfPresent("btnIntersection"); }

    public boolean searchCenterPoint(String term) { return fill("etCenterSearch", term); }

    public boolean enterStreets(String one, String two) {
        boolean a = one == null || fill("etStreet1", one);
        boolean b = two == null || fill("etStreet2", two);
        return a && b;
    }

    // findIntersection() lives with the rest of the intersection API below — it has to
    // WAIT for the geocoding round trip, not just click and return.

    public String lat() { return text("tvLat"); }
    public String lng() { return text("tvLng"); }

    /**
     * Types a custom radius, picking the "Custom" chip first if it has to.
     *
     * <p>{@code etCustomRadius} is {@code visibility="gone"} in the layout and only shown
     * by the Custom chip's listener ({@code StudioZoneFormFragment:156}). Without that tap
     * the field does not exist, which is how "A zero radius is rejected" came to skip with
     * "Custom radius field not available" — the case had no way to express a radius at all.
     *
     * <p>The chip is added in code with the literal text "Custom", so it is matched by
     * text rather than by id; there is no id to match.
     */
    public boolean setCustomRadius(String miles) {
        if (!present("etCustomRadius")) {
            pickRadiusChip("Custom");
            pause(800);
        }
        scrollToId("etCustomRadius");
        return fill("etCustomRadius", miles);
    }

    /** True once the Custom chip has revealed the free-text radius field. */
    public boolean customRadiusFieldShown() { return present("etCustomRadius"); }

    public boolean pickRadiusChip(String label) {
        // Two tries: after Find Intersection the form is scrolled down to the result and
        // the radius chips are above the fold — absent from the tree, not just hidden.
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                driver.findElement(AppiumBy.xpath(
                        "//*[@resource-id='" + appPackage() + ":id/chipGroupRadius']"
                                + "//*[@text='" + label + "']")).click();
                pause(700);
                return true;
            } catch (Exception e) {
                if (attempt == 0) {
                    // Typing the zone name leaves the IME up; the form resizes and the
                    // chips under the name field fall out of the viewport. Close it and
                    // pull the form up one notch.
                    closeKeyboardIfStablyShown();
                    pause(500);
                    try {
                        org.openqa.selenium.Dimension size = driver.manage().window().getSize();
                        swipeVertically(size.getWidth() / 2, (int) (size.getHeight() * 0.70),
                                (int) (size.getHeight() * 0.45));
                        pause(600);
                    } catch (Exception ignored) { }
                }
            }
        }
        return false;
    }

    /** The form renders a live preview of the circle it is about to save. */
    public boolean mapPreviewShown() {
        try { return !driver.findElements(byId("mapPreview")).isEmpty(); }
        catch (Exception e) { return false; }
    }

    public boolean save()   {
        scrollToId("btnSave");
        if (clickIfPresent("btnSave")) return true;
        // A typed radius or name can leave the IME up; the form resizes and btnSave drops
        // out of the tree ("Save not available" in Z-08c/d). Only then close it and retry —
        // closing it unconditionally sent a BACK that popped the form itself.
        if (!closeKeyboardIfStablyShown()) return false;
        pause(600);
        scrollToId("btnSave");
        return clickIfPresent("btnSave");
    }
    public boolean cancel() { return clickIfPresent("btnCancel"); }
    public boolean close()  { return clickIfPresent("btnClose"); }

    private boolean present(String id) {
        try { return !driver.findElements(byId(id)).isEmpty(); }
        catch (Exception e) { return false; }
    }

    public boolean formStillOpen() {
        return formLoaded(Duration.ofSeconds(3));
    }

    // ---------------- web-gap helpers ----------------

    /** N from the list header "N zones" (all zones, whatever the search shows), or -1. */
    public int totalFromHeader() {
        String t = countText();
        if (t == null) return -1;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)").matcher(t);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    /** The Custom radius field's current text, or null when it is not shown. */
    public String customRadiusValue() { return text("etCustomRadius"); }

    /** The ZONE NAME field's current text. */
    public String zoneNameValue() { return text("etZoneName"); }

    /** Clears the zone name — setZoneName("") would leave the IME's last char on some builds. */
    public boolean clearZoneName() {
        scrollFormTo("etZoneName");
        WebElement f = lastWithId("etZoneName");
        if (f == null) return false;
        try { f.click(); f.clear(); pause(600); return true; } catch (Exception e) { return false; }
    }

    /** Taps the "Custom" chip and leaves etCustomRadius EMPTY. */
    public boolean chooseCustomRadiusLeftBlank() {
        if (!present("etCustomRadius")) { pickRadiusChip("Custom"); pause(800); }
        scrollToId("etCustomRadius");
        WebElement f = lastWithId("etCustomRadius");
        if (f == null) return false;
        try { f.clear(); pause(400); return true; } catch (Exception e) { return false; }
    }

    /** Two taps on Save in one gesture — the double-submit case. */
    public boolean doubleTapSave() {
        scrollToId("btnSave");
        WebElement b = lastWithId("btnSave");
        if (b == null) return false;
        try {
            driver.executeScript("mobile: doubleClickGesture",
                    java.util.Map.of("elementId", ((org.openqa.selenium.remote.RemoteWebElement) b).getId()));
            return true;
        } catch (Exception e) {
            try { b.click(); b.click(); return true; } catch (Exception ex) { return false; }
        }
    }

    /** Every zone name, following the list past the bottom of the screen. */
    public List<String> allZoneNames() {
        return readWholeList("rvZones", "tvName", this::zoneNames, s -> s);
    }

    /**
     * How many rows read exactly {@code name} (a double submit makes two). Narrows the
     * list with the zone search first, so the answer never depends on scrolling, then
     * clears the search again.
     */
    public int rowsNamed(String name) {
        boolean searched = searchZones(name);
        pause(1200);
        int n = (int) zoneNames().stream().filter(name::equals).count();
        if (searched) clearZoneSearch();
        return n;
    }

    public void clearZoneSearch() {
        try {
            WebElement f = lastWithId("etZoneSearch");
            if (f != null) { f.clear(); pause(800); }
        } catch (Exception ignored) { }
    }

    /**
     * "N mi radius" on the row named exactly {@code name}, or null when absent.
     *
     * <p>In {@code item_studio_zone_card.xml} the radius sits ABOVE the name, so the usual
     * name-anchored band (name → next name) holds the NEXT card's radius, not this one's —
     * which once made a 4-mile zone read "30 mi". Take the closest tvRadius above the name.
     */
    public String radiusOf(String name) {
        if (!scrollToRow("rvZones", "tvName", name)) return null;
        for (WebElement e : elementsById("tvName")) {
            try {
                if (!name.equals(e.getText() == null ? null : e.getText().trim())) continue;
                int nameY = e.getRect().getY();
                WebElement best = null;
                int bestY = Integer.MIN_VALUE;
                for (WebElement r : elementsById("tvRadius")) {
                    int y = r.getRect().getY();
                    if (y < nameY && y > bestY) { best = r; bestY = y; }
                }
                return best == null ? null : best.getText().trim();
            } catch (Exception ignored) { }
        }
        return null;
    }

    /** Taps a row button on the row named {@code name}, pairing by position. */
    public boolean tapRowByBand(String name, String buttonId) {
        if (!scrollToRow("rvZones", "tvName", name)) return false;
        for (WebElement e : elementsById("tvName")) {
            try {
                if (name.equals(e.getText() == null ? null : e.getText().trim())) {
                    WebElement b = elementIn(bandOfAnchor("tvName", e), buttonId);
                    if (b == null) return false;
                    b.click();
                    pause(1300);
                    return true;
                }
            } catch (Exception ignored) { }
        }
        return false;
    }

    // ---------------- helpers ----------------

    private String text(String id) {
        try {
            List<WebElement> els = driver.findElements(byId(id));
            return els.isEmpty() ? null : els.get(0).getText();
        } catch (Exception e) { return null; }
    }

    private List<String> textsIn(String containerId, String childId) {
        List<String> out = new ArrayList<>();
        try {
            for (WebElement e : driver.findElements(AppiumBy.xpath(
                    "//*[@resource-id='" + appPackage() + ":id/" + containerId + "']"
                            + "//*[@resource-id='" + appPackage() + ":id/" + childId + "']"))) {
                String t = e.getText();
                if (t != null && !t.isBlank()) out.add(t.trim());
            }
        } catch (Exception ignored) { }
        return out;
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
     * The LAST element with this id, which on a pushed form is the form's own.
     *
     * <p>{@code etCitySearch} exists on the Map's city PANEL and again on the city FORM
     * that opens over it. {@code findElements(...).get(0)} returns the panel's — still in
     * the hierarchy, behind the form — so typing goes into a field nobody can see and the
     * case skips with "Could not type a city name". The form is added later, so it is last.
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

    // ──────────────── intersection mode ────────────────
    //
    // THIS IS THE PATH THAT WORKS WITHOUT PLACES, and it is the one a real user takes when
    // they do not want to search for a place. btnFindIntersection reads the two street
    // fields as TEXT and resolves them with the Geocoding HTTP API
    // (StudioZoneFormFragment:279-295, findIntersectionViaGoogleGeocoding, with
    // android.location.Geocoder as fallback). Geocoding is NOT Places Autocomplete — it is
    // the same API that already fills a dropped pin's address on this build.
    //
    // The catch: etStreet1/etStreet2 are isFocusable=false and their CLICK launches Places
    // (:273-276). So the text must be set WITHOUT clicking — sendKeys alone goes through
    // ACTION_SET_TEXT, which does not need focus. fill() clicks first, which is why every
    // earlier attempt bounced into a dead Places overlay and I wrongly concluded that a
    // zone could not be created at all.

    /** Switches the form to street-intersection mode. */
    public boolean chooseIntersectionMode() {
        scrollFormTo("btnIntersection");
        return clickIfPresent("btnIntersection");
    }

    public boolean intersectionFieldsShown() {
        return present("etStreet1") && present("etStreet2");
    }

    public boolean setStreet1(String v) { return setTextWithoutClick("etStreet1", v); }

    public boolean setStreet2(String v) { return setTextWithoutClick("etStreet2", v); }

    /** Taps "Find Intersection" and waits for the button to come back from "Finding...". */
    public boolean findIntersection() {
        scrollFormTo("btnFindIntersection");
        if (!clickIfPresent("btnFindIntersection")) return false;
        long deadline = System.currentTimeMillis() + Duration.ofSeconds(25).toMillis();
        while (System.currentTimeMillis() < deadline) {
            String label = text("btnFindIntersection");
            if (label != null && !label.toLowerCase().contains("finding")) return true;
            pause(500);
        }
        return false;
    }

    /** The resolved intersection line, or null while it is hidden. */
    public String intersectionResult() { return text("tvIntersectionResult"); }

    /**
     * Latitude the form settled on, or null.
     *
     * <p>{@code tvLat} / {@code tvLng} — NOT tvCenterLat/tvCenterLng, which was my guess
     * and does not exist in this layout. StudioZoneFormFragment.applyIntersectionResult
     * (:540-541) writes {@code "%.6f".format(lat)} into these two, so the value is a plain
     * six-decimal number.
     */
    public String centerLat() { return text("tvLat"); }

    public String centerLng() { return text("tvLng"); }

    /** The city this form says the zone belongs to, for cross-checking the coordinate. */
    public String cityBadgeText() { return text("tvCityBadge"); }

    /**
     * Sets a field's text WITHOUT clicking it first.
     *
     * <p>For fields whose click opens something else — the street inputs and the centre
     * search all launch Places Autocomplete on click. {@code sendKeys} by itself is served
     * by {@code ACTION_SET_TEXT}, which ignores focusability, so the value lands in the
     * field and the form's own button can read it.
     */
    private boolean setTextWithoutClick(String id, String value) {
        scrollFormTo(id);
        WebElement f = lastWithId(id);
        if (f == null) return false;
        try {
            f.sendKeys(value);
            pause(800);
            String got = f.getText();
            return got != null && got.trim().toLowerCase().contains(value.trim().toLowerCase());
        } catch (Exception e) {
            return false;
        }
    }

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
