# 2026-08-17 14:48 UTC — residual error-handling defects in `backend-clean-java`

**Status:** 🟡 **Executed down to one deferred item.** The four surviving defects this plan tracked
(gateway failure signalling, exception-type location, the leaky 500 body, the `Integer.MAX_VALUE`
workaround) were all fixed on 2026-08-18. What remains is the follow-up the status-mapping decision
deliberately created.

## TL;DR

**Why:** `1449` fixed tier 2 — expected business outcomes are now values in a sealed `UseCaseError`,
declared in every use case signature. It left tier 3 alone. Infrastructure failures were signalled
three different ways across three gateways, the catch-all handler leaked internal messages into the
500 body, and a stale workaround in `PublishCoupon` wrote `Integer.MAX_VALUE` where the domain models
unlimited as `null`.

**End result:** One infrastructure-failure policy applied uniformly to all three gateways, a 500
response that says nothing about the server's internals, and `PublishCoupon` writing what
`UsageQuota` actually means. All three are done; the HTTP status a gateway failure maps to is the one
open follow-up.

## ▶ Next executable step (resume here)

**Item 5 — map gateway failures off the catch-all 500.** All the groundwork is in place: the three
gateways now throw `TaxGatewayException` / `ClockGatewayException` / `ErpGatewayException`, all
extending `infrastructure/external/GatewayException`, so the mapping is a single
`@ExceptionHandler(GatewayException.class)` in `presentation/exception/GlobalExceptionHandler`.

This is a **behaviour change visible to the system tests**, which is why it was split out. Executing
it means: add the handler, then run the system-test suite for the affected configuration — and per
`feedback_ask_before_local_system_tests`, ask the user before running it; never self-initiate.
Decide 502 (upstream gave a bad answer) vs 503 (upstream unreachable, try later) as part of the same
change, and update the "HTTP status" note in `docs/atdd/code/language-equivalents.md` to match.

## Items

- [ ] **Item 5: dedicated HTTP status for gateway failures** — ⏳ Deferred: it is a behaviour change
      visible to the system tests, and items 1–4 were deliberately landed as a pure refactor so the
      suite stayed a control rather than a variable. Pick it up on its own, where it can be tested in
      isolation.

## Settled — do not reopen

- **Use case failure signalling → result objects.** `UseCase<TRequest, TResponse>` returning
  `Result<TResponse, UseCaseError>`, enforced by the `USECASES_IMPLEMENT_THE_USECASE_INTERFACE`
  ArchUnit rule.
- **Compiler-enforced exhaustiveness → sealed `UseCaseError`**, switched over in
  `presentation/UseCaseResponder` with no `default` branch.
- **Per-rule types vs messages → messages.** `UseCaseError.Invalid(field, message)`.
- **Domain exception taxonomy → resolved by demolition.** `NotExistValidationException` deleted;
  `ValidationException` survives as the domain's own broken-rule signal.
- **`Guard` stays as-is.** `IllegalArgumentException` is the JDK's idiomatic programmer-error signal.
- **Gateway failures are their own type family**, under `infrastructure/external/GatewayException`,
  never `IllegalStateException`. An unknown external-system mode is misconfiguration, not a gateway
  failure, and deliberately keeps `IllegalStateException`.
- **The catch-all 500 body is a fixed string.** Message and causes go to the log, not the response.
- **Unlimited is `null`, not a sentinel.** The legacy services keep `Integer.MAX_VALUE`; they are the
  before-picture.

## Cross-language gate — satisfied

`docs/atdd/code/language-equivalents.md` was created on 2026-08-18 and states the .NET and TypeScript
equivalents of all three decisions above. Item 5 must update its "HTTP status" note when it lands.

## Non-goals

- **The legacy `backend-java` / `backend-dotnet` / `backend-typescript` projects.** This is about the
  clean variant only. They are the before-picture.
- **Bean Validation / `MethodArgumentNotValid` handling.** A presentation concern, orthogonal to the
  error model.
- **The `TypeValidationMessageExtractor` / Jackson class-name regex machinery.** A different problem.
