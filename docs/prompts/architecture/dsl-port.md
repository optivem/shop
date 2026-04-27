# DSL Port Rules

## Fluent Interface

All DSL port interfaces use a fluent builder pattern. Every `with*()` method returns the same interface type to enable method chaining.

## with*() Method Signatures

- Every configurable field must have a `withFieldName(String value)` method.
- For fields that have a natural typed representation, add an overloaded `withFieldName(T value)` (e.g. `withQuantity(int)`, `withUnitPrice(double)`).
- The `String` overload is mandatory; the typed overload is optional but recommended for readability.
- No-arg `with*()` variants are permitted where the field has a meaningful "use default" semantic (e.g. `withCouponCode()` with no argument means omit the coupon).

## Stage Transitions

- `and()` — returns the parent stage to allow chaining more steps at the same level.
- `when()` — transitions from a Given step to `WhenStage`.
- `then()` — transitions from a Given step directly to `ThenStage` (skipping When).
- Given/When/Then base interfaces must declare these transitions explicitly.

## Then Generics

`ThenStep<TThen>` is the generic base interface for all Then step types. The type parameter `TThen` is always the implementing interface itself (e.g. `ThenSuccess extends ThenStep<ThenSuccess>`). This ensures that `and()` and navigation methods return the correct type, preserving the fluent chain without casts.

## ThenResultStage

`ThenResultStage` extends `ThenStage` and adds two terminal assertions:

- `shouldSucceed()` → `ThenSuccess`
- `shouldFail()` → `ThenFailure`

`ThenFailure` adds assertion methods specific to error cases (e.g. `errorMessage()`, `fieldErrorMessage()`). `ThenSuccess` adds no extra assertion methods — assertions are accessed via `and()` navigation to `ThenOrder`, `ThenCoupon`, etc.

## No-arg Assertion Variants

Assertion methods with no arguments (e.g. `hasAppliedCoupon()`, `hasTime()`) are permitted where verifying the presence of a value (without asserting its exact value) is meaningful.
