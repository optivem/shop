# 2026-11-06 15:33:50 UTC — Place Order use case matches the Substack articles, with a spec gate that checks all three languages

> 🤖 **Picked up by agent** — `8e8cd7062d0f` at `2026-11-07T11:12:16Z`

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

## Decisions (resolved 2026-11-06 with the user)

1. **Criteria format:** Gherkin, as the articles show it. The parser gets taught Gherkin (track C); shop doesn't change gh-optivem directly.
2. **Deleting criteria once their tests pass:** keep the criteria in the shop for now. Tell the substack side about the conflict with the article's "delete" paragraph.
3. **Criteria are shared:** `docs/use-cases/` serves all three languages. Only the tests and the gate's `results` input are Java-only, and `results` is already a list (`StringList` in gh-optivem `internal/spec/config.go`).

## Track A scope decision (resolved 2026-11-07: approved)

- **Track A scope:** Approved production error handling for an unreachable ERP: a dedicated exception plus a **503** with the message "product information can't be retrieved right now", shown in the UI, in the Java, .NET and TypeScript monoliths and multitier backends. Plus an "ERP unavailable" test kit step in all three languages. Recommendation: approve. Without it, UC-001.14 can't have a meaningful test, and the gate (track D) can never reach 14 of 14.

## Parallel tracks

```
A (shop: UC-001.14, production code + test kit) ─┐
B (shop: .NET/TS criterion tags) ✅ done         ─┼─▶ D (shop: re-enable gate, all languages, verify)
C (gh-optivem: parse Gherkin + release)          ─┘
```

A and C are independent. B is done (see Done); A adds the `UC-001.14` tests already tagged in all three languages. D waits for A and C.

### ▶ Next executable step (resume here)

Next is Track A (A1), reusing the tag mechanism recorded under Done (Track B) for the new UC-001.14 tests in .NET and TypeScript. Track C is external (gh-optivem session); D waits for A and C's release.

### Track A — UC-001.14 "ERP unavailable" (shop, all languages)

Today an unreachable ERP makes `ErpGateway.getProductDetails` throw `IllegalStateException`, and `GlobalExceptionHandler` turns that into a generic 500 ("An unexpected error occurred...").

- [ ] A1: Production code: add a dedicated exception plus a 503 handler with the "product information can't be retrieved right now" message, and make the UI show it. Do this in the Java, .NET and TypeScript monoliths and multitier backends (and frontends where the message is rendered).
- [ ] A2: Test kit: add an "ERP unavailable" step through `ErpDriver`, `ErpStubDriver` (a WireMock 503 or fault), `ErpRealDriver`, and the use-case and scenario DSL, in all three languages.
- [ ] A3: Add the acceptance test, marked `@Isolated`, in `PlaceOrderNegativeTest` or `PlaceOrderNegativeIsolatedTest` (whichever already stubs the ERP). Tag it `@Tag("UC-001.14")` in Java, and with the same ID in .NET and TypeScript, using the mechanism under Done (Track B). First check that no test anywhere is already tagged `UC-001.14`.
- [ ] A4: Verify: `./compile-all.sh`, then the `--sample` system tests for each language (see CLAUDE.md).

### Track C — gh-optivem reads Gherkin criteria (other repo)

> ⚠️ Shop never changes gh-optivem. Run this track in a **gh-optivem session**, not from shop. A shop executor marks it as external and waits for the release.

- [ ] C1: In `internal/spec/usecase.go`, also read fenced ```` ```gherkin ```` blocks in use-case markdown. Each `Scenario:` tagged `@UC-NNN.M` becomes a criterion: the ID comes from the tag and the text from the scenario title. Ignore other tags (e.g. `@narrative-4a`), or keep them as metadata. Keep bullet support, and reuse the scenario parsing in `internal/spec/storymerge.go`. Cover these cases in `usecase_test.go`, using shop's UC-001 as a testdata fixture: duplicate IDs across both formats, a scenario with no UC tag, and a scenario with two UC tags. Consider making `validate` warn when a use-case file has zero criteria, since that silent pass is what hid this problem.
- [ ] C2: Publish a gh-optivem release.

### Track D — Re-enable the gate for all languages (shop; after A, B and C)

- [ ] D1: Uncomment the `spec-gate` step in `.github/workflows/_acceptance-stage-docker-compose.yml` and delete the DISABLED comment above it.
- [ ] D2: Add the .NET and TypeScript result globs to `results:` in `spec.yaml`. Decide whether the gate reports per language or pooled. Pooled lets one language hide another's gap, so check how gh-optivem merges results first.
- [ ] D3: Extend the `spec-gate` step's `if:` to all languages.
- [ ] D4: Verify with the new gh-optivem. Run `gh optivem spec validate`, then `gh optivem spec status` after an acceptance run in each language. Check that all 14 criteria (UC-001.1 to UC-001.14) are found and coverage is at least the `spec.yaml` threshold (90%). Push, and confirm that the acceptance stage passes the `spec-gate` step for monolith Java, .NET and TypeScript.

## Out of scope

- Gift wrapping, the story-to-criterion mapping, and the illustrative legacy examples (Track Shipment, the wrong draft of 6a, the year-end check). The articles flag these as illustrative SHOP WORK, not as things the shop must have.
- Use cases other than Place Order. The shop has only UC-001.
