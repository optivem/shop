using System.Runtime.CompilerServices;

namespace Dsl.Port.Then.Steps;

public interface IThenFailure
{
    IThenFailure ErrorMessage(string expectedMessage);

    IThenFailure FieldErrorMessage(string expectedField, string expectedMessage);

    IThenFailure NoOrderPersisted();

    IThenFailure ServiceUnavailable();

    IThenFailure FailedWithinSeconds(int seconds);

    IThenFailureAnd And();

    TaskAwaiter GetAwaiter();
}
