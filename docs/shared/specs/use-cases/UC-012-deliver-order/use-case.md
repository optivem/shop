---
id: UC-012
title: Deliver Order
---

# UC-012 Deliver Order

**Primary actor:** Admin
**Secondary actors:** none

## Main Success Scenario
1. The admin opens the details of a customer's order (from UC-011 View All Orders) and chooses Deliver Order. The option is offered only while the order is placed ([BR-2](business-rules.md)).
2. The system checks that the user is an admin ([BR-3](business-rules.md)).
3. The system finds the order.
4. The system checks that the order is placed ([BR-2](business-rules.md)).
5. The system sets the order's status to DELIVERED ([BR-1](business-rules.md)).
6. The system tells the admin the order has been delivered and shows the order with status DELIVERED, without the Deliver Order and Cancel Order options.

## Extensions
- 2a. The user is a customer:
    - The system tells the user they don't have permission. The use case ends.
- 3a. The order does not exist:
    - The system tells the admin the order does not exist. The use case ends.
- 4a. The order is not placed (it is already cancelled or delivered):
    - The system tells the admin the order cannot be delivered in its current status. The use case ends.

## Notes
- `backend-clean-java` also has an admin-only bulk variant, `POST /api/admin/orders/sweep-deliveries?olderThanDays=N`. It delivers every PLACED order placed before the Clock's current time minus N days (default 7; N must be at least 1, otherwise "olderThanDays must be at least 1") and answers with the cutoff time and the number of orders delivered. It has no UI and no system test, and the other backends don't have it, so it is not part of this use case's acceptance criteria.

## Acceptance criteria

See [acceptance-criteria.md](acceptance-criteria.md).
