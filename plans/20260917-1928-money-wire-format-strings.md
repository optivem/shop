# 2026-09-17 19:28:40 UTC — Money on the wire becomes strings (all languages)

**Origin:** Split out of `plans/20260917-1811-cross-language-parity-fixes.md` on 2026-09-17. That plan
bundled this with three small, verified defect fixes; the audit showed this item is a breaking API
contract change of a completely different size and risk, so it was separated rather than holding the
small fixes hostage to it.

## TL;DR

**Why:** Money is currently a bare JSON number on the wire in all six backends. The recorded
cross-language direction is string-canonical (`"20.00"`), because TypeScript has no native exact
decimal and a decimal dependency on the wire contract would push cost onto cloning students. Today's
fixed-looking 2dp/4dp output is *incidental* — it falls out of `BigDecimal`/`decimal` scale surviving
the DB round trip, and nothing enforces it.

**End result:** Money and rate fields are emitted as JSON strings with explicit scale by every
backend, and every consumer (three system-test suites, the Pact contract, frontend types) reads them
as strings.

## Audit facts carried over (2026-09-17)

- **No formatter exists to repurpose in Java or .NET.** Unlike TypeScript
  (`decimal-format.interceptor.ts`, `numeric.transformer.ts`), the five Java/.NET backends have zero
  JSON customization for decimals — `monolith/java`, `backend-java` and `backend-clean-java` use
  default Jackson `BigDecimal` serialization; `monolith/dotnet` and `backend-dotnet` register only
  `JsonStringEnumConverter`. This change needs a **new Jackson serializer** and a **new
  `System.Text.Json` `JsonConverter<decimal>`** written from scratch.
- **Field surface is identical in every backend** (shared schema,
  `system/db/migrations/V20260514085249__init.sql`):
  - Order details response — 6 amounts @2dp (`unitPrice`, `basePrice`, `discountAmount`,
    `subtotalPrice`, `taxAmount`, `totalPrice`) + 2 rates @4dp (`discountRate`, `taxRate`).
  - Order-history list item — `totalPrice` only.
  - Coupon browse response — `discountRate`. (`PublishCouponRequest` is **already** `String` on the
    way in and stays that way.)
- **Pact contract records numbers today** — `contracts/frontend-backend.json` carries
  `"totalPrice": 22`, `"discountRate": 0.2`, `"basePrice": 20` etc. with numeric matchers; these move
  with the change.

## Open questions

- [ ] **Do rates become strings too, or only amounts?** Rates (`discountRate`, `taxRate`, scale 4)
      are not currency. `"0.2000"` carries scale the way `"20.00"` does, but a rate has no
      round-half-up money semantics and `0.2` is not lossy at realistic precision. Options: all
      decimal fields become strings (uniform, one rule), or amounts-only (narrower blast radius,
      two rules to remember).
- [ ] **Is this coordinated with `plans/20260722-1216-string-only-money-surface.md`?** That plan
      decides whether the *test DSL* keeps both `String` and numeric surfaces or goes String-only,
      and is itself blocked on an unresolved "is the breaking change acceptable" question. A
      string wire format makes the DSL question more pressing, since assertions would be comparing
      against strings that came off the wire as strings.
- [ ] **One commit or staged per language?** The contract is shared, so a backend that has moved and
      a system-test suite that has not will fail. Either all six backends + all consumers move in one
      commit, or a transition window is engineered (e.g. accept both forms on read first).

## Steps

*(To be filled in once the open questions above are resolved — run `/refine-plan` on this file.)*

- [ ] Decide the questions above.
- [ ] Java: Jackson serializer for `BigDecimal` money/rate fields, applied in `monolith/java`,
      `backend-java`, `backend-clean-java`.
- [ ] .NET: `JsonConverter<decimal>` registered in `monolith/dotnet` and `backend-dotnet`.
- [ ] TypeScript: adapt `decimal-format.interceptor.ts` to emit quoted strings (it currently uses a
      sentinel-quote/regex trick specifically to *strip* the quotes and produce an unquoted number).
- [ ] Update `contracts/frontend-backend.json` (bodies + matchers).
- [ ] Update all three system-test suites' money assertions and their verification layers.
- [ ] Update `frontend-react` types and any parsing/formatting that assumes numbers.
- [ ] Full verification: `./compile-all.sh`, then system tests across all three languages. **Ask
      before running system tests locally.**
