using Dsl.Port.When.Steps.Base;

namespace Dsl.Port.When.Steps;

public interface IWhenDeliverOrder : IWhenStep
{
    IWhenDeliverOrder WithOrderNumber(string? orderNumber);
}
