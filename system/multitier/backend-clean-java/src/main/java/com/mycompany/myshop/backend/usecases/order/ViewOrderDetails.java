package com.mycompany.myshop.backend.usecases.order;

import com.mycompany.myshop.backend.domain.values.CouponCode;
import com.mycompany.myshop.backend.domain.exceptions.NotExistValidationException;
import com.mycompany.myshop.backend.domain.repositories.OrderRepository;
import com.mycompany.myshop.backend.usecases.dtos.ViewOrderDetailsResponse;

/**
 * Returns the full detail of one order. The domain → response mapping lives here rather than in the
 * controller: the response DTO belongs to this use case, so this is the layer that knows how to
 * fill it in — including unwrapping the domain's {@code Money} and {@code Rate} back to the plain
 * numbers the wire contract asks for.
 */
public class ViewOrderDetails {

    private final OrderRepository orderRepository;

    public ViewOrderDetails(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public ViewOrderDetailsResponse execute(String orderNumber) {
        var order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new NotExistValidationException("Order " + orderNumber + " does not exist."));

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

        return response;
    }
}
