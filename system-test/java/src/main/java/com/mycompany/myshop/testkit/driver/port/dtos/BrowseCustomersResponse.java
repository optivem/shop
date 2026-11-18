package com.mycompany.myshop.testkit.driver.port.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.List;

/** The usernames that have placed an order, A to Z. On the wire it is a bare JSON array of strings, not an object. */
public class BrowseCustomersResponse {
    private final List<String> customers;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public BrowseCustomersResponse(List<String> customers) {
        this.customers = customers;
    }

    @JsonValue
    public List<String> getCustomers() {
        return customers;
    }

    @Override
    public String toString() {
        return String.valueOf(customers);
    }
}
