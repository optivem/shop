using Dsl.Port.When.Steps;

namespace Dsl.Port.When;

public interface IWhenStage
{
    IGoToShop GoToShop();

    IPlaceOrder PlaceOrder();

    IViewOrder ViewOrder();

    IPublishCoupon PublishCoupon();

    IBrowseCoupons BrowseCoupons();
}
