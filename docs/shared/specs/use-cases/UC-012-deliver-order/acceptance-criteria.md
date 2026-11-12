---
id: UC-012
title: Deliver Order
status: planned
---

# UC-012 Deliver Order — Acceptance criteria

Narrative: [use-case-narrative.md](use-case-narrative.md). Business rules: [business-rules.md](business-rules.md).

Tags: `@UC-012.N` for N = 1 to 3 checks business rule BR-N. Tags from `@UC-012.4` on check an extension, which is also tagged `@narrative-<step><letter>`.

```gherkin
Feature: Deliver Order

Background:
  Given a customer has placed an order with status PLACED
  And the user is logged in as an admin

@UC-012.1
Scenario: Should have delivered status when delivered
  When the admin delivers the order
  Then the order is delivered successfully
  And the admin is told "Order has been delivered successfully"
  And the order status is DELIVERED

@UC-012.2
Scenario: Should offer to deliver a placed order
  When the admin opens the details of the order
  Then the order details offer Deliver Order

@UC-012.2
Scenario: Should offer neither deliver nor cancel once the order is delivered
  Given the admin has delivered the order
  When the admin opens the details of the order
  Then the order details do not offer Deliver Order
  And the order details do not offer Cancel Order

@UC-012.3
Scenario: Admin should be able to deliver any customer's order
  Given customer B has placed order ORD-2001 with status PLACED
  When the admin delivers order ORD-2001
  Then the order is delivered successfully
  And order ORD-2001 has status DELIVERED

@UC-012.3
Scenario: Customer should not be offered Deliver Order
  Given the user is logged in as the customer who placed the order
  When the user opens the details of the order
  Then the order details do not offer Deliver Order

@UC-012.4 @narrative-2a
Scenario: Customer should not be able to deliver an order
  Given the user is logged in as the customer who placed the order
  When the user delivers the order
  Then the delivery is rejected
  And the user is told "You do not have permission to perform this action"
  And the order status is PLACED

@UC-012.5 @narrative-3a
Scenario: Should not deliver an order that does not exist
  When the admin delivers order NON-EXISTENT-ORDER-99999
  Then the delivery is rejected
  And the admin is told "Order NON-EXISTENT-ORDER-99999 does not exist."

@UC-012.6 @narrative-4a
Scenario Outline: Should not deliver an order that is not placed
  Given the order has status <status>
  When the admin delivers the order
  Then the delivery is rejected
  And the admin is told "Order cannot be delivered in its current status"
  And the order status is <status>

  Examples:
    | status    |
    | CANCELLED |
    | DELIVERED |
```
