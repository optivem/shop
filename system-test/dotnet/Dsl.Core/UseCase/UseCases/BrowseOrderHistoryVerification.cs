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

    private List<string?> ListedOrderNumbers()
    {
        Response.ShouldNotBeNull("Response should not be null");
        Response.Orders.ShouldNotBeNull("Orders list should not be null");

        return Response.Orders.Select(o => o.OrderNumber).ToList();
    }
}
