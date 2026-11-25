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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The document-approval workflow shared by <b>Call Sheet Creation</b> and <b>Production Report
 * Creation</b>: Drafts · Approvals (Sent / Received / Finalized) · Published, plus the dialogs
 * both tools open from those lists. The two tools inflate the <i>same</i> layout files, so every
 * id here is valid in both; the module page objects only add their entry and create button.
 *
 * <p>Rules learned from the source and the 2026-11-23 device walk (see
 * {@code CALL_SHEET_CREATION_TESTCASES.md}):
 * <ul>
 *   <li>Rows on the Approvals sub-tabs have a <b>blank name</b> — every row is found by its
 *       <b>Serial No.</b>;</li>
 *   <li>row menus are platform PopupMenus — items are {@code :id/title} (app package for AppCompat menus), matched by text;</li>
 *   <li>confirm dialogs are platform AlertDialogs — {@code android:id/alertTitle}, {@code message},
 *       {@code button1..3};</li>
 *   <li>a badge on a tab replaces the tab's content-desc with "N new notification(s)"; the tab's
 *       TextView keeps its name;</li>
 *   <li>Received is dropped from the sub-tabs when it is empty;</li>
 *   <li>the approver's lists do not refresh by themselves after an action — pull to refresh.</li>
 * </ul>
 */
public class DocWorkflowPage extends VisiblePage {

    public static final String TAB_DRAFTS = "Drafts";
    public static final String TAB_APPROVALS = "Approvals";
    public static final String TAB_PUBLISHED = "Published";
    public static final String TAB_PERMISSION = "Permission";
    public static final String SUB_SENT = "Sent";
    public static final String SUB_RECEIVED = "Received";
    public static final String SUB_FINALIZED = "Finalized";

    public static final String ST_DRAFT = "Draft";
    public static final String ST_FOR_COMMENTS = "For Comments";
    public static final String ST_PENDING = "Pending Signature";
    public static final String ST_REJECTED = "Final Rejected";
    public static final String ST_APPROVED = "Final Approved";
    public static final String ST_PUBLISHED = "Published";

    /** The status pill — present on every row of every list, on the row's first line. */
    private static final String PILL = "badge_status";

    private static final Pattern SERIAL = Pattern.compile("Serial No\\.?:\\s*(\\S+)");

    public DocWorkflowPage(AndroidDriver driver) {
        super(driver);
    }

    // ───────────────────────────── header & tabs ─────────────────────────────

    public String header() { return firstText("headerText"); }

    public boolean isLoaded(Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < end) {
            if (!onScreen("tlTab").isEmpty() && !tabNames().isEmpty()) return true;
            pause(500);
        }
        return false;
    }

    /** Top tab names, left to right. */
    public List<String> tabNames() { return tabTexts("tlTab"); }

    public boolean openTab(String name) { return tapTab("tlTab", name); }

    public boolean tabSelected(String name) { return tabIsSelected("tlTab", name); }

    /** The content-desc of a top tab — "N new notification(s)" while it carries a badge. */
    public String tabBadge(String name) { return tabDesc("tlTab", name); }

    /** Approvals sub-tab names, left to right (Received is absent while it is empty). */
    public List<String> subTabNames() { return tabTexts("tab_layout_approvals"); }

    public boolean openSubTab(String name) { return tapTab("tab_layout_approvals", name); }

    public String subTabBadge(String name) { return tabDesc("tab_layout_approvals", name); }

    /** Waits for a sub-tab to appear — Received only shows up once it has rows (or a badge). */
    public boolean waitSubTab(String name, Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < end) {
            if (subTabNames().contains(name)) return true;
            pause(700);
        }
        return false;
    }

    private List<String> tabTexts(String layoutId) {
        List<String> out = new ArrayList<>();
        for (WebElement t : tabTextViews(layoutId)) {
            String s = text(t);
            if (!s.isEmpty()) out.add(s);
        }
        return out;
    }

    private List<WebElement> tabTextViews(String layoutId) {
        try {
            List<WebElement> all = driver.findElements(By.xpath("//*[@resource-id='" + appPackage()
                    + ":id/" + layoutId + "']//android.widget.TextView"));
            return leftToRight(all);
        } catch (WebDriverException e) {
            return new ArrayList<>();
        }
    }

    private WebElement tabView(String layoutId, String name) {
        for (WebElement t : tabTextViews(layoutId)) if (name.equals(text(t))) return t;
        return null;
    }

    private boolean tapTab(String layoutId, String name) {
        WebElement t = tabView(layoutId, name);
        if (t == null) return false;
        try { t.click(); pause(1800); return true; } catch (WebDriverException e) { return false; }
    }

    private boolean tabIsSelected(String layoutId, String name) {
        WebElement t = tabView(layoutId, name);
        try { return t != null && "true".equals(t.getAttribute("selected")); }
        catch (WebDriverException e) { return false; }
    }

    private String tabDesc(String layoutId, String name) {
        // The tab's LinearLayout carries the content-desc; it sits at the TextView's centre.
        WebElement t = tabView(layoutId, name);
        if (t == null) return null;
        int cx = centreX(t);
        try {
            for (WebElement l : driver.findElements(By.xpath("//*[@resource-id='" + appPackage()
                    + ":id/" + layoutId + "']//android.widget.LinearLayout[@content-desc]"))) {
                if (Math.abs(centreX(l) - cx) < 40) return l.getAttribute("content-desc");
            }
        } catch (WebDriverException ignored) { }
        return null;
    }

    // ───────────────────────────── Drafts ─────────────────────────────

    /** Filter chip text, e.g. "All 3". */
    public String chip(String id) { return firstText(id); }

    public boolean tapChip(String id) { return tapFirst(id); }

    /** The number at the end of a chip, or -1. */
    public int chipCount(String id) {
        String t = chip(id);
        if (t == null) return -1;
        Matcher m = Pattern.compile("(\\d+)\\s*$").matcher(t);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    public boolean draftsEmptyShown() {
        String t = firstText("text_empty");
        return t != null && t.startsWith("No drafts");
    }

    public String emptyText() { return firstText("text_empty"); }

    /** One list row, whichever tab it came from. Name is blank on the Approvals sub-tabs. */
    public static final class Row {
        public final String name, status, serial, shootDay, createdBy, dates, stage, approvals;
        Row(String name, String status, String serial, String shootDay, String createdBy,
            String dates, String stage, String approvals) {
            this.name = name; this.status = status; this.serial = serial; this.shootDay = shootDay;
            this.createdBy = createdBy; this.dates = dates; this.stage = stage; this.approvals = approvals;
        }
        @Override public String toString() {
            return "[#" + serial + " '" + name + "' " + status + (stage == null ? "" : " " + stage)
                    + (approvals == null ? "" : " " + approvals) + "]";
        }
    }

    /** Which list is on screen — they differ only in a few ids. */
    public enum ListKind {
        DRAFTS("recycler_drafts", "text_draft_id", "text_draft_name", "createdBy"),
        SENT("recycler_approvals", "text_sent_id", "text_sent_name", "text_created_by"),
        RECEIVED("recycler_approvals", "text_id", "text_approval_name", "text_created_by"),
        FINALIZED("recycler_approvals", "text_id", "text_finalized_name", "text_created_by");

        final String list, anchor, name, createdBy;
        ListKind(String list, String anchor, String name, String createdBy) {
            this.list = list; this.anchor = anchor; this.name = name; this.createdBy = createdBy;
        }
    }

    /** Every row of the list on screen, followed past the fold. */
    public List<Row> rows(ListKind kind) {
        return readWholeList(kind.list, kind.anchor, () -> windowRows(kind), r -> r.serial);
    }

    private List<Row> windowRows(ListKind kind) {
        List<Row> out = new ArrayList<>();
        // Sliced by the status pill: it is on the row's TOP line, with the name; the serial
        // line is below them, so a band started there would miss both.
        for (Band b : bandsOf(PILL)) {
            String serial = serialOf(textIn(b, kind.anchor));
            if (serial == null) continue;
            String status = textIn(b, "text_status_badge");
            if (status == null || status.isEmpty()) status = textIn(b, "badge_status");
            out.add(new Row(nz(textIn(b, kind.name)), status, serial, textIn(b, "text_shoot_day"),
                    textIn(b, kind.createdBy), textIn(b, "text_dates"), textIn(b, "badge_stage"),
                    textIn(b, "text_approval_count")));
        }
        return out;
    }

    public Row row(ListKind kind, String serial) {
        for (Row r : rows(kind)) if (serial.equals(r.serial)) return r;
        return null;
    }

    public Row rowNamed(String name) {
        for (Row r : rows(ListKind.DRAFTS)) if (name.equals(r.name)) return r;
        return null;
    }

    /** Waits for a row with this serial to appear (pulling to refresh between looks). */
    public Row waitRow(ListKind kind, String serial, Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            Row r = row(kind, serial);
            if (r != null || System.currentTimeMillis() > end) return r;
            refresh(kind);
            pause(1500);
        }
    }

    /** Waits for the row to be gone (pulling to refresh between looks). */
    public boolean waitRowGone(ListKind kind, String serial, Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < end) {
            if (row(kind, serial) == null) return true;
            refresh(kind);
            pause(1500);
        }
        return row(kind, serial) == null;
    }

    /** Waits for the row to reach a status, refreshing between looks. */
    public Row waitStatus(ListKind kind, String serial, String status, Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        Row r = null;
        while (System.currentTimeMillis() < end) {
            r = row(kind, serial);
            if (r != null && status.equals(r.status)) return r;
            refresh(kind);
            pause(1500);
        }
        return r;
    }

    /** Pull to refresh on the visible list. */
    public void refresh(ListKind kind) {
        List<WebElement> c = onScreen("swipe_refresh");
        if (c.isEmpty()) c = onScreen(kind.list);
        if (c.isEmpty()) return;
        try {
            org.openqa.selenium.Rectangle box = c.get(0).getRect();
            int x = box.getX() + box.getWidth() / 2;
            swipe(x, box.getY() + box.getHeight() / 6, x, box.getY() + box.getHeight() * 3 / 4);
            pause(2500);
        } catch (WebDriverException ignored) { }
    }

    /** Opens a row's ⋮ menu and returns the items offered (empty when the row is not found). */
    public List<String> openRowMenu(ListKind kind, String serial) {
        if (!scrollToRow(kind.list, kind.anchor, "Serial No.: " + serial)) return new ArrayList<>();
        for (Band b : bandsOf(PILL)) {
            if (!serial.equals(serialOf(textIn(b, kind.anchor)))) continue;
            WebElement btn = elementIn(b, "btn_actions");
            if (btn == null) return new ArrayList<>();
            try { btn.click(); } catch (WebDriverException e) { return new ArrayList<>(); }
            pause(1200);
            return menuTitles();
        }
        return new ArrayList<>();
    }

    /** Taps a PopupMenu item by its exact text. */
    public boolean tapPopupItem(String label) {
        try {
            for (WebElement e : driver.findElements(DocEditorPage.MENU_TITLE)) {
                if (label.equals(text(e))) { e.click(); pause(1500); return true; }
            }
        } catch (WebDriverException ignored) { }
        return false;
    }

    /** Items of the open PopupMenu, in order (app- or android-package {@code :id/title}). */
    public List<String> menuTitles() {
        List<String> out = new ArrayList<>();
        try { for (WebElement e : driver.findElements(DocEditorPage.MENU_TITLE)) out.add(text(e)); }
        catch (WebDriverException ignored) { }
        return out;
    }

    /** Opens the row's menu and taps an item; false if either step fails. */
    public boolean rowAction(ListKind kind, String serial, String item) {
        List<String> items = openRowMenu(kind, serial);
        if (!items.contains(item)) { closePopup(); return false; }
        return tapPopupItem(item);
    }

    public void closePopup() {
        try {
            if (!driver.findElements(DocEditorPage.MENU_TITLE).isEmpty()) {
                driver.navigate().back();
                pause(700);
            }
        } catch (WebDriverException ignored) { }
    }

    // ───────────────────────────── template picker ─────────────────────────────

    public boolean templatePickerShown(Duration timeout) {
        return isVisible("tplConfirm", timeout);
    }

    public String pickerTitle() { return firstText("tplTitle"); }

    public List<String> templateOptions() {
        List<String> out = new ArrayList<>();
        try {
            for (WebElement c : driver.findElements(By.xpath("//*[@resource-id='" + appPackage()
                    + ":id/tplOptions']//*[@text]"))) {
                String s = text(c);
                if (!s.isEmpty()) out.add(s);
            }
        } catch (WebDriverException ignored) { }
        return out;
    }

    public String confirmLabel() { return firstText("tplConfirm"); }

    /** Picks a template option chip by its label ("Template 2"). */
    public boolean selectTemplate(String label) {
        try {
            for (WebElement c : driver.findElements(By.xpath("//*[@resource-id='" + appPackage()
                    + ":id/tplOptions']//*[@text='" + label + "']"))) {
                c.click(); pause(1200); return true;
            }
        } catch (WebDriverException ignored) { }
        return false;
    }

    // ───────────────────────────── Published tab ─────────────────────────────

    /** The "Currently Published" card is up. */
    public boolean publishedCardShown() { return !onScreen("card_latest").isEmpty(); }

    /** Waits for the Published tab to show either its card or its empty text; returns which ("card" / the text / null). */
    public String publishedState(Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < end) {
            if (publishedCardShown()) return "card";
            String e = firstText("text_empty");
            if (e != null && !e.isEmpty()) return e;
            pause(600);
        }
        return null;
    }

    public boolean confirmTemplate() { return tapFirst("tplConfirm"); }

    public boolean cancelTemplatePicker() { return tapFirst("tplCancel"); }

    // ───────────────────────────── platform AlertDialog ─────────────────────────────

    // AppCompat's AlertDialog puts alertTitle in the APP's package and message/button1..3 in
    // android's — match on the id's tail so either lands.
    public String alertTitle() { return androidText("alertTitle"); }

    public String alertMessage() { return androidText("message"); }

    public boolean alertShown(Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < end) {
            if (alertTitle() != null) return true;
            pause(300);
        }
        return false;
    }

    /** Buttons of the AlertDialog, by their text (e.g. "Cancel", "Discard", "Save &amp; Leave"). */
    public List<String> alertButtons() {
        List<String> out = new ArrayList<>();
        for (String id : new String[]{"button3", "button2", "button1"}) {
            String t = androidText(id);
            if (t != null && !t.isEmpty()) out.add(t);
        }
        return out;
    }

    public boolean tapAlertButton(String label) {
        for (String id : new String[]{"button1", "button2", "button3"}) {
            try {
                for (WebElement b : driver.findElements(tail(id))) {
                    if (label.equalsIgnoreCase(text(b))) { b.click(); pause(1500); return true; }
                }
            } catch (WebDriverException ignored) { }
        }
        return false;
    }

    private String androidText(String idTail) {
        try {
            List<WebElement> e = driver.findElements(tail(idTail));
            return e.isEmpty() ? null : text(e.get(0));
        } catch (WebDriverException ex) { return null; }
    }

    /** Any package's {@code :id/<tail>} — exact tail, so "message" does not match "messageTxt". */
    private static By tail(String idTail) {
        return AppiumBy.androidUIAutomator("new UiSelector().resourceIdMatches(\".*:id/" + idTail + "\")");
    }

    // ───────────────────────────── History / Comments dialogs ─────────────────────────────

    /** One History row: action, stage/revision badge, "Name (Role)", time, reason. */
    public static final class HistoryEntry {
        public final String action, stage, revision, by, time, reason;
        HistoryEntry(String action, String stage, String revision, String by, String time, String reason) {
            this.action = action; this.stage = stage; this.revision = revision;
            this.by = by; this.time = time; this.reason = reason;
        }
        @Override public String toString() { return action + " by " + by + (reason == null ? "" : " — " + reason); }
    }

    public boolean historyShown(Duration timeout) { return isVisible("recycler_revisions", timeout)
            || isVisible("text_empty", Duration.ofSeconds(1)); }

    public List<HistoryEntry> historyEntries() {
        // the rows arrive after the dialog opens — wait for the first one (or the empty text)
        long end = System.currentTimeMillis() + 15000;
        while (elementsById("text_action").isEmpty() && onScreen("text_empty").isEmpty()
                && System.currentTimeMillis() < end) pause(500);
        return readWholeList("recycler_revisions", "text_action", () -> {
            List<HistoryEntry> out = new ArrayList<>();
            for (Band b : bandsOf("text_action")) {
                out.add(new HistoryEntry(textIn(b, "text_action"), textIn(b, "badge_stage"),
                        textIn(b, "badge_revision"), textIn(b, "text_by"), textIn(b, "text_time"),
                        textIn(b, "text_reason")));
            }
            return out;
        }, h -> h.action + "|" + h.time + "|" + h.by);
    }

    /** Closes History / Comments / Reminders (all use {@code btn_close}). */
    public boolean closeDialog() { return tapFirst("btn_close"); }

    // ───────────────────────────── recipients picker (Send for Comments) ─────────────────────────────

    public boolean recipientsShown(Duration timeout) { return isVisible("recycler_approvers", timeout); }

    public String recipientsTitle() { return firstText("text_toolbar_title"); }

    public String selectionCount() { return firstText("text_selection_count"); }

    public String sendLabel() { return firstText("btn_send"); }

    public boolean sendEnabled() {
        List<WebElement> b = onScreen("btn_send");
        try { return !b.isEmpty() && b.get(0).isEnabled(); } catch (WebDriverException e) { return false; }
    }

    public List<String> recipientNames() {
        List<String> out = new ArrayList<>();
        for (WebElement e : elementsById("text_member_name")) out.add(text(e));
        return out;
    }

    /** Ticks / unticks a recipient by name (row checkbox). */
    public boolean toggleRecipient(String name) {
        for (Band b : bandsOf("text_member_name")) {
            if (!name.equals(textIn(b, "text_member_name"))) continue;
            WebElement box = elementIn(b, "checkbox_select");
            if (box == null) {
                // the checkbox sits on the same line, slightly ABOVE the name — tap the name instead
                box = elementIn(b, "text_member_name");
            }
            try { box.click(); pause(700); return true; } catch (WebDriverException e) { return false; }
        }
        return false;
    }

    public boolean tapSelectAll() { return tapFirst("btn_select_all"); }

    public String selectAllLabel() { return firstText("btn_select_all"); }

    public void searchRecipients(String q) {
        List<WebElement> f = onScreen("edit_search");
        if (f.isEmpty()) return;
        try { f.get(0).clear(); f.get(0).sendKeys(q); pause(1200); } catch (WebDriverException ignored) { }
    }

    public boolean clearRecipientSearch() { return tapFirst("btn_clear_search"); }

    public boolean closeRecipients() { return tapFirst("btn_back"); }

    public boolean sendToRecipients() { return tapFirst("btn_send"); }

    // ───────────────────────────── reject / approve / reminder ─────────────────────────────

    public boolean rejectDialogShown(Duration timeout) { return isVisible("edit_rejection_reason", timeout); }

    public void typeRejectReason(String reason) {
        List<WebElement> f = onScreen("edit_rejection_reason");
        if (f.isEmpty()) return;
        try { f.get(0).clear(); if (!reason.isEmpty()) f.get(0).sendKeys(reason); } catch (WebDriverException ignored) { }
    }

    public boolean tapReject() { return tapFirst("btn_reject"); }

    /** The TextInputLayout error under the reason, e.g. "Please provide a rejection reason". */
    public String rejectError() {
        try {
            List<WebElement> e = driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceIdMatches(\".*:id/textinput_error\")"));
            return e.isEmpty() ? null : text(e.get(0));
        } catch (WebDriverException ex) { return null; }
    }

    public boolean cancelDialogButton() { return tapFirst("btn_cancel"); }

    public boolean approveDialogShown(Duration timeout) { return isVisible("text_approver_name", timeout); }

    public String approveTitle() { return firstText("text_title"); }

    public String approverName() { return firstText("text_approver_name"); }

    /** The two cards have no ids worth using — they are found by their own label. */
    public boolean approveWithoutSignature() { return tapText("Approve Without Signature"); }

    public boolean reminderDialogShown(Duration timeout) { return isVisible("input_message", timeout); }

    public String reminderMessage() { return firstText("input_message"); }

    // ───────────────────────────── helpers ─────────────────────────────

    public static String serialOf(String s) {
        if (s == null) return null;
        Matcher m = SERIAL.matcher(s);
        return m.find() ? m.group(1) : null;
    }

    protected boolean tapText(String label) {
        try {
            List<WebElement> e = driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().text(\"" + label.replace("\"", "\\\"") + "\")"));
            if (e.isEmpty()) return false;
            e.get(0).click();
            pause(1500);
            return true;
        } catch (WebDriverException ex) { return false; }
    }

    protected void swipe(int x1, int y1, int x2, int y2) {
        org.openqa.selenium.interactions.PointerInput f = new org.openqa.selenium.interactions.PointerInput(
                org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        org.openqa.selenium.interactions.Sequence s = new org.openqa.selenium.interactions.Sequence(f, 1);
        s.addAction(f.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x1, y1));
        s.addAction(f.createPointerDown(0));
        s.addAction(f.createPointerMove(Duration.ofMillis(450),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x2, y2));
        s.addAction(f.createPointerUp(0));
        try { driver.perform(List.of(s)); } catch (WebDriverException ignored) { }
    }

    private static String nz(String s) { return s == null ? "" : s; }
}
