---
id: UC-011
title: View All Orders
status: planned
---

# UC-011 View All Orders — Acceptance criteria

Narrative: [use-case.md](use-case.md). Business rules: [business-rules.md](business-rules.md).

Tags: `@UC-011.N` for N = 1 to 5 checks business rule BR-N. Tags from `@UC-011.6` on check an extension, which is also tagged `@narrative-<step><letter>`.

```gherkin
Feature: View All Orders

Background:
  Given these orders have been placed:
    | order number | customer | order date       | sku        | quantity | status    |
    | ORD-1001     | alice    | 2026-11-01 09:00 | HP-15      | 1        | PLACED    |
    | ORD-1002     | bob      | 2026-11-02 09:00 | DELL-XPS   | 2        | PLACED    |
    | ORD-1003     | alice    | 2026-11-03 09:00 | LOGI-MX3S  | 1        | CANCELLED |
    | ORD-1013     | carol    | 2026-11-04 09:00 | LG-27UL500 | 1        | DELIVERED |

@UC-011.1
Scenario: Should show the admin every customer's orders, newest first
  Given the user is logged in as an admin
  When the admin views all orders
  Then the order list shows orders ORD-1013, ORD-1003, ORD-1002, ORD-1001

@UC-011.1
Scenario: Should show the details of each order in the list
  Given the user is logged in as an admin
  When the admin views all orders
  Then order ORD-1002 shows customer "bob", SKU DELL-XPS, quantity 2 and status PLACED
  And order ORD-1002 links to its details

@UC-011.1
Scenario: Should let the admin open another customer's order
  Given the user is logged in as an admin
  When the admin views all orders
  And the admin opens the details of order ORD-1002
  Then the order details show order ORD-1002 with SKU DELL-XPS

@UC-011.2
Scenario: Should show "-" as the customer of an order with no recorded customer
  Given order ORD-0999 was placed before the shop recorded who placed orders
  And the user is logged in as an admin
  When the admin views all orders
  Then order ORD-0999 shows customer "-"

@UC-011.3
Scenario: Should offer the customers who have placed orders
  Given the user is logged in as an admin
  When the admin views all orders
  Then the customer filter offers All customers, alice, bob and carol
  And the customer filter is set to All customers

@UC-011.3
Scenario: Should show only the orders of the chosen customer
  Given the user is logged in as an admin
  When the admin filters all orders by customer alice
  Then the order list shows orders ORD-1003, ORD-1001

@UC-011.4
Scenario: Should show only the chosen customer's orders that match the order number
  Given the user is logged in as an admin
  When the admin filters all orders by customer alice and order number "3"
  Then the order list shows orders ORD-1003

@UC-011.5
Scenario: Should show a customer only their own orders
  Given the user is logged in as customer "alice"
  When the customer views their orders
  Then the order list shows orders ORD-1003, ORD-1001
  And the order list does not show orders ORD-1002, ORD-1013

@UC-011.5
Scenario: Should not let a customer see another customer's orders through the customer filter
  Given the user is logged in as customer "alice"
  When the customer asks for the orders of customer bob
  Then the order list shows orders ORD-1003, ORD-1001
  And the order list does not show orders ORD-1002

@UC-011.5
Scenario: Should not show an order with no recorded customer to any customer
  Given order ORD-0999 was placed before the shop recorded who placed orders
  And the user is logged in as customer "alice"
  When the customer views their orders
  Then the order list does not show orders ORD-0999

@UC-011.6 @narrative-2a
Scenario: Should ask an anonymous visitor to sign in
  Given the visitor is not signed in
  When the visitor opens all orders
  Then the visitor is asked to sign in

@UC-011.7 @narrative-2b
Scenario: Should not show a customer the customer column or the customer filter
  Given the user is logged in as customer "alice"
  When the customer views their orders
  Then the order list does not show the customer of each order
  And the customer is not offered a customer filter

@UC-011.8 @narrative-5a
Scenario: Should tell the admin when no order matches the filters
  Given the user is logged in as an admin
  When the admin filters all orders by customer bob and order number "1013"
  Then the order list shows no orders
  And the admin is told "No orders found"

@UC-011.8 @narrative-5a
Scenario: Should show every order again after resetting the filters
  Given the admin has filtered all orders by customer bob and order number "1013"
  When the admin sets the customer filter to All customers and clears the order number
  Then the order list shows orders ORD-1013, ORD-1003, ORD-1002, ORD-1001
