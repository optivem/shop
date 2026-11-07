---
id: UC-007
title: View Product
---

# UC-007 View Product

**Primary actor:** Visitor (anonymous; a signed-in Customer can view products the same way)
**Secondary actors:** ERP

## Main Success Scenario
1. The visitor opens a product, either from the catalog (UC-006 Browse Catalog) or by its page address. No sign-in is needed ([BR-4](business-rules.md)).
2. The system gets the product from the ERP by its SKU.
3. The system shows the product's details, with the quantity set to 1 ([BR-1](business-rules.md)).
4. The visitor sets the quantity, with the stepper or by typing it ([BR-2](business-rules.md)).
5. The visitor adds the product to the cart.
6. The system adds the chosen quantity of the product to the cart, following the cart rules of UC-008 Manage Cart, and confirms it was added ([BR-3](business-rules.md)).
7. The visitor goes on browsing (UC-006 Browse Catalog) or opens the cart (UC-008 Manage Cart).

Step 6 includes the "add to cart" step of UC-008 Manage Cart: this use case chooses the product and the quantity, and UC-008 owns what the cart does with them (merging lines, the quantity limit, the cart badge).

## Extensions
- 2a. No product has that SKU:
    - The system shows a "Product not found" page with a link back to the catalog. The use case ends.
- 2b. The ERP is unavailable:
    - The system tells the visitor products are temporarily unavailable. The rest of the site (header, cart, My orders) keeps working. The use case ends.
- 5a. The quantity is not a whole number from 1 to 99:
    - The system tells the visitor the quantity must be a whole number from 1 to 99 and adds nothing to the cart. Resume at step 4.

## Acceptance criteria

See [acceptance-criteria.md](acceptance-criteria.md).
