package com.zillit.automation.pages.map;

import com.zillit.automation.pages.common.BasePage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Map &rarr; <b>location form</b> ({@code fragment_location_form.xml}) and the pin detail
 * sheet ({@code bottomsheet_view_pinned_location.xml}).
 *
 * <p>Reached two ways — by dropping a pin in pin mode, or from {@code btnAddLocation} in
 * the location list. The form validates name and address only; coordinates arrive from
 * whichever route opened it.
 *
 * <p>One behaviour here belongs to the boundary family rather than to the form: picking an
 * address that falls outside the selected city raises its own
 * <b>"Outside &lt;city&gt;"</b> / <i>"This address is outside &lt;city&gt;. Do you want to
 * use it anyway?"</i> confirmation, handled through {@link MapDialogPage}.
 */
public class MapLocationFormPage extends BasePage {

    public static final String ERR_NO_NAME = "Please enter a location name";
    public static final String ERR_NO_ADDRESS = "Please enter an address";
    public static final String ERR_MAX_MEDIA = "Maximum 10 media allowed";
    public static final String ERR_TYPE_BLANK = "Type name cannot be empty";

    public MapLocationFormPage(AndroidDriver driver) {
        super(driver);
    }

    /**
     * Is the location form the screen we are on?
     *
     * <p>Asks whether the form's markers EXIST, not whether they are both on screen at
     * once. The previous version required {@code etName} and {@code btnSave} to be visible
     * together, and this form is a tall ScrollView — scrolling down to Save pushes the name
     * field off the top, so the check went false while the form was perfectly healthy.
     * That produced two failures that looked like app defects: "Form closed with no
     * address" (save() scrolls to the button first) and "Form lost after opening the type
     * dropdown" (the dropdown popup covers the fields it was looking at).
     */
    public boolean isLoaded(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        do {
            if (onForm()) return true;
            pause(400);
        } while (System.currentTimeMillis() < deadline);
        return false;
    }

    /** Both ids belong only to this form, so either one present means we are on it. */
    private boolean onForm() {
        return present("etName") || present("btnSave");
    }

    private boolean present(String id) {
        try { return !driver.findElements(byId(id)).isEmpty(); }
        catch (Exception e) { return false; }
    }

    public String formTitle() { return text("tvFormTitle"); }

    public boolean setName(String v)        { return fill("etName", v); }
    public boolean setAddress(String v)     { return fill("etAddress", v); }
    public boolean setDescription(String v) { return fill("etDescription", v); }
    public boolean setScene(String v)       { return fill("etScene", v); }
    public boolean setLat(String v)         { return fill("etLat", v); }
    public boolean setLng(String v)         { return fill("etLng", v); }

    public String nameValue()    { return text("etName"); }
    public String addressValue() { return text("etAddress"); }
    public String sceneValue()   { return text("etScene"); }
    public String typeValue()    { return text("actvType"); }
    public String latValue() { return text("etLat"); }
    public String lngValue() { return text("etLng"); }

    /** Location-type dropdown. */
    public boolean openTypeDropdown() { return clickIfPresent("actvType"); }

    public boolean setType(String v) { return fill("actvType", v); }

    public boolean addNewType() { return clickIfPresent("btnAddNewType"); }

    public boolean addNewTypeShown() { return present("btnAddNewType"); }

    /**
     * Closes the type dropdown's popup window.
     *
     * <p>An {@code AutoCompleteTextView}'s dropdown is a separate window on top of the
     * form. While it is up it swallows taps aimed at the form underneath, so a case that
     * opens it and walks away leaves the app unusable for everything that follows. BACK
     * dismisses the popup without touching the form behind it.
     */
    public boolean dismissTypeDropdown() {
        try {
            driver.navigate().back();
            pause(600);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean subtypesShown() {
        try { return !driver.findElements(byId("chipGroupSubtypes")).isEmpty(); }
        catch (Exception e) { return false; }
    }

    public List<String> subtypeLabels() {
        return textsIn("chipGroupSubtypes", null);
    }

    public boolean sceneFieldShown() {
        try { return !driver.findElements(byId("etScene")).isEmpty(); }
        catch (Exception e) { return false; }
    }

    public boolean openUploadArea() { return clickIfPresent("uploadArea"); }

    public int photoCount() {
        try { return driver.findElements(byId("rvPhotos")).isEmpty() ? 0
                : driver.findElements(AppiumBy.xpath(
                    "//*[@resource-id='" + appPackage() + ":id/rvPhotos']/*")).size(); }
        catch (Exception e) { return 0; }
    }

    public boolean save()   { scrollToId("btnSave"); return clickIfPresent("btnSave"); }
    public boolean cancel() { return clickIfPresent("btnCancel"); }
    public boolean close()  { return clickIfPresent("btnClose"); }

    /**
     * Did the form survive whatever just happened to it?
     *
     * <p>Presence, not visibility — see {@link #isLoaded(Duration)}. A rejected save keeps
     * the form mounted but leaves it scrolled to the Save button, with the name field out
     * of view.
     */
    public boolean stillOpen() {
        return isLoaded(Duration.ofSeconds(3));
    }

    // ---------------- pin detail sheet ----------------

    public boolean detailSheetShown(Duration timeout) {
        return isVisible("tvLatLng", timeout) || isVisible("tvCopyLink", timeout);
    }

    public String detailName()    { return text("tvName"); }
    public String detailLatLng()  { return text("tvLatLng"); }
    public String detailAddress() { return text("tvAddress"); }
    public String detailType()    { return text("tvType"); }
    public String detailSubType() { return text("tvSubType"); }
    public String detailDesc()    { return text("tvDesc"); }

    public boolean copyLink()        { return clickIfPresent("tvCopyLink"); }
    public boolean closeDetail()     { return clickIfPresent("btnClose"); }

    /**
     * Edit/Delete on the detail sheet.
     *
     * <p>{@code bottomsheet_view_pinned_location.xml} declares {@code btnEdit} and
     * {@code btnDelete} TWICE — once in the name row and once in the type row — so a
     * count of these is not a count of actions available. Tests assert on presence only.
     */
    public boolean detailEditShown() {
        try { return !driver.findElements(byId("btnEdit")).isEmpty(); }
        catch (Exception e) { return false; }
    }

    public boolean detailDeleteShown() {
        try { return !driver.findElements(byId("btnDelete")).isEmpty(); }
        catch (Exception e) { return false; }
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
