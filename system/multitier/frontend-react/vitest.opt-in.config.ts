import { defineConfig, configDefaults } from 'vitest/config';
import base from './vite.config';

// OPT-IN test config: runs the in-process component + Pact contract suites that
// the default `npm test` (vite.config.ts) deliberately excludes. It inherits
// every base setting (react plugin, jsdom, setup, timeouts, sequential file
// running) and only overrides which files are collected — so the two stay in
// sync. `npm run test:pact` runs every opt-in suite (the component and integration
// suites both drive the Pact mock server and write the contract); narrow to one
// suite with a CLI path arg, as `npm run test:component` does:
//   vitest run --config vitest.opt-in.config.ts src/test/latest/component
const baseTest = (base as { test?: Record<string, unknown> }).test ?? {};

export default defineConfig({
  ...base,
  test: {
    ...baseTest,
    include: [
      'src/test/legacy/**/*.{test,spec}.{ts,tsx}',
      'src/test/latest/**/*.{test,spec}.{ts,tsx}',
    ],
    // Drop the base's component/pact/integration exclusions; keep only the standard ignores.
    exclude: [...configDefaults.exclude],
  },
});
