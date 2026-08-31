import { defineConfig, devices } from '@playwright/test'

/**
 * End-to-end tests run against the real stack: Postgres, the Quarkus API and the built
 * interface. The language model is switched off for these (MUSIC_LLM_ENABLED=false) so
 * the assertions are about the tutor's behaviour rather than a model's wording — which is
 * exactly the boundary this application is built around.
 *
 * `make test-e2e` starts the database and the backend first; this config starts the
 * frontend itself.
 */
export default defineConfig({
  testDir: './e2e',
  timeout: 30_000,
  expect: { timeout: 10_000 },
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [['list']],
  use: {
    baseURL: 'http://localhost:5173',
    trace: 'retain-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: {
    command: 'pnpm dev',
    url: 'http://localhost:5173',
    reuseExistingServer: true,
    timeout: 60_000,
  },
})
