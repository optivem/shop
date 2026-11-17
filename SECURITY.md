# Security policy

MyShop is a teaching reference, not a hosted product: the cloud environments run test data only. Reports are still welcome, because students copy this code.

## Reporting a vulnerability

Please do not open a public issue or pull request for a vulnerability. Use GitHub's private vulnerability reporting: the repository's **Security** tab, then **Report a vulnerability**. Include the affected path (for example `system/monolith/java`), the steps to reproduce, and the impact you expect.

You can expect a first reply within a week. Fixes land on `main`; there are no maintained release branches, so only the latest `main` is supported.

## What is already in place

- **Secret scanning and push protection** are on for this repository (since 2026-11-17): a push that contains a recognised secret is blocked.
- **Dependabot** version updates (`.github/dependabot.yml`), **CodeQL** (`.github/workflows/codeql.yml`), and **Trivy** image scanning plus SBOMs (`.github/workflows/supply-chain.yml`, `release-sbom.yml`).
- Production start-up guards: under the prod profile every backend refuses to start with a test-default secret, a localhost issuer, or the stub clock.
- Security headers (CSP, HSTS, `nosniff`, `X-Frame-Options`, Referrer-Policy) on every backend, and Keycloak brute-force detection on every realm. See [edge rate limiting](docs/operations/edge-rate-limiting.md) for what is deliberately not covered.

## Test credentials in this repository

The committed passwords and secrets (Keycloak test users such as `customer1`/`admin1`, the `shop-monolith-test-secret` client secret, the database password `app`) belong to the local and pipeline test stacks only. They are not vulnerabilities. The production realm has no test users, and production secrets live in GCP Secret Manager.
