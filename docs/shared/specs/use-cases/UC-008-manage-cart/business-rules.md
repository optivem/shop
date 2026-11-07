# Business Rules: Manage Cart

Use case: [UC-008 Manage Cart](use-case-narrative.md). Acceptance criteria: [acceptance-criteria.md](acceptance-criteria.md).

- BR-1. The cart badge in the header, shown on every page, shows the cart's total quantity (the sum of all line quantities). It is hidden when the cart is empty.
- BR-2. Adding a product that is already in the cart raises the quantity of its line instead of adding a second line. Adding from the catalog adds quantity 1; adding from the product page adds the chosen quantity. Every add confirms "Added to cart".
- BR-3. A line's quantity is a whole number from 1 to 99. An add that would take a line above 99 sets it to 99.
- BR-4. Each cart line shows the product's title, unit price, quantity and line total (unit price × quantity). Lines are listed in the order their products were first added. The cart shows the number of items, which is the number of lines.
- BR-5. The subtotal is the sum of the line totals. Tax and discounts are calculated at checkout, not in the cart. Unit prices are the current catalog prices, read again every time the cart is shown; the cart keeps only products and quantities, and the order is priced when it is placed. Prices are in dollars with thousands separators and 2 decimals (e.g. $1,299.99).
- BR-6. The visitor can change a line's quantity by typing it or with the stepper, which raises or lowers it by 1 within 1 to 99. The line total, subtotal and cart badge follow the new quantity.
- BR-7. Removing a line takes the product out of the cart entirely. The subtotal and cart badge follow.
- BR-8. Keeping a cart needs no sign-in. The cart stays with the visitor's browser across pages and visits, and it is still there, unchanged, after the visitor signs in.
