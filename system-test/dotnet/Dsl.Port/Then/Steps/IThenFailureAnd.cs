namespace Dsl.Port.Then.Steps;

public interface IThenFailureAnd
{
    IThenOrder Order(string orderNumber);

    IThenOrder Order();

    IThenClock Clock();

    IThenProduct Product(string skuAlias);

    IThenCountry Country(string countryAlias);

    IThenCoupon Coupon(string couponCode);

    IThenCoupon Coupon();
}
