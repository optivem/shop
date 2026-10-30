# 2026-10-30 19:55:32 UTC — Ensure .gitattributes is correct everywhere

## TL;DR

**Why:** Line endings depend on each machine's Git settings wherever `.gitattributes` is missing or incomplete. In one repo with no root `.gitattributes`, a Windows checkout opened in the Linux dev container showed 641 files and 38,119 lines as changed with no real edits; a stray `git add .` would bury real changes and wreck `git blame`, and CRLF shell scripts can fail with `bad interpreter: /bin/bash^M`. Shop has a root file, but it is incomplete and its rules are not uniformly applied.
**End result:** The shop repo has one consistent `.gitattributes` policy, every tracked file matches it in both the index and the working tree, and CI fails if anyone drifts. Other repos are out of scope.

## Outcomes

What we get out of this — the goals and deliverables:

- A checkout on Windows, WSL or Linux shows a clean `git status`, whatever that machine's `core.autocrlf` is.
- Shell scripts (`run-sonar.sh`, `gradlew`, etc.) always have LF, so they run in the dev container and on Linux runners.
- Windows batch files always have CRLF; `gradlew.bat` gets the same treatment in every Java project.
- Binary files are declared explicitly, not left to auto-detection.
- `git ls-files --eol` reports no CRLF in the index other than files that are meant to have it.
- A CI check catches a missing `.gitattributes` or a misnormalized file before it reaches `main`.

## Current state (shop, 2026-10-30)

- Root `.gitattributes` contains only `* text=auto eol=lf`: no Windows-script exceptions and no binary declarations.
- Three nested files (`system/monolith/java`, `system/multitier/backend-java`, `system/multitier/backend-clean-java`) each carry `/gradlew text eol=lf`, `*.bat text eol=crlf`, `*.jar binary`.
- `system-test/java` has no nested file, so its `gradlew.bat` resolves to `eol=lf`, unlike the other Java projects. Its `gradle-wrapper.jar` relies on auto-detection.
- `system/monolith/dotnet/wwwroot/favicon.ico` also relies on auto-detection.
- 3 files are stored with CRLF in the index despite the policy:
  - `reports/20260418-1751-compare-tests-both.md`
  - `system-test/dotnet/SystemTests/Legacy/Mod11/ExternalSystemContractTests/Tax/BaseTaxContractTest.cs`
  - `system-test/java/src/test/java/com/mycompany/myshop/systemtest/legacy/mod11/contract/tax/BaseTaxContractTest.java`
- 12 files are LF in the index but CRLF on disk when policy says LF, and `git status` does not report them. This includes 6 `*-qa-stage-cloud.yml` workflows and 3 `tests.legacy.yaml` files.
- `system/monolith/java/gradlew.bat` and `system/multitier/backend-java/gradlew.bat` are LF on disk though their attribute says CRLF, and all three nested-rule `gradlew.bat` files are LF in the index, which is the expected storage form.

`git status` is therefore not a sufficient check: with `eol=lf`, CRLF on disk counts as unmodified. Verification has to use `git ls-files --eol`.

## ▶ Next executable step (resume here)

Do Step 1: in shop, replace the root `.gitattributes` with the agreed policy (see Step 1 for the proposed content), delete the three nested files, and confirm with `git check-attr text eol -- <gradlew, gradlew.bat, *.jar, *.ico, *.sh>` across all Java projects. Stop before renormalizing. This unblocks Step 2.

## Steps

- [ ] Step 1: Consolidate the policy in shop's root `.gitattributes`, proposed as:
  ```gitattributes
  * text=auto eol=lf
  *.bat text eol=crlf
  *.cmd text eol=crlf
  *.jar binary
  *.ico binary
  *.png binary
  ```
  `.ps1` stays LF, because `pwsh` runs in the Linux dev container and a shebang line breaks under CRLF. Delete the three nested files so root is the single source of truth, and add any other binary types found in the audit.
- [ ] Step 2: Renormalize shop with `git add --renormalize .`, then commit it as a single line-endings-only commit.
- [ ] Step 3: Refresh the working tree so on-disk files match. `git checkout -- .` is not enough for the 12 hidden CRLF files, because Git does not consider them modified. Start from a clean tree and use a command that rewrites every file, such as `git rm --cached -r . && git reset --hard`.
- [ ] Step 4: Add a CI check, either a new workflow or a step in an existing one, that fails when the root `.gitattributes` is missing or `git ls-files --eol` shows an index entry that disagrees with its attributes.
- [ ] Step 5: Document in CONTRIBUTING.md what Windows users should do: clone inside WSL or the dev container, and how to recover a stale checkout.
- [ ] Step 6: Verify on a fresh clone in both Windows and the dev container: clean `git status`, no unexpected `w/crlf`, and `run-sonar.sh` runs. Then run the pre-commit gates from CLAUDE.md, namely `./compile-all.sh` plus the `--sample` system tests for the languages touched.
