# Backlog (consolidated 2026-10-29)

Priority order. Work top-down; each folder is one theme.

| Folder | Theme | Notes |
|---|---|---|
| `p1-ci-pipeline/` | Pipeline correctness | Pinned-sha gate fix is missing from all `-cloud` and `-legacy` acceptance-stage workflows. QA/Production stages run no smoke tests. |
| `p2-product-correctness/` | Production-grade reference | Production-readiness trio, cross-language parity, Keycloak follow-ups. Money-on-the-wire and string-only-money are one theme: do together. Tax-country is tied to a paid article. |
| `p3-test-parity/` | Test-architecture parity | Decide first whether to pursue at all; if not, delete the folder. Cross-language mirrors are gated on Java settling. `ErpStubParityContractTest` keep-or-drop belongs to the option-C discussion in `legacy-twin-stub-fidelity-gap`. |
| `after-cloud-deploy/` | Unblocked: the cloud deploy plan finished on 2026-10-31 | Pinned-sha gate (also needs `-legacy` workflows), QA/Production smoke tests, test-order alignment, production-readiness gaps (its step 6.1 Terraform may duplicate the finished cloud deploy work: drop it if so). Resume P1/P2 from here afterwards. |
| `p4-maintenance/` | Low urgency | Frontend major upgrades, SonarLint local fix. |
| `parked/` | Probably delete | Speculative, cosmetic, or belongs elsewhere (articles repo). |
