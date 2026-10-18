import { useEffect, useRef, useState } from 'react'
import type { KitchenTicket } from '../kitchenApi'

/**
 * Ids of tickets that arrived after the screen opened (by stream or by polling); the first load counts as seen.
 * `onArrive` gets each batch of new tickets, e.g. to play a sound.
 */
export function useFreshTickets(
  tickets: KitchenTicket[] | undefined,
  onArrive: (added: KitchenTicket[]) => void,
): Set<string> {
  const seen = useRef<Set<string> | null>(null)
  const onArriveRef = useRef(onArrive)
  const [fresh, setFresh] = useState<Set<string>>(() => new Set())

  useEffect(() => {
    onArriveRef.current = onArrive
  })

  useEffect(() => {
    if (!tickets) return
    if (seen.current == null) {
      seen.current = new Set(tickets.map((t) => String(t.id)))
      return
    }
    const known = seen.current
    const added = tickets.filter((t) => !known.has(String(t.id)))
    if (added.length === 0) return
    added.forEach((t) => known.add(String(t.id)))
    setFresh((previous) => new Set([...previous, ...added.map((t) => String(t.id))]))
    onArriveRef.current(added)
  }, [tickets])

  return fresh
}
