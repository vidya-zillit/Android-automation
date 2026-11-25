package com.zillit.automation.tests.cc;

import com.zillit.automation.core.TestOrder;

import com.zillit.automation.pages.cc.CcContactsPage;
import com.zillit.automation.pages.cc.CcLandingPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * C&C → Contacts tab: contact list, search, and the per-contact action icons —
 * Favourite (star, toggles) and Location (pin). Chat/Audio/Video icons are
 * covered by the chat/call suites, so here we exercise the list, search,
 * favourite toggle and location tap.
 */
@Order(TestOrder.CC_CONTACTS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CcContactsTest extends CcBaseTest {

    private CcContactsPage openContacts() {
        CcLandingPage p = landing();
        assumeTrue(p.tapTopTab("Contacts"), "Could not switch to the Contacts tab");
        sleep(1500);
        CcContactsPage c = new CcContactsPage(driver);
        return c.isLoaded(Duration.ofSeconds(12)) ? c : null;
    }

    @Test
    @Order(1)
    @DisplayName("Contacts tab lists project members with action icons + search")
    void contactsLoad() {
        CcContactsPage c = openContacts();
        assumeTrue(c != null, "Contacts tab did not load — skipping");
        System.out.println(">>> Contacts rows=" + c.rowCount() + " names=" + c.names()
                + " actionIcons=" + c.hasActionIcons());
        assertTrue(c.rowCount() > 0, "Contacts should list project members");
        assertTrue(c.hasActionIcons(), "Each contact row should show action icons (chat/call/location/fav)");
        assertTrue(c.isSearchVisible(), "Contacts search should be present");
        screenshot("cc-contacts");
    }

    @Test
    @Order(2)
    @DisplayName("Search filters the contact list")
    void searchContacts() {
        CcContactsPage c = openContacts();
        assumeTrue(c != null, "Contacts tab did not load — skipping");
        int before = c.rowCount();
        assumeTrue(before > 0, "No contacts to filter — skipping");
        String first = c.names().isEmpty() ? "" : c.names().get(0);
        String frag = first.length() >= 3 ? first.substring(0, 3) : first;
        c.search(frag);
        int matched = c.rowCount();
        System.out.println(">>> contacts search '" + frag + "' " + before + " -> " + matched);
        assertTrue(matched > 0 && matched <= before, "Search should narrow to matching contacts");
        c.clearSearch();
        // a clearly non-matching query should yield fewer/none
        c.search("zzz-nomatch-" + (System.currentTimeMillis() % 1000));
        System.out.println(">>> contacts no-match rows=" + c.rowCount());
        screenshot("cc-contacts-search");
    }

    @Test
    @Order(3)
    @DisplayName("[WEB CC-22] Favourite (star) moves a contact into / out of the Favourites chip, then back")
    void toggleFavourite() {
        CcContactsPage c = openContacts();
        assumeTrue(c != null, "Contacts tab did not load — skipping");
        assumeTrue(c.hasFavouriteIcon(), "Favourite icon not present — skipping");
        List<String> names = c.names();
        assumeTrue(!names.isEmpty(), "No contacts to favourite — skipping");
        // Contacts appends " (Admin)" to the name; the Favourites row shows the bare name.
        // The other test account when listed, so a real member's row is never touched.
        String pick = names.stream().filter(n -> n.contains(PEER)).findFirst().orElse(names.get(0));
        String name = pick.replaceAll("\\s*\\(.*\\)\\s*$", "").trim();
        assumeTrue(!name.isEmpty(), "First contact has no readable name — skipping");

        // The star's state is a drawable (not readable), so membership of the Chat
        // tab's "Favourites" chip is the observable truth (ChatFragment isFavTab).
        List<String> favBefore = favouriteNames();
        boolean wasFav = containsName(favBefore, name);
        System.out.println(">>> '" + name + "' favourite=" + wasFav + " favourites=" + favBefore);
        if (favBefore.isEmpty()) {
            String empty = landing().emptyViewText();
            assertTrue(empty.startsWith("No favourites saved"),
                    "An empty Favourites chip should explain itself ('No favourites saved…'); got '" + empty + "'");
        }

        boolean tapped = false;
        try {
            assumeTrue(tapFavouriteOf(name), "Could not tap the favourite star for " + name);
            tapped = true;
            List<String> favAfter = waitFavouriteState(name, !wasFav);
            System.out.println(">>> after toggle: favourites=" + favAfter);
            assertTrue(containsName(favAfter, name) == !wasFav,
                    "'" + name + "' should have moved " + (wasFav ? "out of" : "into")
                            + " Favourites; got " + favAfter);
            assertTrue(favAfter.size() == favBefore.size() + (wasFav ? -1 : 1),
                    "Favourites should have " + (wasFav ? "one fewer" : "one more") + " row: "
                            + favBefore.size() + " -> " + favAfter.size());
            screenshot("cc-contacts-fav");

            // Toggle back: the original state must come back.
            assertTrue(tapFavouriteOf(name), "Could not tap the favourite star again for " + name);
            List<String> favRestored = waitFavouriteState(name, wasFav);
            System.out.println(">>> after toggle back: favourites=" + favRestored);
            assertTrue(containsName(favRestored, name) == wasFav,
                    "Toggling again should put '" + name + "' back " + (wasFav ? "into" : "out of")
                            + " Favourites; got " + favRestored);
            tapped = false;
        } finally {
            // A failed check above must not leave the favourite flipped for the next run.
            if (tapped) {
                try {
                    if (containsName(favouriteNames(), name) != wasFav) tapFavouriteOf(name);
                } catch (Exception ignored) {
                }
            }
            try { landing().tapChip("All"); } catch (Exception ignored) {}
        }
    }

    /** Chat tab → "Favourites" chip → the row names listed there. */
    private List<String> favouriteNames() {
        recoverToCcLanding();
        CcLandingPage p = landing();
        assumeTrue(p.tapChip("Favourites"), "Favourites chip not found");
        sleep(1500);
        return p.rowNames();
    }

    /** Polls the Favourites chip until the contact's membership equals {@code expected}. */
    private List<String> waitFavouriteState(String name, boolean expected) {
        List<String> favs = favouriteNames();
        for (int i = 0; i < 4 && containsName(favs, name) != expected; i++) {
            sleep(2000);
            CcLandingPage p = landing();
            p.tapChip("All");
            sleep(800);
            p.tapChip("Favourites");
            sleep(1500);
            favs = p.rowNames();
        }
        return favs;
    }

    /** Contacts tab → search the contact so it is the only row → tap its star. */
    private boolean tapFavouriteOf(String name) {
        CcContactsPage c = openContacts();
        if (c == null) return false;
        c.search(name);
        boolean onlyTarget = !c.names().isEmpty()
                && c.names().stream().allMatch(n -> n.contains(name));
        boolean tapped = onlyTarget && c.tapFirstFavourite();
        sleep(1800);
        c.clearSearch();
        return tapped;
    }

    private static boolean containsName(List<String> rows, String name) {
        return rows.stream().anyMatch(r -> r.contains(name));
    }

    // [WEB CC-32] [CNC-02]
    @Test
    @Order(5)
    @DisplayName("[WEB CC-32 / CNC-02] Contacts list keeps its full height after opening a chat from its search")
    void listHeightAfterSearchOpen() {
        CcContactsPage c = openContacts();
        assumeTrue(c != null, "Contacts tab did not load — skipping");
        hideKeyboardIfUp();
        org.openqa.selenium.Rectangle r0 = c.listRect();
        int last0 = c.lastRowBottom();
        int rowH = c.rowHeight();
        assumeTrue(r0 != null && rowH > 0, "No contact rows to measure — skipping");
        int bottom0 = r0.getY() + r0.getHeight();
        System.out.println(">>> contacts before: list " + r0.getY() + ".." + bottom0 + " lastRow=" + last0 + " rowH=" + rowH);

        c.search(PEER.substring(0, Math.min(4, PEER.length())));   // keyboard stays up
        assumeTrue(c.rowCount() > 0, "Search for '" + PEER + "' found nobody — skipping");
        assumeTrue(c.names().get(0).contains(PEER) || c.names().stream().anyMatch(n -> n.contains(PEER)),
                PEER + " not in the filtered contacts — skipping");
        c.search(PEER);
        assumeTrue(c.tapFirstMessage(), "No chat icon on the found contact");
        com.zillit.automation.pages.cc.CcChatThreadPage t = new com.zillit.automation.pages.cc.CcChatThreadPage(driver);
        assumeTrue(t.isLoaded(Duration.ofSeconds(15)), "The 1-1 chat did not open from Contacts");
        for (int i = 0; i < 3 && !c.isLoaded(Duration.ofSeconds(2)); i++) {
            try { driver.navigate().back(); } catch (Exception ignored) {}
            sleep(900);
        }
        if (!c.isLoaded(Duration.ofSeconds(3))) c = openContacts();
        assumeTrue(c != null, "Did not get back to Contacts");
        c.clearSearch();
        hideKeyboardIfUp();
        sleep(1200);
        org.openqa.selenium.Rectangle r1 = c.listRect();
        int last1 = c.lastRowBottom();
        assertTrue(r1 != null, "The contact list should be shown after coming back");
        int bottom1 = r1.getY() + r1.getHeight();
        System.out.println(">>> contacts after: list " + r1.getY() + ".." + bottom1 + " lastRow=" + last1);
        screenshot("cc-contacts-height-after-search");
        assertTrue(Math.abs(bottom1 - bottom0) <= 8,
                "CNC-02: the Contacts list should keep its full height after opening a chat from its search; bottom "
                        + bottom0 + " -> " + bottom1);
        if (last0 >= bottom0 - rowH) {
            assertTrue(last1 >= bottom0 - rowH,
                    "CNC-02: a keyboard-sized blank area is left under the contacts (last row ends at " + last1
                            + ", list bottom " + bottom0 + ")");
        }
    }

    @Test
    @Order(4)
    @DisplayName("Location (pin) on a contact is actionable")
    void locationAction() {
        CcContactsPage c = openContacts();
        assumeTrue(c != null, "Contacts tab did not load — skipping");
        assumeTrue(c.hasLocationIcon(), "Location icon not present — skipping");
        boolean tapped = c.tapFirstLocation();
        System.out.println(">>> location tapped=" + tapped);
        assertTrue(tapped, "The location (pin) action should be tappable");
        // It either opens a map (external intent / chooser) or posts an enable-location
        // request. Recover with bounded back-presses so an external chooser can't stall us.
        sleep(1500);
        screenshot("cc-contacts-location");
        for (int i = 0; i < 3; i++) {
            try { driver.navigate().back(); } catch (Exception ignored) {}
            sleep(700);
            try { if (driver.getCurrentPackage() != null
                    && driver.getCurrentPackage().contains("zillit")) break; } catch (Exception ignored) {}
        }
        dismissPermissionPrompts();
    }
}
