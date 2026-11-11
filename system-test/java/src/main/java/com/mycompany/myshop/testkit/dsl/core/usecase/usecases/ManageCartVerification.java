package com.mycompany.myshop.testkit.dsl.core.usecase.usecases;

import com.mycompany.myshop.testkit.driver.port.dtos.ManageCartResponse;
import com.mycompany.myshop.testkit.dsl.core.shared.ResponseVerification;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseContext;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** SKUs are aliases, resolved to the values the ERP was given; everything else is a literal as shown. */
public class ManageCartVerification extends ResponseVerification<ManageCartResponse> {
    public ManageCartVerification(ManageCartResponse response, UseCaseContext context) {
        super(response, context);
    }

    public ManageCartVerification message(String expectedMessage) {
        assertThat(response().getMessage()).as("Message shown").isEqualTo(expectedMessage);
        return this;
    }

    public ManageCartVerification cartItemCount(int expectedCount) {
        assertThat(response().getCartItemCount()).as("Cart badge").isEqualTo(expectedCount);
        return this;
    }

    public ManageCartVerification noCartBadge() {
        assertThat(response().getCartItemCount()).as("Cart badge should be hidden").isZero();
        return this;
    }

    /** Exactly these lines, in this order. */
    public ManageCartVerification lines(String... expectedSkuAliases) {
        var expectedSkus = Arrays.stream(expectedSkuAliases).map(getContext()::getParamValue).toList();
        assertThat(lines().stream().map(ManageCartResponse.CartLineDto::getSku).toList())
                .as("Cart lines, in order")
                .containsExactlyElementsOf(expectedSkus);
        return this;
    }

    public ManageCartVerification line(String skuAlias, String expectedTitle, String expectedUnitPrice,
                                       int expectedQuantity, String expectedLineTotal) {
        assertThat(line(skuAlias).getTitle()).as("Title of cart line " + skuAlias).isEqualTo(expectedTitle);
        return linePrices(skuAlias, expectedUnitPrice, expectedLineTotal)
                .lineQuantity(skuAlias, expectedQuantity);
    }

    public ManageCartVerification lineQuantity(String skuAlias, int expectedQuantity) {
        assertThat(line(skuAlias).getQuantity()).as("Quantity of cart line " + skuAlias).isEqualTo(expectedQuantity);
        return this;
    }

    public ManageCartVerification linePrices(String skuAlias, String expectedUnitPrice, String expectedLineTotal) {
        assertThat(line(skuAlias).getUnitPrice()).as("Unit price of cart line " + skuAlias).isEqualTo(expectedUnitPrice);
        return lineTotal(skuAlias, expectedLineTotal);
    }

    public ManageCartVerification lineTotal(String skuAlias, String expectedLineTotal) {
        assertThat(line(skuAlias).getLineTotal()).as("Line total of cart line " + skuAlias).isEqualTo(expectedLineTotal);
        return this;
    }

    public ManageCartVerification itemCount(int expectedCount) {
        assertThat(response().getItemCount()).as("Number of items the cart shows").isEqualTo(expectedCount);
        return this;
    }

    public ManageCartVerification subtotal(String expectedSubtotal) {
        assertThat(response().getSubtotal()).as("Cart subtotal").isEqualTo(expectedSubtotal);
        return this;
    }

    public ManageCartVerification checkoutNote(String expectedNote) {
        assertThat(response().getCheckoutNote()).as("Cart note about checkout").isEqualTo(expectedNote);
        return this;
    }

    public ManageCartVerification startShoppingOffered() {
        assertThat(response().getStartShoppingOffered()).as("Cart should offer a link to start shopping").isTrue();
        return this;
    }

    public ManageCartVerification notAskedToSignIn() {
        assertThat(response().getAskedToSignIn()).as("Visitor should not be asked to sign in").isFalse();
        return this;
    }

    private ManageCartResponse.CartLineDto line(String skuAlias) {
        var sku = getContext().getParamValue(skuAlias);
        return lines().stream()
                .filter(line -> sku.equals(line.getSku()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("The cart has no line " + skuAlias + "; it has " + lines()));
    }

    private List<ManageCartResponse.CartLineDto> lines() {
        assertThat(response().getLines()).as("Cart lines should have been read").isNotNull();
        return response().getLines();
    }

    private ManageCartResponse response() {
        assertThat(getResponse()).as("Response should not be null").isNotNull();
        return getResponse();
    }
}
