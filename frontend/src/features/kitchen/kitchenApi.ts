import { useQuery, useQueryClient } from '@tanstack/react-query'
import { ApiError, problemOf } from '../../shared/errors'
import { useStream } from '../../shared/useStream'

// Shapes of the frozen API contract (PLAN.md) until the generated types exist (FE-B1); then switch to `api/schema`.
export type Severity = 'INTOLERANCE' | 'SEVERE'
export type Verdict = 'SAFE' | 'WARNING' | 'DANGER' | 'UNKNOWN'
export type DangerFor = {
  memberId: string | number
  memberName: string
  allergen: string
  severity: Severity
}
export type TicketSource = 'CHECKED' | 'UNCHECKED' | 'UNLINKED'

export type TicketLine = {
  name: string
  quantity: number
  condiments: string[]
  allergens: string[]
  verdict: Verdict
  dangerFor: DangerFor[]
  forMemberName?: string | null
  waiterConfirmed: boolean
  consentBy?: string | null
}
export type KitchenTicket = {
  id: string | number
  rvc: number
  checkNumber: number
  table?: string | null
  roomNumber?: string | null
  guestName?: string | null
  sentAt: string
  source: TicketSource
  hasAllergens: boolean
  lines: TicketLine[]
}

async function fetchTickets(rvc: number): Promise<KitchenTicket[]> {
  let response: Response
  try {
    response = await fetch(`/api/kitchen/${rvc}/tickets`)
  } catch {
    throw new ApiError(0, undefined, 'No response from our server')
  }
  if (!response.ok) throw await problemOf(response)
  return (await response.json()) as KitchenTicket[]
}

/** Tickets of one outlet; polled only while the live stream is down. */
export function useTickets(rvc: number | null, live: boolean) {
  return useQuery({
    queryKey: ['kitchen', rvc],
    queryFn: () => fetchTickets(rvc ?? 0),
    enabled: rvc != null,
    refetchInterval: live ? false : 10_000,
  })
}

/** Pushes `ticket` events of `/api/stream` into the ticket list; returns whether the stream is connected. */
export function useTicketStream(rvc: number | null): boolean {
  const client = useQueryClient()
  return useStream<KitchenTicket>(
    'ticket',
    (ticket) => {
      if (rvc == null || ticket.rvc !== rvc) return
      client.setQueryData<KitchenTicket[]>(['kitchen', rvc], (old = []) => [
        ticket,
        ...old.filter((t) => String(t.id) !== String(ticket.id)),
      ])
    },
    () => void client.invalidateQueries({ queryKey: ['kitchen', rvc] }), // catch up on what came while disconnected
  )
}

/** "Not checked at ordering" first, then newest first. */
export function sortTickets(tickets: KitchenTicket[]): KitchenTicket[] {
  const rank = (t: KitchenTicket) => (t.source === 'UNCHECKED' && t.hasAllergens ? 0 : 1)
  return [...tickets].sort((a, b) => rank(a) - rank(b) || b.sentAt.localeCompare(a.sentAt))
}
