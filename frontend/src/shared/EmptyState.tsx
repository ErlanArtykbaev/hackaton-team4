import { Paper, Stack, Text } from '@mantine/core'

/** A screen or list with nothing to show yet: what is missing and what makes it appear. */
export function EmptyState({
  icon,
  title,
  hint,
  testId,
}: {
  icon: string
  title: string
  hint?: string
  testId?: string
}) {
  return (
    <Paper withBorder p="xl" data-testid={testId}>
      <Stack gap={4} align="center" ta="center">
        <Text fz={40} lh={1} aria-hidden>
          {icon}
        </Text>
        <Text fw={600} size="lg">
          {title}
        </Text>
        {hint && <Text c="dimmed">{hint}</Text>}
      </Stack>
    </Paper>
  )
}
