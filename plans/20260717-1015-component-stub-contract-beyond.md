# Component stub-contract tests — beyond system-test

**Source plan:** `plans/20260717-1015-component-stub-contract-mirror.md` (PASS 1 = mirror only).
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

### 1. Negative / missing-resource contract tests
System-test has **no** `doesNotExist` on `given` or `then` for the contract DSL; it expresses
"missing" implicitly (unregistered SKU) and only end-to-end (place-order rejection). Adding, at
component level:
- `then().product().doesNotExist()` — pins `ErpGateway.getProductDetails` `404 → Optional.empty()`.
- `then().country().doesNotExist()` — pins `TaxGateway.getTaxDetails` `404 → Optional.empty()`
  (`TaxStubDriver.stubTaxMissing` already exists on the given side).
- New `doesNotExist()` verb on the `Then*` steps — DSL surface system-test deliberately never had.

**Value:** these adapter 404 branches are only exercised implicitly today. **Cost:** new assertion
verbs beyond system-test. **If built,** add matching negative contract tests to system-test too.

### 2. ERP promotion contract test
System-test's `BaseErpContractTest` covers only `getProduct`, not `getPromotionDetails`. The SUT has
`ErpGateway.getPromotionDetails()` (parses `{"promotionActive","discount"}`) and the component
`ErpStubDriver.stubPromotion` already programs it. A `then().promotion().isActive(...).hasDiscount(...)`
stub-contract test would pin that parse. Requires a new `SutErpReader.readPromotion()` +
`ThenPromotion` step. Add to system-test as well for symmetry.

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

**Proposal:** widen the shared body to assert every field the SUT's `ProductDetailsResponse` actually
binds, **and** enrich the stub mappings to match — as one change, since widening alone turns
`contract-stub` red immediately. Six test files (three languages × latest/legacy) share one body per
language; the `Real`/`Stub` subclasses stay 5-line mode overrides.

**Constraint that bounds this — the real-driver no-op convention.** In REAL mode the `returns*`
seeding steps are deliberately no-ops, because a real external system cannot be configured:
`ClockRealDriver.returnsTime` says so explicitly (`// No-op because real clock cannot be configured`),
`TaxRealDriver.returnsTaxRate` likewise, and `ErpRealDriver.returnsPromotion` follows the same
pattern. This is what lets one scenario script run in both modes — **do not "fix" these no-ops.**
Consequence: real mode can only assert what the backing happens to serve, except for ERP *products*,
where `returnsProduct` genuinely POSTs to the simulator. So this item is viable for products and
**not** for promotion (see item 2) or clock/tax.

**Also note:** "real" here is the in-repo simulator, not a vendor ERP. Even a green `contract-real`
gives no assurance against an actual vendor — closing that would mean a live vendor dependency in CI,
which conflicts with the zero-infra/$0 default-path constraint.

### 5. Cross-language mirror
Port PASS 1 (and any of the above that ship) to the **.NET** and **TypeScript** multitier backends,
which have the equivalent component/stub structure. Keep the three languages' component contract
tests in lockstep.

### 6. System-test symmetry back-fill
If items 1–2 are deemed worth having, the cleanest end state adds the same negative/promotion
contract coverage to **system-test** too, so "component DSL mirrors system-test" stays true rather
than the component being strictly richer.

## Notes
- None of these block PASS 1; PASS 1 stands alone as a faithful mirror.
- Item 4 was raised independently of items 1–3 and can be decided on its own.
- Moved out of `plans/deferred/` on 2026-07-20. Still requires a build decision per item — being
  active means "on the table for discussion", not "approved".
