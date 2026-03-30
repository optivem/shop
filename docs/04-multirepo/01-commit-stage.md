# Commit Stage - Multirepo

For a working example, see the [Starter](https://github.com/optivem/starter) template.

## 1. Separate Frontend

Create a new frontend repository (CLI):

```bash
gh repo create <owner>/<repo>-frontend --public --license mit --clone
```

1. In the system repository README, add a link to the new frontend repository.
2. Move the `frontend` folder and `.github/workflows/commit-stage-frontend.yml` to the frontend repository.
3. Move the frontend Commit Stage badge from the system repository README to the frontend repository README. Update the badge URL to point to the new repository.
4. Add credentials and variables to the new repository (CLI):
   ```bash
   gh variable set DOCKERHUB_USERNAME --body "<your-dockerhub-username>" --repo <owner>/<repo>-frontend
   gh secret set DOCKERHUB_TOKEN --body "<your-dockerhub-token>" --repo <owner>/<repo>-frontend
   ```
5. Commit and push in the frontend repository (CLI):
   ```bash
   git add -A && git commit -m "Move frontend from system repo" && git push
   ```
6. Verify the frontend Commit Stage passes (CLI):
   ```bash
   gh run watch --repo <owner>/<repo>-frontend
   ```
7. Verify the frontend package is created (CLI):
   ```bash
   gh api users/<owner>/packages?package_type=container --jq '.[].name'
   ```
8. Delete the frontend package from the system repository (browser — cannot be done via CLI):
   Go to Packages → click the package → Package settings → Delete.
9. Copy the frontend Commit Stage badge from the frontend repository back into the system repository README for a consolidated view.

## 2. Separate Backend

Repeat the same process for the backend.

> After moving components to separate repositories, the Acceptance Stage, QA Stage, and Production Stage will fail because they still reference the old image URLs. This is expected — you will update each stage in the corresponding multirepo lessons. For now, only verify that the Commit Stages pass.

## 3. Create SonarCloud Projects for Components

Create a separate SonarCloud project for each component repository:

```bash
SONAR_ORG="<your-sonar-org>"
SONAR_TOKEN="<your-sonar-token>"

curl -s -u "${SONAR_TOKEN}:" -X POST "https://sonarcloud.io/api/projects/create" \
  -d "name=<repo>-frontend&project=${SONAR_ORG}_<repo>-frontend&organization=${SONAR_ORG}"

curl -s -u "${SONAR_TOKEN}:" -X POST "https://sonarcloud.io/api/projects/create" \
  -d "name=<repo>-backend&project=${SONAR_ORG}_<repo>-backend&organization=${SONAR_ORG}"
```

Rename the default branch to `main` for each project:

```bash
curl -s -u "${SONAR_TOKEN}:" -X POST "https://sonarcloud.io/api/project_branches/rename" \
  -d "project=${SONAR_ORG}_<repo>-frontend&name=main"

curl -s -u "${SONAR_TOKEN}:" -X POST "https://sonarcloud.io/api/project_branches/rename" \
  -d "project=${SONAR_ORG}_<repo>-backend&name=main"
```

Add the `SONAR_TOKEN` secret to each component repository:

```bash
gh secret set SONAR_TOKEN --body "<your-sonar-token>" --repo <owner>/<repo>-frontend
gh secret set SONAR_TOKEN --body "<your-sonar-token>" --repo <owner>/<repo>-backend
```

## Checklist

1. Frontend repository created and commit stage passes
2. Backend repository created and commit stage passes
3. Old packages deleted from system repository (browser)
4. System README has links to component repos and consolidated badges
5. SonarCloud projects created for each component repository
6. `SONAR_TOKEN` secret set on each component repository
