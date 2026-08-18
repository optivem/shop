package com.mycompany.myshop.backend.usecases.order;

/**
 * What placing an order returns. No Jackson annotation: serialization is an outer-ring concern, and
 * the field name is already the wire name.
 */
public class PlaceOrderResponse {

    private String orderNumber;

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }
}
