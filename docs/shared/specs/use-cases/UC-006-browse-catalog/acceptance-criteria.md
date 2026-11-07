---
id: UC-006
title: Browse Catalog
status: planned
---

# UC-006 Browse Catalog — Acceptance criteria

Narrative: [use-case.md](use-case.md). Business rules: [business-rules.md](business-rules.md).

Tags: `@UC-006.N` for N = 1 to 8 checks business rule BR-N. Tags from `@UC-006.9` on check an extension, which is also tagged `@narrative-<step><letter>`.

```gherkin
Feature: Browse Catalog

Background:
  Given the ERP has these products, in this order:
    | sku          | title                     | brand    | category    | price   | description               |
    | HP-15        | HP Laptop 15              | HP       | Laptops     | 699.99  | Everyday 15-inch laptop   |
    | DELL-XPS     | Dell XPS 13               | Dell     | Laptops     | 1299.99 | Compact 13-inch ultrabook |
    | LENOVO-T14   | Lenovo ThinkPad T14       | Lenovo   | Laptops     | 999.99  | Business laptop           |
    | DELL-U2723QE | Dell UltraSharp 27 4K     | Dell     | Monitors    | 579.99  | 27-inch USB-C monitor     |
    | LG-27UL500   | LG 27-inch 4K UHD Monitor | LG       | Monitors    | 299.99  | HDR10 monitor             |
    | LOGI-MX3S    | Logitech MX Master 3S     | Logitech | Accessories | 99.99   | Wireless mouse            |

@UC-006.1
Scenario: Should list every ERP product in the ERP's order
  When the visitor browses the catalog
  Then the catalog shows products HP-15, DELL-XPS, LENOVO-T14, DELL-U2723QE, LG-27UL500, LOGI-MX3S
  And the product count is 6

@UC-006.1
Scenario: Should show the title, brand and price of each product
  When the visitor browses the catalog
  Then product DELL-XPS shows title "Dell XPS 13", brand "Dell" and price "$1,299.99"
  And product LOGI-MX3S shows title "Logitech MX Master 3S", brand "Logitech" and price "$99.99"

@UC-006.2
Scenario: Should offer the categories of the catalog's products
  When the visitor browses the catalog
  Then the category filter offers All, Laptops, Monitors and Accessories
  And the category filter is set to All

@UC-006.2
Scenario: Should show only the products in the chosen category
  When the visitor filters the catalog by category Monitors
  Then the catalog shows products DELL-U2723QE, LG-27UL500
  And the product count is 2

@UC-006.2
Scenario: Should filter by category from the header link
  When the visitor follows the header link for category Laptops
  Then the catalog shows products HP-15, DELL-XPS, LENOVO-T14
  And the category filter is set to Laptops

@UC-006.3
Scenario: Should offer the brands of the catalog's products
  When the visitor browses the catalog
  Then the brand filter offers HP, Dell, Lenovo, LG and Logitech
  And no brand is ticked

@UC-006.3
Scenario Outline: Should show only the products of the ticked brands
  When the visitor filters the catalog by brands <brands>
  Then the catalog shows products <products>

  Examples:
    | brands   | products                                                         |
    | Dell     | DELL-XPS, DELL-U2723QE                                           |
    | Dell, LG | DELL-XPS, DELL-U2723QE, LG-27UL500                               |
    | (none)   | HP-15, DELL-XPS, LENOVO-T14, DELL-U2723QE, LG-27UL500, LOGI-MX3S |

@UC-006.4
Scenario Outline: Should find products by title, brand or description, ignoring case
  When the visitor searches the catalog for "<term>"
  Then the catalog shows products <products>

  Examples:
    | term     | products                                                         |
    | thinkpad | LENOVO-T14                                                       |
    | LOGITECH | LOGI-MX3S                                                        |
    | wireless | LOGI-MX3S                                                        |
    | 4k       | DELL-U2723QE, LG-27UL500                                         |
    |          | HP-15, DELL-XPS, LENOVO-T14, DELL-U2723QE, LG-27UL500, LOGI-MX3S |

@UC-006.5
Scenario: Should show only products that match every filter
  When the visitor filters the catalog by category Laptops and brands Dell
  Then the catalog shows products DELL-XPS
  And the product count is 1

@UC-006.6
Scenario Outline: Should sort the products
  When the visitor sorts the catalog by <sort>
  Then the catalog shows products <products>

  Examples:
    | sort              | products                                                         |
    | Featured          | HP-15, DELL-XPS, LENOVO-T14, DELL-U2723QE, LG-27UL500, LOGI-MX3S |
    | price low to high | LOGI-MX3S, LG-27UL500, DELL-U2723QE, HP-15, LENOVO-T14, DELL-XPS |
    | price high to low | DELL-XPS, LENOVO-T14, HP-15, DELL-U2723QE, LG-27UL500, LOGI-MX3S |
    | name A to Z       | DELL-U2723QE, DELL-XPS, HP-15, LENOVO-T14, LG-27UL500, LOGI-MX3S |

@UC-006.6
Scenario: Should sort only the filtered products
  When the visitor filters the catalog by category Laptops
  And the visitor sorts the catalog by price high to low
  Then the catalog shows products DELL-XPS, LENOVO-T14, HP-15

@UC-006.7
Scenario: Should show the same filtered view when the page address is opened again
  Given the visitor has filtered the catalog by category Laptops and sorted it by price high to low
  When the visitor opens the same page address again
  Then the catalog shows products DELL-XPS, LENOVO-T14, HP-15
  And the category filter is set to Laptops
  And the sort is price high to low

@UC-006.8
Scenario: Should let an anonymous visitor browse without signing in
  Given the visitor is not signed in
  When the visitor browses the catalog
  Then the visitor is not asked to sign in
  And the product count is 6

@UC-006.8
Scenario: Should show a signed-in customer the same catalog
  Given the user is logged in as a customer
  When the customer browses the catalog
  Then the catalog shows products HP-15, DELL-XPS, LENOVO-T14, DELL-U2723QE, LG-27UL500, LOGI-MX3S

@UC-006.9 @narrative-5a
Scenario: Should tell the visitor when no product matches
  When the visitor searches the catalog for "printer"
  Then the catalog shows no products
  And the visitor is told "No products match your search."
  And the visitor is offered to clear the filters

@UC-006.9 @narrative-5a
Scenario: Should show the full catalog after clearing the filters
  Given the visitor has searched the catalog for "printer"
  When the visitor clears the filters
  Then the catalog shows products HP-15, DELL-XPS, LENOVO-T14, DELL-U2723QE, LG-27UL500, LOGI-MX3S
  And the category filter is set to All
  And the sort is Featured

@UC-006.10 @narrative-2a
Scenario: Should tell the visitor when the ERP is unavailable
  Given the ERP is unavailable
  When the visitor browses the catalog
  Then the catalog shows no products
  And the visitor is told "Products are temporarily unavailable."

@UC-006.10 @narrative-2a
Scenario: Should keep the rest of the site working when the ERP is unavailable
  Given the user is logged in as a customer
  And the ERP is unavailable
  When the customer browses the catalog
  Then the customer is told "Products are temporarily unavailable."
  And the customer can still open My orders
```
