# Seed data

Scripts here are **not migrations**. Nothing in this directory is ever applied by the `db-migrate`
Flyway sidecar: every `docker-compose*.yml` and every `gh-optivem-*.yaml` names
`system/db/migrations` explicitly, so `system/db/seed` is outside the mounted path by construction.
Keeping the two apart is the point — a seed is demo data someone loads on purpose, and it must not
be able to reach CI or production by being in the wrong folder.

| File | What it is for |
|---|---|
| [`demo-volume.sql`](demo-volume.sql) | 100k orders + 300 coupons, for measuring the theme-2 demonstrations in `system/multitier/backend-clean-java`. Deterministic and idempotent; every row it writes is prefixed `DEMO-`. |

Load one by hand against a running database:

```bash
psql "$DATABASE_URL" -v ON_ERROR_STOP=1 --single-transaction -f system/db/seed/demo-volume.sql
```

`demo-volume.sql` is also loaded automatically by the `benchmark` Gradle task in
`system/multitier/backend-clean-java`, which is how the theme-2 before/after numbers are taken.
