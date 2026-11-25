# Web E2E — C&C

Source: Zillit E2E Report (generated 2026-11-17T10:27:04.399Z, env dev.zillit.com).

## Web test cases (34)

### CNC chat

- **chat list loads with tabs, filters and empty state** — passed (cnc.spec.js:97)
  - Where: Sidebar → C & C · account: iPhone XRR
  - Steps: Open C & C → Check that the Chat, Calls and Contacts tabs are shown → Check that the All, Unread, Members, Groups and Favorites filters are shown → Check that the chat area says “Click on any User or group to start conversation”
  - Expected: C & C opens with its three tabs, its five filters and the prompt to pick a chat.

- **Members and Groups filters show their card types** — passed (cnc.spec.js:114)
  - Where: Sidebar → C & C → Chat tab · account: iPhone XRR
  - Steps: Open C & C → Click the Members filter → Check that the first card shows a “Last Entry” line (a person) → Click the Groups filter → Check that the first card has no “Last Entry” line (a group)
  - Expected: Members lists people (with their last entry) and Groups lists group chats.

- **search filters the chat list** — passed (cnc.spec.js:127)
  - Where: Sidebar → C & C → Chat tab · account: iPhone XRR
  - Steps: Open C & C → Take the first chat name in the list that has at least 3 letters or digits → Type its first 4 letters in “Search User...” → Check that at least one listed chat contains those letters
  - Expected: Typing part of a name in the search box lists the matching chats.

- **Calls and Contacts tabs render** — passed (cnc.spec.js:148)
  - Where: Sidebar → C & C · account: iPhone XRR
  - Steps: Open C & C → Click the Contacts tab and check that people are listed → Click the Calls tab and check that it switches (it may be empty) → Click the Chat tab and check that chats are listed again
  - Expected: Contacts lists the project’s members, Calls opens, and Chat lists the chats again.

- **refresh chats re-renders the list** — passed (cnc.spec.js:165)
  - Where: Sidebar → C & C → Chat tab · account: iPhone XRR
  - Steps: Open C & C → Click the “Refresh chats” button → Check that the chat list shows its chats again
  - Expected: After Refresh chats the chat list is reloaded and shows chats.

- **open a personal chat and send a message** — passed (cnc.spec.js:175)
  - Where: Sidebar → C & C → Chat tab, Members filter, first person in the list · account: iPhone XRR
  - Steps: Open C & C → Click the Members filter and open the first person’s chat → Type “[e2e] personal …” in “Type a message” and click Send → Check that the message appears as sent by you → Check that the message box is empty again
  - Expected: The message is sent in the personal chat and the box clears.

- **copy a personal message to clipboard** — passed (cnc.spec.js:181)
  - Where: Sidebar → C & C → Chat tab, Members filter, first person in the list · account: iPhone XRR
  - Steps: Open C & C → Click the Members filter and open the first person’s chat → Type “[e2e] copy-me …” in “Type a message” and click Send → Hover the message and open its options arrow → Click Copy → Check that the toast says “Message copied to clipboard”
  - Expected: Copy on your own personal message copies it and confirms with a toast.

- **open a group chat and send a message** — passed (cnc.spec.js:192)
  - Where: Sidebar → C & C → Chat tab, Groups filter, first group in the list · account: iPhone XRR
  - Steps: Open C & C → Click the Groups filter and open the first group → Type “[e2e] group …” in “Type a message” and click Send → Check that the message appears as sent by you → Check that the message box is empty again
  - Expected: The message is sent in the group chat and the box clears.

- **edit an own group message** — passed (cnc.spec.js:198)
  - Where: Sidebar → C & C → Chat tab, Groups filter, first group in the list · account: iPhone XRR
  - Steps: Open C & C → Click the Groups filter and open the first group → Type “[e2e] edit-original …” in “Type a message” and click Send → Hover the message, open its options arrow and click Edit → In “Edit Message”, replace the text with “[e2e] edit-updated …” and click Update → Check that the message now shows the new text
  - Expected: Your own group message can be edited and shows the new text.

- **reply to an own group message** — passed (cnc.spec.js:216)
  - Where: Sidebar → C & C → Chat tab, Groups filter, first group in the list · account: iPhone XRR
  - Steps: Open C & C → Click the Groups filter and open the first group → Type “[e2e] reply-target …” in “Type a message” and click Send → Hover the message, open its options arrow and click Reply → In “Reply Message”, type “[e2e] reply-body …” and click Reply → Check that the reply appears as a new message from you
  - Expected: Replying to your own message posts the reply in the chat.

- **delete an own group message** — passed (cnc.spec.js:236)
  - Where: Sidebar → C & C → Chat tab, Groups filter, first group in the list · account: iPhone XRR
  - Steps: Open C & C → Click the Groups filter and open the first group → Type “[e2e] delete-me …” in “Type a message” and click Send → Hover the message, open its options arrow and click Delete → Click “Delete Chats”, then Yes → Check that the toast says “Messages Deleted” → Check that the message is gone from the chat
  - Expected: Your own message can be deleted and disappears from the chat.

- **search messages panel finds a sent message** — passed (cnc.spec.js:258)
  - Where: Sidebar → C & C → Chat tab, Groups filter, first group in the list · account: iPhone XRR
  - Steps: Open C & C → Click the Groups filter and open the first group → Type “[e2e] searchable …” in “Type a message” and click Send → Click the “Search Messages” button in the chat header → Type the same text in “Search Messages...” → Check that the message is listed in the results
  - Expected: Search Messages finds the message just sent.

- **media panel opens from the chat header** — passed (cnc.spec.js:273)
  - Where: Sidebar → C & C → Chat tab, Groups filter, first group in the list · account: iPhone XRR
  - Steps: Open C & C → Click the Groups filter and open the first group → Click the Media button in the chat header → Check that a panel with “Media” in it is shown
  - Expected: The Media button opens the chat’s media panel. (The check is loose: any text containing “media” passes.)

- **group header opens the members panel** — passed (cnc.spec.js:281)
  - Where: Sidebar → C & C → Chat tab, Groups filter, first group in the list · account: iPhone XRR
  - Steps: Open C & C → Click the Groups filter and open the first group → Click “View members” in the chat header → Check that the group’s members panel is shown
  - Expected: View members opens the group’s info panel with its members. (The check is loose: any text containing “members” passes.)

- **realtime: a message sent in one session appears live in another** — passed (cnc.spec.js:293)
  - Where: Sidebar → C & C → Chat tab, Groups filter, first group in the list, in two browsers · account: iPhone XRR
  - Steps: Sign in as iPhone XRR in two separate browsers → In both, open C & C, click the Groups filter and open the first group → In the first browser, type “[e2e] realtime …” and click Send → In the second browser, without reloading, check that the message appears
  - Expected: A message sent in one session shows up live in another session of the same account.

- **theme toggle switches between light and dark** — passed (cnc.spec.js:326)
  - Where: Sidebar → C & C → Chat tab, Groups filter, first group in the list · account: iPhone XRR
  - Steps: Open C & C → Click the Groups filter and open the first group → Read the theme button’s label (“Switch to Light mode” or “Switch to Dark mode”) → Click the theme button → Check that its label has changed to the other mode
  - Expected: The theme button switches C & C between light and dark.

- **upload a single image to a group chat** — passed (cnc.spec.js:377)
  - Where: Sidebar → C & C → Chat tab, Groups filter, first group in the list · account: iPhone XRR
  - Steps: Open C & C → Click the Groups filter and open the first group → Count your sent messages once the chat has finished loading → Click the paper-clip and choose Image Upload → Pick the image red.png → Click Upload in the preview → Check that one more sent message appears
  - Expected: The image is sent to the group as one new message.

- **upload multiple images at once** — passed (cnc.spec.js:388)
  - Where: Sidebar → C & C → Chat tab, Groups filter, first group in the list · account: iPhone XRR
  - Steps: Open C & C → Click the Groups filter and open the first group → Count your sent messages once the chat has finished loading → Click the paper-clip and choose Image Upload → Pick the two images red.png and blue.png together → Click Upload in the preview → Check that two more sent messages appear
  - Expected: Two images picked together arrive as two new messages.

- **upload a document** — passed (cnc.spec.js:402)
  - Where: Sidebar → C & C → Chat tab, Groups filter, first group in the list · account: iPhone XRR
  - Steps: Open C & C → Click the Groups filter and open the first group → Click the paper-clip and choose Document Upload → Pick sample.pdf → Click Upload in the preview → Check that a sent message named “sample.pdf” appears
  - Expected: The PDF is sent to the group and shows its file name.

- **upload an audio file** — passed (cnc.spec.js:413)
  - Where: Sidebar → C & C → Chat tab, Groups filter, first group in the list · account: iPhone XRR
  - Steps: Open C & C → Click the Groups filter and open the first group → Count your sent messages once the chat has finished loading → Click the paper-clip and choose Audio Upload → Pick sample.wav → Click Upload in the preview → Check that one more sent message appears
  - Expected: The audio file is sent to the group as one new message.

- **send current location** — passed (cnc.spec.js:424)
  - Where: Sidebar → C & C → Chat tab, Groups filter, first group in the list · account: iPhone XRR
  - Setup: The test browser shares a fixed location (New Delhi).
  - Steps: Open C & C → Click the Groups filter and open the first group → Count the location messages (map links) once the chat has finished loading → Click the paper-clip and choose Location Upload → Wait until the map has drawn → Click SELECT → Check that one more location message appears
  - Expected: Your current location is sent to the group as a map link.

- **mark a member as favorite and unmark** — flaky (cnc.spec.js:446)
  - Where: Sidebar → C & C → Chat tab (Favorites filter) and Contacts tab · account: iPhone XRR
  - Steps: Open C & C, click the Favorites filter and count the favourites → Click the Contacts tab → Click the empty star of the first contact who is not a favourite yet (try the next one, up to four, if the star does not fill) → Back on the Chat tab, click Favorites and check there is one more favourite, that contact → On the Contacts tab, click the same contact’s filled star → On the Chat tab, click Favorites and check the count is back to the first number
  - Expected: Starring a contact adds them to Favorites, and unstarring removes them again.
  - Web error: Error: expect(locator).toHaveCount(expected) failed

- **search a user in the contacts list** — passed (cnc.spec.js:503)
  - Where: Sidebar → C & C → Contacts tab · account: iPhone XRR
  - Steps: Open C & C and click the Contacts tab → Take the first 4 letters of the first contact’s name → Type them in the Contacts “Search User...” box → Check that the first listed contact contains those letters
  - Expected: Searching Contacts by part of a name lists the matching person first.

- **edit an own personal message** — passed (cnc.spec.js:517)
  - Where: Sidebar → C & C → Chat tab, Members filter, first person in the list · account: iPhone XRR
  - Steps: Open C & C → Click the Members filter and open the first person’s chat → Type “[e2e] p-edit-original …” in “Type a message” and click Send → Hover the message, open its options arrow and click Edit → In “Edit Message”, replace the text with “[e2e] p-edit-updated …” and click Update → Check that the message now shows the new text
  - Expected: Your own personal message can be edited and shows the new text.

- **forward media from the media panel** — passed (cnc.spec.js:537)
  - Where: Sidebar → C & C → Chat tab, Groups filter, first group in the list, Media panel · account: iPhone XRR
  - Setup: Sends the image red.png to the group (paper-clip → Image Upload → Upload).
  - Steps: Click the Media button in the chat header → On the first media card, click its forward icon → Choose “Forward In App” → In the forward dialog, choose the group option → In “Select Group”, click the group “test 001” (and Send, if a Send button is shown) → Check that a toast says “Forward Successfully”
  - Expected: An image from the Media panel can be forwarded to another group chat. Known bug: freshly uploaded media often does not show in the Media panel, and the test then skips.

- **share media from the media panel opens email compose** — passed (cnc.spec.js:597)
  - Where: Sidebar → C & C → Chat tab, Groups filter, first group in the list, Media panel · account: iPhone XRR
  - Setup: Sends the image blue.png to the group (paper-clip → Image Upload → Upload).
  - Steps: Click the Media button in the chat header → On the first media card, click its forward icon → Choose Share → Check that the email compose window opens
  - Expected: Share on a media item opens the email compose window (nothing is sent). Known bug: freshly uploaded media often does not show in the Media panel, and the test then skips.

- **group admin flow: rename room, promote member, leave** — passed (cnc.spec.js:637)
  - Where: Sidebar → C & C → Chat tab, Create Group button · account: iPhone XRR
  - Setup: Creates a group “[e2e] adm …” with one other member: Create Group button → “Create New Group”, Group Name, pick the first person in Select Users, Submit.
  - Steps: Click the Groups filter, search for the new group and open it → Click “View members”, then “Edit name” → Type “[e2e] ren …” and click the tick → Check that the new name is shown → Open the other member’s ⋮ menu, click “Make Admin” and answer Yes → Check that two members are now marked Admin → Click “Leave Group” and confirm with OK → Check that the group is gone from your chat list
  - Expected: A group admin can rename the group, make another member admin, and then leave it.

- **group lifecycle: create, add member, leave** — passed (cnc.spec.js:711)
  - Where: Sidebar → C & C → Chat tab, Create Group button · account: iPhone XRR
  - Steps: Click the Create Group button → In “Create New Group”, type “[e2e] grp …” as the Group Name, pick the first person in Select Users and click Submit → Click the Groups filter and open the new group → Click “View members”, then “Add members” → Pick one person who is not in the group yet and click Save → Check that the Add members panel closes → Click “Leave Group”, or the bin (delete group) button when you are the only admin, and confirm with OK → Check that the group is gone from your chat list
  - Expected: A new group can be created, a member added to it, and the group left or deleted.

### Sheet bugs — C&C

- **the Groups filter clears its loading state and stays usable when the project has no groups (stub) @smoke** — passed (sheet-bugs/cnc.spec.js:274)
  - Where: Sidebar → C & C → Chat tab → Groups filter (a project with no chat groups) · account: iPhone XRR
  - Setup: The test makes the chat list come back without any group chats, as in a project that has no groups.
  - Steps: Open C & C in a project with no chat groups → Click the Groups filter → Check that the loading placeholders go away, the search box can be used and no chats are listed → Click All and check that chats are listed → Click Groups again and check that nothing is listed and the Create Group button is still shown → Click “Refresh chats” and wait for the list to reload → Check that the loading placeholders go away again and Groups still lists nothing → Click Members and check that people are listed
  - Expected: With no groups, the Groups filter finishes loading and the list stays usable, also after a refresh.

- **the Groups filter shows an empty state when the project has no groups (stub) @smoke** — suspected · known bug AllChats.jsx renders an empty state for the Favourites filter only (ZL-17468); with no groups the Groups filter is a blank Virtuoso list with no message,AllChats.jsx renders an empty state for the Favourites filter only (ZL-17468); with no groups the Groups filter is a blank Virtuoso list with no message,AllChats.jsx renders an empty state for the Favourites filter only (ZL-17468); with no groups the Groups filter is a blank Virtuoso list with no message (sheet-bugs/cnc.spec.js:311)
  - Where: Sidebar → C & C → Chat tab → Groups filter (a project with no chat groups) · account: iPhone XRR
  - Setup: The test makes the chat list come back without any group chats, as in a project that has no groups.
  - Steps: Open C & C in a project with no chat groups → Click the Groups filter → Wait until the loading placeholders go away → Check that no chats are listed → Check that the list shows an empty-state message (such as “no groups” or “create a group”)
  - Expected: With no groups, the Groups filter shows an empty-state message. Known bug: the list is just blank, with no message.
  - Web error: Error: expect(locator).toBeVisible() failed

- **the Chat list keeps its full height after a chat is opened from search** — passed (sheet-bugs/cnc.spec.js:336)
  - Where: Sidebar → C & C → Chat tab → All filter · account: iPhone XRR
  - Steps: Open C & C and click the All filter → Note how far down the column the chat list reaches → Click in “Search User...” and type the first name of Lava Device → Open Lava Device’s chat from the results → Go back to the list, clearing the search box if it still holds the text → Check that the list fills its column as before, with no extra blank space under the last chat
  - Expected: After a chat is opened from search, the chat list keeps its full height with no blank area left at the bottom.

- **the Contacts list keeps its full height after a chat is opened from its search** — passed (sheet-bugs/cnc.spec.js:369)
  - Where: Sidebar → C & C → Contacts tab · account: iPhone XRR
  - Steps: Open C & C and click the Contacts tab → Note how far down the column the contacts list reaches → Click in “Search User...” and type the first name of Lava Device → Open Lava Device’s chat from the results → Go back to the list and clear the search box (Contacts keeps the search by design) → Check that the list fills its column as before, with no extra blank space under the last contact
  - Expected: After a chat is opened from the Contacts search, the contacts list keeps its full height with no blank area left at the bottom.

- **a New Group name longer than 25 characters is kept and the limit is shown @smoke** — passed (sheet-bugs/cnc.spec.js:398)
  - Where: Sidebar → C & C → Create Group button → Create New Group · account: iPhone XRR
  - Steps: Click the Create Group button to start a new group → In Group Name, type “[e2e] cnc-03 xxxxxxxxxxxxxxxxx” (30 characters) → Check that all 30 characters are kept and the message under the field names the 25-character limit → Clear the field and paste the same name → Check again that nothing is cut and the limit message shows → Shorten the name to exactly 25 characters and check that the limit message goes away → Click Cancel
  - Expected: A group name over 25 characters is not cut silently: the field keeps the text and says the limit is 25.

- **renaming a group past 25 characters keeps the text or shows the limit (Group Info name editor)** — suspected · known bug InfoSiderGroup.jsx inline name Input has maxLength={25} and no showCount, so typing or pasting stops at 25 with no hint (the same symptom as the iOS New Group field),InfoSiderGroup.jsx inline name Input has maxLength={25} and no showCount, so typing or pasting stops at 25 with no hint (the same symptom as the iOS New Group field),InfoSiderGroup.jsx inline name Input has maxLength={25} and no showCount, so typing or pasting stops at 25 with no hint (the same symptom as the iOS New Group field) (sheet-bugs/cnc.spec.js:445)
  - Where: Sidebar → C & C → Chat tab → Groups filter → the e2e group → View members → Edit name · account: iPhone XRR
  - Setup: Creates a group “[e2e] c03 …” whose only other member is Lava Device (Create Group button → Create New Group → Submit).
  - Steps: Open the new group from the Groups filter and click “View members” → Click “Edit name” and clear the name field → Type “[e2e] cnc-03 xxxxxxxxxxxxxxxxx” (30 characters) → Check that the field keeps all 30 characters, or shows that the limit is 25 → Clear the field and paste the same name → Check again that the name is kept in full or the limit is shown
  - Expected: Renaming a group past 25 characters either keeps the text or tells you the limit is 25. Known bug: typing or pasting stops at 25 characters with no hint.
  - Web error: Error: typing 30 characters left "[e2e] cnc-03 xxxxxxxxxxxx" (25) with no limit shown

## Sheet bugs (3)

- **CNC-01** Group tab shows a loading spinner that never goes away when the project has no groups — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. Use a project with no chat groups (or delete the last group from C&C → Group). 2. Stay on the Group tab while the list refreshes (after Delete Group, or when a message arrives).
  - Expected: The list shows its empty state and stays usable.
  - Actual: A full-screen orange spinner stays for 2+ minutes and blocks taps on the segments. Switching to another tab and back clears it. The group itself was deleted.

- **CNC-02** Contacts list keeps a keyboard-sized blank area after opening a chat with the keyboard up — Low · Web: Not checked, Android: Not checked
  - Steps: 1. In C&C, search with the software keyboard up. 2. Open a chat from the results. 3. Go back to the list.
  - Expected: The list fills the screen with no extra blank space.
  - Actual: A keyboard-sized blank area stayed at the bottom of the list.

- **CNC-03** Group name is cut at 25 characters without any message — Low · Web: Not checked, Android: Not checked
  - Steps: 1. Start a new group. 2. Type or paste a name longer than 25 characters.
  - Expected: Decided by product: a longer name is allowed, or the limit is shown.
  - Actual: Typing stops at 25 characters with no hint, so longer names are cut.
