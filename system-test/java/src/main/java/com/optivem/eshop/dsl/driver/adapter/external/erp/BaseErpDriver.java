package com.optivem.eshop.dsl.driver.adapter.external.erp;

import com.optivem.eshop.dsl.driver.port.external.erp.ErpDriver;

import com.optivem.eshop.dsl.driver.adapter.external.erp.client.BaseErpClient;
import com.optivem.eshop.dsl.driver.port.external.erp.dtos.GetProductRequest;
import com.optivem.eshop.dsl.driver.port.external.erp.dtos.GetProductResponse;
import com.optivem.eshop.dsl.driver.port.shared.dtos.ErrorResponse;
import com.optivem.eshop.dsl.common.Closer;
import com.optivem.eshop.dsl.common.Result;

public abstract class BaseErpDriver<TClient extends BaseErpClient> implements ErpDriver {
    protected final TClient client;

    protected BaseErpDriver(TClient client) {
        this.client = client;
    }

    @Override
    public void close() throws Exception {
        Closer.close(client);
    }

    @Override
    public Result<Void, ErrorResponse> goToErp() {
        return client.checkHealth()
                .mapError(ext -> ErrorResponse.builder().message(ext.getMessage()).build());
    }

    @Override
    public Result<GetProductResponse, ErrorResponse> getProduct(GetProductRequest request) {
        return client.getProduct(request.getSku())
                .map(productDetails -> GetProductResponse.builder()
                        .sku(productDetails.getId())
                        .price(productDetails.getPrice())
                        .build())
                .mapError(ext -> ErrorResponse.builder().message(ext.getMessage()).build());
    }
}
