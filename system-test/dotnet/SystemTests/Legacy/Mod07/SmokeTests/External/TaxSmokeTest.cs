using Common;
using SystemTests.Legacy.Mod07.Base;
using Shouldly;
using Xunit;

namespace SystemTests.Legacy.Mod07.SmokeTests.External;

public class TaxSmokeTest : BaseUseCaseDslTest
{
    [Fact]
    public async Task ShouldBeAbleToGoToTax()
    {
        (await _app.Tax().GoToTax()
            .Execute())
            .ShouldSucceed();
    }
}
