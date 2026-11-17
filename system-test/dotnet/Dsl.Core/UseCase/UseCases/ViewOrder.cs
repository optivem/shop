using Driver.Port;
using Dsl.Core.UseCase.UseCases.Base;
using Dsl.Core.Shared;
using Driver.Port.Dtos;
using Driver.Port.Dtos.Error;

namespace Dsl.Core.UseCase.UseCases;

public class ViewOrder : BaseMyShopUseCase<ViewOrderResponse, ViewOrderVerification>
{
    private string? _orderNumberResultAlias;
    private string? _whenAskedToSignIn;

    public ViewOrder(IMyShopDriver driver, UseCaseContext context)
        : base(driver, context)
    {
    }

    public ViewOrder OrderNumber(string? orderNumberResultAlias)
    {
        _orderNumberResultAlias = orderNumberResultAlias;
        return this;
    }

    /// <summary>A visitor asked to sign in signs in as the customer (UI only).</summary>
    public ViewOrder SignInWhenAsked()
    {
        _whenAskedToSignIn = ViewOrderRequest.SignIn;
        return this;
    }

    public override async Task<MyShopUseCaseResult<ViewOrderResponse, ViewOrderVerification>> Execute()
    {
        var orderNumber = _context.GetResultValue(_orderNumberResultAlias);
        var request = new ViewOrderRequest { OrderNumber = orderNumber, WhenAskedToSignIn = _whenAskedToSignIn };

        var result = await _driver.ViewOrderAsync(request);

        return new MyShopUseCaseResult<ViewOrderResponse, ViewOrderVerification>(
            result,
            _context,
            (response, ctx) => new ViewOrderVerification(response, ctx));
    }
}



