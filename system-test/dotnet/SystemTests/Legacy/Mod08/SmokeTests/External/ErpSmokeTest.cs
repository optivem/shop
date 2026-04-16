using Common;
using SystemTests.Legacy.Mod07.Base;
using Shouldly;
using Xunit;

namespace SystemTests.Legacy.Mod08.SmokeTests.External;

public class ErpSmokeTest : BaseUseCaseDslTest
{
    [Fact]
    public async Task ShouldBeAbleToGoToErp()
    {
        (await _app.Erp().GoToErp()
            .Execute())
            .ShouldSucceed();
    }
}










