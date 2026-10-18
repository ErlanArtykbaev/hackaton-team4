import { useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../../api/client'
import type { components } from '../../api/schema'
import { unwrap } from '../../shared/errors'
import { useStream } from '../../shared/useStream'

export type LogEntry = components['schemas']['LogEntry']
export type LogType = NonNullable<LogEntry['type']>
export type Summary = components['schemas']['SafetyLogSummary']

const POLL_MS = 10_000

/** Safety log, newest first; `query` is a room number or a last name, `type` narrows to one kind of record. */
export function useSafetyLog(query: string, type: LogType | null, live: boolean) {
  const q = query.trim()
  return useQuery({
    queryKey: ['safety-log', 'entries', q, type],
    queryFn: () =>
      unwrap(
        api.GET('/api/safety-log', {
          params: { query: { query: q || undefined, type: type ?? undefined } },
        }),
      ),
    placeholderData: (previous) => previous, // keep the list while the next search loads
    refetchInterval: live ? false : POLL_MS,
  })
}

export function useSummary(live: boolean) {
  return useQuery({
    queryKey: ['safety-log', 'summary'],
    queryFn: () => unwrap(api.GET('/api/safety-log/summary')),
    refetchInterval: live ? false : POLL_MS,
  })
}

/** Refreshes the log and the KPIs on every new record; returns whether the stream is live. */
export function useLogStream(): boolean {
  const client = useQueryClient()
  const refresh = () => void client.invalidateQueries({ queryKey: ['safety-log'] })
  return useStream<LogEntry>('log', refresh, refresh)
}
