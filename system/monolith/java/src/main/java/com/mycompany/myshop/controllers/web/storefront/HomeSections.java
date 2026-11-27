package com.mycompany.myshop.controllers.web.storefront;

import com.mycompany.myshop.core.dtos.ViewProductDetailsResponse;

import java.util.List;
import java.util.Objects;

/**
 * The home page above the full catalog (Retail look): deal hero, round-photo category shortcuts, and the Bestsellers /
 * New arrivals rows. The ERP has no sales figures or listing dates yet, so the rows are hand-picked SKUs (any the
 * catalog no longer carries are skipped); the hero's text and price are read from the catalog, never written here.
 * The same picks as the .NET monolith's _HomeSections and frontend-react's HomeSections.
 */
public record HomeSections(ViewProductDetailsResponse featured,
                           List<CategoryShortcut> categories,
                           List<ViewProductDetailsResponse> bestsellers,
                           List<ViewProductDetailsResponse> newArrivals) {

    static final String FEATURED_SKU = "DELL-U2723QE";
    static final List<String> BESTSELLER_SKUS = List.of("DELL-XPS", "HP-15", "LG-27UL500", "LOGI-MX3S", "LOGI-MXKEYS");
    static final List<String> NEW_ARRIVAL_SKUS = List.of("ASUS-ZENBOOK-14", "LENOVO-T14", "SAMSUNG-S24", "ANKER-HUB-7");

    /** One category link: the photo is the first product photo of the category, or null when none has one. */
    public record CategoryShortcut(String name, String photoUrl) {
    }

    public static HomeSections of(List<ViewProductDetailsResponse> products) {
        var shortcuts = CatalogQuery.categoriesOf(products).stream()
                .map(category -> new CategoryShortcut(category, products.stream()
                        .filter(product -> category.equals(product.getCategory()))
                        .map(ViewProductDetailsResponse::getImageUrl)
                        .filter(url -> url != null && !url.isEmpty())
                        .findFirst().orElse(null)))
                .toList();
        return new HomeSections(
                find(products, FEATURED_SKU), shortcuts, pick(products, BESTSELLER_SKUS), pick(products, NEW_ARRIVAL_SKUS));
    }

    private static ViewProductDetailsResponse find(List<ViewProductDetailsResponse> products, String sku) {
        return products.stream().filter(product -> sku.equals(product.getSku())).findFirst().orElse(null);
    }

    private static List<ViewProductDetailsResponse> pick(List<ViewProductDetailsResponse> products, List<String> skus) {
        return skus.stream().map(sku -> find(products, sku)).filter(Objects::nonNull).toList();
    }
}
