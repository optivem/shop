using System.Globalization;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using Microsoft.EntityFrameworkCore;
using Npgsql;
using MyCompany.MyShop.Backend.Core.Dtos;
using MyCompany.MyShop.Backend.Core.Entities;
using MyCompany.MyShop.Backend.Core.Exceptions;
using MyCompany.MyShop.Backend.Core.Services.External;
using MyCompany.MyShop.Backend.Data;

namespace MyCompany.MyShop.Backend.Core.Services;

public class OrderService
{
    private const int MoneyScale = 2;
    private const int RateScale = 4;
    private static readonly int RestrictedMonth = 12;
    private static readonly int RestrictedDay = 31;
    private static readonly TimeOnly RestrictedTimeStart = new(23, 59);

    public const string IdempotencyKeyField = "Idempotency-Key";
    private const int MaxIdempotencyKeyLength = 255;
    private static readonly TimeSpan IdempotencyKeyTtl = TimeSpan.FromHours(24);
    private const string MsgIdempotencyKeyTooLong = "Idempotency-Key must be at most 255 characters";
    private const string MsgIdempotencyKeyReused = "Idempotency-Key has already been used with a different request";

    public const int FirstPage = 1;
    public const int DefaultPageSize = 50;
    public const int MaxPageSize = 100;
    private const string FieldPage = "page";
    private const string FieldSize = "size";
    private static readonly string MsgInvalidPage = $"Page must be {FirstPage} or greater";
    private static readonly string MsgInvalidSize = $"Page size must be between 1 and {MaxPageSize}";

    private const string FieldCountry = "country";
    private const string FieldShippingAddressCountry = "shippingAddress.country";

    private static readonly JsonSerializerOptions StoredResponseJson = new(JsonSerializerDefaults.Web);

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

    /// <summary>
    /// Places an order. With an <paramref name="idempotencyKey"/>, a repeat of the same request by the same
    /// caller within 24 hours answers with the stored response instead of placing a second order, and a
    /// repeat with a different request is rejected. Only successes are stored, so a failed request leaves
    /// the key free for a corrected retry.
    /// </summary>
    public async Task<PlaceOrderResponse> PlaceOrderAsync(PlaceOrderRequest request, CurrentUser user, string? idempotencyKey = null)
    {
        var key = string.IsNullOrWhiteSpace(idempotencyKey) ? null : idempotencyKey;
        if (key == null)
        {
            return await PlaceNewOrderAsync(request, user, null);
        }

        if (key.Length > MaxIdempotencyKeyLength)
        {
            throw new ValidationException(IdempotencyKeyField, MsgIdempotencyKeyTooLong);
        }

        var claim = new IdempotencyClaim(user.Subject ?? string.Empty, key, Fingerprint(request));

        var stored = await FindStoredKeyAsync(claim);
        if (stored != null && !IsExpired(stored))
        {
            return Replay(stored, claim);
        }

        try
        {
            return await PlaceNewOrderAsync(request, user, claim with { Expired = stored });
        }
        catch (DbUpdateException ex) when (IsIdempotencyKeyConflict(ex))
        {
            // A concurrent first use of the same key committed first; its transaction holds the order, ours
            // rolled back. Answer as the replay we turned out to be.
            _dbContext.ChangeTracker.Clear();
            var winner = await FindStoredKeyAsync(claim)
                ?? throw new InvalidOperationException("Idempotency key conflict without a stored key", ex);
            return Replay(winner, claim);
        }
    }

    private async Task<PlaceOrderResponse> PlaceNewOrderAsync(PlaceOrderRequest request, CurrentUser user, IdempotencyClaim? claim)
    {
        var sku = request.Sku!;
        var quantity = request.Quantity!.Value;
        // With a shipping address its (trimmed) country is the tax country and the top-level one is ignored.
        var shippingAddress = request.ShippingAddress?.Trimmed();
        var country = shippingAddress != null ? shippingAddress.Country! : request.Country!;
        var countryField = shippingAddress != null ? FieldShippingAddressCountry : FieldCountry;
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

        var taxRate = await GetTaxRateAsync(country, countryField);
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
            UnitPrice = RoundMoney(unitPrice),
            BasePrice = RoundMoney(basePrice),
            DiscountRate = RoundRate(discountRate),
            DiscountAmount = RoundMoney(discountAmount),
            SubtotalPrice = RoundMoney(subtotalPrice),
            TaxRate = RoundRate(taxRate),
            TaxAmount = RoundMoney(taxAmount),
            TotalPrice = RoundMoney(totalPrice),
            Status = OrderStatus.PLACED,
            AppliedCouponCode = appliedCouponCode,
            Owner = user.Subject,
            OwnerName = user.Username,
            ShippingAddress = shippingAddress == null ? null : JsonSerializer.Serialize(shippingAddress, StoredResponseJson)
        };

        var response = new PlaceOrderResponse { OrderNumber = orderNumber };

        await using var transaction = await _dbContext.Database.BeginTransactionAsync();

        // The key row goes in first: a concurrent first use of the same key then waits on the unique
        // constraint and fails, before it has redeemed a coupon or written an order.
        if (claim != null)
        {
            await StoreKeyAsync(claim, response);
        }

        // Increment next: a rejected increment leaves no order behind.
        if (appliedCouponCode != null)
        {
            await _couponService.IncrementUsageCountAsync(appliedCouponCode);
        }

        _dbContext.Orders.Add(order);
        await _dbContext.SaveChangesAsync();
        await transaction.CommitAsync();

        return response;
    }

    private sealed record IdempotencyClaim(string Owner, string Key, string RequestHash)
    {
        /// <summary>A stored row for the same key that has outlived its TTL and is replaced by this use.</summary>
        public IdempotencyKey? Expired { get; init; }
    }

    private Task<IdempotencyKey?> FindStoredKeyAsync(IdempotencyClaim claim) =>
        _dbContext.IdempotencyKeys.FirstOrDefaultAsync(k => k.Owner == claim.Owner && k.Key == claim.Key);

    private static bool IsExpired(IdempotencyKey stored) => stored.CreatedAt <= DateTime.UtcNow - IdempotencyKeyTtl;

    private async Task StoreKeyAsync(IdempotencyClaim claim, PlaceOrderResponse response)
    {
        if (claim.Expired != null)
        {
            _dbContext.IdempotencyKeys.Remove(claim.Expired);
            await _dbContext.SaveChangesAsync();
        }

        _dbContext.IdempotencyKeys.Add(new IdempotencyKey
        {
            Owner = claim.Owner,
            Key = claim.Key,
            RequestHash = claim.RequestHash,
            ResponseStatus = StatusCodes.Status201Created,
            ResponseBody = JsonSerializer.Serialize(response, StoredResponseJson),
            CreatedAt = DateTime.UtcNow
        });
        await _dbContext.SaveChangesAsync();
    }

    private static PlaceOrderResponse Replay(IdempotencyKey stored, IdempotencyClaim claim)
    {
        if (stored.RequestHash != claim.RequestHash)
        {
            throw new ValidationException(MsgIdempotencyKeyReused);
        }

        return JsonSerializer.Deserialize<PlaceOrderResponse>(stored.ResponseBody, StoredResponseJson)!;
    }

    private static bool IsIdempotencyKeyConflict(DbUpdateException ex) =>
        ex.InnerException is PostgresException
        {
            SqlState: PostgresErrorCodes.UniqueViolation,
            ConstraintName: AppDbContext.IdempotencyKeyUniqueConstraint
        };

    /// <summary>
    /// SHA-256 (hex) over the request fields exactly as received, null as empty, joined by newlines. The shipping
    /// address fields are appended only when an address is given, so a Quick order hashes as it always has.
    /// </summary>
    private static string Fingerprint(PlaceOrderRequest request)
    {
        var fields = new List<string>
        {
            request.Sku ?? string.Empty,
            request.Quantity?.ToString(CultureInfo.InvariantCulture) ?? string.Empty,
            request.Country ?? string.Empty,
            request.CouponCode ?? string.Empty
        };
        if (request.ShippingAddress is { } address)
        {
            fields.AddRange(new[]
            {
                address.FirstName, address.LastName, address.Street, address.PostalCode, address.City, address.Country
            }.Select(field => field ?? string.Empty));
        }

        var canonical = string.Join('\n', fields);
        return Convert.ToHexString(SHA256.HashData(Encoding.UTF8.GetBytes(canonical))).ToLowerInvariant();
    }

    // Intermediate amounts stay exact; each value is rounded exactly once, here, to the scale of the
    // column it is persisted in (NUMERIC(10,2) for money, NUMERIC(5,4) for rates).
    private static decimal RoundMoney(decimal value) => Math.Round(value, MoneyScale, MidpointRounding.AwayFromZero);

    private static decimal RoundRate(decimal value) => Math.Round(value, RateScale, MidpointRounding.AwayFromZero);

    private async Task<decimal> GetUnitPriceAsync(string sku)
    {
        var productDetails = await _erpGateway.GetProductDetailsAsync(sku);
        if (productDetails == null)
        {
            throw new ValidationException("sku", $"Product does not exist for SKU: {sku}");
        }

        return productDetails.Price;
    }

    private async Task<decimal> GetTaxRateAsync(string country, string countryField)
    {
        var taxDetails = await _taxGateway.GetTaxDetailsAsync(country);
        if (taxDetails == null)
        {
            throw new ValidationException(countryField, $"Country does not exist: {country}");
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

    public async Task<RecallSkuResponse> RecallSkuAsync(string sku)
    {
        if (string.IsNullOrWhiteSpace(sku))
        {
            throw new ValidationException("sku", "SKU must not be empty");
        }

        var orders = await _dbContext.Orders
            .Where(o => o.Sku == sku)
            .ToListAsync();

        var cancelledCount = 0;
        foreach (var order in orders)
        {
            if (order.Status == OrderStatus.PLACED)
            {
                order.Status = OrderStatus.CANCELLED;
                await _dbContext.SaveChangesAsync();
                cancelledCount++;
            }
        }

        return new RecallSkuResponse
        {
            Sku = sku,
            CancelledCount = cancelledCount
        };
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
                throw new ValidationException("Order cancellation is not allowed on December 31st between 22:00 and 22:30");
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

    /// <summary>
    /// A page of orders, newest first. A customer sees only their own; an admin sees all, optionally narrowed to
    /// one customer (<paramref name="customerFilter"/> matches the owner's username exactly; ignored for customers).
    /// </summary>
    public async Task<BrowseOrderHistoryResponse> BrowseOrderHistoryAsync(
        string? orderNumberFilter, CurrentUser user, int? page = null, int? size = null, string? customerFilter = null)
    {
        var (requestedPage, requestedSize) = ResolvePage(page, size);

        var query = _dbContext.Orders.AsQueryable();

        if (!user.Admin)
        {
            query = query.Where(o => o.Owner == user.Subject);
        }
        else if (!string.IsNullOrWhiteSpace(customerFilter))
        {
            var customer = customerFilter.Trim();
            query = query.Where(o => o.OwnerName == customer);
        }

        if (!string.IsNullOrWhiteSpace(orderNumberFilter))
        {
            var filter = orderNumberFilter.Trim();
            query = query.Where(o => EF.Functions.ILike(o.OrderNumber, $"%{filter}%"));
        }

        var totalElements = await query.LongCountAsync();
        var offset = (long)(requestedPage - FirstPage) * requestedSize;
        var orders = offset >= totalElements
            ? new List<Order>()
            : await query
                .OrderByDescending(o => o.OrderTimestamp)
                .ThenByDescending(o => o.Id)
                .Skip((int)offset)
                .Take(requestedSize)
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

        return new BrowseOrderHistoryResponse
        {
            Orders = items,
            Page = requestedPage,
            Size = requestedSize,
            TotalElements = totalElements,
            TotalPages = TotalPages(totalElements, requestedSize)
        };
    }

    /// <summary>Validates an optional 1-based page and page size, and fills in the defaults.</summary>
    public static (int Page, int Size) ResolvePage(int? page, int? size)
    {
        if (page is < FirstPage)
        {
            throw new ValidationException(FieldPage, MsgInvalidPage);
        }

        if (size is < 1 or > MaxPageSize)
        {
            throw new ValidationException(FieldSize, MsgInvalidSize);
        }

        return (page ?? FirstPage, size ?? DefaultPageSize);
    }

    /// <summary>Ceiling division; an empty list is zero pages.</summary>
    public static int TotalPages(long totalElements, int size) => (int)((totalElements + size - 1) / size);

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
            AppliedCouponCode = order.AppliedCouponCode,
            ShippingAddress = order.ShippingAddress == null
                ? null
                : JsonSerializer.Deserialize<ShippingAddress>(order.ShippingAddress, StoredResponseJson)
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
