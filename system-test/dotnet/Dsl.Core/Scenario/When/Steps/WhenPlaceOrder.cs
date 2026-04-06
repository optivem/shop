using Dsl.Core.Scenario.When.Steps.Base;
using Dsl.Port;
using Dsl.Port.When.Steps;
using Dsl.Core.Shared;
using Common;
using Driver.Adapter;
using Driver.Port.Shop.Dtos;
using Dsl.Core.Shop.UseCases;
using Optivem.Testing;
using static Dsl.Core.Gherkin.GherkinDefaults;

namespace Dsl.Core.Scenario.When.Steps;

public class PlaceOrder : BaseWhen<PlaceOrderResponse, PlaceOrderVerification>, IPlaceOrder
{
    private string? _orderNumber;
    private string? _sku;
    private string? _quantity;
    private string? _country;
    private string? _couponCode;
    public PlaceOrder(UseCaseDsl app, ScenarioDsl scenario, Func<Task> ensureGiven) : base(app, scenario, ensureGiven)
    {
        WithOrderNumber(DefaultOrderNumber);
        WithSku(DefaultSku);
        WithQuantity(DefaultQuantity);
        WithCountry(DefaultCountry);
    }

    public PlaceOrder WithOrderNumber(string? orderNumber)
    {
        _orderNumber = orderNumber;
        return this;
    }

    IPlaceOrder IPlaceOrder.WithOrderNumber(string? orderNumber) => WithOrderNumber(orderNumber);

    public PlaceOrder WithSku(string? sku)
    {
        _sku = sku;
        return this;
    }

    IPlaceOrder IPlaceOrder.WithSku(string? sku) => WithSku(sku);

    public PlaceOrder WithQuantity(string? quantity)
    {
        _quantity = quantity;
        return this;
    }

    IPlaceOrder IPlaceOrder.WithQuantity(string? quantity) => WithQuantity(quantity);

    public PlaceOrder WithQuantity(int quantity)
    {
        return WithQuantity(Converter.FromInteger(quantity));
    }

    IPlaceOrder IPlaceOrder.WithQuantity(int quantity) => WithQuantity(quantity);

    public PlaceOrder WithCountry(string? country)
    {
        _country = country;
        return this;
    }

    IPlaceOrder IPlaceOrder.WithCountry(string? country) => WithCountry(country);

    public PlaceOrder WithCouponCode(string? couponCode)
    {
        _couponCode = couponCode;
        return this;
    }

    IPlaceOrder IPlaceOrder.WithCouponCode(string? couponCode) => WithCouponCode(couponCode);

    protected override async Task<ExecutionResult<PlaceOrderResponse, PlaceOrderVerification>> Execute(UseCaseDsl app)
    {
        var shop = await app.Shop(ChannelMode.Dynamic, Channel);
        var result = await shop.PlaceOrder()
            .OrderNumber(_orderNumber)
            .Sku(_sku)
            .Quantity(_quantity)
            .Country(_country)
            .CouponCode(_couponCode)
            .Execute();

        return new ExecutionResultBuilder<PlaceOrderResponse, PlaceOrderVerification>(result)
            .OrderNumber(_orderNumber)
            .Build();
    }
}



