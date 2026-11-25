# Web E2E — Schedule Full & One Line

Source: Zillit E2E Report (generated 2026-11-17T10:27:04.399Z, env dev.zillit.com).

## Web test cases (0)

## Sheet bugs (4)

- **SCH-01** An undated One Line upload made right after a dated one shows the earlier date — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. Upload a schedule with a date. 2. In the same session, upload a One Line without picking a date.
  - Expected: The new One Line card has no Schedule Date row.
  - Actual: It showed the first upload's date.

- **SCH-02** Scene search is case-sensitive and can show "No Schedule Page found" when typing fast — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. Upload a page with a scene number. 2. Search for it in a different case, and type it quickly.
  - Expected: The page is found every time.
  - Actual: Different case found nothing; fast typing ended on "No Schedule Page found" because an older keystroke's answer arrived last.

- **SCH-03** Tools still shows a red badge on Schedule Full & One Line after the uploads were deleted — Low · Web: Not checked, Android: Not checked
  - Steps: 1. On A, upload a schedule page, then delete it. 2. Look at the Tools list on B.
  - Expected: No badge once the upload is gone.
  - Actual: A red 1 stayed on Schedule Full & One Line.

- **SCH-04** Empty scene row stays in the Pages list after its last page is deleted — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. Open a scene folder that has one page. 2. Delete the page and tap Ok on "Page has been deleted successfully."
  - Expected: The scene row disappears from the Pages list.
  - Actual: The row stays; tapping it opens the folder still showing the deleted card. It clears only after leaving the tool or switching tabs. Seen 2 of 2 (also in the 3 Nov run).
