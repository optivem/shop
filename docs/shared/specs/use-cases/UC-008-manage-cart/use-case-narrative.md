---
id: UC-008
title: Manage Cart
---

# UC-008 Manage Cart

**Primary actor:** Visitor (anonymous; a signed-in Customer keeps a cart the same way)
**Secondary actors:** ERP (current product prices)

## Main Success Scenario
1. The visitor adds a product to the cart, either from the catalog (UC-006 Browse Catalog, quantity 1) or from the product page with a chosen quantity (UC-007 View Product). No sign-in is needed ([BR-8](business-rules.md)).
2. The system adds the product to the cart, or raises the quantity of its line if it is already there, up to 99 ([BR-2](business-rules.md), [BR-3](business-rules.md)). It confirms the product was added, and the cart badge in the header shows the cart's total quantity ([BR-1](business-rules.md)).
3. The visitor opens the cart from the header.
4. The system gets the products' current prices from the ERP and shows each cart line, the number of items and the subtotal ([BR-4](business-rules.md), [BR-5](business-rules.md)).
5. The visitor changes the quantity of a line.
6. The system shows the new quantity, line total, subtotal and cart badge ([BR-6](business-rules.md)).
7. The visitor removes a line.
8. The system removes the line and shows the new subtotal and cart badge ([BR-7](business-rules.md)).
9. The visitor proceeds to checkout (UC-009 Check Out) or goes on shopping (UC-006 Browse Catalog).

Steps 1 to 8 can repeat, in any order. The cart is kept while the visitor moves around the site and when the visitor signs in ([BR-8](business-rules.md)).

## Extensions
- 4a. The cart is empty (nothing was added, or the last line was removed):
    - The system tells the visitor the cart is empty and offers a link to start shopping. The use case ends.
- 4b. The ERP is unavailable:
    - The system tells the visitor products are temporarily unavailable. The cart keeps its products and quantities for later. The use case ends.
- 5a. The new quantity is not a whole number from 1 to 99:
    - The system tells the visitor the quantity must be a whole number from 1 to 99 and keeps the line's previous quantity. Resume at step 5.

## Acceptance criteria

See [acceptance-criteria.md](acceptance-criteria.md).
