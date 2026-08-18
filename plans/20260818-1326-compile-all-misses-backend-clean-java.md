# 2026-08-18 13:26 UTC — `compile-all.sh` silently skips `backend-clean-java`

## TL;DR

**Why:** `compile-all.sh` fans out over the `gh-optivem-*.yaml` configs in the repo root, and no config references `system/multitier/backend-clean-java`. The sweep therefore reports "All variants compiled cleanly" while never compiling that project — and `CLAUDE.md` tells every contributor and agent that the sweep "compiles every system and system-test project across all three languages", which is no longer true.

**End result:** `compile-all.sh` covers `backend-clean-java` — via an honest `kind: component` config that its existing glob picks up, with no change to the script's fan-out logic — and it fails loudly if any future project directory under `system/` or `system-test/` is covered by no config at all, so the next standalone project cannot be silently skipped. `CLAUDE.md` says what the script actually does.

> **⛔ Blocked on `gh-optivem`.** This plan depends on `kind: component` existing in the CLI — see `gh-optivem/plans/20260818-1351-kind-component-config-discriminator.md` (its Step 11 is the `shop`-side config addition that unblocks this plan). Do not start here until that has landed; the coverage check below would turn `compile-all.sh` red on `backend-clean-java` until the config exists to claim it.

## Outcomes

What we get out of this — the goals and deliverables:

- A green `./compile-all.sh` means every project in the repo compiled, `backend-clean-java` included. Today a green run is compatible with that project being entirely broken.
- The gap closes **by construction**, not by one more path being written down: a coverage check enumerates the project directories and fails on anything no config claims. This is the same failure mode as the `GlobalExceptionHandler` regex — a fact recorded in one place drifting from a fact living in another, invisible to every tool — and it gets the same kind of answer.
- There stays **one** registration mechanism, not two. A project is covered because a `gh-optivem-*.yaml` declares it; the script's own contract ("drop a new yaml — no changes to this script") remains literally true, rather than being contradicted by a second in-script list of exceptions.
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

**Alternative originally rejected — and now the chosen route.** The first draft of this plan considered adding a `gh-optivem-multitier-clean-java.yaml` so the existing glob picks it up with no script change, and rejected it on the grounds that a config file is a declaration that a *full SUT* exists — `system.config`, `system-test.path`, the channel and driver path map — every field of which would have to be invented or copied from the legacy sibling, i.e. wrong. The repo would gain a config true for `gh optivem compile` and a lie for `system start` and `system-test run`.

That reasoning was correct **about the CLI as it stood**, and wrong about where the defect was. The real problem is in `gh-optivem`: `system.architecture` doubles as the "is this a whole SUT?" discriminator, so a component-only project cannot be declared at all. Fixing that — a top-level `kind: system | component` — makes a seven-line config for `backend-clean-java` *honest*: it declares exactly what exists and claims nothing it does not have. See `gh-optivem/plans/20260818-1351-kind-component-config-discriminator.md`.

So the config route is adopted, and the counter-proposal that replaced it here — a `STANDALONE_PROJECTS` array declared inside `compile-all.sh` — is dropped. It would have been a second registration mechanism competing with the config glob, which is the very drift this plan exists to stop. What survives from it is the coverage check: still worth having, and now with one thing to check against instead of two.

## ▶ Next executable step (resume here)

**Blocked — nothing here is executable yet.** The first move belongs to the other repo: land `kind: component` via `gh-optivem/plans/20260818-1351-kind-component-config-discriminator.md`, whose Step 11 adds `gh-optivem-multitier-clean-java.yaml` to this repo. That single file is what makes `backend-clean-java` claimed, and it is what the coverage check below needs in place before it can pass.

Once that config exists here, Step 1 is the coverage check in `compile-all.sh`: before the config loop, enumerate directories under `system/monolith/*`, `system/multitier/*`, `system-test/*`, and `external-systems/*` that look like projects (contain a `build.gradle`, `package.json`, or `*.csproj`/`*.sln` — this is what excludes the `VERSION`-only marker dirs without hardcoding their names), resolve every path referenced by every `gh-optivem-*.yaml` (including `*-legacy`, which are excluded from compiling but still declare coverage), and fail naming any project directory no config claims, with the fix spelled out: add it to an existing config, or give it its own `kind: component` config.

## Steps

- [ ] Step 1: Add the pre-flight coverage check to `compile-all.sh` — fail when a project directory is claimed by no `gh-optivem-*.yaml`, naming the offending path and how to fix it.
- [ ] Step 2: Extend the same enumeration pass to assert the reverse direction — a config referencing a path that no longer exists is the same class of silent drift, and the comparison is already in hand. (Resolved: yes, do it — see Decisions.)
- [ ] Step 3: Update the `compile-all.sh` docstring — the "drop a new yaml, no changes to this script" contract still holds and is now *enforced* by the coverage check; note that a project with no SUT registers via a `kind: component` config rather than being exempt.
- [ ] Step 4: Update the **Pre-Commit Verification** section of `CLAUDE.md` to state what the sweep actually covers, and add the note that `test-all.sh` deliberately does *not* cover `backend-clean-java` (see Decisions).
- [ ] Step 5: Run `./compile-all.sh` — expect seven config rows including `gh-optivem-multitier-clean-java.yaml`, all PASSED.
- [ ] Step 6: Prove the check bites — create a scratch project directory (or temporarily move a config aside), confirm the run fails with the specific message, then undo.
- [ ] Step 7: Commit (ask first, per the repo's commit gate).

## Decisions taken (resolved before execution)

1. **Should `test-all.sh` get the same treatment?** — **No, not in this plan.** It runs *system* tests, which need a booted docker stack, and `backend-clean-java` has no `docker/**/systems.yaml` to boot; covering it there means standing up a whole SUT, a much larger piece of work with its own design questions. Step 4 records the gap as a note in `CLAUDE.md`. Revisit if the clean variant ever gets a system stack.
2. **Should the coverage check also assert the reverse — a config referencing a path that no longer exists?** — **Yes.** The same enumeration pass compares in both directions for nearly no extra code, and a config pointing at a deleted or renamed project is the same class of silent drift. Now Step 2.
3. **What depth should the standalone compile pass use?** — **Moot.** There is no standalone pass any more; `gh optivem compile` drives the `kind: component` config exactly as it drives the other six, so every row in the summary means the same thing by construction rather than by matching commands by hand.
