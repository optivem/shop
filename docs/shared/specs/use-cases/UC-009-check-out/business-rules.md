# Business Rules: Check Out

Use case: [UC-009 Check Out](use-case.md). Acceptance criteria: [acceptance-criteria.md](acceptance-criteria.md).

- BR-1. One order per cart line: checkout places one order per cart line, in cart order, each through UC-001 Place Order with that line's SKU and quantity. Every order of a checkout gets the same shipping address and coupon, and its own order number.
- BR-2. Tax country: the tax rate of each order is the rate of the shipping address's country (US 0%, DE 19%, GB 20%). Checkout has no separate country field. The order review shows the tax country and its estimated tax rate.
- BR-3. Shipping address: the customer's saved profile address is preselected. The customer may ship to a different address instead; that address is used for this checkout only and does not change the profile. A shipping address needs a first name, last name, street, postal code, city and country, and the country must be US, DE or GB (the same rules as UC-010 Manage Profile).
- BR-4. Coupon: the coupon code is optional. The same code is applied to every order of the checkout, and each order validates it on its own (UC-001), so a coupon with a usage limit may apply to only some lines.
- BR-5. Order review: the review shows each cart line with its quantity and line total and the cart subtotal, at catalog prices. Discount and tax are worked out per order when it is placed, and each order is priced by UC-001 from the ERP price at that moment, so a stale cart can't change what is charged.
- BR-6. Confirmation: when every line is placed, the cart is emptied and the confirmation lists every new order number, each linking to its order details, with links to "View my orders" and "Continue shopping".
