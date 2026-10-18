import { Alert, Badge, Button, Group, Loader, SimpleGrid, Stack, Title } from '@mantine/core'
import { EmptyState } from '../../../shared/EmptyState'
import { ErrorAlert } from '../../../shared/ErrorAlert'
import { useDevice } from '../../../shared/deviceContext'
import { TicketCard } from '../components/TicketCard'
import { useChime } from '../hooks/useChime'
import { useFreshTickets } from '../hooks/useFreshTickets'
import { sortTickets, useTicketStream, useTickets } from '../kitchenApi'

/** Kitchen display: every round sent to this outlet's kitchen, live, with allergens and for whom they are dangerous. */
export function KitchenPage() {
  const { device } = useDevice()
  const live = useTicketStream(device.rvc)
  const tickets = useTickets(device.rvc, live)
  const chime = useChime()
  const fresh = useFreshTickets(tickets.data, (added) =>
    chime.play(added.some((t) => t.source === 'UNCHECKED' && t.hasAllergens)),
  )

  if (device.rvc == null) {
    return (
      <Alert color="yellow" title="Set up this display" data-testid="kitchen-setup-needed">
        Choose the outlet with the button at the top right.
      </Alert>
    )
  }

  return (
    <Stack data-testid="kitchen-page">
      <Group justify="space-between">
        <Title order={2}>Kitchen — {device.outletName}</Title>
        <Group>
          <Button
            variant={chime.enabled ? 'light' : 'default'}
            onClick={chime.toggle}
            data-testid="kitchen-sound"
          >
            {chime.enabled ? 'Sound on' : 'Sound off'}
          </Button>
          <Badge color={live ? 'green' : 'yellow'} variant="dot" size="lg" data-testid="kitchen-live">
            {live ? 'Live' : 'Reconnecting…'}
          </Badge>
        </Group>
      </Group>
      {tickets.isLoading && <Loader />}
      {tickets.isError && (
        <ErrorAlert error={tickets.error} onRetry={() => void tickets.refetch()} testId="kitchen-error" />
      )}
      {tickets.isSuccess && tickets.data.length === 0 && (
        <EmptyState
          icon="🍳"
          title="No orders yet."
          hint="Rounds sent from a waiter tablet or typed into the POS appear here."
        />
      )}
      <SimpleGrid cols={{ base: 1, md: 2, xl: 3 }}>
        {sortTickets(tickets.data ?? []).map((t) => (
          <TicketCard key={String(t.id)} ticket={t} fresh={fresh.has(String(t.id))} />
        ))}
      </SimpleGrid>
    </Stack>
  )
}
