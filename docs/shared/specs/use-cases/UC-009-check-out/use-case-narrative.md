---
id: UC-009
title: Check Out
---

# UC-009 Check Out

**Primary actor:** Customer
**Secondary actors:** ERP, Clock (both through UC-001 Place Order)

## Main Success Scenario
1. The customer proceeds to checkout from the cart (UC-008 Manage Cart).
2. The system checks that the user is a signed-in customer.
3. The system shows the checkout: the customer's saved shipping address is preselected, and the order review lists the cart lines with their quantities and line totals, the cart subtotal, and the tax country (the shipping address's country) with its estimated tax rate ([BR-2](business-rules.md), [BR-3](business-rules.md), [BR-5](business-rules.md)).
4. The customer keeps the saved address, or chooses to ship to a different address and enters it ([BR-3](business-rules.md)).
5. The customer optionally enters a coupon code ([BR-4](business-rules.md)).
6. The customer places the order.
7. The system validates the shipping address ([BR-3](business-rules.md)).
8. For each cart line, in cart order, the system places an order through UC-001 Place Order with the line's SKU and quantity, the shipping address and the coupon. The tax rate of each order is the rate of the shipping address's country ([BR-1](business-rules.md), [BR-2](business-rules.md), [BR-4](business-rules.md)).
9. The system empties the cart and shows the order confirmation with every new order number ([BR-6](business-rules.md)).

## Extensions
- 2a. The user is not signed in:
    - The system asks the visitor to sign in. After signing in, the customer comes back to the checkout with the cart unchanged. Resume at step 3.
- 2b. The user is an admin:
    - The system tells the user they are not authorized. The use case ends.
- 3a. The cart is empty:
    - The system sends the customer to the cart, which says the cart is empty. The use case ends.
- 3b. The customer has no saved shipping address (first order):
    - The system opens the address form directly, with the first and last name prefilled from the sign-in account where known, and "Save as my default shipping address" ticked.
    - The customer enters the address and may untick "Save as my default shipping address". Resume at step 5.
    - At step 9, if the box was ticked, the system saves the address as the customer's profile shipping address (UC-010 Manage Profile).
- 7a. The shipping address is incomplete, or its country is not supported:
    - The system tells the customer which fields to correct. No order is placed and the cart is unchanged. Resume at step 4.
- 8a. Place Order rejects some of the cart lines (for example the coupon has reached its usage limit, or the ERP no longer offers the product):
    - The other lines are still placed.
    - The system removes the placed lines from the cart and keeps the rejected lines in it.
    - The system shows the order confirmation with a result per line: the order number for each placed line, and the SKU and the reason for each rejected line. The use case ends.
- 8b. Place Order rejects every cart line (for example the coupon doesn't exist):
    - No order is placed. The system keeps the customer on the checkout with the cart unchanged and tells them, per SKU, why it was rejected. Resume at step 4.

## Acceptance criteria

See [acceptance-criteria.md](acceptance-criteria.md).
