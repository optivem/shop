using Dsl.Core.Scenario.Given;
using Dsl.Core.Scenario.When.Steps.Base;
using Dsl.Port;
using Dsl.Port.When.Steps;
using Driver.Port.Dtos;
using Dsl.Core.UseCase.UseCases;

namespace Dsl.Core.Scenario.When.Steps;

public class WhenCheckOut : BaseWhen<CheckOutResponse, CheckOutVerification>, IWhenCheckOut
{
    private readonly StagedProducts _stagedProducts;
    private readonly List<(string SkuAlias, string Quantity)> _cartLines = [];
    private readonly List<Func<CheckOut, CheckOut>> _steps = [];
    private string? _changedPriceSku;
    private string? _changedPrice;
    private string? _withdrawnSku;
    private string? _couponCode;

    public WhenCheckOut(UseCaseDsl app, ScenarioDsl scenario, Func<Task> ensureGiven, StagedProducts stagedProducts)
        : base(app, scenario, ensureGiven)
    {
        _stagedProducts = stagedProducts;
    }

    public WhenCheckOut WithCartContaining(string skuAlias, int quantity)
    {
        _cartLines.Add((skuAlias, quantity.ToString()));
        return this;
    }

    IWhenCheckOut IWhenCheckOut.WithCartContaining(string skuAlias, int quantity) => WithCartContaining(skuAlias, quantity);

    public WhenCheckOut WithErpPriceChanged(string skuAlias, string unitPrice)
    {
        _changedPriceSku = skuAlias;
        _changedPrice = unitPrice;
        return this;
    }

    IWhenCheckOut IWhenCheckOut.WithErpPriceChanged(string skuAlias, string unitPrice) => WithErpPriceChanged(skuAlias, unitPrice);

    public WhenCheckOut WithErpNoLongerOffering(string skuAlias)
    {
        _withdrawnSku = skuAlias;
        return this;
    }

    IWhenCheckOut IWhenCheckOut.WithErpNoLongerOffering(string skuAlias) => WithErpNoLongerOffering(skuAlias);

    public WhenCheckOut WithProceededFromCart() => Step(checkOut => checkOut.ProceedFromCart());

    IWhenCheckOut IWhenCheckOut.WithProceededFromCart() => WithProceededFromCart();

    public WhenCheckOut WithProceededFromCartAndSignedIn() => Step(checkOut => checkOut.ProceedFromCart().SignIn());

    IWhenCheckOut IWhenCheckOut.WithProceededFromCartAndSignedIn() => WithProceededFromCartAndSignedIn();

    public WhenCheckOut WithShippingAddress(string firstName, string lastName, string street, string postalCode,
        string city, string country)
        => Step(checkOut => checkOut.ShippingAddress(firstName, lastName, street, postalCode, city, country));

    IWhenCheckOut IWhenCheckOut.WithShippingAddress(string firstName, string lastName, string street, string postalCode,
        string city, string country)
        => WithShippingAddress(firstName, lastName, street, postalCode, city, country);

    public WhenCheckOut WithCouponCode(string couponCode)
    {
        _couponCode = couponCode;
        return Step(checkOut => checkOut.CouponCode(couponCode));
    }

    IWhenCheckOut IWhenCheckOut.WithCouponCode(string couponCode) => WithCouponCode(couponCode);

    public WhenCheckOut WithOrderPlaced() => Step(checkOut => checkOut.PlaceOrder());

    IWhenCheckOut IWhenCheckOut.WithOrderPlaced() => WithOrderPlaced();

    private WhenCheckOut Step(Func<CheckOut, CheckOut> step)
    {
        _steps.Add(step);
        return this;
    }

    /// <summary>When the ERP changes after the cart is filled, filling the cart and the checkout are separate calls in one session.</summary>
    protected override async Task<ExecutionResult<CheckOutResponse, CheckOutVerification>> Execute(UseCaseDsl app)
    {
        var erpChanges = _changedPriceSku != null || _withdrawnSku != null;
        var shop = await app.MyShop(ChannelMode.Dynamic, Channel);
        var checkOut = shop.CheckOut();
        if (erpChanges)
        {
            var fillCart = shop.ManageCart();
            foreach (var line in _cartLines)
            {
                fillCart.CartLine(line.SkuAlias, line.Quantity);
            }
            (await fillCart.Execute()).ShouldSucceed();
            if (_changedPriceSku != null)
            {
                await _stagedProducts.ChangePriceAsync(_changedPriceSku, _changedPrice!);
            }
            if (_withdrawnSku != null)
            {
                await _stagedProducts.WithdrawAsync(_withdrawnSku);
            }
            checkOut.ContinueSession();
        }
        else
        {
            foreach (var line in _cartLines)
            {
                checkOut.CartLine(line.SkuAlias, line.Quantity);
            }
        }
        foreach (var step in _steps)
        {
            step(checkOut);
        }

        var result = await checkOut.Execute();
        return new ExecutionResultBuilder<CheckOutResponse, CheckOutVerification>(result).CouponCode(_couponCode).Build();
    }
}
