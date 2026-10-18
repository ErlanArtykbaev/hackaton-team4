import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../../api/client'
import type { components } from '../../api/schema'
import { unwrap } from '../../shared/errors'

export type ReservationSummary = components['schemas']['ReservationSummary']
export type Passport = components['schemas']['Passport']
export type PassportUpdate = components['schemas']['PassportUpdateRequest']

/** Today's arrivals and in-house guests with allergies or children, those without a passport first. */
export function useArrivals() {
  return useQuery({
    queryKey: ['front-desk', 'arrivals'],
    queryFn: () => unwrap(api.GET('/api/front-desk/arrivals')),
  })
}

/** Room number (exact) or last-name prefix of 2+ characters. */
export function isSearchable(query: string): boolean {
  const q = query.trim()
  return /^\d+$/.test(q) || q.length >= 2
}

export function useReservationSearch(query: string) {
  const q = query.trim()
  return useQuery({
    queryKey: ['reservations', q],
    queryFn: () => unwrap(api.GET('/api/reservations', { params: { query: { query: q } } })),
    enabled: isSearchable(q),
  })
}

export function usePassport(reservationId: string | null) {
  return useQuery({
    queryKey: ['passport', reservationId],
    queryFn: () =>
      unwrap(
        api.GET('/api/passports/{reservationId}', {
          params: { path: { reservationId: reservationId ?? '' } },
        }),
      ),
    enabled: reservationId != null,
  })
}

export function useSavePassport() {
  const client = useQueryClient()
  return useMutation({
    mutationFn: ({ reservationId, body }: { reservationId: string; body: PassportUpdate }) =>
      unwrap(api.PUT('/api/passports/{reservationId}', { params: { path: { reservationId } }, body })),
    onSuccess: (passport) => {
      client.setQueryData(['passport', passport.reservationId], passport)
      void client.invalidateQueries({ queryKey: ['front-desk', 'arrivals'] })
      void client.invalidateQueries({ queryKey: ['reservations'] })
      void client.invalidateQueries({ queryKey: ['menu'] }) // verdicts on the waiter tablet change with the passport
    },
  })
}
