package com.zillit.automation.pages.map;

import com.zillit.automation.pages.common.BasePage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;

/**
 * The Map module's confirmation dialogs — boundary checks and deletes.
 *
 * <p>These are {@code MaterialAlertDialogBuilder} dialogs, so they carry the framework ids
 * {@code android:id/alertTitle}, {@code android:id/message} and {@code button1/2/3} rather
 * than any of the app's own. That matters for the three-option boundary dialog, where the
 * middle choice is the NEUTRAL button ({@code button3}), not the second positive one:
 *
 * <pre>
 *   button1  POSITIVE   "Create Another City"   "Delete"   "Yes"   "Move"
 *   button2  NEGATIVE   "Cancel"                "Cancel"   "No"    "Cancel"
 *   button3  NEUTRAL    "Pin in &lt;city&gt;"  —          —       —
 * </pre>
 *
 * <p>Matching on the label rather than the slot is therefore the safer habit, and is what
 * {@link #tap(String)} does; the slot accessors are there for the cases that need to prove
 * a particular button is absent.
 *
 * <p>The wording is asserted verbatim in several cases because it is the only thing that
 * tells a user WHICH boundary they crossed — "outside the studio zone" and "outside the
 * selected city and studio zone" lead to different recoveries.
 */
public class MapDialogPage extends BasePage {

    // Titles
    public static final String TITLE_OUTSIDE_ZONE = "Outside Studio Zone";
    public static final String TITLE_DELETE_CITY = "Delete City";
    public static final String TITLE_DELETE_LOCATION = "Delete Location";
    public static final String TITLE_DELETE_TYPE = "Delete Location Type";

    // Button labels
    public static final String BTN_YES = "Yes";
    public static final String BTN_NO = "No";
    public static final String BTN_CANCEL = "Cancel";
    public static final String BTN_DELETE = "Delete";
    public static final String BTN_MOVE = "Move";
    public static final String BTN_CREATE_ANOTHER_CITY = "Create Another City";

    public MapDialogPage(AndroidDriver driver) {
        super(driver);
    }

    public boolean shown(Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        do {
            if (!driver.findElements(By.id("android:id/alertTitle")).isEmpty()) return true;
            if (!driver.findElements(By.id("android:id/message")).isEmpty()) return true;
            pause(300);
        } while (System.currentTimeMillis() < end);
        return false;
    }

    /**
     * The dialog's title.
     *
     * <p>Falls back to scanning the dialog's own text when {@code android:id/alertTitle}
     * is absent: a {@code MaterialAlertDialogBuilder} under a themed overlay renders its
     * title through the Material resource id instead, so the framework id reads null on a
     * dialog that is plainly on screen — which looked like "no dialog appeared" when the
     * real problem was only that the title lived elsewhere.
     */
    public String title() {
        String t = textOf(By.id("android:id/alertTitle"));
        if (t != null && !t.isBlank()) return t;
        for (String known : List.of(TITLE_OUTSIDE_ZONE, TITLE_DELETE_CITY,
                TITLE_DELETE_LOCATION, TITLE_DELETE_TYPE,
                "Update Location Type", "Studio Zone Details", "Move Location", "Outside")) {
            try {
                if (!driver.findElements(AppiumBy.androidUIAutomator(
                        "new UiSelector().textStartsWith(\"" + known + "\")")).isEmpty()) {
                    return known;
                }
            } catch (Exception ignored) { }
        }
        return null;
    }

    public String message() {
        return textOf(By.id("android:id/message"));
    }

    public String positiveLabel() { return textOf(By.id("android:id/button1")); }
    public String negativeLabel() { return textOf(By.id("android:id/button2")); }
    public String neutralLabel()  { return textOf(By.id("android:id/button3")); }

    /** Labels of the buttons actually present, in slot order. */
    public List<String> buttonLabels() {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String slot : List.of("button1", "button2", "button3")) {
            String t = textOf(By.id("android:id/" + slot));
            if (t != null && !t.isBlank()) out.add(t.trim());
        }
        return out;
    }

    /**
     * Is a button offered whose label STARTS with {@code prefix}?
     *
     * <p>For "Pin in &lt;city&gt;", where the city name is baked into the label and there
     * is no fixed string to compare against.
     */
    public boolean hasButtonStartingWith(String prefix) {
        for (String l : buttonLabels()) {
            if (l != null && l.trim().startsWith(prefix)) return true;
        }
        return false;
    }

    public boolean hasButton(String label) {
        for (String l : buttonLabels()) {
            if (l.equalsIgnoreCase(label) || l.toLowerCase().startsWith(label.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Taps a button by its visible label.
     *
     * <p>Matched case-insensitively and by prefix, because two of these labels are built at
     * runtime from the city name — "Pin in Mumbai", "Outside Mumbai" — so an exact match
     * would need the test to know the city.
     */
    /**
     * Taps a button whose label only STARTS with {@code prefix}.
     *
     * <p>The out-of-city dialog offers "Pin in &lt;city&gt;" — the city name is part of
     * the label, so there is no fixed string to match. ({@code MapFragment} builds it
     * from the selected city.)
     */
    public boolean tapStartsWith(String prefix) {
        try {
            for (WebElement e : driver.findElements(
                    io.appium.java_client.AppiumBy.xpath("//*[@text]"))) {
                String t = e.getText();
                if (t != null && t.trim().startsWith(prefix)) {
                    e.click();
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    public boolean tap(String label) {
        for (String slot : List.of("button1", "button2", "button3")) {
            String t = textOf(By.id("android:id/" + slot));
            if (t == null) continue;
            if (t.equalsIgnoreCase(label) || t.toLowerCase().startsWith(label.toLowerCase())) {
                try {
                    driver.findElement(By.id("android:id/" + slot)).click();
                    pause(1200);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
        }
        // Fall back to a text match: some Material dialogs render buttons without the
        // framework ids when the theme overrides the button bar.
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().textStartsWith(\"" + label + "\")")).click();
            pause(1200);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Dismisses with BACK — which the app treats as Cancel via its dismiss listener. */
    public void dismiss() {
        try { driver.navigate().back(); } catch (Exception ignored) { }
        pause(900);
    }

    private String textOf(By by) {
        try {
            java.util.List<WebElement> els = driver.findElements(by);
            return els.isEmpty() ? null : els.get(0).getText();
        } catch (Exception e) {
            return null;
        }
    }

    private void pause(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }
}
