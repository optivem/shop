package com.mycompany.myshop.testkit.dsl.core.scenario.when.steps;

import com.mycompany.myshop.testkit.driver.port.dtos.BrowseCatalogResponse;
import com.mycompany.myshop.testkit.dsl.core.ScenarioDslImpl;
import com.mycompany.myshop.testkit.dsl.core.scenario.ExecutionResult;
import com.mycompany.myshop.testkit.dsl.core.scenario.ExecutionResultBuilder;
import com.mycompany.myshop.testkit.dsl.core.usecase.UseCaseDsl;
import com.mycompany.myshop.testkit.dsl.core.usecase.usecases.BrowseCatalogVerification;
import com.mycompany.myshop.testkit.dsl.port.ChannelMode;
import com.mycompany.myshop.testkit.dsl.port.when.steps.WhenBrowseCatalog;

import java.util.List;

public class WhenBrowseCatalogImpl extends BaseWhenStep<BrowseCatalogResponse, BrowseCatalogVerification> implements WhenBrowseCatalog {
    private String headerCategory;
    private String search;
    private String category;
    private List<String> brands;
    private String sort;
    private boolean reopenPageAddress;
    private boolean clearFilters;

    public WhenBrowseCatalogImpl(UseCaseDsl app, ScenarioDslImpl scenario) {
        super(app, scenario);
    }

    @Override
    public WhenBrowseCatalogImpl withHeaderCategoryLink(String category) {
        this.headerCategory = category;
        return this;
    }

    @Override
    public WhenBrowseCatalogImpl withSearch(String term) {
        this.search = term;
        return this;
    }

    @Override
    public WhenBrowseCatalogImpl withCategory(String category) {
        this.category = category;
        return this;
    }

    @Override
    public WhenBrowseCatalogImpl withBrands(String... brands) {
        this.brands = List.of(brands);
        return this;
    }

    @Override
    public WhenBrowseCatalogImpl withSort(String sort) {
        this.sort = sort;
        return this;
    }

    @Override
    public WhenBrowseCatalogImpl withPageAddressReopened() {
        this.reopenPageAddress = true;
        return this;
    }

    @Override
    public WhenBrowseCatalogImpl withFiltersCleared() {
        this.clearFilters = true;
        return this;
    }

    @Override
    protected ExecutionResult<BrowseCatalogResponse, BrowseCatalogVerification> execute(UseCaseDsl app) {
        var browseCatalog = app.myShop(ChannelMode.DYNAMIC).browseCatalog()
                .headerCategory(headerCategory)
                .search(search)
                .category(category)
                .brands(brands)
                .sort(sort);
        if (reopenPageAddress) {
            browseCatalog.reopenPageAddress();
        }
        if (clearFilters) {
            browseCatalog.clearFilters();
        }
        return new ExecutionResultBuilder<>(browseCatalog.execute()).build();
    }
}
