# 2026-11-03 13:36 UTC — Follow-ups found during the backlog meta-plan run

## TL;DR

**Why:** Running the backlog meta-plan (U4 on 2026-11-02, M1 on 2026-11-03) turned up problems outside each unit's scope. They were only listed in the meta-plan's ▶ block, and that block goes away when the meta-plan is deleted.
**End result:** Each item below is either fixed or consciously dropped.

## ▶ Next executable step (resume here)

Nothing is scheduled. Items 6, 7, 9 and the gh-optivem `--test` item wait for a decision or a trigger (6 and `--test` are gh-optivem fixes, 7 waits until prod needs real users, 9 waits for the Neon limit to come up again).

## Items

- [ ] 6. **Stale local images within one language:** `16012a4e` gave each language its own local image tag, but `gh optivem system start` still doesn't rebuild, so after a code change the stack can run an old image of the same language. Consider making `system start` rebuild (or warn) when sources are newer than the image. That's likely a gh-optivem change, so do it in a separate plan there if so.
- [ ] **gh-optivem `system-test run --test` false failure on .NET** (found 2026-11-04): with `-c gh-optivem-monolith-dotnet.yaml --suite acceptance --test <camelCase names>`, every test reports Passed, but the run exits 1 with `ERROR: requested test(s) never executed ... not found in any selected suite`. Probably a camelCase vs PascalCase name comparison, or only the last suite's results are read. This is a gh-optivem repo fix. Also seen 2026-11-20 (U12) on Java and TS: a glob (`ViewOrder*`) or regex (`view-order|cancel-order`) `--test` value exits 1 with the same error, although every selected suite passed. So the check compares the raw `--test` string with the reported test names.
- [ ] 7. **Production realm has no way to get an account (all six flavors)** (found 2026-11-05, P2 bundle item 5): `shop-prod` has no users and `registrationAllowed: false`, so nobody can log in to production in any flavor. Prod smoke tests are anonymous, so they stay green. User decision (2026-11-05): production is a demo/teaching environment for now, so this is deliberate and not urgent. If real users ever need prod, choose an onboarding route (self-registration, an admin-created user, or an external IdP); that applies to all flavors.
- [ ] 9. **Shared Neon compute is the cloud chain's concurrency ceiling (all flavors + Keycloak)** (found 2026-11-06): every acceptance/qa/prod stack and `shop-keycloak` connect to one Neon compute on the direct endpoint (`max_connections` 112, 4 reserved). The .NET Npgsql pool was capped at 10 to match HikariCP/node-postgres (that was the meta-prerelease 42108105346 .NET failure: Postgres 53300 on `shop-multitier-dotnet-stub-backend` at 22:12 UTC, and `XX000: Couldn't connect to compute node` on `shop-monolith-dotnet-real` at 22:14). But the worst case is still 12 acceptance stacks x up to 2 instances x 10 connections, plus Keycloak. Keycloak also logged `Acquisition timeout while waiting for new connection` at 22:33:50 UTC in that run. If this shows up again, options are: use the Neon pooler endpoint for the app backends (Flyway and Keycloak stay on the direct endpoint), give acceptance its own Neon branch/compute, or bump the compute size.
