using Dsl.Port.When.Steps.Base;

namespace Dsl.Port.When.Steps;

public interface IPublishCoupon : IWhenStep
{
    IPublishCoupon WithCode(string? code);

    IPublishCoupon WithDiscountRate(decimal discountRate);
}
