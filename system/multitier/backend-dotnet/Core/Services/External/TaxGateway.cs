using System.Net;
using System.Text.Json;
using Optivem.Shop.Backend.Core.Dtos.External;

namespace Optivem.Shop.Backend.Core.Services.External;

public class TaxGateway
{
    private readonly HttpClient _httpClient;
    private readonly string _taxUrl;
    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        PropertyNameCaseInsensitive = true
    };

    public TaxGateway(HttpClient httpClient, IConfiguration configuration)
    {
        _httpClient = httpClient;
        _taxUrl = Environment.GetEnvironmentVariable("TAX_API_URL") ?? configuration["Tax:Url"] ?? "http://localhost:9001/tax";
    }

    public async Task<TaxDetailsResponse?> GetTaxDetailsAsync(string country)
    {
        var url = $"{_taxUrl}/api/countries/{Uri.EscapeDataString(country)}";

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
                throw new InvalidOperationException($"Tax API returned status {(int)response.StatusCode} for country: {country}. URL: {url}. Response: {body}");
            }

            var content = await response.Content.ReadAsStringAsync();
            return JsonSerializer.Deserialize<TaxDetailsResponse>(content, JsonOptions);
        }
        catch (HttpRequestException e)
        {
            throw new InvalidOperationException($"Failed to fetch tax details for country: {country} from URL: {url}. Error: {e.GetType().Name}: {e.Message}", e);
        }
    }
}
