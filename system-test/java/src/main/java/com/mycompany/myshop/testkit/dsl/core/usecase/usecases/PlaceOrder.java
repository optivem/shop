package com.mycompany.myshop.testkit.dsl.core.usecase.usecases;

import com.mycompany.myshop.testkit.driver.port.dtos.PlaceOrderRequest;
import com.mycompany.myshop.testkit.driver.port.dtos.PlaceOrderResponse;
import com.mycompany.myshop.testkit.driver.port.MyShopDriver;
import com.mycompany.myshop.testkit.dsl.core.usecase.usecases.base.BaseMyShopUseCase;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseResult;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseContext;

public class PlaceOrder extends BaseMyShopUseCase<PlaceOrderResponse, PlaceOrderVerification> {
    private String orderNumberResultAlias;
    private String skuParamAlias;
    private String quantity;
    private String countryAlias;
    private String couponCodeAlias;
    private boolean giftWrap;

    public PlaceOrder(MyShopDriver driver, UseCaseContext context) {
        super(driver, context);
    }

    public PlaceOrder orderNumber(String orderNumberResultAlias) {
        this.orderNumberResultAlias = orderNumberResultAlias;
        return this;
    }

    public PlaceOrder sku(String skuParamAlias) {
        this.skuParamAlias = skuParamAlias;
        return this;
    }

    public PlaceOrder quantity(String quantity) {
        this.quantity = quantity;
        return this;
    }

    public PlaceOrder quantity(int quantity) {
        return quantity(String.valueOf(quantity));
    }

    public PlaceOrder country(String countryAlias) {
        this.countryAlias = countryAlias;
        return this;
    }

    public PlaceOrder couponCode(String couponCodeAlias) {
        this.couponCodeAlias = couponCodeAlias;
        return this;
    }

    public PlaceOrder giftWrap(boolean giftWrap) {
        this.giftWrap = giftWrap;
        return this;
    }

    @Override
    public UseCaseResult<PlaceOrderResponse, PlaceOrderVerification> execute() {
        var sku = context.getParamValue(skuParamAlias);
        var country = context.getParamValueOrLiteral(countryAlias);
        var couponCode = context.getParamValue(couponCodeAlias);

        var request = PlaceOrderRequest.builder()
                .sku(sku)
                .quantity(quantity)
                .country(country)
                .couponCode(couponCode)
                .giftWrap(giftWrap)
                .build();

        var result = driver.placeOrder(request);

        if(orderNumberResultAlias != null) {
            if(result.isSuccess()) {
                var orderNumber = result.getValue().getOrderNumber();
                context.setResultEntry(orderNumberResultAlias, orderNumber);
            } else {
                context.setResultEntryFailed(orderNumberResultAlias, result.getError().toString());
            }
        }

        return new UseCaseResult<>(result, context, PlaceOrderVerification::new);
    }
}
