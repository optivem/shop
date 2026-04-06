using SystemTests.Latest.AcceptanceTests.Base;
using Driver.Port.Shop.Dtos;
using Optivem.Testing;

namespace SystemTests.Latest.AcceptanceTests;

public class PlaceOrderPositiveTest : BaseAcceptanceTest
{
    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    public async Task OrderNumberShouldStartWithORD(Channel channel)
    {
        await Scenario(channel)
            .When().PlaceOrder()
            .Then().ShouldSucceed()
            .And().Order()
            .HasOrderNumberPrefix("ORD-");
    }

    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    public async Task OrderStatusShouldBePlacedAfterPlacingOrder(Channel channel)
    {
        await Scenario(channel)
            .When().PlaceOrder()
            .Then().ShouldSucceed()
            .And().Order()
            .HasStatus(OrderStatus.Placed);
    }

    [Theory]
    [ChannelData(ChannelType.API)]
    public async Task OrderTotalShouldIncludeTax(Channel channel)
    {
        await Scenario(channel)
            .When().PlaceOrder().WithCountry("DE")
            .Then().ShouldSucceed()
            .And().Order()
            .HasSubtotalPrice(20.0m)
            .HasTaxRate(0.19m)
            .HasTotalPrice(23.8m);
    }

    [Theory]
    [ChannelData(ChannelType.API)]
    public async Task OrderTotalShouldReflectCouponDiscount(Channel channel)
    {
        await Scenario(channel)
            .Given().Coupon().WithCode("DISC10").WithDiscountRate(0.1m)
            .When().PlaceOrder().WithCouponCode("DISC10")
            .Then().ShouldSucceed()
            .And().Order()
            .HasSubtotalPrice(18.0m)
            .HasDiscountRate(0.1m)
            .HasAppliedCouponCode("DISC10")
            .HasTotalPrice(18.0m);
    }

    [Theory]
    [ChannelData(ChannelType.API)]
    public async Task OrderTotalShouldApplyCouponDiscountAndTax(Channel channel)
    {
        await Scenario(channel)
            .Given().Coupon().WithCode("COMBO10").WithDiscountRate(0.1m)
            .When().PlaceOrder().WithCountry("GB").WithCouponCode("COMBO10")
            .Then().ShouldSucceed()
            .And().Order()
            .HasSubtotalPrice(18.0m)
            .HasDiscountRate(0.1m)
            .HasTaxRate(0.2m)
            .HasAppliedCouponCode("COMBO10")
            .HasTotalPrice(21.6m);
    }
}
