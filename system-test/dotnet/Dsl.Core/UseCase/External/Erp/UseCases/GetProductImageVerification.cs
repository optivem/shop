using Driver.Port.External.Erp.Dtos;
using Dsl.Core.Shared;
using Shouldly;

namespace Dsl.Core.External.Erp.UseCases;

public class GetProductImageVerification : ResponseVerification<GetProductImageResponse>
{
    public GetProductImageVerification(GetProductImageResponse response, UseCaseContext context)
        : base(response, context)
    {
    }

    public GetProductImageVerification ContentType(string expectedContentType)
    {
        var actualContentType = Response.ContentType;
        actualContentType.ShouldBe(expectedContentType, $"Expected image content type to be '{expectedContentType}', but was '{actualContentType}'");
        return this;
    }

    public GetProductImageVerification NotEmpty()
    {
        var actualSize = Response.SizeInBytes;
        actualSize.ShouldBeGreaterThan(0, "Expected the image to have content, but it was empty");
        return this;
    }
}
