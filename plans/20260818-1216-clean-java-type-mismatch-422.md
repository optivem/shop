# 2026-08-18 12:16:44 UTC — Restore the 422 type-mismatch response in backend-clean-java by removing the DTO package regex

## TL;DR

**Why:** Commit `caa5cb61` moved the use case request DTOs out of `usecases.dtos` into their use case packages, but `GlobalExceptionHandler` still identifies the target DTO by regex-matching the old package name out of Jackson's parse-failure message. The match now fails, so a non-integer quantity falls through to a generic `400 Bad Request` instead of the `422` field error, and three component tests fail on `main`.

**End result:** `backend-clean-java` reports a Jackson type mismatch as a `422` validation error again, and it does so by reading the offending class and field straight off the exception — so no package name is written down as a string anywhere, and the next DTO move cannot silently break it.

## Outcomes

What we get out of this — the goals and deliverables:

- `multitier-backend-clean-java-commit-stage` is green again: `PlaceOrderNegativeComponentTest.shouldRejectOrderWithInvalidQuantity` and both `shouldRejectOrderWithNonIntegerQuantity` cases (`"3.5"`, `"lala"`) return `422 UNPROCESSABLE_ENTITY` with field error `quantity` / `"Quantity must be an integer"`.
- `GlobalExceptionHandler` no longer names a package in a string literal. The DTO class and field come from Jackson's own `MismatchedInputException` path, so moving a request DTO cannot break the handler again — the failure mode that neither the compiler nor `REQUESTS_AND_RESPONSES_LIVE_WITH_THEIR_USECASE` could catch is gone by construction.
- The `400 Invalid request format` branch still covers bodies Jackson genuinely cannot read (malformed JSON), so the two rejection kinds stay distinguishable: a bad value on a known field is a `422` naming that field, an unparseable body is a `400`.
- The blocked downstream CI stages — Integration, Contract, Build External System Simulator, Real-Mode Contract, Linter — get to run, confirming nothing else regressed behind the component-test gate.

## Background — what broke and why

The failure, reproduced locally (`./gradlew componentTest --tests '…PlaceOrderNegativeComponentTest'` in `system/multitier/backend-clean-java`: 19 tests, 3 failed — identical to CI run [32134725668](https://github.com/optivem/shop/actions/runs/32134725668), which reported 62 tests / 3 failed):

```
java.lang.AssertionError: [rejection status]
Expecting actual:
  400 BAD_REQUEST
to be in:
  [422 UNPROCESSABLE_ENTITY]
    at …testkit.dsl.core.shared.UseCaseResult.shouldFail(UseCaseResult.java:70)
    at …component.latest.PlaceOrderNegativeComponentTest.shouldRejectOrderWithNonIntegerQuantity(PlaceOrderNegativeComponentTest.java:89)
```

Root cause, pinned to `system/multitier/backend-clean-java/src/main/java/com/mycompany/myshop/backend/presentation/exception/GlobalExceptionHandler.java:36`:

```java
private static final Pattern CLASS_NAME_PATTERN =
    Pattern.compile("(com\\.mycompany\\.myshop\\.backend\\.usecases\\.dtos\\.[^\\[\\]\"\\s\\)]+)");
```

The chain: Jackson raises `InvalidFormatException` whose message ends `(through reference chain: com.mycompany.myshop.backend.usecases.order.PlaceOrderRequest["quantity"])`. The pattern still expects `usecases.dtos.…`, so `extractDtoClass` (line 154) returns `null` → `tryParseFieldError` (line 113) returns `null` → `handleHttpMessageNotReadable` (line 84) falls through to the generic `400` branch at lines 102–110, never reaching the `@TypeValidationMessage("Quantity must be an integer")` declared at `usecases/order/PlaceOrderRequest.java:21`.

The comment at lines 34–35 states the invariant the commit broke: *"Must track wherever the request DTOs live."* Nothing enforced it — a package name in a string literal is invisible to both `javac` and ArchUnit.

The empty / null / negative quantity tests keep passing because Jackson coerces `""` to `null`; those go through bean validation (`handleMethodArgumentNotValid`, already `422`) and never reach this handler. Only genuinely unparseable values (`"3.5"`, `"lala"`, `"invalid-quantity"`) hit it.

**Scope is one project.** The sibling Java handlers were checked and need no change: `system/multitier/backend-java/…/api/exception/GlobalExceptionHandler.java:30` targets `com.mycompany.myshop.backend.core.dtos` and `system/monolith/java/…/api/exception/GlobalExceptionHandler.java:32` targets `com.mycompany.myshop.core.dtos` — both packages still exist, since the DTO move touched `backend-clean-java` only. The TypeScript and .NET implementations do not use this reflection/regex mechanism at all, so there is no twin fix in those languages.

**Alternative considered and rejected:** widening the regex to `com\.mycompany\.myshop\.backend\.usecases\.`. One token, restores green, and leaves exactly the same trap armed for the next package move. Not a fallback — do not implement it; the whole point of the fix is that no package name is written down as a string.

Note that the `400 "Invalid request format"` branch is not an alternative to the `422` — it is the answer for a *different* input. A body Jackson cannot parse at all (truncated or malformed JSON) has no property and no target class to name, so there is no field error to build. Type mismatch on a known property → `422` with the field; unreadable body → `400`. Both branches stay.

## ▶ Next executable step (resume here)

Rewrite the type-mismatch detection in `system/multitier/backend-clean-java/src/main/java/com/mycompany/myshop/backend/presentation/exception/GlobalExceptionHandler.java` to read the DTO class and field from the exception instead of its message:

- In `handleHttpMessageNotReadable` (line 84), walk `ex.getCause()` looking for a `com.fasterxml.jackson.databind.exc.MismatchedInputException` (`InvalidFormatException` is a subclass).
- From it, take the last entry of `getPath()`: `Reference.getFrom()` gives the owning object or class (normalise to a `Class<?>` — `getFrom()` returns `Object`, so handle both the instance and `Class` cases), and `getFieldName()` gives the JSON property.
- Feed that class to the existing `TypeValidationMessageExtractor.extractFieldMessages(Class<?>)` and look the field up by name (the extractor lower-cases its keys — match accordingly). Only produce the `422` when a `@TypeValidationMessage` exists for that field.
- Keep the emitted `422` ProblemDetail byte-identical in shape: type `validationErrorTypeUri`, title `"Validation Error"`, detail `VALIDATION_DETAIL`, `timestamp`, and `errors` = one entry with `field`, `message`, `code: "TYPE_MISMATCH"`.
- Keep the existing `400 "Invalid request format"` branch for everything else — an unparseable body names no property, so there is no field error to build.
- Delete `CLASS_NAME_PATTERN` (line 36), `extractDtoClass` (lines 154–165), the now-unused `java.util.regex.Pattern` import, and the stale lines 34–35 comment. Replace it with a short comment noting the class and field come from the exception itself, so no package name needs keeping in sync.

Then run `./gradlew componentTest` in `system/multitier/backend-clean-java` and confirm 62/62.

## Steps

- [ ] Step 1: Replace the message-regex lookup in `GlobalExceptionHandler` with `MismatchedInputException.getPath()`-based extraction of the DTO class and field, as detailed in the resume block above. Preserve the `422` ProblemDetail shape and the `400` unreadable-body branch exactly.
- [ ] Step 2: Delete the dead machinery — `CLASS_NAME_PATTERN`, `extractDtoClass`, the `java.util.regex.Pattern` import — and replace the stale "must track wherever the request DTOs live" comment with one explaining the new, coupling-free mechanism.
- [ ] Step 3: Run `./gradlew componentTest` in `system/multitier/backend-clean-java` — all 62 component tests pass, including the three that failed.
- [ ] Step 4: Run `./gradlew build` in `system/multitier/backend-clean-java` — unit tests, integration tests, and `ArchitectureTest` (including `REQUESTS_AND_RESPONSES_LIVE_WITH_THEIR_USECASE`) pass.
- [ ] Step 5: Run `./compile-all.sh` from the repo root — every system and system-test project across all three languages still compiles.
- [ ] Step 6: Commit (ask first, per the repo's commit gate) and confirm the `multitier-backend-clean-java-commit-stage` re-run reaches the stages this failure blocked: Integration Tests, Contract Tests, Build External System Simulator Image, Real-Mode Contract Tests, Linter.

## Open questions

- `Reference.getFrom()` returns `Object` — for a failure while binding a top-level request body it is normally the partially-built DTO instance, but it can be a `Class` (or a `Map`/collection for nested structures). Step 1 should handle instance and `Class` uniformly and simply fall through to the `400` when the owner cannot be resolved to a class carrying `@TypeValidationMessage`; confirm during implementation that the `PlaceOrderRequest` case resolves as expected.
- Nothing currently pins this behaviour at the unit level — the regression was caught only by a booted component test. Worth deciding whether a small unit test on `GlobalExceptionHandler` (feed it a synthetic `HttpMessageNotReadableException` wrapping an `InvalidFormatException`, assert `422` + field message) earns its place, or whether the component test is the right and sufficient guard.
