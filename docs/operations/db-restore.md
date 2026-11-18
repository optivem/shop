# Runbook: restore the production database (Neon)

**$0 only.** Backups are Neon's own history (point-in-time restore through branches) on the free plan, plus an optional `pg_dump` you keep yourself. No paid backup service.

## What exists

- One Neon project per system, named after it (`terraform/neon.sh`, created by `setup-gcp.sh`), with one database per stack and stage: `<system>_<architecture>_<language>_production` (dashes become underscores), plus Keycloak's.
- Neon keeps the write-ahead history of the project's branches for the plan's restore window (Neon console → project **Settings → Instant restore** shows it; the free plan's window is short, hours rather than days). Inside that window any point in time can be restored. Older than that, only a `pg_dump` you took helps.
- The connection details live in Secret Manager (`<system>-db`, JSON with `host`, `port`, `user`, `password`), which is what the deploys read.

## Restore (a bad migration, deleted or corrupted data)

1. Stop writes: roll the app back or scale it down first ([rollback](rollback.md)), so nothing writes while you restore.
2. Pick the point in time just before the damage (UTC, from the run log or the app's JSON logs).
3. Make a branch at that point and check it, without touching the live branch:

   ```bash
   source terraform/neon.sh && neon_resolve_org
   PROJECT_ID=$(neon_find_project <system>)
   neon_cli branches create --project-id "$PROJECT_ID" --name restore-check --parent 2026-11-18T10:00:00Z   # a timestamp on the default branch
   neon_cli connection-string restore-check --project-id "$PROJECT_ID" --role-name neondb_owner --database-name <database>
   psql "<that connection string>" -c 'select count(*) from orders;'
   ```

4. When the branch looks right, restore the default branch to that point: in the console, **Branches → <default branch> → Restore** (point in time), or `neon_cli branches restore <default branch> "^self@<timestamp>" --project-id "$PROJECT_ID"`. Neon keeps a backup branch of the state it replaced, so the restore itself can be undone.
5. Bring the app back ([deploy](deploy.md) or `update-traffic`), check `/readyz` and the order list.
6. Delete the `restore-check` branch (`neon_cli branches delete restore-check --project-id "$PROJECT_ID"`). Branches count against the free plan's limits.

The connection host stays the same after a restore, so Secret Manager and the services need no change.

## Logical backup (optional, outside the restore window)

```bash
pg_dump "<connection string>" --format=custom --file=shop-production-$(date -u +%Y%m%d).dump
pg_restore --clean --if-exists --no-owner --dbname "<connection string>" shop-production-<date>.dump
```

Keep the file off the repository: production data is not committed.

## Restore drill

Do it once per quarter, and after any change to the Neon setup. It only creates and deletes a branch, so it costs nothing and does not touch production data.

1. Steps 3 and 6 above with a point in time from an hour ago: create `restore-check`, connect, run `select count(*) from orders;` and `select max(version) from flyway_schema_history;` (the schema version must match the release running in production), delete the branch.
2. Write down the date, the restore point, the counts and how long it took (the recovery time). If the restore window was too short for the point you wanted, that is the finding: take a `pg_dump` on a schedule you can keep.
