import { Paper, SimpleGrid, Skeleton, Text } from '@mantine/core'
import type { Summary } from '../managerApi'

const TILES: { key: keyof Summary; label: string; alertWhenAny?: boolean }[] = [
  { key: 'ordersChecked', label: 'Orders checked' },
  { key: 'linesWithAllergens', label: 'Lines with allergens' },
  { key: 'waiterConfirmations', label: 'Waiter confirmations' },
  { key: 'guestConsents', label: 'Guest consents' },
  { key: 'uncheckedPosOrders', label: 'Unchecked POS orders', alertWhenAny: true },
]

/** Shift KPIs. Unchecked POS orders turn red: allergens reached the kitchen without our check. */
export function SummaryTiles({ summary }: { summary: Summary | undefined }) {
  return (
    <SimpleGrid cols={{ base: 2, sm: 5 }} spacing="sm" data-testid="manager-kpis">
      {TILES.map((tile) => {
        const value = summary?.[tile.key]
        const alert = tile.alertWhenAny && (value ?? 0) > 0
        return (
          <Paper
            key={tile.key}
            withBorder
            p="sm"
            bg={alert ? 'var(--mantine-color-red-light)' : undefined}
            data-testid={`manager-kpi-${tile.key}`}
          >
            <Text size="xs" c="dimmed">
              {tile.label}
            </Text>
            {value == null ? (
              <Skeleton h={28} w={40} mt={4} />
            ) : (
              <Text fw={700} fz={28} c={alert ? 'red' : undefined}>
                {value}
              </Text>
            )}
          </Paper>
        )
      })}
    </SimpleGrid>
  )
}
