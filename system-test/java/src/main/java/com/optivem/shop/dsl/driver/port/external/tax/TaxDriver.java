package com.optivem.shop.dsl.driver.port.external.tax;

import com.optivem.shop.dsl.driver.port.external.tax.dtos.GetTaxResponse;
import com.optivem.shop.dsl.driver.port.external.tax.dtos.ReturnsTaxRateRequest;
import com.optivem.shop.dsl.driver.port.shared.dtos.ErrorResponse;
import com.optivem.shop.dsl.common.Result;

public interface TaxDriver extends AutoCloseable {
    Result<Void, ErrorResponse> goToTax();

    Result<GetTaxResponse, ErrorResponse> getTaxRate(String country);

    Result<Void, ErrorResponse> returnsTaxRate(ReturnsTaxRateRequest request);
}
