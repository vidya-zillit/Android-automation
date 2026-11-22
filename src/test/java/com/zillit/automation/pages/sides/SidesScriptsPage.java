package com.zillit.automation.pages.sides;

import com.zillit.automation.pages.common.BasePage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * SIDES — the Script tab: the scripts list with each script's embedded Pages, plus the
 * Add Script and Add Page dialogs.
 *
 * <p>Ids from {@code item_sides_script_card.xml}, {@code item_sides_page_row.xml},
 * {@code dialog_sides_add_script.xml} and {@code dialog_sides_add_page.xml}.
 *
 * <p><b>A card's action set tells you whether the script has a file.</b> Without one the
 * card offers only "Upload Script" and delete; with one it offers Replace, View, a
 * download icon and delete. That is the same {@code hasFile} the Generate and Autogenerate
 * gates read, so the buttons are a visible proxy for the gate.
 */
public class SidesScriptsPage extends BasePage {

    public static final String HEADER = "SCRIPT";
    /** The scrollable the script cards live in. */
    private static final String SCROLLER = "swipeRefresh";
    public static final String UPLOAD_SCRIPT = "Upload Script";
    public static final String REPLACE = "Replace";

    public SidesScriptsPage(AndroidDriver driver) {
        super(driver);
    }

    /** One script card. */
    public static final class Card {
        public final String title;
        public final String meta;
        public final boolean hasFile;
        public final List<String> pages;

        Card(String title, String meta, boolean hasFile, List<String> pages) {
            this.title = title; this.meta = meta; this.hasFile = hasFile; this.pages = pages;
        }

        @Override public String toString() {
            return "\"" + title + "\" " + meta + (hasFile ? " [file]" : " [no file]")
                    + " pages=" + pages;
        }
    }

    public boolean isLoaded(Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        do {
            if (displayed("btnAddScript") || displayed("rvScripts") || displayed("layEmpty")) {
                return true;
            }
            pause(400);
        } while (System.currentTimeMillis() < end);
        return false;
    }

    public boolean emptyStateShown() { return displayed("layEmpty"); }

    public boolean openAddScript() { return clickIfPresent("btnAddScript"); }

    /**
     * Every script card on screen.
     *
     * <p>Sliced by {@link #bandsOf(String)} rather than by walking up from the title: the
     * card's own {@code lay_actions} container is pruned out of the accessibility tree, so
     * there is nothing above the title to walk to. The page rows embedded in a card reuse
     * {@code tv_meta}, so the card's meta is the FIRST one in the band — which is also
     * where the adapter puts it.
     */
    public List<Card> cards() {
        return readWholeList(SCROLLER, "tv_title", this::cardsOnScreen, c -> c.title);
    }

    /**
     * The cards currently rendered. {@link #cards()} follows the list past the fold, which
     * matters here more than anywhere: a script card carries a title, a meta line, five
     * buttons and its embedded pages, so only two or three fit on a screen.
     */
    private List<Card> cardsOnScreen() {
        List<Card> out = new ArrayList<>();
        List<WebElement> titles = elementsById("tv_title");
        List<Band> bands = bandsOf("tv_title");
        for (int i = 0; i < titles.size() && i < bands.size(); i++) {
            Band band = bands.get(i);
            // "Replace" only exists once a file is uploaded; before that the same button
            // reads "Upload Script". It is on every card, and on no page row, which also
            // makes it the marker that a title belongs to a card at all.
            String replaceLabel = textIn(band, "btn_replace");
            if (replaceLabel == null) continue;
            List<String> pages = new ArrayList<>();
            for (WebElement sc : elementsIn(band, "tv_scene")) {
                String t = safe(sc);
                if (t != null) pages.add(t);
            }
            out.add(new Card(
                    safe(titles.get(i)),
                    textIn(band, "tv_meta"),
                    REPLACE.equalsIgnoreCase(replaceLabel.trim()),
                    pages));
        }
        return out;
    }

    public List<String> titles() {
        List<String> out = new ArrayList<>();
        for (Card c : cards()) if (c.title != null) out.add(c.title);
        return out;
    }

    /** The card whose title matches EXACTLY, or null. */
    public Card find(String title) {
        for (Card c : cards()) if (title.equals(c.title)) return c;
        return null;
    }

    /**
     * Waits for a card's meta line to report the page count the file really has.
     *
     * <p>The page count comes from the backend parsing the PDF, which it does after
     * accepting the upload: the first card to come back reads "1 pages" for a five-page
     * script and corrects itself a few polls later. Asserting on the first value makes a
     * working upload look broken.
     *
     * @return the card once its meta names {@code pages} pages, or the last card seen
     */
    public Card waitForPageCount(String title, int pages, Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        Card last = null;
        String wanted = pages + " page";
        do {
            last = find(title);
            if (last != null && last.meta != null && last.meta.contains(wanted)) return last;
            pause(3000);
            pullToRefresh();
        } while (System.currentTimeMillis() < end);
        return last;
    }

    /**
     * Swipe-to-refresh on the scripts list.
     *
     * <p>The list only reloads on a refresh or on the fragment resuming, so polling alone
     * would keep reading the same stale card forever.
     */
    public void pullToRefresh() {
        try {
            List<WebElement> sr = driver.findElements(byId("swipeRefresh"));
            if (sr.isEmpty()) return;
            org.openqa.selenium.Rectangle r = sr.get(0).getRect();
            int x = r.getX() + r.getWidth() / 2;
            swipe(x, r.getY() + r.getHeight() / 4, x, r.getY() + (int) (r.getHeight() * 0.8));
            pause(3500);
        } catch (Exception ignored) { }
    }

    private void swipe(int x1, int y1, int x2, int y2) {
        org.openqa.selenium.interactions.PointerInput f =
                new org.openqa.selenium.interactions.PointerInput(
                        org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        org.openqa.selenium.interactions.Sequence seq =
                new org.openqa.selenium.interactions.Sequence(f, 1);
        seq.addAction(f.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x1, y1));
        seq.addAction(f.createPointerDown(0));
        for (int i = 1; i <= 8; i++) {
            seq.addAction(f.createPointerMove(Duration.ofMillis(45),
                    org.openqa.selenium.interactions.PointerInput.Origin.viewport(),
                    x1 + (x2 - x1) * i / 8, y1 + (y2 - y1) * i / 8));
        }
        seq.addAction(f.createPointerUp(0));
        driver.perform(List.of(seq));
    }

    // ─────────────────────── per-card actions ───────────────────────

    public boolean tapReplace(String title)  { return tapCardControl(title, "btn_replace"); }
    public boolean tapView(String title)     { return tapCardControl(title, "btn_view"); }
    public boolean tapDownload(String title) { return tapCardControl(title, "btn_download"); }
    public boolean tapDelete(String title)   { return tapCardControl(title, "btn_delete"); }
    public boolean tapAddPage(String title)  { return tapCardControl(title, "btn_add_page"); }

    private boolean tapCardControl(String title, String controlId) {
        // Three attempts, because every step here is a measurement of a moving list: the
        // row has to be scrolled up, its band re-derived from the new geometry, and the
        // control inside that band found and still on screen. A card is tall enough that
        // scrolling the TITLE into view routinely leaves its buttons below the fold, and
        // the band is only valid for the frame it was measured in.
        //
        // Every exit says why. This used to have four silent `return false`s, and the
        // caller could only report "it could not be tapped" — true, useless, and
        // indistinguishable from the row not existing at all.
        for (int attempt = 1; attempt <= 3; attempt++) {
            if (!scrollToRow(SCROLLER, "tv_title", title)) {
                System.out.println(">>> sides scripts: \"" + title + "\" could not be"
                        + " scrolled to (attempt " + attempt + " of 3). On screen: " + titles());
                pause(900);
                continue;
            }
            WebElement control = resolveCardControl(title, controlId);
            if (control == null) {
                // The title is up but the card's own controls are past the fold: nudge the
                // list by one window and re-measure.
                scrollList(SCROLLER, true);
                control = resolveCardControl(title, controlId);
            }
            if (control == null) {
                System.out.println(">>> sides scripts: \"" + title + "\" has no " + controlId
                        + " in view even after scrolling (attempt " + attempt + " of 3)");
                pause(900);
                continue;
            }
            try {
                if (!control.isDisplayed()) {
                    System.out.println(">>> sides scripts: " + controlId + " for \"" + title
                            + "\" was found but is not on screen (attempt " + attempt
                            + " of 3)");
                    scrollList(SCROLLER, true);
                    continue;
                }
                control.click();
                pause(1800);
                return true;
            } catch (Exception e) {
                System.out.println(">>> sides scripts: tapping " + controlId + " on \"" + title
                        + "\" threw " + e.getClass().getSimpleName() + " (attempt " + attempt
                        + " of 3)");
                pause(900);
            }
        }
        return false;
    }

    /** The named control inside {@code title}'s own band, or null if it is not in view. */
    private WebElement resolveCardControl(String title, String controlId) {
        List<WebElement> titles = elementsById("tv_title");
        List<Band> bands = bandsOf("tv_title");
        for (int i = 0; i < titles.size() && i < bands.size(); i++) {
            if (title.equals(safe(titles.get(i)))) {
                return elementIn(bands.get(i), controlId);
            }
        }
        return null;
    }

    // ─────────────────────── Add Script dialog ───────────────────────

    public boolean addScriptDialogShown() { return displayed("etName"); }

    public boolean setScriptName(String v) { return fill("etName", v); }

    public String scriptNameValue() { return text("etName"); }

    public boolean pickScriptFile() { return clickIfPresent("btnPickFile"); }

    public String pickedFileName() { return text("tvFileName"); }

    public boolean saveScript()   { return clickIfPresent("btnSave"); }
    public boolean cancelScript() { return clickIfPresent("btnCancel"); }

    // ──────────────────────── Add Page dialog ────────────────────────

    public boolean addPageDialogShown() { return displayed("etSceneNumber"); }

    public String addPageTitle() { return text("tvDialogTitle"); }

    public boolean setSceneNumber(String v) { return fill("etSceneNumber", v); }

    public boolean setPageDescription(String v) { return fill("etDescription", v); }

    /** The colour swatches the page dialog offers. */
    public boolean colourChipsShown() { return displayed("colorChips"); }

    public boolean pickPageFile() { return clickIfPresent("btnPickFile"); }

    public boolean savePage()   { return clickIfPresent("btnSave"); }
    public boolean cancelPage() { return clickIfPresent("btnCancel"); }

    // ───────────────────────── system file picker ─────────────────────────

    /**
     * Picks a file from the system picker by searching for its name.
     *
     * <p>The picker's MIME filter includes {@code *&#47;*}, so images and XML show up beside
     * the PDFs — the app accepts only {@code .pdf} / {@code .fdx} and refuses the rest
     * afterwards, by extension.
     */
    /**
     * Picks a pushed fixture out of the system file picker.
     *
     * <p>Delegates to {@link #pickSystemFile(String, Duration)} rather than driving the
     * picker's own ids: the picker is DocumentsUI, a different package, so every id on it
     * resolves against the wrong prefix from here — searching for
     * {@code <app>:id/option_menu_search} finds nothing however well the picker is
     * behaving. The shared helper also backs out of the picker when the file is missing,
     * which keeps one absent fixture from taking the rest of the suite with it.
     */
    public boolean pickFromSystemPicker(String nameFragment, Duration timeout) {
        return pickSystemFile(nameFragment, timeout);
    }

    public boolean systemPickerShown() {
        try {
            return String.valueOf(driver.getCurrentPackage()).contains("documentsui");
        } catch (Exception e) { return false; }
    }

    // ───────────────────────────── helpers ─────────────────────────────

    private String childText(WebElement parent, String id) {
        try {
            List<WebElement> els = parent.findElements(byId(id));
            if (els.isEmpty()) return null;
            return safe(els.get(0));
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
