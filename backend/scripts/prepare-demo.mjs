// Brings the team hotel into the known demo state (PLAN.md, checkpoint 15:40).
//
// Usage, from backend/:
//   node scripts/prepare-demo.mjs           dry run: checks the sandbox and lists the steps, changes nothing
//   node scripts/prepare-demo.mjs --prepare applies the steps without a reset (the safe choice before the demo)
//   node scripts/prepare-demo.mjs --reset   resets the hotel to its seed, then runs the steps (may reshuffle reservations)
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

// The demo family: Kuznetsov, room 222. A reset can hand reservation ids to other guests (seen on the event day),
// so the checks verify the family, and --prepare applies the steps WITHOUT a reset.
const DEMO_RESERVATION = 'R-203156';
const DEMO_ROOM = '222';
const DEMO_LAST_NAME = 'Kuznetsov';
// Waiter's table and bartender's table: B4,B6 for the rehearsal, --tables=B7,B8 for the demo itself.
const DEMO_TABLES = (process.argv.find((a) => a.startsWith('--tables='))?.slice(9) ?? 'B4,B6').split(',');
const POOL_BAR = 4;

// Calls that set up the demo story. Each step uses `call`; POS calls pass { pos: true }.
const steps = [
  {
    name: `PEANUTS on the PMS profile of room ${DEMO_ROOM} (the allergy recorded on the parent)`,
    run: async () => {
      const reservation = (await call('GET', `/v1/pms/reservations/${DEMO_RESERVATION}`)).reservation;
      await call('PATCH', `/v1/pms/guests/${reservation.guest.id}`, { body: { allergies: ['PEANUTS'] } });
    },
  },
];

// Read-only checks of the demo state (DEMO.md, preparation): reported on a dry run, required after --prepare or --reset.
const checks = [
  {
    name: `Room ${DEMO_ROOM} (${DEMO_LAST_NAME}) in house, only PEANUTS in the PMS profile`,
    run: async () => {
      const reservation = (await call('GET', `/v1/pms/reservations/${DEMO_RESERVATION}`)).reservation;
      const allergies = reservation.guest?.allergies ?? [];
      if (reservation.status !== 'IN_HOUSE' || reservation.roomNumber !== DEMO_ROOM || reservation.guest?.lastName !== DEMO_LAST_NAME) {
        throw new Error(`${DEMO_RESERVATION} is ${reservation.status}, room ${reservation.roomNumber}, ${reservation.guest?.lastName}`);
      }
      if (allergies.join() !== 'PEANUTS') throw new Error(`PMS allergies are ${allergies.join(', ') || 'none'}`);
    },
  },
  {
    name: `No open check at the Pool Bar tables ${DEMO_TABLES.join(', ')}`,
    run: async () => {
      const open = ((await call('GET', `/v1/pos/checks?rvc-on=${POOL_BAR}`, { pos: true })).elements ?? [])
        .filter((c) => DEMO_TABLES.includes(c.table));
      if (open.length) {
        throw new Error(`open: ${open.map((c) => `check ${c.checkNumber ?? c.checkNum} table ${c.table}`).join(', ')}`);
      }
    },
  },
];

async function runChecks(required) {
  let failed = 0;
  for (const check of checks) {
    try {
      await check.run();
      console.log(`ok: ${check.name}`);
    } catch (error) {
      failed += 1;
      console.log(`NOT READY: ${check.name} — ${error.message}`);
    }
  }
  if (required && failed) {
    console.log(`${failed} check(s) failed: the demo state is not ready.`);
    process.exit(1);
  }
}

const reset = process.argv.includes('--reset');
const prepare = process.argv.includes('--prepare');
const health = await (await fetch(`${base}/health`)).json();
const before = (await call('GET', '/v1/pms/hotel')).hotel;
console.log(`Sandbox ${health.status}; hotel "${before.name}", business date ${before.businessDate}`);
console.log(`${steps.length} prep step(s): ${steps.map((s) => s.name).join('; ') || 'none yet'}`);
if (!reset && !prepare) {
  await runChecks(false);
  console.log('Dry run: nothing changed. --prepare applies the steps; --reset resets the hotel first (may reshuffle reservations).');
  process.exit(0);
}

if (reset) {
  console.log('Resetting the hotel (up to 30 s)...');
  await call('POST', '/v1/sandbox/reset', { timeoutMs: 45000 });
}
for (const step of steps) {
  await step.run();
  console.log(`done: ${step.name}`);
}
await runChecks(true);
const after = (await call('GET', '/v1/pms/hotel')).hotel;
console.log(`Demo state ready: "${after.name}", business date ${after.businessDate}`);
