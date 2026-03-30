# SonarCloud Setup (Code Analysis)

This guide covers setting up SonarCloud static code analysis for public GitHub repositories. After completing this, the Commit Stage will include automated code analysis with coverage reporting.

All steps use the command line except where noted as (UI).

> **Note:** SonarCloud project creation is now part of [Apply Template](04-apply-template.md). This page is a reference for account setup, troubleshooting, and bulk operations.

## Prerequisites

- A public GitHub repository
- `gh` CLI installed and authenticated
- `curl` available

## 1. Create a SonarCloud Account and Token (UI — one time only)

These are the only steps that require the browser:

1. Go to [sonarcloud.io](https://sonarcloud.io), click **Log in**, and sign in with your **GitHub** account.
2. Go to **My Account** → **Security** → **Generate Tokens**, enter a name (e.g. `ci`), click **Generate**, and copy the token.

Set the token as an environment variable for the remaining steps:

```bash
export SONAR_TOKEN="<your-token>"
```

## 2. Import Your GitHub Organization (CLI)

```bash
SONAR_ORG="<your-github-owner>"  # your GitHub username or org name (e.g. optivem)

curl -s -u "${SONAR_TOKEN}:" \
  -X POST "https://sonarcloud.io/api/organizations/create" \
  -d "key=${SONAR_ORG}&name=${SONAR_ORG}"
```

If the organization already exists, you will get an error saying so — that is fine, move on.

## 3. Create the SonarCloud Project (CLI)

The project key must match the `sonar.projectKey` in your build config. After applying the starter template and running the sed replacements, check your build config to find the actual key:
- **Java:** `system/monolith/<lang>/build.gradle` → look for `sonar.projectKey`
- **.NET:** `.github/workflows/*-commit-stage.yml` → look for `/k:"..."` in the `dotnet sonarscanner begin` command
- **TypeScript:** `.github/workflows/*-commit-stage.yml` → look for `-Dsonar.projectKey=...`

The key typically includes a component suffix (e.g. `myowner_myrepo-monolith-java`), not just `myowner_myrepo`.

```bash
SONAR_PROJECT="<project-key-from-build-config>"  # e.g. myowner_myrepo-monolith-java

curl -s -u "${SONAR_TOKEN}:" \
  -X POST "https://sonarcloud.io/api/projects/create" \
  -d "organization=${SONAR_ORG}&project=${SONAR_PROJECT}&name=${SONAR_PROJECT}"
```

Verify the project was created:

```bash
curl -s -u "${SONAR_TOKEN}:" \
  "https://sonarcloud.io/api/projects/search?organization=${SONAR_ORG}" | jq '.components[].key'
```

**Important:** SonarCloud defaults to `master` as the main branch. If your repository uses `main` (which is standard), you **must** rename it — otherwise SonarCloud will show "not analyzed" on the project overview even though analysis runs successfully on the `main` branch:

```bash
curl -s -u "${SONAR_TOKEN}:" \
  -X POST "https://sonarcloud.io/api/project_branches/rename" \
  -d "project=${SONAR_PROJECT}&name=main"
```

Verify the rename took effect:

```bash
curl -s -u "${SONAR_TOKEN}:" \
  "https://sonarcloud.io/api/project_branches/list?project=${SONAR_PROJECT}" | jq '.branches[] | {name, isMain}'
```

## 4. Add GitHub Secret (CLI)

The `SONAR_TOKEN` is the same for all repos in your organization — it is your personal token, not per-project.

**Option A — Per-repository secret:**

```bash
gh secret set SONAR_TOKEN --body "${SONAR_TOKEN}"
```

**Option B — Organization secret (recommended if you have multiple repos):**

If you set `SONAR_TOKEN` as an organization-level secret, it applies to all repos automatically — no need to repeat this step per repo.

```bash
gh secret set SONAR_TOKEN --org "${SONAR_ORG}" --visibility all --body "${SONAR_TOKEN}"
```

To restrict it to specific repos instead of all:

```bash
gh secret set SONAR_TOKEN --org "${SONAR_ORG}" --visibility selected --repos "repo1,repo2" --body "${SONAR_TOKEN}"
```

Verify:

```bash
gh secret list
```

## 5. SonarCloud Onboarding (informational)

When you open your new project on [sonarcloud.io](https://sonarcloud.io), SonarCloud will show an onboarding wizard with instructions for setting up the GitHub secret, build file, and CI workflow. **You can skip this** — the starter template already includes the SonarCloud configuration in the build file (`build.gradle` / `package.json`) and the commit stage workflow. The onboarding will disappear automatically after the first successful analysis.

## 6. Workflow Condition

The Code Analysis step in the commit stage workflow should run on the main branch regardless of how the workflow was triggered (push, manual dispatch, etc.). Use `github.ref` only — do **not** restrict by event type:

```yaml
- name: Run Code Analysis
  if: github.ref == 'refs/heads/main'
```

Do **not** use `github.event_name == 'push' && github.ref == 'refs/heads/main'` — this would skip code analysis on manual workflow runs (`workflow_dispatch`), which is unexpected.

## 7. Verify

Commit and push, then check the workflow:

```bash
git add -A && git commit -m "Add SonarCloud code analysis" && git push
gh run watch
```

Once the workflow completes, verify the analysis appeared in SonarCloud:

```bash
curl -s -u "${SONAR_TOKEN}:" \
  "https://sonarcloud.io/api/measures/component?component=${SONAR_PROJECT}&metricKeys=bugs,vulnerabilities,code_smells,coverage,duplicated_lines_density" | jq '.component.measures'
```

You should see metrics for: bugs, vulnerabilities, code smells, coverage, and duplicated lines.

## Bulk Operations

To rename the default branch from `master` to `main` across all SonarCloud projects in your organization:

```bash
SONAR_ORG="<your-github-org>"

for PROJECT in $(curl -s -u "${SONAR_TOKEN}:" \
  "https://sonarcloud.io/api/projects/search?organization=${SONAR_ORG}" | jq -r '.components[].key'); do
  echo "Renaming default branch for ${PROJECT}..."
  curl -s -u "${SONAR_TOKEN}:" \
    -X POST "https://sonarcloud.io/api/project_branches/rename" \
    -d "project=${PROJECT}&name=main"
done
```

If the rename fails with `a branch with name "main" already exists`, it means a previous analysis already created `main` as a short-lived branch. Delete it first, then rename:

```bash
curl -s -u "${SONAR_TOKEN}:" \
  -X POST "https://sonarcloud.io/api/project_branches/delete" \
  -d "project=${SONAR_PROJECT}&branch=main"

curl -s -u "${SONAR_TOKEN}:" \
  -X POST "https://sonarcloud.io/api/project_branches/rename" \
  -d "project=${SONAR_PROJECT}&name=main"
```

After renaming the default branch, SonarCloud may show the onboarding wizard again. This is normal — it disappears after the next successful analysis on the `main` branch. Push a commit or trigger the workflow manually to resolve it.

To verify the branch configuration for all projects:

```bash
for PROJECT in $(curl -s -u "${SONAR_TOKEN}:" \
  "https://sonarcloud.io/api/projects/search?organization=${SONAR_ORG}" | jq -r '.components[].key'); do
  echo "=== ${PROJECT} ==="
  curl -s -u "${SONAR_TOKEN}:" \
    "https://sonarcloud.io/api/project_branches/list?project=${PROJECT}" | jq '.branches[] | {name, isMain}'
done
```

## Troubleshooting

Check project exists:

```bash
curl -s -u "${SONAR_TOKEN}:" \
  "https://sonarcloud.io/api/projects/search?organization=${SONAR_ORG}" | jq '.components[].key'
```

Check analysis status:

```bash
curl -s -u "${SONAR_TOKEN}:" \
  "https://sonarcloud.io/api/ce/activity?component=${SONAR_PROJECT}" | jq '.tasks[0].status'
```

Common issues:
- **"Not analyzed" or "master branch has not been analyzed yet"** — SonarCloud's default branch is `master`, but your repo uses `main`. Rename it via the API (see Step 3 above) or in the SonarCloud UI: go to **Administration > Branches & Pull Requests**, click the `master` branch settings, and rename it to `main`.
- **"Not authorized"** — Verify `SONAR_TOKEN` is correct (`gh secret list` to check it exists).
- **"Project not found"** — Verify the project key and organization match between your build config and SonarCloud.
- **No coverage data** — Ensure tests run and produce coverage reports before the sonar step.
- **Analysis not appearing** — SonarCloud free tier only analyzes public repositories. Check: `gh repo view --json visibility -q '.visibility'`
