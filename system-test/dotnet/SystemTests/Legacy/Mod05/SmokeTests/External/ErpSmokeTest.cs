using Common;
using SystemTests.Legacy.Mod05.Base;
using Shouldly;
using Xunit;

namespace SystemTests.Legacy.Mod05.SmokeTests.External;

public class ErpSmokeTest : BaseDriverTest
{
    public override async Task InitializeAsync()
    {
        await base.InitializeAsync();
        SetUpExternalDrivers();
    }

    [Fact]
    public async Task ShouldBeAbleToGoToErp()
    {
        var result = await _erpDriver!.GoToErpAsync();
        result.ShouldBeSuccess();
    }
}










