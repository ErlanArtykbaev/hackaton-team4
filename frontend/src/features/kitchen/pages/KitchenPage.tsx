import { Alert, Badge, Group, Loader, SimpleGrid, Stack, Text, Title } from '@mantine/core'
import { ErrorAlert } from '../../../shared/ErrorAlert'
import { useDevice } from '../../../shared/deviceContext'
import { TicketCard } from '../components/TicketCard'
import { sortTickets, useTicketStream, useTickets } from '../kitchenApi'

/** Kitchen display: every round sent to this outlet's kitchen, live, with allergens and for whom they are dangerous. */
export function KitchenPage() {
  const { device } = useDevice()
  const live = useTicketStream(device.rvc)
  const tickets = useTickets(device.rvc, live)

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
        <Badge color={live ? 'green' : 'yellow'} variant="dot" size="lg" data-testid="kitchen-live">
          {live ? 'Live' : 'Reconnecting…'}
        </Badge>
      </Group>
      {tickets.isLoading && <Loader />}
      {tickets.isError && (
        <ErrorAlert error={tickets.error} onRetry={() => void tickets.refetch()} testId="kitchen-error" />
      )}
      {tickets.isSuccess && tickets.data.length === 0 && <Text c="dimmed">No orders yet.</Text>}
      <SimpleGrid cols={{ base: 1, md: 2, xl: 3 }}>
        {sortTickets(tickets.data ?? []).map((t) => (
          <TicketCard key={String(t.id)} ticket={t} />
        ))}
      </SimpleGrid>
    </Stack>
  )
}
