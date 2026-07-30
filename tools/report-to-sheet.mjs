#!/usr/bin/env node
/**
 * Parses the JUnit XML results in build/test-results/test and appends them to
 * the team Google Sheet through the Apps Script webhook.
 *
 * Usage:  node tools/report-to-sheet.mjs
 * Config: zillit.sheetWebhook in config/config.properties
 *         (or env ZILLIT_SHEET_WEBHOOK, which wins).
 *
 * No Claude, no cloud SDKs — plain Node 18+ (built-in fetch) and zero deps:
 * the JUnit XML subset Gradle writes is parsed with targeted regexes.
 */
import { readFileSync, readdirSync, existsSync } from 'node:fs';
import { hostname } from 'node:os';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const resultsDir = join(root, 'build', 'test-results', 'test');

function config(key, envKey) {
  if (process.env[envKey]) return process.env[envKey].trim();
  const props = readFileSync(join(root, 'config', 'config.properties'), 'utf8');
  const m = props.match(new RegExp('^' + key.replace('.', '\\.') + '=(.*)$', 'm'));
  return m ? m[1].trim() : '';
}

const webhook = config('zillit.sheetWebhook', 'ZILLIT_SHEET_WEBHOOK');
if (!webhook) {
  console.error('✗ No webhook configured. Set zillit.sheetWebhook in config/config.properties');
  console.error('  (see tools/google-apps-script.gs for the one-time sheet setup).');
  process.exit(1);
}
if (!existsSync(resultsDir)) {
  console.error('✗ No test results found at ' + resultsDir + ' — run ./gradlew test first.');
  process.exit(1);
}

const unescape = (s) => s
    .replace(/&quot;/g, '"').replace(/&apos;/g, "'").replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>').replace(/&#10;/g, '\n').replace(/&amp;/g, '&');

const tests = [];
for (const file of readdirSync(resultsDir).filter(f => f.endsWith('.xml'))) {
  const xml = readFileSync(join(resultsDir, file), 'utf8');
  const suite = (xml.match(/<testsuite[^>]*name="([^"]+)"/) || [])[1]?.split('.').pop() ?? file;
  // Each <testcase …> up to its closing (self-closed or paired).
  for (const tc of xml.matchAll(/<testcase\b([^>]*)(?:\/>|>([\s\S]*?)<\/testcase>)/g)) {
    const attrs = tc[1];
    const body = tc[2] ?? '';
    const name = unescape((attrs.match(/name="([^"]*)"/) || [])[1] ?? '?');
    const time = parseFloat((attrs.match(/time="([^"]*)"/) || [])[1] ?? '0');
    let status = 'PASSED';
    let failure = '';
    const fail = body.match(/<(failure|error)[^>]*message="([^"]*)"/);
    if (fail) {
      status = 'FAILED';
      failure = unescape(fail[2]).split('\n')[0].slice(0, 500);
    } else if (/<skipped\b/.test(body)) {
      status = 'SKIPPED';
      const why = body.match(/<skipped[^>]*message="([^"]*)"/);
      failure = why ? unescape(why[1]).slice(0, 300) : '';
    }
    tests.push({ suite, name, status, durationSeconds: time, failure });
  }
}

if (tests.length === 0) {
  console.error('✗ Result files contained no test cases.');
  process.exit(1);
}

const summary = {
  total: tests.length,
  passed: tests.filter(t => t.status === 'PASSED').length,
  failed: tests.filter(t => t.status === 'FAILED').length,
  skipped: tests.filter(t => t.status === 'SKIPPED').length,
  durationSeconds: Math.round(tests.reduce((a, t) => a + t.durationSeconds, 0) * 10) / 10,
};

const payload = {
  runDate: new Date().toLocaleString('en-IN', { hour12: false }),
  device: config('zillit.deviceName', 'ZILLIT_DEVICE_NAME') || 'emulator',
  reportedBy: process.env.USER || hostname(),
  summary,
  tests,
};

console.log(`Pushing ${tests.length} test rows to the sheet ` +
    `(${summary.passed} passed, ${summary.failed} failed, ${summary.skipped} skipped)…`);

const res = await fetch(webhook, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(payload),
  redirect: 'follow', // Apps Script replies via redirect
});
const text = await res.text();
if (res.ok) {
  console.log('✓ Report uploaded to Google Sheet.');
} else {
  console.error('✗ Upload failed (' + res.status + '): ' + text.slice(0, 300));
  process.exit(1);
}
