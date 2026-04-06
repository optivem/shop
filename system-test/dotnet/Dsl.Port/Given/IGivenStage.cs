using Dsl.Port.Given.Steps;
using Dsl.Port.Then;
using Dsl.Port.When;

namespace Dsl.Port.Given;

public interface IGivenStage
{
    IGivenProduct Product();

    IGivenOrder Order();

    IGivenClock Clock();

    IGivenPromotion Promotion();

    IGivenCoupon Coupon();

    IWhenStage When();

    IThenStage Then();
}
