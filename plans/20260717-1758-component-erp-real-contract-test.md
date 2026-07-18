# Component external-system contract test — extend to "real life" (real ERP)

**Status:** ✅ Decided (2026-07-17) — **do not build.** The component-layer contract test stays
stub-only; stub-vs-real drift detection remains a system-test concern.

## Decision (target state)

**No real-ERP target is added at the component layer. Nothing in the shop changes.**

- The component external-system contract tests
  (`Erp/Tax/ClockStubContractComponentTest`) keep their single, distinct job: prove the WireMock
  **stub is consumable by the SUT** (real HTTP + real DTO parse through the production gateway). They
  stay **fully in-process** — Spring + Testcontainers Postgres + in-process WireMock, no network, no
  live ERP, $0 — which is the property that defines the component layer's value.
- **Stub-vs-real drift is already owned by the system-test layer.** `ErpRealContractTest` (and its
  Tax/Clock siblings) already run the identical scenario in `ExternalSystemMode.REAL` against the
  **seedable ERP simulator**, with the same exact-value pins (`withUnitPrice(12.0)` → `hasPrice(12.0)`)
  as the stub run. Adding a second copy at the component layer would duplicate that coverage while
  breaking the in-process/zero-infra profile — a net loss.
- This keeps the layered test taxonomy symmetric: the component contract test proves
  *stub↔SUT consumability*; the system-test contract test proves *stub↔real parity*. The two are
  complementary, not redundant, and neither should absorb the other's job.
- **Articles:** the two Substack external-systems articles stay deliberately scoped to the
  **component / stub-only** layer. The `TODO` in
  `substack/articles/drafts/PAID-TDD-contract-tests-external-systems.md` that pointed here should be
  converted (in the substack repo) from "real-life section — see plan" into a one-line recorded
  decision: *real-life stub-vs-real is taught at the system-test layer, out of scope for the
  component-layer articles.* No new article section is written.

**What is explicitly unchanged:** no new test classes, no `system-mode`/`ExternalSystemMode` wiring in
the component harness, no pipeline changes, no new network dependency. The only follow-up is the
one-line article-TODO cleanup above, in the sibling substack repo.

## Context

The component-layer external-system contract tests
(`system/multitier/backend-java/src/componentTest/.../latest/contract/{Erp,Tax,Clock}StubContractComponentTest.java`)
currently prove **one** thing: the WireMock stub the fast Component Tests rely on is **consumable by
the SUT's production gateway**. The read-back goes through the real `ErpGateway` / `TaxGateway` /
`ClockGateway` (real HTTP + real DTO parse via `SutErpReader` etc.), so a field drift in
`ErpStubDriver` (e.g. `price`→`cost`) fails the test instead of silently yielding null.

What it does **not** catch: drift between our **stub** and the **real ERP**. The stub is a hand-written
guess about the ERP's API; if the real ERP renames `price`, moves an endpoint, or changes a status
code, every component/stub test stays green. That gap only surfaces downstream (in an environment
wired to the ERP's test/prod instance).

The system-test layer already runs the contract scenario against a real target
(`ExternalSystemMode.REAL` → the ERP simulator). But the two Substack articles
("Contract Tests - External Systems" and its maintainable follow-up) are deliberately scoped to the
**component** layer only. The idea here: could the **component-layer** contract test also run
"real life" — against the real ERP — so it catches stub-vs-real drift at the component layer too?

## The question

Should the component-layer external-system contract test gain a **real-ERP ("real life") target**, in
addition to the stub target — and if so, how?

## Resolved decisions

1. **Do we even want it at the component layer? → No.** Stub-vs-real drift is already caught by
   the system-test layer (`ErpRealContractTest` in `ExternalSystemMode.REAL`, against the seedable
   simulator). Adding it at the component layer duplicates that coverage and breaks the component
   harness's defining property — fully in-process, zero-infra, $0. The component contract test keeps
   its distinct, complementary job (stub consumable by SUT). *(2026-07-17)*
2. **Mechanism → moot.** Determined by (1). No `ExternalSystemMode`-style mode flag and no separate
   `*RealContractComponentTest` are added; the component harness's dependency profile is unchanged.
3. **Seeding → moot.** Determined by (1). For reference: the system-test "real" target is itself the
   *seedable* ERP simulator, so even the existing REAL mode is controllable — there is no genuine
   third-party instance anywhere in the layered model.
4. **Value pinning vs shape → moot.** Determined by (1). The existing system-test REAL run already
   pins exact values against the seedable simulator; no shape-only policy is introduced.
5. **Pipeline placement → moot.** Determined by (1). No real-target run is added, so no new
   network/live-instance stage. The fast in-process stub run is unaffected.
6. **Articles → keep component/stub-only scope.** No "real life" section is added to the two Substack
   external-systems articles. The `TODO` pointer in
   `substack/articles/drafts/PAID-TDD-contract-tests-external-systems.md` is converted (in the
   substack repo) into a recorded decision: real-life stub-vs-real is taught at the system-test layer,
   out of scope for these component-layer articles. *(2026-07-17)*

## Notes

- Raised while reframing the Substack external-systems articles to be component-layer / stub-only.
- Don't build until the questions above are settled.
