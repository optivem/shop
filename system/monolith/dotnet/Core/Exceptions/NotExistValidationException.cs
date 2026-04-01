namespace Optivem.Starter.Monolith.Core.Exceptions;

public class NotExistValidationException : ValidationException
{
    public NotExistValidationException(string message) : base(message)
    {
    }
}
