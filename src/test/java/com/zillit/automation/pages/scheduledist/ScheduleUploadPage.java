package com.zillit.automation.pages.scheduledist;

import com.zillit.automation.pages.common.VisiblePage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Uploading to Schedule Distribution: the "Choose below" picker (one item, "Document"), the
 * SYSTEM file picker (DocumentsUI — another app, driven through {@link #pickSystemFile}),
 * {@code GalleryItemView}'s confirm, and {@code UploadDocumentActivity} — a form shared with
 * Script Distribution and Forms.
 *
 * <p>The form's fields are includes that all carry the same inner ids
 * ({@code editTextWidget} / {@code editTextInput} / {@code autoCompleteDropDown}), so a field
 * is reached DOWNWARD from its include id: {@code etEpisodeNo}, {@code etChooser} (colour),
 * {@code etSceneName}, {@code etTypeChooser} (type), {@code etDocumentName} (page number).
 * The two dropdowns are AutoCompleteTextViews whose rows are {@code android:id/text1} —
 * the ANDROID package's id, not the app's.
 *
 * <p>Validation is {@code EditText.setError} for the text fields (not readable reliably —
 * assert the form stayed open) and a snackbar for the missing schedule type.
 */
public class ScheduleUploadPage extends VisiblePage {

    public static final String PICKER_TITLE = "Choose below";
    public static final String EPISODE = "etEpisodeNo";
    public static final String COLOUR = "etChooser";
    public static final String SCENE = "etSceneName";
    public static final String TYPE = "etTypeChooser";
    public static final String PAGE_NO = "etDocumentName";
    public static final String TYPE_FULL_PAGES = "Full Schedule Pages";
    public static final String TYPE_ONE_LINE_PAGES = "One Line Schedule Pages";

    public ScheduleUploadPage(AndroidDriver driver) {
        super(driver);
    }

    // ─────────────────────────── picker ───────────────────────────

    public boolean pickerShown(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (!onScreen("tvLabel").isEmpty()) return true;
            pause(300);
        }
        return false;
    }

    public String pickerTitle() { return firstText("tvTitle"); }

    public List<String> pickerOptions() {
        List<String> out = new ArrayList<>();
        for (WebElement l : onScreen("tvLabel")) out.add(text(l));
        return out;
    }

    /**
     * "Document" → system picker → the fixture → {@code GalleryItemView} → the form.
     * Returns false with the app back in front when any step fails.
     */
    public boolean pickPdf(String namePart, Runnable dismissPermissions) {
        if (!tapLabel("Document")) return false;
        if (dismissPermissions != null) dismissPermissions.run();
        if (!pickSystemFile(namePart, Duration.ofSeconds(40))) return false;
        // "Loading / please wait" while the file is prepared, then the preview.
        if (!isVisible("sendBtn", Duration.ofSeconds(40))) { returnToAppUnderTest(); return false; }
        try { driver.findElement(byId("sendBtn")).click(); }
        catch (WebDriverException e) { return false; }
        return formShown(Duration.ofSeconds(40));
    }

    private boolean tapLabel(String label) {
        for (WebElement l : onScreen("tvLabel")) {
            if (label.equals(text(l))) {
                try { l.click(); pause(2000); return true; }
                catch (WebDriverException e) { return false; }
            }
        }
        return false;
    }

    // ─────────────────────────── the form ───────────────────────────

    public boolean formShown(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (!driver.findElements(byId("btnSubmit")).isEmpty()
                    && !driver.findElements(byId("dateSelector")).isEmpty()) return true;
            pause(400);
        }
        return false;
    }

    public boolean formShown() { return formShown(Duration.ofSeconds(2)); }

    public boolean fieldShown(String includeId) {
        scrollToId(includeId);
        return !onScreen(includeId).isEmpty();
    }

    private WebElement input(String includeId, String innerId) {
        scrollToId(includeId);
        List<WebElement> roots = onScreen(includeId);
        if (roots.isEmpty()) return null;
        try {
            List<WebElement> in = roots.get(0).findElements(byId(innerId));
            return in.isEmpty() ? null : in.get(0);
        } catch (WebDriverException e) { return null; }
    }

    public boolean setField(String includeId, String value) {
        WebElement in = input(includeId, "editTextInput");
        if (in == null) return false;
        try {
            in.click();
            in.clear();
            if (!value.isEmpty()) in.sendKeys(value);
            pause(400);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    public String fieldText(String includeId) {
        WebElement in = input(includeId, "editTextInput");
        if (in == null) in = input(includeId, "autoCompleteDropDown");
        if (in == null) return null;
        String t = text(in);
        try {
            String hint = in.getAttribute("hint");
            if (hint != null && t.equals(hint.trim())) return "";
        } catch (WebDriverException ignored) { }
        return t;
    }

    /** The hint a field shows (from its TextInputLayout or the input itself). */
    public String fieldHint(String includeId) {
        WebElement in = input(includeId, "editTextInput");
        if (in == null) in = input(includeId, "autoCompleteDropDown");
        if (in == null) return null;
        try {
            String h = in.getAttribute("hint");
            return h == null || h.isBlank() ? text(in) : h.trim();
        } catch (WebDriverException e) { return null; }
    }

    /**
     * Opens a dropdown and returns its rows ({@code android:id/text1}). Leaves it open.
     *
     * <p>These are plain {@code AutoCompleteTextView}s inside a Material exposed-dropdown
     * {@code TextInputLayout}, and Material opens the list from its own touch handling — on
     * the device the colour field (the first dropdown on the form) did not open on a single
     * tap of the text while the type field did. So: tap the text, then the end icon, then the
     * text again once it holds focus, reading the rows after each. If none works, the screen
     * source is saved for diagnosis rather than reported as "no options".
     */
    public List<String> openDropdown(String includeId) {
        // Seen on the device: the list OPENS on the first tap — but an AutoCompleteTextView's
        // popup is a NON-focusable window (the field keeps focus), and UiAutomator2 reads only
        // the focused window, so the rows were on screen and absent from the tree. (A Spinner's
        // popup takes focus, which is why Spinner rows are found without this.) Read all
        // windows while a dropdown is involved; chooseDropdown switches it back off.
        multiWindows(true);
        for (int attempt = 0; attempt < 3; attempt++) {
            WebElement target = attempt == 1 ? endIcon(includeId) : input(includeId, "autoCompleteDropDown");
            if (target == null) continue;
            try { target.click(); } catch (WebDriverException e) { continue; }
            List<String> rows = waitForRows(Duration.ofSeconds(3));
            if (!rows.isEmpty()) return allRowsScrolling(rows);
        }
        saveSource("dropdown_" + includeId);
        multiWindows(false);
        return List.of();
    }

    /**
     * The popup shows a window's worth of rows (nine of the eleven colours on the emulator).
     * Scroll it down until nothing new appears, then back to the top.
     */
    private List<String> allRowsScrolling(List<String> first) {
        java.util.LinkedHashSet<String> seen = new java.util.LinkedHashSet<>(first);
        int scrolls = 0;
        for (int i = 0; i < 6; i++) {
            if (!scrollPopup(true)) break;
            scrolls++;
            int before = seen.size();
            seen.addAll(waitForRows(Duration.ofSeconds(1)));
            if (seen.size() == before) break;
        }
        for (int i = 0; i < scrolls; i++) scrollPopup(false);
        return new ArrayList<>(seen);
    }

    /** Drags inside the dropdown's list. True when a list was found to drag. */
    private boolean scrollPopup(boolean down) {
        try {
            List<WebElement> rows = driver.findElements(By.id("android:id/text1"));
            if (rows.size() < 2) return false;
            org.openqa.selenium.Rectangle top = rows.get(0).getRect();
            org.openqa.selenium.Rectangle bottom = rows.get(rows.size() - 1).getRect();
            int x = top.getX() + top.getWidth() / 2;
            int yLow = bottom.getY() + bottom.getHeight() / 2;
            int yHigh = top.getY() + top.getHeight() / 2;
            org.openqa.selenium.interactions.PointerInput f = new org.openqa.selenium.interactions.PointerInput(
                    org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
            org.openqa.selenium.interactions.Sequence s = new org.openqa.selenium.interactions.Sequence(f, 1);
            int from = down ? yLow : yHigh, to = down ? yHigh : yLow;
            s.addAction(f.createPointerMove(Duration.ZERO, org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, from));
            s.addAction(f.createPointerDown(0));
            s.addAction(f.createPointerMove(Duration.ofMillis(500), org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, to));
            s.addAction(f.createPointerUp(0));
            driver.perform(List.of(s));
            pause(700);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    private void multiWindows(boolean on) {
        try { driver.setSetting("enableMultiWindows", on); }
        catch (WebDriverException ignored) { }
    }

    private WebElement endIcon(String includeId) {
        List<WebElement> roots = onScreen(includeId);
        if (roots.isEmpty()) return null;
        try {
            List<WebElement> icon = roots.get(0).findElements(byId("text_input_end_icon"));
            return icon.isEmpty() ? null : icon.get(0);
        } catch (WebDriverException e) { return null; }
    }

    private List<String> waitForRows(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            List<String> out = new ArrayList<>();
            try {
                for (WebElement r : driver.findElements(By.id("android:id/text1"))) out.add(text(r));
            } catch (WebDriverException ignored) { }
            if (!out.isEmpty()) return out;
            pause(300);
        }
        return List.of();
    }

    private void saveSource(String name) {
        try {
            java.nio.file.Path dir = java.nio.file.Paths.get("build", "screenshots");
            java.nio.file.Files.createDirectories(dir);
            java.nio.file.Files.writeString(dir.resolve("DIAG__" + name + ".xml"), driver.getPageSource());
            java.nio.file.Files.write(dir.resolve("DIAG__" + name + ".png"),
                    driver.getScreenshotAs(org.openqa.selenium.OutputType.BYTES));
            System.out.println(">>> dropdown " + name + " never opened — saved build/screenshots/DIAG__" + name + ".*");
        } catch (Exception ignored) { }
    }

    /** Picks an option and checks the field now shows it — a silent miss uploads without it. */
    public boolean chooseDropdown(String includeId, String option) {
        if (openDropdown(includeId).isEmpty()) return false;
        try {
            boolean tapped = false;
            for (int pass = 0; pass < 6 && !tapped; pass++) {
                for (WebElement r : driver.findElements(By.id("android:id/text1"))) {
                    if (option.equals(text(r))) { r.click(); pause(800); tapped = true; break; }
                }
                if (!tapped && !scrollPopup(true)) break;
            }
        } catch (WebDriverException ignored) {
        } finally {
            multiWindows(false);
        }
        String now = fieldText(includeId);
        return now != null && now.equals(option);
    }

    /** Closes an open dropdown popup without choosing (BACK closes a ListPopupWindow only). */
    public void closeDropdown() {
        try {
            if (!driver.findElements(By.id("android:id/text1")).isEmpty()) {
                driver.navigate().back();
                pause(600);
            }
        } catch (WebDriverException ignored) {
        } finally {
            multiWindows(false);
        }
    }

    public String dateText() {
        scrollToId("dateSelector");
        return firstText("tvSelectedDate");
    }

    /** Opens the system date picker and accepts its default (today). */
    public boolean pickTodayDate() {
        scrollToId("dateSelector");
        if (!tapFirst("dateSelector")) return false;
        pause(1200);
        try {
            List<WebElement> ok = driver.findElements(By.id("android:id/button1"));
            if (ok.isEmpty()) return false;
            ok.get(0).click();
            pause(800);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    public String submitLabel() {
        List<WebElement> b = driver.findElements(byId("btnSubmit"));
        if (b.isEmpty()) return null;
        try {
            List<WebElement> t = b.get(0).findElements(byId("tvButton"));
            return t.isEmpty() ? null : text(t.get(0));
        } catch (WebDriverException e) { return null; }
    }

    public boolean submit() {
        scrollToId("btnSubmit");
        try {
            List<WebElement> b = driver.findElements(byId("btnSubmit"));
            if (b.isEmpty()) return false;
            b.get(0).click();
        } catch (WebDriverException e) { return false; }
        pause(1500);
        return true;
    }

    /**
     * Waits for the form to finish. The upload runs in the foreground: the activity stays
     * until the POST returns, then finishes with a toast.
     */
    public boolean waitFormClosed(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (driver.findElements(byId("btnSubmit")).isEmpty()) return true;
            pause(1000);
        }
        return false;
    }

    /** Leaves the form without uploading (toolbar back). */
    public void leaveForm() {
        if (!tapFirst("backBtn")) {
            try { driver.navigate().back(); } catch (WebDriverException ignored) { }
        }
        pause(1200);
        // GalleryItemView may ask "Are you sure you want to exit?" if it is still underneath.
        try {
            List<WebElement> yes = driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().textMatches(\"(?i)yes\")"));
            if (!yes.isEmpty()) { yes.get(0).click(); pause(800); }
        } catch (WebDriverException ignored) { }
    }
}
