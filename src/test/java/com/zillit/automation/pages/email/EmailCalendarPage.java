package com.zillit.automation.pages.email;

import com.zillit.automation.pages.calendar.CalendarPage;
import io.appium.java_client.android.AndroidDriver;

import java.time.Duration;

/**
 * The EMAIL drawer Calendar. The drawer opens EmailCalendarActivity
 * (new_email/ui/folders/FolderDrawerFragment.kt:87-89), which hosts the SAME
 * new-calendar {@code CalendarFragment} the Home unit uses
 * (new_email/ui/calendar/EmailCalendarActivity.kt:15-38) — so the ids are the
 * new calendar's (layout new_calendar_fragment_calendar.xml +
 * new_calendar_layout_calendar_header.xml): fab, monthGrid, tvMonthTitle,
 * btnPrevMonth / btnToday / btnNextMonth, btnViewMode → tvViewMode.
 * Everything is inherited from {@link CalendarPage}; this class only adds the
 * timeout-taking load check the email suite uses.
 */
public class EmailCalendarPage extends CalendarPage {

    public EmailCalendarPage(AndroidDriver driver) {
        super(driver);
    }

    /** Calendar screen visible: the add-event FAB, the month grid or the month title. */
    public boolean isLoaded(Duration timeout) {
        return isVisible("fab", timeout)
                || isVisible("monthGrid", Duration.ofSeconds(2))
                || isVisible("tvMonthTitle", Duration.ofSeconds(2));
    }
}
