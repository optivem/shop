namespace Dsl.Port.Then.Steps;

public interface IThenSuccessAnd
{
    IThenOrder Order(string orderNumber);

    IThenOrder Order();

    /// <summary>The order the checkout placed for this product (an alias), as the system shows it now.</summary>
    IThenOrder OrderFor(string skuAlias);

    IThenOrderHistory OrderHistory();

    IThenClock Clock();

    IThenProduct Product(string skuAlias);

    IThenCountry Country(string countryAlias);

    IThenCoupon Coupon(string couponCode);

    IThenCoupon Coupon();

    /// <summary>The acting user's profile, fetched afresh.</summary>
    IThenProfile Profile();
}
