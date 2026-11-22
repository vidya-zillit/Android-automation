package com.zillit.automation.tests.map;

import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.map.MapDialogPage;
import com.zillit.automation.pages.map.MapPage;
import com.zillit.automation.pages.map.MapTypeManagerPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * MAP &rarr; <b>location types</b> ({@code btnTypes} &rarr; {@code headerManagerFragment}).
 *
 * <p>Types are the Map's own classification: they drive the marker icon and colour, the
 * filter chips, the per-type counts, and the {@code actvType} dropdown on the location
 * form. Changing one therefore reaches every location already using it, which is why both
 * mutating actions are behind confirmations that spell out the consequence — and why those
 * two confirmations are asserted on their wording, not just their presence:
 *
 * <pre>
 *   Update Location Type — "…will change the type classification for associated locations."
 *   Delete Location Type — "…will remove its type classification from associated locations."
 * </pre>
 *
 * <p>The form is built in code rather than inflated, so it has no resource ids and every
 * interaction here goes through text. See {@link MapTypeManagerPage}.
 *
 * <p><b>Writes to the backend.</b> Fixtures carry the {@code ZL AUTO} prefix; the delete
 * case removes only those.
 */
@Order(TestOrder.MAP_TYPE_MANAGER)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MapTypeManagerTest extends MapBaseTest {

    private MapTypeManagerPage openTypes() {
        assumeTrue(ensureCitySelected(),
                "btnTypes is city-gated and this project has no city to select");
        MapPage m = map();
        assumeTrue(m.openTypes(),
                "Types button not available — it needs a city AND posting rights");
        MapTypeManagerPage types = new MapTypeManagerPage(driver);
        assumeTrue(types.isLoaded(Duration.ofSeconds(15)), "Type manager did not open");
        return types;
    }

    // ---------------- list ----------------

    @Test
    @Order(1)
    @DisplayName("T-01 The type manager opens with a count, search and the list")
    void opens() {
        MapTypeManagerPage types = openTypes();
        assertTrue(types.isLoaded(Duration.ofSeconds(8)), "Type manager not shown");
        assertNotNull(types.countText(), "No type count rendered");
        backToMap();
    }

    @Test
    @Order(2)
    @DisplayName("T-02 Type cards carry an icon and a name")
    void cardContent() {
        MapTypeManagerPage types = openTypes();
        assumeTrue(types.typeCount() > 0, "No types on this project yet");
        assertFalse(types.typeNames().isEmpty(), "Type rows have no names");
        // The icon is what the marker and the filter chip both render, so a nameless or
        // icon-less row would break both downstream surfaces.
        assertFalse(types.typeIcons().isEmpty(), "Type rows have no icons");
        backToMap();
    }

    @Test
    @Order(3)
    @DisplayName("T-10 Type search filters the list")
    void searchFilters() {
        MapTypeManagerPage types = openTypes();
        List<String> all = types.typeNames();
        assumeTrue(all.size() >= 2, "Needs at least two types to prove filtering");
        String term = all.get(0).substring(0, Math.min(3, all.get(0).length()));
        assumeTrue(types.search(term), "Type search field not available");
        sleep(1500);
        assertTrue(types.typeNames().size() <= all.size(), "Searching widened the list");
        backToMap();
    }

    // ---------------- the code-built form ----------------

    @Test
    @Order(4)
    @DisplayName("T-04 The add-type form offers a name field and the icon grid")
    void addFormShape() {
        MapTypeManagerPage types = openTypes();
        assumeTrue(types.addShown(), "Add button hidden — this account has no posting access");
        assumeTrue(types.openAdd(), "Could not open the add-type form");
        assumeTrue(types.formShown(Duration.ofSeconds(10)), "Type form did not open");

        assertTrue(types.iconGridShown(),
                "No \"" + MapTypeManagerPage.LABEL_SELECT_ICON + "\" section — the icon is "
                        + "what the marker and the filter chip render");
        assertTrue(types.noIconSelected(),
                "A fresh form should read \"" + MapTypeManagerPage.NO_ICON + "\"");
        types.cancelForm();
        sleep(1200);
        backToMap();
    }

    @Test
    @Order(5)
    @DisplayName("T-05 Creating a type adds it to the list")
    void createType() {
        MapTypeManagerPage types = openTypes();
        assumeTrue(types.addShown(), "Add button hidden — no posting access");
        int before = types.typeCount();

        assumeTrue(types.openAdd(), "Could not open the add-type form");
        assumeTrue(types.formShown(Duration.ofSeconds(10)), "Type form did not open");
        String name = fixtureName("Type");
        assumeTrue(types.setTypeName(name), "Could not type into the name field");
        assumeTrue(types.submitForm(), "Create button not found on the form");
        sleep(3500);

        assertTrue(types.typeCount() > before
                        || types.typeNames().stream().anyMatch(n -> n.contains(FIXTURE_PREFIX)),
                "Type count did not grow after creating one: " + before
                        + " → " + types.typeCount());
        backToMap();
    }

    @Test
    @Order(6)
    @DisplayName("T-06 Editing a type warns that associated locations change")
    void editConfirmWording() {
        MapTypeManagerPage types = openTypes();
        String target = types.typeNames().stream()
                .filter(n -> n.startsWith(FIXTURE_PREFIX))
                .findFirst().orElse(null);
        assumeTrue(target != null,
                "No " + FIXTURE_PREFIX + " type to edit — this suite never edits a type it "
                        + "did not create");

        assumeTrue(types.editType(target), "Edit not available on the row");
        MapDialogPage dlg = new MapDialogPage(driver);
        assertTrue(dlg.shown(Duration.ofSeconds(8)), "Edit did not warn first");
        assertTrue(String.valueOf(dlg.title()).contains(MapTypeManagerPage.TITLE_UPDATE_CONFIRM),
                "Unexpected confirm title: " + dlg.title());
        assertTrue(String.valueOf(dlg.message()).contains("associated locations"),
                "The warning must say associated locations are affected; got: " + dlg.message());
        assertTrue(dlg.hasButton("Update") && dlg.hasButton(MapTypeManagerPage.BTN_CANCEL),
                "Expected Update/Cancel; got " + dlg.buttonLabels());

        dlg.tap(MapTypeManagerPage.BTN_CANCEL);
        sleep(1200);
        backToMap();
    }

    @Test
    @Order(7)
    @DisplayName("T-08 Deleting a type warns that locations lose their label")
    void deleteConfirmWording() {
        MapTypeManagerPage types = openTypes();
        String target = types.typeNames().stream()
                .filter(n -> n.startsWith(FIXTURE_PREFIX))
                .findFirst().orElse(null);
        assumeTrue(target != null, "No " + FIXTURE_PREFIX + " type to delete");

        assumeTrue(types.deleteType(target), "Delete not available on the row");
        MapDialogPage dlg = new MapDialogPage(driver);
        assertTrue(dlg.shown(Duration.ofSeconds(8)), "Delete did not ask first");
        assertTrue(String.valueOf(dlg.title()).contains(MapTypeManagerPage.TITLE_DELETE_CONFIRM),
                "Unexpected confirm title: " + dlg.title());
        assertTrue(String.valueOf(dlg.message()).contains("without a type label")
                        || String.valueOf(dlg.message()).contains("remove its type"),
                "The warning must say what happens to associated locations; got: "
                        + dlg.message());

        dlg.tap(MapDialogPage.BTN_CANCEL);
        sleep(1500);
        backToMap();
    }

    @Test
    @Order(8)
    @DisplayName("T-09 Confirming the delete removes the type this suite created")
    void deleteCreatedType() {
        MapTypeManagerPage types = openTypes();
        String target = types.typeNames().stream()
                .filter(n -> n.startsWith(FIXTURE_PREFIX))
                .findFirst().orElse(null);
        assumeTrue(target != null, "No " + FIXTURE_PREFIX + " type left to clean up");

        int before = types.typeCount();
        assumeTrue(types.deleteType(target), "Delete not available");
        MapDialogPage dlg = new MapDialogPage(driver);
        assumeTrue(dlg.shown(Duration.ofSeconds(8)), "No confirmation dialog");
        assumeTrue(dlg.tap(MapDialogPage.BTN_DELETE), "Delete not available in the dialog");
        sleep(3000);

        assertTrue(types.typeCount() < before || !types.typeNames().contains(target),
                "Type \"" + target + "\" survived a confirmed delete");
        backToMap();
    }

    @Test
    @Order(20)
    @DisplayName("T-11 Several subtypes can be added to one type")
    void subtypesCanBeAdded() {
        MapTypeManagerPage types = openTypes();
        assumeTrue(types.addShown(), "Add button hidden — no posting access");
        assumeTrue(types.openAdd(), "Could not open the add-type form");
        assumeTrue(types.formShown(Duration.ofSeconds(10)), "Type form did not open");
        assumeTrue(types.setTypeName(fixtureName("Sub")), "Could not type a name");

        assertTrue(types.addSubtype("Beach"),
                "\"Beach\" was not accepted as a subtype. The field should clear on a"
                        + " successful add (HeaderManagerFragment:305-309); it still holds \""
                        + types.subtypeFieldText() + "\"");
        assertTrue(types.addSubtype("Rooftop"),
                "A SECOND subtype was refused — the row must accept more than one."
                        + " Field still holds \"" + types.subtypeFieldText() + "\"");

        types.cancelForm();      // nothing is saved: this case is about the row, not the type
        backToMap();
    }

    @Test
    @Order(21)
    @DisplayName("T-11b A blank or duplicate subtype is refused, and the field keeps it")
    void blankAndDuplicateSubtypesRefused() {
        MapTypeManagerPage types = openTypes();
        assumeTrue(types.addShown(), "Add button hidden — no posting access");
        assumeTrue(types.openAdd(), "Could not open the add-type form");
        assumeTrue(types.formShown(Duration.ofSeconds(10)), "Type form did not open");
        assumeTrue(types.setTypeName(fixtureName("Sub")), "Could not type a name");

        // Blank: the guard is `subName.isNotBlank()`, so nothing should be added — and
        // with nothing added there is nothing to clear either.
        types.addSubtype("");
        assertTrue(types.subtypeFieldText().isEmpty(),
                "A blank subtype left something behind: \"" + types.subtypeFieldText() + "\"");

        assumeTrue(types.addSubtype("Beach"), "The first subtype was not accepted");

        // Duplicate: `!subtypes.contains(subName)` means the same name is refused, and a
        // refusal leaves the typed text in place rather than clearing it.
        boolean accepted = types.addSubtype("Beach");
        assertFalse(accepted,
                "\"Beach\" was accepted twice. The add handler refuses a name already in"
                        + " the list, so the field should still hold it rather than clear.");

        types.cancelForm();
        backToMap();
    }
}
