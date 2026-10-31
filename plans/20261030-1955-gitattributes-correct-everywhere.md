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

## ▶ Next executable step (resume here)

Only the Windows half of Step 6 remains, and it needs a person on a Windows machine. Everything else is done, and the dev container checks passed.

## Steps

- [ ] Step 6: On Windows, make a fresh clone inside WSL and confirm a clean `git status`, no output from `git ls-files --eol | grep -E 'w/crlf.*eol=lf|w/lf.*eol=crlf'`, and that `run-sonar.sh` starts. Then delete this plan. — ⏳ Deferred: needs a Windows machine, which the dev container can't provide.
