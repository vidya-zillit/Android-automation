package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DOCUMENT DISTRIBUTION — the PUBLISH bottom sheet
 * ({@code PublishBottomSheet} / bottomsheet_dd_publish.xml).
 *
 * <p>Seven destination cards in a 2-column grid, a per-category set of extra fields, a
 * file checklist and Cancel / Publish. Everything here is read from the layout and from
 * {@code PublishBottomSheet.kt}; nothing is guessed.
 *
 * <p><b>The seven cards are not symmetrical.</b> The spec's identifier list implies every
 * category carries {@code _badge}, {@code _view_only}, {@code _chevron} and
 * {@code _selected}. The layout says otherwise, and a case that looks for the wrong
 * variant reports a missing control on a healthy screen:
 * <ul>
 *   <li>The four MODULE cards (call sheet · report · info · confidential) have
 *       {@code _badge} and nothing else.</li>
 *   <li>The three TOOL cards (dod · schedule · script) have {@code _view_only} instead —
 *       the same "access badge" widget under a different id.</li>
 *   <li>Only schedule and script have {@code _chevron} and {@code _selected}: D.O.D is a
 *       single-option destination, so {@code wireToolDestinations()} sets its chevron
 *       GONE and gives it no sub-label at all.</li>
 * </ul>
 *
 * <p><b>Two ids collide with the library behind the sheet.</b> {@code dd_publish} is also
 * the folder/grid row's publish ImageButton, and {@code dd_name} is every library row —
 * so both are matched by CLASS ({@code android.widget.Button}) or scoped inside
 * {@code dd_files} rather than looked up bare. {@code dd_cat_callsheet} exists in this one
 * layout and is the sheet's identity anchor.
 *
 * <p><b>Tapping a card can raise a MODAL dialog.</b> {@code onCategoryActivated} fires the
 * "Module not enabled" popup the moment a destination with no rights row is selected —
 * every time, not once. That dialog is the app's custom family
 * ({@code common_dialog_layout_with_button}) built with a null left button, so only
 * {@code rightButton} ("OK") is on screen, and the dialog is {@code setCancelable(false)}
 * — BACK will NOT close it. {@link #dismissBlockingDialog()} is the only safe unwind, and
 * it is deliberately asymmetric: it declines through {@code leftButton} when one exists
 * (the request-rights dialog's "No") and only ever presses {@code rightButton} when the
 * dialog has no left button at all. It must never press "Proceed" — that fires
 * {@code BaseViewModel.requestToAdmin}, which posts a real message to a real admin.
 */
public class DocDistPublishSheet extends BasePage {

    /** The seven destination card ids, in layout order. */
    public static final List<String> CATEGORY_IDS = List.of(
            "dd_cat_callsheet", "dd_cat_report", "dd_cat_info", "dd_cat_confidential",
            "dd_cat_dod", "dd_cat_schedule", "dd_cat_script");

    /** The visible card titles, same order — used to prove N-2 reads as a human would. */
    public static final List<String> CATEGORY_LABELS = List.of(
            "Call Sheet (per unit)", "Production Report", "Info", "Confidential Info",
            "Schedule D.O.D", "Schedule Full & One Line", "Script & Pages Distribution");

    /** The two badge strings {@code applyAccessBadge} can render. */
    public static final String BADGE_VIEW_ONLY = "View only";
    public static final String BADGE_NO_ACCESS = "No access";

    /** Only these two destinations open a sub-selection PopupMenu. */
    public static final List<String> SCHEDULE_OPTIONS =
            List.of("Schedule Full", "Pages", "Schedule One Line");
    public static final List<String> SCRIPT_OPTIONS = List.of("Script", "Page");

    /**
     * Card id → the id of ITS access badge. Four module cards use {@code _badge}; three
     * tool cards use {@code _view_only}. There is no card that has both.
     */
    private static final Map<String, String> BADGE_OF = badgeMap();

    private static Map<String, String> badgeMap() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("dd_cat_callsheet",    "dd_cat_callsheet_badge");
        m.put("dd_cat_report",       "dd_cat_report_badge");
        m.put("dd_cat_info",         "dd_cat_info_badge");
        m.put("dd_cat_confidential", "dd_cat_confidential_badge");
        m.put("dd_cat_dod",          "dd_cat_dod_view_only");
        m.put("dd_cat_schedule",     "dd_cat_schedule_view_only");
        m.put("dd_cat_script",       "dd_cat_script_view_only");
        return m;
    }

    public DocDistPublishSheet(AndroidDriver driver) {
        super(driver);
    }

    // ==================== identity ====================

    /** {@code dd_cat_callsheet} exists in bottomsheet_dd_publish.xml and nowhere else. */
    public boolean isOpen(Duration timeout) { return isVisible("dd_cat_callsheet", timeout); }

    public boolean waitClosed(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (!displayed("dd_cat_callsheet")) return true;
            sleep(300);
        }
        return false;
    }

    /** "Publish documents · 1 document" — the header, which carries the file count. */
    public String title() { return textOf("dd_title"); }

    // ==================== categories ====================

    public boolean hasCategory(String catId) { return displayed(catId); }

    /** Every card that is actually on screen, by id. */
    public List<String> visibleCategories() {
        List<String> out = new ArrayList<>();
        for (String id : CATEGORY_IDS) {
            scrollToId(id);
            if (displayed(id)) out.add(id);
        }
        return out;
    }

    /**
     * Selects a destination.
     *
     * <p>For the two multi-option tool cards this only OPENS the PopupMenu — the category
     * is not active until {@link #tapMenuOption} picks one. For every other card the tap
     * activates the category directly, which can immediately raise the "Module not
     * enabled" modal.
     */
    public boolean tapCategory(String catId) { return click(catId); }

    /**
     * The rows of a card's sub-selection PopupMenu.
     *
     * <p>Read generically off {@code android:id/title} rather than matched against the
     * English strings: {@code resolveOptionLabel()} runs every label through the project's
     * server-supplied label dictionary first, so "Schedule Full" can legitimately render
     * as something else on a project that ships its own copy. The COUNT and the ORDER are
     * what the app guarantees; the text is data to report.
     */
    public List<String> popupMenuItems() {
        List<String> out = new ArrayList<>();
        for (WebElement e : menuRows()) {
            String t = e.getText();
            if (t != null && !t.isBlank()) out.add(t);
        }
        return out;
    }

    /**
     * The row views of the open PopupMenu.
     *
     * <p><b>The title id depends on WHICH PopupMenu the app used, and this one is not the
     * framework's.</b> {@code PublishBottomSheet} imports
     * {@code androidx.appcompat.widget.PopupMenu} (line 7) and inflates appcompat's
     * {@code abc_popup_menu_item_layout}, whose title is declared {@code @+id/title} — so
     * it resolves to <b>{@code com.zillit.zillitapp.dev:id/title}</b>, in the app's own id
     * space. Only {@code android.widget.PopupMenu} gives you {@code android:id/title}.
     *
     * <p>Asking for the framework id alone found nothing, so N-5 read
     * {@code options=[]} and reported "Schedule offers 0 options" about a menu that was
     * open on screen with all three rows showing. Try both id spaces, then fall back to
     * the row views themselves.
     */
    private List<WebElement> menuRows() {
        // Scope by the row TYPE first, not by an id. ListMenuItemView exists ONLY inside
        // an open menu, so this cannot collide with the sheet's own card titles the way
        // a bare ":id/title" would — several cards behind the popup carry a title view,
        // and matching those would have swapped one wrong answer for another. Take the
        // TextViews INSIDE the rows: the row itself is a ViewGroup whose text is empty,
        // and clicking a title still hits the row.
        try {
            List<WebElement> rows = driver.findElements(AppiumBy.xpath(
                    "//androidx.appcompat.view.menu.ListMenuItemView//android.widget.TextView"));
            if (!rows.isEmpty()) return rows;
        } catch (WebDriverException ignored) { }
        for (String id : new String[]{appPackage() + ":id/title", "android:id/title"}) {
            try {
                List<WebElement> rows = driver.findElements(
                        AppiumBy.androidUIAutomator("new UiSelector().resourceId(\"" + id + "\")"));
                if (!rows.isEmpty()) return rows;
            } catch (WebDriverException ignored) { }
        }
        return List.of();
    }

    /** Picks the n-th row of the open PopupMenu (0-based, layout order). */
    public boolean tapMenuOptionAt(int index) {
        try {
            List<WebElement> rows = menuRows();
            if (index < 0 || index >= rows.size()) return false;
            rows.get(index).click();
            sleep(600);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    /** Picks a row out of a card's sub-selection PopupMenu by its exact visible label. */
    public boolean tapMenuOption(String label) {
        try {
            for (WebElement e : menuRows()) {
                if (label.equalsIgnoreCase(e.getText())) { e.click(); sleep(600); return true; }
            }
        } catch (WebDriverException ignored) { }
        return false;
    }

    /** The id of this card's access badge — {@code _badge} or {@code _view_only}. */
    public String badgeIdFor(String catId) { return BADGE_OF.get(catId); }

    /**
     * The badge's text, or "" when it is not rendered.
     *
     * <p>{@code applyAccessBadge} sets the badge {@code View.GONE} whenever the user HAS
     * posting access, so an empty answer means "this user can publish here" — it is a
     * result, never a failure. Trap 3.2's lesson generalised: read the badge, do not
     * demand one.
     */
    public String badgeText(String catId) {
        String id = BADGE_OF.get(catId);
        if (id == null) return "";
        scrollToId(catId);
        return displayed(id) ? textOf(id) : "";
    }

    /** Every card that currently shows a badge, mapped to what it says. */
    public Map<String, String> badgedCategories() {
        Map<String, String> out = new LinkedHashMap<>();
        for (String catId : CATEGORY_IDS) {
            String text = badgeText(catId);
            if (!text.isBlank()) out.put(catId, text);
        }
        return out;
    }

    /** Only schedule and script own a chevron — D.O.D is single-option. */
    public boolean chevronShown(String catId) {
        return displayed(catId + "_chevron");
    }

    /** "Selected: Schedule Full" once an option is picked; the card's subtitle before. */
    public String selectedLabel(String catId) {
        scrollToId(catId);
        return textOf(catId + "_selected");
    }

    // ==================== request-rights banner ====================

    public boolean requestRightsBannerShown() {
        return isVisible("dd_request_rights_banner", Duration.ofSeconds(5));
    }

    public String requestRightsMessage() { return textOf("dd_request_rights_message"); }

    public boolean requestRightsButtonShown() { return displayed("dd_request_rights_btn"); }

    /**
     * Taps "Request Posting Rights".
     *
     * <p>This only OPENS the No / Proceed dialog. The caller MUST decline it — "Proceed"
     * runs {@code BaseViewModel.requestToAdmin}, which posts a permission request into a
     * real admin's chat. {@link #dismissBlockingDialog()} declines by construction.
     */
    public boolean tapRequestRights() { return click("dd_request_rights_btn"); }

    // ==================== per-category extra fields ====================

    // Visibility is entirely driven by renderExtras() and the PublishCategory flags:
    //   name          → SCHEDULE_DOD only
    //   episode       → tool-form categories AND a TELEVISION project
    //   scene         → SCHEDULE_PAGE / PAGE_DISTRIBUTION
    //   schedule type → SCHEDULE_PAGE
    //   schedule date → SCHEDULE_FULL / SCHEDULE_ONELINE (shown, optional)
    //   note block    → the four module categories only

    public boolean nameFieldShown()         { return isVisible("dd_pub_name_layout", Duration.ofSeconds(4)); }
    public boolean episodeFieldShown()      { return isVisible("dd_pub_episode_layout", Duration.ofSeconds(3)); }
    public boolean sceneFieldShown()        { return isVisible("dd_pub_scene_layout", Duration.ofSeconds(4)); }
    public boolean scheduleTypeFieldShown() { return isVisible("dd_pub_schedule_type_layout", Duration.ofSeconds(3)); }
    public boolean scheduleRowShown()       { return isVisible("dd_pub_schedule_row", Duration.ofSeconds(3)); }
    public boolean noteBlockShown()         { return isVisible("dd_note_block", Duration.ofSeconds(3)); }

    public boolean typeName(String text)    { return typeIn("dd_pub_name_input", text); }
    public boolean typeEpisode(String text) { return typeIn("dd_pub_episode_input", text); }
    public boolean typeScene(String text)   { return typeIn("dd_pub_scene_input", text); }

    public String nameText()    { return textOf("dd_pub_name_input"); }
    public String episodeText() { return textOf("dd_pub_episode_input"); }
    public String sceneText()   { return textOf("dd_pub_scene_input"); }

    /** "Only PDFs can be published to X" / "X accepts one file at a time". */
    public String ruleNote() { return displayed("dd_pub_rule_note") ? textOf("dd_pub_rule_note") : ""; }

    // ==================== republish (ZL-20245) ====================

    /**
     * True when this build ships the Replace / Addition / Resend block.
     *
     * <p>On the current source it is FALSE: the whole section is commented out in BOTH
     * PublishBottomSheet.kt AND bottomsheet_dd_publish.xml, so none of the
     * {@code dd_republish_*} ids the spec lists are compiled into the layout. This method
     * exists so the suite can assert that absence rather than silently skip N-10…N-12.
     */
    public boolean republishSectionShipped() {
        return present("dd_republish_section") || present("dd_republish_radio_replace")
                || present("dd_republish_radio_addition") || present("dd_republish_radio_resend")
                || present("dd_republish_target_input");
    }

    // ==================== file checklist ====================

    /**
     * The names in the sheet's own list.
     *
     * <p>Scoped inside {@code dd_files} on purpose: {@code dd_name} is also every library
     * row's id, and the library is right behind this sheet.
     */
    public List<String> fileNames() {
        List<String> out = new ArrayList<>();
        try {
            for (WebElement e : driver.findElements(AppiumBy.xpath(
                    "//*[@resource-id='" + appPackage() + ":id/dd_files']"
                            + "//*[@resource-id='" + appPackage() + ":id/dd_name']"))) {
                String t = e.getText();
                if (t != null && !t.isBlank()) out.add(t);
            }
        } catch (WebDriverException ignored) { }
        return out;
    }

    /** Ticks / unticks the checkbox on the sheet row carrying this name. */
    public boolean toggleFile(String name) {
        try {
            driver.findElement(AppiumBy.xpath(
                    "//*[@resource-id='" + appPackage() + ":id/dd_files']"
                            + "//*[@resource-id='" + appPackage() + ":id/dd_row']"
                            + "[.//*[@text=" + xpathLiteral(name) + "]]"
                            + "//*[@resource-id='" + appPackage() + ":id/dd_check']")).click();
            sleep(350);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    public boolean fileChecked(String name) {
        try {
            WebElement box = driver.findElement(AppiumBy.xpath(
                    "//*[@resource-id='" + appPackage() + ":id/dd_files']"
                            + "//*[@resource-id='" + appPackage() + ":id/dd_row']"
                            + "[.//*[@text=" + xpathLiteral(name) + "]]"
                            + "//*[@resource-id='" + appPackage() + ":id/dd_check']"));
            return "true".equalsIgnoreCase(box.getAttribute("checked"));
        } catch (WebDriverException e) { return false; }
    }

    /** Makes sure the row is ticked — a category switch to a single-file destination clears it. */
    public boolean ensureFileChecked(String name) {
        if (fileChecked(name)) return true;
        toggleFile(name);
        return fileChecked(name);
    }

    // ==================== footer ====================

    /**
     * Publish's enabled state — the app's own gate for every required field.
     *
     * <p>Matched by CLASS: the sheet's Publish is a MaterialButton
     * ({@code android.widget.Button}), while the library folder / grid rows behind it
     * carry an ImageButton with the SAME {@code dd_publish} id.
     */
    public boolean isPublishEnabled() {
        WebElement b = publishButton();
        try { return b != null && b.isEnabled(); }
        catch (WebDriverException e) { return false; }
    }

    public boolean tapPublish() {
        WebElement b = publishButton();
        if (b == null) return false;
        try { b.click(); sleep(500); return true; }
        catch (WebDriverException e) { return false; }
    }

    /** Closes the sheet without publishing — the only exit this suite ever uses. */
    public boolean tapCancel() {
        try {
            driver.findElement(AppiumBy.xpath(
                    "//android.widget.Button[@resource-id='" + appPackage() + ":id/dd_cancel']")).click();
            sleep(400);
            return true;
        } catch (WebDriverException e) {
            return click("dd_cancel");
        }
    }

    private WebElement publishButton() {
        try {
            List<WebElement> found = driver.findElements(AppiumBy.xpath(
                    "//android.widget.Button[@resource-id='" + appPackage() + ":id/dd_publish']"));
            for (WebElement e : found) {
                if (e.isDisplayed()) return e;
            }
        } catch (WebDriverException ignored) { }
        return null;
    }

    // ==================== dialog unwinding ====================

    /**
     * Clears whatever app dialog the sheet raised, WITHOUT ever consenting to anything.
     *
     * <p>Two dialogs can appear from inside this sheet, both from
     * {@code showDialogWithButton} (the custom {@code leftButton} / {@code rightButton}
     * family, not the framework one):
     * <ul>
     *   <li><b>Request posting rights</b> — "No" (left) / "Proceed" (right). Proceed sends
     *       a real permission request to an admin's chat. We press "No", always.</li>
     *   <li><b>Module not enabled</b> — built with {@code leftButtonName = null}, so only
     *       "OK" (right) is rendered. There is nothing to decline, and the dialog is
     *       {@code setCancelable(false)} so BACK is a no-op. Pressing OK is the ONLY way
     *       out, and it consents to nothing.</li>
     * </ul>
     *
     * <p>This is why {@code ConfirmPopupPage.clearIfBlocking()} is not used here: it
     * declines first and falls back to BACK, which on the not-enabled dialog leaves the
     * modal on screen and takes out every later case (hard-won lesson 3.1).
     *
     * @return true when a dialog was present and has been cleared
     */
    public boolean dismissBlockingDialog() {
        ConfirmPopupPage popup = new ConfirmPopupPage(driver);
        if (!popup.isShown(Duration.ofSeconds(2))) return false;
        System.out.println(">>> publish dialog says: '" + popup.text()
                + "' [left='" + popup.cancelLabel() + "' right='" + popup.confirmLabel() + "']");
        if (displayed("leftButton")) {
            // Declining is always available and always safe.
            return popup.cancel(Duration.ofSeconds(2));
        }
        // OK-only informational dialog — accept is the only exit, and consents to nothing.
        return popup.accept(Duration.ofSeconds(2));
    }

    /** True when any app dialog is currently covering the sheet. */
    public boolean dialogShown() {
        return new ConfirmPopupPage(driver).isShown(Duration.ofSeconds(2));
    }

    public String dialogText() { return new ConfirmPopupPage(driver).text(); }

    // ---------------- helpers ----------------

    private boolean typeIn(String id, String text) {
        try {
            scrollToId(id);
            WebElement f = driver.findElement(byId(id));
            f.click();
            f.clear();
            if (!text.isEmpty()) f.sendKeys(text);
            hideKeyboardIfShown();
            sleep(300);
            return true;
        } catch (WebDriverException e) { return false; }
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
            sleep(400);
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
