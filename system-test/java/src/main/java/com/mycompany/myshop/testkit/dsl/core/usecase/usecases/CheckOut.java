package com.mycompany.myshop.testkit.dsl.core.usecase.usecases;

import com.mycompany.myshop.testkit.driver.port.MyShopDriver;
import com.mycompany.myshop.testkit.driver.port.dtos.CheckOutRequest;
import com.mycompany.myshop.testkit.driver.port.dtos.CheckOutResponse;
import com.mycompany.myshop.testkit.driver.port.dtos.ShippingAddress;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseContext;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseResult;
import com.mycompany.myshop.testkit.dsl.core.usecase.usecases.base.BaseMyShopUseCase;

import java.util.ArrayList;
import java.util.List;

/**
 * SKUs and coupon codes are aliases, resolved to the values the ERP and the coupon were given. The shipping address
 * is literal: its country is a code the checkout's form offers. Without {@link #placeOrder()}, the checkout is only
 * opened (and the address entered).
 */
public class CheckOut extends BaseMyShopUseCase<CheckOutResponse, CheckOutVerification> {
    private final List<CartLineEntry> cartLines = new ArrayList<>();
    private Boolean continueSession;
    private String entry = CheckOutRequest.OPEN;
    private Boolean signIn;
    private ShippingAddress shippingAddress;
    private String couponCodeAlias;
    private Boolean placeOrder;

    public CheckOut(MyShopDriver driver, UseCaseContext context) {
        super(driver, context);
    }

    /** A line the cart gets first, added from its product page; lines are added in the order given. */
    public CheckOut cartLine(String skuAlias, String quantity) {
        cartLines.add(new CartLineEntry(skuAlias, quantity));
        return this;
    }

    /** Keeps the browser session (and its cart) of the previous call instead of starting a new one. */
    public CheckOut continueSession() {
        this.continueSession = true;
        return this;
    }

    public CheckOut proceedFromCart() {
        this.entry = CheckOutRequest.PROCEED_FROM_CART;
        return this;
    }

    public CheckOut signIn() {
        this.signIn = true;
        return this;
    }

    public CheckOut shippingAddress(String firstName, String lastName, String street, String postalCode, String city,
                                    String country) {
        this.shippingAddress = ShippingAddress.builder()
                .firstName(firstName)
                .lastName(lastName)
                .street(street)
                .postalCode(postalCode)
                .city(city)
                .country(country)
                .build();
        return this;
    }

    public CheckOut couponCode(String couponCodeAlias) {
        this.couponCodeAlias = couponCodeAlias;
        return this;
    }

    public CheckOut placeOrder() {
        this.placeOrder = true;
        return this;
    }

    @Override
    public UseCaseResult<CheckOutResponse, CheckOutVerification> execute() {
        var lines = cartLines.stream()
                .map(line -> CheckOutRequest.CartLineDto.builder()
                        .sku(context.getParamValue(line.skuAlias()))
                        .quantity(line.quantity())
                        .build())
                .toList();
        var request = CheckOutRequest.builder()
                .cartLines(lines)
                .continueSession(continueSession)
                .entry(entry)
                .signIn(signIn)
                .shippingAddress(shippingAddress)
                .couponCode(couponCodeAlias == null ? null : context.getParamValue(couponCodeAlias))
                .placeOrder(placeOrder)
                .build();
        var result = driver.checkOut(request);

        return new UseCaseResult<>(result, context, CheckOutVerification::new);
    }

    private record CartLineEntry(String skuAlias, String quantity) {
    }
}
