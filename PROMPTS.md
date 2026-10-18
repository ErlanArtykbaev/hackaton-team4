# PROMPTS.md — start prompts for each team member

Before the first prompt: `git pull`, put the Dev key, access code and PIN (from the team chat) into `backend/.env` with `SANDBOX_RATE_LIMIT=2`. Never paste secrets into a prompt. Next task at any time: `Next: <task ID> from PLAN.md, same rules.`

## Gennadii — BE-A

```text
I am Gennadii, slot BE-A in PLAN.md (backend today). Pull main. Read PLAN.md (Team, Agreements, Demo path, API contract, my section, reserved data), ai-rules/PROJECT-CONTEXT.md §3 and ai-rules/BACKEND.md.
My packages: reservation, menu, order, stream, kitchen.feed under com.hackathon.team4, plus backend/scripts. After BE-A2, touch nothing else.
1. BE-A2 now, done by 13:00: every endpoint of the API contract as a controller with request/response records exactly as in the contract, returning fixed data from the demo story (room 518, R-203844, Kuznetsov: Mikhail, Anna, Misha 7 with PEANUTS severe). Records for passports, kitchen tickets and the safety log go into packages passport, kitchen, safetylog; after my push Erlan owns them. springdoc must list all endpoints. Build and tests green, pull --rebase, push, tell the team "contract stubs are in".
2. Then BE-A3, BE-A4, BE-A5, BE-A6 in order. Use the existing sandbox gateways; always filter PMS queries (room, status, last name). Verdicts and requirements come only from Erlan's AllergenEngine (checkItem, checkLine, safeAlternatives); the SSE stream forwards Erlan's TicketChanged, LogAppended, PassportChanged.
Rules: DOING/DONE only in my section of PLAN.md; only rooms reserved for BE-A; never call the sandbox reset; push only green builds.
```

## Erlan — BE-B

```text
I am Erlan, slot BE-B in PLAN.md (backend today). Pull main. Read PLAN.md (Team, Agreements, Demo path, API contract, my section, reserved data), ai-rules/PROJECT-CONTEXT.md §3 (domain rules confirmed by the hosts) and ai-rules/BACKEND.md.
My packages: allergen, passport, kitchen (except kitchen.feed), safetylog under com.hackathon.team4.
1. BE-B2 now (no dependencies): AllergenEngine in package allergen, pure code with parameterized unit tests, exactly as in PLAN.md "Agreements": checkItem, checkLine (requirement NONE / WAITER_CONFIRMATION / GUEST_CONSENT; nothing is ever blocked), safeAlternatives; item allergens include the chosen condiments; an empty party gives UNKNOWN. Push early: Gennadii calls it.
2. Then BE-B1 (Liquibase changes 200_…), BE-B3, BE-B4, then BE-B5 after 14:00. When Gennadii's contract stubs land (about 13:00), take over the passport, kitchen ticket and safety-log controllers and records. Publish Spring events TicketChanged, LogAppended, PassportChanged.
Rules: DOING/DONE only in my section of PLAN.md; only rooms reserved for BE-B; never call the sandbox reset; push only green builds.
```

## Nikolai — FE-B

```text
I am Nikolai, slot FE-B in PLAN.md (frontend). Pull main. Read PLAN.md (Demo path, API contract, my section), ai-rules/PROJECT-CONTEXT.md §3 and ai-rules/FRONTEND.md.
My folders: frontend/src/shared, frontend/src/api, frontend/src/features/front-desk, frontend/src/features/manager, frontend/e2e. App.tsx, main.tsx, vite.config.ts and package.json belong to Sergey: ask him for changes.
1. FE-B0 once the frontend skeleton is in main: in shared/, chips with an icon for each of the 14 EU allergens, colours for severity (INTOLERANCE, SEVERE) and verdict (SAFE, WARNING, DANGER, UNKNOWN), a party badge "name, age: allergens". Mantine components; data-testid on everything on the demo path.
2. FE-B1 when the contract stubs are in main (about 13:00): with the backend running, `npm run gen:api`; a useStream hook for /api/stream (events ticket, log, passport); error messages by ProblemDetail code in shared/.
3. Then FE-B2 (front desk: a card per member), FE-B3 (manager: search by room or last name, consent records highlighted, KPIs), FE-B4 (Playwright happy path after 14:30, screen recording at 15:15).
Rules: never hand-write API types; DOING/DONE only in my section of PLAN.md; push only when typecheck, lint and build are green (pull --rebase first).
```

## Sergey — FE-A, tech lead

```text
I am Sergey, slot FE-A in PLAN.md and tech lead. My folders: frontend/src/features/waiter, frontend/src/features/kitchen, App.tsx, main.tsx, vite.config.ts, package.json.
1. Finish FE-A1: typecheck, lint, build green; frontend job in .gitlab-ci.yml; push by 12:45 and tell Nikolai.
2. FE-A2 and FE-A3 (waiter tablet: verdicts, "for whom", waiter confirmation, guest consent screen, quick passport update) against the contract stubs, then FE-A4 (kitchen display, notification only, live over SSE with Nikolai's useStream).
Rules: DOING/DONE only in my section of PLAN.md; push only green builds; data-testid on the demo path.
```
