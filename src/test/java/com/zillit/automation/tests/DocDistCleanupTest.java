package com.zillit.automation.tests;

import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.ConfirmPopupPage;
import com.zillit.automation.pages.DocDistLibraryPage;
import com.zillit.automation.pages.DocDistPresetsPage;
import com.zillit.automation.pages.DocDistTemplatesPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * HOUSEKEEPING — removes the {@code QA_*} entries this suite family leaves behind.
 *
 * <p>Every Doc-Distribution suite deletes what it creates, but only when it reaches its
 * cleanup case: a case that fails earlier leaves its folder, list or template in the
 * project. Those strays are harmless to the app and invisible to most cases — but they
 * change what the EMPTY-STATE cases see, and that is a real problem. H-2 asserts the
 * template manager shows rows XOR the empty state; it passed early in a session and
 * failed later purely because previous runs had left templates behind.
 *
 * <p>The temptation there is to weaken H-2 until it tolerates any state. That would hide
 * a genuine XOR violation for ever. Cleaning the data is the honest fix, and this is it.
 *
 * <p><b>Deliberately NOT part of {@code --doc-dist-all}.</b> It deletes data, so it runs
 * only when asked for by name ({@code --doc-dist-cleanup}). It touches ONLY names starting
 * with {@code QA_} — anything a human created is left alone.
 */
@Order(TestOrder.DOC_DISTRIBUTION_CLEANUP)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DocDistCleanupTest extends DocDistBaseTest {

    /** The one prefix this suite family ever creates. Nothing else is touched. */
    private static final String PREFIX = "QA_";

    @BeforeEach
    void reachLibrary() {
        reachLibraryOrSkip();
    }

    @Test
    @Order(1)
    @DisplayName("CLEANUP: remove stray QA_* email templates")
    void cleanTemplates() {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        assumeTrue(lib.openTemplates(), "Templates shortcut not reachable");
        DocDistTemplatesPage templates = new DocDistTemplatesPage(driver);
        assumeTrue(templates.isManager(Duration.ofSeconds(20)), "Template manager did not open");

        int removed = 0;
        for (int pass = 0; pass < 20; pass++) {
            String victim = firstQaName(templates.allTemplateNamesScrolled());
            if (victim == null) break;
            if (!templates.tapDelete(victim)) break;
            if (!acceptFrameworkConfirm()) break;
            sleep(1500);
            removed++;
            System.out.println(">>> deleted template '" + victim + "'");
        }
        System.out.println(">>> templates removed=" + removed
                + " remaining=" + templates.templateNames());
        backOut();
    }

    @Test
    @Order(2)
    @DisplayName("CLEANUP: remove stray QA_* distribution lists")
    void cleanPresets() {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        assumeTrue(lib.openLists(), "Lists shortcut not reachable");
        DocDistPresetsPage presets = new DocDistPresetsPage(driver);
        assumeTrue(presets.isOverview(Duration.ofSeconds(20)), "Preset overview did not open");

        int removed = 0;
        for (int pass = 0; pass < 20; pass++) {
            String victim = firstQaName(presets.allListNamesScrolled());
            if (victim == null) break;
            if (!presets.tapDelete(victim)) break;
            if (!acceptFrameworkConfirm()) break;
            sleep(1500);
            removed++;
            System.out.println(">>> deleted list '" + victim + "'");
        }
        System.out.println(">>> lists removed=" + removed
                + " remaining=" + presets.listNames());
        backOut();
    }

    @Test
    @Order(3)
    @DisplayName("CLEANUP: remove stray QA_* library folders")
    void cleanFolders() {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        assumeTrue(lib.returnToRoot(), "Not at the library root");

        int removed = 0;
        for (int pass = 0; pass < 20; pass++) {
            String victim = firstQaName(lib.allItemNamesScrolled());
            if (victim == null) break;
            if (!deleteItem(victim)) break;
            sleep(1500);
            removed++;
            System.out.println(">>> deleted folder '" + victim + "'");
        }
        System.out.println(">>> folders removed=" + removed
                + " remaining=" + lib.itemNames());
    }

    // ---------------- helpers ----------------

    private String firstQaName(List<String> names) {
        return names.stream()
                .filter(n -> n != null && n.startsWith(PREFIX))
                .findFirst().orElse(null);
    }

    /**
     * Accepts the FRAMEWORK delete confirmation only.
     *
     * <p>Never blind-accepts: without posting rights the app raises its CUSTOM dialog
     * instead, whose right button posts a permission request into a real admin's chat
     * (trap 3.25). That one is declined and the loop stops.
     */
    private boolean acceptFrameworkConfirm() {
        ConfirmPopupPage confirm = new ConfirmPopupPage(driver);
        if (!confirm.isShown(Duration.ofSeconds(8))) return false;
        if (!confirm.isFrameworkDialog()) {
            System.out.println(">>> NOT a delete confirm — declining: '" + confirm.text() + "'");
            confirm.cancel(Duration.ofSeconds(3));
            return false;
        }
        return confirm.accept(Duration.ofSeconds(5));
    }

    private void backOut() {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        for (int i = 0; i < 6; i++) {
            if (!lib.isDocDistActivity()) break;
            if (lib.isLibrary(Duration.ofSeconds(2))) break;
            try { driver.navigate().back(); Thread.sleep(900); } catch (Exception e) { break; }
        }
        if (lib.isDocDistActivity()) lib.returnToRoot();
    }
}
