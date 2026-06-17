package com.mycompany.myshop.testkit.driver.adapter.external.erp;

import com.mycompany.myshop.testkit.driver.port.external.erp.ErpDriver;

import com.mycompany.myshop.testkit.driver.adapter.external.erp.client.BaseErpClient;
import com.mycompany.myshop.testkit.driver.port.external.erp.dtos.GetProductRequest;
import com.mycompany.myshop.testkit.driver.port.external.erp.dtos.GetProductResponse;
import com.mycompany.myshop.testkit.driver.port.external.erp.dtos.error.ErpErrorResponse;
import com.mycompany.myshop.testkit.common.Closer;
import com.mycompany.myshop.testkit.common.Result;

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
    public Result<Void, ErpErrorResponse> goToErp() {
        return client.checkHealth()
                .mapError(ext -> new ErpErrorResponse(ext.getMessage()));
    }

    @Override
    public Result<GetProductResponse, ErpErrorResponse> getProduct(GetProductRequest request) {
        return client.getProduct(request.getSku())
                .map(productDetails -> GetProductResponse.builder()
                        .sku(productDetails.getId())
                        .price(productDetails.getPrice())
                        .weight(productDetails.getWeight())
                        .build())
                .mapError(ext -> new ErpErrorResponse(ext.getMessage()));
    }
}
