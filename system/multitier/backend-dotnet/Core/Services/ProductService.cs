using MyCompany.MyShop.Backend.Core.Dtos;
using MyCompany.MyShop.Backend.Core.Dtos.External;
using MyCompany.MyShop.Backend.Core.Exceptions;
using MyCompany.MyShop.Backend.Core.Services.External;

namespace MyCompany.MyShop.Backend.Core.Services;

/// <summary>Products are owned by the ERP; MyShop only proxies its catalog.</summary>
public class ProductService
{
    private const string MsgProductDoesNotExist = "Product {0} does not exist.";

    private readonly ErpGateway _erpGateway;

    public ProductService(ErpGateway erpGateway)
    {
        _erpGateway = erpGateway;
    }

    public async Task<List<ViewProductDetailsResponse>> BrowseProductsAsync()
    {
        var products = await _erpGateway.GetProductsAsync();
        return products.Select(ToResponse).ToList();
    }

    public async Task<ViewProductDetailsResponse> ViewProductAsync(string sku)
    {
        var product = await _erpGateway.GetProductDetailsAsync(sku)
            ?? throw new NotExistValidationException(string.Format(MsgProductDoesNotExist, sku));
        return ToResponse(product);
    }

    private static ViewProductDetailsResponse ToResponse(ErpProductDetailsResponse product)
        => new()
        {
            Sku = product.Id,
            Title = product.Title,
            Description = product.Description,
            Brand = product.Brand,
            Category = product.Category,
            Price = product.Price
        };
}
