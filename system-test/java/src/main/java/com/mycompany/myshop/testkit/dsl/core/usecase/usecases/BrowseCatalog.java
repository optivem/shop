package com.mycompany.myshop.testkit.dsl.core.usecase.usecases;

import com.mycompany.myshop.testkit.driver.port.MyShopDriver;
import com.mycompany.myshop.testkit.driver.port.dtos.BrowseCatalogRequest;
import com.mycompany.myshop.testkit.driver.port.dtos.BrowseCatalogResponse;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseContext;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseResult;
import com.mycompany.myshop.testkit.dsl.core.usecase.usecases.base.BaseMyShopUseCase;

import java.util.List;

public class BrowseCatalog extends BaseMyShopUseCase<BrowseCatalogResponse, BrowseCatalogVerification> {
    private String headerCategory;
    private String search;
    private String category;
    private List<String> brands;
    private String sort;
    private String followUp;

    public BrowseCatalog(MyShopDriver driver, UseCaseContext context) {
        super(driver, context);
    }

    public BrowseCatalog headerCategory(String headerCategory) {
        this.headerCategory = headerCategory;
        return this;
    }

    public BrowseCatalog search(String search) {
        this.search = search;
        return this;
    }

    public BrowseCatalog category(String category) {
        this.category = category;
        return this;
    }

    public BrowseCatalog brands(List<String> brands) {
        this.brands = brands;
        return this;
    }

    public BrowseCatalog sort(String sort) {
        this.sort = sort;
        return this;
    }

    public BrowseCatalog reopenPageAddress() {
        this.followUp = BrowseCatalogRequest.REOPEN_PAGE_ADDRESS;
        return this;
    }

    public BrowseCatalog clearFilters() {
        this.followUp = BrowseCatalogRequest.CLEAR_FILTERS;
        return this;
    }

    @Override
    public UseCaseResult<BrowseCatalogResponse, BrowseCatalogVerification> execute() {
        var request = BrowseCatalogRequest.builder()
                .headerCategory(headerCategory)
                .search(search)
                .category(category)
                .brands(brands == null ? null : String.join(BrowseCatalogRequest.BRAND_SEPARATOR, brands))
                .sort(sort)
                .followUp(followUp)
                .build();
        var result = driver.browseCatalog(request);

        return new UseCaseResult<>(result, context, BrowseCatalogVerification::new);
    }
}
