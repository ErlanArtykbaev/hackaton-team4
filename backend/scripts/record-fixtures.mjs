// Records sandbox responses into backend/src/test/resources/sandbox/ for WireMock tests (ai-rules/TESTING.md §3).
//
// Usage, from backend/:   node scripts/record-fixtures.mjs
// Reads SANDBOX_* values from backend/.env. Tokens are replaced with "test-token"; keys are never written.
//
// Writes to the hotel are kept small and reverted where the API allows it: PATCH calls send the values already
// stored, the folio test charge is followed by an adjustment. One room-service check is opened and charged to a
// room, which leaves a real POS line on that guest's folio.

import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { randomUUID } from 'node:crypto';
import { fileURLToPath } from 'node:url';

const backendDir = join(dirname(fileURLToPath(import.meta.url)), '..');
const outDir = join(backendDir, 'src', 'test', 'resources', 'sandbox');
const env = Object.fromEntries(
  readFileSync(join(backendDir, '.env'), 'utf8')
    .split(/\r?\n/)
    .filter((line) => line && !line.startsWith('#') && line.includes('='))
    .map((line) => [line.slice(0, line.indexOf('=')).trim(), line.slice(line.indexOf('=') + 1).trim()]),
);
const base = env.SANDBOX_BASE_URL;
const key = env.SANDBOX_API_KEY;
const deviceId = env.SANDBOX_DEVICE_ID || 'team4-backend';
if (!base || !key || !env.SANDBOX_ACCESS_CODE || !env.SANDBOX_PIN) {
  throw new Error('Fill SANDBOX_BASE_URL, SANDBOX_API_KEY, SANDBOX_ACCESS_CODE and SANDBOX_PIN in backend/.env');
}

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));
let token = '';

async function call(method, path, { body, auth = 'key', headers = {}, expect } = {}) {
  await sleep(150); // stay well below 10 requests per second
  const h = { Accept: 'application/json', ...headers };
  if (auth === 'key') h['X-API-KEY'] = key;
  if (auth === 'pos') h.Authorization = `Bearer ${token}`;
  if (auth === 'bad-pos') h.Authorization = 'Bearer not-a-token';
  if (body !== undefined) h['Content-Type'] = 'application/json';
  const response = await fetch(base + path, { method, headers: h, body: body === undefined ? undefined : JSON.stringify(body) });
  const text = await response.text();
  const json = text ? JSON.parse(text) : null;
  if (expect !== undefined && response.status !== expect) {
    throw new Error(`${method} ${path}: expected ${expect}, got ${response.status} ${text.slice(0, 300)}`);
  }
  console.log(`${response.status} ${method} ${path}`);
  return json;
}

function save(name, json) {
  const file = join(outDir, `${name}.json`);
  mkdirSync(dirname(file), { recursive: true });
  writeFileSync(file, JSON.stringify(json, null, 2) + '\n');
}

// ---------------------------------------------------------------- PMS
const hotel = await call('GET', '/v1/pms/hotel', { expect: 200 });
save('pms/hotel', hotel);
const today = hotel.hotel.businessDate;
const plusDays = (days) => new Date(Date.parse(today) + days * 86400000).toISOString().slice(0, 10);

save('pms/rooms', await call('GET', '/v1/pms/rooms?size=200', { expect: 200 }));

const inHouse = await call('GET', '/v1/pms/reservations?status=IN_HOUSE&size=200', { expect: 200 });
save('pms/reservations-in-house', inHouse);
save('pms/reservations-arrivals', await call('GET', `/v1/pms/reservations?status=BOOKED&arrivalFrom=${today}&arrivalTo=${today}&size=200`, { expect: 200 }));

const guest = inHouse.elements.find((r) => r.postingAllowed && r.creditLimit - r.balance > 200 && r.roomNumber);
if (!guest) throw new Error('No in-house reservation with room-charge headroom; reset the practice hotel and retry');
const resId = guest.id;

save('pms/reservation', await call('GET', `/v1/pms/reservations/${resId}`, { expect: 200 }));
save('pms/reservation-patched', await call('PATCH', `/v1/pms/reservations/${resId}`, { body: { departureTime: guest.departureTime }, expect: 200 }));
save('pms/guest-patched', await call('PATCH', `/v1/pms/guests/${guest.guest.id}`, { body: { allergies: guest.guest.allergies }, expect: 200 }));
const rooms = await call('GET', `/v1/pms/rooms?size=200`, { expect: 200 });
const room = rooms.elements.find((r) => r.number === guest.roomNumber);
save('pms/room-patched', await call('PATCH', `/v1/pms/rooms/${room.number}`, { body: { attendantId: room.attendantId }, expect: 200 }));

save('pms/folio', await call('GET', `/v1/pms/reservations/${resId}/folio`, { expect: 200 }));
const charge = { description: 'Fixture recording test charge', amount: 1.0, postingGuid: randomUUID() };
save('pms/folio-posted', await call('POST', `/v1/pms/reservations/${resId}/folio`, { body: charge, expect: 201 }));
save('pms/folio-posted-repeat', await call('POST', `/v1/pms/reservations/${resId}/folio`, { body: charge, expect: 200 }));
await call('POST', `/v1/pms/reservations/${resId}/folio`, {
  body: { description: 'Fixture recording test charge, reverted', amount: -1.0, postingGuid: randomUUID() }, expect: 201,
});

save('pms/forecast', await call('GET', `/v1/pms/forecast?from=${today}&to=${plusDays(13)}`, { expect: 200 }));

// ---------------------------------------------------------------- POS
const login = await call('POST', `/v1/auth/token?device-id=${deviceId}`, {
  body: { pinCode: env.SANDBOX_PIN, accessCode: env.SANDBOX_ACCESS_CODE }, expect: 201,
});
token = login.token;
save('pos/token', { ...login, token: 'test-token' });

const outlets = await call('GET', '/v1/pos/dict/outlets', { auth: 'pos', expect: 200 });
save('pos/outlets', outlets);
for (const outlet of outlets.elements) {
  save(`pos/menu-${outlet.objectNumber}`, await call('GET', `/v1/pos/dict/menu?rvc-on=${outlet.objectNumber}`, { auth: 'pos', expect: 200 }));
}
const roomService = outlets.elements.find((o) => o.kind === 'ROOM_SERVICE');
const rvc = roomService.objectNumber;
save(`pos/menu-${rvc}-${hotel.hotel.localLanguage}`, await call('GET', `/v1/pos/dict/menu?rvc-on=${rvc}`, {
  auth: 'pos', headers: { 'Accept-Language': hotel.hotel.localLanguage }, expect: 200,
}));
save('pos/lookup', await call('GET', `/v1/pos/dict/pms/lookup?rvc-on=${rvc}&query=${guest.roomNumber}`, { auth: 'pos', expect: 200 }));
save('pos/checks-open', await call('GET', '/v1/pos/checks?status=OPEN&size=200', { auth: 'pos', expect: 200 }));
save('pos/checks-closed', await call('GET', '/v1/pos/checks?status=CLOSED&size=3', { auth: 'pos', expect: 200 }));

const menu = (await call('GET', `/v1/pos/dict/menu?rvc-on=${rvc}`, { auth: 'pos', expect: 200 })).elements;
const simple = menu
  .filter((item) => item.type === 'ITEM' && item.price > 0 && !(item.condiments || []).some((g) => g.minSelect > 0))
  .sort((a, b) => a.price - b.price);
const [first, second] = simple;
const opened = await call('POST', `/v1/pos/checks?rvc-on=${rvc}`, {
  auth: 'pos',
  headers: { 'X-Device-Id': deviceId },
  body: { orderType: roomService.orderTypes[0].code, checkName: guest.roomNumber, reservationId: resId, details: [{ code: first.code, quantity: 1 }] },
  expect: 201,
});
save('pos/check-opened', opened);
const { checkNumber, checkSeq } = opened.check;
save('pos/check', await call('GET', `/v1/pos/checks/${checkNumber}?rvc-on=${rvc}&seq=${checkSeq}`, { auth: 'pos', expect: 200 }));
const withRound = await call('PUT', `/v1/pos/checks/${checkNumber}?rvc-on=${rvc}&seq=${checkSeq}`, {
  auth: 'pos', body: { details: [{ code: second.code, quantity: 1 }] }, expect: 200,
});
save('pos/check-round', withRound);
const roomCharge = roomService.tenders.find((t) => t.type === 'ROOM_CHARGE');
const payment = {
  tender: roomCharge.code, tenderAmount: withRound.check.totalDueRaw, paymentAccount: resId, postingGuid: randomUUID(),
};
save('pos/check-paid', await call('PUT', `/v1/pos/checks/${checkNumber}?rvc-on=${rvc}&seq=${checkSeq}`, { auth: 'pos', body: payment, expect: 200 }));

// ---------------------------------------------------------------- Events
save('events/events', await call('GET', '/v1/events?since=0&limit=100', { expect: 200 }));

// ---------------------------------------------------------------- Errors (all refused by the sandbox, nothing changes)
save('errors/401-unauthorized', await call('GET', '/v1/pms/hotel', { auth: 'none', expect: 401 }));
save('errors/401-pos-unauthorized', await call('GET', '/v1/pos/dict/outlets', { auth: 'bad-pos', expect: 401 }));
save('errors/401-auth-invalid', await call('POST', `/v1/auth/token?device-id=${deviceId}`, {
  body: { pinCode: '0000', accessCode: env.SANDBOX_ACCESS_CODE }, expect: 401,
}));
save('errors/404-not-found', await call('GET', '/v1/pms/reservations/R-000000', { expect: 404 }));
save('errors/400-validation-error', await call('GET', '/v1/pms/forecast?from=2026-13-01&to=2026-13-02', { expect: 400 }));
save('errors/400-invalid-status-transition', await call('PATCH', `/v1/pms/reservations/${resId}`, { body: { status: 'IN_HOUSE' }, expect: 400 }));
save('errors/400-check-closed', await call('PUT', `/v1/pos/checks/${checkNumber}?rvc-on=${rvc}&seq=${checkSeq}`, {
  auth: 'pos', body: { ...payment, postingGuid: randomUUID(), tenderAmount: 1 }, expect: 400,
}));
const checkedOut = await call('GET', '/v1/pms/reservations?status=CHECKED_OUT&size=1', { expect: 200 });
save('errors/400-guest-not-in-house', await call('POST', `/v1/pms/reservations/${checkedOut.elements[0].id}/folio`, {
  body: { description: 'Must be refused', amount: 1.0, postingGuid: randomUUID() }, expect: 400,
}));

console.log(`\nFixtures written to ${outDir}`);
