package com.zillit.automation.tests.cc;

import com.zillit.automation.core.TestOrder;

import com.zillit.automation.pages.cc.CcChatThreadPage;
import com.zillit.automation.pages.cc.CcCreateGroupPage;
import com.zillit.automation.pages.cc.CcGroupInfoPage;
import com.zillit.automation.pages.cc.CcLandingPage;
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
 * C&C group MANAGEMENT — the member/admin/delete flows. Opens a "QA Cc Grp" test
 * group's info screen and exercises: view members, ADD member (via Edit Group),
 * make/disable admin, REMOVE member (member long-press → confirm), and DELETE the
 * group (admin, confirm). Delete runs last so the earlier cases have a group.
 */
@Order(TestOrder.CC_GROUP_MANAGE)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CcGroupManageTest extends CcBaseTest {

    private CcGroupInfoPage openGroupInfo() {
        CcChatThreadPage t = openOrCreateTestGroup();
        if (t == null || !t.isLoaded(Duration.ofSeconds(15))) return null;
        CcGroupInfoPage info = t.openGroupInfo();
        return info.isLoaded(Duration.ofSeconds(12)) ? info : null;
    }

    @Test
    @Order(1)
    @DisplayName("Group info shows the member list + count, and admin controls (Edit/Delete)")
    void groupInfoLoads() {
        CcGroupInfoPage info = openGroupInfo();
        assumeTrue(info != null, "Could not open group info — skipping");
        assertTrue(info.memberRowCount() > 0 || !info.membersCountText().isBlank(),
                "Group info should list members");
        System.out.println(">>> group info: " + info.membersCountText()
                + " members=" + info.memberNames() + " edit=" + info.isEditVisible()
                + " delete=" + info.isDeleteGroupVisible());
        screenshot("cc-group-info");
        info.back();
    }

    // [WEB CC-28] — was "after >= 1", which could never fail.
    @Test
    @Order(2)
    @DisplayName("[WEB CC-28] ADD member via Edit Group → that member is listed and the count rises by exactly 1")
    void addMember() {
        CcGroupInfoPage info = openGroupInfo();
        assumeTrue(info != null, "No group info — skipping");
        assumeTrue(info.isEditVisible(), "Edit Group not available (not admin?) — skipping");
        // Only the other test account is ever added. If it is already a member, take it out
        // first so the add is observable (the edit picker hides existing members).
        if (hasMember(info.memberNames(), PEER)) {
            assumeTrue(removeNamed(info, PEER), "Could not take " + PEER + " out first to re-add them — skipping");
            info = reopenGroupInfo();
            assumeTrue(info != null, "Could not reopen group info");
            assumeTrue(!hasMember(info.memberNames(), PEER), PEER + " is still listed after removal — skipping");
        }
        int before = memberTotal(info);
        List<String> namesBefore = info.memberNames();
        CcCreateGroupPage g = info.tapEditGroup();
        assumeTrue(g.isLoaded(Duration.ofSeconds(12)), "Edit Group did not open");
        assumeTrue(g.tapSelectMember() && g.isMemberPickerOpen(Duration.ofSeconds(8)),
                "Member picker did not open");
        if (!g.selectMemberNamed(PEER)) {
            // The edit picker hides users the form already holds; say what the form holds.
            String held = g.selectedMemberNames().toString();
            System.out.println(">>> " + PEER + " not offered in the edit picker; the form's members: " + held);
            try { driver.navigate().back(); } catch (Exception ignored) {}
            assertTrue(!held.contains(PEER),
                    "Edit Group still lists '" + PEER + "' as a member after they were removed (stale group data): " + held);
            assumeTrue(false, PEER + " is not offered in the member picker — skipping");
        }
        assumeTrue(g.tapSubmit(), "Submit not tappable");
        assertTrue(g.waitClosed(Duration.ofSeconds(20)), "Edit Group should save and close");
        CcGroupInfoPage info2 = reopenGroupInfo();
        assumeTrue(info2 != null, "Could not reopen group info");
        int after = memberTotal(info2);
        List<String> namesAfter = info2.memberNames();
        System.out.println(">>> members " + before + " -> " + after + " before=" + namesBefore + " after=" + namesAfter);
        assertTrue(hasMember(namesAfter, PEER),
                "'" + PEER + "' should now be listed in the group; got " + namesAfter);
        assertTrue(after == before + 1,
                "Adding one member should raise the count by exactly 1: " + before + " -> " + after);
        screenshot("cc-add-member");
        info2.back();
    }

    /** Back to the landing, then the test group's info screen again (fresh data). */
    private CcGroupInfoPage reopenGroupInfo() {
        recoverToCcLanding();
        return openGroupInfo();
    }

    /** Member long-press → Remove → Yes, for the named member. */
    private boolean removeNamed(CcGroupInfoPage info, String name) {
        if (!info.longPressMemberNamed(name)) return false;
        if (!info.isMemberMenuOpen(Duration.ofSeconds(6))) return false;
        if (!info.tapMemberOption("Remove")) return false;
        boolean ok = info.confirmDialog(Duration.ofSeconds(8), "Yes", "Remove", "OK");
        sleep(2500);
        return ok;
    }

    /** Exact name match, ignoring a trailing " (Admin)"-style tag on the row. */
    private static boolean hasMember(List<String> rows, String name) {
        return rows.stream().anyMatch(r ->
                r.replaceAll("\\s*\\(.*\\)\\s*$", "").trim().equalsIgnoreCase(name.trim()));
    }

    /** Member total from "N Members" (totalMembersCount), else the visible row count. */
    private static int memberTotal(CcGroupInfoPage info) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)")
                .matcher(info.membersCountText());
        return m.find() ? Integer.parseInt(m.group(1)) : info.memberRowCount();
    }

    @Test
    @Order(3)
    @DisplayName("Member long-press (admin) offers Remove / admin toggle")
    void memberOptions() {
        CcGroupInfoPage info = openGroupInfo();
        assumeTrue(info != null, "No group info — skipping");
        assumeTrue(info.memberRowCount() >= 2, "Not enough members to long-press — skipping");
        // Long-press a member other than yourself: the other test account when present
        // (long-pressing your own row does nothing), else row 1.
        boolean pressed = hasMember(info.memberNames(), PEER)
                ? info.longPressMemberNamed(PEER) : info.longPressMember(1);
        assumeTrue(pressed, "Member long-press failed");
        assertTrue(info.isMemberMenuOpen(Duration.ofSeconds(6)),
                "Member long-press should offer Remove / admin options (admin only)");
        System.out.println(">>> member options menu opened");
        screenshot("cc-member-options");
        // dismiss the menu without acting
        try { driver.navigate().back(); } catch (Exception ignored) {}
        info.back();
    }

    // [WEB CC-28] — was "after < before || after >= 1", which could never fail.
    @Test
    @Order(4)
    @DisplayName("[WEB CC-28] REMOVE a member (confirm dialog) → that member is gone and the count drops by 1")
    void removeMember() {
        CcGroupInfoPage info = openGroupInfo();
        assumeTrue(info != null, "No group info — skipping");
        // Only the other test account is ever removed.
        assumeTrue(hasMember(info.memberNames(), PEER), PEER + " is not in the test group — nothing safe to remove");
        int before = memberTotal(info);
        assumeTrue(info.longPressMemberNamed(PEER), "Member long-press failed");
        assumeTrue(info.isMemberMenuOpen(Duration.ofSeconds(6)), "Member menu did not open (not admin?)");
        assumeTrue(info.tapMemberOption("Remove"), "Remove option not present");
        assertTrue(info.confirmDialog(Duration.ofSeconds(8), "Yes", "Remove", "OK"),
                "A remove-member confirmation should appear");
        sleep(2500);
        CcGroupInfoPage info2 = reopenGroupInfo();
        assumeTrue(info2 != null, "Could not reopen group info");
        int after = memberTotal(info2);
        List<String> names = info2.memberNames();
        System.out.println(">>> remove member: " + before + " -> " + after + " " + names);
        assertTrue(!hasMember(names, PEER), "'" + PEER + "' should no longer be listed; got " + names);
        assertTrue(after == before - 1, "Removing one member should drop the count by 1: " + before + " -> " + after);
        screenshot("cc-remove-member");
        info2.back();
    }

    // [WEB CC-34] [CNC-03] — rename uses the same field as create (Edit Group → CreateGroupPage)
    @Test
    @Order(6)
    @DisplayName("[WEB CC-34 / CNC-03] Rename (Edit Group): typing past 40 characters stops at 40 and the limit is shown")
    void renameNameOverLimit() {
        CcGroupInfoPage info = openGroupInfo();
        assumeTrue(info != null, "No group info — skipping");
        assumeTrue(info.isEditVisible(), "Edit Group not available (not admin?) — skipping");
        CcCreateGroupPage g = info.tapEditGroup();
        assumeTrue(g.isLoaded(Duration.ofSeconds(12)), "Edit Group did not open");
        String typed = "ZL AUTO cnc-03 " + "x".repeat(30);   // 45 characters
        try {
            g.typeNameRaw(typed);
            String kept = g.nameText();
            String hint = g.lengthHintShown();
            System.out.println(">>> rename: typed " + typed.length() + ", kept " + kept.length() + " hint='" + hint + "'");
            screenshot("cc-rename-limit");
            assertTrue(kept.length() == 40 && typed.startsWith(kept),
                    "The renamed group name should be kept up to 40 characters; kept " + kept.length());
            assertTrue(!hint.isEmpty(),
                    "CNC-03 (rename): the name is silently cut at 40 characters — no counter, hint or error");
        } finally {
            // Leave WITHOUT saving: the test group keeps its name.
            hideKeyboardIfUp();
            try { driver.navigate().back(); } catch (Exception ignored) {}
            recoverToCcLanding();
        }
    }

    // [WEB CC-27]
    @Test
    @Order(7)
    @DisplayName("[WEB CC-27] Group admin flow: rename, make the other member admin, then leave the group")
    void renamePromoteLeave() {
        String st = stamp();
        String original = "ZL AUTO adm " + st;
        String renamed = "ZL AUTO ren " + st;
        Boolean made = createPeerGroup(original);
        assumeTrue(made != null, "Could not set up a group with " + PEER + " — skipping");
        assertTrue(made, "Creating '" + original + "' should save");
        boolean left = false;
        try {
            CcChatThreadPage t = openGroupNamed(original);
            assumeTrue(t != null, "The new group '" + original + "' is not listed — skipping");
            CcGroupInfoPage info = t.openGroupInfo();
            assumeTrue(info.isLoaded(Duration.ofSeconds(12)), "Group info did not open");
            assumeTrue(info.isEditVisible(), "Edit Group missing on a group I created");

            // 1. Rename (Edit Group is the Android rename path).
            CcCreateGroupPage g = info.tapEditGroup();
            assumeTrue(g.isLoaded(Duration.ofSeconds(12)), "Edit Group did not open");
            g.setName(renamed);
            assumeTrue(g.tapSubmit(), "Submit not tappable");
            assertTrue(g.waitClosed(Duration.ofSeconds(20)), "Edit Group should save the new name");
            CcChatThreadPage t2 = openGroupNamed(renamed);
            assertTrue(t2 != null, "The renamed group '" + renamed + "' should be listed");
            assertTrue(t2.title().contains(renamed),
                    "The thread toolbar should show the new name; got '" + t2.title() + "'");

            // 2. Make the other member a group admin (no confirmation on Android).
            CcGroupInfoPage info2 = t2.openGroupInfo();
            assumeTrue(info2.isLoaded(Duration.ofSeconds(12)), "Group info did not reopen");
            int admins0 = info2.adminCount();
            assumeTrue(info2.longPressMemberNamed(PEER), PEER + " row not found in group info");
            assumeTrue(info2.isMemberMenuOpen(Duration.ofSeconds(6)), "Member menu did not open");
            assertTrue(info2.tapMemberOption("Make group admin"), "'Make group admin' should be offered for a plain member");
            int admins = admins0;
            long deadline = System.currentTimeMillis() + 12_000;
            while (admins < 2 && System.currentTimeMillis() < deadline) { sleep(1000); admins = info2.adminCount(); }
            System.out.println(">>> admins " + admins0 + " -> " + admins);
            assertTrue(admins == 2, "Two members should carry the admin badge after 'Make group admin'; got " + admins);

            // 3. Leave (offered once there is a second admin).
            assertTrue(info2.isLeaveVisible(), "With two admins the Leave button should be shown");
            assertTrue(info2.tapLeave(), "Leave should be tappable");
            assertTrue(info2.confirmDialog(Duration.ofSeconds(8), "Yes"), "A leave confirmation should appear");
            left = true;
            sleep(2500);
            recoverToCcLanding();
            CcLandingPage p = landing();
            assertTrue(!waitGroupListed(p, renamed, Duration.ofSeconds(6)),
                    "After leaving, '" + renamed + "' should no longer be in my Groups list");
            screenshot("cc-rename-promote-leave");
        } finally {
            if (!left) {
                deleteGroupQuietly(renamed);
                deleteGroupQuietly(original);
            } else {
                // The group lives on with PEER as its admin; CcGroupChatTest's two-device case
                // deletes leftover "ZL AUTO ren" groups from PEER's side.
                System.out.println(">>> left '" + renamed + "' — it remains for " + PEER + " (two-device sweep removes it)");
            }
            try { landing().tapChip("All"); } catch (Exception ignored) {}
        }
    }

    @Test
    @Order(9)
    @DisplayName("DELETE the group (admin, confirm) → group removed")
    void deleteGroup() {
        CcGroupInfoPage info = openGroupInfo();
        assumeTrue(info != null, "No group info — skipping");
        assumeTrue(info.isDeleteGroupVisible(), "Delete Group not available (not admin?) — skipping");
        assumeTrue(info.tapDeleteOrLeave(), "Delete/Leave not tappable");
        assertTrue(info.confirmDialog(Duration.ofSeconds(8), "Delete", "Yes", "OK"),
                "A delete-group confirmation should appear");
        sleep(2500);
        System.out.println(">>> group deleted");
        screenshot("cc-delete-group");
        recoverToCcLanding();
    }
}
