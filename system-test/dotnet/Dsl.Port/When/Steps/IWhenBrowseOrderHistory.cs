using Dsl.Port.When.Steps.Base;

namespace Dsl.Port.When.Steps;

public interface IWhenBrowseOrderHistory : IWhenStep
{
    IWhenBrowseOrderHistory WithOrderNumber(string? orderNumber);
}
