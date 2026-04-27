# Plan — Shop Workflow Warnings Cleanup

🤖 **Picked up by agent** — `Valentina_Desk` at `2026-04-27T08:35:44Z`

**Date:** 2026-04-27
**Source audit:** Latest runs of all 69 workflows in the shop repo. 48 had completed runs analyzed; 19 cloud workflows have never run; 2 were in progress at audit time.
**Scope:** Address warnings (not failures). The Docker YAML parse error breaking every commit-stage workflow is tracked separately.

## Out of scope

- Hard failures already breaking commit-stage runs (Docker YAML `(7:7)` error). Fix those independently — they are blockers, not warnings.
- Cloud-stage workflows that have never run: their warning surface is unknown. Re-evaluate once they have run at least once with the GCP secrets configured.

---

## W1 — NuGet `NU1603`: WireMock.Net version mismatch

**Symptom**
```
warning NU1603: Driver.Adapter depends on WireMock.Net (>= 1.6.28) but WireMock.Net 1.6.28 was not found.
                WireMock.Net 1.7.0 was resolved instead.
```

**Affected workflows (5):** `monolith-dotnet-acceptance-stage`, `monolith-dotnet-acceptance-stage-legacy`, `multitier-dotnet-acceptance-stage`, `multitier-dotnet-acceptance-stage-legacy`, `prerelease-pipeline-monolith-dotnet`.

**Root cause:** `Driver.Adapter` references WireMock.Net `1.6.28` but the package was unpublished/withdrawn from NuGet; only 1.7.0+ is available, so NuGet auto-bumps and warns.

**Proposed fix:** In `system-test/dotnet/Driver.Adapter/*.csproj`, bump `WireMock.Net` reference from `1.6.28` to `1.7.0` (or floating `1.7.*`). Run `dotnet restore` locally to confirm the warning is gone. No code changes expected — 1.6→1.7 is a minor bump per WireMock.Net's semver.

**Risk:** Low. WireMock.Net is dev-only (test infrastructure). API differences between 1.6.28 and 1.7.0 are documented; if any DSL we use changed signature, the test compilation will catch it immediately. No production binary impact.

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

---

## W4 — Node.js 20 actions deprecation (runner-level)

**Symptom**
```
##[warning]Node.js 20 actions are deprecated. The following actions are running on Node.js 20 and may not work as expected:
   nick-fields/retry@v3, Wandalen/wretry.action@v3.8.0_js_action.
   Actions will be forced to run with Node.js 24 by default starting June 2nd, 2026.
   Node.js 20 will be removed from the runner on September 16th, 2026.
```

**Affected workflows (9):** All `*-commit-stage` + `monolith-java-acceptance-stage`, `multitier-java-acceptance-stage`.

**Deadlines:** **June 2, 2026** — forced Node 24. **Sept 16, 2026** — Node 20 removed. We have ~5 weeks until the forced switch.

**Proposed fix:**
- `Wandalen/wretry.action` — check if a Node 20→24 release exists; if not, file an upstream issue and pin to whatever the latest version is. As of audit, `v3.8.0_js_action` is the pinned tag.
- `nick-fields/retry` — same; check for a version published with Node 24 runtime in `action.yml`. v3 has been stable for years; upstream may not have rebuilt for Node 24.

If neither has a Node 24 release: replace with bash-loop retries (`for i in 1 2 3; do … && break; done`) or run-script step. Both actions are small wrappers and we don't depend on exotic features.

**Risk:** Medium. Hard deadline is external (GitHub). If upstream doesn't ship Node 24 versions before June 2, our pipelines will start failing automatically. Plan to migrate off these actions before the deadline regardless of warning urgency.

**Recommendation:** Schedule a follow-up agent for May 15, 2026 to re-check upstream status and either bump pins or open replacement PRs.

---

## W5 — Playwright host-validation warning (missing system libraries)

**Symptom**
```
Playwright Host validation warning:
╔══════════════════════════════════════════════════════╗
║ Host system is missing dependencies to run browsers. ║
║ Missing libraries:                                   ║
║     libwoff2dec.so.1.0.2                             ║
║     libvpx.so.7                                      ║
║     ...                                              ║
╚══════════════════════════════════════════════════════╝
```

**Affected workflows (7):** `monolith-java-acceptance-stage(-legacy)`, `multitier-java-acceptance-stage(-legacy)`, `prerelease-pipeline-monolith-java`, `prerelease-pipeline-multitier-java`, `prerelease-pipeline-monolith-dotnet`.

**Root cause:** The runner has Playwright browser binaries installed but not the OS-level libraries they link against. Tests still pass because Playwright falls back to whatever is available, but the warning indicates fragility.

**Proposed fix:** In each affected workflow (or the shared composite action that drives system tests), run `npx playwright install-deps` (or `playwright install --with-deps`) before the test step. For Java/Maven builds, the command is `mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install-deps"`.

If this is a shared action in `actions/`, fix it once there and let all callers inherit. Otherwise edit each workflow.

**Risk:** Low. `install-deps` uses `apt-get install` for known-safe packages and only adds time (~30s) to the run. No behavioral change to the tests themselves; they will continue to pass with or without the libs in place because Playwright is falling back. Adds reliability for less-common code paths (video capture, certain font rendering).

---

## W6 — Gradle 9.0 deprecation warnings

**Symptom**
```
Deprecated Gradle features were used in this build, making it incompatible with Gradle 9.0.
You can use '--warning-mode all' to show the individual deprecation warnings…
```

**Affected workflows (8):** All Java commit/acceptance/prerelease workflows.

**Root cause:** Gradle build scripts use APIs scheduled for removal in Gradle 9.0. The current build runs with `--warning-mode summary` so the specific deprecations are hidden.

**Proposed fix:**
1. Run a one-off `./gradlew build --warning-mode all` locally on each Java module to enumerate the actual deprecations.
2. Address each (most are 1–2 line fixes — typical examples: `task.dependsOn(...)` ordering, deprecated `ConfigurationContainer` APIs, `org.gradle.api.publish` v2 plugins).
3. Bump Gradle version once warnings are clean.

**Risk:** Medium. Gradle deprecations are usually mechanical fixes, but the build files in this repo are referenced by course materials. Some patterns may be deliberately old to teach a specific approach. Coordinate with the Java track owner before bulk-rewriting build.gradle files.

---

## W7 — npm `warn deprecated` (transitive)

**Symptom**
```
npm warn deprecated inflight@1.0.6: This module is not supported, and leaks memory.
npm warn deprecated glob@7.2.3: Old versions of glob are not supported, and contain widely publicized security vulnerabilities…
npm warn deprecated glob@10.5.0: Old versions of glob are not supported…
```

**Affected workflows (4):** `monolith-typescript-commit-stage`, `multitier-backend-typescript-commit-stage`, `prerelease-pipeline-monolith-typescript`, `prerelease-pipeline-multitier-typescript`.

**Root cause:** Transitive dependencies pinned via direct dependencies' lockfiles. We don't import these directly.

**Proposed fix:**
1. Run `npm outdated` and `npm audit fix` on each TypeScript project.
2. For genuinely stuck transitive deps, use npm `overrides` in `package.json` to force a newer version (e.g. `"glob": "^11"`).
3. Re-run lockfile generation and commit.

**Risk:** Medium. `npm overrides` can break sub-deps that genuinely require the older API. Test each project after the override. Easier path: bump the direct dependencies that pull these in, which usually carries the transitive bumps for free.

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

---

## W9 — ESLint `@typescript-eslint/no-unsafe-argument`

**Symptom**
```
##[warning]  19:20  warning  Unsafe argument of type `any` assigned to a parameter of type `App`  @typescript-eslint/no-unsafe-argument
✖ 1 problem (0 errors, 1 warning)
```

**Affected workflows (1):** `multitier-backend-typescript-commit-stage`.

**Proposed fix:** At line 19 col 20 of the offending file, add an explicit type annotation or `as App` cast where the `any` flows in. Single-file, single-line fix.

**Risk:** Trivial. Local code change; no behavioral impact.

---

## W10 — `awk: warning: escape sequence \. treated as plain .`

**Symptom**
```
awk: warning: escape sequence `\.' treated as plain `.'
```

**Affected workflows (1):** `meta-release-stage` (~6 occurrences).

**Root cause:** Inline awk script uses `\.` outside a character class. POSIX awk treats backslash before non-special characters as the literal char and warns.

**Proposed fix:** In the `meta-release-stage.yml` (or a script it sources), find the awk invocation and replace `\.` with `[.]` or just `.` (depending on whether literal-period was intended).

**Risk:** Trivial. Cosmetic — script already produces correct output.

---

## Priority order (recommended)

| Priority | Warning | Why |
|---|---|---|
| **P0** | W4 (Node 20 deprecation) | Hard external deadline — June 2, 2026 |
| **P0** | W8 (npm vulnerabilities) | Security; students copy this template |
| **P1** | W5 (Playwright deps) | Cheap reliability win |
| **P1** | W1 (NuGet NU1603) | Cheap one-line bump |
| **P2** | W6 (Gradle 9.0) | Future-proofing; needs investigation pass |
| **P2** | W3 (CS8603/CS8604) | Test-DSL hardening |
| **P3** | W2 (C# Sonar) | Pedagogical review needed first |
| **P3** | W7 (npm deprecated) | Cleanup after W8 |
| **P3** | W9 (ESLint) | One-line fix; do at any time |
| **P3** | W10 (awk) | Cosmetic |

---

## Cross-cutting risk

- **Pedagogical impact.** This repo is course material. Some "warnings" are intentional teachable smells. Run W2 / W3 / W6 fixes past the course author before bulk-applying.
- **Shared-action churn.** Several warnings (W4, W5) likely originate from composite actions in the [actions/](../actions/) repo, not in shop's own workflows. Fix at the source there to propagate everywhere.
- **No CI gate on warnings.** Adding `--warn-as-error` everywhere would be the strongest preventive measure, but it would block course work mid-iteration. Recommend opt-in per-workflow only after each warning class is cleared.
