import { defineConfig, devices } from '@playwright/test'

// Run by hand against the running app (`npm run dev` + backend); not in CI (TESTING.md §5).
export default defineConfig({
  testDir: '.',
  timeout: 60_000,
  expect: { timeout: 8_000 }, // the sandbox answers in seconds, the kitchen feed in ~3 s
  retries: 0,
  workers: 1,
  reporter: 'list',
  use: {
    baseURL: 'http://localhost:5173',
    trace: 'retain-on-failure',
    video: 'retain-on-failure',
    viewport: { width: 1366, height: 850 },
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'], viewport: { width: 1366, height: 850 } } },
  ],
})
