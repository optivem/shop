package com.mycompany.myshop.backend.testkit.dsl.core.usecase.usecases;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myshop.backend.usecases.commands.order.PlaceOrderRequest;
import com.mycompany.myshop.backend.usecases.commands.order.PlaceOrderResponse;
import com.mycompany.myshop.backend.testkit.driver.port.MyShopDriver;
import com.mycompany.myshop.backend.testkit.dsl.core.shared.ResponseParser;
import com.mycompany.myshop.backend.testkit.dsl.core.shared.UseCaseContext;
import com.mycompany.myshop.backend.testkit.dsl.core.shared.UseCaseResult;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.usecases.base.BaseMyShopUseCase;
import java.util.Set;
import org.springframework.http.HttpStatus;

public class PlaceOrder extends BaseMyShopUseCase<PlaceOrderResponse, PlaceOrderVerification> {

    private final ObjectMapper objectMapper;
    private String sku;
    private Integer quantity;
    private String country;
    private String couponCode;
    private String rawQuantity;
    private boolean quantityIsRaw;
    private String orderNumberResultAlias;

    public PlaceOrder(MyShopDriver driver, UseCaseContext context, ObjectMapper objectMapper) {
        super(driver, context);
        this.objectMapper = objectMapper;
    }

    public PlaceOrder orderNumber(String orderNumberResultAlias) {
        this.orderNumberResultAlias = orderNumberResultAlias;
        return this;
    }

    public PlaceOrder sku(String sku) {
        this.sku = sku;
        return this;
    }

    public PlaceOrder quantity(Integer quantity) {
        this.quantity = quantity;
        this.quantityIsRaw = false;
        return this;
    }

    public PlaceOrder rawQuantity(String rawQuantity) {
        this.rawQuantity = rawQuantity;
        this.quantityIsRaw = true;
        return this;
    }

    public PlaceOrder country(String country) {
        this.country = country;
        return this;
    }

    public PlaceOrder couponCode(String couponCode) {
        this.couponCode = couponCode;
        return this;
    }

    @Override
    public UseCaseResult<PlaceOrderResponse, PlaceOrderVerification> execute() {
        var response = quantityIsRaw ? driver.placeOrderRaw(rawBody()) : driver.placeOrder(typedBody());

        var result = new UseCaseResult<PlaceOrderResponse, PlaceOrderVerification>(
            response.getStatusCode(),
            HttpStatus.CREATED,
            Set.of(HttpStatus.UNPROCESSABLE_ENTITY),
            () -> ResponseParser.parseSuccess(response, PlaceOrderResponse.class, objectMapper),
            () -> ResponseParser.parseRejection(response, objectMapper),
            PlaceOrderVerification::new);

        registerOrderNumber(result);

        return result;
    }

    // Only an accepted placement has an order number to register. A rejected one leaves the alias
    // unregistered, so a later step resolving it gets the alias back and fails against the SUT rather
    // than here — the failure a test wants to see is "no such order", not a DSL error.
    private void registerOrderNumber(UseCaseResult<PlaceOrderResponse, PlaceOrderVerification> result) {
        if (orderNumberResultAlias == null) {
            return;
        }

        var placed = result.responseOrNull();
        if (placed != null) {
            context.setResultEntry(orderNumberResultAlias, placed.orderNumber());
        }
    }

    private PlaceOrderRequest typedBody() {
        var request = new PlaceOrderRequest();
        request.setSku(sku);
        request.setQuantity(quantity);
        request.setCountry(country);
        request.setCouponCode(couponCode);
        return request;
    }

    // quantity goes in as a JSON *string* (or null) — the shape a form-backed client actually posts,
    // and the one Jackson rejects with the @TypeValidationMessage.
    private String rawBody() {
        var body = objectMapper.createObjectNode();
        body.put("sku", sku);
        if (rawQuantity == null) {
            body.putNull("quantity");
        } else {
            body.put("quantity", rawQuantity);
        }
        body.put("country", country);
        body.put("couponCode", couponCode);
        return body.toString();
    }
}
