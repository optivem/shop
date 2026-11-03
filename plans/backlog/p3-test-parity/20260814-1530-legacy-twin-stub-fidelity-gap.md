# Legacy twins' inline stub JSON has no fidelity guard

**Status:** Option A done (2026-11-03). Only option C remains, and it is **deferred**: it needs its
own discussion with the user before anyone executes it.

## TL;DR

**Why:** The `legacy/` twins hand-write their own WireMock stub JSON on purpose, as the "before" of
the stub-DSL refactor. The stub-fidelity suite (`contract/external/`) guards only the stub drivers,
so the twins can drift from the real wire format without any test noticing. The severity is
pedagogical: a stale twin teaches a wire format the real system no longer sends.

**End result:** The repo carries only as many unguarded "before" twins as the teaching material
needs. That may be the pact twin alone, which would shrink the unguarded surface to a single file.

## ▶ Next executable step (resume here)

**Deferred, needs a user decision.** Discuss option C with the user: which `legacy/` twins earn
their keep? Do not delete any twin before that discussion.

## Resolved (2026-11-03)

- **Option A (done):** the "byte-identical" claim in
  `contractTest/.../contract/internal/frontend/legacy/BackendPactVerificationTest` was replaced
  with an honest note. The twin is a frozen snapshot of the pre-DSL wire format, nothing guards it,
  and it has to be updated by hand when a stub driver changes.
- **Option B (rejected):** a byte-comparison test against shared literals. It would reintroduce
  sharing into twins whose purpose is to show the absence of sharing.

## Steps

- [ ] Option C (**deferred**, separate discussion): delete the legacy twins that are not carrying
      their weight.
  - `component/legacy/` (`CouponComponentTest`, `OrderHistoryComponentTest`,
    `smoke/system/MyShopSmokeTest`) is already a representative subset. Its value is showing the
    vocabulary difference on a familiar scenario, not scale. Does it survive at all?
  - `integration/legacy/ErpGatewayIntegrationTest` largely duplicates what
    `contract/external/erp/ErpStubParityContractTest` now covers. The keep-or-drop decision on
    `ErpStubParityContractTest` belongs to this discussion too (see `plans/backlog/README.md`).
  - Only the pact twin (`contract/internal/frontend/legacy/BackendPactVerificationTest`, about 17
    inline stub lines across 7 state handlers) shows duplication at the scale the DSL argument
    needs (see `docs/atdd/test-taxonomy.md`, "Why counterparty comes first in the path").

## Not in scope

- The `.NET` / TypeScript ports, which have no `contract/external/` suite yet.
