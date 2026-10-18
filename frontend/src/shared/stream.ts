// One EventSource on `/api/stream` for the whole browser tab, shared by every screen that listens.
// The browser reconnects by itself after a drop; screens refetch on `connected` to catch up on what they missed.

export const STREAM_EVENTS = ['ticket', 'log', 'passport'] as const
export type StreamEvent = (typeof STREAM_EVENTS)[number]

type Listener = (payload: unknown) => void
type StatusListener = (live: boolean) => void

const listeners = new Map<StreamEvent, Set<Listener>>(STREAM_EVENTS.map((e) => [e, new Set()]))
const statusListeners = new Set<StatusListener>()
let source: EventSource | null = null
let live = false
let users = 0

function setLive(next: boolean) {
  if (live === next) return
  live = next
  statusListeners.forEach((l) => l(next))
}

function open() {
  source = new EventSource('/api/stream')
  source.onopen = () => setLive(true)
  source.onerror = () => setLive(false)
  for (const event of STREAM_EVENTS) {
    source.addEventListener(event, (message) => {
      let payload: unknown
      try {
        payload = JSON.parse((message as MessageEvent<string>).data)
      } catch {
        return // a malformed event must not break the screen; the next refetch brings the data
      }
      listeners.get(event)?.forEach((l) => l(payload))
    })
  }
}

/** Starts using the stream; returns the function that stops. The connection closes with its last user. */
export function joinStream(): () => void {
  users += 1
  if (!source) open()
  return () => {
    users -= 1
    if (users > 0 || !source) return
    source.close()
    source = null
    setLive(false)
  }
}

export function onStreamEvent(event: StreamEvent, listener: Listener): () => void {
  listeners.get(event)?.add(listener)
  return () => listeners.get(event)?.delete(listener)
}

export function onStreamStatus(listener: StatusListener): () => void {
  statusListeners.add(listener)
  return () => statusListeners.delete(listener)
}

export function isStreamLive(): boolean {
  return live
}
