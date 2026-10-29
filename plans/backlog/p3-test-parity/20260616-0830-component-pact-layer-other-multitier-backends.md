# Mirror the opt-in component/Pact layer into the other multitier backends (DEFERRED)

**Source plan:** `plans/20260616-0830-component-pact-layer-opt-in-canonical-projects.md` (Step 6).
**Status:** Deferred — execute only **after** the java/react opt-in shape is proven and merged.
**Scope (this file):** `system/multitier/backend-dotnet` + `system/multitier/backend-typescript`. **Monolith stays untouched.**

## TL;DR

**Why:** The opt-in in-process component + Pact provider-verification layer is being built first in `backend-java` (+ `frontend-react`). The CLAUDE.md "check all languages" rule means the *other* multitier backends should eventually get the same capability — but only once the pattern is settled, to avoid porting a shape that's still moving.

**End result:** `backend-dotnet` and `backend-typescript` each carry the same opt-in component/Pact layer as `backend-java`: in-process component tests + a Pact provider-verification test that replays the **same** `frontend-react` consumer contract, all **off the default build**, with the free `contracts/` + filesystem distribution as default and a broker as a documented opt-in.

## Outcomes

- **`backend-dotnet`** has an opt-in component-test seam (in-process host + Testcontainers-Postgres + external-system stubs) and a Pact provider-verification test, runnable via an explicit opt-in command, **not** part of `dotnet build` / default `dotnet test`.
- **`backend-typescript`** has the equivalent opt-in component + Pact provider-verification tests, off the default `npm test`.
- Both verify the **same** `frontend-react` consumer pact (one contract, three providers) from the neutral `contracts/` location (mono) / umbrella root repo (multi).
- Default builds for both remain **byte-for-byte unchanged** for existing students.
- gh-optivem generation flags (`--no-component-testing` / `--no-contract-tests`, per the source plan's resolved polarity) apply uniformly across all three backends.

## ▶ Next executable step (resume here)

**Blocked — do not start until the source plan (`20260616-0830`) is executed and the java/react shape is merged.** First executable unit once unblocked: replicate the `backend-java` Step-1 seam in `backend-dotnet` — a separate component-test project/target (xUnit + Testcontainers + WireMock.Net) wired to an opt-in command, deps off the default build — mirroring the Java `componentTest` source set decision.

## Steps

- [ ] Step 1: **backend-dotnet component seam** — in-process host (`WebApplicationFactory` on a real port) + Testcontainers-Postgres + WireMock.Net for ERP/Tax/Clock, in a dedicated opt-in test project/target excluded from default `dotnet build`/`dotnet test`.
- [ ] Step 2: **backend-dotnet Pact provider verification** — PactNet provider test replaying the `frontend-react` consumer contract from `contracts/`, reusing the component-test host; state handlers seed Postgres / stub externals.
- [ ] Step 3: **backend-typescript component seam** — in-process app (supertest/real port) + Testcontainers-Postgres + contract-driven stubs, off the default `npm test`, opt-in script.
- [ ] Step 4: **backend-typescript Pact provider verification** — `@pact-foundation/pact` provider verification against the same consumer contract.
- [ ] Step 5: **CI + docs parity** — non-default opt-in CI jobs for both; mirror the source plan's README/docs "Optional: component & contract tests" section into both projects.
- [ ] Step 6: **Cross-language consistency check** — confirm the three providers verify identical interactions and the same provider-state vocabulary; reconcile any drift (the publish-coupon 204 fix must already be settled in the consumer contract).

## Notes / open items inherited from the source plan

- Contract distribution default stays `contracts/` + filesystem; broker only as a documented, cost-labelled opt-in.
- Generation-flag polarity follows whatever the source plan resolves (opt-out vs opt-in).
- The local Testcontainers/Engine-29 block applies here too — verify compile locally, rely on CI for runtime.
