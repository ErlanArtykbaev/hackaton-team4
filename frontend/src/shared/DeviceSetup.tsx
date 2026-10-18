import { Button, Modal, Select, Stack, Text } from '@mantine/core'
import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { STAFF } from './staff'
import { useDevice } from './deviceContext'

// Minimal shape until the generated API types exist (FE-B1); then switch to the generated `Outlet` type.
type OutletOption = { rvc: number; name: string }

async function fetchOutlets(): Promise<OutletOption[]> {
  const response = await fetch('/api/outlets')
  if (!response.ok) {
    throw new Error(`Outlets: HTTP ${response.status}`)
  }
  return (await response.json()) as OutletOption[]
}

export function DeviceSetup({ opened, onClose }: { opened: boolean; onClose: () => void }) {
  const { device, save } = useDevice()
  const [rvc, setRvc] = useState<string | null>(device.rvc?.toString() ?? null)
  const [staffCode, setStaffCode] = useState<string | null>(device.staffCode)
  const outlets = useQuery({ queryKey: ['outlets'], queryFn: fetchOutlets, enabled: opened })

  const confirm = () => {
    const outlet = outlets.data?.find((o) => o.rvc.toString() === rvc)
    save({ rvc: outlet?.rvc ?? null, outletName: outlet?.name ?? null, staffCode })
    onClose()
  }

  return (
    <Modal opened={opened} onClose={onClose} title="This device">
      <Stack>
        <Select
          label="Outlet"
          placeholder={outlets.isLoading ? 'Loading…' : 'Choose the outlet'}
          data={(outlets.data ?? []).map((o) => ({ value: o.rvc.toString(), label: o.name }))}
          value={rvc}
          onChange={setRvc}
          data-testid="device-outlet"
        />
        {outlets.isError && (
          <Text c="red" size="sm">
            Outlets are not available. Is the backend running?
          </Text>
        )}
        <Select
          label="Staff member"
          placeholder="Who uses this device"
          data={STAFF.map((s) => ({ value: s.code, label: `${s.code} · ${s.name} (${s.role})` }))}
          value={staffCode}
          onChange={setStaffCode}
          data-testid="device-staff"
        />
        <Button onClick={confirm} data-testid="device-save">
          Save
        </Button>
      </Stack>
    </Modal>
  )
}
