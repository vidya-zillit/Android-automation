package com.zillit.automation.tests;

import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.ConfirmPopupPage;
import com.zillit.automation.pages.DocDistAnalyticsPage;
import com.zillit.automation.pages.DocDistComposerPage;
import com.zillit.automation.pages.DocDistDistributionDetailPage;
import com.zillit.automation.pages.DocDistHistoryPage;
import com.zillit.automation.pages.DocDistLibraryPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * DOCUMENT DISTRIBUTION — areas J (History), K (Distribution detail) and L (Analytics)
 * of DOC_DISTRIBUTION_TEST_CASES.md. 28 cases, J-1…J-10, K-1…K-10, L-1…L-8.
 *
 * <p><b>This suite is READ-MOSTLY and it does not send email.</b> Every other content
 * suite seeds the world it needs; this one cannot, because the only way to create a
 * history record is a real send to real addresses (F-25's job, deliberately kept out of a
 * default run — trap 3.5). So it runs against whatever the project already has, and every
 * case is written to mean something BOTH ways:
 *
 * <ul>
 *   <li>with rows, it asserts the real thing;</li>
 *   <li>with an empty history, J-3 still asserts the empty state — and it asserts it even
 *       on a busy project by searching for a string nothing can match, so the case is
 *       never a no-op;</li>
 *   <li>the cases that genuinely need a record {@code assumeTrue} out with a message that
 *       names the missing data, never a bare "skipped".</li>
 * </ul>
 *
 * No case here passes by finding nothing.
 *
 * <p><b>Three writes exist in this area, and this suite performs none of them.</b>
 * Duplicate (K-7) opens a pre-filled composer and is cancelled out of — it never sends.
 * Save-as-list (K-8) would CREATE a distribution list; the flow is driven as far as the
 * name prompt, the prefill is asserted, and the prompt is cancelled, because deleting the
 * preset afterwards belongs to the Lists screen this suite does not own. Export CSV (K-9)
 * stops at the SAF picker rather than writing a file.
 *
 * <p><b>Three findings from the app source contradict the case table</b> — each is
 * asserted in the direction the SOURCE says, with the contradiction spelled out at the
 * case:
 * <ol>
 *   <li><b>J-9 / all of L.</b> ZL-19532 removed Analytics from History. The menu resource
 *       that declares {@code dd_action_analytics} is never inflated and NOTHING navigates
 *       to {@code historyAnalyticsFragment} — it is an orphan in dd_nav_graph. The screen
 *       is therefore unreachable by any tap on a current build, so the L cases skip with
 *       that reason; they are written in full and run unchanged against an older APK that
 *       still has the entry.</li>
 *   <li><b>K-1.</b> {@code dd_message_id} is commented out in both the layout and
 *       {@code render()}. There is no message id on the detail screen; per-recipient ids
 *       moved into the long-press copy menu.</li>
 *   <li><b>K-7.</b> Duplicate does NOT copy recipients — the source says "Recipients
 *       intentionally NOT seeded". The case asserts the subject IS carried and the To
 *       chips are EMPTY.</li>
 * </ol>
 *
 * <p>Trap discipline, all of it learned elsewhere in this module: History is identified by
 * {@code dd_result_count} / the toolbar header and the detail by {@code dd_donut}, never
 * by the nine-screen {@code dd_recycler} (3.3); rows below the fold are absent from the
 * tree AND scrolling appends another server page, so absence is only asserted after a
 * scroll-collect (3.4); both confirmation families go through {@link ConfirmPopupPage}
 * (3.1 / 3.15); the KPI tiles are {@code <include>}s whose child ids collide three ways
 * (3.16); and every case unwinds to the library in a {@code finally} so one failure
 * cannot leave the app parked in a sub-screen (the cascade that took out B-5).
 */
@Order(TestOrder.DOC_DISTRIBUTION_HISTORY)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DocDistHistoryTest extends DocDistBaseTest {

    /**
     * Subject prefixes a QA send leaves behind. J-10 cannot create one — it can only look.
     * {@code QA_DD_SEND_} is what the opt-in send suite (F-25, {@code DocDistSendTest})
     * stamps on its subject; the second is the older hand-run composer wording, kept so a
     * project seeded before the send suite existed still resolves.
     */
    private static final String[] F25_SUBJECT_PREFIXES = {"QA_DD_SEND", "QA DD"};

    /** A query no subject or recipient can match — J-3's lever for the empty state. */
    private static final String NO_MATCH_QUERY = "zzqq-no-such-distribution-9182";

    private static final String NO_HISTORY =
            "This project's Document Distribution history is EMPTY — the case needs at "
            + "least one sent distribution. Nothing in this suite sends (F-25 owns the "
            + "send, and it is opt-in so a routine run does not email real people). Run "
            + "the send suite first, or point the run at a project with history.";

    private static final String ANALYTICS_UNREACHABLE =
            "The Analytics screen has no entry point on this build. ZL-19532 removed the "
            + "Analytics button from History (\"analytics now live INSIDE each individual "
            + "distribution detail\"); menu/dd_history_list.xml still declares "
            + "dd_action_analytics but is never inflated, and no code calls "
            + "navigate(R.id.historyAnalyticsFragment) — the destination is an orphan in "
            + "dd_nav_graph. Area L is therefore unreachable by tapping. The page object "
            + "and these cases are complete: they run as written against an older APK "
            + "that still has the entry, or once a new one is added.";

    /** Remembered from the first case that looked, so later skips can quote the subject. */
    private static String firstSubject = "";

    @BeforeEach
    void reachLibrary() {
        reachLibraryOrSkip();
    }

    // ==================== plumbing ====================

    /** Library → History, settled. Skips (never fails) if the shortcut is not reachable. */
    private DocDistHistoryPage openHistoryOrSkip() {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        // The shortcut lives past the second chip in a horizontally scrolling row, so it
        // is genuinely INVISIBLE until scrolled to — openHistory() handles that (3.10).
        assumeTrue(lib.openHistory(), "The History shortcut is not reachable from the library");
        DocDistHistoryPage history = new DocDistHistoryPage(driver);
        assumeTrue(history.isOpen(Duration.ofSeconds(20)), "The History screen did not open");
        history.waitLoaded(Duration.ofSeconds(45));
        return history;
    }

    /** History → the first row's detail, loaded. Skips with the data reason when empty. */
    private DocDistDistributionDetailPage openFirstDistributionOrSkip(DocDistHistoryPage history) {
        assumeTrue(history.visibleRowCount() > 0, NO_HISTORY);
        firstSubject = history.rowField(1, "dd_subject");
        assumeTrue(history.openRowAt(1), "Could not open the first history row");
        DocDistDistributionDetailPage detail = new DocDistDistributionDetailPage(driver);
        assumeTrue(detail.isOpen(Duration.ofSeconds(20)), "The distribution detail did not open");
        assumeTrue(detail.isLoaded(Duration.ofSeconds(45)),
                "The distribution record never loaded (dd_scroll stayed hidden) for '"
                        + firstSubject + "'");
        return detail;
    }

    private DocDistAnalyticsPage openAnalyticsOrSkip(DocDistHistoryPage history) {
        assumeTrue(history.hasAnalyticsEntry(), ANALYTICS_UNREACHABLE);
        assumeTrue(history.openAnalytics(), "Found an Analytics entry but the tap did not open it");
        DocDistAnalyticsPage analytics = new DocDistAnalyticsPage(driver);
        assumeTrue(analytics.isOpen(Duration.ofSeconds(20)), "The Analytics screen did not open");
        analytics.waitLoaded(Duration.ofSeconds(45));
        return analytics;
    }

    /**
     * Unwinds to the library, whatever went wrong. Called from every {@code finally}: a
     * failure deep in the detail (or worse, in a foreign SAF picker) would otherwise leave
     * every later case running against the wrong screen and turn one red into twenty.
     */
    private void backToLibrary() {
        new ConfirmPopupPage(driver).clearIfBlocking();
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        for (int i = 0; i < 8 && !lib.isLibrary(Duration.ofSeconds(2)); i++) {
            try { driver.navigate().back(); Thread.sleep(900); }
            catch (Exception e) { break; }
        }
        if (lib.isLibrary(Duration.ofSeconds(2))) lib.returnToRoot();
    }

    /** Leaves whatever foreign activity (the SAF picker) a case may have opened. */
    private void leaveForeignActivity() {
        for (int i = 0; i < 6; i++) {
            String pkg;
            try { pkg = driver.getCurrentPackage(); }
            catch (Exception e) { return; }
            if (pkg == null || pkg.equals(appPackage)) return;
            try { driver.navigate().back(); Thread.sleep(900); }
            catch (Exception e) { return; }
        }
    }

    private String currentPackageSafe() {
        try { return String.valueOf(driver.getCurrentPackage()); }
        catch (Exception e) { return ""; }
    }

    // ==================== J · HISTORY ====================

    @Test
    @Order(1)
    @DisplayName("J-1: History opens and reports a result count")
    void historyOpens() {
        DocDistHistoryPage history = openHistoryOrSkip();
        try {
            System.out.println(">>> history header='" + history.headerText()
                    + "' count='" + history.resultCountText()
                    + "' rows=" + history.visibleRowCount()
                    + " empty=" + history.isEmptyStateShown());
            screenshot("dd-history");

            assertTrue(history.isReadable(),
                    "History must settle on rows or the empty state, not a spinner");
            // dd_result_count is GONE until the first response lands (total == null on
            // mount), so it can only be REQUIRED once rows prove a response arrived.
            if (history.visibleRowCount() > 0) {
                assertTrue(history.hasResultCount(),
                        "With rows on screen the server answered, so the '<n> results' header "
                                + "must be shown. Rows=" + history.visibleRowCount());
                assertTrue(history.resultCountValue() >= 0,
                        "The result count should read a number. Got: '"
                                + history.resultCountText() + "'");
            } else {
                assertTrue(history.isEmptyStateShown(),
                        "With no rows the empty state must be up. " + NO_HISTORY);
                System.out.println(">>> J-1: history is empty; the count header is hidden by "
                        + "design until a response carries a total.");
            }
        } finally {
            backToLibrary();
        }
    }

    @Test
    @Order(2)
    @DisplayName("J-2: the result count is consistent with the rows the list holds")
    void resultCountMatchesRows() {
        DocDistHistoryPage history = openHistoryOrSkip();
        try {
            assumeTrue(history.visibleRowCount() > 0, NO_HISTORY);
            int total = history.resultCountValue();
            assumeTrue(total >= 0,
                    "The result count header is not readable ('" + history.resultCountText()
                            + "'), so there is nothing to compare the rows against");

            List<String> rows = history.allRowKeysScrolled();
            System.out.println(">>> J-2: server total=" + total + " collected=" + rows.size());
            screenshot("dd-history-count");

            // The header is the SERVER's total for the query, not "rows on screen"
            // (HistoryListFragment.render). With PAGE_LIMIT = 50 a 300-send project shows
            // "300 results" over 50 loaded rows, so equality is only the rule while the
            // whole result set fits one page. Asserting the RELATIONSHIP is what survives
            // live data.
            assertTrue(rows.size() <= total,
                    "The list can never hold more rows than the server says exist — "
                            + rows.size() + " collected vs a reported total of " + total);
            if (total <= DocDistHistoryPage.PAGE_LIMIT) {
                assertEquals(total, rows.size(),
                        "A result set inside one page (" + DocDistHistoryPage.PAGE_LIMIT
                                + ") must be fully rendered, so the count and the rows agree");
            } else {
                System.out.println(">>> J-2: " + total + " > PAGE_LIMIT, so equality is not "
                        + "expected — the paged relationship is what is asserted (J-7 covers "
                        + "the paging itself).");
            }
        } finally {
            backToLibrary();
        }
    }

    @Test
    @Order(3)
    @DisplayName("J-3: the empty state shows — naturally when there is no history, and on a "
            + "no-match search when there is")
    void emptyStateShows() {
        DocDistHistoryPage history = openHistoryOrSkip();
        try {
            // Written so it asserts on EVERY project rather than passing for free on a busy
            // one: if history already has rows, a query nothing can match drives the same
            // state through the server-side search.
            boolean naturallyEmpty = history.visibleRowCount() == 0;
            if (!naturallyEmpty) {
                assumeTrue(history.search(NO_MATCH_QUERY), "Could not type in the history search box");
            }
            System.out.println(">>> J-3: naturallyEmpty=" + naturallyEmpty
                    + " empty='" + history.emptyText() + "' count='" + history.resultCountText() + "'");
            screenshot("dd-history-empty");

            assertTrue(history.isEmptyStateShown(),
                    (naturallyEmpty ? "An empty history" : "A search matching nothing")
                            + " must show dd_empty. Rows on screen: " + history.visibleRowCount());
            assertEquals(0, history.visibleRowCount(),
                    "Nothing should be listed alongside the empty state");
            if (!naturallyEmpty && history.hasResultCount()) {
                assertEquals(0, history.resultCountValue(),
                        "A no-match search should report zero results. Got: '"
                                + history.resultCountText() + "'");
            }
            if (!naturallyEmpty) history.clearSearch();
        } finally {
            backToLibrary();
        }
    }

    @Test
    @Order(4)
    @DisplayName("J-4: search filters the list by subject")
    void searchFiltersBySubject() {
        DocDistHistoryPage history = openHistoryOrSkip();
        try {
            assumeTrue(history.visibleRowCount() > 0, NO_HISTORY);
            String subject = history.rowField(1, "dd_subject");
            assumeTrue(!subject.isBlank() && !subject.equals("(no subject)"),
                    "The newest distribution has no subject to search for (the adapter renders "
                            + "'(no subject)'), so there is no term to filter on");

            // A whole subject may be ellipsised in the row; the longest word is both a real
            // substring of it and specific enough to filter with.
            String term = longestWord(subject);
            assumeTrue(term.length() >= 3,
                    "Subject '" + subject + "' has no word long enough (3+ chars) to search with");

            assumeTrue(history.search(term), "Could not type in the history search box");
            List<String> hits = history.visibleSubjects();
            System.out.println(">>> J-4: term='" + term + "' → " + hits);
            screenshot("dd-history-search");

            assertFalse(hits.isEmpty(),
                    "Searching '" + term + "', taken from the newest row's own subject, must "
                            + "still match that row");
            // The search is server-side over subject AND recipient (dd_history_search_hint:
            // "Search subject, recipient…"), so a hit whose SUBJECT lacks the term is a
            // legitimate recipient match — assert the term drove the result set, not that
            // every subject contains it.
            assertTrue(hits.stream().anyMatch(s -> s.toLowerCase().contains(term.toLowerCase())),
                    "At least one result must carry the searched term in its subject. Got: " + hits);
            history.clearSearch();
        } finally {
            backToLibrary();
        }
    }

    @Test
    @Order(5)
    @DisplayName("J-5: a row shows subject, status, to, recipients and date")
    void rowShowsAllFields() {
        DocDistHistoryPage history = openHistoryOrSkip();
        try {
            assumeTrue(history.visibleRowCount() > 0, NO_HISTORY);
            String subject = history.rowField(1, "dd_subject");
            String status = history.rowField(1, "dd_status");
            String to = history.rowField(1, "dd_to");
            String recipients = history.rowField(1, "dd_recipients");
            String date = history.rowField(1, "dd_date");
            boolean hasCounts = history.rowHasField(1, "dd_counts");
            String counts = hasCounts ? history.rowField(1, "dd_counts") : "(hidden)";

            System.out.println(">>> J-5 row: subject='" + subject + "' status='" + status
                    + "' to='" + to + "' recipients='" + recipients + "' date='" + date
                    + "' counts='" + counts + "'");
            screenshot("dd-history-row");

            assertFalse(subject.isBlank(),
                    "dd_subject is always bound — blank subjects render '(no subject)'");
            assertFalse(status.isBlank(), "dd_status carries the Sent / Queued / Failed pill");
            assertTrue(to.startsWith("To:"),
                    "dd_to is formatted 'To: <addresses>' (R.string.dd_to_line). Got: '" + to + "'");
            assertTrue(recipients.matches("(?s).*\\d+.*"),
                    "dd_recipients reads '<n> recipients'. Got: '" + recipients + "'");
            assertFalse(date.isBlank(), "dd_date carries the send timestamp");
            // dd_counts is the ONE field that is legitimately absent: bindCounts() hides it
            // when nothing is delivered, opened or failed yet. Report which case this row is
            // rather than failing an honest render.
            if (hasCounts) {
                assertFalse(counts.isBlank(),
                        "A visible dd_counts must carry text — it is hidden, not blanked, "
                                + "when there is nothing to show");
            } else {
                System.out.println(">>> J-5: dd_counts hidden — this send has nothing delivered, "
                        + "opened or failed yet (HistoryListAdapter.bindCounts).");
            }
        } finally {
            backToLibrary();
        }
    }

    @Test
    @Order(6)
    @DisplayName("J-6: swipe-to-refresh reloads and the list survives")
    void swipeRefreshReloads() {
        DocDistHistoryPage history = openHistoryOrSkip();
        try {
            int before = history.visibleRowCount();
            boolean wasEmpty = history.isEmptyStateShown();
            assumeTrue(history.pullToRefresh(), "dd_swipe is not present, so there is nothing to pull");
            assertTrue(history.waitLoaded(Duration.ofSeconds(45)),
                    "The list must settle again after a refresh");
            int after = history.visibleRowCount();
            System.out.println(">>> J-6: rows " + before + " → " + after
                    + " (empty before=" + wasEmpty + " after=" + history.isEmptyStateShown() + ")");
            screenshot("dd-history-refreshed");

            assertTrue(history.isReadable(),
                    "After a refresh the list must be readable — rows or the empty state");
            // refresh() re-fetches page 0, so a list that had been paged out to 150 rows comes
            // back as 50. Absolute counts are meaningless here; what must hold is that a
            // project with history still HAS history.
            if (!wasEmpty) {
                assertTrue(after > 0,
                        "A refresh must not empty a list that had " + before + " rows");
                assertFalse(history.isEmptyStateShown(),
                        "The empty state must not appear on a project that has distributions");
            }
        } finally {
            backToLibrary();
        }
    }

    @Test
    @Order(7)
    @DisplayName("J-7: scrolling to the bottom pages in more rows")
    void scrollingPagesInMore() {
        DocDistHistoryPage history = openHistoryOrSkip();
        try {
            assumeTrue(history.visibleRowCount() > 0, NO_HISTORY);
            int total = history.resultCountValue();
            assumeTrue(total > DocDistHistoryPage.PAGE_LIMIT,
                    "This project has " + total + " distribution(s); pagination only exists "
                            + "above HistoryListViewModel.PAGE_LIMIT = "
                            + DocDistHistoryPage.PAGE_LIMIT + ", so there is no second page "
                            + "to load. J-7 needs a project with more than "
                            + DocDistHistoryPage.PAGE_LIMIT + " sends.");

            // The spinner is a race by nature — the fragment pre-fetches 8 rows before the
            // end, so a quick response can repaint before any poll sees dd_paging_progress.
            // The ROWS are the assertion; the spinner is reported, never required.
            boolean spinnerSeen = history.scrollAndObservePaging(12);
            List<String> rows = history.allRowKeysScrolled(60);
            System.out.println(">>> J-7: total=" + total + " collected=" + rows.size()
                    + " pagingSpinnerObserved=" + spinnerSeen);
            screenshot("dd-history-paged");

            assertTrue(rows.size() > DocDistHistoryPage.PAGE_LIMIT,
                    "Scrolling past the first page must append the next one: expected more than "
                            + DocDistHistoryPage.PAGE_LIMIT + " rows out of a reported " + total
                            + ", collected " + rows.size());
            assertTrue(rows.size() <= total,
                    "Paging must never produce more rows than the server's total (" + total + ")");
        } finally {
            backToLibrary();
        }
    }

    @Test
    @Order(8)
    @DisplayName("J-8: tapping a row opens that distribution's detail")
    void rowOpensDetail() {
        DocDistHistoryPage history = openHistoryOrSkip();
        try {
            assumeTrue(history.visibleRowCount() > 0, NO_HISTORY);
            String subject = history.rowField(1, "dd_subject");
            assumeTrue(history.openRowAt(1), "Could not tap the first history row");

            DocDistDistributionDetailPage detail = new DocDistDistributionDetailPage(driver);
            assertTrue(detail.isOpen(Duration.ofSeconds(20)),
                    "Tapping a row must open the distribution detail (dd_duplicate_btn)");
            assertTrue(detail.isLoaded(Duration.ofSeconds(45)),
                    "The record must load — dd_scroll stays GONE until it does");
            String opened = detail.subject();
            System.out.println(">>> J-8: row '" + subject + "' → detail '" + opened + "'");
            screenshot("dd-detail-opened");

            // Compared with containment rather than equality: the row's subject is
            // single-line and ellipsised, and while the accessibility node normally carries
            // the full string, a device that reports the TRANSFORMED text would fail an
            // equality check on a distribution that was opened perfectly correctly.
            assertTrue(opened.equals(subject)
                            || opened.startsWith(stripEllipsis(subject))
                            || subject.startsWith(stripEllipsis(opened)),
                    "The detail must be the distribution that was tapped — row '" + subject
                            + "' opened '" + opened + "'");
        } finally {
            backToLibrary();
        }
    }

    @Test
    @Order(9)
    @DisplayName("J-9: the Analytics entry — removed from History by ZL-19532")
    void analyticsEntryOnHistory() {
        DocDistHistoryPage history = openHistoryOrSkip();
        try {
            boolean entry = history.hasAnalyticsEntry();
            System.out.println(">>> J-9: analytics entry on History = " + entry);
            screenshot("dd-history-analytics-entry");

            // The case table says "the overflow offers Analytics"; the current source says
            // otherwise and says so explicitly (fragment comment + an uninflated
            // menu/dd_history_list.xml + an orphaned nav destination). Both branches ASSERT
            // — this never passes by finding nothing.
            if (entry) {
                System.out.println(">>> J-9: this build PREDATES ZL-19532 — following the entry.");
                assertTrue(history.openAnalytics(), "The Analytics entry did not open anything");
                DocDistAnalyticsPage analytics = new DocDistAnalyticsPage(driver);
                assertTrue(analytics.isOpen(Duration.ofSeconds(20)),
                        "The Analytics entry must land on the analytics screen");
            } else {
                // Assert the absence ON the right screen — "no Analytics button" would be
                // trivially true anywhere else in the app.
                assertTrue(history.isOpen(Duration.ofSeconds(5)),
                        "The absence must be observed on History itself, not on whatever screen "
                                + "a stray back-press landed on");
                assertFalse(entry,
                        "ZL-19532 removed Analytics from History; per-record analytics live on "
                                + "the distribution detail instead. CONTRADICTS the case table's "
                                + "J-9 (\"the overflow offers Analytics\").");
            }
        } finally {
            backToLibrary();
        }
    }

    @Test
    @Order(10)
    @DisplayName("J-10: a distribution sent by F-25 is listed here")
    void sentDistributionAppears() {
        DocDistHistoryPage history = openHistoryOrSkip();
        try {
            // J-10 is the one case in this area that is about a SEND, and this suite must not
            // send (trap 3.5 — a send is real email to real addresses). F-25 owns the write
            // half and asserts it inline in DocDistSendTest; all this case can honestly do is
            // look for the subject that suite leaves behind, and say precisely what was
            // missing when there is none.
            String matched = null;
            List<String> hits = List.of();
            for (String prefix : F25_SUBJECT_PREFIXES) {
                assumeTrue(history.search(prefix), "Could not type in the history search box");
                hits = history.visibleSubjects();
                System.out.println(">>> J-10: '" + prefix + "' → " + hits);
                if (hits.stream().anyMatch(s -> s.startsWith(prefix))) { matched = prefix; break; }
            }
            screenshot("dd-history-f25");

            assumeTrue(matched != null,
                    "No distribution whose subject starts with any of "
                            + String.join(" / ", F25_SUBJECT_PREFIXES) + " is in this project's "
                            + "history. F-25 (the opt-in send suite, DocDistSendTest) is what "
                            + "creates one — this suite deliberately never sends, so J-10 can "
                            + "only VERIFY a send that already happened. Run the send suite in "
                            + "the same project first. Last search returned: " + hits);

            final String found = matched;
            assertTrue(hits.stream().anyMatch(s -> s.startsWith(found)),
                    "The search matched rows, but none carries the '" + found
                            + "' subject prefix — the hits look like recipient matches: " + hits);
            assertTrue(history.hasResultCount() && history.resultCountValue() >= 1,
                    "A matched send must be counted in the header. Got: '"
                            + history.resultCountText() + "'");
            history.clearSearch();
        } finally {
            backToLibrary();
        }
    }

    // ==================== K · DISTRIBUTION DETAIL ====================

    @Test
    @Order(11)
    @DisplayName("K-1: the detail shows subject and date (there is NO message id on this build)")
    void detailHeaderFields() {
        DocDistHistoryPage history = openHistoryOrSkip();
        try {
            DocDistDistributionDetailPage detail = openFirstDistributionOrSkip(history);
            System.out.println(">>> K-1: subject='" + detail.subject() + "' date='" + detail.date()
                    + "' messageIdPresent=" + detail.hasMessageId());
            screenshot("dd-detail-header");

            assertFalse(detail.subject().isBlank(),
                    "dd_subject is always bound — a blank subject renders '(no subject)'");
            assertFalse(detail.date().isBlank(),
                    "dd_date joins the absolute and relative timestamps");
            // render(): setDdToolbarHeader(ddSubject.text). Compared as a prefix because the
            // header is a single-line TextView in the shared toolbar and a long subject is
            // ellipsised there.
            assertTrue(stripEllipsis(detail.headerText()).startsWith(
                            detail.subject().substring(0, Math.min(12, detail.subject().length())))
                            || detail.headerText().equals(detail.subject()),
                    "render() pushes the subject into the shared toolbar header — subject='"
                            + detail.subject() + "' header='" + detail.headerText() + "'");

            // The case table lists dd_message_id under "Distribution detail". It is
            // COMMENTED OUT — both the TextView in fragment_dd_distribution_detail.xml and
            // the binding.ddMessageId block in render(). Per-recipient message ids moved to
            // the row long-press copy menu (menu/dd_recipient_copy). Asserting its absence
            // is what keeps this finding from quietly rotting.
            assertFalse(detail.hasMessageId(),
                    "dd_message_id is commented out in the layout and in render() — CONTRADICTS "
                            + "the case table's K-1 and the §2 identifier list. If it is back, "
                            + "the app changed and both should be updated.");
        } finally {
            backToLibrary();
        }
    }

    @Test
    @Order(12)
    @DisplayName("K-2: the donut and its six-row legend render")
    void donutAndLegend() {
        DocDistHistoryPage history = openHistoryOrSkip();
        try {
            DocDistDistributionDetailPage detail = openFirstDistributionOrSkip(history);
            List<String> legend = detail.legendTexts();
            System.out.println(">>> K-2: donut=" + detail.donutShown() + " legend=" + legend);
            screenshot("dd-detail-donut");

            assertTrue(detail.donutShown(), "dd_donut must render once the record has loaded");
            assertTrue(detail.legendShown(), "dd_legend must render beside it");

            // buildLegend() walks a FIXED legendOrder, so all six rows exist whatever the
            // data — this is a real assertion on an empty-ish record, not a data-dependent one.
            for (String label : DocDistDistributionDetailPage.LEGEND_LABELS) {
                assertTrue(legend.stream().anyMatch(t -> t.equalsIgnoreCase(label)),
                        "The legend always renders all six statuses; '" + label
                                + "' is missing from " + legend);
            }
            // Delivered = accepted + opened (ZL-19731), so Delivered can never be the smaller
            // of the two. The legend's numbers deliberately do NOT sum to the recipient total.
            int opened = detail.legendCount("Opened");
            int delivered = detail.legendCount("Delivered");
            System.out.println(">>> K-2: opened=" + opened + " delivered=" + delivered);
            if (opened >= 0 && delivered >= 0) {
                assertTrue(delivered >= opened,
                        "Opened ⊂ Delivered — the legend counts accepted + opened as Delivered, "
                                + "so Delivered (" + delivered + ") cannot be below Opened ("
                                + opened + ")");
            }
        } finally {
            backToLibrary();
        }
    }

    @Test
    @Order(13)
    @DisplayName("K-3: recipients are listed, each with an address and a status")
    void recipientsWithStatus() {
        DocDistHistoryPage history = openHistoryOrSkip();
        try {
            DocDistDistributionDetailPage detail = openFirstDistributionOrSkip(history);
            String subject = detail.subject();   // read before scrolling it off screen
            detail.scrollToRecipients();
            List<String> emails = detail.recipientEmails();
            List<String> statuses = detail.recipientStatuses();
            System.out.println(">>> K-3: " + emails.size() + " recipient rows " + emails
                    + " statuses=" + statuses);
            screenshot("dd-detail-recipients");

            assertFalse(emails.isEmpty(),
                    "A distribution always has at least one recipient — none rendered under "
                            + "dd_recipients for '" + subject + "'");
            assertEquals(emails.size(), statuses.size(),
                    "Every recipient row binds a status pill (StatusPills.apply)");
            assertTrue(emails.stream().allMatch(e -> e.contains("@")),
                    "Each row's dd_email carries the address. Got: " + emails);
            assertTrue(statuses.stream().noneMatch(String::isBlank),
                    "Each row's status pill carries a label. Got: " + statuses);
        } finally {
            backToLibrary();
        }
    }

    @Test
    @Order(14)
    @DisplayName("K-4: attachments are listed when the send carried any")
    void attachmentsListed() {
        DocDistHistoryPage history = openHistoryOrSkip();
        try {
            DocDistDistributionDetailPage detail = openFirstDistributionOrSkip(history);
            boolean has = detail.hasAttachments();
            System.out.println(">>> K-4: attachments=" + has + " text='" + detail.attachmentsText() + "'");
            screenshot("dd-detail-attachments");

            assumeTrue(has,
                    "The newest distribution ('" + detail.subject() + "') was sent with NO "
                            + "documents, so dd_attachments is View.GONE by design. K-4 needs a "
                            + "send that carried at least one attachment — F-25 with an attached "
                            + "library document produces one.");

            String text = detail.attachmentsText();
            assertFalse(text.isBlank(), "A visible attachments line must carry text");
            // render() builds "<n> attachments: a.pdf, b.pdf" — a number, then the names.
            assertTrue(text.matches("(?s).*\\d+.*"),
                    "The line is formatted '<n> attachments: <names>'. Got: '" + text + "'");
            assertTrue(text.contains(":"),
                    "The names follow a colon (render(): \"$line: $names\"). Got: '" + text + "'");
        } finally {
            backToLibrary();
        }
    }

    @Test
    @Order(15)
    @DisplayName("K-5: presets used are listed, or absent cleanly")
    void presetsUsedOrAbsentCleanly() {
        DocDistHistoryPage history = openHistoryOrSkip();
        try {
            DocDistDistributionDetailPage detail = openFirstDistributionOrSkip(history);
            // Read the line BEFORE scrolling: it sits between the donut and the recipients
            // table, so scrolling to the table first would take it off screen and read as
            // "this send used no list" on a send that plainly did.
            boolean has = detail.hasPresetsUsed();
            String presetsText = detail.presetsUsedText();
            detail.scrollToRecipients();
            int tags = detail.presetTagCount();
            System.out.println(">>> K-5: presetsUsed=" + has + " text='" + presetsText
                    + "' recipientTags=" + tags);
            screenshot("dd-detail-presets");

            // "Either is valid" — but the two halves of ZL-20299 must AGREE, and that is
            // assertable whichever way the data falls. The line and the per-recipient pills
            // come from the same presetsUsed list, so one without the other is a bug.
            if (has) {
                assertFalse(presetsText.isBlank(), "A visible presets line must carry the list names");
                assertTrue(presetsText.startsWith("Distribution List"),
                        "The line reads 'Distribution List(s): <names>' "
                                + "(R.string.dd_presets_used_line). Got: '" + presetsText + "'");
                assertTrue(tags > 0,
                        "A send that used a distribution list tags the recipients it "
                                + "contributed, but no dd_preset_tag pill rendered");
            } else {
                assertEquals(0, tags,
                        "With no presets line, setPresetTagLookup(emptyMap()) wipes the tags — "
                                + tags + " stale pill(s) rendered instead");
                System.out.println(">>> K-5: this send applied no distribution list; the line is "
                        + "hidden and no recipient carries a tag — the clean absence.");
            }
        } finally {
            backToLibrary();
        }
    }

    @Test
    @Order(16)
    @DisplayName("K-6: Refresh reloads the detail without losing it")
    void refreshReloadsDetail() {
        DocDistHistoryPage history = openHistoryOrSkip();
        try {
            DocDistDistributionDetailPage detail = openFirstDistributionOrSkip(history);
            String before = detail.subject();
            int recipientsBefore = detail.recipientCount();

            assertTrue(detail.hasRefresh(), "dd_refresh_btn is part of the detail's action row");
            assertTrue(detail.tapRefresh(), "Could not tap Refresh");
            // refresh() → load() flips isLoading, but render() only shows the spinner while
            // record == null — on a reload the record is still there, so the screen never
            // blanks. Give the round trip time, then assert it is intact.
            sleep(3000);
            assertTrue(detail.isLoaded(Duration.ofSeconds(30)),
                    "The detail must still be rendered after a refresh");
            System.out.println(">>> K-6: '" + before + "' → '" + detail.subject() + "' recipients "
                    + recipientsBefore + " → " + detail.recipientCount());
            screenshot("dd-detail-refreshed");

            assertEquals(before, detail.subject(),
                    "A refresh reloads the SAME distribution");
            assertTrue(detail.donutShown(), "The donut must survive the reload");
        } finally {
            backToLibrary();
        }
    }

    @Test
    @Order(17)
    @DisplayName("K-7: Duplicate opens a pre-filled composer — subject carried, recipients NOT")
    void duplicateOpensPrefilledComposer() {
        DocDistHistoryPage history = openHistoryOrSkip();
        try {
            DocDistDistributionDetailPage detail = openFirstDistributionOrSkip(history);
            String subject = detail.subject();
            assertTrue(detail.hasDuplicate(), "dd_duplicate_btn is always visible (the QA flip)");
            assertTrue(detail.isDuplicateEnabled(),
                    "Duplicate is enabled once the record has loaded (render(): "
                            + "isEnabled = state.record != null)");
            assertTrue(detail.tapDuplicate(), "Could not tap Duplicate");

            // The click runs inside runWithPostingRights, so a view-only user gets the
            // request-permission dialog instead of the composer. That is correct behaviour,
            // not a failure — clear it and skip (trap 3.6).
            if (detail.rightsDialogShown()) {
                String text = detail.confirmPopup().text();
                detail.confirmPopup().clearIfBlocking();
                assumeTrue(false,
                        "Duplicate hit the posting-rights gate for this user — the app raised "
                                + "its request-permission dialog ('" + text + "') instead of "
                                + "opening the composer. K-7 needs an account with canPost.");
            }

            DocDistComposerPage composer = new DocDistComposerPage(driver);
            assertTrue(composer.isOpen(Duration.ofSeconds(30)),
                    "Duplicate navigates to the composer once hydration settles "
                            + "(prepareDuplicate re-fetches every attachment first)");
            String prefilled = composer.subject();
            int toChips = composer.chipCount("dd_to_chips");
            System.out.println(">>> K-7: subject='" + subject + "' prefilled='" + prefilled
                    + "' toChips=" + toChips + " attachments=" + composer.attachmentNames());
            screenshot("dd-duplicate-composer");

            assertEquals(subject, prefilled,
                    "The composer opens pre-filled with the original subject "
                            + "(ARG_INITIAL_SUBJECT)");
            // The case table says "same subject/recipients". The source disagrees, in as many
            // words: "Recipients intentionally NOT seeded — the whole point of Duplicate is
            // 'send to someone new'." Assert what the app promises.
            assertEquals(0, toChips,
                    "Duplicate must NOT seed recipients (onDuplicateReady: \"Recipients "
                            + "intentionally NOT seeded\") — CONTRADICTS the case table's K-7 "
                            + "\"same subject/recipients\"");

            // NOTHING is sent here. Cancel is goBack() → popBackStack, no discard prompt on
            // this build (settled by the composer suite's F-26).
            assertTrue(composer.tapCancel(), "Could not cancel out of the duplicated composer");
            sleep(1500);
            assertFalse(composer.isOpen(Duration.ofSeconds(3)),
                    "Cancel must leave the composer — an open composer would block every "
                            + "later case");
        } finally {
            backToLibrary();
        }
    }

    @Test
    @Order(18)
    @DisplayName("K-8: Save-as-list offers a pre-filled name prompt (cancelled — no list is created)")
    void saveAsListPrompt() {
        DocDistHistoryPage history = openHistoryOrSkip();
        try {
            DocDistDistributionDetailPage detail = openFirstDistributionOrSkip(history);
            String subject = detail.subject();
            assertTrue(detail.hasSaveAsList(), "dd_save_as_list_btn is part of the action row");
            assertTrue(detail.isSaveAsListEnabled(),
                    "Save-as-list enables once the record has at least one recipient "
                            + "(render(): recipients/cc/bcc non-empty)");
            assertTrue(detail.tapSaveAsList(), "Could not tap Save as list");

            if (detail.rightsDialogShown()) {
                String text = detail.confirmPopup().text();
                detail.confirmPopup().clearIfBlocking();
                assumeTrue(false,
                        "Save-as-list hit the posting-rights gate — the app raised its "
                                + "request-permission dialog ('" + text + "'). K-8 needs an "
                                + "account with canPost.");
            }

            assertTrue(detail.saveListPromptShown(),
                    "Save-as-list must prompt for a name ('"
                            + DocDistDistributionDetailPage.SAVE_LIST_PROMPT_TITLE + "')");
            String prefill = detail.saveListPrefill();
            System.out.println(">>> K-8: prefill='" + prefill + "' (subject='" + subject + "')");
            screenshot("dd-detail-save-as-list");

            // defaultListName() = "<subject> — recipients", or "New distribution list" when the
            // send had no subject.
            assertFalse(prefill.isBlank(), "The prompt is pre-filled, never empty");
            assertTrue(prefill.startsWith(subject) || prefill.equals("New distribution list"),
                    "The prompt pre-fills '<subject> — recipients' (defaultListName()). "
                            + "Got: '" + prefill + "' for subject '" + subject + "'");

            // ─────────────────────────────────────────────────────────────────────────────
            // The case table's K-8 ends "appears under Lists" — i.e. it CREATES a preset.
            // This suite stops at the prompt on purpose. Pressing Save would POST /presets
            // and leave a distribution list behind in a real project, and deleting it again
            // means driving the Lists screen (presetOverviewFragment), which belongs to the
            // G suite and is not modelled here — a create this suite cannot reliably undo is
            // worse than a case that stops one tap short. Everything up to the write is
            // asserted: the button, its enabled gate, the prompt, and the prefill contract.
            // When the G suite lands, this case can press Save and hand the cleanup to it.
            // ─────────────────────────────────────────────────────────────────────────────
            assertTrue(detail.cancelSaveListPrompt(),
                    "Cancel must dismiss the prompt — it is modal, so leaving it up would "
                            + "take out every later case (trap 3.1)");
            assertFalse(detail.saveListPromptShown(),
                    "No distribution list is created by this suite; the prompt is gone");
        } finally {
            backToLibrary();
        }
    }

    @Test
    @Order(19)
    @DisplayName("K-9: Export CSV reaches the system save dialog")
    void exportCsvReachesSaf() {
        DocDistHistoryPage history = openHistoryOrSkip();
        try {
            DocDistDistributionDetailPage detail = openFirstDistributionOrSkip(history);
            assertTrue(detail.hasExportCsv(), "dd_export_csv_btn is part of the action row");
            assertTrue(detail.isExportCsvEnabled(),
                    "Export CSV enables once the record has loaded");
            assertTrue(detail.tapExportCsv(), "Could not tap Export CSV");
            sleep(2500);

            // Export gates on canDownload (runWithDownloadRights), so the honest outcome is
            // ONE OF: the request-permission dialog, or the SAF CreateDocument picker. Both
            // are correct; which one depends on the account.
            boolean rights = detail.rightsDialogShown();
            String pkg = currentPackageSafe();
            boolean saf = pkg.contains("documentsui") || pkg.contains("packageinstaller")
                    || !pkg.equals(appPackage);
            System.out.println(">>> K-9: rightsDialog=" + rights + " package='" + pkg + "'");
            screenshot("dd-detail-export-csv");

            if (rights) {
                String text = detail.confirmPopup().text();
                detail.confirmPopup().clearIfBlocking();
                assumeTrue(false,
                        "Export CSV hit the DOWNLOAD-rights gate for this user ('" + text
                                + "'). That is the documented behaviour for an account without "
                                + "canDownload; K-9 needs one that has it.");
            }

            assertTrue(saf,
                    "Export CSV launches ActivityResultContracts.CreateDocument, so the system "
                            + "save dialog must come up. Still in '" + pkg + "'.");

            // ─────────────────────────────────────────────────────────────────────────────
            // The boundary, stated honestly: this case asserts the export STARTS — the bytes
            // are built in-process (DistributionRecipientsCsvBuilder, no backend call) and
            // the picker is launched with the right filename. It does NOT complete the save.
            // Finishing it means driving a foreign package whose Save button, folder chrome
            // and confirmation differ by OEM and Android version, and it writes a file to the
            // device that nothing here can clean up. "The picker opened" is the last step
            // this suite can assert without either flaking or littering.
            // ─────────────────────────────────────────────────────────────────────────────
        } finally {
            leaveForeignActivity();
            backToLibrary();
        }
    }

    @Test
    @Order(20)
    @DisplayName("K-10: a failed distribution explains itself; a sent one shows no error banner")
    void errorStateHandled() {
        DocDistHistoryPage history = openHistoryOrSkip();
        try {
            assumeTrue(history.visibleRowCount() > 0, NO_HISTORY);

            // renderErrorBanner(): dd_error is VISIBLE for FAILED records and for QUEUED ones
            // carrying an error, GONE for SENT. So look for a failed row first — and when
            // there is none, assert the other half of the same rule on a sent one. Both
            // branches assert; neither passes by finding nothing.
            int failedRow = -1;
            for (int pass = 0; pass < 6 && failedRow < 0; pass++) {
                failedRow = history.indexOfRowWithStatus("Failed");
                if (failedRow < 0) history.scrollDown();
            }
            boolean failedFound = failedRow > 0;
            if (!failedFound) {
                history.scrollToTop();
                failedRow = 1;
            }
            String rowSubject = history.rowField(failedRow, "dd_subject");
            String rowStatus = history.rowField(failedRow, "dd_status");
            assumeTrue(history.openRowAt(failedRow), "Could not open the history row to inspect");

            DocDistDistributionDetailPage detail = new DocDistDistributionDetailPage(driver);
            assumeTrue(detail.isOpen(Duration.ofSeconds(20)), "The distribution detail did not open");
            assumeTrue(detail.isLoaded(Duration.ofSeconds(45)),
                    "The record never loaded for '" + rowSubject + "'");
            System.out.println(">>> K-10: row='" + rowSubject + "' status='" + rowStatus
                    + "' errorBanner=" + detail.hasErrorBanner()
                    + " text='" + detail.errorText() + "'");
            screenshot("dd-detail-error");

            if (failedFound) {
                assertTrue(detail.hasErrorBanner(),
                        "A FAILED distribution must surface dd_error (ZL-19446)");
                assertFalse(detail.errorText().isBlank(),
                        "The banner carries a SENTENCE, not a code — DistributionErrorMessages"
                                + ".resolve() maps the wire value to readable text");
                // Reported, not asserted: the banner is a TextView. There is no retry, resend
                // or dismiss control anywhere in the layout or the fragment, so the case
                // table's "offers recovery" overstates what the app does — the recovery
                // available to the user is Duplicate, which is a separate control.
                System.out.println(">>> K-10: dd_error is TEXT ONLY — no retry/resend affordance "
                        + "exists on this screen; the recovery path is the Duplicate button.");
            } else {
                assertFalse(detail.hasErrorBanner(),
                        "This project has no failed distribution, and a '" + rowStatus
                                + "' one must NOT show the error banner — renderErrorBanner() "
                                + "hides it for anything that is not FAILED or QUEUED-with-error");
                System.out.println(">>> K-10: no FAILED distribution exists in this project, so "
                        + "the positive half (banner text for a failure) was not exercised. It "
                        + "needs a send that the SMTP layer rejects — an over-cap attachment "
                        + "set or an undeliverable address.");
            }
        } finally {
            backToLibrary();
        }
    }

    // ==================== L · ANALYTICS ====================
    //
    // Every case here reaches the screen through openAnalyticsOrSkip(), which is where the
    // ZL-19532 finding bites: on a current build there is NO tap that leads to
    // historyAnalyticsFragment, so all eight skip with that exact reason rather than
    // pretending. They are written against the fragment in full and run unchanged the
    // moment an entry point exists (or against an older APK that still has one).

    @Test
    @Order(21)
    @DisplayName("L-1: Analytics opens on the default 30-day range")
    void analyticsDefaultRange() {
        DocDistHistoryPage history = openHistoryOrSkip();
        try {
            DocDistAnalyticsPage analytics = openAnalyticsOrSkip(history);
            System.out.println(">>> L-1: unavailable=" + analytics.isUnavailable()
                    + " range30checked=" + analytics.isRangeChecked("dd_range_30")
                    + " sent='" + analytics.kpiValue(DocDistAnalyticsPage.KPI_SENT) + "'");
            screenshot("dd-analytics");

            assumeTrue(!analytics.isUnavailable(),
                    "Analytics answered 'unavailable' for this project (any failure, including "
                            + "a 404 on an older deployment, takes that path) — L-7 asserts that "
                            + "state; the rest of L needs a working endpoint.");
            assertTrue(analytics.hasRangeToggle(), "dd_range must offer the range buttons");
            assertTrue(analytics.hasKpiTile(DocDistAnalyticsPage.KPI_SENT),
                    "dd_kpi_sent must be present on the analytics screen");
            // onViewCreated: binding.ddRange.check(R.id.dd_range_30) — 30 days is the default,
            // and AnalyticsViewModel's init selects the same range.
            assertTrue(analytics.isRangeChecked("dd_range_30"),
                    "The screen opens on Last 30 days (ddRange.check(R.id.dd_range_30))");
        } finally {
            backToLibrary();
        }
    }

    @Test
    @Order(22)
    @DisplayName("L-2: the 7 / 30 / 90-day ranges switch and the KPIs re-render")
    void rangesSwitch() {
        DocDistHistoryPage history = openHistoryOrSkip();
        try {
            DocDistAnalyticsPage analytics = openAnalyticsOrSkip(history);
            assumeTrue(!analytics.isUnavailable(), "Analytics is unavailable for this project (L-7)");

            String[][] ranges = {
                    {"dd_range_7", "Last 7 days"},
                    {"dd_range_90", "Last 90 days"},
                    {"dd_range_30", "Last 30 days"},
            };
            for (String[] range : ranges) {
                boolean tapped = "dd_range_7".equals(range[0]) ? analytics.selectRange7()
                        : "dd_range_90".equals(range[0]) ? analytics.selectRange90()
                        : analytics.selectRange30();
                assertTrue(tapped, "Could not tap " + range[1]);
                assertTrue(analytics.waitLoaded(Duration.ofSeconds(40)),
                        range[1] + " never settled — the fetch did not answer");
                System.out.println(">>> L-2: " + range[1] + " checked="
                        + analytics.isRangeChecked(range[0])
                        + " sent='" + analytics.kpiValue(DocDistAnalyticsPage.KPI_SENT)
                        + "' opened='" + analytics.kpiValue(DocDistAnalyticsPage.KPI_OPENED) + "'");

                assertTrue(analytics.isRangeChecked(range[0]),
                        range[1] + " must become the checked range (singleSelection toggle group)");
                // The VALUES are not asserted to change: a quiet project can legitimately
                // report the same totals for 7 and 90 days. What must hold is that each range
                // re-queries and re-renders rather than blanking the tiles.
                assertFalse(analytics.kpiValue(DocDistAnalyticsPage.KPI_SENT).isBlank(),
                        "The Sent tile must still carry a value after switching to " + range[1]);
            }
            screenshot("dd-analytics-ranges");
        } finally {
            backToLibrary();
        }
    }

    @Test
    @Order(23)
    @DisplayName("L-3: Custom opens the date-range picker")
    void customRangeOpensPicker() {
        DocDistHistoryPage history = openHistoryOrSkip();
        try {
            DocDistAnalyticsPage analytics = openAnalyticsOrSkip(history);
            assumeTrue(!analytics.isUnavailable(), "Analytics is unavailable for this project (L-7)");

            assertTrue(analytics.selectRangeCustom(), "Could not tap Custom");
            boolean picker = analytics.datePickerShown(Duration.ofSeconds(12));
            System.out.println(">>> L-3: date picker shown = " + picker);
            screenshot("dd-analytics-custom");

            assertTrue(picker,
                    "Custom opens MaterialDatePicker.Builder.dateRangePicker() — showCustomRange()");

            // ─────────────────────────────────────────────────────────────────────────────
            // The boundary: this case asserts the picker OPENS, then dismisses it. Applying a
            // range means tapping two day cells in a Material calendar grid whose cells carry
            // no ids, whose labels are locale-formatted, and which starts on a month that
            // depends on today's date — a tap sequence that would be guesswork and would go
            // stale on the first calendar change. selectRange(CUSTOM) also short-circuits
            // (it returns early and waits for setCustomRange), so nothing is fetched until
            // two dates are picked; there is no intermediate state worth asserting.
            // ─────────────────────────────────────────────────────────────────────────────
            assertTrue(analytics.dismissDatePicker(), "Could not dismiss the date picker");
            // Put the screen back on a real range: the toggle stays on Custom after a cancel
            // and no fetch ever ran, so a later case would read stale tiles.
            assertTrue(analytics.selectRange30(), "Could not restore the 30-day range");
            assertTrue(analytics.waitLoaded(Duration.ofSeconds(40)),
                    "The 30-day range must reload after the picker was cancelled");
        } finally {
            backToLibrary();
        }
    }

    @Test
    @Order(24)
    @DisplayName("L-4: the three KPI tiles render with their labels and values")
    void threeKpisRender() {
        DocDistHistoryPage history = openHistoryOrSkip();
        try {
            DocDistAnalyticsPage analytics = openAnalyticsOrSkip(history);
            assumeTrue(!analytics.isUnavailable(), "Analytics is unavailable for this project (L-7)");

            String sentLabel = analytics.kpiLabel(DocDistAnalyticsPage.KPI_SENT);
            String openedLabel = analytics.kpiLabel(DocDistAnalyticsPage.KPI_OPENED);
            String rateLabel = analytics.kpiLabel(DocDistAnalyticsPage.KPI_RATE);
            String sent = analytics.kpiValue(DocDistAnalyticsPage.KPI_SENT);
            String opened = analytics.kpiValue(DocDistAnalyticsPage.KPI_OPENED);
            String rate = analytics.kpiValue(DocDistAnalyticsPage.KPI_RATE);
            System.out.println(">>> L-4: [" + sentLabel + "=" + sent + "] [" + openedLabel + "="
                    + opened + "] [" + rateLabel + "=" + rate + "]");
            screenshot("dd-analytics-kpis");

            // Each value is read XPath-anchored on its own include id — dd_kpi_value exists
            // three times over, so an unanchored read would return the same tile three times
            // and this case would pass on a broken screen (trap 3.16).
            assertEquals("Sent", sentLabel, "The first tile is labelled Sent");
            assertEquals("Opened", openedLabel, "The second tile is labelled Opened");
            assertEquals("Open rate", rateLabel, "The third tile is labelled Open rate");
            assertFalse(sent.isBlank(), "The Sent tile must carry a value");
            assertFalse(opened.isBlank(), "The Opened tile must carry a value");
            assertFalse(rate.isBlank(), "The Open rate tile must carry a value");
            assertTrue(rate.contains("%"),
                    "The rate is formatted \"%.2f%%\". Got: '" + rate + "'");
        } finally {
            backToLibrary();
        }
    }

    @Test
    @Order(25)
    @DisplayName("L-5: the chart renders, or its empty state does — exactly one of them")
    void chartOrEmptyState() {
        DocDistHistoryPage history = openHistoryOrSkip();
        try {
            DocDistAnalyticsPage analytics = openAnalyticsOrSkip(history);
            assumeTrue(!analytics.isUnavailable(), "Analytics is unavailable for this project (L-7)");

            boolean chart = analytics.chartShown();
            boolean empty = analytics.chartEmptyShown();
            System.out.println(">>> L-5: chart=" + chart + " chartEmpty=" + empty
                    + " emptyText='" + analytics.chartEmptyText() + "'");
            screenshot("dd-analytics-chart");

            // renderChart() toggles them as a pair — an empty timeline hides dd_chart and
            // shows dd_chart_empty, and vice versa. Both up, or neither, means render()
            // returned early on a null response.
            assertTrue(chart ^ empty,
                    "Exactly one of dd_chart / dd_chart_empty is shown (renderChart toggles the "
                            + "pair). chart=" + chart + " chartEmpty=" + empty);
            if (empty) {
                assertFalse(analytics.chartEmptyText().isBlank(),
                        "The chart's empty state carries 'No sends in this window'");
                System.out.println(">>> L-5: no timeline data in this window — the empty half "
                        + "of the pair is what was exercised.");
            }
        } finally {
            backToLibrary();
        }
    }

    @Test
    @Order(26)
    @DisplayName("L-6: the three top-N cards render with their titles")
    void topSectionsRender() {
        DocDistHistoryPage history = openHistoryOrSkip();
        try {
            DocDistAnalyticsPage analytics = openAnalyticsOrSkip(history);
            assumeTrue(!analytics.isUnavailable(), "Analytics is unavailable for this project (L-7)");

            String[][] cards = {
                    {DocDistAnalyticsPage.TOP_RECIPIENTS, "Top recipients"},
                    {DocDistAnalyticsPage.TOP_FOLDERS, "Top folders"},
                    {DocDistAnalyticsPage.TOP_LISTS, "Top distribution lists"},
            };
            for (String[] card : cards) {
                analytics.scrollTo(card[0]);
                String title = analytics.topTitle(card[0]);
                int rows = analytics.topRowCount(card[0]);
                System.out.println(">>> L-6: " + card[0] + " title='" + title + "' rows=" + rows
                        + " " + analytics.topRowTexts(card[0]));

                assertTrue(analytics.hasTopCard(card[0]),
                        card[0] + " must be present — it is an <include> of view_dd_top_list");
                // Each title is read anchored on its own include id; dd_top_title exists three
                // times, so this is the same collision as the KPI tiles.
                assertEquals(card[1], title,
                        card[0] + " is titled '" + card[1] + "' (set in onViewCreated)");
                // Zero rows is legitimate — bindTop() renders whatever the response carries,
                // and a project with no sends in the window has nothing to rank.
                assertTrue(rows >= 0, "Row count must be readable for " + card[0]);
            }
            screenshot("dd-analytics-top");
        } finally {
            backToLibrary();
        }
    }

    @Test
    @Order(27)
    @DisplayName("L-7: the unavailable state hides the content, and vice versa")
    void unavailableStateHandled() {
        DocDistHistoryPage history = openHistoryOrSkip();
        try {
            DocDistAnalyticsPage analytics = openAnalyticsOrSkip(history);
            boolean unavailable = analytics.isUnavailable();
            boolean content = analytics.contentShown();
            System.out.println(">>> L-7: unavailable=" + unavailable + " contentShown=" + content
                    + " text='" + analytics.unavailableText() + "'");
            screenshot("dd-analytics-unavailable");

            // render(): ddUnavailable and ddScroll are driven by the SAME flag in opposite
            // directions, so the invariant holds whichever way the endpoint answered — this
            // case asserts on every project, not only a broken one.
            assertTrue(unavailable != content,
                    "dd_unavailable and dd_scroll are mutually exclusive (render() sets both "
                            + "from state.unavailable). unavailable=" + unavailable
                            + " contentShown=" + content);
            if (unavailable) {
                assertFalse(analytics.unavailableText().isBlank(),
                        "The unavailable state explains itself ('Analytics isn't available for "
                                + "this project.')");
            } else {
                System.out.println(">>> L-7: the endpoint answered, so only the negative half "
                        + "(content shown, banner hidden) was exercised. The positive half needs "
                        + "a project or deployment where GET /distributions/analytics fails — "
                        + "AnalyticsViewModel routes EVERY failure, 404 included, to this state.");
            }
        } finally {
            backToLibrary();
        }
    }

    @Test
    @Order(28)
    @DisplayName("L-8: the open rate is consistent with sent and opened")
    void openRateConsistent() {
        DocDistHistoryPage history = openHistoryOrSkip();
        try {
            DocDistAnalyticsPage analytics = openAnalyticsOrSkip(history);
            assumeTrue(!analytics.isUnavailable(), "Analytics is unavailable for this project (L-7)");

            double sent = analytics.kpiNumber(DocDistAnalyticsPage.KPI_SENT);
            double opened = analytics.kpiNumber(DocDistAnalyticsPage.KPI_OPENED);
            double rate = analytics.kpiNumber(DocDistAnalyticsPage.KPI_RATE);
            System.out.println(">>> L-8: sent=" + sent + " opened=" + opened + " rate=" + rate
                    + " (raw '" + analytics.kpiValue(DocDistAnalyticsPage.KPI_SENT) + "' / '"
                    + analytics.kpiValue(DocDistAnalyticsPage.KPI_OPENED) + "' / '"
                    + analytics.kpiValue(DocDistAnalyticsPage.KPI_RATE) + "')");
            screenshot("dd-analytics-rate");

            assumeTrue(!Double.isNaN(sent) && !Double.isNaN(opened) && !Double.isNaN(rate),
                    "One of the KPIs is not a number — sent='"
                            + analytics.kpiValue(DocDistAnalyticsPage.KPI_SENT) + "' opened='"
                            + analytics.kpiValue(DocDistAnalyticsPage.KPI_OPENED) + "' rate='"
                            + analytics.kpiValue(DocDistAnalyticsPage.KPI_RATE) + "'. A tile "
                            + "reading '—' or 'N/A' cannot be reconciled arithmetically.");

            // Invariants first, so a failure names the specific thing that broke rather than
            // just "the numbers disagree".
            assertTrue(rate >= 0.0 && rate <= 100.0,
                    "An open rate must sit in 0…100%. Got: " + rate);
            assertTrue(opened <= sent,
                    "Opened (" + opened + ") cannot exceed Sent (" + sent + ") — opened ⊆ "
                            + "delivered ⊆ sent");
            if (sent == 0.0) {
                assertEquals(0.0, opened, 0.0001, "Nothing sent means nothing opened");
                assertEquals(0.0, rate, 0.0001, "Nothing sent means a 0% open rate");
                System.out.println(">>> L-8: no sends in the 30-day window — only the zero case "
                        + "was exercised. The ratio itself needs a window containing a send.");
                return;
            }

            double expected = (opened / sent) * 100.0;
            // The tile is rendered "%.2f%%", so rounding alone is worth ~0.005; a whole
            // percentage point of slack absorbs that and any server-side rounding.
            assertEquals(expected, rate, 1.0,
                    "Open rate must be opened ÷ sent: " + opened + " / " + sent + " = "
                            + String.format("%.2f", expected) + "%, but the tile reads " + rate
                            + "%. The server supplies engagement.open_rate independently of "
                            + "totals.sent, so a mismatch most likely means it is computed over "
                            + "engagement.delivered instead — worth confirming against the API "
                            + "before treating it as a UI bug.");
        } finally {
            backToLibrary();
        }
    }

    // ==================== helpers ====================

    /** Drops a trailing ellipsis so a single-line row's subject can be compared as a prefix. */
    private static String stripEllipsis(String s) {
        String t = s.trim();
        if (t.endsWith("…")) t = t.substring(0, t.length() - 1);
        if (t.endsWith("...")) t = t.substring(0, t.length() - 3);
        return t.trim();
    }

    /** The longest word in a subject — a substring specific enough to search on (J-4). */
    private static String longestWord(String subject) {
        String best = "";
        for (String word : subject.split("[^\\p{L}\\p{N}]+")) {
            if (word.length() > best.length()) best = word;
        }
        return best;
    }
}
