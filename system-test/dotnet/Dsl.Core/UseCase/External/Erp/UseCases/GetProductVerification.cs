using Driver.Port.External.Erp.Dtos;
using Dsl.Core.Shared;
using Shouldly;

namespace Dsl.Core.External.Erp.UseCases;

public class GetProductVerification : ResponseVerification<GetProductResponse>
{
    public GetProductVerification(GetProductResponse response, UseCaseContext context)
        : base(response, context)
    {
    }

    public GetProductVerification Sku(string skuParamAlias)
    {
        var expectedSku = Context.GetParamValue(skuParamAlias);
        var actualSku = Response.Sku;
        actualSku.ShouldBe(expectedSku, $"Expected SKU to be '{expectedSku}', but was '{actualSku}'");
        return this;
    }

    public GetProductVerification Price(decimal expectedPrice)
    {
        var actualPrice = Response.Price;
        actualPrice.ShouldBe(expectedPrice, $"Expected price to be {expectedPrice}, but was {actualPrice}");
        return this;
    }

    public GetProductVerification Price(string expectedPrice)
    {
        return Price(decimal.Parse(expectedPrice));
    }

    public GetProductVerification Title(string expectedTitle)
    {
        var actualTitle = Response.Title;
        actualTitle.ShouldBe(expectedTitle, $"Expected title to be '{expectedTitle}', but was '{actualTitle}'");
        return this;
    }

    public GetProductVerification Description(string expectedDescription)
    {
        var actualDescription = Response.Description;
        actualDescription.ShouldBe(expectedDescription, $"Expected description to be '{expectedDescription}', but was '{actualDescription}'");
        return this;
    }

    public GetProductVerification Category(string expectedCategory)
    {
        var actualCategory = Response.Category;
        actualCategory.ShouldBe(expectedCategory, $"Expected category to be '{expectedCategory}', but was '{actualCategory}'");
        return this;
    }

    public GetProductVerification Brand(string expectedBrand)
    {
        var actualBrand = Response.Brand;
        actualBrand.ShouldBe(expectedBrand, $"Expected brand to be '{expectedBrand}', but was '{actualBrand}'");
        return this;
    }
}
