namespace Dsl.Port.Then.Steps;

public interface IThenSuccessAnd
{
    IThenOrder Order(string orderNumber);

    IThenOrder Order();

    Task<IThenClock> Clock();

    Task<IThenProduct> Product(string skuAlias);

    IThenBrowseCoupons Coupons();
}
