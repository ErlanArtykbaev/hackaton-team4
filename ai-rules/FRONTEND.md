# FRONTEND.md — frontend rules

> Status: **STUB**. Load for any frontend work. Decisions below are owned by the frontend developers
> (Erlan Artykbaev, Gennadii Chzhen); filling §6 is their first step.

## §1. Stack

> Preliminary, to be confirmed by the frontend developers.

- React, Vite, TypeScript in strict mode; one app under `frontend/`.
- UI kit: TODO (§6, question 1).
- Routing: TODO (§6, question 2).
- Server state and data fetching: TODO (§6, question 3).
- API types: generated from the backend's OpenAPI document (decided, see `BACKEND.md` §3). Generator: TODO (§6, question 4).

## §2. Layout

- TODO: folder structure (§6, question 10).

## §3. UX principles

- Staff screens in English; guest-facing content in the guest's language (decided).
- TODO: staff screens vs guest screens, phone-first where a phone is the device.

## §4. Conventions

- The browser calls only our backend, never the sandbox or the model directly (keys stay on the backend).
- TODO: state, styling, forms, lint and format.

## §5. Tests

- TODO (§6, question 9).

## §6. Open questions for the frontend developers

1. UI kit: Mantine, shadcn/ui + Tailwind, MUI, or another?
2. Routing: React Router or TanStack Router?
3. Server state: TanStack Query, or plain fetch with a thin wrapper?
4. Type generation from the backend OpenAPI: `openapi-typescript` (+ `openapi-fetch`), Orval, or another?
5. Forms and validation: React Hook Form + Zod, or the UI kit's own form helpers?
6. Localisation for guest screens: i18next, or the UI kit's tools? How do we pick the language (from `guest.language`, a switcher, or both)?
7. Live updates on staff screens: polling our backend, or server-sent events from it?
8. Device targets: which screens are phone-first (room attendant, guest), which are desktop or tablet (front desk, kitchen, manager)? Do we need a PWA?
9. Tests: Vitest + Testing Library for components? One Playwright end-to-end test for the demo path?
10. Folder structure: by feature or by type?
11. Package manager (npm or pnpm) and the exact Node version.
12. How the frontend runs in the demo: Vite dev server with a proxy to the backend, or a production build served by Spring Boot?
13. Look and feel: theme, colours, a hotel brand per topic, dark mode?
14. Lint and format: ESLint + Prettier with which presets?
