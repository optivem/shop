using Driver.Port;
using Dsl.Core.UseCase.UseCases.Base;
using Driver.Port.Dtos;
using Driver.Port.Dtos.Error;
using Dsl.Core.Shared;

namespace Dsl.Core.UseCase.UseCases;

public class PlaceOrder : BaseMyShopUseCase<PlaceOrderResponse, PlaceOrderVerification>
{
    private string? _orderNumberResultAlias;
    private string? _skuParamAlias;
    private string? _quantity;
    private string? _country;
    private string? _couponCode;
    private string? _idempotencyKeyAlias;
    private Driver.Port.Dtos.ShippingAddress? _shippingAddress;

    public PlaceOrder(IMyShopDriver driver, UseCaseContext context)
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

    public PlaceOrder CouponCode(string? couponCode)
    {
        _couponCode = couponCode;
        return this;
    }

    /// <summary>Ships the order to this address; the country is an alias like <see cref="Country"/>. Null fields stay null.</summary>
    public PlaceOrder ShippingAddress(string? firstName, string? lastName, string? street, string? postalCode, string? city, string? countryAlias)
    {
        _shippingAddress = new Driver.Port.Dtos.ShippingAddress
        {
            FirstName = firstName,
            LastName = lastName,
            Street = street,
            PostalCode = postalCode,
            City = city,
            Country = countryAlias,
        };
        return this;
    }

    public PlaceOrder IdempotencyKey(string? idempotencyKeyAlias)
    {
        _idempotencyKeyAlias = idempotencyKeyAlias;
        return this;
    }

    public override async Task<MyShopUseCaseResult<PlaceOrderResponse, PlaceOrderVerification>> Execute()
    {
        var sku = _context.GetParamValue(_skuParamAlias);
        var country = _context.GetParamValueOrLiteral(_country);
        var couponCode = _context.GetParamValue(_couponCode);
        // Keys are the shop's own data, so the alias always maps to a scenario-unique value: tests share a
        // database, and two scenarios reusing a literal key would replay each other's orders.
        var idempotencyKey = _context.GetParamValue(_idempotencyKeyAlias);

        var request = new PlaceOrderRequest
        {
            Sku = sku,
            Quantity = _quantity,
            Country = country,
            CouponCode = couponCode,
            IdempotencyKey = idempotencyKey,
            ShippingAddress = _shippingAddress == null ? null : new Driver.Port.Dtos.ShippingAddress
            {
                FirstName = _shippingAddress.FirstName,
                LastName = _shippingAddress.LastName,
                Street = _shippingAddress.Street,
                PostalCode = _shippingAddress.PostalCode,
                City = _shippingAddress.City,
                Country = _context.GetParamValueOrLiteral(_shippingAddress.Country),
            },
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

        return new MyShopUseCaseResult<PlaceOrderResponse, PlaceOrderVerification>(
            result,
            _context,
            (response, ctx) => new PlaceOrderVerification(response, ctx));
    }
}



