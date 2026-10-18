// Visual vocabulary shared by every staff screen: allergen icons, severity and verdict colours.
// The codes come from the generated contract (`api/schema.d.ts`); a new enum value fails the type check here.
import type { components } from '../api/schema'

type Schemas = components['schemas']
export type AllergenCode = NonNullable<Schemas['MemberAllergy']['allergen']>
export type SeverityCode = NonNullable<Schemas['MemberAllergy']['severity']>
export type VerdictCode = NonNullable<Schemas['MenuItem']['verdict']>

/** The 14 EU allergens as the sandbox names them, with an icon and a label for staff. */
export const ALLERGEN_META = {
  GLUTEN: { icon: '🌾', label: 'Gluten' },
  CRUSTACEANS: { icon: '🦐', label: 'Crustaceans' },
  EGGS: { icon: '🥚', label: 'Eggs' },
  FISH: { icon: '🐟', label: 'Fish' },
  PEANUTS: { icon: '🥜', label: 'Peanuts' },
  SOYBEANS: { icon: '🫘', label: 'Soy' },
  MILK: { icon: '🥛', label: 'Milk' },
  NUTS: { icon: '🌰', label: 'Tree nuts' },
  CELERY: { icon: '🥬', label: 'Celery' },
  MUSTARD: { icon: '🫙', label: 'Mustard' },
  SESAME: { icon: '🥯', label: 'Sesame' },
  SULPHITES: { icon: '🍷', label: 'Sulphites' },
  LUPIN: { icon: '🌸', label: 'Lupin' },
  MOLLUSCS: { icon: '🦪', label: 'Molluscs' },
} as const satisfies Record<AllergenCode, { icon: string; label: string }>

export const ALLERGEN_CODES = Object.keys(ALLERGEN_META) as AllergenCode[]

type Mark = { color: string; variant: 'filled' | 'light'; label: string }

/** SEVERE is life-threatening and shown as danger; INTOLERANCE is shown, but quieter. */
export const SEVERITY_MARK: Record<SeverityCode, Mark> = {
  SEVERE: { color: 'red', variant: 'filled', label: 'severe' },
  INTOLERANCE: { color: 'orange', variant: 'light', label: 'intolerance' },
}

/** Verdict of a dish or condiment for the party at the table. */
export const VERDICT_MARK: Record<VerdictCode, Mark> = {
  SAFE: { color: 'green', variant: 'filled', label: 'Safe' },
  WARNING: { color: 'orange', variant: 'filled', label: 'Intolerance' },
  DANGER: { color: 'red', variant: 'filled', label: 'Danger' },
  UNKNOWN: { color: 'gray', variant: 'light', label: 'Not checked' },
}

export function allergenLabel(code: AllergenCode): string {
  return ALLERGEN_META[code].label
}

/** Strongest severity first, then the canonical allergen order: the dangerous chip is always leftmost. */
export function byRisk<T extends { allergen: AllergenCode; severity: SeverityCode }>(allergies: T[]): T[] {
  return [...allergies].sort(
    (a, b) =>
      Number(b.severity === 'SEVERE') - Number(a.severity === 'SEVERE') ||
      ALLERGEN_CODES.indexOf(a.allergen) - ALLERGEN_CODES.indexOf(b.allergen),
  )
}
