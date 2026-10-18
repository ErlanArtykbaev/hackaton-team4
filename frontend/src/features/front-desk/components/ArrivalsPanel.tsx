import { Badge, Group, Loader, Paper, Stack, Text, TextInput, UnstyledButton } from '@mantine/core'
import { useDebouncedValue } from '@mantine/hooks'
import { useState } from 'react'
import { AllergenChips } from '../../../shared/AllergenMarks'
import { ErrorAlert } from '../../../shared/ErrorAlert'
import { hotelDate } from '../../../shared/format'
import {
  isSearchable,
  useArrivals,
  usePassportStream,
  useReservationSearch,
  type ReservationSummary,
} from '../frontDeskApi'

/** Worklist of today's arrivals, or the search results while the agent types a room or a last name. */
export function ArrivalsPanel({
  selectedId,
  onSelect,
}: {
  selectedId: string | null
  onSelect: (reservationId: string) => void
}) {
  const [query, setQuery] = useState('')
  const [debounced] = useDebouncedValue(query, 300)
  const searching = isSearchable(debounced)
  const arrivals = useArrivals()
  usePassportStream()
  const search = useReservationSearch(debounced)
  const list = searching ? search : arrivals

  return (
    <Stack gap="sm">
      <TextInput
        placeholder="Room number or last name"
        value={query}
        onChange={(e) => setQuery(e.currentTarget.value)}
        rightSection={list.isFetching ? <Loader size="xs" /> : null}
        data-testid="front-desk-search"
      />
      <Text size="sm" fw={600} c="dimmed">
        {searching ? 'Search results' : 'Arrivals today — allergies or children'}
      </Text>

      {list.isPending && <Loader size="sm" data-testid="front-desk-list-loading" />}
      {list.isError && <ErrorAlert error={list.error} onRetry={() => void list.refetch()} />}
      {list.data?.length === 0 && (
        <Text size="sm" c="dimmed" data-testid="front-desk-list-empty">
          {searching
            ? 'No guest in house or arriving today with this room or name.'
            : 'No arrivals with allergies or children today.'}
        </Text>
      )}

      {list.data?.map((r) => (
        <ReservationRow
          key={r.reservationId}
          reservation={r}
          selected={r.reservationId === selectedId}
          onSelect={onSelect}
        />
      ))}
    </Stack>
  )
}

function ReservationRow({
  reservation: r,
  selected,
  onSelect,
}: {
  reservation: ReservationSummary
  selected: boolean
  onSelect: (reservationId: string) => void
}) {
  const children = r.children ?? 0
  return (
    <UnstyledButton
      onClick={() => r.reservationId && onSelect(r.reservationId)}
      data-testid={`front-desk-reservation-${r.roomNumber}`}
    >
      <Paper
        withBorder
        p="sm"
        bg={selected ? 'var(--mantine-primary-color-light)' : undefined}
        style={selected ? { borderColor: 'var(--mantine-primary-color-filled)' } : undefined}
      >
        <Group justify="space-between" wrap="nowrap" align="flex-start">
          <div>
            <Text fw={600}>
              Room {r.roomNumber} · {r.guestName}
            </Text>
            <Text size="xs" c="dimmed">
              {hotelDate(r.arrivalDate)} – {hotelDate(r.departureDate)} · {r.adults ?? 0} adult
              {r.adults === 1 ? '' : 's'}
              {children > 0 ? `, ${children} child${children === 1 ? '' : 'ren'}` : ''}
            </Text>
          </div>
          {r.hasPassport ? (
            <Badge color="teal" variant="light" tt="none">
              Passport
            </Badge>
          ) : (
            <Badge color="orange" variant="filled" tt="none" data-testid="front-desk-no-passport">
              No passport
            </Badge>
          )}
        </Group>
        {(r.pmsAllergens?.length ?? 0) > 0 && (
          <Group gap={6} mt={6}>
            <AllergenChips allergens={r.pmsAllergens ?? []} size="xs" />
          </Group>
        )}
      </Paper>
    </UnstyledButton>
  )
}
