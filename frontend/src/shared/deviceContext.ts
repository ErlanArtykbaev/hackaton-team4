import { createContext, useContext } from 'react'

/** Which outlet and which staff member this browser is used by. Set once per device, kept in the browser. */
export type Device = {
  rvc: number | null
  outletName: string | null
  staffCode: string | null
}

export type DeviceContextValue = { device: Device; save: (next: Device) => void }

export const DeviceContext = createContext<DeviceContextValue | null>(null)

export function useDevice(): DeviceContextValue {
  const context = useContext(DeviceContext)
  if (!context) {
    throw new Error('useDevice must be used inside DeviceProvider')
  }
  return context
}
