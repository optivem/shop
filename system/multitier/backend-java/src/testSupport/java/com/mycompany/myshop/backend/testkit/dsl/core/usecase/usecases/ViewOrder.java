package com.mycompany.myshop.backend.testkit.dsl.core.usecase.usecases;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myshop.backend.core.dtos.ViewOrderDetailsResponse;
import com.mycompany.myshop.backend.testkit.driver.port.MyShopDriver;
import com.mycompany.myshop.backend.testkit.dsl.core.shared.ResponseParser;
import com.mycompany.myshop.backend.testkit.dsl.core.shared.UseCaseResult;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.usecases.base.BaseMyShopUseCase;
import org.springframework.http.HttpStatus;

/**
 * {@code GET /api/orders/{orderNumber}} — {@code 200 OK} with the persisted details, or {@code 404
 * NOT_FOUND} with a {@code ProblemDetail} when no such order exists.
 */
public class ViewOrder extends BaseMyShopUseCase<ViewOrderDetailsResponse, ViewOrderVerification> {

    private final ObjectMapper objectMapper;
    private String orderNumber;

    public ViewOrder(MyShopDriver driver, ObjectMapper objectMapper) {
        super(driver);
        this.objectMapper = objectMapper;
    }

    public ViewOrder orderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
        return this;
    }

    @Override
    public UseCaseResult<ViewOrderDetailsResponse, ViewOrderVerification> execute() {
        var response = driver.viewOrder(orderNumber);

        return new UseCaseResult<>(
            response.getStatusCode(),
            HttpStatus.OK,
            HttpStatus.NOT_FOUND,
            () -> ResponseParser.parseSuccess(response, ViewOrderDetailsResponse.class, objectMapper),
            () -> ResponseParser.parseRejection(response, objectMapper),
            ViewOrderVerification::new);
    }
}
