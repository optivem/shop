namespace Dsl.Port.Then.Steps;

public interface IThenSuccessAnd
{
    IThenOrder Order(string orderNumber);

    IThenOrder Order();

    IThenOrderHistory OrderHistory();

    Task<IThenClock> Clock();

    Task<IThenProduct> Product(string skuAlias);

    Task<IThenCountry> Country(string countryAlias);

    IThenCoupon Coupon(string couponCode);

    IThenCoupon Coupon();
}
