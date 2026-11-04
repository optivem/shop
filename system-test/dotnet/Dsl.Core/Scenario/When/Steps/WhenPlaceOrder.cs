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

    protected override async Task<ExecutionResult<PlaceOrderResponse, PlaceOrderVerification>> Execute(UseCaseDsl app)
    {
        var shop = await app.MyShop(ChannelMode.Dynamic, Channel);
        var result = await shop.PlaceOrder()
            .OrderNumber(_orderNumber)
            .Sku(_sku)
            .Quantity(_quantity)
            .Country(_country)
            .CouponCode(_couponCode)
            .IdempotencyKey(_idempotencyKey)
            .Execute();

        return new ExecutionResultBuilder<PlaceOrderResponse, PlaceOrderVerification>(result)
            .OrderNumber(_orderNumber)
            .CouponCode(_couponCode)
            .Build();
    }
}
