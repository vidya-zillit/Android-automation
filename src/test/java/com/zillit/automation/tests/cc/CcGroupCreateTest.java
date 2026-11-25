package com.zillit.automation.tests.cc;

import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.cc.CcCreateGroupPage;
import com.zillit.automation.pages.cc.CcLandingPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.WebDriverException;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * C&C — landing + CREATE GROUP with worst-to-worst validation, then a real
 * group creation. The created group name is shared (static) so the group-manage
 * suite can act on it.
 */
@Order(TestOrder.CC_GROUP_CREATE)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CcGroupCreateTest extends CcBaseTest {

    static String createdGroup;

    // ================================================================ landing

    @Test
    @Order(1)
    @DisplayName("C&C landing shows the chat/group list, filter chips, search and Create-Group FAB")
    void landingLoads() {
        CcLandingPage p = landing();
        assertTrue(p.isLoaded(Duration.ofSeconds(10)), "C&C landing (list) should load");
        assertTrue(p.isSearchVisible(), "Search should be present");
        assertTrue(p.isCreateGroupVisible(), "Create-Group FAB should be present");
        System.out.println(">>> C&C chips: " + p.chipLabels() + " | rows=" + p.rowCount());
        screenshot("cc-landing");
    }

    @Test
    @Order(2)
    @DisplayName("C&C filter chips (All/Members/Groups) switch the list")
    void filterChips() {
        CcLandingPage p = landing();
        List<String> chips = p.chipLabels();
        assumeTrue(!chips.isEmpty(), "No filter chips — skipping");
        assertTrue(chips.stream().anyMatch(c -> c.equalsIgnoreCase("Groups"))
                        || chips.stream().anyMatch(c -> c.equalsIgnoreCase("All")),
                "Chips should include Groups/All. Got: " + chips);
        p.tapChip("Groups");
        assertTrue(p.isLoaded(Duration.ofSeconds(6)), "List should stay usable after chip switch");
        p.tapChip("All");
    }

    // ============================================================ create group

    @Test
    @Order(3)
    @DisplayName("Create Group validation: empty name, <3 chars, and no members are rejected")
    void createGroupValidation() {
        CcLandingPage p = landing();
        assumeTrue(p.isCreateGroupVisible(), "Create-Group FAB missing — skipping");
        CcCreateGroupPage g = p.tapCreateGroup();
        assumeTrue(g.isLoaded(Duration.ofSeconds(12)), "Create Group did not open");
        // Empty name → blocked.
        g.tapSubmit();
        assertTrue(g.isStillOpen(), "Empty group name should be rejected (form stays open)");
        // Too-short name (<3) → blocked.
        g.setName("ab");
        g.tapSubmit();
        assertTrue(g.isStillOpen(), "A <3-char group name should be rejected");
        // Valid name but no members → blocked.
        g.setName("QA Cc Group " + (System.currentTimeMillis() % 100000));
        g.tapSubmit();
        assertTrue(g.isStillOpen(), "A group with no members should be rejected");
        screenshot("cc-create-validation");
        g.waitClosed(Duration.ofSeconds(1)); // no-op; leave form
        driver.navigate().back(); // exit create-group
        recoverToCcLanding();
    }

    @Test
    @Order(4)
    @DisplayName("Create a group with a name + members — it appears in the C&C list")
    void createGroup() {
        CcLandingPage p = landing();
        assumeTrue(p.isCreateGroupVisible(), "Create-Group FAB missing — skipping");
        CcCreateGroupPage g = p.tapCreateGroup();
        assumeTrue(g.isLoaded(Duration.ofSeconds(12)), "Create Group did not open");
        String name = "QA Cc Grp " + (System.currentTimeMillis() % 1000000);
        g.setName(name);
        assumeTrue(g.tapSelectMember(), "Select-member not present");
        assumeTrue(g.isMemberPickerOpen(Duration.ofSeconds(8)), "Member picker did not open");
        // The other test account; the historic "first two in the picker" only as a fallback.
        int picked = g.selectMemberNamed(PEER) ? 1 : 0;
        if (picked == 0 && g.tapSelectMember() && g.isMemberPickerOpen(Duration.ofSeconds(8))) {
            picked = g.selectMembers(2);
        }
        System.out.println(">>> members picked: " + picked);
        assumeTrue(g.selectedMemberCount() > 0 || picked > 0, "Could not select any member — skipping");
        assumeTrue(g.tapSubmit(), "Submit not tappable");
        // Success returns to the list (or opens the new group thread).
        boolean created = g.waitClosed(Duration.ofSeconds(20));
        System.out.println(">>> group create closed=" + created + " name=" + name);
        assertTrue(created || landing().isRowShown(name, Duration.ofSeconds(10)),
                "The created group should be saved (form closes / group appears)");
        createdGroup = name;
        screenshot("cc-group-created");
        recoverToCcLanding();
    }

    // ====================================================== web-gap cases (C&C)

    /** designationVw of a GROUP row: "N members in this group" (ChatAndGroupListingAdapter
     *  designationName), or the department-group line. */
    private static final java.util.regex.Pattern GROUP_DESIGNATION = java.util.regex.Pattern.compile(
            "(?s)^\\d+ members in this group$|^Default department group.*");

    // [WEB CC-2]
    @Test
    @Order(5)
    @DisplayName("[WEB CC-2] Members chip lists only people and Groups chip only groups (\"N members in this group\")")
    void chipsShowTheirCardTypes() {
        CcLandingPage p = landing();
        assumeTrue(p.tapChip("Members"), "No Members chip — skipping");
        sleep(1500);
        List<CcLandingPage.Row> members = p.rows();
        assumeTrue(p.tapChip("Groups"), "No Groups chip (pending project?) — skipping");
        sleep(2000);
        List<CcLandingPage.Row> groups = p.rows();
        p.tapChip("All");
        System.out.println(">>> Members rows=" + members);
        System.out.println(">>> Groups rows=" + groups);
        assumeTrue(!groups.isEmpty(), "This project has no C&C groups — the Groups half needs one");
        assertTrue(!members.isEmpty(), "The Members chip should list people");

        List<String> groupLike = members.stream()
                .filter(r -> GROUP_DESIGNATION.matcher(r.designation()).matches())
                .map(CcLandingPage.Row::name).toList();
        assertTrue(groupLike.isEmpty(), "Members chip should list no group cards; got " + groupLike);
        List<String> notGroups = groups.stream()
                .filter(r -> !GROUP_DESIGNATION.matcher(r.designation()).matches())
                .map(r -> r.name() + " [" + r.designation() + "]").toList();
        assertTrue(notGroups.isEmpty(),
                "Every Groups-chip card should read 'N members in this group'; these don't: " + notGroups);
        assertTrue(groups.stream().noneMatch(CcLandingPage.Row::star),
                "Group cards should not carry the favourite star (user cards only)");
        List<String> mNames = members.stream().map(CcLandingPage.Row::name).toList();
        List<String> gNames = groups.stream().map(CcLandingPage.Row::name).toList();
        assertTrue(!mNames.equals(gNames), "Members and Groups should list different cards: " + mNames);
        screenshot("cc-chips-card-types");
    }

    // [WEB CC-3]
    @Test
    @Order(6)
    @DisplayName("[WEB CC-3] Landing search narrows the list to matching names (Groups chip), and on All opens member search")
    void landingSearchFilters() {
        CcLandingPage p = landing();
        assumeTrue(p.tapChip("Groups"), "No Groups chip — skipping");
        sleep(1500);
        List<String> all = p.rowNames();
        assumeTrue(!all.isEmpty(), "No groups to search — skipping");
        String base = all.stream().filter(n -> n.replaceAll("[^A-Za-z]", "").length() >= 3)
                .findFirst().orElse(null);
        assumeTrue(base != null, "No group with a 3+ letter name — skipping");
        String q = base.substring(0, Math.min(4, base.length()));
        // On Groups the search box is a real filter: the "new chat" overlay (layHideSearch)
        // is only laid over it on All / Members (ChatFragment.kt:120).
        assertTrue(p.isSearchCardShown(), "The Groups chip should show the search box");
        try {
            p.typeInlineSearch(q);
            List<String> hits = p.rowNames();
            System.out.println(">>> search '" + q + "' -> " + hits);
            assertTrue(!hits.isEmpty(), "Searching '" + q + "' (from '" + base + "') should keep that group listed");
            List<String> bad = hits.stream().filter(n -> !n.toLowerCase().contains(q.toLowerCase())).toList();
            assertTrue(bad.isEmpty(), "Every listed name should contain '" + q + "'; these don't: " + bad);

            p.typeInlineSearch("zzz-nomatch-" + stamp());
            int none = p.rowCount();
            assertTrue(none == 0, "A no-match search should empty the list; " + none + " rows left");

            p.clearInlineSearch();
            List<String> back = p.rowNames();
            System.out.println(">>> cleared search -> " + back.size() + " rows");
            assertTrue(back.size() >= hits.size() && back.contains(base),
                    "Clearing the search should bring the rows back (incl. '" + base + "'); got " + back);
        } finally {
            p.clearInlineSearch();
            hideKeyboardIfUp();
        }

        // All chip: Android lays the new-chat overlay over the box — tapping search opens
        // SearchUsersPage (a member picker) instead of filtering the list inline as the web does.
        p.tapChip("All");
        sleep(1000);
        assertTrue(p.openSearchUsers(), "On All, tapping search should open the member search page (new chat)");
        screenshot("cc-landing-search");
        hideKeyboardIfUp();
        try { driver.navigate().back(); } catch (WebDriverException ignored) {}
        recoverToCcLanding();
    }

    // [WEB CC-31] [CNC-02]
    @Test
    @Order(7)
    @DisplayName("[WEB CC-31 / CNC-02] Chat list keeps its full height after opening a chat from search with the keyboard up")
    void listHeightAfterSearchOpen() {
        CcLandingPage p = landing();
        assumeTrue(p.tapChip("Groups"), "No Groups chip — skipping");
        sleep(1500);
        hideKeyboardIfUp();
        org.openqa.selenium.Rectangle r0 = p.listRect();
        int last0 = p.lastRowBottom();
        int rowH = p.rowHeight();
        List<String> names = p.rowNames();
        assumeTrue(r0 != null && rowH > 0 && !names.isEmpty(), "No group rows to measure — skipping");
        String target = names.get(0);
        String q = target.substring(0, Math.min(4, target.length()));
        int bottom0 = r0.getY() + r0.getHeight();
        System.out.println(">>> before: list " + r0.getY() + ".." + bottom0 + " lastRow=" + last0 + " rowH=" + rowH);

        p.typeInlineSearch(q);   // leaves the soft keyboard up
        boolean kbUp;
        try { kbUp = driver.isKeyboardShown(); } catch (WebDriverException e) { kbUp = false; }
        assumeTrue(p.openRow(target, Duration.ofSeconds(6)), "Could not open '" + target + "' from the search");
        com.zillit.automation.pages.cc.CcChatThreadPage t = new com.zillit.automation.pages.cc.CcChatThreadPage(driver);
        assumeTrue(t.isLoaded(Duration.ofSeconds(15)), "The group thread did not open");
        // Back while the keyboard state from the search is still pending (no hideKeyboard first).
        for (int i = 0; i < 3 && !p.isLoaded(Duration.ofSeconds(2)); i++) {
            try { driver.navigate().back(); } catch (WebDriverException ignored) {}
            sleep(900);
        }
        assumeTrue(p.isLoaded(Duration.ofSeconds(5)), "Did not get back to the C&C list");
        p.clearInlineSearch();
        hideKeyboardIfUp();
        sleep(1200);
        org.openqa.selenium.Rectangle r1 = p.listRect();
        int last1 = p.lastRowBottom();
        assertTrue(r1 != null, "The chat list should be shown after coming back");
        int bottom1 = r1.getY() + r1.getHeight();
        System.out.println(">>> after (kbUpAtOpen=" + kbUp + "): list " + r1.getY() + ".." + bottom1 + " lastRow=" + last1);
        screenshot("cc-list-height-after-search");
        assertTrue(Math.abs(bottom1 - bottom0) <= 8,
                "CNC-02: the chat list should keep its full height after opening a chat from search; bottom "
                        + bottom0 + " -> " + bottom1);
        if (last0 >= bottom0 - rowH) {   // the list filled the screen before
            assertTrue(last1 >= bottom0 - rowH,
                    "CNC-02: a blank, keyboard-sized area is left under the rows (last row ends at " + last1
                            + ", list bottom " + bottom0 + ")");
        }
    }

    // [WEB CC-33] [CNC-03]
    @Test
    @Order(8)
    @DisplayName("[WEB CC-33 / CNC-03] Group name: typing past 40 characters stops at 40 and the limit is shown")
    void groupNameOverLimit() {
        CcLandingPage p = landing();
        p.tapChip("All");
        assumeTrue(p.isCreateGroupVisible(), "Create-Group FAB missing — skipping");
        CcCreateGroupPage g = p.tapCreateGroup();
        assumeTrue(g.isLoaded(Duration.ofSeconds(12)), "Create Group did not open");
        String typed = "ZL AUTO cnc-03 " + "x".repeat(30);   // 45 characters
        try {
            g.typeNameRaw(typed);
            String kept = g.nameText();
            String hint = g.lengthHintShown();
            System.out.println(">>> typed " + typed.length() + " chars, field kept " + kept.length()
                    + " ('" + kept + "'), hint='" + hint + "'");
            screenshot("cc-group-name-limit");
            assertTrue(kept.length() == 40 && typed.startsWith(kept),
                    "The group name should be kept up to the 40-character limit; kept " + kept.length() + ": '" + kept + "'");
            assertTrue(!hint.isEmpty(),
                    "CNC-03: the name is silently cut at 40 characters — no counter, hint or error tells the user "
                            + "about the limit (edit_text_widget_no_margins maxLength=40, no counter)");
        } finally {
            hideKeyboardIfUp();
            try { driver.navigate().back(); } catch (WebDriverException ignored) {}
            recoverToCcLanding();
        }
    }

    // [WEB CC-33]
    @Test
    @Order(9)
    @DisplayName("[WEB CC-33] Group names of exactly 3 and exactly 40 characters both save")
    void groupNameBoundaries() {
        String st = stamp();
        String n40 = ("ZL AUTO len40 " + st + " " + "y".repeat(40)).substring(0, 40);
        String n3 = "ZL" + (char) ('a' + (int) (System.currentTimeMillis() % 26));
        try {
            Boolean saved40 = createPeerGroup(n40);
            assumeTrue(saved40 != null, "Could not set up a group with " + PEER + " — skipping");
            assertTrue(saved40, "A 40-character group name should save");
            assertTrue(waitGroupListed(landing(), n40, Duration.ofSeconds(20)),
                    "The 40-character group '" + n40 + "' should be listed");

            Boolean saved3 = createPeerGroup(n3);
            assumeTrue(saved3 != null, "Could not set up the 3-character group — skipping");
            assertTrue(saved3, "A 3-character group name ('" + n3 + "') should save");
            assertTrue(waitGroupListed(landing(), n3, Duration.ofSeconds(20)),
                    "The 3-character group '" + n3 + "' should be listed");
            screenshot("cc-group-name-boundaries");
        } finally {
            deleteGroupQuietly(n40);
            deleteGroupQuietly(n3);
            sweepBoundaryGroups();
        }
    }

    /** Removes "ZLx" / "ZL AUTO len40 …" groups an earlier interrupted run left behind. */
    private void sweepBoundaryGroups() {
        try {
            recoverToCcLanding();
            CcLandingPage p = landing();
            for (String q : new String[]{"ZL AUTO len40", "ZL"}) {
                for (int i = 0; i < 6; i++) {
                    p.tapChip("Groups");
                    sleep(1000);
                    p.typeInlineSearch(q);
                    String left = p.rowNames().stream()
                            .filter(n -> n.startsWith("ZL AUTO len40 ") || n.matches("ZL[a-z]"))
                            .findFirst().orElse(null);
                    if (left == null) break;
                    System.out.println(">>> sweep: deleting leftover '" + left + "' = " + p.deleteGroupViaLongPress(left));
                }
            }
            p.tapChip("All");
        } catch (Exception e) {
            System.out.println(">>> boundary sweep failed: " + e.getMessage());
        }
    }

    // [WEB CC-29] [WEB CC-30] [CNC-01]
    @Test
    @Order(10)
    @DisplayName("[WEB CC-29/30 / CNC-01] Groups chip with no groups: no stuck loader, FAB stays, empty state shown")
    void groupsChipWithNoGroups() {
        CcLandingPage p = landing();
        assumeTrue(p.tapChip("Groups"), "No Groups chip — skipping");
        sleep(2000);
        int n = p.rowCount();
        assumeTrue(n == 0, "This project has " + n + " C&C group(s); the empty-Groups case needs a project "
                + "with none, and the suite always enters the first project — skipping");
        long until = System.currentTimeMillis() + 10_000;
        while (p.isLoaderShown() && System.currentTimeMillis() < until) sleep(1000);
        assertTrue(!p.isLoaderShown(), "CNC-01: a loader is still spinning on the empty Groups chip after 10 s");
        assertTrue(p.tapChip("All") && p.tapChip("Groups"), "The chips should stay tappable");
        sleep(1500);
        assertTrue(p.isCreateGroupVisible(), "Create Group should stay available on the empty Groups chip");
        String empty = p.emptyViewText();
        System.out.println(">>> empty Groups chip: emptyView=" + p.isEmptyViewShown() + " text='" + empty + "'");
        screenshot("cc-groups-empty");
        assertTrue(p.isEmptyViewShown() && !empty.isBlank(),
                "CNC-01 / ZL-17468: with no groups the Groups chip is a blank list — no empty-state message "
                        + "(ChatFragment shows emptyView only on Favourites)");
    }
}
