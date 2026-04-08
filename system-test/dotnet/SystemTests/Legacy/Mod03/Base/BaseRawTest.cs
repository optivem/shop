using System.Text.Json;
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

    protected async Task SetUpShopBrowserAsync()
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

    protected void SetUpShopHttpClient()
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

    private JsonSerializerOptions CreateObjectMapper()
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











