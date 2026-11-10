using Dsl.Core.Scenario.When.Steps.Base;
using Dsl.Port;
using Dsl.Port.When.Steps;
using Driver.Port.Dtos;
using Dsl.Core.UseCase.UseCases;

namespace Dsl.Core.Scenario.When.Steps;

public class WhenBrowseCatalog : BaseWhen<BrowseCatalogResponse, BrowseCatalogVerification>, IWhenBrowseCatalog
{
    private string? _headerCategory;
    private string? _search;
    private string? _category;
    private IReadOnlyList<string>? _brands;
    private string? _sort;
    private bool _reopenPageAddress;
    private bool _clearFilters;

    public WhenBrowseCatalog(UseCaseDsl app, ScenarioDsl scenario, Func<Task> ensureGiven)
        : base(app, scenario, ensureGiven)
    {
    }

    public WhenBrowseCatalog WithHeaderCategoryLink(string category)
    {
        _headerCategory = category;
        return this;
    }

    IWhenBrowseCatalog IWhenBrowseCatalog.WithHeaderCategoryLink(string category) => WithHeaderCategoryLink(category);

    public WhenBrowseCatalog WithSearch(string term)
    {
        _search = term;
        return this;
    }

    IWhenBrowseCatalog IWhenBrowseCatalog.WithSearch(string term) => WithSearch(term);

    public WhenBrowseCatalog WithCategory(string category)
    {
        _category = category;
        return this;
    }

    IWhenBrowseCatalog IWhenBrowseCatalog.WithCategory(string category) => WithCategory(category);

    public WhenBrowseCatalog WithBrands(params string[] brands)
    {
        _brands = brands;
        return this;
    }

    IWhenBrowseCatalog IWhenBrowseCatalog.WithBrands(params string[] brands) => WithBrands(brands);

    public WhenBrowseCatalog WithSort(string sort)
    {
        _sort = sort;
        return this;
    }

    IWhenBrowseCatalog IWhenBrowseCatalog.WithSort(string sort) => WithSort(sort);

    public WhenBrowseCatalog WithPageAddressReopened()
    {
        _reopenPageAddress = true;
        return this;
    }

    IWhenBrowseCatalog IWhenBrowseCatalog.WithPageAddressReopened() => WithPageAddressReopened();

    public WhenBrowseCatalog WithFiltersCleared()
    {
        _clearFilters = true;
        return this;
    }

    IWhenBrowseCatalog IWhenBrowseCatalog.WithFiltersCleared() => WithFiltersCleared();

    protected override async Task<ExecutionResult<BrowseCatalogResponse, BrowseCatalogVerification>> Execute(UseCaseDsl app)
    {
        var shop = await app.MyShop(ChannelMode.Dynamic, Channel);
        var browseCatalog = shop.BrowseCatalog()
            .HeaderCategory(_headerCategory)
            .Search(_search)
            .Category(_category)
            .Brands(_brands)
            .Sort(_sort);
        if (_reopenPageAddress)
        {
            browseCatalog.ReopenPageAddress();
        }
        if (_clearFilters)
        {
            browseCatalog.ClearFilters();
        }

        var result = await browseCatalog.Execute();
        return new ExecutionResultBuilder<BrowseCatalogResponse, BrowseCatalogVerification>(result).Build();
    }
}
