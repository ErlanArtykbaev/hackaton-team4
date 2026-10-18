import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

// Shapes of the frozen API contract (PLAN.md) until the generated types exist (FE-B1); then switch to `api/schema`.
export const ALLERGENS = [
  'GLUTEN',
  'CRUSTACEANS',
  'EGGS',
  'FISH',
  'PEANUTS',
  'SOYBEANS',
  'MILK',
  'NUTS',
  'CELERY',
  'MUSTARD',
  'SESAME',
  'SULPHITES',
  'LUPIN',
  'MOLLUSCS',
] as const
export type Allergen = (typeof ALLERGENS)[number]
export type Severity = 'INTOLERANCE' | 'SEVERE'
export type Verdict = 'SAFE' | 'WARNING' | 'DANGER' | 'UNKNOWN'
export type Requirement = 'NONE' | 'WAITER_CONFIRMATION' | 'GUEST_CONSENT'
export type MemberId = string | number

export type DangerFor = { memberId: MemberId; memberName: string; allergen: Allergen; severity: Severity }

export type ReservationSummary = {
  reservationId: string
  roomNumber: string
  guestName: string
  arrivalDate: string
  departureDate: string
  adults: number
  children: number
  language: string
  pmsAllergens: Allergen[]
  hasPassport: boolean
}

export type Allergy = { allergen: Allergen; severity: Severity }
export type Member = {
  id?: MemberId
  name: string
  age?: number | null
  primary: boolean
  allergies: Allergy[]
}
export type Passport = {
  reservationId: string
  roomNumber: string
  guestName: string
  members: Member[]
  updatedAt?: string
  updatedBy?: string
}

export type Outlet = { rvc: number; name: string; kind: string; tables: string[] }

export type CondimentOption = {
  code: string
  name: string
  price: number
  allergens: Allergen[]
  verdict: Verdict
  dangerFor: DangerFor[]
}
export type CondimentGroup = {
  code: string
  name: string
  minSelect: number
  maxSelect: number
  options: CondimentOption[]
}
export type MenuItem = {
  code: string
  name: string
  group: string
  price: number
  allergens: Allergen[]
  verdict: Verdict
  dangerFor: DangerFor[]
  safeAlternatives: string[]
  condimentGroups: CondimentGroup[]
}

export type OrderLine = {
  code: string
  quantity: number
  condimentCodes: string[]
  forMemberId?: MemberId
  waiterConfirmed: boolean
  consent?: { givenBy: string }
}
export type OrderBody = {
  rvc: number
  reservationId?: string
  table?: string
  waiterCode: string
  lines: OrderLine[]
}
export type LineCheck = { code: string; verdict: Verdict; dangerFor: DangerFor[]; requirement: Requirement }
export type OrderResult = { checkNumber: number; checkSeq: number; lines: LineCheck[] }

export class ApiError extends Error {
  readonly status: number
  readonly code: string | undefined

  constructor(status: number, code: string | undefined, message: string) {
    super(message)
    this.status = status
    this.code = code
  }
}

async function call<T>(path: string, method = 'GET', body?: unknown): Promise<T> {
  let response: Response
  try {
    response = await fetch(path, {
      method,
      headers: body === undefined ? undefined : { 'Content-Type': 'application/json' },
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  } catch {
    throw new ApiError(0, undefined, 'Our server is not reachable.')
  }
  if (!response.ok) {
    const problem = (await response.json().catch(() => null)) as {
      code?: string
      detail?: string
      title?: string
    } | null
    throw new ApiError(
      response.status,
      problem?.code,
      problem?.detail ?? problem?.title ?? `HTTP ${response.status}`,
    )
  }
  return (await response.json()) as T
}

const MESSAGES: Record<string, string> = {
  WAITER_CONFIRMATION_REQUIRED: 'A line still needs your confirmation.',
  GUEST_CONSENT_REQUIRED: 'A line still needs the guest’s consent.',
}

/** Text for the waiter; replaced by the shared messages by `code` once FE-B1 lands. */
export function errorText(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.code && MESSAGES[error.code]) return MESSAGES[error.code]
    if (error.status === 502 || error.status === 503 || error.status === 504)
      return 'The hotel system does not answer. Try again in a moment.'
    return error.message
  }
  return 'Something went wrong. Try again.'
}

export function sameMember(a: MemberId | undefined | null, b: MemberId | undefined | null): boolean {
  return a != null && b != null && String(a) === String(b)
}

export function useOutlets() {
  return useQuery({
    queryKey: ['outlets'],
    queryFn: () => call<Outlet[]>('/api/outlets'),
    staleTime: 300_000,
  })
}

/** Room number (exact) or last-name prefix of 2+ characters. */
export function isSearchable(query: string): boolean {
  const q = query.trim()
  return /^\d+$/.test(q) || q.length >= 2
}

export function useReservations(query: string) {
  const q = query.trim()
  return useQuery({
    queryKey: ['reservations', q],
    queryFn: () => call<ReservationSummary[]>(`/api/reservations?query=${encodeURIComponent(q)}`),
    enabled: isSearchable(q),
  })
}

export function usePassport(reservationId: string | null) {
  return useQuery({
    queryKey: ['passport', reservationId],
    queryFn: () => call<Passport>(`/api/passports/${encodeURIComponent(reservationId ?? '')}`),
    enabled: reservationId != null,
  })
}

export function useMenu(rvc: number | null, reservationId: string | null) {
  return useQuery({
    queryKey: ['menu', rvc, reservationId],
    queryFn: () =>
      call<MenuItem[]>(
        `/api/outlets/${rvc}/menu` +
          (reservationId ? `?reservationId=${encodeURIComponent(reservationId)}` : ''),
      ),
    enabled: rvc != null,
  })
}

/** What each line needs before sending; asked again whenever the cart or the passport changes. */
export function useOrderCheck(body: OrderBody | null) {
  return useQuery({
    queryKey: ['order-check', body],
    queryFn: () => call<{ lines: LineCheck[] }>('/api/orders/check', 'POST', body),
    enabled: body != null && body.lines.length > 0,
    placeholderData: (previous) => previous,
  })
}

export function useSavePassport() {
  const client = useQueryClient()
  return useMutation({
    mutationFn: (input: {
      reservationId: string
      members: Member[]
      updatedBy: string
      rvc: number | null
    }) =>
      call<Passport>(`/api/passports/${encodeURIComponent(input.reservationId)}`, 'PUT', {
        members: input.members,
        updatedBy: input.updatedBy,
        source: 'WAITER',
        rvc: input.rvc ?? undefined,
      }),
    onSuccess: (passport) => {
      client.setQueryData(['passport', passport.reservationId], passport)
      void client.invalidateQueries({ queryKey: ['menu'] })
      void client.invalidateQueries({ queryKey: ['order-check'] })
      void client.invalidateQueries({ queryKey: ['reservations'] })
    },
  })
}

export function useSendOrder() {
  return useMutation({ mutationFn: (body: OrderBody) => call<OrderResult>('/api/orders', 'POST', body) })
}
