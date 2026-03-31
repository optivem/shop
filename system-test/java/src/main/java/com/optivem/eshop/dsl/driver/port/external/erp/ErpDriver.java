package com.optivem.eshop.dsl.driver.port.external.erp;

import com.optivem.eshop.dsl.driver.port.external.erp.dtos.GetProductRequest;
import com.optivem.eshop.dsl.driver.port.external.erp.dtos.GetProductResponse;
import com.optivem.eshop.dsl.driver.port.external.erp.dtos.ReturnsProductRequest;
import com.optivem.eshop.dsl.driver.port.shared.dtos.ErrorResponse;
import com.optivem.eshop.dsl.common.Result;

public interface ErpDriver extends AutoCloseable {
    Result<Void, ErrorResponse> goToErp();

    Result<GetProductResponse, ErrorResponse> getProduct(GetProductRequest request);

    Result<Void, ErrorResponse> returnsProduct(ReturnsProductRequest request);
}
