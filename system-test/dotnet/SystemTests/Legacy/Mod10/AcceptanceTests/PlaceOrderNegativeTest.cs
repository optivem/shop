using SystemTests.Commons.Providers;
using SystemTests.Legacy.Mod10.AcceptanceTests.Base;
using Dsl.Core.Shop;
using Optivem.Testing;
using Xunit;

namespace SystemTests.Legacy.Mod10.AcceptanceTests;

public class PlaceOrderNegativeTest : BaseAcceptanceTest
{
    [Theory]
    [ChannelData(ChannelType.API, AlsoForFirstRow = new[] { ChannelType.UI })]
    [ChannelInlineData("3.5")]
    [ChannelInlineData("lala")]
    [ChannelInlineData("invalid-quantity")]
    public async Task ShouldRejectOrderWithNonIntegerQuantity(Channel channel, string nonIntegerQuantity)
    {
        await Scenario(channel)
            .When().PlaceOrder()
                .WithQuantity(nonIntegerQuantity)
            .Then().ShouldFail()
                .ErrorMessage("The request contains one or more validation errors")
                .FieldErrorMessage("quantity", "Quantity must be an integer");
    }

    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    public async Task ShouldRejectOrderForNonExistentProduct(Channel channel)
    {
        await Scenario(channel)
            .When().PlaceOrder()
                .WithSku("NON-EXISTENT-SKU-12345")
                .WithQuantity(1)
            .Then().ShouldFail()
                .ErrorMessage("The request contains one or more validation errors")
                .FieldErrorMessage("sku", "Product does not exist for SKU: NON-EXISTENT-SKU-12345");
    }

    [Theory]
    [ChannelData(ChannelType.API, AlsoForFirstRow = new[] { ChannelType.UI })]
    [ChannelClassData(typeof(EmptyArgumentsProvider))]
    public async Task ShouldRejectOrderWithEmptySku(Channel channel, string sku)
    {
        await Scenario(channel)
            .When().PlaceOrder()
                .WithSku(sku)
                .WithQuantity(1)
            .Then().ShouldFail()
                .ErrorMessage("The request contains one or more validation errors")
                .FieldErrorMessage("sku", "SKU must not be empty");
    }

    [Theory]
    [ChannelData(ChannelType.API, AlsoForFirstRow = new[] { ChannelType.UI })]
    [ChannelInlineData("-10")]
    [ChannelInlineData("-1")]
    [ChannelInlineData("0")]
    public async Task ShouldRejectOrderWithNonPositiveQuantity(Channel channel, string quantity)
    {
        await Scenario(channel)
            .When().PlaceOrder()
                .WithQuantity(quantity)
            .Then().ShouldFail()
                .ErrorMessage("The request contains one or more validation errors")
                .FieldErrorMessage("quantity", "Quantity must be positive");
    }

    [Theory]
    [ChannelData(ChannelType.API, AlsoForFirstRow = new[] { ChannelType.UI })]
    [ChannelClassData(typeof(EmptyArgumentsProvider))]
    public async Task ShouldRejectOrderWithEmptyQuantity(Channel channel, string quantity)
    {
        await Scenario(channel)
            .When().PlaceOrder()
                .WithQuantity(quantity)
            .Then().ShouldFail()
                .ErrorMessage("The request contains one or more validation errors")
                .FieldErrorMessage("quantity", "Quantity must not be empty");
    }

    [Theory]
    [ChannelData(ChannelType.API)]
    public async Task ShouldRejectOrderWithNullQuantity(Channel channel)
    {
        await Scenario(channel)
            .When().PlaceOrder()
                .WithQuantity(null)
            .Then().ShouldFail()
                .ErrorMessage("The request contains one or more validation errors")
                .FieldErrorMessage("quantity", "Quantity must not be empty");
    }
}
