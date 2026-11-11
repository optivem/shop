using Driver.Port;
using Dsl.Core.UseCase.UseCases.Base;
using Dsl.Core.Shared;
using Driver.Port.Dtos;

namespace Dsl.Core.UseCase.UseCases;

/// <summary>
/// SKUs and coupon codes are aliases, resolved to the values the ERP and the coupon were given. The shipping address
/// is literal: its country is a code the checkout's form offers. Without <see cref="PlaceOrder"/>, the checkout is
/// only opened (and the address entered).
/// </summary>
public class CheckOut : BaseMyShopUseCase<CheckOutResponse, CheckOutVerification>
{
    private readonly List<(string SkuAlias, string Quantity)> _cartLines = [];
    private bool? _continueSession;
    private string _entry = CheckOutRequest.Open;
    private bool? _signIn;
    private ShippingAddress? _shippingAddress;
    private string? _couponCodeAlias;
    private bool? _placeOrder;

    public CheckOut(IMyShopDriver driver, UseCaseContext context)
        : base(driver, context)
    {
    }

    /// <summary>A line the cart gets first, added from its product page; lines are added in the order given.</summary>
    public CheckOut CartLine(string skuAlias, string quantity)
    {
        _cartLines.Add((skuAlias, quantity));
        return this;
    }

    /// <summary>Keeps the browser session (and its cart) of the previous call instead of starting a new one.</summary>
    public CheckOut ContinueSession()
    {
        _continueSession = true;
        return this;
    }

    public CheckOut ProceedFromCart()
    {
        _entry = CheckOutRequest.ProceedFromCart;
        return this;
    }

    public CheckOut SignIn()
    {
        _signIn = true;
        return this;
    }

    public CheckOut ShippingAddress(string firstName, string lastName, string street, string postalCode, string city,
        string country)
    {
        _shippingAddress = new ShippingAddress
        {
            FirstName = firstName,
            LastName = lastName,
            Street = street,
            PostalCode = postalCode,
            City = city,
            Country = country,
        };
        return this;
    }

    public CheckOut CouponCode(string couponCodeAlias)
    {
        _couponCodeAlias = couponCodeAlias;
        return this;
    }

    public CheckOut PlaceOrder()
    {
        _placeOrder = true;
        return this;
    }

    public override async Task<MyShopUseCaseResult<CheckOutResponse, CheckOutVerification>> Execute()
    {
        var request = new CheckOutRequest
        {
            CartLines = _cartLines
                .Select(line => new CheckOutRequest.CartLineDto
                {
                    Sku = _context.GetParamValue(line.SkuAlias),
                    Quantity = line.Quantity,
                })
                .ToList(),
            ContinueSession = _continueSession,
            Entry = _entry,
            SignIn = _signIn,
            ShippingAddress = _shippingAddress,
            CouponCode = _couponCodeAlias == null ? null : _context.GetParamValue(_couponCodeAlias),
            PlaceOrder = _placeOrder,
        };

        var result = await _driver.CheckOutAsync(request);

        return new MyShopUseCaseResult<CheckOutResponse, CheckOutVerification>(
            result,
            _context,
            (response, ctx) => new CheckOutVerification(response, ctx));
    }
}
