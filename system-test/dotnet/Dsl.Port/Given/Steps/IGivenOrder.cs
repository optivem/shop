using Dsl.Port.Given.Steps.Base;
using Driver.Port.Dtos;
using Common.Domain;

namespace Dsl.Port.Given.Steps;

public interface IGivenOrder : IGivenStep
{
    IGivenOrder WithOrderNumber(string orderNumber);

    IGivenOrder WithSku(string? sku);

    IGivenOrder WithQuantity(string? quantity);

    IGivenOrder WithQuantity(int? quantity);

    IGivenOrder WithCountry(string? country);

    IGivenOrder WithCouponCode(string? couponCode);

    IGivenOrder WithStatus(OrderStatus status);
}
