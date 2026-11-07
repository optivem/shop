using Common;
using Dsl.Port.Given.Steps;
using Dsl.Core.Scenario.Given;
using static Dsl.Core.Scenario.ScenarioDefaults;

namespace Dsl.Core.Scenario.Given;

public class GivenProduct : BaseGiven, IGivenProduct
{
    private string? _sku;
    private string? _unitPrice;
    private string? _title;
    private string? _description;
    private string? _category;
    private string? _brand;

    public GivenProduct(GivenStage givenClause)
        : base(givenClause)
    {
        WithSku(DefaultSku);
        WithUnitPrice(DefaultUnitPrice);
    }

    public GivenProduct WithSku(string? sku)
    {
        _sku = sku;
        return this;
    }

    IGivenProduct IGivenProduct.WithSku(string? sku) => WithSku(sku);

    public GivenProduct WithUnitPrice(string? unitPrice)
    {
        _unitPrice = unitPrice;
        return this;
    }

    IGivenProduct IGivenProduct.WithUnitPrice(string? unitPrice) => WithUnitPrice(unitPrice);

    public GivenProduct WithUnitPrice(decimal? unitPrice)
    {
        return WithUnitPrice(Converter.FromDecimal(unitPrice));
    }

    IGivenProduct IGivenProduct.WithUnitPrice(decimal? unitPrice) => WithUnitPrice(unitPrice);

    public GivenProduct WithTitle(string? title)
    {
        _title = title;
        return this;
    }

    IGivenProduct IGivenProduct.WithTitle(string? title) => WithTitle(title);

    public GivenProduct WithDescription(string? description)
    {
        _description = description;
        return this;
    }

    IGivenProduct IGivenProduct.WithDescription(string? description) => WithDescription(description);

    public GivenProduct WithCategory(string? category)
    {
        _category = category;
        return this;
    }

    IGivenProduct IGivenProduct.WithCategory(string? category) => WithCategory(category);

    public GivenProduct WithBrand(string? brand)
    {
        _brand = brand;
        return this;
    }

    IGivenProduct IGivenProduct.WithBrand(string? brand) => WithBrand(brand);

    internal override async Task Execute(UseCaseDsl app)
    {
        (await app.Erp().ReturnsProduct()
            .Sku(_sku)
            .UnitPrice(_unitPrice)
            .Title(_title)
            .Description(_description)
            .Category(_category)
            .Brand(_brand)
            .Execute())
            .ShouldSucceed();
    }
}


