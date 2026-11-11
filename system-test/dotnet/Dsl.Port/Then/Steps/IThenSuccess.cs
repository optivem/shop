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

    /// <summary>The product page the executed view showed.</summary>
    IThenProductPage ProductPage();

    /// <summary>The cart as the visitor's action left it.</summary>
    IThenCart Cart();

    /// <summary>Where the checkout left the customer.</summary>
    IThenCheckout Checkout();

    TaskAwaiter GetAwaiter();
}
