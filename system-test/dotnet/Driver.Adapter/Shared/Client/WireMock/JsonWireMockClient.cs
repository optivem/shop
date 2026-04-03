using System.Text.Json;
using System.Text.Json.Serialization;
using Common;

namespace Driver.Adapter.Shared.Client.WireMock;

public class JsonWireMockClient : IDisposable
{
    private const string ContentType = "Content-Type";
    private const string ApplicationJson = "application/json";

    private readonly HttpClient _httpClient;
    private readonly string _wireMockBaseUrl;
    private readonly JsonSerializerOptions _jsonOptions;
    private readonly List<string> _stubIds = new();
    private bool _disposed;

    public JsonWireMockClient(string baseUrl)
        : this(baseUrl, CreateDefaultJsonOptions())
    {
    }

    public void Dispose()
    {
        Dispose(true);
        GC.SuppressFinalize(this);
    }

    protected virtual void Dispose(bool disposing)
    {
        if (!_disposed)
        {
            if (disposing)
            {
                _httpClient?.Dispose();
            }
            _disposed = true;
        }
    }

    private JsonWireMockClient(string baseUrl, JsonSerializerOptions jsonOptions)
    {
        var uri = new Uri(baseUrl);
        _wireMockBaseUrl = $"http://{uri.Host}:{uri.Port}";
        _httpClient = new HttpClient()
        {
            BaseAddress = uri,
        };

        _jsonOptions = jsonOptions;
    }

    private static JsonSerializerOptions CreateDefaultJsonOptions()
    {
        var options = new JsonSerializerOptions
        {
            PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
            DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull
        };
        return options;
    }

    public async Task<Result<VoidValue, string>> StubGetAsync<T>(string path, int statusCode, T response)
        => await RegisterStubAsync("GET", path, statusCode, Serialize(response));

    public async Task<Result<VoidValue, string>> StubGetAsync(string path, int statusCode)
        => await RegisterStubAsync("GET", path, statusCode, null);

    public async Task<Result<VoidValue, string>> StubPostAsync<T>(string path, int statusCode, T response)
        => await RegisterStubAsync("POST", path, statusCode, Serialize(response));

    public async Task<Result<VoidValue, string>> StubPostAsync(string path, int statusCode)
        => await RegisterStubAsync("POST", path, statusCode, null);

    public async Task<Result<VoidValue, string>> StubPutAsync<T>(string path, int statusCode, T response)
        => await RegisterStubAsync("PUT", path, statusCode, Serialize(response));

    public async Task<Result<VoidValue, string>> StubPutAsync(string path, int statusCode)
        => await RegisterStubAsync("PUT", path, statusCode, null);

    public async Task<Result<VoidValue, string>> StubDeleteAsync<T>(string path, int statusCode, T response)
        => await RegisterStubAsync("DELETE", path, statusCode, Serialize(response));

    public async Task<Result<VoidValue, string>> StubDeleteAsync(string path, int statusCode)
        => await RegisterStubAsync("DELETE", path, statusCode, null);

    public async Task RemoveStubsAsync()
    {
        foreach (var id in _stubIds)
        {
            await _httpClient.DeleteAsync($"{_wireMockBaseUrl}/__admin/mappings/{id}");
        }
        _stubIds.Clear();
    }

    private async Task<Result<VoidValue, string>> RegisterStubAsync(string method, string path, int statusCode, string? responseBody)
    {
        try
        {
            var response = new
            {
                status = statusCode,
                headers = new Dictionary<string, string>
                {
                    { ContentType, ApplicationJson }
                },
                body = responseBody
            };

            var mappingRequest = new
            {
                request = new
                {
                    method = method,
                    urlPath = path
                },
                response = response
            };

            var requestJson = JsonSerializer.Serialize(mappingRequest, _jsonOptions);
            var content = new StringContent(requestJson, System.Text.Encoding.UTF8, ApplicationJson);

            var apiResponse = await _httpClient.PostAsync($"{_wireMockBaseUrl}/__admin/mappings", content);

            if (apiResponse.IsSuccessStatusCode)
            {
                var responseContent = await apiResponse.Content.ReadAsStringAsync();
                var responseJson = JsonSerializer.Deserialize<JsonElement>(responseContent);
                if (responseJson.TryGetProperty("id", out var idElement))
                {
                    var id = idElement.GetString();
                    if (id != null)
                    {
                        _stubIds.Add(id);
                    }
                }
                return Result<VoidValue, string>.Success(VoidValue.Empty);
            }
            else
            {
                var errorContent = await apiResponse.Content.ReadAsStringAsync();
                return Result<VoidValue, string>.Failure($"Failed to register stub for {method} {path}: {errorContent}");
            }
        }
        catch (Exception ex)
        {
            return Result<VoidValue, string>.Failure($"Failed to configure {method} stub for {path}: {ex.Message}");
        }
    }

    private string Serialize<T>(T obj)
    {
        try
        {
            return JsonSerializer.Serialize(obj, _jsonOptions);
        }
        catch (Exception ex)
        {
            throw new InvalidOperationException("Failed to serialize object", ex);
        }
    }
}
