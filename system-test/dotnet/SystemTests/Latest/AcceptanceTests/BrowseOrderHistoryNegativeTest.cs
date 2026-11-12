using static SystemTests.Commons.Constants.ErrorMessages;
using SystemTests.Latest.AcceptanceTests.Base;
using Dsl.Core.UseCase;
using Optivem.Testing;
using static Dsl.Core.Scenario.ScenarioDefaults;

namespace SystemTests.Latest.AcceptanceTests;

public class BrowseOrderHistoryNegativeTest : BaseAcceptanceTest
{
    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    [Trait("tag", "UC-002.6")]
    [Trait("tag", "UC-011.5")]
    public async Task CustomerShouldNotSeeAnotherCustomersOrderInHistory(Channel channel)
    {
        await Scenario(channel)
            .Given().Order().PlacedByCustomer("B")
            .And().LoggedInAsCustomer("A")
            .When().BrowseOrderHistory()
            .Then().ShouldSucceed()
            .OrderHistory().DoesNotContainOrder(DefaultOrderNumber);
    }

    [Theory]
    [ChannelData(ChannelType.API)]
    [Trait("tag", "UC-002.8")]
    public async Task ShouldRejectPageSizeAboveTheCap(Channel channel)
    {
        await Scenario(channel)
            .When().BrowseOrderHistory().WithSize("101")
            .Then().ShouldFail()
            .ErrorMessage("The request contains one or more validation errors")
            .FieldErrorMessage("size", "Page size must be between 1 and 100");
    }

    [Theory]
    [ChannelData(ChannelType.API)]
    [Trait("tag", "UC-002.8")]
    public async Task ShouldRejectPageBelowTheFirst(Channel channel)
    {
        await Scenario(channel)
            .When().BrowseOrderHistory().WithPage("0")
            .Then().ShouldFail()
            .ErrorMessage("The request contains one or more validation errors")
            .FieldErrorMessage("page", "Page must be 1 or greater");
    }

    [Theory]
    [ChannelData(ChannelType.API)]
    [Trait("tag", "UC-002.8")]
    public async Task ShouldRejectPageSizeBelowOne(Channel channel)
    {
        await Scenario(channel)
            .When().BrowseOrderHistory().WithSize("0")
            .Then().ShouldFail()
            .ErrorMessage("The request contains one or more validation errors")
            .FieldErrorMessage("size", "Page size must be between 1 and 100");
    }

    [Theory]
    [ChannelData(ChannelType.API)]
    [Trait("tag", "UC-002.9")]
    public async Task ShouldShowNoOrdersWhenNoOrderMatchesTheFilter(Channel channel)
    {
        await Scenario(channel)
            .Given().LoggedInAsCustomer()
            .When().BrowseOrderHistory().WithOrderNumber("NON-EXISTENT-ORDER-99999")
            .Then().ShouldSucceed()
            .OrderHistory().IsEmpty();
    }

    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    [Trait("tag", "UC-011.3")]
    public async Task AdminCustomerFilterShouldExcludeOtherCustomersOrders(Channel channel)
    {
        await Scenario(channel)
            .Given().Order().WithOrderNumber("ORD-A").PlacedByCustomer("A")
            .And().Order().WithOrderNumber("ORD-B").PlacedByCustomer("B")
            .And().LoggedInAsAdmin()
            .When().BrowseOrderHistory().WithCustomer("A").WithOrderNumber("ORD-B")
            .Then().ShouldSucceed()
            .OrderHistory().DoesNotContainOrder("ORD-B");
    }

    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    [Trait("tag", "UC-011.8")]
    public async Task AdminShouldSeeNoOrdersWhenNoOrderMatchesFilters(Channel channel)
    {
        await Scenario(channel)
            .Given().Order().WithOrderNumber("ORD-A").PlacedByCustomer("A")
            .And().Order().WithOrderNumber("ORD-B").PlacedByCustomer("B")
            .And().LoggedInAsAdmin()
            .When().BrowseOrderHistory().WithCustomer("A").WithOrderNumber("ORD-B")
            .Then().ShouldSucceed()
            .OrderHistory().IsEmpty();
    }

    // API only: a customer's order history has no customer filter to choose another customer with.
    [Theory]
    [ChannelData(ChannelType.API)]
    [Trait("tag", "UC-011.5")]
    public async Task CustomerShouldNotSeeAnotherCustomersOrdersThroughCustomerFilter(Channel channel)
    {
        await Scenario(channel)
            .Given().Order().WithOrderNumber("ORD-A").PlacedByCustomer("A")
            .And().Order().WithOrderNumber("ORD-B").PlacedByCustomer("B")
            .And().LoggedInAsCustomer("A")
            .When().BrowseOrderHistory().WithCustomer("B").WithOrderNumber("ORD-B")
            .Then().ShouldSucceed()
            .OrderHistory().DoesNotContainOrder("ORD-B");
    }

    // Not covered: the @UC-011.5 order with no recorded customer (such an order cannot be placed).
    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-011.8")]
    public async Task ShouldTellTheAdminWhenNoOrderMatchesTheFilters(Channel channel)
    {
        await Scenario(channel)
            .Given().Order().WithOrderNumber("ORD-B").PlacedByCustomer("B")
            .And().LoggedInAsAdmin()
            .When().BrowseOrderHistory().WithCustomer("A").WithOrderNumber("ORD-B")
            .Then().ShouldSucceed()
            .OrderHistory().IsEmpty().ShowsMessage("No orders found");
    }

    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    [Trait("tag", "UC-011.6")]
    public async Task ShouldAskAnAnonymousVisitorToSignIn(Channel channel)
    {
        await Scenario(channel)
            .Given().NotLoggedIn()
            .When().BrowseOrderHistory()
            .Then().ShouldFail()
            .ErrorMessage(AuthenticationRequired);
    }
}
