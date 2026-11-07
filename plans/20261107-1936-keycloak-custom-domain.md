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

Step 7 (user, outside the repo): Steps 1–6 are done (code in one commit: `KEYCLOAK_DOMAIN` opt-in in `_acceptance-stage.yml`, `_deploy-stage.yml`, `deploy-keycloak`, `setup-custom-domains.sh`, `setup-gcp.sh`, `teardown-gcp.sh`, `docs/environments.md`). Add a CNAME `shop-auth` → `ghs.googlehosted.com` at the DNS provider, then run `KEYCLOAK_DOMAIN=shop-auth.optivem.com ./setup-custom-domains.sh` and re-run it until it reports `shop-auth.optivem.com` ready (it sets the `KEYCLOAK_DOMAIN` repo variable only then). Setting the variable starts the cutover, so do Step 8 right after.

## Steps

- [ ] Step 7: **User-side setup.** The user adds a CNAME `shop-auth` → `ghs.googlehosted.com`, then runs `KEYCLOAK_DOMAIN=shop-auth.optivem.com ./setup-custom-domains.sh` until it reports ready.
- [ ] Step 8: **Cutover and verification** (decided 2026-11-07: accept a short window, no dual-issuer support). Once `KEYCLOAK_DOMAIN` is set, the first deploy that restarts Keycloak changes the issuer, and every backend not yet redeployed rejects tokens until it is. So redeploy straight away: the acceptance stage for all 6 flavors, then QA for each. For production, redeploy only multitier-typescript (behind `shop.optivem.com`; needs a new QA-approved RC, so sign it off). The other five flavors' production picks up the new issuer on its next normal release. Until then, login on their `run.app` URLs fails, which is acceptable because they have no custom domain. Verify: acceptance green for all 6 flavors, and a real browser login on `shop.optivem.com` lands on `shop-auth.optivem.com` and returns signed in.

