# PLAN.md — implementation plan: Allergy passport

> Topic, rules and cut list: `ai-rules/PROJECT-CONTEXT.md` §3.
> Agents: work by task ID; read its row first and touch only the files of that task's feature.

## Team mode

`[x]` 4 people (Nikolai Ivliev replaces Aleksandr). Roles swapped on the day: Sergey and Nikolai build the frontend, Gennadii and Erlan the backend.

| Slot | Owner | Scope |
|---|---|---|
| **BE-A** | Gennadii | API contract, sandbox integration (search, menu, orders, kitchen feed, SSE), demo scripts |
| **BE-B** | Erlan | Allergen engine, passport + PMS sync, kitchen tickets, safety log, backend demo-path test |
| **FE-A** | Sergey | Frontend skeleton and routes; waiter tablet; kitchen display |
| **FE-B** | Nikolai | API types and client; front desk passport; manager log; design; Playwright, slides, video; clicks in the demo |

**Tech lead: Sergey** — this file, checkpoints, scope and cut decisions, the hotel reset, the pitch.

## Demo path — Pool Bar (rvc 4), Kuznetsov family, room 518 (R-203844)

| # | The jury sees | Screen | Our API | Sandbox calls |
|---|---|---|---|---|
| 1 | The PMS says only "PEANUTS" on Mikhail's profile. The front desk records the real picture: son **Misha, 7 — PEANUTS, severe**; wife **Anna — MILK, intolerance (new during the stay)**. Save → "PMS updated". | Front desk | `GET /api/front-desk/arrivals`, `GET /api/reservations?query=518`, `GET`/`PUT /api/passports/R-203844` | `GET /v1/pms/reservations?roomNumber=518`, `PATCH /v1/pms/guests/{guestId}` (allergies = PEANUTS, MILK) |
| 2 | Pool Bar waiter opens room 518: badges per person. Chicken satay, Som tam, Pad thai red for Misha; Espresso green, but its **Whole milk** condiment red for Anna; Spring rolls and Kids nuggets green. | Waiter | `GET /api/reservations?query=518`, `GET /api/outlets/4/menu?reservationId=R-203844` | `GET /v1/pos/dict/pms/lookup`, `GET /v1/pos/dict/menu?rvc-on=4` |
| 3 | Misha: Kids nuggets + Coconut ice cream. Anna: Espresso → whole milk warning → switch to oat milk. Mikhail: Chicken satay **for himself** → "Misha (severe) is at the table" → reason "adult, served separately" → Send. | Waiter | `POST /api/orders` | `POST /v1/pos/checks?rvc-on=4` (table, `reservationId`) |
| 4 | Within ~3 s the kitchen display shows the ticket: satay line red "PEANUTS — not for Misha, 7 · override W02: adult, served separately"; the cook acknowledges. Then a bartender types **Som tam** for room 518 straight into the POS → a red ticket "not checked at ordering" jumps to the top. | Kitchen (Pool Bar) | `GET /api/kitchen/4/tickets`, `POST /api/kitchen/tickets/{id}/ack`, SSE `GET /api/stream` | `GET /v1/events` (`pos.check.round_sent`); the bartender's order via `scripts/pos-order.mjs` |
| 5 | The manager sees the chain with times: passport change → warning → override → ticket → acknowledgement (time to react) → unchecked POS order caught; KPIs of the shift. | Manager | `GET /api/safety-log`, `GET /api/safety-log/summary`, SSE | — |

## API contract — frozen at 12:30

Changes after 12:30 only through BE-A (`CLAUDE.md` §5). Enums: `Allergen` = the 14 EU allergens of the sandbox; `Severity` = `INTOLERANCE` | `SEVERE`; `Verdict` = `SAFE` | `WARNING` (intolerance) | `DANGER` (severe).
Shared shape `DangerFor` = `{memberId, memberName, allergen, severity}`.

| Method and path | Request | Response | Owner | Status |
|---|---|---|---|---|
| `GET /api/reservations?query=` | room number (exact) or last-name prefix (2+ chars) | `ReservationSummary[]`: `reservationId, roomNumber, guestName, arrivalDate, departureDate, adults, children, language, pmsAllergens[], hasPassport` — in-house and today's arrivals | BE-A | stub |
| `GET /api/front-desk/arrivals` | — | `ReservationSummary[]`: today's arrivals and in-house guests with allergies or children, without a passport first | BE-A | stub |
| `GET /api/passports/{reservationId}` | — | `Passport`: `reservationId, roomNumber, guestName, notes, pmsAllergens[], members[]: {id, name, age, primary, allergies[]: {allergen, severity}}, updatedAt, updatedBy`. No passport yet → the primary guest with the PMS allergens as `SEVERE` (most restrictive) | BE-B | stub |
| `PUT /api/passports/{reservationId}` | `{members[]: {id?, name, age?, primary, allergies[]}, updatedBy}` | `Passport`; side effect: PMS profile allergies = union of members | BE-B | stub |
| `GET /api/outlets` | — | `{rvc, name, kind, tables[]}[]` | BE-A | stub |
| `GET /api/outlets/{rvc}/menu?reservationId=` | reservation optional | `MenuItem[]`: `code, name, group, price, allergens[], verdict, dangerFor[], safeAlternatives[] (codes), condimentGroups[]: {code, name, minSelect, maxSelect, options[]: {code, name, price, allergens[], verdict, dangerFor[]}}`. Without a reservation every verdict is `SAFE` | BE-A (verdicts by BE-B's engine) | stub |
| `POST /api/orders` | `{rvc, reservationId?, table?, waiterCode, lines[]: {code, quantity, condimentCodes[], forMemberId? (none = table), overrideReason?}}` | `{checkNumber, checkSeq, lines[]: {code, verdict, dangerFor[]}}`. Errors: 409 `ALLERGEN_BLOCKED` (severe allergen for the member it is for), 400 `OVERRIDE_REASON_REQUIRED` | BE-A | stub |
| `GET /api/kitchen/{rvc}/tickets` | — | `KitchenTicket[]`: `id, rvc, checkNumber, table, roomNumber, guestName, sentAt, source (CHECKED / UNCHECKED / UNLINKED), status (NEW / ACKNOWLEDGED / ESCALATED), dangerous, lines[]: {name, quantity, condiments[], allergens[], verdict, dangerFor[], forMemberName?, overrideReason?, waiterCode?}, acknowledgement?: {action, cook, at}` — dangerous and unacknowledged first | BE-B | stub |
| `POST /api/kitchen/tickets/{id}/ack` | `{action: PREPARED_SEPARATELY / DISH_CHANGED / WAITER_CALLED, cook}` | `KitchenTicket` | BE-B | stub |
| `GET /api/safety-log?rvc=&dangerOnly=` | filters optional | `LogEntry[]`: `id, at, type (PASSPORT_UPDATED / ORDER_WARNING / ORDER_BLOCKED / OVERRIDE / TICKET_DANGER / TICKET_ACKNOWLEDGED / TICKET_ESCALATED), rvc, outletName, reservationId, roomNumber, memberName, item, allergen, actor, reason, minutesToAck` — newest first | BE-B | stub |
| `GET /api/safety-log/summary` | — | `{ordersChecked, dangersCaught, overrides, blocked, unacknowledged, avgMinutesToAck}` | BE-B | stub |
| `GET /api/stream` | — | Server-sent events: `ticket` (`KitchenTicket`), `log` (`LogEntry`), `passport` (`Passport`) | BE-A | stub |

## Tasks

Statuses: `TODO` · `DOING` · `DONE` · `CUT`. Each person edits only their own section.

### BE-A — Gennadii

| ID | Task | Demo step | Depends on | Status |
|---|---|---|---|---|
| BE-A1 | New team key in `.env`, live smoke test, slow-sandbox fix | — | — | DONE |
| BE-A2 | Whole API contract as stub controllers + records with fixed data from the demo story; springdoc up (by 12:30) | all | — | TODO |
| BE-A3 | Reservation search + front-desk arrivals + outlets (filtered PMS calls, POS lookup) | 1, 2 | BE-A2 | TODO |
| BE-A4 | Menu with verdicts (menu cached 5 min, engine from BE-B2) + `POST /api/orders` → POS check linked to the reservation, rules for "for whom" and overrides, log entries | 2, 3 | BE-B2 | TODO |
| BE-A5 | Kitchen feed: listener on `pos.check.round_sent` → engine → ticket (BE-B4 storage) → SSE `/api/stream` | 4 | BE-B4 | TODO |
| BE-A6 | `scripts/pos-order.mjs` (bartender order straight into the POS, PIN from `.env`) + demo prep steps in `prepare-demo.mjs` | 4 | — | TODO |

### BE-B — Erlan

| ID | Task | Demo step | Depends on | Status |
|---|---|---|---|---|
| BE-B1 | Storage: passport members + allergies, kitchen tickets, safety log; Liquibase `200_…` | 1, 4, 5 | — | TODO |
| BE-B2 | Allergen engine (pure code + parameterized unit tests): item + chosen condiments vs members → verdict, `dangerFor`; "for whom" rules; safe alternatives from the same group | 2, 3, 4 | — | TODO |
| BE-B3 | Passport GET/PUT, default from the PMS profile, PMS sync (union), `guest.updated` listener, log entry | 1 | BE-B1 | TODO |
| BE-B4 | Kitchen tickets store + acknowledge + 3-minute escalation (scheduled), safety log + summary | 4, 5 | BE-B1 | TODO |
| BE-B5 | Backend demo-path test over WireMock fixtures (after 14:00) | all | BE-A4, BE-A5 | TODO |

### FE-A — Sergey

| ID | Task | Demo step | Depends on | Status |
|---|---|---|---|---|
| FE-A1 | Frontend skeleton per `FRONTEND.md` §6 block A (defaults unless objections); routes `/staff/front-desk`, `/staff/waiter`, `/staff/kitchen`, `/staff/manager`; device setup (outlet, waiter W01–W05) | — | — | TODO |
| FE-A2 | Waiter: party search, badges per person, menu by group with verdict colours and "only safe" toggle | 2 | FE-B1 | TODO |
| FE-A3 | Waiter: condiment picker with verdicts, "for whom" per line, warning and override-reason dialog, blocked state, safe alternatives, cart → send | 3 | FE-A2 | TODO |
| FE-A4 | Kitchen display: tickets live via SSE, red lines "not for X", source badges, acknowledge actions, timer since sent | 4 | FE-B1 | TODO |

### FE-B — Nikolai

| ID | Task | Demo step | Depends on | Status |
|---|---|---|---|---|
| FE-B0 | Design: allergen chips with icons for the 14 allergens, severity and verdict colours, party badge — in `shared/` | all | FE-A1 | TODO |
| FE-B1 | Generated API types + client, SSE hook, error messages by `code` | — | BE-A2 | TODO |
| FE-B2 | Front desk: arrivals worklist, search, passport editor (members, allergen picker, severity), save with "PMS updated" | 1 | FE-B1 | TODO |
| FE-B3 | Manager: KPIs + live timeline with filters | 5 | FE-B1 | TODO |
| FE-B4 | Playwright happy path (after 14:30), slides, screen recording at 15:15 | all | UI stable | TODO |
| FE-B5 | Stretch: guest passport page by QR in the guest's language | — | everything else DONE | TODO |

## Checkpoints — 5 minutes, one sentence each: done / doing / blocked

| Time | Goal |
|---|---|
| 12:30 | API contract stubs pushed; frontend skeleton pushed; engine rules agreed |
| 13:00 | Before lunch: each agent gets a bounded 40-minute task |
| 13:45 | Status after lunch |
| **14:15** | **First end-to-end pass of the demo path on the live sandbox, however ugly** |
| 14:30 | Polish; slides start |
| 15:00 | Whatever does not work goes to the cut list |
| 15:15 | Feature freeze; clean clone of `main` on the demo laptop (App key); screen recording |
| 15:40 | `prepare-demo.mjs --reset` (tech lead only) + empty `backend/data/`; two rehearsals done |
| 16:00 | Demo: Sergey speaks, Nikolai clicks; windows side by side: waiter left, kitchen right |

Blocked for more than 15 minutes: say so at once.

## Shared resources

- **Dev key: 10 requests per second for the team.** `SANDBOX_RATE_LIMIT=2` in every developer's `backend/.env`. The demo laptop uses the App key with the default 8.
- **Hotel data is shared.** Work only on your reserved rooms; only the tech lead resets, after telling the team.
- **No language model** in this topic.
- **Demo laptop:** TODO (whose).

| Who | Rooms (reservation) |
|---|---|
| Demo path — nobody touches | 518 (R-203844, Kuznetsov, PEANUTS); backup 729 (R-203981, Thomas, GLUTEN / FISH / MUSTARD) |
| BE-A | 633 (R-203423), 822 (R-203830) |
| BE-B | 223 (R-203478), 539 (R-203804) |
| FE-A | 139 (R-203358) |
| FE-B | 227 (R-203363) |

## Demo prep (scripts: BE-A6; run at 15:40 by the tech lead)

1. `node scripts/prepare-demo.mjs --reset` — the seed restores 518 with only PEANUTS on the PMS profile.
2. Stop the backend, delete `backend/data/`, start it again: no passports, no tickets, empty log.
3. Check: room 518 is in house; no open check on the Pool Bar table used in the demo.
4. Open four windows: front desk, waiter (Pool Bar, W02), kitchen (Pool Bar), manager.

## Cut list

- Guest self-service passport (stretch FE-B5 only).
- "May contain" and cross-contamination.
- Printing, payment and room charge, login, translation of staff screens.
