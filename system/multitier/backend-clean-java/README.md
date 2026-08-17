# Backend — Clean Architecture variant

An alternative implementation of the same backend as [`../backend-java`](../backend-java), structured
the clean-architecture way instead of the CRUD/layered way.

**Same HTTP contract. Same database schema. Same component tests.** Only `src/main` differs — and
that is the whole point: the acceptance-level specs do not change when the inside is rearranged.

## Status

The refactor is complete. `src/main` is organised by the dependency rule rather than by technical
layer, and the evidence that behaviour survived is that the component suite was never edited to
accommodate any of it: **62 component tests, the same 62, green at every commit.**

| Suite | Tests | Needs Docker |
|---|---|---|
| `test` (unit — domain, use cases, ArchUnit) | 58 | no |
| `componentTest` | 62 | yes (Postgres) |
| `integrationTest` | 38 | yes (Postgres; the gateway half is in-process) |
| `contractTest` | 43 | yes (Postgres + the simulator image for real-parity) |

## Package map

Four top-level packages under `com.mycompany.myshop.backend`, dependencies pointing inward only:

| Package | Holds | May depend on |
|---|---|---|
| `presentation` | REST controllers, the global exception handler, web config | `usecases`, `domain` |
| `usecases` | one class per use case, plus the request/response DTOs they take and return | `domain` |
| `domain` | entities as POJOs (no ORM annotations), plain repository interfaces (no Spring Data), gateway interfaces, `Money`/`Rate` value objects, domain exceptions | nothing |
| `infrastructure` | JPA entities, Spring Data repositories, repository adapters, gateway adapters + their wire DTOs, bean wiring | all of the above |

The shape worth noticing:

- **`domain` has no framework imports at all.** No `org.springframework.*`, no
  `jakarta.persistence.*`, no Lombok — enforced by `ArchitectureTest`, not by review.
- **Interfaces are owned by the inside, implementations by the outside.** `OrderRepository`,
  `CouponRepository`, `ErpGateway`, `TaxGateway` and `ClockGateway` are plain interfaces in `domain`;
  the Spring Data repositories, JPA entities and HTTP clients implement them from `infrastructure`.
- **Gateways return domain types.** `ErpGateway` hands back `Optional<Product>` / `Promotion`, not
  the ERP's JSON shape. The wire DTOs stay in `infrastructure/external`, so a supplier renaming a
  field cannot reach the centre.
- **The domain has behaviour.** `Order` owns its own state machine (`cancel()`, `deliver()`),
  `Coupon` owns validity and redemption, and the pricing chain is typed arithmetic in
  `OrderPricing` — instead of all of it sitting inline in `OrderService.placeOrder`.
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

## Shared vs duplicated — the measurement

`src/testSupport` and `src/componentTest` are duplicated from `backend-java`. The duplication was
deliberate and was supposed to be temporary: copying first turns "these files should port unchanged"
from an assumption into a measurement. Here is the measurement, taken after the refactor.

The two trees are **identical file-for-file** — no file was added, removed or renamed on either side.
Of ~130 files, 31 differ at all:

| Kind of change | Lines | Where |
|---|---|---|
| Import lines re-pointed at the new packages | 48 | 27 files |
| Injected field types in `BaseComponentTest` | 4 | 1 file |
| Field type, constructor param and delegate call in `Sut*Reader` | 6 | 2 files |
| Javadoc `{@link}` targets following the rename | 2 | 2 files |
| **Assertions, scenarios, expectations** | **0** | — |

**So the fork is semantically zero and structurally total, and that combination is the finding.** Not
one line of what a test *asserts* had to change — the boundary types were right. But 31 files still
differ, because a Java import binds to a package name and rearranging the packages is exactly what
this variant is.

That makes the borrowed-source-roots plan this README used to propose **not viable**, and it is worth
being precise about why, because the reason is not the one you would guess. The snippet anticipated
two exclusions (`**/testkit/driver/adapter/sut/**` and `BaseComponentTest.java`). The measurement
finds 31 files needing exclusion, not 4. Excluding them means maintaining forked copies of them
anyway, plus a fragile exclude list that goes stale silently — strictly worse than the honest copy
that exists today.

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
