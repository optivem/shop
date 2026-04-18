using Dsl.Port;

namespace SystemTests.Legacy.Mod11.ExternalSystemContractTests.Tax;

public class TaxStubContractTest : BaseTaxContractTest
{
    protected override ExternalSystemMode? FixedExternalSystemMode => ExternalSystemMode.Stub;
}
