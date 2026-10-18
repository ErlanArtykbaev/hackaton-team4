import { Stack, Text, Title } from '@mantine/core'

/** Waiter tablet: party search, menu with verdicts, order with "for whom". Built in task FE-A2 and FE-A3 (PLAN.md). */
export function WaiterPage() {
  return (
    <Stack data-testid="waiter-page">
      <Title order={2}>Waiter</Title>
      <Text c="dimmed">Coming in FE-A2 and FE-A3.</Text>
    </Stack>
  )
}
