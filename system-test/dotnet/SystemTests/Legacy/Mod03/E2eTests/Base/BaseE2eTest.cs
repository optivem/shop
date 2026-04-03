using Dsl.Port;
using Dsl.Core.Shared;
using ConfigEnvironment = SystemTests.TestInfrastructure.Configuration.Environment;
using SystemTests.TestInfrastructure.Configuration;
using SystemTests.Legacy.Mod03.Base;

namespace SystemTests.Legacy.Mod03.E2eTests.Base;

public abstract class BaseE2eTest : BaseRawTest
{
    protected override ExternalSystemMode? GetFixedExternalSystemMode()
    {
        return ExternalSystemMode.Real;
    }

    public override async Task InitializeAsync()
    {
        await base.InitializeAsync();
        await SetShopRawAsync();
        SetUpExternalHttpClients();
    }

    protected abstract Task SetShopRawAsync();

    protected static string CreateUniqueSku(string baseSku)
    {
        var suffix = Guid.NewGuid().ToString("N")[..8];
        return $"{baseSku}-{suffix}";
    }
}













