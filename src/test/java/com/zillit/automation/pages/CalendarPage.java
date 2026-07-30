package com.zillit.automation.pages;

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

    public boolean isEventsButtonVisible() {
        return isVisible("btnEvents", Duration.ofSeconds(4));
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
            hideKeyboard();
        }

        public String descriptionText() { return textOf("etDescription"); }

        public void selectMembersType() {
            tap("btnTypeMembers");
        }

        public void selectPersonalType() {
            tap("btnTypePersonal");
        }

        public boolean isInviteeControlVisible() {
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

        /** Timezone picker screen: search + pick first row; returns new tz text. */
        public String selectTimezone(String query) {
            scrollToId("tvTimezone");
            waitVisible("tvTimezone").click();
            if (!isVisible("etSearch", Duration.ofSeconds(8))) {
                driver.navigate().back();
                return textOf("tvTimezone");
            }
            org.openqa.selenium.WebElement s = waitVisible("etSearch");
            s.clear();
            s.sendKeys(query);
            hideKeyboard();
            try {
                new WebDriverWait(driver, Duration.ofSeconds(6))
                        .until(ExpectedConditions.elementToBeClickable(byId("tvTimezoneValue")))
                        .click();
            } catch (WebDriverException e) {
                driver.navigate().back();
            }
            return textOf("tvTimezone");
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
                waitVisible("rvColors").findElement(By.className("android.view.ViewGroup")).click();
                return true;
            } catch (WebDriverException e) {
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
