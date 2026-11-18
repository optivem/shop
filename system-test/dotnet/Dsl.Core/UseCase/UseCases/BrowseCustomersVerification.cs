using Dsl.Core.Shared;
using Driver.Port.Dtos;
using Shouldly;

namespace Dsl.Core.UseCase.UseCases;

public class BrowseCustomersVerification : ResponseVerification<BrowseCustomersResponse>
{
    public BrowseCustomersVerification(BrowseCustomersResponse response, UseCaseContext context)
        : base(response, context)
    {
    }

    /// <summary>
    /// Each of these customers (usernames) is listed exactly once, in this order. The list is shared, so other
    /// customers may come before, between or after them.
    /// </summary>
    public BrowseCustomersVerification ListsCustomersInOrder(List<string> expectedCustomers)
    {
        Response.ShouldNotBeNull("Response should not be null");
        var listed = Response.Customers;
        listed.ShouldNotBeNull("Customers listed");
        var shown = $"[{string.Join(", ", listed)}]";
        foreach (var customer in expectedCustomers)
        {
            listed.Count(c => c == customer).ShouldBe(1, $"Times customer '{customer}' is listed, in {shown}");
        }

        var positions = expectedCustomers.Select(c => listed.IndexOf(c)).ToList();
        positions.ShouldBe(positions.Order().ToList(),
            $"Customers [{string.Join(", ", expectedCustomers)}] should be listed in this order, in {shown}");
        return this;
    }
}
