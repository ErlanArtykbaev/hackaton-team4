import { Badge, Group, Paper, Stack, Text } from '@mantine/core'
import { AllergenChip } from '../../../shared/AllergenMarks'
import { ALLERGEN_META, SEVERITY_MARK } from '../../../shared/allergens'
import type { KitchenTicket, TicketLine, TicketSource } from '../kitchenApi'

const hotelTime = new Intl.DateTimeFormat('en-GB', {
  timeZone: 'Asia/Bangkok',
  hour: '2-digit',
  minute: '2-digit',
})

const SOURCE: Record<TicketSource, { label: string; color: string }> = {
  CHECKED: { label: 'Checked at ordering', color: 'green' },
  UNCHECKED: { label: 'Not checked at ordering', color: 'red' },
  UNLINKED: { label: 'No reservation — allergens only', color: 'gray' },
}

/** One round sent to the kitchen. Notification only: the kitchen does not approve anything. */
export function TicketCard({ ticket }: { ticket: KitchenTicket }) {
  const source = SOURCE[ticket.source]
  const alarm = ticket.source === 'UNCHECKED' && ticket.hasAllergens

  return (
    <Paper
      withBorder
      p="md"
      bg={alarm ? 'red.0' : undefined}
      style={{
        borderWidth: ticket.hasAllergens ? 3 : 1,
        borderColor: ticket.hasAllergens ? 'var(--mantine-color-red-6)' : undefined,
      }}
      data-testid={`kitchen-ticket-${ticket.checkNumber}`}
      data-source={ticket.source}
    >
      <Group justify="space-between" align="flex-start">
        <div>
          <Text fw={800} size="xl">
            {ticket.table ? `Table ${ticket.table}` : `Check ${ticket.checkNumber}`}
            {ticket.roomNumber ? ` · Room ${ticket.roomNumber}` : ''}
          </Text>
          {ticket.guestName && <Text c="dimmed">{ticket.guestName}</Text>}
        </div>
        <Stack gap={4} align="flex-end">
          <Text fw={700} size="lg">
            {hotelTime.format(new Date(ticket.sentAt))}
          </Text>
          <Badge
            color={source.color}
            variant={alarm ? 'filled' : 'light'}
            size="lg"
            data-testid="kitchen-ticket-source"
          >
            {source.label}
          </Badge>
        </Stack>
      </Group>
      <Stack gap="xs" mt="sm">
        {ticket.lines.map((line, i) => (
          <Line key={i} line={line} />
        ))}
      </Stack>
    </Paper>
  )
}

function Line({ line }: { line: TicketLine }) {
  const danger = line.dangerFor.length > 0
  return (
    <div data-testid="kitchen-line">
      <Group gap="xs">
        <Text fw={700} size="lg">
          {line.quantity} × {line.name}
        </Text>
        {line.condiments.length > 0 && <Text c="dimmed">({line.condiments.join(', ')})</Text>}
        {line.allergens.map((a) => (
          <AllergenChip key={a} allergen={a} severity={worstSeverity(line, a)} />
        ))}
      </Group>
      {danger && (
        <Text c="red.8" fw={700} data-testid="kitchen-line-danger">
          {lineWarning(line)}
        </Text>
      )}
    </div>
  )
}

/** "MILK — Anna (intolerance) · guest consent" or "PEANUTS — for Mikhail; Misha (severe) at the table". */
function lineWarning(line: TicketLine): string {
  const allergens = [
    ...new Set(
      line.dangerFor.map((d) => `${ALLERGEN_META[d.allergen].icon} ${ALLERGEN_META[d.allergen].label}`),
    ),
  ].join(', ')
  const people = line.dangerFor.map((d) => `${d.memberName} (${SEVERITY_MARK[d.severity].label})`)
  const forOther = line.forMemberName && !line.dangerFor.some((d) => d.memberName === line.forMemberName)
  const who = forOther
    ? `for ${line.forMemberName}; ${[...new Set(people)].join(', ')} at the table`
    : [...new Set(people)].join(', ')
  const marks = [
    line.consentBy ? `guest consent (${line.consentBy})` : null,
    line.waiterConfirmed ? 'waiter confirmed' : null,
  ]
    .filter(Boolean)
    .join(' · ')
  return `${allergens} — ${who}${marks ? ` · ${marks}` : ''}`
}

/** The severity this allergen has for someone at the table; none when it endangers nobody. */
function worstSeverity(line: TicketLine, allergen: TicketLine['allergens'][number]) {
  const hits = line.dangerFor.filter((d) => d.allergen === allergen)
  if (hits.length === 0) return undefined
  return hits.some((d) => d.severity === 'SEVERE') ? 'SEVERE' : 'INTOLERANCE'
}
