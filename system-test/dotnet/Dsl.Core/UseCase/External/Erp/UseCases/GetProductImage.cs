using Driver.Port.External.Erp;
using Driver.Port.External.Erp.Dtos;
using Dsl.Core.External.Erp.UseCases.Base;
using Dsl.Core.Shared;

namespace Dsl.Core.External.Erp.UseCases;

public class GetProductImage : BaseErpUseCase<GetProductImageResponse, GetProductImageVerification>
{
    private string? _imageUrl;

    public GetProductImage(IErpDriver driver, UseCaseContext context)
        : base(driver, context)
    {
    }

    public GetProductImage ImageUrl(string? imageUrl)
    {
        _imageUrl = imageUrl;
        return this;
    }

    public override async Task<ErpUseCaseResult<GetProductImageResponse, GetProductImageVerification>> Execute()
    {
        var request = new GetProductImageRequest
        {
            ImageUrl = _imageUrl
        };

        var result = await _driver.GetProductImageAsync(request);

        return new ErpUseCaseResult<GetProductImageResponse, GetProductImageVerification>(
            result,
            _context,
            (response, ctx) => new GetProductImageVerification(response, ctx));
    }
}
