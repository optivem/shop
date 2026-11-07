---
id: UC-001
title: Place Order
---

# UC-001 Place Order

**Primary actor:** Customer
**Secondary actors:** ERP, Clock

## Main Success Scenario
1. The customer submits an order for a product: its SKU, a quantity, a country and, optionally, a coupon code.
2. The system checks that the user is allowed to place orders.
3. The system checks with the Clock that orders are being accepted at this time.
4. The system validates the quantity and the country.
5. The system gets the product and its unit price from the ERP.
6. The system validates the coupon.
7. The system calculates the price: base price, any promotion or coupon discount, and tax for the country ([BR-1 to BR-4](business-rules.md)).
8. The system stores the order and records the coupon use.
9. The system shows the customer the order confirmation with the order number.

## Extensions
- 2a. The user is an admin:
    - The system tells the user they don't have permission. The use case ends.
- 3a. It is between 23:59 and 00:00 on December 31st:
    - The system tells the customer orders can't be placed during year-end closing. The use case ends.
- 4a. The quantity is missing, not a whole number, or not positive:
    - The system tells the customer the quantity must be a positive whole number. Resume at step 1.
- 4b. The country is missing or not supported:
    - The system tells the customer to enter a valid country. Resume at step 1.
- 5a. The ERP is unavailable:
    - The system tells the customer product information can't be retrieved right now. The use case ends.
- 5b. The SKU is missing, or no product has that SKU:
    - The system tells the customer the product doesn't exist. Resume at step 1.
- 6a. The coupon doesn't exist, has expired or has reached its usage limit:
    - The system tells the customer the coupon can't be used. Resume at step 1.

## Acceptance criteria

See [acceptance-criteria.md](acceptance-criteria.md).
