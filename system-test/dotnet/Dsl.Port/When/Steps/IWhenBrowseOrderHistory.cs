using Dsl.Port.When.Steps.Base;

namespace Dsl.Port.When.Steps;

public interface IWhenBrowseOrderHistory : IWhenStep
{
    IWhenBrowseOrderHistory WithOrderNumber(string? orderNumber);

    /// <summary>
    /// Filters by part of the order number of the order known by this alias, in lower case: a few characters from its
    /// middle, so it matches that order only, whatever the case.
    /// </summary>
    IWhenBrowseOrderHistory WithOrderNumberPartOf(string orderNumber);

    /// <summary>No order-number filter: every order the user may see (the default filters by the default order).</summary>
    IWhenBrowseOrderHistory WithNoOrderNumberFilter();

    /// <summary>After browsing, sets the customer filter to All customers and clears the order number (UI only).</summary>
    IWhenBrowseOrderHistory WithFiltersResetAfterwards();

    IWhenBrowseOrderHistory WithPage(string? page);

    IWhenBrowseOrderHistory WithSize(string? size);

    /// <summary>Filters on the default customer's orders (customer1).</summary>
    IWhenBrowseOrderHistory WithCustomer();

    /// <summary>Filters on the orders of the customer known in this scenario by the given alias.</summary>
    IWhenBrowseOrderHistory WithCustomer(string customerAlias);
}
