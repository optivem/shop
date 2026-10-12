# 2026-10-12 12:00:00 UTC — One place for default Given values (scenario DSL)

## TL;DR

**Why:** A student asked why default test data is created in two places in the .NET scenario DSL. They are right: the same knowledge ("what is the default product / country / promotion") is spread across two stages, and the two paths can disagree.
**End result:** One component owns defaults and the "ensure present" logic. `GivenStage` and `WhenStage` both call it. Each default is created at most once per scenario, and Java and TypeScript match .NET.

## Findings already established (so we don't re-derive)

Source: `system-test/dotnet/Dsl.Core/Scenario/Given/GivenStage.cs` and `.../When/WhenStage.cs`.

| Default | `GivenStage.SetupGiven` | `WhenStage.EnsureGiven` |
|---|---|---|
| Product | `SetupErp`: `new GivenProduct(this)` when orders exist and no product declared | `ReturnsProduct()` with `DefaultSku` / `DefaultUnitPrice` when `!_hasProduct` |
| Country / tax rate | `SetupTax`: `new GivenCountry(this)` when orders exist and no country declared | `ReturnsTaxRate()` with `DefaultCountry` / `DefaultTaxRate` when `!_hasTaxRate` |
| Promotion | always executes `_promotion` (a default `GivenPromotion`) | `ReturnsPromotion()` with `DefaultPromotionActive` / `DefaultPromotionDiscount` when `!_hasPromotion` |
| Coupon | `SetupCoupons`: `new GivenCoupon(this)` when orders exist and no coupon declared | none |

- Two different mechanisms produce the same default: default *builders* in `GivenStage` (via `GivenProduct` and friends) versus direct ERP/Tax calls built from `ScenarioDefaults` in `WhenStage`.
- `GivenStage.When()` passes `_products.Count > 0` and `_countries.Count > 0` *before* `SetupGiven` runs, so a default product or country that `SetupGiven` adds for orders is not seen by `EnsureGiven`. It will create the default product and tax rate a second time.
- `GivenStage.When()` passes `hasPromotion = true` unconditionally, so the promotion branch in `EnsureGiven` only runs for the no-Given constructor.
- `ThenStageBase` gets only `SetupGiven`, so the Then path never gets the `WhenStage` defaults.

### Java and TypeScript (checked 2026-10-12)

- **Java: same duplication.** `core/scenario/given/GivenImpl.java` creates defaults for product, country and coupon when orders exist. `core/scenario/when/WhenImpl.java` `ensureDefaults()` creates product, tax rate and promotion from `ScenarioDefaults`.
- **Java: no double creation.** `when()` runs `setup()` first and reads `!products.isEmpty()` afterwards, so the default product added for orders is seen. Only .NET reads the flags before setup.
- **Java: inconsistent coverage.** `deliverOrder`, `browseOrderHistory`, `publishCoupon` and `browseCoupons` skip `ensureDefaults()`, while .NET runs `EnsureGiven` for all of them. Decide which is intended.
- **Java: dead branch.** `when()` always passes `hasPromotion = true`, as in .NET.
- **TypeScript: not affected.** `arrangeGiven` is the only place that decides defaults (country, product; promotion is always set) and `WhenStage` has none. Minor gaps: no default coupon for orders, and `DEFAULTS.SKU` / `UNIT_PRICE` appear both in `GivenStage.product()` and in the `arrangeGiven` fallback.

## Outcomes

- A single type (working name `DefaultGivens`) holds the rules: which defaults are needed, and when.
- `GivenStage.SetupGiven` and `WhenStage.EnsureGiven` no longer contain default-creation code. They call the one component.
- Each default is created at most once per scenario, whichever path runs first.
- The default values themselves still come from `ScenarioDefaults` only.
- No existing acceptance, contract or e2e test changes behaviour.
- Java and TypeScript implement the same structure.

## ▶ Next executable step (resume here)

Step 1: in `system-test/dotnet`, add a small failing test (or a recorded-call assertion on the ERP and Tax stubs) for a scenario with an order and no declared product. It asserts that the default product and tax rate are each created exactly once. This confirms the double creation described above before any refactor. Gate: `dotnet build` and `gh optivem system-test run --sample` for .NET.

## Steps

1. **Pin the current behaviour (.NET).** Step above. Report whether the double creation is reproduced.
2. **Introduce the single owner (.NET).** Add `DefaultGivens` next to `ScenarioDefaults`. It tracks which of product, country/tax rate, promotion and coupon are already present, and has one idempotent `EnsureAsync(...)`.
3. **Route `GivenStage` through it.** Replace the three `if (_orders.Count > 0 && _x.Count == 0)` blocks with calls to the owner. Declared givens mark the matching default as present.
4. **Route `WhenStage` through it.** Remove the `hasProduct` / `hasPromotion` / `hasTaxRate` constructor flags and the `Returns*` default calls from `EnsureGiven`. It calls the owner after `_givenSetup`.
5. **Decide the Then path.** Either `ThenStageBase` gets the same owner, or it is documented why it doesn't need it. Recommended: share the owner, so all three stages behave the same.
6. **Verify (.NET).** `./compile-all.sh`, then `gh optivem system-test run --sample` with `GH_OPTIVEM_CONFIG=gh-optivem-monolith-dotnet.yaml`.
7. **Parity.** Java has the same duplication (`GivenImpl.setupErp/setupTax/setupCoupons` versus `WhenImpl.ensureDefaults`), so apply the same structure there and run the Java sample suite. TypeScript already has a single owner (`arrangeGiven` in `given/arrange-given.ts`, no defaults in `WhenStage`), so only align naming and the small gaps listed in the findings.
8. **Docs.** If `docs/atdd/architecture/*.md` describes where defaults live, update it to name the single owner.

## Open questions

- Should a declared Given that has no dependents (for example a product with no orders) still trigger default country and coupon creation? Recommended: no, keep today's rule (defaults only when something depends on them), and encode it once in the owner.
- Where does the coupon default belong when there is no When-side coupon fallback today? Recommended: keep it Given-driven (only when orders exist), and don't introduce a new When-side default.

## Reply note (what changed, for the follow-up to the question)

Once executed, the reply can state: defaults now live in one component (`DefaultGivens`); `SetupGiven` and `EnsureGiven` both delegate to it; a previously hidden double creation of the default product and tax rate is gone; Java and TypeScript were aligned.
