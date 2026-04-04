using Driver.Port.Shop;
using Dsl.Core.Shop.UseCases.Base;
using Driver.Port.Shop.Dtos;
using Driver.Port.Shop.Dtos.Error;
using Dsl.Core.Shared;

namespace Dsl.Core.Shop.UseCases;

public class PlaceOrder : BaseShopCommand<PlaceOrderResponse, PlaceOrderVerification>
{
    private string? _orderNumberResultAlias;
    private string? _skuParamAlias;
    private string? _quantity;
    private string? _country;

    public PlaceOrder(IShopDriver driver, UseCaseContext context)
        : base(driver, context)
    {
    }

    public PlaceOrder OrderNumber(string? orderNumberResultAlias)
    {
        _orderNumberResultAlias = orderNumberResultAlias;
        return this;
    }

    public PlaceOrder Sku(string? skuParamAlias)
    {
        _skuParamAlias = skuParamAlias;
        return this;
    }

    public PlaceOrder Quantity(string? quantity)
    {
        _quantity = quantity;
        return this;
    }

    public PlaceOrder Quantity(int quantity)
    {
        return Quantity(quantity.ToString());
    }

    public PlaceOrder Country(string? country)
    {
        _country = country;
        return this;
    }

    public override async Task<ShopUseCaseResult<PlaceOrderResponse, PlaceOrderVerification>> Execute()
    {
        var sku = _context.GetParamValue(_skuParamAlias);

        var request = new PlaceOrderRequest
        {
            Sku = sku,
            Quantity = _quantity,
            Country = _country,
        };

        var result = await _driver.PlaceOrderAsync(request);

        if (_orderNumberResultAlias != null)
        {
            if (result.IsSuccess)
            {
                var orderNumber = result.Value.OrderNumber;
                _context.SetResultEntry(_orderNumberResultAlias, orderNumber);
            }
            else
            {
                _context.SetResultEntryFailed(_orderNumberResultAlias, result.Error.ToString());
            }
        }

        return new ShopUseCaseResult<PlaceOrderResponse, PlaceOrderVerification>(
            result,
            _context,
            (response, ctx) => new PlaceOrderVerification(response, ctx));
    }
}



