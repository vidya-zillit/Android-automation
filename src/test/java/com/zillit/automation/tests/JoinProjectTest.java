package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.core.Messages;
import com.zillit.automation.pages.JoinProjectPage;
import com.zillit.automation.pages.StartAndJoinPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Join Project — validations chained on ONE screen instance: the app stays on
 * the join screen and each test just swaps the input, like a user retrying.
 */
@Order(TestOrder.PROJECT_JOIN)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JoinProjectTest extends BaseTest {

    @Override
    protected boolean relaunchBetweenTests() {
        return false;
    }

    /** Returns the join screen, opening (or recovering to) it when needed. */
    private JoinProjectPage joinScreen() {
        JoinProjectPage page = new JoinProjectPage(driver);
        if (page.isStillOnPage(Duration.ZERO)) {
            return page;
        }
        try {
            new StartAndJoinPage(driver).tapJoinProject();
        } catch (org.openqa.selenium.WebDriverException e) {
            relaunchApp();
            new StartAndJoinPage(driver).tapJoinProject();
        }
        assertTrue(page.isLoaded(), "Join Project screen should open");
        return page;
    }

    @Test
    @Order(1)
    @DisplayName("Join screen shows code field, terms checkbox and Next; QR-join hidden on phones")
    void allElementsVisible() {
        JoinProjectPage page = joinScreen();
        assertTrue(page.isTermsCheckboxVisible(), "Terms checkbox should be visible");
        assertTrue(page.isNextVisible(), "Next button should be visible");
        // JoinProjectActivity.initM() hides the QR button on phones by design.
        assertTrue(!page.isQrJoinButtonShown(),
                "QR-join button must stay hidden on phone devices");
        screenshot("join-elements");
    }

    @Test
    @Order(2)
    @DisplayName("Empty project code shows 'Project Code Cannot be Empty'")
    void emptyCodeShowsError() {
        JoinProjectPage page = joinScreen();
        page.tapNext();
        assertTrue(page.textShown(Messages.PROJECT_CODE_EMPTY),
                "Expected snackbar: " + Messages.PROJECT_CODE_EMPTY);
    }

    @Test
    @Order(3)
    @DisplayName("Whitespace-only code is treated as empty")
    void whitespaceCodeShowsError() {
        JoinProjectPage page = joinScreen();
        page.enterProjectCode("   ");
        page.tapNext();
        assertTrue(page.textShown(Messages.PROJECT_CODE_EMPTY),
                "Whitespace-only code should be rejected as empty");
    }

    @Test
    @Order(4)
    @DisplayName("Valid-looking code without accepting terms shows terms error")
    void codeWithoutTermsShowsError() {
        JoinProjectPage page = joinScreen();
        page.enterProjectCode("abc123");
        page.tapNext();
        assertTrue(page.textShown(Messages.ACCEPT_TERMS),
                "Expected snackbar: " + Messages.ACCEPT_TERMS);
    }

    /** Random 6-digit code, never the real project code (983699). */
    private static String randomCode() {
        java.util.Random rnd = new java.util.Random();
        String code;
        do {
            code = String.valueOf(100000 + rnd.nextInt(900000));
        } while ("983699".equals(code));
        return code;
    }

    @Test
    @Order(5)
    @DisplayName("Random 6-digit code is rejected by the server and the user stays on the join screen")
    void invalidCodeRejectedByServer() {
        JoinProjectPage page = joinScreen();
        page.enterProjectCode(randomCode());
        page.acceptTerms();
        page.tapNext();
        assertTrue(page.isStillOnPage(Duration.ofSeconds(12)),
                "Invalid code must not navigate away from the join screen");
        screenshot("join-invalid-code");
    }

    @Test
    @Order(6)
    @DisplayName("Very long code with special characters does not crash the app")
    void hugeSpecialCharCodeDoesNotCrash() {
        JoinProjectPage page = joinScreen();
        String hostile = "a!@#$%^&*()_+{}|:<>?~`b".repeat(8); // 184 chars of noise
        page.enterProjectCode(hostile);
        page.tapNext();
        assertTrue(page.isStillOnPage(Duration.ofSeconds(12)),
                "Hostile input must not navigate away");
        assertEquals(appPackage, driver.getCurrentPackage(),
                "App must still be in the foreground (no crash)");
    }

    @Test
    @Order(7)
    @DisplayName("Back from the join screen returns to Start & Join")
    void backReturnsToStartAndJoin() {
        JoinProjectPage page = joinScreen();
        page.goBack();
        assertTrue(new StartAndJoinPage(driver).isLoaded(),
                "Back should land on the Start & Join screen");
    }
}
