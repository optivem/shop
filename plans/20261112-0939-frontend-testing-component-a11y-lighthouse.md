# 2026-11-12 09:39:08 UTC — Frontend testing: component tests, axe a11y, Lighthouse CI

Spun off from `20261111-1831-interview-showcase-skill-gaps.md` (gap #8, Step 7).

## TL;DR

**Why:** For a full-stack/tech-lead interview, shop's frontend is tested only end to end (the UI channel of the system tests). Interviewers ask about the test pyramid on the frontend: component tests, accessibility and performance budgets. Shop shows none of these today.
**End result:** The React frontend (`system/multitier/frontend-react`) has component tests for its main screens (catalog, product, cart, checkout, order history) with Testing Library. Accessibility is checked by axe in those component tests and, as a dedicated assertion, in a few UI-channel system-test scenarios. Lighthouse CI runs in an opt-in `workflow_dispatch` + nightly workflow against a locally started stack and reports performance, accessibility and best-practice scores against budgets, report-only (it never turns the nightly red). The server-rendered monoliths (Razor, Thymeleaf, Next.js) get no component tests; they are covered by the axe system-test check only.
**Unchanged:** the default clone and the commit/acceptance pipelines stay $0 and zero-infra (no hosted Lighthouse server, no paid SaaS). Existing e2e/system tests and their names stay as they are. The opt-in frontend Pact layer stays opt-in.

## ▶ Next executable step (resume here)

Steps 1–3, 5 and 6 are done (U10 no-docker part, 2026-11-17: `8cf687c3` axe screen tests, `5a294ef7` Lighthouse workflow, `a2520492` tour row). Only **Step 4** is left. It needs **Lane D** (a booted docker stack) and touches `system-test/**`, so it runs only after U7 has pushed its `system-test/**` and `frontend-react/src/auth/**` changes. Start by adding the axe check to the TypeScript UI driver (`@axe-core/playwright`, devDependency in `system-test/typescript`), then Java and .NET, then call it from one scenario per main screen; gate with `--sample` per language. When it lands, change the last sentence of the `docs/tour.md` "Frontend testing" row from "is planned to cover them" to a link to the driver check.

## Steps

- [ ] Step 4: axe in the UI-channel system tests (`@axe-core/playwright` for TS, `axe-core` via Playwright for Java and .NET) as a reusable driver check, so the monolith storefronts are covered too, in all three languages. Called as a dedicated assertion from a few scenarios (one per main screen), not automatically after every UI step. Report serious and critical violations only, like `frontend-react/src/test/axe.ts`.

## Follow-ups (from the U10 no-docker run, 2026-11-17)

- **Auth/login screen tests:** `SignIn` and the deep link through login got no axe or screen test, because U7 was changing `frontend-react/src/auth/**`. Add them to `src/test/unit/a11y.unit.test.tsx` once U7 is pushed. **Recommended:** one axe test for `SignIn`; the deep-link behaviour stays with U7's own tests.
- **First Lighthouse run:** `lighthouse.yml` was committed but not yet pushed (shared dirty tree), so it has never run. After it is on `origin/main`, dispatch it once (`gh workflow run lighthouse.yml`; it boots its own stub stack on the runner, $0, no prod) and check the job summary shows three pages with scores. **Recommended:** then set the `warn` budgets in `frontend-react/lighthouserc.json` just under the measured medians.
- **Screen tests already existed:** Step 2's behaviour tests were already in the default `npm test` (`src/test/unit/storefront.interaction.test.tsx`, `checkout.interaction.test.tsx`, `account.interaction.test.tsx`, `cart.unit.test.tsx`), mocked at the service boundary rather than at `fetch`. U10 kept that pattern and added axe on top (8 screens). REL #70's list of React criterion gaps (UC-007, UC-008.5/.6/.8–.11, UC-009.2/.3/.5/.7/.8/.11/.12) was not re-checked against those tests. **Recommended:** check it per criterion in a small follow-up unit; many are likely already covered.

## Resolved decisions

All decided (user, 2026-11-16, meta-plan G0 answer 4):

- **Lighthouse budgets:** report-only first, so the nightly cannot go red on noise. Failing budgets can be switched on later once the scores are stable.
- **axe in system tests:** a dedicated assertion in a few scenarios, not an automatic check after every UI step (that would be slow and noisy).
- **Monolith component tests:** the server-rendered monolith pages (Razor, Thymeleaf, Next.js) are out of scope for Testing Library; the axe system-test check (Step 4) covers them.

## Constraints

- Opt-in, $0 and zero-infra by default: Lighthouse runs on the GitHub runner against a local stack, no hosted LHCI server.
- System-test changes land in Java, .NET and TypeScript.
- Frontend test dependencies go to devDependencies only.
