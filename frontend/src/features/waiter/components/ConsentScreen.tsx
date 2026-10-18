import { Button, Group, Modal, Paper, Stack, Text, TextInput, Title } from '@mantine/core'
import { useState } from 'react'
import type { LineCheck } from '../waiterApi'
import type { CartLine } from './Cart'
import { severityLabel } from './marks'

/** The waiter hands the tablet to the guest: the guest reads what the dish contains and signs with their name. */
export function ConsentScreen({
  line,
  check,
  onGive,
  onClose,
}: {
  line: CartLine | null
  check: LineCheck | undefined
  onGive: (givenBy: string) => void
  onClose: () => void
}) {
  return (
    <Modal opened={line != null} onClose={onClose} fullScreen withCloseButton={false}>
      {line && check && (
        <ConsentForm key={line.key} line={line} check={check} onGive={onGive} onClose={onClose} />
      )}
    </Modal>
  )
}

function ConsentForm({
  line,
  check,
  onGive,
  onClose,
}: {
  line: CartLine
  check: LineCheck
  onGive: (givenBy: string) => void
  onClose: () => void
}) {
  const [name, setName] = useState('')
  const allergens = [...new Set(check.dangerFor.map((d) => d.allergen))].join(', ')
  const child = line.forMember?.age != null && line.forMember.age < 18

  return (
    <Stack maw={720} mx="auto" mt="xl" gap="lg" data-testid="waiter-consent">
      <Title order={1}>{line.item.name}</Title>
      <Text size="xl">for {line.forMember ? line.forMember.name : 'the whole table'}</Text>
      <Paper withBorder p="lg" bg="red.0">
        <Text size="xl" fw={700} c="red.8">
          Contains {allergens}
        </Text>
        {check.dangerFor.map((d) => (
          <Text key={`${String(d.memberId)}-${d.allergen}`} size="lg">
            {d.allergen} — {d.memberName} ({severityLabel(d.severity)})
          </Text>
        ))}
      </Paper>
      <Text size="xl" fw={600}>
        I know this dish contains {allergens} and I order it.
      </Text>
      {child && <Text size="lg">For a child, a parent gives the consent.</Text>}
      <TextInput
        size="xl"
        label="Your name"
        value={name}
        onChange={(e) => setName(e.currentTarget.value)}
        data-testid="waiter-consent-name"
        autoFocus
      />
      <Group justify="space-between">
        <Button size="xl" variant="default" onClick={onClose} data-testid="waiter-consent-cancel">
          Back
        </Button>
        <Button
          size="xl"
          color="violet"
          disabled={name.trim().length < 2}
          onClick={() => onGive(name.trim())}
          data-testid="waiter-consent-accept"
        >
          I agree and order
        </Button>
      </Group>
    </Stack>
  )
}
