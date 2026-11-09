using System.Runtime.CompilerServices;
using Dsl.Port.Then.Steps;
using Dsl.Core.Shared;
using Dsl.Core.Scenario;
using Driver.Adapter;
using Dsl.Core.UseCase.UseCases;

namespace Dsl.Core.Scenario.Then;

public abstract class BaseThenResultCoupon<TSuccessResponse, TSuccessVerification, TDerived>
    : IThenCoupon
    where TSuccessVerification : ResponseVerification<TSuccessResponse>
    where TDerived : BaseThenResultCoupon<TSuccessResponse, TSuccessVerification, TDerived>
{
    protected readonly ThenStage<TSuccessResponse, TSuccessVerification> _thenClause;
    protected readonly Func<Task<string>> _couponCodeFactory;
    protected readonly List<Action<BrowseCouponsVerification, string>> _verifications = [];

    protected BaseThenResultCoupon(
        ThenStage<TSuccessResponse, TSuccessVerification> thenClause,
        Func<Task<string>> couponCodeFactory)
    {
        _thenClause = thenClause;
        _couponCodeFactory = couponCodeFactory;
    }

    protected abstract Task RunPrelude(ExecutionResult<TSuccessResponse, TSuccessVerification> result);

    protected TDerived Self => (TDerived)this;

    public TDerived HasDiscountRate(decimal discountRate)
    {
        _verifications.Add((v, code) => v.CouponHasDiscountRate(code, discountRate));
        return Self;
    }

    IThenCoupon IThenCoupon.HasDiscountRate(decimal discountRate) => HasDiscountRate(discountRate);

    public TDerived IsValidFrom(string validFrom)
    {
        _verifications.Add((v, code) => v.CouponHasValidFrom(code, validFrom));
        return Self;
    }

    IThenCoupon IThenCoupon.IsValidFrom(string validFrom) => IsValidFrom(validFrom);

    public TDerived IsValidTo(string validTo)
    {
        _verifications.Add((v, code) => v.CouponHasValidTo(code, validTo));
        return Self;
    }

    IThenCoupon IThenCoupon.IsValidTo(string validTo) => IsValidTo(validTo);

    public TDerived HasUsageLimit(int usageLimit)
    {
        _verifications.Add((v, code) => v.CouponHasUsageLimit(code, usageLimit));
        return Self;
    }

    IThenCoupon IThenCoupon.HasUsageLimit(int usageLimit) => HasUsageLimit(usageLimit);

    public TDerived IsValidImmediately()
    {
        _verifications.Add((v, code) => v.CouponHasNoValidFrom(code));
        return Self;
    }

    IThenCoupon IThenCoupon.IsValidImmediately() => IsValidImmediately();

    public TDerived NeverExpires()
    {
        _verifications.Add((v, code) => v.CouponHasNoValidTo(code));
        return Self;
    }

    IThenCoupon IThenCoupon.NeverExpires() => NeverExpires();

    public TDerived HasUnlimitedUsage()
    {
        _verifications.Add((v, code) => v.CouponHasNoUsageLimit(code));
        return Self;
    }

    IThenCoupon IThenCoupon.HasUnlimitedUsage() => HasUnlimitedUsage();

    public TDerived HasUsedCount(int expectedUsedCount)
    {
        _verifications.Add((v, code) => v.CouponHasUsedCount(code, expectedUsedCount));
        return Self;
    }

    IThenCoupon IThenCoupon.HasUsedCount(int expectedUsedCount) => HasUsedCount(expectedUsedCount);

    public TaskAwaiter GetAwaiter() => Execute().GetAwaiter();

    private async Task Execute()
    {
        var result = await _thenClause.GetExecutionResult();
        await RunPrelude(result);

        var couponCode = await _couponCodeFactory();
        var shop = await _thenClause.App.MyShop(_thenClause.Channel!);
        var browseResult = await shop.BrowseCoupons().Execute();
        var verification = browseResult.ShouldSucceed();
        verification.HasCouponWithCode(couponCode);

        foreach (var v in _verifications)
        {
            v(verification, couponCode);
        }
    }
}
