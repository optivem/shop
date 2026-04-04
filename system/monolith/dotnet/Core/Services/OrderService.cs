using Microsoft.EntityFrameworkCore;
using Optivem.Shop.Monolith.Core.Dtos;
using Optivem.Shop.Monolith.Core.Entities;
using Optivem.Shop.Monolith.Core.Exceptions;
using Optivem.Shop.Monolith.Core.Services.External;
using Optivem.Shop.Monolith.Data;

namespace Optivem.Shop.Monolith.Core.Services;

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

    public async Task<PlaceOrderResponse> PlaceOrderAsync(PlaceOrderRequest request)
    {
        var sku = request.Sku!;
        var quantity = request.Quantity!.Value;
        var country = !string.IsNullOrWhiteSpace(request.Country) ? request.Country : "US";
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
        var basePrice = unitPrice * quantity * promotionFactor;

        var discountRate = await _couponService.GetDiscountAsync(couponCode);
        var discountAmount = basePrice * discountRate;
        var subtotalPrice = basePrice - discountAmount;

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
            AppliedCouponCode = appliedCouponCode
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

    public async Task<BrowseOrderHistoryResponse> BrowseOrderHistoryAsync(string? orderNumberFilter)
    {
        List<Order> orders;

        if (string.IsNullOrWhiteSpace(orderNumberFilter))
        {
            orders = await _dbContext.Orders
                .OrderByDescending(o => o.OrderTimestamp)
                .ToListAsync();
        }
        else
        {
            var filter = orderNumberFilter.Trim();
            orders = await _dbContext.Orders
                .Where(o => EF.Functions.ILike(o.OrderNumber, $"%{filter}%"))
                .OrderByDescending(o => o.OrderTimestamp)
                .ToListAsync();
        }

        var items = orders.Select(order => new BrowseOrderHistoryItemResponse
        {
            OrderNumber = order.OrderNumber,
            OrderTimestamp = order.OrderTimestamp,
            Sku = order.Sku,
            Country = order.Country,
            Quantity = order.Quantity,
            TotalPrice = order.TotalPrice,
            Status = order.Status,
            AppliedCouponCode = order.AppliedCouponCode
        }).ToList();

        return new BrowseOrderHistoryResponse { Orders = items };
    }

    public async Task<ViewOrderDetailsResponse> GetOrderAsync(string orderNumber)
    {
        var order = await _dbContext.Orders
            .FirstOrDefaultAsync(o => o.OrderNumber == orderNumber);

        if (order == null)
        {
            throw new NotExistValidationException($"Order {orderNumber} does not exist.");
        }

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

    private static string GenerateOrderNumber()
    {
        return $"ORD-{Guid.NewGuid().ToString().ToUpper()}";
    }
}
