using Driver.Port.External.Erp;
using Driver.Port.External.Erp.Dtos;
using Driver.Port.External.Erp.Dtos.Error;
using Dsl.Core.External.Erp.UseCases.Base;
using Common;
using Dsl.Core.Shared;

namespace Dsl.Core.External.Erp.UseCases;

public class ReturnsProduct : BaseErpUseCase<VoidValue, VoidVerification>
{
    private string? _skuParamAlias;
    private string? _unitPrice;
    private string? _title;
    private string? _description;
    private string? _category;
    private string? _brand;

    public ReturnsProduct(IErpDriver driver, UseCaseContext context)
        : base(driver, context)
    {
    }

    public ReturnsProduct Sku(string? skuParamAlias)
    {
        _skuParamAlias = skuParamAlias;
        return this;
    }

    public ReturnsProduct UnitPrice(string? unitPrice)
    {
        _unitPrice = unitPrice;
        return this;
    }

    public ReturnsProduct UnitPrice(decimal price)
    {
        return UnitPrice(price.ToString());
    }

    public ReturnsProduct Title(string? title)
    {
        _title = title;
        return this;
    }

    public ReturnsProduct Description(string? description)
    {
        _description = description;
        return this;
    }

    public ReturnsProduct Category(string? category)
    {
        _category = category;
        return this;
    }

    public ReturnsProduct Brand(string? brand)
    {
        _brand = brand;
        return this;
    }

    public override async Task<ErpUseCaseResult<VoidValue, VoidVerification>> Execute()
    {
        var sku = _context.GetParamValue(_skuParamAlias);

        var request = new ReturnsProductRequest
        {
            Sku = sku,
            Price = _unitPrice,
            Title = _title,
            Description = _description,
            Category = _category,
            Brand = _brand
        };

        var result = await _driver.ReturnsProductAsync(request);

        return new ErpUseCaseResult<VoidValue, VoidVerification>(
            result,
            _context,
            (response, ctx) => new VoidVerification(response, ctx));
    }
}



