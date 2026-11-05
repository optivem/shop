# 2026-11-03 13:36 UTC — Follow-ups found during the backlog meta-plan run

## TL;DR

**Why:** Running the backlog meta-plan (U4 on 2026-11-02, M1 on 2026-11-03) turned up problems outside each unit's scope. They were only listed in the meta-plan's ▶ block, and that block goes away when the meta-plan is deleted.
**End result:** Each item below is either fixed or consciously dropped.

## ▶ Next executable step (resume here)

Item 3: keep the React deep link across login.

## Items

- [ ] 3. **React deep link lost across login (production bug):** a frontend-react user who pastes an order-details URL lands on home. Tokens are held in memory, so the deep link is dropped across the login redirect. Keep the target route through the Keycloak redirect (e.g. `redirectUri`/state), and add a UI test for it. The Next.js monolith frontend isn't affected, because it uses cookie sessions and `returnTo`.
- [ ] 4. **.NET `cancelOrder` UI test silently uses the API:** the .NET cancel-order UI path is still `CHANNEL_MODE=static`. Make it exercise the UI like Java/TS, or document why not.
- [ ] 5. **UI drivers only work against their own language's SUT:** order-details routes differ (`/order-details/{n}` in Java vs `/order-details?orderNumber=…` in Next.js). If `.github/workflows/cross-lang-system-verification.yml` runs one language's UI tests against another's monolith, the order-details UI tests fail there. Decide: unify the routes, or keep cross-lang UI runs within a language. Found by the M1 fix in `16012a4e`.
- [ ] 6. **Stale local images within one language:** `16012a4e` gave each language its own local image tag, but `gh optivem system start` still doesn't rebuild, so after a code change the stack can run an old image of the same language. Consider making `system start` rebuild (or warn) when sources are newer than the image. That's likely a gh-optivem change, so do it in a separate plan there if so.
- [ ] **gh-optivem `system-test run --test` false failure on .NET** (found 2026-11-04): with `-c gh-optivem-monolith-dotnet.yaml --suite acceptance --test <camelCase names>`, every test reports Passed, but the run exits 1 with `ERROR: requested test(s) never executed ... not found in any selected suite`. Probably a camelCase vs PascalCase name comparison, or only the last suite's results are read. This is a gh-optivem repo fix.
- [ ] 7. **Production realm has no way to get an account (all six flavors)** (found 2026-11-05, P2 bundle item 5): `shop-prod` has no users and `registrationAllowed: false`, so nobody can log in to production in any flavor. Prod smoke tests are anonymous, so they stay green. User decision (2026-11-05): production is a demo/teaching environment for now, so this is deliberate and not urgent. If real users ever need prod, choose an onboarding route (self-registration, an admin-created user, or an external IdP); that applies to all flavors.
