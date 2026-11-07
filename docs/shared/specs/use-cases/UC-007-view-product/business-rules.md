# Business Rules: View Product

Use case: [UC-007 View Product](use-case-narrative.md). Acceptance criteria: [acceptance-criteria.md](acceptance-criteria.md).

- BR-1. The product page shows the product's title, brand, category, SKU, price and description, and a breadcrumb trail: Home › category › title. Prices are in dollars with thousands separators and 2 decimals (e.g. $1,299.99).
- BR-2. Quantity: a whole number from 1 to 99, starting at 1. The stepper raises or lowers it by 1 and never goes below 1 or above 99.
- BR-3. Adding to the cart adds the chosen quantity of the product, by the cart rules of UC-008 Manage Cart, and confirms "Added to cart". The visitor stays on the product page.
- BR-4. Viewing a product needs no sign-in. Each product has its own page address, so it can be bookmarked or shared. A signed-in customer sees the same product page as an anonymous visitor.
