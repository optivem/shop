using Dsl.Core.Shared;
using Dsl.Port.Then;
using Dsl.Port.Then.Steps;
using Driver.Adapter;
using Optivem.Testing;

namespace Dsl.Core.Scenario.Then
{
    public class ThenStage<TSuccessResponse, TSuccessVerification> : BaseClause, IThenResultStage
        where TSuccessVerification : ResponseVerification<TSuccessResponse>
    {
        private readonly UseCaseDsl _app;
        private readonly Func<Task<ExecutionResult<TSuccessResponse, TSuccessVerification>>> _lazyExecute;
        private ExecutionResult<TSuccessResponse, TSuccessVerification>? _executionResult;
        private bool _executionCompleted = false;

        public ThenStage(Channel channel, UseCaseDsl app, Func<Task<ExecutionResult<TSuccessResponse, TSuccessVerification>>> lazyExecute)
            : base(channel)
        {
            _app = app;
            _lazyExecute = lazyExecute;
        }

        public ThenSuccess<TSuccessResponse, TSuccessVerification> ShouldSucceed()
        {
            return new ThenSuccess<TSuccessResponse, TSuccessVerification>(this);
        }

        IThenSuccess IThenResultStage.ShouldSucceed() => ShouldSucceed();

        public ThenFailure<TSuccessResponse, TSuccessVerification> ShouldFail()
        {
            return new ThenFailure<TSuccessResponse, TSuccessVerification>(this);
        }

        IThenFailure IThenResultStage.ShouldFail() => ShouldFail();

        internal UseCaseDsl App => _app;

        /// <summary>How long the When step took; only meaningful once the execution result has been fetched.</summary>
        internal TimeSpan Elapsed => _executionResult?.Elapsed ?? TimeSpan.Zero;

        internal async Task<ExecutionResult<TSuccessResponse, TSuccessVerification>> GetExecutionResult()
        {
            if (!_executionCompleted)
            {
                _executionResult = await _lazyExecute();
                _executionCompleted = true;
            }
            return _executionResult!;
        }
    }
}


