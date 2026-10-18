# PLAN.md — implementation plan: Allergy passport

> Topic, domain rules and cut list: `ai-rules/PROJECT-CONTEXT.md` §3 (rules confirmed by the hosts).
> Agents: work by task ID; read its row first and touch only your own packages or folders (below).

## Team

4 people; roles swapped on the day: Sergey and Nikolai build the frontend, Gennadii and Erlan the backend.

| Slot | Owner | Scope | Owns (backend `com.hackathon.team4.*`, frontend `frontend/src/*`) |
|---|---|---|---|
| **BE-A** | Gennadii | API contract, sandbox integration, live stream, demo scripts | `reservation`, `menu`, `order`, `stream`, `kitchen.feed`, `backend/scripts` |
| **BE-B** | Erlan | Allergen engine, passport, kitchen tickets, safety log, demo-path test | `allergen`, `passport`, `kitchen` (except `kitchen.feed`), `safetylog` |
| **FE-A** | Sergey | Skeleton, waiter tablet, kitchen display | `features/waiter`, `features/kitchen`, `App.tsx`, `main.tsx`, `vite.config.ts`, `package.json` |
| **FE-B** | Nikolai | Design, API types, front desk, manager log, Playwright, video; clicks in the demo | `shared`, `api`, `features/front-desk`, `features/manager`, `frontend/e2e` |

**Tech lead: Sergey** — this file, checkpoints, scope and cut decisions, the hotel reset, slides and the pitch.

## Agreements between the backend developers (fixed before coding)

- **Allergen engine** (Erlan provides, Gennadii calls), package `allergen`:
  - `checkItem(party, itemAllergens)` → `verdict` + `dangerFor[]`; an empty party (walk-in) → `UNKNOWN`;
  - `checkLine(party, itemAllergens, forMemberId or null for the table)` → `verdict`, `dangerFor[]`, `requirement`: `NONE` | `WAITER_CONFIRMATION` | `GUEST_CONSENT` (rules in `PROJECT-CONTEXT.md` §3);
  - `safeAlternatives(menu, party, item)` → codes of `SAFE` items in the same group.
  - `itemAllergens` always include the allergens of the chosen condiments.
- **Spring events for the live stream** (Erlan publishes, Gennadii streams over SSE): `TicketChanged`, `LogAppended`, `PassportChanged`.

## Demo path — Pool Bar (rvc 4), Kuznetsov family, room 518 (R-203844)

| # | The jury sees | Screen | Our API | Sandbox calls |
|---|---|---|---|---|
| 1 | Check-in card per person: **Mikhail** (adult), **Anna** (adult), **Misha, 7 — PEANUTS, severe**. Save → "PMS updated". | Front desk | `GET /api/front-desk/arrivals`, `GET /api/reservations?query=518`, `GET`/`PUT /api/passports/R-203844` | `GET /v1/pms/reservations?roomNumber=518`, `PATCH /v1/pms/guests/{guestId}` |
| 2 | Pool Bar waiter finds "Kuznetsov": badges per person; satay, Som tam, Pad thai marked for Misha; Spring rolls and Kids nuggets green. Anna says she is lactose intolerant → the waiter adds **MILK, intolerance** to her card on the tablet → Espresso's **Whole milk** turns red for Anna; she takes oat milk. | Waiter | `GET /api/reservations?query=Kuznetsov`, `GET /api/outlets/4/menu?reservationId=…`, `PUT /api/passports/R-203844` | `GET /v1/pos/dict/pms/lookup`, `GET /v1/pos/dict/menu?rvc-on=4`, `PATCH /v1/pms/guests/{guestId}` |
| 3 | Misha: Kids nuggets + Coconut ice cream. Mikhail: Chicken satay for himself → **waiter confirms** "Misha (severe) is at the table". Anna insists on **Thai iced tea** (MILK) → she gives **consent on the tablet** with her name. Send. | Waiter | `POST /api/orders` | `POST /v1/pos/checks?rvc-on=4` (table, `reservationId`) |
| 4 | Within ~3 s the kitchen display shows the ticket: "Thai iced tea — MILK — Anna (intolerance) · guest consent"; "Chicken satay — PEANUTS — for Mikhail; Misha (severe) at the table". Then a bartender types **Som tam** for room 518 straight into the POS → the ticket "not checked at ordering — PEANUTS: Misha (severe)" appears on top. | Kitchen (Pool Bar) | `GET /api/kitchen/4/tickets`, SSE `GET /api/stream` | `GET /v1/events` (`pos.check.round_sent`); the bartender's order via `scripts/pos-order.mjs` |
| 5 | The manager searches "518": Anna's consent (dish, allergen, time, waiter W02, Pool Bar), the waiter's confirmation, the passport change by the waiter, the unchecked POS order — proof for an incident. | Manager | `GET /api/safety-log?query=518`, `GET /api/safety-log/summary`, SSE | — |

## API contract — frozen at 13:00

Changes after 13:00 only through BE-A (`CLAUDE.md` §5). Enums: `Allergen` = the 14 EU allergens of the sandbox; `Severity` = `INTOLERANCE` | `SEVERE`; `Verdict` = `SAFE` | `WARNING` (intolerance) | `DANGER` (severe) | `UNKNOWN` (no reservation); `Requirement` = `NONE` | `WAITER_CONFIRMATION` | `GUEST_CONSENT`.
Shared shape `DangerFor` = `{memberId, memberName, allergen, severity}`.

| Method and path | Request | Response | Owner | Status |
|---|---|---|---|---|
| `GET /api/reservations?query=` | room number (exact) or last-name prefix (2+ chars) | `ReservationSummary[]`: `reservationId, roomNumber, guestName, arrivalDate, departureDate, adults, children, language, pmsAllergens[], hasPassport` — in-house and today's arrivals | BE-A | stub |
| `GET /api/front-desk/arrivals` | — | `ReservationSummary[]`: today's arrivals and in-house guests with allergies or children, those without a passport first | BE-A | stub |
| `GET /api/passports/{reservationId}` | — | `Passport`: `reservationId, roomNumber, guestName, notes, pmsAllergens[], members[]: {id, name, age, primary, allergies[]: {allergen, severity}}, updatedAt, updatedBy`. No passport yet → the primary guest with the PMS allergens as `SEVERE` | BE-B | stub |
| `PUT /api/passports/{reservationId}` | `{members[]: {id?, name, age?, primary, allergies[]}, updatedBy, source: FRONT_DESK / WAITER, rvc?}` | `Passport`; side effects: PMS allergies = union of members; log `PASSPORT_UPDATED` | BE-B | stub |
| `GET /api/outlets` | — | `{rvc, name, kind, tables[]}[]` | BE-A | stub |
| `GET /api/outlets/{rvc}/menu?reservationId=` | reservation optional | `MenuItem[]`: `code, name, group, price, allergens[], verdict, dangerFor[], safeAlternatives[], condimentGroups[]: {code, name, minSelect, maxSelect, options[]: {code, name, price, allergens[], verdict, dangerFor[]}}` | BE-A (verdicts from the engine) | stub |
| `POST /api/orders/check` | same body as `POST /api/orders` | `lines[]: {code, verdict, dangerFor[], requirement}` — what each line needs, before sending | BE-A | stub |
| `POST /api/orders` | `{rvc, reservationId?, table?, waiterCode, lines[]: {code, quantity, condimentCodes[], forMemberId?, waiterConfirmed, consent?: {givenBy}}}` | `{checkNumber, checkSeq, lines[]: {code, verdict, dangerFor[], requirement}}`. Errors: 400 `WAITER_CONFIRMATION_REQUIRED`, 400 `GUEST_CONSENT_REQUIRED` | BE-A | stub |
| `GET /api/kitchen/{rvc}/tickets` | — | `KitchenTicket[]`: `id, rvc, checkNumber, table, roomNumber, guestName, sentAt, source (CHECKED / UNCHECKED / UNLINKED), hasAllergens, lines[]: {name, quantity, condiments[], allergens[], verdict, dangerFor[], forMemberName?, waiterConfirmed, consentBy?}` — newest first | BE-B | stub |
| `GET /api/safety-log?query=&rvc=&type=` | all optional; `query` = room or last name | `LogEntry[]`: `id, at, type (PASSPORT_UPDATED / ORDER_PLACED / WAITER_CONFIRMED / GUEST_CONSENT / POS_ORDER_UNCHECKED), rvc, outletName, reservationId, roomNumber, memberName, item, allergen, severity, actor, statement` — newest first | BE-B | stub |
| `GET /api/safety-log/summary` | — | `{ordersChecked, linesWithAllergens, waiterConfirmations, guestConsents, uncheckedPosOrders}` | BE-B | stub |
| `GET /api/stream` | — | Server-sent events: `ticket` (`KitchenTicket`), `log` (`LogEntry`), `passport` (`Passport`) | BE-A | stub |

## Tasks

Statuses: `TODO` · `DOING` · `DONE` · `CUT`. Each person edits only their own section.

### BE-A — Gennadii

| ID | Task | Demo step | Depends on | Status |
|---|---|---|---|---|
| BE-A1 | New team key, live smoke test, slow-sandbox fix (done by Sergey) | — | — | DONE |
| BE-A2 | The whole API contract as stub controllers + records, fixed data from the demo story; springdoc lists every endpoint — **by 13:00** | all | — | DONE |
| BE-A3 | Reservation search, front-desk arrivals, outlets (filtered PMS calls, POS lookup) | 1, 2 | BE-A2 | TODO |
| BE-A4 | Menu with verdicts (menu cached 5 min) + `POST /api/orders/check` + `POST /api/orders` → POS check linked to the reservation; enforce requirements; publish order, confirmation and consent entries | 2, 3 | BE-B2 | DOING |
| BE-A5 | Kitchen feed: listener on `pos.check.round_sent` → ticket through Erlan's service; SSE `/api/stream` from `TicketChanged`, `LogAppended`, `PassportChanged` | 4 | BE-B4 | DONE |
| BE-A6 | `scripts/pos-order.mjs` (bartender order straight into the POS, PIN from `.env`) + demo prep steps in `prepare-demo.mjs` | 4 | — | TODO |

Progress (details: `BE-A-PLAN.md`):
- BE-A2 done: the whole contract is in `main` (`/v3/api-docs`, 11 paths, contract tests green). Stubs: reservations, arrivals, outlets, menu, orders (BE-A3/BE-A4 make them real). Passport, kitchen and safety-log controllers call Erlan's services. `GET /api/stream` is open; events follow in BE-A5. `PUT /api/passports/{id}` takes an optional `notes`.
- BE-A4, step 2 of 4, on branch `claude/be-a4-2ece74` (not in `main` yet): the menu comes from the POS (cached 5 min) with the engine's verdicts; `POST /api/orders/check` goes through the engine; `POST /api/orders` rejects with 400 `WAITER_CONFIRMATION_REQUIRED` / `GUEST_CONSENT_REQUIRED` but still answers the fixed check 1017. Next: POS check, kitchen ticket, log entries.
- BE-A5 done: `/api/stream` sends Erlan's events after commit; `pos.check.round_sent` → kitchen tickets (`kitchen.feed`), tests green, not checked live (no round in the feed yet). Erlan: a PMS failure inside `recordPosRound` loses the ticket (rollback-only from `PassportService.party`), details in `BE-A-PLAN.md`.

### BE-B — Erlan

| ID | Task | Demo step | Depends on | Status |
|---|---|---|---|---|
| BE-B2 | Allergen engine per the agreement above, parameterized unit tests — **first, push early** | 2, 3, 4 | — | DONE |
| BE-B1 | Storage: passport members + allergies, kitchen tickets, safety log; Liquibase `200_…` | 1, 4, 5 | — | DONE |
| BE-B3 | Passport GET/PUT (front desk and waiter), default from the PMS profile, PMS sync (union), `guest.updated` listener, `PASSPORT_UPDATED` entries | 1, 2 | BE-B1 | DONE |
| BE-B4 | Kitchen tickets (create from a round, list), safety log with search by room or last name, summary; publish `TicketChanged`, `LogAppended`, `PassportChanged` | 4, 5 | BE-B1 | DONE |
| BE-B5 | Backend demo-path test over WireMock fixtures (after 14:00) | all | BE-A4, BE-A5 | DONE |

Progress: passport, kitchen ticket and safety-log endpoints run on the real services. `DemoPathTest` covers steps 1, 2 (passport), 4 (tickets through `KitchenTicketService`) and 5; menu verdicts, `POST /api/orders` and the feed are not in it until BE-A4/BE-A5 land. What BE-A calls:
- `AllergenEngine` (`allergen`): `checkItem(party, allergens)`, `checkLine(party, allergens, forMemberId)`, `safeAlternatives(menu, party, item)`; merge condiments with `AllergenEngine.withCondiments(...)`; unknown `forMemberId` → `UnknownPartyMemberException`.
- `PassportService.party(reservationId)` → `Party` (room, guest name, members for the engine); reads the PMS only when there is no passport yet.
- `KitchenTicketService.recordCheckedRound(Round)` right after the POS accepted a tablet round (lines with `forMemberId`, `waiterConfirmed`, `consentBy`); `recordPosRound(Round)` from the `pos.check.round_sent` feed, deduplicated per round. Line allergens include the condiments' (resolve them from the menu).
- `SafetyLogService.append(safetyLog.entry(type)…build())` for `ORDER_PLACED`, `WAITER_CONFIRMED`, `GUEST_CONSENT` (one entry per line, so the summary adds up).
- Events for `/api/stream`: `PassportChanged(PassportResponse)`, `TicketChanged(KitchenTicketResponse)`, `LogAppended(LogEntry)` — the same records the endpoints return. They are published inside the transaction: listen with `@TransactionalEventListener(fallbackExecution = true)`.

### FE-A — Sergey

| ID | Task | Demo step | Depends on | Status |
|---|---|---|---|---|
| FE-A1 | Frontend skeleton (block A defaults), routes, device setup, CI job — **by 12:45** | — | — | DONE |
| FE-A2 | Waiter: party search (room or last name), badges per person, menu by group with verdict colours, "only safe" toggle, walk-in mode (allergens only) | 2 | FE-B1 | DOING |
| FE-A3 | Waiter: condiment picker, "for whom", waiter-confirmation tap, **guest consent screen** (guest types their name), safe alternatives, quick passport update (add member or allergen), cart → send | 2, 3 | FE-A2 | DOING |
| FE-A4 | Kitchen display: tickets live via SSE, allergen lines "for whom", consent and confirmation marks, "not checked at ordering" on top — no approval | 4 | FE-B1 | DOING |

### FE-B — Nikolai

| ID | Task | Demo step | Depends on | Status |
|---|---|---|---|---|
| FE-B0 | Design in `shared/`: chips with icons for the 14 allergens, severity and verdict colours, party badge | all | FE-A1 | DONE |
| FE-B1 | Generated API types (`npm run gen:api`) + `useStream` hook for `/api/stream` + error messages by `code` | — | BE-A2 | DONE |
| FE-B2 | Front desk: arrivals worklist, search, a card per member (name, age, allergens, severity), save with "PMS updated" | 1 | FE-B1 | DOING |
| FE-B3 | Manager: search by room or last name, timeline with consent records highlighted, KPIs, live via SSE | 5 | FE-B1 | DOING |
| FE-B4 | Playwright happy path (after 14:30), screen recording at 15:15 | all | UI stable | DOING |
| FE-B5 | Stretch: guest passport page by QR | — | everything else DONE | CUT |

## Checkpoints — 5 minutes, one sentence each: done / doing / blocked

| Time | Goal |
|---|---|
| 12:45 | Frontend skeleton in `main` |
| **13:00** | **API contract stubs in `main`; frontend generates types**; engine pushed |
| 13:00–13:45 | Lunch: each agent works on a bounded task (no contract changes, no push without a green build) |
| 13:45 | Status after lunch |
| **14:15** | **First end-to-end pass of the demo path on the live sandbox, however ugly** |
| 14:30 | Polish; slides start (Sergey) |
| 15:00 | Whatever does not work goes to the cut list |
| 15:15 | Feature freeze; clean clone of `main` on the demo laptop (App key); screen recording |
| 15:40 | `prepare-demo.mjs --reset` (tech lead only) + empty `backend/data/`; two rehearsals done |
| 16:00 | Demo: Sergey speaks, Nikolai clicks; windows side by side: waiter left, kitchen right |

Blocked for more than 15 minutes: say so at once.

## Shared resources

- **Dev key: 10 requests per second for the team.** `SANDBOX_RATE_LIMIT=2` in every developer's `backend/.env`. The demo laptop uses the App key with the default 8.
- **Hotel data is shared.** Work only on your reserved rooms; only the tech lead resets, after telling the team.
- **No language model** in this topic.
- **Demo laptop:** Sergey's; a clean clone of `main` next to the working checkout.

| Who | Rooms (reservation) |
|---|---|
| Demo path — nobody touches | 518 (R-203844, Kuznetsov); backup 729 (R-203981, Thomas, GLUTEN / FISH / MUSTARD) |
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
- Kitchen approval and escalation (the kitchen is notified only).
- "May contain" and cross-contamination; printing and export; payment and room charge; login; translation of staff screens.
