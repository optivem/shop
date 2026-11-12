package com.mycompany.myshop.testkit.dsl.core.usecase.usecases;

import com.mycompany.myshop.testkit.driver.port.dtos.CheckOutResponse;
import com.mycompany.myshop.testkit.driver.port.dtos.ViewProductResponse;
import com.mycompany.myshop.testkit.dsl.core.shared.ResponseVerification;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseContext;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SKUs are aliases, resolved to the values the ERP was given; messages may name SKU and coupon aliases, which are
 * expanded; everything else is a literal as shown.
 */
public class CheckOutVerification extends ResponseVerification<CheckOutResponse> {
    public CheckOutVerification(CheckOutResponse response, UseCaseContext context) {
        super(response, context);
    }

    /** Passes each confirmed order number, in the order shown, to {@code action}; the numbers must each be different. */
    public void forEachConfirmedOrder(Consumer<String> action) {
        orderNumbers().forEach(action);
    }

    /** The confirmed orders' products, in the order shown, are exactly these. */
    public CheckOutVerification placedFor(Map<String, String> placedSkus, String... expectedSkuAliases) {
        assertThat(List.copyOf(placedSkus.values())).as("Products of the confirmed orders, in order")
                .containsExactlyElementsOf(skus(expectedSkuAliases));
        return this;
    }

    /**
     * Passes the number of the order placed for this product to {@code action}, given each confirmed order's SKU by
     * order number; fails if the confirmation shows no order for it.
     */
    public void forOrderPlacedFor(Map<String, String> placedSkus, String skuAlias, Consumer<String> action) {
        var sku = getContext().getParamValue(skuAlias);
        var orderNumber = placedSkus.entrySet().stream()
                .filter(entry -> sku.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new AssertionError("The confirmation shows no order for " + skuAlias
                        + "; it shows orders for " + placedSkus.values()));
        action.accept(orderNumber);
    }

    public CheckOutVerification shownPage(String expectedPage) {
        assertThat(response().getShownPage()).as("Page the customer ended up on").isEqualTo(expectedPage);
        return this;
    }

    public CheckOutVerification message(String expectedMessage) {
        assertThat(response().getMessage()).as("Message shown").isEqualTo(getContext().expandAliases(expectedMessage));
        return this;
    }

    /** One of the lines under the error notification's message. */
    public CheckOutVerification messageDetail(String expectedDetail) {
        assertThat(response().getMessageDetails()).as("Lines under the message").contains(getContext().expandAliases(expectedDetail));
        return this;
    }

    /** Exactly these review lines, in this order. */
    public CheckOutVerification reviewLines(String... expectedSkuAliases) {
        assertThat(reviewLines().stream().map(CheckOutResponse.ReviewLineDto::getSku).toList())
                .as("Order review lines, in order")
                .containsExactlyElementsOf(skus(expectedSkuAliases));
        return this;
    }

    public CheckOutVerification reviewQuantity(String skuAlias, int expectedQuantity) {
        assertThat(reviewLine(skuAlias).getQuantity()).as("Quantity of review line " + skuAlias).isEqualTo(expectedQuantity);
        return this;
    }

    public CheckOutVerification reviewLineTotal(String skuAlias, String expectedLineTotal) {
        assertThat(reviewLine(skuAlias).getLineTotal()).as("Line total of review line " + skuAlias).isEqualTo(expectedLineTotal);
        return this;
    }

    public CheckOutVerification reviewSubtotal(String expectedSubtotal) {
        assertThat(response().getReviewSubtotal()).as("Order review subtotal").isEqualTo(expectedSubtotal);
        return this;
    }

    public CheckOutVerification openedTax(String expectedCountry, String expectedRate) {
        assertThat(response().getOpenedTaxCountry()).as("Tax country when the checkout opened").isEqualTo(expectedCountry);
        assertThat(response().getOpenedEstimatedTaxRate()).as("Estimated tax rate when the checkout opened").isEqualTo(expectedRate);
        return this;
    }

    public CheckOutVerification tax(String expectedCountry, String expectedRate) {
        assertThat(response().getTaxCountry()).as("Tax country").isEqualTo(expectedCountry);
        assertThat(response().getEstimatedTaxRate()).as("Estimated tax rate").isEqualTo(expectedRate);
        return this;
    }

    public CheckOutVerification reviewNote(String expectedNote) {
        assertThat(response().getReviewNote()).as("Order review note").isEqualTo(expectedNote);
        return this;
    }

    public CheckOutVerification savedAddressSelected(String expectedAddress) {
        assertThat(response().getSavedAddress()).as("Saved address shown").isEqualTo(expectedAddress);
        assertThat(response().getSavedAddressSelected()).as("Saved address should be selected").isTrue();
        return this;
    }

    public CheckOutVerification addressFormOpen() {
        assertThat(response().getAddressFormOpen()).as("Shipping address form should be open").isTrue();
        return this;
    }

    public CheckOutVerification shippingName(String expectedFirstName, String expectedLastName) {
        assertThat(response().getShippingFirstName()).as("Shipping first name").isEqualTo(expectedFirstName);
        assertThat(response().getShippingLastName()).as("Shipping last name").isEqualTo(expectedLastName);
        return this;
    }

    public CheckOutVerification saveAsDefaultTicked() {
        assertThat(response().getSaveAsDefaultTicked()).as("\"Save as my default shipping address\" should be ticked").isTrue();
        return this;
    }

    public CheckOutVerification thanks(String expectedThanks) {
        assertThat(response().getThanks()).as("Confirmation heading").isEqualTo(expectedThanks);
        return this;
    }

    /** The confirmed order numbers, in the order shown; each distinct. */
    private List<String> orderNumbers() {
        assertThat(response().getConfirmedOrders()).as("Confirmed orders should have been read").isNotNull();
        var numbers = response().getConfirmedOrders().stream().map(CheckOutResponse.ConfirmedOrderDto::getOrderNumber).toList();
        assertThat(numbers).as("Confirmed order numbers should each be different").doesNotHaveDuplicates();
        return numbers;
    }

    public CheckOutVerification noOrderConfirmed() {
        assertThat(orderNumbers()).as("Confirmed orders").isEmpty();
        return this;
    }

    public CheckOutVerification eachOrderLinksToItsDetails() {
        assertThat(response().getConfirmedOrders()).as("Confirmed orders").isNotEmpty();
        for (var order : response().getConfirmedOrders()) {
            var encoded = URLEncoder.encode(order.getOrderNumber(), StandardCharsets.UTF_8).replace("+", "%20");
            assertThat(order.getLink()).as("Link of confirmed order " + order.getOrderNumber())
                    .contains("/order-details")
                    .contains(encoded);
        }
        return this;
    }

    public CheckOutVerification viewMyOrdersAndContinueShoppingOffered() {
        assertThat(response().getViewMyOrdersOffered()).as("Confirmation should offer \"View my orders\"").isTrue();
        assertThat(response().getContinueShoppingOffered()).as("Confirmation should offer \"Continue shopping\"").isTrue();
        return this;
    }

    public CheckOutVerification rejectedLine(String skuAlias, String expectedReason) {
        var sku = getContext().getParamValue(skuAlias);
        var line = rejectedLines().stream()
                .filter(rejected -> sku.equals(rejected.getSku()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("The confirmation shows no rejected line " + skuAlias + "; it shows " + rejectedLines()));
        assertThat(line.getReason()).as("Why " + skuAlias + " was rejected").isEqualTo(getContext().expandAliases(expectedReason));
        return this;
    }

    public CheckOutVerification noRejectedLines() {
        assertThat(rejectedLines()).as("Rejected lines").isEmpty();
        return this;
    }

    /** Exactly these cart lines, in this order; none for an empty cart. */
    public CheckOutVerification cartLines(String... expectedSkuAliases) {
        assertThat(cart().stream().map(ViewProductResponse.CartLineDto::getSku).toList())
                .as("Cart lines after checkout, in order")
                .containsExactlyElementsOf(skus(expectedSkuAliases));
        return this;
    }

    public CheckOutVerification cartLineQuantity(String skuAlias, int expectedQuantity) {
        var sku = getContext().getParamValue(skuAlias);
        var line = cart().stream()
                .filter(cartLine -> sku.equals(cartLine.getSku()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("The cart has no line " + skuAlias + "; it has " + cart()));
        assertThat(line.getQuantity()).as("Quantity of cart line " + skuAlias).isEqualTo(expectedQuantity);
        return this;
    }

    private List<String> skus(String... skuAliases) {
        return Arrays.stream(skuAliases).map(getContext()::getParamValue).toList();
    }

    private CheckOutResponse.ReviewLineDto reviewLine(String skuAlias) {
        var sku = getContext().getParamValue(skuAlias);
        return reviewLines().stream()
                .filter(line -> sku.equals(line.getSku()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("The order review has no line " + skuAlias + "; it has " + reviewLines()));
    }

    private List<CheckOutResponse.ReviewLineDto> reviewLines() {
        assertThat(response().getReviewLines()).as("Order review should have been read").isNotNull();
        return response().getReviewLines();
    }

    private List<CheckOutResponse.RejectedLineDto> rejectedLines() {
        assertThat(response().getRejectedLines()).as("Rejected lines should have been read").isNotNull();
        return response().getRejectedLines();
    }

    private List<ViewProductResponse.CartLineDto> cart() {
        assertThat(response().getCartLines()).as("Cart should have been read after placing the order").isNotNull();
        return response().getCartLines();
    }

    private CheckOutResponse response() {
        assertThat(getResponse()).as("Response should not be null").isNotNull();
        return getResponse();
    }
}
