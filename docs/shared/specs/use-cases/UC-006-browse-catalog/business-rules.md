# Business Rules: Browse Catalog

Use case: [UC-006 Browse Catalog](use-case.md). Acceptance criteria: [acceptance-criteria.md](acceptance-criteria.md).

- BR-1. The catalog lists every product the ERP offers, with the number of products shown. Each product shows its title, brand and price. Prices are in dollars with thousands separators and 2 decimals (e.g. $1,299.99).
- BR-2. Category filter: the visitor picks one category, or "All" (the default). The categories offered are the ones the catalog's products belong to.
- BR-3. Brand filter: the visitor ticks any number of brands, and a product matches if its brand is one of them. With no brand ticked, every brand matches. The brands offered are the ones the catalog's products have.
- BR-4. Search: a product matches if the search term appears in its title, brand or description, ignoring case. An empty search matches every product.
- BR-5. Filters combine: a product is shown only if it matches the category, the brands and the search together.
- BR-6. Sort orders: Featured (the ERP's order, the default), price low to high, price high to low, and name A to Z. Sorting applies to the filtered list.
- BR-7. The chosen category, brands, search and sort are part of the page address. Opening that address again shows the same products in the same order.
- BR-8. Browsing needs no sign-in. A signed-in customer sees the same catalog as an anonymous visitor.
