# Environments

Every cloud environment runs on Cloud Run in the project that `setup-gcp.sh` creates. Each is a set of
permanent services with stable names. A run deploys a new revision of the same services and never deletes
them, so an environment's URLs stay the same between runs. Services scale to zero (min-instances 0): an
environment that has been idle answers its first request after a cold start, and costs nothing while idle.

| Environment | Used by | Stack | Reference-variant URL (multitier-typescript) |
|---|---|---|---|
| Acceptance | acceptance suites in the cloud acceptance stage | `stub`: system + stubs | `https://shop-acc.optivem.com` (custom domain, opt-in) |
| E2E | e2e suites in the cloud acceptance stage | `real`: system + simulators | `https://shop-e2e.optivem.com` (custom domain, opt-in) |
| QA | QA stage and QA sign-off | system + simulators | `https://shop-qa.optivem.com` (custom domain, opt-in) |
| Production | production stage | system + simulators | `https://shop.optivem.com` (custom domain, opt-in) |

Service names, with `<system>` from the `SYSTEM_NAME` repo variable and `<flavor>` as `<architecture>-<language>`:

| Environment | Monolith | Multitier |
|---|---|---|
| Acceptance | `<system>-<flavor>-stub` | `<system>-<flavor>-stub-frontend`, `-stub-backend` |
| E2E | `<system>-<flavor>-real` | `<system>-<flavor>-real-frontend`, `-real-backend` |
| QA | `<system>-<flavor>-qa` | `<system>-<flavor>-qa-frontend`, `-qa-backend` |
| Production | `<system>-<flavor>-production` | `<system>-<flavor>-production-frontend`, `-production-backend` |

Each has the `run.app` URL `https://<service>-<project-number>.<region>.run.app`. All of them share one
Keycloak (`<system>-keycloak`), with a realm per stage.

## Custom domains

Only the reference variant (multitier-typescript) gets custom domains. Only its frontend is mapped; the
backend stays on `run.app`, and the frontend proxies `/api` to it. The other five flavors, and repos
scaffolded from this one, stay on `run.app` until you opt in. The one exception is the shared Keycloak,
which has its own domain for every flavor (see [Keycloak](#keycloak) below).

The domains are opt-in through five repo variables, which `setup-custom-domains.sh` sets:

| Variable | Example | Maps |
|---|---|---|
| `ACC_DOMAIN` | `shop-acc.optivem.com` | `<system>-multitier-typescript-stub-frontend` |
| `E2E_DOMAIN` | `shop-e2e.optivem.com` | `<system>-multitier-typescript-real-frontend` |
| `QA_DOMAIN` | `shop-qa.optivem.com` | `<system>-multitier-typescript-qa-frontend` |
| `PROD_DOMAIN` | `shop.optivem.com` | `<system>-multitier-typescript-production-frontend` |
| `KEYCLOAK_DOMAIN` | `shop-auth.optivem.com` | `<system>-keycloak` (all flavors and stages; see [Keycloak](#keycloak)) |

If a variable is set, the stage workflow passes it on: `multitier-typescript-acceptance-stage.yml` (acc, E2E) to
`_acceptance-stage.yml`, and `multitier-typescript-qa-stage.yml` / `multitier-typescript-prod-stage.yml` (QA, production)
through `_qa-stage.yml` / `_prod-stage.yml` to `_deploy-stage.yml`. That stage:

- adds the domain to the backend's `ALLOWED_ORIGINS` and, through the Keycloak admin API, to the
  `shop-frontend` client's redirect URIs, post-logout redirect URIs and web origins (add-only, in the
  stage's realm: `shop`, `shop-qa` or `shop-prod`),
- waits for `https://<domain>/` after the deploy, which checks TLS and the mapping,
- points the UI tests (acceptance) or smoke tests (QA, production) at `https://<domain>`.

If a variable is unset, the stage behaves exactly as it did before: everything stays on `run.app`.

### Keycloak

`KEYCLOAK_DOMAIN` belongs to the one Keycloak that every flavor and stage shares, so `_acceptance-stage.yml`
and `_deploy-stage.yml` read it directly (no caller passes it). When it is set:

- `deploy-keycloak` sets Keycloak's public hostname (`KC_HOSTNAME`) to `https://<domain>`, so every realm
  (`shop`, `shop-qa`, `shop-prod`) issues tokens with `iss` = `https://<domain>/realms/<realm>` and serves
  its login pages there, and waits for the realm's discovery document on the domain,
- every backend gets `AUTH_ISSUER_URI` / `AUTH_JWK_SET_URI` (and the monolith `AUTH_TOKEN_URI`) on the
  domain, every multitier frontend `KEYCLOAK_URL`, and the acceptance harness `KEYCLOAK_URL[_REAL|_STUB]`,
- the admin API calls in `deploy-keycloak` (shop-prod client reconcile, frontend origins) stay on
  `run.app`, so they work whether or not the mapping serves yet.

Unset, Keycloak's hostname and every URL stay on `run.app`, exactly as before.

**Cutover.** Setting the variable changes the token issuer on the next Keycloak deploy, which is one
coordinated switch with a known cost: existing sessions and tokens stop working, so users log in again
once, and every backend not yet redeployed rejects the new tokens until it is. Redeploy the acceptance
stage of all six flavors and the QA stages straight away; a production deploy picks up the new issuer
with its next release (until then, login on that flavor's production fails). Deleting the variable
switches back the same way.

### One-time setup (domain owner)

The workflow doesn't create the mapping, because Cloud Run only maps a domain for a verified owner of
it, and the CI deployer service account isn't one. You create the mapping once with your own gcloud
account, and it stays in place across redeploys.

1. **Verify the domain.** In [Search Console](https://search.google.com/search-console), verify the parent
   domain (e.g. `optivem.com`) for the Google account that gcloud uses (a TXT record at your DNS provider).
   Check it with `gcloud domains list-user-verified --project=<project>`.
2. **Add the DNS records.** Add a CNAME for each subdomain, pointing to `ghs.googlehosted.com`
   (e.g. `shop-acc`, `shop-e2e`, `shop-qa`, `shop` and `shop-auth` for Keycloak).
3. **Deploy once.** Run the acceptance, QA and production stages of multitier-typescript once, so the services to map exist.
4. **Map and set the variables:**

   ```bash
   ACC_DOMAIN=shop-acc.optivem.com E2E_DOMAIN=shop-e2e.optivem.com \
   QA_DOMAIN=shop-qa.optivem.com PROD_DOMAIN=shop.optivem.com \
   KEYCLOAK_DOMAIN=shop-auth.optivem.com ./setup-custom-domains.sh
   ```

   The script creates the domain mappings (it needs the gcloud `beta` component). It sets each
   variable only once that domain's mapping is ready and its certificate is issued, usually 15 to 60
   minutes after the CNAME resolves. Re-run the script until it reports all of them as ready. `setup-gcp.sh`
   runs it too when any of the five variables is set.

To opt out, delete the variables (e.g. `gh variable delete QA_DOMAIN`).
The mappings can stay; the `run.app` URLs keep working. `teardown-gcp.sh` removes the variables, and
the mappings go with the project.
