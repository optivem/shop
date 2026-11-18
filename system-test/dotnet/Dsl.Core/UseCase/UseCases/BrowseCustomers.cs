using Driver.Port;
using Driver.Port.Dtos;
using Dsl.Core.Shared;
using Dsl.Core.UseCase.UseCases.Base;

namespace Dsl.Core.UseCase.UseCases;

public class BrowseCustomers : BaseMyShopUseCase<BrowseCustomersResponse, BrowseCustomersVerification>
{
    public BrowseCustomers(IMyShopDriver driver, UseCaseContext context)
        : base(driver, context)
    {
    }

    public override async Task<MyShopUseCaseResult<BrowseCustomersResponse, BrowseCustomersVerification>> Execute()
    {
        var result = await _driver.BrowseCustomersAsync(new BrowseCustomersRequest());

        return new MyShopUseCaseResult<BrowseCustomersResponse, BrowseCustomersVerification>(
            result,
            _context,
            (response, ctx) => new BrowseCustomersVerification(response, ctx));
    }
}
