# Component stub-contract tests — beyond system-test

**Source plan:** `plans/20260717-1015-component-stub-contract-mirror.md` (PASS 1 = mirror only) —
completed and deleted; kept here only as provenance.
**Scope (this file):** `system/multitier/backend-java` + `system/multitier/frontend-react` only.
Porting to `backend-dotnet` / `backend-typescript` is deliberately **out of scope** — see
`plans/deferred/20260720-1118-component-stub-contract-cross-language-mirror.md`. This mirrors the
scoping of `plans/deferred/20260616-0830-component-pact-layer-other-multitier-backends.md`: prove the
shape in java/react first, port once it stops moving.
**Status:** Active — **not yet approved to build. Discuss before executing any item.** Each item goes
**past what system-test's contract DSL currently has**, so it was intentionally excluded from PASS 1
to keep the component DSL a faithful mirror. Prefer adding any item to **both** layers so component
and system-test stay symmetric rather than the component sprouting steps system-test lacks.

## Context

PASS 1 mirrors system-test exactly: three positive, exact-value stub-contract tests
(`clock`/`country`/`product`) whose `then().<external>()` reads through the SUT's production gateway.
System-test's contract DSL is positive-only, product-only (no promotion), and has no interaction
checks. The items here are the honest-but-extra things the design discussion surfaced.

## Deferred items

### 3. ERP interaction verification (different concern)
Not a read-back at all: verify the SUT *made the expected outbound call* to the ERP —
`then().erp().wasAskedForProduct("ABC")` / `wasNotAskedForProduct(...)`, backed by WireMock's request
log (`wireMock.verifyThat(getRequestedFor(...))`). Catches "SUT built a wrong/absent outbound
request" — orthogonal to shape drift. No system-test equivalent; decide separately whether
interaction pinning is wanted at all (it couples tests to implementation, so only where the
fact-of-the-call is part of the contract).

Sketch:

```java
// target test
scenario
    .given().product().withSku("ABC").withUnitPrice(20.00)
    .when().placeOrder().withSku("ABC")
    .then().shouldSucceed()
    .and().erp().wasAskedForProduct("ABC");     // NEW: verifies the outbound GET happened

// port/then/steps/ThenErp.java
public interface ThenErp extends ThenStep<ThenErp> {
    ThenErp wasAskedForProduct(String sku);
    ThenErp wasNotAskedForProduct(String sku);
}

// ThenErpImpl delegates to the stub driver's WireMock client:
//   app.erp().verifyProductRequested(sku)  /  verifyProductNotRequested(sku)

// new methods on ErpStubDriver.java — assert against WireMock's request log:
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;

public void verifyProductRequested(String sku) {
    wireMock.verifyThat(getRequestedFor(urlEqualTo("/api/products/" + sku)));
}
public void verifyProductNotRequested(String sku) {
    wireMock.verifyThat(exactly(0), getRequestedFor(urlEqualTo("/api/products/" + sku)));
}
```

This reads WireMock's record of what the SUT *did* (not what the test planted), so it is a real
assertion — it fails if the SUT forgets to call the ERP, hits the wrong URL, or calls it twice.

### 4. Stub-vs-real field-set divergence in system-test's ERP contract suite
Surfaced 2026-07-20 while asking "what assures the stub matches the real?". The acceptance stage runs
`BaseErpContractTest` twice — `ErpStubContractTest` (WireMock) and `ErpRealContractTest` (the
`external-systems/simulators/mock-server.js` json-server). Both are default steps in every
`*-acceptance-stage.yml`. But the shared body asserts only two fields:

```java
.given().product().withSku("BOOK-123").withUnitPrice(12.0)
.then().product("BOOK-123").hasSku("BOOK-123").hasPrice(12.0)
```

Meanwhile the two backings **already disagree on field set** and nothing catches it:
- stub mapping `external-systems/stubs/mappings/erp-products-hp15.json` → `{id, price}`
- simulator `mock-server.js` products → `{id, title, description, price, category, brand}`

**The original proposal does not work.** It was "widen the shared body to assert every field the SUT's
`ProductDetailsResponse` actually binds, and enrich the stub mappings to match". But all three SUTs
bind exactly `{id, price}` and nothing else — Java `ProductDetailsResponse` is two fields plus
`@JsonIgnoreProperties(ignoreUnknown = true)`; .NET and TypeScript are the same two fields. The
shared body **already** asserts both (`hasSku` + `hasPrice`). So widening to the bound field set adds
zero assertions and could never turn `contract-stub` red. The divergence is invisible by
construction: the SUT ignores every field the two backings disagree on.

**Revised open question:** do we want the stub mappings and the simulator payloads to match for
their own sake — fidelity, and teaching value about what a stub is meant to represent — accepting
that no test can detect the difference? That is a much weaker case than the original framing, and it
is the only one left. Decide on that basis or drop the item.

If it is pursued, note the test-file shapes differ per language: Java and .NET use one shared base
per version (latest/legacy) with 5-line `Real`/`Stub` subclass mode overrides; TypeScript uses
`erp-stub-contract-test.spec.ts` / `erp-real-contract-test.spec.ts` spec files around a shared
`BaseErpContractTest.ts`.

**Constraint that bounds this — the real-driver no-op convention.** In REAL mode the `returns*`
seeding steps are deliberately no-ops, because a real external system cannot be configured:
`ClockRealDriver.returnsTime` says so explicitly (`// No-op because real clock cannot be configured`),
`TaxRealDriver.returnsTaxRate` likewise, and `ErpRealDriver.returnsPromotion` follows the same
pattern. This is what lets one scenario script run in both modes — **do not "fix" these no-ops.**
Consequence: real mode can only assert what the backing happens to serve, except for ERP *products*,
where `returnsProduct` genuinely POSTs to the simulator. So this item is viable for products and
**not** for promotion or clock/tax.

**Also note:** "real" here is the in-repo simulator, not a vendor ERP. Even a green `contract-real`
gives no assurance against an actual vendor — closing that would mean a live vendor dependency in CI,
which conflicts with the zero-infra/$0 default-path constraint.

## Decided against

### ERP promotion contract test (was item 2) — dropped 2026-07-20
Proposed a component stub-contract test pinning `ErpGateway.getPromotionDetails()`'s parse of
`{"promotionActive","discount"}`, via a new `SutErpReader.readPromotion()` + `ThenPromotion` step,
plus a matching system-test back-fill for symmetry (was item 5).

**Dropped for the same reason as the negative-contract item below: the coverage already exists one
layer down.** `ErpGatewayIntegrationTest.getPromotionDetailsReturnsPromotion` programs
`returnsPromotion().active(true).discount("0.15")` and asserts `isPromotionActive()` /
`getDiscount()` through the production gateway — no Spring context, no Testcontainers. That is
exactly the parse the component test would have pinned.

The system-test half was independently non-viable: item 4's real-driver no-op constraint means
`ErpRealDriver.returnsPromotion` is a deliberate no-op, so a promotion contract test cannot run in
REAL mode at all.

As with the item below, if this resurfaces the question is whether the *component DSL's
expressiveness* is wanted for its own sake — the coverage argument is settled.

### Negative / missing-resource contract tests (was item 1) — dropped 2026-07-20
Proposed `then().product().doesNotExist()` / `then().country().doesNotExist()` at component level to
pin the `404 → Optional.empty()` branches of `ErpGateway.getProductDetails` / `TaxGateway.getTaxDetails`.

**Dropped because the coverage now exists one layer down, cheaper.** `TaxGatewayIntegrationTest` and
`ErpGatewayIntegrationTest` (added in `e05c16df`) pin those exact branches at the narrow-integration
layer — same stub drivers, same production gateways, but no Spring context and no Testcontainers
Postgres. The item's stated value ("these adapter 404 branches are only exercised implicitly today")
is no longer true.

A 404-to-empty mapping is adapter behaviour, which is what the narrow-integration layer exists to
pin. Component tests should stay scenario-shaped: *unknown SKU → order rejected*, which needs only
the **given** side (`returnsNoProduct`, already present) and asserts on the rejection.

The follow-on cost was the deciding factor: the item required matching negative tests in system-test,
whose contract DSL must run identically in STUB and REAL mode (`ErpStubContractTest` /
`ErpRealContractTest` share one base). Every verb needs both adapters — `returnsProduct` survives only
because `ErpRealDriver` POSTs to the simulator's create API. A `doesNotExist` real adapter would need
a delete/absence capability that does not exist on the port today. Buildable (the "real" ERP is the
in-repo simulator, not a vendor system — see item 4), but it is real design work to duplicate coverage
that is already green.

**Do not re-defer this.** If it resurfaces, the question to ask is whether the *component DSL's
expressiveness* is wanted for its own sake — the coverage argument is settled.

## Notes
- None of these block PASS 1; PASS 1 stands alone as a faithful mirror.
- Item 4 was raised independently of the others and can be decided on its own.
- Moved out of `plans/deferred/` on 2026-07-20. Still requires a build decision per item — being
  active means "on the table for discussion", not "approved".
- The cross-language mirror was split out to `plans/deferred/20260720-1118-component-stub-contract-cross-language-mirror.md`
  on 2026-07-20 to keep this file's scope to java/react. Whatever ships here defines that file's payload.
