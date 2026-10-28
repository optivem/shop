using Dsl.Port.Then.Steps;
using Dsl.Core.Shared;
using Dsl.Core.UseCase.UseCases.Base;

namespace Dsl.Core.Scenario.Then;

public class ThenFailureAnd<TSuccessResponse, TSuccessVerification>
    : BaseThenAnd<TSuccessResponse, TSuccessVerification, ThenFailureOrder<TSuccessResponse, TSuccessVerification>>, IThenFailureAnd
    where TSuccessVerification : ResponseVerification<TSuccessResponse>
{
    private readonly List<Action<SystemErrorFailureVerification>> _failureAssertions;

    internal ThenFailureAnd(
        ThenStage<TSuccessResponse, TSuccessVerification> thenClause,
        List<Action<SystemErrorFailureVerification>> failureAssertions)
        : base(thenClause)
    {
        _failureAssertions = failureAssertions;
    }

    protected override ThenFailureOrder<TSuccessResponse, TSuccessVerification> CreateOrderAssertion(Func<Task<string>> orderNumberFactory)
    {
        return new ThenFailureOrder<TSuccessResponse, TSuccessVerification>(_thenClause, _failureAssertions, orderNumberFactory);
    }

    IThenOrder IThenFailureAnd.Order(string orderNumber) => Order(orderNumber);

    IThenOrder IThenFailureAnd.Order() => Order();

    public IThenClock Clock()
    {
        return new Steps.ThenClock(_thenClause.App, VerifyFailure);
    }

    public IThenProduct Product(string skuAlias)
    {
        return new Steps.ThenProduct(_thenClause.App, skuAlias, VerifyFailure);
    }

    public IThenCountry Country(string countryAlias)
    {
        return new Steps.ThenCountry(_thenClause.App, countryAlias, VerifyFailure);
    }

    private async Task VerifyFailure()
    {
        var result = await _thenClause.GetExecutionResult();
        if (result.Result == null)
            throw new InvalidOperationException("Cannot verify failure: no operation was executed");
        var failureVerification = result.Result.ShouldFail();
        foreach (var assertion in _failureAssertions)
        {
            assertion(failureVerification);
        }
    }

    public ThenFailureCoupon<TSuccessResponse, TSuccessVerification> Coupon(string couponCode)
    {
        return new ThenFailureCoupon<TSuccessResponse, TSuccessVerification>(
            _thenClause, _failureAssertions, () => Task.FromResult(couponCode));
    }

    public ThenFailureCoupon<TSuccessResponse, TSuccessVerification> Coupon()
    {
        return new ThenFailureCoupon<TSuccessResponse, TSuccessVerification>(
            _thenClause, _failureAssertions,
            async () =>
            {
                var result = await _thenClause.GetExecutionResult();
                return result.CouponCode ?? throw new InvalidOperationException("Cannot verify coupon: no coupon code available from the executed operation");
            });
    }

    IThenCoupon IThenFailureAnd.Coupon(string couponCode) => Coupon(couponCode);

    IThenCoupon IThenFailureAnd.Coupon() => Coupon();
}


