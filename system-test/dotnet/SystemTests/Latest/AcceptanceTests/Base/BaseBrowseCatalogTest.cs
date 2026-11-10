using Dsl.Port.Given.Steps;
using Optivem.Testing;

namespace SystemTests.Latest.AcceptanceTests.Base;

/// <summary>UC-006's Background: the ERP's product list every Browse Catalog scenario starts from.</summary>
public abstract class BaseBrowseCatalogTest : BaseAcceptanceTest
{
    protected const string Hp15 = "HP-15";
    protected const string DellXps = "DELL-XPS";
    protected const string LenovoT14 = "LENOVO-T14";
    protected const string DellU2723Qe = "DELL-U2723QE";
    protected const string Lg27Ul500 = "LG-27UL500";
    protected const string LogiMx3S = "LOGI-MX3S";
    protected static readonly string[] AllProducts = [Hp15, DellXps, LenovoT14, DellU2723Qe, Lg27Ul500, LogiMx3S];

    protected const string Featured = "Featured";
    protected const string PriceLowToHigh = "Price: low to high";
    protected const string PriceHighToLow = "Price: high to low";
    protected const string NameAToZ = "Name: A to Z";

    /// <summary>Given the ERP has these products, in this order.</summary>
    protected IGivenProducts GivenTheCatalog(Channel channel)
        => Scenario(channel).Given().Products()
            .WithProduct(Hp15, "HP Laptop 15", "HP", "Laptops", "699.99", "Everyday 15-inch laptop")
            .WithProduct(DellXps, "Dell XPS 13", "Dell", "Laptops", "1299.99", "Compact 13-inch ultrabook")
            .WithProduct(LenovoT14, "Lenovo ThinkPad T14", "Lenovo", "Laptops", "999.99", "Business laptop")
            .WithProduct(DellU2723Qe, "Dell UltraSharp 27 4K", "Dell", "Monitors", "579.99", "27-inch USB-C monitor")
            .WithProduct(Lg27Ul500, "LG 27-inch 4K UHD Monitor", "LG", "Monitors", "299.99", "HDR10 monitor")
            .WithProduct(LogiMx3S, "Logitech MX Master 3S", "Logitech", "Accessories", "99.99", "Wireless mouse");
}
