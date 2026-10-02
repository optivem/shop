using SystemTests.TestInfrastructure.Configuration;
using Dsl.Core;
using Driver.Adapter.External.Erp.Client;
using Driver.Adapter.Api.Client;
using Driver.Adapter.Ui.Client;
using Driver.Adapter.External.Tax.Client;
using Xunit;

namespace SystemTests.Legacy.Mod04.Base;

public abstract class BaseClientTest : BaseConfigurableTest, IAsyncLifetime
{
    protected readonly Dsl.Core.Configuration _configuration;

    protected MyShopUiClient? _shopUiClient;
    protected MyShopApiClient? _shopApiClient;

    protected ErpRealClient? _erpClient;
    protected TaxRealClient? _taxClient;

    protected BaseClientTest()
    {
        _configuration = LoadConfiguration();
    }

    public virtual Task InitializeAsync()
    {
        return Task.CompletedTask;
    }

    protected async Task SetUpMyShopUiClientAsync()
    {
        _shopUiClient = await MyShopUiClient.CreateAsync(_configuration.MyShopUiBaseUrl, _configuration.KeycloakBaseUrl);
    }

    protected void SetUpMyShopApiClient()
    {
        _shopApiClient = new MyShopApiClient(_configuration.MyShopApiBaseUrl, _configuration.KeycloakBaseUrl);
    }

    protected void SetUpExternalClients()
    {
        _erpClient = new ErpRealClient(_configuration.ErpBaseUrl);
        _taxClient = new TaxRealClient(_configuration.TaxBaseUrl);
    }

    public virtual async Task DisposeAsync()
    {
        if (_shopUiClient != null)
            await _shopUiClient.DisposeAsync();
        _shopApiClient?.Dispose();
        _erpClient?.Dispose();
        _taxClient?.Dispose();
    }
}











