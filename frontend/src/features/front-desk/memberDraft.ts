import { ALLERGEN_CODES, type AllergenCode, type SeverityCode } from '../../shared/allergens'
import type { Passport, PassportUpdate } from './frontDeskApi'

/** One family member as the front desk edits it: form state, mapped to the passport only on save. */
export type MemberDraft = {
  key: string
  id?: string
  name: string
  age: number | null
  primary: boolean
  allergies: { allergen: AllergenCode; severity: SeverityCode }[]
}

let nextKey = 0
export function newKey(): string {
  nextKey += 1
  return `new-${nextKey}`
}

export function emptyMember(): MemberDraft {
  return { key: newKey(), name: '', age: null, primary: false, allergies: [] }
}

/** A new allergy starts as SEVERE: the safer reading until the guest says it is an intolerance. */
export function toggleAllergen(member: MemberDraft, allergen: AllergenCode): MemberDraft {
  const has = member.allergies.some((a) => a.allergen === allergen)
  const allergies = has
    ? member.allergies.filter((a) => a.allergen !== allergen)
    : [...member.allergies, { allergen, severity: 'SEVERE' as const }].sort(
        (a, b) => ALLERGEN_CODES.indexOf(a.allergen) - ALLERGEN_CODES.indexOf(b.allergen),
      )
  return { ...member, allergies }
}

export function setSeverity(
  member: MemberDraft,
  allergen: AllergenCode,
  severity: SeverityCode,
): MemberDraft {
  return {
    ...member,
    allergies: member.allergies.map((a) => (a.allergen === allergen ? { ...a, severity } : a)),
  }
}

/** What stops saving, per member key; empty when the passport can be saved. */
export function problems(members: MemberDraft[]): Record<string, string> {
  const result: Record<string, string> = {}
  const names = new Set<string>()
  for (const m of members) {
    const name = m.name.trim().toLowerCase()
    if (!name) result[m.key] = 'Enter the name.'
    else if (names.has(name)) result[m.key] = 'Two members have the same name: add a surname or initial.'
    else if (m.age != null && (m.age < 0 || m.age > 120)) result[m.key] = 'Age must be between 0 and 120.'
    names.add(name)
  }
  return result
}

type PassportMember = NonNullable<Passport['members']>[number]

/** Drafts from the stored passport; fields the backend leaves out or sends as null get safe defaults. */
export function toDrafts(passport: Passport): MemberDraft[] {
  return (passport.members ?? []).map((m: PassportMember) => ({
    key: m.id ?? newKey(),
    id: m.id ?? undefined,
    name: m.name ?? '',
    age: m.age ?? null,
    primary: m.primary ?? false,
    allergies: (m.allergies ?? []).flatMap((a) =>
      a.allergen && a.severity ? [{ allergen: a.allergen, severity: a.severity }] : [],
    ),
  }))
}

/** The PUT body: names trimmed, age left out when unknown (an adult). */
export function toUpdate(members: MemberDraft[], updatedBy: string): PassportUpdate {
  return {
    members: members.map((m) => ({
      id: m.id,
      name: m.name.trim(),
      age: m.age ?? undefined,
      primary: m.primary,
      allergies: m.allergies,
    })),
    updatedBy,
    source: 'FRONT_DESK',
  }
}
