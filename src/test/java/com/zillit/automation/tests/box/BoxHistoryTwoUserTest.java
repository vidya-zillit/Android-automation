package com.zillit.automation.tests.box;

import com.zillit.automation.core.TestOrder;

import com.zillit.automation.core.DriverFactory;
import com.zillit.automation.pages.box.BoxCreateEventPage;
import com.zillit.automation.pages.box.BoxHistoryPage;
import com.zillit.automation.pages.box.BoxListPage;
import com.zillit.automation.pages.box.BoxSchedulePage;
import io.appium.java_client.android.AndroidDriver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * PRODUCTION DIARY across TWO users — web gap BOX-02: a Personal Note must leave no trace in
 * ANOTHER member's History. {@code HistoryActivity} shows the server's activity log as sent
 * (filtered only by action and date), so if the server returns A's personal-note row to B,
 * Android shows its title to B.
 *
 * <p>A ({@code ZILLIT_UDID}, the emulator, Vidya Testing) writes the note and checks its own
 * History as the control; B ({@code ZILLIT_RECEIVER_UDID}, the Pixel, Vidya Pixel) reads its
 * History. Steps swap {@link #driver}, as {@code CallSheetApprovalTest} does. The note is
 * deleted on A in the {@code finally}.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Order(TestOrder.BOX_HISTORY_TWO_USER)
class BoxHistoryTwoUserTest extends BoxBaseTest {

    private AndroidDriver a, b;

    @BeforeAll
    void secondDevice() {
        String udidB = System.getenv().getOrDefault("ZILLIT_RECEIVER_UDID", "");
        assumeTrue(!udidB.isBlank(), "ZILLIT_RECEIVER_UDID is required — BOX-02 reads a second member's History");
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

    /** Asked of whichever device is current — the base asks adb for ZILLIT_UDID only. */
    @Override
    String resumedActivity() {
        try { return "mResumedActivity " + driver.currentActivity(); }
        catch (Exception e) { return "(unreadable)"; }
    }

    @Test
    @Order(10)
    @DisplayName("HS-08 [WEB BOX-02] A's Personal Note is in A's History and not in B's")
    void personalNoteHiddenFromOthers() {
        String title = fixtureName("personal");
        boolean made = on(a, () -> createPersonalNote(title));
        assumeTrue(made, "A could not save a Personal note");
        try {
            List<BoxHistoryPage.Card> mine = on(a, this::historyCards);
            boolean control = mine.stream().anyMatch(c -> String.valueOf(c.title).contains(title));
            System.out.println(">>> HS-08 A's History (" + mine.size() + " cards) lists the note: " + control);
            assumeTrue(control, "Control failed: A's own History does not list \"" + title
                    + "\", so B's not listing it would prove nothing. A's cards: " + mine);

            List<BoxHistoryPage.Card> theirs = on(b, this::historyCards);
            List<BoxHistoryPage.Card> leaked = theirs.stream()
                    .filter(c -> String.valueOf(c.title).contains(title)).toList();
            System.out.println(">>> HS-08 B's History (" + theirs.size() + " cards); leaked: " + leaked);
            assertTrue(leaked.isEmpty(), "[BOX-02] B's History shows A's Personal Note \"" + title + "\": " + leaked);
        } finally {
            on(a, () -> { deleteNote(title); return null; });
        }
    }

    // ─────────────────────────── steps ───────────────────────────

    private boolean createPersonalNote(String title) {
        backToBox();
        BoxSchedulePage box = box();
        if (!box.openDrawer() || !box.drawerCreateNote()) return false;
        BoxCreateEventPage f = new BoxCreateEventPage(driver);
        if (!f.isLoaded(Duration.ofSeconds(15))) return false;
        if (!f.onNoteTab() && !f.chooseNoteTab()) return false;
        if (!f.setNoteTitle(title)) return false;
        f.setNoteText("Written by automation (BOX-02).");
        if (f.noteTypeToggleShown() && !f.chooseNoteType("Personal")) return false;
        sleep(1200);
        if (f.noteDistributeShown()) return false;   // not personal after all — do not send it to anyone
        if (!f.save()) return false;
        sleep(4000);
        if (f.calendarPromptShown(Duration.ofSeconds(4))) f.answerCalendarPrompt(false);
        sleep(2500);
        return !f.stillOpen();
    }

    /** Drawer → History, no date filter, every card. Leaves History again. */
    private List<BoxHistoryPage.Card> historyCards() {
        backToBox();
        BoxSchedulePage box = box();
        assumeTrue(box.openDrawer() && box.drawerHistory(), "History is not offered in the drawer");
        BoxHistoryPage h = new BoxHistoryPage(driver);
        assumeTrue(h.isLoaded(Duration.ofSeconds(25)), "History did not load");
        h.clearDate();
        sleep(1500);
        List<BoxHistoryPage.Card> cards = h.allCards(8);
        try { driver.navigate().back(); } catch (Exception ignored) { }
        sleep(1000);
        return cards;
    }

    private void deleteNote(String title) {
        try {
            backToBox();
            BoxSchedulePage box = box();
            assumeTrue(box.openListView(), "List view not available");
            sleep(1500);
            box.chooseByDate();
            sleep(2500);
            BoxListPage list = new BoxListPage(driver);
            list.search(title);
            sleep(2200);
            if (list.findEvent(title) == null) {
                list.clearSearch();
                sleep(2200);
                list.scrollToTop();
                if (list.findEventScrolling(title, 25) == null) {
                    System.out.println(">>> HS-08 cleanup: \"" + title + "\" not found to delete");
                    return;
                }
            }
            if (list.deleteEvent(title)) { sleep(1200); list.confirmDelete(); sleep(2500); }
            list.clearSearch();
        } catch (Exception | AssertionError e) {
            System.out.println(">>> HS-08 cleanup failed: " + e);
        }
    }
}
