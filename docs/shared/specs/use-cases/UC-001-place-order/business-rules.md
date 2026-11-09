# Business Rules: Place Order

Use case: [UC-001 Place Order](use-case-narrative.md). Acceptance criteria: [acceptance-criteria.md](acceptance-criteria.md).

- BR-1. Base price = unit price x quantity.
- BR-2. Subtotal = base price - discount. With no coupon, the discount is 0.
- BR-3. Tax = subtotal x the tax rate of the shipping address's country. There is no separate country field; on Quick order (extension 1a) the entered country is used instead.
- BR-4. Total price = subtotal + tax.
- BR-5. A shipping address needs a first name, last name, street, postal code, city and country; a field that holds only spaces counts as empty. The country must be a supported shipping country: one the tax service has a rate for (today US, DE and GB, the same list as UC-010 Manage Profile).
- BR-6. The order keeps the shipping address it was placed with, and the order details show it.
