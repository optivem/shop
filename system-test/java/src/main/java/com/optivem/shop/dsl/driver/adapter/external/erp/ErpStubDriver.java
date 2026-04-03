package com.optivem.shop.dsl.driver.adapter.external.erp;

import com.optivem.shop.dsl.driver.adapter.external.erp.client.ErpStubClient;
import com.optivem.shop.dsl.driver.adapter.external.erp.client.dtos.ExtProductDetailsResponse;
import com.optivem.shop.dsl.driver.port.external.erp.dtos.ReturnsProductRequest;
import com.optivem.shop.dsl.driver.port.shared.dtos.ErrorResponse;
import com.optivem.shop.dsl.common.Converter;
import com.optivem.shop.dsl.common.Result;

/**
 * ErpStubDriver uses WireMock to stub ERP API responses.
 * This allows tests to run without a real ERP system.
 */
public class ErpStubDriver extends BaseErpDriver<ErpStubClient> {
    public ErpStubDriver(String baseUrl) {
        super(new ErpStubClient(baseUrl));
    }

    @Override
    public void close() throws Exception {
        client.removeStubs();
        super.close();
    }

    @Override
    public Result<Void, ErrorResponse> returnsProduct(ReturnsProductRequest request) {
        var extProductDetailsResponse = ExtProductDetailsResponse.builder()
                .id(request.getSku())
                .title("")
                .description("")
                .price(Converter.toBigDecimal(request.getPrice()))
                .category("")
                .brand("")
                .build();

        return client.configureGetProduct(extProductDetailsResponse)
                .mapError(ext -> ErrorResponse.builder().message(ext.getMessage()).build());
    }
}
