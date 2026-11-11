# Home page redesign — explorations

Styling-only redesign explorations for the storefront home page (catalog). Nothing here is wired into the frontends yet.

**Live canvas (private, owner access):** https://claude.ai/artifact/6PfCB87jVYkf7vfxGhEJBk

## Directions

| # | File | Direction |
|---|---|---|
| 1 | `designs/Main.dc.html` | **Bento**: search-first hero, mixed-size tile grid, light warm neutrals (Geist) |
| 2 | `designs/Split.dc.html` | **Split**: app-like three-pane layout (filters, spec list, live preview), IBM Plex, violet |
| 3 | `designs/Swiss.dc.html` | **Swiss**: black and white, oversized Archivo type, strict 12-column grid, orange accent |
| 4 | `designs/Graphite.dc.html` | **Graphite**: restrained monochrome, black pills, dark hero (Space Grotesk + Inter) |
| 5 | `designs/Tonal.dc.html` | **Tonal**: soft tonal surfaces, rounded, chip filters, blue primary (Manrope) |
| 6 | `designs/Terminal.dc.html` | **Terminal**: dark, spec-dense, monospace details, lime accent |
| 7 | `designs/Aurora.dc.html` | **Aurora**: dark premium showcase with large photography (Inter Tight) |
| 8 | `designs/Market.dc.html` | **Market**: dense big-store layout, deals, filter sidebar, teal + yellow (Figtree) |
| 9 | `designs/Industrial.dc.html` | **Industrial**: hardware-lab look, warm grey, mono spec readouts, orange (Martian Mono) |

Designs 7–9 use photos; 1–6 use icon placeholders. Product names, brands and prices are fictional sample data.

The `.dc.html` files are the canvas source format: they render inside the canvas, not as standalone pages. Image references (`/_blob/...`) point to the canvas copies of the files in `images/`.

## Acceptance-test safety

Every design keeps the hooks the UI drivers in `system-test/*` select on: the `aria-label`s (`Store navigation`, `Search Products`, `Search`, `Cart`, `Cart Item Count`, `Category Filter`, `Brand Filter`, `Sort`, `Clear Filters`, `Product Count`, `Product List`, `Catalog`, `Add to Cart`), roles, visible labels, and the Bootstrap hook classes `.form-check`, `.alert-danger` and `table.table` (coupon table column order). The real redesign must keep these too, or move the drivers to role/ARIA selectors first.

## Images

Photos in `images/` are from Unsplash under the Unsplash License (free, not Unsplash+). See [images/SOURCES.md](images/SOURCES.md) for each photo's page, photographer, and the photos excluded for showing branded products.
