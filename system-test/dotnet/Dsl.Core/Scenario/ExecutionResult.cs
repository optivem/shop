using Dsl.Core.Shared;
using Common;
using Dsl.Core.UseCase.UseCases.Base;

namespace Dsl.Core.Scenario
{
    public class ExecutionResult<TSuccessResponse, TSuccessVerification>
        where TSuccessVerification : ResponseVerification<TSuccessResponse>
    {
        internal ExecutionResult(MyShopUseCaseResult<TSuccessResponse, TSuccessVerification> result,
            string? orderNumber, string? couponCode)
        {
            if (result == null)
            {
                throw new ArgumentException("Result cannot be null");
            }

            Result = result;
            OrderNumber = orderNumber;
            CouponCode = couponCode;
            Context = new ExecutionResultContext(orderNumber, couponCode);
        }

        public MyShopUseCaseResult<TSuccessResponse, TSuccessVerification> Result { get; }

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


