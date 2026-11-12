using Dsl.Core.Shared;
using Driver.Port.Dtos;
using Common.Domain;
using Shouldly;
using System.Globalization;
using Common;

namespace Dsl.Core.UseCase.UseCases;

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

    public ViewOrderVerification OrderNumberIsNot(string orderNumberResultAlias)
    {
        var otherOrderNumber = Context.GetResultValue(orderNumberResultAlias);
        Response.OrderNumber.ShouldNotBe(otherOrderNumber,
            $"Expected order number to differ from '{otherOrderNumber}', but it was the same");
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

    public ViewOrderVerification Country(string expectedCountryAlias)
    {
        var expectedCountry = Context.GetParamValueOrLiteral(expectedCountryAlias);
        Response.Country.ShouldBe(expectedCountry,
            $"Expected country: '{expectedCountry}', but got: '{Response.Country}'");
        return this;
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

    public ViewOrderVerification SubtotalPrice(decimal expectedSubtotalPrice)
    {
        Response.SubtotalPrice.ShouldBe(expectedSubtotalPrice,
            $"Expected subtotal price: {expectedSubtotalPrice}, but got: {Response.SubtotalPrice}");
        return this;
    }

    public ViewOrderVerification SubtotalPrice(string expectedSubtotalPrice)
    {
        return SubtotalPrice(Converter.ToDecimal(expectedSubtotalPrice)!.Value);
    }

    public ViewOrderVerification SubtotalPriceGreaterThanZero()
    {
        Response.SubtotalPrice.ShouldBeGreaterThan(0m,
            $"Subtotal price should be positive, but was: {Response.SubtotalPrice}");
        return this;
    }

    public ViewOrderVerification DiscountRateGreaterThanOrEqualToZero()
    {
        Response.DiscountRate.ShouldBeGreaterThanOrEqualTo(0m,
            $"Discount rate should be non-negative, but was: {Response.DiscountRate}");
        return this;
    }

    public ViewOrderVerification DiscountAmountGreaterThanOrEqualToZero()
    {
        Response.DiscountAmount.ShouldBeGreaterThanOrEqualTo(0m,
            $"Discount amount should be non-negative, but was: {Response.DiscountAmount}");
        return this;
    }

    public ViewOrderVerification TaxRateGreaterThanOrEqualToZero()
    {
        Response.TaxRate.ShouldBeGreaterThanOrEqualTo(0m,
            $"Tax rate should be non-negative, but was: {Response.TaxRate}");
        return this;
    }

    public ViewOrderVerification TaxAmountGreaterThanOrEqualToZero()
    {
        Response.TaxAmount.ShouldBeGreaterThanOrEqualTo(0m,
            $"Tax amount should be non-negative, but was: {Response.TaxAmount}");
        return this;
    }

    public ViewOrderVerification TotalPriceGreaterThanZero()
    {
        Response.TotalPrice.ShouldBeGreaterThan(0m,
            $"Total price should be positive, but was: {Response.TotalPrice}");
        return this;
    }

    public ViewOrderVerification BasePrice(decimal expectedBasePrice)
    {
        Response.BasePrice.ShouldBe(expectedBasePrice,
            $"Expected base price to be {expectedBasePrice}, but was {Response.BasePrice}");
        return this;
    }

    public ViewOrderVerification BasePrice(string expectedBasePrice)
    {
        return BasePrice(Converter.ToDecimal(expectedBasePrice)!.Value);
    }

    public ViewOrderVerification BasePriceGreaterThanZero()
    {
        Response.BasePrice.ShouldBeGreaterThan(0m,
            $"Base price should be positive, but was: {Response.BasePrice}");
        return this;
    }

    public ViewOrderVerification DiscountRate(decimal expectedDiscountRate)
    {
        Response.DiscountRate.ShouldBe(expectedDiscountRate,
            $"Expected discount rate to be {expectedDiscountRate}, but was {Response.DiscountRate}");
        return this;
    }

    public ViewOrderVerification DiscountRate(string expectedDiscountRate)
    {
        return DiscountRate(Converter.ToDecimal(expectedDiscountRate)!.Value);
    }

    public ViewOrderVerification DiscountAmount(decimal expectedDiscountAmount)
    {
        Response.DiscountAmount.ShouldBe(expectedDiscountAmount,
            $"Expected discount amount to be {expectedDiscountAmount}, but was {Response.DiscountAmount}");
        return this;
    }

    public ViewOrderVerification DiscountAmount(string expectedDiscountAmount)
    {
        return DiscountAmount(Converter.ToDecimal(expectedDiscountAmount)!.Value);
    }

    public ViewOrderVerification TaxRate(decimal expectedTaxRate)
    {
        Response.TaxRate.ShouldBe(expectedTaxRate,
            $"Expected tax rate to be {expectedTaxRate}, but was {Response.TaxRate}");
        return this;
    }

    public ViewOrderVerification TaxRate(string expectedTaxRate)
    {
        return TaxRate(Converter.ToDecimal(expectedTaxRate)!.Value);
    }

    public ViewOrderVerification TaxAmount(decimal expectedTaxAmount)
    {
        Response.TaxAmount.ShouldBe(expectedTaxAmount,
            $"Expected tax amount to be {expectedTaxAmount}, but was {Response.TaxAmount}");
        return this;
    }

    public ViewOrderVerification TaxAmount(string expectedTaxAmount)
    {
        return TaxAmount(Converter.ToDecimal(expectedTaxAmount)!.Value);
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

    public ViewOrderVerification AppliedCouponCode(string expectedCouponCodeAlias)
    {
        var expectedCouponCode = Context.GetParamValue(expectedCouponCodeAlias);
        Response.AppliedCouponCode.ShouldBe(expectedCouponCode,
            $"Expected applied coupon code to be '{expectedCouponCode}', but was '{Response.AppliedCouponCode}'");
        return this;
    }

    public ViewOrderVerification NoAppliedCoupon()
    {
        Response.AppliedCouponCode.ShouldBeNull("Applied coupon code (none)");
        return this;
    }

    /// <summary>UI only: whether the order details offer Cancel Order.</summary>
    public ViewOrderVerification CancelOffered(bool expected)
    {
        Response.CancelOffered.ShouldBe(expected, "Cancel Order offered (UI only)");
        return this;
    }

    /// <summary>UI only: whether the order details offer Deliver Order.</summary>
    public ViewOrderVerification DeliverOffered(bool expected)
    {
        Response.DeliverOffered.ShouldBe(expected, "Deliver Order offered (UI only)");
        return this;
    }

    public ViewOrderVerification ShippingAddress(string firstName, string lastName, string street, string postalCode, string city, string countryAlias)
    {
        var expectedCountry = Context.GetParamValueOrLiteral(countryAlias);
        var actual = Response.ShippingAddress;
        actual.ShouldNotBeNull("Expected the order to have a shipping address, but it had none");
        actual.FirstName.ShouldBe(firstName, $"Expected shipping first name '{firstName}', but got '{actual.FirstName}'");
        actual.LastName.ShouldBe(lastName, $"Expected shipping last name '{lastName}', but got '{actual.LastName}'");
        actual.Street.ShouldBe(street, $"Expected shipping street '{street}', but got '{actual.Street}'");
        actual.PostalCode.ShouldBe(postalCode, $"Expected shipping postal code '{postalCode}', but got '{actual.PostalCode}'");
        actual.City.ShouldBe(city, $"Expected shipping city '{city}', but got '{actual.City}'");
        actual.Country.ShouldBe(expectedCountry, $"Expected shipping country '{expectedCountry}', but got '{actual.Country}'");
        return this;
    }

    /// <summary>The order's country as a code, such as "DE", not an alias: checkout offers only real shipping countries.</summary>
    public ViewOrderVerification CountryCode(string expectedCountry)
    {
        Response.Country.ShouldBe(expectedCountry,
            $"Expected country to be '{expectedCountry}', but was '{Response.Country}'");
        return this;
    }

    /// <summary>The order's shipping address, with its country as a code, not an alias.</summary>
    public ViewOrderVerification ShippingAddressWithCountryCode(string firstName, string lastName, string street, string postalCode, string city, string country)
    {
        var actual = Response.ShippingAddress;
        actual.ShouldNotBeNull("Expected the order to have a shipping address, but it had none");
        actual.FirstName.ShouldBe(firstName, $"Expected shipping first name '{firstName}', but got '{actual.FirstName}'");
        actual.LastName.ShouldBe(lastName, $"Expected shipping last name '{lastName}', but got '{actual.LastName}'");
        actual.Street.ShouldBe(street, $"Expected shipping street '{street}', but got '{actual.Street}'");
        actual.PostalCode.ShouldBe(postalCode, $"Expected shipping postal code '{postalCode}', but got '{actual.PostalCode}'");
        actual.City.ShouldBe(city, $"Expected shipping city '{city}', but got '{actual.City}'");
        actual.Country.ShouldBe(country, $"Expected shipping country '{country}', but got '{actual.Country}'");
        return this;
    }

    /// <summary>The SKU the order is for, as the system returned it.</summary>
    public string? OrderedSku() => Response.Sku;

    public ViewOrderVerification NoShippingAddress()
    {
        Response.ShippingAddress.ShouldBeNull("Expected the order to have no shipping address, but it had one");
        return this;
    }

    public ViewOrderVerification OrderNumberHasPrefix(string expectedPrefix)
    {
        Response.OrderNumber.ShouldStartWith(expectedPrefix, Case.Sensitive,
            $"Expected order number to start with '{expectedPrefix}', but was '{Response.OrderNumber}'");
        return this;
    }
}
