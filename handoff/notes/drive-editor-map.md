# Drive editor map: `origin/development` vs. our Appium Drive suites

Source: `/Users/vidyasagar/StudioProjects/Zillit-Android`, read only with `git show origin/development:…`. Nothing was checked out, built or run.
Date: 2026-11-23. The development tip is `cc3dc2a96`.

## 0. TL;DR

* **The editor is no longer Collabora.** `OnlyOfficeEditorActivity` (same class name and package) now loads **Zillit Docs / Sheets / Slides**. This is an in-house host page that Drive serves (`editorUrl?WOPISrc=…&access_token=…&theme=dark|light[&mode=view]`), shown full screen in a WebView that the activity builds in code. The switch happened in **113fcbef6 (2026-10-16)**, before the Nov work. The Nov commits added theme/mode in the URL, session reopen, chat/version sources, the notify prompt and the text editor.
* There is **no "What's New" modal, no "Edit document" pencil, no "Leave Editor" dialog, no formula bar / `pos_window-input-address`, and no ✓ accept button.** Every Collabora locator in `DriveEditorPage` is dead. The `drive_leave_editor_*` strings are still in `strings.xml` but no code uses them.
* **Mode is decided by the app and the server, not by a pencil:**
  * File detail **`btnEdit`** opens the editor in **EDIT** mode straight away (no `mode` extra).
  * File detail **`btnOpen`** opens in **VIEW** mode (`mode=view`). It adds an app-drawn native **`Button` "Edit"** at the bottom-right, only when the config says `canSwitchToEdit`.
  * Chat attachments and version previews are always VIEW mode and never get Edit.
* **Back** in view mode closes at once. In edit mode, Back commits the open cell (ENTER on sheets, blur on docs/slides), shows **"Saving…"** for 1.5 s, then either closes or shows **"Let others know you updated this file?"** (Send / Skip). The prompt only appears when the page reports that this person changed the file.
* The WebView is **not debuggable**: `setWebContentsDebuggingEnabled` is not called anywhere, so there is no `WEBVIEW_` context. Only the native accessibility tree is reachable. The editor page's own DOM labels are **unknown from the source**, because the page is served by zillit_drive and is not in this repo or `zillit_web`. They must be captured from a live page-source dump before any in-page assertions are written (see §6, case E-00).
* New, and fully native (easy to automate):
  * "New" blank file group in the + sheet.
  * `DriveTextFileActivity` (md/txt viewer and editor).
  * Version rows: "v3 · Autosaved", "Saved by …", "Open".
  * Notify prompt.
  * Folder "Download as ZIP".
  * Show filter values "Last 7 days" and "Large files".
  * Upload Pause/Resume.
  * Emailed share link permission "Can edit".

---

## 1. Commits since 2026-11-10 that touch Drive or the editor

| Commit | Date | What |
|---|---|---|
| 0c909b7fb (also folded into 305cf8345) | 11-16 | Editor: `theme=` and `mode=view` in the URL, WebView background #FFFFFF / #0F1115, page handles all gestures (wheel-event shim removed), `EditorSession` reopen after token expiry, `configChanges` (rotation no longer reloads). Share links: "Can edit" badge, edit-link URL not copyable, Copy-link 30-day option removed (now 1h / 24h / 7d), expiry read from `expires_on`. |
| 3ab6a41d0 | 11-19 | + sheet "New" group, `POST /files/new` |
| fe79a4620 | 11-19 | "Can edit" emailed share links |
| f824d9211, 504942bef | 11-19 | Office files from chats open read-only in the editor (C&C, Home, Info/Confidential, Accounts, Catering, Production report, Location/Casting/Wardrobe) |
| b5ae04154 | 11-19 | `DriveTextFileActivity` for .md / .markdown / .txt |
| 2e8cd2801 | 11-19 | Version history labels, saved by, change summary, Open a version |
| 99120a767 | 11-19 | "Let others know you updated this file?" |
| 579f6b3eb | 11-19 | `drive:version:restored` socket event reloads File details; 0-byte files in folder upload |
| f9a3bf916 | 11-19 | Folder "Download as ZIP"; folders included in multi-select Download |
| 782476d83 | 11-19 | Show filter: Last 7 days / Large files, plus a hint line |
| 44594ac44 | 11-19 | 0-byte picked files go through the `/uploads` session |
| cdc957a23 | 11-19 | Pause / Resume in the upload progress sheet |
| b6fffc15d | 11-19 | Our own parallel version of New-file, chat-editor and Can-edit. **Mostly discarded** in merge 59332b63b (PR #5 versions kept). Kept from it: Shared-with/by-me server order, and Forms & Signature opening from `FormDiscussionChatPage` (unit `form_signature`). |
| 59332b63b, 89f1a0110, cc3dc2a96 | 11-20…23 | Merges and the production send |

Earlier commit that matters: **113fcbef6 (10-16)** "Drive sharing hand-off, in-app viewers, editor fixes". It removed Collabora, the Leave dialog and two-finger wheel events, and added view-only with Edit and Open → editor read-only.

---

## 2. The office editor today

### 2.1 Host

* Activity: `com.zillit.zillitapp.bottomNav.tools.drive.presentation.editor.OnlyOfficeEditorActivity`. `driver.currentActivity()` contains `editor.OnlyOfficeEditorActivity`, unchanged.
* The layout is built **in code**: a `FrameLayout` containing a `WebView`, a horizontal `ProgressBar` (top), a centred `TextView` (loading / status / error) and a bottom-end `LinearLayout` holding one `Button`.
  * **None of these views has a resource-id.** Locate by class and text.
* Action bar hidden, no back arrow. Only system Back closes the screen.
* Manifest: `configChanges="orientation|screenSize|screenLayout|smallestScreenSize|keyboard|keyboardHidden"`, `windowSoftInputMode="adjustPan"`.
  * Rotation and keyboard do NOT reload the page.
  * A **uiMode (theme) change recreates the activity**, which reopens the editor in the new theme.
* Editor kinds come from config `editor` = `sheet` / `doc` / `slide`.
* Editable / openable extensions (`FileUtils.isEditableFile`, unchanged since before Nov 10): `xlsx xlsm xls ods csv docx doc odt rtf pptx ppt odp`.
  * **txt / md are NOT office-editable.** They go to `DriveTextFileActivity`.

### 2.2 How it is opened (intent extras)

| Entry point | Extras | Mode | Edit switch? |
|---|---|---|---|
| File detail **`btnEdit`** (visible if `(isEditableFile ‖ isTextFile) && perms.canEdit && Drive postingAccess`) | `fileId`, `fileName` | **edit** (no `mode` extra) | n/a |
| File detail **`btnOpen`** (text "Open") for an office file | `fileId`, `fileName`, `mode=view` | **view** | native Button "Edit" if config `canSwitchToEdit == true` |
| + sheet → New Document / Spreadsheet / Presentation, after the name dialog | `fileId`, `fileName` | **edit** | n/a |
| Version row **"Open"** (office file) | `fileId`, `fileName`, `versionId`, `mode=view` | view, from `GET /versions/:fileId/:versionId/preview` | never |
| Chat attachment tap (office type, stored 24-hex message `_id`, not Box storage) | `chatMessageId`, `chatUnit` (absent for C&C), `fileName`, `mode=view` | view, from `GET /editor/chat/<msgId>/config` or `/editor/unit-chat/<unit>/<msgId>/config` | never |

* A Drive file-list **row tap still goes to File details** (`action_home_to_file_detail`). Nothing on the list opens the editor directly. The context menu has no Open/Edit row.
* Chat surfaces that call `openChatAttachment`:
  * `HoldersViewhandler` (C&C private and group chats)
  * `Home.kt` (unit `home`)
  * `ConfidentialInfo.kt` (`info` / `confidential_info`)
  * `AccountsActivity` (`accounts`)
  * `CateringActivity` (`catering`)
  * `FormDiscussionChatPage` (`form_signature`)
  * `ProductionReportChatPage` / `ProductionReportChatFragment` (`production_report`)
  * `OtherChatsActivity`: `location`, `location_unit`, `casting`, `casting_main_unit`, `casting_background_unit`, `wardrobe`, `wardrobe_main_unit`, `wardrobe_background_unit`
* Any failure (config error, non-office type, unsent message, Box project, unknown unit) falls back to the **old external-app open** (`openFileFromExternalFromUrl`). Repeat taps within 1.5 s are ignored.

### 2.3 Loading and errors (native TextView in the centre)

* While the config is fetched and the page loads: **"Loading editor…"** (`drive_loading_editor`). The WebView is GONE until `onPageFinished`; then the text hides and the WebView becomes VISIBLE.
* Errors, shown in the same TextView with the WebView hidden:
  * `"Failed to load editor"`
  * `"Failed to load editor: %1$s"` (page-token fallback)
  * `"SSL certificate error"` (release builds only)
  * `"File ID missing"` (toast, then finish)
  * The server's own text for a config error, e.g. keys `file_type_not_editable`, `file_too_large_to_edit`, `live_editor_unavailable`, humanised by `DriveApiException`.
* Renderer crash: `recreate()` reopens the editor with a fresh token.

### 2.4 View vs edit: how a test can tell

Native signals (reliable):

1. **Edit switch present.** `//android.widget.Button[@text='Edit']` inside `OnlyOfficeEditorActivity`, with no resource-id, at the bottom-right. Present ⇒ VIEW mode **and** the user may edit. Tapping it reloads in edit mode: "Loading editor…" shows again and the Button disappears.
2. **Back behaviour.**
   * VIEW: the activity finishes immediately; "Saving…" never shows.
   * EDIT (after the page loaded): the centre TextView shows **"Saving…"** (`drive_saving`) for about 1.5 s, then the activity finishes or the notify dialog appears.
3. **Entry point determines mode.** `btnEdit` ⇒ edit. `btnOpen`, version Open or chat ⇒ view.
4. VIEW opened from a chat or a version, or by someone without edit rights: no Edit Button. That is indistinguishable natively from edit mode, except by the Back behaviour in (2).

Page signals (unverified, need a dump):

* Per the code comment (113fcbef6 / 16 Oct §2), "on phones the editor page shows its own title and **'View only'** notice". So a `View only` text node is probably exposed in view mode.
* The URL carries `mode=view`, but the page strips the token from its URL. There is no webview context, so the URL cannot be read.
* The page keeps `window.zillitHost.debug.edited`. The app reads it via `evaluateJavascript` on close; tests cannot read it.

### 2.5 Toolbar and actions

* The app draws only the one "Edit" Button. Everything else (toolbar, title, View-only notice, sheet tabs, Insert picture…) is drawn by the Zillit Sheets / Docs / Slides page.
* The page "draws with WebGL / canvas" and "handles every gesture itself: swipe, tap, press-and-hold, scroll-bar drag, two-finger pinch (editors bundle 2026.11.06.4)".
* **Cell contents are almost certainly not in the a11y tree.** Toolbar labels are unknown.
* "Insert picture" is supported: the page's `<input type=file>` opens the system chooser (`ACTION_GET_CONTENT image/*`). That is a system-picker package switch a test can detect.
* Typing: the WebView is focusable. The first touch requests focus so the IME opens. The page types into a hidden text field.
  * `adb shell input text` should still reach it once a cell or paragraph has focus.
  * Our formula-bar / address-box path is gone.
* Saving: **autosave as you type, no Save button.** Back commits the last open cell (ENTER for `sheet`) and waits 1.5 s.

### 2.6 Close / leave / notify (exact strings)

* **No leave confirmation of any kind** in the office editor.
* After an edit-mode close, and only for a Drive file (not chat, not version) whose page says this person edited it:
  * MaterialAlertDialog title **"Let others know you updated this file?"**
  * Message **"Everyone with access gets a notification."**
  * EditText hint **"Add a note (optional)"**, max 300 characters, no id.
  * Positive **"Send"**, negative **"Skip"**. Back or outside tap = Skip.
  * Results (toast):
    * **"People with access were notified."** when notified > 0
    * **"No one else has access to this file."** when 0
    * **"You let them know a moment ago. Try again in a minute."** for `drive_edit_announce_too_soon`, rate-limited to one per person per file per minute
    * **"You need edit access to notify others."** for `no_edit_permission` / `insufficient_permissions`
    * **"Couldn't send the notification. Please try again."**
  * The editor finishes after the answer.
  * ⚠ **Send notifies real collaborators.** Automation should tap **Skip** unless the case is specifically about notifying.

### 2.7 Session reopen (`EditorSession`)

* Config `accessTokenTTL` is an epoch-ms expiry (about 8 h for Drive files).
* On `onStart` after that deadline, the editor fetches a fresh config and reloads once, in the same mode ("Loading editor…" reappears).
* Values that are missing, already in the past, or more than 7 days away are ignored.
* Not practical to automate (8 h). It could be faked only by changing the device clock, which the code deliberately ignores by using elapsedRealtime. **Skip or leave it manual.**

### 2.8 Theme

* URL `theme=dark|light` follows the app's uiMode.
* The WebView and its container background are **#FFFFFF (light) / #0F1115 (dark)** before the page paints.
* A test can screenshot during "Loading editor…" and sample a corner pixel. A uiMode change recreates the editor in the new theme.

---

## 3. New blank file (+ menu)

* FAB `fabAdd` opens `bottom_sheet_add_menu.xml`, which is now a **NestedScrollView**. Order of ids:
  `menuUploadFile` "Upload File" → `menuUploadFolder` "Upload Folder" → `menuCreateFolder` "Create Folder" → **`menuNewGroup`** containing:
  * header `menuNewHeader` **"New"**
  * `menuNewDocument` **"Document"**
  * `menuNewSpreadsheet` **"Spreadsheet"**
  * `menuNewPresentation` **"Presentation"**
  * `menuNewMarkdown` **"Markdown file"**
  * `menuNewText` **"Text file"**
  
  → then `menuRequestFiles`, `menuViewFileRequests` (only inside a folder).
* Visibility follows the same rule as Create Folder, `HomeViewModel.canAddToOpenFolder()`:
  * Shown in My Drive (root and subfolders).
  * Shown in a shared folder where the user is Editor / Owner (`openFolderCanEdit`).
  * Hidden at the Shared-with-me root and for Viewers.
  * The Uploads screen's copy of the sheet hides it.
  * FolderActivity hosts HomeFragment, so Shared-with-me subfolders follow the same rule.
* Tapping a type opens a MaterialAlertDialog:
  * Title = the type label ("Spreadsheet"…).
  * Message **"Name"**.
  * A single-line EditText (no id), prefilled with the default and **all text selected**: "Untitled document" / "Untitled spreadsheet" / "Untitled presentation" / "Untitled" (md and txt).
  * A TextView showing the extension ".docx" / ".xlsx" / ".pptx" / ".md" / ".txt".
  * Buttons **"Create"** / **"Cancel"**. IME Done = Create.
* Name rules (client side):
  * Whitespace is collapsed and trimmed.
  * A typed own extension is dropped ("Budget.xlsx" → "Budget"; "Notes.docx" for a spreadsheet stays "Notes.docx").
  * An empty box falls back to the default.
  * Capped at 500 characters (the server then cuts to 200).
  * **There is no client-side validation message.** An emptied box silently uses the default.
* `POST /files/new {type: docx|xlsx|pptx|md|txt, name, folder_id}`. The file lands in the **open folder** (`currentFolderId`; null = root). The list refreshes.
* Errors (toast, long):
  * `new_file_type_validation` → "Choose what to create: Document, Spreadsheet, Presentation, Markdown or Text file."
  * `folder_not_found` → "This folder no longer exists."
  * `insufficient_permissions` → "You can't add files to this folder."
  * Otherwise the server text, or "Couldn't create the file. Please try again."
* After creating:
  * docx / xlsx / pptx open **straight in `OnlyOfficeEditorActivity` in EDIT mode**.
  * md / txt open **`DriveTextFileActivity` already editing** (EditText focused, Save visible).
* Duplicate-name behaviour is server-side and unknown; a test should record it.

---

## 4. Other new features

### 4.1 Text / Markdown viewer and editor: `DriveTextFileActivity`

* Activity `…drive.presentation.text.DriveTextFileActivity`, with `adjustResize` and `configChanges` (rotation keeps unsaved text).
* Opened from:
  * File detail **btnOpen** for .md / .markdown / .txt → read-only. This replaces the old "download and hand to another app".
  * **btnEdit** → editing.
  * + New Markdown / Text → editing.
  * Version Open → read-only, titled **"<name> · version N"**.
* Layout is built in code, so **there are no resource-ids**:
  * Top bar: title TextView (bold, file name), Button **"Edit"**, Button **"Save"** (shows **"Saving…"** while saving).
  * Indeterminate ProgressBar.
  * Read-only body: monospace, selectable TextView inside a ScrollView.
  * Edit body: a full-screen multi-line `android.widget.EditText`.
  * Status TextView: **"Opening file…"**, **"This file is empty."**, or an error.
* Edit is shown when server `can_edit` && Drive posting right && not a version. Save is shown only while editing.
* Save (`PUT /files/:id/text?content_time=`) creates a version each time.
  * Toast **"Saved"**, or **"No changes to save"**.
  * Error toasts:
    * "This text is too large to save."
    * "You can't edit this file."
    * "This file can't be opened as text."
    * "You don't have access to this file."
    * "This file no longer exists."
    * "Couldn't save. Please try again."
* Load errors (status text):
  * "This file is too large to open here."
  * the same keys as above
  * "Couldn't open the file. Please try again."
* Conflict (409 `file_changed_since_opened`): dialog **"This file has changed"** / **"Someone saved this file after you opened it."**
  * Buttons **"Save my version anyway"** (positive), **"Load the latest version"** (negative), **"Cancel"** (neutral).
* Back with unsaved text: dialog **"Discard your changes?"** / **"Your changes haven't been saved."**, buttons **"Discard"** / **"Keep editing"**.
* Back after at least one successful save: the **notify prompt** (same strings as §2.6). Otherwise it closes.
* Markdown is shown raw. No renderer ships with the app.

### 4.2 Version history (File details `cardVersions` / `tvVersionsHeader` "Versions (N)" / `rvVersions`)

* Row views are built in code with **no ids**:
  * `tvVersion` **"v3 · Autosaved"** (plus " · <name>" if named). Labels by `save_type`:
    * created → "Created"
    * autosave → "Autosaved"
    * manual → "Saved"
    * exit → "Saved on close"
    * restore → "Restored"
    * upload → "Uploaded"
    * legacy → "Earlier version"
    * unknown → "Saved"
    * missing → no label
  * `tvInfo` "<size>  -  MMM dd, yyyy"
  * `tvSavedBy` **"Saved by <name>"** or **"Saved by <email> (guest)"**
  * `tvSummary`: the server text ("Formatted 12 cells · Resized columns F, G") or **"Cells changed: N"**
  * Buttons (TextViews) **"Open"** (office + text files only), **"Download"**, **"Restore"**. Restore is hidden on the first (latest) row and when there is only one version.
* Restore dialog **"Restore Version"** / "Are you sure you want to restore this version? The current version will be replaced." with **"Restore"** / **"Cancel"**.
* The socket `drive:version:restored` reloads an open File details screen.

### 4.3 Folder ZIP (`bottom_sheet_context_menu.xml`)

* New row **`menuDownloadZip`** (LinearLayout, text **"Download as ZIP"**). Shown only for folders where `DriveZipDownloads.mayZipFolder` holds: the folder's `can_download`, or the user's own folder. Hidden in Search.
* Tap → toast **"Download started"**, then DownloadManager.
* Multi-select `btnBulkDownload`:
  * Files: one goes as itself, two or more as one zip.
  * **Each folder becomes its own zip.**
  * A folders-only selection no longer says there is nothing to download.

### 4.4 Show filter (`myDriveFilterRow` / `spMyDriveFilter`)

* Options in order: **All, Shared by Me, Last 7 days, Large files**.
* New hint `tvMyDriveFilterHint`:
  * Root: "Added in the last 7 days, in My Drive and Shared with me" / "Files of 100 MB or more, in My Drive and Shared with me"
  * In a folder: "Added to this folder in the last 7 days" / "Files of 100 MB or more in this folder"
* At root these give a flat list of everything visible. Large files lists files only. Network-only.

### 4.5 Upload pause / resume (`UploadProgressBottomSheet`, `item_upload_progress.xml`)

* Row ids: `ivThumb`, `tvName`, `tvStatus`, `progressBar`, **`btnPauseResume`** (new ImageButton), `btnAction` (cancel / retry).
* `btnPauseResume` content-desc:
  * **"Pause"** while Queued / Uploading
  * **"Resume"** while Paused, with status **"Paused 45%"** (`Paused %1$d%%`)
  * Hidden while "Finishing…" or Failed
* Toasts: "This upload can't be paused right now." (Box, or past S3). A failed resume puts the row into "Failed: Can't resume this upload. Retry to upload it again."
* Statuses that already existed: "Queued", "Uploading N%", "Finishing…", "Failed: …".

### 4.6 "Can edit" emailed share links (ShareFragment, link tab)

* `spPermission` offers **"View only (no download)", "View + download"**, plus **"Can edit"** only for **xlsx, xlsm, docx, pptx**.
* With Can edit, `spExpires` drops "Never" (24 hours / 7 days / 30 days). Generating with no recipient gives the toast **"Add at least one email to share with edit access."**
* Server errors:
  * "Links that allow editing must expire within 30 days."
  * "Only .docx, .xlsx, .xlsm and .pptx files can be shared for editing."
  * "Sharing for editing is turned off."
  * "You need edit access to share this file."
  * "This file no longer exists."
* Active links (`emailLinksContainer`):
  * Badge **"Can edit"** / "View only" / "View + download".
  * Edit links have **no copy icon**.
  * Edit links show **"Edits: N · last <date>"** or **"Not edited yet"**.
  * Copy-link links are tagged **"Anyone with the link"**.
* File details "Share Link Expiry" (Copy link) now offers **1 hour / 24 hours / 7 days** only (30 days removed). The expiry text is "Expires: <date>" from `expires_on`.

---

## 5. What the new code breaks in our automation

`pages/drive/DriveEditorPage.java`:

| Line(s) | Assumption | Status now |
|---|---|---|
| 14-35 (javadoc) | WebView hosting Collabora; "What's New" modal; opens read-only; blue pencil | **Wrong.** Zillit Sheets / Docs / Slides; no modal; `btnEdit` opens editable. |
| 20 | `isEditableFile` includes txt | Wrong. txt / md go to the text editor. |
| 39-48 `EDIT_TOOLS` / `VIEW_TOOLS` | Collabora content-descs ("Undo", "Font Color", "InsertRowsBefore", "Function Wizard", "Open A1 list", "Insert sheet", "Show sheet list", "Show the search bar", …) | **Dead.** The new page's a11y labels are unknown and must be dumped. |
| 54-73 `isEditorActivity` / `waitForEditorActivity` | `editor.OnlyOfficeEditorActivity` | **Still valid.** |
| 76-83 `waitLoaded` | waits for "Edit document" / "Open A1 list" / "Undo" | **Broken.** Times out after 150 s, so every caller `assumeTrue`-skips. Replace with: activity, then "Loading editor…" gone, then `android.webkit.WebView` displayed, and no error text. |
| 87-112 `welcomeDialogShown` / `dismissWelcomeDialog` | "Collabora Online Development Edition"; blind tap at 90.5%×26.8% | **Obsolete, and harmful:** the blind tap lands on the live page. Delete it. |
| 114 `isReadOnly` | "Edit document" control | **Broken.** Use the native Button "Edit" (view + may edit) or the entry point. |
| 124-140 `enterEditMode` | taps "Edit document", waits for "Undo" / "Font Color" | **Broken.** Open via `btnEdit`, or tap the native "Edit" Button in view mode and wait for the reload. |
| 181-205 `addressBox` / `contentInput` | `pos_window-input-address`, wide EditText formula bar | **Dead** (Collabora DOM). |
| 216-352 `gotoCell` / `readCell` / `writeCell` / `writeCellDirect` / `writeCellSendKeys` | depend on the above | **Dead.** Cell I/O must be redesigned (§6). |
| 362-367 `waitForSave` (12 s) | Collabora autosave lag | Partly valid. Autosave still exists; Back now commits and waits 1.5 s itself. |
| 382-400 `acceptEdit` / `waitLeftEditMode` | ✓ at 4.5%×5.5%; "Undo" disappears | **Dead.** No ✓; the blind tap hits the page header. |
| 403-405 `leaveDialogMentionsAutosave` | "saved automatically" in the Leave dialog | **Dead.** Dialog removed 10-16; string unused. |
| 410-415 `cellReference` | first EditText | Dead or meaningless. |
| 417-423 `documentTextShown` / `sheetTabShown` | page text in the a11y tree | **Unverified.** Canvas rendering means cells are likely absent. |
| 428-446 `pressBack` / `leaveDialogShown` / `confirmLeave` / `cancelLeave` | "Leave Editor" / LEAVE / CANCEL | **Dead.** Back closes (view) or shows "Saving…" then maybe the notify dialog (edit). |
| 449-455 `forceExit` | Back plus LEAVE | Works by accident, but does not handle the **notify dialog**. It must tap **"Skip"** (never "Send"). A Back press while the dialog is up cancels it (= Skip), which is OK. Each loop wastes about 8 s on `leaveDialogShown`. |

`tests/drive/DriveEditorTest.java`:

| # | Case | Verdict |
|---|---|---|
| 1 | editButtonShownForSpreadsheet (`btnEdit`) | OK |
| 2 | editButtonHiddenForPdf | OK |
| 3 | editorOpens | Activity check OK; `waitLoaded` **fails** |
| 4 | documentContentRendered ("Schedule" tab / "Scene" cell as text) | **Likely fails** (canvas). Needs dump. |
| 5 | welcomeDialogDismissable | **Obsolete.** Delete. |
| 6 | opensReadOnly (via `btnEdit`) | **Inverted.** `btnEdit` opens EDIT. Re-point to `btnOpen`. |
| 7 | editModeToolbar | **Dead** (Collabora ids) |
| 8 | allEditorToolsPresent | **Dead** |
| 9 | toolbarControlResponds | **Dead** |
| 10 | leaveConfirmationCancel | **Dead.** No dialog. |
| 11 | leaveConfirmed | **Dead** as written; replace with "Back closes" |
| 12 | editCellData | **Dead** (formula bar) |
| 13 | autosaveStated | **Dead** |
| 14 | editPersistsAfterReopen | **Dead** (readCell) |
| 15 | originalDataIntact | **Dead** (readCell) |
| 16 | wordShowsEditButton | OK |
| 17 | wordOpensInEditor ("Zillit QA document" text) | Unverified (canvas?) |
| 18 | wordEditToolbar ("Undo" / "Insert sheet") | **Dead** |
| 19 | wordLeaveConfirmation | **Dead** |

`tests/drive/DriveEditorWriteProbeTest.java`: the whole harness is Collabora-specific (`writeCell*`, `gotoCell`, `acceptEdit` ✓ ratio, `waitLoaded`, `enterEditMode`), so it skips at `waitLoaded`. Retire it, or rewrite it as a probe for the new page's input path.

`tests/drive/DriveCollabEditTest.java`: `openEditorOn` lines 110-138 (`waitLoaded`, `enterEditMode`) and every `readCell` / `writeCell` mean all five cases skip.

`tests/drive/DriveTwoDeviceE2ETest.java` cases 10-12 (lines 410-512): same Collabora chain. They skip at `waitLoaded` / `enterEditMode`.

Other suites (indirect impact):

* **DriveTest** + menu cases (root ~L189, folder ~L516):
  * Ids are still valid.
  * The sheet now also holds the 6-row New group and scrolls. `menuRequestFiles` / `menuViewFileRequests` are pushed lower and may be **off-screen on small phones**, so `fabHasRequestFiles` / `fabHasViewFileRequests` may need a scroll.
  * Add asserts for `menuNewGroup`.
* **DriveViewTest #6 showFilter**: passes. It doesn't assert the options; extend it.
* **DriveShareTest #10 linkSpinnerOptions**: passes, because DOC is a **PDF**, so there is no "Can edit" and expiry still includes Never. A Can-edit case needs xlsx, docx or pptx.
* **DriveCommentsTest** (`zl_drive_note.txt`): the txt detail screen now shows **`btnEdit`** (if canEdit and posting), and `btnOpen` opens the in-app text viewer instead of an external app. Comment-row actions use content-desc, so they are unaffected.
* **DriveUploadTest #9 offlineUploadRetries**: still fine ("Queued" / "Failed" texts unchanged). Rows now also have `btnPauseResume`.
* **DriveItemMenuPage**: the context menu gained `menuDownloadZip` for folders. No existing assert breaks.
* No test references the Copy-link 30-day option, so nothing breaks there.

---

## 6. Proposed new / rewritten cases

Conventions:

* `byAppId(x)` = `com.zillit…:id/x`.
* "native Edit" = `//android.widget.Button[@text='Edit']` while `currentActivity` contains `OnlyOfficeEditorActivity`.
* For text-editor "Edit" / "Save", use the same Button[@text] pattern in `DriveTextFileActivity`.
* Always answer the notify dialog with **Skip** unless the case is about notifying.

### A. Discovery (run first and commit its output)

* **E-00 "Dump the Zillit Sheets / Docs page tree in view and edit mode."**
  * Steps:
    * Open xlsx via `btnOpen`, wait for loaded, save `getPageSource()` to `build/editor-dumps/sheet-view.xml`.
    * Tap native Edit, wait, save `sheet-edit.xml`.
    * Repeat both for docx.
    * Log every `content-desc` / `text` (like `allControls()`).
  * Expected: the files exist. The output becomes the new `EDIT_TOOLS` / `VIEW_TOOLS` and tells us whether "View only", sheet tabs, cell text or a name box are exposed.
  * Note: no `uiautomator dump` during a run (see memory). Use `driver.getPageSource()`.

### B. Office editor: open, mode, close (native-only, can be written now)

* **E-01 Edit opens EDIT mode directly.**
  * Steps: detail xlsx → `btnEdit` → `waitForEditorActivity(30s)` → wait until text "Loading editor…" is gone and a `android.webkit.WebView` is displayed (≤150 s).
  * Expected:
    * no Button "Edit" in the editor
    * no "Collabora" / "Edit document" / "Leave Editor" text anywhere
* **E-02 Open opens VIEW mode with the Edit switch.**
  * Steps: detail xlsx (owner) → `btnOpen` → loaded.
  * Expected: Button "Edit" displayed at the bottom-right (its x is greater than half the screen width and its y is greater than 0.8 of the screen height). Optional, after E-00 confirms it: page text "View only".
* **E-03 The view-mode Edit switch reloads in edit mode.**
  * Steps: E-02, then tap Button "Edit".
  * Expected: "Loading editor…" reappears, then disappears; Button "Edit" is gone; still in `OnlyOfficeEditorActivity`.
* **E-04 Back in view mode closes immediately.**
  * Steps: E-02 → Back.
  * Expected: within 3 s the activity is not the editor and `btnOpen` is visible (File details). "Saving…" is never seen and no dialog appears.
* **E-05 Back in edit mode saves, then closes, with no leave dialog.**
  * Steps: E-01 with no edits → Back.
  * Expected:
    * the text "Saving…" appears (poll every 200 ms)
    * no "Leave Editor"
    * if "Let others know you updated this file?" is absent, the activity closes within 5 s and File details is back
* **E-06 A viewer without edit rights gets no Edit anywhere** (two-device or a pre-shared view-only file).
  * Steps: user B, file shared View only → detail.
  * Expected: `btnEdit` absent. `btnOpen` opens the editor with **no** Button "Edit".
* **E-07 docx and pptx follow the same rules.** Run E-01 / E-02 for `zl_drive_doc.docx` (and a pptx fixture once one is added).
* **E-08 Theme follows the app.**
  * Steps: the app is in dark theme → `btnOpen` → screenshot while "Loading editor…" shows.
  * Expected: corner pixel ≈ #0F1115 (light theme: #FFFFFF).
* **E-09 Rotation does not reload.**
  * Steps: loaded editor → rotate to landscape and back.
  * Expected: "Loading editor…" never reappears; same activity.
* **E-10 Non-editable type error path** (optional): a server-side `file_too_large_to_edit` on a big xlsx shows the server text in the centre TextView and no WebView.

### C. Editing and persistence (rewrite after E-00)

* **E-11 Edit a cell → a new version shows in history.**
  * Steps: E-01 → tap a cell area (coordinate from E-00) → `mobile: shell input text QA<n>` → ENTER → wait 12 s → Back → (Skip if the notify dialog appears) → File details.
  * Expected:
    * `tvVersionsHeader` count increased by ≥1
    * the top row text matches `^v\d+ · (Autosaved|Saved on close|Saved)`
    * `tvSavedBy` = "Saved by <current user>"
    * optionally `tvSummary` "Cells changed: N" or a server line
  * This replaces the formula-bar read-back with a native proof that the page saved.
* **E-12 The edit shows in the reopened file**: only if E-00 shows cell text in the tree. Otherwise verify through version Download and a parsed xlsx (heavy) or leave it manual.
* **E-13 Two-device live edit** (DriveCollabEditTest rewrite): A and B both on `btnEdit`; A types; B observes. Only feasible if E-00 exposes text. Otherwise assert both reach edit mode and both produce versions with their names in "Saved by".

### D. Notify collaborators

* **N-01 Prompt after a real edit.**
  * Steps: E-11 up to Back.
  * Expected:
    * dialog title "Let others know you updated this file?"
    * message "Everyone with access gets a notification."
    * an EditText with hint "Add a note (optional)"
    * buttons "Send", "Skip"
  * Then tap Skip. Expected: the editor closes.
* **N-02 No prompt without an edit**: E-05. Expected: the dialog is never shown.
* **N-03 No prompt in view mode / version / chat**: E-04, V-03, C-01. Expected: no dialog.
* **N-04 Send** (dedicated test project only):
  * Steps: N-01 → type a note → Send.
  * Expected toast: "People with access were notified." (file shared) or "No one else has access to this file." (private).
  * Repeat within 60 s. Expected: "You let them know a moment ago. Try again in a minute."

### F. New blank file

* **F-01 The + sheet shows the New group at root.**
  * Steps: `fabAdd`.
  * Expected: `menuNewHeader` "New" and `menuNewDocument` / `menuNewSpreadsheet` / `menuNewPresentation` / `menuNewMarkdown` / `menuNewText` with texts Document / Spreadsheet / Presentation / Markdown file / Text file (scroll the NestedScrollView if needed).
* **F-02 The New group is hidden at the Shared-with-me root and for a Viewer in a shared folder.** Expected: `menuNewGroup` absent, consistent with `menuCreateFolder` being absent.
* **F-03 Name dialog defaults.**
  * Steps: tap Spreadsheet.
  * Expected: title "Spreadsheet", message "Name", EditText text "Untitled spreadsheet" (selected), TextView ".xlsx", buttons "Create" / "Cancel".
* **F-04 Cancel creates nothing.** Expected: the list is unchanged.
* **F-05 Create xlsx → the editor opens in EDIT mode.**
  * Steps: name `QA_New_<stamp>` → Create.
  * Expected: `OnlyOfficeEditorActivity` loads, with no Button "Edit" (edit mode). Back (Skip if asked) → the list shows `QA_New_<stamp>.xlsx`, and its detail shows `cardVersions` with "v1 · Created".
* **F-06 Same for Document (.docx) and Presentation (.pptx).**
* **F-07 Typed extension is stripped**: type "QA_Ext.xlsx" for a Spreadsheet → file "QA_Ext.xlsx", not "QA_Ext.xlsx.xlsx".
* **F-08 Emptied name falls back**: clear the box → Create → the file is named "Untitled spreadsheet.xlsx" (or the server's de-duplicated name; record it).
* **F-09 Create inside a folder** lands in that folder and not at root.
* **F-10 Markdown / Text → text editor, already editing.**
  * Steps: Markdown file → Create.
  * Expected: activity `text.DriveTextFileActivity`, an EditText displayed, Button "Save" displayed, Button "Edit" absent.
* **F-11 Cleanup** of the created `QA_New_*` files.

### T. Text / Markdown editor

* **T-01 Open shows read-only text**: upload or ensure `zl_drive_note.txt` → detail → `btnOpen`. Expected: `DriveTextFileActivity`, the title equals the file name, the fixture's text is in a TextView, no EditText, Button "Edit" present (owner).
* **T-02 Edit, then Save makes a version.**
  * Steps: Edit → `sendKeys` append "QA<n>" (a native EditText, so `sendKeys` works) → Save.
  * Expected: toast "Saved". Back → the notify dialog (Skip) → detail `rvVersions` top row "vN · Saved".
* **T-03 Save without changes**: Edit → Save. Expected toast "No changes to save".
* **T-04 Discard prompt.**
  * Steps: Edit → type → Back.
  * Expected: "Discard your changes?" / "Your changes haven't been saved." / "Discard" / "Keep editing".
  * "Keep editing" stays in the editor; "Discard" closes and the text is unchanged on reopen.
* **T-05 Persistence**: after T-02, reopen. Expected: the TextView contains "QA<n>".
* **T-06 Conflict** (two devices): A and B open Edit; A saves; B saves. Expected on B: dialog "This file has changed" with "Save my version anyway" / "Load the latest version" / "Cancel".
* **T-07 txt detail shows btnEdit** for the owner, and `btnOpen` does not leave the app.

### V. Version history

* **V-01 Row format**: for any version row, `tvVersion` matches `^v\d+( · (Created|Autosaved|Saved|Saved on close|Restored|Uploaded|Earlier version))?`, and "Saved by …" is shown.
* **V-02 Restore hidden on the latest row; Restore dialog.** Expected: the first row has no "Restore". On row 2, "Restore" shows "Restore Version" / "Are you sure you want to restore this version? The current version will be replaced." Confirm → a new top row "· Restored".
* **V-03 Open a version (office).**
  * Steps: row 2 "Open".
  * Expected: `OnlyOfficeEditorActivity`, no Button "Edit". Back closes instantly with no "Saving…" and no dialog.
* **V-04 Open a version (txt).** Expected: `DriveTextFileActivity`, title "<name> · version N", no Button "Edit".

### Z. ZIP, filters, uploads, share

* **Z-01 Folder ⋯ shows "Download as ZIP"** for an own folder (`menuDownloadZip`). Tap → toast "Download started".
* **Z-02 A file ⋯ has no `menuDownloadZip`.** Search results ⋯ have none either.
* **Z-03 Multi-select of folders only → Download** gives no "nothing to download" message (toast "Download started", or a DownloadManager notification).
* **S-01 Show filter options**: tap `spMyDriveFilter`. Expected options exactly All / Shared by Me / Last 7 days / Large files.
* **S-02 Last 7 days at root**: `tvMyDriveFilterHint` = "Added in the last 7 days, in My Drive and Shared with me", and a file uploaded this run is listed. Inside a folder: "Added to this folder in the last 7 days".
* **S-03 Large files**: hint "Files of 100 MB or more, in My Drive and Shared with me". Small fixtures are absent and no folders are listed.
* **U-01 Pause / Resume.**
  * Steps: upload a large file → open the uploads sheet → `btnPauseResume` content-desc "Pause" → tap.
  * Expected: `tvStatus` matches "Paused \d+%", content-desc "Resume". Tap → "Queued" / "Uploading N%" and the file eventually appears.
  * Needs a file large enough to catch mid-upload.
* **U-02 Cancel a paused row** removes it.
* **L-01 "Can edit" appears only for office formats**: share link tab on xlsx → `spPermission` options include "Can edit". On the PDF → absent.
* **L-02 Can edit removes Never**: choose Can edit. Expected `spExpires` = 24 hours / 7 days / 30 days.
* **L-03 Can edit needs a recipient**: empty `etRecipients` → `btnGenerateEmailLink`. Expected toast "Add at least one email to share with edit access."
* **L-04 Can edit link row**: with a recipient. Expected: badge "Can edit", "Not edited yet", no copy icon on that row.
* **L-05 Copy-link expiry**: detail share → "Share Link Expiry" offers 1 hour / 24 hours / 7 days only.

### C. Chat attachments (cross-module)

* **C-01 C&C chat xlsx opens in the editor read-only.**
  * Steps: send `zl_drive_book.xlsx` in a C&C chat → tap the attachment.
  * Expected: `OnlyOfficeEditorActivity` loads, no Button "Edit". Back closes instantly with no dialog.
* **C-02 A PDF in chat still opens outside** (activity or package changes away from `OnlyOfficeEditorActivity`).
* **C-03 Same for a unit chat** (Home / Catering / Production report).

### Fixtures and wiring

* Add a `.md` fixture (e.g. `zl_drive_note.md`) and a `.pptx` fixture. `zl_drive_note.txt` already exists.
* Large-file filter and pause cases need a file of 100 MB or more, or at least one that takes several seconds to upload.
* Keep the `--drive-editor` / `--drive-collab` / `--drive-editor-probe` runner flags and add `.run` entries for any new class, per the "wire new modules into the runner" rule.
* Suggested split:
  * `DriveEditorTest`: B, C, D
  * `DriveNewFileTest`: F
  * `DriveTextEditorTest`: T
  * `DriveVersionsTest`: V
  * extend `DriveViewTest`, `DriveUploadTest` and `DriveShareTest` with S, U and L
  * `DriveChatEditorTest` or the CC suite: C

---

## 7. Open questions (need a live device)

1. The a11y tree of Zillit Sheets / Docs / Slides: toolbar labels, whether "View only" and the title are exposed, sheet-tab and cell text, any name box (E-00).
2. Whether `adb shell input text` reaches the page's hidden input after a tap on a cell (the code says the WebView takes focus on the first touch).
3. The server's duplicate-name behaviour for `POST /files/new`.
4. Whether the installed device build includes the Nov 19-23 merges. Check the app version, or simply check that `menuNewGroup` exists.
