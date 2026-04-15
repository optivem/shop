package com.optivem.shop.testkit.dsl.core.usecase.shop.usecases;

import com.optivem.shop.testkit.driver.port.shop.dtos.PlaceOrderRequest;
import com.optivem.shop.testkit.driver.port.shop.dtos.PlaceOrderResponse;
import com.optivem.shop.testkit.driver.port.shop.ShopDriver;
import com.optivem.shop.testkit.dsl.core.usecase.shop.usecases.base.BaseShopUseCase;
import com.optivem.shop.testkit.dsl.core.shared.UseCaseResult;
import com.optivem.shop.testkit.dsl.core.shared.UseCaseContext;

public class PlaceOrder extends BaseShopUseCase<PlaceOrderResponse, PlaceOrderVerification> {
    private String orderNumberResultAlias;
    private String skuParamAlias;
    private String quantity;
    private String countryAlias;
    private String couponCodeAlias;

    public PlaceOrder(ShopDriver driver, UseCaseContext context) {
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
