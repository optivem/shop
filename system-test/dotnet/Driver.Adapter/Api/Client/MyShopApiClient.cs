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
    private readonly ProfileController _profileController;
    private readonly ProductController _productController;
    private readonly KeycloakTokenProvider? _tokenProvider;
    private volatile ApiIdentity _identity = ApiIdentity.Customer;
    private volatile TestUser _customer = TestUser.Customer;
    private bool _disposed;

    /// <param name="keycloakBaseUrl">When null or blank, no token is acquired and no Authorization header is sent.</param>
    public MyShopApiClient(string baseUrl, string? keycloakBaseUrl = null)
    {
        _httpClient = new JsonHttpClient<ProblemDetailResponse>(baseUrl);
        _healthController = new HealthController(_httpClient);
        _orderController = new OrderController(_httpClient);
        _couponController = new CouponController(_httpClient);
        _profileController = new ProfileController(_httpClient);
        _productController = new ProductController(_httpClient);
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

    private async Task<string?> TokenForAsync()
    {
        return _identity switch
        {
            ApiIdentity.Anonymous => null,
            ApiIdentity.Admin => await _tokenProvider!.GetTokenAsync(TestUser.Admin),
            _ => await _tokenProvider!.GetTokenAsync(_customer)
        };
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

    public ProfileController Profile() => _profileController;

    public ProductController Products() => _productController;
}
