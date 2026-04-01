# Repository Setup

Before applying any pipeline template, set up all environments, secrets, and variables on your repository. This avoids scattered setup across later steps and ensures the first workflow run has everything it needs.

## 1. Create Environments

Each architecture/language combination gets its own set of environments (acceptance, qa, production). This allows each system to have its own deployment URLs — matching how real deployments work.

For the starter repo (all combos):

```bash
# Multitier environments
for arch in multitier-java multitier-dotnet multitier-typescript; do
  for stage in acceptance qa production; do
    gh api repos/<owner>/<repo>/environments/${arch}-${stage} -X PUT
  done
done

# Monolith environments
for arch in monolith-java monolith-dotnet monolith-typescript; do
  for stage in acceptance qa production; do
    gh api repos/<owner>/<repo>/environments/${arch}-${stage} -X PUT
  done
done
```

For a scaffolded repo (single architecture/language), you only need 3 environments:

```bash
gh api repos/<owner>/<repo>/environments/acceptance -X PUT
gh api repos/<owner>/<repo>/environments/qa -X PUT
gh api repos/<owner>/<repo>/environments/production -X PUT
```

## 2. Set Secrets and Variables

### Secrets (repo-level)

```bash
gh secret set DOCKERHUB_TOKEN --body "<your-dockerhub-token>" --repo <owner>/<repo>
gh secret set SONAR_TOKEN --body "<your-sonarcloud-token>" --repo <owner>/<repo>
```

### Variables (repo-level)

```bash
gh variable set DOCKERHUB_USERNAME --body "<your-dockerhub-username>" --repo <owner>/<repo>
```

> **Note:** System deployment URLs (e.g., `SYSTEM_UI_URL`, `SYSTEM_API_URL`, `ERP_URL`, `CLOCK_URL`) are hardcoded directly in the workflow files to match the Docker Compose port mappings. No environment-level URL variables are needed.

> **Note:** You created these credentials during [Prerequisites](01-prerequisites.md). If you haven't yet, go back and create your Docker Hub token and SonarCloud token first.

## Checklist

1. Environments created (per architecture/language × stage)
2. `DOCKERHUB_TOKEN` secret is set
3. `SONAR_TOKEN` secret is set
4. `DOCKERHUB_USERNAME` variable is set
