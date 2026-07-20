// The seam the test-kit depends on: the Backend Stub DSL stages interactions with
// stub(); the harness owns the lifecycle (reset/backendUrl/finish). A Component
// Test's in-process stub could implement the same surface without Pact.
//
// CAVEAT: not yet true as written. stub() still takes Pact's own V3Interaction, so
// any implementor depends on Pact for its types — the seam is structural, not
// vocabulary-level. Neutralizing it means re-expressing Pact's matcher policy
// (like/eachLike/integer/decimal, built in test/interactions/*.interactions.ts) in a
// neutral type the adapter maps back — a design change, not a move, and tracked in
// its own plan.
import type { V3Interaction } from '@pact-foundation/pact';

export interface BackendStubDriver {
  stub(interaction: V3Interaction): void;
  backendUrl(): Promise<string>;
  finish(): Promise<void>;
  reset(): void;
}
