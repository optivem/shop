# Business Rules: View All Orders

Use case: [UC-011 View All Orders](use-case.md). Acceptance criteria: [acceptance-criteria.md](acceptance-criteria.md).

- BR-1. An admin sees every order placed in the shop, whichever customer placed it, newest first (by order date). Each order shows its order number, customer, order date, SKU, quantity, total price and status, and links to its details. The admin can open the details of any customer's order.
- BR-2. The customer of an order is the username of the customer who placed it. Orders placed before the shop recorded who placed them show "-" as the customer.
- BR-3. Customer filter: the admin picks one customer, or "All customers" (the default). With a customer picked, only that customer's orders are shown. The customers offered are the ones who have placed orders.
- BR-4. Filters combine: with a customer picked and part of an order number entered, only that customer's orders whose order number contains the entered text are shown, ignoring case.
- BR-5. Customers never see other customers' orders. A customer's order list holds only the orders they placed, whatever filter they send, and never shows the customer column or the customer filter. Orders with no recorded customer appear to no customer. Only an admin sees every customer's orders.
