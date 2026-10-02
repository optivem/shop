using SystemTests.TestInfrastructure.Configuration;
using Dsl.Core;
using Driver.Port.External.Erp;
using Driver.Port;
using Driver.Adapter.External.Erp;
using Driver.Adapter.Api;
using Driver.Adapter.Ui;
using Driver.Adapter.External.Tax;
using Xunit;

namespace SystemTests.Legacy.Mod05.Base;

public abstract class BaseDriverTest : BaseConfigurableTest, IAsyncLifetime
{
    protected readonly Dsl.Core.Configuration _configuration;
    protected IMyShopDriver? _shopDriver;
    protected ErpRealDriver? _erpDriver;
    protected TaxRealDriver? _taxDriver;

    protected BaseDriverTest()
    {
        _configuration = LoadConfiguration();
    }

    public virtual Task InitializeAsync()
    {
        return Task.CompletedTask;
    }

    protected void SetUpMyShopApiDriver()
    {
        _shopDriver = new MyShopApiDriver(_configuration.MyShopApiBaseUrl, _configuration.KeycloakBaseUrl);
    }

    protected async Task SetUpMyShopUiDriverAsync()
    {
        _shopDriver = await MyShopUiDriver.CreateAsync(_configuration.MyShopUiBaseUrl, _configuration.KeycloakBaseUrl);
    }

    protected void SetUpExternalDrivers()
    {
        _erpDriver = new ErpRealDriver(_configuration.ErpBaseUrl);
        _taxDriver = new TaxRealDriver(_configuration.TaxBaseUrl);
    }

    public virtual async Task DisposeAsync()
    {
        if (_shopDriver != null)
            await _shopDriver.DisposeAsync();
        _erpDriver?.Dispose();
        _taxDriver?.Dispose();
    }
}













