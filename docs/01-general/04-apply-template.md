# Apply Template

This repository (`starter`) is the template. It contains workflows, system code, and system tests for Java, .NET, and TypeScript.

## 1. Copy Template Files

Choose your architecture and copy the corresponding files from the starter template into your repository.

*If your chosen language is not on the list, no worries, just choose any of the templates because the Pipeline Architecture is the same — you can do language replacement afterwards.*

### Monolith

Set your language variables:
```bash
STARTER_PATH="<path-to-starter-repo>"
LANG="<your-language>"           # java, dotnet, or typescript
TEST_LANG="<your-test-language>" # java, dotnet, or typescript (same as LANG unless QA uses a different language)
```

Copy workflows, system code, system tests, and VERSION file — **only for your chosen languages**:
```bash
mkdir -p .github/workflows system/monolith system-test

# Commit stage workflow (based on system language)
cp "$STARTER_PATH/.github/workflows/monolith-${LANG}-commit-stage.yml" .github/workflows/

# Acceptance, QA, QA signoff, and production stage workflows (based on test language)
cp "$STARTER_PATH/.github/workflows/monolith-${TEST_LANG}-acceptance-stage.yml" .github/workflows/
cp "$STARTER_PATH/.github/workflows/monolith-${TEST_LANG}-qa-stage.yml" .github/workflows/
cp "$STARTER_PATH/.github/workflows/monolith-${TEST_LANG}-qa-signoff.yml" .github/workflows/
cp "$STARTER_PATH/.github/workflows/monolith-${TEST_LANG}-prod-stage.yml" .github/workflows/

# Verify workflow (only when system and test languages are the same)
if [ "$LANG" = "$TEST_LANG" ]; then
  cp "$STARTER_PATH/.github/workflows/monolith-${LANG}-verify.yml" .github/workflows/
fi

# System code and system tests
cp -r "$STARTER_PATH/system/monolith/${LANG}" system/monolith/
cp -r "$STARTER_PATH/system-test/${TEST_LANG}" system-test/
cp -f "$STARTER_PATH/VERSION" . 2>/dev/null || true
```

- Also copy the top part of `README.md` (the status badges section) from the template — update the badge URLs to use your language (e.g. `monolith-java-commit-stage`).

### Multitier

Set your language variables:
```bash
STARTER_PATH="<path-to-starter-repo>"
BACKEND_LANG="<your-backend-language>"   # java, dotnet, or typescript
FRONTEND_LANG="<your-frontend-language>" # react
TEST_LANG="<your-test-language>"         # java, dotnet, or typescript (same as BACKEND_LANG unless QA uses a different language)
```

Copy workflows, system code, system tests, and VERSION file — **only for your chosen languages**:
```bash
mkdir -p .github/workflows system/multitier system-test

# Commit stage workflows (one per component)
cp "$STARTER_PATH/.github/workflows/multitier-backend-${BACKEND_LANG}-commit-stage.yml" .github/workflows/
cp "$STARTER_PATH/.github/workflows/multitier-frontend-${FRONTEND_LANG}-commit-stage.yml" .github/workflows/

# Acceptance, QA, QA signoff, and production stage workflows (based on test language)
cp "$STARTER_PATH/.github/workflows/multitier-system-${TEST_LANG}-acceptance-stage.yml" .github/workflows/
cp "$STARTER_PATH/.github/workflows/multitier-system-${TEST_LANG}-qa-stage.yml" .github/workflows/
cp "$STARTER_PATH/.github/workflows/multitier-system-${TEST_LANG}-qa-signoff.yml" .github/workflows/
cp "$STARTER_PATH/.github/workflows/multitier-system-${TEST_LANG}-prod-stage.yml" .github/workflows/

# Verify workflow (only when backend and test languages are the same)
if [ "$BACKEND_LANG" = "$TEST_LANG" ]; then
  cp "$STARTER_PATH/.github/workflows/multitier-${BACKEND_LANG}-verify.yml" .github/workflows/
fi

# System code (backend + frontend)
cp -r "$STARTER_PATH/system/multitier/backend-${BACKEND_LANG}" system/multitier/
cp -r "$STARTER_PATH/system/multitier/frontend-${FRONTEND_LANG}" system/multitier/

# System tests
cp -r "$STARTER_PATH/system-test/${TEST_LANG}" system-test/
cp -f "$STARTER_PATH/VERSION" . 2>/dev/null || true
```

- Also copy the top part of `README.md` (the status badges section) from the template — update the badge URLs to use your languages (e.g. `multitier-backend-java-commit-stage`, `multitier-frontend-react-commit-stage`).

## 2. Replace Repository References

Replace `optivem/starter` with `<your_repo_owner>/<your_repo_name>` in the whole project (CLI).

> **macOS note:** The `sed -i` commands below use Linux syntax. On macOS, use `sed -i ''` instead of `sed -i` (add an empty string argument after `-i`).

```bash
grep -rl "optivem/starter" . --include="*.yml" --include="*.yaml" --include="*.md" --include="*.gradle" --include="*.gradle.kts" --include="*.csproj" --include="*.sln" --include="*.cshtml" --include="*.json" --include="Dockerfile" | xargs sed -i 's|optivem/starter|<owner>/<repo>|g'
```

Also replace the underscore variant (used by SonarCloud config) and standalone `optivem` org reference (CLI):

```bash
grep -rl "optivem_starter" . --include="*.yml" --include="*.yaml" --include="*.gradle" --include="*.gradle.kts" --include="*.csproj" --include="*.sln" | xargs sed -i 's|optivem_starter|<owner>_<repo>|g'
grep -rl "sonar.organization.*optivem\|/o:\"optivem\"\|/o:.*optivem" . --include="*.yml" --include="*.yaml" --include="*.gradle" --include="*.gradle.kts" --include="*.csproj" | xargs sed -i -e "s|sonar.organization=optivem|sonar.organization=<owner>|g" -e "s|'sonar.organization', 'optivem'|'sonar.organization', '<owner>'|g" -e 's|/o:"optivem"|/o:"<owner>"|g' -e "s|/o:optivem|/o:<owner>|g"
```

> **Warning:** After running the SonarCloud org replacement, verify that `optivem/actions` references in `.github/workflows/*.yml` are still intact. If any were changed to `<owner>/actions`, revert them — those must remain as `optivem/actions`.

This covers `.yml` files (including `docker-compose.yml` and workflow files), `.md` files, `.gradle`/`.gradle.kts` files (including SonarCloud config), and .NET files (`.csproj`, `.sln`, `.cshtml`, `.json`, `Dockerfile`):
- In the README file, so that the status badges point to your workflows (not the template workflows)
- In `system-test/docker-compose.yml`, to reference your Docker Image (not the template image)
- In SonarCloud config (`sonar.projectKey` and `sonar.organization`), so analysis runs under your organization

## 3. Docker Compose

In the Docker Compose file, ensure that everything is lowercase in the image url.

## 4. Create SonarCloud Project

The SonarCloud project key must match the `sonar.projectKey` in your build config file. After the sed replacement in step 2, check your build config to find the actual key:
- **Java:** `system/monolith/<lang>/build.gradle` or `system/multitier/backend-<lang>/build.gradle` — look for `sonar.projectKey`
- **.NET:** `.github/workflows/*-commit-stage.yml` — look for `/k:"..."` in the `dotnet sonarscanner begin` command
- **TypeScript:** `.github/workflows/*-commit-stage.yml` — look for `-Dsonar.projectKey=...`

```bash
SONAR_ORG="<your-github-owner>"
SONAR_PROJECT="<project-key-from-build-config>"  # e.g. myowner_myrepo-monolith-java

# Create organization (skip if already exists)
curl -s -u "${SONAR_TOKEN}:" \
  -X POST "https://sonarcloud.io/api/organizations/create" \
  -d "key=${SONAR_ORG}&name=${SONAR_ORG}"

# Create project
curl -s -u "${SONAR_TOKEN}:" \
  -X POST "https://sonarcloud.io/api/projects/create" \
  -d "organization=${SONAR_ORG}&project=${SONAR_PROJECT}&name=${SONAR_PROJECT}"

# Rename default branch from master to main
curl -s -u "${SONAR_TOKEN}:" \
  -X POST "https://sonarcloud.io/api/project_branches/rename" \
  -d "project=${SONAR_PROJECT}&name=main"
```

> For more details, troubleshooting, and bulk operations, see [SonarCloud Setup](05-sonarcloud-setup.md).

**Multitier note:** If your architecture has multiple components with separate SonarCloud projects (e.g. backend and frontend), repeat the project creation for each component.

## 5. Namespace Replacement

1. Find template namespace references:
   - Java: `com.optivem.starter`
   - .NET: `Optivem.Starter`
   - TypeScript: `@optivem/starter-system-test`
   - Also search for any other references like "accelerator" and "Accelerator"
   - For TypeScript, also update `author`, `license`, `description` in `package.json`
   - For .NET, also check `.cshtml` (Razor views) and `Dockerfile` — these contain namespace and project references
   - For TypeScript, also check `Dockerfile` — it may contain project references
2. Replace all references with your corresponding namespace and info.
   - Also update the README title to your system name and language.

## 6. Commit, Push, and Verify

1. Commit and push (CLI):
   ```bash
   git add -A && git commit -m "Apply pipeline template" && git push
   ```
2. Wait for the commit stage to finish (CLI):
   ```bash
   gh run watch --repo <owner>/<repo>
   ```
3. Trigger the acceptance stage and wait for it to finish (CLI):

   **Monolith:**
   ```bash
   gh workflow run "monolith-${TEST_LANG}-acceptance-stage.yml" --repo <owner>/<repo>
   gh run watch --repo <owner>/<repo>
   ```

   **Multitier:**
   ```bash
   gh workflow run "multitier-system-${TEST_LANG}-acceptance-stage.yml" --repo <owner>/<repo>
   gh run watch --repo <owner>/<repo>
   ```

## Customization

*Skip this section if your chosen language already matches the template you applied.*

### Monolith Language

1. Open the `monolith` folder.
2. **If your target language is Java, .NET, or TypeScript:**
   - Find the target language template in the starter repo.
   - Delete the `monolith` folder in your repo.
   - Copy from the target template: `monolith` folder, `system-test/docker-compose.yml` (overwrite), `.github/workflows/commit-stage-monolith.yml` (overwrite).
   - In `system-test/docker-compose.yml`, replace `optivem/starter` with `<your_repo_owner>/<your_repo_name>` for the image field.
3. **If your target language is something else:**
   - Rewrite the `monolith` folder in your target language (or use an LLM to assist).
   - Update the `monolith` README.md with build and run instructions.
   - Build and run locally. Note the port (e.g. 2500).
   - In `system-test/docker-compose.yml`, set ports to `8080:YOUR_PORT`.
   - In `.github/workflows/commit-stage-monolith.yml`, replace the steps between 'Checkout Repository' and 'Publish Docker Image' with your language's setup and build commands.
4. Commit and push (CLI):
   ```bash
   git add -A && git commit -m "Customize monolith language" && git push
   ```
5. Verify that the commit stage and acceptance stage workflows pass (CLI):
   ```bash
   gh run watch --repo <owner>/<repo>
   ```

### System Test Language

*Skip this section if your developers and QA automation engineers use the same language.*

1. Open the `system-test` folder.
2. **If your target language is Java, .NET, or TypeScript:**
   - Find the target template in the starter repo.
   - Delete everything in `system-test` **except** `docker-compose.yml`.
   - Copy from the target template: `system-test` folder (except `docker-compose.yml`), `.github/workflows/acceptance-stage.yml`.
3. **If your target language is something else:**
   - Rewrite the `system-test` folder in your target language. Check Playwright language support; switch to Selenium if needed.
   - Update the `system-test` README.md with instructions to run tests.
   - In `.github/workflows/acceptance-stage.yml`, replace the steps after 'Deploy System' with your language setup and E2E test commands.
4. Commit and push (CLI):
   ```bash
   git add -A && git commit -m "Customize system test language" && git push
   ```
5. Trigger `acceptance-stage` and verify it passes (CLI):
   ```bash
   gh workflow run acceptance-stage.yml --repo <owner>/<repo>
   gh run watch --repo <owner>/<repo>
   ```

> **Why different languages?** It is common for development teams and QA teams to use different languages.

## Checklist

1. Template files copied for your chosen architecture and language(s)
2. All references to `optivem/starter` have been replaced with your own repository
3. Docker Compose image urls are lowercase
4. SonarCloud project created with correct project key from build config
5. Namespace customization is complete
6. Root README file contains correct links to GitHub Actions
7. Commit stage and acceptance stage workflows pass
