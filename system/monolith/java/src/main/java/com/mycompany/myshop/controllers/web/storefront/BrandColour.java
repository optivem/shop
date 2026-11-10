package com.mycompany.myshop.controllers.web.storefront;

import java.util.List;

/** The tile colour of a brand: the same brand always gets the same colour (storefront-ux §5). */
public final class BrandColour {

    /** For the templates: {@code ${brandColours.of(product.brand)}}. */
    public static final BrandColour INSTANCE = new BrandColour();

    // Dark enough for white text at >= 4.5:1 contrast; the same palette and hash as the other storefronts.
    private static final List<String> TILE_COLOURS = List.of(
            "#1f4e9c", "#7a2e8e", "#0f6b5c", "#9c3d1f", "#3b4a5a", "#8a1c3f", "#2f5d1e", "#5b3a99");

    private BrandColour() {
    }

    public String of(String brand) {
        long hash = 0;
        var text = brand == null ? "" : brand;
        for (int i = 0; i < text.length(); ) {
            var codePoint = text.codePointAt(i);
            hash = (hash * 31 + codePoint) % 2147483647L;
            i += Character.charCount(codePoint);
        }
        return TILE_COLOURS.get((int) (hash % TILE_COLOURS.size()));
    }
}
