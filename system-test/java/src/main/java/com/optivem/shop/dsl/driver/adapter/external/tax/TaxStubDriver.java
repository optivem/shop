package com.optivem.shop.dsl.driver.adapter.external.tax;

import com.optivem.shop.dsl.common.Closer;
import com.optivem.shop.dsl.common.Converter;
import com.optivem.shop.dsl.common.Result;
import com.optivem.shop.dsl.driver.adapter.external.tax.client.TaxStubClient;
import com.optivem.shop.dsl.driver.adapter.external.tax.client.dtos.ExtGetCountryResponse;
import com.optivem.shop.dsl.driver.port.external.tax.TaxDriver;
import com.optivem.shop.dsl.driver.port.external.tax.dtos.GetCountryRequest;
import com.optivem.shop.dsl.driver.port.external.tax.dtos.GetCountryResponse;
import com.optivem.shop.dsl.driver.port.external.tax.dtos.ReturnsCountryRequest;
import com.optivem.shop.dsl.driver.port.shared.dtos.ErrorResponse;

public class TaxStubDriver implements TaxDriver {
    private final TaxStubClient client;

    public TaxStubDriver(String baseUrl) {
        this.client = new TaxStubClient(baseUrl);
    }

    @Override
    public void close() throws Exception {
        client.removeStubs();
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
        var taxRate = Converter.toBigDecimal(request.getTaxRate());
        var extResponse = ExtGetCountryResponse.builder()
                .id(request.getCountry())
                .countryName(request.getCountry())
                .taxRate(taxRate)
                .build();

        return client.configureGetCountry(request.getCountry(), extResponse)
                .mapError(ext -> ErrorResponse.builder().message(ext.getMessage()).build());
    }
}
