# .NET — System Test Alignment Plan

Reference report: `reports/20260417-1841-compare-tests-both.md`
Reference implementation: **Java** (align .NET to Java unless otherwise noted).

Execute the tasks below in order. Each task names the concrete target file(s) and the Java reference file to copy behavior from.

---

## 1. Architecture — Clients Layer: rename Tax DTO

Target file:
- `system-test/dotnet/Driver.Adapter/External/Tax/Client/Dtos/ExtCountryDetailsResponse.cs` → rename to `ExtGetCountryResponse.cs`.

Reference:
- Java: `system-test/java/src/main/java/com/optivem/shop/testkit/driver/adapter/external/tax/client/dtos/ExtGetCountryResponse.java`
- TypeScript: `system-test/typescript/src/testkit/driver/adapter/external/tax/client/dtos/ExtGetCountryResponse.ts`

Changes:
1. Rename the file.
2. Rename the class `ExtCountryDetailsResponse` → `ExtGetCountryResponse` inside the file.
3. Update every `using`, type reference, and `new ExtCountryDetailsResponse(...)` usage across `system-test/dotnet/` (in particular `Driver.Adapter/External/Tax/Client/TaxRealClient.cs`, `TaxStubClient.cs`, `BaseTaxClient.cs`, and anywhere else it is deserialized).

## 2. Architecture — Driver Port / DSL Core: relocate `SystemResults`

Target file:
- `system-test/dotnet/Driver.Port/Shop/SystemResults.cs` → move to `system-test/dotnet/Dsl.Core/UseCase/Shop/Commons/SystemResults.cs`.

Reference:
- Java: `system-test/java/src/main/java/com/optivem/shop/testkit/dsl/core/usecase/shop/commons/SystemResults.java`
- TypeScript: `system-test/typescript/src/testkit/dsl/core/usecase/shop/commons/system-results.ts`

Changes:
1. Create the destination directory `system-test/dotnet/Dsl.Core/UseCase/Shop/Commons/`.
2. Move the file; update the namespace from `Driver.Port.Shop` to `Dsl.Core.UseCase.Shop.Commons` (pick the namespace that matches the existing `Dsl.Core.UseCase.Shop.*` convention — check neighboring files).
3. If the project layout puts this under the `Dsl.Core.UseCase.Shop` csproj, ensure `Dsl.Core.UseCase.Shop.csproj` compiles the new file.
4. Update every `using Driver.Port.Shop;` consumer that referenced `SystemResults` to the new namespace.

## 3. Legacy Tests — mod11 Tax contract trio

Target files (new):
- `system-test/dotnet/SystemTests/Legacy/Mod11/ExternalSystemContractTests/Tax/BaseTaxContractTest.cs`
- `system-test/dotnet/SystemTests/Legacy/Mod11/ExternalSystemContractTests/Tax/TaxRealContractTest.cs`
- `system-test/dotnet/SystemTests/Legacy/Mod11/ExternalSystemContractTests/Tax/TaxStubContractTest.cs`

Reference:
- Java: `system-test/java/src/test/java/com/optivem/shop/systemtest/legacy/mod11/contract/tax/BaseTaxContractTest.java`, `TaxRealContractTest.java`, `TaxStubContractTest.java`
- .NET latest equivalents (for shape): `system-test/dotnet/SystemTests/Latest/ExternalSystemContractTests/Tax/BaseTaxContractTest.cs`, `TaxRealContractTest.cs`, `TaxStubContractTest.cs`

Behavior:
- `BaseTaxContractTest.cs`: abstract, inherits from `SystemTests.Legacy.Mod11.ExternalSystemContractTests.Base.BaseExternalSystemContractTest`, defines `[Fact] public async Task ShouldBeAbleToGetTaxRate()` body:
  ```
  (await Scenario()
      .Given().Country().WithCode("US").WithTaxRate(0.09m)
      .Then().Country("US"))
      .HasTaxRateIsPositive();
  ```
- `TaxRealContractTest.cs`: inherits `BaseTaxContractTest`, overrides `FixedExternalSystemMode => ExternalSystemMode.Real`.
- `TaxStubContractTest.cs`: inherits `BaseTaxContractTest`, overrides `FixedExternalSystemMode => ExternalSystemMode.Stub`.

Use the namespace `SystemTests.Legacy.Mod11.ExternalSystemContractTests.Tax`. Verify `SystemTests.csproj` picks up the new files (it should via globbing — confirm after first compile).

---

## Local verification & commit

From `system-test/dotnet/`:

1. Run the latest suite:
   ```
   ./Run-SystemTests.ps1 -Architecture monolith
   ```
2. Run the legacy suite:
   ```
   ./Run-SystemTests.ps1 -Architecture monolith -Legacy
   ```
3. Fix any failures before proceeding. Do not substitute raw `dotnet test` — `Run-SystemTests.ps1` is the only supported entry point because it manages containers and configuration.
4. Commit the changes as a single logical commit:
   ```
   Align .NET testkit to Java: rename ExtGetCountryResponse, relocate SystemResults, add mod11 tax contract tests
   ```
