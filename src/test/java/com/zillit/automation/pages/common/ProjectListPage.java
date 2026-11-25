package com.zillit.automation.pages.common;

import com.zillit.automation.core.Config;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/** Project chooser shown to logged-in users (ProjectListActivity). */
public class ProjectListPage extends BasePage {

    public ProjectListPage(AndroidDriver driver) {
        super(driver);
    }

    public boolean isLoaded() {
        return waitVisible("projectListRecyclerView").isDisplayed();
    }

    /**
     * The project names on screen, WAITING for the list to populate first.
     *
     * <p><b>Why the wait is the whole point.</b> settleLanding() reports PROJECT_LIST as
     * soon as {@code projectListRecyclerView} exists — but the RecyclerView is in the
     * hierarchy before the API has returned a single row. An instant read therefore comes
     * back empty on a cold start, and all 58 callers guard with
     * {@code assumeTrue(!names.isEmpty(), "No projects to enter")}. The suite then skips
     * every case while the phone sits on a project list that fills in a second later —
     * reporting "no projects" about an account with plenty of them.
     *
     * <p>Returning empty after the wait is still a real answer (an account genuinely
     * without projects), so the behaviour callers rely on does not change — only the
     * timing does.
     */
    public List<String> visibleProjectNames() {
        List<String> names = readProjectNames();
        String run = runProject();
        if (run.isEmpty()) return names;
        List<String> mine = only(names, run);
        if (mine.isEmpty()) {
            try {
                search(run);
            } catch (WebDriverException ignored) {
                // No search field on this screen — the empty answer below is the safe one.
            }
            mine = only(readProjectNames(), run);
        }
        if (mine.isEmpty()) System.out.println(">>> run project \"" + run + "\" is not in the project list");
        return mine;
    }

    /**
     * The one project this run may use (zillit.runProject / ZILLIT_RUN_PROJECT), or "". The
     * full sweep sets it to the project it has just created. While it is set, the list offers
     * that project only, so a suite can never enter — and write into — a different one.
     */
    public static String runProject() {
        return Config.get("zillit.runProject");
    }

    /** True when a row or a toolbar title names this project (the row's " - Admin" is ignored). */
    public static boolean sameProject(String shown, String project) {
        String t = shown.trim();
        return t.equalsIgnoreCase(project.trim())
                || t.replaceFirst("\\s+-\\s+\\S+$", "").trim().equalsIgnoreCase(project.trim());
    }

    private static List<String> only(List<String> names, String project) {
        return names.stream().filter(n -> sameProject(n, project)).limit(1).collect(Collectors.toList());
    }

    private List<String> readProjectNames() {
        long deadline = System.currentTimeMillis() + PROJECTS_APPEAR.toMillis();
        while (true) {
            try {
                List<String> names = driver.findElements(byId("projectNameText")).stream()
                        .map(WebElement::getText)
                        .filter(t -> t != null && !t.isBlank())
                        .collect(Collectors.toList());
                if (!names.isEmpty()) {
                    return names;
                }
            } catch (WebDriverException ignored) {
                // List re-binding mid-read — try again until the budget runs out.
            }
            if (System.currentTimeMillis() >= deadline) {
                return List.of();
            }
            try {
                Thread.sleep(400);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return List.of();
            }
        }
    }

    /** How long the project list gets to come back from the API before we call it empty. */
    private static final Duration PROJECTS_APPEAR = Duration.ofSeconds(25);

    public void search(String text) {
        WebElement field = waitVisible("search_widget_edit_textvw");
        // Tap the field before typing. Without focus the keystrokes can land
        // elsewhere and the list is never filtered, so the caller then picks from
        // the FULL project list believing it picked from search results.
        try {
            field.click();
        } catch (WebDriverException ignored) {
            // Already focused.
        }
        field.clear();
        if (!text.isEmpty()) {
            field.sendKeys(text);
        }
        hideKeyboard();
        pauseForFilter();
    }

    public void clearSearch() {
        search("");
    }

    public boolean isSearchVisible() {
        return isVisible("search_widget_edit_textvw", Duration.ofSeconds(5));
    }

    public boolean isStartProjectVisible() {
        return isVisible("start_project_btn", Duration.ofSeconds(5));
    }

    public boolean isJoinProjectVisible() {
        return isVisible("join_project_btn", Duration.ofSeconds(5));
    }

    public boolean isEmptyStateShown() {
        return isVisible("noDataFound", Duration.ofSeconds(5))
                || visibleProjectNames().isEmpty();
    }

    /** Taps the row of the first project whose name contains the given text. */
    public boolean tapProject(String nameContains) {
        // "contains" could also hit a shooting unit named after the run project.
        if (!runProject().isEmpty() && sameProject(nameContains, runProject())) {
            return tapProjectExact(runProject());
        }
        for (WebElement label : driver.findElements(byId("projectNameText"))) {
            String name = label.getText();
            if (name != null && name.toLowerCase().contains(nameContains.toLowerCase())) {
                try {
                    // Click the whole row — the click listener lives on the
                    // holder, not the text label.
                    driver.findElement(io.appium.java_client.AppiumBy.xpath(
                            "//*[contains(@resource-id,'projectNameText') and contains(@text,\""
                                    + name + "\")]/ancestor::*[contains(@resource-id,'projectHolder')]"))
                            .click();
                } catch (WebDriverException e) {
                    label.click(); // fallback: tap the label itself
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Opens the project whose name matches EXACTLY (ignoring case and padding).
     *
     * <p>Use this whenever the wrong neighbour would be a real problem.
     * {@link #tapProject} matches on "contains" and takes the first hit, which is
     * unsafe here: a remote shooting unit created from a project carries a name
     * containing the parent's, so searching the parent matches both and the unit
     * — listed first — is the one that opens.
     *
     * @return false when no row matches exactly, so the caller can fail loudly
     *         rather than silently landing somewhere else
     */
    public boolean tapProjectExact(String name) {
        String wanted = name.trim().toLowerCase();
        for (WebElement label : driver.findElements(byId("projectNameText"))) {
            String shown = label.getText();
            if (shown == null || !matchesProject(shown, wanted)) {
                continue;
            }
            try {
                driver.findElement(io.appium.java_client.AppiumBy.xpath(
                        "//*[contains(@resource-id,'projectNameText') and @text=\"" + shown
                                + "\"]/ancestor::*[contains(@resource-id,'projectHolder')]"))
                        .click();
            } catch (WebDriverException e) {
                label.click();
            }
            return true;
        }
        return false;
    }

    /**
     * True when a listed row names this project.
     *
     * <p>Rows carry the viewer's ROLE as a suffix — "ZL TESTING 01 AUGUST - Admin"
     * — while the in-project header shows the bare name, so a plain equality test
     * never matches. Exactly one trailing " - token" is stripped before comparing,
     * which keeps the match tight: a shooting unit listed as
     * "Parent - Unit A - Admin" strips to "Parent - Unit A" and correctly fails to
     * match its parent, which is the confusion that opened the wrong project
     * before.
     */
    private boolean matchesProject(String shown, String wantedLower) {
        return sameProject(shown, wantedLower);
    }

    /** Project names currently listed — useful for diagnosing a failed pick. */
    public java.util.List<String> listedProjectNames() {
        java.util.List<String> out = new java.util.ArrayList<>();
        try {
            for (WebElement label : driver.findElements(byId("projectNameText"))) {
                String t = label.getText();
                if (t != null && !t.isBlank()) {
                    out.add(t.trim());
                }
            }
        } catch (WebDriverException ignored) {
        }
        return out;
    }

    private void pauseForFilter() {
        try {
            Thread.sleep(800); // TextWatcher filtering is instant but re-layout isn't
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
