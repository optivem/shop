# UI Acceptance Test Performance Optimization

## Problem

The acceptance stage takes ~19 minutes, with ~15 minutes (79%) spent on UI test steps alone.

**Baseline (monolith-java-acceptance-stage, 2026-04-08):**

| Step                                  | Duration  |
|---------------------------------------|-----------|
| Setup + Deploy + Install              | ~1.5 min  |
| Smoke Tests (Stub + Real)             | ~25s      |
| Acceptance Tests - API Channel        | ~17s      |
| **Acceptance Tests - UI Channel**     | **~8 min 13s** |
| Acceptance Tests (Isolated) - API     | ~9s       |
| **Acceptance Tests (Isolated) - UI**  | **~6 min 52s** |
| Contract + E2E + Promote              | ~1 min    |
| **Total**                             | **~19 min** |

API tests for the same test cases complete in seconds. The bottleneck is entirely in the UI channel.

## Root Causes

1. **Browser overhead per test** - Each UI test launches Chromium, creates a context, navigates multiple pages (Home -> Form -> Submit -> Verify). A single place-order UI test performs 5-8 Playwright operations with 30s timeout caps.

2. **Limited parallelism** - Java uses `maxParallelForks = availableProcessors() / 2` (= 2 on a 4-vCPU runner). TypeScript uses `maxWorkers: 1` (zero parallelism).

3. **Uneven test distribution** - The largest test classes (`PlaceOrderPositiveTest` = 16 tests, `PlaceOrderNegativeTest` = 14 tests) dominate runtime. With 2 forks, one fork may get both heavy classes while the other idles.

4. **Sequential isolated tests** - Isolated tests run with `maxParallelForks = 1` / `--runInBand` by design. The 6 isolated Java tests (14 TypeScript) run one at a time, each with full browser lifecycle.

## Test Suite Size

### Java Acceptance Tests (55 total)

| Class                            | Tests | Isolated |
|----------------------------------|-------|----------|
| PlaceOrderPositiveTest           | 16    | No       |
| PlaceOrderNegativeTest           | 14    | No       |
| PublishCouponNegativeTest        | 6     | No       |
| PublishCouponPositiveTest        | 4     | No       |
| CancelOrderNegativeTest          | 3     | No       |
| BrowseCouponsPositiveTest        | 2     | No       |
| CancelOrderPositiveTest          | 1     | No       |
| ViewOrderPositiveTest            | 1     | No       |
| ViewOrderNegativeTest            | 1     | No       |
| PlaceOrderPositiveIsolatedTest   | 3     | Yes      |
| PlaceOrderNegativeIsolatedTest   | 2     | Yes      |
| CancelOrderPositiveIsolatedTest  | 1     | Yes      |
| CancelOrderNegativeIsolatedTest  | 1     | Yes      |
| **Non-isolated: 49, Isolated: 6** |     |          |

### TypeScript Acceptance Tests (63 total)

| File                                        | Tests | Isolated |
|---------------------------------------------|-------|----------|
| place-order-positive-test.spec.ts           | 15    | No       |
| place-order-negative-test.spec.ts           | 11    | No       |
| publish-coupon-negative-test.spec.ts        | 8     | No       |
| cancel-order-negative-test.spec.ts          | 5     | No       |
| publish-coupon-positive-test.spec.ts        | 3     | No       |
| view-order-negative-test.spec.ts            | 3     | No       |
| browse-coupons-positive-test.spec.ts        | 2     | No       |
| cancel-order-positive-test.spec.ts          | 1     | No       |
| view-order-positive-test.spec.ts            | 1     | No       |
| cancel-order-negative-isolated-test.spec.ts | 5     | Yes      |
| cancel-order-positive-isolated-test.spec.ts | 4     | Yes      |
| place-order-positive-isolated-test.spec.ts  | 3     | Yes      |
| place-order-negative-isolated-test.spec.ts  | 2     | Yes      |
| **Non-isolated: 51, Isolated: 12**          |       |          |

---

## Optimization Strategies

### Strategy 1: Increase Test Worker/Fork Count

**Impact: Medium (~8 min -> ~5 min for non-isolated UI)**
**Risk: Medium (memory pressure)**
**Effort: Low**

Currently Java uses `maxParallelForks = availableProcessors() / 2` and TypeScript uses `maxWorkers: 1`.

- **Java**: Could increase to `availableProcessors()` (4 forks). Risk: each fork runs a JVM + Chromium browser (~300-500MB each). Four of them + Docker containers could cause OOM on the 7GB GitHub Actions runner.
- **TypeScript**: Override `maxWorkers` to 2 in the workflow command. Currently zero parallelism, so even 2 workers would be a significant improvement with lower risk than 4.

**Safeguard**: Monitor for flaky timeout failures after increasing. If OOM occurs, fall back to current values.

### Strategy 2: CI Sharding (Split Across Multiple Jobs)

**Impact: High (~15 min -> ~5-7 min for all UI tests)**
**Risk: Low**
**Effort: Medium**

Split UI test execution across multiple parallel GitHub Actions jobs. Each job gets a subset of tests.

- **Java (Gradle)**: No built-in shard flag. Requires manually splitting test classes across jobs using `-DincludeTestsMatching` patterns, or using a Gradle plugin like `test-distribution`.
- **TypeScript (Jest)**: Built-in `--shard=1/3` flag (Jest 28+). Easy to implement.

This is the single highest-impact optimization because it adds CPU and memory linearly (each job gets its own runner) without the OOM risk of more forks on a single runner.

**Trade-off**: More CI complexity. Need to aggregate test results across jobs. Increases total CI compute cost (more runner-minutes).

### Strategy 3: Rebalance Test Classes

**Impact: Medium (improves fork utilization)**
**Risk: Low**
**Effort: Medium**

The two heaviest classes (`PlaceOrderPositiveTest` = 16, `PlaceOrderNegativeTest` = 14) contain 30 of 49 non-isolated tests (61%). With 2 forks, if both land on the same fork, one fork does 61% of the work while the other finishes early.

Options:
- Split large test classes into smaller ones (e.g., `PlaceOrderPositiveBasicTest`, `PlaceOrderPositiveCouponTest`)
- Use Gradle's `test-distribution` or Jest's `--shard` to distribute by estimated duration rather than by class

**Trade-off**: Splitting classes for performance reasons adds organizational complexity and may conflict with the domain grouping.

### Strategy 4: API Setup + UI Verify Pattern

**Impact: High (could halve per-test time)**
**Risk: Low**
**Effort: High**

Many UI tests follow the pattern: navigate -> fill form -> submit -> verify result. The navigation and form-filling portion is slow but isn't testing anything unique to the UI - it's just setting up state.

Alternative: Use the API to create test data (place orders, publish coupons), then use the UI only for the verification step. For example, a "view order" test could create the order via API, then only use Playwright to navigate to the order details page and verify display.

This would reduce UI operations per test from 5-8 steps to 1-3 steps.

**Trade-off**: This changes what the test verifies. A "place order via UI" test that uses the API for setup is no longer testing the full UI flow. This strategy works best for tests where the core assertion is about display, not about form submission.

### Strategy 5: Shared Browser Instance

**Impact: Low-Medium (saves ~1-2s per test file)**
**Risk: Medium (test isolation)**
**Effort: Medium**

Currently each test file calls `chromium.launch()` in its `beforeAll`. With 9 non-isolated test files, that's 9 browser launches.

Alternative: Use a global setup to launch one browser instance shared across all non-isolated test files. Each test file creates its own `BrowserContext` (which is lightweight) instead of a new browser.

- **Java**: Use a JUnit `@BeforeAll` in a shared base class or a custom Extension
- **TypeScript**: Use Jest's `globalSetup` to launch browser and store the WebSocket endpoint

**Trade-off**: Shared browser means a crash in one test could affect others. Browser contexts provide isolation for cookies/storage but share the browser process.

### Strategy 6: Reduce Playwright Timeouts

**Impact: Low (only helps when things are fast)**
**Risk: Medium (false failures on slow CI)**
**Effort: Low**

The current timeout is 30s per Playwright operation. On a healthy system, operations complete in 1-3s, so the timeout doesn't affect happy-path runtime. However, if a test is going to fail, it wastes up to 30s waiting before timing out.

Reducing to 10-15s would make failing tests fail faster, but risks false failures on a loaded CI runner.

**Not recommended** as a primary strategy - the timeout only affects failing tests.

### Strategy 7: Cache Playwright Browser Binaries

**Impact: Low (~30s savings)**
**Risk: Low**
**Effort: Low**

The `npx playwright install --with-deps chromium` step takes ~30s on every run. Caching the browser binary across runs would eliminate this.

```yaml
- uses: actions/cache@v4
  with:
    path: ~/.cache/ms-playwright
    key: playwright-${{ hashFiles('**/package-lock.json') }}
```

Small absolute savings but essentially free to implement.

### Strategy 8: Larger CI Runners

**Impact: High (proportional to runner size)**
**Risk: Low**
**Effort: Low (if budget allows)**

GitHub Actions offers larger runners (8, 16, 32, 64 vCPUs) on paid plans. A larger runner allows more parallel forks without memory pressure, and each individual test runs faster due to more CPU.

An 8-vCPU runner with `maxParallelForks = 4` would likely cut the non-isolated UI time in half with no code changes.

**Trade-off**: Direct cost increase. Larger runners are 2-4x more expensive per minute.

### Strategy 9: Run UI Tests Only for Changed Features

**Impact: High (skip most UI tests on most runs)**
**Risk: Medium (missed regressions)**
**Effort: High**

If the change only affects order placement, skip coupon and cancel-order UI tests. Requires mapping code changes to test categories and maintaining that mapping.

**Not recommended** for acceptance stage - the whole point is to run the full suite. Better suited for a developer-feedback CI stage.

### Strategy 10: Reduce Isolated Test Count

**Impact: Medium (~7 min isolated UI -> less)**
**Risk: Low**
**Effort: Medium**

Review whether all isolated tests truly need isolation. A test needs isolation only if it modifies shared state (e.g., WireMock stubs) that would affect other tests. If some isolated tests are marked conservatively, moving them to non-isolated would let them run in parallel.

---

## Recommended Priority

| Priority | Strategy | Impact | Risk | Effort |
|----------|----------|--------|------|--------|
| 1 | TypeScript: add `--maxWorkers=2` | Medium | Low | Low |
| 2 | Cache Playwright binaries | Low | Low | Low |
| 3 | CI sharding (TypeScript first) | High | Low | Medium |
| 4 | Shared browser instance | Medium | Medium | Medium |
| 5 | Rebalance test classes | Medium | Low | Medium |
| 6 | API setup + UI verify | High | Low | High |
| 7 | Java: increase maxParallelForks | Medium | Medium | Low |
| 8 | Larger CI runners | High | Low | Low (cost) |
| 9 | Reduce isolated test count | Medium | Low | Medium |
| 10 | Selective test execution | High | Medium | High |
