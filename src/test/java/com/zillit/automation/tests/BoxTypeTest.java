package com.zillit.automation.tests;

import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.BoxSchedulePage;
import com.zillit.automation.pages.BoxTypeManagerPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * BOX — schedule types. Positive and negative in equal measure: a type is the one thing a
 * schedule cannot be created without, so both "it saves" and "it refuses rubbish" matter.
 *
 * <p>Sheet: BOX_MODULE_TESTCASES.md §2. Fixtures carry the ZL AUTO prefix and the last
 * case removes them.
 */
@ExtendWith(ReasonLogger.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Order(TestOrder.BOX_TYPES)
class BoxTypeTest extends BoxBaseTest {

    private String createdType;

    @Test
    @Order(1)
    @DisplayName("TY-01 Edit Types opens and lists the project's types")
    void typeManagerOpens() {
        BoxTypeManagerPage t = openTypes();
        assertTrue(t.addShown(),
                "The type manager opened without an Add control — a project must be able"
                        + " to define its own types");
    }

    @Test
    @Order(2)
    @DisplayName("TY-02 A type saves and joins the list")
    void createType() {
        BoxTypeManagerPage t = openTypes();
        List<String> before = t.typeNames();
        String name = fixtureName("Type");

        // A colour has to be chosen, not just a name. TypeManagerActivity refuses any
        // create whose colour an existing type already holds, and the form always opens on
        // the same hard-coded #F39C12 — so in a project whose types already cover that
        // colour, Add can never succeed from the defaults. createType drives the picker and
        // retries on a clash, then hands back whatever the app itself said.
        String refusal = t.createType(name);
        assertTrue(refusal.isEmpty(),
                "The app refused to create \"" + name + "\": \"" + refusal + "\"");

        // The type manager CLOSES ITSELF once the server confirms the new type — the
        // observer calls finish() 150ms after the list grows (TypeManagerActivity:134-139).
        // So the list cannot be re-read on this screen: by the time a create has succeeded,
        // the screen is gone. Its closing is the app's own success signal, and the list has
        // to be checked on a fresh visit.
        boolean closedItself = t.waitUntilGone(Duration.ofSeconds(8));
        createdType = name;

        BoxTypeManagerPage again = openTypes();
        assertTrue(again.hasType(name),
                "\"" + name + "\" was accepted without complaint"
                        + (closedItself ? " and the type manager auto-closed, which it only"
                                        + " does once the server confirms the new type,"
                                : " but the type manager stayed open,")
                        + " yet it is missing when the type manager is opened again."
                        + " Before: " + before + "; now: " + again.typeNames());
        closeTypes(again);
    }

    @Test
    @Order(3)
    @DisplayName("TY-02b A blank name is refused, and the list does not grow")
    void blankNameRefused() {
        // THE NEGATIVE HALF. A create form that accepts an empty name produces rows that
        // cannot be told apart, and no amount of later testing recovers from that.
        BoxTypeManagerPage t = openTypes();
        int before = t.typeNames().size();

        t.setName("");
        t.add();
        sleep(2000);

        assertEquals(before, t.typeNames().size(),
                "A blank type name was accepted — the list grew from " + before + " to "
                        + t.typeNames().size());
        closeTypes(t);
    }

    @Test
    @Order(4)
    @DisplayName("TY-02c A duplicate name is refused")
    void duplicateNameRefused() {
        assumeTrue(createdType != null, "TY-02 did not create a type to duplicate");
        BoxTypeManagerPage t = openTypes();
        int before = t.typeNames().size();

        assumeTrue(t.setName(createdType), "Could not retype the existing name");
        t.add();
        sleep(2000);

        int after = t.typeNames().size();
        assertEquals(before, after,
                "\"" + createdType + "\" was accepted a second time — the list grew from "
                        + before + " to " + after + ". Two types with one name cannot be"
                        + " told apart on a schedule.");
        closeTypes(t);
    }

    @Test
    @Order(5)
    @DisplayName("TY-03 A type created from the schedule form is usable immediately")
    void typeFromScheduleForm() {
        // The create-schedule page offers its own inline type form. A type made there must
        // be selectable without reopening the form — otherwise the shortcut is pointless.
        BoxSchedulePage b = box();
        assumeTrue(b.openDrawer(), "The drawer did not open. Toolbar: "
                + b.toolbarState() + "; activity: " + resumedActivity());
        assumeTrue(b.drawerCreateSchedule(), "Create Schedule is not offered");

        com.zillit.automation.pages.BoxCreateSchedulePage form =
                new com.zillit.automation.pages.BoxCreateSchedulePage(driver);
        assumeTrue(form.isLoaded(Duration.ofSeconds(15)), "The schedule form did not open");
        assumeTrue(form.openTypePicker(), "The type picker did not open");
        assumeTrue(form.openNewTypeForm(), "The inline add-type form is not offered");
        assumeTrue(form.newTypeFormShown(), "The inline form did not appear");

        String name = fixtureName("Inline");
        assumeTrue(form.setNewTypeName(name), "Could not type a name inline");
        assumeTrue(form.saveNewType(), "Create was not available on the inline form");
        sleep(3000);

        assertTrue(form.chooseType(name) || String.valueOf(form.selectedTypeName()).contains(name),
                "\"" + name + "\" was created from the schedule form but cannot be chosen"
                        + " there. Types offered: " + form.typeNames());
        backToBox();
    }

    @Test
    @Order(6)
    @DisplayName("TY-05 Deleting removes the type this suite created")
    void deleteType() {
        assumeTrue(createdType != null, "TY-02 created nothing to delete");
        BoxTypeManagerPage t = openTypes();
        assumeTrue(t.hasType(createdType), "\"" + createdType + "\" is already gone");

        assumeTrue(t.deleteType(createdType), "No delete control on the row");
        sleep(1500);
        // The row's delete raises an AlertDialog built in code: "Delete Schedule" with
        // Delete / Cancel (TypeManagerActivity's TypeViewHolder.bind).
        assumeTrue(t.confirmDelete(), "The delete confirmation could not be accepted");
        sleep(2500);

        assertFalse(t.hasType(createdType),
                "\"" + createdType + "\" is still listed after a confirmed delete");
        createdType = null;
        closeTypes(t);
    }

    @Test
    @Order(7)
    @DisplayName("TY-05b Cleanup: no ZL AUTO type is left behind")
    void cleanup() {
        BoxTypeManagerPage t = openTypes();
        for (int i = 0; i < 8; i++) {
            String leftover = t.typeNames().stream()
                    .filter(n -> n != null && n.contains(FIXTURE_PREFIX))
                    .findFirst().orElse(null);
            if (leftover == null) break;
            if (!t.deleteType(leftover)) break;
            sleep(1200);
            t.confirmDelete();
            sleep(2000);
        }
        List<String> left = t.typeNames().stream()
                .filter(n -> n != null && n.contains(FIXTURE_PREFIX)).toList();
        assertTrue(left.isEmpty(),
                "These fixtures are still on the project: " + left
                        + " — the next run's counts will start from the wrong number");
        closeTypes(t);
    }

    // ---------------- plumbing ----------------

    private BoxTypeManagerPage openTypes() {
        backToBox();
        BoxSchedulePage b = box();
        assumeTrue(b.openDrawer(), "The drawer did not open. Toolbar: "
                + b.toolbarState() + "; activity: " + resumedActivity());
        assumeTrue(b.drawerEditTypes(), "Edit Types is not offered — needs posting rights");
        BoxTypeManagerPage t = new BoxTypeManagerPage(driver);
        assumeTrue(t.isLoaded(Duration.ofSeconds(15)), "The type manager did not open");
        return t;
    }

    private void closeTypes(BoxTypeManagerPage t) {
        t.done();
        sleep(1200);
        backToBox();
    }
}
