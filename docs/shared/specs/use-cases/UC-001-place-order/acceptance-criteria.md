---
id: UC-001
title: Place Order
status: implemented
---

# UC-001 Place Order — Acceptance criteria

Narrative: [use-case-narrative.md](use-case-narrative.md). Business rules: [business-rules.md](business-rules.md).

```gherkin
Feature: Place Order

@UC-001.1
Scenario: Order status should be placed after placing order
  When the customer places an order
  Then the order is placed successfully
  And the order status is PLACED

@UC-001.1
Scenario: Order number should start with ORD-
  When the customer places an order
  Then the order is placed successfully
  And the order number starts with "ORD-"

@UC-001.2
Scenario: Should calculate base price as product of unit price and quantity
  Given a product with unit price 20.00
  When the customer places an order with quantity 5
  Then the order is placed successfully
  And the base price is 100.00

@UC-001.3
Scenario: Should subtract the coupon discount from the base price
  Given a product with unit price 20.00
  And a coupon "SUMMER2025" with discount rate 0.15
  When the customer places an order with quantity 5 and coupon "SUMMER2025"
  Then the order is placed successfully
  And the base price is 100.00
  And the discount amount is 15.00
  And the subtotal is 85.00

@UC-001.3
Scenario: Should apply no discount when there is no coupon
  Given a product with unit price 20.00
  When the customer places an order with quantity 5 and no coupon
  Then the order is placed successfully
  And the discount amount is 0.00
  And the subtotal is 100.00

@UC-001.4
Scenario Outline: Should apply the tax rate of the country
  Given country <country> has tax rate <tax rate>
  And a product with unit price <subtotal>
  When the customer places an order with quantity 1 for country <country>
  Then the order is placed successfully
  And the tax rate is <tax rate>
  And the tax amount is <tax>
  And the total price is <total>

  Examples:
    | country | tax rate | subtotal | tax   | total  |
    | UK      | 0.09     | 50.00    | 4.50  | 54.50  |
    | US      | 0.20     | 100.00   | 20.00 | 120.00 |

@UC-001.5
Scenario: Should increment coupon usage count after it is used
  Given a coupon "SUMMER2025" that has not been used
  When the customer places an order with coupon "SUMMER2025"
  Then the order is placed successfully
  And coupon "SUMMER2025" has been used 1 time

@UC-001.6
Scenario: Should record placement timestamp
  Given the Clock time is 2026-01-15T10:30:00Z
  When the customer places an order
  Then the order is placed successfully
  And the order was placed at 2026-01-15T10:30:00Z

@UC-001.7
Scenario: Should apply full price without promotion
  Given a product with unit price 20.00
  And no promotion is active
  And the country has tax rate 0.00
  When the customer places an order with quantity 5
  Then the order is placed successfully
  And the total price is 100.00

@UC-001.7
Scenario: Should apply discount when promotion is active
  Given a product with unit price 20.00
  And a promotion with discount 0.5 is active
  And the country has tax rate 0.00
  When the customer places an order with quantity 5
  Then the order is placed successfully
  And the total price is 50.00

@UC-001.8 @narrative-4a
Scenario: Should reject order with zero quantity
  When the customer places an order with quantity 0
  Then the order is rejected
  And the customer is told "Quantity must be positive"

@UC-001.8 @narrative-4a
Scenario Outline: Should reject order with invalid quantity
  When the customer places an order with quantity "<quantity>"
  Then the order is rejected
  And the customer is told "<message>"

  Examples:
    | quantity | message                      |
    | -10      | Quantity must be positive    |
    | 3.5      | Quantity must be an integer  |
    | lala     | Quantity must be an integer  |
    |          | Quantity must not be empty   |

@UC-001.9 @narrative-5b
Scenario: Should reject order with non-existent SKU
  When the customer places an order with SKU "NON-EXISTENT-SKU-12345"
  Then the order is rejected
  And the customer is told "Product does not exist for SKU: NON-EXISTENT-SKU-12345"

@UC-001.9 @narrative-5b
Scenario: Should reject order with missing SKU
  When the customer places an order with no SKU
  Then the order is rejected
  And the customer is told "SKU must not be empty"

@UC-001.10 @narrative-4b
Scenario: Should reject order with invalid country
  When the customer places an order with country "XX"
  Then the order is rejected
  And the customer is told "Country does not exist: XX"

@UC-001.10 @narrative-4b
Scenario: Should reject order with missing country
  When the customer places an order with no country
  Then the order is rejected
  And the customer is told "Country must not be empty"

@UC-001.11 @narrative-6a
Scenario: Should reject order with non-existent coupon
  When the customer places an order with coupon "INVALIDCOUPON"
  Then the order is rejected
  And the customer is told "Coupon code INVALIDCOUPON does not exist"
  And no order is stored

@UC-001.11 @narrative-6a
Scenario: Should reject order with expired coupon
  Given the Clock time is 2023-09-01T12:00:00Z
  And a coupon "SUMMER2023" valid from 2023-06-01T00:00:00Z to 2023-08-31T23:59:59Z
  When the customer places an order with coupon "SUMMER2023"
  Then the order is rejected
  And the customer is told "Coupon code SUMMER2023 has expired"

@UC-001.11 @narrative-6a
Scenario: Should reject order with coupon that has reached its usage limit
  Given a coupon "LIMITED2024" with usage limit 2
  And 2 orders have already used coupon "LIMITED2024"
  When the customer places an order with coupon "LIMITED2024"
  Then the order is rejected
  And the customer is told "Coupon code LIMITED2024 has exceeded its usage limit"

@UC-001.12 @narrative-2a
Scenario: Admin should not be able to place order
  Given the user is logged in as an admin
  When the user places an order
  Then the order is rejected
  And the user is told "You do not have permission to perform this action"

@UC-001.13 @narrative-3a
Scenario: Should reject order placed at year end
  Given the Clock time is 2026-12-31T23:59:30Z
  When the customer places an order
  Then the order is rejected
  And the customer is told "Orders cannot be placed between 23:59 and 00:00 on December 31st"

@UC-001.14 @narrative-5a
Scenario: Should reject order when ERP is unavailable
  Given the ERP is unavailable
  When the customer places an order
  Then the order is rejected
  And the customer is told product information can't be retrieved right now
```
