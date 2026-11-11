using Dsl.Core.Scenario.Given;
using Dsl.Core.Scenario.When.Steps.Base;
using Dsl.Port;
using Dsl.Port.When.Steps;
using Driver.Port.Dtos;
using Dsl.Core.UseCase;
using Dsl.Core.UseCase.UseCases;

namespace Dsl.Core.Scenario.When.Steps;

public class WhenManageCart : BaseWhen<ManageCartResponse, ManageCartVerification>, IWhenManageCart
{
    private readonly StagedProducts _stagedProducts;
    private readonly List<(string SkuAlias, string Quantity)> _cartLines = [];
    private string? _changedPriceSku;
    private string? _changedPrice;
    private bool _erpUnavailable;
    private bool _viewedWhileErpUnavailable;
    private Func<ManageCart, ManageCart> _action = manageCart => manageCart.ViewCart();

    public WhenManageCart(UseCaseDsl app, ScenarioDsl scenario, Func<Task> ensureGiven, StagedProducts stagedProducts)
        : base(app, scenario, ensureGiven)
    {
        _stagedProducts = stagedProducts;
    }

    public WhenManageCart WithCartContaining(string skuAlias, int quantity)
    {
        _cartLines.Add((skuAlias, quantity.ToString()));
        return this;
    }

    IWhenManageCart IWhenManageCart.WithCartContaining(string skuAlias, int quantity) => WithCartContaining(skuAlias, quantity);

    public WhenManageCart WithErpPriceChanged(string skuAlias, string unitPrice)
    {
        _changedPriceSku = skuAlias;
        _changedPrice = unitPrice;
        return this;
    }

    IWhenManageCart IWhenManageCart.WithErpPriceChanged(string skuAlias, string unitPrice) => WithErpPriceChanged(skuAlias, unitPrice);

    public WhenManageCart WithErpUnavailable()
    {
        _erpUnavailable = true;
        return this;
    }

    IWhenManageCart IWhenManageCart.WithErpUnavailable() => WithErpUnavailable();

    public WhenManageCart WithCartViewedWhileErpUnavailable()
    {
        _viewedWhileErpUnavailable = true;
        return this;
    }

    IWhenManageCart IWhenManageCart.WithCartViewedWhileErpUnavailable() => WithCartViewedWhileErpUnavailable();

    public WhenManageCart WithCatalogBrowsed() => Action(manageCart => manageCart.BrowseCatalog());

    IWhenManageCart IWhenManageCart.WithCatalogBrowsed() => WithCatalogBrowsed();

    public WhenManageCart WithAddedFromCatalog(string skuAlias) => Action(manageCart => manageCart.AddFromCatalog(skuAlias));

    IWhenManageCart IWhenManageCart.WithAddedFromCatalog(string skuAlias) => WithAddedFromCatalog(skuAlias);

    public WhenManageCart WithAddedFromProductPage(string skuAlias, string quantity)
        => Action(manageCart => manageCart.AddFromProductPage(skuAlias, quantity));

    IWhenManageCart IWhenManageCart.WithAddedFromProductPage(string skuAlias, string quantity) => WithAddedFromProductPage(skuAlias, quantity);

    public WhenManageCart WithQuantityChanged(string skuAlias, string quantity)
        => Action(manageCart => manageCart.ChangeQuantity(skuAlias, quantity));

    IWhenManageCart IWhenManageCart.WithQuantityChanged(string skuAlias, string quantity) => WithQuantityChanged(skuAlias, quantity);

    public WhenManageCart WithQuantityIncreased(string skuAlias) => Action(manageCart => manageCart.IncreaseQuantity(skuAlias));

    IWhenManageCart IWhenManageCart.WithQuantityIncreased(string skuAlias) => WithQuantityIncreased(skuAlias);

    public WhenManageCart WithQuantityDecreased(string skuAlias) => Action(manageCart => manageCart.DecreaseQuantity(skuAlias));

    IWhenManageCart IWhenManageCart.WithQuantityDecreased(string skuAlias) => WithQuantityDecreased(skuAlias);

    public WhenManageCart WithLineRemoved(string skuAlias) => Action(manageCart => manageCart.RemoveLine(skuAlias));

    IWhenManageCart IWhenManageCart.WithLineRemoved(string skuAlias) => WithLineRemoved(skuAlias);

    public WhenManageCart WithSiteLeftAndRevisited() => Action(manageCart => manageCart.Revisit());

    IWhenManageCart IWhenManageCart.WithSiteLeftAndRevisited() => WithSiteLeftAndRevisited();

    public WhenManageCart WithSignedInAsCustomer() => Action(manageCart => manageCart.SignIn());

    IWhenManageCart IWhenManageCart.WithSignedInAsCustomer() => WithSignedInAsCustomer();

    private WhenManageCart Action(Func<ManageCart, ManageCart> action)
    {
        _action = action;
        return this;
    }

    /// <summary>When the ERP changes after the cart is filled, filling the cart and the action are separate calls in one session.</summary>
    protected override async Task<ExecutionResult<ManageCartResponse, ManageCartVerification>> Execute(UseCaseDsl app)
    {
        var erpChanges = _changedPriceSku != null || _erpUnavailable || _viewedWhileErpUnavailable;
        var shop = await app.MyShop(ChannelMode.Dynamic, Channel);
        var manageCart = shop.ManageCart();
        if (erpChanges)
        {
            var fillCart = shop.ManageCart();
            foreach (var line in _cartLines)
            {
                fillCart.CartLine(line.SkuAlias, line.Quantity);
            }
            (await fillCart.Execute()).ShouldSucceed();
            await ChangeErpAsync(app, shop);
            manageCart.ContinueSession();
        }
        else
        {
            foreach (var line in _cartLines)
            {
                manageCart.CartLine(line.SkuAlias, line.Quantity);
            }
        }

        var result = await _action(manageCart).Execute();
        return new ExecutionResultBuilder<ManageCartResponse, ManageCartVerification>(result).Build();
    }

    private async Task ChangeErpAsync(UseCaseDsl app, MyShopDsl shop)
    {
        if (_changedPriceSku != null)
        {
            await _stagedProducts.ChangePriceAsync(_changedPriceSku, _changedPrice!);
        }
        if (_viewedWhileErpUnavailable)
        {
            (await app.Erp().ReturnsUnavailable().Execute()).ShouldSucceed();
            (await shop.ManageCart().ContinueSession().ViewCart().Execute()).ShouldSucceed();
            (await app.Erp().ReturnsAvailable().Execute()).ShouldSucceed();
        }
        if (_erpUnavailable)
        {
            (await app.Erp().ReturnsUnavailable().Execute()).ShouldSucceed();
        }
    }
}
