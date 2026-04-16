# System Test Comparison — Action Plan (2026-04-16)

**Source:** [TEST-COMPARISON-2026-04-16.md](../reports/TEST-COMPARISON-2026-04-16.md)  
**Date:** 2026-04-16  
**Target repo:** `starter` only. Do NOT modify `eshop-tests`.

---

## Cross-Language Decisions Made

- **DIFF-L11-L15 (`.withQuantity(1)`)**: Keep in TS — more explicit is better. Eventually add to Java/.NET.
- **DIFF-L20 (`undefined` vs `null`)**: Keep TS `undefined` — idiomatic TS for optional fields.
- **DIFF-G8/G9 (mod08 structure)**: Combined into one test matching Java/.NET, with all missing assertions added.
- **DIFF-L3 (`.withCouponCode(null)`)**: TS DSL uses optional param (omit = no coupon), not `null`. Kept TS approach.
- **DIFF-G16 (clock `.withTime()`)**: Already matches Java — no change needed.

---

## Remaining (blocked)

- [ ] (DIFF-L23): **ViewOrderNegative** — TS UI driver cannot navigate to a non-existent order (tries to find table row, times out). Java UI driver handles this via direct URL navigation. Fix: update TS `viewOrder` UI driver to support direct URL navigation by order number, then add `forChannels('ui', 'api')` for first test row.

---

## Investigations

- [ ] **DSL: skip `.given()` when no setup needed** — Investigate whether `scenario.then()` (without `.given()`) is possible across all three DSL implementations. Would simplify contract tests like `shouldBeAbleToGetTime` which currently use `.given().then()` with no actual setup.

---

## Structural Observations (No Action)

- **DIFF-G1, G2, G4**: TS legacy mod03/mod04 uses unified channel-aware tests while Java/.NET use separate Api/Ui classes. TS is structurally ahead.
- **DIFF-G6**: Java/.NET mod07 use use-case DSL while TS uses scenario DSL. TS is at a later DSL stage.
- **DIFF-G12, G13**: mod10 isolated tests are consistent across all languages.
