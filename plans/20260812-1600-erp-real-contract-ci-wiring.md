# Wire ErpRealParityContractTest into CI

**Status:** Deferred — **not yet approved to build. Discuss before executing.**

## Context

`system/multitier/backend-java/src/contractTest/java/.../contract/latest/external/erp/` has a
`Stub`/`Real` pair for the ERP product read (`ErpStubParityContractTest`,
`ErpRealParityContractTest`), mirroring `shop/system-test/java`'s `mod11/contract/erp`
`Base`/`Real`/`Stub` pattern but scoped to products only and without touching `ErpDriver`/`ErpDsl`
(see `docs/atdd/test-taxonomy.md`, "Backend: five-suite instantiation", for why).

`ErpStubParityContractTest` is already part of `component-tests.yaml`'s `external-contract` suite
(`--tests '*StubParityContractTest' --tests '*StubConsumabilityContractTest'`) and runs in
commit-stage today. `ErpRealParityContractTest` carries the `Real` role marker rather than `Stub`, so
that same filter skips it — it needs the ERP simulator (`external-systems/simulators`) running at
`ERP_REAL_BASE_URL` (default `http://localhost:9111/erp`) and is currently **manual-only**:

```
docker compose -f docker/java/multitier/docker-compose.local.real.yml up external-system-simulators
./gradlew.bat contractTest --tests '*ErpRealParityContractTest'
```

## Deferred item: CI wiring

Add a new suite to `system/multitier/backend-java/component-tests.yaml`, e.g.:

```yaml
- id: external-contract-real
  name: External System Contract (Real ERP)
  command: .\gradlew.bat contractTest --tests '*RealParityContractTest'
  sampleTest: "getProductDetailsReturnsDetailsWhenFound"
  requiresDocker: true
```

Open questions to resolve before building this:

1. **Which pipeline stage.** Commit-stage is meant to stay fast/hermetic (Testcontainers Postgres only,
   no other live services) — adding the simulator there changes that contract for everyone, not just
   this suite. QA/acceptance stage already deploys `docker-compose.pipeline.real.yml` (the full stack,
   for system-test), so bringing up just `external-system-simulators` there — or reusing that same
   deployment — is the more consistent fit. Decide which workflow file(s) this belongs in.
2. **Bring-up mechanism.** `docker-compose.pipeline.real.yml` deploys the full multitier stack via
   `optivem/actions/deploy-docker-compose@v1` for system-test's use. Reusing that deployment (rather
   than a second, backend-java-only container start) avoids running the simulator twice per pipeline
   run, but couples this Gradle suite's timing to when that deployment step runs.
3. **Not one of the five default suites.** Add `external-contract-real` to `suiteGroups` only if/when
   a stage is meant to run it by default — otherwise leave it named but unlisted, runnable only via
   explicit `--suite external-contract-real`.
4. **Fixture idempotency in CI.** `SimulatorErpProductClient.createProduct` already handles the
   duplicate-id case locally (POST, fall back to PUT on conflict) — confirm this still holds if CI runs
   against a simulator instance that isn't torn down between pipeline runs (i.e. state can accumulate
   across runs, not just within one).

## Not in scope for this item

Promotion and error-injection (`500`/`503`) real-mode twins — already ruled out; see
`shop/plans/20260717-1015-component-stub-contract-beyond.md` item 4 (real-mode `returns*` seeding is a
documented no-op convention) and `docs/atdd/test-taxonomy.md`.
