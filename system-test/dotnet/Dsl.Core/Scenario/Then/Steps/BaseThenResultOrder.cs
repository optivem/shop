using System.Runtime.CompilerServices;
using Dsl.Port.Then.Steps;
using Dsl.Core.Shared;
using Dsl.Core.Scenario;
using Driver.Adapter;
using Dsl.Core.Gherkin;
using Driver.Port.Shop.Dtos;
using Dsl.Core.Shop.UseCases;

namespace Dsl.Core.Scenario.Then;

public abstract class BaseThenResultOrder<TSuccessResponse, TSuccessVerification, TDerived>
    : IThenOrder
    where TSuccessVerification : ResponseVerification<TSuccessResponse>
    where TDerived : BaseThenResultOrder<TSuccessResponse, TSuccessVerification, TDerived>
{
    protected readonly ThenStage<TSuccessResponse, TSuccessVerification> _thenClause;
    protected readonly Func<Task<string>> _orderNumberFactory;
    protected readonly List<Action<ViewOrderVerification>> _verifications = [];

    protected BaseThenResultOrder(
        ThenStage<TSuccessResponse, TSuccessVerification> thenClause,
        Func<Task<string>> orderNumberFactory)
    {
        _thenClause = thenClause;
        _orderNumberFactory = orderNumberFactory;
    }

    protected abstract Task<ViewOrderVerification?> RunPrelude(ExecutionResult<TSuccessResponse, TSuccessVerification> result);

    protected TDerived Self => (TDerived)this;

    public TDerived HasStatus(OrderStatus expectedStatus)
    {
        _verifications.Add(v => v.Status(expectedStatus));
        return Self;
    }

    IThenOrder IThenOrder.HasStatus(OrderStatus expectedStatus) => HasStatus(expectedStatus);

    public TDerived HasBasePrice(decimal expectedBasePrice)
    {
        _verifications.Add(v => v.BasePrice(expectedBasePrice));
        return Self;
    }

    IThenOrder IThenOrder.HasBasePrice(decimal expectedBasePrice) => HasBasePrice(expectedBasePrice);

    public TDerived HasBasePrice(string basePrice)
    {
        _verifications.Add(v => v.BasePrice(basePrice));
        return Self;
    }

    IThenOrder IThenOrder.HasBasePrice(string basePrice) => HasBasePrice(basePrice);

    public TDerived HasSubtotalPrice(decimal expectedSubtotalPrice)
    {
        _verifications.Add(v => v.SubtotalPrice(expectedSubtotalPrice));
        return Self;
    }

    IThenOrder IThenOrder.HasSubtotalPrice(decimal expectedSubtotalPrice) => HasSubtotalPrice(expectedSubtotalPrice);

    public TDerived HasSubtotalPrice(string expectedSubtotalPrice)
    {
        _verifications.Add(v => v.SubtotalPrice(expectedSubtotalPrice));
        return Self;
    }

    IThenOrder IThenOrder.HasSubtotalPrice(string expectedSubtotalPrice) => HasSubtotalPrice(expectedSubtotalPrice);

    public TDerived HasTotalPrice(decimal expectedTotalPrice)
    {
        _verifications.Add(v => v.TotalPrice(expectedTotalPrice));
        return Self;
    }

    IThenOrder IThenOrder.HasTotalPrice(decimal expectedTotalPrice) => HasTotalPrice(expectedTotalPrice);

    public TDerived HasTotalPrice(string expectedTotalPrice)
    {
        _verifications.Add(v => v.TotalPrice(expectedTotalPrice));
        return Self;
    }

    IThenOrder IThenOrder.HasTotalPrice(string expectedTotalPrice) => HasTotalPrice(expectedTotalPrice);

    public TDerived HasTaxRate(decimal expectedTaxRate)
    {
        _verifications.Add(v => v.TaxRate(expectedTaxRate));
        return Self;
    }

    IThenOrder IThenOrder.HasTaxRate(decimal expectedTaxRate) => HasTaxRate(expectedTaxRate);

    public TDerived HasTaxRate(string expectedTaxRate)
    {
        _verifications.Add(v => v.TaxRate(expectedTaxRate));
        return Self;
    }

    IThenOrder IThenOrder.HasTaxRate(string expectedTaxRate) => HasTaxRate(expectedTaxRate);

    public TDerived HasTaxAmount(string expectedTaxAmount)
    {
        _verifications.Add(v => v.TaxAmount(expectedTaxAmount));
        return Self;
    }

    IThenOrder IThenOrder.HasTaxAmount(string expectedTaxAmount) => HasTaxAmount(expectedTaxAmount);

    public TDerived HasDiscountRate(decimal expectedDiscountRate)
    {
        _verifications.Add(v => v.DiscountRate(expectedDiscountRate));
        return Self;
    }

    IThenOrder IThenOrder.HasDiscountRate(decimal expectedDiscountRate) => HasDiscountRate(expectedDiscountRate);

    public TDerived HasDiscountAmount(decimal expectedDiscountAmount)
    {
        _verifications.Add(v => v.DiscountAmount(expectedDiscountAmount));
        return Self;
    }

    IThenOrder IThenOrder.HasDiscountAmount(decimal expectedDiscountAmount) => HasDiscountAmount(expectedDiscountAmount);

    public TDerived HasDiscountAmount(string expectedDiscountAmount)
    {
        _verifications.Add(v => v.DiscountAmount(expectedDiscountAmount));
        return Self;
    }

    IThenOrder IThenOrder.HasDiscountAmount(string expectedDiscountAmount) => HasDiscountAmount(expectedDiscountAmount);

    public TDerived HasAppliedCoupon(string expectedCouponCode)
    {
        _verifications.Add(v => v.AppliedCouponCode(expectedCouponCode));
        return Self;
    }

    IThenOrder IThenOrder.HasAppliedCoupon(string expectedCouponCode) => HasAppliedCoupon(expectedCouponCode);

    public TDerived HasAppliedCoupon()
    {
        _verifications.Add(v => v.AppliedCouponCode(GherkinDefaults.DefaultCouponCode));
        return Self;
    }

    IThenOrder IThenOrder.HasAppliedCoupon() => HasAppliedCoupon();

    public TDerived HasOrderNumberPrefix(string expectedPrefix)
    {
        _verifications.Add(v => v.OrderNumberHasPrefix(expectedPrefix));
        return Self;
    }

    IThenOrder IThenOrder.HasOrderNumberPrefix(string expectedPrefix) => HasOrderNumberPrefix(expectedPrefix);

    public TDerived HasSku(string expectedSku)
    {
        _verifications.Add(v => v.Sku(expectedSku));
        return Self;
    }

    IThenOrder IThenOrder.HasSku(string expectedSku) => HasSku(expectedSku);

    public TDerived HasQuantity(int expectedQuantity)
    {
        _verifications.Add(v => v.Quantity(expectedQuantity));
        return Self;
    }

    IThenOrder IThenOrder.HasQuantity(int expectedQuantity) => HasQuantity(expectedQuantity);

    public TDerived HasCountry(string expectedCountry)
    {
        _verifications.Add(v => v.Country(expectedCountry));
        return Self;
    }

    IThenOrder IThenOrder.HasCountry(string expectedCountry) => HasCountry(expectedCountry);

    public TDerived HasUnitPrice(decimal expectedUnitPrice)
    {
        _verifications.Add(v => v.UnitPrice(expectedUnitPrice));
        return Self;
    }

    IThenOrder IThenOrder.HasUnitPrice(decimal expectedUnitPrice) => HasUnitPrice(expectedUnitPrice);

    public TDerived HasDiscountRateGreaterThanOrEqualToZero()
    {
        _verifications.Add(v => v.DiscountRateGreaterThanOrEqualToZero());
        return Self;
    }

    IThenOrder IThenOrder.HasDiscountRateGreaterThanOrEqualToZero() => HasDiscountRateGreaterThanOrEqualToZero();

    public TDerived HasDiscountAmountGreaterThanOrEqualToZero()
    {
        _verifications.Add(v => v.DiscountAmountGreaterThanOrEqualToZero());
        return Self;
    }

    IThenOrder IThenOrder.HasDiscountAmountGreaterThanOrEqualToZero() => HasDiscountAmountGreaterThanOrEqualToZero();

    public TDerived HasSubtotalPriceGreaterThanZero()
    {
        _verifications.Add(v => v.SubtotalPriceGreaterThanZero());
        return Self;
    }

    IThenOrder IThenOrder.HasSubtotalPriceGreaterThanZero() => HasSubtotalPriceGreaterThanZero();

    public TDerived HasTaxRateGreaterThanOrEqualToZero()
    {
        _verifications.Add(v => v.TaxRateGreaterThanOrEqualToZero());
        return Self;
    }

    IThenOrder IThenOrder.HasTaxRateGreaterThanOrEqualToZero() => HasTaxRateGreaterThanOrEqualToZero();

    public TDerived HasTaxAmountGreaterThanOrEqualToZero()
    {
        _verifications.Add(v => v.TaxAmountGreaterThanOrEqualToZero());
        return Self;
    }

    IThenOrder IThenOrder.HasTaxAmountGreaterThanOrEqualToZero() => HasTaxAmountGreaterThanOrEqualToZero();

    public TDerived HasTotalPriceGreaterThanZero()
    {
        _verifications.Add(v => v.TotalPriceGreaterThanZero());
        return Self;
    }

    IThenOrder IThenOrder.HasTotalPriceGreaterThanZero() => HasTotalPriceGreaterThanZero();

    public TaskAwaiter GetAwaiter() => Execute().GetAwaiter();

    private async Task Execute()
    {
        var result = await _thenClause.GetExecutionResult();
        var cachedVerification = await RunPrelude(result);

        var orderNumber = await _orderNumberFactory();

        ViewOrderVerification verification;
        if (cachedVerification != null)
        {
            verification = cachedVerification;
        }
        else
        {
            var shop = await _thenClause.App.Shop(_thenClause.Channel);
            var viewOrderResult = await shop.ViewOrder().OrderNumber(orderNumber).Execute();
            verification = viewOrderResult.ShouldSucceed();
        }

        foreach (var v in _verifications)
        {
            v(verification);
        }
    }
}
