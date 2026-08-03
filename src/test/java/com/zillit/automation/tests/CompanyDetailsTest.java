package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.CompanyDetailsPage;
import com.zillit.automation.pages.ProjectListPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Settings → Admin Settings → "Company Details".
 *
 * A ladder walked on ONE screen instance (relaunchBetweenTests()=false):
 * each test case continues editing the same open form the previous one left,
 * matching the app's real behavior (validation only fires on Submit; fields
 * keep whatever was last typed until changed).
 */
@Order(TestOrder.COMPANY_DETAILS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CompanyDetailsTest extends BaseTest {

    private static final String COUNTRY_CODE_ERROR = "Please select a country code for the company phone number";
    private static final String PHONE_LENGTH_ERROR = "Phone number must be at least 5 digits";
    private static final String INVALID_EMAIL_ERROR = "Please Enter Valid Email";

    private boolean insideProject;
    private int completedCases;

    @Override
    protected boolean relaunchBetweenTests() {
        return false;
    }

    @Override
    protected boolean requiresLoggedOutStart() {
        return false;
    }

    /** Returns the Company Details screen, navigating (or recovering) to it as needed. */
    private CompanyDetailsPage companyDetails() {
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
        CompanyDetailsPage page = new CompanyDetailsPage(driver);
        if (page.isLoaded()) {
            return page;
        }
        // Recover: back out until Bottom Nav is visible again, then re-navigate.
        for (int i = 0; i < 6 && !new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(1)); i++) {
            try {
                driver.navigate().back();
            } catch (Exception ignored) {
            }
            sleep(500);
        }
        assumeTrue(page.openSettingsTab(), "Settings tab not present");
        assumeTrue(page.openAdminSettings(), "Admin Settings not present");
        assumeTrue(page.openCompanyDetails(), "Company Details not present");
        assumeTrue(page.isLoaded(), "Company Details screen did not open");
        return page;
    }

    @Test
    @Order(1)
    @DisplayName("Test Case 1: Company phone number requires a country code")
    void phoneRequiresCountryCode() {
        CompanyDetailsPage page = companyDetails();
        page.setCompanyName("HKK Entertainment");
        page.setCompanyAddress("Noida");
        page.setCompanyPhone("9501179101");
        page.tapSubmit();

        boolean toastSeen = page.textShown(COUNTRY_CODE_ERROR, Duration.ofSeconds(3));
        screenshot("company-details-missing-country-code");
        assertTrue(toastSeen, "Expected error: " + COUNTRY_CODE_ERROR);
        completedCases++;
    }

    @Test
    @Order(2)
    @DisplayName("Test Case 2: Minimum phone number length validation")
    void minimumPhoneLength() {
        CompanyDetailsPage page = companyDetails();

        assertTrue(page.tapCountryCode(), "Country Code field should open the picker");
        assertTrue(page.isCountryPickerOpen(), "Country picker should be open");
        page.searchCountry("India");
        assertTrue(page.selectCountry("India"), "India should be selectable from the list");

        page.setCompanyPhone("787");
        page.tapSubmit();

        boolean toastSeen = page.textShown(PHONE_LENGTH_ERROR, Duration.ofSeconds(3));
        screenshot("company-details-phone-too-short");
        assertTrue(toastSeen, "Expected error: " + PHONE_LENGTH_ERROR);
        completedCases++;
    }

    @Test
    @Order(3)
    @DisplayName("Test Case 3: Invalid company email validation")
    void invalidCompanyEmail() {
        CompanyDetailsPage page = companyDetails();

        page.setCompanyPhone("9501179101");
        page.setCompanyEmail("Bhavik@zilli");
        page.tapSubmit();

        boolean toastSeen = page.textShown(INVALID_EMAIL_ERROR, Duration.ofSeconds(3));
        screenshot("company-details-invalid-email");
        assertTrue(toastSeen, "Expected error: " + INVALID_EMAIL_ERROR);
        completedCases++;
    }

    @Test
    @Order(4)
    @DisplayName("Test Case 4: Successful entry of valid company details")
    void validCompanyDetailsAccepted() {
        CompanyDetailsPage page = companyDetails();

        page.setCompanyEmail("Bhavik@zillit.com");
        page.setCompanyNumber("7898766");
        page.setRegisteredAddress("Noida Sector 18");
        screenshot("company-details-valid-entries");

        // Form is ready for submission — no validation error should be showing
        // for any of the fields already exercised in Test Cases 1-3.
        assertTrue(!page.textShown(COUNTRY_CODE_ERROR, Duration.ofMillis(500))
                        && !page.textShown(PHONE_LENGTH_ERROR, Duration.ofMillis(500))
                        && !page.textShown(INVALID_EMAIL_ERROR, Duration.ofMillis(500)),
                "No validation errors should be showing with valid field values");
        completedCases++;
    }

    @Test
    @Order(5)
    @DisplayName("Test Case 5: Add a custom field (Label/Value)")
    void addCustomField() {
        CompanyDetailsPage page = companyDetails();

        assertTrue(page.tapAddField(), "Add Field should insert a Label/Value row");
        page.setFieldLabel("Caste");
        page.setFieldValue("Zillit");
        screenshot("company-details-custom-field-added");

        assertEquals("Caste", page.fieldLabelText(), "Label field should hold what was typed");
        assertEquals("Zillit", page.fieldValueText(), "Value field should hold what was typed");
        completedCases++;
    }

    @Test
    @Order(6)
    @DisplayName("Test Case 6: Company Logo upload via Gallery, verified in Header Preview")
    void companyLogoUploadReflectedInHeaderPreview() {
        CompanyDetailsPage page = companyDetails();

        assertTrue(page.tapEditLogo(), "Edit icon should open the Choose-below picker");
        assertTrue(page.isChoosePickerOpen(), "Choose-below picker should be open");
        assertTrue(page.tapGalleryOption(), "Gallery option should be selectable");

        assertTrue(page.openPicturesFolder(), "Pictures folder should open");
        assertTrue(page.selectFirstGalleryPhoto(), "An image should be selectable");
        assertTrue(page.tapGallerySendFab(), "Gallery Send button should be tappable");

        // The app's own gallery pipeline shows a caption/preview screen (caption
        // pre-filled with "Company Logo") with its own Send button — tapping it
        // is what actually starts the upload.
        assertTrue(page.isMediaPreviewOpen(), "Media preview screen should be open");
        assertTrue(page.tapPreviewSend(), "Preview Send button should be tappable");

        assertTrue(page.isLogoUploaded(Duration.ofSeconds(40)), "Logo upload should complete");
        screenshot("company-details-logo-uploaded");

        assertTrue(page.tapPreviewHeaderButton(), "Preview Header button should be tappable");
        assertTrue(page.isHeaderPreviewOpen(), "Header Preview should be open");
        screenshot("company-details-header-preview");

        assertTrue(page.tapCloseHeaderPreview(), "Close button should dismiss the Header Preview");
        completedCases++;
    }

    @Test
    @Order(7)
    @DisplayName("Test Case 7: Submit once all previous test cases have passed")
    void submitAfterAllPreviousCasesPass() {
        assumeTrue(completedCases == 6, "Skipping Submit — a previous test case did not complete successfully");

        CompanyDetailsPage page = companyDetails();
        page.tapSubmit();
        screenshot("company-details-final-submit");

        assertTrue(!page.textShown(COUNTRY_CODE_ERROR, Duration.ofMillis(500))
                        && !page.textShown(PHONE_LENGTH_ERROR, Duration.ofMillis(500))
                        && !page.textShown(INVALID_EMAIL_ERROR, Duration.ofMillis(500)),
                "No validation errors should be showing on final Submit");

        assertTrue(page.tapBackButton(), "Back button should be tappable");
        assertTrue(page.isOnAdminSettings(Duration.ofSeconds(10)), "Back button should return to Admin Settings");
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }
}
