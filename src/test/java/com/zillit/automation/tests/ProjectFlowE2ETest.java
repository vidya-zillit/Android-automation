package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.core.Config;
import com.zillit.automation.core.Messages;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.JoinProjectPage;
import com.zillit.automation.pages.JoinUserDetailPage;
import com.zillit.automation.pages.ProjectListPage;
import com.zillit.automation.pages.StartProjectPage;
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
 * THE HAPPY-PATH JOURNEY, in the order a real user lives it, on ONE app session:
 *
 *   1. Create a project   (Entertainment / Feature, English, real email)
 *   2. Join a project      (project code 983699 → user details)
 *   3. Select a project    (tap it and enter the main app)
 *
 * Permission popups (the in-app "important Notifications" sheet and Android's
 * system dialogs) are ALLOWED automatically at every step.
 *
 * The app is launched once; between the three phases it is only relaunched
 * (terminate + reopen — no reinstall) to return to a known entry screen.
 *
 * MANUAL STEP: step 1 pauses up to 5 minutes for you to type the email OTP on
 * the device and tap the sheet's Verify button. Everything else is automatic.
 */
@Order(TestOrder.PROJECT_FLOW_E2E)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProjectFlowE2ETest extends BaseTest {

    private static final String EMAIL = "vidyasagar+01@zillit.com";
    private static final Duration OTP_WAIT = Duration.ofMinutes(5);
    private static final Duration ENTER = Duration.ofSeconds(60);

    @Override
    protected boolean relaunchBetweenTests() {
        return false; // one session; we relaunch deliberately at phase boundaries
    }

    @Override
    protected boolean requiresLoggedOutStart() {
        return false; // take the app as it is; never wipe data mid-journey
    }

    // ---------- navigation helpers (work from any landing screen) ----------

    /** If the app opened straight into a project, hop to the list first. */
    private void ensureOnListOrStart() {
        dismissPermissionPrompts();
        if (landing == Landing.BOTTOM_NAV) {
            new BottomNavPage(driver).tapChangeProject();
            landing = Landing.PROJECT_LIST;
        }
    }

    private void tapStartProject() {
        ensureOnListOrStart();
        driver.findElement(byAppId("start_project_btn")).click();
    }

    private void tapJoinProject() {
        ensureOnListOrStart();
        driver.findElement(byAppId("join_project_btn")).click();
    }

    // ============================ 1. CREATE ============================

    @Test
    @Order(1)
    @DisplayName("STEP 1 — Create a project (manual OTP), landing on the share-code screen")
    void step1_createProject() {
        tapStartProject();
        StartProjectPage page = new StartProjectPage(driver);
        assertTrue(page.isLoaded(), "Start Project form should open");

        page.setFirstName("Vidya");
        page.setLastName("Sagar");
        assumeTrue(page.selectProjectType("Entertainment"),
                "'Entertainment' type unavailable — cannot create");
        assumeTrue(page.selectSubTypeFromList("Feature"),
                "'Feature' sub-type unavailable — cannot create");
        page.selectLanguage("English");
        String projectName = "QA Flow " + (System.currentTimeMillis() % 100000);
        page.setProjectName(projectName);
        page.setEmail(EMAIL);
        screenshot("flow-1-form-filled");

        page.tapVerifyEmail();
        assertTrue(page.waitForOtpSheet(Duration.ofSeconds(45)),
                "OTP sheet should open after tapping Verify");
        System.out.println("\n==============================================================");
        System.out.println(">>> STEP 1: ENTER THE EMAIL OTP ON THE DEVICE NOW (sent to " + EMAIL + ")");
        System.out.println(">>> Type the 6 digits, tap the sheet's Verify button. Waiting 5 min...");
        System.out.println("==============================================================\n");
        assertTrue(page.waitForManualOtpVerification(OTP_WAIT),
                "Email not verified within 5 minutes — OTP not entered/incorrect");

        page.acceptTerms();
        page.tapSubmit();
        // Project created → Share Project Code screen shows the invite code.
        boolean created = new BottomNavPage(driver).textShownOnAppId("invite_code_view", ENTER)
                || page.waitForProjectCreated(ENTER);
        assertTrue(created, "Project should be created (share-code screen or main app)");
        System.out.println(">>> STEP 1 done — created project: " + projectName);
        screenshot("flow-1-created");
    }

    // ============================ 2. JOIN ============================

    @Test
    @Order(2)
    @DisplayName("STEP 2 — Join project 983699 and complete the user-details form")
    void step2_joinProject() {
        relaunchApp();          // fast reopen → project list / start & join
        tapJoinProject();

        JoinProjectPage join = new JoinProjectPage(driver);
        assertTrue(join.isLoaded(), "Join screen should open");
        String code = Config.get("zillit.projectCode");
        assumeTrue(!code.isEmpty(), "Set zillit.projectCode (e.g. 983699) to run the join step");
        join.enterProjectCode(code);
        join.acceptTerms();
        join.tapNext();

        JoinUserDetailPage details = new JoinUserDetailPage(driver);
        assertTrue(details.isLoaded(OTP_WAIT), "User-details page should open after a valid code");
        screenshot("flow-2-user-details");

        details.setFirstName("Vidya");
        details.setLastName("Sagar");
        assertTrue(details.selectFirstDepartment(), "Pick a department");
        assertTrue(details.selectFirstDesignation(), "Pick a designation");
        assertTrue(details.selectFirstUnit(), "Pick a unit");
        details.tapSubmit();
        boolean selfie = details.textShown(Messages.TAKE_SELFIE, Duration.ofSeconds(15));
        screenshot("flow-2-selfie");
        assertTrue(selfie, "Valid details should reach the take-a-selfie step");
        details.dismissDialog(); // selfie/face-recognition stays manual
        System.out.println(">>> STEP 2 done — joined 983699 and validated user details");
    }

    // ============================ 3. SELECT ============================

    @Test
    @Order(3)
    @DisplayName("STEP 3 — Select a project from the list and enter the main app")
    void step3_selectProject() {
        relaunchApp();          // fast reopen → project list
        assumeTrue(landing == Landing.PROJECT_LIST || landing == Landing.BOTTOM_NAV,
                "Need a logged-in state with projects to select one (landing=" + landing + ")");

        if (landing == Landing.BOTTOM_NAV) {
            // Already inside a project — hop out via Change Project to the list.
            new BottomNavPage(driver).tapChangeProject();
        }
        ProjectListPage list = new ProjectListPage(driver);
        assertTrue(list.isLoaded(), "Project list should be visible");
        List<String> names = list.visibleProjectNames();
        assertTrue(!names.isEmpty(), "There should be at least one project to select");

        String target = names.get(0);
        assertTrue(list.tapProject(target), "Should tap project: " + target);

        BottomNavPage main = new BottomNavPage(driver);
        boolean entered = main.waitUntilLoaded(ENTER); // auto-allows permission popups
        if (!entered) {
            screenshot("flow-3-enter-timeout");
        }
        assertTrue(entered, "Main app should open after selecting: " + target);
        assertTrue(main.currentProjectName().contains(target.split(" - ")[0]),
                "Toolbar should show the selected project's name");
        System.out.println(">>> STEP 3 done — entered project: " + main.currentProjectName());
        screenshot("flow-3-entered");
    }
}
