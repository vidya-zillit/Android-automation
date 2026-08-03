#!/usr/bin/env node
/**
 * Builds ONE self-contained, shareable HTML report from the JUnit XML that
 * Gradle writes to build/test-results/test/*.xml. No network, no Google Sheet,
 * no dependencies. Hand the resulting HTML to a developer — each failure is
 * classified (environment vs real app/test issue) with a plain-English
 * explanation and a next step, so they (or their own Claude) can act on it.
 *
 *   node tools/make-report.mjs            # -> build/report/zillit-report.html
 */
import fs from 'node:fs';
import path from 'node:path';

const RESULTS_DIR = path.resolve('build/test-results/test');
const OUT_DIR = path.resolve('build/report');
const OUT_FILE = path.join(OUT_DIR, 'zillit-report.html');

if (!fs.existsSync(RESULTS_DIR)) {
  console.error('✗ No test results at ' + RESULTS_DIR + ' — run the tests first.');
  process.exit(1);
}

// ---- tiny XML helpers (JUnit XML is simple and regular) --------------------
const attr = (tag, name) => {
  const m = tag.match(new RegExp(name + '="([^"]*)"'));
  return m ? decode(m[1]) : '';
};
const decode = (s) =>
  s.replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&quot;/g, '"')
   .replace(/&#10;/g, '\n').replace(/&#9;/g, '\t').replace(/&apos;/g, "'")
   .replace(/&amp;/g, '&');
const esc = (s) =>
  String(s ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

// ---- classify a failure into an actionable issue ---------------------------
function classify(kind, message, stack) {
  const blob = (message + ' ' + stack).toLowerCase();
  if (kind === 'skipped') {
    return {
      cat: 'SKIPPED',
      color: 'skip',
      note: 'A precondition was not met, so the test did not run. ' +
        (message ? 'Reason: ' + message : 'Common causes: no posting rights on the unit, ' +
          'no matching content on the device, or the required screen/unit not present.'),
    };
  }
  if (blob.includes('sessionnotcreated') || blob.includes('protocolhandshake') ||
      blob.includes('was not created') ||
      (blob.includes('webdriverexception') && (message === '' || blob.includes('constructor')))) {
    return {
      cat: 'ENVIRONMENT — not an app bug',
      color: 'env',
      note: 'The Appium session could not be created. This means the Appium server was ' +
        'not running, the device was offline, or the app was not launchable. ' +
        'Fix: start Appium (ANDROID_HOME=$HOME/Library/Android/sdk npm run appium), ' +
        'connect the device, make sure the app is installed and logged into a project, ' +
        'then re-run. No code change is needed for this.',
    };
  }
  if (blob.includes('assertionfailed') || blob.includes('opentest4j')) {
    return {
      cat: 'APP / TEST ASSERTION FAILED',
      color: 'fail',
      note: 'The app behaved differently from what the test expected. The message below ' +
        'states what was expected vs. what happened — this is the concrete issue to investigate.',
    };
  }
  if (blob.includes('nosuchelement') || blob.includes('timeoutexception') ||
      blob.includes('waiting for')) {
    return {
      cat: 'ELEMENT NOT FOUND / TIMED OUT',
      color: 'fail',
      note: 'A screen element the test waited for never appeared in time. Either the flow ' +
        'changed (a control was renamed/moved), the screen was slow, or an unexpected ' +
        'dialog blocked it. Check the step named in the stack trace.',
    };
  }
  return {
    cat: 'ERROR',
    color: 'fail',
    note: 'An unexpected error occurred during the test. See the stack trace below.',
  };
}

// ---- parse all suites ------------------------------------------------------
const files = fs.readdirSync(RESULTS_DIR).filter((f) => f.endsWith('.xml'));
const suites = [];

// ---- failure screenshots ---------------------------------------------------
// FailureScreenshot (JUnit extension) writes build/screenshots/manifest.tsv:
//   status \t TestClass \t displayName \t file.png
// We embed the PNG inline (base64) so the report stays ONE shareable file.
const SHOTS_DIR = path.resolve('build/screenshots');
const shotByKey = new Map();
try {
  const mf = path.join(SHOTS_DIR, 'manifest.tsv');
  if (fs.existsSync(mf)) {
    for (const line of fs.readFileSync(mf, 'utf8').split(/\r?\n/)) {
      if (!line.trim()) continue;
      const [status, cls, display, file] = line.split('\t');
      if (!file) continue;
      shotByKey.set(`${cls}\u0000${(display || '').trim()}`, file);   // later runs win
    }
  }
} catch { /* screenshots are optional */ }

function shotFor(suiteName, caseName) {
  const file = shotByKey.get(`${suiteName}\u0000${(caseName || '').trim()}`);
  if (!file) return '';
  const full = path.join(SHOTS_DIR, file);
  try {
    const b64 = fs.readFileSync(full).toString('base64');
    return `<div class="shot"><div class="shotlab">Screen at the moment of failure</div>` +
           `<img src="data:image/png;base64,${b64}" alt="failure screenshot"/></div>`;
  } catch { return ''; }
}

let totals = { tests: 0, passed: 0, failed: 0, skipped: 0 };

for (const f of files) {
  const xml = fs.readFileSync(path.join(RESULTS_DIR, f), 'utf8');
  const suiteTag = (xml.match(/<testsuite\b[^>]*>/) || [''])[0];
  const suiteName = attr(suiteTag, 'name').replace(/^com\.zillit\.automation\.tests\./, '');
  const cases = [];
  const caseRe = /<testcase\b([^>]*)>([\s\S]*?)<\/testcase>|<testcase\b([^>]*)\/>/g;
  let m;
  while ((m = caseRe.exec(xml)) !== null) {
    const head = m[1] || m[3] || '';
    const body = m[2] || '';
    const name = attr(head, 'name');
    const time = parseFloat(attr(head, 'time') || '0');
    let status = 'passed', kind = '', message = '', stack = '';
    const fm = body.match(/<(failure|error|skipped)\b([^>]*)(?:\/>|>([\s\S]*?)<\/\1>)/);
    if (fm) {
      kind = fm[1];
      status = kind === 'skipped' ? 'skipped' : 'failed';
      message = attr(fm[2] || '', 'message');
      stack = decode((fm[3] || '').trim());
    }
    cases.push({ name, time, status, kind, message, stack });
    totals.tests++;
    totals[status === 'passed' ? 'passed' : status === 'skipped' ? 'skipped' : 'failed']++;
  }
  cases.sort((a, b) => (a.status === b.status ? 0 : a.status === 'failed' ? -1 : b.status === 'failed' ? 1 : 0));
  suites.push({ name: suiteName, cases });
}
suites.sort((a, b) => a.name.localeCompare(b.name));

// ---- render ----------------------------------------------------------------
const when = new Date().toLocaleString();
const badge = (s) =>
  s === 'passed' ? '<span class="b pass">PASS</span>'
  : s === 'skipped' ? '<span class="b skip">SKIP</span>'
  : '<span class="b fail">FAIL</span>';

// ---- Jira-ready ticket text per failing/skipped test -----------------------
function expectedActual(message) {
  const m = (message || '').match(/==>\s*expected:\s*<([^>]*)>\s*but was:\s*<([^>]*)>/);
  return m ? { expected: m[1], actual: m[2] } : { expected: '', actual: '' };
}
function moduleOf(suiteName) {
  const n = suiteName.toLowerCase();
  if (n.includes('emailcalendar')) return { label: 'Email · Calendar', nav: ['Open the Email tab (bottom navigation).', 'Open the left drawer and tap "Calendar".'] };
  if (n.includes('email')) return { label: 'Email', nav: ['Open the Email tab (bottom navigation).'] };
  if (n.includes('calendar')) return { label: 'Calendar', nav: ['Open the Calendar unit from the Home units strip.'] };
  if (n.includes('callsheet')) return { label: 'Call Sheet', nav: ['Open the Call Sheet unit from the Home units strip.'] };
  if (n.includes('twouser')) return { label: 'Chat · two-device', nav: ['Sign in as two different users in the same project on two devices.', 'Open the same chat unit on both.'] };
  if (n.match(/chat|media|regression|postingrights|allunits/)) return { label: 'Home · Chat', nav: ['Open a chat unit (e.g. Bulletin) from the Home units strip.'] };
  if (n.match(/project|join|flow/)) return { label: 'Project', nav: ['Start from the Start & Join / Project List screen.'] };
  return { label: suiteName, nav: [] };
}
function jiraText(suiteName, c, info) {
  const { expected, actual } = expectedActual(c.message);
  const mod = moduleOf(suiteName);
  const assertText = (c.message || '').split('==>')[0].trim();
  const steps = ['Launch the Zillit app and open a project.', ...mod.nav,
    `Perform the scenario: ${c.name}.`, 'Observe the result.']
    .map((s, i) => `${i + 1}. ${s}`).join('\n');
  const isEnv = info.color === 'env';
  const sev = isEnv ? 'N/A (environment)' : (c.status === 'skipped' ? 'Low' : 'Medium');
  const type = isEnv ? 'Task (setup / environment)'
    : c.status === 'skipped' ? 'Bug (blocked — needs verification)' : 'Bug';
  return [
    `Summary: [Android][${mod.label}] ${c.name}`,
    ``,
    `Type: ${type}`,
    `Severity: ${sev}`,
    `Component: ${mod.label}`,
    ``,
    `Environment:`,
    `- App: com.zillit.zillitapp.dev (develop / debug build)`,
    `- Platform: Android (emulator / physical device)`,
    `- Found by: Zillit-Automation (Appium + UiAutomator2)`,
    ``,
    `Description:`,
    isEnv
      ? `The automated check "${c.name}" could not run due to a setup/environment problem (${info.cat}). This is NOT an app defect — fix the setup and re-run. ${info.note}`
      : c.status === 'skipped'
      ? `The automated check "${c.name}" was SKIPPED because a precondition was not met (it did not run). ${info.note}`
      : `Automated UI check "${c.name}" FAILED: the app did not behave as expected. ${info.note}`,
    ``,
    `Steps to Reproduce:`,
    steps,
    ``,
    `Expected Result:`,
    assertText || expected || '(see message)',
    ``,
    `Actual Result:`,
    actual ? `The condition was not met (was: ${actual}).` : (isEnv ? 'The test could not start.' : 'The expected condition was not met.'),
    ``,
    `Technical reference:`,
    `- Test: ${suiteName} › ${c.name}`,
    `- Category: ${info.cat}`,
    c.message ? `- Assertion: ${c.message}` : '',
  ].filter((l) => l !== undefined).join('\n');
}

let jiraSeq = 0;
const rows = suites.map((su) => {
  const items = su.cases.map((c) => {
    if (c.status === 'passed') {
      return `<tr class="passrow"><td>${badge(c.status)}</td><td>${esc(c.name)}</td>` +
        `<td class="dur">${c.time.toFixed(1)}s</td></tr>`;
    }
    const info = classify(c.kind, c.message, c.stack);
    const jid = 'jira' + (++jiraSeq);
    const ticket = jiraText(su.name, c, info);
    return `<tr class="issuerow ${info.color}"><td>${badge(c.status)}</td>` +
      `<td><div class="tname">${esc(c.name)}</div>` +
      `<div class="cat ${info.color}">${esc(info.cat)}</div>` +
      `<div class="note">${esc(info.note)}</div>` +
      (c.message ? `<div class="msg"><b>Message:</b> ${esc(c.message)}</div>` : '') +
      `<details class="jira" open><summary>📋 Jira ticket — copy &amp; paste</summary>` +
        `<button class="copybtn" data-target="${jid}">Copy to clipboard</button>` +
        shotFor(su.name, c.name) +
        `<pre id="${jid}" class="jiratext">${esc(ticket)}</pre></details>` +
      (c.stack ? `<details><summary>Stack trace</summary><pre>${esc(c.stack)}</pre></details>` : '') +
      `</td><td class="dur">${c.time.toFixed(1)}s</td></tr>`;
  }).join('');
  const nFail = su.cases.filter((c) => c.status === 'failed').length;
  const nSkip = su.cases.filter((c) => c.status === 'skipped').length;
  const tag = nFail ? `<span class="b fail">${nFail} failed</span>`
    : nSkip ? `<span class="b skip">${nSkip} skipped</span>`
    : `<span class="b pass">all passed</span>`;
  return `<section class="suite"><h2>${esc(su.name)} ${tag}</h2>` +
    `<table><thead><tr><th>Status</th><th>Test</th><th>Time</th></tr></thead><tbody>${items}</tbody></table></section>`;
}).join('');

const html = `<!doctype html><html lang="en"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Zillit Automation — Test Report</title>
<style>
:root{--bg:#f6f7f9;--card:#fff;--tx:#1b1f24;--mut:#5b6570;--line:#e4e7eb;
--pass:#1a7f37;--fail:#c9252d;--skip:#9a6700;--env:#0a5ad6;}
@media(prefers-color-scheme:dark){:root{--bg:#0e1116;--card:#161b22;--tx:#e6edf3;
--mut:#9aa4af;--line:#2a2f37;--pass:#3fb950;--fail:#f85149;--skip:#d29922;--env:#4f9dff;}}
*{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--tx);
font:15px/1.55 -apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,sans-serif;padding:24px}
.wrap{max-width:1000px;margin:0 auto}
h1{font-size:22px;margin:0 0 4px}.sub{color:var(--mut);margin:0 0 18px;font-size:13px}
.intro{background:var(--card);border:1px solid var(--line);border-radius:12px;padding:14px 16px;margin:0 0 18px;font-size:14px;color:var(--mut)}
.cards{display:flex;gap:10px;flex-wrap:wrap;margin:0 0 22px}
.c{flex:1;min-width:120px;background:var(--card);border:1px solid var(--line);border-radius:12px;padding:14px 16px}
.c .n{font-size:26px;font-weight:700}.c .l{font-size:12px;color:var(--mut);text-transform:uppercase;letter-spacing:.04em}
.c.pass .n{color:var(--pass)}.c.fail .n{color:var(--fail)}.c.skip .n{color:var(--skip)}
.suite{background:var(--card);border:1px solid var(--line);border-radius:12px;padding:6px 16px 14px;margin:0 0 16px;overflow-x:auto}
h2{font-size:16px;display:flex;align-items:center;gap:10px}
table{width:100%;border-collapse:collapse;font-size:14px}
th{text-align:left;color:var(--mut);font-size:11px;text-transform:uppercase;letter-spacing:.04em;border-bottom:1px solid var(--line);padding:6px 8px}
td{padding:9px 8px;border-bottom:1px solid var(--line);vertical-align:top}
.passrow td{color:var(--mut)}.dur{color:var(--mut);white-space:nowrap;text-align:right}
.b{font-size:11px;font-weight:700;padding:2px 7px;border-radius:99px;color:#fff;white-space:nowrap}
.b.pass{background:var(--pass)}.b.fail{background:var(--fail)}.b.skip{background:var(--skip)}
.tname{font-weight:600}.cat{font-size:11px;font-weight:700;margin-top:3px}
.cat.fail{color:var(--fail)}.cat.skip{color:var(--skip)}.cat.env{color:var(--env)}
.note{color:var(--mut);font-size:13px;margin-top:3px}
.msg{margin-top:6px;font-size:13px}
details{margin-top:6px}summary{cursor:pointer;color:var(--mut);font-size:12px}
pre{background:var(--bg);border:1px solid var(--line);border-radius:8px;padding:10px;overflow-x:auto;font-size:12px;white-space:pre-wrap}
.issuerow.env{background:color-mix(in srgb,var(--env) 6%,transparent)}
.issuerow.fail{background:color-mix(in srgb,var(--fail) 6%,transparent)}
.jira{margin-top:8px}.jira summary{color:var(--env);font-weight:600;cursor:pointer;font-size:13px}
.jiratext{background:var(--bg);border:1px solid var(--line);border-radius:8px;padding:12px;margin-top:8px;font-size:12.5px;white-space:pre-wrap;line-height:1.5}
.shot{margin-top:10px}
.shotlab{font-size:11.5px;color:var(--mut);text-transform:uppercase;letter-spacing:.04em;margin-bottom:4px}
.shot img{max-width:280px;width:100%;border:1px solid var(--line);border-radius:8px;display:block}
.copybtn{margin-top:8px;border:1px solid var(--line);background:var(--card);color:var(--tx);
border-radius:8px;padding:6px 12px;font-size:12px;font-weight:600;cursor:pointer}
.copybtn:hover{border-color:var(--env);color:var(--env)}.copybtn.done{color:var(--pass);border-color:var(--pass)}
</style></head><body><div class="wrap">
<h1>Zillit Android — UI Automation Report</h1>
<p class="sub">Generated ${esc(when)} · ${files.length} suite file(s)</p>
<div class="intro"><b>For the developer:</b> each failing/skipped test below is labelled with an
issue category and a plain-English explanation. <b>ENVIRONMENT</b> issues are setup problems
(e.g. Appium not running) — not app bugs. <b>APP / TEST ASSERTION</b> issues are real behavioural
differences worth investigating. Every issue includes a <b>📋 Jira ticket</b> block (Summary,
Environment, Description, Steps to Reproduce, Expected/Actual) — click <b>Copy to clipboard</b> and
paste it straight into Jira.</div>
<div class="cards">
<div class="c"><div class="n">${totals.tests}</div><div class="l">Total</div></div>
<div class="c pass"><div class="n">${totals.passed}</div><div class="l">Passed</div></div>
<div class="c fail"><div class="n">${totals.failed}</div><div class="l">Failed</div></div>
<div class="c skip"><div class="n">${totals.skipped}</div><div class="l">Skipped</div></div>
</div>
${rows}
<p class="sub" style="margin-top:20px">Zillit-Automation · Appium + UiAutomator2 · JUnit 5</p>
</div>
<script>
document.addEventListener('click', function (e) {
  var btn = e.target.closest('.copybtn');
  if (!btn) return;
  var pre = document.getElementById(btn.getAttribute('data-target'));
  if (!pre) return;
  var text = pre.innerText;
  var done = function () { btn.textContent = 'Copied ✓'; btn.classList.add('done');
    setTimeout(function () { btn.textContent = 'Copy to clipboard'; btn.classList.remove('done'); }, 1800); };
  if (navigator.clipboard && navigator.clipboard.writeText) {
    navigator.clipboard.writeText(text).then(done, function () { fallback(text, done); });
  } else { fallback(text, done); }
  function fallback(t, cb) {
    var ta = document.createElement('textarea'); ta.value = t;
    ta.style.position = 'fixed'; ta.style.opacity = '0'; document.body.appendChild(ta);
    ta.select(); try { document.execCommand('copy'); } catch (x) {} document.body.removeChild(ta); cb();
  }
});
</script>
</body></html>`;

fs.mkdirSync(OUT_DIR, { recursive: true });
fs.writeFileSync(OUT_FILE, html);
console.log(`✓ HTML report: ${OUT_FILE}`);
console.log(`  ${totals.passed} passed · ${totals.failed} failed · ${totals.skipped} skipped (of ${totals.tests})`);
