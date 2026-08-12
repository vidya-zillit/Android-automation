package com.zillit.automation.core;

/**
 * The class-level {@code @Order} map, in ONE place, so two people adding suites on
 * different branches cannot silently pick the same number.
 *
 * <p><b>Why this file exists.</b> {@code @Order} is a single global integer
 * namespace. When two people invent a number on their own branch, git merges both
 * happily and the clash is invisible — exactly what happened when {@code InfoTest}
 * and {@code ToolsManagementTest} both ended up on 49. Add your constant HERE and
 * git raises a real merge conflict on this file, so a human resolves it.
 *
 * <p><b>Own a RANGE, not a number.</b> Ranges are by MODULE, not by person, so they
 * stay valid when someone moves area:
 *
 * <pre>
 *    0– 9   auth / project (create, join, list, flow)
 *   10–18   project list / misc project
 *   19–29   home / bulletin chat + media + calendar
 *   30–39   email
 *   40–47   C&amp;C (chat &amp; calling)
 *   48–59   tools                                  ← VIDYA
 *   60–69   tools: standalone modules (Recce, …)   ← VIDYA
 *   70–89   ADMIN SETTINGS                         ← BHAVIK
 *   90–99   destructive / global-state — MUST run last
 * </pre>
 *
 * <p><b>Leave gaps.</b> Inside your range, jump by 2–3 (70, 72, 74…) so a suite can
 * be slotted in later without renumbering anything.
 *
 * <p><b>Never renumber someone else's suite to make room.</b> If your range is full,
 * claim the next free range in this file — that edit is visible in review.
 *
 * <p><b>Ordering only matters within one Gradle run.</b> {@code run-and-report.sh}
 * runs each module as its own invocation, so cross-module numbers rarely matter in
 * practice — but suites that mutate global state (enabling/disabling tools,
 * reordering groups, changing a department) must still sort LAST so they cannot pull
 * the rug from under everything before them.
 *
 * <p>{@code TestOrderGuardTest} fails the build on any duplicate, so a collision is
 * caught the moment it is introduced rather than surfacing as a weird run order.
 */
public final class TestOrder {

    private TestOrder() { }

    // 0–9 · auth / project ---------------------------------------------------
    public static final int PROJECT_FLOW_E2E    = 0;
    public static final int PROJECT_JOIN        = 1;
    public static final int PROJECT_VALIDATION  = 2;
    public static final int PROJECT_SUBTYPE     = 3;
    public static final int PROJECT_CREATE_E2E  = 9;

    // 10–18 · project list ---------------------------------------------------
    public static final int PROJECT_LIST        = 10;
    public static final int PROJECT_JOIN_E2E    = 11;

    // 19–29 · home / bulletin / calendar --------------------------------------
    public static final int POSTING_RIGHTS      = 19;
    public static final int HOME_CHAT           = 20;
    public static final int CALENDAR            = 21;
    public static final int CALL_SHEET          = 22;
    public static final int MEDIA_PREVIEW       = 23;
    public static final int CHAT_OPTIONS        = 24;
    public static final int MEDIA_SYSTEM_PICKER = 25;
    public static final int REGRESSION_STRESS   = 26;
    public static final int ALL_UNITS_MEDIA     = 27;
    public static final int MANUAL_PICK_SEND    = 28;

    // 30–39 · email ----------------------------------------------------------
    public static final int EMAIL               = 30;
    public static final int EMAIL_DRAWER        = 31;
    public static final int EMAIL_EXTRAS        = 32;
    public static final int EMAIL_ACTIONS       = 33;
    public static final int EMAIL_CALENDAR      = 34;

    // 40–47 · C&C ------------------------------------------------------------
    public static final int CC_GROUP_CREATE     = 40;
    public static final int CC_GROUP_CHAT       = 41;
    public static final int CC_GROUP_MANAGE     = 42;
    public static final int CC_CALL             = 43;
    public static final int CC_DIRECT_CHAT      = 44;
    public static final int CC_CALL_TAB         = 45;
    public static final int CC_CONTACTS         = 46;
    public static final int CC_CHAT_MEDIA       = 47;

    // 48–59 · tools — chat-style modules · VIDYA ------------------------------
    public static final int TOOL_CONFIDENTIAL   = 48;
    public static final int TOOL_INFO           = 49;
    public static final int TOOL_CATERING       = 51;

    // Drive OVERFLOW — 62–69 (Drive's own range) is full, and renumbering an existing
    // suite to make room is exactly what this file forbids. These four claim the next
    // free slots instead. They are independent of the 62–69 suites (each seeds and
    // cleans up its own folder / file / comments), so running earlier is harmless.
    public static final int DRIVE_COMMENTS      = 52;
    public static final int DRIVE_REQUESTS      = 53;
    public static final int DRIVE_ACTIVITY      = 54;
    public static final int DRIVE_SETTINGS      = 55;
    /** Two devices — isolates the folder-sharing failure (plan §2.1). */
    public static final int DRIVE_FOLDER_SHARE  = 56;
    /** Experiment harness for the editor cell-write failure (plan §2.2), not coverage. */
    public static final int DRIVE_EDITOR_PROBE  = 57;
    // 58–59 · tools — Document Distribution · VIDYA -------------------------
    /** Areas A+B: entry, shell and the library listing. */
    public static final int DOC_DISTRIBUTION    = 58;
    /** Area C: folders (create / edit / delete) + the area-B cases that need content. */
    public static final int DOC_DISTRIBUTION_FOLDERS = 59;
    // 58–59 full — the content suites continue at 81–88 (each seeds and cleans up
    // its own folder, so running after the Drive range is harmless).

    // 60–69 · tools — standalone modules · VIDYA ------------------------------
    public static final int RECCE               = 60;
    public static final int RECCE_PAGINATION    = 61;
    public static final int DRIVE               = 62;
    public static final int DRIVE_UPLOAD        = 63;
    public static final int DRIVE_SHARE         = 64;
    /** Recipient side — runs on the SECOND device, after DRIVE_SHARE has granted access. */
    public static final int DRIVE_SHARED_WITH_ME = 65;
    public static final int DRIVE_EDITOR        = 66;
    public static final int DRIVE_VIEW          = 67;
    /** Two devices editing one document — needs a SECOND device. */
    public static final int DRIVE_COLLAB        = 68;
    public static final int DRIVE_ORGANISE      = 69;
    // 62–69 full — Drive's symmetric two-device journey continues the tools range.

    // 80–89 · cross-cutting / two-device ---------------------------------------
    /** Both devices upload, share, receive and edit — the full symmetric loop. */
    public static final int DRIVE_E2E           = 80;

    // 81–88 · tools — Document Distribution content suites · VIDYA ------------
    // (DOC_DISTRIBUTION_TEST_CASES.md §6 — areas D/E/F now; G/H/I, J/K/L, M/N to
    // follow in this block. Self-contained: each seeds and deletes its own folder.)
    /** Area D: files &amp; selection — upload, preview, row menu, selection bar, move. */
    public static final int DOC_DISTRIBUTION_FILES    = 81;
    /** Areas E+F core: the composer and its attach-from-library picker. NO real send. */
    public static final int DOC_DISTRIBUTION_COMPOSER = 82;
    /** Grid view — the second layout family: inline row actions the list view has not got. */
    public static final int DOC_DISTRIBUTION_GRID      = 94;
    /** Publish TYPE rules: pdfOnly / documentsOnly destinations vs every media family. */
    public static final int DOC_DISTRIBUTION_PUBLISH_TYPES = 96;
    /** Nested folders: folder-in-folder, deep breadcrumb, uploads at depth. */
    public static final int DOC_DISTRIBUTION_NESTED    = 95;
    /** Area G: distribution lists (presets) — CRUD, badge, apply in the composer. */
    public static final int DOC_DISTRIBUTION_PRESETS   = 83;
    /** Area H: email templates — CRUD, badge, apply in the composer. */
    public static final int DOC_DISTRIBUTION_TEMPLATES = 84;
    /** Area I: address book, contact detail/editor, CSV import, export. */
    public static final int DOC_DISTRIBUTION_CONTACTS  = 85;
    /** Areas J+K+L: history list, distribution detail, analytics. Read-mostly. */
    public static final int DOC_DISTRIBUTION_HISTORY   = 86;
    /** Area M: watermark wizard, batch screen, download. */
    public static final int DOC_DISTRIBUTION_WATERMARK = 87;
    /** Area N: publish sheet — categories, fields, republish. Writes into OTHER modules. */
    public static final int DOC_DISTRIBUTION_PUBLISH   = 88;
    /** Area O: rights — the click-time permission gate. Two cases need a second account. */
    public static final int DOC_DISTRIBUTION_RIGHTS    = 89;
    /**
     * F-25 — the only suite that SENDS. Recipients are RFC-2606 example.com addresses,
     * which cannot reach a real person. Runs late so History/detail have something real.
     */
    public static final int DOC_DISTRIBUTION_SEND      = 91;
    /**
     * Housekeeping: deletes stray {@code QA_*} folders / lists / templates left behind by
     * failed cases. In the DESTRUCTIVE range because it removes data, and opt-in only
     * ({@code --doc-dist-cleanup}) — never part of --doc-dist-all.
     */
    public static final int DOC_DISTRIBUTION_CLEANUP    = 93;
    /** Area P: regression &amp; edge — offline, rotation, long names, rapid back. */
    public static final int DOC_DISTRIBUTION_REGRESSION = 92;

    // 70–89 · ADMIN SETTINGS · BHAVIK -----------------------------------------
    public static final int DEPARTMENT_LISTING_ORDER      = 70;
    public static final int COMPANY_DETAILS               = 72;
    public static final int CREATE_ADDITIONAL_SHOOTING_UNIT = 74;
    public static final int CREATE_NEW_DEPARTMENT         = 76;
    public static final int CREATE_NEW_DESIGNATION        = 78;
    // free: 71, 73, 75, 77, 79–89 — next admin-settings suites go here

    // 90–99 · destructive / global-state — always last ------------------------
    /** Disables tools and reorders groups — must not run before the tool suites. */
    public static final int TOOLS_MANAGEMENT    = 90;
}
