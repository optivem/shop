# Wire ErpRealParityContractTest into CI

**Status:** Implemented 2026-08-14, verified locally, **not yet committed.**

## Context

`system/multitier/backend-java/src/contractTest/java/.../contract/external/erp/` has a
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

## Resolutions (2026-08-14)

1. **Which pipeline stage → commit stage**, in `multitier-backend-java-commit-stage.yml`, as a step
   adjacent to the existing `external-contract` one.

   The deciding argument is not cost/benefit, it is correctness of placement. A parity test's job is
   to establish that the stub is trustworthy enough to assert against; the suite that asserts against
   that stub is `component`, which is a commit-stage suite. Running the parity check a stage later
   means a green commit stage can ship on a stub already known-wrong downstream — the test guarding a
   suite's validity must not run later than the suite it guards. This retires the acceptance-stage
   option rather than merely outranking it.

   It also makes the failure self-diagnosing: red `external-contract-real` beside green `component`
   means "the stub is lying, and every component test leaning on it is currently proving nothing."
   Remediation is the pair — fix the stub, then fix the component expectations calibrated against the
   wrong shape — surfaced on the diff that caused it. The suite must therefore be **blocking**, not
   advisory.

2. **Bring-up mechanism → Testcontainers, not docker-compose.**

   Commit stage's real convention is tighter than "hermetic": containers come from Testcontainers,
   compose belongs to acceptance and later. Honour it rather than bending it — start the simulator
   with `GenericContainer`, the same way Postgres is already started.

   **Amended during implementation:** the image is built by a new `erpSimulatorImage` Gradle `Exec`
   task (`docker build`, BuildKit on) rather than Testcontainers' `ImageFromDockerfile`.
   `ImageFromDockerfile` drives docker-java's *classic* builder, which rejects the simulator's
   Dockerfile — verified: `RUN --mount=type=cache,target=/root/.npm npm ci` fails with "the --mount
   option requires BuildKit". Only the image *build* moves out; Testcontainers still owns the
   container lifecycle, so the random port, per-run freshness, and reaper teardown all stand. The
   task is deliberately not a `dependsOn` of `contractTest`, so the other contract suites don't pay
   for the build.

   This is not a hermeticity breach: the simulator is built from in-repo source pinned to the commit
   under test, started and torn down per run, never reaching a vendor. `ErpRealParityContractTest`
   drives the production `ErpGateway` in-process against a Node mock-server, so nothing leaves the
   runner. Testcontainers additionally buys a random mapped port (no fixed `9111` on the runner, so no
   collision with the `system start` ordering the prerelease pipeline works around) and a lifecycle the
   Gradle task owns.

   **Required refactor:** `ErpRealParityContractTest.REAL_BASE_URL` is `static final`, read from
   `System.getenv()` at class-init, so it cannot see a dynamically assigned port. Resolve it after
   container start, keeping the `ERP_REAL_BASE_URL` env var as an override for the existing
   manual/compose workflow documented above.

   **Accepted cost:** the simulator image is not published anywhere, so commit stage pays an
   `npm ci` + image build (small — `node:22-alpine` — and layer-cacheable, but not free).

3. **Not one of the five default suites — confirmed.** Leave `external-contract-real` named but absent
   from `suiteGroups.all`, runnable only via explicit `--suite external-contract-real`. Keeping it out
   of `all` also keeps it clear of the prerelease pipeline's pre-`system start` component-test block.

4. **Fixture idempotency → moot under resolution 2.** A Testcontainers-managed simulator is fresh per
   run, so state cannot accumulate across runs and `SimulatorErpProductClient.createProduct`'s existing
   POST-then-PUT fallback is sufficient. (This is strictly safer than the acceptance stage, where a
   deployed stack outlives individual suites.) Re-open only if the bring-up mechanism changes.

## Not in scope for this item

Promotion and error-injection (`500`/`503`) real-mode twins — already ruled out; see
`shop/plans/20260717-1015-component-stub-contract-beyond.md` item 4 (real-mode `returns*` seeding is a
documented no-op convention) and `docs/atdd/test-taxonomy.md`.
