package com.mycompany.myshop.testkit.dsl.core.usecase.usecases;

import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseContext;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseResult;
import com.mycompany.myshop.testkit.dsl.core.usecase.usecases.base.BaseMyShopUseCase;
import com.mycompany.myshop.testkit.driver.port.MyShopDriver;
import com.mycompany.myshop.testkit.driver.port.dtos.BrowseOrderHistoryRequest;
import com.mycompany.myshop.testkit.driver.port.dtos.BrowseOrderHistoryResponse;

import java.util.Locale;

public class BrowseOrderHistory extends BaseMyShopUseCase<BrowseOrderHistoryResponse, BrowseOrderHistoryVerification> {
    private static final int ORDER_NUMBER_PART_START = 4;
    private static final int ORDER_NUMBER_PART_LENGTH = 8;

    private String orderNumberResultAlias;
    private boolean orderNumberPartOnly;
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

    /** Filters by part of this order's number, in lower case: characters from its middle, unique to it. */
    public BrowseOrderHistory orderNumberPartOf(String orderNumberResultAlias) {
        this.orderNumberResultAlias = orderNumberResultAlias;
        this.orderNumberPartOnly = true;
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
        if (orderNumberPartOnly) {
            orderNumber = partOf(orderNumber);
        }

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

    /** Eight characters after the "ORD-" prefix (the start of the generated part), in lower case. */
    private static String partOf(String orderNumber) {
        var start = Math.min(ORDER_NUMBER_PART_START, orderNumber.length());
        var end = Math.min(start + ORDER_NUMBER_PART_LENGTH, orderNumber.length());
        return orderNumber.substring(start, end).toLowerCase(Locale.ROOT);
    }
}
