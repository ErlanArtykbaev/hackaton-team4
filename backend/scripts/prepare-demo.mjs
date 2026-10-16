// Brings the team hotel into the known demo state (PLAN.md, checkpoint 15:40).
//
// Usage, from backend/:
//   node scripts/prepare-demo.mjs           dry run: checks the sandbox and lists the steps, changes nothing
//   node scripts/prepare-demo.mjs --reset   resets the hotel to its seed, then runs the steps
//
// Reset affects the whole team (only BE-A runs it, after telling everyone), is allowed once per minute and takes
// up to 30 s. Before the event the key points to the practice hotel shared by ALL teams: never reset it then.
// The seed is deterministic relative to today, so the same steps give the same demo state every time.

import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const backendDir = join(dirname(fileURLToPath(import.meta.url)), '..');
const env = Object.fromEntries(
  readFileSync(join(backendDir, '.env'), 'utf8')
    .split(/\r?\n/)
    .filter((line) => line && !line.startsWith('#') && line.includes('='))
    .map((line) => [line.slice(0, line.indexOf('=')).trim(), line.slice(line.indexOf('=') + 1).trim()]),
);
const base = env.SANDBOX_BASE_URL;
const deviceId = env.SANDBOX_DEVICE_ID || 'team4-backend';
let token = '';

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

async function call(method, path, { body, pos = false, headers = {}, timeoutMs = 10000 } = {}) {
  await sleep(150);
  const h = { Accept: 'application/json', 'X-API-KEY': env.SANDBOX_API_KEY, ...headers };
  if (pos) h.Authorization = `Bearer ${await posToken()}`;
  if (body !== undefined) h['Content-Type'] = 'application/json';
  const response = await fetch(base + path, {
    method, headers: h, body: body === undefined ? undefined : JSON.stringify(body), signal: AbortSignal.timeout(timeoutMs),
  });
  const text = await response.text();
  const json = text ? JSON.parse(text) : null;
  if (!response.ok) throw new Error(`${method} ${path}: ${response.status} ${json?.code ?? ''} ${json?.message ?? text}`);
  return json;
}

async function posToken() {
  if (!token) {
    const login = await call('POST', `/v1/auth/token?device-id=${deviceId}`, {
      body: { pinCode: env.SANDBOX_PIN, accessCode: env.SANDBOX_ACCESS_CODE },
    });
    token = login.token;
  }
  return token;
}

// Filled on the event day (PLAN.md task BE-A3): topic-specific calls that set up the demo story. Each step uses
// `call`; POS calls pass { pos: true }; folio postings and room charges need a fresh postingGuid
// (import { randomUUID } from 'node:crypto').
const steps = [
  // { name: 'Room 612 dirty with a VIP arrival', run: () => call('PATCH', '/v1/pms/rooms/612', { body: { status: 'DIRTY' } }) },
];

const reset = process.argv.includes('--reset');
const health = await (await fetch(`${base}/health`)).json();
const before = (await call('GET', '/v1/pms/hotel')).hotel;
console.log(`Sandbox ${health.status}; hotel "${before.name}", business date ${before.businessDate}`);
console.log(`${steps.length} prep step(s): ${steps.map((s) => s.name).join('; ') || 'none yet'}`);
if (!reset) {
  console.log('Dry run: nothing changed. Run with --reset to reset the hotel and apply the steps.');
  process.exit(0);
}

console.log('Resetting the hotel (up to 30 s)...');
await call('POST', '/v1/sandbox/reset', { timeoutMs: 45000 });
for (const step of steps) {
  await step.run();
  console.log(`done: ${step.name}`);
}
const after = (await call('GET', '/v1/pms/hotel')).hotel;
console.log(`Demo state ready: "${after.name}", business date ${after.businessDate}`);
