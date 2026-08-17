# 2026-08-17 09:40:29 UTC — Refactor backend-clean-java to clean architecture

🤖 **Picked up by agent** — `Valentina_Desk` at `2026-08-17T12:09:00Z`

## TL;DR

**Why:** `system/multitier/backend-clean-java/src/main` is currently a verbatim copy of `backend-java`'s CRUD/layered implementation — Spring/JPA/Jakarta annotations reach all the way into `core`, business logic lives in two fat `@Service` transaction scripts, and `core.dtos` doubles as both the HTTP wire contract and the service-layer boundary. The variant exists to demonstrate the *other* way of arranging the same behaviour.
**End result:** The same HTTP contract, the same DB schema, and the same component-test suite, running against a `src/main` organised by the dependency rule: a framework-free domain at the centre, one interactor per use case around it, and every framework concern (web, JPA, HTTP gateways) pushed out to adapters behind ports. An ArchUnit suite makes the dependency rule executable rather than aspirational.

## Outcomes

What we get out of this — the goals and deliverables:

- **`src/main` reorganised by the dependency rule**, not by technical layer — four top-level packages under `com.mycompany.myshop.backend`, dependencies pointing inward only:

  | Package | Holds | May depend on |
  |---|---|---|
  | `presentation` | REST controllers, the global exception handler, web config | `usecases`, `domain` |
  | `usecases` | one class per use case, plus the request/response DTOs they take and return | `domain` |
  | `domain` | entities as **POJOs** (no ORM annotations), plain repository interfaces (no Spring Data), gateway interfaces (`ErpGateway`, `TaxGateway`, `ClockGateway`), domain exceptions | nothing |
  | `infrastructure` | JPA entities, Spring Data repositories, repository adapters, gateway adapters + their wire DTOs, bean wiring | all of the above |
- **A domain with behaviour, not just fields.** `Order` owns its own state machine (`cancel()`, `deliver()`), `Coupon` owns validity + redemption, pricing arithmetic lives in a domain policy — instead of all of it sitting inline in `OrderService.placeOrder`.
- **Zero framework imports in `domain`.** No `org.springframework.*`, no `jakarta.persistence.*`, no Lombok — verified by test, not by review. `Order` and `Coupon` become plain POJOs; the ORM mapping moves to separate JPA entities in `infrastructure`.
- **One class per use case.** `PlaceOrder`, `CancelOrder`, `DeliverOrder`, `ViewOrderDetails`, `BrowseOrderHistory`, `PublishCoupon`, `BrowseCoupons` replace the two grab-bag services, each owning the request/response DTO it takes and returns.
- **Interfaces owned by the inside, implementations owned by the outside.** `OrderRepository`, `CouponRepository`, `ErpGateway`, `TaxGateway`, `ClockGateway` are plain interfaces declared in `domain`; the Spring Data repositories, JPA entities and HTTP clients implement them from `infrastructure` via adapters.
- **The HTTP contract and the DB schema are byte-identical to `backend-java`.** Same paths, same status codes, same JSON field names, same `orders`/`coupons` tables — the wire is a fixed point of the refactor.
- **The component suite stays green at every commit** and is never edited to accommodate the refactor. If a component test has to change, that is a defect in the refactor, not in the test.
- **The README's open measurement is answered.** After the refactor, diff `src/testSupport` + `src/componentTest` against `backend-java`: near-zero fork means the boundary types were right and the copies can be replaced with borrowed source roots.
- **Fast domain unit tests become possible** — `src/test` can exercise pricing, coupon validity and the order state machine with no Spring context and no Docker.

## Baseline (Step 1 — captured 2026-08-17)

`./gradlew build componentTest` on `Valentina_Desk`, Docker Engine 29.5.2 — **BUILD SUCCESSFUL in 41s**:

| Task | Tests | Passed | Failed | Skipped |
|---|---|---|---|---|
| `test` (unit — `OrderServiceTest`) | 10 | 10 | 0 | 0 |
| `componentTest` | 62 | 62 | 0 | 0 |

**62 component tests, all green** is the invariant — that count may never move. The unit count is a
floor, not a fixed point: it only grows as the refactor makes the centre testable (Step 9 took it to
18, Step 10 to 58). See the resume block for the current gate.

## Test-side fork, measured (early answer to Step 12)

Steps 3–5 forced the D3 revision below, and the resulting delta across `src/testSupport` +
`src/componentTest` is **31 files, 60 insertions, 60 deletions — and every single one is mechanical**:

| Kind of change | Count | Where |
|---|---|---|
| Import lines re-pointed at the new packages | 49 | 29 files |
| Injected field types (`OrderJpaRepository`, `CouponJpaRepository`, `HttpErpGateway`, `HttpTaxGateway`) | 4 | `BaseComponentTest` |
| Constructor param + delegate call (`fetchProductDetails` / `fetchTaxDetails`) | 6 | `SutErpReader`, `SutTaxReader` |
| Javadoc `{@link}` targets following the rename | 2 | `SutErpReader`, `SutTaxReader` |
| **Assertions / scenarios / expectations** | **0** | — |

Verified with `git diff -U0 src/testSupport src/componentTest | grep -v '^[+-]import'` — the full
non-import delta is 12 lines. So the answer Step 12 was going to look for is **the fork is
near-zero and purely mechanical**: the boundary types were right. Two caveats for Step 12 to
settle: the harness names four production internals it would not need if it went through the HTTP
contract alone, and `Sut*Reader` deliberately reaches for the concrete adapter to keep reading the
wire DTO.

## ▶ Next executable step (resume here)

Step 11 — the per-variant contract + narrow-integration source sets. `build.gradle` already carries
the `testSupport` + `componentTest` opt-in wiring (source sets, `extendsFrom` chain, a `Test` task
off `build`, `checkstyleAll`); mirror it for `contractTest` and `integrationTest` against
`backend-java`'s equivalents. The subjects that now exist and did not before: the four
`infrastructure/external/**` `Http*Gateway` adapters (contract) and the two
`infrastructure/persistence/adapters/*RepositoryAdapter` + their mappers (narrow integration).

Gate for every step from here: `./gradlew build componentTest` must stay at **58 unit tests, all
passed** — Step 10 added 40 domain tests to the 18 that Step 9 left — **plus 62 component tests, all
green**, and the delta in `src/testSupport` / `src/componentTest` must stay at zero lines.

All design questions are settled (see **Decisions**).

## Steps

- [ ] **Step 11 — Per-variant contract + narrow-integration layers.** The README defers these precisely because their subject is an adapter; now that the adapters exist, add `contractTest` / `integrationTest` source sets for `Http*` gateway adapters and the persistence adapters, mirroring `backend-java`'s opt-in wiring in `build.gradle`.
- [ ] **Step 12 — Answer the sharing question.** Diff `src/testSupport` and `src/componentTest` against `backend-java`. If the fork is near-zero, replace the copies with borrowed source roots per the snippet already in the README; if it is not, record *what* forked and why — that is the more interesting finding.
- [ ] **Step 13 — Update the README.** Replace the "Status: verbatim CRUD copy" section with the realised architecture, a package-map table, and the measured answer from Step 12.

## Decisions

All nine questions raised during drafting are settled. Recorded here because each one shows up in
every import line, and a later reader will want the *why*, not just the shape.

- **D1 — Package vocabulary:** `presentation` / `usecases` / `domain` / `infrastructure`.
- **D2 — Where the interfaces live:** in `domain` — repository interfaces and gateway interfaces both, as plain Java interfaces.
- **D3 — Component test *behaviour* is hard off-limits; *plumbing* may move.** *(Revised 2026-08-17
  during execution — the original "not one line changes" was found to be unsatisfiable: 33 import
  lines across 22 files in `testSupport`/`componentTest` bind to `src/main` types by package name, so
  Step 3's carve breaks them before any logic changes, and D8 removes the wire DTOs that
  `SutErpReader`/`SutTaxReader` hand to their assertions.)*

  - **Frozen:** no scenario, no expectation, no assertion semantics. The count stays **62, all
    green**, at every commit. A change to what a test *asserts* is still evidence the refactor broke
    an observable contract — fix the production code, not the test.
  - **Permitted:** import lines, and the constructor/field types at the five points where the harness
    names a production internal (`BaseComponentTest`'s repository + gateway injections, the three
    `Sut*Reader` constructors). These bind to internals, not to the HTTP contract.
  - **Consequence for D8:** the `Sut*Reader`s take the concrete `Http*Gateway` from
    `infrastructure/external` — which still returns the wire DTOs — rather than the domain gateway
    interface. This preserves their design intent exactly ("the stub's bytes travel through the SUT's
    real HTTP call + real parse") and keeps `ThenProductImpl`/`ThenCountryImpl` assertions
    byte-identical.
  - The resulting delta *is* the answer to Step 12's measurement question, gathered early.
- **D4 — Domain entity vs JPA entity: full split.** Domain entities are POJOs; JPA entities live in `infrastructure/persistence` and the repository adapters map across.
- **D5 — `Money` + `Rate` value objects land at Step 8.** The pricing chain becomes typed arithmetic owning its own rounding, replacing eight loose `BigDecimal` locals in `placeOrder`. Persisted precision is unchanged — `Money` maps to the same `DECIMAL(10,2)`. *(The collision flagged during drafting wasn't real: `plans/20260722-1216-string-only-money-surface.md` governs how **test DSLs** write money literals, not production types.)*
- **D6 — The variant stays commit-stage-only.** No VERSION, no Dockerfile, no docker-compose service, no `gh-optivem-*.yaml` entry, no release tag, no SonarCloud project — as the README documents today. The plan ends at Step 13; deployability is a non-goal, not a deferred item.
- **D7 — `deliverOrder` is live behaviour, preserve it.** Covered by three unit tests in `OrderServiceTest`; `GivenOrder.withStatus` documents that `DELIVERED` has no driver method as *"a deliberate non-goal of the plan that added cancellation"* — absent by decision, not neglect. Its status guard is one of the `if` blocks Step 8 moves onto `Order.deliver()`.
- **D8 — Domain gateway interfaces return domain types.** `ErpGateway` → `Optional<Product>` / `Promotion`, `TaxGateway` → `Optional<TaxRate>`, `ClockGateway` → `Instant`. Wire DTOs stay in `infrastructure/external` and the `Http*Gateway` adapters map wire → domain, so a supplier renaming a JSON field cannot reach the centre.
- **D9 — `usecases` request DTOs keep their Jakarta validation annotations.** `PlaceOrderRequest` keeps `@NotBlank` / `@Positive` / `@TypeValidationMessage`; the controller keeps `@Valid @RequestBody`. `usecases` therefore imports `jakarta.validation` — a scoped, deliberate exception that the Step 2 ArchUnit rule allows **by name**, so it reads as a decision rather than an oversight. A parallel set of near-identical web request classes was judged worse than the import. `TypeValidationMessageExtractor` is exception-handler plumbing and moves to `presentation`.
- ~~**Q6 — Does this variant stay commit-stage-only?**~~ **Resolved: yes, commit-stage-only.** No VERSION, no Dockerfile, no docker-compose service, no `gh-optivem-*.yaml` entry, no release tag, no SonarCloud project — exactly as the README documents today. The plan ends at Step 13; deployability is not a deferred item, it is a non-goal.
- ~~**Q8 — What do the domain gateway interfaces return?**~~ **Resolved: domain types.** `ErpGateway` returns `Optional<Product>` / `Promotion`, `TaxGateway` returns `Optional<TaxRate>`, `ClockGateway` returns `Instant`. The wire DTOs (`ProductDetailsResponse`, `GetPromotionResponse`, `TaxDetailsResponse`, `GetTimeResponse`) stay in `infrastructure/external` and the `Http*Gateway` adapters map wire → domain. A supplier renaming a JSON field must not reach the centre.
- ~~**Q9 — Do the `usecases` DTOs carry the Jakarta validation annotations?**~~ **Resolved: yes, keep them in `usecases`.** `PlaceOrderRequest` keeps `@NotBlank` / `@Positive` / `@TypeValidationMessage` and the controller keeps binding it with `@Valid @RequestBody`, exactly as today. `usecases` therefore imports `jakarta.validation` — an accepted, scoped exception to the framework-free rule, and the ArchUnit rule in Step 2 must allow it explicitly rather than by omission. Note `core/validation/TypeValidationMessageExtractor` is exception-handler plumbing and moves to `presentation`; the `@TypeValidationMessage` annotation itself travels with the DTOs.
- ~~**Q7 — Is `deliverOrder` in scope?**~~ **Resolved: live behaviour, preserve it.** It is covered by three unit tests in `OrderServiceTest` (`deliverOrderTransitionsStatusToDelivered`, `…ThrowsWhenOrderNotFound`, `…ThrowsWhenOrderAlreadyDelivered`), and `GivenOrder.withStatus` documents that `DELIVERED` has no driver method as *"a deliberate non-goal of the plan that added cancellation"* — absent by decision, not by neglect. Its status-transition guard is one of the `if` blocks Step 8 moves onto `Order.deliver()`.
