# PLAN.md — implementation plan

> Template. Filled in at 11:35 on the event day from `ai-rules/PROJECT-CONTEXT.md` §3.
> Agents: work by task ID; read its row first and touch only the files of that task's feature.

## Team mode

Tick one at 11:00: `[ ]` 4 people (Aleksandr) · `[ ]` 4 people (substitute: name, profile) · `[ ]` 3 people

| Slot | 4 people | 3 people |
|---|---|---|
| **BE-A** Sergey | Tech lead: API contract, sandbox integration, demo path end to end, this file, checkpoints, pitch | Same, plus the BE-B scope; the agent works through the plan task by task |
| **BE-B** Aleksandr or a backend substitute | Domain logic, our storage, the model, backend demo-path test | — (scope moves to BE-A; pick the variant with the least backend work) |
| **FE-A** Gennadii | Staff screens; owns `App.tsx` and routes | Same |
| **FE-B** Erlan | Guest and phone screens, design consistency, Playwright happy path, slides, clicks in the demo | Same; may take read-only backend endpoints that copy an existing pattern; Playwright only if time remains |

A substitute who is not a backend developer takes **QA and demo** instead of BE-B: backend demo-path test, Playwright, demo data script, video, slides. BE-B's scope then moves to BE-A, as in the 3-person mode.

## Demo path

| # | The jury sees | Screen | Our API | Sandbox calls |
|---|---|---|---|---|
| 1 | | | | |
| 2 | | | | |
| 3 | | | | |
| 4 | | | | |
| 5 | | | | |

## API contract — frozen at 12:30

Changes after 12:30 only through BE-A (`CLAUDE.md` §5).

| Method and path | Request | Response | Owner | Status (stub / real) |
|---|---|---|---|---|
| | | | | |

## Tasks

Statuses: `TODO` · `DOING` · `DONE` · `CUT`. Each person edits only their own section; the blank lines between sections keep `git pull --rebase` free of conflicts. Tasks are 30–60 minutes and name the demo step they serve.

### BE-A — Sergey

| ID | Task | Demo step | Depends on | Status |
|---|---|---|---|---|
| BE-A1 | New team key in `.env`, live smoke test, re-record fixtures | — | — | TODO |
| BE-A2 | API contract as stub controllers and records (fake data) | all | — | TODO |
| BE-A3 | Demo prep steps in `backend/scripts/prepare-demo.mjs` | — | demo path | TODO |

### BE-B — Aleksandr or substitute (3 people: merged into BE-A)

| ID | Task | Demo step | Depends on | Status |
|---|---|---|---|---|
| BE-B1 | Storage of our own data: entities + Liquibase changeset (range 200–299) | | BE-A2 | TODO |
| BE-B2 | Backend demo-path test over WireMock fixtures | all | BE-A2 | TODO |

### FE-A — Gennadii

| ID | Task | Demo step | Depends on | Status |
|---|---|---|---|---|
| FE-A1 | Frontend skeleton per `FRONTEND.md` §6 block A; routes `/staff`, `/guest` | — | — | TODO |

### FE-B — Erlan

| ID | Task | Demo step | Depends on | Status |
|---|---|---|---|---|
| FE-B1 | Generated API types + thin client in `src/api/`, error messages by `code` | — | BE-A2 | TODO |
| FE-B2 | Playwright happy path of the demo path (after 14:30, run by hand) | all | UI stable | TODO |

## Checkpoints — 5 minutes, one sentence each: done / doing / blocked

| Time | Goal |
|---|---|
| 12:30 | API contract frozen; frontend calls the stubs |
| 13:00 | Before lunch: each agent gets a bounded 40-minute task |
| 13:45 | Status after lunch |
| **14:15** | **First end-to-end pass of the demo path on the live sandbox, however ugly** |
| 14:30 | Polish and the "wow" moment; slides start |
| 15:00 | Whatever does not work goes to the cut list |
| 15:15 | Feature freeze; clean clone of `main` on the demo laptop; screen recording |
| 15:40 | `node scripts/prepare-demo.mjs --reset` (BE-A only); two rehearsals done |
| 16:00 | Demo: one person speaks, one clicks |

Blocked for more than 15 minutes: say so at once, do not wait for the checkpoint.

## Shared resources

- **Sandbox key: 10 requests per second for the whole team.** Every developer sets `SANDBOX_RATE_LIMIT` in `backend/.env`: 2 with 4 people, 3 with 3 people; the demo laptop keeps the default 8.
- **The hotel is shared.** Each person works on their own rooms and guests (table below). Only BE-A calls reset, after telling the team.
- **Model key: one budget.** The real model runs only on the laptop of the person building the model feature; everyone else uses the stubbed `LlmClient`.
- **Demo laptop:** TODO (whose). Java 21, Node, Playwright browsers and `.env` checked before Sunday.

| Person | Rooms | Reservations / guests |
|---|---|---|
| Demo path | | |
| BE-A | | |
| BE-B | | |
| FE-A | | |
| FE-B | | |

## Cut list

-
