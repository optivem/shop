package com.mycompany.myshop.testkit.dsl.core.scenario.when.steps;

import com.mycompany.myshop.testkit.driver.port.dtos.CheckOutResponse;
import com.mycompany.myshop.testkit.dsl.core.ScenarioDslImpl;
import com.mycompany.myshop.testkit.dsl.core.scenario.ExecutionResult;
import com.mycompany.myshop.testkit.dsl.core.scenario.ExecutionResultBuilder;
import com.mycompany.myshop.testkit.dsl.core.scenario.given.StagedProducts;
import com.mycompany.myshop.testkit.dsl.core.usecase.UseCaseDsl;
import com.mycompany.myshop.testkit.dsl.core.usecase.usecases.CheckOut;
import com.mycompany.myshop.testkit.dsl.core.usecase.usecases.CheckOutVerification;
import com.mycompany.myshop.testkit.dsl.port.ChannelMode;
import com.mycompany.myshop.testkit.dsl.port.when.steps.WhenCheckOut;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

public class WhenCheckOutImpl extends BaseWhenStep<CheckOutResponse, CheckOutVerification> implements WhenCheckOut {
    private final StagedProducts stagedProducts;
    private final List<CartLine> cartLines = new ArrayList<>();
    private final List<UnaryOperator<CheckOut>> steps = new ArrayList<>();
    private String changedPriceSku;
    private String changedPrice;
    private String withdrawnSku;
    private String couponCode;

    public WhenCheckOutImpl(UseCaseDsl app, ScenarioDslImpl scenario, StagedProducts stagedProducts) {
        super(app, scenario);
        this.stagedProducts = stagedProducts;
    }

    @Override
    public WhenCheckOutImpl withCartContaining(String skuAlias, int quantity) {
        cartLines.add(new CartLine(skuAlias, String.valueOf(quantity)));
        return this;
    }

    @Override
    public WhenCheckOutImpl withErpPriceChanged(String skuAlias, String unitPrice) {
        this.changedPriceSku = skuAlias;
        this.changedPrice = unitPrice;
        return this;
    }

    @Override
    public WhenCheckOutImpl withErpNoLongerOffering(String skuAlias) {
        this.withdrawnSku = skuAlias;
        return this;
    }

    @Override
    public WhenCheckOutImpl withProceededFromCart() {
        return step(CheckOut::proceedFromCart);
    }

    @Override
    public WhenCheckOutImpl withProceededFromCartAndSignedIn() {
        return step(checkOut -> checkOut.proceedFromCart().signIn());
    }

    @Override
    public WhenCheckOutImpl withShippingAddress(String firstName, String lastName, String street, String postalCode,
                                                String city, String country) {
        return step(checkOut -> checkOut.shippingAddress(firstName, lastName, street, postalCode, city, country));
    }

    @Override
    public WhenCheckOutImpl withSaveAsDefaultUnticked() {
        return step(CheckOut::doNotSaveAsDefault);
    }

    @Override
    public WhenCheckOutImpl withCouponCode(String couponCode) {
        this.couponCode = couponCode;
        return step(checkOut -> checkOut.couponCode(couponCode));
    }

    @Override
    public WhenCheckOutImpl withOrderPlaced() {
        return step(CheckOut::placeOrder);
    }

    private WhenCheckOutImpl step(UnaryOperator<CheckOut> step) {
        steps.add(step);
        return this;
    }

    /** When the ERP changes after the cart is filled, filling the cart and the checkout are separate calls in one session. */
    @Override
    protected ExecutionResult<CheckOutResponse, CheckOutVerification> execute(UseCaseDsl app) {
        var erpChanges = changedPriceSku != null || withdrawnSku != null;
        var checkOut = app.myShop(ChannelMode.DYNAMIC).checkOut();
        if (erpChanges) {
            var fillCart = app.myShop(ChannelMode.DYNAMIC).manageCart();
            cartLines.forEach(line -> fillCart.cartLine(line.skuAlias(), line.quantity()));
            fillCart.execute().shouldSucceed();
            if (changedPriceSku != null) {
                stagedProducts.changePrice(changedPriceSku, changedPrice);
            }
            if (withdrawnSku != null) {
                stagedProducts.withdraw(withdrawnSku);
            }
            checkOut.continueSession();
        } else {
            cartLines.forEach(line -> checkOut.cartLine(line.skuAlias(), line.quantity()));
        }
        for (var step : steps) {
            step.apply(checkOut);
        }
        return new ExecutionResultBuilder<>(checkOut.execute()).couponCode(couponCode).build();
    }

    private record CartLine(String skuAlias, String quantity) {
    }
}
