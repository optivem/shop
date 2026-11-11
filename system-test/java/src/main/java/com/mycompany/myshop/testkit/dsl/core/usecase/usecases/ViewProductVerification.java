package com.mycompany.myshop.testkit.dsl.core.usecase.usecases;

import com.mycompany.myshop.testkit.driver.port.dtos.ViewProductResponse;
import com.mycompany.myshop.testkit.dsl.core.shared.ResponseVerification;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseContext;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** SKUs are aliases, resolved to the values the ERP was given; everything else is a literal as shown. */
public class ViewProductVerification extends ResponseVerification<ViewProductResponse> {
    public ViewProductVerification(ViewProductResponse response, UseCaseContext context) {
        super(response, context);
    }

    public ViewProductVerification title(String expectedTitle) {
        assertThat(response().getTitle()).as("Product title").isEqualTo(expectedTitle);
        return this;
    }

    public ViewProductVerification brand(String expectedBrand) {
        assertThat(response().getBrand()).as("Product brand").isEqualTo(expectedBrand);
        return this;
    }

    public ViewProductVerification category(String expectedCategory) {
        assertThat(response().getCategory()).as("Product category").isEqualTo(expectedCategory);
        return this;
    }

    public ViewProductVerification sku(String expectedSkuAlias) {
        assertThat(response().getSku()).as("Product SKU").isEqualTo(getContext().getParamValue(expectedSkuAlias));
        return this;
    }

    public ViewProductVerification price(String expectedPrice) {
        assertThat(response().getPrice()).as("Product price").isEqualTo(expectedPrice);
        return this;
    }

    public ViewProductVerification description(String expectedDescription) {
        assertThat(response().getDescription()).as("Product description").isEqualTo(expectedDescription);
        return this;
    }

    public ViewProductVerification breadcrumbs(String... expectedBreadcrumbs) {
        assertThat(response().getBreadcrumbs()).as("Breadcrumb trail").containsExactly(expectedBreadcrumbs);
        return this;
    }

    public ViewProductVerification quantity(int expectedQuantity) {
        assertThat(response().getQuantity()).as("Quantity").isEqualTo(String.valueOf(expectedQuantity));
        return this;
    }

    public ViewProductVerification message(String expectedMessage) {
        assertThat(response().getMessage()).as("Product page message").isEqualTo(expectedMessage);
        return this;
    }

    public ViewProductVerification backToCatalogOffered() {
        assertThat(response().getBackToCatalogOffered()).as("Product page should offer a link back to the catalog").isTrue();
        return this;
    }

    public ViewProductVerification addToCartNotOffered() {
        assertThat(response().getAddToCartOffered()).as("Product page should not offer to add to the cart").isFalse();
        return this;
    }

    public ViewProductVerification cartItemCount(int expectedCount) {
        assertThat(response().getCartItemCount()).as("Cart badge").isEqualTo(expectedCount);
        return this;
    }

    public ViewProductVerification onPageOfProduct(String skuAlias) {
        assertThat(response().getPageSku()).as("Product page the visitor is on").isEqualTo(getContext().getParamValue(skuAlias));
        return this;
    }

    /** Exactly these lines, SKU alias to quantity, in any order. */
    public ViewProductVerification cartContains(Map<String, Integer> expectedLines) {
        var expected = expectedLines.entrySet().stream()
                .map(line -> ViewProductResponse.CartLineDto.builder()
                        .sku(getContext().getParamValue(line.getKey()))
                        .quantity(line.getValue())
                        .build())
                .toList();
        assertThat(cartLines()).as("Cart lines").containsExactlyInAnyOrderElementsOf(expected);
        return this;
    }

    public ViewProductVerification cartIsEmpty() {
        assertThat(cartLines()).as("Cart lines").isEmpty();
        return this;
    }

    public ViewProductVerification catalogShowsProducts(String... skuAliases) {
        var expectedSkus = Arrays.stream(skuAliases).map(getContext()::getParamValue).toList();
        assertThat(response().getCatalogSkus())
                .as("Catalog products, in order")
                .isNotNull()
                .containsExactlyElementsOf(expectedSkus);
        return this;
    }

    private List<ViewProductResponse.CartLineDto> cartLines() {
        assertThat(response().getCartLines()).as("Cart lines should have been read").isNotNull();
        return response().getCartLines();
    }

    private ViewProductResponse response() {
        assertThat(getResponse()).as("Response should not be null").isNotNull();
        return getResponse();
    }
}
