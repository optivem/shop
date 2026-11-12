using System.Text.Json;
using System.Text.Json.Serialization;
using Common;

namespace Driver.Adapter.Shared.Client.WireMock;

public class JsonWireMockClient : IDisposable
{
    private const string ContentType = "Content-Type";
    private const string ApplicationJson = "application/json";
    private const int OutagePriority = 0;

    private readonly HttpClient _httpClient;
    private readonly string _wireMockBaseUrl;
    private readonly JsonSerializerOptions _jsonOptions;
    private readonly List<string> _stubIds = new();
    private readonly Dictionary<string, List<string>> _matchingStubIds = new();
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
        // Honour the base URL's scheme and default port: in the cloud the stubs are an HTTPS
        // Cloud Run service with no explicit port (e.g. https://<svc>.run.app/erp). Authority
        // omits the default port, matching what a browser/curl sends to the front end. Only the
        // origin is used: the admin API lives at /__admin on the root, whatever stub prefix.
        _wireMockBaseUrl = $"{uri.Scheme}://{uri.Authority}";
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
        => await RegisterStubAsync("GET", path, statusCode, response);

    public async Task<Result<VoidValue, string>> StubGetAsync(string path, int statusCode)
        => await RegisterStubAsync<object>("GET", path, statusCode, null);

    public async Task<Result<VoidValue, string>> StubPostAsync<T>(string path, int statusCode, T response)
        => await RegisterStubAsync("POST", path, statusCode, response);

    public async Task<Result<VoidValue, string>> StubPostAsync(string path, int statusCode)
        => await RegisterStubAsync<object>("POST", path, statusCode, null);

    public async Task<Result<VoidValue, string>> StubPutAsync<T>(string path, int statusCode, T response)
        => await RegisterStubAsync("PUT", path, statusCode, response);

    public async Task<Result<VoidValue, string>> StubPutAsync(string path, int statusCode)
        => await RegisterStubAsync<object>("PUT", path, statusCode, null);

    public async Task<Result<VoidValue, string>> StubDeleteAsync<T>(string path, int statusCode, T response)
        => await RegisterStubAsync("DELETE", path, statusCode, response);

    public async Task<Result<VoidValue, string>> StubDeleteAsync(string path, int statusCode)
        => await RegisterStubAsync<object>("DELETE", path, statusCode, null);

    /// <summary>
    /// Stubs every GET whose path matches <paramref name="pathPattern"/> with a bodyless
    /// <paramref name="statusCode"/>, at priority 0 so it outranks the priority-1 stubs this client
    /// registers. Used to take a whole external system down.
    /// </summary>
    public async Task<Result<VoidValue, string>> StubGetMatchingAsync(string pathPattern, int statusCode, int fixedDelayMilliseconds = 0)
    {
        try
        {
            // A delay makes the whole external system slow, so the system's outbound timeout is what ends the call.
            object response = fixedDelayMilliseconds > 0
                ? new { status = statusCode, fixedDelayMilliseconds }
                : new { status = statusCode };
            var mappingRequest = new
            {
                priority = OutagePriority,
                request = new
                {
                    method = "GET",
                    urlPathPattern = pathPattern
                },
                response
            };

            var requestJson = JsonSerializer.Serialize(mappingRequest, _jsonOptions);
            var content = new StringContent(requestJson, System.Text.Encoding.UTF8, ApplicationJson);

            var apiResponse = await _httpClient.PostAsync($"{_wireMockBaseUrl}/__admin/mappings", content);
            var responseContent = await apiResponse.Content.ReadAsStringAsync();

            if (!apiResponse.IsSuccessStatusCode)
            {
                return Result<VoidValue, string>.Failure($"Failed to register stub for GET {pathPattern}: {responseContent}");
            }

            var trackedBefore = _stubIds.Count;
            var tracked = await TrackAndVerifyAsync($"GET {pathPattern}", responseContent);
            if (_stubIds.Count > trackedBefore)
            {
                if (!_matchingStubIds.TryGetValue(pathPattern, out var ids))
                {
                    ids = new List<string>();
                    _matchingStubIds[pathPattern] = ids;
                }
                ids.Add(_stubIds[^1]);
            }
            return tracked;
        }
        catch (Exception ex)
        {
            return Result<VoidValue, string>.Failure($"Failed to configure GET stub for {pathPattern}: {ex.Message}");
        }
    }

    /// <summary>Removes the stubs this client registered with <see cref="StubGetMatchingAsync"/> for <paramref name="pathPattern"/>.</summary>
    public async Task<Result<VoidValue, string>> RemoveStubsMatchingAsync(string pathPattern)
    {
        if (!_matchingStubIds.TryGetValue(pathPattern, out var ids))
        {
            return Result<VoidValue, string>.Success(VoidValue.Empty);
        }

        try
        {
            foreach (var id in ids.ToList())
            {
                var apiResponse = await _httpClient.DeleteAsync($"{_wireMockBaseUrl}/__admin/mappings/{id}");
                if (!apiResponse.IsSuccessStatusCode)
                {
                    var errorContent = await apiResponse.Content.ReadAsStringAsync();
                    return Result<VoidValue, string>.Failure($"Failed to remove stub {id} for GET {pathPattern}: {errorContent}");
                }
                ids.Remove(id);
                _stubIds.Remove(id);
            }
            _matchingStubIds.Remove(pathPattern);
            return Result<VoidValue, string>.Success(VoidValue.Empty);
        }
        catch (Exception ex)
        {
            return Result<VoidValue, string>.Failure($"Failed to remove GET stubs for {pathPattern}: {ex.Message}");
        }
    }

    public async Task RemoveStubsAsync()
    {
        foreach (var id in _stubIds)
        {
            await _httpClient.DeleteAsync($"{_wireMockBaseUrl}/__admin/mappings/{id}");
        }
        _stubIds.Clear();
        _matchingStubIds.Clear();
    }

    /// <summary>
    /// Records the new stub's id for removal, then reads the stub back, so a stub the server did not
    /// keep fails here, at registration.
    /// </summary>
    private async Task<Result<VoidValue, string>> TrackAndVerifyAsync(string description, string registrationResponse)
    {
        var responseJson = JsonSerializer.Deserialize<JsonElement>(registrationResponse);
        if (!responseJson.TryGetProperty("id", out var idElement) || idElement.GetString() is not { } id)
        {
            return Result<VoidValue, string>.Failure($"Failed to register stub for {description}: WireMock returned no mapping id: {registrationResponse}");
        }
        _stubIds.Add(id);

        var readBack = await _httpClient.GetAsync($"{_wireMockBaseUrl}/__admin/mappings/{id}");
        if (!readBack.IsSuccessStatusCode)
        {
            var readBackContent = await readBack.Content.ReadAsStringAsync();
            return Result<VoidValue, string>.Failure($"Failed to register stub for {description}: stub {id} is not visible on read-back ({(int)readBack.StatusCode} {readBack.ReasonPhrase}): {readBackContent}");
        }

        return Result<VoidValue, string>.Success(VoidValue.Empty);
    }

    private async Task<Result<VoidValue, string>> RegisterStubAsync<T>(string method, string path, int statusCode, T? responseBody)
    {
        try
        {
            object response = responseBody != null
                ? new
                {
                    status = statusCode,
                    headers = new Dictionary<string, string>
                    {
                        { ContentType, ApplicationJson }
                    },
                    jsonBody = responseBody
                }
                : new
                {
                    status = statusCode,
                    headers = new Dictionary<string, string>
                    {
                        { ContentType, ApplicationJson }
                    }
                };

            var mappingRequest = new
            {
                priority = 1,
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
                return await TrackAndVerifyAsync($"{method} {path}", responseContent);
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

}
