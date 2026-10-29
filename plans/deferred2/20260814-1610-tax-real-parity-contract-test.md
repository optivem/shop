# Add TaxRealParityContractTest (and decide clock explicitly)

**Status:** Open — **not yet approved to build. Discuss before executing.**

## Context

`contract/external/` pins stub fidelity two ways (see `docs/atdd/test-taxonomy.md`):

| External | Consumability | Stub-vs-real parity |
|---|---|---|
| ERP | `ErpStubConsumabilityContractTest` | `ErpStubParityContractTest` + `ErpRealParityContractTest` |
| Tax | `TaxStubConsumabilityContractTest` | **missing** |
| Clock | `ClockStubConsumabilityContractTest` | **missing** |

Consumability only asks whether our own stub's JSON parses through our own gateway — both halves are
ours, so it cannot notice the pair drifting away from the real system together. Only a parity pair
catches that. So `TaxStubDriver`'s JSON is currently held against nothing external.

This surfaced while deciding that `contract/external/` gets no `latest/legacy` twin. The reasoning
there — the DSL is a choke point, so pinning it once covers every test that arranges through it —
is exactly what makes the tax gap worth closing: `TaxStubDriver` is a single artifact, and one
parity pair would cover every test that stubs tax.

## Tax: buildable, and the infra now exists

`external-systems/simulators/mock-server.js` mounts a full json-server router at `/tax/api`
(`server.use('/tax/api', taxRouter)`), seeded with US, GB, DE, FR, JP. json-server routers accept
`POST` / `PUT`, so a country can be provisioned on demand — the same fixture shape
`SimulatorErpProductClient` already uses for products.

The bring-up problem is already solved by the ERP work (`20260812-1600`, now implemented): the
`erpSimulatorImage` Gradle task builds the image, `ErpRealParityContractTest` starts it via
Testcontainers on a random port, and `component-tests.yaml` has an `external-contract-real` suite.
**The same image serves ERP, tax and clock** — `mock-server.js` is one process — so tax needs no new
container, only a share of the existing one.

### Work

1. `SimulatorTaxCountryClient` in `testkit/driver/adapter/external/tax/client/`, mirroring
   `SimulatorErpProductClient` (POST, fall back to PUT on duplicate id). Provision a test-only
   country code rather than mutating seeded `US`, so the test does not depend on seed values and
   cannot corrupt them for other consumers.
2. `BaseTaxCountryParityContractTest` with the two scenarios `TaxGateway` actually distinguishes:
   a known country returns the rate, an unknown one returns empty (the 404 branch). Mirror
   `BaseErpProductParityContractTest`.
3. `TaxStubParityContractTest` (WireMock) and `TaxRealParityContractTest` (simulator) as the twins.
4. **Share the container.** The simulator container is currently a private static singleton inside
   `ErpRealParityContractTest`. Two `*RealParityContractTest` classes must not start two copies —
   extract it to a shared holder in `contract/external/` before adding the second one.
5. **Rename the ERP-specific infra** once it is serving more than ERP: the `erpSimulatorImage` task
   and the `myshop/erp-simulator:contract-test` tag both name only one of the three systems the
   image actually serves. `externalSimulatorImage` / `myshop/external-simulator:contract-test`.
   Cosmetic, but the misnomer will mislead the moment tax lands.

`external-contract-real` already selects `--tests '*RealParityContractTest'`, so the new class joins
the suite with no config change.

## Clock: not buildable — recommend documenting instead

`ClockGateway.getCurrentTime()` branches on `external.system-mode`: in `real` mode it returns
`Instant.now()` and **never issues the HTTP call**. The compose stacks that set
`EXTERNAL_SYSTEM_MODE=real` therefore never consume the simulator's `/clock/api/time`, which in any
case returns a hardcoded `2024-01-15T10:30:00.000Z`.

So the clock's wire format has no real counterparty to be in parity with. `{"time": …}` is entirely
our own invention, consumed only by our own stub path. A `ClockRealParityContractTest` would pin our
stub against a simulator endpoint that production never reads — verification theatre.

**Recommendation:** leave clock at consumability only, and say so in `test-taxonomy.md` so the
asymmetry reads as a decision rather than an oversight. The interesting observation to record is
that clock's stub-vs-real risk is displaced, not absent: it lives in the `stub`/`real` mode branch
itself, which `ClockGatewayIntegrationTest` already pins (`getCurrentTimeIgnoresStubInRealMode`,
`getCurrentTimeRejectsUnknownMode`).

## Open questions

1. **Does tax parity justify its cost in the commit stage?** ERP's case rests on prices flowing into
   order totals. Tax rates do too, so the argument likely transfers — but the marginal value is one
   more field (`taxRate`) on an already-running container, so this is cheap either way. Confirm
   rather than assume.
2. **Item 5 renaming — now or when tax lands?** Doing it now touches the user's in-flight ERP work;
   doing it later means a second rename commit. Suggest bundling it into this item.
3. **Does the monolith need the same?** This whole suite is backend-java multitier only so far.
   `.NET` and TypeScript have no `contract/external/` at all — out of scope here, but the gap widens
   each time this one grows.

## Not in scope for this item

- `ClockRealParityContractTest` — see above; recommend explicitly declining it.
- Porting `contract/external/` to .NET / TypeScript.
- The legacy twins' unguarded inline stub JSON — see
  `20260814-1530-legacy-twin-stub-fidelity-gap.md`.
