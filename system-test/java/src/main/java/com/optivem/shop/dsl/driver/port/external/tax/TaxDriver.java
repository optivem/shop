package com.optivem.shop.dsl.driver.port.external.tax;

import com.optivem.shop.dsl.driver.port.external.tax.dtos.GetCountryRequest;
import com.optivem.shop.dsl.driver.port.external.tax.dtos.GetCountryResponse;
import com.optivem.shop.dsl.driver.port.external.tax.dtos.ReturnsCountryRequest;
import com.optivem.shop.dsl.driver.port.shared.dtos.ErrorResponse;
import com.optivem.shop.dsl.common.Result;

public interface TaxDriver extends AutoCloseable {
    Result<Void, ErrorResponse> goToTax();

    Result<GetCountryResponse, ErrorResponse> getCountry(GetCountryRequest request);

    Result<Void, ErrorResponse> returnsCountry(ReturnsCountryRequest request);
}
