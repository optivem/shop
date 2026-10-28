using SystemTests.Latest.ExternalSystemContractTests.Base;

namespace SystemTests.Latest.ExternalSystemContractTests.Erp;

public abstract class BaseErpContractTest : BaseExternalSystemContractTest
{
    [Fact]
    public async Task ShouldBeAbleToGetProduct()
    {
        await Scenario()
            .Given().Product().WithSku("BOOK-123").WithUnitPrice(12.0m)
            .Then().Product("BOOK-123")
            .HasSku("BOOK-123")
            .HasPrice(12.0m);
    }
}










