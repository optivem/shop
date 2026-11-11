using Dsl.Core.Shared;
using Driver.Port.Dtos;
using Shouldly;

namespace Dsl.Core.UseCase.UseCases;

/// <summary>SKUs are aliases, resolved to the values the ERP was given; everything else is a literal as shown.</summary>
public class ViewProductVerification : ResponseVerification<ViewProductResponse>
{
    public ViewProductVerification(ViewProductResponse response, UseCaseContext context)
        : base(response, context)
    {
    }

    public ViewProductVerification Title(string expectedTitle)
    {
        Response.Title.ShouldBe(expectedTitle, "Product title");
        return this;
    }

    public ViewProductVerification Brand(string expectedBrand)
    {
        Response.Brand.ShouldBe(expectedBrand, "Product brand");
        return this;
    }

    public ViewProductVerification Category(string expectedCategory)
    {
        Response.Category.ShouldBe(expectedCategory, "Product category");
        return this;
    }

    public ViewProductVerification Sku(string expectedSkuAlias)
    {
        Response.Sku.ShouldBe(Context.GetParamValue(expectedSkuAlias), "Product SKU");
        return this;
    }

    public ViewProductVerification Price(string expectedPrice)
    {
        Response.Price.ShouldBe(expectedPrice, "Product price");
        return this;
    }

    public ViewProductVerification Description(string expectedDescription)
    {
        Response.Description.ShouldBe(expectedDescription, "Product description");
        return this;
    }

    public ViewProductVerification Breadcrumbs(params string[] expectedBreadcrumbs)
    {
        Response.Breadcrumbs.ShouldNotBeNull("Breadcrumb trail");
        Response.Breadcrumbs.ShouldBe(expectedBreadcrumbs.ToList(), "Breadcrumb trail");
        return this;
    }

    public ViewProductVerification Quantity(int expectedQuantity)
    {
        Response.Quantity.ShouldBe(expectedQuantity.ToString(), "Quantity");
        return this;
    }

    public ViewProductVerification Message(string expectedMessage)
    {
        Response.Message.ShouldBe(expectedMessage, "Product page message");
        return this;
    }

    public ViewProductVerification BackToCatalogOffered()
    {
        Response.BackToCatalogOffered.ShouldBe(true, "Product page should offer a link back to the catalog");
        return this;
    }

    public ViewProductVerification AddToCartNotOffered()
    {
        Response.AddToCartOffered.ShouldBe(false, "Product page should not offer to add to the cart");
        return this;
    }

    public ViewProductVerification CartItemCount(int expectedCount)
    {
        Response.CartItemCount.ShouldBe(expectedCount, "Cart badge");
        return this;
    }

    public ViewProductVerification OnPageOfProduct(string skuAlias)
    {
        Response.PageSku.ShouldBe(Context.GetParamValue(skuAlias), "Product page the visitor is on");
        return this;
    }

    /// <summary>Exactly this line.</summary>
    public ViewProductVerification CartContains(string skuAlias, int expectedQuantity)
    {
        var expected = new ViewProductResponse.CartLineDto(Context.GetParamValue(skuAlias), expectedQuantity);
        CartLines().ShouldBe(new List<ViewProductResponse.CartLineDto> { expected }, "Cart lines");
        return this;
    }

    public ViewProductVerification CartIsEmpty()
    {
        CartLines().ShouldBeEmpty("Cart lines");
        return this;
    }

    public ViewProductVerification CatalogShowsProducts(params string[] skuAliases)
    {
        var expectedSkus = skuAliases.Select(alias => Context.GetParamValue(alias)).ToList();
        Response.CatalogSkus.ShouldNotBeNull("Catalog products should have been read");
        Response.CatalogSkus.ShouldBe(expectedSkus, "Catalog products, in order");
        return this;
    }

    private List<ViewProductResponse.CartLineDto> CartLines()
    {
        Response.CartLines.ShouldNotBeNull("Cart lines should have been read");
        return Response.CartLines;
    }
}
