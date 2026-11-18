using System.Runtime.CompilerServices;

namespace Dsl.Port.Then.Steps;

public interface IThenCustomerList
{
    /// <summary>
    /// Each of these customers (aliases) is listed once, in this order. Every customer who has ever ordered on the
    /// shared stack is listed too, so others may come before, between or after them.
    /// </summary>
    IThenCustomerList ListsCustomersInOrder(params string[] customerAliases);

    TaskAwaiter GetAwaiter();
}
