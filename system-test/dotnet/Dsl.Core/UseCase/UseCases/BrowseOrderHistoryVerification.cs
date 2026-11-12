using Common.Domain;
using Dsl.Core.Shared;
using Driver.Port.Dtos;
using Shouldly;

namespace Dsl.Core.UseCase.UseCases;

public class BrowseOrderHistoryVerification : ResponseVerification<BrowseOrderHistoryResponse>
{
    public BrowseOrderHistoryVerification(BrowseOrderHistoryResponse response, UseCaseContext context)
        : base(response, context)
    {
    }

    public BrowseOrderHistoryVerification ContainsOrder(string orderNumberResultAlias)
    {
        var orderNumber = Context.GetResultValue(orderNumberResultAlias);
        ListedOrderNumbers().ShouldContain(orderNumber, $"Order history should contain order '{orderNumber}'");
        return this;
    }

    public BrowseOrderHistoryVerification DoesNotContainOrder(string orderNumberResultAlias)
    {
        var orderNumber = Context.GetResultValue(orderNumberResultAlias);
        ListedOrderNumbers().ShouldNotContain(orderNumber, $"Order history should not contain order '{orderNumber}'");
        return this;
    }

    public BrowseOrderHistoryVerification IsEmpty()
    {
        ListedOrderNumbers().ShouldBeEmpty("Order history page should be empty");
        return this;
    }

    public BrowseOrderHistoryVerification TotalElements(long expectedTotalElements)
    {
        Response.TotalElements.ShouldBe(expectedTotalElements, "Order history total elements");
        return this;
    }

    /// <summary>The listed order shows this customer (a username), SKU (an alias), quantity and status.</summary>
    public BrowseOrderHistoryVerification ShowsOrder(string orderNumberResultAlias, string expectedCustomer, string skuAlias,
        int expectedQuantity, OrderStatus expectedStatus)
    {
        var order = ListedOrder(orderNumberResultAlias);
        var expectedSku = Context.GetParamValue(skuAlias);
        order.Customer.ShouldBe(expectedCustomer, $"Customer of order '{order.OrderNumber}'");
        order.Sku.ShouldBe(expectedSku, $"SKU of order '{order.OrderNumber}'");
        order.Quantity.ShouldBe(expectedQuantity, $"Quantity of order '{order.OrderNumber}'");
        order.Status.ShouldBe(expectedStatus, $"Status of order '{order.OrderNumber}'");
        return this;
    }

    /// <summary>UI only: the listed order's "View Details" link goes to its details page.</summary>
    public BrowseOrderHistoryVerification LinksOrderToItsDetails(string orderNumberResultAlias)
    {
        var order = ListedOrder(orderNumberResultAlias);
        order.DetailsPath.ShouldNotBeNull($"Details link of order '{order.OrderNumber}'");
        order.DetailsPath.ShouldContain("/order-details", Case.Sensitive, $"Details link of order '{order.OrderNumber}'");
        order.DetailsPath.ShouldContain(Uri.EscapeDataString(order.OrderNumber!), Case.Sensitive,
            $"Details link of order '{order.OrderNumber}'");
        return this;
    }

    /// <summary>UI only: the customer filter offers All customers and each of these usernames.</summary>
    public BrowseOrderHistoryVerification OffersCustomers(List<string> expectedCustomers)
    {
        Response.CustomerFilterOptions.ShouldNotBeNull("Customer filter choices");
        Response.CustomerFilterOptions.ShouldContain("All customers", "Customer filter choices");
        foreach (var customer in expectedCustomers)
        {
            Response.CustomerFilterOptions.ShouldContain(customer,
                $"Customer filter choices [{string.Join(", ", Response.CustomerFilterOptions)}]");
        }
        return this;
    }

    /// <summary>UI only: the customer filter's chosen option, as shown.</summary>
    public BrowseOrderHistoryVerification CustomerFilterSetTo(string expectedSelection)
    {
        Response.CustomerFilterSelection.ShouldBe(expectedSelection, "Customer filter choice");
        return this;
    }

    /// <summary>UI only: there is no customer filter.</summary>
    public BrowseOrderHistoryVerification NoCustomerFilter()
    {
        Response.CustomerFilterOptions.ShouldBeNull("Customer filter choices");
        return this;
    }

    /// <summary>UI only: the list has no Customer column.</summary>
    public BrowseOrderHistoryVerification NoCustomerColumn()
    {
        Response.CustomerColumnShown.ShouldBe(false, "Customer column shown");
        return this;
    }

    /// <summary>UI only: the message the list shows instead of orders.</summary>
    public BrowseOrderHistoryVerification Message(string expectedMessage)
    {
        Response.Message.ShouldBe(expectedMessage, "Order list message");
        return this;
    }

    private BrowseOrderHistoryResponse.OrderDto ListedOrder(string orderNumberResultAlias)
    {
        var orderNumber = Context.GetResultValue(orderNumberResultAlias);
        return Response.Orders?.FirstOrDefault(order => order.OrderNumber == orderNumber)
            ?? throw new ShouldAssertException(
                $"Order history should list order '{orderNumber}', but lists [{string.Join(", ", ListedOrderNumbers())}]");
    }

    private List<string?> ListedOrderNumbers()
    {
        Response.ShouldNotBeNull("Response should not be null");
        Response.Orders.ShouldNotBeNull("Orders list should not be null");

        return Response.Orders.Select(o => o.OrderNumber).ToList();
    }
}
