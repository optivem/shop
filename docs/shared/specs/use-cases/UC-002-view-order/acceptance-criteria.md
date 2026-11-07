---
id: UC-002
title: View Order
status: planned
---

# UC-002 View Order — Acceptance criteria

Narrative: [use-case-narrative.md](use-case-narrative.md). Business rules: [business-rules.md](business-rules.md).

Tags: `@UC-002.N` for N = 1 to 6 checks business rule BR-N. Tags from `@UC-002.7` on check an extension, which is also tagged `@narrative-<step><letter>`.

```gherkin
Feature: View Order

Background:
  Given these orders exist:
    | order number | customer | placed at            | sku      | quantity | country | total price | status    |
    | ORD-1001     | Alice    | 2026-01-10T09:00:00Z | BOOK-123 | 5        | US      | 99.00       | PLACED    |
    | ORD-1002     | Alice    | 2026-01-12T14:30:00Z | PEN-001  | 2        | US      | 4.40        | DELIVERED |
    | ORD-1003     | Alice    | 2026-01-15T08:00:00Z | MUG-777  | 1        | GB      | 12.00       | CANCELLED |
    | ORD-2001     | Bob      | 2026-01-11T10:00:00Z | BOOK-123 | 1        | US      | 22.00       | PLACED    |
  And the user is logged in as customer Alice

@UC-002.1
Scenario: Should list the customer's orders, newest first
  When the customer opens My orders
  Then My orders shows orders ORD-1003, ORD-1002, ORD-1001

@UC-002.1
Scenario: Should show the summary of each order
  When the customer opens My orders
  Then order ORD-1001 shows order date 2026-01-10T09:00:00Z, SKU "BOOK-123", quantity 5, total price "$99.00" and status PLACED
  And order ORD-1001 links to its details

@UC-002.2
Scenario Outline: Should filter orders by part of the order number, ignoring case
  When the customer filters My orders by order number "<filter>"
  Then My orders shows orders <orders>

  Examples:
    | filter  | orders                       |
    | 1002    | ORD-1002                     |
    | ord-100 | ORD-1003, ORD-1002, ORD-1001 |
    |         | ORD-1003, ORD-1002, ORD-1001 |

@UC-002.3
Scenario Outline: Should read the orders one page at a time
  When the customer opens page <page> of My orders with page size 2
  Then My orders shows orders <orders>
  And the total number of orders is 3

  Examples:
    | page | orders             |
    | 1    | ORD-1003, ORD-1002 |
    | 2    | ORD-1001           |
    | 3    | (none)             |

@UC-002.3
Scenario: Should show the first page of 50 orders by default
  When the customer opens My orders
  Then My orders shows page 1 with page size 50

@UC-002.4
Scenario: Should show every detail of the order
  Given the order ORD-1001 was placed for product "BOOK-123" with unit price 20.00, quantity 5, country "US" with tax rate 0.10, and coupon "SUMMER2025" with discount rate 0.10
  When the customer opens the details of order ORD-1001
  Then the order details show:
    | field           | value                |
    | Order Number    | ORD-1001             |
    | Order Timestamp | 2026-01-10T09:00:00Z |
    | Status          | PLACED               |
    | SKU             | BOOK-123             |
    | Country         | US                   |
    | Quantity        | 5                    |
    | Unit Price      | $20.00               |
    | Base Price      | $100.00              |
    | Discount Rate   | 10.00%               |
    | Discount Amount | $10.00               |
    | Subtotal Price  | $90.00               |
    | Tax Rate        | 10.00%               |
    | Tax Amount      | $9.00                |
    | Total Price     | $99.00               |
    | Applied Coupon  | SUMMER2025           |

@UC-002.4
Scenario: Should show no applied coupon when the order used none
  Given the order ORD-1002 was placed without a coupon
  When the customer opens the details of order ORD-1002
  Then the order details show applied coupon "None"

@UC-002.5
Scenario Outline: Should show the status of the order
  When the customer opens the details of order <order>
  Then the order details show status <status>

  Examples:
    | order    | status    |
    | ORD-1001 | PLACED    |
    | ORD-1002 | DELIVERED |
    | ORD-1003 | CANCELLED |

@UC-002.6
Scenario: Should not list another customer's order
  When the customer opens My orders
  Then My orders does not show order ORD-2001

@UC-002.6
Scenario: Should not reveal another customer's order
  When the customer opens the details of order ORD-2001
  Then the customer is told "Order ORD-2001 does not exist."

@UC-002.7 @narrative-1a
Scenario: Should require sign-in to view an order
  Given the user is not logged in
  When the user opens the details of order ORD-1001
  Then the user is told "Authentication is required"

@UC-002.8 @narrative-2a
Scenario Outline: Should reject a page or page size out of range
  When the customer opens page <page> of My orders with page size <size>
  Then the request is rejected
  And the customer is told "<message>" for field <field>

  Examples:
    | page | size | field | message                             |
    | 0    | 50   | page  | Page must be 1 or greater           |
    | 1    | 101  | size  | Page size must be between 1 and 100 |
    | 1    | 0    | size  | Page size must be between 1 and 100 |

@UC-002.9 @narrative-4a
Scenario: Should tell the customer when no order matches
  When the customer filters My orders by order number "ORD-9999"
  Then My orders shows no orders
  And the customer is told "No orders found"

@UC-002.10 @narrative-5a
Scenario: Should tell the customer when the order does not exist
  When the customer opens the details of order NON-EXISTENT-ORDER-99999
  Then the customer is told "Order NON-EXISTENT-ORDER-99999 does not exist."
```
