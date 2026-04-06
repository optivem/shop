using System.Runtime.CompilerServices;
using Dsl.Port.Then.Steps;
using Dsl.Core.Shared;
using Driver.Port.Shop.Dtos;
using Dsl.Core.Shop.UseCases;

namespace Dsl.Core.Scenario.Then;

public class ThenBrowseCoupons : IThenBrowseCoupons
{
    private readonly ThenStage<BrowseCouponsResponse, BrowseCouponsVerification> _thenClause;
    private readonly List<Action<BrowseCouponsVerification>> _verifications = [];

    internal ThenBrowseCoupons(ThenStage<BrowseCouponsResponse, BrowseCouponsVerification> thenClause)
    {
        _thenClause = thenClause;
    }

    public ThenBrowseCoupons ContainsCouponWithCode(string expectedCode)
    {
        _verifications.Add(v => v.ContainsCouponWithCode(expectedCode));
        return this;
    }

    IThenBrowseCoupons IThenBrowseCoupons.ContainsCouponWithCode(string expectedCode) => ContainsCouponWithCode(expectedCode);

    public ThenBrowseCoupons CouponCount(int expectedCount)
    {
        _verifications.Add(v => v.CouponCount(expectedCount));
        return this;
    }

    IThenBrowseCoupons IThenBrowseCoupons.CouponCount(int expectedCount) => CouponCount(expectedCount);

    public TaskAwaiter GetAwaiter() => Execute().GetAwaiter();

    private async Task Execute()
    {
        var result = await _thenClause.GetExecutionResult();
        var verification = result.Result.ShouldSucceed();

        foreach (var v in _verifications)
        {
            v(verification);
        }
    }
}
