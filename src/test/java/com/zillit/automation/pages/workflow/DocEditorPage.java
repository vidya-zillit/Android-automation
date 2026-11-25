package com.zillit.automation.pages.workflow;

import com.zillit.automation.pages.common.VisiblePage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The template editor shared by Call Sheet Creation ({@code CallSheetEditorActivity}) and
 * Production Report Creation ({@code ProductionReportEditorActivity}): the Preview / Sections
 * toggle, the Sections list, the native dialogs it opens (Header "… info.", Manage Approvers,
 * Insert Section, the name dialog) and the toolbar overflow.
 *
 * <p>The <b>section editor is a WebView without debugging</b>, so it is not driven here at all;
 * the <b>Preview WebView is readable</b> through the accessibility tree (its texts are exposed as
 * plain views), which is how header values are asserted — the Header row's own summary is fixed
 * text ("Shoot day, date, day type") and never shows them.
 */
public class DocEditorPage extends VisiblePage {

    public static final String MENU_SAVE = "Save";
    public static final String MENU_SAVE_AS = "Save As";
    public static final String MENU_SAVE_TEMPLATE = "Save as Template";
    public static final String MENU_UPDATE_TEMPLATE = "Update Template";
    public static final String MENU_SEND_SIGNATURE = "Send for Signature";
    public static final String MENU_SEND_COMMENTS = "Send for Comments";

    public static final String ROW_HEADER = "Header";
    public static final String ROW_APPROVERS = "Approvers";

    public DocEditorPage(AndroidDriver driver) {
        super(driver);
    }

    public boolean isOpen(Duration timeout) { return isVisible("toggle_mode", timeout); }

    /** "New Call Sheet", "Edit Production Report", … — the toolbar's untitled TextView. */
    public String toolbarTitle() {
        try {
            for (WebElement t : driver.findElements(By.xpath("//*[@resource-id='" + appPackage()
                    + ":id/toolbar']//android.widget.TextView"))) {
                String s = text(t);
                if (s.startsWith("New ") || s.startsWith("Edit ")) return s;
            }
        } catch (WebDriverException ignored) { }
        return null;
    }

    // ───────────────────────────── mode ─────────────────────────────

    public boolean showSections() { boolean ok = tapFirst("btn_sections"); pause(800); return ok; }

    public boolean showPreview() { boolean ok = tapFirst("btn_preview"); pause(1500); return ok; }

    public boolean sectionsSelected() { return checked("btn_sections"); }

    public boolean previewSelected() { return checked("btn_preview"); }

    private boolean checked(String id) {
        List<WebElement> e = onScreen(id);
        try { return !e.isEmpty() && "true".equals(e.get(0).getAttribute("checked")); }
        catch (WebDriverException ex) { return false; }
    }

    /** Whether the Preview shows a text (substring). Preview must be the selected mode. */
    public boolean previewContains(String part, Duration timeout) {
        // The Preview WebView's accessibility tree lags behind a re-render: the screenshot shows
        // the new title while the tree still has none (seen 2026-11-23). Toggling Sections →
        // Preview rebuilds it, so poll with a toggle between looks.
        long end = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            if (textShown(part, Duration.ofSeconds(3))) return true;
            if (System.currentTimeMillis() > end) return false;
            showSections();
            showPreview();
        }
    }

    // ───────────────────────────── Sections list ─────────────────────────────

    /** One row of the Sections list. */
    public static final class Section {
        public final String title, type, summary;
        Section(String title, String type, String summary) { this.title = title; this.type = type; this.summary = summary; }
        @Override public String toString() { return title + " [" + type + "] " + summary; }
    }

    public List<Section> sections() {
        return readWholeList("recycler_sections", "tv_section_title", () -> {
            List<Section> out = new ArrayList<>();
            for (Band b : bandsOf("tv_section_title")) {
                String t = textIn(b, "tv_section_title");
                if (t != null) out.add(new Section(t, textIn(b, "tv_type_badge"), textIn(b, "tv_section_summary")));
            }
            return out;
        }, s -> s.title + "|" + s.type + "|" + s.summary);
    }

    /**
     * Whether the template's content arrived. A report opened from "Use Template" can come up
     * BLANK — only the virtual Header and Approvers rows (seen on the device 2026-11-23).
     */
    public boolean hasTemplateContent() {
        for (Section s : sections()) {
            if (!ROW_HEADER.equals(s.title) && !ROW_APPROVERS.equals(s.title)) return true;
        }
        return false;
    }

    /** Scrolls the Sections list to a row by title and taps it. */
    public boolean openSection(String title) {
        if (!scrollToRow("recycler_sections", "tv_section_title", title)) {
            // scrollToRow only looks downward — the row may be above (e.g. Header after a
            // read that ended at Approvers). Go back to the top and look again.
            for (int i = 0; i < 14; i++) scrollList("recycler_sections", false);
            if (!scrollToRow("recycler_sections", "tv_section_title", title)) return false;
        }
        for (WebElement t : elementsById("tv_section_title")) {
            if (title.equals(text(t))) {
                try { t.click(); pause(2000); return true; } catch (WebDriverException e) { return false; }
            }
        }
        return false;
    }

    /** Taps a section row's delete button (opens the "Delete Section" confirm). */
    public boolean deleteSection(String title) {
        if (!scrollToRow("recycler_sections", "tv_section_title", title)) return false;
        for (Band b : bandsOf("tv_section_title")) {
            if (!title.equals(textIn(b, "tv_section_title"))) continue;
            WebElement del = elementIn(b, "btn_delete_section");
            if (del == null) return false;
            try { del.click(); pause(1200); return true; } catch (WebDriverException e) { return false; }
        }
        return false;
    }

    public boolean tapAddSection() { return tapFirst("fab_add_section"); }

    public String addSectionLabel() { return firstText("fab_add_section"); }

    /** The Insert Section dialog's choices are built in code, without ids — matched by text. */
    public boolean pickInsertType(String label) {
        try {
            List<WebElement> e = driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().text(\"" + label + "\")"));
            if (e.isEmpty()) return false;
            e.get(0).click();
            pause(1500);
            return true;
        } catch (WebDriverException ex) { return false; }
    }

    // ───────────────────────────── Header dialog ─────────────────────────────

    public boolean openHeader() {
        if (!sectionsSelected()) showSections();
        return openSection(ROW_HEADER) && isVisible("tvDialogTitle", Duration.ofSeconds(8));
    }

    public String headerDialogTitle() { return firstText("tvDialogTitle"); }

    public String field(String id) { return firstText(id); }

    public void setField(String id, String value) {
        List<WebElement> f = onScreen(id);
        if (f.isEmpty()) return;
        try { f.get(0).clear(); if (!value.isEmpty()) f.get(0).sendKeys(value); pause(400); }
        catch (WebDriverException ignored) { }
    }

    public boolean fieldShown(String id) { return !onScreen(id).isEmpty(); }

    /**
     * Picks a day type. The list is a ListPopupWindow — its own window, which a plain
     * UiAutomator lookup does not search — so multi-window lookup is switched on for the pick.
     */
    public boolean pickDayType(String value) {
        if (!tapFirst("edit_day_type")) return false;
        pause(900);
        boolean ok = false;
        try {
            driver.setSetting("enableMultiWindows", true);
            ok = tapByBounds(value);   // a click() on another window's node does not register
        } catch (WebDriverException ignored) {
        } finally {
            try { driver.setSetting("enableMultiWindows", false); } catch (WebDriverException ignored) { }
        }
        pause(600);
        if (value.equals(field("edit_day_type"))) return true;
        // Fallback: the popup lists its rows straight under the field, 48dp each after an 8dp
        // inset (measured on the device). Default order SWD, CWD, SCWD, "+ Add new".
        int index = java.util.Arrays.asList("SWD", "CWD", "SCWD").indexOf(value);
        if (index < 0) return false;
        try {
            if (onScreen("edit_day_type").isEmpty()) return false;
            if (!ok) { driver.navigate().back(); pause(500); }
            WebElement f = onScreen("edit_day_type").get(0);
            org.openqa.selenium.Rectangle r = f.getRect();
            double dp = driver.getDisplayDensity() / 160.0;
            tapFirst("edit_day_type");
            pause(900);
            int x = r.getX() + r.getWidth() / 3;
            int y = r.getY() + r.getHeight() + (int) Math.round((8 + 24 + 48 * index) * dp);
            tapPoint(x, y);
            pause(700);
        } catch (WebDriverException ignored) { }
        return value.equals(field("edit_day_type"));
    }

    private void tapPoint(int x, int y) {
        org.openqa.selenium.interactions.PointerInput f = new org.openqa.selenium.interactions.PointerInput(
                org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        org.openqa.selenium.interactions.Sequence seq = new org.openqa.selenium.interactions.Sequence(f, 1);
        seq.addAction(f.createPointerMove(Duration.ZERO, org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, y));
        seq.addAction(f.createPointerDown(0));
        seq.addAction(f.createPointerUp(0));
        driver.perform(List.of(seq));
    }

    /** The titles on screen once the Sections list is scrolled to its end (Approvers last). */
    public List<String> lastSectionTitles() {
        scrollToRow("recycler_sections", "tv_section_title", ROW_APPROVERS);
        List<String> out = new ArrayList<>();
        for (WebElement t : elementsById("tv_section_title")) out.add(text(t));
        return out;
    }

    /** Reads the day-type choices from the open popup (multi-window lookup). */
    public List<String> dayTypeChoices() {
        List<String> out = new ArrayList<>();
        if (!tapFirst("edit_day_type")) return out;
        pause(900);
        try {
            driver.setSetting("enableMultiWindows", true);
            for (WebElement t : driver.findElements(AppiumBy.className("android.widget.TextView"))) {
                String s = text(t);
                if (s.matches("SWD|CWD|SCWD|\\+ Add new|[A-Z]{2,6}")) out.add(s);
            }
        } catch (WebDriverException ignored) {
        } finally {
            try { driver.setSetting("enableMultiWindows", false); } catch (WebDriverException ignored) { }
            try { driver.navigate().back(); } catch (WebDriverException ignored) { }
            pause(600);
        }
        return out;
    }

    /**
     * Sets a call time through the Material picker (keyboard-input mode) and returns what the
     * button then reads ("06:00" — the app stores 24-hour text). Production Report only.
     */
    public String setTime(String buttonId, int hour, int minute) {
        if (!tapFirst(buttonId)) return null;
        pause(1200);
        try {
            List<WebElement> edits = driver.findElements(AppiumBy.className("android.widget.EditText"));
            // keyboard mode: Hour then Minute; clock mode would show chips instead
            if (edits.size() >= 1) { edits.get(0).click(); edits.get(0).clear(); edits.get(0).sendKeys(String.format("%02d", hour)); }
            List<WebElement> minute2 = driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().className(\"android.widget.EditText\").instance(1)"));
            if (!minute2.isEmpty()) { minute2.get(0).click(); minute2.get(0).clear(); minute2.get(0).sendKeys(String.format("%02d", minute)); }
        } catch (WebDriverException ignored) { }
        tapFirst("material_timepicker_ok_button");
        pause(800);
        return field(buttonId);
    }

    public boolean headerDone() { boolean ok = tapFirst("btn_done"); pause(1000); return ok; }

    public boolean headerCancel() { boolean ok = tapFirst("btn_cancel"); pause(800); return ok; }

    // ───────────────────────────── Manage Approvers ─────────────────────────────

    public boolean openApprovers() {
        if (!sectionsSelected()) showSections();
        return openSection(ROW_APPROVERS) && isVisible("recycler_members", Duration.ofSeconds(10));
    }

    /** Member name → ticked. */
    public Map<String, Boolean> approverChoices() {
        Map<String, Boolean> out = new java.util.LinkedHashMap<>();
        for (Band b : bandsOf("text_member_name")) {
            WebElement box = checkboxNear(b);
            boolean on = false;
            try { on = box != null && "true".equals(box.getAttribute("checked")); } catch (WebDriverException ignored) { }
            out.put(textIn(b, "text_member_name"), on);
        }
        return out;
    }

    /** Ticks or unticks so that {@code name} ends up {@code on}. */
    public boolean setApprover(String name, boolean on) {
        for (Band b : bandsOf("text_member_name")) {
            if (!name.equals(textIn(b, "text_member_name"))) continue;
            WebElement box = checkboxNear(b);
            if (box == null) return false;
            try {
                if ("true".equals(box.getAttribute("checked")) != on) { box.click(); pause(600); }
                return true;
            } catch (WebDriverException e) { return false; }
        }
        return false;
    }

    /** The checkbox sits a few pixels ABOVE the name's top, so the name's band can miss it. */
    private WebElement checkboxNear(Band b) {
        WebElement name = elementIn(b, "text_member_name");
        if (name == null) return null;
        int y;
        try { y = name.getRect().getY() + name.getRect().getHeight() / 2; } catch (WebDriverException e) { return null; }
        WebElement best = null;
        int bestGap = Integer.MAX_VALUE;
        for (WebElement c : elementsById("checkbox_select")) {
            try {
                int cy = c.getRect().getY() + c.getRect().getHeight() / 2;
                int gap = Math.abs(cy - y);
                if (gap < bestGap) { bestGap = gap; best = c; }
            } catch (WebDriverException ignored) { }
        }
        return bestGap < 120 ? best : null;
    }

    public boolean saveApproversEnabled() {
        List<WebElement> b = onScreen("btn_save");
        try { return !b.isEmpty() && b.get(0).isEnabled(); } catch (WebDriverException e) { return false; }
    }

    public boolean saveApprovers() { boolean ok = tapFirst("btn_save"); pause(1800); return ok; }

    public boolean closeApprovers() { boolean ok = tapFirst("btn_close"); pause(800); return ok; }

    public void searchMembers(String q) {
        List<WebElement> f = onScreen("edit_search");
        if (f.isEmpty()) return;
        try { f.get(0).clear(); f.get(0).sendKeys(q); pause(1000); } catch (WebDriverException ignored) { }
    }

    // ───────────────────────────── overflow & name dialog ─────────────────────────────

    /** Opens the toolbar overflow and returns its items. */
    public List<String> overflowItems() {
        if (!tapFirst("action_more")) return new ArrayList<>();
        pause(1000);
        List<String> out = new ArrayList<>();
        try { for (WebElement e : driver.findElements(MENU_TITLE)) out.add(text(e)); } catch (WebDriverException ignored) { }
        return out;
    }

    /** Menu item titles: AppCompat menus use the APP's {@code :id/title}, platform ones android's. */
    static final By MENU_TITLE = AppiumBy.androidUIAutomator("new UiSelector().resourceIdMatches(\".*:id/title\")");

    public boolean tapOverflow(String item) {
        List<String> items = overflowItems();
        if (!items.contains(item)) {
            // the first tap on Options is sometimes swallowed while the editor settles
            closeOverflow();
            pause(1500);
            items = overflowItems();
            if (!items.contains(item)) { closeOverflow(); return false; }
        }
        try {
            for (WebElement e : driver.findElements(MENU_TITLE)) {
                if (item.equals(text(e))) { e.click(); pause(1500); return true; }
            }
        } catch (WebDriverException ignored) { }
        return false;
    }

    public void closeOverflow() {
        try {
            if (!driver.findElements(MENU_TITLE).isEmpty()) { driver.navigate().back(); pause(600); }
        } catch (WebDriverException ignored) { }
    }

    public boolean nameDialogShown(Duration timeout) { return isVisible("input_name", timeout); }

    public String nameDialogTitle() { return firstText("text_title"); }

    public String nameHint() { return firstText("input_name"); }

    public void typeName(String name) {
        List<WebElement> f = onScreen("input_name");
        if (f.isEmpty()) return;
        try { f.get(0).clear(); if (!name.isEmpty()) f.get(0).sendKeys(name); pause(500); } catch (WebDriverException ignored) { }
    }

    public String typedName() { return firstText("input_name"); }

    public boolean nameSaveEnabled() {
        List<WebElement> b = onScreen("btn_save");
        try { return !b.isEmpty() && b.get(0).isEnabled(); } catch (WebDriverException e) { return false; }
    }

    public boolean nameSave() { boolean ok = tapFirst("btn_save"); pause(1000); return ok; }

    public boolean nameCancel() { boolean ok = tapFirst("btn_cancel"); pause(800); return ok; }

    /** Toolbar navigation (up arrow) — not system back; QA CS-08 / PR-11 are about this one. */
    public boolean tapToolbarBack() {
        try {
            List<WebElement> up = driver.findElements(AppiumBy.accessibilityId("Navigate up"));
            if (up.isEmpty()) up = driver.findElements(By.xpath("//*[@resource-id='" + appPackage()
                    + ":id/toolbar']/android.widget.ImageButton"));
            if (up.isEmpty()) return false;
            up.get(0).click();
            pause(1200);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    // ───────────────────────────── platform AlertDialog (delegated) ─────────────────────────────

    public String alertTitle() { return new DocWorkflowPage(driver).alertTitle(); }

    public String alertMessage() { return new DocWorkflowPage(driver).alertMessage(); }

    /** Finds a node by exact text (any window) and taps the centre of its bounds. */
    private boolean tapByBounds(String label) {
        try {
            for (WebElement x : driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().text(\"" + label + "\")"))) {
                org.openqa.selenium.Rectangle r = x.getRect();
                if (r.getWidth() <= 0 || r.getHeight() <= 0) continue;
                org.openqa.selenium.interactions.PointerInput f = new org.openqa.selenium.interactions.PointerInput(
                        org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
                org.openqa.selenium.interactions.Sequence seq = new org.openqa.selenium.interactions.Sequence(f, 1);
                seq.addAction(f.createPointerMove(Duration.ZERO, org.openqa.selenium.interactions.PointerInput.Origin.viewport(),
                        r.getX() + r.getWidth() / 2, r.getY() + r.getHeight() / 2));
                seq.addAction(f.createPointerDown(0));
                seq.addAction(f.createPointerUp(0));
                driver.perform(List.of(seq));
                pause(700);
                return true;
            }
        } catch (WebDriverException ignored) { }
        return false;
    }

}
