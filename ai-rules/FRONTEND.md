# FRONTEND.md — frontend rules

> Status: **STUB**. Load for any frontend work. Decisions are owned by the frontend developers
> on the day (Sergey Zubov, Nikolai Ivliev); going through §6 is their first step.

## §1. Stack

> Preliminary, to be confirmed by the frontend developers.

- React, Vite, TypeScript in strict mode; one app under `frontend/`.
- UI kit, routing, server state, type generator: TODO (§6, block A).
- API types are generated from the backend's OpenAPI document (decided, see `BACKEND.md` §3).

## §2. Layout

> Proposal, to be confirmed by the frontend developers (§6, A6).

```text
frontend/
├── package.json, vite.config.ts     ← dev proxy /api → localhost:8080
└── src/
    ├── main.tsx, App.tsx            ← providers and routes; the only place that knows the libraries
    ├── api/                         ← types generated from /v3/api-docs + one thin client; no hand-written API types
    ├── shared/                      ← UI building blocks, i18n, error messages, money and hotel-time formatting
    └── features/<feature>/          ← one folder per feature, same name as the backend package
        ├── pages/  components/  hooks/
        └── <feature>Api.ts          ← this feature's calls through api/
```

- Routes by persona: `/staff/...` (front desk, kitchen, manager) and `/guest/...`. No login (`CLAUDE.md` §4).
- Features are created on the event day, as on the backend. Before 11:00 only `api/`, `shared/` and `App.tsx` exist.

## §3. UX principles

- Staff screens in English; guest-facing content in the guest's language (decided).
- TODO: phone-first where a phone is the device (§6, B3).

## §4. Conventions

- The browser calls only our backend, never the sandbox or the model directly (keys stay on the backend).
- Errors: show a message chosen by the ProblemDetail `code`, in one place in `shared/` (`BACKEND.md` §3).
- Every element on the demo path gets a `data-testid` (`<feature>-<element>`), so the Playwright test is quick to write.
- TODO: state, styling, forms, lint and format (§6).

## §5. Tests

- TODO (§6, B5).

## §6. Kickoff questions for the frontend developers

Each question has a default: if nobody objects, the default stands. Block A blocks the frontend skeleton.

**A. Before the skeleton**

1. Node version and package manager. Default: Node 22 LTS, npm.
2. UI kit. Default: Mantine. Alternatives: shadcn/ui + Tailwind, MUI.
3. Routing. Default: React Router.
4. Server state and data fetching. Default: TanStack Query.
5. Types from the backend OpenAPI. Default: `openapi-typescript` + `openapi-fetch`. Alternative: Orval.
6. Layout in §2: confirm or change.
7. Client state beyond server state. Default: none (URL + TanStack Query); a store only if a screen really needs one.
8. Lint and format. Default: the Vite template's ESLint + Prettier with default settings.

**B. Before 11:00 on Sunday**

1. Live updates on staff screens. Default: server-sent events from `GET /api/stream` (backend proposal), polling as fallback.
2. Localisation of guest screens. Default: i18next on guest screens only; language from `guest.language` with a switcher.
3. Device targets. Default: guest and room-attendant screens phone-first; front desk, kitchen and manager desktop or tablet. No PWA.
4. Forms and validation. Default: the UI kit's form helpers + Zod.
5. Tests. Default: Vitest for logic with rules (formatting, mapping); type check, lint and build in CI; one Playwright happy path of the demo path, run by hand (`TESTING.md` §5). Install the Playwright browsers before Sunday.
6. How the frontend runs in the demo. Default: Vite dev server with the proxy; the backend runs separately.
7. Formatting of money and time. Default: `Intl` with the hotel currency and time zone from the backend.

**C. On the day, once the topic is known**

1. Who builds which screens: staff screens vs guest screens.
2. Look and feel: the UI kit theme with one accent colour for the hotel; light mode only.
3. Which screens are on the demo path, and in which order they are built.
