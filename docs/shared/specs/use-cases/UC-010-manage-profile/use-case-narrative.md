---
id: UC-010
title: Manage Profile
---

# UC-010 Manage Profile

**Primary actor:** Customer
**Secondary actors:** none (the email and the sign-in name come from the sign-in account)

## Main Success Scenario
1. The customer opens My account › Profile & address.
2. The system shows the customer's profile: first name, last name, email (read-only) and the shipping address (street, postal code, city and country), with a hint that sales tax is charged at the rate of the shipping address's country ([BR-1](business-rules.md), [BR-4](business-rules.md)).
3. The customer changes their name and/or shipping address.
4. The customer saves the profile.
5. The system validates the profile ([BR-2](business-rules.md), [BR-3](business-rules.md)).
6. The system saves the profile and confirms it was saved. From now on the saved address is the customer's default shipping address at checkout, and its country prefills the Quick order country ([BR-5](business-rules.md)).

## Extensions
- 1a. The user is not signed in:
    - The system asks the visitor to sign in. After signing in, the customer comes back to the profile page. Resume at step 2.
- 2a. The customer has no saved profile yet:
    - The system shows an empty profile, with the first and last name prefilled from the sign-in account where known and the email shown read-only. Resume at step 3.
- 5a. A required field is empty, or the country is not supported:
    - The system tells the customer which fields to correct. Nothing is saved, and the previously saved profile is unchanged. Resume at step 3.

## Acceptance criteria

See [acceptance-criteria.md](acceptance-criteria.md).
