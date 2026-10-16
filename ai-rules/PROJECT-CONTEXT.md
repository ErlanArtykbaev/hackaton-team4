# PROJECT-CONTEXT.md — this repository

> Status: **STUB**. The topic section is filled in at 11:00 on the event day.

## §1. Event

- TYNGO × Sonata hackathon, Tbilisi, Sunday 2026-10-18, 11:00–17:00, Paragraph Freedom Square, Conference Hall 1.
- One of four topics is assigned at the start: AI concierge, allergy passport, room ready, upsell that does not annoy.
- The hosts play the hotel staff (general manager, front desk, chef, revenue manager); interviewing them is part of the task.
- Agenda:

| Time | What |
|---|---|
| 10:30 | Registration, coffee, laptop setup |
| 11:00 | Opening, teams start working |
| 13:00 | Lunch |
| 13:45 | Second session, with a coffee break |
| 15:15 | Our feature freeze (team rule, `CLAUDE.md` §4) |
| 16:00 | Team demos to the jury |
| 17:00 | Awards and close |

## §2. Team

| Member | Company | Side | Slot in `PLAN.md` |
|---|---|---|---|
| Zubov Sergey | TYNGO | BE | BE-A, tech lead |
| Dudko Aleksandr | Sonata | BE | BE-B — **participation not confirmed**: the team is 3 or 4 people, or a substitute joins |
| Chzhen Gennadii | TYNGO | FE | FE-A |
| Artykbaev Erlan | Sonata | FE | FE-B |

Roles per team mode (3 people, 4 people, a substitute): `PLAN.md`, "Team mode".

## §3. Topic

- TODO (on the day): hotel, client quote, personas, the problem in one sentence, what we build, what we cut.

## §4. Stack

> Preliminary, to be confirmed with the whole team.

- Backend: Java 21, Spring Boot 3, Maven — details in `BACKEND.md`.
- Frontend: React, Vite, TypeScript — details in `FRONTEND.md`.
- Storage: H2 in file mode.
- Contract between backend and frontend: code-first, TypeScript types generated from the backend's OpenAPI document.
- Languages: staff screens in English; guest-facing content in the guest's language (`guest.language`).
- AI coding agent: Claude Code.
- TODO: LLM provider (ask the organizers), how it all starts locally with one command.

## §5. Repository layout

- TODO: directory tree.

## §6. Environments

- Sandbox base URL: `https://hackathon2026.tyngo.com` (see `BACKEND.md` §5).
- Practice keys work against one shared practice hotel until the event; new per-team keys arrive on Sunday.
