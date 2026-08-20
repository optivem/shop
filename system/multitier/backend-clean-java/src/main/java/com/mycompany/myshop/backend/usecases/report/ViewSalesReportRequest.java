package com.mycompany.myshop.backend.usecases.report;

// A null topSkuLimit means "use the default"; anything else must be 1..100.
public record ViewSalesReportRequest(Integer topSkuLimit) { }
