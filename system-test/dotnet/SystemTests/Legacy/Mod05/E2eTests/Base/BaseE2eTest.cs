using Dsl.Port;
using Dsl.Core.Shared;
using SystemTests.Legacy.Mod05.Base;
using ConfigEnvironment = SystemTests.TestInfrastructure.Configuration.Environment;
using SystemTests.TestInfrastructure.Configuration;

namespace SystemTests.Legacy.Mod05.E2eTests.Base;

public abstract class BaseE2eTest : BaseDriverTest
{
    protected override ExternalSystemMode? GetFixedExternalSystemMode()
    {
        return ExternalSystemMode.Real;
    }

    public override async Task InitializeAsync()
    {
        await base.InitializeAsync();
        await SetShopDriverAsync();
        SetUpExternalDrivers();
    }

    protected abstract Task SetShopDriverAsync();

    protected static string CreateUniqueSku(string baseSku)
    {
        var suffix = Guid.NewGuid().ToString("N")[..8];
        return $"{baseSku}-{suffix}";
    }
}












