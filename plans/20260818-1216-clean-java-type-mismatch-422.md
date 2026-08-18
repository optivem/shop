# 2026-08-18 12:16:44 UTC — Restore the 422 type-mismatch response in backend-clean-java by removing the DTO package regex

🤖 **Picked up by agent** — `Valentina_Desk` at `2026-08-18T12:33:30Z`


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

The code fix is done and verified locally; only the commit-and-confirm step is left.

- Ask the user for the commit gate, then commit `system/multitier/backend-clean-java/src/main/java/com/mycompany/myshop/backend/presentation/exception/GlobalExceptionHandler.java` (plus this plan file's deletion) via `gh optivem commit`.
- After the push, watch `multitier-backend-clean-java-commit-stage` and confirm it now reaches the stages this failure blocked: Integration Tests, Contract Tests, Build External System Simulator Image, Real-Mode Contract Tests, Linter.

Already verified locally: `./gradlew componentTest` → 62/62, `./gradlew build` → 95/95 (including `REQUESTS_AND_RESPONSES_LIVE_WITH_THEIR_USECASE` and `JACKSON_IS_CONFINED_TO_THE_OUTSIDE`), `./compile-all.sh` → all six variants clean.

## Steps

- [ ] Step 6: Commit (ask first, per the repo's commit gate) and confirm the `multitier-backend-clean-java-commit-stage` re-run reaches the stages this failure blocked: Integration Tests, Contract Tests, Build External System Simulator Image, Real-Mode Contract Tests, Linter.

## Open questions

None — all resolved before execution.

- `Reference.getFrom()` resolution: confirmed during implementation. `resolveOwnerClass` normalises instance-or-`Class` and falls through to the `400` when unresolvable; the `PlaceOrderRequest` case resolves as expected (the three previously-failing tests now pass).
- A unit test on `GlobalExceptionHandler`: decided **no**. The class and field now come off the exception object, so there is no string constant left to drift — the component test is the right and sufficient guard.
