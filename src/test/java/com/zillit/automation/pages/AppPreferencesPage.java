package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Settings → "App Preferences", plus the two places its settings show up: the
 * Home/Bulletin chat (font size) and the whole screen (theme).
 *
 * <p>Ids confirmed on the device: {@code fontSizeSelector} /
 * {@code selectedFontSizeTv}, {@code themeModeSelector} /
 * {@code selectedThemeModeTv}, and {@code cardDefaultUnit} /
 * {@code defaultHomeUnitText}.
 *
 * <p><b>Font size is measured, not read.</b> Nothing in the tree reports a text
 * size, so the effect is checked on a real chat message: the {@code tvMessage}
 * bounds shrink as the setting drops. Measured live on one message —
 * Large 85px, Medium 69px, Small 61px — so the assertion is "strictly smaller
 * than the previous setting" rather than any fixed number.
 *
 * <p><b>Dark mode is measured too.</b> The app themes itself internally, so the
 * system night-mode flag never changes and there is no attribute to read.
 * Average screen luminance is used instead: the same screen measured 187/255 in
 * Light and 35/255 in Dark, so the gap is wide enough to assert on safely.
 *
 * <p><b>The font radio ids skip a number</b> — Small is {@code radio1}, Medium
 * {@code radio2} and Large {@code radio4}. There is no {@code radio3}.
 */
public class AppPreferencesPage extends BasePage {

    public static final String FONT_SMALL = "Small";
    public static final String FONT_MEDIUM = "Medium";
    public static final String FONT_LARGE = "Large";

    public static final String THEME_SYSTEM = "System Default";
    public static final String THEME_LIGHT = "Light";
    public static final String THEME_DARK = "Dark";

    /** Radio ids for the font sizes — note the gap: there is no radio3. */
    private static final String RADIO_SMALL = "radio1";
    private static final String RADIO_MEDIUM = "radio2";
    private static final String RADIO_LARGE = "radio4";

    public AppPreferencesPage(AndroidDriver driver) {
        super(driver);
    }

    // ---------------- navigation ----------------

    public boolean openSettingsTab() {
        return tapBottomNav("Settings");
    }

    public boolean openHomeTab() {
        return tapBottomNav("Home");
    }

    private boolean tapBottomNav(String label) {
        hideKeyboard();
        sleep(500);
        try {
            WebElement navBar = driver.findElement(byId("bottomNavigation"));
            for (WebElement it : navBar.findElements(byId("menuText"))) {
                String t = it.getText();
                if (t != null && t.equalsIgnoreCase(label)) {
                    it.click();
                    sleep(2000);
                    return true;
                }
            }
        } catch (WebDriverException ignored) {
        }
        return false;
    }

    public boolean openAppPreferences() {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true))"
                            + ".scrollIntoView(new UiSelector().textContains(\"App Preferences\"))"));
        } catch (WebDriverException notScrollable) {
            // Short list or already visible.
        }
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath(
                            "//*[contains(@resource-id,'nameTv') and contains(@text,\"App Preferences\")]"
                                    + "/ancestor::*[contains(@resource-id,'commonListItemHolder')]")))
                    .click();
            sleep(1500);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Gets to APP PREFERENCES from wherever the app is, retrying each hop. */
    public boolean ensureOnAppPreferences(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (isLoaded(Duration.ofMillis(800))) {
                return true;
            }
            if (isOnSettingsTab(Duration.ofMillis(500))) {
                openAppPreferences();
                sleep(1500);
                continue;
            }
            if (hasBottomNav()) {
                openSettingsTab();
                sleep(1500);
                continue;
            }
            try {
                driver.navigate().back();
            } catch (WebDriverException ignored) {
            }
            sleep(900);
        }
        return false;
    }

    public boolean isOnSettingsTab(Duration timeout) {
        return onSettingsTabScreen(timeout);
    }

    private boolean hasBottomNav() {
        try {
            return !driver.findElements(byId("bottomNavigation")).isEmpty();
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean isLoaded(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            if ("APP PREFERENCES".equalsIgnoreCase(textOf("headerText"))) {
                return true;
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(250);
        }
    }

    public boolean tapBack() {
        return tapById("backBtn");
    }

    // ---------------- chat font size ----------------

    public String selectedFontSize() {
        return textOf("selectedFontSizeTv");
    }

    public boolean openFontSizeDialog() {
        return tapById("fontSizeSelector");
    }

    public boolean isFontDialogOpen(Duration timeout) {
        return presentWithin("fontRadioList", timeout);
    }

    /** Labels offered by the font dialog, in list order. */
    public List<String> fontOptions() {
        List<String> out = new ArrayList<>();
        for (String id : new String[] {RADIO_SMALL, RADIO_MEDIUM, RADIO_LARGE}) {
            try {
                for (WebElement e : driver.findElements(byId(id))) {
                    String t = safeText(e);
                    if (t != null && !t.isBlank()) {
                        out.add(t.trim());
                    }
                }
            } catch (WebDriverException ignored) {
            }
        }
        return out;
    }

    /** Picks a font size. The dialog closes itself once a radio is tapped. */
    public boolean chooseFontSize(String label) {
        String id = switch (label) {
            case FONT_SMALL -> RADIO_SMALL;
            case FONT_MEDIUM -> RADIO_MEDIUM;
            case FONT_LARGE -> RADIO_LARGE;
            default -> null;
        };
        if (id == null || !tapById(id)) {
            return false;
        }
        sleep(1500);
        return label.equalsIgnoreCase(selectedFontSize());
    }

    // ---------------- theme ----------------

    public String selectedTheme() {
        return textOf("selectedThemeModeTv");
    }

    public boolean openThemeDialog() {
        return tapById("themeModeSelector");
    }

    public boolean isThemeDialogOpen(Duration timeout) {
        return presentWithin("themeRadioGroup", timeout);
    }

    public List<String> themeOptions() {
        List<String> out = new ArrayList<>();
        for (String id : new String[] {"radioThemeSystem", "radioThemeLight", "radioThemeDark"}) {
            try {
                for (WebElement e : driver.findElements(byId(id))) {
                    String t = safeText(e);
                    if (t != null && !t.isBlank()) {
                        out.add(t.trim());
                    }
                }
            } catch (WebDriverException ignored) {
            }
        }
        return out;
    }

    public boolean chooseTheme(String label) {
        String id = switch (label) {
            case THEME_SYSTEM -> "radioThemeSystem";
            case THEME_LIGHT -> "radioThemeLight";
            case THEME_DARK -> "radioThemeDark";
            default -> null;
        };
        if (id == null || !tapById(id)) {
            return false;
        }
        sleep(2500);                     // the theme re-draws the whole activity
        return label.equalsIgnoreCase(selectedTheme());
    }

    /**
     * Average screen luminance, 0 (black) to 255 (white), or -1 if unreadable.
     *
     * <p>The only honest way to prove the theme actually applied: the app themes
     * itself, so the system night-mode flag never flips and no attribute reports
     * it. Measured live at 187 in Light and 35 in Dark.
     */
    public int screenBrightness() {
        try {
            byte[] png = driver.getScreenshotAs(OutputType.BYTES);
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(png));
            if (img == null) {
                return -1;
            }
            long total = 0;
            int samples = 0;
            // Sample a grid rather than every pixel — 40x40 is plenty and keeps
            // this well under a second.
            int stepX = Math.max(1, img.getWidth() / 40);
            int stepY = Math.max(1, img.getHeight() / 40);
            for (int y = 0; y < img.getHeight(); y += stepY) {
                for (int x = 0; x < img.getWidth(); x += stepX) {
                    int rgb = img.getRGB(x, y);
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;
                    total += (r * 299 + g * 587 + b * 114) / 1000;
                    samples++;
                }
            }
            return samples == 0 ? -1 : (int) (total / samples);
        } catch (Exception e) {
            return -1;
        }
    }

    // ---------------- default home unit ----------------

    /** Reads as "Calendar - Default Home Unit." — the unit name is the prefix. */
    public String defaultHomeUnitLabel() {
        return textOf("defaultHomeUnitText");
    }

    public String defaultHomeUnit() {
        String label = defaultHomeUnitLabel();
        int dash = label.indexOf(" - ");
        return dash > 0 ? label.substring(0, dash).trim() : label.trim();
    }

    public boolean openDefaultUnitPicker() {
        return tapById("cardDefaultUnit");
    }

    public boolean isOnUnitPicker(Duration timeout) {
        return presentWithin("homeUnitRecyclerView", timeout);
    }

    public List<String> unitOptions() {
        List<String> out = new ArrayList<>();
        try {
            for (WebElement e : driver.findElements(byId("labelTextView"))) {
                String t = safeText(e);
                if (t != null && !t.isBlank()) {
                    out.add(t.trim());
                }
            }
        } catch (WebDriverException ignored) {
        }
        return out;
    }

    public boolean searchUnit(String text) {
        try {
            WebElement box = new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(byId("search_widget_edit_textvw")));
            box.click();
            box.sendKeys(text);
            hideKeyboard();
            sleep(2000);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Picks by EXACT label — "Call Sheet" must not be matched by "Call". */
    public boolean chooseUnit(String name) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath(
                            "//*[contains(@resource-id,'labelTextView') and @text=\"" + name + "\"]")))
                    .click();
            sleep(3000);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    // ---------------- location + timing ----------------

    /** Location On/Off state, or null when the switch is not on screen. */
    public Boolean locationEnabled() {
        try {
            List<WebElement> found = driver.findElements(byId("locationOnOffSwitch"));
            return found.isEmpty() ? null : "true".equals(found.get(0).getAttribute("checked"));
        } catch (WebDriverException e) {
            return null;
        }
    }

    public boolean tapLocationToggle() {
        return tapById("locationOnOffSwitch");
    }

    public boolean waitForLocation(boolean wanted, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            Boolean s = locationEnabled();
            if (s != null && s == wanted) {
                return true;
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(400);
        }
    }

    /** True while the Start/End time row is on screen — it hides with location off. */
    public boolean timingFieldsShown() {
        try {
            return !driver.findElements(byId("tvEverydayStartTime")).isEmpty()
                    && !driver.findElements(byId("tvEverydayEndTime")).isEmpty();
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean waitForTimingFields(boolean shown, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            if (timingFieldsShown() == shown) {
                return true;
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(400);
        }
    }

    public String startTime() {
        return textOf("tvEverydayStartTime");
    }

    public String endTime() {
        return textOf("tvEverydayEndTime");
    }

    /** Refusal when Selected Day mode is saved with no day ticked. */
    public static final String NO_DAY_SELECTED = "Please select at least one day";

    /** Success message from "Update Location". */
    public static final String PROFILE_UPDATED = "Profile updated successfully.";

    /**
     * Taps "Update Location" — the ONLY thing that saves the location block.
     *
     * <p>Nothing on this card persists without it: changing the toggle or either
     * time only updates the screen, and leaving without pressing this discards
     * the lot. It sits below the fold, so it is scrolled into view first.
     *
     * <p>It is also the second validation point. Choosing an End before the Start
     * is refused by the picker itself, but pushing the START past the end slips
     * through the picker and is only caught here.
     */
    public boolean tapUpdateLocation() {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true)).scrollToEnd(6)"));
        } catch (WebDriverException notScrollable) {
            // Already fully visible.
        }
        sleep(800);
        return tapById("subTimeBtn");
    }

    public boolean openStartTimePicker() {
        return tapById("tvEverydayStartTime");
    }

    public boolean openEndTimePicker() {
        return tapById("tvEverydayEndTime");
    }

    // ---------------- Every Day / Selected Day ----------------

    public Boolean everyDayChecked() {
        return checkedOf("cbEveryDay");
    }

    public Boolean selectedDayChecked() {
        return checkedOf("cbSelectedDay");
    }

    public boolean tapEveryDay() {
        return tapById("cbEveryDay");
    }

    public boolean tapSelectedDay() {
        return tapById("cbSelectedDay");
    }

    /** True once the per-day list is on screen. */
    public boolean isDayListShown(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            try {
                if (!driver.findElements(byId("rvSelectedDays")).isEmpty()) {
                    return true;
                }
            } catch (WebDriverException ignored) {
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(250);
        }
    }

    /**
     * Every weekday in the list, scrolling to collect them.
     *
     * <p>Only four fit on screen at once, so a viewport read would report the
     * week as Monday–Thursday and a "Monday to Sunday" assertion would fail on a
     * list that is perfectly correct.
     */
    public List<String> dayNames() {
        java.util.LinkedHashSet<String> seen = new java.util.LinkedHashSet<>(visibleDayNames());
        for (int i = 0; i < 10 && seen.size() < 7; i++) {
            scrollForward();
            seen.addAll(visibleDayNames());
        }
        return new ArrayList<>(seen);
    }

    private List<String> visibleDayNames() {
        List<String> out = new ArrayList<>();
        try {
            for (WebElement e : driver.findElements(byId("dayNameTv"))) {
                String t = safeText(e);
                if (t != null && !t.isBlank()) {
                    out.add(t.trim());
                }
            }
        } catch (WebDriverException ignored) {
        }
        return out;
    }

    private void scrollForward() {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true)).scrollForward()"));
            sleep(600);
        } catch (WebDriverException ignored) {
        }
    }

    /** Brings a weekday row on screen. */
    public boolean scrollToDay(String day) {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true))"
                            + ".scrollIntoView(new UiSelector().textContains(\"" + day + "\"))"));
            sleep(600);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public Boolean dayChecked(String day) {
        WebElement c = dayRowControl(day, "dayCheckBox");
        if (c == null) {
            return null;
        }
        try {
            return "true".equals(c.getAttribute("checked"));
        } catch (WebDriverException e) {
            return null;
        }
    }

    public boolean tapDayCheckbox(String day) {
        WebElement c = dayRowControl(day, "dayCheckBox");
        if (c == null) {
            return false;
        }
        try {
            c.click();
            sleep(2000);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** True when that day's Start and End time fields are both enabled. */
    public boolean dayTimesEnabled(String day) {
        WebElement s = dayRowControl(day, "startTimeTv");
        WebElement e = dayRowControl(day, "endTimeTv");
        if (s == null || e == null) {
            return false;
        }
        try {
            return "true".equals(s.getAttribute("enabled")) && "true".equals(e.getAttribute("enabled"));
        } catch (WebDriverException ex) {
            return false;
        }
    }

    public String dayStartTime(String day) {
        WebElement e = dayRowControl(day, "startTimeTv");
        return e == null ? "" : String.valueOf(safeText(e)).trim();
    }

    public String dayEndTime(String day) {
        WebElement e = dayRowControl(day, "endTimeTv");
        return e == null ? "" : String.valueOf(safeText(e)).trim();
    }

    public boolean openDayStartPicker(String day) {
        return clickIfPresent(dayRowControl(day, "startTimeTv"));
    }

    public boolean openDayEndPicker(String day) {
        return clickIfPresent(dayRowControl(day, "endTimeTv"));
    }

    private boolean clickIfPresent(WebElement e) {
        if (e == null) {
            return false;
        }
        try {
            e.click();
            sleep(1500);
            return true;
        } catch (WebDriverException ex) {
            return false;
        }
    }

    /**
     * A control on one weekday's row, matched by vertical position.
     *
     * <p>Every row repeats the same ids ({@code dayCheckBox}, {@code startTimeTv},
     * {@code endTimeTv}), so the only way to reach a particular day's control is
     * through its label's position.
     */
    private WebElement dayRowControl(String day, String controlId) {
        try {
            WebElement label = null;
            for (WebElement n : driver.findElements(byId("dayNameTv"))) {
                String t = safeText(n);
                if (t != null && t.trim().equalsIgnoreCase(day)) {
                    label = n;
                    break;
                }
            }
            if (label == null) {
                return null;
            }
            Rectangle lr = label.getRect();
            int centre = lr.getY() + lr.getHeight() / 2;
            WebElement best = null;
            int bestDelta = Integer.MAX_VALUE;
            for (WebElement c : driver.findElements(byId(controlId))) {
                Rectangle cr = c.getRect();
                int delta = Math.abs((cr.getY() + cr.getHeight() / 2) - centre);
                if (delta < bestDelta && delta < 90) {
                    bestDelta = delta;
                    best = c;
                }
            }
            return best;
        } catch (WebDriverException e) {
            return null;
        }
    }

    private Boolean checkedOf(String id) {
        try {
            List<WebElement> f = driver.findElements(byId(id));
            return f.isEmpty() ? null : "true".equals(f.get(0).getAttribute("checked"));
        } catch (WebDriverException e) {
            return null;
        }
    }

    // ---------------- framework clock picker ----------------

    public boolean isTimePickerOpen(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            try {
                if (!driver.findElements(AppiumBy.id("android:id/timePicker")).isEmpty()) {
                    return true;
                }
            } catch (WebDriverException ignored) {
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(250);
        }
    }

    /** The hour currently shown in the picker header. */
    public String pickerHours() {
        return frameworkText("android:id/hours");
    }

    /** The minute currently shown in the picker header. */
    public String pickerMinutes() {
        return frameworkText("android:id/minutes");
    }

    public boolean tapTimePickerCancel() {
        return tapFramework("android:id/button2");
    }

    public boolean tapTimePickerOk() {
        return tapFramework("android:id/button1");
    }

    /**
     * Rejection shown when the end time is not after the start time.
     *
     * <p>The app really does say "Date" for a time-only field, and capitalises
     * "Should".
     */
    public static final String END_BEFORE_START =
            "End Date Should always be greater than start date.";

    /**
     * Waits for a TOAST. Read from the page source rather than by locator: a
     * Toast lives in its own window, which {@code UiSelector} does not search.
     */
    public boolean toastShown(String message, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            try {
                String src = driver.getPageSource();
                if (src != null && src.contains(message)) {
                    return true;
                }
            } catch (WebDriverException ignored) {
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
        }
    }

    public String currentSnackbar() {
        try {
            List<WebElement> found = driver.findElements(byId("snackbar_text"));
            return found.isEmpty() ? null : safeText(found.get(0));
        } catch (WebDriverException e) {
            return null;
        }
    }

    public boolean snackbarShown(String message, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            String s = currentSnackbar();
            if (s != null && s.contains(message)) {
                return true;
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(250);
        }
    }

    /** Load-bearing: a leftover message is read instead of the new one otherwise. */
    public boolean waitForSnackbarToClear(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            if (currentSnackbar() == null) {
                return true;
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(400);
        }
    }

    public boolean tapAm() {
        return tapFramework("android:id/am_label");
    }

    public boolean tapPm() {
        return tapFramework("android:id/pm_label");
    }

    /**
     * Numbers currently on the clock dial, mapped to their centre point.
     *
     * <p>They are virtual accessibility nodes on the RadialTimePickerView, exposed
     * through {@code content-desc} rather than text, and reported as NOT clickable
     * — so they are hit by coordinate. The dial shows hours 1–12 first and swaps
     * to minutes once an hour is chosen; minutes are "0", "5" … "55", not
     * zero-padded.
     */
    public java.util.Map<String, int[]> dialNumbers() {
        java.util.Map<String, int[]> out = new java.util.LinkedHashMap<>();
        try {
            for (WebElement e : driver.findElements(AppiumBy.xpath(
                    "//*[contains(@class,'RadialPickerTouchHelper') and string-length(@content-desc)>0]"))) {
                String desc = e.getAttribute("content-desc");
                Rectangle r = e.getRect();
                if (desc != null && !desc.isBlank()) {
                    out.put(desc.trim(),
                            new int[] {r.getX() + r.getWidth() / 2, r.getY() + r.getHeight() / 2});
                }
            }
        } catch (WebDriverException ignored) {
        }
        return out;
    }

    /** Taps a number on the dial. Minutes must be unpadded — "5", not "05". */
    public boolean tapDialNumber(String value) {
        int[] p = dialNumbers().get(value);
        if (p == null) {
            return false;
        }
        tapAt(p[0], p[1]);
        sleep(1500);
        return true;
    }

    private void tapAt(int x, int y) {
        var finger = new org.openqa.selenium.interactions.PointerInput(
                org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        var seq = new org.openqa.selenium.interactions.Sequence(finger, 1);
        seq.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, y));
        seq.addAction(finger.createPointerDown(0));
        seq.addAction(finger.createPointerMove(Duration.ofMillis(140),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, y));
        seq.addAction(finger.createPointerUp(0));
        driver.perform(java.util.Collections.singletonList(seq));
    }

    private String frameworkText(String id) {
        try {
            List<WebElement> f = driver.findElements(AppiumBy.id(id));
            return f.isEmpty() ? "" : String.valueOf(safeText(f.get(0))).trim();
        } catch (WebDriverException e) {
            return "";
        }
    }

    private boolean tapFramework(String id) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(AppiumBy.id(id)))
                    .click();
            sleep(1200);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    // ---------------- Manage Notification ----------------

    public boolean openManageNotification() {
        return tapById("manageNotification");
    }

    public boolean isOnManageNotification(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            if ("MANAGE NOTIFICATION".equalsIgnoreCase(textOf("headerText"))) {
                return true;
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(250);
        }
    }

    /** Module and section names currently listed, in display order. */
    public List<String> notificationModules() {
        List<String> out = new ArrayList<>();
        try {
            for (WebElement e : driver.findElements(byId("moduleName"))) {
                String t = safeText(e);
                if (t != null && !t.isBlank()) {
                    out.add(t.trim());
                }
            }
        } catch (WebDriverException ignored) {
        }
        return out;
    }

    public boolean searchNotification(String text) {
        try {
            WebElement box = new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(byId("search_widget_edit_textvw")));
            box.click();
            box.sendKeys(text);
            hideKeyboard();
            sleep(2000);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Empties the search box with DELETE — {@code clear()} hangs on this field. */
    public boolean clearNotificationSearch() {
        try {
            WebElement box = new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(byId("search_widget_edit_textvw")));
            box.click();
            sleep(400);
            String cur = safeText(box);
            int len = cur == null ? 0 : cur.length();
            driver.pressKey(new io.appium.java_client.android.nativekey.KeyEvent(
                    io.appium.java_client.android.nativekey.AndroidKey.MOVE_END));
            for (int i = 0; i < len + 6; i++) {
                driver.pressKey(new io.appium.java_client.android.nativekey.KeyEvent(
                        io.appium.java_client.android.nativekey.AndroidKey.DEL));
            }
            hideKeyboard();
            sleep(1800);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Toggle state for a module, or null when the row is not on screen. */
    public Boolean notificationEnabled(String module) {
        WebElement t = rowControl(module, "checkOnOff");
        if (t == null) {
            return null;
        }
        try {
            return "true".equals(t.getAttribute("checked"));
        } catch (WebDriverException e) {
            return null;
        }
    }

    public boolean tapNotificationToggle(String module) {
        WebElement t = rowControl(module, "checkOnOff");
        if (t == null) {
            return false;
        }
        try {
            t.click();
            sleep(2000);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean waitForNotification(String module, boolean wanted, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            Boolean s = notificationEnabled(module);
            if (s != null && s == wanted) {
                return true;
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(400);
        }
    }

    /** Collapses or expands a section such as Tools. */
    public boolean tapSectionArrow(String section) {
        WebElement a = rowControl(section, "ivArrow");
        if (a == null) {
            a = rowControl(section, "ivActivityArrow");
        }
        if (a == null) {
            return false;
        }
        try {
            a.click();
            sleep(2000);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /**
     * A control on the named module's row, matched by vertical position.
     *
     * <p>Toggles and arrows carry no text, so geometry is the only way to tie one
     * to its label. The window is deliberately tight — rows are ~130px apart, and
     * a section sits directly above its children, so a loose match would return
     * the parent's toggle instead of the child's.
     */
    private WebElement rowControl(String module, String controlId) {
        try {
            WebElement label = null;
            for (WebElement n : driver.findElements(byId("moduleName"))) {
                String t = safeText(n);
                if (t != null && t.trim().equalsIgnoreCase(module)) {
                    label = n;
                    break;
                }
            }
            if (label == null) {
                return null;
            }
            Rectangle lr = label.getRect();
            int centre = lr.getY() + lr.getHeight() / 2;
            WebElement best = null;
            int bestDelta = Integer.MAX_VALUE;
            for (WebElement c : driver.findElements(byId(controlId))) {
                Rectangle cr = c.getRect();
                int delta = Math.abs((cr.getY() + cr.getHeight() / 2) - centre);
                if (delta < bestDelta && delta < 80) {
                    bestDelta = delta;
                    best = c;
                }
            }
            return best;
        } catch (WebDriverException e) {
            return null;
        }
    }

    // ---------------- Home / Bulletin chat ----------------

    /**
     * Project name from the header, without the role suffix the project LIST adds.
     *
     * <p>Two different ids carry it: the bottom-nav screens use
     * {@code tvProjectName}, while pushed screens such as APP PREFERENCES use
     * {@code projectName}. Reading only the first returns "" on App Preferences,
     * which is exactly where this is called.
     */
    public String projectNameOnTop() {
        for (String id : new String[] {"tvProjectName", "projectName"}) {
            String t = textOf(id);
            if (t != null && !t.isBlank()) {
                return t.trim();
            }
        }
        return "";
    }

    /**
     * The Home sub-tab currently selected (Bulletin, Calendar, Call Sheet).
     *
     * <p>Scoped to the sub-tab strip on purpose. The bottom navigation reuses
     * {@code menuText} AND also reports {@code selected=true} on "Home", so an
     * unscoped scan returns whichever happens to come first in the tree — it
     * answered correctly only by accident.
     */
    public String selectedHomeUnitTab() {
        for (String container : new String[] {"recTab", "layTabs"}) {
            try {
                for (WebElement strip : driver.findElements(byId(container))) {
                    for (WebElement e : strip.findElements(byId("menuText"))) {
                        if ("true".equals(e.getAttribute("selected"))) {
                            String t = safeText(e);
                            if (t != null && !t.isBlank()) {
                                return t.trim();
                            }
                        }
                    }
                }
            } catch (WebDriverException ignored) {
            }
        }
        return "";
    }

    /**
     * Selects a Home sub-tab (Bulletin, Calendar, Call Sheet).
     *
     * <p>Scoped to the sub-tab strip, because the bottom navigation reuses
     * {@code menuText} — an unscoped lookup for "Home" would hit the nav item.
     * Needed before posting a chat message: once the default home unit is
     * changed, Home no longer opens on Bulletin.
     */
    public boolean openHomeUnitTab(String unit) {
        for (String container : new String[] {"recTab", "layTabs"}) {
            try {
                for (WebElement strip : driver.findElements(byId(container))) {
                    for (WebElement e : strip.findElements(byId("menuText"))) {
                        String t = safeText(e);
                        if (t != null && t.trim().equalsIgnoreCase(unit)) {
                            e.click();
                            sleep(2500);
                            return true;
                        }
                    }
                }
            } catch (WebDriverException ignored) {
            }
        }
        return false;
    }

    /** True when a Home sub-tab with this name is showing as selected. */
    public boolean homeUnitTabSelected(String unit, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            if (unit.equalsIgnoreCase(selectedHomeUnitTab())) {
                return true;
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(500);
        }
    }

    public boolean typeMessage(String text) {
        try {
            WebElement box = new WebDriverWait(driver, Duration.ofSeconds(15))
                    .until(ExpectedConditions.elementToBeClickable(byId("etMessage")));
            box.click();
            sleep(500);
            box.sendKeys(text);
            sleep(700);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean sendMessage() {
        for (String id : new String[] {"ivSendMessage", "mainSendVw", "emptySendView"}) {
            try {
                if (!driver.findElements(byId(id)).isEmpty()) {
                    driver.findElement(byId(id)).click();
                    sleep(2500);
                    hideKeyboard();
                    sleep(1000);
                    return true;
                }
            } catch (WebDriverException ignored) {
            }
        }
        return false;
    }

    /**
     * Height in pixels of the chat bubble carrying this text, or -1 if absent.
     *
     * <p>This is the font-size probe: the same message rendered at a bigger font
     * occupies a taller {@code tvMessage}.
     */
    public int messageHeight(String contains) {
        try {
            for (WebElement e : driver.findElements(byId("tvMessage"))) {
                String t = safeText(e);
                if (t != null && t.contains(contains)) {
                    Rectangle r = e.getRect();
                    return r.getHeight();
                }
            }
        } catch (WebDriverException ignored) {
        }
        return -1;
    }

    public boolean messageShown(String contains, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            if (messageHeight(contains) > 0) {
                return true;
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(500);
        }
    }

    // ---------------- helpers ----------------

    private boolean presentWithin(String id, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            try {
                if (!driver.findElements(byId(id)).isEmpty()) {
                    return true;
                }
            } catch (WebDriverException ignored) {
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(250);
        }
    }

    private boolean tapById(String id) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(byId(id)))
                    .click();
            sleep(900);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    private String textOf(String id) {
        try {
            List<WebElement> found = driver.findElements(byId(id));
            return found.isEmpty() ? "" : String.valueOf(safeText(found.get(0))).trim();
        } catch (WebDriverException e) {
            return "";
        }
    }

    private String safeText(WebElement e) {
        try {
            return e.getText();
        } catch (WebDriverException stale) {
            return "";
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
