import {
  ActionIcon,
  Alert,
  Badge,
  Button,
  Checkbox,
  Group,
  Paper,
  Select,
  Stack,
  Text,
  Title,
} from '@mantine/core'
import type { LineCheck } from '../waiterApi'
import type { LineDraft } from './LineEditor'
import { dangerText, money } from './marks'
import { VerdictBadge } from '../../../shared/AllergenMarks'

export type CartLine = LineDraft & { key: string; waiterConfirmed: boolean; consentBy: string | null }

export function Cart({
  lines,
  checks,
  checkError,
  tables,
  table,
  onTable,
  onConfirm,
  onConsent,
  onRemove,
  onSend,
  sending,
  blocker,
}: {
  lines: CartLine[]
  checks: LineCheck[] | undefined
  checkError: string | null
  tables: string[]
  table: string | null
  onTable: (table: string | null) => void
  onConfirm: (key: string, confirmed: boolean) => void
  onConsent: (key: string) => void
  onRemove: (key: string) => void
  onSend: () => void
  sending: boolean
  blocker: string | null
}) {
  const total = lines.reduce(
    (sum, l) => sum + l.quantity * (l.item.price + l.condiments.reduce((s, c) => s + c.price, 0)),
    0,
  )

  return (
    <Paper withBorder p="md" data-testid="waiter-cart">
      <Stack>
        <Group justify="space-between">
          <Title order={3}>Order</Title>
          {tables.length > 0 && (
            <Select
              placeholder="Table"
              data={tables}
              value={table}
              onChange={onTable}
              w={120}
              data-testid="waiter-table"
            />
          )}
        </Group>
        {lines.length === 0 && <Text c="dimmed">Tap a dish to add it.</Text>}
        {lines.map((line, index) => {
          const check = checks?.[index]
          return (
            <Paper key={line.key} withBorder p="sm" data-testid={`waiter-line-${index}`}>
              <Group justify="space-between" wrap="nowrap" align="flex-start">
                <div>
                  <Text fw={600}>
                    {line.quantity} × {line.item.name}
                  </Text>
                  {line.condiments.length > 0 && (
                    <Text size="sm" c="dimmed">
                      {line.condiments.map((c) => c.name).join(', ')}
                    </Text>
                  )}
                  <Text size="sm">for {line.forMember ? line.forMember.name : 'the table'}</Text>
                </div>
                <Group gap="xs" wrap="nowrap">
                  {check && <VerdictBadge verdict={check.verdict} />}
                  <ActionIcon
                    variant="subtle"
                    color="gray"
                    onClick={() => onRemove(line.key)}
                    aria-label="Remove"
                  >
                    ✕
                  </ActionIcon>
                </Group>
              </Group>
              {check && check.dangerFor.length > 0 && (
                <Text size="sm" c="red" mt={4}>
                  {dangerText(check.dangerFor)}
                </Text>
              )}
              {check?.requirement === 'WAITER_CONFIRMATION' && (
                <Checkbox
                  mt="xs"
                  size="md"
                  checked={line.waiterConfirmed}
                  onChange={(e) => onConfirm(line.key, e.currentTarget.checked)}
                  label={`I confirmed with the guest: not for ${atRisk(check)}, who is at the table`}
                  data-testid={`waiter-line-confirm-${index}`}
                />
              )}
              {check?.requirement === 'GUEST_CONSENT' &&
                (line.consentBy ? (
                  <Badge mt="xs" color="violet" size="lg" data-testid={`waiter-line-consented-${index}`}>
                    Consent given by {line.consentBy}
                  </Badge>
                ) : (
                  <Button
                    mt="xs"
                    color="violet"
                    fullWidth
                    onClick={() => onConsent(line.key)}
                    data-testid={`waiter-line-consent-${index}`}
                  >
                    Guest consent on this tablet
                  </Button>
                ))}
            </Paper>
          )
        })}
        {checkError && <Alert color="red">{checkError}</Alert>}
        {lines.length > 0 && (
          <>
            <Group justify="space-between">
              <Text fw={600}>Total</Text>
              <Text fw={600}>{money.format(total)}</Text>
            </Group>
            {blocker && (
              <Text size="sm" c="dimmed" data-testid="waiter-send-blocker">
                {blocker}
              </Text>
            )}
            <Button
              size="lg"
              disabled={blocker != null}
              loading={sending}
              onClick={onSend}
              data-testid="waiter-send"
            >
              Send to kitchen
            </Button>
          </>
        )}
      </Stack>
    </Paper>
  )
}

function atRisk(check: LineCheck): string {
  return [...new Set(check.dangerFor.map((d) => `${d.memberName} (${d.allergen})`))].join(', ')
}
