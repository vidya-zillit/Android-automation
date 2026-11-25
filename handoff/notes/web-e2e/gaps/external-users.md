# External Users: sheet bugs vs Android suite

Web input: `external-users.md` (0 web tests, 2 sheet bugs).
Android suites: `tests/externalusers/ExternalUserTest.java` (EU-01..41) and `ExternalUserDropdownProbeTest`.
App source: `Zillit-Android/.../bottomNav/settings/activity/AddUpdateExternalUserPage.kt`, called `AUEUP.kt` below.

## Summary

| Bug | Title | Status | Android case / note |
|---|---|---|---|
| EXT-01 | Reserved Additional Info titles accepted in another case; email auto-capitalises; several errors at once | **MISSING** (partly COVERED) | The invalid-email error is COVERED by EU-23 and the one-error-at-a-time rule by EU-20..28. Reserved titles and email case are MISSING, and Android has **no reserved-title check at all** |
| EXT-02 | The back chevron needs two taps | **MISSING** | Every test leaves with system back (`driver.navigate().back()`). None taps the toolbar `backBtn` |

Counts: 2 MISSING, 0 COVERED, 0 WEB-ONLY, 0 CONFLICT.

---

## MISSING, in full

### EXT-01a: reserved titles in "Add More Information"

What the Android code does:
- The "Add More Information" button (`addMoreField` → `btnAdd`, AUEUP.kt:259-275) opens `showDialogWithMultipleEditBox` (utils/UIExtentions.kt:952).
- That dialog checks only that the title and description are not empty: `p_enter_title` and `p_enter_description` (UIExtentions.kt:~1003-1008).
- There is **no** reserved-title check, so "Full Name", "full name", "e-mail" and "Email" are all accepted.

**Proposed case:** `ExternalUserTest.EU_42_reservedInfoTitlesRefused`

**Steps:**
1. Open the add form ("ADD EXTERNAL USER").
2. Tap "Add More Information". In the dialog, set the title field (`editText1`) to "Full Name" and the description (`editText2`) to "x". Tap "Add".
3. Repeat with "full name", "FULL NAME", "e-mail" and "Email".

**Expected (per the sheet):** each one is refused, and no row is added to `recMoreField`.

**Today on Android:**
- Every one is added. Mark the case `known-bug` and raise it, since Android has the same gap as the web, only worse.
- If product says Android should not have the rule, re-classify this sub-item as WEB-ONLY.
- Leave the form with back afterwards. Nothing is submitted.

### EXT-01b: the email stays lower case

- The email field is set up with `inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS` (AUEUP.kt:195-198). That has no `TYPE_TEXT_FLAG_CAP_*` flag, while the default for the other fields is `TYPE_TEXT_FLAG_CAP_SENTENCES` (UIExtentions.kt:1191-1199).
- It also lacks `TYPE_CLASS_TEXT`, which is unusual; worth a look by a developer.

**What automation can check:** the stored value. Appium `sendKeys` sets the text directly, so it **cannot** show whether the soft keyboard capitalises.

**Proposed check (add to EU-31 `savedUserAppears`):**
1. Save with email `zl.auto.<epoch>@example.com`.
2. Open the detail screen.

**Expected:** the email shown is exactly lower case.

**Manual step:** whether the IME capitalises the first letter has to be checked by hand: tap the field and type with the Gboard keyboard.

### EXT-01c: one error at a time (already COVERED)

`validate()` is an `if / else if` chain (AUEUP.kt:574-621), so only one error shows at a time. EU-20..28 already pin each step, and EU-23 (`malformedEmailRefused`) pins "Please Enter Valid Email." (`p_enter_valid_email`). Nothing to add here.

### EXT-02: one tap on the toolbar back chevron leaves the form

The toolbar back is `toolbar.backBtn.setOnClickListener { onBackCLick() }` (baseUtils/BaseActivity.kt:524). It is wired once, so one tap should be enough.

**Proposed case:** `ExternalUserTest.EU_43_backChevronOneTap`

**Steps:**
1. On the External Users list, tap the add button → the form "ADD EXTERNAL USER" opens.
2. Tap `backBtn` inside `toolbar` **once**. Use `findElement(id toolbar).findElement(id backBtn)`, not system back.
3. Wait 2 s.

**Expected:**
- The list screen (ExternalUserListingPage) is in front.
- The header "ADD EXTERNAL USER" is gone.

**Repeat** the same on the detail screen ("USER DETAILS") and the update form ("UPDATE EXTERNAL USER").
