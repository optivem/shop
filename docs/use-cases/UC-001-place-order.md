---
id: UC-001
title: Place Order
---

# UC-001 Place Order

Actor: Customer. Goal: place an order for a product.

## Main scenario
1. Customer enters SKU, quantity, country (and optional coupon).
2. System validates input, checks product in ERP, creates order, confirms.

## Acceptance criteria
- UC-001.1: A valid order is placed and gets status PLACED with an ORD- prefixed number.
- UC-001.2: Base price = unit price x quantity.
- UC-001.3: Coupon discount applies; no coupon means no discount; subtotal = base - discount.
- UC-001.4: Tax rate follows country; total = subtotal + tax.
- UC-001.5: Coupon usage count increments after use.
- UC-001.6: Placement timestamp is recorded.
- UC-001.7: Promotion price rules (full price without promotion, discount when active).
- UC-001.8: Non-positive / non-integer / empty / null quantity is rejected (narrative 4a).
- UC-001.9: Unknown SKU or empty / null SKU is rejected (narrative 5b).
- UC-001.10: Empty / null / invalid country is rejected.
- UC-001.11: Non-existent, expired or over-used coupon is rejected.
- UC-001.12: Admin cannot place an order.
- UC-001.13: Order rejected at year end.
- UC-001.14: ERP unavailable message (narrative 5a).
