package com.mycompany.myshop.testkit.dsl.core.usecase.external.erp.usecases;

import com.mycompany.myshop.testkit.driver.port.external.erp.ErpDriver;
import com.mycompany.myshop.testkit.driver.port.external.erp.dtos.ReturnsProductRequest;
import com.mycompany.myshop.testkit.driver.port.external.erp.dtos.ReturnsProductsRequest;
import com.mycompany.myshop.testkit.dsl.core.usecase.external.erp.usecases.base.BaseErpUseCase;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseResult;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseContext;
import com.mycompany.myshop.testkit.dsl.core.shared.VoidVerification;
import com.mycompany.myshop.testkit.driver.port.dtos.error.SystemError;

import java.util.ArrayList;
import java.util.List;

/** Stages the ERP's enumerable product list (GET /api/products); an empty list stages no products. */
public class ReturnsProducts extends BaseErpUseCase<Void, VoidVerification> {
    private final List<ProductEntry> products = new ArrayList<>();

    public ReturnsProducts(ErpDriver driver, UseCaseContext context) {
        super(driver, context);
    }

    public ReturnsProducts product(String skuParamAlias, String unitPrice) {
        products.add(new ProductEntry(skuParamAlias, unitPrice));
        return this;
    }

    @Override
    public UseCaseResult<Void, VoidVerification> execute() {
        var productRequests = products.stream()
                .map(product -> ReturnsProductRequest.builder()
                        .sku(context.getParamValue(product.skuParamAlias()))
                        .price(product.unitPrice())
                        .build())
                .toList();

        var request = ReturnsProductsRequest.builder()
                .products(productRequests)
                .build();

        var result = driver.returnsProducts(request);
        return new UseCaseResult<>(result.mapError(e -> SystemError.of(e.getMessage())), context, VoidVerification::new);
    }

    private record ProductEntry(String skuParamAlias, String unitPrice) {
    }
}
