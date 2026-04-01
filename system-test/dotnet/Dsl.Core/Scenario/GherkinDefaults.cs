using Driver.Port.Shop.Dtos;

namespace Dsl.Core.Gherkin;

/// <summary>
/// Default values for Gherkin test builders.
/// These defaults are used when test data is not explicitly specified.
/// </summary>
public static class GherkinDefaults
{
    // Product defaults
    public const string DefaultSku = "DEFAULT-SKU";
    public const string DefaultUnitPrice = "20.00";

    // Order defaults
    public const string DefaultOrderNumber = "DEFAULT-ORDER";
    public const string DefaultQuantity = "1";
    public const OrderStatus DefaultOrderStatus = OrderStatus.Placed;

    // Clock defaults
    public const string DefaultTime = "2025-12-24T10:00:00Z";
    public const string WeekdayTime = "2026-01-15T10:30:00Z";
    public const string WeekendTime = "2026-01-17T10:30:00Z";

    public const string Empty = "";
}



