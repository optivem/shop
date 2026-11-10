package com.mycompany.myshop.controllers.web.storefront;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import com.mycompany.myshop.core.dtos.ViewProductDetailsResponse;
import org.junit.jupiter.api.Test;

class CatalogQueryTest {

    private static final List<ViewProductDetailsResponse> PRODUCTS = List.of(
            product("HP-15", "HP Laptop 15", "HP", "Laptops", "699.99", "Everyday 15-inch laptop"),
            product("DELL-XPS", "Dell XPS 13", "Dell", "Laptops", "1299.99", "Compact 13-inch ultrabook"),
            product("LENOVO-T14", "Lenovo ThinkPad T14", "Lenovo", "Laptops", "999.99", "Business laptop"),
            product("DELL-U2723QE", "Dell UltraSharp 27 4K", "Dell", "Monitors", "579.99", "27-inch USB-C monitor"),
            product("LOGI-MX3S", "Logitech MX Master 3S", "Logitech", "Accessories", "99.99", "Wireless mouse"));

    @Test
    void featuredKeepsTheErpOrder() {
        assertThat(skus(CatalogQuery.parse(null, null, null, null))).containsExactly(
                "HP-15", "DELL-XPS", "LENOVO-T14", "DELL-U2723QE", "LOGI-MX3S");
    }

    @Test
    void filtersByCategoryAndBrandTogether() {
        assertThat(skus(CatalogQuery.parse("", "Laptops", List.of("Dell"), ""))).containsExactly("DELL-XPS");
    }

    @Test
    void searchesTitleBrandAndDescriptionIgnoringCase() {
        assertThat(skus(CatalogQuery.parse("MOUSE", null, null, null))).containsExactly("LOGI-MX3S");
        assertThat(skus(CatalogQuery.parse("dell", null, null, null))).containsExactly("DELL-XPS", "DELL-U2723QE");
    }

    @Test
    void sortsOnlyTheFilteredProducts() {
        assertThat(skus(CatalogQuery.parse(null, "Laptops", null, "price-desc"))).containsExactly(
                "DELL-XPS", "LENOVO-T14", "HP-15");
    }

    @Test
    void anUnknownSortIsFeatured() {
        assertThat(CatalogQuery.parse(null, null, null, "cheapest").sort()).isEqualTo(CatalogQuery.FEATURED);
    }

    @Test
    void thePathLeavesOutTheDefaults() {
        assertThat(CatalogQuery.parse("", "", List.of(""), "featured").path()).isEqualTo("/");
        assertThat(CatalogQuery.parse("usb c", "Laptops", List.of("HP", "Dell"), "name").path())
                .isEqualTo("/?q=usb+c&category=Laptops&brand=HP&brand=Dell&sort=name");
    }

    @Test
    void categoriesAndBrandsComeInErpOrder() {
        assertThat(CatalogQuery.categoriesOf(PRODUCTS)).containsExactly("Laptops", "Monitors", "Accessories");
        assertThat(CatalogQuery.brandsOf(PRODUCTS)).containsExactly("HP", "Dell", "Lenovo", "Logitech");
    }

    private static List<String> skus(CatalogQuery query) {
        return query.apply(PRODUCTS).stream().map(ViewProductDetailsResponse::getSku).toList();
    }

    private static ViewProductDetailsResponse product(String sku, String title, String brand, String category, String price,
                                                      String description) {
        var product = new ViewProductDetailsResponse();
        product.setSku(sku);
        product.setTitle(title);
        product.setBrand(brand);
        product.setCategory(category);
        product.setPrice(new BigDecimal(price));
        product.setDescription(description);
        return product;
    }
}
