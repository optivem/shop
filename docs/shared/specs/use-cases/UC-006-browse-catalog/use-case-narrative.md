---
id: UC-006
title: Browse Catalog
---

# UC-006 Browse Catalog

**Primary actor:** Visitor (anonymous; a signed-in Customer can browse the same way)
**Secondary actors:** ERP

## Main Success Scenario
1. The visitor opens the catalog, either from the home page or from a category link in the header. No sign-in is needed ([BR-8](business-rules.md)).
2. The system gets the product list from the ERP.
3. The system shows the products in the ERP's order, with the number of products, and offers the catalog's categories and brands as filters ([BR-1](business-rules.md)).
4. The visitor narrows the list: picks a category, ticks one or more brands, and/or searches for a term.
5. The system shows only the products that match every chosen filter, with the new count ([BR-2 to BR-5](business-rules.md)).
6. The visitor picks a sort order.
7. The system shows the matching products in that order ([BR-6](business-rules.md)). The chosen filters and sort are kept in the page address, so the view can be bookmarked ([BR-7](business-rules.md)).
8. The visitor opens a product (UC-007 View Product) or adds one to the cart (UC-008 Manage Cart).

Steps 4 to 7 can repeat, in any order, until the visitor finds a product.

## Extensions
- 2a. The ERP is unavailable:
    - The system tells the visitor products are temporarily unavailable. The rest of the site (header, cart, My orders) keeps working. The use case ends.
- 5a. No product matches the chosen filters:
    - The system tells the visitor no products match and offers to clear the filters.
    - The visitor clears the filters. Resume at step 3.

## Acceptance criteria

See [acceptance-criteria.md](acceptance-criteria.md).
