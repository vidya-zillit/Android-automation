package com.zillit.automation.pages;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Create / edit a schedule DAY — a type plus the dates it covers.
 *
 * <p>Three ways to choose dates, all producing a discrete list on the wire:
 * <b>Date Range</b> (by number of days, or to an end date), <b>Calendar</b> (tap individual
 * cells, which may be non-adjacent) and <b>Day Wise</b> (weekday filtering over a span).
 *
 * <p>Ids from `new_box_activity_create_schedule.xml`.
 */
public class BoxCreateSchedulePage extends BoxSchedulePage {

    /** Toast wording, quoted from strings.xml so a change in the app shows up here. */
    public static final String ERR_NO_TYPE  = "Please select a type";
    public static final String ERR_NO_DATES = "Please select dates";

    public BoxCreateSchedulePage(AndroidDriver driver) {
        super(driver);
    }

    public boolean isLoaded(Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        do {
            if (present("btnSave") && (present("dateModeTabs") || present("selectedTypeContainer"))) {
                return true;
            }
            pause(400);
        } while (System.currentTimeMillis() < end);
        return false;
    }

    public boolean stillOpen() { return isLoaded(Duration.ofSeconds(3)); }

    // ---------------- type ----------------

    public String selectedTypeName() { return text("selectedTypeName"); }

    /**
     * Opens the type picker, whichever state the form is in.
     *
     * <p>This is an autocomplete dropdown, not a button-driven list, and the code gates it
     * on nothing being selected yet ({@code CreateScheduleActivity:232-236}):
     *
     * <pre>
     * etTypeSearch.setOnClickListener { openTypeDropdown() }
     * private fun openTypeDropdown() {
     *     if (selectedTypeId.isEmpty()) { typeList.visibility = VISIBLE }
     * }
     * </pre>
     *
     * So there are two states and two controls:
     * <ul>
     *   <li><b>Nothing selected</b> — {@code etTypeSearch} is visible; tapping it opens
     *       the list.</li>
     *   <li><b>A type already chosen</b> — {@code showSelectedType} has hidden both the
     *       search box and the list (:297-301), and only {@code btnChangeType} brings them
     *       back, clearing the selection as it goes (:323-329).</li>
     * </ul>
     *
     * <p>The old version only ever tapped {@code btnChangeType}, which on a fresh form is
     * not even there — hence "typeList=absent rows=0" and seventeen cases skipping with
     * "No type could be chosen" on a project that has five types.
     */
    public boolean openTypePicker() {
        if (typeListShown() && !typeNames().isEmpty()) return true;

        // Nothing selected yet: the search box is the handle.
        if (displayed("etTypeSearch")) {
            clickIfPresent("etTypeSearch");
            hideKeyboard();                 // the dropdown sits under the keyboard
            if (waitForRows()) return true;
        }
        // A type is already chosen: clear it, which re-shows the search and the list.
        if (clickIfPresent("btnChangeType")) {
            hideKeyboard();
            if (waitForRows()) return true;
        }
        // Last resort: typing re-opens the dropdown (the TextWatcher calls
        // openTypeDropdown on every change), which helps if focus was lost.
        if (displayed("etTypeSearch")) {
            searchTypes(" ");
            searchTypes("");
            hideKeyboard();
            return waitForRows();
        }
        return false;
    }

    /** The rows are fetched, so the container can be visible and empty for a moment. */
    private boolean waitForRows() {
        long deadline = System.currentTimeMillis() + 10000;
        while (System.currentTimeMillis() < deadline) {
            if (!typeNames().isEmpty()) return true;
            pause(400);
        }
        return false;
    }

    /** What the picker shows, for saying why no type could be chosen. */
    public String typePickerState() {
        return "typeList=" + (typeListShown() ? "shown" : present("typeList") ? "hidden" : "absent")
                + " etTypeSearch=" + (displayed("etTypeSearch") ? "shown" : "no")
                + " btnChangeType=" + (displayed("btnChangeType") ? "shown" : "no")
                + " rows=" + typeNames().size()
                + " selected=\"" + selectedTypeName() + "\"";
    }

    public boolean typeListShown() { return displayed("typeList"); }

    public boolean searchTypes(String term) { return fill("etTypeSearch", term); }

    /**
     * The types offered in the picker.
     *
     * <p>The rows are built in code — {@code LinearLayout(this)} holding a colour
     * {@code View} and a {@code TextView} ({@code CreateScheduleActivity:249-276}) — so
     * they carry NO resource ids. Reading them by id found nothing, which is why seven
     * cases skipped with "No type could be chosen"; {@code selectedTypeName} belongs to
     * the chosen-type display above the list, not to its rows.
     */
    public List<String> typeNames() {
        List<String> out = new ArrayList<>();
        try {
            List<WebElement> lists = driver.findElements(byId("typeList"));
            if (lists.isEmpty()) return out;
            for (WebElement t : lists.get(0).findElements(
                    io.appium.java_client.AppiumBy.className("android.widget.TextView"))) {
                String s = t.getText();
                if (s != null && !s.isBlank()) out.add(s.trim());
            }
        } catch (Exception ignored) { }
        return out;
    }

    /** Taps a type row by its label — the only handle these id-less rows have. */
    public boolean chooseType(String name) {
        try {
            List<WebElement> lists = driver.findElements(byId("typeList"));
            if (!lists.isEmpty()) {
                for (WebElement t : lists.get(0).findElements(
                        io.appium.java_client.AppiumBy.className("android.widget.TextView"))) {
                    String s = t.getText();
                    if (s != null && s.trim().equals(name.trim())) {
                        t.click();
                        pause(1200);
                        return true;
                    }
                }
            }
        } catch (Exception ignored) { }
        return tapText(name);
    }

    // ---------------- a new type, inline ----------------

    public boolean openNewTypeForm() { return clickIfPresent("btnAddNewType"); }

    public boolean newTypeFormShown() { return displayed("etNewTypeName"); }

    public boolean setNewTypeName(String name) { return fill("etNewTypeName", name); }

    public boolean saveNewType()   { return clickIfPresent("btnCreateType"); }

    public boolean cancelNewType() { return clickIfPresent("btnCancelNewType"); }

    // ---------------- dates ----------------

    /** Tab 0 Date Range · 1 Calendar · 2 Day Wise. Tapped by their on-screen labels. */
    public boolean chooseDateRangeTab() { return tapText("Date Range") || tapTabAt(0); }

    public boolean chooseCalendarTab()  { return tapText("Calendar") || tapTabAt(1); }

    public boolean chooseDayWiseTab()   { return tapText("Day Wise") || tapTabAt(2); }

    private boolean tapTabAt(int index) {
        try {
            List<WebElement> tabs = driver.findElements(byId("dateModeTabs"));
            if (tabs.isEmpty()) return false;
            List<WebElement> children = tabs.get(0).findElements(
                    io.appium.java_client.AppiumBy.className("android.widget.TextView"));
            if (index >= children.size()) return false;
            children.get(index).click();
            pause(900);
            return true;
        } catch (Exception e) { return false; }
    }

    public boolean dateRangeShown() { return displayed("dateRangeContent"); }
    public boolean calendarShown()  { return displayed("calendarPickerGrid"); }
    public boolean dayWiseShown()   { return displayed("weekdayChips"); }

    /**
     * Ticks a weekday in the Day Wise tab. Labels are Sun…Sat, built in code as checkable
     * Chips, and a weekday that does not occur inside the chosen span is disabled
     * (CreateScheduleActivity:693-722) — so a false return can mean "not in this span",
     * which is itself worth asserting on.
     */
    public boolean chooseWeekday(String label) {
        try {
            List<WebElement> groups = driver.findElements(byId("weekdayChips"));
            if (groups.isEmpty()) return false;
            for (WebElement chip : groups.get(0).findElements(
                    io.appium.java_client.AppiumBy.xpath(".//*"))) {
                String t = chip.getText();
                if (t == null || !t.trim().equalsIgnoreCase(label)) continue;
                if (!chip.isEnabled()) return false;
                chip.click();
                pause(700);
                return true;
            }
            return false;
        } catch (Exception e) { return false; }
    }

    /** Which weekdays the span actually offers — the rest are drawn disabled. */
    public List<String> weekdaysOffered() {
        List<String> out = new java.util.ArrayList<>();
        try {
            List<WebElement> groups = driver.findElements(byId("weekdayChips"));
            if (groups.isEmpty()) return out;
            for (WebElement chip : groups.get(0).findElements(
                    io.appium.java_client.AppiumBy.xpath(".//*"))) {
                String t = chip.getText();
                if (t != null && !t.isBlank() && chip.isEnabled()) out.add(t.trim());
            }
        } catch (Exception ignored) { }
        return out;
    }

    /** The Day Wise count line — "N day(s) selected" once weekdays are ticked. */
    public String dayWiseCount() { return text("tvDayWiseCount"); }

    public boolean chooseByDays()    { return clickIfPresent("btnByDays"); }
    public boolean chooseByEndDate() { return clickIfPresent("btnByEndDate"); }

    public String startDate() { return text("tvStartDate"); }
    public String endDate()   { return text("tvEndDate"); }

    public boolean setNumberOfDays(String n) { return fill("etNumberOfDays", n); }

    public String numberOfDays() { return text("etNumberOfDays"); }

    public boolean plusDay()  { return clickIfPresent("btnPlus"); }
    public boolean minusDay() { return clickIfPresent("btnMinus"); }

    /** How many dates the Calendar tab says are picked. */
    public String pickedCount() { return text("tvCalSelectedCount"); }

    /**
     * The count label, after scrolling it into view.
     *
     * <p>The whole form lives in a ScrollView, and this label sits below the date chips.
     * Each picked date adds a chip, so by the third one the label is pushed off screen —
     * and a view scrolled out of its container is absent from the accessibility tree, not
     * merely hidden. Reading it without scrolling therefore returns null for three dates
     * while returning "1 day(s) selected" for one, which looks like an app bug and is not.
     */
    public String pickedCountScrolled() {
        String now = pickedCount();
        if (now != null) return now;
        scrollToId("tvCalSelectedCount");
        pause(600);
        return pickedCount();
    }

    /**
     * The dates the Calendar tab currently holds, read from the chips.
     *
     * <p>This is the only trustworthy evidence. tvCalSelectedCount is set to "" while
     * nothing is picked (CreateScheduleActivity:510), and an empty wrap_content TextView
     * leaves no bounds, so it drops out of the accessibility tree and reads as absent —
     * indistinguishable from the view not existing. Every picked date, on the other hand,
     * adds a Chip carrying the formatted date (renderPickedDateChips).
     */
    public List<String> pickedDateChips() {
        List<String> out = new java.util.ArrayList<>();
        try {
            List<WebElement> groups = driver.findElements(byId("pickedDatesChips"));
            // The ChipGroup itself drops out of the tree while it holds nothing.
            if (groups.isEmpty()) return out;
            // A Material Chip surfaces as android.widget.Button, NOT a TextView, and each
            // one carries a second, textless Button for its close icon - so match on any
            // descendant that actually has text rather than on a class name.
            for (WebElement chip : groups.get(0).findElements(
                    io.appium.java_client.AppiumBy.xpath(".//*"))) {
                String t = chip.getText();
                if (t != null && !t.trim().isEmpty()) out.add(t.trim());
            }
        } catch (Exception ignored) { }
        return out;
    }

    /**
     * Diagnostic dump of the Calendar tab: every grid cell and every chip, with the
     * attributes that decide whether a tap can land. Used to settle whether tapping a
     * day cell actually selects a date. Not used by any assertion.
     */
    public String calendarProbe(String label) {
        StringBuilder sb = new StringBuilder("PROBE ").append(label).append('\n');
        sb.append("  month=").append(calendarMonth())
          .append("  countLabel=").append(pickedCount()).append('\n');
        for (String id : new String[] { "calendarPickerGrid", "pickedDatesChips" }) {
            List<WebElement> boxes = driver.findElements(byId(id));
            if (boxes.isEmpty()) { sb.append("  ").append(id).append(" ABSENT\n"); continue; }
            List<WebElement> kids;
            try {
                kids = boxes.get(0).findElements(io.appium.java_client.AppiumBy.xpath(".//*"));
            } catch (Exception e) {
                sb.append("  ").append(id).append(" unreadable\n"); continue;
            }
            sb.append("  ").append(id).append(": ").append(kids.size()).append(" descendant(s)\n");
            int n = 0;
            for (WebElement k : kids) {
                if (n++ > 48) { sb.append("    …truncated\n"); break; }
                try {
                    String t = k.getText();
                    sb.append("    <").append(k.getAttribute("class")).append("> text=")
                      .append(t == null ? "null" : "\"" + t + "\"")
                      .append(" enabled=").append(k.getAttribute("enabled"))
                      .append(" clickable=").append(k.getAttribute("clickable"))
                      .append(" bounds=").append(k.getAttribute("bounds")).append('\n');
                } catch (Exception e) {
                    sb.append("    <unreadable>\n");
                }
            }
        }
        return sb.toString();
    }

    /**
     * Takes every picked date off the form, so the SAME open form can be reused.
     *
     * <p>Each chip carries a close icon — a second, textless Button beside the date
     * (renderPickedDateChips sets isCloseIconVisible on all of them, including the one
     * pre-filled from a tapped cell). Clicking those is how the form is emptied without
     * leaving the page: closing a page only to walk the drawer again costs a navigation per
     * case and risks stepping out of the module altogether.
     */
    public boolean clearPickedDates() {
        for (int round = 0; round < 12; round++) {
            List<WebElement> groups = driver.findElements(byId("pickedDatesChips"));
            if (groups.isEmpty()) return true;              // group drops out when empty
            List<WebElement> closers = new java.util.ArrayList<>();
            try {
                for (WebElement el : groups.get(0).findElements(
                        io.appium.java_client.AppiumBy.xpath(".//*"))) {
                    String t = el.getText();
                    if (t == null || t.trim().isEmpty()) closers.add(el);
                }
            } catch (Exception e) { return false; }
            if (closers.isEmpty()) return pickedDateChips().isEmpty();
            try {
                closers.get(closers.size() - 1).click();
                pause(600);
            } catch (Exception e) { return pickedDateChips().isEmpty(); }
        }
        return pickedDateChips().isEmpty();
    }

    public boolean calendarNextMonth() { return clickIfPresent("btnCalNext"); }
    public boolean calendarPrevMonth() { return clickIfPresent("btnCalPrev"); }

    public String calendarMonth() { return text("tvCalMonth"); }

    /**
     * Taps a day number in the calendar grid.
     *
     * <p>Returns false when that cell is not tappable — which is how a past date reads,
     * and is exactly what the negative case wants to see.
     */
    public boolean pickCalendarDay(String dayNumber) {
        try {
            List<WebElement> grids = driver.findElements(byId("calendarPickerGrid"));
            if (grids.isEmpty()) return false;
            // The 42-cell grid pads both ends with the neighbouring months, so one day
            // number can appear twice (e.g. a December grid trails into January). Those
            // padding cells are disabled, so try every match until one actually takes.
            int before = pickedDateChips().size();
            for (WebElement cell : grids.get(0).findElements(
                    io.appium.java_client.AppiumBy.className("android.widget.TextView"))) {
                String t = cell.getText();
                if (t == null || !t.trim().equals(dayNumber)) continue;
                if (!cell.isEnabled()) continue;        // past or adjacent month
                cell.click();
                pause(700);
                if (pickedDateChips().size() > before) return true;
            }
            return false;
        } catch (Exception e) { return false; }
    }

    /** Is a given day cell enabled? Past dates must not be. */
    public Boolean calendarDayEnabled(String dayNumber) {
        try {
            List<WebElement> grids = driver.findElements(byId("calendarPickerGrid"));
            if (grids.isEmpty()) return null;
            for (WebElement cell : grids.get(0).findElements(
                    io.appium.java_client.AppiumBy.className("android.widget.TextView"))) {
                String t = cell.getText();
                if (t != null && t.trim().equals(dayNumber)) return cell.isEnabled();
            }
            return null;
        } catch (Exception e) { return null; }
    }

    public String summary() { return text("tvSummary"); }

    public boolean save() { return clickIfPresent("btnSave"); }

    public String saveLabel() { return text("btnSave"); }

    // ---------------- helpers ----------------

    private boolean fill(String id, String value) {
        try {
            for (WebElement e : driver.findElements(byId(id))) {
                if (!e.isDisplayed()) continue;
                e.click();
                e.clear();
                if (!value.isEmpty()) e.sendKeys(value);
                hideKeyboard();
                pause(600);
                return true;
            }
            return false;
        } catch (Exception e) { return false; }
    }
}
