package com.mycompany.myshop.testkit.dsl.core.scenario.then.steps;

import com.mycompany.myshop.testkit.driver.port.dtos.CheckOutResponse;
import com.mycompany.myshop.testkit.dsl.core.scenario.ExecutionResultContext;
import com.mycompany.myshop.testkit.dsl.core.shared.ResponseVerification;
import com.mycompany.myshop.testkit.dsl.core.usecase.UseCaseDsl;
import com.mycompany.myshop.testkit.dsl.core.usecase.usecases.CheckOutVerification;
import com.mycompany.myshop.testkit.dsl.port.then.steps.ThenCheckout;

import java.util.Map;

/**
 * The confirmation shows order numbers only; which product each order is for is looked up on the order itself, the
 * way the confirmation's links lead to it.
 */
public class ThenCheckoutImpl<R, V extends ResponseVerification<R>>
        extends BaseThenStep<R, V> implements ThenCheckout {
    private static final String REVIEW_NOTE = "Discount and tax are worked out per order when it is placed.";
    private static final String THANKS = "Thank you for your order!";

    private final CheckOutVerification verification;

    public ThenCheckoutImpl(UseCaseDsl app, ExecutionResultContext executionResult, V successVerification) {
        super(app, executionResult, successVerification);
        this.verification = checkoutVerification(successVerification);
    }

    /** The number of the order the checkout placed for this product. */
    static String orderNumberFor(UseCaseDsl app, Object successVerification, String skuAlias) {
        var verification = checkoutVerification(successVerification);
        return verification.orderNumberFor(placedSkus(app, verification), skuAlias);
    }

    private static Map<String, String> placedSkus(UseCaseDsl app, CheckOutVerification verification) {
        return verification.placedSkus(orderNumber ->
                app.myShop().viewOrder().orderNumber(orderNumber).execute().shouldSucceed().orderedSku());
    }

    private static CheckOutVerification checkoutVerification(Object successVerification) {
        if (!(successVerification instanceof CheckOutVerification checkOutVerification)) {
            throw new IllegalStateException("Cannot verify the checkout: the executed operation was not a checkout");
        }
        return checkOutVerification;
    }

    @Override
    public ThenCheckoutImpl<R, V> isShown() {
        verification.shownPage(CheckOutResponse.CHECKOUT);
        return this;
    }

    @Override
    public ThenCheckoutImpl<R, V> showsTheCart() {
        verification.shownPage(CheckOutResponse.CART);
        return this;
    }

    @Override
    public ThenCheckoutImpl<R, V> hasReviewLines(String... skuAliases) {
        verification.reviewLines(skuAliases);
        return this;
    }

    @Override
    public ThenCheckoutImpl<R, V> hasReviewLine(String skuAlias, int expectedQuantity, String expectedLineTotal) {
        verification.reviewQuantity(skuAlias, expectedQuantity).reviewLineTotal(skuAlias, expectedLineTotal);
        return this;
    }

    @Override
    public ThenCheckoutImpl<R, V> hasReviewQuantity(String skuAlias, int expectedQuantity) {
        verification.reviewQuantity(skuAlias, expectedQuantity);
        return this;
    }

    @Override
    public ThenCheckoutImpl<R, V> hasReviewSubtotal(String expectedSubtotal) {
        verification.reviewSubtotal(expectedSubtotal);
        return this;
    }

    @Override
    public ThenCheckoutImpl<R, V> hadTaxWhenOpened(String expectedCountry, String expectedRate) {
        verification.openedTax(expectedCountry, expectedRate);
        return this;
    }

    @Override
    public ThenCheckoutImpl<R, V> hasTax(String expectedCountry, String expectedRate) {
        verification.tax(expectedCountry, expectedRate);
        return this;
    }

    @Override
    public ThenCheckoutImpl<R, V> saysDiscountAndTaxAreWorkedOutPerOrder() {
        verification.reviewNote(REVIEW_NOTE);
        return this;
    }

    @Override
    public ThenCheckoutImpl<R, V> hasSavedAddressSelected(String expectedAddress) {
        verification.savedAddressSelected(expectedAddress);
        return this;
    }

    @Override
    public ThenCheckoutImpl<R, V> showsMessage(String expectedMessage) {
        verification.message(expectedMessage);
        return this;
    }

    @Override
    public ThenCheckoutImpl<R, V> showsMessageDetail(String expectedDetail) {
        verification.messageDetail(expectedDetail);
        return this;
    }

    @Override
    public ThenCheckoutImpl<R, V> saysRejected(String skuAlias, String expectedReason) {
        verification.messageDetail(skuAlias + ": " + expectedReason);
        return this;
    }

    @Override
    public ThenCheckoutImpl<R, V> thanksForTheOrder() {
        verification.thanks(THANKS);
        return this;
    }

    @Override
    public ThenCheckoutImpl<R, V> confirmsOrdersFor(String... skuAliases) {
        verification.shownPage(CheckOutResponse.CONFIRMATION);
        verification.placedFor(placedSkus(app, verification), skuAliases);
        return this;
    }

    @Override
    public ThenCheckoutImpl<R, V> confirmsNoOrder() {
        verification.noOrderConfirmed();
        return this;
    }

    @Override
    public ThenCheckoutImpl<R, V> linksEachOrderToItsDetails() {
        verification.eachOrderLinksToItsDetails();
        return this;
    }

    @Override
    public ThenCheckoutImpl<R, V> offersViewMyOrdersAndContinueShopping() {
        verification.viewMyOrdersAndContinueShoppingOffered();
        return this;
    }

    @Override
    public ThenCheckoutImpl<R, V> confirmsRejected(String skuAlias, String expectedReason) {
        verification.rejectedLine(skuAlias, expectedReason);
        return this;
    }

    @Override
    public ThenCheckoutImpl<R, V> confirmsNothingRejected() {
        verification.noRejectedLines();
        return this;
    }

    @Override
    public ThenCheckoutImpl<R, V> hasCartLines(String... skuAliases) {
        verification.cartLines(skuAliases);
        return this;
    }

    @Override
    public ThenCheckoutImpl<R, V> hasCartLineQuantity(String skuAlias, int expectedQuantity) {
        verification.cartLineQuantity(skuAlias, expectedQuantity);
        return this;
    }

    @Override
    public ThenCheckoutImpl<R, V> and() {
        return this;
    }
}
