import { Badge, Group } from '@mantine/core'
import type { Allergen, Verdict } from '../waiterApi'
import { VERDICT_COLOR } from './marks'

const VERDICT_LABEL: Record<Verdict, string> = {
  SAFE: 'Safe',
  WARNING: 'Intolerance',
  DANGER: 'Danger',
  UNKNOWN: 'Not checked',
}

export function VerdictBadge({ verdict }: { verdict: Verdict }) {
  return (
    <Badge color={VERDICT_COLOR[verdict]} variant="filled" data-verdict={verdict}>
      {VERDICT_LABEL[verdict]}
    </Badge>
  )
}

export function AllergenChips({ allergens, size = 'sm' }: { allergens: Allergen[]; size?: 'xs' | 'sm' }) {
  if (allergens.length === 0) return null
  return (
    <Group gap={4}>
      {allergens.map((a) => (
        <Badge key={a} size={size} variant="outline" color="gray">
          {a}
        </Badge>
      ))}
    </Group>
  )
}
