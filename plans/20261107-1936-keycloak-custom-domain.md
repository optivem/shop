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

Step 8 remainder (user sign-off, then production). Done on 2026-11-07: `KEYCLOAK_DOMAIN=shop-auth.optivem.com` is set; acceptance is green for all 6 flavors and QA green for 4 (monolith-dotnet and multitier-dotnet QA were still running at hand-off: runs 42415102639 and 42415234619, check them) (after `0dea9564` capped Keycloak at one Cloud Run instance; two instances broke in-flight logins with `expired_code`). Left: sign off the multitier-typescript RC `multitier-typescript-v1.0.238-rc.3402` (`multitier-typescript-qa-signoff.yml`), run `multitier-typescript-prod-stage.yml`, then log in through a real browser on `https://shop.optivem.com` and check it goes to `shop-auth.optivem.com` and comes back signed in. The other five flavors' production picks up the new issuer on their next normal release.

## Steps

- [ ] Step 8 (remainder): **Production cutover for multitier-typescript and browser check.** Sign off `multitier-typescript-v1.0.238-rc.3402` in QA (`multitier-typescript-qa-signoff.yml`), run `multitier-typescript-prod-stage.yml`, then do a real browser login on `shop.optivem.com`: it should land on `shop-auth.optivem.com` and return signed in. Until then, production login on `shop.optivem.com` is broken (old issuer), so do this soon. Other flavors' production: next normal release (accepted).
