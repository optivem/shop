import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  timeout: 60000,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  fullyParallel: true,
  workers: process.env.CI ? 2 : 4,
  // junit: per-suite file (tests.yaml sets PLAYWRIGHT_JUNIT_OUTPUT_NAME) in its own
  // folder, so one run's suites don't overwrite each other and Playwright's
  // test-results/ cleanup doesn't wipe them. Each `tag` annotation (see
  // tests/commons/criteria.ts) becomes a <property name="tag"> for the spec gate.
  reporter: [
    ['./channel-list-reporter.ts'],
    ['html', { open: 'never' }],
    ['json'],
    ['junit', { outputDir: 'junit-results', fileName: 'results.xml' }],
  ],
  use: {
    trace: 'on-first-retry',
    timezoneId: 'UTC',
  },
  projects: [
    {
      name: 'architecture-test',
      testDir: './tests',
      testMatch: '**/architecture/**/*.spec.ts',
    },
    {
      name: 'acceptance-test',
      testDir: './tests',
      testMatch: '**/acceptance/**/*.spec.ts',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'smoke-test',
      testDir: './tests',
      testMatch: '**/smoke/**/*.spec.ts',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'e2e-test',
      testDir: './tests',
      testMatch: '**/e2e/**/*.spec.ts',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'external-system-contract-test',
      testDir: './tests',
      testMatch: '**/contract/**/*.spec.ts',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
});
