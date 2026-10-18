import {
  ActionIcon,
  Badge,
  Chip,
  Group,
  NumberInput,
  Paper,
  SegmentedControl,
  Stack,
  Text,
  TextInput,
} from '@mantine/core'
import { ALLERGEN_CODES, ALLERGEN_META, SEVERITY_MARK, type SeverityCode } from '../../../shared/allergens'
import { setSeverity, toggleAllergen, type MemberDraft } from '../memberDraft'

/** The card of one family member: name, age, allergens and the severity of each. */
export function MemberCard({
  member,
  index,
  problem,
  onChange,
  onRemove,
}: {
  member: MemberDraft
  index: number
  problem?: string
  onChange: (next: MemberDraft) => void
  onRemove?: () => void
}) {
  const testId = `front-desk-member-${index}`
  const severe = member.allergies.some((a) => a.severity === 'SEVERE')
  return (
    <Paper
      withBorder
      p="md"
      style={severe ? { borderColor: 'var(--mantine-color-red-6)', borderWidth: 2 } : undefined}
      data-testid={testId}
    >
      <Stack gap="sm">
        <Group align="flex-end" wrap="nowrap">
          <TextInput
            label="Name"
            placeholder="First name"
            value={member.name}
            onChange={(e) => onChange({ ...member, name: e.currentTarget.value })}
            error={problem}
            style={{ flex: 1 }}
            data-testid={`${testId}-name`}
          />
          <NumberInput
            label="Age"
            placeholder="Adult"
            min={0}
            max={120}
            allowDecimal={false}
            allowNegative={false}
            value={member.age ?? ''}
            onChange={(v) => onChange({ ...member, age: typeof v === 'number' ? v : null })}
            w={90}
            data-testid={`${testId}-age`}
          />
          {member.primary ? (
            <Badge variant="light" mb={8}>
              Booker
            </Badge>
          ) : (
            onRemove && (
              <ActionIcon
                variant="subtle"
                color="gray"
                mb={4}
                onClick={onRemove}
                aria-label={`Remove ${member.name || 'member'}`}
                data-testid={`${testId}-remove`}
              >
                ✕
              </ActionIcon>
            )
          )}
        </Group>

        <div>
          <Text size="sm" fw={500} mb={4}>
            Allergens
          </Text>
          <Group gap={6}>
            {ALLERGEN_CODES.map((code) => (
              <Chip
                key={code}
                size="xs"
                color="red"
                checked={member.allergies.some((a) => a.allergen === code)}
                onChange={() => onChange(toggleAllergen(member, code))}
                data-testid={`${testId}-allergen-${code}`}
              >
                {ALLERGEN_META[code].icon} {ALLERGEN_META[code].label}
              </Chip>
            ))}
          </Group>
        </div>

        {member.allergies.length === 0 ? (
          <Text size="sm" c="dimmed">
            No allergies recorded.
          </Text>
        ) : (
          <Stack gap={6}>
            {member.allergies.map((a) => (
              <Group key={a.allergen} justify="space-between" wrap="nowrap">
                <Text size="sm">
                  {ALLERGEN_META[a.allergen].icon} {ALLERGEN_META[a.allergen].label}
                </Text>
                <SegmentedControl
                  size="xs"
                  value={a.severity}
                  color={SEVERITY_MARK[a.severity].color}
                  onChange={(v) => onChange(setSeverity(member, a.allergen, v as SeverityCode))}
                  data={[
                    { value: 'INTOLERANCE', label: 'Intolerance' },
                    { value: 'SEVERE', label: 'Severe' },
                  ]}
                  data-testid={`${testId}-severity-${a.allergen}`}
                />
              </Group>
            ))}
          </Stack>
        )}
      </Stack>
    </Paper>
  )
}
