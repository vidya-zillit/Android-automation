package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;

/**
 * RECCE create/edit form (RecceFormFragment / fragment_recce_form.xml).
 *
 * Required to PUBLISH (first missing wins): fTitle → fUnit → fDate → fRdvTime →
 * fRdvPlace, each toasting "<label> is required". SAVING A DRAFT SKIPS ALL
 * VALIDATION — that asymmetry is the point of several tests.
 *
 * Stops live in the fStops LinearLayout (rows: stopTimeBtn / stopKind /
 * stopPlace / stopPickLocation / stopContact / stopEndTimeBtn / stopNotes /
 * stopTravel / stopDelete — delete has NO confirm). People live in fPeople
 * (personName / personRole / personEmail / personDelete). There is NO stop
 * reordering and NO attachment picker on this form.
 */
public class RecceFormPage extends BasePage {

    public RecceFormPage(AndroidDriver driver) {
        super(driver);
    }

    public boolean isLoaded(Duration timeout) {
        return isVisible("fTitle", timeout)
                || textShown("NEW RECCE", Duration.ofSeconds(3))
                || isVisible("formScroll", Duration.ofSeconds(3));
    }

    public boolean isEditMode() { return textShown("EDIT RECCE", Duration.ofSeconds(3)); }

    /** Cheap "is the form up?" — no implicit waits, for the fast seeding loop. */
    public boolean isFormUp() {
        try { return !driver.findElements(byId("fTitle")).isEmpty(); }
        catch (WebDriverException e) { return false; }
    }

    /** Types the title directly (it is at the top of the form — no scrolling). */
    public boolean setTitleFast(String text) {
        try {
            WebElement f = driver.findElement(byId("fTitle"));
            f.click();
            f.sendKeys(text);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    /**
     * FAST draft save for bulk seeding: taps Save-as-Draft and returns as soon as
     * the form has gone (polling), instead of sleeping a fixed 3s. The action bar
     * is pinned at the bottom of the form, so no scrolling is needed.
     */
    public boolean saveDraftFast(Duration timeout) {
        try { driver.findElement(byId("fSaveDraft")).click(); }
        catch (WebDriverException e) { return false; }
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (!isFormUp()) return true;      // navigated away → saved
            sleep(150);
        }
        return false;
    }

    // ---------------- simple fields ----------------

    public void setTitle(String t)       { type("fTitle", t); }
    public void setStation(String t)     { type("fStation", t); }
    public void setRdvPlace(String t)    { type("fRdvPlace", t); }
    public void setCrewNote(String t)    { type("fCrewNote", t); }
    public void setWeatherLow(String t)  { type("fWeatherLow", t); }
    public void setWeatherHigh(String t) { type("fWeatherHigh", t); }
    public void setWeatherConditions(String t) { type("fWeatherConditions", t); }

    public String titleText() { return textOf("fTitle"); }
    public String summaryText() { return textOf("fSummary"); }

    private void type(String id, String text) {
        try {
            scrollTo(id);
            WebElement f = driver.findElement(byId(id));
            f.click();
            f.clear();
            if (text != null && !text.isEmpty()) f.sendKeys(text);
            hideKeyboard();
        } catch (WebDriverException ignored) { }
    }

    private String textOf(String id) {
        try { return driver.findElement(byId(id)).getText(); }
        catch (WebDriverException e) { return ""; }
    }

    /**
     * Scrolls the form until the given id is on screen. Searches DOWN first, and
     * if that fails rewinds to the TOP and searches down again — otherwise a field
     * that is above the current scroll position can never be reached.
     */
    public boolean scrollTo(String id) {
        if (isPresent(id)) return true;
        for (int i = 0; i < 8; i++) {
            swipeUp();
            if (isPresent(id)) return true;
        }
        scrollToTop();
        for (int i = 0; i < 10; i++) {
            if (isPresent(id)) return true;
            swipeUp();
        }
        return isPresent(id);
    }

    private boolean isPresent(String id) {
        try { return !driver.findElements(byId(id)).isEmpty(); }
        catch (WebDriverException e) { return false; }
    }

    private void scrollToTop() {
        for (int i = 0; i < 10; i++) swipeDown();
    }

    private void swipeDown() {
        var size = driver.manage().window().getSize();
        var finger = new org.openqa.selenium.interactions.PointerInput(
                org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        var seq = new org.openqa.selenium.interactions.Sequence(finger, 1);
        seq.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(),
                size.getWidth() / 2, (int) (size.getHeight() * 0.35)));
        seq.addAction(finger.createPointerDown(0));
        seq.addAction(finger.createPointerMove(Duration.ofMillis(300),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(),
                size.getWidth() / 2, (int) (size.getHeight() * 0.8)));
        seq.addAction(finger.createPointerUp(0));
        driver.perform(java.util.Collections.singletonList(seq));
        sleep(400);
    }

    // ---------------- unit dropdown ----------------

    public boolean pickUnit(String unitContains) {
        try {
            scrollTo("fUnit");
            driver.findElement(byId("fUnit")).click();
            sleep(1500);
            // Match a dropdown row (android:id/text1), never an arbitrary TextView.
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceId(\"android:id/text1\").textContains(\""
                            + unitContains + "\")")).click();
            sleep(1200);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    /**
     * Picks the first entry of the unit dropdown and returns its label. The unit
     * field is a MaterialAutoCompleteTextView, so its options come up in a
     * ListPopupWindow whose rows are `android:id/text1` — matching on "any
     * clickable TextView" instead grabs a form field (e.g. the date row).
     */
    public String pickFirstUnit() {
        try {
            scrollTo("fUnit");
            String preset = textOf("fUnit");
            driver.findElement(byId("fUnit")).click();
            sleep(1500);
            var items = driver.findElements(AppiumBy.id("android:id/text1"));
            if (items.isEmpty()) {
                // popup didn't open (or it's already set) — keep whatever is there
                hideKeyboard();
                return preset;
            }
            String label = items.get(0).getText();
            items.get(0).click();
            sleep(1200);
            return label == null ? "" : label;
        } catch (WebDriverException e) { return ""; }
    }

    public String unitText() { return textOf("fUnit"); }

    // ---------------- date / time pickers ----------------

    /**
     * Opens the date picker, SELECTS a day, then confirms. The MaterialDatePicker
     * opens with nothing selected (today is only outlined) and its OK button is
     * DISABLED until a day is tapped — confirming straight away silently does
     * nothing and the date stays empty.
     */
    public boolean pickDate() {
        try {
            boolean found = scrollTo("fDate");
            driver.findElement(byId("fDate")).click();
            sleep(2000);
            boolean day = selectADay();
            boolean ok = confirmMaterialPicker();
            String after = textOf("fDate");
            System.out.println(">>> pickDate: scrolled=" + found + " daySelected=" + day
                    + " confirmed=" + ok + " fDate='" + after + "'");
            return day && ok;
        } catch (WebDriverException e) {
            System.out.println(">>> pickDate error: " + e.getMessage());
            return false;
        }
    }

    /** Taps a day cell in the open MaterialDatePicker grid. */
    private boolean selectADay() {
        // Prefer today's day-of-month, then a few safe mid-month fallbacks.
        java.util.List<String> days = new java.util.ArrayList<>();
        days.add(String.valueOf(java.time.LocalDate.now().getDayOfMonth()));
        for (String d : new String[]{"15", "10", "12", "20", "5"}) if (!days.contains(d)) days.add(d);
        for (String d : days) {
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiSelector().clickable(true).text(\"" + d + "\")")).click();
                sleep(900);
                return true;
            } catch (WebDriverException ignored) { }
            // day cells are sometimes only reachable by their description ("31 July 2026")
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiSelector().descriptionStartsWith(\"" + d + " \")")).click();
                sleep(900);
                return true;
            } catch (WebDriverException ignored) { }
        }
        return false;
    }

    /** Opens the RDV time picker and accepts the shown time. */
    public boolean pickRdvTime() {
        try {
            scrollTo("fRdvTime");
            driver.findElement(byId("fRdvTime")).click();
            sleep(2000);
            boolean ok = confirmMaterialPicker();
            String after = textOf("fRdvTime");
            System.out.println(">>> pickRdvTime: confirmed=" + ok + " fRdvTime='" + after + "'");
            return ok;
        } catch (WebDriverException e) {
            System.out.println(">>> pickRdvTime error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Confirms a MaterialDatePicker / MaterialTimePicker. These use their own
     * button ids ({@code confirm_button}, {@code material_timepicker_ok_button})
     * — a generic "OK" text match does not find them reliably.
     */
    private boolean confirmMaterialPicker() {
        String[] ids = {"confirm_button", "material_timepicker_ok_button"};
        for (String id : ids) {
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiSelector().resourceIdMatches(\".*:id/" + id + "\")")).click();
                sleep(1500);
                return true;
            } catch (WebDriverException ignored) { }
        }
        for (String pat : new String[]{"OK", "Ok", "Save", "Done", "Set"}) {
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiSelector().clickable(true).textMatches(\"(?i)" + pat + "\")")).click();
                sleep(1500);
                return true;
            } catch (WebDriverException ignored) { }
        }
        return false;
    }

    public String dateText()    { return textOf("fDate"); }
    public String rdvTimeText() { return textOf("fRdvTime"); }

    // ---------------- location picker ----------------

    /** Opens the rendezvous location picker (map + W3W + pin). */
    public boolean openRdvLocationPicker() {
        try {
            scrollTo("fRdvPick");
            driver.findElement(byId("fRdvPick")).click();
            sleep(2500);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    public boolean isLocationPickerOpen(Duration timeout) {
        return isVisible("pickerUse", timeout)
                || isVisible("pickerMap", Duration.ofSeconds(3))
                || isVisible("pickerW3w", Duration.ofSeconds(2));
    }

    public void setPickerW3w(String words) {
        try {
            WebElement f = driver.findElement(byId("pickerW3w"));
            f.click(); f.clear(); f.sendKeys(words); hideKeyboard();
        } catch (WebDriverException ignored) { }
    }

    public String pickerPinText() {
        try { return driver.findElement(byId("pickerPin")).getText(); }
        catch (WebDriverException e) { return ""; }
    }

    public boolean tapPickerUse() {
        try { driver.findElement(byId("pickerUse")).click(); sleep(2000); return true; }
        catch (WebDriverException e) { return false; }
    }

    public boolean closeLocationPicker() {
        try { driver.findElement(byId("pickerClose")).click(); sleep(1200); return true; }
        catch (WebDriverException e) {
            try { driver.navigate().back(); sleep(1000); return true; }
            catch (WebDriverException ignored) { return false; }
        }
    }

    // ---------------- stops ----------------

    /**
     * The number of stops on the form, taken from the app's OWN summary line
     * ("N stops · M personnel").
     *
     * <p>Counting {@code stopPlace} rows is WRONG: only ~3 stop rows fit on screen
     * and Android exposes just the rendered ones, so the count freezes at 3 no
     * matter how many stops exist. The summary is the reliable source of truth.
     */
    public int stopCount() {
        int n = countFromSummary("stops");
        if (n >= 0) return n;
        try { return driver.findElements(byId("stopPlace")).size(); }   // fallback
        catch (WebDriverException e) { return 0; }
    }

    /** Personnel count from the same summary line. */
    public int personCountFromSummary() { return countFromSummary("personnel"); }

    private int countFromSummary(String word) {
        String s = summaryText();
        if (s == null || s.isBlank()) return -1;
        java.util.regex.Matcher m =
                java.util.regex.Pattern.compile("(\\d+)\\s*" + word).matcher(s.toLowerCase());
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    /** Stop rows currently RENDERED (only ~3 fit on screen) — rarely what you want. */
    public int visibleStopRows() {
        try { return driver.findElements(byId("stopPlace")).size(); }
        catch (WebDriverException e) { return 0; }
    }

    public boolean addStop() {
        try {
            scrollTo("fAddStop");
            driver.findElement(byId("fAddStop")).click();
            sleep(1500);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    /** Deletes the LAST stop row (stopDelete has no confirm dialog). */
    public boolean deleteLastStop() {
        try {
            var dels = driver.findElements(byId("stopDelete"));
            if (dels.isEmpty()) return false;
            dels.get(dels.size() - 1).click();
            sleep(1500);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    public void setStopPlace(int index, String text) {
        try {
            var places = driver.findElements(byId("stopPlace"));
            if (index >= places.size()) return;
            WebElement f = places.get(index);
            f.click(); f.clear(); f.sendKeys(text); hideKeyboard();
        } catch (WebDriverException ignored) { }
    }

    public String stopPlaceText(int index) {
        try {
            var places = driver.findElements(byId("stopPlace"));
            return index < places.size() ? places.get(index).getText() : "";
        } catch (WebDriverException e) { return ""; }
    }

    /** All stop place values currently on the form, in order. */
    public java.util.List<String> stopPlaces() {
        try {
            return driver.findElements(byId("stopPlace")).stream()
                    .map(WebElement::getText).toList();
        } catch (WebDriverException e) { return java.util.List.of(); }
    }

    /**
     * Sets a stop's KIND via its dropdown. Options: Rendezvous / Start /
     * Continue / Lunch / End. Dropdown rows are android:id/text1.
     */
    public boolean setStopKind(int index, String kind) {
        try {
            var kinds = driver.findElements(byId("stopKind"));
            if (index >= kinds.size()) return false;
            kinds.get(index).click();
            sleep(1200);
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceId(\"android:id/text1\").textContains(\"" + kind + "\")")).click();
            sleep(1000);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    public String stopKindText(int index) {
        try {
            var kinds = driver.findElements(byId("stopKind"));
            return index < kinds.size() ? kinds.get(index).getText() : "";
        } catch (WebDriverException e) { return ""; }
    }

    /** Sets a stop's start time via its time picker. */
    public boolean setStopTime(int index) {
        try {
            var btns = driver.findElements(byId("stopTimeBtn"));
            if (index >= btns.size()) return false;
            btns.get(index).click();
            sleep(1800);
            return confirmMaterialPicker();
        } catch (WebDriverException e) { return false; }
    }

    // ---------------- people ----------------

    public int personCount() {
        try { return driver.findElements(byId("personName")).size(); }
        catch (WebDriverException e) { return 0; }
    }

    public boolean addPerson() {
        try {
            scrollTo("fAddPerson");
            driver.findElement(byId("fAddPerson")).click();
            sleep(1500);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    public boolean openAddFromCrew() {
        try {
            scrollTo("fAddFromCrew");
            driver.findElement(byId("fAddFromCrew")).click();
            sleep(2500);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    public void setPerson(int index, String name, String role, String email) {
        setPersonField("personName", index, name);
        setPersonField("personRole", index, role);
        setPersonField("personEmail", index, email);
    }

    private void setPersonField(String id, int index, String text) {
        if (text == null) return;
        try {
            var els = driver.findElements(byId(id));
            if (index >= els.size()) return;
            WebElement f = els.get(index);
            f.click(); f.clear(); f.sendKeys(text); hideKeyboard();
        } catch (WebDriverException ignored) { }
    }

    // ---------------- save ----------------

    public boolean saveDraft() {
        try {
            scrollTo("fSaveDraft");
            driver.findElement(byId("fSaveDraft")).click();
            sleep(3000);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    public boolean publish() {
        try {
            scrollTo("fPublish");
            driver.findElement(byId("fPublish")).click();
            sleep(3000);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    /** A "<field> is required" validation toast/message is showing. */
    public boolean requiredWarningShown(String fieldWord, Duration timeout) {
        return textShown(fieldWord, timeout) || textShown("is required", Duration.ofSeconds(2));
    }

    public boolean anyRequiredWarningShown(Duration timeout) {
        return textShown("is required", timeout);
    }

    public void back() {
        try { driver.findElement(byId("backBtn")).click(); }
        catch (WebDriverException e) {
            try { driver.navigate().back(); } catch (WebDriverException ignored) { }
        }
        sleep(1500);
    }

    private void swipeUp() {
        var size = driver.manage().window().getSize();
        var finger = new org.openqa.selenium.interactions.PointerInput(
                org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        var seq = new org.openqa.selenium.interactions.Sequence(finger, 1);
        seq.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(),
                size.getWidth() / 2, (int) (size.getHeight() * 0.75)));
        seq.addAction(finger.createPointerDown(0));
        seq.addAction(finger.createPointerMove(Duration.ofMillis(350),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(),
                size.getWidth() / 2, (int) (size.getHeight() * 0.35)));
        seq.addAction(finger.createPointerUp(0));
        driver.perform(java.util.Collections.singletonList(seq));
        sleep(600);
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
