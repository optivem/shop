package com.mycompany.myshop.testkit.dsl.core.scenario.when.steps;

import static com.mycompany.myshop.testkit.dsl.core.scenario.ScenarioDefaults.*;

import com.mycompany.myshop.testkit.common.Converter;
import com.mycompany.myshop.testkit.dsl.core.ScenarioDslImpl;
import com.mycompany.myshop.testkit.dsl.core.usecase.UseCaseDsl;
import com.mycompany.myshop.testkit.dsl.core.scenario.ExecutionResult;
import com.mycompany.myshop.testkit.dsl.core.scenario.ExecutionResultBuilder;
import com.mycompany.myshop.testkit.driver.port.dtos.PlaceOrderResponse;
import com.mycompany.myshop.testkit.dsl.port.ChannelMode;
import com.mycompany.myshop.testkit.dsl.port.when.steps.WhenPlaceOrder;
import com.mycompany.myshop.testkit.dsl.core.usecase.usecases.PlaceOrderVerification;

public class WhenPlaceOrderImpl extends BaseWhenStep<PlaceOrderResponse, PlaceOrderVerification> implements WhenPlaceOrder {
    private String orderNumber;
    private String sku;
    private String quantity;
    private String country;
    private String couponCode;
    private String idempotencyKey;
    private boolean shippingAddressMode;
    private String shippingFirstName = DEFAULT_FIRST_NAME;
    private String shippingLastName = DEFAULT_LAST_NAME;
    private String shippingStreet = DEFAULT_STREET;
    private String shippingPostalCode = DEFAULT_POSTAL_CODE;
    private String shippingCity = DEFAULT_CITY;
    private String shippingCountry = DEFAULT_COUNTRY;

    public WhenPlaceOrderImpl(UseCaseDsl app, ScenarioDslImpl scenario) {
        super(app, scenario);
        withOrderNumber(DEFAULT_ORDER_NUMBER);
        withSku(DEFAULT_SKU);
        withQuantity(DEFAULT_QUANTITY);
        withCountry(DEFAULT_COUNTRY);
        withCouponCode(EMPTY);
    }

    public WhenPlaceOrderImpl withOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
        return this;
    }

    public WhenPlaceOrderImpl withSku(String sku) {
        this.sku = sku;
        return this;
    }

    public WhenPlaceOrderImpl withQuantity(String quantity) {
        this.quantity = quantity;
        return this;
    }

    public WhenPlaceOrderImpl withQuantity(int quantity) {
        return withQuantity(Converter.fromInteger(quantity));
    }

    public WhenPlaceOrderImpl withCountry(String country) {
        this.country = country;
        return this;
    }

    @Override
    public WhenPlaceOrderImpl withPrefilledCountry() {
        return withCountry(null);
    }

    public WhenPlaceOrderImpl withCouponCode(String couponCode) {
        this.couponCode = couponCode;
        return this;
    }

    public WhenPlaceOrderImpl withCouponCode() {
        return withCouponCode(DEFAULT_COUPON_CODE);
    }

    public WhenPlaceOrderImpl withIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
        return this;
    }

    public WhenPlaceOrderImpl withShippingAddress() {
        this.shippingAddressMode = true;
        return this;
    }

    public WhenPlaceOrderImpl withShippingAddress(String firstName, String lastName, String street, String postalCode,
                                                  String city, String country) {
        return withShippingFirstName(firstName)
                .withShippingLastName(lastName)
                .withShippingStreet(street)
                .withShippingPostalCode(postalCode)
                .withShippingCity(city)
                .withShippingCountry(country);
    }

    public WhenPlaceOrderImpl withShippingFirstName(String firstName) {
        this.shippingFirstName = firstName;
        return withShippingAddress();
    }

    public WhenPlaceOrderImpl withShippingLastName(String lastName) {
        this.shippingLastName = lastName;
        return withShippingAddress();
    }

    public WhenPlaceOrderImpl withShippingStreet(String street) {
        this.shippingStreet = street;
        return withShippingAddress();
    }

    public WhenPlaceOrderImpl withShippingPostalCode(String postalCode) {
        this.shippingPostalCode = postalCode;
        return withShippingAddress();
    }

    public WhenPlaceOrderImpl withShippingCity(String city) {
        this.shippingCity = city;
        return withShippingAddress();
    }

    public WhenPlaceOrderImpl withShippingCountry(String country) {
        this.shippingCountry = country;
        return withShippingAddress();
    }

    @Override
    protected ExecutionResult<PlaceOrderResponse, PlaceOrderVerification> execute(UseCaseDsl app) {
        var placeOrder = app.myShop(ChannelMode.DYNAMIC).placeOrder()
                .orderNumber(orderNumber)
                .sku(sku)
                .quantity(quantity)
                .couponCode(couponCode)
                .idempotencyKey(idempotencyKey);
        if (shippingAddressMode) {
            // In address mode the tax country is the address's; no top-level country is sent.
            placeOrder.shippingAddress(shippingFirstName, shippingLastName, shippingStreet, shippingPostalCode,
                    shippingCity, shippingCountry);
        } else {
            placeOrder.country(country);
        }
        var result = placeOrder.execute();

        return new ExecutionResultBuilder<>(result)
                .orderNumber(orderNumber)
                .couponCode(couponCode)
                .build();
    }
}
