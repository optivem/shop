# Repository Setup

Before applying any pipeline template, set up all environments, secrets, and variables on your repository. This avoids scattered setup across later steps and ensures the first workflow run has everything it needs.

## 1. Create Environments

Create the `qa` and `production` GitHub environments (CLI):

```bash
gh api repos/<owner>/<repo>/environments/qa -X PUT
gh api repos/<owner>/<repo>/environments/production -X PUT
```

## 2. Set Secrets and Variables

Set all credentials and variables at once (CLI):

```bash
# Secrets
gh secret set DOCKERHUB_TOKEN --body "<your-dockerhub-token>" --repo <owner>/<repo>
gh secret set SONAR_TOKEN --body "<your-sonarcloud-token>" --repo <owner>/<repo>

# Variables
gh variable set DOCKERHUB_USERNAME --body "<your-dockerhub-username>" --repo <owner>/<repo>
gh variable set SYSTEM_URL --body "http://localhost:8080" --repo <owner>/<repo>
```

> **Note:** You created these credentials during [Prerequisites](01-prerequisites.md). If you haven't yet, go back and create your Docker Hub token and SonarCloud token first.

## Checklist

1. `qa` environment exists
2. `production` environment exists
3. `DOCKERHUB_TOKEN` secret is set
4. `SONAR_TOKEN` secret is set
5. `DOCKERHUB_USERNAME` variable is set
6. `SYSTEM_URL` variable is set
