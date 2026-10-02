using System.Text.Json;
using System.Net.Http.Headers;
using Driver.Adapter.Shared.Client.Http;
using Driver.Adapter.Shared.Client.Playwright;
using Microsoft.Playwright;
using SystemTests.TestInfrastructure.Configuration;
using Dsl.Core;
using Xunit;

namespace SystemTests.Legacy.Mod03.Base;

public abstract class BaseRawTest : BaseConfigurableTest, IAsyncLifetime
{
    protected readonly Dsl.Core.Configuration _configuration;

    protected IPlaywright? shopUiPlaywright;
    protected IBrowser? shopUiBrowser;
    protected IBrowserContext? shopUiBrowserContext;
    protected IPage? shopUiPage;
    protected HttpClient? _shopApiHttpClient;

    protected HttpClient? _erpHttpClient;

    protected JsonSerializerOptions? _httpObjectMapper;

    protected BaseRawTest()
    {
        _configuration = LoadConfiguration();
    }

    public virtual Task InitializeAsync()
    {
        return Task.CompletedTask;
    }

    protected async Task SetUpMyShopBrowserAsync()
    {
        shopUiPlaywright = await Playwright.CreateAsync();

        var launchOptions = new BrowserTypeLaunchOptions
        {
            Headless = true
        };

        shopUiBrowser = await shopUiPlaywright.Chromium.LaunchAsync(launchOptions);

        var contextOptions = new BrowserNewContextOptions
        {
            ViewportSize = new ViewportSize { Width = 1920, Height = 1080 },
            StorageStatePath = null
        };

        shopUiBrowserContext = await shopUiBrowser.NewContextAsync(contextOptions);
        shopUiPage = await shopUiBrowserContext.NewPageAsync();
    }

    protected async Task<HttpRequestMessage> WithCustomerAuthAsync(HttpRequestMessage request)
    {
        var keycloakBaseUrl = _configuration.KeycloakBaseUrl;
        if (!string.IsNullOrWhiteSpace(keycloakBaseUrl))
        {
            var token = await KeycloakTokenProvider.ForBaseUrl(keycloakBaseUrl).GetTokenAsync(TestUser.Customer);
            request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", token);
        }
        return request;
    }

    protected async Task LoginToMyShopUiIfRequiredAsync()
    {
        var login = KeycloakUiLogin.ForBaseUrl(_configuration.KeycloakBaseUrl);
        if (login != null)
        {
            await login.EnsureLoggedInAsync(shopUiPage!, "a[href='/new-order']");
        }
    }

    protected void SetUpMyShopHttpClient()
    {
        _shopApiHttpClient = new HttpClient();
        if (_httpObjectMapper == null)
        {
            _httpObjectMapper = CreateObjectMapper();
        }
    }

    protected void SetUpExternalHttpClients()
    {
        _erpHttpClient = new HttpClient();
        _httpObjectMapper = CreateObjectMapper();
    }

    private static JsonSerializerOptions CreateObjectMapper()
    {
        return new JsonSerializerOptions
        {
            PropertyNameCaseInsensitive = true
        };
    }

    public virtual async Task DisposeAsync()
    {
        if (shopUiPage != null)
            await shopUiPage.CloseAsync();
        if (shopUiBrowserContext != null)
            await shopUiBrowserContext.CloseAsync();
        if (shopUiBrowser != null)
            await shopUiBrowser.CloseAsync();
        shopUiPlaywright?.Dispose();

        _shopApiHttpClient?.Dispose();
        _erpHttpClient?.Dispose();
    }
}











