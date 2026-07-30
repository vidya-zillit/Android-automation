package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.Messages;
import com.zillit.automation.pages.StartAndJoinPage;
import com.zillit.automation.pages.StartProjectPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Create Project — client-side validation ladder, walked on ONE app session.
 * The app stays on the form; each test mutates only the field it exercises,
 * exactly in the order the app validates (a real user fixing one error at a
 * time). If the form is ever lost, the next test recovers by relaunching.
 */
@Order(2)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CreateProjectValidationTest extends BaseTest {

    @Override
    protected boolean relaunchBetweenTests() {
        return false;
    }

    /** Returns the form, navigating (or recovering) to it when needed. */
    private StartProjectPage form() {
        StartProjectPage page = new StartProjectPage(driver);
        if (page.isFormVisible()) {
            return page;
        }
        if (!isOnStartAndJoin()) {
            relaunchApp();
        }
        new StartAndJoinPage(driver).tapStartProject();
        assertTrue(page.isLoaded(), "Start Project form should open");
        return page;
    }

    private boolean isOnStartAndJoin() {
        try {
            return driver.findElement(
                    io.appium.java_client.AppiumBy.id(appPackage + ":id/start_project_btn"))
                    .isDisplayed();
        } catch (org.openqa.selenium.WebDriverException e) {
            return false;
        }
    }

    @Test
    @Order(1)
    @DisplayName("Create-project form renders all fields down to the Submit button")
    void formRenders() {
        StartProjectPage page = form();
        screenshot("create-form-top");
        assertTrue(page.scrollToSubmit(), "Submit button should be reachable by scrolling");
        screenshot("create-form-bottom");
    }

    @Test
    @Order(2)
    @DisplayName("Verify tap without a language selected shows language error (masks email error)")
    void verifyTapWithoutLanguage() {
        StartProjectPage page = form();
        page.setEmail("vidyasagar+01@zillit.com");
        page.tapVerifyEmail();
        assertTrue(page.textShown(Messages.SELECT_LANGUAGE),
                "Expected snackbar: " + Messages.SELECT_LANGUAGE);
        page.setEmail(""); // restore the ladder's starting state
    }

    @Test
    @Order(3)
    @DisplayName("Submitting a fully empty form shows 'All Fields are required'")
    void emptyFormShowsAllRequired() {
        StartProjectPage page = form();
        page.tapSubmit();
        assertTrue(page.textShown(Messages.ALL_REQUIRED),
                "Expected snackbar: " + Messages.ALL_REQUIRED);
    }

    @Test
    @Order(4)
    @DisplayName("First name shorter than 3 characters is rejected")
    void shortFirstName() {
        StartProjectPage page = form();
        page.setFirstName("Vi");
        page.tapSubmit();
        assertTrue(page.textShown(Messages.FIRST_NAME_LENGTH),
                "Expected snackbar: " + Messages.FIRST_NAME_LENGTH);
    }

    @Test
    @Order(5)
    @DisplayName("First name longer than 15 characters is rejected")
    void longFirstName() {
        StartProjectPage page = form();
        page.setFirstName("Vidyasagarabcdefg"); // 17 chars
        page.tapSubmit();
        assertTrue(page.textShown(Messages.FIRST_NAME_LENGTH),
                "Expected snackbar: " + Messages.FIRST_NAME_LENGTH);
    }

    @Test
    @Order(6)
    @DisplayName("First name with special characters is rejected")
    void specialCharFirstName() {
        StartProjectPage page = form();
        page.setFirstName("Vid@#$");
        page.tapSubmit();
        assertTrue(page.textShown(Messages.SPECIAL_CHARS),
                "Expected snackbar: " + Messages.SPECIAL_CHARS);
    }

    @Test
    @Order(7)
    @DisplayName("Valid first name with empty last name is rejected")
    void emptyLastName() {
        StartProjectPage page = form();
        page.setFirstName("Vidya"); // fix the previous field, expose the next rule
        page.tapSubmit();
        assertTrue(page.textShown(Messages.LAST_NAME_EMPTY),
                "Expected snackbar: " + Messages.LAST_NAME_EMPTY);
    }

    @Test
    @Order(8)
    @DisplayName("Last name shorter than 3 characters is rejected")
    void shortLastName() {
        StartProjectPage page = form();
        page.setLastName("Sa");
        page.tapSubmit();
        assertTrue(page.textShown(Messages.LAST_NAME_LENGTH),
                "Expected snackbar: " + Messages.LAST_NAME_LENGTH);
    }

    @Test
    @Order(9)
    @DisplayName("Last name with special characters is rejected")
    void specialCharLastName() {
        StartProjectPage page = form();
        page.setLastName("Sagar$%");
        page.tapSubmit();
        assertTrue(page.textShown(Messages.SPECIAL_CHARS),
                "Expected snackbar: " + Messages.SPECIAL_CHARS);
    }

    @Test
    @Order(10)
    @DisplayName("Valid names without a project type show 'Please Select Project Type'")
    void missingProjectType() {
        StartProjectPage page = form();
        page.setLastName("Sagar");
        page.tapSubmit();
        assertTrue(page.textShown(Messages.SELECT_PROJECT_TYPE),
                "Expected snackbar: " + Messages.SELECT_PROJECT_TYPE);
    }

    @Test
    @Order(11)
    @DisplayName("Entertainment project without a sub-type shows sub-type error")
    void missingSubType() {
        StartProjectPage page = form();
        assumeTrue(page.selectProjectType("Entertainment"),
                "'Entertainment' project type not available from backend — skipping");
        page.tapSubmit();
        assertTrue(page.textShown(Messages.SELECT_PROJECT_SUBTYPE),
                "Expected snackbar: " + Messages.SELECT_PROJECT_SUBTYPE);
    }

    @Test
    @Order(12)
    @DisplayName("Sub-type selected but no language shows language error")
    void missingLanguage() {
        StartProjectPage page = form();
        assumeTrue(page.selectSubTypeFromList("Feature"),
                "'Feature' sub-type not available — skipping");
        page.tapSubmit();
        assertTrue(page.textShown(Messages.SELECT_LANGUAGE),
                "Expected snackbar: " + Messages.SELECT_LANGUAGE);
    }

    @Test
    @Order(13)
    @DisplayName("Empty project name is rejected")
    void emptyProjectName() {
        StartProjectPage page = form();
        page.selectLanguage("English");
        page.tapSubmit();
        assertTrue(page.textShown(Messages.PROJECT_NAME_EMPTY),
                "Expected snackbar: " + Messages.PROJECT_NAME_EMPTY);
    }

    @Test
    @Order(14)
    @DisplayName("Project name shorter than 3 characters is rejected")
    void shortProjectName() {
        StartProjectPage page = form();
        page.setProjectName("ab");
        page.tapSubmit();
        assertTrue(page.textShown(Messages.PROJECT_NAME_LENGTH),
                "Expected snackbar: " + Messages.PROJECT_NAME_LENGTH);
    }

    @Test
    @Order(15)
    @DisplayName("Project name longer than 25 characters is rejected")
    void longProjectName() {
        StartProjectPage page = form();
        page.setProjectName("abcdefghijklmnopqrstuvwxyz"); // 26 chars
        page.tapSubmit();
        assertTrue(page.textShown(Messages.PROJECT_NAME_LENGTH),
                "Expected snackbar: " + Messages.PROJECT_NAME_LENGTH);
    }

    @Test
    @Order(16)
    @DisplayName("Empty email is rejected on submit")
    void emptyEmail() {
        StartProjectPage page = form();
        page.setProjectName("QA Automation");
        page.tapSubmit();
        assertTrue(page.textShown(Messages.EMAIL_EMPTY),
                "Expected snackbar: " + Messages.EMAIL_EMPTY);
    }

    @Test
    @Order(17)
    @DisplayName("Malformed email is rejected on submit")
    void invalidEmail() {
        StartProjectPage page = form();
        page.setEmail("bad@@zillit");
        page.tapSubmit();
        assertTrue(page.textShown(Messages.EMAIL_INVALID),
                "Expected snackbar: " + Messages.EMAIL_INVALID);
    }

    @Test
    @Order(18)
    @DisplayName("Valid but unverified email blocks submission")
    void unverifiedEmailBlocksSubmit() {
        StartProjectPage page = form();
        page.setEmail("vidyasagar+01@zillit.com");
        page.tapSubmit();
        assertTrue(page.textShown(Messages.EMAIL_NOT_VERIFIED),
                "Expected snackbar: " + Messages.EMAIL_NOT_VERIFIED);
    }

    @Test
    @Order(19)
    @DisplayName("Verify tap with an empty email (language selected) shows email error")
    void verifyTapWithEmptyEmail() {
        StartProjectPage page = form();
        page.setEmail("");
        page.tapVerifyEmail();
        assertTrue(page.textShown(Messages.EMAIL_EMPTY),
                "Expected snackbar: " + Messages.EMAIL_EMPTY);
    }

    @Test
    @Order(20)
    @DisplayName("Verify tap with a malformed email (language selected) shows invalid-email error")
    void verifyTapWithInvalidEmail() {
        StartProjectPage page = form();
        page.setEmail("not-an-email");
        page.tapVerifyEmail();
        assertTrue(page.textShown(Messages.EMAIL_INVALID),
                "Expected snackbar: " + Messages.EMAIL_INVALID);
    }
}
