package com.mycompany.myshop.backend.testkit.dsl.core.usecase.usecases;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myshop.backend.usecases.order.ViewOrderDetailsResponse;
import com.mycompany.myshop.backend.testkit.driver.port.MyShopDriver;
import com.mycompany.myshop.backend.testkit.dsl.core.shared.ResponseParser;
import com.mycompany.myshop.backend.testkit.dsl.core.shared.UseCaseContext;
import com.mycompany.myshop.backend.testkit.dsl.core.shared.UseCaseResult;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.usecases.base.BaseMyShopUseCase;
import java.util.Set;
import org.springframework.http.HttpStatus;

public class ViewOrder extends BaseMyShopUseCase<ViewOrderDetailsResponse, ViewOrderVerification> {

    private final ObjectMapper objectMapper;
    private String orderNumberResultAlias;

    public ViewOrder(MyShopDriver driver, UseCaseContext context, ObjectMapper objectMapper) {
        super(driver, context);
        this.objectMapper = objectMapper;
    }

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
