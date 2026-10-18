import { Badge, Group, Paper, Stack, Text } from '@mantine/core'
import { AllergenChip } from '../../../shared/AllergenMarks'
import { hotelDayTime } from '../../../shared/format'
import { LOG_MARK } from '../logMarks'
import type { LogEntry } from '../managerApi'

/** One record of the safety log: what happened, to whom, where, by whom, and the statement as recorded. */
export function LogEntryCard({ entry }: { entry: LogEntry }) {
  const mark = entry.type ? LOG_MARK[entry.type] : null
  const consent = entry.type === 'GUEST_CONSENT'
  const where = [
    entry.outletName,
    entry.roomNumber && `Room ${entry.roomNumber}`,
    entry.memberName,
    entry.actor && `by ${entry.actor}`,
  ].filter(Boolean)

  return (
    <Paper
      withBorder
      p="sm"
      bg={consent ? 'var(--mantine-color-red-light)' : undefined}
      style={consent ? { borderColor: 'var(--mantine-color-red-6)', borderWidth: 2 } : undefined}
      data-testid={`manager-entry-${entry.type ?? 'UNKNOWN'}`}
    >
      <Stack gap={4}>
        <Group justify="space-between" wrap="nowrap">
          <Group gap="xs">
            {mark && (
              <Badge color={mark.color} variant={consent ? 'filled' : 'light'} tt="none">
                {mark.icon} {mark.label}
              </Badge>
            )}
            {entry.item && <Text fw={600}>{entry.item}</Text>}
            {entry.allergen && <AllergenChip allergen={entry.allergen} severity={entry.severity} size="sm" />}
          </Group>
          <Text size="sm" c="dimmed" style={{ whiteSpace: 'nowrap' }}>
            {hotelDayTime(entry.at)}
          </Text>
        </Group>
        <Text size="sm" c="dimmed">
          {where.join(' · ')}
        </Text>
        {entry.statement && (
          <Text size="sm" fw={consent ? 600 : undefined} fs={consent ? 'italic' : undefined}>
            {consent ? `“${entry.statement}”` : entry.statement}
          </Text>
        )}
      </Stack>
    </Paper>
  )
}
