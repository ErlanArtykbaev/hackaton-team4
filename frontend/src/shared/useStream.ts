import { useEffect, useRef, useSyncExternalStore } from 'react'
import { isStreamLive, joinStream, onStreamEvent, onStreamStatus, type StreamEvent } from './stream'

/**
 * Listens to one event of `/api/stream` (`ticket`, `log` or `passport`) and returns whether the stream is live.
 * `onConnect` runs on every (re)connection: refetch there to catch up on events missed while disconnected.
 * While the stream is down the screen should poll (`refetchInterval: live ? false : 10_000`).
 */
export function useStream<T>(
  event: StreamEvent,
  onEvent: (payload: T) => void,
  onConnect?: () => void,
): boolean {
  const onEventRef = useRef(onEvent)
  const onConnectRef = useRef(onConnect)
  useEffect(() => {
    onEventRef.current = onEvent
    onConnectRef.current = onConnect
  })

  useEffect(() => {
    const leave = joinStream()
    const stopEvents = onStreamEvent(event, (payload) => onEventRef.current(payload as T))
    const stopStatus = onStreamStatus((live) => {
      if (live) onConnectRef.current?.()
    })
    return () => {
      stopStatus()
      stopEvents()
      leave()
    }
  }, [event])

  return useSyncExternalStore(onStreamStatus, isStreamLive)
}
