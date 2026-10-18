import { useCallback, useMemo, useState, type ReactNode } from 'react'
import { DeviceContext, type Device } from './deviceContext'

const STORAGE_KEY = 'team4.device'
const EMPTY: Device = { rvc: null, outletName: null, staffCode: null }

function load(): Device {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? { ...EMPTY, ...(JSON.parse(raw) as Partial<Device>) } : EMPTY
  } catch {
    return EMPTY // storage blocked (private window): the device works for this session only
  }
}

export function DeviceProvider({ children }: { children: ReactNode }) {
  const [device, setDevice] = useState<Device>(load)
  const save = useCallback((next: Device) => {
    setDevice(next)
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
    } catch {
      // storage blocked: keep the value in memory only
    }
  }, [])
  const value = useMemo(() => ({ device, save }), [device, save])
  return <DeviceContext.Provider value={value}>{children}</DeviceContext.Provider>
}
