# PROJECT-CONTEXT.md — this repository

> Status: **ACTIVE** — topic 2, Allergy passport. Tasks and owners: `PLAN.md`.

## §1. Event

- TYNGO × Sonata hackathon, Tbilisi, Sunday 2026-10-18, Paragraph Freedom Square, Conference Hall 1.
- The hosts play the hotel staff; interviewing them is part of the task.

| Time (Tbilisi) | What |
|---|---|
| 11:00 | Teams start working |
| 13:00 | Lunch |
| 13:45 | Second session |
| 15:15 | Our feature freeze (`CLAUDE.md` §4) |
| 16:00 | Team demos to the jury |
| 17:00 | Awards and close |

## §2. Team

| Member | Company | Side | Slot in `PLAN.md` |
|---|---|---|---|
| Zubov Sergey | TYNGO | FE on the day | FE-A, tech lead |
| Ivliev Nikolai | — | FE on the day | FE-B (replaces Dudko Aleksandr) |
| Chzhen Gennadii | TYNGO | BE on the day | BE-A |
| Artykbaev Erlan | Sonata | BE on the day | BE-B |

## §3. Topic — Allergy passport, TYNGO Resort Phuket

- **Hotel:** 320 rooms; 4 outlets: Main Restaurant (rvc 1), Beach Bar (rvc 2), Room Service (rvc 3), Pool Bar (rvc 4); 35 % families with children; 6 nights on average. Hotel time Asia/Bangkok (UTC+7, 3 hours ahead of Tbilisi); money in THB.
- **Client:** "Hundreds of guests and four places to eat. A mistake with a child's allergen is the worst thing that can happen to a resort."
- **Personas:** front desk agent (records allergies), waiter or bartender (takes orders), cook (prepares), shift manager (responsible when something goes wrong), parent (orders for a child).
- **The problem in one sentence:** allergies recorded at check-in stay in the PMS, so the waiter in any of the four outlets and the kitchen cannot see which dish is dangerous for whom.

**What we build** — one web app, routes per persona (`FRONTEND.md` §2):

1. **Front desk — allergy passport:** family members per reservation, each with allergens and severity; today's arrivals with allergies as a worklist.
2. **Waiter — order tablet:** find the party by room or name; menu of the outlet with a verdict per item and per condiment; "for whom" on each line; safe alternatives; send the order to the POS on a check linked to the reservation.
3. **Kitchen — order display:** every round sent to the kitchen, from our tablet or typed straight into the POS, checked against the passport; dangerous lines show for whom; a dangerous ticket needs the cook's acknowledgement.
4. **Manager — safety log:** warnings, overrides with reasons, kitchen acknowledgements, escalations, with times and outlets.

**Domain rules** (deterministic code, no language model in this topic):

- The PMS keeps one guest profile per reservation; **family members live in our database**. On save, the PMS profile gets the union of all members' allergens.
- Severity: `INTOLERANCE` or `SEVERE` (life-threatening).
- A line is dangerous for a member when the allergens of the item **plus its chosen condiments** intersect the member's allergens.
- "For whom" on a line:
  - for the allergic member, `SEVERE` → blocked, no override; `INTOLERANCE` → warning, reason required;
  - for someone else or the table while a member at the table has the allergen → warning, confirmation and reason required.
- Every warning, override and confirmation goes to the safety log with outlet, waiter and time.
- The kitchen checks every `pos.check.round_sent` event. A round not ordered through our tablet is marked "not checked at ordering". A check without a reservation shows allergens only ("guest unknown").
- A dangerous ticket not acknowledged within 3 minutes escalates to the manager.

**Needs a person's confirmation:** overrides (waiter, with reason); dangerous tickets (cook); passport changes (front desk).

**Cut list:** guest self-service passport (stretch only); "may contain" and cross-contamination; printing; translation of staff screens; payment and room charge; login; real devices.

**Open questions for the hosts:** who is responsible for the check (waiter or kitchen); a guest who insists on a dangerous dish; which language the cooks read; are children's names and ages known at check-in; in-house reservations whose departure date has passed (seen in the data).

## §4. Stack

- Backend: Java 21, Spring Boot 3, Maven, H2 file + Liquibase — `BACKEND.md`.
- Frontend: React, Vite, TypeScript — `FRONTEND.md`.
- Contract between backend and frontend: code-first, TypeScript types generated from the backend's OpenAPI document.
- Staff screens in English.
- AI coding agent: Claude Code. No language model in the product for this topic.

## §5. Environments

- Sandbox `https://hackathon2026.tyngo.com` (`BACKEND.md` §5); the backend logs in to the POS as `INT01`.
- **Dev key** in `backend/.env` on every developer laptop (development, tests, Postman); **App key** only on the demo laptop. Each key allows 10 requests per second.
- Staff PINs (waiters, bartenders, manager) come from the organizers' handout; they live only in `.env` and in the scripts' environment.
- The sandbox answers unfiltered reservation lists slowly (8–11 s): always filter (room, status, last name).
