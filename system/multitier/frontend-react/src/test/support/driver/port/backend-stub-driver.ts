// The seam the test-kit depends on: the Backend Stub DSL stages interactions with
// stub(); the harness owns the lifecycle (reset/backendUrl/finish).
//
// stub() takes Pact's own V3Interaction, and that is INTENTIONAL — not a known gap.
// The Pact vocabulary is load-bearing here: `states` describes a provider you must
// put into a state (an in-process stub has no provider), and matchers exist so a real
// provider can be verified loosely (an in-process stub only ever needs the concrete
// example). A "neutral" interaction type would carry a field one implementor ignores
// entirely and a matcher concept it flattens to the example — a fiction, not an
// abstraction.
//
// The driver-agnostic claim belongs one level up and is already true there: the
// Backend Stub DSL (support/backend-stub-dsl.ts) never names Pact, so
// `backend.returnsPlacedOrder().execute()` reads the same regardless. For a genuine
// two-driver swap, see componentHarness() vs integrationHarness().
//
// Declined 2026-07-20 with the reasoning and the avoided cost recorded in
// plans/deferred/20260720-0920-second-backend-stub-driver-in-process.md. If reopened,
// do it as a timeboxed spike on one interaction, not a full build.
import type { V3Interaction } from '@pact-foundation/pact';

export interface BackendStubDriver {
  stub(interaction: V3Interaction): void;
  backendUrl(): Promise<string>;
  finish(): Promise<void>;
  reset(): void;
}
