package com.mycompany.myshop.backend.testkit.dsl.core.usecase.usecases;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myshop.backend.usecases.dtos.ViewOrderDetailsResponse;
import com.mycompany.myshop.backend.testkit.driver.port.MyShopDriver;
import com.mycompany.myshop.backend.testkit.dsl.core.shared.ResponseParser;
import com.mycompany.myshop.backend.testkit.dsl.core.shared.UseCaseContext;
import com.mycompany.myshop.backend.testkit.dsl.core.shared.UseCaseResult;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.usecases.base.BaseMyShopUseCase;
import java.util.Set;
import org.springframework.http.HttpStatus;

/**
 * {@code GET /api/orders/{orderNumber}} — {@code 200 OK} with the persisted details, or {@code 404
 * NOT_FOUND} with a {@code ProblemDetail} when no such order exists.
 */
public class ViewOrder extends BaseMyShopUseCase<ViewOrderDetailsResponse, ViewOrderVerification> {

    private final ObjectMapper objectMapper;
    private String orderNumberResultAlias;

    public ViewOrder(MyShopDriver driver, UseCaseContext context, ObjectMapper objectMapper) {
        super(driver, context);
        this.objectMapper = objectMapper;
    }

    /**
     * An alias registered by an earlier step, or a literal order number. Unregistered strings pass
     * through unchanged, which is what makes {@code withOrderNumber("UNKNOWN")} still name an order
     * that does not exist.
     */
    public ViewOrder orderNumber(String orderNumberResultAlias) {
        this.orderNumberResultAlias = orderNumberResultAlias;
        return this;
    }

    @Override
    public UseCaseResult<ViewOrderDetailsResponse, ViewOrderVerification> execute() {
        var response = driver.viewOrder(context.getResultValue(orderNumberResultAlias));

        return new UseCaseResult<>(
            response.getStatusCode(),
            HttpStatus.OK,
            Set.of(HttpStatus.NOT_FOUND),
            () -> ResponseParser.parseSuccess(response, ViewOrderDetailsResponse.class, objectMapper),
            () -> ResponseParser.parseRejection(response, objectMapper),
            ViewOrderVerification::new);
    }
}
