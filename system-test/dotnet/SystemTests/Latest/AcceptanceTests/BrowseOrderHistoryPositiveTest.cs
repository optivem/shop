using Common.Domain;
using SystemTests.Latest.AcceptanceTests.Base;
using Dsl.Core.UseCase;
using Optivem.Testing;
using static Dsl.Core.Scenario.ScenarioDefaults;

namespace SystemTests.Latest.AcceptanceTests;

public class BrowseOrderHistoryPositiveTest : BaseAcceptanceTest
{
    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    [Trait("tag", "UC-002.1")]
    [Trait("tag", "UC-002.2")]
    [Trait("tag", "UC-011.5")]
    public async Task CustomerShouldSeeOwnOrderInHistory(Channel channel)
    {
        await Scenario(channel)
            .Given().Order().PlacedByCustomer()
            .And().LoggedInAsCustomer()
            .When().BrowseOrderHistory()
            .Then().ShouldSucceed()
            .OrderHistory().ContainsOrder(DefaultOrderNumber);
    }

    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    [Trait("tag", "UC-011.1")]
    public async Task AdminShouldSeeCustomerOrderInHistory(Channel channel)
    {
        await Scenario(channel)
            .Given().Order().PlacedByCustomer()
            .And().LoggedInAsAdmin()
            .When().BrowseOrderHistory()
            .Then().ShouldSucceed()
            .OrderHistory().ContainsOrder(DefaultOrderNumber);
    }

    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    [Trait("tag", "UC-011.1")]
    public async Task AdminShouldSeeAnotherCustomersOrderInHistory(Channel channel)
    {
        await Scenario(channel)
            .Given().Order().PlacedByCustomer("B")
            .And().LoggedInAsAdmin()
            .When().BrowseOrderHistory()
            .Then().ShouldSucceed()
            .OrderHistory().ContainsOrder(DefaultOrderNumber);
    }

    [Theory]
    [ChannelData(ChannelType.API)]
    [Trait("tag", "UC-002.3")]
    public async Task FirstPageShouldReportTheTotalNumberOfMatchingOrders(Channel channel)
    {
        await Scenario(channel)
            .Given().Order().PlacedByCustomer()
            .And().LoggedInAsCustomer()
            .When().BrowseOrderHistory().WithPage("1").WithSize("1")
            .Then().ShouldSucceed()
            .OrderHistory().ContainsOrder(DefaultOrderNumber).HasTotalElements(1);
    }

    [Theory]
    [ChannelData(ChannelType.API)]
    [Trait("tag", "UC-002.3")]
    public async Task PagePastTheLastShouldBeEmpty(Channel channel)
    {
        await Scenario(channel)
            .Given().Order().PlacedByCustomer()
            .And().LoggedInAsCustomer()
            .When().BrowseOrderHistory().WithPage("2")
            .Then().ShouldSucceed()
            .OrderHistory().IsEmpty();
    }

    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    [Trait("tag", "UC-011.3")]
    [Trait("tag", "UC-011.4")]
    public async Task AdminShouldSeeOrderOfChosenCustomer(Channel channel)
    {
        await Scenario(channel)
            .Given().Order().WithOrderNumber("ORD-A").PlacedByCustomer("A")
            .And().LoggedInAsAdmin()
            .When().BrowseOrderHistory().WithCustomer("A").WithOrderNumber("ORD-A")
            .Then().ShouldSucceed()
            .OrderHistory().ContainsOrder("ORD-A");
    }

    [Theory]
    [ChannelData(ChannelType.API)]
    [Trait("tag", "UC-011.5")]
    public async Task CustomerFilterShouldBeIgnoredForCustomer(Channel channel)
    {
        await Scenario(channel)
            .Given().Order().WithOrderNumber("ORD-A").PlacedByCustomer("A")
            .And().Order().WithOrderNumber("ORD-B").PlacedByCustomer("B")
            .And().LoggedInAsCustomer("A")
            .When().BrowseOrderHistory().WithCustomer("B").WithOrderNumber("ORD-A")
            .Then().ShouldSucceed()
            .OrderHistory().ContainsOrder("ORD-A");
    }

    // Not covered: the "newest first" order of @UC-011.1 (the given orders cannot be dated) and @UC-011.2 (an order
    // with no recorded customer cannot be placed).
    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    [Trait("tag", "UC-011.1")]
    public async Task ShouldShowTheDetailsOfEachOrderInTheList(Channel channel)
    {
        await Scenario(channel)
            .Given().Order().WithOrderNumber("ORD-B").WithQuantity(2).PlacedByCustomer("B")
            .And().LoggedInAsAdmin()
            .When().BrowseOrderHistory().WithOrderNumber("ORD-B")
            .Then().ShouldSucceed()
            .OrderHistory().ShowsOrder("ORD-B", "B", DefaultSku, 2, OrderStatus.Placed);
    }

    // Opening the linked order is ViewOrderPositiveTest's AdminShouldBeAbleToViewAnotherCustomersOrder.
    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-011.1")]
    public async Task ShouldLinkEachOrderToItsDetails(Channel channel)
    {
        await Scenario(channel)
            .Given().Order().WithOrderNumber("ORD-B").PlacedByCustomer("B")
            .And().LoggedInAsAdmin()
            .When().BrowseOrderHistory().WithOrderNumber("ORD-B")
            .Then().ShouldSucceed()
            .OrderHistory().LinksOrderToItsDetails("ORD-B");
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-011.3")]
    public async Task ShouldOfferTheCustomersWhoHavePlacedOrders(Channel channel)
    {
        await Scenario(channel)
            .Given().Order().WithOrderNumber("ORD-A").PlacedByCustomer("A")
            .And().Order().WithOrderNumber("ORD-B").PlacedByCustomer("B")
            .And().LoggedInAsAdmin()
            .When().BrowseOrderHistory().WithNoOrderNumberFilter()
            .Then().ShouldSucceed()
            .OrderHistory().OffersCustomers("A", "B").HasCustomerFilterSetToAllCustomers();
    }

    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    [Trait("tag", "UC-011.3")]
    public async Task ShouldShowOnlyTheOrdersOfTheChosenCustomer(Channel channel)
    {
        await Scenario(channel)
            .Given().Order().WithOrderNumber("ORD-A").PlacedByCustomer("A")
            .And().Order().WithOrderNumber("ORD-B").PlacedByCustomer("B")
            .And().LoggedInAsAdmin()
            .When().BrowseOrderHistory().WithCustomer("A").WithNoOrderNumberFilter()
            .Then().ShouldSucceed()
            .OrderHistory().ContainsOrder("ORD-A").DoesNotContainOrder("ORD-B");
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-011.7")]
    public async Task ShouldNotShowACustomerTheCustomerColumnOrTheCustomerFilter(Channel channel)
    {
        await Scenario(channel)
            .Given().Order().PlacedByCustomer()
            .And().LoggedInAsCustomer()
            .When().BrowseOrderHistory()
            .Then().ShouldSucceed()
            .OrderHistory().ContainsOrder(DefaultOrderNumber).ShowsNoCustomers().OffersNoCustomerFilter();
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-011.8")]
    public async Task ShouldShowEveryOrderAgainAfterResettingTheFilters(Channel channel)
    {
        await Scenario(channel)
            .Given().Order().WithOrderNumber("ORD-A").PlacedByCustomer("A")
            .And().Order().WithOrderNumber("ORD-B").PlacedByCustomer("B")
            .And().LoggedInAsAdmin()
            .When().BrowseOrderHistory().WithCustomer("B").WithOrderNumber("ORD-A").WithFiltersResetAfterwards()
            .Then().ShouldSucceed()
            .OrderHistory().ContainsOrder("ORD-A").ContainsOrder("ORD-B").HasCustomerFilterSetToAllCustomers();
    }

    [Theory]
    [ChannelData(ChannelType.API)]
    [Trait("tag", "UC-002.3")]
    public async Task ShouldShowTheFirstPageOf50OrdersByDefault(Channel channel)
    {
        await Scenario(channel)
            .Given().LoggedInAsCustomer()
            .When().BrowseOrderHistory().WithNoOrderNumberFilter()
            .Then().ShouldSucceed()
            .OrderHistory().HasPage(1).HasPageSize(50);
    }

    // Opening the linked order is ViewOrderPositiveTest's CustomerShouldBeAbleToViewOwnOrder.
    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-002.1")]
    public async Task ShouldLinkEachOfTheCustomersOrdersToItsDetails(Channel channel)
    {
        await Scenario(channel)
            .Given().Order().PlacedByCustomer()
            .And().LoggedInAsCustomer()
            .When().BrowseOrderHistory()
            .Then().ShouldSucceed()
            .OrderHistory().LinksOrderToItsDetails(DefaultOrderNumber);
    }
}
