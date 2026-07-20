// Component-test harness — the TS twin of backend-java's AbstractComponentTest.
//
// It owns everything a spec shouldn't see: the stubbed backend's lifecycle (boot on
// first gesture, verify-and-tear-down at teardown), the driver wiring, and the
// per-test reset. A spec calls this once at the top of the file and gets two
// ready-wired handles — `backend` to arrange what the backend returns, `frontend`
// to act and assert — so the test body is pure DSL.
//
// componentHarness() drives the rendered UI; integrationHarness() drives the gateway
// directly. That choice of driver is the ONLY difference between the two levels —
// the same DSL lines run over both.
import { beforeEach, afterEach, vi } from 'vitest';
import { PactBackendStubDriver } from './driver/adapter/pact-backend-stub-driver';
import { BackendStubDsl } from './backend-stub-dsl';
import { FrontendDsl } from './frontend-dsl';
import type { FrontendDriver } from './driver/port/frontend-driver';
import { UiFrontendDriver } from './driver/adapter/ui-frontend-driver';
import { GatewayFrontendDriver } from './driver/adapter/gateway-frontend-driver';

export interface Harness {
  backend: BackendStubDsl;
  frontend: FrontendDsl;
}

export function componentHarness(): Harness {
  return harness(() => new UiFrontendDriver());
}

export function integrationHarness(): Harness {
  return harness(() => new GatewayFrontendDriver());
}

function harness(newFrontendDriver: () => FrontendDriver): Harness {
  const backendStub = new PactBackendStubDriver();
  const backend = new BackendStubDsl(backendStub);
  const frontend = new FrontendDsl(newFrontendDriver, () => backendStub.backendUrl());

  beforeEach(() => {
    backendStub.reset();
    frontend.reset();
  });

  // finish() releases the held-open Pact callback and rethrows any unmatched-interaction
  // failure, so a contract violation fails the test that caused it.
  afterEach(async () => {
    try {
      await backendStub.finish();
    } finally {
      vi.unstubAllGlobals();
    }
  });

  return { backend, frontend };
}
