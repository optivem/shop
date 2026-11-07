# 2026-11-07 19:36:19 UTC — Custom domain for the shared Keycloak

## TL;DR

**Why:** The frontends now live on custom domains (`shop-acc`, `shop-e2e`, `shop-qa`, `shop.optivem.com`, all multitier-typescript), but login still bounces users to `https://shop-keycloak-108735428179.us-central1.run.app/...`. That raw Cloud Run URL looks untrustworthy on a sign-in page and leaks the project number.
**End result:** The one shared Keycloak answers on its own custom domain, `shop-auth.optivem.com` (decided 2026-11-07, matching the `shop-*` names). Every realm (`shop`, `shop-qa`, `shop-prod`) issues tokens with that domain as the issuer, every backend and frontend in all 6 flavors and all stages points at it, and repos without the opt-in variable keep working on `run.app` exactly as today.

## Outcomes

- Signing in on any environment shows a login page on the Keycloak custom domain, not a `run.app` URL.
- Tokens' `iss` is `https://<keycloak-domain>/realms/<realm>`, and every backend (all 6 flavors, acceptance / QA / production) accepts them.
- All frontends (multitier `KEYCLOAK_URL`, monolith OIDC login) and the acceptance test harness (`KEYCLOAK_URL[_REAL|_STUB]`) use the custom domain when it's set.
- Local Docker stacks (`docker/**`, `localhost`, their own realm imports) are untouched.
- Opt-in through one repo variable (e.g. `KEYCLOAK_DOMAIN`), set by `setup-custom-domains.sh` once the mapping is ready; unset means `run.app` everywhere, so scaffolded repos are unaffected.
- `docs/environments.md` documents it, and `teardown-gcp.sh` removes the variable.
- The cutover is one coordinated switch with a known cost: existing sessions and tokens issued under the old issuer stop working, so users log in again once.

## ▶ Next executable step (resume here)

Step 1: read `docker/keycloak/Dockerfile`, `.github/actions/deploy-keycloak/action.yml`, the `docker/keycloak/*.jq` realm files, `_acceptance-stage.yml` and `_deploy-stage.yml`, and list every place that builds the Keycloak `run.app` URL or sets Keycloak's hostname. Write the list into Step 1 here. It's read-only; Steps 2–6 build on it (code, in one commit), then Step 7 is the user's DNS and mapping step.

## Steps

- [ ] Step 1: **Map how Keycloak gets its URL today.** Read `docker/keycloak/Dockerfile` (realm placeholders `SYSTEM` / `RUN_APP_SUFFIX`), `.github/actions/deploy-keycloak/action.yml` (`KC_HOSTNAME` / host env), and the realm `.jq` files. List every place that builds `https://${SYSTEM}-keycloak-…run.app`: `_acceptance-stage.yml` (backend/monolith `AUTH_*`, `Resolve System URLs` → `KEYCLOAK_URL*`), `_deploy-stage.yml` (`KEYCLOAK_URL`, `AUTH_ISSUER_URI`, frontend env), the `deploy-keycloak` admin-API steps, and any docker/systems.yaml or test config.
- [ ] Step 2: **One URL source.** Add a `keycloak-domain` input (from `vars.KEYCLOAK_DOMAIN`) to `_acceptance-stage.yml`, `_deploy-stage.yml` and `deploy-keycloak`, and compute `KEYCLOAK_URL` once per job: `https://<domain>` if set, else the `run.app` URL. Pass it to all 6 flavors (the domain belongs to the shared Keycloak, not one flavor).
- [ ] Step 3: **Keycloak hostname.** Set Keycloak's public hostname (`KC_HOSTNAME`) to the custom domain when it's set, so the issuer and every login/redirect URL use it. The admin API calls in `deploy-keycloak` (realm reconcile, adding frontend origins) stay on `run.app` (decided 2026-11-07), so they work before the mapping is ready.
- [ ] Step 4: **Backends, frontends and harness.** Point `AUTH_ISSUER_URI`, `AUTH_JWK_SET_URI`, `AUTH_TOKEN_URI` (monolith), frontend `KEYCLOAK_URL`, and the acceptance harness `KEYCLOAK_URL*` at the computed URL. Keep the QA/production smoke tests anonymous.
- [ ] Step 5: **Mapping script.** Extend `setup-custom-domains.sh` with `KEYCLOAK_DOMAIN` → `${SYSTEM}-keycloak`, setting the variable only once the certificate is issued (same rule as the frontend domains). Add it to `setup-gcp.sh`'s trigger and to `teardown-gcp.sh`'s variable list.
- [ ] Step 6: **Docs.** Add a Keycloak row and the cutover note (one re-login) to `docs/environments.md`.
- [ ] Step 7: **User-side setup.** The user adds a CNAME `shop-auth` → `ghs.googlehosted.com`, then runs `KEYCLOAK_DOMAIN=shop-auth.optivem.com ./setup-custom-domains.sh` until it reports ready.
- [ ] Step 8: **Cutover and verification** (decided 2026-11-07: accept a short window, no dual-issuer support). Once `KEYCLOAK_DOMAIN` is set, the first deploy that restarts Keycloak changes the issuer, and every backend not yet redeployed rejects tokens until it is. So redeploy straight away: the acceptance stage for all 6 flavors, then QA for each. For production, redeploy only multitier-typescript (behind `shop.optivem.com`; needs a new QA-approved RC, so sign it off). The other five flavors' production picks up the new issuer on its next normal release. Until then, login on their `run.app` URLs fails, which is acceptable because they have no custom domain. Verify: acceptance green for all 6 flavors, and a real browser login on `shop.optivem.com` lands on `shop-auth.optivem.com` and returns signed in.

