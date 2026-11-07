---
id: UC-011
title: View All Orders
---

# UC-011 View All Orders

**Primary actor:** Admin
**Secondary actors:** none

## Main Success Scenario
1. The admin opens All orders from the admin area (Admin › All orders). It is the same order list that customers see as My orders.
2. The system checks that the user is signed in and is an admin ([BR-5](business-rules.md)).
3. The system shows every customer's orders, newest first. Each order shows the customer who placed it. The customer filter is set to "All customers" ([BR-1, BR-2](business-rules.md)).
4. The admin narrows the list: picks one customer and/or enters part of an order number.
5. The system shows only the orders that match every chosen filter ([BR-3, BR-4](business-rules.md)).
6. The admin opens an order's details (UC-002 View Order), for example to deliver it (UC-012 Deliver Order).

Steps 4 and 5 can repeat, in any order, until the admin finds the order.

## Extensions
- 2a. The user is not signed in:
    - The system asks the user to sign in. The use case ends.
- 2b. The user is a customer, not an admin:
    - The system shows only the customer's own orders, without the customer of each order and without the customer filter ([BR-5](business-rules.md)). The use case continues as UC-002 View Order.
- 5a. No order matches the chosen filters:
    - The system tells the admin no orders were found.
    - The admin sets the customer filter back to "All customers" and clears the order number. Resume at step 3.

## Acceptance criteria

See [acceptance-criteria.md](acceptance-criteria.md).
