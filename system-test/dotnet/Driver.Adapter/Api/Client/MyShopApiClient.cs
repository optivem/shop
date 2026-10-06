using Driver.Adapter.Shared.Client.Http;
using Driver.Adapter.Api.Client.Dtos.Errors;
using Driver.Adapter.Api.Client.Controllers;

namespace Driver.Adapter.Api.Client;

public class MyShopApiClient : IDisposable
{
    private readonly JsonHttpClient<ProblemDetailResponse> _httpClient;
    private readonly HealthController _healthController;
    private readonly OrderController _orderController;
    private readonly CouponController _couponController;
    private readonly KeycloakTokenProvider? _tokenProvider;
    private volatile ApiIdentity _identity = ApiIdentity.Default;
    private volatile TestUser _customer = TestUser.Customer;
    private bool _disposed;

    /// <param name="keycloakBaseUrl">When null or blank, no token is acquired and no Authorization header is sent.</param>
    public MyShopApiClient(string baseUrl, string? keycloakBaseUrl = null)
    {
        _httpClient = new JsonHttpClient<ProblemDetailResponse>(baseUrl);
        _healthController = new HealthController(_httpClient);
        _orderController = new OrderController(_httpClient);
        _couponController = new CouponController(_httpClient);
        if (!string.IsNullOrWhiteSpace(keycloakBaseUrl))
        {
            _tokenProvider = KeycloakTokenProvider.ForBaseUrl(keycloakBaseUrl);
            _httpClient.SetBearerTokenSource(TokenForAsync);
        }
    }

    public MyShopApiClient As(ApiIdentity identity)
    {
        _identity = identity;
        return this;
    }

    /// <summary>Calls are made as the given customer, until changed again.</summary>
    public MyShopApiClient AsCustomer(TestUser customer)
    {
        _identity = ApiIdentity.Customer;
        _customer = customer;
        return this;
    }

    private async Task<string?> TokenForAsync(string method, string path)
    {
        return _identity switch
        {
            ApiIdentity.Anonymous => null,
            ApiIdentity.Customer => await _tokenProvider!.GetTokenAsync(_customer),
            ApiIdentity.Admin => await _tokenProvider!.GetTokenAsync(TestUser.Admin),
            _ => await _tokenProvider!.GetTokenAsync(IsAdminOnly(method, path) ? TestUser.Admin : TestUser.Customer)
        };
    }

    private static bool IsAdminOnly(string method, string path)
    {
        if (path.StartsWith("/api/admin/", StringComparison.Ordinal))
            return true;
        if (path == "/api/coupons" && (method == "POST" || method == "GET"))
            return true;
        return method == "POST"
            && path.StartsWith("/api/orders/", StringComparison.Ordinal)
            && path.EndsWith("/deliver", StringComparison.Ordinal);
    }

    public void Dispose()
    {
        Dispose(true);
        GC.SuppressFinalize(this);
    }

    protected virtual void Dispose(bool disposing)
    {
        if (_disposed) return;
        if (disposing)
            _httpClient?.Dispose();
        _disposed = true;
    }

    public HealthController Health() => _healthController;

    public OrderController Orders() => _orderController;

    public CouponController Coupons() => _couponController;
}
