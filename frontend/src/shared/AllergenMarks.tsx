import { Badge, Group, type BadgeProps } from '@mantine/core'
import {
  ALLERGEN_META,
  SEVERITY_MARK,
  VERDICT_MARK,
  byRisk,
  type AllergenCode,
  type SeverityCode,
  type VerdictCode,
} from './allergens'

type Size = BadgeProps['size']

/**
 * One allergen with its icon. Without a severity it is neutral (what a dish contains);
 * with a severity it takes the severity colour (what a guest must avoid).
 */
export function AllergenChip({
  allergen,
  severity,
  size = 'sm',
  testId,
}: {
  allergen: AllergenCode
  severity?: SeverityCode
  size?: Size
  testId?: string
}) {
  const meta = ALLERGEN_META[allergen]
  const mark = severity ? SEVERITY_MARK[severity] : null
  return (
    <Badge
      size={size}
      color={mark?.color ?? 'gray'}
      variant={mark?.variant ?? 'outline'}
      leftSection={<span aria-hidden>{meta.icon}</span>}
      tt="none"
      data-testid={testId ?? `allergen-${allergen}`}
      data-allergen={allergen}
      data-severity={severity}
    >
      {meta.label}
      {mark ? ` · ${mark.label}` : ''}
    </Badge>
  )
}

/** What a dish or condiment contains. Renders nothing when the list is empty. */
export function AllergenChips({ allergens, size = 'sm' }: { allergens: AllergenCode[]; size?: Size }) {
  if (allergens.length === 0) return null
  return (
    <Group gap={4}>
      {allergens.map((a) => (
        <AllergenChip key={a} allergen={a} size={size} />
      ))}
    </Group>
  )
}

export function SeverityBadge({ severity, size = 'sm' }: { severity: SeverityCode; size?: Size }) {
  const mark = SEVERITY_MARK[severity]
  return (
    <Badge
      size={size}
      color={mark.color}
      variant={mark.variant}
      data-testid={`severity-${severity}`}
      data-severity={severity}
    >
      {mark.label}
    </Badge>
  )
}

export function VerdictBadge({
  verdict,
  size = 'sm',
  testId,
}: {
  verdict: VerdictCode
  size?: Size
  testId?: string
}) {
  const mark = VERDICT_MARK[verdict]
  return (
    <Badge
      size={size}
      color={mark.color}
      variant={mark.variant}
      data-testid={testId ?? `verdict-${verdict}`}
      data-verdict={verdict}
    >
      {mark.label}
    </Badge>
  )
}

/** The allergies of one person, most dangerous first; "No allergies" when there are none. */
export function AllergyChips({
  allergies,
  size = 'sm',
}: {
  allergies: { allergen: AllergenCode; severity: SeverityCode }[]
  size?: Size
}) {
  if (allergies.length === 0) {
    return (
      <Badge size={size} color="green" variant="light" tt="none" data-testid="allergy-none">
        No allergies
      </Badge>
    )
  }
  return (
    <Group gap={4}>
      {byRisk(allergies).map((a) => (
        <AllergenChip key={a.allergen} allergen={a.allergen} severity={a.severity} size={size} />
      ))}
    </Group>
  )
}
