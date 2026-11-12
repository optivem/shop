namespace Dsl.Core.Scenario;

/// <summary>
/// Context extracted from execution result - order number and coupon code from the executed operation.
/// </summary>
public class ExecutionResultContext
{
    private static readonly ExecutionResultContext _empty = new(null, null);

    public ExecutionResultContext(string? orderNumber, string? couponCode)
    {
        OrderNumber = orderNumber;
        CouponCode = couponCode;
    }

    public static ExecutionResultContext Empty() => _empty;

    public string? OrderNumber { get; }
    public string? CouponCode { get; }

    /// <summary>The SKU of each order the operation placed, by order number, once looked up.</summary>
    private Task<List<KeyValuePair<string, string?>>>? _placedSkus;
    private readonly object _placedSkusLock = new();

    /// <summary>The SKU of each order the operation placed, by order number; looked up with <paramref name="lookUp"/> the first time only.</summary>
    public Task<List<KeyValuePair<string, string?>>> PlacedSkusAsync(Func<Task<List<KeyValuePair<string, string?>>>> lookUp)
    {
        lock (_placedSkusLock)
        {
            return _placedSkus ??= lookUp();
        }
    }
}
