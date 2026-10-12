# 2026-10-12 12:00:00 UTC — One place for default Given values (scenario DSL)

> 🤖 **Picked up by agent** — `ValentinaLaptop` at `2026-10-12T10:45:59Z`

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

Step 7 (TypeScript part): .NET and Java are done and committed. TypeScript already has a single owner (`arrangeGiven` in `given/arrange-given.ts`, no defaults in `WhenStage`), so only align naming and the small gaps listed in the findings (no default coupon for orders; `DEFAULTS.SKU` / `UNIT_PRICE` duplicated between `GivenStage.product()` and the `arrangeGiven` fallback). Run the TypeScript sample suite with `GH_OPTIVEM_CONFIG=gh-optivem-monolith-typescript.yaml`. Then Step 8 (docs).

## Steps

7. **Parity (TypeScript remaining).** TypeScript already has a single owner (`arrangeGiven` in `given/arrange-given.ts`, no defaults in `WhenStage`), so only align naming and the small gaps listed in the findings.
8. **Docs.** If `docs/atdd/architecture/*.md` describes where defaults live, update it to name the single owner.

## Open questions

None — all resolved before execution (2026-10-12), following the recommendations:

- A declared Given with no dependents does not trigger default country/coupon creation; defaults only when something depends on them, encoded once in the owner.
- The coupon default stays Given-driven (only when orders exist); no new When-side default.
- Java runs the owner for every When (the .NET behaviour); confirm no Java test relies on the skip.

## Reply note (what changed, for the follow-up to the question)

Once executed, the reply can state: defaults now live in one component (`DefaultGivens`); `SetupGiven` and `EnsureGiven` both delegate to it; a previously hidden double creation of the default product and tax rate is gone; Java and TypeScript were aligned.
