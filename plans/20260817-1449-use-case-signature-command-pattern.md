# 2026-08-17 14:49:58 UTC — Uniform use case signature (Interactor + Result) in `backend-clean-java`

**Source:** `optivem/academy/substack/articles/reviewed/PAID-CLEAN-use-case-signature.md`
("Clean Architecture: One Class Per Use Case Is NOT Enough")

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
- **No behaviour change.** All existing tests pass unmodified except the use case unit tests, whose
  assertions move from `assertThrows` to `Result` inspection.

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

**Step 1 — land the vocabulary types, no use case touched yet.** Create three new files under
`system/multitier/backend-clean-java/src/main/java/com/mycompany/myshop/backend/usecases/`:

- `UseCase.java` — `public interface UseCase<TRequest, TResponse> { Result<TResponse, UseCaseError> execute(TRequest request); }`
- `UseCaseError.java` — `public sealed interface UseCaseError` with records `NotFound(String entityType, String id)`, `Invalid(String field, String message)`, `Conflict(String message)`
- `Result.java` — hand-rolled sealed `Result<T, E>` with `Ok(T value)` / `Err(E error)` records plus `isOk()`, `value()`, `error()`, `map`, `flatMap`. No new dependency (no Vavr, no third-party Either).

Java 21 (`build.gradle` `JavaLanguageVersion.of(21)`) gives sealed interfaces and pattern-matching
switch, so no preview flags are needed. Gate: `./gradlew build` in
`system/multitier/backend-clean-java` passes with the three new files unused. Unblocks Steps 2–5.

Note the ArchUnit constraints these files must satisfy (from `ArchitectureTest`):
`USECASES_DEPEND_ONLY_INWARD` and
`USECASES_ARE_FRAMEWORK_FREE_EXCEPT_JAKARTA_VALIDATION` — so all three are plain Java, no Spring,
no Lombok, no Jackson.

## Steps

- [ ] **Step 1: Add the vocabulary.** `UseCase`, `UseCaseError`, `Result` under `usecases/`
      (details in the resume block above). Nothing else changes; build stays green.

- [ ] **Step 2: Add the five missing request DTOs.** In `usecases/dtos/`:
      `ViewOrderDetailsRequest(String orderNumber)`,
      `BrowseOrderHistoryRequest(String orderNumberFilter)`,
      `CancelOrderRequest(String orderNumber)`,
      `DeliverOrderRequest(String orderNumber)`,
      `BrowseCouponsRequest()`. Match the shape of the existing `PlaceOrderRequest` /
      `PublishCouponRequest` (which are mutable classes with Jakarta validation annotations, not
      records — check before choosing a form: the two existing ones are deserialized by Jackson
      from the HTTP body, the five new ones are built in the controller from path/query params and
      have no Jackson involvement, so records are appropriate for the new five).

- [ ] **Step 3: Convert use cases one at a time, simplest first.** Recommended order:
      `DeliverOrder` → `CancelOrder` → `ViewOrderDetails` → `BrowseOrderHistory` →
      `BrowseCoupons` → `PublishCoupon` → `PlaceOrder`. For each:
      1. `implements UseCase<XRequest, XResponse>` (`Void` where the use case returns nothing:
         `CancelOrder`, `DeliverOrder`, `PublishCoupon`).
      2. Replace `throw new NotExistValidationException(...)` with
         `Result.err(new UseCaseError.NotFound("Order", orderNumber))`, preserving the exact
         message text so the `ProblemDetail.detail` field is unchanged.
      3. Replace `throw new ValidationException(...)` raised *by the use case itself* with
         `Result.err(new UseCaseError.Invalid(field, message))`, again preserving message text and
         field name (`ValidationException` carries an optional `fieldName` that drives whether the
         handler emits an `errors[]` array — this distinction must survive the translation).
      4. Where a *domain* constructor throws `ValidationException` (`CouponCode`, `Rate`,
         `ValidityPeriod`, `UsageQuota`, `Country`, `Order.cancel()`, `Order.deliver()`), catch it
         at the use case boundary and translate to `UseCaseError`. The domain stays
         exception-based; the use case is the seam.
      5. Update that use case's unit test in `src/test/.../usecases/` from `assertThrows` to
         `Result` inspection. Note only `PlaceOrderTest`, `CancelOrderTest`, `DeliverOrderTest`
         exist today — the other four use cases have no unit test, and this plan does not add one.
      6. Update the calling controller method for that use case (Step 4's mapper must exist first,
         so do Step 4 before or alongside the first conversion).
      Keep the build green after each use case. Do **not** keep the old method as a wrapper — the
      article suggests that for codebases with external callers; here every caller is in this
      repo and visible.

- [ ] **Step 4: Map `Result` to HTTP in the presentation layer.** Add a package-private helper in
      `presentation/` that turns `Result<T, UseCaseError>` into `ResponseEntity`, with an
      exhaustive `switch` over `UseCaseError` and **no `default` branch** — that exhaustiveness is
      the whole point. It must reproduce today's responses exactly:
      `NotFound` → 404 + `resource-not-found` type URI + title `"Resource Not Found"`;
      `Invalid` with a field → 422 + `validation-error` type URI + title `"Validation Error"` +
      detail `"The request contains one or more validation errors"` + `errors[]` array;
      `Invalid` without a field → 422 with the message as `detail` and no `errors[]`;
      `Conflict` → decide during execution (no current use case produces a 409; if none is needed,
      drop `Conflict` from `UseCaseError` rather than inventing a mapping).
      Every response also carries the `timestamp` property. Cross-check against
      `GlobalExceptionHandler:53-96` line by line.

- [ ] **Step 5: Shrink `GlobalExceptionHandler`.** Delete `handleValidationException` and
      `handleNotExistValidationException` once no use case throws them. Keep
      `handleMethodArgumentNotValid`, `handleHttpMessageNotReadable` (+ `tryParseFieldError` /
      `TypeValidationMessageExtractor`), and `handleGeneralException`. If domain code still throws
      `ValidationException` on a path that reaches the controller un-caught, that's a bug in
      Step 3.4 — don't leave the handler as a safety net, because that reintroduces the very
      "reassembled somewhere else entirely" problem the change removes.

- [ ] **Step 6: Enforce the shape in `ArchitectureTest`.** Add
      `USECASES_IMPLEMENT_THE_USECASE_INTERFACE`: all classes in
      `..usecases.order..` / `..usecases.coupon..` must implement `UseCase`. Follow the existing
      rule style in that file (static `ArchRule` field + explanatory javadoc naming the regression
      it prevents).

- [ ] **Step 7: Verify.** `./gradlew build` in `system/multitier/backend-clean-java`, then the full
      `./compile-all.sh` from the repo root. Then — **only with explicit user approval** — the
      `--sample` system-test run for Java per `CLAUDE.md`, plus the `componentTest`,
      `contractTest`, and `integrationTest` source sets for this project. The component and Pact
      contract tests are the real proof that HTTP behaviour is unchanged.

- [ ] **Step 8: Sync the article.** The article's opening "before" snippet describes a state
      `backend-clean-java` has already partly moved past (method names already all `execute`,
      `BrowseCoupons` already returns a response DTO, `PublishCoupon` already takes a request).
      Decide whether the article's example stays illustrative-only or is realigned to what the
      repo actually shows — and whether the repo now becomes the article's linked reference.

## Decisions (settled 2026-08-17)

- **`Conflict` is dropped from `UseCaseError`.** No current use case maps to 409, and an
  unreachable sealed case is dead weight that the exhaustive switch still forces every adapter to
  handle. `UseCaseError` ships as `NotFound` + `Invalid` only; add `Conflict` when a use case
  actually needs it.
- **`Void` is the empty response type.** `UseCase<CancelOrderRequest, Void>` matches the article,
  so the repo and the article agree. Revisit only if `Result.ok(null)` proves noisy.
- **Gateway failures stay exceptions.** `TaxGatewayException` and friends are infrastructure
  failures — by the article's own rule they stay thrown and reach `handleGeneralException`.
  Verify during Step 7 that no component test asserts a non-500 status for a gateway failure.
- **`Result` lives in `usecases/`.** It is a use-case-layer vocabulary type, the domain must not
  depend on it, and a new top-level package would need its own ArchUnit dependency rule to stay
  honest.
