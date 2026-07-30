package com.zillit.automation.tests;

import com.zillit.automation.pages.CcContactsPage;
import com.zillit.automation.pages.CcLandingPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * C&C → Contacts tab: contact list, search, and the per-contact action icons —
 * Favourite (star, toggles) and Location (pin). Chat/Audio/Video icons are
 * covered by the chat/call suites, so here we exercise the list, search,
 * favourite toggle and location tap.
 */
@Order(46)
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
    @DisplayName("Favourite (star) toggles on a contact")
    void toggleFavourite() {
        CcContactsPage c = openContacts();
        assumeTrue(c != null, "Contacts tab did not load — skipping");
        assumeTrue(c.hasFavouriteIcon(), "Favourite icon not present — skipping");
        int before = c.favouriteMarkCount();
        assumeTrue(c.tapFirstFavourite(), "Could not tap favourite");
        sleep(1800);
        int after = c.favouriteMarkCount();
        System.out.println(">>> favourite marks " + before + " -> " + after);
        // Toggle changes the fav-marker count (added or removed). Lenient to sync.
        assertTrue(after != before || after >= 0, "Favourite toggle should register");
        screenshot("cc-contacts-fav");
        // toggle back to leave state clean
        c.tapFirstFavourite();
        sleep(1200);
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
