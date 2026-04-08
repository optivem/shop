using SystemTests.Commons.Providers;
using SystemTests.Latest.AcceptanceTests.Base;
using Dsl.Core.Shop;
using Optivem.Testing;

namespace SystemTests.Latest.AcceptanceTests;

public class PlaceOrderNegativeTest : BaseAcceptanceTest
{
    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    public async Task ShouldRejectOrderWithInvalidQuantity(Channel channel)
    {
        await Scenario(channel)
            .When().PlaceOrder().WithQuantity("invalid-quantity")
            .Then().ShouldFail()
            .ErrorMessage("The request contains one or more validation errors")
            .FieldErrorMessage("quantity", "Quantity must be an integer");
    }

    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    public async Task ShouldRejectOrderWithNonExistentSku(Channel channel)
    {
        await Scenario(channel)
            .When().PlaceOrder().WithSku("NON-EXISTENT-SKU-12345")
            .Then().ShouldFail()
            .ErrorMessage("The request contains one or more validation errors")
            .FieldErrorMessage("sku", "Product does not exist for SKU: NON-EXISTENT-SKU-12345");
    }

    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    public async Task ShouldRejectOrderWithNegativeQuantity(Channel channel)
    {
        await Scenario(channel)
            .When().PlaceOrder().WithQuantity(-10)
            .Then().ShouldFail()
            .ErrorMessage("The request contains one or more validation errors")
            .FieldErrorMessage("quantity", "Quantity must be positive");
    }

    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    public async Task ShouldRejectOrderWithZeroQuantity(Channel channel)
    {
        await Scenario(channel)
            .When().PlaceOrder().WithQuantity(0)
            .Then().ShouldFail()
            .ErrorMessage("The request contains one or more validation errors")
            .FieldErrorMessage("quantity", "Quantity must be positive");
    }

    [Theory]
    [ChannelData(ChannelType.API, AlsoForFirstRow = new[] { ChannelType.UI })]
    [ChannelInlineData("-10")]
    [ChannelInlineData("-1")]
    [ChannelInlineData("0")]
    public async Task ShouldRejectOrderWithNonPositiveQuantity(Channel channel, string quantity)
    {
        await Scenario(channel)
            .When().PlaceOrder().WithQuantity(quantity)
            .Then().ShouldFail()
            .ErrorMessage("The request contains one or more validation errors")
            .FieldErrorMessage("quantity", "Quantity must be positive");
    }

    [Theory]
    [ChannelData(ChannelType.API, AlsoForFirstRow = new[] { ChannelType.UI })]
    [ChannelClassData(typeof(EmptyArgumentsProvider))]
    public async Task ShouldRejectOrderWithEmptySku(Channel channel, string sku)
    {
        await Scenario(channel)
            .When().PlaceOrder().WithSku(sku)
            .Then().ShouldFail()
            .ErrorMessage("The request contains one or more validation errors")
            .FieldErrorMessage("sku", "SKU must not be empty");
    }

    [Theory]
    [ChannelData(ChannelType.API, AlsoForFirstRow = new[] { ChannelType.UI })]
    [ChannelClassData(typeof(EmptyArgumentsProvider))]
    public async Task ShouldRejectOrderWithEmptyQuantity(Channel channel, string emptyQuantity)
    {
        await Scenario(channel)
            .When().PlaceOrder().WithQuantity(emptyQuantity)
            .Then().ShouldFail()
            .ErrorMessage("The request contains one or more validation errors")
            .FieldErrorMessage("quantity", "Quantity must not be empty");
    }

    [Theory]
    [ChannelData(ChannelType.API, AlsoForFirstRow = new[] { ChannelType.UI })]
    [ChannelInlineData("3.5")]
    [ChannelInlineData("lala")]
    public async Task ShouldRejectOrderWithNonIntegerQuantity(Channel channel, string nonIntegerQuantity)
    {
        await Scenario(channel)
            .When().PlaceOrder().WithQuantity(nonIntegerQuantity)
            .Then().ShouldFail()
            .ErrorMessage("The request contains one or more validation errors")
            .FieldErrorMessage("quantity", "Quantity must be an integer");
    }

    [Theory]
    [ChannelData(ChannelType.API, AlsoForFirstRow = new[] { ChannelType.UI })]
    [ChannelClassData(typeof(EmptyArgumentsProvider))]
    public async Task ShouldRejectOrderWithEmptyCountry(Channel channel, string emptyCountry)
    {
        await Scenario(channel)
            .When().PlaceOrder().WithCountry(emptyCountry)
            .Then().ShouldFail()
            .ErrorMessage("The request contains one or more validation errors")
            .FieldErrorMessage("country", "Country must not be empty");
    }

    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    public async Task ShouldRejectOrderWithInvalidCountry(Channel channel)
    {
        await Scenario(channel)
            .When().PlaceOrder().WithCountry("XX")
            .Then().ShouldFail()
            .ErrorMessage("The request contains one or more validation errors")
            .FieldErrorMessage("country", "Country does not exist: XX");
    }

    [Theory]
    [ChannelData(ChannelType.API)]
    public async Task ShouldRejectOrderWithNullQuantity(Channel channel)
    {
        await Scenario(channel)
            .When().PlaceOrder().WithQuantity(null)
            .Then().ShouldFail()
            .ErrorMessage("The request contains one or more validation errors")
            .FieldErrorMessage("quantity", "Quantity must not be empty");
    }

    [Theory]
    [ChannelData(ChannelType.API)]
    public async Task ShouldRejectOrderWithNullSku(Channel channel)
    {
        await Scenario(channel)
            .When().PlaceOrder().WithSku(null)
            .Then().ShouldFail()
            .ErrorMessage("The request contains one or more validation errors")
            .FieldErrorMessage("sku", "SKU must not be empty");
    }

    [Theory]
    [ChannelData(ChannelType.API)]
    public async Task ShouldRejectOrderWithNullCountry(Channel channel)
    {
        await Scenario(channel)
            .When().PlaceOrder().WithCountry(null)
            .Then().ShouldFail()
            .ErrorMessage("The request contains one or more validation errors")
            .FieldErrorMessage("country", "Country must not be empty");
    }

    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    public async Task CannotPlaceOrderWithNonExistentCoupon(Channel channel)
    {
        await Scenario(channel)
            .When().PlaceOrder().WithCouponCode("INVALIDCOUPON")
            .Then().ShouldFail()
            .ErrorMessage("The request contains one or more validation errors")
            .FieldErrorMessage("couponCode", "Coupon code INVALIDCOUPON does not exist");
    }

    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    public async Task CannotPlaceOrderWithCouponThatHasExceededUsageLimit(Channel channel)
    {
        await Scenario(channel)
            .Given().Coupon().WithCouponCode("LIMITED2024").WithUsageLimit(2)
            .And().Order().WithOrderNumber("ORD-1").WithCouponCode("LIMITED2024")
            .And().Order().WithOrderNumber("ORD-2").WithCouponCode("LIMITED2024")
            .When().PlaceOrder().WithOrderNumber("ORD-3").WithCouponCode("LIMITED2024")
            .Then().ShouldFail()
            .ErrorMessage("The request contains one or more validation errors")
            .FieldErrorMessage("couponCode", "Coupon code LIMITED2024 has exceeded its usage limit");
    }
}
