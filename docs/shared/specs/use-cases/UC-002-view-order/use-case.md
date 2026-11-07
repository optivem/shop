---
id: UC-002
title: View Order
---

# UC-002 View Order

**Primary actor:** Customer
**Secondary actors:** none

## Main Success Scenario
1. The customer opens My orders. Signing in is required.
2. The system shows the customer's own orders, newest first, one page at a time ([BR-1](business-rules.md), [BR-3](business-rules.md), [BR-6](business-rules.md)).
3. The customer types part of an order number to narrow the list.
4. The system shows only the customer's orders whose number contains that text ([BR-2](business-rules.md)).
5. The customer opens one of the orders.
6. The system shows the order details: the product, quantity, country, every step of the price, the applied coupon and the status ([BR-4](business-rules.md), [BR-5](business-rules.md)).
7. From the order details the customer can cancel the order (UC-003 Cancel Order) or go back to My orders.

Steps 3 and 4 are optional and can repeat.

## Extensions
- 1a. The user is not signed in:
    - The system tells the user authentication is required. The use case ends.
- 2a. The requested page is below 1, or the page size is not between 1 and 100:
    - The system rejects the request and says which value is wrong. Resume at step 1.
- 4a. No order of the customer matches:
    - The system tells the customer no orders were found. Resume at step 3.
- 5a. The order does not exist, or it belongs to another customer:
    - The system tells the customer the order does not exist. The use case ends.

## Acceptance criteria

See [acceptance-criteria.md](acceptance-criteria.md).
