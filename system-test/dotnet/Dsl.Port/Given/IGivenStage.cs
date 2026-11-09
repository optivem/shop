using Dsl.Port.Given.Steps;
using Dsl.Port.Then;
using Dsl.Port.When;

namespace Dsl.Port.Given;

public interface IGivenStage
{
    IGivenErp Erp();

    IGivenProduct Product();

    IGivenProducts Products();

    IGivenOrder Order();

    IGivenClock Clock();

    IGivenCountry Country();

    IGivenPromotion Promotion();

    IGivenCoupon Coupon();

    IGivenProfile Profile();

    /// <summary>Logs in as the default customer (customer1).</summary>
    IGivenStage LoggedInAsCustomer();

    /// <summary>Logs in as the customer known in this scenario by the given alias.</summary>
    IGivenStage LoggedInAsCustomer(string alias);

    IGivenStage LoggedInAsAdmin();

    IGivenStage NotLoggedIn();

    IWhenStage When();

    IThenStage Then();
}
