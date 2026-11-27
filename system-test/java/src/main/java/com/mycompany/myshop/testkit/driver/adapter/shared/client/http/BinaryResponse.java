package com.mycompany.myshop.testkit.driver.adapter.shared.client.http;

/** A successful non-JSON response: its Content-Type header and raw body. */
public record BinaryResponse(String contentType, byte[] body) {
}
