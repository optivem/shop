using SystemTests.TestInfrastructure.Configuration;
using Dsl.Core;
using Driver.Port.External.Erp;
using Driver.Adapter.Api;
using Driver.Adapter.Ui;
using Dsl.Core.UseCase;
using Driver.Port;
using Driver.Adapter.External.Erp;
using Driver.Adapter.External.Tax;
using Optivem.Testing;
using Xunit;

namespace SystemTests.Legacy.Mod06.Base;


public abstract class BaseChannelDriverTest : BaseConfigurableTest, IAsyncLifetime
{
    protected IMyShopDriver? _shopDriver;
    protected ErpRealDriver? _erpDriver;
    protected TaxRealDriver? _taxDriver;

    public virtual async Task InitializeAsync()
    {
        await SetupDrivers();
    }

    private async Task SetupDrivers()
    {
        var configuration = LoadConfiguration();

        // Only create shop driver if channel context is set (for channel-parameterized tests)
        // For non-channel tests (like Erp), skip shop driver creation
        try
        {
            _shopDriver = await CreateMyShopDriverAsync(configuration);
        }
        catch (InvalidOperationException ex) when (ex.Message.Contains("Channel type is not set"))
        {
            _shopDriver = null;
        }

        _erpDriver = new ErpRealDriver(configuration.ErpBaseUrl);
        _taxDriver = new TaxRealDriver(configuration.TaxBaseUrl);
    }

    public virtual async Task DisposeAsync()
    {
        if (_shopDriver != null)
            await _shopDriver.DisposeAsync();

        _erpDriver?.Dispose();
        _taxDriver?.Dispose();
    }

    protected async Task SetChannelAsync(Channel channel)
    {
        ChannelContext.Set(channel.Type);
        await SetupDrivers();
    }

    private static async Task<IMyShopDriver?> CreateMyShopDriverAsync(Dsl.Core.Configuration configuration)
    {
        var channelType = ChannelContext.Get();

        if (channelType == ChannelType.UI)
        {
            return await MyShopUiDriver.CreateAsync(configuration.MyShopUiBaseUrl, configuration.KeycloakBaseUrl);
        }
        else if (channelType == ChannelType.API)
        {
            return new MyShopApiDriver(configuration.MyShopApiBaseUrl, configuration.KeycloakBaseUrl);
        }
        else
        {
            throw new InvalidOperationException($"Unknown channel: {channelType}");
        }
    }
}













