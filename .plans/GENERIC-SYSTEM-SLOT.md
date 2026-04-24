# Plan: Generic `system` slot in artifact identifiers

## Status: DRAFT — awaiting go-ahead

## Relationship to PLACEHOLDER-RENAME

**Land this first, then land PLACEHOLDER-RENAME.** Every slot this plan rewrites is one less placeholder the rename plan has to track.

## Goal

The repo already treats a handful of words as generic architecture vocabulary that is **never replaced during scaffolding**: `backend`, `frontend`, `monolith`, `multitier`. Docker image names extend this list with `system`, used as the "single deployable = the whole system" role in monolith images:

- `ghcr.io/${github.repository}/monolith-system-java`
- `ghcr.io/${github.repository}/multitier-backend-java`
- `ghcr.io/${github.repository}/multitier-frontend-react`

No placeholder appears in any of these names — they are fully generic.

Other artifact identifiers are **not** consistent with that model. They embed `shop` as the system-name slot and so require placeholder replacement at scaffold time:

- SonarCloud: `optivem_shop-monolith-java`, `shop-multitier-backend-java`, …
- docker-compose project names: `name: shop-monolith`, `name: shop-stub`, `name: shop-real`, `name: shop-backend`
- docker-compose DB env: `POSTGRES_DB=shop`, `POSTGRES_USER=shop_user`, `POSTGRES_PASSWORD=shop_password`
- Workflow inputs/env: `shop-tag`, `SHOP_TAG`, `shop-auto-bump-patch` concurrency group
- Terraform: `optivem-shop`, `optivem-shop-${hex}`
- Devcontainer: `optivem-shop`
- npm package names: `@optivem/shop-system-test`, `optivem-shop-frontend`, `shop-backend`, `shop-monolith`

This plan promotes `system` to a real generic slot, used everywhere the system name appears next to architecture descriptors (`monolith`, `multitier-backend`, `multitier-frontend-react`, `tests`, etc.). After this change, those identifiers need no placeholder substitution — they read as `system-monolith-java`, `my-company_system-multitier-backend-java`, and so on.

---

## Scope

**In scope (rewrite `shop` → `system`):** artifact identifiers — things published, named, tagged, or registered that describe "the system".

**Out of scope (handled by PLACEHOLDER-RENAME):** every `shop`/`Shop` reference inside source code, domain objects, directory structure, doc prose, class names, driver names, imports, Java packages, .NET namespaces. Those remain meaningful domain names and still get renamed to `my-shop`/`MyShop` during scaffolding.

Boundary rule: **if the identifier sits next to an architecture word (`monolith`, `multitier`, `backend`, `frontend`, `tests`) or is a workflow/container/org-level label, rewrite it. Otherwise leave it for PLACEHOLDER-RENAME.**

---

## Inventory

### A. SonarCloud project keys / names

| File | Before | After |
|---|---|---|
| `.github/workflows/monolith-dotnet-commit-stage.yml` | `/k:"optivem_shop-monolith-dotnet" /n:"shop-monolith-dotnet"` | `/k:"optivem_system-monolith-dotnet" /n:"system-monolith-dotnet"` |
| `.github/workflows/monolith-typescript-commit-stage.yml` | `optivem_shop-monolith-typescript`, `shop-monolith-typescript` | `optivem_system-monolith-typescript`, `system-monolith-typescript` |
| `.github/workflows/multitier-backend-dotnet-commit-stage.yml` | `optivem_shop-multitier-backend-dotnet`, `shop-multitier-backend-dotnet` | `optivem_system-multitier-backend-dotnet`, `system-multitier-backend-dotnet` |
| `.github/workflows/multitier-backend-typescript-commit-stage.yml` | `optivem_shop-multitier-backend-typescript`, `shop-multitier-backend-typescript` | `optivem_system-multitier-backend-typescript`, `system-multitier-backend-typescript` |
| `.github/workflows/multitier-frontend-react-commit-stage.yml` | `optivem_shop-multitier-frontend-react`, `shop-multitier-frontend-react` | `optivem_system-multitier-frontend-react`, `system-multitier-frontend-react` |
| `system/monolith/java/build.gradle` | `optivem_shop-monolith-java`, `shop-monolith-java` | `optivem_system-monolith-java`, `system-monolith-java` |
| `system/multitier/backend-java/build.gradle` | `optivem_shop-multitier-backend-java`, `shop-multitier-backend-java` | `optivem_system-multitier-backend-java`, `system-multitier-backend-java` |
| `system-test/dotnet/Run-Sonar.ps1` | `optivem_shop-tests-dotnet` | `optivem_system-tests-dotnet` |

Cross-check: audit `system-test/java/**` and `system-test/typescript/**` for equivalent sonar keys (not surfaced in initial grep; confirm during execution).

**Required side effect:** each renamed project key must be re-registered in SonarCloud before the first commit that uses it, or the scan will fail. Coordinate with the user on who does the register step.

### B. docker-compose project and container names

| File | Before | After |
|---|---|---|
| `system/monolith/*/docker-compose*.yml` | `name: shop-monolith` | `name: system-monolith` |
| `system/external-stub/docker-compose*.yml` | `name: shop-stub` | `name: system-stub` |
| `system/external-real-sim/docker-compose*.yml` | `name: shop-real` | `name: system-real` |
| `system/multitier/**/docker-compose*.yml` | `name: shop-backend`, `name: shop-frontend` | `name: system-backend`, `name: system-frontend` |
| `system-test/*/Run-SystemTests.Config.Architecture.ps1` | `ContainerName = "shop-stub"`, `"shop-real"` | `ContainerName = "system-stub"`, `"system-real"` |

### C. docker-compose database identifiers

These are internal to the container network — they don't leak into published artifacts — but they *are* placeholder slots. Rewriting them now is the same benefit (one fewer thing for PLACEHOLDER-RENAME to touch).

| File | Before | After |
|---|---|---|
| `system/**/docker-compose*.yml` | `POSTGRES_DB=shop` | `POSTGRES_DB=system` |
| `system/**/docker-compose*.yml` | `POSTGRES_USER=shop_user` | `POSTGRES_USER=system_user` |
| `system/**/docker-compose*.yml` | `POSTGRES_PASSWORD=shop_password` | `POSTGRES_PASSWORD=system_password` |
| `system/**/docker-compose*.yml` | `pg_isready -U shop -d shop` | `pg_isready -U system -d system` |

Also chase application config files (`application.yml`, `appsettings*.json`, `.env*`, TS config) that hard-code `shop` / `shop_user` / `shop_password` — must flip together with compose to avoid a connection failure mid-rename.

### D. Workflow inputs / env vars / concurrency groups

| File | Before | After |
|---|---|---|
| `.github/workflows/meta-release-stage.yml` | `SHOP_TAG`, `shop-tag` | `SYSTEM_TAG`, `system-tag` |
| `.github/workflows/auto-bump-patch.yml` | `group: shop-auto-bump-patch` | `group: system-auto-bump-patch` |
| any other workflow passing `shop-tag` input | same rename | same rename |

**Coordination:** the receiving end of `shop-tag` lives in `optivem/actions` (per PLACEHOLDER-RENAME line 103). The action input must be renamed in lockstep with the caller — single PR across both repos, or feature-flag via supporting both names for one release cycle. Prefer the single-PR option.

### E. Terraform

| File | Before | After |
|---|---|---|
| `terraform/main.tf` | `name = "optivem-shop"`, `project_id = "optivem-shop-${hex}"` | `name = "optivem-system"`, `project_id = "optivem-system-${hex}"` |

GCP project IDs are globally unique across all of GCP — renaming the project ID requires provisioning a new project. **Defer** this one unless the user confirms the existing GCP project is expendable; otherwise leave Terraform for the PLACEHOLDER-RENAME phase and accept the single placeholder swap there.

### F. Devcontainer

| File | Before | After |
|---|---|---|
| `.devcontainer/devcontainer.json` | `"name": "optivem-shop"` | `"name": "optivem-system"` |

Display-name only; safe.

### G. npm package names

| File | Before | After |
|---|---|---|
| `system-test/typescript/package.json` | `"name": "@optivem/shop-system-test"` | `"name": "@optivem/system-tests"` |
| `system/multitier/frontend-react/package.json` | `"name": "optivem-shop-frontend"` | `"name": "optivem-system-frontend"` |
| `system/multitier/backend-typescript/package.json` | `"name": "shop-backend"` | `"name": "system-backend"` |
| `system/monolith/typescript/package.json` | `"name": "shop-monolith"` | `"name": "system-monolith"` |
| `**/package-lock.json` | cascade from above | regenerate via `npm install` |

These packages are not published (private workspaces); the name is a local label. Safe to rename.

---

## What stays (do not rewrite)

- `ghcr.io/optivem/shop/…` image URIs — the `optivem/shop` portion is the repo path, resolved from `${github.repository}` and already generic. Don't touch.
- `optivem/shop` repo slug in README links, `@optivem/`-scoped published packages, `LICENSE`, author attributions — all publisher-real, not placeholders.
- `ShopController`, `ShopService`, `shop-api-driver.ts`, `com.optivem.shop.*`, `Optivem.Shop.*` — these are domain-model names, handled by PLACEHOLDER-RENAME, not this plan.
- The `shop/system-test` directory structure and the `system/` parent directory — unaffected (no `shop` in the path).

---

## Phases

### Phase 1 — Bulk rewrite in artifact identifier slots
Run per-section from the inventory (A–G). Each section is independently reviewable and independently testable (pipeline stage for A, `docker compose up` for B/C, workflow dispatch for D, GCP plan for E, dev container rebuild for F, `npm install` + build for G).

### Phase 2 — SonarCloud re-registration
Before the first push that carries the new keys, (re-)register every new `*_system-*` project in SonarCloud. Existing `*_shop-*` projects can be archived or kept as historical.

### Phase 3 — Coordinate with `optivem/actions`
If any action under `optivem/actions` names an input `shop-tag` (or similar), that must be renamed in the same PR window. Audit step: grep `optivem/actions` for `shop-tag`, `SHOP_TAG`, `inputs.shop-` before opening the shop PR.

### Phase 4 — Validation
- Full local build: .NET `dotnet build`, Java `./gradlew build`, TS `npm run build`
- System tests: `Run-SystemTests.ps1` (full suite per `feedback_full_test_runs`)
- Full pipeline dry-run against the branch — verify every commit-stage workflow creates its SonarCloud report under the new key
- Grep check: `rg -n "\bshop-(monolith|multitier|backend|frontend|stub|real|tag|system|tests|auto-bump)"` should return zero hits; `rg -n "optivem_shop-"` should return zero hits

### Phase 5 — Cut over PLACEHOLDER-RENAME
With this plan merged, re-scope PLACEHOLDER-RENAME's Phase 1d / 1e to cover only the residual placeholders (domain names, Java packages, .NET namespaces, source identifiers, and `optivem` → `my-company` company-level swaps). The workflow-inputs and SonarCloud sections of PLACEHOLDER-RENAME shrink substantially.

---

## Decisions required before execution

1. **Word choice: `system`.** Confirm. Alternatives considered:
   - `app` — too generic, collides with `appsettings.json` pattern in .NET.
   - `product` — less neutral, implies commercial offering.
   - `service` — confusing next to Cloud Run services.
   - **Recommended: `system`.** It's already established in the repo (top-level `system/` dir, `monolith-system-*` image names), short, and neutral.
2. **Terraform rewrite (section E): in or out?** Recommended: **out** for this plan, handled inside PLACEHOLDER-RENAME. Rationale: GCP project ID rename is a re-provisioning operation, not a refactor.
3. **Single PR or split per section?** Recommended: **single PR**. Sections are related and splitting would leave the repo in mixed-naming intermediate states; the total diff size is moderate (~40–60 files) and reviewers can navigate by section heading.
4. **SonarCloud re-registration ownership.** Who runs it? (Needs SonarCloud admin rights.) Plan assumes the user handles it manually before PR merge.
5. **Courses impact.** Does `courses/` reference any of these artifact identifiers by literal name (e.g. "check the `shop-monolith-java` Sonar dashboard")? If yes, they need a coordinated update — add to PLACEHOLDER-RENAME's FU-2 or handle here.

---

## Expected outcome

- `system` becomes the canonical generic system-slot word, alongside `backend`, `frontend`, `monolith`, `multitier`.
- PLACEHOLDER-RENAME's surface shrinks from ~12 placeholder literals × many artifact identifier slots down to the domain-name slots only.
- Scaffolded users' repos show `my-company_system-monolith-java` instead of `my-company_my-shop-monolith-java` in their SonarCloud — cleaner and one fewer thing they'd wonder whether to customize.
