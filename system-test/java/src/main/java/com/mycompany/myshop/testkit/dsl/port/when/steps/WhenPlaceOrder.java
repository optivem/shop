package com.mycompany.myshop.testkit.dsl.port.when.steps;

import com.mycompany.myshop.testkit.dsl.port.when.steps.base.WhenStep;

public interface WhenPlaceOrder extends WhenStep {
    WhenPlaceOrder withOrderNumber(String orderNumber);

    WhenPlaceOrder withSku(String sku);

    WhenPlaceOrder withQuantity(String quantity);

    WhenPlaceOrder withQuantity(int quantity);

    WhenPlaceOrder withCountry(String country);

    /**
     * Leaves the country as Quick order prefilled it from the saved profile (UC-010 BR-5): the UI keeps the field as
     * shown; the API gets no country.
     */
    WhenPlaceOrder withPrefilledCountry();

    WhenPlaceOrder withCouponCode(String couponCode);

    WhenPlaceOrder withCouponCode();

    WhenPlaceOrder withIdempotencyKey(String idempotencyKey);

    /**
     * Places the order with the default shipping address. Any shipping-address setter switches the request to
     * address mode: the address starts from the default, and no top-level country is sent.
     */
    WhenPlaceOrder withShippingAddress();

    WhenPlaceOrder withShippingAddress(String firstName, String lastName, String street, String postalCode,
                                       String city, String country);

    WhenPlaceOrder withShippingFirstName(String firstName);

    WhenPlaceOrder withShippingLastName(String lastName);

    WhenPlaceOrder withShippingStreet(String street);

    WhenPlaceOrder withShippingPostalCode(String postalCode);

    WhenPlaceOrder withShippingCity(String city);

    WhenPlaceOrder withShippingCountry(String country);
}
