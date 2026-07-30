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
 * Create Project — sub-type rules per project type, on one shared screen.
 * Tests are skipped (not failed) when the backend doesn't offer that type.
 */
@Order(3)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CreateProjectSubTypeTest extends BaseTest {

    private boolean namesFilled;

    @Override
    protected boolean relaunchBetweenTests() {
        return false;
    }

    private StartProjectPage form() {
        StartProjectPage page = new StartProjectPage(driver);
        if (!page.isFormVisible()) {
            new StartAndJoinPage(driver).tapStartProject();
            assertTrue(page.isLoaded(), "Start Project form should open");
            namesFilled = false;
        }
        if (!namesFilled) {
            page.setFirstName("Vidya");
            page.setLastName("Sagar");
            namesFilled = true;
        }
        return page;
    }

    @Test
    @Order(1)
    @DisplayName("Entertainment project without a sub-type shows sub-type error")
    void entertainmentWithoutSubTypeRejected() {
        StartProjectPage page = form();
        assumeTrue(page.selectProjectType("Entertainment"),
                "'Entertainment' project type not available — skipping");
        assertTrue(page.isSubTypeFieldVisible(),
                "Sub-type field should appear for Entertainment projects");
        page.tapSubmit();
        assertTrue(page.textShown(Messages.SELECT_PROJECT_SUBTYPE),
                "Expected snackbar: " + Messages.SELECT_PROJECT_SUBTYPE);
        screenshot("subtype-entertainment-error");
    }

    @Test
    @Order(2)
    @DisplayName("'Other' project with a sub-type under 3 characters is rejected")
    void otherWithShortSubTypeRejected() {
        StartProjectPage page = form();
        assumeTrue(page.selectProjectType("Other"),
                "'Other' project type not available — skipping");
        assumeTrue(page.isSubTypeFieldVisible(),
                "Sub-type field not shown for Other projects — skipping");
        page.setSubType("ab");
        page.tapSubmit();
        assertTrue(page.textShown(Messages.SUBTYPE_LENGTH),
                "Expected snackbar: " + Messages.SUBTYPE_LENGTH);
    }

    @Test
    @Order(3)
    @DisplayName("Personal project hides the sub-type field entirely")
    void personalHidesSubType() {
        StartProjectPage page = form();
        assumeTrue(page.selectProjectType("Personal"),
                "'Personal' project type not available — skipping");
        assertTrue(!page.isSubTypeFieldVisible(),
                "Sub-type field must be hidden for Personal projects");
    }
}
