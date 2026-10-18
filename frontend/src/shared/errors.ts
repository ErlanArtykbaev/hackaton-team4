// The one place that turns a failed call into a sentence for staff (FRONTEND.md §4).
// The backend answers errors as ProblemDetail with a stable `code` (BACKEND.md §3); we branch on it, never on `detail`.

/** A failed call to our backend. `status` 0 means no response at all. */
export class ApiError extends Error {
  readonly status: number
  readonly code: string | undefined

  constructor(status: number, code: string | undefined, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
  }
}

const NOT_REACHABLE = 'Our server is not reachable. Check that the backend is running, then try again.'
const HOTEL_DOWN = 'The hotel system is not responding. Try again in a moment.'
const UNEXPECTED = 'Something went wrong. Try again; if it happens again, tell the shift manager.'

const MESSAGES: Record<string, string> = {
  // Our backend
  VALIDATION_ERROR: 'Some details are missing or not valid. Check the form and try again.',
  UPSTREAM_UNAVAILABLE: HOTEL_DOWN,
  INTERNAL_ERROR: UNEXPECTED,
  WAITER_CONFIRMATION_REQUIRED: 'A line still needs your confirmation.',
  GUEST_CONSENT_REQUIRED: 'A line still needs the guest’s consent.',
  // Hotel rules passed through from the sandbox
  NOT_FOUND: 'Not found in the hotel system. Check the room number or the name.',
  GUEST_NOT_IN_HOUSE: 'The guest is not checked in. Ask the front desk.',
  OCCUPIED_TABLE: 'This table already has an open check in the POS. Choose another table.',
  CHECK_CLOSED: 'This check is already closed in the POS.',
  POSTING_BLOCKED: 'The hotel system does not accept charges for this guest.',
}

/** The sentence to show for any error thrown by a call. */
export function errorMessage(error: unknown): string {
  if (!(error instanceof ApiError)) return UNEXPECTED
  if (error.code && MESSAGES[error.code]) return MESSAGES[error.code]
  if (error.status === 0) return NOT_REACHABLE
  if (error.status >= 502 && error.status <= 504) return HOTEL_DOWN
  return UNEXPECTED
}

/** Reads the ProblemDetail of a failed response (plain `fetch`) into an ApiError. */
export async function problemOf(response: Response): Promise<ApiError> {
  const problem = (await response.json().catch(() => null)) as {
    code?: string
    detail?: string
    title?: string
  } | null
  return new ApiError(
    response.status,
    problem?.code,
    problem?.detail ?? problem?.title ?? `HTTP ${response.status}`,
  )
}

/**
 * Unwraps an `api` (openapi-fetch) call for TanStack Query: returns the data or throws an ApiError.
 * Usage: `queryFn: () => unwrap(api.GET('/api/outlets'))`.
 */
export async function unwrap<T>(
  call: Promise<{ data?: T; error?: unknown; response: Response }>,
): Promise<T> {
  let result: Awaited<typeof call>
  try {
    result = await call
  } catch {
    throw new ApiError(0, undefined, 'No response from our server')
  }
  if (!result.response.ok || result.data === undefined) {
    const problem = result.error as { code?: string; detail?: string } | undefined
    throw new ApiError(
      result.response.status,
      problem?.code,
      problem?.detail ?? `HTTP ${result.response.status}`,
    )
  }
  return result.data
}
