using System.Net;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using System.Text.Json.Serialization;
using Common;

namespace Driver.Adapter.Shared.Client.Http;

public class JsonHttpClient<E> : IDisposable
{
    private const string ApplicationJson = "application/json";

    private readonly JsonSerializerOptions _jsonOptions = new()
    {
        PropertyNameCaseInsensitive = true,
        PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
        NumberHandling = JsonNumberHandling.AllowReadingFromString,
        DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull,
        Converters = { new JsonStringEnumConverter(JsonNamingPolicy.CamelCase, allowIntegerValues: false) }
    };

    private readonly HttpClient _httpClient;
    private readonly string _baseUrl;
    private Func<string, string, Task<string?>>? _bearerTokenSource;
    private bool _disposed;

    public JsonHttpClient(string baseUrl)
    {
        _httpClient = new HttpClient { BaseAddress = new Uri(baseUrl) };
        _baseUrl = baseUrl;
    }

    /// <summary>
    /// Supplies the access token for each request (method, path), or null to send it without an
    /// Authorization header.
    /// </summary>
    public void SetBearerTokenSource(Func<string, string, Task<string?>> bearerTokenSource)
    {
        _bearerTokenSource = bearerTokenSource;
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

    public async Task<Result<T, E>> GetAsync<T>(string path)
        => await GetResultOrFailureAsync<T>(await DoGetAsync(path));

    public async Task<Result<VoidValue, E>> GetAsync(string path)
        => await GetResultOrFailureAsync<VoidValue>(await DoGetAsync(path));

    public async Task<Result<T, E>> PostAsync<T>(string path, object request)
        => await GetResultOrFailureAsync<T>(await DoPostAsync(path, request));

    public async Task<Result<VoidValue, E>> PostAsync(string path, object request)
        => await GetResultOrFailureAsync<VoidValue>(await DoPostAsync(path, request));

    public async Task<Result<VoidValue, E>> PostAsync(string path)
        => await GetResultOrFailureAsync<VoidValue>(await DoPostAsync(path));

    public async Task<Result<T, E>> PutAsync<T>(string path, object request)
        => await GetResultOrFailureAsync<T>(await DoPutAsync(path, request));

    public async Task<Result<VoidValue, E>> PutAsync(string path, object request)
        => await GetResultOrFailureAsync<VoidValue>(await DoPutAsync(path, request));

    public async Task<Result<T, E>> DeleteAsync<T>(string path)
        => await GetResultOrFailureAsync<T>(await DoDeleteAsync(path));

    public async Task<Result<VoidValue, E>> DeleteAsync(string path)
        => await GetResultOrFailureAsync<VoidValue>(await DoDeleteAsync(path));

    private async Task<HttpResponseMessage> DoGetAsync(string path)
    {
        var uri = GetUri(path);
        var httpRequest = new HttpRequestMessage(HttpMethod.Get, uri);
        return await SendRequest(httpRequest);
    }

    #region Helpers

    private Uri GetUri(string path)
    {
        return new Uri(_baseUrl + path);
    }

    private async Task<HttpResponseMessage> DoPostAsync(string path, object request)
    {
        var uri = GetUri(path);
        var jsonBody = SerializeRequest(request);
        var httpRequest = new HttpRequestMessage(HttpMethod.Post, uri)
        {
            Content = new StringContent(jsonBody, Encoding.UTF8, ApplicationJson)
        };
        return await SendRequest(httpRequest);
    }

    private async Task<HttpResponseMessage> DoPostAsync(string path)
    {
        var uri = GetUri(path);
        var httpRequest = new HttpRequestMessage(HttpMethod.Post, uri)
        {
            Content = new StringContent(string.Empty, Encoding.UTF8, ApplicationJson)
        };
        return await SendRequest(httpRequest);
    }

    private async Task<HttpResponseMessage> DoPutAsync(string path, object request)
    {
        var uri = GetUri(path);
        var jsonBody = SerializeRequest(request);
        var httpRequest = new HttpRequestMessage(HttpMethod.Put, uri)
        {
            Content = new StringContent(jsonBody, Encoding.UTF8, ApplicationJson)
        };
        return await SendRequest(httpRequest);
    }

    private async Task<HttpResponseMessage> DoDeleteAsync(string path)
    {
        var uri = GetUri(path);
        var httpRequest = new HttpRequestMessage(HttpMethod.Delete, uri);
        return await SendRequest(httpRequest);
    }

    private async Task<HttpResponseMessage> SendRequest(HttpRequestMessage httpRequest)
    {
        var tokenSource = _bearerTokenSource;
        if (tokenSource != null)
        {
            var token = await tokenSource(httpRequest.Method.Method, httpRequest.RequestUri!.AbsolutePath);
            if (token != null)
            {
                httpRequest.Headers.Authorization = new AuthenticationHeaderValue("Bearer", token);
            }
        }

        return await _httpClient.SendAsync(httpRequest);
    }

    private string SerializeRequest(object request)
    {
        return JsonSerializer.Serialize(request, _jsonOptions);
    }

    // A rejection without a body (for example a bare 401) still has to surface its status.
    private static E CreateEmptyBodyError(int statusCode)
    {
        var instance = Activator.CreateInstance<E>();
        var statusProperty = typeof(E).GetProperty("Status");
        if (statusProperty != null && (statusProperty.PropertyType == typeof(int) || statusProperty.PropertyType == typeof(int?)))
        {
            statusProperty.SetValue(instance, statusCode);
        }
        return instance;
    }

    private static async Task<T> ReadResponseAsync<T>(HttpResponseMessage httpResponse, JsonSerializerOptions jsonOptions)
    {
        var responseBody = await httpResponse.Content.ReadAsStringAsync();
        return JsonSerializer.Deserialize<T>(responseBody, jsonOptions)!;
    }

    private async Task<Result<T, E>> GetResultOrFailureAsync<T>(HttpResponseMessage httpResponse)
    {
        if (!httpResponse.IsSuccessStatusCode)
        {
            var errorBody = await httpResponse.Content.ReadAsStringAsync();
            var error = string.IsNullOrWhiteSpace(errorBody)
                ? CreateEmptyBodyError((int)httpResponse.StatusCode)
                : JsonSerializer.Deserialize<E>(errorBody, _jsonOptions)!;
            return Result<T, E>.Failure(error);
        }

        if (typeof(T) == typeof(VoidValue) || httpResponse.StatusCode == HttpStatusCode.NoContent)
        {
            return Result<T, E>.Success(default!);
        }

        var response = await ReadResponseAsync<T>(httpResponse, _jsonOptions);
        return Result<T, E>.Success(response);
    }

    #endregion
}
