# 2026-08-17 14:48 UTC — error-handling strategy for `backend-clean-java`: exceptions vs result objects

**Status:** 🔴 **Blocked — contradicts `20260817-1449`.** Every question below is pending, and the
headline one is answered the other way by a sibling plan (see *Contradiction* below). No code changes
are authorised by this plan; the *Items* section is deliberately empty until the conflict and the
open questions are resolved.

**Origin:** Raised by the user during the clean-architecture refactor of `backend-clean-java`
(commits `093efb09` → `33884122`), while `Guard`, `UsageQuota`, and `ValidityPeriod` were being
introduced. The question was "should we throw exceptions or use result objects?" — the discussion
established a framework but took no decisions.

## ⛔ Contradiction — this plan conflicts with `20260817-1449`

`plans/20260817-1449-use-case-signature-command-pattern.md` was written one minute after this plan,
from a different source (the `PAID-CLEAN-use-case-signature` article), and **answers this plan's
headline question in the opposite direction**. Both were committed together in `1b3aafc1`. They
cannot both be executed.

| Question | This plan (`1448`) | `1449` |
|---|---|---|
| **OQ1 — use case failure signalling** | **Recommends exceptions.** Java has no `?` and no must-use; a dropped result fails silently; 7 signatures + controllers + future twins churn | **Decides results.** All 7 use cases become `UseCase<TRequest, TResponse>` returning `Result<TResponse, UseCaseError>` |
| **OQ3 — domain exception taxonomy** | Reshape it: `NotFoundException` as a *sibling* of `ValidationException`, split field-scoped from request-scoped | Moot under `1449` — the domain exception taxonomy shrinks to infrastructure only, and `ValidationException` / `NotExistValidationException` handlers are deleted (Step 5) |
| **OQ8 — cross-language parity** | Treats it as a **gate**: "a decision that only works in Java is not a decision", to be settled before the .NET/TS clean twins exist | Explicit **non-goal**: `backend-clean-java` is the clean-architecture reference and the twins are deliberately not clean-architecture, so they stay untouched |

**Not actually in conflict**, despite appearances:

- **OQ2 (sealing + exhaustive `switch`).** Both plans want the compiler to enforce handler coverage.
  They differ only in what carries the cases — this plan seals the *exception* hierarchy, `1449`
  seals `UseCaseError`. Same goal, same mechanism, different carrier. Whichever plan wins, the
  enforcement idea survives.
- **OQ4 (per-rule types vs messages).** This plan recommends messages; `1449`'s `UseCaseError` has
  three generic cases (`NotFound` / `Invalid` / `Conflict`), which is the same answer.
- **OQ5 / OQ6 (infrastructure failures, catch-all handler).** `1449` keeps gateway failures as
  exceptions routed to `handleGeneralException`, which is compatible with this plan's tier 3.

**Gaps `1449` does not cover** — these survive regardless of which plan wins, and are the part of
this plan worth keeping even if OQ1 goes to `1449`:

- **Defect 2** — `HttpClockGateway` and `HttpErpGateway` throw bare `IllegalStateException` (10
  sites) where `HttpTaxGateway` throws a dedicated `TaxGatewayException` (3 sites), for the same
  failure class. `1449` mentions `TaxGatewayException` in an open question but never addresses the
  inconsistency.
- **Defect 3** — the `NotExistValidationException extends ValidationException` / 404-vs-422
  mismatch. `1449` deletes both types rather than fixing the modelling, which resolves it by
  demolition; if `1449` is *not* executed, the defect stands.
- **Defect 4** — the stale `Integer.MAX_VALUE` workaround in `PublishCoupon:33-36`. Confirmed still
  present as of `1b3aafc1`. Neither plan's item list covers it.
- **OQ6's response-body leak** — `handleGeneralException` puts `ex.getMessage()` *and* the root
  cause message into the 500 body. `1449` Step 5 keeps that handler unchanged.

**Resolution: not taken.** The likely shape is to fold this plan's census, defect list, and the four
gaps above into `1449` as its supporting analysis, then close this plan — `1449` is the one with
executable steps. But that presumes OQ1 goes to results, which is exactly what has not been decided.
**Do not execute either plan until this is resolved.**

> ⚠️ **Sequence before the .NET / TypeScript clean twins exist.** `system/multitier/` currently has
> `backend-clean-java` only — there is no `backend-clean-dotnet` or `backend-clean-typescript` yet.
> Deciding the error model now means porting **one** model to two more languages. Deciding after the
> ports means changing three codebases and the language-equivalents doc.

## TL;DR

**Why:** The clean-architecture refactor introduced a deliberate two-tier split (`Guard` →
`IllegalArgumentException` for programmer errors, `ValidationException` for business rules) but never
stated the rule, never covered the third tier (infrastructure failures), and left the
domain↔presentation coupling unenforced. As the template that students clone, the error model *is*
part of the lesson — it should be a stated decision, not an accident of refactoring order.

**End result (proposed, not agreed):** A written three-tier error policy, enforced where the compiler
can enforce it, consistent across all three languages, and documented where a student will read it.

## Census — current state (2026-08-17)

Reproduce before starting; these will drift.

### Throw sites by exception type

| Type | Count | Where |
|---|---|---|
| `IllegalStateException` | 10 | `infrastructure/external/{clock,erp}` only |
| `ValidationException` | 9 | `domain/{entities,policies}`, `usecases/{order,coupon}` |
| `IllegalArgumentException` | 6 | `domain/Guard`, `domain/values`, `domain/entities` |
| `TaxGatewayException` | 3 | `infrastructure/external/tax` only |

### Throw sites by package

| Package | Count |
|---|---|
| `infrastructure/external/erp` | 6 |
| `usecases/order` | 4 |
| `infrastructure/external/clock` | 4 |
| `infrastructure/external/tax` | 3 |
| `domain/entities` | 3 |
| `domain` (`Guard`) | 3 |
| `domain/values` | 2 |
| `domain/policies` | 2 |
| `usecases/coupon` | 1 |

### Structures already in place

- `domain/exceptions/ValidationException` — carries an optional `fieldName`; **not sealed**.
- `domain/exceptions/NotExistValidationException extends ValidationException`.
- `presentation/exception/GlobalExceptionHandler` — `@RestControllerAdvice`, maps
  `ValidationException` → 422, `NotExistValidationException` → 404, plus a
  `@ExceptionHandler(Exception.class)` catch-all → 500.
- `domain/Guard` — `notNull` / `notNullOrEmpty` / `notNegative`, all → `IllegalArgumentException`,
  with a javadoc that already states the programmer-error-vs-business-rule distinction.
- Use cases (7): `BrowseCoupons`, `PublishCoupon`, `BrowseOrderHistory`, `CancelOrder`,
  `DeliverOrder`, `PlaceOrder`, `ViewOrderDetails`. All signal failure by throwing.
- `CouponRepository.findByCode` returns `Optional` — a result object for the not-found case, already
  inconsistent with the throwing use cases above it.

### Observed defects (facts, not yet decisions)

1. **The catch-all swallows the taxonomy.** `handleGeneralException(Exception.class)` means any
   domain exception type added later silently becomes a 500 with an internal message in the body.
   Nothing at compile time says the handler is incomplete.
2. **Infrastructure failures are inconsistent.** `HttpTaxGateway` throws a dedicated
   `TaxGatewayException` (3 sites); `HttpClockGateway` and `HttpErpGateway` throw bare
   `IllegalStateException` (10 sites) for the same class of failure — non-2xx status, IO failure,
   interrupt.
3. **`NotExistValidationException` inherits from `ValidationException`** but maps to a different
   status (404 vs 422). The subtype relationship says "a not-found *is a* validation error", which
   the HTTP mapping contradicts.
4. **Stale workaround in `PublishCoupon`** (lines 33–36): converts a null `usageLimit` to
   `Integer.MAX_VALUE` although `UsageQuota` now models unlimited as `null`. The comment admits it.
   Independent of the exceptions/results decision — but in the same blast radius.

## Framework established in discussion (input to the decisions, not itself a decision)

The three-tier split, which both the Clean Architecture and DDD positions converge on:

| Tier | Category | Proposed mechanism | Rationale |
|---|---|---|---|
| 1 | Invariant violations inside aggregates & value objects | **Throw** | An aggregate must never exist in an invalid state; constructors cannot return result types; there is no correct handling for a broken invariant |
| 2 | Expected business outcomes at the use case boundary | **Contested** — see OQ1 | Contract visibility and exhaustiveness pay here; but Java lacks `?` and must-use enforcement |
| 3 | Infrastructure failures (DB, network, timeout) | **Throw**, handled at a boundary | Nothing in the domain can act on them |

Tiers 1 and 3 are settled in principle. **Tier 2 is the real question**, and it is where informed
practitioners genuinely differ.

## Open questions — all pending

### OQ1 — Do use cases throw, or return result objects?

- [ ] **Pending.** The headline decision; everything else is downstream of it.

  **For results:** the use case signature is the application's contract in Clean Architecture, and
  hiding failure modes at exactly that layer undercuts the layer's purpose. Controller → use case is
  *one hop*, so the usual "propagate past frames that can't act" argument for exceptions barely
  applies. And a named rejection type is a modelled domain concept in ubiquitous-language terms;
  `ValidationException` is plumbing wearing a domain name.

  **For exceptions:** Java has no `?` operator and no `#[must_use]`, so every call site pays manual
  unwrap ceremony and a dropped result fails *silently* — worse than an unhandled exception. All 7
  use case signatures change, plus the controllers above them, plus the .NET and TypeScript twins
  when they land. Spring, JPA, and the HTTP client all throw regardless, so a second channel exists
  either way.

  **Recommendation: keep exceptions**, and close the real defect (OQ2) instead. The architectural
  complaint that actually bites is the *unenforced* domain↔presentation coupling, and sealing the
  hierarchy fixes that at a fraction of the cost. Result objects would buy exhaustiveness plus
  multi-error aggregation; sealing buys exhaustiveness alone, for roughly 1% of the churn.

  If **results** win instead, this plan is a rewrite, not an edit — say so and re-draft rather than
  bolting result types onto the item list below.

### OQ2 — Seal the domain exception hierarchy?

- [ ] **Pending.** Make `ValidationException` a `sealed` class (Java 17+) permitting its known
      subtypes, so `GlobalExceptionHandler` can be checked for coverage rather than trusted.

  **Recommendation: yes**, regardless of how OQ1 resolves. This is the concrete fix for defect 1.
  Blocked on OQ3, since sealing forces the question of what the permitted set actually is.

  Open sub-question: sealing alone does not make the *handler* exhaustive — `@ExceptionHandler`
  dispatch is runtime, not compile-time. Decide whether to (a) accept sealing as documentation-plus-
  discipline, (b) add a `switch` over the sealed type inside a single handler method so the compiler
  enforces coverage, or (c) add an ArchUnit / test-level check. **Leaning (b)** — it is the only
  option that makes the compiler the enforcement mechanism, which was the entire point.

### OQ3 — What is the domain exception taxonomy?

- [ ] **Pending.** Today: `ValidationException` + `NotExistValidationException`, with the naming and
      inheritance problems in defect 3.

  Needs a decision on: whether not-found is a sibling rather than a subtype (it maps to a different
  status, so the *is-a* is false); whether the `fieldName`-present and `fieldName`-absent branches of
  `handleValidationException` are really two different concepts that deserve two types; and whether
  business-rule rejections (`CouponExpired`, `OrderAlreadyCancelled`, …) get their own types or stay
  as `ValidationException` with a message.

  **Recommendation: make not-found a sibling** (`NotFoundException`, not extending
  `ValidationException`) and split field-scoped from request-scoped validation. Per-rule types are a
  bigger question — defer to OQ4.

### OQ4 — Per-rule exception types, or messages?

- [ ] **Pending.** A `CouponExpiredException` is more DDD (the failure is a named domain concept the
      business would recognise) but multiplies types; `ValidationException("coupon has expired")`
      keeps the taxonomy small but makes the failure a string.

  **Recommendation: messages for now.** The ubiquitous-language argument is real but the template's
  job is to teach the *layering*, not to maximise type count; and this is reversible later in a way
  that OQ1 is not. Revisit if a caller ever needs to branch on a specific rule.

### OQ5 — Infrastructure failure policy

- [ ] **Pending.** Resolve defect 2: `TaxGatewayException` vs bare `IllegalStateException` for the
      same failure class across three gateways.

  **Recommendation: one policy, applied to all three** — either every gateway gets a dedicated
  exception type or none does. Leaning **dedicated types per gateway** (`ClockGatewayException`,
  `ErpGatewayException`, alongside the existing `TaxGatewayException`), since `IllegalStateException`
  is Java's programmer-error signal and using it for a network timeout conflates tier 3 with tier 1 —
  the exact confusion this plan exists to remove.

  Also needs: what status these map to (503? 502? currently they all fall into the 500 catch-all),
  and whether that is a **behaviour change visible to the system tests**.

### OQ6 — Does the catch-all handler stay?

- [ ] **Pending.** `@ExceptionHandler(Exception.class)` → 500 is what makes defect 1 silent, but
      removing it means an unmapped exception escapes to the container's default error page.

  Note it currently puts `ex.getMessage()` **and the root cause message** into the response body.
  Decide whether that is acceptable for a teaching template (it leaks internals; it may also be what
  a system test asserts on — check before changing).

  **Recommendation: keep it, but make it loud** — keep the 500 mapping as a backstop, drop the
  internal detail from the body, and rely on OQ2's compiler check to make "unmapped" a build failure
  rather than a runtime surprise.

### OQ7 — Does `Guard` stay as-is?

- [ ] **Pending.** `Guard` throws `IllegalArgumentException` for programmer errors, which matches
      tier 1 and the discussion framework. The question is only whether tier-1 signalling should use
      a domain-owned type instead of the JDK type, for symmetry with tiers 2 and 3.

  **Recommendation: leave it.** `IllegalArgumentException` is the JDK's idiomatic
  programmer-error signal and using it keeps tier 1 visibly *different* from the domain's own
  exception types — which is the distinction the whole policy is built on.

### OQ8 — Cross-language parity

- [ ] **Pending.** Whatever is decided must state its .NET and TypeScript equivalents before
      `backend-clean-dotnet` / `backend-clean-typescript` are created, and land in
      `docs/atdd/code/language-equivalents.md`.

  Known asymmetries to resolve: C# has no `sealed` hierarchy with exhaustiveness over exception types
  (its `sealed` means something else); TypeScript has no exception type hierarchy worth checking at
  compile time and is the one language where result objects are genuinely idiomatic. **A decision
  that only works in Java is not a decision** — see the `feedback_component_pact_layer_opt_in`
  precedent on keeping the three implementations honest twins.

### OQ9 — Is this in scope for the current refactor at all? ✅ Resolved

- [x] **Resolved 2026-08-17 by events.** The recommendation was "land the current refactor first",
      and it has landed: the surrogate-key removal from `Coupon` / `Order` plus the adapter and
      mapper changes were committed in `1b3aafc1`, with `./gradlew build` green. The working tree is
      clean, so an error-model change now starts from a committed baseline and any system-test
      failure it causes is unambiguously its own.

## Items

**Deliberately empty.** Populate only after OQ1, OQ2, OQ3, and OQ5 are resolved — the item list is a
different shape depending on how OQ1 goes, and writing it now would presume the answer.

Two things are worth capturing regardless of the outcome:

- The stale `Integer.MAX_VALUE` workaround in `PublishCoupon` (defect 4) is independent of every
  question above and can be fixed on its own.
- Any change to gateway failure mapping (OQ5) or the 500 body (OQ6) is **behaviour-affecting** and
  requires a system-test run before commit. Per `feedback_ask_before_local_system_tests`: ask the
  user; never self-initiate.

## Non-goals

- **The legacy `backend-java` / `backend-dotnet` / `backend-typescript` projects.** This is about the
  clean variant only. Whether the legacy twins follow is a separate question and probably "no" —
  they are the before-picture.
- **Bean Validation / `MethodArgumentNotValid` handling.** The DTO-annotation path in
  `GlobalExceptionHandler` is a presentation concern and orthogonal to the domain error model.
- **The `TypeValidationMessageExtractor` / Jackson class-name regex machinery.** Ugly, but a
  different problem.
