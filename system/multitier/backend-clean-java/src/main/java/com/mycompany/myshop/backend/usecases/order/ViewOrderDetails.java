package com.mycompany.myshop.backend.usecases.order;

import com.mycompany.myshop.backend.domain.repositories.OrderRepository;
import com.mycompany.myshop.backend.domain.values.CouponCode;
import com.mycompany.myshop.backend.usecases.Result;
import com.mycompany.myshop.backend.usecases.UseCase;
import com.mycompany.myshop.backend.usecases.UseCaseError;

public class ViewOrderDetails implements UseCase<ViewOrderDetailsRequest, ViewOrderDetailsResponse> {

    private static final String ORDER_ENTITY = "Order";

    private final OrderRepository orderRepository;

    public ViewOrderDetails(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public Result<ViewOrderDetailsResponse, UseCaseError> execute(ViewOrderDetailsRequest request) {
        var orderNumber = request.orderNumber();
        var found = orderRepository.findByOrderNumber(orderNumber);
        if (found.isEmpty()) {
            return Result.err(new UseCaseError.NotFound(ORDER_ENTITY, orderNumber));
        }

        var order = found.get();

        var response = new ViewOrderDetailsResponse();
        response.setOrderNumber(orderNumber);
        response.setOrderTimestamp(order.getOrderTimestamp());
        response.setSku(order.getSku());
        response.setQuantity(order.getQuantity());
        response.setUnitPrice(order.getUnitPrice().amount());
        response.setBasePrice(order.getBasePrice().amount());
        response.setDiscountRate(order.getDiscountRate().value());
        response.setDiscountAmount(order.getDiscountAmount().amount());
        response.setSubtotalPrice(order.getSubtotalPrice().amount());
        response.setTaxRate(order.getTaxRate().value());
        response.setTaxAmount(order.getTaxAmount().amount());
        response.setTotalPrice(order.getTotalPrice().amount());
        response.setStatus(order.getStatus());
        response.setCountry(order.getCountry().value());
        response.setAppliedCouponCode(CouponCode.valueOrNull(order.getAppliedCouponCode()));

        return Result.ok(response);
    }
}
