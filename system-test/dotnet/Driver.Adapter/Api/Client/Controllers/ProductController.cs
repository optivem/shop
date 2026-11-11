using System.Globalization;
using Common;
using Driver.Adapter.Shared.Client.Http;
using Driver.Adapter.Api.Client.Dtos.Errors;
using Driver.Port.Dtos;

namespace Driver.Adapter.Api.Client.Controllers;

public class ProductController
{
    private const string Endpoint = "/api/products";

    private readonly JsonHttpClient<ProblemDetailResponse> _httpClient;

    public ProductController(JsonHttpClient<ProblemDetailResponse> httpClient)
    {
        _httpClient = httpClient;
    }

    public Task<Result<List<BrowseCatalogResponse.ProductDto>, ProblemDetailResponse>> BrowseProductsAsync()
        => _httpClient.GetAsync<List<ProductJson>>(Endpoint)
            .MapAsync(products => products.Select(ToProductDto).ToList());

    public Task<Result<BrowseCatalogResponse.ProductDto, ProblemDetailResponse>> ViewProductAsync(string sku)
        => _httpClient.GetAsync<ProductJson>($"{Endpoint}/{Uri.EscapeDataString(sku)}")
            .MapAsync(ToProductDto);

    private static BrowseCatalogResponse.ProductDto ToProductDto(ProductJson product) => new()
    {
        Sku = product.Sku,
        Title = product.Title,
        Brand = product.Brand,
        Category = product.Category,
        Description = product.Description,
        Price = product.Price?.ToString(CultureInfo.InvariantCulture),
    };

    /// <summary>The API's product shape: the price is a JSON number.</summary>
    private sealed class ProductJson
    {
        public string? Sku { get; set; }
        public string? Title { get; set; }
        public string? Brand { get; set; }
        public string? Category { get; set; }
        public string? Description { get; set; }
        public decimal? Price { get; set; }
    }
}
