using System.Text.Json;
using System.Text.Json.Serialization;

namespace Driver.Port.Dtos;

/// <summary>The usernames that have placed an order, A to Z. On the wire it is a bare JSON array of strings, not an object.</summary>
[JsonConverter(typeof(BareArrayConverter))]
public class BrowseCustomersResponse
{
    public List<string>? Customers { get; set; }

    public override string ToString() => $"[{string.Join(", ", Customers ?? [])}]";

    private sealed class BareArrayConverter : JsonConverter<BrowseCustomersResponse>
    {
        public override BrowseCustomersResponse Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
            => new() { Customers = JsonSerializer.Deserialize<List<string>>(ref reader, options) };

        public override void Write(Utf8JsonWriter writer, BrowseCustomersResponse value, JsonSerializerOptions options)
            => JsonSerializer.Serialize(writer, value.Customers, options);
    }
}
