using System.Text.Json.Serialization;
using MyCompany.MyShop.Backend.Core.Dtos.Json;

namespace MyCompany.MyShop.Backend.Core.Dtos;

/// <summary>One catalog product, as served by GET /api/products and GET /api/products/{sku}.</summary>
public class ViewProductDetailsResponse
{
    public string Sku { get; set; } = null!;
    public string? Title { get; set; }
    public string? Description { get; set; }
    public string? Brand { get; set; }
    public string? Category { get; set; }
    [JsonConverter(typeof(MoneyJsonConverter))]
    public decimal Price { get; set; }
}
