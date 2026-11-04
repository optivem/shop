using System.Globalization;
using System.Text.Json;
using System.Text.Json.Serialization;

namespace MyCompany.MyShop.Monolith.Core.Dtos.Json;

/// <summary>
/// Writes a response <c>decimal</c> as a JSON string with a fixed scale (e.g. <c>"20.00"</c>,
/// <c>"0.2000"</c>), rounding half away from zero. Money and rates go on the wire as strings so no
/// consumer has to read them through a binary float. Applied per property on the response DTOs only,
/// so request reading (<see cref="PublishCouponRequest"/>) and the ERP/Tax client reads are untouched.
/// Reading accepts both a JSON string and a JSON number.
/// </summary>
public abstract class DecimalStringJsonConverter : JsonConverter<decimal>
{
    private readonly int _scale;

    protected DecimalStringJsonConverter(int scale)
    {
        _scale = scale;
    }

    public override decimal Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
    {
        if (reader.TokenType == JsonTokenType.String)
        {
            return decimal.Parse(reader.GetString()!, NumberStyles.Number, CultureInfo.InvariantCulture);
        }
        return reader.GetDecimal();
    }

    public override void Write(Utf8JsonWriter writer, decimal value, JsonSerializerOptions options)
    {
        var rounded = Math.Round(value, _scale, MidpointRounding.AwayFromZero);
        writer.WriteStringValue(rounded.ToString("F" + _scale, CultureInfo.InvariantCulture));
    }
}

/// <summary>Money amount: 2 decimal places on the wire (<c>NUMERIC(10,2)</c>).</summary>
public sealed class MoneyJsonConverter : DecimalStringJsonConverter
{
    public const int Scale = 2;

    public MoneyJsonConverter() : base(Scale)
    {
    }
}

/// <summary>Rate (discount, tax): 4 decimal places on the wire (<c>NUMERIC(5,4)</c>).</summary>
public sealed class RateJsonConverter : DecimalStringJsonConverter
{
    public const int Scale = 4;

    public RateJsonConverter() : base(Scale)
    {
    }
}
