package com.zillit.automation.tests.sides;

import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.sides.SidesGeneratePage;
import com.zillit.automation.pages.sides.SidesListPage;
import com.zillit.automation.pages.sides.SidesScriptsPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * SIDES — the whole tool: both tabs, the two gates, scripts and pages, the manual Generate
 * flow through to a published side, the row actions, History, and the Autogenerate sheet.
 *
 * <p>A "side" is the day's pages handed to the unit: a PDF cut from a script down to the
 * scenes being shot. So the cases follow that job rather than the screens — a script with
 * a file, scenes picked from a version, a side generated, reviewed, published, viewed and
 * finally deleted.
 *
 * <p>Fixtures carry the {@code ZL_AUTO} prefix. The script PDF pushed by the runner holds
 * five numbered scene headings, which matters: the backend derives the scene list from the
 * PDF's text, so a blank stub produces zero scenes and every picker in this suite would
 * come back empty with nothing to say why.
 */
@ExtendWith(ReasonLogger.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Order(TestOrder.SIDES)
class SidesTest extends SidesBaseTest {

    /**
     * The five-page script fixture, named WITH its extension.
     *
     * <p>It used to be {@code "zl_sides_script"}, which is a prefix of BOTH fixtures the
     * runner pushes — {@code zl_sides_script.pdf} and {@code zl_sides_script.fdx}. The
     * picker matches on "contains", so the bare prefix could stop on the .fdx, and a
     * Final Draft file has no PDF pages: the upload then succeeded and the card read
     * "1 pages" for a script this suite believes has five. That looked like a page-count
     * defect in the app and cost several runs before the name itself turned out to be
     * ambiguous.
     */
    private static final String SCRIPT_PDF = "zl_sides_script.pdf";
    private static final String CALL_SHEET_PDF = "zl_sides_callsheet";
    /** Pages in {@link #SCRIPT_PDF} — five, one numbered scene heading on each. */
    private static final int SCRIPT_PDF_PAGES = 5;
    /** The same five scenes as a Final Draft file — the module accepts .pdf and .fdx. */
    private static final String SCRIPT_FDX = "zl_sides_script.fdx";

    /** Carried between cases: each is set only once the step that creates it has succeeded. */
    private String scriptNoFile;
    private String scriptWithFile;
    private String sideTitle;
    /** Extra scripts SI-16 makes to build a list taller than the screen. */
    private final List<String> extraScripts = new ArrayList<>();

    // ─────────────────────────── the shell ───────────────────────────

    @Test
    @Order(10)
    @DisplayName("SI-01 The module opens on Sides, with both actions and both tabs")
    void moduleOpens() {
        SidesListPage list = new SidesListPage(driver);
        assertTrue(list.isLoaded(Duration.ofSeconds(25)), "The Sides module did not load");
        assertEquals(SidesListPage.HEADER, String.valueOf(list.header()).trim(),
                "The header reads \"" + list.header() + "\"");
        assertTrue(list.generateShown(), "Generate Sides is not offered");
        assertTrue(list.autogenerateShown(), "Autogenerate Sides is not offered");
        assertTrue(list.historyShown(), "The History section is not on the list screen");
    }

    @Test
    @Order(20)
    @DisplayName("SI-02 The Script tab opens and the header follows it")
    void scriptTabOpens() {
        SidesListPage list = new SidesListPage(driver);
        assumeTrue(list.openScriptTab(), "The Script tab could not be tapped");
        sleep(2500);
        SidesScriptsPage scripts = new SidesScriptsPage(driver);
        assertTrue(scripts.isLoaded(Duration.ofSeconds(15)), "The Script tab did not load");
        assertEquals(SidesScriptsPage.HEADER, String.valueOf(list.header()).trim(),
                "The toolbar header reads \"" + list.header() + "\" on the Script tab."
                        + " It is meant to track the selected tab.");
    }

    @Test
    @Order(30)
    @DisplayName("SI-03 Going back to Sides restores its header and its actions")
    void sidesTabRestores() {
        SidesListPage list = new SidesListPage(driver);
        assumeTrue(list.openSidesTab(), "The Sides tab could not be tapped");
        sleep(2500);
        assertEquals(SidesListPage.HEADER, String.valueOf(list.header()).trim(),
                "The header reads \"" + list.header() + "\" back on the Sides tab");
        assertTrue(list.generateShown() && list.autogenerateShown(),
                "The action buttons did not come back with the tab");
    }

    // ─────────────────────── scripts and pages ───────────────────────

    @Test
    @Order(40)
    @DisplayName("SI-10 Add Script asks for a name and offers an OPTIONAL file")
    void addScriptDialogShape() {
        SidesScriptsPage scripts = openScripts();
        assumeTrue(scripts.openAddScript(), "Add Script is not offered");
        assertTrue(scripts.addScriptDialogShown(), "The Add Script dialog did not open");
        // The file being optional is the whole reason a "pages only" script can exist, and
        // it is what makes the two gates differ later on.
        assertNotNull(scripts.pickedFileName(),
                "The dialog shows no file row, so a script could not be created without one");
        assumeTrue(scripts.cancelScript(), "Cancel was not available");
        sleep(1200);
    }

    @Test
    @Order(50)
    @DisplayName("SI-11 A blank script name is refused and the dialog stays open")
    void blankScriptNameRefused() {
        SidesScriptsPage scripts = openScripts();
        assumeTrue(scripts.openAddScript(), "Add Script is not offered");
        assumeTrue(scripts.addScriptDialogShown(), "The Add Script dialog did not open");
        assumeTrue(scripts.setScriptName(""), "The name could not be cleared");
        assumeTrue(scripts.saveScript(), "Save was not clickable");
        sleep(2500);
        assertTrue(scripts.addScriptDialogShown(),
                "A script saved with no name. A nameless script cannot be told from another"
                        + " in the picker that every generate starts from.");
        scripts.cancelScript();
        sleep(1200);
    }

    @Test
    @Order(60)
    @DisplayName("SI-12 A script saves without a file, and offers only Upload")
    void nameOnlyScriptSaves() {
        SidesScriptsPage scripts = openScripts();
        String candidate = fixtureName("ScriptA");
        assumeTrue(scripts.openAddScript(), "Add Script is not offered");
        assumeTrue(scripts.setScriptName(candidate), "The name could not be typed");
        assumeTrue(scripts.saveScript(), "Save was not clickable");
        sleep(6000);

        SidesScriptsPage after = openScripts();
        SidesScriptsPage.Card card = after.find(candidate);
        assertNotNull(card, "\"" + candidate + "\" was saved and no card carries exactly"
                + " that name. Cards: " + after.titles());
        // No file yet, so the card must not offer Replace / View / Download.
        assertFalse(card.hasFile,
                "A script created without a file reports one: " + card);
        scriptNoFile = candidate;
    }

    @Test
    @Order(70)
    @DisplayName("SI-13 Uploading a script file turns the card into Replace / View / Download")
    void uploadScriptFile() {
        assumeTrue(scriptNoFile != null, "SI-12 created no script to upload to");
        SidesScriptsPage scripts = openScripts();
        assumeTrue(scripts.tapReplace(scriptNoFile),
                "Upload Script is not offered on \"" + scriptNoFile + "\"");
        assumeTrue(scripts.systemPickerShown(), "The system file picker did not open");
        assumeTrue(scripts.pickFromSystemPicker(SCRIPT_PDF, Duration.ofSeconds(20)),
                "\"" + SCRIPT_PDF + "\" could not be picked. The runner pushes it to"
                        + " /sdcard/Documents before the run.");
        sleep(12000);

        SidesScriptsPage after = openScripts();
        SidesScriptsPage.Card card = after.find(scriptNoFile);
        assertNotNull(card, "\"" + scriptNoFile + "\" vanished after the upload: "
                + after.titles());
        // Only the upload itself is asserted here. What the card then SAYS about the file
        // is SI-14's business: a wrong page count is a real defect, but it must not stand
        // in for a failed upload and take every later case down with it.
        assertTrue(card.hasFile,
                "The file uploaded and the card still offers no Replace / View / Download: "
                        + card);
        scriptWithFile = scriptNoFile;
    }

    @Test
    @Order(75)
    @DisplayName("SI-14 A script's page count matches the file it holds")
    void pageCountMatchesTheFile() {
        assumeTrue(scriptWithFile != null, "SI-13 uploaded no script file");
        SidesScriptsPage scripts = openScripts();
        SidesScriptsPage.Card card =
                scripts.waitForPageCount(scriptWithFile, SCRIPT_PDF_PAGES, Duration.ofSeconds(90));
        assumeTrue(card != null, "\"" + scriptWithFile + "\" is no longer listed");

        // The fixture is five pages with five numbered scene headings, and the card's meta
        // is where the unit reads how much script it is holding. A five-page script
        // reported as one page is wrong wherever that number came from.
        assertTrue(String.valueOf(card.meta).contains(SCRIPT_PDF_PAGES + " page"),
                "The script PDF has " + SCRIPT_PDF_PAGES + " pages and 90 seconds after the"
                        + " upload the card still reads \"" + card.meta + "\"");
    }

    @Test
    @Order(78)
    @DisplayName("SI-15 Replacing the file makes a new version, whose page count also matches")
    void replaceMakesANewVersion() {
        assumeTrue(scriptWithFile != null, "SI-13 uploaded no script file");
        SidesScriptsPage scripts = openScripts();
        SidesScriptsPage.Card before = scripts.find(scriptWithFile);
        assumeTrue(before != null, "\"" + scriptWithFile + "\" is no longer listed");

        assumeTrue(scripts.tapReplace(scriptWithFile),
                "Replace is not offered on \"" + scriptWithFile + "\"");
        assumeTrue(scripts.systemPickerShown(), "The system file picker did not open");
        assumeTrue(scripts.pickFromSystemPicker(SCRIPT_PDF, Duration.ofSeconds(20)),
                "\"" + SCRIPT_PDF + "\" could not be picked");
        sleep(14000);

        SidesScriptsPage after = openScripts();
        SidesScriptsPage.Card card =
                after.waitForPageCount(scriptWithFile, SCRIPT_PDF_PAGES, Duration.ofSeconds(90));
        assumeTrue(card != null, "\"" + scriptWithFile + "\" vanished after the replace");

        // Same file, second version. Both halves matter: the version has to move on, and
        // the page count has to be right on this version too — if it is right here and
        // wrong on the first upload, the parse is only running on replaces.
        assertNotEquals(String.valueOf(before.meta), String.valueOf(card.meta),
                "Replacing the file left the card reading exactly as before: \""
                        + card.meta + "\"");
        assertTrue(String.valueOf(card.meta).contains(SCRIPT_PDF_PAGES + " page"),
                "After replacing with the same " + SCRIPT_PDF_PAGES + "-page file the card"
                        + " reads \"" + card.meta + "\" (it read \"" + before.meta
                        + "\" before the replace)");
    }

    @Test
    @Order(79)
    @DisplayName("SI-16 A list taller than the screen is read in full, not just its first window")
    void longListIsReadInFull() {
        SidesScriptsPage scripts = openScripts();
        List<String> made = new ArrayList<>();
        if (scriptNoFile != null) made.add(scriptNoFile);

        // A script card carries a title, a meta line, five buttons and its embedded pages,
        // so three of them do not fit on one screen. That is the point: a reader that only
        // ever sees the first window would report the last card as missing, and a cleanup
        // sweep would call the project clean with fixtures still sitting below the fold.
        for (int i = 0; i < 3; i++) {
            String name = fixtureName("Deep" + i);
            if (!scripts.openAddScript()) break;
            if (!scripts.setScriptName(name)) break;
            if (!scripts.saveScript()) break;
            sleep(6000);
            made.add(name);
            extraScripts.add(name);
            scripts = openScripts();
        }
        assumeTrue(made.size() >= 3,
                "Only " + made.size() + " script(s) could be created, so a list longer than"
                        + " the screen could not be built");

        SidesScriptsPage after = openScripts();
        List<String> titles = after.titles();
        List<String> missing = made.stream().filter(n -> !titles.contains(n)).toList();
        assertTrue(missing.isEmpty(),
                "Of " + made.size() + " scripts on the project the reader found "
                        + (made.size() - missing.size()) + ". Missing: " + missing
                        + ". It read: " + titles);

        // And the last one must be reachable, not merely countable: an action on a row
        // below the fold has to scroll to it before it can tap anything.
        String last = made.get(made.size() - 1);
        assertTrue(after.find(last) != null && after.tapDelete(last),
                "\"" + last + "\" was read but its delete could not be tapped, so rows"
                        + " past the first window can be listed and not acted on");
        sleep(1200);
        new SidesListPage(driver).confirmDelete();
        sleep(3000);
        extraScripts.remove(last);
    }

    // ── from the QA sheet's Android column: defects a human confirmed by hand ──

    @Test
    @Order(76)
    @DisplayName("SI-17 A one-page script reads \"1 page\", not \"1 pages\"")
    void onePageReadsSingular() {
        SidesScriptsPage scripts = openScripts();
        String name = fixtureName("OnePage");
        assumeTrue(scripts.openAddScript(), "Add Script is not offered");
        assumeTrue(scripts.setScriptName(name), "The name could not be typed");
        assumeTrue(scripts.saveScript(), "Save was not clickable");
        sleep(6000);
        extraScripts.add(name);

        scripts = openScripts();
        assumeTrue(scripts.tapReplace(name), "Upload Script is not offered on \"" + name + "\"");
        assumeTrue(scripts.systemPickerShown(), "The system file picker did not open");
        // The call-sheet fixture is a single page, which is all this case needs.
        assumeTrue(scripts.pickFromSystemPicker(CALL_SHEET_PDF, Duration.ofSeconds(20)),
                "\"" + CALL_SHEET_PDF + "\" could not be picked");
        sleep(14000);

        SidesScriptsPage after = openScripts();
        SidesScriptsPage.Card card = after.waitForPageCount(name, 1, Duration.ofSeconds(90));
        assumeTrue(card != null && card.meta != null,
                "\"" + name + "\" has no meta line to read");

        // The plural is hard-coded in the adapter, so a one-page script announces itself as
        // "1 pages". Small, but it is on the card the unit reads to see what it is holding.
        assertFalse(card.meta.contains("1 pages"),
                "A one-page script reads \"" + card.meta + "\" — the plural is hard-coded,"
                        + " so one page is reported as \"1 pages\"");
    }

    @Test
    @Order(135)
    @DisplayName("SI-18 The scene picker lists exactly the scenes in the file, and no others")
    void pickerListsOnlyRealScenes() {
        assumeTrue(scriptWithFile != null, "SI-13 uploaded no script file");
        SidesGeneratePage gen = openGenerate();
        List<SidesGeneratePage.Picker> pickers = gen.pickers();
        assumeTrue(!pickers.isEmpty(), "The form offers no versions");
        String label = pickers.get(0).label;
        assumeTrue(gen.togglePicker(label), "\"" + label + "\" could not be expanded");

        SidesGeneratePage.Picker open = gen.pickers().stream()
                .filter(p -> label.equals(p.label)).findFirst().orElse(null);
        assumeTrue(open != null && !open.scenes.isEmpty(), "No scenes were listed");

        // The fixture's five pages carry scene headings 1 to 5 and nothing else, so the
        // picker's list is checkable against the file rather than against itself. A scene
        // the parser invents puts a number on screen that the script does not contain —
        // and a side generated from it is pages the unit was never meant to get.
        List<String> numbers = open.scenes.stream()
                .map(sc -> sc.replaceAll("[^0-9]", ""))
                .filter(sc -> !sc.isEmpty())
                .toList();
        List<String> unexpected = numbers.stream()
                .filter(nx -> Integer.parseInt(nx) < 1 || Integer.parseInt(nx) > SCRIPT_PDF_PAGES)
                .toList();
        assertTrue(unexpected.isEmpty(),
                "The picker offers scene(s) " + unexpected + " which are not in the script."
                        + " It listed: " + open.scenes);
        assertEquals(SCRIPT_PDF_PAGES, numbers.size(),
                "The script has " + SCRIPT_PDF_PAGES + " numbered scenes and the picker"
                        + " lists " + numbers.size() + ": " + open.scenes);
        backIntoSides();
    }

    @Test
    @Order(77)
    @DisplayName("SI-19 A Final Draft upload shows on the card without leaving the tab")
    void fdxUploadRefreshesTheList() {
        SidesScriptsPage scripts = openScripts();
        String name = fixtureName("Fdx");
        assumeTrue(scripts.openAddScript(), "Add Script is not offered");
        assumeTrue(scripts.setScriptName(name), "The name could not be typed");
        assumeTrue(scripts.saveScript(), "Save was not clickable");
        sleep(6000);
        extraScripts.add(name);

        scripts = openScripts();
        assumeTrue(scripts.tapReplace(name), "Upload Script is not offered on \"" + name + "\"");
        assumeTrue(scripts.systemPickerShown(), "The system file picker did not open");
        assumeTrue(scripts.pickFromSystemPicker(SCRIPT_FDX, Duration.ofSeconds(20)),
                "\"" + SCRIPT_FDX + "\" could not be picked. The module's picker accepts"
                        + " .pdf and .fdx, and the runner pushes both.");

        // Deliberately WITHOUT leaving the tab and without a pull-to-refresh: the question
        // is whether the list re-reads itself once the upload lands. Switching tabs calls
        // the fragment's onResume, which reloads, and would hide the very thing being asked.
        SidesScriptsPage inPlace = new SidesScriptsPage(driver);
        boolean refreshed = false;
        for (int i = 0; i < 12 && !refreshed; i++) {
            sleep(3000);
            SidesScriptsPage.Card c = inPlace.find(name);
            refreshed = c != null && c.hasFile;
        }

        // Prove the upload itself worked before blaming the list: come back to the tab and
        // look again. If it is here now, the file went up and only the refresh was missing.
        SidesScriptsPage reopened = openScripts();
        SidesScriptsPage.Card afterTrip = reopened.find(name);
        assumeTrue(afterTrip != null && afterTrip.hasFile,
                "The .fdx never attached at all, even after reopening the tab — so this is"
                        + " an upload problem, not a refresh one: " + afterTrip);

        assertTrue(refreshed,
                "The .fdx uploaded but the card only showed it after leaving the Script tab"
                        + " and coming back. Until then the script still offered"
                        + " \"Upload Script\", so the file looked lost.");
    }

    // ───────────────────────────── the gates ─────────────────────────────

    @Test
    @Order(80)
    @DisplayName("SI-20 Generate opens once any script exists")
    void generateGateOpens() {
        assumeTrue(scriptNoFile != null, "No script exists, so the gate cannot be tested");
        SidesListPage list = openList();
        assumeTrue(list.tapGenerate(), "Generate Sides could not be tapped");
        sleep(5000);
        SidesGeneratePage gen = new SidesGeneratePage(driver);
        assertTrue(gen.isLoaded(Duration.ofSeconds(15)),
                "Generate did not open although a script exists. On screen: " + whereAreWe());
        backIntoSides();
    }

    @Test
    @Order(90)
    @DisplayName("SI-21 Autogenerate opens only once a script has a FILE")
    void autogenerateGateOpens() {
        assumeTrue(scriptWithFile != null, "SI-13 uploaded no script file");
        SidesListPage list = openList();
        assumeTrue(list.tapAutogenerate(), "Autogenerate Sides could not be tapped");
        sleep(4000);
        SidesGeneratePage sheet = new SidesGeneratePage(driver);
        assertTrue(sheet.sheetShown(),
                "Autogenerate did not open although a script with a file exists."
                        + " Its gate wants a PUBLISHED script, which is one with a file.");
        assertTrue(String.valueOf(sheet.activeScript()).contains(scriptWithFile),
                "The sheet names \"" + sheet.activeScript() + "\" as the active script,"
                        + " not \"" + scriptWithFile + "\"");
        dismissSheet();
    }

    // ───────────────────────── the Generate form ─────────────────────────

    @Test
    @Order(100)
    @DisplayName("SI-30 The form opens with the script, the pickers and the display modes")
    void generateFormShape() {
        SidesGeneratePage gen = openGenerate();
        assertTrue(gen.scriptChipsShown(), "The form shows no selected script");
        assertTrue(gen.rearrangeShown(),
                "Rearrange is not offered. It belongs on BOTH display modes, so it should"
                        + " be here before a mode is even chosen.");
        assertTrue(gen.crossoutSelected(),
                "Cross-out is the documented default and the form opened on something else");
        backIntoSides();
    }

    @Test
    @Order(110)
    @DisplayName("SI-31 Submitting with nothing picked is refused")
    void submitNothingRefused() {
        SidesGeneratePage gen = openGenerate();
        assumeTrue(gen.submit(), "Generate Sides was not clickable");
        sleep(3000);
        assertTrue(gen.formShown(),
                "An empty selection generated a side. A side with no scenes is a blank"
                        + " hand-out.");
        backIntoSides();
    }

    @Test
    @Order(120)
    @DisplayName("SI-32 Rearrange reveals a way to set the order, and hides it again")
    void rearrangeTogglesOrderInput() {
        assumeTrue(scriptWithFile != null, "SI-13 uploaded no script file");
        SidesGeneratePage gen = openGenerate();

        // A scene has to be picked BEFORE rearrange is touched, and that omission is why
        // this case used to pass and fail across runs with no code change between them.
        // The app draws the draggable chips only when there is something to order
        // (rvOrderChips = rearrangeOn && orderChips.isNotEmpty()) and the single text
        // field only in single-script mode (tilOrder = rearrangeOn && !isMultiScript).
        // With no scene picked AND more than one script selected, hiding both is correct
        // behaviour — so the old assertion was measuring a state it had never established,
        // and read the app's correct answer as a defect. Picking one scene makes a control
        // mandatory in either mode, which is what the case is actually about.
        List<SidesGeneratePage.Picker> pickers = gen.pickers();
        assumeTrue(!pickers.isEmpty(), "The form offers no versions, so no scene can be picked");
        String label = pickers.get(0).label;
        assumeTrue(gen.togglePicker(label), "\"" + label + "\" could not be expanded");
        SidesGeneratePage.Picker open = gen.pickers().stream()
                .filter(p -> label.equals(p.label)).findFirst().orElse(null);
        assumeTrue(open != null && !open.scenes.isEmpty(),
                "\"" + label + "\" offers no scene to pick");
        assumeTrue(gen.pickScene(label, open.scenes.get(0)), "Scene could not be picked");
        sleep(1500);

        boolean before = gen.orderControlsShown();
        assumeTrue(gen.toggleRearrange(), "Rearrange could not be toggled");
        sleep(1500);
        boolean after = gen.orderControlsShown();
        assertTrue(before != after,
                "With a scene picked, Rearrange was toggled and the order controls stayed "
                        + (before ? "visible" : "hidden") + ". One of the two shapes must"
                        + " appear — a single order field in single-script mode, draggable"
                        + " chips otherwise. Rearrange block: " + gen.rearrangeDiag());
        gen.toggleRearrange();
        sleep(1200);
        backIntoSides();
    }

    @Test
    @Order(130)
    @DisplayName("SI-33 Expanding a version lists the scenes found in its PDF")
    void versionScenesLoad() {
        assumeTrue(scriptWithFile != null, "SI-13 uploaded no script file");
        SidesGeneratePage gen = openGenerate();
        List<SidesGeneratePage.Picker> pickers = gen.pickers();
        assumeTrue(!pickers.isEmpty(), "The form offers no versions to expand");

        String label = pickers.get(0).label;
        assumeTrue(gen.togglePicker(label), "\"" + label + "\" could not be expanded");

        SidesGeneratePage.Picker open = gen.pickers().stream()
                .filter(p -> label.equals(p.label)).findFirst().orElse(null);
        assertNotNull(open, "\"" + label + "\" disappeared when expanded");
        // The fixture holds five numbered scenes. An expanded picker with none is either a
        // parse failure or — the case this catches — an expanded card that says nothing at
        // all about why it is empty.
        assertFalse(open.scenes.isEmpty(),
                "Expanding \"" + label + "\" listed no scenes, and the card shows no reason"
                        + " why. The script PDF carries five numbered scene headings.");
        backIntoSides();
    }

    @Test
    @Order(140)
    @DisplayName("SI-34 Picking scenes is counted in the summary")
    void pickingScenesCounts() {
        assumeTrue(scriptWithFile != null, "SI-13 uploaded no script file");
        SidesGeneratePage gen = openGenerate();
        List<SidesGeneratePage.Picker> pickers = gen.pickers();
        assumeTrue(!pickers.isEmpty(), "The form offers no versions");
        String label = pickers.get(0).label;
        assumeTrue(gen.togglePicker(label), "\"" + label + "\" could not be expanded");

        SidesGeneratePage.Picker open = gen.pickers().stream()
                .filter(p -> label.equals(p.label)).findFirst().orElse(null);
        assumeTrue(open != null && open.scenes.size() >= 2,
                "Fewer than two scenes to pick from");

        assumeTrue(gen.pickScene(label, open.scenes.get(0)), "Scene could not be picked");
        assumeTrue(gen.pickScene(label, open.scenes.get(1)), "Scene could not be picked");
        sleep(1500);

        String count = String.valueOf(gen.summaryCount());
        assertTrue(count.contains("2"),
                "Two scenes were picked and the summary reads \"" + count + "\"");
        backIntoSides();
    }

    // ──────────────────── generate, review, publish ────────────────────

    @Test
    @Order(150)
    @DisplayName("SI-35 A side generates, the review offers its three actions, and Publish lands it")
    void generateReviewAndPublish() {
        assumeTrue(scriptWithFile != null, "SI-13 uploaded no script file");
        SidesGeneratePage gen = openGenerate();
        List<SidesGeneratePage.Picker> pickers = gen.pickers();
        assumeTrue(!pickers.isEmpty(), "The form offers no versions");
        String label = pickers.get(0).label;
        assumeTrue(gen.togglePicker(label), "\"" + label + "\" could not be expanded");

        SidesGeneratePage.Picker open = gen.pickers().stream()
                .filter(p -> label.equals(p.label)).findFirst().orElse(null);
        assumeTrue(open != null && !open.scenes.isEmpty(), "No scenes to pick");
        for (String sc : open.scenes.subList(0, Math.min(3, open.scenes.size()))) {
            gen.pickScene(label, sc);
        }

        String candidate = fixtureName("Side");
        gen.setTitle(candidate);
        assumeTrue(gen.submit(), "Generate Sides was not clickable");

        // Generate, review and publish in ONE case on purpose. The review is a stage of
        // this activity, not a screen of its own: it cannot be navigated back to, and
        // splitting it across cases made the later ones skip for want of a review the
        // earlier one had already consumed.
        assertTrue(gen.waitForReady(Duration.ofSeconds(120)),
                "Generation did not reach the ready stage."
                        + (gen.failedShown() ? " It failed with: " + gen.failedMessage() : ""));

        assertTrue(gen.reviewViewShown() && gen.reviewDownloadShown() && gen.reviewPublishShown(),
                "The ready review does not offer all of View / Download / Publish"
                        + " (view=" + gen.reviewViewShown()
                        + ", download=" + gen.reviewDownloadShown()
                        + ", publish=" + gen.reviewPublishShown() + ")");

        // Asserting the CURRENT behaviour, and saying why: btnMove exists in the layout,
        // the activity wires a click listener to it and the chain behind it is implemented
        // down to the Document Distribution call — but it is declared visibility="gone"
        // and nothing ever shows it. If this starts failing, the path was switched on.
        assertFalse(gen.moveToDocDistShown(),
                "\"Move to Document Distribution\" is now offered in the review. It used to"
                        + " be permanently hidden although fully implemented — if that was"
                        + " fixed on purpose, this assertion should be inverted.");

        // The QA sheet records Download on this screen opening Chrome and saving nothing on
        // Android, while web saved the file. Tapped here, inside the case that owns a ready
        // review, because the review cannot be navigated back to once it is gone.
        if (gen.reviewDownload()) {
            sleep(7000);
            String pkg;
            try { pkg = String.valueOf(driver.getCurrentPackage()); }
            catch (Exception e) { pkg = "(unreadable)"; }
            assertFalse(pkg.contains("chrome") || pkg.contains("browser"),
                    "Download on the review handed the side to " + pkg + " instead of"
                            + " saving it. The side is the unit's hand-out; it has to land"
                            + " on the device, not in a browser tab.");
            if (!pkg.contains("zillit")) backIntoSides();
        }

        assumeTrue(gen.reviewPublish(), "Publish was not clickable");
        sleep(9000);

        SidesListPage list = openList();
        assertNotNull(list.find(candidate),
                "\"" + candidate + "\" was published and is not on the list: "
                        + list.titles());
        sideTitle = candidate;
    }

    // ─────────────────────────── the list row ───────────────────────────

    @Test
    @Order(180)
    @DisplayName("SI-40 The row carries a title, a status and a meta line")
    void rowShape() {
        assumeTrue(sideTitle != null, "No side was published to look at");
        SidesListPage list = openList();
        SidesListPage.Row row = list.find(sideTitle);
        assumeTrue(row != null, "\"" + sideTitle + "\" is not on the list: " + list.titles());
        assertNotNull(row.status, "The row shows no status: " + row);
        assertNotNull(row.meta, "The row shows no meta line: " + row);
    }

    @Test
    @Order(190)
    @DisplayName("SI-41 The row's scene list and its scene count agree")
    void rowMetaIsConsistent() {
        assumeTrue(sideTitle != null, "No side was published to look at");
        SidesListPage list = openList();
        SidesListPage.Row row = list.find(sideTitle);
        assumeTrue(row != null && row.meta != null, "No meta line to read");

        // The row prints the scene numbers and a count as two separate facts, straight from
        // two backend fields, without reconciling them. When they disagree the user is told
        // "Scenes: 1, 3, 5 · 1 scene(s)" in one breath.
        java.util.regex.Matcher scenes =
                java.util.regex.Pattern.compile("Scenes:\\s*([0-9,\\s]+)").matcher(row.meta);
        java.util.regex.Matcher count =
                java.util.regex.Pattern.compile("(\\d+)\\s+scene\\(s\\)").matcher(row.meta);
        assumeTrue(scenes.find() && count.find(),
                "The meta line carries no scene list and count to compare: " + row.meta);

        int listed = scenes.group(1).split(",").length;
        int stated = Integer.parseInt(count.group(1));
        assertEquals(listed, stated,
                "The row lists " + listed + " scene(s) and then says there are " + stated
                        + ": \"" + row.meta + "\"");
    }

    @Test
    @Order(200)
    @DisplayName("SI-42 The row overflow offers all four actions on a side with a PDF")
    void rowMenuComplete() {
        assumeTrue(sideTitle != null, "No side was published to look at");
        SidesListPage list = openList();
        assumeTrue(list.openRowMenu(sideTitle), "The row overflow could not be opened");

        List<String> options = list.rowMenuOptions();
        for (String wanted : List.of(SidesListPage.ACTION_VIEW, SidesListPage.ACTION_DOWNLOAD,
                SidesListPage.ACTION_SEND_DD, SidesListPage.ACTION_DELETE)) {
            assertTrue(options.stream().anyMatch(o -> o.contains(wanted)),
                    "The row menu does not offer \"" + wanted + "\". It offers: " + options);
        }
        list.dismissRowMenu();
    }

    @Test
    @Order(210)
    @DisplayName("SI-43 View opens the side in the in-app viewer")
    void viewOpensTheViewer() {
        assumeTrue(sideTitle != null, "No side was published to look at");
        SidesListPage list = openList();
        assumeTrue(list.openRowMenu(sideTitle), "The row overflow could not be opened");
        assumeTrue(list.chooseRowAction(SidesListPage.ACTION_VIEW), "View could not be chosen");
        sleep(8000);

        String activity;
        try { activity = String.valueOf(driver.currentActivity()); }
        catch (Exception e) { activity = "(unreadable)"; }
        assertTrue(activity.contains("SidesWebView"),
                "View went to " + activity + " instead of the in-app PDF viewer");
        backIntoSides();
    }

    @Test
    @Order(220)
    @DisplayName("SI-44 Delete asks first, and Cancel keeps the side")
    void deleteAsksFirst() {
        assumeTrue(sideTitle != null, "No side was published to look at");
        SidesListPage list = openList();
        assumeTrue(list.openRowMenu(sideTitle), "The row overflow could not be opened");
        assumeTrue(list.chooseRowAction(SidesListPage.ACTION_DELETE), "Delete could not be chosen");

        assertTrue(list.deleteConfirmShown(Duration.ofSeconds(6)),
                "Delete did not ask for confirmation. A side is the unit's hand-out for a"
                        + " shoot day, and a mis-tap would take it with no warning.");
        assumeTrue(list.cancelDelete(), "Cancel was not available");
        sleep(2500);

        SidesListPage after = openList();
        assertNotNull(after.find(sideTitle),
                "Cancelling the delete removed \"" + sideTitle + "\" anyway: "
                        + after.titles());
    }

    @Test
    @Order(230)
    @DisplayName("SI-45 Expanding History shows its rows, or says there are none")
    void historyExpands() {
        SidesListPage list = openList();
        assumeTrue(list.historyShown(), "The History section is not on screen");
        assumeTrue(list.toggleHistory(), "History could not be toggled");
        sleep(4000);

        // The fragment sets rvHistory VISIBLE when expanded — but an empty RecyclerView
        // has no children and therefore no height, so it never reaches the accessibility
        // tree and nothing at all appears. Expanded-with-rows and expanded-but-empty are
        // indistinguishable to the user: the chevron turns and the screen does not change.
        boolean rows = list.historyRowCount() > 0;
        boolean explained = list.emptyExplanationShown("history")
                || list.emptyExplanationShown("side");
        assertTrue(rows || explained,
                "History was expanded and showed neither a row nor a line saying it is"
                        + " empty, so there is no way to tell it apart from a failed load.");
        list.toggleHistory();
        sleep(1500);
    }

    // ───────────────────────── the Autogenerate sheet ─────────────────────────

    @Test
    @Order(250)
    @DisplayName("SI-52 Each section's heading sits above the controls it heads")
    void sectionHeadingsAreAboveTheirControls() {
        assumeTrue(scriptWithFile != null, "SI-13 uploaded no script file");
        SidesGeneratePage sheet = openAutogenerate();

        Integer uploadY = sheet.topOf("tvUploadSchedule");
        Integer headingY = sheet.topOfText("SCHEDULE");
        assumeTrue(uploadY != null && headingY != null,
                "The SCHEDULE section is not on screen (heading=" + headingY
                        + ", upload=" + uploadY + ")");

        // "+ Upload new" is the SCHEDULE section's action, so it has to render below the
        // SCHEDULE heading. Drawn above it, it reads as the action of the section before.
        assertTrue(headingY < uploadY,
                "The SCHEDULE heading is at y=" + headingY + " and its own \"+ Upload new\""
                        + " is at y=" + uploadY + ", i.e. above it, so the action looks like"
                        + " it belongs to the section above.");
        dismissSheet();
    }

    @Test
    @Order(270)
    @DisplayName("SI-53 An empty call-sheet list says it is empty")
    void callSheetEmptyState() {
        assumeTrue(scriptWithFile != null, "SI-13 uploaded no script file");
        SidesGeneratePage sheet = openAutogenerate();
        assumeTrue(sheet.callSheetRows().isEmpty(),
                "This project already has call sheets: " + sheet.callSheetRows());
        assertTrue(sheet.emptyExplanationShown("call sheet"),
                "The call-sheet list is empty and nothing says so, so a project with no"
                        + " call sheets looks the same as a list that failed to load.");
        dismissSheet();
    }

    @Test
    @Order(280)
    @DisplayName("SI-54 An empty schedule list says it is empty")
    void scheduleEmptyState() {
        assumeTrue(scriptWithFile != null, "SI-13 uploaded no script file");
        SidesGeneratePage sheet = openAutogenerate();
        assumeTrue(sheet.scheduleRows().isEmpty(),
                "This project already has schedules: " + sheet.scheduleRows());
        assertTrue(sheet.emptyExplanationShown("schedule"),
                "The schedule list is empty and nothing says so.");
        dismissSheet();
    }


    @Test
    @Order(260)
    @DisplayName("SI-50 Autogenerate with no call sheet is refused")
    void autogenerateNeedsACallSheet() {
        assumeTrue(scriptWithFile != null, "SI-13 uploaded no script file");
        SidesGeneratePage sheet = openAutogenerate();
        assumeTrue(!sheet.callSheetsShown() || sheet.callSheetRows().isEmpty(),
                "This project already has call sheets, so 'none selected' cannot be tested");
        assumeTrue(sheet.submit(), "Autogenerate Sides was not clickable");
        sleep(3000);
        assertTrue(sheet.sheetShown(),
                "Autogenerate ran with no call sheet chosen. The call sheet is what tells"
                        + " it which scenes the day covers.");
        dismissSheet();
    }

    @Test
    @Order(290)
    @DisplayName("SI-51 A call sheet uploads and is then offered for selection")
    void callSheetUploads() {
        assumeTrue(scriptWithFile != null, "SI-13 uploaded no script file");
        SidesGeneratePage sheet = openAutogenerate();
        assumeTrue(sheet.uploadCallSheet(), "Upload new is not offered for call sheets");

        SidesScriptsPage picker = new SidesScriptsPage(driver);
        assumeTrue(picker.systemPickerShown(), "The system file picker did not open");
        assumeTrue(picker.pickFromSystemPicker(CALL_SHEET_PDF, Duration.ofSeconds(20)),
                "\"" + CALL_SHEET_PDF + "\" could not be picked");
        sleep(14000);

        SidesGeneratePage after = new SidesGeneratePage(driver);
        assumeTrue(after.sheetShown(), "The sheet closed during the upload");
        // Asserted by NAME, not by a row count: the sheet shows only the most recent
        // published call sheet alongside the uploaded ones, and a bottom sheet only ever
        // renders the rows that fit, so counting rows says nothing about what was added.
        List<String> rows = after.callSheetRows();
        assertTrue(rows.stream().anyMatch(r -> r.contains(CALL_SHEET_PDF)),
                "\"" + CALL_SHEET_PDF + "\" was uploaded and the call-sheet list offers: "
                        + rows);
        dismissSheet();
    }

    // ─────────────────────────── cleanup ───────────────────────────

    @Test
    @Order(400)
    @DisplayName("SI-60 Cleanup: no ZL_AUTO side or script is left on the project")
    void cleanup() {
        // Sweeps every fixture this suite has ever left, not only this run's: a run that
        // broke halfway leaves a script behind, and those pile up on a real project.
        //
        // A delete dialog left standing would block every tap below it, so clear one first.
        SidesListPage opening = new SidesListPage(driver);
        if (opening.deleteConfirmShown(Duration.ofSeconds(2))) {
            opening.cancelDelete();
            sleep(1200);
        }
        int sides = 0;
        for (int pass = 0; pass < 12; pass++) {
            SidesListPage list = openList();
            String victim = null;
            for (String t : list.titles()) {
                if (t != null && t.startsWith(FIXTURE_PREFIX)) { victim = t; break; }
            }
            if (victim == null) break;
            if (!list.openRowMenu(victim)
                    || !list.chooseRowAction(SidesListPage.ACTION_DELETE)
                    || !list.deleteConfirmShown(Duration.ofSeconds(6))
                    || !list.confirmDelete()) {
                System.out.println(">>> SI-60 could not delete side \"" + victim + "\"");
                break;
            }
            sleep(3500);
            System.out.println(">>> SI-60 deleted side \"" + victim + "\"");
            sides++;
        }
        sideTitle = null;

        int scripts = 0;
        for (int pass = 0; pass < 12; pass++) {
            SidesScriptsPage page = openScripts();
            String victim = null;
            for (String t : page.titles()) {
                if (t != null && t.startsWith(FIXTURE_PREFIX)) { victim = t; break; }
            }
            if (victim == null) break;
            if (!page.tapDelete(victim)) {
                System.out.println(">>> SI-60 could not delete script \"" + victim + "\"");
                break;
            }
            sleep(1200);
            new SidesListPage(driver).confirmDelete();
            sleep(3500);
            System.out.println(">>> SI-60 deleted script \"" + victim + "\"");
            scripts++;
        }
        scriptNoFile = null;
        scriptWithFile = null;

        System.out.println(">>> SI-60 removed " + sides + " side(s) and " + scripts
                + " script(s)");
        assertTrue(openList().titles().stream().noneMatch(t -> t.startsWith(FIXTURE_PREFIX)),
                "Fixture sides are still on the project: " + openList().titles());
        assertTrue(openScripts().titles().stream().noneMatch(t -> t.startsWith(FIXTURE_PREFIX)),
                "Fixture scripts are still on the project: " + openScripts().titles());
    }

    // ─────────────────────────── helpers ───────────────────────────

    private SidesListPage openList() {
        openSides();
        SidesListPage list = new SidesListPage(driver);
        if (!list.onSidesTab()) {
            list.openSidesTab();
            sleep(2500);
        }
        assumeTrue(list.isLoaded(Duration.ofSeconds(20)), "The Sides list did not load");
        return list;
    }

    private SidesScriptsPage openScripts() {
        openSides();
        SidesListPage list = new SidesListPage(driver);
        if (!list.onScriptTab()) {
            assumeTrue(list.openScriptTab(), "The Script tab could not be tapped");
            sleep(2500);
        }
        SidesScriptsPage scripts = new SidesScriptsPage(driver);
        assumeTrue(scripts.isLoaded(Duration.ofSeconds(20)), "The Script tab did not load");
        return scripts;
    }

    private SidesGeneratePage openGenerate() {
        SidesGeneratePage gen = new SidesGeneratePage(driver);
        if (!gen.formShown()) {
            SidesListPage list = openList();
            assumeTrue(list.tapGenerate(), "Generate Sides could not be tapped");
            sleep(5000);
            assumeTrue(gen.isLoaded(Duration.ofSeconds(20)), "The Generate screen did not open");
        }
        // Say WHICH script this is about. The form chooses its own starting selection, and
        // on a project holding several scripts that can be one with no file — which has no
        // versions at all, so every scene case after it skipped for want of something the
        // case had never actually asked for.
        if (scriptWithFile != null && !gen.ensureScriptSelected(scriptWithFile)) {
            System.out.println(">>> could not select \"" + scriptWithFile + "\" on the form;"
                    + " it is showing: " + gen.selectedScripts());
        }
        return gen;
    }

    private SidesGeneratePage openAutogenerate() {
        SidesGeneratePage sheet = new SidesGeneratePage(driver);
        if (sheet.sheetShown()) return sheet;
        SidesListPage list = openList();
        assumeTrue(list.tapAutogenerate(), "Autogenerate Sides could not be tapped");
        sleep(5000);
        assumeTrue(sheet.sheetShown(), "The Autogenerate sheet did not open");
        return sheet;
    }

    /** Closes the bottom sheet without leaving the module. */
    private void dismissSheet() {
        try { driver.navigate().back(); } catch (Exception ignored) { }
        sleep(1500);
    }
}
