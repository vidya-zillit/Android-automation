package com.zillit.automation.pages.calendar;

import com.zillit.automation.pages.common.BasePage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * The new-calendar screen shown by the Home "Calendar" unit
 * (new_calendar CalendarFragment) and its event form (EventFormFragment).
 *
 * Validation errors surface as Android Toasts with hardcoded English text —
 * UiAutomator2 exposes them briefly, so {@link #toastShown} polls quickly.
 */
public class CalendarPage extends BasePage {

    public CalendarPage(AndroidDriver driver) {
        super(driver);
    }

    // ---------- calendar screen ----------

    public boolean isCalendarLoaded() {
        return isVisible("fab", Duration.ofSeconds(15))
                || isVisible("monthGrid", Duration.ofSeconds(5));
    }

    /** Quick check that the calendar screen (FAB) is on top. */
    public boolean isVisibleFab() {
        return isVisible("fab", Duration.ofSeconds(2));
    }

    public String monthTitle() {
        try {
            return waitVisible("tvMonthTitle").getText();
        } catch (WebDriverException e) {
            return "";
        }
    }

    public void nextMonth() {
        tap("btnNextMonth");
    }

    public void previousMonth() {
        tap("btnPrevMonth");
    }

    public void tapToday() {
        tap("btnToday");
    }

    /** Opens the create-event form via the FAB. */
    public EventFormView openEventForm() {
        waitVisible("fab").click();
        return new EventFormView(driver);
    }

    private void tap(String id) {
        waitVisible(id).click();
    }

    // ---------- agenda (events list) + detail sheet ----------

    /** Number of event cards shown in the day's agenda list. */
    public int agendaEventCount() {
        return driver.findElements(byId("tvTitle")).size();
    }

    public boolean agendaHasEvent(String titleContains, Duration timeout) {
        return textShown(titleContains, timeout);
    }

    /** Text of every agenda row title (tvTitle) currently on screen. */
    public java.util.List<String> agendaTitles() {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (WebElement e : driver.findElements(byId("tvTitle"))) {
            try {
                String t = e.getText();
                if (t != null) out.add(t);
            } catch (WebDriverException ignored) {
            }
        }
        return out;
    }

    /**
     * True when an agenda row shows the title WITHOUT the "Cancelled: " /
     * "Declined: " prefix the app puts on cancelled/declined rows
     * (new_calendar DayAgendaAdapter.kt:37-52) — i.e. a live occurrence.
     */
    public boolean agendaHasLiveEvent(String title) {
        for (String t : agendaTitles()) {
            if (t.contains(title) && !t.startsWith("Cancelled: ") && !t.startsWith("Declined: ")) {
                return true;
            }
        }
        return false;
    }

    /** Taps the agenda event whose title contains the text; opens the detail sheet. */
    public boolean tapAgendaEvent(String titleContains) {
        for (org.openqa.selenium.WebElement e : driver.findElements(byId("tvTitle"))) {
            String t = e.getText();
            if (t != null && t.toLowerCase().contains(titleContains.toLowerCase())) {
                e.click();
                return true;
            }
        }
        return false;
    }

    public boolean isDetailOpen() {
        return isVisible("btnEdit", Duration.ofSeconds(6))
                || isVisible("btnDelete", Duration.ofSeconds(3));
    }

    public void detailTapEdit() {
        waitVisible("btnEdit").click();
    }

    public void detailTapDelete() {
        waitVisible("btnDelete").click();
    }

    public void detailClose() {
        try {
            waitVisible("btnClose").click();
        } catch (WebDriverException e) {
            driver.navigate().back();
        }
    }

    /** Confirms a delete dialog. Non-recurring: btnDelete. Recurring: pick scope + btnOk. */
    public void confirmDelete() {
        // Recurring events show a scope dialog (rbSingle/rbThisAndFuture/rbAll + btnOk).
        if (isRecurringActionDialogOpen()) {
            pickRecurringScope("all");
            tapRecurringConfirm();
            return;
        }
        // Non-recurring confirm dialog (btnDelete) or a generic yes/delete/ok.
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.elementToBeClickable(AppiumBy.androidUIAutomator(
                            "new UiSelector().textMatches(\"(?i)(yes|delete|ok|confirm)\")")))
                    .click();
        } catch (WebDriverException ignored) {
        }
    }

    // ---------- recurring scope dialog (this / this-and-future / all) ----------

    public boolean isRecurringActionDialogOpen() {
        return isVisible("btnOk", Duration.ofSeconds(4))
                && (isVisible("rbAll", Duration.ofSeconds(2))
                    || isVisible("rbSingle", Duration.ofSeconds(2)));
    }

    /** scope = "single" | "future" | "all". */
    public void pickRecurringScope(String scope) {
        String id = switch (scope) {
            case "single" -> "rbSingle";
            case "future" -> "rbThisAndFuture";
            default -> "rbAll";
        };
        try {
            waitVisible(id).click();
        } catch (WebDriverException ignored) {
        }
    }

    public void tapRecurringConfirm() {
        waitVisible("btnOk").click();
    }

    public void tapRecurringCancel() {
        try {
            waitVisible("btnCancel").click();
        } catch (WebDriverException e) {
            driver.navigate().back();
        }
    }

    // ---------- view mode + quick actions ----------

    public boolean toggleViewMode(String modeLabel) {
        try {
            waitVisible("tvViewMode").click();
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.elementToBeClickable(AppiumBy.androidUIAutomator(
                            "new UiSelector().textMatches(\"(?i)" + modeLabel + "\")")))
                    .click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Current view-mode label in the header (tvViewMode: "Month" / "Week" / "Day"). */
    public String viewModeLabel() {
        try {
            String t = waitVisible("tvViewMode").getText();
            return t == null ? "" : t;
        } catch (WebDriverException e) {
            return "";
        }
    }

    public boolean isEventsButtonVisible() {
        return isVisible("btnEvents", Duration.ofSeconds(4));
    }

    // =====================================================================
    // Web-gap helpers (CalendarWebGapTest / CalendarInviteTest).
    // App source: Zillit-Android-devbuild .../bottomNav/new_calendar/.
    // =====================================================================

    private static final java.time.format.DateTimeFormatter MONTH_TITLE =
            java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.ENGLISH);

    /** Makes sure the header is in Month view (tvViewMode reads "Month"). */
    public void ensureMonthView() {
        if (!"Month".equalsIgnoreCase(viewModeLabel())) {
            toggleViewMode("Month");
            pause(800);
        }
    }

    /**
     * Selects a day on the Month grid: pages the month (btnNext/PrevMonth) until tvMonthTitle
     * reads that month ("MMMM yyyy", CalendarViewModel.kt:38), then taps the cell whose
     * tvDayNumber is that day (padding cells are blank, MonthGridAdapter.kt:33-40). The agenda
     * below then lists that day's events (CalendarViewModel.selectedDayEvents).
     */
    public boolean selectDay(java.time.LocalDate day) {
        if (!isVisible("tvMonthTitle", Duration.ofSeconds(5))) return false;   // not on the calendar
        ensureMonthView();
        java.time.YearMonth target = java.time.YearMonth.from(day);
        for (int i = 0; i < 30; i++) {
            java.time.YearMonth shown;
            try {
                shown = java.time.YearMonth.parse(monthTitle().trim(), MONTH_TITLE);
            } catch (java.time.format.DateTimeParseException e) {
                pause(500);
                continue;
            }
            if (shown.equals(target)) break;
            if (shown.isBefore(target)) nextMonth(); else previousMonth();
            pause(600);
        }
        String want = String.valueOf(day.getDayOfMonth());
        for (int attempt = 0; attempt < 3; attempt++) {
            for (WebElement cell : driver.findElements(byId("tvDayNumber"))) {
                try {
                    if (want.equals(cell.getText())) {
                        cell.click();
                        pause(1200);
                        return true;
                    }
                } catch (WebDriverException ignored) {
                }
            }
            pause(800);
        }
        return false;
    }

    /** Waits until an agenda row containing the text is on screen, scrolling the page to it. */
    public boolean waitAgendaRow(String text, Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < end) {
            for (String t : agendaTitles()) {
                if (t.contains(text)) return true;
            }
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView("
                                + "new UiSelector().resourceIdMatches(\".*:id/tvTitle\").textContains(\""
                                + text.replace("\"", "\\\"") + "\"))"));
                return true;
            } catch (WebDriverException ignored) {
            }
            pause(1000);
        }
        return false;
    }

    /** The agenda row titles on screen that contain the text (live, Cancelled:, Declined: ...). */
    public java.util.List<String> agendaRowsFor(String text) {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String t : agendaTitles()) if (t.contains(text)) out.add(t);
        return out;
    }

    /** Pulls the agenda to refresh (swipeRefresh) so server changes are re-read. */
    public void pullToRefresh() {
        try {
            WebElement list = driver.findElement(byId("swipeRefresh"));
            org.openqa.selenium.Rectangle r = list.getRect();
            int x = r.getX() + r.getWidth() / 2;
            swipe(x, r.getY() + 40, x, r.getY() + Math.min(r.getHeight(), 900));
            pause(2500);
        } catch (WebDriverException ignored) {
        }
    }

    /** Taps the agenda row whose title contains the text; prefers a live (unprefixed) row. */
    public boolean openAgendaEvent(String text) {
        if (!waitAgendaRow(text, Duration.ofSeconds(15))) return false;
        WebElement fallback = null;
        for (WebElement e : driver.findElements(byId("tvTitle"))) {
            String t;
            try { t = e.getText(); } catch (WebDriverException ex) { continue; }
            if (t == null || !t.contains(text)) continue;
            if (!t.startsWith("Cancelled: ") && !t.startsWith("Declined: ")) {
                e.click();
                return waitDetailSheet(Duration.ofSeconds(10));
            }
            if (fallback == null) fallback = e;
        }
        if (fallback != null) {
            fallback.click();
            return waitDetailSheet(Duration.ofSeconds(10));
        }
        return false;
    }

    // ---------- event detail sheet (EventDetailBottomSheet) ----------

    /**
     * The detail sheet is open. Detected by its tvEventType chip ("Members"/"Personal"),
     * which every event shows — unlike btnEdit/btnDelete, which are hidden for expired,
     * cancelled, box-schedule and other users' events (EventDetailBottomSheet.kt:240-252).
     */
    public boolean waitDetailSheet(Duration timeout) {
        return isVisible("tvEventType", timeout) || textShown("Event Details", Duration.ofSeconds(2));
    }

    /** Text of a view in the detail sheet, scrolling to it first; "" if absent/gone. */
    public String detailText(String id) {
        if (driver.findElements(byId(id)).isEmpty()) scrollToId(id);
        try {
            String t = driver.findElement(byId(id)).getText();
            return t == null ? "" : t.trim();
        } catch (WebDriverException e) {
            return "";
        }
    }

    /** A detail-sheet view is shown (scrolls to it; GONE views are absent from the tree). */
    public boolean detailShows(String id) {
        if (!driver.findElements(byId(id)).isEmpty()) return true;
        scrollToId(id);
        return !driver.findElements(byId(id)).isEmpty();
    }

    /** Closes the detail sheet (btnClose, else BACK) and waits for the calendar. */
    public void closeDetail() {
        // An Accept/Decline/Delete dismisses the sheet by itself (EventDetailBottomSheet.kt:507-525);
        // a blind BACK then leaves the Calendar (on Home it backs out of the app).
        if (!isVisible("btnClose", Duration.ofSeconds(2)) && !isVisible("tvEventType", Duration.ofSeconds(1))) return;
        detailClose();
        pause(800);
        if (isVisible("tvEventType", Duration.ofSeconds(1))) {
            driver.navigate().back();
            pause(600);
        }
    }

    // ---------- recurring scope dialog ----------

    /** Text of a scope radio (rbSingle / rbThisAndFuture / rbAll); "" if absent. */
    public String scopeLabel(String id) {
        try {
            return driver.findElement(byId(id)).getText();
        } catch (WebDriverException e) {
            return "";
        }
    }

    /** The one-off delete confirm ("Are you sure you want to delete this event?") is up. */
    public boolean isConfirmDeleteDialogOpen(Duration timeout) {
        return textShown("Are you sure you want to delete this event?", timeout);
    }

    /** Cancels whatever confirm/scope dialog is up (btnCancel), else BACK. */
    public void cancelDialog() {
        try {
            waitVisible("btnCancel").click();
        } catch (WebDriverException e) {
            driver.navigate().back();
        }
        pause(600);
    }

    // ---------- header: Events menu, views ----------

    /** Opens the btnEvents popup; returns the row labels found. */
    public java.util.List<String> openEventsMenu() {
        java.util.List<String> rows = new java.util.ArrayList<>();
        try {
            waitVisible("btnEvents").click();
        } catch (WebDriverException e) {
            return rows;
        }
        if (isVisible("rowReceived", Duration.ofSeconds(5))) rows.add("Received Events");
        if (isVisible("rowCreated", Duration.ofSeconds(1))) rows.add("Created Events");
        if (isVisible("rowDeclined", Duration.ofSeconds(1))) rows.add("Declined Events");
        return rows;
    }

    /** Taps one row of the Events popup: "rowReceived" / "rowCreated" / "rowDeclined". */
    public boolean openEventsScreen(String rowId) {
        if (!isVisible(rowId, Duration.ofSeconds(2))) openEventsMenu();
        try {
            waitVisible(rowId).click();
            return isVisible("tabLayout", Duration.ofSeconds(15));
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Opens the view-mode popup and returns its item labels (then closes it). */
    public java.util.List<String> viewModeOptions() {
        java.util.List<String> out = new java.util.ArrayList<>();
        try {
            waitVisible("btnViewMode").click();
            pause(700);
            for (WebElement e : driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceIdMatches(\"android:id/title\")"))) {
                out.add(e.getText());
            }
            driver.navigate().back();
            pause(500);
        } catch (WebDriverException ignored) {
        }
        return out;
    }

    // ---------- list screens (Received / Created / Declined / Join Call) ----------

    /** Selects a tab of the list screen's tabLayout by its text. */
    public boolean selectListTab(String label) {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceIdMatches(\".*:id/tabLayout\").childSelector("
                            + "new UiSelector().textMatches(\"(?i)" + label + "\"))")).click();
            pause(1500);
            return true;
        } catch (WebDriverException e) {
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiSelector().descriptionMatches(\"(?i)" + label + ".*\")")).click();
                pause(1500);
                return true;
            } catch (WebDriverException e2) {
                return false;
            }
        }
    }

    /** The selected tab's label in the list screen ("" if unknown). */
    public String selectedListTab() {
        try {
            for (WebElement tab : driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceIdMatches(\".*:id/tabLayout\").childSelector("
                            + "new UiSelector().selected(true))"))) {
                String d = tab.getAttribute("content-desc");
                if (d != null && !d.isBlank()) return d;
                String t = tab.getText();
                if (t != null && !t.isBlank()) return t;
                for (WebElement tv : tab.findElements(By.className("android.widget.TextView"))) {
                    String s = tv.getText();
                    if (s != null && !s.isBlank()) return s;
                }
            }
        } catch (WebDriverException ignored) {
        }
        return "";
    }

    /** Types into the list screen's etSearch (replacing its text). */
    public void searchList(String query) {
        try {
            WebElement s = waitVisible("etSearch");
            s.click();
            s.clear();
            if (!query.isEmpty()) s.sendKeys(query);
            pause(2500); // debounce + server
        } catch (WebDriverException ignored) {
        }
    }

    public String listSearchText() {
        try {
            String t = driver.findElement(byId("etSearch")).getText();
            return t == null ? "" : t;
        } catch (WebDriverException e) {
            return "";
        }
    }

    public boolean isListEmptyState(Duration timeout) {
        return isVisible("emptyState", timeout);
    }

    /** Text of the empty-state view: emptyState is a LinearLayout wrapping a TextView. */
    public String listEmptyText() {
        try {
            WebElement box = driver.findElement(byId("emptyState"));
            String own = box.getText();
            if (own != null && !own.isBlank()) return own;
            for (WebElement tv : box.findElements(By.className("android.widget.TextView"))) {
                String t = tv.getText();
                if (t != null && !t.isBlank()) return t;
            }
            return "";
        } catch (WebDriverException e) {
            return "";
        }
    }

    /** Card titles (tvCardTitle) on screen in Received/Created lists. */
    public java.util.List<String> cardTitles() {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (WebElement e : driver.findElements(byId("tvCardTitle"))) {
            try { out.add(e.getText()); } catch (WebDriverException ignored) { }
        }
        return out;
    }

    /** Waits for a card whose tvCardTitle contains the text. */
    public boolean waitCard(String text, Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < end) {
            for (String t : cardTitles()) if (t != null && t.contains(text)) return true;
            pause(1000);
        }
        return false;
    }

    /** tvCardDateTime of the first card whose title contains the text; "" if none. */
    public String cardDateTime(String text) {
        for (WebElement title : driver.findElements(byId("tvCardTitle"))) {
            try {
                if (!title.getText().contains(text)) continue;
                Band band = bandOfAnchor("tvCardTitle", title);
                String v = textIn(band, "tvCardDateTime");
                if (v != null && !v.isBlank()) return v;
            } catch (WebDriverException ignored) {
            }
        }
        try {
            return driver.findElement(byId("tvCardDateTime")).getText();
        } catch (WebDriverException e) {
            return "";
        }
    }

    /** Leaves a list screen (Activity) with BACK until the calendar is back. */
    public void backToCalendar() {
        for (int i = 0; i < 4 && !isVisible("fab", Duration.ofSeconds(2)); i++) {
            driver.navigate().back();
            pause(700);
        }
    }

    /** Events badge on the header (tvEventsBadge); "" when hidden. */
    public String eventsBadge() {
        try {
            return driver.findElement(byId("tvEventsBadge")).getText();
        } catch (WebDriverException e) {
            return "";
        }
    }

    /** Taps a card-level control (btnCardEdit / btnCardDelete / btnCardAccept / btnViewDetails …) on the card with that title. */
    public boolean tapOnCard(String title, String controlId) {
        for (WebElement t : driver.findElements(byId("tvCardTitle"))) {
            try {
                if (!t.getText().contains(title)) continue;
                Band band = bandOfAnchor("tvCardTitle", t);
                WebElement c = elementIn(band, controlId);
                if (c == null) {
                    scrollToId(controlId);
                    c = elementIn(bandOfAnchor("tvCardTitle", t), controlId);
                }
                if (c == null) return false;
                c.click();
                pause(1000);
                return true;
            } catch (WebDriverException ignored) {
            }
        }
        return false;
    }

    /** Number of event cards rendered + the last card title, after scrolling the list to its end. */
    public int countCardsScrolling(int maxSwipes) {
        java.util.Set<String> seen = new java.util.LinkedHashSet<>();
        for (int i = 0; i < maxSwipes; i++) {
            int before = seen.size();
            for (WebElement e : driver.findElements(byId("tvCardTitle"))) {
                try {
                    String key = e.getText() + "|" + textIn(bandOfAnchor("tvCardTitle", e), "tvCardDateTime");
                    seen.add(key);
                } catch (WebDriverException ignored) {
                }
            }
            if (i > 0 && seen.size() == before) break;
            scrollList("rvEvents", true);
            pause(1500);
        }
        return seen.size();
    }

    /** Taps the list screen's "Today" checkbox (cbToday). */
    public boolean tapListToday() {
        try {
            waitVisible("cbToday").click();
            pause(2000);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public String listDateButtonText() {
        try {
            return driver.findElement(byId("btnDatePicker")).getText();
        } catch (WebDriverException e) {
            return "";
        }
    }

    /** btnDatePicker → MaterialDatePicker → type the date → OK. */
    public boolean pickListDate(java.time.LocalDate d) {
        for (String pattern : new String[]{"MM/dd/yyyy", "dd/MM/yyyy"}) {
            try {
                waitVisible("btnDatePicker").click();
            } catch (WebDriverException e) {
                return false;
            }
            if (new EventFormView(driver).confirmTypedDate(d, pattern)) {
                pause(2000);
                return true;
            }
        }
        return false;
    }

    private void swipe(int x1, int y1, int x2, int y2) {
        var finger = new org.openqa.selenium.interactions.PointerInput(
                org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        var seq = new org.openqa.selenium.interactions.Sequence(finger, 1);
        seq.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x1, y1));
        seq.addAction(finger.createPointerDown(0));
        seq.addAction(finger.createPointerMove(Duration.ofMillis(500),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x2, y2));
        seq.addAction(finger.createPointerUp(0));
        driver.perform(java.util.Collections.singletonList(seq));
    }

    static void pause(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public boolean isJoinCallVisible() {
        return isVisible("btnJoinCall", Duration.ofSeconds(4));
    }

    // ---------- event form (nested view object) ----------

    /** The add/edit-event form. Default event type is MEMBERS. */
    public static class EventFormView extends BasePage {

        public EventFormView(AndroidDriver driver) {
            super(driver);
        }

        public boolean isLoaded() {
            return isVisible("etTitle", Duration.ofSeconds(15))
                    && isVisible("btnSave", Duration.ofSeconds(5));
        }

        /** Fast check used to decide whether the form is already on screen. */
        public boolean isOpen(Duration timeout) {
            return isVisible("etTitle", timeout);
        }

        public void setTitle(String title) {
            WebElement f = waitVisible("etTitle");
            f.click();
            f.clear();
            if (!title.isEmpty()) {
                f.sendKeys(title);
            }
            hideKeyboard();
        }

        // ---- field visibility (data-present checks) ----

        public boolean hasTitleField()   { return isVisible("etTitle", Duration.ofSeconds(6)); }
        public boolean hasFullDayToggle(){ return isVisible("switchAllDay", Duration.ofSeconds(4)); }
        public boolean hasStartDate()    { return isVisible("tvDate", Duration.ofSeconds(4)); }
        public boolean hasEndDate()      { return isVisible("tvEndDate", Duration.ofSeconds(4)); }
        public boolean hasTimezone()     { return isVisible("tvTimezone", Duration.ofSeconds(4)); }
        public boolean hasRepeat()       { return isVisible("tvRecurrence", Duration.ofSeconds(4)); }
        public boolean hasSaveButton()   { scrollToId("btnSave"); return isVisible("btnSave", Duration.ofSeconds(4)); }

        public boolean hasDescription() {
            scrollToId("etDescription");
            return isVisible("etDescription", Duration.ofSeconds(4));
        }

        public boolean hasLocation() {
            scrollToId("etLocation");
            return isVisible("etLocation", Duration.ofSeconds(4));
        }

        public boolean hasCallType() {
            scrollToId("tvCallType");
            return isVisible("tvCallType", Duration.ofSeconds(4));
        }

        // ---- data readers (verify defaults are populated) ----

        public String startTimeText() { return textOf("tvStartTime"); }
        public String endTimeText()   { return textOf("tvEndTime"); }
        public String dateText()      { return textOf("tvDate"); }
        public String timezoneText()  { return textOf("tvTimezone"); }
        public String callTypeText()  { scrollToId("tvCallType"); return textOf("tvCallType"); }

        private String textOf(String id) {
            try {
                String t = driver.findElement(byId(id)).getText();
                return t == null ? "" : t;
            } catch (WebDriverException e) {
                return "";
            }
        }

        public void setDescription(String text) {
            scrollToId("etDescription");
            WebElement f = waitVisible("etDescription");
            f.click();
            f.clear();
            if (!text.isEmpty()) {
                f.sendKeys(text);
            }
            hideKeyboardIfShown();   // hideKeyboard() is a BACK press — it closed the edit form when no IME was up
        }

        public String descriptionText() { return textOf("etDescription"); }

        public void selectMembersType() {
            tap("btnTypeMembers");
        }

        public void selectPersonalType() {
            tap("btnTypePersonal");
        }

        public boolean isInviteeControlVisible() {
            scrollToId("btnAddInvitees");   // below the fold on a phone
            return isVisible("btnAddInvitees", Duration.ofSeconds(4));
        }

        public void toggleAllDay() {
            tap("switchAllDay");
        }

        public boolean areTimePickersVisible() {
            return isVisible("tvStartTime", Duration.ofSeconds(3));
        }

        /** Opens the "Repeat" dialog and picks a label (e.g. "Daily", "Weekly"). */
        public boolean selectRecurrence(String label) {
            tap("tvRecurrence");
            try {
                new WebDriverWait(driver, Duration.ofSeconds(6))
                        .until(ExpectedConditions.elementToBeClickable(
                                AppiumBy.androidUIAutomator(
                                        "new UiSelector().textMatches(\"(?i)" + label + "\")")))
                        .click();
                return true;
            } catch (WebDriverException e) {
                dismissDialog();
                return false;
            }
        }

        // ---- pickers (open + select) ----

        /** Call-type dialog: pick e.g. "Video Call"; returns the field's new text. */
        public String selectCallType(String label) {
            scrollToId("tvCallType");
            waitVisible("tvCallType").click();
            pickFromDialog(label);
            return textOf("tvCallType");
        }

        /** Reminder dialog (field id tvNotify). */
        public boolean selectReminder(String label) {
            scrollToId("tvNotify");
            if (!isVisible("tvNotify", Duration.ofSeconds(4))) return false;
            waitVisible("tvNotify").click();
            return pickFromDialog(label);
        }

        public String reminderText() { return textOf("tvNotify"); }

        /**
         * Timezone picker screen: search + pick the first row; returns the form's new tz text.
         *
         * <p>The picker is its own Activity (TimezonePickerFragment) that finishes on a row tap.
         * Do NOT dismiss the keyboard with {@code hideKeyboard()} here: on UiAutomator2 that is
         * a BACK press, and when the IME is not up it closes the picker — the row wait then
         * timed out, the fallback BACK closed the FORM too, and tvTimezone read "" (the
         * 2026-11-23 "Timezone field should stay populated" failure was this test bug, not the
         * app). The rows sit above the IME (adjustResize), so they can be tapped as they are.
         */
        public String selectTimezone(String query) {
            scrollToId("tvTimezone");
            waitVisible("tvTimezone").click();
            if (!isVisible("etSearch", Duration.ofSeconds(10))) {
                return textOf("tvTimezone");
            }
            WebElement s = waitVisible("etSearch");
            s.click();
            s.clear();
            s.sendKeys(query);
            boolean picked = false;
            try {
                // Wait for the FILTERED list: a row whose value or identifier carries the query
                // (identifiers use "_" for spaces, e.g. America/New_York). Tapping the first row
                // before the filter applied picked an unrelated / the current zone.
                String q = query.toLowerCase(java.util.Locale.ROOT).replace(' ', '_');
                long end = System.currentTimeMillis() + 20000;
                while (!picked && System.currentTimeMillis() < end) {
                    for (String id : new String[]{"tvTimezoneIdentifier", "tvTimezoneValue"}) {
                        for (WebElement row : driver.findElements(byId(id))) {
                            String t;
                            try { t = row.getText(); } catch (WebDriverException ex) { continue; }
                            if (t != null && t.toLowerCase(java.util.Locale.ROOT).replace(' ', '_').contains(q)) {
                                row.click();
                                picked = true;
                                break;
                            }
                        }
                        if (picked) break;
                    }
                    if (!picked) pause(700);
                }
                if (!picked) throw new org.openqa.selenium.TimeoutException("no row for " + query);
            } catch (WebDriverException e) {
                // no row for the query — leave the picker (only the picker) with one BACK
                if (isVisible("etSearch", Duration.ofSeconds(1))) {
                    hideKeyboardIfShown();
                    if (isVisible("rvTimezones", Duration.ofSeconds(1))) driver.navigate().back();
                }
            }
            // back on the form
            isVisible("etTitle", Duration.ofSeconds(picked ? 10 : 5));
            scrollToId("tvTimezone");
            isVisible("tvTimezone", Duration.ofSeconds(5));
            return textOf("tvTimezone");
        }

        /** In the timezone picker: types a query and returns whether tvEmpty is shown. */
        public boolean timezoneSearchShowsEmpty(String query) {
            scrollToId("tvTimezone");
            waitVisible("tvTimezone").click();
            if (!isVisible("etSearch", Duration.ofSeconds(10))) return false;
            WebElement s = waitVisible("etSearch");
            s.click();
            s.sendKeys(query);
            boolean empty = isVisible("tvEmpty", Duration.ofSeconds(8))
                    && driver.findElements(byId("tvTimezoneValue")).isEmpty();
            hideKeyboardIfShown();
            if (isVisible("etSearch", Duration.ofSeconds(1))) driver.navigate().back();
            isVisible("etTitle", Duration.ofSeconds(8));
            return empty;
        }

        // ---------------- web-gap helpers ----------------

        /** Types the title exactly as given (no trimming), e.g. whitespace-only. */
        public void typeTitleRaw(String raw) {
            WebElement f = waitVisible("etTitle");
            f.click();
            f.clear();
            f.sendKeys(raw);
            hideKeyboardIfShown();
        }

        public String titleText() { return textOf("etTitle"); }

        public String endDateText() { scrollToId("tvEndDate"); return textOf("tvEndDate"); }
        public String timeStartText() { scrollToId("tvStartTime"); return textOf("tvStartTime"); }
        public String timeEndText() { scrollToId("tvEndTime"); return textOf("tvEndTime"); }

        public boolean isTypeToggleShown() {
            scrollToId("etTitle");
            return isVisible("layoutTypeToggle", Duration.ofSeconds(2));
        }

        public boolean isRecurrenceSectionShown() {
            scrollToId("tvTimezone");
            return isVisible("tvRecurrence", Duration.ofSeconds(2));
        }

        public boolean isChipGroupDaysShown() {
            scrollToId("tvRecurrence");
            return isVisible("chipGroupDays", Duration.ofSeconds(2));
        }

        public String recurrenceText() { scrollToId("tvRecurrence"); return textOf("tvRecurrence"); }

        public String saveButtonText() { scrollToId("btnSave"); return textOf("btnSave"); }

        public boolean isPeopleSectionShown() {
            scrollToId("tvCallType");
            return isVisible("tvCallType", Duration.ofSeconds(2));
        }

        public boolean isCallLineShown() {
            scrollToId("btnAddInvitees");
            return !driver.findElements(byId("tvCallLine")).isEmpty();
        }

        public String callLineText() { scrollToId("tvCallLine"); return textOf("tvCallLine"); }

        public boolean selectCallLine(String label) {
            scrollToId("tvCallLine");
            try {
                waitVisible("tvCallLine").click();
            } catch (WebDriverException e) {
                return false;
            }
            return pickFromDialog(label);
        }

        /** Opens the call-type dialog and returns its option labels (dialog closed after). */
        public java.util.List<String> callTypeOptions() {
            java.util.List<String> out = new java.util.ArrayList<>();
            scrollToId("tvCallType");
            waitVisible("tvCallType").click();
            pause(800);
            for (WebElement e : driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceIdMatches(\"android:id/text1\")"))) {
                try { out.add(e.getText()); } catch (WebDriverException ignored) { }
            }
            driver.navigate().back();
            pause(500);
            return out;
        }

        /** Opens a list dialog from a form field and returns its option labels. */
        public java.util.List<String> dialogOptions(String fieldId) {
            java.util.List<String> out = new java.util.ArrayList<>();
            scrollToId(fieldId);
            waitVisible(fieldId).click();
            pause(800);
            for (WebElement e : driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceIdMatches(\"android:id/text1\")"))) {
                try { out.add(e.getText()); } catch (WebDriverException ignored) { }
            }
            driver.navigate().back();
            pause(500);
            return out;
        }

        /** Ticks a weekday chip ("Mon", "Wed", ...) of the Custom repeat. */
        public boolean tickDayChip(String day) {
            scrollToId("chipGroupDays");
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiSelector().resourceIdMatches(\".*:id/chipGroupDays\").childSelector("
                                + "new UiSelector().text(\"" + day + "\"))")).click();
                pause(400);
                return true;
            } catch (WebDriverException e) {
                return false;
            }
        }

        /** Exclude-me checkbox (cbExcludeMe). */
        public boolean tickExcludeMe() {
            scrollToId("cbExcludeMe");
            try {
                WebElement cb = waitVisible("cbExcludeMe");
                if (!"true".equals(cb.getAttribute("checked"))) cb.click();
                return true;
            } catch (WebDriverException e) {
                return false;
            }
        }

        public String inviteeCountText() { scrollToId("tvInviteeCount"); return textOf("tvInviteeCount"); }

        /**
         * Invitee picker (InviteePickerFragment): opens it, goes to "Select Users", searches the
         * query and ticks the row whose tvUserName contains {@code nameContains}, then Done.
         * Returns true when tvInviteeCount appears on the form.
         */
        public boolean addInvitee(String query, String nameContains) {
            scrollToId("btnAddInvitees");
            waitVisible("btnAddInvitees").click();
            if (!isVisible("btnDone", Duration.ofSeconds(15))) return false;
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiSelector().resourceIdMatches(\".*:id/tabLayout\").childSelector("
                                + "new UiSelector().textMatches(\"(?i)select users\"))")).click();
                pause(1200);
            } catch (WebDriverException ignored) {
            }
            try {
                WebElement s = waitVisible("etSearch");
                s.click();
                s.sendKeys(query);
                pause(1500);
            } catch (WebDriverException ignored) {
            }
            boolean ticked = false;
            for (int i = 0; i < 8 && !ticked; i++) {
                for (WebElement n : driver.findElements(byId("tvUserName"))) {
                    try {
                        if (n.getText().contains(nameContains)) {
                            n.click();
                            ticked = true;
                            break;
                        }
                    } catch (WebDriverException ignored) {
                    }
                }
                if (!ticked) pause(1000);
            }
            hideKeyboardIfShown();
            try {
                waitVisible("btnDone").click();
            } catch (WebDriverException e) {
                driver.navigate().back();
                return false;
            }
            isVisible("etTitle", Duration.ofSeconds(10));
            return ticked && !inviteeCountText().isEmpty();
        }

        /**
         * Sets the Start Date through the MaterialDatePicker's text-input mode: header toggle
         * (mtrl_picker_header_toggle) → type the date → OK. The input format follows the
         * device locale's short date, so it tries MM/dd/yyyy, then dd/MM/yyyy, and checks the
         * form's tvDate afterwards. The picker forbids past days (DateValidatorPointForward,
         * EventFormFragment.kt:618-631).
         */
        public boolean setDate(java.time.LocalDate date) {
            for (String pattern : new String[]{"MM/dd/yyyy", "dd/MM/yyyy"}) {
                scrollToId("tvDate");
                waitVisible("tvDate").click();
                if (!typeIntoDatePicker(date.format(java.time.format.DateTimeFormatter.ofPattern(pattern)))) {
                    continue;
                }
                if (date.equals(parseFormDate(textOf("tvDate")))) return true;
            }
            return false;
        }

        /** Sets Repeat Until (tvRecurrenceEnd, an unconstrained MaterialDatePicker) by typing. */
        public boolean setRecurrenceEnd(java.time.LocalDate date) {
            for (String pattern : new String[]{"MM/dd/yyyy", "dd/MM/yyyy"}) {
                scrollToId("tvRecurrenceEnd");
                if (!isVisible("tvRecurrenceEnd", Duration.ofSeconds(4))) return false;
                waitVisible("tvRecurrenceEnd").click();
                if (!typeIntoDatePicker(date.format(java.time.format.DateTimeFormatter.ofPattern(pattern)))) {
                    continue;
                }
                if (date.equals(parseFormDate(textOf("tvRecurrenceEnd")))) return true;
            }
            return false;
        }

        private boolean typeIntoDatePicker(String text) {
            if (!isVisible("mtrl_picker_header_toggle", Duration.ofSeconds(8))) {
                driver.navigate().back();
                return false;
            }
            if (driver.findElements(byId("mtrl_picker_text_input_date")).isEmpty()) {
                driver.findElement(byId("mtrl_picker_header_toggle")).click();
                pause(800);
            }
            try {
                WebElement field = waitVisible("mtrl_picker_text_input_date")
                        .findElement(By.className("android.widget.EditText"));
                field.click();
                field.clear();
                field.sendKeys(text);
                pause(500);
                WebElement ok = driver.findElement(byId("confirm_button"));
                if (!ok.isEnabled()) {
                    driver.navigate().back();
                    if (isVisible("mtrl_picker_header_toggle", Duration.ofSeconds(1))) driver.navigate().back();
                    return false;
                }
                ok.click();
                pause(800);
                return true;
            } catch (WebDriverException e) {
                driver.navigate().back();
                if (isVisible("mtrl_picker_header_toggle", Duration.ofSeconds(1))) driver.navigate().back();
                return false;
            }
        }

        /** Sets Start (or End) time through MaterialTimePicker's keyboard mode. */
        public boolean setStartTime(java.time.LocalTime t) { return setTimeChecked("tvStartTime", t); }
        public boolean setEndTime(java.time.LocalTime t)   { return setTimeChecked("tvEndTime", t); }

        /**
         * Sets a time and reads the field back; one retry when the picker typed something else.
         * Returns true when the field took the picked value OR the app deliberately corrected it
         * (past start / under-30-min end — callers assert those cases themselves).
         */
        private boolean setTimeChecked(String fieldId, java.time.LocalTime t) {
            String want = t.format(java.time.format.DateTimeFormatter.ofPattern("h:mm a", java.util.Locale.ENGLISH));
            for (int attempt = 0; attempt < 2; attempt++) {
                if (!setTime(fieldId, t)) continue;
                String got = textOf(fieldId);
                if (want.equalsIgnoreCase(got)) return true;
                System.out.println(">>> " + fieldId + ": picked " + want + " but the field reads '" + got + "'");
                if (attempt == 1) return true;   // the app's own correction — let the caller judge
            }
            return false;
        }

        private boolean setTime(String fieldId, java.time.LocalTime t) {
            scrollToId(fieldId);
            waitVisible(fieldId).click();
            if (!isVisible("material_timepicker_ok_button", Duration.ofSeconds(8))) return false;
            try {
                if (driver.findElements(byId("material_hour_text_input")).isEmpty()) {
                    driver.findElement(byId("material_timepicker_mode_button")).click();
                    pause(700);
                }
                int h12 = t.getHour() % 12 == 0 ? 12 : t.getHour() % 12;
                typeTimePart("material_hour_text_input", String.valueOf(h12));
                typeTimePart("material_minute_text_input", String.format("%02d", t.getMinute()));
                String period = t.getHour() < 12 ? "material_clock_period_am_button" : "material_clock_period_pm_button";
                driver.findElement(byId(period)).click();
                pause(300);
                driver.findElement(byId("material_timepicker_ok_button")).click();
                pause(800);
                return true;
            } catch (WebDriverException e) {
                driver.navigate().back();
                return false;
            }
        }

        private void typeTimePart(String comboId, String value) {
            WebElement combo = waitVisible(comboId);
            combo.click();
            pause(300);
            WebElement edit;
            java.util.List<WebElement> edits = combo.findElements(By.className("android.widget.EditText"));
            edit = edits.isEmpty() ? combo : edits.get(0);
            edit.clear();
            edit.sendKeys(value);
            pause(300);
        }

        /** Parses "h:mm a" from tvStartTime / tvEndTime. */
        public static java.time.LocalTime parseFormTime(String text) {
            if (text == null || text.isBlank()) return null;
            try {
                return java.time.LocalTime.parse(text.trim().toUpperCase(java.util.Locale.ENGLISH),
                        java.time.format.DateTimeFormatter.ofPattern("h:mm a", java.util.Locale.ENGLISH));
            } catch (java.time.format.DateTimeParseException e) {
                return null;
            }
        }

        /** Picks a preset colour swatch by position (ColorPickerDialog rvColors). */
        public boolean pickPresetColor(int index) {
            scrollToId("etTitle");
            try {
                driver.findElement(byId("layoutColorField")).click();
            } catch (WebDriverException e) {
                return false;
            }
            try {
                new WebDriverWait(driver, Duration.ofSeconds(5))
                        .until(ExpectedConditions.elementToBeClickable(AppiumBy.androidUIAutomator(
                                "new UiSelector().text(\"Select from preset colors\")")))
                        .click();
                new WebDriverWait(driver, Duration.ofSeconds(6))
                        .until(ExpectedConditions.visibilityOfElementLocated(byId("rvColors")));
                // swatch cells: new_calendar_item_color_swatch.xml (viewColor + ivCheck)
                java.util.List<WebElement> cells = driver.findElements(byId("viewColor"));
                if (cells.size() <= index) return false;
                cells.get(index).click();
                pause(800);
                return true;
            } catch (WebDriverException e) {
                return false;
            }
        }

        public String colorHexText() { scrollToId("etTitle"); return textOf("tvColorHex"); }

        /** Colour menu → "Select custom color": the host ColorPickerDialog (leftButton Cancel / rightButton Next). Cancels it. */
        public boolean openCustomColorDialog() {
            scrollToId("etTitle");
            try {
                driver.findElement(byId("layoutColorField")).click();
                new WebDriverWait(driver, Duration.ofSeconds(5))
                        .until(ExpectedConditions.elementToBeClickable(AppiumBy.androidUIAutomator(
                                "new UiSelector().text(\"Select custom color\")")))
                        .click();
                boolean open = isVisible("leftButton", Duration.ofSeconds(6)) && isVisible("rightButton", Duration.ofSeconds(2));
                if (open) driver.findElement(byId("leftButton")).click(); else driver.navigate().back();
                pause(600);
                return open;
            } catch (WebDriverException e) {
                return false;
            }
        }

        // ---------------- batch 2: external guests, invitee picker modes, pickers ----------------

        /** Types an address into an already-open date picker (text-input mode) and confirms. */
        public boolean confirmTypedDate(java.time.LocalDate date, String pattern) {
            return typeIntoDatePicker(date.format(java.time.format.DateTimeFormatter.ofPattern(pattern)));
        }

        public boolean isRecurrenceEndShown() {
            scrollToId("tvRecurrence");
            return !driver.findElements(byId("tvRecurrenceEnd")).isEmpty();
        }

        /**
         * Opens the Start Date picker (calendar mode, the month of the current Start Date) and
         * reports whether the day cell for {@code day} is enabled. Null when the cell is not in
         * the visible month. DateValidatorPointForward greys past days (EventFormFragment.kt:618-631).
         */
        public Boolean startDatePickerDayEnabled(java.time.LocalDate day) {
            scrollToId("tvDate");
            waitVisible("tvDate").click();
            if (!isVisible("mtrl_picker_header_toggle", Duration.ofSeconds(8))) return null;
            Boolean out = null;
            String want = String.valueOf(day.getDayOfMonth());
            String monthName = day.getMonth().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH);
            for (WebElement cell : driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().text(\"" + want + "\")"))) {
                try {
                    String desc = cell.getAttribute("content-desc");
                    if (desc != null && !desc.isBlank() && !desc.contains(monthName)) continue;
                    out = cell.isEnabled() && !"false".equals(cell.getAttribute("clickable"));
                    break;
                } catch (WebDriverException ignored) {
                }
            }
            driver.navigate().back();
            pause(600);
            return out;
        }

        // external guests (ExternalEmailFragment: etEmail, btnAddEmail, rvEmails > tvEmail/btnRemove, btnDone)
        public boolean openExternalGuests() {
            scrollToId("btnAddExternalEmails");
            try {
                waitVisible("btnAddExternalEmails").click();
                return isVisible("etEmail", Duration.ofSeconds(10));
            } catch (WebDriverException e) {
                return false;
            }
        }

        /** Types an address and commits it with Add (or the IME action when {@code ime}). */
        public void externalAdd(String email, boolean ime) {
            WebElement f = waitVisible("etEmail");
            f.click();
            f.clear();
            f.sendKeys(email);
            pause(400);
            if (ime) {
                try {
                    driver.executeScript("mobile: performEditorAction", java.util.Map.of("action", "done"));
                } catch (WebDriverException e) {
                    driver.pressKey(new io.appium.java_client.android.nativekey.KeyEvent(
                            io.appium.java_client.android.nativekey.AndroidKey.ENTER));
                }
            } else {
                try {
                    driver.findElement(byId("btnAddEmail")).click();
                } catch (WebDriverException ignored) {
                    // Add is hidden for an invalid / duplicate address (updateAddButtonVisibility)
                }
            }
            pause(900);
            answerExternalPrompt();
        }

        /** "Do you want to add this user as an external user?" → No (never register a user). */
        public void answerExternalPrompt() {
            if (textShown("add this user as an external user", Duration.ofSeconds(2))) {
                try {
                    driver.findElement(AppiumBy.androidUIAutomator("new UiSelector().textMatches(\"(?i)no\")")).click();
                    pause(500);
                } catch (WebDriverException ignored) {
                }
            }
        }

        public java.util.List<String> externalEmails() {
            java.util.List<String> out = new java.util.ArrayList<>();
            for (WebElement e : driver.findElements(byId("tvEmail"))) {
                try { out.add(e.getText()); } catch (WebDriverException ignored) { }
            }
            return out;
        }

        public boolean externalRemoveFirst() {
            try {
                driver.findElements(byId("btnRemove")).get(0).click();
                pause(600);
                return true;
            } catch (WebDriverException | IndexOutOfBoundsException e) {
                return false;
            }
        }

        public void externalDone() {
            hideKeyboardIfShown();
            try {
                waitVisible("btnDone").click();
            } catch (WebDriverException e) {
                driver.navigate().back();
            }
            isVisible("etTitle", Duration.ofSeconds(10));
        }

        public String externalCountText() {
            scrollToId("tvExternalEmailCount");
            return driver.findElements(byId("tvExternalEmailCount")).isEmpty() ? "" : textOf("tvExternalEmailCount");
        }

        /** The form-level Clear next to a count row (tvClearInvitees / tvClearExternalEmails). */
        public boolean tapFormClear(String id) {
            scrollToId(id);
            try {
                waitVisible(id).click();
                pause(600);
                return true;
            } catch (WebDriverException e) {
                return false;
            }
        }

        // location (etLocation is not editable — a tap opens MapPickerFragment)
        public void tapLocationField() {
            scrollToId("etLocation");
            try { waitVisible("etLocation").click(); } catch (WebDriverException ignored) { }
        }

        /** In the map picker: waits for an address under the crosshair, then Confirm. */
        public boolean confirmMapPicker() {
            if (!isVisible("btnConfirm", Duration.ofSeconds(15))) return false;
            long end = System.currentTimeMillis() + 20000;
            while (System.currentTimeMillis() < end) {
                String a = textOf("tvPickedAddress");
                WebElement b = driver.findElement(byId("btnConfirm"));
                if (b.isEnabled() && !a.isBlank() && !a.toLowerCase().contains("move the map")) break;
                pause(1000);
            }
            try {
                driver.findElement(byId("btnConfirm")).click();
            } catch (WebDriverException e) {
                driver.navigate().back();
                return false;
            }
            // back on the (scrolled) form: etTitle may be off-screen, the location row is not
            pause(1000);
            scrollToId("etLocation");
            return isVisible("etLocation", Duration.ofSeconds(10));
        }

        public String locationText() { scrollToId("etLocation"); return textOf("etLocation"); }

        // invitee picker (InviteePickerFragment)
        public boolean openInviteePicker() {
            scrollToId("btnAddInvitees");
            try {
                waitVisible("btnAddInvitees").click();
                return isVisible("btnDone", Duration.ofSeconds(15));
            } catch (WebDriverException e) {
                return false;
            }
        }

        public java.util.List<String> pickerTabs() {
            java.util.List<String> out = new java.util.ArrayList<>();
            // UiSelector.childSelector returns only the first match — use XPath for all tab labels.
            for (WebElement t : driver.findElements(By.xpath(
                    "//*[@resource-id='" + appPackage() + ":id/tabLayout']//android.widget.TextView"))) {
                try {
                    String x = t.getText();
                    if (x != null && !x.isBlank()) out.add(x);
                } catch (WebDriverException ignored) { }
            }
            return out;
        }

        public boolean pickerTab(String label) {
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiSelector().resourceIdMatches(\".*:id/tabLayout\").childSelector("
                                + "new UiSelector().textMatches(\"(?i)" + label + "\"))")).click();
                pause(1500);
                return true;
            } catch (WebDriverException e) {
                return false;
            }
        }

        public String pickerDoneText() { return textOf("btnDone"); }

        public boolean pickerTapSelectAll() {
            try {
                waitVisible("btnSelectAll").click();
                pause(800);
                return true;
            } catch (WebDriverException e) {
                return false;
            }
        }

        public String pickerSelectAllText() { return textOf("btnSelectAll"); }

        /**
         * Taps the first department that actually has members ("x/N selected" with N > 0,
         * DepartmentAdapter) — the list also shows empty departments, and the creator's own
         * department shows 0 because self is filtered out. Scrolls the list a few times.
         */
        public boolean pickerTapFirstDepartment() {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("(\\d+)/(\\d+) selected");
            for (int page = 0; page < 6; page++) {
                for (WebElement count : driver.findElements(byId("tvDeptCount"))) {
                    try {
                        java.util.regex.Matcher m = p.matcher(count.getText());
                        if (!m.find() || Integer.parseInt(m.group(2)) == 0) continue;
                        Band band = bandOfAnchor("tvDeptCount", count);
                        WebElement cb = elementIn(band, "cbDept");
                        (cb != null ? cb : count).click();
                        pause(800);
                        return true;
                    } catch (WebDriverException ignored) {
                    }
                }
                if (!scrollList("rvUsers", true)) break;
                pause(600);
            }
            return false;
        }

        public void pickerSearch(String q) {
            try {
                WebElement s = waitVisible("etSearch");
                s.click();
                s.clear();
                s.sendKeys(q);
                pause(1500);
            } catch (WebDriverException ignored) {
            }
        }

        public java.util.List<String> pickerNames() {
            java.util.List<String> out = new java.util.ArrayList<>();
            for (WebElement n : driver.findElements(byId("tvUserName"))) {
                try { out.add(n.getText()); } catch (WebDriverException ignored) { }
            }
            return out;
        }

        public void pickerDone() {
            hideKeyboardIfShown();
            try {
                waitVisible("btnDone").click();
            } catch (WebDriverException e) {
                driver.navigate().back();
            }
            isVisible("etTitle", Duration.ofSeconds(10));
        }

        private static void pause(long ms) {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        /** Invitee picker: open, pick the first member, tap Done; returns true if a member was added. */
        public boolean addFirstInvitee() {
            scrollToId("btnAddInvitees");
            waitVisible("btnAddInvitees").click();
            if (!isVisible("btnDone", Duration.ofSeconds(8))) {
                driver.navigate().back();
                return false;
            }
            var rows = driver.findElements(byId("tvName"));
            if (rows.isEmpty()) {
                driver.navigate().back();
                return false;
            }
            rows.get(0).click();
            waitVisible("btnDone").click();
            return true;
        }

        public boolean isInviteeSearchable() {
            scrollToId("btnAddInvitees");
            waitVisible("btnAddInvitees").click();
            boolean ok = isVisible("etSearch", Duration.ofSeconds(8))
                    && isVisible("btnDone", Duration.ofSeconds(2));
            driver.navigate().back();
            return ok;
        }

        /** Color picker: open the color field menu and pick the first swatch. */
        public boolean pickFirstColor() {
            scrollToId("etTitle");
            try {
                driver.findElement(byId("viewColorCircle")).click();
            } catch (WebDriverException e) {
                try { driver.findElement(byId("layoutColorField")).click(); }
                catch (WebDriverException e2) { return false; }
            }
            // Popup menu → "preset colors" opens rvColors, or swatches appear inline.
            try {
                new WebDriverWait(driver, Duration.ofSeconds(4))
                        .until(ExpectedConditions.elementToBeClickable(AppiumBy.androidUIAutomator(
                                "new UiSelector().textMatches(\"(?i).*(preset|color).*\")")))
                        .click();
            } catch (WebDriverException ignored) {
            }
            try {
                waitVisible("rvColors");
                // rvColors is a GridView of FrameLayout cells (new_calendar_item_color_swatch.xml:
                // viewColor + ivCheck) — there is no ViewGroup child, so the old lookup always
                // failed and colorPicker always skipped.
                driver.findElements(byId("viewColor")).get(0).click();
                return true;
            } catch (WebDriverException | IndexOutOfBoundsException e) {
                driver.navigate().back();
                return false;
            }
        }

        /** Opens the date picker and confirms it (MaterialDatePicker "OK"). */
        public boolean openAndConfirmDate() {
            scrollToId("tvDate");
            waitVisible("tvDate").click();
            return confirmMaterialPicker();
        }

        /** Opens the start-time picker and confirms it (MaterialTimePicker "OK"). */
        public boolean openAndConfirmStartTime() {
            scrollToId("tvStartTime");
            waitVisible("tvStartTime").click();
            return confirmMaterialPicker();
        }

        /**
         * Sets a recurrence end date by opening tvRecurrenceEnd and picking a
         * late day in the visible month (so it is after the start date), then OK.
         */
        public boolean setRecurrenceEndLate() {
            scrollToId("tvRecurrenceEnd");
            if (!isVisible("tvRecurrenceEnd", Duration.ofSeconds(4))) {
                return false;
            }
            waitVisible("tvRecurrenceEnd").click();
            // In the MaterialDatePicker grid, tap a high day number then OK.
            for (String day : new String[]{"28", "27", "26", "25", "24"}) {
                try {
                    driver.findElement(AppiumBy.androidUIAutomator(
                            "new UiSelector().textMatches(\"" + day + "\")")).click();
                    break;
                } catch (WebDriverException ignored) {
                }
            }
            return confirmMaterialPicker();
        }

        /** Repeat-until field text (tvRecurrenceEnd, format "EEE, MMM d, yyyy"); "" if hidden. */
        public String recurrenceEndText() {
            scrollToId("tvRecurrenceEnd");
            return textOf("tvRecurrenceEnd");
        }

        /**
         * Sets Repeat Until to a date BEFORE the start date: opens tvRecurrenceEnd
         * (an unconstrained MaterialDatePicker that opens on the current month —
         * EventFormFragment.showRecurrenceEndPicker), steps back one month, taps
         * day 1, then OK. Returns false if the picker could not be driven.
         */
        public boolean setRecurrenceEndBeforeStart() {
            scrollToId("tvRecurrenceEnd");
            if (!isVisible("tvRecurrenceEnd", Duration.ofSeconds(4))) {
                return false;
            }
            waitVisible("tvRecurrenceEnd").click();
            try {
                new WebDriverWait(driver, Duration.ofSeconds(6))
                        .until(ExpectedConditions.elementToBeClickable(AppiumBy.androidUIAutomator(
                                "new UiSelector().descriptionMatches(\"(?i).*previous month.*\")")))
                        .click();
            } catch (WebDriverException e) {
                try {
                    driver.findElement(AppiumBy.androidUIAutomator(
                            "new UiSelector().resourceIdMatches(\".*:id/month_navigation_previous\")")).click();
                } catch (WebDriverException e2) {
                    driver.navigate().back();
                    return false;
                }
            }
            boolean picked = false;
            for (int i = 0; i < 6 && !picked; i++) {
                for (WebElement day : driver.findElements(AppiumBy.androidUIAutomator(
                        "new UiSelector().text(\"1\")"))) {
                    try {
                        if (day.isDisplayed()) {
                            day.click();
                            picked = true;
                            break;
                        }
                    } catch (WebDriverException ignored) {
                    }
                }
                if (!picked) {
                    try { Thread.sleep(400); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                }
            }
            if (!picked) {
                driver.navigate().back();
                return false;
            }
            return confirmMaterialPicker();
        }

        /** Parses the form's date text ("EEE, MMM d, yyyy" — EventFormFragment.kt:49); null if unparseable. */
        public static java.time.LocalDate parseFormDate(String text) {
            if (text == null || text.isBlank()) return null;
            try {
                return java.time.LocalDate.parse(text.trim(),
                        java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", java.util.Locale.ENGLISH));
            } catch (java.time.format.DateTimeParseException e) {
                return null;
            }
        }

        private boolean confirmMaterialPicker() {
            try {
                new WebDriverWait(driver, Duration.ofSeconds(6))
                        .until(ExpectedConditions.elementToBeClickable(AppiumBy.androidUIAutomator(
                                "new UiSelector().textMatches(\"(?i)ok\")")))
                        .click();
                return true;
            } catch (WebDriverException e) {
                driver.navigate().back();
                return false;
            }
        }

        private boolean pickFromDialog(String label) {
            try {
                new WebDriverWait(driver, Duration.ofSeconds(6))
                        .until(ExpectedConditions.elementToBeClickable(AppiumBy.androidUIAutomator(
                                "new UiSelector().textMatches(\"(?i)" + label + "\")")))
                        .click();
                return true;
            } catch (WebDriverException e) {
                driver.navigate().back();
                return false;
            }
        }

        /** True once the form has left (a successful save returns to the calendar). */
        public boolean waitFormClosed(Duration timeout) {
            long deadline = System.currentTimeMillis() + timeout.toMillis();
            while (System.currentTimeMillis() < deadline) {
                if (!isVisible("etTitle", Duration.ofSeconds(1))
                        && !isVisible("btnSave", Duration.ofSeconds(1))) {
                    return true;
                }
            }
            return false;
        }

        public void tapSave() {
            scrollToId("btnSave");
            waitVisible("btnSave").click();
        }

        /**
         * True if the form is still open a moment after tapping Save — i.e. a
         * validation blocked the save (a valid save navigates back to the
         * calendar and the title field disappears). This is the reliable signal
         * on Android versions where Toasts aren't exposed to accessibility.
         */
        public boolean stillOnFormAfterSave(Duration settle) {
            try {
                Thread.sleep(settle.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return isVisible("etTitle", Duration.ofSeconds(4))
                    || isVisible("btnSave", Duration.ofSeconds(2));
        }

        public void goBack() {
            driver.navigate().back();
        }

        public void dismissDialog() {
            driver.navigate().back();
        }

        private void tap(String id) {
            scrollToId(id);
            waitVisible(id).click();
        }

        /**
         * Polls fast for a transient Toast containing the text. Toasts last
         * ~2s, so we check rapidly right after the action that raised one.
         */
        public boolean toastShown(String text, Duration timeout) {
            long deadline = System.currentTimeMillis() + timeout.toMillis();
            By locator = AppiumBy.androidUIAutomator(
                    "new UiSelector().textContains(\"" + text.replace("\"", "\\\"") + "\")");
            while (System.currentTimeMillis() < deadline) {
                try {
                    if (!driver.findElements(locator).isEmpty()) {
                        return true;
                    }
                } catch (WebDriverException ignored) {
                }
            }
            return false;
        }
    }
}
