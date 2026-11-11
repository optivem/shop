using Dsl.Core.Scenario.When.Steps.Base;
using Dsl.Port;
using Dsl.Port.When.Steps;
using Driver.Port.Dtos;
using Dsl.Core.UseCase.UseCases;

namespace Dsl.Core.Scenario.When.Steps;

public class WhenViewProduct : BaseWhen<ViewProductResponse, ViewProductVerification>, IWhenViewProduct
{
    private string? _sku;
    private bool _fromCatalog;
    private string? _cartSku;
    private string? _cartQuantity;
    private string? _quantity;
    private bool _increase;
    private bool _decrease;
    private bool _addToCart;
    private bool _backToCatalog;

    public WhenViewProduct(UseCaseDsl app, ScenarioDsl scenario, Func<Task> ensureGiven)
        : base(app, scenario, ensureGiven)
    {
    }

    public WhenViewProduct WithSku(string skuAlias)
    {
        _sku = skuAlias;
        return this;
    }

    IWhenViewProduct IWhenViewProduct.WithSku(string skuAlias) => WithSku(skuAlias);

    public WhenViewProduct FromCatalog()
    {
        _fromCatalog = true;
        return this;
    }

    IWhenViewProduct IWhenViewProduct.FromCatalog() => FromCatalog();

    public WhenViewProduct WithCartContaining(string skuAlias, int quantity)
    {
        _cartSku = skuAlias;
        _cartQuantity = quantity.ToString();
        return this;
    }

    IWhenViewProduct IWhenViewProduct.WithCartContaining(string skuAlias, int quantity) => WithCartContaining(skuAlias, quantity);

    public WhenViewProduct WithQuantity(string quantity)
    {
        _quantity = quantity;
        return this;
    }

    IWhenViewProduct IWhenViewProduct.WithQuantity(string quantity) => WithQuantity(quantity);

    public WhenViewProduct WithQuantityIncreased()
    {
        _increase = true;
        return this;
    }

    IWhenViewProduct IWhenViewProduct.WithQuantityIncreased() => WithQuantityIncreased();

    public WhenViewProduct WithQuantityDecreased()
    {
        _decrease = true;
        return this;
    }

    IWhenViewProduct IWhenViewProduct.WithQuantityDecreased() => WithQuantityDecreased();

    public WhenViewProduct WithAddedToCart(string quantity)
    {
        _quantity = quantity;
        _addToCart = true;
        return this;
    }

    IWhenViewProduct IWhenViewProduct.WithAddedToCart(string quantity) => WithAddedToCart(quantity);

    public WhenViewProduct WithBackToCatalogFollowed()
    {
        _backToCatalog = true;
        return this;
    }

    IWhenViewProduct IWhenViewProduct.WithBackToCatalogFollowed() => WithBackToCatalogFollowed();

    protected override async Task<ExecutionResult<ViewProductResponse, ViewProductVerification>> Execute(UseCaseDsl app)
    {
        var shop = await app.MyShop(ChannelMode.Dynamic, Channel);
        var viewProduct = shop.ViewProduct()
            .Sku(_sku)
            .Quantity(_quantity);
        if (_fromCatalog)
        {
            viewProduct.FromCatalog();
        }
        if (_cartSku != null)
        {
            viewProduct.CartLine(_cartSku, _cartQuantity!);
        }
        if (_increase)
        {
            viewProduct.IncreaseQuantity();
        }
        if (_decrease)
        {
            viewProduct.DecreaseQuantity();
        }
        if (_addToCart)
        {
            viewProduct.AddToCart();
        }
        if (_backToCatalog)
        {
            viewProduct.BackToCatalog();
        }

        var result = await viewProduct.Execute();
        return new ExecutionResultBuilder<ViewProductResponse, ViewProductVerification>(result).Build();
    }
}
