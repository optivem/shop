---
id: UC-007
title: View Product
status: planned
---

# UC-007 View Product — Acceptance criteria

Narrative: [use-case.md](use-case.md). Business rules: [business-rules.md](business-rules.md).

Tags: `@UC-007.N` for N = 1 to 4 checks business rule BR-N. Tags from `@UC-007.5` on check an extension, which is also tagged `@narrative-<step><letter>`.

```gherkin
Feature: View Product

Background:
  Given the ERP has these products, in this order:
    | sku          | title                     | brand    | category    | price   | description               |
    | HP-15        | HP Laptop 15              | HP       | Laptops     | 699.99  | Everyday 15-inch laptop   |
    | DELL-XPS     | Dell XPS 13               | Dell     | Laptops     | 1299.99 | Compact 13-inch ultrabook |
    | LENOVO-T14   | Lenovo ThinkPad T14       | Lenovo   | Laptops     | 999.99  | Business laptop           |
    | DELL-U2723QE | Dell UltraSharp 27 4K     | Dell     | Monitors    | 579.99  | 27-inch USB-C monitor     |
    | LG-27UL500   | LG 27-inch 4K UHD Monitor | LG       | Monitors    | 299.99  | HDR10 monitor             |
    | LOGI-MX3S    | Logitech MX Master 3S     | Logitech | Accessories | 99.99   | Wireless mouse            |
  And the visitor's cart is empty

@UC-007.1
Scenario: Should show the details of the product
  When the visitor views product DELL-XPS
  Then the product page shows title "Dell XPS 13", brand "Dell", category "Laptops" and SKU "DELL-XPS"
  And the product page shows price "$1,299.99"
  And the product page shows description "Compact 13-inch ultrabook"

@UC-007.1
Scenario: Should show the breadcrumb trail of the product
  When the visitor views product LOGI-MX3S
  Then the breadcrumb trail is Home, Accessories, Logitech MX Master 3S

@UC-007.1
Scenario: Should open the product from the catalog
  Given the visitor is browsing the catalog
  When the visitor opens product HP-15 from the catalog
  Then the product page shows title "HP Laptop 15" and price "$699.99"

@UC-007.2
Scenario: Should start with quantity 1
  When the visitor views product HP-15
  Then the quantity is 1

@UC-007.2
Scenario Outline: Should keep the quantity between 1 and 99 when using the stepper
  Given the visitor is viewing product HP-15 with quantity <before>
  When the visitor <action> the quantity
  Then the quantity is <after>

  Examples:
    | before | action    | after |
    | 1      | increases | 2     |
    | 2      | decreases | 1     |
    | 1      | decreases | 1     |
    | 99     | increases | 99    |

@UC-007.3
Scenario: Should add the chosen quantity to the cart
  Given the visitor is viewing product HP-15
  When the visitor adds 3 to the cart
  Then the visitor is told "Added to cart"
  And the visitor's cart contains:
    | sku   | quantity |
    | HP-15 | 3        |
  And the cart badge shows 3
  And the visitor is still on the page of product HP-15

@UC-007.3
Scenario: Should add to a product already in the cart
  Given the visitor's cart contains:
    | sku   | quantity |
    | HP-15 | 1        |
  And the visitor is viewing product HP-15
  When the visitor adds 2 to the cart
  Then the visitor's cart contains:
    | sku   | quantity |
    | HP-15 | 3        |

@UC-007.4
Scenario: Should let an anonymous visitor view a product without signing in
  Given the visitor is not signed in
  When the visitor opens the page address of product LENOVO-T14
  Then the visitor is not asked to sign in
  And the product page shows title "Lenovo ThinkPad T14"

@UC-007.4
Scenario: Should show a signed-in customer the same product page
  Given the user is logged in as a customer
  When the customer views product LENOVO-T14
  Then the product page shows title "Lenovo ThinkPad T14", brand "Lenovo", category "Laptops" and SKU "LENOVO-T14"
  And the product page shows price "$999.99"

@UC-007.5 @narrative-2a
Scenario: Should show product not found for an unknown SKU
  When the visitor views product NO-SUCH-SKU
  Then the visitor is told "Product not found"
  And the visitor is offered a link back to the catalog

@UC-007.5 @narrative-2a
Scenario: Should go back to the catalog from product not found
  Given the visitor has viewed product NO-SUCH-SKU
  When the visitor follows the link back to the catalog
  Then the catalog shows products HP-15, DELL-XPS, LENOVO-T14, DELL-U2723QE, LG-27UL500, LOGI-MX3S

@UC-007.6 @narrative-2b
Scenario: Should tell the visitor when the ERP is unavailable
  Given the ERP is unavailable
  When the visitor views product HP-15
  Then the visitor is told "Products are temporarily unavailable."
  And the visitor cannot add the product to the cart

@UC-007.6 @narrative-2b
Scenario: Should keep the rest of the site working when the ERP is unavailable
  Given the user is logged in as a customer
  And the ERP is unavailable
  When the customer views product HP-15
  Then the customer is told "Products are temporarily unavailable."
  And the customer can still open My orders

@UC-007.7 @narrative-5a
Scenario Outline: Should not add a quantity that is not a whole number from 1 to 99
  Given the visitor is viewing product HP-15
  When the visitor adds <quantity> to the cart
  Then the visitor is told "Quantity must be a whole number from 1 to 99."
  And the visitor's cart is empty

  Examples:
    | quantity |
    | 0        |
    | -1       |
    | 1.5      |
    | abc      |
    | 100      |
```
