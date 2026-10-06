using Microsoft.EntityFrameworkCore;
using MyCompany.MyShop.Monolith.Core.Dtos;
using MyCompany.MyShop.Monolith.Core.Entities;
using MyCompany.MyShop.Monolith.Core.Exceptions;
using MyCompany.MyShop.Monolith.Core.Services.External;
using MyCompany.MyShop.Monolith.Data;

namespace MyCompany.MyShop.Monolith.Core.Services;

public class OrderService
{
    private static readonly int RestrictedMonth = 12;
    private static readonly int RestrictedDay = 31;
    private static readonly TimeOnly RestrictedTimeStart = new(23, 59);

    private readonly AppDbContext _dbContext;
    private readonly ErpGateway _erpGateway;
    private readonly TaxGateway _taxGateway;
    private readonly ClockGateway _clockGateway;
    private readonly CouponService _couponService;

    public OrderService(AppDbContext dbContext, ErpGateway erpGateway, TaxGateway taxGateway, ClockGateway clockGateway, CouponService couponService)
    {
        _dbContext = dbContext;
        _erpGateway = erpGateway;
        _taxGateway = taxGateway;
        _clockGateway = clockGateway;
        _couponService = couponService;
    }

    public async Task<PlaceOrderResponse> PlaceOrderAsync(PlaceOrderRequest request, CurrentUser user)
    {
        var sku = request.Sku!;
        var quantity = request.Quantity!.Value;
        var country = request.Country!;
        var couponCode = request.CouponCode;

        var orderTimestamp = await _clockGateway.GetCurrentTimeAsync();

        var utcTime = orderTimestamp.ToUniversalTime();

        if (utcTime.Month == RestrictedMonth && utcTime.Day == RestrictedDay)
        {
            var currentTime = TimeOnly.FromDateTime(utcTime);
            if (currentTime >= RestrictedTimeStart)
            {
                throw new ValidationException("Orders cannot be placed between 23:59 and 00:00 on December 31st");
            }
        }

        var unitPrice = await GetUnitPriceAsync(sku);
        var promotion = await _erpGateway.GetPromotionDetailsAsync();
        var promotionFactor = promotion.PromotionActive ? promotion.Discount : 1.0m;
        var basePrice = unitPrice * quantity;
        var promotedPrice = basePrice * promotionFactor;

        var discountRate = await _couponService.GetDiscountAsync(couponCode);
        var discountAmount = promotedPrice * discountRate;
        var subtotalPrice = promotedPrice - discountAmount;

        var taxRate = await GetTaxRateAsync(country);
        var taxAmount = subtotalPrice * taxRate;
        var totalPrice = subtotalPrice + taxAmount;

        var appliedCouponCode = discountRate > 0m ? couponCode : null;

        var orderNumber = GenerateOrderNumber();

        var order = new Order
        {
            OrderNumber = orderNumber,
            OrderTimestamp = orderTimestamp,
            Country = country,
            Sku = sku,
            Quantity = quantity,
            UnitPrice = unitPrice,
            BasePrice = basePrice,
            DiscountRate = discountRate,
            DiscountAmount = discountAmount,
            SubtotalPrice = subtotalPrice,
            TaxRate = taxRate,
            TaxAmount = taxAmount,
            TotalPrice = totalPrice,
            Status = OrderStatus.PLACED,
            AppliedCouponCode = appliedCouponCode,
            Owner = user.Subject,
            OwnerName = user.Username
        };

        _dbContext.Orders.Add(order);
        await _dbContext.SaveChangesAsync();

        if (appliedCouponCode != null)
        {
            await _couponService.IncrementUsageCountAsync(appliedCouponCode);
        }

        return new PlaceOrderResponse { OrderNumber = orderNumber };
    }

    private async Task<decimal> GetUnitPriceAsync(string sku)
    {
        var productDetails = await _erpGateway.GetProductDetailsAsync(sku);
        if (productDetails == null)
        {
            throw new ValidationException("sku", $"Product does not exist for SKU: {sku}");
        }

        return productDetails.Price;
    }

    private async Task<decimal> GetTaxRateAsync(string country)
    {
        var taxDetails = await _taxGateway.GetTaxDetailsAsync(country);
        if (taxDetails == null)
        {
            throw new ValidationException("country", $"Country does not exist: {country}");
        }

        return taxDetails.TaxRate;
    }

    public async Task DeliverOrderAsync(string orderNumber)
    {
        var order = await _dbContext.Orders
            .FirstOrDefaultAsync(o => o.OrderNumber == orderNumber);

        if (order == null)
        {
            throw new NotExistValidationException($"Order {orderNumber} does not exist.");
        }

        if (order.Status != OrderStatus.PLACED)
        {
            throw new ValidationException("Order cannot be delivered in its current status");
        }

        order.Status = OrderStatus.DELIVERED;
        await _dbContext.SaveChangesAsync();
    }

    public async Task CancelOrderAsync(string orderNumber, CurrentUser user)
    {
        var now = (await _clockGateway.GetCurrentTimeAsync()).ToUniversalTime();

        if (now.Month == 12 && now.Day == 31)
        {
            var currentTime = TimeOnly.FromDateTime(now);
            var cancelBlackoutStart = new TimeOnly(22, 0);
            var cancelBlackoutEnd = new TimeOnly(22, 30);

            if (currentTime >= cancelBlackoutStart && currentTime <= cancelBlackoutEnd)
            {
                throw new ValidationException("Order cancellation is not allowed on December 31st between 22:00 and 23:00");
            }
        }

        var order = await FindAccessibleOrderAsync(orderNumber, user);

        if (order.Status == OrderStatus.CANCELLED)
        {
            throw new ValidationException("Order has already been cancelled");
        }

        order.Status = OrderStatus.CANCELLED;
        await _dbContext.SaveChangesAsync();
    }

    public async Task<BrowseOrderHistoryResponse> BrowseOrderHistoryAsync(string? orderNumberFilter, CurrentUser user)
    {
        var query = _dbContext.Orders.AsQueryable();

        if (!user.Admin)
        {
            query = query.Where(o => o.Owner == user.Subject);
        }

        if (!string.IsNullOrWhiteSpace(orderNumberFilter))
        {
            var filter = orderNumberFilter.Trim();
            query = query.Where(o => EF.Functions.ILike(o.OrderNumber, $"%{filter}%"));
        }

        var orders = await query
            .OrderByDescending(o => o.OrderTimestamp)
            .ToListAsync();

        var items = orders.Select(order => new BrowseOrderHistoryItemResponse
        {
            OrderNumber = order.OrderNumber,
            OrderTimestamp = order.OrderTimestamp,
            Sku = order.Sku,
            Country = order.Country,
            Quantity = order.Quantity,
            TotalPrice = order.TotalPrice,
            Status = order.Status,
            AppliedCouponCode = order.AppliedCouponCode,
            Customer = order.OwnerName
        }).ToList();

        return new BrowseOrderHistoryResponse { Orders = items };
    }

    public async Task<ViewOrderDetailsResponse> GetOrderAsync(string orderNumber, CurrentUser user)
    {
        var order = await FindAccessibleOrderAsync(orderNumber, user);

        return new ViewOrderDetailsResponse
        {
            OrderNumber = order.OrderNumber,
            OrderTimestamp = order.OrderTimestamp,
            Sku = order.Sku,
            Quantity = order.Quantity,
            UnitPrice = order.UnitPrice,
            BasePrice = order.BasePrice,
            DiscountRate = order.DiscountRate,
            DiscountAmount = order.DiscountAmount,
            SubtotalPrice = order.SubtotalPrice,
            TaxRate = order.TaxRate,
            TaxAmount = order.TaxAmount,
            TotalPrice = order.TotalPrice,
            Status = order.Status,
            Country = order.Country,
            AppliedCouponCode = order.AppliedCouponCode
        };
    }

    /// <summary>Another customer's order is reported as non-existent, so its existence is not revealed.</summary>
    private async Task<Order> FindAccessibleOrderAsync(string orderNumber, CurrentUser user)
    {
        var order = await _dbContext.Orders
            .FirstOrDefaultAsync(o => o.OrderNumber == orderNumber);

        if (order == null || (!user.Admin && order.Owner != user.Subject))
        {
            throw new NotExistValidationException($"Order {orderNumber} does not exist.");
        }

        return order;
    }

    private static string GenerateOrderNumber()
    {
        return $"ORD-{Guid.NewGuid().ToString().ToUpper()}";
    }
}
