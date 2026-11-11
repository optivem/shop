using Driver.Port;
using Dsl.Core.UseCase.UseCases.Base;
using Dsl.Core.Shared;
using Driver.Port.Dtos;

namespace Dsl.Core.UseCase.UseCases;

/// <summary>SKUs are aliases, resolved to the values the ERP was given. Without an action, only the cart is filled.</summary>
public class ManageCart : BaseMyShopUseCase<ManageCartResponse, ManageCartVerification>
{
    private readonly List<(string SkuAlias, string Quantity)> _cartLines = [];
    private bool? _continueSession;
    private string? _action;
    private string? _skuAlias;
    private string? _quantity;

    public ManageCart(IMyShopDriver driver, UseCaseContext context)
        : base(driver, context)
    {
    }

    /// <summary>A line the cart gets first, added from its product page; lines are added in the order given.</summary>
    public ManageCart CartLine(string skuAlias, string quantity)
    {
        _cartLines.Add((skuAlias, quantity));
        return this;
    }

    /// <summary>Keeps the browser session of the previous call instead of starting a new one.</summary>
    public ManageCart ContinueSession()
    {
        _continueSession = true;
        return this;
    }

    public ManageCart ViewCart() => Action(ManageCartRequest.ViewCart, null, null);

    public ManageCart BrowseCatalog() => Action(ManageCartRequest.BrowseCatalog, null, null);

    public ManageCart AddFromCatalog(string skuAlias) => Action(ManageCartRequest.AddFromCatalog, skuAlias, null);

    public ManageCart AddFromProductPage(string skuAlias, string quantity)
        => Action(ManageCartRequest.AddFromProductPage, skuAlias, quantity);

    public ManageCart ChangeQuantity(string skuAlias, string quantity)
        => Action(ManageCartRequest.ChangeQuantity, skuAlias, quantity);

    public ManageCart IncreaseQuantity(string skuAlias) => Action(ManageCartRequest.IncreaseQuantity, skuAlias, null);

    public ManageCart DecreaseQuantity(string skuAlias) => Action(ManageCartRequest.DecreaseQuantity, skuAlias, null);

    public ManageCart RemoveLine(string skuAlias) => Action(ManageCartRequest.RemoveLine, skuAlias, null);

    public ManageCart Revisit() => Action(ManageCartRequest.Revisit, null, null);

    public ManageCart SignIn() => Action(ManageCartRequest.SignIn, null, null);

    private ManageCart Action(string action, string? skuAlias, string? quantity)
    {
        _action = action;
        _skuAlias = skuAlias;
        _quantity = quantity;
        return this;
    }

    public override async Task<MyShopUseCaseResult<ManageCartResponse, ManageCartVerification>> Execute()
    {
        var request = new ManageCartRequest
        {
            CartLines = _cartLines
                .Select(line => new ManageCartRequest.CartLineDto
                {
                    Sku = _context.GetParamValue(line.SkuAlias),
                    Quantity = line.Quantity,
                })
                .ToList(),
            ContinueSession = _continueSession,
            Action = _action,
            Sku = _skuAlias == null ? null : _context.GetParamValue(_skuAlias),
            Quantity = _quantity,
        };

        var result = await _driver.ManageCartAsync(request);

        return new MyShopUseCaseResult<ManageCartResponse, ManageCartVerification>(
            result,
            _context,
            (response, ctx) => new ManageCartVerification(response, ctx));
    }
}
