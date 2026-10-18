import {
  Button,
  Checkbox,
  Group,
  Modal,
  NumberInput,
  Radio,
  SegmentedControl,
  Stack,
  Text,
} from '@mantine/core'
import { useState } from 'react'
import type { CondimentOption, DangerFor, Member, MenuItem } from '../waiterApi'
import { VERDICT_COLOR, dangerText, money } from './marks'
import { AllergenChips } from './VerdictMarks'

export type LineDraft = {
  item: MenuItem
  condiments: CondimentOption[]
  forMember: Member | null
  quantity: number
}

const TABLE = 'table'

/** One dish: condiments, "for whom", quantity. Verdicts come from the backend; this only shows them. */
export function LineEditor({
  item,
  members,
  onAdd,
  onClose,
}: {
  item: MenuItem | null
  members: Member[] | null
  onAdd: (line: LineDraft) => void
  onClose: () => void
}) {
  return (
    <Modal opened={item != null} onClose={onClose} title={item?.name} size="lg">
      {item && <LineForm key={item.code} item={item} members={members} onAdd={onAdd} />}
    </Modal>
  )
}

function LineForm({
  item,
  members,
  onAdd,
}: {
  item: MenuItem
  members: Member[] | null
  onAdd: (line: LineDraft) => void
}) {
  const [forWhom, setForWhom] = useState(TABLE)
  const [quantity, setQuantity] = useState(1)
  const [chosen, setChosen] = useState<Record<string, string[]>>(() =>
    Object.fromEntries(item.condimentGroups.map((g) => [g.code, []])),
  )

  const options = item.condimentGroups.flatMap((g) => g.options)
  const selected = options.filter((o) => Object.values(chosen).some((codes) => codes.includes(o.code)))
  const complete = item.condimentGroups.every((g) => (chosen[g.code]?.length ?? 0) >= g.minSelect)
  const dangers = uniqueDangers([...item.dangerFor, ...selected.flatMap((o) => o.dangerFor)])
  const member = members?.find((m, i) => memberKey(m, i) === forWhom) ?? null

  return (
    <Stack>
      <Group justify="space-between">
        <AllergenChips allergens={item.allergens} />
        <Text>{money.format(item.price)}</Text>
      </Group>

      {members && members.length > 0 && (
        <Stack gap={4}>
          <Text fw={600}>For whom</Text>
          <SegmentedControl
            value={forWhom}
            onChange={setForWhom}
            data={[
              { value: TABLE, label: 'Table (sharing)' },
              ...members.map((m, i) => ({ value: memberKey(m, i), label: m.name })),
            ]}
            data-testid="waiter-line-for"
          />
        </Stack>
      )}

      {item.condimentGroups.map((g) => (
        <Stack key={g.code} gap={4}>
          <Text fw={600}>
            {g.name}
            {g.minSelect > 0 ? ' (required)' : ''}
          </Text>
          {g.maxSelect === 1 ? (
            <Radio.Group
              value={chosen[g.code]?.[0] ?? ''}
              onChange={(v) => setChosen({ ...chosen, [g.code]: [v] })}
            >
              <Stack gap={6}>
                {g.options.map((o) => (
                  <Radio
                    key={o.code}
                    value={o.code}
                    label={<OptionLabel option={o} />}
                    data-testid={`waiter-condiment-${o.code}`}
                  />
                ))}
              </Stack>
            </Radio.Group>
          ) : (
            <Checkbox.Group
              value={chosen[g.code] ?? []}
              onChange={(v) => setChosen({ ...chosen, [g.code]: v.slice(0, g.maxSelect || v.length) })}
            >
              <Stack gap={6}>
                {g.options.map((o) => (
                  <Checkbox
                    key={o.code}
                    value={o.code}
                    label={<OptionLabel option={o} />}
                    data-testid={`waiter-condiment-${o.code}`}
                  />
                ))}
              </Stack>
            </Checkbox.Group>
          )}
        </Stack>
      ))}

      {dangers.length > 0 && (
        <Text c="red" fw={600} data-testid="waiter-line-dangers">
          Contains: {dangerText(dangers)}
        </Text>
      )}

      <Group justify="space-between">
        <NumberInput
          label="Quantity"
          min={1}
          max={20}
          value={quantity}
          onChange={(v) => setQuantity(typeof v === 'number' && v > 0 ? v : 1)}
          w={120}
        />
        <Button
          size="lg"
          disabled={!complete}
          onClick={() => onAdd({ item, condiments: selected, forMember: member, quantity })}
          data-testid="waiter-line-add"
        >
          Add to order
        </Button>
      </Group>
    </Stack>
  )
}

function OptionLabel({ option }: { option: CondimentOption }) {
  return (
    <Group gap="xs">
      <Text
        c={
          option.verdict === 'SAFE' || option.verdict === 'UNKNOWN'
            ? undefined
            : VERDICT_COLOR[option.verdict]
        }
        fw={option.dangerFor.length ? 600 : 400}
      >
        {option.name}
        {option.price ? ` +${money.format(option.price)}` : ''}
      </Text>
      <AllergenChips allergens={option.allergens} size="xs" />
      {option.dangerFor.length > 0 && (
        <Text size="sm" c={VERDICT_COLOR[option.verdict]}>
          {dangerText(option.dangerFor)}
        </Text>
      )}
    </Group>
  )
}

function memberKey(member: Member, index: number): string {
  return member.id != null ? String(member.id) : `new-${index}`
}

function uniqueDangers(dangers: DangerFor[]): DangerFor[] {
  const seen = new Set<string>()
  return dangers.filter((d) => {
    const key = `${String(d.memberId)}-${d.allergen}`
    if (seen.has(key)) return false
    seen.add(key)
    return true
  })
}
