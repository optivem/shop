using Dsl.Core.Shared;
using Driver.Port.Shop.Dtos;
using Shouldly;
using System.Globalization;
using Common;

namespace Dsl.Core.Shop.UseCases;

public class ViewOrderVerification : ResponseVerification<ViewOrderResponse>
{
    public ViewOrderVerification(ViewOrderResponse response, UseCaseContext context)
        : base(response, context)
    {
    }

    public ViewOrderVerification OrderNumber(string orderNumberResultAlias)
    {
        var expectedOrderNumber = Context.GetResultValue(orderNumberResultAlias);
        Response.OrderNumber.ShouldBe(expectedOrderNumber,
            $"Expected order number to be '{expectedOrderNumber}', but was '{Response.OrderNumber}'");
        return this;
    }

    public ViewOrderVerification Sku(string skuParamAlias)
    {
        var expectedSku = Context.GetParamValue(skuParamAlias);
        Response.Sku.ShouldBe(expectedSku,
            $"Expected SKU to be '{expectedSku}', but was '{Response.Sku}'");
        return this;
    }

    public ViewOrderVerification Quantity(int expectedQuantity)
    {
        Response.Quantity.ShouldBe(expectedQuantity,
            $"Expected quantity: {expectedQuantity}, but got: {Response.Quantity}");
        return this;
    }

    public ViewOrderVerification Quantity(string expectedQuantity)
    {
        return Quantity(Converter.ToInteger(expectedQuantity)!.Value);
    }

    public ViewOrderVerification Status(OrderStatus expectedStatus)
    {
        Response.Status.ShouldBe(expectedStatus,
            $"Expected status: {expectedStatus}, but got: {Response.Status}");
        return this;
    }

    public ViewOrderVerification Status(string expectedStatus)
    {
        return Status(Enum.Parse<OrderStatus>(expectedStatus));
    }

    public ViewOrderVerification UnitPrice(decimal expectedUnitPrice)
    {
        Response.UnitPrice.ShouldBe(expectedUnitPrice,
            $"Expected unit price: {expectedUnitPrice}, but got: {Response.UnitPrice}");
        return this;
    }

    public ViewOrderVerification UnitPrice(string expectedUnitPrice)
    {
        return UnitPrice(Converter.ToDecimal(expectedUnitPrice)!.Value);
    }

    public ViewOrderVerification UnitPriceGreaterThanZero()
    {
        Response.UnitPrice.ShouldBeGreaterThan(0m,
            $"Unit price should be positive, but was: {Response.UnitPrice}");
        return this;
    }

    public ViewOrderVerification TotalPriceGreaterThanZero()
    {
        Response.TotalPrice.ShouldBeGreaterThan(0m,
            $"Total price should be positive, but was: {Response.TotalPrice}");
        return this;
    }

    public ViewOrderVerification TotalPrice(decimal expectedTotalPrice)
    {
        Response.TotalPrice.ShouldBe(expectedTotalPrice,
            $"Expected total price to be {expectedTotalPrice}, but was {Response.TotalPrice}");
        return this;
    }

    public ViewOrderVerification TotalPrice(string expectedTotalPrice)
    {
        return TotalPrice(Converter.ToDecimal(expectedTotalPrice)!.Value);
    }

    public ViewOrderVerification OrderNumberHasPrefix(string expectedPrefix)
    {
        Response.OrderNumber.ShouldStartWith(expectedPrefix, Case.Sensitive,
            $"Expected order number to start with '{expectedPrefix}', but was '{Response.OrderNumber}'");
        return this;
    }

    public ViewOrderVerification SubtotalPrice(decimal expectedSubtotalPrice)
    {
        Response.SubtotalPrice.ShouldBe(expectedSubtotalPrice,
            $"Expected subtotal price: {expectedSubtotalPrice}, but got: {Response.SubtotalPrice}");
        return this;
    }

    public ViewOrderVerification TaxRate(decimal expectedTaxRate)
    {
        Response.TaxRate.ShouldBe(expectedTaxRate,
            $"Expected tax rate: {expectedTaxRate}, but got: {Response.TaxRate}");
        return this;
    }

    public ViewOrderVerification DiscountRate(decimal expectedDiscountRate)
    {
        Response.DiscountRate.ShouldBe(expectedDiscountRate,
            $"Expected discount rate: {expectedDiscountRate}, but got: {Response.DiscountRate}");
        return this;
    }

    public ViewOrderVerification AppliedCouponCode(string expectedCouponCode)
    {
        Response.AppliedCouponCode.ShouldBe(expectedCouponCode,
            $"Expected applied coupon code: '{expectedCouponCode}', but got: '{Response.AppliedCouponCode}'");
        return this;
    }
}



