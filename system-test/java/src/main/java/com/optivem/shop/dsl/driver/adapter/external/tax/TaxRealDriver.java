package com.optivem.shop.dsl.driver.adapter.external.tax;

import com.optivem.shop.dsl.common.Closer;
import com.optivem.shop.dsl.common.Result;
import com.optivem.shop.dsl.driver.adapter.external.tax.client.TaxRealClient;
import com.optivem.shop.dsl.driver.port.external.tax.TaxDriver;
import com.optivem.shop.dsl.driver.port.external.tax.dtos.GetTaxResponse;
import com.optivem.shop.dsl.driver.port.external.tax.dtos.ReturnsTaxRateRequest;
import com.optivem.shop.dsl.driver.port.shared.dtos.ErrorResponse;

public class TaxRealDriver implements TaxDriver {
    private final TaxRealClient client;

    public TaxRealDriver(String baseUrl) {
        this.client = new TaxRealClient(baseUrl);
    }

    @Override
    public void close() {
        Closer.close(client);
    }

    @Override
    public Result<Void, ErrorResponse> goToTax() {
        return client.checkHealth()
                .mapError(ext -> ErrorResponse.builder().message(ext.getMessage()).build());
    }

    @Override
    public Result<GetTaxResponse, ErrorResponse> getTaxRate(String country) {
        return client.getCountry(country)
                .map(ext -> GetTaxResponse.builder()
                        .country(ext.getId())
                        .taxRate(ext.getTaxRate())
                        .build())
                .mapError(ext -> ErrorResponse.builder().message(ext.getMessage()).build());
    }

    @Override
    public Result<Void, ErrorResponse> returnsTaxRate(ReturnsTaxRateRequest request) {
        return Result.success();
    }
}
