# TESTING.md — tests and sandbox stubs

> Status: **DRAFT**. Load when writing or running tests. The team has no QA engineer: tests and the demo check are everyone's job.

## §1. Principles

- Tests protect the demo path and the business rules; they are not a coverage exercise.
- Automated tests never call the live sandbox or the real model. The live sandbox is checked by hand on the demo path (`CLAUDE.md` §3, item 1).
- The whole backend suite runs in under 2 minutes. No `Thread.sleep`; retry backoff is 1 ms in the test profile.
- Tests ship in the same commit as the code they cover.

## §2. What we test

Minimum per feature (`CLAUDE.md` §3, item 4):

| Layer | Tool | What |
|---|---|---|
| Business rules | JUnit 5 + AssertJ, no Spring context | Every rule a feature decides on: allergen matching (item **and** condiment allergens), offer rules, cleaning priority. Parameterized tests for rule tables. |
| Sandbox facade | WireMock | Each facade method: one happy path and one failure the feature handles. |
| Our API | `@WebMvcTest` + MockMvc | Request validation gives 400 `ProblemDetail` with `code`; sandbox error codes map to the right status and `code` (`BACKEND.md` §3). |
| Model features | stubbed `LlmClient` | The validation layer rejects unsafe proposals (a dangerous allergen, a price or time the hotel does not offer); the fallback works when the model fails. Never assert on the model's wording. |
| Application | one `@SpringBootTest` | The context starts with WireMock as the sandbox, a stubbed `LlmClient` and in-memory H2. |

Cross-cutting sandbox tests, written once in the skeleton (`BACKEND.md` §5.2):

- 429, then 200 → the call succeeds after a retry.
- 503 on every attempt → `SandboxException` with `SERVICE_UNAVAILABLE` after 3 attempts.
- 401 on a POS call → one re-login, then the call is repeated.
- 503 on `POST /v1/pos/checks` → exactly **one** request reached the sandbox (no automatic retry).
- A retried folio posting sends the same `postingGuid` every time.
- The events poller processes a duplicate `key` once and stores `nextSince`. Tests call the poll method directly, not through the scheduler.

Not tested: generated client code, records without logic, layout and styling.

## §3. Sandbox stubs

- WireMock on a random port through its JUnit 5 extension; the test profile points `sandbox.base-url` at it.
- Fixtures are JSON files in `backend/src/test/resources/sandbox/<pms|pos|events>/<name>.json`, recorded from the practice hotel and trimmed to what the test needs. The full set (every endpoint, plus the error examples) is recorded during the skeleton phase, while the practice keys still work.
- Fixtures contain no keys, PINs or tokens: strip headers when recording.
- Error fixtures follow the contract's envelope: `{"success": false, "message": "...", "code": "..."}`.
- One helper class, `SandboxStubs`, owns the stub setup (`hotel()`, `rooms(...)`, `failWith(code, status)`, `rateLimitedThenOk()` through WireMock scenarios). Tests use it instead of raw WireMock calls.
- A fixture is updated when the contract changes, never edited to make a failing test pass.
- Stubs are for tests only: there is no offline profile for the demo. The demo fallback is a screen recording (`CLAUDE.md` §4).

## §4. Structure and naming

- `backend/src/test/java/com/hackathon/team4/` mirrors the main packages.
- All test classes are named `<Class>Test` and run with `mvnw test`; no separate integration phase.
- Method names read as the spec: `shouldRefuseDishWhenGuestIsAllergicToCondiment`. Arrange, act, assert.
- AssertJ for assertions. Forbidden: assertion-free tests, testing private methods, mocking the class under test, `@Disabled` without a reason in the same line, weakening a test to make it pass.

## §5. Frontend tests

- Owned by the frontend developers (`FRONTEND.md` §6, question 9).
- Minimum until they decide: type check, lint and build pass in CI.

## §6. Commands

Run from `backend/` (on Windows `mvnw.cmd`):

```bash
./mvnw test                                   # whole backend suite
./mvnw test -Dtest=SandboxClientTest          # one class
./mvnw verify                                 # what CI runs
./mvnw test -Dtest=LiveSandboxSmokeTest -Dsandbox.live=true   # read-only check of the live sandbox (.env values)
node scripts/record-fixtures.mjs              # re-record the fixtures (small, mostly reverted writes to the hotel)
```

- Run the live smoke test first thing on the event day, as soon as the new team key is in `.env`.

- Frontend: TODO, after the frontend developers choose the tools.
