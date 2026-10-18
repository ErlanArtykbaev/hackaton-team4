import { Group, Paper, SimpleGrid, Stack, Switch, Text, Title } from '@mantine/core'
import { useState } from 'react'
import type { MenuItem } from '../waiterApi'
import { VERDICT_COLOR, dangerText, money } from './marks'
import { AllergenChips, VerdictBadge } from './VerdictMarks'

export function MenuList({
  items,
  walkIn,
  onPick,
}: {
  items: MenuItem[]
  walkIn: boolean
  onPick: (item: MenuItem) => void
}) {
  const [onlySafe, setOnlySafe] = useState(false)
  const names = new Map(items.map((i) => [i.code, i.name]))
  const visible = onlySafe ? items.filter((i) => i.verdict === 'SAFE') : items
  const groups = [...new Set(visible.map((i) => i.group))]

  return (
    <Stack>
      <Group justify="space-between">
        <Title order={3}>Menu</Title>
        {walkIn ? (
          <Text c="dimmed">Walk-in: allergens only, nothing is matched.</Text>
        ) : (
          <Switch
            label="Only safe for the whole table"
            checked={onlySafe}
            onChange={(e) => setOnlySafe(e.currentTarget.checked)}
            data-testid="waiter-only-safe"
          />
        )}
      </Group>
      {visible.length === 0 && <Text c="dimmed">No dish is safe for everyone at this table.</Text>}
      {groups.map((group) => (
        <Stack key={group} gap="xs">
          <Text fw={700} tt="uppercase" c="dimmed" size="sm">
            {group}
          </Text>
          <SimpleGrid cols={{ base: 1, sm: 2, lg: 3 }}>
            {visible
              .filter((i) => i.group === group)
              .map((item) => (
                <Paper
                  key={item.code}
                  withBorder
                  p="sm"
                  style={{
                    cursor: 'pointer',
                    borderLeft: `6px solid var(--mantine-color-${VERDICT_COLOR[item.verdict]}-6)`,
                  }}
                  onClick={() => onPick(item)}
                  data-testid={`waiter-item-${item.code}`}
                  data-verdict={item.verdict}
                >
                  <Group justify="space-between" wrap="nowrap" align="flex-start">
                    <Text fw={600}>{item.name}</Text>
                    <Text size="sm">{money.format(item.price)}</Text>
                  </Group>
                  <Group justify="space-between" mt={6}>
                    <AllergenChips allergens={item.allergens} size="xs" />
                    {!walkIn && <VerdictBadge verdict={item.verdict} />}
                  </Group>
                  {item.dangerFor.length > 0 && (
                    <Text size="sm" c={VERDICT_COLOR[item.verdict]} mt={6} fw={600}>
                      {dangerText(item.dangerFor)}
                    </Text>
                  )}
                  {item.verdict !== 'SAFE' && item.safeAlternatives.length > 0 && (
                    <Text size="sm" c="dimmed" mt={4}>
                      Safe instead: {item.safeAlternatives.map((c) => names.get(c) ?? c).join(', ')}
                    </Text>
                  )}
                </Paper>
              ))}
          </SimpleGrid>
        </Stack>
      ))}
    </Stack>
  )
}
