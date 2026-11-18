package com.mycompany.myshop.testkit.dsl.core.usecase.usecases;

import com.mycompany.myshop.testkit.driver.port.MyShopDriver;
import com.mycompany.myshop.testkit.driver.port.dtos.BrowseCustomersRequest;
import com.mycompany.myshop.testkit.driver.port.dtos.BrowseCustomersResponse;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseContext;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseResult;
import com.mycompany.myshop.testkit.dsl.core.usecase.usecases.base.BaseMyShopUseCase;

public class BrowseCustomers extends BaseMyShopUseCase<BrowseCustomersResponse, BrowseCustomersVerification> {
    public BrowseCustomers(MyShopDriver driver, UseCaseContext context) {
        super(driver, context);
    }

    @Override
    public UseCaseResult<BrowseCustomersResponse, BrowseCustomersVerification> execute() {
        var result = driver.browseCustomers(BrowseCustomersRequest.builder().build());
        return new UseCaseResult<>(result, context, BrowseCustomersVerification::new);
    }
}
