package com.zillit.automation.pages;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;

/**
 * Create / edit an EVENT or a NOTE — one screen, two tabs.
 *
 * <p>The two are the same record with a different `eventType`, which is why the drawer's
 * "Create Event" and "Create Note" open this same screen with a different tab preselected.
 *
 * <p>Ids from `new_box_activity_create_event.xml`.
 */
public class BoxCreateEventPage extends BoxSchedulePage {

    /** The Home-Calendar question, asked on save in CREATE mode only. */
    public static final String CALENDAR_PROMPT =
            "Would you like to set a reminder for this event on your Home Calendar?";

    public BoxCreateEventPage(AndroidDriver driver) {
        super(driver);
    }

    public boolean isLoaded(Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        do {
            if (present("btnSave") && (present("eventForm") || present("noteForm")
                    || present("eventNoteToggle"))) {
                return true;
            }
            pause(400);
        } while (System.currentTimeMillis() < end);
        return false;
    }

    public boolean stillOpen() { return isLoaded(Duration.ofSeconds(3)); }

    // ---------------- tabs ----------------

    public boolean chooseEventTab() { return clickIfPresent("btnEventTab"); }
    public boolean chooseNoteTab()  { return clickIfPresent("btnNoteTab"); }

    public boolean onEventTab() { return displayed("etEventTitle"); }
    public boolean onNoteTab()  { return displayed("etNoteTitle"); }

    // ---------------- the schedule-day link ----------------

    public boolean linkCardShown() { return displayed("linkDayCard"); }

    public boolean openLinkDayPicker() { return clickIfPresent("spinnerLinkDay"); }

    /** What the link spinner currently reads — "" when nothing is linked. */
    public String linkedDay() {
        String t = text("spinnerLinkDay");
        return t == null ? "" : t.trim();
    }

    /** Picks a schedule day by its label in the spinner's dropdown. */
    public boolean linkScheduleDay(String label) {
        if (!openLinkDayPicker()) return false;
        pause(800);
        return tapText(label);
    }

    /** The days on offer, read from the open dropdown. */
    public List<String> linkDayOptions() {
        java.util.List<String> out = new java.util.ArrayList<>();
        try {
            for (WebElement e : driver.findElements(
                    io.appium.java_client.AppiumBy.className("android.widget.TextView"))) {
                String t = e.getText();
                if (t != null && !t.isBlank() && e.isDisplayed()) out.add(t.trim());
            }
        } catch (Exception ignored) { }
        return out;
    }

    // ---------------- event fields ----------------

    public boolean setTitle(String v) { return fill("etEventTitle", v); }

    public String titleValue() { return text("etEventTitle"); }

    public boolean setDescription(String v) { return fill("etEventDesc", v); }

    public boolean toggleFullDay() { return clickIfPresent("switchFullDay"); }

    public boolean timeRowShown() { return displayed("timeRow"); }

    public String startDate() { return text("tvEventStartDate"); }
    public String endDate()   { return text("tvEventEndDate"); }

    /**
     * Moves the start date to a given day of the month.
     *
     * <p>The date row opens a plain {@link android.app.DatePickerDialog} whose minimum is
     * today (CreateEventActivity.showDatePicker), so only today or later can be chosen.
     * The dialog's day grid carries the day numbers as text and its confirm is the standard
     * button1 — neither belongs to this app, so both are matched by what they say.
     */
    public boolean setStartDay(String dayNumber) {
        if (!clickIfPresent("tvEventStartDate")) return false;
        pause(1500);
        if (!tapDayInPicker(dayNumber)) { dismissPicker(); return false; }
        return confirmPicker();
    }

    public boolean setEndDay(String dayNumber) {
        if (!clickIfPresent("tvEventEndDate")) return false;
        pause(1500);
        if (!tapDayInPicker(dayNumber)) { dismissPicker(); return false; }
        return confirmPicker();
    }

    private boolean tapDayInPicker(String dayNumber) {
        try {
            for (WebElement el : driver.findElements(
                    io.appium.java_client.AppiumBy.xpath(
                            "//android.view.View[@text=" + quote(dayNumber) + "]"
                                    + " | //android.widget.TextView[@text="
                                    + quote(dayNumber) + "]"))) {
                if (!el.isDisplayed() || !el.isEnabled()) continue;
                el.click();
                pause(800);
                return true;
            }
        } catch (Exception ignored) { }
        return false;
    }

    private boolean confirmPicker() {
        for (String label : new String[] { "OK", "Ok", "DONE", "Done", "Set" }) {
            if (tapText(label)) { pause(1500); return true; }
        }
        return false;
    }

    private void dismissPicker() {
        if (!tapText("Cancel")) {
            try { driver.navigate().back(); } catch (Exception ignored) { }
        }
        pause(800);
    }

    private static String quote(String s) {
        return s.contains("'") ? "\"" + s + "\"" : "'" + s + "'";
    }

    public boolean openRepeat()   { return clickIfPresent("spinnerRepeat"); }
    public String repeatValue()   { return text("spinnerRepeat"); }
    public boolean repeatEndShown() { return displayed("repeatEndDateContainer"); }

    public boolean openReminder() { return clickIfPresent("spinnerReminder"); }
    public String reminderValue() { return text("spinnerReminder"); }

    public boolean openCallType()  { return clickIfPresent("spinnerCallType"); }
    public String callTypeValue()  { return text("spinnerCallType"); }
    public boolean callLineShown() { return displayed("layoutCallLine"); }

    public boolean setLocation(String v) { return fill("etLocation", v); }

    public String distributeTo() { return text("tvDistributeTo"); }

    public boolean openDistribute() { return clickIfPresent("tvDistributeTo"); }

    /**
     * Sets Distribute To = "Only Me".
     *
     * <p>Required before any event can be saved: {@code distributeTo.isEmpty() &&
     * externalEmails.isEmpty()} is refused ({@code CreateEventActivity:1850}), which is
     * why a titled, full-day event still never reached the Home-Calendar question.
     *
     * <p>"Only Me" on purpose — every other option sends the event to real people on the
     * project, and a test must not post to anyone's calendar.
     */
    public boolean distributeToSelf() {
        if (!openDistribute()) return false;
        pause(1200);
        if (tapText("Only Me")) { pause(900); return true; }
        // Some builds word it differently; fall back to the self-ish options, never to
        // a department or "all".
        return tapText("Myself") || tapText("Self");
    }

    public boolean addExternalGuests() { return clickIfPresent("btnAddExternalGuests"); }

    public String externalGuestCount() { return text("tvExternalGuestsCount"); }

    // ---------------- note fields ----------------

    public boolean setNoteTitle(String v) { return fill("etNoteTitle", v); }

    public String noteTitleValue() { return text("etNoteTitle"); }

    public boolean setNoteText(String v) { return fill("etNoteText", v); }

    public String noteDate() { return text("tvNoteDate"); }

    public boolean noteTypeToggleShown() { return displayed("noteTypeToggle"); }

    /** Picks a note type by its label — the options come from the server. */
    public boolean chooseNoteType(String label) { return tapText(label); }

    public boolean noteDistributeShown() { return displayed("noteDistributeSection"); }

    public String noteDistributeTo() { return text("tvDistributeToNote"); }

    // ---------------- save, and the calendar question ----------------

    public boolean save() { return clickIfPresent("btnSave"); }

    /** Is the Home-Calendar question up? */
    public boolean calendarPromptShown(Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        do {
            if (textShown("Home Calendar")) return true;
            pause(350);
        } while (System.currentTimeMillis() < end);
        return false;
    }

    /**
     * Answers the Home-Calendar question.
     *
     * <p>It is {@code setCancelable(false)} — there is no way past it but Yes or No, so a
     * case that ignores it will sit here until it times out.
     */
    public boolean answerCalendarPrompt(boolean yes) {
        return tapText(yes ? "Yes" : "No");
    }

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
