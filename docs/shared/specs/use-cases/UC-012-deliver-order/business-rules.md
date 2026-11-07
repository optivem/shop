# Business Rules: Deliver Order

Use case: [UC-012 Deliver Order](use-case-narrative.md). Acceptance criteria: [acceptance-criteria.md](acceptance-criteria.md).

- BR-1. Delivering an order sets its status to DELIVERED. Nothing else about the order changes.
- BR-2. Only a PLACED order can be delivered. A cancelled or already delivered order cannot be. The order details offer Deliver Order only while the order is PLACED; once it is delivered, neither Deliver Order nor Cancel Order is offered.
- BR-3. Only an admin can deliver an order, and an admin can deliver any customer's order.
