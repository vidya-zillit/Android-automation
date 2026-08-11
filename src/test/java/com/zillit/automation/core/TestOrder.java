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
 *   90–99   destructive / global-state (tools)     ← VIDYA
 *  100–149  ADMIN SETTINGS                         ← BHAVIK
 *  150–169  SETTINGS TAB (non-admin)               ← BHAVIK
 *   (70–89 retired — the admin block moved to 100+ to make room)
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

    // 90–99 · destructive / global-state · VIDYA -----------------------------
    /** Disables tools and reorders groups — must not run before the tool suites. */
    public static final int TOOLS_MANAGEMENT             = 90;
    // free: 91–99

    // 100–149 · ADMIN SETTINGS · BHAVIK ---------------------------------------
    // This list IS the run order for --admin-settings. Steps of 3 leave two free
    // numbers between every pair, so a new admin module can be slotted anywhere
    // without renumbering its neighbours.
    //
    // CUSTOMIZATION_OF_TOOLS_ALL lives here, not in the destructive range: it is
    // an Admin Settings screen, and its position is fixed by the agreed sequence.
    //
    // DELETE_PROJECT is last because it schedules the project for real deletion
    // and leaves the timer armed.
    public static final int DEPARTMENT_LISTING_ORDER        = 100;
    public static final int COMPANY_DETAILS                 = 103;
    public static final int CREATE_ADDITIONAL_SHOOTING_UNIT = 106;
    public static final int CREATE_NEW_DEPARTMENT           = 109;
    public static final int CREATE_NEW_DESIGNATION          = 112;
    public static final int CREATE_REMOTE_SHOOTING_UNIT     = 115;
    public static final int CREATE_UPDATE_HOME_UNIT         = 118;
    public static final int CUSTOMIZATION_OF_TOOLS_ALL      = 121;
    public static final int EDIT_PROJECT_NAME               = 124;
    public static final int MANAGE_TOOL_GROUPS              = 127;
    public static final int PRE_APPROVED_USERS              = 130;
    public static final int APPROVE_NEW_USER_REQUEST        = 133;
    public static final int SOS_RECEIVERS                   = 136;
    public static final int USER_MANAGEMENT                 = 139;
    public static final int WATERMARK_LOGO                  = 142;
    public static final int DELETE_PROJECT                  = 145;
    // free: 101–102, 104–105, 107–108, … 146–149 — new admin modules go here

    // 150–169 · SETTINGS TAB (non-admin) · BHAVIK -----------------------------
    // NOT part of Admin Settings — its own --settings flag, run after that block.
    public static final int EDIT_MY_PROFILE              = 150;
    /**
     * Chat font size, theme and default home unit. Measures the EFFECT of each
     * setting rather than its stored value, and restarts the app to prove the
     * default unit persists.
     */
    public static final int APP_PREFERENCES              = 153;
    /**
     * Read-only: shows the laptop-linking instructions and confirms Link Device
     * opens the QR scanner. Never actually links a device — that needs a QR code
     * from a browser — so it changes nothing.
     */
    public static final int LINKED_DEVICE                = 156;
    /**
     * Read-only: shows the project invitation text and code. Never presses
     * Share, so no invitation is sent and nothing changes.
     */
    public static final int INVITE_USERS                 = 159;
    /**
     * The whole ZILLIT GUIDE, entry by entry: Contact us (including Email Us),
     * FAQ, Privacy Policy, Terms of Use, and the Tutorial for Admin tour.
     * Tutorial for Admin is a row in that guide, so it lives here rather than in
     * a suite of its own.
     *
     * <p>SENDS A REAL EMAIL every run: the composer arrives addressed to Zillit
     * Support, so the message reaches their inbox as well as the BCC address.
     */
    public static final int ZILLIT_HELP                  = 162;
    // free: 151–152, 154–155, 157–158, 160–161, 163–169
}
