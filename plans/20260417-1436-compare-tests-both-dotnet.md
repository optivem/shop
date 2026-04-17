# .NET — System Test Alignment Plan

Reference report: `reports/20260417-1436-compare-tests-both.md`

Reference implementation: **Java**. Each task aligns .NET to Java unless noted.

Ordering: architectural mismatches first, then architecture layers (clients → drivers → channels → use-case DSL → scenario DSL → common → ports), then tests (acceptance → contract → e2e → smoke).

---

## B. Architecture Layers — Clients

### B8. .NET — verify `SystemErrorMapper` equivalent exists
- Check `system-test/dotnet/Driver.Adapter/Shop/Api/` for an equivalent. If absent, add it mirroring Java.

---

## F. Architecture Layers — Scenario DSL

### F1. .NET — remove extra `WhenGoToShop.cs`
- File: `system-test/dotnet/Dsl.Core/Scenario/When/Steps/WhenGoToShop.cs`.
- Decision: Java and TS have no equivalent. Either remove from .NET, or add to Java and TS. **Recommended**: remove from .NET (Java is reference and has no equivalent).

### F2. .NET — reconcile ThenFailureAnd/ThenSuccessAnd/ThenFailureCoupon/ThenFailureOrder/ThenSuccessCoupon/ThenSuccessOrder/BaseThenResultCoupon/BaseThenResultOrder/ThenStageBase
- Files under `system-test/dotnet/Dsl.Core/Scenario/Then/Steps/`.
- Java aggregates by entity (`ThenClock`, `ThenCountry`, `ThenCoupon`, `ThenOrder`, `ThenProduct`). .NET splits by outcome+entity.
- Decision: align .NET to Java — collapse the `Success*` / `Failure*` / `*And` variants into a unified entity-based `Then*` set. **Recommended**: collapse; Java's decomposition is simpler.

### F6. .NET — add `GivenStep`/`ThenStep` base ports
- Files: `system-test/dotnet/Dsl.Port/Given/Steps/Base/IGivenStep.cs` (exists), `Then/Steps/Base/IThenStep.cs` (add).

### F8. .NET — remove `ScenarioDslFactory.cs` or add to Java/TS
- Decision: .NET has it; Java does not. **Recommended**: remove from .NET to match Java.

---

## G. Architecture Layers — Common

### G4. .NET — reconcile `ResultTaskExtensions.cs` and `VoidValue.cs`
- Files: `system-test/dotnet/Common/ResultTaskExtensions.cs`, `VoidValue.cs`.
- Java does not have these. **Recommended**: keep in .NET only if language idiom requires them; document they're language-specific in a comment. Do not add to Java or TS.

### G5. Java — reconcile `Closer.java`
- Referenced by nearly every base test — keep. Ensure .NET has an equivalent pattern (IDisposable + using).

---

## H. Architecture Layers — Driver Ports

### H2. .NET — add `GetCountryRequest`
- File (new): `system-test/dotnet/Driver.Port/External/Tax/Dtos/GetCountryRequest.cs`.
- Reference: Java `GetCountryRequest.java`.

### H4. .NET and TypeScript — add `GetPromotionResponse`
- Files: `system-test/dotnet/Driver.Port/External/Erp/Dtos/GetPromotionResponse.cs`, `system-test/typescript/src/testkit/driver/port/external/erp/dtos/GetPromotionResponse.ts`.
- Reference: Java `GetPromotionResponse.java`.
- **Source (TS):** ✏️ Net-new — `GetPromotionResponse` not present anywhere in `eshop-tests/typescript/` (grep finds zero hits).

---

## P. Legacy Tests — mod05

### P1. .NET — align `PlaceOrderNegativeBaseTest` parameterization with Java
- File: `system-test/dotnet/SystemTests/Legacy/Mod05/E2eTests/PlaceOrderNegativeBaseTest.cs`.
- Current: `[InlineData("3.5"), InlineData("lala")]` — two cases.
- Java: single case `"3.5"`.
- **Recommended**: remove `[InlineData("lala")]` so .NET matches Java. Alternatively, add `"lala"` to Java if both languages should have both cases; but Java is the reference.

---

## U. Legacy Tests — mod10

### U1. .NET — add `ShouldRejectOrderWithNonPositiveQuantity` to mod10 acceptance
- File: `system-test/dotnet/SystemTests/Legacy/Mod10/AcceptanceTests/PlaceOrderNegativeTest.cs`.
- Add method parameterized over `"-10"`, `"-1"`, `"0"` asserting field error `quantity / Quantity must be positive`.
- Reference: `system-test/java/.../legacy/mod10/acceptance/PlaceOrderNegativeTest.java` lines with `@ValueSource(strings = {"-10", "-1", "0"})`.

---

## Local verification & commit

- From `system-test/dotnet/`, run `Run-SystemTests -Architecture monolith` (latest suite) and `Run-SystemTests -Architecture monolith -Legacy` (legacy suite). Do not substitute `dotnet test` — `Run-SystemTests.ps1` is the only supported entry point because it manages containers and config.
- Fix any failures before moving on.
- Commit .NET changes as one logical commit (or a small series if the .NET work groups into distinct concerns such as scenario-DSL cleanup vs. driver-port additions).
