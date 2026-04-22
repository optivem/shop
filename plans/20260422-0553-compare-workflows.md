# 20260422-0553 — Workflow Diff Plan

Architecture: both
Stage: all

## Differences

### DIFF-1: commit-stage — `Run Unit Tests` TODO in .NET and TypeScript but implemented in Java (monolith)

**Stage:** commit-stage
**Scope:** monolith — Java vs .NET vs TypeScript

**Files:**
- `.github/workflows/monolith-java-commit-stage.yml:81-83` — `./gradlew test` (implemented)
- `.github/workflows/monolith-dotnet-commit-stage.yml:80-82` — `echo "TODO - not yet implemented"`
- `.github/workflows/monolith-typescript-commit-stage.yml:81-83` — `echo "TODO - not yet implemented"`

**Details:**
Java runs real unit tests via `./gradlew test` under `system/monolith/java`. .NET and TypeScript are placeholder echo statements with `# TODO: Implement` markers. All three workflows still declare the step (so step ordering is preserved), but only Java actually executes tests.

**Recommendation:**
Implement the step in both `.NET` and `TypeScript` using Java as the reference template:
- `monolith-dotnet-commit-stage.yml`: replace with `dotnet test` (filtered to unit tests only, e.g. `--filter "FullyQualifiedName~UnitTests"`) and drop the TODO comment.
- `monolith-typescript-commit-stage.yml`: replace with `npm test` (matching what `multitier-backend-typescript-commit-stage.yml` already does) and drop the TODO comment.
Keep `working-directory` pointing at the per-language system folder.

---

### DIFF-2: commit-stage — `Run Unit Tests` TODO in Java and .NET but implemented in TypeScript (multitier backend)

**Stage:** commit-stage
**Scope:** multitier — Java vs .NET vs TypeScript (backend)

**Files:**
- `.github/workflows/multitier-backend-java-commit-stage.yml:88-90` — `echo "TODO - not yet implemented"`
- `.github/workflows/multitier-backend-dotnet-commit-stage.yml:87-89` — `echo "TODO - not yet implemented"`
- `.github/workflows/multitier-backend-typescript-commit-stage.yml:88-90` — `npm test` (implemented)

**Details:**
In multitier, the situation is the opposite of the monolith: TypeScript has a real unit-test invocation (`npm test` in `system/multitier/backend-typescript`), while Java and .NET are still `echo "TODO - not yet implemented"` placeholders. This creates an inconsistent signal — the pipeline passes even when Java/.NET unit tests never run.

**Recommendation:**
Implement the step in both Java and .NET using TypeScript as the reference template:
- `multitier-backend-java-commit-stage.yml`: replace with `./gradlew test` (mirroring the monolith Java approach) in `system/multitier/backend-java`.
- `multitier-backend-dotnet-commit-stage.yml`: replace with `dotnet test --filter "FullyQualifiedName~UnitTests"` in `system/multitier/backend-dotnet`.
Drop the `# TODO: Implement` markers once implemented.

---

### DIFF-3: commit-stage — `Run Narrow Integration Tests`, `Run Component Tests`, `Run Contract Tests` all TODO across every language

**Stage:** commit-stage
**Scope:** monolith and multitier — Java + .NET + TypeScript (+ frontend-react)

**Files (monolith):**
- `.github/workflows/monolith-java-commit-stage.yml:86-95` — three TODO placeholders
- `.github/workflows/monolith-dotnet-commit-stage.yml:85-94` — three TODO placeholders
- `.github/workflows/monolith-typescript-commit-stage.yml:86-95` — three TODO placeholders

**Files (multitier backend + frontend):**
- `.github/workflows/multitier-backend-java-commit-stage.yml:92-102` — three TODO placeholders
- `.github/workflows/multitier-backend-dotnet-commit-stage.yml:91-101` — three TODO placeholders
- `.github/workflows/multitier-backend-typescript-commit-stage.yml:92-102` — three TODO placeholders
- `.github/workflows/multitier-frontend-react-commit-stage.yml:92-102` — three TODO placeholders (plus `Run Unit Tests` at 88-90 also TODO)

**Details:**
These three test categories are placeholders in every language in every architecture. They do not cause pipeline failures but they mean the commit stage is not exercising narrow integration / component / contract tests anywhere. This is consistent across the triplet, but consistent with zero implementation.

**Recommendation:**
This is an architecture-wide gap rather than a triplet inconsistency. Keep the TODO rows (so step names stay stable across languages) but track implementation as a separate cross-language rollout. Pick the language with the most established testing story first (likely Java in monolith) and use it as the reference once implemented.

---

### DIFF-4: commit-stage — job-level step ordering: `Read Target Component Version` placed before setup/build in multitier, but after Sonar in monolith

**Stage:** commit-stage
**Scope:** monolith vs multitier — all languages

**Files:**
- `.github/workflows/monolith-java-commit-stage.yml:106` — `Read Target Component Version` is step #11 (after `Run Code Analysis`, before `Build Artifact`)
- `.github/workflows/monolith-dotnet-commit-stage.yml:109` — same late position
- `.github/workflows/monolith-typescript-commit-stage.yml:112` — same late position
- `.github/workflows/multitier-backend-java-commit-stage.yml:72` — `Read Target Component Version` is step #3 (right after `Verify Built SHA Is On Main`, before `Setup Java and Gradle`)
- `.github/workflows/multitier-backend-dotnet-commit-stage.yml:72` — same early position
- `.github/workflows/multitier-backend-typescript-commit-stage.yml:72` — same early position
- `.github/workflows/multitier-frontend-react-commit-stage.yml:72` — same early position

**Details:**
Each architecture is internally consistent across its language triplet, but the two architectures disagree: multitier reads the target version at the very top of the `run` job so it can be exposed via `component-version` as a job output; monolith reads it only just before packaging. Both architectures also differ in job outputs:
- monolith `run` → `image-digest-url`
- multitier `run` → `component-version`, `image-version-url`

**Recommendation:**
This is cross-architecture rather than a triplet inconsistency; flag for verification of intent. If the two architectures are supposed to share a shape, move the monolith `Read Target Component Version` step up to immediately after `Verify Built SHA Is On Main` and add matching `component-version` / `image-version-url` outputs on the `run` job so downstream `pipeline-*` drivers can consume a uniform contract. Recommended: align monolith to the multitier shape, because it produces a more useful output contract (the version is known to downstream jobs without needing to parse the image URL).

---

### DIFF-5: commit-stage — summary uses `image-digest-url` in monolith but `image-version-url` in multitier

**Stage:** commit-stage
**Scope:** monolith vs multitier — all languages

**Files:**
- `.github/workflows/monolith-java-commit-stage.yml:165` — `` `${{ needs.run.outputs.image-digest-url }}` ``
- `.github/workflows/monolith-dotnet-commit-stage.yml:168` — same
- `.github/workflows/monolith-typescript-commit-stage.yml:171` — same
- `.github/workflows/multitier-backend-java-commit-stage.yml:165` — `` `${{ needs.run.outputs.image-version-url }}` ``
- `.github/workflows/multitier-backend-dotnet-commit-stage.yml:168` — same
- `.github/workflows/multitier-backend-typescript-commit-stage.yml:171` — same
- `.github/workflows/multitier-frontend-react-commit-stage.yml:171` — same

**Details:**
The summary step on `summary` job renders a different artifact URL depending on architecture. Within each architecture all three languages agree, so this is not a triplet issue — but it is a cross-architecture inconsistency in what the stage summary reports.

**Recommendation:**
Pair this with DIFF-4. Once monolith exposes `image-version-url` as a job output, standardize the summary to use `image-version-url` in both architectures. Recommended because the versioned URL is more human-useful than the digest URL for a commit-stage artifact.

---

### DIFF-6: acceptance-stage — `Debug Smoke Discovery` diagnostic step exists only in multitier-typescript

**Stage:** acceptance-stage
**Scope:** multitier — TypeScript only

**Files:**
- `.github/workflows/multitier-typescript-acceptance-stage.yml:180-207` — `Debug Smoke Discovery` step with `# TODO(debug-smoke-0-tests)` marker
- `.github/workflows/multitier-java-acceptance-stage.yml` — no such step
- `.github/workflows/multitier-dotnet-acceptance-stage.yml` — no such step
- `.github/workflows/monolith-typescript-acceptance-stage.yml` — no such step
- `.github/workflows/monolith-java-acceptance-stage.yml` — no such step
- `.github/workflows/monolith-dotnet-acceptance-stage.yml` — no such step

**Details:**
`multitier-typescript-acceptance-stage.yml` contains a large ad-hoc `Debug Smoke Discovery` diagnostic step (running `pwd`, `ls -laR`, `npx playwright test --list`, etc.) tagged with a `TODO(debug-smoke-0-tests)` marker saying it is "temporary diagnostic scaffolding — remove once the 'Total tests: 0' failure on CI is resolved". No other workflow in the repo contains this step.

**Recommendation:**
Decide whether the underlying "Total tests: 0" issue is resolved. If yes, remove lines 180-207 from `multitier-typescript-acceptance-stage.yml` so the workflow matches its Java/.NET siblings. If no, keep it but open a tracking issue and reference it in the TODO comment. Recommended: remove if the diagnostics have already served their purpose; otherwise leave as-is.

---

### DIFF-7: acceptance-stage — per-language test-project compile step is Java-only and .NET-only (TypeScript lacks it)

**Stage:** acceptance-stage
**Scope:** monolith and multitier — Java vs .NET vs TypeScript

**Files (monolith):**
- `.github/workflows/monolith-java-acceptance-stage.yml:179-181` — `Compile System Tests` → `./gradlew clean compileJava compileTestJava`
- `.github/workflows/monolith-dotnet-acceptance-stage.yml:169-171` — `Build Test Project` → `dotnet build`
- `.github/workflows/monolith-typescript-acceptance-stage.yml` — no equivalent step (Playwright compiles on demand)

**Files (multitier):**
- `.github/workflows/multitier-java-acceptance-stage.yml:180-182` — `Compile System Tests` → `./gradlew clean compileJava compileTestJava`
- `.github/workflows/multitier-dotnet-acceptance-stage.yml:170-172` — `Build Test Project` → `dotnet build`
- `.github/workflows/multitier-typescript-acceptance-stage.yml` — no equivalent step

**Details:**
Java has a "Compile System Tests" step (name #1), .NET has a "Build Test Project" step (name #2), and TypeScript has no up-front compile step at all. This is a mix of step-name mismatch (Java vs .NET) and missing-step (TypeScript). The TypeScript case is technically valid because `npx playwright test` compiles sources lazily, but losing the up-front compile step makes failures slower to surface and the step list harder to eyeball as aligned.

**Recommendation:**
- Standardize the step name across Java and .NET. Recommended: rename both to `Compile System Tests` (more descriptive than the .NET `Build Test Project`). Update `monolith-dotnet-acceptance-stage.yml:169`, `multitier-dotnet-acceptance-stage.yml:170`, `monolith-dotnet-acceptance-stage-cloud.yml` (all instances), `monolith-dotnet-acceptance-stage-legacy.yml:153`, `multitier-dotnet-acceptance-stage-cloud.yml` (all instances), `multitier-dotnet-acceptance-stage-legacy.yml:154`.
- For TypeScript, decide whether to add a `Compile System Tests` step that runs `npx tsc --noEmit` (or `npm run build` in `system-test/typescript`) before running the first test. Recommended: add the step — it makes failures fail-fast and aligns the triplet. Apply to `monolith-typescript-acceptance-stage.yml` and `multitier-typescript-acceptance-stage.yml` (insert right after `Install Playwright System Dependencies`).

---

### DIFF-8: acceptance-stage-legacy — same `Compile System Tests` / `Build Test Project` / missing pattern as DIFF-7

**Stage:** acceptance-stage-legacy
**Scope:** monolith and multitier — Java vs .NET vs TypeScript

**Files (monolith):**
- `.github/workflows/monolith-java-acceptance-stage-legacy.yml:163-165` — `Compile System Tests` present
- `.github/workflows/monolith-dotnet-acceptance-stage-legacy.yml:153-155` — `Build Test Project` present
- `.github/workflows/monolith-typescript-acceptance-stage-legacy.yml` — no equivalent step

**Files (multitier):**
- `.github/workflows/multitier-java-acceptance-stage-legacy.yml:163-165` — `Compile System Tests` present
- `.github/workflows/multitier-dotnet-acceptance-stage-legacy.yml:153-155` — `Build Test Project` present
- `.github/workflows/multitier-typescript-acceptance-stage-legacy.yml` — no equivalent step

**Details:**
Identical shape to DIFF-7 but in the legacy variant. Java uses `Compile System Tests`, .NET uses `Build Test Project`, TypeScript has no compile step.

**Recommendation:**
Apply the same fixes as DIFF-7 to the four legacy workflow files: rename .NET step to `Compile System Tests`, and add a TypeScript compile step.

---

### DIFF-9: acceptance-stage-cloud — .NET repeats `Setup .NET` + `Cache NuGet Packages` + `Build Test Project` in every test job; Java and TypeScript do not have an equivalent up-front compile step

**Stage:** acceptance-stage (cloud)
**Scope:** monolith and multitier — Java vs .NET vs TypeScript

**Files:**
- `.github/workflows/monolith-dotnet-acceptance-stage-cloud.yml` — `Build Test Project` appears in every test-* job (lines 221, 253, 285, 318, 368, 401, 451, 483, 515, 547, 579)
- `.github/workflows/multitier-dotnet-acceptance-stage-cloud.yml` — same pattern (11 occurrences)
- `.github/workflows/monolith-java-acceptance-stage-cloud.yml` — no up-front compile step in test-* jobs; Gradle compiles on first `test` invocation
- `.github/workflows/multitier-java-acceptance-stage-cloud.yml` — same pattern as java monolith
- `.github/workflows/monolith-typescript-acceptance-stage-cloud.yml` — no up-front compile step
- `.github/workflows/multitier-typescript-acceptance-stage-cloud.yml` — same

**Details:**
In the cloud variants, every test job is a standalone `runs-on: ubuntu-latest` job that needs its own language runtime. .NET compiles explicitly in each test job via `Build Test Project`; Java and TypeScript let the test runner compile implicitly. This is more a reflection of how each language's test runner works than a true inconsistency, but the step list is visibly different.

**Recommendation:**
Verify intent. If .NET is doing it only because the Playwright helper script (`playwright.ps1`) needs to exist before `Install Playwright Browsers`, that's a legitimate need. Mark this difference as architecturally necessary and document it in a comment at the top of the .NET cloud workflows. No code changes recommended unless the team wants to normalize step names (e.g. rename to `Compile System Tests` in .NET to at least align names with the non-cloud variants — see DIFF-7).

---

### DIFF-10: commit-stage — missing newline/space in `run:` block for .NET Sonar scanner

**Stage:** commit-stage
**Scope:** monolith and multitier — .NET only

**Files:**
- `.github/workflows/monolith-dotnet-commit-stage.yml:104` — Sonar `begin` command is a single enormous one-line invocation with 7 ignore criteria
- `.github/workflows/multitier-backend-dotnet-commit-stage.yml:111` — Sonar `begin` command is a single enormous one-line invocation with 5 ignore criteria
- `.github/workflows/monolith-java-commit-stage.yml:103` — compact `./gradlew build sonar --info`
- `.github/workflows/multitier-backend-java-commit-stage.yml:110` — compact `./gradlew build sonar --info`
- `.github/workflows/monolith-typescript-commit-stage.yml:101-110` — multi-line `uses: SonarSource/sonarcloud-github-action@v5` with `args: >`
- `.github/workflows/multitier-backend-typescript-commit-stage.yml:108-117` — same pattern

**Details:**
These are all legitimate language-specific Sonar setups (expected differences), but the .NET one-liner is hard to read and the multitier .NET version has 5 ignore criteria vs monolith .NET's 7 — different rule sets. Specifically:
- monolith .NET: ignores `csharpsquid:S2699, S1118, S2068, css:S4654, csharpsquid:S1186, S1075, S3267` (7 rules)
- multitier backend .NET: ignores `csharpsquid:S2699, S1118, S2068, S1075, S3267` (5 rules — lacks `css:S4654` and `csharpsquid:S1186`)

**Recommendation:**
This is a deliberate-looking per-project Sonar configuration, not a workflow bug. The missing `css:S4654` and `csharpsquid:S1186` rules in multitier backend probably reflect that the backend has no CSS and no Razor `Pages/**`. Mark as expected and verify with author intent; no action recommended unless those projects have started including CSS/Pages.

---

### DIFF-11: Sonar project organization inputs differ between per-language monolith/multitier Sonar invocations

**Stage:** commit-stage
**Scope:** monolith and multitier — Java, .NET, TypeScript (project keys only)

**Files:**
- `.github/workflows/monolith-java-commit-stage.yml:103` — `./gradlew build sonar --info` (project keys configured in `build.gradle`, not workflow)
- `.github/workflows/monolith-dotnet-commit-stage.yml:104` — `/k:"optivem_shop-monolith-dotnet" /n:"shop-monolith-dotnet" /o:"optivem"`
- `.github/workflows/monolith-typescript-commit-stage.yml:107-110` — `-Dsonar.projectKey=optivem_shop-monolith-typescript -Dsonar.projectName=shop-monolith-typescript -Dsonar.organization=optivem`
- `.github/workflows/multitier-backend-java-commit-stage.yml:110` — `./gradlew build sonar --info`
- `.github/workflows/multitier-backend-dotnet-commit-stage.yml:111` — `/k:"optivem_shop-multitier-backend-dotnet" /n:"shop-multitier-backend-dotnet" /o:"optivem"`
- `.github/workflows/multitier-backend-typescript-commit-stage.yml:114-117` — `-Dsonar.projectKey=optivem_shop-multitier-backend-typescript …`
- `.github/workflows/multitier-frontend-react-commit-stage.yml:114-117` — `-Dsonar.projectKey=optivem_shop-multitier-frontend-react …`

**Details:**
Java configures Sonar project keys in the Gradle build, not in the workflow. .NET and TypeScript configure them in the workflow. Project keys / names follow a consistent `optivem_shop-{arch}-{lang}` pattern across .NET and TS, so the project naming is aligned.

**Recommendation:**
Expected difference — leave as-is. Java's reliance on Gradle configuration is idiomatic and doesn't need to be duplicated in the workflow.

---

### DIFF-12: acceptance-stage — Java `Setup Java and Gradle` step order relative to Playwright install differs from .NET

**Stage:** acceptance-stage
**Scope:** monolith and multitier — Java vs .NET

**Files:**
- `.github/workflows/monolith-java-acceptance-stage.yml:157-161` — `Setup Java and Gradle` is step #5 in the test block (before Playwright cache)
- `.github/workflows/monolith-dotnet-acceptance-stage.yml:157-161` — `Setup .NET` also step #5 (before NuGet cache, then `Build Test Project`, then Playwright)
- `.github/workflows/monolith-typescript-acceptance-stage.yml:157-161` — `Setup Node` also step #5

**Details:**
All three languages put the language setup at the same relative position. The difference is that .NET injects two extra steps (`Cache NuGet Packages`, `Build Test Project`) between setup and Playwright cache; Java and TypeScript do not. This is related to DIFF-7 — renaming the .NET step to `Compile System Tests` and adding one to TypeScript will bring them closer to parity.

**Recommendation:**
Already covered by DIFF-7 + DIFF-9. No separate action needed.

---

### DIFF-13: acceptance-stage — `CHANNEL` env value case differs between Java/.NET (uppercase) and TypeScript (lowercase)

**Stage:** acceptance-stage, acceptance-stage-legacy, acceptance-stage-cloud
**Scope:** monolith and multitier — Java/.NET vs TypeScript

**Files:**
- `.github/workflows/monolith-java-acceptance-stage.yml` — uses `-Dchannel=API` and `-Dchannel=UI` (uppercase)
- `.github/workflows/monolith-dotnet-acceptance-stage.yml` — uses `CHANNEL: API` and `CHANNEL: UI` (uppercase)
- `.github/workflows/monolith-typescript-acceptance-stage.yml` — uses `CHANNEL: api` and `CHANNEL: ui` (lowercase)
- Same pattern across `multitier-*-acceptance-stage.yml`, `*-acceptance-stage-legacy.yml`, `*-acceptance-stage-cloud.yml`.

**Details:**
The `CHANNEL` environment variable is passed as `API`/`UI` in Java and .NET but as `api`/`ui` in TypeScript. This is a subtle but real inconsistency; if any shared tooling or log grep relies on the case, it will behave differently per language.

**Recommendation:**
Verify intent with the author. Both TypeScript and Java/.NET test code may be case-sensitive. Recommended: pick one convention for the entire repo and align. Preferred: uppercase `API`/`UI` for consistency with Java/.NET and with common HTTP convention (`API`); update TypeScript configurations in `system-test/typescript` and all `*-typescript-acceptance-stage*.yml` files to use `API`/`UI`. If TypeScript test code is lowercase, an alternate (and less disruptive) fix is to normalize case inside the TypeScript test harness.

---

### DIFF-14: acceptance-stage-cloud — monolith variants have explicit `CHANNEL` env on E2E API but JAVA monolith uses `-DexternalSystemMode=stub` on some tests while .NET/TS don't

**Stage:** acceptance-stage-cloud
**Scope:** monolith — Java vs .NET vs TypeScript

**Files:**
- `.github/workflows/monolith-java-acceptance-stage-cloud.yml:250` — `Run Acceptance Tests - API Channel` → includes `-Dchannel=API`, no `EXTERNAL_SYSTEM_MODE` (passed as env in .NET/TS)
- `.github/workflows/monolith-dotnet-acceptance-stage-cloud.yml:293-297` — env block includes `EXTERNAL_SYSTEM_MODE: stub` and `CHANNEL: API`
- `.github/workflows/monolith-typescript-acceptance-stage-cloud.yml:294-299` — env block includes `EXTERNAL_SYSTEM_MODE: stub` and `CHANNEL: api`

**Details:**
Java passes `-DexternalSystemMode=…` via Gradle args for some tests but omits it in the acceptance-API cloud job (line 250), while .NET/TS explicitly set `EXTERNAL_SYSTEM_MODE: stub` in env. Effectively the Java test harness may be inferring stub-mode from another signal or defaulting.

**Recommendation:**
Audit Java system-test configuration to confirm what `externalSystemMode` defaults to when unspecified. Recommended: add an explicit `-DexternalSystemMode=stub` (or similar) in `monolith-java-acceptance-stage-cloud.yml` for the acceptance-* jobs so the intent is visible in the workflow, matching .NET/TS.

---

### DIFF-15: acceptance-stage — multitier-typescript debug step has `run` block before the main test flow (affects step count vs siblings)

**Stage:** acceptance-stage
**Scope:** multitier — TypeScript only

**Files:**
- `.github/workflows/multitier-typescript-acceptance-stage.yml:183` — `Debug Smoke Discovery` adds an extra step not present in `multitier-java-acceptance-stage.yml` or `multitier-dotnet-acceptance-stage.yml`

**Details:**
Already covered by DIFF-6. Listed here to make the step-count variation in the `run` job visible: Java run job has ~22 steps, .NET run job has ~24 steps, TypeScript run job has ~23 steps because of this debug step (and because TypeScript lacks `Compile System Tests`).

**Recommendation:**
Covered by DIFF-6. No separate action needed.

---

### DIFF-16: acceptance-stage — `concurrency:` group naming is consistent across languages per-architecture (no inconsistency found, documented for completeness)

**Stage:** all stages
**Scope:** monolith and multitier — all languages

**Files:**
- `monolith-java-acceptance-stage.yml:13-14` — `group: monolith-java-acceptance-stage`
- `monolith-dotnet-acceptance-stage.yml:13-14` — `group: monolith-dotnet-acceptance-stage`
- `monolith-typescript-acceptance-stage.yml:13-14` — `group: monolith-typescript-acceptance-stage`
- Same per-language suffix pattern across multitier and across all stages.

**Details:**
All workflows use `{architecture}-{language}-{stage}` as the concurrency group. No inconsistency found.

**Recommendation:**
No action — included only to certify that concurrency is uniform.

---

### DIFF-17: acceptance-stage — `TODO(debug-smoke-0-tests)` comment-style deviation from other TODO comments in the repo

**Stage:** acceptance-stage
**Scope:** multitier-typescript only

**Files:**
- `.github/workflows/multitier-typescript-acceptance-stage.yml:180-182` — parenthesised tag `TODO(debug-smoke-0-tests)`
- Other TODO markers (e.g. `monolith-java-commit-stage.yml:85, 89, 93`) are bare `# TODO: Implement`

**Details:**
The debug step TODO uses a different syntax from the rest of the repo. Minor stylistic inconsistency.

**Recommendation:**
Covered by DIFF-6 (if the step is removed, this goes away). If kept, align the style to `# TODO: ...` or accept the richer `TODO(tag)` format as a convention worth adopting more broadly.

---

### DIFF-18: Cloud acceptance stages — `deploy-app` job depends on different upstream in monolith cloud vs `deploy-frontend`/`deploy-backend` in multitier cloud (expected — structural)

**Stage:** acceptance-stage-cloud
**Scope:** monolith vs multitier

**Files:**
- `.github/workflows/monolith-*-acceptance-stage-cloud.yml` — single `deploy-app` job
- `.github/workflows/multitier-*-acceptance-stage-cloud.yml` — `deploy-frontend` + `deploy-backend` jobs, all test jobs depend on both

**Details:**
Monolith deploys one app; multitier deploys two (frontend + backend). This reflects the architecture, not a bug. Each architecture is internally consistent across all three languages.

**Recommendation:**
No action — architectural, not inconsistency.

---

### DIFF-19: qa-stage, qa-signoff, prod-stage, prod-stage-cloud — all triplets are aligned

**Stage:** qa-stage, qa-signoff, prod-stage, prod-stage-cloud
**Scope:** monolith and multitier — all languages

**Files:**
- All `qa-stage`, `qa-signoff`, `prod-stage`, `prod-stage-cloud`, `qa-stage-cloud` workflow files.

**Details:**
Language diffs across triplets in these stages show only expected language-specific differences: workflow `name`, concurrency `group`, `tag-prefix`, `base-image-urls` pointing at per-language images, `environment` name, `working-directory`, port numbers in the systems JSON, `service-name`, env-var names (`SPRING_PROFILES_ACTIVE` vs `ASPNETCORE_ENVIRONMENT` vs `NODE_ENV`), and the `components` map in `Create Component Tags`.

**Recommendation:**
No action — these stages are cleanly aligned across the triplet.

---

### DIFF-20: pipeline drivers (prerelease-pipeline-*) — all triplets are aligned

**Stage:** pipeline drivers (not a stage, but full-pipeline runners)
**Scope:** monolith and multitier — all languages

**Files:**
- `prerelease-pipeline-monolith-java.yml`, `-dotnet.yml`, `-typescript.yml`
- `prerelease-pipeline-multitier-java.yml`, `-dotnet.yml`, `-typescript.yml`

**Details:**
Differences are limited to `name`, `prefix`, `language`, and `commit-workflows` (which by definition must reference the language-specific commit-stage workflow filenames). No other structural differences.

**Recommendation:**
No action.

---

## Summary

| #      | Stage                           | Issue                                                                                             | Recommendation                                                                                  |
|--------|---------------------------------|---------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------|
| DIFF-1 | commit-stage (monolith)         | `Run Unit Tests` TODO in .NET and TypeScript but implemented in Java                              | Implement in .NET (`dotnet test`) and TypeScript (`npm test`)                                    |
| DIFF-2 | commit-stage (multitier backend)| `Run Unit Tests` TODO in Java and .NET but implemented in TypeScript                              | Implement in Java (`./gradlew test`) and .NET (`dotnet test`)                                    |
| DIFF-3 | commit-stage (all)              | `Run Narrow Integration Tests`, `Run Component Tests`, `Run Contract Tests` TODO in every language | Track as an architecture-wide gap, not a triplet inconsistency                                  |
| DIFF-4 | commit-stage (all)              | `Read Target Component Version` placed early in multitier but late in monolith                    | Move monolith step to top of run job; add matching `component-version` / `image-version-url` outputs |
| DIFF-5 | commit-stage (all)              | Summary uses `image-digest-url` in monolith vs `image-version-url` in multitier                   | Align to `image-version-url` after DIFF-4 is applied                                             |
| DIFF-6 | acceptance-stage                | `Debug Smoke Discovery` diagnostic step exists only in multitier-typescript                       | Remove if the "Total tests: 0" issue is resolved; otherwise track with a real issue             |
| DIFF-7 | acceptance-stage                | `Compile System Tests` (Java) vs `Build Test Project` (.NET) vs missing (TypeScript)              | Rename .NET to `Compile System Tests`; add a compile step in TypeScript                          |
| DIFF-8 | acceptance-stage-legacy         | Same pattern as DIFF-7 in legacy variants                                                         | Apply same fix to four `*-acceptance-stage-legacy.yml` files                                     |
| DIFF-9 | acceptance-stage-cloud          | .NET repeats `Build Test Project` in every cloud test job; Java/TS don't                          | Rename to `Compile System Tests` for naming parity; otherwise document as architectural          |
| DIFF-10| commit-stage                    | .NET Sonar ignore-criteria differ between monolith (7) and multitier (5)                          | Verify author intent; likely deliberate and expected                                             |
| DIFF-11| commit-stage                    | Java Sonar config lives in Gradle; .NET/TS live in workflow                                       | Expected; no action                                                                              |
| DIFF-12| acceptance-stage                | Language-setup step position relative to Playwright install differs                               | Covered by DIFF-7 / DIFF-9                                                                      |
| DIFF-13| acceptance-stage (all variants) | `CHANNEL` env is uppercase in Java/.NET (`API`/`UI`) but lowercase in TypeScript (`api`/`ui`)     | Standardize to uppercase across TypeScript workflows                                              |
| DIFF-14| acceptance-stage-cloud          | Java acceptance-cloud omits `externalSystemMode` while .NET/TS pass `EXTERNAL_SYSTEM_MODE: stub`  | Add explicit `-DexternalSystemMode=stub` in Java cloud workflows                                 |
| DIFF-15| acceptance-stage                | multitier-typescript has an extra debug step                                                      | Covered by DIFF-6                                                                                |
| DIFF-16| all stages                      | Concurrency naming (no inconsistency — documented for completeness)                               | No action                                                                                        |
| DIFF-17| acceptance-stage                | Unique `TODO(debug-smoke-0-tests)` comment style in multitier-typescript                          | Covered by DIFF-6                                                                                |
| DIFF-18| acceptance-stage-cloud          | Monolith `deploy-app` vs multitier `deploy-frontend`+`deploy-backend` (architectural)             | No action — architectural, not inconsistency                                                    |
| DIFF-19| qa-stage, qa-signoff, prod-stage, prod-stage-cloud | All triplets aligned — only expected per-language differences                    | No action                                                                                        |
| DIFF-20| pipeline drivers                | All triplets aligned                                                                              | No action                                                                                        |

**Total: 20 inconsistencies found** (13 actionable, 7 documented-only)

By architecture:
  - Monolith: 7 actionable (DIFF-1, DIFF-4, DIFF-5, DIFF-7, DIFF-8, DIFF-13, DIFF-14)
  - Multitier: 8 actionable (DIFF-2, DIFF-3*, DIFF-4, DIFF-5, DIFF-6, DIFF-7, DIFF-8, DIFF-13)
  - Cross-architecture (shared findings): DIFF-4, DIFF-5, DIFF-7, DIFF-8, DIFF-13 apply to both architectures.

By severity:
  - Missing steps/jobs: 3 (DIFF-6 removed diagnostic in TS-only, DIFF-7 missing compile in TS, DIFF-8 missing compile in TS legacy)
  - TODO placeholders (implemented elsewhere): 2 (DIFF-1, DIFF-2) — these are the highest-signal findings
  - TODO placeholders (missing everywhere — gap, not inconsistency): 1 (DIFF-3)
  - Step-name mismatches: 2 (DIFF-7, DIFF-8 — "Compile System Tests" vs "Build Test Project")
  - Configuration / ordering mismatches: 5 (DIFF-4 ordering, DIFF-5 output name, DIFF-13 CHANNEL case, DIFF-14 externalSystemMode, DIFF-10 Sonar rules)
  - Unique debug scaffolding: 2 (DIFF-6, DIFF-15, DIFF-17 — all the same root cause)
  - Action version mismatches: 0 (all workflows use `actions/checkout@v5`, `docker/login-action@v4`, `actions/cache@v4`, `google-github-actions/auth@v2`, `google-github-actions/setup-gcloud@v2`, `optivem/actions/*@v1`, `SonarSource/sonarcloud-github-action@v5`; none mismatch across the triplet)
  - Expected / architectural (no action): 6 (DIFF-3, DIFF-10, DIFF-11, DIFF-16, DIFF-18, DIFF-19, DIFF-20)
