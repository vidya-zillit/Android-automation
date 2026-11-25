package com.zillit.automation.tests.homechat;

import com.zillit.automation.core.TestOrder;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.Config;
import com.zillit.automation.core.Messages;
import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.pages.common.BottomNavPage;
import com.zillit.automation.pages.common.ProjectListPage;
import com.zillit.automation.pages.homechat.HomeChatPage;
import io.appium.java_client.AppiumBy;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Home unit chats — the cases the web E2E report has and the Android suites lacked
 * (gap file {@code web-e2e/gaps/home.md}, ids M1..M39). One device, chained, inside the
 * project; Bulletin plays the "chat unit" role and Call Sheet the "other unit".
 *
 * <p>Every message posted is {@code ZL_AUTO}-prefixed and deleted again in the case's
 * {@code finally} (best effort). Cases that reproduce a known app bug FAIL and name it.
 *
 * <p>Run: {@code ZILLIT_FLAVOR=dev ZILLIT_UDID=<serial> ./run-and-report.sh --only HomeWebGapTest}
 */
@Order(TestOrder.HOME_WEB_GAP)
@ExtendWith(ReasonLogger.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class HomeWebGapTest extends BaseTest {

    private static final Duration SENT = Duration.ofSeconds(30);
    private boolean insideProject;
    private String bulletin;
    private String callSheet;

    @Override
    protected boolean relaunchBetweenTests() {
        return false;
    }

    @Override
    protected boolean requiresLoggedOutStart() {
        return false;
    }

    @BeforeEach
    void enterProject() {
        if (insideProject) {
            dismissPermissionPrompts();
            clearBlockingPopup();
            recover();
            return;
        }
        enterTestProjectIfOnList();
        assumeTrue(new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(60)),
                "Could not enter a project (landing=" + landing + ")");
        HomeChatPage p = new HomeChatPage(driver);
        assumeTrue(p.isLoaded(), "Home units strip not visible");
        bulletin = p.bulletinLikeUnit();
        callSheet = p.unitNameContaining("call");
        System.out.println(">>> units=" + p.visibleUnitNames() + " bulletin=" + bulletin + " callSheet=" + callSheet);
        insideProject = true;
        sweepKnownLeftovers(p);
    }

    /**
     * Kill and reopen the app. BaseTest.relaunchApp() uses terminateApp's 500 ms default, which the
     * Pixel overran ("is still running after 500ms timeout") and the case errored on it.
     */
    private void relaunch() {
        // A cold start on the Pixel raises Zillit's app lock (BiometricPrompt with the device
        // credential), which a test must never answer: relaunch on emulators only.
        assumeTrue(udid().startsWith("emulator"), "Relaunch needed, but a cold start on " + udid()
                + " raises the app lock (device credential) — emulator only");
        try {
            driver.executeScript("mobile: terminateApp", java.util.Map.of("appId", appPackage, "timeout", 10000));
        } catch (WebDriverException e) {
            System.out.println(">>> terminateApp: " + e.getMessage());
        }
        sleep(1000);
        driver.executeScript("mobile: activateApp", java.util.Map.of("appId", appPackage));
        landing = settleLanding(90, false);
    }

    /**
     * Rows an early run of this suite left behind (posted by Vidya Pixel, before the markers became
     * digit-free). In-chat search scrolls each one into view wherever it is; an admin account
     * (Vidya Testing) may delete another member's row.
     */
    private static final List<String> KNOWN_LEFTOVERS = List.of("ZL_AUTO-m1-base-1453025", "ZL_AUTO-m6-dbl-1582984",
            "ZL_AUTO-m21-gal-odzsyho", "ZL_AUTO-m4-off-oecbsyy", "ZL_AUTO-m4-offdel-oecknqx",
            "ZL_AUTO-m3-offsw-oecvurp");

    private void sweepKnownLeftovers(HomeChatPage p) {
        if (bulletin == null || !p.selectUnit(bulletin) || !p.isInputBarVisible()) return;
        for (String t : KNOWN_LEFTOVERS) {
            try {
                if (p.openSearch()) {
                    p.searchFor(t, true);
                    String c = p.searchCount();
                    p.closeSearch();
                    if ("0".equals(c)) { System.out.println(">>> leftover '" + t + "' not in " + bulletin); continue; }
                }
                if (p.messageFailed(t)) { p.tapRetry(t); p.waitMessageSent(t, Duration.ofSeconds(30)); }
                boolean gone = p.deleteOwnMessage(t);
                if (!gone && p.messageShown(t, Duration.ofSeconds(2))) {
                    // admin deleting another member's row: Delete enters selection mode → action bar → Yes
                    if (p.longPressMessage(t) && p.tapMenuOption("Delete")) {
                        p.tapDeleteActionBar(Duration.ofSeconds(5));
                        p.confirmPopup().acceptIfShown(Duration.ofSeconds(5));
                        sleep(1500);
                    }
                    if (p.inSelectionMode(Duration.ofMillis(500))) p.exitSelectionMode();
                    gone = !p.messageShown(t, Duration.ofSeconds(2));
                }
                System.out.println(">>> leftover '" + t + "' removed=" + gone);
            } catch (RuntimeException e) {
                System.out.println(">>> leftover sweep of '" + t + "' failed: " + e.getMessage());
            } finally {
                if (p.isSearchOpen(Duration.ofMillis(300))) p.closeSearch();
                recover();
            }
        }
    }

    /** On the project list, enter the automation project (first project as a fallback). */
    private void enterTestProjectIfOnList() {
        if (landing != Landing.PROJECT_LIST) return;
        ProjectListPage list = new ProjectListPage(driver);
        List<String> names = list.visibleProjectNames();
        assumeTrue(!names.isEmpty(), "No projects to enter");
        String target = names.stream().filter(n -> n.contains("Automation")).findFirst().orElse(names.get(0)); // run project, when set, is the only name listed
        assumeTrue(list.tapProject(target), "Could not tap project " + target);
    }

    @AfterAll
    void clearDrafts() {
        // Never leave an emulator offline for the next suite / agent.
        if (udid().startsWith("emulator")) setOffline(false);
        // Drafts are kept per unit in memory AND SharedPreferences; never leave one behind
        // for the next suite.
        try {
            HomeChatPage p = new HomeChatPage(driver);
            recover();
            for (String u : new String[]{bulletin, callSheet}) {
                if (u != null && p.selectUnit(u)) {
                    int n = p.sweepOwnRows("ZL_AUTO-m", 12);   // this suite's own tags (m1, m2 …) only
                    if (n > 0) System.out.println(">>> swept " + n + " leftover ZL_AUTO rows from " + u);
                }
            }
            // Images this suite posted and could not delete in its own finally: the runner's
            // zl_test_*.jpg fixture is 1.44 KB, which no real photo in this project is.
            if (bulletin != null && p.selectUnit(bulletin)) {
                int imgs = 0;
                while (imgs < 6 && p.deleteNewestMediaWithSize("File Size : 1.44 KB")) imgs++;
                if (imgs > 0) System.out.println(">>> swept " + imgs + " fixture images from " + bulletin);
            }
            for (String u : new String[]{bulletin, callSheet}) {
                if (u != null && p.selectUnit(u) && p.isInputBarVisible()) p.clearMessage();
            }
        } catch (RuntimeException ignored) { }
    }

    // ------------------------------------------------------------------ helpers

    /** Back to a unit chat with the composer: closes menus / selection / search / dialogs. */
    private void recover() {
        HomeChatPage p = new HomeChatPage(driver);
        // A case that crashed the app (M7 reproduces an OOM) leaves the launcher in front:
        // relaunch and re-enter the project instead of pressing BACK at the launcher.
        String cur;
        try { cur = driver.getCurrentPackage(); } catch (WebDriverException e) { cur = null; }
        if (cur == null || !cur.equals(appPackage)) {
            System.out.println(">>> app not in front (" + cur + ") — relaunching");
            relaunch();
            enterTestProjectIfOnList();
            new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(60));
            p.isLoaded();
        }
        for (int i = 0; i < 4 && !p.isInputBarVisible(); i++) {
            if (p.inSelectionMode(Duration.ofMillis(500)) || p.isSearchOpen(Duration.ofMillis(300))) {
                p.exitSelectionMode();
                continue;
            }
            if (p.isMenuOpen(Duration.ofMillis(300))) { p.closeMenu(); continue; }
            if (p.isLoaded() && bulletin != null) { p.selectUnit(bulletin); continue; }
            try { driver.navigate().back(); } catch (WebDriverException ignored) { }
            sleep(600);
        }
        if (!p.isInputBarVisible() && !p.isLoaded()) {
            // Still not inside the project (splash, project list, a half-finished relaunch).
            System.out.println(">>> recover: Home not reachable — relaunching into the project");
            relaunch();
            enterTestProjectIfOnList();
            new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(60));
            if (p.isLoaded() && bulletin != null) p.selectUnit(bulletin);
        }
    }

    private HomeChatPage onBulletin() {
        HomeChatPage p = new HomeChatPage(driver);
        assumeTrue(p.isLoaded(), "Home units strip not visible");
        assumeTrue(bulletin != null, "No Bulletin-like chat unit on this project");
        assumeTrue(p.selectUnit(bulletin), "Could not select " + bulletin);
        assumeTrue(p.isInputBarVisible(), "No posting rights on " + bulletin);
        return p;
    }

    private HomeChatPage onCallSheet() {
        HomeChatPage p = new HomeChatPage(driver);
        assumeTrue(callSheet != null, "No Call Sheet unit on this project");
        assumeTrue(p.selectUnit(callSheet), "Could not select " + callSheet);
        assumeTrue(p.isInputBarVisible(), "No posting rights on " + callSheet);
        return p;
    }

    private static String marker(String tag) {
        return "ZL_AUTO-" + tag + "-" + HomeChatPage.letterStamp();
    }

    /** Posts and waits for the server ack; skips (precondition) when it never arrives. */
    private void post(HomeChatPage p, String text) {
        p.sendMessage(text);
        assumeTrue(p.waitMessageSent(text, SENT),
                "Precondition: '" + text + "' must reach the server — it is " + p.sendStateOf(text));
        p.revealNewestRow();
    }

    private void cleanup(HomeChatPage p, String... texts) {
        for (String t : texts) {
            try {
                if (t != null && p.messageFailed(t)) {      // an offline leftover: send it, then delete
                    p.tapRetry(t);
                    p.waitMessageSent(t, Duration.ofSeconds(30));
                }
                if (t != null) p.deleteOwnMessage(t);
            } catch (RuntimeException e) {
                System.out.println(">>> cleanup of '" + t + "' failed: " + e.getMessage());
            }
        }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    // ===================================================================== P1

    @Test
    @Order(1)
    // [WEB M22 #25 #27 #28] (also strengthens #25 unitsVisible)
    @DisplayName("[WEB M22] Unit strip: real labels, Bulletin/Calendar/Call Sheet present, exactly one chip selected, Calendar hides the composer")
    void unitStripLabelsAndCalendar() {
        HomeChatPage p = new HomeChatPage(driver);
        assumeTrue(p.isLoaded(), "Home units strip not visible");
        List<String> raw = p.rawUnitLabels();
        System.out.println(">>> unit chips: " + raw);
        assertTrue(!raw.isEmpty(), "The unit strip lists no chips");
        for (String l : raw) {
            assertTrue(!l.isBlank(), "A unit chip has a blank label: " + raw);
            assertTrue(!l.toLowerCase().endsWith("_label"), "A unit chip shows a raw label key: " + l);
        }
        String all = String.join("|", raw).toLowerCase();
        assertTrue(all.contains("bulletin"), "Bulletin chip missing: " + raw);
        assertTrue(all.contains("calendar"), "Calendar chip missing: " + raw);
        assertTrue(all.contains("call"), "Call Sheet chip missing: " + raw);

        String cal = p.unitNameContaining("calendar");
        assertTrue(p.selectUnit(cal), "Could not tap the Calendar chip");
        sleep(1200);
        assertTrue(!p.isInputBarVisible(), "Calendar must hide the composer (Home.kt showCalendarView)");
        assertTrue(cal.equals(p.selectedUnitName()) || p.selectedUnitName().isEmpty(),
                "After tapping Calendar the selected chip is '" + p.selectedUnitName() + "'");
        p.selectUnit(bulletin);
        assertTrue(p.isInputBarVisible(), "Back on " + bulletin + " the composer should return");
    }

    @Test
    @Order(2)
    // [WEB M1 #33]
    @DisplayName("[WEB M1] A pending reply quote is dropped when switching units; the next Send is a normal post")
    void replyQuoteDroppedOnUnitSwitch() {
        HomeChatPage p = onCallSheet();
        p.clearMessage();                      // no Call Sheet draft in the way
        p = onBulletin();
        String base = marker("m1-base");
        String after = null;
        try {
            post(p, base);
            assumeTrue(p.longPressMessage(base), "Long-press on the base message failed");
            assumeTrue(p.tapMenuOption("Reply"), "Reply missing from the menu");
            assumeTrue(p.replyBarShown(Duration.ofSeconds(5)), "Reply bar did not open");

            assertTrue(p.selectUnit(callSheet), "Could not switch to " + callSheet);
            sleep(1200);
            boolean quoteSurvived = p.replyBarShown(Duration.ofSeconds(2));
            String quote = p.replyBarQuote();
            assertTrue(!quoteSurvived,
                    "[WEB #33] The reply quote ('" + quote + "') from " + bulletin + " is still shown after switching to "
                            + callSheet + " — Home.handleUnitChangeObserver never calls keyBoardFragment.resetUi(), so "
                            + "the next Send would become a comment on the other unit's message");
            assertEquals("", p.composerText(), "The composer should be empty after the unit switch");

            after = marker("m1-after");
            p.sendMessage(after);
            assertTrue(p.waitMessageSent(after, SENT),
                    "The post after the switch should be a normal row in " + callSheet + " — it is " + p.sendStateOf(after));
        } finally {
            p.cancelReplyPreview();
            if (after != null) cleanup(p, after);
            p.selectUnit(bulletin);
            p.cancelReplyPreview();
            p.clearMessage();
            cleanup(p, base);
        }
    }

    @Test
    @Order(3)
    // [WEB M2 #41 #42 #57] (sheet BUL-27)
    @DisplayName("[WEB M2] Each unit keeps its own unsent draft (unit switch and bottom-nav round trip); a sent draft is cleared")
    void draftsArePerUnit() {
        HomeChatPage p = onCallSheet();
        p.clearMessage();
        p = onBulletin();
        p.clearMessage();
        String dB = marker("m2-draft-b");
        String dC = marker("m2-draft-cs");
        boolean sentB = false;
        try {
            p.typeMessage(dB);
            assertTrue(p.selectUnit(callSheet), "Could not switch to " + callSheet);
            assertEquals("", p.composerText(), "BUL-27: the " + bulletin + " draft leaked into " + callSheet);
            p.typeMessage(dC);

            p.selectUnit(bulletin);
            assertEquals(dB, p.composerText(), "Back on " + bulletin + " its own draft should be restored");
            p.selectUnit(callSheet);
            assertEquals(dC, p.composerText(), "Back on " + callSheet + " its own draft should be restored");

            p.selectUnit(bulletin);
            BottomNavPage nav = new BottomNavPage(driver);
            if (nav.selectTab("Email")) {
                sleep(2500);
                dismissPermissionPrompts();
                // Email can open on top of the tab bar (account setup / an inbox screen).
                for (int i = 0; i < 3 && driver.findElements(byAppId("bottomNavigation")).isEmpty(); i++) {
                    try { driver.navigate().back(); } catch (WebDriverException ignored) { }
                    sleep(900);
                }
                assumeTrue(nav.goHome(), "Could not get back to the Home tab from Email (bottom nav: " + nav.tabLabels() + ")");
                boolean back = false;
                for (int i = 0; i < 3 && !back; i++) back = p.isLoaded();
                assertTrue(back, "Home did not come back after Home → Email → Home");
                if (!bulletin.equals(p.selectedUnitName())) p.selectUnit(bulletin);
                assertEquals(dB, p.composerText(), "The " + bulletin + " draft should survive Home → Email → Home");
            } else {
                System.out.println(">>> M2: no Email tab — bottom-nav round trip not exercised");
            }

            p.tapSend();
            sentB = true;
            assertTrue(p.waitMessageSent(dB, SENT), "The draft should post — it is " + p.sendStateOf(dB));
            p.selectUnit(callSheet);
            p.selectUnit(bulletin);
            assertEquals("", p.composerText(), "A sent draft must be cleared, not restored");
        } finally {
            p.selectUnit(callSheet);
            p.clearMessage();
            p.selectUnit(bulletin);
            p.clearMessage();
            if (sentB) cleanup(p, dB);
        }
    }

    @Test
    @Order(4)
    // [WEB M3 #148] (sheet BUL-28) — online half only; the offline half needs a network cut
    @DisplayName("[WEB M3] A post still sending when the unit changes ends as exactly one sent row")
    void postInFlightDuringUnitSwitch() {
        HomeChatPage p = onBulletin();
        String m = marker("m3-inflight");
        try {
            p.typeMessage(m);
            p.tapSend();
            p.selectUnit(callSheet);           // at once
            sleep(8000);
            p.selectUnit(bulletin);
            assertTrue(p.waitMessageSent(m, SENT), "The in-flight post should end SENT — it is " + p.sendStateOf(m));
            int rows = p.rowsContaining(m);
            assertEquals(1, rows, "BUL-28: the in-flight post shows " + rows + " rows (duplicated or lost)");
        } finally {
            cleanup(p, m);
        }
    }

    @Test
    @Order(5)
    // [WEB M6 #60 #146] (sheet BUL-22)
    @DisplayName("[WEB M6] A fast double tap on Send posts once and does not start a voice recording")
    void doubleTapSendPostsOnce() {
        HomeChatPage p = onBulletin();
        String m = marker("m6-dbl");
        try {
            p.typeMessage(m);
            p.doubleTapSend();
            sleep(4000);
            boolean recording = p.recordingBarShown(Duration.ofSeconds(1));
            p.revealNewestRow();
            String pkgNow = p.safeCurrentPackage();
            boolean permissionDialog = pkgNow != null && pkgNow.contains("permissioncontroller");
            int rows = p.rowsContaining(m);
            assertEquals(1, rows, "BUL-22: a double tap on Send produced " + rows + " rows");
            assertTrue(!recording && !permissionDialog,
                    "BUL-22: the second tap landed on the mic (ivRecord swaps into Send's place) — recording="
                            + recording + " permissionDialog=" + permissionDialog);
        } finally {
            p.cancelRecording();
            if (p.recordingBarShown(Duration.ofMillis(500))) p.cancelRecording();
            dismissPermissionPrompts();
            cleanup(p, m);
        }
    }

    @Test
    @Order(99)   // last: on this build it can crash the app (OOM), see the run notes
    // [WEB M7 #142] (sheet BUL-13)
    @DisplayName("[WEB M7] Typing 1,500 characters at machine speed keeps the app alive with all the text and bounded heap")
    void machineSpeedTyping() {
        HomeChatPage p = onBulletin();
        assumeTrue(adbAvailable(), "adb is not reachable from the test JVM (ANDROID_HOME) — cannot feed the IME");
        p.clearMessage();
        try {
            p.tapComposer();
            long before = javaHeapKb();
            String chunk = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWX"; // 50
            long t0 = System.currentTimeMillis();
            for (int i = 0; i < 30; i++) adb("shell", "input", "text", chunk);
            long injectMs = System.currentTimeMillis() - t0;
            // The app may still be working through the queued key events (every keystroke writes
            // the whole draft to SharedPreferences): wait until the text stops growing, ≤ 90 s.
            int last = -1, stable = 0;
            long until = System.currentTimeMillis() + 90_000;
            while (stable < 2 && System.currentTimeMillis() < until) {
                sleep(3000);
                int now = p.composerText().length();
                stable = now == last ? stable + 1 : 0;
                last = now;
            }
            System.out.println(">>> M7: injected in " + injectMs + " ms, composer settled at " + last + " chars");
            long after = javaHeapKb();
            String pkgNow = p.safeCurrentPackage();
            assertTrue(appPackage.equals(pkgNow), "BUL-13: the app left the foreground (crash/OOM?) — now " + pkgNow);
            int len = p.composerText().length();
            System.out.println(">>> M7: typed len=" + len + " heap before=" + before + "KB after=" + after + "KB");
            assertTrue(len >= 1500, "The composer should hold all 1,500 typed characters, holds " + len);
            if (before > 0 && after > 0) {
                assertTrue(after - before < 150 * 1024,
                        "BUL-13: Java heap grew by " + (after - before) / 1024 + " MB while typing 1,500 characters");
            }
        } finally {
            recover();
            p.clearMessage();
        }
    }

    @Test
    @Order(7)
    // [WEB M10 #77 #78] (sheet BUL-12) + strengthens #76 (prefill and "Edited" tag)
    @DisplayName("[WEB M10] Edit dialog: prefilled, Cancel keeps text, unchanged Update adds no Edited, empty/over-limit rejected, real edit marks Edited")
    void editDialogValidation() {
        HomeChatPage p = onBulletin();
        String m = marker("m10-edit");
        String edited = m + "-EDITED";
        try {
            post(p, m);
            assertTrue(!p.rowTime(m).startsWith("Edited"), "A fresh row must not say Edited: " + p.rowTime(m));

            openEdit(p, m);
            assertEquals(m, p.editDialogText(), "#76: the edit dialog should be prefilled with the message");
            p.cancelEditDialog();
            sleep(800);
            assertTrue(p.messageShown(m, Duration.ofSeconds(3)), "Cancel must keep the text");
            assertTrue(!p.rowTime(m).startsWith("Edited"), "Cancel must not mark the row Edited");

            openEdit(p, m);
            p.saveEditDialog();                // nothing changed
            sleep(2500);
            assertTrue(!p.isEditDialogOpen(Duration.ofSeconds(1)), "Unchanged Update should close the dialog");
            assertTrue(!p.rowTime(m).startsWith("Edited"),
                    "BUL-12: an unchanged Update marked the row Edited ('" + p.rowTime(m) + "')");

            openEdit(p, m);
            p.setEditDialogTextNoBack("");
            p.saveEditDialog();
            sleep(800);
            assertTrue(p.isEditDialogOpen(Duration.ofSeconds(2)),
                    "Empty text must keep the dialog open (\"Message could not be empty\")");
            p.cancelEditDialog();
            sleep(600);
            assertTrue(p.messageShown(m, Duration.ofSeconds(3)), "The empty-text attempt must not change the row");

            openEdit(p, m);
            p.setEditDialogTextNoBack("b".repeat(Messages.TEXT_LIMIT + 50));
            p.saveEditDialog();
            sleep(800);
            boolean limitMsg = p.textShown(Messages.WORD_LIMIT, Duration.ofSeconds(2));
            assertTrue(p.isEditDialogOpen(Duration.ofSeconds(2)),
                    "Over 2,000 characters must be refused and keep the dialog open");
            System.out.println(">>> M10: over-limit snackbar visible through the dialog: " + limitMsg);
            p.cancelEditDialog();
            sleep(600);
            assertTrue(p.messageShown(m, Duration.ofSeconds(3)), "The over-limit attempt must not change the row");

            openEdit(p, m);
            p.setEditDialogTextNoBack(edited);
            p.saveEditDialog();
            assertTrue(p.messageShown(edited, Duration.ofSeconds(15)), "#76: the edited text should show");
            sleep(1500);
            assertTrue(p.rowTime(edited).startsWith("Edited"),
                    "#76: an edited row should carry the red 'Edited' prefix, tvMsgTime='" + p.rowTime(edited) + "'");
        } finally {
            if (p.isEditDialogOpen(Duration.ofMillis(500))) p.cancelEditDialog();
            cleanup(p, edited, m);
        }
    }

    private void openEdit(HomeChatPage p, String text) {
        assumeTrue(p.longPressMessage(text), "Long-press failed on " + text);
        assumeTrue(p.tapMenuOption("Edit"), "Edit missing from the own-message menu: " + p.menuOptions());
        assumeTrue(p.isEditDialogOpen(Duration.ofSeconds(6)), "Edit dialog did not open");
    }

    // ===================================================================== P2

    @Test
    @Order(8)
    // [WEB M18 #87 #88] + strengthens #86 (sender name in the reply bar) and #84 (nesting)
    @DisplayName("[WEB M18] Replying: quote names the sender, paperclip and mic hidden, spaces refused, reply nests once under the post")
    void composerRulesWhileReplying() {
        HomeChatPage p = onBulletin();
        String base = marker("m18-base");
        String reply = marker("m18-reply");
        try {
            post(p, base);
            String sender = p.rowText(base, "tvUserName");
            assumeTrue(p.longPressMessage(base), "Long-press failed");
            assumeTrue(p.tapMenuOption("Reply"), "Reply missing");
            assertTrue(p.replyBarShown(Duration.ofSeconds(5)), "Reply bar should open");
            assertTrue(p.replyBarQuote().contains(base), "#86: the reply bar should quote the post, shows '" + p.replyBarQuote() + "'");
            if (!sender.isBlank()) {
                assertEquals(sender, p.replyBarUserName(), "#86: the reply bar should name the post's sender");
            }
            assertTrue(!p.isAttachmentVisible(), "The paperclip must be hidden while replying");
            assertTrue(!p.isRecordButtonVisible(), "The mic must be hidden while replying");

            p.typeMessage("   ");
            try { p.tapSend(); } catch (WebDriverException ignored) { /* Send may stay hidden */ }
            assertTrue(p.replyBarShown(Duration.ofSeconds(2)), "Spaces must not consume the reply bar");

            p.cancelReplyPreview();
            sleep(600);
            assertTrue(!p.replyBarShown(Duration.ofSeconds(1)), "Cancel should close the reply bar");
            assertTrue(p.isAttachmentVisible(), "After cancel the paperclip is back");

            assumeTrue(p.longPressMessage(base), "Long-press failed (2nd)");
            assumeTrue(p.tapMenuOption("Reply"), "Reply missing (2nd)");
            p.sendMessage(reply);
            sleep(5000);                         // socket echo window (Home.kt:600-620)
            assertTrue(p.replyUnderRow(base, reply), "#84: the reply should nest inside the post's rvMessageReply");
            assertEquals(1, p.rowsContaining(reply), "#84: the reply should show exactly once");
        } finally {
            p.cancelReplyPreview();
            p.clearMessage();
            cleanup(p, base);
        }
    }

    @Test
    @Order(9)
    // [WEB M23 #43]
    @DisplayName("[WEB M23] Empty composer: mic on Bulletin, no mic on Call Sheet")
    void micOnBulletinNotOnCallSheet() {
        HomeChatPage p = onBulletin();
        p.clearMessage();
        assertTrue(p.isRecordButtonVisible(), "The mic should show on " + bulletin + " with an empty composer");
        p = onCallSheet();
        p.clearMessage();
        assertTrue(!p.isRecordButtonVisible(), "The mic must be hidden on " + callSheet + " (hideRecordingBtn)");
        assertTrue(p.isHistoryButtonVisible(), "History shows on " + callSheet);
        p.selectUnit(bulletin);
        assertTrue(!p.isHistoryButtonVisible(), "History must not show on " + bulletin);
    }

    @Test
    @Order(10)
    // [WEB M11 #20 #104 #105 #143 #153] (sheet BUL-14; BUL-30 needs two devices)
    @DisplayName("[WEB M11] In-chat search: blank query refused, exact match counted, edited query re-searched, sender name matches, close restores composer")
    void inChatSearch() {
        HomeChatPage p = onBulletin();
        String m = marker("m11-find");
        try {
            post(p, m);
            String me = p.rowText(m, "tvUserName");
            assertTrue(p.openSearch(), "The search bar should open from ivSearch");

            p.searchFor("   ", true);
            assertTrue(p.textShown("Please type text to search", Duration.ofSeconds(4)),
                    "A blank query should say 'Please type text to search.'");

            p.searchFor(m, true);
            assertEquals("1", p.searchCount(), "An exact fresh marker should match exactly once");
            assertTrue(p.messageShown(m, Duration.ofSeconds(3)), "The match should be scrolled into view");

            p.searchFor("zzq-no-match-" + System.currentTimeMillis(), false);
            p.tapSearchArrow(true);
            assertEquals("0", p.searchCount(),
                    "BUL-14: after changing the query the arrows must search again, not step the old result");

            if (!me.isBlank()) {
                p.searchFor(me, true);
                String c = p.searchCount();
                assertTrue(!c.isBlank() && !"0".equals(c), "Searching your own name ('" + me + "') should match (sender is searched), count=" + c);
            }

            p.closeSearch();
            assertTrue(p.isInputBarVisible(), "Closing search should restore the composer");
        } finally {
            if (p.isSearchOpen(Duration.ofMillis(500))) p.closeSearch();
            cleanup(p, m);
        }
    }

    @Test
    @Order(11)
    // [WEB M12 #4 #22 #108 #145]
    @DisplayName("[WEB M12] Feed opens on the newest row; jump-to-latest arrow shows only when scrolled away and brings the newest back")
    void scrollToBottomArrow() {
        HomeChatPage p = onBulletin();
        String m = marker("m12-newest");
        // Two tall (~900-character, below the 1,000 Read-more limit) rows first, so the feed is
        // long enough to scroll the newest row away even on a freshly cleaned unit.
        String f1 = marker("m12-fill1"), f2 = marker("m12-fill2");
        String pad = " lorem ipsum dolor".repeat(48);
        try {
            post(p, f1 + pad);
            post(p, f2 + pad);
            post(p, m);
            p.selectUnit(callSheet);
            p.selectUnit(bulletin);
            sleep(1500);
            assertTrue(p.messageShown(m, Duration.ofSeconds(5)), "Bulletin should open on its newest row");
            assertTrue(!p.arrowDownShown(Duration.ofSeconds(1)), "The jump arrow must be hidden at the newest row");
            assumeTrue(p.chatRowCount() >= 3, "Too few rows to scroll away from the newest");
            p.scrollToOlder(4);
            assumeTrue(!p.rowFullyVisible(m), "The feed is too short to scroll the newest row away");
            assertTrue(p.arrowDownShown(Duration.ofSeconds(3)), "The jump arrow should show once the newest row is off screen");
            p.tapArrowDown();
            assertTrue(p.messageShown(m, Duration.ofSeconds(5)), "The arrow should bring the newest row back");
            assertTrue(!p.arrowDownShown(Duration.ofSeconds(2)), "The arrow should hide again at the newest row");
        } finally {
            p.tapArrowDown();
            cleanup(p, m, f2, f1);
        }
    }

    @Test
    @Order(12)
    // [WEB M14 #50 #51 #52 #53 #55]
    @DisplayName("[WEB M14] Text rendering: markup shown literally, special characters and emoji round-trip, >1,000 characters collapse behind Read more")
    void messageTextRendering() {
        HomeChatPage p = onBulletin();
        String ts = HomeChatPage.letterStamp();
        String xss = "<img src=x onerror=alert(1)> <b>bold</b> ZL_AUTO-xss-" + ts;
        String chars = "Film Café ©®™ *bold* _it_ مرحبا 12345 🎬 ZL_AUTO-chars-" + ts;
        String tail = "TAIL-" + ts;
        String longText = "ZL_AUTO-clip-" + ts + " " + "lorem ipsum ".repeat(100) + tail;  // > 1,000
        try {
            post(p, xss);
            assertTrue(p.rowMessage("ZL_AUTO-xss-" + ts).contains("<b>bold</b>"),
                    "Markup must show literally, got '" + p.rowMessage("ZL_AUTO-xss-" + ts) + "'");

            post(p, chars);
            assertEquals(chars, p.rowMessage("ZL_AUTO-chars-" + ts),
                    "Accents, symbols, RTL text and emoji must round-trip unchanged");

            p.sendMessage(longText);
            sleep(3000);
            String shown = p.rowMessage("ZL_AUTO-clip-" + ts);
            System.out.println(">>> M14 long row: len=" + shown.length() + " tail=" + shown.substring(Math.max(0, shown.length() - 30)));
            assertTrue(!shown.isEmpty(), "The long message row should render");
            assertTrue(shown.trim().endsWith("Read more"), "A >1,000-character text should end with 'Read more'");
            assertTrue(!shown.contains(tail), "The collapsed text must hide the tail");
        } finally {
            cleanup(p, xss, chars, "ZL_AUTO-clip-" + ts);
        }
    }

    @Test
    @Order(13)
    // [WEB M15 #71 #72 #74 #19] — text half (strengthens #71 with absence asserts)
    @DisplayName("[WEB M15] Own text menu: no Download / Image Reply / Copy / Distribute; the menu closes with back")
    void ownTextMenuAbsences() {
        HomeChatPage p = onBulletin();
        String m = marker("m15-menu");
        try {
            post(p, m);
            assumeTrue(p.longPressMessage(m), "Long-press failed");
            List<String> opts = p.menuOptions();
            System.out.println(">>> M15 own text menu: " + opts);
            for (String want : List.of("Reply", "Edit", "Delete")) {
                assertTrue(opts.stream().anyMatch(o -> o.equalsIgnoreCase(want)), "Own text menu should offer " + want + ": " + opts);
            }
            for (String bad : List.of("download", "save", "image reply", "copy", "distribut")) {
                assertTrue(opts.stream().noneMatch(o -> o.toLowerCase().contains(bad)),
                        "Own text menu must not offer '" + bad + "': " + opts);
            }
            p.closeMenu();
            sleep(600);
            assertTrue(!p.isMenuOpen(Duration.ofSeconds(1)), "Back should close the menu");
            assertTrue(p.isInputBarVisible(), "Back should leave us in the chat");
        } finally {
            cleanup(p, m);
        }
    }

    @Test
    @Order(14)
    // [WEB M16 #75 #81 #82 #118 #141] (sheet BUL-09)
    @DisplayName("[WEB M16] Selection mode: counts, deselect, back keeps rows, No keeps, unit switch exits, Yes deletes two at once")
    void selectionModeAndDelete() {
        HomeChatPage p = onBulletin();
        String a = marker("m16-a");
        String b = marker("m16-b");
        try {
            post(p, a);
            post(p, b);
            assumeTrue(p.longPressMessage(b), "Long-press failed");
            assumeTrue(p.tapMenuOption("Delete"), "Delete missing");
            assertTrue(p.inSelectionMode(Duration.ofSeconds(4)), "Delete should enter selection mode");
            assertEquals("1", p.selectionCount(), "One row selected after Delete");
            assertTrue(!p.isInputBarVisible(), "The composer hides in selection mode");
            p.tapRow(a);
            assertEquals("2", p.selectionCount(), "Tapping a second own row selects it");
            p.tapRow(a);
            assertEquals("1", p.selectionCount(), "Tapping it again deselects it");
            p.exitSelectionMode();
            assertTrue(p.messageShown(a, Duration.ofSeconds(3)) && p.messageShown(b, Duration.ofSeconds(3)),
                    "Leaving selection mode must delete nothing");

            assumeTrue(p.longPressMessage(b), "Long-press failed (No pass)");
            assumeTrue(p.tapMenuOption("Delete"), "Delete missing (No pass)");
            assertTrue(p.tapDeleteActionBar(Duration.ofSeconds(5)), "Action-bar Delete missing");
            assertTrue(p.confirmPopup().cancel(Duration.ofSeconds(6)), "The delete confirmation should offer No");
            sleep(800);
            if (p.inSelectionMode(Duration.ofMillis(500))) p.exitSelectionMode();
            assertTrue(p.messageShown(b, Duration.ofSeconds(3)), "No must keep the row");

            assumeTrue(p.longPressMessage(b), "Long-press failed (switch pass)");
            assumeTrue(p.tapMenuOption("Delete"), "Delete missing (switch pass)");
            assertTrue(p.inSelectionMode(Duration.ofSeconds(4)), "Selection mode again");
            p.selectUnit(callSheet);
            p.selectUnit(bulletin);
            assertTrue(!p.inSelectionMode(Duration.ofSeconds(1)), "A unit switch must leave selection mode");
            assertTrue(p.messageShown(b, Duration.ofSeconds(3)), "A unit switch must delete nothing");

            assumeTrue(p.longPressMessage(b), "Long-press failed (Yes pass)");
            assumeTrue(p.tapMenuOption("Delete"), "Delete missing (Yes pass)");
            p.tapRow(a);
            assertEquals("2", p.selectionCount(), "Both rows selected");
            assertTrue(p.tapDeleteActionBar(Duration.ofSeconds(5)), "Action-bar Delete missing");
            assertTrue(p.confirmPopup().accept(Duration.ofSeconds(6)), "The delete confirmation should offer Yes");
            sleep(2500);
            assertTrue(!p.messageShown(a, Duration.ofSeconds(2)) && !p.messageShown(b, Duration.ofSeconds(2)),
                    "Yes should delete both selected rows");
        } finally {
            if (p.inSelectionMode(Duration.ofMillis(500))) p.exitSelectionMode();
            cleanup(p, a, b);
        }
    }

    @Test
    @Order(15)
    // [WEB M17 #85 #89 #90 #91]
    @DisplayName("[WEB M17] Comment menu: Edit / Delete / Read By; edit shows, No keeps, Yes removes")
    void commentMenu() {
        HomeChatPage p = onBulletin();
        String base = marker("m17-base");
        String reply = marker("m17-reply");
        String edited = reply + "-ED";
        try {
            post(p, base);
            assumeTrue(p.longPressMessage(base), "Long-press failed");
            assumeTrue(p.tapMenuOption("Reply"), "Reply missing");
            p.sendMessage(reply);
            sleep(5000);
            assumeTrue(p.replyUnderRow(base, reply), "The reply did not nest under the post");

            assertTrue(p.openCommentMenu(reply), "The comment arrow should open the comment menu");
            List<String> opts = p.menuOptions();
            System.out.println(">>> M17 comment menu: " + opts);
            assertTrue(opts.stream().anyMatch(o -> o.equalsIgnoreCase("Edit")), "Comment menu should offer Edit: " + opts);
            assertTrue(opts.stream().anyMatch(o -> o.equalsIgnoreCase("Delete")), "Comment menu should offer Delete: " + opts);
            assertTrue(opts.stream().anyMatch(o -> o.toLowerCase().contains("read by")), "Comment menu should offer Read By: " + opts);

            assertTrue(p.tapMenuOption("Edit"), "Edit tap");
            assertTrue(p.isEditDialogOpen(Duration.ofSeconds(6)), "The comment edit dialog should open");
            p.setEditDialogTextNoBack(edited);
            p.saveEditDialog();
            assertTrue(p.messageShown(edited, Duration.ofSeconds(15)), "The edited comment should show");

            assertTrue(p.openCommentMenu(edited), "Comment menu (No pass)");
            assertTrue(p.tapMenuOption("Delete"), "Delete tap");
            assertTrue(p.confirmPopup().cancel(Duration.ofSeconds(6)), "Delete confirmation should offer No");
            sleep(800);
            assertTrue(p.messageShown(edited, Duration.ofSeconds(3)), "No must keep the comment");

            assertTrue(p.openCommentMenu(edited), "Comment menu (Yes pass)");
            assertTrue(p.tapMenuOption("Delete"), "Delete tap (Yes pass)");
            assertTrue(p.confirmPopup().accept(Duration.ofSeconds(6)), "Delete confirmation should offer Yes");
            sleep(2500);
            assertTrue(!p.replyNestedUnderParent(edited), "Yes must remove the comment");
        } finally {
            p.cancelReplyPreview();
            cleanup(p, base);
        }
    }

    @Test
    @Order(16)
    // [WEB M19 #93 #94 #96] (sheet BUL-20)
    @DisplayName("[WEB M19] Forward page: lists Home and Members; the Home unit list has no Calendar and no Call Sheet for a text")
    void forwardPageContent() {
        HomeChatPage p = onBulletin();
        String m = marker("m19-fwd");
        try {
            post(p, m);
            assertTrue(p.forwardOpensPage(m), "Forward → Yes should open the forward page");
            for (String row : List.of("Home", "Members")) {
                assertTrue(p.textShown(row, Duration.ofSeconds(4)), "The forward page should list '" + row + "'");
            }
            WebElement home = driver.findElement(AppiumBy.androidUIAutomator("new UiSelector().text(\"Home\")"));
            home.click();
            sleep(2000);
            assertTrue(p.textShown(bulletin, Duration.ofSeconds(6)), "Select Unit should list " + bulletin);
            List<String> units = visibleTexts();
            System.out.println(">>> M19 Select Unit texts: " + units);
            assertTrue(units.stream().noneMatch(t -> t.toLowerCase().contains("calendar")),
                    "Calendar must not be a forward target: " + units);
            assertTrue(units.stream().noneMatch(t -> t.equalsIgnoreCase(callSheet)),
                    "BUL-20: Call Sheet is offered as a forward target for a TEXT message (ForwardPage.kt:262-285): " + units);
        } finally {
            new HomeChatPage(driver).leaveOverlayScreen();
            recover();
            cleanup(new HomeChatPage(driver), m);
        }
    }

    private List<String> visibleTexts() {
        List<String> out = new ArrayList<>();
        try {
            for (WebElement e : driver.findElements(AppiumBy.className("android.widget.TextView"))) {
                String t = e.getText();
                if (t != null && !t.isBlank()) out.add(t.trim());
            }
        } catch (WebDriverException ignored) { }
        return out;
    }

    @Test
    @Order(17)
    // [WEB M20 #98] (sheet BUL-18) — single-device half
    @DisplayName("[WEB M20] Read By: a name nobody has shows 'No Users Found', clearing restores the lists")
    void readBySearch() {
        HomeChatPage p = onBulletin();
        String m = marker("m20-readby");
        try {
            post(p, m);
            assertTrue(p.readByOpensPage(m), "Read By User should open the read-receipt page");
            WebElement search = driver.findElement(byAppId("search_widget_edit_textvw"));
            search.click();
            search.sendKeys("zzq-nobody-" + System.currentTimeMillis());
            sleep(1200);
            assertTrue(!driver.findElements(byAppId("tvNoUsersFound")).isEmpty()
                            && driver.findElement(byAppId("tvNoUsersFound")).isDisplayed(),
                    "BUL-18: a search that matches nobody should say 'No Users Found'");
            search.clear();
            sleep(1200);
            boolean stillEmpty = !driver.findElements(byAppId("tvNoUsersFound")).isEmpty()
                    && driver.findElement(byAppId("tvNoUsersFound")).isDisplayed();
            assertTrue(!stillEmpty, "Clearing the search should hide 'No Users Found'");
        } finally {
            new HomeChatPage(driver).leaveOverlayScreen();
            recover();
            cleanup(new HomeChatPage(driver), m);
        }
    }

    @Test
    @Order(18)
    // [WEB M21 #21 #106 #135] — tabs half (the raw-file_size half needs a PDF in Bulletin)
    @DisplayName("[WEB M21] Gallery from the menu shows Media / Docs / Links tabs (no Audio), and a posted URL is counted under Links")
    void galleryTabs() {
        HomeChatPage p = onBulletin();
        // A text carrying a URL is what the library files under Links (HomeChatLibraryActivity.scanList).
        // The URL goes in its own message: a long-press in the middle of a linkified URL lands on
        // the link, not the bubble, and the menu never opened (emulator run, 2026-11-24).
        String key = marker("m21-gal");
        String urlKey = marker("m21-url");
        // URL first, then several lines of words, so the bubble's centre (where the cleanup
        // long-press lands) is plain text, not the link.
        String m = "https://example.com/zl-auto-" + HomeChatPage.letterStamp() + " " + urlKey + " " + "lorem ipsum dolor ".repeat(10).trim();   // the app trims what it sends
        try {
            post(p, m);
            post(p, key);
            assertTrue(p.galleryOpensLibrary(key), "Gallery should open the chat library");
            List<String> tabs = new ArrayList<>();
            try {
                for (WebElement t : driver.findElement(byAppId("tlTabs")).findElements(AppiumBy.className("android.widget.TextView"))) {
                    if (t.getText() != null && !t.getText().isBlank()) tabs.add(t.getText());
                }
            } catch (WebDriverException ignored) { }
            System.out.println(">>> M21 library tabs: " + tabs);
            for (String want : List.of("Media", "Docs", "Links")) {
                assertTrue(tabs.stream().anyMatch(t -> t.startsWith(want)), "Library should have a '" + want + "' tab: " + tabs);
            }
            assertTrue(tabs.stream().noneMatch(t -> t.startsWith("Audio")), "Android's library has no Audio tab: " + tabs);
            assertTrue(tabs.stream().anyMatch(t -> t.matches("Links \\(\\d+\\)")),
                    "Links should show its count once a URL was posted, e.g. 'Links (1)': " + tabs);
        } finally {
            new HomeChatPage(driver).leaveOverlayScreen();
            recover();
            cleanup(new HomeChatPage(driver), key, urlKey);
        }
    }

    @Test
    @Order(19)
    // [WEB M38 #10 #56]
    @DisplayName("[WEB M38] Emoji picker opens, appends the emoji, closes when the text box is tapped, and the message posts with it")
    void emojiPicker() {
        HomeChatPage p = onBulletin();
        String m = marker("m38-emoji");
        String posted = null;
        try {
            p.typeMessage(m);
            assertTrue(p.openEmojiPicker(), "ivEmoji should open the emoji picker");
            String emoji = p.pickFirstEmoji();
            assumeTrue(emoji != null, "No emoji cell could be read from the picker");
            String text = p.composerText();
            assertTrue(text.startsWith(m) && text.length() > m.length(),
                    "The emoji should be appended after the text, composer='" + text + "'");
            p.tapComposer();
            assertTrue(!p.emojiPickerShown(Duration.ofSeconds(2)), "Tapping the text box should close the picker");
            posted = text;
            p.tapSend();
            assertTrue(p.waitMessageSent(m, SENT), "The message with the emoji should post — it is " + p.sendStateOf(m));
            assertTrue(p.rowMessage(m).length() > m.length(), "The posted row should carry the emoji: '" + p.rowMessage(m) + "'");
        } finally {
            p.tapComposer();
            p.clearMessage();
            if (posted != null) cleanup(p, m);
        }
    }

    @Test
    @Order(20)
    // [WEB M39 #9 #107]
    @DisplayName("[WEB M39] '@a' opens no mention list; the row header shows the sender and 'MMM dd, yyyy hh:mm a'")
    void mentionAndRowHeader() {
        HomeChatPage p = onBulletin();
        String m = marker("m39-hdr");
        try {
            p.typeMessage("@a");
            sleep(1500);
            assertTrue(driver.findElements(AppiumBy.androidUIAutomator(
                            "new UiSelector().resourceIdMatches(\".*(mention|tag|suggestion).*\")")).isEmpty(),
                    "The Home composer has no tagging — '@a' must not open a mention list");
            p.clearMessage();
            post(p, m);
            String time = p.rowTime(m);
            String who = p.rowText(m, "tvUserName");
            System.out.println(">>> M39 header: name='" + who + "' time='" + time + "'");
            assertTrue(!who.isBlank(), "The row header should name the sender");
            assertTrue(time.matches("(?i)[a-z]{3} \\d{2}, \\d{4} \\d{2}:\\d{2} (am|pm)"),
                    "tvMsgTime should read 'MMM dd, yyyy hh:mm a', got '" + time + "'");
        } finally {
            p.clearMessage();
            cleanup(p, m);
        }
    }

    @Test
    @Order(21)
    // [WEB M30 #58 #83] + [WEB M22 #31] default unit after relaunch
    @DisplayName("[WEB M30] A post survives a relaunch, a deleted post stays gone, and exactly one unit is selected after relaunch")
    void persistenceAcrossRelaunch() {
        HomeChatPage p = onBulletin();
        String keep = marker("m30-keep");
        String gone = marker("m30-gone");
        try {
            post(p, keep);
            post(p, gone);
            assertTrue(p.deleteOwnMessage(gone), "Could not delete the second marker");
            relaunch();
            enterTestProjectIfOnList();
            assumeTrue(new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(60)), "Project did not reopen after relaunch");
            p = new HomeChatPage(driver);
            assumeTrue(p.isLoaded(), "Home strip did not come back");
            String sel = p.selectedUnitName();
            System.out.println(">>> M30 selected unit after relaunch: '" + sel + "'");
            long selectedCount = p.visibleUnitNames().stream().filter(sel::equals).count();
            assertTrue(!sel.isBlank() && selectedCount == 1, "Exactly one unit should be selected after a relaunch");
            p.selectUnit(bulletin);
            assertTrue(p.messageShownScrolled(keep, 3), "The post should still be there after a relaunch");
            assertTrue(!p.messageShown(gone, Duration.ofSeconds(3)), "The deleted post must not come back after a relaunch");
        } finally {
            HomeChatPage q = new HomeChatPage(driver);
            if (q.isLoaded() && bulletin != null) q.selectUnit(bulletin);
            cleanup(q, keep);
        }
    }

    // ============================================== media / documents (emulator)

    /**
     * Gallery sends pick the first thumbnails in the device gallery. On the Pixel that is the
     * owner's own photo roll, so the image cases run only on an emulator, whose gallery holds
     * the runner's zl_test_*.jpg fixtures.
     */
    private void assumeEmulatorGallery() {
        assumeTrue(udid().startsWith("emulator"),
                "Image cases send the first gallery photos — run them on an emulator, not " + udid());
    }

    /** "Media (n)" / "Docs (n)" / "Links (n)" counts from the chat library, opened from a marker. */
    private java.util.Map<String, Integer> libraryCounts(HomeChatPage p, String marker) {
        java.util.Map<String, Integer> out = new java.util.HashMap<>();
        try {
            if (!p.galleryOpensLibrary(marker)) return out;
            for (WebElement t : driver.findElement(byAppId("tlTabs")).findElements(AppiumBy.className("android.widget.TextView"))) {
                // tabLabel(): "Docs (3)", or the bare "Docs" when the bucket is empty.
                Matcher mt = Pattern.compile("^(\\w+)(?:\\s*\\((\\d+)\\))?$").matcher(t.getText() == null ? "" : t.getText().trim());
                if (mt.find()) out.put(mt.group(1), mt.group(2) == null ? 0 : Integer.parseInt(mt.group(2)));
            }
        } catch (WebDriverException ignored) {
        } finally {
            p.leaveOverlayScreen();
            recover();
        }
        System.out.println(">>> library counts: " + out);
        return out;
    }

    /** Paperclip → Document → system picker → the named fixture. True once the picker handed it over. */
    private boolean sendDocument(HomeChatPage p, String fileName) {
        p.tapAttachment();
        if (!p.isAttachmentPickerVisible()) return false;
        if (!p.tapAttachmentOption("Document")) { p.dismissAttachmentPicker(); return false; }
        for (int i = 0; i < 3; i++) { if (!dismissPermissionPrompts()) break; sleep(400); }
        boolean picked = p.pickSystemFileByName(fileName, Duration.ofSeconds(15));
        if (picked) {
            com.zillit.automation.pages.common.MediaPreviewPage prev = new com.zillit.automation.pages.common.MediaPreviewPage(driver);
            if (prev.isPreviewOpen()) prev.tapPreviewSend();
            p.waitAttachmentSent(Duration.ofSeconds(60));
        }
        backToApp();
        return picked;
    }

    /** Brings the app back to the front after a system picker / gallery. */
    private void backToApp() {
        for (int i = 0; i < 4; i++) {
            String cur;
            try { cur = driver.getCurrentPackage(); } catch (WebDriverException e) { cur = null; }
            if (appPackage.equals(cur)) return;
            try { driver.navigate().back(); } catch (WebDriverException ignored) { }
            sleep(700);
        }
        try { driver.executeScript("mobile: activateApp", java.util.Map.of("appId", appPackage)); } catch (WebDriverException ignored) { }
        sleep(1200);
    }

    /** Deletes the newest own media/document bubble (best effort). */
    private void deleteNewestMedia(HomeChatPage p) {
        try {
            p.revealNewestRow();
            if (!(p.longPressNewestMedia() || p.longPressNewestDocBubble())) return;
            if (!p.tapMenuOption("Delete")) { p.closeMenu(); return; }
            p.tapDeleteActionBar(Duration.ofSeconds(5));
            p.confirmPopup().accept(Duration.ofSeconds(6));
            sleep(1500);
        } catch (RuntimeException ignored) {
        } finally {
            if (p.inSelectionMode(Duration.ofMillis(500))) p.exitSelectionMode();
        }
    }

    @Test
    @Order(22)
    // [WEB M24 #12 #62 #63 #64] (sheet BUL-24)
    @DisplayName("[WEB M24] Document types: .docx and .xlsx post as documents; .csv is pickable and posts (BUL-24)")
    void documentTypes() {
        HomeChatPage p = onBulletin();
        String m = marker("m24-docs");
        int sent = 0;
        try {
            post(p, m);
            Integer before = libraryCounts(p, m).get("Docs");
            assumeTrue(before != null, "Could not read the library Docs count");
            boolean docx = sendDocument(p, "zl_dd_word");
            if (docx) sent++;
            boolean xlsx = sendDocument(p, "zl_dd_book");
            if (xlsx) sent++;
            assumeTrue(docx || xlsx, "The system picker surfaced neither fixture (zl_dd_word.docx / zl_dd_book.xlsx)");
            Integer mid = libraryCounts(p, m).get("Docs");
            assertEquals(before + sent, mid, "Each picked office document should add one Docs item (docx=" + docx + " xlsx=" + xlsx + ")");

            boolean csv = sendDocument(p, "zl_drive_sheet");
            if (csv) sent++;
            assertTrue(csv, "BUL-24: zl_drive_sheet.csv could not be picked — text/csv is missing from the "
                    + "Document picker MIME list (FilePickerContract.kt:29-37)");
            Integer after = libraryCounts(p, m).get("Docs");
            assertEquals(mid + 1, after, "The picked .csv should post as a document");
        } finally {
            for (int i = 0; i < sent; i++) deleteNewestMedia(p);
            cleanup(p, m);
        }
    }

    @Test
    @Order(23)
    // [WEB M27 #67] + [WEB M39 #14]
    @DisplayName("[WEB M27] Backing out of the image preview posts nothing; two images picked together post two items")
    void twoImagesAndPreviewBackOut() {
        assumeEmulatorGallery();
        HomeChatPage p = onBulletin();
        String m = marker("m27-imgs");
        int sent = 0;
        com.zillit.automation.pages.common.MediaPreviewPage media = new com.zillit.automation.pages.common.MediaPreviewPage(driver);
        try {
            post(p, m);
            Integer before = libraryCounts(p, m).get("Media");
            assumeTrue(before != null, "Could not read the library Media count");

            p.tapAttachment();
            assumeTrue(p.isAttachmentPickerVisible() && p.tapAttachmentOption("Gallery"), "Gallery option missing");
            dismissPermissionPrompts();
            assumeTrue(media.isGalleryOpen(), "The in-app gallery did not open");
            media.openAllMediaAlbumIfListed();
            assumeTrue(media.selectPhotos(1) == 1, "No photo in the gallery");
            media.tapGallerySendFab();
            assumeTrue(media.isPreviewOpen(), "The preview did not open");
            media.tapPreviewBack();
            sleep(1500);
            backToApp();
            recover();
            assertEquals(before, libraryCounts(p, m).get("Media"), "#14: backing out of the preview must post nothing");

            p.tapAttachment();
            assumeTrue(p.isAttachmentPickerVisible() && p.tapAttachmentOption("Gallery"), "Gallery option missing (2nd)");
            dismissPermissionPrompts();
            assumeTrue(media.isGalleryOpen(), "The in-app gallery did not open (2nd)");
            media.openAllMediaAlbumIfListed();
            assumeTrue(media.selectPhotos(2) == 2, "Fewer than two photos in the gallery");
            media.tapGallerySendFab();
            assumeTrue(media.isPreviewOpen(), "The preview did not open (2nd)");
            media.tapPreviewSend();
            media.waitPreviewClosed(Duration.ofSeconds(60));
            sent = 2;
            p.waitAttachmentSent(Duration.ofSeconds(90));
            assertEquals(before + 2, libraryCounts(p, m).get("Media"), "Two images picked together should post two items");
        } finally {
            for (int i = 0; i < sent; i++) deleteNewestMedia(p);
            cleanup(p, m);
        }
    }

    @Test
    @Order(24)
    // [WEB M26 #65]
    @DisplayName("[WEB M26] Edit a media caption: the dialog is prefilled and the new caption shows")
    void editMediaCaption() {
        assumeEmulatorGallery();
        HomeChatPage p = onBulletin();
        String cap = marker("m26-cap");
        String cap2 = cap + "-ED";
        boolean sent = false;
        com.zillit.automation.pages.common.MediaPreviewPage media = new com.zillit.automation.pages.common.MediaPreviewPage(driver);
        try {
            p.tapAttachment();
            assumeTrue(p.isAttachmentPickerVisible() && p.tapAttachmentOption("Gallery"), "Gallery option missing");
            dismissPermissionPrompts();
            assumeTrue(media.isGalleryOpen(), "The in-app gallery did not open");
            media.openAllMediaAlbumIfListed();
            assumeTrue(media.selectPhotos(1) == 1, "No photo in the gallery");
            media.tapGallerySendFab();
            assumeTrue(media.isPreviewOpen(), "The preview did not open");
            // Type the caption WITHOUT MediaPreviewPage.setCaption's hideKeyboard (a BACK press),
            // and confirm the preview really holds it before sending.
            WebElement d = driver.findElement(byAppId("description"));
            d.click();
            d.sendKeys(cap);
            assumeTrue(cap.equals(media.captionText()), "The preview caption field did not take the text: '" + media.captionText() + "'");
            media.tapPreviewSend();
            media.waitPreviewClosed(Duration.ofSeconds(60));
            sent = true;
            backToApp();
            assumeTrue(p.waitAttachmentSent(Duration.ofSeconds(90)), "The image did not finish sending: " + p.attachmentSendState());
            p.revealNewestRow();
            assertTrue(p.messageShown(cap, Duration.ofSeconds(10)),
                    "#66: the caption typed in the preview ('" + cap + "') is not shown under the posted image");
            assumeTrue(p.longPressMessage(cap) || p.longPressNewestMedia(), "Long-press on the image failed");
            List<String> opts = p.menuOptions();
            assertTrue(p.tapMenuOption("Edit"), "An image with a caption should offer Edit: " + opts);
            assertTrue(p.isEditDialogOpen(Duration.ofSeconds(6)), "The Edit description dialog should open");
            assertEquals(cap, p.editDialogText(), "The dialog should be prefilled with the caption");
            p.setEditDialogTextNoBack(cap2);
            p.saveEditDialog();
            assertTrue(p.messageShown(cap2, Duration.ofSeconds(15)), "The new caption should show");
        } finally {
            if (p.isEditDialogOpen(Duration.ofMillis(500))) p.cancelEditDialog();
            backToApp();
            recover();
            if (sent) deleteNewestMedia(p);
        }
    }

    // ============================================== more web-gap rows (batch 2)

    /** Taps the chat-library tab whose label starts with the name. */
    private boolean tapLibraryTab(String name) {
        try {
            for (WebElement t : driver.findElement(byAppId("tlTabs")).findElements(AppiumBy.className("android.widget.TextView"))) {
                if (t.getText() != null && t.getText().startsWith(name)) { t.click(); sleep(1200); return true; }
            }
        } catch (WebDriverException ignored) { }
        return false;
    }

    /** Text of a toast currently shown (UiAutomator2 exposes toasts as android.widget.Toast), or "". */
    private String toastText(Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < end) {
            try {
                for (WebElement t : driver.findElements(AppiumBy.xpath("//android.widget.Toast"))) {
                    if (t.getText() != null && !t.getText().isBlank()) return t.getText();
                }
            } catch (WebDriverException ignored) { }
            sleep(300);
        }
        return "";
    }

    /** tvFileSize of the newest document bubble ("File Size : 927 B"), or "". */
    private String newestFileSizeLabel() {
        try {
            List<WebElement> l = driver.findElements(byAppId("tvFileSize"));
            for (int i = l.size() - 1; i >= 0; i--) {
                if (l.get(i).isDisplayed() && l.get(i).getText() != null) return l.get(i).getText();
            }
        } catch (WebDriverException ignored) { }
        return "";
    }

    @Test
    @Order(25)
    // [WEB M28 #100 #101] + [WEB M21 #135 file size] (BUL-19) + [WEB M39 #102] + strengthens #61
    @DisplayName("[WEB M28] A posted PDF: bubble shows its size with a unit, Download says 'File Downloaded Successfully.', the library lists a readable size, tapping it opens a viewer")
    void pdfDownloadLibrarySizeAndOpen() {
        HomeChatPage p = onBulletin();
        String m = marker("m28-pdf");
        boolean sent = false;
        String bul19 = null;
        try {
            post(p, m);
            assumeTrue(sendDocument(p, "zl_test_doc"), "The system picker could not surface zl_test_doc.pdf");
            sent = true;
            recover();
            p.revealNewestRow();
            String size = newestFileSizeLabel();
            System.out.println(">>> M28 bubble size label: '" + size + "'");
            assertTrue(size.matches("(?i).*\\d+(\\.\\d+)?\\s*(B|KB|MB|GB)\\s*$"),
                    "#61: the PDF bubble should show its size with a unit, shows '" + size + "'");

            assertTrue(p.galleryOpensLibrary(m), "Gallery should open the chat library");
            assertTrue(tapLibraryTab("Docs"), "The library should have a Docs tab");
            List<String> subs = new ArrayList<>();
            for (WebElement e : driver.findElements(byAppId("tvSubtitle"))) if (e.getText() != null) subs.add(e.getText());
            System.out.println(">>> M21 Docs subtitles: " + subs);
            assumeTrue(!subs.isEmpty(), "The Docs tab listed nothing to read a size from");
            String first = subs.get(0);
            String sizePart = first.contains("·") ? first.substring(0, first.indexOf('·')).trim() : first.trim();
            // Recorded, not thrown here: the #102 open and the Download check below still run.
            if (!sizePart.matches("(?i).*\\d\\s*(B|KB|MB|GB)$")) {
                bul19 = "BUL-19: the library shows the raw file_size without a unit ('" + first + "') — ChatLibraryAdapter.bindDocument";
                System.out.println(">>> " + bul19);
            }
            p.leaveOverlayScreen();
            recover();

            assertTrue(p.openNewestDoc(), "#102: tapping the PDF bubble should open a viewer or another app");
            backToApp();
            recover();
            assertTrue(p.longPressNewestDocBubble(), "Long-press on the PDF bubble failed");
            List<String> opts = p.menuOptions();
            assertTrue(opts.stream().anyMatch(o -> o.toLowerCase().contains("download")), "A PDF should offer Download: " + opts);
            assertTrue(opts.stream().noneMatch(o -> o.equalsIgnoreCase("Edit")), "A PDF without a caption offers no Edit (M15 #72): " + opts);
            assertTrue(p.tapMenuOption("Download"), "Download tap");
            // The toast comes only after the watermark round-trip and the download finish
            // (Home.handleSave → withWatermarkedAttachment → downloadFile): allow 30 s.
            String toast = toastText(Duration.ofSeconds(30));
            boolean ok = toast.contains("Downloaded") || p.textShown("File Downloaded Successfully", Duration.ofSeconds(2));
            System.out.println(">>> M28 download toast: '" + toast + "'");
            assertTrue(ok, "Download should confirm 'File Downloaded Successfully.' (toast seen: '" + toast + "')"
                    + (bul19 == null ? "" : "; also " + bul19));
            recover();
            assertTrue(bul19 == null, bul19);

        } finally {
            backToApp();
            recover();
            if (sent) deleteNewestMedia(p);
            cleanup(p, m);
        }
    }

    @Test
    @Order(26)
    // [WEB M25 #17] (sheet BUL-23)
    @DisplayName("[WEB M25] A PDF carrying JavaScript is refused and nothing is posted (BUL-23)")
    void pdfWithJavaScriptRefused() {
        HomeChatPage p = onBulletin();
        assumeTrue(adbAvailable(), "adb not reachable — cannot push the generated PDF");
        String m = marker("m25-evil");
        boolean posted = false;
        try {
            Path pdf = Files.createTempFile("zl_evil_js", ".pdf");
            Files.writeString(pdf, "%PDF-1.4\n"
                    + "1 0 obj << /Type /Catalog /Pages 2 0 R /OpenAction 4 0 R >> endobj\n"
                    + "2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n"
                    + "3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 200 200] >> endobj\n"
                    + "4 0 obj << /S /JavaScript /JS (app.alert\\('ZL_AUTO evil js'\\);) >> endobj\n"
                    + "trailer << /Root 1 0 R >>\n%%EOF\n");
            adb("push", pdf.toString(), "/sdcard/Download/zl_evil_js.pdf");
            adb("shell", "touch", "/sdcard/Download/zl_evil_js.pdf");
            adb("shell", "am", "broadcast", "-a", "android.intent.action.MEDIA_SCANNER_SCAN_FILE",
                    "-d", "file:///sdcard/Download/zl_evil_js.pdf");
            post(p, m);
            Integer before = libraryCounts(p, m).get("Docs");
            assumeTrue(before != null, "Could not read the library Docs count");
            assumeTrue(sendDocument(p, "zl_evil_js"), "The system picker could not surface zl_evil_js.pdf");
            recover();
            Integer after = libraryCounts(p, m).get("Docs");
            posted = after != null && after > before;
            assertTrue(!posted, "BUL-23: a PDF with an /OpenAction JavaScript was posted (Docs " + before + " → " + after
                    + "); Home.startUpload has no PDF content check");
        } catch (java.io.IOException e) {
            assumeTrue(false, "Could not write the temp PDF: " + e.getMessage());
        } finally {
            recover();
            if (posted) deleteNewestMedia(p);
            cleanup(p, m);
            adb("shell", "rm", "-f", "/sdcard/Download/zl_evil_js.pdf");
        }
    }

    @Test
    @Order(27)
    // [WEB M29 #150] (sheet BUL-34)
    @DisplayName("[WEB M29] A posted video opens in the player and playback advances")
    void postedVideoPlays() {
        assumeEmulatorGallery();
        HomeChatPage p = onBulletin();
        com.zillit.automation.pages.common.MediaPreviewPage media = new com.zillit.automation.pages.common.MediaPreviewPage(driver);
        boolean sent = false;
        // No video fixture is pushed by the runner; use the SDK's own sample clip
        // ($ANDROID_HOME/emulator/resources/default.mp4, 0.5 MB), pushed only to this emulator.
        String sdk = System.getenv("ANDROID_HOME");
        Path clip = sdk == null ? null : Paths.get(sdk, "emulator", "resources", "default.mp4");
        if (clip != null && Files.isReadable(clip) && adbAvailable()) {
            adb("push", clip.toString(), "/sdcard/Movies/zl_test_video.mp4");
            adb("shell", "am", "broadcast", "-a", "android.intent.action.MEDIA_SCANNER_SCAN_FILE",
                    "-d", "file:///sdcard/Movies/zl_test_video.mp4");
            sleep(2000);
        }
        try {
            p.tapAttachment();
            assumeTrue(p.isAttachmentPickerVisible() && p.tapAttachmentOption("Video Gallery"), "Video Gallery option missing");
            dismissPermissionPrompts();
            assumeTrue(media.isGalleryOpen(), "The in-app video gallery did not open");
            media.openAllMediaAlbumIfListed();
            assumeTrue(media.selectVideos(1) == 1, "No video in the emulator gallery (the SDK sample clip could not be pushed)");
            media.tapGallerySendFab();
            assumeTrue(media.isPreviewOpen(), "The video preview did not open");
            media.tapPreviewSend();
            media.waitPreviewClosed(Duration.ofSeconds(90));
            sent = true;
            assumeTrue(p.waitAttachmentSent(Duration.ofSeconds(120)), "The video did not finish sending: " + p.attachmentSendState());
            assertTrue(p.openNewestImage() || p.isPlayerOpen(Duration.ofSeconds(5)), "BUL-34: tapping the video bubble should open the player");
            String t1 = textOfFirst("exo_position");
            sleep(3000);
            String t2 = textOfFirst("exo_position");
            System.out.println(">>> M29 player position " + t1 + " → " + t2);
            if (!t1.isEmpty() || !t2.isEmpty()) {
                assertTrue(!t1.equals(t2), "BUL-34: the video position did not advance (" + t1 + " → " + t2 + ")");
            } else {
                assertTrue(p.isPlayerOpen(Duration.ofSeconds(2)), "The player should still be open after 3 s");
            }
        } finally {
            p.closeViewer();
            backToApp();
            recover();
            if (sent) deleteNewestMedia(p);
        }
    }

    private String textOfFirst(String id) {
        try {
            List<WebElement> l = driver.findElements(byAppId(id));
            if (l.isEmpty()) l = driver.findElements(AppiumBy.id("androidx.media3.ui:id/" + id));
            return l.isEmpty() || l.get(0).getText() == null ? "" : l.get(0).getText();
        } catch (WebDriverException e) {
            return "";
        }
    }

    @Test
    @Order(28)
    // [WEB M36 #35] — the half this 3-unit project can show
    @DisplayName("[WEB M36] The unit-strip overflow arrow shows only when there are more than 3 units")
    void overflowArrowRule() {
        HomeChatPage p = onBulletin();
        int units = p.rawUnitLabels().size();
        boolean arrow = !driver.findElements(byAppId("more_units_arrow")).isEmpty()
                && driver.findElement(byAppId("more_units_arrow")).isDisplayed();
        System.out.println(">>> M36 units=" + units + " arrow=" + arrow);
        assertEquals(units > 3, arrow, "more_units_arrow must show exactly when there are more than 3 units (Home.kt:418)");
    }

    @Test
    @Order(29)
    // [WEB M39 #149] (sheet BUL-32)
    @DisplayName("[WEB M39] Scrolling to older rows and switching units at once shows no error")
    void scrollThenSwitchShowsNoError() {
        HomeChatPage p = onBulletin();
        p.scrollToOlder(3);
        p.selectUnit(callSheet);
        boolean error = !driver.findElements(AppiumBy.androidUIAutomator(
                "new UiSelector().textMatches(\"(?i).*(went wrong|error|failed to).*\")")).isEmpty();
        assertTrue(!error, "BUL-32: an error message showed after scrolling and switching units");
        assertTrue(p.isInputBarVisible(), "The new unit should be usable");
        p.selectUnit(bulletin);
    }

    @Test
    @Order(31)
    // strengthens web #159 (voice note: timer runs, the bin discards)
    @DisplayName("[WEB #159] Voice note: the timer runs while recording and the bin discards it without posting")
    void voiceNoteTimerAndDiscard() {
        HomeChatPage p = onBulletin();
        p.clearMessage();
        int before = p.audioBubbleCount();
        try {
            assumeTrue(p.isRecordButtonAvailable(), "No mic on " + bulletin);
            p.tapRecord();
            dismissPermissionPrompts();
            assumeTrue(p.recordingBarShown(Duration.ofSeconds(5)), "Recording did not start (mic permission?)");
            String t1 = textOfFirst("audioTimer");
            sleep(2500);
            String t2 = textOfFirst("audioTimer");
            System.out.println(">>> #159 timer " + t1 + " → " + t2);
            assertTrue(!t2.isBlank() && !t2.equals(t1), "The recording timer should advance (" + t1 + " → " + t2 + ")");
            p.cancelRecording();
            sleep(1500);
            assertTrue(!p.recordingBarShown(Duration.ofSeconds(1)), "The bin should close the recording bar");
            assertTrue(p.isInputBarVisible(), "The composer should come back after the bin");
            assertTrue(p.audioBubbleCount() <= before, "The bin must not post a voice note");
        } finally {
            if (p.recordingBarShown(Duration.ofMillis(500))) p.cancelRecording();
        }
    }

    @Test
    @Order(32)
    // [Android-only BUL-15] deny media permissions, then Document
    @DisplayName("[BUL-15] With media permissions denied, Document still opens the picker or explains why")
    void documentWithMediaPermissionsDenied() {
        assumeTrue(udid().startsWith("emulator"), "Revokes runtime permissions — emulator only");
        assumeTrue(adbAvailable(), "adb not reachable");
        String[] perms = {"READ_MEDIA_IMAGES", "READ_MEDIA_VIDEO", "READ_MEDIA_AUDIO", "READ_EXTERNAL_STORAGE"};
        try {
            for (String perm : perms) adb("shell", "pm", "revoke", appPackage, "android.permission." + perm);
            recover();                                  // revoking kills the app
            HomeChatPage p = onBulletin();
            p.tapAttachment();
            assumeTrue(p.isAttachmentPickerVisible() && p.tapAttachmentOption("Document"), "Document option missing");
            for (int i = 0; i < 3; i++) {
                sleep(1500);
                try {
                    driver.findElement(AppiumBy.androidUIAutomator(
                            "new UiSelector().textMatches(\"(?i)(don.t allow|deny)\")")).click();
                } catch (WebDriverException none) { break; }
            }
            sleep(2500);
            String pkg = p.safeCurrentPackage();
            boolean picker = pkg != null && pkg.contains("documentsui");
            boolean explained = p.textShown("ermission", Duration.ofSeconds(2));
            System.out.println(">>> BUL-15 after deny: package=" + pkg + " explained=" + explained);
            assertTrue(picker || explained,
                    "BUL-15: after denying media permissions, Document did nothing — no picker and no message");
        } finally {
            backToApp();
            for (String perm : perms) adb("shell", "pm", "grant", appPackage, "android.permission." + perm);
            recover();
        }
    }

    @Test
    @Order(33)
    // [WEB M31 #119 #124] — same-language half
    @DisplayName("[WEB M31] Translation gate: the 'Project language' banner and the composer's translate toggle agree")
    void translateGateConsistent() {
        HomeChatPage p = onBulletin();
        boolean hint = isShown("translateHint");
        boolean toggle = isShown("translateSendToggle");
        System.out.println(">>> M31 translateHint=" + hint + " translateSendToggle=" + toggle);
        assertEquals(hint, toggle, "The banner and the toggle share CommonApis.isShowTranslateOption() and must agree");
        String m = marker("m31-own");
        try {
            post(p, m);
            assumeTrue(p.longPressMessage(m), "Long-press failed");
            List<String> opts = p.menuOptions();
            assertTrue(opts.stream().noneMatch(o -> o.toLowerCase().contains("translate")),
                    "Translate is never offered on your own message: " + opts);
            p.closeMenu();
        } finally {
            cleanup(p, m);
        }
    }

    private boolean isShown(String id) {
        try {
            for (WebElement e : driver.findElements(byAppId(id))) if (e.isDisplayed()) return true;
        } catch (WebDriverException ignored) { }
        return false;
    }

    // ============================================== offline (emulator only)

    /**
     * Airplane mode on the EMULATOR we hold the lock for. adb reaches an emulator through the
     * host, not the device's network, so Appium keeps working, and no other device is touched.
     * Always restored in the case's finally and again in @AfterAll.
     */
    private void setOffline(boolean off) {
        adb("shell", "cmd", "connectivity", "airplane-mode", off ? "enable" : "disable");
        sleep(off ? 3000 : 6000);
    }

    private void assumeOfflineAllowed() {
        assumeTrue(udid().startsWith("emulator"), "Offline cases toggle airplane mode — emulator only, not " + udid());
        assumeTrue(adbAvailable(), "adb not reachable");
    }

    private boolean tapRetry(String text) {
        return new HomeChatPage(driver).tapRetry(text);
    }

    private boolean waitFailed(HomeChatPage p, String text, Duration t) {
        long end = System.currentTimeMillis() + t.toMillis();
        while (System.currentTimeMillis() < end) {
            p.revealNewestRow();
            if (p.messageFailed(text)) return true;
            sleep(1000);
        }
        return false;
    }

    @Test
    @Order(40)
    // [WEB M4 #112 #113] (a)
    @DisplayName("[WEB M4a] Offline send marks the row failed; back online, Retry sends it exactly once")
    void offlineSendThenRetry() {
        assumeOfflineAllowed();
        HomeChatPage p = onBulletin();
        String m = marker("m4-off");
        try {
            setOffline(true);
            p.sendMessage(m);
            // 75 s: when the app has not yet noticed the drop, the send goes to the API and fails
            // only on the HTTP timeout (HomeVm.sendTextMessage onError → status -1).
            assertTrue(waitFailed(p, m, Duration.ofSeconds(75)),
                    "An offline send should end with Retry (btnTryAgain) on the row — after 75 s offline it is "
                            + p.sendStateOf(m) + " (airplane mode on: " + adb("shell", "settings", "get", "global", "airplane_mode_on").trim() + ")");
            setOffline(false);
            if (p.messageFailed(m)) tapRetry(m);
            assertTrue(p.waitMessageSent(m, Duration.ofSeconds(40)), "After Retry online the row should be sent — it is " + p.sendStateOf(m));
            assertEquals(1, p.rowsContaining(m), "The retried message must appear exactly once");
        } finally {
            setOffline(false);
            cleanup(p, m);
        }
    }

    @Test
    @Order(41)
    // [WEB M4 #114 #147] (b) (sheet BUL-25)
    @DisplayName("[WEB M4b] An unsent (offline) row can be deleted locally and stays gone after a relaunch (BUL-25)")
    void offlineDeleteUnsentRow() {
        assumeOfflineAllowed();
        HomeChatPage p = onBulletin();
        String m = marker("m4-offdel");
        try {
            setOffline(true);
            p.sendMessage(m);
            assumeTrue(waitFailed(p, m, Duration.ofSeconds(75)), "The offline row never reached the failed state — " + p.sendStateOf(m));
            assumeTrue(p.longPressMessage(m), "Long-press on the failed row failed");
            assertTrue(p.tapMenuOption("Delete"), "A failed row should offer Delete: " + p.menuOptions());
            p.tapDeleteActionBar(Duration.ofSeconds(5));
            boolean blocked = p.textShown("nternet", Duration.ofSeconds(3));
            p.confirmPopup().acceptIfShown(Duration.ofSeconds(4));
            sleep(1500);
            if (p.inSelectionMode(Duration.ofMillis(500))) p.exitSelectionMode();
            boolean gone = !p.messageShown(m, Duration.ofSeconds(2));
            assertTrue(gone && !blocked, "BUL-25: deleting an unsent row offline was refused (\"Internet connection lost\" shown="
                    + blocked + ", row still there=" + !gone + ") — Home.kt:1441 blocks before deleteSelectedUnSendMessage");
            setOffline(false);
            relaunch();
            enterTestProjectIfOnList();
            recover();
            p = onBulletin();
            assertTrue(!p.messageShown(m, Duration.ofSeconds(4)), "The deleted unsent row must not come back after a relaunch");
        } finally {
            setOffline(false);
            recover();
            cleanup(new HomeChatPage(driver), m);
        }
    }

    @Test
    @Order(42)
    // [WEB M3 #148] offline half (sheet BUL-28)
    @DisplayName("[WEB M3b] An offline post during a unit switch ends as one failed row, never duplicated or stuck on the clock")
    void offlinePostDuringUnitSwitch() {
        assumeOfflineAllowed();
        HomeChatPage p = onBulletin();
        String m = marker("m3-offsw");
        try {
            setOffline(true);
            p.typeMessage(m);
            p.tapSend();
            p.selectUnit(callSheet);
            sleep(8000);
            p.selectUnit(bulletin);
            p.revealNewestRow();
            int rows = p.rowsContaining(m);
            assertEquals(1, rows, "BUL-28: the offline post shows " + rows + " rows after the unit switch");
            assertTrue(!p.messagePending(m), "BUL-28: the offline post is stuck on the clock instead of offering Retry");
        } finally {
            setOffline(false);
            if (p.messageFailed(m)) tapRetry(m);
            p.waitMessageSent(m, Duration.ofSeconds(30));
            cleanup(p, m);
        }
    }

    @Test
    @Order(43)
    // [WEB M39 #115] + [WEB M39 #92]
    @DisplayName("[WEB M39] Offline: Bulletin reopens without a crash or an endless loader; an offline reply adds no comment")
    void offlineOpenAndReply() {
        assumeOfflineAllowed();
        HomeChatPage p = onBulletin();
        String base = marker("m39-offbase");
        String reply = marker("m39-offrep");
        try {
            post(p, base);
            setOffline(true);
            p.selectUnit(callSheet);
            p.selectUnit(bulletin);
            sleep(10000);
            assertTrue(appPackage.equals(p.safeCurrentPackage()), "#115: the app should survive opening Bulletin offline");
            assertTrue(!isShown("loaderView") && !isShown("progress"), "#115: the loader should not spin forever offline");
            assertTrue(p.isInputBarVisible(), "#115: the composer should be usable offline");

            p.revealNewestRow();
            p.messageShownScrolled(base, 3);
            assumeTrue(p.longPressMessage(base), "Long-press on the base post failed (offline)");
            assumeTrue(p.tapMenuOption("Reply"), "Reply missing");
            p.sendMessage(reply);
            sleep(5000);
            boolean nested = p.replyUnderRow(base, reply);
            System.out.println(">>> #92 offline reply nested=" + nested);
            assertTrue(!nested, "#92: an offline reply should give an error and add no comment; Android added it under the post");
        } finally {
            setOffline(false);
            p.cancelReplyPreview();
            p.clearMessage();
            cleanup(p, base);
        }
    }

    @Test
    @Order(44)
    // [WEB M35 #37 #38 #136 #40]
    @DisplayName("[WEB M35] A cold start offline lists the cached units without crashing")
    void coldStartOffline() {
        assumeOfflineAllowed();
        try {
            setOffline(true);
            relaunch();
            enterTestProjectIfOnList();
            sleep(8000);
            HomeChatPage p = new HomeChatPage(driver);
            assertTrue(appPackage.equals(p.safeCurrentPackage()), "The app should survive a cold start offline");
            assertTrue(p.isLoaded(), "Home should show the unit strip offline");
            assertTrue(!p.visibleUnitNames().isEmpty(), "The cached units should be listed offline (UnitDbManager)");
        } finally {
            setOffline(false);
            recover();
        }
    }

    // ------------------------------------------------------------------ adb (M7)

    private String udid() {
        String u = Config.get("zillit.udid");
        return u == null ? "" : u.trim();
    }

    private static String adbBinary() {
        for (String var : new String[]{"ANDROID_HOME", "ANDROID_SDK_ROOT"}) {
            String root = System.getenv(var);
            if (root == null || root.isBlank()) continue;
            Path c = Paths.get(root, "platform-tools", "adb");
            if (Files.isExecutable(c)) return c.toString();
        }
        return "adb";
    }

    private boolean adbAvailable() {
        return !adb("get-state").isBlank();
    }

    private String adb(String... args) {
        List<String> cmd = new ArrayList<>(List.of(adbBinary()));
        if (!udid().isEmpty()) { cmd.add("-s"); cmd.add(udid()); }
        cmd.addAll(List.of(args));
        StringBuilder out = new StringBuilder();
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd).redirectErrorStream(true);
            pb.environment().put("ADB_MDNS", "0");
            Process pr = pb.start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(pr.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) out.append(line).append('\n');
            }
            pr.waitFor(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            return "";
        }
        return out.toString();
    }

    /** "Java Heap:" from dumpsys meminfo, in KB; -1 when unreadable. */
    private long javaHeapKb() {
        Matcher mt = Pattern.compile("Java Heap:\\s+(\\d+)").matcher(adb("shell", "dumpsys", "meminfo", appPackage));
        return mt.find() ? Long.parseLong(mt.group(1)) : -1;
    }
}
