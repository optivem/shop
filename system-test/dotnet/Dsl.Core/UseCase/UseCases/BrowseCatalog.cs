using Driver.Port;
using Dsl.Core.UseCase.UseCases.Base;
using Dsl.Core.Shared;
using Driver.Port.Dtos;

namespace Dsl.Core.UseCase.UseCases;

public class BrowseCatalog : BaseMyShopUseCase<BrowseCatalogResponse, BrowseCatalogVerification>
{
    private string? _headerCategory;
    private string? _search;
    private string? _category;
    private IReadOnlyList<string>? _brands;
    private string? _sort;
    private string? _followUp;

    public BrowseCatalog(IMyShopDriver driver, UseCaseContext context)
        : base(driver, context)
    {
    }

    public BrowseCatalog HeaderCategory(string? headerCategory)
    {
        _headerCategory = headerCategory;
        return this;
    }

    public BrowseCatalog Search(string? search)
    {
        _search = search;
        return this;
    }

    public BrowseCatalog Category(string? category)
    {
        _category = category;
        return this;
    }

    public BrowseCatalog Brands(IReadOnlyList<string>? brands)
    {
        _brands = brands;
        return this;
    }

    public BrowseCatalog Sort(string? sort)
    {
        _sort = sort;
        return this;
    }

    public BrowseCatalog ReopenPageAddress()
    {
        _followUp = BrowseCatalogRequest.ReopenPageAddress;
        return this;
    }

    public BrowseCatalog ClearFilters()
    {
        _followUp = BrowseCatalogRequest.ClearFilters;
        return this;
    }

    public override async Task<MyShopUseCaseResult<BrowseCatalogResponse, BrowseCatalogVerification>> Execute()
    {
        var request = new BrowseCatalogRequest
        {
            HeaderCategory = _headerCategory,
            Search = _search,
            Category = _category,
            Brands = _brands == null ? null : string.Join(BrowseCatalogRequest.BrandSeparator, _brands),
            Sort = _sort,
            FollowUp = _followUp,
        };

        var result = await _driver.BrowseCatalogAsync(request);

        return new MyShopUseCaseResult<BrowseCatalogResponse, BrowseCatalogVerification>(
            result,
            _context,
            (response, ctx) => new BrowseCatalogVerification(response, ctx));
    }
}
