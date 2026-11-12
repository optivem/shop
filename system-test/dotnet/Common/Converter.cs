namespace Common;

public static class Converter
{
    public static decimal? ToDecimal(string? value)
    {
        return To(value, decimal.Parse);
    }

    public static string? FromDecimal(decimal? value)
    {
        return From(value, v => v.ToString());
    }

    public static int? ToInteger(string? value, params string[] nullValues)
    {
        if (string.IsNullOrEmpty(value))
        {
            return null;
        }

        if (nullValues != null && nullValues.Any(nv => value.Equals(nv, StringComparison.OrdinalIgnoreCase)))
        {
            return null;
        }

        return int.Parse(value);
    }

    public static string? FromInteger(int? value)
    {
        return From(value, v => v.ToString());
    }

    public static DateTimeOffset? ToDateTimeOffset(string? value)
    {
        return To(value, DateTimeOffset.Parse);
    }

    public static string? FromDateTimeOffset(DateTimeOffset? value)
    {
        return From(value, v => v.ToString("O")); // ISO 8601 format
    }

    /// <summary>Parses an ISO or en-US date as UTC (Kind = Utc), so the result never depends on the machine's timezone.</summary>
    public static DateTime? ToDateTime(string? text, params string[] nullValues)
    {
        if (string.IsNullOrEmpty(text))
        {
            return null;
        }

        if (nullValues != null && nullValues.Any(nv => text.Equals(nv, StringComparison.OrdinalIgnoreCase)))
        {
            return null;
        }

        // Try ISO format first
        if (DateTime.TryParse(text, System.Globalization.CultureInfo.InvariantCulture,
            UtcStyles, out var isoResult))
        {
            return isoResult;
        }

        // Try various locale-specific formats that JavaScript's toLocaleString() might produce
        string[] formats = {
            "M/d/yyyy, h:mm:ss tt",      // US format: 1/30/2026, 11:44:29 AM
            "d/M/yyyy, HH:mm:ss",        // UK format
            "yyyy-MM-dd HH:mm:ss",       // Generic
            "M/d/yyyy h:mm:ss tt"
        };

        foreach (var format in formats)
        {
            if (DateTime.TryParseExact(text, format, System.Globalization.CultureInfo.InvariantCulture,
                UtcStyles, out var result))
            {
                return result;
            }
        }

        throw new FormatException($"Invalid date format: {text} - Expected ISO format or locale-specific format.");
    }

    private const System.Globalization.DateTimeStyles UtcStyles =
        System.Globalization.DateTimeStyles.AssumeUniversal | System.Globalization.DateTimeStyles.AdjustToUniversal;

    private static string? From<TSource>(TSource? value, Func<TSource, string> converter)
        where TSource : struct
    {
        return value == null ? null : converter(value.Value);
    }

    private static TResult? To<TResult>(string? value, Func<string, TResult> converter)
        where TResult : struct
    {
        return string.IsNullOrEmpty(value) ? null : converter(value);
    }
}
