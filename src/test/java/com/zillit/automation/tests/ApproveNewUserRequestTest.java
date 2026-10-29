package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.ApproveNewUserRequestPage;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.ProjectListPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Settings → Admin Settings → "Approve New User Requests".
 *
 * <p>One continuous flow across the pending queue, the request detail, the photo
 * viewer and finally C&amp;C → Contacts, run on a single screen instance
 * ({@code relaunchBetweenTests()=false}) because each case carries on where the
 * last one stopped.
 *
 * <p>The queue is verified in full FIRST — every pending request must carry both
 * a name and a designation — and only then is one of those users used as the
 * fixture for the search, accept and decline cases.
 *
 * <p><b>The suite consumes its own fixture.</b> Accepting or declining resolves a
 * join request for good, so the users named in the written steps ("Bhavik Mehra",
 * "Pramod") exist only until the first run. Hard-coding them would make the suite
 * pass once and skip forever after, so the users, the name and designation
 * searched for, and the department picked are all derived from whatever is
 * actually pending. When the queue is empty the cases skip rather than fail —
 * there is nothing to approve, which is not a defect.
 *
 * <p>Behaviour confirmed on the device while building this:
 * <ul>
 *   <li>search filters on NAME <em>and</em> DESIGNATION — "Director" narrows to
 *       the user whose designation is Director;</li>
 *   <li>the search box APPENDS, so it must be cleared between searches;</li>
 *   <li>changing department RESETS designation to "Select Designation";</li>
 *   <li>Accept returns to the queue in about two seconds and drops the user;</li>
 *   <li>Decline reports "User request to join the project has been rejected".</li>
 * </ul>
 */
@Order(TestOrder.APPROVE_NEW_USER_REQUEST)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApproveNewUserRequestTest extends BaseTest {

    /** Confirmed against the live app. */
    private static final String REJECTED = "User request to join the project has been rejected";

    private static final Duration MESSAGE_WAIT = Duration.ofSeconds(10);
    private static final Duration RETURN_WAIT = Duration.ofSeconds(45);

    private boolean insideProject;

    // ---- read from the live queue in Test Case 2 ----
    private static Map<String, String> pendingQueue;
    private static String approveUser;
    private static String approveDesignation;
    private static String declineUser;
    private static String newDepartment;
    private static String newDesignation;

    @Override
    protected boolean relaunchBetweenTests() {
        return false;
    }

    @Override
    protected boolean requiresLoggedOutStart() {
        return false;
    }

    /** Returns the pending queue, navigating (or recovering) to it as needed. */
    private ApproveNewUserRequestPage queue() {
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
        ApproveNewUserRequestPage page = new ApproveNewUserRequestPage(driver);
        assumeTrue(page.ensureOnSelectUserList(Duration.ofSeconds(60)),
                "Could not reach Settings -> Admin Settings -> Approve New User Requests");
        return page;
    }

    @Test
    @Order(1)
    @DisplayName("TC1: Admin Settings -> Approve New User Requests opens the pending queue")
    void opensPendingQueue() {
        ApproveNewUserRequestPage page = queue();
        assertTrue(page.isOnSelectUserList(Duration.ofSeconds(20)),
                "SELECT USER queue should be open");
        screenshot("approve-new-user-queue");
    }

    @Test
    @Order(2)
    @DisplayName("TC2: Every pending request lists a name and a designation")
    void everyRequestHasNameAndDesignation() {
        ApproveNewUserRequestPage page = queue();

        // Scrolled, not viewport-only: the list recycles rows, so a short read
        // would verify the first screenful and call the whole queue clean.
        pendingQueue = page.allListedUsersScrolled();
        System.out.println(">>> pending requests (" + pendingQueue.size() + "):");
        pendingQueue.forEach((n, d) -> System.out.println("      " + n + "  ->  " + d));
        screenshot("approve-new-user-queue-verified");

        assumeTrue(!pendingQueue.isEmpty(),
                "No pending join requests on this project — nothing to verify, approve or decline");

        List<String> missing = new ArrayList<>();
        pendingQueue.forEach((name, desig) -> {
            if (name == null || name.isBlank()) {
                missing.add("(blank name) -> " + desig);
            } else if (desig == null || desig.isBlank()) {
                missing.add(name + " -> (no designation)");
            }
        });
        assertTrue(missing.isEmpty(),
                "Every pending request should show a name and a designation; missing: " + missing);

        // Fixture comes from the verified queue.
        approveUser = pendingQueue.keySet().iterator().next();
        approveDesignation = pendingQueue.get(approveUser);
        declineUser = pendingQueue.keySet().stream()
                .filter(n -> !n.equals(approveUser))
                .findFirst().orElse(null);
        System.out.println(">>> will approve: " + approveUser + " (" + approveDesignation + ")");
        System.out.println(">>> will decline: " + declineUser);
    }

    @Test
    @Order(3)
    @DisplayName("TC3: Searching a user's NAME narrows the list to that user")
    void searchByName() {
        assumeTrue(approveUser != null, "No pending user captured in TC2");
        ApproveNewUserRequestPage page = queue();

        assertTrue(page.search(approveUser), "Search box should accept input");
        Map<String, String> filtered = page.listedUsers();
        screenshot("approve-new-user-search-name");

        assertTrue(filtered.containsKey(approveUser),
                "Searching '" + approveUser + "' should show that user; got " + filtered.keySet());
        assertEquals(1, filtered.size(),
                "A full-name search should leave a single row; got " + filtered.keySet());
    }

    @Test
    @Order(4)
    @DisplayName("TC4: Clearing the search restores the full queue")
    void clearSearchRestoresList() {
        assumeTrue(approveUser != null, "No pending user captured in TC2");
        ApproveNewUserRequestPage page = queue();

        assertTrue(page.clearSearch(), "Search box should clear");
        Map<String, String> restored = page.allListedUsersScrolled();
        screenshot("approve-new-user-search-cleared");

        assertEquals(pendingQueue.keySet(), restored.keySet(),
                "Clearing the search should restore the whole queue");
    }

    @Test
    @Order(5)
    @DisplayName("TC5: Searching that user's DESIGNATION also finds them")
    void searchByDesignation() {
        assumeTrue(approveUser != null, "No pending user captured in TC2");
        ApproveNewUserRequestPage page = queue();

        // The box appends rather than replaces, so TC4's clear is load-bearing.
        assertTrue(page.search(approveDesignation), "Search box should accept input");
        Map<String, String> filtered = page.listedUsers();
        screenshot("approve-new-user-search-designation");

        assertTrue(filtered.containsKey(approveUser),
                "Searching '" + approveDesignation + "' should still show " + approveUser
                        + "; got " + filtered.keySet());
        assertTrue(filtered.values().stream()
                        .allMatch(d -> d != null && d.toLowerCase().contains(approveDesignation.toLowerCase())),
                "Every remaining row should match the designation searched; got " + filtered);
    }

    @Test
    @Order(6)
    @DisplayName("TC6: Opening the request shows its prefilled department and designation")
    void opensRequestDetail() {
        assumeTrue(approveUser != null, "No pending user captured in TC2");
        ApproveNewUserRequestPage page = queue();

        page.clearSearch();
        assertTrue(page.openUser(approveUser), approveUser + " row should be tappable");
        assertTrue(page.isOnUserDetail(Duration.ofSeconds(20)),
                "APPROVE NEW USER REQUESTS detail should open");

        String dept = page.department();
        String desig = page.designation();
        screenshot("approve-new-user-detail");
        System.out.println(">>> prefilled department=" + dept + " designation=" + desig
                + " unit=" + page.unit());

        assertTrue(dept != null && !dept.isBlank(), "Department should be prefilled");
        assertTrue(desig != null && !desig.isBlank(), "Designation should be prefilled");
        assertNotEquals(ApproveNewUserRequestPage.NO_DESIGNATION, desig,
                "Designation should be prefilled from the request, not empty");
    }

    @Test
    @Order(7)
    @DisplayName("TC7: The user photo opens full screen, loads, and closes")
    void userPhotoLoads() {
        assumeTrue(approveUser != null, "No pending user captured in TC2");
        ApproveNewUserRequestPage page = new ApproveNewUserRequestPage(driver);
        assumeTrue(page.isOnUserDetail(Duration.ofSeconds(10)),
                "Not on the request detail — TC6 did not leave it open");

        assertTrue(page.openUserImage(), "User photo should be tappable");
        assertTrue(page.isImageViewerOpen(Duration.ofSeconds(15)), "Photo viewer should open");
        screenshot("approve-new-user-photo");

        assertTrue(page.isImageLoaded(),
                "Photo should actually render, not just exist as an empty ImageView");

        assertTrue(page.closeImageViewer(), "Viewer back arrow should close the overlay");
        assertTrue(page.isOnUserDetail(Duration.ofSeconds(15)),
                "Closing the photo should return to the request detail");
    }

    @Test
    @Order(8)
    @DisplayName("TC8: Changing department clears the designation")
    void changeDepartmentResetsDesignation() {
        assumeTrue(approveUser != null, "No pending user captured in TC2");
        ApproveNewUserRequestPage page = new ApproveNewUserRequestPage(driver);
        assumeTrue(page.isOnUserDetail(Duration.ofSeconds(10)), "Not on the request detail");

        String current = page.department();
        List<String> options = page.openDepartmentPicker();
        assertTrue(options.size() > 1,
                "Department picker should offer alternatives; got " + options);

        newDepartment = options.stream()
                .filter(o -> !o.equalsIgnoreCase(current))
                .findFirst().orElseThrow();
        assertTrue(page.choosePickerValue(newDepartment),
                "Should be able to pick " + newDepartment);

        screenshot("approve-new-user-department-changed");
        assertEquals(newDepartment, page.department(),
                "Department should now read " + newDepartment);
        assertNotEquals(current, page.department(), "Department should have changed from " + current);
        assertEquals(ApproveNewUserRequestPage.NO_DESIGNATION, page.designation(),
                "Changing department should clear the designation");
    }

    @Test
    @Order(9)
    @DisplayName("TC9: A designation from the new department can be selected")
    void changeDesignation() {
        assumeTrue(newDepartment != null, "TC8 did not change the department");
        ApproveNewUserRequestPage page = new ApproveNewUserRequestPage(driver);
        assumeTrue(page.isOnUserDetail(Duration.ofSeconds(10)), "Not on the request detail");

        List<String> options = page.openDesignationPicker();
        assertTrue(!options.isEmpty(),
                "Designation picker should offer values for " + newDepartment);

        newDesignation = options.stream()
                .filter(o -> !o.equalsIgnoreCase(approveDesignation))
                .findFirst().orElse(options.get(0));
        assertTrue(page.choosePickerValue(newDesignation),
                "Should be able to pick " + newDesignation);

        screenshot("approve-new-user-designation-changed");
        assertEquals(newDesignation, page.designation(),
                "Designation should now read " + newDesignation);
        assertNotEquals(ApproveNewUserRequestPage.NO_DESIGNATION, page.designation(),
                "Designation should no longer be the placeholder");
    }

    @Test
    @Order(10)
    @DisplayName("TC10: Accept approves the request and returns to the queue without it")
    void acceptRequest() {
        assumeTrue(newDesignation != null, "TC9 did not set a designation");
        ApproveNewUserRequestPage page = new ApproveNewUserRequestPage(driver);
        assumeTrue(page.isOnUserDetail(Duration.ofSeconds(10)), "Not on the request detail");

        assertTrue(page.tapAccept(), "Accept button should be tappable");
        assertTrue(page.isOnSelectUserList(RETURN_WAIT),
                "Accepting should return to the SELECT USER queue");

        Map<String, String> remaining = page.allListedUsersScrolled();
        screenshot("approve-new-user-accepted");
        System.out.println(">>> queue after accept: " + remaining.keySet());

        assertTrue(!remaining.containsKey(approveUser),
                approveUser + " should no longer be pending; queue = " + remaining.keySet());
    }

    @Test
    @Order(11)
    @DisplayName("TC11: Decline rejects a request and reports it")
    void declineRequest() {
        assumeTrue(declineUser != null, "Only one pending request — nothing left to decline");
        ApproveNewUserRequestPage page = queue();

        assumeTrue(page.isUserListed(declineUser), declineUser + " is no longer pending");
        assertTrue(page.openUser(declineUser), declineUser + " row should be tappable");
        assertTrue(page.isOnUserDetail(Duration.ofSeconds(20)), "Request detail should open");

        assertTrue(page.tapDecline(), "Decline button should be tappable");
        assertTrue(page.messageShown(REJECTED, MESSAGE_WAIT),
                "Expected the rejection message \"" + REJECTED + "\"");

        // Declining the LAST request empties the queue, and the app then closes
        // it and drops back to Admin Settings by itself. Both landings are
        // correct; insisting on the queue failed on a decline that had worked.
        boolean onQueue = page.isOnSelectUserList(RETURN_WAIT);
        boolean onAdmin = !onQueue && page.isOnAdminSettings(Duration.ofSeconds(10));
        screenshot("approve-new-user-declined");
        assertTrue(onQueue || onAdmin,
                "After declining, the app should return to the queue or to Admin Settings");

        if (onQueue) {
            assertTrue(!page.allListedUsersScrolled().containsKey(declineUser),
                    declineUser + " should no longer be pending");
        } else {
            System.out.println(">>> queue emptied by the decline — app returned to Admin Settings");
        }
    }

    @Test
    @Order(12)
    @DisplayName("TC12: Back leaves the queue to Admin Settings, then to the Settings tab")
    void backOutToSettingsTab() {
        ApproveNewUserRequestPage page = new ApproveNewUserRequestPage(driver);

        // Only back out of the queue if it is still open — once the last request
        // is resolved the app closes it itself, and demanding it be there turned
        // correct behaviour into a skipped test.
        if (page.isOnSelectUserList(Duration.ofSeconds(3))) {
            assertTrue(page.tapBack(), "Back should be tappable on the queue");
        }
        assertTrue(page.isOnAdminSettings(Duration.ofSeconds(20)),
                "Leaving the queue should land on ADMIN SETTINGS");

        assertTrue(page.tapBack(), "Back should be tappable on Admin Settings");
        assertTrue(page.isOnSettingsTab(Duration.ofSeconds(20)),
                "Leaving Admin Settings should land on the Settings tab");
        screenshot("approve-new-user-back-on-settings-tab");
    }

    @Test
    @Order(13)
    @DisplayName("TC13: The approved user appears in C&C -> Contacts")
    void approvedUserReachesContacts() {
        assumeTrue(approveUser != null, "No user was approved");
        ApproveNewUserRequestPage page = new ApproveNewUserRequestPage(driver);
        assumeTrue(page.ensureOnSettingsTab(Duration.ofSeconds(45)),
                "Could not get back to the Settings tab");

        assertTrue(page.openCncTab(), "C&C tab should be reachable from the bottom nav");
        assertTrue(page.openContactsTab(), "Contacts tab should open inside C&C");

        Map<String, String> contacts = page.contacts();
        screenshot("approve-new-user-contacts");
        System.out.println(">>> contacts: " + contacts);

        assertTrue(page.isContactListed(approveUser),
                approveUser + " should appear in C&C -> Contacts after approval; got "
                        + contacts.keySet());
    }

    @Test
    @Order(14)
    @DisplayName("TC14: The run finishes back on the Settings tab")
    void finishOnSettingsTab() {
        ApproveNewUserRequestPage page = new ApproveNewUserRequestPage(driver);
        // Unwinds first: Admin Settings and the request screens have no bottom
        // nav, so a bare openSettingsTab() fails there.
        assertTrue(page.ensureOnSettingsTab(Duration.ofSeconds(45)),
                "Should finish on the Settings tab");
        screenshot("approve-new-user-finished");
    }
}
