using Driver.Port.External.Identity.Dtos.Error;
using Common;
using Dsl.Core.Shared;

namespace Dsl.Core.External.Identity.UseCases.Base;

public class IdentityProviderUseCaseResult<TSuccessResponse, TSuccessVerification>
    : UseCaseResult<TSuccessResponse, IdentityProviderErrorResponse, TSuccessVerification, ErrorVerification<IdentityProviderErrorResponse>>
    where TSuccessVerification : ResponseVerification<TSuccessResponse>
{
    public IdentityProviderUseCaseResult(
        Result<TSuccessResponse, IdentityProviderErrorResponse> result,
        UseCaseContext context,
        Func<TSuccessResponse, UseCaseContext, TSuccessVerification> verificationFactory)
        : base(result, context, verificationFactory, (error, ctx) => new ErrorVerification<IdentityProviderErrorResponse>(error, ctx, e => e.Message))
    {
    }
}
