import { ALLERGEN_META, SEVERITY_MARK, VERDICT_MARK } from '../../../shared/allergens'
import type { DangerFor, Verdict } from '../waiterApi'

export function verdictColor(verdict: Verdict): string {
  return VERDICT_MARK[verdict].color
}

/** "🥜 Peanuts — Misha (severe)" per member at risk. */
export function dangerText(dangers: DangerFor[]): string {
  return dangers
    .map(
      (d) =>
        `${ALLERGEN_META[d.allergen].icon} ${ALLERGEN_META[d.allergen].label} — ${d.memberName} (${SEVERITY_MARK[d.severity].label})`,
    )
    .join(', ')
}

export const money = new Intl.NumberFormat('en', {
  style: 'currency',
  currency: 'THB',
  maximumFractionDigits: 0,
})
