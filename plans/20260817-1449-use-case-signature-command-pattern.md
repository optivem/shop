# 2026-08-17 14:49:58 UTC — Uniform use case signature (Interactor + Result) in `backend-clean-java`

**Source:** `optivem/academy/substack/articles/reviewed/PAID-CLEAN-use-case-signature.md`
("Clean Architecture: One Class Per Use Case Is NOT Enough")

**Status:** 🔴 **Blocked — contradicts `20260817-1448`.** This plan's premise is a decision that a
sibling plan holds open and recommends the other way. Do not start Step 1 until that is resolved.

## ⛔ Contradiction — this plan conflicts with `20260817-1448`

`plans/20260817-1448-error-handling-strategy-clean-java.md` was written one minute before this plan,
from a different origin (a design discussion with the user rather than the article), and treats
"exceptions or result objects at the use case boundary?" as **an open question — its OQ1 — with a
recommendation to keep exceptions**. This plan takes results as settled and builds seven steps on
top. Both were committed together in `1b3aafc1`. They cannot both be executed.

| Question | This plan (`1449`) | `1448` |
|---|---|---|
| **Use case failure signalling** | **Decides results.** All 7 use cases become `UseCase<TRequest, TResponse>` returning `Result<TResponse, UseCaseError>` | **Recommends exceptions.** Java has no `?` and no must-use, so a dropped `Result` fails *silently* — worse than an unhandled exception; 7 signatures + controllers + future twins churn |
| **Domain exception taxonomy** | Shrinks it to infrastructure; deletes `handleValidationException` and `handleNotExistValidationException` (Step 5) | Reshapes it instead: `NotFoundException` as a *sibling* of `ValidationException` (the current `extends` asserts an is-a that the 404-vs-422 mapping contradicts) |
| **Cross-language parity** | Explicit **non-goal** — `backend-clean-java` is the clean-architecture reference; the twins are deliberately not clean-architecture | Treats it as a **gate**: "a decision that only works in Java is not a decision", to be settled *before* `backend-clean-dotnet` / `backend-clean-typescript` exist |

**Not actually in conflict**, despite appearances:

- **Compiler-enforced exhaustiveness.** Both plans want the compiler to fail the build when a
  failure case is unhandled. They differ only in what carries the cases — this plan seals
  `UseCaseError`, `1448` seals the *exception* hierarchy and switches over it in a single handler.
  Same goal, same mechanism, different carrier. **This idea survives either way** and is the single
  highest-value item in both plans.
- **Generic vs per-rule failure cases.** `UseCaseError`'s three cases (`NotFound` / `Invalid` /
  `Conflict`) are the same answer `1448` OQ4 recommends (messages, not a type per business rule).
- **Infrastructure failures stay exceptions.** This plan's third open question and `1448`'s tier 3
  agree.

**Gaps this plan does not cover** — carried over from `1448`, and worth folding in if this plan wins:

- **Gateway inconsistency.** `HttpClockGateway` and `HttpErpGateway` throw bare
  `IllegalStateException` (10 sites) where `HttpTaxGateway` throws a dedicated `TaxGatewayException`
  (3 sites), for the same failure class — non-2xx status, IO failure, interrupt. This plan's open
  question mentions `TaxGatewayException` but never addresses the split. `IllegalStateException` is
  the JDK's *programmer-error* signal, so using it for a network timeout conflates tiers.
- **`handleGeneralException` leaks internals.** It puts `ex.getMessage()` *and* the root cause
  message into the 500 response body. Step 5 keeps the handler unchanged; check whether a system
  test asserts on that body before altering it.
- **Stale `Integer.MAX_VALUE` workaround** in `PublishCoupon:33-36` — converts a null `usageLimit`
  to `Integer.MAX_VALUE` although `UsageQuota` now models unlimited as `null`. Confirmed still
  present as of `1b3aafc1`. Independent of this plan, but inside Step 3's blast radius.

**Resolution: not taken.** The likely shape is to fold `1448`'s census, defect list, and the gaps
above into this plan as supporting analysis, then close `1448` — this plan is the one with executable
steps. But that presumes the headline question goes to results, which is exactly what has not been
decided. **Do not execute either plan until this is resolved.**

## TL;DR

**Why:** `backend-clean-java` already has one class per use case, but nothing constrains what those
classes look like from outside. Seven use cases have four input shapes (`PlaceOrderRequest`,
`PublishCouponRequest`, raw `String`, no argument) and three output shapes
(response DTO, `void`, nothing). Expected business outcomes — order not found, order not
cancellable, coupon code invalid — are thrown as `ValidationException` /
`NotExistValidationException` and reassembled downstream in `GlobalExceptionHandler`, so no
signature in the codebase says those outcomes exist.

**End result:** Every use case implements
`UseCase<TRequest, TResponse>` with the single method
`Result<TResponse, UseCaseError> execute(TRequest request)`. Expected outcomes are values in a
sealed `UseCaseError`, mapped exhaustively in the presentation layer. `GlobalExceptionHandler`
keeps only genuine infrastructure/framework failures. HTTP behaviour is byte-identical — every
status code and `ProblemDetail` body stays exactly as it is today, proven by the existing
component, contract, and system tests.

## Outcomes

- **One shape for all seven use cases.** `PlaceOrder`, `ViewOrderDetails`, `BrowseOrderHistory`,
  `CancelOrder`, `DeliverOrder`, `PublishCoupon`, `BrowseCoupons` all read
  `implements UseCase<XRequest, XResponse>` in the class header.
- **Every use case takes exactly one request object.** The four raw-`String` / no-argument callers
  gain `ViewOrderDetailsRequest`, `BrowseOrderHistoryRequest`, `CancelOrderRequest`,
  `DeliverOrderRequest`, `BrowseCouponsRequest`.
- **Expected outcomes are declared, not thrown.** Order-not-found, order-not-in-cancellable-status,
  invalid coupon code, discount rate out of range become `UseCaseError` cases returned in a
  `Result`, visible in the signature.
- **Adapters map outcomes exhaustively.** `UseCaseError` is a sealed interface, so controllers
  switch over it with no `default` branch. Adding a case breaks compilation in every adapter that
  doesn't handle it — instead of a new exception subclass silently falling through to the 500
  handler.
- **`GlobalExceptionHandler` shrinks to infrastructure only.** Its `ValidationException` and
  `NotExistValidationException` handlers are deleted; what remains is Jackson parse failures, bean
  validation, and the catch-all 500.
- **The rule is enforced, not documented.** A new ArchUnit rule in `ArchitectureTest` fails the
  build if a class under `usecases.order` / `usecases.coupon` doesn't implement `UseCase`.
- **No behaviour change.** The only tests that change are the ones that speak to the seam directly:
  the three use case unit tests (assertions move from `catchThrowable` to `Result` inspection) and
  `OrderControllerIntegrationTest` (mocks now return a `Result`). Every component, contract and
  system test passes unmodified.

## Non-goals

- **No `CommandBus`.** The article's "Going further" section is explicit that a bus earns its keep
  past some fleet size or when cross-cutting concerns need uniform application. At seven use cases
  with two controllers and no logging/transaction/authorization decorators wanted, plain
  constructor wiring through `UseCaseConfig` is the right amount of infrastructure. The uniform
  signature this plan lands is exactly what makes a bus possible *later*, without touching a use
  case.
- **No `BaseUseCase`.** Interface only. Article: "Interface yes, inheritance no."
- **No other language/project.** `backend-clean-java` is the clean-architecture reference; the
  Java/.NET/TypeScript monolith and multitier twins are deliberately not clean-architecture and
  stay untouched.
- **No domain change.** Domain entities and value objects keep throwing `ValidationException` from
  their constructors. The translation to `UseCaseError` happens at the use case boundary
  (see Step 3).

## ▶ Next executable step (resume here)

**Step 8 — sync the article.** Steps 1–7 are landed and committed. The full local check is green
across every layer:

```bash
cd system/multitier/backend-clean-java
./gradlew build checkstyleAll componentTest integrationTest contractTest
```

238 tests — 94 unit (including the new `USECASES_IMPLEMENT_THE_USECASE_INTERFACE` rule), 62
component, 39 narrow-integration, 43 Pact contract — all passing, with the component, contract and
integration suites unchanged apart from `OrderControllerIntegrationTest`'s mocks. Behaviour is
proven unchanged; what remains is the article, which still describes a "before" state the repo has
moved past.

## Steps

- [x] **Steps 1–7 landed 2026-08-17, verified 2026-08-18.** `UseCase` / `UseCaseError` / `Result` under
      `usecases/`; five request records in `usecases/dtos/`; all seven use cases converted; the
      exhaustive `Result` → HTTP mapping in `presentation/UseCaseResponder`; both controllers on it;
      `GlobalExceptionHandler` reduced to framework failures; `USECASES_IMPLEMENT_THE_USECASE_INTERFACE`
      live in `ArchitectureTest`. All four layers green: 94 unit, 62 component, 39 narrow-integration,
      43 Pact contract.

      Three things landed differently from the plan above, each with a reason:
      - **`NotExistValidationException` is deleted, not just unhandled.** Only the three converted
        use cases ever threw it; its whole purpose was to signal 404 to the handler, which is now
        `UseCaseError.NotFound`. Leaving the class behind would leave a trap: a future thrower would
        get silently caught by a use case's `catch (ValidationException)` and rendered as a 422.
      - **`Result` has no `map`/`flatMap`.** Nothing needs them — the responder pattern-matches and
        the tests use `isOk()` / `value()` / `error()`. Dead API in a teaching codebase is worse than
        absent API; add them when a caller wants one.
      - **`PlaceOrder` translates at its boundary rather than returning each refusal inline.**
        Its six refusal paths are interleaved with the pricing sequence, and which refusal a caller
        sees first depends on that order. `execute` is a try/catch seam over an unchanged private
        `place`, so the ordering is preserved by construction rather than by re-derivation. The
        other six use cases return `Result.err` inline.

- [ ] **Step 8: Sync the article.** The article's opening "before" snippet describes a state
      `backend-clean-java` has already partly moved past (method names already all `execute`,
      `BrowseCoupons` already returns a response DTO, `PublishCoupon` already takes a request).
      Decide whether the article's example stays illustrative-only or is realigned to what the
      repo actually shows — and whether the repo now becomes the article's linked reference.

- [ ] **Step 9 (optional): restore the OpenAPI response schemas.** Controller methods now return
      `ResponseEntity<Object>` so a failure can carry a `ProblemDetail`, which costs springdoc the
      per-endpoint success schema it used to infer from `ResponseEntity<PlaceOrderResponse>`.
      Nothing consumes the generated spec today (no committed `openapi.json`, no contract test
      reads it — the Swagger UI is the only consumer), so this is cosmetic. The fix, if wanted, is
      an `@ApiResponse(responseCode = "...", content = @Content(schema = @Schema(implementation = X.class)))`
      per method.

## Decisions (settled 2026-08-17)

- **`Conflict` is dropped from `UseCaseError`.** No current use case maps to 409, and an
  unreachable sealed case is dead weight that the exhaustive switch still forces every adapter to
  handle. `UseCaseError` ships as `NotFound` + `Invalid` only; add `Conflict` when a use case
  actually needs it.
- **`Void` is the empty response type.** `UseCase<CancelOrderRequest, Void>` matches the article,
  so the repo and the article agree. Revisit only if `Result.ok(null)` proves noisy.
- **Gateway failures stay exceptions.** `TaxGatewayException` and friends are infrastructure
  failures — by the article's own rule they stay thrown and reach `handleGeneralException`.
  Verified 2026-08-18: no component test asserts a non-500 status for a gateway failure.
- **`Result` lives in `usecases/`.** It is a use-case-layer vocabulary type, the domain must not
  depend on it, and a new top-level package would need its own ArchUnit dependency rule to stay
  honest.
