using SystemTests.Latest.AcceptanceTests.Base;
using Dsl.Core.UseCase;
using Common.Domain;
using Optivem.Testing;

namespace SystemTests.Latest.AcceptanceTests;

public class ViewOrderPositiveTest : BaseAcceptanceTest
{
    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    public async Task ShouldBeAbleToViewOrder(Channel channel)
    {
        await Scenario(channel)
            .Given().Product().WithSku("BOOK-123").WithUnitPrice(20.00m)
            .And().Country().WithCode("US").WithTaxRate(0.10m)
            .And().Coupon().WithCouponCode("SUMMER2025").WithDiscountRate(0.10m)
            .And().Order().WithSku("BOOK-123").WithQuantity(5).WithCountry("US").WithCouponCode("SUMMER2025")
            .When().ViewOrder()
            .Then().ShouldSucceed()
            .And().Order()
            .HasOrderNumberPrefix("ORD-")
            .HasSku("BOOK-123")
            .HasQuantity(5)
            .HasCountry("US")
            .HasUnitPrice(20.00m)
            .HasBasePrice(100.00m)
            .HasAppliedCoupon("SUMMER2025")
            .HasDiscountRate(0.10m)
            .HasDiscountAmount(10.00m)
            .HasSubtotalPrice(90.00m)
            .HasTaxRate(0.10m)
            .HasTaxAmount("9.00")
            .HasTotalPrice(99.00m)
            .HasStatus(OrderStatus.Placed);
    }

    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    public async Task CustomerShouldBeAbleToViewOwnOrder(Channel channel)
    {
        await Scenario(channel)
            .Given().Order().PlacedByCustomer()
            .And().LoggedInAsCustomer()
            .When().ViewOrder()
            .Then().ShouldSucceed();
    }

    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    public async Task SameCustomerAliasShouldResolveToSameCustomer(Channel channel)
    {
        await Scenario(channel)
            .Given().Order().PlacedByCustomer("B")
            .And().LoggedInAsCustomer("B")
            .When().ViewOrder()
            .Then().ShouldSucceed();
    }

    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    public async Task AdminShouldBeAbleToViewCustomerOrder(Channel channel)
    {
        await Scenario(channel)
            .Given().Order().PlacedByCustomer()
            .And().LoggedInAsAdmin()
            .When().ViewOrder()
            .Then().ShouldSucceed();
    }

    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    public async Task AdminShouldBeAbleToViewAnotherCustomersOrder(Channel channel)
    {
        await Scenario(channel)
            .Given().Order().PlacedByCustomer("B")
            .And().LoggedInAsAdmin()
            .When().ViewOrder()
            .Then().ShouldSucceed();
    }
}
