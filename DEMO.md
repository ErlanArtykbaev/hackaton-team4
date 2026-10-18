# DEMO.md — run sheet for 16:00

Story: Kuznetsov family, room 518 (R-203844), Pool Bar (rvc 4). Each step proves one acceptance criterion (AC) from the deck.

## Format

| Part | Time | What |
|---|---|---|
| Slides 1–4 | ~1:30 | problem → solution → the six acceptance criteria |
| **Live demo** | ~3:00 | five steps below, the presenter names the AC at each step |
| Slide 5 | ~0:20 | engineering criteria and next steps, if time allows |

If the two-minute deck is a separate round, the 16:00 slot is the live demo only, opened with one sentence on the criteria.

## Roles

| Who | Job |
|---|---|
| Sergey | speaks, names the AC at each step |
| Nikolai | drives the demo laptop (tabs are open in advance) |
| Erlan | on cue, sends the bartender's order straight into the POS from his laptop (step 4) |
| Gennadii | watches the backend log; starts the fallback video if needed |

## Preparation

| Time | Action | Who |
|---|---|---|
| 15:15 | Feature freeze. Demo laptop: clean clone of `main`, `backend/.env` with the **App key**, default rate limit | Sergey |
| 15:15 | One full run, screen-recorded: the fallback video | Nikolai |
| 15:15–15:35 | Two timed rehearsals; reset the hotel after each (once per minute, takes up to 30 s) | all |
| 15:40 | Final reset, below | Sergey |

Final reset, on the demo laptop, from `backend/`:

```bash
node scripts/prepare-demo.mjs --reset
```

Then stop the backend, delete `backend/data/`, start the backend and the frontend again. The script checks that room 518 is in house with only PEANUTS in the PMS and that no check is open at the Pool Bar.

Erlan's laptop: `POS_BARTENDER_PIN` set in `backend/.env`; dry run `node scripts/pos-order.mjs` once after the reset (sends nothing).

**Screen:** browser zoom 125 %, four tabs: front desk · waiter (device: Pool Bar, W02) · kitchen (Pool Bar) · manager. For steps 3–4, waiter on the left half, kitchen on the right half.

## Script

| # | AC | Click | Say |
|---|---|---|---|
| 1 | AC1 | Front desk → search 518 → the PMS shows PEANUTS on Mikhail's profile → add cards: Mikhail, Anna, **Misha, 7 — PEANUTS, severe** (move PEANUTS from Mikhail to Misha) → Save → "PMS updated" | "The PMS keeps one profile per booking. We keep a card per person, and the PMS gets the union." |
| 2 | AC2, AC4 | Waiter → search "Kuznetsov" → badges per person → satay, Som tam, Pad thai red for Misha; Spring rolls, Kids nuggets green → Anna: "I'm lactose intolerant" → add MILK (intolerance) to Anna → Espresso's Whole milk turns red → choose oat milk | "A new allergy in the middle of the stay, and all four outlets see it at once." |
| 3 | AC3 | Table **B4**. Misha: Kids nuggets + Coconut ice cream. Mikhail: Chicken satay for himself → waiter confirms "Misha is at the table". Anna insists on Thai iced tea (MILK) → hand the tablet to "Anna", she types her name → Send | "Nothing is blocked. The guest decides, the system records the consent." |
| 4 | AC5 | Kitchen (right half): the ticket arrives within seconds — allergen lines, for whom, the consent mark. **Cue Erlan:** `node scripts/pos-order.mjs --send` (Som tam for 518 at table B5) → a red ticket "not checked at ordering — PEANUTS: Misha" on top | "The kitchen knows about every order, even one typed past our tablet." |
| 5 | AC6 | Manager → search 518: passport changes (front desk, waiter W02), waiter confirmation, Anna's consent with time and waiter, the unchecked POS order, the shift numbers | "After an incident the hotel can prove it was the guest's decision, not a mistake by the waiter or the kitchen." |

Close: "Six criteria, live on the hotel's real PMS and POS. Next: staff sign-in with the POS PIN, the guest's own passport by QR, translated screens."

## When something goes wrong

| Problem | Do |
|---|---|
| The sandbox is slow or answers 503 | the client retries; keep talking for 5–10 s |
| No ticket on the kitchen screen | reload the kitchen tab: tickets are loaded again |
| Room 518 is in a broken state | switch to the backup family: room 729 (R-203981, Thomas: GLUTEN, FISH, MUSTARD) |
| No network | play the 15:15 recording |
| A step fails twice | say what should happen, move to the next step |

## Open points

- Slot length; whether the two-minute deck is a separate round; projector connector; network on stage — ask the organizers.
- Demo laptop: whose (`PLAN.md`).
- Who plays Anna at step 3: Erlan or someone from the jury.
