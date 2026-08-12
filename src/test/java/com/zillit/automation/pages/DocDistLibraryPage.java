package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * DOCUMENT DISTRIBUTION — the LIBRARY, which is the module's hub
 * ({@code ddLibraryFragment} / fragment_library.xml inside
 * {@code DocumentDistributionActivity}).
 *
 * <p>Everything else is reached from here: Upload · Create folder · Compose, and the four
 * shortcut buttons Templates · Lists · Contacts · History. The list itself is a folder
 * tree of {@code item_dd_library_folder} / {@code item_dd_library_file} rows with a
 * breadcrumb, a selection mode, search, sort, a date filter and a list/grid toggle.
 *
 * <p><b>Every id in this module is {@code dd_}-prefixed</b>, which makes "am I in Document
 * Distribution" cheap and unambiguous — unlike Drive, where {@code recyclerView} was
 * shared by four screens. But {@code dd_recycler} itself is reused by NINE screens inside
 * the module (library, picker, history, address book, presets, templates, move, CSV,
 * detail lists), so it can never identify WHICH screen you are on. Pair it with a
 * screen-unique id — here, {@code dd_btn_compose}.
 */
public class DocDistLibraryPage extends BasePage {

    /**
     * Display names the Tools list may use for this module. The code constant is a label
     * KEY ({@code document_distribution_tool}), not the visible text, and the visible text
     * comes from the server's tool list — so match on any of the plausible renderings
     * rather than guessing one.
     */
    public static final List<String> TOOL_LABELS =
            List.of("Document Distribution", "Doc Distribution", "Document distribution");

    public DocDistLibraryPage(AndroidDriver driver) {
        super(driver);
    }

    // ---------------- identity ----------------

    public boolean isDocDistActivity() {
        try {
            String a = driver.currentActivity();
            return a != null && a.contains("docdistribution.ui.DocumentDistributionActivity");
        } catch (WebDriverException e) {
            return false;
        }
    }

    /**
     * True on the LIBRARY specifically — not any other {@code dd_recycler} screen.
     *
     * <p><b>Checks an always-ON-SCREEN marker first, and that is a speed fix, not a
     * cosmetic one.</b> This used to wait on {@code dd_btn_compose}, which sits PAST THE
     * FOLD in the horizontally scrolling action row — so on a perfectly good library the
     * wait ran to its full timeout before falling through to a second timed wait. Every
     * case pays this in its {@code @BeforeEach}, which is why moving between cases felt
     * slower than the cases themselves.
     *
     * <p>{@code dd_view_toggle} (the list/grid switch) is library-only AND always within
     * the viewport, so it answers immediately. The old ids stay as fallbacks for any build
     * that lays the toolbar out differently.
     */
    public boolean isLibrary(Duration timeout) {
        if (isVisible("dd_view_toggle", timeout)) return true;
        if (anyDisplayed("dd_btn_filter_date") || anyDisplayed("dd_btn_sort")) return true;
        return isVisible("dd_btn_create_folder", Duration.ofSeconds(1))
                || isVisible("dd_btn_compose", Duration.ofSeconds(1));
    }

    /**
     * The same question as {@link #isLibrary(Duration)} with NO waiting — for the
     * recovery loop, which asks it after every BACK press.
     *
     * <p>{@code isLibrary(2s)} costs about four seconds to say "no": two seconds on
     * {@code dd_view_toggle} plus a second each on {@code dd_btn_create_folder} and
     * {@code dd_btn_compose}. Multiplied by the rounds of a BACK ladder, that alone was
     * most of the 27-second setups in the phone run. No implicit wait is configured on
     * this driver, so {@code findElements} returns straight away and this costs one round
     * trip per id.
     */
    public boolean isLibraryNow() {
        return anyDisplayed("dd_view_toggle")
                || anyDisplayed("dd_btn_filter_date")
                || anyDisplayed("dd_btn_sort")
                || anyDisplayed("dd_btn_create_folder")
                || anyDisplayed("dd_btn_compose");
    }

    /** True anywhere inside the module (any dd_ screen). */
    public boolean isInModule(Duration timeout) {
        return isVisible("dd_nav_host", timeout) || isDocDistActivity();
    }

    // ---------------- shell / shortcuts ----------------

    public boolean hasCompose()      { return present("dd_btn_compose"); }
    public boolean hasUpload()       { return present("dd_btn_upload"); }
    public boolean hasCreateFolder() { return present("dd_btn_create_folder"); }
    public boolean hasTemplates()    { return present("dd_btn_templates"); }
    public boolean hasLists()        { return present("dd_btn_lists"); }
    public boolean hasContacts()     { return present("dd_btn_contacts"); }
    public boolean hasHistory()      { return present("dd_btn_history"); }

    public String templatesBadge() { return textOf("dd_badge_templates"); }
    public String listsBadge()     { return textOf("dd_badge_lists"); }
    public String historyBadge()   { return textOf("dd_badge_history"); }

    public boolean openCompose()      { return clickChip("dd_btn_compose"); }
    public boolean openUpload()       { return clickChip("dd_btn_upload"); }
    public boolean openCreateFolder() { return clickChip("dd_btn_create_folder"); }
    public boolean openTemplates()    { return clickChip("dd_btn_templates"); }
    public boolean openLists()        { return clickChip("dd_btn_lists"); }
    public boolean openContacts()     { return clickChip("dd_btn_contacts"); }
    public boolean openHistory()      { return clickChip("dd_btn_history"); }

    // ---------------- help card ----------------

    public boolean hasHelpCard()    { return present("dd_help_card"); }
    public boolean helpBodyShown()  { return isVisible("dd_help_body", Duration.ofSeconds(2)); }
    public boolean toggleHelpCard() { return click("dd_help_header") || click("dd_help_chevron"); }

    // ---------------- list ----------------

    public boolean isEmptyStateShown() {
        return isVisible("dd_empty", Duration.ofSeconds(4))
                || isVisible("dd_empty_text", Duration.ofSeconds(2));
    }

    public String emptyText()     { return textOf("dd_empty_text"); }
    public String emptySubtitle() { return textOf("dd_empty_subtitle"); }
    public boolean hasEmptyCta()  { return present("dd_empty_cta"); }
    public boolean hasClearFiltersCta() { return present("dd_empty_clear_filters"); }

    /** Rendered row names only — see {@link #allItemNamesScrolled()} for the full set. */
    public List<String> itemNames() {
        try {
            return driver.findElements(byId("dd_name")).stream()
                    .map(WebElement::getText).filter(t -> t != null && !t.isBlank()).toList();
        } catch (WebDriverException e) {
            return List.of();
        }
    }

    public int itemCount() { return itemNames().size(); }

    /**
     * The full list, scroll-collected. The library is a RecyclerView, so a row below the
     * fold is simply absent from the tree — asserting on one screenful is how "the file is
     * missing" gets reported for a file that is plainly there.
     */
    public List<String> allItemNamesScrolled() {
        Set<String> seen = new LinkedHashSet<>(itemNames());
        int stable = 0;
        for (int i = 0; i < 12 && stable < 2; i++) {
            int before = seen.size();
            swipe(0.75, 0.35);
            seen.addAll(itemNames());
            stable = (seen.size() == before) ? stable + 1 : 0;
        }
        for (int i = 0; i < 5; i++) swipe(0.35, 0.75);
        return new ArrayList<>(seen);
    }

    public boolean hasItem(String name) {
        return allItemNamesScrolled().stream().anyMatch(n -> n.equalsIgnoreCase(name));
    }

    /**
     * Waits for an item to exist, checking SEARCH before the raw list.
     *
     * <p>Observed on device: right after a folder is created, a scroll-collect of the list
     * does not find it while typing its name into the search box finds it immediately —
     * the list simply has not re-queried yet. So search goes FIRST: it is both the faster
     * check (one query against the backend vs. up to 25 swipes) and the one that is
     * actually correct in the seconds after a create (trap 3.13b). The visible rows are
     * still checked before typing, so an on-screen item costs nothing.
     */
    public boolean waitForItem(String name, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        boolean firstPass = true;
        while (System.currentTimeMillis() < deadline) {
            if (itemNames().stream().anyMatch(n -> n.equalsIgnoreCase(name))) return true;
            if (search(name)) {
                boolean found = itemNames().stream().anyMatch(n -> n.equalsIgnoreCase(name));
                clearSearch();
                if (found) return true;
            }
            // Only after search has missed: the slow scroll-collect over the full list.
            if (!firstPass && hasItem(name)) return true;
            firstPass = false;
            pullToRefresh();
            sleep(350);
        }
        return false;
    }

    /** Taps a row in a PopupMenu (Edit / Delete / sort orders) by its visible label. */
    /**
     * Taps a PopupMenu row by label — EXACT match first, prefix only as a fallback.
     *
     * <p>The prefix-only version (`textMatches("(?i)Edit.*")`) is dangerous in this module:
     * the popup is drawn OVER the library, so the folder rows behind it are still in the
     * tree, and a folder called "Edited plans" (or any row starting with the label) wins
     * the match. The tap then lands on a row instead of the menu item, the expected sheet
     * never opens, and the failure points at the sheet rather than the tap.
     */
    public boolean tapMenuItem(String label) {
        String[] locators = {
                "new UiSelector().text(\"" + label + "\")",
                "new UiSelector().textMatches(\"(?i)" + label + "\")",
                "new UiSelector().textMatches(\"(?i)" + label + ".*\")",
        };
        for (String locator : locators) {
            try {
                driver.findElement(AppiumBy.androidUIAutomator(locator)).click();
                sleep(500);
                return true;
            } catch (WebDriverException ignored) { }
        }
        return false;
    }

    /** Visible texts right now — for diagnosing a menu/sheet that did not appear. */
    public List<String> visibleTexts(int limit) {
        try {
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("text=\"([^\"]{1,40})\"").matcher(driver.getPageSource());
            java.util.LinkedHashSet<String> out = new java.util.LinkedHashSet<>();
            while (m.find() && out.size() < limit) {
                String t = m.group(1).trim();
                if (!t.isEmpty()) out.add(t);
            }
            return new ArrayList<>(out);
        } catch (Exception e) { return List.of(); }
    }

    /** Opens a row by name, scrolling to reach it. */
    public boolean openItem(String name) {
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < 20; i++) {
                if (tapRowByName(name)) return true;
                swipe(0.75, 0.35);
            }
            for (int i = 0; i < 5; i++) swipe(0.35, 0.75);
        }
        return false;
    }

    private boolean tapRowByName(String name) {
        try {
            for (WebElement e : driver.findElements(byId("dd_name"))) {
                String t = e.getText();
                if (t != null && t.equalsIgnoreCase(name)) { e.click(); return true; }
            }
        } catch (WebDriverException ignored) { }
        return false;
    }

    /**
     * Opens the ⋮ menu on the row with this name.
     *
     * <p>Anchored on the row that CONTAINS the name rather than pairing two id lists by
     * index: folder rows carry extra controls ({@code dd_send}, {@code dd_publish}) that
     * file rows do not, so the n-th name and the n-th ⋮ are not the same row.
     */
    public boolean openRowMenu(String name) {
        String xpath = "//*[@resource-id='" + appPackage() + ":id/dd_card']"
                + "[.//*[@text=" + xpathLiteral(name) + "]]"
                + "//*[@resource-id='" + appPackage() + ":id/dd_more']";
        try {
            driver.findElement(AppiumBy.xpath(xpath)).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Ticks a row's checkbox — the way into selection mode. */
    public boolean selectRow(String name) {
        String xpath = "//*[@resource-id='" + appPackage() + ":id/dd_card']"
                + "[.//*[@text=" + xpathLiteral(name) + "]]"
                + "//*[@resource-id='" + appPackage() + ":id/dd_check']";
        try {
            driver.findElement(AppiumBy.xpath(xpath)).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    // ---------------- row-level actions (folder rows / grid file rows) ----------------

    /**
     * The row's own Send button — "distribute this folder by email". Folder rows carry
     * {@code dd_send} and {@code dd_publish} of their own; list-layout FILE rows do not
     * (they only have {@code dd_more}), while grid-layout file rows do.
     */
    public boolean tapRowSend(String name)    { return tapWithinRow(name, "dd_send"); }

    /** The row's own Publish button — publishes into another module. */
    public boolean tapRowPublish(String name) { return tapWithinRow(name, "dd_publish"); }

    private boolean tapWithinRow(String name, String controlId) {
        String xpath = "//*[@resource-id='" + appPackage() + ":id/dd_card']"
                + "[.//*[@text=" + xpathLiteral(name) + "]]"
                + "//*[@resource-id='" + appPackage() + ":id/" + controlId + "']";
        try {
            driver.findElement(AppiumBy.xpath(xpath)).click();
            sleep(500);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    // ---------------- date grouping ----------------

    /** Date-group headers ({@code item_dd_library_date_header}) — "Today · Aug 7, 2026". */
    public List<String> dateHeaders() {
        try {
            return driver.findElements(byId("dd_header_label")).stream()
                    .map(WebElement::getText).filter(t -> t != null && !t.isBlank()).toList();
        } catch (WebDriverException e) { return List.of(); }
    }

    /**
     * Ticks the FIRST date header's checkbox, which selects every row in that day's
     * group in one go. Undocumented in the original 152-case map — the header check
     * ({@code dd_header_check}) is a real bulk-selection path.
     */
    public boolean selectFirstDateGroup() {
        try {
            var checks = driver.findElements(byId("dd_header_check"));
            if (checks.isEmpty()) return false;
            checks.get(0).click();
            sleep(400);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    public boolean hasDateHeaders() { return !dateHeaders().isEmpty(); }

    // ---------------- folder header bar (inside a folder) ----------------

    public boolean hasFolderHeaderActions() { return anyDisplayed("dd_folder_header_actions"); }
    public boolean tapFolderPublish()       { return click("dd_folder_publish"); }
    public boolean tapFolderDistribute()    { return click("dd_folder_distribute"); }
    /** Shown instead of the actions when the folder holds no files. */
    public boolean folderHeaderEmptyShown() { return anyDisplayed("dd_folder_header_empty"); }

    // ---------------- selection mode ----------------

    public boolean isSelectionMode()  { return isVisible("dd_selection_bar", Duration.ofSeconds(3)); }

    /**
     * Long-presses a row.
     *
     * <p>Kept deliberately, even though the library has NO long-press handler: the test
     * map assumed "long-press / check enters selection mode", and the only way to report
     * that half as untrue is to actually try it. {@code LibraryAdapter} wires
     * {@code dd_card} to a plain click and selection to the always-visible
     * {@code dd_check}; the module's only long-presses live on the date-filter button,
     * the tooltip helper and history recipient rows.
     */
    public boolean longPressRow(String name) {
        String xpath = "//*[@resource-id='" + appPackage() + ":id/dd_card']"
                + "[.//*[@text=" + xpathLiteral(name) + "]]";
        try {
            WebElement row = driver.findElement(AppiumBy.xpath(xpath));
            var r = row.getRect();
            driver.executeScript("mobile: longClickGesture", java.util.Map.of(
                    "x", r.getX() + r.getWidth() / 2,
                    "y", r.getY() + r.getHeight() / 2,
                    "duration", 1200));
            sleep(500);
            return true;
        } catch (WebDriverException e) { return false; }
    }
    public String selectionCount()    { return textOf("dd_selection_count"); }
    public boolean openSelectionActions() { return click("dd_selection_actions"); }
    public boolean clearSelection()   { return click("dd_selection_clear"); }

    // ---------------- search / sort / filter / view ----------------

    public boolean hasSearch() { return present("dd_search_input"); }

    public boolean search(String text) {
        try {
            WebElement f = driver.findElement(byId("dd_search_input"));
            f.click();
            f.clear();
            if (!text.isEmpty()) f.sendKeys(text);
            hideKeyboardIfShown();
            sleep(600);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    public boolean clearSearch() { return search(""); }

    public boolean openSort()       { return clickChip("dd_btn_sort"); }
    public boolean openDateFilter() { return clickChip("dd_btn_filter_date"); }
    // ---------------- date range picker ----------------

    /**
     * True while the Material date picker is up. Its confirm button carries the
     * Material id {@code confirm_button} under the app's package.
     */
    public boolean datePickerOpen(Duration timeout) {
        // ONLY the Material picker's confirm button. The obvious-looking extra check on
        // the text "Filter by date" was wrong: that is ALSO the label of the library's own
        // date-filter CHIP, which is on screen the whole time — so the picker read as
        // "still open" for ever, and applyDateRange() reported failure after successfully
        // applying the range (the chip even showed the right range). Matching on text that
        // another visible control shares, again.
        return isVisible("confirm_button", timeout);
    }

    /**
     * Applies a date RANGE and confirms.
     *
     * <p>The app builds {@code MaterialDatePicker.Builder.dateRangePicker()}, so this is a
     * RANGE, not a single date: the confirm button stays DISABLED until both ends are
     * chosen, and {@code addOnPositiveButtonClickListener} — the only thing that calls
     * {@code viewModel.setDateFilter} — never fires. That is exactly what "the calendar
     * opens and then nothing happens" looks like: one tap selects a start, and without a
     * second tap there is nothing to confirm.
     *
     * @return true once the picker has closed with a range applied.
     */
    public boolean applyDateRange(String startDay, String endDay) {
        if (!datePickerOpen(Duration.ofSeconds(8))) return false;
        if (!tapDayCell(startDay)) return false;
        sleep(400);
        if (!tapDayCell(endDay)) return false;
        sleep(400);
        boolean enabled = confirmEnabled();
        System.out.println(">>> date picker: picked " + startDay + "–" + endDay
                + " confirmEnabled=" + enabled);
        if (!enabled) return false;      // a half-range cannot be confirmed — say so
        if (!click("confirm_button")) return false;
        for (int i = 0; i < 20; i++) {
            if (!datePickerOpen(Duration.ofMillis(300))) return true;
            sleep(300);
        }
        return false;
    }

    private boolean confirmEnabled() {
        try {
            return "true".equalsIgnoreCase(
                    driver.findElement(byId("confirm_button")).getAttribute("enabled"));
        } catch (WebDriverException e) { return false; }
    }

    /** Taps a day number in the calendar grid, matching the cell exactly. */
    private boolean tapDayCell(String day) {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().text(\"" + day + "\")")).click();
            return true;
        } catch (WebDriverException e) {
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiSelector().descriptionContains(\"" + day + "\")")).click();
                return true;
            } catch (WebDriverException ignored) { return false; }
        }
    }

    /** Long-pressing the date button clears an applied filter (the app's own shortcut). */
    public boolean clearDateFilterByLongPress() {
        if (!present("dd_btn_filter_date")) return false;
        try {
            var r = driver.findElement(byId("dd_btn_filter_date")).getRect();
            driver.executeScript("mobile: longClickGesture", java.util.Map.of(
                    "x", r.getX() + r.getWidth() / 2,
                    "y", r.getY() + r.getHeight() / 2,
                    "duration", 900));
            sleep(800);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    /** The × on the active-filter chip — the discoverable way to clear it. */
    public boolean clearDateFilterChip() {
        try {
            var chip = driver.findElement(byId("dd_date_filter_chip"));
            var r = chip.getRect();
            driver.executeScript("mobile: clickGesture", java.util.Map.of(
                    "x", r.getX() + r.getWidth() - Math.min(30, r.getHeight() / 2),
                    "y", r.getY() + r.getHeight() / 2));
            sleep(800);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    public String dateFilterChipText() { return textOf("dd_date_filter_chip"); }

    public boolean dateFilterChipShown() { return isVisible("dd_date_filter_chip", Duration.ofSeconds(3)); }

    public boolean hasViewToggle() { return present("dd_view_toggle"); }
    public boolean showGrid()      { return click("dd_view_grid"); }
    public boolean showList()      { return click("dd_view_list"); }

    /** The sort menu's four options, by their visible labels. */
    public boolean sortOptionShown(String label) {
        return textShown(label, Duration.ofSeconds(4));
    }

    public boolean chooseSort(String label) {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().textContains(\"" + label + "\")")).click();
            sleep(600);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    // ---------------- breadcrumb ----------------

    public boolean hasBreadcrumb()  { return present("dd_breadcrumb"); }
    public String currentCrumb()    { return textOf("dd_crumb_current"); }
    public boolean tapHomeCrumb()   { return click("dd_crumb_home"); }

    /**
     * True at the library ROOT — no folder open.
     *
     * <p>The app's own rule (LibraryFragment.render): the {@code dd_breadcrumb}
     * CONTAINER, the folder header bar and the Upload chip are all
     * {@code View.GONE} unless {@code state.isInFolder} — so "any of them displayed"
     * is the in-folder signal. NOT the crumb TEXT: {@code dd_crumb_current} keeps the
     * last folder's name after backing out, so a text test reports "not at root"
     * forever and any return-to-root loop spins until it gives up.
     */
    public boolean isAtRoot() {
        return !anyDisplayed("dd_breadcrumb") && !anyDisplayed("dd_folder_header_include");
    }

    /** The inverse, for cases that must PROVE they are inside a folder. */
    public boolean isInFolder() {
        return anyDisplayed("dd_breadcrumb") || anyDisplayed("dd_folder_header_include");
    }

    /**
     * The folder header bar (Publish / Distribute) — shown only inside a folder.
     *
     * <p>The runtime id is {@code dd_folder_header_include}: the {@code <include>} tag
     * carries its own id, which REPLACES the included layout's root id — so the
     * {@code dd_folder_header} the layout file declares never exists at runtime
     * (verified in a live dump; searching for it fails inside a real folder).
     */
    public boolean hasFolderHeader() { return anyDisplayed("dd_folder_header_include"); }

    public String folderHeaderName() { return textOf("dd_folder_header_name"); }

    private boolean anyDisplayed(String id) {
        try {
            for (WebElement e : driver.findElements(byId(id))) {
                if (e.isDisplayed()) return true;
            }
        } catch (WebDriverException ignored) { }
        return false;
    }

    /**
     * Returns to the root, so the next case does not run inside someone else's folder.
     *
     * <p><b>Uses BACK, not the home crumb</b> — the crumb does not work one level deep.
     * Opening a folder navigates {@code ddLibraryFragment → ddLibraryFragment} (the
     * graph's {@code dd_action_open_folder} targets the library itself), while
     * {@code ddCrumbHome} calls {@code popBackStack(R.id.ddLibraryFragment, false)},
     * which pops nothing when the CURRENT destination is already that id. So the tap
     * succeeds, changes nothing, and a crumb-only loop spins until it gives up — which
     * is exactly how one failed case used to poison every later one.
     *
     * <p>{@link #tapHomeCrumbForTest()} keeps the crumb path available for the case that
     * is actually about the crumb (B-12), so the bug is REPORTED rather than papered over.
     */
    public boolean returnToRoot() {
        for (int i = 0; i < 6 && !isAtRoot(); i++) {
            try { driver.navigate().back(); Thread.sleep(250); } catch (Exception e) { break; }
        }
        return isAtRoot();
    }

    /** The home-crumb tap on its own — for B-12, which tests the crumb itself. */
    public boolean tapHomeCrumbForTest() { return tapHomeCrumb(); }

    public boolean pullToRefresh() {
        try {
            driver.findElement(byId("dd_swipe"));
            swipe(0.35, 0.80);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    /** True when the list can actually be READ — rows rendered, or the empty state up. */
    public boolean isListReadable() {
        return isEmptyStateShown() || itemCount() > 0;
    }

    /** The app's shared confirmation dialog — delete, publish, discard. */
    public ConfirmPopupPage confirmPopup() {
        return new ConfirmPopupPage(driver);
    }

    // ---------------- helpers ----------------

    /**
     * "Is this control part of the library?" — scrolling the action row if needed.
     *
     * <p>The actions live in a HORIZONTALLY scrolling chip row
     * ({@code Filter by date · Name (A–Z) · Create folder · Upload · Compose · Templates ·
     * Lists · Contacts · History}). On a 1280-wide screen it is cut off after the second
     * chip, so a plain visibility check reports Upload, Compose and all four shortcuts as
     * MISSING on a perfectly good screen — which is exactly how this suite first failed.
     *
     * <p>Scrolls right looking for the control, then rewinds so the next lookup starts
     * from a known position.
     */
    private boolean present(String id) {
        if (isVisible(id, Duration.ofSeconds(1))) return true;

        // The action row is an un-idded HorizontalScrollView, so scroll it by CLASS with
        // UiAutomator's own horizontal scroller: one round trip that walks the row in
        // both directions, instead of a blind swipe loop.
        //
        // The blind loop is what this replaced, and it was wrong twice over: it only ever
        // swiped one way (left, revealing chips further right), so a chip resting to the
        // LEFT could never be found — every swipe moved away from it. That is how
        // "Upload is not offered inside this folder" got reported for a build whose folder
        // view plainly renders dd_btn_upload, skipping the whole files area. Scanning both
        // ways fixed the correctness bug but cost ~16 swipes per lookup, which made a full
        // run crawl; this costs one call.
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().className(\"android.widget.HorizontalScrollView\"))"
                            + ".setAsHorizontalList().setMaxSearchSwipes(8)"
                            + ".scrollIntoView(new UiSelector().resourceId(\""
                            + appPackage() + ":id/" + id + "\"))"));
            return isVisible(id, Duration.ofMillis(800));
        } catch (WebDriverException ignored) { }

        // Fallback for builds where the row is not a HorizontalScrollView: rewind, scan.
        rewindChips();
        if (isVisible(id, Duration.ofMillis(500))) return true;
        for (int i = 0; i < 6; i++) {
            if (!swipeChipsLeft()) break;
            if (isVisible(id, Duration.ofMillis(500))) return true;
        }
        return isVisible(id, Duration.ofMillis(500));
    }

    /**
     * The centre X of a chip, revealing it first. -1 when it cannot be reached.
     *
     * <p>Only comparable between two chips that are on screen AT THE SAME TIME — the row
     * scrolls, so an x read before scrolling and one read after are in different frames of
     * reference and comparing them is meaningless.
     */
    public int chipCenterX(String id) {
        if (!present(id)) return -1;
        try {
            var r = driver.findElement(byId(id)).getRect();
            return r.getX() + r.getWidth() / 2;
        } catch (WebDriverException e) { return -1; }
    }

    /** True when both chips are visible right now, so their x values can be compared. */
    public boolean bothOnScreen(String idA, String idB) {
        return anyDisplayed(idA) && anyDisplayed(idB);
    }

    /** Scrolls the action row back to its start (Filter by date / Name A-Z end). */
    public void rewindChipRow() { rewindChips(); }

    /** The y of the action chip row, taken from a chip that is always on screen. */
    private int chipRowY() {
        for (String anchor : new String[]{"dd_btn_filter_date", "dd_btn_sort", "dd_view_toggle"}) {
            try {
                var r = driver.findElement(byId(anchor)).getRect();
                return r.getY() + r.getHeight() / 2;
            } catch (WebDriverException ignored) { }
        }
        return -1;
    }

    private boolean swipeChipsLeft()  { return swipeChips(true); }

    private void rewindChips() {
        for (int i = 0; i < 8; i++) {
            if (!swipeChips(false)) return;
        }
    }

    /** One horizontal swipe along the chip row. left=true reveals the chips further right. */
    private boolean swipeChips(boolean left) {
        int y = chipRowY();
        if (y < 0) return false;
        try {
            var size = driver.manage().window().getSize();
            int width = (int) (size.width * 0.6);
            driver.executeScript("mobile: swipeGesture", java.util.Map.of(
                    "left", (int) (size.width * 0.2),
                    "top", y - 40,
                    "width", width,
                    "height", 80,
                    "direction", left ? "left" : "right",
                    "percent", 0.75));
            Thread.sleep(250);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Taps a control in the action row, scrolling it into view first.
     *
     * <p>{@link #click} alone fails on anything past the second chip.
     */
    private boolean clickChip(String id) {
        if (!present(id)) return false;
        // present() rewinds when it finds the chip off-screen, so re-scan up to the chip
        // before clicking rather than assuming it is still under the finger.
        if (click(id)) return true;
        for (int i = 0; i < 8; i++) {
            if (!swipeChipsLeft()) break;
            if (click(id)) return true;
        }
        return false;
    }

    private String textOf(String id) {
        try { return driver.findElement(byId(id)).getText(); }
        catch (WebDriverException e) { return ""; }
    }

    private boolean click(String id) {
        try { driver.findElement(byId(id)).click(); sleep(350); return true; }
        catch (WebDriverException e) { return false; }
    }

    private static String xpathLiteral(String s) {
        if (!s.contains("'")) return "'" + s + "'";
        return "concat('" + s.replace("'", "',\"'\",'") + "')";
    }

    private void swipe(double fromY, double toY) {
        try {
            var size = driver.manage().window().getSize();
            driver.executeScript("mobile: swipeGesture", java.util.Map.of(
                    "left", size.width / 2 - 100,
                    "top", (int) (size.height * Math.min(fromY, toY)),
                    "width", 200,
                    "height", (int) (size.height * Math.abs(fromY - toY)),
                    "direction", fromY > toY ? "up" : "down",
                    "percent", 0.8));
            Thread.sleep(250);
        } catch (Exception ignored) { }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
