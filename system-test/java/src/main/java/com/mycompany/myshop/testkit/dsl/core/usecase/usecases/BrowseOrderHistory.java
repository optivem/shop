package com.mycompany.myshop.testkit.dsl.core.usecase.usecases;

import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseContext;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseResult;
import com.mycompany.myshop.testkit.dsl.core.usecase.usecases.base.BaseMyShopUseCase;
import com.mycompany.myshop.testkit.driver.port.MyShopDriver;
import com.mycompany.myshop.testkit.driver.port.dtos.BrowseOrderHistoryRequest;
import com.mycompany.myshop.testkit.driver.port.dtos.BrowseOrderHistoryResponse;

public class BrowseOrderHistory extends BaseMyShopUseCase<BrowseOrderHistoryResponse, BrowseOrderHistoryVerification> {
    private String orderNumberResultAlias;
    private String page;
    private String size;
    private String customer;
    private String followUp;

    public BrowseOrderHistory(MyShopDriver driver, UseCaseContext context) {
        super(driver, context);
    }

    public BrowseOrderHistory orderNumber(String orderNumberResultAlias) {
        this.orderNumberResultAlias = orderNumberResultAlias;
        return this;
    }

    public BrowseOrderHistory page(String page) {
        this.page = page;
        return this;
    }

    public BrowseOrderHistory size(String size) {
        this.size = size;
        return this;
    }

    /** The customer filter: a customer's username, sent as is. */
    public BrowseOrderHistory customer(String customer) {
        this.customer = customer;
        return this;
    }

    /** After browsing, the customer filter is set to All customers and the order number cleared (UI only). */
    public BrowseOrderHistory resetFiltersAfterwards() {
        this.followUp = BrowseOrderHistoryRequest.RESET_FILTERS;
        return this;
    }

    @Override
    public UseCaseResult<BrowseOrderHistoryResponse, BrowseOrderHistoryVerification> execute() {
        var orderNumber = context.getResultValue(orderNumberResultAlias);

        var request = BrowseOrderHistoryRequest.builder()
                .orderNumber(orderNumber)
                .page(page)
                .size(size)
                .customer(customer)
                .followUp(followUp)
                .build();
        var result = driver.browseOrderHistory(request);

        return new UseCaseResult<>(result, context, BrowseOrderHistoryVerification::new);
    }
}
