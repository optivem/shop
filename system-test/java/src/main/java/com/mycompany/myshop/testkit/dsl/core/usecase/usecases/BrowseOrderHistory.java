package com.mycompany.myshop.testkit.dsl.core.usecase.usecases;

import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseContext;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseResult;
import com.mycompany.myshop.testkit.dsl.core.usecase.usecases.base.BaseMyShopUseCase;
import com.mycompany.myshop.testkit.driver.port.MyShopDriver;
import com.mycompany.myshop.testkit.driver.port.dtos.BrowseOrderHistoryRequest;
import com.mycompany.myshop.testkit.driver.port.dtos.BrowseOrderHistoryResponse;

public class BrowseOrderHistory extends BaseMyShopUseCase<BrowseOrderHistoryResponse, BrowseOrderHistoryVerification> {
    private String orderNumberResultAlias;

    public BrowseOrderHistory(MyShopDriver driver, UseCaseContext context) {
        super(driver, context);
    }

    public BrowseOrderHistory orderNumber(String orderNumberResultAlias) {
        this.orderNumberResultAlias = orderNumberResultAlias;
        return this;
    }

    @Override
    public UseCaseResult<BrowseOrderHistoryResponse, BrowseOrderHistoryVerification> execute() {
        var orderNumber = context.getResultValue(orderNumberResultAlias);

        var request = BrowseOrderHistoryRequest.builder().orderNumber(orderNumber).build();
        var result = driver.browseOrderHistory(request);

        return new UseCaseResult<>(result, context, BrowseOrderHistoryVerification::new);
    }
}
