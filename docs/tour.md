# Guided Tour

MyShop is a small e-commerce system (catalog, cart, orders, coupons) built three times, in Java, .NET and TypeScript, as both a monolith and a multitier (React frontend + backend) system. Around it sits a full Acceptance Test-Driven Development harness and a multi-stage CI/CD pipeline that deploys to Google Cloud Run, with production running at [shop.optivem.com](https://shop.optivem.com). The point of the repo is the delivery practice: the same design, tests and pipeline, kept in parity across languages and architectures.

**How to use this tour:** follow the guided path top to bottom (about 10 minutes) to see one use case travel from spec to production, then use the skills map to jump to anything specific.

## Guided path: Place Order, end to end

### 1. The spec

Each use case starts as plain-language docs: a narrative, business rules and numbered acceptance criteria. The criteria IDs (e.g. `UC-001.1`) are the same tags the acceptance tests carry, so every test traces back to a requirement.

- [Use-case narrative](shared/specs/use-cases/UC-001-place-order/use-case-narrative.md) · [Business rules](shared/specs/use-cases/UC-001-place-order/business-rules.md) · [Acceptance criteria](shared/specs/use-cases/UC-001-place-order/acceptance-criteria.md) · [All use cases](shared/specs/use-cases/README.md)

### 2. The acceptance test

Tests read as given/when/then in business language, with no HTTP, selectors or SQL. `@Channel({UI, API})` runs the same test through the browser and through the REST API, so one spec verifies both channels.

- Java: [PlaceOrderPositiveTest.java](../system-test/java/src/test/java/com/mycompany/myshop/systemtest/latest/acceptance/PlaceOrderPositiveTest.java), [PlaceOrderNegativeTest.java](../system-test/java/src/test/java/com/mycompany/myshop/systemtest/latest/acceptance/PlaceOrderNegativeTest.java)
- .NET: [PlaceOrderPositiveTest.cs](../system-test/dotnet/SystemTests/Latest/AcceptanceTests/PlaceOrderPositiveTest.cs)
- TypeScript: [place-order-positive-test.spec.ts](../system-test/typescript/tests/latest/acceptance/place-order-positive-test.spec.ts)
- Test levels explained: [test taxonomy](atdd/test-taxonomy.md)

### 3. The scenario DSL

The fluent `given() / when() / then()` layer that the test reads against. Port (interfaces) and core (implementation) are split so tests depend only on the vocabulary, not on how it is executed.

- Port: [ScenarioDsl.java](../system-test/java/src/main/java/com/mycompany/myshop/testkit/dsl/port/ScenarioDsl.java), [WhenPlaceOrder.java](../system-test/java/src/main/java/com/mycompany/myshop/testkit/dsl/port/when/steps/WhenPlaceOrder.java)
- Core: [given/](../system-test/java/src/main/java/com/mycompany/myshop/testkit/dsl/core/scenario/given/), [WhenPlaceOrderImpl.java](../system-test/java/src/main/java/com/mycompany/myshop/testkit/dsl/core/scenario/when/steps/WhenPlaceOrderImpl.java), [then/](../system-test/java/src/main/java/com/mycompany/myshop/testkit/dsl/core/scenario/then/)

### 4. The use-case DSL

One level down: one class per business action, with sensible defaults and alias resolution, so scenario steps stay short. This is where test data is generated and results are verified.

- [MyShopDsl.java](../system-test/java/src/main/java/com/mycompany/myshop/testkit/dsl/core/usecase/MyShopDsl.java), [PlaceOrder.java](../system-test/java/src/main/java/com/mycompany/myshop/testkit/dsl/core/usecase/usecases/PlaceOrder.java), [PlaceOrderVerification.java](../system-test/java/src/main/java/com/mycompany/myshop/testkit/dsl/core/usecase/usecases/PlaceOrderVerification.java)

### 5. The drivers (API and UI)

Both drivers implement the same driver port, so the DSL never knows which channel it is talking to. The API driver uses a typed HTTP client; the UI driver uses Playwright page objects.

- Port: [MyShopDriver.java](../system-test/java/src/main/java/com/mycompany/myshop/testkit/driver/port/MyShopDriver.java)
- API: [MyShopApiDriver.java](../system-test/java/src/main/java/com/mycompany/myshop/testkit/driver/adapter/api/MyShopApiDriver.java)
- UI: [MyShopUiDriver.java](../system-test/java/src/main/java/com/mycompany/myshop/testkit/driver/adapter/ui/MyShopUiDriver.java)
- .NET equivalents: [Driver.Adapter/](../system-test/dotnet/Driver.Adapter/) · TypeScript: [driver/adapter/](../system-test/typescript/src/testkit/driver/adapter/)

### 6. External systems: stub driver and contract tests

Acceptance tests control the ERP through a WireMock stub, so they are fast and deterministic. Contract tests keep the stub honest: the same test suite runs against the stub and against the real ERP, and fails if they disagree. Separately, the multitier frontend and backend are pinned together with consumer-driven Pact contracts.

- ERP drivers: [ErpStubDriver.java](../system-test/java/src/main/java/com/mycompany/myshop/testkit/driver/adapter/external/erp/ErpStubDriver.java), [ErpRealDriver.java](../system-test/java/src/main/java/com/mycompany/myshop/testkit/driver/adapter/external/erp/ErpRealDriver.java) · stub mappings: [external-systems/stubs/](../external-systems/stubs/)
- Stub-vs-real contract tests: [BaseErpContractTest.java](../system-test/java/src/test/java/com/mycompany/myshop/systemtest/latest/contract/erp/BaseErpContractTest.java), [ErpRealContractTest.java](../system-test/java/src/test/java/com/mycompany/myshop/systemtest/latest/contract/erp/ErpRealContractTest.java), [ErpStubContractTest.java](../system-test/java/src/test/java/com/mycompany/myshop/systemtest/latest/contract/erp/ErpStubContractTest.java)
- Pact (frontend consumer → backend provider): [contracts/](../contracts/README.md), consumer [order.interactions.ts](../system/multitier/frontend-react/src/test/interactions/order.interactions.ts), provider [BackendPactVerificationTest.java](../system/multitier/backend-java/src/contractTest/java/com/mycompany/myshop/backend/contract/internal/frontend/latest/BackendPactVerificationTest.java)

### 7. The backend implementation

The production code the tests drive. Ordering is wrapped in an idempotent service so a retried POST does not create a duplicate order.

- Monolith Java: [OrderApiController.java](../system/monolith/java/src/main/java/com/mycompany/myshop/api/controller/OrderApiController.java), [OrderService.java](../system/monolith/java/src/main/java/com/mycompany/myshop/core/services/OrderService.java), [IdempotentOrderService.java](../system/monolith/java/src/main/java/com/mycompany/myshop/core/services/IdempotentOrderService.java)
- Multitier backends: [Java](../system/multitier/backend-java/src/main/java/com/mycompany/myshop/backend/api/controller/OrderController.java), [.NET](../system/multitier/backend-dotnet/Controllers/OrderController.cs), [TypeScript](../system/multitier/backend-typescript/src/api/controller/order.controller.ts), frontend [React](../system/multitier/frontend-react/)
- Clean-architecture variant: [PlaceOrder.java](../system/multitier/backend-clean-java/src/main/java/com/mycompany/myshop/backend/usecases/commands/order/PlaceOrder.java) use case behind a [presentation controller](../system/multitier/backend-clean-java/src/main/java/com/mycompany/myshop/backend/presentation/controllers/OrderController.java)

### 8. The pipeline

A change moves through commit stage (compile, lint, unit, component, Sonar, images) → acceptance stage (system tests against Docker Compose and Cloud Run) → QA → production. Meta-stages orchestrate this across all six flavors: the prerelease meta-stage tags a release candidate only when every flavor is green, and the release meta-stage promotes it to production.

- Commit stage: [monolith-java-commit-stage.yml](../.github/workflows/monolith-java-commit-stage.yml) · [docs](pipeline/commit-stage.md)
- Acceptance stage: [monolith-java-acceptance-stage.yml](../.github/workflows/monolith-java-acceptance-stage.yml), [monolith-java-acceptance-stage-docker-compose.yml](../.github/workflows/monolith-java-acceptance-stage-docker-compose.yml), shared [_acceptance-stage.yml](../.github/workflows/_acceptance-stage.yml) · [docs](pipeline/acceptance-stage.md)
- QA and production: [monolith-java-qa-stage.yml](../.github/workflows/monolith-java-qa-stage.yml), [monolith-java-prod-stage.yml](../.github/workflows/monolith-java-prod-stage.yml) · [docs](pipeline/qa-and-prod-stages.md)
- Meta-stages: [meta-prerelease-stage.yml](../.github/workflows/meta-prerelease-stage.yml) (via [_meta-prerelease-pipeline.yml](../.github/workflows/_meta-prerelease-pipeline.yml)), [meta-release-stage.yml](../.github/workflows/meta-release-stage.yml)

## Skills map

| Skill | Where to look | Notes |
|---|---|---|
| CI/CD meta-stages | [meta-prerelease-stage.yml](../.github/workflows/meta-prerelease-stage.yml), [meta-release-stage.yml](../.github/workflows/meta-release-stage.yml), [workflows/](../.github/workflows/), [pipeline docs](pipeline/) | Commit → acceptance → RC → release, across 6 flavors; reusable `_*.yml` workflows and composite [actions](../.github/actions/) |
| ATDD layers (test → DSL → driver → client) | [acceptance tests](../system-test/java/src/test/java/com/mycompany/myshop/systemtest/latest/acceptance/), [dsl/](../system-test/java/src/main/java/com/mycompany/myshop/testkit/dsl/), [driver/](../system-test/java/src/main/java/com/mycompany/myshop/testkit/driver/), [language equivalents](atdd/code/language-equivalents.md) | One test runs on both UI and API channels |
| Contract tests | [ERP contract tests](../system-test/java/src/test/java/com/mycompany/myshop/systemtest/latest/contract/), [Pact contracts](../contracts/README.md) | Stub-vs-real for ERP/Tax/Clock; consumer-driven Pact for frontend ↔ backend (opt-in, no broker) |
| Three languages | [system-test/java](../system-test/java/), [system-test/dotnet](../system-test/dotnet/), [system-test/typescript](../system-test/typescript/) | Same design and test names in Java, .NET and TypeScript |
| Monolith + multitier | [system/monolith/](../system/monolith/), [system/multitier/](../system/multitier/) | Multitier = React frontend + Java/.NET/TS backends |
| Clean architecture | [backend-clean-java/](../system/multitier/backend-clean-java/) | Use cases, ports and adapters; domain entities free of persistence ids |
| Keycloak / OIDC | [docker/keycloak/](../docker/keycloak/), [SecurityConfig.java](../system/monolith/java/src/main/java/com/mycompany/myshop/config/SecurityConfig.java) | Realm seeded for local/acceptance; prod realm strips test users at image build |
| Postgres + Flyway | [system/db/](../system/db/), [migrations/](../system/db/migrations/), [schema-change doctrine](../CONTRIBUTING.md#schema-changes) | Forward-only, expand-contract migrations |
| Terraform + GCP Cloud Run | [terraform/](../terraform/README.md), [_deploy-stage.yml](../.github/workflows/_deploy-stage.yml) | Infra as code, includes a billing kill switch |
| SonarCloud | [sonar-scan action](../.github/actions/sonar-scan/action.yml) | Runs in every commit stage |
| Mutation testing (PITest, Java) | [multitier-backend-java-mutation-nightly.yml](../.github/workflows/multitier-backend-java-mutation-nightly.yml), [build.gradle](../system/multitier/backend-java/build.gradle) | Nightly, Java multitier backend |
| OpenAPI (Java) | [monolith OpenApiConfig.java](../system/monolith/java/src/main/java/com/mycompany/myshop/config/OpenApiConfig.java), [multitier OpenApiConfig.java](../system/multitier/backend-java/src/main/java/com/mycompany/myshop/backend/config/OpenApiConfig.java) | Java backends only today |
| Idempotency | [IdempotentOrderService.java](../system/monolith/java/src/main/java/com/mycompany/myshop/core/services/IdempotentOrderService.java), [test](../system/monolith/java/src/test/java/com/mycompany/myshop/core/services/IdempotentOrderServiceTest.java), [migration](../system/db/migrations/V20261103180000__add_idempotency_keys.sql) | Idempotency keys on order placement; .NET in [IdempotencyKey.cs](../system/monolith/dotnet/Core/Entities/IdempotencyKey.cs) |
| Docker Compose | [docker/](../docker/), e.g. [docker/java/monolith/](../docker/java/monolith/) | Local and pipeline stacks, each in stub and real ERP modes |
| Production deploy | [shop.optivem.com](https://shop.optivem.com), [monolith-java-prod-stage.yml](../.github/workflows/monolith-java-prod-stage.yml) | Released via the meta-release stage |
| Supply-chain security | [dependabot.yml](../.github/dependabot.yml), [codeql.yml](../.github/workflows/codeql.yml), [supply-chain.yml](../.github/workflows/supply-chain.yml) | Grouped Dependabot updates; CodeQL for Java, C#, TS and Actions; Trivy scan + CycloneDX SBOM per image (report-only, free on public GitHub) |

## Coming next

Planned additions, not yet in the repo. Each moves into the skills map when it lands.

- **Observability** (planned): OpenTelemetry tracing frontend → backend → ERP, structured logs with trace IDs, opt-in `grafana/otel-lgtm` Compose profile.
- **Resilience** (planned): timeouts, retries and circuit breakers on ERP/Clock calls, proven by an acceptance test against a slow or failing stub.
- **Performance** (planned): k6 load test for browse + place order, opt-in workflow reporting p95/p99.
- **Parity** (planned): OpenAPI for the .NET and TypeScript backends; Stryker mutation testing for .NET and TypeScript.
