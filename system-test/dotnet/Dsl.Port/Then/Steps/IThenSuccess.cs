using System.Runtime.CompilerServices;

namespace Dsl.Port.Then.Steps;

public interface IThenSuccess
{
    IThenSuccessAnd And();

    IThenOrderHistory OrderHistory();

    /// <summary>The profile the executed operation answered with.</summary>
    IThenProfile Profile();

    /// <summary>The catalog the executed browse showed.</summary>
    IThenCatalog Catalog();

    TaskAwaiter GetAwaiter();
}
