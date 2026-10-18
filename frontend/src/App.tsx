import { AppShell, Button, Group, Stack, Text, Title } from '@mantine/core'
import { useDisclosure } from '@mantine/hooks'
import { NavLink, Navigate, Route, Routes } from 'react-router'
import { FrontDeskPage } from './features/front-desk/pages/FrontDeskPage'
import { KitchenPage } from './features/kitchen/pages/KitchenPage'
import { ManagerPage } from './features/manager/pages/ManagerPage'
import { WaiterPage } from './features/waiter/pages/WaiterPage'
import { DeviceSetup } from './shared/DeviceSetup'
import { useDevice } from './shared/deviceContext'

const STAFF_ROUTES = [
  { path: '/staff/front-desk', label: 'Front desk', testId: 'nav-front-desk' },
  { path: '/staff/waiter', label: 'Waiter', testId: 'nav-waiter' },
  { path: '/staff/kitchen', label: 'Kitchen', testId: 'nav-kitchen' },
  { path: '/staff/manager', label: 'Manager', testId: 'nav-manager' },
]

export function App() {
  const [setupOpened, setup] = useDisclosure(false)
  const { device } = useDevice()
  const deviceLabel = device.staffCode
    ? `${device.outletName ?? 'No outlet'} · ${device.staffCode}`
    : 'Set up device'

  return (
    <AppShell header={{ height: 64 }} padding="md">
      <AppShell.Header>
        <Group h="100%" px="md" justify="space-between" wrap="nowrap">
          <Group gap="xs" wrap="nowrap">
            <Stack gap={0} mr="xs" style={{ whiteSpace: 'nowrap' }}>
              <Title order={4} lh={1.2} textWrap="nowrap">
                Allergy passport
              </Title>
              <Text size="xs" c="dimmed" fw={600} data-testid="hotel-name">
                TYNGO Resort Phuket
              </Text>
            </Stack>
            {STAFF_ROUTES.map((route) => (
              <Button
                key={route.path}
                component={NavLink}
                to={route.path}
                variant="subtle"
                size="md"
                px="xs"
                data-testid={route.testId}
              >
                {route.label}
              </Button>
            ))}
          </Group>
          <Button
            variant="light"
            size="md"
            maw={190}
            styles={{ label: { overflow: 'hidden', textOverflow: 'ellipsis' } }}
            title={deviceLabel}
            onClick={setup.open}
            data-testid="device-setup-open"
          >
            {deviceLabel}
          </Button>
        </Group>
      </AppShell.Header>
      <AppShell.Main>
        <Routes>
          <Route path="/" element={<Navigate to="/staff/front-desk" replace />} />
          <Route path="/staff/front-desk" element={<FrontDeskPage />} />
          <Route path="/staff/waiter" element={<WaiterPage />} />
          <Route path="/staff/kitchen" element={<KitchenPage />} />
          <Route path="/staff/manager" element={<ManagerPage />} />
          <Route path="*" element={<Navigate to="/staff/front-desk" replace />} />
        </Routes>
      </AppShell.Main>
      <DeviceSetup opened={setupOpened} onClose={setup.close} />
    </AppShell>
  )
}
