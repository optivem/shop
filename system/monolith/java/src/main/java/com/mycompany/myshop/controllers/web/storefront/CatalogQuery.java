package com.mycompany.myshop.controllers.web.storefront;

import com.mycompany.myshop.core.dtos.ViewProductDetailsResponse;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

/**
 * Catalog filtering and sorting (UC-006 BR-2..BR-7). The catalog is small and the ERP has no query API, so
 * filtering happens here over the full list, and the query lives in the page address.
 *
 * @param q        search term over title, brand and description, case-insensitive
 * @param category one category, or "" for All
 * @param brands   any number of brands; none means every brand
 * @param sort     one of {@link #SORT_OPTIONS}
 */
public record CatalogQuery(String q, String category, List<String> brands, String sort) {

    public static final String FEATURED = "featured";

    public static final List<SortOption> SORT_OPTIONS = List.of(
            new SortOption(FEATURED, "Featured"),
            new SortOption("price-asc", "Price: low to high"),
            new SortOption("price-desc", "Price: high to low"),
            new SortOption("name", "Name: A to Z"));

    public static CatalogQuery parse(String q, String category, List<String> brands, String sort) {
        var knownSort = SORT_OPTIONS.stream().anyMatch(option -> option.value().equals(sort)) ? sort : FEATURED;
        var tickedBrands = brands == null ? List.<String>of()
                : brands.stream().filter(Objects::nonNull).filter(brand -> !brand.isEmpty()).toList();
        return new CatalogQuery(q == null ? "" : q, category == null ? "" : category, tickedBrands, knownSort);
    }

    /** The page address for this query; defaults are left out so the plain catalog is just "/". */
    public String path() {
        var params = new ArrayList<String>();
        if (!q.isBlank()) {
            params.add("q=" + encode(q.trim()));
        }
        if (!category.isEmpty()) {
            params.add("category=" + encode(category));
        }
        brands.forEach(brand -> params.add("brand=" + encode(brand)));
        if (!FEATURED.equals(sort)) {
            params.add("sort=" + encode(sort));
        }
        return params.isEmpty() ? "/" : "/?" + String.join("&", params);
    }

    public boolean isBrandTicked(String brand) {
        return brands.contains(brand);
    }

    /** The products matching every filter, in the chosen order (Featured keeps the ERP order). */
    public List<ViewProductDetailsResponse> apply(List<ViewProductDetailsResponse> products) {
        var filtered = products.stream().filter(this::matches).toList();
        var comparator = comparator();
        return comparator == null ? filtered : filtered.stream().sorted(comparator).toList();
    }

    public static List<String> categoriesOf(List<ViewProductDetailsResponse> products) {
        return distinct(products, ViewProductDetailsResponse::getCategory);
    }

    public static List<String> brandsOf(List<ViewProductDetailsResponse> products) {
        return distinct(products, ViewProductDetailsResponse::getBrand);
    }

    /** Distinct non-empty values, in first-seen (ERP) order. */
    private static List<String> distinct(List<ViewProductDetailsResponse> products,
                                         Function<ViewProductDetailsResponse, String> field) {
        var values = new LinkedHashSet<String>();
        products.stream().map(field).filter(Objects::nonNull).filter(value -> !value.isEmpty()).forEach(values::add);
        return List.copyOf(values);
    }

    private boolean matches(ViewProductDetailsResponse product) {
        var term = q.trim().toLowerCase(Locale.ROOT);
        var matchesTerm = term.isEmpty() || contains(product.getTitle(), term) || contains(product.getBrand(), term)
                || contains(product.getDescription(), term);
        var matchesCategory = category.isEmpty() || category.equals(product.getCategory());
        var matchesBrand = brands.isEmpty() || brands.contains(product.getBrand());
        return matchesTerm && matchesCategory && matchesBrand;
    }

    private static boolean contains(String text, String term) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(term);
    }

    private Comparator<ViewProductDetailsResponse> comparator() {
        return switch (sort) {
            case "price-asc" -> Comparator.comparing(ViewProductDetailsResponse::getPrice);
            case "price-desc" -> Comparator.comparing(ViewProductDetailsResponse::getPrice).reversed();
            case "name" -> Comparator.comparing(ViewProductDetailsResponse::getTitle, String.CASE_INSENSITIVE_ORDER);
            default -> null;
        };
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public record SortOption(String value, String label) {
    }
}
