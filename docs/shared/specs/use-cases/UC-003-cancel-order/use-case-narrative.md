---
id: UC-003
title: Cancel Order
---

# UC-003 Cancel Order

**Primary actor:** Customer (an Admin can cancel any customer's order the same way)
**Secondary actors:** Clock

## Main Success Scenario
1. The customer opens the details of one of their orders (UC-002 View Order) and chooses Cancel Order. The option is offered only while the order is placed ([BR-2](business-rules.md)).
2. The system checks with the Clock that cancellations are being accepted at this time ([BR-3](business-rules.md)).
3. The system finds the order among the orders the user may cancel ([BR-4](business-rules.md)).
4. The system checks that the order has not already been cancelled ([BR-2](business-rules.md)).
5. The system sets the order's status to CANCELLED ([BR-1](business-rules.md)).
6. The system tells the customer the order has been cancelled and shows the order with status CANCELLED, without the Cancel Order option.

## Extensions
- 2a. It is December 31st between 22:00 and 22:30 (UTC):
    - The system tells the customer cancellation is not allowed at this time. The order keeps its status. The use case ends.
- 3a. The order does not exist, or it belongs to another customer:
    - The system tells the customer the order does not exist. The use case ends.
- 4a. The order has already been cancelled:
    - The system tells the customer the order has already been cancelled. The use case ends.

## Acceptance criteria

See [acceptance-criteria.md](acceptance-criteria.md).
