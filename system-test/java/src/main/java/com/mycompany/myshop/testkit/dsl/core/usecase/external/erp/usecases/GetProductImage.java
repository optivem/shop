package com.mycompany.myshop.testkit.dsl.core.usecase.external.erp.usecases;

import com.mycompany.myshop.testkit.driver.port.external.erp.ErpDriver;
import com.mycompany.myshop.testkit.driver.port.external.erp.dtos.GetProductImageRequest;
import com.mycompany.myshop.testkit.driver.port.external.erp.dtos.GetProductImageResponse;
import com.mycompany.myshop.testkit.dsl.core.usecase.external.erp.usecases.base.BaseErpUseCase;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseResult;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseContext;
import com.mycompany.myshop.testkit.driver.port.dtos.error.SystemError;

public class GetProductImage extends BaseErpUseCase<GetProductImageResponse, GetProductImageVerification> {
    private String imageUrl;

    public GetProductImage(ErpDriver driver, UseCaseContext context) {
        super(driver, context);
    }

    public GetProductImage imageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
        return this;
    }

    @Override
    public UseCaseResult<GetProductImageResponse, GetProductImageVerification> execute() {
        var request = GetProductImageRequest.builder()
                .imageUrl(imageUrl)
                .build();

        var result = driver.getProductImage(request);

        return new UseCaseResult<>(result.mapError(e -> SystemError.of(e.getMessage())), context, GetProductImageVerification::new);
    }
}
