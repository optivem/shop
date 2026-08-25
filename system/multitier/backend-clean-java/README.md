# Backend — Clean Architecture variant

An alternative implementation of the same backend as [`../backend-java`](../backend-java), structured
the clean-architecture way instead of the CRUD/layered way.

**Same HTTP contract. Same database schema. Same component tests.** Only `src/main` differs — and
that is the whole point: the acceptance-level specs do not change when the inside is rearranged.

## Running the tests

All commands run from this directory (`system/multitier/backend-clean-java`).

```bash
./gradlew build             # compile + 106 unit tests + checkstyle on main/test — no Docker
./gradlew componentTest     # 62 in-process component tests                      — Docker
./gradlew integrationTest   # 51 narrow-integration tests (real Postgres)        — Docker
./gradlew contractTest      # 43 Pact provider-verification tests                — Docker
./gradlew checkstyleAll     # lints every source set, including the opt-in ones
```

The whole commit-stage gate, which is what CI runs and what to run before committing:

```bash
./gradlew test componentTest integrationTest contractTest checkstyleAll
```

- Docker only has to be **running** — the suites start their own Postgres via Testcontainers. There
  is no stack to bring up or tear down, and no `gh optivem system start`.
- The three Docker suites are opt-in by design and are **not** part of `build`.
- The `*RealParityContractTest` classes also need the external-system simulator image, which is
  deliberately not a `dependsOn` (the other contract tests shouldn't pay for the build):
  `./gradlew externalSimulatorImage contractTest`.
- From the repo root, `gh optivem component-test run -c gh-optivem-multitier-clean-java.yaml` runs
  the component layer the way CI does. This variant has no `docker/**/systems.yaml`, so it has no
  system-test stack and is not covered by the repo-root `test-all.sh`.
- `./gradlew benchmark` is a measurement harness, not a verdict: minutes to run, Docker required,
  never part of `build`. See [`docs/theme2-measurements.md`](docs/theme2-measurements.md).

## Status

The refactor is complete. `src/main` is organised by the dependency rule rather than by technical
layer, and the evidence that behaviour survived is that the component suite was never edited to
accommodate any of it: **62 component tests, the same 62, green at every commit.**

| Suite | Tests | Needs Docker |
|---|---|---|
| `test` (unit — domain, use cases, ArchUnit) | 106 | no |
| `componentTest` | 62 | yes (Postgres) |
| `integrationTest` | 51 | yes (Postgres; the gateway half is in-process) |
| `contractTest` | 43 | yes (Postgres + the simulator image for real-parity) |

## Package map

Four top-level packages under `com.mycompany.myshop.backend`, dependencies pointing inward only:

| Package | Holds | May depend on |
|---|---|---|
| `presentation` | REST controllers, the global exception handler, web config | `usecases`, `domain` |
| `usecases` | one class per use case, plus the request/response DTOs they take and return | `domain` |
| `domain` | entities as POJOs (no ORM annotations), plain repository interfaces (no Spring Data), gateway interfaces, value objects (`Money`, `Rate`, and the external snapshots `Product`/`TaxRate`/`Promotion`), domain exceptions | nothing |
| `infrastructure` | JPA entities, Spring Data repositories, repository adapters, gateway adapters + their wire DTOs, bean wiring | all of the above |

The shape worth noticing:

- **`domain` has no framework imports at all.** No `org.springframework.*`, no
  `jakarta.persistence.*`, no Lombok — enforced by `ArchitectureTest`, not by review.
- **Interfaces are owned by the inside, implementations by the outside.** `OrderRepository`,
  `CouponRepository`, `ErpGateway`, `TaxGateway` and `ClockGateway` are plain interfaces in `domain`;
  the Spring Data repositories, JPA entities and HTTP clients implement them from `infrastructure`.
- **Gateways return domain types, and the wire DTOs are package-private.** `ErpGateway` hands back
  `Optional<Product>` / `Promotion`, not the ERP's JSON shape. `ProductDetailsResponse`,
  `GetPromotionResponse`, `TaxDetailsResponse` and `GetTimeResponse` are package-private classes
  reached only by private methods, so the supplier's field names cannot be *named* outside the one
  package that parses them — the compiler enforces the boundary, not the convention. Every one of
  them sets `ignoreUnknown = true` on top of that, so a supplier renaming or adding a field cannot
  reach the centre either.
  Test support obeys the same rule: `SutErpReader` and `SutTaxReader` read through `ErpGateway` /
  `TaxGateway` and assert on `Product` / `TaxRate`, exactly as `SutClockReader` always did. "Just for
  a test" is how a wire type becomes public, and a public wire type is how it reaches the domain.
- **The ports own their failures too.** `GatewayException` and its three per-system subtypes sit in
  `domain/gateways` beside the ports, not beside the adapters that throw them — a port declares both
  what it answers with and how it says it could not answer. That is what lets `presentation` map the
  whole family to one **502**, without depending on `infrastructure` to name the type.
- **The domain has behaviour.** `Order` owns its own state machine (`place()`, `deliver()`,
  `cancel()`), `Coupon` owns validity and redemption, and the pricing chain is typed arithmetic in
  `OrderPricing` — instead of all of it sitting inline in `OrderService.placeOrder`. `Order`'s
  constructor is private: an order is either **placed** or **restored**, and only the mapper restores
  one. That is what keeps "a new order starts PLACED" a rule rather than an argument the caller passes.
- **Only what MyShop owns is an entity.** `domain/entities` holds `Order` and `Coupon` — the two
  things with identity, lifecycle and state transitions here. `Product`, `TaxRate` and `Promotion` are
  immutable snapshots of records the ERP and the tax service own, so they are values, and they live in
  `domain/values` together — as `record`s, like everything else in that package, so two readings of
  the same product compare equal. (`Money` and `Rate` are hand-written classes rather than records,
  but they define `equals` too: `BigDecimal` equality is scale-sensitive, so they cannot inherit it.)
  The gateway ports therefore traffic in values, never in aggregates.
- **Ports live in `domain`, and that is a choice.** Repository and gateway interfaces sit beside the
  model (the DDD placement) rather than in `usecases` (the placement Uncle Bob's interactor implies).
  Both are defensible; this variant picked one and applied it everywhere. The dependency rule is
  unaffected either way — what matters is that the implementations live outside, which
  `ArchitectureTest` enforces.
- **The domain throws, and exactly one place catches.** A domain object states its rules by throwing
  `ValidationException` carrying a sealed `RuleViolation` — `order.deliver()`, `order.cancel()`,
  `coupon.discountAt()`, `YearEndBlackoutPolicy`. No use case catches it. `RefusalTranslatingUseCase`
  wraps every use case in `UseCaseConfig` and turns the throw into `Result.err(UseCaseError.from(e))`
  in one place, for all of them. The transport is chosen by the *caller*, not the callee: nobody
  branches on which refusal came back — every one ends as the same 422 — so handing it back as a
  value would only make each frame re-implement stack unwinding to reach where the throw already
  lands. `OrderNumber.parse` is the one refusal that stays a returned `Result`, because its two
  callers genuinely disagree: `DeliverOrder` answers a malformed number with "malformed",
  `CancelOrder` with "no such order". That is the bar — a branch someone actually takes.
  Where the *use case* is the one rejecting (missing field, not found, code already taken) it returns
  `Result.err` directly, with no exception involved. `Guard` is the other exception that proves the
  rule — its null checks throw `IllegalArgumentException` and nothing translates them, because
  `Guard.notNull(pricing)` failing is a programming error and a 500 is the honest answer.
- **`add` and `update`, not `save`.** No caller was ever unsure which it meant, so the port says
  which: `PlaceOrder` has just minted an order number that cannot exist yet, `CancelOrder` is holding
  a row it just read. A single `save` discarded that and paid the database to work it out again —
  a guaranteed-empty `SELECT` before every insert, and a re-read of the row the caller was already
  holding before every update. Placement is now one `INSERT`.
- **One scoped exception, by decision rather than by omission.** `usecases` request DTOs keep their
  `jakarta.validation` annotations so the controller can bind them with `@Valid @RequestBody`. The
  ArchUnit rule allows that import **by name**; a parallel set of near-identical web request classes
  was judged worse than the import.

## Test layers

| Layer | Subject | Present |
|---|---|---|
| `test` (unit) | domain entities, pricing, use cases, the dependency rule itself | yes |
| `componentTest` | the whole app over HTTP — architecture-independent | yes |
| `contractTest` | the `Http*Gateway` adapters (stub-vs-real parity), and the frontend's pact against the whole provider | yes |
| `integrationTest` | one adapter at a time — gateways, the repository adapters, the controller | yes |

Contract and narrow-integration tests take an *adapter* as their subject, and adapters are precisely
what changes between architectures — so unlike the component suite they could not simply be copied.
They were written per-variant once the design had settled, against the adapters that design produced.
Two of them are worth calling out:

- `integration/OrderRepositoryIntegrationTest` and `CouponRepositoryIntegrationTest` pin the
  domain → JPA → Postgres → domain round trip. That mapping is new code with no other test;
  `backend-java` needed no equivalent because it had no mapping to get wrong.
- `contract/internal/frontend/latest/BackendPactVerificationTest` replays the *same* consumer pact
  from `shop/contracts/` that `backend-java` verifies. It is the sharpest executable statement of
  this variant's whole claim: the identical consumer contract, satisfied by a completely rearranged
  inside.

## Shared vs duplicated

`src/testSupport` and `src/componentTest` are duplicated from `backend-java`. The duplication was
deliberate and was supposed to be temporary: copying first turns "these files should port unchanged"
from an assumption into a finding.

The finding is that **the fork is semantically zero and structurally total, and that combination is
the point.** Not one line of what a test *asserts* had to change — the boundary types were right.
But most of the tree still differs anyway, because a Java import binds to a package name and
rearranging the packages is exactly what this variant is.

That makes the borrowed-source-roots plan this README used to propose **not viable**, and it is worth
being precise about why, because the reason is not the one you would guess. The snippet anticipated
two exclusions (`**/testkit/driver/adapter/sut/**` and `BaseComponentTest.java`). In practice the
exclusion list would have to cover most of the duplicated tree, not a handful of files. Excluding
them means maintaining forked copies of them anyway, plus a fragile exclude list that goes stale
silently — strictly worse than the honest copy that exists today.

Real sharing would require the two variants to **agree on the package names of the boundary types** —
the request/response DTOs, `OrderStatus`, and the driver's view of the gateways — which means
extracting a shared contract module. That is a much larger change than a `srcDirs` tweak, and it
would couple the two variants at exactly the seam the exercise wants to leave free. The copy stays.

## What this variant is not

- **Not scaffolded.** `gh optivem init` copies `system/multitier/backend-java/` and nothing else, so
  students never receive this directory. The CRUD implementation stays the one canonical template.
- **Not deployed and not system-tested.** No `VERSION`, no `Dockerfile`, no release tag, no image
  push, no docker-compose service, no entry in any `gh-optivem-*.yaml`. It has a commit stage only.
  This is a non-goal, not a deferred item.
- **Not on SonarCloud.** No project exists for it; the `sonarqube` plugin and `sonar {}` block are
  removed rather than pointed at a key that would 404.

## Instructions

```shell
./gradlew build                    # compile + unit tests (no Docker)
./gradlew componentTest            # in-process component tests (requires Docker)
./gradlew integrationTest          # narrow-integration tests (requires Docker)
./gradlew externalSimulatorImage   # build the simulator image the real-parity contract tests need
./gradlew contractTest             # contract tests (requires Docker + the image above)
./gradlew checkstyleAll            # lint every source set
```

`contractTest` runs the real-parity classes against the external-system simulator, so build that
image first — or point them at an already-running instance by setting `EXTERNAL_SIMULATOR_BASE_URL`.
To skip the real-mode half entirely, filter it out:

```shell
./gradlew contractTest --tests 'com.mycompany.myshop.backend.contract.internal.*' \
                       --tests 'com.mycompany.myshop.backend.contract.external.*.*Stub*'
```
