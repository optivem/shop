namespace Optivem.Starter.Monolith.Core.Exceptions;

public class ValidationException : Exception
{
    public string? FieldName { get; }

    public ValidationException(string message) : base(message)
    {
        FieldName = null;
    }

    public ValidationException(string fieldName, string message) : base(message)
    {
        FieldName = fieldName;
    }
}
