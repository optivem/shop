using Driver.Port.Dtos;
using Common.Domain;

namespace Dsl.Core.Scenario;

/// <summary>
/// Default values for scenario test builders.
/// These defaults are used when test data is not explicitly specified.
/// </summary>
public static class ScenarioDefaults
{
    // Product defaults
    public const string DefaultSku = "DEFAULT-SKU";
    public const string DefaultUnitPrice = "20.00";

    // Order defaults
    public const string DefaultOrderNumber = "DEFAULT-ORDER";
    public const string DefaultQuantity = "1";
    public const string DefaultCountry = "US";
    public const OrderStatus DefaultOrderStatus = OrderStatus.Placed;

    // Promotion defaults
    public const bool DefaultPromotionActive = false;
    public const string DefaultPromotionDiscount = "1.00";

    // Clock defaults
    public const string DefaultTime = "2025-12-24T10:00:00Z";
    public const string WeekdayTime = "2026-01-15T10:30:00Z";
    public const string WeekendTime = "2026-01-17T10:30:00Z";

    // Tax defaults
    public const string DefaultTaxRate = "0.07";

    // Coupon defaults
    public const string DefaultCouponCode = "DEFAULT-COUPON";
    public const string DefaultDiscountRate = "0.10";
    public const string DefaultValidFrom = "2024-01-01T00:00:00Z";
    public const string DefaultValidTo = "2024-12-31T23:59:59Z";
    public const string DefaultUsageLimit = "1000";

    public const string? Empty = null;
}



