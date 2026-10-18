import { ErrorAlert } from '../../../shared/ErrorAlert'
import { errorMessage } from '../../../shared/errors'
import { Alert, Button, Grid, Group, Loader, Paper, Stack, Text, Title } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMemo, useState } from 'react'
import { useDevice } from '../../../shared/deviceContext'
import { Cart, type CartLine } from '../components/Cart'
import { ConsentScreen } from '../components/ConsentScreen'
import { LineEditor, type LineDraft } from '../components/LineEditor'
import { MenuList } from '../components/MenuList'
import { PartyHeader } from '../components/PartyHeader'
import { PartySearch } from '../components/PartySearch'
import { PassportUpdate } from '../components/PassportUpdate'
import {
  useMenu,
  useOrderCheck,
  useOutlets,
  usePassport,
  useSendOrder,
  type MenuItem,
  type OrderBody,
  type ReservationSummary,
} from '../waiterApi'

type Party = ReservationSummary | 'walk-in'

/** Waiter tablet: find the party, order with verdicts per person, confirmations and guest consent, send to the POS. */
export function WaiterPage() {
  const { device } = useDevice()
  const [party, setParty] = useState<Party | null>(null)
  const [cart, setCart] = useState<CartLine[]>([])
  const [table, setTable] = useState<string | null>(null)
  const [picking, setPicking] = useState<MenuItem | null>(null)
  const [consentKey, setConsentKey] = useState<string | null>(null)
  const [editingPassport, setEditingPassport] = useState(false)

  const reservationId = party && party !== 'walk-in' ? party.reservationId : null
  const passport = usePassport(reservationId)
  const menu = useMenu(device.rvc, reservationId)
  const outlets = useOutlets()
  const tables = outlets.data?.find((o) => o.rvc === device.rvc)?.tables ?? []
  const send = useSendOrder()

  const checkBody = useMemo(
    () =>
      device.rvc != null && device.staffCode
        ? toBody(device.rvc, device.staffCode, reservationId, null, cart, false)
        : null,
    [device.rvc, device.staffCode, reservationId, cart],
  )
  const check = useOrderCheck(checkBody)
  const checks =
    check.data && !check.isPlaceholderData && check.data.lines.length === cart.length
      ? check.data.lines
      : undefined

  if (device.rvc == null || !device.staffCode) {
    return (
      <Alert color="yellow" title="Set up this tablet" data-testid="waiter-setup-needed">
        Choose the outlet and the waiter with the button at the top right.
      </Alert>
    )
  }
  const rvc = device.rvc
  const staffCode = device.staffCode

  const choose = (next: Party | null) => {
    setParty(next)
    setCart([])
    setTable(null)
  }
  const update = (key: string, change: Partial<CartLine>) =>
    setCart((lines) => lines.map((l) => (l.key === key ? { ...l, ...change } : l)))
  const add = (draft: LineDraft) => {
    setCart((lines) => [
      ...lines,
      { ...draft, key: crypto.randomUUID(), waiterConfirmed: false, consentBy: null },
    ])
    setPicking(null)
  }
  const submit = () =>
    send.mutate(toBody(rvc, staffCode, reservationId, table, cart, true), {
      onSuccess: (result) => {
        notifications.show({
          color: 'green',
          title: 'Sent to the kitchen',
          message: `Check ${result.checkNumber}`,
        })
        setCart([])
      },
      onError: (error) =>
        notifications.show({ color: 'red', title: 'Not sent', message: errorMessage(error) }),
    })

  const blocker = sendBlocker(cart, checks, check.isError, tables.length > 0 && table == null)
  const consentLine = cart.find((l) => l.key === consentKey) ?? null
  const members = party === 'walk-in' ? null : (passport.data?.members ?? null)

  if (party == null) {
    return (
      <Stack data-testid="waiter-page">
        <Title order={2}>{device.outletName} — new order</Title>
        <PartySearch onSelect={choose} onWalkIn={() => choose('walk-in')} />
      </Stack>
    )
  }

  return (
    <Grid data-testid="waiter-page">
      <Grid.Col span={{ base: 12, md: 8 }}>
        <Stack>
          {party === 'walk-in' ? (
            <Paper withBorder p="md">
              <Group justify="space-between">
                <Text fw={700} size="lg">
                  Walk-in guest — no allergy passport
                </Text>
                <Button variant="subtle" onClick={() => choose(null)} data-testid="waiter-party-change">
                  Change party
                </Button>
              </Group>
            </Paper>
          ) : passport.isLoading ? (
            <Loader />
          ) : passport.isError ? (
            <Paper withBorder p="md">
              <Group justify="space-between">
                <Text fw={700} size="lg">
                  Room {party.roomNumber} · {party.guestName}
                </Text>
                <Button variant="subtle" onClick={() => choose(null)} data-testid="waiter-party-change">
                  Change party
                </Button>
              </Group>
              <ErrorAlert
                error={passport.error}
                onRetry={() => void passport.refetch()}
                testId="waiter-passport-error"
              />
            </Paper>
          ) : (
            passport.data && (
              <PartyHeader
                passport={passport.data}
                onEditPassport={() => setEditingPassport(true)}
                onChangeParty={() => choose(null)}
              />
            )
          )}
          {menu.isLoading && <Loader />}
          {menu.isError && <Alert color="red">{errorMessage(menu.error)}</Alert>}
          {menu.data && <MenuList items={menu.data} walkIn={party === 'walk-in'} onPick={setPicking} />}
        </Stack>
      </Grid.Col>
      <Grid.Col span={{ base: 12, md: 4 }}>
        <div style={{ position: 'sticky', top: 72 }}>
          <Cart
            lines={cart}
            checks={checks}
            checkError={check.isError ? errorMessage(check.error) : null}
            tables={tables}
            table={table}
            onTable={setTable}
            onConfirm={(key, confirmed) => update(key, { waiterConfirmed: confirmed })}
            onConsent={setConsentKey}
            onRemove={(key) => setCart((lines) => lines.filter((l) => l.key !== key))}
            onSend={submit}
            sending={send.isPending}
            blocker={blocker}
          />
        </div>
      </Grid.Col>
      <LineEditor item={picking} members={members} onAdd={add} onClose={() => setPicking(null)} />
      <ConsentScreen
        line={consentLine}
        check={consentLine ? checks?.[cart.indexOf(consentLine)] : undefined}
        onGive={(givenBy) => {
          if (consentKey) update(consentKey, { consentBy: givenBy })
          setConsentKey(null)
        }}
        onClose={() => setConsentKey(null)}
      />
      {passport.data && (
        <PassportUpdate
          passport={passport.data}
          opened={editingPassport}
          staffCode={staffCode}
          rvc={rvc}
          onClose={() => setEditingPassport(false)}
        />
      )}
    </Grid>
  )
}

function toBody(
  rvc: number,
  waiterCode: string,
  reservationId: string | null,
  table: string | null,
  lines: CartLine[],
  withDecisions: boolean,
): OrderBody {
  return {
    rvc,
    reservationId: reservationId ?? undefined,
    table: table ?? undefined,
    waiterCode,
    lines: lines.map((l) => ({
      code: l.item.code,
      quantity: l.quantity,
      condimentCodes: l.condiments.map((c) => c.code),
      forMemberId: l.forMember?.id ?? undefined,
      waiterConfirmed: withDecisions && l.waiterConfirmed,
      consent: withDecisions && l.consentBy ? { givenBy: l.consentBy } : undefined,
    })),
  }
}

function sendBlocker(
  cart: CartLine[],
  checks: { requirement: string }[] | undefined,
  checkFailed: boolean,
  needsTable: boolean,
): string | null {
  if (cart.length === 0) return 'The order is empty.'
  if (checkFailed) return 'The allergen check failed. Change the order or try again.'
  if (!checks) return 'Checking allergens…'
  if (checks.some((c, i) => c.requirement === 'WAITER_CONFIRMATION' && !cart[i].waiterConfirmed))
    return 'Confirm the marked lines.'
  if (checks.some((c, i) => c.requirement === 'GUEST_CONSENT' && !cart[i].consentBy))
    return 'A marked line needs the guest’s consent.'
  if (needsTable) return 'Choose the table.'
  return null
}
