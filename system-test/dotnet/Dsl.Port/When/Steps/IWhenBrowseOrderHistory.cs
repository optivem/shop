using Dsl.Port.When.Steps.Base;

namespace Dsl.Port.When.Steps;

public interface IWhenBrowseOrderHistory : IWhenStep
{
    IWhenBrowseOrderHistory WithOrderNumber(string? orderNumber);

    IWhenBrowseOrderHistory WithPage(string? page);

    IWhenBrowseOrderHistory WithSize(string? size);

    /// <summary>Filters on the default customer's orders (customer1).</summary>
    IWhenBrowseOrderHistory WithCustomer();

    /// <summary>Filters on the orders of the customer known in this scenario by the given alias.</summary>
    IWhenBrowseOrderHistory WithCustomer(string customerAlias);
}
