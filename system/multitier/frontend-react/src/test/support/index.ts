// Shared contract-test kit. Specs import ONE thing from here — the harness for
// their level — and get ready-wired `backend` / `frontend` DSL handles; the Pact
// scaffolding, the driver wiring and the mock-server lifecycle live behind it.
// The pieces below the harness are exported too, for the kit's own tests and for
// anyone assembling a non-standard level.
export { componentHarness, integrationHarness } from './component-harness';
export type { Harness } from './component-harness';
export { PactBackendStubDriver } from './driver/adapter/pact-backend-stub-driver';
export type { BackendStubDriver } from './driver/port/backend-stub-driver';
export { BackendStubDsl } from './backend-stub-dsl';
export { FrontendDsl } from './frontend-dsl';
export type { FrontendDriver } from './driver/port/frontend-driver';
export type { PlaceOrderGesture } from './driver/port/dtos/PlaceOrderGesture';
export { UiFrontendDriver } from './driver/adapter/ui-frontend-driver';
export { GatewayFrontendDriver } from './driver/adapter/gateway-frontend-driver';
