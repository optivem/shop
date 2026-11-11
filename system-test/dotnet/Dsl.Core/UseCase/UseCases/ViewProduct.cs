using Driver.Port;
using Dsl.Core.UseCase.UseCases.Base;
using Dsl.Core.Shared;
using Driver.Port.Dtos;

namespace Dsl.Core.UseCase.UseCases;

/// <summary>SKUs are aliases, resolved to the values the ERP was given.</summary>
public class ViewProduct : BaseMyShopUseCase<ViewProductResponse, ViewProductVerification>
{
    private string? _skuAlias;
    private bool? _fromCatalog;
    private string? _cartSkuAlias;
    private string? _cartQuantity;
    private string? _quantity;
    private string? _quantityStep;
    private bool? _addToCart;
    private string? _followUp;

    public ViewProduct(IMyShopDriver driver, UseCaseContext context)
        : base(driver, context)
    {
    }

    public ViewProduct Sku(string? skuAlias)
    {
        _skuAlias = skuAlias;
        return this;
    }

    public ViewProduct FromCatalog()
    {
        _fromCatalog = true;
        return this;
    }

    public ViewProduct CartLine(string skuAlias, string quantity)
    {
        _cartSkuAlias = skuAlias;
        _cartQuantity = quantity;
        return this;
    }

    public ViewProduct Quantity(string? quantity)
    {
        _quantity = quantity;
        return this;
    }

    public ViewProduct IncreaseQuantity()
    {
        _quantityStep = ViewProductRequest.Increase;
        return this;
    }

    public ViewProduct DecreaseQuantity()
    {
        _quantityStep = ViewProductRequest.Decrease;
        return this;
    }

    public ViewProduct AddToCart()
    {
        _addToCart = true;
        return this;
    }

    public ViewProduct BackToCatalog()
    {
        _followUp = ViewProductRequest.BackToCatalog;
        return this;
    }

    public override async Task<MyShopUseCaseResult<ViewProductResponse, ViewProductVerification>> Execute()
    {
        var request = new ViewProductRequest
        {
            Sku = _context.GetParamValue(_skuAlias!),
            FromCatalog = _fromCatalog,
            CartSku = _cartSkuAlias == null ? null : _context.GetParamValue(_cartSkuAlias),
            CartQuantity = _cartQuantity,
            Quantity = _quantity,
            QuantityStep = _quantityStep,
            AddToCart = _addToCart,
            FollowUp = _followUp,
        };

        var result = await _driver.ViewProductAsync(request);

        return new MyShopUseCaseResult<ViewProductResponse, ViewProductVerification>(
            result,
            _context,
            (response, ctx) => new ViewProductVerification(response, ctx));
    }
}
