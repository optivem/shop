using System.Net;
using System.Text.Json;
using MyCompany.MyShop.Backend.Core.Dtos.External;

namespace MyCompany.MyShop.Backend.Core.Services.External;

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
        _erpUrl = Environment.GetEnvironmentVariable("ERP_API_URL") ?? configuration["Erp:Url"] ?? "http://localhost:9001/erp";
    }

    public virtual async Task<ErpProductDetailsResponse?> GetProductDetailsAsync(string sku)
    {
        var url = $"{_erpUrl}/api/products/{Uri.EscapeDataString(sku)}";

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
                throw new InvalidOperationException($"ERP API returned status {(int)response.StatusCode} for SKU: {sku}. URL: {url}. Response: {body}");
            }

            var content = await response.Content.ReadAsStringAsync();
            return JsonSerializer.Deserialize<ErpProductDetailsResponse>(content, JsonOptions);
        }
        catch (HttpRequestException e)
        {
            throw new InvalidOperationException($"Failed to fetch product details for SKU: {sku} from URL: {url}. Error: {e.GetType().Name}: {e.Message}", e);
        }
    }

    public virtual async Task<ErpGetPromotionResponse> GetPromotionDetailsAsync()
    {
        var url = $"{_erpUrl}/api/promotion";

        try
        {
            var response = await _httpClient.GetAsync(url);

            if (!response.IsSuccessStatusCode)
            {
                var body = await response.Content.ReadAsStringAsync();
                throw new InvalidOperationException($"ERP API returned status {(int)response.StatusCode} for promotion. URL: {url}. Response: {body}");
            }

            var content = await response.Content.ReadAsStringAsync();
            return JsonSerializer.Deserialize<ErpGetPromotionResponse>(content, JsonOptions)!;
        }
        catch (HttpRequestException e)
        {
            throw new InvalidOperationException($"Failed to fetch promotion details from URL: {url}. Error: {e.GetType().Name}: {e.Message}", e);
        }
    }
}
