// Dates and times as staff read them: always in hotel time, whatever the device's time zone.

/** TYNGO Resort Phuket (PROJECT-CONTEXT.md §3). */
export const HOTEL_TIME_ZONE = 'Asia/Bangkok'

const time = new Intl.DateTimeFormat('en-GB', {
  timeZone: HOTEL_TIME_ZONE,
  hour: '2-digit',
  minute: '2-digit',
})
const dayTime = new Intl.DateTimeFormat('en-GB', {
  timeZone: HOTEL_TIME_ZONE,
  day: 'numeric',
  month: 'short',
  hour: '2-digit',
  minute: '2-digit',
})
const day = new Intl.DateTimeFormat('en-GB', { timeZone: 'UTC', day: 'numeric', month: 'short' })

/** "18:45" for an ISO timestamp. */
export function hotelTime(iso: string | null | undefined): string {
  return iso ? time.format(new Date(iso)) : ''
}

/** "18 Oct, 18:45" for an ISO timestamp. */
export function hotelDayTime(iso: string | null | undefined): string {
  return iso ? dayTime.format(new Date(iso)) : ''
}

/** "18 Oct" for a hotel-local date `2026-10-18`. */
export function hotelDate(date: string | null | undefined): string {
  return date ? day.format(new Date(`${date}T00:00:00Z`)) : ''
}
