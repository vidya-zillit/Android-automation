---
name: zillit-two-device-roles
description: "Which device plays which role in two-device approval suites (Call Sheet, Production Report)"
metadata:
  node_type: memory
  type: project
  originSessionId: b1614600-558f-4f70-91b2-a2665802f9c5
  modified: 2026-11-23T09:37:20.193Z
---

For two-device approval flows (Call Sheet Creation, Production Report Creation, set 2026-11-23):

- **Pixel 7 (`34281FDH2005XC`, USB) = approver.**
- **Emulator (`emulator-5554`, AVD `Medium_Phone_API_37.0`) = creator** — creates, edits, sends for approval/signature.

Accounts (seen 2026-11-23): emulator = **Vidya Testing** (Production coordinator, admin);
Pixel = **Vidya Pixel** (was Screen writer; **department changed to Catering / caterer on 2026-11-23** by the user; also a project **admin** on ZL-Android-Automation). As the caterer it is the Catering-side user, and it unblocks Catering chat posting (which needs a department member). They are
the project's only two members. Vidya Pixel is the approver + project default approver on both
tools (user approved). Developer options → Stay awake was turned ON on the Pixel 2026-11-23 (`settings get global stay_on_while_plugged_in` = 15); if it reads 0 the Pixel will lock — ask the user to unlock. Never enter the Pixel PIN, even if the user offers it. The Zillit app on the Pixel can also show a fingerprint/PIN App Lock at launch (seen 2026-11-23): suites then loop open→close with every case skipping — stop the run and ask the user to unlock. It appears on any COLD start of the app on the Pixel, so suites must never terminate/force-stop/relaunch the app there (recover with BACK/activateApp only).

Both run the `dev` flavour. The emulator is not always running; boot it with
`~/Library/Android/sdk/emulator/emulator -avd Medium_Phone_API_37.0`.

**Why:** the user assigned these roles; approvals need a second account on a second device.
**How to apply:** wire creator/approver UDIDs this way (like ZILLIT_SENDER_UDID / ZILLIT_RECEIVER_UDID in --two-device) and never swap them without asking. See [[zillit-automation-local-env]], [[zillit-device-run-stability]].
