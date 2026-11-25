# Business Rules: Create Customer

Use case: [UC-013 Create Customer](use-case-narrative.md). Acceptance criteria: [acceptance-criteria.md](acceptance-criteria.md).

- BR-1. Signing up needs a username, a password (entered twice), an email, a first name, a last name and a shipping country. The shipping country must be one of the supported shipping countries: US, DE or GB (UC-010 Manage Profile, BR-3).
- BR-2. A new account is a customer: it can place orders (UC-001) and never has the admin role. Admins are created by an admin (UC-014).
- BR-3. Until the customer saves a profile (UC-010), the profile shows the name and the shipping country from the sign-up, with an empty street address, and that country prefills the Quick order country (UC-001).
