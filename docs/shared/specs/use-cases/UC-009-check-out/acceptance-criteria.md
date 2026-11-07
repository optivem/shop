---
id: UC-009
title: Check Out
status: planned
---

# UC-009 Check Out — Acceptance criteria

Narrative: [use-case.md](use-case.md). Business rules: [business-rules.md](business-rules.md).

Tags: `@UC-009.N` for N = 1 to 6 checks business rule BR-N. Tags from `@UC-009.7` on check an extension, which is also tagged `@narrative-<step><letter>`.

```gherkin
Feature: Check Out

Background:
  Given the ERP has these products, in this order:
    | sku          | title                     | brand    | category    | price   | description               |
    | HP-15        | HP Laptop 15              | HP       | Laptops     | 699.99  | Everyday 15-inch laptop   |
    | DELL-XPS     | Dell XPS 13               | Dell     | Laptops     | 1299.99 | Compact 13-inch ultrabook |
    | LENOVO-T14   | Lenovo ThinkPad T14       | Lenovo   | Laptops     | 999.99  | Business laptop           |
    | DELL-U2723QE | Dell UltraSharp 27 4K     | Dell     | Monitors    | 579.99  | 27-inch USB-C monitor     |
    | LG-27UL500   | LG 27-inch 4K UHD Monitor | LG       | Monitors    | 299.99  | HDR10 monitor             |
    | LOGI-MX3S    | Logitech MX Master 3S     | Logitech | Accessories | 99.99   | Wireless mouse            |
  And the countries have these tax rates:
    | country | tax rate |
    | US      | 0.00     |
    | DE      | 0.19     |
    | GB      | 0.20     |

@UC-009.1
Scenario: Should place one order per cart line
  Given the user is logged in as a customer
  And the customer's saved shipping address is "Ana Novak, Main St 1, 10115 Berlin, DE"
  And the customer's cart has:
    | sku      | quantity |
    | HP-15    | 2        |
    | DELL-XPS | 1        |
  When the customer places the checkout order
  Then 2 orders are placed, each with its own order number
  And the order for HP-15 has quantity 2
  And the order for DELL-XPS has quantity 1
  And both orders have shipping address "Ana Novak, Main St 1, 10115 Berlin, DE"

@UC-009.1
Scenario: Should place the orders in cart order
  Given the user is logged in as a customer
  And the customer's saved shipping address is "Ana Novak, Main St 1, 10115 Berlin, DE"
  And the customer's cart has:
    | sku       | quantity |
    | LOGI-MX3S | 1        |
    | HP-15     | 1        |
  When the customer places the checkout order
  Then the order for LOGI-MX3S is placed before the order for HP-15

@UC-009.2
Scenario Outline: Should apply the tax rate of the shipping address's country
  Given the user is logged in as a customer
  And the customer's saved shipping address is "<address>"
  And the customer's cart has:
    | sku       | quantity |
    | LOGI-MX3S | 1        |
  When the customer places the checkout order
  Then the order for LOGI-MX3S has country <country>
  And the order for LOGI-MX3S has tax rate <tax rate>

  Examples:
    | address                                    | country | tax rate |
    | John Smith, 1 Main St, 10001 New York, US  | US      | 0.00     |
    | Ana Novak, Main St 1, 10115 Berlin, DE     | DE      | 0.19     |
    | Jane Brown, 1 High St, SW1A 1AA London, GB | GB      | 0.20     |

@UC-009.2
Scenario: Should show the estimated tax for the chosen shipping country
  Given the user is logged in as a customer
  And the customer's saved shipping address is "Ana Novak, Main St 1, 10115 Berlin, DE"
  And the customer's cart has:
    | sku   | quantity |
    | HP-15 | 1        |
  When the customer opens the checkout
  Then the order review shows tax country DE with estimated tax rate 19%
  When the customer chooses to ship to "Jane Brown, 1 High St, SW1A 1AA London, GB"
  Then the order review shows tax country GB with estimated tax rate 20%

@UC-009.3
Scenario: Should preselect the saved shipping address
  Given the user is logged in as a customer
  And the customer's saved shipping address is "Ana Novak, Main St 1, 10115 Berlin, DE"
  And the customer's cart has:
    | sku   | quantity |
    | HP-15 | 1        |
  When the customer opens the checkout
  Then the shipping address "Ana Novak, Main St 1, 10115 Berlin, DE" is selected

@UC-009.3
Scenario: Should ship to a different address without changing the profile
  Given the user is logged in as a customer
  And the customer's saved shipping address is "Ana Novak, Main St 1, 10115 Berlin, DE"
  And the customer's cart has:
    | sku   | quantity |
    | HP-15 | 1        |
  When the customer ships to "Jane Brown, 1 High St, SW1A 1AA London, GB" and places the checkout order
  Then the order for HP-15 has shipping address "Jane Brown, 1 High St, SW1A 1AA London, GB"
  And the order for HP-15 has tax rate 0.20
  And the customer's saved shipping address is still "Ana Novak, Main St 1, 10115 Berlin, DE"

@UC-009.4
Scenario: Should apply the coupon to every order
  Given the user is logged in as a customer
  And the customer's saved shipping address is "John Smith, 1 Main St, 10001 New York, US"
  And a coupon "SUMMER2025" with discount rate 0.15
  And the customer's cart has:
    | sku       | quantity |
    | HP-15     | 1        |
    | LOGI-MX3S | 1        |
  When the customer places the checkout order with coupon "SUMMER2025"
  Then 2 orders are placed
  And both orders have applied coupon "SUMMER2025" with discount rate 0.15
  And coupon "SUMMER2025" has been used 2 times

@UC-009.4
Scenario: Should apply no discount when there is no coupon
  Given the user is logged in as a customer
  And the customer's saved shipping address is "John Smith, 1 Main St, 10001 New York, US"
  And the customer's cart has:
    | sku   | quantity |
    | HP-15 | 1        |
  When the customer places the checkout order with no coupon
  Then the order for HP-15 has no applied coupon
  And the order for HP-15 has discount amount 0.00

@UC-009.5
Scenario: Should review the cart lines and subtotal at catalog prices
  Given the user is logged in as a customer
  And the customer's saved shipping address is "Ana Novak, Main St 1, 10115 Berlin, DE"
  And the customer's cart has:
    | sku      | quantity |
    | HP-15    | 2        |
    | DELL-XPS | 1        |
  When the customer opens the checkout
  Then the order review shows HP-15 with quantity 2 and line total "$1,399.98"
  And the order review shows DELL-XPS with quantity 1 and line total "$1,299.99"
  And the order review shows subtotal "$2,699.97"
  And the customer is told discount and tax are worked out per order when it is placed

@UC-009.5
Scenario: Should price each order from the ERP when it is placed
  Given the user is logged in as a customer
  And the customer's saved shipping address is "John Smith, 1 Main St, 10001 New York, US"
  And the customer's cart has:
    | sku   | quantity |
    | HP-15 | 1        |
  And the ERP price of HP-15 has since changed to 649.99
  When the customer places the checkout order
  Then the order for HP-15 has unit price 649.99

@UC-009.6
Scenario: Should confirm every order and empty the cart
  Given the user is logged in as a customer
  And the customer's saved shipping address is "Ana Novak, Main St 1, 10115 Berlin, DE"
  And the customer's cart has:
    | sku      | quantity |
    | HP-15    | 2        |
    | DELL-XPS | 1        |
  When the customer places the checkout order
  Then the customer is thanked for the order
  And the confirmation lists the order numbers of the orders for HP-15 and DELL-XPS
  And each confirmed order number links to its order details
  And the confirmation offers "View my orders" and "Continue shopping"
  And the customer's cart is empty

@UC-009.7 @narrative-2a
Scenario: Should ask an anonymous visitor to sign in before checkout
  Given the visitor is not signed in
  And the visitor's cart has:
    | sku   | quantity |
    | HP-15 | 1        |
  When the visitor proceeds to checkout
  Then the visitor is asked to sign in

@UC-009.7 @narrative-2a
Scenario: Should return to the checkout with the cart unchanged after sign-in
  Given the visitor is not signed in
  And the visitor's cart has:
    | sku       | quantity |
    | HP-15     | 2        |
    | LOGI-MX3S | 1        |
  When the visitor proceeds to checkout and signs in as a customer
  Then the checkout is shown
  And the order review shows HP-15 with quantity 2 and LOGI-MX3S with quantity 1

@UC-009.8 @narrative-2b
Scenario: Admin should not be able to check out
  Given the user is logged in as an admin
  When the user opens the checkout
  Then the user is told they are not authorized

@UC-009.9 @narrative-3a
Scenario: Should send the customer to the cart when the cart is empty
  Given the user is logged in as a customer
  And the customer's cart is empty
  When the customer opens the checkout
  Then the customer is shown the cart
  And the customer is told "Your cart is empty."

@UC-009.10 @narrative-3b
Scenario: Should open the address form on a first order
  Given the user is logged in as a customer whose sign-in account has name "Ana Novak"
  And the customer has no saved shipping address
  And the customer's cart has:
    | sku   | quantity |
    | HP-15 | 1        |
  When the customer opens the checkout
  Then the shipping address form is open
  And the shipping first name is "Ana" and the shipping last name is "Novak"
  And "Save as my default shipping address" is ticked

@UC-009.10 @narrative-3b
Scenario: Should save the address to the profile when "Save as default" is ticked
  Given the user is logged in as a customer
  And the customer has no saved shipping address
  And the customer's cart has:
    | sku   | quantity |
    | HP-15 | 1        |
  When the customer ships to "Ana Novak, Main St 1, 10115 Berlin, DE" and places the checkout order
  Then the order for HP-15 is placed
  And the customer's saved shipping address is "Ana Novak, Main St 1, 10115 Berlin, DE"

@UC-009.10 @narrative-3b
Scenario: Should not save the address when "Save as default" is unticked
  Given the user is logged in as a customer
  And the customer has no saved shipping address
  And the customer's cart has:
    | sku   | quantity |
    | HP-15 | 1        |
  When the customer ships to "Ana Novak, Main St 1, 10115 Berlin, DE", unticks "Save as my default shipping address" and places the checkout order
  Then the order for HP-15 is placed
  And the customer still has no saved shipping address

@UC-009.11 @narrative-7a
Scenario Outline: Should reject an incomplete shipping address
  Given the user is logged in as a customer
  And the customer has no saved shipping address
  And the customer's cart has:
    | sku   | quantity |
    | HP-15 | 1        |
  When the customer ships to "Ana Novak, Main St 1, 10115 Berlin, DE" with an empty <field> and places the checkout order
  Then no order is placed
  And the customer is told <field> is required
  And the customer's cart still has HP-15 with quantity 1

  Examples:
    | field       |
    | first name  |
    | last name   |
    | street      |
    | postal code |
    | city        |
    | country     |

@UC-009.11 @narrative-7a
Scenario: Should reject a shipping country that is not supported
  Given the user is logged in as a customer
  And the customer has no saved shipping address
  And the customer's cart has:
    | sku   | quantity |
    | HP-15 | 1        |
  When the customer ships to "Marie Dupont, 1 Rue de Rivoli, 75001 Paris, FR" and places the checkout order
  Then no order is placed
  And the customer is told the country must be US, DE or GB

@UC-009.12 @narrative-8a
Scenario: Should still place the other lines when the coupon runs out part-way
  Given the user is logged in as a customer
  And the customer's saved shipping address is "Ana Novak, Main St 1, 10115 Berlin, DE"
  And a coupon "LIMITED2024" with usage limit 1
  And the customer's cart has:
    | sku      | quantity |
    | HP-15    | 1        |
    | DELL-XPS | 1        |
  When the customer places the checkout order with coupon "LIMITED2024"
  Then the order for HP-15 is placed with coupon "LIMITED2024"
  And no order is placed for DELL-XPS
  And the confirmation shows the order number of the order for HP-15
  And the confirmation shows DELL-XPS was rejected because "Coupon code LIMITED2024 has exceeded its usage limit"
  And the customer's cart has only DELL-XPS with quantity 1

@UC-009.12 @narrative-8a
Scenario: Should still place the other lines when a product is no longer offered
  Given the user is logged in as a customer
  And the customer's saved shipping address is "Ana Novak, Main St 1, 10115 Berlin, DE"
  And the customer's cart has:
    | sku       | quantity |
    | DELL-XPS  | 1        |
    | LOGI-MX3S | 2        |
  And the ERP no longer offers DELL-XPS
  When the customer places the checkout order
  Then the order for LOGI-MX3S is placed
  And the confirmation shows the order number of the order for LOGI-MX3S
  And the confirmation shows DELL-XPS was rejected because "Product does not exist for SKU: DELL-XPS"
  And the customer's cart has only DELL-XPS with quantity 1

@UC-009.13 @narrative-8b
Scenario: Should place nothing and keep the cart when every line is rejected
  Given the user is logged in as a customer
  And the customer's saved shipping address is "Ana Novak, Main St 1, 10115 Berlin, DE"
  And the customer's cart has:
    | sku      | quantity |
    | HP-15    | 1        |
    | DELL-XPS | 1        |
  When the customer places the checkout order with coupon "INVALIDCOUPON"
  Then no order is placed
  And the checkout is shown
  And the customer is told HP-15 was rejected because "Coupon code INVALIDCOUPON does not exist"
  And the customer is told DELL-XPS was rejected because "Coupon code INVALIDCOUPON does not exist"
  And the customer's cart still has HP-15 and DELL-XPS
```
