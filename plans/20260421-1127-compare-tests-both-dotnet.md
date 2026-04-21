# 20260421-1127 — .NET System Test Alignment Plan (both)

Reference report: [20260421-1127-compare-tests-both.md](../reports/20260421-1127-compare-tests-both.md)

Timestamp: 20260421-1127
Mode: both
Reference implementation: **Java** (align .NET to Java unless explicitly stated otherwise).

All action items below are consistent with the **Known Language-Specific Divergences (Exceptions)** list in the `compare-tests` agent spec. Items covered by that list (e.g. `VoidValue.cs`, `ResultTaskExtensions.cs`, Then Success/Failure split, `IThenFailureAnd.cs`, `IThenSuccessAnd.cs`) are intentionally **not** included here.

Ordering: architectural mismatches → architecture layers (clients → drivers → channels → use-case DSL → scenario DSL → common) → tests (acceptance → contract → e2e → smoke).

---

## 1. Architecture — Use Case DSL

### 1.1 Rename `Base{X}Command` → `Base{X}UseCase` across all use-case bases

Java uses `BaseShopUseCase`, `BaseErpUseCase`, `BaseTaxUseCase`, `BaseClockUseCase`. TS matches Java. .NET currently uses `Base{X}Command`.

- File: `system-test/dotnet/Dsl.Core/UseCase/Shop/UseCases/Base/BaseShopCommand.cs` → rename to `BaseShopUseCase.cs`; rename class `BaseShopCommand` → `BaseShopUseCase`.
- File: `system-test/dotnet/Dsl.Core/UseCase/External/Erp/UseCases/Base/BaseErpCommand.cs` → rename to `BaseErpUseCase.cs`; rename class `BaseErpCommand` → `BaseErpUseCase`.
- File: `system-test/dotnet/Dsl.Core/UseCase/External/Tax/UseCases/Base/BaseTaxCommand.cs` → rename to `BaseTaxUseCase.cs`; rename class `BaseTaxCommand` → `BaseTaxUseCase`.
- File: `system-test/dotnet/Dsl.Core/UseCase/External/Clock/UseCases/Base/BaseClockCommand.cs` → rename to `BaseClockUseCase.cs`; rename class `BaseClockCommand` → `BaseClockUseCase`.
- Update all `: BaseShopCommand<...>` / `: BaseErpCommand<...>` / `: BaseTaxCommand<...>` / `: BaseClockCommand<...>` subclass inheritance across `Dsl.Core/UseCase/**/UseCases/*.cs` (ReturnsProduct.cs, PlaceOrder.cs, GetProduct.cs, GetTaxRate.cs, ReturnsTime.cs, GetTime.cs, GoToShop.cs, CancelOrder.cs, BrowseCoupons.cs, PublishCoupon.cs, DeliverOrder.cs, ViewOrder.cs, GetTimeVerification.cs, GetProductVerification.cs, GetTaxVerification.cs, BrowseCouponsVerification.cs, PlaceOrderVerification.cs, ViewOrderVerification.cs, GoToClock.cs, GoToErp.cs, GoToTax.cs, ReturnsTaxRate.cs, ReturnsPromotion.cs).
- Reference: `system-test/java/src/main/java/com/optivem/shop/testkit/dsl/core/usecase/shop/usecases/base/BaseShopUseCase.java` and siblings under `testkit/dsl/core/usecase/external/{clock,erp,tax}/usecases/base/`.

APPROVED

### 1.2 Add `SystemResults` at the use-case layer for shop

Java has `system-test/java/src/main/java/com/optivem/shop/testkit/dsl/core/usecase/shop/commons/SystemResults.java`. TS has `system-test/typescript/src/testkit/dsl/core/usecase/shop/commons/system-results.ts`. .NET has only `Driver.Port/Shop/SystemResults.cs` (at port layer).

- Add a new file: `system-test/dotnet/Dsl.Core/UseCase/Shop/Commons/SystemResults.cs` mirroring the Java use-case-layer helper.
- Keep `Driver.Port/Shop/SystemResults.cs` only if it is genuinely needed at the port layer for the `VoidValue` idiom; otherwise remove to avoid duplication. Decide after porting.
- Reference: `system-test/java/src/main/java/com/optivem/shop/testkit/dsl/core/usecase/shop/commons/SystemResults.java`.

## 2. Architecture — Scenario DSL

### 2.1 Rename `GherkinDefaults` → `ScenarioDefaults` and move out of `Dsl.Core.Gherkin` namespace

Java: `ScenarioDefaults` in `com.optivem.shop.testkit.dsl.core.scenario` package. .NET: `GherkinDefaults` in `Dsl.Core.Gherkin` namespace — both name and namespace diverge.

- File: `system-test/dotnet/Dsl.Core/Scenario/GherkinDefaults.cs` → rename to `ScenarioDefaults.cs`.
- Change class name `GherkinDefaults` → `ScenarioDefaults`.
- Change namespace `Dsl.Core.Gherkin` → `Dsl.Core.Scenario` (match the folder path).
- Rename all member constants from Java-equivalent names: `DefaultSku` → match Java `DEFAULT_SKU` spirit (keep .NET PascalCase `DefaultSku` naming conventions — no change needed), but align the **set of constants** exactly to Java's `ScenarioDefaults` (they already match).
- Update all imports across the .NET codebase: `using Dsl.Core.Gherkin;` → `using Dsl.Core.Scenario;`.
- Reference: `system-test/java/src/main/java/com/optivem/shop/testkit/dsl/core/scenario/ScenarioDefaults.java`.

### 2.2 Rename `When/Steps/I{X}.cs` → `IWhen{X}.cs` interfaces

Java uses `WhenBrowseCoupons`, `WhenCancelOrder`, `WhenPlaceOrder`, `WhenPublishCoupon`, `WhenViewOrder` as interface names. TS matches Java. .NET drops the `When` prefix.

- File: `system-test/dotnet/Dsl.Port/When/Steps/IBrowseCoupons.cs` → rename file to `IWhenBrowseCoupons.cs`; rename interface `IBrowseCoupons` → `IWhenBrowseCoupons`.
- File: `system-test/dotnet/Dsl.Port/When/Steps/ICancelOrder.cs` → rename to `IWhenCancelOrder.cs`; rename interface `ICancelOrder` → `IWhenCancelOrder`.
- File: `system-test/dotnet/Dsl.Port/When/Steps/IPlaceOrder.cs` → rename to `IWhenPlaceOrder.cs`; rename interface `IPlaceOrder` → `IWhenPlaceOrder`.
- File: `system-test/dotnet/Dsl.Port/When/Steps/IPublishCoupon.cs` → rename to `IWhenPublishCoupon.cs`; rename interface `IPublishCoupon` → `IWhenPublishCoupon`.
- File: `system-test/dotnet/Dsl.Port/When/Steps/IViewOrder.cs` → rename to `IWhenViewOrder.cs`; rename interface `IViewOrder` → `IWhenViewOrder`.
- Update all implementations and usages in `Dsl.Core/Scenario/When/Steps/*.cs` and any `using Dsl.Port.When.Steps;` consumers.
- Reference: `system-test/java/src/main/java/com/optivem/shop/testkit/dsl/port/when/steps/{WhenBrowseCoupons,WhenCancelOrder,WhenPlaceOrder,WhenPublishCoupon,WhenViewOrder}.java`.

APPROVED

---

## Local verification & commit

1. From `system-test/dotnet/`:
   - Run `Run-SystemTests -Architecture monolith` (latest suite). Do not substitute raw `dotnet test`.
   - Run `Run-SystemTests -Architecture monolith -Legacy` (legacy suite).
2. Fix any failures introduced by the renames (most likely: broken `using` directives, missing interface references).
3. Commit the .NET alignment changes as a single logical commit with a message such as:
   `dotnet: rename Base*Command → Base*UseCase, GherkinDefaults → ScenarioDefaults, I{X} → IWhen{X}, add Dsl.Core SystemResults`
