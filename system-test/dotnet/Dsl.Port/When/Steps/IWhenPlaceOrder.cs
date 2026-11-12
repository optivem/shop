using Dsl.Port.When.Steps.Base;

namespace Dsl.Port.When.Steps;

public interface IWhenPlaceOrder : IWhenStep
{
    IWhenPlaceOrder WithOrderNumber(string? orderNumber);

    IWhenPlaceOrder WithSku(string? sku);

    IWhenPlaceOrder WithQuantity(string? quantity);

    IWhenPlaceOrder WithQuantity(int quantity);

    IWhenPlaceOrder WithCountry(string? country);

    /// <summary>
    /// Leaves the country as Quick order prefilled it from the saved profile (UC-010 BR-5): the UI keeps the field as
    /// shown; the API gets no country.
    /// </summary>
    IWhenPlaceOrder WithPrefilledCountry();

    IWhenPlaceOrder WithCouponCode(string? couponCode);

    IWhenPlaceOrder WithCouponCode();

    IWhenPlaceOrder WithIdempotencyKey(string? idempotencyKey);

    /// <summary>Ships the order to the default address (Ana Novak, Main St 1, 10115 Berlin, default country) instead of a Quick-order country.</summary>
    IWhenPlaceOrder WithShippingAddress();

    /// <summary>Ships the order to this address; the country is an alias like <see cref="WithCountry"/>.</summary>
    IWhenPlaceOrder WithShippingAddress(string? firstName, string? lastName, string? street, string? postalCode, string? city, string? country);

    IWhenPlaceOrder WithShippingFirstName(string? firstName);

    IWhenPlaceOrder WithShippingLastName(string? lastName);

    IWhenPlaceOrder WithShippingStreet(string? street);

    IWhenPlaceOrder WithShippingPostalCode(string? postalCode);

    IWhenPlaceOrder WithShippingCity(string? city);

    IWhenPlaceOrder WithShippingCountry(string? country);
}
