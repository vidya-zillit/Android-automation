package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.core.Config;
import com.zillit.automation.core.Messages;
import com.zillit.automation.pages.JoinProjectPage;
import com.zillit.automation.pages.JoinUserDetailPage;
import com.zillit.automation.pages.ProjectListPage;
import com.zillit.automation.pages.StartAndJoinPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Join with the REAL project code, then validate every rule on the
 * user-details page that follows.
 *
 * MANUAL STEP: the first test opens the join screen and waits up to 5 minutes
 * for a human to type the project code (983699), tick the terms checkbox and
 * tap Next. (If zillit.projectCode is set in config, entry is automated.)
 * Once the user-details page opens, the whole validation ladder runs chained
 * on that one screen. The final case stops at the take-a-selfie dialog — the
 * selfie/actual submission stays manual (camera + face recognition).
 */
@Order(TestOrder.PROJECT_JOIN_E2E)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JoinProjectE2ETest extends BaseTest {

    private static final Duration MANUAL_WAIT = Duration.ofMinutes(5);

    @Override
    protected boolean relaunchBetweenTests() {
        return false;
    }

    @Override
    protected boolean requiresLoggedOutStart() {
        return false;
    }

    /** The details form; every test after the first assumes we're on it. */
    private JoinUserDetailPage details() {
        JoinUserDetailPage page = new JoinUserDetailPage(driver);
        assumeTrue(page.isLoaded(Duration.ofSeconds(5)),
                "Not on the user-details page — the join step must have succeeded first");
        return page;
    }

    @Test
    @Order(1)
    @DisplayName("Join with the real project code opens the user-details page")
    void joinWithRealCode() {
        // Reach the join screen from whichever landing the app is on.
        if (landing == Landing.BOTTOM_NAV) {
            // Inside a project — hop to the project list via Change Project.
            new com.zillit.automation.pages.BottomNavPage(driver).tapChangeProject();
            landing = Landing.PROJECT_LIST;
        }
        if (landing == Landing.PROJECT_LIST) {
            new ProjectListPage(driver).isLoaded();
            driver.findElement(byAppId("join_project_btn")).click();
        } else if (landing == Landing.START_AND_JOIN) {
            new StartAndJoinPage(driver).tapJoinProject();
        } else {
            assumeTrue(false, "Cannot reach the join screen from landing " + landing);
        }
        JoinProjectPage join = new JoinProjectPage(driver);
        assertTrue(join.isLoaded(), "Join screen should open");

        String code = Config.get("zillit.projectCode");
        if (!code.isEmpty()) {
            join.enterProjectCode(code);
            join.acceptTerms();
            join.tapNext();
        } else {
            System.out.println("\n==============================================================");
            System.out.println(">>> ENTER THE PROJECT CODE ON THE EMULATOR NOW (e.g. 983699)");
            System.out.println(">>> Type the code, tick Terms, and tap Next.");
            System.out.println(">>> Waiting up to 5 minutes...");
            System.out.println("==============================================================\n");
        }

        JoinUserDetailPage detailPage = new JoinUserDetailPage(driver);
        assertTrue(detailPage.isLoaded(MANUAL_WAIT),
                "User-details page should open after a valid code was submitted");
        screenshot("join-user-details");
    }

    @Test
    @Order(2)
    @DisplayName("Submitting the empty details form flags the first name")
    void emptyDetailsForm() {
        // App quirk: the "All Fields are required" branch is unreachable here —
        // department/designation always hold placeholder text, never "", so the
        // ladder falls through to the first-name check instead.
        JoinUserDetailPage page = details();
        page.tapSubmit();
        assertTrue(page.textShown(Messages.FIRST_NAME_EMPTY),
                "Expected snackbar: " + Messages.FIRST_NAME_EMPTY);
    }

    @Test
    @Order(3)
    @DisplayName("First name shorter than 3 characters is rejected")
    void shortFirstName() {
        JoinUserDetailPage page = details();
        page.setFirstName("Vi");
        page.tapSubmit();
        assertTrue(page.textShown(Messages.FIRST_NAME_LENGTH),
                "Expected snackbar: " + Messages.FIRST_NAME_LENGTH);
    }

    @Test
    @Order(4)
    @DisplayName("First name with special characters is rejected")
    void specialFirstName() {
        JoinUserDetailPage page = details();
        page.setFirstName("Vid@#$");
        page.tapSubmit();
        assertTrue(page.textShown(Messages.SPECIAL_CHARS),
                "Expected snackbar: " + Messages.SPECIAL_CHARS);
    }

    @Test
    @Order(5)
    @DisplayName("Valid first name with empty last name is rejected")
    void emptyLastName() {
        JoinUserDetailPage page = details();
        page.setFirstName("Vidya");
        page.tapSubmit();
        assertTrue(page.textShown(Messages.LAST_NAME_EMPTY),
                "Expected snackbar: " + Messages.LAST_NAME_EMPTY);
    }

    @Test
    @Order(6)
    @DisplayName("Last name shorter than 3 characters is rejected")
    void shortLastName() {
        JoinUserDetailPage page = details();
        page.setLastName("Sa");
        page.tapSubmit();
        assertTrue(page.textShown(Messages.LAST_NAME_LENGTH),
                "Expected snackbar: " + Messages.LAST_NAME_LENGTH);
    }

    @Test
    @Order(7)
    @DisplayName("Last name with special characters is rejected")
    void specialLastName() {
        JoinUserDetailPage page = details();
        page.setLastName("Sagar$%");
        page.tapSubmit();
        assertTrue(page.textShown(Messages.SPECIAL_CHARS),
                "Expected snackbar: " + Messages.SPECIAL_CHARS);
    }

    @Test
    @Order(8)
    @DisplayName("Valid names without a department show department error")
    void missingDepartment() {
        JoinUserDetailPage page = details();
        page.setLastName("Sagar");
        page.tapSubmit();
        assertTrue(page.textShown(Messages.SELECT_DEPARTMENT),
                "Expected snackbar: " + Messages.SELECT_DEPARTMENT);
    }

    @Test
    @Order(9)
    @DisplayName("Opening designation before department shows 'select department first'")
    void designationBeforeDepartment() {
        JoinUserDetailPage page = details();
        page.tapDesignation();
        assertTrue(page.textShown(Messages.SELECT_DEPARTMENT_FIRST),
                "Expected snackbar: " + Messages.SELECT_DEPARTMENT_FIRST);
    }

    @Test
    @Order(10)
    @DisplayName("Department selected but no designation shows designation error")
    void missingDesignation() {
        JoinUserDetailPage page = details();
        assertTrue(page.selectFirstDepartment(), "Should pick a department from the list");
        page.tapSubmit();
        assertTrue(page.textShown(Messages.SELECT_DESIGNATION),
                "Expected snackbar: " + Messages.SELECT_DESIGNATION);
    }

    @Test
    @Order(11)
    @DisplayName("Designation selected but no unit shows unit error")
    void missingUnit() {
        JoinUserDetailPage page = details();
        assertTrue(page.selectFirstDesignation(), "Should pick a designation from the list");
        page.tapSubmit();
        assertTrue(page.textShown(Messages.SELECT_UNIT),
                "Expected snackbar: " + Messages.SELECT_UNIT);
    }

    @Test
    @Order(12)
    @DisplayName("Malformed optional email is rejected")
    void invalidEmail() {
        JoinUserDetailPage page = details();
        assertTrue(page.selectFirstUnit(), "Should pick a unit from the dropdown");
        page.setEmail("bad@@zillit");
        page.tapSubmit();
        assertTrue(page.textShown(Messages.EMAIL_INVALID),
                "Expected snackbar: " + Messages.EMAIL_INVALID);
    }

    @Test
    @Order(13)
    @DisplayName("Phone without a country code is rejected")
    void phoneWithoutCountry() {
        JoinUserDetailPage page = details();
        page.setEmail("");
        page.setPhone("9876543210");
        page.tapSubmit();
        assertTrue(page.textShown(Messages.COUNTRY_CODE_REQUIRED),
                "Expected snackbar: " + Messages.COUNTRY_CODE_REQUIRED);
    }

    @Test
    @Order(14)
    @DisplayName("Fully valid details reach the take-a-selfie step")
    void validDetailsReachSelfieStep() {
        JoinUserDetailPage page = details();
        page.setPhone("");
        page.tapSubmit();
        boolean selfieAsked = page.textShown(Messages.TAKE_SELFIE, Duration.ofSeconds(15));
        screenshot("join-selfie-dialog");
        assertTrue(selfieAsked,
                "All validations passed — the selfie dialog should appear");
        page.dismissDialog(); // camera/face-recognition stays manual
        System.out.println(">>> Details validated end-to-end; selfie + final submission left manual");
    }
}
