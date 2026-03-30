# Commit Stage - Multitier

For a working example, see the [Starter](https://github.com/optivem/starter) template.

## 1. Decompose System

Suppose your architecture is Frontend + Backend.

1. Create `frontend` and `backend` folders in your repo.
2. Migrate functionality from `monolith` into `frontend` and `backend`.
3. Delete the `monolith` folder.
4. Run frontend and backend locally and verify the application loads in your browser.

Do not commit/push yet.

## 2. Decompose Commit Stage

In `.github/workflows`:

**Create the Frontend Commit Stage:**

1. Copy `commit-stage-monolith.yml` to `commit-stage-frontend.yml`.
2. Find-replace `monolith` with `frontend`.
3. Replace the build steps (between Checkout Repository and Publish Docker Image) with your frontend language's build commands.

**Create the Backend Commit Stage:**

1. Copy `commit-stage-monolith.yml` to `commit-stage-backend.yml`.
2. Find-replace `monolith` with `backend`.
3. Replace the build steps with your backend language's build commands.

**Delete the Monolith Commit Stage:**

1. Delete `commit-stage-monolith.yml`.

**Java/Gradle projects:** Verify that `backend/gradle/wrapper/gradle-wrapper.jar` is tracked by git (`git ls-files backend/gradle`). If it's missing (e.g., due to a global gitignore excluding `*.jar`), force-add it: `git add -f backend/gradle/wrapper/gradle-wrapper.jar`

**Commit and push (CLI):**

```bash
git add -A && git commit -m "Decompose into multitier" && git push
```

> After pushing, the Acceptance Stage, QA Stage, and Production Stage will fail because they still reference "monolith". This is expected — you will update each stage in the corresponding multitier lessons. For now, only verify that the Commit Stages pass.

Verify (CLI):

```bash
gh run list --repo <owner>/<repo> --limit 5 --json name,status,conclusion
```

- `commit-stage-frontend` passes
- `commit-stage-backend` passes
- `commit-stage-monolith` no longer exists

Verify packages exist (CLI):

```bash
gh api users/<owner>/packages?package_type=container --jq '.[].name'
```

Delete the `monolith` package (browser — cannot be done via CLI):

Go to Packages → click monolith → Package settings → Delete this package.

## 3. Update README

Delete the `commit-stage-monolith` status badge and replace it with badges for `commit-stage-frontend` and `commit-stage-backend`.

## 4. Update Docker Compose

In `system-test/docker-compose.yml`:

1. Replace the single monolith service with separate `frontend` and `backend` services.
2. Update the image references accordingly.
3. **Port mapping:** Verify the backend's actual listening port (check `.env`, `Dockerfile`, or startup config). The nginx reverse proxy and docker-compose port mapping must match. For example, if the backend listens on port 8080 (common for NestJS via `.env`), the nginx proxy should target `http://backend:8080`, not the default `3000`.
4. Run Docker Compose locally and verify the application loads.
5. Run the system tests locally and verify they pass.

## Frontend + Microservice Backend

*Only if your project uses a microservice architecture.* Complete the Frontend + Backend steps above first, then split the backend into microservices — create a separate Commit Stage per microservice and update the README and Docker Compose accordingly.

## Checklist

1. `commit-stage-frontend` passes
2. `commit-stage-backend` passes
3. `commit-stage-monolith` is deleted
4. `monolith` package is deleted from Packages (browser)
5. README badges updated
6. Docker Compose updated and working locally
