using Dsl.Core.Scenario;
using Dsl.Core.Scenario.When.Steps.Base;
using Dsl.Port.When.Steps;
using Dsl.Core.Shared;
using Common;
using Driver.Port.Dtos;
using Dsl.Core.UseCase.UseCases;
using Optivem.Testing;
using Dsl.Port;
using static Dsl.Core.Scenario.ScenarioDefaults;

namespace Dsl.Core.Scenario.When.Steps;

public class WhenPublishCoupon : BaseWhen<VoidValue, VoidVerification>, IWhenPublishCoupon
{
    private string? _couponCode;
    private string? _discountRate;
    private string? _validFrom;
    private string? _validTo;
    private string? _usageLimit;

    public WhenPublishCoupon(UseCaseDsl app, ScenarioDsl scenario, Func<Task> ensureGiven) : base(app, scenario, ensureGiven)
    {
        WithCouponCode(DefaultCouponCode);
        WithDiscountRate(DefaultDiscountRate);
    }

    public WhenPublishCoupon WithCouponCode(string? couponCode)
    {
        _couponCode = couponCode;
        return this;
    }

    IWhenPublishCoupon IWhenPublishCoupon.WithCouponCode(string? couponCode) => WithCouponCode(couponCode);

    public WhenPublishCoupon WithDiscountRate(string? discountRate)
    {
        _discountRate = discountRate;
        return this;
    }

    IWhenPublishCoupon IWhenPublishCoupon.WithDiscountRate(string? discountRate) => WithDiscountRate(discountRate);

    public WhenPublishCoupon WithDiscountRate(decimal discountRate)
    {
        _discountRate = Converter.FromDecimal(discountRate);
        return this;
    }

    IWhenPublishCoupon IWhenPublishCoupon.WithDiscountRate(decimal discountRate) => WithDiscountRate(discountRate);

    public WhenPublishCoupon WithValidFrom(string? validFrom)
    {
        _validFrom = validFrom;
        return this;
    }

    IWhenPublishCoupon IWhenPublishCoupon.WithValidFrom(string? validFrom) => WithValidFrom(validFrom);

    public WhenPublishCoupon WithValidTo(string? validTo)
    {
        _validTo = validTo;
        return this;
    }

    IWhenPublishCoupon IWhenPublishCoupon.WithValidTo(string? validTo) => WithValidTo(validTo);

    public WhenPublishCoupon WithUsageLimit(string? usageLimit)
    {
        _usageLimit = usageLimit;
        return this;
    }

    IWhenPublishCoupon IWhenPublishCoupon.WithUsageLimit(string? usageLimit) => WithUsageLimit(usageLimit);

    public WhenPublishCoupon WithUsageLimit(int usageLimit)
    {
        _usageLimit = Converter.FromInteger(usageLimit);
        return this;
    }

    IWhenPublishCoupon IWhenPublishCoupon.WithUsageLimit(int usageLimit) => WithUsageLimit(usageLimit);

    protected override async Task<ExecutionResult<VoidValue, VoidVerification>> Execute(UseCaseDsl app)
    {
        var shop = await app.MyShop(ChannelMode.Dynamic, Channel);
        var result = await shop.PublishCoupon()
            .CouponCode(_couponCode)
            .DiscountRate(_discountRate)
            .ValidFrom(_validFrom)
            .ValidTo(_validTo)
            .UsageLimit(_usageLimit)
            .Execute();

        return new ExecutionResultBuilder<VoidValue, VoidVerification>(result)
            .CouponCode(_couponCode)
            .Build();
    }
}
