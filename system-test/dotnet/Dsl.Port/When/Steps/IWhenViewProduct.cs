using Dsl.Port.When.Steps.Base;

namespace Dsl.Port.When.Steps;

/// <summary>
/// Views a product as a visitor (or as the logged-in user). The steps run in this order: put the cart line in the
/// cart, open the product (by its page address, or from the catalog), type a quantity, use the stepper, add to the
/// cart; then follow the link back to the catalog. SKUs are aliases.
/// </summary>
public interface IWhenViewProduct : IWhenStep
{
    IWhenViewProduct WithSku(string skuAlias);

    /// <summary>Opens the product from its card in the catalog instead of by its page address.</summary>
    IWhenViewProduct FromCatalog();

    /// <summary>Before viewing the product, the visitor's cart already contains this line.</summary>
    IWhenViewProduct WithCartContaining(string skuAlias, int quantity);

    /// <summary>Types this quantity into the quantity field.</summary>
    IWhenViewProduct WithQuantity(string quantity);

    IWhenViewProduct WithQuantityIncreased();

    IWhenViewProduct WithQuantityDecreased();

    /// <summary>Types this quantity and adds the product to the cart.</summary>
    IWhenViewProduct WithAddedToCart(string quantity);

    /// <summary>After the other steps, follows the link back to the catalog.</summary>
    IWhenViewProduct WithBackToCatalogFollowed();
}
