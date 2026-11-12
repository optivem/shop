using Dsl.Core.Shared;
using Shouldly;
using Driver.Port.Dtos.Error;

namespace Dsl.Core.UseCase.UseCases.Base;

public class SystemErrorFailureVerification : ErrorVerification<SystemError>
{
    public SystemErrorFailureVerification(SystemError error, UseCaseContext context)
        : base(error, context, e => e.Message)
    {
    }

    public new SystemErrorFailureVerification ErrorMessage(string expectedMessage)
    {
        base.ErrorMessage(expectedMessage);
        return this;
    }

    /// <summary>
    /// The call failed fast as 503 Service Unavailable, told the client when to retry (a whole number of
    /// seconds in Retry-After) and explained itself as problem+json. Only the API channel sees HTTP.
    /// </summary>
    public SystemErrorFailureVerification ServiceUnavailable()
    {
        var error = Response;
        error.HttpStatus.ShouldBe(503,
            $"Expected HTTP status 503, but got: {error.HttpStatus} (the UI channel has no HTTP status; run this on the API channel)");
        error.RetryAfter.ShouldNotBeNull("Expected a Retry-After header in whole seconds, but there was none");
        error.RetryAfter.ShouldMatch(@"^[1-9]\d*$", $"Expected a Retry-After header in whole seconds, but got: '{error.RetryAfter}'");
        error.ContentType.ShouldBe("application/problem+json",
            $"Expected Content-Type application/problem+json, but got: '{error.ContentType}'");
        return this;
    }

    public SystemErrorFailureVerification NoOrderPersisted()
    {
        Response.ShouldNotBeNull("Expected the rejected operation to yield an error response (nothing persisted), but no error was returned");
        return this;
    }

    public SystemErrorFailureVerification FieldErrorMessage(string expectedField, string expectedMessage)
    {
        var expandedExpectedField = Context.ExpandAliases(expectedField);
        var expandedExpectedMessage = Context.ExpandAliases(expectedMessage);
        var error = Response;
        var fields = error.Fields;

        fields.ShouldNotBeNull("Expected field errors but none were found");
        fields.ShouldNotBeEmpty("Expected field errors but none were found");

        var matchingFieldError = fields.FirstOrDefault(f => expandedExpectedField.Equals(f.Field));

        matchingFieldError.ShouldNotBeNull(
            $"Expected field error for field '{expandedExpectedField}', but field was not found in errors: {string.Join(", ", fields.Select(f => f.Field))}");

        var actualMessage = matchingFieldError.Message;
        actualMessage.ShouldBe(expandedExpectedMessage,
            $"Expected field error message for field '{expandedExpectedField}': '{expandedExpectedMessage}', but got: '{actualMessage}'");

        return this;
    }
}
