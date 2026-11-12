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

    IGivenOrder WithIdempotencyKey(string? idempotencyKey);

    /// <summary>
    /// The order is placed at this time (ISO-8601): the clock is set to it just before the order is placed. It mutates
    /// the shared clock, so the scenario must run isolated.
    /// </summary>
    IGivenOrder PlacedAt(string orderTimestamp);

    /// <summary>The order is placed by the default customer (customer1).</summary>
    IGivenOrder PlacedByCustomer();

    /// <summary>The order is placed by the customer known in this scenario by the given alias.</summary>
    IGivenOrder PlacedByCustomer(string alias);

    IGivenOrder WithStatus(OrderStatus status);
}
