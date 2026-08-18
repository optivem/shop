# 2026-08-18 13:26 UTC — `compile-all.sh` silently skips `backend-clean-java`

## TL;DR

**Why:** `compile-all.sh` fans out over the `gh-optivem-*.yaml` configs in the repo root, and no config references `system/multitier/backend-clean-java`. The sweep therefore reports "All variants compiled cleanly" while never compiling that project — and `CLAUDE.md` tells every contributor and agent that the sweep "compiles every system and system-test project across all three languages", which is no longer true.

**End result:** `compile-all.sh` covers `backend-clean-java`, and it fails loudly if any future project directory under `system/` or `system-test/` is covered by neither a config nor an explicit declaration — so the next standalone project cannot be silently skipped. `CLAUDE.md` says what the script actually does.

## Outcomes

What we get out of this — the goals and deliverables:

- A green `./compile-all.sh` means every project in the repo compiled, `backend-clean-java` included. Today a green run is compatible with that project being entirely broken.
- The gap closes **by construction**, not by one more path being written down: a coverage check enumerates the project directories and fails on anything nothing claims. This is the same failure mode as the `GlobalExceptionHandler` regex — a fact recorded in one place drifting from a fact living in another, invisible to every tool — and it gets the same kind of answer.
- `CLAUDE.md`'s "Pre-Commit Verification" section describes real coverage, so an agent following it doesn't skip a compile it believes it ran.
- `backend-clean-java` is the reference implementation for the theme-1/theme-2 talk work — it is the project under most active change and had the *least* local pre-commit coverage. That inverts.

## Background — what the gap is and why it exists

`compile-all.sh` is deliberately config-driven; its own docstring states the contract:

> Adding a new variant: drop a new `gh-optivem-<arch>-<lang>.yaml` in the repo root — no changes to this script.

It globs `gh-optivem-!(*-legacy).yaml` and runs `gh optivem compile -c <cfg>` per config. The six non-legacy configs between them reference these paths:

```
external-systems/simulators        system/multitier/backend-dotnet
external-systems/stubs             system/multitier/backend-java
system/db/migrations               system/multitier/backend-typescript
system/monolith/dotnet             system/multitier/frontend-react
system/monolith/java               system-test/dotnet
system/monolith/typescript         system-test/java
                                   system-test/typescript
```

Against the actual tree, exactly one real project directory is missing: **`system/multitier/backend-clean-java`**. (`system/multitier/{java,dotnet,typescript}` each contain only a `VERSION` file — they are version markers, not projects, and correctly absent from the list.)

The reason no config covers it is structural, not an oversight: a `gh-optivem-*.yaml` declares a whole SUT — backend *and* frontend *and* `system.config` pointing at a `docker/<lang>/<arch>/systems.yaml` *and* a `system-test` project with its full path map. `backend-clean-java` has none of that. There is no `docker/java/multitier-clean/`, and `system-test/java` drives the legacy backend. The project is a standalone Gradle build exercised on its own by `.github/workflows/multitier-backend-clean-java-commit-stage.yml`, which runs `./gradlew test`, `componentTest`, `integrationTest`, `externalSimulatorImage`, `contractTest`, and `checkstyleAll` directly against `system/multitier/backend-clean-java`.

**So CI does cover the project.** The gap is purely in the *local* pre-commit sweep — which is exactly where it hurts, because `CLAUDE.md` names `./compile-all.sh` as the default gate to run *before* committing, and describes it as compiling "every system and system-test project across all three languages". A contributor or agent runs it, sees six PASSED rows and "All variants compiled cleanly", and reasonably concludes `backend-clean-java` compiles. It was never opened.

This was found while executing `plans/20260818-1216-clean-java-type-mismatch-422.md`: Step 5 ran `./compile-all.sh` expecting it to cover the file just edited. It did not. The change was independently verified by a direct `./gradlew build`, so nothing broken shipped — but the verification step the plan relied on was hollow.

**Alternative considered and rejected:** adding a `gh-optivem-multitier-clean-java.yaml` so the existing glob picks it up with no script change. It is the smallest diff and honours the script's stated contract, but a config file is a declaration that a *full SUT* exists — `system.config`, `system-test.path`, the channel and driver path map. For `backend-clean-java` every one of those fields would have to be either invented or copied from the legacy sibling, i.e. wrong. `gh optivem compile` might well ignore the wrong fields, but `gh optivem system start` and `gh optivem system-test run` read the same file, so the repo would gain a config that is true for one command and a lie for the rest. Rejected: it trades a missing-coverage bug for a wrong-configuration bug.

## ▶ Next executable step (resume here)

Extend `compile-all.sh` with a second, explicitly-declared pass for standalone projects, plus a coverage assertion — in this order:

1. Add a `STANDALONE_PROJECTS` array near the top of the script declaring `system/multitier/backend-clean-java|java`, with a comment saying why it is not a config (no SUT: no `docker/**/systems.yaml`, no system-test project) and pointing at the workflow that owns it in CI.
2. After the config loop, iterate that array and compile each entry with the language's compile command (`./gradlew compileJava compileTestJava` for java — match what `gh optivem compile` invokes so the two passes mean the same thing). Feed results into the same `RESULTS` array so standalone rows appear in the SUMMARY table and count toward the exit code.
3. Add a coverage check that runs **before** compiling: enumerate directories under `system/monolith/*`, `system/multitier/*`, `system-test/*`, and `external-systems/*` that look like projects (contain a `build.gradle`, `package.json`, or `*.csproj`/`*.sln` — this is what excludes the `VERSION`-only marker dirs without hardcoding their names), and fail with a specific message naming any that is referenced by neither a `gh-optivem-*.yaml` nor `STANDALONE_PROJECTS`.
4. Update the script docstring: the "drop a new yaml, no changes to this script" contract still holds for full SUTs, and a standalone project is added to `STANDALONE_PROJECTS` instead. The coverage check enforces that one of the two happened.

Then update the **Pre-Commit Verification** section of `CLAUDE.md` so the sentence matches: the sweep covers every config variant plus the declared standalone projects, and it fails if a project is covered by neither.

Verify by running `./compile-all.sh` (expect a `backend-clean-java` row in the summary), then by temporarily renaming a config or adding a scratch project directory to confirm the coverage check fails loudly rather than passing green.

## Steps

- [ ] Step 1: Add the `STANDALONE_PROJECTS` declaration and the standalone compile pass to `compile-all.sh`, wiring results into the existing `RESULTS`/SUMMARY/exit-code machinery.
- [ ] Step 2: Add the pre-flight coverage check that fails when a project directory is claimed by neither a config nor `STANDALONE_PROJECTS`, naming the offending path and both ways to fix it.
- [ ] Step 3: Update the `compile-all.sh` docstring to describe both registration paths and the check that enforces them.
- [ ] Step 4: Update the **Pre-Commit Verification** section of `CLAUDE.md` to state what the sweep actually covers.
- [ ] Step 5: Run `./compile-all.sh` — all six config variants plus a `system/multitier/backend-clean-java` row, all PASSED.
- [ ] Step 6: Prove the check bites — create a scratch project directory (or temporarily move a config aside), confirm the run fails with the specific message, then undo.
- [ ] Step 7: Commit (ask first, per the repo's commit gate).

## Open questions

1. **Should `test-all.sh` get the same treatment?** It is arch × language driven (`monolith|multitier` × `dotnet,java,typescript`) and equally blind to `backend-clean-java`. **Recommendation: no, not in this plan.** `test-all.sh` runs *system* tests, which need a booted docker stack, and `backend-clean-java` has no `docker/**/systems.yaml` to boot — covering it there means standing up a whole SUT, which is a much larger piece of work with its own design questions. This plan should fix the compile sweep and leave a note; if the clean variant ever gets a system stack, that is the moment to revisit.

2. **Should the coverage check also assert the reverse — a config referencing a path that no longer exists?** **Recommendation: yes, it is nearly free.** The same enumeration pass can compare in both directions, and a config pointing at a deleted or renamed project is the same class of silent drift. Cheap to add while the code is open; say no if you would rather keep the check to one job.

3. **Is `./gradlew compileJava compileTestJava` the right depth for the standalone pass, or should it be `./gradlew build`?** **Recommendation: `compileJava compileTestJava`.** It matches what `gh optivem compile` does for the config variants, so every row in the summary means the same thing, and it keeps the sweep fast — `CLAUDE.md` already directs contributors to run the project's own tests separately. Using `build` here would make one row silently much slower and much stronger than the others.
