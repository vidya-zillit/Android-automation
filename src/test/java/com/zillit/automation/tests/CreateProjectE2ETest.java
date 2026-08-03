package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.core.Messages;
import com.zillit.automation.pages.StartAndJoinPage;
import com.zillit.automation.pages.StartProjectPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * End-to-end create-project flow with real email verification.
 *
 * MANUAL STEP: when the OTP bottom sheet opens on the emulator, a human must
 * type the 6-digit OTP sent to the email and tap Verify. The test waits up to
 * 5 minutes for that. It also walks the post-verification validation ladder
 * (storage code, phone/country pairing, terms) that is unreachable without a
 * verified email. Runs LAST — it creates a real project on the dev backend.
 */
@Order(TestOrder.PROJECT_CREATE_E2E)
class CreateProjectE2ETest extends BaseTest {

    private static final String EMAIL = "vidyasagar+01@zillit.com";
    private static final Duration OTP_WAIT = Duration.ofMinutes(5);

    @Test
    @DisplayName("Full create-project flow: OTP email verification, post-verification validations, project created")
    void createProjectEndToEnd() {
        StartProjectPage page = new StartAndJoinPage(driver).tapStartProject();

        // --- fill the form with valid data ---
        // Entertainment + a real sub-type; never Personal Communication (user rule).
        page.setFirstName("Vidya");
        page.setLastName("Sagar");
        assumeTrue(page.selectProjectType("Entertainment"),
                "'Entertainment' project type not available — cannot run E2E");
        assumeTrue(page.selectSubTypeFromList("Feature"),
                "'Feature' sub-type not available — cannot run E2E");
        page.selectLanguage("English");
        String projectName = "QA Auto " + (System.currentTimeMillis() % 100000);
        page.setProjectName(projectName);
        page.setEmail(EMAIL);
        screenshot("e2e-form-filled");

        // --- email verification with manual OTP ---
        page.tapVerifyEmail();
        assertTrue(page.waitForOtpSheet(Duration.ofSeconds(45)),
                "OTP bottom sheet should open after tapping Verify");
        screenshot("e2e-otp-sheet");
        System.out.println("\n==============================================================");
        System.out.println(">>> ENTER THE OTP ON THE EMULATOR NOW (sent to " + EMAIL + ")");
        System.out.println(">>> Type the 6 digits and tap the sheet's Verify button.");
        System.out.println(">>> Waiting up to 5 minutes...");
        System.out.println("==============================================================\n");
        assertTrue(page.waitForManualOtpVerification(OTP_WAIT),
                "Email was not verified within " + OTP_WAIT.toMinutes()
                        + " minutes — OTP not entered or rejected");
        screenshot("e2e-email-verified");

        // --- editing a verified email must reset it to unverified ---
        page.setEmail(EMAIL + "x");
        assertTrue(Messages.VERIFY.equalsIgnoreCase(page.verifyLabelText()),
                "Editing the email must reset the Verified badge to Verify");
        page.setEmail(EMAIL);
        assertTrue(Messages.VERIFIED.equalsIgnoreCase(page.verifyLabelText()),
                "Restoring the verified email must restore the Verified badge");

        // --- post-verification validation ladder ---
        if (page.isStorageCodeVisible()) {
            page.setStorageCode("abc#$%");
            page.tapSubmit();
            assertTrue(page.textShown(Messages.SPECIAL_CHARS),
                    "Storage code with special characters must be rejected");
            page.setStorageCode("");
        } else {
            System.out.println(">>> Storage-code field not rendered in this build — step skipped");
        }

        page.setPhone("9876543210");
        page.tapSubmit();
        assertTrue(page.textShown(Messages.COUNTRY_CODE_REQUIRED),
                "Phone without a country code must be rejected");
        page.setPhone("");

        page.tapSubmit();
        assertTrue(page.textShown(Messages.ACCEPT_TERMS),
                "Submission without accepting terms must be rejected");
        screenshot("e2e-terms-error");

        // --- accept terms and create the project ---
        page.acceptTerms();
        page.tapSubmit();
        assertTrue(page.waitForProjectCreated(Duration.ofSeconds(90)),
                "Expected to land on the Share Project Code screen after creation");
        screenshot("e2e-project-created");
        System.out.println(">>> Project created: " + projectName);
    }
}
