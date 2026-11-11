using Dsl.Core.Shared;
using Driver.Port.Dtos;
using Shouldly;

namespace Dsl.Core.UseCase.UseCases;

/// <summary>SKUs are aliases, resolved to the values the ERP was given; everything else is a literal as shown.</summary>
public class ManageCartVerification : ResponseVerification<ManageCartResponse>
{
    public ManageCartVerification(ManageCartResponse response, UseCaseContext context)
        : base(response, context)
    {
    }

    public ManageCartVerification Message(string expectedMessage)
    {
        Response.Message.ShouldBe(expectedMessage, "Message shown");
        return this;
    }

    public ManageCartVerification CartItemCount(int expectedCount)
    {
        Response.CartItemCount.ShouldBe(expectedCount, "Cart badge");
        return this;
    }

    public ManageCartVerification NoCartBadge()
    {
        Response.CartItemCount.ShouldBe(0, "Cart badge should be hidden");
        return this;
    }

    /// <summary>Exactly these lines, in this order.</summary>
    public ManageCartVerification Lines(params string[] expectedSkuAliases)
    {
        var expectedSkus = expectedSkuAliases.Select(alias => Context.GetParamValue(alias)).ToList();
        CartLines().Select(line => line.Sku).ToList().ShouldBe(expectedSkus, "Cart lines, in order");
        return this;
    }

    public ManageCartVerification Line(string skuAlias, string expectedTitle, string expectedUnitPrice,
        int expectedQuantity, string expectedLineTotal)
    {
        FindLine(skuAlias).Title.ShouldBe(expectedTitle, $"Title of cart line {skuAlias}");
        return LinePrices(skuAlias, expectedUnitPrice, expectedLineTotal)
            .LineQuantity(skuAlias, expectedQuantity);
    }

    public ManageCartVerification LineQuantity(string skuAlias, int expectedQuantity)
    {
        FindLine(skuAlias).Quantity.ShouldBe(expectedQuantity, $"Quantity of cart line {skuAlias}");
        return this;
    }

    public ManageCartVerification LinePrices(string skuAlias, string expectedUnitPrice, string expectedLineTotal)
    {
        FindLine(skuAlias).UnitPrice.ShouldBe(expectedUnitPrice, $"Unit price of cart line {skuAlias}");
        return LineTotal(skuAlias, expectedLineTotal);
    }

    public ManageCartVerification LineTotal(string skuAlias, string expectedLineTotal)
    {
        FindLine(skuAlias).LineTotal.ShouldBe(expectedLineTotal, $"Line total of cart line {skuAlias}");
        return this;
    }

    public ManageCartVerification ItemCount(int expectedCount)
    {
        Response.ItemCount.ShouldBe(expectedCount, "Number of items the cart shows");
        return this;
    }

    public ManageCartVerification Subtotal(string expectedSubtotal)
    {
        Response.Subtotal.ShouldBe(expectedSubtotal, "Cart subtotal");
        return this;
    }

    public ManageCartVerification CheckoutNote(string expectedNote)
    {
        Response.CheckoutNote.ShouldBe(expectedNote, "Cart note about checkout");
        return this;
    }

    public ManageCartVerification StartShoppingOffered()
    {
        Response.StartShoppingOffered.ShouldBe(true, "Cart should offer a link to start shopping");
        return this;
    }

    public ManageCartVerification NotAskedToSignIn()
    {
        Response.AskedToSignIn.ShouldBe(false, "Visitor should not be asked to sign in");
        return this;
    }

    private ManageCartResponse.CartLineDto FindLine(string skuAlias)
    {
        var sku = Context.GetParamValue(skuAlias);
        var lines = CartLines();
        return lines.FirstOrDefault(line => line.Sku == sku)
            ?? throw new ShouldAssertException($"The cart has no line {skuAlias}; it has {string.Join(", ", lines)}");
    }

    private List<ManageCartResponse.CartLineDto> CartLines()
    {
        Response.ShouldNotBeNull("Response should not be null");
        Response.Lines.ShouldNotBeNull("Cart lines should have been read");
        return Response.Lines;
    }
}
