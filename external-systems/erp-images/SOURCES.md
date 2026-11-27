# ERP product photos (Unsplash License, free; not Unsplash+)

The made-up ERP owns one photo per product, served as `/erp/images/<PRODUCT-ID>.jpg` by both the simulator (`external-systems/simulators`) and the WireMock stub (`external-systems/stubs`, mapping `erp-images.json`). The files here are the only copy: both Docker images build from `external-systems/` and copy this folder in, and the local stub compose services bind-mount it.

License: https://unsplash.com/license (free commercial use and modification; attribution not required but given). Checked 2026-11-27.
Excluded, as for the promo photos (`docs/design/home-redesign/images/SOURCES.md`): photos where a brand's logo is the subject (for example an Apple logo), since the license does not cover trademarks. The Logitech mouse photo is kept because it shows the product's own brand.

Each file is cropped to 4:3 and resized to 600x450 JPEG. Photos stand in for the product category; they are not the exact model.

| Product id | File | Photographer | Profile | Photo page |
|---|---|---|---|---|
| HP-15 | HP-15.jpg | Dhony Koswara | https://unsplash.com/@dhony24 | https://unsplash.com/photos/btVTuAIhvk8 |
| DELL-XPS | DELL-XPS.jpg | Lasse Jensen | https://unsplash.com/@maybejensen | https://unsplash.com/photos/fSSO5U_iwzk |
| LENOVO-T14 | LENOVO-T14.jpg | Faraaz Zuberi | https://unsplash.com/@ffz_20 | https://unsplash.com/photos/YoIq2GyYcAU |
| ASUS-ZENBOOK-14 | ASUS-ZENBOOK-14.jpg | 2H Media | https://unsplash.com/@2hmedia | https://unsplash.com/photos/YWIgfYaTvYw |
| DELL-U2723QE | DELL-U2723QE.jpg | Huy Phan | https://unsplash.com/@huyphan2602 | https://unsplash.com/photos/VXpeQ3GetDU |
| LG-27UL500 | LG-27UL500.jpg | Roberto Nickson | https://unsplash.com/@rpnickson | https://unsplash.com/photos/Gvm2wM3V5PA |
| SAMSUNG-S24 | SAMSUNG-S24.jpg | Nikita Kachanovsky | https://unsplash.com/@nkachanovskyyy | https://unsplash.com/photos/OVbeSXRk_9E |
| LOGI-MX3S | LOGI-MX3S.jpg | Ravi Palwe | https://unsplash.com/@ravipalwe | https://unsplash.com/photos/zrlWSkTYLu0 |
| LOGI-MXKEYS | LOGI-MXKEYS.jpg | Jay Zhang | https://unsplash.com/@jay_zhang | https://unsplash.com/photos/U1I0IoQx3Yo |
| ANKER-HUB-7 | ANKER-HUB-7.jpg | Lasse Jensen | https://unsplash.com/@maybejensen | https://unsplash.com/photos/QirgO8svCBI |

HP-15, DELL-U2723QE and LOGI-MXKEYS reuse the promo photos (`laptop.jpg`, `monitor.jpg`, `keyboard.jpg`) listed in `docs/design/home-redesign/images/SOURCES.md`.
