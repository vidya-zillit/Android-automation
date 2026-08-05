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
    // free: 50, 52–59

    // 60–69 · tools — standalone modules · VIDYA ------------------------------
    public static final int RECCE               = 60;
    public static final int RECCE_PAGINATION    = 61;
    // free: 62–69

    // 70–89 · ADMIN SETTINGS · BHAVIK -----------------------------------------
    public static final int DEPARTMENT_LISTING_ORDER      = 70;
    public static final int COMPANY_DETAILS               = 72;
    public static final int CREATE_ADDITIONAL_SHOOTING_UNIT = 74;
    public static final int CREATE_NEW_DEPARTMENT         = 76;
    public static final int CREATE_NEW_DESIGNATION        = 78;
    public static final int CREATE_REMOTE_SHOOTING_UNIT   = 80;
    public static final int CREATE_UPDATE_HOME_UNIT       = 82;
    public static final int EDIT_PROJECT_NAME             = 84;
    public static final int MANAGE_TOOL_GROUPS            = 86;
    // free: 71, 73, 75, 77, 79, 81, 83, 85, 87–89 — next admin-settings suites go here

    // 90–99 · destructive / global-state — always last ------------------------
    /** Disables tools and reorders groups — must not run before the tool suites. */
    public static final int TOOLS_MANAGEMENT    = 90;
    /** Disables EVERY tool via Admin Settings, then re-enables all — bigger blast radius than TOOLS_MANAGEMENT's single-tool toggle. */
    public static final int CUSTOMIZATION_OF_TOOLS_ALL = 91;
    // free: 92–99
}
