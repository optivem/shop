package com.mycompany.myshop.testkit.driver.adapter.shared.client.http;

/**
 * An error body that also records the HTTP response it came from: the status code, the
 * {@code Retry-After} header and the {@code Content-Type}, none of which are in the body itself.
 */
public interface ResponseMetadataAware {
    void captureResponseMetadata(int httpStatus, String retryAfter, String contentType);
}
