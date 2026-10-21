# Tax country from the customer's location, not the place-order request

**Source:** substack draft `articles/drafts/PAID-AI-characterization-tests-legacy.md`
(Place order narrative: "The system gets the tax rate for the customer's country from the
Taxation System"). The article treats the country as where the customer is located; the
customer does not pick it per order.
**Status:** Active — **not yet approved to build. Resolve the open questions before executing.**

## Context

Today every backend takes the country from the request:

- `PlaceOrderRequest.country` is required (`@NotBlank "Country must not be empty"`) in
  monolith java/dotnet/typescript and multitier backend-java/backend-clean-java/
  backend-dotnet/backend-typescript.
- `OrderService.placeOrder` passes `request.getCountry()` to `TaxGateway.getTaxDetails(country)`
  and stores it on `Order`.
- `frontend-react` sends it from the order form.
- system-test: ~179 files reference country (DSL `withCountry(...)`, validation scenarios
  such as "Country must not be empty" / "Country does not exist").

There is no customer profile in the shop. The caller is resolved from the Keycloak JWT / OIDC
session (`CurrentUserResolver` → `CurrentUser(subject, username, admin)`), which carries no
country.

## Open questions

1. **Where does the customer's country live?**
   - **(Recommended)** A Keycloak user attribute exposed as a `country` claim, read by
     `CurrentUserResolver` into `CurrentUser`. No new table, and the identity provider already
     owns customer data.
   - A `customer_profile` table in the shop DB, keyed by the JWT subject.
2. **What if the customer has no country?** Recommended: reject the order with a validation
   error ("Customer country is not set"), replacing "Country must not be empty".
3. **Keep `country` on `Order`?** Recommended: yes — store the country used for tax at order
   time, so order history stays correct if the customer later moves.
4. **Scope of the first pass.** Recommended: one language end to end first
   (monolith java + system-test java), then port, matching how other cross-language changes
   in `plans/` are staged.

## Items

1. Add the country source chosen in Q1 (Keycloak realm config for test users in `docker/`,
   or the profile table + migration in `system/db/`).
2. Extend `CurrentUser` with `country`; populate it in `CurrentUserResolver`.
3. Remove `country` from `PlaceOrderRequest`; `OrderService.placeOrder` takes the country from
   the current user.
4. Remove the country field from the `frontend-react` order form; update
   `contracts/frontend-backend.json`.
5. system-test: replace per-order `withCountry(...)` with a customer-level setup
   (e.g. `given().customer().withCountry("US")`); rework the "Country must not be empty"
   scenarios into the Q2 behaviour; keep "Country does not exist" (now about the customer's
   country).
6. Port items 2–5 to the remaining backends (monolith dotnet/typescript, multitier
   backend-java/backend-clean-java/backend-dotnet/backend-typescript).
7. Update the substack characterization tests draft if the component test snippet changes
   (`placeOrder("BOOK-123", 2, "US", "SAVE20")` currently passes the country).
