# BE-A-PLAN.md — working plan of slot BE-A (Gennadii)

> Tasks, owners and statuses: `PLAN.md` → "BE-A — Gennadii". This file only details how BE-A does them.

## BE-A2 — API contract as stub controllers (due 13:00)

**Approach.** Controllers are final from the start and follow the "API contract" table in `PLAN.md` exactly. Fixed demo data lives in a `*Service` of the same package; BE-A3/BE-A4 (and Erlan for his packages) replace only the service bodies, never the controllers. No sandbox DTOs outside `sandbox` (`BACKEND.md` §2).

### Packages and classes

All under `com.hackathon.team4`; request and response records in each package's `dto/`.

| Package | Classes | Records and enums | Owner after the push |
|---|---|---|---|
| `allergen` | — | `Allergen` (14 EU), `Severity`, `Verdict`, `Requirement` (enums); `DangerFor` {memberId, memberName, allergen, severity} | BE-B |
| `reservation` | `ReservationController` (`GET /api/reservations`, `GET /api/front-desk/arrivals`), `ReservationService` | `ReservationSummary` | BE-A |
| `menu` | `OutletController` (`GET /api/outlets`, `GET /api/outlets/{rvc}/menu`), `MenuService` | `Outlet`, `OutletKind` (RESTAURANT / BAR / ROOM_SERVICE, as in the POS), `MenuItem`, `CondimentGroup`, `CondimentOption` | BE-A |
| `order` | `OrderController` (`POST /api/orders/check`, `POST /api/orders`), `OrderService` | `OrderRequest`, `OrderLineRequest`, `ConsentRequest` {givenBy}, `OrderCheckResponse`, `LineCheck` {code, verdict, dangerFor, requirement}, `OrderResponse` {checkNumber, checkSeq, lines} | BE-A |
| `passport` | `PassportController` (`GET`/`PUT /api/passports/{reservationId}`) → BE-B's `PassportService` | `PassportResponse`, `PassportMemberResponse`, `MemberAllergyResponse` (schemas `Passport`, `PassportMember`, `MemberAllergy`), `PassportUpdateRequest`, `PassportMemberRequest`, `MemberAllergyRequest` | BE-B |
| `kitchen` | `KitchenTicketController` (`GET /api/kitchen/{rvc}/tickets`) → BE-B's `KitchenTicketService` | `KitchenTicketResponse` (schema `KitchenTicket`), `KitchenTicketLine`; BE-B's `TicketSource` | BE-B |
| `safetylog` | `SafetyLogController` (`GET /api/safety-log`, `GET /api/safety-log/summary`) → BE-B's `SafetyLogService` | `LogEntry`, `SafetyLogSummary`; BE-B's `LogEntryType` | BE-B |
| `stream` | `StreamController` (`GET /api/stream`, `text/event-stream`), `StreamHub` (registry of `SseEmitter`s) | events `ticket` / `log` / `passport` | BE-A |

`kitchen.feed` stays empty until BE-A5. About 45 small files, mostly records.

### Field types and validation

- Dates `LocalDate`; timestamps `OffsetDateTime`; price `BigDecimal`.
- Member ids `UUID`, the same values in every stub.
- `rvc` and `checkNumber` `int`; `checkSeq` `long`; item codes and tables `String`.
- Lists are never `null`.
- Request records: `quantity` positive, `lines` not empty, `waiterCode` required (`jakarta.validation`).
- A search `query` shorter than 2 characters returns `[]`, not 400: the search box calls it on every keystroke.

### Stub data — the demo story after step 2, so every verdict and severity shows

- Reservation R-203844, room 518, Kuznetsov; backup R-203981, room 729, "Oliver Thomas" (placeholder first name), GLUTEN / FISH / MUSTARD, no passport, so it comes first in the arrivals worklist. The search filters for real: a room number or a last-name prefix finds them, anything else returns `[]` (empty state for the frontend).
- Member ids and names, the same in every stub: Mikhail `518a0000-0000-4000-8000-000000000001`, Anna `…0002`, Misha `…0003`.
- Outlets: Main Restaurant (1), Beach Bar (2), Room Service (3, no tables), Pool Bar (4); tables are placeholders.
- Passport: Mikhail (primary), Anna — MILK intolerance, Misha, 7 — PEANUTS severe.
- The Pool Bar menu, returned for every outlet:
  - Satay, Som tam, Pad thai: `DANGER` for Misha;
  - Thai iced tea and the Espresso condiment Whole milk: `WARNING` for Anna;
  - Spring rolls, Kids nuggets, Oat milk: `SAFE`;
  - without `reservationId`, or with any other reservation, every item is `UNKNOWN`.
- Kitchen: three tickets, one per `source`. Safety log: one entry per type, summary counts to match.
- `PUT` passport echoes the submitted members and gives new ones an id.
- Stream keeps the connection open; real events come with BE-A5. `KitchenTicket`, `LogEntry` and `Passport` already appear in OpenAPI through other endpoints, so `npm run gen:api` generates them.

### Test

`ApiContractTest` (`@SpringBootTest` + MockMvc), no sandbox and no `.env` needed:

- `/v3/api-docs` lists all 12 paths;
- `GET /api/reservations?query=518` returns R-203844;
- an invalid `POST /api/orders` returns 400 `VALIDATION_ERROR`.

### Steps — one per prompt; the backend compiles after each, review before the next

1. **Done.** BE-A2 → `DOING` in my section of `PLAN.md`; shared contract types in `allergen`.
2. **Done.** `reservation` and `menu` (outlets and menu) stubs; checked on a running backend.
3. **Done.** `order` stubs; checked on a running backend. Lines are checked against the demo menu (condiments included) with the requirement rules of `PROJECT-CONTEXT.md` §3, so "for whom" behaves as in the demo; BE-A4 swaps this for the engine. `POST /api/orders` returns 201 with check 1017 / seq 88231 and sends nothing to the POS.
4. **Done.** `passport`, `kitchen`, `safetylog` endpoints, wired straight to BE-B's services (no stubs: BE-B had them in `main` before my stubs landed).
   - The API records get a `Response` suffix where BE-B's entities already use the name; `@Schema` keeps the contract names in OpenAPI. BE-B's `TicketSource`, `LogEntryType` and `PassportService.Source` are reused.
   - `PUT /api/passports/{id}` takes an optional `notes` (additive contract change: BE-B's save stores it); the outlet name for the log comes from `MenuService.outletName(rvc)`.
   - Timestamps from BE-B's entities are sent in UTC (`Z`) until a hotel clock exists.
   - `checkNumber` is a number in our API (`OrderResponse`, `KitchenTicket`), as the frontend already expects; BE-B's ticket entity stores it as a string, so the mapping parses it.
   - The passport endpoints call the PMS, so they need `backend/.env`; kitchen and safety log read our database only.
5. **Done.** `stream`: `GET /api/stream` (SSE) through `StreamHub`; `:connected` on open, `:heartbeat` every 15 s; `broadcast(StreamEvent, payload)` sends `ticket` / `log` / `passport` with the REST record as JSON, as `frontend/src/shared/stream.ts` expects. Nothing is broadcast yet: BE-B publishes no domain events so far, forwarding them is BE-A5.
6. `ApiContractTest`, `./mvnw test` green; commit `feat(be): add API contract stubs (BE-A2)`, no AI co-author trailer (`CLAUDE.md` §7); review before the push.
7. `git pull --rebase`, push; BE-A2 → `DONE`; tell the team "contract stubs are in".

### Open points

1. **Shared enums in `allergen`.** Erlan is writing the engine now and may already have `Verdict` and `Requirement`. Ask him first; if they are in `origin/main`, reuse his types instead of creating duplicates.
2. **Error codes `WAITER_CONFIRMATION_REQUIRED` and `GUEST_CONSENT_REQUIRED`** are only documented in BE-A2 (`@ApiResponse 400`). The exception and the check come in BE-A4 with the engine; earlier they would be dead code.
3. **Price without a currency field**, as in the contract: one hotel, THB.
4. **Item codes in the stub are placeholders.** Real codes come from the live menu in BE-A3/BE-A4 (needs `backend/.env`); the frontend must not depend on them.
5. **A malformed path or query parameter** (e.g. `/api/outlets/abc/menu`) gives 400 without `code`: `GlobalExceptionHandler` adds `VALIDATION_ERROR` only for invalid request bodies. Small fix in `common`, to agree with the team.
6. **Generated frontend types make every response field optional**, and request primitives (`rvc`, `quantity`, `waiterConfirmed`) too: the OpenAPI schemas list only `@NotNull`/`@NotBlank` fields as required. Ask FE-B whether to mark record fields required (one `OpenApiCustomizer`) before they write much code.
