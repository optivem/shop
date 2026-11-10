# Contributing

How to verify changes locally and make schema changes safely.

- [Running system tests](#running-system-tests)
- [Schema changes](#schema-changes)

## Running system tests

The `gh optivem` CLI extension orchestrates the docker-compose stacks and runs
the test suites. Install it once with:

```bash
gh extension install optivem/gh-optivem
```

### All languages at once — `test-all.sh`

From the repo root:

```bash
./test-all.sh -a monolith
```

Runs both **latest** and **legacy** suites across all three languages (.NET,
Java, TypeScript) sequentially, prints a per-language / per-phase summary, and
exits non-zero on any failure.

Useful flags:

- `-l dotnet,java` — run only a subset of languages
- `-a multitier` — run against the multitier architecture instead of monolith
- `--restart` — pass `--restart` through to `gh optivem system start`, so
  changed services are rebuilt/recreated instead of reusing possibly stale
  images (opt-in; the default is a plain `system start`)

After each phase (latest, then legacy, per language) the script runs
`gh optivem system stop` with that phase's config, so stacks don't pile up and
eat memory. A trap does the same if the run is cut off (Ctrl+C / SIGTERM), and
the script's exit code is preserved.

This is the preferred entry point for verifying cross-language changes.

**Low memory / running from Claude Code.** `./test-all.sh -a multitier` can be
killed partway through by Claude Code's background-shell memory reaper (seen
twice, even starting with ~7.6 GB free; `-a monolith` finishes). Any one of
these avoids it:

- run multitier one language at a time: `./test-all.sh -a multitier -l <lang>`
- run it from a terminal outside Claude Code
- start Claude Code with `CLAUDE_CODE_DISABLE_BG_SHELL_PRESSURE_REAP=1`

### Single language — `gh optivem system-test run`

Point `GH_OPTIVEM_CONFIG` at the variant yaml at the repo root, then run the
commands without per-flag overrides. Substitute `<language>` ∈ {java, dotnet,
typescript} and `<architecture>` ∈ {monolith, multitier}:

```pwsh
$env:GH_OPTIVEM_CONFIG = "gh-optivem-<architecture>-<language>.yaml"

# Bring up the docker-compose stacks
gh optivem system start

# Prepare the test harness (npm ci / gradle compile / dotnet build, depending on language)
gh optivem system-test setup

# Run the latest suites
gh optivem system-test run

# Or a fast smoke (one sample per suite)
gh optivem system-test run --sample

# Stop when done
gh optivem system stop
```

For the legacy suites, switch the env var to the `-legacy` sibling and re-run setup (legacy has its own setupCommands block):

```pwsh
$env:GH_OPTIVEM_CONFIG = "gh-optivem-<architecture>-<language>-legacy.yaml"
gh optivem system-test setup
gh optivem system-test run
```

Use this when iterating on a single language, or for the `--sample`
pre-commit verification described in [CLAUDE.md](CLAUDE.md).

Do **not** substitute `./gradlew test`, `mvn test`, `dotnet test`, or `npm
test` — these wrappers manage Docker containers and per-suite environment
variables that the raw toolchain does not.

## Line endings

The root [`.gitattributes`](.gitattributes) is the single source of truth: LF everywhere, CRLF for `*.bat` and `*.cmd`, and binary types declared explicitly. Don't add nested `.gitattributes` files. When you add a new binary type, declare it in the root file. The `check-line-endings` workflow fails otherwise.

On Windows, clone inside WSL or the dev container rather than on the Windows filesystem, so scripts keep their LF endings and run in Linux.

`git status` can't show line-ending drift, because with `eol=lf` a CRLF file on disk counts as unmodified. Check with `git ls-files --eol` instead:

```bash
git ls-files --eol | grep -E 'w/crlf.*eol=lf|w/lf.*eol=crlf'
```

To recover a stale checkout, first commit or stash your work, because this discards local changes. Then rewrite every file:

```bash
git rm --cached -r -q . && git reset --hard
```

## Schema changes

How to change the shop database schema safely.

The shop has a single canonical schema in `system/db/migrations/`, applied to every Postgres instance by the `db-migrate` Flyway sidecar before any app starts. All six SUT implementations (3 languages × 2 architectures) are pure schema consumers — none of them create, drop, or alter tables on boot.

Every schema change is a new SQL migration file. Read this whole section before opening a PR that touches `system/db/migrations/`.

### 1. Forward-only doctrine

No `U__*.sql` undo files. The Flyway undo feature is treated as if it doesn't exist. The sidecar runs with `FLYWAY_CLEAN_DISABLED=true` so a misconfigured CI step cannot wipe a schema.

**Recovery from a bad migration is a new forward migration, never a reversal.** If `V20260601120000__add_email.sql` was wrong, the fix is `V20260601153000__drop_email.sql` or `V20260601153000__fix_email.sql`. Don't edit the original file once it has run anywhere — Flyway stores its checksum in `flyway_schema_history` and will refuse to run again with a mismatch.

Why forward-only:

- A down migration that drops a column also deletes the data the previous app version wrote there. There is no `reverse(ALTER TABLE … DROP COLUMN x)` that brings the data back.
- Forward-only forces every migration to be safe-by-construction at write time, when you still have the context, rather than relying on a hypothetical reversal step at incident time.
- Tools that offer undo (Flyway undo, EF Core `Down()`, ActiveRecord `down`) are footguns in any environment with real data. The Postgres community has been clear about this for years.

### 2. App rollback ≠ schema rollback

Apps roll back via the platform — `kubectl rollout undo`, `docker compose up` with the previous image, blue/green flip. Seconds, no data risk.

The schema does not roll back. The previous app version runs against the current schema by design.

This only works if every migration preserves the previous app version's invariants. The table below summarises the common change types.

| Change type        | Wrong (couples to one app version)             | Right (rollback-safe)                                                  |
|--------------------|------------------------------------------------|------------------------------------------------------------------------|
| Add column         | `ADD COLUMN x NOT NULL` (old code can't write) | `ADD COLUMN x DEFAULT 0` (old code keeps working)                      |
| Rename column      | `RENAME COLUMN a TO b` (old code can't find a) | Add `b` → dual-write → migrate readers → drop `a` (multi-step)         |
| Drop column        | `DROP COLUMN a` (old code still reads a)       | Stop writing → wait → stop reading → drop (multi-step)                 |
| Tighten constraint | `ALTER … SET NOT NULL` (old code writes NULL)  | Add tolerant default → backfill → tighten                              |

The right-hand column is the **expand-contract pattern**: every change is split into safe steps that each leave both the previous and the new app version functional.

### 3. Expand-contract worked examples

#### Renaming a column (`old_name` → `new_name`)

1. **Expand.** Add `new_name` alongside `old_name`. Both nullable, no constraints yet.
2. **Dual-write.** Update the app to write to both columns. Deploy.
3. **Backfill.** Migration that copies `old_name` → `new_name` for existing rows.
4. **Migrate readers.** Update the app to read from `new_name`. Deploy.
5. **Stop writing the old name.** Update the app to stop writing `old_name`. Deploy.
6. **Contract.** Drop `old_name`.

Five or six migrations across multiple deploys. Every intermediate state is rollback-safe.

#### Dropping a column

1. **Stop writing it.** Deploy.
2. **Wait.** Confirm no traffic still depends on it (logs, metrics, retention window for in-flight requests).
3. **Stop reading it.** Deploy.
4. **Contract.** Drop the column.

#### Tightening a constraint (`NULL`-able → `NOT NULL`)

1. **Add a tolerant default.** `ALTER COLUMN … SET DEFAULT …` so new writes can't insert NULL.
2. **Backfill.** Update any existing NULL rows to the default value.
3. **Contract.** `ALTER COLUMN … SET NOT NULL`.

### 4. PR checklist

Before merging a PR that touches `system/db/migrations/`:

- [ ] One new SQL file per logical migration, named `V{YYYYMMDDHHMMSS}__{description}.sql`.
- [ ] The migration preserves the previous app version's invariants (see the rollback-safety table above), or is part of a documented expand-contract sequence.
- [ ] The migration does not edit a previously released migration file (checksums are forever).
- [ ] If renaming a column or table, you are doing it in expand-contract steps and not all at once.
- [ ] Local verification: `docker compose down -v && docker compose up` from at least one of the six stack/mode pairings boots cleanly and `flyway_schema_history` shows `success=t` for the new entry.

### 5. Open work

- **Previous-version smoke test** is not yet implemented. The shop has no production deploys today, so rolling-deploy safety is theoretical. When shop adopts release tagging for deploys, add a CI job that runs the previous release's image against the current migration set. Until then, rollback-safety is enforced by review against the table above.

### References

- Humble & Farley, *Continuous Delivery*, ch. 12 ("Managing Data")
- Sadalage & Fowler, *Refactoring Databases* — full expand-contract catalogue
- Origin incident: Issue #61 acceptance retry (rehearsal-20260513), `coupons.used_count` NULL constraint
- Migration set conventions: [`system/db/migrations/README.md`](system/db/migrations/README.md)
