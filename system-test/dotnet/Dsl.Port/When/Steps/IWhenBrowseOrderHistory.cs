using Dsl.Port.When.Steps.Base;

namespace Dsl.Port.When.Steps;

public interface IWhenBrowseOrderHistory : IWhenStep
{
    IWhenBrowseOrderHistory WithOrderNumber(string? orderNumber);

    IWhenBrowseOrderHistory WithPage(string? page);

    IWhenBrowseOrderHistory WithSize(string? size);
}
