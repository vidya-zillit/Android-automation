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
 *  170–189  tools overflow (Schedule Dist, Continuity…) ← VIDYA
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
    /** First step of the full sweep: create a project, select it, leave the app inside it. */
    public static final int PROJECT_CREATE_AND_SELECT = 4;
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
    /** Call Sheet upload-mode sheet (ZL-21154) — runs after CALL_SHEET so the unit has content. */
    public static final int CALL_SHEET_UPLOAD_MODE = 29;

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

    /**
     * External users: the listing, its filters, the three user types with their show/hide
     * rules, every field's validation, the read-only detail screen, and the two taps that
     * hand over to the Email module and to the dialer.
     */
    public static final int EXTERNAL_USERS             = 50;


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

    /** Tools → Map (new_map). Sits after the Drive block, before the DocDist 80s. */
    public static final int MAP                 = 70;
    /** Map's read-only overlay of Location-tool Shortlisted/Final pins. */
    public static final int MAP_LOCATION_PINS   = 71;
    /** Tools → Location — the tool that feeds that overlay. */
    public static final int LOCATION_TOOL       = 72;

    /** Map → cities: create / validate / edit / delete / switch. */
    public static final int MAP_CITY_CRUD       = 73;
    /** Map → studio zones, including applying one as the active boundary. */
    public static final int MAP_ZONE_CRUD       = 74;
    /**
     * Map → pinning and the boundary verdicts. Runs AFTER the zone suite on purpose:
     * the "outside the zone but inside the city" verdict is only reachable once a zone
     * has been applied.
     */
    public static final int MAP_PIN_BOUNDARY    = 75;
    /** Map → locations: form validation, detail sheet, list view, delete. */
    public static final int MAP_LOCATION_CRUD   = 77;
    /** Map → location types (header manager). Before locations, which use them. */
    public static final int MAP_TYPE_MANAGER    = 76;

    /** Map → directions and share. Last, because both hand off to other apps. */
    public static final int MAP_DIRECTIONS_SHARE = 78;
    /** Map → empty-state guards and the state the module persists for itself. */
    public static final int MAP_GUARDS          = 79;
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
     * Map → the whole data lifecycle on one city: several locations created by dropping
     * pins, renamed, scene-numbered, the out-of-city choices, a drag, deletes, cleanup.
     *
     * <p>NOT in the 70s with the other Map suites because that band is full (70–79, and
     * 80 is DRIVE_E2E). Late suits it anyway: this is the only Map suite that WRITES and
     * DELETES data, so it should run after the read-only ones have had a quiet project to
     * read. It removes every fixture it creates.
     */
    public static final int MAP_CRUD_LIFECYCLE         = 97;

    /**
     * Housekeeping — deletes any {@code ZL AUTO} location, zone or type left on the
     * project when a Map suite could not finish its own cleanup (a lost session mid-run,
     * usually). OPT-IN ONLY: {@code --map-cleanup}, never part of --map-all or --all,
     * because it removes data and nothing should do that unasked.
     */
    public static final int MAP_CLEANUP                = 98;

    /**
     * Map → a SECOND city: created through the out-of-city pin dialog, then switching
     * between cities and proving pins stay scoped to the one selected.
     *
     * <p>Its own suite, kept small on purpose. The long Map suites lose the device before
     * they finish on this emulator, and these cases are worthless unless they complete.
     */
    public static final int MAP_CITY_MULTI             = 99;

    // ── Production Diary / Box Schedule ─────────────────────────────────────
    // One module, two names (strings.xml: "Production Diary/Box Schedule").
    // Ordered so each suite starts from what the one before it made: a type, then a
    // schedule day, then the events and notes that hang off it.
    /** Views, sub-modes, default view, the drawer. Read-only. */
    public static final int BOX_SHELL                  = 102;
    /** Schedule types — needed before a schedule can be created. */
    public static final int BOX_TYPES                  = 104;
    /** Schedule days: the three date modes, validation, edit, delete. */
    public static final int BOX_SCHEDULE_CRUD          = 105;
    /** Replace / Extend / Overlap, including the scattered-dates case (ZL-19566). */
    public static final int BOX_CONFLICT               = 107;
    /**
     * A whole production laid out with DIFFERENT types across real dates — Prep, Travel,
     * Shoot, a Day Off inside the Shoot, Wrap — and each conflict resolution applied and
     * read back. Runs after the conflict suite, which proves the chooser appears at all.
     */
    public static final int BOX_PRODUCTION             = 111;

    /**
     * UPDATING schedules: the scope prompt, the single-day split and the whole-block
     * retype. A different code path from creating, with its own meanings for
     * Replace / Extend / Overlap. Runs after the production suite has laid out a timeline.
     */
    public static final int BOX_EDIT                   = 113;

    /**
     * One schedule, five renderings — Month, Week, Day, By Date and By Schedule — held to
     * saying the same thing. Runs last of the Box suites, on days it lays down itself.
     */
    public static final int BOX_CROSS_VIEW             = 114;

    /**
     * The whole life of an event and of a note: made, renamed, moved, re-addressed,
     * removed — each change checked from the List row and the Calendar, not from the form.
     */
    public static final int BOX_EVENT_LIFECYCLE        = 116;

    /**
     * Opt-in housekeeping for the Box module: deletes the schedule days the suites left in
     * their date window. Never part of --box or --all; needs -Dzillit.boxCleanup=true.
     */
    public static final int BOX_CLEANUP                = 117;
    /** Events and notes, the Home-Calendar prompt, general vs personal. */
    public static final int BOX_EVENT_NOTE             = 108;
    /** Filters, day detail, PDF actions, history. */
    public static final int BOX_VIEWS                  = 110;
    /**
     * Housekeeping: deletes stray {@code QA_*} folders / lists / templates left behind by
     * failed cases. In the DESTRUCTIVE range because it removes data, and opt-in only
     * ({@code --doc-dist-cleanup}) — never part of --doc-dist-all.
     */
    public static final int DOC_DISTRIBUTION_CLEANUP    = 93;
    /** Area P: regression &amp; edge — offline, rotation, long names, rapid back. */
    public static final int DOC_DISTRIBUTION_REGRESSION = 92;

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
    /** Admin Settings regrouped into sections + cross-group search (new UI). */
    public static final int ADMIN_SETTINGS_GROUPS = 101;
    public static final int COMPANY_DETAILS                 = 103;
    public static final int CREATE_ADDITIONAL_SHOOTING_UNIT = 106;
    public static final int CREATE_NEW_DEPARTMENT           = 109;
    public static final int CREATE_NEW_DESIGNATION          = 112;
    public static final int CREATE_REMOTE_SHOOTING_UNIT     = 115;
    public static final int CREATE_UPDATE_HOME_UNIT         = 118;
    // Sides OVERFLOW — the tools range 48–69 is full end to end (48–51 tools,
    // 52–69 Drive and Doc Distribution), and the file's own rule is to claim the next
    // free slot rather than renumber a suite that already has a run behind it. 119 is
    // the first one free after the Box block (110–117).
    /**
     * Sides: both tabs, the two §9.1 gates, scripts and pages, the manual generate flow
     * through review and publish, the row actions, History and the Autogenerate sheet.
     */
    public static final int SIDES                           = 119;

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

    // 170–189 · tools overflow · VIDYA ----------------------------------------
    // The tools ranges are full (48–69 end to end; 90–99 taken by Tools Management, Doc
    // Distribution and Map), and the gaps between Bhavik's admin constants are kept for new
    // admin modules — so new tool modules start a range of their own here. Steps of 5 leave
    // room for a second suite per module.
    /**
     * Schedule Distribution (Tools tile "Schedule Full &amp; One Line"): the three tabs, the
     * sentinel Full / One Line upload and its Replace, View / Download / counts, page uploads
     * and their validation, the Pages search and folders, delete, publish (to Cancel), History.
     */
    public static final int SCHEDULE_DIST                = 170;
    /**
     * Continuity: both tabs, adding a record through the app's gallery, the grid and its
     * card menu, edit, forward to All Department, distribute, download, delete per level,
     * the All Department department list, and a sweep of everything marked ZLAUTO.
     */
    /** Schedule D.O.D. — reserved 2026-11-23 for the parallel module sessions. */
    public static final int SCHEDULE_DOD                 = 171;
    /** Supporting Artistes / Extras (SA Portal). */
    public static final int SA_EXTRAS                    = 172;
    /** Costumes &amp; Set Sync. */
    public static final int COSTUMES_SET_SYNC            = 173;
    /** Z-E-Signature. */
    public static final int E_SIGNATURE                  = 174;
    public static final int CONTINUITY                   = 175;
    /** Crew DOOD. */
    public static final int CREW_DOOD                    = 176;
    /**
     * Call Sheet Creation, creator side on one device: tabs, template picker, the editor's
     * native parts, Drafts actions, Send for Comments / Signature, Permission tab, sweep.
     */
    public static final int CALLSHEET_CREATION           = 180;
    /** Call Sheet Creation approval round across two devices (creator + approver). */
    public static final int CALLSHEET_APPROVAL           = 181;
    /** Production Report Creation, creator side on one device (same workflow as Call Sheet). */
    public static final int PRODUCTION_REPORT            = 185;
    /** Production Report approval round across two devices (creator + approver). */
    public static final int PRODUCTION_REPORT_APPROVAL   = 186;
    /**
     * Cross-module chat check: an Office document message opens the app's own editor in VIEW
     * mode, in every chat (Home units, C&amp;C, Info, Catering, Production Report chat, …). Not in
     * 19–29 with the Home suites because that band is full; it touches many modules anyway.
     */
    public static final int CHAT_DOCUMENT_EDITOR         = 177;
    /**
     * Map ← Location tool, end to end: a ZL_AUTO_ shoot location is created in Selects with
     * a pin inside a Map city, moved to Shortlist and then Final, and read back on the Map
     * at each stage (L-12…L-14, L-19). Here, not in the 70s, because the Map band is full;
     * it WRITES to both modules and removes everything it made.
     */
    public static final int MAP_LOCATION_FLOW            = 179;
    // free: 178, 182–184, 187–189

    // 190–224 · web-report gap suites (2026-11-23) · VIDYA -----------------------
    // One block per module group: 190–194 small modules, 195–199 Doc Distribution,
    // 200–204 Calendar/Email, 205–209 Home, 210–214 Info/CI, 215–219 Catering/Settings,
    // 220–224 Map.
    /** Notifications list and push routing (NOTIF-01 admin screens for non-admins, NOTIF-02). */
    public static final int NOTIFICATIONS                = 190;
    /** Schedule Full &amp; One Line across two devices (SCH-03 badge). */
    public static final int SCHEDULE_DIST_TWO_USER       = 192;
    /** Production Diary / Box History across two users (BOX-02). */
    public static final int BOX_HISTORY_TWO_USER         = 193;
    /** Document Distribution web-report gap cases (DD-23, DD-11, moves, …). */
    public static final int DOCDIST_WEB_GAP              = 195;
    /** Calendar: edit a recurring event — this / following / all. */
    public static final int CALENDAR_RECURRING_SCOPE     = 200;
    /** Calendar web-report gap cases. */
    public static final int CALENDAR_WEB_GAP             = 201;
    /** Calendar invitees accept / decline across two devices. */
    public static final int CALENDAR_INVITE              = 202;
    /** Home unit chats: web-report gap cases. */
    public static final int HOME_WEB_GAP                 = 205;
    /** Info / Confidential Info across two devices (rights, live updates). */
    public static final int INFO_TWO_USER                = 210;
    /** Viewing &amp; Posting Rights Grid (Settings web gaps). */
    public static final int PERMISSION_GRID              = 215;
    /** Admin Settings web-report gap cases. */
    public static final int ADMIN_SETTINGS_WEB_GAP       = 216;
    /** Catering across two devices: crew member (emulator) and caterer (Pixel). */
    public static final int CATERING_TWO_USER            = 217;
    /** Map web-report gap cases. */
    public static final int MAP_WEB_GAP                  = 220;
    /** Map zone guards (blank radius, double tap). */
    public static final int MAP_ZONE_GUARDS              = 221;

    // 225–239 · Location / Casting / Costume tools (2026-11-24) · VIDYA ------------
    /** Location tool (Selects / Shortlist / Final, episodes, cities, details, rights). */
    public static final int LOCATION_TOOL_FULL           = 225;
    /** Location tool across two devices. */
    public static final int LOCATION_TOOL_TWO_USER       = 226;
    /** Casting (Main Cast) and Casting (Background Cast). */
    public static final int CASTING                      = 230;
    /** Casting across two devices. */
    public static final int CASTING_TWO_USER             = 231;
    /** Costume (Main Cast) and Costume (Background Cast) — the Wardrobe tools. */
    public static final int COSTUME                      = 235;
    /** Costume across two devices. */
    public static final int COSTUME_TWO_USER             = 236;
}
