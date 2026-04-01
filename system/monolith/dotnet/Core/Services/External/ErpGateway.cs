using System.Net;
using System.Text.Json;
using Optivem.Starter.Monolith.Core.Dtos.External;

namespace Optivem.Starter.Monolith.Core.Services.External;

public class ErpGateway
{
    private readonly HttpClient _httpClient;
    private readonly string _erpUrl;
    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        PropertyNameCaseInsensitive = true
    };

    public ErpGateway(HttpClient httpClient, IConfiguration configuration)
    {
        _httpClient = httpClient;
        _erpUrl = configuration["Erp:Url"] ?? "http://localhost:9001/erp";
    }

    public async Task<ProductDetailsResponse?> GetProductDetailsAsync(string sku)
    {
        var url = $"{_erpUrl}/api/products/{sku}";

        try
        {
            var response = await _httpClient.GetAsync(url);

            if (response.StatusCode == HttpStatusCode.NotFound)
            {
                return null;
            }

            if (!response.IsSuccessStatusCode)
            {
                var body = await response.Content.ReadAsStringAsync();
                throw new Exception($"ERP API returned status {(int)response.StatusCode} for SKU: {sku}. URL: {url}. Response: {body}");
            }

            var content = await response.Content.ReadAsStringAsync();
            return JsonSerializer.Deserialize<ProductDetailsResponse>(content, JsonOptions);
        }
        catch (HttpRequestException e)
        {
            throw new Exception($"Failed to fetch product details for SKU: {sku} from URL: {url}. Error: {e.GetType().Name}: {e.Message}", e);
        }
    }
}
