using System.ComponentModel.DataAnnotations;
using MyCompany.MyShop.Monolith.Core.Dtos;
using MyCompany.MyShop.Monolith.Core.Exceptions;
using MyCompany.MyShop.Monolith.Core.Services;
using MyCompany.MyShop.Monolith.Data;
using ValidationException = MyCompany.MyShop.Monolith.Core.Exceptions.ValidationException;

namespace MyCompany.MyShop.Monolith.Pages.Storefront;

/// <summary>
/// Places one order per cart line, in cart order, with the same shipping address and coupon (UC-009 BR-1,
/// storefront-ux §6.5). A rejected line doesn't stop the rest; its reason is the backend's own message, as the
/// place-order API would report it.
/// </summary>
public sealed class CheckoutPlacer
{
    // The same texts the place-order API answers with (GlobalExceptionHandler).
    internal const string ErpUnavailable = "Product information can't be retrieved right now";
    internal const string UnexpectedError = "An unexpected error occurred. Please try again later.";

    private readonly OrderService _orderService;
    private readonly AppDbContext _dbContext;
    private readonly ILogger<CheckoutPlacer> _logger;

    public CheckoutPlacer(OrderService orderService, AppDbContext dbContext, ILogger<CheckoutPlacer> logger)
    {
        _orderService = orderService;
        _dbContext = dbContext;
        _logger = logger;
    }

    public async Task<CheckoutResult> PlaceAsync(
        IReadOnlyList<CartLine> lines, ShippingAddress address, string? couponCode, CurrentUser user)
    {
        var placed = new List<PlacedLine>();
        var rejected = new List<RejectedLine>();
        foreach (var line in lines)
        {
            var request = new PlaceOrderRequest
            {
                Sku = line.Sku,
                Quantity = line.Quantity,
                ShippingAddress = address,
                CouponCode = couponCode,
            };
            var (orderNumber, reason) = await PlaceOneAsync(request, user);
            if (orderNumber != null)
            {
                placed.Add(new PlacedLine(line.Sku, line.Quantity, orderNumber));
            }
            else
            {
                rejected.Add(new RejectedLine(line.Sku, line.Quantity, reason!));
            }
        }

        return new CheckoutResult(placed, rejected);
    }

    /// <summary>Places the order: its number when placed, else why it was rejected.</summary>
    private async Task<(string? OrderNumber, string? Reason)> PlaceOneAsync(PlaceOrderRequest request, CurrentUser user)
    {
        var violations = new List<ValidationResult>();
        if (!Validator.TryValidateObject(request, new ValidationContext(request), violations, validateAllProperties: true))
        {
            return (null, string.Join("; ", violations.Select(v => v.ErrorMessage).Order(StringComparer.Ordinal)));
        }

        try
        {
            var response = await _orderService.PlaceOrderAsync(request, user);
            return (response.OrderNumber, null);
        }
        catch (ValidationException e)
        {
            Forget();
            return (null, e.Message);
        }
        catch (ErpUnavailableException e)
        {
            Forget();
            _logger.LogError(e, "ERP unavailable while placing a checkout order");
            return (null, ErpUnavailable);
        }
        catch (Exception e)
        {
            Forget();
            _logger.LogError(e, "Unexpected error while placing a checkout order");
            return (null, UnexpectedError);
        }
    }

    /// <summary>A rejected line must leave nothing tracked that the next line's save would write.</summary>
    private void Forget() => _dbContext.ChangeTracker.Clear();
}
