# 2026-11-20 17:30 UTC — Visitors can sign up (UC-013 Create Customer)

## TL;DR

**Why:** The substack series now names two shop use cases that the shop didn't document: **UC-013 Create Customer** (actor: Visitor) and **UC-014 Create Admin** (actor: Admin). A visitor couldn't sign up, and Place Order's Quick order needs a country from the customer's profile.
**End result:** A visitor can sign up from the frontend, gets the `CUSTOMER` role and a profile country from the sign-up, and can place an order straight away. UC-013 and UC-014 are in the use case README, and UC-013 has acceptance tests.
**Unchanged:** the app's role checks (`SecurityConfig` ADMIN/CUSTOMER), how admins are created, and Place Order's country source.

## ▶ Next executable step (resume here)

Items 1–5 are done (U27, 2026-11-25): use case docs (UC-013 implemented, UC-014 planned); realm (registration on; prod off and guarded by `prod-realm-check.jq`; `country` required on the registration form; Verify Profile off; `CUSTOMER` in the default role; `country` claim on both login clients; `deploy-keycloak` reconciles the existing cloud `shop` and `shop-qa` realms with `reconcile-sign-up.sh`); every backend prefills the profile country from the `country` claim; every frontend has a "Sign up" link (`prompt=create`); the test kits take the default role away from each admin they create; `CreateCustomerPositiveTest` (UC-013.1, UC-013.2, UI only) passes locally in Java, .NET and TypeScript on all six stacks; `./compile-all.sh` and `--sample` are green on all six stacks. Resume at item 6: CI.

## Items

6. **Verify in CI:** the next acceptance-stage runs (all six variants) must be green. The first run also runs `reconcile-sign-up.sh` against the cloud `shop` realm, and the next QA stage runs it against `shop-qa`: check that both steps log "Sign-up is on". Then the full `test-all` run. Delete this plan when that is done.
