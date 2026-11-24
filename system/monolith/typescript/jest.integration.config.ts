import type { Config } from 'jest';

// Narrow-integration suite: real Postgres via Testcontainers (requires Docker).
// Scoped to *.integration.spec.ts so it never runs alongside the unit suite.
// No env-mutating globalSetup — the container is started and POSTGRES_DB_* env
// vars are set inside the spec's beforeAll, because env set in a Jest
// globalSetup does not reliably reach the test-worker process.
//
// forceExit: src/lib/db.ts builds its pg Pool at module load and exposes no
// close seam (left untouched on purpose), so the idle pool keeps the worker
// alive after the suite. Force-exit once the container is stopped.
const config: Config = {
  moduleFileExtensions: ['js', 'json', 'ts'],
  rootDir: 'src',
  testRegex: '.*\\.integration\\.spec\\.ts$',
  transform: {
    '^.+\\.ts$': 'ts-jest',
    // cockatiel 4 ships ESM only; transpile it to CommonJS for Jest (`.` matches either path separator).
    'node_modules.cockatiel.+\\.js$': [
      'ts-jest',
      { tsconfig: { allowJs: true, module: 'commonjs', moduleResolution: 'node10' } },
    ],
  },
  transformIgnorePatterns: ['/node_modules/(?!cockatiel/)'],
  testEnvironment: 'node',
  moduleNameMapper: {
    '^@/(.*)$': '<rootDir>/$1',
  },
  forceExit: true,
};

export default config;
