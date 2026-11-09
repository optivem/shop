using Dsl.Core.Scenario.When.Steps.Base;
using Dsl.Port.When.Steps;
using Dsl.Core.Shared;
using Common;
using Driver.Adapter;
using Driver.Port.Dtos;
using Dsl.Core.UseCase.UseCases;
using Optivem.Testing;
using Dsl.Port;
using static Dsl.Core.Scenario.ScenarioDefaults;

namespace Dsl.Core.Scenario.When.Steps;

public class PlaceOrder : BaseWhen<PlaceOrderResponse, PlaceOrderVerification>, IWhenPlaceOrder
{
    private string? _orderNumber;
    private string? _sku;
    private string? _quantity;
    private string? _country;
    private string? _couponCode;
    private string? _idempotencyKey;
    private bool _shipsToAddress;
    private string? _shippingFirstName;
    private string? _shippingLastName;
    private string? _shippingStreet;
    private string? _shippingPostalCode;
    private string? _shippingCity;
    private string? _shippingCountry;

    public PlaceOrder(UseCaseDsl app, ScenarioDsl scenario, Func<Task> ensureGiven) : base(app, scenario, ensureGiven)
    {
        WithOrderNumber(DefaultOrderNumber);
        WithSku(DefaultSku);
        WithQuantity(DefaultQuantity);
        WithCountry(DefaultCountry);
        WithCouponCode(Empty);
    }

    public PlaceOrder WithOrderNumber(string? orderNumber)
    {
        _orderNumber = orderNumber;
        return this;
    }

    IWhenPlaceOrder IWhenPlaceOrder.WithOrderNumber(string? orderNumber) => WithOrderNumber(orderNumber);

    public PlaceOrder WithSku(string? sku)
    {
        _sku = sku;
        return this;
    }

    IWhenPlaceOrder IWhenPlaceOrder.WithSku(string? sku) => WithSku(sku);

    public PlaceOrder WithQuantity(string? quantity)
    {
        _quantity = quantity;
        return this;
    }

    IWhenPlaceOrder IWhenPlaceOrder.WithQuantity(string? quantity) => WithQuantity(quantity);

    public PlaceOrder WithQuantity(int quantity)
    {
        return WithQuantity(Converter.FromInteger(quantity));
    }

    IWhenPlaceOrder IWhenPlaceOrder.WithQuantity(int quantity) => WithQuantity(quantity);

    public PlaceOrder WithCountry(string? country)
    {
        _country = country;
        return this;
    }

    IWhenPlaceOrder IWhenPlaceOrder.WithCountry(string? country) => WithCountry(country);

    public PlaceOrder WithCouponCode(string? couponCode)
    {
        _couponCode = couponCode;
        return this;
    }

    IWhenPlaceOrder IWhenPlaceOrder.WithCouponCode(string? couponCode) => WithCouponCode(couponCode);

    public PlaceOrder WithCouponCode()
    {
        return WithCouponCode(DefaultCouponCode);
    }

    IWhenPlaceOrder IWhenPlaceOrder.WithCouponCode() => WithCouponCode();

    public PlaceOrder WithIdempotencyKey(string? idempotencyKey)
    {
        _idempotencyKey = idempotencyKey;
        return this;
    }

    IWhenPlaceOrder IWhenPlaceOrder.WithIdempotencyKey(string? idempotencyKey) => WithIdempotencyKey(idempotencyKey);

    public PlaceOrder WithShippingAddress()
    {
        ShipToAddress();
        return this;
    }

    IWhenPlaceOrder IWhenPlaceOrder.WithShippingAddress() => WithShippingAddress();

    public PlaceOrder WithShippingAddress(string? firstName, string? lastName, string? street, string? postalCode, string? city, string? country)
    {
        ShipToAddress();
        _shippingFirstName = firstName;
        _shippingLastName = lastName;
        _shippingStreet = street;
        _shippingPostalCode = postalCode;
        _shippingCity = city;
        _shippingCountry = country;
        return this;
    }

    IWhenPlaceOrder IWhenPlaceOrder.WithShippingAddress(string? firstName, string? lastName, string? street, string? postalCode, string? city, string? country)
        => WithShippingAddress(firstName, lastName, street, postalCode, city, country);

    public PlaceOrder WithShippingFirstName(string? firstName)
    {
        ShipToAddress();
        _shippingFirstName = firstName;
        return this;
    }

    IWhenPlaceOrder IWhenPlaceOrder.WithShippingFirstName(string? firstName) => WithShippingFirstName(firstName);

    public PlaceOrder WithShippingLastName(string? lastName)
    {
        ShipToAddress();
        _shippingLastName = lastName;
        return this;
    }

    IWhenPlaceOrder IWhenPlaceOrder.WithShippingLastName(string? lastName) => WithShippingLastName(lastName);

    public PlaceOrder WithShippingStreet(string? street)
    {
        ShipToAddress();
        _shippingStreet = street;
        return this;
    }

    IWhenPlaceOrder IWhenPlaceOrder.WithShippingStreet(string? street) => WithShippingStreet(street);

    public PlaceOrder WithShippingPostalCode(string? postalCode)
    {
        ShipToAddress();
        _shippingPostalCode = postalCode;
        return this;
    }

    IWhenPlaceOrder IWhenPlaceOrder.WithShippingPostalCode(string? postalCode) => WithShippingPostalCode(postalCode);

    public PlaceOrder WithShippingCity(string? city)
    {
        ShipToAddress();
        _shippingCity = city;
        return this;
    }

    IWhenPlaceOrder IWhenPlaceOrder.WithShippingCity(string? city) => WithShippingCity(city);

    public PlaceOrder WithShippingCountry(string? country)
    {
        ShipToAddress();
        _shippingCountry = country;
        return this;
    }

    IWhenPlaceOrder IWhenPlaceOrder.WithShippingCountry(string? country) => WithShippingCountry(country);

    /// <summary>Switches to address mode (once), starting from the default address; no top-level country is then sent.</summary>
    private void ShipToAddress()
    {
        if (_shipsToAddress)
        {
            return;
        }

        _shipsToAddress = true;
        _shippingFirstName = DefaultFirstName;
        _shippingLastName = DefaultLastName;
        _shippingStreet = DefaultStreet;
        _shippingPostalCode = DefaultPostalCode;
        _shippingCity = DefaultCity;
        _shippingCountry = DefaultCountry;
    }

    protected override async Task<ExecutionResult<PlaceOrderResponse, PlaceOrderVerification>> Execute(UseCaseDsl app)
    {
        var shop = await app.MyShop(ChannelMode.Dynamic, Channel);
        var placeOrder = shop.PlaceOrder()
            .OrderNumber(_orderNumber)
            .Sku(_sku)
            .Quantity(_quantity)
            .Country(_shipsToAddress ? null : _country)
            .CouponCode(_couponCode)
            .IdempotencyKey(_idempotencyKey);
        if (_shipsToAddress)
        {
            placeOrder.ShippingAddress(_shippingFirstName, _shippingLastName, _shippingStreet,
                _shippingPostalCode, _shippingCity, _shippingCountry);
        }

        var result = await placeOrder.Execute();

        return new ExecutionResultBuilder<PlaceOrderResponse, PlaceOrderVerification>(result)
            .OrderNumber(_orderNumber)
            .CouponCode(_couponCode)
            .Build();
    }
}
