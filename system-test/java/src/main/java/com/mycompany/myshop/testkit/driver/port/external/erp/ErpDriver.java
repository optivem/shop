package com.mycompany.myshop.testkit.driver.port.external.erp;

import com.mycompany.myshop.testkit.driver.port.external.erp.dtos.GetProductRequest;
import com.mycompany.myshop.testkit.driver.port.external.erp.dtos.GetProductResponse;
import com.mycompany.myshop.testkit.driver.port.external.erp.dtos.GetProductsResponse;
import com.mycompany.myshop.testkit.driver.port.external.erp.dtos.ReturnsProductRequest;
import com.mycompany.myshop.testkit.driver.port.external.erp.dtos.ReturnsProductsRequest;
import com.mycompany.myshop.testkit.driver.port.external.erp.dtos.ReturnsPromotionRequest;
import com.mycompany.myshop.testkit.driver.port.external.erp.dtos.error.ErpErrorResponse;
import com.mycompany.myshop.testkit.common.Result;

public interface ErpDriver extends AutoCloseable {
    Result<Void, ErpErrorResponse> goToErp();

    Result<GetProductResponse, ErpErrorResponse> getProduct(GetProductRequest request);

    Result<GetProductsResponse, ErpErrorResponse> getProducts();

    Result<Void, ErpErrorResponse> returnsProduct(ReturnsProductRequest request);

    Result<Void, ErpErrorResponse> returnsProducts(ReturnsProductsRequest request);

    Result<Void, ErpErrorResponse> returnsPromotion(ReturnsPromotionRequest request);
}
