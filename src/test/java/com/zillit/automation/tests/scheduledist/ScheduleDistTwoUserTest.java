package com.zillit.automation.tests.scheduledist;

import com.zillit.automation.core.TestOrder;

import com.zillit.automation.core.DriverFactory;
import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.pages.common.BottomNavPage;
import com.zillit.automation.pages.common.ProjectListPage;
import com.zillit.automation.pages.common.ToolsPage;
import com.zillit.automation.pages.scheduledist.ScheduleDistPage;
import com.zillit.automation.pages.scheduledist.ScheduleUploadPage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * SCHEDULE DISTRIBUTION across TWO devices — web gap SCH-03: the red badge on the Tools tile
 * "Schedule Full &amp; One Line" must clear once the upload that raised it is deleted.
 *
 * <p>Device A ({@code ZILLIT_UDID}, the emulator, Vidya Testing) uploads and deletes a page;
 * device B ({@code ZILLIT_RECEIVER_UDID}, the Pixel, Vidya Pixel) only READS its Tools tab — it
 * never opens the tool, which would clear the badge for the wrong reason. Each step swaps
 * {@link #driver}, as {@code CallSheetApprovalTest} does, so the base navigation works on
 * either side.
 *
 * <p>Leaves nothing: the page is scene {@code 96ZLAUTO<run>}, deleted in the case (and in the
 * {@code finally}); {@code ScheduleDistTest} SD-99 sweeps any ZLAUTO folder a broken run leaves.
 */
@ExtendWith(ReasonLogger.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Order(TestOrder.SCHEDULE_DIST_TWO_USER)
class ScheduleDistTwoUserTest extends ScheduleDistBaseTest {

    private static final String RUN = String.format("%04d", System.currentTimeMillis() % 10000);
    private final String scene = "96" + ScheduleDistTest.SCENE_MARK + RUN;
    private final String pageNo = "ZL_AUTO_" + RUN + "_badge";
    private AndroidDriver a, b;

    @BeforeAll
    void secondDevice() {
        String udidB = System.getenv().getOrDefault("ZILLIT_RECEIVER_UDID", "");
        assumeTrue(!udidB.isBlank(), "ZILLIT_RECEIVER_UDID is required — the badge is read on a second user's device");
        a = driver;
        b = DriverFactory.create(udidB, 8210);
    }

    @AfterAll
    void closeSecondDevice() {
        if (b != null) { try { b.quit(); } catch (Exception ignored) { } }
        driver = a;
    }

    private <T> T on(AndroidDriver d, Supplier<T> body) {
        AndroidDriver keep = driver;
        driver = d;
        try { return body.get(); } finally { driver = keep; }
    }

    @Test
    @Order(10)
    @DisplayName("SD-70 [WEB SCH-03] (A uploads, B sees the badge rise; A deletes, B's badge clears)")
    void badgeClearsAfterDelete() {
        int b0 = on(b, this::badgeOnB);
        System.out.println(">>> SD-70 B's badge before: " + b0);
        boolean deleted = false;
        try {
            boolean up = on(a, () -> uploadPage(scene, pageNo, "BLUE", ScheduleUploadPage.TYPE_FULL_PAGES));
            assumeTrue(up, "A's page upload did not finish");

            int raised = on(b, () -> waitBadge(n -> n >= b0 + 1, Duration.ofSeconds(60)));
            System.out.println(">>> SD-70 B's badge after A's upload: " + raised);
            assumeTrue(raised >= b0 + 1, "Control failed: B's badge did not rise after A's upload (" + b0 + " → "
                    + raised + "), so whether it clears cannot be judged");

            deleted = on(a, this::deleteThePage);
            assumeTrue(deleted, "A could not delete the page");

            int after = on(b, () -> waitBadge(n -> n <= b0, Duration.ofSeconds(30)));
            System.out.println(">>> SD-70 B's badge after A's delete: " + after);
            assertTrue(after <= b0, "[SCH-03] B's Tools badge on \"" + TOOL + "\" stayed at " + after
                    + " after A deleted the upload that raised it (it was " + b0 + " before the upload)");
        } finally {
            if (!deleted) {
                try { on(a, this::deleteThePage); } catch (Exception | AssertionError ignored) { }
            }
            on(a, () -> { leaveSchedule(); return null; });
        }
    }

    // ─────────────────────────── A side ───────────────────────────

    private boolean deleteThePage() {
        ScheduleDistPage inside = openFolder(scene);
        if (inside.openRowMenu(r -> ("Page No. " + pageNo).equals(r.name)) == null
                || !inside.tapMenuItem("Delete") || !inside.dialogShown()) {
            backToTabs();
            return false;
        }
        inside.tapDialogRight();
        boolean gone = false;
        for (int i = 0; i < 8 && !gone; i++) {
            sleep(2500);
            gone = new ScheduleDistPage(driver).visibleRows().stream()
                    .noneMatch(r -> ("Page No. " + pageNo).equals(r.name));
        }
        backToTabs();
        tab(ScheduleDistPage.TAB_PAGES).searchPages("");
        return gone;
    }

    // ─────────────────────────── B side ───────────────────────────

    /** B's Tools tab → the tile's badge count (hidden = 0). Re-enters the tab to refresh it. */
    private int badgeOnB() {
        reachBottomNavOnB();
        tapBottomTab(0);                       // leave Tools so re-entering refetches
        sleep(1500);
        ToolsPage tools = new ToolsPage(driver);
        assumeTrue(tools.openToolsTab() && tools.isLoaded(Duration.ofSeconds(20)), "B's Tools tab did not load");
        tools.searchTools("Schedule Full");
        sleep(1500);
        try {
            List<WebElement> badge = driver.findElements(AppiumBy.xpath(
                    "//*[contains(@resource-id,':id/nameTv') and @text=\"" + TOOL + "\"]"
                            + "/following-sibling::*[contains(@resource-id,':id/badge')]"));
            if (badge.isEmpty() || !badge.get(0).isDisplayed()) return 0;
            String t = String.valueOf(badge.get(0).getText()).replaceAll("\\D", "");
            return t.isEmpty() ? 1 : Integer.parseInt(t);
        } catch (Exception e) {
            return 0;
        }
    }

    private int waitBadge(java.util.function.IntPredicate ok, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        int n = badgeOnB();
        while (!ok.test(n) && System.currentTimeMillis() < deadline) {
            sleep(5000);
            n = badgeOnB();
        }
        return n;
    }

    private void reachBottomNavOnB() {
        bringAppToFront();
        BottomNavPage nav = new BottomNavPage(driver);
        for (int i = 0; i < 6 && !nav.waitUntilLoaded(Duration.ofSeconds(2)); i++) {
            if (onProjectList()) {
                ProjectListPage projects = new ProjectListPage(driver);
                List<String> names = projects.visibleProjectNames();
                assumeTrue(!names.isEmpty() && projects.tapProject(names.get(0)), "B could not enter a project");
                nav.waitUntilLoaded(Duration.ofSeconds(60));
                break;
            }
            if (closeBlockingDialog()) continue;
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(900);
        }
        assumeTrue(nav.waitUntilLoaded(Duration.ofSeconds(10)), "B never reached the bottom navigation: " + whereAreWe());
    }

    private void tapBottomTab(int index) {
        try {
            List<WebElement> items = driver.findElement(byAppId("bottomNavigation")).findElements(byAppId("menuText"));
            if (items.size() > index) items.get(index).click();
        } catch (Exception ignored) { }
    }
}
