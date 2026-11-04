using Dsl.Port.When.Steps.Base;

namespace Dsl.Port.When.Steps;

public interface IWhenPlaceOrder : IWhenStep
{
    IWhenPlaceOrder WithOrderNumber(string? orderNumber);

    IWhenPlaceOrder WithSku(string? sku);

    IWhenPlaceOrder WithQuantity(string? quantity);

    IWhenPlaceOrder WithQuantity(int quantity);

    IWhenPlaceOrder WithCountry(string? country);

    IWhenPlaceOrder WithCouponCode(string? couponCode);

    IWhenPlaceOrder WithCouponCode();

    IWhenPlaceOrder WithIdempotencyKey(string? idempotencyKey);
}
