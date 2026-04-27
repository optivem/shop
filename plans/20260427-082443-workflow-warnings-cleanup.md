# Plan — Shop Workflow Warnings Cleanup

**Date:** 2026-04-27
**Source audit:** Latest runs of all 69 workflows in the shop repo. 48 had completed runs analyzed; 19 cloud workflows have never run; 2 were in progress at audit time.
**Scope:** Address warnings (not failures). The Docker YAML parse error breaking every commit-stage workflow is tracked separately.

## Out of scope

- Hard failures already breaking commit-stage runs (Docker YAML `(7:7)` error). Fix those independently — they are blockers, not warnings.
- Cloud-stage workflows that have never run: their warning surface is unknown. Re-evaluate once they have run at least once with the GCP secrets configured.

---

## W2 — C# Sonar S-rule warnings (S1075, S1118, S1186, S1939, S2068, S2699, S2955, S3267, S4136, S4502)

**Affected workflows (7):** `monolith-dotnet-commit-stage`, `multitier-backend-dotnet-commit-stage`, all 4 `*-dotnet-acceptance-stage*`, `prerelease-pipeline-monolith-dotnet`.

**Sample findings:**
- `S2068` — `"password"` literal in `appsettings.json`
- `S2699` — test method with no assertion (`MonolithApplicationTests.cs:31`)
- `S1186` — empty methods in `Pages/Privacy.cshtml.cs`
- `S1075` — hardcoded URLs
- `S4502` — CSRF disable
- `S4136` — overload adjacency
- `S1939` — redundant interface declaration
- `S2955` — comparison with `default(T)`
- `S3267` — loops that could be LINQ
- `S1118` — utility class missing `static`

**Proposed fix:** Triage each rule. Most are low-effort one-line fixes:
- `S2068`: rename the property or add `// NOSONAR — placeholder for sample app` if intentional.
- `S2699`: add `Assert.Pass()` or a real assertion to the smoke test.
- `S1186`: add `// nothing to do — required by framework` comment inside empty methods.
- `S1075`, `S4502`, `S1118`, `S1939`, `S2955`, `S3267`, `S4136`: address per Sonar guidance.

This is teaching material — some "smells" may be deliberate. Annotate with comments where intentional; fix where not.

**Risk:** Low–medium. Behavioral risk is near zero (these are style/guideline warnings, not bugs). Risk is **pedagogical**: this codebase is a sample for students. Fixing a smell may erase a teachable example. Recommend reviewing each rule with course author before applying — do not auto-fix in bulk.

---

## W3 — C# nullable-reference warnings (CS8603, CS8604)

**Affected workflows (5):** All 4 `*-dotnet-acceptance-stage*` + `prerelease-pipeline-monolith-dotnet`.

**Sample:**
```
.../Dsl.Core/Scenario/Assume/AssumeStage.cs(22,39): warning CS8604: Possible null reference argument for parameter 'channel' …
.../Dsl.Core/Scenario/When/Steps/Base/BaseWhenStep.cs(38,34): warning CS8603: Possible null reference return.
```

**Root cause:** Nullable reference types are enabled but the DSL helpers use `T?`-able fields without null-checks.

**Proposed fix:** In `system-test/dotnet/Dsl.Core/`, either:
1. Add `ArgumentNullException.ThrowIfNull(channel)` guards (preferred — surfaces real null bugs at runtime).
2. Mark parameters as `T?` and add explicit `??` fallbacks.
3. Use `!` null-forgiving where the value is guaranteed non-null by construction.

**Risk:** Low. Code is in test DSL only. Adding guards strengthens test reliability; loosening type with `?` weakens it. Prefer option 1.

**Investigation note (2026-04-27):** The DSL already treats `Channel` as nullable across `ScenarioDsl`, `BaseClause`, `WhenStage`, `GivenStage`, `BaseGivenStep` — `BaseWhen.Channel` is the only outlier re-typing it as non-null. `AssumeStage`'s constructor defaults `channel = null`. Plan-recommended option 1 (throw guards) conflicts with that prevailing design. Option 2 (extend the nullable pattern: change `BaseWhen.Channel` to `Channel?` and `UseCaseDsl.MyShop` to accept `Channel?`) better matches existing semantics. Decide before re-attempting.

---

## W6 — Gradle 9.0 deprecation warnings

**Symptom**
```
Deprecated Gradle features were used in this build, making it incompatible with Gradle 9.0.
You can use '--warning-mode all' to show the individual deprecation warnings…
```

**Affected workflows (8):** All Java commit/acceptance/prerelease workflows.

**Root cause:** Gradle build scripts use APIs scheduled for removal in Gradle 9.0. The current build runs with `--warning-mode summary` so the specific deprecations are hidden. Project is on Gradle 8.14.3 as of 2026-04-27.

**Proposed fix:**
1. Run a one-off `./gradlew build --warning-mode all` locally on each of the 3 Java modules (`system/monolith/java`, `system/multitier/backend-java`, `system-test/java`) to enumerate the actual deprecations.
2. Address each (most are 1–2 line fixes — typical examples: `task.dependsOn(...)` ordering, deprecated `ConfigurationContainer` APIs, `org.gradle.api.publish` v2 plugins).
3. Bump Gradle version once warnings are clean.

**Risk:** Medium. Gradle deprecations are usually mechanical fixes, but the build files in this repo are referenced by course materials. Some patterns may be deliberately old to teach a specific approach. Coordinate with the Java track owner before bulk-rewriting build.gradle files.

---

## W8 — npm audit reports vulnerabilities

**Findings (from latest run):**

| Workflow | Vulns reported |
|---|---|
| `monolith-typescript-commit-stage` | 2 (1 moderate, 1 high) |
| `multitier-backend-typescript-commit-stage` | 15 (9 moderate, 6 high) |
| `prerelease-pipeline-monolith-typescript` | 2 (1 moderate, 1 high) |
| `prerelease-pipeline-multitier-typescript` | 15 (9 moderate, 6 high) + 8 (3 moderate, 5 high) (backend Docker prod stage) |

**Proposed fix:**
1. Run `npm audit --json` locally on each project to see exact CVEs.
2. For each: bump direct dep, override transitive, or accept-with-comment if the path is unreachable.
3. Add an `npm audit --audit-level=high` step that **fails** the build on high+ vulnerabilities once cleaned up. Right now nothing enforces it.

**Risk:** Medium–high (security). Some vulns may be in dev-only dependencies and not exploitable in prod. Triage before fixing. This is the warning category that most directly affects students who copy this repo as a template — they will inherit the vulns.

**Investigation note (2026-04-27):** Deferred from inline W-sweep — needs dedicated session. 4 npm projects in scope (monolith/typescript, multitier/backend-typescript, multitier/frontend-react, system-test/typescript). Lockfile diffs will be huge; cleanup deserves its own focus rather than being mixed into mechanical warning fixes.

---

## W7 — npm `warn deprecated` (transitive)

**Do AFTER W8** — W8's direct-dep bumps will likely clear most of these transitives for free; running W7 first means doing the work twice.

**Symptom**
```
npm warn deprecated inflight@1.0.6: This module is not supported, and leaks memory.
npm warn deprecated glob@7.2.3: Old versions of glob are not supported, and contain widely publicized security vulnerabilities…
npm warn deprecated glob@10.5.0: Old versions of glob are not supported…
```

**Affected workflows (4):** `monolith-typescript-commit-stage`, `multitier-backend-typescript-commit-stage`, `prerelease-pipeline-monolith-typescript`, `prerelease-pipeline-multitier-typescript`.

**Root cause:** Transitive dependencies pinned via direct dependencies' lockfiles. We don't import these directly.

**Proposed fix:**
1. After W8 completes, re-run `npm outdated` on each TypeScript project to see what deprecations remain.
2. For genuinely stuck transitive deps, use npm `overrides` in `package.json` to force a newer version (e.g. `"glob": "^11"`).
3. Re-run lockfile generation and commit.

**Risk:** Medium. `npm overrides` can break sub-deps that genuinely require the older API. Test each project after the override. Easier path: bump the direct dependencies that pull these in, which usually carries the transitive bumps for free.

---

## Priority order (recommended)

| Priority | Warning | Why |
|---|---|---|
| **P0** | W8 (npm vulnerabilities) | Security; students copy this template |
| **P2** | W6 (Gradle 9.0) | Future-proofing; needs investigation pass |
| **P2** | W3 (CS8603/CS8604) | Test-DSL hardening |
| **P3** | W2 (C# Sonar) | Pedagogical review needed first |
| **P3** | W7 (npm deprecated) | Cleanup after W8 |

---

## Cross-cutting risk

- **Pedagogical impact.** This repo is course material. Some "warnings" are intentional teachable smells. Run W2 / W3 / W6 fixes past the course author before bulk-applying.
- **Shared-action churn.** Several warnings (W4, W5) likely originate from composite actions in the [actions/](../actions/) repo, not in shop's own workflows. Fix at the source there to propagate everywhere.
- **No CI gate on warnings.** Adding `--warn-as-error` everywhere would be the strongest preventive measure, but it would block course work mid-iteration. Recommend opt-in per-workflow only after each warning class is cleared.
