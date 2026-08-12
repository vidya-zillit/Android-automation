package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Pause;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DOCUMENT DISTRIBUTION — every WATERMARK surface, which is FOUR screens, not one.
 *
 * <p>Area M reads like a single feature but the app spreads it across four different
 * layouts, reached from three different places. Getting them confused is the fastest way
 * to write a case that passes against the wrong screen, so they are separated here by an
 * id that exists in exactly ONE of the four layouts:
 *
 * <table>
 *   <caption>The four watermark surfaces</caption>
 *   <tr><th>Surface</th><th>Layout</th><th>Reached from</th><th>Unique id</th></tr>
 *   <tr><td>Download sheet</td><td>bottomsheet_dd_watermark_download</td>
 *       <td>file row ⋮ → Watermark</td><td>{@code dd_supported} / {@code dd_unsupported}</td></tr>
 *   <tr><td>Batch screen</td><td>fragment_dd_watermark_batch</td>
 *       <td>selection bar → Watermark PDF and download</td><td>{@code dd_download_zip}</td></tr>
 *   <tr><td>Wizard sheet</td><td>bottomsheet_dd_watermark_wizard</td>
 *       <td>composer → {@code dd_watermark_bar}</td><td>{@code dd_preview_text} / {@code dd_l1_hint}</td></tr>
 *   <tr><td>Composer preview</td><td>bottomsheet_dd_watermark_preview</td>
 *       <td>composer attachment → eye button</td><td>{@code dd_preview_fallback} + {@code dd_close}</td></tr>
 * </table>
 *
 * <p><b>Why the obvious ids are all traps here.</b>
 * <ul>
 *   <li>{@code dd_files} is the batch screen's ChipGroup — and ALSO the publish sheet's
 *       RecyclerView. It cannot say "I am on the watermark batch screen".</li>
 *   <li>{@code dd_filename} and {@code dd_preview_image} are shared by the download sheet
 *       and the composer preview sheet — the two surfaces most likely to be confused.</li>
 *   <li>{@code dd_save} is shared with the contact editor, the template editor and the
 *       preset editor; {@code dd_cancel} with five more layouts; {@code dd_download} with
 *       FilePreviewActivity and the grid file row.</li>
 *   <li>{@code dd_appearance} is an {@code <include>} id — trap 3.16, it REPLACES the
 *       included layout's root id, so at runtime the appearance block is
 *       {@code dd_appearance} and never partial_dd_watermark_appearance's own root. The
 *       ids INSIDE it ({@code dd_wm_size_group}, {@code dd_wm_swatches},
 *       {@code dd_wm_opacity}) are unique to that partial and are safe to use — but they
 *       appear on three screens, so they identify the BLOCK, never the SCREEN.</li>
 * </ul>
 *
 * <p><b>Half of every watermark surface starts BELOW THE FOLD.</b> Three of the four
 * layouts wrap their body in a scroll container and put the shared appearance block near
 * the bottom of it:
 * <ul>
 *   <li>{@code fragment_dd_watermark_batch.xml} — a {@code NestedScrollView} holding
 *       files → lines → recipients → {@code dd_appearance} → {@code dd_info}. On a phone
 *       the viewport ends at the opacity slider, so {@code dd_info} is NOT in the
 *       accessibility tree until the body is scrolled. Only {@code dd_download_zip} lives
 *       outside the scroll view and is therefore always readable.</li>
 *   <li>{@code bottomsheet_dd_watermark_download.xml} — a 320dp preview card plus both
 *       stamp inputs sit above {@code dd_appearance}, so on open only the "APPEARANCE"
 *       header is visible and {@code dd_wm_size_*} is off-screen.</li>
 *   <li>{@code bottomsheet_dd_watermark_wizard.xml} — {@code dd_save} is the LAST child of
 *       the sheet's {@code ScrollView}, below {@code dd_appearance}.</li>
 * </ul>
 * A uiautomator dump only carries nodes that are visible on screen, so "id absent" on any
 * of these means "scroll first", never "the app does not have it". {@link #revealId} is
 * the reveal used everywhere below; it prefers {@code UiScrollable} and falls back to a
 * {@code mobile: swipeGesture} on the body, because a bottom sheet stacked over the
 * library gives {@code UiScrollable} more than one scrollable to choose from.
 *
 * <p><b>The batch screen writes to disk.</b> {@code dd_download_zip} saves a ZIP into
 * Downloads and then fires an {@code ACTION_VIEW} intent at it, which can hand the
 * foreground to a foreign package. {@link #inForeignPackage()} exists so the caller can
 * unwind that in a finally block instead of losing every later case to a ZIP viewer.
 */
public class DocDistWatermarkPage extends BasePage {

    /** The toast the batch screen raises on a successful ZIP build. */
    public static final String ZIP_SAVED_TOAST = "Watermarked ZIP saved to Downloads";

    /** Selection-bar labels — the REAL strings, not the spec's shorthand. */
    public static final String MENU_WATERMARK_DOWNLOAD = "Watermark PDF and download";
    public static final String MENU_WATERMARK_SHARE    = "Watermark PDF and share";
    /** File-row ⋮ label. */
    public static final String MENU_WATERMARK          = "Watermark";

    public DocDistWatermarkPage(AndroidDriver driver) {
        super(driver);
    }

    // ==================== identity — one per surface ====================

    /**
     * The single-doc download sheet (file row ⋮ → Watermark).
     *
     * <p>Pairs {@code dd_filename} (shared with the composer preview sheet) with
     * {@code dd_supported} / {@code dd_unsupported} (this layout only). Exactly one of the
     * latter pair is VISIBLE at a time — the other is {@code View.GONE} and therefore
     * absent from the accessibility tree, so "either one" is the correct test.
     */
    public boolean isDownloadSheetOpen(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (displayed("dd_filename") && (displayed("dd_supported") || displayed("dd_unsupported"))) {
                return true;
            }
            sleep(300);
        }
        return false;
    }

    /** The batch screen (selection bar → Watermark PDF and download). */
    public boolean isBatchOpen(Duration timeout) {
        return isVisible("dd_download_zip", timeout);
    }

    /** The composer's watermark wizard ({@code dd_watermark_bar} → sheet). */
    public boolean isWizardOpen(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (displayed("dd_preview_text") || displayed("dd_l1_hint")) return true;
            sleep(300);
        }
        return false;
    }

    /**
     * The composer's per-file watermark preview (eye button on a watermarked attachment).
     *
     * <p>{@code dd_close} alone matches FilePreviewActivity and the email-view sheet;
     * {@code dd_filename} alone matches the download sheet. Together they are unique.
     */
    public boolean isComposerPreviewOpen(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (displayed("dd_filename") && displayed("dd_close")) return true;
            sleep(300);
        }
        return false;
    }

    // ==================== batch screen ====================

    /**
     * The file chips ({@code dd_files} is a ChipGroup here, NOT a RecyclerView — the batch
     * fragment rebuilds it with {@code removeAllViews()} + {@code addView(Chip)}).
     */
    public List<String> fileChips() { return childTexts("dd_files"); }

    /** The recipient chips ({@code dd_recipients}, same ChipGroup shape). */
    public List<String> recipientChips() { return childTexts("dd_recipients"); }

    /**
     * "Will generate N watermarked files (D docs × R recipients) into a single ZIP."
     *
     * <p>{@code dd_info} is the LAST child of the batch screen's NestedScrollView
     * (fragment_dd_watermark_batch.xml, the MaterialCardView after the {@code dd_appearance}
     * include), so on a phone it is off-screen — and therefore absent from the dump —
     * until the body is scrolled. Reveal first, then read; otherwise this returns "" and
     * the caller concludes the app rendered nothing.
     */
    public String infoText() {
        revealInfo();
        return textOf("dd_info");
    }

    /** Scrolls the info card at the very bottom of the batch body into view. */
    public boolean revealInfo() { return revealId("dd_info"); }

    /**
     * Types an address and commits it with the Add button.
     *
     * <p>The fragment also commits on IME-done, but Add is the deterministic path — a
     * hidden keyboard makes the IME action unreachable.
     *
     * <p>Returning true means the tap landed, NOT that a recipient exists:
     * {@code WatermarkBatchViewModel.addRecipientEmail} silently drops anything
     * {@code EmailValidator} rejects (it gates the TLD against the bundled IANA list), so
     * callers must verify {@link #recipientChips()} afterwards.
     */
    public boolean addRecipientEmail(String email) {
        try {
            revealId("dd_email");
            WebElement f = driver.findElement(byId("dd_email"));
            f.click();
            f.clear();
            f.sendKeys(email);
            hideKeyboardIfShown();
            sleep(400);
        } catch (WebDriverException e) { return false; }
        return click("dd_add");
    }

    /**
     * Opens the "Choose…" preset picker.
     *
     * <p>{@code showPresetPicker()} returns EARLY and silently when the project has no
     * distribution lists — a tap that does nothing is the app behaving correctly, not a
     * failure. Callers must treat "no dialog" as an environment gap, not a bug.
     */
    public boolean tapChoose() { return click("dd_choose"); }

    /**
     * True when the preset picker actually opened.
     *
     * <p>{@code showPresetPicker()} builds a {@code MaterialAlertDialogBuilder.setItems}
     * list, which the framework renders into {@code android:id/select_dialog_listview} —
     * a platform id, so it is matched raw rather than through the app prefix.
     */
    public boolean presetPickerShown(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            try {
                if (!driver.findElements(AppiumBy.androidUIAutomator(
                        "new UiSelector().resourceId(\"android:id/select_dialog_listview\")")).isEmpty()) {
                    return true;
                }
            } catch (WebDriverException ignored) { }
            sleep(400);
        }
        return false;
    }

    public boolean isDownloadZipEnabled() { return enabled("dd_download_zip"); }

    public boolean tapDownloadZip() { return click("dd_download_zip"); }

    // ==================== line 1 / line 2 (batch AND wizard) ====================

    // Both layouts declare the same radio ids, which is why these helpers are shared —
    // and why they can never be used to tell the two screens apart.

    public boolean line1NameShown()   { return displayed("dd_l1_name"); }
    public boolean line1CustomShown() { return displayed("dd_l1_custom"); }
    public boolean line2NoneShown()   { return displayed("dd_l2_none"); }
    public boolean line2CustomShown() { return displayed("dd_l2_custom"); }

    public boolean chooseLine1Name()   { return click("dd_l1_name"); }
    public boolean chooseLine1Custom() { return click("dd_l1_custom"); }
    public boolean chooseLine2None()   { return click("dd_l2_none"); }
    public boolean chooseLine2Custom() { return click("dd_l2_custom"); }

    /** The custom inputs are {@code View.GONE} until their radio is picked. */
    public boolean line1CustomInputShown() { return isVisible("dd_l1_custom_input", Duration.ofSeconds(4)); }
    public boolean line2CustomInputShown() { return isVisible("dd_l2_custom_input", Duration.ofSeconds(4)); }

    public boolean typeLine1Custom(String text) { return typeIn("dd_l1_custom_input", text); }
    public boolean typeLine2Custom(String text) { return typeIn("dd_l2_custom_input", text); }

    public String line1CustomText() { return textOf("dd_l1_custom_input"); }
    public String line2CustomText() { return textOf("dd_l2_custom_input"); }

    // ==================== appearance block (three screens share it) ====================

    /**
     * The {@code <include>} id — trap 3.16, the partial's own root id never exists.
     *
     * <p>Beware: this alone proves almost nothing. {@code dd_appearance} is a full-width
     * LinearLayout ~350dp tall, so the moment its "APPEARANCE" heading scrolls into view
     * the node exists — while every control inside it ({@code dd_wm_size_*},
     * {@code dd_wm_swatches}, {@code dd_wm_opacity}) is still below the fold and therefore
     * still absent. Call {@link #revealAppearance()} before asserting on the controls.
     */
    public boolean hasAppearanceBlock() { return present("dd_appearance"); }

    /**
     * Scrolls the shared appearance partial's CONTROLS into view.
     *
     * <p>Anchored on {@code dd_wm_size_small} — the first control below the block's
     * heading and its 110dp live-preview frame — so a true return means the chips, not
     * just the section title, are on screen.
     */
    public boolean revealAppearance() { return revealId("dd_wm_size_small"); }

    /** Brings the opacity row (the LAST row of the partial) fully into view. */
    public boolean revealOpacity() { return revealId("dd_wm_opacity"); }

    public boolean sizeChipsShown() {
        return displayed("dd_wm_size_small") && displayed("dd_wm_size_medium")
                && displayed("dd_wm_size_large");
    }

    public boolean chooseSizeSmall()  { return click("dd_wm_size_small"); }
    public boolean chooseSizeLarge()  { return click("dd_wm_size_large"); }
    public boolean chooseSizeMedium() { return click("dd_wm_size_medium"); }

    public boolean sizeSelected(String chipId) {
        try { return "true".equalsIgnoreCase(driver.findElement(byId(chipId)).getAttribute("checked")); }
        catch (Exception e) { return false; }
    }

    /**
     * The preset colour swatches — there are EIGHT, not seven.
     *
     * <p>{@code WatermarkAppearance.PRESET_SWATCHES} lists
     * {@code #6b7280 #dc2626 #111827 #2563eb #16a34a #ea580c #7c3aed #0891b2} and
     * {@code WatermarkAppearanceBinder.buildSwatches()} adds one FrameLayout per entry, so
     * the runtime count is 8. (The live run confirmed {@code swatches=8}; the "seven" in
     * the older spec prose predates the {@code #0891b2} teal added with ZL-19547.)
     *
     * <p>Index 0 is {@code #6b7280}, the DEFAULT colour — tapping it is a documented
     * no-op, because {@code onSwatchSelected} returns early when the hex already matches
     * {@code current.color}. Any test that wants to prove a swatch applies must pick a
     * non-zero index. The binder builds them at runtime, so they carry no ids of their
     * own — index is all there is.
     */
    public int swatchCount() { return swatchViews().size(); }

    public boolean chooseSwatch(int index) {
        try {
            List<WebElement> swatches = swatchViews();
            if (index < 0 || index >= swatches.size()) return false;
            swatches.get(index).click();
            sleep(300);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    /** The swatch frames, revealing the colour row first when it is below the fold. */
    private List<WebElement> swatchViews() {
        String xpath = "//*[@resource-id='" + appPackage() + ":id/dd_wm_swatches']/*";
        try {
            List<WebElement> found = driver.findElements(AppiumBy.xpath(xpath));
            if (!found.isEmpty()) return found;
            revealId("dd_wm_swatches");
            return driver.findElements(AppiumBy.xpath(xpath));
        } catch (WebDriverException e) { return List.of(); }
    }

    /** "42%" — the live readout beside the slider. */
    public String opacityValue() { return textOf("dd_wm_opacity_value"); }

    /**
     * The readout as a number, or -1 when it is not on screen / not a percentage.
     *
     * <p>The binder formats it with {@code R.string.dd_watermark_opacity_percent}
     * ("%d%%") from {@code slider.value.toInt()}, and the layout pins the Slider to
     * {@code valueFrom="5"} / {@code valueTo="70"} — so a live readout is always an
     * integer in 5…70 followed by '%'.
     */
    public int opacityPercent() {
        String raw = opacityValue().trim();
        if (!raw.endsWith("%")) return -1;
        try { return Integer.parseInt(raw.substring(0, raw.length() - 1).trim()); }
        catch (NumberFormatException e) { return -1; }
    }

    /**
     * Scrubs the opacity Slider to a fraction of its track.
     *
     * <p><b>Why this is not a tap.</b> The Slider lives inside a NestedScrollView on every
     * surface that embeds the appearance partial, and Material 1.13's
     * {@code BaseSlider.onTouchEvent} treats that case specially: ACTION_DOWN
     * {@code break}s out untouched when {@code isPotentialVerticalScroll(event)} (lines
     * 3111-3119), and ACTION_MOVE {@code return false}s while the horizontal delta is
     * still under {@code scaledTouchSlop} (lines 3142-3149). Only a press that then moves
     * MORE than the touch slop starts a scrub. The first attempt at this case used
     * {@code mobile: clickGesture} plus a two-point {@code mobile: dragGesture} and the
     * readout never budged off 40% — hence the explicit W3C pointer sequence below:
     * press, hold, one hop that clears the slop outright, then finer steps, hold, release.
     *
     * <p>The press starts on the CURRENT thumb (derived from the readout, since the track
     * runs 5…70) rather than the track's midpoint, so the very first move is already a
     * real drag of the thumb.
     *
     * <p>The track is clamped to 5…70, so a fraction near either end lands on the clamp
     * rather than 0/100. Reports success only when the readout ACTUALLY changed, so the
     * caller never asserts against a gesture that silently missed.
     */
    public boolean setOpacityFraction(double fraction) {
        revealOpacity();
        String before = opacityValue();
        Rectangle r = sliderRect();
        if (r == null) return false;

        double f = Math.max(0.02, Math.min(0.98, fraction));
        int y = r.getY() + r.getHeight() / 2;
        int targetX = r.getX() + (int) (r.getWidth() * f);
        // Where the thumb sits right now, as a fraction of the 5…70 track.
        int pct = opacityPercent();
        double thumbF = pct < 0 ? 0.5 : Math.max(0.0, Math.min(1.0, (pct - 5) / 65.0));
        int thumbX = r.getX() + (int) (r.getWidth() * thumbF);

        if (scrub(thumbX, targetX, y) && !opacityValue().equals(before)) return true;

        // Fallback 1 — a plain drag, in case the pointer sequence was rejected.
        try {
            driver.executeScript("mobile: dragGesture", Map.of(
                    "startX", thumbX, "startY", y, "endX", targetX, "endY", y, "speed", 2000));
            sleep(300);
            if (!opacityValue().equals(before)) return true;
        } catch (WebDriverException ignored) { }

        // Fallback 2 — a tap on the track. Material handles this on ACTION_UP, so it does
        // work on surfaces where the body happens not to be scrollable.
        try {
            driver.executeScript("mobile: clickGesture", Map.of("x", targetX, "y", y));
            sleep(300);
        } catch (WebDriverException ignored) { }
        return !opacityValue().equals(before);
    }

    /**
     * The Slider's bounds, or null when it is not reachable.
     *
     * <p>Picks the WIDEST match on purpose. Material's Slider publishes an
     * {@code ExploreByTouchHelper} virtual child per thumb; if a build ever lets that
     * child inherit the host's resource-id, {@code findElement} would hand back the
     * thumb's few-pixel box and every computed track coordinate would land on the thumb.
     */
    private Rectangle sliderRect() {
        try {
            Rectangle widest = null;
            for (WebElement e : driver.findElements(byId("dd_wm_opacity"))) {
                Rectangle r = e.getRect();
                if (widest == null || r.getWidth() > widest.getWidth()) widest = r;
            }
            return widest;
        } catch (WebDriverException e) { return null; }
    }

    /** press → hold → slop-clearing hop → finer steps → hold → release. */
    private boolean scrub(int fromX, int toX, int y) {
        try {
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence s = new Sequence(finger, 0);
            s.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), fromX, y));
            s.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            s.addAction(new Pause(finger, Duration.ofMillis(350)));
            // First hop covers half the distance in ONE move so it is unmistakably past
            // scaledTouchSlop (~8dp) — see the ACTION_MOVE bail-out quoted above.
            int half = fromX + (toX - fromX) / 2;
            s.addAction(finger.createPointerMove(Duration.ofMillis(200), PointerInput.Origin.viewport(), half, y));
            for (int i = 1; i <= 4; i++) {
                int x = half + (toX - half) * i / 4;
                s.addAction(finger.createPointerMove(Duration.ofMillis(120), PointerInput.Origin.viewport(), x, y));
            }
            s.addAction(new Pause(finger, Duration.ofMillis(350)));
            s.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            driver.perform(List.of(s));
            sleep(350);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    // ==================== single-doc download sheet ====================

    public String downloadSheetFilename() { return textOf("dd_filename"); }

    /** PDF / JPEG / PNG / WEBP only — anything else renders the unsupported block. */
    public boolean sheetSaysSupported()   { return displayed("dd_supported"); }
    public boolean sheetSaysUnsupported() { return displayed("dd_unsupported"); }

    public String stampLine1() { return textOf("dd_text"); }
    public String stampLine2() { return textOf("dd_text_line2"); }

    public boolean setStampLine1(String text) { return typeIn("dd_text", text); }
    public boolean setStampLine2(String text) { return typeIn("dd_text_line2", text); }

    /** Fills BOTH lines with the canned confidential notice. */
    public boolean tapUseFullNotice() { return click("dd_use_full_notice"); }

    /** The first-page render has three states; exactly one is visible at a time. */
    public boolean previewSpinnerShown() { return displayed("dd_preview_progress"); }
    public boolean previewImageShown()   { return displayed("dd_preview_image"); }
    public boolean previewFailedShown()  { return displayed("dd_preview_failed"); }

    /** Waits for the first-page render to settle into image-or-failed. */
    public boolean waitPreviewSettled(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (previewImageShown() || previewFailedShown()) return true;
            sleep(500);
        }
        return false;
    }

    public boolean isDownloadEnabled() { return enabled("dd_download"); }

    /** Closes the download sheet without saving anything. */
    public boolean tapSheetCancel() { return click("dd_cancel"); }

    // ==================== wizard sheet ====================

    /** The wizard's big preview card — wizard-unique, safe as an identity anchor. */
    public String wizardPreviewText() { return textOf("dd_preview_text"); }

    /**
     * Saves the wizard.
     *
     * <p>Save ABORTS (and stays open, with an inline error on the offending input) when a
     * Custom line is selected but blank — so "did the sheet close" is the assertable
     * signal for the validation rule, not the error string, which lives on the
     * TextInputLayout and is not reliably readable through UiAutomator.
     *
     * <p>{@code dd_save} is the LAST child of the wizard's ScrollView, underneath the
     * {@code dd_appearance} include (bottomsheet_dd_watermark_wizard.xml line 168), so it
     * has to be revealed before it can be tapped.
     */
    public boolean tapWizardSave() {
        revealId("dd_save");
        return click("dd_save");
    }

    // ==================== composer preview sheet ====================

    public String composerPreviewFilename() { return textOf("dd_filename"); }
    public boolean composerPreviewFallbackShown() { return displayed("dd_preview_fallback"); }

    /** Ready / Failed — the sheet only ever settles into one of the two. */
    public boolean waitComposerPreviewSettled(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (displayed("dd_preview_image") || displayed("dd_preview_fallback")) return true;
            sleep(500);
        }
        return false;
    }

    public boolean closeComposerPreview() { return click("dd_close"); }

    // ==================== composer-side watermark controls ====================

    /**
     * The composer's watermark bar — {@code View.GONE} unless at least one attachment has
     * its watermark switch on ({@code state.anyWatermark}). Tapping it opens the wizard.
     *
     * <p><b>It is normally already VISIBLE the moment the composer opens.</b>
     * {@code ComposerAttachment.watermarkOn} defaults to {@code true} (ZL-19624, "Every
     * freshly-picked attachment opens with watermark enabled; the user opts OUT"), and
     * {@code anyWatermark = attachments.any { watermarkOn && canWatermark }} — so a single
     * attached PDF is enough. A test that "turns the watermark on" by blindly flipping the
     * switch turns it OFF and makes this bar disappear.
     */
    public boolean watermarkBarShown() { return isVisible("dd_watermark_bar", Duration.ofSeconds(6)); }

    /** True once the bar is gone — the other half of the {@code anyWatermark} rule. */
    public boolean watermarkBarHidden(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (!displayed("dd_watermark_bar")) return true;
            sleep(300);
        }
        return !displayed("dd_watermark_bar");
    }

    public String watermarkSummary()   { return textOf("dd_watermark_summary"); }
    public boolean tapWatermarkBar()   { return click("dd_watermark_bar"); }

    /**
     * Flips the watermark switch on the attachment row carrying this name.
     *
     * <p>Anchored on the row that CONTAINS the name — the switch itself is hidden entirely
     * for non-watermarkable attachments, so pairing two id lists by index would line the
     * n-th name up against a different row's switch.
     *
     * <p>This is a TOGGLE, not a "turn on". Because the switch arrives checked (see
     * {@link #watermarkBarShown()}), prefer {@link #setAttachmentWatermark} whenever the
     * caller wants a known end state.
     */
    public boolean toggleAttachmentWatermark(String name) {
        return clickInAttachmentRow(name, "dd_watermark_switch");
    }

    /**
     * Reads the row's watermark switch: TRUE / FALSE, or null when the row (or the switch)
     * is not there — {@code AttachmentsAdapter} sets the switch to
     * {@code View.GONE} for anything outside {@code FileTypeDetector.canWatermark}.
     */
    public Boolean attachmentWatermarkOn(String name) {
        try {
            List<WebElement> found = attachmentRowControls(name, "dd_watermark_switch");
            if (found.isEmpty()) return null;
            return "true".equalsIgnoreCase(found.get(0).getAttribute("checked"));
        } catch (WebDriverException e) { return null; }
    }

    /** Drives the row's switch to a known state; a no-op when it is already there. */
    public boolean setAttachmentWatermark(String name, boolean on) {
        Boolean state = attachmentWatermarkOn(name);
        if (state == null) return false;
        if (state == on) return true;
        if (!toggleAttachmentWatermark(name)) return false;
        sleep(350);
        return Boolean.valueOf(on).equals(attachmentWatermarkOn(name));
    }

    /** The eye button — only rendered while {@code watermarkOn && canWatermark}. */
    public boolean openAttachmentWatermarkPreview(String name) {
        return clickInAttachmentRow(name, "dd_btn_preview_watermark");
    }

    public boolean attachmentWatermarkPreviewAvailable(String name) {
        return existsInAttachmentRow(name, "dd_btn_preview_watermark");
    }

    // ==================== recovery ====================

    /**
     * True when the ZIP's {@code ACTION_VIEW} (or any other hand-off) took the foreground.
     *
     * <p>The batch screen saves to Downloads and then opens the file, which can raise a
     * chooser or a third-party archive viewer. Left alone that costs every later case, so
     * a caller that taps Download ZIP must unwind with {@link #returnToApp}.
     */
    public boolean inForeignPackage() {
        try {
            String pkg = driver.getCurrentPackage();
            return pkg != null && !pkg.startsWith(appPackage());
        } catch (WebDriverException e) { return false; }
    }

    /** Presses BACK until the app is in the foreground again. */
    public boolean returnToApp() {
        for (int i = 0; i < 6 && inForeignPackage(); i++) {
            try { driver.navigate().back(); Thread.sleep(250); }
            catch (Exception e) { break; }
        }
        return !inForeignPackage();
    }

    // ---------------- helpers ----------------

    /**
     * Scrolls until {@code id} is genuinely on screen.
     *
     * <p>{@code UiScrollable} is tried first, but it picks the FIRST scrollable in the
     * tree, and a watermark bottom sheet is always stacked over the library's own
     * RecyclerView — so it can end up scrolling the wrong container. The fallback is a
     * {@code mobile: swipeGesture} over the middle band of the screen, which is inside the
     * sheet body (or the batch fragment's NestedScrollView) whichever surface is up.
     */
    private boolean revealId(String id) {
        for (int attempt = 0; attempt < 5; attempt++) {
            if (displayed(id)) return true;
            scrollToId(id);
            if (displayed(id)) return true;
            if (!swipeBodyUp()) break;
        }
        return displayed(id);
    }

    /** One upward swipe over the body — scrolls the content up, revealing what is below. */
    private boolean swipeBodyUp() {
        try {
            Dimension size = driver.manage().window().getSize();
            driver.executeScript("mobile: swipeGesture", Map.<String, Object>of(
                    "left", (int) (size.getWidth() * 0.10),
                    "top", (int) (size.getHeight() * 0.30),
                    "width", (int) (size.getWidth() * 0.80),
                    "height", (int) (size.getHeight() * 0.45),
                    "direction", "up",
                    "percent", 0.75));
            sleep(350);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    /**
     * A control inside the composer attachment row that carries this filename.
     *
     * <p>The predicate walks to any ancestor holding a matching {@code dd_name}, so the
     * first match is the row card itself and the trailing step lands on that row's own
     * control — never the n-th control of a differently-ordered id list.
     */
    private String rowControlXpath(String name, String controlId) {
        return "//*[@resource-id='" + appPackage() + ":id/dd_attachments_recycler']"
                + "//*[.//*[@resource-id='" + appPackage() + ":id/dd_name'"
                + " and contains(@text," + xpathLiteral(name) + ")]]"
                + "//*[@resource-id='" + appPackage() + ":id/" + controlId + "']";
    }

    /**
     * The row's control, or an empty list.
     *
     * <p>Self-healing: the composer is one long NestedScrollView and the attachments block
     * sits near its bottom, so a sheet that scrolled the body (the wizard's Save is below
     * its own appearance block) can leave the row off-screen and therefore out of the
     * dump. One reveal, then look again.
     */
    private List<WebElement> attachmentRowControls(String name, String controlId) {
        try {
            List<WebElement> found =
                    driver.findElements(AppiumBy.xpath(rowControlXpath(name, controlId)));
            if (!found.isEmpty()) return found;
            revealId("dd_attachments_recycler");
            return driver.findElements(AppiumBy.xpath(rowControlXpath(name, controlId)));
        } catch (WebDriverException e) { return List.of(); }
    }

    private boolean clickInAttachmentRow(String name, String controlId) {
        try {
            List<WebElement> found = attachmentRowControls(name, controlId);
            if (found.isEmpty()) return false;
            found.get(0).click();
            sleep(400);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    private boolean existsInAttachmentRow(String name, String controlId) {
        return !attachmentRowControls(name, controlId).isEmpty();
    }

    /** Direct children of a ChipGroup, as text. */
    private List<String> childTexts(String groupId) {
        List<String> out = new ArrayList<>();
        try {
            for (WebElement e : driver.findElements(AppiumBy.xpath(
                    "//*[@resource-id='" + appPackage() + ":id/" + groupId + "']/*"))) {
                String t = e.getText();
                if (t != null && !t.isBlank()) out.add(t);
            }
        } catch (WebDriverException ignored) { }
        return out;
    }

    private boolean typeIn(String id, String text) {
        try {
            scrollToId(id);
            WebElement f = driver.findElement(byId(id));
            f.click();
            f.clear();
            if (!text.isEmpty()) f.sendKeys(text);
            hideKeyboardIfShown();
            sleep(600);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    private boolean enabled(String id) {
        try { return driver.findElement(byId(id)).isEnabled(); }
        catch (WebDriverException e) { return false; }
    }

    private boolean present(String id) {
        try { return !driver.findElements(byId(id)).isEmpty(); }
        catch (WebDriverException e) { return false; }
    }

    private boolean displayed(String id) {
        try {
            for (WebElement e : driver.findElements(byId(id))) {
                if (e.isDisplayed()) return true;
            }
        } catch (WebDriverException ignored) { }
        return false;
    }

    private String textOf(String id) {
        try { return driver.findElement(byId(id)).getText(); }
        catch (WebDriverException e) { return ""; }
    }

    private boolean click(String id) {
        try {
            scrollToId(id);
            driver.findElement(byId(id)).click();
            sleep(350);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    private static String xpathLiteral(String s) {
        if (!s.contains("'")) return "'" + s + "'";
        return "concat('" + s.replace("'", "',\"'\",'") + "')";
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
