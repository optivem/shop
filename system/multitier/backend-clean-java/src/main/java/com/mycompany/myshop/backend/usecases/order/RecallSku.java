package com.mycompany.myshop.backend.usecases.order;

import com.mycompany.myshop.backend.domain.repositories.OrderRepository;
import com.mycompany.myshop.backend.domain.values.Sku;
import com.mycompany.myshop.backend.common.Result;
import com.mycompany.myshop.backend.usecases.UseCase;
import com.mycompany.myshop.backend.usecases.UseCaseError;

public class RecallSku implements UseCase<RecallSkuRequest, RecallSkuResponse> {

    private final OrderRepository orderRepository;

    public RecallSku(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public Result<RecallSkuResponse, UseCaseError> execute(RecallSkuRequest request) {
        var sku = request.sku();
        if (sku == null || sku.trim().isEmpty()) {
            return Result.err(new UseCaseError.Invalid(Sku.FIELD_NAME, "SKU must not be empty"));
        }

        var response = new RecallSkuResponse();
        response.setSku(sku);
        response.setCancelledCount(orderRepository.cancelOutstandingForSku(Sku.of(sku)));
        return Result.ok(response);
    }
}
