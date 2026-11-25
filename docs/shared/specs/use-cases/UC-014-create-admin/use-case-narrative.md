---
id: UC-014
title: Create Admin
---

# UC-014 Create Admin

**Primary actor:** Admin
**Secondary actors:** none (the sign-in account lives in the identity provider, Keycloak)

## Main Success Scenario
1. The admin opens the identity provider's user management.
2. The admin creates a sign-in account for the new admin: username, email, first name, last name and a password.
3. The admin gives the account the admin role, and takes away the customer role that every new account gets (UC-013, BR-2).
4. The new admin signs in and can use the admin pages (UC-004, UC-005, UC-011, UC-012).

## Extensions
- 2a. The username or email is already taken:
    - The identity provider says so. No account is created. Resume at step 2.

## Notes
- The shop has no page for this: admins are managed in the identity provider. The system tests create a fresh admin per scenario through the identity provider's admin API.

## Acceptance criteria

See [acceptance-criteria.md](acceptance-criteria.md).
