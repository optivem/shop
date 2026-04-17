# Acceptance Stage Monitoring Process

## Process

1. **Trigger** the failing acceptance stage workflow(s):
   ```bash
   gh workflow run <workflow-name>.yml --repo optivem/shop --ref main
   ```
   Only trigger workflows that are currently failing. Do not re-trigger workflows that are already passing.

2. **Monitor** the run. Sleep 5 minutes between status checks (to avoid rate limiting):
   ```bash
   sleep 300 && gh run list --workflow <workflow-name>.yml --repo optivem/shop --limit 1
   ```
   Repeat until the run status is "completed".

3. **If the run succeeded**, report success and move to the next failing workflow (if any).

4. **If the run failed:**
   - Get the failed job logs:
     ```bash
     gh run view <run-id> --repo optivem/shop --log-failed
     ```
   - Investigate the root cause of the failure.
   - Fix the issue in the shop repo only.
   - Verify the fix compiles:
     ```bash
     dotnet build
     ```
     (run from the relevant test project directory, e.g. `system-test/dotnet`)
   - Commit the fix using the `/commit` skill. Do not ask the user for approval — commit directly.
   - Go back to step 1 (re-trigger the failing workflow).

5. **Repeat** until all failing acceptance stage workflows pass.

## Acceptance Stage Workflows

There are 12 acceptance stage workflows, organized by architecture and language:

| Architecture | Language   | Latest Workflow                              | Legacy Workflow                                     |
|-------------|------------|----------------------------------------------|-----------------------------------------------------|
| Monolith    | Java       | monolith-java-acceptance-stage.yml           | monolith-java-acceptance-stage-legacy.yml           |
| Monolith    | .NET       | monolith-dotnet-acceptance-stage.yml         | monolith-dotnet-acceptance-stage-legacy.yml         |
| Monolith    | TypeScript | monolith-typescript-acceptance-stage.yml     | monolith-typescript-acceptance-stage-legacy.yml     |
| Multitier   | Java       | multitier-java-acceptance-stage.yml          | multitier-java-acceptance-stage-legacy.yml          |
| Multitier   | .NET       | multitier-dotnet-acceptance-stage.yml        | multitier-dotnet-acceptance-stage-legacy.yml        |
| Multitier   | TypeScript | multitier-typescript-acceptance-stage.yml    | multitier-typescript-acceptance-stage-legacy.yml    |

To check which acceptance stage workflows are currently failing:
```bash
gh run list --repo optivem/shop --limit 20 --json name,conclusion | jq '.[] | select(.conclusion=="failure" and (.name | test("acceptance-stage")))'
```

## Stop Conditions

Stop the loop and report to the user if:
- A test fails due to an external issue not under your control (subscription limits, third-party service outage, rate limiting).
- You cannot determine the root cause after thorough investigation.
- You have already attempted two fix-and-retrigger cycles for the same failure and it keeps failing in CI.

## Guidelines

- Always sleep at least 5 minutes between CI status checks to avoid GitHub API rate limiting.
- When investigating failures, check both the system test code (`system-test/`) and the application code (`system/`).
- Java is the reference implementation. All backends (Java/.NET/TypeScript) must return identical API responses.
- Never use `git pull --rebase`. Always plain `git pull`.
- Never commit, push, or sync repos with ad-hoc commands. Always use the `/commit` skill.
- Only make changes to the shop repo. Do not modify other repos.
