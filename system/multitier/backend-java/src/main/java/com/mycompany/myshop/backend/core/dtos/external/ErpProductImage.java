package com.mycompany.myshop.backend.core.dtos.external;

/** A product photo as the ERP serves it: the raw bytes and their content type (e.g. image/jpeg). */
public record ErpProductImage(byte[] content, String contentType) {
}
