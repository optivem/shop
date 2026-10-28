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

    public IThenClock Clock()
    {
        return new Steps.ThenClock(_thenClause.App, VerifySuccess);
    }

    public IThenProduct Product(string skuAlias)
    {
        return new Steps.ThenProduct(_thenClause.App, skuAlias, VerifySuccess);
    }

    public IThenCountry Country(string countryAlias)
    {
        return new Steps.ThenCountry(_thenClause.App, countryAlias, VerifySuccess);
    }

    private async Task VerifySuccess()
    {
        var result = await _thenClause.GetExecutionResult();
        _ = result.Result.ShouldSucceed();
    }

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

