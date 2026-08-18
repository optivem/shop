package com.mycompany.myshop.backend.usecases.dtos;

/**
 * What browsing coupons needs: nothing. It exists so that {@code BrowseCoupons} has the same shape
 * as every other use case — a no-argument {@code execute()} would be a fourth input shape, which is
 * exactly what the uniform signature is there to prevent.
 */
public record BrowseCouponsRequest() { }
