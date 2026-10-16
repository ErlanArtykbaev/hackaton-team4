# Team 4 — rules for Claude Code

> Status: **DRAFT**. Decisions marked preliminary are to be confirmed with the whole team.

Always read the project context now:

@ai-rules/PROJECT-CONTEXT.md

Load on demand, only when the task needs it:

| Situation | Load |
|---|---|
| Backend code, calls to the hotel sandbox, language-model features | `ai-rules/BACKEND.md` |
| Frontend code | `ai-rules/FRONTEND.md` |
| Writing or running tests, sandbox stubs | `ai-rules/TESTING.md` |

## 1. Priorities

- We build a product for a hotel client on top of its PMS and POS (the sandbox). The client's real need comes first.
- Priority order: **quality → reliability → simplicity and intuitive use → speed.**

## 2. Hackathon hard rules

- No code for a specific topic before **2026-10-18 11:00 (Asia/Tbilisi)**. Before that: skeleton, CI, sandbox API client, test setup, stubs, rules only.
- The jury reads the git history (see §7).
- The demo runs from a team laptop: everything must start locally.
- The sandbox is an external system: we cannot change it, only call it.

## 3. Definition of done

A feature is done when all of these hold:

1. It works end to end against the **live sandbox**, not only against stubs, started from a clean checkout with the command in `README.md`.
2. Sandbox and model failures are handled: 429 and 503 are retried; when retries run out the user sees a clear message, never a stack trace or a blank screen.
3. The screen has loading, empty and error states, and its texts make sense to the persona who uses it.
4. Tests: at least one test for the business rule, and a stubbed test for each new sandbox call, including one failure case (`ai-rules/TESTING.md`).
5. The build is green locally and in CI.
6. The commit has no secrets, no dead code, no debug output, and no `TODO` that the demo depends on.

## 4. Scope discipline

- **Demo path first.** Build the one end-to-end journey the jury will see, make it solid, then widen.
- **Vertical slices.** One thin flow that works from screen to sandbox beats three half-built screens.
- **Every feature traces to the client.** It answers something the hosts said or the problem stated in the topic (`ai-rules/PROJECT-CONTEXT.md` §3). If it does not, ask before building it.
- **Fake what the demo does not show.** No login, user management, admin CRUD or settings pages unless the demo needs them; use configuration and seed data instead.
- **The cut list is binding.** Do not implement anything listed under "what we cut" in `ai-rules/PROJECT-CONTEXT.md` §3.
- **Feature freeze at 15:15.** Demos to the jury start at 16:00 (agenda in `ai-rules/PROJECT-CONTEXT.md` §1). After the freeze: bug fixes, two demo rehearsals, the pitch; the demo laptop is ready by 15:45. One successful run of the demo path is screen-recorded as the fallback if the sandbox or the network fails during the demo.

## 5. How you work

- Before code: read the relevant rules and the code you will touch; state a short plan (files, tests).
- Minimal, targeted changes; match existing patterns; no drive-by refactoring.
- Ambiguity: ask, or take the most restrictive reading. Name your uncertainty in the report, never hide it.
- Before reporting done: check §3. Report failures plainly.
- Stop and ask a person when:
  - adding a dependency or framework;
  - changing the contract between backend and frontend;
  - writing to the sandbox in bulk, or calling `POST /v1/sandbox/reset`;
  - the work goes beyond the demo path or touches the cut list (§4);
  - the same thing failed 3 times.

## 6. Secrets

- Team API key, POS PIN and access code, model key: backend environment only. Never in the browser, git, logs, test fixtures, prompts to the model, or screenshots.
- Layout:
  - `backend/.env` holds the real values; it is git-ignored and loaded by Spring Boot through `spring.config.import=optional:file:.env[.properties]`.
  - `backend/.env.example` is committed: every variable with an empty value and a one-line comment. A new variable goes into both files in the same commit.
  - Variable names: `SANDBOX_BASE_URL`, `SANDBOX_API_KEY`, `SANDBOX_ACCESS_CODE`, `SANDBOX_PIN`, `SANDBOX_DEVICE_ID`, `LLM_API_KEY`, `LLM_MODEL`.
- The frontend has no secrets. Vite ships every `VITE_*` variable to the browser, so a secret never goes into a `VITE_*` variable.
- HTTP logging masks the `X-API-KEY` and `Authorization` headers.
- Never print, echo or quote the contents of `.env`.
- If a secret reaches a commit: stop and tell the team. If the commit is not pushed yet, rewrite it. If it is pushed, ask the organizers to rotate the key.

## 7. Git

> Preliminary: the team may switch tactics on the day.

- Trunk-based: small commits straight to `main`, no long-lived branches.
- `git pull --rebase` before every push; never force-push `main`; push only what builds and passes tests.
- Commit messages follow Conventional Commits: `<type>(<scope>): <subject>`.
  - type: `feat`, `fix`, `test`, `refactor`, `docs`, `chore`, `ci`;
  - scope: `be`, `fe`, `ci`, `rules`;
  - subject: imperative, English, at most 72 characters; a body explains *why* when it is not obvious.
- One logical change per commit; tests go in the same commit as the code they cover.
- Everyone commits their own work under their own name: the history shows how the team worked.
- No AI co-author trailers (`Co-Authored-By: Claude …`) in commit messages.
- Before 11:00 on the event day: no `feat` commits (see §2).

## 8. Language

- Code, comments, docs and commit messages in English.
- Staff screens in English; guest-facing content in the guest's language.
