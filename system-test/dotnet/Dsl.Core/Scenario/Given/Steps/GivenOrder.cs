using Common;
using Dsl.Port.Given.Steps;
using Dsl.Core.Scenario.Given;
using Driver.Port.Dtos;
using Common.Domain;
using static Dsl.Core.Scenario.ScenarioDefaults;

namespace Dsl.Core.Scenario.Given;

public class GivenOrder : BaseGiven, IGivenOrder
{
    private string? _orderNumber;
    private string? _sku;
    private string? _quantity;
    private string? _country;
    private string? _couponCodeAlias;
    private OrderStatus _status;

    public GivenOrder(GivenStage givenClause) : base(givenClause)
    {
        WithOrderNumber(DefaultOrderNumber);
        WithSku(DefaultSku);
        WithQuantity(DefaultQuantity);
        WithCountry(DefaultCountry);
        WithCouponCode(Empty);
        WithStatus(DefaultOrderStatus);
    }

    public GivenOrder WithOrderNumber(string orderNumber)
    {
        _orderNumber = orderNumber;
        return this;
    }

    IGivenOrder IGivenOrder.WithOrderNumber(string orderNumber) => WithOrderNumber(orderNumber);

    public GivenOrder WithSku(string? sku)
    {
        _sku = sku;
        return this;
    }

    IGivenOrder IGivenOrder.WithSku(string? sku) => WithSku(sku);

    public GivenOrder WithQuantity(string? quantity)
    {
        _quantity = quantity;
        return this;
    }

    IGivenOrder IGivenOrder.WithQuantity(string? quantity) => WithQuantity(quantity);

    public GivenOrder WithQuantity(int? quantity)
    {
        return WithQuantity(Converter.FromInteger(quantity));
    }

    IGivenOrder IGivenOrder.WithQuantity(int? quantity) => WithQuantity(quantity);

    public GivenOrder WithCountry(string? country)
    {
        _country = country;
        return this;
    }

    IGivenOrder IGivenOrder.WithCountry(string? country) => WithCountry(country);

    public GivenOrder WithCouponCode(string? couponCodeAlias)
    {
        _couponCodeAlias = couponCodeAlias;
        return this;
    }

    IGivenOrder IGivenOrder.WithCouponCode(string? couponCode) => WithCouponCode(couponCode);

    public GivenOrder WithStatus(OrderStatus status)
    {
        _status = status;
        return this;
    }

    IGivenOrder IGivenOrder.WithStatus(OrderStatus status) => WithStatus(status);

    internal override async Task Execute(UseCaseDsl app)
    {
        var shop = await app.MyShop(Channel!);

        (await shop.PlaceOrder()
            .OrderNumber(_orderNumber)
            .Sku(_sku)
            .Quantity(_quantity)
            .Country(_country)
            .CouponCode(_couponCodeAlias)
            .Execute())
            .ShouldSucceed();

        if (_status == OrderStatus.Cancelled)
        {
            (await shop.CancelOrder()
                .OrderNumber(_orderNumber)
                .Execute())
                .ShouldSucceed();
        }

        if (_status == OrderStatus.Delivered)
        {
            (await shop.DeliverOrder()
                .OrderNumber(_orderNumber)
                .Execute())
                .ShouldSucceed();
        }
    }
}
