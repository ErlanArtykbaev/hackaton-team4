import { ALLERGEN_CODES, ALLERGEN_META } from '../../../shared/allergens'
import { errorMessage } from '../../../shared/errors'
import {
  Alert,
  Button,
  Group,
  Modal,
  NumberInput,
  SegmentedControl,
  Select,
  Stack,
  Text,
  TextInput,
} from '@mantine/core'
import { useState } from 'react'
import { useSavePassport, type Allergen, type Member, type Passport, type Severity } from '../waiterApi'

const SEVERITIES = [
  { value: 'INTOLERANCE', label: 'Intolerance' },
  { value: 'SEVERE', label: 'Severe (life-threatening)' },
]

/** Quick passport update after asking the guest: add an allergen to a member, or add a member. */
export function PassportUpdate({
  passport,
  opened,
  staffCode,
  rvc,
  onClose,
}: {
  passport: Passport
  opened: boolean
  staffCode: string
  rvc: number | null
  onClose: () => void
}) {
  return (
    <Modal opened={opened} onClose={onClose} title="Update the allergy passport" size="lg">
      {opened && <PassportForm passport={passport} staffCode={staffCode} rvc={rvc} onClose={onClose} />}
    </Modal>
  )
}

function PassportForm({
  passport,
  staffCode,
  rvc,
  onClose,
}: {
  passport: Passport
  staffCode: string
  rvc: number | null
  onClose: () => void
}) {
  const [mode, setMode] = useState<'allergen' | 'member'>('allergen')
  const [memberIndex, setMemberIndex] = useState<string | null>(null)
  const [name, setName] = useState('')
  const [age, setAge] = useState<number | null>(null)
  const [allergen, setAllergen] = useState<string | null>(null)
  const [severity, setSeverity] = useState<Severity>('INTOLERANCE')
  const save = useSavePassport()

  const ready = mode === 'allergen' ? memberIndex != null && allergen != null : name.trim().length >= 2

  const submit = () => {
    const allergies = allergen ? [{ allergen: allergen as Allergen, severity }] : []
    const members: Member[] =
      mode === 'allergen'
        ? passport.members.map((m, i) =>
            String(i) === memberIndex
              ? { ...m, allergies: [...m.allergies.filter((a) => a.allergen !== allergen), ...allergies] }
              : m,
          )
        : [...passport.members, { name: name.trim(), age, primary: false, allergies }]
    save.mutate(
      { reservationId: passport.reservationId, members, updatedBy: staffCode, rvc },
      { onSuccess: onClose },
    )
  }

  return (
    <Stack>
      <Text c="dimmed">
        Ask the guest first. The change is saved with your name and sent to the hotel system.
      </Text>
      <SegmentedControl
        value={mode}
        onChange={(v) => setMode(v as 'allergen' | 'member')}
        data={[
          { value: 'allergen', label: 'Add an allergen' },
          { value: 'member', label: 'Add a person' },
        ]}
        data-testid="waiter-passport-mode"
      />
      {mode === 'allergen' ? (
        <Select
          label="Who"
          data={passport.members.map((m, i) => ({ value: String(i), label: m.name }))}
          value={memberIndex}
          onChange={setMemberIndex}
          data-testid="waiter-passport-member"
        />
      ) : (
        <Group grow>
          <TextInput
            label="Name"
            value={name}
            onChange={(e) => setName(e.currentTarget.value)}
            data-testid="waiter-passport-name"
          />
          <NumberInput
            label="Age (children)"
            min={0}
            max={120}
            value={age ?? ''}
            onChange={(v) => setAge(typeof v === 'number' ? v : null)}
            data-testid="waiter-passport-age"
          />
        </Group>
      )}
      <Select
        label={mode === 'allergen' ? 'Allergen' : 'Allergen (optional)'}
        data={ALLERGEN_CODES.map((c) => ({
          value: c,
          label: `${ALLERGEN_META[c].icon} ${ALLERGEN_META[c].label}`,
        }))}
        value={allergen}
        onChange={setAllergen}
        searchable
        clearable={mode === 'member'}
        data-testid="waiter-passport-allergen"
      />
      <SegmentedControl
        value={severity}
        onChange={(v) => setSeverity(v as Severity)}
        data={SEVERITIES}
        data-testid="waiter-passport-severity"
      />
      {save.isError && <Alert color="red">{errorMessage(save.error)}</Alert>}
      <Button
        size="lg"
        disabled={!ready}
        loading={save.isPending}
        onClick={submit}
        data-testid="waiter-passport-save"
      >
        Save to the passport
      </Button>
    </Stack>
  )
}
