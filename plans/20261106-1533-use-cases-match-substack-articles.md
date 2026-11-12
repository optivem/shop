# 2026-11-06 15:33:50 UTC — Place Order use case matches the Substack articles, with a spec gate that checks all three languages

> Merged on 2026-11-06 from three plans: this one, `20261106-2030-spec-gate-dotnet-typescript-tags.md` and `20261106-2030-spec-gate-read-gherkin-criteria.md`. The work is split into **parallel tracks** for autonomous execution with subagents (one subagent per track; tracks A, B and C don't touch the same files). Track D joins them at the end.

## TL;DR

**Why:** Two paid Substack articles use Place Order as the reader's worked example: `articles/published/2026-11-06-PAID-ATDD-acceptance-test-coverage-with-ai.md` and `articles/drafts/next/2026-11-06-PAID-ATDD-use-cases-stay-user-stories-go.md` in the `optivem/substack` repo. They show a full use case narrative and Gherkin acceptance criteria. The shop now matches them, but that broke the spec gate: gh-optivem's parser (`internal/spec/usecase.go`, `criterionPattern`) only reads bullet lines (`- UC-001.N: text`), so with Gherkin it finds zero criteria and passes without checking anything (`gh optivem spec validate` returns `OK`, exit 0). The `spec-gate` step is commented out for now. Also, only the Java tests carry `UC-001.N` tags, and `spec.yaml` reads only Java JUnit XML, so the gate can't say anything about .NET or TypeScript.
**End result:** The Place Order narrative and Gherkin criteria match the articles (done). Every criterion, UC-001.14 (ERP unavailable) included, has a tagged acceptance test in Java, .NET and TypeScript. gh-optivem reads `@UC-NNN.M`-tagged Gherkin scenarios. The shop `spec-gate` step is back on, finds all 14 criteria, and reads the results of all three languages.

## Done

- `docs/shared/specs/use-cases/UC-001-place-order/use-case-narrative.md` holds the narrative as the articles show it (primary actor Customer, secondary actors ERP and Clock, 9-step main success scenario, extensions 2a, 3a, 4a, 4b, 5a, 5b, 6a), saying ERP everywhere, never SAP.
- The criteria are Gherkin scenarios tagged `@UC-001.N` plus the extension tag where one applies: 2a → UC-001.12, 3a → UC-001.13, 4a → UC-001.8, 4b → UC-001.10, 5a → UC-001.14, 5b → UC-001.9, 6a → UC-001.11.
- The `spec-gate` step in `.github/workflows/_acceptance-stage-docker-compose.yml` is commented out, with a pointer to this plan. `spec.yaml` stays.
- **Track B: .NET and TypeScript tests carry the UC IDs** (one for one with Java, including the e2e `PlaceOrderPositiveTest.shouldPlaceOrder` → UC-001.1; no UC-001.14 yet). All three languages emit the same JUnit shape that gh-optivem `internal/spec/junit.go` reads: `<testcase classname=… name=…><properties><property name="tag" value="UC-001.N"/></properties></testcase>` (it accepts property name `tag` or `criterion`, value `UC-NNN.M`, optional `@`).
  - **.NET:** `[Trait("tag", "UC-001.N")]` on the test method (one per ID). The `JunitXml.TestLogger` package (added to `SystemTests.csproj`) writes every xUnit trait as a testcase property. Each latest suite in `system-test/dotnet/tests.yaml` adds `--logger 'junit;LogFilePath=TestResults/junit/<suite-id>.xml'`, so results land in `system-test/dotnet/SystemTests/TestResults/junit/*.xml`. Names look like `ShouldX(channel: API)`; gh-optivem's `()` stripping merges the channels.
  - **TypeScript:** `test('name', criteria('UC-001.N', …), async …)` with the helper `tests/commons/criteria.ts`, which turns each ID into a `{ type: 'tag', description: id }` annotation. `test.each` / `test.eachAlsoFirstRow` take the same optional details argument (`(name, criteria(…), fn)`; the shop facade `src/testkit/.../playwright/bindTestEach.ts` passes it to every row). Playwright's built-in `junit` reporter (added in `playwright.config.ts`, `outputDir: 'junit-results'`) writes annotations as testcase properties. Each latest suite in `system-test/typescript/tests.yaml` sets `PLAYWRIGHT_JUNIT_OUTPUT_NAME: <suite-id>.xml`, so results land in `system-test/typescript/junit-results/*.xml` (gitignored; not under `test-results/`, which Playwright wipes per run). Note for D: TS testcase names are `[API Channel] › shouldX` (and parameterised rows carry `_param=value`), so channels/rows don't merge into one test as in Java/.NET; each still carries the IDs.
- **Track A: UC-001.14 (ERP unavailable) in all three languages.** Any ERP failure (error status, unreachable, timeout, malformed body; a 404 product is still a validation error) throws `ErpUnavailableException` (Java, .NET, TS multitier) / `ErpUnavailableError` (TS monolith), which the global handler maps to a **503** problem detail with `detail` "Product information can't be retrieved right now" (type `…/errors/service-unavailable`), in the monoliths and multitier backends (`backend-clean-java` untouched: it already maps gateway failures to its own 502). The UIs already render `detail`, so no frontend change. Test kit: `given().erp().unavailable()` → `ErpDriver.returnsUnavailable()`; the stub driver registers a WireMock 503 on `GET /erp/api/.*` that outranks the per-resource stubs (priority 1 vs default in Java/TS, 0 vs 1 in .NET); the real driver throws (the real ERP can't be taken down). Test: `PlaceOrderNegativeIsolatedTest.shouldRejectOrderWhenErpIsUnavailable` (latest tree only, like the other UC tags), UI + API channels, tagged `@Tag("UC-001.14")` / `[Trait("tag", "UC-001.14")]` / `criteria('UC-001.14')`; the JUnit XML of all three carries `<property name="tag" value="UC-001.14"/>`.

## Decisions (resolved 2026-11-06 with the user)

1. **Criteria format:** Gherkin, as the articles show it. The parser gets taught Gherkin (track C); shop doesn't change gh-optivem directly.
2. **Deleting criteria once their tests pass:** keep the criteria in the shop for now. Tell the substack side about the conflict with the article's "delete" paragraph.
3. **Criteria are shared:** `docs/shared/specs/use-cases/` serves all three languages. Only the tests and the gate's `results` input are Java-only, and `results` is already a list (`StringList` in gh-optivem `internal/spec/config.go`).

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

D4: verify after the test phase. D1–D3 are done: the `spec-gate` step runs again for monolith Java, .NET and TypeScript (latest suite set), **per language**: `gh optivem spec check --mode lenient --config spec-<test-language>.yaml`. `spec-java.yaml`, `spec-dotnet.yaml` and `spec-typescript.yaml` each read only their language's JUnit XML; `spec.yaml` keeps the shared settings without results and is what `spec validate` uses. `spec validate` (2026-11-12, gh-optivem v1.6.108): 12 use cases valid, 53 criteria gated (UC-001 16, UC-004 9, UC-006 10, UC-007 7, UC-008 11), 7 planned use cases not gated.

### Track C — gh-optivem reads Gherkin criteria (other repo)

> ⚠️ Shop never changes gh-optivem. Run this track in a **gh-optivem session**, not from shop. A shop executor marks it as external and waits for the release. The gh-optivem work is written up as `plans/move-to-gh-optivem/20261107-1300-spec-parse-gherkin-criteria.md`; move it into gh-optivem and run it there.

- Track C done (2026-11-12): C1, C1b and C1c shipped in gh-optivem `7399d6f2`, released as `v1.6.108` (accepted as C2 by the user).

### Track D — Re-enable the gate for all languages (shop; after A, B and C)

- [ ] D4 (after the test phase; recount if a use case flips to `implemented` first): after an acceptance run in each language, run `gh optivem spec status --config spec-<language>.yaml` for java, dotnet and typescript **separately**. In each, every criterion of the `implemented` use cases (53 today: UC-001 16, UC-004 9, UC-006 10, UC-007 7, UC-008 11) must be found and Verified, none Failing, and no use case with `status: planned` may count. Also check the result globs match real files (the gate exits 2 on "no JUnit XML files matched"). Then push and confirm that the acceptance stage passes the `spec-gate` step for monolith Java, .NET and TypeScript. **Threshold gap (gh-optivem v1.6.108):** the 90% `threshold` applies only in `--mode strict`, and strict also blocks on *every* unverified in-scope criterion, so it works as 100% in practice. The step runs `--mode lenient` (validation errors and Failing criteria only), so no coverage percent is enforced per language yet. Decide in D4: (a) **recommended:** switch the step to `--mode strict` once `spec status --mode strict` passes in all three languages, because only that makes each language prove its own coverage; or (b) keep lenient and ask gh-optivem for a mode that enforces only the threshold.

## Out of scope

- Gift wrapping, the story-to-criterion mapping, and the illustrative legacy examples (Track Shipment, the wrong draft of 6a, the year-end check). The articles flag these as illustrative SHOP WORK, not as things the shop must have.
- Use cases other than Place Order. The shop has only UC-001.
