import { defineConfig, configDefaults } from 'vitest/config';
import base from './vite.config';

// OPT-IN test config: runs the in-process component + Pact contract suites that
// the default `npm test` (vite.config.ts) deliberately excludes. It inherits
// every base setting (react plugin, jsdom, setup, timeouts, sequential file
// running) and only overrides which files are collected — so the two stay in
// sync. Narrow to a single suite with a CLI path arg, e.g.
//   vitest run --config vitest.opt-in.config.ts src/test/pact
// which is exactly what `npm run test:component` / `test:pact` do.
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
