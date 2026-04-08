# Migration 6: Convert parameterized tests to API-only with `alsoFirstRow: UI`

## Context

Parameterized tests currently run every data row on both UI and API channels, doubling execution time. UI testing one row is sufficient to verify the UI path; remaining rows only need API (faster).

## Approach

Added `alsoFirstRow` to the `@Channel` annotation in optivem-testing. This works with ALL data source types (`@DataSource`, `@ValueSource`, `@ArgumentsSource`, `@MethodSource`, etc.) without needing to convert annotations.

For each parameterized test with `@Channel({ChannelType.UI, ChannelType.API})`:
- Change to `@Channel(value = {ChannelType.API}, alsoFirstRow = ChannelType.UI)`
- No other changes needed — data source annotations stay as they are

## Changes

### optivem-testing (framework)
- `Channel.java` — added `alsoFirstRow` property
- `ChannelExtension.java` — added logic to apply extra channels only to the first data row

### starter (test files)
- `latest/acceptance/PlaceOrderPositiveTest.java` — 3 methods
- `latest/acceptance/PlaceOrderNegativeTest.java` — 4 methods
- `latest/acceptance/ViewOrderNegativeTest.java` — 1 method
- `latest/acceptance/PublishCouponNegativeTest.java` — 3 methods
- `latest/acceptance/CancelOrderNegativeIsolatedTest.java` — 1 method
- `latest/acceptance/CancelOrderPositiveIsolatedTest.java` — 1 method
- `legacy/mod10/acceptance/PlaceOrderNegativeTest.java` — 4 methods

## Status

- [x] optivem-testing: `@Channel.alsoFirstRow` added
- [x] optivem-testing: `ChannelExtension` updated
- [x] starter: all 7 Java test files updated (17 methods total)
- [ ] optivem-testing: release new version
- [ ] starter: upgrade optivem-testing dependency
- [ ] verify via CI
