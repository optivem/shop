using Common;
using Dsl.Core.Scenario.When.Steps.Base;
using Dsl.Port.When.Steps;
using Dsl.Core.Shared;
using Driver.Adapter;

namespace Dsl.Core.Scenario.When.Steps;

public class WhenPublishCoupon : BaseWhen<VoidValue, VoidVerification>, IPublishCoupon
{
    private string? _code;
    private decimal? _discountRate;

    public WhenPublishCoupon(UseCaseDsl app, ScenarioDsl scenario, Func<Task> ensureGiven)
        : base(app, scenario, ensureGiven)
    {
    }

    public WhenPublishCoupon WithCode(string? code)
    {
        _code = code;
        return this;
    }

    IPublishCoupon IPublishCoupon.WithCode(string? code) => WithCode(code);

    public WhenPublishCoupon WithDiscountRate(decimal discountRate)
    {
        _discountRate = discountRate;
        return this;
    }

    IPublishCoupon IPublishCoupon.WithDiscountRate(decimal discountRate) => WithDiscountRate(discountRate);

    protected override async Task<ExecutionResult<VoidValue, VoidVerification>> Execute(UseCaseDsl app)
    {
        var shop = await app.ApiShop();
        var result = await shop.PublishCoupon()
            .Code(_code)
            .DiscountRate(_discountRate)
            .Execute();

        return new ExecutionResultBuilder<VoidValue, VoidVerification>(result)
            .Build();
    }
}
