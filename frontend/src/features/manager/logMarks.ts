import type { LogType } from './managerApi'

type LogMark = { label: string; color: string; icon: string }

/** How each kind of record reads in the log. Guest consent is the proof after an incident: it stands out. */
export const LOG_MARK: Record<LogType, LogMark> = {
  GUEST_CONSENT: { label: 'Guest consent', color: 'red', icon: '✍️' },
  WAITER_CONFIRMED: { label: 'Waiter confirmation', color: 'orange', icon: '☑️' },
  POS_ORDER_UNCHECKED: { label: 'Not checked at ordering', color: 'grape', icon: '⚠️' },
  ORDER_PLACED: { label: 'Order placed', color: 'teal', icon: '🧾' },
  PASSPORT_UPDATED: { label: 'Passport updated', color: 'blue', icon: '🪪' },
}

export const LOG_TYPES = Object.keys(LOG_MARK) as LogType[]
