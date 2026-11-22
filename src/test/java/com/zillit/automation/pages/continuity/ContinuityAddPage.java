package com.zillit.automation.pages.continuity;

import com.zillit.automation.pages.common.MediaPreviewPage;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Adding and editing a Continuity record: the "Choose below" picker, the app's own
 * gallery (Fallery → {@code GalleryItemView}), and the {@code dialog_add_scene_continuity}
 * bottom sheet that is both "Add Scene Details" and "Edit Details".
 *
 * <p><b>Every field in the sheet is the same include</b> ({@code edit_text_widget_no_margins}),
 * so {@code editTextInput} appears once per field, each with {@code content-desc="Zillit"}.
 * A field is therefore always reached DOWNWARD from its include id — {@code scene_no},
 * {@code episodeContinuity}, {@code scene_notes}. Upward lookups are useless in Appium.
 * {@code actor_name}, {@code episode} and {@code gender} are GONE in every build read, so
 * they are absent from the tree, which is what the shape case asserts.
 *
 * <p><b>The sheet is {@code setCancelable(false)}.</b> BACK does not close it; only the X
 * ({@code btn_cancel}) does. Nothing in this class presses BACK while the sheet is up.
 *
 * <p><b>Validation is {@code EditText.setError}</b> — a popup bubble, not a snackbar, and not
 * reliably in the tree. A refusal is therefore asserted as "the sheet is still open".
 */
public class ContinuityAddPage extends ContinuityPage {

    public static final String PICKER_TITLE = "Choose below";
    public static final List<String> PICKER_OPTIONS =
            List.of("Photo", "Gallery", "Video", "Video Gallery", "Document");
    public static final String ADD_TITLE = "Add Scene Details";
    public static final String EDIT_TITLE = "Edit Details";
    public static final String MORE_TITLE = "Add More Details";

    /** Include ids of the sheet's fields. */
    public static final String SCENE_NO = "scene_no";
    public static final String EPISODE = "episodeContinuity";
    public static final String NOTES = "scene_notes";
    public static final String ACTOR = "actor_name";
    public static final String DEAD_EPISODE = "episode";
    public static final String GENDER = "gender";

    public ContinuityAddPage(AndroidDriver driver) {
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

    public String pickerTitle() {
        // dialog_picker's own title. The sheet's header id is also tvTitle, but the two are
        // never up together.
        return firstText("tvTitle");
    }

    /** Picker options in screen order (row by row, left to right). */
    public List<String> pickerOptions() {
        List<WebElement> labels = new ArrayList<>(onScreen("tvLabel"));
        labels.sort((a, b) -> {
            int ya = a.getRect().getY(), yb = b.getRect().getY();
            if (Math.abs(ya - yb) > 20) return Integer.compare(ya, yb);
            return Integer.compare(centreX(a), centreX(b));
        });
        List<String> out = new ArrayList<>();
        for (WebElement l : labels) out.add(text(l));
        return out;
    }

    public boolean choosePickerOption(String label) {
        for (WebElement l : onScreen("tvLabel")) {
            if (label.equals(text(l))) {
                try { l.click(); pause(1500); return true; }
                catch (WebDriverException e) { return false; }
            }
        }
        return false;
    }

    /**
     * Gallery → pick {@code count} photos → the gallery's send FAB → {@code GalleryItemView}'s
     * {@code sendBtn}. Ends with the Add sheet up, or returns false with the app back in front.
     *
     * <p>Continuity opens the preview with {@code showDescriptionField = false}, so the
     * caption box {@link MediaPreviewPage#isPreviewOpen()} waits for is not there — this
     * waits for {@code sendBtn} alone.
     */
    public boolean pickPhotos(int count, Runnable dismissPermissions) {
        if (!choosePickerOption("Gallery")) return false;
        if (dismissPermissions != null) dismissPermissions.run();
        MediaPreviewPage media = new MediaPreviewPage(driver);
        if (!media.isGalleryOpen()) { returnToAppUnderTest(); return false; }
        media.openAllMediaAlbumIfListed();
        if (media.galleryThumbCount() == 0) { driver.navigate().back(); return false; }
        if (media.selectPhotos(count) < count) { driver.navigate().back(); return false; }
        if (!media.isSendFabVisible()) { driver.navigate().back(); return false; }
        media.tapGallerySendFab();
        if (!isVisible("sendBtn", Duration.ofSeconds(30))) return false;
        try { driver.findElement(byId("sendBtn")).click(); }
        catch (WebDriverException e) { return false; }
        return sheetShown(Duration.ofSeconds(30));
    }

    // ─────────────────────────── the sheet ───────────────────────────

    public boolean sheetShown(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (!onScreen(SCENE_NO).isEmpty() && !onScreen("submit_btn").isEmpty()) return true;
            pause(300);
        }
        return false;
    }

    public boolean sheetShown() { return sheetShown(Duration.ofSeconds(2)); }

    /**
     * The sheet's own header — the FIRST {@code tvTitle} top to bottom. There is a second
     * one, the "Add More Details" card's title inside {@code addMoreField}.
     */
    public String sheetTitle() {
        List<WebElement> t = onScreen("tvTitle");
        return t.isEmpty() ? null : text(t.get(0));
    }

    public boolean fieldShown(String includeId) {
        if (onScreen(includeId).isEmpty()) return false;
        return input(includeId) != null;
    }

    private WebElement input(String includeId) {
        scrollToId(includeId);
        List<WebElement> roots = onScreen(includeId);
        if (roots.isEmpty()) return null;
        try {
            List<WebElement> in = roots.get(0).findElements(byId("editTextInput"));
            return in.isEmpty() ? null : in.get(0);
        } catch (WebDriverException e) { return null; }
    }

    /** Types into one field. Never presses BACK to settle the keyboard (see class note). */
    public boolean setField(String includeId, String value) {
        WebElement in = input(includeId);
        if (in == null) return false;
        try {
            in.click();
            in.clear();
            if (!value.isEmpty()) in.sendKeys(value);
            pause(400);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    /** The field's text — or "" when it is empty (an empty EditText may report its hint). */
    public String fieldText(String includeId) {
        WebElement in = input(includeId);
        if (in == null) return null;
        String t = text(in);
        try {
            String hint = in.getAttribute("hint");
            if (hint != null && t.equals(hint.trim())) return "";
        } catch (WebDriverException ignored) { }
        return t;
    }

    public boolean submit() {
        scrollToId("submit_btn");
        boolean ok = tapFirst("submit_btn");
        pause(1500);
        return ok;
    }

    /** The X. The only way out of the sheet. */
    public boolean close() {
        boolean ok = tapFirst("btn_cancel");
        pause(1000);
        return ok;
    }

    // ─────────────────── "Add More Details" ───────────────────

    public boolean openMoreDetails() {
        scrollToId("addMoreField");
        boolean ok = tapFirst("addMoreField");
        if (!ok) ok = tapFirst("moreFileHolder");
        pause(1000);
        return ok && moreDialogShown();
    }

    /** The small dialog: {@code editText1} Title, {@code editText2} Description. */
    public boolean moreDialogShown() {
        return !onScreen("editText1").isEmpty() && !onScreen("rightButton").isEmpty();
    }

    public boolean fillMoreDialog(String title, String description) {
        return setInside("editText1", title) && setInside("editText2", description);
    }

    private boolean setInside(String wrapperId, String value) {
        List<WebElement> w = onScreen(wrapperId);
        if (w.isEmpty()) return false;
        try {
            List<WebElement> in = w.get(0).findElements(byId("editTextInput"));
            if (in.isEmpty()) return false;
            in.get(0).click();
            in.get(0).clear();
            if (!value.isEmpty()) in.get(0).sendKeys(value);
            pause(300);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    public boolean tapMoreAdd()    { return tapFirst("rightButton"); }
    public boolean tapMoreCancel() { return tapFirst("leftButton"); }

    /** The extra-detail rows in the sheet, as "label: value". */
    public List<String> moreRows() {
        scrollToId("recMoreField");
        List<WebElement> titles = onScreen("titleTv");
        List<WebElement> values = onScreen("descriptionTv");
        List<String> out = new ArrayList<>();
        for (int i = 0; i < titles.size(); i++) {
            String v = i < values.size() ? text(values.get(i)) : "";
            out.add(text(titles.get(i)) + ": " + v);
        }
        return out;
    }

    /** Removes an extra-detail row by its label. The app does it with no confirm. */
    public boolean deleteMoreRow(String label) {
        List<Band> bands = bandsOf("titleTv");
        List<WebElement> titles = elementsById("titleTv");
        for (int i = 0; i < titles.size() && i < bands.size(); i++) {
            if (label.equals(text(titles.get(i)))) {
                WebElement del = elementIn(bands.get(i), "imgDelete");
                if (del == null) return false;
                try { del.click(); pause(800); return true; }
                catch (WebDriverException e) { return false; }
            }
        }
        return false;
    }
}
