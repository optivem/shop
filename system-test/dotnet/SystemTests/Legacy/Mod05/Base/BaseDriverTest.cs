using SystemTests.TestInfrastructure.Configuration;
using Dsl.Core;
using Driver.Port.External.Erp;
using Driver.Port.Shop;
using Driver.Adapter.External.Erp;
using Driver.Adapter.Shop.Api;
using Driver.Adapter.Shop.Ui;
using Xunit;

namespace SystemTests.Legacy.Mod05.Base;

public abstract class BaseDriverTest : BaseConfigurableTest, IAsyncLifetime
{
    protected readonly Dsl.Core.Configuration _configuration;
    protected IShopDriver? _shopDriver;
    protected ErpRealDriver? _erpDriver;

    protected BaseDriverTest()
    {
        _configuration = LoadConfiguration();
    }

    public virtual async Task InitializeAsync()
    {
        await TestLock.WaitAsync();
    }

    protected void SetUpShopApiDriver()
    {
        _shopDriver = new ShopApiDriver(_configuration.ShopApiBaseUrl);
    }

    protected async Task SetUpShopUiDriverAsync()
    {
        _shopDriver = await ShopUiDriver.CreateAsync(_configuration.ShopUiBaseUrl);
    }

    protected void SetUpExternalDrivers()
    {
        _erpDriver = new ErpRealDriver(_configuration.ErpBaseUrl);
    }

    public virtual async Task DisposeAsync()
    {
        try
        {
            if (_shopDriver != null)
                await _shopDriver.DisposeAsync();
            _erpDriver?.Dispose();
        }
        finally
        {
            TestLock.Release();
        }
    }
}













