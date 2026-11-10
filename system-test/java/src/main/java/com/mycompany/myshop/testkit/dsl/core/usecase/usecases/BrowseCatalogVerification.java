package com.mycompany.myshop.testkit.dsl.core.usecase.usecases;

import com.mycompany.myshop.testkit.driver.port.dtos.BrowseCatalogResponse;
import com.mycompany.myshop.testkit.dsl.core.shared.ResponseVerification;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseContext;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** SKUs are aliases, resolved to the values the ERP was given; everything else is a literal as shown. */
public class BrowseCatalogVerification extends ResponseVerification<BrowseCatalogResponse> {
    public BrowseCatalogVerification(BrowseCatalogResponse response, UseCaseContext context) {
        super(response, context);
    }

    public BrowseCatalogVerification showsProducts(String... skuAliases) {
        var expectedSkus = Arrays.stream(skuAliases).map(getContext()::getParamValue).toList();
        assertThat(listedSkus())
                .as("Catalog products, in order")
                .containsExactlyElementsOf(expectedSkus);
        return this;
    }

    public BrowseCatalogVerification showsNoProducts() {
        assertThat(listedSkus())
                .as("Catalog products")
                .isEmpty();
        return this;
    }

    public BrowseCatalogVerification productCount(int expectedCount) {
        assertThat(getResponse().getProductCount())
                .as("Catalog product count")
                .isEqualTo(expectedCount);
        return this;
    }

    public BrowseCatalogVerification product(String skuAlias, String expectedTitle, String expectedBrand, String expectedPrice) {
        var sku = getContext().getParamValue(skuAlias);
        var product = products().stream()
                .filter(listed -> sku.equals(listed.getSku()))
                .findFirst();
        assertThat(product)
                .as("Catalog should show product '%s'", sku)
                .isPresent();
        assertThat(product.get().getTitle()).as("Title of product '%s'", sku).isEqualTo(expectedTitle);
        assertThat(product.get().getBrand()).as("Brand of product '%s'", sku).isEqualTo(expectedBrand);
        assertThat(product.get().getPrice()).as("Price of product '%s'", sku).isEqualTo(expectedPrice);
        return this;
    }

    public BrowseCatalogVerification categoryOptions(String... expectedCategories) {
        assertThat(getResponse().getCategories())
                .as("Category filter options")
                .containsExactly(expectedCategories);
        return this;
    }

    public BrowseCatalogVerification selectedCategory(String expectedCategory) {
        assertThat(getResponse().getSelectedCategory())
                .as("Category filter selection")
                .isEqualTo(expectedCategory);
        return this;
    }

    public BrowseCatalogVerification brandOptions(String... expectedBrands) {
        assertThat(getResponse().getBrands())
                .as("Brand filter options")
                .containsExactly(expectedBrands);
        return this;
    }

    public BrowseCatalogVerification noBrandTicked() {
        assertThat(getResponse().getTickedBrands())
                .as("Ticked brands")
                .isEmpty();
        return this;
    }

    public BrowseCatalogVerification sort(String expectedSort) {
        assertThat(getResponse().getSort())
                .as("Catalog sort")
                .isEqualTo(expectedSort);
        return this;
    }

    public BrowseCatalogVerification message(String expectedMessage) {
        assertThat(getResponse().getMessage())
                .as("Catalog message")
                .isEqualTo(expectedMessage);
        return this;
    }

    public BrowseCatalogVerification clearFiltersOffered() {
        assertThat(getResponse().getClearFiltersOffered())
                .as("Catalog should offer to clear the filters")
                .isTrue();
        return this;
    }

    private List<String> listedSkus() {
        return products().stream().map(BrowseCatalogResponse.ProductDto::getSku).toList();
    }

    private List<BrowseCatalogResponse.ProductDto> products() {
        assertThat(getResponse())
                .as("Response should not be null")
                .isNotNull();
        assertThat(getResponse().getProducts())
                .as("Products list should not be null")
                .isNotNull();
        return getResponse().getProducts();
    }
}
