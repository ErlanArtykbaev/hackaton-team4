import { Badge, Chip, Group, Loader, Stack, TextInput, Title } from '@mantine/core'
import { useDebouncedValue } from '@mantine/hooks'
import { useState } from 'react'
import { EmptyState } from '../../../shared/EmptyState'
import { ErrorAlert } from '../../../shared/ErrorAlert'
import { LogEntryCard } from '../components/LogEntryCard'
import { SummaryTiles } from '../components/SummaryTiles'
import { LOG_MARK, LOG_TYPES } from '../logMarks'
import { useLogStream, useSafetyLog, useSummary, type LogType } from '../managerApi'

const ALL = 'ALL'

/** Manager: the safety log, searchable by room or last name, to prove what was ordered with whose consent. */
export function ManagerPage() {
  const [query, setQuery] = useState('')
  const [debounced] = useDebouncedValue(query, 300)
  const [type, setType] = useState<string>(ALL)
  const live = useLogStream()
  const log = useSafetyLog(debounced, type === ALL ? null : (type as LogType), live)
  const summary = useSummary(live)
  const filtered = debounced.trim() !== '' || type !== ALL

  return (
    <Stack data-testid="manager-page">
      <Group justify="space-between">
        <Title order={2}>Safety log</Title>
        <Badge
          color={live ? 'green' : 'gray'}
          variant="dot"
          tt="none"
          data-testid="manager-live"
          data-live={live}
        >
          {live ? 'Live' : 'Updating every 10 s'}
        </Badge>
      </Group>

      {summary.isError ? (
        <ErrorAlert
          error={summary.error}
          onRetry={() => void summary.refetch()}
          testId="manager-kpis-error"
        />
      ) : (
        <SummaryTiles summary={summary.data} />
      )}

      <TextInput
        placeholder="Room number or last name"
        value={query}
        onChange={(e) => setQuery(e.currentTarget.value)}
        rightSection={log.isFetching ? <Loader size="xs" /> : null}
        data-testid="manager-search"
      />
      <Chip.Group value={type} onChange={(v) => setType(v as string)}>
        <Group gap="xs">
          <Chip value={ALL} size="sm" data-testid="manager-filter-ALL">
            All records
          </Chip>
          {LOG_TYPES.map((t) => (
            <Chip key={t} value={t} size="sm" color={LOG_MARK[t].color} data-testid={`manager-filter-${t}`}>
              {LOG_MARK[t].label}
            </Chip>
          ))}
        </Group>
      </Chip.Group>

      {log.isPending && <Loader size="sm" data-testid="manager-log-loading" />}
      {log.isError && <ErrorAlert error={log.error} onRetry={() => void log.refetch()} />}
      {log.data?.length === 0 && (
        <EmptyState
          icon={filtered ? '🔍' : '📋'}
          title={filtered ? 'No records for this search.' : 'No records yet in this shift.'}
          hint={
            filtered
              ? 'Check the room number or last name, or clear the filters.'
              : 'Orders, confirmations, consents and passport changes appear here.'
          }
          testId="manager-log-empty"
        />
      )}
      <Stack gap="xs" data-testid="manager-log">
        {log.data?.map((entry) => (
          <LogEntryCard key={entry.id} entry={entry} />
        ))}
      </Stack>
    </Stack>
  )
}
