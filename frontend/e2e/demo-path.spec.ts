import { expect, test, type Page } from '@playwright/test'

// The demo path of PLAN.md, run by hand before the rehearsals against the live sandbox (TESTING.md §5).
// Before a run: prepare-demo.mjs --reset, empty backend/data/, backend and `npm run dev` started.

const ROOM = '518'

/** This browser is the Pool Bar tablet of waiter W02 (the device setup, without clicking through it). */
async function poolBarDevice(page: Page) {
  await page.addInitScript(() =>
    localStorage.setItem(
      'team4.device',
      JSON.stringify({ rvc: 4, outletName: 'Pool Bar', staffCode: 'W02' }),
    ),
  )
}

/** The allergen chip of a member card (its label: the input itself is hidden by Mantine). */
function allergenChip(page: Page, member: number, label: string) {
  return page.getByTestId(`front-desk-member-${member}`).locator('label', { hasText: label })
}

/** Index of the member card with this name; adds the card when the passport has no such member yet. */
async function ensureMember(page: Page, name: string): Promise<number> {
  const names = await page
    .getByTestId(/^front-desk-member-\d+-name$/)
    .evaluateAll((inputs) => inputs.map((input) => (input as HTMLInputElement).value))
  if (names.includes(name)) return names.indexOf(name)
  await page.getByTestId('front-desk-member-add').click()
  await page.getByTestId(`front-desk-member-${names.length}-name`).fill(name)
  return names.length
}

/** A recorded allergy shows its severity switch on the member card. */
function hasAllergy(page: Page, member: number, allergen: string): Promise<boolean> {
  return page.getByTestId(`front-desk-member-${member}-severity-${allergen}`).isVisible()
}

/** Picks an option of a Mantine Select; option labels may start with an icon ("🥛 Milk"). */
async function choose(page: Page, testId: string, label: string) {
  await page.getByTestId(testId).click()
  await page.getByRole('option', { name: new RegExp(`(^|\\s)${label}$`) }).click()
}

/** Clicks one segment of a Mantine SegmentedControl. */
async function segment(page: Page, testId: string, label: string) {
  await page.getByTestId(testId).getByText(label, { exact: true }).click()
}

test.describe.configure({ mode: 'serial' })

test('1 · front desk records a card per family member', async ({ page }) => {
  await page.goto('/staff/front-desk')
  await page.getByTestId('front-desk-search').fill(ROOM)
  await page.getByTestId(`front-desk-reservation-${ROOM}`).click()
  await expect(page.getByTestId('front-desk-passport')).toBeVisible()

  // The primary guest comes from the PMS; Anna and Misha are added at check-in.
  await expect(page.getByTestId('front-desk-member-0-name')).toHaveValue('Mikhail')
  await ensureMember(page, 'Anna')
  const misha = await ensureMember(page, 'Misha')
  await page.getByTestId(`front-desk-member-${misha}-age`).fill('7')
  if (!(await hasAllergy(page, misha, 'PEANUTS'))) await allergenChip(page, misha, 'Peanuts').click()
  // PMS allergens arrive on the primary guest as SEVERE: move PEANUTS to Misha only.
  if (await hasAllergy(page, 0, 'PEANUTS')) await allergenChip(page, 0, 'Peanuts').click()

  // Save is enabled only when something changed (a re-run against a recorded passport changes nothing).
  const save = page.getByTestId('front-desk-save')
  if (await save.isEnabled()) {
    await save.click()
    await expect(page.getByTestId('front-desk-saved')).toContainText('PMS updated')
    await expect(page.getByTestId('front-desk-saved')).toContainText('Peanuts')
  }
  await expect(page.getByTestId(`front-desk-member-${misha}-severity-PEANUTS`)).toBeVisible()
  await expect(page.getByTestId('front-desk-member-0-severity-PEANUTS')).toHaveCount(0)
})

test('2–3 · waiter adds Anna’s milk intolerance and sends the order', async ({ page }) => {
  await poolBarDevice(page)
  await page.goto('/staff/waiter')
  await page.getByTestId('waiter-search').fill('Kuznetsov')
  await page.getByTestId(`waiter-result-${ROOM}`).click()
  await expect(page.getByTestId('waiter-party')).toContainText('Misha')

  // Anna says she is lactose intolerant.
  await page.getByTestId('waiter-passport-edit').click()
  await choose(page, 'waiter-passport-member', 'Anna')
  await choose(page, 'waiter-passport-allergen', 'Milk')
  await segment(page, 'waiter-passport-severity', 'Intolerance')
  await page.getByTestId('waiter-passport-save').click()
  await expect(page.getByTestId('waiter-party')).toContainText('Milk')

  const item = (name: string) => page.getByTestId(/^waiter-item-/).filter({ hasText: name })
  const addFor = async (name: string, member: string) => {
    await item(name).click()
    await segment(page, 'waiter-line-for', member)
    await page.getByTestId('waiter-line-add').click()
  }

  await addFor('Kids nuggets', 'Misha')
  await addFor('Coconut ice cream', 'Misha')
  await addFor('Chicken satay', 'Mikhail') // Misha (severe) at the table → waiter confirmation
  await addFor('Thai iced tea', 'Anna') // Anna's own allergen → guest consent

  const cart = page.getByTestId('waiter-cart')
  await cart
    .getByTestId(/^waiter-line-confirm-\d+$/)
    .first()
    .check()
  await cart
    .getByTestId(/^waiter-line-consent-\d+$/)
    .first()
    .click()
  await page.getByTestId('waiter-consent-name').fill('Anna Kuznetsova')
  await page.getByTestId('waiter-consent-accept').click()
  await expect(cart.getByTestId(/^waiter-line-consented-/)).toContainText('Anna Kuznetsova')

  // The demo table is not fixed yet (PLAN.md, demo prep step 3): take the first Pool Bar table.
  await page.getByTestId('waiter-table').click()
  await page.getByRole('option').first().click()
  await expect(page.getByTestId('waiter-send-blocker')).toHaveCount(0)
  await page.getByTestId('waiter-send').click()
  await expect(page.getByText('Sent to the kitchen')).toBeVisible()
})

test('4 · kitchen shows the ticket with allergens and for whom', async ({ page }) => {
  await poolBarDevice(page)
  await page.goto('/staff/kitchen')
  // Unchecked POS orders sit on top by design: find our round by its dish.
  const ticket = page
    .getByTestId(/^kitchen-ticket-\d+$/)
    .filter({ hasText: 'Thai iced tea' })
    .first()
  await expect(ticket).toBeVisible({ timeout: 10_000 })
  await expect(ticket).toContainText('Anna')
  await expect(ticket).toContainText('Chicken satay')
  await expect(ticket.getByTestId('kitchen-line-danger').first()).toBeVisible()
})

test('5 · manager finds the consent for room 518', async ({ page }) => {
  await page.goto('/staff/manager')
  await page.getByTestId('manager-search').fill(ROOM)
  const consent = page.getByTestId('manager-entry-GUEST_CONSENT').first()
  await expect(consent).toContainText('Thai iced tea')
  await expect(consent).toContainText('Anna')
  await expect(consent).toContainText('W02')
  await expect(page.getByTestId('manager-entry-WAITER_CONFIRMED').first()).toContainText('Chicken satay')
  await expect(page.getByTestId('manager-entry-PASSPORT_UPDATED').first()).toBeVisible()
})
