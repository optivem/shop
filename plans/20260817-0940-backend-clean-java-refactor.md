# 2026-08-17 09:40:29 UTC — Refactor backend-clean-java to clean architecture

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

## ▶ Next executable step (resume here)

Step 1 — capture the green baseline. In `system/multitier/backend-clean-java`, run `./gradlew build` and `./gradlew componentTest`; record the passing counts in this plan under Step 1. No code change, no commit. That number is the invariant every later step is measured against, and Step 3's restructure is only "done" when it reproduces it exactly.

All design questions are settled (see **Decisions**) — nothing blocks execution from here.

## Steps

- [ ] **Step 1 — Baseline.** Run `./gradlew build` + `./gradlew componentTest` in `system/multitier/backend-clean-java`; record the passing counts here. No code change. This is the safety net the whole refactor leans on.
- [ ] **Step 2 — Make the rule executable.** Add an ArchUnit dependency-rule test to `src/test` encoding the target shape: `domain` depends on no framework at all; `usecases` depends only on `domain`, **plus one explicitly-named exception for `jakarta.validation` on the request DTOs** (per Q9 — allowed by name, so it reads as a decision rather than an oversight); `presentation` depends on `usecases` + `domain`; Spring, `jakarta.persistence` and Jackson are confined to `infrastructure` and `presentation`. Start it scoped to the packages that already comply and widen it as each step lands — the test is the definition of done, not a review checklist.
- [ ] **Step 3 — Carve the four packages (move, don't change).** Pure restructure into `presentation` / `usecases` / `domain` / `infrastructure`. Package declarations and imports only, zero logic edits. `Order`/`Coupon` still carry their JPA annotations and `ErpGateway` is still a concrete HTTP class at the end of this step — Steps 4 and 5 fix that. The shape becomes visible immediately, and the component suite must reproduce Step 1's counts exactly.

  <details><summary>Target package map</summary>

  ```
  com.mycompany.myshop.backend
  ├── BackendApplication
  ├── presentation/     controller/{Order,Coupon,Health}Controller · exception/GlobalExceptionHandler · config/{Cors,OpenApi}Config
  ├── usecases/         order/{PlaceOrder,CancelOrder,DeliverOrder,ViewOrderDetails,BrowseOrderHistory}
  │                     coupon/{PublishCoupon,BrowseCoupons} · dtos/*Request, *Response
  ├── domain/           entities/{Order,Coupon,OrderStatus,Product,Promotion,TaxRate}  (POJOs)
  │                     values/{Money,Rate}                    (Step 8)
  │                     repositories/{Order,Coupon}Repository   (plain interfaces)
  │                     gateways/{Erp,Tax,Clock}Gateway         (plain interfaces, domain return types)
  │                     exceptions/{Validation,NotExistValidation,TaxGateway}Exception
  └── infrastructure/   persistence/{entities/*JpaEntity, repositories/*JpaRepository, adapters/*RepositoryAdapter, mappers/*}
                        external/{erp,tax,clock}/{Http*Gateway + wire DTOs}
                        config/  (bean wiring for the use cases)
  ```
  </details>

- [ ] **Step 4 — Split the gateway interfaces from their HTTP implementations.** `ErpGateway` / `TaxGateway` / `ClockGateway` become plain interfaces in `domain/gateways` returning **domain types** — `Optional<Product>`, `Promotion`, `Optional<TaxRate>`, `Instant`. The current `@Service` HTTP classes become `HttpErpGateway` / `HttpTaxGateway` / `HttpClockGateway` in `infrastructure/external`, keeping the wire DTOs from `core/dtos/external` and mapping wire → domain. Watch the `Optional.empty()`-means-404 convention in `getProductDetails` / `getTaxDetails`: it currently encodes "absent" as an HTTP status check, and that translation belongs in the adapter now.
- [ ] **Step 5 — Split the domain entities from the JPA entities.** `domain/entities/Order` and `Coupon` become POJOs with no `jakarta.persistence` and no Lombok; `infrastructure/persistence/entities/{Order,Coupon}JpaEntity` carry the ORM mapping; `*JpaRepository` stay Spring Data; `*RepositoryAdapter` implement the plain `domain/repositories` interfaces by mapping between the two. Table and column names are untouched — the schema is a fixed point.
- [ ] **Step 6 — One class per use case.** Split `OrderService` into `PlaceOrder`, `CancelOrder`, `DeliverOrder`, `ViewOrderDetails`, `BrowseOrderHistory`; split `CouponService` into `PublishCoupon`, `BrowseCoupons`. `getDiscount` / `incrementUsageCount` are not use cases — they are coupon behaviour called by `PlaceOrder`, and Step 8 moves them onto the `Coupon` entity. Each use case takes and returns its own DTO from `usecases/dtos`.
- [ ] **Step 7 — Point the controllers at the use cases.** `OrderController` / `CouponController` inject use case classes instead of services, and the response-shaping loops currently split across `CouponController.browseCoupons` (in the controller) and `OrderService.browseOrderHistory` (in the service) both move into their use case — since D9 puts the response DTOs in `usecases`, that is where domain → DTO mapping belongs, and the inconsistency disappears. JSON field names, paths and status codes must not shift by a byte.
- [ ] **Step 8 — Push behaviour into the domain.** Four moves, each removing a block of logic from a use case:
  - **`Order.cancel()` / `Order.deliver()`** enforce the status transitions currently written as `if (order.getStatus() != …) throw` in `OrderService`.
  - **`Coupon`** owns its validity window, usage limit and `redeem()` — absorbing `CouponService.getDiscount`'s four validation branches and `incrementUsageCount`.
  - **`Money` + `Rate` value objects** own the rounding and the arithmetic: `base.applyRate(…)`, `promoted.minus(discount)`, `subtotal.plus(tax)` replace eight loose `BigDecimal` locals. Same persisted `DECIMAL(10,2)` / `DECIMAL(5,4)` precision as today.
  - **A calendar policy** for the two December-31st blackout windows (the 23:59 placement block and the 22:00–22:30 cancellation block), which are currently duplicated `MonthDay`/`LocalTime` blocks in two methods.
- [ ] **Step 9 — De-frameworkify the inside.** Remove Lombok and Spring stereotypes from `domain` and `usecases`; wire the use case classes as explicit beans in `infrastructure/config` instead of `@Service`-scanning them. Widen the Step 2 ArchUnit rule to its full form — it must now pass unscoped.
- [ ] **Step 10 — Domain unit tests.** Add fast, Spring-free tests in `src/test` for pricing, coupon validity, the order state machine and the blackout policy. These are the tests the CRUD variant structurally cannot have.
- [ ] **Step 11 — Per-variant contract + narrow-integration layers.** The README defers these precisely because their subject is an adapter; now that the adapters exist, add `contractTest` / `integrationTest` source sets for `Http*` gateway adapters and the persistence adapters, mirroring `backend-java`'s opt-in wiring in `build.gradle`.
- [ ] **Step 12 — Answer the sharing question.** Diff `src/testSupport` and `src/componentTest` against `backend-java`. If the fork is near-zero, replace the copies with borrowed source roots per the snippet already in the README; if it is not, record *what* forked and why — that is the more interesting finding.
- [ ] **Step 13 — Update the README.** Replace the "Status: verbatim CRUD copy" section with the realised architecture, a package-map table, and the measured answer from Step 12.

## Decisions

All nine questions raised during drafting are settled. Recorded here because each one shows up in
every import line, and a later reader will want the *why*, not just the shape.

- **D1 — Package vocabulary:** `presentation` / `usecases` / `domain` / `infrastructure`.
- **D2 — Where the interfaces live:** in `domain` — repository interfaces and gateway interfaces both, as plain Java interfaces.
- **D3 — Component tests are hard off-limits.** Not one line of `src/testSupport` or `src/componentTest` changes, for the whole refactor. `git diff --stat src/testSupport src/componentTest` must come back empty at every commit. An edit that seems necessary is evidence the refactor broke an observable contract — fix the production code, not the test.
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
