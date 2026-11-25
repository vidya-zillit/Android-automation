# Chat document → editor (view mode): code map for automation

Source: `Zillit-Android` at `origin/development` (tip cc3dc2a96), read with `git show` / `git grep` only.
Automation: `Android-automation` working tree (branch vidya_bhavik).

---

## 0. Merge history (read this first)

- **f824d9211** (feat/drive-web-parity) added the code that survived: `ChatAttachmentEditor.kt` with
  `Context.openChatAttachment(...)`, `ChatEditorSource` (unit keys), and the chat-source mode in
  `OnlyOfficeEditorActivity`.
- **504942bef** added `OtherChatEditorUnits` (Location / Casting / Wardrobe).
- **b6fffc15d** (vidya_AI/email_editor_update) added a **second, parallel** implementation,
  `ChatFileEditorOpener.kt` + `openChatFileFromUrl(...)`. In merge **59332b63b** ("Merge
  origin/feat/drive-web-parity (PR #5)") that file was **deleted**, and every call site was resolved to
  f824d9211's `openChatAttachment`. On development, `ChatFileEditorOpener` / `openChatFileFromUrl`
  **do not exist**. Both versions used the same type list, so the behaviour is the same.
- **Regression in that same merge:** f824d9211 had wired `FormChatPage` (the form/contract chat window)
  to `openChatAttachment`. The merge put `FormChatPage` back to `openFileFromExternalFromUrl`, so on
  development it **opens documents in another app** (see §2, gaps). `git show 59332b63b^2:<FormChatPage>`
  has 2 `openChatAttachment` hits; `59332b63b` and `origin/development` have 0.

---

## 1. Shared entry point

### 1.1 `Context.openChatAttachment(data, remoteStorageOptions, messageId, unit = null, isShowToast = false)`
File: `app/src/main/java/com/zillit/zillitapp/bottomNav/tools/drive/presentation/editor/ChatAttachmentEditor.kt`

Decision (`ChatAttachmentEditor.canOpen`). The editor is tried only when **all** of these hold:
1. The storage is **not Box** (`remoteStorageOptions != BoxUploading`). A Box project always opens the file in another app.
2. The extension of `data.name`, or of `data.media` when the name is blank, is in `EDITOR_EXTENSIONS`:
   `xlsx xlsm xls ods csv | docx doc odt rtf | pptx ppt odp`.
3. `messageId` is a stored 24-hex Mongo `_id`. A message still uploading or unsent falls back.
4. `unit == null` (C&C), or `unit` is one of `ChatEditorSource.UNITS`.
5. A static tap guard (`TAP_GUARD_MS = 1500`) drops repeat taps within 1.5 s. This guard is **global**, so
   any two opens less than 1.5 s apart, even in different chats, open only once.

If all hold, the chat screen first calls Drive **before** any new screen appears:
- C&C: `GET {DRIVE_EDITOR}/chat/<messageId>/config`
- unit chats: `GET {DRIVE_EDITOR}/unit-chat/<unit>/<messageId>/config`

On success it calls `startActivity(OnlyOfficeEditorActivity)` with these extras:
`chatMessageId`, `chatUnit` (absent for C&C), `fileName`, `mode=view`.
On **any** failure (deleted message, too large, editors down, offline, unknown unit) it falls back to
`openFileFromExternalFromUrl` **silently**, with no toast or error.

### 1.2 Everything else: `openFileFromExternalFromUrl` (DownloadingHelper.kt:714)
This downloads to the cache, then `openFileFromExternal` sends `Intent.ACTION_VIEW` through the FileProvider
with `FLAG_ACTIVITY_NEW_TASK`. The result is **another app** (a system chooser, Drive/Docs/PDF viewer, WPS, …).
If no app can open the file, a toast shows: *"You may don't have proper application for viewing this file."*
(`no_application_to_open_file`).

### 1.3 Fixture types

| Fixture (runner path) | Ext | From a chat it opens in | Notes |
|---|---|---|---|
| `zl_drive_doc.docx` (/sdcard/Download) | docx | **Editor, view mode** ("Docs") | primary positive fixture |
| `zl_drive_book.xlsx` (/sdcard/Download) | xlsx | **Editor, view mode** ("Sheets") | second positive fixture |
| `zl_test_doc.pdf` (/sdcard/Documents) | pdf | **another app** (ACTION_VIEW) | negative control. Drive's own `DrivePdfViewerActivity` is **not** used from chats |
| `zl_drive_note.txt` (/sdcard/Download) | txt | **another app** | not in EDITOR_EXTENSIONS. Drive's `DriveTextFileActivity` (b5ae04154) is **not** used from chats |
| `zl_drive_sheet.csv` (also pushed) | csv | would be the editor, **but it can't be sent**: the chat Document picker (`FilePickerContract` default MIME list: doc/docx/ppt/pptx/ppsx/xls/xlsx/text/plain/pdf) excludes text/csv | skip |
| `.md` | md | another app | not an editor type |

### 1.4 How view-only is enforced, and what you can see
`OnlyOfficeEditorActivity` (`.../drive/presentation/editor/OnlyOfficeEditorActivity.kt`; manifest
`exported=false`). The whole layout is **built in code: there are no resource ids**. It has a FrameLayout with
a WebView, a horizontal ProgressBar, a loading TextView, and a bottom-end LinearLayout holding a native `Button`.

- When `chatSource != null`, it always calls `loadEditor(null, MODE_VIEW)`. `reopenEditor` also forces VIEW
  for a chat source. That is the enforcement: the URL gets `&mode=view`, and the server issues a
  view token (comment: "the server sends canSwitchToEdit false anyway").
- `canSwitchToEdit = source == null && …`, so for a chat file the app's **Edit button is always GONE**
  (`viewOnlyBar` and `btnSwitchToEdit`, text = `R.string.edit` = **"Edit"**, class `android.widget.Button`).
- `supportActionBar?.hide()`: no native toolbar, no title, no back arrow.
- A code comment says the "View only" notice and the title are drawn **by the editor web page**
  ("on phones the editor page shows its own title and 'View only' notice"). The app does not draw them.
- Strings (values/strings.xml):
  - `drive_loading_editor` = **"Loading editor…"** (U+2026 ellipsis), shown until the page loads
  - `drive_failed_to_load_editor` = **"Failed to load editor"**
  - `drive_failed_to_load_editor_format` = **"Failed to load editor: %1$s"**
  - `drive_ssl_cert_error` = "SSL certificate error"
  - `drive_saving` = "Saving…" (edit mode only; must NOT appear on Back from a chat file)
- **Back:** `closeEditor()` calls `finish()` **immediately** when `currentMode == MODE_VIEW` (or the WebView
  isn't visible yet). There is no "Leave Editor" dialog, no "Saving…", and no "Let others know you updated this
  file?" prompt (that prompt is skipped when `chatSource != null`). **One BACK returns to the chat.**

---

## 2. Per chat module (development)

Tap targets come from the layouts:
- Unit chats use `HomeChatAdapter` → `AttachmentVH` → `chat_attachment_view.xml`. The click listener
  (`CHAT_ENUM.CLICK` → `handleClick`) is attached **only** to `landScapeView`, `portraitView`, `centerView`
  and `ivPlay` (all inside `imageParent`). An Office doc has no width/height, so it renders in **`centerView`**.
  `attachmentType` (type-icon overlay) and `fileNameTv` have **no** click listener. Tapping the centre of
  `imageParent` (or `centerView`) is the reliable target.
- C&C uses `ChatAndGroupAdapter` → `Mine*/Other*ImageHeight…VH` → `rv_item_image_video_*.xml`. The click
  is on **`srcImage`** (inside `mainCardVw`), wired by `handleImageAndShowInView` → `handleImageClickListener`.

The long-press menu has **no "Open"/"View" entry**: the `Options` enum is Save, Delete, Forward, ImageReply,
FileCabinet, Reply, ReplyAll, Print, Copy, Share, ReadByUser, EditComment, Edit, Search, Translate, Move,
Distribute, DistributeToDD, SetPrimary, RemovePrimary, AddToContacts, Gallery. Opening a document is a **single tap** only.

### 2.1 Chats that open Office docs in the editor (view mode)

| Module | Screen (class / activity) | `unit` key | Tap target | Notes |
|---|---|---|---|---|
| **Home units**: Bulletin (notices), **Call Sheet**, every other Home unit tab | `Home` fragment (bottom-nav Home; `Home.kt:1851` `handleClick`) | `home` | newest `imageParent` / `centerView` | Call Sheet's picker is Document-only. `fileNameTv` ("File: <name>") is visible only in Call Sheet and Production Report units, and it is **not clickable**. `onDownloadDone` also calls `handleClick`, so a finished download auto-opens |
| **C&C 1:1** | `ChatAndGroupPage` (`HoldersViewhandler.handleImageClickListener`, l.324) | `null` → `/editor/chat/<id>` | newest `srcImage` in `mainCardVw` | |
| **C&C group** | same `ChatAndGroupPage` | `null` | same | |
| **Info** (Tools) | `ConfidentialInfo` activity, identifier `info_tool` | `info` | `imageParent`/`centerView` | `infoChatUnit()`: the **Script notes / Reports tabs return null → another app** |
| **Confidential Info** (Tools) | `ConfidentialInfo` activity, identifier `confidential_info_tool` | `confidential_info` | same | same tab caveat |
| **Catering** | `CateringActivity` (l.1753) | `catering` | same | |
| **Accounts** chat | `AccountsActivity` (l.1780) | `accounts` | same | only message attachments. **Comment-thread attachments** (`handleClick` without chatId, l.1299) → another app |
| **Production Report chat** | `ProductionReportChatActivity` hosting `ProductionReportChatFragment` (l.1434). Legacy `ProductionReportChatPage` (l.1506) is also wired | `production_report` | same | automation constant `ProductionReportPage.CHAT_ACTIVITY = "ProductionReportChatActivity"` |
| **Forms & signature discussion** | `FormDiscussionChatPage` (from `CommonListingView`, `Extensions.kt`) (l.1657) | `form_signature` | same | comment attachments (l.1254, no messageId) → another app |
| **Location / Casting / Wardrobe** | `OtherChatsActivity` (l.1400) via `OtherChatEditorUnits.forIdentifier(viewModel.currentIdentifier)` | `location`, `location_unit`, `casting`, `casting_main_unit`, `casting_background_unit`, `wardrobe`, `wardrobe_main_unit`, `wardrobe_background_unit` | same | an unknown identifier → `null` → another app. The Location tool is deferred in this suite (memory), and Casting/Wardrobe are not automated |

### 2.2 Chats and places that still open documents another way (gaps and inconsistencies, possible bugs)

| Where | What it does | Severity |
|---|---|---|
| **`FormChatPage`** (form/contract chat window: `openFormChatWindow` from `DocumentViewIngPage`, `ContractFragment`, `FormFragment`) l.1127 | `openFileFromExternalFromUrl` → **another app**. f824d9211 wired it, and **merge 59332b63b reverted it** | **likely bug** (merge loss) |
| **Chat library**: long-press → *Gallery* → Docs tab (`HomeChatLibraryActivity.openDocExternally` l.240, `ChatLibraryActivity.openDocExternally` l.275) | another app | inconsistency: the same doc opens in the editor from the bubble, and another app from the library |
| **Email** attachments (`EmailAttachmentViewHolder.matCardVw` → `MailingVM.openOrDownloadAttachment` → `openFileFromExternal`) | another app (downloads first, "Loading please wait..." snackbar/toast) | not covered by the change |
| **Deal Memo discussion** `DMDiscussionPage` l.1460 | another app | not covered |
| **Settings → user chat history** `UserChatHistoryPage` l.878/1018 | another app | not covered (b6fffc15d's note: web doesn't either) |
| **File Cabinet chat** `FileCabinetChatPage` l.404, `ApproveUserRequestActivity` l.860 | another app | not covered |
| Info/CI **Script notes & Reports** tabs | another app (deliberate: "no Drive editor route") | by design |
| Accounts / Form-discussion **comment** attachments | another app (deliberate) | by design |
| `SentNdaHolder` (forms) uses `handleImageAndShowInView` → C&C path with `unit=null` | asks `/editor/chat/<id>` for a non-C&C message → most likely fails → another app | minor |
| Any **Box** project | always another app (`canOpen` false) | by design |
| **PDF / TXT / MD** in any chat | another app, while Drive opens them in its own `DrivePdfViewerActivity` / `DriveTextFileActivity` | product inconsistency, not a code bug |
| Calendar | has no chat (automation already skips "calendar" as a unit) | n/a |
| Document Distribution | not a chat, so no `openChatAttachment`. DD's own screens use their own viewers or ACTION_VIEW. A DD send that lands as a C&C message follows the C&C row | n/a |

---

## 3. Automation impact (what is now wrong or weak)

Searched `tests/**` and `pages/**`. **No hard assertion anywhere says a chat document "opens an external
app" or "opens MediaPreview."** `MediaPreviewPage` covers only the Fallery gallery → GalleryItemView → EditImage
**send** flow for images and videos, and none of these use it for documents: MediaPreviewTest, ChatOptionsTest:262,
MediaSystemPickerTest, CcChatMediaTest and ToolChatBaseTest. Those suites only *send* documents (all PDF via
`zl_test`), and PDF behaviour did not change. Specific items:

1. **`HomeChatPage.openNewestDoc()`** (pages/homechat/HomeChatPage.java:874)
   - It taps `fileNameTv`, falling back to `attachmentType`. **`fileNameTv` has no click listener** and is
     visible only in Call Sheet / Production Report units. `attachmentType` has no listener either; it only
     works by falling through to `centerView`. It should tap the newest `imageParent` / `centerView`.
   - It returns `true` once the click call succeeds, whether or not anything opened. Its javadoc
     "(file viewer / download)" is outdated: Office docs now go to `OnlyOfficeEditorActivity`, and other types go to another app.
2. **`TwoUserChatTest.documentOneWay`** → `softOpen(... s.openNewestDoc())` + `closeViewer()`: it only logs and
   sends a PDF, which still goes to another app, so nothing is wrong. If the fixture becomes docx, `closeViewer()` still works:
   it presses BACK until `isInputBarVisible()`, and one BACK finishes the editor in view mode.
3. **`DriveEditorPage`** (pages/drive): its class javadoc and `pressBack()` say BACK raises a "Leave Editor"
   (LEAVE/CANCEL) dialog. In **view mode, BACK finishes immediately** (`closeEditor` → `finish()`), so a chat-file
   check must not wait for or require that dialog. `forceExit()` copes (it only confirms if the dialog shows).
   `isReadOnly()` = `hasControl("Edit document")` is the **wrong read-only signal for a chat file**: the token
   is view-only (canSwitchToEdit false), so the "Edit document" pencil should be **absent**. Verify this on the first run.
4. `CcChatThreadPage` has no open-document helper. `cardBubbleCount()` counts `mainCardVw`, which also matches
   text message cards (`MineMessageVH` binds `mainCardVw`), so "newest card" ≠ "newest doc". Restrict to cards
   that contain a `srcImage`.
5. CallSheetTest, CallSheetUploadModeTest, AllUnitsMediaTest, Email*, DocDist*: they never open a chat document,
   so nothing breaks. DocDistWatermarkTest's ACTION_VIEW handling is a DD preview and is unrelated.

**Net:** nothing currently goes red because of this change. The gaps are (a) no coverage of the new
behaviour, and (b) `openNewestDoc` targets the wrong view and can't fail.

---

## 4. Proposed reusable check

### 4.1 Page object: `pages/common/ChatDocumentEditorPage.java` (reuses DriveEditorPage's WebView helpers)

```java
public class ChatDocumentEditorPage extends BasePage {
    public static final String EDITOR_ACTIVITY = "editor.OnlyOfficeEditorActivity"; // contains()
    static final String LOADING  = "Loading editor…";
    static final String FAILED   = "Failed to load editor";      // prefix also covers ": %s"
    static final String NO_APP   = "You may don't have proper application for viewing this file.";

    /** Poll up to ~25 s: the chat fetches the Drive config BEFORE the activity starts. */
    public Result waitForOutcome(Duration t)  // EDITOR | EXTERNAL_APP(pkg) | NO_APP_TOAST | NOTHING
    public boolean waitLoaded(Duration t)     // WebView present && LOADING gone && no FAILED text
    public boolean editButtonShown()          // //android.widget.Button[@text='Edit'] (app-drawn) -> must be false
    public boolean editToolsShown()           // any of DriveEditorPage.EDIT_TOOLS ("Undo","Font Color",...) -> must be false
    public boolean editPencilShown()          // accessibilityId "Edit document" -> expected false (verify live)
    public boolean viewOnlyNoticeShown()      // textContains("View only") in the WebView tree (soft, verify live)
    public boolean failedText()               // textStartsWith(FAILED)
    public boolean backToChat(Supplier<Boolean> chatVisible) // ONE back; no "Leave Editor"/"Saving…"; chatVisible within 5 s
}
```

Outcome detection, in this order on each poll:
1. `driver.currentActivity()` contains `editor.OnlyOfficeEditorActivity` → EDITOR
2. `getCurrentPackage() != appPackage` → EXTERNAL_APP (the config call failed, or the type is not an editor type). Then `returnToAppUnderTest()`.
3. the NO_APP toast or text → NO_APP_TOAST

When the outcome is EXTERNAL_APP for a docx, report it as **"Drive refused the editor config (fallback)"**, not
as "editor didn't open". The fallback is silent, so this is the only diagnostic you get.

Dismiss the editor's first-open "What's New" modal with `DriveEditorPage.dismissWelcomeDialog()` before the
toolbar checks.

### 4.2 Assertions per case ("open doc → editor read-only → back")
Hard:
1. Outcome == EDITOR within 25 s (`currentActivity` contains `OnlyOfficeEditorActivity`).
2. `android.webkit.WebView` present, and "Loading editor…" gone within 45 s.
3. No text starting with "Failed to load editor".
4. **No** `android.widget.Button` with text "Edit" (app-drawn switch; GONE for chat files).
5. **None** of `DriveEditorPage.EDIT_TOOLS` present ("Undo", "Redo", "Font Color", "InsertRowsBefore", …).
6. ONE `navigate().back()` → no "Leave Editor"/"LEAVE" and no "Saving…" → the chat screen is back within 5 s
   (see 4.4 for each module's "back in chat" signal), and the bubble count is unchanged.

Soft (log, and promote to hard after one green live run):
7. `hasControl("Edit document")` == false.
8. "View only" text visible (drawn by the web page).
9. The file name (e.g. "zl_drive_doc") visible in the page title.

Negative control (one module is enough, Home/Bulletin): send `zl_test_doc.pdf` and tap it. Expect EXTERNAL_APP
or NO_APP_TOAST, **never** EDITOR. Then `returnToAppUnderTest()`.

### 4.3 Helpers to add or fix in page objects
- `HomeChatPage.openNewestDocBubble()`: tap the last `imageParent` that has an `attachmentType` sibling
  visible (doc, not image), else the last `centerView`. Replace `openNewestDoc()`'s `fileNameTv` target.
- `CcChatThreadPage.openNewestDocBubble()`: the last `mainCardVw` that contains `srcImage` and no `playIcon`,
  then tap its `srcImage`.
- Send with the existing `tapAttachment()` → `tapAttachmentOption("Document")` → `pickSystemFileByName("zl_drive_doc.docx")`.
  Use the **full name**: Download also holds `zl_dd_word.docx` / `zl_dd_book.xlsx` copies, which the runner
  touches to be newest. Then wait for `isDocBubbleShown` and wait for the upload to finish
  (`attachmentSendState()=="sent"`). An unsent message has no 24-hex `_id` and falls back to another app.
- Wait ≥ 1.6 s between two opens (global tap guard).

### 4.4 Per-module test plan (one test per module; base class `ChatDocEditorBaseTest`)

| Test | Reach the chat | Fixture | "Back in chat" signal |
|---|---|---|---|
| Home – Bulletin | `HomeChatPage.selectUnit("Bulletin")` | `zl_drive_doc.docx` (+ PDF negative control) | `bottomNavigation` + `rvChat` |
| Home – Call Sheet | `selectUnit("Call Sheet")`; handle the call-sheet first popup / UploadMode sheet | `zl_drive_book.xlsx` | same; `fileNameTv` "File: zl_drive_book.xlsx" visible |
| C&C 1:1 | `CcBaseTest.openOneToOne()` | `zl_drive_doc.docx` | activity contains `ChatAndGroupPage` + `etMessage` |
| C&C group | `openOrCreateTestGroup()` | `zl_drive_book.xlsx` | same |
| Info | ToolChatBaseTest route, "Info" (main tab, not Script notes/Reports) | `zl_drive_doc.docx` | `rvChat`, no bottom nav (`inModuleScreen`) |
| Confidential Info | same, "Confidential Info" | `zl_drive_book.xlsx` | same |
| Catering | CateringTest route (pick a recipient first, `afterModuleReady`) | `zl_drive_doc.docx` | same |
| Production Report chat | ProductionReportBaseTest → chat workspace | `zl_drive_doc.docx` | `activityIs(ProductionReportPage.CHAT_ACTIVITY)` |
| Forms & signature discussion (optional) | `FormDiscussionChatPage` | `zl_drive_doc.docx` | activity contains `FormDiscussionChatPage` |
| Location/Casting/Wardrobe | **skip**: Location tool is deferred, and Casting/Wardrobe are not automated | — | — |
| **Bug probes** (expected to fail until fixed; mark them as known issues) | `FormChatPage` (form/contract chat), Gallery→Docs library in Home | `zl_drive_doc.docx` | outcome should be EDITOR; today it is EXTERNAL_APP |

Preconditions to `assumeTrue`: the project is not Box storage; the user has posting rights; the device is online
(`/editor/...config` must answer).

Wire-up per memory: add a `TestOrder` constant and a `.run` entry so it shows in Android Studio.
