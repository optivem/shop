using SystemTests.TestInfrastructure.Configuration;
using Dsl.Core;
using Driver.Port.External.Erp;
using Driver.Adapter.Shop.Api;
using Driver.Adapter.Shop.Ui;
using Dsl.Core.Shop;
using Driver.Port.Shop;
using Driver.Adapter.External.Erp;
using Optivem.Testing;
using Xunit;

namespace SystemTests.Legacy.Mod06.Base;


public abstract class BaseChannelDriverTest : BaseConfigurableTest, IAsyncLifetime
{
    protected IShopDriver? _shopDriver;
    protected ErpRealDriver? _erpDriver;

    public virtual async Task InitializeAsync()
    {
        await TestLock.WaitAsync();

        var configuration = LoadConfiguration();

        // Only create shop driver if channel context is set (for channel-parameterized tests)
        // For non-channel tests (like Erp), skip shop driver creation
        try
        {
            _shopDriver = await CreateShopDriverAsync(configuration);
        }
        catch (InvalidOperationException ex) when (ex.Message.Contains("Channel type is not set"))
        {
            _shopDriver = null;
        }

        _erpDriver = new ErpRealDriver(configuration.ErpBaseUrl);
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

    protected async Task SetChannelAsync(Channel channel)
    {
        ChannelContext.Set(channel.Type);
        await InitializeAsync();
    }

    private static async Task<IShopDriver?> CreateShopDriverAsync(Dsl.Core.Configuration configuration)
    {
        var channelType = ChannelContext.Get();

        if (channelType == ChannelType.UI)
        {
            return await ShopUiDriver.CreateAsync(configuration.ShopUiBaseUrl);
        }
        else if (channelType == ChannelType.API)
        {
            return new ShopApiDriver(configuration.ShopApiBaseUrl);
        }
        else
        {
            throw new InvalidOperationException($"Unknown channel: {channelType}");
        }
    }
}













