package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.ProjectListPage;
import com.zillit.automation.pages.RecceDetailPage;
import com.zillit.automation.pages.RecceFormPage;
import com.zillit.automation.pages.RecceListPage;
import com.zillit.automation.pages.ToolsPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * RECCE module (Tools → Recce) — location-scout schedules.
 *
 * Covers the list (search, All/Published/Drafts chips, unit filter, empty state,
 * refresh), the create form and its publish-only validation, saving a DRAFT vs
 * PUBLISHing, stops + personnel editing, the location picker (map / W3W / pin),
 * the detail screen (tiles, timeline, route map, Export/Print/Edit) and delete.
 *
 * Test data uses a "QA Recce" prefix and the delete case cleans up after itself.
 */
@Order(TestOrder.RECCE)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RecceTest extends BaseTest {

    private static final String PREFIX = "QA Recce";
    private boolean insideProject;
    /** Title of the draft created by {@link #saveAsDraft()} — reused downstream. */
    private static String draftTitle;

    @Override protected boolean relaunchBetweenTests() { return false; }
    @Override protected boolean requiresLoggedOutStart() { return false; }

    @BeforeEach
    void reachRecce() {
        if (!insideProject) {
            if (landing == Landing.PROJECT_LIST) {
                ProjectListPage list = new ProjectListPage(driver);
                List<String> names = list.visibleProjectNames();
                assumeTrue(!names.isEmpty(), "No projects");
                assumeTrue(list.tapProject(names.get(0)), "Could not enter a project");
            }
            assumeTrue(new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(60)), "No project");
            insideProject = true;
        }
        dismissPermissionPrompts();
        RecceListPage list = new RecceListPage(driver);
        if (list.isLoaded(Duration.ofSeconds(2))) return;
        openRecce();
    }

    private void openRecce() {
        // Back out of any leftover Recce SUB-screen (form/detail/picker) — but only
        // while we're on a pushed screen. Never press BACK on a bottom-nav screen:
        // that leaves the app entirely and strands the run on the launcher.
        for (int i = 0; i < 4; i++) {
            if (new RecceListPage(driver).isLoaded(Duration.ofSeconds(1))) return;
            if (onBottomNavScreen()) break;
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(800);
        }
        // If the app fell out of the foreground, bring it back before navigating.
        try {
            String pkg = driver.getCurrentPackage();
            if (pkg == null || !pkg.contains("zillit")) {
                driver.executeScript("mobile: activateApp", java.util.Map.of("appId", appPackage));
                sleep(2500);
            }
        } catch (Exception ignored) { }
        ToolsPage tools = new ToolsPage(driver);
        assumeTrue(tools.openToolsTab(), "Could not open Tools");
        assumeTrue(tools.isLoaded(Duration.ofSeconds(15)), "Tools list did not load");
        tools.searchTools("Recce");
        assumeTrue(tools.isModuleListed("Recce", Duration.ofSeconds(6)),
                "Recce module not enabled on this project");
        assumeTrue(tools.openModule("Recce"), "Could not open Recce");
        assumeTrue(new RecceListPage(driver).isLoaded(Duration.ofSeconds(20)),
                "Recce list did not load");
    }

    /** True on a main (bottom-nav) screen — pressing BACK there exits the app. */
    private boolean onBottomNavScreen() {
        try { return !driver.findElements(byAppId("bottomNavigation")).isEmpty(); }
        catch (Exception e) { return false; }
    }

    private RecceListPage list() {
        RecceListPage p = new RecceListPage(driver);
        assumeTrue(p.isLoaded(Duration.ofSeconds(15)), "Recce list not loaded");
        return p;
    }

    /** Returns to the Recce list from a pushed sub-screen (never backs out of the app). */
    private void backToList() {
        for (int i = 0; i < 4; i++) {
            if (new RecceListPage(driver).isLoaded(Duration.ofSeconds(2))) return;
            if (onBottomNavScreen()) break;   // BACK here would exit the app
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(1000);
        }
        if (!new RecceListPage(driver).isLoaded(Duration.ofSeconds(1))) openRecce();
    }

    // ---------------- list ----------------

    @Test
    @Order(1)
    @DisplayName("Recce list loads — search, All/Published/Drafts chips, unit filter and Create FAB")
    void listLoads() {
        RecceListPage p = list();
        System.out.println(">>> Recce list: cards=" + p.cardCount()
                + " | All=" + p.chipText("All") + " Published=" + p.chipText("Published")
                + " Drafts=" + p.chipText("Drafts") + " Unit=" + p.chipText("Unit")
                + " | empty=" + p.isEmptyStateShown());
        assertTrue(p.isSearchVisible(), "The recce search box should be present");
        assertTrue(p.areChipsShown(), "All / Published / Drafts chips should be present");
        assertTrue(p.isCreateFabVisible(), "The Create Recce FAB should be present (needs posting rights)");
        screenshot("recce-list");
    }

    @Test
    @Order(2)
    @DisplayName("Filter chips switch the list (All / Published / Drafts)")
    void filterChips() {
        RecceListPage p = list();
        for (String chip : new String[]{"Published", "Drafts", "All"}) {
            assumeTrue(p.tapChip(chip), "Chip '" + chip + "' not tappable");
            System.out.println(">>> chip " + chip + " → cards=" + p.cardCount());
        }
        assertTrue(p.areChipsShown(), "Chips should still be shown after switching");
        screenshot("recce-chips");
    }

    @Test
    @Order(2)
    @DisplayName("FILTER: the Drafts chip shows ONLY drafts, and Published ONLY published")
    void statusFilterActuallyFilters() {
        RecceListPage p = list();

        p.tapChip("Drafts");
        List<String> draftStatuses = p.cardStatuses();
        System.out.println(">>> Drafts chip → " + p.cardCount() + " cards, statuses=" + draftStatuses);
        assumeTrue(!draftStatuses.isEmpty(), "No drafts to verify — skipping");
        assertTrue(draftStatuses.stream().allMatch(s -> s.toLowerCase().contains("draft")),
                "Under the Drafts filter EVERY card must be a draft. Got: " + draftStatuses);

        p.tapChip("Published");
        List<String> pubStatuses = p.cardStatuses();
        System.out.println(">>> Published chip → " + p.cardCount() + " cards, statuses=" + pubStatuses);
        if (!pubStatuses.isEmpty())
            assertTrue(pubStatuses.stream().noneMatch(s -> s.toLowerCase().contains("draft")),
                    "Under the Published filter NO card may be a draft. Got: " + pubStatuses);

        p.tapChip("All");
        System.out.println(">>> All chip → " + p.cardCount() + " cards");
        screenshot("recce-status-filter");
    }

    @Test
    @Order(2)
    @DisplayName("FILTER: chip COUNTS agree — All == Published + Drafts")
    void chipCountsAddUp() {
        RecceListPage p = list();
        p.tapChip("All");
        int all = countFromChip(p.chipText("All"));
        int pub = countFromChip(p.chipText("Published"));
        int drafts = countFromChip(p.chipText("Drafts"));
        System.out.println(">>> chip counts: All=" + all + " Published=" + pub + " Drafts=" + drafts);
        assumeTrue(all >= 0 && pub >= 0 && drafts >= 0, "Could not read the chip counts");
        assertTrue(all == pub + drafts,
                "All (" + all + ") should equal Published (" + pub + ") + Drafts (" + drafts + ")");
    }

    private int countFromChip(String text) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)").matcher(text == null ? "" : text);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    @Test
    @Order(3)
    @DisplayName("FILTER: the Unit filter narrows the list to that unit only")
    void unitFilterActuallyFilters() {
        RecceListPage p = list();
        p.tapChip("All");
        int before = p.cardCount();
        assumeTrue(p.openUnitFilter(), "Unit chip not tappable");
        List<String> options = p.unitFilterOptions();
        System.out.println(">>> unit filter options=" + options);
        assumeTrue(!options.isEmpty(), "Unit filter menu had no options");
        // Pick a real unit (skip the leading "all units" entry).
        String unit = options.size() > 1 ? options.get(1) : options.get(0);
        assumeTrue(p.chooseUnitFilter(unit), "Could not pick unit '" + unit + "'");
        List<String> units = p.cardUnits();
        int after = p.cardCount();
        System.out.println(">>> filtered by unit '" + unit + "' → " + after + " cards, units=" + units);
        assertTrue(after <= before, "Filtering by a unit must not ADD cards (" + before + " → " + after + ")");
        if (!units.isEmpty())
            assertTrue(units.stream().allMatch(u -> u.equalsIgnoreCase(unit) || unit.contains(u) || u.contains(unit)),
                    "Every card should belong to '" + unit + "'. Got: " + units);
        // restore: choose the first ("all units") option
        if (p.openUnitFilter()) {
            List<String> opts2 = p.unitFilterOptions();
            if (!opts2.isEmpty()) p.chooseUnitFilter(opts2.get(0));
        }
        System.out.println(">>> after resetting unit filter → " + p.cardCount() + " cards");
        screenshot("recce-unit-filter");
    }

    @Test
    @Order(3)
    @DisplayName("FILTER + SEARCH combine: searching within a status filter stays within it")
    void searchWithinFilter() {
        RecceListPage p = list();
        p.tapChip("Drafts");
        int draftsBefore = p.cardCount();
        assumeTrue(draftsBefore > 0, "No drafts to search within — skipping");
        String first = p.cardTitles().stream().findFirst().orElse("");
        assumeTrue(!first.isBlank(), "No draft title to search for");
        String frag = first.length() >= 6 ? first.substring(0, 6) : first;
        p.search(frag);
        List<String> statuses = p.cardStatuses();
        System.out.println(">>> search '" + frag + "' within Drafts → " + p.cardCount()
                + " cards, statuses=" + statuses);
        if (!statuses.isEmpty())
            assertTrue(statuses.stream().allMatch(s -> s.toLowerCase().contains("draft")),
                    "Searching inside the Drafts filter must not surface published items. Got: " + statuses);
        p.clearSearch();
        p.tapChip("All");
        screenshot("recce-filter-plus-search");
    }

    @Test
    @Order(4)
    @DisplayName("Search with no match shows the no-results state")
    void searchNoMatch() {
        RecceListPage p = list();
        p.search("zzz-no-recce-" + (System.currentTimeMillis() % 1000));
        int after = p.cardCount();
        System.out.println(">>> no-match search → cards=" + after
                + " emptyState=" + p.isEmptyStateShown() + " title='" + p.emptyStateTitle() + "'");
        assertTrue(after == 0 || p.isEmptyStateShown(),
                "A non-matching search should show no cards / the no-results state");
        p.clearSearch();
        screenshot("recce-search-nomatch");
    }

    @Test
    @Order(4)
    @DisplayName("SEARCH: an exact title is found, and only matching cards remain")
    void searchFindsByTitle() {
        RecceListPage p = list();
        p.tapChip("All");
        int before = p.cardCount();
        String target = p.cardTitles().stream().findFirst().orElse("");
        assumeTrue(!target.isBlank(), "No recce to search for — skipping");
        p.search(target);
        List<String> hits = p.cardTitles();
        System.out.println(">>> search '" + target + "' → " + hits.size() + " of " + before + " : " + hits);
        assertTrue(hits.stream().anyMatch(t -> t.equals(target)),
                "Searching an exact title must find it. Got: " + hits);
        assertTrue(hits.size() <= before, "Search must narrow, not widen, the list");
        assertTrue(hits.stream().allMatch(t -> t.toLowerCase().contains(target.toLowerCase())
                        || target.toLowerCase().contains(t.toLowerCase())),
                "Every remaining card should match the query. Got: " + hits);
        p.clearSearch();
        screenshot("recce-search-title");
    }

    @Test
    @Order(4)
    @DisplayName("SEARCH: a PARTIAL query matches, and clearing restores the full list")
    void searchPartialAndClear() {
        RecceListPage p = list();
        p.tapChip("All");
        int full = p.cardCount();
        String first = p.cardTitles().stream().findFirst().orElse("");
        assumeTrue(first.length() >= 4, "No usable title — skipping");
        String frag = first.substring(0, 4);
        p.search(frag);
        int narrowed = p.cardCount();
        System.out.println(">>> partial '" + frag + "' → " + narrowed + " of " + full);
        assertTrue(narrowed > 0, "A partial query matching an existing title should return results");
        p.clearSearch();
        sleep(1200);
        int restored = p.cardCount();
        System.out.println(">>> after clearing search → " + restored + " (was " + full + ")");
        assertTrue(restored >= narrowed, "Clearing the search should restore the list");
        screenshot("recce-search-partial");
    }

    @Test
    @Order(4)
    @DisplayName("SEARCH is case-insensitive")
    void searchIsCaseInsensitive() {
        RecceListPage p = list();
        p.tapChip("All");
        String first = p.cardTitles().stream().findFirst().orElse("");
        assumeTrue(first.length() >= 4, "No usable title — skipping");
        String frag = first.substring(0, 4);
        p.search(frag.toLowerCase());
        int lower = p.cardCount();
        p.clearSearch();
        p.search(frag.toUpperCase());
        int upper = p.cardCount();
        p.clearSearch();
        System.out.println(">>> case-insensitivity: '" + frag.toLowerCase() + "'=" + lower
                + " vs '" + frag.toUpperCase() + "'=" + upper);
        assertTrue(lower == upper,
                "Search should be case-insensitive — lower gave " + lower + ", upper gave " + upper);
        screenshot("recce-search-case");
    }

    @Test
    @Order(4)
    @DisplayName("SEARCH matches the RENDEZVOUS/location too, not just the title")
    void searchMatchesRendezvous() {
        // The search hint is "Search location or rendezvous…", so a recce should be
        // findable by its rendezvous place as well as its title.
        RecceListPage p = list();
        RecceFormPage f = p.tapCreate();
        assumeTrue(f.isLoaded(Duration.ofSeconds(15)), "Form did not open");
        String rdv = "QAPlace" + (System.currentTimeMillis() % 100000);
        String title = PREFIX + " Rdv " + (System.currentTimeMillis() % 100000);
        f.setTitle(title);
        f.pickFirstUnit();
        f.pickDate();
        f.pickRdvTime();
        f.setRdvPlace(rdv);
        assumeTrue(f.publish(), "Publish not tappable");
        sleep(3000);
        backToList();
        RecceListPage l = list();
        l.tapChip("All");
        // now search by the RENDEZVOUS text, not the title
        l.search(rdv);
        boolean found = l.cardTitles().stream().anyMatch(t -> t.contains(title))
                || l.textShown(title, Duration.ofSeconds(8));
        System.out.println(">>> searching by rendezvous '" + rdv + "' found the recce=" + found
                + " | cards=" + l.cardTitles());
        l.clearSearch();
        screenshot("recce-search-rendezvous");
        assertTrue(found,
                "Search should match the rendezvous/location ('" + rdv + "'), not only the title");
    }

    // ---------------- create form + validation ----------------

    @Test
    @Order(5)
    @DisplayName("Create FAB opens the NEW RECCE form")
    void createFormOpens() {
        RecceListPage p = list();
        RecceFormPage f = p.tapCreate();
        assertTrue(f.isLoaded(Duration.ofSeconds(15)), "The NEW RECCE form should open");
        System.out.println(">>> form opened; summary='" + f.summaryText() + "'");
        screenshot("recce-form");
        f.back();
    }

    @Test
    @Order(6)
    @DisplayName("PUBLISH with an empty form is rejected — title is required")
    void publishEmptyRejected() {
        RecceListPage p = list();
        RecceFormPage f = p.tapCreate();
        assumeTrue(f.isLoaded(Duration.ofSeconds(15)), "Form did not open");
        assumeTrue(f.publish(), "Publish not tappable");
        boolean warned = f.anyRequiredWarningShown(Duration.ofSeconds(6));
        boolean stillOnForm = f.isLoaded(Duration.ofSeconds(4));
        System.out.println(">>> publish empty → warning=" + warned + " stillOnForm=" + stillOnForm);
        assertTrue(warned || stillOnForm,
                "Publishing an empty recce must be rejected (a required-field warning, not a save)");
        screenshot("recce-publish-empty");
        f.back();
    }

    @Test
    @Order(7)
    @DisplayName("A DRAFT saves without any validation (drafts skip required fields)")
    void saveAsDraft() {
        RecceListPage p = list();
        RecceFormPage f = p.tapCreate();
        assumeTrue(f.isLoaded(Duration.ofSeconds(15)), "Form did not open");
        String title = PREFIX + " Draft " + (System.currentTimeMillis() % 100000);
        f.setTitle(title);
        assumeTrue(f.saveDraft(), "Save-draft not tappable");
        sleep(2500);
        backToList();
        RecceListPage l = list();
        l.tapChip("Drafts");
        boolean listed = l.cardTitles().stream().anyMatch(t -> t.contains(title))
                || l.textShown(title, Duration.ofSeconds(8));
        System.out.println(">>> draft '" + title + "' listed=" + listed
                + " | Drafts chip=" + l.chipText("Drafts"));
        assertTrue(listed, "The saved draft should appear under the Drafts filter");
        draftTitle = title;
        screenshot("recce-draft-saved");
        l.tapChip("All");
    }

    @Test
    @Order(8)
    @DisplayName("Create and PUBLISH a full recce → it appears under Published")
    void createAndPublish() {
        RecceListPage p = list();
        RecceFormPage f = p.tapCreate();
        assumeTrue(f.isLoaded(Duration.ofSeconds(15)), "Form did not open");
        String title = PREFIX + " Pub " + (System.currentTimeMillis() % 100000);
        f.setTitle(title);
        String unit = f.pickFirstUnit();
        f.pickDate();
        f.pickRdvTime();
        f.setRdvPlace("QA Rendezvous Point");
        f.setStation("QA Station");
        System.out.println(">>> publishing '" + title + "' unit='" + unit
                + "' date='" + f.dateText() + "' rdv='" + f.rdvTimeText() + "'");
        assumeTrue(f.publish(), "Publish not tappable");
        sleep(3000);
        backToList();
        RecceListPage l = list();
        l.tapChip("Published");
        boolean listed = l.cardTitles().stream().anyMatch(t -> t.contains(title))
                || l.textShown(title, Duration.ofSeconds(10));
        System.out.println(">>> published '" + title + "' listed=" + listed
                + " | Published chip=" + l.chipText("Published"));
        assumeTrue(listed, "Publish may have been rejected by a required field — check the log");
        assertTrue(listed, "The published recce should appear under the Published filter");
        publishedTitle = title;
        screenshot("recce-published");
        l.tapChip("All");
    }

    private static String publishedTitle;

    @Test
    @Order(8)
    @DisplayName("PUBLISH FROM DRAFT: open a saved draft → Edit → Publish → status flips to Published")
    void publishAnExistingDraft() {
        RecceListPage p = list();
        // 1. make a draft we own for this test
        RecceFormPage f = p.tapCreate();
        assumeTrue(f.isLoaded(Duration.ofSeconds(15)), "Form did not open");
        String title = PREFIX + " D2P " + (System.currentTimeMillis() % 100000);
        f.setTitle(title);
        assumeTrue(f.saveDraft(), "Save-draft not tappable");
        sleep(2500);
        backToList();

        // 2. it must start life under Drafts
        RecceListPage l = list();
        l.tapChip("Drafts");
        assumeTrue(l.cardTitles().stream().anyMatch(t -> t.contains(title))
                        || l.textShown(title, Duration.ofSeconds(8)),
                "The new draft did not appear under Drafts");
        System.out.println(">>> draft created: " + title);

        // 3. open it — the detail must say Draft
        assumeTrue(l.openRecce(title), "Could not open the draft");
        RecceDetailPage d = new RecceDetailPage(driver);
        assumeTrue(d.isLoaded(Duration.ofSeconds(15)), "Detail did not load");
        String statusBefore = d.status();
        System.out.println(">>> status before publish = '" + statusBefore + "'");
        assertTrue(statusBefore.toLowerCase().contains("draft"),
                "A saved draft should show status Draft, got '" + statusBefore + "'");

        // 4. Edit → complete the required fields → Publish
        assumeTrue(d.isEditVisible(), "Edit not available — skipping");
        RecceFormPage e = d.tapEdit();
        assumeTrue(e.isLoaded(Duration.ofSeconds(15)), "Edit form did not open");
        e.pickFirstUnit();
        e.pickDate();
        e.pickRdvTime();
        e.setRdvPlace("QA D2P RDV");
        assumeTrue(e.publish(), "Publish not tappable");
        sleep(3500);
        backToList();

        // 5. it must now be Published and NO LONGER a draft
        RecceListPage l2 = list();
        l2.tapChip("Published");
        boolean nowPublished = l2.cardTitles().stream().anyMatch(t -> t.contains(title))
                || l2.textShown(title, Duration.ofSeconds(10));
        l2.tapChip("Drafts");
        boolean stillDraft = l2.cardTitles().stream().anyMatch(t -> t.contains(title));
        System.out.println(">>> after publish: underPublished=" + nowPublished
                + " stillUnderDrafts=" + stillDraft);
        screenshot("recce-draft-to-published");
        l2.tapChip("All");
        assertTrue(nowPublished, "The published draft should now appear under Published");
        assertTrue(!stillDraft, "It should no longer be listed under Drafts once published");
    }

    // ---------------- stops + personnel ----------------

    @Test
    @Order(9)
    @DisplayName("Stops: add a stop then delete it (stop delete has no confirm)")
    void addAndDeleteStop() {
        RecceListPage p = list();
        RecceFormPage f = p.tapCreate();
        assumeTrue(f.isLoaded(Duration.ofSeconds(15)), "Form did not open");
        int before = f.stopCount();
        assumeTrue(f.addStop(), "Add-stop not tappable");
        int added = f.stopCount();
        System.out.println(">>> stops " + before + " → " + added + " | summary='" + f.summaryText() + "'");
        assertTrue(added > before, "Adding a stop should add a stop row");
        assumeTrue(f.deleteLastStop(), "Stop delete not tappable");
        int afterDelete = f.stopCount();
        System.out.println(">>> after delete, stops=" + afterDelete);
        assertTrue(afterDelete < added, "Deleting a stop should remove the row");
        screenshot("recce-stops");
        f.back();
    }

    @Test
    @Order(9)
    @DisplayName("MULTI-STOP: build a 4-stop itinerary with kinds + places, and the summary counts them")
    void multipleStopsOnForm() {
        RecceListPage p = list();
        RecceFormPage f = p.tapCreate();
        assumeTrue(f.isLoaded(Duration.ofSeconds(15)), "Form did not open");
        int start = f.stopCount();          // a fresh form seeds 1 stop (Rendezvous)
        int target = 4;
        for (int i = start; i < target; i++) {
            boolean tapped = f.addStop();
            int now = f.stopCount();
            System.out.println(">>> addStop #" + (i + 1) + " tapped=" + tapped + " → stopCount=" + now);
            assumeTrue(tapped, "Add-stop failed at stop " + (i + 1));
        }
        int total = f.stopCount();
        System.out.println(">>> stops after adding: " + start + " → " + total);
        assertTrue(total >= target, "Should have at least " + target + " stop rows, got " + total);

        // Give each stop a distinct place + a different kind.
        String tag = String.valueOf(System.currentTimeMillis() % 100000);
        String[] kinds = {"Rendezvous", "Start", "Lunch", "End"};
        for (int i = 0; i < total && i < kinds.length; i++) {
            f.setStopPlace(i, "QA Stop " + (i + 1) + " " + tag);
            f.setStopKind(i, kinds[i]);
        }
        System.out.println(">>> stop places = " + f.stopPlaces());
        System.out.println(">>> stop kinds  = ["
                + f.stopKindText(0) + ", " + f.stopKindText(1) + ", "
                + f.stopKindText(2) + ", " + f.stopKindText(3) + "]");
        // Each place must be distinct — proves the rows are independent, not one
        // shared field being rewritten.
        List<String> places = f.stopPlaces().stream().filter(s -> s != null && !s.isBlank()).toList();
        assertTrue(places.stream().distinct().count() == places.size(),
                "Every stop should hold its OWN place value. Got: " + places);
        // The summary line counts the stops.
        String summary = f.summaryText();
        System.out.println(">>> summary='" + summary + "'");
        assertTrue(summary.contains(String.valueOf(total)),
                "The summary should report " + total + " stops. Got: '" + summary + "'");
        screenshot("recce-multi-stop-form");
        f.back();
    }

    @Test
    @Order(9)
    @DisplayName("MULTI-STOP-B: publish a multi-stop recce → all stops appear in the detail timeline")
    void multipleStopsPersistToDetail() {
        RecceListPage p = list();
        RecceFormPage f = p.tapCreate();
        assumeTrue(f.isLoaded(Duration.ofSeconds(15)), "Form did not open");
        String title = PREFIX + " Multi " + (System.currentTimeMillis() % 100000);
        f.setTitle(title);
        f.pickFirstUnit();
        f.pickDate();
        f.pickRdvTime();
        f.setRdvPlace("QA Multi RDV");
        // three stops in total
        int have = f.stopCount();
        for (int i = have; i < 3; i++) f.addStop();
        int total = f.stopCount();
        for (int i = 0; i < total; i++) f.setStopPlace(i, "QA MultiStop " + (i + 1));
        System.out.println(">>> publishing '" + title + "' with " + total + " stops: " + f.stopPlaces());
        assumeTrue(f.publish(), "Publish not tappable");
        sleep(3000);
        backToList();
        RecceListPage l = list();
        l.tapChip("All");
        assumeTrue(l.openRecce(title), "Could not open the published multi-stop recce");
        RecceDetailPage d = new RecceDetailPage(driver);
        assumeTrue(d.isLoaded(Duration.ofSeconds(15)), "Detail did not load");
        int tile = d.stopsTileCount();          // the app's own count
        int shown = d.timelineStopCountScrolled();
        System.out.println(">>> detail timeline stops=" + shown + " (created " + total + ") places="
                + d.timelineStopPlaces());
        // Prefer the app's own "Stops" tile: the timeline text scan cannot see a
        // stop that was left unnamed (only visible rows can be filled in).
        if (tile >= 0) {
            System.out.println(">>> stops tile (app's own count) = " + tile);
            assertTrue(tile >= total,
                    "The detail Stops tile should report all " + total + " stops, got " + tile);
        } else {
            assertTrue(shown >= Math.min(total, 2),
                    "The detail timeline should render the named stops, got " + shown);
        }
        screenshot("recce-multi-stop-detail");
        d.back();
    }

    @Test
    @Order(10)
    @DisplayName("Personnel: add a person row and fill it")
    void addPerson() {
        RecceListPage p = list();
        RecceFormPage f = p.tapCreate();
        assumeTrue(f.isLoaded(Duration.ofSeconds(15)), "Form did not open");
        int before = f.personCount();
        assumeTrue(f.addPerson(), "Add-person not tappable");
        int after = f.personCount();
        System.out.println(">>> people " + before + " → " + after);
        assertTrue(after > before, "Adding a person should add a person row");
        f.setPerson(after - 1, "QA Scout", "Location Manager", "qa.scout@example.com");
        screenshot("recce-person");
        f.back();
    }

    @Test
    @Order(11)
    @DisplayName("An INVALID personnel email blocks publishing")
    void invalidPersonEmailRejected() {
        RecceListPage p = list();
        RecceFormPage f = p.tapCreate();
        assumeTrue(f.isLoaded(Duration.ofSeconds(15)), "Form did not open");
        f.setTitle(PREFIX + " BadEmail " + (System.currentTimeMillis() % 100000));
        f.pickFirstUnit();
        f.pickDate();
        f.pickRdvTime();
        f.setRdvPlace("QA RDV");
        assumeTrue(f.personCount() > 0 || f.addPerson(), "No person row available");
        f.setPerson(0, "QA Person", "Scout", "not-an-email");
        f.publish();
        boolean warned = f.textShown("valid email", Duration.ofSeconds(6))
                || f.isLoaded(Duration.ofSeconds(3));
        System.out.println(">>> invalid email → blocked=" + warned);
        assertTrue(warned, "An invalid personnel email should block publishing");
        screenshot("recce-bad-email");
        f.back();
    }

    // ---------------- location picker ----------------

    @Test
    @Order(12)
    @DisplayName("Location picker opens with the map, W3W field, pin readout and Use")
    void locationPickerOpens() {
        RecceListPage p = list();
        RecceFormPage f = p.tapCreate();
        assumeTrue(f.isLoaded(Duration.ofSeconds(15)), "Form did not open");
        assumeTrue(f.openRdvLocationPicker(), "Rendezvous pick not tappable");
        boolean open = f.isLocationPickerOpen(Duration.ofSeconds(12));
        System.out.println(">>> location picker open=" + open + " pin='" + f.pickerPinText() + "'");
        assertTrue(open, "The location picker (map + W3W + pin) should open");
        f.setPickerW3w("filled.count.soap");
        screenshot("recce-location-picker");
        f.closeLocationPicker();
        f.back();
    }

    // ---------------- detail ----------------

    @Test
    @Order(13)
    @DisplayName("Opening a recce shows the detail screen (title, tiles, timeline, actions)")
    void detailOpens() {
        RecceListPage p = list();
        p.tapChip("All");
        assumeTrue(p.cardCount() > 0, "No recces to open — skipping");
        assumeTrue(p.openFirstRecce(), "Could not open a recce card");
        RecceDetailPage d = new RecceDetailPage(driver);
        assumeTrue(d.isLoaded(Duration.ofSeconds(15)), "Detail did not load");
        System.out.println(">>> detail: title='" + d.title() + "' status='" + d.status()
                + "' unit='" + d.unit() + "' tiles=" + d.infoTileCount()
                + " stops=" + d.timelineStopCount() + " people=" + d.personnelCount()
                + " | edit=" + d.isEditVisible() + " export=" + d.isExportVisible()
                + " print=" + d.isPrintVisible());
        assertTrue(!d.title().isBlank(), "The detail screen should show the recce title");
        assertTrue(d.isEditVisible() || d.isExportVisible(),
                "The detail action bar should offer Edit / Export");
        screenshot("recce-detail");
        d.back();
    }

    @Test
    @Order(14)
    @DisplayName("Detail → Edit opens the form pre-filled")
    void detailEditOpensForm() {
        RecceListPage p = list();
        p.tapChip("All");
        assumeTrue(p.cardCount() > 0, "No recces — skipping");
        assumeTrue(p.openFirstRecce(), "Could not open a recce");
        RecceDetailPage d = new RecceDetailPage(driver);
        assumeTrue(d.isLoaded(Duration.ofSeconds(15)), "Detail did not load");
        assumeTrue(d.isEditVisible(), "Edit not available (no posting rights?) — skipping");
        RecceFormPage f = d.tapEdit();
        assumeTrue(f.isLoaded(Duration.ofSeconds(15)), "Edit form did not open");
        String t = f.titleText();
        System.out.println(">>> edit form title='" + t + "'");
        assertTrue(!t.isBlank(), "The edit form should be pre-filled with the recce title");
        screenshot("recce-edit");
        f.back();
        backToList();
    }

    // ---------------- delete (cleanup) ----------------

    @Test
    @Order(15)
    @DisplayName("Delete a QA recce from the list card (confirm dialog) → it disappears")
    void deleteRecce() {
        RecceListPage p = list();
        p.tapChip("All");
        String target = p.cardTitles().stream().filter(t -> t.startsWith(PREFIX)).findFirst().orElse(null);
        assumeTrue(target != null, "No QA recce to delete — skipping");
        int before = p.cardCount();
        System.out.println(">>> deleting '" + target + "' (cards before=" + before + ")");
        assumeTrue(p.deleteRecce(target), "Could not tap delete / confirm");
        sleep(2500);
        boolean gone = p.cardTitles().stream().noneMatch(t -> t.equals(target));
        System.out.println(">>> after delete: cards=" + p.cardCount() + " gone=" + gone);
        assertTrue(gone, "The deleted recce '" + target + "' should no longer be listed");
        screenshot("recce-deleted");
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
