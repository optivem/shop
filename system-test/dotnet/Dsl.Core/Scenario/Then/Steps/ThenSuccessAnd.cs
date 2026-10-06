using Dsl.Port.Then.Steps;
using Dsl.Core.Shared;

namespace Dsl.Core.Scenario.Then;

public class ThenSuccessAnd<TSuccessResponse, TSuccessVerification>
    : BaseThenAnd<TSuccessResponse, TSuccessVerification, ThenSuccessOrder<TSuccessResponse, TSuccessVerification>>, IThenSuccessAnd
    where TSuccessVerification : ResponseVerification<TSuccessResponse>
{
    internal ThenSuccessAnd(ThenStage<TSuccessResponse, TSuccessVerification> thenClause)
        : base(thenClause)
    {
    }

    protected override ThenSuccessOrder<TSuccessResponse, TSuccessVerification> CreateOrderAssertion(Func<Task<string>> orderNumberFactory)
    {
        return new ThenSuccessOrder<TSuccessResponse, TSuccessVerification>(_thenClause, orderNumberFactory);
    }

    IThenOrder IThenSuccessAnd.Order(string orderNumber) => Order(orderNumber);

    IThenOrder IThenSuccessAnd.Order() => Order();

    public ThenSuccessOrderHistory<TSuccessResponse, TSuccessVerification> OrderHistory()
    {
        return new ThenSuccessOrderHistory<TSuccessResponse, TSuccessVerification>(_thenClause);
    }

    IThenOrderHistory IThenSuccessAnd.OrderHistory() => OrderHistory();

    public async Task<IThenClock> Clock()
    {
        await _thenClause.GetExecutionResult();
        var verification = (await _thenClause.App.Clock().GetTime().Execute()).ShouldSucceed();
        return new Steps.ThenClock(_thenClause.App, verification);
    }

    async Task<IThenClock> IThenSuccessAnd.Clock() => await Clock();

    public async Task<IThenProduct> Product(string skuAlias)
    {
        await _thenClause.GetExecutionResult();
        var verification = (await _thenClause.App.Erp().GetProduct().Sku(skuAlias).Execute()).ShouldSucceed();
        return new Steps.ThenProduct(_thenClause.App, verification);
    }

    async Task<IThenProduct> IThenSuccessAnd.Product(string skuAlias) => await Product(skuAlias);

    public async Task<IThenCountry> Country(string countryAlias)
    {
        var verification = (await _thenClause.App.Tax().GetTaxRate().Country(countryAlias).Execute()).ShouldSucceed();
        return new Steps.ThenCountry(_thenClause.App, verification);
    }

    async Task<IThenCountry> IThenSuccessAnd.Country(string countryAlias) => await Country(countryAlias);

    public ThenSuccessCoupon<TSuccessResponse, TSuccessVerification> Coupon(string couponCode)
    {
        return new ThenSuccessCoupon<TSuccessResponse, TSuccessVerification>(
            _thenClause,
            () => Task.FromResult(couponCode));
    }

    IThenCoupon IThenSuccessAnd.Coupon(string couponCode) => Coupon(couponCode);

    public ThenSuccessCoupon<TSuccessResponse, TSuccessVerification> Coupon()
    {
        return new ThenSuccessCoupon<TSuccessResponse, TSuccessVerification>(
            _thenClause,
            async () =>
            {
                var result = await _thenClause.GetExecutionResult();
                return result.CouponCode ?? throw new InvalidOperationException("Cannot verify coupon: no coupon code available from the executed operation");
            });
    }

    IThenCoupon IThenSuccessAnd.Coupon() => Coupon();
}

