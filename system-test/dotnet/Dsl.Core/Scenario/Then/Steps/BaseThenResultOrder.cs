using System.Runtime.CompilerServices;
using Dsl.Port.Then.Steps;
using Dsl.Core.Shared;
using Dsl.Core.Scenario;
using Driver.Adapter;
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

    protected abstract Task RunPrelude(ExecutionResult<TSuccessResponse, TSuccessVerification> result);

    protected TDerived Self => (TDerived)this;

    public TDerived HasStatus(OrderStatus expectedStatus)
    {
        _verifications.Add(v => v.Status(expectedStatus));
        return Self;
    }

    IThenOrder IThenOrder.HasStatus(OrderStatus expectedStatus) => HasStatus(expectedStatus);

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

    public TDerived HasUnitPrice(decimal expectedUnitPrice)
    {
        _verifications.Add(v => v.UnitPrice(expectedUnitPrice));
        return Self;
    }

    IThenOrder IThenOrder.HasUnitPrice(decimal expectedUnitPrice) => HasUnitPrice(expectedUnitPrice);

    public TDerived HasTotalPriceGreaterThanZero()
    {
        _verifications.Add(v => v.TotalPriceGreaterThanZero());
        return Self;
    }

    IThenOrder IThenOrder.HasTotalPriceGreaterThanZero() => HasTotalPriceGreaterThanZero();

    public TDerived HasSubtotalPrice(decimal expectedSubtotalPrice)
    {
        _verifications.Add(v => v.SubtotalPrice(expectedSubtotalPrice));
        return Self;
    }

    IThenOrder IThenOrder.HasSubtotalPrice(decimal expectedSubtotalPrice) => HasSubtotalPrice(expectedSubtotalPrice);

    public TDerived HasTaxRate(decimal expectedTaxRate)
    {
        _verifications.Add(v => v.TaxRate(expectedTaxRate));
        return Self;
    }

    IThenOrder IThenOrder.HasTaxRate(decimal expectedTaxRate) => HasTaxRate(expectedTaxRate);

    public TDerived HasDiscountRate(decimal expectedDiscountRate)
    {
        _verifications.Add(v => v.DiscountRate(expectedDiscountRate));
        return Self;
    }

    IThenOrder IThenOrder.HasDiscountRate(decimal expectedDiscountRate) => HasDiscountRate(expectedDiscountRate);

    public TDerived HasAppliedCouponCode(string expectedCouponCode)
    {
        _verifications.Add(v => v.AppliedCouponCode(expectedCouponCode));
        return Self;
    }

    IThenOrder IThenOrder.HasAppliedCouponCode(string expectedCouponCode) => HasAppliedCouponCode(expectedCouponCode);

    public TaskAwaiter GetAwaiter() => Execute().GetAwaiter();

    private async Task Execute()
    {
        var result = await _thenClause.GetExecutionResult();
        await RunPrelude(result);

        var orderNumber = await _orderNumberFactory();
        var shop = await _thenClause.App.Shop(_thenClause.Channel);
        var viewOrderResult = await shop.ViewOrder().OrderNumber(orderNumber).Execute();
        var verification = viewOrderResult.ShouldSucceed();

        foreach (var v in _verifications)
        {
            v(verification);
        }
    }
}



