using SystemTests.Latest.AcceptanceTests.Base;
using Dsl.Core.Shop;
using Driver.Port.Shop.Dtos;
using Dsl.Port.Then.Steps;
using Optivem.Testing;

namespace SystemTests.Latest.AcceptanceTests;

public class PlaceOrderPositiveTest : BaseAcceptanceTest
{
    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    public async Task ShouldBeAbleToPlaceOrderForValidInput(Channel channel)
    {
        await Scenario(channel)
            .Given().Product().WithSku("ABC").WithUnitPrice(20.00m)
            .And().Country().WithCode("US").WithTaxRate(0.10m)
            .When().PlaceOrder().WithSku("ABC").WithQuantity(5).WithCountry("US")
            .Then().ShouldSucceed();
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
    [ChannelData(ChannelType.UI, ChannelType.API)]
    public async Task ShouldCalculateBasePriceAsProductOfUnitPriceAndQuantity(Channel channel)
    {
        await Scenario(channel)
            .Given().Product().WithUnitPrice(20.00m)
            .When().PlaceOrder().WithQuantity(5)
            .Then().ShouldSucceed()
            .And().Order()
            .HasBasePrice(100.00m);
    }

    [Theory]
    [ChannelData(ChannelType.API, AlsoForFirstRow = new[] { ChannelType.UI })]
    [ChannelInlineData("20.00", 5, "100.00")]
    [ChannelInlineData("10.00", 3, "30.00")]
    [ChannelInlineData("15.50", 4, "62.00")]
    [ChannelInlineData("99.99", 1, "99.99")]
    public async Task ShouldPlaceOrderWithCorrectBasePriceParameterized(Channel channel, string unitPrice, int quantity, string basePrice)
    {
        await Scenario(channel)
            .Given().Product().WithUnitPrice(unitPrice)
            .When().PlaceOrder().WithQuantity(quantity)
            .Then().ShouldSucceed()
            .And().Order()
            .HasBasePrice(basePrice);
    }

    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    public async Task OrderPrefixShouldBeORD(Channel channel)
    {
        await Scenario(channel)
            .When().PlaceOrder()
            .Then().ShouldSucceed()
            .And().Order()
            .HasOrderNumberPrefix("ORD-");
    }

    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    public async Task DiscountRateShouldBeAppliedForCoupon(Channel channel)
    {
        await Scenario(channel)
            .Given().Coupon().WithCouponCode("SUMMER2025").WithDiscountRate(0.15m)
            .When().PlaceOrder().WithCouponCode("SUMMER2025")
            .Then().ShouldSucceed()
            .And().Order()
            .HasAppliedCoupon("SUMMER2025")
            .HasDiscountRate(0.15m);
    }

    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    public async Task DiscountRateShouldBeNotAppliedWhenThereIsNoCoupon(Channel channel)
    {
        await Scenario(channel)
            .When().PlaceOrder().WithCouponCode(null)
            .Then().ShouldSucceed()
            .And().Order()
            .HasAppliedCoupon(null!)
            .HasDiscountRate(0.00m)
            .HasDiscountAmount(0.00m);
    }

    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    public async Task SubtotalPriceShouldBeCalculatedAsTheBasePriceMinusDiscountAmountWhenWeHaveCoupon(Channel channel)
    {
        await Scenario(channel)
            .Given().Coupon().WithDiscountRate(0.15m)
            .And().Product().WithUnitPrice(20.00m)
            .When().PlaceOrder().WithCouponCode().WithQuantity(5)
            .Then().ShouldSucceed()
            .And().Order()
            .HasAppliedCoupon()
            .HasDiscountRate(0.15m)
            .HasBasePrice(100.00m)
            .HasDiscountAmount(15.00m)
            .HasSubtotalPrice(85.00m);
    }

    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    public async Task SubtotalPriceShouldBeSameAsBasePriceWhenNoCoupon(Channel channel)
    {
        await Scenario(channel)
            .Given().Product().WithUnitPrice(20.00m)
            .When().PlaceOrder().WithQuantity(5)
            .Then().ShouldSucceed()
            .And().Order()
            .HasBasePrice(100.00m)
            .HasDiscountAmount(0.00m)
            .HasSubtotalPrice(100.00m);
    }

    [Theory]
    [ChannelData(ChannelType.API, AlsoForFirstRow = new[] { ChannelType.UI })]
    [ChannelInlineData("UK", 0.09)]
    [ChannelInlineData("US", 0.20)]
    public async Task CorrectTaxRateShouldBeUsedBasedOnCountry(Channel channel, string country, double taxRate)
    {
        await Scenario(channel)
            .Given().Country().WithCode(country).WithTaxRate((decimal)taxRate)
            .When().PlaceOrder().WithCountry(country)
            .Then().ShouldSucceed()
            .And().Order()
            .HasTaxRate((decimal)taxRate);
    }

    [Theory]
    [ChannelData(ChannelType.API, AlsoForFirstRow = new[] { ChannelType.UI })]
    [ChannelInlineData("UK", 0.09, "50.00", "4.50", "54.50")]
    [ChannelInlineData("US", 0.20, "100.00", "20.00", "120.00")]
    public async Task TotalPriceShouldBeSubtotalPricePlusTaxAmount(Channel channel, string country, double taxRate, string subtotalPrice, string expectedTaxAmount, string expectedTotalPrice)
    {
        await Scenario(channel)
            .Given().Country().WithCode(country).WithTaxRate((decimal)taxRate)
            .And().Product().WithUnitPrice(subtotalPrice)
            .When().PlaceOrder().WithCountry(country).WithQuantity(1)
            .Then().ShouldSucceed()
            .And().Order()
            .HasTaxRate((decimal)taxRate)
            .HasSubtotalPrice(subtotalPrice)
            .HasTaxAmount(expectedTaxAmount)
            .HasTotalPrice(expectedTotalPrice);
    }

    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    public async Task CouponUsageCountHasBeenIncrementedAfterItsBeenUsed(Channel channel)
    {
        await Scenario(channel)
            .Given().Coupon().WithCouponCode("SUMMER2025")
            .When().PlaceOrder().WithCouponCode("SUMMER2025")
            .Then().ShouldSucceed()
            .And().Coupon("SUMMER2025")
            .HasUsedCount(1);
    }
}
