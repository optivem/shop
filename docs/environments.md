# Environments

Every cloud environment runs on Cloud Run in the project that `setup-gcp.sh` creates. Each is a set of
permanent services with stable names. A run deploys a new revision of the same services and never deletes
them, so an environment's URLs stay the same between runs. Services scale to zero (min-instances 0): an
environment that has been idle answers its first request after a cold start, and costs nothing while idle.

| Environment | Used by | Stack | Reference-variant URL (multitier-java) |
|---|---|---|---|
| Acceptance | acceptance suites in the cloud acceptance stage | `stub`: system + stubs | `https://shop-acc.optivem.com` (custom domain, opt-in) |
| E2E | e2e suites in the cloud acceptance stage | `real`: system + simulators | `https://shop-e2e.optivem.com` (custom domain, opt-in) |
| QA | QA stage and QA sign-off | system + simulators | `run.app` only |
| Production | production stage | system + simulators | `run.app` only |

Service names, with `<system>` from the `SYSTEM_NAME` repo variable and `<flavor>` as `<architecture>-<language>`:

| Environment | Monolith | Multitier |
|---|---|---|
| Acceptance | `<system>-<flavor>-stub` | `<system>-<flavor>-stub-frontend`, `-stub-backend` |
| E2E | `<system>-<flavor>-real` | `<system>-<flavor>-real-frontend`, `-real-backend` |
| QA | `<system>-<flavor>-qa` | `<system>-<flavor>-qa-frontend`, `-qa-backend` |
| Production | `<system>-<flavor>-production` | `<system>-<flavor>-production-frontend`, `-production-backend` |

Each has the `run.app` URL `https://<service>-<project-number>.<region>.run.app`. All of them share one
Keycloak (`<system>-keycloak`), with a realm per stage.

## Custom domains for acceptance and E2E

Only the reference variant (multitier-java) gets custom domains. Only its frontend is mapped; the
backend stays on `run.app`, and the frontend proxies `/api` to it. The other five flavors, and repos
scaffolded from this one, stay on `run.app` until you opt in.

The domains are opt-in through two repo variables, which `setup-custom-domains.sh` sets:

| Variable | Example | Maps |
|---|---|---|
| `ACC_DOMAIN` | `shop-acc.optivem.com` | `<system>-multitier-java-stub-frontend` |
| `E2E_DOMAIN` | `shop-e2e.optivem.com` | `<system>-multitier-java-real-frontend` |

If a variable is set, `multitier-java-acceptance-stage.yml` passes it to `_acceptance-stage.yml`, which:

- adds the domain to the backend's `ALLOWED_ORIGINS` and, through the Keycloak admin API, to the
  `shop-frontend` client's redirect URIs, post-logout redirect URIs and web origins (add-only, in the `shop` realm),
- waits for `https://<domain>/` after the deploy, which checks TLS and the mapping,
- points the UI tests at `https://<domain>` (`MYSHOP_UI_BASE_URL_STUB` / `_REAL` in `Resolve System URLs`).

If a variable is unset, the stage behaves exactly as it did before: everything stays on `run.app`.

### One-time setup (domain owner)

The workflow doesn't create the mapping, because Cloud Run only maps a domain for a verified owner of
it, and the CI deployer service account isn't one. You create the mapping once with your own gcloud
account, and it stays in place across redeploys.

1. **Verify the domain.** In [Search Console](https://search.google.com/search-console), verify the parent
   domain (e.g. `optivem.com`) for the Google account that gcloud uses (a TXT record at your DNS provider).
   Check it with `gcloud domains list-user-verified --project=<project>`.
2. **Add the DNS records.** Add a CNAME for each subdomain, pointing to `ghs.googlehosted.com`
   (e.g. `shop-acc` and `shop-e2e`).
3. **Deploy once.** Run `gh workflow run multitier-java-acceptance-stage.yml` once, so the services to map exist.
4. **Map and set the variables:**

   ```bash
   ACC_DOMAIN=shop-acc.optivem.com E2E_DOMAIN=shop-e2e.optivem.com ./setup-custom-domains.sh
   ```

   The script creates the domain mappings (it needs the gcloud `beta` component). It sets each
   variable only once that domain's mapping is ready and its certificate is issued, usually 15 to 60
   minutes after the CNAME resolves. Re-run the script until it reports both as ready. `setup-gcp.sh`
   runs it too when `ACC_DOMAIN` or `E2E_DOMAIN` is set.

To opt out, delete the variables (`gh variable delete ACC_DOMAIN`, `gh variable delete E2E_DOMAIN`).
The mappings can stay; the `run.app` URLs keep working. `teardown-gcp.sh` removes the variables, and
the mappings go with the project.
