# 2026-11-03 15:04:02 UTC — Money and rates are strings end to end (test DSLs + wire format)

**Origin:** Merges `20260722-1216-string-only-money-surface` (#3) and
`20260917-1928-money-wire-format-strings` (#5), per meta-plan `20261103-1021` consolidation **CS1**
(unit U6). Both source plans are deleted; their content lives here. Grounded against `main` at
`e13588d3` (after U1 optimistic locking and U5 rounding / 500 text).

## TL;DR

**Why:** Money and rates exist in two surface forms in the Java test DSLs (`String` and `double`), and
as bare JSON numbers on the wire in all seven backends. The `double` form loses the author's decimal
before the DSL sees it, and the number on the wire only looks fixed-scale because `BigDecimal`/`decimal`
scale survives the DB round trip; nothing enforces it. TypeScript has no exact decimal, so the
recorded cross-language direction is string-canonical.
**End result:** Every decimal field (amounts and rates) is a string: on the Java test DSL surface
(the `double` overloads and `Converter.fromDouble` are gone) and on the wire (`"20.00"`, `"0.2000"`), in
every backend and every consumer (Pact contract, three system-test suites, frontend-react and the three
monolith UIs). It lands as one coordinated change set, so `main` is never in a mixed state.

## Outcomes

- **One surface form in the Java test DSLs.** `system-test/java`, `backend-java` and
  `backend-clean-java` testkits accept money and rates only as `String`. A numeric literal such as
  `hasTotalPrice(120.00)` no longer compiles. Counts (`hasQuantity(int)`, `hasUsageLimit(int)`, ...)
  are untouched.
- **Money is a JSON string with explicit scale in all 7 backends.** Amounts are written at 2dp and
  rates at 4dp, `HALF_UP` (the same `MONEY_SCALE`/`RATE_SCALE` rule U5 made explicit in
  `OrderService`). The field-to-scale map is the one `backend-typescript` already uses
  (`DECIMAL_FIELDS`). Each backend has a test that pins the string shape.
- **The shared Pact contract records strings.** `contracts/frontend-backend.json` carries
  `"totalPrice": "22.00"` etc. with regex matchers that fix the scale, and provider verification is
  green in `backend-java`, `backend-clean-java`, `backend-dotnet` and `backend-typescript`.
- **Every consumer reads strings.** frontend-react types and guards are `string`, the three monolith
  UIs render from strings, and coupon publish requests send `discountRate` as a string. The text shown
  in the UI is unchanged, so UI system tests don't move.
- **System-test suites agree with the wire.** Java and .NET keep their exact decimal types internally
  (`BigDecimal`, `decimal`) and parse the strings. TypeScript DTOs become `string` and compare
  normalized decimal strings. The legacy raw-JSON tests (mod03–mod06) assert strings.
- **The rule is written down where it is read.** Javadoc on the three `ViewOrderVerification` classes,
  and the mirror plan #12's *Agreed target* now says "only surface: `String`", so its Step 3 ports one
  form, not two.

## ▶ Next executable step (resume here)

Step 1. Pre-flight first: `git status` (don't stage `system-test/java/.../wiremock/JsonWireMockClient.java`
or `deploy-config.yml`; they belong to another session), `git config core.hooksPath .githooks`,
`gh optivem system stop`, and the meta-plan's pre-execute grep for in-flight markers. Then convert the
67 numeric-literal call sites in the 14 `system-test/java/src/test` files listed in Step 1 to `String`
literals and stop at Step 1's gate. Steps 1–4 are Phase 1 (Java DSL); Steps 5–10 are Phase 2 (wire).
Nothing is pushed until Step 11.

## Steps

### Phase 1 — String-only Java test DSLs (#3 Items 1–5)

- [ ] Step 1 — **Convert `system-test/java` call sites to `String` literals (14 files, 67 sites:
  31 `with*` setup, 36 assertions).** Keep currency scale for amounts (`20.00` → `"20.00"`, `100` →
  `"100.00"`) and the written form for rates (`0.20` → `"0.20"`). Files under
  `system-test/java/src/test/java/com/mycompany/myshop/systemtest/`:
  `latest/acceptance/{PlaceOrderPositiveTest, PlaceOrderPositiveIsolatedTest, ViewOrderPositiveTest, PublishCouponPositiveTest, PublishCouponNegativeTest}.java`,
  `latest/contract/erp/BaseErpContractTest.java`, `latest/contract/tax/{BaseTaxContractTest, TaxStubContractTest}.java`,
  `legacy/mod07/e2e/PlaceOrderPositiveTest.java`, `legacy/mod08/e2e/PlaceOrderPositiveTest.java`,
  `legacy/mod10/acceptance/PlaceOrderPositiveIsolatedTest.java`,
  `legacy/mod11/contract/erp/BaseErpContractTest.java`, `legacy/mod11/contract/tax/{BaseTaxContractTest, TaxStubContractTest}.java`.
  Re-enumerate before editing, since counts drift:
  `grep -rEn '\.(has|with)?(UnitPrice|TotalPrice|BasePrice|SubtotalPrice|DiscountAmount|TaxRate|TaxAmount|DiscountRate|Price|Discount|unitPrice|totalPrice|subtotalPrice|basePrice|discountAmount|taxRate|taxAmount|discountRate|price|couponHasDiscountRate)\(-?[0-9]' system-test/java/src/test`.
  Gate: `./gradlew compileTestJava` in `system-test/java`.
- [ ] Step 2 — **Convert the numeric outliers in the backend testkits (4 files, 8 sites).**
  `system/multitier/backend-java/src/contractTest/java/com/mycompany/myshop/backend/contract/external/erp/ErpStubConsumabilityContractTest.java`
  and `.../external/tax/TaxStubConsumabilityContractTest.java` (`withUnitPrice(10.00)`, `hasPrice(10.00)`,
  `withTaxRate(0.09)`, `hasTaxRate(0.09)`), plus the identical pair in
  `system/multitier/backend-clean-java/src/contractTest/...`. (#3 placed these in `componentTest`; they
  have since moved to `contractTest`, and clean-java has its own copy.) Gate: compile in each project.
- [ ] Step 3 — **Delete the numeric overloads and `Converter.fromDouble` (64 files).** The compiler is the
  safety net: if Steps 1–2 missed a site, it names it.
  - `system-test/java/src/main/java/com/mycompany/myshop/testkit/` (26 files): the `double` overloads in
    `dsl/port/given/steps/{GivenCountry, GivenCoupon, GivenProduct, GivenPromotion}`,
    `dsl/port/then/steps/{ThenOrder, ThenProduct, ThenCountry, ThenCoupon}`,
    `dsl/port/when/steps/WhenPublishCoupon` and their `*Impl`s under `dsl/core/scenario/{given,then,when}/steps/`;
    the verification and stub use-cases `dsl/core/usecase/usecases/{ViewOrderVerification, BrowseCouponsVerification}`,
    `dsl/core/usecase/external/erp/usecases/{GetProductVerification, ReturnsProduct, ReturnsPromotion}`,
    `dsl/core/usecase/external/tax/usecases/{GetTaxVerification, ReturnsTaxRate}`. In `common/Converter.java`,
    delete `fromDouble(double)` and `toBigDecimal(double)` (only the overloads call them) and the
    already-unused `toDouble(String)`. The `BigDecimal` verification forms stay, because they are the
    internal canonical type.
  - `system/multitier/backend-java/src/testSupport/.../testkit/` (19 files): the same port/Impl overloads,
    including the six added in `85c5b65c`. Delete `common/Converter.java` entirely, since `fromDouble` is
    its only member.
  - `system/multitier/backend-clean-java/src/testSupport/.../testkit/` (19 files): same as backend-java;
    its `Converter.java` also holds only `fromDouble`, so delete it. (Not in #3, because clean-java's
    testkit came later.)
  - Gate: `./gradlew build` in `system-test/java`, then
    `gh optivem component-test run -c gh-optivem-multitier-java.yaml` and
    `gh optivem component-test run -c gh-optivem-multitier-clean-java.yaml` (unit, integration,
    component, contract and checkstyle).
- [ ] Step 4 — **Write the rule down, re-run the census, commit Phase 1 locally (3 files + this plan).**
  Add Javadoc to `ViewOrderVerification` in `system-test/java`, `backend-java` and `backend-clean-java`
  (`.../dsl/core/usecase/usecases/ViewOrderVerification.java`): money and rates are written as `String`
  literals, and there is no numeric form. Re-run Step 1's grep over all three trees and over
  `src/main`/`src/testSupport` for `(double ` and `fromDouble`: expect zero hits outside non-money code.
  Check that `hasQuantity`/`hasUsageLimit`/`hasUsedCount` are still `int`. Enumerate; don't eyeball.
  Gates: `./compile-all.sh`, then `--sample` for `gh-optivem-monolith-java.yaml` and
  `gh-optivem-monolith-java-legacy.yaml` (latest and legacy call sites both changed). Commit locally with
  a descriptive message, and **don't push**: Phase 2 follows in the same session.

### Phase 2 — Money on the wire becomes strings (#5)

Build Steps 5–9 in the working tree and commit them together at Step 10. Between those steps the
backends and consumers are deliberately out of step, so run only per-project gates until Step 10.

- [ ] Step 5 — **Java backends emit strings (≈9 files).** Add a Jackson module, registered through
  Spring's `ObjectMapper` (a `Jackson2ObjectMapperBuilderCustomizer` or `@JsonComponent`), with a
  `BigDecimal` serializer that writes a JSON string. The scale comes from the property name, using the TS
  `DECIMAL_FIELDS` map (amounts 2, rates 4, `HALF_UP`); any other `BigDecimal` is written as
  `toPlainString()`. Keep it off the DTOs: clean-java's response records live in `usecases`, and
  `ArchitectureTest.JACKSON_IS_CONFINED_TO_THE_OUTSIDE` forbids Jackson there, so the module goes in
  `presentation` (clean-java) or `config` (the other two). The outbound ERP/Tax/Clock gateways build
  their own `new ObjectMapper()` and are unaffected; check this per backend.
  - `system/monolith/java`: the module, plus a `@JsonTest`/MockMvc test asserting
    `"totalPrice":"22.00"`, `"discountRate":"0.2000"`. The UI templates
    `src/main/resources/templates/{order-details, order-history, admin-coupons}.html` replace
    `x.toFixed(2)` with the string for amounts and `(Number(rate) * 100).toFixed(2)` for rates. The
    publish form sends `discountRate` as the input's string value.
  - `system/multitier/backend-java`: the module and test.
  - `system/multitier/backend-clean-java`: the module and test. Also covers the
    `/api/reports` money fields (`ViewSalesReport{RevenueByCountryMonth, TopSku, CouponEffectiveness}Response`:
    `subtotalPrice`, `taxAmount`, `totalPrice`, `discountAmount`), which are already in the map.
  - Gate: `./gradlew build` per project. Pact verification is expected to fail until Step 8.
- [ ] Step 6 — **.NET backends emit strings (≈9 files).** Add a string writer for `decimal` response
  properties with the same name-to-scale map, e.g. a `DefaultJsonTypeInfoResolver` modifier or
  per-property `[JsonConverter]` on `Core/Dtos/{ViewOrderDetailsResponse, BrowseOrderHistoryResponse, BrowseCouponsResponse}.cs`.
  Register it in both `Microsoft.AspNetCore.Mvc.JsonOptions` and `Http.Json.JsonOptions` in
  `Program.cs`, next to `JsonStringEnumConverter`. **Don't change how requests are read:**
  `PublishCouponRequest.DiscountRate` is `decimal?` and today accepts both `"0.2"` and `0.2` through
  the Web defaults (`AllowReadingFromString`); a global `JsonConverter<decimal>` would also take over
  reads, so either scope the writer to the response types or keep its `Read` identical, so that the
  `"abc"` negative tests still produce the same error. Check that the ERP/Tax `HttpClient` reads don't
  pick it up.
  - `system/monolith/dotnet`: writer, `Program.cs`, a `ShopWebApplicationFactory`-based test pinning the
    shape (`Tests/`), plus `Pages/{OrderDetails, OrderHistory, AdminCoupons}.cshtml` (the same display
    and request change as Step 5).
  - `system/multitier/backend-dotnet`: writer, `Program.cs`, a test in `Tests/Component`.
  - Gate: `dotnet build` per project.
- [ ] Step 7 — **TypeScript backends emit strings (≈9 files).** Drop the sentinel-quote/regex trick, which
  exists only to strip the quotes, and emit `decimal.toFixed(scale, ROUND_HALF_UP)` as a plain string
  value.
  - `system/multitier/backend-typescript`: `src/api/interceptor/decimal-format.interceptor.ts`, plus a
    new spec for it. `test/component/place-order.component.spec.ts` switches from
    `Number(x)).toBeCloseTo(...)` to exact string asserts (`'20.00'`) and types its response
    fields as `string`. `test/component/coupon.component.spec.ts` sends `discountRate: '0.2'`.
  - `system/monolith/typescript`: `src/lib/decimal-format.ts` and `src/__tests__/decimal-format.spec.ts`;
    `src/lib/api-types.ts` (the zod schemas `z.number()` → `z.string()` for the 8 order fields, the
    history `totalPrice` and the coupon `discountRate`); `src/app/{order-details, order-history, admin-coupons}/page.tsx`
    (display as in Step 5; the publish request sends `discountRate` as the string). The Next.js
    pages are this monolith's own UI, so they move with it.
  - Gate: `npx tsc --noEmit` and `npm test` per project.
- [ ] Step 8 — **frontend-react and the Pact contract (24 files).**
  - Types and runtime: `src/types/api.types.ts`, `src/types/api.guards.ts` (`isNumber` → `isString`
    for the 10 money/rate fields), `src/features/orders/{OrderDetailView, OrderHistoryTable}.tsx`,
    `src/features/coupons/{CouponTable, CouponForm}.tsx`, `src/hooks/useCoupons.ts` (drop
    `Number.parseFloat`), `src/services/coupon-service.ts` (`discountRate: string`). The rendered
    text must stay identical (`$20.00`, `20.00%`).
  - Consumer tests and fixtures: `src/test/interactions/{order, coupon}.interactions.ts` (replace
    `decimal(…)` with `regex` matchers that fix the scale, `^-?\d+\.\d{2}$` for amounts and
    `^-?\d+\.\d{4}$` for rates, with string examples; the publish-coupon request body carries a string
    `discountRate`), `src/test/support/{backend-stub-dsl, frontend-dsl}.ts`,
    `src/test/support/driver/adapter/{gateway-frontend-driver.ts, ui-frontend-driver.tsx}`,
    `src/test/support/driver/port/{frontend-driver.ts, dtos/OrderDetailExpectation.ts, dtos/OrderHistoryRowExpectation.ts}`,
    `src/test/latest/component/{browse-order-history, view-order}.component.test.tsx`,
    `src/test/legacy/component/{coupon, order}.component.test.tsx`,
    `src/test/legacy/integration/coupon.integration.test.ts`, `src/test/unit/order-history-customer.unit.test.tsx`.
  - Regenerate `contracts/frontend-backend.json` with `npm run test:pact`, and review `git diff contracts/`:
    only money/rate values and matchers should change.
  - Gates: `npx tsc --noEmit`, `npm test`, `npm run test:component`, `npm run test:pact` in frontend-react.
    Then provider verification in all four Pact providers: `gh optivem component-test run -c` with
    `gh-optivem-multitier-java.yaml`, `gh-optivem-multitier-clean-java.yaml`,
    `gh-optivem-multitier-dotnet.yaml` and `gh-optivem-multitier-typescript.yaml`.
- [ ] Step 9 — **System-test suites read strings (≈18 files).**
  - Java: the DTOs stay `BigDecimal` (Jackson reads `"20.00"` into `BigDecimal`). Change
    `src/test/java/.../legacy/mod03/e2e/PlaceOrderPositiveApiTest.java` from `asDouble()` to
    `asText()` (`"20.00"`, `"100.00"`), so the raw-JSON lesson shows the real wire. `mod04`+ already
    compare `new BigDecimal("20.00")`, which still holds because the scale is explicit.
  - .NET: the DTOs stay `decimal` (`JsonHttpClient` has `AllowReadingFromString`). Fix
    `SystemTests/Legacy/Mod03/E2eTests/PlaceOrderPositiveApiTest.cs`, where `GetDecimal()` throws on a
    JSON string, to `GetString()` with `"20.00"`/`"100.00"`.
  - TypeScript (`system-test/typescript/src/testkit/`): `driver/port/dtos/{ViewOrderResponse, BrowseOrderHistoryResponse, BrowseCouponsResponse}.ts`
    → `string`; add a normalized decimal-string compare (strip trailing zeros, value-equal) to
    `common/converter.ts`, and use it in `dsl/core/scenario/then/{then-view-order, then-place-order, then-browse-coupons}.ts`
    and `dsl/core/usecase/usecases/{ViewOrderVerification, BrowseCouponsVerification}.ts` in place of
    `toBe(Number(x))`. The UI channel (`driver/adapter/ui/client/pages/{OrderDetailsPage, CouponManagementPage}.ts`,
    `driver/adapter/ui/my-shop-ui-driver.ts`) produces strings: `"$20.00"` → `"20.00"`, `"20.00%"` →
    `"0.2000"` by shifting the decimal point, not by float division. Legacy:
    `tests/legacy/mod03/e2e/place-order-positive-api-test.spec.ts`, `mod04/e2e/place-order-positive-api-test.spec.ts`,
    `mod05/e2e/place-order-positive-base-test.ts`, `mod06/e2e/place-order-positive-test.spec.ts`
    (`toBe(20)` → `toBe('20.00')`; `toBeGreaterThan(0)` on `Number(...)`). The TS DSL's
    `string | number` surface stays (see Decisions).
  - Leave the external ERP/Tax DTOs and stub drivers alone (`.../external/**`): that wire isn't ours.
  - Gates: `./gradlew build` (system-test/java), `dotnet build` (system-test/dotnet),
    `npx tsc --noEmit` and `npm run lint` (system-test/typescript).
- [ ] Step 10 — **Full verification, then commit Phase 2 as one commit.** Run `./compile-all.sh`, then the
  four `component-test run` configs from Step 8 again. Then `--sample`, serially, holding the docker
  lock, for each of the 12 configs: `gh-optivem-{monolith,multitier}-{java,dotnet,typescript}.yaml`
  and their `-legacy` twins (`GH_OPTIVEM_CONFIG=<cfg> gh optivem system start && gh optivem system-test setup && gh optivem system-test run --sample; gh optivem system stop`).
  Rebuild images with `--no-cache` if a UI test times out on `aria-label="SKU"`. All must pass.
  Commit Steps 5–9 and the regenerated contract as one commit with a descriptive message (never
  "Sync changes"; the pre-commit hook rejects that shape on `system/**`). Stage by path only.
- [ ] Step 11 — **Bookkeeping, then push the series together.**
  - Rewrite plan #12 (`plans/backlog/p3-test-parity/20260720-1055-backend-testkit-alignment-cross-language-mirror.md`).
    In *Agreed target*, "Preferred surface" becomes "Only surface: `String`". The "Numeric surface"
    row becomes "none (deleted in Java; .NET/TS port the deletion)". Replace the struck-through
    "Guidance, not enforcement" paragraph and its "⚠️ Under review" pointer with this plan's outcome.
    Step 3 changes from "additive only — nothing is deleted" to "port the String-only surface to
    .NET/TS, and delete their numeric overloads". Remove the "Blocked on U6" wording from its ▶ block.
    This replaces #3 Item 4's plan-to-plan edit.
  - Meta-plan `20261103-1021`: mark U6 done in Step M5, with commit hashes.
  - Commit the plan edits. Push Phase 1, Phase 2 and the bookkeeping commit together
    (`git pull --rebase`, then `git push`; if other unstaged changes block the rebase, cherry-pick onto
    `origin/main` in a temp worktree and push `HEAD:main`; never force-push). Then delete this plan
    file.

## Decisions

All resolved; the Q-B answers come from meta-plan `20261103-1021`, "User decisions (2026-11-03)".

- **Q-B.1 — accept the breaking change.** Delete the `double` DSL overloads and `Converter.fromDouble`.
  The DSL's shape is the lesson; carrying a discouraged second form to protect old clones teaches the
  wrong thing.
- **Q-B.2 — every decimal field is a string, rates included.** One rule, on the DSL surface and on the
  wire. That includes clean-java's `/api/reports` money fields and the frontend's publish-coupon request
  body; neither source plan listed them.
- **Q-B.3 — one coordinated change set.** Three local commits (Phase 1, Phase 2, bookkeeping) pushed
  together, so `main` never has a backend and a consumer that disagree. No transition window
  ("accept both on read") is built: the Pact contract is shared, and a window would be permanent
  complexity.
- **Q-B.4 — `--sample` system tests are pre-approved** (repo CLAUDE.md gate); there is no "ask before
  running system tests" stop.
- **Explicit scale, keyed by field name.** Amounts at 2dp, rates at 4dp, `HALF_UP`, from one map that
  mirrors the TS `DECIMAL_FIELDS` and the DB columns (`NUMERIC(10,2)`, `NUMERIC(5,4)`). This makes #5's
  "explicit scale" real, and it means the wire values are the ones today's backends already emit
  (`20.00`, `0.2000`), now quoted.
- **Internal canonical types don't change** (#3 non-goal): `system-test/java` stays `BigDecimal`,
  `backend-java`/clean-java testkits stay `String`, .NET stays `decimal`. TS system-test goes
  normalized-string because it has no exact decimal (the #12 *Agreed target*).
- **Enforcement of the wire shape:** Pact regex matchers for the multitier backends, plus one
  serialization test per backend (all 7; the monoliths have no Pact). The system-test clients stay
  lenient readers; they aren't the guard.
- **UI text doesn't change.** Amounts render the wire string; rates use `Number(rate) * 100` only to
  format a percentage (display, not arithmetic). UI system tests therefore need no change.
- **Request DTOs stay `String`/lenient** (#3 non-goal; the `PublishCouponRequest` negative-test reason).
- **Resolved by the merge:** #3 Item 4's edit to plan #12 is Step 11 here, and #5's open question
  "is this coordinated with the String-only DSL plan?" is answered by both phases living in one plan.

## Dropped as already done or obsolete

- #3 "Open question: is the breaking change acceptable?" and #5's three open questions and its "Decide
  the questions above" step: answered by Q-B.
- #3 Item 1 "the 6 variable-passing sites already pass `String` — verify, do not change": verified on
  2026-11-03. All 12 variable-passing sites in `system-test/java/src/test` take `String` parameters
  (`PlaceOrderPositiveTest`, `PublishCouponNegativeTest`), and no test method has a `double`
  parameter.
- #5's "Field surface is identical in every backend" stays true; nothing to do beyond the steps above.

## Open questions

None. All resolved.

## Out of scope

- **The .NET and TS test-DSL numeric surfaces** (`IThenOrder.HasUnitPrice(decimal)`, TS
  `hasUnitPrice(number)`, `string | number` params). They are ported by plan #12 Step 3, which this plan
  unblocks and rewrites (Step 11). #3 scoped .NET/TS out, and Phase 2 changes only what the wire forces.
- **External-system wire formats** (ERP/Tax/Clock simulators under `external-systems/`, their
  `Ext*Response` DTOs and stub drivers). Those mimic third-party APIs; #5's field list covers the shop
  API only.
- **Existing display inconsistencies** across UIs, e.g. .NET `AdminCoupons.cshtml` shows a coupon rate
  with `toFixed(0)` while Java shows `toFixed(2)`. Keep each as it is today.
- **Pagination / idempotency** (meta U7, #7 steps 1.4/1.5). It also edits `contracts/frontend-backend.json`
  and frontend-react (C3, C13), so it runs after this plan lands.
- `system-test/java/.../wiremock/JsonWireMockClient.java` (uncommitted work from another session at
  plan time). Don't stage it.
