# 2026-11-12 09:39:08 UTC — Frontend testing: component tests, axe a11y, Lighthouse CI

Spun off from `20261111-1831-interview-showcase-skill-gaps.md` (gap #8, Step 7).

## TL;DR

**Why:** For a full-stack/tech-lead interview, shop's frontend is tested only end to end (the UI channel of the system tests). Interviewers ask about the test pyramid on the frontend: component tests, accessibility and performance budgets. Shop shows none of these today.
**End result:** The React frontend (`system/multitier/frontend-react`) has component tests for its main screens (catalog, product, cart, checkout, order history) with Testing Library. Accessibility is checked by axe in those component tests and, as a dedicated assertion, in a few UI-channel system-test scenarios. Lighthouse CI runs in an opt-in `workflow_dispatch` + nightly workflow against a locally started stack and reports performance, accessibility and best-practice scores against budgets, report-only (it never turns the nightly red). The server-rendered monoliths (Razor, Thymeleaf, Next.js) get no component tests; they are covered by the axe system-test check only.
**Unchanged:** the default clone and the commit/acceptance pipelines stay $0 and zero-infra (no hosted Lighthouse server, no paid SaaS). Existing e2e/system tests and their names stay as they are. The opt-in frontend Pact layer stays opt-in.

## ▶ Next executable step (resume here)

Open questions are resolved (G0, 2026-11-16). Runs as meta-plan unit U10 (Lane D, after U7 because of the shared `frontend-react` files). Start **Step 1**: list the screens in `system/multitier/frontend-react/src/pages/` (Catalog, ProductDetail, Cart, Checkout, OrderHistory, ...) and the existing test setup (default `npm test` = `vitest run` configured by the `test` block in `vite.config.ts`, which excludes `src/test/{latest,legacy}/**`; the opt-in layer under `src/test/{latest,legacy}/component` run by `test:component` with `vitest.opt-in.config.ts` and `src/test/support/component-harness.ts`), then decide where the new default-run screen tests live so they don't collide with the opt-in Pact/component layer; record the choice in Step 2.

## Steps

- [ ] Step 1: Inventory the React screens and the existing frontend test setup (vitest, the excluded Pact scripts) and pick the screens to cover first. Recommendation: catalog, product page, cart, checkout.
- [ ] Step 2: Component tests with Testing Library + vitest for those screens, with the API mocked at the fetch boundary (MSW or a stub fetch), wired into the frontend's default `npm test` so they run in the commit stage.
- [ ] Step 3: axe in the component tests (`vitest-axe` / `jest-axe`): every covered screen has no serious or critical violations.
- [ ] Step 4: axe in the UI-channel system tests (`@axe-core/playwright` for TS, `axe-core` via Playwright for Java and .NET) as a reusable driver check, so the monolith storefronts are covered too, in all three languages. Called as a dedicated assertion from a few scenarios (one per main screen), not automatically after every UI step.
- [ ] Step 5: Lighthouse CI (`@lhci/cli`, temporary public storage off, filesystem upload) in an opt-in `workflow_dispatch` + nightly workflow against a locally started multitier stack in stub mode; budgets in `lighthouserc.json`, report-only (assertions at `warn` level, the job does not fail on a missed budget).
- [ ] Step 6: `docs/tour.md` skills-map row (Frontend testing) linking the component tests, the axe checks and the Lighthouse workflow.

## Resolved decisions

All decided (user, 2026-11-16, meta-plan G0 answer 4):

- **Lighthouse budgets:** report-only first, so the nightly cannot go red on noise. Failing budgets can be switched on later once the scores are stable.
- **axe in system tests:** a dedicated assertion in a few scenarios, not an automatic check after every UI step (that would be slow and noisy).
- **Monolith component tests:** the server-rendered monolith pages (Razor, Thymeleaf, Next.js) are out of scope for Testing Library; the axe system-test check (Step 4) covers them.

## Constraints

- Opt-in, $0 and zero-infra by default: Lighthouse runs on the GitHub runner against a local stack, no hosted LHCI server.
- System-test changes land in Java, .NET and TypeScript.
- Frontend test dependencies go to devDependencies only.
