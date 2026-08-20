package com.mycompany.myshop.backend.usecases.report;

/** A {@code null} {@code topSkuLimit} means "use the default"; anything else must be 1..100. */
public record ViewSalesReportRequest(Integer topSkuLimit) { }
