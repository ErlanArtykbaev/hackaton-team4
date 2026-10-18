# DEMO.md — run sheet for 16:00

Story: Kuznetsov family, room 222 (R-203156), Pool Bar (rvc 4). Each step proves one acceptance criterion (AC) from the deck.

## Format — about 10 minutes

| Part | Time | What |
|---|---|---|
| Slides 1–4 | 0:00–2:00 | problem → solution → the six acceptance criteria (the two-minute deck) |
| **Live demo** | 2:00–7:00 | the five steps below, about a minute each, the presenter names the AC at each step |
| How it is built | 7:00–8:00 | slide 5, then 20 s in the repository: green CI, tests, `PLAN.md` and the commit history |
| Questions | 8:00–10:00 | answers below; if ahead of time, the bonus: a walk-in at the Beach Bar sees allergens only |

## Roles

| Who | Job |
|---|---|
| Sergey | speaks, names the AC at each step |
| Nikolai | drives the demo laptop (tabs are open in advance) |
| Erlan | plays Anna: gives the guest consent on the demo laptop (step 3) |
| Gennadii | on cue, sends the bartender's order straight into the POS from his laptop (step 4); watches the backend log; starts the fallback video if needed |

## Preparation

| Time | Action | Who |
|---|---|---|
| 15:15 | Feature freeze. Demo laptop = **Sergey's laptop** on the projector: clean clone of `main`, `backend/.env` with the **App key**, default rate limit and `SANDBOX_EVENTS_SKIP_HISTORY=true` (a fresh database must not replay old rounds on the kitchen screen) | Sergey |
| 15:15 | One full run, screen-recorded: the fallback video | Nikolai |
| 15:15–15:35 | Two timed rehearsals; reset the hotel after each (once per minute, takes up to 30 s) | all |
| 15:40 | Final reset, below | Sergey |

Final preparation, on the demo laptop, from `backend/` — **no hotel reset**: on the event day a reset handed our reservations to other guests.

```bash
node scripts/prepare-demo.mjs --prepare --tables=B7,B8
```

It puts PEANUTS on Ivan's PMS profile and checks that room 222 is in house and the demo tables are free. Then stop the backend, delete `backend/data/`, start the backend and the frontend again (`SANDBOX_EVENTS_SKIP_HISTORY=true` keeps the kitchen empty). Tables: **B4 / B6** for the rehearsal, **B7 / B8** for the demo itself (a run leaves open checks behind).

Gennadii's laptop: `POS_BARTENDER_PIN` set in `backend/.env`; dry run `node scripts/pos-order.mjs` once after the reset (sends nothing).

**Screen:** browser zoom 125 %, four tabs: front desk · waiter (device: Pool Bar, W02) · kitchen (Pool Bar) · manager. For steps 3–4, waiter on the left half, kitchen on the right half.

## Script

| # | AC | Click | Say |
|---|---|---|---|
| 1 | AC1 | Front desk → search 222 → the PMS shows PEANUTS on Ivan's profile → add cards: Ivan, Anna, **Misha, 7 — PEANUTS, severe** (move PEANUTS from Ivan to Misha) → Save → "PMS updated" | "The PMS keeps one profile per booking. We keep a card per person, and the PMS gets the union." |
| 2 | AC2, AC4 | Waiter → search "Kuznetsov" → badges per person → satay, Som tam, Pad thai red for Misha; Spring rolls, Kids nuggets green → Anna: "I'm lactose intolerant" → add MILK (intolerance) to Anna → Espresso's Whole milk turns red → choose oat milk | "A new allergy in the middle of the stay, and all four outlets see it at once." |
| 3 | AC3 | Table **B4** (demo: **B7**). Misha: Kids nuggets + Coconut ice cream. Ivan: Chicken satay for himself → waiter confirms "Misha is at the table". Anna insists on Thai iced tea (MILK) → Erlan, as Anna, steps up and types her name → Send | "Nothing is blocked. The guest decides, the system records the consent." |
| 4 | AC5 | Kitchen (right half): the ticket arrives within seconds — allergen lines, for whom, the consent mark. **Cue Gennadii:** `node scripts/pos-order.mjs --send` (Som tam for 222 at table B6; demo: `--table B8`) → a red ticket "not checked at ordering — PEANUTS: Misha" on top | "The kitchen knows about every order, even one typed past our tablet." |
| 5 | AC6 | Manager → search 222: passport changes (front desk, waiter W02), waiter confirmation, Anna's consent with time and waiter, the unchecked POS order, the shift numbers | "After an incident the hotel can prove it was the guest's decision, not a mistake by the waiter or the kitchen." |

Close: "Six criteria, live on the hotel's real PMS and POS. Next: staff sign-in with the POS PIN, the guest's own passport by QR, translated screens."

## Expected questions

| Question | Answer |
|---|---|
| Who is the waiter? There is no sign-in. | Out of scope for today: the device names its staff member. Next step: sign-in with the staff member's own POS PIN, checked by the hotel's POS, so the log names a verified person. |
| Why not block a dangerous dish? | The hosts decided: the guest decides. We make the danger impossible to miss and record the consent as proof. |
| Allergies are health data. | Today we keep only allergens and severity per person, and the PMS gets only the union of allergens. For production: delete after checkout and limit access by role. |
| What if the PMS or POS is down? | Safe calls retry with backoff; orders are never retried blindly, so no double check or charge; the screens show a clear message. |
| What about a child's allergy the parents forgot? | The waiter adds it on the spot after asking; every outlet sees it at once. |
| How fast is the kitchen? | The POS event reaches the kitchen screen within about 5 seconds (feed polled every 3 s, pushed to the screen). |

## When something goes wrong

| Problem | Do |
|---|---|
| The sandbox is slow or answers 503 | the client retries; keep talking for 5–10 s |
| No ticket on the kitchen screen | reload the kitchen tab: tickets are loaded again |
| Room 222 is in a broken state | switch to the backup family: room 729 (R-203981, Thomas: GLUTEN, FISH, MUSTARD) |
| No network | play the 15:15 recording |
| A step fails twice | say what should happen, move to the next step |

## Open points

- Slot length and whether the two-minute deck is a separate round — ask the organizers. A projector is there; check the connector for Sergey's laptop before 15:15.
