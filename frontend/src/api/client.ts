import createClient from 'openapi-fetch'
import type { paths } from './schema'

/** Typed client for our backend. `schema.d.ts` is generated (`npm run gen:api`); never write API types by hand. */
export const api = createClient<paths>({ baseUrl: '' })
