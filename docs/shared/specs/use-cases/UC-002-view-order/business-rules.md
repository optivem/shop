# Business Rules: View Order

Use case: [UC-002 View Order](use-case.md). Acceptance criteria: [acceptance-criteria.md](acceptance-criteria.md).

- BR-1. My orders lists the customer's orders, newest first by the time they were placed. Each order shows its order number, order date, SKU, quantity, total price and status, and links to its details.
- BR-2. Order number filter: an order matches if the typed text appears anywhere in its order number, ignoring case and surrounding spaces. An empty filter matches every order.
- BR-3. The list is read one page at a time. Pages are numbered from 1. Without a page the first page is shown; without a page size, 50 orders per page. The page size is at most 100. The result reports the total number of matching orders. A page past the last one is empty, not an error.
- BR-4. Order details show the order number, order timestamp (UTC), status, SKU, country, quantity, unit price, base price, discount rate, discount amount, subtotal, tax rate, tax amount, total price and applied coupon ("None" when no coupon was used). Prices are in dollars with 2 decimals; rates are shown as percentages with 2 decimals (e.g. 10.00%).
- BR-5. An order's status is PLACED, CANCELLED or DELIVERED. A new order is PLACED; it becomes CANCELLED through UC-003 Cancel Order and DELIVERED through UC-012 Deliver Order.
- BR-6. A customer sees only their own orders. Another customer's order is not listed, and opening it is answered as if it did not exist, so its existence is not revealed. (An admin sees every customer's orders: UC-011 View All Orders.)
