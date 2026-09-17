# 2026-09-17 18:11:19 UTC — Cross-language parity fixes (Java, .NET, TypeScript)

## TL;DR

**Why:** The TypeScript best-practice plan (shop `a20bd626`…`ad4a0070`) fixed defects that most likely also exist in the Java/.NET twins, and deliberately left out three changes that only make sense when all languages move together.
**End result:** Java, .NET and TypeScript share the same fixes for the money round-trip, parse-not-cast input validation and the coupon usage race, and all move together on the money wire format, generic 500 details and the blackout message.

## ▶ Next executable step (resume here)

Design work remains: run `/refine-plan plans/20260917-1811-cross-language-parity-fixes.md` to audit the Java/.NET twins and turn the items below into concrete steps.

## Steps

- [ ] **Audit the Java/.NET twins** for the same defects as TypeScript Steps 4, 5 and 7 (money round-trip, cast-instead-of-parse input validation, coupon usage race) and fix them in all three languages.
- [ ] **Money wire format → strings** (`"20.00"`), the recorded string-canonical direction. It changes the API contract, so every backend, all three system-test suites, Pact contracts and frontend types move together in one coordinated change.
- [ ] **Don't leak internal error messages in 500 responses.** All six backends (monolith + multitier, Java/.NET/TypeScript) return `detail: "Internal server error: <exception message>"`. Log the exception server-side and return a generic detail in all of them together (moved from TypeScript Step 15, 2026-09-17, so the TypeScript monolith doesn't diverge from the other five).
- [ ] **Blackout message fix.** Every implementation (all Java/.NET/TS backends, backend-clean-java `YearEndBlackoutPolicy`) enforces 22:00–22:30 inclusive, and every test's boundary data agrees (22:30:00 blocked, 22:30:01 allowed), but the error message everywhere says "between 22:00 and 23:00". Behavior is the consistent spec; change the message to "between 22:00 and 22:30" in all backends, system tests (3 languages), component tests and `frontend-react/src/test/interactions/order.interactions.ts:140`.
