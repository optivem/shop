package com.mycompany.myshop.testkit.dsl.core.usecase.usecases;

import com.mycompany.myshop.testkit.driver.port.dtos.PlaceOrderRequest;
import com.mycompany.myshop.testkit.driver.port.dtos.PlaceOrderResponse;
import com.mycompany.myshop.testkit.driver.port.dtos.ShippingAddress;
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
    private String idempotencyKeyAlias;
    private ShippingAddress shippingAddress;

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

    public PlaceOrder idempotencyKey(String idempotencyKeyAlias) {
        this.idempotencyKeyAlias = idempotencyKeyAlias;
        return this;
    }

    /**
     * Places the order with a shipping address (the tax country is then the address's country). The
     * country is an alias, resolved like {@link #country(String)}; the other fields are literals.
     */
    public PlaceOrder shippingAddress(String firstName, String lastName, String street, String postalCode,
                                      String city, String countryAlias) {
        this.shippingAddress = ShippingAddress.builder()
                .firstName(firstName)
                .lastName(lastName)
                .street(street)
                .postalCode(postalCode)
                .city(city)
                .country(countryAlias)
                .build();
        return this;
    }

    @Override
    public UseCaseResult<PlaceOrderResponse, PlaceOrderVerification> execute() {
        var sku = context.getParamValue(skuParamAlias);
        var country = context.getParamValueOrLiteral(countryAlias);
        var couponCode = context.getParamValue(couponCodeAlias);
        // Keys are the shop's own data, so the alias always maps to a scenario-unique value: tests share a
        // database, and two scenarios reusing a literal key would replay each other's orders.
        var idempotencyKey = context.getParamValue(idempotencyKeyAlias);

        var request = PlaceOrderRequest.builder()
                .sku(sku)
                .quantity(quantity)
                .country(country)
                .couponCode(couponCode)
                .idempotencyKey(idempotencyKey)
                .shippingAddress(resolveShippingAddress())
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

    private ShippingAddress resolveShippingAddress() {
        if (shippingAddress == null) {
            return null;
        }
        return shippingAddress.toBuilder()
                .country(context.getParamValueOrLiteral(shippingAddress.getCountry()))
                .build();
    }
}
