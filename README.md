# Zillit Automation

Standalone Appium UI-automation project for the Zillit Android app
(drives the installed APK from outside — the app repo stays untouched).

## Stack

- Java 17 + Gradle + JUnit 5
- Appium 2 (local install via npm) + UiAutomator2 driver
- Page Object Model under `src/test/java/com/zillit/automation/pages`

## One-time setup

```bash
npm install            # installs Appium + UiAutomator2 driver locally
```

## Running from Android Studio (one click)

Nothing to start by hand — no emulator window, no Appium terminal, no `adb` commands.

1. Open the project in Android Studio and let the Gradle sync finish.
2. Open the **run-configuration dropdown** in the toolbar. The Zillit entries are
   committed in `.run/` and show up on their own:

   | Configuration | Module flag | What it runs |
   |---|---|---|
   | `Zillit ▸ Home` | `--home` | chat, posting rights, media send/preview, call sheet, calendar |
   | `Zillit ▸ Chat` | `--chat` | every chat suite, no calendar |
   | `Zillit ▸ Calendar` | `--calendar` | calendar CRUD, all pickers |
   | `Zillit ▸ Email` | `--email` | email module |
   | `Zillit ▸ Comms and Collaboration` | `--cc` | C&C module |
   | `Zillit ▸ Tools` | `--tools` | tools module |
   | `Zillit ▸ Recce` | `--recce` | recce module |
   | `Zillit ▸ Drive (full)` | `--drive-all` | the whole Drive sweep |
   | `Zillit ▸ Document Distribution` | `--doc-dist-all` | all Document Distribution content suites |
   | `Zillit ▸ Admin Settings` | `--admin-settings` | Settings → Admin Settings |
   | `Zillit ▸ Settings tab` | `--settings` | the Settings tab |
   | `Zillit ▸ ALL modules` | `--all-in-project` | every module, one combined report |

3. Press **▶ Run**. That is the whole procedure.

Each run does the following by itself, and skips any step already done:

1. resolves `adb` and the emulator from the SDK — no hardcoded paths;
2. runs `npm install` if Appium is not installed yet;
3. finds a device, **booting an AVD when nothing is attached** and waiting for
   `sys.boot_completed` rather than just for `adb` to notice it;
4. grants `io.appium.settings` its location permissions — on Android 14+ its
   foreground service is location-typed and silently refuses to start without
   them, which is what produces the misleading
   *"Appium Settings app is not running after 5000ms"* on real phones;
5. starts the Appium server if it is not already listening, logging to
   `build/appium.log`;
6. pushes the media fixtures and grants the app's permissions;
7. runs the suites, builds the HTML report and shares it.

The same thing from a terminal:

```bash
./run-ui.sh --home            # one module
./run-ui.sh --all-in-project  # everything, already logged in
./run-ui.sh --help            # the module list
```

### Choosing the build (QA / DEV / PROD)

Every flavour is a separate app with its own applicationId, so switching is one
variable — and because **resource ids are prefixed by the applicationId**, that
same variable decides what every locator in the suite resolves against:

| `ZILLIT_FLAVOR` | Package | Notes |
|---|---|---|
| `dev` | `com.zillit.zillitapp.dev` | develop flavour |
| `qa` | `com.zillit.zillitapp.qa` | the config default |
| `stg` | `com.zillit.zillitapp.stg` | staging builds, where installed |
| `prod` | `com.zillit.zillitapp` | **also needs `ZILLIT_ALLOW_PROD=1`** |

```bash
ZILLIT_FLAVOR=dev ./run-ui.sh --home
```

The launcher activity does **not** change with the flavour — `.splash.SplashActivity`
resolves against the manifest namespace, which stays `com.zillit.zillitapp` for all
of them — so `zillit.appActivity` is correct as it is and needs no edit.

Prod is gated deliberately. These suites send messages, replace documents and can
clear a Call Sheet unit; on prod that lands in a live production.

To change the default permanently, edit `zillit.appPackage` in
`config/config.properties`.

Before the run starts, the chosen flavour is checked against what is actually
installed on the chosen device. If it is missing you get the list of Zillit builds
that *are* there, instead of an hour of locator timeouts that look like "no project".

### Choosing which device to run on

See everything attached and what is installed on each:

```bash
./run-ui.sh --devices
```

```
  34281FDH2005XC  —  Pixel 7, Android 17
      ZILLIT_FLAVOR=dev   com.zillit.zillitapp.dev    Zillit_32535-dev
      ZILLIT_FLAVOR=qa    com.zillit.zillitapp.qa     Zillit_22533-qa
```

Then:

```bash
ZILLIT_UDID=34281FDH2005XC ZILLIT_FLAVOR=qa ./run-ui.sh --home
```

**For a physical device the only requirements are:** USB debugging on, the device
authorised (`adb devices` shows `device`, not `unauthorized`), the flavour you want
installed, and that build **logged in with a project you can post in**. The location
grant that Android 14+ needs for Appium's helper app is applied automatically.


### Device selection rules

| Situation | What happens |
|---|---|
| Nothing attached | the AVD boots automatically (`ZILLIT_AVD` picks which one) |
| Exactly one device attached | that one is used |
| Several attached | `zillit.udid` in `config/config.properties` decides |
| `ZILLIT_UDID` set | that one wins, and the run fails fast if it is not attached |

It never guesses between two devices without being told, because guessing wrong
means posting real chat messages into a project on the wrong phone.

```bash
ZILLIT_UDID=34281FDH2005XC ./run-ui.sh --home   # a specific phone
ZILLIT_AVD=Medium_Phone_API_37.0 ./run-ui.sh --home
```

To change the default permanently, edit `zillit.udid` in `config/config.properties`.

### Sharing the results

Every run ends with a **self-contained HTML report**, kept per run instead of
overwritten:

```
build/report/zillit-home-20261117-1543.html
```

One file, no server, no assets — attach it to a chat or an email as is. Each
failure is classified (environment vs. real app/test issue) with a plain-English
explanation and a next step. It opens automatically when the run finishes; set
`ZILLIT_NO_OPEN=1` to stop that.

When `zillit.sheetWebhook` is set in `config/config.properties` (one-time setup
in `tools/google-apps-script.gs`), the same run also appends **one row per test
case** to the team Google Sheet — name, module, status, failure reason — so the
sheet is the running test-case log. Set `ZILLIT_SHARE=0` to skip that for a run.

## Running the tests (manual runbook)

**Terminal 1 — build the app** (only when the app code changed):
```bash
cd "$HOME/Android Automation using Claude/Zillit-Android"
./gradlew assembleDevelopDebug
```

**Terminal 2 — emulator** (or start Pixel_10_Pro from Android Studio's Device Manager):
```bash
~/Library/Android/sdk/emulator/emulator -avd Pixel_10_Pro
```
Wait until Android is fully booted (home screen visible).

**Terminal 3 — Appium server** (leave it running):
```bash
cd "$HOME/Android Automation using Claude/Android-automation"
ANDROID_HOME=$HOME/Library/Android/sdk npm run appium
```

**Terminal 4 — run everything and report to the Google Sheet:**
```bash
cd "$HOME/Android Automation using Claude/Android-automation"
./run-and-report.sh            # full suite — watch the emulator, type the OTP when the sheet opens
./run-and-report.sh --no-e2e   # unattended run without the OTP/E2E test
```

Results: Google Sheet "Test Reports" tab (developers see failures in red),
HTML report at `build/reports/tests/test/index.html`, screenshots in
`build/screenshots/`.

Notes
- Entering a project triggers permission prompts (an in-app "important
  Notifications" sheet, then Android's system notification/location dialogs).
  The framework auto-dismisses all of them (`dismissPermissionPrompts()`), so
  no manual clicking is needed during a run.
- Tests are pinned to the emulator via `zillit.udid=emulator-5554` in
  `config/config.properties` — a plugged-in phone won't be touched. To test on
  a real device instead, set `zillit.udid` to its serial from `adb devices`.
- If tests can't start: check the Appium terminal is still running and shows
  no errors, and `adb devices` lists `emulator-5554` as `device`.
- The E2E test creates a real project on the dev backend each time it passes.

## Configuration

Defaults live in [config/config.properties](config/config.properties); every key can be
overridden per-run with a system property or environment variable:

```bash
# Submit a real project code in the join flow
./gradlew test -Dzillit.projectCode=ABC123

# Test an already-installed build instead of reinstalling the APK
./gradlew test -Dzillit.apkPath= -Dzillit.appPackage=com.zillit.zillitapp.dev
```

## Current coverage

Scope: **Create Project** and **Join Project** flows only.

**JoinProjectTest** (7 cases)
1. All screen elements visible (code field, terms, Next, QR option)
2. Empty code → "Project Code Cannot be Empty"
3. Whitespace-only code → treated as empty
4. Code without accepting terms → terms error
5. Invalid code → server rejection, stays on screen
6. 184-char special-character code → no crash, no navigation
7. Back button returns to Start & Join

**CreateProjectValidationTest** (20 cases — mirrors the app's validation ladder;
all project creation uses the Entertainment type, never Personal Communication)
1. Form renders end-to-end (scroll to Submit)
2. Verify tap without language (masks email error)
3. Empty form → "All Fields are required"
4–6. First name: too short / too long / special characters
7–9. Last name: empty / too short / special characters
10. Missing project type
11. Entertainment without sub-type
12. Sub-type (Feature) selected, missing language
13–15. Project name: empty / under 3 chars / over 25 chars
16–18. Email: empty / malformed / valid-but-unverified
19–20. Verify tap with empty / malformed email (language selected)

**CreateProjectSubTypeTest** (3 cases, skipped if the backend lacks the type)
1. Entertainment without sub-type → error
2. "Other" with sub-type under 3 chars → error
3. Personal hides the sub-type field

**HomeChatTest** (13 cases — inside a project, needs posting rights; runs via
`--home`). Units strip + per-unit chat:
units listed / select unit → chat opens; send validation (empty keeps Send
hidden, whitespace not sendable, typing reveals Send, valid message posts,
>2000 chars → limit error); attachment picker offers ALL 8 media types
(Photo/Gallery/Video/Video Gallery/Audio/Document/Location/Contact) and each
type opens its next screen without crashing / cancel sends nothing; reply via
long-press menu → preview → cancel, and reply-to-own-message posts. Sends real
messages tagged `qa-auto-…`.

Note: HomeChatTest verifies every type is present and each route opens. Full
send + editing + post-send is automated for the app's-own-UI types in
**MediaPreviewTest** (`--home`); the system-picker/camera types are covered by
[MANUAL_MEDIA_CHECKLIST.md](MANUAL_MEDIA_CHECKLIST.md).

**MediaPreviewTest** (8 cases — image preview/editor + post-send; needs a chat
unit with posting rights and an image on the device — the runner pushes one):
preview opens (caption/edit/send); caption accepts text; editor exercises the
three tools (Paint/Crop/Add-Text — **no rotate exists**) with draw+apply, text,
crop, undo, save; send posts with loader + media bubble; tapping opens the
viewer; download control (received media); reply-on-media; and Video Gallery
send (skips if no video on device). Fixtures in src/test/resources/fixtures.

**CallSheetTest** (6 cases — the call-sheet unit's document-upload popups; runs
via `--home`). On a call-sheet unit with existing content, tapping attachment
shows the first confirmation popup (Continuation / New / Cancel): popup appears,
Cancel opens nothing, **Continuation → document-only picker**; New → the
replace-warning popup (No / Yes): No cancels, **Yes → document-only picker**.
Skips when the project has no call-sheet unit or it is empty.

**CalendarTest** (13 cases — verified live against the real new-calendar UI):
calendar screen loads (+ auto-allows the calendar-permission popup); month
next/prev/Today navigation; event form shows **all** fields (title, color,
full-day, start/end dates+times, timezone, repeat, description, location,
call-type, invitees, Create); form **defaults populated** (today, times,
timezone, "Audio Call"); validation ladder (empty/short title, members-no-
invitees, recurring-no-end) asserted via **save-blocked / form-stays-open**
(Android 12+ hides Toasts from automation); Repeat dialog options; all-day
hides time pickers; Personal type; description accepts text; back discards.

**CalendarTest** (26 cases — comprehensive, verified against the real UI):
screen + month/week navigation; all form fields + populated defaults; validation
ladder (empty/short title, members-no-invitees, recurring-no-end) via save-blocked
signal; **all pickers** (call-type, reminder, timezone search+select, invitee
picker, colour, date, time) + Repeat dialog; **successful Personal event create →
appears on calendar → view detail → edit → delete**; week-view toggle; Events /
Join Call quick actions. Errors are Toasts (hidden from automation on Android 12+),
so validation asserts on save-blocked + navigation, not toast text.

Legacy note — the older 13/9-case descriptions are superseded. **CalendarTest** (older 13-case block — Home "Calendar" unit → new-calendar event form; runs
via `--home`). Calendar loads (month grid + FAB); next/prev month navigation;
event-form validation ladder (empty title / <3 chars → "Title must be at least
3 characters"; members event with no invitees blocked; recurring event without
a repeat-end blocked); Personal vs Members type; All-day hides time pickers;
back discards. Errors are Android Toasts.

**ProjectListTest** (7 cases — needs a logged-in app with projects; every case
skips with instructions otherwise. Runs after the E2E.)
1. List shows project rows, search, Start/Join buttons
2. Search filters by name
3. Non-matching search shows empty state
4. Clearing search restores the list
5. Tapping a project enters it (toolbar shows the project name)
6. "Change Project" switches to another project from inside the app
7. Relaunch lands back on the list (multi-project account)

**JoinProjectE2ETest** (14 cases — joins with the REAL project code from
`zillit.projectCode` in config, currently 983699; clear it to enter the code
manually on the emulator instead)
1. Valid code opens the user-details page
2. Empty form flags first name (the "All Fields required" branch is unreachable — app quirk)
3–7. First/last name: length + special-character rules
8. Missing department
9. Designation tapped before department → "select department first"
10–11. Missing designation / missing unit
12–13. Malformed optional email / phone without country code
14. Fully valid details reach the take-a-selfie dialog
    (the selfie + final submission stay manual — camera/face recognition)

**CreateProjectE2ETest** (1 case, creates real backend data)
- Full form → send OTP to `vidyasagar+01@zillit.com` → **human enters OTP on
  the emulator (up to 5 min)** → verified badge → editing email resets
  verification (and restoring it restores the badge) → storage-code special
  chars rejected → phone without country code rejected → submit without terms
  rejected → accept terms → project created (Share Project Code screen).

## Google Sheet reporting (no Claude needed)

One-time setup (~3 min):
1. Open the team Google Sheet → **Extensions → Apps Script**.
2. Paste the contents of [tools/google-apps-script.gs](tools/google-apps-script.gs) and
   **Deploy → New deployment → Web app** (Execute as: *Me*, Access: *Anyone with the link*).
3. Copy the `/exec` URL into `config/config.properties` as `zillit.sheetWebhook=…`.

Then a single command runs everything and reports for the developers:

```bash
./run-and-report.sh            # full suite (E2E pauses for manual OTP entry)
./run-and-report.sh --flow     # ONLY the ordered journey: create -> join -> select
./run-and-report.sh --no-e2e   # validation suites only, fully unattended
```

### The happy-path flow (`--flow`)

`ProjectFlowE2ETest` runs the real user journey on ONE app session, in order:
**1. Create a project → 2. Join project 983699 → 3. Select a project.**
The app launches once; permission popups are allowed automatically throughout.
Only step 1 pauses (up to 5 min) for you to type the email OTP on the device.
This is the flow to watch when you want to see create/join/select actually
exercised end to end (the validation suites only check error messages).

The sheet gets a color-coded **Test Reports** tab: a summary row per run
(green = all passed, red = failures) plus one row per test case with the
failure message developers need. `node tools/report-to-sheet.mjs` can also be
run alone to re-push the latest results.

## Adding flows

Create a page object per screen in `pages/<module>/` (locators use the app's view IDs,
prefix `com.zillit.zillitapp:id/`), then compose them in a test under `tests/<module>/`.
Each module has its own package on both sides, e.g. `tests.continuity` and
`pages.continuity`. Shared bases and screens used by several modules (`BasePage`,
`VisiblePage`, `BottomNavPage`, `ToolsPage`, `ProjectListPage`, the popup and file
picker pages) live in `pages.common`. The runner selects suites by simple class name
(`--tests '*.ContinuityTest'`), so a suite can move package without touching it.
