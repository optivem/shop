package com.mycompany.myshop.backend.integration.latest.base;

import com.mycompany.myshop.backend.backendtest.configuration.ExternalSystemMode;
import com.mycompany.myshop.backend.domain.gateways.ClockGateway;
import com.mycompany.myshop.backend.domain.gateways.ErpGateway;
import com.mycompany.myshop.backend.domain.gateways.TaxGateway;
import com.mycompany.myshop.backend.infrastructure.external.clock.HttpClockGateway;
import com.mycompany.myshop.backend.infrastructure.external.erp.HttpErpGateway;
import com.mycompany.myshop.backend.infrastructure.external.tax.HttpTaxGateway;

final class Gateways {

    private Gateways() {
    }

    static ErpGateway erp(String baseUrl) {
        return new HttpErpGateway(baseUrl);
    }

    static TaxGateway tax(String baseUrl) {
        return new HttpTaxGateway(baseUrl);
    }

    static ClockGateway clock(String baseUrl, ExternalSystemMode mode) {
        return clockWithRawMode(baseUrl, mode.propertyValue());
    }

    static ClockGateway clockWithRawMode(String baseUrl, String rawMode) {
        return new HttpClockGateway(rawMode, baseUrl);
    }
}
