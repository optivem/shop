package com.mycompany.myshop.backend.usecases.order;

import com.mycompany.myshop.backend.common.Result;
import com.mycompany.myshop.backend.usecases.UseCase;
import com.mycompany.myshop.backend.usecases.UseCaseError;
import com.mycompany.myshop.backend.usecases.queries.OrderDetail;
import com.mycompany.myshop.backend.usecases.queries.OrderQuery;

// A pure query: every field of the response is a column in orders, so the domain model is
// not built at all. The error contract does not move -- an absent row is still
// UseCaseError.NotFound.
public class ViewOrderDetails implements UseCase<ViewOrderDetailsRequest, ViewOrderDetailsResponse> {

    private static final String ORDER_ENTITY = "Order";

    private final OrderQuery orderQuery;

    public ViewOrderDetails(OrderQuery orderQuery) {
        this.orderQuery = orderQuery;
    }

    @Override
    public Result<ViewOrderDetailsResponse, UseCaseError> execute(ViewOrderDetailsRequest request) {
        var orderNumber = request.orderNumber();
        var found = orderQuery.findOrderDetail(orderNumber);
        if (found.isEmpty()) {
            return Result.err(new UseCaseError.NotFound(ORDER_ENTITY, orderNumber));
        }

        return Result.ok(toResponse(found.get()));
    }

    private static ViewOrderDetailsResponse toResponse(OrderDetail detail) {
        var response = new ViewOrderDetailsResponse();
        response.setOrderNumber(detail.orderNumber());
        response.setOrderTimestamp(detail.orderTimestamp());
        response.setSku(detail.sku());
        response.setQuantity(detail.quantity());
        response.setUnitPrice(detail.unitPrice());
        response.setBasePrice(detail.basePrice());
        response.setDiscountRate(detail.discountRate());
        response.setDiscountAmount(detail.discountAmount());
        response.setSubtotalPrice(detail.subtotalPrice());
        response.setTaxRate(detail.taxRate());
        response.setTaxAmount(detail.taxAmount());
        response.setTotalPrice(detail.totalPrice());
        response.setStatus(detail.status());
        response.setCountry(detail.country());
        response.setAppliedCouponCode(detail.appliedCouponCode());
        return response;
    }
}
