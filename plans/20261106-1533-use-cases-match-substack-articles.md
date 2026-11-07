# 2026-11-06 15:33:50 UTC — Place Order use case matches the Substack articles, with a spec gate that checks all three languages

> Merged on 2026-11-06 from three plans: this one, `20261106-2030-spec-gate-dotnet-typescript-tags.md` and `20261106-2030-spec-gate-read-gherkin-criteria.md`. The work is split into **parallel tracks** for autonomous execution with subagents (one subagent per track; tracks A, B and C don't touch the same files). Track D joins them at the end.

## TL;DR

**Why:** Two paid Substack articles use Place Order as the reader's worked example: `articles/published/2026-11-06-PAID-ATDD-acceptance-test-coverage-with-ai.md` and `articles/drafts/next/2026-11-06-PAID-ATDD-use-cases-stay-user-stories-go.md` in the `optivem/substack` repo. They show a full use case narrative and Gherkin acceptance criteria. The shop now matches them, but that broke the spec gate: gh-optivem's parser (`internal/spec/usecase.go`, `criterionPattern`) only reads bullet lines (`- UC-001.N: text`), so with Gherkin it finds zero criteria and passes without checking anything (`gh optivem spec validate` returns `OK`, exit 0). The `spec-gate` step is commented out for now. Also, only the Java tests carry `UC-001.N` tags, and `spec.yaml` reads only Java JUnit XML, so the gate can't say anything about .NET or TypeScript.
**End result:** The Place Order narrative and Gherkin criteria match the articles (done). Every criterion, UC-001.14 (ERP unavailable) included, has a tagged acceptance test in Java, .NET and TypeScript. gh-optivem reads `@UC-NNN.M`-tagged Gherkin scenarios. The shop `spec-gate` step is back on, finds all 14 criteria, and reads the results of all three languages.

## Done

- `docs/use-cases/UC-001-place-order.md` holds the narrative as the articles show it (primary actor Customer, secondary actors ERP and Clock, 9-step main success scenario, extensions 2a, 3a, 4a, 4b, 5a, 5b, 6a), saying ERP everywhere, never SAP.
- The criteria are Gherkin scenarios tagged `@UC-001.N` plus the extension tag where one applies: 2a → UC-001.12, 3a → UC-001.13, 4a → UC-001.8, 4b → UC-001.10, 5a → UC-001.14, 5b → UC-001.9, 6a → UC-001.11.
- The `spec-gate` step in `.github/workflows/_acceptance-stage-docker-compose.yml` is commented out, with a pointer to this plan. `spec.yaml` stays.
- **Track B: .NET and TypeScript tests carry the UC IDs** (one for one with Java, including the e2e `PlaceOrderPositiveTest.shouldPlaceOrder` → UC-001.1; no UC-001.14 yet). All three languages emit the same JUnit shape that gh-optivem `internal/spec/junit.go` reads: `<testcase classname=… name=…><properties><property name="tag" value="UC-001.N"/></properties></testcase>` (it accepts property name `tag` or `criterion`, value `UC-NNN.M`, optional `@`).
  - **.NET:** `[Trait("tag", "UC-001.N")]` on the test method (one per ID). The `JunitXml.TestLogger` package (added to `SystemTests.csproj`) writes every xUnit trait as a testcase property. Each latest suite in `system-test/dotnet/tests.yaml` adds `--logger 'junit;LogFilePath=TestResults/junit/<suite-id>.xml'`, so results land in `system-test/dotnet/SystemTests/TestResults/junit/*.xml`. Names look like `ShouldX(channel: API)`; gh-optivem's `()` stripping merges the channels.
  - **TypeScript:** `test('name', criteria('UC-001.N', …), async …)` with the helper `tests/commons/criteria.ts`, which turns each ID into a `{ type: 'tag', description: id }` annotation. `test.each` / `test.eachAlsoFirstRow` take the same optional details argument (`(name, criteria(…), fn)`; the shop facade `src/testkit/.../playwright/bindTestEach.ts` passes it to every row). Playwright's built-in `junit` reporter (added in `playwright.config.ts`, `outputDir: 'junit-results'`) writes annotations as testcase properties. Each latest suite in `system-test/typescript/tests.yaml` sets `PLAYWRIGHT_JUNIT_OUTPUT_NAME: <suite-id>.xml`, so results land in `system-test/typescript/junit-results/*.xml` (gitignored; not under `test-results/`, which Playwright wipes per run). Note for D: TS testcase names are `[API Channel] › shouldX` (and parameterised rows carry `_param=value`), so channels/rows don't merge into one test as in Java/.NET; each still carries the IDs.
- **Track A: UC-001.14 (ERP unavailable) in all three languages.** Any ERP failure (error status, unreachable, timeout, malformed body; a 404 product is still a validation error) throws `ErpUnavailableException` (Java, .NET, TS multitier) / `ErpUnavailableError` (TS monolith), which the global handler maps to a **503** problem detail with `detail` "Product information can't be retrieved right now" (type `…/errors/service-unavailable`), in the monoliths and multitier backends (`backend-clean-java` untouched: it already maps gateway failures to its own 502). The UIs already render `detail`, so no frontend change. Test kit: `given().erp().unavailable()` → `ErpDriver.returnsUnavailable()`; the stub driver registers a WireMock 503 on `GET /erp/api/.*` that outranks the per-resource stubs (priority 1 vs default in Java/TS, 0 vs 1 in .NET); the real driver throws (the real ERP can't be taken down). Test: `PlaceOrderNegativeIsolatedTest.shouldRejectOrderWhenErpIsUnavailable` (latest tree only, like the other UC tags), UI + API channels, tagged `@Tag("UC-001.14")` / `[Trait("tag", "UC-001.14")]` / `criteria('UC-001.14')`; the JUnit XML of all three carries `<property name="tag" value="UC-001.14"/>`.

## Decisions (resolved 2026-11-06 with the user)

1. **Criteria format:** Gherkin, as the articles show it. The parser gets taught Gherkin (track C); shop doesn't change gh-optivem directly.
2. **Deleting criteria once their tests pass:** keep the criteria in the shop for now. Tell the substack side about the conflict with the article's "delete" paragraph.
3. **Criteria are shared:** `docs/use-cases/` serves all three languages. Only the tests and the gate's `results` input are Java-only, and `results` is already a list (`StringList` in gh-optivem `internal/spec/config.go`).

## Track A scope decision (resolved 2026-11-07: approved)

- **Track A scope:** Approved production error handling for an unreachable ERP: a dedicated exception plus a **503** with the message "product information can't be retrieved right now", shown in the UI, in the Java, .NET and TypeScript monoliths and multitier backends. Plus an "ERP unavailable" test kit step in all three languages. Recommendation: approve. Without it, UC-001.14 can't have a meaningful test, and the gate (track D) can never reach 14 of 14.

## Parallel tracks

```
A (shop: UC-001.14, production code + test kit) ✅ done ─┐
B (shop: .NET/TS criterion tags) ✅ done         ─┼─▶ D (shop: re-enable gate, all languages, verify)
C (gh-optivem: parse Gherkin + release)          ─┘
```

A, B and C are independent. A and B are done (see Done). D waits for C.

### ▶ Next executable step (resume here)

Track C is external: run it in a gh-optivem session (parse `@UC-NNN.M` Gherkin scenarios, then publish a release). Once that release is out, Track D (D1) re-enables the gate here.

### Track C — gh-optivem reads Gherkin criteria (other repo)

> ⚠️ Shop never changes gh-optivem. Run this track in a **gh-optivem session**, not from shop. A shop executor marks it as external and waits for the release.

- [ ] C1: In `internal/spec/usecase.go`, also read fenced ```` ```gherkin ```` blocks in use-case markdown. Each `Scenario:` tagged `@UC-NNN.M` becomes a criterion: the ID comes from the tag and the text from the scenario title. Ignore other tags (e.g. `@narrative-4a`), or keep them as metadata. Keep bullet support, and reuse the scenario parsing in `internal/spec/storymerge.go`. Cover these cases in `usecase_test.go`, using shop's UC-001 as a testdata fixture: duplicate IDs across both formats, a scenario with no UC tag, and a scenario with two UC tags. Consider making `validate` warn when a use-case file has zero criteria, since that silent pass is what hid this problem.
- [ ] C1b (requested 2026-11-07 by `plans/20261107-1638-storefront-use-cases-rules-acceptance.md`): support a folder per use case. Shop is moving to `docs/shared/specs/use-cases/UC-NNN-<name>/{use-case.md, business-rules.md, acceptance-criteria.md}`, with an index `use-cases/README.md` that has no front matter. `LoadDir` should read `<specDir>/*/acceptance-criteria.md`; that file carries the front matter (`id`, `title`) and the Gherkin. Skip top-level `.md` files such as `README.md` (or at least don't require front matter there). Keep reading flat `<specDir>/*.md` if that's cheap, for other repos.
- [ ] C2: Publish a gh-optivem release.

### Track D — Re-enable the gate for all languages (shop; after A, B and C)

- [ ] D1: Uncomment the `spec-gate` step in `.github/workflows/_acceptance-stage-docker-compose.yml` and delete the DISABLED comment above it.
- [ ] D2: Add the .NET and TypeScript result globs to `results:` in `spec.yaml`. Decide whether the gate reports per language or pooled. Pooled lets one language hide another's gap, so check how gh-optivem merges results first.
- [ ] D3: Extend the `spec-gate` step's `if:` to all languages.
- [ ] D4: Verify with the new gh-optivem. Run `gh optivem spec validate`, then `gh optivem spec status` after an acceptance run in each language. Check that all 14 criteria (UC-001.1 to UC-001.14) are found and coverage is at least the `spec.yaml` threshold (90%). Push, and confirm that the acceptance stage passes the `spec-gate` step for monolith Java, .NET and TypeScript.

## Out of scope

- Gift wrapping, the story-to-criterion mapping, and the illustrative legacy examples (Track Shipment, the wrong draft of 6a, the year-end check). The articles flag these as illustrative SHOP WORK, not as things the shop must have.
- Use cases other than Place Order. The shop has only UC-001.
