// Shared contract-test helper module for mod11.
//
// In Java/.NET this is an abstract base class that ties the scenario DSL
// to a fixed external-system mode. In TypeScript each spec file pins the
// mode with `test.use({ externalSystemMode: 'stub' | 'real' })`, so this module exposes only the test-type alias used by the per-entity
// contract helpers (BaseClockContractTest, BaseErpContractTest).

import type { withApp } from '../../../../../src/testkit/driver/adapter/shared/client/playwright/withApp.js';

export type ContractTest = ReturnType<typeof withApp>;
