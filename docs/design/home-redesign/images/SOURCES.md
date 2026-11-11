# Image sources (Unsplash License, free; not Unsplash+)

License: https://unsplash.com/license (free commercial use and modification; attribution not required but given). Checked 2026-11-11.
Excluded: photos showing identifiable branded products (Apple Watch Ultra, AirPods), since the license does not cover trademarks or product design.

| File | Photo page | Photographer |
|---|---|---|
| laptop.jpg | https://unsplash.com/photos/btVTuAIhvk8 | Dhony Koswara (@dhony24) |
| monitor.jpg | https://unsplash.com/photos/VXpeQ3GetDU | Huy Phan (@huyphan2602) |
| headphones.jpg | https://unsplash.com/photos/oBI4NBP2Lhg | C D-X (@cdx2) |
| keyboard.jpg | https://unsplash.com/photos/U1I0IoQx3Yo | Jay Zhang (@jay_zhang) |
| cable.jpg | https://unsplash.com/photos/4xMAiJZPQXI | Marcus Urbenz (@marcusurbenz) |

## Shipped in frontend-react

Copied unchanged to `system/multitier/frontend-react/public/images/promo/` for the home hero and promo tiles, and credited in the footer (`PHOTO_CREDITS` in `src/components/StoreFooter.tsx`):

| File | Used in | Credit |
|---|---|---|
| laptop.jpg | Home hero ("New season laptops", links to Laptops) | Dhony Koswara |
| monitor.jpg | Promo tile "Shop monitors" (links to Monitors) | Huy Phan |
| keyboard.jpg | Promo tile "Build your desk setup" (links to Accessories) | Jay Zhang |

The same three files are also copied unchanged to `system/monolith/dotnet/wwwroot/images/promo/` (same tiles in `Pages/Index.cshtml`, same credits in the footer of `Pages/Shared/_Layout.cshtml`).

They also ship in the Java monolith, copied unchanged to `system/monolith/java/src/main/resources/static/css/images/promo/` (same tiles in `templates/catalog.html`, same credits in the footer of `templates/layout.html`).

They also ship in the TypeScript monolith, copied unchanged to `system/monolith/typescript/public/images/promo/` (same tiles in the catalog view, same credits in the store footer).

Not shipped: headphones.jpg (the catalog has no Audio category), cable.jpg. Product cards and the product page show the catalog placeholder (`ProductImage`), never stock photos.
