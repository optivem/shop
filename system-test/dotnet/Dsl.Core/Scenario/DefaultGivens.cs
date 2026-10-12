using Optivem.Testing;
using static Dsl.Core.Scenario.ScenarioDefaults;

namespace Dsl.Core.Scenario;

/// <summary>
/// Single owner of "which default givens are present". Values come only from ScenarioDefaults;
/// each default is created at most once per scenario.
/// </summary>
public sealed class DefaultGivens
{
    private readonly UseCaseDsl _app;
    private readonly Channel? _channel;
    private bool _hasProduct;
    private bool _hasTaxRate;
    private bool _hasPromotion;
    private bool _hasCoupon;
    private bool _hasOrder;

    public DefaultGivens(UseCaseDsl app, Channel? channel)
    {
        _app = app;
        _channel = channel;
    }

    public void MarkProduct() => _hasProduct = true;

    public void MarkTaxRate() => _hasTaxRate = true;

    public void MarkPromotion() => _hasPromotion = true;

    public void MarkCoupon() => _hasCoupon = true;

    public void MarkOrder() => _hasOrder = true;

    /// <summary>Creates the missing defaults that something depends on: orders, or a When step that acts.</summary>
    public async Task EnsureAsync(bool whenStepRuns)
    {
        if (whenStepRuns && !_hasPromotion)
        {
            (await _app.Erp().ReturnsPromotion()
                .WithActive(DefaultPromotionActive)
                .WithDiscount(DefaultPromotionDiscount)
                .Execute())
                .ShouldSucceed();
            _hasPromotion = true;
        }

        var dependentsExist = whenStepRuns || _hasOrder;

        if (dependentsExist && !_hasProduct)
        {
            (await _app.Erp().ReturnsProduct()
                .Sku(DefaultSku)
                .UnitPrice(DefaultUnitPrice)
                .Execute())
                .ShouldSucceed();
            _hasProduct = true;
        }

        if (dependentsExist && !_hasTaxRate)
        {
            (await _app.Tax().ReturnsTaxRate()
                .Country(DefaultCountry)
                .TaxRate(DefaultTaxRate)
                .Execute())
                .ShouldSucceed();
            _hasTaxRate = true;
        }

        if (_hasOrder && !_hasCoupon)
        {
            var shop = await _app.MyShop(_channel!);
            (await shop.PublishCoupon()
                .CouponCode(DefaultCouponCode)
                .DiscountRate(DefaultDiscountRate)
                .ValidFrom(Empty)
                .ValidTo(Empty)
                .UsageLimit(Empty)
                .Execute())
                .ShouldSucceed();
            _hasCoupon = true;
        }
    }
}
