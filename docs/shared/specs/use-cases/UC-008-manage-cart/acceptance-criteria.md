---
id: UC-008
title: Manage Cart
status: planned
---

# UC-008 Manage Cart — Acceptance criteria

Narrative: [use-case.md](use-case.md). Business rules: [business-rules.md](business-rules.md).

Tags: `@UC-008.N` for N = 1 to 8 checks business rule BR-N. Tags from `@UC-008.9` on check an extension, which is also tagged `@narrative-<step><letter>`.

```gherkin
Feature: Manage Cart

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

@UC-008.1
Scenario: Should show the cart's total quantity in the header
  Given the visitor's cart contains:
    | sku      | quantity |
    | HP-15    | 2        |
    | DELL-XPS | 1        |
  When the visitor browses the catalog
  Then the cart badge shows 3

@UC-008.1
Scenario: Should hide the cart badge when the cart is empty
  When the visitor browses the catalog
  Then the cart badge is hidden

@UC-008.2
Scenario: Should add quantity 1 from the catalog
  Given the visitor is browsing the catalog
  When the visitor adds product LOGI-MX3S to the cart from the catalog
  Then the visitor is told "Added to cart"
  And the visitor's cart contains:
    | sku       | quantity |
    | LOGI-MX3S | 1        |
  And the cart badge shows 1

@UC-008.2
Scenario: Should raise the quantity of a product already in the cart
  Given the visitor's cart contains:
    | sku       | quantity |
    | LOGI-MX3S | 1        |
  When the visitor adds product LOGI-MX3S to the cart from the catalog
  Then the visitor's cart contains:
    | sku       | quantity |
    | LOGI-MX3S | 2        |
  And the cart shows 1 item

@UC-008.3
Scenario Outline: Should cap the quantity of a line at 99 when adding
  Given the visitor's cart contains:
    | sku   | quantity |
    | HP-15 | <before> |
  When the visitor adds <added> of product HP-15 from the product page
  Then the visitor's cart contains:
    | sku   | quantity |
    | HP-15 | <after>  |

  Examples:
    | before | added | after |
    | 97     | 2     | 99    |
    | 98     | 5     | 99    |
    | 99     | 1     | 99    |

@UC-008.4
Scenario: Should show each cart line with its title, unit price, quantity and line total
  Given the visitor's cart contains:
    | sku      | quantity |
    | HP-15    | 2        |
    | DELL-XPS | 1        |
  When the visitor views the cart
  Then the cart shows these lines, in this order:
    | sku      | title        | unit price | quantity | line total |
    | HP-15    | HP Laptop 15 | $699.99    | 2        | $1,399.98  |
    | DELL-XPS | Dell XPS 13  | $1,299.99  | 1        | $1,299.99  |
  And the cart shows 2 items

@UC-008.4
Scenario: Should list the lines in the order their products were first added
  Given the visitor has added product LG-27UL500 to the cart
  And the visitor has added product HP-15 to the cart
  And the visitor has added product LG-27UL500 to the cart
  When the visitor views the cart
  Then the cart shows lines LG-27UL500, HP-15

@UC-008.5
Scenario: Should show the subtotal and leave tax and discounts to checkout
  Given the visitor's cart contains:
    | sku      | quantity |
    | HP-15    | 2        |
    | DELL-XPS | 1        |
  When the visitor views the cart
  Then the cart subtotal is "$2,699.97"
  And the visitor is told tax and discounts are calculated at checkout

@UC-008.5
Scenario: Should show the current catalog price
  Given the visitor's cart contains:
    | sku   | quantity |
    | HP-15 | 2        |
  And the ERP price of HP-15 has changed to 649.99
  When the visitor views the cart
  Then cart line HP-15 shows unit price "$649.99" and line total "$1,299.98"
  And the cart subtotal is "$1,299.98"

@UC-008.6
Scenario: Should update the line total, subtotal and badge when the quantity changes
  Given the visitor's cart contains:
    | sku      | quantity |
    | HP-15    | 2        |
    | DELL-XPS | 1        |
  When the visitor changes the quantity of HP-15 to 3
  Then cart line HP-15 shows quantity 3 and line total "$2,099.97"
  And the cart subtotal is "$3,399.96"
  And the cart badge shows 4

@UC-008.6
Scenario Outline: Should keep the quantity between 1 and 99 when using the stepper
  Given the visitor's cart contains:
    | sku   | quantity |
    | HP-15 | <before> |
  When the visitor <action> the quantity of HP-15
  Then cart line HP-15 shows quantity <after>

  Examples:
    | before | action    | after |
    | 1      | increases | 2     |
    | 2      | decreases | 1     |
    | 1      | decreases | 1     |
    | 99     | increases | 99    |

@UC-008.7
Scenario: Should remove a line from the cart
  Given the visitor's cart contains:
    | sku      | quantity |
    | HP-15    | 2        |
    | DELL-XPS | 1        |
  When the visitor removes HP-15 from the cart
  Then the cart shows lines DELL-XPS
  And the cart subtotal is "$1,299.99"
  And the cart badge shows 1

@UC-008.8
Scenario: Should keep the cart without signing in
  Given the visitor is not signed in
  When the visitor adds product HP-15 to the cart from the catalog
  And the visitor views the cart
  Then the visitor is not asked to sign in
  And the cart shows lines HP-15

@UC-008.8
Scenario: Should keep the cart across visits
  Given the visitor's cart contains:
    | sku   | quantity |
    | HP-15 | 2        |
  When the visitor leaves the site and comes back later in the same browser
  Then the visitor's cart contains:
    | sku   | quantity |
    | HP-15 | 2        |

@UC-008.8
Scenario: Should keep the cart when the visitor signs in
  Given the visitor is not signed in
  And the visitor's cart contains:
    | sku      | quantity |
    | HP-15    | 2        |
    | DELL-XPS | 1        |
  When the visitor signs in as a customer
  Then the customer's cart contains:
    | sku      | quantity |
    | HP-15    | 2        |
    | DELL-XPS | 1        |
  And the cart badge shows 3

@UC-008.9 @narrative-4a
Scenario: Should tell the visitor when the cart is empty
  When the visitor views the cart
  Then the visitor is told "Your cart is empty."
  And the visitor is offered a link to start shopping

@UC-008.9 @narrative-4a
Scenario: Should show the empty cart after removing the last line
  Given the visitor's cart contains:
    | sku   | quantity |
    | HP-15 | 1        |
  When the visitor removes HP-15 from the cart
  Then the visitor is told "Your cart is empty."
  And the cart badge is hidden

@UC-008.10 @narrative-4b
Scenario: Should tell the visitor when the ERP is unavailable and keep the cart
  Given the visitor's cart contains:
    | sku   | quantity |
    | HP-15 | 2        |
  And the ERP is unavailable
  When the visitor views the cart
  Then the visitor is told "Products are temporarily unavailable."
  And the cart badge shows 2

@UC-008.10 @narrative-4b
Scenario: Should show the cart again once the ERP is back
  Given the visitor's cart contains:
    | sku   | quantity |
    | HP-15 | 2        |
  And the visitor has viewed the cart while the ERP was unavailable
  When the ERP is available again
  And the visitor views the cart
  Then cart line HP-15 shows quantity 2 and line total "$1,399.98"

@UC-008.11 @narrative-5a
Scenario Outline: Should keep the previous quantity when the new one is not a whole number from 1 to 99
  Given the visitor's cart contains:
    | sku   | quantity |
    | HP-15 | 2        |
  When the visitor changes the quantity of HP-15 to <quantity>
  Then the visitor is told "Quantity must be a whole number from 1 to 99."
  And cart line HP-15 shows quantity 2

  Examples:
    | quantity |
    | 0        |
    | -1       |
    | 1.5      |
    | abc      |
    | 100      |
```
