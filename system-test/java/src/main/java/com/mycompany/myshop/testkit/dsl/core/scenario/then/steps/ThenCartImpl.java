package com.mycompany.myshop.testkit.dsl.core.scenario.then.steps;

import com.mycompany.myshop.testkit.dsl.core.scenario.ExecutionResultContext;
import com.mycompany.myshop.testkit.dsl.core.shared.ResponseVerification;
import com.mycompany.myshop.testkit.dsl.core.usecase.UseCaseDsl;
import com.mycompany.myshop.testkit.dsl.core.usecase.usecases.ManageCartVerification;
import com.mycompany.myshop.testkit.dsl.port.then.steps.ThenCart;

public class ThenCartImpl<R, V extends ResponseVerification<R>>
        extends BaseThenStep<R, V> implements ThenCart {
    private static final String CHECKOUT_NOTE = "Tax and discounts are calculated at checkout.";

    private final ManageCartVerification verification;

    public ThenCartImpl(UseCaseDsl app, ExecutionResultContext executionResult, V successVerification) {
        super(app, executionResult, successVerification);
        if (!(successVerification instanceof ManageCartVerification cartVerification)) {
            throw new IllegalStateException("Cannot verify the cart: the executed operation did not manage the cart");
        }
        this.verification = cartVerification;
    }

    @Override
    public ThenCartImpl<R, V> showsMessage(String expectedMessage) {
        verification.message(expectedMessage);
        return this;
    }

    @Override
    public ThenCartImpl<R, V> hasCartBadge(int expectedCount) {
        verification.cartItemCount(expectedCount);
        return this;
    }

    @Override
    public ThenCartImpl<R, V> hasNoCartBadge() {
        verification.noCartBadge();
        return this;
    }

    @Override
    public ThenCartImpl<R, V> hasLines(String... skuAliases) {
        verification.lines(skuAliases);
        return this;
    }

    @Override
    public ThenCartImpl<R, V> hasLine(String skuAlias, String expectedTitle, String expectedUnitPrice,
                                      int expectedQuantity, String expectedLineTotal) {
        verification.line(skuAlias, expectedTitle, expectedUnitPrice, expectedQuantity, expectedLineTotal);
        return this;
    }

    @Override
    public ThenCartImpl<R, V> hasLineQuantity(String skuAlias, int expectedQuantity) {
        verification.lineQuantity(skuAlias, expectedQuantity);
        return this;
    }

    @Override
    public ThenCartImpl<R, V> hasLineTotal(String skuAlias, String expectedLineTotal) {
        verification.lineTotal(skuAlias, expectedLineTotal);
        return this;
    }

    @Override
    public ThenCartImpl<R, V> hasLinePrices(String skuAlias, String expectedUnitPrice, String expectedLineTotal) {
        verification.linePrices(skuAlias, expectedUnitPrice, expectedLineTotal);
        return this;
    }

    @Override
    public ThenCartImpl<R, V> hasItemCount(int expectedCount) {
        verification.itemCount(expectedCount);
        return this;
    }

    @Override
    public ThenCartImpl<R, V> hasSubtotal(String expectedSubtotal) {
        verification.subtotal(expectedSubtotal);
        return this;
    }

    @Override
    public ThenCartImpl<R, V> saysTaxAndDiscountsAreCalculatedAtCheckout() {
        verification.checkoutNote(CHECKOUT_NOTE);
        return this;
    }

    @Override
    public ThenCartImpl<R, V> offersStartShopping() {
        verification.startShoppingOffered();
        return this;
    }

    @Override
    public ThenCartImpl<R, V> didNotAskToSignIn() {
        verification.notAskedToSignIn();
        return this;
    }

    @Override
    public ThenCartImpl<R, V> and() {
        return this;
    }
}
