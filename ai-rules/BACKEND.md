# BACKEND.md — backend rules

> Status: **DRAFT**. Load for any backend work. Decisions marked preliminary are to be confirmed with the whole team.

## §1. Stack

> Preliminary.

- Java 21, Spring Boot 3, Maven with the wrapper (`mvnw`) committed; one module under `backend/`.
- Outbound HTTP (sandbox, model): Spring `RestClient` with explicit connect and read timeouts.
- Storage: H2 in file mode (no Docker needed on the demo laptop), Spring Data JPA, Liquibase with YAML changelogs. Data survives a restart; moving to PostgreSQL is a URL change.
- Resilience: Resilience4j for retry and the client-side rate limiter (§5.2).
- Sandbox client: generated from `openapi.yaml` by `openapi-generator-maven-plugin` (`library=restclient`), wrapped by our own `sandbox` package (§5.1).
- Tests: JUnit 5, AssertJ, Mockito, WireMock for the sandbox (`TESTING.md`).
- DTOs and value objects are Java records. No Lombok.
- Package root: `com.hackathon.team4`.

## §2. Layout and layering

```text
backend/
├── pom.xml
├── .env.example                 ← every variable, empty values (CLAUDE.md §6)
└── src/main/
    ├── java/com/hackathon/team4/
    │   ├── Application.java
    │   ├── config/              ← @ConfigurationProperties records, beans, CORS, scheduling
    │   ├── common/              ← error handling, hotel clock, ids, money
    │   ├── sandbox/             ← the only code that talks to the sandbox (§5)
    │   │   ├── pms/  pos/  events/
    │   │   └── SandboxException, SandboxErrorCode
    │   ├── llm/                 ← the only code that talks to the model (§6)
    │   └── <feature>/           ← one package per feature, created on the day
    │       ├── <Feature>Controller, <Feature>Service, <Feature>Repository
    │       ├── domain types (entities, enums)
    │       └── dto/             ← request and response records of our API
    └── resources/
        ├── application.yml
        ├── db/changelog/        ← Liquibase: db.changelog-master.yaml + changes/<NNN>_<description>.yaml
        └── prompts/             ← model prompts (§6)
```

- Package by feature. Shared code goes to `common`, `sandbox`, `llm` only when two features need it.
- Inside a feature: Controller → Service → Repository / sandbox / llm. Controllers stay thin: validation and mapping only. Business rules live in services.
- Our API never exposes sandbox DTOs or JPA entities: a feature maps them to its own records. The sandbox naming (`rvc-on`, `checkSeq`) stops at the `sandbox` package.

## §3. API for the frontend

- Path prefix `/api`, JSON only. springdoc serves the OpenAPI document at `/v3/api-docs` and Swagger UI at `/swagger-ui.html`; the frontend generates its types from `/v3/api-docs`.
- Errors: Spring `ProblemDetail` (`application/problem+json`) with an extra stable `code` property. A sandbox error the user can act on keeps its sandbox code (`CREDIT_LIMIT_EXCEEDED`, `ROOM_NOT_READY`); anything else becomes `INTERNAL_ERROR` or `UPSTREAM_UNAVAILABLE`. One `@RestControllerAdvice` in `common`; no stack traces or upstream bodies in responses. `detail` is English; the frontend translates by `code`, so codes never depend on language.
- Lists are never `null`; an empty list is `[]`. No pagination unless a screen really needs it (the hotel has tens to hundreds of records).
- Money is `BigDecimal` with the hotel currency next to it; never `double`. Dates are hotel-local `LocalDate`; timestamps are ISO-8601 with offset.
- Live updates: one server-sent events endpoint `GET /api/stream` pushes our own domain events to staff screens (confirm with the frontend developers, `FRONTEND.md` §6 question 7).
- No authentication: personas are separate routes on the frontend (`CLAUDE.md` §4).

## §4. Conventions

- Configuration through `@ConfigurationProperties` records with prefixes `sandbox` and `llm`; values come from `.env` (`CLAUDE.md` §6). No hard-coded URLs, PINs, codes or rvc numbers in code.
- **Time.** JVM and database run in UTC (set in `Application.main` and `application.yml`). "Now" comes from the injected `Clock` bean; the hotel's business date and time zone come from `GET /v1/pms/hotel` through a `HotelClock` in `common`. Never call `now()` without a clock.
- **Code.** Guard clauses over nesting. Enums for statuses and codes, never string literals. `Optional` only as a return type. Never return a `null` collection. Singletons hold no per-request state.
- **Exceptions.** A domain exception per case. `catch (Exception)` only at boundaries (scheduler, error handler) and with a log. No empty `catch`.
- **Validation.** Simple constraints on request records (`jakarta.validation`); invariants in services. Request and response records are separate types.
- **Persistence.** Domain entities: app-generated UUID id, `created_at`, `updated_at`; `@Version` where two people may edit the same row. Repositories only query: no logic, no `@Transactional` (it lives in services). `existsBy`/`countBy` instead of loading; no `findAll()` + filtering in memory; `Optional` for a single result; no N+1 (fetch join or `@EntityGraph`).
- **Logging.** SLF4J with `{}` placeholders. ERROR: a person must act, throwable attached. WARN: survived (fallback, retry). INFO: a business action with its ids. DEBUG: diagnostics. Log an exception once, where it is handled, with the throwable (never only `getMessage()`). No logging inside loops: one summary line plus one per failure. Never log secrets or request bodies.
- Schema changes go through Liquibase only, as YAML: one file per change, `db/changelog/changes/<NNN>_<description>.yaml` (`NNN` from the author's range: 001–099 skeleton, 100–199 BE-A, 200–299 BE-B — so two developers never take the same number), changeset id = the file name without extension, author = the developer's name. The master changelog includes `changes/` in order. Changesets are append-only: never edit one that has been pushed.

## §5. The hotel sandbox (PMS / POS)

The contract is `backend/src/main/resources/sandbox/openapi.yaml`, a copy of the organizers' file. Base URL `https://hackathon2026.tyngo.com`.

### §5.1 Facts from the contract

- 20 endpoints: 10 PMS, 8 POS, 2 platform (events feed, reset).
- PMS and platform: header `X-API-KEY`. POS (`/v1/pos/**`): `Authorization: Bearer <token>` from `POST /v1/auth/token` (key + PIN + access code), valid 12 h. `POST /v1/pos/checks` also needs `X-Device-Id`.
- Limits and failures: 10 req/s per key (429 `RATE_LIMITED`); 503 on purpose (`SERVICE_UNAVAILABLE`, `PMS_UNAVAILABLE`).
- Errors: branch on `code`, never on `message`.
- Folio postings are idempotent by `postingGuid`.
- Events: poll `GET /v1/events?since=<nextSince>`; at-least-once, deduplicate by `key`.
- The hotel is alive: a simulator checks guests in and out, cleans rooms and opens checks.

### §5.2 Client rules

- Only the `sandbox` package calls the sandbox. Features use its typed facade, never the generated API classes directly.
- Timeouts: connect 2 s, read 5 s; `POST /v1/sandbox/reset` read 40 s.
- Client-side rate limiter at **8 req/s**, shared by the whole application, so we never reach the sandbox's 429.
- Retry on 429, 503 and connect timeouts: 3 attempts, exponential backoff from 200 ms with jitter. **Only for safe calls:**
  - every `GET`;
  - `PATCH` of rooms, reservations and guests (they set values, a repeat is harmless);
  - folio postings and room-charge payments, because they carry a `postingGuid`.
  - **Never retry automatically** `POST /v1/pos/checks` (opens a second check) or a `PUT` that adds a round (sends the dishes to the kitchen twice). On failure, re-read the open checks and decide.
- A `postingGuid` is generated once per business action, stored before the first call, and reused on every retry.
- POS token: log in lazily, cache the token; on 401 `UNAUTHORIZED` log in again once and repeat the call. The employee (PIN) comes from configuration: guest-facing actions use the integration employee `INT01` (PIN to be requested from the organizers), staff actions use a staff PIN.
- Sandbox errors become `SandboxException(SandboxErrorCode, httpStatus)`; features catch the specific codes they handle.
- Reference data (hotel profile, outlets, menus) is cached in memory for 5 minutes. Rooms, reservations, folios and checks are never cached: the simulator changes them.
- Menu names in the guest's language: pass `Accept-Language` (`en` or the hotel's local language only).
- Events: one `@Scheduled` poller every 3 s. It stores `nextSince` and the processed event keys in the database, and publishes each new event as a Spring application event; features listen to those. The cursor survives a sandbox reset.
- `POST /v1/sandbox/reset` is never called from a feature; only a manual demo-preparation action, with confirmation (`CLAUDE.md` §5).

### §5.3 Domain recipes

- **Charge to room:** look up the guest (`GET /v1/pos/dict/pms/lookup?query=<room or last name>`, in-house only) → open the check with items → `PUT` with `tender` of type `ROOM_CHARGE`, `tenderAmount`, `paymentAccount = reservationId`, `postingGuid`.
- **Room service order:** `POST /v1/pos/checks` on the room-service revenue center with its room-service order type, no `table`, `checkName` = room number, `reservationId` set.
- **Late checkout:** `PATCH /v1/pms/reservations/{id}` with `departureTime` (`HH:mm`); the fee, if any, is a folio posting with a `postingGuid`.
- **Check-in:** the reservation is `BOOKED` for today, a room is assigned and `CLEAN` or `INSPECTED` (else `ROOM_NOT_READY`).
- **Room status and attendant:** `PATCH /v1/pms/rooms/{number}`; `OUT_OF_ORDER` is refused for an occupied room.
- **Allergies:** the guest's `allergies` against each menu item's `allergens` **and** the allergens of the chosen condiments. This check is deterministic code, never the model.
- **Remove a dish from a check:** comp it with a 100 % line discount (`DISCOUNT` detail with `reference` = the line's `pos`).

### §5.4 Gotchas

- `seq` in POS URLs is the check's `checkSeq`; the contract returns it as a string but takes it as an integer.
- A `PUT` with `details` sends a new round to the kitchen immediately; there is no draft.
- A check closes by itself when `totalDueRaw` reaches 0 and it has at least one item.
- Lists default to 50 items and allow at most 200: page through reservations and closed checks.
- The forecast range is at most 62 days.
- Revenue center numbers, order type codes and tender codes come from `GET /v1/pos/dict/outlets`; never hard-code them.

## §6. Language-model features

> Provider and model are announced by the organizers; until then this section is provider-neutral.

- The model key has a spending limit and lives only in the backend environment.
- One `LlmClient` interface in the `llm` package; features never call a provider SDK directly. Client: Spring AI (`ChatClient`, tool calling, structured output) with the provider's starter.
- **Facts come from the hotel systems, never from the model.** Prices, times, availability, allergens and policies reach the model as tool results or context; the model never invents them.
- **The model proposes, code decides.** Any action that changes data (an order, a charge, a late checkout, a room move) goes through a service method that validates it (allergens, credit limit, opening hours) before calling the sandbox. What needs a person's confirmation is decided per topic and recorded in `PROJECT-CONTEXT.md` §3.
- Prompts live in `src/main/resources/prompts/` as files, not as Java strings. System prompts are in English; the model answers in the guest's language.
- Send the model only the fields the task needs.
- Cost and failure: a token cap per call, a 30 s timeout, at most one retry. Log tokens used per call. When the model fails or is slow, the feature falls back to a fixed text or to a person (a task in the staff queue), so the demo path survives a model outage.
- Tests never call the real model: `LlmClient` is mocked or stubbed (`TESTING.md`).
