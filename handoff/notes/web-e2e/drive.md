# Web E2E — Drive

Source: Zillit E2E Report (generated 2026-11-17T10:27:04.399Z, env dev.zillit.com).
Coverage: deep — Browse, upload, preview, editor, sharing, links, requests, rights, two users

## Web test cases (326)

### Sheet bugs — Drive

- **Folder Info never reports "0 files, 0 folders" for a folder holding 2 subfolders and 2 files** — passed (sheet-bugs/drive.spec.js:66)
  - Where: Tools → Drive → My Drive → “[e2e] drive …” → “e2e-drive-…-area-sheet-bugs” · account: iPhone XRR
  - Setup: Opens the run’s area folder “e2e-drive-…-area-sheet-bugs” (created on first use).
  - Steps: Create a folder “e2e-drive-…-drive01” with Create Folder → Open it, create two subfolders in it and upload two PNG images → Check that the folder lists 4 items, then go back one level with the back arrow → Open the folder’s ⋯ menu and click Info → Check that the Info drawer is titled with the folder’s name and its details say Folder → Check that the details never say 0 files, 0 folders or 0 items → If counts are shown, check they read 2 files, 2 folders or 4 items → Close the Info drawer
  - Expected: Folder Info never reports a zero count for a folder that has content: the counts are either right (2 files, 2 folders) or not shown.

- **opening a starred folder from the Starred filter shows a breadcrumb trail to it** — failed (sheet-bugs/drive.spec.js:116)
  - Where: Tools → Drive → My Drive → “[e2e] drive …” → “e2e-drive-…-area-sheet-bugs” · account: iPhone XRR
  - Setup: Opens the run’s area folder and creates a folder “e2e-drive-…-drive02” holding one subfolder “e2e-drive-…-drive02-child”.
  - Steps: Open the folder’s ⋯ menu and click “Add to favorites” → Click the star filter button in the toolbar (Show favorites) → Check that the starred folder is listed → Open the folder from the starred view → Check that the breadcrumb shows a full trail with no blank part: “Drive”, the run folder, the area folder, then the folder → Check that there is exactly one back arrow → Note (not checked) whether the star filter is still on and whether the unstarred subfolder is shown → Click the back arrow and check that the folder is listed again
  - Expected: Opening a starred folder shows a breadcrumb trail to it. On web Starred is a filter on My Drive, not a section of its own, so the trail starts at “Drive”, not “Favorites” (by design).
  - Web error: Error: expect(locator).toBeVisible() failed

### Drive — landing and editor (ZL-21229)

- **lands on "Shared with me", or on My Drive when nothing is shared @drive** — passed (drive.spec.js:97)
  - Where: Tools → Drive (a fresh visit) · account: iPhone XRR
  - Steps: Open Drive → Note which section tab is active: Shared with me or My Drive → If it is Shared with me, look at the Folders tab and then the Files tab → Check that at least one item is listed on one of them → If it is My Drive instead, click Shared with me → Check that both the Folders and the Files tabs are empty
  - Expected: Drive opens on Shared with me when something is shared with the account; it opens on My Drive only when Shared with me has nothing in it.

- **a section the user picked is restored on re-entry, not overridden @drive** — passed (drive.spec.js:116)
  - Where: Tools → Drive → My Drive · account: iPhone XRR
  - Steps: Open Drive and click My Drive → Stay on the Folders tab → Click the first folder to open it → Leave Drive for the Film Tools page → Open Drive again → Check that My Drive is the active section → Check that no “no longer exists” message appears
  - Expected: Drive reopens on My Drive, the section the user was in, and shows no “no longer exists” message.

- **clicking an editable file shows the three-dot actions @drive** — passed (drive.spec.js:143)
  - Where: Tools → Drive, Files tab at the root (Shared with me or My Drive) · account: iPhone XRR
  - Steps: Open Drive and find an Office file (Word, Excel, PowerPoint, OpenDocument or CSV) on the Files tab of the opening section, else of My Drive, else of Shared with me → Click the file’s name → Check that an action menu opens with “Preview” and “Open in Editor” (or “View in Editor”) → Check that the editor did not open behind the menu → Press Escape and check that the menu closes → Click the file’s name again, then click the row’s three-dot button → Check that only one menu is open → Click the search box and check that the menu closes
  - Expected: Clicking an Office file opens its action menu, not the editor; the menu closes on Escape and on an outside click, and the three-dot button never opens a second menu.

- **an Office file opens in the live editor, token kept out of the URL @drive** — passed (drive.spec.js:173)
  - Where: Tools → Drive, Files tab at the root (Shared with me or My Drive) · account: iPhone XRR
  - Steps: Open Drive and find an Office file on the Files tab of the opening section, else of My Drive, else of Shared with me → Click the file’s name → Click “Open in Editor” (or “View in Editor”) → Wait for the live editor to appear → Check the address of the embedded editor page → Check that it carries no access token and no file-source (WOPI) value
  - Expected: The Office file opens in the live editor, and the editor page’s address never exposes the access token or the file source.

- **the editor has a Back button that returns to the file list @drive** — passed (drive.spec.js:194)
  - Where: Tools → Drive, Files tab at the root (Shared with me or My Drive) · account: iPhone XRR
  - Steps: Open Drive and find an Office file on the Files tab of the opening section, else of My Drive, else of Shared with me → Click the file’s name and click “Open in Editor” (or “View in Editor”) → Wait for the editor to open → Check that the editor header shows a “Back to Drive” button → Check that the button sits fully inside the Drive window → Click “Back to Drive” → Check that the editor closes and the file list is shown again
  - Expected: The editor has a visible “Back to Drive” button inside the Drive window, and pressing it closes the editor and returns to the file list.

### Drive — shell, navigation and filters

- **opens by route: window, banner and the landing requests @smoke @drive** — passed (drive.spec.js:266)
  - Where: Tools → Drive (opened from its direct link) · account: iPhone XRR
  - Steps: Open Drive from its direct link → Check that Drive loads the shared folders (with their sizes), the shared files at the root, your favourites, the project’s tags and the list of people with Drive access → Check that the banner reads “Z-Drive” → In Windowed view, check that the window title reads “Drive” → In Windowed view, check that exactly one “Drive” tab is open in the window tab bar
  - Expected: Drive opens with the “Z-Drive” banner, makes its five landing loads, and in Windowed view shows one window and one tab named “Drive”.

- **opens from the Film Tools tile @smoke @drive** — passed (drive.spec.js:303)
  - Where: Tools (the Film Tools page) · account: iPhone XRR
  - Steps: Open the Film Tools page → Find the Drive tile → Click the tile’s title → Check that Drive opens with its My Drive and Shared with me tabs
  - Expected: Clicking the Drive tile on the Film Tools page opens Drive.

- **"Back to Film Tools" leaves Drive (closes the window in MDI) @smoke @drive** — passed (drive.spec.js:319)
  - Where: Tools → Drive · account: iPhone XRR
  - Steps: Open Drive → Click the “Back to Film Tools” arrow in the Drive banner → Check that Drive closes (its My Drive and Shared with me tabs are gone)
  - Expected: “Back to Film Tools” leaves Drive; in Windowed view the Drive window closes.

- **re-entering at the root lands on Shared with me again @smoke @drive** — passed (drive.spec.js:327)
  - Where: Tools → Drive → My Drive · account: iPhone XRR
  - Steps: Open Drive and click My Drive (without opening a folder) → Click “Back to Film Tools” → Check that Drive closes → Open Drive again → Check that Drive loads the Shared with me list first
  - Expected: Reopening Drive at the root starts on Shared with me again; My Drive is remembered only when a folder was open.

- **My Drive shows the add actions and quick filter; the Shared root does not @smoke @drive** — passed (drive.spec.js:343)
  - Where: Tools → Drive → My Drive, then Shared with me · account: iPhone XRR
  - Steps: Open Drive and click My Drive → Check that no folder path bar is shown at the root → Check that the All / Shared by Me filter is shown → Check that Upload, New, Create Folder and Request Files are shown → Click Shared with me → Check that the All / Shared by Me filter, Upload, New, Create Folder and Request Files are all hidden
  - Expected: My Drive shows the add actions and the All / Shared by Me filter; the Shared with me root shows none of them.

- **inner tabs split root folders and files, and hide during search @smoke @drive** — passed (drive.spec.js:364)
  - Where: Tools → Drive → My Drive · account: iPhone XRR
  - Steps: Open Drive and click My Drive → Check that the Folders tab is selected → Click Files and check that it is selected and no folder rows are listed → Click Folders and check that no file rows are listed → Type a search that matches nothing (“zz-e2e-none-” plus a number) in the search box → Check that the Folders and Files tabs are hidden → Clear the search box → Check that the Folders and Files tabs are back
  - Expected: At the root the Folders tab lists only folders and the Files tab only files, and both tabs hide while a search is active.

- **opens a folder, then climbs out by back arrow and root crumb @smoke @drive** — passed (drive.spec.js:385)
  - Where: Tools → Drive → My Drive, Folders tab · account: iPhone XRR
  - Steps: Open Drive, click My Drive and stay on the Folders tab → Click the first folder → Check that the path shows two crumbs, the last being the folder’s name, and the Folders / Files tabs are gone → Click the back arrow beside the path → Check that Drive returns to the My Drive root (the path bar is gone) → Open the same folder again → Click the first crumb in the path → Check that Drive is back at the root
  - Expected: Opening a folder shows a two-crumb path, and both the back arrow and the first crumb return to the root.

- **the row menu stays inside the window and closes on Escape and outside click @smoke @drive** — passed (drive.spec.js:416)
  - Where: Tools → Drive → My Drive, Folders tab (Files tab when there are no folders) · account: iPhone XRR
  - Steps: Open Drive and click My Drive → Open the three-dot menu of the first listed item → In Windowed view, check that the menu fits inside the Drive window → Press Escape and check that the menu closes → Open the same menu again → Click the search box → Check that the menu closes
  - Expected: The row menu opens inside the Drive window and closes on Escape and on a click outside it.

- **column sort goes to the server for files and cycles asc → desc → off @smoke @drive** — passed (drive.spec.js:442)
  - Where: Tools → Drive → My Drive, Files tab · account: iPhone XRR
  - Steps: Open Drive, click My Drive and click the Files tab → Click the Name column header and check the list reloads sorted by name, ascending → Click it again and check the list reloads sorted descending → Click it a third time and check the list reloads unsorted → Repeat the three clicks on the Date Modified column header → Repeat the three clicks on the Size column header
  - Expected: Each sortable column asks the server for the files in ascending order, then descending, and the third click turns sorting off.

- **list ⇄ grid toggle @smoke @drive** — passed (drive.spec.js:474)
  - Where: Tools → Drive → My Drive · account: iPhone XRR
  - Steps: Open Drive and click My Drive → Click the Grid view button → Check that it is highlighted, the table is gone, and cards (or the empty state) are shown → Check that the first card is visible → Click the List view button → Check that the table is back
  - Expected: The view toggle switches My Drive between the table and the card grid and back.

- **search goes to the server and shows "No results found" @smoke @drive** — passed (drive.spec.js:495)
  - Where: Tools → Drive → My Drive · account: iPhone XRR
  - Steps: Open Drive and click My Drive → Type a search that matches nothing (“zz-e2e-no-match-” plus a number) in the search box → Check that Drive searches My Drive’s folders and files on the server → Check that the path bar reads “Search results for” the query → Check that the list says “No results found” and repeats the query → Clear the search box → Check that the path bar is gone
  - Expected: A search is run on the server; with no match the list says “No results found” with the query, and clearing the search restores the normal view.

- **"Shared by Me" sends quick_filter=shared_by_me and resets on a section switch @smoke @drive** — passed (drive.spec.js:513)
  - Where: Tools → Drive → My Drive · account: iPhone XRR
  - Steps: Open Drive and click My Drive → Pick “Shared by Me” in the All / Shared by Me filter → Check that the list reloads with only the items you shared → If the list is empty, check that it says you haven’t shared any items yet, with no hints → Click Shared with me, then My Drive again → Check that the filter reads “All”
  - Expected: “Shared by Me” reloads My Drive with only the items you shared, and switching section resets the filter to “All”.

- **the Starred filter shows only starred rows, or says none are starred (ZL-21810) @smoke @drive** — passed (drive.spec.js:539)
  - Where: Tools → Drive → My Drive · account: iPhone XRR
  - Steps: Open Drive and click My Drive → Click the star button (Show favorites) → If rows are listed, check that every row is starred → If nothing is listed, check that the list says “No favorite items found” with no hints → Click the star button again to show everything
  - Expected: The Starred filter lists only starred items, or says “No favorite items found” when none are starred.

- **the tag filter asks the server which items carry the tag @smoke @drive** — passed (drive.spec.js:558)
  - Where: Tools → Drive → My Drive · account: iPhone XRR
  - Steps: Open Drive and click My Drive → Open the Tag filter → Pick the first tag in the list → Check that Drive asks the server which files and which folders carry that tag → Wait for the list to finish loading
  - Expected: Picking a tag makes Drive look up, on the server, the files and folders that carry it.

- **Activity Log drawer: chips, refresh, close @smoke @drive** — passed (drive.spec.js:585)
  - Where: Tools → Drive · account: iPhone XRR
  - Steps: Open Drive → Click the Activity Log button (clock icon) → Check that the “Activity Log” drawer opens with three filter chips (All, Files, Folders) → Click the Files chip and check that it is highlighted → Click the refresh button at the top of the drawer → Check that the activity list reloads → Close the drawer
  - Expected: The Activity Log drawer opens with its three chips, its refresh button reloads the list, and it closes.

- **Trash view opens; "Empty trash" is only ever cancelled @smoke @drive** — passed (drive.spec.js:605)
  - Where: Tools → Drive → My Drive → View Trash · account: iPhone XRR
  - Steps: Open Drive and click My Drive → Click “View Trash” → Check that the trash view shows “Trash (n)” and the header button now reads “Close Trash” → If an “Empty trash” button is shown, click it → Check that the dialog asks “Empty trash?” and warns the items will be permanently deleted → Click Cancel and check that the dialog closes and nothing is deleted → Click “Back to Drive” → Check that the header button reads “View Trash” again
  - Expected: The trash view opens and closes, and cancelling “Empty trash” deletes nothing.

- **the theme toggle flips Drive and its drawers, and is restored @smoke @drive** — passed (drive.spec.js:628)
  - Where: Tools → Drive · account: iPhone XRR
  - Steps: Open Drive and note whether it is light or dark → Click the theme button in the Drive banner → Check that Drive switches to the other theme and the button now reads “Switch to light mode” (or “Switch to dark mode”) → Check that the app saved the new theme as its own → Click the Activity Log button → Check that the drawer follows the new theme (dark styling only in dark) → Close the drawer → Click the theme button again to go back
  - Expected: The theme button flips Drive and its drawers between light and dark, and the original theme is restored at the end.

- **Create Folder refuses an empty, blank or long name and an unpicked destination (ZL-21806) @smoke @drive** — passed (drive.spec.js:658)
  - Where: Tools → Drive → My Drive → Create Folder · account: iPhone XRR
  - Steps: Open Drive, click My Drive and click Create Folder → Press “Create Folder” with the name empty and check that an error shows → Type only spaces as the name, press “Create Folder”, and check that an error shows → Type a 101-character name, press “Create Folder”, and check for the “under 100 characters” error → If “Inside a folder” is available, pick it, type “e2e never created” and press “Create Folder” without choosing a folder → Check the warning asking for a destination folder → Check that no folder was created, then close the drawer
  - Expected: Create Folder refuses an empty, blank or too-long name and an unpicked destination folder, and nothing is created.

- **the New menu offers five types; cancelling the name prompt creates nothing @smoke @drive** — passed (drive.spec.js:695)
  - Where: Tools → Drive → My Drive → New · account: iPhone XRR
  - Steps: Open Drive, click My Drive and click New → Check that the menu offers five file types → Click Document → Check that the name box is filled with “Untitled document” and shows “.docx” → Click Cancel → Check that the name box closes and no file is created
  - Expected: New offers five file types, Document suggests “Untitled document.docx”, and Cancel creates nothing.

- **the upload drawer lists unsupported files, removes and clears, and uploads nothing on Cancel @smoke @drive** — passed (drive.spec.js:719)
  - Where: Tools → Drive → My Drive → Upload · account: iPhone XRR
  - Steps: Open Drive, click My Drive and click Upload → Check that the “Upload Files” drawer shows the destination choice → Click “Choose files” and pick a file of an unsupported type (.xyz) and a .png image → Check “1 unsupported file skipped” and “1 file selected” → Remove the .png and check that no file is selected; click Clear on the unsupported notice and check it goes → Choose two .png images and check “2 files selected” → Click “Clear all” and check that nothing is selected → Click “Add details (optional)” and check that the Name field appears → Click Cancel and check that the drawer closes
  - Expected: The upload drawer lists and skips unsupported files, removes and clears the chosen files, and Cancel uploads nothing.

- **Request files needs a title and, at the root, a picked folder @smoke @drive** — passed (drive.spec.js:772)
  - Where: Tools → Drive → My Drive → Request Files · account: iPhone XRR
  - Steps: Open Drive, click My Drive and click Request Files → Check that the drawer asks you to pick a folder above to see its requests → Click “Create request” with no title and check the “Title is required” error → If “An existing folder” is available, pick it (do not choose a folder) → Type “[e2e] never created” as the title and click “Create request” → Check the error asking you to pick the folder the uploads go to → Check that no request and no folder were created, then close the drawer
  - Expected: Request Files refuses to create a request without a title, and at the root without a picked folder; nothing is created.

### Drive — stubbed states

- **an empty My Drive shows the empty state, and its hints open the drawers @smoke @drive** — passed (drive.spec.js:878)
  - Where: Tools → Drive → My Drive · account: iPhone XRR
  - Setup: The test makes the server answer with an empty My Drive and an empty Shared with me.
  - Steps: Open Drive and click My Drive → Check that the list says “No files or folders yet” and “Drag & drop files here, or use the Upload button to get started” → Click the “Upload files” hint in the empty list → Check that the “Upload Files” drawer opens, then close it → Click the “Create folder” hint in the empty list → Check that the “Create Folder” drawer opens, then close it
  - Expected: An empty My Drive shows the empty-state message, and its two hints open the Upload Files and Create Folder drawers.

- **with nothing shared the landing drops to My Drive; Shared with me says so @smoke @drive** — passed (drive.spec.js:902)
  - Where: Tools → Drive · account: iPhone XRR
  - Setup: The test makes the server answer with nothing in Shared with me and one folder, “e2e-stub-folder”, in My Drive.
  - Steps: Open Drive → Check that Drive lands on My Drive → Click Shared with me → Check that the list says “No folder/files have been shared with you yet”
  - Expected: With nothing shared, Drive drops to My Drive on opening, and Shared with me says nothing has been shared yet.

- **a failed list load is reported, not shown as an empty Drive @smoke @drive** — bug · known bug fetchDriveDataInner turns a failed GET /files or /folders into empty lists with no toast (apiInstance never rejects, so its catch never runs); the landing then also drops to My Drive,a failed GET /files or /folders (HTTP 500) shows an empty Drive and no error — confirmed run 2,fetchDriveDataInner turns a failed GET /files or /folders into empty lists with no toast (apiInstance never rejects, so its catch never runs); the landing then also drops to My Drive,a failed GET /files or /folders (HTTP 500) shows an empty Drive and no error — confirmed run 2 (drive.spec.js:916)
  - Where: Tools → Drive · account: iPhone XRR
  - Setup: The test makes the server fail every file and folder list load with a server error.
  - Steps: Open Drive → Wait for the lists to load → Check that an error message appears
  - Expected: A failed list load is reported with an error message instead of showing an empty Drive. Known bug: Drive shows an empty list with no error, and the landing drops to My Drive.
  - Web error: Error: expect(locator).toBeVisible() failed

- **a failed trash load is reported, not shown as "Trash is empty" @smoke @drive** — bug · known bug TrashView only toasts "Failed to load trash" from a catch that apiInstance never reaches; a refused GET /trash shows "Trash is empty",a refused GET /trash (HTTP 500) shows "Trash is empty" and no error — confirmed run 2,TrashView only toasts "Failed to load trash" from a catch that apiInstance never reaches; a refused GET /trash shows "Trash is empty",a refused GET /trash (HTTP 500) shows "Trash is empty" and no error — confirmed run 2 (drive.spec.js:930)
  - Where: Tools → Drive → My Drive → View Trash · account: iPhone XRR
  - Setup: The test makes the server answer with an empty Drive, and makes the trash load fail with a server error.
  - Steps: Open Drive and click My Drive → Click “View Trash” → Check that the trash view opens → Check that an error message appears
  - Expected: A failed trash load is reported with an error message. Known bug: the trash view says “Trash is empty” and shows no error.
  - Web error: Error: expect(locator).toBeVisible() failed

- **a 403 on the whole-drive list shows the no-access pane, and Back to tools leaves @smoke @drive** — passed (drive.spec.js:946)
  - Where: Tools → Drive · account: iPhone XRR
  - Setup: The test makes the server refuse every file and folder list load as access denied.
  - Steps: Open Drive → Check that the page says “You no longer have access to Z-Drive” → Click “Back to tools” → Check that the no-access message is gone
  - Expected: When the server denies access to the whole Drive, a “You no longer have access to Z-Drive” pane shows, and “Back to tools” leaves it.

- **a 403 on an opened folder says so and returns to the section root @smoke @drive** — passed (drive.spec.js:964)
  - Where: Tools → Drive → My Drive · account: iPhone XRR
  - Setup: The test makes the server list one folder, “e2e-stub-folder”, in My Drive and refuse to open it as access denied.
  - Steps: Open Drive and click My Drive → Click “e2e-stub-folder” → Check for a warning that you no longer have access to this folder → Check that Drive stays at the My Drive root (no folder path is shown)
  - Expected: Opening a folder the server refuses shows an access warning and keeps Drive at the section root.

- **a failed preview toasts and the modal says "Preview not available" @smoke @drive** — passed (drive.spec.js:996)
  - Where: Tools → Drive → My Drive, Files tab · account: iPhone XRR
  - Setup: The test makes the server list one image, “e2e-stub-image.png”, in My Drive and refuse its preview.
  - Steps: Open Drive, click My Drive and click the Files tab → Hover the image’s row and click its eye (Open) button → Check that the preview window opens with the file’s name as its title → Check for the error “Failed to load preview” → Check that the preview window says “Preview not available” → Close the preview window
  - Expected: A refused preview shows the “Failed to load preview” error, and the preview window says “Preview not available”.

- **a file the editors cannot open shows a worded message, never the raw key @smoke @drive** — passed (drive.spec.js:1013)
  - Where: Tools → Drive → My Drive, Files tab · account: iPhone XRR
  - Setup: The test makes the server list one Word file, “e2e-stub-doc.docx”, in My Drive and refuse to open it in the editor as a type that cannot be edited.
  - Steps: Open Drive, click My Drive and click the Files tab → Click “e2e-stub-doc.docx” → Click “Open in Editor” (or “View in Editor”) in the menu → Check that an error message appears in plain words, not the raw code “file_type_not_editable” → Check that the editor did not open
  - Expected: When the editor refuses a file, Drive shows a worded error message and does not open the editor.

- **an unusable editor config shows the failed pane without Try again; swipe-back is locked while open @smoke @drive** — passed (drive.spec.js:1024)
  - Where: Tools → Drive → My Drive, Files tab · account: iPhone XRR
  - Setup: The test makes the server list one Word file, “e2e-stub-doc.docx”, and answer its editor request with an editor address that is not secure and cannot be used.
  - Steps: Open Drive, click My Drive and click the Files tab → Note whether the page allows sideways swipe-back → Click “e2e-stub-doc.docx” and click “Open in Editor” (or “View in Editor”) → Check that the editor shows a failure pane saying the document couldn’t be opened → Check that the pane has no “Try again” button → Check that sideways swipe-back is switched off while the editor is open → Click “Back to Drive” in the pane → Check that the editor closes and swipe-back is as it was
  - Expected: An unusable editor setup shows the failure pane without “Try again”, swipe-back is locked while the editor is open, and it is restored after closing.

- **a host page that never says ready fails after 30 s and offers Try again @smoke @drive** — passed (drive.spec.js:1044)
  - Where: Tools → Drive → My Drive, Files tab · account: iPhone XRR
  - Setup: The test makes the server list one Word file, “e2e-stub-doc.docx”, and serves an editor page that loads but never reports that it is ready.
  - Steps: Open Drive, click My Drive and click the Files tab → Click “e2e-stub-doc.docx” and click “Open in Editor” (or “View in Editor”) → Check that the editor frame appears → Wait about 30 seconds → Check that the failure pane appears → Click “Try again” and check that the failure pane goes away → Click “Back to Drive” and check that the editor closes
  - Expected: An editor that never becomes ready fails after about 30 seconds with a failure pane, and “Try again” reloads it.

- **going offline keeps the editor open with a reconnecting banner (REL-BUG-09) @smoke @drive** — passed (drive.spec.js:1062)
  - Where: Tools → Drive → My Drive, Files tab · account: iPhone XRR
  - Setup: The test makes the server list one Word file, “e2e-stub-doc.docx”, and serves a stand-in editor page for it.
  - Steps: Open Drive, click My Drive and click the Files tab → Click “e2e-stub-doc.docx” and click “Open in Editor” (or “View in Editor”) → Check that the editor is open → Take the browser offline → Check that a banner says you’re offline and reconnecting, and the editor stays open → Bring the browser back online → Check that the offline banner goes away within about 20 seconds → Click “Back to Drive” and check that the editor closes
  - Expected: Going offline keeps the editor open with an offline, reconnecting banner, and the banner clears when the connection is back.

- **Markdown: sanitised render, conflict bar on a 409, discard prompt on close @smoke @drive** — passed (drive.spec.js:1085)
  - Where: Tools → Drive → My Drive, Files tab · account: iPhone XRR
  - Setup: The test makes the server list one Markdown file, “e2e-stub-notes.md”, whose text is a sample with a heading, a list, an outside link, a script and a button, and makes every save fail as “changed since you opened it”.
  - Steps: Open Drive, click My Drive, click the Files tab, hover “e2e-stub-notes.md” and click its eye (Open) button → Check that the preview shows “Drive e2e sample”, the button is stripped out and the script did not run → Check that the outside link opens in a new tab, safely → Click Edit, replace the text with “# changed by e2e”, and check for “Unsaved changes” → Click Save → Check for a conflict bar with “Load the latest version” and “Save my version anyway” → Click “Load the latest version” and check that the original text is back → Replace the text with “# changed again” and click “Back to Drive” → Check for the bar “You have unsaved changes. Close without saving?” and click “Discard changes” → Check that the document closes and no notify prompt appears
  - Expected: Markdown renders safely, a save conflict offers to load the latest version or save anyway, and closing with unsaved changes asks before discarding.

- **a refused upload start ends Failed in the Operations panel @smoke @drive** — passed (drive.spec.js:1144)
  - Where: Tools → Drive → My Drive → Upload · account: iPhone XRR
  - Setup: The test makes the server answer with an empty Drive and refuse every upload request.
  - Steps: Open Drive and click My Drive → Click Upload → Click “Choose files” and pick an image named “e2e-stub-refused.png” → Press Upload → Check that an error message appears or the file shows “Failed” in the Operations panel
  - Expected: A refused upload start ends as an error, with the file marked Failed in the Operations panel.

- **a chunk answer without an ETag fails the upload ("Missing ETag") @smoke @drive** — passed (drive.spec.js:1167)
  - Where: Tools → Drive → My Drive → Upload · account: iPhone XRR
  - Setup: The test makes the server accept the upload start for one part, and makes the storage accept that part without returning its receipt tag (ETag).
  - Steps: Open Drive and click My Drive → Click Upload → Click “Choose files” and pick an image named “e2e-stub-noetag.png” → Press Upload → Check that the file shows “Failed” in the Operations panel
  - Expected: An uploaded part with no receipt tag fails the upload (“Missing ETag”), shown as Failed in the Operations panel.

- **a refused move shows the server reason, not "Move failed" @smoke @drive** — passed (drive.spec.js:1209)
  - Where: Tools → Drive → My Drive, Files tab · account: iPhone XRR
  - Setup: The test makes the server list a folder “e2e-stub-folder” and a file “e2e-stub-move.pdf” in My Drive, and refuse to move the file with the reason that only the owner can move it out of its folder.
  - Steps: Open Drive, click My Drive and click the Files tab → Open the three-dot menu of “e2e-stub-move.pdf” and click “Move to...” → Check that the “Move to...” dialog names the file → Pick “e2e-stub-folder” in the folder tree → Click “Move here” → Check that an error message shows the server’s reason, not just “Move failed” → Check that the file shows “Failed” in the Operations panel
  - Expected: A refused move shows the server’s reason instead of a bare “Move failed”, and the Operations panel marks it Failed.

- **a refused share keeps the drawer open with the server message @smoke @drive** — passed (drive.spec.js:1234)
  - Where: Tools → Drive → My Drive, Files tab · account: iPhone XRR
  - Setup: The test makes the server list one file, “e2e-stub-move.pdf”, in My Drive with nobody on its access list, and refuse any change to that list.
  - Steps: Open Drive, click My Drive and click the Files tab → Open the three-dot menu of “e2e-stub-move.pdf” and click Share → In the “Share File” drawer, search for Lava Device (or the configured sandbox user) → Tick View for that person → Click “Save Access” → Check that an error message appears → Check that the drawer stays open
  - Expected: When the server refuses a share, the error is shown and the Share drawer stays open.

- **public share link error screens map each server code @smoke @drive** — passed (drive.spec.js:1258)
  - Where: A public Drive share link, opened in a signed-out browser · account: Signed-out visitor (no account)
  - Setup: The test makes the server answer each share link with a different error.
  - Steps: Open a share link the server says does not exist and check the title “Link not found” → Open a revoked link and check “Link revoked” → Open an expired link and check “Link expired” → Open a link past its view limit and check “View limit reached” → Open a link that needs an invited recipient and check “Invite only” → Open a link whose file is gone and check “File unavailable” → Open a link with an unknown error and check “Cannot open link”
  - Expected: Each share-link error from the server shows its own titled error screen.

- **public upload link error screens @smoke @drive** — passed (drive.spec.js:1287)
  - Where: A public Drive file-request (upload) link, opened in a signed-out browser · account: Signed-out visitor (no account)
  - Setup: The test makes the server answer each upload link with a different error.
  - Steps: Open an upload link the server says does not exist and reject the cookie banner if shown → Check the title “Can't open this link” and the text that it does not exist or has been removed → Open a revoked upload link → Check “Can't open this link” and the text that it was revoked by the sender → Open an expired upload link → Check “Can't open this link” and the text that it has expired
  - Expected: Each upload-link error shows “Can't open this link” with its own explanation.

- **the public uploader checks email and name before starting a session @smoke @drive** — passed (drive.spec.js:1313)
  - Where: A public Drive file-request (upload) link, opened in a signed-out browser · account: Signed-out visitor (no account)
  - Setup: The test makes the server answer with a request titled “[e2e] stub request” that requires the uploader’s email and name.
  - Steps: Open the upload link and reject the cookie banner if shown → Check that the page shows the title “[e2e] stub request” → Click Continue with no email and check “Please enter your email to continue.” → Type “not-an-email” as the email, click Continue, and check “Please enter a valid email address.” → Type “e2e-uploader@example.com”, click Continue, and check “Please enter your name to continue.” → Type “E2E Uploader” as the name and click Continue → Check that the upload page shows you as “e2e-uploader@example.com”
  - Expected: The public uploader checks that an email is given and valid and that a name is given before it starts the upload session.

### Drive — folders and new files

- **a new folder lands in the open folder, private ("Only you"), dated in the past @drive** — passed (drive.spec.js:1379)
  - Where: Tools → Drive → My Drive → “[e2e] drive …” → “e2e-drive-…-area-folders” · account: iPhone XRR
  - Setup: Opens the run’s private folder “[e2e] drive …” in My Drive and its area folder “e2e-drive-…-area-folders”, creating them on first use (older leftover run folders are removed first).
  - Steps: Open the area folder → Click Create Folder → Type “e2e-drive-…-n01” as the name and press “Create Folder” → Check that the folder is created inside the open folder, with nobody else given access → Check that its Sharing column reads “Only you” → Check that its date is in the past, not “in a few seconds”
  - Expected: A new folder is made inside the open folder, private to its creator (“Only you”), with a date that is not in the future.

- **nested folders: no Location inside a folder, crumbs, crumb click and back arrow @drive** — passed (drive.spec.js:1397)
  - Where: Tools → Drive → My Drive → “[e2e] drive …” → “e2e-drive-…-area-folders” · account: iPhone XRR
  - Setup: Opens the run’s area folder “e2e-drive-…-area-folders” (created on first use).
  - Steps: Create a folder “e2e-drive-…-n03-parent” and open it → Click Create Folder and check that the drawer has no destination (Location) choice inside a folder, then close it → Create a folder “e2e-drive-…-n03-child” and check it is made inside the parent → Open the child folder → Check that the path shows five crumbs → Click the parent’s crumb and check that the child folder is listed → Click the back arrow → Check that Drive is back in the area folder with the parent folder listed
  - Expected: Inside a folder, Create Folder has no destination choice and nests the new folder there; the crumbs and the back arrow climb back up.

- **at the root, "Inside a folder" needs a pick, then creates there @drive** — passed (drive.spec.js:1434)
  - Where: Tools → Drive → My Drive (root), in a second browser tab · account: iPhone XRR
  - Setup: Opens the run’s area folder “e2e-drive-…-area-folders” (created on first use).
  - Steps: In a second tab, open Drive, click My Drive and click Create Folder → Type “e2e-drive-…-n04” as the name and choose “Inside a folder” → Press “Create Folder” without picking a folder → Check for a warning asking for a destination folder, and that nothing is created → Search the folder picker for the area folder’s name and pick it → Check that the area folder shows as selected → Press “Create Folder” and check that the folder is created inside the area folder → Back in the first tab, open the area folder and check that “e2e-drive-…-n04” is listed
  - Expected: At the root, “Inside a folder” refuses to create without a picked folder, and once one is picked the new folder is made there.

- **a second folder with the same name @drive** — passed (drive.spec.js:1477)
  - Where: Tools → Drive → My Drive → “[e2e] drive …” → “e2e-drive-…-area-folders” · account: iPhone XRR
  - Setup: Opens the run’s area folder “e2e-drive-…-area-folders” (created on first use) and creates a folder “e2e-drive-…-n05” in it.
  - Steps: Click Create Folder → Type the same name, “e2e-drive-…-n05” → Press “Create Folder” → If the server refuses, check that an error shows and the drawer stays open, then close it → If the server accepts, check that the drawer closes
  - Expected: A duplicate folder name is either refused with an error while the drawer stays open, or accepted (the test then only records that duplicates are allowed).

- **Create Folder with one specific user sends one viewer entry @drive** — skipped (drive.spec.js:1498)
  - Where: Tools → Drive → My Drive → “[e2e] drive …” → “e2e-drive-…-area-folders” · account: iPhone XRR
  - Setup: Opens the run’s area folder “e2e-drive-…-area-folders” (created on first use).
  - Steps: Click Create Folder → Type “e2e-drive-…-n06-shared” as the name → Type Lava Device (or the configured sandbox user) in “Search team members...” and click the person → Check that the drawer says “1 user selected” → Press “Create Folder” → Check that the folder is created with exactly one person given Viewer access → Check that the folder’s Sharing column no longer reads “Only you”
  - Expected: Creating a folder with one chosen person shares it with that person as a Viewer, and the Sharing column shows it is shared.

- **renaming a folder onto a sibling’s name shows the server message and keeps the drawer (ZL-21807) @drive** — passed (drive.spec.js:1528)
  - Where: Tools → Drive → My Drive → “[e2e] drive …” → “e2e-drive-…-area-folders” · account: iPhone XRR
  - Setup: Opens the run’s area folder (created on first use) and creates two folders, “e2e-drive-…-a02-a” and “e2e-drive-…-a02-b”.
  - Steps: Open the three-dot menu of “…-a02-b” and click “Edit Info” → Check that the Name field holds “…-a02-b” → Change the name to “…-a02-a” and press “Save Changes” → Check that the server refuses it and the error shown is the server’s message, not “Failed to update” → Check that the drawer stays open → Change the name to “…-a02-b-renamed” and press “Save Changes” → Check that the rename is saved and “…-a02-b-renamed” is listed
  - Expected: Renaming a folder onto a sibling’s name shows the server’s message and keeps the drawer open; a valid name is then saved.

- **a blank rename is refused in the form, before any request @drive** — bug · known bug EditItemDrawer’s name rule is `required` without `whitespace: true`, so "   " is sent (Create Folder got the fix in ZL-21806),a blank ("   ") folder rename is sent as PUT /folders/:id instead of being refused in the form — confirmed run 2,EditItemDrawer’s name rule is `required` without `whitespace: true`, so "   " is sent (Create Folder got the fix in ZL-21806),a blank ("   ") folder rename is sent as PUT /folders/:id instead of being refused in the form — confirmed run 2 (drive.spec.js:1559)
  - Where: Tools → Drive → My Drive → “[e2e] drive …” → “e2e-drive-…-area-folders” · account: iPhone XRR
  - Setup: Opens the run’s area folder (created on first use) and creates a folder “e2e-drive-…-a03”.
  - Steps: Open the folder’s three-dot menu and click “Edit Info” → Replace the name with only spaces → Press “Save Changes” → Wait a moment → Check that no rename was sent to the server → Check that the form shows an error under the Name field
  - Expected: A blank name is refused in the form before anything is sent. Known bug: the blank name is sent to the server instead of being refused in the form.
  - Web error: Error: expect(received).toHaveLength(expected)

- **New → Markdown opens the editor; Save makes a version; closing asks to notify (Skip) @drive** — passed (drive.spec.js:1591)
  - Where: Tools → Drive → My Drive → “[e2e] drive …” → “e2e-drive-…-area-folders” · account: iPhone XRR
  - Setup: Opens the run’s area folder “e2e-drive-…-area-folders” (created on first use).
  - Steps: Click New, choose “Markdown file”, type “e2e-drive-…-n08” and press Enter → Check that the Markdown editor opens → Type a heading “e2e heading” and a line of text, and check for “Unsaved changes” → Click Save and check for “All changes saved” → Click Preview and check that the heading reads “e2e heading” → Click “Back to Drive” → Check for the prompt “Let others know you updated this file?” and click Skip → Check that nobody was notified and the new file is listed
  - Expected: New → Markdown file opens the editor, Save stores the text, and closing asks whether to notify others; Skip notifies nobody.

- **New → Text file is shown as written (no Markdown) @drive** — passed (drive.spec.js:1624)
  - Where: Tools → Drive → My Drive → “[e2e] drive …” → “e2e-drive-…-area-folders” · account: iPhone XRR
  - Setup: Opens the run’s area folder “e2e-drive-…-area-folders” (created on first use).
  - Steps: Click New, choose “Text file”, type “e2e-drive-…-n08b” and press Enter → Check that the file is created as a .txt file → Check that the editor opens with the placeholder “Write here…” → Type “# not a heading in a text file” and click Save → Click Preview → Check that the text is shown exactly as typed, not as a heading → Click “Back to Drive” (and Skip the notify prompt if it shows) → Check that the document closes
  - Expected: A new text file is saved and shown as plain text, with no Markdown formatting.

### Drive — uploads

- **three files upload into the open folder; the Operations panel tracks and clears them @drive** — passed (drive.spec.js:1655)
  - Where: Tools → Drive → My Drive → “[e2e] drive …” → “e2e-drive-…-area-uploads” · account: iPhone XRR
  - Setup: Opens the run’s area folder “e2e-drive-…-area-uploads” (created on first use).
  - Steps: Click Upload → Click “Choose files” and pick an image “…-u01-image.png”, an audio file “…-u01-audio.wav” and a PDF “…-u01-doc.pdf” → Check that the drawer says 3 files are selected, and press Upload → Check that all three uploads go into the open folder → Check for the “uploaded successfully” message → Check that the Operations panel marks each of the three files “Completed” → Click the clear (bin) button in the Operations panel header → Check that the three files are gone from the panel
  - Expected: Three files upload into the open folder, the Operations panel shows each as Completed, and its clear button empties the panel.

- **a second upload from the same folder also lands in it (ZL-21873) @drive** — passed (drive.spec.js:1684)
  - Where: Tools → Drive → My Drive → “[e2e] drive …” → “e2e-drive-…-area-uploads” · account: iPhone XRR
  - Setup: Opens the run’s area folder “e2e-drive-…-area-uploads” (created on first use).
  - Steps: Click Upload, choose an image “…-u02-first.png” and press Upload → Wait for it to finish and appear in the list → Click Upload again, choose an image “…-u02-second.png” and press Upload → Wait for it to finish and appear in the list → Check that both uploads went into the open folder
  - Expected: A second upload from the same folder also lands in that folder, not elsewhere.

- **at the root, "Existing folder" needs a pick, then uploads there @drive** — passed (drive.spec.js:1697)
  - Where: Tools → Drive → My Drive (root), in a second browser tab · account: iPhone XRR
  - Setup: Opens the run’s area folder “e2e-drive-…-area-uploads” (created on first use).
  - Steps: In a second tab, open Drive, click My Drive and click Upload → Choose “Existing folder” → Click “Choose files” and pick an image “…-u03.png” → Press Upload without picking a folder → Check for a warning asking for a destination folder, and that nothing is uploaded → Search the folder picker for the area folder’s name and pick it → Press Upload and check that the file uploads into the area folder → Back in the first tab, open the area folder and check that “…-u03.png” is listed
  - Expected: At the root, “Existing folder” refuses to upload without a picked folder, and once one is picked the file uploads there.

- **a folder upload creates the tree first, then uploads into it @drive** — passed (drive.spec.js:1739)
  - Where: Tools → Drive → My Drive → “[e2e] drive …” → “e2e-drive-…-area-uploads” · account: iPhone XRR
  - Setup: Opens the run’s area folder “e2e-drive-…-area-uploads” (created on first use).
  - Steps: Click Upload → Choose a folder “drive-tree” (it holds folder “a” with one.txt, and “a/b” with two.txt) → Check that the drawer says 2 files are selected → Press Upload → Check that the folders “drive-tree”, “a” and “b” are created, “drive-tree” inside the open folder → Check that both files finish uploading → Check that “drive-tree” is listed
  - Expected: A folder upload rebuilds the folder tree inside the open folder and uploads its files into it.

- **the drawer’s Name keeps the file’s extension @drive** — bug · known bug resolveUploadFileName appends the extension only when the typed name has none, so "x.v2" for a.pdf uploads as "x.v2" (rename keeps it, ZL-18310),a typed Name with a dot ("x.v2") for a.pdf uploads as "x.v2" — the extension is lost; confirmed run 2,resolveUploadFileName appends the extension only when the typed name has none, so "x.v2" for a.pdf uploads as "x.v2" (rename keeps it, ZL-18310),a typed Name with a dot ("x.v2") for a.pdf uploads as "x.v2" — the extension is lost; confirmed run 2 (drive.spec.js:1763)
  - Where: Tools → Drive → My Drive → “[e2e] drive …” → “e2e-drive-…-area-uploads” · account: iPhone XRR
  - Setup: Opens the run’s area folder “e2e-drive-…-area-uploads” (created on first use).
  - Steps: Click Upload → Click “Choose files” and pick a PDF “…-u07-original.pdf” → Click “Add details (optional)” → Type “e2e-drive-…-u07.v2” in Name → Press Upload → Check that the uploaded file’s name ends in “.pdf”
  - Expected: A name typed in the drawer keeps the file’s real extension (“…u07.v2.pdf”). Known bug: the file is saved as “…u07.v2” and loses “.pdf”.
  - Web error: Error: expect(received).toMatch(expected)

### Drive — preview by file type

- **each item type gets its own action menu (owner) @drive** — passed (drive.spec.js:1797)
  - Where: Tools → Drive → My Drive → “[e2e] drive …” → “e2e-drive-…-area-seed” · account: iPhone XRR
  - Setup: Opens the run’s area folder “e2e-drive-…-area-seed” and, once per run, uploads ten sample files into it (“e2e-drive-…-seed-…” as .png, .wav, .pdf, .csv, .docx, .xlsx, .md, .txt, .rtf and .zip).
  - Steps: Open the three-dot menu of the seed PDF → Check it offers Preview, Download, Share, Copy link, “Move to...”, “Edit Info”, the favourites item, Info and Delete, but no editor item and no “Edit file”; press Escape → Open the menu of the seed Word file (.docx) → Check it offers Preview, Download, “Open in Editor”, “Version history” and the same Share-to-Delete items; press Escape → Open the menu of the seed Markdown file (.md) → Check it offers Preview, “Edit file” and the same Share-to-Delete items, but no editor item; press Escape → Go up to the run folder and open the seed area folder’s menu → Check it offers “Open folder”, “Download as ZIP”, Share, “Request files”, “Move to...”, “Edit Info”, Info and Delete, but no Preview and no Copy link
  - Expected: The owner sees a menu that fits each item type: PDF, Office file, Markdown file and folder each get their own set of actions.

- **search is section-scoped; opening a folder from results rebuilds the crumbs (ZL-20182) @drive** — failed (drive.spec.js:1826)
  - Where: Tools → Drive → My Drive, search box (in a second browser tab) · account: iPhone XRR
  - Setup: Opens the run’s seed area folder “e2e-drive-…-area-seed” and uploads the ten sample files once per run.
  - Steps: In a second tab, open Drive and click My Drive → Search for the seed PDF’s name and check that it is found → Click Shared with me and check that the PDF is not listed there → Click My Drive again → Search for the seed area folder’s name → Click the folder in the results → Check that the search box is emptied → Check that the path shows three crumbs: Drive, “[e2e] drive …” and the area folder
  - Expected: Search only covers the current section, and opening a folder from the results clears the search and rebuilds the full path to it.
  - Web error: Error: expect(locator).toHaveCount(expected) failed

- **an image opens in the preview modal with size and Download @drive** — notrun (drive.spec.js:1855)
  - Where: Tools → Drive → My Drive → “[e2e] drive …” → “e2e-drive-…-area-seed” · account: iPhone XRR
  - Setup: Opens the run’s seed area folder and uploads the ten sample files once per run.
  - Steps: Hover the seed image (.png) row and click its eye (Open) button → Check that the preview window opens with the image → Check that its footer shows the file size → Check that it has a Download button → Close the preview window
  - Expected: An image opens in the preview window with its size and a Download button for the owner.

- **audio opens in the preview modal with a player @drive** — notrun (drive.spec.js:1869)
  - Where: Tools → Drive → My Drive → “[e2e] drive …” → “e2e-drive-…-area-seed” · account: iPhone XRR
  - Setup: Opens the run’s seed area folder and uploads the ten sample files once per run.
  - Steps: Hover the seed audio file (.wav) row and click its eye (Open) button → Check that the preview window opens → Check that it shows an audio player → Close the preview window
  - Expected: An audio file opens in the preview window with a player.

- **a PDF opens in the shared PdfViewer with Download for the owner @drive** — notrun (drive.spec.js:1880)
  - Where: Tools → Drive → My Drive → “[e2e] drive …” → “e2e-drive-…-area-seed” · account: iPhone XRR
  - Setup: Opens the run’s seed area folder and uploads the ten sample files once per run.
  - Steps: Hover the seed PDF row and click its eye (Open) button → Check that the PDF viewer opens showing the file’s name → Check that it has a Download button → Click Close → Check that the viewer closes
  - Expected: A PDF opens in the app’s PDF viewer, with Download for the owner, and Close shuts it.

- **an RTF opens in the DocumentViewer @drive** — notrun (drive.spec.js:1894)
  - Where: Tools → Drive → My Drive → “[e2e] drive …” → “e2e-drive-…-area-seed” · account: iPhone XRR
  - Setup: Opens the run’s seed area folder and uploads the ten sample files once per run.
  - Steps: Hover the seed RTF file row and click its eye (Open) button → Check that the document viewer window opens → Press Escape → Check that the viewer closes
  - Expected: An RTF file opens in the document viewer window, which closes on Escape.

- **a type the browser cannot show says so and offers "Open in new tab" to the owner @drive** — notrun (drive.spec.js:1904)
  - Where: Tools → Drive → My Drive → “[e2e] drive …” → “e2e-drive-…-area-seed” · account: iPhone XRR
  - Setup: Opens the run’s seed area folder and uploads the ten sample files once per run.
  - Steps: Hover the seed .zip file row and click its eye (Open) button → Check that the preview window says “Preview is not supported for this file type” → Check that it offers an “Open in new tab” button → Close the preview window
  - Expected: A file type the browser cannot show says so, and the owner is offered “Open in new tab”.

- **Markdown renders sanitised in Drive’s own viewer @drive** — notrun (drive.spec.js:1916)
  - Where: Tools → Drive → My Drive → “[e2e] drive …” → “e2e-drive-…-area-seed” · account: iPhone XRR
  - Setup: Opens the run’s seed area folder and uploads the ten sample files once per run; the Markdown sample holds a heading, a list, an outside link, a script and a button.
  - Steps: Hover the seed Markdown file row and click its eye (Open) button → Check that the heading reads “Drive e2e sample” → Check that the button from the file is stripped out and its script did not run → Check that the outside link opens in a new tab, safely → Click “Back to Drive” → Check that the document closes with no notify prompt
  - Expected: Markdown renders in Drive’s own viewer with unsafe content removed, and closing without an edit asks nothing.

- **a .txt file opens as plain text @drive** — notrun (drive.spec.js:1938)
  - Where: Tools → Drive → My Drive → “[e2e] drive …” → “e2e-drive-…-area-seed” · account: iPhone XRR
  - Setup: Opens the run’s seed area folder and uploads the ten sample files once per run.
  - Steps: Hover the seed text file (.txt) row and click its eye (Open) button → Check that it shows “Drive e2e plain text sample.” as plain text → Click “Back to Drive” → Check that the document closes
  - Expected: A .txt file opens in Drive’s text viewer as plain text.

### Drive — organise: rename, move, delete, trash

- **rename keeps the extension (ZL-18310); the description shows in its column @drive** — passed (drive.spec.js:1955)
  - Where: Tools → Drive → My Drive → “[e2e] drive …” → “e2e-drive-…-area-organise” · account: iPhone XRR
  - Setup: Opens the run’s area folder “e2e-drive-…-area-organise” (created on first use) and uploads a PDF “e2e-drive-…-a01.pdf”.
  - Steps: Open the PDF’s three-dot menu and click “Edit Info” → Check that the Name field shows the name without “.pdf” → Type “e2e-drive-…-a01-renamed” as the name → Type “e2e description” as the description → Press “Save Changes” → Check that the file is saved as “…-a01-renamed.pdf” → Check that the row shows the new name and “e2e description”
  - Expected: Renaming a file keeps its extension, and the description appears in its column.

- **Move to… hides the moved folder and everything below it, and moves a file (REL-BUG-17) @drive** — passed (drive.spec.js:1977)
  - Where: Tools → Drive → My Drive → “[e2e] drive …” → “e2e-drive-…-area-organise” · account: iPhone XRR
  - Setup: Opens the run’s area folder “e2e-drive-…-area-organise”, creates folders “…-a05-parent” and “…-a05-dest”, uploads an image “…-a05-file.png”, and creates “…-a05-child” inside the parent.
  - Steps: Back in the area folder, open the parent folder’s menu and click “Move to...” → Check that the folder tree lists “…-a05-dest” and “Drive (Root)” → Check that it does not list the parent folder itself or its child folder → Close the dialog → Open the image’s menu, click “Move to...” and check the dialog names the image → Pick “…-a05-dest” and click “Move here” → Check for the “moved” message and that the image leaves the list → Open “…-a05-dest” and check that the image is there
  - Expected: “Move to...” never offers the folder being moved or anything below it, and a file moves into the picked folder.

- **bulk bar: folders-only has no Download; bulk move; bulk delete @drive** — passed (drive.spec.js:2021)
  - Where: Tools → Drive → My Drive → “[e2e] drive …” → “e2e-drive-…-area-organise” · account: iPhone XRR
  - Setup: Opens the run’s area folder “e2e-drive-…-area-organise”, creates a folder “…-a09-folder” and uploads two images “…-a09-one.png” and “…-a09-two.png”.
  - Steps: Tick the folder’s checkbox and check the bar says “1 item selected” with Delete and “Move to...” but no Download → Click Clear in the bar → Tick both images and check “2 items selected” with Download shown → Click “Move to...” in the bar, pick “…-a09-folder” and click “Move here” → Check for “2 of 2 items moved” → Open “…-a09-folder” and tick both images → Click Delete in the bar, check the dialog asks “Delete 2 items?”, and click Delete → Check for the “of 2 items deleted” message and that the images leave the list → Click “View Trash” and delete both images forever → Click “Back to Drive”
  - Expected: The selection bar hides Download for a folders-only selection, moves two files in one go, and deletes two files to the trash.

- **delete to trash, restore it, then delete it forever @drive** — failed (drive.spec.js:2081)
  - Where: Tools → Drive → My Drive → “[e2e] drive …” → “e2e-drive-…-area-organise” · account: iPhone XRR
  - Setup: Opens the run’s area folder “e2e-drive-…-area-organise” and uploads an image “…-a12.png”.
  - Steps: Hover the image’s row and click its bin (delete) button → Check the dialog says the item will be moved to trash, and confirm → Check that the image leaves the list → Click “View Trash” and check that the image is listed → Click Restore on its row and check for the “restored” message → Click “Back to Drive” and check that the image is back in the folder → Delete the image again from its three-dot menu → Open the trash, click Delete on its row and confirm “Delete forever” → Check that it is gone from the trash and click “Back to Drive”
  - Expected: A deleted file goes to the trash, Restore puts it back in its folder, and “Delete forever” removes it for good.
  - Web error: Test timeout of 90000ms exceeded.

### Drive — favourites, tags, comments, Info, activity

- **star from the row and the menu; the Starred filter shows only starred rows @drive** — passed (drive.spec.js:2125)
  - Where: Tools → Drive → My Drive → “[e2e] drive …” → “e2e-drive-…-area-meta” · account: iPhone XRR
  - Setup: Opens the run’s area folder “e2e-drive-…-area-meta” (created on first use) and uploads two images, “…-i01-star.png” and “…-i01-plain.png”.
  - Steps: Hover the “…-i01-star.png” row and click its star → Check that it is saved as a favourite and the star is filled → Open its three-dot menu and check that it offers “Remove from favorites”; press Escape → Click the star filter button in the toolbar (Show favorites) → Check that only “…-i01-star.png” is listed → Click the star filter button again → Check that “…-i01-plain.png” is listed again
  - Expected: Starring a row marks it a favourite, the menu then offers to remove it, and the Starred filter lists only starred items.

- **Info shows details, access, tags, comments and activity; its Favorite button toggles @drive** — passed (drive.spec.js:2161)
  - Where: Tools → Drive → My Drive → “[e2e] drive …” → “e2e-drive-…-area-meta” · account: iPhone XRR
  - Setup: Opens the run’s area folder “e2e-drive-…-area-meta” and uploads an image “…-i03.png”.
  - Steps: Open the image’s three-dot menu and click Info → Check that the Info drawer is titled with the file’s name → Check that it shows Created by, Created on, Size and Type, with the type PNG → Check that it says “No tags assigned” and “No comments yet”, and that the file’s activity loads → Click Favorite and check for “added to favorites” and a “Favorited” button → Click Favorited and check for “removed from favorites” → Close the drawer
  - Expected: The Info drawer shows the file’s details, tags, comments and activity, and its Favorite button turns the favourite on and off.

- **tag a file with e2e-drive, filter the list by it, then remove the tag @drive** — passed (drive.spec.js:2200)
  - Where: Tools → Drive → My Drive → “[e2e] drive …” → “e2e-drive-…-area-meta” · account: iPhone XRR
  - Setup: Opens the run’s area folder “e2e-drive-…-area-meta” and uploads two images, “…-i05-tagged.png” and “…-i05-other.png”. If the project has no “e2e-drive” tag yet, the test creates it (it is kept and reused).
  - Steps: Open the menu of “…-i05-tagged.png” and click Info → Click “Add a tag...” and pick “e2e-drive” (if it is missing, click +, type “e2e-drive” as the Tag name and click Create) → Check that the “e2e-drive” chip shows in the drawer, then close it → In the toolbar’s Tag filter, pick “e2e-drive” → Check that only “…-i05-tagged.png” is listed → Clear the Tag filter → Check that “…-i05-other.png” is listed again
  - Expected: A tag added in Info is saved on the file, and the toolbar’s Tag filter lists only the files that carry it.

- **comments: add with Enter, edit, delete @drive** — passed (drive.spec.js:2271)
  - Where: Tools → Drive → My Drive → “[e2e] drive …” → “e2e-drive-…-area-meta” · account: iPhone XRR
  - Setup: Opens the run’s area folder “e2e-drive-…-area-meta” and uploads an image “…-i06.png”.
  - Steps: Open the image’s three-dot menu and click Info → Type “e2e comment” plus a number in “Add a comment...” and press Enter → Check that the comment is saved and listed → Click the comment’s edit (pencil) button, add “ (edited)” and save → Check that the edited comment is listed → Click the comment’s delete (bin) button and confirm → Check for “Comment deleted” and that the comment is gone → Close the drawer
  - Expected: A comment can be added with Enter, edited and deleted in the Info drawer.

- **the Activity Log lists this run’s upload @drive** — failed (drive.spec.js:2316)
  - Where: Tools → Drive → My Drive → “[e2e] drive …” → “e2e-drive-…-area-meta” · account: iPhone XRR
  - Setup: Opens the run’s area folder “e2e-drive-…-area-meta” and uploads an image “…-i11.png”.
  - Steps: Click the Activity Log button (clock icon) → Wait for the activity to load → Check that the drawer lists “…-i11.png” → Close the drawer
  - Expected: The Activity Log lists the file this run just uploaded.
  - Web error: Error: expect(locator).toContainText(expected) failed

### Drive — sharing

- **the file Share drawer: tabs, grid, level rules, and no empty save @drive** — bug · known bug Save Access with "0 selected" sends PUT …/access carrying only the owner’s own prefilled row and closes the drawer; the "select at least one user" warning never shows — confirmed run 2,Save Access with "0 selected" sends PUT …/access carrying only the owner’s own prefilled row and closes the drawer; the "select at least one user" warning never shows — confirmed run 2,Save Access with "0 selected" sends PUT …/access carrying only the owner’s own prefilled row and closes the drawer; the "select at least one user" warning never shows — confirmed run 2 (drive.spec.js:2333)
  - Where: Tools → Drive → My Drive → “[e2e] drive …” → “e2e-drive-…-area-share” · account: iPhone XRR
  - Setup: Opens the run’s area folder “e2e-drive-…-area-share” (created on first use) and uploads a PDF “…-s01.pdf”.
  - Steps: Open the PDF’s three-dot menu and click Share; wait for the access list to load → Check the drawer is titled “Share File” with the People tab selected and a “Share via link” tab → Check the grid header shows “0 selected” and the View, Download and Edit columns, with the hint “Edit includes Download and View. Download includes View.” → If anyone is listed, tick Edit on the first person and check that all three boxes tick and the header says “1 selected” → Click Edit again and check that Edit unticks while Download stays ticked → Click View and check that all three untick and the header says “0 selected” → Check that none of this was saved → Click “Save Access” with nobody selected → Check that nothing is saved and a warning asks you to select at least one user
  - Expected: The Share File drawer shows its tabs, grid and level rules (a higher level ticks the lower ones), and saving with nobody selected is refused with a warning. Known bug: Save sends a change holding only the owner’s own entry and closes the drawer; the warning never shows.
  - Web error: Error: an empty save sends nothing

- **a folder’s owner sees Share; the drawer is "Manage Access" with roles @drive** — passed (drive.spec.js:2398)
  - Where: Tools → Drive → My Drive → “[e2e] drive …” → “e2e-drive-…-area-share” · account: iPhone XRR
  - Setup: Opens the run’s area folder “e2e-drive-…-area-share” and creates a folder “…-s02” in it.
  - Steps: Open the folder’s three-dot menu and click Share; wait for the access list to load → Check that the drawer is titled “Manage Access” and has no tabs → Check that the grid header shows Viewer, Editor and Owner → Check for “Everything inside this folder, subfolders included, gets the same access.” → Check for “Owner includes Editor and Viewer. Editor includes Viewer.” → Check that “Save Access” is shown, then close the drawer
  - Expected: A folder’s owner can open Share, which is the “Manage Access” drawer with Viewer, Editor and Owner roles.

- **sharing a file with the sandbox user at Download level @drive** — failed (drive.spec.js:2425)
  - Where: Tools → Drive → My Drive → “[e2e] drive …” → “e2e-drive-…-area-share” · account: iPhone XRR
  - Setup: Opens the run’s area folder “e2e-drive-…-area-share” and uploads a PDF “…-s03.pdf”.
  - Steps: Open the PDF’s three-dot menu and click Share; wait for the access list to load → Type Lava Device (or the configured sandbox user) in “Search users...” → Tick Download for that person (View ticks with it) → Click “Save Access” → Check that the save names that one person with view and download but not edit; your own row may be sent along too, and it keeps full rights → Check that the drawer closes and the file’s Sharing column no longer reads “Only you”
  - Expected: Sharing a file at Download level gives that person view and download rights, not edit; your own owner row, if sent along, keeps full rights; the file shows as shared.
  - Web error: Error: expect(received).toHaveLength(expected)

- **an Editor role on a folder reaches its subfolder ("through the parent folder") @drive** — notrun (drive.spec.js:2446)
  - Where: Tools → Drive → My Drive → “[e2e] drive …” → “e2e-drive-…-area-share” · account: iPhone XRR
  - Setup: Opens the run’s area folder “e2e-drive-…-area-share”, creates a folder “…-s04-parent” and, inside it, “…-s04-child”.
  - Steps: Go back to the area folder → Open the parent folder’s menu, click Share and wait for the access list → Search for Lava Device (or the configured sandbox user) and tick Editor → Click “Save Access” and check that the person is saved as Editor, replacing the folder’s access list → Open the parent folder → Open the child folder’s menu and click Share → Search for the same person → Check that their row says “Has access through the parent folder” and its boxes are disabled → Close the drawer
  - Expected: An Editor role on a folder reaches its subfolder: the subfolder’s Share drawer shows the person as having access through the parent, locked.

### Drive — copy link and share via link

- **Copy link makes a tracked anyone-with-the-link URL, reused while fresh, listed and revocable @drive** — passed (drive.spec.js:2508)
  - Where: Tools → Drive → My Drive → “[e2e] drive …” → “e2e-drive-…-area-links” · account: iPhone XRR (plus a signed-out visitor at the end)
  - Setup: Opens the run’s area folder “e2e-drive-…-area-links” (created on first use) and uploads an image “…-l01.png”.
  - Steps: Open the image’s three-dot menu and click the “1h” chip beside Copy link → Check that a 1-hour link is made, “Link copied, works for 1 hour” shows, and the clipboard holds the link → Open the menu again and click the “1h” chip → Check that the same link is given back, not a new one → Open Share, click the “Share via link” tab → Check that a link row “Anyone with the link” with “Total views” is listed, then close the drawer → Revoke every link on the file from Share → Share via link → Open the copied link in a signed-out browser and check “Link revoked” (or “Link not found”)
  - Expected: Copy link makes an anyone-with-the-link address, reuses it while it is still valid, lists it under Share via link, and it stops working once revoked.

- **Copy link reports a clipboard that refuses the write @drive** — passed (drive.spec.js:2548)
  - Where: Tools → Drive → My Drive → “[e2e] drive …” → “e2e-drive-…-area-links” · account: iPhone XRR
  - Setup: The test makes the browser refuse to write to the clipboard. Opens the run’s area folder “e2e-drive-…-area-links” and uploads an image “…-l02.png”.
  - Steps: Open the image’s three-dot menu → Click the words “Copy link” (not a chip) → Check that a link valid for 24 hours (the default) is requested → Check for the error “Couldn't copy the link”
  - Expected: When the clipboard refuses the write, Copy link says it couldn’t copy the link; clicking the label itself picks the 24-hour default.

- **Generate link → an outsider sees the file; after revoke, "Link revoked" @drive** — passed (drive.spec.js:2580)
  - Where: Tools → Drive → My Drive → “[e2e] drive …” → “e2e-drive-…-area-links” · account: iPhone XRR (plus a signed-out visitor)
  - Setup: Opens the run’s area folder “e2e-drive-…-area-links” and uploads an image “…-l03.png”.
  - Steps: Open the image’s menu, click Share and open the “Share via link” tab → With no recipients, click “Generate link” → Check the link is view-only, unlimited views, valid for 7 days, the “Share link copied” (or “created”) message shows, and the row reads “View only”; close the drawer → In a signed-out browser, open the link → Check that the page shows the file’s name, the image and “This link is for your use only”, with no Download button → Check that a watermark shows only if the server sends one, and right-click on the image is blocked → Revoke every link on the file from Share → Share via link → Open the link again signed out and check “Link revoked”
  - Expected: A generated view-only link lets an outsider see the file without download or right-click, and after revoking it the link says “Link revoked”.

- **link form rules: a bad email; "Can edit" needs recipients and drops "Never" @drive** — passed (drive.spec.js:2640)
  - Where: Tools → Drive → My Drive → “[e2e] drive …” → “e2e-drive-…-area-links” · account: iPhone XRR
  - Setup: Opens the run’s area folder “e2e-drive-…-area-links” and uploads a spreadsheet “…-l04.xlsx”.
  - Steps: Open the spreadsheet’s menu, click Share and open the “Share via link” tab → Type “not-an-email” in the recipients box and click “Send link by email” → Check for the error “Invalid email: not-an-email” and empty the recipients box → Pick “Can edit” in Permission → Open Expires and check that “30 days” is offered but “Never expires” is not; press Escape → Click “Generate link” → Check for an error asking you to list everyone who should edit → Close the drawer and check that no link was created
  - Expected: The link form refuses a malformed email, and a “Can edit” link needs recipients and cannot be set to never expire; nothing is created.

- **the Info panel’s Copy link offers 1 hour / 24 hours / 7 days @drive** — passed (drive.spec.js:2680)
  - Where: Tools → Drive → My Drive → “[e2e] drive …” → “e2e-drive-…-area-links” · account: iPhone XRR
  - Setup: Opens the run’s area folder “e2e-drive-…-area-links” and uploads an image “…-i08.png”.
  - Steps: Open the image’s three-dot menu and click Info → Click “Copy link” in the Info drawer → Check that it offers three choices: 1 hour, 24 hours and 7 days → Click “7 days” → Check that a 7-day link is made and “Link copied” shows → Close the Info drawer
  - Expected: The Info drawer’s Copy link offers 1 hour, 24 hours and 7 days, and 7 days makes a link valid for a week.

- **"Send link by email" emails the recipient (E2E_REAL_SEND) @drive** — skipped (drive.spec.js:2705)
  - Where: Tools → Drive → My Drive → “[e2e] drive …” → “e2e-drive-…-area-links” · account: iPhone XRR
  - Setup: Opens the run’s area folder “e2e-drive-…-area-links” and uploads a PDF “…-l06.pdf”.
  - Steps: Open the PDF’s menu, click Share and open the “Share via link” tab → Type the test email address in the recipients box → Click “Send link by email” → Check that the link is sent to exactly that one recipient → Check for “Link sent to 1 recipient” → Close the drawer
  - Expected: “Send link by email” emails the link to the recipient and confirms it was sent to one recipient.

- **a "Can edit" link answers the bare URL with "Invite only" (E2E_REAL_SEND) @drive** — skipped (drive.spec.js:2733)
  - Where: Tools → Drive → My Drive → “[e2e] drive …” → “e2e-drive-…-area-links” · account: iPhone XRR (plus a signed-out visitor)
  - Setup: Opens the run’s area folder “e2e-drive-…-area-links” and uploads a spreadsheet “…-l13.xlsx”.
  - Steps: Open the spreadsheet’s menu, click Share and open the “Share via link” tab → Type the test email address in the recipients box → Pick “Can edit” in Permission → Click “Send link by email” and check that an edit link is made → Close the drawer → Open the bare link in a signed-out browser → Check that the page says “Invite only”
  - Expected: A “Can edit” link opened without the recipient’s invite answers “Invite only”.

### Drive — file requests and the public uploader

- **a request from inside a folder: created, copied, used by an outsider, revoked @drive** — passed (drive.spec.js:2834)
  - Where: Tools → Drive → My Drive → “[e2e] drive …” → “e2e-drive-…-area-requests” → Request Files · account: iPhone XRR (plus a signed-out uploader)
  - Setup: Opens the run’s area folder “e2e-drive-…-area-requests” (created on first use).
  - Steps: Click Request Files and check that “Uploads go to” names the open folder → Type the title “[e2e] request r03 …”, the Thank-you message “Thanks from the e2e suite”, and “not-an-email” in “Email the link to” → Click “Create request” and check the request targets the open folder, with no recipients and the warning “1 invalid email skipped” → Check the “Request created” panel shows the link; click Copy and check “Link copied” and the clipboard → Check that the request is listed in the drawer, then close it → In a signed-out browser open the link, click Continue with no email, and check “Please enter your email to continue.” → Type “e2e-uploader@example.com”, click Continue, and pick an image “…-r06-public.png” → Check the image is marked Uploaded and the page says “Upload complete” with “Thanks from the e2e suite” → Back in Drive, open the area folder and check the image is listed → Revoke the request from Request Files, open the link signed-out again, and check it says the link was revoked
  - Expected: A request made inside a folder drops bad emails, gives a copyable link, lets an outsider upload into that folder with a thank-you message, and stops working once revoked.

- **at the root, "An existing folder" puts the request there — no new folder is made @drive** — failed (drive.spec.js:2918)
  - Where: Tools → Drive → My Drive (root) → Request Files, in a second browser tab · account: iPhone XRR
  - Setup: Opens the run’s area folder “e2e-drive-…-area-requests” (created on first use).
  - Steps: In a second tab, open Drive, click My Drive and click Request Files → Choose “An existing folder” → Search the folder picker for the area folder’s name and pick it → Type the title “[e2e] request r01 …” → Click “Create request” → Check that the request targets the area folder and no new folder is created → Check that the “Request created” panel shows
  - Expected: At the root, “An existing folder” puts the request in the picked folder without making a new one.
  - Web error: TimeoutError: page.waitForResponse: Timeout 60000ms exceeded while waiting for event "response"

- **request rules: "Images only" refuses a PDF; "1 file" hides the drop zone after one upload @drive** — notrun (drive.spec.js:2956)
  - Where: Tools → Drive → My Drive → “[e2e] drive …” → “e2e-drive-…-area-requests” → Request Files · account: iPhone XRR (plus a signed-out uploader)
  - Setup: Opens the run’s area folder “e2e-drive-…-area-requests” (created on first use).
  - Steps: Create a request “[e2e] request r08 images …” with “Allowed file types” set to “Images only”, check it allows images only, and close the drawer → Create a request “[e2e] request r08 single …” with “Files per upload” set to “1 file”, check it allows one file, and close the drawer → In a signed-out browser open the images-only link, type “e2e-uploader@example.com” and click Continue → Pick a PDF and check “This file type is not allowed.” → Pick an image and check that it is marked Uploaded → In a new signed-out tab open the one-file link, type the same email and click Continue → Check that the file picker takes only one file → Pick an image, check that it is marked Uploaded, and check that the drop zone disappears
  - Expected: An “Images only” request refuses a PDF but takes an image, and a “1 file” request removes the drop zone after one upload.

- **a request emailed to a recipient (E2E_REAL_SEND) @drive** — notrun (drive.spec.js:3012)
  - Where: Tools → Drive → My Drive → “[e2e] drive …” → “e2e-drive-…-area-requests” → Request Files · account: iPhone XRR
  - Setup: Opens the run’s area folder “e2e-drive-…-area-requests” (created on first use).
  - Steps: Click Request Files → Type the title “[e2e] request r04 …” → Type the test email address in “Email the link to” → Click “Create request” → Check that the request is sent to exactly that address → Check for a message that the link was emailed → Close the drawer
  - Expected: A file request with a recipient emails the upload link and says so.

### Drive — two users

- **a Viewer on a folder: view-only menu, no add actions, no delete or move @drive** — passed (drive.spec.js:3086)
  - Where: Tools → Drive → My Drive (iPhone XRR), then Tools → Drive → Shared with me (Lava Device) · account: iPhone XRR and Lava Device
  - Setup: As iPhone XRR, inside a private test folder in My Drive, creates a folder “e2e-drive-…-s10-viewer” holding one png file, then shares the folder with Lava Device as Viewer (Share → tick Viewer → Save Access).
  - Steps: As Lava Device, open Drive → Shared with me and search for the folder → Check that the folder row shows the shared (team) icon and iPhone XRR’s name → Open the folder’s ⋯ menu and check it offers only Open folder and Info (besides the favourites item) → Close the menu and open the folder → Check that Upload, New, Create Folder and Request Files are not shown → Hover the png file row and check it has no delete (bin) button → Tick the png file’s checkbox → Check that the selection bar reads “1 item selected” and has no Delete and no Move to... button
  - Expected: A Viewer gets a view-only folder: only Open folder, favourite and Info in its menu, no add actions inside, and no way to delete or move the file in it.

- **an Editor adds files to the folder (ZL-21871) but never sees Move (ZL-21869); A sees the file live @drive** — passed (drive.spec.js:3130)
  - Where: Tools → Drive → My Drive (iPhone XRR, inside the shared folder), and Tools → Drive → Shared with me (Lava Device) · account: iPhone XRR and Lava Device
  - Setup: As iPhone XRR, inside a private test folder in My Drive, creates a folder “e2e-drive-…-s11-editor”, shares it with Lava Device as Editor, then opens the folder and leaves it open.
  - Steps: As Lava Device, open Drive → Shared with me and search for the folder → Open the folder’s ⋯ menu and check it has Edit Info and Download as ZIP, but no Share, Move to... or Delete → Close the menu and open the folder → Check that Upload, New and Create Folder are shown → Upload a png file through Upload → Choose files → Upload → Tick the uploaded file’s checkbox and check that the selection bar has no Move to... button → Without reloading, check that iPhone XRR’s open folder now lists the png Lava Device uploaded
  - Expected: An Editor can add files inside the shared folder but is never offered Move, Share or Delete; the owner sees the new file appear live. If Lava Device lacks the Drive posting right, the add actions stay hidden and the test stops after the menu check.

- **an Owner role on a folder gives B "Share" under Shared with me @drive** — passed (drive.spec.js:3172)
  - Where: Tools → Drive → My Drive (iPhone XRR), then Tools → Drive → Shared with me (Lava Device) · account: iPhone XRR and Lava Device
  - Setup: As iPhone XRR, inside a private test folder in My Drive, creates a folder “e2e-drive-…-s12-owner” and shares it with Lava Device as Owner.
  - Steps: As Lava Device, open Drive → Shared with me → Search for the folder → Open the folder’s ⋯ menu → Check that the menu offers Share → Close the menu
  - Expected: With the Owner role on a shared folder, Lava Device can share it on (the menu has Share).

- **file levels as B sees them: View, then Download, then Edit (ZL-21915) @drive** — failed (drive.spec.js:3192)
  - Where: Tools → Drive → My Drive (iPhone XRR), and Tools → Drive → Shared with me (Lava Device) · account: iPhone XRR and Lava Device
  - Setup: As iPhone XRR, uploads a pdf and a zip file into a private test folder in My Drive and shares both with Lava Device at the View level.
  - Steps: As Lava Device, open Drive → Shared with me, search for the pdf and check its ⋯ menu has only Preview and Info (besides the favourites item) → Hover the pdf and click its eye (preview) button; check the PDF viewer has no Download button, then Close it → Tick the pdf’s checkbox and check that the selection bar has no Download button → Search for the zip, preview it, and check the popup says you can view this file but not download it and has no “Open in new tab” button → As iPhone XRR, raise Lava Device to Download on the pdf (Share → tick Download → Save Access) → As Lava Device, search the pdf again and check its menu now has Download → As iPhone XRR, raise Lava Device to Edit on the pdf → As Lava Device, search the pdf again and check its menu has Share, Copy link and Edit Info, but no Move to...
  - Expected: Each file level adds exactly its actions: View allows preview only (no Download anywhere, no Open in new tab), Download adds Download, Edit adds Share, Copy link and Edit Info; Move is never offered on a shared file.
  - Web error: Error: expect(locator).toBeVisible() failed

- **Shared with me root lists the folder B can open, not its ancestor (REL-BUG-05) @drive** — notrun (drive.spec.js:3261)
  - Where: Tools → Drive → My Drive (iPhone XRR), then Tools → Drive → Shared with me → Folders tab (Lava Device) · account: iPhone XRR and Lava Device
  - Setup: As iPhone XRR, inside a private test folder in My Drive, creates a folder “e2e-drive-…-b12-parent” with a folder “e2e-drive-…-b12-child” inside it, and shares only the child with Lava Device as Viewer.
  - Steps: As Lava Device, open Drive → Click Shared with me → Click the Folders tab → Read the folder names on every page of the list → Check that the child folder is listed → Check that the parent folder is not listed
  - Expected: Shared with me lists the folder Lava Device was given, not the parent folder above it.

- **removing B (one of two people) saves a shorter list; B’s row goes without a reload (ZL-21864, ZL-21815) @drive** — notrun (drive.spec.js:3286)
  - Where: Tools → Drive → My Drive (iPhone XRR), and Tools → Drive → Shared with me (Lava Device) · account: iPhone XRR and Lava Device
  - Setup: As iPhone XRR, inside a private test folder in My Drive, creates a folder “e2e-drive-…-s14-revoke” and shares it as Viewer with Lava Device and with one more project member chosen for the run; checks the saved access list holds two people.
  - Steps: As Lava Device, open Drive → Shared with me and search for the folder → As iPhone XRR, open the folder’s ⋯ menu → Share (Manage Access) → Search “Lava Device” in Search users... and untick Lava Device’s row → Click Save Access → Check that the saved access list now holds only one person → Check that no “Shared with N user(s)” success message appears → Without reloading, check that the folder disappears from Lava Device’s Shared with me list
  - Expected: Removing one of two people saves a list with only the other person, shows no misleading “shared with” message, and the folder leaves Lava Device’s list live.

- **a new share raises B’s "Shared with me" badge; opening the folder lowers it @drive** — notrun (drive.spec.js:3321)
  - Where: Tools → Drive → Shared with me (Lava Device), and Tools → Drive → My Drive (iPhone XRR) · account: iPhone XRR and Lava Device
  - Steps: As Lava Device, open Drive → Shared with me and note the number on the Shared with me tab badge → As iPhone XRR, create a folder “e2e-drive-…-rt02-badge” in a private test folder in My Drive → Share that folder with Lava Device as Viewer → As Lava Device, wait (up to a minute) and check the badge number went up → Search for the new folder and open it → Check that the badge number goes down again
  - Expected: A new share raises Lava Device’s Shared with me badge without a reload, and opening the shared folder lowers it.

- **A’s rename reaches B’s list without a reload @drive** — notrun (drive.spec.js:3350)
  - Where: Tools → Drive → My Drive (iPhone XRR), and Tools → Drive → Shared with me (Lava Device) · account: iPhone XRR and Lava Device
  - Setup: As iPhone XRR, inside a private test folder in My Drive, creates a folder “e2e-drive-…-rt04-rename” and shares it with Lava Device as Viewer.
  - Steps: As Lava Device, open Drive → Shared with me and search for the folder → As iPhone XRR, open the folder’s ⋯ menu → Edit Info → Change the name to the same name plus “-renamed” → Click Save Changes → Without reloading, check that Lava Device’s list shows the new name
  - Expected: The owner’s rename appears in Lava Device’s open list within about 45 seconds, without a reload.

- **A deletes a shared folder: B’s row goes; A restores it: it comes back @drive** — notrun (drive.spec.js:3375)
  - Where: Tools → Drive → My Drive (iPhone XRR), and Tools → Drive → Shared with me (Lava Device) · account: iPhone XRR and Lava Device
  - Setup: As iPhone XRR, inside a private test folder in My Drive, creates a folder “e2e-drive-…-rt05-delete” and shares it with Lava Device as Viewer.
  - Steps: As Lava Device, open Drive → Shared with me and search for the folder → As iPhone XRR, open the folder’s ⋯ menu → Delete and confirm → Without reloading, check that the folder disappears from Lava Device’s list → As iPhone XRR, click View Trash → Click Restore on the folder’s row, then Back to Drive → Without reloading, check that the folder comes back in Lava Device’s list
  - Expected: Deleting a shared folder removes it from Lava Device’s open list live, and restoring it from the trash brings it back live.

- **a comment A adds shows in B’s open Info panel, and goes when deleted @drive** — notrun (drive.spec.js:3403)
  - Where: Tools → Drive → My Drive (iPhone XRR), and Tools → Drive → Shared with me (Lava Device) · account: iPhone XRR and Lava Device
  - Setup: As iPhone XRR, uploads a png file into a private test folder in My Drive and shares it with Lava Device at the View level.
  - Steps: As Lava Device, open Drive → Shared with me, search for the png and open its ⋯ menu → Info → As iPhone XRR, open the same file’s ⋯ menu → Info → Type a comment “e2e live comment …” in Add a comment... and press Enter → Check that the comment appears in Lava Device’s open Info panel → As iPhone XRR, click the comment’s delete button and confirm Delete → Check that the comment disappears from Lava Device’s open Info panel
  - Expected: A comment the owner adds shows up in the other person’s open Info panel without a reload, and disappears there once deleted.

- **without the Drive posting right, add actions hide and a guarded action asks for permission @drive** — notrun (drive.spec.js:3443)
  - Where: Tools → Drive → My Drive, then Shared with me (Lava Device) · account: iPhone XRR and Lava Device
  - Setup: As iPhone XRR, inside a private test folder in My Drive, creates a folder “e2e-drive-…-o08-guard” and shares it with Lava Device as Viewer.
  - Steps: As Lava Device, open Drive → My Drive → Check that Upload, New, Create Folder and Request Files are not shown → Click Shared with me and search for the shared folder → Open the folder’s ⋯ menu → Add to favorites → Check that a permission dialog opens instead → Check that the folder was not added to favourites → Press Escape to close the dialog
  - Expected: Without the Drive posting right, the add actions are hidden and a guarded action (adding a favourite) asks for permission instead of doing anything.

- **both people in one document show as presence chips @drive** — notrun (drive.spec.js:3477)
  - Where: Tools → Drive → My Drive (iPhone XRR), and Tools → Drive → Shared with me (Lava Device) · account: iPhone XRR and Lava Device
  - Setup: As iPhone XRR, uploads a Word document (.docx) into a private test folder in My Drive and shares it with Lava Device at the Edit level.
  - Steps: As iPhone XRR, open the document’s ⋯ menu → Open in Editor and wait for the editor → As Lava Device, open Drive → Shared with me and search for the document → Click the document’s name and choose Open in Editor → Wait for Lava Device’s editor to open → Check that iPhone XRR’s editor header shows at least two presence chips (both people) → Click Back to Drive in both editors
  - Expected: When both people have the same document open, the editor shows a presence chip for each of them (two or more).

- **another person’s restore offers a Reload in the open editor (REL-BUG-08) @drive** — notrun (drive.spec.js:3512)
  - Where: Tools → Drive → My Drive (iPhone XRR), and Tools → Drive → Shared with me (Lava Device) · account: iPhone XRR and Lava Device
  - Setup: As iPhone XRR, uploads a Word document (.docx) into a private test folder in My Drive and shares it with Lava Device at the Edit level.
  - Steps: As iPhone XRR, open the document’s ⋯ menu → Version history → Check that the Version history panel lists at least two versions → As Lava Device, open Drive → Shared with me, search for the document, click its name and choose Open in Editor → As iPhone XRR, open the older version’s menu → Restore this version and confirm → Check that Lava Device’s open editor shows a bar saying the file was restored to an earlier version
  - Expected: When someone else restores an older version, the person with the document open is told it was restored to an earlier version (with a Reload offer).

- **"Let others know" Send announces once a minute (E2E_REAL_SEND) @drive** — notrun (drive.spec.js:3553)
  - Where: Tools → Drive → My Drive, inside a test folder shared with Lava Device · account: iPhone XRR
  - Setup: Inside a private test folder in My Drive, creates a folder “e2e-drive-…-rt08-notify”, shares it with Lava Device as Editor, opens it and creates a new Markdown file there (New → Markdown → name → Enter).
  - Steps: In the Markdown editor, type “# first change”, click Save, then click Back to Drive → In the “Let others know you updated this file?” prompt, click Send → Check that the announcement is accepted and the prompt closes → Open the file’s ⋯ menu → Edit file → Type “# second change”, click Save, then Back to Drive → Click Send again in the prompt → Check that this second announcement is refused and an error message appears → Click Skip
  - Expected: The first “Let others know” Send notifies everyone with access; a second Send within a minute is refused with an error message.

### Drive — flaky: drag and drop

- **dragging a file row onto a folder row moves it (Classic layout) @drive** — passed (drive.spec.js:3630)
  - Where: Tools → Drive → My Drive, in the Classic (full-page) layout, inside a test folder · account: iPhone XRR
  - Setup: Switches the app to the Classic (full-page) layout; inside a private test folder in My Drive, creates a folder “e2e-drive-…-a08-target” and uploads a png “e2e-drive-…-a08-file.png” next to it.
  - Steps: Close the Operations panel at the bottom right → Press the mouse down on the png file’s row → Move the pointer a little to start the drag → Move the pointer onto the folder’s row → Check that the folder row is highlighted as the drop target → Release the mouse → Check that a “moved” message appears and the png row leaves the list
  - Expected: Dropping a file row on a folder row moves the file into that folder: the move succeeds, a “moved” message shows and the file leaves the open list.

- **dragging a file row onto a folder row moves it (Windowed view) @drive** — bug · known bug Windowed view: the drag chip (DragOverlay, position: fixed) sits inside the transformed tool window, drawn one window-offset away from the pointer, and the drop is tested against the chip — the folder row under the pointer never becomes the target (same trap as the row-menu anchor, toFixedAnchorPosition) — confirmed run 2,Windowed view: the drag chip (DragOverlay, position: fixed) sits inside the transformed tool window, drawn one window-offset away from the pointer, and the drop is tested against the chip — the folder row under the pointer never becomes the target (same trap as the row-menu anchor, toFixedAnchorPosition) — confirmed run 2 (drive.spec.js:3630)
  - Where: Tools → Drive → My Drive, in the Windowed view (Drive inside a tool window), inside a test folder · account: iPhone XRR
  - Setup: Inside a private test folder in My Drive, creates a folder “e2e-drive-…-a08w-target” and uploads a png “e2e-drive-…-a08w-file.png” next to it.
  - Steps: Close the Operations panel at the bottom right → Press the mouse down on the png file’s row → Move the pointer a little to start the drag → Move the pointer onto the folder’s row → Check that the folder row is highlighted as the drop target → Release the mouse → Check that a “moved” message appears and the png row leaves the list
  - Expected: In the Windowed view too, dropping a file row on a folder row moves the file into that folder. Known bug: the dragged chip is drawn away from the pointer (offset by the window’s position) and the drop is measured against the chip, so the folder under the pointer never becomes the target and nothing moves.
  - Web error: Error: expect(locator).toHaveClass(expected) failed

- **dropping a file on the page asks for permissions, then uploads into the open folder @drive** — passed (drive.spec.js:3660)
  - Where: Tools → Drive → My Drive, inside a test folder · account: iPhone XRR
  - Setup: Opens a private test folder in My Drive.
  - Steps: Drag a png file “e2e-drive-…-u08-dropped.png” from the computer over the Drive page → Check that the drop overlay says “Drop files to upload” and names the open folder → Drop the file → Check that the “Set File Permissions” dialog says “1 file ready to upload” → Click “Skip & Upload” → Check that the upload goes into the open folder and the png appears in the list
  - Expected: A file dropped on the page goes through the permissions dialog and is uploaded into the folder that is open.

- **a page drop refuses unsupported or empty files and more than 100 files @smoke @drive** — passed (drive.spec.js:3683)
  - Where: Tools → Drive → My Drive · account: iPhone XRR
  - Steps: Drag an unsupported “.xyz” file and an empty “.txt” file together onto the Drive page and drop them → Check that a warning says “2 file(s) skipped” → Check that no “Set File Permissions” dialog opens → Drag 101 png files onto the page and drop them → Check that a warning says “Maximum 100 files per upload” and “101 files were dropped” → Check that no “Set File Permissions” dialog opens and no upload starts
  - Expected: A page drop skips unsupported and empty files with a warning, and refuses a drop of more than 100 files outright; nothing is uploaded.

### Drive — flaky: live editor

- **New → Document opens straight into the editor in edit mode @drive** — passed (drive.spec.js:3737)
  - Where: Tools → Drive → My Drive, inside a test folder · account: iPhone XRR
  - Setup: Opens a private test folder in My Drive.
  - Steps: Click New → Document → Type the name “e2e-drive-…-n07” in File name and press Enter → Check that the new file is named with “.docx” at the end → Check that the editor opens for editing, showing the file name as its title and no “View only” tag → Click Back to Drive → Check that no “Let others know you updated this file?” prompt appears → Check that the new document is listed in the folder
  - Expected: A new document opens straight into the editor in edit mode, and closing it without typing asks nothing and leaves the file in the list.

- **an Office Preview opens view-only, and Edit reopens it for editing @drive** — passed (drive.spec.js:3758)
  - Where: Tools → Drive → My Drive, inside a test folder · account: iPhone XRR
  - Setup: Uploads a Word document “e2e-drive-…-p07.docx” into a private test folder in My Drive.
  - Steps: Hover the document’s row and click its eye (preview) button → Check that a message says it is opening in preview mode → Check that the editor shows a “View only” tag → Click Edit in the editor header → Check that the editor reopens for editing and the “View only” tag goes → Click Back to Drive
  - Expected: Previewing an Office file opens it read-only in the editor, and Edit reopens it in edit mode. If the editor offers no Edit button, that is only noted.

- **a .csv opens in the sheet editor @drive** — passed (drive.spec.js:3791)
  - Where: Tools → Drive → My Drive, inside a test folder · account: iPhone XRR
  - Setup: Uploads a csv file “e2e-drive-…-p08b.csv” into a private test folder in My Drive.
  - Steps: Open the csv file’s ⋯ menu → Click Open in Editor → Wait for the editor to open → Check that it is the spreadsheet (sheet) editor → Click Back to Drive
  - Expected: A .csv file opens in the spreadsheet editor.

- **editor chrome: title, status, Full screen, Version history panel @drive** — passed (drive.spec.js:3801)
  - Where: Tools → Drive → My Drive, inside a test folder · account: iPhone XRR
  - Setup: Uploads a Word document “e2e-drive-…-e01.docx” into a private test folder in My Drive.
  - Steps: Open the document’s ⋯ menu → Open in Editor → Check that the editor header shows Back to Drive, the file name and a save status → Click Full screen, then Exit full screen (when the browser allows full screen) → Click Version history → Check that the Version history panel opens → Click Close version history and check the panel goes → Click Back to Drive
  - Expected: The editor header carries Back to Drive, the file name, a status, a working Full screen toggle and a Version history panel that opens and closes.

- **naming the current version shows the "Only named versions" filter @drive** — bug · known bug "Name current version" on a file with no saved version sends PATCH /versions/:file/current (the history’s synthetic id); the server answers 406 "params: It must have a valid ObjectId." — confirmed run 2,"Name current version" on a file with no saved version sends PATCH /versions/:file/current (the history’s synthetic id); the server answers 406 "params: It must have a valid ObjectId." — confirmed run 2 (drive.spec.js:3840)
  - Where: Tools → Drive → My Drive, inside a test folder · account: iPhone XRR
  - Setup: Uploads a Word document “e2e-drive-…-e03.docx” into a private test folder in My Drive.
  - Steps: Open the document’s ⋯ menu → Open in Editor → Click Version history in the editor header → Click “Name current version” → In “Name this version”, type “e2e named version” and click Save → Check that the “Only named versions” switch appears in the panel → Check that the panel lists “e2e named version” → Click Back to Drive
  - Expected: Naming the current version saves the name, lists it, and shows the “Only named versions” filter. Known bug: on a freshly uploaded file with no saved version, the name is sent for a placeholder “current” version and the server refuses it, so nothing is named.
  - Web error: Error: name version: params: It must have a valid ObjectId.

- **viewing and restoring an older version @drive** — skipped (drive.spec.js:3873)
  - Where: Tools → Drive → My Drive, inside a test folder · account: iPhone XRR
  - Setup: Uploads a Word document “e2e-drive-…-e04.docx” into a private test folder in My Drive.
  - Steps: Open the document’s ⋯ menu → Open in Editor → Click Version history and check it lists at least two versions → Click the older version → Check that the editor bar says you are viewing that version → Click “Back to the current version” → Open the older version’s menu → Restore this version → Check the “Restore this version?” confirm and press OK → Check that the restore succeeds, then click Back to Drive
  - Expected: An older version can be viewed (the bar says so, with a way back to the current one) and restored after a confirm.

- **typing in the editor marks an edit; closing asks to let others know (Skip) @drive** — passed (drive.spec.js:3917)
  - Where: Tools → Drive → My Drive, inside a test folder · account: iPhone XRR
  - Setup: Inside a private test folder in My Drive, creates a new document with New → Document, which opens in the editor.
  - Steps: Wait for the document to finish loading in the editor → Click inside the document and type “Typed by the Drive e2e suite.” → Check that the header status changes to Unsaved changes, Saving or All changes saved → Wait a few seconds, then click Back to Drive → Check that the “Let others know you updated this file?” prompt appears and names the file → Click Skip → Check that the prompt closes and no announcement is sent
  - Expected: Typing marks the document as edited, and closing it asks whether to let others know; Skip closes the prompt without notifying anyone.

- **Version history opens from the row menu and from Info @drive** — passed (drive.spec.js:3950)
  - Where: Tools → Drive → My Drive, inside a test folder · account: iPhone XRR
  - Setup: Uploads a Word document “e2e-drive-…-e16.docx” into a private test folder in My Drive.
  - Steps: Open the document’s ⋯ menu → Version history → Check that the editor opens with the Version history panel showing → Click Back to Drive → Open the document’s ⋯ menu → Info → Click “Open version history” in the Info panel → Check that the editor opens with the Version history panel showing → Click Back to Drive
  - Expected: Version history opens in the editor both from the row menu and from the Info panel.

- **an Office file on a public link opens read-only (editor, or the PDF fallback) @drive** — passed (drive.spec.js:3973)
  - Where: Tools → Drive → My Drive, inside a test folder; then the share link in a signed-out browser · account: iPhone XRR
  - Setup: Uploads an Excel file “e2e-drive-…-l11.xlsx” into a private test folder in My Drive.
  - Steps: Open the file’s ⋯ menu → Share → In Share File, open the “Share via link” tab → Click Generate link and copy the link → Close the drawer → Open the link in a signed-out browser window → Check that the page shows the file name as its title → Check that the file shows read-only: in the editor, as a PDF, or as the plain fallback block
  - Expected: An outsider opening a public link to an Office file sees it by name, read-only (in the editor, or the PDF or plain fallback).

### Drive — flaky: downloads and popups

- **the row menu’s Download saves the file @drive** — passed (drive.spec.js:4018)
  - Where: Tools → Drive → My Drive, inside a test folder · account: iPhone XRR
  - Setup: Uploads a png file “e2e-drive-…-d01.png” into a private test folder in My Drive.
  - Steps: Open the png file’s ⋯ menu → Click Download → Check that the app gets a download link for the file → Check that the browser starts downloading the file
  - Expected: The row menu’s Download fetches a download link and the file is downloaded.

- **the preview modal’s Download saves the file @drive** — passed (drive.spec.js:4031)
  - Where: Tools → Drive → My Drive, inside a test folder · account: iPhone XRR
  - Setup: Uploads a png file “e2e-drive-…-d02.png” into a private test folder in My Drive.
  - Steps: Hover the png file’s row and click its eye (preview) button → Check that the preview popup shows the image → Click Download in the preview → Check that the browser starts downloading the file → Close the preview
  - Expected: Download in the image preview downloads the file.

- **a folder downloads as a .zip through a short-lived link @drive** — passed (drive.spec.js:4049)
  - Where: Tools → Drive → My Drive, inside a test folder · account: iPhone XRR
  - Setup: Inside a private test folder in My Drive, creates a folder “e2e-drive-…-d03-folder” with one png file inside, then goes back to the test folder.
  - Steps: Open the folder’s ⋯ menu → Click Download as ZIP → Check that the app gets a short-lived download link for the zip → Check that the browser downloads a file whose name ends in .zip
  - Expected: A folder downloads as one .zip file through a short-lived link.

- **bulk Download of one file opens it @drive** — passed (drive.spec.js:4068)
  - Where: Tools → Drive → My Drive, inside a test folder · account: iPhone XRR
  - Setup: Uploads a png file “e2e-drive-…-d04.png” into a private test folder in My Drive.
  - Steps: Tick the png file’s checkbox → Click Download in the selection bar → Check that the app gets the file’s download link → Check that the browser starts downloading the file
  - Expected: A bulk Download with a single file selected downloads that file itself (no zip).

- **bulk Download of two files is one server .zip @drive** — passed (drive.spec.js:4082)
  - Where: Tools → Drive → My Drive, inside a test folder · account: iPhone XRR
  - Setup: Turns off the browser’s “choose where to save” dialog so the zip is built on the server; uploads two png files “e2e-drive-…-d05-one.png” and “…-d05-two.png” into a private test folder in My Drive.
  - Steps: Tick both png files’ checkboxes → Click Download in the selection bar → Check that the app asks the server for one zip link covering both files → Check that the browser downloads one file whose name ends in .zip
  - Expected: Downloading two selected files gives a single .zip made by the server, holding both files.

- **the Info panel downloads a saved version @drive** — skipped (drive.spec.js:4107)
  - Where: Tools → Drive → My Drive, inside a test folder · account: iPhone XRR
  - Setup: Uploads a Word document “e2e-drive-…-d07.docx” into a private test folder in My Drive.
  - Steps: Open the document’s ⋯ menu → Info → Find a saved version in the Info panel’s version list → Click that version’s “Download this version” button → Check that the app gets the version’s download link → Check that the browser starts downloading the file → Close the Info panel
  - Expected: A saved version can be downloaded from the Info panel.

- **a "View + download" link lets the outsider download @drive** — passed (drive.spec.js:4128)
  - Where: Tools → Drive → My Drive, inside a test folder; then the share link in a signed-out browser · account: iPhone XRR
  - Setup: Uploads a pdf file “e2e-drive-…-l09.pdf” into a private test folder in My Drive.
  - Steps: Open the pdf’s ⋯ menu → Share, then the “Share via link” tab → Pick “View + download” under Permission → Click Generate link and check the link is made with view-and-download access → Close the drawer → Open the link in a signed-out browser window → Check that the page shows a Download link → Click Download and check that the browser downloads the file
  - Expected: An outsider opening a “View + download” link can download the file.

- **"Open in new tab" opens the file in a popup @drive** — passed (drive.spec.js:4165)
  - Where: Tools → Drive → My Drive, inside a test folder · account: iPhone XRR
  - Setup: Uploads a zip file “e2e-drive-…-p06b.zip” into a private test folder in My Drive.
  - Steps: Hover the zip file’s row and click its eye (preview) button → In the preview popup, click “Open in new tab” → Check that a new browser tab opens → Close the new tab → Close the preview
  - Expected: “Open in new tab” in the preview of a file the browser cannot show opens it in a new tab.

### Drive — flaky: uploads in flight

- **pause, resume and cancel a large upload @drive** — passed (drive.spec.js:4214)
  - Where: Tools → Drive → My Drive, inside a test folder · account: iPhone XRR
  - Setup: Makes a 100 MB text file “e2e-drive-…-u10-big.txt” on the computer and opens a private test folder in My Drive.
  - Steps: Click Upload → Choose files, pick the 100 MB file and click Upload → In the Operations panel, check the file’s row says Uploading → Click its Pause button and check it says Paused → Click its Resume button and check it says Uploading again → Click its Cancel button → Check that an “Upload Cancelled” notification appears
  - Expected: A large upload can be paused, resumed and cancelled from the Operations panel; cancelling shows “Upload Cancelled”. The test slows the upload so there is always time to pause.

- **an interrupted upload is offered for recovery and resumes from its saved parts @drive** — passed (drive.spec.js:4243)
  - Where: Tools → Drive → My Drive, inside a test folder · account: iPhone XRR
  - Setup: Makes a 100 MB text file “e2e-drive-…-u11-big.txt” on the computer and opens a private test folder in My Drive.
  - Steps: Click Upload → Choose files, pick the 100 MB file and click Upload → Wait until some parts of the file have been sent → Close the browser tab in the middle of the upload → Open Drive again in a new tab, in the same test folder → Check that a “Recover uploads” notification appears → Click Upload → Choose files, pick the same file and click Upload → Check that a “Resuming upload for…” message appears → Check that the upload completes and the file is listed in the folder
  - Expected: An upload cut off by closing the tab is offered for recovery, and choosing the same file again resumes from the parts already sent until it completes.

- **an upload keeps going after leaving Drive @drive** — passed (drive.spec.js:4282)
  - Where: Tools → Drive → My Drive, inside a test folder · account: iPhone XRR
  - Setup: Makes a 60 MB text file “e2e-drive-…-u14-big.txt” on the computer and opens a private test folder in My Drive.
  - Steps: Click Upload → Choose files, pick the 60 MB file and click Upload → As soon as it starts sending, click “Back to Film Tools” → Check that Drive closes → Wait for the upload to complete in the background → Open Drive again in the same test folder → Check that the uploaded file is listed
  - Expected: Leaving Drive does not stop an upload: it completes in the background and the file is in the folder afterwards.

- **the public uploader pauses, resumes and cancels an upload @drive** — passed (drive.spec.js:4296)
  - Where: Tools → Drive → My Drive, inside a test folder; then the request link in a signed-out browser · account: iPhone XRR
  - Setup: Inside a private test folder in My Drive, creates a file request “[e2e] request r09 …” (Request Files → Title → Create request) and makes a 100 MB text file on the computer.
  - Steps: Open the request link in a signed-out browser window and click “Reject all” on the cookie banner if it shows → Type “e2e-uploader@example.com” as the email and click Continue → Click the drop area and pick the 100 MB file → Once the upload starts, click Pause upload and check the file shows as paused → Click Resume upload and check the paused status goes → Click Cancel upload → Check that the file is not marked as Uploaded
  - Expected: On the public request page an outsider can pause, resume and cancel an upload; a cancelled file never shows as uploaded.

- **a one-view link answers the second visitor with "View limit reached" @drive** — passed (drive.spec.js:4331)
  - Where: Tools → Drive → My Drive, inside a test folder; then the share link in two signed-out browsers · account: iPhone XRR
  - Setup: Uploads a png file “e2e-drive-…-l12b.png” into a private test folder in My Drive.
  - Steps: Open the png’s ⋯ menu → Share, then the “Share via link” tab → Pick “1 view” under Max views → Click Generate link and check the link is made with a one-view limit → Close the drawer → Open the link in a signed-out browser window and check the image shows → Open the link again in a second, fresh signed-out window → Check that the page says “View limit reached”
  - Expected: A one-view link shows the file to the first visitor and answers the second with “View limit reached”.

### Drive — stubbed: refused writes and filters (known bugs)

- **Starred with nothing starred says so in grid view too (ZL-21810) @smoke @drive** — bug · known bug the ZL-21810 empty-state fix is in DriveTable only; DriveGridView’s empty state has no Starred case and shows "No files or folders yet" with upload hints,grid view, Starred on with nothing starred: "No files or folders yet" plus upload hints (the table view says "No favorite items found") — confirmed run 2,the ZL-21810 empty-state fix is in DriveTable only; DriveGridView’s empty state has no Starred case and shows "No files or folders yet" with upload hints,grid view, Starred on with nothing starred: "No files or folders yet" plus upload hints (the table view says "No favorite items found") — confirmed run 2 (drive.spec.js:4374)
  - Where: Tools → Drive → My Drive → Files tab · account: iPhone XRR
  - Setup: The test makes the server answer that nothing is starred, and replaces the My Drive list with one fake png file.
  - Steps: Open Drive → My Drive and click the Files tab → Click the star button (Show favorites) → Check that the list says “No favorite items found” → Click Grid view → Check that the grid also says “No favorite items found” → Check that the grid shows no upload hints
  - Expected: With the Starred filter on and nothing starred, both the list and the grid say “No favorite items found”. Known bug: the grid view says “No files or folders yet” with upload hints instead.
  - Web error: Error: expect(locator).toContainText(expected) failed

- **a refused star toggle leaves the star off and says so @smoke @drive** — bug · known bug handleToggleFavorite flips the star optimistically and never reads the response status,a refused POST /favorites/toggle shows no error — confirmed run 2,handleToggleFavorite flips the star optimistically and never reads the response status,a refused POST /favorites/toggle shows no error — confirmed run 2 (drive.spec.js:4393)
  - Where: Tools → Drive → My Drive → Files tab · account: iPhone XRR
  - Setup: The test replaces the My Drive list with one fake png file and makes the server refuse any star (favourite) change.
  - Steps: Open Drive → My Drive and click the Files tab → Hover the png file’s row → Click its star → Check that an error message appears → Check that the star stays off
  - Expected: When starring is refused, the user gets an error and the star stays off. Known bug: no error appears and the star turns on as if it had worked.
  - Web error: Error: expect(locator).toBeVisible() failed

- **a refused bulk delete is reported @smoke @drive** — bug · known bug handleBulkDelete has a success branch only; a refused POST /bulk/delete shows nothing,a refused POST /bulk/delete shows no error (handleBulkDelete has a success branch only) — confirmed in code; run 2 stopped earlier on a test selector,handleBulkDelete has a success branch only; a refused POST /bulk/delete shows nothing,a refused POST /bulk/delete shows no error (handleBulkDelete has a success branch only) — confirmed in code; run 2 stopped earlier on a test selector (drive.spec.js:4410)
  - Where: Tools → Drive → My Drive → Files tab · account: iPhone XRR
  - Setup: The test replaces the My Drive list with two fake png files and makes the server refuse a bulk delete.
  - Steps: Open Drive → My Drive and click the Files tab → Tick both files’ checkboxes → Click Delete in the selection bar → Confirm the delete → Check that an error message appears
  - Expected: A refused bulk delete is reported with an error message. Known bug: nothing is shown at all.
  - Web error: Error: expect(locator).toBeVisible() failed

- **a refused "Generate link" is reported, not silent @smoke @drive** — bug · known bug ShareViaLink.handleGenerate never reads the status: a refused create shows nothing (no recipients) or "Link sent to N recipients",a refused "Generate link" (POST …/email-share-link) shows no error — confirmed run 2,ShareViaLink.handleGenerate never reads the status: a refused create shows nothing (no recipients) or "Link sent to N recipients",a refused "Generate link" (POST …/email-share-link) shows no error — confirmed run 2 (drive.spec.js:4429)
  - Where: Tools → Drive → My Drive → Files tab, file ⋯ menu → Share → Share via link · account: iPhone XRR
  - Setup: The test replaces the My Drive list with one fake pdf (no one shares it, no links yet) and makes the server refuse creating a share link.
  - Steps: Open Drive → My Drive and click the Files tab → Open the pdf’s ⋯ menu → Share → Click the “Share via link” tab → Click Generate link → Check that an error message appears → Check that no success message appears
  - Expected: A refused “Generate link” shows an error and no success message. Known bug: nothing is shown; the failure is silent.
  - Web error: Error: expect(locator).toBeVisible() failed

- **a refused file request is reported, not "Request link created" @smoke @drive** — bug · known bug RequestFilesDrawer.submit never reads the status: a refused create toasts "Request link created" and shows an empty link panel,a refused POST /file-requests shows "Request created" with an empty link box and no error — confirmed run 2,RequestFilesDrawer.submit never reads the status: a refused create toasts "Request link created" and shows an empty link panel,a refused POST /file-requests shows "Request created" with an empty link box and no error — confirmed run 2 (drive.spec.js:4459)
  - Where: Tools → Drive → My Drive, folder ⋯ menu → Request files · account: iPhone XRR
  - Setup: The test replaces the My Drive list with one fake folder (with no requests yet) and makes the server refuse creating a file request.
  - Steps: Open Drive → My Drive → Open the folder’s ⋯ menu → Request files → Type “[e2e] stubbed refusal” as the Title → Click Create request → Check that an error message appears → Check that no “request created” link panel is shown
  - Expected: A refused file request shows an error and no created-link panel. Known bug: it shows “Request created” with an empty link box and no error.
  - Web error: Error: expect(locator).toBeVisible() failed

### Drive — edge: form, picker and search rules

- **the picker sorts names by their last extension: no extension, trailing dot, ".pdf.exe" refused; ".exe.pdf", "UPPER.PNG", ".png" taken @smoke @drive** — passed (drive.spec.js:4542)
  - Where: Tools → Drive → My Drive → Upload · account: iPhone XRR
  - Steps: Open Drive → My Drive and click Upload → Click Choose files and pick six small files: “e2e-noext”, “e2e-trailing.”, “e2e-double.pdf.exe”, “e2e-double.exe.pdf”, “E2E-UPPER.PNG” and “.png” → Check that the drawer says “3 unsupported files skipped” → Check that the skipped list names “e2e-noext”, “e2e-trailing.” and “e2e-double.pdf.exe” → Check that the drawer says “3 files selected” → Close the drawer and check nothing was uploaded
  - Expected: The picker judges each name by its last extension: a name with no extension, a trailing dot or ending in .exe is refused; “.exe.pdf”, an upper-case “.PNG” and a bare “.png” are taken.

- **every accepted type is taken by the picker, none listed as unsupported @smoke @drive** — passed (drive.spec.js:4570)
  - Where: Tools → Drive → My Drive → Upload · account: iPhone XRR
  - Steps: Open Drive → My Drive and click Upload → Click Choose files and pick one small file for each of the 41 accepted types (pdf, Office, text, image, video, audio and zip) → Check that the drawer says “41 files selected” → Check that no “unsupported files skipped” notice appears → Close the drawer
  - Expected: Every file type Drive accepts is taken by the picker; none is listed as unsupported.

- **the drawer holds at most 100 files, like the page drop @smoke @drive** — bug · known bug the drawer’s maxCount sits on the Dragger, but Choose files feeds the form through ingestFiles and bypasses it; the page drop refuses more than 100,Choose files takes 101 files into the drawer ("101 files selected"); the page drop refuses more than 100 — confirmed run 2,the drawer’s maxCount sits on the Dragger, but Choose files feeds the form through ingestFiles and bypasses it; the page drop refuses more than 100,Choose files takes 101 files into the drawer ("101 files selected"); the page drop refuses more than 100 — confirmed run 2 (drive.spec.js:4586)
  - Where: Tools → Drive → My Drive → Upload · account: iPhone XRR
  - Steps: Open Drive → My Drive and click Upload → Click Choose files and pick 101 small png files → Check that the drawer does not take all 101 files → Close the drawer
  - Expected: The Upload drawer holds at most 100 files, as the page drop does. Known bug: Choose files takes all 101 (“101 files selected”).
  - Web error: Error: expect(locator).not.toContainText(expected) failed

- **a page drop of exactly 100 files is accepted (permissions prompt), then cancelled @smoke @drive** — passed (drive.spec.js:4606)
  - Where: Tools → Drive → My Drive · account: iPhone XRR
  - Steps: Open Drive → My Drive → Drag exactly 100 png files onto the page and drop them → Check that the “Set File Permissions” dialog says “100 files ready to upload” → Close the dialog with its X → Check that nothing was uploaded
  - Expected: A drop of exactly 100 files is accepted (the permissions dialog opens); closing it uploads nothing.

- **Upload with nothing chosen warns and sends nothing @smoke @drive** — passed (drive.spec.js:4627)
  - Where: Tools → Drive → My Drive → Upload · account: iPhone XRR
  - Steps: Open Drive → My Drive and click Upload → Without choosing any file, click Upload in the drawer → Check that a warning message appears → Check that no upload starts and no folder is created
  - Expected: Pressing Upload with nothing chosen only warns; nothing is sent.

- **the new-file name stops at 190 characters @smoke @drive** — passed (drive.spec.js:4641)
  - Where: Tools → Drive → My Drive → New → Spreadsheet · account: iPhone XRR
  - Steps: Open Drive → My Drive → Click New → Spreadsheet → Check that File name is prefilled with “Untitled spreadsheet” → Type 200 characters into File name → Check that the field keeps only 190 characters → Click Cancel and check the dialog closes
  - Expected: The new-file name field stops at 190 characters.

- **Create Folder caps the description at 500 and shows the count @smoke @drive** — passed (drive.spec.js:4662)
  - Where: Tools → Drive → My Drive → Create Folder · account: iPhone XRR
  - Steps: Open Drive → My Drive → Click Create Folder → Type 520 characters into the description → Check that the description keeps only 500 characters → Check that the counter shows “500 / 500” → Close the drawer without creating anything
  - Expected: The folder description stops at 500 characters and the counter shows “500 / 500”.

- **Request files caps title (200) and instructions (2000); the summary follows the rules picked @smoke @drive** — passed (drive.spec.js:4675)
  - Where: Tools → Drive → My Drive → Request Files · account: iPhone XRR
  - Steps: Open Drive → My Drive and click Request Files → Type 220 characters as the Title and check only 200 are kept → Type 2,100 characters as the Instructions and check only 2,000 are kept → Check that the summary says “expires in 7 days” and “unlimited files” → Set Link expires to Never, Files per upload to 5 files, Total size per upload to Unlimited and Allowed file types to Videos only → Check that the summary now says “never expires”, “up to 5 files per upload”, “no size limit” and “videos only” → Close the drawer and check that no request or folder was created
  - Expected: The request title stops at 200 characters and the instructions at 2,000, and the summary sentence follows the upload rules picked.

- **search takes special characters, RTL and emoji, and echoes them as text @smoke @drive** — passed (drive.spec.js:4703)
  - Where: Tools → Drive → My Drive, search box · account: iPhone XRR
  - Steps: Open Drive → My Drive → Search in turn for: [e2e], 100%, a*b, “quoted” (with double quotes), the 🎬 emoji, the Arabic word مرحبا, ../.. and <b>x</b> → After each search, check that the bar above the list repeats the search in quotes, as plain text → Check that “<b>x</b>” is shown literally, not as bold text → Check that no error message appears → Clear the search
  - Expected: Search accepts special characters, emoji and right-to-left text, and shows each query back as plain text without errors.

- **a saved path to a folder that does not exist falls back to the root @smoke @drive** — fixed · known bug after "Navigated to root as current folder no longer exists" the list is the root, but the path bar still reads "Drive / e2e-ghost-folder" — confirmed run 2,after "Navigated to root as current folder no longer exists" the list is the root, but the path bar still reads "Drive / e2e-ghost-folder" — confirmed run 2,after "Navigated to root as current folder no longer exists" the list is the root, but the path bar still reads "Drive / e2e-ghost-folder" — confirmed run 2 (drive.spec.js:4717)
  - Where: Tools → Drive (reopened in a tab that remembers a folder that never existed) · account: iPhone XRR
  - Setup: The test plants a remembered Drive location in the browser tab pointing at a made-up folder “e2e-ghost-folder” in My Drive.
  - Steps: Open Drive → Check that a message says the folder no longer exists (or that Drive went back to the root) → Check that the root list shows → Check that no folder path bar (“Drive / e2e-ghost-folder”) is left
  - Expected: Drive tells the user the remembered folder is gone and falls back to the root, with no stale path shown. Known bug: the root list loads, but the path bar still reads “Drive / e2e-ghost-folder”.

### Drive — edge: states and recovery (stubbed)

- **a list load failing with HTTP 404 is reported @smoke @drive** — bug · known bug a failed list load becomes an empty Drive with no message (see X04),a list load failing with HTTP 404 shows an empty Drive and no error — confirmed run 2,a failed list load becomes an empty Drive with no message (see X04),a list load failing with HTTP 404 shows an empty Drive and no error — confirmed run 2 (drive.spec.js:4753)
  - Where: Tools → Drive · account: iPhone XRR
  - Setup: The test makes the server answer every file and folder list with a “not found” (404) error.
  - Steps: Open Drive → Wait for the list to finish loading → Check that an error message appears within 15 seconds
  - Expected: A list load that fails with a not-found error is reported with an error message. Known bug: Drive shows an empty list and no message.
  - Web error: Error: expect(locator).toBeVisible() failed

- **a list load failing with a network abort is reported @smoke @drive** — suspected · known bug a failed list load becomes an empty Drive with no message (see X04),a failed list load becomes an empty Drive with no message (see X04),a failed list load becomes an empty Drive with no message (see X04) (drive.spec.js:4753)
  - Where: Tools → Drive · account: iPhone XRR
  - Setup: The test makes every file and folder list request fail at the network level (the connection drops).
  - Steps: Open Drive → Wait for the list request to fail → Check that an error message appears within 15 seconds
  - Expected: A list load lost to a network failure is reported with an error message. Known bug: Drive shows an empty list and no message.
  - Web error: Error: expect(locator).toBeVisible() failed

- **a slow list shows the spinner, then the rows @smoke @drive** — passed (drive.spec.js:4764)
  - Where: Tools → Drive → My Drive → Files tab · account: iPhone XRR
  - Setup: The test replaces the My Drive list with one fake png file, then makes the next list answer arrive 4 seconds late.
  - Steps: Open Drive → My Drive and click the Files tab → Click Shared with me → Click My Drive again → Check that the loading spinner shows while the list is slow → Wait for the spinner to clear and click the Files tab → Check that the png file is listed
  - Expected: A slow list shows the loading spinner, then the rows once the answer arrives.

- **after a failed load, the next refresh recovers the rows @smoke @drive** — passed (drive.spec.js:4785)
  - Where: Tools → Drive → My Drive → Files tab · account: iPhone XRR
  - Setup: The test makes every file and folder list fail with a server error at first; once Drive has opened, the lists answer again (My Drive holds one fake png file).
  - Steps: Open Drive while the lists are failing → Click Shared with me → Click My Drive → Click the Files tab → Check that the png file is listed
  - Expected: After a failed load, the next refresh (switching section) brings the rows back.

- **a preview request that never answers (abort) toasts and leaves no viewer @smoke @drive** — passed (drive.spec.js:4804)
  - Where: Tools → Drive → My Drive → Files tab · account: iPhone XRR
  - Setup: The test replaces the My Drive list with one fake pdf file and makes its preview request fail at the network level.
  - Steps: Open Drive → My Drive and click the Files tab → Hover the pdf’s row → Click its eye (preview) button → Check that an error message says “Failed to load preview” → Check that no PDF viewer opens
  - Expected: A preview that cannot load shows “Failed to load preview” and opens no viewer.

- **an editor config that fails with HTTP 500 or an abort opens nothing @smoke @drive** — passed (drive.spec.js:4819)
  - Where: Tools → Drive → My Drive → Files tab · account: iPhone XRR
  - Setup: The test replaces the My Drive list with one fake Word document and makes opening it in the editor fail with a server error, then (second try) fail on the network.
  - Steps: Open Drive → My Drive and click the Files tab → Click the document’s name and choose Open in Editor → Check that an error message appears and no editor opens → Click the document’s name again and choose Open in Editor (this time the request is dropped) → Wait two seconds and check that no editor opens
  - Expected: When the editor cannot be prepared (server error or dropped request), nothing opens; the server error is reported with a message.

- **a failed access prefill is reported and Save is held back @smoke @drive** — bug · known bug handleShare fills the list only from a status-1 GET; on a refusal the drawer opens with nobody selected and Save would REPLACE the access list,a refused access prefill (GET …/access, HTTP 500) opens the Share drawer with nobody selected, no error and Save enabled — confirmed run 2,handleShare fills the list only from a status-1 GET; on a refusal the drawer opens with nobody selected and Save would REPLACE the access list,a refused access prefill (GET …/access, HTTP 500) opens the Share drawer with nobody selected, no error and Save enabled — confirmed run 2 (drive.spec.js:4839)
  - Where: Tools → Drive → My Drive → Files tab, file ⋯ menu → Share · account: iPhone XRR
  - Setup: The test replaces the My Drive list with one fake pdf file and makes loading who already has access to it fail with a server error.
  - Steps: Open Drive → My Drive and click the Files tab → Open the pdf’s ⋯ menu → Share → Check that nothing is saved just by opening the drawer → Check that an error message appears, or that Save Access is disabled → Close the drawer
  - Expected: When the current access list cannot be loaded, the user is told or Save Access is held back, so the list cannot be overwritten by mistake. Known bug: the drawer opens with nobody selected, no error and Save Access enabled; saving would replace the whole access list.
  - Web error: Error: expect(locator).toBeVisible() failed

- **when the share-eligible list fails, nobody can be picked @smoke @drive** — passed (drive.spec.js:4862)
  - Where: Tools → Drive → My Drive → Files tab, file ⋯ menu → Share · account: iPhone XRR
  - Setup: The test replaces the My Drive list with one fake pdf file and makes loading the people the file can be shared with fail with a server error.
  - Steps: Open Drive → My Drive and click the Files tab → Open the pdf’s ⋯ menu → Share → Check that the drawer lists no people → Check that it shows its empty state → Close the drawer
  - Expected: If the list of people cannot load, the Share drawer shows an empty state and nobody can be picked.

- **a failed folder list in Move to… is reported @smoke @drive** — bug · known bug MoveToDialog toasts "Failed to load folder list" only from a catch; a refused GET shows "No folders available",a refused GET /folders in Move to… shows no error (the dialog lists no folders) — confirmed run 2,MoveToDialog toasts "Failed to load folder list" only from a catch; a refused GET shows "No folders available",a refused GET /folders in Move to… shows no error (the dialog lists no folders) — confirmed run 2 (drive.spec.js:4878)
  - Where: Tools → Drive → My Drive → Files tab, file ⋯ menu → Move to... · account: iPhone XRR
  - Setup: The test replaces the My Drive list with one fake pdf file, then makes the folder list for Move to... fail with a server error.
  - Steps: Open Drive → My Drive and click the Files tab → Open the pdf’s ⋯ menu → Move to... → Check that the Move to... dialog opens → Check that an error message appears within 15 seconds → Close the dialog
  - Expected: When Move to... cannot load the folders, the user gets an error message. Known bug: no error appears; the dialog just lists no folders.
  - Web error: Error: expect(locator).toBeVisible() failed

- **a failed favourites load heals when Starred is turned on (ZL-21810) @smoke @drive** — passed (drive.spec.js:4896)
  - Where: Tools → Drive → My Drive → Files tab · account: iPhone XRR
  - Setup: The test replaces the My Drive list with a fake png and a fake pdf and makes loading the starred items fail with a server error; once Drive has opened, it answers again with the png starred.
  - Steps: Open Drive → My Drive and click the Files tab → Click the star button (Show favorites) → Check that Drive asks for the starred items again → Check that the png is listed → Check that the pdf is not listed
  - Expected: If the starred items failed to load, turning on the Starred filter loads them again and shows only the starred file.

- **when tags fail to load there is no tag filter and nothing breaks @smoke @drive** — passed (drive.spec.js:4918)
  - Where: Tools → Drive → My Drive · account: iPhone XRR
  - Setup: The test replaces the My Drive list with one fake png file and makes loading the tags fail with a server error.
  - Steps: Open Drive → Click My Drive → Check that there is no Tag filter in the toolbar → Check that the My Drive and Shared with me tabs still show
  - Expected: When tags fail to load, the Tag filter is simply absent and the rest of Drive works.

- **trash: a listed item, a refused Restore ("Restore failed"), a refused Delete forever ("Delete failed") @smoke @drive** — passed (drive.spec.js:4929)
  - Where: Tools → Drive → My Drive → View Trash · account: iPhone XRR
  - Setup: The test fakes a trash holding one png file, makes the server refuse restoring or permanently deleting it, and blocks any other trash action.
  - Steps: Open Drive → My Drive and click View Trash → Check that the heading says “Trash (1)” → Click Restore on the png’s row → Check that an error says “Restore failed” → Click Delete on the same row and confirm → Check that an error says “Delete failed” → Click Back to Drive
  - Expected: The trash lists the item; a refused Restore shows “Restore failed” and a refused Delete forever shows “Delete failed”.

- **a refused "complete" is retried three times, then the upload fails @smoke @drive** — passed (drive.spec.js:4954)
  - Where: Tools → Drive → My Drive → Upload · account: iPhone XRR
  - Setup: The test fakes the whole upload: the file part is accepted, but the server refuses the final “complete” step every time.
  - Steps: Open Drive → My Drive and click Upload → Click Choose files and pick a png “e2e-stub-complete.png” → Click Upload → Watch the file’s row in the Operations panel → Check that it ends as Failed → Check that the final step was tried at least three times
  - Expected: When the server refuses to finish an upload, the app retries the last step three times and then marks the upload as Failed.

- **a chunk answered 503 twice is retried and the upload completes @smoke @drive** — passed (drive.spec.js:5005)
  - Where: Tools → Drive → My Drive → Upload · account: iPhone XRR
  - Setup: The test fakes the whole upload: the file part is answered “service unavailable” (503) twice, then accepted, and the final step succeeds.
  - Steps: Open Drive → My Drive and click Upload → Click Choose files and pick a png “e2e-stub-retried.png” → Click Upload → Watch the file’s row in the Operations panel → Check that it ends as Completed → Check that the file part was sent at least three times
  - Expected: A part refused twice with “service unavailable” is retried and the upload still completes.

### Drive — edge: public pages (stubbed)

- **the share page shows "Loading…" while slow, and "Cannot open link" on a network failure @smoke @drive** — passed (drive.spec.js:5082)
  - Where: A public share link, opened in a signed-out browser · account: none (signed-out visitor)
  - Setup: The test makes the share link’s details arrive 4 seconds late and then fail on the network.
  - Steps: Open a share link in a signed-out browser window → Check that the page shows “Loading…” while it waits → Wait for the request to fail → Check that the page heading says “Cannot open link”
  - Expected: A slow share page shows “Loading…”, and a network failure ends on “Cannot open link”.

- **an Office file falls back to the rendered PDF, then to the plain block @smoke @drive** — passed (drive.spec.js:5105)
  - Where: A public share link to a Word document, opened in a signed-out browser · account: none (signed-out visitor)
  - Setup: The test fakes two share links to “e2e-stub.docx” where the document editor cannot open: on the first a rendered PDF copy is available, on the second it is not.
  - Steps: Open the first link in a signed-out browser window → Check that the page title is “e2e-stub.docx” → Check that the document shows as a PDF → Open the second link in a fresh signed-out window → Check that the page title is “e2e-stub.docx” → Check that a plain block says “Download is disabled by the sender.”
  - Expected: When the editor cannot show an Office file on a public link, the page falls back to a rendered PDF, and when that is missing too, to a plain block saying download is disabled by the sender.

- **view_download gets Download links and a player without "nodownload"; view does not @smoke @drive** — passed (drive.spec.js:5141)
  - Where: A public share link to a video, opened in a signed-out browser · account: none (signed-out visitor)
  - Setup: The test fakes two share links to a video “e2e-stub.mp4”: one with “View only (no download)” access and one with “View + download”.
  - Steps: Open the view-only link in a signed-out browser window → Check that the video player hides its own download option → Check that there is no Download link on the page → Open the view-and-download link in a fresh signed-out window → Check that the video player keeps its download option → Check that the Download link points at the file’s download
  - Expected: A view-only video link offers no way to download (player or page); a “View + download” link offers a Download link and the player’s own download.

- **a PDF frame hides the toolbar; the watermark renders; a link that never expires shows no expiry @smoke @drive** — passed (drive.spec.js:5172)
  - Where: A public share link to a PDF, opened in a signed-out browser · account: none (signed-out visitor)
  - Setup: The test fakes a share link to “e2e-stub.pdf” with the watermark “e2e viewer • Shared by the Drive suite” and no expiry date.
  - Steps: Open the link in a signed-out browser window → Check that the PDF shows without its toolbar and side panel → Check that the watermark text “e2e viewer” is drawn over it → Check that the header shows the file name → Check that the header shows no expiry line
  - Expected: A shared PDF is shown without the viewer toolbar, carries its watermark, and a link that never expires shows no expiry at all.

- **a public upload refused per file shows the reason; Retry sends it again @smoke @drive** — passed (drive.spec.js:5199)
  - Where: A public file-request link, opened in a signed-out browser · account: none (signed-out visitor)
  - Setup: The test fakes a file request “[e2e] stub retry” that needs no email; its first upload is refused for going over the total size limit, the second is accepted.
  - Steps: Open the request link in a signed-out browser window and click “Reject all” on the cookie banner if it shows → Click Start upload → Click the drop area and pick a png “e2e-stub-retry.png” → Check that the file’s line says “This file would exceed the total size limit.” → Click Retry on that file → Check that the file is marked Uploaded and was sent twice
  - Expected: A file refused by the request page shows the reason next to it, and Retry sends it again until it is uploaded.

### Drive — edge: permission matrix (stubbed)

- **a file’s menu follows its permission: view, download, edit, full (pdf and docx) @smoke @drive** — passed (drive.spec.js:5269)
  - Where: Tools → Drive → My Drive → Files tab · account: iPhone XRR
  - Setup: The test replaces the My Drive list with eight fake files: a pdf and a Word document at each of four rights levels (view, download, edit, full).
  - Steps: Open Drive → My Drive and click the Files tab → Open each file’s ⋯ menu in turn and read its items → Check view: Preview, Add to favorites, Info only → Check download: adds Download (and for the Word file View in Editor and Version history) → Check edit: adds Share, Copy link, Move to... and Edit Info (the Word file has Open in Editor instead of View in Editor) → Check full: adds Delete at the end
  - Expected: Each file’s menu offers exactly the actions its rights allow, in the expected order, for both pdf and Word files.

- **under Shared with me: no Move for anyone (ZL-21869); Share only with the full set; Request files with edit @smoke @drive** — passed (drive.spec.js:5295)
  - Where: Tools → Drive → Shared with me → Folders tab, then Files tab · account: iPhone XRR
  - Setup: The test replaces the Shared with me list with four fake folders (view, download, edit and full rights) and one fake pdf with edit rights.
  - Steps: Open Drive → Shared with me and click the Folders tab → Open each folder’s ⋯ menu and read its items → Check view: Open folder, favourites, Info; download adds Download as ZIP → Check edit adds Request files and Edit Info; full adds Share and Delete → Check that no folder offers Move to... → Click the Files tab and open the pdf’s ⋯ menu → Check that it has Share but no Move to...
  - Expected: Under Shared with me nothing can be moved; Share needs the full set of rights on a folder; Request files and Edit Info need edit rights.

- **inside a shared folder the add actions follow the folder’s can_edit (ZL-21871) @smoke @drive** — passed (drive.spec.js:5323)
  - Where: Tools → Drive → Shared with me → Folders tab · account: iPhone XRR
  - Setup: The test replaces the Shared with me list with two empty fake folders: one with edit rights and one with view rights.
  - Steps: Open Drive → Shared with me and click the Folders tab → Check that Upload is not shown at the Shared with me root → Open the folder with edit rights → Check that Upload, New, Create Folder and Request Files are shown → Click the back arrow to return to Shared with me → Open the folder with view rights → Check that Upload, New, Create Folder and Request Files are not shown
  - Expected: Inside a shared folder the add actions appear only when the folder grants edit rights.

- **hover delete and the bulk bar follow each item’s rights @smoke @drive** — passed (drive.spec.js:5352)
  - Where: Tools → Drive → My Drive → Files tab · account: iPhone XRR
  - Setup: The test replaces the My Drive list with four fake png files at the view, download, edit and full rights levels.
  - Steps: Open Drive → My Drive and click the Files tab → Hover each file and check only the full-rights one shows a delete (bin) button → Tick the view file: the bar says “1 item selected” with no Delete, Move to... or Download → Also tick the download file: the bar says “2 items selected” and shows Download but no Move to... → Also tick the edit file: Move to... shows, Delete still does not → Also tick the full file: Delete shows → Click Clear and check the selection bar goes
  - Expected: The hover bin and each selection-bar button appear only when the selected items’ rights allow them, and the count reads “1 item” / “2 items”.

- **Info: Manage, Copy link and the tag picker only with edit; folders need the full set for Manage @smoke @drive** — passed (drive.spec.js:5383)
  - Where: Tools → Drive → My Drive, item ⋯ menu → Info · account: iPhone XRR
  - Setup: The test replaces the My Drive list with a fake view-only pdf, a fake pdf with edit rights and a fake folder with edit rights (not the full set).
  - Steps: Open Drive → My Drive and click the Files tab → Open Info on the view-only pdf: check there is no Manage, no Copy link and no “Add a tag...” picker, then close it → Open Info on the edit pdf: check Manage, Copy link and the tag picker are shown, then close it → Click the Folders tab → Open Info on the edit folder and check there is no Manage button → Close the Info panel
  - Expected: In Info, Manage, Copy link and the tag picker need edit rights on a file; a folder needs the full set of rights for Manage.

- **a per-file "no download" hides the preview Download and "Open in new tab" (ZL-21915) @smoke @drive** — passed (drive.spec.js:5418)
  - Where: Tools → Drive → My Drive → Files tab · account: iPhone XRR
  - Setup: The test replaces the My Drive list with a fake view-only png and a fake view-only zip, and serves their previews.
  - Steps: Open Drive → My Drive and click the Files tab → Hover the png and click its eye (preview) button → Check that the image shows and there is no Download button; close the preview → Hover the zip and click its eye (preview) button → Check that it says you can view this file but not download it → Check that there is no “Open in new tab” button; close the preview
  - Expected: Without the download right on a file, the preview offers no Download, and a file the browser cannot show says it can be viewed but not downloaded, without “Open in new tab”.

- **a stubbed rights-free Markdown file has no Edit switch; Save stays disabled until a change @smoke @drive** — passed (drive.spec.js:5446)
  - Where: Tools → Drive → My Drive → Files tab, Markdown files · account: iPhone XRR
  - Setup: The test replaces the My Drive list with two fake Markdown files reading “# stub”: “e2e-stub-readonly.md” (view only) and “e2e-stub-notes.md” (editable).
  - Steps: Open Drive → My Drive and click the Files tab → Preview the read-only file: check the heading “stub” shows and there is no View/Edit switch, then click Back to Drive → Preview the editable file and switch to Edit → Check that Save is disabled and Version history is enabled → Change the text to “# changed” → Check that Save is enabled and Version history is disabled with the hint “Save or discard your changes first” → Change the text back to “# stub” and check Save is disabled again → Click Back to Drive
  - Expected: A read-only Markdown file has no Edit switch; in an editable one Save stays disabled until the text changes, and Version history is blocked while there are unsaved changes.

- **empty comments and blank tag names cannot be sent; the link button reads by recipients @smoke @drive** — passed (drive.spec.js:5491)
  - Where: Tools → Drive → My Drive → Files tab, file ⋯ menu → Info and → Share · account: iPhone XRR
  - Setup: The test replaces the My Drive list with one fake pdf with full rights.
  - Steps: Open the pdf’s ⋯ menu → Info → Check the comment send button is disabled with the box empty and with only spaces typed → Click the + (new tag) button and check Create is disabled, also with only spaces in Tag name → Press Escape and close the Info panel → Open the pdf’s ⋯ menu → Share → “Share via link” tab and check the button reads “Generate link” → Type “someone@example.com” as a recipient and check the button reads “Send link by email” → Clear the recipient, check it reads “Generate link” again, and close the drawer
  - Expected: Blank comments and blank tag names cannot be sent, and the link button says “Generate link” without recipients and “Send link by email” with one. If Escape in the tag popup also closes the Info panel, that is only noted.

### Drive — edge: layouts, themes and widths

- **every drawer, modal and menu follows the dark theme @smoke @drive** — passed (drive.spec.js:5590)
  - Where: Tools → Drive → My Drive, with the app in the dark theme · account: iPhone XRR
  - Setup: Sets the app theme to dark; the test replaces the My Drive list with one fake folder and one fake png file (full rights) and serves the png’s preview.
  - Steps: Open Drive and check Drive itself is in the dark theme → Open the folder’s ⋯ menu, then close it → Open and close in turn: Upload, Create Folder, Request Files, the folder’s Share (Manage Access), Edit Info, Info and the Activity Log → Open the folder’s Move to... dialog and close it → On the Files tab, open and close the png’s Share (Share File) and its preview → Open New → Document to show the name dialog, then close it → Check that every one of these menus, drawers and dialogs was drawn in the dark style
  - Expected: With the dark theme on, every Drive menu, drawer and dialog opened is drawn dark.

- **every drawer, modal and menu follows the light theme @smoke @drive** — passed (drive.spec.js:5590)
  - Where: Tools → Drive → My Drive, with the app in the light theme · account: iPhone XRR
  - Setup: Sets the app theme to light; the test replaces the My Drive list with one fake folder and one fake png file (full rights) and serves the png’s preview.
  - Steps: Open Drive and check Drive itself is in the light theme → Open the folder’s ⋯ menu, then close it → Open and close in turn: Upload, Create Folder, Request Files, the folder’s Share (Manage Access), Edit Info, Info and the Activity Log → Open the folder’s Move to... dialog and close it → On the Files tab, open and close the png’s Share (Share File) and its preview → Open New → Document to show the name dialog, then close it → Check that none of these menus, drawers and dialogs was drawn in the dark style
  - Expected: With the light theme on, no Drive menu, drawer or dialog is drawn dark.

- **Classic layout: Drive is a full page, drawers sit outside any window, Back goes to /film-tools @smoke @drive** — passed (drive.spec.js:5609)
  - Where: Tools → Drive, in the Classic (full-page) layout · account: iPhone XRR
  - Setup: Switches the app to the Classic (full-page) layout.
  - Steps: Open Drive → Check that Drive fills the page and is not inside a tool window → Click My Drive → Click Upload and check the Upload Files drawer opens outside any window → Close the drawer → Click “Back to Film Tools” → Check that the Film Tools page opens
  - Expected: In the Classic layout Drive fills the whole page, its drawers are not inside a window, and “Back to Film Tools” returns to the Film Tools screen.

- **at phone width (390px) the page has no sideways scroll and drawers fit @smoke @drive** — passed (drive.spec.js:5631)
  - Where: Tools → Drive → My Drive, in a browser window 390 × 844 pixels (phone size) · account: iPhone XRR
  - Setup: Sizes the browser to 390 × 844; the test replaces the My Drive list with one fake pdf with full rights.
  - Steps: Open Drive → My Drive → Check that the section tabs and Upload are visible → Check that the page cannot be scrolled sideways → Click the Files tab, open the pdf’s ⋯ menu and check it fits inside the screen → Open the pdf’s Share drawer and check it fits the screen width, then close it → Click Upload and check the Upload Files drawer fits the screen width, then close it
  - Expected: At phone width Drive has no sideways scroll, and the row menu, Share drawer and Upload drawer all fit on screen.

- **at tablet width (768px) the page has no sideways scroll and drawers fit @smoke @drive** — passed (drive.spec.js:5631)
  - Where: Tools → Drive → My Drive, in a browser window 768 × 1024 pixels (tablet size) · account: iPhone XRR
  - Setup: Sizes the browser to 768 × 1024; the test replaces the My Drive list with one fake pdf with full rights.
  - Steps: Open Drive → My Drive → Check that the section tabs and Upload are visible → Check that the page cannot be scrolled sideways → Click the Files tab, open the pdf’s ⋯ menu and check it fits inside the screen → Open the pdf’s Share drawer and check it fits the screen width, then close it → Click Upload and check the Upload Files drawer fits the screen width, then close it
  - Expected: At tablet width Drive has no sideways scroll, and the row menu, Share drawer and Upload drawer all fit on screen.

- **grid cards: folder and file cards, hover actions, ⋯ menu, checkbox selection, double-click opens @smoke @drive** — passed (drive.spec.js:5664)
  - Where: Tools → Drive → My Drive, Grid view · account: iPhone XRR
  - Setup: The test replaces the My Drive list with one fake folder (empty) and one fake png file, both with full rights.
  - Steps: Open Drive → My Drive and click Grid view → Check the folder shows as a folder card; hover it, click its ⋯ and check the menu has Open folder, then close it → Tick the folder card’s checkbox and check the bar says “1 item selected”, then click Clear → Click the Files tab and check the png shows as a file card → Hover the file card and check it shows the eye (preview) and delete buttons → Click the Folders tab and double-click the folder card → Check that the folder opens and its name shows in the path bar
  - Expected: In Grid view, folder and file cards show their hover actions, ⋯ menu and checkbox selection, and double-clicking a folder card opens it.

### Drive — edge: names

- **a 100-character folder name is accepted; 101 is refused in the form @drive** — passed (drive.spec.js:5738)
  - Where: Tools → Drive → My Drive, inside a test folder → Create Folder · account: iPhone XRR
  - Setup: Opens a private test folder in My Drive.
  - Steps: Click Create Folder → Type a folder name 101 characters long → Click Create Folder in the drawer → Check that the form shows an error saying the name must be under 100 characters, and nothing is created → Shorten the name to exactly 100 characters → Click Create Folder again → Check that the folder is created and listed
  - Expected: A 101-character folder name is refused in the form without contacting the server; exactly 100 characters is accepted.

- **a unicode, emoji and RTL name is kept in the list, the crumbs and search @drive** — passed (drive.spec.js:5761)
  - Where: Tools → Drive → My Drive, inside a test folder · account: iPhone XRR
  - Setup: Opens a private test folder in My Drive.
  - Steps: Click Create Folder and name it “e2e-drive-…-en02 ünïcødé 🎬 مرحبا 日本” → Click Create Folder and check the folder is listed with that exact name → Open the folder → Check that the last crumb in the path bar shows the full name → Go back to the test folder → Search for “مرحبا” and check the folder is found → Clear the search
  - Expected: Accented letters, emoji, Arabic and Japanese in a folder name are kept in the list, the path bar and search.

- **HTML in a name is shown as text in the list, Info and crumbs, never run @drive** — passed (drive.spec.js:5778)
  - Where: Tools → Drive → My Drive, inside a test folder · account: iPhone XRR
  - Setup: Opens a private test folder in My Drive.
  - Steps: Click Create Folder and type a name ending in an HTML image tag and “<b>bold</b>” → Click Create Folder → Check that the list shows the name as plain text, with no bold text or image → Open the folder’s ⋯ menu → Info and check its title shows “<b>bold</b>” literally; close Info → Open the folder and check the path bar shows the name as plain text → Check that no script from the name ran → Go back to the test folder
  - Expected: HTML typed into a name is shown as text everywhere (list, Info, path bar) and never runs. If the server refuses such a name, it must say so in words; that outcome is noted.

- **path-like names ("../", "/", "\\", ".", "..") stay plain names in the open folder, or are refused with words @drive** — passed (drive.spec.js:5806)
  - Where: Tools → Drive → My Drive, inside a test folder → Create Folder · account: iPhone XRR
  - Setup: Opens a private test folder in My Drive.
  - Steps: Create a folder named “../e2e-escape” → Create a folder whose name contains a slash (“…-en04/slash”) → Create a folder whose name contains a backslash (“…-en04\back”) → Create a folder named “.” and one named “..” → For each accepted name, check it is listed as a plain folder in the open test folder and the path bar still shows the test folder → For each refused name, check the error message is in words, not a raw code, and close the drawer
  - Expected: Path-like names never escape the open folder: each is either kept as a plain folder name there or refused with a readable message. Which names were accepted is noted.

- **file rename with dots: inner dots, a leading dot, an upper-case extension, a trailing dot @drive** — failed (drive.spec.js:5831)
  - Where: Tools → Drive → My Drive, inside a test folder, file ⋯ menu → Edit Info · account: iPhone XRR
  - Setup: Uploads a pdf “e2e-drive-…-en05.pdf” into a private test folder in My Drive.
  - Steps: Open the pdf’s ⋯ menu → Edit Info and check the name field shows the name without “.pdf” → Rename it to “…-en05.a.b.c”, click Save Changes, and check it is saved as “…-en05.a.b.c.pdf” → Rename it to “.e2e-leading-dot” and check it is saved as “.e2e-leading-dot.pdf” → Rename it to “…-en05-UPPER.PDF” and check it is kept exactly as typed → Rename it to “…-en05-trailing.” and check it is saved as “…-en05-trailing..PDF” (the file’s current extension, upper case since the step before) → After each accepted rename, check the list shows the new name
  - Expected: Renaming a file adds the file’s current extension unless the typed name already ends in it (any case): inner dots, a leading dot and a trailing dot all get it added, and an upper-case “.PDF” typed by hand is kept and becomes the current extension. A refused rename is noted.
  - Web error: Error: expect(locator).toBeVisible() failed

- **a case-only duplicate, and the same name in two folders @drive** — notrun (drive.spec.js:5859)
  - Where: Tools → Drive → My Drive, inside a test folder · account: iPhone XRR
  - Setup: Opens a private test folder in My Drive.
  - Steps: Create a folder “e2e-drive-…-en08-Case” → Create another folder with the same name in lower case → Check that it is either listed, or refused with a readable error message → Create folders “…-en09-parent” and “…-en09-same” in the test folder → Open “…-en09-parent” and create a folder “…-en09-same” inside it → Check that it is created, then go back to the test folder
  - Expected: The same folder name is allowed in two different folders. A name differing only in case is either accepted or refused in words; which one is noted.

- **renaming to the same name, and a name with outer spaces @drive** — notrun (drive.spec.js:5886)
  - Where: Tools → Drive → My Drive, inside a test folder, folder ⋯ menu → Edit Info · account: iPhone XRR
  - Setup: Creates a folder “e2e-drive-…-en10” in a private test folder in My Drive.
  - Steps: Open the folder’s ⋯ menu → Edit Info and check the name is prefilled → Keep the same name and click Save Changes (the outcome is noted) → Open Edit Info again and type the name with two spaces before and after it, plus “-padded” → Click Save Changes and check the name is sent exactly as typed, spaces included → Check that the list shows one folder named “…-en10-padded”, or a readable error if refused
  - Expected: A rename keeps outer spaces as typed when sent and the folder then shows under its new name (the stored form is noted); a same-name rename is noted, whatever it does.

- **a 230-character file name uploads; the list shows it (or the server’s shortened form) @drive** — notrun (drive.spec.js:5911)
  - Where: Tools → Drive → My Drive, inside a test folder → Upload · account: iPhone XRR
  - Setup: Opens a private test folder in My Drive.
  - Steps: Click Upload → Choose files → Pick a png whose name is 230 characters long → Click Upload and wait for it to finish → Check that the list shows exactly one file starting with that name
  - Expected: A file with a 230-character name uploads and is listed, under its full name or the server’s shortened form (the stored length is noted).

- **New file: an emptied name takes the default, a repeat gets a suffix, unicode is kept @drive** — notrun (drive.spec.js:5931)
  - Where: Tools → Drive → My Drive, inside a test folder → New → Text file · account: iPhone XRR
  - Setup: Opens a private test folder in My Drive.
  - Steps: Click New → Text file, clear File name and press Enter → Check the file is created as “Untitled”, then click Back to Drive → Click New → Text file, type only spaces as File name and press Enter → Check it is also sent as “Untitled” but stored under a different name from the first, then click Back to Drive → Click New → Text file, type “e2e-drive-…-en13 ü 🎬” and press Enter → Check the stored name starts with exactly that text, then click Back to Drive
  - Expected: An empty or blank new-file name falls back to “Untitled”, a repeat gets a different stored name, and accented letters and emoji are kept.

### Drive — edge: files and limits

- **the drawer refuses a 0-byte file, as the page drop does @drive** — bug · known bug the drawer checks only the extension, so a 0-byte file is sent; the page drop refuses it as "(empty file)",the Upload drawer sends a 0-byte file (POST /uploads); the page drop refuses it — confirmed run 2,the drawer checks only the extension, so a 0-byte file is sent; the page drop refuses it as "(empty file)",the Upload drawer sends a 0-byte file (POST /uploads); the page drop refuses it — confirmed run 2 (drive.spec.js:5961)
  - Where: Tools → Drive → My Drive, inside a test folder → Upload · account: iPhone XRR
  - Setup: Opens a private test folder in My Drive.
  - Steps: Click Upload → Choose files → Pick an empty (0-byte) text file “e2e-drive-…-ef01-empty.txt” → Click Upload → Wait five seconds → Check that no upload was started
  - Expected: The Upload drawer refuses an empty file, as dropping it on the page does. Known bug: the drawer sends the empty file for upload anyway.
  - Web error: Error: expect(received).toHaveLength(expected)

- **a folder upload 7 levels deep makes every folder parent-first and lands the file at the bottom @drive** — passed (drive.spec.js:5978)
  - Where: Tools → Drive → My Drive, inside a test folder → Upload → Choose folder · account: iPhone XRR
  - Setup: Makes a folder tree on the computer, “e2e-drive-…-ef05-deep/level-1/…/level-7”, with one text file “deepest.txt” at the bottom; opens a private test folder in My Drive.
  - Steps: Click Upload → Choose folder and pick the tree’s top folder → Click Upload and wait for the file to finish → Check that the eight folders were created top-down, the first one inside the open test folder → Open the top folder, then level-1, level-2 … down to level-7 → Check that the path bar now shows 11 crumbs → Check that “deepest.txt” is listed → Click the test folder’s crumb to jump back to it
  - Expected: A folder upload 7 levels deep recreates every folder parent-first and puts the file in the deepest folder; the path bar crumbs lead back up.

- **a text over 2 MB is refused before saving @drive** — passed (drive.spec.js:6009)
  - Where: Tools → Drive → My Drive, inside a test folder → New → Markdown · account: iPhone XRR
  - Setup: Inside a private test folder in My Drive, creates a new Markdown file “e2e-drive-…-ef08”, which opens in the text editor.
  - Steps: Fill the editor with just over 2 MB of text → Click Save → Check that an error says the file is over 2 MB → Check that nothing was saved → Click Back to Drive, then Discard changes
  - Expected: Text over 2 MB is refused before saving, with a clear message.

- **a 190-character new-file name is created as typed @drive** — passed (drive.spec.js:6027)
  - Where: Tools → Drive → My Drive, inside a test folder → New → Markdown · account: iPhone XRR
  - Setup: Opens a private test folder in My Drive.
  - Steps: Click New → Markdown → Type a name exactly 190 characters long in File name and press Enter → Check that the file is created as that name plus “.md”, unchanged → Click Back to Drive
  - Expected: A new-file name of exactly 190 characters is kept as typed.

- **25 files at once; paging, server sort, Starred and search on them @drive** — passed (drive.spec.js:6037)
  - Where: Tools → Drive → My Drive, inside a fresh test folder · account: iPhone XRR
  - Setup: Opens an empty private test folder in My Drive.
  - Steps: Click Upload → Choose files, pick 25 png files “…ev07-00.png” to “…ev07-24.png”, check “25 files selected” and click Upload → Check the pager says “1–20 of 25 items” with 20 rows; go to page 2 and check it has 5 rows → Click the Name column header and check the list is sorted A→Z, back on page 1, starting with “…ev07-00.png” → Click the Name header again and check it starts with “…ev07-24.png” → Star “…ev07-24.png” and “…ev07-23.png” → Click the star button (Show favorites) and check only those 2 files show, with no pager; click it again → Search for “…ev07-1” and check exactly 10 files are found, all containing it; clear the search
  - Expected: 25 files upload at once; the list pages at 20, sorting by name resets to page 1, the Starred filter shows only the 2 starred files, and search narrows to the 10 matches.

- **uploading a name that already exists in the folder @drive** — passed (drive.spec.js:6106)
  - Where: Tools → Drive → My Drive, inside a test folder → Upload · account: iPhone XRR
  - Setup: Uploads a png “e2e-drive-…-ef11-dup.png” into a private test folder in My Drive.
  - Steps: Click Upload → Choose files → Pick a different png with the same name “e2e-drive-…-ef11-dup.png” → Click Upload and wait for it to finish → Note the name the second file was stored under → Check that the list shows two files starting with that name
  - Expected: Uploading a name that already exists keeps both files (two rows); the stored name of the second is noted.

### Drive — edge: navigation

- **a saved path to a folder deleted since falls back, with a message @drive** — failed (drive.spec.js:6129)
  - Where: Tools → Drive → My Drive, inside a test folder; then Drive in a second tab · account: iPhone XRR
  - Setup: Inside a private test folder in My Drive, creates a folder “e2e-drive-…-ev01-gone” and deletes it; the test then plants that folder as the remembered location of a new tab.
  - Steps: Open Drive in the new tab, which tries to reopen the deleted folder → Check that the path bar no longer names the deleted folder → Check that a message says the folder no longer exists (or that Drive moved to the parent or root) → Close the tab
  - Expected: Reopening Drive on a folder deleted since falls back to a folder that exists and tells the user why.
  - Web error: Test timeout of 90000ms exceeded.

- **the crumbs show an ancestor’s new name after it is renamed elsewhere @drive** — notrun (drive.spec.js:6173)
  - Where: Tools → Drive → My Drive, inside a test folder, in two tabs · account: iPhone XRR
  - Setup: Inside a private test folder in My Drive, creates a folder “e2e-drive-…-ev04-parent” with a folder “…-ev04-child” inside it, and opens the child.
  - Steps: Open Drive in a second tab at the test folder → There, rename the parent folder to “…-ev04-parent-renamed” (⋯ menu → Edit Info → Save Changes) and close the tab → In the first tab, open Drive again → Check that the path bar still shows the child folder → Check that the path bar shows the parent’s new name
  - Expected: After an ancestor folder is renamed elsewhere, reopening Drive shows its new name in the path bar. Known bug: the path bar keeps the old name, because the crumb names are remembered in the tab and never refreshed.

- **deleting the open folder (found by search) moves up to its parent @drive** — notrun (drive.spec.js:6201)
  - Where: Tools → Drive → My Drive, inside a test folder · account: iPhone XRR
  - Setup: Inside a private test folder in My Drive, creates a folder “e2e-drive-…-ev06-open” and opens it.
  - Steps: Search for the open folder’s name → Open the folder’s ⋯ menu in the results → Delete, and confirm → Clear the search → Check that the path bar shows the test folder (its parent) → Check that the path bar no longer names the deleted folder
  - Expected: Deleting the folder you are in (found through search) moves you up to its parent.

- **switching section inside a folder resets to the root without a "no longer exists" notice @drive** — notrun (drive.spec.js:6219)
  - Where: Tools → Drive → My Drive, inside a test folder · account: iPhone XRR
  - Setup: Opens a private test folder in My Drive.
  - Steps: Click Shared with me → Check that Drive is at the Shared with me root (no path bar) → Click My Drive → Check that Drive is at the My Drive root (no path bar) → Check that no “no longer exists” message appears
  - Expected: Switching section from inside a folder goes to that section’s root, without a misleading “no longer exists” message.

- **a reload in the middle of a folder comes back to the same folder @drive** — notrun (drive.spec.js:6231)
  - Where: Tools → Drive → My Drive, inside a test folder · account: iPhone XRR
  - Setup: Inside a private test folder in My Drive, creates a folder “e2e-drive-…-ev10-here” and opens it.
  - Steps: Reload the browser page → Open Drive again (in the Windowed view the reload lands on Home) → Check that Drive reopens inside the same folder → Check that the last crumb in the path bar shows the folder’s name
  - Expected: After a reload, Drive comes back to the folder that was open.

### Drive — edge: two users (stale state and concurrency)

- **B renames a file in A’s shared folder; A’s open list follows live @drive** — passed (drive.spec.js:6262)
  - Where: Tools → Drive → My Drive, inside the shared folder (iPhone XRR), and Tools → Drive → Shared with me (Lava Device) · account: iPhone XRR and Lava Device
  - Setup: As iPhone XRR, inside a private test folder in My Drive, creates a folder “e2e-drive-…-e201” holding a png “…-e201-file.png”, shares the folder with Lava Device as Editor, then opens the folder and leaves it open.
  - Steps: As Lava Device, open Drive → Shared with me, search for the folder and open it → Open the png’s ⋯ menu → Edit Info → Rename it to “…-e201-file-by-b” and click Save Changes → Check that the rename is accepted → Without reloading, check that iPhone XRR’s open folder lists “…-e201-file-by-b.png”
  - Expected: A rename made by the Editor inside the owner’s shared folder appears in the owner’s open list live.

- **B deletes a file while A has its Info open: A’s row goes; a late comment is refused @drive** — passed (drive.spec.js:6289)
  - Where: Tools → Drive → My Drive, inside the shared folder (iPhone XRR), and Tools → Drive → Shared with me (Lava Device) · account: iPhone XRR and Lava Device
  - Setup: As iPhone XRR, inside a private test folder in My Drive, creates a folder “e2e-drive-…-e202” holding a png, shares the folder with Lava Device as Owner, opens it and opens the png’s Info panel.
  - Steps: As Lava Device, open Drive → Shared with me, search for the folder and open it → Open the png’s ⋯ menu → Delete and confirm → Without reloading, check that the png disappears from iPhone XRR’s open folder → As iPhone XRR, type “a comment on a file deleted meanwhile” in the still-open Info panel and press Enter → Check that the server refuses the comment → Close the Info panel
  - Expected: When someone else deletes a file, it leaves the owner’s list live, and a comment sent afterwards from the stale Info panel is refused.

- **A moves a folder out of the shared parent B is in: B no longer finds it there @drive** — passed (drive.spec.js:6324)
  - Where: Tools → Drive → My Drive, inside a test folder (iPhone XRR), and Tools → Drive → Shared with me (Lava Device) · account: iPhone XRR and Lava Device
  - Setup: As iPhone XRR, inside a private test folder in My Drive, creates a folder “e2e-drive-…-e203-shared” with a folder “…-e203-inner” inside it, plus a folder “…-e203-private”; shares only “…-e203-shared” with Lava Device as Viewer.
  - Steps: As Lava Device, open Drive → Shared with me, search for “…-e203-shared”, open it, then open “…-e203-inner” → As iPhone XRR, open “…-e203-shared” and open the ⋯ menu of “…-e203-inner” → Move to... → Pick “…-e203-private” in the folder tree and click Move here → Check that the move goes to “…-e203-private” → As Lava Device, click the “…-e203-shared” crumb in the path bar → Check that “…-e203-inner” is no longer listed there
  - Expected: After the owner moves a folder out of the shared parent, the other person no longer finds it in that parent.

- **A and B rename the same folder at once; both end on one name @drive** — passed (drive.spec.js:6363)
  - Where: Tools → Drive → My Drive, inside a test folder (iPhone XRR), and Tools → Drive → Shared with me (Lava Device) · account: iPhone XRR and Lava Device
  - Setup: As iPhone XRR, inside a private test folder in My Drive, creates a folder “e2e-drive-…-e204” and shares it with Lava Device as Editor.
  - Steps: As Lava Device, open Drive → Shared with me and search for the folder → As iPhone XRR, open the folder’s ⋯ menu → Edit Info and type “…-e204-by-a” → As Lava Device, open the folder’s ⋯ menu → Edit Info and type “…-e204-by-b” → Click Save Changes in both drawers at the same moment (each outcome is noted) → As iPhone XRR, reopen the test folder and check exactly one folder starting with “…-e204” is listed → As Lava Device, search again and check the same name shows
  - Expected: Two people renaming the same folder at once end up seeing one and the same name.

- **A lowers B from Editor to Viewer while B uploads: B’s add actions go @drive** — failed (drive.spec.js:6399)
  - Where: Tools → Drive → My Drive, inside a test folder (iPhone XRR), and Tools → Drive → Shared with me (Lava Device) · account: iPhone XRR and Lava Device
  - Setup: As iPhone XRR, inside a private test folder in My Drive, creates a folder “e2e-drive-…-e205” and shares it with Lava Device as Editor; makes a 100 MB text file on the computer.
  - Steps: As Lava Device, open Drive → Shared with me, search for the folder and open it → Click Upload → Choose files, pick the 100 MB file and click Upload → While it uploads, as iPhone XRR lower Lava Device to Viewer on the folder (Share → Viewer → Save Access) → Check that the saved access gives Lava Device the Viewer role → As Lava Device, wait for the upload to end as Completed or Failed (noted) → Check that Lava Device’s Upload button disappears within a minute
  - Expected: Lowering an Editor to Viewer mid-upload takes their add actions away without a reload; how the running upload ends is noted.
  - Web error: Error: expect(received).toEqual(expected) // deep equality

- **A lowers B to View while B has the document open in the editor @drive** — notrun (drive.spec.js:6426)
  - Where: Tools → Drive → My Drive, inside a test folder (iPhone XRR), and Tools → Drive → Shared with me (Lava Device) · account: iPhone XRR and Lava Device
  - Setup: As iPhone XRR, uploads a Word document “e2e-drive-…-e206.docx” into a private test folder in My Drive and shares it with Lava Device at the Edit level.
  - Steps: As Lava Device, open Drive → Shared with me, search for the document, click its name and choose Open in Editor → Check that the editor opens for editing (no “View only” tag) → As iPhone XRR, lower Lava Device to View on the document (Share → View → Save Access) → Wait 20 seconds and note what Lava Device’s editor shows → As Lava Device, click Back to Drive if the editor is still open → Search for the document again and check its ⋯ menu now offers “View in Editor”
  - Expected: After being lowered to View while editing, the person’s menu offers only “View in Editor”; what the open editor did is noted.

- **a file level going down (Edit → Download → View) takes B’s actions away step by step @drive** — notrun (drive.spec.js:6464)
  - Where: Tools → Drive → My Drive, inside a test folder (iPhone XRR), and Tools → Drive → Shared with me (Lava Device) · account: iPhone XRR and Lava Device
  - Setup: As iPhone XRR, uploads a pdf “e2e-drive-…-e207.pdf” into a private test folder in My Drive and shares it with Lava Device at the Edit level.
  - Steps: As Lava Device, open Drive → Shared with me, search for the pdf and check its ⋯ menu has Download, Share, Copy link and Edit Info → As iPhone XRR, lower Lava Device to Download on the pdf → As Lava Device, search again and check the menu still has Download but no Share, Copy link or Edit Info → As iPhone XRR, lower Lava Device to View → As Lava Device, search again and check the menu is only Preview, favourites and Info
  - Expected: Each step down in file level removes its actions from the other person’s menu: Edit → Download drops Share, Copy link and Edit Info; Download → View drops Download.

- **a folder role going down (Owner → Editor → Viewer) takes Share, then the add actions away @drive** — notrun (drive.spec.js:6490)
  - Where: Tools → Drive → My Drive, inside a test folder (iPhone XRR), and Tools → Drive → Shared with me (Lava Device) · account: iPhone XRR and Lava Device
  - Setup: As iPhone XRR, inside a private test folder in My Drive, creates a folder “e2e-drive-…-e208” and shares it with Lava Device as Owner.
  - Steps: As Lava Device, open Drive → Shared with me, search for the folder and check its ⋯ menu has Share → As iPhone XRR, lower Lava Device to Editor → As Lava Device, search again and check the menu has Edit Info but no Share → As iPhone XRR, lower Lava Device to Viewer → As Lava Device, search again and check the menu is only Open folder, favourites and Info → Open the folder and check there is no Upload button
  - Expected: Lowering a folder role from Owner to Editor removes Share, and from Editor to Viewer removes the editing and add actions.

- **inherited Viewer and direct Editor: B edits only in the child; the floor is locked in Share @drive** — notrun (drive.spec.js:6517)
  - Where: Tools → Drive → My Drive, inside a test folder (iPhone XRR), and Tools → Drive → Shared with me (Lava Device) · account: iPhone XRR and Lava Device
  - Setup: As iPhone XRR, inside a private test folder in My Drive, creates a folder “e2e-drive-…-e209-parent” with a folder “…-e209-child” inside it, and shares the parent with Lava Device as Viewer.
  - Steps: As iPhone XRR, open the parent and share the child with Lava Device as Editor; check it is saved with the Editor role → Open the child’s Share (Manage Access) again and search Lava Device → Check that Lava Device is at Editor, the Viewer box is locked and the Editor box is not; close the drawer → As Lava Device, open Drive → Shared with me, search for the parent and open it → Check that there is no Upload button in the parent → Open the child and check that Upload is shown
  - Expected: With Viewer inherited from the parent and Editor given directly on the child, the person can add files only in the child, and the inherited Viewer level is locked in Share.

- **DRV-E2-10/E2-11 B’s star is B’s own; B’s search never finds A’s private folder @drive** — notrun (drive.spec.js:6554)
  - Where: Tools → Drive → My Drive, inside a test folder (iPhone XRR), and Tools → Drive → Shared with me / My Drive (Lava Device) · account: iPhone XRR and Lava Device
  - Setup: As iPhone XRR, inside a private test folder in My Drive, uploads a png “e2e-drive-…-e210-shared.png”, creates a private folder “…-e211-private”, and shares only the png with Lava Device at the View level.
  - Steps: As Lava Device, open Drive → Shared with me, search for the png, hover it and click its star → As iPhone XRR, reopen the test folder and check the png is not starred for iPhone XRR → As Lava Device, search for the private folder and check it is not found → Click My Drive and check the private folder is not found there either
  - Expected: A star is personal: Lava Device starring a shared file does not star it for the owner. Lava Device’s search never finds a folder that was not shared.

### Drive — B’s rights lowered in User Management

- **without Drive view, B cannot use Drive @drive** — passed (drive.spec.js:6647)
  - Where: Settings → Admin Settings → User Management (iPhone XRR); Tools → Drive (Lava Device) · account: iPhone XRR and Lava Device
  - Setup: As iPhone XRR, creates (once for this group) a folder “e2e-drive-…-ur-folder” in a private test folder in My Drive, holding a png, a pdf and a Word document, shared with Lava Device as Editor. Then in User Management turns Lava Device’s Admin switch off and, under Posting Rights → Tools → Drive, turns Post, View and Download off.
  - Steps: As Lava Device, open Tools → Drive → Check that the page says “You no longer have access to Z-Drive”, or that the app stays on the Film Tools page → Check that Drive’s My Drive / Shared with me tabs do not show
  - Expected: Without the Drive view right, Lava Device cannot use Drive at all.

- **view only: no add actions; edits, stars and downloads ask for permission; no preview Download @drive** — failed (drive.spec.js:6666)
  - Where: Settings → Admin Settings → User Management (iPhone XRR); Tools → Drive (Lava Device) · account: iPhone XRR and Lava Device
  - Setup: Uses the shared folder “e2e-drive-…-ur-folder” (png, pdf, Word document; Lava Device is Editor on it). In User Management, turns Lava Device’s Admin switch off and sets its Drive rights to View only (Post and Download off).
  - Steps: As Lava Device, open Drive → My Drive and check Upload, New, Create Folder and Request Files are not shown → Open the shared folder from Shared with me and check there is still no Upload button → Open the png’s ⋯ menu and look for Edit Info: check that Lava Device cannot do it: the item is either missing from the menu, or it opens a permission dialog (dismiss it); which one is noted → Hover the png and click its star: check the permission dialog opens, and dismiss it → Open the pdf’s ⋯ menu and look for Download: check that Lava Device cannot do it: the item is either missing from the menu, or it opens a permission dialog (dismiss it); which one is noted → Preview the png: check the image shows with no Download button, and close it → Tick the pdf and look for Download in the selection bar: if it shows, click it and check the permission dialog opens, then dismiss it → Check that nothing was edited, starred or downloaded
  - Expected: With View only, the add actions are hidden even inside a folder where Lava Device is Editor; Edit Info and Download are hidden from the menu (the server marks the items view-only) or ask for permission; starring asks for permission; the preview has no Download; nothing is edited, starred or downloaded. Whether View Trash shows is noted.
  - Web error: Test timeout of 270000ms exceeded.

- **posting on, download off: Upload and New show; Download still asks @drive** — notrun (drive.spec.js:6721)
  - Where: Settings → Admin Settings → User Management (iPhone XRR); Tools → Drive → Shared with me (Lava Device) · account: iPhone XRR and Lava Device
  - Setup: Uses the shared folder “e2e-drive-…-ur-folder” (png, pdf, Word document; Lava Device is Editor on it). In User Management, turns Lava Device’s Admin switch off and sets its Drive rights to View and Post on, Download off.
  - Steps: Open the fixture folder as Lava Device → Check Upload and New show → Open the pdf’s ⋯ menu and look for Download: check that Lava Device cannot do it: the item is either missing from the menu, or it opens a permission dialog (dismiss it); which one is noted → Check that nothing was downloaded
  - Expected: With the posting right but not the download right, Upload and New show, while Download is hidden or asks for permission, and nothing downloads.

- **download on, posting off: Download goes through; Edit Info asks @drive** — notrun (drive.spec.js:6741)
  - Where: Settings → Admin Settings → User Management (iPhone XRR); Tools → Drive → Shared with me (Lava Device) · account: iPhone XRR and Lava Device
  - Setup: Uses the shared folder “e2e-drive-…-ur-folder” (png, pdf, Word document; Lava Device is Editor on it). In User Management, turns Lava Device’s Admin switch off and sets its Drive rights to View and Download on, Post off.
  - Steps: Open the pdf’s ⋯ menu → Download → Check that the download goes ahead with no permission dialog → Open the png’s ⋯ menu and look for Edit Info: check that Lava Device cannot do it: the item is either missing from the menu, or it opens a permission dialog (dismiss it); which one is noted → Check the Edit drawer never opens
  - Expected: With the download right but not the posting right, Download goes through while Edit Info is hidden or asks for permission, and the Edit drawer never opens.

- **every right but not admin: no permission prompts @drive** — notrun (drive.spec.js:6762)
  - Where: Settings → Admin Settings → User Management (iPhone XRR); Tools → Drive → Shared with me (Lava Device) · account: iPhone XRR and Lava Device
  - Setup: Uses the shared folder “e2e-drive-…-ur-folder” (png, pdf, Word document; Lava Device is Editor on it). In User Management, turns Lava Device’s Admin switch off and turns its Drive Post, View and Download rights all on.
  - Steps: As Lava Device, open Drive → Shared with me, search for the folder and open it → Check that Upload is shown → Open the png’s ⋯ menu → Edit Info and check the Edit drawer opens; close it → Preview the png and check the preview has a Download button; close it → Check that no permission dialog appeared at any point
  - Expected: A non-admin with all three Drive rights is never asked for permission: editing and the preview Download are available.

- **an admin without the download right can still download a version (as everywhere else for admins) @drive** — notrun (drive.spec.js:6785)
  - Where: Settings → Admin Settings → User Management (iPhone XRR); Tools → Drive → Shared with me (Lava Device) · account: iPhone XRR and Lava Device
  - Setup: Uses the shared folder “e2e-drive-…-ur-folder” (png, pdf, Word document; Lava Device is Editor on it). In User Management, makes Lava Device an admin and sets its Drive rights to View and Post on, Download off.
  - Steps: As Lava Device, open Drive → Shared with me, search for the folder and open it → Open the Word document’s ⋯ menu → Info → In the version list, click a version’s “Download this version” button → Check that the version’s download link is handed out and the download starts
  - Expected: As everywhere else for admins, an admin can download a saved version even with the Drive download right off. Known bug: version download has no admin exception, so it is refused.

### Drive — editors: live editor host messages (stubbed)

- **the header follows zdrive:state: save status, connection and people chips @smoke @drive** — notrun (drive-editors.spec.js:618)
  - Where: Tools → Drive → My Drive, Files tab · account: iPhone XRR
  - Setup: The test makes the server list one Word file, “e2e-stub-editor.docx”, answers its editor requests itself, and serves a stand-in editor page that it controls (it can make that page report saving, errors, an expired session or an edit).
  - Steps: Open Drive, click My Drive and the Files tab, open “e2e-stub-editor.docx”’s ⋯ menu and click “Open in Editor” → Make the editor report that it is saving and connecting, with seven people (one with no name, one with an odd colour) → Check that the header shows “Saving…” and “Connecting…”, four people bubbles (“AR”, “BC”, …) and “+2” → Hover the first bubble and check it reads “Asha Rao (you)” → Make the editor report unsaved changes, connected, one person: check “Unsaved changes”, “Live” and one bubble → Make it report saved and disconnected: check “All changes saved” and “Offline”, and no bubbles → Make it report solo editing with nothing saved: check that no status shows → Click “Back to Drive” and check that no “Let others know” prompt appears
  - Expected: The editor header always matches what the editor reports: save state, connection state and who is in the file (at most four bubbles plus a count), and a blank name or an unsafe colour is ignored.

- **zdrive:failed shows the reason and Try again reloads the frame; a long reason is cut at 200 @smoke @drive** — notrun (drive-editors.spec.js:686)
  - Where: Tools → Drive → My Drive, Files tab · account: iPhone XRR
  - Setup: The test makes the server list one Word file, “e2e-stub-editor.docx”, answers its editor requests itself, and serves a stand-in editor page that it controls (it can make that page report saving, errors, an expired session or an edit).
  - Steps: Open “e2e-stub-editor.docx” in the editor → Make the editor report a failure with the reason “e2e: the file is locked” → Check for “This file couldn’t be opened.” with that reason, and that the editor frame is gone → Click “Try again” and check that the editor loads again → Make it fail again with a 250-character reason and check that only 200 characters are shown → Click “Back to Drive” in the failure pane and check that the editor closes with no prompt
  - Expected: An editor failure shows the reason it gave (trimmed, at most 200 characters) with “Try again”, which reloads the editor.

- **zdrive:session-expired offers Reload, which fetches a fresh editor config and reopens @smoke @drive** — notrun (drive-editors.spec.js:718)
  - Where: Tools → Drive → My Drive, Files tab · account: iPhone XRR
  - Setup: The test makes the server list one Word file, “e2e-stub-editor.docx”, answers its editor requests itself, and serves a stand-in editor page that it controls (it can make that page report saving, errors, an expired session or an edit).
  - Steps: Open “e2e-stub-editor.docx” in the editor → Make the editor report that the editing session expired → Check for “Your editing session expired.” and “Reload to keep working…”, with Reload and Back to Drive buttons → Click Reload → Check that Drive asks for the editor settings again, the pane goes away and the editor opens again with a new access key → Click “Back to Drive”
  - Expected: An expired editing session can be picked up again with Reload, which gets a fresh editor session without leaving the file.

- **messages from another origin, another window or of an unknown kind are ignored @smoke @drive** — notrun (drive-editors.spec.js:753)
  - Where: Tools → Drive → My Drive, Files tab · account: iPhone XRR
  - Setup: The test makes the server list one Word file, “e2e-stub-editor.docx”, answers its editor requests itself, and serves a stand-in editor page that it controls (it can make that page report saving, errors, an expired session or an edit).
  - Steps: Open “e2e-stub-editor.docx” in the editor → Send “session expired” and “failed” messages from the Drive page itself, and from a second hidden page on the editor’s own site → Send an unknown message, a plain text message and a list from the real editor → Wait a moment and check that the editor is still open with no failure pane → Send a real “failed” message from the editor and check that the failure pane now shows its reason → Click “Back to Drive”
  - Expected: Only the editor’s own frame can talk to Drive, and only with messages Drive knows; everything else is ignored.

- **zdrive:init carries the file to the editor origin, the frame URL carries no credentials, and a theme change posts zdrive:theme @smoke @drive** — notrun (drive-editors.spec.js:800)
  - Where: Tools → Drive → My Drive, Files tab · account: iPhone XRR
  - Setup: The test makes the server list one Word file, “e2e-stub-editor.docx”, answers its editor requests itself, and serves a stand-in editor page that it controls (it can make that page report saving, errors, an expired session or an edit). The editor address it gives contains an access key and a file address in the link.
  - Steps: Open “e2e-stub-editor.docx” in the editor → Check that the editor frame’s address no longer contains the access key or the file address → Check that the editor was handed the file name, the file address, the access key, the editor type, edit mode and the current theme → Switch Drive’s light/dark theme (the moon/sun button) and check that the editor is told the new theme → Switch it back and check that the editor is told again → Click “Back to Drive”
  - Expected: The access key travels only inside the hand-over message to the editor, never in its address, and the editor follows Drive’s theme while open.

- **after zdrive:edited, closing asks the host to flush and then shows the notify prompt; close-ready {edited} does too @smoke @drive** — notrun (drive-editors.spec.js:848)
  - Where: Tools → Drive → My Drive, Files tab · account: iPhone XRR
  - Setup: The test makes the server list one Word file, “e2e-stub-editor.docx”, answers its editor requests itself, and serves a stand-in editor page that it controls (it can make that page report saving, errors, an expired session or an edit).
  - Steps: Open “e2e-stub-editor.docx” in the editor and make the editor report an edit → Click “Back to Drive” and check that the editor closes at once (it confirms it saved) → Check for “Let others know you updated this file?” with the file name and “Everyone with access…” → Click Skip → Open the file again, make the editor say only on closing that the file was edited, and click “Back to Drive” → Check that the prompt appears again, click Skip, and check that nothing was sent
  - Expected: Closing a file someone changed asks once whether to let others know; Skip tells nobody.

- **a host that never answers close-request: Back waits, then closes after about 5 s, no prompt @smoke @drive** — notrun (drive-editors.spec.js:878)
  - Where: Tools → Drive → My Drive, Files tab · account: iPhone XRR
  - Setup: The test makes the server list one Word file, “e2e-stub-editor.docx”, answers its editor requests itself, and serves a stand-in editor page that it controls (it can make that page report saving, errors, an expired session or an edit).
  - Steps: Open “e2e-stub-editor.docx” in the editor and make the editor stop answering when asked to save on close → Click “Back to Drive” → Check that the button shows it is working and the editor is still open after 2 seconds → Check that the editor closes on its own after about 5 seconds → Check that no “Let others know” prompt appears
  - Expected: When the editor never confirms its last save, Drive waits about 5 seconds and then closes anyway.

- **a view-only preview closes at once without a close-request, and a viewer never gets the notify prompt @smoke @drive** — notrun (drive-editors.spec.js:898)
  - Where: Tools → Drive → My Drive, Files tab · account: iPhone XRR
  - Setup: The test makes the server list one Word file, “e2e-stub-editor.docx”, answers its editor requests itself, and serves a stand-in editor page that it controls (it can make that page report saving, errors, an expired session or an edit).
  - Steps: Hover “e2e-stub-editor.docx” and click its eye (Open) button → Check that the editor opens in view mode with the plain viewer and a “View only” tag → Make the editor stop answering on close, and report an edit and unsaved changes → Check that the header shows “Live” but no save status → Click “Back to Drive” and check that it closes within 3 seconds, without asking the editor to save → Check that no “Let others know” prompt appears
  - Expected: A preview is read-only: it closes immediately and never asks to notify anyone, whatever the editor reports.

### Drive — editors: notify prompt (stubbed)

- **Send posts the trimmed note, shows the server message and closes @smoke @drive** — notrun (drive-editors.spec.js:930)
  - Where: Tools → Drive → My Drive, Files tab · account: iPhone XRR
  - Setup: The test makes the server list one Word file, “e2e-stub-editor.docx”, answers its editor requests itself, and serves a stand-in editor page that it controls (it can make that page report saving, errors, an expired session or an edit). Sending the notification is answered by the test with the message “e2e_announce_sent”, so nobody is notified.
  - Steps: Open “e2e-stub-editor.docx” in the editor, make the editor report an edit, and click “Back to Drive” → In “Let others know you updated this file?”, type “   Updated the call times for Day 3   ” → Click Send → Check that the note sent is “Updated the call times for Day 3” (no outer spaces) → Check for the message “e2e_announce_sent” and that the prompt closes
  - Expected: Send notifies with the note trimmed, shows the server’s message and closes the prompt.

- **the note stops at 300 with a counter; a refusal shows the server message and keeps the prompt; a blank note sends "" @smoke @drive** — notrun (drive-editors.spec.js:951)
  - Where: Tools → Drive → My Drive, Files tab · account: iPhone XRR
  - Setup: The test makes the server list one Word file, “e2e-stub-editor.docx”, answers its editor requests itself, and serves a stand-in editor page that it controls (it can make that page report saving, errors, an expired session or an edit). The first notification is refused with “e2e_announce_refused”, the second is accepted; nobody is notified.
  - Steps: Open “e2e-stub-editor.docx” in the editor, make the editor report an edit, and click “Back to Drive” → Type 350 characters in the note and check that only 300 stay, with the counter at “300 / 300” → Click Send and check for the error “e2e_announce_refused”, with the prompt still open and the note kept → Replace the note with only spaces and click Send → Check that an empty note was sent and the prompt closes
  - Expected: The note stops at 300 characters; a refused notification shows the server’s reason and leaves the prompt open to try again or skip.

### Drive — editors: Markdown (stubbed)

- **Ctrl/Cmd+S saves only in Edit with changes; "Keep editing" keeps the document; closing after a save asks to notify @smoke @drive** — notrun (drive-editors.spec.js:985)
  - Where: Tools → Drive → My Drive, Files tab · account: iPhone XRR
  - Setup: The test makes the server list one Markdown file, “e2e-stub-editor-notes.md”, whose text is “Live notes”, and answers its saves, versions and notifications itself.
  - Steps: Open Drive, click My Drive and the Files tab, hover “e2e-stub-editor-notes.md” and click its eye (Open) button → Click Edit and press Ctrl+S (Cmd+S on a Mac) without changing anything: check nothing is saved → Replace the text with “# changed by e2e” and check for “Unsaved changes” → Click Preview and press Ctrl/Cmd+S: check nothing is saved → Click “Back to Drive”, check for “Close without saving?”, and click “Keep editing” → Check that the document stays open; click Edit and check the text is still there → Press Ctrl/Cmd+S and check that it saves that text (with the time the file was opened) and shows “All changes saved” → Click “Back to Drive”, check for the “Let others know” prompt with the file name, and click Skip
  - Expected: The keyboard save works only while editing with changes; “Keep editing” keeps the document; closing after a save asks once whether to notify.

- **"Save my version anyway" sends the save without content_time and clears the conflict @smoke @drive** — notrun (drive-editors.spec.js:1033)
  - Where: Tools → Drive → My Drive, Files tab · account: iPhone XRR
  - Setup: The test makes the server list one Markdown file, “e2e-stub-editor-notes.md”, whose text is “Live notes”, and answers its saves, versions and notifications itself. The first save is refused as “changed since you opened it”; a save without the opening time is accepted.
  - Steps: Open “e2e-stub-editor-notes.md” with its eye button and click Edit → Replace the text with “# my version wins” and click Save → Check for the conflict bar with “Save my version anyway” → Click “Save my version anyway” → Check that the save was sent without the opening time, the bar goes away and “All changes saved” shows → Click “Back to Drive” and Skip the “Let others know” prompt
  - Expected: “Save my version anyway” overwrites the newer version on purpose and clears the conflict.

- **restoring a version reloads the text, the history shows the restore, and closing asks to notify @smoke @drive** — notrun (drive-editors.spec.js:1072)
  - Where: Tools → Drive → My Drive, Files tab · account: iPhone XRR
  - Setup: The test makes the server list one Markdown file, “e2e-stub-editor-notes.md”, whose text is “Live notes”, and answers its saves, versions and notifications itself. Its version history has three editing sessions: the newest with two saves (version 4, current, and version 3), version 2 (saved on close, with “Made the heading bold”), and version 1 (uploaded). Version 2’s text is “Version two”.
  - Steps: Open “e2e-stub-editor-notes.md” with its eye button and click Version history → Click the version 2 row and check for “You are viewing version 2” → Click Restore in the version’s card → Check for “Restore this version?” mentioning version 2, and that its Restore button is on top and clickable → Click Restore → Check for the message “e2e_version_restored”, that the viewing bar goes away and the document now shows “Version two” → Click “All versions” and check that the newest row reads “Restored from version 2” and is the current version → Click “Back to Drive”, check for the “Let others know” prompt, and click Skip
  - Expected: Restoring a version reloads its text, the history shows the restore, and closing asks once whether to notify. Known bug: the “Restore this version?” box opens underneath the document, so its button cannot be clicked.

### Drive — editors: version history in the Markdown viewer (stubbed)

- **naming and un-naming the current version, the "Only named versions" filter, and a version’s ⋯ menu @smoke @drive** — notrun (drive-editors.spec.js:1129)
  - Where: Tools → Drive → My Drive, Files tab · account: iPhone XRR
  - Setup: The test makes the server list one Markdown file, “e2e-stub-editor-notes.md”, whose text is “Live notes”, and answers its saves, versions and notifications itself. Its version history has three editing sessions: the newest with two saves (version 4, current, and version 3), version 2 (saved on close, with “Made the heading bold”), and version 1 (uploaded).
  - Steps: Open “e2e-stub-editor-notes.md” with its eye button and click Version history → Click “Name current version”, type “  e2e final draft  ” and click Save → Check for “Version named”, the name on the newest row, “Rename current version” and “Only named versions (1)” → Turn on “Only named versions” and check that only the named version is listed; turn it off → Click “Rename current version”, clear the name and click Save → Check for “Name removed” and that the name and the filter are gone → Open the ⋯ menu of the oldest version and check it offers “Name this version” and “Restore this version”, shown above the document
  - Expected: Versions can be named and the name removed, the named-only filter lists just them, and an older version’s ⋯ menu offers naming and restore. Known bug: that ⋯ menu opens underneath the Markdown document.

- **sessions: "2 saves", "Show saves in this session", change lines and save types @smoke @drive** — notrun (drive-editors.spec.js:1193)
  - Where: Tools → Drive → My Drive, Files tab · account: iPhone XRR
  - Setup: The test makes the server list one Markdown file, “e2e-stub-editor-notes.md”, whose text is “Live notes”, and answers its saves, versions and notifications itself. Its version history has three editing sessions: the newest with two saves (version 4, current, and version 3), version 2 (saved on close, with “Made the heading bold”), and version 1 (uploaded).
  - Steps: Open “e2e-stub-editor-notes.md” with its eye button and click Version history → Check that the newest row is the current version and reads “2 saves” → Check that the next row reads “Saved on close · 2 paragraphs changed · Made the heading bold” and the oldest “Uploaded” → Click the arrow “Show saves in this session” on the newest row → Check that one more row appears, “Autosaved · 1 paragraph changed”, and that no version opened → Click the arrow again and check that the extra row goes away
  - Expected: The history groups saves into sessions, says how many saves a session has, and describes what each save changed.

- **a version’s changes, the person filter with two people, and viewing an older version @smoke @drive** — notrun (drive-editors.spec.js:1231)
  - Where: Tools → Drive → My Drive, Files tab · account: iPhone XRR
  - Setup: The test makes the server list one Markdown file, “e2e-stub-editor-notes.md”, whose text is “Live notes”, and answers its saves, versions and notifications itself. Its version history has three editing sessions: the newest with two saves (version 4, current, and version 3), version 2 (saved on close, with “Made the heading bold”), and version 1 (uploaded). Version 4 changed three paragraphs, two by Riya Kapoor and one by Sam Mehta; version 2 changed one, by Riya Kapoor.
  - Steps: Open “e2e-stub-editor-notes.md” with its eye button and click Version history → Click the newest row and check its card: “Current version”, “Saved · Version 4”, “3 paragraphs changed” → Check the three changes read Edited (6:30 struck out, 7:00 added), Added and Removed → Check the person buttons “All”, “Riya Kapoor”, “Sam Mehta”; click “Sam Mehta” → Check for “1 paragraph changed by Sam Mehta” and only the “Old note” change; click “All” and check all three are back → Click “All versions” and then the version 2 row → Check for “You are viewing version 2”, the text “Version two”, “Made the heading bold”, “1 paragraph changed”, a Restore button and no person buttons → Click “Back to the current version” and check the document shows “Live notes” again
  - Expected: Each version lists what changed; with two or more people the changes can be filtered by person; an older version opens read-only until “Back to the current version”.

- **a synthetic "current" version (fresh file) offers no naming (E03, stubbed) @smoke @drive** — notrun (drive-editors.spec.js:1294)
  - Where: Tools → Drive → My Drive, Files tab · account: iPhone XRR
  - Setup: The test makes the server list one Markdown file, “e2e-stub-editor-notes.md”, whose text is “Live notes”, and answers its saves, versions and notifications itself. Its version history has only the server’s placeholder for the file as it is now (a file with no saved version yet).
  - Steps: Open “e2e-stub-editor-notes.md” with its eye button and click Version history → Check that the one row has no ⋯ menu → Click it and check the card says “This is the file as it is now…” with no Restore or Name buttons; click “All versions” → Check that there is no “Name current version” button
  - Expected: A file with no saved version yet offers no naming anywhere. Known bug: the “Name current version” button still shows, and using it fails on the server (DRV-E03).

### Drive — editors: Info panel versions (stubbed)

- **Info: Restore → "Restore this version?" → "Restored to version N", and the list refreshes @smoke @drive** — notrun (drive-editors.spec.js:1411)
  - Where: Tools → Drive → My Drive, Files tab · account: iPhone XRR
  - Setup: The test makes the server list one Word file, “e2e-stub-editor.docx”, with three versions (3 is current, 2 and 1 older), and answers the restore itself.
  - Steps: Open Drive, click My Drive and the Files tab, and open “e2e-stub-editor.docx”’s ⋯ menu → Info → Under Versions, check that “Version 3” is Current and has no restore button, and “Version 2” has one → Click Version 2’s restore button and check for “Restore this version?” → Click Restore → Check for “Restored to version 4” and that the list now shows Version 4 as Current
  - Expected: Restoring from Info asks first, then confirms with the new version number and refreshes the list.

- **Info: a refused restore says so (today it is silent) @smoke @drive** — notrun (drive-editors.spec.js:1440)
  - Where: Tools → Drive → My Drive, Files tab · account: iPhone XRR
  - Setup: The test makes the server list one Word file, “e2e-stub-editor.docx”, with three versions, and refuses every restore with “e2e_restore_refused”.
  - Steps: Open “e2e-stub-editor.docx”’s ⋯ menu → Info → Click Version 2’s restore button and click Restore in “Restore this version?” → Check that Version 3 is still Current and no success message shows → Check for an error message with the server’s reason
  - Expected: A refused restore tells the person why. Known bug: nothing at all is shown.

### Drive — editors: notify for real (E2E_REAL_SEND)

- **(RT08) a Markdown edit, then Send, notifies for real; a second Send soon after is answered by the server @drive** — notrun (drive-editors.spec.js:1466)
  - Where: Tools → Drive → My Drive, inside a private test folder · account: iPhone XRR
  - Setup: Creates a Markdown file “e2e-drive-…-ed20.md” with New → Markdown in a private test folder. Runs only when real notifications are allowed (E2E_REAL_SEND).
  - Steps: Type a heading in the new file, click Save and click “Back to Drive” → In “Let others know you updated this file?”, type a note and click Send → Check that it is accepted and the prompt closes → Open the file’s ⋯ menu → Edit file, change the text, Save, and click “Back to Drive” → Type a note and click Send again within a minute → If the server refuses it, check for its error message, that the prompt stays open, and click Skip; if it accepts, note it
  - Expected: The first notification goes out; a second for the same file soon after is answered by the server (expected: refused with a reason, and the prompt stays open).

### Drive — forms: Info panel writes

- **a refused comment is reported, and the text is kept @smoke @drive** — notrun (drive-forms.spec.js:132)
  - Where: Tools → Drive → My Drive → Files tab, file ⋯ menu → Info → Comments · account: iPhone XRR
  - Setup: The test replaces the My Drive list with one fake png file and makes the server refuse any new comment.
  - Steps: Open Drive → My Drive and click the Files tab → Open the png’s ⋯ menu → Info → Type “e2e refused comment” in “Add a comment...” and press Enter → Check that the comment text is still in the box and no comment was added to the list → Check that an error message appears with the server’s reason
  - Expected: A refused comment is reported and the typed text is kept so it can be sent again. Known bug: nothing is shown at all.

- **a refused comment edit is reported, and the edit box stays @smoke @drive** — notrun (drive-forms.spec.js:155)
  - Where: Tools → Drive → My Drive → Files tab, file ⋯ menu → Info → Comments · account: iPhone XRR
  - Setup: The test replaces the My Drive list with one fake png file that already has one comment by iPhone XRR, and makes the server refuse any comment edit.
  - Steps: Open Drive → My Drive and click the Files tab → Open the png’s ⋯ menu → Info → On the comment “e2e original comment”, click the pencil (Edit) → Change the text to “e2e original comment (edited)” and click Save → Check that the edit box is still open with the new text → Check that an error message appears with the server’s reason
  - Expected: A refused comment edit is reported and the edit box stays open with the text. Known bug: nothing is shown; the edit box just stays open.

- **a tag name stops at 50; a refused tag (e.g. a duplicate name) is reported and the name kept @smoke @drive** — notrun (drive-forms.spec.js:200)
  - Where: Tools → Drive → My Drive → Files tab, file ⋯ menu → Info → Tags → + · account: iPhone XRR
  - Setup: The test replaces the My Drive list with one fake png file, gives the project no tags, and makes the server refuse any new tag.
  - Steps: Open Drive → My Drive and click the Files tab → Open the png’s ⋯ menu → Info → In Tags, click the + (Create new tag) button → Type 60 characters in “Tag name” and check that it stops at 50 → Replace the name with “e2e-drive” and click Create → Check that the New Tag box is still open and still holds “e2e-drive” → Check that an error message appears with the server’s reason
  - Expected: Tag names stop at 50 characters. A refused tag (for example a name that already exists) is reported and the name is kept. Known bug: nothing is shown.

- **a comment over 2000 characters: no limit while typing, and the server’s refusal is reported @drive** — notrun (drive-forms.spec.js:227)
  - Where: Tools → Drive → My Drive → the run’s test folder, file ⋯ menu → Info → Comments · account: iPhone XRR
  - Setup: Uploads a png file “e2e-drive-…-fm04.png” into the run’s area folder “e2e-drive-…-area-forms”.
  - Steps: Open the png’s ⋯ menu → Info → Paste 2001 characters into “Add a comment...” and check that all 2001 are kept (no limit while typing) → Press Enter → If the server refuses the comment: check that the text stays in the box and an error message appears → If the server takes it: check that the comment is listed (the test notes this)
  - Expected: A comment longer than the server allows (2000 characters) is either stopped while typing or refused with an error. Known bug: the box takes any length and a refusal is silent; the comment simply never appears.

### Drive — forms: trash and delete (stubbed)

- **Empty trash (faked): the confirm counts the items, "Trash emptied", the list empties and is fetched again a second later @smoke @drive** — notrun (drive-forms.spec.js:293)
  - Where: Tools → Drive → My Drive → View Trash · account: iPhone XRR
  - Setup: The test fakes a trash holding one png file and one folder, and fakes the “Empty trash” answer; nothing in the real trash is touched.
  - Steps: Open Drive → My Drive and click View Trash → Check that the trash says “Trash (2)” and lists both fake items → Click Empty trash → Check that the confirm says “Empty trash?” and “All 2 item(s) will be permanently deleted. This cannot be undone.” → Click Empty trash in the confirm → Check that “Trash emptied” appears, the list says “Trash is empty” and the heading “Trash (0)” → Check that the trash is loaded again about a second later
  - Expected: Emptying the trash counts the items in the confirm, says “Trash emptied”, clears the list and reloads it a second later to make sure.

- **a refused Empty trash is reported, and the items stay @smoke @drive** — notrun (drive-forms.spec.js:342)
  - Where: Tools → Drive → My Drive → View Trash · account: iPhone XRR
  - Setup: The test fakes a trash holding one png file and one folder and makes the server refuse “Empty trash”; nothing in the real trash is touched.
  - Steps: Open Drive → My Drive and click View Trash → Click Empty trash, then Empty trash in the confirm → Check that the trash still says “Trash (2)” and lists both items → Check that no “Trash emptied” message appears → Check that an error message appears with the server’s reason
  - Expected: A refused Empty trash is reported and the items stay listed. Known bug: nothing is shown at all.

- **the trash in grid view: a card per item, Restore and Delete forever (faked) @smoke @drive** — notrun (drive-forms.spec.js:367)
  - Where: Tools → Drive → My Drive → Grid view → View Trash · account: iPhone XRR
  - Setup: The test fakes a trash holding one png file and one folder, and fakes the answers to Restore and Delete forever.
  - Steps: Open Drive → My Drive and click Grid view → Click View Trash and check the trash shows two cards (no table): the png tagged “file”, the folder tagged “folder” → Hover the png card and click its Restore (circular arrow) button → Check that “Restored "e2e-stub-fm-image.png"” appears, the card goes and the heading says “Trash (1)” → Hover the folder card and click its bin (Delete forever) → Check that the confirm says “Permanently delete?” and names the folder, then click Delete forever → Check that “Permanently deleted "e2e-stub-fm-folder"” appears and the trash says “Trash is empty”
  - Expected: In grid view the trash shows one card per item, and Restore and Delete forever work from the cards with the same messages as the list view. Empty trash is never sent.

- **a refused single delete shows the server’s reason, not "Failed to delete file/folder" @smoke @drive** — notrun (drive-forms.spec.js:411)
  - Where: Tools → Drive → My Drive → Files tab and Folders tab, ⋯ menu → Delete · account: iPhone XRR
  - Setup: The test replaces the My Drive list with one fake png file and one fake folder, and makes the server refuse deleting either.
  - Steps: Open Drive → My Drive and click the Files tab → Open the png’s ⋯ menu → Delete and check the confirm says “This action will move the selected items to trash.” → Click Ok and note the error message; check the file is still listed → Click the Folders tab, open the folder’s ⋯ menu → Delete, click Ok and note the error message; check the folder is still listed → Check that both error messages give the server’s reason
  - Expected: A refused delete shows the server’s reason. Known bug: it always says “Failed to delete file” / “Failed to delete folder”, whatever the server said.

### Drive — forms: file requests (stubbed)

- **at the root, "A new folder": a top-level folder named after the request, then the request in it @smoke @drive** — notrun (drive-forms.spec.js:463)
  - Where: Tools → Drive → My Drive (top level) → Request Files · account: iPhone XRR
  - Setup: The test replaces the My Drive list with an empty one and fakes both the new folder and the request, so no folder is really made.
  - Steps: Open Drive → My Drive and click Request Files → Check that “Uploads go to” has “A new folder” selected → Type the title “[e2e] stub new-folder request” and click Create request → Check that a folder named after the title is made at the top of My Drive first, then the request is made in that folder → Check that “Request link created” appears, the “Request created” panel shows the link, and the folder picker is gone → Check that the drawer then lists the new folder’s requests
  - Expected: At the top of My Drive, “A new folder” makes a folder named after the request at the top level, then puts the request in it.

- **a refused "A new folder" is reported, and no request is made @smoke @drive** — notrun (drive-forms.spec.js:513)
  - Where: Tools → Drive → My Drive (top level) → Request Files · account: iPhone XRR
  - Setup: The test replaces the My Drive list with an empty one and makes the server refuse the new folder.
  - Steps: Open Drive → My Drive and click Request Files → Leave “A new folder” selected, type the title “[e2e] stub refused folder” and click Create request → Check that the button stops loading, the title is still filled in and no “Request created” panel shows → Check that no request was sent → Check that an error message appears with the server’s reason
  - Expected: When the new folder is refused, no request is made and the user is told why. Known bug: nothing is shown; the drawer just stops.

- **from a folder’s menu: no destination picker, that folder’s requests listed; recipients lower-cased and de-duplicated; a partial email result warns @smoke @drive** — notrun (drive-forms.spec.js:544)
  - Where: Tools → Drive → My Drive → Folders tab, folder ⋯ menu → Request files · account: iPhone XRR
  - Setup: The test replaces the My Drive list with one fake folder that already has one request (2 files received, expiring in 3 days), and fakes the new request’s answer with one email sent and one failed.
  - Steps: Open Drive → My Drive (Folders tab) → Open the folder’s ⋯ menu → Request files → Check that there is no “Uploads go to” picker and the drawer says uploads land in that folder → Check that Active requests shows 1: “[e2e] stub existing request”, “2 files received”, “Expires in 3 days” → Type a title, and in “Email the link to” type “E2E-One@Example.com,e2e-one@example.com e2e-two@example.com;” → Click Create request → Check that the request goes to that folder with exactly two recipients, e2e-one@example.com and e2e-two@example.com → Check that the warning “Request created. Emailed 1/2 — share the link manually with the rest.” appears → Check that Email invites lists e2e-one@example.com as sent and e2e-two@example.com with its reason
  - Expected: From a folder’s menu the request always goes into that folder, its existing requests are listed, recipients are lower-cased and listed once each, and a partly failed email send is reported.

- **a refused request revoke is reported, not "Request revoked" @smoke @drive** — notrun (drive-forms.spec.js:595)
  - Where: Tools → Drive → My Drive → Folders tab, folder ⋯ menu → Request files · account: iPhone XRR
  - Setup: The test replaces the My Drive list with one fake folder holding one request, and makes the server refuse revoking it.
  - Steps: Open Drive → My Drive (Folders tab) → Open the folder’s ⋯ menu → Request files → On “[e2e] stub existing request”, click the bin (Revoke) and confirm Revoke → Check that an error message appears with the server’s reason → Check that no “Request revoked” message appears
  - Expected: A refused revoke is reported and does not claim success. Known bug: it says “Request revoked” although the link still works.

### Drive — forms: share via link (stubbed)

- **"Can edit" is offered for xlsx and docx, not for pdf, doc or csv @smoke @drive** — notrun (drive-forms.spec.js:664)
  - Where: Tools → Drive → My Drive → Files tab, file ⋯ menu → Share → Share via link · account: iPhone XRR
  - Setup: The test replaces the My Drive list with five fake files: a pdf, a doc, a csv, an xlsx and a docx (no links yet).
  - Steps: Open Drive → My Drive and click the Files tab → For each file: open its ⋯ menu → Share, click “Share via link”, open the Permission list, note the choices, close the list and the drawer → Check that the pdf, doc and csv offer only “View only (no download)” and “View + download” → Check that the xlsx and docx also offer “Can edit”
  - Expected: “Can edit” is offered only for files the editors open and save as they are (xlsx, docx), never for pdf, doc or csv.

- **"Can edit" over "Never" falls back to 7 days; three recipients split on space, comma and semicolon; invisible characters stripped (ZL-21475) @smoke @drive** — notrun (drive-forms.spec.js:684)
  - Where: Tools → Drive → My Drive → Files tab, file ⋯ menu → Share → Share via link · account: iPhone XRR
  - Setup: The test replaces the My Drive list with one fake xlsx file and fakes the “send link” answer, so no email is sent.
  - Steps: Open Drive → My Drive, click the Files tab and open the xlsx’s ⋯ menu → Share → Share via link → Set Expires to “Never expires”, then set Permission to “Can edit” → Check that Expires has changed to “7 days” → In Recipients type “e2e-one@example.com e2e-two@example.com,e2e-three@example.com;” → In Optional message paste a message containing hidden characters (copied from Word or a PDF) → Click “Send link by email” → Check that the link is sent to exactly three recipients, as Can edit, for 7 days, and the message arrives without the hidden characters → Check that “Link sent to 3 recipients” appears, both boxes are cleared and the new link is listed
  - Expected: A “Can edit” link never keeps “Never expires” (it drops to 7 days); recipients are split on spaces, commas and semicolons; hidden pasted characters are stripped from the message (ZL-21475).

- **a refused link revoke is reported, not "Link revoked" @smoke @drive** — notrun (drive-forms.spec.js:731)
  - Where: Tools → Drive → My Drive → Files tab, file ⋯ menu → Share → Share via link · account: iPhone XRR
  - Setup: The test replaces the My Drive list with one fake pdf that has one active link, and makes the server refuse revoking it.
  - Steps: Open Drive → My Drive, click the Files tab and open the pdf’s ⋯ menu → Share → Share via link → Check that one active link is listed → Click its red Revoke link button → Check that an error message appears with the server’s reason → Check that no “Link revoked” message appears
  - Expected: A refused link revoke is reported and does not claim success. Known bug: it says “Link revoked” although the link still works.

- **re-copying a listed link with the clipboard refused opens no browser prompt @smoke @drive** — notrun (drive-forms.spec.js:754)
  - Where: Tools → Drive → My Drive → Files tab, file ⋯ menu → Share → Share via link · account: iPhone XRR
  - Setup: The test replaces the My Drive list with one fake pdf that has one active link, and makes the browser refuse clipboard writes.
  - Steps: Open Drive → My Drive, click the Files tab and open the pdf’s ⋯ menu → Share → Share via link → Click the Copy link button on the listed link → Check that the app answers with its own message or dialog → Check that no browser pop-up (“Copy this link:” prompt) opens
  - Expected: When the clipboard is refused, the app shows the link in its own message or dialog. Known bug: it opens the browser’s own “Copy this link:” prompt, which the app must never use.

### Drive — forms: public pages (stubbed)

- **the share page maps share_link_invalid to "Invalid link" @smoke @drive** — notrun (drive-forms.spec.js:860)
  - Where: A public share link, opened in a signed-out browser · account: none (signed-out visitor)
  - Setup: The test makes the share link answer that it is malformed.
  - Steps: Open a share link in a signed-out browser window → Check that the heading says “Invalid link” → Check that the text says “This share link is malformed.”
  - Expected: A malformed share link says “Invalid link — This share link is malformed.”

- **a rate-limited share page (429) says so, not the generic "Cannot open link" @smoke @drive** — notrun (drive-forms.spec.js:878)
  - Where: A public share link, opened in a signed-out browser · account: none (signed-out visitor)
  - Setup: The test makes the share link answer “too many requests” (rate limited).
  - Steps: Open a share link in a signed-out browser window → Check that an error screen shows → Check that its heading is not the generic “Cannot open link”
  - Expected: A visitor who is rate limited is told so (to try again later), not shown the generic error. Known bug: the page says “Cannot open link — We could not load this shared file.”

- **a request asking neither email nor name: no form, "Start upload", the rules column, the default thank-you @smoke @drive** — notrun (drive-forms.spec.js:900)
  - Where: A public upload (file request) link, opened in a signed-out browser · account: none (signed-out visitor)
  - Setup: The test fakes a request “[e2e] stub open request” that asks for neither email nor name, accepts images up to 100 MB, at most 3 files, expires in 3 days and has no thank-you message; uploads are faked.
  - Steps: Open the upload link in a signed-out browser window and reject the cookie banner → Check the left column: the title and description, File types “image/*”, Maximum size “100.0 MB”, File limit “Up to 3 files”, and an Expires row with the date → Check that there is no email or name field, the text says “You’re all set — start the upload whenever you’re ready.”, the first step reads “Get started” and the button says “Start upload” → Click Start upload and check the upload area appears with no “Uploading as” line → Pick a png file and wait for its tick → Check that “Upload complete” shows with “Your files were uploaded successfully. You can close this page.”
  - Expected: A request that asks for no details goes straight to uploading, shows its rules (including expiry and “Up to N files”), and ends with the default thank-you.

- **an expired upload session and a full file limit each show their own message @smoke @drive** — notrun (drive-forms.spec.js:958)
  - Where: A public upload (file request) link, opened in a signed-out browser · account: none (signed-out visitor)
  - Setup: The test fakes a request “[e2e] stub refused uploads” that allows 2 files and never expires; the first upload is answered “session expired”, the second “file limit reached”.
  - Steps: Open the upload link in a signed-out browser window and reject the cookie banner → Check that File limit says “Up to 2 files” and there is no Expires row → Click Start upload and pick two png files → Check that the first file says “Your upload session expired. Please refresh the page.” → Check that the second file says “You have reached the maximum number of files for this upload.” → Check that the count reads “0 of 2 uploaded” and no “Upload complete” shows
  - Expected: Each refused upload shows its own plain message: an expired session and a full file limit are told apart.

### Drive — forms: Create Folder access (stubbed)

- **"Project Users" with no role picked sends no access: the folder stays private @smoke @drive** — notrun (drive-forms.spec.js:1055)
  - Where: Tools → Drive → My Drive (top level) → Create Folder · account: iPhone XRR
  - Setup: The test replaces the My Drive list with an empty one and fakes the folder’s creation, so no folder is made and no one is given access.
  - Steps: Open Drive → My Drive and click Create Folder → Check the drawer says “Skip assigning permissions to keep this folder private…” → Type the name “e2e-stub-fm-private” → Choose “Project Users” and check it says “Applies to all N project users with Drive view access.” → Do not pick a permission; click Create Folder → Check that the folder is created at the top level with no access list (private)
  - Expected: With “Project Users” chosen but no permission picked, the folder is created private: no one else is given access.

- **"Project Users" + Editor sends one editor entry per eligible user @smoke @drive** — notrun (drive-forms.spec.js:1072)
  - Where: Tools → Drive → My Drive (top level) → Create Folder · account: iPhone XRR
  - Setup: The test replaces the My Drive list with an empty one and fakes the folder’s creation, so no folder is made and no one is given access.
  - Steps: Open Drive → My Drive and click Create Folder → Type the name “e2e-stub-fm-project” → Choose “Project Users” and note the N in “Applies to all N project users…” → Pick “Editor” in Select permission and click Create Folder → Check that exactly N people are given Editor access, each once
  - Expected: “Project Users” with a permission gives that permission to every project user with Drive view access, one entry each. (The test notes whether the creator is in the list.)

- **Specific Users as Editor and Owner; the drawer starts clean on every reopen @smoke @drive** — notrun (drive-forms.spec.js:1095)
  - Where: Tools → Drive → My Drive (top level) → Create Folder · account: iPhone XRR
  - Setup: The test replaces the My Drive list with an empty one and fakes the folder’s creation, so no folder is made and no one is given access.
  - Steps: Open Create Folder, type a name, choose “Project Users” with Owner, then click Cancel → Open Create Folder again and check it is clean: empty name, “Specific Users” chosen, nobody selected → Click the first person in the list and check they are ticked as Viewer; change them to Editor → Click the second person and change them to Owner; check “2 users selected” → Type the name “e2e-stub-fm-specific” and click Create Folder → Check that exactly those two people are given access, first as Editor, second as Owner, and the creator is not in the list → Open Create Folder again and check it is clean once more
  - Expected: Specific Users can be given Editor or Owner, and the drawer forgets every earlier choice each time it opens, after Cancel and after a create (ZL-20170).

### Drive — forms: Activity Log (stubbed)

- **the Files and Folders chips filter the rows and show counts; rows are grouped Today / Yesterday @smoke @drive** — notrun (drive-forms.spec.js:1203)
  - Where: Tools → Drive → Activity Log (clock button) · account: iPhone XRR
  - Setup: The test fakes the project’s activity: 50 entries on the first page (30 about files, 20 about folders, half today and half yesterday) out of 60.
  - Steps: Open Drive and click the Activity Log (clock) button → Check the heading shows “50 / 60” and the chips count All 50, Files 30, Folders 20 → Check the entries are grouped under “Today” then “Yesterday” → Click Files and check 30 entries show, all tagged “file”, still under Today and Yesterday → Click Folders and check 20 entries show, all tagged “folder” → Click All and check all 50 are back
  - Expected: The Files and Folders chips really filter the entries and show how many each holds; entries are grouped by day.

- **scrolling to the end loads the next page with offset=50, then stops @smoke @drive** — notrun (drive-forms.spec.js:1232)
  - Where: Tools → Drive → Activity Log (clock button) · account: iPhone XRR
  - Setup: The test fakes the project’s activity: 50 entries on the first page and 10 more on the second, 60 in all.
  - Steps: Open Drive and click the Activity Log (clock) button → Check 50 entries show → Scroll to the bottom of the drawer → Check the next page is loaded (from entry 50) and 60 entries show, with “60 / 60” in the heading and Files counting 40 → Scroll to the bottom again and check nothing more is loaded
  - Expected: Scrolling to the end of the Activity Log loads the next page once, and stops when everything is shown.

### Drive — lists: file paging (stubbed)

- **the table pages past the first 100 files: page 6 asks for offset=100, and a refresh reloads all 130 in one request @smoke @drive** — notrun (drive-lists.spec.js:218)
  - Where: Tools → Drive → My Drive → Files tab · account: iPhone XRR
  - Setup: The test replaces the My Drive list with 130 fake png files, handed out 100 at a time like the real server, and answers the delete itself.
  - Steps: Open Drive → My Drive and click the Files tab → Check that the first request asked for 100 files, that the Files tab reads 130 and the table footer reads “1–20 of 130 items” → Click page 6 in the table footer → Check that Drive asks for the next 100 files (from file 101) and shows files 101 to 120, footer “101–120 of 130 items” → Tick file 101, click Delete in the selection bar and confirm “Delete 1 item?” → Check that the list reloads all 130 files in one request and stays on page 6 without asking for file 101 onwards again
  - Expected: Files load 100 at a time as the table pages past them, and a refresh brings back as many as were loaded in a single request.

- **the grid loads the next 100 files when its end scrolls into view @smoke @drive** — notrun (drive-lists.spec.js:263)
  - Where: Tools → Drive → My Drive → Files tab, Grid view · account: iPhone XRR
  - Setup: The test replaces the My Drive list with 130 fake png files, handed out 100 at a time.
  - Steps: Open Drive → My Drive and click the Files tab → Click Grid view → Check that 100 cards are shown → Scroll to the end of the grid → Check that Drive asks for the next 100 files and that 130 cards are now shown, with no loading marker left at the end
  - Expected: The grid loads the rest of the files when its end comes into view. If the next page was asked for before scrolling (a very tall screen), that is noted, not failed.

- **a failed next page stops paging with no retry loop; the next visit pages again @smoke @drive** — notrun (drive-lists.spec.js:293)
  - Where: Tools → Drive → My Drive → Files tab · account: iPhone XRR
  - Setup: The test replaces the My Drive list with 130 fake png files and makes the server fail the request for files 101 onwards, until the test lets it through.
  - Steps: Open Drive → My Drive and click the Files tab; check the Files tab reads 130 → Click page 6 in the table footer → Wait three seconds and check that Drive asked for the next files only once → Check that the footer now counts 100 items and the Files tab reads 100 → Let the server answer again, then click Shared with me and back to My Drive → Check that the Files tab reads 130 again → Click page 6 and check that files 101 onwards load
  - Expected: A failed next page is not retried over and over; leaving and coming back starts paging again.

- **a failed next page is reported, not shown as an empty Drive @smoke @drive** — notrun (drive-lists.spec.js:338)
  - Where: Tools → Drive → My Drive → Files tab · account: iPhone XRR
  - Setup: The test replaces the My Drive list with 130 fake png files and makes the server fail the request for files 101 onwards.
  - Steps: Open Drive → My Drive and click the Files tab → Click page 6 in the table footer → Check that the table still shows files rather than an empty Drive → Check that an error message appears
  - Expected: When the next files fail to load, the user is told, and the files already loaded stay on screen. Known bug: no message; page 6 shows “No files or folders yet” with Upload hints while the footer highlights page 5.

- **a slow list for a section the user already left is dropped @smoke @drive** — notrun (drive-lists.spec.js:364)
  - Where: Tools → Drive → Shared with me and My Drive · account: iPhone XRR
  - Setup: The test replaces both lists: Shared with me holds one fake folder, My Drive one fake folder of iPhone XRR’s own, and the My Drive file list is made to answer 6 seconds late.
  - Steps: Open Drive; it lands on Shared with me; check the shared folder is listed → Click My Drive (its list is slow) → Straight away click Shared with me again → Wait for the late My Drive answer to arrive → Check that Shared with me is still selected, still lists the shared folder and does not list the My Drive folder
  - Expected: A late answer for a section the user already left never replaces what is on screen.

- **a slow folder the user already left does not replace the folder now open @smoke @drive** — notrun (drive-lists.spec.js:404)
  - Where: Tools → Drive → My Drive → Folders tab · account: iPhone XRR
  - Setup: The test replaces the My Drive list with two fake folders, A and B, each holding one png file; folder A’s files answer 6 seconds late.
  - Steps: Open Drive → My Drive (Folders tab) → Open folder A (it is slow) → Straight away click the back arrow in the path bar → Open folder B and check its file is listed → Wait for folder A’s late answer to arrive → Check that the path still shows B, B’s file is still listed and A’s file is not
  - Expected: A late answer for a folder the user already left never replaces the folder now open.

### Drive — lists: selection and bulk actions (stubbed)

- **the header checkbox selects the table page, clears it, and turns half-ticked on one row @smoke @drive** — notrun (drive-lists.spec.js:449)
  - Where: Tools → Drive → My Drive → Files tab · account: iPhone XRR
  - Setup: The test replaces the My Drive list with 25 fake png files.
  - Steps: Open Drive → My Drive and click the Files tab → Tick the checkbox in the table header → Check that the selection bar reads “20 items selected” and all 20 rows on the page are ticked → Untick the header checkbox and check the selection bar goes away → Tick one row; check the bar reads “1 item selected” and the header checkbox is half-ticked → Tick the header checkbox; check the bar reads “20 items selected” → Click Clear in the bar; check the bar goes away and the header checkbox is unticked
  - Expected: The header checkbox ticks and unticks every row of the page on screen, and shows a half tick when only some are ticked.

- **rows ticked on page 1 stay selected when more are ticked on page 2 @smoke @drive** — notrun (drive-lists.spec.js:478)
  - Where: Tools → Drive → My Drive → Files tab · account: iPhone XRR
  - Setup: The test replaces the My Drive list with 25 fake png files.
  - Steps: Open Drive → My Drive and click the Files tab → Tick the header checkbox; the bar reads “20 items selected” → Go to page 2 of the table; check the bar still reads “20 items selected” and the header checkbox is unticked → Tick the header checkbox on page 2 → Check that the bar reads “25 items selected”
  - Expected: A selection adds up across table pages. Known bug: ticking anything on page 2 silently drops everything ticked on page 1 (the bar reads “5 items selected”).

- **switching the Folders/Files tab or the section clears the selection @smoke @drive** — notrun (drive-lists.spec.js:499)
  - Where: Tools → Drive → My Drive (Folders and Files tabs) and Shared with me · account: iPhone XRR
  - Setup: The test replaces the My Drive list with one fake folder and two fake png files; Shared with me is empty.
  - Steps: Open Drive → My Drive, click the Files tab and tick the first file; the bar reads “1 item selected” → Click the Folders tab; check the selection bar goes away → Tick the folder, then click the Files tab; check the bar goes away and the first file is unticked → Tick both files, then click Shared with me; check the bar goes away → Go back to My Drive → Files; check there is no bar and both files are unticked → Tick the folder on the Folders tab and open it; note whether the selection bar is still shown inside
  - Expected: Changing the Folders/Files tab or the section starts with nothing selected. What happens on opening a ticked folder is only noted.

- **bulk Delete and Move on mixed rights act only on what the user may change and say how many were skipped @smoke @drive** — notrun (drive-lists.spec.js:540)
  - Where: Tools → Drive → My Drive → Files tab, selection bar · account: iPhone XRR
  - Setup: The test replaces the My Drive list with a fake folder and five fake png files with different rights — two full, one edit, one download-only, one view-only — and answers the delete and the move itself.
  - Steps: Open Drive → My Drive and click the Files tab → Tick all five files; check the bar reads “5 items selected” and shows Delete, Move to... and Download → Click Delete; check the confirm reads “Delete 2 items?” and “(3 items skipped — insufficient permissions)” → Confirm; check only the two full-rights files are sent and the message reads “2 of 2 items deleted (3 items skipped — insufficient permissions)” → Tick all five again and click Move to...; pick the folder and click Move here → Check the message “2 items skipped — insufficient permissions”, that only the two full and the edit file are sent to that folder, and then “3 of 3 items moved”
  - Expected: Bulk Delete and Move act only on the items the user may delete or edit, and say how many were left out.

- **bulk Download on mixed rights zips the downloadable files and names the one left out @smoke @drive** — notrun (drive-lists.spec.js:597)
  - Where: Tools → Drive → My Drive → Files tab, selection bar · account: iPhone XRR
  - Setup: The test replaces the My Drive list with two fake full-rights png files and one view-only png, answers the zip itself, and turns off the browser’s save-to-folder picker so the server zip is used.
  - Steps: Open Drive → My Drive and click the Files tab → Tick all three files; the bar reads “3 items selected” → Click Download in the bar → Check the message “Downloading 2 files (1 skipped — no download permission)” → Check that only the two full-rights files are sent for the zip → Check the warning “Not in the download (no download permission): …” names the view-only file and not the others
  - Expected: A mixed download zips only what the user may download and names each file left out.

- **a folder in a bulk Download is named as left out @smoke @drive** — notrun (drive-lists.spec.js:626)
  - Where: Tools → Drive → My Drive → inside a folder, selection bar · account: iPhone XRR
  - Setup: The test replaces the My Drive list with a fake folder holding a subfolder, two full-rights png files and one view-only png, and answers the zip itself.
  - Steps: Open Drive → My Drive and open the folder → Tick the subfolder and the three files; the bar reads “4 items selected” → Click Download in the bar → Check the message “Downloading 2 files (1 skipped — no download permission)” and the warning naming the view-only file → Check that some message names the subfolder as not downloaded
  - Expected: Every ticked item is either downloaded or named as left out. Known bug: the subfolder is dropped without a word (4 ticked, “2 files, 1 skipped”).

- **Move to… "Drive (Root)" sends target_folder_id null, for one file and for a selection @smoke @drive** — notrun (drive-lists.spec.js:662)
  - Where: Tools → Drive → My Drive → inside a folder, file ⋯ menu → Move to... and selection bar → Move to... · account: iPhone XRR
  - Setup: The test replaces the My Drive list with a fake folder holding two fake png files and answers both moves itself.
  - Steps: Open Drive → My Drive and open the folder → Open the first file’s ⋯ menu → Move to... → Click the folder in the tree, then click “Drive (Root)”, then Move here → Check the move is sent with no destination folder (the root) and the message “Moved “…”” → Tick both files and click Move to... in the selection bar → Click the folder, then “Drive (Root)”, then Move here → Check the bulk move is sent with no destination folder and the message “2 of 2 items moved”
  - Expected: Choosing “Drive (Root)” moves the item(s) to the top level of My Drive.

### Drive — lists: search and tag filter (stubbed)

- **typing a search sends one request, after the pause, with the whole word @smoke @drive** — notrun (drive-lists.spec.js:722)
  - Where: Tools → Drive → My Drive, search box · account: iPhone XRR
  - Setup: The test replaces the My Drive list with a fake “…report.pdf” and another fake file.
  - Steps: Open Drive → My Drive and click the Files tab → Click the search box and type “report” one letter at a time → Check that one file search and one folder search are sent, both for “report” (none for “r”, “re”, …) → Check that the results show the report file only, with no Folders/Files tabs
  - Expected: Search waits for the user to stop typing and then asks once, for the whole word.

- **the tag filter asks for tagged files and folders and shows only those @smoke @drive** — notrun (drive-lists.spec.js:746)
  - Where: Tools → Drive → My Drive, Tag filter · account: iPhone XRR
  - Setup: The test replaces the project’s tags with two fake tags and the My Drive list with two fake folders and two fake files; one folder and one file carry the first tag.
  - Steps: Open Drive → My Drive (Folders tab) → Open the Tag filter and pick “e2e-stub-tag-match” → Check that Drive asks for the tagged files and the tagged folders → Check the Folders tab shows only the tagged folder → Click the Files tab and check it shows only the tagged file
  - Expected: The tag filter shows only the folders and files that carry the tag.

- **a tag with no items says the filter found nothing, not "No files or folders yet" @smoke @drive** — notrun (drive-lists.spec.js:772)
  - Where: Tools → Drive → My Drive, Tag filter · account: iPhone XRR
  - Setup: The test replaces the project’s tags with two fake tags and the My Drive list with a fake folder and file; nothing carries the second tag.
  - Steps: Open Drive → My Drive (Folders tab) → Open the Tag filter and pick “e2e-stub-tag-none” → Check the list is empty → Check the empty list does not say “No files or folders yet” and shows no Upload files / Create folder hints
  - Expected: An empty tag filter says the filter found nothing. Known bug: it says “No files or folders yet” with upload hints, as if the Drive were empty (the Starred filter had the same fault, fixed in ZL-21810).

### Drive — lists: Sharing column (stubbed)

- **My Drive Sharing column: "Only you", one avatar a person, and past 3 a "+N" that lists the rest @smoke @drive** — notrun (drive-lists.spec.js:798)
  - Where: Tools → Drive → My Drive → Files tab, Sharing column · account: iPhone XRR
  - Setup: The test replaces the My Drive list with three fake files owned by iPhone XRR: shared with nobody, with two people (iPhone XRR’s own entry included), and with five people.
  - Steps: Open Drive → My Drive and click the Files tab → Check the column header reads Sharing → Check the first file reads “Only you” → Check the second file shows two avatars (not one for iPhone XRR) and no “+” → Check the third file shows three avatars and a “+2” → Hover the “+2” and check a popup lists the other two people → Check none of the three rows has the “Shared with you” icon
  - Expected: The Sharing column shows who a file is shared with: “Only you”, one avatar a person, and past three a “+N” that lists the rest.

- **Shared with me: the "Shared with you" icon, and a re-share reads "Shared by X · Created by Y" in list and grid @smoke @drive** — notrun (drive-lists.spec.js:837)
  - Where: Tools → Drive → Shared with me → Files tab (list, then Grid view) · account: iPhone XRR (Lava Device’s name is used on the fake rows)
  - Setup: The test replaces the Shared with me list with two fake files: one made by Lava Device and shared directly, one made by a person no longer in the project and re-shared by Lava Device.
  - Steps: Open Drive → Shared with me and click the Files tab → Check the column header reads Shared by → Check both rows show the team icon, and hovering it says “Shared with you” → Check the first row reads Lava Device; hovering it says “Shared by Lava Device” → Check the second row also reads Lava Device; hovering it says “Shared by Lava Device · Created by Unknown” → Click Grid view, hover the avatar on the second card and check the same “Shared by … · Created by …” text
  - Expected: A shared item credits whoever shared it with you; a re-share also names its creator.

### Drive — flaky: drag rules on stubbed rows (Classic layout)

- **a view-only file dragged onto a folder is refused with a message and nothing is sent @smoke @drive** — notrun (drive-lists.spec.js:945)
  - Where: Tools → Drive (Classic layout) → My Drive → inside a folder · account: iPhone XRR
  - Setup: The test switches Drive to the Classic (full-page) layout and replaces the My Drive list with a fake folder holding a subfolder and a view-only png file.
  - Steps: Open Drive → My Drive and open the folder → Drag the view-only file’s row onto the subfolder’s row (the subfolder highlights) and let go → Check the warning “You do not have permission to move this item” → Check nothing was sent and the file is still listed
  - Expected: An item the user may not edit cannot be moved by dragging; Drive says why.

- **under Shared with me a drag onto a folder sends nothing @smoke @drive** — notrun (drive-lists.spec.js:969)
  - Where: Tools → Drive (Classic layout) → Shared with me → Folders tab · account: iPhone XRR
  - Setup: The test switches Drive to the Classic layout and replaces the Shared with me list with two fake shared folders with full rights.
  - Steps: Open Drive → Shared with me (Folders tab) → Drag the first folder’s row onto the second (the second highlights) and let go → Check no move is sent and the first folder is still listed → Note whether any message explains why nothing happened
  - Expected: Nothing is moved under Shared with me (those items live in their owner’s tree). Whether a message appears is only noted.

- **folder onto folder: refused without edit rights, moved with them @smoke @drive** — notrun (drive-lists.spec.js:999)
  - Where: Tools → Drive (Classic layout) → My Drive → Folders tab · account: iPhone XRR
  - Setup: The test switches Drive to the Classic layout, replaces the My Drive list with three fake folders (one view-only, one with full rights, one target) and answers the move itself.
  - Steps: Open Drive → My Drive (Folders tab) → Drag the view-only folder onto the target folder and let go → Check the warning “You do not have permission to move this item” and that nothing was sent → Drag the full-rights folder onto the target folder and let go → Check the move is sent with the target folder and the message reads “… “<folder>” → “<target>””
  - Expected: A folder moves into another by dragging only when the user may edit it.

- **grid view: a view-only card is refused, an editable file card moves into the folder card @smoke @drive** — notrun (drive-lists.spec.js:1022)
  - Where: Tools → Drive (Classic layout) → My Drive → inside a folder, Grid view · account: iPhone XRR
  - Setup: The test switches Drive to the Classic layout, replaces the My Drive list with a fake folder holding a subfolder, a view-only png and an editable png, and answers the move itself.
  - Steps: Open Drive → My Drive, open the folder and click Grid view → Drag the view-only file’s card onto the subfolder’s card (it highlights) and let go → Check the warning “You do not have permission to move this item” and that nothing was sent → Drag the editable file’s card onto the subfolder’s card and let go → Check the move is sent with the subfolder as target and the message reads “… “<file>” → “<subfolder>””
  - Expected: Dragging in the grid follows the same rights as in the list.

### Drive — live: two users (revoke, realtime, concurrency)

- **A revokes B while B is inside the folder: B refetches, gets the no-access warning and is back at the Shared with me root (ZL-21815) @drive** — notrun (drive-live.spec.js:260)
  - Where: Tools → Drive → My Drive (iPhone XRR), and Tools → Drive → Shared with me, inside the shared folder (Lava Device) · account: iPhone XRR and Lava Device
  - Setup: As iPhone XRR, inside a private test folder in My Drive, creates a folder “e2e-drive-…-lv01-revoke” and shares it with Lava Device as Viewer.
  - Steps: As Lava Device, open Drive → Shared with me, search for the folder and open it; stay inside it → As iPhone XRR, open the folder’s ⋯ menu → Share, untick Lava Device and press Save Access → Without touching Lava Device’s screen, watch it for up to a minute → Check that a yellow message “You don't have access to this folder.” appears (when the server refuses the folder) → Check that Lava Device is back at the Shared with me root (the Folders / Files tabs show, no folder path) → Search for the folder name and check it is no longer listed
  - Expected: Losing access while inside the folder takes Lava Device out of it without a reload: the list is fetched again, the server refuses the folder, the no-access message shows and the view returns to the Shared with me root. The server’s answer to that refetch is noted.

- **a file shared with B appears in B’s open list live, and goes live when A revokes it (drive_file_unshared) @drive** — notrun (drive-live.spec.js:300)
  - Where: Tools → Drive → Shared with me (Lava Device), and Tools → Drive → My Drive (iPhone XRR) · account: iPhone XRR and Lava Device
  - Setup: As iPhone XRR, uploads a png “e2e-drive-…-lv02-file.png” into a private test folder in My Drive (not shared yet).
  - Steps: As Lava Device, open Drive → Shared with me and search for the file name; check nothing is found → As iPhone XRR, share the png with Lava Device at View (⋯ → Share → tick View → Save Access) → Without reloading, check the png appears in Lava Device’s search results → As iPhone XRR, open Share on the png again, untick Lava Device and press Save Access → Without reloading, check the png disappears from Lava Device’s results
  - Expected: A shared file reaches Lava Device’s open list live, and leaves it live when access is taken away (the file revoke event, which until now only folders were tested for).

- **A edits a comment: B’s open Info panel shows the new text live (drive_comment_updated, I07) @drive** — notrun (drive-live.spec.js:326)
  - Where: Tools → Drive → file ⋯ → Info, on both accounts · account: iPhone XRR and Lava Device
  - Setup: As iPhone XRR, uploads a png “e2e-drive-…-lv03-comment.png” into a private test folder in My Drive and shares it with Lava Device at View.
  - Steps: As Lava Device, find the png under Shared with me and open its ⋯ menu → Info; leave the panel open → As iPhone XRR, open the same file’s Info panel and add a comment “e2e live comment …” (type it, press Enter) → Check the comment appears in Lava Device’s panel without a reload → As iPhone XRR, press the pencil on that comment, change the text to “e2e edited remark …” and press Save → Without reloading, check Lava Device’s panel now shows the edited text and no longer the old one
  - Expected: An edited comment updates live in another person’s open Info panel, as added and deleted comments already do (I07).

- **A bulk-deletes two files in the folder B has open: both rows leave B’s list live @drive** — notrun (drive-live.spec.js:377)
  - Where: Tools → Drive, inside the shared folder, on both accounts · account: iPhone XRR and Lava Device
  - Setup: As iPhone XRR, inside a private test folder in My Drive, creates a folder “e2e-drive-…-lv04-bulk” holding two pngs, shares it with Lava Device as Viewer, and opens it.
  - Steps: As Lava Device, find the folder under Shared with me, open it and check both pngs are listed → As iPhone XRR, tick both pngs, press Delete in the selection bar and confirm → Without reloading, check both pngs disappear from Lava Device’s list
  - Expected: A bulk delete reaches the other person’s open folder live, like a single delete does. Which socket event arrived, and its fields, is noted.

- **A creates a subfolder in the folder B has open: it appears in B’s list live @drive** — notrun (drive-live.spec.js:421)
  - Where: Tools → Drive, inside the shared folder, on both accounts · account: iPhone XRR and Lava Device
  - Setup: As iPhone XRR, inside a private test folder in My Drive, creates a folder “e2e-drive-…-lv05-open”, shares it with Lava Device as Viewer and opens it.
  - Steps: As Lava Device, find the folder under Shared with me and open it → As iPhone XRR, press Create Folder and create “e2e-drive-…-lv05-new-sub” inside it → Without reloading, check the new subfolder appears in Lava Device’s list within about 45 seconds
  - Expected: A subfolder someone else creates shows up live in the folder you have open. Known bug: the live update ignores a new folder whose parent is only named inside the event’s folder record, so it may show only after a badge refresh or a reload.

- **a subfolder B makes in A’s folder opens for A without "no longer exists" (BUG-016) @drive** — notrun (drive-live.spec.js:448)
  - Where: Tools → Drive, inside the shared folder, on both accounts · account: iPhone XRR and Lava Device
  - Setup: As iPhone XRR, inside a private test folder in My Drive, creates a folder “e2e-drive-…-lv06-owner”, shares it with Lava Device as Editor and opens it.
  - Steps: As Lava Device, find the folder under Shared with me, open it and create a folder “e2e-drive-…-lv06-by-b” with Create Folder → As iPhone XRR, note whether the new folder appeared without a reload, then reopen Drive into the shared folder → Check the folder Lava Device made is listed → Open it → Check that no “Navigated to root as current folder no longer exists” message appears and that the path still ends in that folder
  - Expected: The owner can open a subfolder an Editor created in their folder; Drive does not mistake it for a deleted folder and bounce to the root.

- **B saves a Markdown file: A’s open version history refreshes (drive_version_created) @drive** — notrun (drive-live.spec.js:489)
  - Where: Tools → Drive → a Markdown file (iPhone XRR in its Version history; Lava Device in Edit file) · account: iPhone XRR and Lava Device
  - Setup: As iPhone XRR, in a private test folder in My Drive, creates a Markdown file with New → Markdown, types a first version and saves, then shares it with Lava Device at Edit.
  - Steps: As iPhone XRR, preview the Markdown file and press Version history; leave the panel open → As Lava Device, find the file under Shared with me, open ⋯ → Edit file, change the text and press Save → Without reloading, check iPhone XRR’s Version history panel loads again and now lists Lava Device → Close both documents (Skip on “Let others know”)
  - Expected: Another person’s save shows in an open version history straight away.

- **A and B edit one Markdown file at once: the later save gets the real 409 conflict bar; "Save my version anyway" wins @drive** — notrun (drive-live.spec.js:531)
  - Where: Tools → Drive → a Markdown file in Edit file, on both accounts · account: iPhone XRR and Lava Device
  - Setup: As iPhone XRR, in a private test folder in My Drive, creates a Markdown file (New → Markdown), saves a first text and shares it with Lava Device at Edit.
  - Steps: As iPhone XRR, open the file with ⋯ → Edit file → As Lava Device, find it under Shared with me and open ⋯ → Edit file too → As Lava Device, change the text and press Save → As iPhone XRR, change the text and press Save → Check iPhone XRR’s save is refused and a red bar offers “Load the latest version” and “Save my version anyway” → Press “Save my version anyway” and check the save goes through and the bar goes → Close both documents (Skip on “Let others know”)
  - Expected: When two people edit the same Markdown file, the later save never silently overwrites the earlier one: the server refuses it (file changed since opened) and the person chooses; “Save my version anyway” then saves.

### Drive — live: badges

- **opening a folder and a file sends notification:read with the item’s reference_id (skipChildFiles for the folder), never notification:level:read (ZL-18873) @drive** — notrun (drive-live.spec.js:587)
  - Where: Tools → Drive → My Drive (iPhone XRR), with the browser’s developer tools on the Network → WS tab · account: iPhone XRR
  - Setup: As iPhone XRR, in a private test folder in My Drive, creates a folder “e2e-drive-…-lv10-folder”.
  - Steps: Open the folder and check the socket sent “notification:read” for this folder’s id, marked for Drive, with “skip child files” on → Upload a png into it and preview the png → Check the socket sent “notification:read” for the png’s id, without “skip child files” → Check no Drive “notification:level:read” was sent at any point
  - Expected: Opening a folder or a file marks only that item read on the server; the tool-wide level read (which cleared every Drive badge) is never used.

- **a file shared with B shows an unread dot; previewing it clears the dot @drive** — notrun (drive-live.spec.js:636)
  - Where: Tools → Drive → Shared with me (Lava Device), and Tools → Drive → My Drive (iPhone XRR) · account: iPhone XRR and Lava Device
  - Setup: As iPhone XRR, uploads a png “e2e-drive-…-lv11-dot.png” into a private test folder in My Drive.
  - Steps: As Lava Device, open Drive → Shared with me and search for the file name → As iPhone XRR, share the png with Lava Device at View → Check the png appears in Lava Device’s results with a small red unread dot on its icon → As Lava Device, preview the png (eye) and close the preview → Check the red dot is gone
  - Expected: A newly shared file carries its own unread dot until the person opens it.

- **the Folders and Files inner tabs count a new shared folder and file; opening each lowers its tab @drive** — notrun (drive-live.spec.js:666)
  - Where: Tools → Drive → Shared with me root, Folders and Files tabs (Lava Device) · account: iPhone XRR and Lava Device
  - Setup: As iPhone XRR, in a private test folder in My Drive, creates a folder “e2e-drive-…-lv12-folder” and uploads a png “e2e-drive-…-lv12-file.png”.
  - Steps: As Lava Device, open Drive → Shared with me and note the red numbers on the Folders and Files tabs → As iPhone XRR, share the folder and the png with Lava Device (Viewer / View) → Check the Folders number and the Files number each go up → As Lava Device, search for the png, preview it, close the preview and clear the search: check the Files number goes down → Search for the folder, open it and go back: check the Folders number goes down
  - Expected: The inner tabs count unread items where Lava Device sees them: the shared folder on Folders, the shared file (at the Shared with me root) on Files; opening each lowers its own tab. Both tab numbers before and after are noted.

- **a grandchild’s unread rolls up to the child and the shared parent, and clears when opened (ZL-20181) @drive** — notrun (drive-live.spec.js:718)
  - Where: Tools → Drive → Shared with me (Lava Device), and Tools → Drive → My Drive (iPhone XRR) · account: iPhone XRR and Lava Device
  - Setup: As iPhone XRR, in a private test folder in My Drive, creates “…-lv13-parent” holding “…-lv13-child” holding “…-lv13-grandchild”.
  - Steps: As Lava Device, open Drive → Shared with me → As iPhone XRR, share the parent with Lava Device as Viewer, then share the grandchild with Lava Device as Editor → As Lava Device, search for the parent and check its folder icon shows a red number → Open the parent: check the child shows a red number → Open the child: check the grandchild shows a red number → Search for the parent again: check it still shows a red number (the grandchild is still unread) → Open parent → child → grandchild, then search for the parent again: check its red number is gone
  - Expected: An unread item deep in a shared tree counts on every folder above it, even after the parent’s own unread was read, and clears from all of them once it is opened.

- **a shared folder with an unread badge moved into another shared folder counts on its new parent (ZL-20181) @drive** — notrun (drive-live.spec.js:772)
  - Where: Tools → Drive → Shared with me (Lava Device), and Tools → Drive → My Drive (iPhone XRR) · account: iPhone XRR and Lava Device
  - Setup: As iPhone XRR, in a private test folder in My Drive, creates two folders “…-lv14-parent” and “…-lv14-moved”.
  - Steps: As Lava Device, open Drive → Shared with me → As iPhone XRR, share both folders with Lava Device as Viewer → As Lava Device, search “…-lv14” and check both folders show a red number → Open the parent folder (this reads its own unread), search “…-lv14” again and check the parent shows no number while the other folder still does → As iPhone XRR, move “…-lv14-moved” into “…-lv14-parent” (⋯ → Move to... → pick it → Move here) → Without reloading, check Lava Device’s parent folder shows a red number again
  - Expected: Badges follow the folder tree as it is now, not as it was when the notification was made: a moved unread folder counts on its new parent.

- **A deletes a folder shared with B: B’s row and its unread go without B opening it (ZL-18490) @drive** — notrun (drive-live.spec.js:817)
  - Where: Tools → Drive → Shared with me (Lava Device), and Tools → Drive → My Drive (iPhone XRR) · account: iPhone XRR and Lava Device
  - Setup: As iPhone XRR, in a private test folder in My Drive, creates a folder “e2e-drive-…-lv15-deleted”.
  - Steps: As Lava Device, open Drive → Shared with me, search for the folder name and note the number on the Shared with me tab → As iPhone XRR, share the folder with Lava Device as Viewer → Check the folder appears for Lava Device and the tab number goes up → As iPhone XRR, delete the folder (⋯ → Delete → OK) → Without Lava Device opening anything, check the folder disappears and the tab number goes back down
  - Expected: Deleting a shared item also removes its unread count for the person it was shared with, without them having to open it.

- **the Film Tools Drive tile badge rises with a new share and falls once B opens it @drive** — notrun (drive-live.spec.js:846)
  - Where: Tools (Film Tools page) → the Drive tile (Lava Device), and Tools → Drive → My Drive (iPhone XRR) · account: iPhone XRR and Lava Device
  - Setup: As iPhone XRR, in a private test folder in My Drive, creates a folder “e2e-drive-…-lv16-tile”.
  - Steps: As Lava Device, open the Tools page and note the red number on the Drive tile (none counts as 0) → As iPhone XRR, share the folder with Lava Device as Viewer → Check the Drive tile’s number goes up without a reload → As Lava Device, open Drive → Shared with me, search for the folder and open it → Go back to the Tools page and check the Drive tile’s number went down
  - Expected: The Drive tile on the Tools page counts Drive’s unread items live and drops once they are opened.

### Drive — live: B’s rights changed while B works (admins, open drawers)

- **an admin with Drive View off still gets Drive and Upload @drive** — notrun (drive-live.spec.js:922)
  - Where: Settings → Admin Settings → User Management (iPhone XRR); Tools → Drive (Lava Device) · account: iPhone XRR and Lava Device
  - Setup: In User Management, iPhone XRR turns Lava Device’s Admin switch off, sets Lava Device’s Drive rights to Post, View and Download all off, then turns Lava Device’s Admin switch back on (Lava Device’s Drive switches as an admin are noted).
  - Steps: As Lava Device, open Tools → Drive → Check that “You no longer have access to Z-Drive” does not show and the My Drive / Shared with me tabs do → Open My Drive and check the Upload button is shown
  - Expected: An admin is never locked out of Drive by the Drive View switch: Drive opens and Upload is offered. The server’s answer to the first file list is noted.

- **an admin with Drive Post off still gets Drive and Upload @drive** — notrun (drive-live.spec.js:922)
  - Where: Settings → Admin Settings → User Management (iPhone XRR); Tools → Drive (Lava Device) · account: iPhone XRR and Lava Device
  - Setup: In User Management, iPhone XRR turns Lava Device’s Admin switch off, sets Lava Device’s Drive rights to View and Download on, Post off, then turns Lava Device’s Admin switch back on (Lava Device’s Drive switches as an admin are noted).
  - Steps: As Lava Device, open Tools → Drive → Check that “You no longer have access to Z-Drive” does not show and the My Drive / Shared with me tabs do → Open My Drive and check the Upload button is shown
  - Expected: An admin keeps Drive’s add actions whatever the Drive Post switch says: Drive opens and Upload is offered.

- **posting turned off while B’s Share drawer is open: Save shows the server’s answer; a refusal keeps the drawer open @drive** — notrun (drive-live.spec.js:968)
  - Where: Tools → Drive → Shared with me → file ⋯ → Share (Lava Device); Settings → Admin Settings → User Management (iPhone XRR) · account: iPhone XRR and Lava Device
  - Setup: As iPhone XRR, creates (once for this group) a png “e2e-drive-…-lv27-shared.png” in a private test folder in My Drive and shares it with Lava Device at Edit.
  - Steps: As Lava Device, find the png under Shared with me and open ⋯ → Share; leave the drawer open → As iPhone XRR, in User Management, turn Lava Device’s Admin switch off and its Drive Post right off (View and Download on) → As Lava Device, without changing anything, press Save Access → If the server refuses: check a red message in words (not a code) shows and the Share drawer stays open → If the server accepts: note it
  - Expected: A save made after the posting right was taken away gets the server’s verdict: a refusal shows the server’s message and keeps the drawer open so nothing is lost. What the server answered is noted.

- **posting turned off while B’s Edit Info drawer is open: Save shows the server’s answer; a refusal keeps the drawer open @drive** — notrun (drive-live.spec.js:998)
  - Where: Tools → Drive → Shared with me → file ⋯ → Edit Info (Lava Device); Settings → Admin Settings → User Management (iPhone XRR) · account: iPhone XRR and Lava Device
  - Setup: Uses the png “e2e-drive-…-lv27-shared.png” shared with Lava Device at Edit.
  - Steps: As Lava Device, find the png under Shared with me, open ⋯ → Edit Info and type a description; leave the drawer open → As iPhone XRR, in User Management, turn Lava Device’s Admin switch off and its Drive Post right off (View and Download on) → As Lava Device, press Save → If the server refuses: check a red message in words (not a code) shows and the Edit drawer stays open → If the server accepts: note it
  - Expected: A rename or description change saved after the posting right was taken away gets the server’s verdict: a refusal shows the server’s message and keeps the drawer open. What the server answered is noted.

- **slot b is back to its admin flag and Drive rights from before this group @drive** — notrun (drive-live.spec.js:1030)
  - Where: Settings → Admin Settings → User Management (iPhone XRR) · account: iPhone XRR
  - Setup: Runs last in its group, only if a test in the group changed Lava Device’s rights.
  - Steps: Open User Management and find Lava Device → Put Lava Device’s Drive Post / View / Download switches and its Admin switch back to how they were before the group → Check the Admin switch and the three Drive switches match the values from before
  - Expected: Lava Device ends the group exactly as it started: same Admin flag, same Drive rights.

### Drive — live: B without the posting right (known gaps)

- **without the posting right, a page drop, drag-to-move, Info Copy link, Info version restore and Trash actions ask first and send nothing @drive** — notrun (drive-live.spec.js:1137)
  - Where: Tools → Drive (Lava Device, full-page layout); Settings → Admin Settings → User Management (iPhone XRR) · account: iPhone XRR and Lava Device
  - Setup: As Lava Device (with its usual rights), creates a folder “e2e-drive-…-lv-b-own” at its My Drive root holding a subfolder, a png, a text file saved twice, and two more pngs that it then deletes (they go to its trash). Then iPhone XRR, in User Management, turns Lava Device’s Admin switch off and its Drive Post right off (View and Download on).
  - Steps: Page drop (LV20): open Shared with me (top level), drag a png from the computer onto the Drive page; check a permission request appears and nothing uploads (if “Set File Permissions” opens instead, press Skip & Upload and note where it uploads and what the server says) → Drag-to-move (LV21): open My Drive → “…-lv-b-own”, drag the png row onto the subfolder row; check a permission request (or “You do not have permission to move this item”) appears and nothing moves → Copy link (LV22): open the text file’s ⋯ → Info, press Copy link → 24 hours; check a permission request appears and no link is made → Version restore (LV23): in the same Info panel, under Versions, press Restore on an older version and confirm; check a permission request appears and nothing is restored → Trash (LV24): View Trash, press Restore on “…-lv-b-trash-restore.png”, then Delete on “…-lv-b-trash-forever.png” (confirm); check each asks for permission first and nothing is restored or deleted
  - Expected: Without the Drive posting right, every way of adding, moving, re-sharing or restoring asks for the right and sends nothing, as Upload, Move to... and the menu’s Copy link already do. Known gaps: none of these five paths checks the posting right, so each sends its request; what the server answers to each is noted.

- **slot b is back to its admin flag and Drive rights from before this group @drive** — notrun (drive-live.spec.js:1303)
  - Where: Settings → Admin Settings → User Management (iPhone XRR) · account: iPhone XRR
  - Setup: Runs last in its group, only if a test in the group changed Lava Device’s rights.
  - Steps: Open User Management and find Lava Device → Put Lava Device’s Drive Post / View / Download switches and its Admin switch back to how they were before the group → Check the Admin switch and the three Drive switches match the values from before
  - Expected: Lava Device ends the group exactly as it started: same Admin flag, same Drive rights.

### Drive — transfers: permissions set at upload

- **the drawer’s File Permissions lists a folder member first at their role and sends a higher level in POST /uploads @smoke @drive** — notrun (drive-transfers.spec.js:396)
  - Where: Tools → Drive → My Drive, inside a folder → Upload · account: iPhone XRR
  - Setup: The test replaces My Drive with one fake folder, “e2e-stub-transfers”, makes the server say Lava Device already has that folder as Viewer, and makes the server refuse any upload, so nothing is created.
  - Steps: Open Drive → My Drive and open the folder → Click Upload and choose one png file → Open the “File Permissions (optional)” section → Check that Lava Device is the first person listed, with “Has access through this folder · Viewer” under the name → Check that the hint “People with access to this folder keep it on every file inside. You can give them more here, not less.” is shown → Check that Lava Device’s tick is on and cannot be changed, and the level reads View → Change Lava Device’s level to Download and check that the section says “1 user selected” → Click Upload → Check that the upload request names the open folder and gives Lava Device view and download, but not edit
  - Expected: Someone who already has the destination folder is listed first, at their folder role, and is never offered less; picking a higher level sends exactly that level with the upload.

- **“Set Permissions & Upload” on a page drop: a folder Editor is never offered less; Project Users sends everyone else @smoke @drive** — notrun (drive-transfers.spec.js:433)
  - Where: Tools → Drive → My Drive, inside a folder (drag and drop onto the page) · account: iPhone XRR
  - Setup: The test replaces My Drive with one fake folder, “e2e-stub-transfers”, makes the server say Lava Device already has that folder as Editor, and makes the server refuse any upload, so nothing is created.
  - Steps: Open Drive → My Drive and open the folder → Drag a png file from the computer onto the Drive page and drop it → Check that the “Set File Permissions” dialog says “1 file ready to upload” → Check that Lava Device is listed first, with “Has access through this folder · Editor”, ticked and locked, at Edit → Open Lava Device’s level list and check that View and Download are greyed out and Edit is not → Pick Edit again to close the list → Choose “Project Users” and note the number in “Applies to all N project users with Drive view access.” → Pick Download in the “Select permission” box → Click “Set Permissions & Upload” → Check that the upload request gives Download (view and download, no edit) to every listed person except Lava Device, and not to iPhone XRR itself
  - Expected: In the drop dialog, a folder Editor can only be given Edit; with Project Users at Download, everyone else gets Download and the Editor keeps the folder’s Edit (no weaker entry is sent for them).

- **a file uploaded with B at Download reaches B at exactly Download @drive** — notrun (drive-transfers.spec.js:485)
  - Where: Tools → Drive → My Drive, inside a test folder (iPhone XRR), then Tools → Drive → Shared with me (Lava Device) · account: iPhone XRR and Lava Device
  - Setup: Opens a private test folder in My Drive as iPhone XRR.
  - Steps: As iPhone XRR, click Upload and choose a png file “e2e-drive-…-tx03-download.png” → Open “File Permissions (optional)”, search for Lava Device and tick the name → Check that the level reads View, change it to Download, and check “1 user selected” → Click Upload and wait for the file to appear → Open the file’s ⋯ menu → Share and check that Lava Device holds the Download level (View and Download ticked, Edit not) → Close the Share drawer → As Lava Device, open Drive → Shared with me and search for the file → Open the file’s ⋯ menu and check that it offers exactly Preview, Download and Info (besides the favourites item)
  - Expected: A level picked in the upload drawer is applied with the upload: Lava Device gets the file at Download, no more (no Share or Edit Info) and no less.

### Drive — transfers: folder uploads whose folders cannot be made

- **a folder upload whose top folder is refused puts every file in the open folder and says so @drive** — notrun (drive-transfers.spec.js:540)
  - Where: Tools → Drive → My Drive, inside a test folder → Upload → Choose folder · account: iPhone XRR
  - Setup: Creates a folder “e2e-drive-…-tx04-fallback” inside a private test folder and opens it. The test then makes the server refuse every new folder.
  - Steps: Click Upload, then Choose folder, and pick the “drive-tree” folder (a/one.txt and a/b/two.txt) → Check that the drawer says 2 files → Click Upload → Check that a warning says “3 folders could not be created — those files will be placed at the destination root.” → Check that only one folder was attempted (“drive-tree”, in the open folder) and both files were sent to the open folder → Check that one.txt and two.txt are listed directly in the open folder and there is no “drive-tree” folder
  - Expected: When a folder upload’s top folder cannot be created, its subfolders are not attempted, the user is told, and every file still uploads into the folder that is open.

- **a dropped folder whose folders are refused uploads its files into the open folder @smoke @drive** — notrun (drive-transfers.spec.js:569)
  - Where: Tools → Drive → My Drive, inside a folder (drag and drop onto the page) · account: iPhone XRR
  - Setup: The test replaces My Drive with one fake folder, “e2e-stub-transfers”, and makes the server refuse every new folder and every upload, so nothing is created.
  - Steps: Open Drive → My Drive and open the folder → Drag a folder “e2e-drop-tree” (top.txt, and inner/one.txt) from the computer onto the page and drop it → Check that the “Set File Permissions” dialog says “2 files ready to upload” → Click “Skip & Upload” → Check that a warning says “2 folders could not be created — those files will be placed at the destination root.” → Check that only “e2e-drop-tree” was attempted, and both files were sent to the open folder with no permissions
  - Expected: A dropped folder whose folders cannot be made still uploads its files, into the open folder, after saying so.

- **uploading the same folder twice: a second tree or the fallback (recorded) @drive** — notrun (drive-transfers.spec.js:596)
  - Where: Tools → Drive → My Drive, inside a test folder → Upload → Choose folder · account: iPhone XRR
  - Setup: Creates a folder “e2e-drive-…-tx06-twice” inside a private test folder and opens it.
  - Steps: Click Upload → Choose folder, pick the “drive-tree” folder and click Upload → Check that folders drive-tree, a and b were created and both files uploaded → Upload the same “drive-tree” folder again the same way → Note what happens to the second “drive-tree”: either a second drive-tree folder appears (with new a and b inside), or a warning says “3 folders could not be created — those files will be placed at the destination root.” and one.txt and two.txt land directly in the open folder
  - Expected: Drive never reuses an existing folder for a folder upload. The outcome is recorded, not judged: two “drive-tree” folders if the server accepts the same name twice, otherwise the warning and the files in the open folder.

### Drive — transfers: synthetic drops (huge files, empty folders)

- **a page drop skips a file over 10 GB by name; exactly 10 GB still goes through @smoke @drive** — notrun (drive-transfers.spec.js:665)
  - Where: Tools → Drive → My Drive (drag and drop onto the page) · account: iPhone XRR
  - Setup: The test drops files whose size it fakes in the browser; no large file is made.
  - Steps: Open Drive → My Drive → Drop a file “e2e-big.zip” of 11 GB onto the page → Check that a warning says “1 file(s) skipped: e2e-big.zip (exceeds 10 GB)” and no “Set File Permissions” dialog opens → Drop “e2e-big.zip” (11 GB) together with “e2e-edge.zip” of exactly 10 GB → Check that the same warning names only e2e-big.zip, and the dialog says “1 file ready to upload” → Close the dialog without uploading → Check that no upload started
  - Expected: A page drop skips a file over 10 GB with a warning naming it, keeps the rest (10 GB exactly is allowed), and uploads nothing until the dialog is confirmed.

- **the drawer refuses an 11 GB file on Upload and sends nothing @smoke @drive** — notrun (drive-transfers.spec.js:694)
  - Where: Tools → Drive → My Drive → Upload (drag and drop into the drawer) · account: iPhone XRR
  - Setup: The test fakes the file’s size in the browser and makes the server refuse any upload, so nothing could be created even if one were sent.
  - Steps: Open Drive → My Drive and click Upload → Drop a file “e2e-big.zip” of 11 GB onto the drawer’s drop area → Check that the drawer lists it as “1 file selected”, showing 11.00 GB → Click Upload → Check that an error says “e2e-big.zip is too large. Maximum file size is 10GB.” → Check that no upload request was sent
  - Expected: The drawer accepts an 11 GB file into its list but refuses it on Upload with a clear error, and never starts the upload.

- **empty folders dropped on the drawer are created, and the drawer reopens empty @drive** — notrun (drive-transfers.spec.js:716)
  - Where: Tools → Drive → My Drive, inside a test folder → Upload (drag and drop into the drawer) · account: iPhone XRR
  - Setup: Creates a folder “e2e-drive-…-tx09-drawer-empty” inside a private test folder and opens it.
  - Steps: Click Upload → Drop two empty folders “e2e-drive-…-tx09-empty-a” and “…-tx09-empty-b” onto the drawer’s drop area → Check that the drawer says “2 empty folders selected” → Click Upload → Check that a message says “2 folders created” and both folders appear in the open folder → Click Upload again → Check that the drawer opens empty (no “empty folders selected” line) → Close the drawer
  - Expected: Empty folders dropped on the drawer are created in the open folder, and the next Upload starts with an empty drawer. Known bug: the drawer reopens still listing the two empty folders, and pressing Upload then creates nothing.

- **a page drop of a folder makes its empty subfolder too; empty folders alone say “2 folders created” @drive** — notrun (drive-transfers.spec.js:750)
  - Where: Tools → Drive → My Drive, inside a test folder (drag and drop onto the page) · account: iPhone XRR
  - Setup: Creates a folder “e2e-drive-…-tx10-page-drop” inside a private test folder and opens it.
  - Steps: Drop a folder “e2e-drive-…-tx10-tree” holding inside.txt and an empty folder “empty” onto the page → Check that the “Set File Permissions” dialog says “1 file ready to upload”, then click “Skip & Upload” → Check that the tree folder was created in the open folder, “empty” was created inside it, and inside.txt uploaded into the tree folder → Drop two empty folders “…-tx10-only-a” and “…-tx10-only-b” onto the page → Note what the dialog says, then click “Skip & Upload” → Check that a message says “2 folders created” and both folders appear in the open folder
  - Expected: A dropped folder keeps its structure, empty subfolders included; a drop of empty folders only creates them and says how many.

### Drive — transfers: the browser zip (stubbed)

- **two files and one without download: the browser zip shows progress, saves “Drive files (2).zip”, lists the third in _skipped.txt @smoke @drive** — notrun (drive-transfers.spec.js:908)
  - Where: Tools → Drive → My Drive, inside a folder → selection bar → Download · account: iPhone XRR
  - Setup: The test replaces My Drive with a fake folder holding three fake pdf files, the third without the download right; serves the files’ contents itself; answers the browser’s “choose where to save” dialog by keeping the zip in the page; and answers the Docs & Media Cabinet copies, so nothing is saved anywhere.
  - Steps: Open Drive → My Drive and open the folder → Tick all three files’ checkboxes and click Download in the selection bar → Check that “Zipping … of 2 files…” shows while the zip is built, and “Downloading 2 files (1 skipped — no download permission)” → In the save dialog, keep the suggested name “Drive files (2).zip” → Check that a message says “Saved “Drive files (2).zip” to the folder you chose” → Open the saved zip and check it holds the two downloadable files and a _skipped.txt naming the third file “(no download permission)” → Check that each of the two files was kept in the Docs & Media Cabinet, and the third was not
  - Expected: In Chrome, several files download as one zip built in the browser, with progress shown and a note of what was left out; the server zip is not used.

- **a cancelled save dialog does nothing; a failing one falls back to the server zip @smoke @drive** — notrun (drive-transfers.spec.js:953)
  - Where: Tools → Drive → My Drive, inside a folder → selection bar → Download · account: iPhone XRR
  - Setup: As DRV-TX11: a fake folder with three fake pdf files (the third without the download right), a faked save dialog, and the server zip and cabinet copies answered by the test.
  - Steps: Open the folder, tick all three files and click Download → Cancel the “choose where to save” dialog → Check that nothing is fetched, zipped, saved or kept in the cabinet → Click Download again; this time the save dialog fails to open → Check that Drive asks the server for one zip of the two downloadable files and the browser downloads a .zip → Check that a warning says “Not in the download (no download permission): ” and names the third file → Check that the two files were kept in the Docs & Media Cabinet
  - Expected: Cancelling the save dialog cancels the download quietly; if the dialog cannot be used, the server zip takes over and says which file was left out.

### Drive — transfers: refused downloads (stubbed)

- **a refused row-menu Download is reported, not silent @smoke @drive** — notrun (drive-transfers.spec.js:1022)
  - Where: Tools → Drive → My Drive → Files tab · account: iPhone XRR
  - Setup: The test replaces My Drive with one fake pdf file and makes the server refuse its download.
  - Steps: Open Drive → My Drive and click the Files tab → Open the pdf’s ⋯ menu and click Download → Check that no new tab opens and nothing is kept in the Docs & Media Cabinet → Check that an error message appears
  - Expected: A refused download tells the user so. Known bug: nothing happens at all — no download, no message.

- **a refused Download in the preview is reported, not silent @smoke @drive** — notrun (drive-transfers.spec.js:1045)
  - Where: Tools → Drive → My Drive → Files tab → a file’s preview · account: iPhone XRR
  - Setup: The test replaces My Drive with one fake png file, serves its preview, and makes the server refuse its download.
  - Steps: Open Drive → My Drive and click the Files tab → Hover the png’s row and click the eye (Open) button → Check that the preview shows the image → Click Download in the preview → Check that no new tab opens and nothing is kept in the Docs & Media Cabinet → Check that an error message appears
  - Expected: A refused download from the preview tells the user so. Known bug: the button does nothing and shows no message.

- **a refused folder ZIP shows the server’s message and downloads nothing @smoke @drive** — notrun (drive-transfers.spec.js:1074)
  - Where: Tools → Drive → My Drive → Folders tab · account: iPhone XRR
  - Setup: The test replaces My Drive with one fake folder and makes the server refuse its zip with the message “e2e_zip_refused”.
  - Steps: Open Drive → My Drive (Folders tab) → Open the folder’s ⋯ menu and click “Download as ZIP” → Check that an error message shows the server’s message → Check that nothing downloads
  - Expected: A refused folder zip shows the server’s reason as an error and downloads nothing.

### Drive — transfers: Docs & Media Cabinet copies (stubbed)

- **a row-menu Download keeps a cabinet copy under Drive and its folder @smoke @drive** — notrun (drive-transfers.spec.js:1118)
  - Where: Tools → Drive → My Drive, inside a folder · account: iPhone XRR
  - Setup: The test replaces My Drive with a fake folder “e2e-stub-transfers” holding one fake pdf, serves its download, and answers the Docs & Media Cabinet copy itself, so nothing is kept.
  - Steps: Open Drive → My Drive and open the folder → Open the pdf’s ⋯ menu and click Download → Check that the download opens in a new tab → Check that Drive asks the Docs & Media Cabinet to keep the file under Drive › e2e-stub-transfers, as that Drive file
  - Expected: Every Drive download also files a copy in the user’s Docs & Media Cabinet, under Drive and the folders the file sits in.

- **a version download keeps that version as its own cabinet entry, “(v2)” @smoke @drive** — notrun (drive-transfers.spec.js:1142)
  - Where: Tools → Drive → My Drive, inside a folder → a file’s Info panel → Versions · account: iPhone XRR
  - Setup: The test replaces My Drive with a fake folder holding one fake docx with a saved version 2, serves that version’s download, and answers the Docs & Media Cabinet copy itself, so nothing is kept.
  - Steps: Open Drive → My Drive and open the folder → Open the docx’s ⋯ menu and click Info → Under Versions, click the download button of the listed version → Check that Drive asks the Docs & Media Cabinet to keep that version under Drive › e2e-stub-transfers, named “e2e-stub-cabinet (v2).docx”
  - Expected: Downloading an earlier version keeps that version in the cabinet as its own entry, its name marked with the version number.

### Drive — transfers: video preview (stubbed)

- **an mp4 previews in a video with no download or playback-speed controls @smoke @drive** — notrun (drive-transfers.spec.js:1204)
  - Where: Tools → Drive → My Drive → Files tab → a video’s preview · account: iPhone XRR
  - Setup: The test replaces My Drive with one fake mp4 file and serves its preview link.
  - Steps: Open Drive → My Drive and click the Files tab → Hover the mp4’s row and click the eye (Open) button → Check that the preview shows a video player → Check that the player’s menu has no Download and no Playback speed, and picture-in-picture is off → Close the preview
  - Expected: A video previews in Drive’s own player without its download, playback-speed and picture-in-picture controls.

### Drive — B’s rights lowered in User Management

- **slot b is back to its admin flag and Drive rights from before this group @drive** — notrun (drive.spec.js:6880)
  - Where: Settings → Admin Settings → User Management → Lava Device’s posting rights (Tools → Drive) · account: iPhone XRR
  - Setup: Run after the other tests in this group. Before the first of them changed anything, the run noted whether Lava Device was an admin and its Drive Post, View and Download switches.
  - Steps: Open Settings, then Admin Settings, then User Management → Find Lava Device → Turn its Admin switch off if it is on → Open its posting rights and pick Tools → Set the Drive row’s Post, View and Download switches back to what they were before the group ran → Close the rights window → Turn the Admin switch back on only if Lava Device was an admin before → Check the Admin switch and the three Drive switches match the values noted before the group
  - Expected: Lava Device has exactly the admin flag and Drive rights it had before the rights tests began, so the rest of the suite sees it unchanged.

## Sheet bugs (3)

- **DRIVE-01** Folder Info shows "0 files, 0 folders" for a folder that has content — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. Create a folder with 2 subfolders and 2 files. 2. Open its ⋯ → Folder Info.
  - Expected: Items shows 2 files, 2 folders (or is hidden when unknown).
  - Actual: Items showed "0 files, 0 folders".

- **DRIVE-02** Favorites drill-in shows an empty breadcrumb bar — Low · Web: Not checked, Android: Not checked
  - Steps: 1. Add a folder to Favorites. 2. Open Favorites and drill into the folder.
  - Expected: A breadcrumb trail starting with Favorites.
  - Actual: The breadcrumb bar was empty.

- **DRIVE-03** Restoring a folder from Trash brings back only its direct files, not its subfolders — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. Move a folder with files and nested subfolders to Trash. 2. Restore the folder.
  - Expected: The folder comes back with all its content.
  - Actual: Only its direct files came back; the subfolders (and a file nested in them) stayed in Trash.
