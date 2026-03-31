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

### System URLs (per environment)

Each environment needs its deployment URLs. For simulated deployment (Docker Compose), these match the compose file port mappings. In real deployments, these would be your actual service URLs.

**Multitier environments** (4 variables each):

| Variable | Description |
|---|---|
| `SYSTEM_UI_URL` | Frontend URL (e.g., `http://localhost:3101`) |
| `SYSTEM_API_URL` | Backend API URL (e.g., `http://localhost:8101`) |
| `ERP_URL` | ERP external service URL (e.g., `http://localhost:9101/erp`) |
| `CLOCK_URL` | Clock external service URL (e.g., `http://localhost:9101/clock`) |

**Monolith environments** (1 variable each):

| Variable | Description |
|---|---|
| `SYSTEM_URL` | Application URL (e.g., `http://localhost:2101`) |

For the starter repo, set variables on each environment:

```bash
# Multitier Java (ports: FE 3101, BE 8101, Ext 9101)
for stage in acceptance qa production; do
  gh variable set SYSTEM_UI_URL --body "http://localhost:3101" --env multitier-java-${stage} --repo <owner>/<repo>
  gh variable set SYSTEM_API_URL --body "http://localhost:8101" --env multitier-java-${stage} --repo <owner>/<repo>
  gh variable set ERP_URL --body "http://localhost:9101/erp" --env multitier-java-${stage} --repo <owner>/<repo>
  gh variable set CLOCK_URL --body "http://localhost:9101/clock" --env multitier-java-${stage} --repo <owner>/<repo>
done

# Multitier .NET (ports: FE 3201, BE 8201, Ext 9201)
for stage in acceptance qa production; do
  gh variable set SYSTEM_UI_URL --body "http://localhost:3201" --env multitier-dotnet-${stage} --repo <owner>/<repo>
  gh variable set SYSTEM_API_URL --body "http://localhost:8201" --env multitier-dotnet-${stage} --repo <owner>/<repo>
  gh variable set ERP_URL --body "http://localhost:9201/erp" --env multitier-dotnet-${stage} --repo <owner>/<repo>
  gh variable set CLOCK_URL --body "http://localhost:9201/clock" --env multitier-dotnet-${stage} --repo <owner>/<repo>
done

# Multitier TypeScript (ports: FE 3301, BE 8301, Ext 9301)
for stage in acceptance qa production; do
  gh variable set SYSTEM_UI_URL --body "http://localhost:3301" --env multitier-typescript-${stage} --repo <owner>/<repo>
  gh variable set SYSTEM_API_URL --body "http://localhost:8301" --env multitier-typescript-${stage} --repo <owner>/<repo>
  gh variable set ERP_URL --body "http://localhost:9301/erp" --env multitier-typescript-${stage} --repo <owner>/<repo>
  gh variable set CLOCK_URL --body "http://localhost:9301/clock" --env multitier-typescript-${stage} --repo <owner>/<repo>
done

# Monolith Java (port: 2101)
for stage in acceptance qa production; do
  gh variable set SYSTEM_URL --body "http://localhost:2101" --env monolith-java-${stage} --repo <owner>/<repo>
done

# Monolith .NET (port: 2201)
for stage in acceptance qa production; do
  gh variable set SYSTEM_URL --body "http://localhost:2201" --env monolith-dotnet-${stage} --repo <owner>/<repo>
done

# Monolith TypeScript (port: 2301)
for stage in acceptance qa production; do
  gh variable set SYSTEM_URL --body "http://localhost:2301" --env monolith-typescript-${stage} --repo <owner>/<repo>
done
```

> **Note:** You created these credentials during [Prerequisites](01-prerequisites.md). If you haven't yet, go back and create your Docker Hub token and SonarCloud token first.

## Checklist

1. Environments created (per architecture/language × stage)
2. `DOCKERHUB_TOKEN` secret is set
3. `SONAR_TOKEN` secret is set
4. `DOCKERHUB_USERNAME` variable is set
5. System URL variables set on each environment (`SYSTEM_UI_URL`, `SYSTEM_API_URL`, `ERP_URL`, `CLOCK_URL` for multitier; `SYSTEM_URL` for monolith)
