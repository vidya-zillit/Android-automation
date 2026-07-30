/**
 * Zillit Automation — Google Sheet report receiver.
 *
 * ONE-TIME SETUP (in the Google Sheet you shared):
 * 1. Open the sheet -> Extensions -> Apps Script.
 * 2. Delete any code there and paste this whole file.
 * 3. Click Deploy -> New deployment -> type: Web app.
 *      - Execute as: Me
 *      - Who has access: Anyone with the link
 * 4. Copy the Web app URL (ends with /exec) and put it in
 *    Zillit-Automation/config/config.properties as:
 *      zillit.sheetWebhook=https://script.google.com/macros/s/…/exec
 *
 * Every test run then appends one summary row + one row per test case
 * to the "Test Reports" tab (created automatically).
 */
const TAB_NAME = 'Test Reports';

const HEADERS = [
  'Run Date', 'Suite', 'Test Case', 'Status',
  'Duration (s)', 'Bug / Failure Details', 'Device', 'Reported By'
];

function doPost(e) {
  const payload = JSON.parse(e.postData.contents);
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  let sheet = ss.getSheetByName(TAB_NAME);
  if (!sheet) {
    sheet = ss.insertSheet(TAB_NAME);
    sheet.appendRow(HEADERS);
    sheet.getRange(1, 1, 1, HEADERS.length).setFontWeight('bold');
    sheet.setFrozenRows(1);
  }

  // Summary row for the run, lightly highlighted.
  const s = payload.summary;
  sheet.appendRow([
    payload.runDate,
    '— RUN SUMMARY —',
    s.total + ' tests',
    s.failed > 0 ? 'FAILED' : 'PASSED',
    s.durationSeconds,
    s.passed + ' passed, ' + s.failed + ' failed, ' + s.skipped + ' skipped',
    payload.device,
    payload.reportedBy
  ]);
  sheet.getRange(sheet.getLastRow(), 1, 1, HEADERS.length)
      .setBackground(s.failed > 0 ? '#f4cccc' : '#d9ead3')
      .setFontWeight('bold');

  // One row per test case.
  payload.tests.forEach(function (t) {
    sheet.appendRow([
      payload.runDate, t.suite, t.name, t.status,
      t.durationSeconds, t.failure || '', payload.device, payload.reportedBy
    ]);
    if (t.status === 'FAILED') {
      sheet.getRange(sheet.getLastRow(), 4).setBackground('#f4cccc');
    } else if (t.status === 'SKIPPED') {
      sheet.getRange(sheet.getLastRow(), 4).setBackground('#fff2cc');
    }
  });

  return ContentService
      .createTextOutput(JSON.stringify({ ok: true, rows: payload.tests.length + 1 }))
      .setMimeType(ContentService.MimeType.JSON);
}
