using Dsl.Core.Shared;
using Driver.Port.Dtos;
using Shouldly;

namespace Dsl.Core.UseCase.UseCases;

/// <summary>
/// SKUs are aliases, resolved to the values the ERP was given; messages may name SKU and coupon aliases, which are
/// expanded; everything else is a literal as shown.
/// </summary>
public class CheckOutVerification : ResponseVerification<CheckOutResponse>
{
    public CheckOutVerification(CheckOutResponse response, UseCaseContext context)
        : base(response, context)
    {
    }

    /// <summary>Passes each confirmed order number, in the order shown, to <paramref name="action"/>; the numbers must each be different.</summary>
    public void ForEachConfirmedOrder(Action<string> action)
    {
        OrderNumbers().ForEach(action);
    }

    /// <summary>The confirmed orders' products, in the order shown, are exactly these.</summary>
    public CheckOutVerification PlacedFor(List<KeyValuePair<string, string?>> placedSkus, params string[] expectedSkuAliases)
    {
        placedSkus.Select(entry => entry.Value).ToList().ShouldBe(Skus(expectedSkuAliases), "Products of the confirmed orders, in order");
        return this;
    }

    /// <summary>
    /// Passes the number of the order placed for this product to <paramref name="action"/>, given each confirmed order's
    /// SKU by order number; fails if the confirmation shows no order for it.
    /// </summary>
    public void ForOrderPlacedFor(List<KeyValuePair<string, string?>> placedSkus, string skuAlias, Action<string> action)
    {
        var sku = Context.GetParamValue(skuAlias);
        foreach (var entry in placedSkus)
        {
            if (entry.Value == sku)
            {
                action(entry.Key);
                return;
            }
        }
        throw new ShouldAssertException($"The confirmation shows no order for {skuAlias}; it shows orders for "
            + string.Join(", ", placedSkus.Select(entry => entry.Value)));
    }

    public CheckOutVerification ShownPage(string expectedPage)
    {
        Resp().ShownPage.ShouldBe(expectedPage, "Page the customer ended up on");
        return this;
    }

    public CheckOutVerification Message(string expectedMessage)
    {
        Resp().Message.ShouldBe(Context.ExpandAliases(expectedMessage), "Message shown");
        return this;
    }

    /// <summary>One of the lines under the error notification's message.</summary>
    public CheckOutVerification MessageDetail(string expectedDetail)
    {
        var details = Resp().MessageDetails;
        details.ShouldNotBeNull("Lines under the message should have been read");
        details.ShouldContain(Context.ExpandAliases(expectedDetail), "Lines under the message");
        return this;
    }

    /// <summary>Exactly these review lines, in this order.</summary>
    public CheckOutVerification ReviewLines(params string[] expectedSkuAliases)
    {
        ReviewLinesRead().Select(line => (string?)line.Sku).ToList().ShouldBe(Skus(expectedSkuAliases), "Order review lines, in order");
        return this;
    }

    public CheckOutVerification ReviewQuantity(string skuAlias, int expectedQuantity)
    {
        ReviewLine(skuAlias).Quantity.ShouldBe(expectedQuantity, $"Quantity of review line {skuAlias}");
        return this;
    }

    public CheckOutVerification ReviewLineTotal(string skuAlias, string expectedLineTotal)
    {
        ReviewLine(skuAlias).LineTotal.ShouldBe(expectedLineTotal, $"Line total of review line {skuAlias}");
        return this;
    }

    public CheckOutVerification ReviewSubtotal(string expectedSubtotal)
    {
        Resp().ReviewSubtotal.ShouldBe(expectedSubtotal, "Order review subtotal");
        return this;
    }

    public CheckOutVerification OpenedTax(string expectedCountry, string expectedRate)
    {
        Resp().OpenedTaxCountry.ShouldBe(expectedCountry, "Tax country when the checkout opened");
        Resp().OpenedEstimatedTaxRate.ShouldBe(expectedRate, "Estimated tax rate when the checkout opened");
        return this;
    }

    public CheckOutVerification Tax(string expectedCountry, string expectedRate)
    {
        Resp().TaxCountry.ShouldBe(expectedCountry, "Tax country");
        Resp().EstimatedTaxRate.ShouldBe(expectedRate, "Estimated tax rate");
        return this;
    }

    public CheckOutVerification ReviewNote(string expectedNote)
    {
        Resp().ReviewNote.ShouldBe(expectedNote, "Order review note");
        return this;
    }

    public CheckOutVerification SavedAddressSelected(string expectedAddress)
    {
        Resp().SavedAddress.ShouldBe(expectedAddress, "Saved address shown");
        Resp().SavedAddressSelected.ShouldBe(true, "Saved address should be selected");
        return this;
    }

    public CheckOutVerification AddressFormOpen()
    {
        Resp().AddressFormOpen.ShouldBe(true, "Shipping address form should be open");
        return this;
    }

    public CheckOutVerification ShippingName(string expectedFirstName, string expectedLastName)
    {
        Resp().ShippingFirstName.ShouldBe(expectedFirstName, "Shipping first name");
        Resp().ShippingLastName.ShouldBe(expectedLastName, "Shipping last name");
        return this;
    }

    public CheckOutVerification SaveAsDefaultTicked()
    {
        Resp().SaveAsDefaultTicked.ShouldBe(true, "\"Save as my default shipping address\" should be ticked");
        return this;
    }

    public CheckOutVerification Thanks(string expectedThanks)
    {
        Resp().Thanks.ShouldBe(expectedThanks, "Confirmation heading");
        return this;
    }

    /// <summary>The confirmed order numbers, in the order shown; each distinct.</summary>
    private List<string> OrderNumbers()
    {
        var orders = Resp().ConfirmedOrders;
        orders.ShouldNotBeNull("Confirmed orders should have been read");
        var numbers = orders.Select(order => order.OrderNumber).ToList();
        numbers.ShouldBeUnique("Confirmed order numbers should each be different");
        return numbers;
    }

    public CheckOutVerification NoOrderConfirmed()
    {
        OrderNumbers().ShouldBeEmpty("Confirmed orders");
        return this;
    }

    public CheckOutVerification EachOrderLinksToItsDetails()
    {
        var orders = Resp().ConfirmedOrders;
        orders.ShouldNotBeNull("Confirmed orders should have been read");
        orders.ShouldNotBeEmpty("Confirmed orders");
        foreach (var order in orders)
        {
            var encoded = Uri.EscapeDataString(order.OrderNumber);
            order.Link.ShouldNotBeNull($"Link of confirmed order {order.OrderNumber}");
            order.Link.ShouldContain("/order-details", Case.Sensitive, $"Link of confirmed order {order.OrderNumber}");
            order.Link.ShouldContain(encoded, Case.Sensitive, $"Link of confirmed order {order.OrderNumber}");
        }
        return this;
    }

    public CheckOutVerification ViewMyOrdersAndContinueShoppingOffered()
    {
        Resp().ViewMyOrdersOffered.ShouldBe(true, "Confirmation should offer \"View my orders\"");
        Resp().ContinueShoppingOffered.ShouldBe(true, "Confirmation should offer \"Continue shopping\"");
        return this;
    }

    public CheckOutVerification RejectedLine(string skuAlias, string expectedReason)
    {
        var sku = Context.GetParamValue(skuAlias);
        var rejected = RejectedLinesRead();
        var line = rejected.FirstOrDefault(r => r.Sku == sku)
            ?? throw new ShouldAssertException($"The confirmation shows no rejected line {skuAlias}; it shows {string.Join(", ", rejected)}");
        line.Reason.ShouldBe(Context.ExpandAliases(expectedReason), $"Why {skuAlias} was rejected");
        return this;
    }

    public CheckOutVerification NoRejectedLines()
    {
        RejectedLinesRead().ShouldBeEmpty("Rejected lines");
        return this;
    }

    /// <summary>Exactly these cart lines, in this order; none for an empty cart.</summary>
    public CheckOutVerification CartLines(params string[] expectedSkuAliases)
    {
        Cart().Select(line => (string?)line.Sku).ToList().ShouldBe(Skus(expectedSkuAliases), "Cart lines after checkout, in order");
        return this;
    }

    public CheckOutVerification CartLineQuantity(string skuAlias, int expectedQuantity)
    {
        var sku = Context.GetParamValue(skuAlias);
        var cart = Cart();
        var line = cart.FirstOrDefault(cartLine => cartLine.Sku == sku)
            ?? throw new ShouldAssertException($"The cart has no line {skuAlias}; it has {string.Join(", ", cart)}");
        line.Quantity.ShouldBe(expectedQuantity, $"Quantity of cart line {skuAlias}");
        return this;
    }

    private List<string?> Skus(params string[] skuAliases)
        => skuAliases.Select(alias => Context.GetParamValue(alias)).ToList();

    private CheckOutResponse.ReviewLineDto ReviewLine(string skuAlias)
    {
        var sku = Context.GetParamValue(skuAlias);
        var lines = ReviewLinesRead();
        return lines.FirstOrDefault(line => line.Sku == sku)
            ?? throw new ShouldAssertException($"The order review has no line {skuAlias}; it has {string.Join(", ", lines)}");
    }

    private List<CheckOutResponse.ReviewLineDto> ReviewLinesRead()
    {
        Resp().ReviewLines.ShouldNotBeNull("Order review should have been read");
        return Resp().ReviewLines!;
    }

    private List<CheckOutResponse.RejectedLineDto> RejectedLinesRead()
    {
        Resp().RejectedLines.ShouldNotBeNull("Rejected lines should have been read");
        return Resp().RejectedLines!;
    }

    private List<ViewProductResponse.CartLineDto> Cart()
    {
        Resp().CartLines.ShouldNotBeNull("Cart should have been read after placing the order");
        return Resp().CartLines!;
    }

    private CheckOutResponse Resp()
    {
        Response.ShouldNotBeNull("Response should not be null");
        return Response;
    }
}
