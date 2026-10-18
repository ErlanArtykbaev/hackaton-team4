import type { DangerFor, Severity, Verdict } from '../waiterApi'

// Local marks until the shared design (FE-B0) lands in `shared/`.
export const VERDICT_COLOR: Record<Verdict, string> = {
  SAFE: 'green',
  WARNING: 'orange',
  DANGER: 'red',
  UNKNOWN: 'gray',
}

export function severityLabel(severity: Severity): string {
  return severity === 'SEVERE' ? 'severe' : 'intolerance'
}

/** "PEANUTS — Misha (severe)" per member at risk. */
export function dangerText(dangers: DangerFor[]): string {
  return dangers.map((d) => `${d.allergen} — ${d.memberName} (${severityLabel(d.severity)})`).join(', ')
}

export const money = new Intl.NumberFormat('en', {
  style: 'currency',
  currency: 'THB',
  maximumFractionDigits: 0,
})
