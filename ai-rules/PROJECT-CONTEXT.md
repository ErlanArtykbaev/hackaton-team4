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

1. **Front desk — allergy passport:** at check-in a card per family member (name, age, allergens with severity); today's arrivals with allergies as a worklist.
2. **Waiter — order tablet:** find the party by room or last name; the menu of the outlet with a verdict per item and per condiment; "for whom" on each line; safe alternatives; waiter confirmation and **guest consent** where the rules require them; add a member or an allergen to the passport after asking the guest; send the order to the POS on a check linked to the reservation.
3. **Kitchen — order display:** every round sent to the kitchen, from our tablet or typed straight into the POS, with its allergens and for whom they are dangerous. **Notification only**: the kitchen does not check or approve.
4. **Manager — safety log:** orders placed, waiter confirmations, guest consents, passport changes, unchecked POS orders; searchable by room or last name, so the hotel can **prove** after an incident that a dish was ordered with the guest's explicit consent.

**Domain rules** (deterministic code, no language model in this topic) — confirmed by the hosts:

- **A card per person.** The PMS keeps one guest profile per reservation; family members live in our database. On every save the PMS profile gets the union of all members' allergens.
- Severity: `INTOLERANCE` or `SEVERE` (life-threatening); both are shown, `SEVERE` as danger.
- A line contains an allergen for a member when the allergens of the item **plus its chosen condiments** intersect the member's allergens.
- What an order line needs, by "for whom":
  - for a member allergic to it, or for the table while any member is allergic to it → **guest consent**: the guest confirms on the tablet with their name ("I know it contains X and I order it"); recorded with dish, allergen, severity, waiter, outlet, time. For a child, a parent gives the consent.
  - for a member not allergic to it while another member at the table is → **waiter confirmation** (one tap), recorded.
  - nobody at the table allergic → nothing.
  - Nothing is ever blocked: the decision is the guest's, and the record proves it.
- **Walk-ins and friends** (no reservation): the menu shows allergens only, verdict `UNKNOWN`; nothing is matched.
- **The waiter may update the passport** after asking the guest's name: add a member or an allergen; recorded with the waiter as author and synced to the PMS.
- The kitchen display shows every `pos.check.round_sent` event. A round not ordered through our tablet is marked "not checked at ordering"; a check without a reservation shows allergens only.
- Guests are found by room number or last name. Staff screens, the kitchen included, are in English.

**Needs a person's confirmation:** guest consent (guest, on the tablet), waiter confirmation (waiter), passport cards (front desk, or the waiter after asking the guest).

**Cut list:** guest self-service passport; kitchen approval and escalation; "may contain" and cross-contamination; printing and export; translation of staff screens; payment and room charge; login; real devices.

**Still open:** in-house reservations whose departure date has passed (seen in the data) — treat them as in house until told otherwise.

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
