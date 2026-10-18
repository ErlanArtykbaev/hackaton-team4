import { errorMessage } from '../../../shared/errors'
import { Alert, Button, Group, Loader, Paper, Stack, Text, TextInput } from '@mantine/core'
import { useDebouncedValue } from '@mantine/hooks'
import { useState } from 'react'
import { isSearchable, useReservations, type ReservationSummary } from '../waiterApi'
import { AllergenChips } from './VerdictMarks'

export function PartySearch({
  onSelect,
  onWalkIn,
}: {
  onSelect: (reservation: ReservationSummary) => void
  onWalkIn: () => void
}) {
  const [query, setQuery] = useState('')
  const [debounced] = useDebouncedValue(query, 300)
  const reservations = useReservations(debounced)

  return (
    <Stack>
      <Group align="flex-end">
        <TextInput
          label="Find the party"
          placeholder="Room number or last name"
          size="lg"
          style={{ flex: 1 }}
          value={query}
          onChange={(e) => setQuery(e.currentTarget.value)}
          rightSection={reservations.isFetching ? <Loader size="sm" /> : null}
          data-testid="waiter-search"
          autoFocus
        />
        <Button size="lg" variant="default" onClick={onWalkIn} data-testid="waiter-walk-in">
          Walk-in (no room)
        </Button>
      </Group>
      {reservations.isError && <Alert color="red">{errorMessage(reservations.error)}</Alert>}
      {isSearchable(debounced) && reservations.isSuccess && reservations.data.length === 0 && (
        <Text c="dimmed">No guest in house matches “{debounced.trim()}”.</Text>
      )}
      {reservations.data?.map((r) => (
        <Paper
          key={r.reservationId}
          withBorder
          p="md"
          style={{ cursor: 'pointer' }}
          onClick={() => onSelect(r)}
          data-testid={`waiter-result-${r.roomNumber}`}
        >
          <Group justify="space-between">
            <div>
              <Text fw={700} size="lg">
                Room {r.roomNumber} · {r.guestName}
              </Text>
              <Text size="sm" c="dimmed">
                {r.adults} adults{r.children ? `, ${r.children} children` : ''} · until {r.departureDate}
                {r.hasPassport ? ' · allergy passport' : ''}
              </Text>
            </div>
            <AllergenChips allergens={r.pmsAllergens} />
          </Group>
        </Paper>
      ))}
    </Stack>
  )
}
