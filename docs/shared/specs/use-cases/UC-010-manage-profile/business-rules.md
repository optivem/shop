# Business Rules: Manage Profile

Use case: [UC-010 Manage Profile](use-case-narrative.md). Acceptance criteria: [acceptance-criteria.md](acceptance-criteria.md).

- BR-1. A profile has a first name, last name, email and a shipping address of street, postal code, city and country. The email comes from the sign-in account and can't be changed here. After saving, the profile page shows the saved values.
- BR-2. First name, last name, street, postal code, city and country are all required. A field that holds only spaces counts as empty.
- BR-3. The country must be one of the supported shipping countries: US, DE or GB.
- BR-4. Tax-country hint: the profile page tells the customer that sales tax is charged at the rate of the shipping address's country (UC-009 Check Out, BR-2).
- BR-5. Each customer has one profile, which only they can see and change. The saved shipping address is preselected at checkout (UC-009), and its country prefills the Quick order country (UC-001).
