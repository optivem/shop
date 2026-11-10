using Dsl.Core.Shared;
using Driver.Port.Dtos;
using Shouldly;

namespace Dsl.Core.UseCase.UseCases;

/// <summary>SKUs are aliases, resolved to the values the ERP was given; everything else is a literal as shown.</summary>
public class BrowseCatalogVerification : ResponseVerification<BrowseCatalogResponse>
{
    public BrowseCatalogVerification(BrowseCatalogResponse response, UseCaseContext context)
        : base(response, context)
    {
    }

    public BrowseCatalogVerification ShowsProducts(params string[] skuAliases)
    {
        var expectedSkus = skuAliases.Select(alias => Context.GetParamValue(alias)).ToList();
        ListedSkus().ShouldBe(expectedSkus, "Catalog products, in order");
        return this;
    }

    public BrowseCatalogVerification ShowsNoProducts()
    {
        ListedSkus().ShouldBeEmpty("Catalog products");
        return this;
    }

    public BrowseCatalogVerification ProductCount(int expectedCount)
    {
        Response.ProductCount.ShouldBe(expectedCount, "Catalog product count");
        return this;
    }

    public BrowseCatalogVerification Product(string skuAlias, string expectedTitle, string expectedBrand, string expectedPrice)
    {
        var sku = Context.GetParamValue(skuAlias);
        var product = Products().FirstOrDefault(listed => listed.Sku == sku);
        product.ShouldNotBeNull($"Catalog should show product '{sku}'");
        product.Title.ShouldBe(expectedTitle, $"Title of product '{sku}'");
        product.Brand.ShouldBe(expectedBrand, $"Brand of product '{sku}'");
        product.Price.ShouldBe(expectedPrice, $"Price of product '{sku}'");
        return this;
    }

    public BrowseCatalogVerification CategoryOptions(params string[] expectedCategories)
    {
        Response.Categories.ShouldBe(expectedCategories, ignoreOrder: false, customMessage: "Category filter options");
        return this;
    }

    public BrowseCatalogVerification SelectedCategory(string expectedCategory)
    {
        Response.SelectedCategory.ShouldBe(expectedCategory, "Category filter selection");
        return this;
    }

    public BrowseCatalogVerification BrandOptions(params string[] expectedBrands)
    {
        Response.Brands.ShouldBe(expectedBrands, ignoreOrder: false, customMessage: "Brand filter options");
        return this;
    }

    public BrowseCatalogVerification NoBrandTicked()
    {
        Response.TickedBrands.ShouldNotBeNull("Ticked brands");
        Response.TickedBrands.ShouldBeEmpty("Ticked brands");
        return this;
    }

    public BrowseCatalogVerification Sort(string expectedSort)
    {
        Response.Sort.ShouldBe(expectedSort, "Catalog sort");
        return this;
    }

    public BrowseCatalogVerification Message(string expectedMessage)
    {
        Response.Message.ShouldBe(expectedMessage, "Catalog message");
        return this;
    }

    public BrowseCatalogVerification ClearFiltersOffered()
    {
        Response.ClearFiltersOffered.ShouldBe(true, "Catalog should offer to clear the filters");
        return this;
    }

    private List<string?> ListedSkus() => Products().Select(product => product.Sku).ToList();

    private List<BrowseCatalogResponse.ProductDto> Products()
    {
        Response.ShouldNotBeNull("Response should not be null");
        Response.Products.ShouldNotBeNull("Products list should not be null");
        return Response.Products;
    }
}
