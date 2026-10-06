using System.Runtime.CompilerServices;

namespace Dsl.Port.Then.Steps;

public interface IThenOrderHistory
{
    IThenOrderHistory ContainsOrder(string orderNumber);

    IThenOrderHistory DoesNotContainOrder(string orderNumber);

    TaskAwaiter GetAwaiter();
}
