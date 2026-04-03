using SystemTests.TestInfrastructure.Configuration;
using Dsl.Core;
using Driver.Adapter.External.Erp.Client;
using Driver.Adapter.Shop.Api.Client;
using Driver.Adapter.Shop.Ui.Client;
using Xunit;

namespace SystemTests.Legacy.Mod04.Base;

public abstract class BaseClientTest : BaseConfigurableTest, IAsyncLifetime
{
    protected readonly Dsl.Core.Configuration _configuration;

    protected ShopUiClient? _shopUiClient;
    protected ShopApiClient? _shopApiClient;

    protected ErpRealClient? _erpClient;

    protected BaseClientTest()
    {
        _configuration = LoadConfiguration();
    }

    public virtual async Task InitializeAsync()
    {
        await TestLock.WaitAsync();
    }

    protected async Task SetUpShopUiClientAsync()
    {
        _shopUiClient = await ShopUiClient.CreateAsync(_configuration.ShopUiBaseUrl);
    }

    protected void SetUpShopApiClient()
    {
        _shopApiClient = new ShopApiClient(_configuration.ShopApiBaseUrl);
    }

    protected void SetUpExternalClients()
    {
        _erpClient = new ErpRealClient(_configuration.ErpBaseUrl);
    }

    public virtual async Task DisposeAsync()
    {
        try
        {
            if (_shopUiClient != null)
                await _shopUiClient.DisposeAsync();
            _shopApiClient?.Dispose();
            _erpClient?.Dispose();
        }
        finally
        {
            TestLock.Release();
        }
    }
}











