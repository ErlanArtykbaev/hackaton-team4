import { Badge, Button, Group, Paper, Stack, Text } from '@mantine/core'
import type { Passport } from '../waiterApi'
import { severityLabel } from './marks'

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
              <Paper key={String(m.id ?? m.name)} withBorder p="xs" data-testid={`waiter-member-${m.name}`}>
                <Text fw={600}>
                  {m.name}
                  {m.age != null ? `, ${m.age}` : ''}
                </Text>
                <Group gap={4} mt={4}>
                  {m.allergies.length === 0 && (
                    <Badge color="green" variant="light">
                      No allergies
                    </Badge>
                  )}
                  {m.allergies.map((a) => (
                    <Badge
                      key={a.allergen}
                      color={a.severity === 'SEVERE' ? 'red' : 'orange'}
                      variant={a.severity === 'SEVERE' ? 'filled' : 'light'}
                    >
                      {a.allergen} · {severityLabel(a.severity)}
                    </Badge>
                  ))}
                </Group>
              </Paper>
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
