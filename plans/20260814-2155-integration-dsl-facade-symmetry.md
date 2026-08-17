# Narrow-integration DSL facade — symmetry with `UseCaseDsl`

**Author:** Claude Code session, 2026-08-14
**Status:** 🟡 **Discussion required before any code is written.** Nothing here is approved. This
file records the analysis from the chat that produced it so the next session can resume cold.

**Trigger:** while reading `BaseGatewayIntegrationTest`, the user noticed it constructs `ErpDsl`,
`TaxDsl` and `ClockDsl` individually, and asked whether the system-test tree already has a facade
that encapsulates the whole DSL — the system *and* the external systems.

---

## What we established (facts, verified this session)

### The facade exists and is called `UseCaseDsl`

Two copies, same name, same role:

- **System test:**
  `system-test/java/src/main/java/com/mycompany/myshop/testkit/dsl/core/usecase/UseCaseDsl.java`
- **Backend component test:**
  `system/multitier/backend-java/src/testSupport/java/com/mycompany/myshop/backend/testkit/dsl/core/usecase/UseCaseDsl.java`

There is no class named `Dsl`, `AppDsl`, `SystemDsl`, `ShopDsl`, `Externals`, or
`ExternalSystemsDsl` anywhere in the repo. `UseCaseDsl` is the single aggregate, and `ScenarioDsl`
sits *on top of* it rather than beside it.

### It is already applied to component tests

`system/multitier/backend-java/src/componentTest/java/com/mycompany/myshop/backend/BaseComponentTest.java:131`
builds it in `@BeforeEach`, exposes both `protected UseCaseDsl app` and `protected ScenarioDslImpl
scenario`. Component tests use `app.erp()` / `scenario.given()…`. The Pact provider test
(`contract/internal/frontend/latest/BackendPactVerificationTest`) drives `@State` methods through
the same `app`.

### The only construction site outside a facade

`system/multitier/backend-java/src/integrationTest/java/com/mycompany/myshop/backend/integration/latest/base/BaseGatewayIntegrationTest.java:75,82,89`

```java
erp   = new ErpDsl(StubDrivers.erp(WIRE_MOCK));
tax   = new TaxDsl(StubDrivers.tax(WIRE_MOCK));
clock = new ClockDsl(StubDrivers.clock(WIRE_MOCK));
```

Three lazy fields + three accessors on the base class itself. This is the whole subject of this plan.

### `UseCaseDsl` holds three kinds of actor, not two

This was the key realisation of the session, and it reframes the problem:

| Actor kind | Component (`UseCaseDsl`) | Narrow-integration (`BaseGatewayIntegrationTest`) |
|---|---|---|
| SUT, **inbound** surface | `myShop()` — real HTTP via `BackendDriver` | **absent** |
| External systems | `erp()` `tax()` `clock()` | `erp()` `tax()` `clock()` ✅ |
| SUT, **outbound** surface | `sutErp()` `sutTax()` `sutClock()` | `erpGateway()` `taxGateway()` `clockGateway()` |

- **Row 1 is absent for a principled reason.** Nothing is booted at the narrow-integration layer —
  no Spring context, no port, no `MyShopDriver`. `ErpGateway` has no inbound surface; the test
  invokes it by method call, and that call *is* the SUT's entire API at this layer. There is no use
  case to wrap.
- **Row 3 is present but sits outside the facade.** `erpGateway()` is the exact analogue of
  `sutErp()`, which *is* inside `UseCaseDsl`. **This asymmetry is incidental, not principled** — and
  it is the actual thing worth fixing.

### `UseCaseDsl` cannot be reused as-is at the integration layer

Five of its eight constructor arguments have no meaning without a booted app:

| Arg | Narrow-integration |
|---|---|
| `MyShopDriver`, `ObjectMapper` | nothing to pass |
| `ErpDriver` / `TaxDriver` / `ClockDriver` | ✅ available via `StubDrivers` |
| `SutErpReader` / `SutTaxReader` / `SutClockReader` | wrap gateways built **per-test, per-mode** — nothing stable for a constructor |

Passing `null` for `myShop` would produce a facade whose headline accessor NPEs — worse than the
current duplication.

### The clock mode wrinkle (constrains any design)

`ClockGateway` is the only gateway with an `external.system-mode` branch, so
`BaseGatewayIntegrationTest` exposes three SUT-side accessors for it, all exercised by
`integration/latest/ClockGatewayIntegrationTest`:

- `clockGateway()` — class-fixed mode (default `STUB`)
- `clockGateway(ExternalSystemMode)` — line 57, pins the `real` branch
- `clockGatewayWithRawMode(String)` — line 63, pins the unknown-mode branch; deliberately untyped,
  since typing it makes the branch unreachable

Any facade must carry all three. Erp and Tax need one each.

### Eager vs lazy — the backend copy is the outlier

The system-test `UseCaseDsl` takes `Supplier<ErpDriver>` / `Supplier<TaxDriver>` /
`Supplier<ClockDriver>` and builds each sub-DSL on first access (so REAL-vs-STUB drivers aren't
constructed until touched); it is also `Closeable` and channel-aware. The backend `UseCaseDsl`
builds all four sub-DSLs eagerly in its constructor. `BaseGatewayIntegrationTest` is lazy by hand.
Worth noting, but the laziness buys almost nothing here — an `ErpDsl` wraps a `WireMock` client.

---

## Options on the table

### Option A — leave as-is

The base class already exposes both sides, and the method names (`erp()`, `tax()`, `clock()`)
already match the component vocabulary. Zero risk, zero work.

### Option B — extract an externals-only `ExternalSystemsDsl`

Proposed earlier in the session, then **withdrawn**: it groups only row 2 of the table above, so it
preserves exactly the stub-side/system-side split the user objected to. Recorded so it is not
re-proposed.

### Option C — `GatewayDsl` holding both sides ⭐ *the live candidate*

A facade for the narrow-integration layer that mirrors `UseCaseDsl`'s shape, with row 1 legitimately
absent:

```java
public class GatewayDsl {                        // integration/latest/base/
    public ErpDsl erp();   public ErpGateway erpSut();
    public TaxDsl tax();   public TaxGateway taxSut();
    public ClockDsl clock();
    public ClockGateway clockSut();                        // class-fixed mode
    public ClockGateway clockSut(ExternalSystemMode mode);
    public ClockGateway clockSutWithRawMode(String raw);
}
```

Tests read `dsl.erp().returnsProduct()…` / `dsl.erpSut().getProductDetails(…)` — the same
stub-side/system-side pairing as `app.erp()` / `app.sutErp()`.

**Honest sizing:** this is a regrouping, not new capability. The base class already gives both sides.
The payoff is structural symmetry with the 4-layer taxonomy, not line count.

---

## Open questions — **all require the user's decision**

### OQ1 — Do we do this at all? (A vs C)

The mechanical win is ~3 lines of duplication. The case for C is conceptual: the narrow-integration
harness would mirror the component harness *structurally* rather than coincidentally matching by
method name. Ties into `project_narrow_integration_target_taxonomy` (symmetric 4-layer test model).
**No recommendation offered — this is a taste/direction call the user should make.**

### OQ2 — If C: naming

`GatewayDsl` was the working name. Alternatives not discussed: `IntegrationUseCaseDsl`,
`NarrowIntegrationDsl`. Also undecided: `erpSut()` vs keeping `erpGateway()` vs a nested `sut().erp()`.
Note the component layer uses the `sut*` prefix (`sutErp()`), which argues for `erpSut()`/`sutErp()`.

### OQ3 — If C: does `UseCaseDsl` change too?

Two sub-questions:
- Should the backend `UseCaseDsl` switch to supplier-based lazy construction to match the system-test
  copy? (Independent of C; probably a separate plan.)
- Should anything be shared between `GatewayDsl` and `UseCaseDsl`, or do they stay independent
  classes that merely rhyme? Sharing risks re-introducing the null-`myShop` problem.

### OQ4 — Scope beyond Java

The repo has parallel .NET and TypeScript implementations. This session looked **only** at Java.
Whether the same facade asymmetry exists there, and whether C would have to be mirrored, is
**unexamined**. Per `CLAUDE.md` ("check all languages for the same issue"), this must be answered
before any C work is called complete.

---

## Not in scope / not touched

- No code was changed this session. Working tree is as it was at the start of the chat (the
  pre-existing modifications listed in the chat's opening git status are unrelated to this topic).
- `integration/legacy/*` twins are untouched by every option here — they deliberately use raw,
  inlined WireMock and no DSL at all.
- The inherited `origin/main` timestamp issue from
  `20260814-2137-chat3-handoff-legacy-integration-twins.md` is **still open and unrelated to this
  plan**; check it first if `main` is still red.

---

## Resume checklist for the next session

1. Read this file top to bottom.
2. Open the three anchors: `BaseGatewayIntegrationTest.java:75-115`, `BaseComponentTest.java:131-141`,
   `testkit/dsl/core/usecase/UseCaseDsl.java`.
3. **Ask the user OQ1 first.** Everything else is downstream of it.
4. Nothing here is pre-approved — do not start editing on the strength of this plan alone.
