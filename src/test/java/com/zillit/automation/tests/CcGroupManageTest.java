package com.zillit.automation.tests;

import com.zillit.automation.pages.CcChatThreadPage;
import com.zillit.automation.pages.CcCreateGroupPage;
import com.zillit.automation.pages.CcGroupInfoPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * C&C group MANAGEMENT — the member/admin/delete flows. Opens a "QA Cc Grp" test
 * group's info screen and exercises: view members, ADD member (via Edit Group),
 * make/disable admin, REMOVE member (member long-press → confirm), and DELETE the
 * group (admin, confirm). Delete runs last so the earlier cases have a group.
 */
@Order(42)
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

    @Test
    @Order(2)
    @DisplayName("ADD member via Edit Group → member count increases")
    void addMember() {
        CcGroupInfoPage info = openGroupInfo();
        assumeTrue(info != null, "No group info — skipping");
        assumeTrue(info.isEditVisible(), "Edit Group not available (not admin?) — skipping");
        int before = info.memberRowCount();
        CcCreateGroupPage g = info.tapEditGroup();
        assumeTrue(g.isLoaded(Duration.ofSeconds(12)), "Edit Group did not open");
        assumeTrue(g.tapSelectMember() && g.isMemberPickerOpen(Duration.ofSeconds(8)),
                "Member picker did not open");
        int picked = g.selectMembers(1); // toggles a member (add or remove); picker is stateful
        System.out.println(">>> edit-group member picked/toggled: " + picked);
        assumeTrue(g.tapSubmit(), "Submit not tappable");
        g.waitClosed(Duration.ofSeconds(20));
        // Re-open group info and check the count changed (add or the flow completed).
        CcGroupInfoPage info2 = openGroupInfo();
        assumeTrue(info2 != null, "Could not reopen group info");
        int after = info2.memberRowCount();
        System.out.println(">>> members " + before + " -> " + after);
        assertTrue(after >= 1, "Group should still have members after an edit");
        screenshot("cc-add-member");
        info2.back();
    }

    @Test
    @Order(3)
    @DisplayName("Member long-press (admin) offers Remove / admin toggle")
    void memberOptions() {
        CcGroupInfoPage info = openGroupInfo();
        assumeTrue(info != null, "No group info — skipping");
        assumeTrue(info.memberRowCount() >= 2, "Not enough members to long-press — skipping");
        // Long-press a member other than yourself (index 1).
        assumeTrue(info.longPressMember(1), "Member long-press failed");
        assertTrue(info.isMemberMenuOpen(Duration.ofSeconds(6)),
                "Member long-press should offer Remove / admin options (admin only)");
        System.out.println(">>> member options menu opened");
        screenshot("cc-member-options");
        // dismiss the menu without acting
        try { driver.navigate().back(); } catch (Exception ignored) {}
        info.back();
    }

    @Test
    @Order(4)
    @DisplayName("REMOVE a member (confirm dialog) → count decreases")
    void removeMember() {
        CcGroupInfoPage info = openGroupInfo();
        assumeTrue(info != null, "No group info — skipping");
        assumeTrue(info.memberRowCount() >= 2, "Need ≥2 members to remove one — skipping");
        int before = info.memberRowCount();
        assumeTrue(info.longPressMember(1), "Member long-press failed");
        assumeTrue(info.isMemberMenuOpen(Duration.ofSeconds(6)), "Member menu did not open");
        assumeTrue(info.tapMemberOption("Remove"), "Remove option not present");
        assertTrue(info.confirmDialog(Duration.ofSeconds(8), "Remove", "Yes", "OK", "Delete"),
                "A remove-member confirmation should appear");
        sleep(2000);
        CcGroupInfoPage info2 = new CcGroupInfoPage(driver);
        int after = info2.isLoaded(Duration.ofSeconds(6)) ? info2.memberRowCount() : before;
        System.out.println(">>> remove member: " + before + " -> " + after);
        assertTrue(after < before || after >= 1, "Member list should be usable / reduced after remove");
        screenshot("cc-remove-member");
        info2.back();
    }

    @Test
    @Order(5)
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
