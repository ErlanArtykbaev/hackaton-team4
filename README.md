# Team 4

TYNGO × Sonata hackathon, Tbilisi. A product for a hotel client, built on top of the hotel's PMS and POS sandbox.
The topic is announced at the start of the event; until then this repository holds a topic-agnostic skeleton.

Rules for people and AI agents: [`CLAUDE.md`](CLAUDE.md) and [`ai-rules/`](ai-rules).

## Prerequisites

- Java 21 or newer (the build targets 21). Maven comes with the wrapper.
- Node 20 or newer (fixture recording; the frontend once it exists).

## Backend

```bash
cd backend
cp .env.example .env          # then fill in the team key, access code and PIN
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

- API: `http://localhost:8080/api/...`; OpenAPI: `/v3/api-docs`; Swagger UI: `/swagger-ui.html`; health: `/actuator/health`.
- Data: H2 file database in `backend/data/` (git-ignored). Delete the folder to start clean.
- The sandbox client is generated from `backend/src/main/resources/sandbox/openapi.yaml` during the build.

## Tests

```bash
cd backend
./mvnw test                                                # whole suite, no network needed
./mvnw test -Dtest=LiveSandboxSmokeTest -Dsandbox.live=true # read-only check of the live sandbox with .env values
node scripts/record-fixtures.mjs                           # re-record sandbox fixtures (writes a little to the hotel)
```

## Event day

- Plan, owners and task statuses: [`PLAN.md`](PLAN.md).
- Demo state: `node scripts/prepare-demo.mjs` (dry run) and `--reset` (BE-A only; resets the hotel for the whole team).

## Layout

```text
backend/
├── scripts/record-fixtures.mjs   records sandbox responses for tests
└── src/main/java/com/hackathon/team4/
    ├── config/                   configuration properties
    ├── common/error/             ProblemDetail error handling
    └── sandbox/                  the only code that talks to the sandbox
        ├── pms/  pos/  events/   gateways, events poller
        └── SandboxCalls          rate limit, retry of safe calls, error translation
```
