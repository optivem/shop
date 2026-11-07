---
id: UC-003
title: Cancel Order
status: planned
---

# UC-003 Cancel Order — Acceptance criteria

Narrative: [use-case-narrative.md](use-case-narrative.md). Business rules: [business-rules.md](business-rules.md).

Tags: `@UC-003.N` for N = 1 to 4 checks business rule BR-N. Tags from `@UC-003.5` on check an extension, which is also tagged `@narrative-<step><letter>`.

```gherkin
Feature: Cancel Order

Background:
  Given the user is logged in as a customer
  And the customer has placed an order with status PLACED

@UC-003.1
Scenario: Should have cancelled status when cancelled
  When the customer cancels the order
  Then the order is cancelled successfully
  And the customer is told "Order has been cancelled successfully"
  And the order status is CANCELLED

@UC-003.2
Scenario Outline: Should not offer to cancel an order that is no longer placed
  Given the order has status <status>
  When the customer opens the details of the order
  Then the order details do not offer Cancel Order

  Examples:
    | status    |
    | CANCELLED |
    | DELIVERED |

@UC-003.2
Scenario: Should offer to cancel a placed order
  When the customer opens the details of the order
  Then the order details offer Cancel Order

@UC-003.3
Scenario Outline: Should be able to cancel an order outside the year-end blackout
  Given the Clock time is <time>
  When the customer cancels the order
  Then the order is cancelled successfully

  Examples:
    | time                 | note                                      |
    | 2024-12-31T21:59:59Z | 1 second before the blackout starts       |
    | 2024-12-31T22:30:01Z | 1 second after the blackout ends          |
    | 2024-12-31T10:00:00Z | another time on December 31st             |
    | 2025-01-01T22:15:00Z | same time of day, but not December 31st   |

@UC-003.4
Scenario: Admin should be able to cancel a customer's order
  Given the user is logged in as an admin
  When the admin cancels the customer's order
  Then the order is cancelled successfully
  And the order status is CANCELLED

@UC-003.5 @narrative-2a
Scenario Outline: Should not cancel an order on December 31st between 22:00 and 22:30
  Given the Clock time is <time>
  When the customer cancels the order
  Then the cancellation is rejected
  And the customer is told "Order cancellation is not allowed on December 31st between 22:00 and 22:30"
  And the order status is PLACED

  Examples:
    | time                 | note                         |
    | 2024-12-31T22:00:00Z | start of the blackout        |
    | 2026-12-31T22:00:01Z | just after the start         |
    | 2025-12-31T22:15:00Z | middle of the blackout       |
    | 2028-12-31T22:29:59Z | just before the end          |
    | 2021-12-31T22:30:00Z | end of the blackout          |

@UC-003.6 @narrative-3a
Scenario: Should not cancel an order that does not exist
  When the customer cancels order NON-EXISTENT-ORDER-99999
  Then the cancellation is rejected
  And the customer is told "Order NON-EXISTENT-ORDER-99999 does not exist."

@UC-003.6 @narrative-3a
Scenario: Customer should not be able to cancel another customer's order
  Given another customer has placed order ORD-2001
  When the customer cancels order ORD-2001
  Then the cancellation is rejected
  And the customer is told "Order ORD-2001 does not exist."
  And order ORD-2001 has status PLACED

@UC-003.7 @narrative-4a
Scenario: Should not cancel an order that has already been cancelled
  Given the order has status CANCELLED
  When the customer cancels the order
  Then the cancellation is rejected
  And the customer is told "Order has already been cancelled"
```
