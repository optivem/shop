using Dsl.Port;

namespace SystemTests.Legacy.Mod11.ExternalSystemContractTests.Tax;

public class TaxStubContractTest : BaseTaxContractTest
{
    protected override ExternalSystemMode? FixedExternalSystemMode => ExternalSystemMode.Stub;

    [Fact]
    public async Task ShouldBeAbleToGetConfiguredTaxRate()
    {
        await Scenario()
            .Given().Country().WithCode("LALA").WithTaxRate(0.23m)
            .Then().Country("LALA")
            .HasCountry("LALA")
            .HasTaxRate(0.23m);
    }
}
