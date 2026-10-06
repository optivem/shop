using System.Runtime.CompilerServices;

namespace Dsl.Port.Then.Steps;

public interface IThenSuccess
{
    IThenSuccessAnd And();

    IThenOrderHistory OrderHistory();

    TaskAwaiter GetAwaiter();
}
