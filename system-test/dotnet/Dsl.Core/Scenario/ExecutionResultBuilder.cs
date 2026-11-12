using Dsl.Core.Shared;
using Common;
using Dsl.Core.UseCase.UseCases.Base;
using Driver.Port.Dtos.Error;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Dsl.Core.Scenario
{
    public class ExecutionResultBuilder<TSuccessResponse, TSuccessVerification>
        where TSuccessVerification : ResponseVerification<TSuccessResponse>
    {
        private readonly MyShopUseCaseResult<TSuccessResponse, TSuccessVerification> _result;
        private string? _orderNumber;
        private string? _couponCode;
        private Func<string, string>? _customerUsernames;

        internal ExecutionResultBuilder(UseCaseResult<TSuccessResponse, SystemError, TSuccessVerification, SystemErrorFailureVerification> result)
        {
            // Cast to derived type - the result is always a MyShopUseCaseResult at runtime
            _result = (MyShopUseCaseResult<TSuccessResponse, TSuccessVerification>)result;
        }

        public ExecutionResultBuilder<TSuccessResponse, TSuccessVerification> OrderNumber(string? orderNumber)
        {
            _orderNumber = orderNumber;
            return this;
        }

        public ExecutionResultBuilder<TSuccessResponse, TSuccessVerification> CouponCode(string? couponCode)
        {
            _couponCode = couponCode;
            return this;
        }

        /// <summary>Resolves the scenario's customer aliases to usernames, for assertions that name customers.</summary>
        public ExecutionResultBuilder<TSuccessResponse, TSuccessVerification> CustomerUsernames(Func<string, string> customerUsernames)
        {
            _customerUsernames = customerUsernames;
            return this;
        }

        public ExecutionResult<TSuccessResponse, TSuccessVerification> Build()
        {
            return new ExecutionResult<TSuccessResponse, TSuccessVerification>(
                _result,
                _orderNumber,
                _couponCode,
                _customerUsernames);
        }
    }
}



