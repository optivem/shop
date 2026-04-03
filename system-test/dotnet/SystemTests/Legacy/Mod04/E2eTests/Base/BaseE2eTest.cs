using Dsl.Port;
using Dsl.Core.Shared;
using ConfigEnvironment = SystemTests.TestInfrastructure.Configuration.Environment;
using SystemTests.TestInfrastructure.Configuration;
using SystemTests.Legacy.Mod04.Base;

namespace SystemTests.Legacy.Mod04.E2eTests.Base;

public abstract class BaseE2eTest : BaseClientTest
{
    protected override ExternalSystemMode? GetFixedExternalSystemMode()
    {
        return ExternalSystemMode.Real;
    }

    public override async Task InitializeAsync()
    {
        await base.InitializeAsync();
        await SetShopClientAsync();
        SetUpExternalClients();
    }

    protected abstract Task SetShopClientAsync();

    protected static string CreateUniqueSku(string baseSku)
    {
        var suffix = Guid.NewGuid().ToString("N")[..8];
        return $"{baseSku}-{suffix}";
    }
}













