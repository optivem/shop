# Plan — Docker Best Practices Cleanup

**Date:** 2026-04-27
**Source:** Review of all 7 Dockerfiles, 7 .dockerignore files, and 24 docker-compose files in the shop repo.
**Scope:** Apply Docker best practices that are currently missing. Items ordered by priority (security/correctness first, then performance, then hygiene). Each item is independent and commit-safe on its own.

---

## 7. Pin image digests in pipeline compose files

**Status:** All compose files use mutable tags (`postgres:16-alpine`, `wiremock/wiremock:3.10.0`, `node:18-alpine`). For reproducibility in CI, pin digests.

**Affected files:** 12 `docker-compose.pipeline.*.yml` files.

**Actions:**
- For each external image used in pipeline compose, look up the current digest (`docker pull <image> && docker inspect <image> --format '{{index .RepoDigests 0}}'`) and pin: `image: postgres:16-alpine@sha256:...`.
- Skip local compose files — keep them on tags for dev convenience.
- Add Renovate/Dependabot config to keep digests fresh (check if already exists in `.github/`).

**Verification:** `docker compose -f docker-compose.pipeline.X.yml pull` returns stable digests.

---

## 8. Tighten `.dockerignore` files

**Status:** Each `.dockerignore` lists `README.md` and `Run-Sonar.ps1` individually. Build context could be smaller and more uniform.

**Affected files:** All 7 `.dockerignore` files.

**Actions per file:**
- Replace `README.md` (and `HELP.md` for Java) with broader `*.md` + `!path/needed.md` if any markdown is consumed at build time (none observed).
- Add: `Dockerfile.*`, `docker-compose*.yml`, `.github/`, `.gitignore`, `.editorconfig`, `*.sln` (.NET only), `tests/`, `test/` (TS — verify not needed for build first).
- Verify build still works after each addition (`docker build .` should not error on missing files).

**Verification:** `docker build` shows smaller "transferring context" size.

---

## 9. Frontend nginx — drop shell wrapper in CMD

**Status:** `system/multitier/frontend-react/Dockerfile` line 32 uses `CMD ["/bin/sh", "-c", "envsubst ... && nginx -g 'daemon off;'"]`. The official `nginx:alpine` (and `nginxinc/nginx-unprivileged:alpine`) image already supports `/etc/nginx/templates/*.template` natively via its entrypoint script.

**Action:** Remove the shell wrapper. Just `COPY nginx.conf /etc/nginx/templates/default.conf.template` and let the image's entrypoint handle envsubst. Default `CMD` becomes unnecessary.

**Verification:** Container starts, `$BACKEND_API_URL` is substituted.

---

## 10. Postgres password env var fallback in pipeline compose

**Status:** All compose files hardcode `POSTGRES_PASSWORD=app`. Fine for local; for pipeline.real running in CI, allow override.

**Affected files:** 12 `docker-compose.pipeline.*.yml` files.

**Action:** Change `POSTGRES_PASSWORD=app` to `POSTGRES_PASSWORD=${POSTGRES_PASSWORD:-app}` in pipeline compose files. Same for backend `POSTGRES_DB_PASSWORD`. Local compose stays as-is (dev convenience).

**Verification:** `POSTGRES_PASSWORD=secret docker compose -f ...pipeline... up` overrides; without override falls back to `app`.

---

## Order of execution

Execute in numerical order. Each item is committable on its own — commit after each item lands and tests pass.

After all items complete, delete this file. If `plans/` is empty, delete the directory.
