# SonarCloud — Deferred Issues

**Run started:** 2026-04-27 10:56 UTC
**Repo:** shop
**Total issues at start of run:** 159
**Auto-fixed:** ~110 (across 6 SonarCloud projects)
**Deferred (this file):** 49

These items were intentionally left alone by `/fix-sonar-warnings` because they require human judgment. Each section explains the issue, what was tried, and the open question.

---

## 1. typescript:S7739 — `Do not add 'then' to a class.` (38 occurrences)

**Files (all in `system-test/typescript/src/testkit/dsl/core/scenario/`):**
- `given/given-clock.ts:37`
- `given/given-country.ts:35`
- `given/given-coupon.ts:50`
- `given/given-order.ts:51`
- `given/given-product.ts:31`
- `given/given-promotion.ts:31`
- `given/given-stage.ts:71`
- `then/then-browse-coupons.ts:52, 69, 92`
- `then/then-cancel-order.ts:141, 160, 178, 214`
- `then/then-contract.ts:97, 119, 148, 184`
- `then/then-place-order.ts:171, 198, 316, 338, 370, 389, 442`
- `then/then-publish-coupon.ts:99` (and 3 more)
- `then/then-view-order.ts` (sub-stage classes)

**Why deferred:** These `.then()` methods are part of a deliberate Given/When/Then BDD DSL. The class `then()` returns the next stage (e.g. `ThenContractStage`), mirroring Gherkin syntax. Replacing this would gut the DSL.

**The real risk** — and the reason Sonar flags it — is that any DSL stage class that has `.then` becomes structurally indistinguishable from a `Promise`/`PromiseLike`. If any code path ever does `await someStage.when()...then()`, the runtime will treat the stage as a thenable and call `.then()` itself, resolving the promise immediately rather than continuing the DSL chain.

**Tried:** Looked at the DSL pattern. Many of these classes already declare `implements PromiseLike<void>` (e.g. `ThenContractStage`, `ThenPublishCouponResultStage`) — so they are *intended* to be awaited. In those cases the `.then(onfulfilled, onrejected)` overload IS the PromiseLike contract. SonarCloud flags it anyway because it can't tell intent from signature.

**Open question for the user:**
- Option A — Accept this as a known false positive for the DSL pattern; suppress via `// NOSONAR` comments or Sonar config exclusion (e.g. exclude `**/scenario/**`). Recommended.
- Option B — Rename DSL `.then()` to something else (e.g. `.thenStage()` or `.continue()`). This would change the readability of the BDD tests, but is the only way to truly silence the rule without suppression.
- Option C — Leave as-is and let the count stay at 38.

---

## 2. java:S107 — Constructor with too many parameters (2 occurrences)

**Files:**
- `system/monolith/java/src/main/java/com/mycompany/myshop/core/entities/Order.java:73`
- `system/multitier/backend-java/src/main/java/com/mycompany/myshop/backend/core/entities/Order.java:73`

**Message:** "Constructor has 15 parameters, which is greater than 7 authorized."

**Why deferred:** `Order` is a domain entity with 15 required fields (orderNumber, sku, quantity, country, couponCode, status, unitPrice, basePrice, discountRate, discountAmount, subtotalPrice, taxRate, taxAmount, totalPrice, timestamp — or similar). Reducing this needs a structural decision.

**Tried:** Read the constructor — it really does take 15 fields, and they're all required to construct a valid order.

**Open question for the user:**
- Option A — Introduce a builder (`Order.builder().orderNumber(...).sku(...)....build()`) and route construction through it. Recommended for API ergonomics.
- Option B — Group related fields into value objects (e.g. `OrderPricing(basePrice, discountRate, discountAmount, subtotalPrice, taxRate, taxAmount, totalPrice)`, `OrderItem(sku, quantity)`). Cleaner but bigger refactor and changes the entity's storage model.
- Option C — Suppress with `@SuppressWarnings("java:S107")` and a comment explaining the entity-snapshot constraint.

The same entity exists in both monolith and multitier-backend, so the chosen pattern should be applied to both for consistency.

---

## 3. typescript:S3776 — Cognitive Complexity too high (7 CRITICAL + 1 MAJOR = 8 occurrences)

**Files:**
- `system/monolith/typescript/src/app/api/orders/route.ts:10` — complexity 26 (allowed 15)
- `system/monolith/typescript/src/lib/validation.ts:31` — complexity 16 (allowed 15)
- `system-test/typescript/src/testkit/driver/adapter/myShop/api/my-shop-api-driver.ts:46` — empty `close()` (S1186, not S3776 — see #4)
- `system-test/typescript/src/testkit/dsl/core/scenario/then/then-cancel-order.ts:46` — complexity 46
- `system-test/typescript/src/testkit/dsl/core/scenario/then/then-contract.ts:55` — complexity 21
- `system-test/typescript/src/testkit/dsl/core/scenario/then/then-place-order.ts:66` — complexity 51
- `system-test/typescript/src/testkit/dsl/core/scenario/then/then-publish-coupon.ts:54` — complexity 22
- `system-test/typescript/src/testkit/dsl/core/scenario/then/then-view-order.ts:46` — complexity 27

**Why deferred:** These functions have many branches (assertion-execution methods that fan out across success/error paths and multiple assertion types). Mechanical extraction would just move the complexity around. A real fix means re-thinking how the assertion-execution loop is structured.

**Open question for the user:**
- Option A — Extract sub-methods per branch (e.g. `_executeOrderAssertions()`, `_executeErrorAssertions()`, `_executeClockAssertions()` already exist as fields — wire them through dedicated runner methods).
- Option B — Replace the branching with a list of `Assertion` objects each with its own `.run()` method (Strategy pattern).
- Option C — Raise the threshold or suppress per-function with `// NOSONAR` and a justification comment.

For the monolith `route.ts:10` and `validation.ts:31` files, the complexity is only 1-11 over the threshold and could likely be tamed with a small extraction. Consider doing those first.

---

## 4. typescript:S1186 — Empty async method `close()` (1 occurrence) — RESOLVED

This was actually fixed during the run — added a single-line comment explaining "no resources to release; the API client uses fetch per-call." Sonar should auto-clear this on next analysis. If it doesn't, the comment may need to be more substantive (e.g. wrap the body in `await Promise.resolve();`).

(Removed from "deferred" — kept here for traceability.)

---

## Notes

- This file is generated by `/fix-sonar-warnings` and is meant to be transient. Once each item above has a decision, apply it and delete the corresponding section. When the file is empty, delete the file (and `plans/` if empty).
- A new `/fix-sonar-warnings` run on this repo will append a new timestamped block above — do not lose context across runs.
