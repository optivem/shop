# Decoupling from External Systems

How to keep systems you do not control from leaking into your core — which goes well beyond putting
an interface in front of the HTTP call.

MyShop talks to three systems it does not own: the **ERP** (products, promotions), the **tax
service** (per-country rates) and the **clock**. The two implementations differ in how much of
those systems reaches the middle:

| | Path |
|---|---|
| **Before** — supplier shapes in `core` | [`system/multitier/backend-java`](../../backend-java) |
| **After** — ports in `domain`, wire types package-private | [`system/multitier/backend-clean-java`](..) — this project |

Related: [Decoupling the Domain from the ORM](decoupling-domain-from-orm.md) ·
[Designing for Performance](designing-for-performance.md)

---

## Six leaks, only one of which is I/O

Everyone gets leak 0 — "put an interface in front of the HTTP call". It is necessary and nowhere
near sufficient.

| # | The leak | The tell |
|---|---|---|
| 0 | **Their I/O** | `HttpClient` constructed inside a service |
| 1 | **Their types** | Your core imports `ProductDetailsResponse` |
| 2 | **Their vocabulary** | Your core writes `promotion.isPromotionActive() ? ... : ONE` |
| 3 | **Their failures** | An ERP outage returns HTTP 500 and pages *your* team |
| 4 | **Their schema evolution** | The supplier adds a JSON field and you ship a hotfix |
| 5 | **Their absence semantics** | "no such product" and "ERP is down" take the same code path |

---

## Leaks 0 and 1 — their types reach the centre

**Before** — `backend-java/.../core/services/external/ErpGateway.java`. It is a **concrete class**,
in `core`, holding an `HttpClient`, returning the supplier's JSON shape:

```java
package com.mycompany.myshop.backend.core.services.external;

@Service
public class ErpGateway {                                     // <- not an interface
    public GetPromotionResponse getPromotionDetails() { ... }  // <- the ERP's shape, on a public
    public Optional<ProductDetailsResponse> getProductDetails(String sku) { ... }  //    signature
}
```

and the wire DTO lives in `core/dtos/external/` — public, in the core:

```java
package com.mycompany.myshop.backend.core.dtos.external;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductDetailsResponse {   // <- public
    private String id;                  // <- the ERP calls it "id". Your domain calls it a SKU.
    private BigDecimal price;
}
```

**After** — the port lives in `domain/gateways`, in MyShop's vocabulary:

```java
public interface ErpGateway {
    Optional<Product> getProductDetails(Sku sku);
    Promotion getPromotionDetails();
}
```

and the wire DTO is **package-private**, in `infrastructure/external/erp`:

```java
package com.mycompany.myshop.backend.infrastructure.external.erp;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class ProductDetailsResponse {   // <- no `public`. That is the whole trick.
    private String id;
    private BigDecimal price;
}
```

The translation is two lines in `HttpErpGateway`, and the methods that touch the wire type are
**private**:

```java
@Override
public Optional<Product> getProductDetails(Sku sku) {
    return fetchProductDetails(sku.value())
            .map(wire -> new Product(Sku.of(wire.getId()), Money.of(wire.getPrice())));
}

// Private, and that is the whole point of the class. The two methods above are the boundary: they
// are the only code that ever holds the ERP's JSON shape, and what they hand on is a domain value.
// A wire DTO on a public signature -- even here in infrastructure, even "just for a test" -- makes
// the supplier's field names reachable from somewhere else, and reachable is how they end up in
// the centre.
private Optional<ProductDetailsResponse> fetchProductDetails(String sku) { ... }
```

Package-private is not a style choice here, it is the enforcement mechanism: the supplier's field
names cannot be *named* outside the one package that parses them. The compiler enforces the
boundary, not the review.

### The leak nobody plans for: test support

From the `backend-clean-java` README:

> Test support obeys the same rule: `SutErpReader` and `SutTaxReader` read through `ErpGateway` /
> `TaxGateway` and assert on `Product` / `TaxRate`, exactly as `SutClockReader` always did.
> **"Just for a test" is how a wire type becomes public, and a public wire type is how it reaches
> the domain.**

The boundary is almost always broken by a test first.

---

## Leak 2 — their vocabulary becomes your conditionals

**Before** — the ERP's two-field shape leaks into pricing logic in `OrderService`:

```java
var promotion = erpGateway.getPromotionDetails();
var promotionFactor = promotion.isPromotionActive() ? promotion.getDiscount() : BigDecimal.ONE;
```

**After** — `domain/values/Promotion.java` owns the meaning:

```java
public record Promotion(boolean active, Rate discount) {

    public static Promotion inactive() { return new Promotion(false, Rate.ONE); }

    public Rate factor() { return active ? discount : Rate.ONE; }
}
```

> it keeps its behaviour: `factor()` is what callers actually want, because an inactive promotion
> and a promotion that discounts nothing multiply the price identically.

The call site becomes `promotion.factor()`. The ERP's *representation* of "no promotion" — two
fields that must be read together — never escapes the boundary.

### What these types are, and are not

From the README:

> Only what MyShop owns is an entity. `Product`, `TaxRate` and `Promotion` are immutable snapshots
> of records the ERP and the tax service own, so they are values, and they live in `domain/values`
> together — as records, like everything else in that package, so two readings of the same product
> compare equal. **The gateway ports therefore traffic in values, never in aggregates.**

This is the subtle one. A `Product` fetched from the ERP has no lifecycle here. Modelling it as an
entity invites somebody to try to change it, and you do not own it.

The `record` choice is load-bearing, not cosmetic — `domain/values/Product.java`:

> A record like every other value here, so two readings of the same product compare equal — which a
> hand-written class without `equals` could not do, and **which the parity contract tests rely on**
> when they compare what the stub returned against what the real system did.

---

## Leak 3 — their failures become indistinguishable from your bugs

**Before** — the legacy `ErpGateway` throws `IllegalStateException`:

```java
throw new IllegalStateException("ERP API returned status " + response.statusCode() + ...);
```

That surfaces as a **500**. On-call gets paged for the supplier's outage, and the response tells the
caller the bug is yours. Where the legacy code did define a gateway type at all
(`core/exceptions/TaxGatewayException`), it filed it next to `ValidationException` — unrelated to
any port.

**After** — the failure family lives **beside the ports**, in `domain/gateways`:

```java
package com.mycompany.myshop.backend.domain.gateways;

// Beside the ports rather than beside the adapters, because a port declares both halves of its
// contract: what it answers with, and how it says it could not answer. While this type lived in
// infrastructure, no layer allowed to catch it was allowed to name it -- so "the ERP is down" could
// only ever surface as the catch-all 500, indistinguishable from a bug of ours.
//
// The subclasses name the system, not the failure mode: from the core's point of view an unreachable
// ERP, a 500 from the ERP, and a body the ERP sent that we could not read are one outcome -- we asked
// a system we do not control and did not get an answer.
public abstract class GatewayException extends RuntimeException { ... }
```

with `ErpGatewayException`, `TaxGatewayException` and `ClockGatewayException` under it. One handler
maps the whole family — `presentation/exception/GlobalExceptionHandler.java`:

```java
// 502 rather than 500: the request was fine and this server is fine, but an upstream system we do
// not control failed to answer. 502 covers all three ways it can fail us -- unreachable, an error
// status, a body we could not read -- where 503 would promise the caller that retrying soon helps,
// which we cannot know. The distinction matters to whoever is paged: a 500 says look at our code,
// a 502 says look at theirs.
@ExceptionHandler(GatewayException.class)
public ResponseEntity<ProblemDetail> handleGatewayException(GatewayException ex) {
    // The message names the upstream URL and its response body. That is exactly what the log needs
    // and exactly what the response must not carry.
    log.error("External system did not answer", ex);

    var problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, GATEWAY_ERROR_DETAIL);
    // ...
}
```

Three distinct points live in there:

1. **A port declares its failure modes.** An interface that only says what it returns is half a
   contract.
2. **Subclasses name the system, not the failure mode.** The core does not care *how* the ERP
   failed. It cares *that the ERP* failed.
3. **The detail goes to the log; a sanitised message goes to the response.** The upstream URL and
   body are exactly what you need to debug and exactly what you must not leak.

Being decoupled from an external system is not only "can I swap the implementation". It is "when
they break, does my system correctly say **they** broke".

---

## Leak 4 — their schema evolution becomes your outage

Every wire DTO in `infrastructure/external` carries this:

```java
@JsonIgnoreProperties(ignoreUnknown = true)
class GetPromotionResponse { ... }
```

> every wire DTO in this package tolerates fields it does not read, so a supplier adding one is a
> non-event rather than an outage.

Compare `backend-java/core/dtos/external/GetPromotionResponse.java`, which has **no annotation**:

```java
@Data
public class GetPromotionResponse {        // ERP adds one field -> deserialization throws
    private boolean promotionActive;
    private BigDecimal discount;
}
```

A tiny diff, and an easy live demo: add a field to the stub's JSON, watch the legacy path fall over
and the decoupled one shrug.

---

## Leak 5 — "not found" versus "could not ask"

The distinction most teams collapse, and the cheapest one to get right —
`infrastructure/external/erp/HttpErpGateway.java`:

```java
if (response.statusCode() == 404) {
    return Optional.empty();                        // a DOMAIN answer: no such product
}
if (response.statusCode() != 200) {
    throw new ErpGatewayException(...);             // a GATEWAY failure: we could not ask
}
```

- `Optional.empty()` becomes a 400/404 for the client. That is *your* validation rule —
  `PlaceOrder` turns a missing product into `Product does not exist for SKU: ...`.
- `GatewayException` becomes a 502. That is *their* problem.

If both are exceptions, or both are `Optional.empty()`, two completely different operational
situations have been merged, and no amount of interface-shaped decoupling recovers the difference.

---

## Proving the stub is honest

Decoupling means testing against a stub, which raises the question that sinks most of these
architectures in practice: **does the stub still behave like the real system?**

The answer here is three tests sharing one abstract base.

**The base states the contract, in domain terms** —
`contractTest/.../erp/BaseErpProductParityContractTest.java`:

```java
abstract class BaseErpProductParityContractTest {

    protected abstract void arrangeProduct(String sku, String price);
    protected abstract ErpGateway erpGateway();

    @Test
    void getProductDetailsReturnsDetailsWhenFound() {
        arrangeProduct("BOOK-123", "10.00");

        var result = erpGateway().getProductDetails(Sku.of("BOOK-123"));

        assertThat(result).isPresent();
        assertThat(result.get().sku()).isEqualTo(Sku.of("BOOK-123"));
        assertThat(result.get().price()).isEqualTo(Money.of("10.00"));
    }

    @Test
    void getProductDetailsReturnsEmptyWhenNotFound() {
        assertThat(erpGateway().getProductDetails(Sku.of("UNKNOWN-CONTRACT-SKU"))).isEmpty();
    }
}
```

**Run 1 — stub parity, against WireMock** (`ErpStubParityContractTest`):

```java
erpGateway = new HttpErpGateway(WIRE_MOCK.baseUrl());

@Override
protected void arrangeProduct(String sku, String price) {
    WIRE_MOCK.stubFor(get("/api/products/" + sku)
        .willReturn(okJson("{\"id\":\"" + sku + "\",\"price\":" + price + "}")));
}
```

**Run 2 — real parity, against the simulator container** (`ErpRealParityContractTest`):

```java
private static final String BASE_URL = ExternalSystemSimulator.baseUrl("/erp");
private final SimulatorErpProductClient client = new SimulatorErpProductClient(BASE_URL);
private final ErpGateway erpGateway = new HttpErpGateway(BASE_URL);

@Override
protected void arrangeProduct(String sku, String price) {
    client.createProduct(sku, price);        // arrange through the real system's own API
}
```

**Run 3 — stub consumability**, i.e. can the booted SUT actually swallow what the stub emits
(`ErpStubConsumabilityContractTest`):

```java
scenario
    .given().product().withSku("BOOK-123").withUnitPrice(10.00)
    .then().product("BOOK-123").hasSku("BOOK-123").hasPrice(10.00);
```

Same assertions, two arrange strategies, two backing systems behind one port. The stub is not
trusted — it is held to the same contract as the real system. That is what turns "we mocked the
ERP" from a liability into a guarantee.

For where these suites sit in the wider test model, see
[docs/atdd/test-taxonomy.md](../../../../docs/atdd/test-taxonomy.md) — external-system contract tests are suite 5,
the counterparty that will not run our verification.

Running them locally requires the simulator image:

```shell
./gradlew externalSimulatorImage   # build the image the real-parity tests need
./gradlew contractTest
```

or point them at an already-running instance with `EXTERNAL_SIMULATOR_BASE_URL`.

---

## Suggested walkthrough order

1. Open legacy `core/services/external/ErpGateway.java` and ask what is wrong. People answer "it is
   not an interface." That is the least of it.
2. Point at `core/dtos/external/ProductDetailsResponse` — public, in `core`. The ERP's field name
   `id` is now a term in your business.
3. Point at `throw new IllegalStateException` — on-call gets paged for their outage.
4. Switch to the decoupled version: interface in `domain`, DTO package-private, `GatewayException`
   beside the port, 502 at the edge.
5. Add a field to the ERP stub's JSON: legacy breaks, decoupled shrugs.
6. Finish on `BaseErpProductParityContractTest` and run it twice — WireMock, then the container.
