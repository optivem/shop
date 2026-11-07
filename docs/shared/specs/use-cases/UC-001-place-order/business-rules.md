# Business Rules: Place Order

Use case: [UC-001 Place Order](use-case.md). Acceptance criteria: [acceptance-criteria.md](acceptance-criteria.md).

- BR-1. Base price = unit price x quantity.
- BR-2. Subtotal = base price - discount. With no coupon, the discount is 0.
- BR-3. Tax = subtotal x the tax rate for the country.
- BR-4. Total price = subtotal + tax.
