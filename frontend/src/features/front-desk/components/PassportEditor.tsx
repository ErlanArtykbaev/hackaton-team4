import { Alert, Button, Group, Loader, Stack, Text, Title } from '@mantine/core'
import { useState } from 'react'
import { AllergenChips } from '../../../shared/AllergenMarks'
import { allergenLabel } from '../../../shared/allergens'
import { ErrorAlert } from '../../../shared/ErrorAlert'
import { hotelDayTime } from '../../../shared/format'
import { usePassport, useSavePassport, type Passport } from '../frontDeskApi'
import { emptyMember, problems, toDrafts, toUpdate, type MemberDraft } from '../memberDraft'
import { MemberCard } from './MemberCard'

/** Who updates the passport from this screen, as the safety log shows it. */
const FRONT_DESK = 'Front desk'

export function PassportEditor({ reservationId }: { reservationId: string }) {
  const passport = usePassport(reservationId)
  const save = useSavePassport()
  if (passport.isPending) return <Loader data-testid="front-desk-passport-loading" />
  if (passport.isError) return <ErrorAlert error={passport.error} onRetry={() => void passport.refetch()} />
  // A new form for every saved version: what the agent sees is always what the backend stored.
  return (
    <PassportForm
      key={`${reservationId}-${passport.data.updatedAt ?? ''}`}
      passport={passport.data}
      save={save}
    />
  )
}

function PassportForm({ passport, save }: { passport: Passport; save: ReturnType<typeof useSavePassport> }) {
  const [members, setMembers] = useState<MemberDraft[]>(() => toDrafts(passport))
  const [dirty, setDirty] = useState(false)
  const errors = problems(members)
  const canSave = dirty && Object.keys(errors).length === 0 && !save.isPending

  const edit = (next: MemberDraft[]) => {
    setMembers(next)
    setDirty(true)
    save.reset()
  }

  const submit = () =>
    save.mutate(
      { reservationId: passport.reservationId ?? '', body: toUpdate(members, FRONT_DESK) },
      { onSuccess: () => setDirty(false) },
    )

  return (
    <Stack data-testid="front-desk-passport">
      <div>
        <Title order={3}>
          Room {passport.roomNumber} · {passport.guestName}
        </Title>
        <Group gap={6} mt={4}>
          <Text size="sm" c="dimmed">
            PMS profile:
          </Text>
          {(passport.pmsAllergens?.length ?? 0) > 0 ? (
            <AllergenChips allergens={passport.pmsAllergens ?? []} size="xs" />
          ) : (
            <Text size="sm" c="dimmed">
              no allergies
            </Text>
          )}
        </Group>
        {passport.updatedBy && (
          <Text size="xs" c="dimmed" mt={4}>
            Last changed by {passport.updatedBy}, {hotelDayTime(passport.updatedAt)}
          </Text>
        )}
      </div>

      {members.map((m, i) => (
        <MemberCard
          key={m.key}
          member={m}
          index={i}
          problem={dirty ? errors[m.key] : undefined}
          onChange={(next) => edit(members.map((x) => (x.key === m.key ? next : x)))}
          onRemove={() => edit(members.filter((x) => x.key !== m.key))}
        />
      ))}

      <Group justify="space-between">
        <Button
          variant="light"
          onClick={() => edit([...members, emptyMember()])}
          data-testid="front-desk-member-add"
        >
          Add family member
        </Button>
        <Button onClick={submit} disabled={!canSave} loading={save.isPending} data-testid="front-desk-save">
          Save passport
        </Button>
      </Group>

      {save.isError && <ErrorAlert error={save.error} testId="front-desk-save-error" />}
      {save.isSuccess && (
        <Alert color="green" variant="light" data-testid="front-desk-saved">
          Passport saved. PMS updated
          {(save.data.pmsAllergens?.length ?? 0) > 0
            ? `: ${(save.data.pmsAllergens ?? []).map(allergenLabel).join(', ')}.`
            : '.'}
        </Alert>
      )}
    </Stack>
  )
}
