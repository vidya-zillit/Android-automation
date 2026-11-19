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
  // unreachablebrowser = the server died MID-run, which is the one that matters most:
  // the session that dies first reports this, and every suite after it reports
  // sessionnotcreated. Without this line the originating failure looked like an app bug
  // while its eight consequences were correctly marked environment.
  if (blob.includes('sessionnotcreated') || blob.includes('protocolhandshake') ||
      blob.includes('unreachablebrowser') || blob.includes('it may have died') ||
      blob.includes('was not created') ||
      (blob.includes('webdriverexception') && (message === '' || blob.includes('constructor')))) {
    return {
      cat: 'ENVIRONMENT — not an app bug',
      color: 'env',
      note: 'The Appium session could not be created, or died mid-run. This means the ' +
        'Appium server was not running or crashed, the device went offline, or the app ' +
        'was not launchable. When one suite reports "it may have died" and the suites ' +
        'after it report SessionNotCreated, that is ONE server crash, not many bugs. ' +
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
const shotByKey = new Map();    // key -> full-size failure/skip PNG
const stepsByKey = new Map();   // key -> [{file,label}] evidence shots taken during the test
const KEY = (cls, display) => `${cls} ${(display || '').trim()}`;
try {
  const mf = path.join(SHOTS_DIR, 'manifest.tsv');
  if (fs.existsSync(mf)) {
    for (const line of fs.readFileSync(mf, 'utf8').split(/\r?\n/)) {
      if (!line.trim()) continue;
      const [status, cls, display, file, label] = line.split('\t');
      if (!file) continue;
      const key = KEY(cls, display);
      if (status === 'STEP') {
        // A re-run repeats the same step names, so keep only the latest pass:
        // reset the list when step 01 comes round again.
        const list = stepsByKey.get(key) || [];
        if (/__01__/.test(file)) list.length = 0;
        list.push({ file, label: label || 'step' });
        stepsByKey.set(key, list);
      } else {
        shotByKey.set(key, file);   // later runs win
      }
    }
  }
} catch { /* screenshots are optional */ }

function embed(file, alt) {
  try {
    const b64 = fs.readFileSync(path.join(SHOTS_DIR, file)).toString('base64');
    const mime = file.endsWith('.jpg') ? 'image/jpeg' : 'image/png';
    return `<img src="data:${mime};base64,${b64}" alt="${alt}"/>`;
  } catch { return ''; }
}

function shotFor(suiteName, caseName) {
  const key = KEY(suiteName, caseName);
  let html = '';

  const failFile = shotByKey.get(key);
  if (failFile) {
    const img = embed(failFile, 'failure screenshot');
    if (img) html += `<div class="shot"><div class="shotlab">Screen at the moment of failure</div>${img}</div>`;
  }

  // Evidence shots taken DURING the test - present for passing tests too, which is
  // what lets the report prove the flow actually reached each screen.
  const steps = stepsByKey.get(key) || [];
  if (steps.length) {
    const thumbs = steps.map(s => {
      const img = embed(s.file, s.label);
      return img ? `<figure class="stepshot">${img}<figcaption>${s.label}</figcaption></figure>` : '';
    }).join('');
    if (thumbs) {
      html += `<details class="steps"><summary>${steps.length} screenshot${steps.length > 1 ? 's' : ''} from this test</summary>` +
              `<div class="stepgrid">${thumbs}</div></details>`;
    }
  }
  return html;
}

let totals = { tests: 0, passed: 0, failed: 0, skipped: 0 };

for (const f of files) {
  const xml = fs.readFileSync(path.join(RESULTS_DIR, f), 'utf8');
  const suiteTag = (xml.match(/<testsuite\b[^>]*>/) || [''])[0];
  const suiteName = attr(suiteTag, 'name').replace(/^com\.zillit\.automation\.tests\./, '');
  const cases = [];
  // Self-closing MUST be tried first, and its attribute run must be lazy: `[^>]*` will
  // otherwise eat the trailing "/" of `<testcase .../>` , match the following ">", and
  // then run `[\s\S]*?` on to the NEXT "</testcase>" — swallowing a passing test into
  // the failing one after it (the pass vanishes, and its name shows on the failure).
  const caseRe = /<testcase\b([^>]*?)\/>|<testcase\b([^>]*?)>([\s\S]*?)<\/testcase>/g;
  let m;
  while ((m = caseRe.exec(xml)) !== null) {
    const head = m[1] !== undefined ? m[1] : (m[2] || '');
    const body = m[3] || '';
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

// ---- bucket every case: passed / real failure / environment / skipped ------
// Classification happens ONCE here so the charts, the counters and the rows can
// never disagree about what a given case is. "failed" on its own is misleading:
// one dead Appium server shows up as a failure in every suite that followed it,
// and lumping those in with real defects makes a run look far worse than it was.
const BUCKETS = {
  passed: { label: 'Passed',      color: 'var(--st-good)' },
  fail:   { label: 'Real bugs',   color: 'var(--st-crit)' },
  env:    { label: 'Environment', color: 'var(--st-serious)' },
  skip:   { label: 'Skipped',     color: 'var(--st-warn)' },
};
for (const su of suites) {
  su.module = moduleOf(su.name).label;
  for (const c of su.cases) {
    if (c.status === 'passed') { c.bucket = 'passed'; continue; }
    c.info = classify(c.kind, c.message, c.stack);
    c.bucket = c.status === 'skipped' ? 'skip' : (c.info.color === 'env' ? 'env' : 'fail');
  }
}
const tally = (cases) => {
  const t = { passed: 0, fail: 0, env: 0, skip: 0, total: cases.length };
  for (const c of cases) t[c.bucket]++;
  return t;
};
const overall = tally(suites.flatMap((s) => s.cases));

// Modules, ordered by what needs attention first: real bugs, then environment.
const moduleMap = new Map();
for (const su of suites) {
  if (!moduleMap.has(su.module)) moduleMap.set(su.module, []);
  moduleMap.get(su.module).push(su);
}
const modules = [...moduleMap.entries()]
  .map(([label, sus]) => ({ label, suites: sus, stats: tally(sus.flatMap((s) => s.cases)) }))
  .sort((a, b) => (b.stats.fail - a.stats.fail) || (b.stats.env - a.stats.env)
    || (b.stats.total - a.stats.total));

// ---- chart pieces (plain HTML/CSS — no SVG scaling, no libraries) ----------
// A stacked bar, not a donut: four parts of one whole, read against a shared
// baseline. 2px gaps between segments, 4px rounded ends, hover gives exact counts.
function stackBar(t, { height = 14 } = {}) {
  const total = t.total || 1;
  const seg = (k) => t[k] > 0
    ? `<span class="seg" style="flex:${t[k]};background:${BUCKETS[k].color}" `
      + `data-tip="${BUCKETS[k].label}: ${t[k]} of ${t.total} (${Math.round(t[k] / total * 100)}%)"></span>`
    : '';
  return `<span class="stack" style="height:${height}px">`
    + ['passed', 'fail', 'env', 'skip'].map(seg).join('') + '</span>';
}
// A donut per the brief: one ring = one whole, segments clickable. Every segment
// also carries its count as text in the legend, so the ring is never the only way
// to read the number — colour and angle are the decoration, not the data.
function donut(t, { size = 190, thick = 26, module = '' } = {}) {
  const total = t.total || 1;
  const keys = ['passed', 'fail', 'env', 'skip'].filter((k) => t[k] > 0);
  const r = (size - thick) / 2;
  const C = 2 * Math.PI * r;
  const gap = keys.length > 1 ? 3 : 0;
  let offset = 0;
  const segs = keys.map((k) => {
    const len = (t[k] / total) * C;
    const dash = Math.max(0.5, len - gap);
    const el = `<circle class="dseg" data-b="${k}" data-module="${esc(module)}" tabindex="0" role="button"`
      + ` aria-label="${BUCKETS[k].label}: ${t[k]} of ${t.total}. Click to filter."`
      + ` data-tip="${BUCKETS[k].label}: ${t[k]} of ${t.total} (${Math.round(t[k] / total * 100)}%) — click to filter"`
      + ` cx="${size / 2}" cy="${size / 2}" r="${r}" fill="none" stroke="${BUCKETS[k].color}"`
      + ` stroke-width="${thick}" stroke-dasharray="${dash.toFixed(2)} ${(C - dash).toFixed(2)}"`
      + ` stroke-dashoffset="${(-offset).toFixed(2)}"/>`;
    offset += len;
    return el;
  }).join('');
  const rate = pct(t.passed, t.total);
  return `<svg class="donut" viewBox="0 0 ${size} ${size}" role="img"`
    + ` aria-label="Pass rate ${rate} percent of ${t.total} cases">`
    + `<circle cx="${size / 2}" cy="${size / 2}" r="${r}" fill="none" stroke="var(--line)" stroke-width="${thick}"/>`
    + `<g transform="rotate(-90 ${size / 2} ${size / 2})">${segs}</g>`
    + `<text class="dnum" x="50%" y="48%" text-anchor="middle">${rate}%</text>`
    + `<text class="dlab" x="50%" y="63%" text-anchor="middle">${t.passed}/${t.total} passed</text>`
    + `</svg>`;
}

function legend() {
  return '<div class="legend">' + ['passed', 'fail', 'env', 'skip'].map((k) =>
    `<button class="lg dseg" data-b="${k}" data-tip="Click to filter to ${BUCKETS[k].label.toLowerCase()}">`
    + `<i style="background:${BUCKETS[k].color}"></i>${BUCKETS[k].label}`
    + `<b>${overall[k]}</b></button>`).join('') + '</div>';
}
const pct = (n, d) => (d ? Math.round((n / d) * 100) : 0);

// Per-module rows. The counts sit beside the bar as text, so the chart is
// readable without colour and the exact numbers never need a hover.
const moduleChart = modules.map((m) => {
  const t = m.stats;
  const chip = (k) => t[k]
    ? `<button class="mc ${k} dseg" data-b="${k}" data-module="${esc(m.label)}"`
      + ` data-tip="${BUCKETS[k].label} in ${esc(m.label)} — click to filter">${t[k]} ${BUCKETS[k].label.toLowerCase()}</button>`
    : '';
  return `<div class="mcard" id="mod-${encodeURIComponent(m.label)}">`
    + `<div class="mtitle">${esc(m.label)}</div>`
    + donut(t, { size: 150, thick: 20, module: m.label })
    + `<div class="mchips">${chip('fail')}${chip('env')}${chip('skip')}`
    + `<button class="mc passed dseg" data-b="passed" data-module="${esc(m.label)}"`
    + ` data-tip="Passed in ${esc(m.label)} — click to filter">${t.passed} passed</button></div></div>`;
}).join('');

// The actionable list: real bugs only, grouped by module.
const realBugs = modules.flatMap((m) => m.suites.flatMap((su) =>
  su.cases.filter((c) => c.bucket === 'fail').map((c) => ({ module: m.label, suite: su.name, name: c.name }))));
const bugList = realBugs.length
  ? realBugs.map((b) => `<li><span class="bmod">${esc(b.module)}</span>`
      + `<span class="bname">${esc(b.name)}</span>`
      + `<span class="bsuite">${esc(b.suite)}</span></li>`).join('')
  : '<li class="none">No real failures in this run.</li>';


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

// Every case carries data-* attributes so the filter bar can hide rows without
// re-rendering anything — the report stays a single static file.
function caseRow(su, c) {
  const dataAttrs = `data-bucket="${c.bucket}" data-module="${esc(su.module)}" `
    + `data-q="${esc((c.name + ' ' + su.name).toLowerCase())}"`;
  if (c.status === 'passed') {
    const shots = shotFor(su.name, c.name);
    return `<tr class="caserow passrow" ${dataAttrs}><td>${badge(c.status)}</td>`
      + `<td>${esc(c.name)}</td><td class="dur">${c.time.toFixed(1)}s</td></tr>`
      + (shots ? `<tr class="caserow passshots" ${dataAttrs}><td></td><td colspan="2">${shots}</td></tr>` : '');
  }
  const info = c.info;
  const jid = 'jira' + (++jiraSeq);
  const ticket = jiraText(su.name, c, info);
  return `<tr class="caserow issuerow ${info.color}" ${dataAttrs}><td>${badge(c.status)}</td>`
    + `<td><div class="tname">${esc(c.name)}</div>`
    + `<div class="cat ${info.color}">${esc(info.cat)}</div>`
    + `<div class="note">${esc(info.note)}</div>`
    + (c.message ? `<div class="msg"><b>Message:</b> ${esc(c.message)}</div>` : '')
    + `<details class="jira"><summary>Jira ticket — copy &amp; paste</summary>`
      + `<button class="copybtn" data-target="${jid}">Copy to clipboard</button>`
      + shotFor(su.name, c.name)
      + `<pre id="${jid}" class="jiratext">${esc(ticket)}</pre></details>`
    + (c.stack ? `<details><summary>Stack trace</summary><pre>${esc(c.stack)}</pre></details>` : '')
    + `</td><td class="dur">${c.time.toFixed(1)}s</td></tr>`;
}

// Suites live inside their module, so the page reads the same way the chart does.
const rows = modules.map((m) => {
  const t = m.stats;
  const suiteBlocks = m.suites.map((su) => {
    const st = tally(su.cases);
    const tag = st.fail ? `<span class="b fail">${st.fail} real</span>`
      : st.env ? `<span class="b env">${st.env} env</span>`
      : st.skip ? `<span class="b skip">${st.skip} skipped</span>`
      : `<span class="b pass">all passed</span>`;
    return `<section class="suite" data-module="${esc(m.module || m.label)}">`
      + `<h3>${esc(su.name)} ${tag}<span class="sbar">${stackBar(st, { height: 8 })}</span></h3>`
      + `<table><thead><tr><th>Status</th><th>Test</th><th>Time</th></tr></thead>`
      + `<tbody>${su.cases.map((c) => caseRow(su, c)).join('')}</tbody></table></section>`;
  }).join('');
  return `<section class="modblock" id="mod-${encodeURIComponent(m.label)}" data-module="${esc(m.label)}">`
    + `<header class="modhead"><h2>${esc(m.label)}</h2>`
    + `<span class="modstat">${t.passed}/${t.total} passed</span>`
    + `<span class="modbar">${stackBar(t, { height: 10 })}</span></header>`
    + suiteBlocks + `</section>`;
}).join('');

// ---- run metadata — what this report is actually OF ------------------------
const runSeconds = suites.flatMap((s) => s.cases).reduce((a, c) => a + (c.time || 0), 0);
const fmtDur = (sec) => sec >= 3600
  ? `${Math.floor(sec / 3600)}h ${Math.round((sec % 3600) / 60)}m`
  : sec >= 60 ? `${Math.floor(sec / 60)}m ${Math.round(sec % 60)}s` : `${Math.round(sec)}s`;
function configValue(key) {
  try {
    const props = fs.readFileSync(path.resolve('config/config.properties'), 'utf8');
    const m = props.match(new RegExp('^' + key.replace('.', '\\.') + '=(.*)$', 'm'));
    return m ? m[1].trim() : '';
  } catch { return ''; }
}
const meta = [
  ['Device', process.env.ZILLIT_UDID || configValue('zillit.udid') || 'unknown'],
  ['Build', process.env.ZILLIT_APP_PACKAGE || configValue('zillit.appPackage') || 'unknown'],
  ['Suites', String(suites.length)],
  ['Run time', fmtDur(runSeconds)],
].map(([k, v]) => `<span class="meta"><b>${esc(k)}</b>${esc(v)}</span>`).join('');

const passRate = pct(overall.passed, overall.total);
const healthWord = overall.fail === 0 ? 'No real failures'
  : overall.fail === 1 ? '1 real failure' : `${overall.fail} real failures`;

const html = `<!doctype html><html lang="en"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Zillit Automation — Test Report</title>
<style>
/* Status palette — fixed roles, never themed, never reused as "series colours".
   Each is always paired with a text label, so meaning is never carried by hue alone. */
:root{--bg:#f4f5f7;--card:#fff;--tx:#0b0b0b;--mut:#52514e;--line:#e4e7eb;--soft:#fafbfc;
--st-good:#0ca30c;--st-warn:#fab219;--st-serious:#ec835a;--st-crit:#d03b3b;
--pass:#0a7a0a;--fail:#c13434;--skip:#8a6300;--env:#b4572c;--accent:#2a78d6;}
/* Dark is SELECTED, not an automatic inversion: its own steps, all clearing 3:1
   on the dark surface. The toggle wins over the OS preference when set. */
@media(prefers-color-scheme:dark){:root:not([data-theme="light"]){--bg:#121211;--card:#1a1a19;--tx:#f0efec;
--mut:#c3c2b7;--line:#33322f;--soft:#201f1e;
--st-good:#0ca30c;--st-warn:#fab219;--st-serious:#ec835a;--st-crit:#d03b3b;
--pass:#3fb950;--fail:#ef6b6b;--skip:#fab219;--env:#ec835a;--accent:#3987e5;}}
:root[data-theme="dark"]{--bg:#121211;--card:#1a1a19;--tx:#f0efec;
--mut:#c3c2b7;--line:#33322f;--soft:#201f1e;
--st-good:#0ca30c;--st-warn:#fab219;--st-serious:#ec835a;--st-crit:#d03b3b;
--pass:#3fb950;--fail:#ef6b6b;--skip:#fab219;--env:#ec835a;--accent:#3987e5;}
:root{color-scheme:light}:root[data-theme="dark"]{color-scheme:dark}
@media(prefers-color-scheme:dark){:root:not([data-theme="light"]){color-scheme:dark}}
*{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--tx);
font:15px/1.55 -apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,sans-serif;padding:24px}
.wrap{max-width:1180px;margin:0 auto}
/* --- header ------------------------------------------------------------- */
.top{display:flex;flex-wrap:wrap;gap:14px;align-items:flex-end;justify-content:space-between;margin:0 0 16px}
.metas{display:flex;flex-wrap:wrap;gap:8px}
.meta{background:var(--card);border:1px solid var(--line);border-radius:99px;padding:4px 12px;font-size:12px;color:var(--mut)}
.meta b{color:var(--tx);font-weight:600;margin-right:6px}
/* --- KPI tiles ----------------------------------------------------------- */
.kpis{display:grid;grid-template-columns:repeat(auto-fit,minmax(150px,1fr));gap:12px;margin:0 0 16px}
.k{background:var(--card);border:1px solid var(--line);border-radius:14px;padding:14px 16px}
.k .n{font-size:30px;font-weight:700;line-height:1.1;letter-spacing:-.02em}
.k .l{font-size:11px;color:var(--mut);text-transform:uppercase;letter-spacing:.06em;margin-top:2px}
.k .s{font-size:12px;color:var(--mut);margin-top:4px}
.k.hero{grid-column:span 2;background:linear-gradient(135deg,var(--card),var(--soft))}
.k.hero .n{font-size:44px}
.k.good .n{color:var(--st-good)}.k.crit .n{color:var(--st-crit)}
.k.serious .n{color:var(--st-serious)}.k.warn .n{color:var(--skip)}
/* --- panels & charts ----------------------------------------------------- */
.panel{background:var(--card);border:1px solid var(--line);border-radius:14px;padding:16px 18px;margin:0 0 16px}
.panel h2{font-size:13px;text-transform:uppercase;letter-spacing:.06em;color:var(--mut);margin:0 0 12px;font-weight:600}
.stack{display:flex;gap:2px;width:100%;border-radius:4px;overflow:hidden}
.stack .seg{display:block;border-radius:4px;cursor:default}
.legend{display:flex;flex-wrap:wrap;gap:14px;margin-top:10px;font-size:12px;color:var(--mut)}
.lg{display:inline-flex;align-items:center;gap:6px}
.lg i{width:10px;height:10px;border-radius:3px;display:inline-block}
/* --- donuts --------------------------------------------------------------- */
.hero2{display:flex;gap:28px;align-items:center;flex-wrap:wrap}
.donut{width:190px;height:190px;flex:0 0 auto}
.donut .dseg{cursor:pointer;transition:opacity .12s,stroke-width .12s}
.donut .dseg:hover{opacity:.82}
.donut .dseg:focus-visible{outline:2px solid var(--accent);outline-offset:2px}
.dnum{font-size:34px;font-weight:700;fill:var(--tx)}
.dlab{font-size:11px;fill:var(--mut)}
.mgrid{display:grid;grid-template-columns:repeat(auto-fit,minmax(210px,1fr));gap:14px}
.mcard{background:var(--soft);border:1px solid var(--line);border-radius:14px;
padding:14px;display:flex;flex-direction:column;align-items:center;gap:10px;text-align:center}
.mcard .donut{width:150px;height:150px}
.mcard .dnum{font-size:26px}
.mtitle{font-weight:700;font-size:14px}
.mchips{display:flex;gap:6px;flex-wrap:wrap;justify-content:center}
.lg{background:none;border:0;font:inherit;cursor:pointer;color:var(--mut);
display:inline-flex;align-items:center;gap:6px;padding:4px 8px;border-radius:8px}
.lg:hover{background:var(--soft);color:var(--tx)}
.lg b{color:var(--tx)}
/* --- theme toggle --------------------------------------------------------- */
.themebtn{border:1px solid var(--line);background:var(--card);color:var(--tx);border-radius:99px;
padding:6px 12px;font:inherit;font-size:12px;font-weight:600;cursor:pointer;display:inline-flex;gap:6px;align-items:center}
.themebtn:hover{border-color:var(--accent);color:var(--accent)}
.mnums{display:flex;gap:6px;flex-wrap:wrap;justify-content:flex-end}
.mc{font-size:11px;font-weight:600;padding:3px 9px;border-radius:99px;border:1px solid var(--line);color:var(--mut);white-space:nowrap;background:var(--card);cursor:pointer;font-family:inherit}
.mc:hover{border-color:var(--accent);color:var(--accent)}
.mc.fail{color:var(--fail);border-color:color-mix(in srgb,var(--st-crit) 45%,transparent)}
.mc.env{color:var(--env);border-color:color-mix(in srgb,var(--st-serious) 45%,transparent)}
.mc.skip{color:var(--skip);border-color:color-mix(in srgb,var(--st-warn) 45%,transparent)}
.buglist{list-style:none;margin:0;padding:0}
.buglist li{display:grid;grid-template-columns:150px 1fr auto;gap:12px;align-items:baseline;
padding:8px 6px;border-bottom:1px solid var(--line);font-size:13.5px}
.buglist li:last-child{border-bottom:0}
.bmod{color:var(--mut);font-size:12px}
.bname{font-weight:600}
.bsuite{color:var(--mut);font-size:11.5px}
.buglist .none{grid-template-columns:1fr;color:var(--mut)}
/* --- filter toolbar ------------------------------------------------------ */
.tools{position:sticky;top:0;z-index:5;background:var(--card);border:1px solid var(--line);
border-radius:14px;padding:10px 12px;margin:0 0 16px;display:flex;gap:10px;flex-wrap:wrap;align-items:center;
box-shadow:0 1px 3px rgba(0,0,0,.05)}
.tools input[type=search]{flex:1;min-width:170px;background:var(--bg);border:1px solid var(--line);
border-radius:9px;padding:7px 11px;font:inherit;font-size:13px;color:var(--tx)}
.tools select{background:var(--bg);border:1px solid var(--line);border-radius:9px;padding:7px 9px;font:inherit;font-size:13px;color:var(--tx)}
.chips{display:flex;gap:6px;flex-wrap:wrap}
.chip{border:1px solid var(--line);background:var(--bg);color:var(--mut);border-radius:99px;
padding:6px 12px;font-size:12px;font-weight:600;cursor:pointer;display:inline-flex;align-items:center;gap:6px}
.chip i{width:9px;height:9px;border-radius:3px;display:inline-block}
.chip[aria-pressed=true]{background:var(--tx);color:var(--card);border-color:var(--tx)}
.hiddenrow{display:none !important}
.modblock{margin:0 0 18px}
.modhead{display:grid;grid-template-columns:1fr auto 190px;gap:14px;align-items:center;margin:0 0 8px}
.modhead h2{font-size:17px;margin:0;text-transform:none;letter-spacing:0;color:var(--tx);font-weight:700}
.modstat{font-size:12px;color:var(--mut)}
.emptymsg{display:none;background:var(--card);border:1px dashed var(--line);border-radius:14px;
padding:26px;text-align:center;color:var(--mut)}
#tip{position:fixed;pointer-events:none;opacity:0;transition:opacity .12s;background:var(--tx);
color:var(--card);font-size:12px;padding:5px 9px;border-radius:7px;z-index:50;white-space:nowrap}
.sbar{flex:0 0 120px;margin-left:auto}
h3{font-size:14.5px;display:flex;align-items:center;gap:10px;margin:10px 0 6px}
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
.b.pass{background:var(--pass)}.b.fail{background:var(--fail)}.b.skip{background:var(--skip)}.b.env{background:var(--env)}
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
.steps{margin-top:10px}
.steps summary{cursor:pointer;font-size:12px;color:var(--mut);user-select:none}
.steps summary:hover{color:inherit}
.stepgrid{display:flex;flex-wrap:wrap;gap:12px;margin-top:10px}
.stepshot{margin:0;width:180px}
.stepshot img{width:100%;border:1px solid var(--line);border-radius:6px;display:block}
.stepshot figcaption{font-size:11px;color:var(--mut);margin-top:4px;word-break:break-word}
.copybtn{margin-top:8px;border:1px solid var(--line);background:var(--card);color:var(--tx);
border-radius:8px;padding:6px 12px;font-size:12px;font-weight:600;cursor:pointer}
.copybtn:hover{border-color:var(--env);color:var(--env)}.copybtn.done{color:var(--pass);border-color:var(--pass)}
</style></head><body><div class="wrap">

<div class="top">
  <div>
    <h1>Zillit Android — UI Automation</h1>
    <p class="sub">Generated ${esc(when)}</p>
  </div>
  <div class="metas">${meta}<button class="themebtn" id="theme" type="button">Theme</button></div>
</div>

<div class="kpis">
  <div class="k hero"><div class="n">${passRate}%</div><div class="l">Pass rate</div>
    <div class="s">${overall.passed} of ${overall.total} cases · ${healthWord}</div></div>
  <div class="k crit"><div class="n">${overall.fail}</div><div class="l">Real bugs</div>
    <div class="s">app or test defects</div></div>
  <div class="k serious"><div class="n">${overall.env}</div><div class="l">Environment</div>
    <div class="s">setup, not defects</div></div>
  <div class="k warn"><div class="n">${overall.skip}</div><div class="l">Skipped</div>
    <div class="s">preconditions unmet</div></div>
  <div class="k"><div class="n">${modules.length}</div><div class="l">Modules</div>
    <div class="s">${suites.length} suites</div></div>
</div>

<div class="panel">
  <h2>Run composition — click a segment to filter</h2>
  <div class="hero2">
    ${donut(overall, { size: 210, thick: 30 })}
    <div style="flex:1;min-width:220px">${legend()}
      <p class="sub" style="margin-top:10px">Click any slice or legend entry to jump to those cases.
      Click it again to clear.</p></div>
  </div>
</div>

<div class="panel">
  <h2>Module breakdown — where the bugs are</h2>
  <div class="mgrid">${moduleChart}</div>
</div>

<div class="panel">
  <h2>Real bugs (${realBugs.length}) — environment failures excluded</h2>
  <ul class="buglist">${bugList}</ul>
</div>

<div class="intro"><b>How to read this:</b> <b>Real bugs</b> are behavioural differences worth
investigating. <b>Environment</b> failures are setup problems — a dead Appium server, an offline
device — and are not app defects; when one suite reports "it may have died" and later suites report
SessionNotCreated, that is <i>one</i> crash, not many bugs. <b>Skipped</b> means a precondition was
missing, so the case never ran. Every issue carries a Jira ticket block ready to paste.</div>

<div class="tools">
  <input type="search" id="q" placeholder="Search test or suite name…" aria-label="Search tests">
  <select id="modsel" aria-label="Filter by module"><option value="">All modules</option>
    ${modules.map((m) => `<option value="${esc(m.label)}">${esc(m.label)} (${m.stats.total})</option>`).join('')}
  </select>
  <div class="chips" id="chips">
    <button class="chip" data-b="fail" aria-pressed="false"><i style="background:var(--st-crit)"></i>Real bugs ${overall.fail}</button>
    <button class="chip" data-b="env" aria-pressed="false"><i style="background:var(--st-serious)"></i>Environment ${overall.env}</button>
    <button class="chip" data-b="skip" aria-pressed="false"><i style="background:var(--st-warn)"></i>Skipped ${overall.skip}</button>
    <button class="chip" data-b="passed" aria-pressed="false"><i style="background:var(--st-good)"></i>Passed ${overall.passed}</button>
  </div>
  <span class="meta" id="count"><b>Showing</b>${overall.total} cases</span>
</div>

<div class="emptymsg" id="empty">Nothing matches these filters.</div>
${rows}
<p class="sub" style="margin-top:20px">Zillit-Automation · Appium + UiAutomator2 · JUnit 5 · self-contained report</p>
</div>
<div id="tip"></div>
<script>
// ---- filters: search + module + status chips (all combine) ----------------
(function () {
  var q = document.getElementById('q');
  var modsel = document.getElementById('modsel');
  var chips = [].slice.call(document.querySelectorAll('.chip'));
  var rows = [].slice.call(document.querySelectorAll('.caserow'));
  var count = document.getElementById('count');
  var empty = document.getElementById('empty');

  function active() {
    return chips.filter(function (c) { return c.getAttribute('aria-pressed') === 'true'; })
                .map(function (c) { return c.dataset.b; });
  }
  function apply() {
    var text = (q.value || '').trim().toLowerCase();
    var mod = modsel.value;
    var want = active();           // empty = show every status
    var shown = 0;
    rows.forEach(function (r) {
      var ok = (!want.length || want.indexOf(r.dataset.bucket) > -1)
            && (!mod || r.dataset.module === mod)
            && (!text || (r.dataset.q || '').indexOf(text) > -1);
      r.classList.toggle('hiddenrow', !ok);
      if (ok && !r.classList.contains('passshots')) shown++;
    });
    // Hide a suite or module whose every row is filtered out, so the page does
    // not fill up with empty tables.
    [].forEach.call(document.querySelectorAll('.suite'), function (sec) {
      var any = sec.querySelector('.caserow:not(.hiddenrow)');
      sec.style.display = any ? '' : 'none';
    });
    [].forEach.call(document.querySelectorAll('.modblock'), function (sec) {
      var any = sec.querySelector('.caserow:not(.hiddenrow)');
      sec.style.display = any ? '' : 'none';
    });
    count.innerHTML = '<b>Showing</b>' + shown + ' case' + (shown === 1 ? '' : 's');
    empty.style.display = shown ? 'none' : 'block';
  }
  chips.forEach(function (c) {
    c.addEventListener('click', function () {
      c.setAttribute('aria-pressed', c.getAttribute('aria-pressed') === 'true' ? 'false' : 'true');
      apply();
    });
  });
  q.addEventListener('input', apply);
  modsel.addEventListener('change', apply);

  // A donut slice, a legend entry and a module chip are all the same action:
  // set the status (and module) filter, then take the reader to the results.
  // Clicking the same slice again clears it, so the chart doubles as the control.
  function setFilter(bucket, module) {
    var already = chips.filter(function (c) {
      return c.dataset.b === bucket && c.getAttribute('aria-pressed') === 'true';
    }).length > 0 && (!module || modsel.value === module);
    chips.forEach(function (c) { c.setAttribute('aria-pressed', 'false'); });
    if (already) { modsel.value = ''; apply(); return; }
    chips.forEach(function (c) {
      if (c.dataset.b === bucket) c.setAttribute('aria-pressed', 'true');
    });
    modsel.value = module || '';
    apply();
    var list = document.querySelector('.tools');
    if (list) list.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
  document.addEventListener('click', function (e) {
    var el = e.target.closest ? e.target.closest('.dseg') : null;
    if (!el || !el.dataset.b) return;
    e.preventDefault();
    setFilter(el.dataset.b, el.dataset.module || '');
  });
  document.addEventListener('keydown', function (e) {
    if (e.key !== 'Enter' && e.key !== ' ') return;
    var el = e.target.closest ? e.target.closest('.dseg') : null;
    if (!el || !el.dataset.b) return;
    e.preventDefault();
    setFilter(el.dataset.b, el.dataset.module || '');
  });
  apply();
})();

// ---- theme toggle: follows the OS until the reader chooses ----------------
(function () {
  var btn = document.getElementById('theme');
  var root = document.documentElement;
  var KEY = 'zillit-report-theme';
  function osDark() { return window.matchMedia('(prefers-color-scheme: dark)').matches; }
  function label() {
    var t = root.getAttribute('data-theme');
    var dark = t ? t === 'dark' : osDark();
    btn.textContent = dark ? 'Light mode' : 'Dark mode';
  }
  try { var saved = localStorage.getItem(KEY); if (saved) root.setAttribute('data-theme', saved); } catch (x) {}
  label();
  btn.addEventListener('click', function () {
    var t = root.getAttribute('data-theme');
    var dark = t ? t === 'dark' : osDark();
    var next = dark ? 'light' : 'dark';
    root.setAttribute('data-theme', next);
    try { localStorage.setItem(KEY, next); } catch (x) {}
    label();
  });
})();

// ---- hover layer on the charts -------------------------------------------
(function () {
  var tip = document.getElementById('tip');
  document.addEventListener('mousemove', function (e) {
    var seg = e.target.closest ? e.target.closest('.seg,.dseg,.mc') : null;
    if (!seg || !seg.dataset.tip) { tip.style.opacity = '0'; return; }
    tip.textContent = seg.dataset.tip;
    tip.style.opacity = '1';
    var x = Math.min(e.clientX + 12, window.innerWidth - tip.offsetWidth - 10);
    tip.style.left = x + 'px';
    tip.style.top = (e.clientY - tip.offsetHeight - 10) + 'px';
  });
})();

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
