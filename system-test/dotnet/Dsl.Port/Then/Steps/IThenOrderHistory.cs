using System.Runtime.CompilerServices;
using Common.Domain;

namespace Dsl.Port.Then.Steps;

public interface IThenOrderHistory
{
    IThenOrderHistory ContainsOrder(string orderNumber);

    IThenOrderHistory DoesNotContainOrder(string orderNumber);

    IThenOrderHistory IsEmpty();

    IThenOrderHistory HasTotalElements(int expectedTotalElements);

    /// <summary>The listed order shows who placed it (a customer alias), its SKU (an alias), quantity and status.</summary>
    IThenOrderHistory ShowsOrder(string orderNumber, string customerAlias, string skuAlias, int expectedQuantity, OrderStatus expectedStatus);

    /// <summary>The listed order links to its details page (UI only).</summary>
    IThenOrderHistory LinksOrderToItsDetails(string orderNumber);

    /// <summary>The customer filter offers All customers and these customers (aliases), among the others who ordered (UI only).</summary>
    IThenOrderHistory OffersCustomers(params string[] customerAliases);

    /// <summary>The customer filter is set to All customers (UI only).</summary>
    IThenOrderHistory HasCustomerFilterSetToAllCustomers();

    /// <summary>There is no customer filter (UI only).</summary>
    IThenOrderHistory OffersNoCustomerFilter();

    /// <summary>The list has no Customer column (UI only).</summary>
    IThenOrderHistory ShowsNoCustomers();

    /// <summary>The list shows this message instead of orders, such as "No orders found" (UI only).</summary>
    IThenOrderHistory ShowsMessage(string expectedMessage);

    TaskAwaiter GetAwaiter();
}
