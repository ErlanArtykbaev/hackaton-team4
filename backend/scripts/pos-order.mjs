// Demo step 4b (PLAN.md): a bartender types an order straight into the POS, skipping our tablet. The POS emits
// `pos.check.round_sent`, and the kitchen display shows the round as "not checked at ordering".
//
// Usage, from backend/:
//   node scripts/pos-order.mjs                 dry run: resolves the outlet, the item and the order type, sends nothing
//   node scripts/pos-order.mjs --send          opens a real check in the POS with one round
// Options (defaults are the demo story): --reservation R-203844 --table B5 --item "Som tam" --rvc 4
//
// Logs in to the POS with POS_BARTENDER_PIN from backend/.env (a bartender from the organizers' handout); falls back
// to SANDBOX_PIN with a warning. The table must have no open check (OCCUPIED_TABLE): the hotel reset clears them.

import { readFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const backendDir = join(dirname(fileURLToPath(import.meta.url)), '..')
const env = Object.fromEntries(
  readFileSync(join(backendDir, '.env'), 'utf8')
    .split(/\r?\n/)
    .filter((line) => line && !line.startsWith('#') && line.includes('='))
    .map((line) => [line.slice(0, line.indexOf('=')).trim(), line.slice(line.indexOf('=') + 1).trim()]),
)

function option(name, fallback) {
  const index = process.argv.indexOf(`--${name}`)
  return index > 0 && process.argv[index + 1] ? process.argv[index + 1] : fallback
}

const reservationId = option('reservation', 'R-203844')
const table = option('table', 'B5')
const itemName = option('item', 'Som tam')
const rvc = Number(option('rvc', '4'))
const send = process.argv.includes('--send')
const base = env.SANDBOX_BASE_URL
const deviceId = 'team4-bartender'

let pin = env.POS_BARTENDER_PIN
if (!pin) {
  console.warn('POS_BARTENDER_PIN is not set in backend/.env: logging in with SANDBOX_PIN instead of a bartender.')
  pin = env.SANDBOX_PIN
}

let token = ''
async function call(method, path, { body, pos = true } = {}) {
  const headers = { Accept: 'application/json', 'X-API-KEY': env.SANDBOX_API_KEY, 'X-Device-Id': deviceId }
  if (pos) headers.Authorization = `Bearer ${await posToken()}`
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  const response = await fetch(base + path, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
    signal: AbortSignal.timeout(20000),
  })
  const text = await response.text()
  const json = text ? JSON.parse(text) : null
  if (!response.ok) throw new Error(`${method} ${path}: ${response.status} ${json?.code ?? ''} ${json?.message ?? text}`)
  return json
}

async function posToken() {
  if (!token) {
    const login = await call('POST', `/v1/auth/token?device-id=${deviceId}`, {
      pos: false,
      body: { pinCode: pin, accessCode: env.SANDBOX_ACCESS_CODE },
    })
    token = login.token
  }
  return token
}

const outlets = (await call('GET', '/v1/pos/dict/outlets')).elements
const outlet = outlets.find((o) => o.objectNumber === rvc)
if (!outlet) throw new Error(`No outlet with rvc ${rvc}`)
const orderType = outlet.orderTypes?.[0]?.code
if (!orderType) throw new Error(`Outlet ${outlet.name} offers no order type`)
if (outlet.tables?.length && !outlet.tables.includes(table)) {
  throw new Error(`Table ${table} is not in ${outlet.name}: ${outlet.tables.join(', ')}`)
}

const menu = (await call('GET', `/v1/pos/dict/menu?rvc-on=${rvc}&size=500`)).elements
const wanted = itemName.toLowerCase()
const item =
  menu.find((m) => m.name.toLowerCase() === wanted) ?? menu.find((m) => m.name.toLowerCase().startsWith(wanted))
if (!item) throw new Error(`No "${itemName}" on the ${outlet.name} menu`)

const check = { table, covers: 1, orderType, reservationId, details: [{ code: item.code, quantity: 1 }] }
console.log(`${outlet.name} (rvc ${rvc}), table ${table}, ${reservationId}: 1 x ${item.name} (${item.code})`)
if (!send) {
  console.log('Dry run: nothing sent. Add --send to open the check in the POS.')
  process.exit(0)
}

const created = await call('POST', `/v1/pos/checks?rvc-on=${rvc}`, { body: check })
const opened = created.check ?? created
console.log(`Sent to the kitchen: check ${opened.checkNumber ?? opened.checkNum ?? '?'} (seq ${opened.checkSeq ?? '?'})`)
