// Shared contract-test helper module for latest.
//
// In Java/.NET this is an abstract base class that ties the scenario DSL
// to a fixed external-system mode. In TypeScript each spec file sets the
// mode via `process.env.EXTERNAL_SYSTEM_MODE` before importing fixtures,
// so this module exposes only the test-type alias used by the per-entity
// contract helpers (BaseClockContractTest, BaseErpContractTest, BaseTaxContractTest).

import type { TestType } from '@playwright/test';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export type ContractTest = TestType<any, any>;
