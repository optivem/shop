using Dsl.Port.When.Steps.Base;

namespace Dsl.Port.When.Steps;

public interface IWhenBrowseCoupons : IWhenStep
{
    IWhenBrowseCoupons WithPage(string? page);

    IWhenBrowseCoupons WithSize(string? size);
}
