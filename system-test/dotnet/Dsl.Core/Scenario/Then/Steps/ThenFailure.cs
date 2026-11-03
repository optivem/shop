using System.Runtime.CompilerServices;
using Dsl.Port.Then.Steps;
using Dsl.Core.Shared;
using Dsl.Core.UseCase.UseCases.Base;

namespace Dsl.Core.Scenario.Then;

/// <summary>
/// Deferred failure assertion - allows chaining ErrorMessage/FieldErrorMessage before awaiting.
/// Enables fluent syntax: await Scenario(...).Then().ShouldFail().ErrorMessage("...").FieldErrorMessage("...");
/// </summary>
public class ThenFailure<TSuccessResponse, TSuccessVerification>
    : IThenFailure
    where TSuccessVerification : ResponseVerification<TSuccessResponse>
{
    private readonly ThenStage<TSuccessResponse, TSuccessVerification> _thenClause;
    private readonly List<Action<SystemErrorFailureVerification>> _assertions = [];

    internal ThenFailure(ThenStage<TSuccessResponse, TSuccessVerification> thenClause)
    {
        _thenClause = thenClause;
    }

    public ThenFailure<TSuccessResponse, TSuccessVerification> ErrorMessage(string expectedMessage)
    {
        _assertions.Add(v => v.ErrorMessage(expectedMessage));
        return this;
    }

    IThenFailure IThenFailure.ErrorMessage(string expectedMessage) => ErrorMessage(expectedMessage);

    public ThenFailure<TSuccessResponse, TSuccessVerification> FieldErrorMessage(string expectedField, string expectedMessage)
    {
        _assertions.Add(v => v.FieldErrorMessage(expectedField, expectedMessage));
        return this;
    }

    IThenFailure IThenFailure.FieldErrorMessage(string expectedField, string expectedMessage)
        => FieldErrorMessage(expectedField, expectedMessage);

    public ThenFailure<TSuccessResponse, TSuccessVerification> NoOrderPersisted()
    {
        _assertions.Add(v => v.NoOrderPersisted());
        return this;
    }

    IThenFailure IThenFailure.NoOrderPersisted() => NoOrderPersisted();

    /// <summary>
    /// Returns the failure assertion and for further chaining (e.g. .And().Order().HasStatus(...)).
    /// Enables fluent syntax: await Scenario(...).Then().ShouldFail().ErrorMessage("...").And().Order().HasStatus(...);
    /// </summary>
    public ThenFailureAnd<TSuccessResponse, TSuccessVerification> And()
    {
        return new ThenFailureAnd<TSuccessResponse, TSuccessVerification>(_thenClause, _assertions);
    }

    IThenFailureAnd IThenFailure.And() => And();

    public TaskAwaiter GetAwaiter() => ExecuteAssertions().GetAwaiter();

    private async Task ExecuteAssertions()
    {
        var result = await _thenClause.GetExecutionResult();
        if (result.Result == null)
            throw new InvalidOperationException("Cannot verify failure: no operation was executed");
        var failureVerification = result.Result.ShouldFail();
        foreach (var assertion in _assertions)
        {
            assertion(failureVerification);
        }
    }
}


