package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.AppPreferencesPage;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.ProjectListPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Settings → "App Preferences".
 *
 * <p>Checks that each preference actually takes effect rather than merely being
 * recorded: the chat font size is measured on a real Bulletin message, the theme
 * is measured as screen luminance, and the default home unit is proved by
 * restarting the app and reopening the project.
 *
 * <p>Two settings have no readable value in the accessibility tree, so both are
 * measured:
 * <ul>
 *   <li><b>font size</b> — the {@code tvMessage} bubble height. Live on one
 *       message: Large 85px, Medium 69px, Small 61px. The assertions compare
 *       against the previous reading, never a fixed number, because the height
 *       depends on the message text;</li>
 *   <li><b>theme</b> — average screen luminance, 187/255 in Light versus 35/255
 *       in Dark. The app themes itself, so the system night-mode flag never
 *       changes and there is nothing to read.</li>
 * </ul>
 *
 * <p><b>Leaves the app in Dark mode with a non-Bulletin default unit</b>, and
 * posts one Bulletin message per run. TC1 re-reads whatever the current settings
 * are rather than assuming, so a rerun is unaffected by the last one.
 */
@Order(TestOrder.APP_PREFERENCES)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AppPreferencesTest extends BaseTest {

    private static final Duration WAIT = Duration.ofSeconds(20);
    private static final Duration LOAD_WAIT = Duration.ofSeconds(60);

    /** Marks this run's Bulletin message so the right bubble is measured. */
    private static final String MESSAGE = "FontSizeCheck";

    /** A tool nested under the Tools section. */
    private static final String MODULE_TOOL = "Confidential Info";
    /** A top-level section, muted to prove both levels behave the same. */
    private static final String MODULE_SECTION = "Email";
    private static final String SECTION_TOOLS = "Tools";
    /** The Home sub-tab the chat messages are posted to. */
    private static final String BULLETIN = "Bulletin";

    // Times are always SET to a known value and then asserted against that value,
    // never against "different from before". The field's starting value varies by
    // project and by previous run, so a "should have changed" assertion fails
    // whenever the field already happens to hold the target time.
    //
    // Ordering is guaranteed by the meridiem, not by the hour: an AM start is
    // always before a PM end whatever the hours are.
    private static final String VALID_START_H = "5";
    private static final String VALID_START_M = "0";
    private static final String VALID_START = "05:00 AM";

    private static final String VALID_END_H = "9";
    private static final String VALID_END_M = "0";
    private static final String VALID_END = "09:00 PM";

    /** 12 AM is midnight — earlier than any other time, so always before the start. */
    private static final String EARLY_H = "12";
    private static final String EARLY_M = "0";

    /** 11 PM is later than the 9 PM end, so always after it. */
    private static final String LATE_H = "11";
    private static final String LATE_M = "0";

    /** The weekday exercised in Selected Day mode. */
    private static final String DAY = "Monday";
    private static final List<String> WEEK = List.of(
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");

    private boolean insideProject;

    private static int heightLarge = -1;
    private static int heightMedium = -1;
    private static int brightnessBefore = -1;
    private static String projectName;
    private static String chosenUnit;

    @Override
    protected boolean relaunchBetweenTests() {
        return false;
    }

    @Override
    protected boolean requiresLoggedOutStart() {
        return false;
    }

    private AppPreferencesPage prefs() {
        if (!insideProject) {
            if (landing == Landing.PROJECT_LIST) {
                ProjectListPage list = new ProjectListPage(driver);
                List<String> names = list.visibleProjectNames();
                assumeTrue(!names.isEmpty(), "No projects");
                assumeTrue(list.tapProject(names.get(0)), "Could not enter a project");
            }
            assumeTrue(new BottomNavPage(driver).waitUntilLoaded(LOAD_WAIT), "No project");
            insideProject = true;
        }
        AppPreferencesPage page = new AppPreferencesPage(driver);
        assumeTrue(page.ensureOnAppPreferences(LOAD_WAIT),
                "Could not reach Settings -> App Preferences");
        return page;
    }

    @Test
    @Order(1)
    @DisplayName("TC1: App Preferences opens and Chat Font Size offers Small, Medium and Large")
    void fontSizeOptions() {
        AppPreferencesPage page = prefs();
        assertTrue(page.isLoaded(WAIT), "APP PREFERENCES should be open");
        System.out.println(">>> font=" + page.selectedFontSize()
                + " theme=" + page.selectedTheme()
                + " defaultUnit=" + page.defaultHomeUnit());
        screenshot("prefs-open");

        assertTrue(page.openFontSizeDialog(), "Chat Font Size should be tappable");
        assertTrue(page.isFontDialogOpen(WAIT), "The font size dialog should open");

        List<String> options = page.fontOptions();
        screenshot("prefs-font-options");
        assertEquals(List.of(AppPreferencesPage.FONT_SMALL, AppPreferencesPage.FONT_MEDIUM,
                        AppPreferencesPage.FONT_LARGE), options,
                "The dialog should offer exactly Small, Medium and Large");
    }

    @Test
    @Order(2)
    @DisplayName("TC2: Large font — a Bulletin message is posted and measured")
    void largeFontMessage() {
        AppPreferencesPage page = new AppPreferencesPage(driver);
        assumeTrue(page.isFontDialogOpen(Duration.ofSeconds(10)), "The font dialog is not open");

        assertTrue(page.chooseFontSize(AppPreferencesPage.FONT_LARGE), "Large should be selectable");
        assertEquals(AppPreferencesPage.FONT_LARGE, page.selectedFontSize(),
                "The setting should now read Large");

        assertTrue(page.tapBack(), "Back should be tappable");
        assertTrue(page.openHomeTab(), "Home tab should be reachable");
        // Select Bulletin explicitly. The default home unit may have been changed
        // by an earlier run, in which case Home opens on that unit instead and the
        // message would be posted to the wrong place.
        assertTrue(page.openHomeUnitTab(BULLETIN), "Bulletin sub-tab should be selectable");
        assertTrue(page.typeMessage(MESSAGE + " " + System.nanoTime() % 100000),
                "The Bulletin message box should accept input");
        assertTrue(page.sendMessage(), "Send should be tappable");
        assertTrue(page.messageShown(MESSAGE, Duration.ofSeconds(30)),
                "The posted message should appear in the Bulletin");

        heightLarge = page.messageHeight(MESSAGE);
        System.out.println(">>> message height at LARGE: " + heightLarge + "px");
        screenshot("prefs-font-large");
        assertTrue(heightLarge > 0, "Should be able to measure the message bubble");
    }

    @Test
    @Order(3)
    @DisplayName("TC3: Medium font makes the same message smaller")
    void mediumFontShrinks() {
        assumeTrue(heightLarge > 0, "TC2 did not measure the Large height");
        AppPreferencesPage page = prefs();

        assertTrue(page.openFontSizeDialog(), "Chat Font Size should be tappable");
        assertTrue(page.isFontDialogOpen(WAIT), "The font dialog should open");
        assertTrue(page.chooseFontSize(AppPreferencesPage.FONT_MEDIUM), "Medium should be selectable");
        assertTrue(page.tapBack(), "Back should be tappable");
        assertTrue(page.openHomeTab(), "Home tab should be reachable");
        assertTrue(page.openHomeUnitTab(BULLETIN), "Bulletin sub-tab should be selectable");
        assertTrue(page.messageShown(MESSAGE, WAIT), "The message should still be listed");

        heightMedium = page.messageHeight(MESSAGE);
        System.out.println(">>> message height at MEDIUM: " + heightMedium + "px (Large was "
                + heightLarge + "px)");
        screenshot("prefs-font-medium");
        assertTrue(heightMedium > 0, "Should be able to measure the message bubble");
        assertTrue(heightMedium < heightLarge,
                "Medium should render smaller than Large; got " + heightMedium + " vs " + heightLarge);
    }

    @Test
    @Order(4)
    @DisplayName("TC4: Small font shrinks it further still")
    void smallFontShrinksMore() {
        assumeTrue(heightMedium > 0, "TC3 did not measure the Medium height");
        AppPreferencesPage page = prefs();

        assertTrue(page.openFontSizeDialog(), "Chat Font Size should be tappable");
        assertTrue(page.isFontDialogOpen(WAIT), "The font dialog should open");
        assertTrue(page.chooseFontSize(AppPreferencesPage.FONT_SMALL), "Small should be selectable");
        assertTrue(page.tapBack(), "Back should be tappable");
        assertTrue(page.openHomeTab(), "Home tab should be reachable");
        assertTrue(page.openHomeUnitTab(BULLETIN), "Bulletin sub-tab should be selectable");
        assertTrue(page.messageShown(MESSAGE, WAIT), "The message should still be listed");

        int small = page.messageHeight(MESSAGE);
        System.out.println(">>> message height at SMALL: " + small + "px (Medium was "
                + heightMedium + "px, Large was " + heightLarge + "px)");
        screenshot("prefs-font-small");
        assertTrue(small > 0, "Should be able to measure the message bubble");
        assertTrue(small < heightMedium,
                "Small should render smaller than Medium; got " + small + " vs " + heightMedium);
    }

    @Test
    @Order(5)
    @DisplayName("TC5: Theme offers System Default, Light and Dark, and Dark darkens the UI")
    void darkTheme() {
        AppPreferencesPage page = prefs();

        brightnessBefore = page.screenBrightness();
        System.out.println(">>> screen brightness before: " + brightnessBefore + "/255");

        assertTrue(page.openThemeDialog(), "Theme should be tappable");
        assertTrue(page.isThemeDialogOpen(WAIT), "The theme dialog should open");
        List<String> options = page.themeOptions();
        screenshot("prefs-theme-options");
        assertEquals(List.of(AppPreferencesPage.THEME_SYSTEM, AppPreferencesPage.THEME_LIGHT,
                        AppPreferencesPage.THEME_DARK), options,
                "The dialog should offer exactly System Default, Light and Dark");

        assertTrue(page.chooseTheme(AppPreferencesPage.THEME_DARK), "Dark should be selectable");
        assertEquals(AppPreferencesPage.THEME_DARK, page.selectedTheme(),
                "The setting should now read Dark");

        int after = page.screenBrightness();
        System.out.println(">>> screen brightness after: " + after + "/255");
        screenshot("prefs-theme-dark");

        assertTrue(after >= 0, "Should be able to measure the screen");
        assertTrue(after < 100,
                "Dark mode should render a dark screen; average luminance was " + after + "/255");
        if (brightnessBefore > 0) {
            assertTrue(after < brightnessBefore,
                    "The screen should be darker than before; " + after + " vs " + brightnessBefore);
        }
        System.out.println("UI appear in dark successfully");
    }

    @Test
    @Order(6)
    @DisplayName("TC6: A unit that is not already selected can be searched and set as the default")
    void setDefaultHomeUnit() {
        AppPreferencesPage page = prefs();

        // Pick something OTHER than whatever is already the default. Excluding a
        // fixed name instead would re-select the current unit on any run where it
        // is already set, and re-picking it changes nothing.
        String current = page.defaultHomeUnit();
        System.out.println(">>> current default home unit: " + current);

        assertTrue(page.openDefaultUnitPicker(), "Default Home Unit should be tappable");
        assertTrue(page.isOnUnitPicker(WAIT), "SELECT DEFAULT HOME UNIT should open");

        List<String> units = page.unitOptions();
        System.out.println(">>> units offered: " + units);
        chosenUnit = units.stream()
                .filter(u -> !u.equalsIgnoreCase(current))
                .findFirst().orElse(null);
        assumeTrue(chosenUnit != null,
                "Only '" + current + "' is available — no other unit to switch to");

        assertTrue(page.searchUnit(chosenUnit), "The unit search should accept input");
        assertTrue(page.unitOptions().contains(chosenUnit),
                "Searching should list " + chosenUnit + "; got " + page.unitOptions());
        assertTrue(page.chooseUnit(chosenUnit), chosenUnit + " should be selectable");

        assertTrue(page.isLoaded(Duration.ofSeconds(30)), "Should return to APP PREFERENCES");
        assertEquals(chosenUnit, page.defaultHomeUnit(),
                "The default home unit should now be " + chosenUnit + " (was " + current + ")");
        screenshot("prefs-default-unit");

        projectName = page.projectNameOnTop();
        System.out.println(">>> project: " + projectName + " | default unit: " + chosenUnit);
        assertTrue(!projectName.isBlank(), "Should be able to read the project name from the header");
    }

    @Test
    @Order(7)
    @DisplayName("TC7: After restarting the app, the project opens on that unit")
    void defaultUnitSurvivesRestart() {
        assumeTrue(chosenUnit != null && projectName != null && !projectName.isBlank(),
                "TC6 did not set a default unit");
        AppPreferencesPage page = new AppPreferencesPage(driver);

        String pkg = appPackageUnderTest();
        driver.terminateApp(pkg);
        sleep(3000);
        driver.activateApp(pkg);
        sleep(8000);

        ProjectListPage list = new ProjectListPage(driver);
        assertTrue(list.isLoaded(), "Restarting should land on the project list");

        // Project rows carry a role suffix ("NAME - Admin"), so this matches on the
        // name with one trailing " - token" stripped, not on a substring.
        list.search(projectName);
        assertTrue(list.tapProjectExact(projectName),
                "Should be able to reopen " + projectName + "; search listed "
                        + list.listedProjectNames());
        assertTrue(new BottomNavPage(driver).waitUntilLoaded(LOAD_WAIT),
                projectName + " should finish loading");

        assertTrue(page.homeUnitTabSelected(chosenUnit, Duration.ofSeconds(45)),
                "Home should open on " + chosenUnit + "; the selected tab was '"
                        + page.selectedHomeUnitTab() + "'");
        screenshot("prefs-default-unit-after-restart");
        System.out.println(">>> Home opened on: " + page.selectedHomeUnitTab());
    }

    @Test
    @Order(8)
    @DisplayName("TC8: Manage Notification search finds a module and its toggle turns off")
    void muteConfidentialInfo() {
        AppPreferencesPage page = prefs();

        assertTrue(page.openManageNotification(), "Manage Notification should be tappable");
        assertTrue(page.isOnManageNotification(WAIT), "MANAGE NOTIFICATION should open");
        screenshot("prefs-manage-notification");

        assertTrue(page.searchNotification(MODULE_TOOL), "The search box should accept input");
        System.out.println(">>> after searching '" + MODULE_TOOL + "': " + page.notificationModules());
        assertTrue(page.notificationModules().contains(MODULE_TOOL),
                "Searching should list " + MODULE_TOOL + "; got " + page.notificationModules());

        Boolean before = page.notificationEnabled(MODULE_TOOL);
        assumeTrue(before != null, "No toggle found for " + MODULE_TOOL);
        assumeTrue(before, MODULE_TOOL + " is already muted — nothing to turn off");

        assertTrue(page.tapNotificationToggle(MODULE_TOOL), "The toggle should be tappable");
        assertTrue(page.waitForNotification(MODULE_TOOL, false, WAIT),
                MODULE_TOOL + " should end up disabled");
        screenshot("prefs-notification-tool-off");
    }

    @Test
    @Order(9)
    @DisplayName("TC9: Clearing the search and muting a second module works too")
    void muteEmail() {
        AppPreferencesPage page = new AppPreferencesPage(driver);
        assumeTrue(page.isOnManageNotification(Duration.ofSeconds(10)), "Not on MANAGE NOTIFICATION");

        assertTrue(page.clearNotificationSearch(), "The search box should clear");
        assertTrue(page.searchNotification(MODULE_SECTION), "The search box should accept input");
        System.out.println(">>> after searching '" + MODULE_SECTION + "': " + page.notificationModules());
        assertTrue(page.notificationModules().contains(MODULE_SECTION),
                "Searching should list " + MODULE_SECTION + "; got " + page.notificationModules());

        Boolean before = page.notificationEnabled(MODULE_SECTION);
        assumeTrue(before != null, "No toggle found for " + MODULE_SECTION);
        assumeTrue(before, MODULE_SECTION + " is already muted — nothing to turn off");

        assertTrue(page.tapNotificationToggle(MODULE_SECTION), "The toggle should be tappable");
        assertTrue(page.waitForNotification(MODULE_SECTION, false, WAIT),
                MODULE_SECTION + " should end up disabled");
        screenshot("prefs-notification-email-off");
    }

    @Test
    @Order(10)
    @DisplayName("TC10: The Tools arrow collapses its list, and tapping again expands it")
    void toolsSectionCollapses() {
        AppPreferencesPage page = new AppPreferencesPage(driver);
        assumeTrue(page.isOnManageNotification(Duration.ofSeconds(10)), "Not on MANAGE NOTIFICATION");

        assertTrue(page.clearNotificationSearch(), "The search box should clear");
        List<String> expanded = page.notificationModules();
        System.out.println(">>> expanded: " + expanded);
        assumeTrue(expanded.contains(SECTION_TOOLS), "Tools section is not listed");

        // Its children are whatever follows Tools in the list.
        int toolsAt = expanded.indexOf(SECTION_TOOLS);
        assumeTrue(toolsAt >= 0 && toolsAt < expanded.size() - 1,
                "Tools has no children listed, so there is nothing to collapse");
        String firstChild = expanded.get(toolsAt + 1);

        assertTrue(page.tapSectionArrow(SECTION_TOOLS), "The Tools arrow should be tappable");
        List<String> collapsed = page.notificationModules();
        System.out.println(">>> collapsed: " + collapsed);
        screenshot("prefs-tools-collapsed");
        assertTrue(!collapsed.contains(firstChild),
                "Collapsing should hide " + firstChild + "; got " + collapsed);
        assertTrue(collapsed.contains(SECTION_TOOLS), "Tools itself should stay listed");

        assertTrue(page.tapSectionArrow(SECTION_TOOLS), "The Tools arrow should be tappable again");
        List<String> reopened = page.notificationModules();
        System.out.println(">>> re-expanded: " + reopened);
        screenshot("prefs-tools-expanded");
        assertTrue(reopened.contains(firstChild),
                "Expanding should bring " + firstChild + " back; got " + reopened);

        // Leave Manage Notification here — the cases that follow work on App
        // Preferences itself, so this is the only point where the back arrow
        // out of this screen can be exercised.
        assertTrue(page.tapBack(), "The top back arrow should be tappable on MANAGE NOTIFICATION");
        assertTrue(page.isLoaded(WAIT), "Back should return to APP PREFERENCES");
        screenshot("prefs-back-from-manage-notification");
    }

    @Test
    @Order(11)
    @DisplayName("TC11: Turning Location off hides the Start and End time fields")
    void locationOffHidesTiming() {
        AppPreferencesPage page = prefs();

        assumeTrue(Boolean.TRUE.equals(page.locationEnabled()),
                "Location is already off — nothing to turn off");
        assertTrue(page.timingFieldsShown(),
                "Start and End time should be visible while Location is on");
        System.out.println(">>> before: start=" + page.startTime() + " end=" + page.endTime());

        assertTrue(page.tapLocationToggle(), "The Location toggle should be tappable");
        assertTrue(page.waitForLocation(false, WAIT), "The Location toggle should settle OFF");
        assertTrue(page.waitForTimingFields(false, WAIT),
                "Start and End time should disappear once Location is off");
        screenshot("prefs-location-off");
    }

    @Test
    @Order(12)
    @DisplayName("TC12: Turning Location back on restores the time fields")
    void locationOnRestoresTiming() {
        AppPreferencesPage page = prefs();
        assumeTrue(Boolean.FALSE.equals(page.locationEnabled()), "Location is not off");

        assertTrue(page.tapLocationToggle(), "The Location toggle should be tappable");
        assertTrue(page.waitForLocation(true, WAIT), "The Location toggle should settle ON");
        assertTrue(page.waitForTimingFields(true, WAIT),
                "Start and End time should come back once Location is on");
        screenshot("prefs-location-on");
    }

    @Test
    @Order(13)
    @DisplayName("TC13: Start Time — Cancel changes nothing, then a new time saves")
    void changeStartTime() {
        AppPreferencesPage page = prefs();
        assumeTrue(page.timingFieldsShown(), "The time fields are not shown");

        String before = page.startTime();
        System.out.println(">>> start time before: " + before);

        // Cancel first: the picker must close without altering the field.
        assertTrue(page.openStartTimePicker(), "Start Time should be tappable");
        assertTrue(page.isTimePickerOpen(WAIT), "The clock picker should open");
        assertTrue(page.tapTimePickerCancel(), "Cancel should be tappable");
        assertEquals(before, page.startTime(), "Cancelling should leave the start time unchanged");
        screenshot("prefs-start-time-cancelled");

        assertTrue(page.openStartTimePicker(), "Start Time should be tappable again");
        assertTrue(page.isTimePickerOpen(WAIT), "The clock picker should open again");
        pickTime(page, VALID_START_H, VALID_START_M, true);
        assertEquals(VALID_START, page.startTime(),
                "The start time should read exactly what was picked");

        // Nothing on this card persists until Update Location is pressed.
        assertTrue(page.waitForSnackbarToClear(WAIT), "Previous snackbar should clear first");
        assertTrue(page.tapUpdateLocation(), "Update Location should be tappable");
        assertTrue(page.snackbarShown(AppPreferencesPage.PROFILE_UPDATED, LOAD_WAIT),
                "Expected the save to report success");
        System.out.println(">>> start time saved as: " + page.startTime());
        screenshot("prefs-start-time-saved");
    }

    @Test
    @Order(14)
    @DisplayName("TC14: End Time — Cancel changes nothing, then a new time saves")
    void changeEndTime() {
        AppPreferencesPage page = prefs();
        assumeTrue(page.timingFieldsShown(), "The time fields are not shown");

        String before = page.endTime();
        System.out.println(">>> end time before: " + before);

        assertTrue(page.openEndTimePicker(), "End Time should be tappable");
        assertTrue(page.isTimePickerOpen(WAIT), "The clock picker should open");
        assertTrue(page.tapTimePickerCancel(), "Cancel should be tappable");
        assertEquals(before, page.endTime(), "Cancelling should leave the end time unchanged");
        screenshot("prefs-end-time-cancelled");

        assertTrue(page.openEndTimePicker(), "End Time should be tappable again");
        assertTrue(page.isTimePickerOpen(WAIT), "The clock picker should open again");
        pickTime(page, VALID_END_H, VALID_END_M, false);
        assertEquals(VALID_END, page.endTime(),
                "The end time should read exactly what was picked");

        assertTrue(page.waitForSnackbarToClear(WAIT), "Previous snackbar should clear first");
        assertTrue(page.tapUpdateLocation(), "Update Location should be tappable");
        assertTrue(page.snackbarShown(AppPreferencesPage.PROFILE_UPDATED, LOAD_WAIT),
                "Expected the save to report success");
        System.out.println(">>> end time saved as: " + page.endTime());
        screenshot("prefs-end-time-saved");
    }

    @Test
    @Order(15)
    @DisplayName("TC15: An End Time before the Start Time is refused by the picker")
    void endBeforeStartRefusedByPicker() {
        AppPreferencesPage page = prefs();
        assumeTrue(page.timingFieldsShown(), "The time fields are not shown");

        String startBefore = page.startTime();
        String endBefore = page.endTime();
        System.out.println(">>> start=" + startBefore + " end=" + endBefore
                + " — attempting an end of " + EARLY_H + ":" + EARLY_M + " AM");

        assertTrue(page.waitForSnackbarToClear(WAIT), "Previous snackbar should clear first");
        assertTrue(page.openEndTimePicker(), "End Time should be tappable");
        assertTrue(page.isTimePickerOpen(WAIT), "The clock picker should open");
        assertTrue(page.tapDialNumber(EARLY_H), "Hour " + EARLY_H + " should be on the dial");
        assertTrue(page.tapDialNumber(EARLY_M), "Minute " + EARLY_M + " should be on the dial");
        assertTrue(page.tapAm(), "AM should be selectable");
        assertTrue(page.tapTimePickerOk(), "OK should be tappable");

        // Caught by the PICKER, and delivered as a Toast rather than a snackbar.
        assertTrue(page.toastShown(AppPreferencesPage.END_BEFORE_START, WAIT),
                "Expected the rejection toast: " + AppPreferencesPage.END_BEFORE_START);
        screenshot("prefs-end-before-start-refused");

        assertEquals(endBefore, page.endTime(), "A refused end time should leave the field unchanged");
        assertEquals(startBefore, page.startTime(), "The start time should be untouched");
    }

    @Test
    @Order(16)
    @DisplayName("TC16: A Start Time after the End Time passes the picker but is refused on save")
    void startAfterEndRefusedOnSave() {
        AppPreferencesPage page = prefs();
        assumeTrue(page.timingFieldsShown(), "The time fields are not shown");

        String endBefore = page.endTime();
        System.out.println(">>> end=" + endBefore + " — attempting a start of "
                + LATE_H + ":" + LATE_M + " PM");

        assertTrue(page.waitForSnackbarToClear(WAIT), "Previous snackbar should clear first");
        assertTrue(page.openStartTimePicker(), "Start Time should be tappable");
        assertTrue(page.isTimePickerOpen(WAIT), "The clock picker should open");
        assertTrue(page.tapDialNumber(LATE_H), "Hour " + LATE_H + " should be on the dial");
        assertTrue(page.tapDialNumber(LATE_M), "Minute " + LATE_M + " should be on the dial");
        assertTrue(page.tapPm(), "PM should be selectable");
        assertTrue(page.tapTimePickerOk(), "OK should be tappable");

        // The picker lets this through — the range is only checked on save, and
        // there the SAME message arrives as a snackbar instead of a toast.
        assertTrue(page.tapUpdateLocation(), "Update Location should be tappable");
        assertTrue(page.snackbarShown(AppPreferencesPage.END_BEFORE_START, WAIT),
                "Saving an inverted range should be refused: " + AppPreferencesPage.END_BEFORE_START);
        screenshot("prefs-start-after-end-refused");

        assertEquals(endBefore, page.endTime(), "The end time should be untouched");
    }

    /**
     * Drives the clock picker: hour, then minute, then AM/PM, then OK.
     *
     * <p>The dial swaps from hours to minutes by itself once an hour is tapped,
     * which is why the minute lookup happens after. Minute labels are unpadded on
     * the dial — "0", "30" — while the field renders them zero-padded.
     */
    private void pickTime(AppPreferencesPage page, String hour, String minute, boolean am) {
        assertTrue(page.tapDialNumber(hour),
                "Hour " + hour + " should be on the dial; got " + page.dialNumbers().keySet());
        assertEquals(hour, page.pickerHours(), "The picker header should show hour " + hour);

        assertTrue(page.tapDialNumber(minute),
                "Minute " + minute + " should be on the dial; got " + page.dialNumbers().keySet());
        assertTrue(am ? page.tapAm() : page.tapPm(), (am ? "AM" : "PM") + " should be selectable");
        assertTrue(page.tapTimePickerOk(), "OK should be tappable");
    }

    @Test
    @Order(17)
    @DisplayName("TC17: Selected Day unchecks Every Day and lists Monday to Sunday")
    void selectedDayShowsWeek() {
        AppPreferencesPage page = prefs();
        assumeTrue(Boolean.TRUE.equals(page.locationEnabled()), "Location is off");

        assertTrue(page.waitForSnackbarToClear(WAIT), "Previous snackbar should clear first");
        assertTrue(page.tapSelectedDay(), "Selected Day should be tappable");
        assertEquals(Boolean.TRUE, page.selectedDayChecked(), "Selected Day should be checked");
        assertEquals(Boolean.FALSE, page.everyDayChecked(),
                "Every Day should uncheck itself when Selected Day is chosen");

        assertTrue(page.isDayListShown(WAIT), "The weekday list should appear");
        List<String> days = page.dayNames();
        System.out.println(">>> days listed: " + days);
        screenshot("prefs-selected-day");
        assertEquals(WEEK, days, "The list should run Monday to Sunday");
    }

    @Test
    @Order(18)
    @DisplayName("TC18: Ticking a day enables its times, and Update Location saves")
    void tickDayEnablesTimes() {
        AppPreferencesPage page = prefs();
        assumeTrue(Boolean.TRUE.equals(page.selectedDayChecked()), "Not in Selected Day mode");

        assertTrue(page.scrollToDay(DAY), DAY + " should be reachable in the list");
        assumeTrue(Boolean.FALSE.equals(page.dayChecked(DAY)), DAY + " is already ticked");
        assertTrue(!page.dayTimesEnabled(DAY),
                "An unticked day should have its times disabled");

        assertTrue(page.tapDayCheckbox(DAY), DAY + " checkbox should be tappable");
        assertEquals(Boolean.TRUE, page.dayChecked(DAY), DAY + " should now be ticked");
        assertTrue(page.dayTimesEnabled(DAY),
                "Ticking " + DAY + " should enable its Start and End time");
        screenshot("prefs-day-ticked");

        assertTrue(page.waitForSnackbarToClear(WAIT), "Previous snackbar should clear first");
        assertTrue(page.tapUpdateLocation(), "Update Location should be tappable");
        assertTrue(page.snackbarShown(AppPreferencesPage.PROFILE_UPDATED, LOAD_WAIT),
                "Expected the save to report success");
    }

    @Test
    @Order(19)
    @DisplayName("TC19: That day's Start Time — Cancel changes nothing, then a new time saves")
    void changeDayStartTime() {
        AppPreferencesPage page = prefs();
        assumeTrue(Boolean.TRUE.equals(page.selectedDayChecked()), "Not in Selected Day mode");
        assertTrue(page.scrollToDay(DAY), DAY + " should be reachable");
        assumeTrue(Boolean.TRUE.equals(page.dayChecked(DAY)), DAY + " is not ticked");

        String before = page.dayStartTime(DAY);
        System.out.println(">>> " + DAY + " start before: " + before);

        assertTrue(page.openDayStartPicker(DAY), DAY + " Start Time should be tappable");
        assertTrue(page.isTimePickerOpen(WAIT), "The clock picker should open");
        assertTrue(page.tapTimePickerCancel(), "Cancel should be tappable");
        assertEquals(before, page.dayStartTime(DAY), "Cancelling should change nothing");

        assertTrue(page.openDayStartPicker(DAY), DAY + " Start Time should be tappable again");
        assertTrue(page.isTimePickerOpen(WAIT), "The clock picker should open again");
        pickTime(page, VALID_START_H, VALID_START_M, true);
        assertEquals(VALID_START, page.dayStartTime(DAY),
                DAY + " start time should read exactly what was picked");

        assertTrue(page.waitForSnackbarToClear(WAIT), "Previous snackbar should clear first");
        assertTrue(page.tapUpdateLocation(), "Update Location should be tappable");
        assertTrue(page.snackbarShown(AppPreferencesPage.PROFILE_UPDATED, LOAD_WAIT),
                "Expected the save to report success");
        screenshot("prefs-day-start-saved");
    }

    @Test
    @Order(20)
    @DisplayName("TC20: That day's End Time — Cancel changes nothing, then a new time saves")
    void changeDayEndTime() {
        AppPreferencesPage page = prefs();
        assumeTrue(Boolean.TRUE.equals(page.selectedDayChecked()), "Not in Selected Day mode");
        assertTrue(page.scrollToDay(DAY), DAY + " should be reachable");
        assumeTrue(Boolean.TRUE.equals(page.dayChecked(DAY)), DAY + " is not ticked");

        String before = page.dayEndTime(DAY);
        System.out.println(">>> " + DAY + " end before: " + before);

        assertTrue(page.openDayEndPicker(DAY), DAY + " End Time should be tappable");
        assertTrue(page.isTimePickerOpen(WAIT), "The clock picker should open");
        assertTrue(page.tapTimePickerCancel(), "Cancel should be tappable");
        assertEquals(before, page.dayEndTime(DAY), "Cancelling should change nothing");

        assertTrue(page.openDayEndPicker(DAY), DAY + " End Time should be tappable again");
        assertTrue(page.isTimePickerOpen(WAIT), "The clock picker should open again");
        pickTime(page, VALID_END_H, VALID_END_M, false);
        assertEquals(VALID_END, page.dayEndTime(DAY),
                DAY + " end time should read exactly what was picked");

        assertTrue(page.waitForSnackbarToClear(WAIT), "Previous snackbar should clear first");
        assertTrue(page.tapUpdateLocation(), "Update Location should be tappable");
        assertTrue(page.snackbarShown(AppPreferencesPage.PROFILE_UPDATED, LOAD_WAIT),
                "Expected the save to report success");
        screenshot("prefs-day-end-saved");
    }

    @Test
    @Order(21)
    @DisplayName("TC21: For that day, an End before the Start is refused by the picker")
    void dayEndBeforeStartRefused() {
        AppPreferencesPage page = prefs();
        assumeTrue(Boolean.TRUE.equals(page.selectedDayChecked()), "Not in Selected Day mode");
        assertTrue(page.scrollToDay(DAY), DAY + " should be reachable");
        assumeTrue(Boolean.TRUE.equals(page.dayChecked(DAY)), DAY + " is not ticked");

        String endBefore = page.dayEndTime(DAY);
        System.out.println(">>> " + DAY + " start=" + page.dayStartTime(DAY) + " end=" + endBefore);

        assertTrue(page.waitForSnackbarToClear(WAIT), "Previous snackbar should clear first");
        assertTrue(page.openDayEndPicker(DAY), DAY + " End Time should be tappable");
        assertTrue(page.isTimePickerOpen(WAIT), "The clock picker should open");
        assertTrue(page.tapDialNumber(EARLY_H), "Hour " + EARLY_H + " should be on the dial");
        assertTrue(page.tapDialNumber(EARLY_M), "Minute " + EARLY_M + " should be on the dial");
        assertTrue(page.tapAm(), "AM should be selectable");
        assertTrue(page.tapTimePickerOk(), "OK should be tappable");

        assertTrue(page.toastShown(AppPreferencesPage.END_BEFORE_START, WAIT),
                "Expected the rejection toast: " + AppPreferencesPage.END_BEFORE_START);
        assertEquals(endBefore, page.dayEndTime(DAY),
                "A refused end time should leave the field unchanged");
        screenshot("prefs-day-end-before-start-refused");
    }

    @Test
    @Order(22)
    @DisplayName("TC22: For that day, a Start after the End is refused on save")
    void dayStartAfterEndRefused() {
        AppPreferencesPage page = prefs();
        assumeTrue(Boolean.TRUE.equals(page.selectedDayChecked()), "Not in Selected Day mode");
        assertTrue(page.scrollToDay(DAY), DAY + " should be reachable");
        assumeTrue(Boolean.TRUE.equals(page.dayChecked(DAY)), DAY + " is not ticked");

        String endBefore = page.dayEndTime(DAY);
        System.out.println(">>> " + DAY + " end=" + endBefore + " — attempting a later start");

        assertTrue(page.waitForSnackbarToClear(WAIT), "Previous snackbar should clear first");
        assertTrue(page.openDayStartPicker(DAY), DAY + " Start Time should be tappable");
        assertTrue(page.isTimePickerOpen(WAIT), "The clock picker should open");
        assertTrue(page.tapDialNumber(LATE_H), "Hour " + LATE_H + " should be on the dial");
        assertTrue(page.tapDialNumber(LATE_M), "Minute " + LATE_M + " should be on the dial");
        assertTrue(page.tapPm(), "PM should be selectable");
        assertTrue(page.tapTimePickerOk(), "OK should be tappable");

        // The picker allows it; only the save checks the range.
        assertTrue(page.tapUpdateLocation(), "Update Location should be tappable");
        assertTrue(page.snackbarShown(AppPreferencesPage.END_BEFORE_START, WAIT),
                "Saving an inverted range should be refused: " + AppPreferencesPage.END_BEFORE_START);
        assertEquals(endBefore, page.dayEndTime(DAY), "The end time should be untouched");
        screenshot("prefs-day-start-after-end-refused");

        // Put a VALID range back and save it. Without this the suite would end on a
        // refused save, leaving 11:00 PM sitting in the start field — unsaved, and
        // misleading to anyone who opens the screen afterwards.
        assertTrue(page.waitForSnackbarToClear(WAIT), "The rejection snackbar should clear");
        assertTrue(page.scrollToDay(DAY), DAY + " should be reachable");
        assertTrue(page.openDayStartPicker(DAY), DAY + " Start Time should be tappable");
        assertTrue(page.isTimePickerOpen(WAIT), "The clock picker should open");
        pickTime(page, VALID_START_H, VALID_START_M, true);
        assertEquals(VALID_START, page.dayStartTime(DAY),
                DAY + " start time should be back to a valid value");

        assertTrue(page.tapUpdateLocation(), "Update Location should be tappable");
        assertTrue(page.snackbarShown(AppPreferencesPage.PROFILE_UPDATED, LOAD_WAIT),
                "Restoring a valid range should save");
        screenshot("prefs-day-restored");
    }

    @Test
    @Order(23)
    @DisplayName("TC23: Saving with no day ticked is refused, and the tick is not lost")
    void noDaySelectedIsRefused() {
        AppPreferencesPage page = prefs();
        assumeTrue(Boolean.TRUE.equals(page.selectedDayChecked()), "Not in Selected Day mode");
        assertTrue(page.scrollToDay(DAY), DAY + " should be reachable");
        assumeTrue(Boolean.TRUE.equals(page.dayChecked(DAY)), DAY + " is not ticked");

        assertTrue(page.waitForSnackbarToClear(WAIT), "Previous snackbar should clear first");
        assertTrue(page.tapDayCheckbox(DAY), DAY + " checkbox should be tappable");
        assertEquals(Boolean.FALSE, page.dayChecked(DAY), DAY + " should now be unticked");

        assertTrue(page.tapUpdateLocation(), "Update Location should be tappable");
        assertTrue(page.snackbarShown(AppPreferencesPage.NO_DAY_SELECTED, WAIT),
                "Expected: " + AppPreferencesPage.NO_DAY_SELECTED);
        screenshot("prefs-no-day-selected");
    }

    @Test
    @Order(24)
    @DisplayName("TC24: Reopening shows the day still ticked, because the untick never saved")
    void untickWasNotPersisted() {
        AppPreferencesPage page = prefs();

        assertTrue(page.waitForSnackbarToClear(WAIT), "The refusal snackbar should clear");
        assertTrue(page.tapBack(), "The top back arrow should be tappable");
        assertTrue(page.isOnSettingsTab(WAIT), "Back should land on the Settings tab");

        assertTrue(page.openAppPreferences(), "App Preferences should reopen");
        assertTrue(page.isLoaded(WAIT), "APP PREFERENCES should be open");
        assertEquals(Boolean.TRUE, page.selectedDayChecked(),
                "Selected Day mode should have persisted");

        assertTrue(page.scrollToDay(DAY), DAY + " should be reachable");
        Boolean still = page.dayChecked(DAY);
        System.out.println(">>> " + DAY + " after reopening: checked=" + still);
        screenshot("prefs-day-still-ticked");

        // The refused save must not have taken the tick with it.
        assertEquals(Boolean.TRUE, still,
                DAY + " should still be ticked — the untick was refused, so it was never saved");
    }

    @Test
    @Order(25)
    @DisplayName("TC25: The run finishes on the Settings tab")
    void finishOnSettingsTab() {
        AppPreferencesPage page = prefs();
        System.out.println(">>> final state: font=" + page.selectedFontSize()
                + " theme=" + page.selectedTheme()
                + " defaultUnit=" + page.defaultHomeUnit()
                + " location=" + page.locationEnabled());

        assertTrue(page.tapBack(), "The top back arrow should be tappable on APP PREFERENCES");
        assertTrue(page.isOnSettingsTab(WAIT), "The run should finish on the Settings tab");
        screenshot("prefs-finished");
    }

    /** The package the session is driving, so the restart targets the right app. */
    private String appPackageUnderTest() {
        String pkg = driver.getCurrentPackage();
        return (pkg == null || pkg.isBlank()) ? "com.zillit.zillitapp.qa" : pkg;
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
