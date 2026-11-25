package com.zillit.automation.tests.project;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.Config;
import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.common.BottomNavPage;
import com.zillit.automation.pages.common.ProjectListPage;
import com.zillit.automation.pages.project.StartProjectPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The first module of the full sweep ({@code --all}): create a project, then select it.
 *
 * <p>Only after the project has been SELECTED is its name written to {@link #RUN_PROJECT_FILE};
 * the runner passes it to every later module as ZILLIT_RUN_PROJECT, and stops the sweep when the
 * file is missing, so nothing is written into some other project.
 *
 * <p>MANUAL STEP: the email OTP. The test waits up to 5 minutes for you to type it on the device
 * and tap Verify. The address is {@code zillit.createEmail}.
 */
@Order(TestOrder.PROJECT_CREATE_AND_SELECT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ExtendWith(ReasonLogger.class)
class CreateAndSelectProjectTest extends BaseTest {

    static final Path RUN_PROJECT_FILE = Path.of("build", "run-project.txt");
    private static final Duration OTP_WAIT = Duration.ofMinutes(5);
    private static final Duration ENTER = Duration.ofSeconds(60);

    private static String created;

    @Override protected boolean relaunchBetweenTests() { return false; }

    /** Never clear app data: the account must stay signed in for the rest of the sweep. */
    @Override protected boolean requiresLoggedOutStart() { return false; }

    @Test
    @Order(1)
    @DisplayName("Create a project (you enter the email OTP)")
    void createProject() throws IOException {
        Files.deleteIfExists(RUN_PROJECT_FILE);
        dismissPermissionPrompts();
        if (landing == Landing.BOTTOM_NAV) {
            new BottomNavPage(driver).tapChangeProject();
            landing = Landing.PROJECT_LIST;
        }
        driver.findElement(byAppId("start_project_btn")).click();
        StartProjectPage page = new StartProjectPage(driver);
        assertTrue(page.isLoaded(), "Start Project form should open");

        String name = "ZL Auto Run " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHHmm"));
        String email = Config.has("zillit.createEmail") ? Config.get("zillit.createEmail") : "vidyasagar+01@zillit.com";
        page.setFirstName("Vidya");
        page.setLastName("Sagar");
        assertTrue(page.selectProjectType("Entertainment"), "'Entertainment' type unavailable");
        assertTrue(page.selectSubTypeFromList("Feature"), "'Feature' sub-type unavailable");
        page.selectLanguage("English");
        page.setProjectName(name);
        page.setEmail(email);
        page.tapVerifyEmail();
        assertTrue(page.waitForOtpSheet(Duration.ofSeconds(45)), "OTP sheet should open after tapping Verify");
        System.out.println("\n==============================================================");
        System.out.println(">>> ENTER THE EMAIL OTP ON THE DEVICE NOW (sent to " + email + ")");
        System.out.println(">>> Type the 6 digits and tap Verify. Waiting 5 minutes...");
        System.out.println("==============================================================\n");
        assertTrue(page.waitForManualOtpVerification(OTP_WAIT), "Email not verified within 5 minutes");
        page.acceptTerms();
        page.tapSubmit();
        assertTrue(page.waitForProjectCreated(ENTER) || new BottomNavPage(driver).waitUntilLoaded(ENTER),
                "Project should be created (share-code screen or main app)");
        created = name;
        screenshot("create-and-select-created");
    }

    @Test
    @Order(2)
    @DisplayName("Select the project just created and land on its Home")
    void selectCreatedProject() throws IOException {
        assumeTrue(created != null, "No project was created");
        BottomNavPage nav = new BottomNavPage(driver);
        // The share-code screen sits on top of the app; leave it without restarting the app.
        for (int i = 0; i < 4 && !onScreen("projectListRecyclerView")
                && !onScreen("bottomNavigation"); i++) {
            driver.navigate().back();
            settle();
        }
        if (!onScreen("projectListRecyclerView") && !onScreen("bottomNavigation")) {
            relaunchApp();
        }
        dismissPermissionPrompts();
        boolean inside = onScreen("bottomNavigation")
                && ProjectListPage.sameProject(nav.currentProjectName(), created);
        if (!inside) {
            ProjectListPage list = onScreen("bottomNavigation") ? nav.tapChangeProject() : new ProjectListPage(driver);
            assertTrue(list.isLoaded(), "Project list should be visible");
            list.search(created);
            assertTrue(list.tapProjectExact(created),
                    "\"" + created + "\" should be in the list. Listed: " + list.listedProjectNames());
        }
        assertTrue(nav.waitUntilLoaded(ENTER), "The created project should open");
        nav.goHome();
        String shown = nav.currentProjectName();
        assertTrue(ProjectListPage.sameProject(shown, created), "Expected to be in " + created + " but the toolbar shows " + shown);
        Files.createDirectories(RUN_PROJECT_FILE.getParent());
        Files.writeString(RUN_PROJECT_FILE, created);
        System.out.println(">>> every later module of this run uses project: " + created);
    }

    private boolean onScreen(String id) {
        return !driver.findElements(byAppId(id)).isEmpty();
    }

    private void settle() {
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
