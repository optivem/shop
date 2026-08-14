# Test Taxonomy: Scope Layers + Contract Counterparties

This taxonomy has two axes, and each governs a different part of the suite list.

- **Scope** governs suites 1–3, shared by every component in the repo, frontend or
  backend. These layers differ by how much of the system a test runs, not by mocking
  technology.
- **Counterparty** governs the contract suites, 4 and 5. Once a test's job is "do the
  two sides still agree about the interface between them?", scope stops being the
  useful question — who the far side is, and whether they will run our verification,
  is what determines how the test must be built.

For the full commit-stage pyramid and CI wiring see
[docs/pipeline/commit-stage.md](../pipeline/commit-stage.md).

---

## The five suites

| # | Suite (`id`) | Frontend | Backend (Java / .NET / TS) |
|---|---|---|---|
| 1 | **Unit** (`unit`) | Real in-memory domain logic: mappers, validation, formatting. `1+1` is used only as a placeholder where a component has no pure logic. | Real in-memory domain logic: `Order` constructor validation, pricing / discount / tax calculations. `1+1` placeholder only. |
| 2 | **Narrow integration** (`integration`) | One adapter (`orderService`) against the Pact mock server. No React render. | One adapter, **inbound or outbound**. Outbound: `OrderRepository` against Testcontainers-Postgres, `TaxGateway`/`ErpGateway` against WireMock. Inbound: `OrderController` sliced via `@WebMvcTest` with `OrderService` faked. No app boot. |
| 3 | **Component** (`component`) | Full UI render against the Pact mock server. | Full service boot, hit the REST API. Postgres real (Testcontainers) + ERP/Tax mocked (WireMock-in-Testcontainers). |
| 4 | **Provider verification** (`provider-verification`) | **None** — the frontend is consumer-only. Contract emission lives in suites 2 + 3. | Counterparty = the frontend, an **internal** one we control. Verify the backend satisfies its `.pact` (`BackendPactVerificationTest`). |
| 5 | **External system contract** (`external-contract`) | **None** — the frontend talks to no external system directly. | Counterparty = an **external** system that will not run our verification (ERP, tax, clock). Agreement is pinned by stub-vs-real parity pairs and stub-consumability tests. Backend-java only so far. |

---

## The two discriminators

### Scope — which of suites 1–3 a non-contract test belongs to

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

### Counterparty — which of suites 4–5 a contract test belongs to

> **Will the far side run our verification?**
> - Yes, we control both ends → **internal**, suite 4 (`provider-verification`)
> - No, the far side is someone else's system → **external**, suite 5 (`external-contract`)

This is a deliberate exception to the scope axis: a suite-5 test may be a bare gateway
call with no Spring context (`ErpStubParityContractTest`) or a full SUT boot
(`ErpStubConsumabilityContractTest`), and both live in the same folder anyway. Scope is
what a contract test costs; counterparty is what it can prove, and for these tests the
latter is what has to drive placement — you cannot use Pact against a system that will
never replay your pact file.

---

## Contract binding mechanisms

"Contract" names three different things in the backend. They are distinguishable by
class name, and each is forced by the counterparty:

| Where | What it proves | Binding mechanism | Name marker |
|---|---|---|---|
| `contract/{latest,legacy}/internal/frontend/` | Pact provider verification — we are the provider to the frontend | Derivation: the consumer's stub *is* the pact | `*PactVerificationTest` |
| `contract/latest/external/{clock,erp,tax}/` | Stub consumability — does our own stub's JSON parse through the production gateway | Boot-and-read-back through the SUT | `*StubConsumabilityContractTest` |
| `contract/latest/external/{clock,erp,tax}/` | Stub-vs-real parity — does the gateway's parse agree with the real system | Twin tests sharing one set of assertions | `*StubParityContractTest` / `*RealParityContractTest` |

Pact is available only where we control both ends. For ERP, tax and clock we do not, so
parity is pinned by running the same assertions twice — once against our stub, once
against the real simulator — from a shared base class.

The `Stub` / `Real` marker is load-bearing, not decorative: `component-tests.yaml`'s
`external-contract` suite selects on it (`--tests '*StubParityContractTest' --tests
'*StubConsumabilityContractTest'`), which is what keeps the `Real` twins — they need a
live simulator — out of the commit stage.

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

## Backend: five-suite instantiation

The backend exposes all five suites; every one except `unit` requires Docker.
Suites 4 and 5 both run on the Gradle `contractTest` task and are separated by
package (`contract.*.internal.*` vs `contract.*.external.*`), not by task.

For the detailed backend pyramid description and CI wiring see
[docs/pipeline/commit-stage.md](../pipeline/commit-stage.md).

`ErpGateway`'s product read has a manual, opt-in `Real`-mode twin —
`contract/latest/external/erp/ErpRealParityContractTest`, run against the ERP simulator
(`external-systems/simulators`) instead of WireMock, alongside its `Stub` twin
(`ErpStubParityContractTest`, which *is* part of the default `external-contract` run).
It proves the stub is still an honest guess about the real thing, not just internally
consistent with the production gateway. Not wired into CI yet — see
`plans/20260812-1600-erp-real-contract-ci-wiring.md`.

Parity is scoped to products only: the simulator's promotion endpoint is hardcoded and
its error responses cannot be provoked on demand, so real-mode twins for those are not
buildable — see `plans/20260717-1015-component-stub-contract-beyond.md` item 4.

`external-contract` is currently **backend-java only**. `backend-dotnet` and
`backend-typescript` still keep their stub-consumability tests inside the `component`
suite; porting them is a follow-up, not a gap in the model.

### Known gap: the static stub mappings are unverified

`external-systems/stubs/mappings/erp-products-*.json` is bind-mounted into the WireMock
container by all twelve `docker-compose.*.stub.yml` files, so it is what every stub-mode
*deployment* serves — but no test loads it. All three ERP stub definitions (the static
mappings, `ErpStubDriver`'s runtime registration, and `ErpStubParityContractTest`'s
inline stub) currently agree — same `{id, price}` shape, same 404-on-unknown — but the
agreement is hand-maintained: renaming `price` in the JSON turns nothing red.

This is drift risk, not a live defect. Closing it would mean having the parity test
arrange through `ErpStubDriver` so one artifact serves both roles, the way the pact does
structurally on the frontend side.

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

The same motive produced suite 5's name. Once `external-contract` split off,
`provider-verification` could be narrowed to what it actually is — Pact against a
counterparty we control — instead of standing for "everything on the `contractTest`
task". Neither suite is called plain `contract`, because that word alone never
identified which of the three binding mechanisms above was in play.
