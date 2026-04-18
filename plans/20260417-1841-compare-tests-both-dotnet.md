# .NET — System Test Alignment Plan

Reference report: `reports/20260417-1841-compare-tests-both.md`
Reference implementation: **Java** (align .NET to Java unless otherwise noted).

---

- [ ] Item 2: Architecture — Driver Port / DSL Core: relocate `SystemResults` from `Driver.Port/Shop/` to `Dsl.Core/UseCase/Shop/Commons/` — ⏳ Deferred: moving `SystemResults` into the `Shop.csproj` project would create a circular project reference, since `Driver.Adapter` consumes it (in `ShopUiDriver.cs`, `BasePage.cs`) and `Shop.csproj` already references `Driver.Adapter`. Java/TS don't hit this because they're single-module. Revisit by either (a) inlining `SystemResults.*` calls in the two `Driver.Adapter` files and then moving, or (b) introducing a third shared project.
