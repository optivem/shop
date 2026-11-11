namespace SystemTests.Latest.AcceptanceTests.Base;

/// <summary>UC-007's Background: the same ERP product list as UC-006's, and an empty cart (every test starts a fresh session).</summary>
public abstract class BaseViewProductTest : BaseBrowseCatalogTest
{
    protected const string NoSuchSku = "NO-SUCH-SKU";
}
