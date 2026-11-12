using System.Runtime.CompilerServices;
using Dsl.Port.Then.Steps;
using Dsl.Core.Shared;
using Dsl.Core.UseCase.UseCases;

namespace Dsl.Core.Scenario.Then;

public class ThenSuccessCouponList<TSuccessResponse, TSuccessVerification> : IThenCouponList
    where TSuccessVerification : ResponseVerification<TSuccessResponse>
{
    private readonly ThenStage<TSuccessResponse, TSuccessVerification> _thenClause;
    private readonly List<Action<BrowseCouponsVerification>> _verifications = [];

    internal ThenSuccessCouponList(ThenStage<TSuccessResponse, TSuccessVerification> thenClause)
    {
        _thenClause = thenClause;
    }

    public ThenSuccessCouponList<TSuccessResponse, TSuccessVerification> StartsWithCoupons(params string[] couponCodes)
    {
        _verifications.Add(v => v.StartsWithCoupons(couponCodes.ToList()));
        return this;
    }

    IThenCouponList IThenCouponList.StartsWithCoupons(params string[] couponCodes) => StartsWithCoupons(couponCodes);

    public TaskAwaiter GetAwaiter() => Execute().GetAwaiter();

    private async Task Execute()
    {
        var result = await _thenClause.GetExecutionResult();
        if (result.Result.ShouldSucceed() is not BrowseCouponsVerification verification)
            throw new InvalidOperationException("Cannot verify the coupon list: the executed operation did not browse the coupons");

        foreach (var v in _verifications)
        {
            v(verification);
        }
    }
}
