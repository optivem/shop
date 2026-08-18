# Restore the OpenAPI per-endpoint response schemas in `backend-clean-java` (DEFERRED)

**Source plan:** `plans/20260817-1449-use-case-signature-command-pattern.md` (Step 9, optional).
**Status:** Deferred 2026-08-18 — cosmetic. Nothing consumes the generated spec today.
**Scope (this file):** `system/multitier/backend-clean-java` presentation layer only. No other language or project.

## TL;DR

**Why:** The uniform use case signature landed `Result<TResponse, UseCaseError>`, so every controller method now returns `ResponseEntity<Object>` — a success body or a `ProblemDetail`, decided at runtime by `UseCaseResponder`. springdoc infers response schemas from the declared generic type, so `ResponseEntity<Object>` costs it the per-endpoint success schema it used to get for free from `ResponseEntity<PlaceOrderResponse>`. The Swagger UI now shows an untyped response for all seven endpoints.

**End result:** Every controller method carries explicit `@ApiResponse` annotations naming its success schema and its failure shape, so the generated spec documents the same response types it did before the refactor — without giving up the runtime flexibility `ResponseEntity<Object>` buys.

## Why this is deferred, not dropped

There is no consumer to break:

- No `openapi.json` is committed to the repo.
- No contract test reads the generated spec — the Pact contract tests verify against recorded interactions, not the OpenAPI document.
- The Swagger UI is the only consumer, and it still renders working endpoints; only the response *schema* box is empty.

Execute this when the generated spec gains a real consumer — a committed spec artifact, a spec-driven client generator, or a schema-validating contract test.

## Outcomes

- Each of the seven endpoints declares its success schema explicitly via `@ApiResponse(responseCode = "...", content = @Content(schema = @Schema(implementation = X.class)))`.
- Failure responses declare `ProblemDetail` as their schema, matching what `UseCaseResponder` actually emits.
- The generated spec's response types match the pre-refactor spec; the Swagger UI shows a typed response body again.
- No runtime behaviour change — annotations only. Every component, contract, integration and system test passes unmodified.

## ▶ Next executable step (resume here)

**Blocked on need — do not start until the generated spec has a consumer.** First executable unit once unblocked: annotate `OrderController.placeOrder` (`presentation/controller/OrderController.java:58`) with its 201 `PlaceOrderResponse` schema plus the 404/422 `ProblemDetail` shape, confirm the change lands in the Swagger UI, then repeat the established shape across the remaining six methods.

## Steps

- [ ] Step 1: **`OrderController`** — annotate all five methods (`browseOrderHistory:52`, `placeOrder:58`, `getOrder:66`, `cancelOrder:72`, `deliverOrder:78`) with success schemas (`BrowseOrderHistoryResponse`, `PlaceOrderResponse`, `ViewOrderDetailsResponse`, `Void`, `Void`) and the `ProblemDetail` failure shape.
- [ ] Step 2: **`CouponController`** — annotate `createCoupon:37` (`Void`) and `browseCoupons:43` (`BrowseCouponsResponse`) the same way.
- [ ] Step 3: **Verify** the generated spec — start the app, fetch `/v3/api-docs`, and confirm each endpoint carries the expected success schema. Run `./gradlew build checkstyleAll componentTest integrationTest contractTest` to confirm nothing else moved.
