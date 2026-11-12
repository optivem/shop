using Dsl.Core.Shared;
using Common;
using Dsl.Core.UseCase.UseCases.Base;

namespace Dsl.Core.Scenario
{
    public class ExecutionResult<TSuccessResponse, TSuccessVerification>
        where TSuccessVerification : ResponseVerification<TSuccessResponse>
    {
        internal ExecutionResult(MyShopUseCaseResult<TSuccessResponse, TSuccessVerification> result,
            string? orderNumber, string? couponCode, Func<string, string>? customerUsernames = null)
        {
            _customerUsernames = customerUsernames;
            if (result == null)
            {
                throw new ArgumentException("Result cannot be null");
            }

            Result = result;
            OrderNumber = orderNumber;
            CouponCode = couponCode;
            Context = new ExecutionResultContext(orderNumber, couponCode);
        }

        private readonly Func<string, string>? _customerUsernames;

        public MyShopUseCaseResult<TSuccessResponse, TSuccessVerification> Result { get; }

        /// <summary>The username of the customer known in this scenario by the given alias, for assertions that name customers.</summary>
        public string CustomerUsername(string customerAlias)
            => (_customerUsernames ?? throw new InvalidOperationException(
                "The executed operation does not know this scenario's customers"))(customerAlias);

        /// <summary>How long the When step took to run, for asserting that a failure was fast.</summary>
        public TimeSpan Elapsed { get; internal set; }

        public string? OrderNumber { get; }

        public string? CouponCode { get; }

        /// <summary>
        /// Context with order number and coupon code from the executed operation.
        /// </summary>
        public ExecutionResultContext Context { get; }
    }
}


