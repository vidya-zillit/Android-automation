package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.ExternalUserFormPage;
import com.zillit.automation.pages.ExternalUserListPage;
import com.zillit.automation.pages.ProjectListPage;
import com.zillit.automation.pages.ToolsPage;
import io.appium.java_client.AppiumBy;
import org.junit.jupiter.api.*;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Diagnostic only — not in any suite, not in --tools, not in --all.
 *
 * <p>Why will the User Type dropdown not open? Its style is the proper
 * {@code Widget.MaterialComponents.TextInputLayout.OutlinedBox.ExposedDropdownMenu}, which
 * installs click-to-show, and the adapter has three items — so a tap ought to raise a
 * popup. It does not, and eight cases are waiting on it. This tries each way of opening it
 * and dumps what is on screen after each, rather than guessing a ninth time.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExternalUserDropdownProbeTest extends BaseTest {

    @Override protected boolean requiresLoggedOutStart() { return false; }

    @Test
    @DisplayName("PROBE the User Type dropdown")
    void probe() {
        reachModule();
        ExternalUserListPage list = new ExternalUserListPage(driver);
        assumeTrue(list.isLoaded(Duration.ofSeconds(20)), "The listing did not load");
        assumeTrue(list.openAdd(), "Add was not available");

        ExternalUserFormPage f = new ExternalUserFormPage(driver);
        assumeTrue(f.isLoaded(Duration.ofSeconds(15)), "The add form did not open");
        System.out.println(">>> PROBE form is open. userType reads \"" + f.userType() + "\"");

        dump("BEFORE any tap");

        // Each chooser gets a clean screen: the last probe chained taps without closing
        // what was open, so the second tap landed inside the FIRST popup and selected from
        // it. Every tap below is preceded by a dismiss.
        tapById("department", "autoCompleteDropDown", "DEPARTMENT (app's own chooser)");
        dump("AFTER tapping DEPARTMENT");
        back("after DEPARTMENT");

        tapById("countryCode", "autoCompleteDropDown", "COUNTRY (app's own chooser)");
        dump("AFTER tapping COUNTRY");
        back("after COUNTRY");

        tapById("designation", "autoCompleteDropDown", "DESIGNATION, with no department set");
        dump("AFTER tapping DESIGNATION");
        back("after DESIGNATION");
    }

    /** Closes whatever is open, so the next tap starts from the form again. */
    private void back(String when) {
        try { driver.navigate().back(); } catch (Exception ignored) { }
        sleepQuiet(1500);
        System.out.println(">>> PROBE   dismissed " + when);
    }

    private void tapById(String container, String inner, String what) {
        System.out.println(">>> PROBE --- tapping " + what + " (" + container + "/" + inner + ")");
        try {
            List<WebElement> boxes = driver.findElements(idSel(container));
            if (boxes.isEmpty()) { System.out.println(">>> PROBE     container absent"); return; }
            List<WebElement> els = boxes.get(0).findElements(idSel(inner));
            if (els.isEmpty()) { System.out.println(">>> PROBE     inner absent"); return; }
            WebElement e = els.get(0);
            System.out.println(">>> PROBE     found: clickable=" + e.getAttribute("clickable")
                    + " enabled=" + e.getAttribute("enabled")
                    + " focusable=" + e.getAttribute("focusable")
                    + " bounds=" + e.getAttribute("bounds"));
            e.click();
            sleepQuiet(2000);
        } catch (Exception ex) {
            System.out.println(">>> PROBE     tap threw " + ex.getClass().getSimpleName());
        }
    }

    private void tapCentreOf(String container) {
        System.out.println(">>> PROBE --- coordinate tap at the centre of " + container);
        try {
            List<WebElement> boxes = driver.findElements(idSel(container));
            if (boxes.isEmpty()) { System.out.println(">>> PROBE     container absent"); return; }
            org.openqa.selenium.Rectangle r = boxes.get(0).getRect();
            int x = r.getX() + r.getWidth() / 2;
            int y = r.getY() + r.getHeight() / 2;
            org.openqa.selenium.interactions.PointerInput finger =
                    new org.openqa.selenium.interactions.PointerInput(
                            org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
            org.openqa.selenium.interactions.Sequence tap =
                    new org.openqa.selenium.interactions.Sequence(finger, 1);
            tap.addAction(finger.createPointerMove(java.time.Duration.ZERO,
                    org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, y));
            tap.addAction(finger.createPointerDown(0));
            tap.addAction(new org.openqa.selenium.interactions.Pause(
                    finger, java.time.Duration.ofMillis(120)));
            tap.addAction(finger.createPointerUp(0));
            driver.perform(List.of(tap));
            sleepQuiet(2000);
        } catch (Exception ex) {
            System.out.println(">>> PROBE     coordinate tap threw "
                    + ex.getClass().getSimpleName());
        }
    }

    /**
     * Saves a screenshot and the raw page source.
     *
     * <p>This is the measurement that separates the two possible truths: a popup that is
     * genuinely not opening, and one that is on the screen but missing from the
     * accessibility tree. Six taps have changed the element count by nothing at all, while
     * the filter popup menu on the previous screen opened and read fine — so the tree
     * alone cannot settle it.
     */
    private void shoot(String label) {
        String safe = label.replaceAll("[^A-Za-z0-9]+", "_");
        try {
            byte[] png = driver.getScreenshotAs(org.openqa.selenium.OutputType.BYTES);
            java.nio.file.Path dir = java.nio.file.Path.of(
                    System.getProperty("zillit.probeDir", "build/probe"));
            java.nio.file.Files.createDirectories(dir);
            java.nio.file.Path f = dir.resolve(safe + ".png");
            java.nio.file.Files.write(f, png);
            System.out.println(">>> PROBE   screenshot -> " + f.toAbsolutePath());
        } catch (Exception e) {
            System.out.println(">>> PROBE   screenshot failed: " + e.getClass().getSimpleName());
        }
        try {
            String src = driver.getPageSource();
            System.out.println(">>> PROBE   page source is " + src.length() + " chars;"
                    + " windows mentioned: " + countOf(src, "<android.widget.PopupWindow")
                    + " popup, " + countOf(src, "CheckedTextView") + " CheckedTextView, "
                    + countOf(src, "ListView") + " ListView, "
                    + countOf(src, "RecyclerView") + " RecyclerView");
        } catch (Exception e) {
            System.out.println(">>> PROBE   page source failed: "
                    + e.getClass().getSimpleName());
        }
    }

    private static int countOf(String hay, String needle) {
        int n = 0, i = 0;
        while ((i = hay.indexOf(needle, i)) >= 0) { n++; i += needle.length(); }
        return n;
    }

    /** Anything that looks like a list item, plus a count of what is on screen. */
    private void dump(String label) {
        System.out.println(">>> PROBE ===== " + label + " =====");
        try {
            List<WebElement> all = driver.findElements(AppiumBy.xpath("//*"));
            System.out.println(">>> PROBE   " + all.size() + " element(s) on screen");
            int shown = 0;
            for (WebElement e : all) {
                try {
                    String t = e.getText();
                    if (t == null || t.isBlank()) continue;
                    String cls = e.getAttribute("class");
                    String rid = String.valueOf(e.getAttribute("resource-id"));
                    // A popup item is a bare TextView/CheckedTextView with no id of ours.
                    boolean looksLikeItem = cls != null
                            && (cls.endsWith("CheckedTextView") || cls.endsWith("TextView"))
                            && (rid == null || rid.equals("null") || rid.isBlank()
                                || rid.endsWith("/text1") || rid.endsWith("/title"));
                    if (!looksLikeItem) continue;
                    if (shown++ > 20) { System.out.println(">>> PROBE   …more"); break; }
                    System.out.println(">>> PROBE   ITEM? <" + cls + "> id=" + rid
                            + " text=\"" + t.trim() + "\" bounds=" + e.getAttribute("bounds"));
                } catch (Exception ignored) { }
            }
            if (shown == 0) System.out.println(">>> PROBE   no id-less list items on screen");
        shoot(label);
        } catch (Exception e) {
            System.out.println(">>> PROBE   dump threw " + e.getClass().getSimpleName());
        }
    }

    private void reachModule() {
        if (landing == Landing.PROJECT_LIST) {
            ProjectListPage projects = new ProjectListPage(driver);
            List<String> names = projects.visibleProjectNames();
            assumeTrue(!names.isEmpty(), "No projects to enter");
            assumeTrue(projects.tapProject(names.get(0)), "Could not enter a project");
        }
        assumeTrue(new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(60)),
                "The bottom navigation never loaded");
        ToolsPage tools = new ToolsPage(driver);
        assumeTrue(tools.openToolsTab(), "The Tools tab could not be opened");
        assumeTrue(tools.isLoaded(Duration.ofSeconds(30)), "The Tools tab did not load");
        tools.searchTools("External");
        sleepQuiet(1500);
        assumeTrue(tools.openIfVisible("External Users"),
                "\"External Users\" is not offered. Tools: " + tools.toolNames());
    }

    private org.openqa.selenium.By idSel(String id) {
        return AppiumBy.androidUIAutomator(
                "new UiSelector().resourceIdMatches(\".*:id/" + id + "\")");
    }

    private void sleepQuiet(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
