using Dsl.Port.Given.Steps;
using Dsl.Core.Scenario.Given;

namespace Dsl.Core.Gherkin.Given;

public class GivenCoupon : BaseGiven, IGivenCoupon
{
    private string? _code;
    private decimal _discountRate;

    public GivenCoupon(GivenStage givenClause)
        : base(givenClause)
    {
    }

    public GivenCoupon WithCode(string? code)
    {
        _code = code;
        return this;
    }

    IGivenCoupon IGivenCoupon.WithCode(string? code) => WithCode(code);

    public GivenCoupon WithDiscountRate(decimal discountRate)
    {
        _discountRate = discountRate;
        return this;
    }

    IGivenCoupon IGivenCoupon.WithDiscountRate(decimal discountRate) => WithDiscountRate(discountRate);

    internal override async Task Execute(UseCaseDsl app)
    {
        var shop = await app.ApiShop();
        (await shop.PublishCoupon()
            .Code(_code)
            .DiscountRate(_discountRate)
            .Execute())
            .ShouldSucceed();
    }
}
