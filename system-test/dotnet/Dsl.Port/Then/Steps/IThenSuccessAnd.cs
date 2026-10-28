namespace Dsl.Port.Then.Steps;

public interface IThenSuccessAnd
{
    IThenOrder Order(string orderNumber);

    IThenOrder Order();

    IThenOrderHistory OrderHistory();

    IThenClock Clock();

    IThenProduct Product(string skuAlias);

    IThenCountry Country(string countryAlias);

    IThenCoupon Coupon(string couponCode);

    IThenCoupon Coupon();
}
