using Dsl.Port.When.Steps;

namespace Dsl.Port.When;

public interface IWhenStage
{
    IWhenPlaceOrder PlaceOrder();

    IWhenCancelOrder CancelOrder();

    IWhenDeliverOrder DeliverOrder();

    IWhenViewOrder ViewOrder();

    IWhenBrowseOrderHistory BrowseOrderHistory();

    IWhenPublishCoupon PublishCoupon();

    IWhenBrowseCoupons BrowseCoupons();
}
