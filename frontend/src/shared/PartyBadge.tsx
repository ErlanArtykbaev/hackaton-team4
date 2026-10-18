import { Group, Paper, Text } from '@mantine/core'
import { AllergyChips } from './AllergenMarks'
import type { AllergenCode, SeverityCode } from './allergens'

type PartyMember = {
  name: string
  age?: number | null
  allergies: { allergen: AllergenCode; severity: SeverityCode }[]
}

/** One member of the party: "name, age: allergens". A member with a severe allergy gets a red border. */
export function PartyBadge({ member, testId }: { member: PartyMember; testId?: string }) {
  const severe = member.allergies.some((a) => a.severity === 'SEVERE')
  return (
    <Paper
      withBorder
      px="sm"
      py={6}
      style={severe ? { borderColor: 'var(--mantine-color-red-6)', borderWidth: 2 } : undefined}
      data-testid={testId ?? `party-badge-${member.name}`}
    >
      <Group gap="xs" wrap="nowrap">
        <Text fw={600} size="sm" style={{ whiteSpace: 'nowrap' }}>
          {member.name}
          {member.age != null ? `, ${member.age}` : ''}
        </Text>
        <AllergyChips allergies={member.allergies} size="sm" />
      </Group>
    </Paper>
  )
}

/** The whole party at the table or at the desk, one badge per member. */
export function PartyBadges({ members, testId }: { members: PartyMember[]; testId?: string }) {
  return (
    <Group gap="xs" data-testid={testId}>
      {members.map((m) => (
        <PartyBadge key={m.name} member={m} />
      ))}
    </Group>
  )
}
