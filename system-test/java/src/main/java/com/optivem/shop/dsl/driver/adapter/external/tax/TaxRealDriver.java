package com.optivem.shop.dsl.driver.adapter.external.tax;

import com.optivem.shop.dsl.common.Closer;
import com.optivem.shop.dsl.common.Result;
import com.optivem.shop.dsl.driver.adapter.external.tax.client.TaxRealClient;
import com.optivem.shop.dsl.driver.port.external.tax.TaxDriver;
import com.optivem.shop.dsl.driver.port.external.tax.dtos.GetCountryRequest;
import com.optivem.shop.dsl.driver.port.external.tax.dtos.GetCountryResponse;
import com.optivem.shop.dsl.driver.port.external.tax.dtos.ReturnsCountryRequest;
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
    public Result<GetCountryResponse, ErrorResponse> getCountry(GetCountryRequest request) {
        return client.getCountry(request.getCountry())
                .map(ext -> GetCountryResponse.builder()
                        .id(ext.getId())
                        .countryName(ext.getCountryName())
                        .taxRate(ext.getTaxRate())
                        .build())
                .mapError(ext -> ErrorResponse.builder().message(ext.getMessage()).build());
    }

    @Override
    public Result<Void, ErrorResponse> returnsCountry(ReturnsCountryRequest request) {
        return Result.success();
    }
}
