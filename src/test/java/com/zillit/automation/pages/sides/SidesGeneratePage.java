package com.zillit.automation.pages.sides;

import com.zillit.automation.pages.common.BasePage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * SIDES — the Generate screen and the Autogenerate sheet, which share one review view.
 *
 * <p>Ids from {@code activity_sides_generate.xml},
 * {@code bottomsheet_sides_autogenerate.xml}, {@code item_sides_scene_picker.xml} and
 * {@code view_sides_review.xml}.
 *
 * <p><b>The review is an overlay, not a replacement.</b> When generation finishes, the
 * review FrameLayout is laid over the form and BOTH remain in the accessibility tree — the
 * version chips, the title field and the submit button are all still findable. So every
 * question about which stage we are in has to be asked of what is DISPLAYED, never of what
 * is merely present.
 */
public class SidesGeneratePage extends BasePage {

    public static final String HEADER = "GENERATE SIDES";
    /** The Generate form's own ScrollView. */
    private static final String FORM_SCROLLER = "scroll";
    /** The Autogenerate sheet's ScrollView. */
    private static final String SHEET_SCROLLER = "formScroll";
    public static final String READY_TEXT = "generated successfully";

    /** The submit-time refusals, in the order the ViewModel checks them. */
    public static final String ERR_NO_SCRIPT = "No script found";
    public static final String ERR_NO_SCENES = "Select at least one scene or page";
    public static final String ERR_NO_ORDER  = "Enter a valid scene order";
    public static final String ERR_NO_CALL_SHEET = "Select or upload a call sheet first";
    public static final String ERR_NO_AUTO_SCENES = "No scenes to generate";

    public SidesGeneratePage(AndroidDriver driver) {
        super(driver);
    }

    /** One expandable version / page picker row. */
    public static final class Picker {
        public final String label;
        public final String subtitle;
        public final boolean expanded;
        public final List<String> scenes;

        Picker(String label, String subtitle, boolean expanded, List<String> scenes) {
            this.label = label; this.subtitle = subtitle;
            this.expanded = expanded; this.scenes = scenes;
        }

        @Override public String toString() {
            return label + " (" + subtitle + ")" + (expanded ? " open " + scenes : " closed");
        }
    }

    public boolean isLoaded(Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        do {
            if (displayed("btnSubmit") || displayed("review")) return true;
            pause(400);
        } while (System.currentTimeMillis() < end);
        return false;
    }

    // ───────────────────────────── the form ─────────────────────────────

    public boolean formShown() { return displayed("btnSubmit") && !reviewShown(); }

    /**
     * The scripts currently selected on the form, read from their chips.
     *
     * <p>A Material {@code Chip} renders as {@code android.widget.Button}, not a TextView —
     * reading these as text views finds nothing and makes a populated form look empty.
     * A script with no file carries a suffix on its chip, so callers match loosely.
     */
    public List<String> selectedScripts() {
        List<String> out = new ArrayList<>();
        try {
            for (WebElement group : driver.findElements(byId("selectedScriptChips"))) {
                // A Material Chip usually renders as android.widget.Button, but a
                // non-clickable one can come through as a TextView, so take either rather
                // than reporting an empty form because of the class name.
                for (String cls : new String[]{"android.widget.Button",
                        "android.widget.TextView", "android.widget.CheckedTextView"}) {
                    for (WebElement c : group.findElements(AppiumBy.className(cls))) {
                        String t = safe(c);
                        if (t != null && !t.isBlank() && !out.contains(t.trim())) {
                            out.add(t.trim());
                        }
                    }
                }
            }
        } catch (Exception ignored) { }
        return out;
    }

    /**
     * Makes sure a named script is one of the selected ones, adding it if it is not.
     *
     * <p>The form picks its own starting selection, and on a project carrying several
     * scripts that can easily be one with NO file — which has no versions, so the version
     * list comes back empty and every case that needs a scene skips with "the form offers
     * no versions". The form is not wrong; the case simply never said which script it meant.
     *
     * <p>It never meddles with a form it cannot read. An earlier version treated "I cannot
     * see the chips" as "no script is selected", went off to add one, and left the form
     * behind it — after which Generate Sides was not even clickable and three cases that
     * had nothing to do with scripts broke.
     */
    public boolean ensureScriptSelected(String title) {
        if (selectedScripts().stream().anyMatch(t -> t.contains(title))) return true;
        // An EMPTY ChipGroup has no children, so it has no height and never reaches the
        // accessibility tree: "I cannot see the chips" and "no script is selected" look
        // identical from here. Both are answered the same way — add the one this case is
        // about — now that adding can no longer wander off the form when it finds nothing.
        if (!addScript(title)) return false;
        pause(2500);
        return selectedScripts().stream().anyMatch(t -> t.contains(title));
    }

    /** Adds a script through the "+ Add another script…" spinner. */
    public boolean addScript(String title) {
        if (!clickIfPresent("spinnerScript")) return false;
        pause(2000);

        // A Spinner's dropdown rows are android.widget.CheckedTextView carrying
        // android:id/text1 — the ANDROID package's id, not the app's. Searching for
        // "android.widget.TextView" matches none of them (measured: 0 TextViews, 3
        // CheckedTextViews), and the app's own id prefix never resolves text1 at all. So
        // match on the id across whatever package owns it, which is also class-agnostic.
        String rowsSelector = "new UiSelector().resourceIdMatches(\".*:id/text1\")";
        boolean dropdownOpen = false;
        try {
            List<WebElement> rows = driver.findElements(
                    AppiumBy.androidUIAutomator(rowsSelector));
            dropdownOpen = !rows.isEmpty();
            for (WebElement row : rows) {
                String t = safe(row);
                if (t == null || !t.contains(title)) continue;
                row.click();
                pause(2500);
                return true;
            }
            if (dropdownOpen) {
                System.out.println(">>> sides generate: the script dropdown does not offer \""
                        + title + "\". It offers: " + rows.stream().map(this::safe).toList());
            }
        } catch (Exception ignored) { }

        // Close the dropdown ONLY if one actually opened. A back press with no popup on
        // screen leaves the Generate form altogether, which is how a failed lookup here
        // took out the cases after it.
        if (dropdownOpen) {
            try { driver.navigate().back(); } catch (Exception ignored) { }
            pause(900);
        }
        return false;
    }

    public boolean scriptChipsShown()  { return displayed("selectedScriptChips"); }
    public boolean addScriptSpinnerShown() { return displayed("spinnerScript"); }

    public boolean noVersionsShown() { return displayed("tvNoVersions"); }
    public boolean noPagesShown()    { return displayed("tvNoPages"); }

    /**
     * The "n scene(s) selected" line.
     *
     * <p>Scrolled to first: with several chips picked, this label ends up below them in the
     * form's ScrollView and drops out of the tree — which once read as the app failing to
     * count a selection it had counted perfectly well.
     */
    public String summaryCount() {
        if (!displayed("tvSummaryCount")) {
            for (int pass = 0; pass < 8 && !displayed("tvSummaryCount"); pass++) {
                if (!scrollList(FORM_SCROLLER, true)) break;
            }
        }
        return text("tvSummaryCount");
    }

    /** Scene chips in the SELECTED SCENES strip. */
    public List<String> summaryChips() { return textsUnder("summaryChips"); }

    /**
     * Brings one of the form's own controls on screen before it is read or tapped.
     *
     * <p>The form is a ScrollView and it GROWS: pick a script and its versions appear,
     * expand a version and its scene chips appear. Submit, Rearrange and the title field
     * slide below the fold as that happens, and a control that is not rendered cannot be
     * clicked — {@code clickIfPresent} simply reports it is not there, which reads as
     * "Generate Sides was not clickable" on a form that is working perfectly.
     */
    private boolean bringIntoView(String id) {
        if (displayed(id)) return true;
        for (int pass = 0; pass < 8; pass++) {
            if (!scrollList(FORM_SCROLLER, true)) break;
            if (displayed(id)) return true;
        }
        // It may have been above us all along.
        for (int pass = 0; pass < 8; pass++) {
            if (!scrollList(FORM_SCROLLER, false)) break;
            if (displayed(id)) return true;
        }
        return false;
    }

    public boolean rearrangeShown()   { return bringIntoView("cbRearrange"); }
    public boolean rearrangeChecked() { return checked("cbRearrange"); }
    public boolean toggleRearrange()  { return bringIntoView("cbRearrange") && clickIfPresent("cbRearrange"); }

    /** The order input only appears once rearrange is on. */
    public boolean orderInputShown() { return bringIntoView("etOrder"); }

    /** The draggable chips that carry the order when several scripts are in play. */
    public boolean orderChipsShown() { return bringIntoView("rvOrderChips"); }

    /**
     * Whether Rearrange has revealed a way to set the order, in EITHER of its two shapes.
     *
     * <p>The app hides the single order field as soon as more than one script is selected
     * ({@code tilOrder.visibility = showOrder && !isMultiScript}) and orders by chips
     * instead, which is the sensible rule — a plain "3, 1, 2" means nothing across two
     * scripts. A case that only ever looked for the text field therefore failed on a form
     * that was behaving exactly as designed.
     */
    public boolean orderControlsShown() { return orderInputShown() || orderChipsShown(); }

    /**
     * What the rearrange block actually looks like right now, for a failure message.
     *
     * <p>Worth its own method because "no order control appeared" has two very different
     * causes that read identically from a boolean: the checkbox never flipped (our click
     * missed), or it flipped and the app drew nothing (its bug). The app sets
     * {@code tilOrder.visibility = rearrangeOn && !isMultiScript}, so a ticked box with
     * no field is the app's problem and an unticked box is ours.
     */
    public String rearrangeDiag() {
        // bringIntoView, NOT displayed: displayed() answers "is it on screen right now",
        // which cannot tell a view the app left GONE from one that is merely scrolled out —
        // and that is the whole question here. bringIntoView scrolls the form both ways
        // before answering, so a false means the view is genuinely not in the form.
        return "checkbox=" + (rearrangeChecked() ? "ticked" : "unticked")
                + " tilOrder=" + bringIntoView("tilOrder")
                + " etOrder=" + bringIntoView("etOrder")
                + " rvOrderChips=" + bringIntoView("rvOrderChips")
                + " orderAddChips=" + bringIntoView("orderAddChips")
                + " tvOrderPreview=" + bringIntoView("tvOrderPreview")
                + " (bringIntoView: scrolled the whole form, so false means absent)";
    }
    public boolean setOrder(String v) { return bringIntoView("etOrder") && fill("etOrder", v); }

    public boolean crossoutSelected() { return checked("rbCrossout"); }
    public boolean hideSelected()     { return checked("rbHide"); }
    public boolean chooseCrossout()   { return clickIfPresent("rbCrossout"); }
    public boolean chooseHide()       { return clickIfPresent("rbHide"); }

    public boolean setTitle(String v) { return bringIntoView("etTitle") && fill("etTitle", v); }
    public String titleValue()        { return text("etTitle"); }

    public boolean submit() { return bringIntoView("btnSubmit") && clickIfPresent("btnSubmit"); }

    // ───────────────────────── version / page pickers ─────────────────────────

    /**
     * Every picker row on screen, versions first then pages, in list order.
     *
     * <p>Sliced by {@link #bandsOf(String)}: a picker's {@code body} and {@code header}
     * are plain ViewGroups, and an element-relative {@code ancestor::} XPath never looks
     * upward in Appium anyway. A collapsed picker's chip group is GONE, so it contributes
     * no chips and no height — which is exactly how {@code expanded} is read.
     */
    public List<Picker> pickers() {
        return readWholeList(FORM_SCROLLER, "tv_label", this::pickersOnScreen, p -> p.label);
    }

    /** The pickers currently rendered. {@link #pickers()} follows the form past the fold. */
    private List<Picker> pickersOnScreen() {
        List<Picker> out = new ArrayList<>();
        List<WebElement> labels = elementsById("tv_label");
        List<Band> bands = bandsOf("tv_label");
        for (int i = 0; i < labels.size() && i < bands.size(); i++) {
            Band band = bands.get(i);
            List<String> scenes = new ArrayList<>();
            boolean open = false;
            for (WebElement group : elementsIn(band, "chip_group")) {
                try {
                    if (!group.isDisplayed()) continue;
                } catch (Exception e) { continue; }
                open = true;
                for (WebElement c : group.findElements(
                        AppiumBy.className("android.widget.Button"))) {
                    String s2 = safe(c);
                    if (s2 != null && !s2.isBlank()) scenes.add(s2);
                }
            }
            out.add(new Picker(safe(labels.get(i)), textIn(band, "tv_subtitle"), open, scenes));
        }
        return out;
    }

    /** Expands (or collapses) the picker whose label matches exactly. */
    public boolean togglePicker(String label) {
        if (!scrollToRow(FORM_SCROLLER, "tv_label", label)) return false;
        List<WebElement> labels = elementsById("tv_label");
        List<Band> bands = bandsOf("tv_label");
        for (int i = 0; i < labels.size() && i < bands.size(); i++) {
            WebElement l = labels.get(i);
            if (!label.equals(safe(l))) continue;
            WebElement header = elementIn(bands.get(i), "header");
            WebElement target = header == null ? l : header;
            try { target.click(); } catch (Exception e) { return false; }
            pause(3000);
            return true;
        }
        return false;
    }

    /** Ticks a scene chip inside an expanded picker. */
    public boolean pickScene(String pickerLabel, String scene) {
        if (!scrollToRow(FORM_SCROLLER, "tv_label", pickerLabel)) return false;
        List<WebElement> labels = elementsById("tv_label");
        List<Band> bands = bandsOf("tv_label");
        for (int i = 0; i < labels.size() && i < bands.size(); i++) {
            if (!pickerLabel.equals(safe(labels.get(i)))) continue;
            for (WebElement g : elementsIn(bands.get(i), "chip_group")) {
                for (WebElement c : g.findElements(
                        AppiumBy.className("android.widget.Button"))) {
                    if (!scene.equals(safe(c))) continue;
                    try { c.click(); } catch (Exception e) { return false; }
                    pause(900);
                    return true;
                }
            }
        }
        return false;
    }

    public boolean selectAllIn(String pickerLabel) { return rowAction(pickerLabel, "btn_select_all"); }
    public boolean clearIn(String pickerLabel)     { return rowAction(pickerLabel, "btn_clear"); }

    /** "Include whole PDF" — pages only; it is what fills sceneFolderIds. */
    public boolean toggleWholePdf(String pickerLabel) { return rowAction(pickerLabel, "cb_whole_pdf"); }

    private boolean rowAction(String label, String controlId) {
        if (!scrollToRow(FORM_SCROLLER, "tv_label", label)) return false;
        List<WebElement> labels = elementsById("tv_label");
        List<Band> bands = bandsOf("tv_label");
        for (int i = 0; i < labels.size() && i < bands.size(); i++) {
            if (!label.equals(safe(labels.get(i)))) continue;
            WebElement c = elementIn(bands.get(i), controlId);
            if (c == null) return false;
            try {
                if (!c.isDisplayed()) return false;
                c.click();
            } catch (Exception e) { return false; }
            pause(1200);
            return true;
        }
        return false;
    }

    // ───────────────────────── Autogenerate sheet ─────────────────────────

    /**
     * Whether the Autogenerate sheet is up.
     *
     * <p>NOT read from {@code tvActiveScript}: the sheet sets that view GONE when it has
     * no active script to name, so the one state most worth testing — the sheet open with
     * nothing selected — looks exactly like the sheet being closed. The upload rows are
     * always there, and the check polls because a bottom sheet animates in.
     */
    public boolean sheetShown() { return sheetShown(Duration.ofSeconds(6)); }

    public boolean sheetShown(Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        do {
            if (displayed("tvUploadCallSheet") || displayed("rvCallSheets")
                    || displayed("tvUploadSchedule")) {
                return true;
            }
            pause(400);
        } while (System.currentTimeMillis() < end);
        return false;
    }

    public String activeScript() { return text("tvActiveScript"); }

    public boolean uploadCallSheet() { return clickIfPresent("tvUploadCallSheet"); }
    public boolean uploadSchedule()  { return clickIfPresent("tvUploadSchedule"); }

    public boolean callSheetsShown() { return displayed("rvCallSheets"); }
    public boolean schedulesShown()  { return displayed("rvSchedules"); }

    /** Call-sheet rows, as "title | meta". */
    public List<String> callSheetRows() { return rowSummaries("rvCallSheets"); }
    public List<String> scheduleRows()  { return rowSummaries("rvSchedules"); }

    private List<String> rowSummaries(String listId) {
        if (!displayed(listId)) return new ArrayList<>();
        return readWholeList(SHEET_SCROLLER, "tv_title",
                () -> rowSummariesOnScreen(listId), r -> r);
    }

    private List<String> rowSummariesOnScreen(String listId) {
        List<String> out = new ArrayList<>();
        if (!displayed(listId)) return out;
        List<WebElement> titles = elementsById("tv_title");
        List<Band> bands = bandsOf("tv_title");
        for (int i = 0; i < titles.size() && i < bands.size(); i++) {
            // Both lists in the sheet use the same row layout, so a row counts for this
            // list only when it carries the radio button those rows have.
            if (elementIn(bands.get(i), "radio") == null) continue;
            out.add(safe(titles.get(i)) + " | " + String.valueOf(textIn(bands.get(i), "tv_meta")));
        }
        return out;
    }

    /** Selects a call sheet by its title fragment. */
    public boolean selectCallSheet(String titleFragment) {
        return selectRadio("rvCallSheets", titleFragment);
    }

    public boolean selectSchedule(String titleFragment) {
        return selectRadio("rvSchedules", titleFragment);
    }

    private boolean selectRadio(String listId, String titleFragment) {
        if (!displayed(listId)) return false;
        List<WebElement> titles = elementsById("tv_title");
        List<Band> bands = bandsOf("tv_title");
        for (int i = 0; i < titles.size() && i < bands.size(); i++) {
            String t = safe(titles.get(i));
            if (t == null || !t.contains(titleFragment)) continue;
            WebElement radio = elementIn(bands.get(i), "radio");
            if (radio == null) return false;
            try { radio.click(); } catch (Exception e) { return false; }
            pause(1500);
            return true;
        }
        return false;
    }

    // ──────────────────── layout order and empty states ────────────────────

    /**
     * Top edge of the view with this id, or {@code null} when it is not on screen.
     *
     * <p>Used to ask whether a section's heading actually sits above the controls it
     * heads. A heading that renders below its own action is not a cosmetic nit: the
     * action then reads as belonging to the section above it.
     */
    public Integer topOf(String id) {
        try {
            List<WebElement> e = driver.findElements(byId(id));
            if (e.isEmpty()) return null;
            return e.get(0).getRect().getY();
        } catch (Exception ex) { return null; }
    }

    /** Top edge of the first view whose text is exactly {@code label}. */
    public Integer topOfText(String label) {
        try {
            List<WebElement> e = driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().text(\"" + label + "\")"));
            if (e.isEmpty()) return null;
            return e.get(0).getRect().getY();
        } catch (Exception ex) { return null; }
    }

    /**
     * Whether anything on screen explains that a list is empty — a line that opens with
     * "no"/"there are no" and names the thing, e.g. "No call sheets yet".
     *
     * <p>An empty RecyclerView draws nothing at all, so without such a line the user is
     * left looking at a blank strip with no way to tell an empty project from a failed
     * load.
     */
    public boolean emptyExplanationShown(String keyword) {
        String scroller = sheetShown(Duration.ZERO) ? SHEET_SCROLLER : FORM_SCROLLER;
        return anyTextMatches(scroller, t -> SidesListPage.saysEmpty(t, keyword));
    }

    // ───────────────────────────── review stage ─────────────────────────────

    public boolean reviewShown()     { return displayed("review"); }
    public boolean generatingShown() { return displayed("layGenerating"); }
    public boolean readyShown()      { return displayed("layReady"); }
    public boolean failedShown()     { return displayed("layFailed"); }
    public String failedMessage()    { return text("tvFailedMsg"); }

    /**
     * Waits for generation to settle.
     *
     * @return true when the review reached READY, false on failure or timeout
     */
    public boolean waitForReady(Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        do {
            if (readyShown()) return true;
            if (failedShown()) return false;
            pause(2000);
        } while (System.currentTimeMillis() < end);
        return false;
    }

    public boolean reviewViewShown()     { return displayed("btnReviewView"); }
    public boolean reviewDownloadShown() { return displayed("btnDownload"); }
    public boolean reviewPublishShown()  { return displayed("btnPublish"); }

    public boolean reviewView()     { return clickIfPresent("btnReviewView"); }
    public boolean reviewDownload() { return clickIfPresent("btnDownload"); }
    public boolean reviewPublish()  { return clickIfPresent("btnPublish"); }

    /**
     * Is "Move to Document Distribution" offered?
     *
     * <p>The button exists in the layout and both hosts wire a click listener to it, but it
     * is declared {@code visibility="gone"} and nothing ever makes it visible — so this
     * answers false, and a case asserting otherwise is asserting about a feature that
     * cannot be reached from here.
     */
    public boolean moveToDocDistShown() { return displayed("btnMove"); }

    // ───────────────────────────── helpers ─────────────────────────────

    private List<String> textsUnder(String containerId) {
        List<String> out = new ArrayList<>();
        try {
            for (WebElement c : driver.findElements(byId(containerId))) {
                for (WebElement e : c.findElements(AppiumBy.xpath(".//*"))) {
                    String s = safe(e);
                    if (s != null) out.add(s);
                }
            }
        } catch (Exception ignored) { }
        return out;
    }

    private String childText(WebElement parent, String id) {
        try {
            List<WebElement> els = parent.findElements(byId(id));
            return els.isEmpty() ? null : safe(els.get(0));
        } catch (Exception e) { return null; }
    }

    private String safe(WebElement e) {
        try {
            String t = e.getText();
            return t == null || t.isBlank() ? null : t.trim();
        } catch (Exception ex) { return null; }
    }

    private String text(String id) {
        try {
            List<WebElement> els = driver.findElements(byId(id));
            return els.isEmpty() ? null : els.get(0).getText();
        } catch (Exception e) { return null; }
    }

    private boolean displayed(String id) {
        try {
            for (WebElement e : driver.findElements(byId(id))) {
                if (e.isDisplayed()) return true;
            }
        } catch (Exception ignored) { }
        return false;
    }

    private boolean checked(String id) {
        try {
            for (WebElement e : driver.findElements(byId(id))) {
                if (e.isDisplayed()) return "true".equals(e.getAttribute("checked"));
            }
        } catch (Exception ignored) { }
        return false;
    }

    private boolean clickIfPresent(String id) {
        try {
            for (WebElement e : driver.findElements(byId(id))) {
                if (e.isDisplayed()) { e.click(); pause(1500); return true; }
            }
        } catch (Exception ignored) { }
        return false;
    }

    private boolean fill(String id, String value) {
        try {
            for (WebElement e : driver.findElements(byId(id))) {
                if (!e.isDisplayed()) continue;
                e.click();
                e.clear();
                if (!value.isEmpty()) e.sendKeys(value);
                hideKeyboard();
                pause(700);
                return true;
            }
        } catch (Exception ignored) { }
        return false;
    }

    private void pause(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
