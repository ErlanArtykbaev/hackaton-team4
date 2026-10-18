import { Alert, Button, Group, Text } from '@mantine/core'
import { errorMessage } from './errors'

/** Error state of a screen or a section: the message by error code, and a retry when the call can be repeated. */
export function ErrorAlert({
  error,
  onRetry,
  testId = 'error-alert',
}: {
  error: unknown
  onRetry?: () => void
  testId?: string
}) {
  return (
    <Alert color="red" variant="light" data-testid={testId}>
      <Group justify="space-between" wrap="nowrap">
        <Text size="sm">{errorMessage(error)}</Text>
        {onRetry && (
          <Button size="xs" variant="white" color="red" onClick={onRetry} data-testid={`${testId}-retry`}>
            Try again
          </Button>
        )}
      </Group>
    </Alert>
  )
}
