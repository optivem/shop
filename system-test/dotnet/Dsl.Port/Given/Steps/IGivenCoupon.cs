using Dsl.Port.Given.Steps.Base;

namespace Dsl.Port.Given.Steps;

public interface IGivenCoupon : IGivenStep
{
    IGivenCoupon WithCode(string? code);

    IGivenCoupon WithDiscountRate(decimal discountRate);
}
