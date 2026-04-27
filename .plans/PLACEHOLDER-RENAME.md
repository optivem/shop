# Plan: Rename template placeholders to `my-company` / `my-shop`

## Status: IN PROGRESS

## Goal

Replace the implicit placeholders `optivem` (company) and `shop` (system) inside this repo with distinctive multi-word literals. Once landed, the scaffolding logic in `gh-optivem/internal/steps/replacements.go` collapses to pure find-replace: no word-boundary checks, no `optivemAllowedPatterns`, no pass-ordering constraints.

This plan covers the `shop` repo only. `gh-optivem` simplification and `courses/` content updates are listed at the bottom as dependent follow-ups.

---

## Execution approach

Work **language by language**. For each language: bulk content replacement → file/dir renames → basic compilation check (`tsc --noEmit`, `dotnet build`, `gradlew compileJava`). Run `gh optivem test system --sample` **only at the end** after all languages are done (expensive to spin up per language).

Order:

1. **TypeScript** — monolith, multitier backend, frontend-react, system-test/typescript
2. **.NET** — monolith, multitier backend-dotnet, system-test/dotnet
3. **Java** — monolith, multitier backend-java, system-test/java
4. **Cross-cutting** — workflows, SonarCloud keys, docs/README/agent prompts
5. **`gh optivem test system --sample`** — one sample run per language once everything is renamed

### Decisions taken (recommendations accepted)

- **A** — Accept that `courses/` will need FU-2 update before next course release.
- **Yes** — Rewrite `api.optivem.com` → `api.my-company.example` in template contexts (errors URIs, etc.).
- **Single branch per language** — test-build-commit per language; do not mix languages in one commit.
- **Keep** `optivem/shop` GitHub repo name; only content inside is renamed.

### Token-minimization guidance (Claude tokens)

Every replacement is a find-replace across 100s–1000s of matches. Surgical `Edit` per-match is far too expensive in Claude tokens (~50x) — use bulk scripted replacement:

- **Prefer PowerShell `Get-ChildItem ... | ForEach-Object { (Get-Content) -replace ... | Set-Content }`** over Edit tool for repetitive replacements in many files.
- **One script pass per language** — all ordered replacements in a single script invocation, not one sed call per pattern.
- **Verify with `Grep` after**, not by re-reading each file.
- **Protect allowlisted strings with sentinels** (e.g. `@optivem/optivem-testing`, `ghcr.io/optivem/shop`, `"author": "Optivem"`, LICENSE) — replace with unique token before bulk pass, restore at end.
- **Directory/file renames via `git mv` in batches**, not one rename per tool call.
- **Do not read the same file twice**; the first `Grep` pass is enough to plan the changes.
- **Aim for ≤ 10 tool calls per phase** — audit, script, run, verify.

---

## Placeholder inventory

### Company (`optivem` → `my-company`)

| Form | Literal |
|---|---|
| kebab | `my-company` |
| PascalCase | `MyCompany` |
| camelCase | `myCompany` |
| lowercase no-sep | `mycompany` |
| snake_case | `my_company` |
| SCREAMING_SNAKE | `MY_COMPANY` |

### System (`shop` → `my-shop`)

| Form | Literal |
|---|---|
| kebab | `my-shop` |
| PascalCase | `MyShop` |
| camelCase | `myShop` |
| lowercase no-sep | `myshop` |
| snake_case | `my_shop` |
| SCREAMING_SNAKE | `MY_SHOP` |

12 literals total, zero substring overlap, zero collision with any English word or common identifier.

---

## What stays as-is (must NOT be renamed)

These are publisher-real references, not template placeholders:

- `optivem/actions` — published GitHub Actions repo
- `@optivem/` npm scope (e.g. `@optivem/optivem-testing`)
- `optivem-testing` — published npm package name
- `Optivem` / `optivem` in `LICENSE`, author attributions in `package.json`, git commit trailers
- Any doc text attributing the publisher (e.g. "this template is published by Optivem")

Concrete rule for the audit: if the string refers to **something published on GitHub/npm by Optivem-the-company and not part of the scaffolded output**, keep it. If it refers to **content that lands in a scaffolded user's repo and should bear the user's brand**, rename it.

---

## Critical decision point: course content impact

Course content under `courses/` declares "Shop" as the canonical example system name ([courses/docs/rules/00-shared.md:171-179](../../courses/docs/rules/00-shared.md#L171-L179)):

> Courses use **Shop** as the example system.
> - Official name: **Shop**
> - Application repo: [Shop](https://github.com/optivem/shop) (`shop`)

Every lesson that says *"look at the `ShopDriver` class"* becomes wrong after this rename. Two options:

- **A (recommended):** accept that `courses/` needs a follow-up update, land it before the next course release.
- **B:** abort this plan. Keep current placeholders, keep the allowlist complexity in `gh-optivem`.

**Confirm A or B before proceeding.**

---

## Phase 1 — Text replacements

Order matters within a pass but not across passes (literals have no substring overlap). Run in this order to minimize intermediate-state churn:

### 1a. .NET namespace `Optivem.Shop` → `MyCompany.MyShop`
- `system/monolith/dotnet/**/*.cs`, `*.cshtml`, `*.csproj`, `*.sln`, `*.slnx`, `*.json`
- `system/multitier/backend-dotnet/**/*` (same exts)
- `system-test/dotnet/**/*` (same exts)
- Dockerfiles that reference the namespace
- Also: `Optivem` → `MyCompany` and `Shop` → `MyShop` as standalone tokens inside .NET files

### 1b. Java package `com.optivem.shop` → `com.mycompany.myshop`
- `system/monolith/java/**/*.java`, `*.gradle`, `*.gradle.kts`, `*.xml`, `*.properties`
- `system/multitier/backend-java/**/*` (same exts)
- `system-test/java/**/*` (same exts)
- Also escaped-dot form `com\\.optivem\\.shop` in regex literals
- Also: bare `Optivem` → `MyCompany`, `Shop` → `MyShop` inside Java files where they appear as class-name prefixes

### 1c. TypeScript identifiers and imports
- `system/monolith/typescript/`, `system/multitier/backend-typescript/`, `system/multitier/frontend-react/`, `system-test/typescript/`
- `shop-*` kebab filenames/imports → `my-shop-*`
- `shopDriver` / `shopUiBaseUrl` camelCase identifiers → `myShopDriver` / `myShopUiBaseUrl`
- `'shop'` / `'shop_user'` / `'shop_password'` string literals in config → `'myshop'` / `'my_shop_user'` / `'my_shop_password'`
- `package.json` `name`, `author` fields

### 1d. Infrastructure / config strings
- `docker-compose*.yml`: `name: shop-*` → `name: my-shop-*`, `POSTGRES_DB=shop` → `POSTGRES_DB=myshop`, `POSTGRES_USER=shop_user` → `POSTGRES_USER=my_shop_user`, `POSTGRES_PASSWORD=shop_password` → `POSTGRES_PASSWORD=my_shop_password`, `pg_isready -U shop -d shop` → `pg_isready -U myshop -d myshop`
- SonarCloud keys: `optivem_shop` → `my-company_my-shop`, `sonar.projectName=shop-*` → `sonar.projectName=my-shop-*`, `sonar.organization=optivem` → `sonar.organization=my-company`
- Problem-details URI: `api.optivem.com/errors/` → `api.my-company.example/errors/`

### 1e. Workflows (`.github/workflows/*.yml`)
- `SHOP_TAG` → `MY_SHOP_TAG` in `meta-release-stage.yml` (and the kebab-form `shop-tag` workflow inputs it dispatches — audit the receiving end in `gh-optivem`)
- Image tag references inside workflow content that use `optivem-shop-*` as a label prefix (distinct from the `ghcr.io/optivem/shop` repo path, which is publisher-real and stays)
- Environment names prefixed with `monolith-java-` etc. are unaffected (they're not `optivem`/`shop` placeholders)

### 1f. Docs and agent prompts
- `README.md`, `CLAUDE.md`, `.devcontainer/README.md`
- `.claude/agents/*.md` — note `SHOP="$ACADEMY_ROOT/shop/system-test"` pattern in `compare-repos.md` and `compare-to-eshop-tests.md` becomes `MY_SHOP="$ACADEMY_ROOT/my-shop/system-test"` (but see Phase 2 — the **repo directory** on disk may or may not be renamed; decide below)
- `docs/**/*.md`
- Replace placeholder-context uses only; preserve publisher-context attributions

---

## Phase 2 — File and directory renames

Run after Phase 1 text replacements complete. These are filesystem operations:

- `.NET`: `Optivem.Shop.Monolith.sln` → `MyCompany.MyShop.Monolith.sln`, `Optivem.Shop.Backend.slnx` → `MyCompany.MyShop.Backend.slnx`, `Optivem.Shop.Monolith.Tests.csproj` → `MyCompany.MyShop.Monolith.Tests.csproj`, `Optivem.Shop.Backend.Tests.csproj` → `MyCompany.MyShop.Backend.Tests.csproj`
- `Java`: `src/main/java/com/optivem/shop/` → `src/main/java/com/mycompany/myshop/` (and equivalent test tree)
- `TypeScript`: `shop-api-driver.ts` → `my-shop-api-driver.ts` and any other `shop-*.ts` / `Shop*.ts`
- `TypeScript`: `src/shop/` subdirectories (camelCase convention) → `src/myShop/`
- Any other on-disk `Shop*` / `shop*` paths surfaced by the audit

**Open question: is the repo itself (`optivem/shop` on GitHub) renamed?** Recommendation: **no**. Keep the GitHub repo name `optivem/shop` as the publisher-real template repo. Only rename the content inside. The template is "the shop by Optivem"; the scaffolded output is "the my-shop by my-company".

---

## Phase 3 — Validation

### 3a. Leftover scan

Run these greps; expect zero hits outside the allowlist:

```
\boptivem\b       # allowlist: optivem/actions, @optivem/, optivem-testing, LICENSE, authors
\bOptivem\b       # allowlist: LICENSE, author attributions in package.json
\bshop\b          # allowlist: none expected (all should become my-shop/MyShop/etc.)
\bShop\b          # allowlist: none expected
\bSHOP\b          # allowlist: none expected (MY_SHOP replaces)
```

### 3b. Local build and test

Per `feedback_use_run_scripts` — use existing scripts, don't craft ad-hoc docker commands.

- .NET: `dotnet build` each renamed solution
- Java: `./gradlew build` each module
- TypeScript: `npm install && npm run build`
- System tests: `gh optivem test system` (full suite, not `--suite smoke`)

### 3c. Scaffold end-to-end with unchanged gh-optivem

Run `gh optivem init --dry-run` against the renamed shop with the **unchanged** `gh-optivem` binary. Expected outcome: every replacement pass in `replacements.go` no-ops because it's searching for `optivem`/`shop` literals that no longer exist in the source. The scaffolded output equals the template content verbatim — which validates that the template is self-contained and the tool's logic is now redundant (and ready for Phase 4 below).

If Phase 3c shows any leftover `optivem`/`shop` literal in the scaffolded output, Phase 1 missed a case — return to audit.

---

## Dependent follow-ups (separate plans)

### FU-1: Simplify `gh-optivem/internal/steps/replacements.go`
After Phase 3c passes:
- Delete `optivemAllowedPatterns` (lines 31-43)
- Delete `skip-when-owner-is-optivem` branches (lines 554-561)
- Replace `FindInTreeWordBoundary` with `FindInTree` (line 597)
- Collapse the 10+ ordered passes into a single loop over the 12 placeholder literals
- Update `replacements_test.go` fixtures

### FU-2: Update `courses/` content
- Replace lesson references from `Shop` / `ShopDriver` / `optivem/shop` to `MyShop` / `MyShopDriver` / `my-company/my-shop`
- Update `courses/docs/rules/00-shared.md` example-system declaration
- Must land close in time to this plan's merge so course content and repo state stay consistent

---

## Decisions required before execution

1. **Course content update acceptable?** (Decision A vs. B above.)
2. **Publisher domain `optivem.com`** — rewrite to `my-company.example` in template contexts only, keep elsewhere? Recommendation: yes, follow the "template vs. publisher" rule.
3. **Single PR or multi-PR?** Recommendation: single PR per phase (Phase 1 and 2 together; Phase 3 is validation on the same branch). Mid-rename state is broken — don't split.
4. **Keep `optivem/shop` GitHub repo name?** Recommendation: yes.
