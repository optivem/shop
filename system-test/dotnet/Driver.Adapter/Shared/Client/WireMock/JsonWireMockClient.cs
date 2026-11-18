using System.Net;
using System.Text.Json;
using System.Text.Json.Serialization;
using Common;

namespace Driver.Adapter.Shared.Client.WireMock;

public class JsonWireMockClient : IDisposable
{
    private const string ContentType = "Content-Type";
    private const string ApplicationJson = "application/json";
    private const int OutagePriority = 0;

    // Admin calls to a Cloud Run stub service occasionally fail in Google's front end before they
    // reach the container (a 502, or a request that reads out). Retry those a bounded number of times.
    private const int MaxAdminAttempts = 4;
    private static readonly TimeSpan InitialBackoff = TimeSpan.FromSeconds(1);
    private static readonly TimeSpan AdminRequestTimeout = TimeSpan.FromSeconds(30);

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
            Timeout = AdminRequestTimeout,
        };

        _jsonOptions = jsonOptions;
    }

    private string MappingsUrl => $"{_wireMockBaseUrl}/__admin/mappings";

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
            var id = Guid.NewGuid().ToString();
            var mappingRequest = new
            {
                id,
                priority = OutagePriority,
                request = new
                {
                    method = "GET",
                    urlPathPattern = pathPattern
                },
                response
            };

            var registered = await RegisterMappingAsync($"GET {pathPattern}", id, mappingRequest);
            if (registered.IsSuccess)
            {
                if (!_matchingStubIds.TryGetValue(pathPattern, out var ids))
                {
                    ids = new List<string>();
                    _matchingStubIds[pathPattern] = ids;
                }
                ids.Add(id);
            }
            return registered;
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
                var apiResponse = await _httpClient.DeleteAsync($"{MappingsUrl}/{id}");
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
            await _httpClient.DeleteAsync($"{MappingsUrl}/{id}");
        }
        _stubIds.Clear();
        _matchingStubIds.Clear();
    }

    /// <summary>
    /// Registers the mapping under its client-chosen <paramref name="id"/>, retrying transient admin failures.
    /// Before each retry it checks whether the failed attempt reached the server after all, so a mapping
    /// is never added twice. Then records the id for removal and reads the stub back, so a stub the server
    /// did not keep fails here, at registration.
    /// </summary>
    private async Task<Result<VoidValue, string>> RegisterMappingAsync(string description, string id, object mapping)
    {
        var requestJson = JsonSerializer.Serialize(mapping, _jsonOptions);
        var mappingUrl = $"{MappingsUrl}/{id}";

        var attempt = 0;
        using var apiResponse = await SendWithRetryAsync($"register stub {description} (id {id})", MappingsUrl, async () =>
        {
            attempt++;
            if (attempt > 1)
            {
                var check = await _httpClient.GetAsync(mappingUrl);
                if (check.IsSuccessStatusCode || IsTransient(check.StatusCode))
                {
                    return check;
                }
                check.Dispose();
            }
            using var content = new StringContent(requestJson, System.Text.Encoding.UTF8, ApplicationJson);
            return await _httpClient.PostAsync(MappingsUrl, content);
        });

        if (!apiResponse.IsSuccessStatusCode)
        {
            var errorContent = await apiResponse.Content.ReadAsStringAsync();
            return Result<VoidValue, string>.Failure($"Failed to register stub for {description} at {MappingsUrl} ({(int)apiResponse.StatusCode} {apiResponse.ReasonPhrase}): {errorContent}");
        }
        _stubIds.Add(id);

        using var readBack = await SendWithRetryAsync($"read back stub {description} (id {id})", mappingUrl, () => _httpClient.GetAsync(mappingUrl));
        if (!readBack.IsSuccessStatusCode)
        {
            var readBackContent = await readBack.Content.ReadAsStringAsync();
            return Result<VoidValue, string>.Failure($"Failed to register stub for {description}: stub {id} is not visible on read-back at {mappingUrl} ({(int)readBack.StatusCode} {readBack.ReasonPhrase}): {readBackContent}");
        }

        return Result<VoidValue, string>.Success(VoidValue.Empty);
    }

    /// <summary>
    /// Sends an admin request, retrying with exponential backoff on a 502/503/504 or a transport failure
    /// (connect/read timeout, reset connection). Returns the last response; throws once the retries run out
    /// on a transport failure.
    /// </summary>
    private static async Task<HttpResponseMessage> SendWithRetryAsync(string description, string url, Func<Task<HttpResponseMessage>> send)
    {
        for (var attempt = 1; ; attempt++)
        {
            if (attempt > 1)
            {
                await Task.Delay(InitialBackoff * Math.Pow(2, attempt - 2));
            }

            string failure;
            try
            {
                var response = await send();
                if (!IsTransient(response.StatusCode) || attempt == MaxAdminAttempts)
                {
                    return response;
                }
                failure = $"{(int)response.StatusCode} {response.ReasonPhrase}";
                response.Dispose();
            }
            catch (Exception ex) when (IsTransient(ex))
            {
                if (attempt == MaxAdminAttempts)
                {
                    throw new InvalidOperationException($"WireMock admin call failed after {MaxAdminAttempts} attempts: {description} at {url}: {ex.Message}", ex);
                }
                failure = ex.Message;
            }

            await Console.Error.WriteLineAsync($"WireMock admin call failed (attempt {attempt}/{MaxAdminAttempts}): {description} at {url}: {failure}");
        }
    }

    private static bool IsTransient(HttpStatusCode statusCode)
        => statusCode is HttpStatusCode.BadGateway or HttpStatusCode.ServiceUnavailable or HttpStatusCode.GatewayTimeout;

    // HttpClient reports its own timeout as a TaskCanceledException (no caller token is passed here).
    private static bool IsTransient(Exception ex)
        => ex is HttpRequestException or TaskCanceledException or IOException;

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

            var id = Guid.NewGuid().ToString();
            var mappingRequest = new
            {
                id,
                priority = 1,
                request = new
                {
                    method = method,
                    urlPath = path
                },
                response = response
            };

            return await RegisterMappingAsync($"{method} {path}", id, mappingRequest);
        }
        catch (Exception ex)
        {
            return Result<VoidValue, string>.Failure($"Failed to configure {method} stub for {path}: {ex.Message}");
        }
    }

}
