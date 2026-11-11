using Dsl.Port.When.Steps.Base;

namespace Dsl.Port.When.Steps;

/// <summary>
/// The visitor manages the cart. The steps run in this order: put the cart lines in the cart (each from its product
/// page, in the order given); change the ERP; then the visitor's one action, which is viewing the cart unless
/// another is given. The cart is read last. SKUs are aliases.
/// </summary>
public interface IWhenManageCart : IWhenStep
{
    /// <summary>Before the visitor acts, the cart already contains this line.</summary>
    IWhenManageCart WithCartContaining(string skuAlias, int quantity);

    /// <summary>After the cart is filled, the ERP's price of this product changes.</summary>
    IWhenManageCart WithErpPriceChanged(string skuAlias, string unitPrice);

    /// <summary>After the cart is filled, the ERP becomes unavailable.</summary>
    IWhenManageCart WithErpUnavailable();

    /// <summary>After the cart is filled, the visitor views it while the ERP is unavailable; then the ERP is available again.</summary>
    IWhenManageCart WithCartViewedWhileErpUnavailable();

    IWhenManageCart WithCatalogBrowsed();

    /// <summary>Adds the product with its card's "Add to cart" in the catalog (quantity 1).</summary>
    IWhenManageCart WithAddedFromCatalog(string skuAlias);

    IWhenManageCart WithAddedFromProductPage(string skuAlias, string quantity);

    /// <summary>Types this quantity into the product's cart line.</summary>
    IWhenManageCart WithQuantityChanged(string skuAlias, string quantity);

    IWhenManageCart WithQuantityIncreased(string skuAlias);

    IWhenManageCart WithQuantityDecreased(string skuAlias);

    IWhenManageCart WithLineRemoved(string skuAlias);

    /// <summary>Leaves the site and comes back later in the same browser.</summary>
    IWhenManageCart WithSiteLeftAndRevisited();

    IWhenManageCart WithSignedInAsCustomer();
}
