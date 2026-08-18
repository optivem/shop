package com.mycompany.myshop.backend.usecases.order;

import com.mycompany.myshop.backend.domain.repositories.OrderRepository;
import com.mycompany.myshop.backend.usecases.Result;
import com.mycompany.myshop.backend.usecases.UseCase;
import com.mycompany.myshop.backend.usecases.UseCaseError;

public class RecallSku implements UseCase<RecallSkuRequest, RecallSkuResponse> {

    private static final String FIELD_SKU = "sku";

    private final OrderRepository orderRepository;

    public RecallSku(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public Result<RecallSkuResponse, UseCaseError> execute(RecallSkuRequest request) {
        var sku = request.sku();
        if (sku == null || sku.trim().isEmpty()) {
            return Result.err(new UseCaseError.Invalid(FIELD_SKU, "SKU must not be empty"));
        }

        var response = new RecallSkuResponse();
        response.setSku(sku);
        response.setCancelledCount(orderRepository.cancelOutstandingForSku(sku));
        return Result.ok(response);
    }
}
