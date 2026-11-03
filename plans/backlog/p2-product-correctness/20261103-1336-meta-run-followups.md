# 2026-11-03 13:36 UTC — Follow-ups found during the backlog meta-plan run

## TL;DR

**Why:** Running the backlog meta-plan (U4 on 2026-11-02, M1 on 2026-11-03) turned up problems outside each unit's scope. They were only listed in the meta-plan's ▶ block, and that block goes away when the meta-plan is deleted.
**End result:** Each item below is either fixed or consciously dropped.

## ▶ Next executable step (resume here)

Item 1: run `npm run lint` in `system-test/typescript` and fix the 3 `--max-warnings 0` warnings in `tests/legacy/mod04/e2e/api-authorization-test.spec.ts`.

## Items

- [ ] 1. **TS lint fails `--max-warnings 0`** in `system-test/typescript`: 3 warnings in `tests/legacy/mod04/e2e/api-authorization-test.spec.ts` (one skipped test, two tests without assertions). These were already there before U4.
- [ ] 2. **Stale README references:** `system-test/java/README.md` still mentions `AccessControlTest` / `OrderOwnershipTest`, which don't exist. Check the .NET/TS READMEs for the same.
- [ ] 3. **React deep link lost across login (production bug):** a frontend-react user who pastes an order-details URL lands on home. Tokens are held in memory, so the deep link is dropped across the login redirect. Keep the target route through the Keycloak redirect (e.g. `redirectUri`/state), and add a UI test for it. The Next.js monolith frontend isn't affected, because it uses cookie sessions and `returnTo`.
- [ ] 4. **.NET `cancelOrder` UI test silently uses the API:** the .NET cancel-order UI path is still `CHANNEL_MODE=static`. Make it exercise the UI like Java/TS, or document why not.
- [ ] 5. **UI drivers only work against their own language's SUT:** order-details routes differ (`/order-details/{n}` in Java vs `/order-details?orderNumber=…` in Next.js). If `.github/workflows/cross-lang-system-verification.yml` runs one language's UI tests against another's monolith, the order-details UI tests fail there. Decide: unify the routes, or keep cross-lang UI runs within a language. Found by the M1 fix in `16012a4e`.
- [ ] 6. **Stale local images within one language:** `16012a4e` gave each language its own local image tag, but `gh optivem system start` still doesn't rebuild, so after a code change the stack can run an old image of the same language. Consider making `system start` rebuild (or warn) when sources are newer than the image. That's likely a gh-optivem change, so do it in a separate plan there if so.
- [ ] 7. **Leftover local `deploy-config.yml`:** `33c832c8` dropped `deploy-config.yml` and its `.gitignore` entry, so an old local copy now shows as untracked. Delete your local copy (nothing reads it). No repo change is needed; delete this item once done.
