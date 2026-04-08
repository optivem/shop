import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  timeout: 60000,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: 1,
  reporter: [['./channel-list-reporter.ts'], ['html', { open: 'never' }]],
  use: {
    trace: 'on-first-retry',
    timezoneId: 'UTC',
  },
  projects: [
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
