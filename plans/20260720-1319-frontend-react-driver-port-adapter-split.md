# frontend-react — split test-kit drivers into `driver/port` + `driver/adapter`

**Status:** 🔵 Proposed (2026-07-20). Raised while explaining
`plans/20260720-0920-second-backend-stub-driver-in-process.md` — this is the structural
half of that discussion, extracted so it can land independently.

## TL;DR

**Why:** Every other test-kit in the repo (`system-test/java`, `system-test/typescript`, `backend-java` testkit) separates drivers into `driver/port/` and `driver/adapter/`. `frontend-react`'s `src/test/support/` is flat: the `BackendStubDriver` port lives in the same file as its Pact adapter, and the `FrontendDriver` port is mixed into the DSL. Worse, the `BackendStubDriver` port is typed in Pact's own vocabulary, so it is not actually driver-agnostic.
**End result:** `frontend-react` follows the same port/adapter layout as the other three test-kits, and `BackendStubDriver` is expressed in neutral terms — making the seam real rather than nominal.

## Problem

### 1. No port/adapter separation

The convention, followed in three places:

```
system-test/java/src/main/java/com/mycompany/myshop/testkit/driver/{port,adapter}/
system-test/typescript/src/testkit/driver/{port,adapter}/
system/multitier/backend-java/src/testSupport/java/.../testkit/driver/{port,adapter}/
```

e.g. `driver/port/external/tax/TaxDriver.java` → `driver/adapter/external/tax/TaxStubDriver.java`.

`frontend-react` has no such split — `src/test/support/` is flat:

```
backend-stub-dsl.ts
component-harness.ts
frontend-dsl.ts            <- contains the FrontendDriver PORT (line 61) + DSL + DTOs
gateway-frontend-driver.ts <- adapter
index.ts
pact-backend-stub-driver.ts <- contains the BackendStubDriver PORT (line 25) + Pact ADAPTER (line 32)
ui-frontend-driver.tsx     <- adapter
```

Two specific defects:

- **`BackendStubDriver` is named after and housed in its only adapter.** A port defined in
  `pact-backend-stub-driver.ts` reads as "the Pact driver's interface", not as the seam it is.
- **`FrontendDriver` lives in `frontend-dsl.ts`**, alongside the DSL and its gesture/expectation
  DTOs — while its two adapters (`ui-frontend-driver.tsx`, `gateway-frontend-driver.ts`) are
  correctly separate files. The port is the odd one out.

### 2. The `BackendStubDriver` port leaks Pact into its signature

This is the more serious finding, and it was not visible from the layout alone:

```ts
// pact-backend-stub-driver.ts
import type { V3Interaction } from '@pact-foundation/pact/src/v3/types';

export interface BackendStubDriver {
  stub(interaction: V3Interaction): void;   // <-- Pact's type, in the port
  backendUrl(): Promise<string>;
  finish(): Promise<void>;
  reset(): void;
}
```

The port's central method is typed in the vocabulary of one specific adapter, imported from a
**deep internal path** of the Pact package (`/src/v3/types`, not the package root). Any non-Pact
adapter would have to accept and interpret a Pact interaction object.

The file's own comment claims the opposite:

> A Component Test's in-process stub could implement the same surface without Pact.

As written, it could not — not without depending on Pact for its types.

## Goal

- `frontend-react` test-kit drivers laid out as `driver/port/` + `driver/adapter/`, matching the
  other three test-kits.
- `BackendStubDriver` expressed in neutral terms, so the comment above becomes true.
- No spec changes. This is structural; if a spec has to change, the move was wrong.

## Proposed work

1. **Create the directory structure** under `src/test/support/driver/`:
   - `port/backend-stub-driver.ts` — the `BackendStubDriver` interface, alone.
   - `port/frontend-driver.ts` — the `FrontendDriver` interface, extracted from `frontend-dsl.ts`.
   - `adapter/pact-backend-stub-driver.ts` — `PactBackendStubDriver` + `newProvider` + `UNREACHABLE_BACKEND`.
   - `adapter/ui-frontend-driver.tsx`, `adapter/gateway-frontend-driver.ts` — moved as-is.
2. **Decide where the gesture/expectation DTOs live.** `PlaceOrderGesture` and
   `OrderHistoryRowExpectation` are referenced by both the port and the DSL. Recommend
   `port/` alongside the interface that uses them, mirroring how the Java testkit keeps port
   types with the port.
3. **Neutralize `stub()`'s parameter type** — see open question below.
4. **Update `index.ts`** re-exports to the new paths. The public surface specs import should be
   unchanged.
5. **Verify:** `npx tsc --noEmit` in `frontend-react`, then the frontend component/contract specs.

## Open questions

- **What replaces `V3Interaction` in the port?** Options, in rough order of preference:
  1. **A neutral `BackendInteraction` type owned by the port**, which the Pact adapter maps to
     `V3Interaction`. Truest to port/adapter, and the only option that makes a non-Pact adapter
     possible. Costs a mapping layer and a look at how `backend-stub-dsl.ts` builds interactions
     today — it may currently construct Pact shapes directly, which would widen the change.
  2. **Keep `V3Interaction` but import it from the package root** rather than `/src/v3/types`.
     Removes the deep-internal-path fragility, leaves the leak.
  3. **Leave it.** Layout parity only; the seam stays nominal.

  Recommend (1), but it should be sized first — if `backend-stub-dsl.ts` is already emitting Pact
  shapes, (1) is a meaningfully larger change than the file moves, and splitting this plan again
  (layout now, neutral type later) is reasonable.
- **Does `system-test/typescript` have a naming convention for these files** worth matching
  exactly (e.g. `driver/port/my-shop-driver.ts`)? Should be checked before choosing filenames.

## Relationship to the second-driver plan

`plans/20260720-0920-second-backend-stub-driver-in-process.md` proposes a second, in-process
`BackendStubDriver` implementation. That plan's premise is that the seam is real but unproven.
Finding 2 above sharpens that: the seam is **not currently implementable** by a non-Pact adapter,
because the port speaks Pact.

Two consequences:

- This plan is a **prerequisite** for that one — a second driver cannot be written cleanly until
  `stub()` takes a neutral type.
- This plan stands on its own regardless of whether the second driver is ever built. Single-implementor
  ports are the norm in this repo (`ClockDriver`, `TaxDriver`, `ErpDriver`, `MyShopDriver` each have
  exactly one adapter), so implementation count is not the design statement — the port/adapter split is.
