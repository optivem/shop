using Dsl.Port.When.Steps.Base;

namespace Dsl.Port.When.Steps;

public interface IPlaceOrder : IWhenStep
{
    IPlaceOrder WithOrderNumber(string? orderNumber);

    IPlaceOrder WithSku(string? sku);

    IPlaceOrder WithQuantity(string? quantity);

    IPlaceOrder WithQuantity(int quantity);

    IPlaceOrder WithCountry(string? country);

    IPlaceOrder WithCouponCode(string? couponCode);
}
