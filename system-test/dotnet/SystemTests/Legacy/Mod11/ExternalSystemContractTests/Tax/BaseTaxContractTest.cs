using SystemTests.Legacy.Mod11.ExternalSystemContractTests.Base;

namespace SystemTests.Legacy.Mod11.ExternalSystemContractTests.Tax;

public abstract class BaseTaxContractTest : BaseExternalSystemContractTest
{
    [Fact]
    public async Task ShouldBeAbleToGetTaxRate()
    {
        (await Scenario()
            .Given().Country().WithCode("US").WithTaxRate(0.09m)
            .Then().Country("US"))
            .HasTaxRateIsPositive();
    }
}
