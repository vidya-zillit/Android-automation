package com.zillit.automation.pages.scheduledod;

import com.zillit.automation.pages.scheduledist.ScheduleUploadPage;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * The D.O.D. branch of {@code UploadDocumentActivity} ({@code fromActivity = DOD_PAGES}),
 * reached through the same "Choose below" → Document → system picker → preview path as
 * Schedule Distribution, so the picker and dropdown helpers are inherited.
 *
 * <p>The D.O.D. form, from {@code UploadDocumentActivity.kt:492}:
 * <ul>
 *   <li>{@code etAutoName} — an AutoCompleteTextView (inner {@code autoCompleteDropDown}),
 *       hint "Name", suggesting the project's existing folder names. Required: blank →
 *       {@code setError("Please Enter Name")}. The value is saved through
 *       {@code generateCamelText()}: lower-cased, then each space-separated word capitalised.</li>
 *   <li>{@code etChooser} — colour, "Select Color (Optional)", rows are the enum names
 *       (WHITE, BLUE, …).</li>
 *   <li>{@code etEpisodeNo} on a TV project only.</li>
 *   <li>NO {@code dateSelector} ({@code isVisible = false}), no scene, no page number.</li>
 * </ul>
 * The parent's {@link #formShown(Duration)} waits for the date row, which this form never has,
 * so it is overridden.
 */
public class ScheduleDodUploadPage extends ScheduleUploadPage {

    public static final String NAME = "etAutoName";

    public ScheduleDodUploadPage(AndroidDriver driver) {
        super(driver);
    }

    @Override
    public boolean formShown(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (!driver.findElements(byId("btnSubmit")).isEmpty()
                    && !driver.findElements(byId(NAME)).isEmpty()) return true;
            pause(400);
        }
        return false;
    }

    @Override
    public boolean formShown() { return formShown(Duration.ofSeconds(2)); }

    /** The form shows a date row ({@code dateSelector}) — the QA sheet's DOD-04 asks for one. */
    public boolean dateShown() {
        return !driver.findElements(byId("dateSelector")).isEmpty()
                || !driver.findElements(byId("tvSelectedDate")).isEmpty();
    }

    private WebElement nameInput() {
        scrollToId(NAME);
        List<WebElement> roots = onScreen(NAME);
        if (roots.isEmpty()) return null;
        try {
            List<WebElement> in = roots.get(0).findElements(byId("autoCompleteDropDown"));
            return in.isEmpty() ? null : in.get(0);
        } catch (WebDriverException e) { return null; }
    }

    /** Types the name and closes the keyboard (the suggestion popup goes with it). */
    public boolean setName(String value) {
        WebElement in = nameInput();
        if (in == null) return false;
        try {
            in.click();
            in.clear();
            if (!value.isEmpty()) in.sendKeys(value);
            pause(600);
        } catch (WebDriverException e) { return false; }
        hideKeyboardIfShown();
        pause(400);
        return true;
    }

    /**
     * Types {@code prefix} into the name and reads the suggestion popup. The popup is a
     * non-focusable window, so the tree is read across windows meanwhile (see
     * {@link ScheduleUploadPage#openDropdown}).
     */
    public List<String> nameSuggestions(String prefix) {
        WebElement in = nameInput();
        if (in == null) return List.of();
        List<String> out = new ArrayList<>();
        try {
            driver.setSetting("enableMultiWindows", true);
            in.click();
            in.clear();
            in.sendKeys(prefix);
            long deadline = System.currentTimeMillis() + 4000;
            while (System.currentTimeMillis() < deadline && out.isEmpty()) {
                for (WebElement r : driver.findElements(By.id("android:id/text1"))) {
                    String t = text(r);
                    if (!t.isEmpty()) out.add(t);
                }
                if (out.isEmpty()) pause(400);
            }
        } catch (WebDriverException ignored) {
        } finally {
            try { driver.setSetting("enableMultiWindows", false); } catch (WebDriverException ignored) { }
        }
        return out;
    }

    public String nameText() { return fieldText(NAME); }
    public String nameHint() { return fieldHint(NAME); }

    /** What {@code generateCamelText()} makes of a typed name — the folder title the app saves. */
    public static String savedName(String typed) {
        StringBuilder b = new StringBuilder();
        for (String w : typed.toLowerCase().split(" ")) {
            if (w.isEmpty()) continue;
            b.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(' ');
        }
        return b.toString().trim();
    }
}
