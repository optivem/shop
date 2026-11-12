using Common.Domain;
using Dsl.Core.UseCase;
using Dsl.Port.Given;
using Optivem.Testing;
using SystemTests.Latest.AcceptanceTests.Base;
using Xunit;

namespace SystemTests.Latest.AcceptanceTests;

/// <summary>
/// My orders for a customer of their own, whose orders are placed at set times. The orders are placed out of date order
/// (ORD-1002, ORD-1001, ORD-1003), so a list in placing order would not pass for one in date order.
/// </summary>
[Collection("Isolated")]
[Trait("Category", "isolated")]
public class BrowseOrderHistoryPositiveIsolatedTest : BaseAcceptanceTest
{
    private const string ClockReason = "sets the shared clock to place each order at its own time; parallel runs would clash on the clock";
    private const string None = "(none)";

    private IGivenStage GivenANewCustomersThreeOrders(Channel channel)
        => Scenario(channel)
            .Given().NewCustomer()
            .And().Order().WithOrderNumber("ORD-1002").PlacedAt("2026-01-12T14:30:00Z")
            .And().Order().WithOrderNumber("ORD-1001").PlacedAt("2026-01-10T09:00:00Z")
            .And().Order().WithOrderNumber("ORD-1003").PlacedAt("2026-01-15T08:00:00Z")
            .And().LoggedInAsCustomer();

    [Theory]
    [Isolated(ClockReason)]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    [Trait("tag", "UC-002.1")]
    public async Task ShouldListTheCustomersOrdersNewestFirst(Channel channel)
    {
        await GivenANewCustomersThreeOrders(channel)
            .When().BrowseOrderHistory().WithNoOrderNumberFilter()
            .Then().ShouldSucceed()
            .OrderHistory().ShowsOrders("ORD-1003", "ORD-1002", "ORD-1001");
    }

    [Theory]
    [Isolated(ClockReason)]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    [Trait("tag", "UC-002.1")]
    public async Task ShouldShowTheSummaryOfEachOrder(Channel channel)
    {
        await Scenario(channel)
            .Given().Product().WithSku("BOOK-123").WithUnitPrice("20.00")
            .And().Country().WithCode("US").WithTaxRate("0.10")
            .And().Coupon().WithCouponCode("SUMMER2025").WithDiscountRate("0.10")
            .And().Order().WithOrderNumber("ORD-1001").WithSku("BOOK-123").WithQuantity(5).WithCountry("US")
                .WithCouponCode("SUMMER2025").PlacedAt("2026-01-10T09:00:00Z")
            .And().LoggedInAsCustomer()
            .When().BrowseOrderHistory().WithOrderNumber("ORD-1001")
            .Then().ShouldSucceed()
            .OrderHistory().ShowsOrderSummary("ORD-1001", "2026-01-10T09:00:00Z", "BOOK-123", 5, "99.00", OrderStatus.Placed);
    }

    [Theory]
    [Isolated(ClockReason)]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    [Trait("tag", "UC-002.2")]
    public async Task ShouldFilterOrdersByPartOfTheOrderNumberIgnoringCase(Channel channel)
    {
        await GivenANewCustomersThreeOrders(channel)
            .When().BrowseOrderHistory().WithOrderNumberPartOf("ORD-1002")
            .Then().ShouldSucceed()
            .OrderHistory().ShowsOrders("ORD-1002");
    }

    // The spec's "ord-100" is the part every order number here shares: the generated numbers share only "ORD-".
    [Theory]
    [Isolated(ClockReason)]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    [Trait("tag", "UC-002.2")]
    public async Task ShouldShowEveryOrderWhoseNumberHasTheFilterIgnoringCase(Channel channel)
    {
        await GivenANewCustomersThreeOrders(channel)
            .When().BrowseOrderHistory().WithOrderNumber("ord-")
            .Then().ShouldSucceed()
            .OrderHistory().ShowsOrders("ORD-1003", "ORD-1002", "ORD-1001");
    }

    [Theory]
    [Isolated(ClockReason)]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    [Trait("tag", "UC-002.2")]
    public async Task ShouldShowEveryOrderWhenTheFilterIsEmpty(Channel channel)
    {
        await GivenANewCustomersThreeOrders(channel)
            .When().BrowseOrderHistory().WithOrderNumber("")
            .Then().ShouldSucceed()
            .OrderHistory().ShowsOrders("ORD-1003", "ORD-1002", "ORD-1001");
    }

    [Theory]
    [Isolated(ClockReason)]
    [ChannelData(ChannelType.API)]
    [ChannelInlineData("1", "ORD-1003,ORD-1002")]
    [ChannelInlineData("2", "ORD-1001")]
    [ChannelInlineData("3", None)]
    [Trait("tag", "UC-002.3")]
    public async Task ShouldReadTheOrdersOnePageAtATime(Channel channel, string page, string orders)
    {
        await GivenANewCustomersThreeOrders(channel)
            .When().BrowseOrderHistory().WithNoOrderNumberFilter().WithPage(page).WithSize("2")
            .Then().ShouldSucceed()
            .OrderHistory()
            .ShowsOrders(orders == None ? [] : orders.Split(','))
            .HasTotalElements(3);
    }
}
