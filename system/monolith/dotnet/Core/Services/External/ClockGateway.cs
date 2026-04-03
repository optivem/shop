using System.Text.Json;
using Optivem.Shop.Monolith.Core.Dtos.External;

namespace Optivem.Shop.Monolith.Core.Services.External;

public class ClockGateway
{
    private readonly HttpClient _httpClient;
    private readonly string _externalSystemMode;
    private readonly string _clockUrl;
    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        PropertyNameCaseInsensitive = true
    };

    public ClockGateway(HttpClient httpClient, IConfiguration configuration)
    {
        _httpClient = httpClient;
        _externalSystemMode = Environment.GetEnvironmentVariable("EXTERNAL_SYSTEM_MODE") ?? configuration["External:SystemMode"] ?? "real";
        _clockUrl = Environment.GetEnvironmentVariable("CLOCK_API_URL") ?? configuration["Clock:Url"] ?? "http://localhost:9001/clock";
    }

    public async Task<DateTime> GetCurrentTimeAsync()
    {
        if (_externalSystemMode == "real")
        {
            return DateTime.UtcNow;
        }
        else if (_externalSystemMode == "stub")
        {
            return await GetStubTimeAsync();
        }
        else
        {
            throw new InvalidOperationException($"Unknown external system mode: {_externalSystemMode}");
        }
    }

    private async Task<DateTime> GetStubTimeAsync()
    {
        try
        {
            var url = $"{_clockUrl}/api/time";
            var response = await _httpClient.GetAsync(url);

            if (!response.IsSuccessStatusCode)
            {
                var body = await response.Content.ReadAsStringAsync();
                throw new InvalidOperationException($"Clock API returned status {(int)response.StatusCode}. URL: {url}. Response: {body}");
            }

            var content = await response.Content.ReadAsStringAsync();
            var clockResponse = JsonSerializer.Deserialize<GetTimeResponse>(content, JsonOptions);
            return DateTime.SpecifyKind(clockResponse!.Time, DateTimeKind.Utc);
        }
        catch (HttpRequestException e)
        {
            throw new InvalidOperationException($"Failed to fetch current time from URL: {_clockUrl}. Error: {e.GetType().Name}: {e.Message}", e);
        }
    }
}
