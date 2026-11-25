---
id: UC-013
title: Create Customer
---

# UC-013 Create Customer

**Primary actor:** Visitor
**Secondary actors:** none (the sign-in account lives in the identity provider, Keycloak)

## Main Success Scenario
1. The visitor picks "Sign up" in the header.
2. The system shows the sign-up form: username, password, email, first name, last name and shipping country ([BR-1](business-rules.md)).
3. The visitor fills in the form and submits it.
4. The system creates the visitor's sign-in account as a customer ([BR-2](business-rules.md)) and signs the new customer in.
5. The system returns the customer to the page they came from. Until they save a profile, their profile and Quick order use the shipping country they chose ([BR-3](business-rules.md)), so they can place an order straight away.

## Extensions
- 3a. A field is missing or invalid, the passwords don't match, or the username or email is already taken:
    - The system shows which fields to correct. No account is created. Resume at step 3.

## Notes
- The sign-up form is the identity provider's registration page; the shop links to it and gets the new customer back signed in. Sign-up is off in production for now (the production identity provider is shared).

## Acceptance criteria

See [acceptance-criteria.md](acceptance-criteria.md).
