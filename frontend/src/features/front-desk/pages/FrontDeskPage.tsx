import { Stack, Text, Title } from '@mantine/core'

/** Front desk: arrivals worklist and the family allergy passport. Built in task FE-B2 (PLAN.md). */
export function FrontDeskPage() {
  return (
    <Stack data-testid="front-desk-page">
      <Title order={2}>Front desk — allergy passport</Title>
      <Text c="dimmed">Coming in FE-B2.</Text>
    </Stack>
  )
}
