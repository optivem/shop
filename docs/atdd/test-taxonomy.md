# Test Taxonomy: Symmetric 4-Layer Model

Every component in this repo — frontend or backend — shares the same four-layer
test taxonomy. The layers differ by **scope**, not by mocking technology.

For the full commit-stage pyramid and CI wiring see
[docs/pipeline/commit-stage.md](../pipeline/commit-stage.md).

---

## The four layers

| # | Layer (suite `id`) | Frontend | Backend (Java / .NET / TS) |
|---|---|---|---|
| 1 | **Unit** (`unit`) | Real in-memory domain logic: mappers, validation, formatting. `1+1` is used only as a placeholder where a component has no pure logic. | Real in-memory domain logic: `Order` constructor validation, pricing / discount / tax calculations. `1+1` placeholder only. |
| 2 | **Narrow integration** (`integration`) | One adapter (`orderService`) against the Pact mock server. No React render. | One adapter, **inbound or outbound**. Outbound: `OrderRepository` against Testcontainers-Postgres, `TaxGateway`/`ErpGateway` against WireMock. Inbound: `OrderController` sliced via `@WebMvcTest` with `OrderService` faked. No app boot. |
| 3 | **Component** (`component`) | Full UI render against the Pact mock server. | Full service boot, hit the REST API. Postgres real (Testcontainers) + ERP/Tax mocked (WireMock-in-Testcontainers). |
| 4 | **Provider verification** (`provider-verification`) | **None** — the frontend is consumer-only. Contract emission lives in suites 2 + 3. | Verify the backend satisfies the frontend consumer's `.pact` (`BackendPactVerificationTest`). |

---

## The one discriminator

> **Does the test boot or render the real component?**
> - Yes → **component** (layer 3)
> - Calls one adapter directly, no boot / render → **narrow integration** (layer 2)

The mocking stack — Pact mock server, WireMock, Testcontainers — is **orthogonal**
to this line. Both middle layers use mock servers; the mock server alone cannot tell
you which layer a test belongs to.

"One adapter" means either direction. An outbound adapter (`ErpGateway`) driven against
a faked external system and an inbound adapter (`OrderController`) driven with its
service faked are the same shape — one adapter, real framework wiring, collaborator
faked — and both are layer 2.

---

## Frontend: three-suite instantiation

The frontend has no provider-verification suite because it is consumer-only. Its
contract emission is distributed across layers 2 and 3 (see "Both suites emit" below).

### Suite 1 — `unit`

Pure in-memory logic: form validation, response mappers, price formatters. No network,
no render of pages that require a running adapter.

The sample test (`test harness`) in `src/test/harness.test.tsx` is a `1+1` placeholder
that proves the Vitest + RTL + jsdom harness is wired correctly; it stands in until the
frontend accumulates real mapper / validation logic worth isolating.

### Suite 2 — `integration` (narrow)

`OrderService` is called directly with no React render. The Pact mock server (in-process
FFI, no Docker) intercepts the HTTP request, replies with the configured response, and
writes the interaction into `contracts/frontend-backend.json`.

```
test → OrderService → fetch → Pact mock server (in-process)
                                      ↓ writes
                              contracts/frontend-backend.json
```

Shared interaction builders live in `src/test/interactions/` (see "Shared fixture"
below). Each test calls `provider.addInteraction(someInteraction())` then
`provider.executeTest(...)`.

### Suite 3 — `component`

The full React page renders and drives user events against the Pact mock server.
`routeApiTo(mockserver.url)` (from `test-utils.tsx`) rewrites relative `/api/*` fetch
calls to the mock server URL so production code is unmodified.

```
test → renderWithProviders(<Page />) → fetch /api/... → Pact mock server (in-process)
                                                                ↓ writes
                                                        contracts/frontend-backend.json
```

Both `integration` and `component` write to the **same** `contracts/` folder
(write-mode: merge). Together they produce one union contract.

---

## Both suites emit: the union contract

The committed `contracts/frontend-backend.json` is the **union** of the interactions
emitted by both `integration` and `component`. This is intentional:

- The narrow-integration suite can cover adapter interactions the UI never exercises
  directly (e.g. cancel-order, deliver-order flows). These interactions simply append to
  the contract; they do not appear in the component suite.
- The component suite covers the same happy-path requests to prove the real UI renders
  them correctly.
- Identical interactions (same `uponReceiving` description + provider state) **merge
  idempotently** — no duplication results.

**Operational rule:** both suites must run together when regenerating the contract.
Running only one suite writes a partial contract and drops the other suite's
interactions.

---

## Shared fixture (`src/test/interactions/`)

Interaction structure — URL, HTTP method, JSON request/response shapes, and Pact
matchers — lives in parameterised builder functions under `src/test/interactions/`.
This folder is test-only code, deliberately **not** named `contracts/` to avoid
colliding with the `.pact` output folder.

Each suite imports the same builder and supplies only the data point:

```typescript
// integration suite
provider.addInteraction(placeOrderInteraction());
await provider.executeTest(async (mockserver) => { /* call service directly */ });

// component suite
provider
  .given('product BOOK-123 exists and US is taxable')
  .uponReceiving('a place-order request for BOOK-123')
  ...
await provider.executeTest(async (mockserver) => { /* render <NewOrder /> */ });
```

One definition per `description` + provider state guarantees no duplication even when
both suites run the same interaction.

---

## Docker-free for both frontend middle suites

The Pact mock server is an in-process FFI server bundled with
`@pact-foundation/pact`. No Docker daemon is required to run the frontend `integration`
or `component` suites. Both are available on the `$0` / zero-infra path.

Backend middle suites (layers 2 and 3) do require Docker (Testcontainers-Postgres,
WireMock-in-Testcontainers) and are opt-in.

---

## Stub-only opt-out

A test that must deliberately *not* touch the contract can use the low-level
`pact-core` `createMockServer` / `cleanupMockServer` API without calling
`writePactFile`. This leaves the contract unchanged while still letting the test use
a real in-process HTTP mock.

This opt-out is **not the default**. The default is `PactV3.executeTest`, which always
emits the interaction.

---

## Which state belongs in which suite

| Test state | Suite |
|---|---|
| Validation short-circuit (empty form, bad input — no request fires) | `unit` |
| Pure formatting (price display, date formatting) | `unit` |
| Single-adapter request/response: place order, browse history | `integration` |
| Single-adapter request/response: cancel order, deliver order | `integration` |
| Full render happy-path: place order, browse history, view details | `component` |
| Full render contracted-error flows: 404 not found, 422 rejected | `component` |
| Loading spinner / network-down states (no real backend) | `component` (vi.fn() stub — not Pact) |

---

## Backend: four-suite instantiation

The backend exposes all four suites. The narrow-integration and component suites
require Docker. Provider verification (`provider-verification`) verifies the backend
against the frontend's committed `.pact`.

For the detailed backend pyramid description and CI wiring see
[docs/pipeline/commit-stage.md](../pipeline/commit-stage.md).

---

## Contract location

The `.pact` file is written to the repo-owned `contracts/` folder
(`shop/contracts/` in this template). The backend points `@PactFolder` at the same
path. Both tiers read and write `contracts/` during development without a Pact Broker.

A Pact Broker (or PactFlow) is the cost-labelled **opt-in** for multi-repo setups —
it is never the default. The zero-infra default is the committed `contracts/` file
checked into each repo.

---

## The old standalone `contract` suite

Before the narrow-integration layer was added, the frontend had a standalone `contract`
suite (`npm run test:pact`) that rendered the UI and emitted the Pact file. That
behaviour is now part of the `component` suite, and the `integration` suite adds
adapter-only emission. There is **no** standalone frontend layer-4 suite afterward.

The `contract` suite `id` in `component-tests.yaml` is being renamed to
`provider-verification` across all components (cross-cutting rename, tracked separately)
to name it by what it does — and to be clear that the **frontend has no
provider-verification suite**.
