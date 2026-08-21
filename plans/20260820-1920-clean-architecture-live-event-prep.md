# 2026-08-20 19:20:18 UTC — Clean Architecture live-event demo prep (backend-clean-java)

## TL;DR

**Why:** `system/multitier/backend-clean-java` is the code demo for the paid live event *Clean
Architecture: Stop doing it wrong* (2026-08-26, 90 min). The theme-3 visibility fix landed in
`01035603` without its Docker-backed suite ever being run.

**End result:** The committed theme-3 fix is verified green. Nothing in
`backend-clean-java/README.md` that might go on screen states a false measurement.

## Outcomes

- **The theme-3 visibility fix is verified.** `componentTest` (62 tests, needs Docker) green against
  the changed `BaseComponentTest` wiring.
- **`backend-clean-java/README.md` states nothing false.** ✅ Done — the "Shared vs duplicated"
  section no longer carries the stale file counts.

## Decisions taken 2026-08-21 (do not re-open)

- **No repo artefacts for themes 1 and 3.** `theme2-measurements.md` earns its place because its
  numbers cannot be re-derived on stage and because `language-equivalents.md` cites it as evidence
  (lines 89, 207). Themes 1 and 3 have neither property — they are demonstrated by opening files.
  Steps to write `theme1-*.md` / `theme3-*.md` are dropped.
- **The demo running order lives with the event assets, not in this repo.** It is presentation
  material. Dropped from this plan.
- **README fork-measurement: cut, not re-measured.** The durable claim ("zero assertion changes,
  total structural divergence") is kept; the precise file counts are gone, because they went stale
  twice already.
- **`backend-dotnet` / `backend-typescript` need no change.** They are before-pictures like
  `backend-java`, which keeps its public wire DTOs deliberately. Narrowing them would destroy the
  contrast the event depends on.

## ▶ Next executable step (resume here)

**Ask, then run `./gradlew componentTest` in `system/multitier/backend-clean-java`.**

Expect 62 passing. Needs Docker (Postgres via Testcontainers). It is the only gate never run against
the theme-3 visibility fix committed in `01035603` — that commit made the `HttpErpGateway` /
`HttpTaxGateway` fetch methods private, made the four wire response types package-private, and
rewired `BaseComponentTest` to autowire `ErpGateway` / `TaxGateway`. `componentTest` is the suite
that exercises that wiring. Unit tests (97/97) and `integrationTest --tests '*GatewayIntegrationTest'`
(27/27) already passed.

**Ask before starting it** — the user does not want local Docker-backed suites self-initiated. If it
fails, fix before the event. If green, this plan is done and the file can be deleted.

## Steps

- [ ] Step 1: Ask, then run `./gradlew componentTest` in `system/multitier/backend-clean-java`.
      Expect 62 passing. If anything fails, fix before the event.
