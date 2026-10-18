import { Button, Group, Paper, Stack, Text } from '@mantine/core'
import { PartyBadge } from '../../../shared/PartyBadge'
import type { Passport } from '../waiterApi'

/** Who sits at the table: one badge per member with their allergens. */
export function PartyHeader({
  passport,
  onEditPassport,
  onChangeParty,
}: {
  passport: Passport
  onEditPassport: () => void
  onChangeParty: () => void
}) {
  return (
    <Paper withBorder p="md" data-testid="waiter-party">
      <Group justify="space-between" align="flex-start">
        <Stack gap="xs">
          <Text fw={700} size="lg">
            Room {passport.roomNumber} · {passport.guestName}
          </Text>
          <Group gap="sm">
            {passport.members.map((m) => (
              <PartyBadge key={String(m.id ?? m.name)} member={m} testId={`waiter-member-${m.name}`} />
            ))}
          </Group>
        </Stack>
        <Group>
          <Button variant="light" onClick={onEditPassport} data-testid="waiter-passport-edit">
            Update passport
          </Button>
          <Button variant="subtle" onClick={onChangeParty} data-testid="waiter-party-change">
            Change party
          </Button>
        </Group>
      </Group>
    </Paper>
  )
}
