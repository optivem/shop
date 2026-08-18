# 2026-08-17 14:48 UTC — residual error-handling defects in `backend-clean-java`

**Status:** 🟢 **Unblocked.** The headline question this plan opened with — exceptions or result
objects at the use case boundary? — was **settled by execution on 2026-08-17**: sibling plan
`20260817-1449` landed `Result<TResponse, UseCaseError>` across all seven use cases, and the
compiler-enforced exhaustiveness this plan wanted is now carried by the sealed `UseCaseError` rather
than by a sealed exception hierarchy. That plan is done and deleted.

**Origin:** Raised by the user during the clean-architecture refactor of `backend-clean-java`
(commits `093efb09` → `33884122`). What survives here is the part `1449` never touched: three
concrete defects in the *infrastructure* and *presentation* tiers, all verified still present on
`main` as of 2026-08-18.

## TL;DR

**Why:** `1449` fixed tier 2 — expected business outcomes are now values in a sealed `UseCaseError`,
declared in every use case signature. It left tier 3 alone. Infrastructure failures are still
signalled three different ways across three gateways, the catch-all handler still leaks internal
messages into the 500 body, and a stale workaround in `PublishCoupon` writes `Integer.MAX_VALUE`
where the domain now models unlimited as `null`.

**End result:** One infrastructure-failure policy applied uniformly to all three gateways, a 500
response that says nothing about the server's internals, and `PublishCoupon` writing what
`UsageQuota` actually means.

## Settled — do not reopen

These were open questions in this plan's first draft. Execution answered them.

- **Use case failure signalling → result objects.** `UseCase<TRequest, TResponse>` returning
  `Result<TResponse, UseCaseError>`, implemented by all seven use cases and enforced by the
  `USECASES_IMPLEMENT_THE_USECASE_INTERFACE` ArchUnit rule.
- **Compiler-enforced exhaustiveness → sealed `UseCaseError`.** `presentation/UseCaseResponder`
  switches over it with no `default` branch. Adding a case breaks every adapter that ignores it.
- **Per-rule types vs messages → messages.** `UseCaseError.Invalid(field, message)`, not a type per
  business rule.
- **Domain exception taxonomy → resolved by demolition.** `NotExistValidationException` is deleted;
  its 404 job belongs to `UseCaseError.NotFound`. `ValidationException` survives as the domain's own
  broken-rule signal, translated at the use case boundary by `UseCaseError.from(ValidationException)`.
- **`Guard` stays as-is.** `IllegalArgumentException` is the JDK's idiomatic programmer-error signal;
  keeping it makes tier 1 visibly different from the domain's own exception types, which is the
  distinction the whole policy rests on.

## Surviving defects — verified present 2026-08-18

1. **Gateway failures are signalled three ways for one failure class.** `HttpTaxGateway` throws a
   dedicated `TaxGatewayException` (3 sites); `HttpClockGateway` and `HttpErpGateway` throw bare
   `IllegalStateException` (10 sites) for the identical failure class — non-2xx status, IO failure,
   interrupt. `IllegalStateException` is the JDK's *programmer-error* signal, so using it for a
   network timeout conflates tier 3 with tier 1 — the exact confusion this plan exists to remove.
2. **`TaxGatewayException` lives in `domain/exceptions/`.** An infrastructure failure type sitting in
   the domain package, alongside `ValidationException`. Whatever policy item 1 lands must also decide
   where these types belong — `infrastructure/external/` is the honest home.
3. **`handleGeneralException` leaks internals into the 500 body.**
   `GlobalExceptionHandler:165-191` puts `ex.getMessage()` *and* the deepest root-cause message into
   the response. Verified 2026-08-18: **no test asserts on that body** — the only matches for
   `"Internal server error"` under `system-test/` are compiled Playwright build artifacts, so the
   body can be changed without touching a test.
4. **Stale `Integer.MAX_VALUE` workaround in `PublishCoupon:36-39`.** Converts a null `usageLimit` to
   `Integer.MAX_VALUE` although `UsageQuota` models unlimited as `null`. The comment admits it and
   explains the constraint: the published row records `MAX_VALUE` and callers read it back — so this
   is a persistence-and-callers change, not a one-line edit.

## ▶ Next executable step (resume here)

**Item 1 — unify the gateway failure policy.** This is the largest of the four and the one the other
three are easiest to fold into. Concretely: add `ClockGatewayException` and `ErpGatewayException` as
siblings of the existing `TaxGatewayException`, replace the 10 `IllegalStateException` throws in
`infrastructure/external/{clock,erp}` with them, and move all three types out of `domain/exceptions/`
into `infrastructure/external/` in the same pass (item 2). Then decide the status mapping — see the
open question below, which must be answered before the code lands.

## Open question — must be answered before item 1 lands

- [ ] **What HTTP status does a gateway failure map to?** Today all three fall into the catch-all
      500. A dedicated 502/503 mapping is arguably more correct, but it is a **behaviour change
      visible to the system tests**. Per `feedback_ask_before_local_system_tests`: ask the user before
      running them; never self-initiate.

      **Recommendation: keep 500 for now** and land items 1–2 as a pure refactor with zero behaviour
      change, so the system-test suite stays a control rather than a variable. Raise the status
      mapping as its own change afterwards, when it can be tested in isolation.

## Items

- [ ] **Item 1: one gateway failure policy.** `ClockGatewayException` + `ErpGatewayException`
      alongside `TaxGatewayException`; all 10 `IllegalStateException` throw sites in
      `infrastructure/external/{clock,erp}` converted. No status-mapping change (see open question).
- [ ] **Item 2: relocate the gateway exception types** out of `domain/exceptions/` into
      `infrastructure/external/`. Fold into item 1's commit — it is the same blast radius.
- [ ] **Item 3: stop leaking internals in the 500 body.** `GlobalExceptionHandler:165-191` — log the
      full message and root cause at ERROR as it does today, but return a fixed, non-revealing body.
      No test asserts on it (verified above).
- [ ] **Item 4: fix the `Integer.MAX_VALUE` workaround** in `PublishCoupon:36-39` — pass the null
      through to `UsageQuota`, and follow the read-back path (repository mapper, `BrowseCoupons`
      response, any system-test assertion on the usage limit) so the persisted representation and the
      callers agree.

## Cross-language gate

Whatever items 1–2 decide must state its .NET and TypeScript equivalents in
`docs/atdd/code/language-equivalents.md` **before** `backend-clean-dotnet` /
`backend-clean-typescript` exist. `system/multitier/` has `backend-clean-java` only today, so
deciding now means porting one model to two languages instead of changing three codebases later.
**A decision that only works in Java is not a decision** — see the
`feedback_component_pact_layer_opt_in` precedent on keeping the three implementations honest twins.

## Non-goals

- **The legacy `backend-java` / `backend-dotnet` / `backend-typescript` projects.** This is about the
  clean variant only. They are the before-picture.
- **Bean Validation / `MethodArgumentNotValid` handling.** The DTO-annotation path in
  `GlobalExceptionHandler` is a presentation concern, orthogonal to the error model.
- **The `TypeValidationMessageExtractor` / Jackson class-name regex machinery.** Ugly, but a
  different problem.
