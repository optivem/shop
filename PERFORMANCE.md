# UI Acceptance Test Performance Optimization

## Problem

The acceptance stage for Java takes ~19 minutes, with ~15 minutes (79%) spent on UI test steps alone.

### Cross-Language Comparison (2026-04-08)

All six acceptance stage workflows run the same test cases against the same application. The only difference is the language of the test runner.

| Workflow                      | Acceptance UI | Isolated UI | Run Job Total |
|-------------------------------|---------------|-------------|---------------|
| **monolith-java**             | **8 min 14s** | **6 min 53s** | **19 min 12s** |
| **multitier-java**            | **8 min 13s** | **6 min 52s** | **18 min 44s** |
| monolith-dotnet               | 45s           | 11s         | 5 min 43s     |
| multitier-dotnet              | 45s           | 11s         | 5 min 3s      |
| monolith-typescript           | 17s           | 10s         | 3 min 28s     |
| multitier-typescript          | 15s           | 10s         | 2 min 51s     |

**Java UI tests are 30-60x slower than TypeScript and 10-20x slower than .NET for the identical test scenarios.**

TypeScript completes all UI acceptance tests (non-isolated + isolated) in ~27 seconds. .NET completes them in ~56 seconds. Java takes ~15 minutes.

The monolith vs multitier architecture makes no meaningful difference within a language - the bottleneck is the test runner, not the system under test.

### Java Step-Level Breakdown

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

API tests for the same Java test cases complete in seconds. The bottleneck is entirely in the UI channel.

## Industry Benchmark

Dave Farley recommends that a full acceptance stage should complete within 1 hour, even for a real-life production project with hundreds of tests and complex infrastructure.

This simple starter project — with only ~55-63 acceptance tests and a straightforward e-commerce domain — already consumes 20 minutes for Java. That's a third of the entire budget before any real-world complexity is added (more features, more test scenarios, more external system integrations, more browsers, more environments).

If this scales linearly, a production-sized test suite with 3-5x more tests would blow past the 1-hour ceiling without optimization. The time to address this is now, while the test suite is small enough to experiment with different strategies safely.

## Root Causes

### Primary: `slowMo: 100` in Java Playwright (FOUND)

The Java `BrowserLifecycleExtension.java` launches Chromium with `.setSlowMo(100)`, which adds a **100ms artificial delay to every single Playwright operation** — every click, fill, navigation, waitFor, and textContent call.

A single place-order UI test performs roughly 50-80 Playwright operations. That's **5-8 seconds of pure artificial delay per test**, just from slowMo. Across 55 acceptance tests, that's ~5-7 minutes of the browser doing nothing but waiting.

**TypeScript had zero slowMo. .NET latest had zero slowMo. Java latest and all legacy code (Java, .NET, TypeScript in eshop-tests) had it.**

The `slowMo` setting is useful for local debugging (watching the browser in headed mode so you can follow along), but it should never run in CI. Removing it should bring Java UI test time from ~15 minutes down to the ~1-2 minute range, close to the .NET results.

**File:** `system-test/java/src/main/java/com/optivem/shop/systemtest/infrastructure/playwright/BrowserLifecycleExtension.java` (line 133)

### Secondary: Other contributing factors

These are minor compared to slowMo but still relevant:

### Secondary: Limited parallelism

- Java uses `maxParallelForks = availableProcessors() / 2` (= 2 on a 4-vCPU runner)
- TypeScript uses `maxWorkers: 1` (zero parallelism — but tests are so fast it barely matters)
- Isolated tests run sequentially by design in all languages

### Secondary: Uneven test distribution

The largest test classes (`PlaceOrderPositiveTest` = 16 tests, `PlaceOrderNegativeTest` = 14 tests) contain 30 of 49 non-isolated tests (61%). With 2 forks, one fork may get both heavy classes while the other idles.

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

### Strategy 1: Remove `slowMo` entirely (DONE)

**Impact: Very High (expected ~15 min -> ~1-2 min for Java UI tests)**
**Risk: Low**
**Effort: Trivial**

Root cause found: `slowMo: 100` was present in multiple files across both repos. Removed from all 11 locations:

**starter repo (5 files):**
- `system-test/java/src/main/java/.../BrowserLifecycleExtension.java` (latest)
- `system-test/java/src/test/java/.../legacy/mod02/base/BaseRawTest.java`
- `system-test/java/src/test/java/.../legacy/mod03/base/BaseRawTest.java`
- `system-test/dotnet/SystemTests/Legacy/Mod02/Base/BaseRawTest.cs`
- `system-test/dotnet/SystemTests/Legacy/Mod03/Base/BaseRawTest.cs`

**eshop-tests repo (6 files):**
- `typescript/driver-adapter/shop/ui/client/ShopUiClient.ts` (latest)
- `java/system-test/src/main/java/.../BrowserLifecycleExtension.java` (latest)
- `java/system-test/src/test/java/.../legacy/mod02/base/BaseRawTest.java`
- `java/system-test/src/test/java/.../legacy/mod03/base/BaseRawTest.java`
- `dotnet/SystemTests/Legacy/Mod02/Base/BaseRawTest.cs`
- `dotnet/SystemTests/Legacy/Mod03/Base/BaseRawTest.cs`

### Strategy 2: Shared Browser Instance (Java)

**Impact: High (eliminates per-class browser launch overhead)**
**Risk: Medium (test isolation)**
**Effort: Medium**

Currently each Java test class calls `chromium.launch()` in its own `@BeforeAll`. With 9 non-isolated test classes running in 2 forks, that's multiple browser launches.

Alternative: Use a JUnit Extension or global setup to launch one browser per fork, shared across test classes. Each test class creates its own `BrowserContext` (lightweight) instead of a new browser.

### Strategy 3: Increase Test Worker/Fork Count

**Impact: Medium (only helps Java; TypeScript/C# already fast enough)**
**Risk: Medium (memory pressure)**
**Effort: Low**

- **Java**: Could increase `maxParallelForks` from `availableProcessors() / 2` to `availableProcessors()` (2 -> 4 forks). Risk: each fork runs a JVM + Chromium browser (~300-500MB each). Four of them + Docker containers could cause OOM on the 7GB GitHub Actions runner.
- **TypeScript**: Override `maxWorkers` from 1 to 2. Currently zero parallelism, but tests are so fast (~27s total) that the savings would be minimal.

**Safeguard**: Monitor for flaky timeout failures after increasing. If OOM occurs, fall back to current values.

### Strategy 4: CI Sharding (Split Across Multiple Jobs)

**Impact: High (divides wall-clock time by shard count)**
**Risk: Low**
**Effort: Medium**

Split Java UI test execution across multiple parallel GitHub Actions jobs. Each job gets its own runner with full CPU and memory.

- **Java (Gradle)**: No built-in shard flag. Requires manually splitting test classes across jobs using `-DincludeTestsMatching` patterns, or using a Gradle plugin.
- **TypeScript (Jest)**: Built-in `--shard=1/3` flag. Easy but unnecessary given tests already finish in ~27s.

This avoids the OOM risk of more forks on a single runner by adding runners instead.

**Trade-off**: More CI complexity. Need to aggregate test results across jobs. Increases total CI compute cost.

### Strategy 5: Rebalance Test Classes

**Impact: Medium (improves fork utilization for Java)**
**Risk: Low**
**Effort: Medium**

The two heaviest classes (`PlaceOrderPositiveTest` = 16, `PlaceOrderNegativeTest` = 14) contain 30 of 49 non-isolated tests (61%). With 2 forks, if both land on the same fork, one fork does 61% of the work while the other finishes early.

Options:
- Split large test classes into smaller ones
- Use Gradle's test distribution to balance by estimated duration rather than by class

**Trade-off**: Splitting classes for performance reasons adds organizational complexity.

### Strategy 6: API Setup + UI Verify Pattern

**Impact: High (could halve per-test time for Java)**
**Risk: Low**
**Effort: High**

Many UI tests follow: navigate -> fill form -> submit -> verify. The setup portion is slow but isn't testing anything unique to the UI.

Alternative: Use the API to create test data, then use the UI only for verification. A "view order" test could create the order via API, then only use Playwright to verify display.

**Trade-off**: Changes what the test verifies. Works best for tests where the assertion is about display, not form submission.

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

**Trade-off**: Direct cost increase. Only worth it for the Java workflow — TypeScript and .NET are already fast on standard runners.

### Strategy 9: Reduce Isolated Test Count

**Impact: Medium (Java isolated UI = ~7 min)**
**Risk: Low**
**Effort: Medium**

Review whether all isolated tests truly need isolation. A test needs isolation only if it modifies shared state (e.g., WireMock stubs) that would affect other tests. If some are marked conservatively, moving them to non-isolated would let them run in parallel.

### Strategy 10: Run UI Tests Only for Changed Features

**Impact: High (skip most UI tests on most runs)**
**Risk: Medium (missed regressions)**
**Effort: High**

If the change only affects order placement, skip coupon and cancel-order UI tests. Requires mapping code changes to test categories.

**Not recommended** for acceptance stage — the whole point is to run the full suite.

---

## Recommended Priority

The cross-language comparison makes the priority clear: **fix the Java-specific problem first**, then consider general optimizations.

| Priority | Strategy | Impact | Risk | Effort |
|----------|----------|--------|------|--------|
| 1 | Investigate Java Playwright overhead | Very High | Low | Medium |
| 2 | Shared browser instance (Java) | High | Medium | Medium |
| 3 | CI sharding (Java only) | High | Low | Medium |
| 4 | Increase Java maxParallelForks | Medium | Medium | Low |
| 5 | Cache Playwright binaries | Low | Low | Low |
| 6 | Rebalance test classes | Medium | Low | Medium |
| 7 | API setup + UI verify | High | Low | High |
| 8 | Larger CI runners | High | Low | Cost |
| 9 | Reduce isolated test count | Medium | Low | Medium |
| 10 | Selective test execution | High | Medium | High |

TypeScript and .NET workflows do not need optimization at this time — their total acceptance stage runtimes (3-6 minutes) are well within the 1-hour budget even at 10x scale.
