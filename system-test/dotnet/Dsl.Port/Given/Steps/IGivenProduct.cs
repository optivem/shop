using Dsl.Port.Given.Steps.Base;

namespace Dsl.Port.Given.Steps;

public interface IGivenProduct : IGivenStep
{
    IGivenProduct WithSku(string? sku);

    IGivenProduct WithUnitPrice(string? unitPrice);

    IGivenProduct WithUnitPrice(decimal? unitPrice);

    IGivenProduct WithTitle(string? title);

    IGivenProduct WithDescription(string? description);

    IGivenProduct WithCategory(string? category);

    IGivenProduct WithBrand(string? brand);
}
