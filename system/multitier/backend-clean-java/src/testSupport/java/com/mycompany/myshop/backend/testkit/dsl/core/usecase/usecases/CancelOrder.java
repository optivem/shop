package com.mycompany.myshop.backend.testkit.dsl.core.usecase.usecases;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myshop.backend.testkit.driver.port.MyShopDriver;
import com.mycompany.myshop.backend.testkit.dsl.core.shared.ResponseParser;
import com.mycompany.myshop.backend.testkit.dsl.core.shared.UseCaseContext;
import com.mycompany.myshop.backend.testkit.dsl.core.shared.UseCaseResult;
import com.mycompany.myshop.backend.testkit.dsl.core.shared.VoidVerification;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.usecases.base.BaseMyShopUseCase;
import java.util.Set;
import org.springframework.http.HttpStatus;

/**
 * {@code POST /api/orders/{orderNumber}/cancel} — {@code 204 NO_CONTENT} on acceptance, no body.
 *
 * <p>Two rejection statuses, not one. {@code 422 UNPROCESSABLE_ENTITY} covers both business rules —
 * the December 31st 22:00–22:30 blackout and an order that is already cancelled — while an unknown
 * order number is {@code 404 NOT_FOUND}. Note the service checks the blackout <em>before</em> it
 * looks the order up, so inside the window even a non-existent order is rejected with {@code 422}.
 */
public class CancelOrder extends BaseMyShopUseCase<Void, VoidVerification> {

    private final ObjectMapper objectMapper;
    private String orderNumberResultAlias;

    public CancelOrder(MyShopDriver driver, UseCaseContext context, ObjectMapper objectMapper) {
        super(driver, context);
        this.objectMapper = objectMapper;
    }

    /**
     * An alias registered by an earlier step — {@code given().order()} most of all — or a literal
     * order number. Unregistered strings pass through unchanged, which is what lets a test name an
     * order that deliberately does not exist.
     */
    public CancelOrder orderNumber(String orderNumberResultAlias) {
        this.orderNumberResultAlias = orderNumberResultAlias;
        return this;
    }

    @Override
    public UseCaseResult<Void, VoidVerification> execute() {
        var response = driver.cancelOrder(context.getResultValue(orderNumberResultAlias));

        return new UseCaseResult<>(
            response.getStatusCode(),
            HttpStatus.NO_CONTENT,
            Set.of(HttpStatus.UNPROCESSABLE_ENTITY, HttpStatus.NOT_FOUND),
            () -> null,
            () -> ResponseParser.parseRejection(response, objectMapper),
            VoidVerification::new);
    }
}
