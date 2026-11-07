# Business Rules: Cancel Order

Use case: [UC-003 Cancel Order](use-case.md). Acceptance criteria: [acceptance-criteria.md](acceptance-criteria.md).

- BR-1. Cancelling an order sets its status to CANCELLED. Nothing else about the order changes.
- BR-2. An order that is already cancelled cannot be cancelled again. The order details offer Cancel Order only while the order is PLACED; a cancelled or delivered order shows no Cancel Order option.
- BR-3. Year-end blackout: no order can be cancelled on December 31st from 22:00:00 to 22:30:00 UTC, both ends included. The current time comes from the Clock. Outside that window (any other time on December 31st, or any other day) cancelling is allowed.
- BR-4. A customer can cancel only their own orders. Another customer's order is answered as if it did not exist. An admin can cancel any customer's order.
