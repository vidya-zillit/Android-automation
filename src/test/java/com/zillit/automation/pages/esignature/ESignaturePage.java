package com.zillit.automation.pages.esignature;

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
 * Tools → "Z-E-Signature" (identifier {@code e_signature_tool}): {@code DocSignMainActivity},
 * a single-activity nav graph whose start destination is {@code DocuSignEntryFragment}.
 *
 * <p>What the source says (see {@code E_SIGNATURE_TESTCASES.md}):
 * <ul>
 *   <li><b>Entry routes by posting rights.</b> A poster sees the admin shell
 *       ({@code dsEntryAdminGroup}) with four segment chips — "Manage Envelopes",
 *       "Sign Documents", "Templates", "Bulk Sends" — a "Signatures" button and the
 *       "Upload Doc for Z-E-Signature" FAB ({@code btnUploadDocFab}, Manage only). A viewer is
 *       sent straight to the received inbox.</li>
 *   <li><b>Manage Envelopes</b> = {@code DocuSignListFragment}: top tabs "Z-E-Signature" /
 *       "Complete" / "Rejected", sub-tabs "Sent" / "Draft", Sent filter chips, search
 *       {@code etSearch}, list/grid toggle, rows {@code tvTitle} + {@code tvStatusChip} +
 *       kebab {@code ivOverflow} (PopupMenu "View" [+ "Edit", "Delete" on a DRAFT]).</li>
 *   <li><b>Only drafts can be deleted.</b> There is no void / delete for a sent envelope on
 *       Android, which is why the suite never sends one.</li>
 *   <li><b>"Signatures"</b> opens {@code DocuSignPreSignGateDialog} in manage mode ("My Saved
 *       Signatures"): two carousels of dynamic tiles WITHOUT ids, a "Draw New" tile each,
 *       long-press a tile → "Delete saved signature?".</li>
 * </ul>
 */
public class ESignaturePage extends VisiblePage {

    public static final String TOOL_TILE = "Z-E-Signature";

    public static final String SEG_MANAGE = "Manage Envelopes";
    public static final String SEG_SIGN = "Sign Documents";
    public static final String SEG_TEMPLATES = "Templates";
    public static final String SEG_BULK = "Bulk Sends";
    public static final String FAB_UPLOAD = "Upload Doc for Z-E-Signature";

    public static final String TOP_ESIGN = "Z-E-Signature";
    public static final String TOP_COMPLETE = "Complete";
    public static final String TOP_REJECTED = "Rejected";
    public static final String SUB_SENT = "Sent";
    public static final String SUB_DRAFT = "Draft";

    public static final String EMPTY_DRAFTS = "No draft envelopes. Tap + to create one.";
    public static final String EMPTY_SENT = "No sent envelopes";
    public static final String EMPTY_COMPLETE = "No completed envelopes yet";
    public static final String EMPTY_REJECTED = "No rejected envelopes";

    public static final String MANAGE_SHEET_TITLE = "My Saved Signatures";
    public static final String DRAW_NEW = "Draw New";
    public static final String DELETE_SIG_TITLE = "Delete saved signature?";
    public static final String DELETE_DRAFT_TITLE = "Delete this draft?";

    public ESignaturePage(AndroidDriver driver) {
        super(driver);
    }

    // ─────────────────────────── entry shell ───────────────────────────

    /** The admin shell (segment chips) is up. */
    public boolean isLoaded(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (!onScreen("dsEntryChipManage").isEmpty()) return true;
            pause(400);
        }
        return false;
    }

    /** A viewer lands on the received inbox instead of the admin shell. */
    public boolean isReceivedOnly() {
        return onScreen("dsEntryChipManage").isEmpty() && !onScreen("tabAction").isEmpty();
    }

    /**
     * The four segment labels. The chip row is a HorizontalScrollView and on a 1080-px screen
     * "Bulk Sends" sits past the edge (only its first ~70 px show, and its label is not in the
     * tree), so the row is swiped left to read it and swiped back.
     */
    public List<String> segmentLabels() {
        List<String> out = new ArrayList<>();
        String[] ids = {"dsEntryChipManageLabel", "dsEntryChipSignLabel",
                "dsEntryChipTemplatesLabel", "dsEntryChipBulkLabel"};
        for (String id : ids) {
            String t = firstText(id);
            if (t != null && !out.contains(t)) out.add(t);
        }
        if (out.size() < ids.length) {
            List<WebElement> manage = onScreen("dsEntryChipManage");
            if (!manage.isEmpty()) {
                try {
                    var r = manage.get(0).getRect();
                    int w = driver.manage().window().getSize().getWidth();
                    int y = r.getY() + r.getHeight() / 2;
                    swipe((int) (w * 0.85), y, (int) (w * 0.15), y);
                    for (String id : ids) {
                        String t = firstText(id);
                        if (t != null && !out.contains(t)) out.add(t);
                    }
                    swipe((int) (w * 0.15), y, (int) (w * 0.85), y);
                } catch (WebDriverException ignored) { }
            }
        }
        return out;
    }

    public boolean openSegment(String label) {
        String id = switch (label) {
            case SEG_MANAGE -> "dsEntryChipManage";
            case SEG_SIGN -> "dsEntryChipSign";
            case SEG_TEMPLATES -> "dsEntryChipTemplates";
            case SEG_BULK -> "dsEntryChipBulk";
            default -> null;
        };
        if (id == null) return false;
        boolean ok = tapFirst(id);
        pause(1200);
        return ok;
    }

    public boolean segmentSelected(String label) {
        String id = switch (label) {
            case SEG_MANAGE -> "dsEntryChipManage";
            case SEG_SIGN -> "dsEntryChipSign";
            case SEG_TEMPLATES -> "dsEntryChipTemplates";
            case SEG_BULK -> "dsEntryChipBulk";
            default -> null;
        };
        List<WebElement> e = id == null ? List.of() : onScreen(id);
        try { return !e.isEmpty() && "true".equals(e.get(0).getAttribute("selected")); }
        catch (WebDriverException ex) { return false; }
    }

    public boolean uploadFabShown() {
        return !onScreen("btnUploadDocFab").isEmpty();
    }

    public boolean tapUploadFab() {
        return tapFirst("btnUploadDocFab");
    }

    public boolean tapBack() {
        return tapFirst("btnBack");
    }

    // ─────────────────────── Manage Envelopes list ───────────────────────

    public boolean listShown(Duration timeout) {
        return isVisible("topTabESignature", timeout);
    }

    public boolean openTopTab(String label) {
        String id = switch (label) {
            case TOP_ESIGN -> "topTabESignature";
            case TOP_COMPLETE -> "topTabComplete";
            case TOP_REJECTED -> "topTabRejected";
            default -> null;
        };
        boolean ok = id != null && tapFirst(id);
        waitListSettled();
        return ok;
    }

    public boolean openSubTab(String label) {
        String id = SUB_SENT.equals(label) ? "subTabSent" : SUB_DRAFT.equals(label) ? "subTabDraft" : null;
        boolean ok = id != null && tapFirst(id);
        waitListSettled();
        return ok;
    }

    /** Whether the Sent/Draft sub-tab bar is on screen (only under the Z-E-Signature top tab). */
    public boolean subTabsShown() {
        return !onScreen("subTabSent").isEmpty() && !onScreen("subTabDraft").isEmpty();
    }

    public boolean sentChipsShown() {
        return !onScreen("chipSentAll").isEmpty();
    }

    public List<String> sentChipLabels() {
        List<String> out = new ArrayList<>();
        for (String id : new String[]{"tvChipAllLabel", "tvChipAwaitingLabel", "tvChipInProgressLabel"}) {
            String t = firstText(id);
            if (t != null) out.add(t);
        }
        return out;
    }

    public void waitListSettled() {
        long deadline = System.currentTimeMillis() + 15_000;
        pause(600);
        while (System.currentTimeMillis() < deadline) {
            if (onScreen("progressBar").isEmpty()) break;
            pause(400);
        }
        pause(500);
    }

    /** Titles of the envelope rows on screen ({@code tvTitle} inside {@code rvEnvelopes}). */
    public List<String> envelopeTitles() {
        List<String> out = new ArrayList<>();
        for (WebElement e : onScreen("tvTitle")) out.add(text(e));
        return out;
    }

    public boolean emptyStateShown() {
        return !onScreen("emptyState").isEmpty() || !onScreen("tvEmpty").isEmpty();
    }

    public String emptyText() {
        return firstText("tvEmpty");
    }

    public void search(String q) {
        List<WebElement> f = onScreen("etSearch");
        if (f.isEmpty()) return;
        try {
            f.get(0).click();
            f.get(0).clear();
            if (q != null && !q.isEmpty()) f.get(0).sendKeys(q);
        } catch (WebDriverException ignored) { }
        pause(1200);
        hideKeyboardIfShown();
    }

    public void clearSearch() { search(""); }

    public boolean searchBoxShown() { return !onScreen("etSearch").isEmpty(); }

    public String searchText() { return firstText("etSearch"); }

    public boolean toggleViewMode() {
        return tapFirst("btnViewModeToggle");
    }

    /** Grid cards carry their own Send/Delete buttons; list rows carry the kebab. */
    public boolean isGridMode() {
        return onScreen("ivOverflow").isEmpty() && !envelopeTitles().isEmpty();
    }

    /** Finds a row whose title contains {@code part}, scrolling the list. */
    public WebElement findRow(String part) {
        for (int pass = 0; pass < 6; pass++) {
            for (WebElement t : onScreen("tvTitle")) {
                if (text(t).contains(part)) return t;
            }
            if (!scrollList("rvEnvelopes", true)) break;
        }
        return null;
    }

    public boolean rowShown(String part, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            for (WebElement t : onScreen("tvTitle")) if (text(t).contains(part)) return true;
            pause(700);
        }
        return findRow(part) != null;
    }

    /** Status chip text in the same row as the title containing {@code part}. */
    public String statusOf(String part) {
        WebElement t = findRow(part);
        if (t == null) return null;
        Band b = bandOfAnchor("tvTitle", t);
        return b == null ? null : textIn(b, "tvStatusChip");
    }

    /** Opens the kebab PopupMenu on the row; returns its items. */
    public List<String> openRowMenu(String part) {
        WebElement t = findRow(part);
        if (t == null) return List.of();
        Band b = bandOfAnchor("tvTitle", t);
        WebElement k = b == null ? null : elementIn(b, "ivOverflow");
        if (k == null) return List.of();
        try { k.click(); } catch (WebDriverException e) { return List.of(); }
        pause(1000);
        return popupItems();
    }

    /**
     * Rows of the open PopupMenu. These are {@code androidx.appcompat.widget.PopupMenu}s, whose
     * rows use the APP's {@code :id/title} — not {@code android:id/title} as a platform
     * PopupMenu does (so {@link #popupMenuTitles()} reads nothing here). Matches both.
     */
    public List<String> popupItems() {
        List<String> out = new ArrayList<>();
        for (WebElement e : elementsByAnyId("title")) {
            String t = text(e);
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    /** BACK closes an open PopupMenu; does nothing when none is open. */
    public void closePopup() {
        if (popupItems().isEmpty()) return;
        try { driver.navigate().back(); } catch (WebDriverException ignored) { }
        pause(800);
    }

    public boolean tapPopupItem(String label) {
        for (WebElement e : elementsByAnyId("title")) {
            if (label.equals(text(e))) {
                try { e.click(); pause(1500); return true; } catch (WebDriverException ignored) { }
            }
        }
        return false;
    }

    public boolean tapRow(String part) {
        WebElement t = findRow(part);
        if (t == null) return false;
        try { t.click(); pause(2000); return true; }
        catch (WebDriverException e) { return false; }
    }

    // ── framework AlertDialog (delete draft / delete saved signature / tooltip) ──

    public boolean alertShown(String title, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            for (WebElement e : elementsByAnyId("alertTitle")) if (title.equals(text(e))) return true;
            pause(300);
        }
        return false;
    }

    public String alertTitle() {
        List<WebElement> e = elementsByAnyId("alertTitle");
        return e.isEmpty() ? null : text(e.get(0));
    }

    public String alertMessage() {
        List<WebElement> e = elementsByAnyId("message");
        return e.isEmpty() ? null : text(e.get(0));
    }

    /** button1 = positive, button2 = negative. */
    public boolean tapAlertButton(String label) {
        for (String id : new String[]{"button1", "button2", "button3"}) {
            for (WebElement b : elementsByAnyId(id)) {
                if (label.equalsIgnoreCase(text(b))) {
                    try { b.click(); pause(1500); return true; } catch (WebDriverException ignored) { }
                }
            }
        }
        return false;
    }

    /** Ids from EITHER package — a framework AlertDialog uses android:id/…, AppCompat the app's. */
    protected List<WebElement> elementsByAnyId(String id) {
        try {
            return driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceIdMatches(\".*:id/" + id + "\")"));
        } catch (WebDriverException e) {
            return List.of();
        }
    }

    /** Deletes a DRAFT through its kebab → Delete → confirm. */
    public boolean deleteDraft(String part) {
        List<String> items = openRowMenu(part);
        if (!items.contains("Delete")) { closePopup(); return false; }
        if (!tapPopupItem("Delete")) return false;
        if (!alertShown(DELETE_DRAFT_TITLE, Duration.ofSeconds(5))) return false;
        if (!tapAlertButton("Delete")) return false;
        waitListSettled();
        return true;
    }

    // ─────────────────────── Sign Documents / Templates / Bulk ───────────────────────

    public boolean receivedTabsShown(Duration timeout) {
        return isVisible("tabAction", timeout) && !onScreen("tabComplete").isEmpty();
    }

    public List<String> receivedTabLabels() {
        List<String> out = new ArrayList<>();
        for (String id : new String[]{"tabAction", "tabComplete", "tabRejected"}) {
            String t = firstText(id);
            if (t != null) out.add(t);
        }
        return out;
    }

    public boolean openReceivedTab(String id) {
        boolean ok = tapFirst(id);
        pause(1500);
        return ok;
    }

    public String receivedEmptyTitle() { return firstText("tvEmptyTitle"); }

    public boolean templatesShown(Duration timeout) {
        return isVisible("btnNewTemplate", timeout) || isVisible("rvTemplates", Duration.ofSeconds(2));
    }

    public boolean tapNewTemplate() { return tapFirst("btnNewTemplate"); }

    /** Template card names ({@code tvName}) on screen. */
    public List<String> templateNames() {
        List<String> out = new ArrayList<>();
        for (WebElement e : onScreen("tvName")) out.add(text(e));
        return out;
    }

    public boolean templateShown(String name, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (templateNames().contains(name)) return true;
            pause(700);
        }
        return false;
    }

    /**
     * The control with {@code id} that belongs to the card titled {@code name}. Cards sit in a
     * grid (two per row), so a horizontal band is not enough: take the nearest such control
     * below the name, by distance.
     */
    private WebElement cardControl(String name, String id) {
        WebElement title = null;
        for (WebElement e : onScreen("tvName")) if (name.equals(text(e))) { title = e; break; }
        if (title == null) return null;
        int tx = centreX(title), ty = title.getRect().getY();
        WebElement best = null;
        long bestD = Long.MAX_VALUE;
        for (WebElement c : onScreen(id)) {
            int cy = c.getRect().getY();
            if (cy < ty - 20) continue;
            long dx = centreX(c) - tx, dy = cy - ty;
            long d = dx * dx + dy * dy;
            if (d < bestD) { bestD = d; best = c; }
        }
        return best;
    }

    public boolean viewTemplate(String name) {
        WebElement v = cardControl(name, "btnView");
        if (v == null) return false;
        try { v.click(); pause(1500); return true; } catch (WebDriverException e) { return false; }
    }

    public List<String> openTemplateMenu(String name) {
        WebElement m = cardControl(name, "btnMore");
        if (m == null) return List.of();
        try { m.click(); } catch (WebDriverException e) { return List.of(); }
        pause(1000);
        return popupItems();
    }

    public boolean templateDetailShown(Duration timeout) {
        return isVisible("tvDetailName", timeout);
    }

    public String templateDetailName() { return firstText("tvDetailName"); }
    public String templateDetailFieldsLabel() { return firstText("tvDetailFieldsLabel"); }

    /**
     * The detail sheet rendered the template's document: {@code DocuSignTemplateDetailSheet}
     * makes {@code tvDetailPageLabel} ("Page 1 of n") visible only after the PDF downloaded
     * and rasterised; {@code tvDetailDocError} is the failure. The pager can sit below the
     * fold of the sheet, so the sheet is swiped up once if neither is found.
     */
    public boolean templateDocumentRendered(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        boolean scrolled = false;
        while (System.currentTimeMillis() < deadline) {
            if (!elementsByAnyId("tvDetailDocError").isEmpty()) return false;
            for (WebElement l : elementsByAnyId("tvDetailPageLabel")) {
                if (text(l).matches("Page \\d+ of \\d+")) return true;
            }
            if (!scrolled && System.currentTimeMillis() > deadline - timeout.toMillis() / 2) {
                try {
                    var size = driver.manage().window().getSize();
                    swipe(size.getWidth() / 2, (int) (size.getHeight() * 0.8), size.getWidth() / 2, (int) (size.getHeight() * 0.45));
                } catch (WebDriverException ignored) { }
                scrolled = true;
            }
            pause(700);
        }
        return false;
    }

    public String templateDocError() {
        List<WebElement> e = elementsByAnyId("tvDetailDocError");
        return e.isEmpty() ? null : text(e.get(0));
    }

    public boolean closeTemplateDetail() {
        if (tapFirst("btnDetailCloseFooter")) return true;
        return tapFirst("btnDetailClose");
    }

    /** ⋮ → Delete → "Delete template?" → Delete. */
    public boolean deleteTemplate(String name) {
        List<String> items = openTemplateMenu(name);
        if (!items.contains("Delete")) { closePopup(); return false; }
        if (!tapPopupItem("Delete")) return false;
        if (!alertShown("Delete template?", Duration.ofSeconds(5))) return false;
        if (!tapAlertButton("Delete")) return false;
        pause(2000);
        return true;
    }

    public boolean bulkSendsShown(Duration timeout) {
        return isVisible("rvJobs", timeout) || isVisible("btnViewList", Duration.ofSeconds(2));
    }

    // ─────────────────────── My Saved Signatures sheet ───────────────────────

    public boolean openSignaturesSheet() {
        if (!tapFirst("btnManageSignatures")) return false;
        return signaturesSheetShown(Duration.ofSeconds(8));
    }

    public boolean signaturesSheetShown(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            String t = firstText("tvGateTitle");
            if (MANAGE_SHEET_TITLE.equals(t)) return true;
            pause(400);
        }
        return false;
    }

    public String sheetSubtitle() { return firstText("tvGateSubtitle"); }
    public String sheetButton() { return firstText("btnContinue"); }
    public String signatureCountLabel() { return firstText("tvSignatureCount"); }
    public String initialCountLabel() { return firstText("tvInitialCount"); }

    /** "N / CAP" → N, or -1. */
    public static int countOf(String label) {
        if (label == null) return -1;
        String[] p = label.split("/");
        try { return Integer.parseInt(p[0].trim()); } catch (Exception e) { return -1; }
    }

    /** "N / CAP" → CAP, or -1. */
    public static int capOf(String label) {
        if (label == null) return -1;
        String[] p = label.split("/");
        try { return Integer.parseInt(p[1].trim()); } catch (Exception e) { return -1; }
    }

    /** Taps the "Draw New" tile of the signatures (row 0) or initials (row 1) carousel. */
    public boolean tapDrawNew(boolean initials) {
        String rowId = initials ? "rowInitials" : "rowSignatures";
        for (int i = 0; i < 4; i++) {
            List<WebElement> rows = onScreen(rowId);
            if (!rows.isEmpty()) {
                try {
                    List<WebElement> d = rows.get(0).findElements(AppiumBy.androidUIAutomator(
                            "new UiSelector().text(\"" + DRAW_NEW + "\")"));
                    if (!d.isEmpty()) { d.get(0).click(); pause(1500); return true; }
                } catch (WebDriverException ignored) { }
                // The tile sits at the END of a horizontal carousel: swipe it left.
                swipeLeft(rows.get(0));
            } else {
                scrollSheetDown();
            }
        }
        return false;
    }

    /** Labels of the saved tiles in a carousel (excludes the Draw New tile). */
    public List<String> savedTileLabels(boolean initials) {
        List<String> out = new ArrayList<>();
        List<WebElement> rows = onScreen(initials ? "rowInitials" : "rowSignatures");
        if (rows.isEmpty()) return out;
        try {
            for (WebElement t : rows.get(0).findElements(By.className("android.widget.TextView"))) {
                String s = text(t);
                if (!s.isEmpty() && !DRAW_NEW.equals(s) && !"+".equals(s)) out.add(s);
            }
        } catch (WebDriverException ignored) { }
        return out;
    }

    /** Long-presses the first saved tile of a carousel (opens "Delete saved signature?"). */
    public boolean longPressFirstSavedTile(boolean initials) {
        List<WebElement> rows = onScreen(initials ? "rowInitials" : "rowSignatures");
        if (rows.isEmpty()) return false;
        try {
            for (WebElement t : rows.get(0).findElements(By.className("android.widget.TextView"))) {
                String s = text(t);
                if (s.isEmpty() || DRAW_NEW.equals(s) || "+".equals(s)) continue;
                longPress(t);
                pause(1200);
                return true;
            }
        } catch (WebDriverException ignored) { }
        return false;
    }

    public boolean tapSheetDone() { return tapFirst("btnContinue"); }

    private void swipeLeft(WebElement row) {
        try {
            int y = row.getRect().getY() + row.getRect().getHeight() / 2;
            int x1 = row.getRect().getX() + (int) (row.getRect().getWidth() * 0.85);
            int x2 = row.getRect().getX() + (int) (row.getRect().getWidth() * 0.15);
            swipe(x1, y, x2, y);
        } catch (WebDriverException ignored) { }
    }

    private void scrollSheetDown() {
        try {
            var size = driver.manage().window().getSize();
            swipe(size.getWidth() / 2, (int) (size.getHeight() * 0.75), size.getWidth() / 2, (int) (size.getHeight() * 0.45));
        } catch (WebDriverException ignored) { }
    }

    protected void swipe(int x1, int y1, int x2, int y2) {
        var f = new org.openqa.selenium.interactions.PointerInput(
                org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        var s = new org.openqa.selenium.interactions.Sequence(f, 1);
        s.addAction(f.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x1, y1));
        s.addAction(f.createPointerDown(0));
        s.addAction(f.createPointerMove(Duration.ofMillis(450),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x2, y2));
        s.addAction(f.createPointerUp(0));
        try { driver.perform(List.of(s)); } catch (WebDriverException ignored) { }
        pause(600);
    }
}
