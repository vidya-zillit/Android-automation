package com.zillit.automation.pages.recce;

import com.zillit.automation.pages.common.BasePage;
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

    /**
     * Sets the kind of the LAST stop row (the one {@link #addStop()} just added).
     *
     * <p>Indexing visible rows does not work for that row: a stop row is taller than half the
     * screen, so after scrolling to "Add stop" the new row's header line (time · kind · delete)
     * is usually above the fold and the last visible {@code stopKind} belongs to the row before.
     * With {@code fAddStop} on screen, the lowest {@code stopKind} above it IS the last row's
     * header — so the form is nudged down in small steps until one is showing. [WEB RECCE-02]
     */
    public boolean setLastStopKind(String kind) {
        if (!scrollTo("fAddStop")) { System.out.println(">>> setLastStopKind: fAddStop not found"); return false; }
        WebElement header = null;
        for (int i = 0; i < 8 && header == null; i++) {
            header = lastStopKindAboveAddStop();
            if (header == null) nudgeDown();
        }
        if (header == null) {
            System.out.println(">>> setLastStopKind: no stopKind above fAddStop after nudging");
            return false;
        }
        // The exposed-dropdown popup is drawn but absent from the accessibility tree (same as
        // the External Users dropdowns), so the item is tapped where it is drawn — rows of
        // simple_list_item_1 (48dp) directly under the field, in RecceEnums.Kind.ALL order —
        // and the field is read back to verify, correcting the aim if another kind landed.
        int index = STOP_KINDS.indexOf(kind);
        if (index < 0) return false;
        int aim = index;
        for (int attempt = 0; attempt < 4; attempt++) {
            try {
                org.openqa.selenium.Rectangle r = header.getRect();
                header.click();
                sleep(1300);
                int itemH = dp(48);
                int x = r.getX() + r.getWidth() / 2;
                int y = r.getY() + r.getHeight() + dp(8) + itemH * aim + itemH / 2;
                tapAt(x, y);
                sleep(1000);
                header = lastStopKindAboveAddStop();
                String now = header == null ? "" : String.valueOf(header.getText()).trim();
                System.out.println(">>> setLastStopKind: attempt " + attempt + " aim=" + aim + " → '" + now + "'");
                if (now.equalsIgnoreCase(kind)) return true;
                int landed = STOP_KINDS.indexOf(now);
                if (landed >= 0) aim = Math.max(0, aim + (index - landed));
                if (header == null) return false;
            } catch (WebDriverException e) {
                System.out.println(">>> setLastStopKind: " + e.getClass().getSimpleName());
                return false;
            }
        }
        return false;
    }

    /** RecceEnums.Kind.ALL — the dropdown's order. */
    private static final java.util.List<String> STOP_KINDS =
            java.util.List.of("Rendezvous", "Start", "Continue", "Lunch", "End");

    private int dp(int v) {
        try {
            Object d = driver.getCapabilities().getCapability("deviceScreenDensity");
            if (d != null) {
                int dpi = Integer.parseInt(String.valueOf(d).trim());
                if (dpi > 0) return Math.round(v * dpi / 160f);
            }
        } catch (Exception ignored) { }
        return Math.round(v * 2.625f);
    }

    private void tapAt(int x, int y) {
        var finger = new org.openqa.selenium.interactions.PointerInput(
                org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        var seq = new org.openqa.selenium.interactions.Sequence(finger, 1);
        seq.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, y));
        seq.addAction(finger.createPointerDown(0));
        seq.addAction(finger.createPointerUp(0));
        driver.perform(java.util.Collections.singletonList(seq));
    }

    private WebElement lastStopKindAboveAddStop() {
        try {
            java.util.List<WebElement> add = driver.findElements(byId("fAddStop"));
            if (add.isEmpty()) return null;
            int addY = add.get(0).getRect().getY();
            WebElement best = null;
            int bestY = -1;
            for (WebElement k : driver.findElements(byId("stopKind"))) {
                int y = k.getRect().getY();
                if (y < addY && y > bestY) { best = k; bestY = y; }
            }
            return best;
        } catch (WebDriverException e) { return null; }
    }

    /** Moves the form content DOWN by ~20% of the screen (reveals what is just above). */
    private void nudgeDown() {
        var size = driver.manage().window().getSize();
        var finger = new org.openqa.selenium.interactions.PointerInput(
                org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        var seq = new org.openqa.selenium.interactions.Sequence(finger, 1);
        seq.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(),
                size.getWidth() / 2, (int) (size.getHeight() * 0.40)));
        seq.addAction(finger.createPointerDown(0));
        seq.addAction(finger.createPointerMove(Duration.ofMillis(600),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(),
                size.getWidth() / 2, (int) (size.getHeight() * 0.60)));
        seq.addAction(finger.createPointerUp(0));
        driver.perform(java.util.Collections.singletonList(seq));
        sleep(500);
    }

    /** Text of the last stop row's kind field ("" when unreadable or not on screen). */
    public String lastStopKindText() {
        WebElement k = lastStopKindAboveAddStop();
        if (k == null) return "";
        String t = k.getText();
        return t == null ? "" : t;
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

    // ---------------- crew picker (RecceCrewPickerBottomSheet) [WEB RECCE-01] ----------------
    // Ids: crewSearch, crewRecycler, crewEmpty, crewChosen ("%d chosen"), crewAdd ("Done");
    // rows item_recce_crew.xml: crewName / crewRole / crewCheck. A row tap toggles a set of
    // user ids, and the search rebuilds the list from the full crew (filter()).

    public boolean isCrewPickerOpen(Duration timeout) {
        return isVisible("crewSearch", timeout) || isVisible("crewRecycler", Duration.ofSeconds(2));
    }

    public java.util.List<String> crewNames() {
        try {
            return driver.findElements(byId("crewName")).stream()
                    .map(WebElement::getText).filter(t -> t != null && !t.isBlank()).toList();
        } catch (WebDriverException e) { return java.util.List.of(); }
    }

    /** Types into the picker's search WITHOUT hideKeyboard (a BACK would close the sheet). */
    public void searchCrew(String text) {
        try {
            WebElement f = driver.findElement(byId("crewSearch"));
            f.click(); f.clear();
            if (!text.isEmpty()) f.sendKeys(text);
            sleep(1200);
        } catch (WebDriverException ignored) { }
    }

    /** Index of the row named exactly {@code name}, or -1. Names and checks pair by row order. */
    private int crewRowIndex(String name) {
        java.util.List<WebElement> names = driver.findElements(byId("crewName"));
        for (int i = 0; i < names.size(); i++) {
            if (name.equals(names.get(i).getText())) return i;
        }
        return -1;
    }

    public boolean tapCrewRow(String name) {
        try {
            int i = crewRowIndex(name);
            if (i < 0) return false;
            driver.findElements(byId("crewName")).get(i).click();
            sleep(800);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    /** True / false for the row's crewCheck, or null when the row is not on screen. */
    public Boolean crewChecked(String name) {
        try {
            int i = crewRowIndex(name);
            java.util.List<WebElement> checks = driver.findElements(byId("crewCheck"));
            if (i < 0 || i >= checks.size()) return null;
            return "true".equals(checks.get(i).getAttribute("checked"));
        } catch (WebDriverException e) { return null; }
    }

    public String crewChosenText() {
        try { return driver.findElement(byId("crewChosen")).getText(); }
        catch (WebDriverException e) { return ""; }
    }

    public boolean tapCrewDone() {
        try { driver.findElement(byId("crewAdd")).click(); sleep(1500); return true; }
        catch (WebDriverException e) { return false; }
    }

    /**
     * Every personName value on the form (crew rows are filled with the member's name).
     * The Personnel section sits below the fold, so it is scrolled into view first: the
     * rows ({@code fPeople}) sit BELOW the "Add from project crew" / "Add person" buttons
     * (fragment_recce_form.xml:292-311).
     */
    public java.util.List<String> personNames() {
        java.util.List<String> names = readPersonNames();
        if (names.isEmpty() && scrollTo("fAddPerson")) {
            names = readPersonNames();
            for (int i = 0; i < 4 && names.isEmpty(); i++) { swipeUp(); names = readPersonNames(); }
        }
        return names;
    }

    private java.util.List<String> readPersonNames() {
        try {
            return driver.findElements(byId("personName")).stream()
                    .map(WebElement::getText).filter(t -> t != null && !t.isBlank()).toList();
        } catch (WebDriverException e) { return java.util.List.of(); }
    }

    /**
     * Ticks the first member in the crew picker and confirms; returns the name, or "" when
     * the picker had nobody. Publishing requires at least one crew-sourced person
     * (RecceFormFragment.submit: "recce_publish_needs_crew", since Oct 2026).
     */
    public String addFirstCrewMember() {
        if (!openAddFromCrew() || !isCrewPickerOpen(Duration.ofSeconds(10))) return "";
        java.util.List<String> crew = crewNames();
        if (crew.isEmpty() || !tapCrewRow(crew.get(0))) { tapCrewDone(); return ""; }
        tapCrewDone();
        return crew.get(0);
    }

    // ---------------- picker "Use" button look [WEB RECCE-05] ----------------

    /** pickerUse's enabled attribute ("true"/"false"), or "" when absent. */
    public String pickerUseEnabled() {
        try { return driver.findElement(byId("pickerUse")).getAttribute("enabled"); }
        catch (WebDriverException e) { return ""; }
    }

    /**
     * The button's dominant fill colour as 0xRRGGBB: the most common pixel along its middle
     * row, so the white label and icon do not decide it. -1 when it cannot be captured.
     */
    public int pickerUseDominantColor() {
        try {
            WebElement b = driver.findElement(byId("pickerUse"));
            byte[] png = b.getScreenshotAs(org.openqa.selenium.OutputType.BYTES);
            java.awt.image.BufferedImage img =
                    javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(png));
            if (img == null) return -1;
            java.util.Map<Integer, Integer> freq = new java.util.HashMap<>();
            int y = img.getHeight() / 2;
            for (int x = 0; x < img.getWidth(); x++) {
                int rgb = img.getRGB(x, y) & 0xFFFFFF;
                // Bucket to 4 bits a channel so anti-aliasing does not split the vote.
                int key = rgb & 0xF0F0F0;
                freq.merge(key, 1, Integer::sum);
            }
            int bestKey = freq.entrySet().stream().max(java.util.Map.Entry.comparingByValue())
                    .map(java.util.Map.Entry::getKey).orElse(-1);
            if (bestKey < 0) return -1;
            // Return the first real pixel of that bucket (not the bucket's floor).
            for (int x = 0; x < img.getWidth(); x++) {
                int rgb = img.getRGB(x, y) & 0xFFFFFF;
                if ((rgb & 0xF0F0F0) == bestKey) return rgb;
            }
            return bestKey;
        } catch (Exception e) { return -1; }
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
